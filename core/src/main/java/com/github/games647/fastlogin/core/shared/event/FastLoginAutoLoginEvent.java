package com.github.games647.fastlogin.core.shared.event;

import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.auth.LoginSession;

public interface FastLoginAutoLoginEvent extends FastLoginCancellableEvent {
    LoginSession getSession();

    StoredProfile getProfile();
}
