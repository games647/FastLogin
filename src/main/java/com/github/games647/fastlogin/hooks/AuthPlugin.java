package com.github.games647.fastlogin.hooks;

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
}
