package com.github.games647.fastlogin.bungee.hooks;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

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
    public void forceLogin(ProxiedPlayer player) {
//https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Login.java#L92-95
        Main.plonline.add(player.getName());
        //renamed from ct to databaseConnection
//        databaseConnection.setStatus(player.getName(), "online");
        ListenerClass.movePlayer(player, false);
        ListenerClass.prelogin.get(player.getName()).cancel();
    }

    @Override
    public boolean isRegistered(String playerName) {
        //https://github.com/MatteCarra/BungeeAuth/blob/master/src/me/vik1395/BungeeAuth/Register.java#L46
        //renamed t to databaseConnection
        return databaseConnection.checkPlayerEntry(playerName);
    }

    @Override
    public void forceRegister(ProxiedPlayer player, String password) {
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
//        try {
            //renamed t to databaseConnection
//            databaseConnection.newPlayerEntry(player.getName(), hash, pType, "", lastip, regdate, lastip, lastseen);
            ListenerClass.prelogin.get(player.getName()).cancel();
//        } catch (SQLException e) {
//            Main.plugin.getLogger().severe("[BungeeAuth] Error when creating a new player in the MySQL Database");
//            e.printStackTrace();
//        }
    }
}
