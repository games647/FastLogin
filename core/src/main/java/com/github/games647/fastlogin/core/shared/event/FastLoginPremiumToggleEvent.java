package com.github.games647.fastlogin.core.shared.event;

import com.github.games647.fastlogin.core.StoredProfile;

public interface FastLoginPremiumToggleEvent {

    StoredProfile getProfile();
    PremiumToggleReason getReason();
}
