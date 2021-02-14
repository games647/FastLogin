package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.listener.BungeeListener;
import com.github.games647.fastlogin.core.message.ChannelMessage;
import com.github.games647.fastlogin.core.message.LoginActionMessage;
import com.github.games647.fastlogin.core.message.NamespaceKey;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import static com.github.games647.fastlogin.core.message.ChangePremiumMessage.CHANGE_CHANNEL;
import static com.github.games647.fastlogin.core.message.SuccessMessage.SUCCESS_CHANNEL;
import static java.util.stream.Collectors.toSet;

public class BungeeManager {

    private static final String LEGACY_FILE_NAME = "proxy-whitelist.txt";
    private static final String FILE_NAME = "allowed-proxies.txt";

    //null if proxies allowed list is empty so bungeecord support is disabled
    private Set<UUID> proxyIds;

    private final FastLoginBukkit plugin;
    private boolean enabled;

    private final Set<UUID> firedJoinEvents = new HashSet<>();

    public BungeeManager(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    public void cleanup() {
        //remove old blocked status
        Bukkit.getOnlinePlayers().forEach(player -> player.removeMetadata(plugin.getName(), plugin));
    }

    public void sendPluginMessage(PluginMessageRecipient player, ChannelMessage message) {
        if (player != null) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            message.writeTo(dataOutput);

            NamespaceKey channel = new NamespaceKey(plugin.getName(), message.getChannelName());
            player.sendPluginMessage(plugin, channel.getCombinedName(), dataOutput.toByteArray());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void initialize() {
        try {
            enabled = detectBungeeCord();
        } catch (Exception ex) {
            plugin.getLog().warn("Cannot check bungeecord support. Fallback to non-bungee mode", ex);
        }

        if (enabled) {
            proxyIds = loadBungeeCordIds();
            registerPluginChannels();
        }
    }

    private boolean detectBungeeCord() throws Exception {
        try {
            enabled = Class.forName("org.spigotmc.SpigotConfig").getDeclaredField("bungee").getBoolean(null);
            return enabled;
        } catch (ClassNotFoundException notFoundEx) {
            //ignore server has no bungee support
            return false;
        } catch (Exception ex) {
            throw ex;
        }
    }

    private void registerPluginChannels() {
        Server server = Bukkit.getServer();

        // check for incoming messages from the bungeecord version of this plugin
        String groupId = plugin.getName();
        String forceChannel = NamespaceKey.getCombined(groupId, LoginActionMessage.FORCE_CHANNEL);
        server.getMessenger().registerIncomingPluginChannel(plugin, forceChannel, new BungeeListener(plugin));

        // outgoing
        String successChannel = new NamespaceKey(groupId, SUCCESS_CHANNEL).getCombinedName();
        String changeChannel = new NamespaceKey(groupId, CHANGE_CHANNEL).getCombinedName();
        server.getMessenger().registerOutgoingPluginChannel(plugin, successChannel);
        server.getMessenger().registerOutgoingPluginChannel(plugin, changeChannel);
    }

    private Set<UUID> loadBungeeCordIds() {
        Path proxiesFile = plugin.getPluginFolder().resolve(FILE_NAME);
        Path legacyFile = plugin.getPluginFolder().resolve(LEGACY_FILE_NAME);
        try {
            if (Files.notExists(proxiesFile)) {
                if (Files.exists(legacyFile)) {
                    Files.move(legacyFile, proxiesFile);
                }

                if (Files.notExists(legacyFile)) {
                    Files.createFile(proxiesFile);
                }
            }

            Files.deleteIfExists(legacyFile);
            try (Stream<String> lines = Files.lines(proxiesFile)) {
                return lines.map(String::trim)
                        .map(UUID::fromString)
                        .collect(toSet());
            }
        } catch (IOException ex) {
            plugin.getLog().error("Failed to read proxies", ex);
        } catch (Exception ex) {
            plugin.getLog().error("Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        return Collections.emptySet();
    }

    public boolean isProxyAllowed(UUID proxyId) {
        return proxyIds != null && proxyIds.contains(proxyId);
    }

    /**
     * Mark the event to be fired including the task delay.
     *
     * @param player
     */
    public synchronized void markJoinEventFired(Player player) {
        firedJoinEvents.add(player.getUniqueId());
    }

    /**
     * Check if the event fired including with the task delay. This necessary to restore the order of processing the
     * BungeeCord messages after the PlayerJoinEvent fires including the delay.
     *
     * If the join event fired, the delay exceeded, but it ran earlier and couldn't find the recently started login
     * session. If not fired, we can start a new force login task. This will still match the requirement that we wait
     * a certain time after the player join event fired.
     *
     * @param player
     * @return event fired including delay
     */
    public synchronized boolean didJoinEventFired(Player player) {
        return firedJoinEvents.contains(player.getUniqueId());
    }

    /**
     * Player quit clean up
     *
     * @param player
     */
    public synchronized void cleanup(Player player) {
        firedJoinEvents.remove(player.getUniqueId());
    }
}
