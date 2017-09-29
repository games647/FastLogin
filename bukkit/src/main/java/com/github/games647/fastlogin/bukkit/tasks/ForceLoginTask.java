package com.github.games647.fastlogin.bukkit.tasks;

import com.github.games647.fastlogin.bukkit.BukkitLoginSession;
import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.core.shared.FastLoginCore;
import com.github.games647.fastlogin.core.shared.ForceLoginManagement;
import com.github.games647.fastlogin.core.shared.LoginSession;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class ForceLoginTask extends ForceLoginManagement<Player, CommandSender, BukkitLoginSession, FastLoginBukkit> {

    public ForceLoginTask(FastLoginCore<Player, CommandSender, FastLoginBukkit> core, Player player) {
        super(core, player);
    }

    @Override
    public void run() {
        //remove the bungeecord identifier if there is ones
        String id = '/' + player.getAddress().getAddress().getHostAddress() + ':' + player.getAddress().getPort();
        session = core.getPlugin().getLoginSessions().remove(id);

        //blacklist this target player for BungeeCord Id brute force attacks
        FastLoginBukkit plugin = core.getPlugin();
        player.setMetadata(core.getPlugin().getName(), new FixedMetadataValue(plugin, true));

        super.run();
    }

    @Override
    public void onForceActionSuccess(LoginSession session) {
        if (core.getPlugin().isBungeeCord()) {
            ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
            dataOutput.writeUTF("SUCCESS");

            player.sendPluginMessage(core.getPlugin(), core.getPlugin().getName(), dataOutput.toByteArray());
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
            core.getPlugin().getLog().error("Failed to perform thread-safe online check", ex);
            return false;
        }
    }

    @Override
    public boolean isOnlineMode() {
        return session.isVerified() && player.getName().equals(session.getUsername());
    }
}
