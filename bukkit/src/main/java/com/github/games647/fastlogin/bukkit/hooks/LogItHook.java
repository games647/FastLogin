package com.github.games647.fastlogin.bukkit.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import io.github.lucaseasedup.logit.CancelledState;
import io.github.lucaseasedup.logit.LogItCore;
import io.github.lucaseasedup.logit.account.Account;
import io.github.lucaseasedup.logit.session.SessionManager;
import org.bukkit.entity.Player;

/**
 * Github: https://github.com/XziomekX/LogIt
 * Project page:
 *
 * Bukkit: Unknown
 * Spigot: Unknown
 */
public class LogItHook implements AuthPlugin<Player> {

    @Override
    public boolean forceLogin(Player player) {
        SessionManager sessionManager = LogItCore.getInstance().getSessionManager();
        if (sessionManager.isSessionAlive(player)) {
            return true;
        }

        return sessionManager.startSession(player) == CancelledState.NOT_CANCELLED;
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
