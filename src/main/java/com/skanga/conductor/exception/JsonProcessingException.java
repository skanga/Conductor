package com.skanga.conductor.exception;

/**
 * Exception thrown when JSON processing operations fail.
 * <p>
 * This exception is thrown when JSON serialization, deserialization, or parsing
 * operations encounter errors. It provides detailed context about the failure
 * to help with debugging and error handling.
 * </p>
 * <p>
 * Common scenarios that trigger this exception:
 * </p>
 * <ul>
 * <li>Malformed JSON syntax during parsing</li>
 * <li>Type mismatches during deserialization</li>
 * <li>Circular references during serialization</li>
 * <li>Unsupported data types or structures</li>
 * <li>IO errors during JSON processing</li>
 * </ul>
 *
 * @see com.skanga.conductor.utils.JsonUtils
 * @since 1.0.0
 */
public class JsonProcessingException extends ConductorRuntimeException {

    /**
     * Constructs a new JsonProcessingException with the specified detail message.
     *
     * @param message the detail message explaining the JSON processing error
     */
    public JsonProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new JsonProcessingException with the specified detail message and cause.
     *
     * @param message the detail message explaining the JSON processing error
     * @param cause   the underlying cause of the JSON processing error
     */
    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}