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

import java.io.ObjectInputStream;
import java.sql.Connection;

/**
 * Holder for a database connection that allows the user of the connection to record any failures
 * associated with the connection. This is a simple expediency to avoid requiring complex internal tracking of
 * failures by the {@code Connection} implementation and its derived statements and result sets.
 * Instead, the requirement is the user of the connection will invoke {@link #recordFailureOnConnection()} if
 * any exception occurs during use of the connection. Note that it is possible the underlying {@code Connection}
 * will in fact track failures on its own, but users should not assume this will be done.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */

public interface FallibleConnection {

    enum State {
        /** Normal state for a connection */
        NORMAL,
        /** The connection has been marked for destruction, either due to a
         * {@link FallibleConnection#recordFailureOnConnection() failure} or due to some background cleanup  */
        DESTROY,
        /** The connection has been destroyed */
        DESTROYED
    }

    /**
     * Gets the underlying {@code Connection}
     * @return the connection
     *
     * @throws IllegalStateException if {@link #getState()} would not return {@link State#NORMAL}
     */
    Connection getConnection();

    /**
     * Record that a failure associated with use of this connection has occurred, marking it for
     * {@link State#DESTROY destruction}.
     */
    void recordFailureOnConnection();

    /**
     * Gets the current state.
     *
     * @return  the state
     */
    State getState();
}
