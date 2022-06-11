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
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class EncryptionUtilTest {

    @Test
    public void testVerifyToken() {
        SecureRandom random = new SecureRandom();
        byte[] token = EncryptionUtil.generateVerifyToken(random);

        assertThat(token, notNullValue());
        assertThat(token.length, is(4));
    }

    @Test
    public void testExpiredClientKey() throws Exception {
        var clientKey = loadClientKey("client_keys/valid.json");

        // Client expires at the exact second mentioned, so use it for verification
        var expiredTimestamp = clientKey.getExpiry();
        assertThat(EncryptionUtil.verifyClientKey(clientKey, expiredTimestamp), is(false));
    }

    // @Test(expected = Exception.class)
    @Test
    public void testInvalidChangedExpiration() throws Exception {
        // expiration date changed should make the signature invalid
        // expiration should still be valid
        var clientKey = loadClientKey("client_keys/invalid_wrong_expiration.json");
        assertThat(EncryptionUtil.verifyClientKey(clientKey, clientKey.getExpiry().minus(5, ChronoUnit.HOURS)), is(false));
    }

    // @Test(expected = Exception.class)
    @Test
    public void testInvalidChangedKey() throws Exception {
        // changed public key no longer corresponding to the signature
        var clientKey = loadClientKey("client_keys/invalid_wrong_key.json");
        assertThat(EncryptionUtil.verifyClientKey(clientKey, clientKey.getExpiry().minus(5, ChronoUnit.HOURS)), is(false));
    }

    @Test
    public void testInvalidChangedSignature() throws Exception {
        // signature modified no longer corresponding to key and expiration date
        var clientKey = loadClientKey("client_keys/invalid_wrong_signature.json");
        assertThat(EncryptionUtil.verifyClientKey(clientKey, clientKey.getExpiry().minus(5, ChronoUnit.HOURS)), is(false));
    }

    @Test
    public void testValidClientKey() throws Exception {
        var clientKey = loadClientKey("client_keys/valid.json");

        var verificationTimestamp = clientKey.getExpiry().minus(5, ChronoUnit.HOURS);
        assertThat(EncryptionUtil.verifyClientKey(clientKey, verificationTimestamp), is(true));
    }

    private ClientPublicKey loadClientKey(String path)
        throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        var keyUrl = Resources.getResource(path);
        var gson = new Gson();

        var lines = Resources.toString(keyUrl, StandardCharsets.US_ASCII);
        var object = gson.fromJson(lines, JsonObject.class);

        Instant expires = Instant.parse(object.getAsJsonPrimitive("expires_at").getAsString());
        String key = object.getAsJsonPrimitive("key").getAsString();
        RSAPublicKey publicKey = parsePublicKey(key);

        byte[] signature = Base64.getDecoder().decode(object.getAsJsonPrimitive("signature").getAsString());
        return new ClientPublicKey(expires, publicKey.getEncoded(), signature);
    }

    private RSAPublicKey parsePublicKey(String lines)
        throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        try (
            Reader reader = new StringReader(lines);
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
