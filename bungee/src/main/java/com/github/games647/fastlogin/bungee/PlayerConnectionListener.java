package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.bungee.hooks.BungeeAuthPlugin;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
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

    protected final FastLoginBungee plugin;
    private final ConcurrentMap<PendingConnection, Object> pendingAutoRegister = CacheBuilder
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .<PendingConnection, Object>build().asMap();

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

        PlayerProfile playerProfile = plugin.getStorage().getProfile(username, true);
        if (playerProfile != null) {
            if (playerProfile.isPremium()) {
                connection.setOnlineMode(true);
            } else if (playerProfile.getUserId() == -1) {
                //user not exists in the db
                BungeeAuthPlugin authPlugin = plugin.getBungeeAuthPlugin();
                if (plugin.getConfiguration().getBoolean("autoRegister")
                        && (authPlugin == null || !authPlugin.isRegistered(username))) {
                    UUID premiumUUID = plugin.getMojangApiConnector().getPremiumUUID(username);
                    if (premiumUUID != null) {
                        plugin.getLogger().log(Level.FINER, "Player {0} uses a premium username", username);
                        connection.setOnlineMode(true);
                        pendingAutoRegister.put(connection, new Object());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent serverConnectedEvent) {
        ProxiedPlayer player = serverConnectedEvent.getPlayer();
        //send message even when the online mode is activated by default

        final PlayerProfile playerProfile = plugin.getStorage().getProfile(player.getName(), false);
        if (playerProfile.getUserId() == -1) {
            ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.getStorage().save(playerProfile);
                }
            });
        }

        if (player.getPendingConnection().isOnlineMode()) {
            Server server = serverConnectedEvent.getServer();

            boolean autoRegister = pendingAutoRegister.remove(player.getPendingConnection()) != null;

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
            if ("ON".equals(subchannel)) {
                final ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();

                ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        PlayerProfile playerProfile = plugin.getStorage().getProfile(forPlayer.getName(), true);
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
                    }
                });
            } else if ("OFF".equals(subchannel)) {
                final ProxiedPlayer forPlayer = (ProxiedPlayer) pluginMessageEvent.getReceiver();
                ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                    @Override
                    public void run() {
                        PlayerProfile playerProfile = plugin.getStorage().getProfile(forPlayer.getName(), true);
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
                        //todo: set uuid
                        plugin.getStorage().save(playerProfile);
                    }
                });
            }
        }
    }
}
