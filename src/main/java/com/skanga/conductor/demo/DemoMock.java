package com.skanga.conductor.demo;

import com.skanga.conductor.agent.LLMSubAgent;
import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Demonstrates basic Conductor framework functionality with memory persistence.
 * <p>
 * This demo shows:
 * </p>
 * <ul>
 * <li>Multi-agent workflow orchestration using BookWorkflow</li>
 * <li>Memory persistence and rehydration across application restarts</li>
 * <li>Explicit vs implicit agent creation patterns</li>
 * <li>Configuration-driven demo behavior</li>
 * </ul>
 * <p>
 * The demo runs in two phases:
 * </p>
 * <ol>
 * <li><strong>First Run</strong>: Creates a book workflow, generates chapters, and persists memory</li>
 * <li><strong>Second Run</strong>: Simulates application restart, creates explicit agent, and demonstrates memory rehydration</li>
 * </ol>
 * <p>
 * Configuration can be customized via demo.properties or environment variables.
 * </p>
 *
 * @since 1.0.0
 * @see BookWorkflow
 * @see DemoConfig
 * @see MemoryStore
 */
public class DemoMock {

    private static final Logger logger = LoggerFactory.getLogger(DemoMock.class);
    private static final DemoConfig demoConfig = DemoConfig.getInstance();

    /**
     * Main entry point for the demo application.
     *
     * @param args command line arguments; if provided, will be used as the book summary
     */
    public static void main(String[] args) {
        try {
            runDemo(args);
        } catch (Exception e) {
            logger.error("Demo failed with unexpected error: ", e);
            System.exit(1);
        }
    }

    /**
     * Runs the complete demo workflow with configuration-driven behavior.
     *
     * @param args command line arguments for custom book summary
     */
    private static void runDemo(String[] args) {
        // Use configured provider type
        LLMProvider provider = createLLMProvider();

        // Use custom prompt if provided, otherwise use configured default
        String summary = getBookSummary(args);

        if (demoConfig.isVerboseLoggingEnabled()) {
            logger.info("=== Demo Configuration ===");
            logger.info("Provider type: {}", demoConfig.getDemoProviderType());
            logger.info("Target words: {}", demoConfig.getBookTargetWords());
            logger.info("Database isolation: {}", demoConfig.isDatabaseIsolationEnabled());
            logger.info("Memory limit: {}", demoConfig.getDemoMemoryLimit());
            logger.info("Failure simulation: {}", demoConfig.isFailureSimulationEnabled());
            logger.info("Summary length: {} characters", summary.length());
        }

        // Generate unique workflow ID for this demo run
        String workflowId = demoConfig.getWorkflowId();

        // 1) First run: create isolated database and MemoryStore
        try (DemoDatabaseManager dbManager = new DemoDatabaseManager(workflowId);
             MemoryStore store = dbManager.createIsolatedMemoryStore()) {
            SubAgentRegistry registry = new SubAgentRegistry();
            Orchestrator orchestrator = new Orchestrator(registry, store);
            BookWorkflow workflow = new BookWorkflow(orchestrator);

            logger.info("=== FIRST RUN: generating chapters and persisting memory ===");
            logger.info("Workflow ID: {}", workflowId);
            logger.info("Isolated Database: {}", dbManager.getDatabasePath());

            List<Chapter> chapters = workflow.produceChapters(summary, provider);

            logger.info("Generated {} chapters. Persisted subagent outputs to isolated database.", chapters.size());

            if (demoConfig.isVerboseLoggingEnabled()) {
                chapters.forEach(chapter -> {
                    String content = truncateOutput(chapter.content());
                    logger.info("Chapter: {} ({}...)", chapter.title(), content);
                });

                // Show database statistics
                var stats = dbManager.getStatistics();
                logger.info("Database stats: {}", stats);
            }
        } catch (ConductorException e) {
            logger.error("An error occurred during the first run: ", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during the first run: ", e);
        }

        // 2) Simulate process restart: create new isolated database for rehydration demo
        String rehydrationWorkflowId = workflowId + "-rehydration";
        try (DemoDatabaseManager dbManager2 = new DemoDatabaseManager(rehydrationWorkflowId);
             MemoryStore store2 = dbManager2.createIsolatedMemoryStore()) {
            SubAgentRegistry registry2 = new SubAgentRegistry();
            Orchestrator orchestrator2 = new Orchestrator(registry2, store2);

            // Create an explicit agent with a configurable name to show rehydration
            String explicitAgentName = "explicit-outline-agent-" + workflowId;
            LLMSubAgent explicit = new LLMSubAgent(
                    explicitAgentName,
                    "Outline generator (explicit) - demonstrates rehydration",
                    provider,
                    "Produce an outline from a summary.",
                    store2
            );

            // Register and call - first let's check any rehydrated memory snapshot
            registry2.register(explicit);

            logger.info("=== SECOND RUN (rehydration) ===");
            logger.info("Rehydration Database: {}", dbManager2.getDatabasePath());
            logger.info("Memory snapshot for {} (before any new call):", explicitAgentName);

            int snapshotSize = demoConfig.getMemorySnapshotSize();
            List<String> snap = explicit.getMemorySnapshot(snapshotSize);
            if (snap.isEmpty()) {
                logger.info("  (no persisted memory yet for this explicit agent)");
            } else {
                for (int i = 0; i < snap.size(); i++) {
                    String memoryEntry = truncateOutput(snap.get(i));
                    logger.info("  [{}] {}", i, memoryEntry);
                }
            }

            // Use configured or custom outline prompt
            String outlinePrompt = demoConfig.getCustomDemoPrompt("outline");
            if (outlinePrompt == null) {
                outlinePrompt = "Produce an outline for a short book about distributed systems.";
            }

            // Use the explicit agent to generate something
            TaskResult res = explicit.execute(new TaskInput(outlinePrompt, null));
            String truncatedOutput = truncateOutput(res.output());
            logger.info("Explicit agent produced output (and persisted it): {}", truncatedOutput);

            // Show snapshot again (in-memory updated)
            if (demoConfig.isVerboseLoggingEnabled()) {
                logger.info("Memory snapshot after call:");
                explicit.getMemorySnapshot(snapshotSize).forEach(m -> {
                    String truncated = truncateOutput(m);
                    logger.info("  -> {}", truncated);
                });
            }
        } catch (ConductorException e) {
            logger.error("An error occurred during the second run: ", e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during the second run: ", e);
        }

        logger.info("Demo complete. Each demo run used isolated databases for proper separation.");
    }

    /**
     * Creates an LLM provider based on the configured provider type.
     *
     * @return configured LLM provider instance
     */
    private static LLMProvider createLLMProvider() {
        String providerType = demoConfig.getDemoProviderType();
        return switch (providerType.toLowerCase()) {
            case "mock" -> new MockLLMProvider("demo");
            // case "openai" -> new OpenAiLLMProvider(); // Would be added when available
            default -> {
                logger.warn("Unknown provider type '{}', using mock provider", providerType);
                yield new MockLLMProvider("demo");
            }
        };
    }


    /**
     * Gets the book summary from command line args or uses a configured default.
     *
     * @param args command line arguments
     * @return book summary to use for generation
     */
    private static String getBookSummary(String[] args) {
        if (args.length > 0) {
            return String.join(" ", args);
        }

        // Check for custom demo prompt
        String customPrompt = demoConfig.getCustomDemoPrompt("technical.book");
        if (customPrompt != null) {
            return customPrompt;
        }

        // Use default summary
        return """
                Chapter 1: Foundations
                Explain events and streams and why they matter.

                Chapter 2: Architecture
                Microservices, schema evolution, reliability.

                Chapter 3: Observability
                Metrics, tracing, testing.
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
