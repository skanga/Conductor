package com.skanga.conductor.utils;

/**
 * Utility class for common validation patterns throughout the Conductor framework.
 * <p>
 * This class provides standardized validation methods to eliminate DRY violations
 * in parameter validation and improve consistency across the codebase. All methods
 * throw IllegalArgumentException with standardized message formatting.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe as all methods are static and stateless.
 * </p>
 *
 * @since 1.0.0
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates that the specified object is not null.
     *
     * @param obj the object to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the object is null
     */
    public static void requireNonNull(Object obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException(paramName + " cannot be null");
        }
    }

    /**
     * Validates that the specified string is not null or empty.
     *
     * @param str the string to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the string is null or empty
     */
    public static void requireNonEmpty(String str, String paramName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
    }

    /**
     * Validates that the specified string is not null or blank.
     *
     * @param str the string to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the string is null or blank
     */
    public static void requireNonBlank(String str, String paramName) {
        if (str == null || str.isBlank()) {
            throw new IllegalArgumentException(paramName + " cannot be null or blank");
        }
    }

    /**
     * Validates that the specified number is positive (greater than 0).
     *
     * @param value the number to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the number is not positive
     */
    public static void requirePositive(int value, String paramName) {
        if (value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive, got: " + value);
        }
    }

    /**
     * Validates that the specified number is positive (greater than 0).
     *
     * @param value the number to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the number is not positive
     */
    public static void requirePositive(long value, String paramName) {
        if (value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive, got: " + value);
        }
    }

    /**
     * Validates that the specified number is non-negative (greater than or equal to 0).
     *
     * @param value the number to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the number is negative
     */
    public static void requireNonNegative(int value, String paramName) {
        if (value < 0) {
            throw new IllegalArgumentException(paramName + " cannot be negative, got: " + value);
        }
    }

    /**
     * Validates that the specified number is non-negative (greater than or equal to 0).
     *
     * @param value the number to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the number is negative
     */
    public static void requireNonNegative(long value, String paramName) {
        if (value < 0) {
            throw new IllegalArgumentException(paramName + " cannot be negative, got: " + value);
        }
    }

    /**
     * Validates that the specified number is within the given range (inclusive).
     *
     * @param value the number to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the number is outside the range
     */
    public static void requireInRange(int value, int min, int max, String paramName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d, got: %d", paramName, min, max, value));
        }
    }

    /**
     * Validates that the specified number is within the given range (inclusive).
     *
     * @param value the number to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the number is outside the range
     */
    public static void requireInRange(long value, long min, long max, String paramName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("%s must be between %d and %d, got: %d", paramName, min, max, value));
        }
    }

    /**
     * Validates that the specified condition is true.
     *
     * @param condition the condition to validate
     * @param message the error message if condition is false
     * @throws IllegalArgumentException if the condition is false
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that the specified array or collection is not null or empty.
     *
     * @param array the array to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the array is null or empty
     */
    public static void requireNonEmpty(Object[] array, String paramName) {
        requireNonNull(array, paramName);
        if (array.length == 0) {
            throw new IllegalArgumentException(paramName + " cannot be empty");
        }
    }

    /**
     * Validates that the specified collection is not null or empty.
     *
     * @param collection the collection to validate
     * @param paramName the parameter name for error messaging
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static void requireNonEmpty(java.util.Collection<?> collection, String paramName) {
        requireNonNull(collection, paramName);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be empty");
        }
    }
}