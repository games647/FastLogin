package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthHook;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.google.common.collect.Sets;

import java.util.Set;

import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord version of FastLogin. This plugin keeps track
 * on online mode connections.
 */
public class FastLoginBungee extends Plugin {

    private final Set<String> enabledPremium = Sets.newConcurrentHashSet();
    private BungeeAuthPlugin bungeeAuthPlugin;

    @Override
    public void onEnable() {
        //events
        getProxy().getPluginManager().registerListener(this, new PlayerConnectionListener(this));

        //this is required to listen to messages from the server
        getProxy().registerChannel(getDescription().getName());

        registerHook();
    }

    /**
     * A set of players who want to use fastlogin
     *
     * @return all player which want to be in onlinemode
     */
    public Set<String> getEnabledPremium() {
        return enabledPremium;
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
