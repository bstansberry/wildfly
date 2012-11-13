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

import static org.junit.Assert.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of {@link JDBCConnectionFactory}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class JDBCConnectionFactoryUnitTestCase {

    private static Class<? extends Driver> driverClass;
    private static ScheduledExecutorService executorService;

    private static OperationContext operationContext;

    @BeforeClass
    public static void getDriverClass() throws ClassNotFoundException {
        driverClass = Class.forName("org.h2.Driver").asSubclass(Driver.class);
        executorService = Executors.newSingleThreadScheduledExecutor();

        operationContext = (OperationContext) Proxy.newProxyInstance(JDBCConnectionFactoryUnitTestCase.class.getClassLoader(),
                new Class<?>[]{OperationContext.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return args[0];
            }
        });
    }

    @AfterClass
    public static void clearDriver() {
        driverClass = null;
        executorService.shutdown();
    }

    private ModelNode config;

    @Before
    public void before() {
        TestValidConnectionChecker.reset();

        config = new ModelNode();
        config.get(DatabaseConnectionResourceDefinition.CONNECTION_URL.getName()).set("jdbc:h2:mem:databaseauthtest");
        config.get(DatabaseConnectionResourceDefinition.USERNAME.getName()).set("sa");
        config.get(DatabaseConnectionResourceDefinition.PASSWORD.getName()).set("sa");
    }

    @After
    public void after() {

        try {
            Driver driver = (Driver) Class.forName("org.h2.Driver", true, this.getClass().getClassLoader()).newInstance();
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

    @Test
    public void testSimpleConnectionURL() throws Exception {

        JDBCConnectionFactory testee = getTestee();
        Connection conn = testee.createJDBCConnection();
        conn.close();

        config.get(DatabaseConnectionResourceDefinition.CONNECTION_URL.getName()).set("bogus");
        testee = getTestee();
        try {
            conn = testee.createJDBCConnection();
            conn.close();
            fail("bogus connection url didn't fail");
        } catch (Exception good) {
            // woohoo
        }
    }

    @Test
    public void testHAConnectionURLs() throws Exception {
        config.get(DatabaseConnectionResourceDefinition.CONNECTION_URL.getName()).set("bogus;wrong;jdbc:h2:mem:databaseauthtest");
        config.get(DatabaseConnectionResourceDefinition.URL_DELIMITER.getName()).set(";");
        JDBCConnectionFactory testee = getTestee();
        Connection conn = testee.createJDBCConnection();
        conn.close();

        config.get(DatabaseConnectionResourceDefinition.CONNECTION_URL.getName()).set("bogus;wrong");
        testee = getTestee();
        try {
            conn = testee.createJDBCConnection();
            conn.close();
            fail("bogus connection url didn't fail");
        } catch (Exception good) {
            // woohoo
        }
    }

    @Test
    public void testNewConnectionSQL() throws Exception {
        config.get(DatabaseConnectionResourceDefinition.NEW_CONNECTION_SQL.getName()).set("SELECT 1");
        JDBCConnectionFactory testee = getTestee();
        Connection conn = testee.createJDBCConnection();
        conn.close();

        config.get(DatabaseConnectionResourceDefinition.NEW_CONNECTION_SQL.getName()).set("SELECT bogus FROM wrong");
        testee = getTestee();
        try {
            conn = testee.createJDBCConnection();
            conn.close();
            fail("bogus new connection sql didn't fail");
        } catch (Exception good) {
            // woohoo
        }
    }

    @Test
    public void testCheckConnectSQL() throws Exception {
        config.get(DatabaseConnectionResourceDefinition.CHECK_VALID_CONNECTION_SQL.getName()).set("SELECT 1");
        JDBCConnectionFactory testee = getTestee();
        Connection conn = testee.createJDBCConnection();
        try {
            assertNull(testee.validateConnection(conn));
        } finally {
            conn.close();
        }

        config.get(DatabaseConnectionResourceDefinition.CHECK_VALID_CONNECTION_SQL.getName()).set("SELECT bogus FROM wrong");
        testee = getTestee();
        conn = testee.createJDBCConnection();
        try {
            assertNotNull(testee.validateConnection(conn));
        } finally {
            conn.close();
        }
    }

    @Test
    public void testValidationCheckerProperties() throws Exception {

        assertNull(TestValidConnectionChecker.string);
        assertFalse(TestValidConnectionChecker.bool);
        assertEquals(0, TestValidConnectionChecker.integer);

        config.get(DatabaseConnectionResourceDefinition.VALID_CONNECTION_CHECKER_CLASS_NAME.getName()).set(TestValidConnectionChecker.class.getCanonicalName());
        ModelNode props = config.get(DatabaseConnectionResourceDefinition.VALID_CONNECTION_CHECKER_PROPERTIES.getName());
        props.add("string", "string");
        props.add("boolean", "true");
        props.add("integer", "10");

        JDBCConnectionFactory testee = getTestee();

        assertEquals("string", TestValidConnectionChecker.string);
        assertTrue(TestValidConnectionChecker.bool);
        assertEquals(10, TestValidConnectionChecker.integer);

        Connection conn = testee.createJDBCConnection();
        try {
            assertNotNull(testee.validateConnection(conn));
        } finally {
            conn.close();
        }

        TestValidConnectionChecker.reset();

        props.set(new ModelNode());
        props.add("string", "string");
        props.add("integer", "10");

        testee = getTestee();

        assertEquals("string", TestValidConnectionChecker.string);
        assertFalse(TestValidConnectionChecker.bool);
        assertEquals(10, TestValidConnectionChecker.integer);

        conn = testee.createJDBCConnection();
        try {
            assertNull(testee.validateConnection(conn));
        } finally {
            conn.close();
        }
    }

    private JDBCConnectionFactory getTestee() throws OperationFailedException, IllegalAccessException, InstantiationException {
        PoolConfiguration poolConfiguration = new PoolConfiguration("test", driverClass.newInstance(), operationContext, config);
        return new JDBCConnectionFactory(poolConfiguration);
    }
}
