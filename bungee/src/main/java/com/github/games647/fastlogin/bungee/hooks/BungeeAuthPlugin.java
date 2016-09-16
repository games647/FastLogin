package com.github.games647.fastlogin.bungee.hooks;

import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * @deprecated please use com.github.games647.fastlogin.core.hooks.AuthPlugin<net.md_5.bungee.api.connection.ProxiedPlayer>
 */
@Deprecated
public interface BungeeAuthPlugin extends AuthPlugin<ProxiedPlayer> {

    @Override
    boolean forceLogin(ProxiedPlayer player);

    @Override
    boolean isRegistered(String playerName) throws Exception;

    @Override
    boolean forceRegister(ProxiedPlayer player, String password);
}
