package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.FastLoginCore;
import com.github.games647.fastlogin.core.MojangApiConnector;

import java.util.List;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;

public class MojangApiBungee extends MojangApiConnector {

    public MojangApiBungee(FastLoginCore plugin, List<String> localAddresses, boolean apiLookup) {
        super(plugin, localAddresses, apiLookup);
    }

    @Override
    protected UUID getUUIDFromJson(String json) {
        MojangPlayer mojangPlayer = BungeeCord.getInstance().gson.fromJson(json, MojangPlayer.class);
        return FastLoginCore.parseId(mojangPlayer.getId());
    }

    @Override
    public boolean hasJoinedServer(Object session, String serverId) {
        //this is not needed in Bungee
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    protected UUID getUUIDFromJsonAPI(String json) {
        McAPIProfile apiPlayer = BungeeCord.getInstance().gson.fromJson(json, McAPIProfile.class);
        return FastLoginCore.parseId(apiPlayer.getUuid());
    }
}
