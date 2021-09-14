package com.github.games647.fastlogin.velocity;

import com.github.games647.fastlogin.core.AsyncScheduler;
import com.github.games647.fastlogin.core.CommonUtil;
import com.github.games647.fastlogin.core.message.ChangePremiumMessage;
import com.github.games647.fastlogin.core.message.ChannelMessage;
import com.github.games647.fastlogin.core.message.NamespaceKey;
import com.github.games647.fastlogin.core.message.SuccessMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.github.games647.fastlogin.velocity.listener.ConnectListener;
import com.github.games647.fastlogin.velocity.listener.PluginMessageListener;
import com.google.common.collect.MapMaker;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

@Plugin(id = "fastlogin")
public class FastLoginVelocity implements PlatformPlugin<CommandSource> {
    private final ProxyServer server;
    private final Path dataDirectory;
    private final Logger logger;
    private FastLoginCore<Player, CommandSource, FastLoginVelocity> core;
    private final ConcurrentMap<InetSocketAddress, VelocityLoginSession> session = new MapMaker().weakKeys().makeMap();
    private AsyncScheduler scheduler;

    @Inject
    public FastLoginVelocity(ProxyServer server, java.util.logging.Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = CommonUtil.createLoggerFromJDK(logger);
        this.dataDirectory = dataDirectory;
        logger.info("FastLogin velocity.");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        scheduler = new AsyncScheduler(logger, getThreadFactory());

        core = new FastLoginCore<>(this);
        core.load();
        if (!core.setupDatabase()) {
            return;
        }
        logger.info("Velocity uuid for allowed proxies:" + UUID.nameUUIDFromBytes("velocity".getBytes(StandardCharsets.UTF_8)));
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.create(getName(), ChangePremiumMessage.CHANGE_CHANNEL));
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.create(getName(), SuccessMessage.SUCCESS_CHANNEL));
        server.getEventManager().register(this, new ConnectListener(this, core.getRateLimiter()));
        server.getEventManager().register(this, new PluginMessageListener(this));
    }

    @Override
    public String getName() {
        //FIXME: some dynamic way to get it?
        return "fastlogin";
    }

    @Override
    public Path getPluginFolder() {
        return dataDirectory;
    }

    @Override
    public Logger getLog() {
        return logger;
    }

    @Override
    public void sendMessage(CommandSource receiver, String message) {
        receiver.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));

    }

    @Override
    public AsyncScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public boolean isPluginInstalled(String name) {
        return server.getPluginManager().isLoaded(name);
    }

    public FastLoginCore<Player, CommandSource, FastLoginVelocity> getCore() {
        return core;
    }

    public ConcurrentMap<InetSocketAddress, VelocityLoginSession>  getSession() {
        return session;
    }

    public ProxyServer getProxy() {
        return server;
    }

    public void sendPluginMessage(RegisteredServer server, ChannelMessage message) {
        if (server != null) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            message.writeTo(dataOutput);

            MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.create(getName(), message.getChannelName());
            server.sendPluginMessage(channel, dataOutput.toByteArray());
        }
    }
}
