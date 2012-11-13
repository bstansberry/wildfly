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

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

/**
 * The Database connection.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
final class DatabaseConnection extends AbstractFallibleConnection {

    private final DatabaseConnectionPool pool;
    private final Connection conn;
    private final ConnectionWrapper wrapper;
    private boolean inuse;
    private long timestamp;
    private long lastValidatedTime;


    public DatabaseConnection(Connection conn, DatabaseConnectionPool pool) {
        this.conn = conn;
        this.wrapper = new ConnectionWrapper();
        this.pool = pool;
        this.inuse = false;
        this.timestamp = 0;
    }

    protected Connection getUnderlyingConnection() {
        return wrapper;
    }

    private synchronized void updateTimeStamp() {
        timestamp = System.currentTimeMillis();
    }

    @Override
    public Connection getConnection() {
        if (!inuse) {
            throw new IllegalStateException();
        }
        return super.getConnection();
    }

    synchronized void lease() {
        inuse = true;
    }

    synchronized boolean isInUse() {
        return inuse;
    }

    synchronized long getLastUse() {
        return timestamp;
    }

    void prepareToDestroy() {
        state.compareAndSet(State.NORMAL, State.DESTROY);
    }

    synchronized void terminateConnection() {
        inuse = false;
        try {
            conn.close();
        } catch (SQLException e) {
            ROOT_LOGGER.terminateConnectionException(this, e);
        } finally {
            state.set(State.DESTROYED);
        }
    }

    synchronized void expireLease() {
        updateTimeStamp();
        inuse = false;
    }

    public long getLastValidatedTime() {
        return lastValidatedTime;
    }

    void setLastValidatedTime(long lastValidatedTime) {
        this.lastValidatedTime = lastValidatedTime;
    }

    private class ConnectionWrapper implements Connection {
        @Override
        public void close() throws SQLException {
            pool.returnConnection(DatabaseConnection.this);
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return conn.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return conn.prepareCall(sql);
        }

        @Override
        public Statement createStatement() throws SQLException {
            return conn.createStatement();
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return conn.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            conn.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return conn.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            conn.commit();
        }

        @Override
        public void rollback() throws SQLException {
            conn.rollback();
        }

        @Override
        public boolean isClosed() throws SQLException {
            return conn.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return conn.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            conn.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return conn.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            conn.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return conn.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            conn.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return conn.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return conn.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            conn.clearWarnings();
        }

        @Override
        public boolean isWrapperFor(Class<?> clazz) throws SQLException {
            return conn.isWrapperFor(clazz);
        }

        @Override
        public <T> T unwrap(Class<T> clazz) throws SQLException {
            return conn.unwrap(clazz);
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return conn.createArrayOf(typeName, elements);
        }

        @Override
        public Blob createBlob() throws SQLException {
            return conn.createBlob();
        }

        @Override
        public Clob createClob() throws SQLException {
            return conn.createClob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return conn.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return conn.createSQLXML();
        }

        @Override
        public Statement createStatement(int resultSet, int resultSetConcurrency) throws SQLException {
            return conn.createStatement(resultSet,resultSetConcurrency);
        }

        @Override
        public Statement createStatement(int resultSet, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return conn.createStatement(resultSet,resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return conn.createStruct(typeName, attributes);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return conn.getClientInfo();
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return conn.getClientInfo(name);
        }

        @Override
        public int getHoldability() throws SQLException {
            return conn.getHoldability();
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return conn.getTypeMap();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return conn.isValid(timeout);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return conn.prepareStatement(sql,autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return conn.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return conn.prepareStatement(sql, columnNames);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            conn.releaseSavepoint(savepoint);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            conn.rollback(savepoint);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            conn.setClientInfo(properties);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            conn.setClientInfo(name, value);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            conn.setHoldability(holdability);
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return conn.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return conn.setSavepoint(name);
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            conn.setTypeMap(map);
        }
    }
}
