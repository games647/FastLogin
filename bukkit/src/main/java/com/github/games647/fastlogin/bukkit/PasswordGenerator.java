package com.github.games647.fastlogin.bukkit;

import org.bukkit.entity.Player;

/**
 *
 * @deprecated please use com.github.games647.fastlogin.core.shared.PasswordGenerator<org.bukkit.entity.Player>
 */
@Deprecated
public interface PasswordGenerator {

    String getRandomPassword(Player player);
}
