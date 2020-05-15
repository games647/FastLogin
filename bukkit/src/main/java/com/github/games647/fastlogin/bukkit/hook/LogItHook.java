package com.github.games647.fastlogin.bukkit.hook;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import io.github.lucaseasedup.logit.CancelledState;
import io.github.lucaseasedup.logit.LogItCore;
import io.github.lucaseasedup.logit.account.Account;
import io.github.lucaseasedup.logit.session.SessionManager;

import java.time.Instant;

import org.bukkit.entity.Player;

/**
 * GitHub: https://github.com/XziomekX/LogIt
 * <p>
 * Project page:
 * <p>
 * Bukkit: Unknown
 * <p>
 * Spigot: Unknown
 */
public class LogItHook implements AuthPlugin<Player> {

    private final FastLoginBukkit plugin;

    public LogItHook(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean forceLogin(Player player) {
        SessionManager sessionManager = LogItCore.getInstance().getSessionManager();
        if (sessionManager.isSessionAlive(player)) {
            plugin.getLog().warn(ALREADY_AUTHENTICATED, player);
            return false;
        }

        return sessionManager.startSession(player) == CancelledState.NOT_CANCELLED;
    }

    @Override
    public boolean isRegistered(String playerName) {
        return LogItCore.getInstance().getAccountManager().isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(Player player, String password) {
        Account account = new Account(player.getName());
        account.changePassword(password);

        Instant now = Instant.now();
        account.setLastActiveDate(now.getEpochSecond());
        account.setRegistrationDate(now.getEpochSecond());
        return LogItCore.getInstance().getAccountManager().insertAccount(account) == CancelledState.NOT_CANCELLED;
    }
}
