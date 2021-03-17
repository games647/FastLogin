package com.github.games647.fastlogin.bukkit.auth.protocollib;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.github.games647.craftapi.model.skin.Textures;
import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

public class SkinApplyListener implements Listener {

    private static final Class<?> GAME_PROFILE = MinecraftReflection.getGameProfileClass();
    private static final MethodAccessor GET_PROPERTIES = Accessors.getMethodAccessor(GAME_PROFILE, "getProperties");

    private final FastLoginBukkit plugin;

    protected SkinApplyListener(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    //run this on the loginEvent to let skins plugins see the skin like in normal Minecraft behaviour
    public void onPlayerLogin(PlayerLoginEvent loginEvent) {
        if (loginEvent.getResult() != Result.ALLOWED) {
            return;
        }

        Player player = loginEvent.getPlayer();

        //go through every session, because player.getAddress is null
        //loginEvent.getAddress is just a InetAddress not InetSocketAddress, so not unique enough
        BukkitLoginSession session = plugin.getSessionManager().getLoginSession(player.getAddress());
        if (session.getUsername().equals(player.getName())) {
            session.getSkin().ifPresent(skin -> applySkin(player, skin.getValue(), skin.getSignature()));
        }
    }

    private void applySkin(Player player, String skinData, String signature) {
        WrappedGameProfile gameProfile = WrappedGameProfile.fromPlayer(player);
        WrappedSignedProperty skin = WrappedSignedProperty.fromValues(Textures.KEY, skinData, signature);
        gameProfile.getProperties().put(Textures.KEY, skin);
    }
}
