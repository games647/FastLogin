######1.2

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