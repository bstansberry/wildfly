/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Book.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Book_ {

    public static final String PAGE_COUNT = "pageCount";
    public static final String AUTHOR = "author";
    public static final String ID = "id";
    public static final String TITLE = "title";


    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Book#pageCount
     **/
    public static volatile SingularAttribute<Book, Integer> pageCount;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Book#author
     **/
    public static volatile SingularAttribute<Book, Author> author;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Book#id
     **/
    public static volatile SingularAttribute<Book, Long> id;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Book#title
     **/
    public static volatile SingularAttribute<Book, String> title;

    /**
     * @see org.jboss.as.test.smoke.jakarta.data.lib.Book
     **/
    public static volatile EntityType<Book> class_;

}

