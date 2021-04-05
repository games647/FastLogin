# FastLogin

Checks if a Minecraft player has a paid account (premium). If so, they can skip offline authentication (auth plugins).
So they don't need to enter passwords. This is also called auto login (auto-login).

## Features

* Detect paid accounts from others
* Automatically login paid accounts (premium)
* Support various of auth plugins
* Cauldron support
* Forge/Sponge message support
* Premium UUID support
* Forward skins
* Detect user name changed and will update the existing database record
* BungeeCord support
* Auto register new premium players
* Plugin: ProtocolSupport is supported and can be used as an alternative to ProtocolLib
* No client modifications needed
* Good performance by using async operations
* Locale messages
* Support for Bedrock players proxied through FloodGate

## Issues

Please use issues for bug reports, suggestions, questions and more. Please check for existing issues. Existing issues 
can be voted up by adding up vote to the original post. Closing issues means that they are marked as resolved. Comments
are still allowed and it could be re-opened.

## Development builds

Development builds of this project can be acquired at the provided CI (continuous integration) server. It contains the
latest changes from the Source-Code in preparation for the following release. This means they could contain new
features, bug fixes and other changes since the last release.

They **could** contain new bugs and are likely to be less stable than released versions.

Specific builds can be grabbed by clicking on the build number on the left side or by clicking on status to retrieve the
latest build.
https://ci.codemc.org/job/Games647/job/FastLogin/changes

***

## Commands

    /premium [player] Label the invoker or the argument as paid account
    /cracked [player] Label the invoker or the argument as cracked account

## Permissions

    fastlogin.bukkit.command.premium
    fastlogin.bukkit.command.cracked
    fastlogin.command.premium.other
    fastlogin.command.cracked.other

## Placeholder

This plugin supports `PlaceholderAPI` on `Spigot`. It exports the following variable 
`%fastlogin_status%`. In BungeeCord environments, the status of a player will be delivered with a delay after the player 
already successful joined the server. This takes about a couple of milliseconds. In this case the value
will be `Unknown`. 

Possible values: `Premium`, `Cracked`, `Unknown` 

## Requirements

* Plugin: 
    * [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) or 
    * [ProtocolSupport](https://www.spigotmc.org/resources/protocolsupport.7201/)
* [Spigot](https://www.spigotmc.org) 1.8.8+
* Java 8+
* Run Spigot (or a fork e.g. Paper) and/or BungeeCord (or a fork e.g. Waterfall) in offline mode
* An auth plugin.

### Supported auth plugins

#### Spigot/Paper

* [AdvancedLogin (Paid)](https://www.spigotmc.org/resources/advancedlogin.10510/)
* [AuthMe (5.X)](https://dev.bukkit.org/bukkit-plugins/authme-reloaded/)
* [CrazyLogin](https://dev.bukkit.org/bukkit-plugins/crazylogin/)
* [LoginSecurity](https://dev.bukkit.org/bukkit-plugins/loginsecurity/)
* [LogIt](https://github.com/games647/LogIt)
* [SodionAuth (2.0+)](https://github.com/Mohist-Community/SodionAuth)
* [UltraAuth](https://dev.bukkit.org/bukkit-plugins/ultraauth-aa/)
* [UserLogin](https://www.spigotmc.org/resources/userlogin.80669/)
* [xAuth](https://dev.bukkit.org/bukkit-plugins/xauth/)

#### BungeeCord/Waterfall

* [BungeeAuth](https://www.spigotmc.org/resources/bungeeauth.493/)
* [BungeeAuthenticator](https://www.spigotmc.org/resources/bungeecordauthenticator.87669/)
* [SodionAuth (2.0+)](https://github.com/Mohist-Community/SodionAuth)

## Network requests

This plugin performs network requests to:

* https://api.mojang.com - retrieving uuid data to decide if we should activate premium login
* https://sessionserver.mojang.com - verify if the player is the owner of that account

***

## How to install

### Spigot/Paper

1. Download and install ProtocolLib/ProtocolSupport
2. Download and install FastLogin (or FastLoginBukkit for newer versions)
3. Set your server in offline mode by setting the value onlinemode in your server.properties to false

### BungeeCord/Waterfall

1. Activate BungeeCord in the Spigot configuration
2. Restart your server
3. Now there is `allowed-proxies.txt` file in the FastLogin folder
Put your stats id from the BungeeCord config into this file
4. Activate ipForward in your BungeeCord config
5. Download and Install FastLogin (or FastLoginBungee in newer versions) on BungeeCord AND Spigot
(on the servers where your login plugin is or where player should be able to execute the commands of FastLogin)
6. Check your database settings in the config of FastLogin on BungeeCord
7. Set proxy and Spigot in offline mode by setting the value onlinemode in your config.yml to false
8. You should *always* firewall your Spigot server that it's only accessible through BungeeCord 
    * https://www.spigotmc.org/wiki/bungeecord-installation/#post-installation
    * BungeeCord doesn't support SQLite per default, so you should change the configuration to MySQL or MariaDB
