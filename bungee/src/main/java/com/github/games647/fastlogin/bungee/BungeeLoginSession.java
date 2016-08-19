package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.LoginSession;
import com.github.games647.fastlogin.core.PlayerProfile;

public class BungeeLoginSession extends LoginSession {

    private boolean alreadySaved;
    private boolean alreadyLogged;

    public BungeeLoginSession(String username, boolean registered, PlayerProfile profile) {
        super(username, registered, profile);
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isAlreadySaved() {
        return alreadySaved;
    }

    public void setAlreadySaved(boolean alreadySaved) {
        this.alreadySaved = alreadySaved;
    }

    public boolean isAlreadyLogged() {
        return alreadyLogged;
    }

    public void setAlreadyLogged(boolean alreadyLogged) {
        this.alreadyLogged = alreadyLogged;
    }
}
