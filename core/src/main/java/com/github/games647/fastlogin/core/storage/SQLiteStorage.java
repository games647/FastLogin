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

public class SQLiteStorage extends SQLStorage {

    private final Lock lock = new ReentrantLock();

    public SQLiteStorage(FastLoginCore<?, ?, ?> core, String databasePath, HikariConfig config) {
        super(core,
                "sqlite://" + replacePathVariables(core.getPlugin(), databasePath),
                setParams(config));
    }

    private static HikariConfig setParams(HikariConfig config) {
        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);

        //a try to fix https://www.spigotmc.org/threads/fastlogin.101192/page-26#post-1874647
        // format strings retrieved by the timestamp column to match them from MySQL
        config.addDataSourceProperty("date_string_format", "yyyy-MM-dd HH:mm:ss");

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
