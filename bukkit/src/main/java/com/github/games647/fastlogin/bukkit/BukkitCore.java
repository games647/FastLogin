package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.core.FastLoginCore;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.File;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

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
    public ThreadFactory getThreadFactory() {
        String pluginName = plugin.getName();
        return new ThreadFactoryBuilder()
                .setNameFormat(pluginName + " Database Pool Thread #%1$d")
                //Hikari create daemons by default
                .setDaemon(true)
                .build();
    }

    @Override
    public void loadMessages() {
        plugin.saveResource("messages.yml", false);

        File messageFile = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        for (String key : messageConfig.getKeys(false)) {
            String message = ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key));
            localeMessages.put(key, message);
        }
    }

    @Override
    public void loadConfig() {
        plugin.saveConfig();
    }
}
