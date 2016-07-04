######1.7

* Added support for making requests to Mojang from different IPv4 addresses
* Added us.mcapi.com as third-party APIs to workaround rate-limits
* Fixed NPE in BungeeCord on cracked session
* Fixed skin applies if premium uuid is deactivated
* Fix player entry is not saved if namechangecheck is enabled

######1.6.2

* Fixed support for new LoginSecurity version

######1.6.1

* Fix message typo in BungeeCord which created a NPE if premium-warning is activated

######1.6

* Add a warning message if the user tries to invoke the premium command
* Added missing translation if the server isn't fully started
* Removed ProtocolLib as required dependency. You can use ProtocolSupport or BungeeCord as alternative
* Reduce the number of worker threads from 5 to 3 in ProtocolLib
* Process packets in ProtocolLib async/non-blocking -> better performance
* Fixed missing translation in commands
* Fixed cracked command not working on BungeeCord
* Fix error if forward skins is disabled

######1.5.2

* Fixed BungeeCord force logins if there is a lobby server
* Removed cache expire in BungeeCord
* Applies skin earlier to make it visible for other plugins listening on login events

######1.5.1

* Fixed BungeeCord support by correctly saving the proxy ids

######1.5

* Added localization
* Fixed NPE on premium name check if it's pure cracked player
* Fixed NPE in BungeeCord on cracked login for existing players
* Fixed saving of existing cracked players

######1.4

* Added Bungee setAuthPlugin method
* Added nameChangeCheck
* Multiple BungeeCord support

######1.3.1

* Prevent thread create violation in BungeeCord

######1.3

* Added support for AuthMe 3.X
* Fixed premium logins if the server is not fully started
* Added other command argument to /premium and /cracked
* Added support for LogIt
* Fixed 1.7 Minecraft support by removing guava 11+ only features -> Cauldron support
* Fixed BungeeCord support in Cauldron

######1.2.1

* Fix premium status change notification message on BungeeCord

######1.2

* Fix race condition in BungeeCord
* Fix dead lock in xAuth
* Added API methods for plugins to set their own password generator
* Added API methods for plugins to set their own auth plugin hook
=> Added support for AdvancedLogin

######1.1

* Make the configuration options also work under BungeeCord (premiumUUID, forwardSkin)
* Catch configuration loading exception if it's not spigot build
* Fix config loading for older Spigot builds

######1.0

* Massive refactor to handle errors on force actions safely
* force Methods now runs async too
* force methods now returns a boolean to reflect if the method was successful
* isRegistered method should now throw an exception if the plugin was unable to query the requested data

######0.8

* Fixed BungeeCord support for the Bukkit module
* Added database storage to save the premium state
* Fix logical error on /premium (Thanks to @NorbiPeti)
* Fixed issues with host lookup from hosts file (Thanks to @NorbiPeti)
* Remove handshake listener because it creates errors on some systems

######0.7

* Added BungeeAuth support
* Added /premium [player] command with optional player parameter
* Added a check if the player is already on the premium list
* Added a forwardSkin config option
* Added premium UUID support
* Updated to the newest changes of Spigot
* Removes the need of an Bukkit auth plugin if you use a bungeecord one
* Optimize performance and thread-safety
* Fixed BungeeCord support
* Changed config option autologin to autoregister to clarify the usage

######0.6

* Fixed 1.9 bugs
* Added UltraAuth support

######0.5

* Added unpremium command
* Added autologin - See config
* Added config
* Added isRegistered API method
* Added forceRegister API method

* Fixed CrazyLogin player data restore -> Fixes memory leaks with this plugin
* Fixed premium name check to protocolsupport
* Improved permissions management

######0.4

* Added forward premium skin
* Added plugin support for protocolsupport

######0.3.2

* Run packet readers in a different thread (separated from the Netty I/O Thread)
-> Improves performance
* Fixed Plugin disable if the server is in online mode but have to be in offline mode

######0.3.1

* Improved BungeeCord security

#####0.3

* Added BungeeCord support
* Decrease timeout checks in order to fail faster on connection problems
* Code style improvements

######0.2.4

* Fixed NPE on invalid sessions
* Improved security by generating a randomized serverId
* Removed /premium [player] because it's safer for premium players who join without registration

######0.2.3

* Remove useless AuthMe forcelogin code
* Send a kick message to the client instead of just "Disconnect"
* Reformat source code
* Fix thread safety for fake start packets (Bukkit.getOfflinePlayer doesn't look like to be thread-safe)
* Added more documentation

######0.2.2

* Compile project with Java 7 :(

######0.2.1
* A couple of security fixes (premium players cannot longer steal the account of a cracked account)
* Added a /premium command to mark you as premium player

#####0.2

* Added support for CrazyLogin and LoginSecurity
* Now minecraft version independent
* Added debug logging
* Code clean up
* More state validation
* Added better error handling

#####0.1
* First release
