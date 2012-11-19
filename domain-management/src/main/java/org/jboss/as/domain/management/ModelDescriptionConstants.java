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

package org.jboss.as.domain.management;

/**
 * Constants specific to the Domain Management module.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ModelDescriptionConstants {

    public static final String ALIAS = "alias";
    public static final String ALLOWED_USERS = "allowed-users";
    public static final String AUTHENTICATION = "authentication";
    public static final String AUTHORIZATION = "authorization";
    public static final String BACKGROUND_VALIDATION = "background-validation";
    public static final String BACKGROUND_VALIDATION_MILLIS = "background-validation-millis";
    public static final String BLOCKING_TIMEOUT_WAIT_MILLIS = "blocking-timeout-wait-millis";
    public static final String CHECK_VALID_CONNECTION_SQL = "check-valid-connection-sql";
    public static final String CONNECTION_PROPERTIES = "connection-properties";
    public static final String CONNECTION_URL = "connection-url";
    public static final String DATABASE = "database";
    public static final String DATABASE_CONNECTION = org.jboss.as.controller.descriptions.ModelDescriptionConstants.DATABASE_CONNECTION;
    public static final String DATA_SOURCE = "data-source";
    public static final String DATA_SOURCE_JNDI_NAME = "data-source-jndi-name";
    public static final String DEFAULT_USER = "default-user";
    public static final String DEFAULT_DEFAULT_USER = "$local";
    public static final String DRIVER_MODULE_NAME = "driver-module-name";
    public static final String DRIVER_CLASS_NAME = "driver-class-name";
    public static final String IDENTITY = "identity";
    public static final String IDLE_TIMEOUT_MINUTES = "idle-timeout-minutes";
    public static final String KEY_PASSWORD = "key-password";
    public static final String KEYSTORE_PASSWORD = "keystore-password";
    public static final String KEYSTORE_PATH = "keystore-path";
    public static final String KEYSTORE_RELATIVE_TO = "keystore-relative-to";
    public static final String LOCAL = "local";
    public static final String MAX_POOL_SIZE = "max-pool-size";
    public static final String MIN_POOL_SIZE = "min-pool-size";
    public static final String MECHANISM = "mechanism";
    public static final String MODULE = org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
    public static final String NAME = "name";
    public static final String NEW_CONNECTION_SQL = "new-connection-sql";
    public static final String PASSWORD = "password";
    public static final String PASSWORD_FIELD = "password-field";
    public static final String PATH = "path";
    public static final String PLAIN_TEXT = org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLAIN_TEXT;
    public static final String PLUG_IN = "plug-in";
    public static final String POOL_PREFILL = "pool-prefill";
    public static final String POOL_USE_STRICT_MIN = "pool-use-strict-min";
    public static final String PROPERTY = "property";
    public static final String REALM = "realm";
    public static final String REF = "ref";
    public static final String RELATIVE_TO = "relative-to";
    public static final String ROLES = "roles";
    public static final String ROLES_FIELD = "roles-field";
    public static final String SECURITY = "security";
    public static final String SIMPLE_SELECT_USERS = "simple-select-users";
    public static final String SIMPLE_SELECT_ROLES = "simple-select-roles";
    public static final String SIMPLE_SELECT_TABLE = "table";
    public static final String SIMPLE_SELECT_USERNAME_FIELD = "username-field";
    public static final String SIMPLE_SELECT_USERS_PASSWORD_FIELD = "password-field";
    public static final String SQL_SELECT_USERS = "sql-select-users";
    public static final String SQL_SELECT_ROLES = "sql-select-roles";
    public static final String SQL_SELECT_USERS_ROLES_STATEMENT = "sql";
    public static final String URL_DELIMITER = "url-delimiter";
    public static final String USERNAME = "username";
    public static final String USERNAME_FIELD = "username-field";
    public static final String USE_FAST_FAIL = "use-fast-fail";
    public static final String VALID_CONNECTION_CHECKER_CLASS_NAME = "valid-connection-checker-class-name";
    public static final String VALID_CONNECTION_CHECKER_MODULE_NAME = "valid-connection-checker-module-name";
    public static final String VALID_CONNECTION_CHECKER_PROPERTIES = "valid-connection-checker-properties";
    public static final String VALIDATE_ON_MATCH = "validate-on-match";
    public static final String VALUE = "value";
    public static final String VERBOSE = "verbose";
    public static final String WHOAMI = "whoami";

    // Prevent instantiation.
    private ModelDescriptionConstants() {}
}
