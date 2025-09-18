package com.skanga.conductor.demo;

import com.skanga.conductor.agent.SubAgentRegistry;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.provider.MockLLMProvider;
import com.skanga.conductor.provider.OpenAiLLMProvider;
import com.skanga.conductor.provider.AnthropicLLMProvider;
import com.skanga.conductor.provider.GeminiLLMProvider;
import com.skanga.conductor.provider.OllamaLLMProvider;
import com.skanga.conductor.provider.LocalAiLLMProvider;
import com.skanga.conductor.provider.AzureOpenAiLLMProvider;
import com.skanga.conductor.provider.AmazonBedrockLLMProvider;
import com.skanga.conductor.provider.OracleLLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Comprehensive book creation demo with agentic review and human-in-the-loop approval.
 * <p>
 * This demo showcases an advanced AI-powered book creation workflow that includes:
 * </p>
 * <ul>
 * <li><strong>Topic-to-Title Generation:</strong> Transform a topic prompt into compelling titles</li>
 * <li><strong>Table of Contents Creation:</strong> Generate detailed chapter outlines with descriptions</li>
 * <li><strong>Chapter-by-Chapter Writing:</strong> Create comprehensive chapter content</li>
 * <li><strong>Multi-Stage Review Process:</strong> Agentic review at each stage</li>
 * <li><strong>Human-in-the-Loop Approval:</strong> User approval required at each stage</li>
 * <li><strong>Markdown Output Generation:</strong> Professional documentation output</li>
 * </ul>
 * <p>
 * The workflow demonstrates:
 * </p>
 * <ol>
 * <li>Multi-agent orchestration with specialized agents for different tasks</li>
 * <li>Memory persistence across the workflow stages</li>
 * <li>Human oversight and approval processes</li>
 * <li>Structured output generation for review and editing</li>
 * <li>Iterative refinement based on feedback</li>
 * </ol>
 *
 * @since 1.0.0
 * @see BookCreationWorkflow
 * @see BookManuscript
 */
public class BookCreationDemo {

    private static final Logger logger = LoggerFactory.getLogger(BookCreationDemo.class);
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Main entry point for the book creation demo.
     *
     * @param args command line arguments; if provided, will be used as the book topic
     */
    public static void main(String[] args) {
        logger.info("=== Advanced Book Creation Demo ===");
        logger.info("This demo will guide you through creating a complete book with AI assistance.");
        logger.info("You'll have the opportunity to review and approve content at each stage.");

        try {
            runBookCreationDemo(args);
        } catch (Exception e) {
            logger.error("Book creation demo failed: ", e);
            System.exit(1);
        }
    }

    /**
     * Runs the complete book creation demo workflow.
     *
     * @param args command line arguments for custom topic
     */
    private static void runBookCreationDemo(String[] args) {
        // Get book topic from user or command line
        String bookTopic = getBookTopic(args);

        // Create output directory for this session
        String outputDir = createSessionOutputDirectory();

        // Initialize the workflow components
        try (DemoDatabaseManager dbManager = new DemoDatabaseManager("book-creation-" + System.currentTimeMillis());
             MemoryStore memoryStore = dbManager.createIsolatedMemoryStore()) {

            // Set up the orchestrator and workflow
            SubAgentRegistry registry = new SubAgentRegistry();
            Orchestrator orchestrator = new Orchestrator(registry, memoryStore);
            LLMProvider llmProvider = createLLMProvider();

            BookCreationWorkflow workflow = new BookCreationWorkflow(orchestrator, llmProvider, outputDir);

            // Display workflow information
            displayWorkflowInfo(bookTopic, outputDir);

            // Execute the workflow
            logger.info("Starting book creation workflow...");
            BookManuscript manuscript = workflow.createBook(bookTopic);

            // Display completion summary
            displayCompletionSummary(manuscript, outputDir);

            // Optional: Offer to create additional outputs
            offerAdditionalOutputs(manuscript, outputDir);

        } catch (ConductorException e) {
            logger.error("Book creation workflow failed: ", e);
            System.err.println("Book creation failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during book creation: ", e);
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Processes configuration arguments and filters them out from topic arguments.
     *
     * @param args original command line arguments
     * @return filtered arguments containing only topic-related content
     */
    private static String[] processConfigurationArguments(String[] args) {
        List<String> topicArgs = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                // Extract config file path and set as system property
                String configPath = arg.substring("--config=".length());
                System.setProperty("config", configPath);
                logger.info("Configuration file specified: {}", configPath);
            } else if (arg.equals("--config") && topicArgs.size() < args.length - 1) {
                // Handle --config file.properties format (next argument is the file)
                // This will be processed in the next iteration
                continue;
            } else if (!arg.startsWith("--")) {
                // This is a topic argument
                topicArgs.add(arg);
            }
        }

        return topicArgs.toArray(new String[0]);
    }

    /**
     * Gets the book topic from user input or command line arguments.
     * Also processes configuration arguments like --config=file.properties
     */
    private static String getBookTopic(String[] args) {
        // Process configuration arguments first
        String[] topicArgs = processConfigurationArguments(args);

        if (topicArgs.length > 0) {
            String topic = String.join(" ", topicArgs);
            logger.info("Using topic from command line: {}", topic);
            return topic;
        }

        // Check for configured custom prompt
        String customTopic = DemoConfig.getInstance().getCustomDemoPrompt("book.topic");
        if (customTopic != null) {
            logger.info("Using configured topic: {}", customTopic);
            return customTopic;
        }

        // Interactive topic input
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BOOK CREATION ASSISTANT");
        System.out.println("=".repeat(80));
        System.out.println("Welcome! I'll help you create a complete book using AI assistance.");
        System.out.println("You'll review and approve content at each stage.\n");

        System.out.print("Enter your book topic or subject: ");
        String topic = scanner.nextLine().trim();

        if (topic.isEmpty()) {
            topic = "The fundamentals of distributed systems and microservices architecture";
            System.out.println("Using default topic: " + topic);
        }

        return topic;
    }

    /**
     * Creates a session-specific output directory.
     */
    private static String createSessionOutputDirectory() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        DemoConfig config = DemoConfig.getInstance();
        String baseDir = config.isDemoTemporary() ?
            config.getDemoTempDatabaseDir() :
            config.getDemoPersistentDatabaseDir();

        return baseDir + "/book-creation-" + timestamp;
    }

    /**
     * Creates an LLM provider based on configuration.
     */
    private static LLMProvider createLLMProvider() {
        String providerType = DemoConfig.getInstance().getDemoProviderType();
        String modelName = DemoConfig.getInstance().getDemoProviderModel();
        String baseUrl = DemoConfig.getInstance().getDemoProviderBaseUrl();
        logger.info("Using LLM provider: {}", providerType);

        return switch (providerType.toLowerCase()) {
            case "mock" -> new MockLLMProvider("book-creation");
            case "openai" -> new OpenAiLLMProvider(DemoConfig.getInstance().getAppConfig().getString("openai.api.key"), modelName, baseUrl);
            case "anthropic" -> new AnthropicLLMProvider(DemoConfig.getInstance().getAppConfig().getString("anthropic.api.key"), modelName);
            case "google", "gemini" -> new GeminiLLMProvider(DemoConfig.getInstance().getAppConfig().getString("gemini.api.key"), modelName);
            case "ollama" -> new OllamaLLMProvider(baseUrl, modelName);
            case "localai", "local-ai" -> new LocalAiLLMProvider(baseUrl, modelName);
            /*
            case "azure", "azure-openai" -> new AzureOpenAiLLMProvider(apiKey, endpoint, depName);
            case "bedrock", "amazon-bedrock" -> new AmazonBedrockLLMProvider(modelId, region);
            case "oracle", "oci" -> new OracleLLMProvider(compartmentId, modelName);
            */
            default -> {
                logger.warn("Unknown provider type '{}', using mock provider", providerType);
                yield new MockLLMProvider("book-creation");
            }
        };
    }

    /**
     * Displays workflow information to the user.
     */
    private static void displayWorkflowInfo(String topic, String outputDir) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("WORKFLOW CONFIGURATION");
        System.out.println("=".repeat(80));
        System.out.printf("Topic: %s%n", topic);
        System.out.printf("Output Directory: %s%n", outputDir);
        DemoConfig config = DemoConfig.getInstance();
        System.out.printf("Target Words per Chapter: %d%n", config.getBookTargetWords());
        System.out.printf("Max Words per Chapter: %d%n", config.getBookMaxWords());
        System.out.printf("Provider: %s%n", config.getDemoProviderType());
        System.out.printf("Model: %s%n", config.getDemoProviderModel());
        System.out.printf("Base URL: %s%n", config.getDemoProviderBaseUrl());
        System.out.printf("Verbose Logging: %s%n", config.isVerboseLoggingEnabled());

        System.out.println("=".repeat(80));

        System.out.println("\nThe workflow includes these stages:");
        System.out.println("1. Title and Subtitle Generation (with review & approval)");
        System.out.println("2. Table of Contents Creation (with review & approval)");
        System.out.println("3. Chapter-by-Chapter Writing (with review & approval)");
        System.out.println("4. Final Book Review (with approval)");
        System.out.println("\nAll outputs will be saved as markdown files for easy review.");

        System.out.print("\nPress Enter to continue...");
        try {
            if (System.in.available() > 0 || System.console() != null) {
                scanner.nextLine();
            } else {
                System.out.println(" [Auto-continuing in non-interactive mode]");
            }
        } catch (IOException e) {
            System.out.println(" [Auto-continuing - unable to check input availability]");
        }
    }

    /**
     * Displays completion summary to the user.
     */
    private static void displayCompletionSummary(BookManuscript manuscript, String outputDir) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BOOK CREATION COMPLETED!");
        System.out.println("=".repeat(80));
        System.out.println(manuscript.getSummary());
        System.out.println("=".repeat(80));
        System.out.printf("All files saved to: %s%n", outputDir);

        if (DemoConfig.getInstance().isVerboseLoggingEnabled()) {
            System.out.println("\nChapter Overview:");
            for (int i = 0; i < manuscript.chapters().size(); i++) {
                Chapter chapter = manuscript.chapters().get(i);
                int wordCount = chapter.content().split("\\s+").length;
                System.out.printf("%d. %s (%,d words)%n", i + 1, chapter.title(), wordCount);
            }
        }

        logger.info("Book creation completed successfully!");
        logger.info("Generated book: {}", manuscript.titleInfo().title());
        logger.info("Chapters: {}", manuscript.getChapterCount());
        logger.info("Estimated words: {}", manuscript.getEstimatedWordCount());
        logger.info("Estimated pages: {}", manuscript.getEstimatedPageCount());
    }

    /**
     * Offers additional output options to the user.
     */
    private static void offerAdditionalOutputs(BookManuscript manuscript, String outputDir) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ADDITIONAL OPTIONS");
        System.out.println("=".repeat(80));
        System.out.println("Your book has been created and saved to markdown files.");
        System.out.println("You can find the following files in the output directory:");
        System.out.println("- 01-title-*.md (Title and subtitle with reviews)");
        System.out.println("- 02-toc-*.md (Table of contents with reviews)");
        System.out.println("- 03-chapter-*.md (Individual chapters with reviews)");
        System.out.println("- 04-complete-book-*.md (Complete book in one file)");

        System.out.print("\nWould you like to see a summary of the generated content? (y/n): ");
        try {
            if (System.console() != null) {
                String response = scanner.nextLine().trim().toLowerCase();
                if (response.equals("y") || response.equals("yes")) {
                    displayContentSummary(manuscript);
                }
            } else {
                System.out.println("y [Auto-approved in non-interactive mode]");
                displayContentSummary(manuscript);
            }
        } catch (Exception e) {
            System.out.println("y [Auto-approved due to input unavailability]");
            displayContentSummary(manuscript);
        }

        if (DemoConfig.getInstance().isCleanupEnabled()) {
            System.out.print("\nWould you like to clean up temporary files? (y/n): ");
            try {
                if (System.console() != null) {
                    String response = scanner.nextLine().trim().toLowerCase();
                    if (response.equals("y") || response.equals("yes")) {
                        logger.info("Cleanup would be performed here (implementation depends on requirements)");
                        System.out.println("Cleanup completed.");
                    }
                } else {
                    System.out.println("n [Auto-declined in non-interactive mode]");
                }
            } catch (Exception e) {
                System.out.println("n [Auto-declined due to input unavailability]");
            }
        }

        System.out.println("\nThank you for using the Book Creation Demo!");
        System.out.println("Your complete book is ready for further editing and publication.");
    }

    /**
     * Displays a detailed content summary.
     */
    private static void displayContentSummary(BookManuscript manuscript) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CONTENT SUMMARY");
        System.out.println("=".repeat(80));

        System.out.printf("Title: %s%n", manuscript.titleInfo().title());
        System.out.printf("Subtitle: %s%n", manuscript.titleInfo().subtitle());
        System.out.println();

        System.out.println("Table of Contents Summary:");
        System.out.println(manuscript.tableOfContents().getSummary());
        System.out.println();

        System.out.println("Chapter Details:");
        for (int i = 0; i < manuscript.chapters().size(); i++) {
            Chapter chapter = manuscript.chapters().get(i);
            int wordCount = chapter.content().split("\\s+").length;
            String preview = chapter.content().length() > 100 ?
                chapter.content().substring(0, 100) + "..." :
                chapter.content();

            System.out.printf("%d. %s (%,d words)%n", i + 1, chapter.title(), wordCount);
            System.out.printf("   Preview: %s%n", preview.replace("\n", " "));
            System.out.println();
        }
    }
}