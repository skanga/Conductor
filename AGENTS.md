# AGENTS.md

This file provides guidance to ALL coding agents when working with code in this repository.

## Common Development Commands

### Building and Testing
```bash
# Standard build (includes all tests)
mvn clean install

# Quick compile without tests
mvn clean compile -DskipTests

# Run all tests (220+ tests)
mvn test

# Run specific test class
mvn test -Dtest=WorkflowTest

# Run tests with pattern matching
mvn test -Dtest="*Agent*"

# Run specific test method
mvn test -Dtest=WorkflowTest#testMethod

# Run integration tests
mvn verify

# Package the application
mvn package

# Performance Testing (disabled by default for fast builds)
# Standard build - performance tests skipped (~1 minute)
mvn test

# Enable basic performance validation - minimal iterations (~30s additional)
mvn test -Dtest.performance.enabled=true

# Full performance benchmarking - extensive iterations (~3-5min additional)
mvn test -Dtest.performance.intensive=true

# Run specific performance tests
mvn test -Dtest=PromptTemplateEnginePerformanceTest -Dtest.performance.enabled=true
```

### Running Demos
```bash
# Main book creation demo (recommended)
mvn exec:java@book-demo -Dexec.args="Your Topic Here"

# YAML-based workflow demo
mvn exec:java@yaml-demo -Dexec.args="YAML Topic"

# Code-based workflow demo
mvn exec:java@code-demo -Dexec.args="Code Topic"

# Run YAML workflow engine directly
mvn exec:java -Dexec.mainClass="com.skanga.conductor.workflow.runners.WorkflowRunner" \
  -Dexec.args="src/main/resources/yaml/workflows/iterative-book-creation.yaml"

# Run with specific main class (legacy demo)
mvn exec:java -Dexec.mainClass="demo.conductor.com.skanga.conductor.DemoMock"
```

### Development Shortcuts
```bash
# Quick development cycle
mvn compile && mvn exec:java@book-demo -Dexec.args="Test"

# Debug mode with verbose logging
mvn exec:java@book-demo -Dexec.args="Debug Topic" -Dconductor.debug=true

# Clean build artifacts
mvn clean && rm -rf output/ logs/
```

## Architecture Overview

**Conductor** is a Java framework for orchestrating LLM-powered agent workflows with unified execution architecture. It provides multiple orchestration patterns and workflow definition methods.

### Core Components

**Orchestrator** (`com.skanga.conductor.orchestration.Orchestrator`)
- Central coordinator for sub-agent execution and workflow management
- Manages agent registry and shared memory store
- Provides both explicit agent calls (by name) and implicit agent creation (dynamic)

**Unified Workflow Engine** (`com.skanga.conductor.engine.DefaultWorkflowEngine`)
- Common execution primitives for both code-based and YAML workflows
- Ensures identical behavior regardless of workflow definition method
- Thread-safe with local execution state per workflow

**Sub-Agents** (`com.skanga.conductor.agent.*`)
- `SubAgent`: Core interface for all agents with `execute(TaskInput) -> TaskResult`
- `ConversationalAgent`: LLM-powered agents with conversational capabilities and persistent memory
- `SubAgentRegistry`: Manages explicit (pre-defined) agents

**PlannerOrchestrator** (`com.skanga.conductor.orchestration.PlannerOrchestrator`)
- Advanced orchestrator with LLM-based task planning and resumable workflows
- Uses `LLMPlanner` to decompose user requests into structured task plans
- Enables workflow resumption after interruption

**YAML Workflow System** (`com.skanga.conductor.workflow.*`)
- `YamlWorkflowEngine`: Converts YAML configurations to unified engine primitives
- Configuration-driven workflows without code changes
- Uses same underlying execution as code-based workflows

### Key Architectural Patterns

1. **Unified Execution**: Both code-based and YAML workflows use identical `DefaultWorkflowEngine` primitives
2. **Builder Pattern**: `WorkflowBuilder` provides fluent API for programmatic workflow creation
3. **Adapter Pattern**: `YamlWorkflowEngine` adapts YAML configs to unified primitives
4. **Registry Pattern**: `SubAgentRegistry` manages explicit agents by name
5. **Planning Pattern**: LLMPlanner generates TaskDefinition[] from user requests with task chaining

### Memory System

**MemoryStore** (`com.skanga.conductor.memory.*`)
- H2 database-backed persistence for agent memory and workflow state
- **Persistent Workflows**: Plans and task outputs are saved, enabling workflow resumption
- **Agent Memory**: Each agent maintains conversation history across executions
- Connection pooling for concurrent database access

**Database Schema** (H2 at `./data/subagentsdb`):
- Agent memory entries (by agent name)
- Workflow plans (by workflow ID)
- Task outputs (by workflow ID + task name)

### Tool System

Located in `src/main/java/com/skanga/conductor/tools/`:
- **Tool Interface**: Base abstraction for executable tools
- **ToolRegistry**: Central registry for tool discovery and execution
- **Security**: Input validation, command injection prevention, configurable whitelists
- Built-in tools: FileReadTool, WebSearchTool, CodeRunnerTool, TextToSpeechTool

### LLM Integration

**LLM Providers** (`com.skanga.conductor.provider.*`)
- `LLMProvider`: Abstraction for different LLM services
- Integrations: OpenAI, Anthropic, Google Gemini, Azure OpenAI, Amazon Bedrock, Ollama
- `DemoMockLLMProvider`: For testing without real LLM API calls

### Key Workflow Patterns

**Planning and Execution**:
1. **Planning**: LLMPlanner generates TaskDefinition[] from user request
2. **Execution**: Each task creates an implicit ConversationalAgent with placeholder substitution
3. **Chaining**: Tasks can reference previous outputs via `{{prev_output}}` or `{{task_name}}`
4. **Persistence**: All outputs saved for resumability

**Task Dependencies**:
- Dynamic task chaining with variable substitution
- Context management across workflow stages
- Error handling and retry mechanisms

### Package Structure
- `agent/`: Core agent interfaces and implementations
- `orchestration/`: Workflow coordination and task planning
- `engine/`: Unified workflow execution engine and builders
- `workflow/`: YAML-based workflow system and configurations
- `provider/`: LLM provider integrations (OpenAI, Anthropic, etc.)
- `memory/`: Agent state persistence with H2 database
- `tools/`: Tool interfaces for file operations, web search, etc.
- `retry/`: Retry policies (NoRetry, FixedDelay, ExponentialBackoff)
- `metrics/`: Performance monitoring and metrics collection
- `demo/`: Complete demo applications including book creation

## Configuration Management

The application uses a comprehensive configuration system that externalizes all hardcoded values. See [CONFIGURATION.md](CONFIGURATION.md) for detailed documentation.

### Key Configuration Features
- **Hierarchical configuration**: Properties files → System properties → Environment variables
- **Environment-specific configs**: `application.properties`, `application-dev.properties`, `application-prod.properties`
- **Type-safe configuration**: Duration, boolean, numeric, and set parsing with defaults
- **Runtime configuration**: Environment variables and system properties override file values

### Configuration Locations
- Main config: `src/main/resources/application.properties`
- Test config: `src/test/resources/test.properties`
- YAML workflows: `src/main/resources/yaml/workflows/`
- YAML agents: `src/main/resources/yaml/agents/`
- External configs: `external-configs/` directory

### Quick Configuration Examples

Database configuration:
```bash
export CONDUCTOR_DATABASE_URL="jdbc:h2:./data/custom_db;FILE_LOCK=FS"
export CONDUCTOR_DATABASE_USERNAME="custom_user"
```

Tool configuration:
```bash
java -Dconductor.tools.coderunner.timeout=10s \
     -Dconductor.tools.fileread.max.size.bytes=52428800 \
     -jar app.jar
```

## Thread Safety

The framework has been made fully thread-safe for concurrent operations. See [TECHNICAL_FEATURES.md](TECHNICAL_FEATURES.md) for detailed documentation.

### Key Thread Safety Features
- **ConversationalAgent**: Thread-safe memory management with `CopyOnWriteArrayList` and `ReadWriteLock`
- **MemoryStore**: Connection pooling with H2 JDBC connection pool for concurrent database access
- **ApplicationConfig**: Thread-safe singleton with double-check locking pattern
- **Registries**: `ConcurrentHashMap` for thread-safe agent and tool registration

### Concurrency Patterns Used
- **Copy-on-Write Collections**: For read-heavy memory operations
- **Connection Pooling**: Prevents database connection contention
- **Read-Write Locks**: Multiple concurrent readers, exclusive writers
- **Double-Check Locking**: Minimizes synchronization overhead
- **Concurrent Collections**: Lock-free operations for registries

### Performance Considerations
- Configurable connection pool size: `conductor.database.max.connections`
- Read-optimized memory access patterns
- Fine-grained locking to minimize contention
- Resource cleanup with try-with-resources pattern

## Testing Infrastructure

The framework includes comprehensive unit test coverage with 220+ tests covering all major components.

### Test Categories
- **Unit Tests**: Isolated component testing with mocked dependencies (JUnit 5 + Mockito)
- **Integration Tests**: Component interaction and workflow validation
- **Security Tests**: Input validation, access control, and attack prevention
- **Performance Tests**: Template rendering, caching, concurrent execution, and memory usage
  - **Disabled by default** to maintain fast CI builds (~1 minute)
  - **Enable with**: `-Dtest.performance.enabled=true` (basic validation)
  - **Intensive mode**: `-Dtest.performance.intensive=true` (full benchmarking)

### Key Test Coverage
- **Agent System**: ConversationalAgent behavior and error handling
- **Retry System**: All retry policies, exception handling, and timeout scenarios
- **Metrics System**: TimerContext, InMemoryMetricsCollector, and aggregation
- **Tool System**: Secure execution, injection prevention, and whitelisting
- **Memory System**: Storage, retrieval, and transaction handling
- **Configuration**: Validation, security, and property management

### Testing Strategy
- Test categories: unit tests (70%), integration tests (20%), end-to-end tests (10%)
- Mock LLM responses using `DemoMockLLMProvider` for consistent testing
- Thread safety and concurrency tests included

## Development Guidelines

### Code Patterns
- All agents implement `SubAgent` interface with `execute(TaskInput)` method
- Use `TaskResult.success(output)` and `TaskResult.failure(error)` for results
- Leverage `MemoryStore` for persistent agent state across sessions
- Apply retry policies through `RetryExecutor` for resilient operations

### Output Management
- Demo outputs: `output/book_[topic]_[timestamp]/`
- Logs: `logs/` directory
- Test outputs: `target/test-output/`

## Framework Usage Patterns

### Creating Workflows Programmatically
```java
List<StageDefinition> stages = WorkflowBuilder.create()
    .addStage("title-generation", "title-generator", "Expert title generator",
              llmProvider, systemPrompt, promptTemplate, maxRetries, validator, metadata)
    .build();

DefaultWorkflowEngine engine = new DefaultWorkflowEngine(orchestrator);
WorkflowResult result = engine.executeWorkflow(stages);
```

### Using YAML Workflows
```java
YamlWorkflowEngine adapter = new YamlWorkflowEngine(orchestrator, llmProvider);
adapter.loadConfiguration("workflow.yaml", "agents.yaml", "context.yaml");
WorkflowResult result = adapter.executeWorkflow(context);
```

### Creating Explicit Agents
```java
ConversationalAgent agent = new ConversationalAgent(
    "agent-name", "description", llmProvider, "prompt template", memoryStore);
registry.register(agent);
ExecutionResult result = orchestrator.callExplicit("agent-name", input);
```

## Common Development Tasks

### Adding New LLM Providers
1. Extend `AbstractLLMProvider` or implement `LLMProvider`
2. Add provider configuration to `application.properties`
3. Register in provider factory if needed
4. Add comprehensive tests

### Creating Custom Tools
1. Implement `Tool` interface with `execute(String input)` method
2. Add security validation and input sanitization
3. Register with `ToolRegistry`
4. Add to security whitelist if needed

### Implementing New Agent Types
1. Implement `SubAgent` interface
2. Add constructor with name, description, and dependencies
3. Implement `execute(TaskInput)` with proper error handling
4. Add comprehensive unit tests

### Adding YAML Workflow Features
1. Update configuration models in `workflow.config` package
2. Extend `YamlWorkflowEngine` conversion logic
3. Ensure compatibility with `DefaultWorkflowEngine` primitives
4. Add test cases for new YAML features

### Testing Best Practices
1. **Always run tests before committing**: `mvn test`
2. **Write tests for new features**: Follow AAA pattern (Arrange, Act, Assert)
3. **Use mocks for dependencies**: Isolate units under test
4. **Test both happy path and edge cases**: Validate error handling
5. **Maintain test independence**: No shared state between tests

## Troubleshooting

### Build Issues
- Ensure Java 21+ is installed: `java --version`
- Clear Maven cache: `rm -rf ~/.m2/repository/com/skanga`
- Increase Maven memory: `export MAVEN_OPTS="-Xmx4g"`

### Test Issues
- Clean test databases: `find target -name "*.db" -delete`
- Run specific failing test: `mvn test -Dtest=FailingTest`
- Check test logs in `target/surefire-reports/`

### Demo Issues
- Check output directory permissions
- Verify LLM provider configuration
- Review logs in `logs/` directory
- Use mock provider for testing: `-Dconductor.llm.provider=mock`

See [DEVELOPMENT.md](DEVELOPMENT.md) for comprehensive testing documentation and guidelines.