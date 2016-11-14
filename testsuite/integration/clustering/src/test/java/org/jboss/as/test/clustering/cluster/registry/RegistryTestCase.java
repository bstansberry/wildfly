package org.jboss.as.test.clustering.cluster.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.cluster.ClusterAbstractTestCase;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetriever;
import org.jboss.as.test.clustering.cluster.registry.bean.RegistryRetrieverBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class RegistryTestCase extends ClusterAbstractTestCase {
    private static final String CLIENT_PROPERTIES = "cluster/ejb3/stateless/jboss-ejb-client.properties";

    private final String moduleName;

    protected RegistryTestCase(String moduleName) {
        this.moduleName = moduleName;
    }

    @Test
    public void test() throws Exception {

        ContextSelector<EJBClientContext> selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES);

        try (EJBDirectory context = new RemoteEJBDirectory(this.moduleName)) {
            RegistryRetriever bean = context.lookupStateless(RegistryRetrieverBean.class, RegistryRetriever.class);
            Collection<String> names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.toString(), names.contains(NODE_1));
            assertTrue(names.toString(), names.contains(NODE_2));

            undeploy(DEPLOYMENT_1);

            names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_2));

            deploy(DEPLOYMENT_1);

            names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));

            stop(CONTAINER_2);

            names = bean.getNodes();
            assertEquals(1, names.size());
            assertTrue(names.contains(NODE_1));

            start(CONTAINER_2);

            names = bean.getNodes();
            assertEquals(2, names.size());
            assertTrue(names.contains(NODE_1));
            assertTrue(names.contains(NODE_2));
        } finally {
            // reset the selector
            if (selector != null) {
                EJBClientContext.setSelector(selector);
            }
        }
    }
}
