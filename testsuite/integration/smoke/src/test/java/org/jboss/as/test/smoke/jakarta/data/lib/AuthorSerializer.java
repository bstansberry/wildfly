/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jakarta.data.lib;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public class AuthorSerializer implements JsonbSerializer<Author> {
    @Override
    public void serialize(Author author, JsonGenerator generator, SerializationContext ctx) {
        generator.write(author.getPerson().getName());
    }
}
