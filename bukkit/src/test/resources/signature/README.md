# About

This contains test resources for the unit tests. Files in this folder include pre-made cryptographic signatures.

## Directory structure

* `valid_signature.json`: Extracted using packet extract from an actual authentication
* `incorrect_nonce.json`: Different nonce token simulating that the server expected a different token than signed
* `incorrect_salt.json`: Salt sent is different to the content signed by the signature (changed salt field)
* `incorrect_signature.json`: Changed signature

## File content

* `nonce`: Server generated nonce token
* `salt`: Client generated random token that will be signed
* `signature`: Nonce and salt signed using the client key from `valid_public_key.json`
