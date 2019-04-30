/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.naming.defaultbindings.concurrency;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 *
 * Test for EE's default data source on a CDI Bean
 *
 * @author Eduardo Martins
 */
@RunWith(Arquillian.class)
public class DefaultConcurrencyCDITestCase {
    private static final String DEPLOYMENT_1 = "ee-concurrency-cdi-1";
    private static final String DEPLOYMENT_2 = "ee-concurrency-cdi-2";

    @Deployment(name = DEPLOYMENT_1)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_1 + ".jar");
        jar.addClasses(DefaultConcurrencyCDITestCase.class, DefaultConcurrencyTestCDIBean.class);
        jar.addAsManifestResource(new StringAsset(""), "beans.xml");
        return jar;
    }

    @Deployment(name = DEPLOYMENT_2)
    public static Archive<?> deploy2() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_2 + ".jar");
        jar.addClasses(DefaultConcurrencyCDITestCase.class, DefaultConcurrencyTestCDIBean.class);
        jar.addAsManifestResource(new StringAsset(""), "beans.xml");
        return jar;
    }

    @Inject
    private DefaultConcurrencyTestCDIBean defaultConcurrencyTestCDIBean;

    @OperateOnDeployment(DEPLOYMENT_1)
    @Test
    public void testCDI() throws Throwable {
        defaultConcurrencyTestCDIBean.test();
    }

    @OperateOnDeployment(DEPLOYMENT_1)
    @Test
    public void testTcclDep1() throws Exception {
        final ClassLoader tccl = defaultConcurrencyTestCDIBean.getTccl().get();
        Assert.assertNotNull(tccl);
        Assert.assertTrue(String.format("Expected the TCCL (%s) to contain %s", tccl, DEPLOYMENT_1), tccl.toString().contains(DEPLOYMENT_1));
    }

    @OperateOnDeployment(DEPLOYMENT_2)
    @Test
    public void testTcclDep2() throws Exception {
        final ClassLoader tccl = defaultConcurrencyTestCDIBean.getTccl().get();
        Assert.assertNotNull(tccl);
        Assert.assertTrue(String.format("Expected the TCCL (%s) to contain %s", tccl, DEPLOYMENT_2), tccl.toString().contains(DEPLOYMENT_2));
    }

}
