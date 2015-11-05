/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j.internal;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.transaction.spi.TransactionalLink;

import java.util.ArrayDeque;
import java.util.Deque;

class GraphDatabaseServiceLink implements TransactionalLink<GraphDatabaseService> {
    private final ThreadLocal<Deque<Transaction>> perThreadObjectContainer = new ThreadLocal<Deque<Transaction>>() {
        @Override
        protected Deque<Transaction> initialValue() {
            return new ArrayDeque<Transaction>();
        }
    };
    private GraphDatabaseService graphDatabaseService;

    public GraphDatabaseService get() {
        if (this.perThreadObjectContainer.get().peek() == null) {
            throw SeedException.createNew(Neo4jErrorCodes.ACCESSING_DATABASE_OUTSIDE_TRANSACTION);
        }
        return this.graphDatabaseService;
    }

    void push(GraphDatabaseService graphDatabaseService, Transaction transaction) {
        this.perThreadObjectContainer.get().push(transaction);
        this.graphDatabaseService = graphDatabaseService;
    }

    void pop() {
        this.graphDatabaseService = null;
        this.perThreadObjectContainer.get().pop();
    }

    Transaction getCurrentTransaction() {
        return perThreadObjectContainer.get().peek();
    }
}
