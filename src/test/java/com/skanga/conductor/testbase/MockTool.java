package com.skanga.conductor.testbase;

import com.skanga.conductor.tools.Tool;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;

/**
 * Mock tool implementation for testing purposes.
 * <p>
 * This mock tool provides configurable behavior for testing tool-related functionality
 * without requiring real tool implementations or external dependencies.
 * </p>
 *
 * @since 2.0.0
 */
public class MockTool implements Tool {

    private final String name;
    private final String output;
    private final boolean shouldFail;
    private final String errorMessage;

    /**
     * Creates a mock tool that always succeeds with the given output.
     *
     * @param name the tool name
     * @param output the output to return
     */
    public MockTool(String name, String output) {
        this(name, output, false, null);
    }

    /**
     * Creates a mock tool with configurable success/failure behavior.
     *
     * @param name the tool name
     * @param output the output to return on success
     * @param shouldFail whether the tool should fail
     * @param errorMessage the error message to return on failure
     */
    public MockTool(String name, String output, boolean shouldFail, String errorMessage) {
        this.name = name;
        this.output = output;
        this.shouldFail = shouldFail;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toolName() {
        return name;
    }

    @Override
    public String toolDescription() {
        return "Mock tool for testing: " + name;
    }

    @Override
    public ExecutionResult runTool(ExecutionInput input) throws Exception {
        if (shouldFail) {
            return new ExecutionResult(false, errorMessage != null ? errorMessage : "Mock tool failure", null);
        }

        // Append input content to output if provided
        String result = output;
        if (input.content() != null && !input.content().trim().isEmpty()) {
            result = output + input.content();
        }

        return new ExecutionResult(true, result, null);
    }

    /**
     * Creates a mock tool that always fails with the given error message.
     *
     * @param name the tool name
     * @param errorMessage the error message
     * @return a failing mock tool
     */
    public static MockTool failing(String name, String errorMessage) {
        return new MockTool(name, "", true, errorMessage);
    }

    /**
     * Creates a mock tool that echoes its input.
     *
     * @param name the tool name
     * @return an echoing mock tool
     */
    public static MockTool echo(String name) {
        return new MockTool(name, "Echo: ");
    }

    /**
     * Creates a mock tool that returns a success message.
     *
     * @param name the tool name
     * @return a success mock tool
     */
    public static MockTool success(String name) {
        return new MockTool(name, "Operation successful");
    }
}