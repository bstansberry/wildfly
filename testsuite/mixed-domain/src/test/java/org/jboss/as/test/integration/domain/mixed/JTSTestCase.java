/*
Copyright 2018 Red Hat, Inc.

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

package org.jboss.as.test.integration.domain.mixed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests enabling and disabling of JTS.
 *
 * @author Brian Stansberry
 */
public class JTSTestCase {

    private MixedDomainTestSupport support;

    @Before
    public void init() {
        support = MixedDomainTestSuite.getSupport(this.getClass());
    }

    @AfterClass
    public static synchronized void afterClass() {
        MixedDomainTestSuite.afterClass();
    }

    @Test
    public void testJTSTransformation() {
        final PathAddress subAddr = PathAddress.pathAddress("profile", "full-ha").append("subsystem", "transactions");
        final PathAddress jtsAddr = subAddr.append("service", "jts");

        DomainLifecycleUtil master = support.getDomainMasterLifecycleUtil();
        DomainLifecycleUtil slave = support.getDomainSlaveLifecycleUtil();

        ModelNode ra = Util.getReadAttributeOperation(subAddr, "jts");
        ModelNode jtsVal = master.executeForResult(ra);
        assertFalse(jtsVal.toString(), jtsVal.asBoolean());
        jtsVal = slave.executeForResult(ra);
        assertFalse(jtsVal.toString(), jtsVal.asBoolean());

        master.executeForResult(Util.createAddOperation(jtsAddr));

        jtsVal = master.executeForResult(ra);
        assertTrue(jtsVal.toString(), jtsVal.asBoolean());
        jtsVal = slave.executeForResult(ra);
        assertTrue(jtsVal.toString(), jtsVal.asBoolean());

        master.executeForResult(Util.createRemoveOperation(jtsAddr));

        jtsVal = master.executeForResult(ra);
        assertFalse(jtsVal.toString(), jtsVal.asBoolean());
        jtsVal = slave.executeForResult(ra);
        assertFalse(jtsVal.toString(), jtsVal.asBoolean());

        ModelNode wa = Util.getWriteAttributeOperation(subAddr, "jts", true);
        master.executeForResult(wa);

        jtsVal = master.executeForResult(ra);
        assertTrue(jtsVal.toString(), jtsVal.asBoolean());
        jtsVal = slave.executeForResult(ra);
        assertTrue(jtsVal.toString(), jtsVal.asBoolean());

        wa.get("value").set(false);
        master.executeForResult(wa);

        jtsVal = master.executeForResult(ra);
        assertFalse(jtsVal.toString(), jtsVal.asBoolean());
        jtsVal = slave.executeForResult(ra);
        assertFalse(jtsVal.toString(), jtsVal.asBoolean());


    }
}
