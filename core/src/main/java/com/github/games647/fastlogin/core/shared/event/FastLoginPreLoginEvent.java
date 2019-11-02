package com.github.games647.fastlogin.core.shared.event;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSource;

public interface FastLoginPreLoginEvent {

    String getUsername();
    LoginSource getSource();
    StoredProfile getProfile();
}
