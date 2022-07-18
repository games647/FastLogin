/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.github.games647.fastlogin.bukkit.listener.protocollib.SignatureTestData.SignatureData;
import com.github.games647.fastlogin.bukkit.listener.protocollib.packet.ClientPublicKey;
import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.var;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    @Test
    void testVerifyToken() {
        var random = ThreadLocalRandom.current();
        byte[] token = EncryptionUtil.generateVerifyToken(random);

        assertAll(
                () -> assertNotNull(token),
                () -> assertEquals(token.length, 4)
        );
    }

    @Test
    void testServerKey() {
        KeyPair keyPair = EncryptionUtil.generateKeyPair();

        Key privateKey = keyPair.getPrivate();
        assertEquals(privateKey.getAlgorithm(), "RSA");

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        assertEquals(publicKey.getAlgorithm(), "RSA");

        // clients accept larger values than the standard vanilla server, but we shouldn't crash them
        assertAll(
                () -> assertTrue(publicKey.getModulus().bitLength() >= 1024),
                () -> assertTrue(publicKey.getModulus().bitLength() < 8192)
        );
    }

    @Test
    void testExpiredClientKey() throws Exception {
        var clientKey = ResourceLoader.loadClientKey("client_keys/valid_public_key.json");

        // Client expires at the exact second mentioned, so use it for verification
        var expiredTimestamp = clientKey.expiry();
        assertFalse(EncryptionUtil.verifyClientKey(clientKey, expiredTimestamp));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // expiration date changed should make the signature invalid while still being not expired
            "client_keys/invalid_wrong_expiration.json",
            // changed public key no longer corresponding to the signature
            "client_keys/invalid_wrong_key.json",
            // signature modified no longer corresponding to key and expiration date
            "client_keys/invalid_wrong_signature.json"
    })
    void testInvalidClientKey(String clientKeySource) throws Exception {
        var clientKey = ResourceLoader.loadClientKey(clientKeySource);
        Instant expireTimestamp = clientKey.expiry().minus(5, ChronoUnit.HOURS);

        assertFalse(EncryptionUtil.verifyClientKey(clientKey, expireTimestamp));
    }

    @Test
    void testValidClientKey() throws Exception {
        var clientKey = ResourceLoader.loadClientKey("client_keys/valid_public_key.json");
        var verificationTimestamp = clientKey.expiry().minus(5, ChronoUnit.HOURS);

        assertTrue(EncryptionUtil.verifyClientKey(clientKey, verificationTimestamp));
    }

    @Test
    void testDecryptSharedSecret() throws Exception {
        KeyPair serverPair = EncryptionUtil.generateKeyPair();
        var serverPK = serverPair.getPublic();

        SecretKey secretKey = generateSharedKey();
        byte[] encryptedSecret = encrypt(serverPK, secretKey.getEncoded());

        SecretKey decryptSharedKey = EncryptionUtil.decryptSharedKey(serverPair.getPrivate(), encryptedSecret);
        assertEquals(decryptSharedKey, secretKey);
    }

    private static byte[] encrypt(PublicKey receiverKey, byte... message)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException {
        var encryptCipher = Cipher.getInstance(receiverKey.getAlgorithm());
        encryptCipher.init(Cipher.ENCRYPT_MODE, receiverKey);
        return encryptCipher.doFinal(message);
    }

    private static SecretKeySpec generateSharedKey() {
        // according to wiki.vg 16 bytes long
        byte[] sharedKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(sharedKey);
        // shared key is to be used for the AES/CFB8 stream cipher to encrypt the traffic
        // therefore the encryption/decryption has to be AES
        return new SecretKeySpec(sharedKey, "AES");
    }

    @Test
    void testServerIdHash() throws Exception {
        var serverId = "";
        var sharedSecret = generateSharedKey();
        var serverPK = ResourceLoader.loadClientKey("client_keys/valid_public_key.json").key();

        String sessionHash = getServerHash(serverId, sharedSecret, serverPK);
        assertEquals(EncryptionUtil.getServerIdHashString(serverId, sharedSecret, serverPK), sessionHash);
    }

    private static String getServerHash(CharSequence serverId, SecretKey sharedSecret, PublicKey serverPK) {
        // https://wiki.vg/Protocol_Encryption#Client
        // sha1 := Sha1()
        // sha1.update(ASCII encoding of the server id string from Encryption Request)
        // sha1.update(shared secret)
        // sha1.update(server's encoded public key from Encryption Request)
        // hash := sha1.hexdigest() # String of hex characters
        @SuppressWarnings("deprecation")
        var hasher = Hashing.sha1().newHasher();
        hasher.putString(serverId, StandardCharsets.US_ASCII);
        hasher.putBytes(sharedSecret.getEncoded());
        hasher.putBytes(serverPK.getEncoded());
        //  It works by treating the sha1 output bytes as one large integer in two's complement and then printing the
        //  integer in base 16, placing a minus sign if the interpreted number is negative.
        // reference: 
        // https://github.com/SpigotMC/BungeeCord/blob/ff5727c5ef9c0b56ad35f9816ae6bd660b622cf0/proxy/src/main/java/net/md_5/bungee/connection/InitialHandler.java#L456
        return new BigInteger(hasher.hash().asBytes()).toString(16);
    }

    @Test
    void testServerIdHashWrongSecret() throws Exception {
        var serverId = "";
        var sharedSecret = generateSharedKey();
        var serverPK = ResourceLoader.loadClientKey("client_keys/valid_public_key.json").key();

        String sessionHash = getServerHash(serverId, sharedSecret, serverPK);
        assertNotEquals(EncryptionUtil.getServerIdHashString("", generateSharedKey(), serverPK), sessionHash);
    }

    @Test
    void testServerIdHashWrongServerKey() {
        var serverId = "";
        var sharedSecret = generateSharedKey();
        var serverPK = EncryptionUtil.generateKeyPair().getPublic();

        String sessionHash = getServerHash(serverId, sharedSecret, serverPK);
        var wrongPK = EncryptionUtil.generateKeyPair().getPublic();
        assertNotEquals(EncryptionUtil.getServerIdHashString("", sharedSecret, wrongPK), sessionHash);
    }

    @Test
    void testValidSignedNonce() throws Exception {
        ClientPublicKey clientKey = ResourceLoader.loadClientKey("client_keys/valid_public_key.json");
        SignatureTestData testData = SignatureTestData.fromResource("signature/valid_signature.json");
        assertTrue(verifySignedNonce(testData, clientKey));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "signature/incorrect_nonce.json",
            "signature/incorrect_salt.json",
            "signature/incorrect_signature.json",
    })
    void testIncorrectNonce(String signatureSource) throws Exception {
        ClientPublicKey clientKey = ResourceLoader.loadClientKey("client_keys/valid_public_key.json");
        SignatureTestData testData = SignatureTestData.fromResource(signatureSource);
        assertFalse(verifySignedNonce(testData, clientKey));
    }

    @Test
    void testWrongPublicKeySigned() throws Exception {
        // load a different public key
        ClientPublicKey clientKey = ResourceLoader.loadClientKey("client_keys/invalid_wrong_key.json");
        SignatureTestData testData = SignatureTestData.fromResource("signature/valid_signature.json");
        assertFalse(verifySignedNonce(testData, clientKey));
    }

    private static boolean verifySignedNonce(SignatureTestData testData, ClientPublicKey clientKey)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        PublicKey clientPublicKey = clientKey.key();

        byte[] nonce = testData.getNonce();
        SignatureData signature = testData.getSignature();
        long salt = signature.getSalt();
        return EncryptionUtil.verifySignedNonce(nonce, clientPublicKey, salt, signature.getSignature());
    }

    @Test
    void testNonce() throws Exception {
        byte[] expected = {1, 2, 3, 4};
        var serverKey = EncryptionUtil.generateKeyPair();
        var encryptedNonce = encrypt(serverKey.getPublic(), expected);

        assertTrue(EncryptionUtil.verifyNonce(expected, serverKey.getPrivate(), encryptedNonce));
    }

    @Test
    void testNonceIncorrect() throws Exception {
        byte[] expected = {1, 2, 3, 4};
        var serverKey = EncryptionUtil.generateKeyPair();

        // flipped first character
        var encryptedNonce = encrypt(serverKey.getPublic(), new byte[]{0, 2, 3, 4});
        assertFalse(EncryptionUtil.verifyNonce(expected, serverKey.getPrivate(), encryptedNonce));
    }

    @Test
    void testNonceFailedDecryption() throws Exception {
        byte[] expected = {1, 2, 3, 4};
        var serverKey = EncryptionUtil.generateKeyPair();
        // generate a new keypair that is different
        var encryptedNonce = encrypt(EncryptionUtil.generateKeyPair().getPublic(), expected);

        assertThrows(GeneralSecurityException.class,
                () -> EncryptionUtil.verifyNonce(expected, serverKey.getPrivate(), encryptedNonce)
        );
    }

    @Test
    void testNonceIncorrectEmpty() {
        byte[] expected = {1, 2, 3, 4};
        var serverKey = EncryptionUtil.generateKeyPair();
        byte[] encryptedNonce = {};

        assertThrows(GeneralSecurityException.class,
                () -> EncryptionUtil.verifyNonce(expected, serverKey.getPrivate(), encryptedNonce)
        );
    }
}
