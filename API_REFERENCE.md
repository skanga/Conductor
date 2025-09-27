# Conductor Framework - API Reference Guide

This comprehensive reference provides detailed documentation for all public APIs, interfaces, and extension points in the Conductor AI framework.

## Table of Contents

- [Core APIs](#core-apis)
- [Agent APIs](#agent-apis)
- [Workflow APIs](#workflow-apis)
- [LLM Provider APIs](#llm-provider-apis)
- [Tool APIs](#tool-apis)
- [Memory Store APIs](#memory-store-apis)
- [Configuration APIs](#configuration-apis)
- [Extension Points](#extension-points)
- [HTTP REST APIs](#http-rest-apis)

---

## Core APIs

### Orchestrator

The central coordination component of the Conductor framework.

```java
public class Orchestrator {

    /**
     * Creates and executes an implicit agent for a one-time task.
     *
     * @param agentName unique identifier for the agent
     * @param description human-readable description of the agent's purpose
     * @param llmProvider the LLM provider to use for this agent
     * @param systemPrompt the system prompt template for the agent
     * @return the created implicit agent
     * @throws ConductorException if agent creation fails
     */
    public SubAgent createImplicitAgent(String agentName, String description,
                                       LLMProvider llmProvider, String systemPrompt)
            throws ConductorException;

    /**
     * Calls an explicit (registered) agent by name.
     *
     * @param agentName the name of the registered agent
     * @param input the task input for the agent
     * @return the execution result
     * @throws ConductorException if the agent is not found or execution fails
     */
    public TaskResult callExplicit(String agentName, TaskInput input)
            throws ConductorException;

    /**
     * Gets the shared memory store used by this orchestrator.
     *
     * @return the memory store instance
     */
    public MemoryStore getMemoryStore();

    /**
     * Gets the agent registry for managing explicit agents.
     *
     * @return the agent registry
     */
    public SubAgentRegistry getRegistry();
}
```

### SubAgent Interface

The base interface for all agents in the framework.

```java
public interface SubAgent {

    /**
     * Gets the unique name of this agent.
     *
     * @return the agent name
     */
    String getName();

    /**
     * Gets the description of this agent's capabilities.
     *
     * @return the agent description
     */
    String getDescription();

    /**
     * Executes the agent with the given input.
     *
     * @param input the execution input containing request and context
     * @return the execution result
     * @throws ConductorException if execution fails
     */
    ExecutionResult execute(ExecutionInput input) throws ConductorException;

    /**
     * Checks if this agent is currently healthy and operational.
     *
     * @return true if the agent is healthy, false otherwise
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * Gets metadata about this agent's configuration and capabilities.
     *
     * @return agent metadata
     */
    default AgentMetadata getMetadata() {
        return AgentMetadata.builder()
            .name(getName())
            .description(getDescription())
            .build();
    }
}
```

---

## Agent APIs

### ConversationalAgent

LLM-powered agent for conversational interactions.

```java
public class ConversationalAgent implements SubAgent {

    /**
     * Creates a new conversational agent.
     *
     * @param name unique agent identifier
     * @param description agent description
     * @param llmProvider the LLM provider to use
     * @param toolRegistry registry of available tools
     * @param memoryStore memory store for persistence
     */
    public ConversationalAgent(String name, String description,
                              LLMProvider llmProvider, ToolRegistry toolRegistry,
                              MemoryStore memoryStore);

    /**
     * Executes the agent with conversation context.
     *
     * @param input execution input with user message and context
     * @return execution result with agent response
     * @throws ConductorException if execution fails
     */
    @Override
    public ExecutionResult execute(ExecutionInput input) throws ConductorException;

    /**
     * Sets the system prompt template for this agent.
     *
     * @param systemPrompt the system prompt template with placeholders
     */
    public void setSystemPrompt(String systemPrompt);

    /**
     * Gets the current conversation history for debugging.
     *
     * @return list of conversation messages
     */
    public List<ConversationMessage> getConversationHistory();
}
```

### AgentFactory

Factory for creating different types of agents.

```java
public class AgentFactory {

    /**
     * Creates an LLM-based tool-using agent.
     *
     * @param agentDefinition the agent configuration
     * @param orchestrator the orchestrator instance
     * @return created agent
     * @throws ConductorException if creation fails
     */
    public SubAgent createLLMToolAgent(AgentDefinition agentDefinition,
                                      Orchestrator orchestrator)
            throws ConductorException;

    /**
     * Creates a direct tool execution agent (no LLM).
     *
     * @param agentDefinition the agent configuration
     * @param orchestrator the orchestrator instance
     * @return created agent
     * @throws ConductorException if creation fails
     */
    public SubAgent createDirectToolAgent(AgentDefinition agentDefinition,
                                         Orchestrator orchestrator)
            throws ConductorException;

    /**
     * Creates an agent based on configuration.
     *
     * @param agentDefinition configuration specifying agent type and settings
     * @param orchestrator the orchestrator instance
     * @return created agent matching the configuration
     * @throws ConductorException if creation fails
     */
    public SubAgent createAgent(AgentDefinition agentDefinition,
                               Orchestrator orchestrator)
            throws ConductorException;
}
```

---

## Workflow APIs

### UnifiedWorkflowEngine

Core workflow execution engine supporting both programmatic and YAML workflows.

```java
public class UnifiedWorkflowEngine implements WorkflowEngine {

    /**
     * Creates a workflow engine with the given orchestrator.
     *
     * @param orchestrator the orchestrator for agent management
     */
    public UnifiedWorkflowEngine(Orchestrator orchestrator);

    /**
     * Executes a workflow defined by stage definitions.
     *
     * @param stages list of workflow stages to execute
     * @return workflow execution result
     * @throws ConductorException if workflow execution fails
     */
    public WorkflowResult executeWorkflow(List<StageDefinition> stages)
            throws ConductorException;

    /**
     * Executes a workflow with initial context variables.
     *
     * @param stages list of workflow stages to execute
     * @param initialVariables initial context variables for the workflow
     * @return workflow execution result
     * @throws ConductorException if workflow execution fails
     */
    public WorkflowResult executeWorkflowWithContext(List<StageDefinition> stages,
                                                   Map<String, Object> initialVariables)
            throws ConductorException;

    /**
     * Validates a workflow definition without executing it.
     *
     * @param stages the workflow stages to validate
     * @return validation result with any issues found
     */
    public WorkflowValidationResult validateWorkflow(List<StageDefinition> stages);
}
```

### WorkflowBuilder

Fluent builder for creating workflow definitions programmatically.

```java
public class WorkflowBuilder {

    /**
     * Creates a new workflow builder.
     *
     * @return new builder instance
     */
    public static WorkflowBuilder create();

    /**
     * Adds a basic stage to the workflow.
     *
     * @param stageName unique stage identifier
     * @param agentName name of the agent to use
     * @param agentDescription description of the agent's role
     * @param llmProvider LLM provider for the agent
     * @param systemPrompt system prompt template
     * @param promptTemplate user prompt template
     * @return this builder for chaining
     */
    public WorkflowBuilder addStage(String stageName, String agentName,
                                   String agentDescription, LLMProvider llmProvider,
                                   String systemPrompt, String promptTemplate);

    /**
     * Adds a stage with retry configuration.
     *
     * @param stageName unique stage identifier
     * @param agentName name of the agent to use
     * @param agentDescription description of the agent's role
     * @param llmProvider LLM provider for the agent
     * @param systemPrompt system prompt template
     * @param promptTemplate user prompt template
     * @param maxRetries maximum number of retry attempts
     * @return this builder for chaining
     */
    public WorkflowBuilder addStage(String stageName, String agentName,
                                   String agentDescription, LLMProvider llmProvider,
                                   String systemPrompt, String promptTemplate,
                                   int maxRetries);

    /**
     * Adds a stage with result validation.
     *
     * @param stageName unique stage identifier
     * @param agentName name of the agent to use
     * @param agentDescription description of the agent's role
     * @param llmProvider LLM provider for the agent
     * @param systemPrompt system prompt template
     * @param promptTemplate user prompt template
     * @param validator function to validate stage results
     * @return this builder for chaining
     */
    public WorkflowBuilder addStage(String stageName, String agentName,
                                   String agentDescription, LLMProvider llmProvider,
                                   String systemPrompt, String promptTemplate,
                                   Function<StageResult, ValidationResult> validator);

    /**
     * Builds the workflow definition.
     *
     * @return list of stage definitions
     */
    public List<StageDefinition> build();

    // Utility validation methods

    /**
     * Creates a validator that checks if output contains required text.
     *
     * @param requiredText text that must be present in the output
     * @return validation function
     */
    public static Function<StageResult, ValidationResult> containsValidator(String requiredText);

    /**
     * Creates a validator that checks minimum output length.
     *
     * @param minLength minimum required output length in characters
     * @return validation function
     */
    public static Function<StageResult, ValidationResult> minLengthValidator(int minLength);

    /**
     * Creates a validator that rejects forbidden text patterns.
     *
     * @param forbiddenTexts text patterns that are not allowed
     * @return validation function
     */
    public static Function<StageResult, ValidationResult> forbiddenTextValidator(String... forbiddenTexts);
}
```

### YamlWorkflowEngine

Adapter for executing YAML-defined workflows using the unified engine.

```java
public class YamlWorkflowEngine {

    /**
     * Creates adapter with orchestrator and default LLM provider.
     *
     * @param orchestrator orchestrator for agent management
     * @param defaultLlmProvider default LLM provider for agents
     */
    public YamlWorkflowEngine(Orchestrator orchestrator, LLMProvider defaultLlmProvider);

    /**
     * Loads configuration from YAML files.
     *
     * @param workflowPath path to workflow definition YAML
     * @param agentsPath path to agents configuration YAML
     * @param contextPath path to context variables YAML
     * @throws ConfigurationException if loading fails
     */
    public void loadConfiguration(String workflowPath, String agentsPath, String contextPath)
            throws ConfigurationException;

    /**
     * Executes the loaded workflow with provided context.
     *
     * @param executionContext runtime context variables
     * @return workflow execution result
     * @throws ConductorException if execution fails
     */
    public WorkflowResult executeWorkflow(Map<String, Object> executionContext)
            throws ConductorException;

    /**
     * Validates the loaded workflow configuration.
     *
     * @return validation result
     */
    public WorkflowValidationResult validateConfiguration();
}
```

---

## LLM Provider APIs

### LLMProvider Interface

Base interface for all LLM providers.

```java
public interface LLMProvider {

    /**
     * Gets the provider name.
     *
     * @return provider identifier
     */
    String getProviderName();

    /**
     * Gets the model name being used.
     *
     * @return model identifier
     */
    String getModelName();

    /**
     * Generates a response for the given prompt.
     *
     * @param prompt the input prompt
     * @return generated response
     * @throws LLMProviderException if generation fails
     */
    String generate(String prompt) throws LLMProviderException;

    /**
     * Generates a response with conversation history.
     *
     * @param messages conversation history
     * @return generated response
     * @throws LLMProviderException if generation fails
     */
    String generateWithHistory(List<ChatMessage> messages) throws LLMProviderException;

    /**
     * Checks if the provider is currently healthy.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Gets provider-specific metrics and statistics.
     *
     * @return provider metrics
     */
    ProviderMetrics getMetrics();
}
```

### AbstractLLMProvider

Base implementation providing common functionality.

```java
public abstract class AbstractLLMProvider implements LLMProvider {

    /**
     * Creates a provider with name, model, and retry policy.
     *
     * @param providerName unique provider identifier
     * @param modelName the model to use
     * @param retryPolicy retry policy for failed requests
     */
    public AbstractLLMProvider(String providerName, String modelName, RetryPolicy retryPolicy);

    /**
     * Performs the actual LLM call - implemented by concrete providers.
     *
     * @param prompt the input prompt
     * @return generated response
     * @throws LLMProviderException if the call fails
     */
    protected abstract String doGenerate(String prompt) throws LLMProviderException;

    /**
     * Validates the prompt before sending to LLM.
     *
     * @param prompt prompt to validate
     * @throws IllegalArgumentException if prompt is invalid
     */
    protected void validatePrompt(String prompt) throws IllegalArgumentException;

    /**
     * Applies retry logic with exponential backoff.
     *
     * @param operation the operation to retry
     * @return operation result
     * @throws LLMProviderException if all retries fail
     */
    protected <T> T executeWithRetry(Supplier<T> operation) throws LLMProviderException;
}
```

### Concrete Provider Examples

#### OpenAiLLMProvider

```java
public class OpenAiLLMProvider extends AbstractLLMProvider {

    /**
     * Creates OpenAI provider with API configuration.
     *
     * @param providerName provider identifier
     * @param modelName OpenAI model name (e.g., "gpt-4", "gpt-3.5-turbo")
     * @param apiKey OpenAI API key
     * @param retryPolicy retry policy for failed requests
     */
    public OpenAiLLMProvider(String providerName, String modelName,
                            String apiKey, RetryPolicy retryPolicy);

    /**
     * Sets additional OpenAI-specific parameters.
     *
     * @param temperature sampling temperature (0.0-2.0)
     * @param maxTokens maximum tokens in response
     * @param topP nucleus sampling parameter
     */
    public void setParameters(double temperature, int maxTokens, double topP);
}
```

#### AnthropicLLMProvider

```java
public class AnthropicLLMProvider extends AbstractLLMProvider {

    /**
     * Creates Anthropic provider with API configuration.
     *
     * @param providerName provider identifier
     * @param modelName Anthropic model name (e.g., "claude-3-sonnet-20240229")
     * @param apiKey Anthropic API key
     * @param retryPolicy retry policy for failed requests
     */
    public AnthropicLLMProvider(String providerName, String modelName,
                               String apiKey, RetryPolicy retryPolicy);

    /**
     * Sets Anthropic-specific parameters.
     *
     * @param temperature sampling temperature
     * @param maxTokens maximum tokens in response
     * @param stopSequences sequences that stop generation
     */
    public void setParameters(double temperature, int maxTokens, List<String> stopSequences);
}
```

---

## Tool APIs

### Tool Interface

Base interface for all tools that agents can use.

```java
public interface Tool {

    /**
     * Gets the unique name of this tool.
     *
     * @return tool name
     */
    String getName();

    /**
     * Gets a description of what this tool does.
     *
     * @return tool description
     */
    String getDescription();

    /**
     * Gets the input schema for this tool.
     *
     * @return JSON schema describing expected input format
     */
    String getInputSchema();

    /**
     * Executes the tool with the given input.
     *
     * @param input tool input parameters
     * @return execution result
     * @throws ToolExecutionException if execution fails
     */
    ToolResult execute(ToolInput input) throws ToolExecutionException;

    /**
     * Validates that the tool can execute with the given input.
     *
     * @param input input to validate
     * @return validation result
     */
    default ToolValidationResult validate(ToolInput input) {
        try {
            validateInput(input);
            return ToolValidationResult.valid();
        } catch (Exception e) {
            return ToolValidationResult.invalid(e.getMessage());
        }
    }

    /**
     * Validates input parameters.
     *
     * @param input input to validate
     * @throws IllegalArgumentException if input is invalid
     */
    default void validateInput(ToolInput input) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException("Tool input cannot be null");
        }
    }
}
```

### ToolRegistry

Registry for managing available tools.

```java
public class ToolRegistry {

    /**
     * Registers a tool with the registry.
     *
     * @param tool tool to register
     * @throws IllegalArgumentException if tool name conflicts
     */
    public void registerTool(Tool tool);

    /**
     * Gets a tool by name.
     *
     * @param toolName name of the tool
     * @return the tool instance
     * @throws ToolNotFoundException if tool is not registered
     */
    public Tool getTool(String toolName) throws ToolNotFoundException;

    /**
     * Gets all registered tools.
     *
     * @return map of tool names to tool instances
     */
    public Map<String, Tool> getAllTools();

    /**
     * Checks if a tool is registered.
     *
     * @param toolName name to check
     * @return true if tool is registered
     */
    public boolean hasTool(String toolName);

    /**
     * Unregisters a tool.
     *
     * @param toolName name of tool to remove
     * @return the removed tool, or null if not found
     */
    public Tool unregisterTool(String toolName);

    /**
     * Gets tools matching a category or tag.
     *
     * @param category category to filter by
     * @return list of matching tools
     */
    public List<Tool> getToolsByCategory(String category);
}
```

### Built-in Tool Examples

#### FileReadTool

```java
public class FileReadTool implements Tool {

    @Override
    public String getName() {
        return "file_read";
    }

    @Override
    public String getDescription() {
        return "Reads the contents of a file from the filesystem";
    }

    @Override
    public ToolResult execute(ToolInput input) throws ToolExecutionException {
        String filePath = input.getParameter("file_path");

        try {
            String content = Files.readString(Paths.get(filePath));
            return ToolResult.success(content);
        } catch (IOException e) {
            throw new ToolExecutionException("Failed to read file: " + filePath, e);
        }
    }
}
```

#### WebSearchTool

```java
public class WebSearchTool implements Tool {

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "Performs web search and returns relevant results";
    }

    @Override
    public ToolResult execute(ToolInput input) throws ToolExecutionException {
        String query = input.getParameter("query");
        int maxResults = input.getParameterAsInt("max_results", 5);

        try {
            List<SearchResult> results = searchEngine.search(query, maxResults);
            return ToolResult.success(formatResults(results));
        } catch (Exception e) {
            throw new ToolExecutionException("Web search failed: " + e.getMessage(), e);
        }
    }
}
```

---

## Memory Store APIs

### MemoryStore Interface

Core interface for agent memory persistence.

```java
public interface MemoryStore extends AutoCloseable {

    /**
     * Stores a key-value pair for an agent.
     *
     * @param agentId the agent identifier
     * @param key the storage key
     * @param value the value to store
     * @throws MemoryStoreException if storage fails
     */
    void store(String agentId, String key, String value) throws MemoryStoreException;

    /**
     * Retrieves a value for an agent.
     *
     * @param agentId the agent identifier
     * @param key the storage key
     * @return the stored value, or null if not found
     * @throws MemoryStoreException if retrieval fails
     */
    String retrieve(String agentId, String key) throws MemoryStoreException;

    /**
     * Retrieves all keys for an agent.
     *
     * @param agentId the agent identifier
     * @return set of all keys for the agent
     * @throws MemoryStoreException if retrieval fails
     */
    Set<String> getAllKeys(String agentId) throws MemoryStoreException;

    /**
     * Removes a key-value pair for an agent.
     *
     * @param agentId the agent identifier
     * @param key the key to remove
     * @return true if the key was removed, false if it didn't exist
     * @throws MemoryStoreException if removal fails
     */
    boolean remove(String agentId, String key) throws MemoryStoreException;

    /**
     * Removes all data for an agent.
     *
     * @param agentId the agent identifier
     * @throws MemoryStoreException if cleanup fails
     */
    void clear(String agentId) throws MemoryStoreException;

    /**
     * Checks if the memory store is healthy and operational.
     *
     * @return true if healthy, false otherwise
     */
    boolean isHealthy();

    /**
     * Gets statistics about memory usage.
     *
     * @return memory store statistics
     */
    MemoryStoreStats getStats();
}
```

### H2MemoryStore

SQL-based memory store implementation using H2 database.

```java
public class H2MemoryStore implements MemoryStore {

    /**
     * Creates H2 memory store with database URL.
     *
     * @param databaseUrl JDBC URL for H2 database
     * @throws MemoryStoreException if initialization fails
     */
    public H2MemoryStore(String databaseUrl) throws MemoryStoreException;

    /**
     * Creates H2 memory store with full configuration.
     *
     * @param databaseUrl JDBC URL for H2 database
     * @param username database username
     * @param password database password
     * @param maxConnections maximum connection pool size
     * @throws MemoryStoreException if initialization fails
     */
    public H2MemoryStore(String databaseUrl, String username, String password, int maxConnections)
            throws MemoryStoreException;

    /**
     * Executes database maintenance operations.
     * This includes cleanup of old entries and optimization.
     */
    public void performMaintenance();

    /**
     * Gets the current database connection pool statistics.
     *
     * @return connection pool statistics
     */
    public ConnectionPoolStats getConnectionPoolStats();
}
```

---

## Configuration APIs

### ApplicationConfig

Main configuration class for the framework.

```java
@ConfigurationProperties("conductor")
public class ApplicationConfig {

    /**
     * Gets database configuration.
     *
     * @return database configuration
     */
    public DatabaseConfig getDatabase();

    /**
     * Gets LLM provider configurations.
     *
     * @return list of LLM provider configurations
     */
    public List<LLMProviderConfig> getLlmProviders();

    /**
     * Gets workflow configuration.
     *
     * @return workflow configuration
     */
    public WorkflowConfig getWorkflow();

    /**
     * Gets security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurity();

    /**
     * Validates the configuration.
     *
     * @throws ConfigurationException if configuration is invalid
     */
    @PostConstruct
    public void validate() throws ConfigurationException;
}
```

### ConfigurationTool

Tool for runtime configuration access and management.

```java
public class ConfigurationTool implements Tool {

    @Override
    public String getName() {
        return "configuration";
    }

    @Override
    public ToolResult execute(ToolInput input) throws ToolExecutionException {
        String action = input.getParameter("action");

        switch (action) {
            case "get":
                return handleGet(input);
            case "set":
                return handleSet(input);
            case "list":
                return handleList(input);
            default:
                throw new ToolExecutionException("Unknown action: " + action);
        }
    }

    private ToolResult handleGet(ToolInput input) {
        String propertyName = input.getParameter("property");
        Object value = configurationManager.getProperty(propertyName);
        return ToolResult.success(String.valueOf(value));
    }
}
```

---

## Extension Points

### Custom Agent Implementation

```java
public class CustomBusinessAgent implements SubAgent {

    private final String name;
    private final BusinessLogicService businessService;
    private final MemoryStore memoryStore;

    public CustomBusinessAgent(String name, BusinessLogicService businessService,
                              MemoryStore memoryStore) {
        this.name = requireNonNull(name, "name cannot be null");
        this.businessService = requireNonNull(businessService, "businessService cannot be null");
        this.memoryStore = requireNonNull(memoryStore, "memoryStore cannot be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Custom agent for business-specific operations";
    }

    @Override
    public ExecutionResult execute(ExecutionInput input) throws ConductorException {
        try {
            // Custom business logic implementation
            BusinessRequest request = parseBusinessRequest(input);
            BusinessResult result = businessService.processRequest(request);

            // Store result in memory for future reference
            memoryStore.store(name, "last_result", result.toJson());

            return ExecutionResult.success(result.getFormattedOutput());

        } catch (Exception e) {
            throw new ConductorException("Business agent execution failed", e);
        }
    }

    private BusinessRequest parseBusinessRequest(ExecutionInput input) {
        // Parse domain-specific input format
        return BusinessRequest.fromJson(input.content());
    }
}
```

### Custom Tool Implementation

```java
public class DatabaseQueryTool implements Tool {

    private final DataSource dataSource;
    private final QueryValidator validator;

    public DatabaseQueryTool(DataSource dataSource, QueryValidator validator) {
        this.dataSource = requireNonNull(dataSource, "dataSource cannot be null");
        this.validator = requireNonNull(validator, "validator cannot be null");
    }

    @Override
    public String getName() {
        return "database_query";
    }

    @Override
    public String getDescription() {
        return "Executes read-only database queries and returns results";
    }

    @Override
    public String getInputSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "query": {
                  "type": "string",
                  "description": "SQL query to execute (SELECT only)"
                },
                "max_rows": {
                  "type": "integer",
                  "minimum": 1,
                  "maximum": 1000,
                  "default": 100,
                  "description": "Maximum number of rows to return"
                }
              },
              "required": ["query"]
            }
            """;
    }

    @Override
    public ToolResult execute(ToolInput input) throws ToolExecutionException {
        String query = input.getParameter("query");
        int maxRows = input.getParameterAsInt("max_rows", 100);

        // Validate query for security
        if (!validator.isSelectQuery(query)) {
            throw new ToolExecutionException("Only SELECT queries are allowed");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setMaxRows(maxRows);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> results = new ArrayList<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }

                return ToolResult.success(JsonUtils.toJson(results));
            }
        } catch (SQLException e) {
            throw new ToolExecutionException("Database query failed: " + e.getMessage(), e);
        }
    }
}
```

### Custom LLM Provider Implementation

```java
public class CustomLLMProvider extends AbstractLLMProvider {

    private final CustomLLMClient client;

    public CustomLLMProvider(String providerName, String modelName,
                            CustomLLMClient client, RetryPolicy retryPolicy) {
        super(providerName, modelName, retryPolicy);
        this.client = requireNonNull(client, "client cannot be null");
    }

    @Override
    protected String doGenerate(String prompt) throws LLMProviderException {
        try {
            CustomLLMRequest request = CustomLLMRequest.builder()
                .prompt(prompt)
                .model(getModelName())
                .maxTokens(2000)
                .temperature(0.7)
                .build();

            CustomLLMResponse response = client.generate(request);

            if (!response.isSuccessful()) {
                throw new LLMProviderException("LLM request failed: " + response.getError());
            }

            return response.getGeneratedText();

        } catch (Exception e) {
            if (e instanceof LLMProviderException) {
                throw e;
            }
            throw new LLMProviderException("Custom LLM provider failed", e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            return client.healthCheck();
        } catch (Exception e) {
            logger.warn("Health check failed for custom LLM provider", e);
            return false;
        }
    }
}
```

---

## HTTP REST APIs

When running in server mode, Conductor exposes REST APIs for external integration.

### Workflow Execution API

```http
POST /api/workflows/execute
Content-Type: application/json
Authorization: Bearer <api-token>

{
  "workflowId": "book-creation",
  "input": {
    "topic": "Machine Learning Fundamentals",
    "author": "Jane Doe"
  },
  "context": {
    "outputFormat": "markdown",
    "targetAudience": "beginners"
  }
}
```

Response:
```json
{
  "executionId": "exec-123",
  "status": "completed",
  "result": {
    "success": true,
    "output": "Book creation completed successfully",
    "artifacts": [
      {
        "type": "file",
        "path": "/output/book_machine_learning_20240101/book.md",
        "size": 45621
      }
    ]
  },
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-01T10:15:30Z",
  "duration": "PT15M30S"
}
```

### Agent Management API

```http
GET /api/agents
Authorization: Bearer <api-token>
```

Response:
```json
{
  "agents": [
    {
      "name": "title-generator",
      "type": "conversational",
      "description": "Generates creative titles for content",
      "status": "healthy",
      "metadata": {
        "llmProvider": "openai",
        "model": "gpt-4",
        "created": "2024-01-01T09:00:00Z"
      }
    }
  ]
}
```

### Health Check API

```http
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "connectionPool": "50/50",
        "responseTime": "5ms"
      }
    },
    "llmProviders": {
      "status": "UP",
      "details": {
        "openai": "UP",
        "anthropic": "UP"
      }
    },
    "memoryStore": {
      "status": "UP",
      "details": {
        "totalEntries": 1542,
        "memoryUsage": "45MB"
      }
    }
  }
}
```

This comprehensive API reference provides detailed documentation for integrating with and extending the Conductor framework through its well-defined interfaces and extension points.