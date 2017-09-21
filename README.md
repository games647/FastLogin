# FastLogin

Checks if a Minecraft player has a paid account (premium). If so, they can skip offline authentication (auth plugins).
So they don't need to enter passwords. This is also called auto login (auto-login).

### Features:

* Detect paid accounts from others
* Automatically login paid accounts (premium)
* Support various of auth plugins
* Cauldron support
* Forge/Sponge message support
* Premium UUID support
* Forwards Skins
* Detect user name changed and will update the existing database record
* BungeeCord support
* Auto register new premium players
* Plugin: ProtocolSupport is supported and can be used as an alternative to ProtocolLib
* No client modifications needed
* Good performance by using async non blocking operations
* Locale messages
* Import the database from similar plugins
* Free
* Open source

***

### Commands:
    * /premium [player] Label the invoker or the argument as paid account
    * /cracked [player] Label the invoker or the argument as cracked account

### Permissions:
    * fastlogin.bukkit.command.premium
    * fastlogin.bukkit.command.cracked
    * fastlogin.command.premium.other
    * fastlogin.command.cracked.other
    * fastlogin.command.import

### Requirements:
* Plugin: [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) or [ProtocolSupport](https://www.spigotmc.org/resources/protocolsupport.7201/)
* Tested [Spigot](https://www.spigotmc.org) 1.8+ (could also work with other versions)
* Java 8+
* Run Spigot and/or BungeeCord/Waterfall in offline mode (see server.properties or config.yml)
* An auth plugin. Supported plugins

#### Bukkit/Spigot/Paper

* [AuthMe (5.X)](https://dev.bukkit.org/bukkit-plugins/authme-reloaded/)
* [xAuth](https://dev.bukkit.org/bukkit-plugins/xauth/)
* [LogIt](https://github.com/games647/LogIt)
* [AdvancedLogin (Paid)](https://www.spigotmc.org/resources/advancedlogin.10510/)
* [CrazyLogin](https://dev.bukkit.org/bukkit-plugins/crazylogin/)
* [LoginSecurity](https://dev.bukkit.org/bukkit-plugins/loginsecurity/)
* [UltraAuth](https://dev.bukkit.org/bukkit-plugins/ultraauth-aa/)

#### BungeeCord/Waterfall

* [BungeeAuth](https://www.spigotmc.org/resources/bungeeauth.493/)

***

### How to install

#### Bukkit/Spigot/Paper

1. Download and install ProtocolLib
2. Download and install FastLogin
3. Set your server in offline mode by setting the value onlinemode in your server.properties to false

#### BungeeCord/Waterfall

1. Activate BungeeCord in the Spigot configuration
2. Restart your server
3. Now there is proxy-whitelist file in the FastLogin folder
Put your stats id from the BungeeCord config into this file
4. Activate ipForward in your BungeeCord config
5. Download and Install FastLogin on BungeeCord AND Spigot
6. Check your database settings in the config of FastLogin on BungeeCord
7. Set your proxy (BungeeCord) in offline mode by setting the value onlinemode in your config.yml to false
8. You should *always* firewall your spigot server that it's only accessible through BungeeCord https://www.spigotmc.org/wiki/bungeecord-installation/#post-installation
9. (BungeeCord doesn't support SQLite per default, so you should change the configuration to MySQL or MariaDB)
