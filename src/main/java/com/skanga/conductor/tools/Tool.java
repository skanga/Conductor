package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;

/**
 * Interface for tools that can be used by agents in the Conductor framework.
 * <p>
 * Tools provide specific capabilities to agents, such as file operations,
 * web searches, code execution, or external API calls. They encapsulate
 * the complexity of external integrations and provide a consistent interface
 * for agents to interact with various services and resources.
 * </p>
 * <p>
 * Tools should be stateless and thread-safe, as they may be used concurrently
 * by multiple agents. All state should be passed through the input parameter
 * and returned in the result.
 * </p>
 *
 * @since 1.0.0
 * @see FileReadTool
 * @see CodeRunnerTool
 * @see TextToSpeechTool
 * @see WebSearchTool
 */
public interface Tool {

    /**
     * Returns the unique identifier for this tool.
     * <p>
     * The name is used for tool registration and selection by agents.
     * It should be descriptive and unique within a tool registry.
     * </p>
     *
     * @return the unique name of this tool, never null or empty
     */
    String toolName();

    /**
     * Returns a human-readable description of this tool's functionality.
     * <p>
     * The description explains what the tool does and may be used by
     * LLM agents to determine when and how to use the tool. It should
     * clearly describe the tool's purpose, inputs, and expected outputs.
     * </p>
     *
     * @return a descriptive string explaining what this tool does, never null
     */
    String toolDescription();

    /**
     * Executes the tool's functionality with the given input.
     * <p>
     * This method performs the tool's primary operation, which may include:
     * </p>
     * <ul>
     * <li>File system operations (reading, writing)</li>
     * <li>Network requests to external APIs</li>
     * <li>System command execution</li>
     * <li>Data processing and transformation</li>
     * <li>Integration with external services</li>
     * </ul>
     * <p>
     * Implementations should be thread-safe and handle errors gracefully,
     * returning appropriate error information in the result rather than
     * throwing exceptions when possible.
     * </p>
     *
     * @param input the execution input containing content, metadata, and parameters
     * @return the execution result containing success status, output, and optional metadata
     * @throws Exception if the tool execution fails due to system errors, invalid input,
     *                   network issues, or other unrecoverable problems
     *
     * @see ExecutionInput
     * @see ExecutionResult
     */
    ExecutionResult runTool(ExecutionInput input) throws Exception;
}
