package com.github.games647.fastlogin.bukkit.event;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.event.FastLoginPremiumToggleEvent;
import com.github.games647.fastlogin.core.shared.event.PremiumToggleReason;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BukkitFastLoginPremiumToggleEvent extends Event implements FastLoginPremiumToggleEvent {

    private static final HandlerList handlers = new HandlerList();
    private final StoredProfile profile;
    private final PremiumToggleReason reason;

    public BukkitFastLoginPremiumToggleEvent(StoredProfile profile, PremiumToggleReason reason) {
        super(true);
        this.profile = profile;
        this.reason = reason;
    }

    @Override
    public StoredProfile getProfile() {
        return profile;
    }

    @Override
    public PremiumToggleReason getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
