package com.github.games647.fastlogin.bukkit;

import com.comphenix.protocol.utility.SafeCacheBuilder;
import com.github.games647.fastlogin.core.FastLoginCore;
import com.github.games647.fastlogin.core.PlayerProfile;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.File;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BukkitCore extends FastLoginCore {

    private final FastLoginBukkit plugin;

    public BukkitCore(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public ConcurrentMap<String, PlayerProfile> buildCache() {
        return SafeCacheBuilder
                .<String, PlayerProfile>newBuilder()
                .concurrencyLevel(20)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build(new CacheLoader<String, PlayerProfile>() {
                    @Override
                    public PlayerProfile load(String key) throws Exception {
                        //should be fetched manually
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                });
    }

    @Override
    public ThreadFactory getThreadFactory() {
        String pluginName = plugin.getName();
        return new ThreadFactoryBuilder()
                .setNameFormat(pluginName + " Database Pool Thread #%1$d")
                //Hikari create daemons by default
                .setDaemon(true)
                .build();
    }
}
