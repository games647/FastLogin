package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.auth.LoginSession;

public class BungeeLoginSession extends LoginSession {

    private boolean alreadySaved;
    private boolean alreadyLogged;

    public BungeeLoginSession(String username, boolean registered, StoredProfile profile) {
        super(username, registered, profile);
    }

    public synchronized void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public synchronized boolean isAlreadySaved() {
        return alreadySaved;
    }

    public synchronized void setAlreadySaved(boolean alreadySaved) {
        this.alreadySaved = alreadySaved;
    }

    public synchronized boolean isAlreadyLogged() {
        return alreadyLogged;
    }

    public synchronized void setAlreadyLogged(boolean alreadyLogged) {
        this.alreadyLogged = alreadyLogged;
    }

    @Override
    public synchronized String toString() {
        return this.getClass().getSimpleName() + '{' +
                "alreadySaved=" + alreadySaved +
                ", alreadyLogged=" + alreadyLogged +
                ", registered=" + registered +
                "} " + super.toString();
    }
}
