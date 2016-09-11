package com.github.games647.fastlogin.bungee.hooks;

import com.github.games647.fastlogin.core.AuthPlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * @deprecated please use com.github.games647.fastlogin.core.AuthPlugin<net.md_5.bungee.api.connection.ProxiedPlayer>
 */
@Deprecated
public interface BungeeAuthPlugin extends AuthPlugin<ProxiedPlayer> {

    boolean forceLogin(ProxiedPlayer player);

    boolean isRegistered(String playerName) throws Exception;

    boolean forceRegister(ProxiedPlayer player, String password);
}
