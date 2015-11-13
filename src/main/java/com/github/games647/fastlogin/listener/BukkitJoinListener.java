package com.github.games647.fastlogin.listener;

import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerSession;
import com.github.games647.fastlogin.hooks.AuthPlugin;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * This listener tells authentication plugins if the player has a premium account and we checked it successfully. So the
 * plugin can skip authentication.
 */
public class BukkitJoinListener implements Listener {

    private static final long DELAY_LOGIN = 2 * 20L;

    protected final FastLogin plugin;
    protected final AuthPlugin authPlugin;

    public BukkitJoinListener(FastLogin plugin, AuthPlugin authPlugin) {
        this.plugin = plugin;
        this.authPlugin = authPlugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent joinEvent) {
        final Player player = joinEvent.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                String address = player.getAddress().toString();
                //removing the session because we now use it
                PlayerSession session = plugin.getSessions().remove(address);

                //check if it's the same player as we checked before
                if (player.isOnline() && session != null
                        && player.getName().equals(session.getUsername()) && session.isVerified()) {
                    plugin.getLogger().log(Level.FINE, "Logging player {0} in", player.getName());
                    authPlugin.forceLogin(player);
                }
            }
            //Wait before auth plugin and we received a message from BungeeCord initializes the player
        }, DELAY_LOGIN);
    }
}
