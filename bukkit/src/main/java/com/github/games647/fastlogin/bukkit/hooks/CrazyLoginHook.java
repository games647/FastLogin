package com.github.games647.fastlogin.bukkit.hooks;

import com.comphenix.protocol.reflect.FuzzyReflection;

import de.st_ddt.crazylogin.CrazyLogin;
import de.st_ddt.crazylogin.data.LoginPlayerData;
import de.st_ddt.crazylogin.databases.CrazyLoginDataDatabase;
import de.st_ddt.crazylogin.listener.PlayerListener;
import de.st_ddt.crazylogin.metadata.Authenticated;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Github: https://github.com/ST-DDT/CrazyLogin
 *
 * Project page:
 *
 * Bukkit: http://dev.bukkit.org/server-mods/crazylogin/
 */
public class CrazyLoginHook implements BukkitAuthPlugin {

    protected final CrazyLogin crazyLoginPlugin = CrazyLogin.getPlugin();
    private final PlayerListener playerListener = getListener();

    @Override
    public boolean forceLogin(final Player player) {
        //not thread-safe operation
        Future<LoginPlayerData> future = Bukkit.getScheduler().callSyncMethod(crazyLoginPlugin
                , new Callable<LoginPlayerData>() {
            @Override
            public LoginPlayerData call() throws Exception {
                LoginPlayerData playerData = crazyLoginPlugin.getPlayerData(player.getName());
                if (playerData != null) {
                    //mark the account as logged in
                    playerData.setLoggedIn(true);

                    String ip = player.getAddress().getAddress().getHostAddress();
//this should be done after login to restore the inventory, unhide players, prevent potential memory leaks...
//from: https://github.com/ST-DDT/CrazyLogin/blob/master/src/main/java/de/st_ddt/crazylogin/CrazyLogin.java#L1948
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
                    player.setMetadata("Authenticated", new Authenticated(crazyLoginPlugin, player));
                    crazyLoginPlugin.unregisterDynamicHooks();
                    return playerData;
                }

                return null;
            }
        });

        try {
            LoginPlayerData result = future.get();
            if (result != null && result.isLoggedIn()) {
                //SQL-Queries should run async
                crazyLoginPlugin.getCrazyDatabase().saveWithoutPassword(result);
                return true;
            }
        } catch (InterruptedException | ExecutionException ex) {
            crazyLoginPlugin.getLogger().log(Level.SEVERE, "Failed to forceLogin", ex);
            return false;
        }

        return false;
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        return crazyLoginPlugin.getPlayerData(playerName) != null;
    }

    @Override
    public boolean forceRegister(final Player player, String password) {
        CrazyLoginDataDatabase crazyDatabase = crazyLoginPlugin.getCrazyDatabase();

        //this executes a sql query and accesses only thread safe collections so we can run it async
        LoginPlayerData playerData = crazyLoginPlugin.getPlayerData(player.getName());
        if (playerData == null) {
            //create a fake account - this will be saved to the database with the password=FAILEDLOADING
            //user cannot login with that password unless the admin uses plain text
            //this automatically marks the player as logged in
            playerData = new LoginPlayerData(player);
            crazyDatabase.save(playerData);

            return forceLogin(player);
        }

        return false;
    }

    private PlayerListener getListener() {
        PlayerListener listener;
        try {
            listener = FuzzyReflection.getFieldValue(crazyLoginPlugin, PlayerListener.class, true);
        } catch (Exception ex) {
            crazyLoginPlugin.getLogger().log(Level.SEVERE, "Failed to get the listener instance for auto login", ex);
            listener = null;
        }

        return listener;
    }
}
