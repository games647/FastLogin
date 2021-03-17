package com.github.games647.fastlogin.bukkit;

import com.github.games647.fastlogin.bukkit.auth.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.event.BukkitFastLoginAutoLoginEvent;
import com.github.games647.fastlogin.core.PremiumStatus;
import com.github.games647.fastlogin.core.storage.StoredProfile;
import com.github.games647.fastlogin.core.message.SuccessMessage;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.auth.ForceLoginManagement;
import com.github.games647.fastlogin.core.auth.LoginSession;
import com.github.games647.fastlogin.core.shared.event.FastLoginAutoLoginEvent;

import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class ForceLoginTask extends ForceLoginManagement<Player, CommandSender, BukkitLoginSession, FastLoginBukkit> {

    public ForceLoginTask(FastLoginCore<Player, CommandSender, FastLoginBukkit> core, Player player,
                          BukkitLoginSession session) {
        super(core, player, session);
    }

    @Override
    public void run() {
        // block this target player for proxy ID brute force attacks
        FastLoginBukkit plugin = core.getPlugin();
        player.setMetadata(core.getPlugin().getName(), new FixedMetadataValue(plugin, true));

        if (session != null && !session.getUsername().equals(player.getName())) {
            String playerName = player.getName();
            plugin.getLog().warn("Player username {} is not matching session {}", playerName, session.getUsername());
            return;
        }

        super.run();

        PremiumStatus status = PremiumStatus.CRACKED;
        if (isOnlineMode()) {
            status = PremiumStatus.PREMIUM;
        }

        plugin.getPremiumPlayers().put(player.getUniqueId(), status);
    }

    @Override
    public FastLoginAutoLoginEvent callFastLoginAutoLoginEvent(LoginSession session, StoredProfile profile) {
        BukkitFastLoginAutoLoginEvent event = new BukkitFastLoginAutoLoginEvent(session, profile);
        core.getPlugin().getServer().getPluginManager().callEvent(event);
        return event;
    }

    @Override
    public void onForceActionSuccess(LoginSession session) {
        if (core.getPlugin().getProxyManager().isEnabled()) {
            core.getPlugin().getProxyManager().sendPluginMessage(player, new SuccessMessage());
        }
    }

    @Override
    public String getName(Player player) {
        return player.getName();
    }

    @Override
    public boolean isOnline(Player player) {
        try {
            //the player-list isn't thread-safe
            return Bukkit.getScheduler().callSyncMethod(core.getPlugin(), player::isOnline).get();
        } catch (InterruptedException | ExecutionException ex) {
            core.getPlugin().getLog().error("Failed to perform thread-safe online check for {}", player, ex);
            return false;
        }
    }

    @Override
    public boolean isOnlineMode() {
        return session.isVerified();
    }
}
