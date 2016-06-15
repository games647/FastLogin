package com.github.games647.fastlogin.bukkit.listener;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.tasks.ForceLoginTask;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * This listener tells authentication plugins if the player has a premium account and we checked it successfully. So the
 * plugin can skip authentication.
 */
public class BukkitJoinListener implements Listener {

    private static final long DELAY_LOGIN = 20L / 2;

    protected final FastLoginBukkit plugin;

    public BukkitJoinListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        if (loginEvent.getResult() == Result.ALLOWED && !plugin.isServerFullyStarted()) {
            loginEvent.disallow(Result.KICK_OTHER, plugin.getCore().getMessage("not-started"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();

        if (!plugin.isBungeeCord()) {
            //Wait before auth plugin and we received a message from BungeeCord initializes the player
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new ForceLoginTask(plugin, player), DELAY_LOGIN);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent quitEvent) {
        Player player = quitEvent.getPlayer();
        player.removeMetadata(plugin.getName(), plugin);
        plugin.getPendingConfirms().remove(player.getUniqueId());
    }
}
