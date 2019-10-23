package com.github.games647.fastlogin.bungee.task;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.bungee.event.BungeeFastLoginAutoLoginEvent;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.message.ChannelMessage;
import com.github.games647.fastlogin.core.message.LoginActionMessage;
import com.github.games647.fastlogin.core.message.LoginActionMessage.Type;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.ForceLoginManagement;
import com.github.games647.fastlogin.core.shared.LoginSession;

import java.util.UUID;

import com.github.games647.fastlogin.core.shared.event.FastLoginAutoLoginEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class ForceLoginTask
        extends ForceLoginManagement<ProxiedPlayer, CommandSender, BungeeLoginSession, FastLoginBungee> {

    private final Server server;

    public ForceLoginTask(FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core,
             ProxiedPlayer player, Server server) {
        super(core, player, core.getPlugin().getSession().get(player.getPendingConnection()));

        this.server = server;
    }

    @Override
    public void run() {
        if (session == null) {
            return;
        }

        super.run();

        if (!isOnlineMode()) {
            session.setAlreadySaved(true);
        }
    }

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
        if (session.isAlreadyLogged()) {
            return true;
        }

        session.setAlreadyLogged(true);
        return super.forceLogin(player);
    }

    @Override
    public FastLoginAutoLoginEvent callFastLoginAutoLoginEvent(LoginSession session, StoredProfile profile) {
        return core.getPlugin().getProxy().getPluginManager()
                .callEvent(new BungeeFastLoginAutoLoginEvent(session, profile));
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player) {
        return session.isAlreadyLogged() || super.forceRegister(player);
    }

    @Override
    public void onForceActionSuccess(LoginSession session) {
        //sub channel name
        Type type = Type.LOGIN;
        if (session.needsRegistration()) {
            type = Type.REGISTER;
        }

        UUID proxyId = UUID.fromString(ProxyServer.getInstance().getConfig().getUuid());
        ChannelMessage loginMessage = new LoginActionMessage(type, player.getName(), proxyId);

        core.getPlugin().sendPluginMessage(server, loginMessage);
    }

    @Override
    public String getName(ProxiedPlayer player) {
        return player.getName();
    }

    @Override
    public boolean isOnline(ProxiedPlayer player) {
        return player.isConnected();
    }

    @Override
    public boolean isOnlineMode() {
        return player.getPendingConnection().isOnlineMode();
    }
}
