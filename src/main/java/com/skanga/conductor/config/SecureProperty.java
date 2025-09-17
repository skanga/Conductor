package com.skanga.conductor.config;

import java.util.Arrays;
import java.util.Objects;

/**
 * Secure wrapper for sensitive configuration properties.
 * <p>
 * This class provides secure handling of sensitive data such as API keys, passwords,
 * and tokens by implementing several security measures:
 * </p>
 * <ul>
 * <li>Memory protection through char arrays instead of strings</li>
 * <li>Automatic memory clearing when no longer needed</li>
 * <li>Protected toString() to prevent accidental logging</li>
 * <li>Proper cleanup via AutoCloseable interface</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>
 * try (SecureProperty apiKey = SecureProperty.of("sk-sensitive-key")) {
 *     String key = apiKey.getValue(); // Use the key
 *     // Memory automatically cleared when leaving try block
 * }
 * </pre>
 * <p>
 * <strong>Security Note:</strong> While this class provides protection against
 * casual exposure, it cannot prevent all forms of memory analysis or provide
 * protection against determined attackers with system-level access.
 * </p>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 * @see PropertyEncryptor
 */
public final class SecureProperty implements AutoCloseable {

    private char[] value;
    private volatile boolean cleared = false;

    /**
     * Creates a SecureProperty from a string value.
     * <p>
     * The input string is immediately converted to a char array and the
     * original string reference is not retained to minimize exposure time.
     * </p>
     *
     * @param value the sensitive value to protect
     * @return a new SecureProperty instance
     * @throws IllegalArgumentException if value is null
     */
    public static SecureProperty of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Secure property value cannot be null");
        }
        return new SecureProperty(value.toCharArray());
    }

    /**
     * Creates a SecureProperty from a char array.
     * <p>
     * The input array is copied to prevent external modification.
     * </p>
     *
     * @param value the sensitive value to protect
     * @return a new SecureProperty instance
     * @throws IllegalArgumentException if value is null
     */
    public static SecureProperty of(char[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Secure property value cannot be null");
        }
        return new SecureProperty(Arrays.copyOf(value, value.length));
    }

    /**
     * Creates an empty SecureProperty.
     *
     * @return a new empty SecureProperty instance
     */
    public static SecureProperty empty() {
        return new SecureProperty(new char[0]);
    }

    /**
     * Private constructor to ensure controlled creation.
     */
    private SecureProperty(char[] value) {
        this.value = value;
    }

    /**
     * Retrieves the sensitive value as a string.
     * <p>
     * <strong>Warning:</strong> This method creates a String object in memory.
     * Use with caution and ensure the returned string is not stored longer
     * than necessary. Consider using {@link #getValueAsChars()} for better
     * security when possible.
     * </p>
     *
     * @return the sensitive value as a string
     * @throws IllegalStateException if the property has been cleared
     */
    public String getValue() {
        checkNotCleared();
        return new String(value);
    }

    /**
     * Retrieves the sensitive value as a char array.
     * <p>
     * Returns a copy of the internal char array to prevent external modification.
     * The caller is responsible for clearing the returned array when done.
     * </p>
     *
     * @return a copy of the sensitive value as a char array
     * @throws IllegalStateException if the property has been cleared
     */
    public char[] getValueAsChars() {
        checkNotCleared();
        return Arrays.copyOf(value, value.length);
    }

    /**
     * Checks if the property has been cleared.
     *
     * @return true if the property has been cleared, false otherwise
     */
    public boolean isCleared() {
        return cleared;
    }

    /**
     * Checks if the property is empty (zero length).
     *
     * @return true if the property is empty, false otherwise
     * @throws IllegalStateException if the property has been cleared
     */
    public boolean isEmpty() {
        checkNotCleared();
        return value.length == 0;
    }

    /**
     * Returns the length of the sensitive value.
     *
     * @return the length of the value
     * @throws IllegalStateException if the property has been cleared
     */
    public int length() {
        checkNotCleared();
        return value.length;
    }

    /**
     * Clears the sensitive data from memory.
     * <p>
     * This method overwrites the internal char array with zeros and marks
     * the property as cleared. After calling this method, any attempt to
     * access the value will throw an IllegalStateException.
     * </p>
     * <p>
     * This method is idempotent - calling it multiple times has no additional effect.
     * </p>
     */
    public void clear() {
        if (!cleared) {
            Arrays.fill(value, '\0');
            cleared = true;
        }
    }

    /**
     * Automatically clears the property when used in try-with-resources.
     * <p>
     * This method calls {@link #clear()} to ensure the sensitive data is
     * removed from memory when the SecureProperty is no longer needed.
     * </p>
     */
    @Override
    public void close() {
        clear();
    }

    /**
     * Provides a safe string representation that doesn't expose the sensitive value.
     * <p>
     * Returns a masked representation showing only the length and cleared status
     * to prevent accidental logging of sensitive data.
     * </p>
     *
     * @return a safe string representation
     */
    @Override
    public String toString() {
        if (cleared) {
            return "SecureProperty{cleared=true}";
        }
        return String.format("SecureProperty{length=%d, cleared=false}", value.length);
    }

    /**
     * Checks equality based on the sensitive value.
     * <p>
     * <strong>Security Note:</strong> This method accesses the sensitive value
     * for comparison. Use with caution.
     * </p>
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SecureProperty that = (SecureProperty) obj;

        // Both must have same cleared status
        if (this.cleared != that.cleared) return false;

        // If both are cleared, they're equal
        if (this.cleared) return true;

        // Compare the actual values
        return Arrays.equals(this.value, that.value);
    }

    /**
     * Returns a hash code for the SecureProperty.
     * <p>
     * <strong>Security Note:</strong> The hash code is computed from the sensitive
     * value, which could potentially leak information. Use with caution in
     * security-sensitive contexts.
     * </p>
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        if (cleared) {
            return Objects.hash(cleared);
        }
        return Objects.hash(Arrays.hashCode(value), cleared);
    }

    /**
     * Checks if the property has been cleared and throws an exception if so.
     */
    private void checkNotCleared() {
        if (cleared) {
            throw new IllegalStateException("SecureProperty has been cleared and is no longer accessible");
        }
    }

}