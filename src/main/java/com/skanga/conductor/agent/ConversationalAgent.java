package com.skanga.conductor.agent;

import com.skanga.conductor.utils.JsonUtils;
import com.skanga.conductor.utils.ValidationUtils;
import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.config.MemoryConfig;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.metrics.TimerContext;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.tools.Tool;
import com.skanga.conductor.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Universal agent implementation that has the following capabilities:
 * <ul>
 * <li>Generate pure text responses using an LLM (LLMSubAgent functionality)</li>
 * <li>Execute tools based on intelligent LLM decisions (LLMToolAgent functionality)</li>
 * <li>Maintain persistent conversational memory with thread-safe access</li>
 * <li>Support both tool-assisted and direct text responses</li>
 * <li>Provide comprehensive metrics and error handling</li>
 * </ul>
 * <p>
 * The agent operates in multiple modes based on the LLM's response:
 * </p>
 * <ol>
 * <li><b>Tool Mode:</b> LLM returns JSON tool calls which are executed automatically</li>
 * <li><b>Text Mode:</b> LLM returns plain text responses directly to the user</li>
 * </ol>
 * <p>
 * Tool calls are expected in JSON format:
 * {@code {"tool": "tool_name", "arguments": "argument_text"}}
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent execution.
 * Multiple threads can safely execute tasks and access memory simultaneously.
 * </p>
 *
 * @since 2.0.0
 * @see SubAgent
 * @see LLMProvider
 * @see ToolRegistry
 * @see MemoryStore
 */
public class ConversationalAgent implements SubAgent {

    private static final Logger logger = LoggerFactory.getLogger(ConversationalAgent.class);

    private final String agentName;
    private final String agentDescription;
    private final LLMProvider llmProvider;
    private final String promptTemplate;
    private final ToolRegistry toolRegistry;
    private final List<String> agentMemory = new CopyOnWriteArrayList<>();
    private final MemoryStore memoryStore;
    private final ReadWriteLock memoryLock = new ReentrantReadWriteLock();
    private final MetricsRegistry metricsRegistry;

    /**
     * Creates a new agent with comprehensive capabilities.
     * <p>
     * The agent is automatically rehydrated from the memory store if previous
     * conversations exist. Memory is loaded up to the configured limit and
     * becomes immediately available for context in subsequent interactions.
     * </p>
     * <p>
     * <strong>Usage Example - Creating an Agent with Tools:</strong>
     * </p>
     * <pre>{@code
     * // Set up tool registry with file operations
     * ToolRegistry toolRegistry = new ToolRegistry();
     * toolRegistry.register(new FileReadTool());
     * toolRegistry.register(new WebSearchTool());
     *
     * // Create a research agent with tool access
     * ConversationalAgent researcher = new ConversationalAgent(
     *     "research-agent",
     *     "AI researcher capable of reading files and searching the web",
     *     new OpenAiLLMProvider(),
     *     "You are a research assistant. Use available tools to gather information: {{input}}",
     *     toolRegistry,
     *     memoryStore
     * );
     *
     * // Execute research task
     * ExecutionResult result = researcher.execute(
     *     new ExecutionInput("Research the latest trends in AI safety", null)
     * );
     * }</pre>
     *
     * @param agentName the unique identifier for this agent, used as the database key
     * @param agentDescription a human-readable description of the agent's purpose
     * @param llmProvider the LLM provider to use for text generation and tool decisions
     * @param promptTemplate the template for formatting prompts to the LLM (can be null for tool-only mode)
     * @param toolRegistry the registry of available tools (can be null for text-only mode)
     * @param memoryStore the shared memory store for persistence across agents
     * @throws SQLException if database error occurs during memory rehydration
     * @throws IllegalArgumentException if required parameters are null or empty
     */
    public ConversationalAgent(String agentName, String agentDescription, LLMProvider llmProvider,
                               String promptTemplate, ToolRegistry toolRegistry, MemoryStore memoryStore) throws SQLException {
        ValidationUtils.requireNonBlank(agentName, "agent name");
        ValidationUtils.requireNonBlank(agentDescription, "agent description");
        ValidationUtils.requireNonNull(llmProvider, "agent llm provider");
        ValidationUtils.requireNonNull(memoryStore, "agent memory store");

        this.agentName = agentName;
        this.agentDescription = agentDescription;
        this.llmProvider = llmProvider;
        this.promptTemplate = promptTemplate;
        this.toolRegistry = toolRegistry;
        this.memoryStore = memoryStore;
        this.metricsRegistry = MetricsRegistry.getInstance();
        rehydrateMemory();
    }

    /**
     * Convenience constructor for text-only agents (no tools).
     * <p>
     * <strong>Usage Example - Simple Text Agent:</strong>
     * </p>
     * <pre>{@code
     * // Create a simple text-based agent for writing tasks
     * ConversationalAgent writer = new ConversationalAgent(
     *     "content-writer",
     *     "Professional content writer specializing in marketing copy",
     *     new AnthropicLLMProvider(),
     *     "Write engaging marketing content for: {{input}}",
     *     memoryStore
     * );
     *
     * // Generate marketing content
     * ExecutionResult result = writer.execute(
     *     new ExecutionInput("eco-friendly water bottles", null)
     * );
     *
     * if (result.success()) {
     *     System.out.println("Marketing copy: " + result.output());
     * }
     * }</pre>
     */
    public ConversationalAgent(String agentName, String agentDescription, LLMProvider llmProvider,
                               String promptTemplate, MemoryStore memoryStore) throws SQLException {
        this(agentName, agentDescription, llmProvider, promptTemplate, null, memoryStore);
    }

    /**
     * Constructor with preloaded memory to avoid N+1 query patterns.
     * This constructor should be used when creating multiple agents in bulk
     * to optimize database performance.
     */
    public ConversationalAgent(String agentName, String agentDescription, LLMProvider llmProvider,
                               String promptTemplate, MemoryStore memoryStore, List<String> preloadedMemory) {
        ValidationUtils.requireNonBlank(agentName, "agent name");
        ValidationUtils.requireNonBlank(agentDescription, "agent description");
        ValidationUtils.requireNonNull(llmProvider, "agent llm provider");
        ValidationUtils.requireNonNull(memoryStore, "agent memory store");

        this.agentName = agentName;
        this.agentDescription = agentDescription;
        this.llmProvider = llmProvider;
        this.promptTemplate = promptTemplate;
        this.toolRegistry = null; // No tools for this constructor
        this.memoryStore = memoryStore;
        this.metricsRegistry = MetricsRegistry.getInstance();

        // Use preloaded memory instead of querying database
        if (preloadedMemory != null) {
            this.agentMemory.addAll(preloadedMemory);
        }
    }

    /**
     * Convenience constructor for tool-only agents (no custom prompt template).
     */
    public ConversationalAgent(String agentName, String agentDescription, LLMProvider llmProvider,
                               ToolRegistry toolRegistry, MemoryStore memoryStore) throws SQLException {
        this(agentName, agentDescription, llmProvider, null, toolRegistry, memoryStore);
    }

    private void rehydrateMemory() throws SQLException {
        List<String> persistedMemory = memoryStore.loadMemory(agentName);
        memoryLock.writeLock().lock();
        try {
            agentMemory.clear();
            agentMemory.addAll(persistedMemory);
        } finally {
            memoryLock.writeLock().unlock();
        }
    }

    @Override
    public String agentName() {
        return agentName;
    }

    @Override
    public String agentDescription() {
        return agentDescription;
    }

    @Override
    public ExecutionResult execute(ExecutionInput input) throws ConductorException.LLMProviderException, ConductorException.ToolExecutionException {
        if (input == null || input.content() == null || input.content().isBlank()) {
            throw new IllegalArgumentException("execution input and content cannot be null or empty");
        }

        // Start timing execution
        try (TimerContext timerContext = metricsRegistry.startTimer(
                "agent.execution.duration",
                Map.of("agent", agentName, "type", "unified"))) {

            boolean success = false;
            try {
                // Build the prompt with memory context and tool availability
                String fullPrompt = buildPrompt(input.content());

                // Get response from LLM
                String llmOutput = llmProvider.generate(fullPrompt);

                // Try to parse as tool call first
                if (toolRegistry != null) {
                    try {
                        ToolCall toolCall = JsonUtils.fromJson(llmOutput, ToolCall.class);
                        if (toolCall != null && toolCall.tool != null && toolCall.arguments != null) {
                            ExecutionResult toolResult = executeToolCall(toolCall);
                            success = toolResult.success();
                            timerContext.recordWithSuccess(success);
                            return toolResult;
                        }
                    } catch (com.skanga.conductor.exception.JsonProcessingException e) {
                        // Not a JSON tool call, treat as plain text response
                    }
                }

                // Handle as pure text response
                success = true;
                persistToMemory(llmOutput, "LLM_OUTPUT");
                timerContext.recordWithSuccess(success);
                return new ExecutionResult(success, llmOutput, null);

            } catch (Exception e) {
                timerContext.recordWithSuccess(false);
                metricsRegistry.recordError(agentName, e.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }
    }

    private String buildPrompt(String userPrompt) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("System: ").append(agentDescription).append("\n\n");

        // Add memory context if available - optimize by checking size before acquiring lock
        if (!agentMemory.isEmpty()) {
            memoryLock.readLock().lock();
            try {
                // Double-check after acquiring lock (memory might have been cleared)
                if (!agentMemory.isEmpty()) {
                    promptBuilder.append("Memory (most recent first):\n");
                    MemoryConfig memoryConfig = ApplicationConfig.getInstance().getMemoryConfig();
                    int memoryLimit = memoryConfig.getDefaultMemoryLimit();
                    int memoryStart = Math.max(0, agentMemory.size() - memoryLimit);
                    for (int i = memoryStart; i < agentMemory.size(); i++) {
                        promptBuilder.append("- ").append(agentMemory.get(i)).append("\n");
                    }
                    promptBuilder.append("\n");
                }
            } finally {
                memoryLock.readLock().unlock();
            }
        }

        // Add tool availability information
        if (toolRegistry != null && !toolRegistry.getAvailableTools().isEmpty()) {
            promptBuilder.append("Available Tools: ").append(String.join(", ", toolRegistry.getAvailableTools())).append("\n");
            promptBuilder.append("You can call tools using JSON format: {\"tool\": \"tool_name\", \"arguments\": \"arguments here\"}\n");
            promptBuilder.append("Only use tools when helpful. Otherwise just answer directly.\n\n");
        }

        promptBuilder.append("User Input:\n").append(userPrompt).append("\n\n");

        // Add custom prompt template if provided
        if (promptTemplate != null && !promptTemplate.isBlank()) {
            promptBuilder.append("Prompt Template:\n").append(promptTemplate).append("\n\n");
        }

        promptBuilder.append("Produce the best output now.\n");
        return promptBuilder.toString();
    }

    private ExecutionResult executeToolCall(ToolCall toolCall) throws ConductorException.ToolExecutionException {
        Tool tool = toolRegistry.get(toolCall.tool);
        if (tool == null) {
            return new ExecutionResult(false, "[ERROR: unknown tool " + toolCall.tool + "]", null);
        }

        try {
            ExecutionResult toolResult = tool.runTool(new ExecutionInput(toolCall.arguments, null));

            // Persist tool call summary in memory
            String memorySummary = "TOOL_CALL " + toolCall.tool + " arg=" +
                (toolCall.arguments.length() > 120 ? toolCall.arguments.substring(0, 120) + "..." : toolCall.arguments);
            persistToMemory(memorySummary, "TOOL_CALL");

            return new ExecutionResult(toolResult.success(), toolResult.output(), toolResult.metadata());
        } catch (Exception e) {
            metricsRegistry.recordError(agentName, e.getClass().getSimpleName(), e.getMessage());
            throw new ConductorException.ToolExecutionException("Error executing tool: " + toolCall.tool, e);
        }
    }

    private void persistToMemory(String content, String type) {
        try {
            String memoryEntry = type + ": " + (content.length() > 300 ? content.substring(0, 300) + "..." : content);
            memoryStore.addMemory(agentName, memoryEntry);

            // Update in-memory storage
            memoryLock.writeLock().lock();
            try {
                agentMemory.add(memoryEntry);
            } finally {
                memoryLock.writeLock().unlock();
            }
        } catch (SQLException e) {
            logger.warn("Failed to persist memory for agent {}: {}", agentName, e.getMessage());
        }
    }

    /**
     * Expose memory for inspection (limited entries for performance).
     */
    public List<String> getMemorySnapshot(int memoryLimit) {
        memoryLock.readLock().lock();
        try {
            int start = Math.max(0, agentMemory.size() - memoryLimit);
            return new ArrayList<>(agentMemory.subList(start, agentMemory.size()));
        } finally {
            memoryLock.readLock().unlock();
        }
    }

    private static class ToolCall {
        public String tool;
        public String arguments;
    }
}