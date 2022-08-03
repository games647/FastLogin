/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
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

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.zaxxer.hikari.HikariConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.sqlite.SQLiteConfig;

public class SQLiteStorage extends SQLStorage {

    private static final String SQLITE_DRIVER = "org.sqlite.SQLiteDataSource";
    private final Lock lock = new ReentrantLock();

    public SQLiteStorage(FastLoginCore<?, ?, ?> core, String databasePath, HikariConfig config) {
        super(core, setParams(config, replacePathVariables(core.getPlugin(), databasePath)));
    }

    private static HikariConfig setParams(HikariConfig config, String path) {
        config.setDataSourceClassName(SQLITE_DRIVER);

        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);

        config.addDataSourceProperty("url", path);

        // a try to fix https://www.spigotmc.org/threads/fastlogin.101192/page-26#post-1874647
        // format strings retrieved by the timestamp column to match them from MySQL
        // vs the default: yyyy-MM-dd HH:mm:ss.SSS
        SQLiteConfig sqLiteConfig = new SQLiteConfig();
        sqLiteConfig.setDateStringFormat("yyyy-MM-dd HH:mm:ss");
        config.addDataSourceProperty("config", config);

        // TODO: test first for compatibility
        // config.addDataSourceProperty("date_precision", "seconds");

        return config;
    }

    @Override
    public StoredProfile loadProfile(String name) {
        lock.lock();
        try {
            return super.loadProfile(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public StoredProfile loadProfile(UUID uuid) {
        lock.lock();
        try {
            return super.loadProfile(uuid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save(StoredProfile playerProfile) {
        lock.lock();
        try {
            super.save(playerProfile);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void createTables() throws SQLException {
        try (Connection con = dataSource.getConnection();
             Statement createStmt = con.createStatement()) {
            // SQLite has a different syntax for auto increment
            createStmt.executeUpdate(CREATE_TABLE_STMT.replace("AUTO_INCREMENT", "AUTOINCREMENT"));
        }
    }

    private static String replacePathVariables(PlatformPlugin<?> plugin, String input) {
        String pluginFolder = plugin.getPluginFolder().toAbsolutePath().toString();
        return input.replace("{pluginDir}", pluginFolder);
    }
}
