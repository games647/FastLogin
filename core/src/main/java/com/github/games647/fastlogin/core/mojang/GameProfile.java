package com.github.games647.fastlogin.core.mojang;

import java.util.UUID;

public class GameProfile {

    private UUID id;
    private String name;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
