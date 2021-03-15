package com.github.games647.fastlogin.core;

import com.github.games647.fastlogin.core.shared.LoginSession;
import com.google.common.collect.MapMaker;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages player connection sessions. Login sessions that are only valid through the login process and play
 * sessions that hold the stored profile object after the login process is finished and until the player leaves the
 * server (Spigot) or proxy (BungeeCord).
 *
 * @param <C> connection object
 * @param <S> platform dependent login session
 */
public abstract class SessionManager<E, C, S extends LoginSession> {

    // 1 minutes should be enough as a timeout for bad internet connection (Server, Client and Mojang)
    // these login sessions are only meant for during the login process not be used after
    private final ConcurrentMap<C, S> loginSessions =  CommonUtil.buildCache(1, 0);
    private final ConcurrentMap<UUID, StoredProfile> playSessions = new MapMaker().makeMap();

    public void startLoginSession(C connectionId, S session) {
        loginSessions.put(connectionId, session);
    }

    public S getLoginSession(C connectionId) {
        return loginSessions.get(connectionId);
    }

    public void endLoginSession(C connectionId) {
        loginSessions.remove(connectionId);
    }

    public ConcurrentMap<C, S> getLoginSessions() {
        return loginSessions;
    }

    public S promoteSession(C connectionId, UUID playerId) {
        S loginSession = loginSessions.remove(connectionId);
        StoredProfile profile = loginSession.getProfile();
        playSessions.put(playerId, profile);
        return loginSession;
    }

    public StoredProfile getPlaySession(UUID playerId) {
        return playSessions.get(playerId);
    }

    public void endPlaySession(UUID playerId) {
        playSessions.remove(playerId);
    }

    public abstract void onPlayQuit(E quitEvent);

    public void clear() {
        loginSessions.clear();
        playSessions.clear();
    }
}
