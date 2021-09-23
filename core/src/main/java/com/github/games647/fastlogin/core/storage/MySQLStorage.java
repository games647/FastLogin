package com.github.games647.fastlogin.core.storage;

import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.zaxxer.hikari.HikariConfig;

public class MySQLStorage extends SQLStorage {

    public MySQLStorage(FastLoginCore<?, ?, ?> core, String host, int port, String database, HikariConfig config, boolean useSSL) {
        super(core,
                "mysql://" + host + ':' + port + '/' + database,
                setParams(config, useSSL));
    }

    private static HikariConfig setParams(HikariConfig config, boolean useSSL) {
        // Require SSL on the server if requested in config - this will also verify certificate
        // Those values are deprecated in favor of sslMode
        config.addDataSourceProperty("useSSL", useSSL);
        config.addDataSourceProperty("requireSSL", useSSL);

        if (useSSL) {
            // require encrypted if possible
            config.addDataSourceProperty("sslMode", "VerifyFull");
        }

        // adding paranoid hides hostname, username, version and so
        // could be useful for hiding server details
        config.addDataSourceProperty("paranoid", true);

        // enable MySQL specific optimizations
        // disabled by default - will return the same prepared statement instance
        config.addDataSourceProperty("cachePrepStmts", true);
        // default prepStmtCacheSize 25 - amount of cached statements
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        // default prepStmtCacheSqlLimit 256 - length of SQL
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        // default false - available in newer versions caches the statements server-side
        config.addDataSourceProperty("useServerPrepStmts", true);
        // default false - prefer use of local values for autocommit and
        // transaction isolation (alwaysSendSetIsolation) should only be enabled if always use the set* methods
        // instead of raw SQL
        // https://forums.mysql.com/read.php?39,626495,626512
        config.addDataSourceProperty("useLocalSessionState", true);
        // rewrite batched statements to a single statement, adding them behind each other
        // only useful for addBatch statements and inserts
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        // cache result metadata
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        // cache results of show variables and collation per URL
        config.addDataSourceProperty("cacheServerConfiguration", true);
        // default false - set auto commit only if not matching
        config.addDataSourceProperty("elideSetAutoCommits", true);

        // default true - internal timers for idle calculation -> removes System.getCurrentTimeMillis call per query
        // Some platforms are slow on this and it could affect the throughput about 3% according to MySQL
        // performance gems presentation
        // In our case it can be useful to see the time in error messages
        // config.addDataSourceProperty("maintainTimeStats", false);

        return config;
    }
}
