/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import java.time.LocalDate;

@StaticMetamodel(Person.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Person_ {

    public static final String BIRTHDATE = "birthdate";
    public static final String NAME = "name";
    public static final String ID = "id";


    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Person#birthdate
     **/
    public static volatile SingularAttribute<Person, LocalDate> birthdate;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Person#name
     **/
    public static volatile SingularAttribute<Person, String> name;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Person#id
     **/
    public static volatile SingularAttribute<Person, Long> id;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Person
     **/
    public static volatile EntityType<Person> class_;

}

