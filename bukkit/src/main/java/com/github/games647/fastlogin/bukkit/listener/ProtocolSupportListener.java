package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerPropertiesResolveEvent;

public class ProtocolSupportListener implements Listener {

    protected final FastLoginBukkit plugin;

    public ProtocolSupportListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        plugin.setServerStarted();
        if (loginStartEvent.isLoginDenied()) {
            return;
        }

        String username = loginStartEvent.getName();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(loginStartEvent.getAddress().toString());

        BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();
        if (authPlugin == null) {
            return;
        }

        PlayerProfile profile = plugin.getCore().getStorage().loadProfile(username);
        if (profile != null) {
            if (profile.getUserId() == -1) {
                UUID premiumUUID = null;
                if (plugin.getConfig().getBoolean("nameChangeCheck") || plugin.getConfig().getBoolean("autoRegister")) {
                    premiumUUID = plugin.getCore().getMojangApiConnector().getPremiumUUID(username);
                }

                //user not exists in the db
                try {
                    if (plugin.getConfig().getBoolean("nameChangeCheck")) {
                        profile = plugin.getCore().getStorage().loadProfile(premiumUUID);
                        if (profile != null) {
                            plugin.getLogger().log(Level.FINER, "Player {0} changed it's username", premiumUUID);
                            startPremiumSession(username, loginStartEvent, false, profile);
                            return;
                        }
                    }

                    if (plugin.getConfig().getBoolean("autoRegister") && !authPlugin.isRegistered(username)) {
                        plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
                        startPremiumSession(username, loginStartEvent, false, profile);
                        return;
                    }

                    //no premium check passed so we save it as a cracked player
                    BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
                    plugin.getSessions().put(loginStartEvent.getAddress().toString(), loginSession);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to query isRegistered", ex);
                }
            } else if (profile.isPremium()) {
                startPremiumSession(username, loginStartEvent, true, profile);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPropertiesResolve(PlayerPropertiesResolveEvent propertiesResolveEvent) {
        //skin was resolved -> premium player
        if (propertiesResolveEvent.hasProperty("textures")) {
            InetSocketAddress address = propertiesResolveEvent.getAddress();
            BukkitLoginSession session = plugin.getSessions().get(address.toString());
            if (session != null) {
                session.setVerified(true);
            }
        }
    }

    private void startPremiumSession(String playerName, PlayerLoginStartEvent loginStartEvent, boolean registered, PlayerProfile playerProfile) {
        loginStartEvent.setOnlineMode(true);
        InetSocketAddress address = loginStartEvent.getAddress();

        BukkitLoginSession playerSession = new BukkitLoginSession(playerName, null, null, registered, playerProfile);
        plugin.getSessions().put(address.toString(), playerSession);
        if (plugin.getConfig().getBoolean("premiumUuid")) {
            loginStartEvent.setUseOnlineModeUUID(true);
        }
    }
}
