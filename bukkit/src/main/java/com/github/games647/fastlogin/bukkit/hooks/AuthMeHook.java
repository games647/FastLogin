package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import fr.xephi.authme.api.NewAPI;
import fr.xephi.authme.api.v3.AuthMeApi;
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

    private final boolean v3APIAvailable;

    public AuthMeHook() {
        boolean apiAvailable = true;
        try {
            Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
        } catch (ClassNotFoundException classNotFoundEx) {
            apiAvailable = false;
        }

        this.v3APIAvailable = apiAvailable;
    }

    @Override
    public boolean forceLogin(Player player) {
        if (v3APIAvailable) {
            //skips registration and login
            if (AuthMeApi.getInstance().isAuthenticated(player)) {
                return false;
            } else {
                AuthMeApi.getInstance().forceLogin(player);
            }
        } else {
            //skips registration and login
            if (NewAPI.getInstance().isAuthenticated(player)) {
                return false;
            } else {
                NewAPI.getInstance().forceLogin(player);
            }
        }

        return true;
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        if (v3APIAvailable) {
            return AuthMeApi.getInstance().isRegistered(playerName);
        }

        return NewAPI.getInstance().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        //this automatically registers the player too
        if (v3APIAvailable) {
            AuthMeApi.getInstance().forceRegister(player, password);
        } else {
            NewAPI.getInstance().forceRegister(player, password);
        }

        return true;
    }
}
