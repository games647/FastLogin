package com.github.games647.fastlogin.bungee.hooks;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;

import me.vik1395.BungeeAuth.ListenerClass;
import me.vik1395.BungeeAuth.Main;
import me.vik1395.BungeeAuth.Password.PasswordHandler;
import me.vik1395.BungeeAuth.Tables;

import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Github: https://github.com/MatteCarra/BungeeAuth
 *
 * Project page:
 *
 * Spigot: https://www.spigotmc.org/resources/bungeeauth.493/
 */
public class BungeeAuthHook implements BungeeAuthPlugin {

    //https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Login.java#L32
    private final Tables databaseConnection = new Tables();

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
//https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Login.java#L92-95
        Main.plonline.add(player.getName());

        //renamed from ct to databaseConnection
//            databaseConnection.setStatus(player.getName(), "online");
        Class<?>[] parameterTypes = new Class<?>[]{String.class, String.class};
        Object[] arguments = new Object[]{player.getName(), "online"};

        try {
            callProtected("setStatus", parameterTypes, arguments);
            ListenerClass.movePlayer(player, false);

            //proparly not thread-safe
            ListenerClass.prelogin.get(player.getName()).cancel();
        } catch (Exception ex) {
            Main.plugin.getLogger().log(Level.SEVERE, "Error force loging in player", ex);
            return false;
        }

        return true;
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        //https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Register.java#L46
        //renamed t to databaseConnection
        return databaseConnection.checkPlayerEntry(playerName);
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player, String password) {
        //https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Register.java#L102
        PasswordHandler ph = new PasswordHandler();
        Random rand = new Random();
        int maxp = 7; //Total Password Hashing methods.
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        String Pw = password;
        String pType = "" + rand.nextInt(maxp + 1);
        String regdate = ft.format(dNow);
        //https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Register.java#L60
        String lastip = player.getAddress().getAddress().getHostAddress();
        String lastseen = regdate;
        String hash = ph.newHash(Pw, pType);

        //creates a new SQL entry with the player's details.

        //renamed t to databaseConnection
//            databaseConnection.newPlayerEntry(player.getName(), hash, pType, "", lastip, regdate, lastip, lastseen);

        Class<?>[] parameterTypes = new Class<?>[] {String.class, String.class, String.class, String.class
                , String.class, String.class, String.class, String.class};
        Object[] arguments = new Object[] {player.getName(), hash, pType, "", lastip, regdate, lastip, lastseen};

        try {
            callProtected("newPlayerEntry", parameterTypes, arguments);
            //proparly not thread-safe
            forceLogin(player);
        } catch (Exception ex) {
            Main.plugin.getLogger().log(Level.SEVERE, "[BungeeAuth] Error when creating a new player in the Database", ex);
            return false;
        }

        return true;
    }

    //pail ;(
    private void callProtected(String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Exception {
        Class<Tables> tableClass = Tables.class;

        Method method = tableClass.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(databaseConnection, arguments);
    }
}
