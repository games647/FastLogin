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