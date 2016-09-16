package com.github.games647.fastlogin.bukkit;

import org.bukkit.entity.Player;

/**
 *
 * @deprecated please use com.github.games647.fastlogin.core.hooks.PasswordGenerator<org.bukkit.entity.Player>
 */
@Deprecated
public interface PasswordGenerator extends com.github.games647.fastlogin.core.hooks.PasswordGenerator<Player> {

    @Override
    String getRandomPassword(Player player);
}
