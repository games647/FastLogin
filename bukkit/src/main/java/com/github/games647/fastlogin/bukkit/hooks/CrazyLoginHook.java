package com.github.games647.fastlogin.bukkit.hooks;

import com.comphenix.protocol.reflect.FuzzyReflection;

import de.st_ddt.crazylogin.CrazyLogin;
import de.st_ddt.crazylogin.data.LoginPlayerData;
import de.st_ddt.crazylogin.databases.CrazyLoginDataDatabase;
import de.st_ddt.crazylogin.listener.PlayerListener;
import de.st_ddt.crazylogin.metadata.Authenticated;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Github: https://github.com/ST-DDT/CrazyLogin
 * Project page: http://dev.bukkit.org/server-mods/crazylogin/
 */
public class CrazyLoginHook implements AuthPlugin {

    private final PlayerListener playerListener = getListener();

    @Override
    public void forceLogin(Player player) {
        CrazyLogin crazyLoginPlugin = CrazyLogin.getPlugin();

        LoginPlayerData playerData = crazyLoginPlugin.getPlayerData(player.getName());
        if (playerData != null) {
            //mark the account as logged in
            playerData.setLoggedIn(true);

            String ip = player.getAddress().getAddress().getHostAddress();
//this should be done after login to restore the inventory, unhide players, prevent potential memory leaks...
//extracted from: https://github.com/ST-DDT/CrazyLogin/blob/master/src/main/java/de/st_ddt/crazylogin/CrazyLogin.java#L1948
            playerData.resetLoginFails();
            player.setFireTicks(0);

            if (playerListener != null) {
                playerListener.removeMovementBlocker(player);
                playerListener.disableHidenInventory(player);
                playerListener.disableSaveLogin(player);
                playerListener.unhidePlayer(player);
            }

            //loginFailuresPerIP.remove(IP);
            //illegalCommandUsesPerIP.remove(IP);
            //tempBans.remove(IP);
            playerData.addIP(ip);
            crazyLoginPlugin.getCrazyDatabase().saveWithoutPassword(playerData);
            player.setMetadata("Authenticated", new Authenticated(crazyLoginPlugin, player));
            crazyLoginPlugin.unregisterDynamicHooks();
        }
    }

    @Override
    public boolean isRegistered(String playerName) {
        CrazyLogin crazyLoginPlugin = CrazyLogin.getPlugin();
        return crazyLoginPlugin.getPlayerData(playerName) != null;
    }

    @Override
    public void forceRegister(final Player player, String password) {
        final CrazyLogin crazyLoginPlugin = CrazyLogin.getPlugin();
        final CrazyLoginDataDatabase crazyDatabase = crazyLoginPlugin.getCrazyDatabase();

        //this executes a sql query and accesses only thread safe collections so we can run it async
        Bukkit.getScheduler().runTaskAsynchronously(crazyLoginPlugin, new Runnable() {
            @Override
            public void run() {
                LoginPlayerData playerData = crazyLoginPlugin.getPlayerData(player.getName());
                if (playerData == null) {
                    //create a fake account - this will be saved to the database with the password=FAILEDLOADING
                    //user cannot login with that password unless the admin uses plain text
                    //this automatically marks the player as logged in
                    playerData = new LoginPlayerData(player);
                    crazyDatabase.save(playerData);

                    //this method is not thread-safe and requires the existence of the account
                    //so reschedule it to the main thread
                    Bukkit.getScheduler().runTask(crazyLoginPlugin, new Runnable() {
                        @Override
                        public void run() {
                            //login the player after registration
                            forceLogin(player);
                        }
                    });
                }
            }
        });
    }

    private PlayerListener getListener() {
        CrazyLogin pluginInstance = CrazyLogin.getPlugin();

        PlayerListener listener;
        try {
            listener = FuzzyReflection.getFieldValue(pluginInstance, PlayerListener.class, true);
        } catch (Exception ex) {
            pluginInstance.getLogger().log(Level.SEVERE, "Failed to get the listener instance for auto login", ex);
            listener = null;
        }

        return listener;
    }
}
