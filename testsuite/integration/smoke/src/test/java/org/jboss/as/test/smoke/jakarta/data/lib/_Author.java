/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

@StaticMetamodel(Author.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Author {

    String PERSON = "person";
    String ID = "id";


    /**
     * @see Author#person
     **/
    SortableAttribute<Author> person = new SortableAttributeRecord<>(PERSON);

    /**
     * @see Author#id
     **/
    SortableAttribute<Author> id = new SortableAttributeRecord<>(ID);

}

