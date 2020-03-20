package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.core.AsyncScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

public class BukkitScheduler extends AsyncScheduler {

    private final Plugin plugin;
    private final Executor syncExecutor;

    public BukkitScheduler(Plugin plugin, Logger logger, ThreadFactory threadFactory) {
        super(logger, threadFactory);
        this.plugin = plugin;

        syncExecutor = r -> Bukkit.getScheduler().runTask(plugin, r);
    }

    public Executor getSyncExecutor() {
        return syncExecutor;
    }
}
