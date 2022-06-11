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

import com.github.games647.fastlogin.bukkit.listener.protocollib.packet.ClientPublicKey;
import com.google.common.io.Resources;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryption and decryption minecraft util for connection between servers
 * and paid Minecraft account clients.
 *
 * @see net.minecraft.server.MinecraftEncryption
 */
class EncryptionUtil {

    public static final int VERIFY_TOKEN_LENGTH = 4;
    public static final String KEY_PAIR_ALGORITHM = "RSA";

    private static final int RSA_LENGTH = 1_024;

    private static final PublicKey mojangSessionKey;
    private static final int LINE_LENGTH = 76;
    private static final Encoder KEY_ENCODER = Base64.getMimeEncoder(LINE_LENGTH, "\n".getBytes(StandardCharsets.UTF_8));

    static {
        try {
            mojangSessionKey = loadMojangSessionKey();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new RuntimeException("Failed to load Mojang session key", ex);
        }
    }

    private EncryptionUtil() {
        // utility
    }

    /**
     * Generate an RSA key pair
     *
     * @return The RSA key pair.
     */
    public static KeyPair generateKeyPair() {
        // KeyPair b()
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_PAIR_ALGORITHM);

            keyPairGenerator.initialize(RSA_LENGTH);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            // Should be existing in every vm
            throw new ExceptionInInitializerError(nosuchalgorithmexception);
        }
    }

    /**
     * Generate a random token. This is used to verify that we are communicating with the same player
     * in a login session.
     *
     * @param random random generator
     * @return an error with 4 bytes long
     */
    public static byte[] generateVerifyToken(Random random) {
        // extracted from LoginListener
        byte[] token = new byte[VERIFY_TOKEN_LENGTH];
        random.nextBytes(token);
        return token;
    }

    /**
     * Generate the server id based on client and server data.
     *
     * @param sessionId session for the current login attempt
     * @param sharedSecret shared secret between the client and the server
     * @param publicKey public key of the server
     * @return the server id formatted as a hexadecimal string.
     */
    public static String getServerIdHashString(String sessionId, SecretKey sharedSecret, PublicKey publicKey) {
        // found in LoginListener
        try {
            byte[] serverHash = getServerIdHash(sessionId, publicKey, sharedSecret);
            return (new BigInteger(serverHash)).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Decrypts the content and extracts the key spec.
     *
     * @param privateKey private server key
     * @param sharedKey the encrypted shared key
     * @return shared secret key
     * @throws GeneralSecurityException if it fails to decrypt the data
     */
    public static SecretKey decryptSharedKey(PrivateKey privateKey, byte[] sharedKey) throws GeneralSecurityException {
        // SecretKey a(PrivateKey var0, byte[] var1)
        return new SecretKeySpec(decrypt(privateKey, sharedKey), "AES");
    }

    public static boolean verifyClientKey(ClientPublicKey clientKey, Instant verifyTimstamp)
        throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        if (!verifyTimstamp.isBefore(clientKey.getExpiry())) {
            return false;
        }

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(mojangSessionKey);
        signature.update(toSignable(clientKey).getBytes(StandardCharsets.US_ASCII));
        return signature.verify(clientKey.getSignature());
    }

    private static PublicKey loadMojangSessionKey()
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        var keyUrl = Resources.getResource("yggdrasil_session_pubkey.der");
        var keyData = Resources.toByteArray(keyUrl);
        var keySpec = new X509EncodedKeySpec(keyData);

        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private static String toSignable(ClientPublicKey clientPublicKey) {
        long expiry = clientPublicKey.getExpiry().toEpochMilli();
        String encoded = KEY_ENCODER.encodeToString(clientPublicKey.getKey());
        return expiry + "-----BEGIN RSA PUBLIC KEY-----\n" + encoded + "\n-----END RSA PUBLIC KEY-----\n";
    }

    public static byte[] decrypt(PrivateKey key, byte[] data) throws GeneralSecurityException {
        // b(Key var0, byte[] var1)
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key);
        return decrypt(cipher, data);
    }

    /**
     * Decrypted the given data using the cipher.
     *
     * @param cipher decryption cypher initialized with the private key
     * @param data the encrypted data
     * @return clear text data
     * @throws GeneralSecurityException if it fails to decrypt the data
     */
    private static byte[] decrypt(Cipher cipher, byte[] data) throws GeneralSecurityException {
        // inlined: byte[] a(int var0, Key var1, byte[] var2), Cipher a(int var0, String var1, Key
        // var2)
        return cipher.doFinal(data);
    }

    private static byte[] getServerIdHash(
            String sessionId, PublicKey publicKey, SecretKey sharedSecret)
            throws NoSuchAlgorithmException {
        // byte[] a(String var0, PublicKey var1, SecretKey var2)
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        // inlined from byte[] a(String var0, byte[]... var1)
        digest.update(sessionId.getBytes(StandardCharsets.ISO_8859_1));
        digest.update(sharedSecret.getEncoded());
        digest.update(publicKey.getEncoded());

        return digest.digest();
    }
}
