package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import com.lenis0012.bukkit.loginsecurity.session.AuthService;
import com.lenis0012.bukkit.loginsecurity.session.PlayerSession;
import com.lenis0012.bukkit.loginsecurity.session.action.LoginAction;
import com.lenis0012.bukkit.loginsecurity.session.action.RegisterAction;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Github: https://github.com/lenis0012/LoginSecurity-2 Project page:
 *
 * Bukkit: https://dev.bukkit.org/bukkit-plugins/loginsecurity/
 * Spigot: https://www.spigotmc.org/resources/loginsecurity.19362/
 */
public class LoginSecurityHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin = JavaPlugin.getPlugin(FastLoginBukkit.class);

    @Override
    public boolean forceLogin(Player player) {
        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        if (session.isAuthorized()) {
            return true;
        }

        return session.performAction(new LoginAction(AuthService.PLUGIN, plugin)).isSuccess();
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        PlayerSession session = LoginSecurity.getSessionManager().getOfflineSession(playerName);
        return session.isRegistered();
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        return session.performAction(new RegisterAction(AuthService.PLUGIN, plugin, password)).isSuccess();
    }
}
