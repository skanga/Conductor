package com.skanga.conductor.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles and renders templates with advanced features.
 * <p>
 * Supports:
 * </p>
 * <ul>
 * <li>Variable substitution: {{variable}}</li>
 * <li>Conditionals: {{#if condition}}content{{/if}}</li>
 * <li>Loops: {{#each items}}{{name}}{{/each}}</li>
 * <li>Filters: {{variable|upper|truncate:50}}</li>
 * </ul>
 * <p>
 * Thread Safety: CompiledTemplate instances are immutable and thread-safe.
 * </p>
 *
 * @since 2.0.0
 */
public class TemplateCompiler {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCompiler.class);

    // Patterns for different template constructs
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]*)\\}\\}");
    private static final Pattern CONDITIONAL_PATTERN = Pattern.compile("\\{\\{#if\\s+([^}]*)\\}\\}([\\s\\S]*?)\\{\\{/if\\}\\}");
    private static final Pattern LOOP_PATTERN = Pattern.compile("\\{\\{#each\\s+([^}]*)\\}\\}([\\s\\S]*?)\\{\\{/each\\}\\}");

    private final VariableResolver variableResolver;

    /**
     * Creates a new TemplateCompiler with the provided variable resolver.
     *
     * @param variableResolver the variable resolver to use
     */
    public TemplateCompiler(VariableResolver variableResolver) {
        this.variableResolver = variableResolver;
    }

    /**
     * Compiles a template for efficient rendering.
     *
     * @param template the template string
     * @return a compiled template
     */
    public CompiledTemplate compile(String template) {
        return new CompiledTemplate(template, variableResolver);
    }

    /**
     * Compiled template for better performance with caching.
     */
    public static class CompiledTemplate {
        private final String originalTemplate;
        private final VariableResolver variableResolver;

        CompiledTemplate(String template, VariableResolver variableResolver) {
            this.originalTemplate = template;
            this.variableResolver = variableResolver;
        }

        /**
         * Renders the compiled template with the given variables.
         *
         * @param variables the variables to substitute
         * @return the rendered template
         */
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

                Object iterable = variableResolver.getNestedValue(iterableExpr, variables);
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
                Object value = variableResolver.getVariableValue(variableExpression, variables);

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
            Object value = variableResolver.getNestedValue(condition, variables);

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
}
