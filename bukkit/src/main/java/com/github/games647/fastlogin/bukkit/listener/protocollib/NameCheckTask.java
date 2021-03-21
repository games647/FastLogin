package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginPreLoginEvent;
import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.JoinManagement;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;

import java.security.PublicKey;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.network.session.GeyserSession;

public class NameCheckTask extends JoinManagement<Player, CommandSender, ProtocolLibLoginSource>
        implements Runnable {

    private final FastLoginBukkit plugin;
    private final PacketEvent packetEvent;
    private final PublicKey publicKey;

    private final Random random;

    private final Player player;
    private final String username;

    public NameCheckTask(FastLoginBukkit plugin, PacketEvent packetEvent, Random random,
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
            // check if the player is connecting through Geyser
            if (!plugin.getCore().getConfig().getString("allowFloodgateNameConflict").equalsIgnoreCase("false") &&
                    Bukkit.getServer().getPluginManager().isPluginEnabled("Geyser-Spigot") &&
                    GeyserConnector.getInstance().getDefaultAuthType() == AuthType.FLOODGATE) {
                // the Floodgate API requires UUID, which is inaccessible at this state
                // workaround: iterate over Geyser's player's usernames
                for (GeyserSession geyserPlayer : GeyserConnector.getInstance().getPlayers()) {
                    if (geyserPlayer.getName().equals(username)) {
                        plugin.getLog().info(
                                "Skipping name conflict checking for player {}",
                                username);
                        return;
                    }
                }
            }
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
        plugin.putSession(player.getAddress(), playerSession);
        //cancel only if the player has a paid account otherwise login as normal offline player
        synchronized (packetEvent.getAsyncMarker().getProcessingLock()) {
            packetEvent.setCancelled(true);
        }
    }

    @Override
    public void startCrackedSession(ProtocolLibLoginSource source, StoredProfile profile, String username) {
        BukkitLoginSession loginSession = new BukkitLoginSession(username, profile);
        plugin.putSession(player.getAddress(), loginSession);
    }
}
