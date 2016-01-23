package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.PlayerSession;
import com.github.games647.fastlogin.bukkit.hooks.AuthPlugin;

import java.util.logging.Level;

import org.bukkit.Bukkit;
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
    protected final AuthPlugin authPlugin;

    public BukkitJoinListener(FastLoginBukkit plugin, AuthPlugin authPlugin) {
        this.plugin = plugin;
        this.authPlugin = authPlugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        final Player player = joinEvent.getPlayer();

        PlayerSession session = plugin.getSessions().get(player.getAddress().toString());
        if (session != null) {
            WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
            WrappedSignedProperty skin = session.getSkin();
            if (skin != null) {
                gameProfile.getProperties().put("textures", skin);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                String address = player.getAddress().toString();
                //removing the session because we now use it
                PlayerSession session = plugin.getSessions().remove(address);

                if (player.isOnline()) {
                    //blacklist this target player for BungeeCord Id brute force attacks
                    player.setMetadata(plugin.getName(), new FixedMetadataValue(plugin, true));
                    //check if it's the same player as we checked before
                    if (session != null && player.getName().equals(session.getUsername()) && session.isVerified()) {
                        plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
                        authPlugin.forceLogin(player);
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
