package com.github.games647.fastlogin.bukkit.hooks;

import com.avaje.ebeaninternal.api.ClassUtil;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import fr.xephi.authme.api.API;
import fr.xephi.authme.api.NewAPI;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/Xephi/AuthMeReloaded/
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/authme-reloaded/
 * Spigot: https://www.spigotmc.org/resources/authme-reloaded.6269/
 */
public class AuthMeHook implements AuthPlugin<Player> {

    private final boolean isNewAPIAvailable;

    public AuthMeHook() {
        this.isNewAPIAvailable = ClassUtil.isPresent("fr.​xephi.​authme.​api.NewAPI");
    }

    @Override
    public boolean forceLogin(Player player) {
        //skips registration and login
        if (isNewAPIAvailable) {
            NewAPI.getInstance().forceLogin(player);
        } else {
            API.forceLogin(player);
        }
        
        return true;
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        if (isNewAPIAvailable) {
            return NewAPI.getInstance().isRegistered(playerName);
        } else {
            return API.isRegistered(playerName);
        }
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        if (isNewAPIAvailable) {
            NewAPI.getInstance().forceRegister(player, password);
        } else {
            API.registerPlayer(player.getName(), password);
            forceLogin(player);
        }

        return true;
    }
}
