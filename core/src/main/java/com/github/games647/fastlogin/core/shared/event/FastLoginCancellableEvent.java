package com.github.games647.fastlogin.core.shared.event;

public interface FastLoginCancellableEvent {

    boolean isCancelled();
    void setCancelled(boolean cancelled);
}
