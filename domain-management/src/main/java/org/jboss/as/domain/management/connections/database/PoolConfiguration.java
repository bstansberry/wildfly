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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.util.Properties;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Configuration object for a {@link DatabaseConnectionPool}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class PoolConfiguration {

    public static final long MAX_DELAY_FOR_REAPER_REQUEST = 300000;

    private final String poolName;
    private final Driver driver;
    private final String connectionURL;
    private final String urlDelimiter;
    private final Properties connectionProperties = new Properties();
    private final String user;
    private final String password;
    private volatile int minPoolSize;
    private final int maxPoolSize;
    private final boolean fillPoolAsync;
    private final boolean prefill;
    private final String validConnectionCheckerModule;
    private final String validConnectionCheckerClassName;
    private final Properties validConnectionCheckerProps = new Properties();
    private volatile long blockingTimeout = DatabaseConnectionResourceDefinition.BLOCKING_TIMEOUT_WAIT_MILLIS.getDefaultValue().asLong();
    private volatile long connectionIdleTimeout = DatabaseConnectionResourceDefinition.IDLE_TIMEOUT_MINUTES.getDefaultValue().asLong();
    private volatile long reaperInterval = MAX_DELAY_FOR_REAPER_REQUEST;
    private volatile boolean strictMin;
    private boolean useFastFail;
    private long backgroundValidationInterval;
    private boolean backgroundValidation;
    private String newConnectionCheckSQL;
    private String checkValidConnectionSQL;

    public PoolConfiguration(final String name, final OperationContext context, final ModelNode model) throws OperationFailedException {
        this(name, loadDriver(context, model), context, model);
    }

    /** Constructor for unit tests, to avoid having to load the driver via a module */
    PoolConfiguration(String name, Driver driver, final OperationContext context, final ModelNode model) throws OperationFailedException {

        poolName = name;
        this.driver = driver;
        fillPoolAsync = true;

        connectionURL = DatabaseConnectionResourceDefinition.CONNECTION_URL.resolveModelAttribute(context, model).asString();
        ModelNode delimiter = DatabaseConnectionResourceDefinition.URL_DELIMITER.resolveModelAttribute(context, model);
        urlDelimiter = delimiter.isDefined() ? delimiter.asString() : null;

        if (model.hasDefined(ModelDescriptionConstants.CONNECTION_PROPERTIES)) {
            for (Property prop : model.get(ModelDescriptionConstants.CONNECTION_PROPERTIES).asPropertyList()) {
                String val = ConnectionPropertyResourceDefinition.CONNECTION_PROPERTY_VALUE.resolveModelAttribute(context, prop.getValue()).asString();
                connectionProperties.setProperty(prop.getName(), val);
            }
        }

        ModelNode sql = DatabaseConnectionResourceDefinition.NEW_CONNECTION_SQL.resolveModelAttribute(context, model);
        newConnectionCheckSQL = sql.isDefined() ? sql.asString() : null;

        maxPoolSize = DatabaseConnectionResourceDefinition.MAX_POOL_SIZE.resolveModelAttribute(context, model).asInt();
        // Use setter method to check invariant
        setMinSize(DatabaseConnectionResourceDefinition.MIN_POOL_SIZE.resolveModelAttribute(context, model).asInt());
        prefill = DatabaseConnectionResourceDefinition.POOL_PREFILL.resolveModelAttribute(context, model).asBoolean();
        strictMin = DatabaseConnectionResourceDefinition.POOL_USE_STRICT_MIN.resolveModelAttribute(context, model).asBoolean();
        blockingTimeout = DatabaseConnectionResourceDefinition.BLOCKING_TIMEOUT_WAIT_MILLIS.resolveModelAttribute(context, model).asLong();
        connectionIdleTimeout = DatabaseConnectionResourceDefinition.IDLE_TIMEOUT_MINUTES.resolveModelAttribute(context, model).asLong();
        useFastFail = DatabaseConnectionResourceDefinition.USE_FAST_FAIL.resolveModelAttribute(context, model).asBoolean();

        ModelNode uname = DatabaseConnectionResourceDefinition.USERNAME.resolveModelAttribute(context, model);
        user = uname.isDefined() ? uname.asString() : null;
        ModelNode pword = DatabaseConnectionResourceDefinition.PASSWORD.resolveModelAttribute(context, model);
        password = pword.isDefined() ? pword.asString() : null;

        backgroundValidation = DatabaseConnectionResourceDefinition.BACKGROUND_VALIDATION.resolveModelAttribute(context, model).asBoolean();
        backgroundValidationInterval= DatabaseConnectionResourceDefinition.BACKGROUND_VALIDATION_MILLIS.resolveModelAttribute(context, model).asLong();

        sql = DatabaseConnectionResourceDefinition.CHECK_VALID_CONNECTION_SQL.resolveModelAttribute(context, model);
        checkValidConnectionSQL = sql.isDefined() ? sql.asString() : null;

        ModelNode checkerModule = DatabaseConnectionResourceDefinition.VALID_CONNECTION_CHECKER_MODULE.resolveModelAttribute(context, model);
        validConnectionCheckerModule = checkerModule.isDefined() ? checkerModule.asString() : null;
        ModelNode checkerClass = DatabaseConnectionResourceDefinition.VALID_CONNECTION_CHECKER_CLASS_NAME.resolveModelAttribute(context, model);
        validConnectionCheckerClassName = checkerClass.isDefined() ? checkerClass.asString() : null;

        if (model.hasDefined(ModelDescriptionConstants.VALID_CONNECTION_CHECKER_PROPERTIES)) {
            for (Property prop : model.get(ModelDescriptionConstants.VALID_CONNECTION_CHECKER_PROPERTIES).asPropertyList()) {
                validConnectionCheckerProps.setProperty(prop.getName(), prop.getValue().asString());
            }
        }

    }

    /** Constructor for unit tests */
    public PoolConfiguration(String name, Driver driver, String url, String user, String password, int minPoolSize, int maxPoolSize, boolean fillPoolAsync) {
        this.poolName = name;
        this.driver = driver;
        this.fillPoolAsync = fillPoolAsync;
        this.connectionURL = url;
        this.urlDelimiter = null;
        this.user = user;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        // Use setter method to check invariant
        setMinSize(minPoolSize);
        this.prefill = DatabaseConnectionResourceDefinition.POOL_PREFILL.getDefaultValue().asBoolean();
        this.validConnectionCheckerClassName = null;
        this.validConnectionCheckerModule = null;
    }

    public Driver getDriver() {
        return driver;
    }

    public int getMaxSize() {
        int max = maxPoolSize;
        int min = minPoolSize;
        return Math.max(min, max);
    }

    public String getConnectionURL() {

        return connectionURL;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getMinSize() {
        return minPoolSize;
    }

    public void setMinSize(int size) {
        if (size > maxPoolSize) {
            throw DomainManagementMessages.MESSAGES.minSizeCannotBeGreaterThanMaxSize(size, maxPoolSize);
        }
        this.minPoolSize = size;
    }

    public long getBlockingTimeout() {
        return blockingTimeout;
    }

    public void setBlockingTimeout(long blockingTimeout) {
        this.blockingTimeout = blockingTimeout;
    }

    public long getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }

    public void setConnectionIdleTimeout(long connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    public boolean isFillPoolAsync() {
        return fillPoolAsync;
    }

    public long getReaperInterval() {
        return reaperInterval;
    }

    public void setReaperInterval(long reaperInterval) {
        this.reaperInterval = reaperInterval;
    }

    public boolean isPrefill() {
        return prefill;
    }

    public boolean isStrictMin() {
        return strictMin;
    }

    public void setStrictMin(boolean strictMin) {
        this.strictMin = strictMin;
    }

    public boolean isFillToMinSupported() {
        return (prefill || strictMin) && minPoolSize > 0;
    }

    public boolean isUseFastFail() {
        return useFastFail;
    }

    public void setUseFastFail(boolean useFastFail) {
        this.useFastFail = useFastFail;
    }

    public String getPoolName() {
        return poolName;
    }

    public long getBackgroundValidationInterval() {
        return backgroundValidationInterval;
    }

    public boolean isValidateOnIssue() {
        return newConnectionCheckSQL != null;
    }

    public boolean isBackgroundValidation() {
        return backgroundValidation;
    }

    public String getURLDelimiter() {
        return urlDelimiter;
    }

    public Properties getConnectionProperties() {
        return (Properties) connectionProperties.clone();
    }

    public String getNewConnectionCheckSQL() {
        return newConnectionCheckSQL;
    }

    public String getCheckValidConnectionSQL() {
        return checkValidConnectionSQL;
    }

    public String getValidConnectionCheckerModule() {
        return validConnectionCheckerModule;
    }

    public String getValidConnectionCheckerClassName() {
        return validConnectionCheckerClassName;
    }

    public Properties getValidConnectionCheckerProps() {
        return (Properties) validConnectionCheckerProps.clone();
    }

    private static Driver loadDriver(final OperationContext context, ModelNode model) throws OperationFailedException {

        String module = DatabaseConnectionResourceDefinition.DRIVER_MODULE_NAME.resolveModelAttribute(context, model).asString();
        String driverClassName = DatabaseConnectionResourceDefinition.DRIVER_CLASS_NAME.resolveModelAttribute(context, model).asString();
        try {

            ModuleIdentifier moduleId = ModuleIdentifier.create(module);
            Module loadModule = Module.getCallerModuleLoader().loadModule(moduleId);

            final Class<? extends Driver> driverClass = loadModule.getClassLoader().loadClass(driverClassName).asSubclass(Driver.class);
            final Constructor<? extends Driver> constructor = driverClass.getConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException e) {
            throw MESSAGES.jdbcDriverClassNotFoundException(e, driverClassName, module);
        } catch (Exception e) {
            throw MESSAGES.jdbcNotLoadedException(e, driverClassName);
        }

    }
}
