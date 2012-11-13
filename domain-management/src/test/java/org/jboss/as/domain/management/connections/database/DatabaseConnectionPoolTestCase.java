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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *  Database Connection Pool Tests
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionPoolTestCase {

    private static Class<? extends Driver> driverClass;
    private static ScheduledExecutorService executorService;

    @BeforeClass
    public static void getDriverClass() throws ClassNotFoundException {
        driverClass = Class.forName("org.h2.Driver").asSubclass(Driver.class);
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterClass
    public static void clearDriver() {
        driverClass = null;
        executorService.shutdown();
    }

    private PoolConfiguration poolConfiguration;
    private DatabaseConnectionPool connectionPool;

    @After
    public void terminate() throws SQLException {
        if (connectionPool != null) {
            connectionPool.stop();
        }
        connectionPool = null;
    }

    @Test
    public void testCloseConnections() throws Exception {
        initDefaultPool();
        DatabaseConnection connection = (DatabaseConnection) connectionPool.getConnection();
        connectionPool.stop();
        assertFalse(connection.isInUse());
        assertEquals(FallibleConnection.State.DESTROYED, connection.getState());
        try {
            connection.getConnection();
            fail("Underlying connection still accessible after pool is stopped");
        } catch (IllegalStateException ignored) {
            // good
        }
    }

    @Test
    public void testGetConnection() throws Exception {
        initDefaultPool();
        FallibleConnection connection = connectionPool.getConnection();
        assertEquals(FallibleConnection.State.NORMAL, connection.getState());
    }

    @Test
    public void testReturnConnection() throws Exception {
        initDefaultPool();
        DatabaseConnection connection = (DatabaseConnection) connectionPool.getConnection();
        connectionPool.returnConnection(connection);
        assertEquals(false,connection.isInUse());
    }

    @Test
    public void testMaxPoolSize() throws Exception {

        initDefaultPool();

        connectionPool.getConnection();
        connectionPool.getConnection();
        try {
            connectionPool.getConnection();
            fail("Expect the getConnection throw a timeout because of timeout on acquire lock");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testReaper() throws Exception {

        poolConfiguration = getPoolConfig(2, 5);
        poolConfiguration.setBlockingTimeout(100);
        poolConfiguration.setConnectionIdleTimeout(100);
        poolConfiguration.setReaperInterval(10);

        connectionPool = new DatabaseConnectionPool(poolConfiguration, Executors.newScheduledThreadPool(1));
        connectionPool.start();

        connectionPool.getConnection();
        connectionPool.getConnection();
        FallibleConnection connection = connectionPool.getFallibleConnection();
        Connection conn = connection.getConnection();
        connection.recordFailureOnConnection();
        conn.close();
        Thread.sleep(500);
        assertEquals(2,connectionPool.getCurrentPoolSize());

    }

    @Test
    public void testReaperConnectionInUse() throws Exception {

        poolConfiguration = getPoolConfig(2, 5);
        poolConfiguration.setBlockingTimeout(100);
        poolConfiguration.setConnectionIdleTimeout(100);
        poolConfiguration.setReaperInterval(100);

        connectionPool = new DatabaseConnectionPool(poolConfiguration, Executors.newScheduledThreadPool(1));
        connectionPool.start();

        connectionPool.getConnection();
        connectionPool.getConnection();
        connectionPool.getConnection();
        Thread.sleep(1000);
        assertEquals(3,connectionPool.getCurrentPoolSize());

    }

    /**
     * Test a connection dosen't get reap just after it return to the pool
     * and just before it get's claim again
     * @throws Exception
     */
    @Test
    public void testReaperConnectionRacecondition() throws Exception {

        poolConfiguration = getPoolConfig(1, 2);
        poolConfiguration.setBlockingTimeout(100);
        poolConfiguration.setConnectionIdleTimeout(10);
        poolConfiguration.setReaperInterval(Long.MAX_VALUE);
        connectionPool = new DatabaseConnectionPool(poolConfiguration, Executors.newScheduledThreadPool(1));
        connectionPool.start();

        connectionPool.getConnection();
        DatabaseConnection connection = (DatabaseConnection) connectionPool.getFallibleConnection();
        Thread.sleep(20); //make sure is lease time is older then 10 milliseconds so the reaper might terminate it
        connectionPool.returnConnection(connection);
        connectionPool.forceReaper();
        Thread.sleep(40); //let the reaper finish
        int poolSize = connectionPool.getCurrentPoolSize();
        assertEquals(2, poolSize);


    }

    private void initDefaultPool() throws Exception {
        poolConfiguration = getPoolConfig(1 ,2);
        poolConfiguration.setBlockingTimeout(500);
        poolConfiguration.setConnectionIdleTimeout(500);
        poolConfiguration.setReaperInterval(500);

        connectionPool = new DatabaseConnectionPool(poolConfiguration, Executors.newScheduledThreadPool(1));
        connectionPool.start();
    }

    private static PoolConfiguration getPoolConfig(int minSize, int maxSize) throws IllegalAccessException, InstantiationException {
        return new PoolConfiguration("test", driverClass.newInstance(), "jdbc:h2:mem:databaseauthtest", "sa", "sa", minSize, maxSize, false);
    }
}
