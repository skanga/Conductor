# Book Creation Demo - Comprehensive AI-Powered Book Writing Workflow

This demo showcases an advanced AI-powered book creation system that transforms a simple topic prompt into a complete, professionally structured book through a multi-stage workflow with agentic review and human-in-the-loop approval.

## 🚀 Quick Start

### Option 1: Run with Maven
```bash
# Run the book creation demo
mvn exec:java@book-demo

# Or with a custom topic
mvn exec:java@book-demo -Dexec.args="Machine Learning in Healthcare"
```

### Option 2: Run with Fat JAR (Standalone)
```bash
# Build the fat JARs (includes all dependencies)
mvn clean package -q

# Run the book creation demo
java -jar target/conductor-book-demo.jar

# Or with a custom topic
java -jar target/conductor-book-demo.jar "Sustainable Energy Solutions"

# Run the original demo
java -jar target/conductor-demo.jar
```

## 📚 What This Demo Does

The Book Creation Demo implements a comprehensive, multi-stage workflow that creates a complete book from just a topic prompt:

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

## 🎯 Example Output Structure

The demo creates a timestamped output directory with the following files:

```
data/book-creation-YYYYMMDD-HHMMSS/
├── 01-title-YYYYMMDD-HHMMSS.md          # Title and subtitle with reviews
├── 02-toc-YYYYMMDD-HHMMSS.md            # Table of contents with reviews
├── 03-chapter-01-YYYYMMDD-HHMMSS.md     # Individual chapters with reviews
├── 03-chapter-02-YYYYMMDD-HHMMSS.md
├── ...
└── 04-complete-book-YYYYMMDD-HHMMSS.md  # Complete book in one file
```

## ⚙️ Configuration Options

You can customize the demo behavior through `demo.properties` or environment variables. The following settings directly affect the **WORKFLOW CONFIGURATION** section shown when the demo starts.

### 📁 Output Directory Configuration
```properties
# Output directory settings
demo.database.temporary=true        # Use temporary databases (default: true)
demo.database.temp.dir=./data/temp  # Temporary database directory
demo.database.persistent.dir=./data # Persistent database directory

# Custom output directory (overrides automatic timestamped directories)
demo.output.directory=/path/to/custom/output
```

**Environment Variables:**
```bash
export DEMO_DATABASE_TEMPORARY=false
export DEMO_OUTPUT_DIRECTORY="/custom/book/output"
```

### 🤖 Provider/Model Configuration
```properties
# LLM Provider settings (affects "Provider", "Model", and "Base URL" in workflow config)
demo.provider.type=mock             # Options: mock, openai, anthropic, google, azure, ollama
demo.provider.model=gpt-4           # Model name for the provider
demo.provider.base.url=http://localhost:11434  # Custom base URL (for Ollama, etc.)
demo.provider.api.key=your-api-key  # API key for cloud providers

# Provider-specific settings
demo.openai.model=gpt-4-turbo       # OpenAI model override
demo.anthropic.model=claude-3-sonnet # Anthropic model override
demo.google.model=gemini-pro        # Google model override
demo.ollama.model=llama2            # Ollama model override
```

**Environment Variables:**
```bash
export DEMO_PROVIDER_TYPE=openai
export DEMO_PROVIDER_MODEL=gpt-4-turbo
export DEMO_PROVIDER_BASE_URL=https://api.openai.com/v1
export DEMO_PROVIDER_API_KEY=sk-your-openai-key-here
export OPENAI_API_KEY=sk-your-openai-key-here  # Alternative
```

### 📝 Book Generation Settings
```properties
# Book generation settings (affects "Target Words" and "Max Words")
demo.book.target.words=400          # Target words per chapter
demo.book.max.words=600             # Maximum words per chapter
demo.book.min.chapters=3            # Minimum chapters to generate
demo.book.max.chapters=12           # Maximum chapters to generate

# Quality settings
demo.book.review.enabled=true       # Enable agentic review (default: true)
demo.book.approval.timeout=300      # Human approval timeout (seconds)
```

**Environment Variables:**
```bash
export DEMO_BOOK_TARGET_WORDS=800
export DEMO_BOOK_MAX_WORDS=1200
export DEMO_BOOK_MAX_CHAPTERS=15
```

### 🔍 Verbose Logging Configuration
```properties
# Logging settings (affects "Verbose Logging" in workflow config)
demo.logging.verbose=true           # Enable detailed logging (default: false)
demo.logging.level=INFO             # Log level: DEBUG, INFO, WARN, ERROR
demo.logging.agent.details=true     # Show agent interaction details
demo.logging.metrics.enabled=true   # Enable metrics logging

# Console output
demo.console.colors=true            # Enable colored console output
demo.console.progress=true          # Show progress indicators
```

**Environment Variables:**
```bash
export DEMO_LOGGING_VERBOSE=true
export DEMO_LOGGING_LEVEL=DEBUG
export DEMO_CONSOLE_COLORS=false
```

### 🔧 Advanced Workflow Configuration
```properties
# Memory and processing
demo.memory.limit=50                # Memory entries per agent
demo.workflow.timeout=30            # Workflow timeout (minutes)
demo.agent.parallel.enabled=true    # Enable parallel agent execution

# Retry and resilience
demo.retry.max.attempts=3           # Maximum retry attempts
demo.retry.delay=1000               # Retry delay in milliseconds
demo.circuit.breaker.enabled=true   # Enable circuit breaker

# Custom prompts and templates
demo.prompts.book.topic=Your custom default topic here
demo.book.writer.prompt.template=Custom writer instructions
demo.book.editor.prompt.template=Custom editor instructions
demo.book.critic.prompt.template=Custom critic instructions
```

### 🏃 Quick Configuration Examples

#### Example 1: Production OpenAI Setup
```bash
# Set environment variables
export DEMO_PROVIDER_TYPE=openai
export DEMO_PROVIDER_MODEL=gpt-4-turbo
export OPENAI_API_KEY=sk-your-key-here
export DEMO_BOOK_TARGET_WORDS=1000
export DEMO_LOGGING_VERBOSE=true
export DEMO_DATABASE_TEMPORARY=false

# Run with larger chapters and persistent storage
mvn exec:java@book-demo -Dexec.args="Advanced Machine Learning Techniques"
```

#### Example 2: Local Ollama Setup
```bash
# Configure for local Ollama
export DEMO_PROVIDER_TYPE=ollama
export DEMO_PROVIDER_BASE_URL=http://localhost:11434
export DEMO_PROVIDER_MODEL=llama2
export DEMO_BOOK_TARGET_WORDS=600

# Run with local model
java -jar target/conductor-book-demo.jar "Open Source AI Development"
```

#### Example 3: Development/Testing Setup
```bash
# Fast testing with mock provider
export DEMO_PROVIDER_TYPE=mock
export DEMO_BOOK_TARGET_WORDS=200
export DEMO_BOOK_MAX_WORDS=300
export DEMO_LOGGING_VERBOSE=false

# Quick test run
mvn exec:java@book-demo -Dexec.args="Test Book Creation"
```

### 📄 Configuration File Options

#### Option 1: External Configuration File (Recommended for Fat JAR)
```bash
# Create a custom config file anywhere
cat > my-book-config.properties << EOF
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
demo.book.target.words=800
demo.book.max.words=1200
demo.logging.verbose=true
demo.database.temporary=false
demo.output.directory=./my-books
openai.api.key=sk-your-actual-key-here
EOF

# Use with Maven
mvn exec:java@book-demo -Dconfig=my-book-config.properties -Dexec.args="AI Development Guide"

# Use with Fat JAR (no need to modify JAR contents!)
java -jar target/conductor-book-demo.jar --config=my-book-config.properties "AI Development Guide"
```

#### Option 2: Local demo.properties File
```bash
# Create demo.properties in current directory
cat > demo.properties << EOF
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
demo.book.target.words=800
demo.book.max.words=1200
demo.logging.verbose=true
demo.database.temporary=false
demo.output.directory=./my-books
EOF

# Automatically loaded when present
mvn exec:java@book-demo -Dexec.args="Your Book Topic"
java -jar target/conductor-book-demo.jar "Your Book Topic"
```

#### Option 3: System Properties (Command Line)
```bash
# Individual property overrides
java -jar target/conductor-book-demo.jar \
  -Ddemo.provider.type=openai \
  -Ddemo.book.target.words=1000 \
  -Dopenai.api.key=sk-your-key \
  "Advanced AI Topics"
```

### 🔄 Property to Environment Variable Conversion

Environment variables use a specific naming convention. Here are examples:

#### Conversion Rules
```
Property Format: demo.section.property.name=value
Environment Format: DEMO_SECTION_PROPERTY_NAME=value

Rules:
1. Convert to UPPERCASE
2. Replace dots (.) with underscores (_)
3. Add DEMO_ prefix if not present
```

#### Common Conversion Examples
```bash
# Property → Environment Variable
demo.provider.type=openai           → DEMO_PROVIDER_TYPE=openai
demo.book.target.words=800          → DEMO_BOOK_TARGET_WORDS=800
demo.logging.verbose=true           → DEMO_LOGGING_VERBOSE=true
demo.database.temporary=false       → DEMO_DATABASE_TEMPORARY=false
demo.provider.base.url=localhost    → DEMO_PROVIDER_BASE_URL=localhost
demo.output.directory=/my/path      → DEMO_OUTPUT_DIRECTORY=/my/path

# Special cases (provider-specific)
openai.api.key=sk-123               → OPENAI_API_KEY=sk-123
anthropic.api.key=sk-ant-123        → ANTHROPIC_API_KEY=sk-ant-123
```

#### Quick Conversion Script
```bash
# Convert any property to env var format
property_to_env() {
    echo "$1" | tr '[:lower:]' '[:upper:]' | sed 's/\./_/g' | sed 's/^/DEMO_/'
}

# Examples:
property_to_env "demo.book.target.words"     # → DEMO_DEMO_BOOK_TARGET_WORDS
property_to_env "book.target.words"          # → DEMO_BOOK_TARGET_WORDS
property_to_env "provider.type"              # → DEMO_PROVIDER_TYPE
```

### 🔄 Configuration Priority

Configuration is loaded in this priority order (highest to lowest):

1. **External configuration file** (`--config=path/to/config.properties`)
2. **System properties** (`-Dproperty=value`)
3. **Environment variables** (`DEMO_*`, `OPENAI_API_KEY`, etc.)
4. **demo.properties file** (current directory)
5. **application.properties** (built-in defaults)

### 📊 Workflow Configuration Display

When you run the demo, you'll see this configuration summary:

```
================================================================================
WORKFLOW CONFIGURATION
================================================================================
Topic: Your Book Topic Here
Output Directory: ./data/temp/demo-databases/book-creation-20231201-140530
Target Words per Chapter: 800
Max Words per Chapter: 1200
Provider: openai
Model: gpt-4-turbo
Base URL: https://api.openai.com/v1
Verbose Logging: true
Database Type: temporary
Memory Limit: 50
================================================================================
```

All the settings shown in this display can be customized using the configuration options above.

### 🔥 Fat JAR Configuration Best Practices

Since editing properties files inside JAR files is cumbersome, use external configuration:

#### Problem: Internal Configuration
```bash
# ❌ Difficult: Need to extract, edit, and repackage JAR
unzip conductor-book-demo.jar
nano application.properties  # Edit inside JAR
zip -u conductor-book-demo.jar application.properties
```

#### Solution: External Configuration
```bash
# ✅ Easy: External config file
cat > production.properties << EOF
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
openai.api.key=sk-your-production-key
demo.book.target.words=1000
demo.logging.verbose=false
demo.database.temporary=false
demo.output.directory=/var/books/output
EOF

# Use with any JAR without modification
java -jar conductor-book-demo.jar --config=production.properties "Production Book Topic"
```

#### Team Configuration Management
```bash
# Different configs for different environments
# development.properties
demo.provider.type=mock
demo.book.target.words=200
demo.logging.verbose=true

# staging.properties
demo.provider.type=openai
demo.provider.model=gpt-3.5-turbo
openai.api.key=sk-staging-key
demo.book.target.words=600

# production.properties
demo.provider.type=openai
demo.provider.model=gpt-4-turbo
openai.api.key=sk-production-key
demo.book.target.words=1200
demo.database.temporary=false

# Easy switching
java -jar conductor-book-demo.jar --config=development.properties "Dev Test"
java -jar conductor-book-demo.jar --config=staging.properties "Staging Test"
java -jar conductor-book-demo.jar --config=production.properties "Production Book"
```

#### Docker Deployment
```dockerfile
# Dockerfile
FROM openjdk:21-jre-slim
COPY conductor-book-demo.jar /app/
COPY production.properties /app/config/
WORKDIR /app
ENTRYPOINT ["java", "-jar", "conductor-book-demo.jar", "--config=config/production.properties"]
```

```bash
# docker-compose.yml
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

## 🔧 Technical Architecture

### Core Components

- **`BookCreationDemo`**: Main entry point and user interaction
- **`BookCreationWorkflow`**: Orchestrates the complete workflow
- **`BookManuscript`**: Complete book data structure
- **`BookTitleInfo`**: Title and subtitle with review comments
- **`TableOfContents`**: Structured TOC with chapter extraction
- **Supporting classes**: Various data structures and utilities

### AI Agent Roles

- **Title Generator**: Creates compelling titles and subtitles
- **Title Reviewer**: Reviews titles for marketability and clarity
- **TOC Generator**: Creates detailed table of contents
- **TOC Reviewer**: Reviews TOC structure and completeness
- **Chapter Writer**: Writes comprehensive chapter content
- **Chapter Reviewer**: Reviews individual chapters for quality
- **Book Reviewer**: Performs final review of complete manuscript

## 📋 Usage Examples

### Interactive Mode
```bash
# Using Maven
mvn exec:java@book-demo

# Using Fat JAR (standalone)
java -jar target/conductor-book-demo.jar
# Follow the prompts to enter your topic interactively
```

### Command Line with Topic
```bash
# Using Maven
mvn exec:java@book-demo -Dexec.args="The Future of Quantum Computing"

# Using Fat JAR (standalone)
java -jar target/conductor-book-demo.jar "The Future of Quantum Computing"
```

### With Custom Configuration
```bash
# Set environment variables
export DEMO_BOOK_TARGET_WORDS=1000
export DEMO_PROVIDER_TYPE=mock
export DEMO_LOGGING_VERBOSE=true

# Using Maven
mvn exec:java@book-demo -Dexec.args="Blockchain Technology Explained"

# Using Fat JAR (standalone)
java -jar target/conductor-book-demo.jar "Blockchain Technology Explained"
```

### Distribution and Deployment
The fat JAR approach provides several benefits:

- **Standalone Execution**: No need for Maven or dependency management
- **Easy Distribution**: Single JAR file contains everything needed
- **Production Ready**: Can be deployed to any Java environment
- **Docker Friendly**: Perfect for containerized deployments
- **CI/CD Integration**: Simple artifact for automated pipelines

## 🎮 Interactive Experience

During execution, you'll see prompts like:

```
================================================================================
HUMAN REVIEW REQUIRED: TITLE AND SUBTITLE
================================================================================
Please review the generated title and subtitle:

Title: The Complete Guide to Sustainable Energy Solutions
Subtitle: From Solar Power to Smart Grids - A Practical Approach

================================================================================
Do you approve this title and subtitle? (y/n/view):
```

Choose:
- `y` or `yes`: Approve and continue
- `n` or `no`: Regenerate content
- `view` or `v`: See complete content before deciding

## 🏗️ Sample Book Structure

Here's what a typical generated book might look like:

```markdown
# The Complete Guide to Sustainable Energy Solutions
## From Solar Power to Smart Grids - A Practical Approach

### Table of Contents

1. **Introduction to Sustainable Energy**
   - Overview of renewable energy landscape
   - Environmental and economic benefits
   - Current global energy challenges

2. **Solar Power Technologies**
   - Photovoltaic systems and applications
   - Solar thermal energy solutions
   - Installation and maintenance considerations

3. **Wind Energy Systems**
   - Wind turbine technology and efficiency
   - Offshore vs onshore wind farms
   - Grid integration challenges

[... additional chapters ...]
```

## 🚦 Quality Control Process

### Agentic Review Criteria

Each AI reviewer evaluates content based on:
- **Clarity and Readability**: Is the content easy to understand?
- **Structure and Flow**: Does the content follow a logical progression?
- **Educational Value**: Does it provide practical, actionable information?
- **Completeness**: Are all important aspects covered?
- **Consistency**: Does it align with the overall book theme?

### Human Approval Points

You'll be asked to approve:
1. **Title and Subtitle**: After agentic review for marketability
2. **Table of Contents**: After structural review for completeness
3. **Each Chapter**: After content quality review
4. **Complete Book**: After final coherence review

## 🔍 Troubleshooting

### Common Issues

**Demo won't start:**
```bash
# Check Java version (requires Java 21+)
java -version

# Compile the project
mvn clean compile
```

**Memory issues:**
```bash
# Reduce memory settings in demo.properties
demo.memory.limit=20
demo.book.target.words=400
```

**Long generation times:**
```bash
# Reduce scope or use mock provider
demo.provider.type=mock
demo.book.max.words=600
```

### Debug Mode

Enable verbose logging to see detailed workflow progress:
```bash
export DEMO_LOGGING_VERBOSE=true
mvn exec:java@book-demo
```

## 🤝 Extending the Demo

### Adding New Agent Types

1. Create a new agent with specific role:
```java
SubAgent customReviewer = orchestrator.createImplicitAgent(
    "custom-reviewer",
    "Reviews content for specific criteria",
    llmProvider,
    "Your custom prompt template here"
);
```

2. Integrate into the workflow stages

### Custom Output Formats

Modify the `BookCreationWorkflow` to support additional output formats:
- PDF generation
- HTML documentation
- EPUB book format
- LaTeX for academic publishing

### Additional Review Stages

Add specialized review phases:
- Technical accuracy review
- Style and tone consistency
- Fact-checking and citation validation
- Accessibility and readability assessment

## 📈 Performance Metrics

The demo tracks and reports:
- **Generation Time**: Time spent on each workflow stage
- **Word Count**: Total and per-chapter word counts
- **Review Cycles**: Number of iterations per stage
- **Approval Rate**: Success rate of first-pass content
- **Agent Performance**: Individual agent response quality

## 🌟 Best Practices

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

## 🔨 Building the Project

### Build Fat JARs
```bash
# Build both demo JARs with all dependencies included
mvn clean package

# This creates:
# - target/conductor-demo.jar (original demo)
# - target/conductor-book-demo.jar (book creation demo)
# - target/Conductor-1.0.0.jar (regular JAR without dependencies)
```

### Build Options
```bash
# Quick build (skip tests)
mvn clean package -DskipTests

# Verbose build with all details
mvn clean package -X

# Build and install to local repository
mvn clean install
```

## 📞 Support and Feedback

For issues, improvements, or questions about the Book Creation Demo:

1. Check the logs in the output directory for detailed error information
2. Review the configuration options to customize behavior
3. Examine the generated markdown files for content quality assessment

The demo showcases the power of multi-agent AI systems for complex, creative tasks while maintaining human oversight and quality control throughout the process.

---

**Ready to create your book?**

**Quick Start:**
```bash
mvn clean package
java -jar target/conductor-book-demo.jar "Your Book Topic"
```

**Or with Maven:**
```bash
mvn exec:java@book-demo -Dexec.args="Your Book Topic"
```