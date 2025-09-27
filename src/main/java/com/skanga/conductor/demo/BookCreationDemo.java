package com.skanga.conductor.demo;

import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.engine.DefaultWorkflowEngine.*;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.workflow.YamlWorkflowEngine;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.DemoMockLLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Book creation demo with two execution modes:
 *
 * --code : Programmatic workflow using WorkflowBuilder (default)
 * --yaml : YAML-configured declarative workflow
 */
public class BookCreationDemo {

    private static final Logger logger = LoggerFactory.getLogger(BookCreationDemo.class);
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Configuration for YAML-based workflow execution.
     */
    private static class YamlConfig {
        private String workflowPath = "yaml/workflows/book-creation.yaml";
        private String agentsPath = "yaml/agents/book-agents.yaml";
        private String contextPath = "yaml/context/book-context.yaml";
        private boolean interactiveMode = false;

        // Getters and setters
        public String getWorkflowPath() { return workflowPath; }
        public void setWorkflowPath(String workflowPath) { this.workflowPath = workflowPath; }

        public String getAgentsPath() { return agentsPath; }
        public void setAgentsPath(String agentsPath) { this.agentsPath = agentsPath; }

        public String getContextPath() { return contextPath; }
        public void setContextPath(String contextPath) { this.contextPath = contextPath; }

        public boolean isInteractiveMode() { return interactiveMode; }
        public void setInteractiveMode(boolean interactiveMode) { this.interactiveMode = interactiveMode; }
    }

    private static YamlConfig yamlConfig = new YamlConfig();

    public static void main(String[] args) {
        try {
            // Process advanced configuration arguments first
            String[] remainingArgs = processAdvancedArguments(args);

            String mode = extractModeFromArgs(remainingArgs);
            String topic = extractTopicFromArgs(remainingArgs);

            // Handle help and interactive modes
            if (mode.equals("help")) {
                showUsage();
                return;
            }

            if (yamlConfig.isInteractiveMode()) {
                topic = getTopicInteractively(topic);
            }

            BookCreationUtils.displayHeader("CONDUCTOR BOOK CREATION DEMO");
            System.out.println("Mode: " + mode.toUpperCase());
            System.out.println("Topic: " + topic);
            if (mode.equals("yaml")) {
                displayYamlConfiguration();
            }

            switch (mode) {
                case "code" -> runCodeBasedWorkflow(topic);
                case "yaml" -> runYamlWorkflow(topic);
                default -> showUsage();
            }
        } catch (Exception e) {
            logger.error("Demo execution failed: ", e);
            System.err.println("Demo failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs the code-based workflow using BookCreationWorkflow.
     */
    private static void runCodeBasedWorkflow(String topic) throws Exception {
        BookCreationUtils.displaySubHeader("Running Code-Based Workflow");

        String outputDir = createOutputDirectory("code");

        try (DemoDatabaseManager dbManager = new DemoDatabaseManager("code-" + System.currentTimeMillis());
             MemoryStore memoryStore = dbManager.createIsolatedMemoryStore()) {

            SubAgentRegistry registry = new SubAgentRegistry();
            Orchestrator orchestrator = new Orchestrator(registry, memoryStore);
            LLMProvider llmProvider = createLLMProvider("code");

            BookCreationWorkflow workflow = new BookCreationWorkflow(orchestrator, llmProvider, outputDir);

            long startTime = System.currentTimeMillis();
            BookManuscript manuscript = workflow.createBook(topic);
            long duration = System.currentTimeMillis() - startTime;

            displayResults("Code-Based Workflow", manuscript, duration, outputDir);
        }
    }

    /**
     * Runs the YAML-based workflow using YamlWorkflowEngine.
     */
    private static void runYamlWorkflow(String topic) throws Exception {
        BookCreationUtils.displaySubHeader("Running YAML-Based Workflow");

        String outputDir = createOutputDirectory("yaml");

        try (DemoDatabaseManager dbManager = new DemoDatabaseManager("yaml-" + System.currentTimeMillis());
             MemoryStore memoryStore = dbManager.createIsolatedMemoryStore()) {

            SubAgentRegistry registry = new SubAgentRegistry();
            Orchestrator orchestrator = new Orchestrator(registry, memoryStore);
            LLMProvider llmProvider = createLLMProvider("yaml");

            YamlWorkflowEngine adapter = new YamlWorkflowEngine(orchestrator, llmProvider);

            // Use configured paths
            adapter.loadConfiguration(
                "src/main/resources/" + yamlConfig.getWorkflowPath(),
                "src/main/resources/" + yamlConfig.getAgentsPath(),
                "src/main/resources/" + yamlConfig.getContextPath()
            );

            Map<String, Object> context = createExecutionContext(topic, outputDir);

            long startTime = System.currentTimeMillis();
            WorkflowResult result = adapter.executeWorkflow(context);
            long duration = System.currentTimeMillis() - startTime;

            displayYamlResults("YAML Workflow", result, duration, outputDir);
        }
    }


    /**
     * Creates execution context for YAML workflow.
     */
    private static Map<String, Object> createExecutionContext(String topic, String outputDir) {
        Map<String, Object> context = new HashMap<>();
        context.put("topic", topic);
        context.put("output_dir", outputDir);
        context.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        return context;
    }

    /**
     * Creates LLM provider for demo (using mock provider for consistent results).
     */
    private static LLMProvider createLLMProvider(String prefix) {
        return new DemoMockLLMProvider(prefix);
    }

    /**
     * Creates output directory for workflow results.
     */
    private static String createOutputDirectory(String mode) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String outputDir = String.format("./data/demo-databases/book-creation-%s-%s", mode, timestamp);
        BookCreationUtils.createDirectories(outputDir);
        return outputDir;
    }

    /**
     * Displays results for manuscript-based workflows.
     */
    private static void displayResults(String workflowName, BookManuscript manuscript, long duration, String outputDir) {
        BookCreationUtils.displayHeader(workflowName + " - COMPLETED");

        System.out.println("✅ Success: Workflow completed successfully");
        System.out.println("📖 Title: " + manuscript.titleInfo().title());
        System.out.println("📄 Subtitle: " + manuscript.titleInfo().subtitle());
        System.out.println("📚 Chapters: " + manuscript.chapters().size());
        System.out.println("⏱️  Duration: " + duration + "ms");
        System.out.println("📁 Output: " + outputDir);

        System.out.println("\nChapter Overview:");
        for (int i = 0; i < manuscript.chapters().size(); i++) {
            Chapter chapter = manuscript.chapters().get(i);
            System.out.printf("%d. %s (%d words)\n",
                i + 1,
                chapter.title(),
                chapter.content().split("\\s+").length
            );
        }
    }

    /**
     * Displays results for WorkflowResult-based workflows.
     */
    private static void displayYamlResults(String workflowName, WorkflowResult result, long duration, String outputDir) {
        BookCreationUtils.displayHeader(workflowName + " - COMPLETED");

        if (result.isSuccess()) {
            System.out.println("✅ Success: YAML workflow completed successfully");
            System.out.println("⏱️  Total Time: " + result.getTotalExecutionTimeMs() + "ms");
            System.out.println("🔄 Stages Executed: " + result.getStageResults().size());
            System.out.println("📁 Output: " + outputDir);

            System.out.println("\nStage Execution Details:");
            for (StageResult stageResult : result.getStageResults()) {
                System.out.println("├─ " + stageResult.getStageName() +
                                 " (attempt " + stageResult.getAttempt() +
                                 ", " + stageResult.getExecutionTimeMs() + "ms)");
                if (stageResult.isSuccess()) {
                    System.out.println("│  ✅ " + BookCreationUtils.truncateOutput(stageResult.getOutput(), 100));
                } else {
                    System.out.println("│  ❌ " + stageResult.getError());
                }
            }
        } else {
            System.err.println("❌ Workflow failed: " + result.getError());
        }
    }


    /**
     * Processes advanced configuration arguments and returns remaining topic arguments.
     */
    private static String[] processAdvancedArguments(String[] args) {
        List<String> remainingArgs = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                String configPath = arg.substring("--config=".length());
                System.setProperty("config", configPath);
                logger.info("Configuration file specified: {}", configPath);
            } else if (arg.startsWith("--workflow=")) {
                String workflowPath = arg.substring("--workflow=".length());
                yamlConfig.setWorkflowPath(workflowPath);
                logger.info("Custom workflow file specified: {}", workflowPath);
            } else if (arg.startsWith("--agents=")) {
                String agentsPath = arg.substring("--agents=".length());
                yamlConfig.setAgentsPath(agentsPath);
                logger.info("Custom agents file specified: {}", agentsPath);
            } else if (arg.startsWith("--context=")) {
                String contextPath = arg.substring("--context=".length());
                yamlConfig.setContextPath(contextPath);
                logger.info("Custom context file specified: {}", contextPath);
            } else if (arg.equals("--enable-approval") || arg.equals("--approval")) {
                // Approval feature removed for simplified 2-option system
                logger.info("Human approval workflow enabled");
            } else if (arg.equals("--interactive") || arg.equals("-i")) {
                yamlConfig.setInteractiveMode(true);
                logger.info("Interactive mode enabled");
            } else if (arg.equals("--help") || arg.equals("-h")) {
                remainingArgs.add("help");
            } else {
                remainingArgs.add(arg);
            }
        }

        return remainingArgs.toArray(new String[0]);
    }

    /**
     * Extracts mode from processed arguments (fallback to BookCreationUtils if simple).
     */
    private static String extractModeFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.equals("help")) return "help";
        }
        return BookCreationUtils.extractMode(args);
    }

    /**
     * Extracts topic from processed arguments (fallback to BookCreationUtils if simple).
     */
    private static String extractTopicFromArgs(String[] args) {
        return BookCreationUtils.extractTopic(args);
    }

    /**
     * Gets topic interactively if in interactive mode.
     */
    private static String getTopicInteractively(String defaultTopic) {
        if (defaultTopic != null && !defaultTopic.equals("Modern Software Architecture")) {
            return defaultTopic; // Already provided via command line
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("CONDUCTOR BOOK CREATION ASSISTANT");
        System.out.println("=".repeat(80));
        System.out.println("Welcome! This demo creates books using different workflow approaches.");
        System.out.println("Choose between code-based or YAML-based workflow approaches.\n");

        System.out.print("Enter your book topic or subject: ");
        try {
            if (System.in.available() > 0) {
                String topic = scanner.nextLine().trim();
                if (!topic.isEmpty()) {
                    return topic;
                }
            }
        } catch (Exception e) {
            logger.debug("Input not available");
        }

        return defaultTopic != null ? defaultTopic : "Modern Software Architecture";
    }

    /**
     * Displays YAML configuration information.
     */
    private static void displayYamlConfiguration() {
        System.out.println("Workflow: " + yamlConfig.getWorkflowPath());
        System.out.println("Agents: " + yamlConfig.getAgentsPath());
        System.out.println("Context: " + yamlConfig.getContextPath());
        System.out.println("Interactive Mode: " + (yamlConfig.isInteractiveMode() ? "ENABLED" : "DISABLED"));
    }

    /**
     * Shows usage information for the demo.
     */
    private static void showUsage() {
        BookCreationUtils.displayHeader("BOOK CREATION DEMO - USAGE");

        System.out.println("Usage: java BookCreationDemo [OPTIONS] [TOPIC]");
        System.out.println();
        System.out.println("Build Options:");
        System.out.println("  --code       Code-based workflow using WorkflowBuilder (default)");
        System.out.println("  --yaml       YAML-based declarative workflow");
        System.out.println();
        System.out.println("YAML Configuration Options:");
        System.out.println("  --workflow=FILE   Path to workflow YAML definition");
        System.out.println("                    (default: yaml/workflows/book-creation.yaml)");
        System.out.println("  --agents=FILE     Path to agents YAML configuration");
        System.out.println("                    (default: yaml/agents/book-agents.yaml)");
        System.out.println("  --context=FILE    Path to context YAML file");
        System.out.println("                    (default: yaml/context/book-context.yaml)");
        System.out.println("  --interactive, -i Enable interactive topic input");
        System.out.println("  --help, -h        Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java BookCreationDemo \"AI and Machine Learning\"");
        System.out.println("  java BookCreationDemo --code \"Software Architecture\"");
        System.out.println("  java BookCreationDemo --yaml \"Cloud Computing\"");
        System.out.println("  java BookCreationDemo --yaml --interactive");
        System.out.println();
        System.out.println("Features:");
        System.out.println("• Code-based: Full programmatic control using WorkflowBuilder");
        System.out.println("• YAML-based: Declarative configuration-driven workflows");
        System.out.println("• Both approaches use the same underlying execution engine");
        System.out.println("• Choose the approach that best fits your team's needs");
    }
}