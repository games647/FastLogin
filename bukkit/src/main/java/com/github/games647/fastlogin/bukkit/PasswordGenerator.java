package com.github.games647.fastlogin.bukkit;

import org.bukkit.entity.Player;

public interface PasswordGenerator {

    String getRandomPassword(Player player);
}
