package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.FastLoginCore;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.ChatColor;

import net.md_5.bungee.api.scheduler.GroupedThreadFactory;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BungeeCore extends FastLoginCore {

    private final FastLoginBungee plugin;

    public BungeeCore(FastLoginBungee plugin) {
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
        String pluginName = plugin.getDescription().getName();
        return new ThreadFactoryBuilder()
                .setNameFormat(pluginName + " Database Pool Thread #%1$d")
                //Hikari create daemons by default
                .setDaemon(true)
                .setThreadFactory(new GroupedThreadFactory(plugin, pluginName)).build();
    }

    @Override
    public void loadMessages() {
        try {
            saveDefaultFile("messages.yml");
            
            File messageFile = new File(getDataFolder(), "messages.yml");
            Configuration messageConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(messageFile);
            for (String key : messageConfig.getKeys()) {
                String message = ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key));
                localeMessages.put(key, message);
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to load messages", ex);
        }
    }

    @Override
    public void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        saveDefaultFile("config.yml");
    }

    private void saveDefaultFile(String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            try (InputStream in = plugin.getResourceAsStream(fileName)) {
                Files.copy(in, configFile.toPath());
            } catch (IOException ioExc) {
                getLogger().log(Level.SEVERE, "Error saving default " + fileName, ioExc);
            }
        }
    }
}
