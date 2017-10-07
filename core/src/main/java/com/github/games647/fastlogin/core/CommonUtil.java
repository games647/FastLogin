package com.github.games647.fastlogin.core;

import com.google.common.cache.CacheLoader;

import java.lang.reflect.Constructor;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.JDK14LoggerAdapter;

public class CommonUtil {

    private static final char COLOR_CHAR = '&';
    private static final char TRANSLATED_CHAR = '§';

    public static <K, V> ConcurrentMap<K, V> buildCache(int expireAfterWrite, int maxSize) {
        CompatibleCacheBuilder<Object, Object> builder = CompatibleCacheBuilder.newBuilder();

        if (expireAfterWrite > 0) {
            builder.expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES);
        }

        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        }

        return builder.build(CacheLoader.from(() -> {
            throw new UnsupportedOperationException();
        }));
    }

    public static UUID parseId(String withoutDashes) {
        if (withoutDashes == null) {
            return null;
        }

        return UUID.fromString(withoutDashes.substring(0, 8)
                + '-' + withoutDashes.substring(8, 12)
                + '-' + withoutDashes.substring(12, 16)
                + '-' + withoutDashes.substring(16, 20)
                + '-' + withoutDashes.substring(20, 32));
    }

    public static String toMojangId(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public static String translateColorCodes(String rawMessage) {
        char[] chars = rawMessage.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = TRANSLATED_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

    public static Logger createLoggerFromJDK(java.util.logging.Logger parent) {
        try {
            parent.setLevel(Level.ALL);

            Class<JDK14LoggerAdapter> adapterClass = JDK14LoggerAdapter.class;
            Constructor<JDK14LoggerAdapter> cons = adapterClass.getDeclaredConstructor(java.util.logging.Logger.class);
            cons.setAccessible(true);
            JDK14LoggerAdapter logger = cons.newInstance(parent);
            return logger;
        } catch (ReflectiveOperationException reflectEx) {
            parent.log(Level.WARNING, "Cannot create slf4j logging adapter", reflectEx);
            parent.log(Level.WARNING, "Creating logger instance manually...");
            return LoggerFactory.getLogger(parent.getName());
        }
    }

    private CommonUtil() {
        //Utility class
    }
}
