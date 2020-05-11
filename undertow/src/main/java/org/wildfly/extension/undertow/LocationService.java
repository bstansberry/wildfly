/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class LocationService implements Service<LocationService>, FilterLocation {

    private final String locationPath;
    private final InjectedValue<HttpHandler> httpHandler = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final CopyOnWriteArrayList<UndertowFilter> filters = new CopyOnWriteArrayList<>();
    private final LocationHandler locationHandler = new LocationHandler();
    private volatile HttpHandler configuredHandler;

    public LocationService(String locationPath) {
        this.locationPath = locationPath;
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.tracef("registering handler %s under path '%s'", httpHandler.getValue(), locationPath);
        host.getValue().registerLocation(this);
    }

    @Override
    public void stop(StopContext context) {
        host.getValue().unregisterLocation(this);
    }

    @Override
    public LocationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    InjectedValue<Host> getHost() {
        return host;
    }

    InjectedValue<HttpHandler> getHttpHandler() {
        return httpHandler;
    }

    String getLocationPath() {
        return locationPath;
    }

    LocationHandler getLocationHandler() {
        return locationHandler;
    }

    private HttpHandler configureHandler() {
        ArrayList<UndertowFilter> filters = new ArrayList<>(this.filters);
        return configureHandlerChain(getHttpHandler().getValue(), filters);
    }

    protected static HttpHandler configureHandlerChain(HttpHandler rootHandler, List<UndertowFilter> filters) {

        int size = filters.size();
        // Do the simple thing for the simple cases where there's no ordering involved
        if (size == 0) {
            return rootHandler;
        } else if (size == 1) {
            return filters.get(0).wrap(rootHandler);
        }

        // Order the filters by priority but for any with equal priority the one
        // that came first in the 'filters' input list gets executed first
        List<UndertowFilter> wrappers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            wrappers.add(new FilterRefWrapper(filters.get(i), size, i));
        }
        wrappers.sort((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
        Collections.reverse(wrappers); //handler chain goes last first

        HttpHandler handler = rootHandler;
        for (UndertowFilter filter : wrappers) {
            handler = filter.wrap(handler);
        }

        return handler;
    }

    @Override
    public void addFilter(UndertowFilter filterRef) {
        filters.add(filterRef);
        configuredHandler = null;
    }

    @Override
    public void removeFilter(UndertowFilter filterRef) {
        filters.remove(filterRef);
        configuredHandler = null;
    }

    private class LocationHandler implements HttpHandler {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            HttpHandler root = configuredHandler;
            if(root == null) {
                synchronized (LocationService.this) {
                    root = configuredHandler;
                    if(root == null) {
                        root = configuredHandler = configureHandler();
                    }
                }
            }
            root.handleRequest(exchange);
        }
    }

    private static class FilterRefWrapper implements UndertowFilter {
        private final UndertowFilter wrapped;
        private final int spacer;
        private final int ordinal;

        private FilterRefWrapper(UndertowFilter wrapped, int spacer, int ordinal) {
            this.wrapped = wrapped;
            this.spacer = spacer;
            this.ordinal = ordinal;
        }

        @Override
        public int getPriority() {
            return (wrapped.getPriority() * spacer) + ordinal;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return wrapped.wrap(handler);
        }
    }
}
