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
     *
     * <strong>This operation will be performed async while the player successfully
     * joined the server.</strong>
     *
     * @param player the player that needs to be logged in
     * @return if the operation was successful
     */
    boolean forceLogin(P player);

    /**
     * Forces a register in order to protect the paid account.
     *
     * <strong>This operation will be performed async while the player successfully
     * joined the server.</strong>
     *
     * After a successful registration the player should be logged
     * in too.
     *
     * The method will be called only for premium accounts.
     * So it's recommended to set additionally premium property
     * if possible.
     *
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
     *
     * This check should check if a cracked player account exists
     * so we can be sure the premium player doesn't steal the account
     * of that player.
     *
     * This operation will be performed async while the player is
     * connecting.
     *
     * @param playerName player name
     * @return if the player has an account
     * @throws Exception if an error occurred
     */
    boolean isRegistered(String playerName) throws Exception;
}
