package com.github.games647.fastlogin.bungee.listener;

import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.PlayerProfile;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ChatColor;
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
        byte[] data = pluginMessageEvent.getData();
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data);
        String subchannel = dataInput.readUTF();

        final ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();
        if ("ON".equals(subchannel)) {
            final String playerName = dataInput.readUTF();

            ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    PlayerProfile playerProfile = plugin.getStorage().getProfile(playerName, true);
                    if (playerProfile.isPremium()) {
                        if (forPlayer.isConnected()) {
                            TextComponent textComponent = new TextComponent("You are already on the premium list");
                            textComponent.setColor(ChatColor.DARK_RED);
                            forPlayer.sendMessage(textComponent);
                        }

                        return;
                    }

                    playerProfile.setPremium(true);
                    //todo: set uuid
                    plugin.getStorage().save(playerProfile);
                    TextComponent textComponent = new TextComponent("Added to the list of premium players");
                    textComponent.setColor(ChatColor.DARK_GREEN);
                    forPlayer.sendMessage(textComponent);
                }
            });
        } else if ("OFF".equals(subchannel)) {
            final String playerName = dataInput.readUTF();

            ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    PlayerProfile playerProfile = plugin.getStorage().getProfile(playerName, true);
                    if (!playerProfile.isPremium()) {
                        if (forPlayer.isConnected()) {
                            TextComponent textComponent = new TextComponent("You are not in the premium list");
                            textComponent.setColor(ChatColor.DARK_RED);
                            forPlayer.sendMessage(textComponent);
                        }

                        return;
                    }

                    playerProfile.setPremium(false);
                    playerProfile.setUuid(null);
                    plugin.getStorage().save(playerProfile);
                    TextComponent textComponent = new TextComponent("Removed to the list of premium players");
                    textComponent.setColor(ChatColor.DARK_GREEN);
                    forPlayer.sendMessage(textComponent);
                }
            });
        } else if ("SUCCESS".equals(subchannel)) {
            if (forPlayer.getPendingConnection().isOnlineMode()) {
                //bukkit module successfully received and force logged in the user
                //update only on success to prevent corrupt data
                PlayerProfile playerProfile = plugin.getStorage().getProfile(forPlayer.getName(), false);
                playerProfile.setPremium(true);
                //we override this in the loginevent
//                    playerProfile.setUuid(forPlayer.getUniqueId());
                plugin.getStorage().save(playerProfile);
            }
        }
    }
}
