package com.github.games647.fastlogin.bungee.listener;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.tasks.AsyncToggleMessage;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.util.Arrays;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessageListener implements Listener {

    protected final FastLoginBungee plugin;

    public PluginMessageListener(FastLoginBungee plugin) {
        this.plugin = plugin;
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
            readMessage(pluginMessageEvent);
        }
    }

    private void readMessage(PluginMessageEvent pluginMessageEvent) {
        //so that we can safely process this in the background
        final byte[] data = Arrays.copyOf(pluginMessageEvent.getData(), pluginMessageEvent.getData().length);
        final ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();

        ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
            @Override
            public void run() {
                ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
                String subchannel = dataInput.readUTF();
                if ("ON".equals(subchannel)) {
                    String playerName = dataInput.readUTF();

                    if (playerName.equals(forPlayer.getName()) && plugin.getConfig().getBoolean("premium-warning")
                            && !plugin.getCore().getPendingConfirms().contains(forPlayer.getUniqueId())) {
                        String message = plugin.getCore().getMessage("premium-warning");
                        forPlayer.sendMessage(TextComponent.fromLegacyText(message));
                        plugin.getCore().getPendingConfirms().add(forPlayer.getUniqueId());
                        return;
                    }

                    plugin.getCore().getPendingConfirms().remove(forPlayer.getUniqueId());
                    AsyncToggleMessage task = new AsyncToggleMessage(plugin, forPlayer, playerName, true);
                    ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
                } else if ("OFF".equals(subchannel)) {
                    String playerName = dataInput.readUTF();

                    AsyncToggleMessage task = new AsyncToggleMessage(plugin, forPlayer, playerName, false);
                    ProxyServer.getInstance().getScheduler().runAsync(plugin, task);
                } else if ("SUCCESS".equals(subchannel)) {
                    if (forPlayer.getPendingConnection().isOnlineMode()) {
                        //bukkit module successfully received and force logged in the user
                        //update only on success to prevent corrupt data
                        BungeeLoginSession loginSession = plugin.getSession().get(forPlayer.getPendingConnection());
                        PlayerProfile playerProfile = loginSession.getProfile();
                        loginSession.setRegistered(true);

                        if (!loginSession.isAlreadySaved()) {
                            playerProfile.setPremium(true);
                            plugin.getCore().getStorage().save(playerProfile);
                            loginSession.setAlreadySaved(true);
                        }
                    }
                }
            }
        });
    }
}
