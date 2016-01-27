package com.github.games647.fastlogin.bukkit.hooks;

import org.bukkit.entity.Player;

/**
 * Represents a supporting authentication plugin
 */
public interface AuthPlugin {

    /**
     * Login the premium (paid account) player
     *
     * @param player the player that needs to be logged in
     */
    void forceLogin(Player player);

    /**
     * Checks whether an account exists for this player name.
     *
     * This check should check if a cracked player account exists
     * so we can be sure the premium player doesn't steal the account
     * of that player.
     *
     * @param playerName player name
     * @return if the player has an account
     */
    boolean isRegistered(String playerName);

    /**
     * Forces a register in order to protect the paid account.
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
     */
    void forceRegister(Player player, String password);
}
