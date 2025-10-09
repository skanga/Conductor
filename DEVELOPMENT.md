# Conductor Development Guide

This comprehensive guide covers development environment setup, guidelines, workflows, and best practices for the Conductor AI framework.

## Table of Contents

- [Quick Start (5 Minutes)](#quick-start-5-minutes)
- [Development Environment Setup](#development-environment-setup)
- [IDE Configuration](#ide-configuration)
- [Development Guidelines](#development-guidelines)
- [Advanced Development Workflows](#advanced-development-workflows)
- [Exception Handling](#exception-handling)
- [Logging Standards](#logging-standards)
- [Testing Strategies](#testing-strategies)
- [Code Quality Standards](#code-quality-standards)
- [Troubleshooting Guide](#troubleshooting-guide)

---

## Quick Start (5 Minutes)

**⚡ Fastest path to running demos**

### Prerequisites Check
```bash
# Verify Java 21+
java --version
# Should show: openjdk 21.x.x or later

# Verify Maven
mvn --version
# Should show: Apache Maven 3.6+
```

### One-Command Setup
```bash
# Clone, build, and run demo
git clone <repository-url>
cd Conductor
mvn clean install && mvn exec:java@book-demo -Dexec.args="Quick Start Demo"
```

### Expected Output
```
✅ Book creation completed!
Your book 'Quick Start Demo' is ready.
📁 Check: output/book_quickstartdemo_[timestamp]/
```

---

## Development Environment Setup

### System Requirements

#### Mandatory Requirements
| Component | Minimum | Recommended | Notes |
|-----------|---------|-------------|--------|
| **Java** | 21.0.0 | 21.0.8+ | LTS version with latest patches |
| **Maven** | 3.6.0 | 3.9.0+ | For dependency management |
| **Memory** | 2 GB RAM | 8 GB RAM | For LLM operations |
| **Storage** | 500 MB | 2 GB | For models and outputs |

#### Optional but Recommended
| Component | Purpose | Installation |
|-----------|---------|-------------|
| **Git** | Version control | [Download](https://git-scm.com/) |
| **Docker** | Containerized development | [Download](https://docker.com/) |
| **Node.js** | Documentation tools | [Download](https://nodejs.org/) |

### Java Installation Options

#### Option A: Oracle JDK (Recommended for Production)
```bash
# Download from Oracle website
# https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html

# Verify installation
java --version
javac --version
```

#### Option B: OpenJDK (Free Alternative)
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-21-jdk

# macOS with Homebrew
brew install openjdk@21

# Windows with Chocolatey
choco install openjdk21

# Verify
echo $JAVA_HOME
```

#### Option C: SDKMAN (Developer Friendly)
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh

# Install Java 21
sdk install java 21.0.8-tem
sdk use java 21.0.8-tem
```

### Maven Configuration

```bash
# Verify Maven installation
mvn --version

# Configure Maven settings (optional)
mkdir -p ~/.m2
cat > ~/.m2/settings.xml <<EOF
<settings>
  <mirrors>
    <mirror>
      <id>central</id>
      <url>https://repo.maven.apache.org/maven2</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
```

### Project Setup

```bash
# Clone repository
git clone <repository-url>
cd Conductor

# Build project (includes tests)
mvn clean install

# Quick compile (skip tests)
mvn clean compile -DskipTests

# Run demo
mvn exec:java@book-demo -Dexec.args="My First Topic"
```

---

## IDE Configuration

### IntelliJ IDEA Setup

**Recommended for Java development**

#### 1. Import Project
```
File → Open → Select Conductor directory → Open as Maven Project
```

#### 2. Configure Java SDK
```
File → Project Structure → Project Settings → Project
- SDK: Choose Java 21
- Language Level: 21 (Preview)
```

#### 3. Enable Preview Features
```
File → Project Structure → Modules → Select module
- Language level: 21 (Preview features - Sealed types, pattern matching)
```

#### 4. Code Style Settings
```
File → Settings → Editor → Code Style → Java
- Import: conductor-code-style.xml (if provided)
- Or configure:
  - Tab size: 4
  - Indent: 4
  - Continuation indent: 8
```

#### 5. Run Configurations
Create run configuration for book demo:
```
Run → Edit Configurations → + → Maven
- Name: Book Demo
- Command line: exec:java@book-demo -Dexec.args="Test Topic"
- Working directory: $PROJECT_DIR$
```

### VS Code Setup

#### 1. Install Extensions
```
- Extension Pack for Java (Microsoft)
- Maven for Java
- Debugger for Java
- Test Runner for Java
```

#### 2. Configure settings.json
```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/jdk-21"
    }
  ],
  "java.jdt.ls.java.home": "/path/to/jdk-21",
  "maven.executable.path": "/path/to/maven/bin/mvn"
}
```

#### 3. Tasks Configuration (.vscode/tasks.json)
```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Run Book Demo",
      "type": "shell",
      "command": "mvn exec:java@book-demo -Dexec.args='${input:topic}'",
      "group": "build"
    }
  ],
  "inputs": [
    {
      "id": "topic",
      "type": "promptString",
      "description": "Enter book topic"
    }
  ]
}
```

### Eclipse Setup

#### 1. Import Maven Project
```
File → Import → Maven → Existing Maven Projects
- Select Conductor directory
- Import
```

#### 2. Configure JDK
```
Window → Preferences → Java → Installed JREs
- Add JDK 21
- Set as default
```

#### 3. Enable Preview Features
```
Project Properties → Java Compiler
- Enable preview features: Enabled
- Compiler compliance level: 21
```

---

## Development Guidelines

### General Development Principles

1. **Clarity Over Cleverness**: Write code that is easy to understand and maintain
2. **Fail Fast**: Validate inputs early and provide clear error messages
3. **Single Responsibility**: Each class and method should have one clear purpose
4. **Dependency Injection**: Use constructor injection for dependencies
5. **Immutability**: Prefer immutable objects where possible

### Code Organization

#### Package Structure Guidelines
```
com.skanga.conductor
├── agent/              # Core agent implementations
│   ├── base/           # Abstract base classes
│   ├── conversational/ # LLM-based agents
│   └── tool/           # Tool-using agents
├── orchestration/      # Workflow orchestration
│   ├── planner/        # Task planning logic
│   └── executor/       # Execution management
├── config/             # Configuration management
│   ├── validation/     # Configuration validators
│   └── loader/         # Configuration loaders
└── util/               # Utility classes
    ├── concurrent/     # Concurrency utilities
    └── json/           # JSON processing
```

#### Naming Conventions
```java
// Classes: PascalCase with descriptive names
public class ConversationalAgent implements SubAgent {

// Constants: SCREAMING_SNAKE_CASE
private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant";

// Variables: camelCase
private final LLMProvider llmProvider;

// Methods: camelCase with verb-noun pattern
public TaskResult executeWithRetry(TaskInput input) {

// Test methods: descriptive with underscores
@Test
void should_execute_successfully_when_valid_input_provided() {
```

---

## Advanced Development Workflows

### Feature Development Workflow

#### Test-Driven Development (TDD) Approach

**Phase 1: Planning and Design**
```bash
# Create feature branch
git checkout -b feature/enhanced-agent-memory
git push -u origin feature/enhanced-agent-memory

# Document feature design
cat > FEATURE_ENHANCED_MEMORY.md << 'EOF'
# Enhanced Agent Memory Feature

## Problem Statement
Current agents lose context between sessions, limiting continuity.

## Proposed Solution
Implement persistent agent memory with semantic search capabilities.

## Technical Approach
- Extend MemoryStore interface with semantic operations
- Add vector storage backend
- Implement context retrieval algorithms
EOF
```

**Phase 2: API Design First**
```java
// Define interfaces before implementation
public interface SemanticMemoryStore extends MemoryStore {
    String storeWithEmbedding(String agentId, String content, Map<String, Object> metadata);
    List<SimilarityResult> findSimilar(String agentId, String query, int maxResults);
}
```

**Phase 3: Test-First Implementation**
```java
@Test
@DisplayName("Should store and retrieve semantically similar content")
void testSemanticMemoryStorage() {
    // Arrange
    SemanticMemoryStore memoryStore = new VectorMemoryStore(config);
    String agentId = "test-agent";
    String originalContent = "The user likes Italian cuisine and prefers vegetarian options";
    String queryContent = "What food does the user prefer?";

    // Act
    String contentId = memoryStore.storeWithEmbedding(agentId, originalContent, Map.of());
    List<SimilarityResult> results = memoryStore.findSimilar(agentId, queryContent, 5);

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getContent()).isEqualTo(originalContent);
    assertThat(results.get(0).getSimilarityScore()).isGreaterThan(0.7);
}
```

### Bug Fix Workflow

#### Bug Investigation Process

**Step 1: Reproduction Script**
```bash
#!/bin/bash
# reproduce-bug-123.sh
# Script to reproduce "Agent memory corruption under load"

echo "Setting up test environment..."
export CONDUCTOR_DB_URL="jdbc:h2:mem:bug123;DB_CLOSE_DELAY=-1"
export CONDUCTOR_LOG_LEVEL="DEBUG"

echo "Starting concurrent execution test..."
mvn test -Dtest=ConcurrentMemoryTest -Dconductor.threads=10 -Dconductor.operations=100

echo "Checking for memory corruption..."
grep -n "Memory corruption detected" target/test-logs/*.log
```

**Step 2: Isolated Test Case**
```java
@Test
@DisplayName("Bug #123: Memory corruption under concurrent access")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void reproduceMemoryCorruptionBug() {
    // Arrange: Set up conditions that trigger the bug
    MemoryStore sharedStore = new H2MemoryStore(testConfig);
    int threadCount = 10;
    int operationsPerThread = 100;

    List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

    // Act: Execute concurrent operations
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        new Thread(() -> {
            try {
                startLatch.await();
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread-" + threadId + "-op-" + j;
                    sharedStore.store("test-agent", key, "value-" + j);
                    String retrieved = sharedStore.retrieve("test-agent", key);
                    if (!("value-" + j).equals(retrieved)) {
                        throw new RuntimeException("Memory corruption detected");
                    }
                }
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                doneLatch.countDown();
            }
        }).start();
    }

    startLatch.countDown();
    assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();

    // Assert: No corruption occurred
    assertThat(exceptions).isEmpty();
}
```

**Step 3: Root Cause Analysis and Fix**
```java
public class ThreadSafeMemoryStore implements MemoryStore {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Map<String, String>> storage = new ConcurrentHashMap<>();

    @Override
    public void store(String agentId, String key, String value) {
        requireNonNull(agentId, "agentId cannot be null");
        requireNonNull(key, "key cannot be null");
        requireNonNull(value, "value cannot be null");

        lock.writeLock().lock();
        try {
            storage.computeIfAbsent(agentId, k -> new ConcurrentHashMap<>()).put(key, value);
            logger.debug("Stored: agentId={}, key={}, value length={}", agentId, key, value.length());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String retrieve(String agentId, String key) {
        requireNonNull(agentId, "agentId cannot be null");
        requireNonNull(key, "key cannot be null");

        lock.readLock().lock();
        try {
            Map<String, String> agentStorage = storage.get(agentId);
            String value = agentStorage != null ? agentStorage.get(key) : null;
            logger.debug("Retrieved: agentId={}, key={}, found={}", agentId, key, value != null);
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

### Performance Optimization Workflow

#### Benchmarking Framework
```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class MemoryStoreBenchmark {

    private MemoryStore h2Store;
    private MemoryStore vectorStore;
    private List<String> testData;

    @Setup
    public void setup() {
        h2Store = new H2MemoryStore(createConfig());
        vectorStore = new VectorMemoryStore(createConfig());
        testData = generateTestData(1000);
    }

    @Benchmark
    public void benchmarkH2Store(Blackhole bh) {
        for (String data : testData) {
            h2Store.store("bench-agent", UUID.randomUUID().toString(), data);
        }
    }

    @Benchmark
    public void benchmarkVectorStore(Blackhole bh) {
        for (String data : testData) {
            vectorStore.store("bench-agent", UUID.randomUUID().toString(), data);
        }
    }
}
```

#### Memory Profiling
```java
@Test
void profileMemoryUsage() {
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    long beforeUsed = memoryBean.getHeapMemoryUsage().getUsed();

    // Execute operation
    List<WorkflowResult> results = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        results.add(orchestrator.executeWorkflow(createTestWorkflow()));
    }

    // Force garbage collection
    System.gc();
    Thread.sleep(100);

    long afterUsed = memoryBean.getHeapMemoryUsage().getUsed();
    long memoryIncrease = afterUsed - beforeUsed;

    System.out.printf("Memory increase: %d bytes (%.2f MB)%n",
        memoryIncrease, memoryIncrease / 1024.0 / 1024.0);

    // Check for memory leaks
    assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // Less than 100MB
}
```

---

## Exception Handling

### Overview

This section provides guidelines for exception handling in the Conductor framework, following the established exception classification strategy defined in `ExceptionStrategy.java`.

### Exception Classification Strategy

#### Checked Exceptions (ConductorException)

Use checked exceptions for **recoverable business logic failures** that calling code should handle:

- **LLMProviderException** - External service communication failures (can retry with backoff)
- **ToolExecutionException** - Tool execution failures (can fallback to alternative tools)
- **ApprovalException** - Human approval timeouts/rejections (can re-prompt user)
- **PlannerException** - Planning failures (can retry with different parameters)

#### Unchecked Exceptions (ConductorRuntimeException)

Use unchecked exceptions for **programming errors and system failures**:

- **ConfigurationException** - Configuration/startup errors (fix configuration)
- **MemoryStoreException** - Database/infrastructure failures (system issue)
- **JsonProcessingRuntimeException** - Data format errors (fix input data)
- **ValidationException** - Parameter validation errors (fix calling code)

### Exception Handling Patterns

#### 1. Business Logic Recovery

```java
// Good: Handle recoverable business failures
try {
    String result = llmProvider.generate(prompt);
    return result;
} catch (LLMProviderException e) {
    // Retry with exponential backoff
    logger.warn("LLM request failed, retrying: {}", e.getMessage());
    return retryWithBackoff(() -> llmProvider.generate(prompt));
}
```

#### 2. System Error Propagation

```java
// Good: Let system errors propagate unchecked
public String processData(String json) {
    // JsonProcessingRuntimeException will propagate naturally
    return JsonUtils.fromJson(json, DataModel.class);
}
```

#### 3. Exception Wrapping

```java
// Good: Wrap external exceptions in framework types
try {
    return externalService.call();
} catch (IOException e) {
    throw new ConductorException.LLMProviderException("External service failed", e);
}
```

#### 4. Context-Aware Error Messages

```java
// Good: Provide actionable error information
throw new ConfigurationException(
    "Invalid database URL: " + url + ". Expected format: jdbc:h2:mem:testdb"
);
```

### Recovery Strategies

#### 1. Retry with Exponential Backoff

```java
public String generateWithRetry(String prompt) throws LLMProviderException {
    return retryExecutor.execute(() -> {
        try {
            return llmProvider.generate(prompt);
        } catch (LLMProviderException e) {
            if (isRetryable(e)) {
                throw new RetryableException(e);
            }
            throw e; // Non-retryable, fail fast
        }
    });
}
```

#### 2. Fallback Mechanisms

```java
public ToolResult executeTool(String toolName, ToolInput input) throws ToolExecutionException {
    try {
        Tool primaryTool = toolRegistry.getTool(toolName);
        return primaryTool.execute(input);
    } catch (ToolExecutionException e) {
        logger.warn("Primary tool failed, trying fallback: {}", e.getMessage());
        Tool fallbackTool = toolRegistry.getFallbackTool(toolName);
        if (fallbackTool != null) {
            return fallbackTool.execute(input);
        }
        throw e; // No fallback available
    }
}
```

#### 3. Graceful Degradation

```java
public ApprovalResponse getApproval(ApprovalRequest request) throws ApprovalException {
    try {
        return humanApprovalHandler.requestApproval(request);
    } catch (ApprovalTimeoutException e) {
        if (request.hasDefaultAction()) {
            logger.warn("Approval timed out, using default action: {}", request.getDefaultAction());
            return ApprovalResponse.defaultAction(request.getDefaultAction());
        }
        throw e; // No default available
    }
}
```

### Testing Exception Scenarios

#### 1. Test Checked Exception Handling

```java
@Test
void testLLMProviderRetry() throws Exception {
    // Arrange: Mock provider to fail then succeed
    when(mockProvider.generate(any()))
        .thenThrow(new LLMProviderException("Temporary failure"))
        .thenReturn("Success");

    // Act & Assert: Should retry and succeed
    String result = agentWithRetry.generate("test prompt");
    assertEquals("Success", result);
    verify(mockProvider, times(2)).generate(any());
}
```

#### 2. Test Unchecked Exception Propagation

```java
@Test
void testConfigurationValidation() {
    // Arrange: Invalid configuration
    ApplicationConfig config = new ApplicationConfig();
    config.setDatabaseUrl("invalid-url");

    // Act & Assert: Should throw unchecked exception
    assertThrows(ConfigurationException.class, () -> {
        new MemoryStore(config);
    });
}
```

### Anti-Patterns to Avoid

#### 1. Catching and Ignoring Exceptions

```java
// Bad: Swallowing exceptions
try {
    riskyOperation();
} catch (Exception e) {
    // Silent failure - never do this
}

// Good: Log and handle appropriately
try {
    riskyOperation();
} catch (BusinessLogicException e) {
    logger.error("Operation failed: {}", e.getMessage());
    return fallbackResult();
} catch (SystemException e) {
    logger.error("System error in operation: {}", e.getMessage(), e);
    throw e; // Let system errors propagate
}
```

#### 2. Overly Broad Exception Catching

```java
// Bad: Catching too broadly
try {
    complexOperation();
} catch (Exception e) {
    // Can't distinguish between different failure types
    throw new RuntimeException(e);
}

// Good: Catch specific exceptions
try {
    complexOperation();
} catch (LLMProviderException e) {
    // Handle service failures
    return retryOperation();
} catch (ValidationException e) {
    // Handle input errors
    throw new IllegalArgumentException("Invalid input: " + e.getMessage(), e);
}
```

### Provider Exception Standards

#### Using ProviderExceptionFactory

The `ProviderExceptionFactory` provides standardized methods for creating common provider exceptions with rich context:

```java
// Create provider context
ProviderExceptionFactory.ProviderContext context =
    ProviderExceptionFactory.ProviderContext.builder("openai")
        .model("gpt-4")
        .operation("generate_completion")
        .duration(5000L)
        .correlationId("req-123")
        .build();

// Create standardized exceptions
LLMProviderException authException =
    ProviderExceptionFactory.invalidApiKey(context, "Check API key format");

LLMProviderException rateLimitException =
    ProviderExceptionFactory.rateLimitExceeded(context, 60);

LLMProviderException timeoutException =
    ProviderExceptionFactory.requestTimeout(context, 30000);
```

#### Available Factory Methods

**Authentication Exceptions**
- `invalidApiKey(context, keyInfo)` - Invalid API key
- `expiredApiKey(context, expirationInfo)` - Expired API key
- `missingApiKey(context)` - Missing API key
- `insufficientPermissions(context, resource)` - Insufficient permissions

**Rate Limit Exceptions**
- `rateLimitExceeded(context, resetTimeSeconds)` - Rate limit exceeded
- `quotaExceeded(context, quotaType)` - Quota exceeded

**Timeout Exceptions**
- `requestTimeout(context, timeoutMs)` - Request timeout
- `connectionTimeout(context, endpoint)` - Connection timeout

**Service Exceptions**
- `serviceUnavailable(context, httpStatus)` - Service unavailable
- `serviceMaintenance(context, maintenanceWindow)` - Service maintenance

---

## Logging Standards

### Overview

This section establishes consistent logging standards for the Conductor project. We use SLF4J for application logging and System.out for user-facing output to maintain clear separation of concerns.

### Logging Strategy

#### Two-Track Approach

1. **Application Logging (SLF4J)**: For system events, debugging, errors, and operational metrics
2. **User Output (System.out/System.err)**: For direct user interaction and feedback

#### When to Use Each

**Use SLF4J Logging For:**
- Application state changes
- Error conditions and exceptions
- Performance metrics and timing
- Debug information for development
- Integration points with external systems
- Business logic flow tracking
- Configuration changes
- Resource management events

**Use System.out For:**
- Direct user prompts and responses
- Progress indicators during long operations
- Interactive console interfaces
- User-requested output display
- Final results presentation
- Menu systems and user navigation

**Use System.err For:**
- Critical user-facing error messages
- Fatal application failures that users must see
- Immediate attention warnings

### Implementation Standards

#### Logger Declaration

Always declare loggers as private static final:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyClass {
    private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
}
```

#### Log Levels

**ERROR**
- Exceptions that affect application functionality
- Failed operations that cannot be recovered
- Configuration errors
- Critical system failures

```java
logger.error("Failed to initialize provider: {}", providerType, exception);
```

**WARN**
- Recoverable errors
- Deprecated functionality usage
- Performance degradation
- Fallback operations

```java
logger.warn("Provider {} not available, falling back to default", providerName);
```

**INFO**
- Application lifecycle events (startup, shutdown)
- Major business operations completion
- Configuration changes
- Successful external integrations

```java
logger.info("Book creation workflow completed successfully for topic: {}", topic);
```

**DEBUG**
- Detailed execution flow
- Method entry/exit with parameters
- Intermediate calculations
- External API calls and responses

```java
logger.debug("Processing chapter {} with {} words", chapterNumber, wordCount);
```

**TRACE**
- Very detailed debugging information
- Loop iterations
- Temporary variable values
- Fine-grained execution paths

```java
logger.trace("Iteration {} processing item: {}", iteration, item);
```

### Message Formatting

#### Use Parameterized Messages

```java
// Good
logger.info("User {} completed task {} in {} ms", userId, taskId, duration);

// Bad - String concatenation
logger.info("User " + userId + " completed task " + taskId + " in " + duration + " ms");
```

#### Exception Logging

```java
// Include exception as last parameter
logger.error("Failed to process request for user {}", userId, exception);

// For debugging, include more context
logger.debug("Database connection failed after {} retries", retryCount, exception);
```

#### Structured Logging Context

```java
// Include relevant context
logger.info("Workflow stage completed: stage={}, duration={}ms, success={}",
           stageName, duration, success);
```

### Best Practices

#### Do's
- ✅ Use consistent logger naming (class-based)
- ✅ Include relevant context in log messages
- ✅ Use appropriate log levels
- ✅ Log exceptions with context
- ✅ Use parameterized messages
- ✅ Separate application logs from user output
- ✅ Include timing information for performance tracking
- ✅ Log configuration changes and startup parameters

#### Don'ts
- ❌ Don't log sensitive information (passwords, API keys, tokens)
- ❌ Don't use System.out.println() for application logging
- ❌ Don't log in tight loops without level checks
- ❌ Don't ignore exceptions - always log them
- ❌ Don't use string concatenation in log messages
- ❌ Don't log the same event multiple times
- ❌ Don't use generic error messages without context

#### Performance Considerations

**Level Checks for Expensive Operations**
```java
if (logger.isDebugEnabled()) {
    logger.debug("Complex operation result: {}", expensiveToStringMethod());
}
```

**Lazy Evaluation**
```java
// Use suppliers for expensive operations
logger.debug("Result: {}", () -> complexCalculation());
```

### Example Implementation

```java
public class BookCreationDemo {
    private static final Logger logger = LoggerFactory.getLogger(BookCreationDemo.class);

    public void createBook(String topic) {
        logger.info("Starting book creation workflow for topic: {}", topic);

        // User-facing output
        System.out.println("Welcome! Creating your book on: " + topic);
        System.out.println("This may take a few minutes...");

        try {
            // Business logic with logging
            logger.debug("Initializing providers and agents");
            BookManuscript manuscript = processWorkflow(topic);

            logger.info("Book creation completed successfully: {} chapters, {} words",
                       manuscript.getChapterCount(), manuscript.getTotalWords());

            // User-facing success message
            System.out.println("✅ Book creation completed!");
            System.out.println("Your book '" + manuscript.getTitle() + "' is ready.");

        } catch (Exception e) {
            logger.error("Book creation failed for topic: {}", topic, e);

            // User-facing error
            System.err.println("❌ Book creation failed. Please try again.");
        }
    }
}
```

---

## Testing Best Practices

### Overview

The Conductor framework includes comprehensive unit test coverage with 220+ tests covering all major components. This section outlines testing standards and patterns.

### Testing Framework

- **JUnit 5**: Modern testing framework with advanced features
- **Mockito 5.14.0**: Mocking framework for dependency isolation
- **AssertJ-style assertions**: Clear and readable test assertions

### Test Organization

Tests are organized by component in the `src/test/java` directory:

```
src/test/java/com/skanga/conductor/
├── agent/               # Agent implementation tests
├── config/              # Configuration and validation tests
├── demo/                # Integration and demo tests
├── memory/              # Memory store tests
├── metrics/             # Metrics collection tests
├── provider/            # LLM provider tests
├── retry/               # Retry system tests
├── tools/               # Tool execution tests
└── ThreadSafetyTest.java # Concurrency tests
```

### Test Categories

#### Unit Tests
- **Isolated component testing** with mocked dependencies
- **Fast execution** (typically < 100ms per test)
- **Focused validation** of specific functionality

#### Integration Tests
- **Component interaction testing** (e.g., DemoIntegrationTest)
- **Workflow validation** with real dependencies
- **End-to-end scenario testing**

#### Security Tests
- **Input validation** and sanitization
- **Access control** enforcement
- **Attack vector** prevention (injection, traversal)

#### Performance Tests
- **Concurrent execution** safety (ThreadSafetyTest)
- **Resource usage** validation
- **Timeout behavior** verification

### Test Patterns

#### Basic Test Structure (AAA Pattern)

```java
@Test
@DisplayName("Should execute tool call when LLM returns valid JSON")
void testToolExecution() {
    // Arrange
    String validJsonResponse = "{\"tool_calls\":[{\"name\":\"test_tool\",\"arguments\":{\"input\":\"test\"}}]}";
    when(mockLLMProvider.generate(any())).thenReturn(validJsonResponse);
    when(mockTool.run(any())).thenReturn(new ToolResult("success", true));

    // Act
    ExecutionResult result = agent.execute(new ExecutionInput("test input", null));

    // Assert
    assertTrue(result.success());
    assertEquals("success", result.output());
    verify(mockTool).run(any());
}
```

#### Exception Testing

```java
@Test
@DisplayName("Should throw IllegalArgumentException for null name")
void testNullNameValidation() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new ConversationalAgent(null, "description", mockLLM, mockTools, mockMemory)
    );
    assertEquals("name cannot be null or empty", exception.getMessage());
}
```

#### Mock Usage Patterns

```java
@Mock
private LLMProvider mockLLMProvider;

@Mock
private ToolRegistry mockToolRegistry;

@Captor
private ArgumentCaptor<String> stringCaptor;

@Captor
private ArgumentCaptor<ToolInput> toolInputCaptor;

// Verification patterns
verify(mockTool).run(toolInputCaptor.capture());
assertEquals("expected-argument", toolInputCaptor.getValue().text());
```

### Testing Standards

#### Test Naming Conventions
- Test class: `ClassNameTest`
- Test method: `shouldDoSomethingWhenCondition`
- Use descriptive `@DisplayName` annotations

#### Test Independence
- Each test method runs in isolation
- Configuration is reset between tests
- No shared mutable state between tests

#### Test Data Management
- Consistent test data patterns
- Realistic but simplified test scenarios
- Edge case coverage (null, empty, invalid inputs)

#### Mock Configuration
- Stubbing order: Configure mock behavior before test execution
- Argument matching: Use appropriate matchers (`any()`, `eq()`, `argThat()`)
- Verification timing: Verify interactions after method execution

### Migration Checklist

When updating existing code:

1. ✅ Add comprehensive unit tests for new functionality
2. ✅ Follow AAA pattern (Arrange, Act, Assert)
3. ✅ Use descriptive test names and DisplayName annotations
4. ✅ Test both happy path and edge cases
5. ✅ Use appropriate assertions (assertEquals, assertTrue, assertThrows)
6. ✅ Mock dependencies appropriately
7. ✅ Verify interactions when testing behavior
8. ✅ Clean up resources in @AfterEach when needed

---

## Code Quality Standards

### General Principles

1. **Clarity Over Cleverness**: Write code that is easy to understand and maintain
2. **Fail Fast**: Validate inputs early and provide clear error messages
3. **Single Responsibility**: Each class and method should have one clear purpose
4. **Dependency Injection**: Use constructor injection for dependencies
5. **Immutability**: Prefer immutable objects where possible

### Code Style

#### Class Design
```java
public class ConversationalAgent implements SubAgent {
    private static final Logger logger = LoggerFactory.getLogger(ConversationalAgent.class);

    private final String name;
    private final String description;
    private final LLMProvider llmProvider;

    public ConversationalAgent(String name, String description, LLMProvider llmProvider) {
        this.name = requireNonNull(name, "name cannot be null");
        this.description = requireNonNull(description, "description cannot be null");
        this.llmProvider = requireNonNull(llmProvider, "llmProvider cannot be null");
    }
}
```

#### Method Design
```java
public ExecutionResult execute(ExecutionInput input) throws ConductorException {
    requireNonNull(input, "input cannot be null");

    logger.debug("Executing agent: name={}, input={}", name, input.content());

    try {
        String result = processInput(input);
        return ExecutionResult.success(result);
    } catch (Exception e) {
        logger.error("Agent execution failed: name={}", name, e);
        throw new ConductorException("Agent execution failed", e);
    }
}
```

#### Error Handling
- Use specific exception types
- Include context in error messages
- Log errors at appropriate levels
- Follow established exception handling patterns

#### Resource Management
- Use try-with-resources for closeable resources
- Implement AutoCloseable for custom resources
- Clean up resources in finally blocks when try-with-resources isn't applicable

### Documentation Standards

#### JavaDoc Requirements
```java
/**
 * Executes the agent with the given input and returns the result.
 *
 * @param input the execution input containing the request content and metadata
 * @return the execution result containing the response and success status
 * @throws ConductorException if the agent execution fails
 * @throws IllegalArgumentException if input is null
 */
public ExecutionResult execute(ExecutionInput input) throws ConductorException {
    // Implementation
}
```

#### Code Comments
- Explain the "why", not the "what"
- Document complex business logic
- Explain non-obvious implementation decisions
- Keep comments up-to-date with code changes

### Performance Guidelines

#### Memory Management
- Use appropriate collection types
- Consider memory usage for large datasets
- Implement proper cleanup for long-running processes

#### Database Operations
- Use connection pooling
- Implement proper transaction boundaries
- Clean up resources properly

#### Concurrency
- Follow thread safety guidelines
- Use appropriate synchronization mechanisms
- Test concurrent scenarios

---

## Advanced Development Workflows

### Feature Development Workflow

#### Test-Driven Development (TDD) Approach

**Phase 1: Planning and Design**
```bash
# Create feature branch
git checkout -b feature/enhanced-agent-memory
git push -u origin feature/enhanced-agent-memory

# Document feature design
cat > FEATURE_ENHANCED_MEMORY.md << 'EOF'
# Enhanced Agent Memory Feature

## Problem Statement
Current agents lose context between sessions, limiting continuity.

## Proposed Solution
Implement persistent agent memory with semantic search capabilities.

## Technical Approach
- Extend MemoryStore interface with semantic operations
- Add vector storage backend
- Implement context retrieval algorithms

## Success Criteria
- Agents remember previous interactions
- Sub-second context retrieval
- Backward compatibility maintained
EOF
```

**Phase 2: API Design First**
```java
// Define interfaces before implementation
public interface SemanticMemoryStore extends MemoryStore {
    /**
     * Stores content with semantic embedding for similarity search.
     * @param agentId the agent identifier
     * @param content the content to store
     * @param metadata additional context metadata
     * @return unique identifier for the stored content
     */
    String storeWithEmbedding(String agentId, String content, Map<String, Object> metadata);

    /**
     * Retrieves semantically similar content.
     * @param agentId the agent identifier
     * @param query the search query
     * @param maxResults maximum number of results to return
     * @return list of similar content with similarity scores
     */
    List<SimilarityResult> findSimilar(String agentId, String query, int maxResults);
}
```

**Phase 3: Test-First Implementation**
```java
@Test
@DisplayName("Should store and retrieve semantically similar content")
void testSemanticMemoryStorage() {
    // Arrange
    SemanticMemoryStore memoryStore = new VectorMemoryStore(config);
    String agentId = "test-agent";
    String originalContent = "The user likes Italian cuisine and prefers vegetarian options";
    String queryContent = "What food does the user prefer?";

    // Act
    String contentId = memoryStore.storeWithEmbedding(agentId, originalContent, Map.of());
    List<SimilarityResult> results = memoryStore.findSimilar(agentId, queryContent, 5);

    // Assert
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getContent()).isEqualTo(originalContent);
    assertThat(results.get(0).getSimilarityScore()).isGreaterThan(0.7);
}
```

**Phase 4: Implementation**
```java
public class VectorMemoryStore implements SemanticMemoryStore {
    private final EmbeddingModel embeddingModel;
    private final VectorDatabase vectorDb;

    @Override
    public String storeWithEmbedding(String agentId, String content, Map<String, Object> metadata) {
        // Generate embedding
        Embedding embedding = embeddingModel.embed(content).content();

        // Store in vector database
        String id = UUID.randomUUID().toString();
        VectorRecord record = VectorRecord.builder()
            .id(id)
            .agentId(agentId)
            .content(content)
            .embedding(embedding.vector())
            .metadata(metadata)
            .timestamp(Instant.now())
            .build();

        vectorDb.store(record);
        return id;
    }

    @Override
    public List<SimilarityResult> findSimilar(String agentId, String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        return vectorDb.search(VectorQuery.builder()
            .vector(queryEmbedding.vector())
            .filter("agentId", agentId)
            .maxResults(maxResults)
            .minSimilarity(0.5)
            .build())
            .stream()
            .map(this::toSimilarityResult)
            .collect(Collectors.toList());
    }
}
```

#### Incremental Development Approach

**Milestone-Based Development**
```bash
# Create milestone branches
git checkout -b milestone/semantic-memory-v1
git checkout -b milestone/semantic-memory-v2
git checkout -b milestone/semantic-memory-v3

# Each milestone has specific deliverables
# v1: Basic vector storage
# v2: Similarity search
# v3: Production optimization
```

**Feature Flags for Safe Rollout**
```java
@Component
public class FeatureFlags {
    @Value("${conductor.features.semantic-memory.enabled:false}")
    private boolean semanticMemoryEnabled;

    public boolean isSemanticMemoryEnabled() {
        return semanticMemoryEnabled;
    }
}

// Usage in agent factory
public SubAgent createAgent(String type, Map<String, Object> config) {
    MemoryStore memoryStore = featureFlags.isSemanticMemoryEnabled()
        ? new VectorMemoryStore(config)
        : new H2MemoryStore(config);

    return new ConversationalAgent(name, description, llmProvider, tools, memoryStore);
}
```

### Bug Fix Workflow

#### Step 1: Reproduction Script
```bash
#!/bin/bash
# reproduce-bug-123.sh
# Script to reproduce "Agent memory corruption under load"

echo "Setting up test environment..."
export CONDUCTOR_DB_URL="jdbc:h2:mem:bug123;DB_CLOSE_DELAY=-1"
export CONDUCTOR_LOG_LEVEL="DEBUG"

echo "Starting concurrent execution test..."
mvn test -Dtest=ConcurrentMemoryTest -Dconductor.threads=10 -Dconductor.operations=100

echo "Checking for memory corruption..."
grep -n "Memory corruption detected" target/test-logs/*.log
```

#### Step 2: Isolated Test Case
```java
@Test
@DisplayName("Bug #123: Memory corruption under concurrent access")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
void reproduceMemoryCorruptionBug() {
    // Arrange: Set up conditions that trigger the bug
    MemoryStore sharedStore = new H2MemoryStore(testConfig);
    int threadCount = 10;
    int operationsPerThread = 100;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

    // Act: Execute concurrent operations
    for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        new Thread(() -> {
            try {
                startLatch.await();
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "thread-" + threadId + "-op-" + j;
                    sharedStore.store("test-agent", key, "value-" + j);
                    String retrieved = sharedStore.retrieve("test-agent", key);
                    if (!("value-" + j).equals(retrieved)) {
                        throw new RuntimeException("Memory corruption: expected value-" + j + " but got " + retrieved);
                    }
                }
            } catch (Exception e) {
                exceptions.add(e);
            } finally {
                doneLatch.countDown();
            }
        }).start();
    }

    startLatch.countDown();
    assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();

    // Assert: No corruption occurred
    assertThat(exceptions).isEmpty();
}
```

#### Step 3: Fix Implementation with Defensive Programming
```java
public class ThreadSafeMemoryStore implements MemoryStore {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Map<String, String>> storage = new ConcurrentHashMap<>();

    @Override
    public void store(String agentId, String key, String value) {
        requireNonNull(agentId, "agentId cannot be null");
        requireNonNull(key, "key cannot be null");
        requireNonNull(value, "value cannot be null");

        lock.writeLock().lock();
        try {
            storage.computeIfAbsent(agentId, k -> new ConcurrentHashMap<>()).put(key, value);
            logger.debug("Stored: agentId={}, key={}, value length={}", agentId, key, value.length());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String retrieve(String agentId, String key) {
        requireNonNull(agentId, "agentId cannot be null");
        requireNonNull(key, "key cannot be null");

        lock.readLock().lock();
        try {
            Map<String, String> agentStorage = storage.get(agentId);
            String value = agentStorage != null ? agentStorage.get(key) : null;
            logger.debug("Retrieved: agentId={}, key={}, found={}", agentId, key, value != null);
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

---

## Advanced Testing Strategies

### Test Categories and Organization

#### Unit Test Structure
```java
// Test class organization
@DisplayName("ConversationalAgent Unit Tests")
class ConversationalAgentTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorTests {
        @Test
        void shouldRejectNullName() { /* ... */ }

        @Test
        void shouldRejectEmptyDescription() { /* ... */ }
    }

    @Nested
    @DisplayName("Execution behavior")
    class ExecutionTests {
        @Test
        void shouldExecuteSuccessfullyWithValidInput() { /* ... */ }

        @Test
        void shouldHandleLLMFailureGracefully() { /* ... */ }
    }

    @Nested
    @DisplayName("Memory integration")
    class MemoryTests {
        @Test
        void shouldPersistConversationHistory() { /* ... */ }

        @Test
        void shouldRetrievePreviousContext() { /* ... */ }
    }
}
```

#### Integration Test Patterns
```java
@IntegrationTest
@TestMethodOrder(OrderAnnotation.class)
class WorkflowIntegrationTest {

    private static TestContainers testContainers;
    private static Orchestrator orchestrator;

    @BeforeAll
    static void setupTestEnvironment() {
        // Start test containers for database, redis, etc.
        testContainers = new TestContainers()
            .withH2Database()
            .withRedisCache()
            .start();

        // Initialize orchestrator with test configuration
        orchestrator = createTestOrchestrator(testContainers.getConfiguration());
    }

    @Test
    @Order(1)
    void setupBasicWorkflow() {
        // Setup phase
    }

    @Test
    @Order(2)
    void executeWorkflowEndToEnd() {
        // Full workflow execution
    }

    @Test
    @Order(3)
    void validateWorkflowResults() {
        // Result validation
    }

    @AfterAll
    static void tearDownTestEnvironment() {
        testContainers.stop();
    }
}
```

### Advanced Testing Techniques

#### Property-Based Testing
```java
@Property
void agentExecutionIsIdempotent(@ForAll("validExecutionInputs") ExecutionInput input) {
    // Given
    ConversationalAgent agent = createTestAgent();

    // When
    ExecutionResult result1 = agent.execute(input);
    ExecutionResult result2 = agent.execute(input);

    // Then
    assertThat(result1.output()).isEqualTo(result2.output());
    assertThat(result1.success()).isEqualTo(result2.success());
}

@Provide
Arbitrary<ExecutionInput> validExecutionInputs() {
    return Arbitraries.create(() -> new ExecutionInput(
        Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(1000).sample(),
        Arbitraries.maps(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
            Arbitraries.oneOf(
                Arbitraries.strings(),
                Arbitraries.integers(),
                Arbitraries.doubles()
            )
        ).ofMaxSize(10).sample()
    ));
}
```

#### Contract Testing
```java
@Contract
interface AgentContract {
    @Test
    default void shouldRejectNullInput(SubAgent agent) {
        assertThrows(IllegalArgumentException.class, () -> agent.execute(null));
    }

    @Test
    default void shouldReturnNonNullResult(SubAgent agent) {
        ExecutionResult result = agent.execute(new ExecutionInput("test", Map.of()));
        assertThat(result).isNotNull();
    }

    @Test
    default void shouldPreserveAgentName(SubAgent agent) {
        String originalName = agent.getName();
        agent.execute(new ExecutionInput("test", Map.of()));
        assertThat(agent.getName()).isEqualTo(originalName);
    }
}

// Implementations
class ConversationalAgentContractTest implements AgentContract {
    private ConversationalAgent createAgent() {
        return new ConversationalAgent("test", "desc", mockLLM, mockTools, mockMemory);
    }
}
```

#### Chaos Engineering Tests
```java
@ChaosTest
class SystemResilienceTest {

    @Test
    @DisplayName("System should handle random component failures")
    void chaosMonkeyTest() {
        // Setup system components
        List<SystemComponent> components = Arrays.asList(
            new DatabaseComponent(),
            new LLMProviderComponent(),
            new MemoryStoreComponent(),
            new ToolRegistryComponent()
        );

        // Randomly fail components during execution
        Random random = new Random();
        ScheduledExecutorService chaosExecutor = Executors.newScheduledThreadPool(1);

        chaosExecutor.scheduleWithFixedDelay(() -> {
            SystemComponent component = components.get(random.nextInt(components.size()));
            component.simulateFailure();

            // Recover after random delay
            Executors.newSingleThreadExecutor().schedule(
                component::recover,
                random.nextInt(5) + 1,
                TimeUnit.SECONDS
            );
        }, 1, 2, TimeUnit.SECONDS);

        // Execute workflow under chaos
        try {
            WorkflowResult result = orchestrator.executeWorkflow(createResilienceTestWorkflow());
            assertThat(result.isSuccess()).isTrue(); // Should succeed despite chaos
        } finally {
            chaosExecutor.shutdown();
        }
    }
}
```

---

## Performance Optimization

### Performance Measurement

#### Benchmarking Framework
```java
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class MemoryStoreBenchmark {

    private MemoryStore h2Store;
    private MemoryStore vectorStore;
    private List<String> testData;

    @Setup
    public void setup() {
        h2Store = new H2MemoryStore(createConfig());
        vectorStore = new VectorMemoryStore(createConfig());
        testData = generateTestData(1000);
    }

    @Benchmark
    public void benchmarkH2Store(Blackhole bh) {
        for (String data : testData) {
            h2Store.store("bench-agent", UUID.randomUUID().toString(), data);
        }
    }

    @Benchmark
    public void benchmarkVectorStore(Blackhole bh) {
        for (String data : testData) {
            vectorStore.store("bench-agent", UUID.randomUUID().toString(), data);
        }
    }

    @Benchmark
    public String benchmarkRetrieval() {
        return h2Store.retrieve("bench-agent", "test-key");
    }
}

// Run with: mvn exec:java -Dexec.mainClass=org.openjdk.jmh.Main -Dexec.args="MemoryStoreBenchmark"
```

#### Memory Profiling
```java
@Test
void profileMemoryUsage() {
    // Enable memory profiling
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    long beforeUsed = memoryBean.getHeapMemoryUsage().getUsed();

    // Execute operation
    List<WorkflowResult> results = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        results.add(orchestrator.executeWorkflow(createTestWorkflow()));
    }

    // Force garbage collection
    System.gc();
    Thread.sleep(100);

    long afterUsed = memoryBean.getHeapMemoryUsage().getUsed();
    long memoryIncrease = afterUsed - beforeUsed;

    System.out.printf("Memory increase: %d bytes (%.2f MB)%n",
        memoryIncrease, memoryIncrease / 1024.0 / 1024.0);

    // Check for memory leaks
    assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // Less than 100MB
}
```

### Performance Optimization Strategies

#### Connection Pooling Optimization
```java
@Configuration
public class OptimizedDataSourceConfig {

    @Bean
    @Primary
    public DataSource optimizedDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);
        config.setUsername(username);
        config.setPassword(password);

        // Optimized settings
        config.setMaximumPoolSize(50);
        config.setMinimumIdle(10);
        config.setIdleTimeout(300000); // 5 minutes
        config.setConnectionTimeout(20000); // 20 seconds
        config.setLeakDetectionThreshold(60000); // 1 minute

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        return new HikariDataSource(config);
    }
}
```

#### Caching Strategy
```java
@Service
public class OptimizedLLMProvider implements LLMProvider {

    @Cacheable(value = "llm-responses", key = "#prompt.hashCode()")
    public String generate(String prompt) {
        // Expensive LLM call
        return delegate.generate(prompt);
    }

    @CacheEvict(value = "llm-responses", allEntries = true)
    public void clearCache() {
        // Cache invalidation
    }
}

// Cache configuration
@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("llm-responses",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    Integer.class, String.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(1000)
                        .offheap(10, MemoryUnit.MB)
                        .disk(100, MemoryUnit.MB)
                ).withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(30)))
            ).build(true);
    }
}
```

#### Async Processing Optimization
```java
@Service
public class AsyncWorkflowExecutor {

    private final TaskExecutor executor = new ThreadPoolTaskExecutor();

    @PostConstruct
    public void initialize() {
        ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) executor;
        threadPool.setCorePoolSize(10);
        threadPool.setMaxPoolSize(50);
        threadPool.setQueueCapacity(200);
        threadPool.setThreadNamePrefix("workflow-");
        threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPool.initialize();
    }

    @Async
    public CompletableFuture<WorkflowResult> executeAsync(WorkflowDefinition workflow) {
        try {
            WorkflowResult result = orchestrator.executeWorkflow(workflow);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<WorkflowResult> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public List<WorkflowResult> executeBatch(List<WorkflowDefinition> workflows) {
        List<CompletableFuture<WorkflowResult>> futures = workflows.stream()
            .map(this::executeAsync)
            .collect(Collectors.toList());

        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
}
```

---

## Integration Development

### Third-Party Service Integration

#### Provider Interface Design
```java
// Generic provider interface
public interface ExternalProvider<T, R> {
    R execute(T request) throws ProviderException;
    boolean isHealthy();
    ProviderMetrics getMetrics();
}

// Specific implementation
public class OpenAIProvider implements ExternalProvider<LLMRequest, LLMResponse> {

    private final OpenAI client;
    private final CircuitBreaker circuitBreaker;
    private final RetryTemplate retryTemplate;

    @Override
    public LLMResponse execute(LLMRequest request) throws ProviderException {
        return circuitBreaker.execute(() ->
            retryTemplate.execute(context -> {
                try {
                    return client.completions().create(toOpenAIRequest(request));
                } catch (OpenAIException e) {
                    throw new ProviderException("OpenAI request failed", e);
                }
            })
        );
    }

    @Override
    public boolean isHealthy() {
        try {
            // Simple health check
            client.models().list();
            return true;
        } catch (Exception e) {
            logger.warn("Health check failed", e);
            return false;
        }
    }
}
```

#### Integration Testing with TestContainers
```java
@IntegrationTest
@TestMethodOrder(OrderAnnotation.class)
class OpenAIProviderIntegrationTest {

    @Container
    static final WireMockContainer wireMock = new WireMockContainer("wiremock/wiremock:latest")
        .withMappingFromResource("openai-mock-responses.json");

    private OpenAIProvider provider;

    @BeforeEach
    void setup() {
        String baseUrl = "http://localhost:" + wireMock.getPort();
        provider = new OpenAIProvider(OpenAI.builder()
            .baseUrl(baseUrl)
            .apiKey("test-key")
            .build());
    }

    @Test
    @Order(1)
    void shouldConnectToMockService() {
        assertThat(provider.isHealthy()).isTrue();
    }

    @Test
    @Order(2)
    void shouldHandleSuccessfulRequest() {
        LLMRequest request = new LLMRequest("Hello, world!");
        LLMResponse response = provider.execute(request);

        assertThat(response.getContent()).isNotBlank();
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @Order(3)
    void shouldHandleRateLimiting() {
        // Configure mock to return rate limit error
        wireMock.stubFor(post(urlPathEqualTo("/v1/completions"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Retry-After", "60")
                .withBody("{\"error\": \"Rate limit exceeded\"}")));

        assertThrows(RateLimitException.class, () ->
            provider.execute(new LLMRequest("test")));
    }
}
```

### Event-Driven Integration

#### Event System Design
```java
// Event system
@Component
public class WorkflowEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishWorkflowStarted(WorkflowStartedEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void publishStageCompleted(StageCompletedEvent event) {
        eventPublisher.publishEvent(event);
    }
}

// Event listeners
@EventListener
@Async
public void handleWorkflowStarted(WorkflowStartedEvent event) {
    // Send notification to monitoring system
    monitoringService.recordWorkflowStart(event.getWorkflowId());

    // Update workflow status in database
    workflowRepository.updateStatus(event.getWorkflowId(), WorkflowStatus.RUNNING);
}

@EventListener
@Async
public void handleStageCompleted(StageCompletedEvent event) {
    // Update progress metrics
    metricsService.recordStageCompletion(
        event.getStageId(),
        event.getDuration(),
        event.isSuccessful()
    );

    // Trigger downstream workflows if configured
    if (event.shouldTriggerDownstream()) {
        workflowTriggerService.triggerDownstream(event.getWorkflowId());
    }
}
```

---

## Documentation Workflow

### Living Documentation Strategy

#### Architecture Decision Records (ADRs)
```markdown
# ADR-001: Memory Store Architecture

## Status
Accepted

## Context
We need to decide on the memory store architecture to support both simple key-value storage and advanced semantic search capabilities.

## Decision
We will use a pluggable memory store architecture with the following components:
- `MemoryStore` interface for basic operations
- `SemanticMemoryStore` interface extending basic operations
- Multiple implementations (H2, Vector, Redis)

## Consequences
**Positive:**
- Flexibility to choose appropriate storage backend
- Easy to test with in-memory implementations
- Can optimize for different use cases

**Negative:**
- Additional complexity in configuration
- Need to maintain multiple implementations

## Implementation
```java
public interface MemoryStore {
    void store(String agentId, String key, String value);
    String retrieve(String agentId, String key);
}

public interface SemanticMemoryStore extends MemoryStore {
    String storeWithEmbedding(String agentId, String content, Map<String, Object> metadata);
    List<SimilarityResult> findSimilar(String agentId, String query, int maxResults);
}
```

## Alternatives Considered
1. Single monolithic store - rejected due to inflexibility
2. Separate services - rejected due to complexity

## References
- [Memory Store Implementation](../src/main/java/com/skanga/conductor/memory/)
- [Related Discussion](https://github.com/project/discussions/123)
```

#### API Documentation Generation
```java
/**
 * Comprehensive workflow execution engine with support for both code-based and YAML-based workflows.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Workflow Execution</h3>
 * <pre><code>
 * UnifiedWorkflowEngine engine = new UnifiedWorkflowEngine(orchestrator);
 * List&lt;StageDefinition&gt; stages = WorkflowBuilder.create()
 *     .addStage("title", "title-agent", "Generate title", llmProvider, prompt)
 *     .build();
 * WorkflowResult result = engine.executeWorkflow(stages);
 * </code></pre>
 *
 * <h3>YAML-Based Workflow</h3>
 * <pre><code>
 * YamlWorkflowEngine adapter = new YamlWorkflowEngine(orchestrator, llmProvider);
 * adapter.loadConfiguration("workflow.yaml", "agents.yaml", "context.yaml");
 * WorkflowResult result = adapter.executeWorkflow(context);
 * </code></pre>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Stage execution: O(n) where n is number of stages</li>
 *   <li>Memory usage: O(k) where k is context size</li>
 *   <li>Concurrent stages: Up to 10 parallel executions by default</li>
 * </ul>
 *
 * @see WorkflowBuilder for programmatic workflow creation
 * @see YamlWorkflowEngine for YAML-based workflows
 * @since 1.0.0
 * @author Conductor Team
 */
public class UnifiedWorkflowEngine {
    // Implementation
}
```

### Documentation Automation

#### Generate Docs from Code
```bash
#!/bin/bash
# docs/generate-api-docs.sh

echo "Generating API documentation..."
mvn javadoc:javadoc -Dshow=private -Dnohelp=true

echo "Generating dependency graph..."
mvn dependency:tree -DoutputFile=docs/dependencies.txt -DappendOutput=false

echo "Generating test coverage report..."
mvn jacoco:report
cp target/site/jacoco/index.html docs/coverage.html

echo "Generating changelog from git history..."
git log --oneline --since="1 month ago" > docs/recent-changes.txt

echo "Creating comprehensive documentation site..."
mvn site
```

#### Documentation Testing
```java
@Test
void verifyCodeExamplesInDocumentation() {
    // Parse documentation files for code examples
    List<Path> docFiles = Files.walk(Paths.get("docs"))
        .filter(p -> p.toString().endsWith(".md"))
        .collect(Collectors.toList());

    for (Path docFile : docFiles) {
        List<String> codeBlocks = extractJavaCodeBlocks(docFile);
        for (String code : codeBlocks) {
            // Compile and verify code examples
            assertThat(compileJavaCode(code)).isTrue();
        }
    }
}
```

---

## Testing Infrastructure

### Overview

The Conductor framework includes extensive unit test coverage with over 220 tests covering all major components. The testing infrastructure is built using JUnit 5 and Mockito, providing robust validation of functionality, error handling, and edge cases.

### Test Architecture

#### Testing Framework
- **JUnit 5**: Modern testing framework with advanced features
- **Mockito 5.14.0**: Mocking framework for dependency isolation
- **AssertJ-style assertions**: Clear and readable test assertions

#### Test Organization
Tests are organized by component in the `src/test/java` directory:

```
src/test/java/com/skanga/conductor/
├── agent/               # Agent implementation tests
├── config/              # Configuration and validation tests
├── demo/                # Integration and demo tests
├── memory/              # Memory store tests
├── metrics/             # Metrics collection tests
├── provider/            # LLM provider tests
├── retry/               # Retry system tests
├── tools/               # Tool execution tests
└── ThreadSafetyTest.java # Concurrency tests
```

### Test Coverage

#### Core Components Tested

**1. Agent System (`agent/`)**
- **ConversationalAgentTest** (22 tests)
  - Constructor validation for all parameters
  - Tool execution via JSON parsing
  - Error handling for unknown tools and tool execution failures
  - Plain text response handling when no valid tool calls detected
  - Invalid JSON handling gracefully
  - Memory store integration and truncation
  - Multiple tool keyword detection (research:, readfile:, run:, speak:)
  - Case-insensitive keyword matching
  - Multiple tools in single prompt
  - Tool not found scenarios
  - Parameter extraction and parsing

**2. Retry System (`retry/`)**
- **RetryExecutorTest** (14 tests)
  - Different retry policies (NoRetry, FixedDelay, ExponentialBackoff)
  - Exception handling and unwrapping
  - Interruption handling during retry delays
  - Callable and Supplier execution patterns

- **RetrySystemTest** (15 tests)
  - Integration testing of retry policies
  - LLM provider retry behavior
  - Timeout and duration limits

**3. Metrics System (`metrics/`)**
- **TimerContextTest** (12 tests)
  - Timer metric recording with various configurations
  - Null parameter handling
  - Elapsed time tracking accuracy
  - Success/failure recording with additional tags
  - Try-with-resources pattern support

- **InMemoryMetricsCollectorTest** (17 tests)
  - Metric recording when enabled/disabled
  - Metric summary creation and updates
  - Time-based metric retrieval
  - Top metrics by value
  - Metric trimming and retention cleanup

- **MetricsSystemTest** (15 tests)
  - End-to-end metrics collection workflows
  - Performance monitoring validation

**4. Tool System (`tools/`)**
- **CodeRunnerToolTest** (14 tests)
  - Secure command execution
  - Command injection prevention
  - Timeout handling
  - Whitelist validation

- **FileReadToolSecurityTest** (15 tests)
  - Path traversal prevention
  - Access control validation
  - Security boundary testing

**5. Configuration (`config/`)**
- **ApplicationConfigTest** (1 test)
  - Configuration loading and validation

- **ConfigurationValidationTest** (9 tests)
  - Configuration parameter validation
  - Error handling for invalid configurations

**6. Memory System (`memory/`)**
- **MemoryStoreTest** (6 tests)
  - Memory storage and retrieval
  - Transaction handling

- **MemoryStoreCLOBTest** (7 tests)
  - Large object storage
  - CLOB handling and serialization

**7. Provider System (`provider/`)**
- **LLMProviderTest** (26 tests)
  - Multiple LLM provider implementations
  - Error handling and retry integration
  - Provider-specific behavior validation

### Running Tests

#### Run All Tests
```bash
mvn test
```

#### Run Specific Test Class
```bash
mvn test -Dtest=RetryExecutorTest
```

#### Run Tests with Coverage
```bash
mvn test jacoco:report
```

#### Run Tests in Parallel
Tests are configured to run safely in parallel where possible:
```bash
mvn test -T 1C
```

### Test Categories

#### Unit Tests
- **Isolated component testing** with mocked dependencies
- **Fast execution** (typically < 100ms per test)
- **Focused validation** of specific functionality

#### Integration Tests
- **Component interaction testing** (e.g., DemoIntegrationTest)
- **Workflow validation** with real dependencies
- **End-to-end scenario testing**

#### Security Tests
- **Input validation** and sanitization
- **Access control** enforcement
- **Attack vector** prevention (injection, traversal)

#### Performance Tests
- **Concurrent execution** safety (ThreadSafetyTest)
- **Resource usage** validation
- **Timeout behavior** verification

### Test Utilities

#### Mock Usage Patterns

**Basic Mocking**
```java
@Mock
private LLMProvider mockLLMProvider;

@Mock
private ToolRegistry mockToolRegistry;
```

**Argument Capturing**
```java
@Captor
private ArgumentCaptor<String> stringCaptor;

@Captor
private ArgumentCaptor<ToolInput> toolInputCaptor;
```

**Verification Patterns**
```java
verify(mockTool).run(toolInputCaptor.capture());
assertEquals("expected-argument", toolInputCaptor.getValue().text());
```

#### Common Test Patterns

**Exception Testing**
```java
IllegalArgumentException exception = assertThrows(
    IllegalArgumentException.class,
    () -> new ConversationalAgent(null, "description", mockLLM, mockTools, mockMemory)
);
assertEquals("name cannot be null or empty", exception.getMessage());
```

**Timing and Delays**
```java
// For tests that need timing validation
Thread.sleep(10);
assertTrue(timer.getElapsed().toMillis() >= 10);
```

**Configuration Reset**
```java
@BeforeEach
void setUp() {
    ApplicationConfig.resetInstance(); // Clean state for each test
}
```

### Test Data Management

#### Test Isolation
- Each test method runs in isolation
- Configuration is reset between tests
- No shared mutable state between tests

#### Mock Data
- Consistent test data patterns
- Realistic but simplified test scenarios
- Edge case coverage (null, empty, invalid inputs)

#### File System Tests
- Temporary directories for file operations
- Automatic cleanup after tests
- No dependency on external file system state

### Continuous Integration

#### Build Pipeline Tests
Tests are automatically run on:
- **Local builds** (`mvn clean install`)
- **Pull request validation**
- **Merge to main branch**

#### Test Reports
- **JUnit reports**: Generated in `target/surefire-reports/`
- **Coverage reports**: Available with jacoco plugin
- **Test summaries**: Displayed in CI output

### Best Practices

#### Writing New Tests

1. **Follow naming conventions**:
   - Test class: `ClassNameTest`
   - Test method: `shouldDoSomethingWhenCondition`

2. **Use descriptive DisplayName annotations**:
   ```java
   @DisplayName("Should execute tool call when LLM returns valid JSON")
   ```

3. **Structure tests with AAA pattern**:
   ```java
   // Arrange
   setup_test_data();

   // Act
   Result result = methodUnderTest();

   // Assert
   assertEquals(expected, result);
   ```

4. **Test both happy path and edge cases**:
   - Valid inputs and expected outputs
   - Invalid inputs and error handling
   - Boundary conditions and null values

5. **Use appropriate assertions**:
   - `assertEquals` for exact matches
   - `assertTrue`/`assertFalse` for boolean conditions
   - `assertThrows` for exception scenarios
   - `assertNotNull`/`assertNull` for null checks

#### Maintaining Tests

1. **Keep tests focused and independent**
2. **Update tests when changing functionality**
3. **Remove or update obsolete tests**
4. **Ensure tests are deterministic and stable**

### Troubleshooting

#### Common Issues

**Flaky Tests**
- **Timing issues**: Use deterministic waits instead of `Thread.sleep()`
- **Resource cleanup**: Ensure proper cleanup in `@AfterEach`
- **Test isolation**: Check for shared state between tests

**Mock Configuration**
- **Stubbing order**: Configure mock behavior before test execution
- **Argument matching**: Use appropriate matchers (`any()`, `eq()`, `argThat()`)
- **Verification timing**: Verify interactions after method execution

**Performance Tests**
- **CI environment**: Account for slower CI execution
- **Resource constraints**: Design tests for various environments
- **Timeout configuration**: Set appropriate timeout values

#### Debugging Tests

**Enable Verbose Logging**
```bash
mvn test -Dlogback.level=DEBUG
```

**Run Single Test with Debug**
```bash
mvn test -Dtest=RetryExecutorTest#shouldRetryOnTransientFailure -Dmaven.surefire.debug
```

**Analyze Test Reports**
Check `target/surefire-reports/` for detailed test output and stack traces.

### Test Metrics

#### Current Coverage
- **220+ total tests** across all components
- **0 failures, 0 errors** in CI pipeline
- **High coverage** of core business logic
- **Comprehensive edge case testing**

#### Quality Metrics
- **Fast execution**: Average test runtime < 50ms
- **Reliable**: Zero flaky tests in CI
- **Maintainable**: Clear test structure and documentation

---

## Troubleshooting Guide

### Common Build Issues

#### Java Version Mismatch
**Problem**: `error: invalid target release: 21`
**Solution**:
```bash
# Verify Java version
java --version

# Set JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# Retry build
mvn clean install
```

#### Maven Dependency Resolution Failures
**Problem**: `Could not resolve dependencies`
**Solution**:
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Force update
mvn clean install -U

# Use specific mirror
mvn clean install -Dmaven.repo.remote=https://repo.maven.apache.org/maven2
```

#### Out of Memory During Build
**Problem**: `java.lang.OutOfMemoryError: Java heap space`
**Solution**:
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"

# Or in .mavenrc
echo "MAVEN_OPTS=-Xmx2g" > ~/.mavenrc
```

### Runtime Issues

#### LLM Provider Connection Failures
**Problem**: `Connection refused` or `API key invalid`
**Solution**:
```bash
# Check API key configuration
cat src/main/resources/application.properties | grep api.key

# Verify network connectivity
curl -I https://api.openai.com

# Check proxy settings if behind firewall
export HTTP_PROXY=http://proxy:8080
export HTTPS_PROXY=http://proxy:8080
```

#### Database Lock Errors
**Problem**: `Database may be already in use`
**Solution**:
```bash
# Close all running demos
pkill -f "conductor"

# Delete lock files
rm -f data/*.lock
rm -f data/*.trace.db

# Restart application
mvn exec:java@book-demo
```

#### File Permission Errors
**Problem**: `Access denied` or `Permission denied`
**Solution**:
```bash
# Fix output directory permissions
chmod -R 755 output/

# Fix data directory permissions
chmod -R 755 data/

# Check file ownership
ls -la data/
```

### Testing Issues

#### Tests Failing Intermittently
**Problem**: Random test failures, especially with concurrency
**Solution**:
```bash
# Run with increased timeout
mvn test -Dtest.timeout.multiplier=2

# Run single test to isolate
mvn test -Dtest=SpecificTest

# Disable parallel execution
mvn test -DforkCount=1
```

#### Mock Provider Issues
**Problem**: Tests fail with `No LLM provider configured`
**Solution**:
```java
// Ensure test uses MockLLMProvider
LLMProvider mockProvider = new MockLLMProvider();
orchestrator = new Orchestrator(mockProvider);
```

### IDE-Specific Issues

#### IntelliJ: "Cannot resolve symbol"
**Solution**:
```
1. File → Invalidate Caches → Invalidate and Restart
2. Right-click pom.xml → Maven → Reload Project
3. File → Project Structure → Verify SDK is Java 21
```

#### VS Code: Java Extension Not Working
**Solution**:
```bash
# Clean Java workspace
rm -rf ~/.config/Code/User/workspaceStorage/*

# Reinstall extensions
code --install-extension vscjava.vscode-java-pack

# Restart VS Code
```

#### Eclipse: Build Path Errors
**Solution**:
```
1. Project → Clean
2. Right-click project → Maven → Update Project
3. Project Properties → Java Build Path → Verify JRE
```

### Performance Issues

#### Slow Test Execution
**Solution**:
```bash
# Skip performance tests
mvn test

# Run only fast tests
mvn test -Dtest="*Test,!*PerformanceTest"

# Use parallel execution
mvn test -DforkCount=4
```

#### High Memory Usage
**Solution**:
```properties
# Configure memory limits in application.properties
conductor.memory.max.entries=1000
conductor.database.max.connections=10
conductor.tools.fileread.max.size.bytes=5242880
```

### Getting Help

If you encounter issues not covered here:

1. **Check Logs**: Review `logs/conductor.log` for detailed error messages
2. **Enable Debug**: Run with `-Dconductor.logging.level=DEBUG`
3. **Search Issues**: Check GitHub issues for similar problems
4. **Ask Community**: Open a new GitHub issue with:
   - Java version (`java --version`)
   - Maven version (`mvn --version`)
   - Operating system
   - Full error message and stack trace
   - Steps to reproduce

---

This comprehensive development guide provides the foundation for successful development with the Conductor framework. Follow these guidelines and workflows to ensure consistent, maintainable, and high-quality code contributions to the project.