package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.tasks.ForceLoginTask;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Responsible for receiving messages from a BungeeCord instance.
 *
 * This class also receives the plugin message from the bungeecord version of this plugin in order to get notified if
 * the connection is in online mode.
 */
public class BungeeCordListener implements PluginMessageListener {

    private static final String FILE_NAME = "proxy-whitelist.txt";

    private final FastLoginBukkit plugin;
    //null if whitelist is empty so bungeecord support is disabled
    private final Set<UUID> proxyIds;

    public BungeeCordListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
        this.proxyIds = loadBungeeCordIds();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(plugin.getName())) {
            return;
        }

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(message);
        String subchannel = dataInput.readUTF();
        plugin.getLogger().log(Level.FINEST, "Received plugin message for subchannel {0} from {1}"
                , new Object[]{subchannel, player});

        String playerName = dataInput.readUTF();

        //check if the player is still online or disconnected
        Player checkedPlayer = plugin.getServer().getPlayerExact(playerName);
        //fail if target player is blacklisted because already authed or wrong bungeecord id
        if (checkedPlayer != null && !checkedPlayer.hasMetadata(plugin.getName())) {
            //bungeecord UUID
            long mostSignificantBits = dataInput.readLong();
            long leastSignificantBits = dataInput.readLong();
            UUID sourceId = new UUID(mostSignificantBits, leastSignificantBits);
            plugin.getLogger().log(Level.FINEST, "Received proxy id {0} from {1}", new Object[]{sourceId, player});

            //fail if BungeeCord support is disabled (id = null)
            if (proxyIds.contains(sourceId)) {
                readMessage(checkedPlayer, subchannel, playerName, player);
            }
        }
    }

    private void readMessage(Player checkedPlayer, String subchannel, String playerName, Player player) {
        InetSocketAddress address = checkedPlayer.getAddress();
        String id = '/' + address.getAddress().getHostAddress() + ':' + address.getPort();
        if ("AUTO_LOGIN".equalsIgnoreCase(subchannel)) {
            BukkitLoginSession playerSession = new BukkitLoginSession(playerName, true);
            playerSession.setVerified(true);
            plugin.getLoginSessions().put(id, playerSession);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new ForceLoginTask(plugin.getCore(), player));
        } else if ("AUTO_REGISTER".equalsIgnoreCase(subchannel)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
                    plugin.getLogger().log(Level.SEVERE, "Failed to query isRegistered", ex);
                }
            });
        }
    }

    public Set<UUID> loadBungeeCordIds() {
        Path whitelistFile = plugin.getDataFolder().toPath().resolve(FILE_NAME);
        try {
            if (!Files.exists(whitelistFile)) {
                Files.createFile(whitelistFile);
            }

            List<String> lines = Files.readAllLines(whitelistFile);
            return lines.stream().map(String::trim).map(UUID::fromString).collect(Collectors.toSet());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create file for Proxy whitelist", ex);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        return null;
    }
}
