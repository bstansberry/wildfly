/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.security.common;

import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.UnsuccessfulOperationException;

// TODO consider a WildFly Core testsuite/shared variant of this and delegate to it.
// Only do that if WildFly Core would use it in its own testing
public final class SecureExpressionUtil {

    private static final PathAddress SUBSYSTEM = PathAddress.pathAddress("subsystem", "elytron");
    private static final PathAddress EXPRESSION_RESOLVER = SUBSYSTEM.append("expression", "encryption");

    public static final class SecureExpressionData {
        private final String clearText;
        private final String systemProperty;
        private String expression;

        public SecureExpressionData(String clearText, String systemProperty) {
            assert clearText != null : "clearText is null";
            this.clearText = clearText;
            this.systemProperty = systemProperty;
        }

        public String getExpression() {
            return expression;
        }
    }

    // TODO if there's no need for a WildFly Core variant of this, drop this and convert everything to
    // use the Arquillian ManagementClient directly
    public static ManagementClient getCoreManagmentClient(org.jboss.as.arquillian.container.ManagementClient arquillianClient) {
        return new ManagementClient(arquillianClient.getControllerClient(), arquillianClient.getMgmtAddress(), arquillianClient.getMgmtPort(), arquillianClient.getMgmtProtocol());
    }

    public static void setupCredentialStoreExpressions(ManagementClient client, String storeName,
                                                       Set<SecureExpressionData> toConfigure)
            throws Exception {
        // Add store
        PathAddress storeAddress = SUBSYSTEM.append("secret-key-credential-store", storeName);
        ModelNode storeAdd = Util.createAddOperation(storeAddress);
        storeAdd.get("path").set(storeName + ".cs");
        storeAdd.get("relative-to").set("jboss.server.config.dir");
        client.executeForResult(storeAdd);

        // Add expression-resolver
        ModelNode exprsAdd = Util.createAddOperation(EXPRESSION_RESOLVER);
        ModelNode resolver = new ModelNode();
        resolver.get("name").set(storeName);
        resolver.get("credential-store").set(storeName);
        resolver.get("secret-key").set("key");
        exprsAdd.get("resolvers").add(resolver);
        client.executeForResult(exprsAdd);

        // Create expressions and add system properties if wanted
        for (SecureExpressionData data : toConfigure) {
            // Create expression
            ModelNode create = Util.createEmptyOperation("create-expression", EXPRESSION_RESOLVER);
            create.get("resolver").set(storeName);
            create.get("clear-text").set(data.clearText);
            ModelNode result = client.executeForResult(create);
            data.expression = result.get("expression").asString();
            if (data.systemProperty != null && !data.systemProperty.isEmpty()) {
                // Add system property that will resolve to the expression
                ModelNode add = Util.createAddOperation(PathAddress.pathAddress("system-property", data.systemProperty));
                add.get("value").set(data.expression);
                client.executeForResult(add);
            }
        }

        ServerReload.reloadIfRequired(client.getControllerClient());
    }

    public static void teardownCredentialStoreExpressions(ManagementClient client, String storeName) throws Exception {
        teardownCredentialStoreExpressions(client, storeName, null);
    }

    public static void teardownCredentialStoreExpressions(ManagementClient client, String storeName,
                                                          Set<SecureExpressionData> toClean) throws Exception {
        UnsuccessfulOperationException toThrow = null;
        try {
            // Remove expression-resolver
            client.executeForResult(Util.createRemoveOperation(EXPRESSION_RESOLVER));
        } catch (UnsuccessfulOperationException uoe) {
            toThrow = uoe;
        } catch (RuntimeException re) {
            toThrow = new UnsuccessfulOperationException(re.toString());
        } finally {
            try {
                // Remove store
                client.executeForResult(Util.createRemoveOperation(SUBSYSTEM.append("secret-key-credential-store", storeName)));
            } catch (UnsuccessfulOperationException uoe) {
                if (toThrow == null) {
                    toThrow = uoe;
                }
            } catch (RuntimeException re) {
                if (toThrow == null) {
                    toThrow = new UnsuccessfulOperationException(re.toString());
                }
            } finally {
                if (toClean != null) {
                    for (SecureExpressionData data : toClean) {
                        if (data.systemProperty != null && !data.systemProperty.isEmpty()) {
                            try {
                                // remove system property
                                client.executeForResult(Util.createRemoveOperation(PathAddress.pathAddress("system-property", data.systemProperty)));
                            } catch (UnsuccessfulOperationException uoe) {
                                if (toThrow == null) {
                                    toThrow = uoe;
                                }
                            } catch (RuntimeException re) {
                                if (toThrow == null) {
                                    toThrow = new UnsuccessfulOperationException(re.toString());
                                }
                            }
                            // remove system property
                        }
                    }
                }

            }
        }

        if (toThrow != null) {
            throw toThrow;
        }

        ServerReload.reloadIfRequired(client.getControllerClient());
    }
}
