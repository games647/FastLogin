package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private final boolean isNewAPIAvailable;

    public AuthMeHook() {
        boolean apiAvailable = false;
        try {
            Class.forName("fr.xephi.authme.api.NewAPI");
            apiAvailable = true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AuthMeHook.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.isNewAPIAvailable = apiAvailable;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean forceLogin(Player player) {
        //skips registration and login
        if (isNewAPIAvailable) {
            if (NewAPI.getInstance().isAuthenticated(player)) {
                return false;
            } else {
                NewAPI.getInstance().forceLogin(player);
            }
        } else if (!API.isAuthenticated(player)) {
            API.forceLogin(player);
        }

        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isRegistered(String playerName) throws Exception {
        if (isNewAPIAvailable) {
            return NewAPI.getInstance().isRegistered(playerName);
        } else {
            return API.isRegistered(playerName);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean forceRegister(Player player, String password) {
        if (isNewAPIAvailable) {
            //this automatically registers the player too
            NewAPI.getInstance().forceRegister(player, password);
        } else {
            API.registerPlayer(player.getName(), password);
            forceLogin(player);
        }

        return true;
    }
}
