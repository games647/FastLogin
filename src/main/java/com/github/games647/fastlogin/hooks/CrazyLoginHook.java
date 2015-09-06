package com.github.games647.fastlogin.hooks;

import de.st_ddt.crazylogin.CrazyLogin;
import de.st_ddt.crazylogin.data.LoginPlayerData;
import de.st_ddt.crazylogin.databases.CrazyLoginDataDatabase;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/ST-DDT/CrazyLogin
 * Project page: http://dev.bukkit.org/server-mods/crazylogin/
 */
public class CrazyLoginHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        CrazyLogin crazyLoginPlugin = CrazyLogin.getPlugin();
        CrazyLoginDataDatabase crazyDatabase = crazyLoginPlugin.getCrazyDatabase();

        LoginPlayerData playerData = crazyLoginPlugin.getPlayerData(player.getName());
        if (playerData == null) {
            //create a fake account - this will be saved to the database with the password=FAILEDLOADING
            //user cannot login with that password unless the admin uses plain text
            playerData = new LoginPlayerData(player);
            crazyDatabase.save(playerData);
        } else {
            //mark the account as logged in
            playerData.setLoggedIn(true);
        }
    }
}
