package com.github.games647.fastlogin.hooks;

import de.luricos.bukkit.xAuth.xAuth;
import de.luricos.bukkit.xAuth.xAuthPlayer;
import de.luricos.bukkit.xAuth.xAuthPlayer.Status;

import java.sql.Timestamp;

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
            //we checked that the player is premium (paid account)
            xAuthPlayer.setPremium(true);
            //mark the player online
            xAuthPlugin.getAuthClass(xAuthPlayer).online(xAuthPlayer.getName());

            //update last login time
            xAuthPlayer.setLoginTime(new Timestamp(System.currentTimeMillis()));

            //mark the player as logged in
            xAuthPlayer.setStatus(Status.AUTHENTICATED);

            //restore inventory
            xAuthPlugin.getPlayerManager().unprotect(xAuthPlayer);
        }
    }
}
