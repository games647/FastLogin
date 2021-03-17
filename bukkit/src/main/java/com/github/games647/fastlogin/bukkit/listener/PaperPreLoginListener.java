package com.github.games647.fastlogin.bukkit.listener;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.games647.craftapi.model.skin.Textures;
import com.github.games647.fastlogin.bukkit.auth.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

public class PaperPreLoginListener implements Listener {

    private final FastLoginBukkit plugin;

    public PaperPreLoginListener(final FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    //if paper is used - player skin must be set at pre login, otherwise usercache is used
    //using usercache makes premium name change basically impossible
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != Result.ALLOWED) {
            return;
        }

        // event gives us only IP, not the port, so we need to loop through all the sessions
        for (BukkitLoginSession session : plugin.getSessionManager().getLoginSessions().values()) {
            if (!event.getName().equals(session.getUsername())) {
                continue;
            }

            session.getSkin().ifPresent(skin -> event.getPlayerProfile().setProperty(new ProfileProperty(Textures.KEY,
                    skin.getValue(), skin.getSignature())));
            break;
        }
    }

}
