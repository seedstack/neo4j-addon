/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j.internal;

import org.aopalliance.intercept.MethodInvocation;
import org.seedstack.neo4j.Neo4jDb;
import org.seedstack.neo4j.Neo4jExceptionHandler;
import org.seedstack.seed.transaction.spi.TransactionMetadata;
import org.seedstack.seed.transaction.spi.TransactionMetadataResolver;

import java.util.Optional;

/**
 * This {@link TransactionMetadataResolver} resolves metadata for transactions marked
 * with {@link Neo4jDb}.
 */
class Neo4jTransactionMetadataResolver implements TransactionMetadataResolver {
    static String defaultDb;

    @Override
    public TransactionMetadata resolve(MethodInvocation methodInvocation, TransactionMetadata defaults) {
        Optional<Neo4jDb> neo4jDb = Neo4jResolver.INSTANCE.apply(methodInvocation.getMethod());

        if (neo4jDb.isPresent() || Neo4jTransactionHandler.class.equals(defaults.getHandler())) {
            TransactionMetadata result = new TransactionMetadata();
            result.setHandler(Neo4jTransactionHandler.class);
            result.setExceptionHandler(Neo4jExceptionHandler.class);
            result.setResource(neo4jDb.isPresent() ? neo4jDb.get().value() : defaultDb);
            return result;
        }

        return null;
    }
}