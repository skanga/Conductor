package com.skanga.conductor.factory;

import com.skanga.conductor.config.ApplicationConfig;
import com.skanga.conductor.engine.DefaultWorkflowEngine;
import com.skanga.conductor.engine.YamlWorkflowEngine;
import com.skanga.conductor.engine.execution.StageExecutor;
import com.skanga.conductor.memory.MemoryManager;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.memory.ResourceTracker;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.templates.PromptTemplateEngine;
import com.skanga.conductor.templates.TemplateCompiler;
import com.skanga.conductor.templates.TemplateFilters;
import com.skanga.conductor.templates.VariableResolver;
import com.skanga.conductor.workflow.config.WorkflowConfigLoader;
import com.skanga.conductor.workflow.templates.AgentFactory;
import com.skanga.conductor.workflow.output.FileOutputGenerator;
import com.skanga.conductor.workflow.output.StandardFileOutputGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Centralized factory for creating workflow components with dependency injection.
 * <p>
 * This factory addresses the testability and maintainability issues caused by
 * hard-coded dependencies throughout the codebase. It provides:
 * </p>
 * <ul>
 * <li>Centralized component creation</li>
 * <li>Dependency injection support</li>
 * <li>Component lifecycle management</li>
 * <li>Singleton management where appropriate</li>
 * <li>Test-friendly dependency substitution</li>
 * </ul>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent component creation.
 * </p>
 *
 * @since 2.0.0
 */
public class WorkflowComponentFactory {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowComponentFactory.class);

    // Singleton registry for shared components
    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();

    // Custom component suppliers for testing
    private final Map<Class<?>, Supplier<?>> customSuppliers = new ConcurrentHashMap<>();

    // Configuration
    private final ApplicationConfig config;

    /**
     * Creates a new WorkflowComponentFactory with default configuration.
     */
    public WorkflowComponentFactory() {
        this(ApplicationConfig.getInstance());
    }

    /**
     * Creates a new WorkflowComponentFactory with custom configuration.
     * <p>
     * This constructor is useful for testing where you want to provide
     * a custom ApplicationConfig instance.
     * </p>
     *
     * @param config the application configuration to use
     * @throws IllegalArgumentException if config is null
     */
    public WorkflowComponentFactory(ApplicationConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        this.config = config;
        logger.debug("WorkflowComponentFactory initialized");
    }

    /**
     * Registers a custom supplier for a component type.
     * <p>
     * This is primarily used for testing to inject mock or stub implementations.
     * </p>
     *
     * @param type the component type
     * @param supplier the supplier that creates instances of the component
     * @param <T> the component type
     * @throws IllegalArgumentException if type or supplier is null
     */
    public <T> void registerCustomSupplier(Class<T> type, Supplier<T> supplier) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("supplier cannot be null");
        }
        customSuppliers.put(type, supplier);
        // Clear any existing singleton
        singletons.remove(type);
        logger.debug("Registered custom supplier for {}", type.getSimpleName());
    }

    /**
     * Clears all custom suppliers and singletons.
     * <p>
     * This is useful for test cleanup to ensure test isolation.
     * </p>
     */
    public void reset() {
        customSuppliers.clear();
        singletons.clear();
        logger.debug("Factory reset - all custom suppliers and singletons cleared");
    }

    // ========== Template Engine Components ==========

    /**
     * Creates a new PromptTemplateEngine instance.
     */
    public PromptTemplateEngine createPromptTemplateEngine() {
        return getOrCreate(PromptTemplateEngine.class, () -> {
            TemplateFilters filters = createTemplateFilters();
            VariableResolver resolver = createVariableResolver(filters);
            TemplateCompiler compiler = createTemplateCompiler(resolver);
            return new PromptTemplateEngine(true, 100);
        });
    }

    /**
     * Creates a new TemplateFilters instance.
     */
    public TemplateFilters createTemplateFilters() {
        return getOrCreate(TemplateFilters.class, TemplateFilters::new);
    }

    /**
     * Creates a new VariableResolver instance.
     *
     * @param filters the template filters to use
     * @return a new VariableResolver instance
     * @throws IllegalArgumentException if filters is null
     */
    public VariableResolver createVariableResolver(TemplateFilters filters) {
        if (filters == null) {
            throw new IllegalArgumentException("filters cannot be null");
        }
        return new VariableResolver(filters);
    }

    /**
     * Creates a new TemplateCompiler instance.
     *
     * @param resolver the variable resolver to use
     * @return a new TemplateCompiler instance
     * @throws IllegalArgumentException if resolver is null
     */
    public TemplateCompiler createTemplateCompiler(VariableResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver cannot be null");
        }
        return new TemplateCompiler(resolver);
    }

    // ========== Workflow Engine Components ==========

    /**
     * Creates a new DefaultWorkflowEngine instance.
     *
     * @param orchestrator the orchestrator for agent management
     * @return a new DefaultWorkflowEngine instance
     * @throws IllegalArgumentException if orchestrator is null
     */
    public DefaultWorkflowEngine createDefaultWorkflowEngine(Orchestrator orchestrator) {
        if (orchestrator == null) {
            throw new IllegalArgumentException("orchestrator cannot be null");
        }
        return new DefaultWorkflowEngine(orchestrator, createPromptTemplateEngine());
    }

    /**
     * Creates a new YamlWorkflowEngine instance.
     */
    public YamlWorkflowEngine createYamlWorkflowEngine() {
        return new YamlWorkflowEngine(
            createWorkflowConfigLoader(),
            createAgentFactory(),
            createPromptTemplateEngine(),
            createStageExecutor(),
            createFileOutputGenerator()
        );
    }

    /**
     * Creates a new StageExecutor instance.
     */
    public StageExecutor createStageExecutor() {
        return new StageExecutor(createPromptTemplateEngine());
    }

    // ========== Configuration Components ==========

    /**
     * Creates a new WorkflowConfigLoader instance.
     */
    public WorkflowConfigLoader createWorkflowConfigLoader() {
        return getOrCreate(WorkflowConfigLoader.class, WorkflowConfigLoader::new);
    }

    /**
     * Creates a new AgentFactory instance.
     */
    public AgentFactory createAgentFactory() {
        return getOrCreate(AgentFactory.class, AgentFactory::new);
    }

    // ========== Memory Components ==========

    /**
     * Gets or creates a singleton MemoryManager instance.
     */
    public MemoryManager getMemoryManager() {
        return getOrCreateSingleton(MemoryManager.class, MemoryManager::new);
    }

    /**
     * Creates a new MemoryStore instance.
     *
     * @param namespace the namespace for the memory store
     * @param context the context for the memory store
     * @param purpose the purpose for the memory store
     * @return a new MemoryStore instance
     * @throws IllegalArgumentException if any parameter is null or blank
     * @throws RuntimeException if memory store creation fails
     */
    public MemoryStore createMemoryStore(String namespace, String context, String purpose) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace cannot be null or blank");
        }
        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException("context cannot be null or blank");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("purpose cannot be null or blank");
        }
        try {
            return new MemoryStore(namespace, context, purpose);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to create MemoryStore", e);
        }
    }

    /**
     * Creates a new ResourceTracker instance.
     */
    public ResourceTracker createResourceTracker() {
        return new ResourceTracker();
    }

    // ========== Metrics Components ==========

    /**
     * Gets the singleton MetricsRegistry instance.
     */
    public MetricsRegistry getMetricsRegistry() {
        return getOrCreateSingleton(MetricsRegistry.class, MetricsRegistry::getInstance);
    }

    // ========== Output Components ==========

    /**
     * Creates a new FileOutputGenerator instance.
     */
    public FileOutputGenerator createFileOutputGenerator() {
        return getOrCreate(FileOutputGenerator.class, StandardFileOutputGenerator::new);
    }

    // ========== Agent Components ==========
    // Note: SubAgent creation is handled by AgentFactory since SubAgent is abstract

    // ========== Helper Methods ==========

    /**
     * Gets an existing instance or creates a new one using the supplier.
     * <p>
     * Checks for custom suppliers first (for testing), then creates using default supplier.
     * Does NOT cache the result.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private <T> T getOrCreate(Class<T> type, Supplier<T> defaultSupplier) {
        // Check for custom supplier (for testing)
        Supplier<?> customSupplier = customSuppliers.get(type);
        if (customSupplier != null) {
            return (T) customSupplier.get();
        }

        // Use default supplier
        return defaultSupplier.get();
    }

    /**
     * Gets or creates a singleton instance.
     * <p>
     * Checks for custom suppliers first, then returns cached singleton,
     * or creates and caches a new instance.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private <T> T getOrCreateSingleton(Class<T> type, Supplier<T> defaultSupplier) {
        // Check for custom supplier (for testing)
        Supplier<?> customSupplier = customSuppliers.get(type);
        if (customSupplier != null) {
            return (T) customSupplier.get();
        }

        // Check for existing singleton
        return (T) singletons.computeIfAbsent(type, k -> defaultSupplier.get());
    }

    /**
     * Gets the ApplicationConfig instance.
     */
    public ApplicationConfig getConfig() {
        return config;
    }

    /**
     * Creates a new builder for fluent configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating WorkflowComponentFactory with custom configuration.
     */
    public static class Builder {
        private ApplicationConfig config;

        public Builder withConfig(ApplicationConfig config) {
            this.config = config;
            return this;
        }

        public WorkflowComponentFactory build() {
            if (config == null) {
                config = ApplicationConfig.getInstance();
            }
            return new WorkflowComponentFactory(config);
        }
    }
}
