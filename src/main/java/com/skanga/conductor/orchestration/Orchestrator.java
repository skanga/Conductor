package com.skanga.conductor.orchestration;

import com.skanga.conductor.agent.ConversationalAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Core orchestrator for managing sub-agent execution and workflow coordination.
 * <p>
 * The Orchestrator serves as the central coordinator for sub-agent interactions,
 * providing capabilities for:
 * </p>
 * <ul>
 * <li>Explicit agent execution by name</li>
 * <li>Dynamic creation of ad-hoc agents</li>
 * <li>Agent registry management and lookup</li>
 * <li>Shared memory store coordination</li>
 * </ul>
 * <p>
 * This class maintains references to the agent registry and memory store,
 * ensuring consistent state management across all agent operations. It provides
 * both explicit agent calls (when you know the specific agent to use) and
 * implicit agent creation (for dynamic workflow scenarios).
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying registry
 * and memory store implementations are thread-safe.
 * </p>
 *
 * @since 1.0.0
 * @see SubAgent
 * @see SubAgentRegistry
 * @see MemoryStore
 * @see PlannerOrchestrator
 */
public class Orchestrator {

    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

    private final SubAgentRegistry registry;
    private final MemoryStore memoryStore;

    /**
     * Creates a new orchestrator with the specified registry and memory store.
     *
     * @param registry the sub-agent registry for agent lookup and management
     * @param memoryStore the shared memory store for agent persistence
     * @throws IllegalArgumentException if registry or memoryStore is null
     */
    public Orchestrator(SubAgentRegistry registry, MemoryStore memoryStore) {
        if (registry == null) {
            throw new IllegalArgumentException("registry cannot be null");
        }
        if (memoryStore == null) {
            throw new IllegalArgumentException("memoryStore cannot be null");
        }
        this.registry = registry;
        this.memoryStore = memoryStore;
    }

    /**
     * Executes a task using a specific agent identified by name.
     * <p>
     * This method looks up the agent in the registry and delegates task
     * execution to the found agent. This is useful when you know exactly
     * which agent should handle a particular type of task.
     * </p>
     *
     * @param agentName the unique name of the agent to execute
     * @param input the execution input containing content and context
     * @return the execution result from the agent
     * @throws IllegalArgumentException if agentName is null/empty, input is null, or agent not found
     * @throws ConductorException if the agent execution fails
     */
    public ExecutionResult callExplicit(String agentName, ExecutionInput input) throws ConductorException {
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("agent name cannot be null or empty");
        }
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        SubAgent agent = registry.get(agentName);
        if (agent == null) throw new IllegalArgumentException("No agent: " + agentName);
        return agent.execute(input);
    }

    /**
     * Creates a new ad-hoc agent with the given parameters.
     * <p>
     * This method creates a temporary agent that exists only within the current
     * JVM session. The agent is stateful and maintains memory during its lifetime
     * but is not registered in the global agent registry. This is useful for
     * one-off tasks or dynamic workflow scenarios.
     * </p>
     *
     * @param nameHint a suggested name for the agent (may be modified for uniqueness)
     * @param description a description of the agent's purpose
     * @param provider the LLM provider for the agent to use
     * @param promptTemplate the template for formatting prompts
     * @return a new LLM sub-agent ready for execution
     * @throws IllegalArgumentException if any parameter is null or empty
     * @throws SQLException if memory store initialization fails
     */
    public SubAgent createImplicitAgent(String nameHint, String description, LLMProvider provider, String promptTemplate) throws SQLException {
        if (nameHint == null || nameHint.isBlank()) {
            throw new IllegalArgumentException("nameHint cannot be null or empty");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or empty");
        }
        if (provider == null) {
            throw new IllegalArgumentException("provider cannot be null");
        }
        if (promptTemplate == null || promptTemplate.isBlank()) {
            throw new IllegalArgumentException("promptTemplate cannot be null or empty");
        }
        String name = nameHint + "-" + UUID.randomUUID();
        return new ConversationalAgent(name, description, provider, promptTemplate, memoryStore);
    }

    /**
     * Gets the shared memory store used by this orchestrator.
     * <p>
     * This method provides access to the shared memory store so that external
     * components (like AgentFactory) can create agents that share the same
     * memory instance. This ensures consistency across all agents in a workflow.
     * </p>
     *
     * @return the shared memory store instance
     * @since 1.0.1
     */
    public MemoryStore getMemoryStore() {
        return memoryStore;
    }

    /**
     * Gets the agent registry used by this orchestrator.
     * <p>
     * This method provides access to the agent registry for external components
     * that need to interact with registered agents or register new ones.
     * </p>
     *
     * @return the agent registry instance
     * @since 1.0.1
     */
    public SubAgentRegistry getRegistry() {
        return registry;
    }
}
