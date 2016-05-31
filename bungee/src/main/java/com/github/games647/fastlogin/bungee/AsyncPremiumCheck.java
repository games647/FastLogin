package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.github.games647.fastlogin.core.LoginSession;
import com.github.games647.fastlogin.core.PlayerProfile;

import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;

public class AsyncPremiumCheck implements Runnable {

    private final FastLoginBungee plugin;
    private final PreLoginEvent preLoginEvent;

    public AsyncPremiumCheck(FastLoginBungee plugin, PreLoginEvent preLoginEvent) {
        this.plugin = plugin;
        this.preLoginEvent = preLoginEvent;
    }

    @Override
    public void run() {
        PendingConnection connection = preLoginEvent.getConnection();
        plugin.getSession().remove(connection);

        String username = connection.getName();
        try {
            PlayerProfile profile = plugin.getCore().getStorage().loadProfile(username);
            if (profile != null) {
                if (profile.isPremium()) {
                    if (profile.getUserId() != -1) {
                        plugin.getSession().put(connection, new LoginSession(username, true, profile));
                        connection.setOnlineMode(true);
                    }
                } else if (profile.getUserId() == -1) {
                    //user not exists in the db
                    BungeeAuthPlugin authPlugin = plugin.getBungeeAuthPlugin();
                    if (plugin.getConfiguration().getBoolean("nameChangeCheck")) {
                        UUID premiumUUID = plugin.getCore().getMojangApiConnector().getPremiumUUID(username);
                        if (premiumUUID != null) {
                            profile = plugin.getCore().getStorage().loadProfile(premiumUUID);
                            if (profile != null) {
                                plugin.getLogger().log(Level.FINER, "Player {0} changed it's username", premiumUUID);
                                connection.setOnlineMode(true);
                                plugin.getSession().put(connection, new LoginSession(username, false, profile));
                            }
                        }
                    }

                    if (plugin.getConfiguration().getBoolean("autoRegister")
                            && (authPlugin == null || !authPlugin.isRegistered(username))) {
                        UUID premiumUUID = plugin.getCore().getMojangApiConnector().getPremiumUUID(username);
                        if (premiumUUID != null) {
                            plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
                            connection.setOnlineMode(true);
                            plugin.getSession().put(connection, new LoginSession(username, false, profile));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check premium state", ex);
        } finally {
            preLoginEvent.completeIntent(plugin);
        }
    }
}
