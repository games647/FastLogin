package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.tasks.ForceLoginTask;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Responsible for receiving messages from a BungeeCord instance.
 *
 * This class also receives the plugin message from the bungeecord version of this plugin in order to get notified if
 * the connection is in online mode.
 */
public class BungeeCordListener implements PluginMessageListener {

    private static final String FILE_NAME = "proxy-whitelist.txt";

    protected final FastLoginBukkit plugin;
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

        final String playerName = dataInput.readUTF();

        //check if the player is still online or disconnected
        final Player checkedPlayer = plugin.getServer().getPlayerExact(playerName);
        //fail if target player is blacklisted because already authed or wrong bungeecord id
        if (checkedPlayer != null && !checkedPlayer.hasMetadata(plugin.getName())) {
            //blacklist this target player for BungeeCord Id brute force attacks
            player.setMetadata(plugin.getName(), new FixedMetadataValue(plugin, true));

            //bungeecord UUID
            long mostSignificantBits = dataInput.readLong();
            long leastSignificantBits = dataInput.readLong();
            UUID sourceId = new UUID(mostSignificantBits, leastSignificantBits);
            plugin.getLogger().log(Level.FINEST, "Received proxy id {0} from {1}", new Object[]{sourceId, player});

            //fail if BungeeCord support is disabled (id = null)
            if (proxyIds.contains(sourceId)) {
                final String id = '/' + checkedPlayer.getAddress().getAddress().getHostAddress() + ':'
                        + checkedPlayer.getAddress().getPort();
                if ("AUTO_LOGIN".equalsIgnoreCase(subchannel)) {
                    BukkitLoginSession playerSession = new BukkitLoginSession(playerName, true);
                    playerSession.setVerified(true);
                    plugin.getSessions().put(id, playerSession);
                } else if ("AUTO_REGISTER".equalsIgnoreCase(subchannel)) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                        @Override
                        public void run() {
                            BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();
                            try {
                                //we need to check if the player is registered on Bukkit too
                                if (authPlugin != null && !authPlugin.isRegistered(playerName)) {
                                    BukkitLoginSession playerSession = new BukkitLoginSession(playerName, false);
                                    playerSession.setVerified(true);
                                    plugin.getSessions().put(id, playerSession);
                                }
                            } catch (Exception ex) {
                                plugin.getLogger().log(Level.SEVERE, "Failed to query isRegistered", ex);
                            }
                        }
                    });
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, new ForceLoginTask(plugin, player));
            }
        }
    }

    public Set<UUID> loadBungeeCordIds() {
        File whitelistFile = new File(plugin.getDataFolder(), FILE_NAME);
        //create a new folder if it doesn't exist. Fail silently otherwise
        whitelistFile.getParentFile().mkdir();
        try {
            if (!whitelistFile.exists()) {
                whitelistFile.createNewFile();
            }

            Set<UUID> ids = Sets.newHashSet();

            List<String> lines = Files.readLines(whitelistFile, Charsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }

                UUID uuid = UUID.fromString(line.trim());
                ids.add(uuid);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create file for Proxy whitelist", ex);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        return null;
    }
}
