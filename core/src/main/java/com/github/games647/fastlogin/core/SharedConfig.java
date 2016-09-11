package com.github.games647.fastlogin.core;

import java.util.Map;

public class SharedConfig {

    private final Map<String, Object> configValues;

    public SharedConfig(Map<String, Object> configValues) {
        this.configValues = configValues;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String path, T def) {
        Object val = configValues.get(path);
        return ( val != null ) ? (T) val : def;
    }

    public <T> T get(String path) {
        return get(path, null);
    }
}
