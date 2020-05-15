package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ultraauth.api.UltraAuthAPI;
import ultraauth.main.Main;
import ultraauth.managers.PlayerManager;

/**
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/ultraauth-aa/
 * <p>
 * Spigot: https://www.spigotmc.org/resources/ultraauth.17044/
 */
public class UltraAuthHook implements AuthPlugin<Player> {

    private final Plugin ultraAuthPlugin = Main.main;
    private final FastLoginBukkit plugin;

    public UltraAuthHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        //not thread-safe
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
            if (UltraAuthAPI.isAuthenticated(player)) {
                plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
                return false;
            }

            UltraAuthAPI.authenticatedPlayer(player);
            return UltraAuthAPI.isAuthenticated(player);
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            plugin.getLog().error("Failed to forceLogin player: {}", player, ex);
            return false;
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        return UltraAuthAPI.isRegisterd(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        UltraAuthAPI.setPlayerPasswordOnline(player, password);
        //the register method silents any exception so check if our entry was saved
        return PlayerManager.getInstance().checkPlayerPassword(player, password) && forceLogin(player);
    }
}
