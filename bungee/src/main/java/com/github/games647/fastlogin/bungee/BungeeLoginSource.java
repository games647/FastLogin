package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.LoginSource;

import java.net.InetSocketAddress;

import net.md_5.bungee.api.connection.PendingConnection;

public class BungeeLoginSource implements LoginSource {

    private final PendingConnection connection;

    public BungeeLoginSource(PendingConnection connection) {
        this.connection = connection;
    }

    @Override
    public void setOnlineMode() {
        connection.setOnlineMode(true);
    }

    @Override
    public void kick(String message) {
        connection.disconnect(message);
    }

    @Override
    public InetSocketAddress getAddress() {
        return connection.getAddress();
    }

    public PendingConnection getConnection() {
        return connection;
    }
}
