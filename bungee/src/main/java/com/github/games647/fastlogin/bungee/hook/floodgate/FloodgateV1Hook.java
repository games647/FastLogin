package com.github.games647.fastlogin.bungee.hook.floodgate;

import org.geysermc.floodgate.FloodgateAPI;

import java.util.UUID;

public class FloodgateV1Hook implements FloodgateHook {

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return FloodgateAPI.isBedrockPlayer(uuid);
    }

}
