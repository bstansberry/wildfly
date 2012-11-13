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
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jboss.as.domain.management.connections.ConnectionManager;

/**
 * {@link ConnectionManager} backed by a {@link DataSource}. This implementation makes no attempt
 * to pass information about {@link FallibleConnection#recordFailureOnConnection() failures} back to the
 * datasource, relying instead on the datasource's own connection monitoring mechanisms.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class DatasourceConnectionManager extends AbstractDatabaseConnectionManager {

    private final DataSource dataSource;

    DatasourceConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected FallibleConnection getFallibleConnection() throws SQLException {
        final Connection conn = dataSource.getConnection();
        return new AbstractFallibleConnection() {
            @Override
            protected Connection getUnderlyingConnection() {
                return conn;
            }
        };
    }

    @Override
    protected void start() {
        // no-op
    }

    @Override
    protected void stop() {
        // no-op
    }
}
