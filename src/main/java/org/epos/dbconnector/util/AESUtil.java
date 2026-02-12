package org.epos.dbconnector.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-CBC encryption/decryption utility compatible with OpenSSL.
 * Supports both:
 * - PBKDF2 key derivation (modern, more secure)
 * - EVP_BytesToKey MD5-based key derivation (legacy OpenSSL compatibility)
 * 
 * Uses the OpenSSL "Salted__" format for encrypted data.
 */
public class AESUtil {

    private static final String SALTED_PREFIX = "Salted__";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int SALT_LENGTH = 8;
    private static final int PBKDF2_ITERATIONS = 10000;

    // Internal salt/passphrase for encryption - change manually as needed
    private static final String INTERNAL_SALT = "fxUoIlLqLVuN";

    /**
     * Encrypts plain text using AES-256-CBC with the internal salt.
     * Uses PBKDF2 key derivation (modern approach).
     *
     * @param plainText The text to encrypt
     * @return Base64 encoded encrypted string in OpenSSL format
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, INTERNAL_SALT);
    }

    /**
     * Encrypts plain text using AES-256-CBC with a custom passphrase.
     * Uses PBKDF2 key derivation (modern approach).
     *
     * @param plainText  The text to encrypt
     * @param passphrase The passphrase to use for encryption
     * @return Base64 encoded encrypted string in OpenSSL format
     */
    public static String encrypt(String plainText, String passphrase) {
        try {
            // Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            // Derive key and IV using PBKDF2
            byte[][] keyAndIv = deriveKeyAndIvPBKDF2(passphrase, salt);
            byte[] key = keyAndIv[0];
            byte[] iv = keyAndIv[1];

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Encrypt
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine: "Salted__" + salt + encrypted data
            byte[] saltedPrefix = SALTED_PREFIX.getBytes(StandardCharsets.US_ASCII);
            byte[] result = new byte[saltedPrefix.length + salt.length + encrypted.length];
            System.arraycopy(saltedPrefix, 0, result, 0, saltedPrefix.length);
            System.arraycopy(salt, 0, result, saltedPrefix.length, salt.length);
            System.arraycopy(encrypted, 0, result, saltedPrefix.length + salt.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }

    /**
     * Encrypts plain text using AES-256-CBC with legacy OpenSSL EVP_BytesToKey (MD5-based).
     * Use this for compatibility with older OpenSSL-encrypted data.
     *
     * @param plainText  The text to encrypt
     * @param passphrase The passphrase to use for encryption
     * @return Base64 encoded encrypted string in OpenSSL format
     */
    public static String encryptLegacy(String plainText, String passphrase) {
        try {
            // Generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(salt);

            // Derive key and IV using EVP_BytesToKey (MD5-based, legacy)
            byte[][] keyAndIv = deriveKeyAndIvEVP(passphrase, salt);
            byte[] key = keyAndIv[0];
            byte[] iv = keyAndIv[1];

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Encrypt
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine: "Salted__" + salt + encrypted data
            byte[] saltedPrefix = SALTED_PREFIX.getBytes(StandardCharsets.US_ASCII);
            byte[] result = new byte[saltedPrefix.length + salt.length + encrypted.length];
            System.arraycopy(saltedPrefix, 0, result, 0, saltedPrefix.length);
            System.arraycopy(salt, 0, result, saltedPrefix.length, salt.length);
            System.arraycopy(encrypted, 0, result, saltedPrefix.length + salt.length, encrypted.length);

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data (legacy)", e);
        }
    }

    /**
     * Decrypts an OpenSSL-formatted AES-256-CBC encrypted string using the internal salt.
     * Automatically tries PBKDF2 first, then falls back to legacy EVP_BytesToKey.
     *
     * @param encryptedText Base64 encoded encrypted string
     * @return Decrypted plain text
     */
    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, INTERNAL_SALT);
    }

    /**
     * Decrypts an OpenSSL-formatted AES-256-CBC encrypted string.
     * Automatically tries PBKDF2 first, then falls back to legacy EVP_BytesToKey.
     *
     * @param encryptedText Base64 encoded encrypted string
     * @param passphrase    The passphrase used for encryption
     * @return Decrypted plain text
     */
    public static String decrypt(String encryptedText, String passphrase) {
        // Try PBKDF2 first (modern)
        try {
            return decryptWithMethod(encryptedText, passphrase, false);
        } catch (Exception e) {
            // Fall back to legacy EVP_BytesToKey
            try {
                return decryptWithMethod(encryptedText, passphrase, true);
            } catch (Exception e2) {
                throw new RuntimeException("Error decrypting data (tried both PBKDF2 and legacy EVP)", e2);
            }
        }
    }

    /**
     * Decrypts using specifically PBKDF2 key derivation.
     *
     * @param encryptedText Base64 encoded encrypted string
     * @param passphrase    The passphrase used for encryption
     * @return Decrypted plain text
     */
    public static String decryptPBKDF2(String encryptedText, String passphrase) {
        return decryptWithMethod(encryptedText, passphrase, false);
    }

    /**
     * Decrypts using specifically legacy EVP_BytesToKey (MD5-based) key derivation.
     * Use this for data encrypted with older OpenSSL versions.
     *
     * @param encryptedText Base64 encoded encrypted string
     * @param passphrase    The passphrase used for encryption
     * @return Decrypted plain text
     */
    public static String decryptLegacy(String encryptedText, String passphrase) {
        return decryptWithMethod(encryptedText, passphrase, true);
    }

    /**
     * Internal method to decrypt with specified key derivation method.
     */
    private static String decryptWithMethod(String encryptedText, String passphrase, boolean useLegacy) {
        try {
            // Decode Base64
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // Check for "Salted__" prefix
            byte[] saltedPrefix = SALTED_PREFIX.getBytes(StandardCharsets.US_ASCII);
            byte[] prefixFromData = Arrays.copyOfRange(decoded, 0, saltedPrefix.length);

            if (!Arrays.equals(saltedPrefix, prefixFromData)) {
                throw new IllegalArgumentException("Invalid encrypted data format: missing 'Salted__' prefix");
            }

            // Extract salt (8 bytes after "Salted__")
            byte[] salt = Arrays.copyOfRange(decoded, saltedPrefix.length, saltedPrefix.length + SALT_LENGTH);

            // Extract encrypted data
            byte[] encrypted = Arrays.copyOfRange(decoded, saltedPrefix.length + SALT_LENGTH, decoded.length);

            // Derive key and IV
            byte[][] keyAndIv = useLegacy 
                    ? deriveKeyAndIvEVP(passphrase, salt) 
                    : deriveKeyAndIvPBKDF2(passphrase, salt);
            byte[] key = keyAndIv[0];
            byte[] iv = keyAndIv[1];

            // Initialize cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Decrypt
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data" + (useLegacy ? " (legacy)" : " (PBKDF2)"), e);
        }
    }

    /**
     * Derives a 256-bit key and 128-bit IV from a passphrase and salt using PBKDF2.
     * This is the modern, more secure approach.
     *
     * @param passphrase The passphrase
     * @param salt       The salt
     * @return Array containing [key, iv]
     */
    private static byte[][] deriveKeyAndIvPBKDF2(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, (KEY_LENGTH + IV_LENGTH * 8));
        SecretKey tmp = factory.generateSecret(spec);
        byte[] keyAndIv = tmp.getEncoded();

        byte[] key = Arrays.copyOfRange(keyAndIv, 0, KEY_LENGTH / 8);
        byte[] iv = Arrays.copyOfRange(keyAndIv, KEY_LENGTH / 8, KEY_LENGTH / 8 + IV_LENGTH);

        return new byte[][]{key, iv};
    }

    /**
     * Derives a 256-bit key and 128-bit IV using OpenSSL's EVP_BytesToKey with MD5.
     * This is the legacy method used by older OpenSSL versions (pre-1.1.0 default).
     * Compatible with: openssl enc -aes-256-cbc -salt -md md5
     *
     * @param passphrase The passphrase
     * @param salt       The salt
     * @return Array containing [key, iv]
     */
    private static byte[][] deriveKeyAndIvEVP(String passphrase, byte[] salt) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] passBytes = passphrase.getBytes(StandardCharsets.UTF_8);
        
        int keyLength = KEY_LENGTH / 8;  // 32 bytes
        int ivLength = IV_LENGTH;         // 16 bytes
        int totalLength = keyLength + ivLength;  // 48 bytes
        
        byte[] result = new byte[totalLength];
        byte[] lastHash = new byte[0];
        int resultOffset = 0;
        
        while (resultOffset < totalLength) {
            md5.reset();
            md5.update(lastHash);
            md5.update(passBytes);
            md5.update(salt);
            lastHash = md5.digest();
            
            int copyLength = Math.min(lastHash.length, totalLength - resultOffset);
            System.arraycopy(lastHash, 0, result, resultOffset, copyLength);
            resultOffset += copyLength;
        }
        
        byte[] key = Arrays.copyOfRange(result, 0, keyLength);
        byte[] iv = Arrays.copyOfRange(result, keyLength, keyLength + ivLength);
        
        return new byte[][]{key, iv};
    }

    /**
     * Checks if a string appears to be encrypted (starts with OpenSSL Salted prefix in Base64).
     *
     * @param text The text to check
     * @return true if the text appears to be encrypted
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.length() < 12) {
            return false;
        }
        // "Salted__" in Base64 starts with "U2FsdGVkX1"
        return text.startsWith("U2FsdGVkX1");
    }

    /**
     * Returns the internal salt value (for reference/documentation purposes).
     *
     * @return The internal salt
     */
    public static String getInternalSalt() {
        return INTERNAL_SALT;
    }
}
