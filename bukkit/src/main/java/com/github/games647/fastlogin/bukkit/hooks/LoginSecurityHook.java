package com.github.games647.fastlogin.bukkit.hooks;

import com.google.common.base.Charsets;
import com.lenis0012.bukkit.ls.LoginSecurity;
import com.lenis0012.bukkit.ls.data.DataManager;

import java.net.InetAddress;

import java.util.UUID;

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
        //remove effects and restore location
        securityPlugin.rehabPlayer(player, name);
    }

    @Override
    public boolean isRegistered(String playerName) {
        //https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/LoginSecurity.java#L296
        LoginSecurity securityPlugin = LoginSecurity.instance;
        DataManager dataManager = securityPlugin.data;

        //https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/LoginSecurity.java#L283
        UUID offlineUuid = UUID.nameUUIDFromBytes(playerName.getBytes(Charsets.UTF_8));
        return dataManager.isRegistered(offlineUuid.toString().replace("-", ""));
        //check for sessions in order to prevent a sql query?
        //sesUse && thread.getSession().containsKey(uuid) && checkLastIp(player)) {
    }

    @Override
    public void forceRegister(Player player, String password) {
        LoginSecurity securityPlugin = LoginSecurity.instance;
        DataManager dataManager = securityPlugin.data;

        UUID playerUUID = player.getUniqueId();
        String uuidString = playerUUID.toString().replace("-", "");
        InetAddress ipAddress = player.getAddress().getAddress();
        dataManager.register(uuidString, password, securityPlugin.hasher.getTypeId(), ipAddress.toString());
    }
}
