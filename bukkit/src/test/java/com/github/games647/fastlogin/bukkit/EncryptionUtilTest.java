package com.github.games647.fastlogin.bukkit;

import java.security.SecureRandom;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EncryptionUtilTest {

    @Test
    public void testVerifyToken() throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] token = EncryptionUtil.generateVerifyToken(random);

        assertNotNull(token);
        assertEquals(4, token.length);
    }

    // @Test
    // public void testDecryptSharedSecret() throws Exception {
    //
    // }
    //
    // @Test
    // public void testDecryptData() throws Exception {
    //
    // }

    // private static SecretKey createNewSharedKey() {
    //     try {
    //         KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
    //         keygenerator.init(128);
    //         return keygenerator.generateKey();
    //     } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
    //         throw new Error(nosuchalgorithmexception);
    //     }
    // }
}
