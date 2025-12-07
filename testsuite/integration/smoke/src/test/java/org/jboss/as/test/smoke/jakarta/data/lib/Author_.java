/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Author.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Author_ {

    public static final String BOOKS = "books";
    public static final String PERSON = "person";
    public static final String ID = "id";


    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Author#books
     **/
    public static volatile SetAttribute<Author, Book> books;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Author#person
     **/
    public static volatile SingularAttribute<Author, Person> person;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Author#id
     **/
    public static volatile SingularAttribute<Author, Long> id;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Author
     **/
    public static volatile EntityType<Author> class_;

}

