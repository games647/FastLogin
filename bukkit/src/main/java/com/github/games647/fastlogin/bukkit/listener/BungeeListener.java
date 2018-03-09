package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.tasks.ForceLoginTask;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.messages.ForceActionMessage;
import com.github.games647.fastlogin.core.messages.ForceActionMessage.Type;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import static java.util.stream.Collectors.toSet;

/**
 * Responsible for receiving messages from a BungeeCord instance.
 *
 * This class also receives the plugin message from the bungeecord version of this plugin in order to get notified if
 * the connection is in online mode.
 */
public class BungeeListener implements PluginMessageListener {

    private static final String FILE_NAME = "proxy-whitelist.txt";

    private final FastLoginBukkit plugin;
    //null if whitelist is empty so bungeecord support is disabled
    private final Set<UUID> proxyIds;

    public BungeeListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
        this.proxyIds = loadBungeeCordIds();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(plugin.getName())) {
            return;
        }

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);
        String subChannel = dataInput.readUTF();
        if (!"FORCE_ACTION".equals(subChannel)) {
            plugin.getLog().info("Unknown sub channel {}", subChannel);
            return;
        }

        ForceActionMessage loginMessage = new ForceActionMessage();
        loginMessage.readFrom(dataInput);

        plugin.getLog().debug("Received plugin message {}", loginMessage);

        //check if the player is still online or disconnected
        Player checkedPlayer = Bukkit.getPlayerExact(loginMessage.getPlayerName());

        //fail if target player is blacklisted because already authenticated or wrong bungeecord id
        if (checkedPlayer != null && !checkedPlayer.hasMetadata(plugin.getName())) {
            //fail if BungeeCord support is disabled (id = null)
            UUID sourceId = loginMessage.getProxyId();
            if (proxyIds.contains(sourceId)) {
                readMessage(checkedPlayer, loginMessage);
            } else {
                plugin.getLog().warn("Received proxy id: {} that doesn't exist in the proxy whitelist file", sourceId);
            }
        }
    }

    private void readMessage(Player player, ForceActionMessage message) {
        String playerName = message.getPlayerName();
        Type type = message.getType();

        InetSocketAddress address = player.getAddress();
        String id = '/' + address.getAddress().getHostAddress() + ':' + address.getPort();
        if (type == Type.LOGIN) {
            BukkitLoginSession playerSession = new BukkitLoginSession(playerName, true);
            playerSession.setVerified(true);
            plugin.getLoginSessions().put(id, playerSession);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new ForceLoginTask(plugin.getCore(), player), 20L);
        } else if (type == Type.REGISTER) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                AuthPlugin<Player> authPlugin = plugin.getCore().getAuthPluginHook();
                try {
                    //we need to check if the player is registered on Bukkit too
                    if (authPlugin == null || !authPlugin.isRegistered(playerName)) {
                        BukkitLoginSession playerSession = new BukkitLoginSession(playerName, false);
                        playerSession.setVerified(true);
                        plugin.getLoginSessions().put(id, playerSession);
                        new ForceLoginTask(plugin.getCore(), player).run();
                    }
                } catch (Exception ex) {
                    plugin.getLog().error("Failed to query isRegistered for player: {}", player, ex);
                }
            }, 20L);
        }
    }

    public Set<UUID> loadBungeeCordIds() {
        Path whitelistFile = plugin.getPluginFolder().resolve(FILE_NAME);
        try {
            if (Files.notExists(whitelistFile)) {
                Files.createFile(whitelistFile);
            }

            try (Stream<String> lines = Files.lines(whitelistFile)) {
                return lines.map(String::trim)
                        .map(UUID::fromString)
                        .collect(toSet());
            }
        } catch (IOException ex) {
            plugin.getLog().error("Failed to create file for Proxy whitelist", ex);
        } catch (Exception ex) {
            plugin.getLog().error("Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        return Collections.emptySet();
    }
}
