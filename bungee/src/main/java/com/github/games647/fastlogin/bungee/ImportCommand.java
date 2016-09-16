package com.github.games647.fastlogin.bungee;

import com.github.games647.fastlogin.core.AuthStorage;
import com.github.games647.fastlogin.core.importer.ImportPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class ImportCommand extends Command {

    private final FastLoginBungee plugin;

    public ImportCommand(FastLoginBungee plugin) {
        super("import-db", plugin.getDescription().getName().toLowerCase() + ".import");

        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            String message = ChatColor.DARK_RED + "You need to specify the import plugin and database type";
            sender.sendMessage(convertFromLegacy(message));
            return;
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
                String message = ChatColor.DARK_RED + "Unknown auto login plugin";
                sender.sendMessage(convertFromLegacy(message));
                return;
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
                String message = ChatColor.DARK_RED + "Unknown storage type to import from. Either SQLite or MySQL";
                sender.sendMessage(convertFromLegacy(message));
                return;
        }

        String host = "";
        String database = "";
        String username = "";
        String password = "";
        if (!sqlite) {
            if (args.length <= 5) {
                String message = ChatColor.DARK_RED + "If importing from MySQL, you need to specify host database "
                        + "and username passowrd too";
                sender.sendMessage(convertFromLegacy(message));
                return;
            }

            host = args[2];
            database = args[3];
            username = args[4];
            password = args[5];
        }

        BungeeCore core = plugin.getCore();
        AuthStorage storage = core.getStorage();
        boolean success = core.importDatabase(importPlugin, true, storage, host, database, username, password);
        if (success) {
            sender.sendMessage(convertFromLegacy(ChatColor.DARK_GREEN + "Successful imported the data"));
        } else {
            sender.sendMessage(convertFromLegacy(ChatColor.DARK_RED + "Failed to import the data. Check out the logs"));
        }
    }

    private BaseComponent[] convertFromLegacy(String message) {
        return TextComponent.fromLegacyText(message);
    }
}
