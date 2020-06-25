package com.github.games647.fastlogin.bungee.event;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent;
import net.md_5.bungee.api.plugin.Event;

public class BungeeFastLoginPremiumToggleEvent extends Event implements FastLoginPremiumToggleEvent {

    private final StoredProfile profile;
    private final PremiumToggleReason reason;

    public BungeeFastLoginPremiumToggleEvent(StoredProfile profile, PremiumToggleReason reason) {
        this.profile = profile;
        this.reason = reason;
    }

    @Override
    public StoredProfile getProfile() {
        return profile;
    }

    @Override
    public FastLoginPremiumToggleEvent.PremiumToggleReason getReason() {
        return reason;
    }
}
