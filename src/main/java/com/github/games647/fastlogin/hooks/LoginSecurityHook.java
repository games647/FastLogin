package com.github.games647.fastlogin.hooks;

import com.lenis0012.bukkit.ls.LoginSecurity;

import org.bukkit.entity.Player;

/**
 * Github: http://dev.bukkit.org/bukkit-plugins/loginsecurity/
 * Project page: https://github.com/lenis0012/LoginSecurity-2
 *
 * on join:
 * https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/LoginSecurity.java#L282
 */
public class LoginSecurityHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        //Login command of this plugin: (How the plugin logs the player in)
        //https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/commands/LoginCommand.java#L39
        LoginSecurity securityPlugin = LoginSecurity.instance;
        String name = player.getName().toLowerCase();

        //mark the user as logged in
        securityPlugin.authList.remove(name);
        //cancel timeout timer
        securityPlugin.thread.timeout.remove(name);
        //remove effects
        securityPlugin.rehabPlayer(player, name);
    }
}
