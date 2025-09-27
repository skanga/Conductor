package com.skanga.conductor.demo;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.utils.SingletonHolder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Configuration class for demo applications with sensible defaults and environment overrides.
 * <p>
 * This class provides configuration management specifically for demo scenarios,
 * allowing customization of demo behavior through properties files or environment
 * variables while providing reasonable defaults for immediate use.
 * </p>
 * <p>
 * Configuration keys follow the pattern: {@code demo.*}
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * DemoConfig config = DemoConfig.getInstance();
 * String workflowId = config.getWorkflowId();
 * int targetWords = config.getBookTargetWords();
 * </pre>
 * <p>
 * Environment variables or properties can override defaults:
 * </p>
 * <pre>
 * # demo.properties
 * demo.workflow.id.prefix=my-demo
 * demo.book.target.words=600
 * demo.database.isolation=true
 * demo.memory.limit=50
 * </pre>
 *
 * @since 1.0.0
 * @see ApplicationConfig
 */
public class DemoConfig {

    private static final SingletonHolder<DemoConfig> HOLDER =
        SingletonHolder.of(DemoConfig::new);

    private final ApplicationConfig appConfig;

    /**
     * Private constructor for singleton pattern.
     */
    private DemoConfig() {
        this.appConfig = ApplicationConfig.getInstance();
    }

    /**
     * Returns the singleton instance of DemoConfig.
     * <p>
     * This method uses the generic SingletonHolder pattern for optimal
     * thread safety and performance. The instance is created only once
     * and subsequent calls return the same instance.
     * </p>
     *
     * @return the singleton DemoConfig instance
     */
    public static DemoConfig getInstance() {
        return HOLDER.get();
    }

    /**
     * Resets the singleton instance for testing purposes.
     * <p>
     * This method should only be used in test scenarios where you need
     * to reset the configuration state between tests.
     * </p>
     */
    public static void resetInstance() {
        HOLDER.reset();
    }

    /**
     * Gets the workflow ID for demo runs.
     * <p>
     * Generates a unique workflow ID with configurable prefix to avoid
     * conflicts between demo runs and enable database isolation.
     * </p>
     *
     * @return unique workflow ID for this demo run
     */
    public String getWorkflowId() {
        String prefix = appConfig.getString("demo.workflow.id.prefix", "demo-workflow");
        String suffix = appConfig.getString("demo.workflow.id.suffix", UUID.randomUUID().toString().substring(0, 8));
        return prefix + "-" + suffix;
    }

    /**
     * Gets the target word count for book chapters.
     *
     * @return target word count for generated chapters
     */
    public int getBookTargetWords() {
        return appConfig.getInt("demo.book.target.words", 400);
    }

    /**
     * Gets the maximum word count for book chapters.
     *
     * @return maximum word count for generated chapters
     */
    public int getBookMaxWords() {
        return appConfig.getInt("demo.book.max.words", 600);
    }

    /**
     * Gets the default chapter title when no title is provided.
     *
     * @return default chapter title
     */
    public String getDefaultChapterTitle() {
        return appConfig.getString("demo.book.default.chapter.title", "Chapter 1");
    }

    /**
     * Gets whether database isolation is enabled for demos.
     * <p>
     * When enabled, each demo run uses a separate database schema
     * to prevent interference between concurrent or sequential runs.
     * </p>
     *
     * @return true if database isolation is enabled
     */
    public boolean isDatabaseIsolationEnabled() {
        return appConfig.getBoolean("demo.database.isolation", false);
    }

    /**
     * Gets the database URL for demo runs.
     * <p>
     * When database isolation is enabled, appends a unique suffix
     * to create separate database instances.
     * </p>
     *
     * @return database URL for demo use
     */
    public String getDemoDatabaseUrl() {
        String baseUrl = appConfig.getString("demo.database.url", "jdbc:h2:./data/demo-subagentsdb");

        if (isDatabaseIsolationEnabled()) {
            String isolationSuffix = appConfig.getString("demo.database.isolation.suffix",
                "_" + System.currentTimeMillis());
            return baseUrl + isolationSuffix;
        }

        return baseUrl;
    }

    /**
     * Gets the memory limit for demo agents.
     *
     * @return maximum number of memory entries per agent
     */
    public int getDemoMemoryLimit() {
        return appConfig.getInt("demo.memory.limit", 20);
    }

    /**
     * Gets the memory snapshot size for demo output.
     *
     * @return number of memory entries to show in snapshots
     */
    public int getMemorySnapshotSize() {
        return appConfig.getInt("demo.memory.snapshot.size", 10);
    }

    /**
     * Gets the maximum output length for demo logs.
     *
     * @return maximum characters to display in demo output
     */
    public int getMaxOutputLength() {
        return appConfig.getInt("demo.output.max.length", 120);
    }

    /**
     * Gets whether failure simulation is enabled in mock providers.
     *
     * @return true if failure simulation should be enabled
     */
    public boolean isFailureSimulationEnabled() {
        return appConfig.getBoolean("demo.failure.simulation.enabled", false);
    }

    /**
     * Gets the failure rate for mock provider simulation.
     *
     * @return failure rate as a decimal (0.0 to 1.0)
     */
    public double getFailureSimulationRate() {
        return appConfig.getDouble("demo.failure.simulation.rate", 0.1);
    }

    /**
     * Gets the delay simulation for mock providers.
     *
     * @return delay duration for simulating network latency
     */
    public Duration getSimulatedDelay() {
        long millis = appConfig.getLong("demo.simulation.delay.millis", 100);
        return Duration.ofMillis(millis);
    }

    /**
     * Gets whether verbose logging is enabled for demos.
     *
     * @return true if verbose demo logging is enabled
     */
    public boolean isVerboseLoggingEnabled() {
        return appConfig.getBoolean("demo.logging.verbose", true);
    }

    /**
     * Gets the list of demo tools to register.
     * <p>
     * Allows configuration of which tools should be available in tool demos.
     * </p>
     *
     * @return list of tool class names to register
     */
    public List<String> getDemoToolClasses() {
        String toolsConfig = appConfig.getString("demo.tools.classes",
            "FileReadTool,WebSearchTool,CodeRunnerTool,TextToSpeechTool");
        return List.of(toolsConfig.split(","));
    }

    /**
     * Gets the demo provider type for LLM operations.
     *
     * @return provider type ("mock", "openai", etc.)
     */
    public String getDemoProviderType() {
        return appConfig.getString("demo.provider.type", "mock");
    }
    
    /**
     * Gets the demo planner provider type.
     *
     * @return planner provider type ("mock", "openai", etc.)
     */
    public String getDemoPlannerProviderType() {
        return appConfig.getString("demo.planner.provider.type", "mock");
    }

    /**
     * Gets the demo provider model name.
     *
     * @return model name for the LLM provider
     */
    public String getDemoProviderModel() {
        return appConfig.getString("demo.provider.model", "gpt-3.5-turbo");
    }

    /**
     * Gets the demo provider base URL.
     *
     * @return base URL for the LLM provider
     */
    public String getDemoProviderBaseUrl() {
        return appConfig.getString("demo.provider.base.url", "https://api.openai.com/v1");
    }

    /**
     * Gets custom demo prompts for specific scenarios.
     *
     * @param scenarioName the name of the demo scenario
     * @return custom prompt for the scenario, or null if not configured
     */
    public String getCustomDemoPrompt(String scenarioName) {
        return appConfig.getString("demo.prompts." + scenarioName, null);
    }

    /**
     * Gets the writer prompt template for book generation.
     *
     * @return prompt template for the writer agent
     */
    public String getWriterPromptTemplate() {
        return appConfig.getString("demo.book.writer.prompt.template",
            "Write a chapter with title: {{title}}. Chapter spec: {{spec}}. " +
            "Aim for " + getBookTargetWords() + "-" + getBookMaxWords() + " words.");
    }

    /**
     * Gets the editor prompt template for book generation.
     *
     * @return prompt template for the editor agent
     */
    public String getEditorPromptTemplate() {
        return appConfig.getString("demo.book.editor.prompt.template",
            "Edit the chapter for clarity and concision. Suggest 3 improvements.");
    }

    /**
     * Gets the critic prompt template for book generation.
     *
     * @return prompt template for the critic agent
     */
    public String getCriticPromptTemplate() {
        return appConfig.getString("demo.book.critic.prompt.template",
            "Critique the chapter. Provide numbered points for improvement.");
    }


    /**
     * Gets the demo run timeout duration.
     *
     * @return maximum time to allow for demo completion
     */
    public Duration getDemoTimeout() {
        long minutes = appConfig.getLong("demo.timeout.minutes", 5);
        return Duration.ofMinutes(minutes);
    }

    /**
     * Gets whether demo databases should be automatically cleaned up after completion.
     * <p>
     * When enabled (default), each agentic run gets its own isolated database
     * that is automatically removed after the run completes. This prevents
     * database file accumulation and ensures clean state for each run.
     * </p>
     *
     * @return true if demo databases should be automatically cleaned up
     */
    public boolean isAutoCleanupEnabled() {
        return appConfig.getBoolean("demo.database.auto.cleanup", true);
    }

    /**
     * Gets whether demo databases should be preserved for debugging.
     * <p>
     * When enabled, databases are kept after completion for inspection and
     * analysis, even if auto-cleanup is enabled. This is useful for development
     * and troubleshooting scenarios.
     * </p>
     *
     * @return true if demo databases should be preserved for debugging
     */
    public boolean isPreserveForDebug() {
        return appConfig.getBoolean("demo.database.preserve.for.debug", false);
    }

    /**
     * Gets the base directory for demo databases.
     * <p>
     * All demo databases will be created in subdirectories under this path,
     * with each agentic run getting its own isolated directory.
     * </p>
     *
     * @return directory path for demo database files
     */
    public String getDemoDatabaseBaseDir() {
        return appConfig.getString("demo.database.base.dir", "./data/demo-databases");
    }

    /**
     * Gets the maximum age for demo databases in hours.
     * <p>
     * Databases older than this age may be cleaned up during maintenance
     * operations, even if they are marked as persistent.
     * </p>
     *
     * @return maximum age in hours before databases are eligible for cleanup
     */
    public int getDatabaseMaxAgeHours() {
        return appConfig.getInt("demo.database.max.age.hours", 24);
    }


    public ApplicationConfig getAppConfig() {
        return appConfig;
    }
}