/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
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
package com.github.games647.fastlogin.bukkit.auth.proxy;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.message.ChannelMessage;
import com.github.games647.fastlogin.core.message.NamespaceKey;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class ProxyVerifier {

    private static final String LEGACY_FILE_NAME = "proxy-whitelist.txt";
    private static final String FILE_NAME = "allowed-proxies.txt";

    //null if proxies allowed list is empty so bungeecord support is disabled
    private Set<UUID> proxyIds;

    private final FastLoginBukkit plugin;

    private final Collection<UUID> firedJoinEvents = new HashSet<>();

    public ProxyVerifier(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    public void cleanup() {
        //remove old blocked status
        Bukkit.getOnlinePlayers().forEach(player -> player.removeMetadata(plugin.getName(), plugin));
    }

    public void sendPluginMessage(PluginMessageRecipient player, ChannelMessage message) {
        if (player != null) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            message.writeTo(dataOutput);

            NamespaceKey channel = new NamespaceKey(plugin.getName(), message.getChannelName());
            player.sendPluginMessage(plugin, channel.getCombinedName(), dataOutput.toByteArray());
        }
    }

    public void loadSecrets() {
        proxyIds = loadBungeeCordIds();
        if (proxyIds.isEmpty()) {
            plugin.getLog().info("No valid IDs found. Minecraft proxy support cannot work in the current state");
        }
    }

    private Set<UUID> loadBungeeCordIds() {
        Path proxiesFile = plugin.getPluginFolder().resolve(FILE_NAME);
        Path legacyFile = plugin.getPluginFolder().resolve(LEGACY_FILE_NAME);
        try {
            if (Files.notExists(proxiesFile)) {
                if (Files.exists(legacyFile)) {
                    Files.move(legacyFile, proxiesFile);
                }

                if (Files.notExists(legacyFile)) {
                    Files.createFile(proxiesFile);
                }
            }

            Files.deleteIfExists(legacyFile);
            try (Stream<String> lines = Files.lines(proxiesFile)) {
                return lines.map(String::trim).map(UUID::fromString).collect(toSet());
            }
        } catch (IOException ex) {
            plugin.getLog().error("Failed to read proxies", ex);
        } catch (Exception ex) {
            plugin.getLog().error("Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        return Collections.emptySet();
    }

    public boolean isProxyAllowed(UUID proxyId) {
        return proxyIds != null && proxyIds.contains(proxyId);
    }

    /**
     * Mark the event to be fired including the task delay.
     *
     * @param player joining player
     */
    public synchronized void markJoinEventFired(Player player) {
        firedJoinEvents.add(player.getUniqueId());
    }

    /**
     * Check if the event fired including with the task delay. This necessary to restore the order of processing the
     * BungeeCord messages after the PlayerJoinEvent fires including the delay.
     * <p>
     * If the join event fired, the delay exceeded, but it ran earlier and couldn't find the recently started login
     * session. If not fired, we can start a new force login task. This will still match the requirement that we wait
     * a certain time after the player join event fired.
     *
     * @param player joining player
     * @return event fired including delay
     */
    public synchronized boolean didJoinEventFired(Player player) {
        return firedJoinEvents.contains(player.getUniqueId());
    }

    /**
     * Player quit clean up
     *
     * @param player joining player
     */
    public synchronized void cleanup(Player player) {
        firedJoinEvents.remove(player.getUniqueId());
    }
}
