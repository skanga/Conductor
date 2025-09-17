package com.skanga.conductor.agent;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;

/**
 * Core interface for sub-agents in the Conductor framework.
 * <p>
 * Sub-agents are autonomous components that can execute specific tasks within a larger workflow.
 * They maintain their own state, memory, and can interact with various tools and services.
 * All sub-agents must provide a unique name, description, and execution capability.
 * </p>
 * <p>
 * Implementations are expected to be thread-safe for concurrent execution scenarios.
 * </p>
 *
 * @since 1.0.0
 * @see LLMSubAgent
 * @see ToolUsingAgent
 */
public interface SubAgent {

    /**
     * Returns the unique identifier for this sub-agent.
     * <p>
     * The name is used for agent registration, memory persistence, and workflow coordination.
     * It must be unique within a given orchestrator context.
     * </p>
     *
     * @return the unique name of this sub-agent, never null or empty
     */
    String name();

    /**
     * Returns a human-readable description of this sub-agent's purpose and capabilities.
     * <p>
     * The description is used for documentation, debugging, and can be included in
     * LLM prompts to help with agent selection and coordination.
     * </p>
     *
     * @return a descriptive string explaining what this sub-agent does, never null
     */
    String description();

    /**
     * Executes the sub-agent's primary task with the given input.
     * <p>
     * This method performs the core work of the sub-agent, which may include:
     * </p>
     * <ul>
     * <li>Processing the input prompt or data</li>
     * <li>Interacting with external services (LLMs, APIs, tools)</li>
     * <li>Updating internal memory or state</li>
     * <li>Persisting results to storage</li>
     * </ul>
     * <p>
     * Implementations should be thread-safe as multiple executions may occur concurrently.
     * </p>
     *
     * @param input the task input containing prompt, metadata, and context information
     * @return the execution result containing output, success status, and optional metadata
     * @throws Exception if execution fails due to LLM errors, network issues, or other problems
     *
     * @see TaskInput
     * @see TaskResult
     */
    TaskResult execute(TaskInput input) throws ConductorException;
}
