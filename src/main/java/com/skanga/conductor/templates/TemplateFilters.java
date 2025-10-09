package com.skanga.conductor.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles filter application for template variable values.
 * <p>
 * Supports:
 * </p>
 * <ul>
 * <li>upper - Convert to uppercase</li>
 * <li>lower - Convert to lowercase</li>
 * <li>trim - Trim whitespace</li>
 * <li>truncate:N - Truncate to N characters</li>
 * <li>default:'value' - Use default if null</li>
 * </ul>
 * <p>
 * Thread Safety: This class is stateless and thread-safe.
 * </p>
 *
 * @since 2.0.0
 */
public class TemplateFilters {

    private static final Logger logger = LoggerFactory.getLogger(TemplateFilters.class);

    /**
     * Applies filter chain to a value.
     *
     * @param value the value to filter
     * @param filterChain the filter chain (e.g., "upper|truncate:50")
     * @return the filtered value
     */
    public Object applyFilters(Object value, String filterChain) {
        String[] filters = filterChain.split("\\|");
        Object result = value;

        for (String filter : filters) {
            result = applyFilter(result, filter.trim());
        }

        return result;
    }

    /**
     * Applies a single filter to a value.
     *
     * @param value the value to filter
     * @param filter the filter name (may include parameters)
     * @return the filtered value
     */
    public Object applyFilter(Object value, String filter) {
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
}
