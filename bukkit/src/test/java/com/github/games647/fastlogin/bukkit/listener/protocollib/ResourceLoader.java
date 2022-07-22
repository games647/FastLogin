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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class ResourceLoader {

    public static RSAPrivateKey parsePrivateKey(String keySpec)
        throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (
            Reader reader = new StringReader(keySpec);
            PemReader pemReader = new PemReader(reader)
        ) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(content);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(privateKeySpec);
        }
    }

    protected static ClientPublicKey loadClientKey(String path)
        throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        URL keyUrl = Resources.getResource(path);

        String lines = Resources.toString(keyUrl, StandardCharsets.US_ASCII);
        JsonObject object = new Gson().fromJson(lines, JsonObject.class);

        Instant expires = Instant.parse(object.getAsJsonPrimitive("expires_at").getAsString());
        String key = object.getAsJsonPrimitive("key").getAsString();
        RSAPublicKey publicKey = parsePublicKey(key);

        byte[] signature = Base64.getDecoder().decode(object.getAsJsonPrimitive("signature").getAsString());
        return ClientPublicKey.of(expires, publicKey, signature);
    }

    private static RSAPublicKey parsePublicKey(String keySpec)
        throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        try (
            Reader reader = new StringReader(keySpec);
            PemReader pemReader = new PemReader(reader)
        ) {
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);

            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(pubKeySpec);
        }
    }
}
