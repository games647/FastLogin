package com.github.games647.fastlogin.bukkit;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.entity.Player;

public class DefaultPasswordGenerator implements PasswordGenerator {

    @Override
    public String getRandomPassword(Player player) {
        return RandomStringUtils.random(8, true, true);
    }
}
