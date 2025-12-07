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

@StaticMetamodel(Person.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Person {

    String BIRTHDATE = "birthdate";
    String NAME = "name";
    String ID = "id";


    /**
     * @see Person#birthdate
     **/
    SortableAttribute<Person> birthdate = new SortableAttributeRecord<>(BIRTHDATE);

    /**
     * @see Person#name
     **/
    TextAttribute<Person> name = new TextAttributeRecord<>(NAME);

    /**
     * @see Person#id
     **/
    SortableAttribute<Person> id = new SortableAttributeRecord<>(ID);

}

