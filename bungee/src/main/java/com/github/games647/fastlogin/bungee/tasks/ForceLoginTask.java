package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;
import java.util.logging.Level;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class ForceLoginTask implements Runnable {

    private final FastLoginBungee plugin;
    private final ProxiedPlayer player;
    private final Server server;

    public ForceLoginTask(FastLoginBungee plugin, ProxiedPlayer player, Server server) {
        this.plugin = plugin;
        this.player = player;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            PendingConnection pendingConnection = player.getPendingConnection();
            BungeeLoginSession session = plugin.getSession().get(pendingConnection);

            if (session == null || !player.isConnected()) {
                plugin.getLogger().log(Level.FINE, "Invalid session player {0} proparly left the server", player);
                return;
            }

            PlayerProfile playerProfile = session.getProfile();

            //force login only on success
            if (pendingConnection.isOnlineMode()) {
                boolean autoRegister = session.needsRegistration();

                //2fa authentication - no need to send bukkit force login notification and so we also don't need
                // to wait for a response -> save immediatly
                if (!plugin.getConfig().getBoolean("autoLogin")) {
                    playerProfile.setPremium(true);
                    plugin.getCore().getStorage().save(playerProfile);
                    session.setAlreadySaved(true);
                }

                AuthPlugin<ProxiedPlayer> authPlugin = plugin.getCore().getAuthPluginHook();
                if (authPlugin == null) {
                    //save will happen on success message from bukkit
                    sendBukkitLoginNotification(autoRegister);
                } else if (session.needsRegistration()) {
                    forceRegister(session, authPlugin);
                } else if (authPlugin.forceLogin(player)) {
                    forceLogin(session, authPlugin);
                }
            } else {
                //cracked player
                if (!session.isAlreadySaved()) {
                    playerProfile.setPremium(false);
                    plugin.getCore().getStorage().save(playerProfile);
                    session.setAlreadySaved(true);
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.INFO, "ERROR ON FORCE LOGIN", ex);
        }
    }

    private void forceRegister(BungeeLoginSession session, AuthPlugin<ProxiedPlayer> authPlugin) {
        if (session.isAlreadyLogged()) {
            sendBukkitLoginNotification(true);
            return;
        }

        session.setAlreadyLogged(true);

        String password = plugin.getCore().getPasswordGenerator().getRandomPassword(player);
        if (authPlugin.forceRegister(player, password)) {
            //save will happen on success message from bukkit
            sendBukkitLoginNotification(true);
            String message = plugin.getCore().getMessage("auto-register");
            if (message != null) {
                message = message.replace("%password", password);
                player.sendMessage(TextComponent.fromLegacyText(message));
            }
        }
    }

    private void forceLogin(BungeeLoginSession session, AuthPlugin<ProxiedPlayer> authPlugin) {
        if (session.isAlreadyLogged()) {
            sendBukkitLoginNotification(false);
            return;
        }

        session.setAlreadyLogged(true);
        if (authPlugin.forceLogin(player)) {
            //save will happen on success message from bukkit
            sendBukkitLoginNotification(false);
            String message = plugin.getCore().getMessage("auto-login");
            if (message != null) {
                player.sendMessage(TextComponent.fromLegacyText(message));
            }
        }
    }

    private void sendBukkitLoginNotification(boolean autoRegister) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        //subchannel name
        if (autoRegister) {
            dataOutput.writeUTF("AUTO_REGISTER");
        } else {
            dataOutput.writeUTF("AUTO_LOGIN");
        }

        //Data is sent through a random player. We have to tell the Bukkit version of this plugin the target
        dataOutput.writeUTF(player.getName());

        //proxy identifier to check if it's a acceptable proxy
        UUID proxyId = UUID.fromString(plugin.getProxy().getConfig().getUuid());
        dataOutput.writeLong(proxyId.getMostSignificantBits());
        dataOutput.writeLong(proxyId.getLeastSignificantBits());

        if (server != null) {
            server.sendData(plugin.getDescription().getName(), dataOutput.toByteArray());
        }
    }
}
