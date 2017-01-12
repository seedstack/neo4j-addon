/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j.internal;

import org.seedstack.neo4j.Neo4jDb;
import org.seedstack.shed.reflect.StandardAnnotationResolver;

import java.lang.reflect.Method;

class Neo4jResolver extends StandardAnnotationResolver<Method, Neo4jDb> {
    static Neo4jResolver INSTANCE = new Neo4jResolver();

    private Neo4jResolver() {
        // no external instantiation allowed
    }
}
