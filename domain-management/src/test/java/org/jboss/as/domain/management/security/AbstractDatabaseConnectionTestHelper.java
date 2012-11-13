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
package org.jboss.as.domain.management.security;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.as.domain.management.connections.database.DatabaseConnectionPool;
import org.jboss.as.domain.management.connections.database.PoolConfiguration;
import org.jboss.sasl.util.UsernamePasswordHashUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *  Test helper for setting up the the {@link ConnectionManager}
 *  and create the proper tables and test data
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public abstract class AbstractDatabaseConnectionTestHelper {

    protected static final String TEST_REALM = "TestRealm";
    protected static UsernamePasswordHashUtil hashUtil;
    protected static String hashedPassword;
    protected static TestDatabaseConnectionPool connectionPool;
    private static ScheduledExecutorService executorService;

    @BeforeClass
    public static void initDatabase() throws Exception {
        hashUtil = new UsernamePasswordHashUtil();
        hashedPassword = hashUtil.generateHashedHexURP("Henry.Deacon",TEST_REALM,"eureka".toCharArray());

        Class<? extends Driver> driverClass = Class.forName("org.h2.Driver").asSubclass(Driver.class);

        PoolConfiguration configuration = new PoolConfiguration("test", driverClass.newInstance(), "jdbc:h2:mem:databaseauthtest", "sa", "sa", 1, 2, false);
        configuration.setBlockingTimeout(500);
        configuration.setConnectionIdleTimeout(500);
        configuration.setReaperInterval(500);

        executorService = Executors.newSingleThreadScheduledExecutor();

        connectionPool = new TestDatabaseConnectionPool(configuration);

        initTables(connectionPool);
    }

    @AfterClass
    public static void terminateDatabase() throws Exception {
        connectionPool.stop();
        connectionPool = null;
        executorService.shutdown();
    }

    @Before
    public void init() throws Exception {
        initAuthenticationModel(true);
        initCallbackHandler(connectionPool);
    }

    private static void initTables(TestDatabaseConnectionPool connectionPool) throws Exception {
        Connection connection = connectionPool.getConnection().getConnection();
        Statement statement = connection.createStatement();
        statement.addBatch("CREATE TABLE USERS(user VARCHAR(32) PRIMARY KEY,   password VARCHAR(255));");
        statement.addBatch("CREATE TABLE ROLES(user VARCHAR(32),   roles VARCHAR(255));");
        statement.addBatch("insert into users values('Jack.Carter','eureka')");
        statement.addBatch("insert into users values('Henry.Deacon','"+hashedPassword+"')");
        statement.addBatch("insert into roles values('Jack.Carter','sheriff,dad,lifesaver')");
        statement.addBatch("insert into roles values('Henry.Deacon','')");
        statement.addBatch("insert into roles values('Christopher.Chance','buggydata;¤¤¤%,,')");
        statement.addBatch("insert into roles values('Jo Lupo','deputy')");
        statement.addBatch("insert into roles values('Jo Lupo','head-of-security')");
        statement.executeBatch();
        statement.close();
        connection.close();
    }

    @AfterClass
    public static void cleanDatabase() {

        try {
            Driver driver = (Driver) Class.forName("org.h2.Driver", true, AbstractDatabaseConnectionTestHelper.class.getClassLoader()).newInstance();
            Properties props = new Properties();
            props.setProperty("user", "sa");
            props.setProperty("password", "sa");
            Connection conn = driver.connect("jdbc:h2:mem:databaseauthtest;DB_CLOSE_DELAY=-1", props);
            conn.createStatement().execute("DROP ALL OBJECTS");
            conn.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Setup the model for the database authentication / authorization
     * @param plainPassword the password
     */
    abstract void initAuthenticationModel(boolean plainPassword);

    static class TestDatabaseConnectionPool extends DatabaseConnectionPool {

        TestDatabaseConnectionPool(PoolConfiguration poolConfiguration) {
            super(poolConfiguration, executorService);
        }
    }

    /**
     * Setup up the proper callback handler for your test
     * @param connectionManager the connection manager
     */
    abstract void initCallbackHandler(ConnectionManager connectionManager) throws Exception;
}
