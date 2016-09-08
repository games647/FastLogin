package com.github.games647.fastlogin.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

public class AuthStorage {

    private static final String PREMIUM_TABLE = "premium";

    private final FastLoginCore core;
    private final HikariDataSource dataSource;

    //a try to fix https://www.spigotmc.org/threads/fastlogin.101192/page-26#post-1874647
    private final Calendar calendar = Calendar.getInstance(Locale.US);

    public AuthStorage(FastLoginCore core, String driver, String host, int port, String databasePath
            , String user, String pass) {
        this.core = core;

        HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setUsername(user);
        databaseConfig.setPassword(pass);
        databaseConfig.setDriverClassName(driver);
        databaseConfig.setThreadFactory(core.getThreadFactory());

        databasePath = databasePath.replace("{pluginDir}", core.getDataFolder().getAbsolutePath());

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

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void createTables() throws SQLException {
        Connection con = null;
        Statement createStmt = null;
        try {
            con = dataSource.getConnection();
            createStmt = con.createStatement();

            String createDataStmt = "CREATE TABLE IF NOT EXISTS " + PREMIUM_TABLE + " ("
                    + "UserID INTEGER PRIMARY KEY AUTO_INCREMENT, "
                    + "UUID CHAR(36), "
                    + "Name VARCHAR(16) NOT NULL, "
                    + "Premium BOOLEAN NOT NULL, "
                    + "LastIp VARCHAR(255) NOT NULL, "
                    + "LastLogin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    //the premium shouldn't steal the cracked account by changing the name
                    + "UNIQUE (Name) "
                    + ")";

            if (dataSource.getJdbcUrl().contains("sqlite")) {
                createDataStmt = createDataStmt.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            }

            createStmt.executeUpdate(createDataStmt);

            //drop the old unique uuid index
            try {
                if (dataSource.getJdbcUrl().contains("sqlite")) {
                    String tempTableCreate = createDataStmt.replace(PREMIUM_TABLE, PREMIUM_TABLE + "_TEMP")
                            //if we already imported the table fail here
                            .replace("IF NOT EXISTS", "");
                    //create a temp table insert it there and then back
                    createStmt.executeUpdate(tempTableCreate);
                    createStmt.executeUpdate("INSERT INTO " + PREMIUM_TABLE + "_TEMP SELECT * FROM " + PREMIUM_TABLE);

                    createStmt.executeUpdate("DROP TABLE " + PREMIUM_TABLE);
                    createStmt.executeUpdate(createDataStmt);

                    //insert it back into the new table
                    createStmt.executeUpdate("INSERT INTO " + PREMIUM_TABLE + " SELECT * FROM " + PREMIUM_TABLE + "_TEMP");
                } else {
                    createStmt.executeUpdate("ALTER TABLE premium DROP INDEX UUID");
                }
            } catch (SQLException sqlEx) {
                core.getLogger().log(Level.FINE, "Error dropping unique uuid index", sqlEx);
            }

            try {
                createStmt.executeUpdate("CREATE INDEX uuid_idx on premium (UUID)");
            } catch (SQLException sqlEx) {
                core.getLogger().log(Level.FINE, "Error creating uuid index", sqlEx);
            }
        } finally {
            closeQuietly(con);
            closeQuietly(createStmt);
        }
    }

    public PlayerProfile loadProfile(String name) {
        Connection con = null;
        PreparedStatement loadStmt = null;
        ResultSet resultSet = null;
        try {
            con = dataSource.getConnection();
            loadStmt = con.prepareStatement("SELECT * FROM " + PREMIUM_TABLE + " WHERE Name=? LIMIT 1");
            loadStmt.setString(1, name);

            resultSet = loadStmt.executeQuery();
            if (resultSet.next()) {
                long userId = resultSet.getInt(1);

                UUID uuid = FastLoginCore.parseId(resultSet.getString(2));

                boolean premium = resultSet.getBoolean(4);
                String lastIp = resultSet.getString(5);
                long lastLogin = resultSet.getTimestamp(6, calendar).getTime();
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
            closeQuietly(loadStmt);
            closeQuietly(resultSet);
        }

        return null;
    }

    public PlayerProfile loadProfile(UUID uuid) {
        Connection con = null;
        PreparedStatement loadStmt = null;
        ResultSet resultSet = null;
        try {
            con = dataSource.getConnection();
            loadStmt = con.prepareStatement("SELECT * FROM " + PREMIUM_TABLE + " WHERE UUID=? LIMIT 1");
            loadStmt.setString(1, uuid.toString().replace("-", ""));

            resultSet = loadStmt.executeQuery();
            if (resultSet.next()) {
                long userId = resultSet.getInt(1);

                String name = resultSet.getString(3);
                boolean premium = resultSet.getBoolean(4);
                String lastIp = resultSet.getString(5);
                long lastLogin = resultSet.getTimestamp(6, calendar).getTime();
                PlayerProfile playerProfile = new PlayerProfile(userId, uuid, name, premium, lastIp, lastLogin);
                return playerProfile;
            }
        } catch (SQLException sqlEx) {
            core.getLogger().log(Level.SEVERE, "Failed to query profile", sqlEx);
        } finally {
            closeQuietly(con);
            closeQuietly(loadStmt);
            closeQuietly(resultSet);
        }

        return null;
    }

    public boolean save(PlayerProfile playerProfile) {
        Connection con = null;
        PreparedStatement updateStmt = null;
        PreparedStatement saveStmt = null;

        ResultSet generatedKeys = null;
        try {
            con = dataSource.getConnection();

            UUID uuid = playerProfile.getUuid();
            
            if (playerProfile.getUserId() == -1) {
                saveStmt = con.prepareStatement("INSERT INTO " + PREMIUM_TABLE
                        + " (UUID, Name, Premium, LastIp) VALUES (?, ?, ?, ?) ", Statement.RETURN_GENERATED_KEYS);

                if (uuid == null) {
                    saveStmt.setString(1, null);
                } else {
                    saveStmt.setString(1, uuid.toString().replace("-", ""));
                }

                saveStmt.setString(2, playerProfile.getPlayerName());
                saveStmt.setBoolean(3, playerProfile.isPremium());
                saveStmt.setString(4, playerProfile.getLastIp());

                saveStmt.execute();

                generatedKeys = saveStmt.getGeneratedKeys();
                if (generatedKeys != null && generatedKeys.next()) {
                    playerProfile.setUserId(generatedKeys.getInt(1));
                }
            } else {
                saveStmt = con.prepareStatement("UPDATE " + PREMIUM_TABLE
                        + " SET UUID=?, Name=?, Premium=?, LastIp=?, LastLogin=CURRENT_TIMESTAMP WHERE UserID=?");

                if (uuid == null) {
                    saveStmt.setString(1, null);
                } else {
                    saveStmt.setString(1, uuid.toString().replace("-", ""));
                }

                saveStmt.setString(2, playerProfile.getPlayerName());
                saveStmt.setBoolean(3, playerProfile.isPremium());
                saveStmt.setString(4, playerProfile.getLastIp());

                saveStmt.setLong(5, playerProfile.getUserId());
                saveStmt.execute();
            }

            return true;
        } catch (SQLException ex) {
            core.getLogger().log(Level.SEVERE, "Failed to save playerProfile", ex);
        } finally {
            closeQuietly(con);
            closeQuietly(updateStmt);
            closeQuietly(saveStmt);
            closeQuietly(generatedKeys);
        }

        return false;
    }

    public void close() {
        dataSource.close();
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception closeEx) {
                core.getLogger().log(Level.SEVERE, "Failed to close connection", closeEx);
            }
        }
    }
}
