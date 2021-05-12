package com.github.games647.fastlogin.bungee.hook.floodgate;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;

import java.util.UUID;

public class FloodgateV2Hook implements FloodgateHook {

    private FloodgateApi floodgateApi;

    public FloodgateV2Hook() {
        this.floodgateApi = InstanceHolder.getApi();
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return floodgateApi.isFloodgatePlayer(uuid);
    }

}
