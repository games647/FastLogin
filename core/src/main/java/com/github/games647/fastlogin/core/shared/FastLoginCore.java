package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.hooks.DefaultPasswordGenerator;
import com.github.games647.fastlogin.core.hooks.PasswordGenerator;
import com.github.games647.fastlogin.core.mojang.MojangApiConnector;
import com.google.common.net.HostAndPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.slf4j.Logger;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @param <P> GameProfile class
 * @param <C> CommandSender
 * @param <T> Plugin class
 */
public class FastLoginCore<P extends C, C, T extends PlatformPlugin<C>> {

    protected final Map<String, String> localeMessages = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> pendingLogin = CommonUtil.buildCache(5, -1);
    private final Collection<UUID> pendingConfirms = new HashSet<>();
    private final T plugin;

    private Configuration config;
    private MojangApiConnector apiConnector;
    private AuthStorage storage;
    private PasswordGenerator<P> passwordGenerator = new DefaultPasswordGenerator<>();
    private AuthPlugin<P> authPlugin;

    public FastLoginCore(T plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaultFile("messages.yml");
        saveDefaultFile("config.yml");

        try {
            config = loadFile("config.yml");
            Configuration messages = loadFile("messages.yml");

            messages.getKeys()
                    .stream()
                    .filter(key -> messages.get(key) != null)
                    .collect(toMap(identity(), messages::get))
                    .forEach((key, message) -> {
                        String colored = CommonUtil.translateColorCodes((String) message);
                        if (!colored.isEmpty()) {
                            localeMessages.put(key, colored);
                        }
                    });
        } catch (IOException ioEx) {
            plugin.getLog().error("Failed to load yaml files", ioEx);
        }

        List<String> ipAddresses = config.getStringList("ip-addresses");
        int requestLimit = config.getInt("mojang-request-limit");
        List<String> proxyList = config.get("proxies", new ArrayList<>());
        List<HostAndPort> proxies = proxyList.stream().map(HostAndPort::fromString).collect(toList());

        this.apiConnector = plugin.makeApiConnector(ipAddresses, requestLimit, proxies);
    }

    private Configuration loadFile(String fileName) throws IOException {
        Configuration defaults;

        ConfigurationProvider configProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        try (InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            defaults = configProvider.load(defaultStream);
        }

        Path file = plugin.getPluginFolder().resolve(fileName);
        return configProvider.load(Files.newBufferedReader(file), defaults);
    }

    public MojangApiConnector getApiConnector() {
        return apiConnector;
    }

    public AuthStorage getStorage() {
        return storage;
    }

    public T getPlugin() {
        return plugin;
    }

    public void sendLocaleMessage(String key, C receiver) {
        String message = localeMessages.get(key);
        if (message != null) {
            plugin.sendMessage(receiver, message);
        }
    }

    public String getMessage(String key) {
        return localeMessages.get(key);
    }

    public boolean setupDatabase() {
        String driver = config.getString("driver");
        if (!checkDriver(driver)) {
            return false;
        }

        String host = config.get("host", "");
        int port = config.get("port", 3306);
        String database = config.getString("database");

        String user = config.get("username", "");
        String password = config.get("password", "");

        boolean useSSL = config.get("useSSL", false);

        storage = new AuthStorage(this, driver, host, port, database, user, password, useSSL);
        try {
            storage.createTables();
            return true;
        } catch (Exception ex) {
            plugin.getLog().warn("Failed to setup database. Disabling plugin...", ex);
            return false;
        }
    }

    private boolean checkDriver(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException notFoundEx) {
            Logger log = plugin.getLog();
            log.warn("This driver {} is not supported on this platform", className);
            log.warn("Please choose MySQL (Spigot+BungeeCord), SQLite (Spigot+Sponge) or MariaDB (Sponge)", notFoundEx);
        }

        return false;
    }


    public Configuration getConfig() {
        return config;
    }

    public PasswordGenerator<P> getPasswordGenerator() {
        return passwordGenerator;
    }

    public void setPasswordGenerator(PasswordGenerator<P> passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    public ConcurrentMap<String, Object> getPendingLogin() {
        return pendingLogin;
    }

    public Collection<UUID> getPendingConfirms() {
        return pendingConfirms;
    }

    public AuthPlugin<P> getAuthPluginHook() {
        return authPlugin;
    }

    public void setAuthPluginHook(AuthPlugin<P> authPlugin) {
        this.authPlugin = authPlugin;
    }

    public void saveDefaultFile(String fileName) {
        Path dataFolder = plugin.getPluginFolder();

        try {
            if (Files.notExists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }

            Path configFile = dataFolder.resolve(fileName);
            if (Files.notExists(configFile)) {
                try (InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
                    Files.copy(defaultStream, configFile);
                }
            }
        } catch (IOException ioExc) {
            plugin.getLog().error("Cannot create plugin folder {}", dataFolder, ioExc);
        }
    }

    public void close() {
        if (storage != null) {
            storage.close();
        }
    }
}
