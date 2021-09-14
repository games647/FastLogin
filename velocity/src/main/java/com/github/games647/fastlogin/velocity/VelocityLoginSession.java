package com.github.games647.fastlogin.velocity;

import com.github.games647.fastlogin.core.StoredProfile;
import com.github.games647.fastlogin.core.shared.LoginSession;

public class VelocityLoginSession extends LoginSession {
    private boolean alreadySaved;
    private boolean alreadyLogged;
    public VelocityLoginSession(String requestUsername, boolean registered, StoredProfile profile) {
        super(requestUsername, registered, profile);
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
