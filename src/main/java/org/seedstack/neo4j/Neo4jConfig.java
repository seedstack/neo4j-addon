/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.neo4j;

import org.seedstack.coffig.Config;
import org.seedstack.coffig.SingleValue;

import javax.validation.constraints.NotNull;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Config("neo4j")
public class Neo4jConfig {
    private Map<String, DatabaseConfig> databases = new HashMap<>();
    private String defaultDatabase;

    public Map<String, DatabaseConfig> getDatabases() {
        return Collections.unmodifiableMap(databases);
    }

    public Neo4jConfig addDatabase(String name, DatabaseConfig config) {
        this.databases.put(name, config);
        return this;
    }

    public String getDefaultDatabase() {
        return defaultDatabase;
    }

    public Neo4jConfig setDefaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
        return this;
    }

    public static class DatabaseConfig {
        @NotNull
        private DatabaseType type = DatabaseType.EMBEDDED;
        @SingleValue
        private String path;
        private URL propertiesURL;
        private Map<String, String> settings = new HashMap<>();
        private Class<? extends Neo4jExceptionHandler> exceptionHandler;

        public Class<? extends Neo4jExceptionHandler> getExceptionHandler() {
            return exceptionHandler;
        }

        public DatabaseConfig setExceptionHandler(Class<? extends Neo4jExceptionHandler> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }

        public DatabaseType getType() {
            return type;
        }

        public DatabaseConfig setType(DatabaseType type) {
            this.type = type;
            return this;
        }

        public String getPath() {
            return path;
        }

        public DatabaseConfig setPath(String path) {
            this.path = path;
            return this;
        }

        public URL getPropertiesURL() {
            return propertiesURL;
        }

        public DatabaseConfig setPropertiesURL(URL propertiesURL) {
            this.propertiesURL = propertiesURL;
            return this;
        }

        public Map<String, String> getSettings() {
            return Collections.unmodifiableMap(settings);
        }

        public void addSetting(String key, String value) {
            this.settings.put(key, value);
        }

        public enum DatabaseType {
            EMBEDDED
        }
    }
}
