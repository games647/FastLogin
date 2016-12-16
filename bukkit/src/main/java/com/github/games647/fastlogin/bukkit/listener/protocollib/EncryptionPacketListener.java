package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


/**
 * Handles incoming encryption responses from connecting clients.
 * It prevents them from reaching the server because that cannot handle
 * it in offline mode.
 *
 * Moreover this manages a started premium check from
 * this plugin. So check if all data is correct and we can prove him as a
 * owner of a paid minecraft account.
 *
 * Receiving packet information:
 * http://wiki.vg/Protocol#Encryption_Response
 *
 * sharedSecret=encrypted byte array
 * verify token=encrypted byte array
 */
public class EncryptionPacketListener extends PacketAdapter {

    //hides the inherit Plugin plugin field, but we need this type
    private final FastLoginBukkit plugin;

    public EncryptionPacketListener(FastLoginBukkit plugin) {
        //run async in order to not block the server, because we make api calls to Mojang
        super(params(plugin, PacketType.Login.Client.ENCRYPTION_BEGIN).optionAsync());

        this.plugin = plugin;
    }

    public static void register(FastLoginBukkit plugin, int workerThreads) {
        ProtocolLibrary.getProtocolManager().getAsynchronousManager()
                .registerAsyncHandler(new EncryptionPacketListener(plugin)).start(workerThreads);
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
     *
     * Minecraft Server implementation
     * https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/LoginListener.java#L180
     */
    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {
        if (packetEvent.isCancelled()) {
            return;
        }

        Player sender = packetEvent.getPlayer();
        byte[] sharedSecret = packetEvent.getPacket().getByteArrays().read(0);
        
        packetEvent.getAsyncMarker().incrementProcessingDelay();
        VerifyResponseTask verifyTask = new VerifyResponseTask(plugin, packetEvent, sender, sharedSecret);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, verifyTask);
    }
}
