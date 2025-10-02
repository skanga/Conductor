# Conductor: A Subagent Architecture Framework in Java

Conductor is a Java-based framework for building sophisticated AI applications using a subagent architecture. It provides a robust and flexible platform for orchestrating multiple specialized AI agents to accomplish complex tasks. This project is inspired by the concepts outlined in "The Rise of Subagents" by
Phil Schmid at https://www.philschmid.de/the-rise-of-subagents and it aims to provide a practical implementation of this powerful architectural pattern.

## Core Concepts

The Conductor framework is built around the idea of a central **Orchestrator** that manages a fleet of **Subagents**. Each subagent is a specialized AI agent designed to perform a specific task. This architecture allows for better modularity, reusability, and scalability of AI applications.

### Orchestrator

The `Orchestrator` is the heart of the framework. It is responsible for:

*   **Task Decomposition**: Using an `LLMPlanner`, the orchestrator can break down a high-level user request into a series of smaller, manageable tasks.
*   **Agent Management**: The orchestrator manages the lifecycle of subagents, including their creation, execution, and persistence.
*   **Workflow Coordination**: The orchestrator coordinates the execution of subagents, chaining them together to create complex workflows.

### Subagents

Subagents are the workhorses of the framework. They are specialized AI agents that can be either:

*   **Explicit**: Pre-defined, long-lived agents that are registered with the orchestrator and can be called by name.
*   **Implicit**: Temporary, on-the-fly agents that are created dynamically to handle a specific task.

The framework provides a `SubAgent` interface which is implemented by `ConversationalAgent` which is a subagent that uses a Large Language Model (LLM) to perform its task with conversational capabilities.

## Features

*   **Subagent Architecture**: A clean and powerful implementation of the subagent architecture.
*   **LLM-based Planning**: An `LLMPlanner` that can dynamically decompose user requests into a sequence of tasks.
*   **Explicit and Implicit Agents**: Support for both pre-defined and on-the-fly agents.
*   **Memory Persistence**: A `MemoryStore` that allows agents to maintain state across sessions.
*   **Tool Use**: A flexible system for both programmatic and LLM-assisted tool use.
*   **Retry System**: Robust retry mechanisms with configurable policies (NoRetry, FixedDelay, ExponentialBackoff).
*   **Metrics Collection**: Comprehensive metrics and monitoring system with timer contexts and aggregated summaries.
*   **Security**: Secure tool execution with command injection prevention and configurable whitelists.
*   **Comprehensive Testing**: Extensive unit test coverage with 220+ tests using JUnit 5 and Mockito.
*   **YAML Workflows**: YAML-based workflow definition system enabling configuration-driven AI applications without code changes.
*   **Unified Architecture**: Both code-based and YAML-configured workflows use identical underlying execution primitives, ensuring consistent behavior.
*   **Human Approval System**: Built-in approval workflows with interactive console interface and timeout support.
*   **Extensible**: The framework is designed to be extensible, allowing you to create your own custom agents, tools, and providers.
*   **Java-based**: Built on Java 21 and `langchain4j`, providing a modern and robust foundation.

## Getting Started

### Quick Start (2 Minutes)

**Prerequisites**: Java 21+ and Maven 3.6+

```bash
# Clone and run demo in one command
git clone https://github.com/skanga/conductor && cd Conductor
mvn clean install && mvn exec:java@book-demo -Dexec.args="Quick Demo"
```

**Expected Output**: ✅ Book creation completed! Check `output/` folder for generated content.

### Development Setup

For comprehensive development environment setup including IDE configuration, testing setup, and troubleshooting guides, see **[DEVELOPER_SETUP.md](DEVELOPER_SETUP.md)**.

#### Prerequisites

| Requirement | Minimum | Recommended | Purpose |
|-------------|---------|-------------|---------|
| **Java** | 21.0.0 | 21.0.8+ LTS | Core runtime |
| **Maven** | 3.6.0 | 3.9.0+ | Build management |
| **Memory** | 2 GB RAM | 8 GB RAM | LLM operations |
| **Storage** | 500 MB | 2 GB | Models and outputs |

#### Quick Environment Check
```bash
java --version    # Should show 21.x.x
mvn --version     # Should show 3.6+
echo $JAVA_HOME   # Should point to Java 21
```

### Building the Project

#### Standard Build
```bash
mvn clean install
```

#### Development Build (Faster)
```bash
mvn clean compile -DskipTests    # Skip tests for faster iteration
mvn test -Dtest=ClassName        # Run specific tests
```

#### Build Profiles
```bash
mvn clean install -Pdev          # Development profile
mvn clean install -Pproduction   # Production optimizations
```

### Running Demos

#### 1. Book Creation Demo (Recommended)
Showcases comprehensive multi-agent book writing workflow:

```bash
# Basic usage
mvn exec:java@book-demo -Dexec.args="AI-Powered Content Creation"

# With custom output directory
mvn exec:java@book-demo -Dexec.args="Your Topic" -Dconductor.output.dir="./my-books"

# Debug mode with verbose logging
mvn exec:java@book-demo -Dexec.args="Debug Topic" -Dconductor.debug=true
```

**Output Location**: `output/book_[topic]_[timestamp]/`

#### 2. YAML Workflow Demos
Configuration-driven workflows without code changes:

```bash
# Run iterative book creation workflow
mvn exec:java -Dexec.mainClass="com.skanga.conductor.workflow.runners.WorkflowRunner" \
  -Dexec.args="src/main/resources/yaml/workflows/iterative-book-creation.yaml"

# Tool demonstration workflow
mvn exec:java -Dexec.mainClass="com.skanga.conductor.workflow.runners.WorkflowRunner" \
  -Dexec.args="src/main/resources/yaml/workflows/tool-demo.yaml"
```

See **[DEMOS.md](DEMOS.md)** for comprehensive information about all available demo applications.

### Development Workflow

#### Quick Development Cycle
```bash
# 1. Make changes to source code
# 2. Quick compile check
mvn compile

# 3. Run specific tests
mvn test -Dtest="*YourFeature*"

# 4. Test with demo
mvn exec:java@book-demo -Dexec.args="Test Topic"

# 5. Full verification before commit
mvn clean install
```

#### IDE Integration
- **IntelliJ IDEA**: Import as Maven project
- **VS Code**: Install Java Extension Pack, configure workspace settings
- **Eclipse**: Import as Existing Maven Project

See **[DEVELOPER_SETUP.md](DEVELOPER_SETUP.md)** for detailed IDE configuration guides.

### Testing

#### Run All Tests
```bash
mvn test                          # All unit tests (~220 tests)
mvn verify                        # Include integration tests
```

#### Focused Testing
```bash
mvn test -Dtest=WorkflowTest              # Specific test class
mvn test -Dtest="*Agent*"                 # Pattern matching
mvn test -Dtest=WorkflowTest#testMethod   # Specific test method
```

#### Test Categories
```bash
mvn test -Dgroups="unit"                  # Unit tests only
mvn test -Dgroups="integration"           # Integration tests only
mvn test -Dgroups="security"              # Security tests only
```

#### Performance Testing

**⚡ Fast Builds**: Performance tests are **disabled by default** to maintain ~1 minute build times.

```bash
# Standard build (fast) - performance tests skipped
mvn test                                   # ~1 minute build time

# Enable basic performance validation - minimal iterations
mvn test -Dtest.performance.enabled=true  # ~30 seconds additional time

# Full performance benchmarking - extensive iterations
mvn test -Dtest.performance.intensive=true # ~3-5 minutes additional time

# Specific performance test classes
mvn test -Dtest=ThreadSafetyTest          # Concurrency tests
mvn test -Dtest=PromptTemplateEnginePerformanceTest -Dtest.performance.enabled=true
```

**📊 Performance Test Categories:**

| Test Type | Default | Basic (`enabled=true`) | Intensive (`intensive=true`) |
|-----------|---------|----------------------|----------------------------|
| **Template Rendering** | ❌ Skipped | 10 iterations | 10,000 iterations |
| **Caching Performance** | ❌ Skipped | 5 iterations | 5,000 iterations |
| **Concurrent Processing** | ❌ Skipped | 2×3 ops | 10×1000 ops |
| **Memory Usage** | ❌ Skipped | 10 templates | 1,000 templates |
| **Cache Eviction** | ❌ Skipped | 10 templates | 1,000 templates |

**🔧 Build Time Optimization:**
- **Problem**: Enabling all performance tests increased build time from 1 min → 7 min
- **Solution**: Conditional execution with `@EnabledIfSystemProperty`
- **Result**: Default builds back to ~1 minute, optional performance validation available

### Troubleshooting

#### Common Issues

| Issue | Solution | Reference |
|-------|----------|-----------|
| `UnsupportedClassVersionError` | Verify Java 21+ with `java --version` | [Setup Guide](DEVELOPER_SETUP.md#java-installation-options) |
| `OutOfMemoryError` | Increase Maven memory: `export MAVEN_OPTS="-Xmx4g"` | [Troubleshooting](DEVELOPER_SETUP.md#troubleshooting-guide) |
| Tests fail with DB locks | Clean test databases: `find target -name "*.db" -delete` | [Testing Guide](TESTING.md) |
| Build hangs during tests | Skip tests: `mvn install -DskipTests` or disable performance tests (default) | [Performance Testing](#performance-testing) |

For comprehensive troubleshooting including IDE issues, environment problems, and debugging techniques, see **[DEVELOPER_SETUP.md](DEVELOPER_SETUP.md#troubleshooting-guide)**.

## Project Structure

The project is organized into the following key packages:

*   `com.skanga.conductor.agent`: Contains the `SubAgent` interface and its implementations.
*   `com.skanga.conductor.orchestration`: Contains the `Orchestrator` and `LLMPlanner`.
*   `com.skanga.conductor.tools`: Contains the `Tool` interface and related classes for tool use.
*   `com.skanga.conductor.memory`: Contains the `MemoryStore` for agent state persistence.
*   `com.skanga.conductor.provider`: Contains the `LLMProvider` interface for integrating with different LLM providers.
*   `com.skanga.conductor.retry`: Contains retry policies and execution logic for resilient operations.
*   `com.skanga.conductor.metrics`: Contains metrics collection and monitoring infrastructure.
*   `com.skanga.conductor.config`: Contains configuration management and validation.
*   `com.skanga.conductor.engine`: Contains the unified workflow execution engine and builder patterns.
*   `com.skanga.conductor.demo`: Contains the demo applications.
*   `com.skanga.conductor.workflow`: Contains the YAML-based workflow system including configuration models, execution engine, and demo applications.
*   `src/test/java`: Contains comprehensive unit tests for all framework components.

## Usage

### Creating an Orchestrator

```java
SubAgentRegistry registry = new SubAgentRegistry();
MemoryStore memoryStore = new H2MemoryStore("jdbc:h2:./data/subagentsdb");
Orchestrator orchestrator = new Orchestrator(registry, memoryStore);
```

### Creating an Explicit Agent

```java
ConversationalAgent explicitAgent = new ConversationalAgent(
    "my-explicit-agent",
    "An agent that can answer questions.",
    new MockLLMProvider(),
    "Answer the following question: {{prompt}}",
    memoryStore
);
registry.register(explicitAgent);

TaskResult result = orchestrator.callExplicit("my-explicit-agent", new TaskInput("What is the capital of France?"));
System.out.println(result.output());
```

### Creating an Implicit Agent

```java
SubAgent implicitAgent = orchestrator.createImplicitAgent(
    "my-implicit-agent",
    "A temporary agent for a specific task.",
    new MockLLMProvider(),
    "Summarize the following text: {{prompt}}"
);

TaskResult result = implicitAgent.execute(new TaskInput("This is a long text..."));
System.out.println(result.output());
```

### Using the Unified Workflow Engine

Both code-based and YAML workflows use the same underlying execution primitives:

```java
// Code-based approach using WorkflowBuilder
List<StageDefinition> stages = WorkflowBuilder.create()
    .addStage("title-generation", "title-generator", "Expert title generator",
              llmProvider, systemPrompt, promptTemplate, maxRetries, validator, metadata)
    .build();

DefaultWorkflowEngine engine = new DefaultWorkflowEngine(orchestrator);
WorkflowResult result = engine.executeWorkflow(stages);

// No-code approach using YAML adapter
YamlWorkflowEngine adapter = new YamlWorkflowEngine(orchestrator, llmProvider);
adapter.loadConfiguration("workflow.yaml", "agents.yaml", "context.yaml");
WorkflowResult yamlResult = adapter.executeWorkflow(context);

// Both produce identical WorkflowResult objects with same behavior
```

## Configuration

See [CONFIGURATION.md](CONFIGURATION.md) for comprehensive configuration documentation.

## Architecture Components

See [TECHNICAL_FEATURES.md](TECHNICAL_FEATURES.md) for detailed information about advanced technical features including thread safety, templating system, and text-to-speech integration.

## Contributing

Contributions are welcome! Please feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
