package org.jboss.as.test.integration.weld.extensions.buildcompatible.subsystem;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.URL;

@RunWith(Arquillian.class)
    public class SubsystemBceRegistrationTest {

    private static final String MODULE_NAME = "build-compatible-extension";
    private static TestModule testModule;

    public static void doSetup() throws Exception {
        URL url = BCEExtension.class.getResource(MODULE_NAME + "-module.xml");
        File moduleXmlFile = new File(url.toURI());
        testModule = new TestModule("test." + MODULE_NAME, moduleXmlFile);
        testModule.addResource("bce-extension.jar")
                .addClasses(BCEExtension.class, RegisteredExtension.class, RegisteredBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsServiceProvider(org.jboss.as.controller.Extension.class, BCEExtension.class);
        testModule.create();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        testModule.remove();
    }

    @Deployment
    public static WebArchive getDeployment() throws Exception {
        doSetup();
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(SubsystemBceRegistrationTest.class, DummyBean.class, TestModule.class, RegisteredBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    DummyBean dummyBean;

    @Inject
    RegisteredBean registeredBean;

    @Test
    public void testBceRegisteredAndExecuted() {
        // check plain WAR deployment
        Assert.assertNotNull(dummyBean);

        // verify BCE was executed; if so, RegisteredBean would now be resolvable
        Assert.assertNotNull(registeredBean);
    }
}
