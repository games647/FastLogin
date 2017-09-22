package com.github.games647.fastlogin.core.shared;

import com.github.games647.fastlogin.core.mojang.MojangApiConnector;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

public interface PlatformPlugin<C> {

    String getName();

    File getDataFolder();

    Logger getLogger();

    void sendMessage(C receiver, String message);

    ThreadFactory getThreadFactory();

    String translateColorCodes(char colorChar, String rawMessage);

    MojangApiConnector makeApiConnector(Logger logger, List<String> addresses, int requests
            , Map<String, Integer> proxies);
}
