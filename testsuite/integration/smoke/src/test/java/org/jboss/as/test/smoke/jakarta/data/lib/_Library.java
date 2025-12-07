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

@StaticMetamodel(Library.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Library {

    String NAME = "name";
    String ID = "id";


    /**
     * @see Library#name
     **/
    TextAttribute<Library> name = new TextAttributeRecord<>(NAME);

    /**
     * @see Library#id
     **/
    SortableAttribute<Library> id = new SortableAttributeRecord<>(ID);

}

