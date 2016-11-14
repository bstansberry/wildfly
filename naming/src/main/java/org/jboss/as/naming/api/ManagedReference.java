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
 * A reference to a container managed object
 *
 * @author Stuart Douglas
 */
public interface ManagedReference {
    /**
     * Release the reference. Depending on the implementation this may destroy
     * the underlying object.
     * <p/>
     * Implementers should take care to make this method idempotent,
     * as it may be called multiple times.
     */
    void release();

    /**
     * Get the object instance.
     *
     * @return the object this reference refers to
     */
    Object getInstance();
}

