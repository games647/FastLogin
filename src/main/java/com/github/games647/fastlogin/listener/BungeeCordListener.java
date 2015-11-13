package com.github.games647.fastlogin.listener;

import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerSession;
import com.google.common.base.Charsets;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Responsible for receiving messages from a BungeeCord instance.
 *
 * This class also receives the plugin message from the bungeecord version of this plugin in order to
 * get notified if the connection is in online mode.
 */
public class BungeeCordListener implements PluginMessageListener {

    private static final String FILE_NAME = "proxy-whitelist.txt";

    private final FastLogin plugin;
    //null if whitelist is empty so bungeecord support is disabled
    private final UUID proxyId;

    public BungeeCordListener(FastLogin plugin) {
        this.plugin = plugin;
        this.proxyId = loadBungeeCordId();
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
        if ("Checked".equalsIgnoreCase(subchannel)) {
            //bungeecord UUID
            long mostSignificantBits = dataInput.readLong();
            long leastSignificantBits = dataInput.readLong();
            UUID sourceId = new UUID(mostSignificantBits, leastSignificantBits);
            //fails too if no proxy id is specified in the whitelist file
            if (sourceId.equals(proxyId)) {
                //make sure the proxy is allowed to transfer data to us
                String playerName = dataInput.readUTF();
                //check if the player is still online or disconnected
                Player checkedPlayer = plugin.getServer().getPlayerExact(playerName);
                if (checkedPlayer != null && checkedPlayer.isOnline()) {
                    PlayerSession playerSession = new PlayerSession(playerName, null, null);
                    playerSession.setVerified(true);
                    //put it only if the user doesn't has a session open
                    //so that the player have to send the bungeecord packet and cannot skip the verification then
                    plugin.getSessions().putIfAbsent(checkedPlayer.getAddress().toString(), playerSession);
                }
            }
        }
    }

    public UUID loadBungeeCordId() {
        File whitelistFile = new File(plugin.getDataFolder(), FILE_NAME);
        //create a new folder if it doesn't exist. Fail silently otherwise
        whitelistFile.getParentFile().mkdir();
        try {
            if (!whitelistFile.exists()) {
                whitelistFile.createNewFile();
            }

            String firstLine = Files.readFirstLine(whitelistFile, Charsets.UTF_8);
            if (firstLine != null && !firstLine.isEmpty()) {
                return UUID.fromString(firstLine.trim());
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create file for Proxy whitelist", ex);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        return null;
    }
}
