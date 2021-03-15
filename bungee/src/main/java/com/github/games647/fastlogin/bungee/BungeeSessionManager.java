package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.SessionManager;

import java.util.UUID;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeSessionManager extends SessionManager<PlayerDisconnectEvent, PendingConnection, BungeeLoginSession>
        implements Listener {

    //todo: memory leak on cancelled login event
    @EventHandler
    public void onPlayQuit(PlayerDisconnectEvent disconnectEvent) {
        ProxiedPlayer player = disconnectEvent.getPlayer();
        UUID playerId = player.getUniqueId();
        endPlaySession(playerId);
    }
}
