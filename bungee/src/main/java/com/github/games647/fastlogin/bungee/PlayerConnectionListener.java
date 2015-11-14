package com.github.games647.fastlogin.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Enables online mode logins for specified users and sends
 * plugin message to the Bukkit version of this plugin in
 * order to clear that the connection is online mode.
 */
public class PlayerConnectionListener implements Listener {

    private final FastLoginBungee plugin;

    public PlayerConnectionListener(FastLoginBungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent preLoginEvent) {
        if (preLoginEvent.isCancelled()) {
            return;
        }

        PendingConnection connection = preLoginEvent.getConnection();
        String username = connection.getName();
        //just enable it for activated users
        if (plugin.getEnabledPremium().contains(username)) {
            connection.setOnlineMode(true);
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent serverConnectedEvent) {
        ProxiedPlayer player = serverConnectedEvent.getPlayer();
        //send message even when the online mode is activated by default
        if (player.getPendingConnection().isOnlineMode()) {
            Server server = serverConnectedEvent.getServer();

            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            //subchannel name
            dataOutput.writeUTF("CHECKED");

            //Data is sent through a random player. We have to tell the Bukkit version of this plugin the target
            dataOutput.writeUTF(player.getName());

            //proxy identifier to check if it's a acceptable proxy
            UUID proxyId = UUID.fromString(plugin.getProxy().getConfig().getUuid());
            dataOutput.writeLong(proxyId.getMostSignificantBits());
            dataOutput.writeLong(proxyId.getLeastSignificantBits());

            server.sendData(plugin.getDescription().getName(), dataOutput.toByteArray());
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent pluginMessageEvent) {
        String channel = pluginMessageEvent.getTag();
        if (pluginMessageEvent.isCancelled() || !plugin.getDescription().getName().equals(channel)) {
            return;
        }

        //the client shouldn't be able to read the messages in order to know something about server internal states
        //moreover the client shouldn't be able fake a running premium check by sending the result message
        pluginMessageEvent.setCancelled(true);

        //check if the message is sent from the server
        if (Server.class.isAssignableFrom(pluginMessageEvent.getSender().getClass())) {
            byte[] data = pluginMessageEvent.getData();
            ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
            String subchannel = dataInput.readUTF();
            if ("ACTIVE".equals(subchannel)) {
                ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();
                plugin.getEnabledPremium().add(forPlayer.getName());
            }
        }
    }
}
