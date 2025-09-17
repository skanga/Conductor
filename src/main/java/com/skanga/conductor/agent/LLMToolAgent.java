package com.skanga.conductor.agent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.metrics.TimerContext;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.tools.Tool;
import com.skanga.conductor.tools.ToolInput;
import com.skanga.conductor.tools.ToolRegistry;
import com.skanga.conductor.tools.ToolResult;

import java.util.regex.Pattern;

/**
 * LLM-powered sub-agent that can execute tools based on LLM responses.
 * <p>
 * This agent integrates Large Language Model capabilities with tool execution,
 * allowing the LLM to request tool usage through structured JSON responses.
 * The agent processes user prompts by:
 * </p>
 * <ol>
 * <li>Sending the prompt to the configured LLM provider</li>
 * <li>Parsing the LLM response for JSON-encoded tool calls</li>
 * <li>Executing requested tools from the tool registry</li>
 * <li>Recording interactions in the memory store</li>
 * <li>Returning the final result with tool outputs</li>
 * </ol>
 * <p>
 * Tool calls are expected in JSON format:
 * {@code {"tool": "tool_name", "arguments": "argument_text"}}
 * </p>
 * <p>
 * If no tool calls are detected, the raw LLM output is returned.
 * This allows the agent to handle both tool-assisted and direct text responses.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe assuming the underlying LLM provider,
 * tool registry, and memory store are thread-safe.
 * </p>
 *
 * @since 1.0.0
 * @see SubAgent
 * @see LLMProvider
 * @see ToolRegistry
 * @see MemoryStore
 */
public class LLMToolAgent implements SubAgent {

    private final String name;
    private final String description;
    private final LLMProvider llm;
    private final ToolRegistry tools;
    private final MemoryStore memoryStore;
    private final Gson gson = new Gson();
    private final MetricsRegistry metricsRegistry;

    private static final Pattern TOOL_PATTERN = Pattern.compile("\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"arguments\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");

    /**
 * Constructs a new {@code LLMToolAgent}.
 *
 * @param name        the unique name of this agent; must be non‑null and non‑blank
 * @param description a short description of the agent’s purpose; must be non‑null and non‑blank
 * @param llm         the {@link LLMProvider} used to generate responses
 * @param tools       the {@link ToolRegistry} containing available tools
 * @param memoryStore the {@link MemoryStore} where interaction history is recorded
 */
public LLMToolAgent(String name, String description, LLMProvider llm, ToolRegistry tools, MemoryStore memoryStore) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or empty");
        }
        if (llm == null) {
            throw new IllegalArgumentException("llm cannot be null");
        }
        if (tools == null) {
            throw new IllegalArgumentException("tools cannot be null");
        }
        if (memoryStore == null) {
            throw new IllegalArgumentException("memoryStore cannot be null");
        }
        this.name = name;
        this.description = description;
        this.llm = llm;
        this.tools = tools;
        this.memoryStore = memoryStore;
        this.metricsRegistry = MetricsRegistry.getInstance();
    }

    /**
 * Returns the agent's name.
 *
 * @return the name supplied at construction
 */
@Override
public String name() {
        return name;
    }

    /**
 * Returns the agent's description.
 *
 * @return the description supplied at construction
 */
@Override
public String description() {
        return description;
    }

    @Override
    /**
 * Executes the agent with the given {@link TaskInput}.
 *
 * <p>The method validates the input, constructs a prompt for the underlying LLM, and attempts to
 * parse a JSON‑encoded {@code ToolCall}. If a tool is identified, it is looked up in the
 * {@link ToolRegistry} and executed. The interaction (tool call or raw LLM output) is recorded in the
 * {@link MemoryStore}. The result of the tool execution or the raw LLM response is wrapped in a
 * {@link TaskResult}.
 *
 * @param input the task input containing the user prompt and optional metadata
 * @return a {@link TaskResult} representing the outcome of the execution
 * @throws ConductorException.LLMProviderException if the LLM provider fails
 * @throws ConductorException.ToolExecutionException if a tool execution fails
 */
public TaskResult execute(TaskInput input) throws ConductorException.LLMProviderException, ConductorException.ToolExecutionException {
        if (input == null || input.prompt() == null || input.prompt().isBlank()) {
            throw new IllegalArgumentException("input and prompt cannot be null or empty");
        }

        // Start timing execution
        try (TimerContext timer = metricsRegistry.startTimer(
                "agent.execution.duration",
                java.util.Map.of("agent", name, "type", "llm_tool"))) {

            try {
                // Ask the LLM for a plan / response. The LLM may embed TOOL directives.
                String prompt = """
                        You are an agent that can call tools. If useful, emit a JSON object with the following format:
                        {"tool": "tool_name", "arguments": "arguments here"}
                        Only use tools when needed. Otherwise just answer.
                        User prompt:
                        %s
                        """.formatted(input.prompt());

                String llmOutput = llm.generate(prompt);

                try {
                    ToolCall toolCall = gson.fromJson(llmOutput, ToolCall.class);
                    if (toolCall != null && toolCall.tool != null && toolCall.arguments != null) {
                        Tool t = tools.get(toolCall.tool);
                        if (t == null) {
                            timer.recordWithSuccess(false);
                            return new TaskResult("[ERROR: unknown tool " + toolCall.tool + "]", false, null);
                        } else {
                            try {
                                ToolResult tr = t.run(new ToolInput(toolCall.arguments, null));
                                // persist tool call summary in memory
                                memoryStore.addMemory(name(), "TOOL_CALL " + toolCall.tool + " arg=" + (toolCall.arguments.length() > 120 ? toolCall.arguments.substring(0, 120) + "..." : toolCall.arguments));
                                timer.recordWithSuccess(tr.success());
                                return new TaskResult(tr.output(), tr.success(), tr.metadata());
                            } catch (Exception e) {
                                timer.recordWithSuccess(false);
                                metricsRegistry.recordError(name, e.getClass().getSimpleName(), e.getMessage());
                                throw new ConductorException.ToolExecutionException("Error executing tool: " + toolCall.tool, e);
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    // Not a JSON tool call, treat as a plain text response
                }

                // Also persist the LLM raw output
                try {
                    memoryStore.addMemory(name(), "LLM_OUTPUT: " + (llmOutput.length() > 300 ? llmOutput.substring(0, 300) + "..." : llmOutput));
                } catch (Exception e) {
                    // Don't let memory store issues fail the whole agent
                }

                timer.recordWithSuccess(true);
                return new TaskResult(llmOutput, true, null);

            } catch (Exception e) {
                timer.recordWithSuccess(false);
                metricsRegistry.recordError(name, e.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }
    }

    private static class ToolCall {
        public String tool;
        public String arguments;
    }
}
