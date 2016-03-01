# FastLogin

[![Build Status](https://travis-ci.org/games647/FastLogin.svg?branch=master)](https://travis-ci.org/games647/FastLogin)
[![Donate Button](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8ZBULMAPN7MZC)

Checks if a minecraft player has a paid account (premium). If so, they can skip offline authentication (auth plugins).
So they don't need to enter passwords. This is also called auto login (auto-login).

###Features:
* Detect paid accounts from others
* Automatically login paid accounts (premium)
* Support various of auth plugins
* Experimental Cauldron support
* BungeeCord support
* No client modifications needed
* Good performance by using async non blocking operations
* Free
* Open source

***

###Commands:
* /premium Label the invoker as paid account

###Permissions:
* fastlogin.bukkit.command.premium

###Requirements:
* Plugin: [ProtocolLib](http://www.spigotmc.org/resources/protocollib.1997/)
* Tested Bukkit/[Spigot](https://www.spigotmc.org) 1.8.8 (could also work with other versions)
* Java 7 or above
* Run in offline mode (see server.properties)
* An auth plugin. Supported Plugins:
    * [AuthMe](http://dev.bukkit.org/bukkit-plugins/authme-reloaded/)
    * [xAuth](http://dev.bukkit.org/bukkit-plugins/xauth/)
    * [CrazyLogin](http://dev.bukkit.org/bukkit-plugins/crazylogin/)
    * [LoginSecurity](http://dev.bukkit.org/bukkit-plugins/loginsecurity/)
    * [RoyalAuth](http://dev.bukkit.org/bukkit-plugins/royalauth/)
    * [UltraAuth](http://dev.bukkit.org/bukkit-plugins/ultraauth-aa/)

###Downloads

https://www.spigotmc.org/resources/fastlogin.14153/history

***

###FAQ

####Index
1. [How does minecraft logins work?](#how-does-minecraft-logins-work)
2. [How does this plugin work?](#how-does-this-plugin-work)
3. [Why does the plugin require offline mode?](#why-does-the-plugin-require-offline-mode)
4. [Can cracked player join with premium usernames?](#can-cracked-player-join-with-premium-usernames)
5. [Why do players have to invoke a command?](#why-do-players-have-to-invoke-a-command)
6. [What happens if a paid account joins with a used username?](#what-happens-if-a-paid-account-joins-with-a-used-username)
7. [Does the plugin have BungeeCord support?](#does-the-plugin-have-bungeecord-support)
8. [Could premium players have a premium UUID and Skin?](#could-premium-players-have-a-premium-uuid-and-skin)
9. [Is this plugin compatible with Cauldron?](#is-this-plugin-compatible-with-cauldron)

####How does minecraft logins work?
######Online Mode
1. Client -> Server: I want to login, here is my username
2. Server -> Client: Okay. I'm in online mode so here is my public key for encryption and my serverid
3. Client -> Mojang: I'm player "xyz". I want to join a server with that serverid
4. Mojang -> Client: Session data checked. You can continue
5. Client -> Server: I received a successful response from Mojang. Heres our shared secret key
6. Server -> Mojang: Does the player "xyz" with this shared secret key has a valid account to join me?
7. Client and Server: encrypt all following communication packet
8. Server -> Client: Everything checked you can play now


######Offline Mode
In offline mode step 2-7 is skipped. So a login request is directly followed by 8.

######More details
http://wiki.vg/Protocol#Login

####How does this plugin work?
By using ProtocolLib, this plugin works as a proxy between the client and server. This plugin will fake that the server
runs in online mode. It does everything an online mode server would do. This will be for example, generating keys or
checking for valid sessions. Because everything is the same compared to an offline mode login after an encrypted
connection, we will intercept only **login** packets of **premium** players.

1. Player is connecting to the server.
2. Plugin checks if the username we received activated the fast login method (i.e. using command)
3. Run a check if the username is currently used by a paid account.
(We don't know yet if the client connecting is premium)
4. Request an Mojang Session Server authentication
5. On response check if all data is correct
6. Encrypt the connection
7. On success intercept all related login packets and fake a new login packet as a normal offline login

####Why does the plugin require offline mode?
1. As you can see in the question "how does minecraft login works", offline mode is equivalent to online mode except of
the encryption and session checks on login. So we can intercept and cancel the first packets for premium players and
enable an encrypted connection. Then we send a new fake packet in order to pretend that this a new login request from
a offline mode player. The server will handle the rest.
2. Some plugins check if the server is in online mode. If so, they could process the real offline (cracked) accounts
incorrectly. For example, a plugin tries to fetch the UUID from Mojang, but the name of the player is not associated to
a paid account.
3. Servers, who allow cracked players and just speed up logins for premium players, are **already** in offline mode.

####Can cracked player join with premium usernames?
Yes, indeed. Therefore the command for toggling the fast login method exists.

####Why do players have to invoke a command?
1. It's a secure way to make sure a person with a paid account cannot steal the account
of a cracked player that has the same username. The player have to proof first that it's his own account.
2. We only receive the username from the player on login. We could check if that username is associated
to a paid account but if we request a online mode login from a cracked player (who uses a username from
a paid account), the player will disconnect with the reason bad login. There is no way to change that message
on the server side (without client modifications), because it's a connection between the Client and the Sessionserver.
3. If a premium player would skip registration too, a player of a cracked account could later still register the
account and would claim and steal the account from the premium player. Because commands cannot be invoked unless the
player has a account or is logged in, protects this method also premium players

###What happens if a paid account joins with a used username?
The player on the server have to activate the feature of this plugin by command. If a person buys the username
of his own account, it's still secured. A normal offline mode login makes sure he's the owner of the server account
and Mojang account. Then the command can be executed. So someone different cannot steal the account of cracked player
by buying the username.

####Does the plugin have BungeeCord support?
Yes it has. Just activate ipForward in your BungeeCord config and place the plugin in the plugins folder of
Bukkit/Spigot and BungeeCord. Then you have fill your BungeeCord Id (from the Stats-Option in the BungeeCord config)
into the whitelist file of your Bukkit/Spigot server. For security reasons, don't post this Id on Forums.

This plugin will automatically detect if BungeeCord is running and handle premium checks on BungeeCord.

####Could premium players have a premium UUID and Skin?
Something like that is possible, but is not yet implemented.

####Is this plugin compatible with Cauldron?
It's not tested yet, but all needed methods also exists in Cauldron so it could work together.

***

###Useful Links:
* [Login Protocol](http://wiki.vg/Protocol#Login)
* [Protocol Encryption](http://wiki.vg/Protocol_Encryption)