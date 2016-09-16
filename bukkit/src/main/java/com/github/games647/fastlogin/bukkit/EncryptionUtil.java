package com.github.games647.fastlogin.bukkit;

import com.google.common.base.Charsets;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryption and decryption minecraft util for connection between servers
 * and paid minecraft account clients.
 *
 * Source: https://github.com/bergerkiller/CraftSource/blob/master/net.minecraft.server/MinecraftEncryption.java
 *
 * Remapped by: https://github.com/Techcable/MinecraftMappings/tree/master/1.8
 */
public class EncryptionUtil {

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(1_024);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            //Should be existing in every vm
            throw new ExceptionInInitializerError(nosuchalgorithmexception);
        }
    }

    public static byte[] getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey) {
        return digestOperation("SHA-1"
                , new byte[][]{serverId.getBytes(Charsets.ISO_8859_1), secretKey.getEncoded(), publicKey.getEncoded()});
    }

    private static byte[] digestOperation(String algo, byte[]... content) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(algo);
            Stream.of(content).forEach(messagedigest::update);

            return messagedigest.digest();
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            nosuchalgorithmexception.printStackTrace();
            return null;
        }
    }

//    public static PublicKey decodePublicKey(byte[] encodedKey) {
//        try {
//            KeyFactory keyfactory = KeyFactory.getInstance("RSA");
//
//            X509EncodedKeySpec x509encodedkeyspec = new X509EncodedKeySpec(encodedKey);
//            return keyfactory.generatePublic(x509encodedkeyspec);
//        } catch (NoSuchAlgorithmException | InvalidKeySpecException nosuchalgorithmexception) {
//            //ignore
//        }
//
//        System.err.println("Public key reconstitute failed!");
//        return null;
//    }

    public static SecretKey decryptSharedKey(PrivateKey privateKey, byte[] encryptedSharedKey) {
        return new SecretKeySpec(decryptData(privateKey, encryptedSharedKey), "AES");
    }

    public static byte[] decryptData(Key key, byte[] data) {
        return cipherOperation(Cipher.DECRYPT_MODE, key, data);
    }

    private static byte[] cipherOperation(int operationMode, Key key, byte[] data) {
        try {
            return createCipherInstance(operationMode, key.getAlgorithm(), key).doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException illegalblocksizeexception) {
            illegalblocksizeexception.printStackTrace();
        }

        System.err.println("Cipher data failed!");
        return null;
    }

    private static Cipher createCipherInstance(int operationMode, String cipherName, Key key) {
        try {
            Cipher cipher = Cipher.getInstance(cipherName);

            cipher.init(operationMode, key);
            return cipher;
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException invalidkeyexception) {
            invalidkeyexception.printStackTrace();
        }

        System.err.println("Cipher creation failed!");
        return null;
    }
//
//    public static Cipher createBufferedBlockCipher(int operationMode, Key key) {
//        try {
//            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
//
//            cipher.init(operationMode, key, new IvParameterSpec(key.getEncoded()));
//            return cipher;
//        } catch (GeneralSecurityException generalsecurityexception) {
//            throw new RuntimeException(generalsecurityexception);
//        }
//    }

    private EncryptionUtil() {
        //utility
    }
}
