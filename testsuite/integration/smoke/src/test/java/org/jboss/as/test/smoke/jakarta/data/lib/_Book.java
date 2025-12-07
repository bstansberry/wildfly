/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

@StaticMetamodel(Book.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Book {

    String PAGE_COUNT = "pageCount";
    String AUTHOR = "author";
    String TITLE = "title";
    String ID = "id";


    /**
     * @see Book#pageCount
     **/
    SortableAttribute<Book> pageCount = new SortableAttributeRecord<>(PAGE_COUNT);

    /**
     * @see Book#author
     **/
    SortableAttribute<Book> author = new SortableAttributeRecord<>(AUTHOR);

    /**
     * @see Book#title
     **/
    TextAttribute<Book> title = new TextAttributeRecord<>(TITLE);

    /**
     * @see Book#id
     **/
    SortableAttribute<Book> id = new SortableAttributeRecord<>(ID);

}

