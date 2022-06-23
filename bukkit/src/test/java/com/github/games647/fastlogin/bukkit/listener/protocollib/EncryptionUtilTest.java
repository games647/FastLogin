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
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertTrue;

public class EncryptionUtilTest {

    @Test
    public void testVerifyToken() {
        var random = ThreadLocalRandom.current();
        byte[] token = EncryptionUtil.generateVerifyToken(random);

        assertThat(token, notNullValue());
        assertThat(token.length, is(4));
    }

    @Test
    public void testServerKey() {
        KeyPair keyPair = EncryptionUtil.generateKeyPair();

        Key privateKey = keyPair.getPrivate();
        assertThat(privateKey.getAlgorithm(), is("RSA"));

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        assertThat(publicKey.getAlgorithm(), is("RSA"));

        // clients accept larger values than the standard vanilla server, but we shouldn't crash them
        assertTrue(publicKey.getModulus().bitLength() >= 1024);
        assertTrue(publicKey.getModulus().bitLength() < 8192);
    }

    @Test
    public void testExpiredClientKey() throws Exception {
        var clientKey = loadClientKey("client_keys/valid_public_key.json");

        // Client expires at the exact second mentioned, so use it for verification
        var expiredTimestamp = clientKey.expiry();
        assertThat(EncryptionUtil.verifyClientKey(clientKey, expiredTimestamp), is(false));
    }

    @Test
    public void testInvalidChangedExpiration() throws Exception {
        // expiration date changed should make the signature invalid
        // expiration should still be valid
        var clientKey = loadClientKey("client_keys/invalid_wrong_expiration.json");
        Instant expireTimestamp = clientKey.expiry().minus(5, ChronoUnit.HOURS);

        assertThat(EncryptionUtil.verifyClientKey(clientKey, expireTimestamp), is(false));
    }

    @Test
    public void testInvalidChangedKey() throws Exception {
        // changed public key no longer corresponding to the signature
        var clientKey = loadClientKey("client_keys/invalid_wrong_key.json");
        Instant expireTimestamp = clientKey.expiry().minus(5, ChronoUnit.HOURS);

        assertThat(EncryptionUtil.verifyClientKey(clientKey, expireTimestamp), is(false));
    }

    @Test
    public void testInvalidChangedSignature() throws Exception {
        // signature modified no longer corresponding to key and expiration date
        var clientKey = loadClientKey("client_keys/invalid_wrong_signature.json");
        Instant expireTimestamp = clientKey.expiry().minus(5, ChronoUnit.HOURS);

        assertThat(EncryptionUtil.verifyClientKey(clientKey, expireTimestamp), is(false));
    }

    @Test
    public void testValidClientKey() throws Exception {
        var clientKey = loadClientKey("client_keys/valid_public_key.json");
        var verificationTimestamp = clientKey.expiry().minus(5, ChronoUnit.HOURS);

        assertThat(EncryptionUtil.verifyClientKey(clientKey, verificationTimestamp), is(true));
    }

    @Test
    public void testDecryptSharedSecret() throws Exception {
        KeyPair serverPair = EncryptionUtil.generateKeyPair();
        var serverPK = serverPair.getPublic();

        SecretKey secretKey = generateSharedKey();
        byte[] encryptedSecret = encrypt(serverPK, secretKey.getEncoded());

        SecretKey decryptSharedKey = EncryptionUtil.decryptSharedKey(serverPair.getPrivate(), encryptedSecret);
        assertThat(decryptSharedKey, is(secretKey));
    }

    private byte[] encrypt(PublicKey receiverKey, byte[] message)
        throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
        IllegalBlockSizeException, BadPaddingException {
        var encryptCipher = Cipher.getInstance(receiverKey.getAlgorithm());
        encryptCipher.init(Cipher.ENCRYPT_MODE, receiverKey);
        return encryptCipher.doFinal(message);
    }

    private SecretKeySpec generateSharedKey() {
        // according to wiki.vg 16 bytes long
        byte[] sharedKey = new byte[16];
        ThreadLocalRandom.current().nextBytes(sharedKey);
        // shared key is to be used for the AES/CFB8 stream cipher to encrypt the traffic
        // therefore the encryption/decryption has to be AES
        return new SecretKeySpec(sharedKey, "AES");
    }

    @Test
    public void testServerIdHash() throws Exception {
        var serverId = "";
        var sharedSecret = generateSharedKey();
        var serverPK = loadClientKey("client_keys/valid_public_key.json").key();

        String sessionHash = getServerHash(serverId, sharedSecret, serverPK);
        assertThat(EncryptionUtil.getServerIdHashString(serverId, sharedSecret, serverPK), is(sessionHash));
    }

    @NotNull
    private String getServerHash(String serverId, SecretKey sharedSecret, PublicKey serverPK) {
        // https://wiki.vg/Protocol_Encryption#Client
        // sha1 := Sha1()
        // sha1.update(ASCII encoding of the server id string from Encryption Request)
        // sha1.update(shared secret)
        // sha1.update(server's encoded public key from Encryption Request)
        // hash := sha1.hexdigest() # String of hex characters
        var hasher = Hashing.sha1().newHasher();
        hasher.putString(serverId, StandardCharsets.US_ASCII);
        hasher.putBytes(sharedSecret.getEncoded());
        hasher.putBytes(serverPK.getEncoded());
        //  It works by treating the sha1 output bytes as one large integer in two's complement and then printing the
        //  integer in base 16, placing a minus sign if the interpreted number is negative.
        // reference: https://github.com/SpigotMC/BungeeCord/blob/ff5727c5ef9c0b56ad35f9816ae6bd660b622cf0/proxy/src/main/java/net/md_5/bungee/connection/InitialHandler.java#L456
        return new BigInteger(hasher.hash().asBytes()).toString(16);
    }

    @Test
    public void testServerIdHashWrongSecret() throws Exception {
        var serverId = "";
        var sharedSecret = generateSharedKey();
        var serverPK = loadClientKey("client_keys/valid_public_key.json").key();

        String sessionHash = getServerHash(serverId, sharedSecret, serverPK);
        assertThat(EncryptionUtil.getServerIdHashString("", generateSharedKey(), serverPK), not(sessionHash));
    }

    @Test
    public void testServerIdHashWrongServerKey() throws Exception {
        var serverId = "";
        var sharedSecret = generateSharedKey();
        var serverPK = EncryptionUtil.generateKeyPair().getPublic();

        String sessionHash = getServerHash(serverId, sharedSecret, serverPK);
        var wrongPK = EncryptionUtil.generateKeyPair().getPublic();
        assertThat(EncryptionUtil.getServerIdHashString("", sharedSecret, wrongPK), not(sessionHash));
    }

    @Test
    public void testValidSignedNonce() throws Exception {
        ClientPublicKey clientKey = loadClientKey("client_keys/valid_public_key.json");
        PublicKey clientPublicKey = clientKey.key();

        SignatureTestData testData = loadSignatureResource("signature/valid_signature.json");
        byte[] nonce = testData.getNonce();
        SignatureData signature = testData.getSignature();
        long salt = signature.getSalt();
        assertThat(EncryptionUtil.verifySignedNonce(nonce, clientPublicKey, salt, signature.getSignature()), is(true));
    }

    @Test
    public void testIncorrectNonce() throws Exception {
        ClientPublicKey clientKey = loadClientKey("client_keys/valid_public_key.json");
        PublicKey clientPublicKey = clientKey.key();

        SignatureTestData testData = loadSignatureResource("signature/incorrect_nonce.json");
        byte[] nonce = testData.getNonce();
        SignatureData signature = testData.getSignature();
        long salt = signature.getSalt();
        assertThat(EncryptionUtil.verifySignedNonce(nonce, clientPublicKey, salt, signature.getSignature()), is(false));
    }

    @Test
    public void testIncorrectSalt() throws Exception {
        // client generated
        ClientPublicKey clientKey = loadClientKey("client_keys/valid_public_key.json");
        PublicKey clientPublicKey = clientKey.key();

        SignatureTestData testData = loadSignatureResource("signature/incorrect_salt.json");
        byte[] nonce = testData.getNonce();
        SignatureData signature = testData.getSignature();
        long salt = signature.getSalt();
        assertThat(EncryptionUtil.verifySignedNonce(nonce, clientPublicKey, salt, signature.getSignature()), is(false));
    }

    @Test
    public void testIncorrectSignature() throws Exception {
        // client generated
        ClientPublicKey clientKey = loadClientKey("client_keys/valid_public_key.json");
        PublicKey clientPublicKey = clientKey.key();

        SignatureTestData testData = loadSignatureResource("signature/incorrect_signature.json");
        byte[] nonce = testData.getNonce();
        SignatureData signature = testData.getSignature();
        long salt = signature.getSalt();
        assertThat(EncryptionUtil.verifySignedNonce(nonce, clientPublicKey, salt, signature.getSignature()), is(false));
    }

    @Test
    public void testWrongPublicKeySigned() throws Exception {
        // load a different public key
        ClientPublicKey clientKey = loadClientKey("client_keys/invalid_wrong_key.json");
        PublicKey clientPublicKey = clientKey.key();

        SignatureTestData testData = loadSignatureResource("signature/valid_signature.json");
        byte[] nonce = testData.getNonce();
        SignatureData signature = testData.getSignature();
        long salt = signature.getSalt();
        assertThat(EncryptionUtil.verifySignedNonce(nonce, clientPublicKey, salt, signature.getSignature()), is(false));
    }

    private SignatureTestData loadSignatureResource(String resourceName) throws IOException {
        var keyUrl = Resources.getResource(resourceName);
        var encodedSignature = Resources.toString(keyUrl, StandardCharsets.US_ASCII);

        return new Gson().fromJson(encodedSignature, SignatureTestData.class);
    }

    private RSAPrivateKey parsePrivateKey(String keySpec)
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (
            Reader reader = new StringReader(keySpec);
            PemReader pemReader = new PemReader(reader)
        ) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            var privateKeySpec = new PKCS8EncodedKeySpec(content);

            var factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(privateKeySpec);
        }
    }

    private ClientPublicKey loadClientKey(String path)
        throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        var keyUrl = Resources.getResource(path);

        var lines = Resources.toString(keyUrl, StandardCharsets.US_ASCII);
        var object = new Gson().fromJson(lines, JsonObject.class);

        Instant expires = Instant.parse(object.getAsJsonPrimitive("expires_at").getAsString());
        String key = object.getAsJsonPrimitive("key").getAsString();
        RSAPublicKey publicKey = parsePublicKey(key);

        byte[] signature = Base64.getDecoder().decode(object.getAsJsonPrimitive("signature").getAsString());
        return new ClientPublicKey(expires, publicKey, signature);
    }

    private RSAPublicKey parsePublicKey(String keySpec)
        throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        try (
            Reader reader = new StringReader(keySpec);
            PemReader pemReader = new PemReader(reader)
        ) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            var pubKeySpec = new X509EncodedKeySpec(content);

            var factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(pubKeySpec);
        }
    }
}
