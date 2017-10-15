package com.github.games647.fastlogin.core.mojang;

import java.util.Arrays;
import java.util.UUID;

public class VerificationReply {

    private UUID id;
    private String name;
    private SkinProperties[] properties;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SkinProperties[] getProperties() {
        return Arrays.copyOf(properties, properties.length);
    }
}
