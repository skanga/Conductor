package com.skanga.conductor.agent;

import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.provider.LLMProvider;

import java.sql.SQLException;

/**
 * Interface for creating agents.
 * <p>
 * This interface breaks the circular dependency between workflow and orchestration packages
 * by providing a minimal contract for agent creation without depending on the full Orchestrator class.
 * </p>
 *
 * @since 1.1.0
 */
public interface AgentCreator {

    /**
     * Creates an implicit agent for workflow tasks.
     *
     * @param agentId unique identifier for the agent
     * @param role descriptive role/purpose of the agent
     * @param llmProvider LLM provider to use
     * @param promptTemplate initial prompt template
     * @return the created agent
     * @throws SQLException if agent creation fails
     */
    SubAgent createImplicitAgent(String agentId, String role, LLMProvider llmProvider, String promptTemplate)
        throws SQLException;

    /**
     * Returns the memory store associated with this agent creator.
     *
     * @return the memory store
     */
    MemoryStore getMemoryStore();
}
