package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.mojang.MojangApiConnector;
import com.google.common.net.HostAndPort;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;

public interface PlatformPlugin<C> {

    String getName();

    Path getPluginFolder();

    Logger getLog();

    void sendMessage(C receiver, String message);

    ThreadFactory getThreadFactory();

    MojangApiConnector makeApiConnector(List<String> addresses, int requests, List<HostAndPort> proxies);
}
