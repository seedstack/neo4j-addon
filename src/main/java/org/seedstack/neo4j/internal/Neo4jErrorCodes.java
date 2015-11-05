/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j.internal;

import org.seedstack.seed.ErrorCode;

enum Neo4jErrorCodes implements ErrorCode {
    // Please keep it sorted
    ACCESSING_DATABASE_OUTSIDE_TRANSACTION,
    INVALID_DATABASE_SETTING,
    INVALID_PROPERTIES_URL,
    UNKNOWN_DATABASE_TYPE, // TODO missing error code
    UNABLE_TO_LOAD_EXCEPTION_HANDLER_CLASS
}
