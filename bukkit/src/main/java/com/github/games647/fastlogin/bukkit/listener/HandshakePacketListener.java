package com.github.games647.fastlogin.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import java.util.logging.Level;

/**
 * Listens to incoming handshake packets.
 *
 * As BungeeCord sends additional information on the Handshake, we can detect it and check so if the player is coming
 * from a BungeeCord instance. IpForward has to be activated in the BungeeCord config to send these extra information.
 *
 * Packet information: http://wiki.vg/Protocol#Handshake
 *
 * Int=Protocol version String=connecting server address (and additional information from BungeeCord) int=server port
 * int=next state
 */
public class HandshakePacketListener extends PacketAdapter {

    //hides the inherit Plugin plugin field, but we need a more detailed type than just Plugin
    private final FastLoginBukkit plugin;

    public HandshakePacketListener(FastLoginBukkit plugin) {
        //run async in order to not block the server, because we are making api calls to Mojang
        super(params(plugin, PacketType.Handshake.Client.SET_PROTOCOL).optionAsync());

        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        PacketContainer packet = packetEvent.getPacket();
        PacketType.Protocol nextProtocol = packet.getProtocols().read(0);

        //we don't want to listen for server ping.
        if (nextProtocol == PacketType.Protocol.LOGIN) {
            //here are the information written separated by a space
            String hostname = packet.getStrings().read(0);
            //https://hub.spigotmc.org/stash/projects/SPIGOT/repos/spigot/browse/CraftBukkit-Patches/0055-BungeeCord-Support.patch
            String[] split = hostname.split("\00");
            if (split.length == 3 || split.length == 4) {
                plugin.getLogger().log(Level.FINER, "Detected BungeeCord for {0}", hostname);

                //object = because there are no concurrent sets with weak keys
                plugin.getBungeeCordUsers().put(packetEvent.getPlayer(), new Object());
            }
        }
    }
}
