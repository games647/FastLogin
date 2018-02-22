package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.xAuthPlayer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * GitHub: https://github.com/LycanDevelopment/xAuth/
 * <p>
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/xauth/
 */
public class xAuthHook implements AuthPlugin<Player> {

    private final xAuth xAuthPlugin = xAuth.getPlugin();
    private final FastLoginBukkit plugin;

    public xAuthHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        //not thread-safe
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(plugin, () -> {
            xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
            if (xAuthPlayer != null) {
                if (xAuthPlayer.isAuthenticated()) {
                    return true;
                }

                //we checked that the player is premium (paid account)
                xAuthPlayer.setPremium(true);

                //unprotect the inventory, op status...
                return xAuthPlugin.getPlayerManager().doLogin(xAuthPlayer);
            }

            return false;
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        //this will load the player if it's not in the cache
        xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(playerName);
        return xAuthPlayer != null && xAuthPlayer.isRegistered();
    }

    @Override
    public boolean forceRegister(Player player, final String password) {
        //not thread-safe
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(xAuthPlugin, () -> {
            xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
            //this should run async because the plugin executes a sql query, but the method
            //accesses non thread-safe collections :(
            return xAuthPlayer != null
                    && xAuthPlugin.getAuthClass(xAuthPlayer).adminRegister(player.getName(), password, null);

        });

        try {
            //login in the player after registration
            return future.get() && forceLogin(player);
        } catch (InterruptedException | ExecutionException ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }
    }
}
