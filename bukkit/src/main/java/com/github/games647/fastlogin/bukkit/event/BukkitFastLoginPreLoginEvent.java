package com.github.games647.fastlogin.bukkit.event;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSource;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitFastLoginPreLoginEvent extends Event implements FastLoginPreLoginEvent {

    private static final HandlerList handlers = new HandlerList();
    private final String username;
    private final LoginSource source;
    private final StoredProfile profile;

    public BukkitFastLoginPreLoginEvent(String username, LoginSource source, StoredProfile profile) {
        super(true);

        this.username = username;
        this.source = source;
        this.profile = profile;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public LoginSource getSource() {
        return source;
    }

    @Override
    public StoredProfile getProfile() {
        return profile;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
