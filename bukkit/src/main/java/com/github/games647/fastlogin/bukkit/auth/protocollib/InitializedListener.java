package com.github.games647.fastlogin.bukkit.auth.protocollib;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class InitializedListener implements Listener {

    private final ProtocolLibListener module;

    protected InitializedListener(ProtocolLibListener protocolLibModule) {
        this.module = protocolLibModule;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        if (loginEvent.getResult() == Result.ALLOWED && !module.isReadyToInject()) {
            loginEvent.disallow(Result.KICK_OTHER, module.getPlugin().getCore().getMessage("not-started"));
        }
    }
}
