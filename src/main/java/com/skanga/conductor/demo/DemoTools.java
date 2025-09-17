package com.skanga.conductor.demo;

import com.skanga.conductor.agent.LLMToolAgent;
import com.skanga.conductor.agent.ToolUsingAgent;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.tools.*;
import com.skanga.conductor.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Demonstrates the Conductor framework's tool integration capabilities.
 * <p>
 * This demo showcases:
 * </p>
 * <ul>
 * <li>Tool registration and management through ToolRegistry</li>
 * <li>Programmatic tool execution with ToolUsingAgent</li>
 * <li>LLM-driven tool selection with LLMToolAgent</li>
 * <li>Configuration-driven tool selection and behavior</li>
 * <li>Memory persistence for tool-using agents</li>
 * </ul>
 * <p>
 * The demo creates two types of agents:
 * </p>
 * <ol>
 * <li><strong>Programmatic Agent</strong>: Executes tools based on explicit directives</li>
 * <li><strong>LLM Agent</strong>: Uses LLM to decide when and how to use tools</li>
 * </ol>
 * <p>
 * Tool selection and configuration can be customized via demo.properties.
 * </p>
 *
 * @since 1.0.0
 * @see ToolRegistry
 * @see DemoConfig
 * @see ToolUsingAgent
 * @see LLMToolAgent
 */
public class DemoTools {

    private static final Logger logger = LoggerFactory.getLogger(DemoTools.class);
    private static final DemoConfig demoConfig = DemoConfig.getInstance();

    /**
     * Main entry point for the tools demo application.
     *
     * @param args command line arguments (currently unused)
     * @throws Exception if demo execution fails
     */
    public static void main(String[] args) throws Exception {
        try {
            runToolsDemo(args);
        } catch (Exception e) {
            logger.error("Tools demo failed with unexpected error: ", e);
            System.exit(1);
        }
    }

    /**
     * Runs the tools demo with configuration-driven behavior.
     *
     * @param args command line arguments (currently unused)
     * @throws Exception if demo execution fails
     */
    private static void runToolsDemo(String[] args) throws Exception {
        // Generate unique workflow ID for this demo run
        String workflowId = demoConfig.getWorkflowId();

        try (DemoDatabaseManager dbManager = new DemoDatabaseManager(workflowId);
             MemoryStore store = dbManager.createIsolatedMemoryStore()) {
            // Create tool registry and register configured tools
            ToolRegistry tools = createAndConfigureToolRegistry();

            // Generate unique agent names for this demo run
            String progAgentName = "prog-agent-" + workflowId;
            String llmAgentName = "llm-agent-" + workflowId;

            if (demoConfig.isVerboseLoggingEnabled()) {
                logger.info("=== Tools Demo Configuration ===");
                logger.info("Workflow ID: {}", workflowId);
                logger.info("Registered tools: {}", demoConfig.getDemoToolClasses());
                logger.info("Provider type: {}", demoConfig.getDemoProviderType());
                logger.info("Database isolation: {}", demoConfig.isDatabaseIsolationEnabled());
                logger.info("Isolated Database: {}", dbManager.getDatabasePath());
                logger.info("Memory limit: {}", demoConfig.getDemoMemoryLimit());
            }

            // Create and run programmatic agent
            runProgrammaticAgentDemo(tools, store, progAgentName);

            // Create and run LLM-assisted agent
            runLLMAssistedAgentDemo(tools, store, llmAgentName);

            // Show persisted memory snapshots
            showMemorySnapshots(store, progAgentName, llmAgentName);

            if (demoConfig.isVerboseLoggingEnabled()) {
                // Show database statistics
                var stats = dbManager.getStatistics();
                logger.info("Database stats: {}", stats);
            }
        }
    }

    /**
     * Demonstrates programmatic tool usage with ToolUsingAgent.
     * <p>
     * This method creates a ToolUsingAgent that executes tools based on explicit
     * directives in the input text. The agent parses tool commands and executes
     * them sequentially, demonstrating deterministic tool usage.
     * </p>
     *
     * @param tools the tool registry containing available tools
     * @param store the memory store for persisting agent interactions
     * @param agentName the unique name for the agent instance
     */
    private static void runProgrammaticAgentDemo(ToolRegistry tools, MemoryStore store, String agentName) {
        try {
            ToolUsingAgent programAgent = new ToolUsingAgent(agentName, "Calls tools programmatically", tools, store);

            logger.info("=== Programmatic agent demo ===");

            // Use configured or custom task input
            String taskInput = demoConfig.getCustomDemoPrompt("programmatic.tools");
            if (taskInput == null) {
                taskInput = "research: distributed tracing\nreadfile:example.txt\nrun: echo Hello from shell\nspeak: Hello world demo";
            }

            TaskResult result = programAgent.execute(new TaskInput(taskInput, null));
            String truncatedOutput = truncateOutput(result.output());
            logger.info("Programmatic agent output: {}", truncatedOutput);

        } catch (Exception e) {
            logger.error("Error in programmatic agent demo: ", e);
        }
    }

    /**
     * Demonstrates LLM-driven tool selection with LLMToolAgent.
     * <p>
     * This method creates an LLMToolAgent that uses an LLM to decide when and how
     * to use tools. The agent analyzes the user request and autonomously determines
     * which tools to invoke, demonstrating intelligent tool selection.
     * </p>
     *
     * @param tools the tool registry containing available tools
     * @param store the memory store for persisting agent interactions
     * @param agentName the unique name for the agent instance
     */
    private static void runLLMAssistedAgentDemo(ToolRegistry tools, MemoryStore store, String agentName) {
        try {
            LLMProvider llmProvider = createLLMProvider();
            LLMToolAgent llmAgent = new LLMToolAgent(agentName, "LLM decides when to call tools", llmProvider, tools, store);

            logger.info("=== LLM-assisted agent demo ===");

            // Use configured or custom task input
            String taskInput = demoConfig.getCustomDemoPrompt("llm.tools");
            if (taskInput == null) {
                taskInput = "Summarize the topic 'observability' and, if helpful, search the web for 'observability best practices' and include the results.";
            }

            TaskResult result = llmAgent.execute(new TaskInput(taskInput, null));
            String truncatedOutput = truncateOutput(result.output());
            logger.info("LLM-assisted agent output: {}", truncatedOutput);

        } catch (Exception e) {
            logger.error("Error in LLM-assisted agent demo: ", e);
        }
    }

    /**
     * Shows memory snapshots for both programmatic and LLM-assisted agents.
     * <p>
     * This method retrieves and displays the memory entries for both types of
     * agents, demonstrating how agent interactions are persisted and can be
     * reviewed for debugging or analysis purposes.
     * </p>
     *
     * @param store the memory store containing agent memories
     * @param progAgentName the name of the programmatic agent
     * @param llmAgentName the name of the LLM-assisted agent
     */
    private static void showMemorySnapshots(MemoryStore store, String progAgentName, String llmAgentName) {
        try {
            logger.info("=== Persisted memory snapshots ===");

            int maxLength = demoConfig.getMaxOutputLength();
            int snapshotSize = demoConfig.getMemorySnapshotSize();

            // Show programmatic agent memory
            List<String> memProg = store.loadMemory(progAgentName, snapshotSize);
            logger.info("{} memory entries: {}", progAgentName, memProg.size());
            memProg.forEach(entry -> {
                String truncated = entry.length() > maxLength ? entry.substring(0, maxLength) + "..." : entry;
                logger.info(" - {}", truncated);
            });

            // Show LLM agent memory
            List<String> memLlm = store.loadMemory(llmAgentName, snapshotSize);
            logger.info("{} memory entries: {}", llmAgentName, memLlm.size());
            memLlm.forEach(entry -> {
                String truncated = entry.length() > maxLength ? entry.substring(0, maxLength) + "..." : entry;
                logger.info(" - {}", truncated);
            });

        } catch (Exception e) {
            logger.error("Error showing memory snapshots: ", e);
        }
    }

    /**
     * Creates and configures the tool registry based on configuration.
     *
     * @return configured ToolRegistry with registered tools
     */
    private static ToolRegistry createAndConfigureToolRegistry() {
        ToolRegistry tools = new ToolRegistry();
        List<String> toolClasses = demoConfig.getDemoToolClasses();

        for (String toolClassName : toolClasses) {
            try {
                Tool tool = createToolInstance(toolClassName.trim());
                if (tool != null) {
                    tools.register(tool);
                    if (demoConfig.isVerboseLoggingEnabled()) {
                        logger.info("Registered tool: {} ({})", tool.name(), tool.description());
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to register tool '{}': {}", toolClassName, e.getMessage());
            }
        }

        return tools;
    }

    /**
     * Creates a tool instance based on class name.
     * <p>
     * This method provides a factory pattern for creating tool instances from
     * configuration. It supports the standard tool classes provided by the
     * Conductor framework and logs warnings for unknown tool types.
     * </p>
     *
     * @param toolClassName the simple class name of the tool (e.g., "FileReadTool")
     * @return tool instance or null if the tool class is unknown or creation fails
     */
    private static Tool createToolInstance(String toolClassName) {
        return switch (toolClassName) {
            case "FileReadTool" -> new FileReadTool();
            case "MockWebSearchTool" -> new MockWebSearchTool();
            case "CodeRunnerTool" -> new CodeRunnerTool();
            case "SimpleAudioTool" -> new SimpleAudioTool();
            default -> {
                logger.warn("Unknown tool class: {}", toolClassName);
                yield null;
            }
        };
    }

    /**
     * Creates an LLM provider based on configuration.
     *
     * @return configured LLM provider instance
     */
    private static LLMProvider createLLMProvider() {
        String providerType = demoConfig.getDemoProviderType();
        return switch (providerType.toLowerCase()) {
            case "mock" -> new MockLLMProvider("planner");
            // case "openai" -> new OpenAiLLMProvider(); // Would be added when available
            default -> {
                logger.warn("Unknown provider type '{}', using mock provider", providerType);
                yield new MockLLMProvider("planner");
            }
        };
    }


    /**
     * Truncates output to configured maximum length for cleaner demo logs.
     *
     * @param output the output string to truncate
     * @return truncated output with ellipsis if needed
     */
    private static String truncateOutput(String output) {
        if (output == null) {
            return "null";
        }
        int maxLength = demoConfig.getMaxOutputLength();
        return output.length() > maxLength ?
            output.substring(0, maxLength) + "..." :
            output;
    }
}