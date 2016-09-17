package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles incoming start packets from connecting clients. It
 * checks if we can start checking if the player is premium and
 * start a request to the client that it should start online mode
 * login.
 *
 * Receiving packet information:
 * http://wiki.vg/Protocol#Login_Start
 *
 * String=Username
 */
public class StartPacketListener extends PacketAdapter {

    //hides the inherit Plugin plugin field, but we need a more detailed type than just Plugin
    private final FastLoginBukkit plugin;

    //just create a new once on plugin enable. This used for verify token generation
    private final Random random = new Random();

    public StartPacketListener(FastLoginBukkit plugin) {
        //run async in order to not block the server, because we are making api calls to Mojang
        super(params(plugin, PacketType.Login.Client.START).optionAsync());

        this.plugin = plugin;
    }

    /**
     * C->S : Handshake State=2
     * C->S : Login Start
     * S->C : Encryption Key Request
     * (Client Auth)
     * C->S : Encryption Key Response
     * (Server Auth, Both enable encryption)
     * S->C : Login Success (*)
     *
     * On offline logins is Login Start followed by Login Success
     */
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        if (packetEvent.isCancelled()
                || plugin.getCore().getAuthPluginHook()== null || !plugin.isServerFullyStarted()) {
            return;
        }

        Player player = packetEvent.getPlayer();

        //this includes ip:port. Should be unique for an incoming login request with a timeout of 2 minutes
        String sessionKey = player.getAddress().toString();

        //remove old data every time on a new login in order to keep the session only for one person
        plugin.getSessions().remove(sessionKey);

        //player.getName() won't work at this state
        PacketContainer packet = packetEvent.getPacket();

        String username = packet.getGameProfiles().read(0).getName();
        plugin.getLogger().log(Level.FINER, "Player {0} with {1} connecting", new Object[]{sessionKey, username});

        packetEvent.getAsyncMarker().incrementProcessingDelay();
        NameCheckTask nameCheckTask = new NameCheckTask(plugin, packetEvent, random, player, username);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, nameCheckTask);
    }
}
