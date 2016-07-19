package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.FastLoginCore;
import com.github.games647.fastlogin.core.MojangApiConnector;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import net.md_5.bungee.BungeeCord;

public class MojangApiBungee extends MojangApiConnector {

    public MojangApiBungee(ConcurrentMap<Object, Object> requests, Logger logger, List<String> localAddresses
            , int rateLimit) {
        super(requests, logger, localAddresses, rateLimit);
    }

    @Override
    protected UUID getUUIDFromJson(String json) {
        boolean isArray = json.startsWith("[");

        MojangPlayer mojangPlayer;
        if (isArray) {
            mojangPlayer = BungeeCord.getInstance().gson.fromJson(json, MojangPlayer[].class)[0];
        } else {
            mojangPlayer = BungeeCord.getInstance().gson.fromJson(json, MojangPlayer.class);
        }

        return FastLoginCore.parseId(mojangPlayer.getId());
    }

    @Override
    public boolean hasJoinedServer(Object session, String serverId) {
        //this is not needed in Bungee
        throw new UnsupportedOperationException("Not supported");
    }
}
