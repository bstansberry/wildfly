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

@StaticMetamodel(Library.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Library_ {

    public static final String BOOKS = "books";
    public static final String NAME = "name";
    public static final String ID = "id";
    public static final String LIBRARIANS = "librarians";


    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Library#books
     **/
    public static volatile SetAttribute<Library, Book> books;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Library#name
     **/
    public static volatile SingularAttribute<Library, String> name;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Library#id
     **/
    public static volatile SingularAttribute<Library, Long> id;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Library
     **/
    public static volatile EntityType<Library> class_;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Library#librarians
     **/
    public static volatile SetAttribute<Library, Librarian> librarians;

}

