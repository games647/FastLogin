package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.github.games647.fastlogin.core.shared.JoinManagement;

import java.util.Random;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

public class NameCheckTask extends JoinManagement<Player, CommandSender, ProtocolLibLoginSource>
        implements Runnable {

    private final FastLoginBukkit plugin;
    private final PacketEvent packetEvent;

    private final Random random;

    private final Player player;
    private final String username;

    public NameCheckTask(FastLoginBukkit plugin, PacketEvent packetEvent, Random random, Player player, String username) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook());

        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.random = random;
        this.player = player;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            super.onLogin(username, new ProtocolLibLoginSource(plugin, packetEvent, player, random));
        } finally {
            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packetEvent);
        }
    }

    //minecraft server implementation
    //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L161
    @Override
    public void requestPremiumLogin(ProtocolLibLoginSource source, PlayerProfile profile, String username, boolean registered) {
        try {
            source.setOnlineMode();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Cannot send encryption packet. Falling back to cracked login", ex);
            return;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        core.getPendingLogins().put(ip + username, new Object());

        String serverId = source.getServerId();
        byte[] verify = source.getVerifyToken();

        BukkitLoginSession playerSession = new BukkitLoginSession(username, serverId, verify, registered, profile);
        plugin.getLoginSessions().put(player.getAddress().toString(), playerSession);
        //cancel only if the player has a paid account otherwise login as normal offline player
        synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
            packetEvent.setCancelled(true);
        }
    }

    @Override
    public void startCrackedSession(ProtocolLibLoginSource source, PlayerProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.getLoginSessions().put(player.getAddress().toString(), loginSession);
    }
}
