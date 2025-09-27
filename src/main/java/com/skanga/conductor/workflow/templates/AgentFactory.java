package com.skanga.conductor.workflow.templates;

import com.skanga.conductor.agent.SubAgent;
import com.skanga.conductor.agent.ConversationalAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.workflow.config.AgentDefinition;
import com.skanga.conductor.orchestration.Orchestrator;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.tools.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating agents from YAML configuration definitions.
 * Supports different agent types and handles provider configuration.
 */
public class AgentFactory {

    private static final Logger logger = LoggerFactory.getLogger(AgentFactory.class);
    private final ToolRegistry toolRegistry;

    /**
     * Creates a new AgentFactory with a default tool registry.
     */
    public AgentFactory() {
        this.toolRegistry = createDefaultToolRegistry();
    }

    /**
     * Creates a new AgentFactory with a custom tool registry.
     */
    public AgentFactory(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry != null ? toolRegistry : createDefaultToolRegistry();
    }

    /**
     * Creates an agent based on its definition and orchestrator.
     * <p>
     * IMPORTANT: All agents created by this factory share the same MemoryStore instance
     * as the orchestrator to ensure consistent memory access across workflow agents.
     * This prevents memory isolation issues where agents cannot access shared context.
     * </p>
     */
    public SubAgent createAgent(AgentDefinition definition, Orchestrator orchestrator) throws ConductorException {
        if (definition == null) {
            throw new IllegalArgumentException("Agent definition cannot be null");
        }
        if (orchestrator == null) {
            throw new IllegalArgumentException("Orchestrator cannot be null");
        }

        String agentType = definition.getType();
        if (agentType == null || agentType.isEmpty()) {
            agentType = "llm"; // Default to LLM agent
        }

        logger.debug("Creating agent of type '{}' with role '{}'", agentType, definition.getRole());

        switch (agentType.toLowerCase()) {
            case "llm":
                return createLLMAgent(definition, orchestrator);
            case "tool":
                return createToolAgent(definition, orchestrator);
            default:
                throw new ConductorException("Unsupported agent type: " + agentType);
        }
    }

    /**
     * Creates an LLM-based agent.
     */
    private SubAgent createLLMAgent(AgentDefinition definition, Orchestrator orchestrator) throws ConductorException {
        String agentId = generateAgentId(definition);
        String role = definition.getRole();

        // Get LLM provider - for now, use the orchestrator's default provider
        // Future enhancement: support per-agent provider configuration
        LLMProvider llmProvider = getDefaultLLMProvider();

        // Create the agent using the orchestrator
        String promptTemplate = definition.getPromptTemplate();
        if (promptTemplate == null || promptTemplate.isEmpty()) {
            throw new ConductorException(
                "LLM agent '" + role + "' must have a prompt template");
        }

        // For now, we'll create a simple prompt. The actual prompt will be generated
        // by the PromptTemplateEngine during execution
        String simplePrompt = "Agent role: " + role;

        try {
            SubAgent agent = orchestrator.createImplicitAgent(agentId, role, llmProvider, simplePrompt);
            logger.info("Created LLM agent: {} ({})", agentId, role);
            return agent;
        } catch (java.sql.SQLException e) {
            throw new ConductorException("Failed to create LLM agent '" + role + "': " + e.getMessage(), e);
        }
    }

    /**
     * Creates a tool-based agent.
     */
    private SubAgent createToolAgent(AgentDefinition definition, Orchestrator orchestrator) throws ConductorException {
        String agentId = generateAgentId(definition);
        String role = definition.getRole();

        // Get the tool agent strategy from the definition or default to "llm-tool"
        String toolStrategy = definition.getProvider(); // Reuse provider field for tool strategy
        if (toolStrategy == null || toolStrategy.isEmpty()) {
            toolStrategy = "llm-tool"; // Default to LLM-guided tool usage
        }

        try {
            SubAgent agent;

            switch (toolStrategy.toLowerCase()) {
                case "llm-tool":
                    // Create LLMToolAgent - uses LLM to determine which tools to call
                    LLMProvider llmProvider = getDefaultLLMProvider();
                    agent = createLLMToolAgent(agentId, role, llmProvider, orchestrator);
                    logger.info("Created LLM-Tool agent: {} ({})", agentId, role);
                    break;

                case "direct-tool":
                    // Create ToolUsingAgent - calls tools directly based on prompt patterns
                    agent = createDirectToolAgent(agentId, role, orchestrator);
                    logger.info("Created Direct-Tool agent: {} ({})", agentId, role);
                    break;

                default:
                    throw new ConductorException("Unsupported tool strategy: " + toolStrategy + ". Use 'llm-tool' or 'direct-tool'");
            }

            return agent;

        } catch (Exception e) {
            throw new ConductorException("Failed to create tool agent '" + role + "': " + e.getMessage(), e);
        }
    }

    /**
     * Creates an LLMToolAgent using the orchestrator's shared memory store.
     * <p>
     * This method now properly uses the orchestrator's shared MemoryStore to ensure
     * memory consistency between agents. All agents created through workflows will
     * share the same memory context, enabling proper inter-agent communication.
     * </p>
     */
    private SubAgent createLLMToolAgent(String agentId, String role, LLMProvider llmProvider, Orchestrator orchestrator) throws ConductorException {
        try {
            // Use the orchestrator's shared memory store to ensure consistency
            MemoryStore sharedMemoryStore = orchestrator.getMemoryStore();
            return new ConversationalAgent(agentId, role, llmProvider, toolRegistry, sharedMemoryStore);
        } catch (Exception e) {
            throw new ConductorException("Failed to create LLM tool agent: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a ToolUsingAgent using the orchestrator's shared memory store.
     * <p>
     * This method now properly uses the orchestrator's shared MemoryStore to ensure
     * memory consistency between agents. All agents created through workflows will
     * share the same memory context, enabling proper inter-agent communication.
     * </p>
     */
    private SubAgent createDirectToolAgent(String agentId, String role, Orchestrator orchestrator) throws ConductorException {
        try {
            // Use the orchestrator's shared memory store to ensure consistency
            MemoryStore sharedMemoryStore = orchestrator.getMemoryStore();
            // Use ConversationalAgent with LLM provider for direct tool mode
            return new ConversationalAgent(agentId, role, getDefaultLLMProvider(), toolRegistry, sharedMemoryStore);
        } catch (Exception e) {
            throw new ConductorException("Failed to create direct tool agent: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a unique agent ID based on the definition.
     */
    private String generateAgentId(AgentDefinition definition) {
        String role = definition.getRole();
        if (role == null || role.isEmpty()) {
            role = "agent";
        }

        // Convert role to a valid agent ID (lowercase, hyphens instead of spaces)
        String agentId = role.toLowerCase()
            .replaceAll("\\s+", "-")
            .replaceAll("[^a-z0-9\\-]", "");

        // Add a timestamp suffix to ensure uniqueness
        long timestamp = System.currentTimeMillis() % 10000;
        return agentId + "-" + timestamp;
    }

    /**
     * Gets the default LLM provider based on configuration.
     * Uses the same configuration system as the demo classes.
     */
    private LLMProvider getDefaultLLMProvider() {
        try {
            // Use the same configuration approach as BookCreationDemo
            com.skanga.conductor.demo.DemoConfig config = com.skanga.conductor.demo.DemoConfig.getInstance();
            String providerType = config.getDemoProviderType();
            String modelName = config.getDemoProviderModel();
            String baseUrl = config.getDemoProviderBaseUrl();

            logger.info("Creating LLM provider: {} with model: {}", providerType, modelName);

            return switch (providerType.toLowerCase()) {
                case "mock" -> new com.skanga.conductor.provider.DemoMockLLMProvider("workflow-demo");
                case "openai" -> new com.skanga.conductor.provider.OpenAiLLMProvider(
                    config.getAppConfig().getString("openai.api.key").orElse(null), modelName, baseUrl);
                case "anthropic" -> new com.skanga.conductor.provider.AnthropicLLMProvider(
                    config.getAppConfig().getString("anthropic.api.key").orElse(null), modelName);
                case "google", "gemini" -> new com.skanga.conductor.provider.GeminiLLMProvider(
                    config.getAppConfig().getString("gemini.api.key").orElse(null), modelName);
                case "ollama" -> new com.skanga.conductor.provider.OllamaLLMProvider(modelName, baseUrl);
                case "localai" -> new com.skanga.conductor.provider.LocalAiLLMProvider(modelName, baseUrl);
                case "azure" -> new com.skanga.conductor.provider.AzureOpenAiLLMProvider(
                    config.getAppConfig().getString("azure.openai.api.key").orElse(null),
                    config.getAppConfig().getString("azure.openai.endpoint").orElse(null),
                    config.getAppConfig().getString("azure.openai.deployment.name").orElse(null));
                case "bedrock" -> new com.skanga.conductor.provider.AmazonBedrockLLMProvider(
                    modelName, config.getAppConfig().getString("aws.region").orElse(null));
                case "oracle" -> new com.skanga.conductor.provider.OracleLLMProvider(
                    config.getAppConfig().getString("oci.compartment.id").orElse(null), modelName);
                default -> {
                    logger.warn("Unknown provider type '{}', falling back to mock", providerType);
                    yield new com.skanga.conductor.provider.DemoMockLLMProvider("workflow-fallback");
                }
            };
        } catch (Exception e) {
            logger.error("Failed to create configured LLM provider, falling back to mock: {}", e.getMessage());
            try {
                return new com.skanga.conductor.provider.DemoMockLLMProvider("workflow-fallback");
            } catch (Exception mockError) {
                logger.error("Even mock provider failed: {}", mockError.getMessage());
                return null;
            }
        }
    }

    /**
     * Validates an agent definition before creating the agent.
     */
    public void validateDefinition(AgentDefinition definition) throws IllegalArgumentException {
        if (definition == null) {
            throw new IllegalArgumentException("Agent definition cannot be null");
        }

        definition.validate();

        // Additional factory-specific validations
        String agentType = definition.getType();
        if (agentType != null) {
            switch (agentType.toLowerCase()) {
                case "llm":
                    validateLLMAgentDefinition(definition);
                    break;
                case "tool":
                    validateToolAgentDefinition(definition);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported agent type: " + agentType);
            }
        }
    }

    /**
     * Validates LLM-specific agent configuration.
     */
    private void validateLLMAgentDefinition(AgentDefinition definition) {
        if (definition.getPromptTemplate() == null || definition.getPromptTemplate().isEmpty()) {
            throw new IllegalArgumentException("LLM agent must have a prompt template");
        }
    }

    /**
     * Validates tool-specific agent configuration.
     */
    private void validateToolAgentDefinition(AgentDefinition definition) {
        String toolStrategy = definition.getProvider(); // Reuse provider field for tool strategy
        if (toolStrategy != null && !toolStrategy.isEmpty()) {
            String strategy = toolStrategy.toLowerCase();
            if (!strategy.equals("llm-tool") && !strategy.equals("direct-tool")) {
                throw new IllegalArgumentException(
                    "Tool agent strategy must be 'llm-tool' or 'direct-tool', got: " + toolStrategy);
            }
        }
        // Note: role and other basic validations are handled by the base definition.validate()
    }

    /**
     * Creates a default tool registry with standard tools for no-code workflows.
     */
    private ToolRegistry createDefaultToolRegistry() {
        ToolRegistry registry = new ToolRegistry();

        try {
            // Register essential tools for workflow operations
            registry.register(new FileReadTool()); // Default constructor uses current directory
            registry.register(new CodeRunnerTool());
            registry.register(new WebSearchTool());
            registry.register(new TextToSpeechTool());

            logger.info("Initialized tool registry with {} tools", 4);
            logger.debug("Available tools: file_read, code_runner, web_search, audio_gen");

        } catch (Exception e) {
            logger.error("Failed to initialize some tools in registry: {}", e.getMessage());
            // Continue with partial registry rather than failing completely
        }

        return registry;
    }
}