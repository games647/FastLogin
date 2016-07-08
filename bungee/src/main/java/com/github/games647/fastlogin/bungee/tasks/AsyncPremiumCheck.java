package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
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
            if (profile == null) {
                return;
            }

            if (profile.getUserId() == -1) {
                UUID premiumUUID = null;
                if (plugin.getConfig().getBoolean("nameChangeCheck") || plugin.getConfig().getBoolean("autoRegister")
                        || plugin.getConfig().getBoolean("protectPremiumUserName")) {
                    premiumUUID = plugin.getCore().getMojangApiConnector().getPremiumUUID(username);
                }

                if (premiumUUID == null
                        || !checkNameChange(premiumUUID, connection, username)
                        || !checkPremiumName(username, connection, profile)
                        || !protectPremiumUserName(username, connection, profile)) {
                    //nothing detected the player as premium -> start a cracked session
                    plugin.getSession().put(connection, new BungeeLoginSession(username, false, profile));
                }
            } else if (profile.isPremium()) {
                requestPremiumLogin(connection, profile, username, true);
            } else {
                //Cracked session
                plugin.getSession().put(connection, new BungeeLoginSession(username, false, profile));
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check premium state", ex);
        } finally {
            preLoginEvent.completeIntent(plugin);
        }
    }

    private boolean protectPremiumUserName(String username, PendingConnection connection, PlayerProfile profile) {
        if (plugin.getConfig().getBoolean("protectPremiumUserName")) {
            plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
            requestPremiumLogin(connection, profile, username, false);
            return true;
        }

        return false;
    }

    private boolean checkPremiumName(String username, PendingConnection connection, PlayerProfile profile)
            throws Exception {
        BungeeAuthPlugin authPlugin = plugin.getBungeeAuthPlugin();
        if (plugin.getConfig().getBoolean("autoRegister")
                && (authPlugin == null || !authPlugin.isRegistered(username))) {
            plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
            requestPremiumLogin(connection, profile, username, false);
            return true;
        }

        return false;
    }

    private boolean checkNameChange(UUID premiumUUID, PendingConnection connection, String username) {
        //user not exists in the db
        if (plugin.getConfig().getBoolean("nameChangeCheck")) {
            PlayerProfile profile = plugin.getCore().getStorage().loadProfile(premiumUUID);
            if (profile != null) {
                //uuid exists in the database
                plugin.getLogger().log(Level.FINER, "Player {0} changed it's username", premiumUUID);
                requestPremiumLogin(connection, profile, username, false);
                return true;
            }
        }

        return false;
    }

    private void requestPremiumLogin(PendingConnection con, PlayerProfile profile, String username, boolean register) {
        con.setOnlineMode(true);
        plugin.getSession().put(con, new BungeeLoginSession(username, register, profile));
    }
}
