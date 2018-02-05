package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.google.common.collect.Sets;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.RestoreSessionEvent;

import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Github: https://github.com/Xephi/AuthMeReloaded/
 * <p>
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/authme-reloaded/
 * <p>
 * Spigot: https://www.spigotmc.org/resources/authme-reloaded.6269/
 */
public class AuthMeHook implements AuthPlugin<Player>, Listener {

    private final Set<UUID> sessionLogins = Sets.newConcurrentHashSet();
    private final FastLoginBukkit plugin;

    public AuthMeHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onRestoreSession(RestoreSessionEvent restoreSessionEvent) {
        UUID uniqueId = restoreSessionEvent.getPlayer().getUniqueId();
        sessionLogins.add(uniqueId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent quitEvent) {
        UUID uniqueId = quitEvent.getPlayer().getUniqueId();
        sessionLogins.remove(uniqueId);
    }

    @Override
    public boolean forceLogin(Player player) {
        //skips registration and login
        if (AuthMeApi.getInstance().isAuthenticated(player) || sessionLogins.contains(player.getUniqueId())) {
            return false;
        } else {
            AuthMeApi.getInstance().forceLogin(player);
        }

        return true;
    }

    @Override
    public boolean isRegistered(String playerName) {
        return AuthMeApi.getInstance().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        //this automatically registers the player too
        AuthMeApi.getInstance().forceRegister(player, password);
        return true;
    }
}
