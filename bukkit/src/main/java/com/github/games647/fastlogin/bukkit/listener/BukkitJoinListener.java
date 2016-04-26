package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerProfile;
import com.github.games647.fastlogin.bukkit.PlayerSession;
import com.github.games647.fastlogin.bukkit.Storage;
import com.github.games647.fastlogin.bukkit.hooks.BukkitAuthPlugin;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * This listener tells authentication plugins if the player has a premium account and we checked it successfully. So the
 * plugin can skip authentication.
 */
public class BukkitJoinListener implements Listener {

    private static final long DELAY_LOGIN = 1 * 20L / 2;

    protected final FastLoginBukkit plugin;

    public BukkitJoinListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        final Player player = joinEvent.getPlayer();

        //removing the session because we now use it
        final PlayerSession session = plugin.getSessions().get(player.getAddress().toString());
        if (session != null && plugin.getConfig().getBoolean("forwardSkin")) {
            WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
            WrappedSignedProperty skin = session.getSkin();
            if (skin != null) {
                gameProfile.getProperties().put("textures", skin);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                if (player.isOnline()) {
                    //remove the bungeecord identifier
                    String id = '/' + player.getAddress().getAddress().getHostAddress() + ':'
                            + player.getAddress().getPort();
                    PlayerSession session = plugin.getSessions().get(id);

                    //blacklist this target player for BungeeCord Id brute force attacks
                    player.setMetadata(plugin.getName(), new FixedMetadataValue(plugin, true));
                    //check if it's the same player as we checked before

                    BukkitAuthPlugin authPlugin = plugin.getAuthPlugin();
                    if (session != null && player.getName().equals(session.getUsername()) && session.isVerified()
                            && authPlugin != null) {
                        if (session.needsRegistration()) {
                            plugin.getLogger().log(Level.FINE, "Register player {0}", player.getName());

                            final Storage storage = plugin.getStorage();
                            if (storage != null) {
                                final PlayerProfile playerProfile = storage.getProfile(session.getUsername(), false);
                                playerProfile.setPremium(true);
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                                    @Override
                                    public void run() {
                                        storage.save(playerProfile);
                                    }
                                });
                            }

                            String generatedPassword = plugin.generateStringPassword();
                            authPlugin.forceRegister(player, generatedPassword);
                            player.sendMessage(ChatColor.DARK_GREEN + "Auto registered with password: "
                                    + generatedPassword);
                            player.sendMessage(ChatColor.DARK_GREEN + "You may want change it?");
                        } else {
                            plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
                            authPlugin.forceLogin(player);
                            player.sendMessage(ChatColor.DARK_GREEN + "Auto logged in");
                        }
                    }
                }
            }
            //Wait before auth plugin and we received a message from BungeeCord initializes the player
        }, DELAY_LOGIN);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent quitEvent) {
        final Player player = quitEvent.getPlayer();

        //prevent memory leaks
        player.removeMetadata(plugin.getName(), plugin);
    }
}
