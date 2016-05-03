package com.github.games647.fastlogin.bungee.hooks;

import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Represents a supporting authentication plugin in BungeeCord/Waterfall/... servers
 */
public interface BungeeAuthPlugin {

    /**
     * Login the premium (paid account) player after
     * the player joined successfully a server.
     *
     * @param player the player that needs to be logged in
     * @return if the operation was successful
     */
    boolean forceLogin(ProxiedPlayer player);

    /**
     * Checks whether an account exists for this player name.
     *
     * This check should check if a cracked player account exists
     * so we can be sure the premium player doesn't steal the account
     * of that player.
     *
     * This operation will be performed async while the player is
     * connecting
     *
     * @param playerName player name
     * @return if the player has an account
     * @throws Exception if an error occurred
     */
    boolean isRegistered(String playerName) throws Exception;

    /**
     * Forces a register in order to protect the paid account.
     * The method will be invoked after the player joined a server.
     *
     * After a successful registration the player should be logged
     * in too.
     *
     * The method will be called only for premium accounts.
     * So it's recommended to set additionally premium property
     * if possible.
     *
     * If we don't register an account, cracked players
     * could steal the unregistered account from the paid
     * player account
     *
     * @param player the premium account
     * @param password a strong random generated password
     * @return if the operation was successful
     */
    boolean forceRegister(ProxiedPlayer player, String password);
}
