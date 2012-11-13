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

import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
/**
 * The Database connection manager maintain the Database connections. It can operate either it
 * "datasource mode" where it utilizes the data source sub system and look up the database driver through
 * JNDI. Or in JDBC connection mode where it connect to a database through JDBC driver interface.
 *
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionManagerService implements Service<ConnectionManager> {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "connection_manager");

    private AbstractDatabaseConnectionManager connectionManager;
    private final InjectedValue<DataSource> dataSource = new InjectedValue<DataSource>();
    private final InjectedValue<PoolConfiguration> poolConfig = new InjectedValue<PoolConfiguration>();
    private final InjectedValue<ScheduledExecutorService> executorService = new InjectedValue<ScheduledExecutorService>();
    private final boolean useDataSource;

    public DatabaseConnectionManagerService(boolean useDataSource) {
        this.useDataSource = useDataSource;
    }

    /*
    *  Service Lifecycle Methods
    */

    public synchronized void start(StartContext context) throws StartException {

        if (useDataSource) {
            connectionManager = new DatasourceConnectionManager(getDatasource().getValue());
        } else {
            connectionManager = new DatabaseConnectionPool(poolConfig.getValue(), executorService.getValue());
        }
        connectionManager.start();
    }

    public synchronized void stop(final StopContext context) {
        if (useDataSource) {
            connectionManager.stop();
        } else {
            context.asynchronous();
            executorService.getValue().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        connectionManager.stop();
                    } finally {
                        context.complete();
                    }
                }
            });
        }
    }

    public synchronized ConnectionManager getValue() throws IllegalStateException, IllegalArgumentException {
        return connectionManager;
    }

    public InjectedValue<DataSource> getDatasource() {
        return dataSource;
    }

    public InjectedValue<PoolConfiguration> getPoolConfig() {
        return poolConfig;
    }

    public InjectedValue<ScheduledExecutorService> getExecutorService() {
        return executorService;
    }

}
