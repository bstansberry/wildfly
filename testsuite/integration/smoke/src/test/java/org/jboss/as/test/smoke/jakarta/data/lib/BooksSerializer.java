/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import java.util.Set;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public class BooksSerializer implements JsonbSerializer<Set<Book>> {
    @Override
    public void serialize(Set<Book> books, JsonGenerator generator, SerializationContext ctx) {
        generator.writeStartArray();
        for (Book book : books) {
            generator.write(book.getTitle());
        }
        generator.writeEnd();
    }
}
