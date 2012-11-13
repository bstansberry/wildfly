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

import java.sql.SQLException;

import org.jboss.as.domain.management.connections.ConnectionManager;

/**
 * Abstract superclass for database-based {@link ConnectionManager} implementations.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public abstract class AbstractDatabaseConnectionManager implements ConnectionManager {

    @Override
    public FallibleConnection getConnection() throws SQLException, InterruptedException {
        ClassLoader original = null;
        try {
            original = Thread.currentThread().getContextClassLoader();
            if (original != null) {
                Thread.currentThread().setContextClassLoader(null);
            }
            return getFallibleConnection();
        } finally {
            if (original != null) {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

    @Override
    public FallibleConnection getConnection(String principal, String credential) {
        throw new UnsupportedOperationException();
    }

    protected abstract FallibleConnection getFallibleConnection() throws SQLException, InterruptedException;

    protected abstract void start();

    protected abstract void stop();
}
