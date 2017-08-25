package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.shared.LoginSession;
import com.github.games647.fastlogin.core.shared.MojangApiConnector;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.md_5.bungee.BungeeCord;

public class MojangApiBungee extends MojangApiConnector {

    public MojangApiBungee(Logger logger, List<String> localAddresses, int rateLimit, Map<String, Integer> proxies) {
        super(logger, localAddresses, rateLimit, proxies);
    }

    @Override
    protected String getUUIDFromJson(String json) {
        boolean isArray = json.startsWith("[");

        MojangPlayer mojangPlayer;
        if (isArray) {
            mojangPlayer = BungeeCord.getInstance().gson.fromJson(json, MojangPlayer[].class)[0];
        } else {
            mojangPlayer = BungeeCord.getInstance().gson.fromJson(json, MojangPlayer.class);
        }

        String id = mojangPlayer.getId();
        if ("null".equals(id)) {
            return null;
        }

        return id;
    }

    @Override
    public boolean hasJoinedServer(LoginSession session, String serverId, InetSocketAddress ip) {
        //this is not needed in Bungee
        throw new UnsupportedOperationException("Not supported");
    }
}
