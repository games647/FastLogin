package com.github.games647.fastlogin.bungee;

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
        String username = connection.getName();
        try {
            PlayerProfile playerProfile = plugin.getCore().getStorage().getProfile(username, true);
            if (playerProfile != null) {
                if (playerProfile.isPremium()) {
                    if (playerProfile.getUserId() != -1) {
                        connection.setOnlineMode(true);
                    }
                } else if (playerProfile.getUserId() == -1) {
                    //user not exists in the db
                    BungeeAuthPlugin authPlugin = plugin.getBungeeAuthPlugin();
                    if (plugin.getConfiguration().getBoolean("autoRegister")
                            && (authPlugin == null || !authPlugin.isRegistered(username))) {
                        UUID premiumUUID = plugin.getCore().getMojangApiConnector().getPremiumUUID(username);
                        if (premiumUUID != null) {
                            plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
                            connection.setOnlineMode(true);
                            plugin.getPendingAutoRegister().put(connection, new Object());
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
