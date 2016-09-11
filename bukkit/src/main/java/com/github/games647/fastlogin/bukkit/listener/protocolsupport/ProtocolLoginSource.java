package com.github.games647.fastlogin.bukkit.listener.protocolsupport;

import com.github.games647.fastlogin.core.shared.LoginSource;

import java.net.InetSocketAddress;

import protocolsupport.api.events.PlayerLoginStartEvent;

public class ProtocolLoginSource implements LoginSource {

    private final PlayerLoginStartEvent loginStartEvent;

    public ProtocolLoginSource(PlayerLoginStartEvent loginStartEvent) {
        this.loginStartEvent = loginStartEvent;
    }

    @Override
    public void setOnlineMode() {
        loginStartEvent.setOnlineMode(true);
    }

    @Override
    public void kick(String message) {
        loginStartEvent.denyLogin(message);
    }

    @Override
    public InetSocketAddress getAddress() {
        return loginStartEvent.getAddress();
    }

    public PlayerLoginStartEvent getLoginStartEvent() {
        return loginStartEvent;
    }
}
