package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import org.bukkit.entity.Player;

/**
 * @deprecated please use com.github.games647.fastlogin.core.hooks.AuthPlugin<org.bukkit.entity.Player>
 */
@Deprecated
public interface BukkitAuthPlugin extends AuthPlugin<Player> {

    @Override
    boolean forceLogin(Player player);

    @Override
    boolean isRegistered(String playerName) throws Exception;

    @Override
    boolean forceRegister(Player player, String password);
}
