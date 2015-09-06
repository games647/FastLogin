package com.github.games647.fastlogin.hooks;

import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.cache.limbo.LimboCache;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/Xephi/AuthMeReloaded/
 * Project page: dev.bukkit.org/bukkit-plugins/authme-reloaded/
 */
public class AuthMeHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        //here is the gamemode, inventory ... saved
        if (!LimboCache.getInstance().hasLimboPlayer(player.getName().toLowerCase())) {
            //add cache entry - otherwise logging in wouldn't work
            LimboCache.getInstance().addLimboPlayer(player);
        }

        //skips registration and login
        NewAPI.getInstance().forceLogin(player);
    }
}
