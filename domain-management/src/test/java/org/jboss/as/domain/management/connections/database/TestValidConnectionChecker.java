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

import org.jboss.jca.adapters.jdbc.spi.ValidConnectionChecker;

/**
 * Mock impl of {@link ValidConnectionChecker}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class TestValidConnectionChecker implements ValidConnectionChecker {

    static String string;
    static boolean bool;
    static int integer;

    static void reset() {
        string = null;
        bool = false;
        integer = 0;
    }

    @Override
    public SQLException isValidConnection(Connection c) {
        return bool ? new SQLException("No soup for you!") : null;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        TestValidConnectionChecker.string = string;
    }

    public boolean isBoolean() {
        return bool;
    }

    public void setBoolean(boolean bool) {
        TestValidConnectionChecker.bool = bool;
    }

    public int getInteger() {
        return integer;
    }

    public void setInteger(int integer) {
        TestValidConnectionChecker.integer = integer;
    }
}
