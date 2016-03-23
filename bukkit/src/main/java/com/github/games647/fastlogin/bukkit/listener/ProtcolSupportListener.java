package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerSession;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;

import java.net.InetSocketAddress;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerPropertiesResolveEvent;

public class ProtcolSupportListener implements Listener {

    protected final FastLoginBukkit plugin;

    public ProtcolSupportListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        if (loginStartEvent.isLoginDenied()) {
            return;
        }

        String playerName = loginStartEvent.getName();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(playerName);

        BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();
        if (plugin.getEnabledPremium().contains(playerName)) {
            //the player have to be registered in order to invoke the command
            startPremiumSession(playerName, loginStartEvent, true);
        } else if (plugin.getConfig().getBoolean("autoRegister")
                && authPlugin != null && !plugin.getAuthPlugin().isRegistered(playerName)) {
            startPremiumSession(playerName, loginStartEvent, false);
            plugin.getEnabledPremium().add(playerName);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPropertiesResolve(PlayerPropertiesResolveEvent propertiesResolveEvent) {
        //skin was resolved -> premium player
        if (propertiesResolveEvent.hasProperty("textures")) {
            InetSocketAddress address = propertiesResolveEvent.getAddress();
            PlayerSession session = plugin.getSessions().get(address.toString());
            if (session != null) {
                session.setVerified(true);
            }
        }
    }

    private void startPremiumSession(String playerName, PlayerLoginStartEvent loginStartEvent, boolean registered) {
        if (plugin.getApiConnector().isPremiumName(playerName)) {
            loginStartEvent.setOnlineMode(true);
            InetSocketAddress address = loginStartEvent.getAddress();

            PlayerSession playerSession = new PlayerSession(playerName, null, null);
            playerSession.setRegistered(registered);
            plugin.getSessions().put(address.toString(), playerSession);
            if (plugin.getConfig().getBoolean("premiumUuid")) {
                loginStartEvent.setUseOnlineModeUUID(true);
            }
        }
    }
}
