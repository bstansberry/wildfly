/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.domain.management.connections.database;

import static org.jboss.as.domain.management.DomainManagementLogger.DATABASE_POOL_LOGGER;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.domain.management.DomainManagementLogger;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.as.domain.management.connections.ConnectionManager;

/**
 * A {@link ConnectionManager} that maintains a pool of connections to a database.
 * <p>
 * The constructor takes a list of parameters and creates pool of minimum connections {@code minPoolSize}
 * this make sure a minimum size of connections always is created. The {@code maxPoolSize} make sure the pool
 * can grow larger then the specified size, and in case the capacity is exceed, a requested will wait for an
 * existing connection to free up or throw an {@code IllegalStateException} on timeout.
 * </p>
 * <p>
 * For acquiring a new connection use the {@link #getFallibleConnection()} method.  This returns a connection from the pool,
 * creating one if necessary. The connection will automatically return itself to the pool when {@link Connection#close()}
 * is called.
 * <p/>
 * A background reaper task will clean up unused connections in the pool after a certain time. The connection idle time
 * can be controlled by calling the {@code setConnectionIdleTime(milliseconds)}. Connections that have been checked
 * out but never {@link Connection#close() closed} will not be reaped.
 * </p>
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DatabaseConnectionPool extends AbstractDatabaseConnectionManager {

    /**
     * Configuration object
     */
    private final PoolConfiguration poolConfiguration;
    /**
     * Permits to create a connection; acquire before create, release after destroy
     */
    private final Semaphore permits;
    /**
     * The available connections
     */
    private final List<DatabaseConnection> availablePool;
    /**
     * Connections that have been checked out for any reason
     */
    private final Set<DatabaseConnection> unavailablePool;
    /**
     * Checked out connections that have been provided to outside callers
     */
    private final ConcurrentMap<DatabaseConnection, Boolean> permitHolders = new ConcurrentHashMap<DatabaseConnection, Boolean>();
    private final ScheduledExecutorService executorService;
    private final Runnable fillPoolTask;
    private final ReaperTask reaper;
    private final ValidationTask validationTask;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final JDBCConnectionFactory connectionFactory;

    protected DatabaseConnectionPool(PoolConfiguration poolConfiguration, ScheduledExecutorService scheduledExecutorService) {
        this.poolConfiguration = poolConfiguration;
        this.connectionFactory = new JDBCConnectionFactory(poolConfiguration);
        int maxPoolSize = poolConfiguration.getMaxSize();
        this.permits = new Semaphore(maxPoolSize, true);
        this.availablePool = new ArrayList<DatabaseConnection>(maxPoolSize);
        this.unavailablePool = new HashSet<DatabaseConnection>(maxPoolSize);
        this.executorService = scheduledExecutorService;
        this.reaper = new ReaperTask();
        this.validationTask = new ValidationTask();
        this.fillPoolTask = new Runnable() {
            @Override
            public void run() {
                fillPoolToMin(false);
            }
        };
    }

    @Override
    public void start() {
        stopped.set(false);
        reaper.schedule();
        validationTask.schedule();
        fillPoolToMin(poolConfiguration.isFillPoolAsync());
    }

    @Override
    public void stop() {
        stopped.set(true);
        reaper.terminate();
        validationTask.terminate();
        flush(true);
    }

    @Override
    protected FallibleConnection getFallibleConnection() throws SQLException {

        try {
            long timeout = poolConfiguration.getBlockingTimeout();
            if (permits.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {

                DatabaseConnection dc = null;
                do {
                    synchronized (availablePool) {

                        if (stopped.get()) {
                            permits.release();
                            throw DomainManagementMessages.MESSAGES.connectionPoolStopped();
                        }

                        DATABASE_POOL_LOGGER.tracef("Looking for a free connection in the pool");

                        int availSize = availablePool.size();
                        if (availSize > 0) {
                            dc = availablePool.remove(availSize - 1);
                            unavailablePool.add(dc);
                        }
                    }

                    if (dc != null) {
                        if (!poolConfiguration.isValidateOnIssue() || validateConnection(dc)) {
                            permitHolders.put(dc, Boolean.TRUE);
                            dc.lease();
                            return dc;
                        }

                        // If we get here the connection was invalid
                        synchronized (availablePool) {
                            unavailablePool.remove(dc);
                        }
                        dc.terminateConnection();

                        if (poolConfiguration.isUseFastFail()) {
                            break;
                        }

                    }
                } while (getAvailablePoolSize() > 0);


                DATABASE_POOL_LOGGER.tracef("No valid connections available in the pool, creating a new one");

                return createConnectionForCheckout();
            } else {
                throw DomainManagementMessages.MESSAGES.timeoutObtainingConnection(timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw DomainManagementMessages.MESSAGES.interruptedObtainingConnection(e);
        }
    }

    int getCurrentPoolSize() {
        synchronized (availablePool) {
            return availablePool.size() + unavailablePool.size();
        }
    }

    void reapConnections() {

        final long idleTimeout = poolConfiguration.getConnectionIdleTimeout();
        final long stale = System.currentTimeMillis() - idleTimeout;
        Set<DatabaseConnection> toDestroy = null;
        try {
            for (;;) {
                synchronized (availablePool) {
                    if (availablePool.size() == 0) {
                        // done
                        break;
                    }
                    DatabaseConnection conn = availablePool.get(0);
                    if (stale > conn.getLastUse()
                            && (!poolConfiguration.isStrictMin() || getCurrentPoolSize() > poolConfiguration.getMaxSize())) {
                        DATABASE_POOL_LOGGER.tracef("Connection has been idle for more than %d ms; terminating", idleTimeout);

                        availablePool.remove(0);
                        if (toDestroy == null) {
                            toDestroy = new HashSet<DatabaseConnection>();
                        }
                        toDestroy.add(conn);

                    } else {
                        break; // everything else will be newer
                    }
                }
            }
        } finally {
            if (toDestroy != null) {
                for (DatabaseConnection dead : toDestroy) {
                    dead.terminateConnection();
                }

                fillPoolToMin(poolConfiguration.isFillPoolAsync());
            }
        }
    }

    /**
     * Called when the connection returned by {@link DatabaseConnection#getUnderlyingConnection()} is closed.
     */
    void returnConnection(DatabaseConnection conn) {

        if (conn.getState() == FallibleConnection.State.DESTROYED) { // could happen following a flush call

            if (permitHolders.containsKey(conn)) {
                permitHolders.remove(conn);
                permits.release();
            }

            return;
        }

        boolean terminate;
        synchronized (availablePool) {
            terminate = conn.getState() != FallibleConnection.State.NORMAL;

            unavailablePool.remove(conn);

            if (!terminate && getCurrentPoolSize() >= poolConfiguration.getMaxSize()) {
                // Hmm, bug somewhere
                DATABASE_POOL_LOGGER.maxPoolSizeExceededDestroyingReturnedConnection(conn);
                terminate = true;
            }

            if (!terminate) {
                conn.expireLease();
                if (!availablePool.contains(conn)) {
                    availablePool.add(conn);
                }
            }

            if (permitHolders.containsKey(conn)) {
                permitHolders.remove(conn);
                permits.release();
            }

        }

        if (terminate) {
            conn.terminateConnection();
        }
    }

    /**
     * Force the reaper to run
     */
    void forceReaper() {
        reaper.force();
    }

    private int getAvailablePoolSize() {
        synchronized (availablePool) {
            return availablePool.size();
        }
    }

    private boolean validateConnection(DatabaseConnection dc) {
        try {
            SQLException e = connectionFactory.validateConnection(dc.getConnection());
            if (e == null) {
                return true;
            }

            DATABASE_POOL_LOGGER.connectionInvalid(dc, e);

        } catch (Throwable t) {
            DATABASE_POOL_LOGGER.throwableWhileValidatingConnection(dc, t);
        }

        return false;

    }

    /**
     * Call with a permit in hand
     */
    private DatabaseConnection createConnectionForCheckout() {
        DatabaseConnection dbConnection = null;
        try {
            // No, the pool was empty, so we have to make a new one.
            Connection conn = connectionFactory.createJDBCConnection();
            dbConnection = new DatabaseConnection(conn, this);

            fillPoolToMin(poolConfiguration.isFillPoolAsync());

            synchronized (availablePool) {
                unavailablePool.add(dbConnection);
            }

            permitHolders.put(dbConnection, Boolean.TRUE);
            dbConnection.lease();

            return dbConnection;
        } catch (Throwable t) {

            if (dbConnection != null) {
                dbConnection.terminateConnection();
            }
            DATABASE_POOL_LOGGER.unexpectedThrowableWhileCreatingConnection(poolConfiguration.getPoolName(), t);

            synchronized (availablePool) {
                unavailablePool.remove(dbConnection);
            }

            permits.release();

            throw DomainManagementMessages.MESSAGES.unexpectedThrowableWhileCreatingConnection(poolConfiguration.getPoolName(), t);
        }

    }

    private void fillPoolToMin(boolean async) {
        if (!stopped.get() && poolConfiguration.isFillToMinSupported()) {
            if (async) {
                executorService.execute(fillPoolTask);
            } else {
                for (; ; ) {
                    try {
                        // See if we can get a permit without barging; if not the pool is full
                        if (permits.tryAcquire(0, TimeUnit.MILLISECONDS)) {
                            try {
                                if (stopped.get()) {
                                    break;
                                }
                                if (getCurrentPoolSize() >= poolConfiguration.getMinSize()) {
                                    break;
                                }
                                DatabaseConnection dbConnection = null;
                                DatabaseConnection added = null;
                                try {
                                    Connection conn = connectionFactory.createJDBCConnection();
                                    dbConnection = new DatabaseConnection(conn, this);
                                    DATABASE_POOL_LOGGER.trace("Connection created");
                                    synchronized (availablePool) {
                                        if (!stopped.get()) {
                                            availablePool.add(dbConnection);
                                            added = dbConnection;
                                        }
                                    }
                                } catch (SQLException e) {
                                    DomainManagementLogger.ROOT_LOGGER.failedToCreateConnectionForPool(poolConfiguration.getPoolName(), e);
                                    break;
                                } finally {
                                    if (added == null && dbConnection != null) {
                                        // An error occurred or we were stopped; clean up
                                        dbConnection.terminateConnection();
                                    }
                                }
                            } finally {
                                permits.release();
                            }
                        } else {
                            // No permit; pool must be full
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        DATABASE_POOL_LOGGER.trace("Interrupted while requesting permit in fillPoolToMin");
                        break;
                    }
                }
            }
        }
    }

    private void flush(boolean includeCheckedOut) {
        final List<DatabaseConnection> toDestroy = new ArrayList<DatabaseConnection>();
        synchronized (availablePool) {
            if (includeCheckedOut) {
                for (Iterator<DatabaseConnection> it = unavailablePool.iterator(); it.hasNext(); ) {
                    DatabaseConnection conn = it.next();
                    it.remove();
                    conn.prepareToDestroy();
                    toDestroy.add(conn);

                    if (permitHolders.containsKey(conn)) {
                        permitHolders.remove(conn);
                        permits.release();
                    }
                    DATABASE_POOL_LOGGER.destroyingActiveConnection(poolConfiguration.getPoolName(), conn);
                }
            }
            for (Iterator<DatabaseConnection> it = availablePool.iterator(); it.hasNext(); ) {
                DatabaseConnection conn = it.next();
                it.remove();
                toDestroy.add(conn);
            }
        }

        for (DatabaseConnection conn : toDestroy) {
            conn.terminateConnection();
        }

        fillPoolToMin(poolConfiguration.isFillPoolAsync());
    }

    private class ReaperTask implements Runnable {

        private Future<?> future;

        @Override
        public void run() {
            try {
                reapConnections();
            } finally {
                schedule();
            }
        }

        private synchronized void schedule() {
            if ((future == null || !future.isCancelled()) && !stopped.get()) {
                future = executorService.schedule(this, poolConfiguration.getReaperInterval(), TimeUnit.MILLISECONDS);
            }
        }

        private synchronized void terminate() {
            if (future != null) {
                future.cancel(true);
            }
        }

        private synchronized void force() {
            terminate();
            future = null;
            future = executorService.schedule(this, 0, TimeUnit.MILLISECONDS);
        }
    }


    private class ValidationTask implements Runnable {

        private Future<?> future;

        @Override
        public void run() {
            try {
                validateConnections();
            } finally {
                schedule();
            }
        }

        private synchronized void schedule() {
            if ((future == null || !future.isCancelled()) && !stopped.get()
                    && poolConfiguration.isBackgroundValidation() && poolConfiguration.getBackgroundValidationInterval() > 0) {
                future = executorService.schedule(this, poolConfiguration.getBackgroundValidationInterval(), TimeUnit.MILLISECONDS);
            }
        }

        private synchronized void terminate() {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    private void validateConnections() {
        try {
            boolean refill = false;
            if (permits.tryAcquire(0, TimeUnit.MILLISECONDS)) {
                try {
                    for (;;) {
                        if (stopped.get()) {
                            break;
                        }
                        DatabaseConnection dc = null;
                        boolean destroyed = false;

                        synchronized (availablePool) {
                            if (availablePool.size() == 0) {
                                break;
                            }

                            for (Iterator<DatabaseConnection> iter = availablePool.iterator(); iter.hasNext(); ) {
                                DatabaseConnection conn = iter.next();
                                if ((System.currentTimeMillis() - conn.getLastValidatedTime()) >= poolConfiguration.getBackgroundValidationInterval()) {
                                    iter.remove();
                                    dc = conn;
                                    break;
                                }
                            }
                        }

                        if (dc == null) {
                            break;
                        }

                        try {
                            if (!validateConnection(dc)) {
                                destroyed = refill = true;
                                dc.terminateConnection();
                            }
                        } finally {
                            if (!destroyed) {
                                synchronized (availablePool) {
                                    dc.setLastValidatedTime(System.currentTimeMillis());
                                    availablePool.add(dc);
                                }
                            }
                        }
                    }
                } finally {
                    permits.release();

                    if (refill) {
                        fillPoolToMin(poolConfiguration.isFillPoolAsync());
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
