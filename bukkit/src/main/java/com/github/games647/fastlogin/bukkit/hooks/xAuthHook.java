package com.github.games647.fastlogin.bukkit.hooks;

import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.xAuthPlayer;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/LycanDevelopment/xAuth/
 *
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/xauth/
 */
public class xAuthHook implements BukkitAuthPlugin {

    @Override
    public void forceLogin(Player player) {
        xAuth xAuthPlugin = xAuth.getPlugin();

        xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
        if (xAuthPlayer != null) {
            //we checked that the player is premium (paid account)
            //unprotect the inventory, op status...
            xAuthPlayer.setPremium(true);

            //not thread-safe
            xAuthPlugin.getPlayerManager().doLogin(xAuthPlayer);
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        xAuth xAuthPlugin = xAuth.getPlugin();
        //this will load the player if it's not in the cache
        xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(playerName);
        return xAuthPlayer != null && xAuthPlayer.isRegistered();
    }

    @Override
    public void forceRegister(Player player, String password) {
        xAuth xAuthPlugin = xAuth.getPlugin();

        xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
        if (xAuthPlayer != null) {
            //this should run async because the plugin executes a sql query, but the method
            //accesses non thread-safe collections :(
            xAuthPlugin.getAuthClass(xAuthPlayer).adminRegister(player.getName(), password, null);

            //login in the player after registration
            forceLogin(player);
        }
    }
}
