package com.skanga.conductor.agent;

import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.utils.ValidationUtils;

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
     * The agent is stored using its {@link SubAgent#agentName()} as the key.
     * If an agent with the same name already exists, it will be replaced.
     * </p>
     *
     * @param agent the sub-agent to register, must not be null
     * @throws IllegalArgumentException if agent is null or agent name is null/blank
     */
    public void register(SubAgent agent) {
        ValidationUtils.requireNonNull(agent, "agent");
        String agentName = agent.agentName();
        ValidationUtils.requireNonBlank(agentName, "agent name");
        agents.put(agentName, agent);
    }

    /**
     * Retrieves a sub-agent by its unique name.
     *
     * @param name the unique name of the agent to retrieve
     * @return the sub-agent with the specified name, or null if not found
     * @throws IllegalArgumentException if name is null or blank
     */
    public SubAgent get(String name) {
        ValidationUtils.requireNonBlank(name, "agent name");
        return agents.get(name);
    }

    /**
     * Removes a sub-agent from the registry.
     * <p>
     * This method should be called when an agent is no longer needed to prevent
     * memory leaks from accumulating agent instances over time.
     * </p>
     *
     * @param name the unique name of the agent to remove
     * @return the removed sub-agent, or null if not found
     * @throws IllegalArgumentException if name is null or blank
     */
    public SubAgent remove(String name) {
        ValidationUtils.requireNonBlank(name, "agent name");
        return agents.remove(name);
    }

    /**
     * Removes all sub-agents from the registry.
     * <p>
     * This method should be called during cleanup to prevent memory leaks.
     * All registered agents will be removed from the registry.
     * </p>
     */
    public void clear() {
        agents.clear();
    }

    /**
     * Returns the number of registered agents.
     *
     * @return the current number of agents in the registry
     */
    public int size() {
        return agents.size();
    }

    /**
     * Checks if the registry is empty.
     *
     * @return true if no agents are registered, false otherwise
     */
    public boolean isEmpty() {
        return agents.isEmpty();
    }
}