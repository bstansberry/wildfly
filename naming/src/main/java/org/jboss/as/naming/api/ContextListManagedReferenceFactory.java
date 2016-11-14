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

package org.jboss.as.naming.api;

/**
 * A {@link org.jboss.as.naming.ManagedReferenceFactory} which knows the class name of its {@link org.jboss.as.naming.ManagedReference} object instance. This type of
 * {@link org.jboss.as.naming.ManagedReferenceFactory} should be used for JNDI bindings, as the {@link NamingStore} relies on it to provide
 * proper support for {@link javax.naming.Context} list operations.
 *
 * @author Eduardo Martins
 *
 */
public interface ContextListManagedReferenceFactory extends ManagedReferenceFactory {

    String DEFAULT_INSTANCE_CLASS_NAME = Object.class.getName();

    /**
     * Retrieves the reference's object instance class name.
     *
     * If it's impossible to obtain such data, the factory should return the static attribute DEFAULT_INSTANCE_CLASS_NAME,
     * exposed by this interface.
     *
     * @return
     */
    String getInstanceClassName();
}
