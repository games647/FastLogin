/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2023 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.core.storage;

import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.zaxxer.hikari.HikariConfig;

public class PostgreSQLStorage extends SQLStorage {

    private static final String JDBC_PROTOCOL = "jdbc:";

    public PostgreSQLStorage(PlatformPlugin<?> plugin, String driver, String host, int port, String database,
                        HikariConfig config, boolean useSSL) {
        super(plugin.getLog(), plugin.getName(), plugin.getThreadFactory(),
                setParams(config, driver, host, port, database, useSSL));
    }

    private static HikariConfig setParams(HikariConfig config,
                                          String driver, String host, int port, String database,
                                          boolean useSSL) {
        // Require SSL on the server if requested in config - this will also verify certificate
        // Those values are deprecated in favor of sslMode
        config.addDataSourceProperty("useSSL", useSSL);
        config.addDataSourceProperty("requireSSL", useSSL);

        // adding paranoid, hides hostname, username, version and so
        // could be useful for hiding server details
        config.addDataSourceProperty("paranoid", true);

        config.setJdbcUrl(JDBC_PROTOCOL + buildJDBCUrl(driver, host, port, database));

        return config;
    }

    private static String buildJDBCUrl(String driver, String host, int port, String database) {
        return "postgresql://" + host + ':' + port + '/' + database;
    }

    @Override
    protected String getCreateTableStmt() {
        // PostgreSQL has a different syntax for id column
        return CREATE_TABLE_STMT
                .replace("`", "\"")
                .replace("INTEGER PRIMARY KEY AUTO_INCREMENT", "SERIAL PRIMARY KEY");
    }

    @Override
    protected String getAddFloodgateColumnStmt() {
        // PostgreSQL has a different syntax
        return ADD_FLOODGATE_COLUMN_STMT
                .replace("`", "\"")
                .replace("INTEGER(3)", "INTEGER");
    }

    @Override
    protected String getLoadByNameStmt() {
        return LOAD_BY_NAME_STMT
                .replace("`", "\"");
    }

    @Override
    protected String getLoadByUuidStmt() {
        return LOAD_BY_UUID_STMT
                .replace("`", "\"");
    }

    @Override
    protected String getInsertProfileStmt() {
        return INSERT_PROFILE_STMT
                .replace("`", "\"");
    }

    @Override
    protected String getUpdateProfileStmt() {
        return UPDATE_PROFILE_STMT
                .replace("`", "\"");
    }
}
