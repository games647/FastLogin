package com.github.games647.fastlogin.core;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class FastLoginCore {
    
    public static UUID parseId(String withoutDashes) {
        return UUID.fromString(withoutDashes.substring(0, 8)
                + "-" + withoutDashes.substring(8, 12)
                + "-" + withoutDashes.substring(12, 16)
                + "-" + withoutDashes.substring(16, 20)
                + "-" + withoutDashes.substring(20, 32));
    }

    private MojangApiConnector mojangApiConnector;
    private Storage storage;

    public void setMojangApiConnector(MojangApiConnector mojangApiConnector) {
        this.mojangApiConnector = mojangApiConnector;
    }

    public MojangApiConnector getMojangApiConnector() {
        return mojangApiConnector;
    }

    public Storage getStorage() {
        return storage;
    }

    public abstract File getDataFolder();

    public abstract Logger getLogger();

    public abstract ThreadFactory getThreadFactory();

    public boolean setupDatabase(String driver, String host, int port, String database, String user, String password) {
        storage = new Storage(this, driver, host, port, database, user, password);
        try {
            storage.createTables();
            return true;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to setup database. Disabling plugin...", ex);
            return false;
        }
    }

    public void close() {
        if (storage != null) {
            storage.close();
        }
    }
}
