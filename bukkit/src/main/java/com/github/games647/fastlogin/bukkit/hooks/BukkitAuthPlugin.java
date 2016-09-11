package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import org.bukkit.entity.Player;

/**
 * @deprecated please use com.github.games647.fastlogin.core.hooks.AuthPlugin<org.bukkit.entity.Player>
 */
@Deprecated
public interface BukkitAuthPlugin extends AuthPlugin<Player> {

    boolean forceLogin(Player player);

    boolean isRegistered(String playerName) throws Exception;

    boolean forceRegister(Player player, String password);
}
