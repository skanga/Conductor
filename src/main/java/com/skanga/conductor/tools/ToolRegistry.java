package com.skanga.conductor.tools;

import com.skanga.conductor.agent.ConversationalAgent;
import com.skanga.conductor.utils.ValidationUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for managing tool instances.
 * <p>
 * This registry provides a centralized location for storing and retrieving
 * tool instances by their unique names. It uses a {@link ConcurrentHashMap}
 * for thread-safe concurrent access, allowing multiple threads to register
 * and lookup tools simultaneously without external synchronization.
 * </p>
 * <p>
 * Tools are stored using their {@link Tool#toolName()} as the registry key.
 * The registry maintains tool instances in memory and does not provide
 * persistence across application restarts. Tools must be re-registered
 * when the application starts.
 * </p>
 * <p>
 * Thread Safety: This class is fully thread-safe for concurrent access.
 * </p>
 *
 * @since 1.0.0
 * @see Tool
 * @see ConversationalAgent
 */
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * Registers a tool in the registry.
     * <p>
     * The tool is stored using its {@link Tool#toolName()} as the key.
     * If a tool with the same name already exists, it will be replaced.
     * </p>
     *
     * @param tool the tool to register, must not be null
     * @throws IllegalArgumentException if tool is null or tool name is null/blank
     */
    public void register(Tool tool) {
        ValidationUtils.requireNonNull(tool, "tool");
        String toolName = tool.toolName();
        ValidationUtils.requireNonBlank(toolName, "tool name");
        tools.put(toolName, tool);
    }

    /**
     * Retrieves a tool by its unique name.
     *
     * @param name the unique name of the tool to retrieve
     * @return the tool with the specified name, or null if not found
     * @throws IllegalArgumentException if name is null or blank
     */
    public Tool get(String name) {
        ValidationUtils.requireNonBlank(name, "tool name");
        return tools.get(name);
    }

    /**
     * Returns a set of all available tool names.
     *
     * @return set of tool names currently registered
     */
    public Set<String> getAvailableTools() {
        return tools.keySet();
    }
}
