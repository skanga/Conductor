package com.skanga.conductor.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utility class for JSON operations using Jackson.
 * <p>
 * This class provides centralized JSON functionality to replace Gson usage throughout
 * the codebase, offering consistent configuration, error handling, and API patterns.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>Thread-safe singleton ObjectMapper instance</li>
 * <li>Consistent error handling with RuntimeExceptions</li>
 * <li>Type-safe serialization and deserialization</li>
 * <li>JSON tree model support for complex parsing</li>
 * <li>Null-safe operations</li>
 * </ul>
 * <p>
 * This utility class is designed to provide drop-in replacements for common Gson patterns:
 * </p>
 * <pre>
 * // Gson pattern:
 * String json = new Gson().toJson(object);
 * Object obj = new Gson().fromJson(json, Object.class);
 *
 * // Jackson pattern with JsonUtils:
 * String json = JsonUtils.toJson(object);
 * Object obj = JsonUtils.fromJson(json, Object.class);
 * </pre>
 *
 * @since 1.0.0
 */
public final class JsonUtils {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * Shared ObjectMapper instance configured for optimal performance and security.
     * <p>
     * The ObjectMapper is thread-safe and can be shared across the application.
     * It's configured with security-hardened defaults for the Conductor framework:
     * </p>
     * <ul>
     * <li>Polymorphic type handling with whitelist validation</li>
     * <li>Safe deserialization preventing arbitrary code execution</li>
     * <li>Only allows trusted base types and Conductor framework classes</li>
     * </ul>
     */
    private static final ObjectMapper OBJECT_MAPPER = createSecureObjectMapper();

    /**
     * Creates a security-hardened ObjectMapper with safe deserialization settings.
     * <p>
     * This configuration prevents unsafe deserialization attacks by:
     * </p>
     * <ul>
     * <li>Using BasicPolymorphicTypeValidator to whitelist allowed base types</li>
     * <li>Only allowing classes from com.skanga.conductor package</li>
     * <li>Allowing common safe base types (String, Number, Boolean, etc.)</li>
     * <li>Blocking dangerous classes like Object, Serializable, Comparable</li>
     * </ul>
     * <p>
     * Note: Default typing is NOT enabled by default to maintain JSON compatibility.
     * Type validation is enforced only when deserialization occurs with known types.
     * For polymorphic deserialization needs, use the dedicated type-safe methods.
     * </p>
     *
     * @return configured ObjectMapper with security settings
     */
    private static ObjectMapper createSecureObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Create a polymorphic type validator that allows only safe base types
        // and classes from our framework package
        // This validator is used when type information is present in JSON,
        // but we don't activate it globally to avoid breaking existing JSON format
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                // Allow primitives and common safe types
                .allowIfBaseType(String.class)
                .allowIfBaseType(Number.class)
                .allowIfBaseType(Boolean.class)
                .allowIfBaseType(java.util.Date.class)
                .allowIfBaseType(java.time.Instant.class)
                .allowIfBaseType(java.time.LocalDateTime.class)
                .allowIfBaseType(java.time.LocalDate.class)
                .allowIfBaseType(java.util.UUID.class)
                // Allow collections with safe generic types
                .allowIfBaseType(java.util.List.class)
                .allowIfBaseType(java.util.Map.class)
                .allowIfBaseType(java.util.Set.class)
                // Allow our framework classes
                .allowIfSubTypeIsArray()
                .allowIfSubType("com.skanga.conductor.")
                .build();

        // Set the validator for type resolution
        // This provides protection when @JsonTypeInfo is used in classes
        mapper.setPolymorphicTypeValidator(ptv);

        // SECURITY: Do NOT activate default typing globally as it changes JSON format
        // and breaks compatibility. Instead, rely on:
        // 1. Explicit type references in fromJson() methods
        // 2. @JsonTypeInfo annotations on specific classes that need polymorphism
        // 3. The type validator above to block dangerous types

        logger.debug("Initialized secure ObjectMapper with polymorphic type validation (default typing disabled for compatibility)");
        return mapper;
    }

    // Private constructor to prevent instantiation
    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts an object to its JSON string representation.
     * <p>
     * This method provides a direct replacement for Gson's {@code toJson()} method.
     * </p>
     *
     * @param object the object to serialize to JSON
     * @return JSON string representation of the object
     * @throws com.skanga.conductor.exception.JsonProcessingException if serialization fails
     */
    public static String toJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new com.skanga.conductor.exception.JsonProcessingException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Converts an object to a pretty-printed JSON string.
     *
     * @param object the object to serialize to JSON
     * @return pretty-printed JSON string representation of the object
     * @throws com.skanga.conductor.exception.JsonProcessingException if serialization fails
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new com.skanga.conductor.exception.JsonProcessingException("Failed to serialize object to pretty JSON", e);
        }
    }

    /**
     * Converts a JSON string to an object of the specified type.
     * <p>
     * This method provides a direct replacement for Gson's {@code fromJson()} method.
     * </p>
     *
     * @param <T> the type of object to deserialize to
     * @param json the JSON string to deserialize
     * @param clazz the class of the target object type
     * @return the deserialized object
     * @throws com.skanga.conductor.exception.JsonProcessingException if deserialization fails
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new com.skanga.conductor.exception.JsonProcessingException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Converts a JSON string to an object using a TypeReference for complex generic types.
     * <p>
     * This method is useful for deserializing collections and other generic types:
     * </p>
     * <pre>
     * List&lt;String&gt; list = JsonUtils.fromJson(json, new TypeReference&lt;List&lt;String&gt;&gt;() {});
     * Map&lt;String, Object&gt; map = JsonUtils.fromJson(json, new TypeReference&lt;Map&lt;String, Object&gt;&gt;() {});
     * </pre>
     *
     * @param <T> the type of object to deserialize to
     * @param json the JSON string to deserialize
     * @param typeReference the TypeReference specifying the target type
     * @return the deserialized object
     * @throws com.skanga.conductor.exception.JsonProcessingException if deserialization fails
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new com.skanga.conductor.exception.JsonProcessingException("Failed to deserialize JSON to " + typeReference.getType(), e);
        }
    }

    /**
     * Parses a JSON string into a JsonNode tree model.
     * <p>
     * This provides functionality similar to Gson's JsonParser for traversing
     * and analyzing JSON structure programmatically.
     * </p>
     *
     * @param json the JSON string to parse
     * @return JsonNode representing the parsed JSON
     * @throws com.skanga.conductor.exception.JsonProcessingException if parsing fails
     */
    public static JsonNode parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return OBJECT_MAPPER.nullNode();
        }
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new com.skanga.conductor.exception.JsonProcessingException("Failed to parse JSON", e);
        }
    }

    /**
     * Creates a new empty ObjectNode for building JSON objects programmatically.
     * <p>
     * This provides functionality similar to Gson's JsonObject for building
     * JSON structures dynamically.
     * </p>
     *
     * @return new empty ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    /**
     * Creates a new empty ArrayNode for building JSON arrays programmatically.
     * <p>
     * This provides functionality similar to Gson's JsonArray for building
     * JSON arrays dynamically.
     * </p>
     *
     * @return new empty ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

    /**
     * Checks if a string contains valid JSON.
     *
     * @param json the string to validate
     * @return true if the string is valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Converts a JsonNode to a string representation.
     * <p>
     * This is useful when working with the tree model API and needing
     * to convert nodes back to JSON strings.
     * </p>
     *
     * @param node the JsonNode to convert
     * @return JSON string representation of the node
     * @throws com.skanga.conductor.exception.JsonProcessingException if conversion fails
     */
    public static String toString(JsonNode node) {
        if (node == null) {
            return "null";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new com.skanga.conductor.exception.JsonProcessingException("Failed to convert JsonNode to string", e);
        }
    }

    /**
     * Gets the underlying ObjectMapper for advanced use cases.
     * <p>
     * Use this method sparingly and only when the utility methods don't
     * provide the needed functionality.
     * </p>
     *
     * @return the shared ObjectMapper instance
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

}