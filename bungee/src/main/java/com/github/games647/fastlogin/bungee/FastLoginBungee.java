package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthHook;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.github.games647.fastlogin.bungee.listener.PlayerConnectionListener;
import com.github.games647.fastlogin.bungee.listener.PluginMessageListener;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * BungeeCord version of FastLogin. This plugin keeps track on online mode connections.
 */
public class FastLoginBungee extends Plugin {

    private final ConcurrentMap<PendingConnection, BungeeLoginSession> session = Maps.newConcurrentMap();

    private BungeeCore core;
    private Configuration config;

    @Override
    public void onEnable() {
        saveDefaultFile("config.yml");

        try {
            File configFile = new File(getDataFolder(), "config.yml");
            ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
            Configuration defaults = provider.load(getResourceAsStream("config.yml"));
            config = provider.load(configFile, defaults);

            core = new BungeeCore(this, config);
            if (!core.setupDatabase()) {
                return;
            }
        } catch (IOException ioExc) {
            getLogger().log(Level.SEVERE, "Error loading config. Disabling plugin...", ioExc);
            return;
        }

        core.setApiConnector();
        core.loadMessages();

        //events
        getProxy().getPluginManager().registerListener(this, new PlayerConnectionListener(this));
        getProxy().getPluginManager().registerListener(this, new PluginMessageListener(this));

        //bungee only commands
        getProxy().getPluginManager().registerCommand(this, new ImportCommand(this));

        //this is required to listen to messages from the server
        getProxy().registerChannel(getDescription().getName());

        registerHook();
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.close();
        }
    }

    public void saveDefaultFile(String fileName) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            InputStream in = getResourceAsStream(fileName);
            try {
                Files.copy(in, configFile.toPath());
            } catch (IOException ioExc) {
                getLogger().log(Level.SEVERE, "Error saving default " + fileName, ioExc);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    getLogger().log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public BungeeCore getCore() {
        return core;
    }

    @Deprecated
    public void setAuthPluginHook(BungeeAuthPlugin authPlugin) {
        core.setAuthPluginHook(authPlugin);
    }

    public Configuration getConfig() {
        return config;
    }

    public ConcurrentMap<PendingConnection, BungeeLoginSession> getSession() {
        return session;
    }

    /**
     * Get the auth plugin hook for BungeeCord
     *
     * @return the auth hook for BungeeCord. null if none found
     */
    @Deprecated
    public BungeeAuthPlugin getBungeeAuthPlugin() {
        return (BungeeAuthPlugin) core.getAuthPluginHook();
    }

    private void registerHook() {
        Plugin plugin = getProxy().getPluginManager().getPlugin("BungeeAuth");
        if (plugin != null) {
            core.setAuthPluginHook(new BungeeAuthHook());
            getLogger().info("Hooked into BungeeAuth");
        }
    }
}
