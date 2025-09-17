package com.skanga.conductor.config;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles encryption and decryption of sensitive configuration properties.
 * <p>
 * This class provides AES-GCM encryption for protecting sensitive configuration
 * values such as API keys, passwords, and tokens. It supports both automatic
 * key generation and key derivation from environment variables or external
 * key management systems.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>AES-256-GCM encryption for authenticated encryption</li>
 * <li>Random IV generation for each encryption operation</li>
 * <li>Base64 encoding for storage in text-based configuration files</li>
 * <li>Key derivation from environment variables</li>
 * <li>Thread-safe operation</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * PropertyEncryptor encryptor = PropertyEncryptor.fromEnvironment();
 * String encrypted = encryptor.encrypt("sensitive-api-key");
 * String decrypted = encryptor.decrypt(encrypted);
 * </pre>
 * <p>
 * <strong>Security Notes:</strong>
 * </p>
 * <ul>
 * <li>The encryption key should be stored securely outside the application</li>
 * <li>Use environment variables or external key management for production</li>
 * <li>Encrypted values include authentication tags to prevent tampering</li>
 * <li>This provides protection at rest, not protection in memory during use</li>
 * </ul>
 *
 * @since 1.0.0
 * @see SecureProperty
 * @see ApplicationConfig
 */
public class PropertyEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // bytes
    private static final int GCM_TAG_LENGTH = 16; // bytes
    private static final int KEY_LENGTH = 256; // bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Creates a PropertyEncryptor with the specified secret key.
     *
     * @param secretKey the secret key for encryption/decryption
     * @throws IllegalArgumentException if secretKey is null
     */
    public PropertyEncryptor(SecretKey secretKey) {
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }
        this.secretKey = secretKey;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Creates a PropertyEncryptor using a key from an environment variable.
     * <p>
     * Looks for the encryption key in the CONDUCTOR_ENCRYPTION_KEY environment
     * variable. The key should be a Base64-encoded AES key.
     * </p>
     *
     * @return a new PropertyEncryptor instance
     * @throws ConfigurationException if the environment variable is not set or invalid
     */
    public static PropertyEncryptor fromEnvironment() {
        String keyBase64 = System.getenv("CONDUCTOR_ENCRYPTION_KEY");
        if (keyBase64 == null || keyBase64.trim().isEmpty()) {
            throw new ConfigurationException(
                "CONDUCTOR_ENCRYPTION_KEY environment variable not set. " +
                "Generate a key with: PropertyEncryptor.generateKeyBase64()");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
            SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);
            return new PropertyEncryptor(key);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid encryption key in environment variable", e);
        }
    }

    /**
     * Creates a PropertyEncryptor with a newly generated random key.
     * <p>
     * <strong>Warning:</strong> This method generates a new key each time it's called.
     * The generated key will not persist across application restarts, making
     * previously encrypted values unreadable. This method is primarily useful
     * for testing or when implementing custom key persistence.
     * </p>
     *
     * @return a new PropertyEncryptor instance with a random key
     */
    public static PropertyEncryptor withRandomKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            return new PropertyEncryptor(key);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to generate encryption key", e);
        }
    }

    /**
     * Encrypts a plaintext value.
     * <p>
     * The encrypted result includes the IV and authentication tag, making it
     * self-contained. The result is Base64-encoded for safe storage in
     * text-based configuration files.
     * </p>
     *
     * @param plaintext the value to encrypt
     * @return the encrypted value as a Base64-encoded string
     * @throws IllegalArgumentException if plaintext is null
     * @throws ConfigurationException if encryption fails
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt the plaintext
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

            // Encode to Base64 for storage
            return Base64.getEncoder().encodeToString(encryptedData);

        } catch (Exception e) {
            throw new ConfigurationException("Failed to encrypt property value", e);
        }
    }

    /**
     * Decrypts a previously encrypted value.
     * <p>
     * The input should be a Base64-encoded string produced by the encrypt method.
     * The method extracts the IV and uses it to decrypt the ciphertext.
     * </p>
     *
     * @param encryptedValue the encrypted value to decrypt
     * @return the decrypted plaintext value
     * @throws IllegalArgumentException if encryptedValue is null or empty
     * @throws ConfigurationException if decryption fails
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Encrypted value cannot be null or empty");
        }

        try {
            // Decode from Base64
            byte[] encryptedData = Base64.getDecoder().decode(encryptedValue.trim());

            // Extract IV and ciphertext
            if (encryptedData.length < GCM_IV_LENGTH) {
                throw new ConfigurationException("Invalid encrypted data: too short");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];

            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt and return
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new ConfigurationException("Failed to decrypt property value", e);
        }
    }

    /**
     * Checks if a property value appears to be encrypted.
     * <p>
     * This is a heuristic check based on the format of encrypted values.
     * It checks for Base64 encoding and appropriate length.
     * </p>
     *
     * @param value the value to check
     * @return true if the value appears to be encrypted, false otherwise
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // Check if it looks like a Base64-encoded encrypted value
        String trimmed = value.trim();

        // Basic Base64 pattern check
        if (!trimmed.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(trimmed);
            // Encrypted values should be at least IV + some ciphertext + tag
            return decoded.length >= (GCM_IV_LENGTH + GCM_TAG_LENGTH + 1);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a Base64-encoded encryption key for use in environment variables.
     * <p>
     * This method generates a new AES-256 key and encodes it as Base64.
     * The generated key can be stored in the CONDUCTOR_ENCRYPTION_KEY
     * environment variable for use with {@link #fromEnvironment()}.
     * </p>
     *
     * @return a Base64-encoded encryption key
     */
    public static String generateKeyBase64() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new ConfigurationException("Failed to generate encryption key", e);
        }
    }

    /**
     * Returns the algorithm used for encryption.
     *
     * @return the encryption algorithm name
     */
    public String getAlgorithm() {
        return TRANSFORMATION;
    }

    /**
     * Returns information about the encryption configuration.
     *
     * @return a string describing the encryption setup
     */
    @Override
    public String toString() {
        return String.format("PropertyEncryptor{algorithm=%s, keyLength=%d}",
            TRANSFORMATION, KEY_LENGTH);
    }
}