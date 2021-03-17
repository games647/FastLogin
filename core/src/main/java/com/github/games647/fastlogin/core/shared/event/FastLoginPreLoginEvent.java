package com.github.games647.fastlogin.core.shared.event;

import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.auth.LoginSource;

public interface FastLoginPreLoginEvent {

    String getUsername();

    LoginSource getSource();

    StoredProfile getProfile();
}
