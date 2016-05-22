package com.github.games647.fastlogin.bukkit.hooks;

import io.github.lucaseasedup.logit.CancelledState;
import io.github.lucaseasedup.logit.LogItCore;
import io.github.lucaseasedup.logit.account.Account;

import org.bukkit.entity.Player;

/**
 * Github: https://github.com/XziomekX/LogIt
 * Project page:
 *
 * Bukkit: Unknown
 * Spigot: Unknown
 */
public class LogItHook implements BukkitAuthPlugin {

    @Override
    public boolean forceLogin(Player player) {
        return LogItCore.getInstance().getSessionManager().startSession(player) == CancelledState.NOT_CANCELLED;
    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        return LogItCore.getInstance().getAccountManager().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        Account account = new Account(player.getName());
        account.changePassword(password);
        account.setLastActiveDate(System.currentTimeMillis() / 1000);
        account.setRegistrationDate(System.currentTimeMillis() / 1000);
        return LogItCore.getInstance().getAccountManager().insertAccount(account) == CancelledState.NOT_CANCELLED;
    }
}
