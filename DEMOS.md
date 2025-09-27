# Conductor Framework Demos

This document provides comprehensive information about all demo applications available in the Conductor framework.

## Quick Start

### Prerequisites
- Java 21 or higher
- Apache Maven 3.6+

### Building All Demos
```bash
# Build all demos with fat JAR support
mvn clean package

# Quick build (skip tests)
mvn clean package -DskipTests
```

## 1. Book Creation Demo

### Overview
The Book Creation Demo showcases an advanced AI-powered book creation system that transforms a simple topic prompt into a complete, professionally structured book through a multi-stage workflow with agentic review and human-in-the-loop approval.

### Quick Start

#### Run with Maven
```bash
# Interactive mode
mvn exec:java@book-demo

# With custom topic
mvn exec:java@book-demo -Dexec.args="Machine Learning in Healthcare"
```

#### Run with Standalone JAR
```bash
# Build the fat JAR
mvn clean package

# Run with topic
java -jar target/conductor-book-demo.jar "Sustainable Energy Solutions"
```

### Workflow Stages

1. **📝 Title & Subtitle Generation**
   - Generates compelling book titles and subtitles
   - Agentic review for marketability and clarity
   - Human approval required before proceeding

2. **📋 Table of Contents Creation**
   - Creates detailed chapter outlines with descriptions
   - Includes key points and writing guidelines for each chapter
   - Agentic review for logical flow and completeness
   - Human approval with option to regenerate

3. **✍️ Chapter-by-Chapter Writing**
   - Generates comprehensive content for each chapter
   - Each chapter includes introduction, main sections, examples, and conclusion
   - Agentic review for quality and educational value
   - Individual human approval for each chapter

4. **🔍 Final Book Review**
   - Complete book assessment for overall coherence
   - Final agentic review of the entire manuscript
   - Human approval for the complete book

### Key Features

- **🤖 Multi-Agent Architecture**: Specialized AI agents for different tasks (writing, editing, reviewing)
- **👥 Human-in-the-Loop**: Approval required at each stage for quality control
- **📄 Markdown Output**: All content saved in professional markdown format
- **🔄 Iterative Refinement**: Regenerate content based on feedback
- **💾 Persistent Memory**: AI agents learn and maintain context throughout the process
- **📊 Progress Tracking**: Real-time progress updates and statistics

### Output Structure

```
data/book-creation-YYYYMMDD-HHMMSS/
├── 01-title-YYYYMMDD-HHMMSS.md          # Title and subtitle with reviews
├── 02-toc-YYYYMMDD-HHMMSS.md            # Table of contents with reviews
├── 03-chapter-01-YYYYMMDD-HHMMSS.md     # Individual chapters with reviews
├── 03-chapter-02-YYYYMMDD-HHMMSS.md
├── ...
└── 04-complete-book-YYYYMMDD-HHMMSS.md  # Complete book in one file
```

### Configuration Options

#### Provider/Model Settings
```bash
# Environment variables
export DEMO_PROVIDER_TYPE=openai
export DEMO_PROVIDER_MODEL=gpt-4-turbo
export OPENAI_API_KEY=sk-your-key-here

# Properties file
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
demo.book.target.words=800
demo.logging.verbose=true
```

#### External Configuration
```bash
# Create custom config
cat > production.properties << EOF
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
demo.book.target.words=1000
openai.api.key=sk-your-production-key
EOF

# Use with JAR
java -jar conductor-book-demo.jar --config=production.properties "AI Development Guide"
```

### Usage Examples

#### Production OpenAI Setup
```bash
export DEMO_PROVIDER_TYPE=openai
export DEMO_PROVIDER_MODEL=gpt-4-turbo
export OPENAI_API_KEY=sk-your-key-here
export DEMO_BOOK_TARGET_WORDS=1000

mvn exec:java@book-demo -Dexec.args="Advanced Machine Learning Techniques"
```

#### Local Ollama Setup
```bash
export DEMO_PROVIDER_TYPE=ollama
export DEMO_PROVIDER_BASE_URL=http://localhost:11434
export DEMO_PROVIDER_MODEL=llama2

java -jar conductor-book-demo.jar "Open Source AI Development"
```

#### Development/Testing Setup
```bash
export DEMO_PROVIDER_TYPE=mock
export DEMO_BOOK_TARGET_WORDS=200

mvn exec:java@book-demo -Dexec.args="Test Book Creation"
```

## 2. YAML-Based Workflow Demo

### Overview
This demo demonstrates the same book creation functionality using YAML configuration files instead of programmatic workflows, showcasing the power of the configuration-driven workflow system.

### Quick Start

```bash
# Run YAML-based demo
mvn exec:java@workflow-demo -Dexec.args="Machine Learning Fundamentals"

# With custom configuration files
mvn exec:java@workflow-demo -Dexec.args="--workflow=custom/my-workflow.yaml --agents=custom/my-agents.yaml Machine Learning"
```

### Key Features

- **Configuration-Driven**: Define workflows in YAML without code changes
- **Agent Templates**: Reusable agent definitions and prompt templates
- **Variable Substitution**: Environment variables and dynamic values
- **Human Approval**: Built-in approval workflow with interactive console

### Configuration Files

#### Workflow Definition (workflow.yaml)
```yaml
workflow:
  name: "book-creation"
  description: "AI-powered book creation with human approval"
  version: "1.0"

stages:
  - name: "title-generation"
    description: "Generate compelling book title and subtitle"
    agents:
      generator: "title-generator"
      reviewer: "title-reviewer"
    approval:
      required: true
      timeout: "5m"
```

#### Agent Configuration (agents.yaml)
```yaml
agents:
  title-generator:
    type: "llm"
    role: "Generates compelling book titles and subtitles"
    provider: "${LLM_PROVIDER:-openai}"
    prompt_template: "title-generation"

prompt_templates:
  title-generation:
    system: |
      You are an expert book title generator and marketing specialist.
      Create compelling, marketable titles that capture attention.
    user: |
      Generate a compelling title and subtitle for: {{topic}}
      Target audience: {{target_audience}}
```

### CLI Options

- `--config=FILE`: Path to configuration properties file
- `--workflow=FILE`: Path to workflow YAML definition
- `--agents=FILE`: Path to agents YAML configuration
- `--context=FILE`: Path to context YAML file
- `--enable-approval, --approval`: Enable human approval workflow
- `--help, -h`: Show help message

### Benefits

- **Rapid Iteration**: Modify workflows without code recompilation
- **Non-Technical Access**: Product managers can configure workflows
- **A/B Testing**: Easy testing of different agent configurations
- **Environment Customization**: Dev/staging/prod workflow variations
- **Template Reuse**: Shareable workflow and agent templates

## 3. Workflow Comparison Demo

### Overview
Side-by-side comparison of code-based vs YAML-based approaches, demonstrating identical functional output from both methods.

### Quick Start

```bash
# Run comparison demo
mvn exec:java@compare-workflows -Dexec.args="Software Architecture"
```

### Features

- **Identical Behavior**: Both approaches produce identical outputs (verified by file diff)
- **Performance Analysis**: Compare execution times and resource usage
- **Benefits Demonstration**: Show advantages and trade-offs of each approach
- **Unified Architecture**: Both use same underlying execution primitives

## 4. Parallel Execution Demo

### Overview
Demonstrates intelligent parallel execution of independent tasks while respecting dependencies in the PlannerOrchestrator.

### Key Features

#### 🔍 Dependency Analysis
- Automatically detects task dependencies from template variables
- Supports direct task dependencies (`{{task_name}}`)
- Handles sequential dependencies (`{{prev_output}}`)
- Identifies independent tasks for parallel execution

#### ⚡ Intelligent Execution Strategy
- **Parallel Execution**: For independent tasks that can run concurrently
- **Sequential Execution**: For dependent tasks that must run in order
- **Mixed Execution**: Batches tasks by dependency level for optimal parallelism

#### 🛡️ Safety & Reliability
- Thread-safe execution with proper resource management
- Configurable timeouts and parallelism limits
- Graceful error handling and rollback capabilities
- Resource cleanup and connection pooling

### Configuration

```properties
# Parallel execution settings
conductor.orchestrator.parallel.enabled=true
conductor.orchestrator.max.parallel.tasks=4
conductor.orchestrator.task.timeout=300s
conductor.orchestrator.parallel.batch.delay=100ms
```

### Usage Examples

```bash
# Enable parallel execution
export CONDUCTOR_ORCHESTRATOR_PARALLEL_ENABLED=true
mvn exec:java@book-demo -Dexec.args="Parallel Processing Guide"

# Configure parallelism level
export CONDUCTOR_ORCHESTRATOR_MAX_PARALLEL_TASKS=8
java -jar conductor-book-demo.jar "High Performance Computing"
```

### Performance Benefits

- **Faster Execution**: Independent tasks run concurrently
- **Resource Efficiency**: Better CPU and network utilization
- **Scalable**: Configurable parallelism based on system capabilities
- **Smart Batching**: Wave-based execution respects dependencies

## 5. Human Approval Workflow Demo

### Overview
The YAML-based workflow system supports human approval at any stage, allowing for quality control and human oversight in AI-generated content workflows.

### Features

#### Interactive Approval Interface
- Console-based approval prompts with formatted content display
- Multiple action options: Approve, Reject, View again, Help
- Timeout support with configurable timeouts per stage
- Feedback collection for continuous improvement

#### Workflow Control
- Stage-level approval configuration in YAML
- Graceful failure handling when approval is denied
- Approval status tracking in execution results
- Feedback preservation for analysis

### Usage Examples

```bash
# Run with approval enabled
mvn exec:java@workflow-demo -Dexec.args="--enable-approval 'AI Book Creation'"

# Use custom workflow with approval
mvn exec:java@workflow-demo -Dexec.args="--enable-approval --workflow=my-workflow.yaml 'Custom Topic'"
```

### Approval Interface

When approval is required, users see:

```
================================================================================
🔍 HUMAN APPROVAL REQUIRED
================================================================================
Workflow: book-creation
Stage: title-generation
Description: Generate compelling book title and subtitle

📝 GENERATED CONTENT:
------------------------------------------------------------
[AI-generated content displayed with word wrapping]

📋 REVIEW FEEDBACK:
------------------------------------------------------------
[AI reviewer feedback if available]

================================================================================
Please review the content and choose an action:
  [A]pprove - Accept and continue to next stage
  [R]eject - Stop workflow execution
  [V]iew - Display content again
  [H]elp - Show detailed options

Your decision (A/R/V/H):
```

### Use Cases

- **Content Publishing**: Review AI-generated articles before publication
- **Technical Documentation**: Validate technical accuracy of generated docs
- **Marketing Materials**: Ensure brand consistency in generated content
- **Educational Content**: Review learning materials for accuracy and appropriateness
- **Research Reports**: Validate findings and conclusions before sharing

## Common Configuration

### Environment Variables

```bash
# Provider settings
export DEMO_PROVIDER_TYPE=openai
export DEMO_PROVIDER_MODEL=gpt-4-turbo
export OPENAI_API_KEY=sk-your-key-here

# Book generation settings
export DEMO_BOOK_TARGET_WORDS=800
export DEMO_BOOK_MAX_WORDS=1200

# Logging settings
export DEMO_LOGGING_VERBOSE=true
export DEMO_CONSOLE_COLORS=true

# Database settings
export DEMO_DATABASE_AUTO_CLEANUP=false
export DEMO_DATABASE_PRESERVE_FOR_DEBUG=true
```

### Properties File

```properties
# demo.properties
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
demo.book.target.words=800
demo.book.max.words=1200
demo.logging.verbose=true
demo.database.auto.cleanup=false
demo.database.preserve.for.debug=true
demo.output.directory=./my-books
```

### External Configuration

```bash
# Create external config
cat > production.properties << EOF
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
openai.api.key=sk-your-production-key
demo.book.target.words=1000
demo.logging.verbose=false
demo.database.auto.cleanup=false
EOF

# Use with any demo
java -jar conductor-book-demo.jar --config=production.properties "Production Book"
```

## Docker Deployment

### Dockerfile
```dockerfile
FROM openjdk:21-jre-slim
COPY conductor-book-demo.jar /app/
COPY production.properties /app/config/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "conductor-book-demo.jar", "--config=config/production.properties"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  book-creator:
    build: .
    environment:
      - DEMO_PROVIDER_TYPE=openai
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    volumes:
      - ./books:/app/output
    command: ["java", "-jar", "conductor-book-demo.jar", "--config=config/production.properties", "Automated Book Creation"]
```

## Troubleshooting

### Common Issues

#### Demo won't start
```bash
# Check Java version (requires Java 21+)
java -version

# Compile the project
mvn clean compile
```

#### Memory issues
```bash
# Reduce memory settings
export DEMO_MEMORY_LIMIT=20
export DEMO_BOOK_TARGET_WORDS=400
```

#### Long generation times
```bash
# Use mock provider or reduce scope
export DEMO_PROVIDER_TYPE=mock
export DEMO_BOOK_MAX_WORDS=600
```

### Debug Mode

```bash
# Enable verbose logging
export DEMO_LOGGING_VERBOSE=true
export DEMO_LOGGING_LEVEL=DEBUG

# Run with debug output
mvn exec:java@book-demo -Dexec.args="Debug Test"
```

## Best Practices

### For Optimal Results

1. **Provide Specific Topics**: More specific topics yield better, focused content
2. **Review Thoroughly**: Take time to review each stage carefully
3. **Use Feedback**: Provide specific feedback when regenerating content
4. **Save Iterations**: Keep previous versions for comparison
5. **Customize Prompts**: Tailor agent prompts for your specific domain

### Topic Examples That Work Well

- **Technical Guides**: "Building Microservices with Docker and Kubernetes"
- **Business Topics**: "Digital Transformation Strategies for SMBs"
- **Educational Content**: "Introduction to Machine Learning for Beginners"
- **How-To Guides**: "Complete Guide to Home Automation Systems"

## Performance Metrics

All demos track and report:
- **Generation Time**: Time spent on each workflow stage
- **Word Count**: Total and per-chapter word counts
- **Review Cycles**: Number of iterations per stage
- **Approval Rate**: Success rate of first-pass content
- **Agent Performance**: Individual agent response quality

## Support and Feedback

For issues, improvements, or questions about any demo:

1. Check the logs in the output directory for detailed error information
2. Review the configuration options to customize behavior
3. Examine the generated files for content quality assessment
4. Use debug mode to understand workflow execution

---

**Ready to try the demos?**

**Quick Start:**
```bash
mvn clean package
java -jar target/conductor-book-demo.jar "Your Book Topic"
```

**Or with Maven:**
```bash
mvn exec:java@book-demo -Dexec.args="Your Book Topic"
```