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

@StaticMetamodel(Librarian.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Librarian_ {

    public static final String HIRE_DATE = "hireDate";
    public static final String LIBRARY = "library";
    public static final String PERSON = "person";
    public static final String ID = "id";


    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Librarian#hireDate
     **/
    public static volatile SingularAttribute<Librarian, LocalDate> hireDate;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Librarian#library
     **/
    public static volatile SingularAttribute<Librarian, Library> library;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Librarian#person
     **/
    public static volatile SingularAttribute<Librarian, Person> person;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Librarian#id
     **/
    public static volatile SingularAttribute<Librarian, Long> id;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Librarian
     **/
    public static volatile EntityType<Librarian> class_;

}

