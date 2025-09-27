# AGENTS.md

This file provides guidance to ALL coding agents when working with code in this repository.

## Build Commands

Build the project:
```bash
mvn clean compile
```

Run tests:
```bash
mvn test
```

Run the demo application:
```bash
mvn exec:java
```

Run with a specific main class:
```bash
mvn exec:java -Dexec.mainClass="demo.conductor.com.skanga.conductor.DemoMock"
```

Package the application:
```bash
mvn package
```

## Architecture Overview

**Conductor** is a Java framework for orchestrating LLM-powered agent workflows. It provides two main orchestration patterns:

### Core Components

- **SubAgent Interface**: Base abstraction for all agents (`execute(TaskInput) -> TaskResult`)
- **ConversationalAgent**: AI-powered agents with persistent memory and tool capabilities using LangChain4j
- **Orchestrator**: Basic agent registry and execution coordinator
- **PlannerOrchestrator**: Advanced orchestrator with LLM-based task planning and resumable workflows

### Memory System

- **MemoryStore**: H2 database-backed persistence for agent memory and workflow state
- **Persistent Workflows**: Plans and task outputs are saved, enabling workflow resumption after interruption
- **Agent Memory**: Each agent maintains conversation history that persists across executions

### Tool System

Located in `src/main/java/com/skanga/conductor/tools/`:
- **Tool Interface**: Base abstraction for executable tools
- **ToolRegistry**: Central registry for tool discovery and execution
- Built-in tools: FileReadTool, WebSearchTool, CodeRunnerTool, TextToSpeechTool

### LLM Integration

- **LLMProvider**: Abstraction for different LLM services (OpenAI, Mock)
- **LLMPlanner**: Uses LLM to decompose user requests into structured task plans
- **ConversationalAgent**: Combines LLM reasoning with tool execution capabilities

### Key Workflow Pattern

1. **Planning**: LLMPlanner generates TaskDefinition[] from user request
2. **Execution**: Each task creates an implicit ConversationalAgent with placeholder substitution
3. **Chaining**: Tasks can reference previous outputs via `{{prev_output}}` or `{{task_name}}`
4. **Persistence**: All outputs saved for resumability

### Database Schema

The H2 database (`./data/subagentsdb`) stores:
- Agent memory entries (by agent name)
- Workflow plans (by workflow ID)
- Task outputs (by workflow ID + task name)

### Demo Structure

- **DemoMock**: Main demo showing agent memory persistence across application restarts
- **BookWorkflow**: Example workflow for generating book chapters
- Uses MockLLMProvider for testing without real LLM API calls

The system supports both explicit agent registration and dynamic implicit agent creation for flexible workflow patterns.

## Configuration Management

The application uses a comprehensive configuration system that externalizes all hardcoded values. See [CONFIGURATION.md](CONFIGURATION.md) for detailed documentation.

### Key Configuration Features
- **Hierarchical configuration**: Properties files → System properties → Environment variables
- **Environment-specific configs**: `application.properties`, `application-dev.properties`, `application-prod.properties`
- **Type-safe configuration**: Duration, boolean, numeric, and set parsing with defaults
- **Runtime configuration**: Environment variables and system properties override file values

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

All classes now use configuration-driven defaults instead of hardcoded values. Legacy constructors are marked deprecated but maintained for backward compatibility.

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

All existing APIs remain unchanged - thread safety is transparent to users.

## Testing Infrastructure

The framework includes comprehensive unit test coverage with 220+ tests covering all major components.

### Test Categories
- **Unit Tests**: Isolated component testing with mocked dependencies (JUnit 5 + Mockito)
- **Integration Tests**: Component interaction and workflow validation
- **Security Tests**: Input validation, access control, and attack prevention
- **Performance Tests**: Concurrent execution safety and resource usage

### Key Test Coverage
- **Agent System**: ConversationalAgent behavior and error handling
- **Retry System**: All retry policies, exception handling, and timeout scenarios
- **Metrics System**: TimerContext, InMemoryMetricsCollector, and aggregation
- **Tool System**: Secure execution, injection prevention, and whitelisting
- **Memory System**: Storage, retrieval, and transaction handling
- **Configuration**: Validation, security, and property management

### Testing Best Practices
1. **Always run tests before committing**: `mvn test`
2. **Write tests for new features**: Follow AAA pattern (Arrange, Act, Assert)
3. **Use mocks for dependencies**: Isolate units under test
4. **Test both happy path and edge cases**: Validate error handling
5. **Maintain test independence**: No shared state between tests

See [DEVELOPMENT.md](DEVELOPMENT.md) for comprehensive testing documentation and guidelines.