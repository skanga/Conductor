package com.skanga.conductor.orchestration;

import com.skanga.conductor.agent.SubAgent;

/**
 * Immutable data transfer object representing input for sub-agent task execution.
 * <p>
 * This record encapsulates the information needed to execute a task within a sub-agent,
 * including the primary prompt/instruction and optional metadata for additional context.
 * </p>
 * <p>
 * The prompt field contains the main text instruction or query that the agent should
 * process. The metadata field can contain any additional contextual information that
 * might be useful for task execution, such as execution parameters, workflow context,
 * or custom configuration data.
 * </p>
 * <p>
 * As a record, this class is immutable and provides automatic implementations of
 * equals(), hashCode(), toString(), and accessor methods.
 * </p>
 *
 * @param prompt the main text instruction or query for the agent to process
 * @param metadata optional additional context data, can be null
 *
 * @since 1.0.0
 * @see SubAgent#execute(TaskInput)
 * @see TaskResult
 */
public record TaskInput(String prompt, Object metadata) {
}