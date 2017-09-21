package com.github.games647.fastlogin.bungee.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;

import me.vik1395.BungeeAuth.Main;
import me.vik1395.BungeeAuthAPI.RequestHandler;

import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Github: https://github.com/vik1395/BungeeAuth-Minecraft
 *
 * Project page:
 *
 * Spigot: https://www.spigotmc.org/resources/bungeeauth.493/
 */
public class BungeeAuthHook implements AuthPlugin<ProxiedPlayer> {

    private final RequestHandler requestHandler = new RequestHandler();

    @Override
    public boolean forceLogin(ProxiedPlayer player) {
        String playerName = player.getName();
        return Main.plonline.contains(playerName) || requestHandler.forceLogin(playerName);

    }

    @Override
    public boolean isRegistered(String playerName) throws Exception {
        return requestHandler.isRegistered(playerName);
    }

    @Override
    public boolean forceRegister(ProxiedPlayer player, String password) {
        return requestHandler.forceRegister(player, password);
    }
}
