/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j;

import org.neo4j.graphdb.Transaction;
import org.seedstack.seed.transaction.spi.ExceptionHandler;
import org.seedstack.seed.transaction.spi.TransactionMetadata;

/**
 * Neo4J flavor of {@link ExceptionHandler}.
 */
public interface Neo4jExceptionHandler extends ExceptionHandler<Transaction> {

    boolean handleException(Exception exception, TransactionMetadata associatedTransactionMetadata, Transaction associatedTransaction);

}
