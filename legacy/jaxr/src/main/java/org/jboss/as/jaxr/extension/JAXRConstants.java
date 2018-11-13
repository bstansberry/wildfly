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

package org.jboss.as.jaxr.extension;

/**
 * Constants used by the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 07-Nov-2011
 */
interface JAXRConstants {

    String LATEST_NAMESPACE = "urn:jboss:domain:jaxr:1.1";

    String CLASS = "class";
    String CONNECTION_FACTORY = "connection-factory";
    String JNDI_NAME = "jndi-name";
    String NAME = "name";
    String PROPERTIES = "properties";
    String PROPERTY = "property";
    String VALUE = "value";
}
