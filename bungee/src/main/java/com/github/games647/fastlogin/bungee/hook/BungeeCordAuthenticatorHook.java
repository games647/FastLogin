package com.github.games647.fastlogin.bungee.hook;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import de.xxschrandxx.bca.bungee.BungeeCordAuthenticatorBungee;

import java.sql.SQLException;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * GitHub: https://github.com/xXSchrandXx/SpigotPlugins/tree/master/BungeeCordAuthenticator
 *
 * Project page:
 *
 * Spigot: https://www.spigotmc.org/resources/bungeecordauthenticator.87669/
 */
public class BungeeCordAuthenticatorHook implements AuthPlugin<ProxiedPlayer> {

    private final BungeeCordAuthenticatorBungee bcab;

    public BungeeCordAuthenticatorHook(Plugin plugin) {
        this.bcab = (BungeeCordAuthenticatorBungee) plugin;
    }

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
        if (bcab.getAPI().isAuthenticated(player)) {
            return true;
        }
        else {
            try {
                bcab.getAPI().setAuthenticated(player);
                return bcab.getAPI().isAuthenticated(player);
            }
            catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player == null) {
            return false;
        }
        else {
            try {
                return bcab.getAPI().getSQL().checkPlayerEntry(player);
            }
            catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player, String password) {
        try {
            return bcab.getAPI().createNewPlayerEntry(player, password);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
