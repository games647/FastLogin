package com.github.games647.fastlogin.bukkit.hooks;

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

    @Override
    public boolean isRegistered(String playerName) {
        return NewAPI.getInstance().isRegistered(playerName);
    }

    @Override
    public void forceRegister(Player player, String password) {
        NewAPI.getInstance().forceRegister(player, password);
    }
}
