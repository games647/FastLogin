package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthHook;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * BungeeCord version of FastLogin. This plugin keeps track on online mode connections.
 */
public class FastLoginBungee extends Plugin {

    public static UUID parseId(String withoutDashes) {
        return UUID.fromString(withoutDashes.substring(0, 8)
                + "-" + withoutDashes.substring(8, 12)
                + "-" + withoutDashes.substring(12, 16)
                + "-" + withoutDashes.substring(16, 20)
                + "-" + withoutDashes.substring(20, 32));
    }

    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();

    private BungeeAuthPlugin bungeeAuthPlugin;
    private final MojangApiConnector mojangApiConnector = new MojangApiConnector(this);
    private Storage storage;
    private Configuration configuration;

    private final Random random = new Random();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException ioExc) {
                getLogger().log(Level.SEVERE, "Error saving default config", ioExc);
            }
        }

        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            String driver = configuration.getString("driver");
            String host = configuration.getString("host", "");
            int port = configuration.getInt("port", 3306);
            String database = configuration.getString("database");

            String username = configuration.getString("username", "");
            String password = configuration.getString("password", "");
            storage = new Storage(this, driver, host, port, database, username, password);
            try {
                storage.createTables();
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Failed to setup database. Disabling plugin...", ex);
                return;
            }

        } catch (IOException ioExc) {
            getLogger().log(Level.SEVERE, "Error loading config. Disabling plugin...", ioExc);
            return;
        }

        //events
        getProxy().getPluginManager().registerListener(this, new PlayerConnectionListener(this));

        //this is required to listen to messages from the server
        getProxy().registerChannel(getDescription().getName());

        registerHook();
    }

    public String generateStringPassword() {
        StringBuilder generatedPassword = new StringBuilder(8);
        for (int i = 1; i <= 8; i++) {
            generatedPassword.append(CHARACTERS[random.nextInt(CHARACTERS.length - 1)]);
        }

        return generatedPassword.toString();
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Storage getStorage() {
        return storage;
    }

    public MojangApiConnector getMojangApiConnector() {
        return mojangApiConnector;
    }

    /**
     * Get the auth plugin hook for BungeeCord
     *
     * @return the auth hook for BungeeCord. null if none found
     */
    public BungeeAuthPlugin getBungeeAuthPlugin() {
        return bungeeAuthPlugin;
    }

    private void registerHook() {
        Plugin plugin = getProxy().getPluginManager().getPlugin("BungeeAuth");
        if (plugin != null) {
            bungeeAuthPlugin = new BungeeAuthHook();
            getLogger().info("Hooked into BungeeAuth");
        }
    }
}
