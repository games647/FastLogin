package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.MojangApiConnector;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BungeeCore extends FastLoginCore<ProxiedPlayer> {

    private static Map<String, Object> generateConfigMap(Configuration config) {
        return config.getKeys().stream().collect(Collectors.toMap(key -> key, config::get));
    }

    private final FastLoginBungee plugin;

    public BungeeCore(FastLoginBungee plugin, Configuration config) {
        super(generateConfigMap(config));

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
            plugin.saveDefaultFile("messages.yml");
            ConfigurationProvider configProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

            Configuration defaults = configProvider.load(getClass().getResourceAsStream("/messages.yml"));

            File messageFile = new File(getDataFolder(), "messages.yml");
            Configuration messageConfig = configProvider.load(messageFile, defaults);

            messageConfig.getKeys().forEach(key -> {
                String message = ChatColor.translateAlternateColorCodes('&', messageConfig.getString(key));
                if (!message.isEmpty()) {
                    localeMessages.put(key, message);
                }
            });
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Failed to load messages", ex);
        }
    }

    @Override
    public void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        plugin.saveDefaultFile("config.yml");
    }

    @Override
    public MojangApiConnector makeApiConnector(Logger logger, List<String> addresses, int requests) {
        return new MojangApiBungee(logger, addresses, requests);
    }
}
