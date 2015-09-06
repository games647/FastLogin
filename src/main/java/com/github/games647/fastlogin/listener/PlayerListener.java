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

public class PlayerListener implements Listener {

    private final FastLogin plugin;
    private final AuthPlugin authPlugin;

    public PlayerListener(FastLogin plugin, AuthPlugin authPlugin) {
        this.plugin = plugin;
        this.authPlugin = authPlugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent joinEvent) {
        final Player player = joinEvent.getPlayer();
        String address = player.getAddress().toString();

        //removing the session because we now use it
        PlayerSession session = plugin.getSessions().remove(address);
        //check if it's the same player as we checked before
        if (session != null && session.getUsername().equals(player.getName())
                && session.isVerified()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getLogger().log(Level.FINER, "Logging player {0} in", player.getName());
                    authPlugin.forceLogin(player);
                }
                //Wait before auth plugin initializes the player
            }, 1 * 20L);
        }
    }
}
