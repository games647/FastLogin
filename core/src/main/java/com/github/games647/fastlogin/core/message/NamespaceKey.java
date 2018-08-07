package com.github.games647.fastlogin.core.message;

public class NamespaceKey {

    private static final char SEPARATOR_CHAR = ':';

    private final String namespace;
    private final String key;

    private final String combined;

    public NamespaceKey(String namespace, String key) {
        this.namespace = namespace.toLowerCase();
        this.key = key.toLowerCase();

        this.combined = this.namespace + SEPARATOR_CHAR + this.key;
    }

    public String getCombinedName()  {
        return combined;
    }

    public static String getCombined(String namespace, String key) {
        return new NamespaceKey(namespace, key).combined;
    }
}
