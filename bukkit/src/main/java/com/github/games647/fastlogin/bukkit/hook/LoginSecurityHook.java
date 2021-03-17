package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import com.lenis0012.bukkit.loginsecurity.session.AuthService;
import com.lenis0012.bukkit.loginsecurity.session.PlayerSession;
import com.lenis0012.bukkit.loginsecurity.session.action.LoginAction;
import com.lenis0012.bukkit.loginsecurity.session.action.RegisterAction;

import org.bukkit.entity.Player;

/**
 * GitHub: https://github.com/lenis0012/LoginSecurity-2
 * <p>
 * Project page:
 * <p>
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/loginsecurity/
 * <p>
 * Spigot: https://www.spigotmc.org/resources/loginsecurity.19362/
 */
public class LoginSecurityHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin;

    protected LoginSecurityHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        if (session.isAuthorized()) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return true;
        }

        return session.isAuthorized()
                || session.performAction(new LoginAction(AuthService.PLUGIN, plugin)).isSuccess();
    }

    @Override
    public boolean isRegistered(String playerName) {
        PlayerSession session = LoginSecurity.getSessionManager().getOfflineSession(playerName);
        return session.isRegistered();
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        return session.performAction(new RegisterAction(AuthService.PLUGIN, plugin, password)).isSuccess();
    }
}
