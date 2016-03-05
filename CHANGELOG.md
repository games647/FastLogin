######0.6

* Added /premium [player] command with optional player parameter
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