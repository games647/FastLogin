package com.github.games647.fastlogin.core;

import com.github.games647.fastlogin.core.importer.AutoInImporter;
import com.github.games647.fastlogin.core.importer.ImportPlugin;
import com.github.games647.fastlogin.core.importer.Importer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class FastLoginCore {
    
    public static UUID parseId(String withoutDashes) {
        if (withoutDashes == null) {
            return null;
        }

        return UUID.fromString(withoutDashes.substring(0, 8)
                + "-" + withoutDashes.substring(8, 12)
                + "-" + withoutDashes.substring(12, 16)
                + "-" + withoutDashes.substring(16, 20)
                + "-" + withoutDashes.substring(20, 32));
    }

    protected final Map<String, String> localeMessages = new ConcurrentHashMap<>();
    private MojangApiConnector mojangApiConnector;
    private AuthStorage storage;

    public void setMojangApiConnector(MojangApiConnector mojangApiConnector) {
        this.mojangApiConnector = mojangApiConnector;
    }

    public MojangApiConnector getMojangApiConnector() {
        return mojangApiConnector;
    }

    public AuthStorage getStorage() {
        return storage;
    }

    public abstract File getDataFolder();

    public abstract Logger getLogger();

    public abstract ThreadFactory getThreadFactory();

    public String getMessage(String key) {
        return localeMessages.get(key);
    }

    public abstract void loadMessages();

    public abstract void loadConfig();

    public boolean setupDatabase(String driver, String host, int port, String database, String user, String password) {
        storage = new AuthStorage(this, driver, host, port, database, user, password);
        try {
            storage.createTables();
            return true;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to setup database. Disabling plugin...", ex);
            return false;
        }
    }

    public boolean importDatabase(ImportPlugin plugin, boolean sqlite, AuthStorage storage, String host, String database
            , String username, String pass) {
        if (sqlite && (plugin == ImportPlugin.BPA || plugin == ImportPlugin.ELDZI)) {
            throw new IllegalArgumentException("These plugins doesn't support flat file databases");
        }

        Importer importer;
        try {
            importer = plugin.getImporter().newInstance();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Couldn't not setup importer class", ex);
            return false;
        } 

        try {
            if (sqlite && plugin == ImportPlugin.AUTO_IN) {
                //load sqlite driver
                Class.forName("org.sqlite.JDBC");

                String jdbcUrl = "jdbc:sqlite:" + AutoInImporter.getSQLitePath();
                Connection con = DriverManager.getConnection(jdbcUrl);
                importer.importData(con, storage.getDataSource(), storage);
                return true;
            } else {
                Class.forName("com.mysql.jdbc.Driver");

                String jdbcUrl = "jdbc:mysql://" + host + "/" + database;
                Connection con = DriverManager.getConnection(jdbcUrl, username, pass);
                importer.importData(con, storage.getDataSource(), storage);
                return true;
            }
        } catch (ClassNotFoundException ex) {
            getLogger().log(Level.SEVERE, "Cannot find SQL driver. Do you removed it?", ex);
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Couldn't import data. Aborting...", ex);
        }

        return false;
    }

    public void close() {
        if (storage != null) {
            storage.close();
        }
    }
}
