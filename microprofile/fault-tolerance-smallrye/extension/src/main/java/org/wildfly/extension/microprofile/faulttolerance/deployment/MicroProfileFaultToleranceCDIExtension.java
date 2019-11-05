/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import org.wildfly.extension.microprofile.faulttolerance.cdi.RequestContextCommandListener;
import org.wildfly.extension.microprofile.faulttolerance.cdi.WeldCommandListenersProvider;

/**
 * Registers annotated types to support {@link io.smallrye.faulttolerance.CommandListener}s.
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceCDIExtension implements Extension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        event.addAnnotatedType(beanManager.createAnnotatedType(RequestContextCommandListener.class), RequestContextCommandListener.class.getName());
        event.addAnnotatedType(beanManager.createAnnotatedType(WeldCommandListenersProvider.class), WeldCommandListenersProvider.class.getName());
    }

}
