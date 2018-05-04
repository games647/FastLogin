package com.github.games647.fastlogin.bukkit.listener.protocolsupport;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.JoinManagement;

import java.net.InetSocketAddress;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupport.api.events.PlayerLoginStartEvent;

public class ProtocolSupportListener extends JoinManagement<Player, CommandSender, ProtocolLoginSource>
        implements Listener {

    private final FastLoginBukkit plugin;

    public ProtocolSupportListener(FastLoginBukkit plugin) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook());

        this.plugin = plugin;
    }

    @EventHandler
    public void onLoginStart(PlayerLoginStartEvent loginStartEvent) {
        if (loginStartEvent.isLoginDenied() || plugin.getCore().getAuthPluginHook() == null) {
            return;
        }

        String username = loginStartEvent.getName();
        InetSocketAddress address = loginStartEvent.getAddress();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getLoginSessions().remove(address.toString());

        super.onLogin(username, new ProtocolLoginSource(loginStartEvent));
    }

    @EventHandler
    public void onConnectionClosed(ConnectionCloseEvent closeEvent) {
        InetSocketAddress address = closeEvent.getConnection().getAddress();
        plugin.getLoginSessions().remove(address.toString());
    }

    @EventHandler
    public void onPropertiesResolve(PlayerLoginFinishEvent loginFinishEvent) {
        if (!loginFinishEvent.isOnlineMode()) {
            return;
        }

        InetSocketAddress address = loginFinishEvent.getAddress();
        BukkitLoginSession session = plugin.getLoginSessions().get(address.toString());

        if (session != null) {
            session.setVerified(true);
        }
    }

    @Override
    public void requestPremiumLogin(ProtocolLoginSource source, StoredProfile profile, String username
            , boolean registered) {
        source.setOnlineMode();

        String ip = source.getAddress().getAddress().getHostAddress();
        plugin.getCore().getPendingLogin().put(ip + username, new Object());

        BukkitLoginSession playerSession = new BukkitLoginSession(username, null, null
                , registered, profile);
        plugin.getLoginSessions().put(source.getAddress().toString(), playerSession);
        if (plugin.getConfig().getBoolean("premiumUuid")) {
            source.getLoginStartEvent().setUseOnlineModeUUID(true);
        }
    }

    @Override
    public void startCrackedSession(ProtocolLoginSource source, StoredProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.getLoginSessions().put(source.getAddress().toString(), loginSession);
    }
}
