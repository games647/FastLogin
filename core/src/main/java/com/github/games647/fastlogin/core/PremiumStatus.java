package com.github.games647.fastlogin.core;

public enum PremiumStatus {

    PREMIUM("Premium"),

    CRACKED("Cracked"),

    UNKNOWN("Unknown");

    private final String readableName;

    PremiumStatus(String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }
}
