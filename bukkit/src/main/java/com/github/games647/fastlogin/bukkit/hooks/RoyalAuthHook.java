package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;
import org.royaldev.royalauth.RoyalAuth;

/**
 * Github: https://github.com/RoyalDev/RoyalAuth
 *
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/royalauth/
 */
public class RoyalAuthHook implements AuthPlugin<Player> {

    private final Plugin royalAuthPlugin = (RoyalAuth) Bukkit.getPluginManager().getPlugin("RoyalAuth");

    @Override
    public boolean forceLogin(Player player) {
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(player);

        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(royalAuthPlugin, () -> {
            if (authPlayer.isLoggedIn()) {
                return true;
            }

//https://github.com/RoyalDev/RoyalAuth/blob/master/src/main/java/org/royaldev/royalauth/commands/CmdLogin.java#L62
//not thread-safe
            authPlayer.login();

            return authPlayer.isLoggedIn();
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            royalAuthPlugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(playerName);
        return authPlayer.isRegistered();
    }

    @Override
    public boolean forceRegister(Player player, String password) {
//https://github.com/RoyalDev/RoyalAuth/blob/master/src/main/java/org/royaldev/royalauth/commands/CmdRegister.java#L50
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(player);

        boolean registerSuccess = authPlayer.setPassword(password, Config.passwordHashType);

        //login in the player after registration
        return registerSuccess && forceLogin(player);
    }
}
