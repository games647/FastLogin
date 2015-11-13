package com.github.games647.fastloginbungee;

import com.google.common.collect.Sets;

import java.util.Set;

import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord version of FastLogin. This plugin keeps track
 * on online mode connections.
 */
public class FastLogin extends Plugin {

    private final Set<String> enabledPremium = Sets.newConcurrentHashSet();

    @Override
    public void onEnable() {
        //events
        getProxy().getPluginManager().registerListener(this, new PlayerConnectionListener(this));

        //commands
        getProxy().getPluginManager().registerCommand(this, new PremiumCommand(this));
    }

    /**
     * A set of players who want to use fastlogin
     *
     * @return all player which want to be in onlinemode
     */
    public Set<String> getEnabledPremium() {
        return enabledPremium;
    }
}
