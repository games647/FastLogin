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

            //mark the player online
            xAuthPlugin.getAuthClass(xAuthPlayer).online(xAuthPlayer.getName());
        }
    }
}
