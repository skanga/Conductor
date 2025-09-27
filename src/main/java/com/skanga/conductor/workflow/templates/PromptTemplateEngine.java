package com.skanga.conductor.workflow.templates;

import com.skanga.conductor.workflow.config.AgentConfigCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced engine for rendering prompt templates with variable substitution.
 * Supports {{variable}} syntax for template variables, conditional logic,
 * loops, filters, and template caching for improved performance.
 *
 * <p>Template Features:</p>
 * <ul>
 * <li>Variables: {{variable}}</li>
 * <li>Conditionals: {{#if condition}}content{{/if}}</li>
 * <li>Loops: {{#each items}}{{name}}{{/each}}</li>
 * <li>Filters: {{variable|upper|truncate:50}}</li>
 * <li>Default values: {{variable|default:'fallback'}}</li>
 * <li>Nested access: {{context.user.name}}</li>
 * </ul>
 *
 * <p>Thread Safety: This class is thread-safe for concurrent template rendering.</p>
 */
public class PromptTemplateEngine {

    /**
     * Default constructor with caching enabled and max cache size of 100.
     */
    public PromptTemplateEngine() {
        this(true, 100);
    }

    /**
     * Constructor with configurable caching.
     *
     * @param cachingEnabled whether to enable template caching
     * @param maxCacheSize maximum number of templates to cache
     */
    public PromptTemplateEngine(boolean cachingEnabled, int maxCacheSize) {
        this.cachingEnabled = cachingEnabled;
        this.maxCacheSize = maxCacheSize;
    }

    private static final Logger logger = LoggerFactory.getLogger(PromptTemplateEngine.class);

    // Patterns for different template constructs
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]*)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#if\\s+([^}]*)\\}\\}([\\s\\S]*?)\\{\\{/if\\}\\}");
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]*)\\}\\}([\\s\\S]*?)\\{\\{/each\\}\\}");
    private static final Pattern FILTER_PATTERN = Pattern.compile("([^|]+)(?:\\|([^|]+(?:\\|[^|]+)*))?");

    // Template cache for performance optimization
    private final Map<String, CompiledTemplate> templateCache = new ConcurrentHashMap<>();
    private final boolean cachingEnabled;
    private final int maxCacheSize;

    /**
     * Renders a prompt template with the given variables.
     *
     * @param template the prompt template to render
     * @param variables the variables to substitute in the template
     * @return the rendered prompt string
     */
    public String renderPrompt(AgentConfigCollection.PromptTemplate template, Map<String, Object> variables) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        StringBuilder result = new StringBuilder();

        // Add system message if present
        if (template.hasSystem()) {
            String systemPrompt = substitute(template.getSystem(), variables);
            result.append("System: ").append(systemPrompt);
        }

        // Add user message if present
        if (template.hasUser()) {
            if (result.length() > 0) {
                result.append("\n\n");
            }
            String userPrompt = substitute(template.getUser(), variables);
            result.append("Human: ").append(userPrompt);
        }

        // Add assistant message if present (for few-shot examples)
        if (template.hasAssistant()) {
            if (result.length() > 0) {
                result.append("\n\n");
            }
            String assistantPrompt = substitute(template.getAssistant(), variables);
            result.append("Assistant: ").append(assistantPrompt);
        }

        String renderedPrompt = result.toString();
        logger.debug("Rendered prompt template with {} variables: {} characters",
                    variables.size(), renderedPrompt.length());

        return renderedPrompt;
    }

    /**
     * Renders a simple string template with variables and advanced features.
     *
     * @param template the template string
     * @param variables the variables to substitute
     * @return the rendered string
     */
    public String renderString(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        return renderAdvancedTemplate(template, variables);
    }

    /**
     * Renders a template with advanced features including conditionals, loops, and filters.
     */
    private String renderAdvancedTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        // Use cached compiled template if available
        CompiledTemplate compiled = getCompiledTemplate(template);
        return compiled.render(variables != null ? variables : Collections.emptyMap());
    }

    /**
     * Gets or creates a compiled template from cache.
     */
    private CompiledTemplate getCompiledTemplate(String template) {
        if (!cachingEnabled) {
            return new CompiledTemplate(template);
        }

        return templateCache.computeIfAbsent(template, t -> {
            // Implement LRU eviction if cache is full
            if (templateCache.size() >= maxCacheSize) {
                String firstKey = templateCache.keySet().iterator().next();
                templateCache.remove(firstKey);
            }
            return new CompiledTemplate(t);
        });
    }

    /**
     * Substitutes variables in a template string with basic {{variable}} syntax.
     * This method is kept for backward compatibility and internal use.
     */
    private String substitute(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        if (variables == null || variables.isEmpty()) {
            // Check if template has any variables that couldn't be substituted
            Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
            if (matcher.find()) {
                logger.warn("Template contains variables but no variable map provided: {}", template);
            }
            return template;
        }

        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = getVariableValue(variableName, variables);

            String replacement;
            if (value != null) {
                replacement = value.toString();
                logger.trace("Substituted variable '{}' with value: {}", variableName, replacement);
            } else {
                // Keep the original placeholder if variable is not found
                replacement = "{{" + variableName + "}}";
                logger.debug("Variable '{}' not found, keeping placeholder", variableName);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Gets variable value with support for nested access and filters.
     */
    private Object getVariableValue(String variableExpression, Map<String, Object> variables) {
        Matcher filterMatcher = FILTER_PATTERN.matcher(variableExpression);
        if (!filterMatcher.matches()) {
            return getNestedValue(variableExpression.trim(), variables);
        }

        String varName = filterMatcher.group(1).trim();
        String filterChain = filterMatcher.group(2);

        Object value = getNestedValue(varName, variables);

        if (filterChain != null && !filterChain.isEmpty()) {
            value = applyFilters(value, filterChain);
        }

        return value;
    }

    /**
     * Gets nested variable value (e.g., context.user.name).
     */
    private Object getNestedValue(String varName, Map<String, Object> variables) {
        if (!varName.contains(".")) {
            return variables.get(varName);
        }

        String[] parts = varName.split("\\.");
        Object current = variables.get(parts[0]);

        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                // Could add reflection support here for POJOs
                return null;
            }
        }

        return current;
    }

    /**
     * Applies filter chain to a value.
     */
    private Object applyFilters(Object value, String filterChain) {
        String[] filters = filterChain.split("\\|");
        Object result = value;

        for (String filter : filters) {
            result = applyFilter(result, filter.trim());
        }

        return result;
    }

    /**
     * Applies a single filter to a value.
     */
    private Object applyFilter(Object value, String filter) {
        if (filter.startsWith("default:")) {
            String defaultValue = filter.substring(8).replaceAll("^['\"]|['\"]$", "");
            return value != null ? value : defaultValue;
        }

        if (value == null) {
            return null;
        }

        String str = value.toString();

        switch (filter) {
            case "upper":
                return str.toUpperCase();
            case "lower":
                return str.toLowerCase();
            case "trim":
                return str.trim();
            default:
                if (filter.startsWith("truncate:")) {
                    try {
                        int length = Integer.parseInt(filter.substring(9));
                        return str.length() > length ? str.substring(0, length) + "..." : str;
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid truncate filter parameter: {}", filter);
                        return str;
                    }
                }
                logger.warn("Unknown filter: {}", filter);
                return value;
        }
    }

    /**
     * Extracts all variable names from a template string.
     *
     * @param template the template to analyze
     * @return array of variable names found in the template
     */
    public String[] extractVariableNames(String template) {
        if (template == null || template.isEmpty()) {
            return new String[0];
        }

        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        return matcher.results()
            .map(result -> result.group(1).trim())
            .distinct()
            .toArray(String[]::new);
    }

    /**
     * Validates that a template is syntactically correct.
     *
     * @param template the template to validate
     * @throws IllegalArgumentException if the template is invalid
     */
    public void validateTemplate(String template) throws IllegalArgumentException {
        if (template == null) {
            return; // null templates are allowed
        }

        // Check for balanced braces and detect invalid patterns
        int openBraces = 0;
        boolean foundSingleBrace = false;

        for (int i = 0; i < template.length(); i++) {
            char current = template.charAt(i);

            if (current == '{') {
                if (i + 1 < template.length() && template.charAt(i + 1) == '{') {
                    openBraces++;
                    i++; // Skip next character
                } else {
                    foundSingleBrace = true;
                }
            } else if (current == '}') {
                if (i + 1 < template.length() && template.charAt(i + 1) == '}') {
                    openBraces--;
                    i++; // Skip next character
                    if (openBraces < 0) {
                        throw new IllegalArgumentException("Template has unbalanced closing braces: " + template);
                    }
                } else {
                    foundSingleBrace = true;
                }
            }
        }

        if (openBraces > 0) {
            throw new IllegalArgumentException("Template has unbalanced opening braces: " + template);
        }

        if (foundSingleBrace) {
            throw new IllegalArgumentException("Template contains single braces (use {{}} for variables): " + template);
        }

        // Check for empty variable names
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            if (variableName.isEmpty()) {
                throw new IllegalArgumentException("Template contains empty variable name: " + template);
            }
        }
    }

    /**
     * Validates a complete prompt template.
     *
     * @param template the prompt template to validate
     * @throws IllegalArgumentException if the template is invalid
     */
    public void validatePromptTemplate(AgentConfigCollection.PromptTemplate template) throws IllegalArgumentException {
        if (template == null) {
            throw new IllegalArgumentException("Prompt template cannot be null");
        }

        validateTemplate(template.getSystem());
        validateTemplate(template.getUser());
        validateTemplate(template.getAssistant());

        if (!template.hasSystem() && !template.hasUser()) {
            throw new IllegalArgumentException("Prompt template must have at least system or user content");
        }
    }

    /**
     * Clears the template cache.
     */
    public void clearCache() {
        templateCache.clear();
    }

    /**
     * Gets cache statistics.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(templateCache.size(), maxCacheSize, cachingEnabled);
    }

    /**
     * Compiled template for better performance with caching.
     */
    private class CompiledTemplate {
        private final String originalTemplate;

        public CompiledTemplate(String template) {
            this.originalTemplate = template;
        }

        public String render(Map<String, Object> variables) {
            String result = originalTemplate;

            // Process conditionals first
            result = processConditionals(result, variables);

            // Process loops
            result = processLoops(result, variables);

            // Process variables (including filtered ones)
            result = processVariables(result, variables);

            return result;
        }

        private String processConditionals(String template, Map<String, Object> variables) {
            Matcher matcher = CONDITIONAL_PATTERN.matcher(template);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String condition = matcher.group(1).trim();
                String content = matcher.group(2);

                boolean conditionTrue = evaluateCondition(condition, variables);
                String replacement = conditionTrue ? content : "";

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(result);
            return result.toString();
        }

        private String processLoops(String template, Map<String, Object> variables) {
            Matcher matcher = LOOP_PATTERN.matcher(template);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String iterableExpr = matcher.group(1).trim();
                String loopContent = matcher.group(2);

                Object iterable = getNestedValue(iterableExpr, variables);
                String replacement = processLoop(iterable, loopContent, variables);

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(result);
            return result.toString();
        }

        private String processLoop(Object iterable, String loopContent, Map<String, Object> variables) {
            if (iterable == null) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            Collection<?> items;

            if (iterable instanceof Collection) {
                items = (Collection<?>) iterable;
            } else if (iterable instanceof Object[]) {
                items = Arrays.asList((Object[]) iterable);
            } else {
                logger.warn("Cannot iterate over non-collection object: {}", iterable.getClass());
                return "";
            }

            for (Object item : items) {
                Map<String, Object> loopVars = new HashMap<>(variables);
                loopVars.put("this", item);

                // If item is a map, add its entries to loop variables
                if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                        if (entry.getKey() instanceof String) {
                            loopVars.put((String) entry.getKey(), entry.getValue());
                        }
                    }
                }

                String processedContent = processVariables(loopContent, loopVars);
                result.append(processedContent);
            }

            return result.toString();
        }

        private String processVariables(String template, Map<String, Object> variables) {
            Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                String variableExpression = matcher.group(1).trim();
                Object value = getVariableValue(variableExpression, variables);

                String replacement;
                if (value != null) {
                    replacement = value.toString();
                } else {
                    replacement = "{{" + variableExpression + "}}";
                    logger.debug("Variable '{}' not found, keeping placeholder", variableExpression);
                }

                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }

            matcher.appendTail(result);
            return result.toString();
        }

        private boolean evaluateCondition(String condition, Map<String, Object> variables) {
            // Simple condition evaluation - can be extended
            Object value = getNestedValue(condition, variables);

            if (value == null) {
                return false;
            }

            if (value instanceof Boolean) {
                return (Boolean) value;
            }

            if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0.0;
            }

            if (value instanceof String) {
                return !((String) value).isEmpty();
            }

            if (value instanceof Collection) {
                return !((Collection<?>) value).isEmpty();
            }

            return true; // Non-null objects are truthy
        }
    }

    /**
     * Cache statistics holder.
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final boolean enabled;

        public CacheStats(int currentSize, int maxSize, boolean enabled) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.enabled = enabled;
        }

        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public boolean isEnabled() { return enabled; }
        public double getUsageRatio() { return enabled ? (double) currentSize / maxSize : 0.0; }

        @Override
        public String toString() {
            return String.format("CacheStats{enabled=%s, size=%d/%d, usage=%.1f%%}",
                enabled, currentSize, maxSize, getUsageRatio() * 100);
        }
    }
}