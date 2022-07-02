# About

This contains test resources for the unit tests. The files are extracted from the Minecraft directory with slight
modifications. The files are found in `$MINECRAFT_HOME$/profilekeys/`, where `$MINECRAFT_HOME$` represents the
OS-dependent minecraft folder.

**Notable the files in this folder do not contain the private key information. It should be explicitly
stripped before including it.**

## Minecraft folder

* Windows: `%appdata%\.minecraft`
* Linux: `/home/username/.minecraft`
* Mac: `~/Library/Application Support/minecraft`

## Directory structure

* `valid_public_key.json`: Extracted from the actual file
* `invalid_wrong_expiration.json`: Changed the expiration date
* `invalid_wrong_key.json`: Modified public key while keeping the RSA structure valid
* `invalid_wrong_signature.json`: Changed a character in the public key signature

## File content

* `expires_at`: Expiration date
* `key`: Public key from the original file out of `public_key.key`
* `signature`: Mojang signed signature of this public key
