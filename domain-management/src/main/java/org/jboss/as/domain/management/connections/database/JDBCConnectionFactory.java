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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.jboss.as.domain.management.DomainManagementLogger;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.jca.adapters.jdbc.CheckValidConnectionSQL;
import org.jboss.jca.adapters.jdbc.extensions.novendor.NullValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.local.URLSelector;
import org.jboss.jca.adapters.jdbc.spi.ValidConnectionChecker;
import org.jboss.jca.adapters.jdbc.util.Injection;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;

/**
 * Factory for {@link Connection} instances for use in a {@link DatabaseConnectionPool}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class JDBCConnectionFactory {

    private final PoolConfiguration poolConfiguration;
    private final URLSelector urlSelector;
    private final ValidConnectionChecker connectionChecker;

    JDBCConnectionFactory(final PoolConfiguration poolConfiguration) {
        this.poolConfiguration = poolConfiguration;
        String urlDelimiter = poolConfiguration.getURLDelimiter();
        if (urlDelimiter != null && urlDelimiter.trim().length() > 0) {
            List<String> urlList = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(poolConfiguration.getConnectionURL(), urlDelimiter);
            while (st.hasMoreTokens()) {
                urlList.add(st.nextToken());
            }
            urlSelector = new URLSelector();
            urlSelector.init(urlList);
        } else {
            urlSelector = null;
        }

        ValidConnectionChecker vcc = new NullValidConnectionChecker();
        if (poolConfiguration.getValidConnectionCheckerClassName() != null) {

            try {

                vcc = loadValidConnectionChecker(poolConfiguration.getValidConnectionCheckerModule(),
                        poolConfiguration.getValidConnectionCheckerClassName(),
                        poolConfiguration.getValidConnectionCheckerProps());
            } catch (Exception e) {
                DomainManagementLogger.DATABASE_POOL_LOGGER.failedToCreateValidConnectionChecker(poolConfiguration.getPoolName(), e);
            }
        } else if (poolConfiguration.getCheckValidConnectionSQL() != null) {
            vcc = new CheckValidConnectionSQL(poolConfiguration.getCheckValidConnectionSQL());
        }
        connectionChecker = vcc;
    }

    Connection createJDBCConnection() throws SQLException {
        Properties props = getConnectionProperties();
        return urlSelector == null ? createSimpleJDBCConnection(poolConfiguration.getConnectionURL(), props)
                : createJDBCConnectionFromURLSelector(props);
    }

    SQLException validateConnection(Connection connection) {
        return connectionChecker.isValidConnection(connection);
    }

    private Connection createJDBCConnectionFromURLSelector(Properties props) throws SQLException {
        while (urlSelector.hasMore()) {
            String url = urlSelector.active();

            Connection con = null;
            try {
                Driver driver = poolConfiguration.getDriver();
                con = poolConfiguration.getDriver().connect(url, props);
                if (con == null) {
                    DomainManagementLogger.DATABASE_POOL_LOGGER.wrongDriverClassForURL(driver.getClass(), url);
                    urlSelector.fail(url);
                } else {
                    checkNewConnection(con);
                    return con;
                }
            } catch (Exception e) {
                if (con != null) {
                    try {
                        con.close();
                    } catch (Throwable ignored) {
                        // Ignore
                    }
                }
                DomainManagementLogger.DATABASE_POOL_LOGGER.failedToCreateConnectionForURL(url, e);
                urlSelector.fail(url);
            }
        }

        urlSelector.reset();

        throw DomainManagementMessages.MESSAGES.noValidURLAvailable(urlSelector.getData());
    }

    private Connection createSimpleJDBCConnection(String connectionURL, Properties props) throws SQLException {
        Driver driver = poolConfiguration.getDriver();
        Connection conn = driver.connect(connectionURL, props);

        if (conn == null) {
            throw DomainManagementMessages.MESSAGES.wrongDriverClassForURL(driver.getClass(), connectionURL);
        }

        checkNewConnection(conn);

        return conn;
    }

    private void checkNewConnection(Connection conn) throws SQLException {

        String checkSQL = poolConfiguration.getNewConnectionCheckSQL();
        if (checkSQL != null && checkSQL.length() > 0) {
            Statement statement = conn.createStatement();
            try {
                statement.execute(checkSQL);
            } finally {
                statement.close();
            }
        }

    }

    private Properties getConnectionProperties() {
        Properties connectionProperties = poolConfiguration.getConnectionProperties();
        if (poolConfiguration.getUser() != null) {
            connectionProperties.put("user", poolConfiguration.getUser());
        }
        if (poolConfiguration.getPassword() != null) {
            connectionProperties.put("password", poolConfiguration.getPassword());
        }
        return connectionProperties;
    }

    private ValidConnectionChecker loadValidConnectionChecker(String module, String plugin, Properties props) throws Exception {

        ClassLoader cl;
        if (module == null) {
            cl = getClass().getClassLoader();
        } else {
            ModuleIdentifier mi = ModuleIdentifier.create(module);
            Module mod = Module.getModuleFromCallerModuleLoader(mi);
            cl = mod.getClassLoader();
        }
        Class<?> clz = Class.forName(plugin, true, cl);

        ValidConnectionChecker result = (ValidConnectionChecker) clz.newInstance();

        if (props != null) {
            Injection injection = new Injection();
            for (Map.Entry<Object, Object> prop : props.entrySet()) {
                injection.inject(result, (String) prop.getKey(), (String) prop.getValue());
            }
        }

        return result;
    }
}
