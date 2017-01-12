/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j.internal;

import com.google.common.base.Strings;
import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequest;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.seedstack.neo4j.Neo4jConfig;
import org.seedstack.neo4j.Neo4jExceptionHandler;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.core.internal.AbstractSeedPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Neo4jPlugin extends AbstractSeedPlugin {
    private static final String EXCEPTION_DB_NAME = "dbName";
    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jPlugin.class);
    private final Map<String, GraphDatabaseService> graphDatabaseServices = new HashMap<String, GraphDatabaseService>();
    private final Map<String, Class<? extends Neo4jExceptionHandler>> exceptionHandlerClasses = new HashMap<String, Class<? extends Neo4jExceptionHandler>>();

    @Override
    public String name() {
        return "neo4j";
    }

    @Override
    public Collection<ClasspathScanRequest> classpathScanRequests() {
        return classpathScanRequestBuilder().descendentTypeOf(Neo4jExceptionHandler.class).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public InitState initialize(InitContext initContext) {
        Neo4jConfig neo4jConfig = getConfiguration(Neo4jConfig.class);

        if (neo4jConfig.getDatabases().isEmpty()) {
            LOGGER.info("No Neo4j graph database configured, Neo4j support disabled");
            return InitState.INITIALIZED;
        }
        for (Map.Entry<String, Neo4jConfig.DatabaseConfig> dbEntry : neo4jConfig.getDatabases().entrySet()) {
            String dbName = dbEntry.getKey();
            Neo4jConfig.DatabaseConfig dbConfig = dbEntry.getValue();

            Class<? extends Neo4jExceptionHandler> exceptionHandler = dbConfig.getExceptionHandler();
            if (exceptionHandler != null) {
                exceptionHandlerClasses.put(dbName, exceptionHandler);
            }

            if (dbConfig.getType() == Neo4jConfig.DatabaseConfig.DatabaseType.EMBEDDED) {
                graphDatabaseServices.put(dbName, createEmbeddedDatabase(dbName, dbConfig));
            } else {
                throw SeedException.createNew(Neo4jErrorCode.UNSUPPORTED_DATABASE_TYPE)
                        .put(EXCEPTION_DB_NAME, dbEntry)
                        .put("dbType", dbConfig.getType());
            }
        }

        if (!Strings.isNullOrEmpty(neo4jConfig.getDefaultDatabase())) {
            Neo4jTransactionMetadataResolver.defaultDb = neo4jConfig.getDefaultDatabase();
        }

        return InitState.INITIALIZED;
    }

    @Override
    public Object nativeUnitModule() {
        return new Neo4jModule(graphDatabaseServices, exceptionHandlerClasses);
    }

    @Override
    public void stop() {
        for (Map.Entry<String, GraphDatabaseService> graphDatabaseServiceEntry : graphDatabaseServices.entrySet()) {
            LOGGER.info("Shutting down {} graph database", graphDatabaseServiceEntry.getKey());
            try {
                graphDatabaseServiceEntry.getValue().shutdown();
            } catch (Exception e) {
                LOGGER.error(String.format("Unable to properly shutdown %s graph database", graphDatabaseServiceEntry.getKey()), e);
            }
        }
    }

    private GraphDatabaseService createEmbeddedDatabase(String name, Neo4jConfig.DatabaseConfig dbConfig) {
        String path = dbConfig.getPath();
        File directory;

        if (path == null || path.isEmpty()) {
            directory = getApplication().getStorageLocation(String.format("neo4j/%s", name));
        } else {
            directory = new File(path);
        }

        GraphDatabaseBuilder databaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(directory);

        URL propertiesURL = dbConfig.getPropertiesURL();
        if (propertiesURL != null) {
            databaseBuilder.loadPropertiesFromURL(propertiesURL);
        }

        for (Map.Entry<String, String> settingEntry : dbConfig.getSettings().entrySet()) {
            String key = settingEntry.getKey();
            try {
                databaseBuilder.setConfig((Setting<?>) GraphDatabaseSettings.class.getField(key).get(null), settingEntry.getValue());
            } catch (Exception e) {
                throw SeedException.wrap(e, Neo4jErrorCode.INVALID_DATABASE_SETTING).put(EXCEPTION_DB_NAME, name).put("setting", key);
            }
        }

        LOGGER.info("Opening {} embedded graph database at {}", name, directory.getAbsoluteFile().toString());

        return databaseBuilder.newGraphDatabase();
    }
}
