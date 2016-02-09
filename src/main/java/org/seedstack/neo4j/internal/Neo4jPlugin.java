/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j.internal;

import com.google.common.collect.Lists;
import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequest;
import io.nuun.kernel.core.AbstractPlugin;
import org.apache.commons.configuration.Configuration;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.seedstack.neo4j.Neo4jExceptionHandler;
import org.seedstack.seed.Application;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.core.internal.application.ApplicationPlugin;
import org.seedstack.seed.transaction.internal.TransactionPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Neo4jPlugin extends AbstractPlugin {
    public static final String CONFIGURATION_PREFIX = "org.seedstack.neo4j";
    public static final String EXCEPTION_DB_NAME = "dbName";

    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jPlugin.class);

    private final Map<String, GraphDatabaseService> graphDatabaseServices = new HashMap<String, GraphDatabaseService>();
    private final Map<String, Class<? extends Neo4jExceptionHandler>> exceptionHandlerClasses = new HashMap<String, Class<? extends Neo4jExceptionHandler>>();

    @Override
    public String name() {
        return "neo4j";
    }

    @Override
    @SuppressWarnings("unchecked")
    public InitState init(InitContext initContext) {
        Application application = initContext.dependency(ApplicationPlugin.class).getApplication();
        TransactionPlugin transactionPlugin = initContext.dependency(TransactionPlugin.class);
        Configuration neo4jConfiguration = application.getConfiguration().subset(Neo4jPlugin.CONFIGURATION_PREFIX);

        String[] graphDatabaseNames = neo4jConfiguration.getStringArray("databases");

        if (graphDatabaseNames == null || graphDatabaseNames.length == 0) {
            LOGGER.info("No Neo4j graph database configured, Neo4j support disabled");
            return InitState.INITIALIZED;
        }
        for (String graphDatabaseName : graphDatabaseNames) {
            Configuration graphDatabaseConfiguration = neo4jConfiguration.subset("database." + graphDatabaseName);

            String exceptionHandler = graphDatabaseConfiguration.getString("exception-handler");
            if (exceptionHandler != null && !exceptionHandler.isEmpty()) {
                try {
                    exceptionHandlerClasses.put(graphDatabaseName, (Class<? extends Neo4jExceptionHandler>) Class.forName(exceptionHandler));
                } catch (Exception e) {
                    throw SeedException.wrap(e, Neo4jErrorCodes.UNABLE_TO_LOAD_EXCEPTION_HANDLER_CLASS)
                            .put(EXCEPTION_DB_NAME, graphDatabaseName).put("exceptionHandlerClass", exceptionHandler);
                }
            }

            String dbType = graphDatabaseConfiguration.getString("type", "embedded");
            if ("embedded".equals(dbType)) {
                graphDatabaseServices.put(graphDatabaseName, createEmbeddedDatabase(graphDatabaseName, graphDatabaseConfiguration, application));
            } else {
                throw SeedException.createNew(Neo4jErrorCodes.UNKNOWN_DATABASE_TYPE).put(EXCEPTION_DB_NAME, graphDatabaseName).put("dbType", dbType);
            }
        }

        if (graphDatabaseNames.length == 1) {
            Neo4jTransactionMetadataResolver.defaultDb = graphDatabaseNames[0];
        }

        transactionPlugin.registerTransactionHandler(Neo4jTransactionHandler.class);

        return InitState.INITIALIZED;
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

    @Override
    public Collection<ClasspathScanRequest> classpathScanRequests() {
        return classpathScanRequestBuilder().descendentTypeOf(Neo4jExceptionHandler.class).build();
    }

    @Override
    public Collection<Class<?>> requiredPlugins() {
        return Lists.<Class<?>>newArrayList(ApplicationPlugin.class, TransactionPlugin.class);
    }

    @Override
    public Object nativeUnitModule() {
        return new Neo4jModule(graphDatabaseServices, exceptionHandlerClasses);
    }

    private GraphDatabaseService createEmbeddedDatabase(String name, Configuration graphDatabaseConfiguration, Application application) {
        String path = graphDatabaseConfiguration.getString("path");
        String propertiesURL = graphDatabaseConfiguration.getString("properties");

        if (path == null || path.isEmpty()) {
            path = application.getStorageLocation(String.format("neo4j/%s", name)).getAbsolutePath();
        }

        GraphDatabaseBuilder databaseBuilder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(path);

        if (propertiesURL != null && !propertiesURL.isEmpty()) {
            try {
                databaseBuilder.loadPropertiesFromURL(new URL(propertiesURL));
            } catch (MalformedURLException e) {
                throw SeedException.wrap(e, Neo4jErrorCodes.INVALID_PROPERTIES_URL).put(EXCEPTION_DB_NAME, name).put("properties", propertiesURL);
            }
        }

        Iterator<String> it = graphDatabaseConfiguration.getKeys("setting");
        while (it.hasNext()) {
            String key = it.next();
            String setting = key.substring(8);
            try {
                databaseBuilder.setConfig((Setting<?>) GraphDatabaseSettings.class.getField(setting).get(null), graphDatabaseConfiguration.getString(key));
            } catch (Exception e) {
                throw SeedException.wrap(e, Neo4jErrorCodes.INVALID_DATABASE_SETTING).put(EXCEPTION_DB_NAME, name).put("setting", setting);
            }
        }

        LOGGER.info("Opening {} embedded graph database at {}", name, path);

        return databaseBuilder.newGraphDatabase();
    }
}
