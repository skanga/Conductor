# Conductor Framework - Developer Setup Guide

This comprehensive guide provides detailed steps, multiple approaches, and practical ideas for setting up a development environment for the Conductor AI framework.

## Table of Contents

- [Quick Start (5 Minutes)](#quick-start-5-minutes)
- [Comprehensive Setup](#comprehensive-setup)
- [Development Environment Options](#development-environment-options)
- [IDE Configuration](#ide-configuration)
- [Testing Setup](#testing-setup)
- [Advanced Development Workflows](#advanced-development-workflows)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Development Best Practices](#development-best-practices)

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

## Comprehensive Setup

### 1. System Requirements

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

### 2. Java Installation Options

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

# List and install Java versions
sdk list java
sdk install java 21.0.8-oracle
sdk use java 21.0.8-oracle

# Automatic switching
echo "21.0.8-oracle" > .sdkmanrc
```

### 3. Maven Configuration

#### Standard Installation
```bash
# Ubuntu/Debian
sudo apt install maven

# macOS
brew install maven

# Windows
choco install maven

# Verify
mvn --version
```

#### Custom Maven Settings
Create `~/.m2/settings.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <profiles>
    <profile>
      <id>conductor-dev</id>
      <properties>
        <!-- Increase memory for large builds -->
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>conductor-dev</activeProfile>
  </activeProfiles>
</settings>
```

### 4. Project Setup

#### Method 1: Direct Clone
```bash
git clone <repository-url>
cd Conductor

# Verify project structure
ls -la
# Should show: pom.xml, src/, README.md, etc.
```

#### Method 2: Fork and Clone (For Contributors)
```bash
# 1. Fork repository on GitHub
# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/Conductor.git
cd Conductor

# 3. Add upstream remote
git remote add upstream <original-repository-url>
git remote -v
```

#### Method 3: Template-Based Setup
```bash
# Create new project based on Conductor
mkdir my-conductor-app
cd my-conductor-app

# Download and extract template
curl -L <template-url> | tar xz --strip-components=1

# Initialize as new project
git init && git add . && git commit -m "Initial commit"
```

---

## Development Environment Options

### Option 1: IntelliJ IDEA (Recommended)

#### Installation
```bash
# Download IntelliJ IDEA Community (Free)
# https://www.jetbrains.com/idea/download/

# Or with package managers
# macOS
brew install --cask intellij-idea-ce

# Windows
choco install intellij-idea-community
```

#### Project Import
1. **Open IntelliJ IDEA**
2. **File > Open** → Select `Conductor` folder
3. **Trust Project** → Click "Trust Project"
4. **Wait for indexing** → Progress bar in bottom status
5. **Verify setup** → Check `src/main/java` is marked as source root

#### Essential Plugins
```
# Install via File > Settings > Plugins
- Lombok Plugin (for cleaner code)
- SonarLint (code quality)
- GitToolBox (enhanced git support)
- Rainbow Brackets (better code readability)
- String Manipulation (text utilities)
```

#### Run Configuration Setup
1. **Run > Edit Configurations**
2. **Add New > Application**
3. **Configure as follows:**
   ```
   Name: Book Creation Demo
   Main Class: com.skanga.conductor.demo.BookCreationDemo
   Program Arguments: "Your Test Topic"
   VM Options: -Xmx4g -Dfile.encoding=UTF-8
   Working Directory: $PROJECT_DIR$
   ```

### Option 2: Visual Studio Code

#### Installation and Setup
```bash
# Install VS Code
# https://code.visualstudio.com/download

# Install Java Extension Pack
code --install-extension vscjava.vscode-java-pack
code --install-extension redhat.java
code --install-extension vscjava.vscode-maven
```

#### Workspace Configuration
Create `.vscode/settings.json`:
```json
{
  "java.home": "/path/to/java-21",
  "java.configuration.detectJdksAtStart": true,
  "java.compile.nullAnalysis.mode": "automatic",
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
  "maven.executable.path": "/path/to/maven/bin/mvn",
  "files.exclude": {
    "**/target": true,
    "**/.classpath": true,
    "**/.project": true,
    "**/.settings": true
  }
}
```

#### Launch Configuration
Create `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Book Demo",
      "request": "launch",
      "mainClass": "com.skanga.conductor.demo.BookCreationDemo",
      "args": ["VS Code Demo Test"],
      "vmArgs": "-Xmx4g"
    },
    {
      "type": "java",
      "name": "Debug Tests",
      "request": "launch",
      "mainClass": "org.junit.platform.console.ConsoleLauncher",
      "args": ["--scan-classpath"]
    }
  ]
}
```

### Option 3: Eclipse IDE

#### Installation
```bash
# Download Eclipse IDE for Java Developers
# https://www.eclipse.org/downloads/

# Import Project
# File > Import > Existing Maven Projects
# Select Conductor folder > Finish
```

#### Project Configuration
1. **Right-click project** → Properties
2. **Java Build Path** → Libraries
3. **Add Library** → JUnit 5
4. **Java Compiler** → Set compliance level to 21

### Option 4: Command Line Development

#### Environment Setup
```bash
# Create development aliases
echo 'alias conductor-build="mvn clean compile"' >> ~/.bashrc
echo 'alias conductor-test="mvn test"' >> ~/.bashrc
echo 'alias conductor-demo="mvn exec:java@book-demo"' >> ~/.bashrc
echo 'alias conductor-clean="mvn clean && rm -rf output/ logs/"' >> ~/.bashrc

source ~/.bashrc
```

#### Development Workflow
```bash
# Quick development cycle
conductor-clean          # Clean artifacts
conductor-build          # Compile sources
conductor-test           # Run tests
conductor-demo -Dexec.args="CLI Demo"  # Run demo
```

---

## IDE Configuration

### Code Style and Formatting

#### IntelliJ IDEA Settings
```
File > Settings > Editor > Code Style > Java
- Scheme: GoogleStyle (import from GitHub)
- Imports:
  - Class count to use import with '*': 999
  - Names count to use static import with '*': 999
- Wrapping and Braces:
  - Keep when reformatting: Line breaks ✓
```

#### Import Google Java Style
```bash
# Download Google Style guide
curl -o google-java-format.xml https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml

# Import in IntelliJ:
# File > Settings > Editor > Code Style > Java
# Click gear icon > Import Scheme > IntelliJ IDEA code style XML
# Select downloaded file
```

### Debugging Configuration

#### Advanced Debug Setup
```java
// VM Options for debugging
-Xdebug
-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005
-Dconductor.debug.level=TRACE
-Dlogback.configurationFile=src/test/resources/logback-debug.xml
```

#### Debug Properties File
Create `debug.properties`:
```properties
# Enhanced debugging
conductor.llm.mock.enabled=true
conductor.llm.mock.responses.file=src/test/resources/mock-responses.json
conductor.memory.store.debug=true
conductor.workflow.stage.timeout=300000
conductor.tools.validation.strict=false
```

### Live Templates and Snippets

#### IntelliJ Live Templates
```
# Add via File > Settings > Editor > Live Templates > Java

# Conductor Agent Template (Abbreviation: condagent)
public class $CLASS_NAME$ implements SubAgent {
    private static final Logger logger = LoggerFactory.getLogger($CLASS_NAME$.class);

    private final String name;
    private final LLMProvider llmProvider;

    public $CLASS_NAME$(String name, LLMProvider llmProvider) {
        this.name = requireNonNull(name, "name cannot be null");
        this.llmProvider = requireNonNull(llmProvider, "llmProvider cannot be null");
    }

    @Override
    public TaskResult execute(TaskInput input) throws ConductorException {
        logger.info("Executing agent: {}", name);
        // TODO: Implement execution logic
        return TaskResult.success("Result");
    }
}

# Test Template (Abbreviation: condtest)
@Test
@DisplayName("Should $ACTION$ when $CONDITION$")
void test$METHOD_NAME$() {
    // Arrange
    $SETUP$

    // Act
    $EXECUTION$

    // Assert
    $ASSERTIONS$
}
```

---

## Testing Setup

### Test Environment Configuration

#### Create Test Properties
`src/test/resources/test.properties`:
```properties
# Test database configuration
conductor.database.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
conductor.database.username=sa
conductor.database.password=

# Mock LLM responses
conductor.llm.provider=mock
conductor.llm.mock.responses.path=src/test/resources/mock-responses.json

# Test timeouts (shorter for faster tests)
conductor.workflow.stage.timeout=5000
conductor.agent.execution.timeout=3000
conductor.tool.execution.timeout=1000

# Test output directories
conductor.output.base.dir=target/test-output
conductor.logs.dir=target/test-logs
```

#### Mock Response Configuration
`src/test/resources/mock-responses.json`:
```json
{
  "title_generation": {
    "patterns": [
      "The Complete Guide to {{topic}}",
      "Mastering {{topic}}: A Comprehensive Guide",
      "{{topic}} Handbook: Essential Knowledge"
    ]
  },
  "chapter_generation": {
    "patterns": [
      "# Chapter {{chapter_number}}: {{chapter_title}}\n\nThis chapter covers {{topic}} fundamentals...",
      "# {{chapter_number}}. {{chapter_title}}\n\nKey concepts in {{topic}}..."
    ]
  },
  "content_generation": {
    "patterns": [
      "{{topic}} is an important field that encompasses...",
      "Understanding {{topic}} requires knowledge of..."
    ]
  }
}
```

### Test Data Management

#### Test Data Factory
```java
public class TestDataFactory {
    public static WorkflowDefinition createBasicWorkflow() {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName("test-workflow");
        workflow.setDescription("Test workflow for development");

        WorkflowStage stage = new WorkflowStage();
        stage.setName("test-stage");
        stage.setAgent("test-agent");
        stage.setPromptTemplate("Test prompt: {{input}}");

        workflow.setStages(List.of(stage));
        return workflow;
    }

    public static AgentDefinition createTestAgent() {
        AgentDefinition agent = new AgentDefinition();
        agent.setName("test-agent");
        agent.setDescription("Test agent for development");
        agent.setProvider("mock");
        agent.setModel("mock-model");
        return agent;
    }
}
```

### Performance Testing Setup

#### Load Testing Configuration
```java
@Test
@DisplayName("Should handle concurrent workflow execution")
void testConcurrentWorkflowExecution() throws Exception {
    int threadCount = 10;
    int operationsPerThread = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);

    List<Future<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount * operationsPerThread; i++) {
        Future<Boolean> future = executor.submit(() -> {
            try {
                WorkflowResult result = engine.executeWorkflow(testStages);
                latch.countDown();
                return result.isSuccess();
            } catch (Exception e) {
                latch.countDown();
                return false;
            }
        });
        futures.add(future);
    }

    // Wait for completion with timeout
    assertTrue(latch.await(30, TimeUnit.SECONDS));

    // Verify all operations succeeded
    long successCount = futures.stream()
        .mapToLong(f -> {
            try { return f.get() ? 1 : 0; }
            catch (Exception e) { return 0; }
        }).sum();

    assertEquals(threadCount * operationsPerThread, successCount);
    executor.shutdown();
}
```

---

## Advanced Development Workflows

### Workflow 1: Feature Development with TDD

#### Step-by-Step Process
```bash
# 1. Create feature branch
git checkout -b feature/new-agent-type

# 2. Write failing test first
cat > src/test/java/com/skanga/conductor/agent/NewAgentTypeTest.java << 'EOF'
@Test
void testNewAgentExecution() {
    NewAgentType agent = new NewAgentType("test", mockLLM);
    TaskResult result = agent.execute(new TaskInput("test"));
    assertTrue(result.success());
}
EOF

# 3. Run test (should fail)
mvn test -Dtest=NewAgentTypeTest

# 4. Implement minimum code to pass
# ... implement NewAgentType class ...

# 5. Run test (should pass)
mvn test -Dtest=NewAgentTypeTest

# 6. Refactor and improve
# 7. Run full test suite
mvn test

# 8. Commit changes
git add . && git commit -m "feat: add NewAgentType with comprehensive tests"
```

### Workflow 2: Integration Testing

#### Local Integration Setup
```bash
# 1. Create integration test profile
cat > integration-test-profile.xml << 'EOF'
<profile>
    <id>integration-tests</id>
    <properties>
        <conductor.llm.provider>openai</conductor.llm.provider>
        <conductor.llm.api.key>${env.OPENAI_API_KEY}</conductor.llm.api.key>
        <conductor.database.url>jdbc:h2:./target/integration-db</conductor.database.url>
    </properties>
</profile>
EOF

# 2. Set up environment
export OPENAI_API_KEY="your-api-key"
export INTEGRATION_TEST_MODE="true"

# 3. Run integration tests
mvn verify -Pintegration-tests -Dtest=*IntegrationTest
```

### Workflow 3: Performance Profiling

#### JProfiler Integration
```bash
# 1. Add JVM options for profiling
export MAVEN_OPTS="-agentpath:/path/to/jprofiler/bin/agent.so=port=8849 -Xmx8g"

# 2. Run performance-sensitive demo
mvn exec:java@book-demo -Dexec.args="Performance Test Topic" &

# 3. Connect JProfiler to port 8849
# 4. Monitor memory usage, CPU, and method calls
```

#### Built-in Metrics
```java
// Enable comprehensive metrics
@Test
void testWithMetrics() {
    MetricsRegistry.reset();
    MetricsRegistry.enableDetailed(true);

    // Execute operation
    WorkflowResult result = engine.executeWorkflow(stages);

    // Analyze metrics
    MetricsSummary summary = MetricsRegistry.getSummary();
    System.out.println("Execution time: " + summary.getTotalExecutionTime());
    System.out.println("Memory usage: " + summary.getPeakMemoryUsage());
    System.out.println("Agent calls: " + summary.getAgentCallCount());
}
```

### Workflow 4: Documentation-Driven Development

#### Auto-Generated API Docs
```bash
# 1. Generate JavaDoc
mvn javadoc:javadoc

# 2. Generate dependency graph
mvn dependency:tree -DoutputFile=dependency-tree.txt

# 3. Generate test coverage report
mvn jacoco:report

# 4. Create comprehensive docs
mvn site

# 5. Serve documentation locally
python -m http.server 8080 -d target/site
```

---

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: Java Version Conflicts
```bash
# Symptom: UnsupportedClassVersionError
# Solution: Verify Java version
java --version
javac --version

# If versions differ, set JAVA_HOME explicitly
export JAVA_HOME=/path/to/java-21
export PATH=$JAVA_HOME/bin:$PATH

# Or use update-alternatives (Linux)
sudo update-alternatives --config java
```

#### Issue 2: Maven Dependency Issues
```bash
# Symptom: Could not resolve dependencies
# Solution: Clear local repository
rm -rf ~/.m2/repository/com/skanga
mvn clean compile -U

# Force dependency refresh
mvn dependency:purge-local-repository
mvn clean install
```

#### Issue 3: Memory Issues During Development
```bash
# Symptom: OutOfMemoryError
# Solution: Increase Maven memory
export MAVEN_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"

# Or create .mvn/jvm.config
mkdir -p .mvn
echo "-Xmx4g -XX:MaxMetaspaceSize=512m" > .mvn/jvm.config
```

#### Issue 4: Test Database Locks
```bash
# Symptom: Database already in use
# Solution: Clean test databases
find target -name "*.db" -delete
rm -rf target/test-db*

# Or use different test database per test class
@TestMethodOrder(OrderAnnotation.class)
class MyTest {
    private static final String DB_NAME = "testdb-" + MyTest.class.getSimpleName();
}
```

#### Issue 5: IDE Performance Issues
```bash
# IntelliJ IDEA optimization
# Help > Edit Custom VM Options
-Xmx8g
-XX:ReservedCodeCacheSize=1g
-XX:+UseG1GC
-Didea.max.intellisense.filesize=5000

# Exclude unnecessary directories
# File > Settings > Build > Compiler > Excludes
# Add: target/, logs/, output/, .idea/, *.iml
```

### Debugging Techniques

#### Enable Verbose Logging
```xml
<!-- src/test/resources/logback-debug.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.skanga.conductor" level="TRACE"/>
    <logger name="org.springframework" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### Remote Debugging Setup
```bash
# Start application with remote debug
mvn exec:java@book-demo \
  -Dexec.args="Debug Topic" \
  -Dexec.options="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

# Connect from IDE:
# Run > Edit Configurations > Add New > Remote JVM Debug
# Host: localhost, Port: 5005
```

#### Memory Debugging
```bash
# Generate heap dump on OOM
export MAVEN_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./target/heapdump.hprof"

# Analyze with Eclipse MAT or VisualVM
# Download: https://www.eclipse.org/mat/
```

### Environment-Specific Issues

#### Windows Development
```powershell
# PowerShell execution policy
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Path separator issues
# Use system property: -Dfile.separator="\"
# Or normalize paths in code:
Paths.get("output", "file.txt").toString()
```

#### macOS Development
```bash
# Homebrew Java switching
brew install --cask temurin21
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk

# Fix Maven wrapper permissions
chmod +x mvnw
```

#### Linux Development
```bash
# Ubuntu/Debian specific
sudo apt update
sudo apt install openjdk-21-jdk maven git curl

# CentOS/RHEL specific
sudo yum install java-21-openjdk-devel maven git curl

# File permissions
find . -name "*.sh" -exec chmod +x {} \;
```

---

## Development Best Practices

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

### Configuration Management

#### Environment-Specific Configs
```bash
# Development
src/main/resources/application-dev.properties

# Testing
src/test/resources/application-test.properties

# Production
src/main/resources/application-prod.properties

# Load with profile
mvn exec:java -Dspring.profiles.active=dev
```

#### Secure Configuration
```properties
# Use environment variables for secrets
conductor.llm.api.key=${CONDUCTOR_LLM_API_KEY:default-dev-key}
conductor.database.password=${CONDUCTOR_DB_PASSWORD:}

# Use strong defaults
conductor.security.tool.whitelist=file_read,web_search,text_to_speech
conductor.security.path.traversal.protection=true
conductor.memory.store.encryption.enabled=true
```

### Testing Strategies

#### Test Pyramid Implementation
```java
// Unit Tests (70%): Fast, isolated
@Test
void testAgentCreation() {
    ConversationalAgent agent = new ConversationalAgent(name, desc, mockLLM, memory);
    assertEquals(name, agent.getName());
}

// Integration Tests (20%): Component interactions
@Test
void testWorkflowExecution() {
    WorkflowResult result = engine.executeWorkflow(stages);
    assertTrue(result.isSuccess());
}

// End-to-End Tests (10%): Full system
@Test
void testBookCreationDemo() {
    BookManuscript book = BookCreationDemo.createBook("Test Topic");
    assertNotNull(book.getTitle());
    assertTrue(book.getChapters().size() > 0);
}
```

#### Test Data Builders
```java
public class WorkflowTestDataBuilder {
    private WorkflowDefinition workflow = new WorkflowDefinition();

    public static WorkflowTestDataBuilder aWorkflow() {
        return new WorkflowTestDataBuilder();
    }

    public WorkflowTestDataBuilder withName(String name) {
        workflow.setName(name);
        return this;
    }

    public WorkflowTestDataBuilder withStage(WorkflowStage stage) {
        workflow.getStages().add(stage);
        return this;
    }

    public WorkflowDefinition build() {
        return workflow;
    }
}

// Usage in tests
@Test
void testComplexWorkflow() {
    WorkflowDefinition workflow = aWorkflow()
        .withName("complex-test")
        .withStage(createTitleStage())
        .withStage(createChapterStage())
        .build();

    WorkflowResult result = engine.executeWorkflow(workflow);
    assertTrue(result.isSuccess());
}
```

### Performance Optimization

#### Memory Management
```java
// Use appropriate collection sizes
List<String> items = new ArrayList<>(expectedSize);
Map<String, Object> cache = new HashMap<>(expectedSize, 0.75f);

// Clear large objects explicitly
try {
    processLargeDataset(dataset);
} finally {
    dataset.clear();
    dataset = null;
}

// Use try-with-resources
try (MemoryStore store = new H2MemoryStore(url)) {
    // Use store
} // Automatically closed
```

#### Concurrency Best Practices
```java
// Use concurrent collections
private final Map<String, SubAgent> agents = new ConcurrentHashMap<>();

// Proper thread pool management
private final ExecutorService executor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors(),
    new ThreadFactoryBuilder()
        .setNameFormat("conductor-worker-%d")
        .setDaemon(true)
        .build()
);

// Shutdown gracefully
@PreDestroy
public void shutdown() {
    executor.shutdown();
    try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

### Documentation Standards

#### JavaDoc Best Practices
```java
/**
 * Executes a workflow stage with retry logic and performance monitoring.
 *
 * <p>This method handles the execution of individual workflow stages with built-in
 * retry capabilities and comprehensive performance metrics collection. It supports
 * both synchronous and asynchronous execution patterns.
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Basic execution
 * StageResult result = executor.executeStage(stageDefinition, context);
 *
 * // With custom timeout
 * StageResult result = executor.executeStage(stageDefinition, context, Duration.ofMinutes(5));
 * }</pre>
 *
 * <h3>Error Handling:</h3>
 * <p>The method implements exponential backoff retry logic for transient failures.
 * Permanent failures are propagated immediately without retry attempts.
 *
 * @param stageDefinition the stage configuration containing agent settings and prompts
 * @param executionContext the current workflow execution context with variables
 * @param timeout maximum execution time before timeout (optional)
 * @return stage execution result containing output and performance metrics
 * @throws ConductorException if stage execution fails after all retry attempts
 * @throws IllegalArgumentException if stageDefinition or executionContext is null
 * @throws TimeoutException if execution exceeds the specified timeout
 *
 * @since 1.0.0
 * @see StageDefinition
 * @see ExecutionContext
 * @see StageResult
 */
public StageResult executeStage(StageDefinition stageDefinition,
                               ExecutionContext executionContext,
                               Duration timeout) throws ConductorException {
    // Implementation
}
```

#### README Templates
```markdown
# Feature: [Feature Name]

## Overview
Brief description of what this feature does and why it's important.

## Quick Start
```java
// Minimal working example
FeatureClass feature = new FeatureClass(config);
Result result = feature.execute(input);
```

## Configuration
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `feature.enabled` | boolean | `true` | Enable/disable feature |
| `feature.timeout` | int | `30000` | Timeout in milliseconds |

## Examples
### Basic Usage
[Detailed example]

### Advanced Usage
[Complex scenario]

## Testing
```bash
# Run feature-specific tests
mvn test -Dtest=*FeatureTest
```

## Troubleshooting
Common issues and solutions.
```

This comprehensive developer setup guide provides multiple approaches and detailed configurations for different development environments, ensuring developers can quickly get productive with the Conductor framework regardless of their preferred tools and workflows.