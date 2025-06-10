/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.library;

import static org.jboss.as.test.smoke.jakarta.data.lib.Constants.CHANTAL;
import static org.jboss.as.test.smoke.jakarta.data.lib.Constants.CHANTAL_BDAY;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.as.test.smoke.jakarta.data.lib.Recruiter;

@ApplicationScoped
public class InitialRecruiter {

    @Inject
    private Recruiter recruiter;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        recruiter.recruit(CHANTAL, CHANTAL_BDAY);
    }
}
