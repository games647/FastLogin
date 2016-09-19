package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.shared.MojangApiConnector;

import java.util.List;
import java.util.logging.Logger;

import net.md_5.bungee.BungeeCord;

public class MojangApiBungee extends MojangApiConnector {

    public MojangApiBungee(Logger logger, List<String> localAddresses, int rateLimit) {
        super(logger, localAddresses, rateLimit);
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
    public boolean hasJoinedServer(Object session, String serverId) {
        //this is not needed in Bungee
        throw new UnsupportedOperationException("Not supported");
    }
}
