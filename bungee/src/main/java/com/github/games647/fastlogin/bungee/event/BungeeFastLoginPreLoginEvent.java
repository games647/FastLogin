package com.github.games647.fastlogin.bungee.event;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSource;
import com.github.games647.fastlogin.core.shared.event.FastLoginPreLoginEvent;
import net.md_5.bungee.api.plugin.Event;

public class BungeeFastLoginPreLoginEvent extends Event implements FastLoginPreLoginEvent {

    private final String username;
    private final LoginSource source;
    private final StoredProfile profile;

    public BungeeFastLoginPreLoginEvent(String username, LoginSource source, StoredProfile profile) {
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
}
