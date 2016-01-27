package com.github.games647.fastlogin.bukkit.hooks;

import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.xAuthPlayer;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/LycanDevelopment/xAuth/
 * Project page: http://dev.bukkit.org/bukkit-plugins/xauth/
 */
public class xAuthHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        xAuth xAuthPlugin = xAuth.getPlugin();

        xAuthPlayer xAuthPlayer = xAuthPlugin.getPlayerManager().getPlayer(player);
        if (xAuthPlayer != null) {
            xAuthPlugin.getPlayerManager().doLogin(xAuthPlayer);

            //we checked that the player is premium (paid account)
            xAuthPlayer.setPremium(true);
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
            xAuthPlugin.getAuthClass(xAuthPlayer).adminRegister(player.getName(), password, null);

            //we checked that the player is premium (paid account)
            xAuthPlayer.setPremium(true);

            //unprotect the inventory, op status...
            xAuthPlugin.getPlayerManager().doLogin(xAuthPlayer);
        }
    }
}
