package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import fr.xephi.authme.api.NewAPI;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/Xephi/AuthMeReloaded/
 *
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/authme-reloaded/
 *
 * Spigot: https://www.spigotmc.org/resources/authme-reloaded.6269/
 */
public class AuthMeHook implements AuthPlugin<Player> {

    @Override
    public boolean forceLogin(Player player) {
        //skips registration and login
        if (NewAPI.getInstance().isAuthenticated(player)) {
            return false;
        } else {
            NewAPI.getInstance().forceLogin(player);
        }

        return true;
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        return NewAPI.getInstance().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        //this automatically registers the player too
        NewAPI.getInstance().forceRegister(player, password);
        return true;
    }
}
