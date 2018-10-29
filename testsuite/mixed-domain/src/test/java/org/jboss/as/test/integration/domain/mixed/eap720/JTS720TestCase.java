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

package org.jboss.as.test.integration.domain.mixed.eap720;

import org.jboss.as.test.integration.domain.mixed.JTSTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

@Version(Version.AsVersion.EAP_7_2_0_TEMP)
public class JTS720TestCase extends JTSTestCase {
    @BeforeClass
    public static void beforeClass() {
        MixedDomain720TestSuite.initializeDomain();
    }
}
