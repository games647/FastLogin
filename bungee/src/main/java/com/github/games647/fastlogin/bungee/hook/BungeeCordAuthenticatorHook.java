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

    public BungeeCordAuthenticatorBungeeAPI api;

    public boolean register(Plugin plugin, Logger log) {
        if (plugin instanceof BungeeCordAuthenticatorBungee) {
          BungeeCordAuthenticatorBungee bcab = (BungeeCordAuthenticatorBungee) plugin;
          if (bcab != null) {
            api = bcab.getAPI();
          }
        }
        else {
            log.warn("BungeeCordAuthenticatorBungee is null!");
            BungeeCordAuthenticatorBungee bcab = BungeeCordAuthenticatorBungee.getInstance();
            if (bcab != null) {
                api = bcab.getAPI();
            }
            else {
                log.warn("BungeeCordAuthenticatorBungee is null, again!");
            }
        }
        if (api == null) {
            return false;
        }
        else {
            return true;
        }
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
