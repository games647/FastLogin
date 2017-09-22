package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.mojang.MojangApiConnector;
import com.google.common.net.HostAndPort;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

public interface PlatformPlugin<C> {

    String getName();

    File getDataFolder();

    Logger getLogger();

    void sendMessage(C receiver, String message);

    ThreadFactory getThreadFactory();

    MojangApiConnector makeApiConnector(List<String> addresses, int requests, List<HostAndPort> proxies);
}
