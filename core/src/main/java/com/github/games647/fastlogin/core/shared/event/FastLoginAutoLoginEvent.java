package com.github.games647.fastlogin.core.shared.event;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSession;

public interface FastLoginAutoLoginEvent extends FastLoginCancellableEvent {
    LoginSession getSession();
    StoredProfile getProfile();
}
