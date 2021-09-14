package com.github.games647.fastlogin.velocity;

import com.github.games647.fastlogin.core.shared.LoginSource;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.net.InetSocketAddress;

public class VelocityLoginSource implements LoginSource {

    private InboundConnection connection;
    private PreLoginEvent preLoginEvent;

    public VelocityLoginSource(InboundConnection connection, PreLoginEvent preLoginEvent) {
        this.connection = connection;
        this.preLoginEvent = preLoginEvent;
    }

    @Override
    public void enableOnlinemode() {
        preLoginEvent.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
    }

    @Override
    public void kick(String message) {


        if (message == null) {
            preLoginEvent.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.text("Kicked").color(NamedTextColor.WHITE)));
        } else {
            preLoginEvent.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(message)));
        }
    }

    @Override
    public InetSocketAddress getAddress() {
        return connection.getRemoteAddress();
    }

    public InboundConnection getConnection() {
        return connection;
    }
}
