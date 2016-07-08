package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class LoginSkinApplyListener implements Listener {

    private final FastLoginBukkit plugin;

    public LoginSkinApplyListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    //run this on the loginEvent to let skins plugins see the skin like in normal minecraft behaviour
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        Player player = loginEvent.getPlayer();

        if (plugin.getConfig().getBoolean("forwardSkin")) {
            //go through every session, because player.getAddress is null 
            //loginEvent.getAddress is just a InetAddress not InetSocketAddres, so not unique enough
            Collection<BukkitLoginSession> sessions = plugin.getSessions().values();
            for (BukkitLoginSession session : sessions) {
                if (session.getUsername().equals(player.getName())) {
                    applySkin(player, session);
                    break;
                }
            }
        }
    }

    private void applySkin(Player player, BukkitLoginSession session) {
        WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
        String skinData = session.getEncodedSkinData();
        String signature = session.getSkinSignature();
        if (skinData != null && signature != null) {
            WrappedSignedProperty skin = WrappedSignedProperty.fromValues("textures", skinData, signature);
            gameProfile.getProperties().put("textures", skin);
        }
    }
}
