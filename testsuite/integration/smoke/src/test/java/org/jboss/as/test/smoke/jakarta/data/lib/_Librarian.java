/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

@StaticMetamodel(Librarian.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Librarian {

    String HIRE_DATE = "hireDate";
    String PERSON = "person";
    String ID = "id";
    String LIBRARY = "library";


    /**
     * @see Librarian#hireDate
     **/
    SortableAttribute<Librarian> hireDate = new SortableAttributeRecord<>(HIRE_DATE);

    /**
     * @see Librarian#person
     **/
    SortableAttribute<Librarian> person = new SortableAttributeRecord<>(PERSON);

    /**
     * @see Librarian#id
     **/
    SortableAttribute<Librarian> id = new SortableAttributeRecord<>(ID);

    /**
     * @see Librarian#library
     **/
    SortableAttribute<Librarian> library = new SortableAttributeRecord<>(LIBRARY);

}

