package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;
import java.util.logging.Level;

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

                BungeeAuthPlugin authPlugin = plugin.getBungeeAuthPlugin();
                if (authPlugin == null) {
                    //save will happen on success message from bukkit
                    sendBukkitLoginNotification(autoRegister);
                } else if (session.needsRegistration()) {
                    String password = plugin.generateStringPassword();
                    if (authPlugin.forceRegister(player, password)) {
                        //save will happen on success message from bukkit
                        sendBukkitLoginNotification(autoRegister);
                        String message = plugin.getCore().getMessage("auto-register");
                        if (message != null) {
                            message = message.replace("%password", password);
                            player.sendMessage(message);
                        }
                    }
                } else if (authPlugin.forceLogin(player)) {
                    //save will happen on success message from bukkit
                    sendBukkitLoginNotification(autoRegister);
                    String message = plugin.getCore().getMessage("auto-login");
                    if (message != null) {
                        player.sendMessage(message);
                    }
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
