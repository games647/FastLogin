package com.github.games647.fastlogin.core;

import com.github.games647.craftapi.UUIDAdapter;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class AuthStorage {

    private static final String PREMIUM_TABLE = "premium";

    private static final String LOAD_BY_NAME = "SELECT * FROM " + PREMIUM_TABLE + " WHERE Name=? LIMIT 1";
    private static final String LOAD_BY_UUID = "SELECT * FROM " + PREMIUM_TABLE + " WHERE UUID=? LIMIT 1";
    private static final String INSERT_PROFILE = "INSERT INTO " + PREMIUM_TABLE + " (UUID, Name, Premium, LastIp) "
            + "VALUES (?, ?, ?, ?) ";
    private static final String UPDATE_PROFILE = "UPDATE " + PREMIUM_TABLE
            + " SET UUID=?, Name=?, Premium=?, LastIp=?, LastLogin=CURRENT_TIMESTAMP WHERE UserID=?";

    private final FastLoginCore<?, ?, ?> core;
    private final HikariDataSource dataSource;

    public AuthStorage(FastLoginCore<?, ?, ?> core, String driver, String host, int port, String databasePath
            , String user, String pass, boolean useSSL) {
        this.core = core;

        HikariConfig config = new HikariConfig();
        config.setPoolName(core.getPlugin().getName());

        config.setUsername(user);
        config.setPassword(pass);
        config.setDriverClassName(driver);

        //a try to fix https://www.spigotmc.org/threads/fastlogin.101192/page-26#post-1874647
        Properties properties = new Properties();
        properties.setProperty("date_string_format", "yyyy-MM-dd HH:mm:ss");
        properties.setProperty("useSSL", String.valueOf(useSSL));
        config.setDataSourceProperties(properties);

        ThreadFactory platformThreadFactory = core.getPlugin().getThreadFactory();
        if (platformThreadFactory != null) {
            config.setThreadFactory(platformThreadFactory);
        }

        String jdbcUrl = "jdbc:";
        if (driver.contains("sqlite")) {
            String pluginFolder = core.getPlugin().getPluginFolder().toAbsolutePath().toString();
            databasePath = databasePath.replace("{pluginDir}", pluginFolder);

            jdbcUrl += "sqlite://" + databasePath;
            config.setConnectionTestQuery("SELECT 1");
            config.setMaximumPoolSize(1);
        } else {
            jdbcUrl += "mysql://" + host + ':' + port + '/' + databasePath;
        }

        config.setJdbcUrl(jdbcUrl);
        this.dataSource = new HikariDataSource(config);
    }

    public void createTables() throws SQLException {
        try (Connection con = dataSource.getConnection();
             Statement createStmt = con.createStatement()) {
            String createDataStmt = "CREATE TABLE IF NOT EXISTS " + PREMIUM_TABLE + " ("
                    + "UserID INTEGER PRIMARY KEY AUTO_INCREMENT, "
                    + "UUID CHAR(36), "
                    + "Name VARCHAR(16) NOT NULL, "
                    + "Premium BOOLEAN NOT NULL, "
                    + "LastIp VARCHAR(255) NOT NULL, "
                    + "LastLogin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    //the premium shouldn't steal the cracked account by changing the name
                    + "UNIQUE (Name) "
                    + ')';

            if (dataSource.getJdbcUrl().contains("sqlite")) {
                createDataStmt = createDataStmt.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            }

            createStmt.executeUpdate(createDataStmt);
        }
    }

    public StoredProfile loadProfile(String name) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement loadStmt = con.prepareStatement(LOAD_BY_NAME)
        ) {
            loadStmt.setString(1, name);

            try (ResultSet resultSet = loadStmt.executeQuery()) {
                return parseResult(resultSet).orElseGet(() -> new StoredProfile(null, name, false, ""));
            }
        } catch (SQLException sqlEx) {
            core.getPlugin().getLog().error("Failed to query profile: {}", name, sqlEx);
        }

        return null;
    }

    public StoredProfile loadProfile(UUID uuid) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement loadStmt = con.prepareStatement(LOAD_BY_UUID)) {
            loadStmt.setString(1, UUIDAdapter.toMojangId(uuid));

            try (ResultSet resultSet = loadStmt.executeQuery()) {
                return parseResult(resultSet).orElse(null);
            }
        } catch (SQLException sqlEx) {
            core.getPlugin().getLog().error("Failed to query profile: {}", uuid, sqlEx);
        }

        return null;
    }

    private Optional<StoredProfile> parseResult(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            long userId = resultSet.getInt(1);

            UUID uuid = Optional.ofNullable(resultSet.getString(2)).map(UUIDAdapter::parseId).orElse(null);

            String name = resultSet.getString(3);
            boolean premium = resultSet.getBoolean(4);
            String lastIp = resultSet.getString(5);
            Instant lastLogin = resultSet.getTimestamp(6).toInstant();
            return Optional.of(new StoredProfile(userId, uuid, name, premium, lastIp, lastLogin));
        }

        return Optional.empty();
    }

    public void save(StoredProfile playerProfile) {
        try (Connection con = dataSource.getConnection()) {
            String uuid = playerProfile.getOptId().map(UUIDAdapter::toMojangId).orElse(null);

            if (playerProfile.isSaved()) {
                try (PreparedStatement saveStmt = con.prepareStatement(UPDATE_PROFILE)) {
                    saveStmt.setString(1, uuid);
                    saveStmt.setString(2, playerProfile.getName());
                    saveStmt.setBoolean(3, playerProfile.isPremium());
                    saveStmt.setString(4, playerProfile.getLastIp());

                    saveStmt.setLong(5, playerProfile.getRowId());
                    saveStmt.execute();
                }
            } else {
                try (PreparedStatement saveStmt = con.prepareStatement(INSERT_PROFILE, RETURN_GENERATED_KEYS)) {
                    saveStmt.setString(1, uuid);

                    saveStmt.setString(2, playerProfile.getName());
                    saveStmt.setBoolean(3, playerProfile.isPremium());
                    saveStmt.setString(4, playerProfile.getLastIp());

                    saveStmt.execute();
                    try (ResultSet generatedKeys = saveStmt.getGeneratedKeys()) {
                        if (generatedKeys != null && generatedKeys.next()) {
                            playerProfile.setRowId(generatedKeys.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            core.getPlugin().getLog().error("Failed to save playerProfile {}", playerProfile, ex);
        }
    }

    public void close() {
        dataSource.close();
    }
}
