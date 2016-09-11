package com.github.games647.fastlogin.bukkit.hooks;


import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.xAuthPlayer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/LycanDevelopment/xAuth/
 *
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/xauth/
 */
public class xAuthHook implements BukkitAuthPlugin {

    protected final xAuth xAuthPlugin = xAuth.getPlugin();

    @Override
    public boolean forceLogin(final Player player) {
        //not thread-safe
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(xAuthPlugin, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
                if (xAuthPlayer != null) {
                    //we checked that the player is premium (paid account)
                    xAuthPlayer.setPremium(true);

                    //unprotect the inventory, op status...
                    return xAuthPlugin.getPlayerManager().doLogin(xAuthPlayer);
                }

                return false;
            }
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            xAuthPlugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        //this will load the player if it's not in the cache
        xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(playerName);
        return xAuthPlayer != null && xAuthPlayer.isRegistered();
    }

    @Override
    public boolean forceRegister(final Player player, final String password) {
        //not thread-safe
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(xAuthPlugin, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
                if (xAuthPlayer != null) {
                    //this should run async because the plugin executes a sql query, but the method
                    //accesses non thread-safe collections :(
                    boolean registerSuccess = xAuthPlugin.getAuthClass(xAuthPlayer)
                            .adminRegister(player.getName(), password, null);

                    return registerSuccess;
                }

                return false;
            }
        });

        try {
            boolean success = future.get();
            if (success) {
                //login in the player after registration
                return forceLogin(player);
            }

            return false;
        } catch (InterruptedException | ExecutionException ex) {
            xAuthPlugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }
    }
}
