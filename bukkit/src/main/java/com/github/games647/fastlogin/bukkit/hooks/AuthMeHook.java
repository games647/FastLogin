package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import fr.xephi.authme.api.v3.AuthMeApi;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/Xephi/AuthMeReloaded/
 * <p>
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/authme-reloaded/
 * <p>
 * Spigot: https://www.spigotmc.org/resources/authme-reloaded.6269/
 */
public class AuthMeHook implements AuthPlugin<Player> {

    @Override
    public boolean forceLogin(Player player) {
        //skips registration and login
        if (AuthMeApi.getInstance().isAuthenticated(player)) {
            return false;
        } else {
            AuthMeApi.getInstance().forceLogin(player);
        }

        return true;
    }

    @Override
    public boolean isRegistered(String playerName) {
        return AuthMeApi.getInstance().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        //this automatically registers the player too
        AuthMeApi.getInstance().forceRegister(player, password);

        return true;
    }
}
