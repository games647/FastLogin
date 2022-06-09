/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.core.shared;

import com.github.games647.craftapi.resolver.MojangResolver;
import com.github.games647.craftapi.resolver.http.RotatingProxySelector;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.RateLimiter;
import com.github.games647.fastlogin.core.TickingRateLimiter;
import com.github.games647.fastlogin.core.hooks.AuthPlugin;
import com.github.games647.fastlogin.core.hooks.DefaultPasswordGenerator;
import com.github.games647.fastlogin.core.hooks.PasswordGenerator;
import com.github.games647.fastlogin.core.mojang.ProxyAgnosticMojangResolver;
import com.github.games647.fastlogin.core.storage.MySQLStorage;
import com.github.games647.fastlogin.core.storage.SQLStorage;
import com.github.games647.fastlogin.core.storage.SQLiteStorage;
import com.google.common.base.Ticker;
import com.google.common.net.HostAndPort;
import com.zaxxer.hikari.HikariConfig;

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
import java.util.Objects;
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

    private static final long MAX_EXPIRE_RATE = 1_000_000;

    private final Map<String, String> localeMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> pendingLogin = CommonUtil.buildCache(5, -1);
    private final Collection<UUID> pendingConfirms = new HashSet<>();
    private final T plugin;

    //private final MojangResolver resolver = new MojangResolver();
    private final MojangResolver resolver = new ProxyAgnosticMojangResolver();

    private Configuration config;
    private SQLStorage storage;
    private RateLimiter rateLimiter;
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
                            localeMessages.put(key, colored.replace("/newline", "\n"));
                        }
                    });
        } catch (IOException ioEx) {
            plugin.getLog().error("Failed to load yaml files", ioEx);
            return;
        }

        rateLimiter = createRateLimiter(config.getSection("anti-bot"));
        Set<Proxy> proxies = config.getStringList("proxies")
                .stream()
                .map(HostAndPort::fromString)
                .map(proxy -> new InetSocketAddress(proxy.getHost(), proxy.getPort()))
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

    private RateLimiter createRateLimiter(Configuration botSection) {
        boolean enabled = botSection.getBoolean("enabled", true);
        if (!enabled) {
            // no-op rate limiter
            return () -> true;
        }

        int maxCon = botSection.getInt("anti-bot.connections", 200);
        long expireTime = botSection.getLong("anti-bot.expire", 5) * 60 * 1_000L;
        if (expireTime > MAX_EXPIRE_RATE) {
            expireTime = MAX_EXPIRE_RATE;
        }

        return new TickingRateLimiter(Ticker.systemTicker(), maxCon, expireTime);
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
            config = configProvider.load(reader, defaults);
        }

        // explicitly add keys here, because Configuration.getKeys doesn't return the keys from the default configuration
        for (String key : defaults.getKeys()) {
            config.set(key, config.get(key));
        }

        return config;
    }

    public MojangResolver getResolver() {
        return resolver;
    }

    public SQLStorage getStorage() {
        return storage;
    }

    public T getPlugin() {
        return plugin;
    }

    public void sendLocaleMessage(String key, C receiver) {
        String message = localeMessages.get(key);
        if (message != null) {
            plugin.sendMultiLineMessage(receiver, message);
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

        HikariConfig databaseConfig = new HikariConfig();
        databaseConfig.setDriverClassName(driver);

        String database = config.getString("database");

        databaseConfig.setConnectionTimeout(config.getInt("timeout", 30) * 1_000L);
        databaseConfig.setMaxLifetime(config.getInt("lifetime", 30) * 1_000L);

        if (driver.contains("sqlite")) {
            storage = new SQLiteStorage(this, database, databaseConfig);
        } else {
            String host = config.get("host", "");
            int port = config.get("port", 3306);
            boolean useSSL = config.get("useSSL", false);

            if (useSSL) {
                databaseConfig.addDataSourceProperty("allowPublicKeyRetrieval", config.getBoolean("allowPublicKeyRetrieval", false));
                databaseConfig.addDataSourceProperty("serverRSAPublicKeyFile", config.getString("ServerRSAPublicKeyFile"));
                databaseConfig.addDataSourceProperty("sslMode", config.getString("sslMode", "Required"));
            }

            databaseConfig.setUsername(config.get("username", ""));
            databaseConfig.setPassword(config.getString("password"));
            storage = new MySQLStorage(this, driver, host, port, database, databaseConfig, useSSL);
        }

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
            log.warn("Please choose either MySQL (Spigot, BungeeCord), SQLite (Spigot, Sponge) or " +
                "MariaDB (Sponge, Velocity)", notFoundEx);
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

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public void setAuthPluginHook(AuthPlugin<P> authPlugin) {
        this.authPlugin = authPlugin;
    }

    public void saveDefaultFile(String fileName) {
        Path dataFolder = plugin.getPluginFolder();

        try {
            Files.createDirectories(dataFolder);

            Path configFile = dataFolder.resolve(fileName);
            if (Files.notExists(configFile)) {
                try (InputStream defaultStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
                    Files.copy(Objects.requireNonNull(defaultStream), configFile);
                }
            }
        } catch (IOException ioExc) {
            plugin.getLog().error("Cannot create plugin folder {}", dataFolder, ioExc);
        }
    }

    public void close() {
        plugin.getLog().info("Safely shutting down scheduler. This could take up to one minute.");
        plugin.getScheduler().shutdown();

        if (storage != null) {
            storage.close();
        }
    }
}
