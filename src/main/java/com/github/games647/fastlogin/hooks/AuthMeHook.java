package com.github.games647.fastlogin.hooks;

import fr.xephi.authme.api.NewAPI;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/Xephi/AuthMeReloaded/
 * Project page: http://dev.bukkit.org/bukkit-plugins/authme-reloaded/
 */
public class AuthMeHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        //skips registration and login
        NewAPI.getInstance().forceLogin(player);
    }
}
