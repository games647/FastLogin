package com.github.games647.fastlogin.bukkit.auth.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.auth.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPreLoginEvent;
import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.auth.JoinManagement;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.security.PublicKey;
import java.util.Random;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NameCheckTask extends JoinManagement<Player, CommandSender, ProtocolLibLoginSource>
        implements Runnable {

    private final FastLoginBukkit plugin;
    private final PacketEvent packetEvent;
    private final PublicKey publicKey;

    private final Random random;

    private final Player player;
    private final String username;

    protected NameCheckTask(FastLoginBukkit plugin, PacketEvent packetEvent, Random random,
                         Player player, String username, PublicKey publicKey) {
        super(plugin.getCore(), plugin.getCore().getAuthPluginHook());

        this.plugin = plugin;
        this.packetEvent = packetEvent;
        this.publicKey = publicKey;
        this.random = random;
        this.player = player;
        this.username = username;
    }

    @Override
    public void run() {
        try {
            super.onLogin(username, new ProtocolLibLoginSource(packetEvent, player, random, publicKey));
        } finally {
            ProtocolLibrary.getProtocolManager().getAsynchronousManager().signalPacketTransmission(packetEvent);
        }
    }

    @Override
    public FastLoginPreLoginEvent callFastLoginPreLoginEvent(String username, ProtocolLibLoginSource source, StoredProfile profile) {
        BukkitFastLoginPreLoginEvent event = new BukkitFastLoginPreLoginEvent(username, source, profile);
        plugin.getServer().getPluginManager().callEvent(event);
        return event;
    }

    //Minecraft server implementation
    //https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L161
    @Override
    public void requestPremiumLogin(ProtocolLibLoginSource source, StoredProfile profile
            , String username, boolean registered) {
        try {
            source.setOnlineMode();
        } catch (Exception ex) {
            plugin.getLog().error("Cannot send encryption packet. Falling back to cracked login for: {}", profile, ex);
            return;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        core.getPendingLogin().put(ip + username, new Object());

        String serverId = source.getServerId();
        byte[] verify = source.getVerifyToken();

        BukkitLoginSession playerSession = new BukkitLoginSession(username, serverId, verify, registered, profile);
        plugin.getSessionManager().startLoginSession(player.getAddress(), playerSession);
        //cancel only if the player has a paid account otherwise login as normal offline player
        synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
            packetEvent.setCancelled(true);
        }
    }

    @Override
    public void startCrackedSession(ProtocolLibLoginSource source, StoredProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.getSessionManager().startLoginSession(player.getAddress(), loginSession);
    }
}
