package com.github.games647.fastlogin.core.storage;

import com.github.games647.fastlogin.core.StoredProfile;

import java.util.UUID;

public interface AuthStorage {
    StoredProfile loadProfile(String name);

    StoredProfile loadProfile(UUID uuid);

    void save(StoredProfile playerProfile);

    void close();
}
