package com.skanga.conductor.workflow.config;

import java.util.Map;
import java.util.Optional;

/**
 * Represents runtime context for workflow execution.
 * Contains variables, settings, and metadata that can be used during workflow execution.
 */
public class WorkflowContext {

    private final Object data;

    public WorkflowContext(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    /**
     * Gets a value from the context by path (e.g., "runtime.session_id").
     *
     * @param path the path to the value (dot-separated)
     * @return Optional containing the value, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public Optional<Object> getValue(String path) {
        if (data == null || path == null) {
            return Optional.empty();
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return Optional.empty();
            }
        }

        return Optional.ofNullable(current);
    }

    /**
     * Gets a string value from the context.
     *
     * @param path the path to the value
     * @return Optional containing the string value, or empty if not found
     */
    public Optional<String> getString(String path) {
        return getValue(path).map(Object::toString);
    }

    /**
     * Gets a string value with a default fallback.
     *
     * @param path the path to the value
     * @param defaultValue the default value if not found
     * @return the string value or default
     */
    public String getString(String path, String defaultValue) {
        return getString(path).orElse(defaultValue);
    }

    /**
     * Gets an integer value from the context.
     *
     * @param path the path to the value
     * @return Optional containing the integer value, or empty if not found or not convertible
     */
    public Optional<Integer> getInteger(String path) {
        return getValue(path).flatMap(value -> {
            if (value instanceof Number) {
                return Optional.of(((Number) value).intValue());
            }
            if (value instanceof String) {
                try {
                    return Optional.of(Integer.parseInt((String) value));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Gets an integer value with a default fallback.
     *
     * @param path the path to the value
     * @param defaultValue the default value if not found
     * @return the integer value or default
     */
    public Integer getInteger(String path, Integer defaultValue) {
        return getInteger(path).orElse(defaultValue);
    }

    /**
     * Gets a boolean value from the context.
     *
     * @param path the path to the value
     * @return Optional containing the boolean value, or empty if not found or not convertible
     */
    public Optional<Boolean> getBoolean(String path) {
        return getValue(path).flatMap(value -> {
            if (value instanceof Boolean) {
                return Optional.of((Boolean) value);
            }
            if (value instanceof String) {
                try {
                    return Optional.of(Boolean.parseBoolean((String) value));
                } catch (Exception e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Gets a boolean value with a default fallback.
     *
     * @param path the path to the value
     * @param defaultValue the default value if not found
     * @return the boolean value or default
     */
    public Boolean getBoolean(String path, Boolean defaultValue) {
        return getBoolean(path).orElse(defaultValue);
    }
}