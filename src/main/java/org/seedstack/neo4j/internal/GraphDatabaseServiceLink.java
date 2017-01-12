/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
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
    private final ThreadLocal<Deque<Holder>> perThreadObjectContainer = new ThreadLocal<Deque<Holder>>() {
        @Override
        protected Deque<Holder> initialValue() {
            return new ArrayDeque<Holder>();
        }
    };

    public GraphDatabaseService get() {
        Holder holder = this.perThreadObjectContainer.get().peek();

        if (holder == null) {
            throw SeedException.createNew(Neo4jErrorCode.ACCESSING_DATABASE_OUTSIDE_TRANSACTION);
        }

        return holder.graphDatabaseService;
    }

    Transaction getCurrentTransaction() {
        Holder holder = perThreadObjectContainer.get().peek();
        if (holder != null) {
            return holder.transaction;
        } else {
            return null;
        }
    }

    void push(GraphDatabaseService graphDatabaseService, Transaction transaction) {
        this.perThreadObjectContainer.get().push(new Holder(graphDatabaseService, transaction));
    }

    void pop() {
        Deque<Holder> holders = this.perThreadObjectContainer.get();
        holders.pop();
        if (holders.isEmpty()) {
            perThreadObjectContainer.remove();
        }
    }

    private static class Holder {
        private final GraphDatabaseService graphDatabaseService;
        private final Transaction transaction;

        private Holder(GraphDatabaseService graphDatabaseService, Transaction transaction) {
            this.graphDatabaseService = graphDatabaseService;
            this.transaction = transaction;
        }
    }
}
