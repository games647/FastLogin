package com.github.games647.fastlogin.bukkit.event;

import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.auth.LoginSession;
import com.github.games647.fastlogin.core.shared.event.FastLoginAutoLoginEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitFastLoginAutoLoginEvent extends Event implements FastLoginAutoLoginEvent, Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final LoginSession session;
    private final StoredProfile profile;
    private boolean cancelled;

    public BukkitFastLoginAutoLoginEvent(LoginSession session, StoredProfile profile) {
        super(true);

        this.session = session;
        this.profile = profile;
    }

    @Override
    public LoginSession getSession() {
        return session;
    }

    @Override
    public StoredProfile getProfile() {
        return profile;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
