package com.skanga.conductor.agent;

import com.skanga.conductor.orchestration.Orchestrator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for managing sub-agent instances.
 * <p>
 * This registry provides a centralized location for storing and retrieving
 * sub-agent instances by their unique names. It uses a {@link ConcurrentHashMap}
 * for thread-safe concurrent access, allowing multiple threads to register
 * and lookup agents simultaneously without external synchronization.
 * </p>
 * <p>
 * The registry maintains agent instances in memory and does not provide
 * persistence across application restarts. Agents must be re-registered
 * when the application starts.
 * </p>
 * <p>
 * Thread Safety: This class is fully thread-safe for concurrent access.
 * </p>
 *
 * @since 1.0.0
 * @see SubAgent
 * @see Orchestrator
 */
public class SubAgentRegistry {
    private final Map<String, SubAgent> agents = new ConcurrentHashMap<>();

    /**
     * Registers a sub-agent in the registry.
     * <p>
     * The agent is stored using its {@link SubAgent#name()} as the key.
     * If an agent with the same name already exists, it will be replaced.
     * </p>
     *
     * @param agent the sub-agent to register, must not be null
     * @throws NullPointerException if agent is null
     */
    public void register(SubAgent agent) {
        agents.put(agent.name(), agent);
    }

    /**
     * Retrieves a sub-agent by its unique name.
     *
     * @param name the unique name of the agent to retrieve
     * @return the sub-agent with the specified name, or null if not found
     */
    public SubAgent get(String name) {
        return agents.get(name);
    }
}