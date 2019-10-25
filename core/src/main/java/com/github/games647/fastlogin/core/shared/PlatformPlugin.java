package com.github.games647.fastlogin.core.shared;

import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;

public interface PlatformPlugin<C> {

    String getName();

    Path getPluginFolder();

    Logger getLog();

    void sendMessage(C receiver, String message);

    default void sendMultiLineMessage(C receiver, String message) {
        for (String line : message.split("%nl%")) {
            sendMessage(receiver, line);
        }
    }

    default ThreadFactory getThreadFactory() {
        return null;
    }
}
