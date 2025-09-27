package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents an agent definition loaded from YAML configuration.
 * This defines how to create and configure an AI agent.
 */
public class AgentDefinition {

    private String type = "llm";
    private String role;
    private String provider;
    private String model;

    @JsonProperty("prompt_template")
    private String promptTemplate;

    @JsonProperty("context_window")
    private Integer contextWindow;

    private Map<String, Object> parameters;

    // Default constructor for Jackson
    public AgentDefinition() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public Integer getContextWindow() {
        return contextWindow;
    }

    public void setContextWindow(Integer contextWindow) {
        this.contextWindow = contextWindow;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /**
     * Gets a parameter value with a default fallback.
     */
    public Object getParameter(String key, Object defaultValue) {
        if (parameters == null) {
            return defaultValue;
        }
        return parameters.getOrDefault(key, defaultValue);
    }

    /**
     * Gets a string parameter with a default fallback.
     */
    public String getStringParameter(String key, String defaultValue) {
        Object value = getParameter(key, defaultValue);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Gets an integer parameter with a default fallback.
     */
    public Integer getIntegerParameter(String key, Integer defaultValue) {
        Object value = getParameter(key, defaultValue);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Validates that this agent definition is complete and valid.
     */
    public void validate() throws IllegalArgumentException {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent role is required");
        }

        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Agent type is required for agent: " + role);
        }

        if ("llm".equals(type)) {
            if (promptTemplate == null || promptTemplate.trim().isEmpty()) {
                throw new IllegalArgumentException("LLM agent '" + role + "' must have a prompt template");
            }
        }
    }

    @Override
    public String toString() {
        return "AgentDefinition{" +
               "type='" + type + '\'' +
               ", role='" + role + '\'' +
               ", provider='" + provider + '\'' +
               ", model='" + model + '\'' +
               ", promptTemplate='" + promptTemplate + '\'' +
               '}';
    }
}