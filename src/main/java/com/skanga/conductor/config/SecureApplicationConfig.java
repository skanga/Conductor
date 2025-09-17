package com.skanga.conductor.config;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure extension of ApplicationConfig with encryption support for sensitive properties.
 * <p>
 * This class extends the standard ApplicationConfig to provide transparent encryption
 * and decryption of sensitive configuration values. It automatically detects encrypted
 * properties and decrypts them when accessed, while providing secure handling of
 * sensitive data in memory.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Transparent encryption/decryption of sensitive properties</li>
 * <li>Automatic detection of encrypted values</li>
 * <li>Secure memory handling with SecureProperty wrappers</li>
 * <li>Configurable sensitive property identification</li>
 * <li>Backwards compatibility with unencrypted properties</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * // In application.properties:
 * conductor.llm.openai.api.key=ENC(base64-encrypted-value)
 * conductor.database.password=ENC(base64-encrypted-value)
 *
 * // In code:
 * SecureApplicationConfig config = SecureApplicationConfig.getInstance();
 * try (SecureProperty apiKey = config.getSecureProperty("conductor.llm.openai.api.key")) {
 *     String key = apiKey.getValue(); // Automatically decrypted
 * }
 * </pre>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 * @see SecureProperty
 * @see PropertyEncryptor
 */
public class SecureApplicationConfig implements AutoCloseable {

    private static volatile SecureApplicationConfig instance;
    private final ApplicationConfig appConfig;
    private final PropertyEncryptor encryptor;
    private final Set<String> sensitiveKeys;
    private final ConcurrentHashMap<String, SecureProperty> securePropertyCache;

    // Encryption marker constants
    private static final String ENCRYPTION_PREFIX = "ENC(";
    private static final String ENCRYPTION_SUFFIX = ")";

    /**
     * Private constructor for singleton pattern.
     */
    private SecureApplicationConfig() {
        this.appConfig = ApplicationConfig.getInstance();
        this.encryptor = initializeEncryptor();
        this.sensitiveKeys = initializeSensitiveKeys();
        this.securePropertyCache = new ConcurrentHashMap<>();
    }

    /**
     * Returns the singleton instance of the secure application configuration.
     * <p>
     * This method uses double-checked locking to ensure thread-safe
     * singleton initialization with encryption support.
     * </p>
     *
     * @return the singleton SecureApplicationConfig instance
     */
    public static SecureApplicationConfig getInstance() {
        if (instance == null) {
            synchronized (SecureApplicationConfig.class) {
                if (instance == null) {
                    instance = new SecureApplicationConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves a sensitive property as a SecureProperty for safe handling.
     * <p>
     * This method automatically decrypts encrypted values and wraps them in
     * a SecureProperty for memory-safe handling. The caller is responsible
     * for closing the SecureProperty when done.
     * </p>
     *
     * @param key the property key to retrieve
     * @return a SecureProperty containing the decrypted value, or null if not found
     */
    public SecureProperty getSecureProperty(String key) {
        return getSecureProperty(key, null);
    }

    /**
     * Retrieves a sensitive property as a SecureProperty with a default value.
     * <p>
     * This method automatically decrypts encrypted values and wraps them in
     * a SecureProperty for memory-safe handling. If the property is not found,
     * the default value is used (and not encrypted).
     * </p>
     *
     * @param key the property key to retrieve
     * @param defaultValue the default value if the property is not found
     * @return a SecureProperty containing the decrypted value or default
     */
    public SecureProperty getSecureProperty(String key, String defaultValue) {
        String rawValue = appConfig.getString(key);

        if (rawValue == null) {
            return defaultValue != null ? SecureProperty.of(defaultValue) : null;
        }

        String decryptedValue = decryptIfNeeded(rawValue, key);
        return SecureProperty.of(decryptedValue);
    }

    /**
     * Retrieves a string property, automatically decrypting if it's encrypted.
     * <p>
     * This overrides the parent method to provide transparent decryption.
     * <strong>Warning:</strong> This method returns a String, which is less
     * secure than using getSecureProperty() for sensitive data.
     * </p>
     *
     * @param key the property key to lookup
     * @param defaultValue the value to return if the property is not found
     * @return the property value (decrypted if necessary) or the default value
     */
    public String getString(String key, String defaultValue) {
        String rawValue = appConfig.getString(key, defaultValue);

        if (rawValue == null || rawValue.equals(defaultValue)) {
            return rawValue;
        }

        return decryptIfNeeded(rawValue, key);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * Encrypts a property value for storage.
     * <p>
     * This method encrypts the given value and wraps it with the encryption
     * markers for recognition during loading. The resulting string can be
     * stored in configuration files.
     * </p>
     *
     * @param value the value to encrypt
     * @return the encrypted value wrapped with encryption markers
     * @throws IllegalArgumentException if value is null
     * @throws ConfigurationException if encryption fails
     */
    public String encryptProperty(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Value to encrypt cannot be null");
        }

        if (encryptor == null) {
            throw new ConfigurationException("PropertyEncryptor not available");
        }

        String encrypted = encryptor.encrypt(value);
        return ENCRYPTION_PREFIX + encrypted + ENCRYPTION_SUFFIX;
    }

    /**
     * Checks if a property key represents sensitive information.
     * <p>
     * This method checks both the predefined sensitive keys and uses heuristics
     * to identify potentially sensitive properties based on naming patterns.
     * </p>
     *
     * @param key the property key to check
     * @return true if the key represents sensitive information
     */
    public boolean isSensitiveProperty(String key) {
        if (key == null) {
            return false;
        }

        // Check predefined sensitive keys
        if (sensitiveKeys.contains(key)) {
            return true;
        }

        // Use heuristics for detection
        return ConfigurationValidator.isSensitiveProperty(key);
    }

    /**
     * Checks if a property value is encrypted.
     *
     * @param value the property value to check
     * @return true if the value appears to be encrypted
     */
    public boolean isEncrypted(String value) {
        return value != null &&
               value.startsWith(ENCRYPTION_PREFIX) &&
               value.endsWith(ENCRYPTION_SUFFIX);
    }

    /**
     * Returns information about the encryption status of the configuration.
     *
     * @return a summary of encryption configuration
     */
    public String getEncryptionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Secure Configuration Status:\n");
        status.append("==========================\n");

        if (encryptor != null) {
            status.append("Encryption: Enabled (").append(encryptor.getAlgorithm()).append(")\n");
        } else {
            status.append("Encryption: Disabled\n");
        }

        status.append("Sensitive properties defined: ").append(sensitiveKeys.size()).append("\n");
        status.append("Secure property cache size: ").append(securePropertyCache.size()).append("\n");

        return status.toString();
    }

    /**
     * Clears all cached secure properties.
     * <p>
     * This method clears all SecureProperty instances from the cache and
     * ensures their memory is properly cleaned up.
     * </p>
     */
    public void clearSecurePropertyCache() {
        securePropertyCache.values().forEach(SecureProperty::clear);
        securePropertyCache.clear();
    }

    /**
     * Closes this SecureApplicationConfig and clears all sensitive data.
     * <p>
     * This method implements AutoCloseable to provide proper resource cleanup.
     * It clears all SecureProperty instances from the cache and ensures their
     * memory is properly cleaned up.
     * </p>
     */
    @Override
    public void close() {
        clearSecurePropertyCache();
    }

    /**
     * Initializes the PropertyEncryptor from environment or returns null if not available.
     */
    private PropertyEncryptor initializeEncryptor() {
        try {
            return PropertyEncryptor.fromEnvironment();
        } catch (ConfigurationException e) {
            // Encryption not configured - log warning and continue without encryption
            System.err.println("Warning: Property encryption not configured. " +
                "Set CONDUCTOR_ENCRYPTION_KEY environment variable to enable encryption.");
            return null;
        }
    }

    /**
     * Initializes the set of known sensitive property keys.
     */
    private Set<String> initializeSensitiveKeys() {
        return Set.of(
            "conductor.llm.openai.api.key",
            "conductor.database.password",
            "conductor.security.jwt.secret",
            "conductor.oauth.client.secret",
            "conductor.webhook.secret",
            "conductor.encryption.key"
        );
    }

    /**
     * Decrypts a value if it's encrypted, otherwise returns it unchanged.
     */
    private String decryptIfNeeded(String value, String key) {
        if (value == null) {
            return null;
        }

        if (isEncrypted(value)) {
            if (encryptor == null) {
                throw new ConfigurationException(
                    "Encrypted property found but no encryption key available: " + key);
            }

            try {
                // Extract the encrypted content between markers
                String encryptedContent = value.substring(
                    ENCRYPTION_PREFIX.length(),
                    value.length() - ENCRYPTION_SUFFIX.length()
                );
                return encryptor.decrypt(encryptedContent);
            } catch (Exception e) {
                throw new ConfigurationException(
                    "Failed to decrypt property: " + key, e);
            }
        }

        return value;
    }

}