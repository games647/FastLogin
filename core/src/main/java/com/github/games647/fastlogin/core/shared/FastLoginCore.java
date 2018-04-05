package com.github.games647.fastlogin.core.shared;

import com.github.games647.craftapi.resolver.MojangResolver;
import com.github.games647.craftapi.resolver.http.RotatingProxySelector;
import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.hooks.DefaultPasswordGenerator;
import com.github.games647.fastlogin.core.hooks.PasswordGenerator;
import com.google.common.net.HostAndPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.slf4j.Logger;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * @param <P> GameProfile class
 * @param <C> CommandSender
 * @param <T> Plugin class
 */
public class FastLoginCore<P extends C, C, T extends PlatformPlugin<C>> {

    private final Map<String, String> localeMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> pendingLogin = CommonUtil.buildCache(5, -1);
    private final Collection<UUID> pendingConfirms = new HashSet<>();
    private final T plugin;

    private final MojangResolver resolver = new MojangResolver();

    private Configuration config;
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

        Set<Proxy> proxies = config.getStringList("proxies")
                .stream()
                .map(HostAndPort::fromString)
                .map(proxy -> new InetSocketAddress(proxy.getHostText(), proxy.getPort()))
                .map(sa -> new Proxy(Type.HTTP, sa))
                .collect(toSet());

        Collection<InetAddress> addresses = new HashSet<>();
        for (String localAddress : config.getStringList("ip-addresses")) {
            try {
                addresses.add(InetAddress.getByName(localAddress.replace('-', '.')));
            } catch (UnknownHostException ex) {
                plugin.getLog().error("IP-Address is unknown to us", ex);
            }
        }

        resolver.setMaxNameRequests(config.getInt("mojang-request-limit"));
        resolver.setProxySelector(new RotatingProxySelector(proxies));
        resolver.setOutgoingAddresses(addresses);
    }

    private Configuration loadFile(String fileName) throws IOException {
        ConfigurationProvider configProvider = ConfigurationProvider.getProvider(YamlConfiguration.class);

        Configuration defaults;
        try (InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            defaults = configProvider.load(defaultStream);
        }

        Path file = plugin.getPluginFolder().resolve(fileName);

        Configuration config;
        try (Reader reader = Files.newBufferedReader(file)) {
            config = configProvider.load(reader);
        }

        //explicitly add keys here, because Configuration.getKeys doesn't return the keys from the default configuration
        for (String key : defaults.getKeys()) {
            config.set(key, defaults.get(key));
        }

        return config;
    }

    public MojangResolver getResolver() {
        return resolver;
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
