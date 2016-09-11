package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.BungeeLoginSource;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.github.games647.fastlogin.core.shared.JoinManagement;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PreLoginEvent;

public class AsyncPremiumCheck extends JoinManagement<ProxiedPlayer, BungeeLoginSource> implements Runnable {

    private final FastLoginBungee plugin;
    private final PreLoginEvent preLoginEvent;

    public AsyncPremiumCheck(FastLoginBungee plugin, PreLoginEvent preLoginEvent) {
        super(plugin.getCore(), plugin.getBungeeAuthPlugin());

        this.plugin = plugin;
        this.preLoginEvent = preLoginEvent;
    }

    @Override
    public void run() {
        PendingConnection connection = preLoginEvent.getConnection();
        plugin.getSession().remove(connection);

        String username = connection.getName();
        try {
            super.onLogin(username, new BungeeLoginSource(connection));
        } finally {
            preLoginEvent.completeIntent(plugin);
        }
    }

    @Override
    public void requestPremiumLogin(BungeeLoginSource source, PlayerProfile profile, String username, boolean registered) {
        source.setOnlineMode();
        plugin.getSession().put(source.getConnection(), new BungeeLoginSession(username, registered, profile));

        String ip = source.getAddress().getAddress().getHostAddress();
        plugin.getCore().getPendingLogins().put(ip + username, new Object());
    }

    @Override
    public void startCrackedSession(BungeeLoginSource source, PlayerProfile profile, String username) {
        plugin.getSession().put(source.getConnection(), new BungeeLoginSession(username, false, profile));
    }
}
