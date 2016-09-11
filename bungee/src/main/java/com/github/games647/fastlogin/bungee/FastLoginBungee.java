package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthHook;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.github.games647.fastlogin.bungee.listener.PlayerConnectionListener;
import com.github.games647.fastlogin.bungee.listener.PluginMessageListener;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

    private final BungeeCore loginCore = new BungeeCore(this);
    private BungeeAuthPlugin bungeeAuthPlugin;
    private Configuration config;

    private final Set<UUID> pendingConfirms = Sets.newHashSet();

    private final ConcurrentMap<PendingConnection, BungeeLoginSession> session = Maps.newConcurrentMap();

    @Override
    public void onEnable() {
        loginCore.loadConfig();
        loginCore.loadMessages();

        try {
            File configFile = new File(getDataFolder(), "config.yml");
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            List<String> ipAddresses = getConfig().getStringList("ip-addresses");
            int requestLimit = getConfig().getInt("mojang-request-limit");
            MojangApiBungee mojangApi = new MojangApiBungee(getLogger(), ipAddresses, requestLimit);
            loginCore.setMojangApiConnector(mojangApi);

            if (!loginCore.setupDatabase()) {
                return;
            }
        } catch (IOException ioExc) {
            getLogger().log(Level.SEVERE, "Error loading config. Disabling plugin...", ioExc);
            return;
        }

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
        loginCore.close();
    }

    public BungeeCore getCore() {
        return loginCore;
    }

    public void setAuthPluginHook(BungeeAuthPlugin authPlugin) {
        this.bungeeAuthPlugin = authPlugin;
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
