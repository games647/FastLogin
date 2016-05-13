package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ForceLoginTask implements Runnable {

    protected final FastLoginBukkit plugin;
    protected final Player player;

    public ForceLoginTask(FastLoginBukkit plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        if (!isOnlineThreadSafe()) {
            return;
        }

        //remove the bungeecord identifier if there is ones
        String id = '/' + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort();
        PlayerSession session = plugin.getSessions().get(id);

        BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();

        Storage storage = plugin.getStorage();
        PlayerProfile playerProfile = null;
        if (storage != null) {
            playerProfile = storage.getProfile(player.getName(), false);
        }

        if (session == null) {
            //cracked player
            if (playerProfile != null) {
                playerProfile.setUuid(null);
                playerProfile.setPremium(false);
                storage.save(playerProfile);
            }
            //check if it's the same player as we checked before
        } else if (player.getName().equals(session.getUsername())) {
            //premium player
            if (authPlugin == null) {
                //maybe only bungeecord plugin
                sendSuccessNotification();
            } else {
                boolean success = false;
                if (isOnlineThreadSafe() && session.isVerified()) {
                    if (session.needsRegistration()) {
                        success = forceRegister(authPlugin, player);
                    } else {
                        success = forceLogin(authPlugin, player);
                    }
                }

                if (success) {
                    //update only on success to prevent corrupt data
                    if (playerProfile != null) {
                        playerProfile.setUuid(session.getUuid());
                        //save cracked players too
                        playerProfile.setPremium(session.isVerified());
                        storage.save(playerProfile);
                    }

                    sendSuccessNotification();
                }
            }
        }
    }

    private boolean forceRegister(BukkitAuthPlugin authPlugin, Player player) {
        plugin.getLogger().log(Level.FINE, "Register player {0}", player.getName());

        String generatedPassword = plugin.generateStringPassword();
        boolean success = authPlugin.forceRegister(player, generatedPassword);
        player.sendMessage(ChatColor.DARK_GREEN + "Auto registered with password: " + generatedPassword);
        player.sendMessage(ChatColor.DARK_GREEN + "You may want change it?");
        return success;
    }

    private boolean forceLogin(BukkitAuthPlugin authPlugin, Player player) {
        plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
        boolean success = authPlugin.forceLogin(player);
        player.sendMessage(ChatColor.DARK_GREEN + "Auto logged in");
        return success;
    }

    private void sendSuccessNotification() {
        if (plugin.isBungeeCord()) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("SUCCESS");

            player.sendPluginMessage(plugin, plugin.getName(), dataOutput.toByteArray());
        }
    }

    private boolean isOnlineThreadSafe() {
        //the playerlist isn't thread-safe
        Future<Boolean> onlineFuture = Bukkit.getScheduler().callSyncMethod(plugin, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return player.isOnline();
            }
        });

        try {
            return onlineFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to perform thread-safe online check", ex);
            return false;
        }
    }
}
