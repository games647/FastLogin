package com.github.games647.fastlogin.bukkit.commands;

import com.github.games647.fastlogin.bukkit.BukkitCore;
import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.importer.ImportPlugin;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ImportCommand implements CommandExecutor {

    private final BukkitCore core;

    public ImportCommand(BukkitCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.DARK_RED + "You need to specify the import plugin and database type");
            return true;
        }

        ImportPlugin importPlugin;
        switch (args[0].toLowerCase()) {
            case "autoin":
                importPlugin = ImportPlugin.AUTO_IN;
                break;
            case "bpa":
                importPlugin = ImportPlugin.BPA;
                break;
            case "eldzi":
                importPlugin = ImportPlugin.ELDZI;
                break;
            default:
                sender.sendMessage(ChatColor.DARK_RED + "Unknown auto login plugin");
                return true;
        }

        boolean sqlite;
        switch (args[1].toLowerCase()) {
            case "sqlite":
                sqlite = true;
                break;
            case "mysql":
                sqlite = false;
                break;
            default:
                sender.sendMessage(ChatColor.DARK_RED + "Unknown storage type to import from. Either SQLite or MySQL");
                return true;
        }

        String host = "";
        String database = "";
        String username = "";
        String password = "";
        if (!sqlite) {
            if (args.length <= 5) {
                sender.sendMessage(ChatColor.DARK_RED + "If importing from MySQL, you need to specify host database "
                        + "and username passowrd too");
                return true;
            }

            host = args[2];
            database = args[3];
            username = args[4];
            password = args[5];
        }

        AuthStorage storage = core.getStorage();
        boolean success = core.importDatabase(importPlugin, true, storage, host, database, username, password);
        if (success) {
            sender.sendMessage(ChatColor.DARK_GREEN + "Successful imported the data");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Failed to import the data. Check out the logs");
        }

        return true;
    }
}
