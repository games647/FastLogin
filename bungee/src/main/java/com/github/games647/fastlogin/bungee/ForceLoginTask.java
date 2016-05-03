package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class ForceLoginTask implements Runnable {

    private final FastLoginBungee plugin;
    private final ProxiedPlayer player;

    public ForceLoginTask(FastLoginBungee plugin, ProxiedPlayer player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        PlayerProfile playerProfile = plugin.getStorage().getProfile(player.getName(), false);

        boolean success = true;
        if (playerProfile.getUserId() == -1) {
            playerProfile.setPremium(player.getPendingConnection().isOnlineMode());
            if (player.getPendingConnection().isOnlineMode()) {
                playerProfile.setUuid(player.getUniqueId());
            }

            success = plugin.getStorage().save(playerProfile);
        }

        //force login only on success
        if (success && player.getPendingConnection().isOnlineMode()) {
            Server server = player.getServer();

            boolean autoRegister = plugin.getPendingAutoRegister().remove(player.getPendingConnection()) != null;

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

            server.sendData(plugin.getDescription().getName(), dataOutput.toByteArray());

            BungeeAuthPlugin authPlugin = plugin.getBungeeAuthPlugin();
            if (authPlugin != null) {
                if (autoRegister) {
                    String password = plugin.generateStringPassword();
                    authPlugin.forceRegister(player, password);
                } else {
                    authPlugin.forceLogin(player);
                }
            }
        }
    }
}
