package com.skanga.conductor.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles variable resolution and nested value access for template rendering.
 * <p>
 * Supports:
 * </p>
 * <ul>
 * <li>Simple variable access: {{name}}</li>
 * <li>Nested access: {{context.user.name}}</li>
 * <li>Filter chain parsing: {{name|upper|truncate:50}}</li>
 * </ul>
 * <p>
 * Thread Safety: This class is stateless and thread-safe.
 * </p>
 *
 * @since 2.0.0
 */
public class VariableResolver {

    private static final Logger logger = LoggerFactory.getLogger(VariableResolver.class);
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]*)\\}\\}");
    private static final Pattern FILTER_PATTERN = Pattern.compile("([^|]+)(?:\\|([^|]+(?:\\|[^|]+)*))?");

    private final TemplateFilters filters;

    /**
     * Creates a new VariableResolver with the provided filter processor.
     *
     * @param filters the filter processor to use
     */
    public VariableResolver(TemplateFilters filters) {
        this.filters = filters;
    }

    /**
     * Gets variable value with support for nested access and filters.
     *
     * @param variableExpression the variable expression (may include filters)
     * @param variables the variable map
     * @return the resolved value, or null if not found
     */
    public Object getVariableValue(String variableExpression, Map<String, Object> variables) {
        Matcher filterMatcher = FILTER_PATTERN.matcher(variableExpression);
        if (!filterMatcher.matches()) {
            return getNestedValue(variableExpression.trim(), variables);
        }

        String varName = filterMatcher.group(1).trim();
        String filterChain = filterMatcher.group(2);

        Object value = getNestedValue(varName, variables);

        if (filterChain != null && !filterChain.isEmpty()) {
            value = filters.applyFilters(value, filterChain);
        }

        return value;
    }

    /**
     * Gets nested variable value (e.g., context.user.name).
     *
     * @param varName the variable name (may include dots for nesting)
     * @param variables the variable map
     * @return the nested value, or null if not found
     */
    public Object getNestedValue(String varName, Map<String, Object> variables) {
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
}
