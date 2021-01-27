package com.github.games647.fastlogin.bungee.hook;

import java.sql.SQLException;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import org.slf4j.Logger;

import de.xxschrandxx.bca.bungee.BungeeCordAuthenticatorBungee;
import de.xxschrandxx.bca.bungee.api.BungeeCordAuthenticatorBungeeAPI;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * GitHub:
 * https://github.com/xXSchrandXx/SpigotPlugins/tree/master/BungeeCordAuthenticator
 *
 * Project page:
 *
 * Spigot: https://www.spigotmc.org/resources/bungeecordauthenticator.87669/
 */
public class BungeeCordAuthenticatorHook implements AuthPlugin<ProxiedPlayer> {

    public final BungeeCordAuthenticatorBungeeAPI api;

    public BungeeCordAuthenticatorHook(Plugin plugin, Logger logger) {
        BungeeCordAuthenticatorBungee bcab = (BungeeCordAuthenticatorBungee) plugin;
        api = bcab.getAPI();
        logger.info("BungeeCordAuthenticatorHook | Hooked successful!");
    }

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
        if (api.isAuthenticated(player)) {
            return true;
        } else {
            try {
                api.setAuthenticated(player);
            }
            catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        try {
            return api.getSQL().checkPlayerEntry(playerName);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player, String password) {
        try {
            return api.createPlayerEntry(player, password);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
