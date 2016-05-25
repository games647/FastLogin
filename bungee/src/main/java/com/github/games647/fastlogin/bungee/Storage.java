package com.github.games647.fastlogin.bungee;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;

public class Storage {

    private static final String PREMIUM_TABLE = "premium";

    private final ConcurrentMap<String, PlayerProfile> profileCache = CacheBuilder
            .<String, PlayerProfile>newBuilder()
            .concurrencyLevel(20)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(new CacheLoader<String, PlayerProfile>() {
                @Override
                public PlayerProfile load(String key) throws Exception {
                    //should be fetched manually
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }).asMap();

    private final HikariDataSource dataSource;
    private final FastLoginBungee plugin;

    public Storage(FastLoginBungee plugin, String driver, String host, int port, String databasePath
            , String user, String pass) {
        this.plugin = plugin;

        HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setUsername(user);
        databaseConfig.setPassword(pass);
        databaseConfig.setDriverClassName(driver);
        String pluginName = plugin.getDescription().getName();

        //set a custom thread factory to remove BungeeCord warning about different threads
        databaseConfig.setThreadFactory(new ThreadFactoryBuilder()
                .setNameFormat(pluginName + " Database Pool Thread #%1$d")
                //Hikari create daemons by default
                .setDaemon(true)
                .setThreadFactory(new GroupedThreadFactory(plugin, pluginName)).build());

        databasePath = databasePath.replace("{pluginDir}", plugin.getDataFolder().getAbsolutePath());

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
                    + "`UserID` INTEGER PRIMARY KEY AUTO_INCREMENT, "
                    + "`UUID` CHAR(36), "
                    + "`Name` VARCHAR(16) NOT NULL, "
                    + "`Premium` BOOLEAN NOT NULL, "
                    + "`LastIp` VARCHAR(255) NOT NULL, "
                    + "`LastLogin` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE (`UUID`), "
                    //the premium shouldn't steal the cracked account by changing the name
                    + "UNIQUE (`Name`) "
                    + ")";

            if (dataSource.getJdbcUrl().contains("sqlite")) {
                createDataStmt = createDataStmt.replace("AUTO_INCREMENT", "AUTOINCREMENT");
            }

            statement.executeUpdate(createDataStmt);
        } finally {
            closeQuietly(con);
        }
    }

    public PlayerProfile getProfile(String name, boolean fetch) {
        if (profileCache.containsKey(name)) {
            return profileCache.get(name);
        } else if (fetch) {
            Connection con = null;
            try {
                con = dataSource.getConnection();
                PreparedStatement loadStatement = con.prepareStatement("SELECT * FROM " + PREMIUM_TABLE
                        + " WHERE `Name`=? LIMIT 1");
                loadStatement.setString(1, name);

                ResultSet resultSet = loadStatement.executeQuery();
                if (resultSet.next()) {
                    long userId = resultSet.getInt(1);

                    String unparsedUUID = resultSet.getString(2);
                    UUID uuid;
                    if (unparsedUUID == null) {
                        uuid = null;
                    } else {
                        uuid = FastLoginBungee.parseId(unparsedUUID);
                    }

//                    String name = resultSet.getString(3);
                    boolean premium = resultSet.getBoolean(4);
                    String lastIp = resultSet.getString(5);
                    long lastLogin = resultSet.getTimestamp(6).getTime();
                    PlayerProfile playerProfile = new PlayerProfile(userId, uuid, name, premium, lastIp, lastLogin);
                    profileCache.put(name, playerProfile);
                    return playerProfile;
                } else {
                    PlayerProfile crackedProfile = new PlayerProfile(null, name, false, "");
                    profileCache.put(name, crackedProfile);
                    return crackedProfile;
                }
            } catch (SQLException sqlEx) {
                plugin.getLogger().log(Level.SEVERE, "Failed to query profile", sqlEx);
            } finally {
                closeQuietly(con);
            }
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
//                saveStatement.setTimestamp(5, new Timestamp(playerProfile.getLastLogin()));

                saveStatement.setLong(5, playerProfile.getUserId());
                saveStatement.execute();
            }

            return true;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save playerProfile", ex);
        } finally {
            closeQuietly(con);
        }

        return false;
    }

    public void close() {
        dataSource.close();
        profileCache.clear();
    }

    private void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException sqlEx) {
                plugin.getLogger().log(Level.SEVERE, "Failed to close connection", sqlEx);
            }
        }
    }
}
