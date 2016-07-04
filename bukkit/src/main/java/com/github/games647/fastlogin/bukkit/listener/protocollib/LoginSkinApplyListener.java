package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

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
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        Player player = loginEvent.getPlayer();

        BukkitLoginSession session = plugin.getSessions().get(player.getAddress().toString());
        if (session != null && plugin.getConfig().getBoolean("forwardSkin")) {
            WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
            String skinData = session.getEncodedSkinData();
            String signature = session.getSkinSignature();
            if (skinData != null && signature != null) {
                WrappedSignedProperty skin = WrappedSignedProperty.fromValues("textures", skinData, signature);
                gameProfile.getProperties().put("textures", skin);
            }
        }
    }
}
