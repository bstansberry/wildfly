/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.health;


import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.DateUtils;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

final class HealthCheckSubsystemAdd extends AbstractAddStepHandler {

    static final HealthCheckSubsystemAdd INSTANCE = new HealthCheckSubsystemAdd();

    private static final byte[] UP = "{\"status\" : \"UP\"}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DOWN = "{\"status\" : \"DOWN\"}".getBytes(StandardCharsets.UTF_8);
    private static final ETag UP_ETAG = new ETag(true, "UP");
    private static final ETag DOWN_ETAG = new ETag(true, "DOWN");

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) {
        ServiceTarget target = context.getServiceTarget();
        CustomContextService service = new CustomContextService();
        target.addService(CustomContextService.SERVICE_NAME, service)
                .addDependency(context.getCapabilityServiceName(HealthCheckSubsystemDefinition.REQUIRED_CAP, ExtensibleHttpManagement.class),
                        ExtensibleHttpManagement.class, service.httpManagementInjector)
                .install();
    }



    private static class CustomContextService implements Service<Void> {

        private static final ServiceName SERVICE_NAME = ServiceName.of("test", "customcontext");

        private final InjectedValue<ExtensibleHttpManagement> httpManagementInjector = new InjectedValue<>();

        private CustomContextService() {
        }

        @Override
        public void start(StartContext context) {
            httpManagementInjector.getValue().addStaticContext("health", new HealthCheckResourceManager());
        }

        @Override
        public void stop(StopContext context) {
            httpManagementInjector.getValue().removeContext("health");
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }

    private static final class HealthCheckResourceManager implements ResourceManager {

        @Override
        public io.undertow.server.handlers.resource.Resource getResource(String path) {
            if (path != null && path.length() > 0 && !path.equals("/") && !path.equals("index.html")&& !path.equals("/index.html")) {
                return null;
            }

            final String config = System.getProperty("health.status", "UP");
            if ("missing".equalsIgnoreCase(config)) {
                return null;
            } else {
                final long now = System.currentTimeMillis();

                return new io.undertow.server.handlers.resource.Resource() {
                    @Override
                    public String getPath() {
                        return "";
                    }

                    @Override
                    public Date getLastModified() {
                        return new Date(now);
                    }

                    @Override
                    public String getLastModifiedString() {
                        return DateUtils.toDateString(getLastModified());
                    }

                    @Override
                    public ETag getETag() {
                        return "DOWN".equalsIgnoreCase(config) ? DOWN_ETAG : UP_ETAG;
                    }

                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public boolean isDirectory() {
                        return false;
                    }

                    @Override
                    public List<io.undertow.server.handlers.resource.Resource> list() {
                        throw new RuntimeException();
                    }

                    @Override
                    public String getContentType(MimeMappings mimeMappings) {
                        return "application/json";
                    }

                    @Override
                    public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
                        if ("DOWN".equalsIgnoreCase(config)) {
                            exchange.setStatusCode(503);
                            exchange.getResponseSender().send(ByteBuffer.wrap(DOWN));
                        } else {
                            exchange.setStatusCode(200);
                            exchange.getResponseSender().send(ByteBuffer.wrap(UP));
                        }
                    }

                    @Override
                    public Long getContentLength() {
                        return (long) ("DOWN".equalsIgnoreCase(config) ? DOWN.length : UP.length);
                    }

                    @Override
                    public String getCacheKey() {
                        return Long.valueOf(System.nanoTime()).toString();
                    }

                    @Override
                    public File getFile() {
                        return null;
                    }

                    @Override
                    public Path getFilePath() {
                        return null;
                    }

                    @Override
                    public File getResourceManagerRoot() {
                        return null;
                    }

                    @Override
                    public Path getResourceManagerRootPath() {
                        return null;
                    }

                    @Override
                    public URL getUrl() {
                        return null;
                    }
                };
            }

        }

        @Override
        public boolean isResourceChangeListenerSupported() {
            return false;
        }

        @Override
        public void registerResourceChangeListener(ResourceChangeListener listener) {
            EMPTY_RESOURCE_MANAGER.registerResourceChangeListener(listener);
        }

        @Override
        public void removeResourceChangeListener(ResourceChangeListener listener) {
            EMPTY_RESOURCE_MANAGER.removeResourceChangeListener(listener);
        }

        @Override
        public void close() {
        }
    }
}

