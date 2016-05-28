package com.github.games647.fastlogin.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class Storage {

    private static final String PREMIUM_TABLE = "premium";

    private final FastLoginCore core;
    private final HikariDataSource dataSource;

    public Storage(FastLoginCore core, String driver, String host, int port, String databasePath
            , String user, String pass) {
        this.core = core;

        HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setUsername(user);
        databaseConfig.setPassword(pass);
        databaseConfig.setDriverClassName(driver);
        databaseConfig.setThreadFactory(core.getThreadFactory());

        databasePath = databasePath.replace("{pluginDir}", core.getDataFolder().getAbsolutePath());

        databaseConfig.setThreadFactory(core.getThreadFactory());

        String jdbcUrl = "jdbc:";
        if (driver.contains("sqlite")) {
            jdbcUrl += "sqlite" + "://" + databasePath;
            databaseConfig.setConnectionTestQuery("SELECT 1");
        } else {
            jdbcUrl += "mysql" + "://" + host + ':' + port + '/' + databasePath;
        }

        databaseConfig.setJdbcUrl(jdbcUrl);
        this.dataSource = new HikariDataSource(databaseConfig);
    }

    public void createTables() throws SQLException {
        Connection con = null;
        try {
            con = dataSource.getConnection();
            Statement statement = con.createStatement();
            String createDataStmt = "CREATE TABLE IF NOT EXISTS " + PREMIUM_TABLE + " ("
                    + "UserID INTEGER PRIMARY KEY AUTO_INCREMENT, "
                    + "UUID CHAR(36), "
                    + "Name VARCHAR(16) NOT NULL, "
                    + "Premium BOOLEAN NOT NULL, "
                    + "LastIp VARCHAR(255) NOT NULL, "
                    + "LastLogin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE (UUID), "
                    //the premium shouldn't steal the cracked account by changing the name
                    + "UNIQUE (Name) "
                    + ")";

            if (dataSource.getJdbcUrl().contains("sqlite")) {
                createDataStmt = createDataStmt.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            }

            statement.executeUpdate(createDataStmt);
        } finally {
            closeQuietly(con);
        }
    }

    public PlayerProfile loadProfile(String name) {
        Connection con = null;
        try {
            con = dataSource.getConnection();
            PreparedStatement loadStatement = con.prepareStatement("SELECT * FROM " + PREMIUM_TABLE
                    + " WHERE Name=? LIMIT 1");
            loadStatement.setString(1, name);

            ResultSet resultSet = loadStatement.executeQuery();
            if (resultSet.next()) {
                long userId = resultSet.getInt(1);

                String unparsedUUID = resultSet.getString(2);
                UUID uuid;
                if (unparsedUUID == null) {
                    uuid = null;
                } else {
                    uuid = FastLoginCore.parseId(unparsedUUID);
                }

                boolean premium = resultSet.getBoolean(4);
                String lastIp = resultSet.getString(5);
                long lastLogin = resultSet.getTimestamp(6).getTime();
                PlayerProfile playerProfile = new PlayerProfile(userId, uuid, name, premium, lastIp, lastLogin);
                return playerProfile;
            } else {
                PlayerProfile crackedProfile = new PlayerProfile(null, name, false, "");
                return crackedProfile;
            }
        } catch (SQLException sqlEx) {
            core.getLogger().log(Level.SEVERE, "Failed to query profile", sqlEx);
        } finally {
            closeQuietly(con);
        }

        return null;
    }

    public boolean save(PlayerProfile playerProfile) {
        Connection con = null;
        try {
            con = dataSource.getConnection();

            UUID uuid = playerProfile.getUuid();

            if (playerProfile.getUserId() == -1) {
                PreparedStatement saveStatement = con.prepareStatement("INSERT INTO " + PREMIUM_TABLE
                        + " (UUID, Name, Premium, LastIp) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                if (uuid == null) {
                    saveStatement.setString(1, null);
                } else {
                    saveStatement.setString(1, uuid.toString().replace("-", ""));
                }

                saveStatement.setString(2, playerProfile.getPlayerName());
                saveStatement.setBoolean(3, playerProfile.isPremium());
                saveStatement.setString(4, playerProfile.getLastIp());
                saveStatement.execute();

                ResultSet generatedKeys = saveStatement.getGeneratedKeys();
                if (generatedKeys != null && generatedKeys.next()) {
                    playerProfile.setUserId(generatedKeys.getInt(1));
                }
            } else {
                PreparedStatement saveStatement = con.prepareStatement("UPDATE " + PREMIUM_TABLE
                        + " SET UUID=?, Name=?, Premium=?, LastIp=?, LastLogin=CURRENT_TIMESTAMP WHERE UserID=?");

                if (uuid == null) {
                    saveStatement.setString(1, null);
                } else {
                    saveStatement.setString(1, uuid.toString().replace("-", ""));
                }

                saveStatement.setString(2, playerProfile.getPlayerName());
                saveStatement.setBoolean(3, playerProfile.isPremium());
                saveStatement.setString(4, playerProfile.getLastIp());

                saveStatement.setLong(5, playerProfile.getUserId());
                saveStatement.execute();
            }

            return true;
        } catch (SQLException ex) {
            core.getLogger().log(Level.SEVERE, "Failed to save playerProfile", ex);
        } finally {
            closeQuietly(con);
        }

        return false;
    }

    public void close() {
        dataSource.close();
    }

    private void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException sqlEx) {
                core.getLogger().log(Level.SEVERE, "Failed to close connection", sqlEx);
            }
        }
    }
}
