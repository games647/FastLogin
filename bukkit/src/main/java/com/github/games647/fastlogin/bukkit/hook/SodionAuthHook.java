package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import org.bukkit.entity.Player;
import red.mohist.sodionauth.bukkit.implementation.BukkitPlayer;
import red.mohist.sodionauth.core.SodionAuthApi;
import red.mohist.sodionauth.core.exception.AuthenticatedException;

/**
 * GitHub: https://github.com/Mohist-Community/SodionAuth
 * <p>
 * Project page:
 * <p>
 * Bukkit: Unknown
 * <p>
 * Spigot: Unknown
 */
public class SodionAuthHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin;

    public SodionAuthHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        try {
            SodionAuthApi.login(new BukkitPlayer(player));
        } catch (AuthenticatedException e) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return false;
        }
        return true;
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        try{
            return SodionAuthApi.register(new BukkitPlayer(player), password);
        } catch (UnsupportedOperationException e){
            plugin.getLog().warn("Currently SodionAuth is not accepting forceRegister, " +
                    "It may be caused by unsupported AuthBackend");
            return false;
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        return SodionAuthApi.isRegistered(playerName);
    }
}
