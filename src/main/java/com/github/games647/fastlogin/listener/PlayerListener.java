package com.github.games647.fastlogin.listener;

import com.github.games647.fastlogin.FastLogin;
import com.github.games647.fastlogin.PlayerData;

import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.xAuthPlayer;
import de.luricos.bukkit.xAuth.xAuthPlayer.Status;

import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.limbo.LimboCache;

import java.sql.Timestamp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final FastLogin plugin;

    public PlayerListener(FastLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent joinEvent) {
        final Player player = joinEvent.getPlayer();
        String address = player.getAddress().toString();

        PlayerData session = plugin.getSession().asMap().get(address);
        if (session != null && session.getUsername().equals(player.getName())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                doLogin(player);
            }, 1 * 20L);
        }
    }

    private void doLogin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            //add cache entry - otherwise loggin wouldn't work
            LimboCache.getInstance().addLimboPlayer(player);

            //skips registration and login
            NewAPI.getInstance().forceLogin(player);
        } else if (Bukkit.getPluginManager().isPluginEnabled("xAuth")) {
            xAuth xAuthPlugin = xAuth.getPlugin();

            xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
            xAuthPlayer.setPremium(true);
            xAuthPlugin.getAuthClass(xAuthPlayer).online(xAuthPlayer.getName());
            xAuthPlayer.setLoginTime(new Timestamp(System.currentTimeMillis()));

            xAuthPlayer.setStatus(Status.AUTHENTICATED);

            xAuthPlugin.getPlayerManager().unprotect(xAuthPlayer);
        }
    }
}
