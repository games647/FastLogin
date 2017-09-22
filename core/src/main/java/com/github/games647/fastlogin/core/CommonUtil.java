package com.github.games647.fastlogin.core;

import com.google.common.cache.CacheLoader;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CommonUtil {

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

    private CommonUtil() {
        //Utility class
    }
}
