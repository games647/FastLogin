# Integration tests for authentication

## Description

Projects require integration tests in order to check against errors that could only occur if connected to other
components. However, they are heavier in terms of performance and require a more complex setup. Unit tests often make
use of fake, mock, stubs, etc. implementations to test the unit in isolation and thus could hide issues across
boundaries of a unit. Nevertheless, both are not replacement for each other.

## Usage in this project

The authentication system is a core component, so it requires some kind of testing. Here we are going to
spin up a Spigot server and test with the supported authentication schemes against the implementation of MCProtocolLib.

### Goals

* OS platform independent
* Reproducible, but not fixed to a specific image hash
    * This is a dev container, so fixing it to feature/major version is enough instead of a version fixed by hash
* Improve container spin up
    * E.g. Remove/Reduce world generation

### Note on automation

The simplest solution it to use the official Mojang session and authentication servers. However, this would require
a spare Minecraft account. Mocking the auth servers would be a solution to avoid this.

## Related

Interest blog article about integration tests and why they are necessary.
https://software.rajivprab.com/2019/04/28/rethinking-software-testing-perspectives-from-the-world-of-hardware/

## Issues

### Slow startup

Tried a lot of optimizations like only loading a single world without the nether or the end. However, there the startup
is still slow. If you have any ideas on how to tune the startup parameters of the Minecraft server or the JVM
itself to reduce the startup time, please suggest it.

### Checkpoint

An idea to optimize the time is to use CRIU (checkpoint and restore). So to save the process at a certain stage and
restore all data multiple times. This could cause a lot of issues like open files have to be present. However, the
impact is significant and since it runs inside the container all files, pids (pid=1) should be matching. Potential
checkpoint locations are:

* Direct before loading the plugins
  * Likely before binding the port to prevent issues
* After loading the libraries

Nevertheless, the current state requires to run it with root and the Java support is currently still in progress.

