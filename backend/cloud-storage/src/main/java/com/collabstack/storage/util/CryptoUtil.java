package com.collabstack.storage.util;

import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

@Component
public class CryptoUtil {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // 96-bit IV
    private static final int GCM_TAG_LENGTH = 128;  // 128-bit auth tag
    private static final int AES_KEY_SIZE = 256;
    private static final int PBKDF2_ITERATIONS = 100_000;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts data with AES/GCM. Returns 12-byte IV prepended to ciphertext.
     */
    public byte[] encrypt(byte[] data, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        byte[] ciphertext = cipher.doFinal(data);

        byte[] result = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH, ciphertext.length);
        return result;
    }

    /**
     * Decrypts data encrypted by {@link #encrypt}. Extracts IV from first 12 bytes.
     */
    public byte[] decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

        byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(ciphertext);
    }

    /**
     * Generates a random AES-256 Data Encryption Key (DEK).
     */
    public SecretKey generateDek() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE, secureRandom);
        return keyGen.generateKey();
    }

    /**
     * Derives a Key Encryption Key (KEK) from a user password using PBKDF2.
     */
    public SecretKey deriveKek(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_SIZE);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a DEK with a KEK. Returns IV-prepended ciphertext.
     */
    public byte[] encryptKey(SecretKey dek, SecretKey kek) throws Exception {
        return encrypt(dek.getEncoded(), kek);
    }

    /**
     * Decrypts an encrypted DEK with a KEK. Returns the original DEK.
     */
    public SecretKey decryptKey(byte[] encryptedDek, SecretKey kek) throws Exception {
        byte[] dekBytes = decrypt(encryptedDek, kek);
        return new SecretKeySpec(dekBytes, "AES");
    }

    /**
     * Generates a random salt for PBKDF2.
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }
}
