package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.auth.BukkitLoginSession;
import com.github.games647.fastlogin.core.SessionManager;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitSessionManager extends SessionManager<PlayerQuitEvent, InetSocketAddress, BukkitLoginSession>
    implements Listener {

    @EventHandler
    @Override
    public void onPlayQuit(PlayerQuitEvent quitEvent) {
        Player player = quitEvent.getPlayer();
        UUID playerId = player.getUniqueId();
        endPlaySession(playerId);
    }
}
