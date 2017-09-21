package com.github.games647.fastlogin.bungee.tasks;

import com.github.games647.fastlogin.bungee.BungeeLoginSession;
import com.github.games647.fastlogin.bungee.FastLoginBungee;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.ForceLoginManagement;
import com.github.games647.fastlogin.core.shared.LoginSession;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

public class ForceLoginTask extends ForceLoginManagement<ProxiedPlayer, CommandSender, BungeeLoginSession, FastLoginBungee> {

    private final Server server;

    public ForceLoginTask(FastLoginCore<ProxiedPlayer, CommandSender, FastLoginBungee> core,
             ProxiedPlayer player, Server server) {
        super(core, player);

        this.server = server;
    }

    @Override
    public void run() {
        PendingConnection pendingConnection = player.getPendingConnection();
        session = core.getPlugin().getSession().get(pendingConnection);

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
    public boolean forceRegister(ProxiedPlayer player) {
        return session.isAlreadyLogged() || super.forceRegister(player);

    }

    @Override
    public void onForceActionSuccess(LoginSession session) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        //sub channel name
        if (session.needsRegistration()) {
            dataOutput.writeUTF("AUTO_REGISTER");
        } else {
            dataOutput.writeUTF("AUTO_LOGIN");
        }

        //Data is sent through a random player. We have to tell the Bukkit version of this plugin the target
        dataOutput.writeUTF(player.getName());

        //proxy identifier to check if it's a acceptable proxy
        UUID proxyId = UUID.fromString(core.getPlugin().getProxy().getConfig().getUuid());
        dataOutput.writeLong(proxyId.getMostSignificantBits());
        dataOutput.writeLong(proxyId.getLeastSignificantBits());

        if (server != null) {
            server.sendData(core.getPlugin().getName(), dataOutput.toByteArray());
        }
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
