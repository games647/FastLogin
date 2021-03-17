package com.github.games647.fastlogin.bukkit.auth.protocolsupport;

import com.github.games647.fastlogin.core.auth.LoginSource;

import java.net.InetSocketAddress;

import protocolsupport.api.events.PlayerLoginStartEvent;

public class ProtocolLoginSource implements LoginSource {

    private final PlayerLoginStartEvent loginStartEvent;

    protected ProtocolLoginSource(PlayerLoginStartEvent loginStartEvent) {
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

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "loginStartEvent=" + loginStartEvent +
                '}';
    }
}
