package com.github.games647.fastlogin.bukkit.hooks;

import org.bukkit.entity.Player;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;

public class RoyalAuthHook implements AuthPlugin {

    @Override
    public void forceLogin(Player player) {
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(player);
        authPlayer.login();
    }

    @Override
    public boolean isRegistered(String playerName) {
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(playerName);
        return authPlayer.isRegistered();
    }

    @Override
    public void forceRegister(Player player, String password) {
        AuthPlayer authPlayer = AuthPlayer.getAuthPlayer(player);
        authPlayer.setPassword(password, Config.passwordHashType);

        //login in the player after registration
        forceLogin(player);
    }
}
