package com.skanga.conductor.workflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles variable substitution in configuration files.
 * Supports environment variables, system properties, built-in variables, and iteration contexts.
 */
public class VariableSubstitution {

    private static final Logger logger = LoggerFactory.getLogger(VariableSubstitution.class);

    // Pattern to match ${VAR_NAME} or ${VAR_NAME:-default_value}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::[-]([^}]*))?\\}");

    private final Map<String, String> builtInVariables;
    private Map<String, Object> runtimeContext;

    public VariableSubstitution() {
        this.builtInVariables = createBuiltInVariables();
        this.runtimeContext = new HashMap<>();
    }

    /**
     * Creates a new instance with an initial runtime context.
     */
    public VariableSubstitution(Map<String, Object> initialContext) {
        this.builtInVariables = createBuiltInVariables();
        this.runtimeContext = initialContext != null ? new HashMap<>(initialContext) : new HashMap<>();
    }

    /**
     * Creates built-in variables that are always available.
     */
    private Map<String, String> createBuiltInVariables() {
        Map<String, String> variables = new HashMap<>();

        // Timestamp variables
        LocalDateTime now = LocalDateTime.now();
        variables.put("current_timestamp", now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        variables.put("timestamp", now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        variables.put("date", now.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        variables.put("time", now.format(DateTimeFormatter.ofPattern("HHmmss")));

        // UUID variable
        variables.put("uuid", UUID.randomUUID().toString());

        // System information
        variables.put("user_name", System.getProperty("user.name", "unknown"));
        variables.put("java_version", System.getProperty("java.version", "unknown"));

        // CI/CD variables
        variables.put("CI", System.getenv().getOrDefault("CI", "false"));

        return variables;
    }

    /**
     * Substitutes variables in a workflow definition.
     */
    public WorkflowDefinition substituteWorkflowVariables(WorkflowDefinition workflow) {
        if (workflow == null) {
            return null;
        }

        // Create a copy to avoid modifying the original
        WorkflowDefinition result = workflow;

        // Substitute in settings
        if (result.getSettings() != null) {
            WorkflowDefinition.WorkflowSettings settings = result.getSettings();
            if (settings.getOutputDir() != null) {
                settings.setOutputDir(substitute(settings.getOutputDir()));
            }
        }

        // Substitute in variables
        if (result.getVariables() != null) {
            Map<String, Object> substitutedVariables = new HashMap<>();
            for (Map.Entry<String, Object> entry : result.getVariables().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    substitutedVariables.put(entry.getKey(), substitute((String) value));
                } else {
                    substitutedVariables.put(entry.getKey(), value);
                }
            }
            result.setVariables(substitutedVariables);
        }

        // Substitute in stage outputs
        if (result.getStages() != null) {
            for (WorkflowStage stage : result.getStages()) {
                if (stage.getOutputs() != null) {
                    for (int i = 0; i < stage.getOutputs().size(); i++) {
                        String output = stage.getOutputs().get(i);
                        stage.getOutputs().set(i, substitute(output));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Substitutes variables in an agent configuration collection.
     */
    public AgentConfigCollection substituteAgentVariables(AgentConfigCollection agentConfig) {
        if (agentConfig == null) {
            return null;
        }

        // Substitute in agent definitions
        if (agentConfig.getAgents() != null) {
            for (AgentDefinition agent : agentConfig.getAgents().values()) {
                if (agent.getProvider() != null) {
                    agent.setProvider(substitute(agent.getProvider()));
                }
                if (agent.getModel() != null) {
                    agent.setModel(substitute(agent.getModel()));
                }
            }
        }

        // Substitute in prompt templates
        if (agentConfig.getPromptTemplates() != null) {
            for (AgentConfigCollection.PromptTemplate template : agentConfig.getPromptTemplates().values()) {
                if (template.getSystem() != null) {
                    template.setSystem(substitute(template.getSystem()));
                }
                if (template.getUser() != null) {
                    template.setUser(substitute(template.getUser()));
                }
                if (template.getAssistant() != null) {
                    template.setAssistant(substitute(template.getAssistant()));
                }
            }
        }

        return agentConfig;
    }

    /**
     * Substitutes variables in a workflow context.
     */
    public WorkflowContext substituteContextVariables(WorkflowContext context) {
        if (context == null) {
            return null;
        }

        Object data = context.getData();
        Object substitutedData = substituteInObject(data);
        return new WorkflowContext(substitutedData);
    }

    /**
     * Recursively substitutes variables in an object structure.
     */
    private Object substituteInObject(Object obj) {
        if (obj instanceof String) {
            return substitute((String) obj);
        } else if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() instanceof String ? (String) entry.getKey() : String.valueOf(entry.getKey());
                result.put(key, substituteInObject(entry.getValue()));
            }
            return result;
        } else if (obj instanceof java.util.List<?>) {
            java.util.List<?> list = (java.util.List<?>) obj;
            java.util.List<Object> result = new java.util.ArrayList<>();
            for (Object item : list) {
                result.add(substituteInObject(item));
            }
            return result;
        } else {
            return obj;
        }
    }

    /**
     * Substitutes variables in a single string.
     * Supports ${VAR_NAME} and ${VAR_NAME:-default_value} patterns.
     */
    public String substitute(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String defaultValue = matcher.group(2);

            String value = resolveVariable(variableName, defaultValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Resolves a variable value from various sources.
     * Priority: Runtime context > Built-in variables > System properties > Environment variables > Default value
     */
    private String resolveVariable(String variableName, String defaultValue) {
        // Check runtime context first (for iteration variables, stage results, etc.)
        if (runtimeContext != null) {
            Object contextValue = resolveFromContext(variableName, runtimeContext);
            if (contextValue != null) {
                String value = convertToString(contextValue);
                logger.debug("Resolved runtime context variable '{}' = '{}'", variableName, value);
                return value;
            }
        }

        // Check built-in variables
        if (builtInVariables.containsKey(variableName)) {
            String value = builtInVariables.get(variableName);
            logger.debug("Resolved built-in variable '{}' = '{}'", variableName, value);
            return value;
        }

        // Check system properties
        String systemProperty = System.getProperty(variableName);
        if (systemProperty != null) {
            logger.debug("Resolved system property '{}' = '{}'", variableName, systemProperty);
            return systemProperty;
        }

        // Check environment variables
        String envValue = System.getenv(variableName);
        if (envValue != null) {
            logger.debug("Resolved environment variable '{}' = '{}'", variableName, envValue);
            return envValue;
        }

        // Use default value if provided
        if (defaultValue != null) {
            logger.debug("Using default value for '{}' = '{}'", variableName, defaultValue);
            return defaultValue;
        }

        // Return the original pattern if no resolution found
        String original = "${" + variableName + "}";
        logger.warn("Unable to resolve variable '{}', keeping original: {}", variableName, original);
        return original;
    }

    /**
     * Resolves a variable from the runtime context using dot notation (e.g., "stage.result.title").
     */
    private Object resolveFromContext(String variableName, Map<String, Object> context) {
        if (variableName.contains(".")) {
            // Handle dot notation (e.g., "toc_generation.chapters")
            String[] parts = variableName.split("\\.");
            Object current = context;

            for (String part : parts) {
                if (current instanceof Map<?, ?>) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
                if (current == null) {
                    return null;
                }
            }

            return current;
        } else {
            // Simple variable lookup
            return context.get(variableName);
        }
    }

    /**
     * Converts various object types to string representation.
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            // For complex objects, try to convert to JSON-like string
            return value.toString();
        }
    }

    /**
     * Adds a custom variable that can be used in substitutions.
     */
    public void addVariable(String name, String value) {
        builtInVariables.put(name, value);
        logger.debug("Added custom variable '{}' = '{}'", name, value);
    }

    /**
     * Gets the current value of a variable.
     */
    public String getVariable(String name) {
        return resolveVariable(name, null);
    }

    /**
     * Sets the runtime context for variable resolution.
     * This is typically used during workflow execution to provide stage results and iteration variables.
     */
    public void setRuntimeContext(Map<String, Object> context) {
        this.runtimeContext = context != null ? new HashMap<>(context) : new HashMap<>();
        logger.debug("Updated runtime context with {} variables", this.runtimeContext.size());
    }

    /**
     * Updates the runtime context with additional variables.
     */
    public void updateRuntimeContext(Map<String, Object> additionalContext) {
        if (additionalContext != null) {
            if (this.runtimeContext == null) {
                this.runtimeContext = new HashMap<>();
            }
            this.runtimeContext.putAll(additionalContext);
            logger.debug("Updated runtime context with {} additional variables", additionalContext.size());
        }
    }

    /**
     * Sets a specific runtime variable.
     */
    public void setRuntimeVariable(String name, Object value) {
        if (this.runtimeContext == null) {
            this.runtimeContext = new HashMap<>();
        }
        this.runtimeContext.put(name, value);
        logger.debug("Set runtime variable '{}' = '{}'", name, value);
    }

    /**
     * Gets the current runtime context.
     */
    public Map<String, Object> getRuntimeContext() {
        return this.runtimeContext != null ? new HashMap<>(this.runtimeContext) : new HashMap<>();
    }

    /**
     * Clears the runtime context.
     */
    public void clearRuntimeContext() {
        if (this.runtimeContext != null) {
            this.runtimeContext.clear();
        }
        logger.debug("Cleared runtime context");
    }

    /**
     * Creates a copy of this VariableSubstitution with the same configuration but a new runtime context.
     */
    public VariableSubstitution withContext(Map<String, Object> context) {
        VariableSubstitution copy = new VariableSubstitution(context);
        // Copy built-in variables
        copy.builtInVariables.putAll(this.builtInVariables);
        return copy;
    }

    /**
     * Substitutes variables in a string using a specific runtime context without modifying this instance.
     */
    public String substituteWithContext(String input, Map<String, Object> context) {
        VariableSubstitution tempSubstitution = withContext(context);
        return tempSubstitution.substitute(input);
    }

    /**
     * Substitutes variables in an object using a specific runtime context.
     */
    public Object substituteObjectWithContext(Object obj, Map<String, Object> context) {
        VariableSubstitution tempSubstitution = withContext(context);
        return tempSubstitution.substituteInObject(obj);
    }
}