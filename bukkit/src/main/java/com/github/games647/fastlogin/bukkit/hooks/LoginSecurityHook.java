package com.github.games647.fastlogin.bukkit.hooks;

import com.avaje.ebeaninternal.api.ClassUtil;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.AuthPlugin;
import com.google.common.base.Charsets;
import com.lenis0012.bukkit.loginsecurity.LoginSecurity;
import com.lenis0012.bukkit.loginsecurity.session.AuthService;
import com.lenis0012.bukkit.loginsecurity.session.PlayerSession;
import com.lenis0012.bukkit.loginsecurity.session.action.LoginAction;
import com.lenis0012.bukkit.loginsecurity.session.action.RegisterAction;
import com.lenis0012.bukkit.ls.data.DataManager;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Github: https://github.com/lenis0012/LoginSecurity-2 Project page:
 *
 * Bukkit: http://dev.bukkit.org/bukkit-plugins/loginsecurity/
 * Spigot: https://www.spigotmc.org/resources/loginsecurity.19362/
 */
public class LoginSecurityHook implements AuthPlugin<Player> {

    protected final com.lenis0012.bukkit.ls.LoginSecurity securityPlugin;
    protected final FastLoginBukkit plugin = (FastLoginBukkit) Bukkit.getPluginManager().getPlugin("FastLogin");
    protected final boolean newVersion;

    public LoginSecurityHook() {
        this.newVersion = ClassUtil.isPresent("com.lenis0012.bukkit.loginsecurity.LoginSecurity", getClass());
        if (newVersion) {
            this.securityPlugin = null;
        } else {
            this.securityPlugin = com.lenis0012.bukkit.ls.LoginSecurity.instance;
        }
    }

    @Override
    public boolean forceLogin(Player player) {
        if (!newVersion) {
            return oldForceLogin(player);
        }

        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        return session.performAction(new LoginAction(AuthService.PLUGIN, plugin)).isSuccess();
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        if (!newVersion) {
            return oldIsRegistred(playerName);
        }

        PlayerSession session = LoginSecurity.getSessionManager().getOfflineSession(playerName);
        return session.isRegistered();
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        if (!newVersion) {
            return oldForceRegister(player, password);
        }

        PlayerSession session = LoginSecurity.getSessionManager().getPlayerSession(player);
        return session.performAction(new RegisterAction(AuthService.PLUGIN, plugin, password)).isSuccess();
    }

    public boolean oldForceLogin(final Player player) {
//Login command of this plugin: (How the plugin logs the player in)
        //https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/commands/LoginCommand.java#L39

        //not thread-safe operation
        Future<Boolean> future = Bukkit.getScheduler().callSyncMethod(securityPlugin, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String name = player.getName().toLowerCase();

                //mark the user as logged in
                securityPlugin.authList.remove(name);
                //cancel timeout timer
                securityPlugin.thread.timeout.remove(name);
                //remove effects and restore location
                securityPlugin.rehabPlayer(player, name);

                return true;
            }
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            securityPlugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }
    }

    public boolean oldIsRegistred(String playerName) throws Exception {
        //https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/LoginSecurity.java#L296
        DataManager dataManager = securityPlugin.data;

        //https://github.com/lenis0012/LoginSecurity-2/blob/master/src/main/java/com/lenis0012/bukkit/ls/LoginSecurity.java#L283
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));
        return dataManager.isRegistered(offlineUuid.toString().replace("-", ""));
    }

    public boolean oldForceRegister(Player player, String password) {
        DataManager dataManager = securityPlugin.data;

        UUID playerUUID = player.getUniqueId();
        String uuidString = playerUUID.toString().replace("-", "");
        InetAddress ipAddress = player.getAddress().getAddress();
        String passwordHash = securityPlugin.hasher.hash(password);

        //this executes a sql query without interacting with other parts so we can run it async.
        dataManager.register(uuidString, passwordHash, securityPlugin.hasher.getTypeId(), ipAddress.toString());
        String storedPassword = dataManager.getPassword(uuidString);
        if (storedPassword != null && storedPassword.equals(passwordHash)) {
            //the register method silents any excpetion so check if our entry was saved
            return forceLogin(player);
        }

        return false;
    }
}
