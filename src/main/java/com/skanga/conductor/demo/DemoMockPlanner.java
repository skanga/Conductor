package com.skanga.conductor.demo;

import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.PlannerOrchestrator;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.provider.MockPlannerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Demonstrates the Conductor framework's planning capabilities with task decomposition.
 * <p>
 * This demo showcases:
 * </p>
 * <ul>
 * <li>Automatic task decomposition using a planner LLM</li>
 * <li>Sequential execution of planned sub-tasks</li>
 * <li>Integration between planning and execution providers</li>
 * <li>Configuration-driven demo behavior</li>
 * </ul>
 * <p>
 * The demo uses mock providers by default but can be configured to use
 * real LLM providers for production scenarios.
 * </p>
 *
 * @since 1.0.0
 * @see PlannerOrchestrator
 * @see DemoConfig
 * @see MockPlannerProvider
 */
public class DemoMockPlanner {

    private static final Logger logger = LoggerFactory.getLogger(DemoMockPlanner.class);
    private static final DemoConfig demoConfig = DemoConfig.getInstance();

    /**
     * Main entry point for the planner demo application.
     *
     * @param args command line arguments; if provided, will be used as the user request
     */
    public static void main(String[] args) {
        try {
            runPlannerDemo(args);
        } catch (Exception e) {
            logger.error("Planner demo failed with unexpected error: ", e);
            System.exit(1);
        }
    }

    /**
     * Runs the planner demo with configuration-driven behavior.
     *
     * @param args command line arguments for custom user request
     */
    private static void runPlannerDemo(String[] args) {
        // Generate unique workflow ID
        String workflowId = demoConfig.getWorkflowId();

        try (DemoDatabaseManager dbManager = new DemoDatabaseManager(workflowId);
             MemoryStore store = dbManager.createIsolatedMemoryStore()) {
            SubAgentRegistry registry = new SubAgentRegistry();
            PlannerOrchestrator orchestrator = new PlannerOrchestrator(registry, store);

            // Create providers based on configuration
            LLMProvider plannerProvider = createPlannerProvider();
            LLMProvider workerProvider = createWorkerProvider();

            String userRequest = getUserRequest(args);

            if (demoConfig.isVerboseLoggingEnabled()) {
                logger.info("=== Planner Demo Configuration ===");
                logger.info("Workflow ID: {}", workflowId);
                logger.info("Planner provider: {}", demoConfig.getDemoPlannerProviderType());
                logger.info("Worker provider: {}", demoConfig.getDemoProviderType());
                logger.info("Database isolation: {}", demoConfig.isDatabaseIsolationEnabled());
                logger.info("Isolated Database: {}", dbManager.getDatabasePath());
                logger.info("Request length: {} characters", userRequest.length());
            }

            logger.info("Planner decomposing the user request and executing subagents...");
            List<TaskResult> results = orchestrator.planAndExecute(workflowId, userRequest, plannerProvider, workerProvider, store);

            logger.info("=== Planner Results ===");
            for (int i = 0; i < results.size(); i++) {
                logger.info("Step {} output:", (i + 1));
                String output = truncateOutput(results.get(i).output());
                logger.info("{}", output);
                logger.info("----");
            }

            if (demoConfig.isVerboseLoggingEnabled()) {
                logger.info("Total steps executed: {}", results.size());
                logger.info("Workflow completed successfully");

                // Show database statistics
                var stats = dbManager.getStatistics();
                logger.info("Database stats: {}", stats);
            }

        } catch (ConductorException e) {
            logger.error("An error occurred during planner demo: ", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during planner demo: ", e);
        }
    }

    /**
     * Creates a planner provider based on configuration.
     *
     * @return configured planner provider instance
     */
    private static LLMProvider createPlannerProvider() {
        String providerType = demoConfig.getDemoPlannerProviderType();
        return switch (providerType.toLowerCase()) {
            case "mock" -> new MockPlannerProvider();
            // case "openai" -> new OpenAiLLMProvider(); // Would be added when available
            default -> {
                logger.warn("Unknown planner provider type '{}', using mock provider", providerType);
                yield new MockPlannerProvider();
            }
        };
    }

    /**
     * Creates a worker provider based on configuration.
     *
     * @return configured worker provider instance
     */
    private static LLMProvider createWorkerProvider() {
        String providerType = demoConfig.getDemoProviderType();
        return switch (providerType.toLowerCase()) {
            case "mock" -> new MockLLMProvider("worker");
            // case "openai" -> new OpenAiLLMProvider(); // Would be added when available
            default -> {
                logger.warn("Unknown worker provider type '{}', using mock provider", providerType);
                yield new MockLLMProvider("worker");
            }
        };
    }


    /**
     * Gets the user request from command line args or uses a configured default.
     *
     * @param args command line arguments
     * @return user request to use for planning
     */
    private static String getUserRequest(String[] args) {
        if (args.length > 0) {
            return String.join(" ", args);
        }

        // Check for custom demo prompt
        String customPrompt = demoConfig.getCustomDemoPrompt("technical.book");
        if (customPrompt != null) {
            return customPrompt;
        }

        // Use default request
        return """
                Write a short technical book (3 chapters) about building resilient microservices.
                Include practical examples and a chapter on testing and observability.
                """;
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
