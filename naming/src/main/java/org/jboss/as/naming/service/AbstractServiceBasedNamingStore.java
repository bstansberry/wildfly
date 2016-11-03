/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.naming.service;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.logging.NamingLogger;
import org.jboss.msc.service.ServiceName;

/**
 * Base class for a {@link NamingStore} implementation based on services.
 *
 * @author Brian Stansberry
 */
public abstract class AbstractServiceBasedNamingStore implements NamingStore {
    private final ConcurrentSkipListSet<ServiceName> boundServices = new ConcurrentSkipListSet<ServiceName>();

    protected final NavigableSet<ServiceName> getBoundServices() {
        return boundServices;
    }

    void add(final ServiceName serviceName) {
        final ConcurrentSkipListSet<ServiceName> boundServices = this.boundServices;
        if (boundServices.contains(serviceName)) {
            throw NamingLogger.ROOT_LOGGER.serviceAlreadyBound(serviceName);
        }
        boundServices.add(serviceName);
    }

    void remove(final ServiceName serviceName) {
        boundServices.remove(serviceName);
    }
}
