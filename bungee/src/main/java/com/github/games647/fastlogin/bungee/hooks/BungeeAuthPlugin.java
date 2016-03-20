package com.github.games647.fastlogin.bungee.hooks;


import net.md_5.bungee.api.connection.ProxiedPlayer;

//do not use yet - 
public interface BungeeAuthPlugin {

    /**
     *
     * @param player the player that needs to be logged in
     */
    void forceLogin(ProxiedPlayer player);

    /**
     *
     * @param playerName player name
     * @return if the player has an account
     */
    boolean isRegistered(String playerName);

    /**
     *
     * @param player the premium account
     * @param password a strong random generated password
     */
    void forceRegister(ProxiedPlayer player, String password);
}
