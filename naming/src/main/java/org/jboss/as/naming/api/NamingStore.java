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

import java.util.List;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.event.NamingListener;

/**
 * Interface to layout a contract for naming entry back-end storage.  This will be used by {@code NamingContext} instances
 * to manage naming entries.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public interface NamingStore {

    /**
     * Retrieves the store's base name, which is the prefix for the absolute name of each entry in the store.
     * @return
     * @throws NamingException
     */
    Name getBaseName() throws NamingException;

    /**
     * Look up an object from the naming store.  An entry for this name must already exist.
     *
     * @param name The entry name
     * @return The object from the store.
     * @throws NamingException If any errors occur.
     */
    Object lookup(Name name) throws NamingException;

    /**
     * Look up an object from the naming store.  An entry for this name must already exist.
     *
     * @param name The entry name
     * @param dereference if true indicates that managed references should retrieve the instance.
     * @return The object from the store.
     * @throws NamingException If any errors occur.
     */
    Object lookup(Name name, boolean dereference) throws NamingException;

    /**
     * List the NameClassPair instances for the provided name.  An entry for this name must already exist and be bound
     * to a valid context.
     *
     * @param name The entry name
     * @return The NameClassPair instances
     * @throws NamingException If any errors occur
     */
    List<NameClassPair> list(Name name) throws NamingException;

    /**
     * List the binding objects for a specified name.  An entry for this name must already exist and be bound
     * to a valid context.
     *
     * @param name The entry name
     * @return The bindings
     * @throws NamingException If any errors occur
     */
    List<Binding> listBindings(Name name) throws NamingException;

    /**
     * Close the naming store and cleanup any resource used by the store.
     *
     * @throws NamingException If any errors occur
     */
    void close() throws NamingException;

    /**
     * Add a {@code NamingListener} for a specific target and scope.
     *
     * @param target   The target name to add the listener to
     * @param scope    The listener scope
     * @param listener The listener
     */
    void addNamingListener(Name target, int scope, NamingListener listener);

    /**
     * Remove a {@code NamingListener} from all targets and scopes
     *
     * @param listener The listener
     */
    void removeNamingListener(NamingListener listener);
}
