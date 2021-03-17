package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.auth.LoginSource;

import java.net.InetSocketAddress;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;

public class BungeeLoginSource implements LoginSource {

    private final PendingConnection connection;
    private final PreLoginEvent preLoginEvent;

    public BungeeLoginSource(PendingConnection connection, PreLoginEvent preLoginEvent) {
        this.connection = connection;
        this.preLoginEvent = preLoginEvent;
    }

    @Override
    public void setOnlineMode() {
        connection.setOnlineMode(true);
    }

    @Override
    public void kick(String message) {
        preLoginEvent.setCancelled(true);

        if (message == null) {
            preLoginEvent.setCancelReason(new ComponentBuilder("Kicked").color(ChatColor.WHITE).create());
        } else {
            preLoginEvent.setCancelReason(TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public InetSocketAddress getAddress() {
        return connection.getAddress();
    }

    public PendingConnection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "connection=" + connection +
                '}';
    }
}
