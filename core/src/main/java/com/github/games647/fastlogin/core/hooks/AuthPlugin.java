/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.core.hooks;

/**
 * Represents a supporting authentication plugin in BungeeCord and Bukkit/Spigot/... servers
 *
 * @param <P> either {@link org.bukkit.entity.Player} for Bukkit or {@link net.md_5.bungee.api.connection.ProxiedPlayer}
 *           for BungeeCord
 */
public interface AuthPlugin<P> {

    String ALREADY_AUTHENTICATED = "Player {} is already authenticated. Cancelling force login.";

    /**
     * Login the premium (paid account) player after the player joined successfully the server.
     * <p>
     * <strong>This operation will be performed async while the player successfully
     * joined the server.</strong>
     *
     * @param player the player that needs to be logged in
     * @return if the operation was successful
     */
    boolean forceLogin(P player);

    /**
     * Forces a register in order to protect the paid account.
     * <p>
     * <strong>This operation will be performed async while the player successfully
     * joined the server.</strong>
     * <p>
     * After a successful registration the player should be logged
     * in too.
     * <p>
     * The method will be called only for premium accounts.
     * So it's recommended to set additionally premium property
     * if possible.
     * <p>
     * Background: If we don't register an account, cracked players
     * could steal the unregistered account from the paid
     * player account
     *
     * @param player the premium account
     * @param password a strong random generated password
     * @return if the operation was successful
     */
    boolean forceRegister(P player, String password);

    /**
     * Checks whether an account exists for this player name.
     * <p>
     * This check should check if a cracked player account exists,
     * so we can be sure the premium player doesn't steal the account
     * of that player.
     * <p>
     * This operation will be performed async while the player is
     * connecting.
     *
     * @param playerName player name
     * @return if the player has an account
     * @throws Exception if an error occurred
     */
    boolean isRegistered(String playerName) throws Exception;
}
