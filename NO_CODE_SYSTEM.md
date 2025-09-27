# YAML-Based Workflow System Implementation

## 🎯 Overview

This document describes the implementation of a comprehensive YAML-based workflow system for the Conductor framework. The system allows users to define complex AI workflows using YAML configuration files instead of programmatic Java logic.

**🔄 UNIFIED ARCHITECTURE**: As of the latest implementation, both YAML-based workflows and code-based workflows use **identical underlying execution primitives** through the `DefaultWorkflowEngine`. This ensures 100% behavioral consistency between both approaches.

## 📁 Architecture

### Package Structure
```
com.skanga.conductor.workflow/
├── config/                           # Configuration models and loading
│   ├── WorkflowDefinition.java       # Root workflow configuration
│   ├── WorkflowStage.java            # Individual workflow stages
│   ├── AgentDefinition.java          # Agent configuration model
│   ├── AgentConfigCollection.java    # Collection of agents and templates
│   ├── WorkflowContext.java          # Runtime context handling
│   ├── WorkflowConfigLoader.java     # YAML configuration loader
│   └── VariableSubstitution.java     # Variable interpolation engine
├── engine/                           # Workflow execution engine (DEPRECATED)
│   ├── YamlWorkflowEngine.java       # Legacy execution engine (replaced by DefaultWorkflowEngine)
│   ├── WorkflowExecutionContext.java # Execution context
│   ├── WorkflowExecutionResult.java  # Execution results
│   └── StageExecutionResult.java     # Individual stage results
├── adapters/                         # NEW: Unified architecture adapters
│   └── YamlWorkflowEngine.java # Converts YAML to DefaultWorkflowEngine primitives
├── templates/                        # Agent and prompt template system
│   ├── AgentFactory.java             # Creates agents from config
│   └── PromptTemplateEngine.java     # Template rendering with variables
└── runners/                          # Demo applications
    ├── (Updated to YAML-based demos)     # YAML-based book creation demo
    └── (Removed legacy comparison demos for simplified 2-option system)
```

### Configuration Files
```
src/main/resources/yaml/
├── workflows/
│   └── book-creation.yaml       # Workflow definition
├── agents/
│   └── book-agents.yaml         # Agent definitions and prompt templates
└── context/
    └── book-context.yaml        # Runtime context and variables
```

## 🔧 Key Components

### 1. Configuration System

#### WorkflowDefinition
- Parses YAML workflow configurations
- Defines workflow metadata, settings, stages, and variables
- Supports validation and error reporting

#### AgentConfigCollection
- Manages agent definitions and prompt templates
- Supports template inheritance and variable substitution
- Validates agent configurations and template references

#### VariableSubstitution
- Handles `${VARIABLE}` and `${VARIABLE:-default}` syntax
- Supports environment variables, system properties, and built-in variables
- Provides timestamp, UUID, and system information variables

### 2. Execution Engine

#### YamlWorkflowEngine (NEW - Unified Architecture)
- **Primary YAML execution system** that uses the `DefaultWorkflowEngine`
- Converts YAML configurations to `StageDefinition` and `AgentDefinition` objects
- Ensures **identical behavior** to code-based workflows
- Uses same retry logic, validation, and context management as code-based workflows

#### YamlWorkflowEngine (DEPRECATED)
- Legacy YAML orchestrator (replaced by unified architecture)
- Maintained for backward compatibility
- Will be removed in future versions

#### WorkflowExecutionResult
- Captures execution metrics, timing, and success status
- Provides detailed stage-by-stage results
- Supports execution summaries and performance analysis

### 3. Agent Factory System

#### AgentFactory
- Creates agents dynamically from YAML definitions
- Supports LLM agents with configurable prompt templates
- Extensible for future agent types (tool-based, etc.)
- Handles agent lifecycle and caching

#### PromptTemplateEngine
- Renders prompt templates with {{variable}} syntax
- Supports system, user, and assistant message templates
- Validates template syntax and variable references
- Enables template composition and reuse

## 📋 Configuration Examples

### Workflow Definition (book-creation.yaml)
```yaml
workflow:
  name: "book-creation"
  description: "AI-powered book creation with human approval"
  version: "1.0"

settings:
  output_dir: "data/workflow-books/${timestamp}"
  max_retries: 3
  target_words_per_chapter: 800

stages:
  - name: "title-generation"
    description: "Generate compelling book title and subtitle"
    agents:
      generator: "title-generator"
      reviewer: "title-reviewer"
    approval:
      required: true
    outputs:
      - "01-title-${timestamp}.md"

variables:
  topic: "${BOOK_TOPIC:-Introduction to AI}"
  author: "${AUTHOR_NAME:-AI Assistant}"
```

### Agent Configuration (book-agents.yaml)
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

      Format: Title: [title] / Subtitle: [subtitle]
```

## 🚀 Usage Examples

### Running No-Code Workflows

#### Command Line Execution

**Basic Usage (Default Configuration)**
```bash
# Run no-code book creation demo with default YAML files
mvn exec:java@workflow-demo -Dexec.args="Machine Learning Fundamentals"

# Run workflow comparison
mvn exec:java@compare-workflows -Dexec.args="Software Architecture"

# Original code-based approach
mvn exec:java@book-demo -Dexec.args="Database Design"
```

**Custom Configuration Files**
```bash
# Use custom YAML configuration files
mvn exec:java@workflow-demo -Dexec.args="--workflow=custom/my-workflow.yaml --agents=custom/my-agents.yaml --context=custom/my-context.yaml AI Book Creation"

# Use specific configuration with real LLM provider
mvn exec:java@workflow-demo -Dexec.args="--config=demo.config --workflow=external-configs/workflows/technical-book.yaml Microservices Patterns"
```

**Standalone JAR Usage**
```bash
# Show help for all available options
java -jar conductor.jar --help

# Use default configuration
java -jar conductor.jar "AI and Machine Learning"

# Use custom configuration files for deployment
java -jar conductor.jar --config=/opt/config/production.config \
                        --workflow=/opt/workflows/technical-book.yaml \
                        --agents=/opt/configs/expert-agents.yaml \
                        --context=/opt/contexts/production-context.yaml \
                        "Kubernetes Best Practices"

# Use external YAML files (useful for containerized deployments)
java -jar conductor.jar --workflow=/mnt/configs/workflow.yaml \
                        --agents=/mnt/configs/agents.yaml \
                        "Docker Container Security"

# Enable human approval workflow for quality control
java -jar conductor.jar --enable-approval \
                        --config=/opt/config/production.config \
                        "Enterprise Software Architecture"
```

**Available CLI Options**
- `--config=FILE`: Path to configuration properties file
- `--workflow=FILE`: Path to workflow YAML definition (default: `yaml/workflows/book-creation.yaml`)
- `--agents=FILE`: Path to agents YAML configuration (default: `yaml/agents/book-agents.yaml`)
- `--context=FILE`: Path to context YAML file (default: `yaml/context/book-context.yaml`)
- `--enable-approval, --approval`: Enable human approval workflow with interactive console prompts
- `--help, -h`: Show help message with usage examples

#### Programmatic Usage (Unified Architecture)

```java
// NEW: Using unified architecture (recommended)
YamlWorkflowEngine adapter = new YamlWorkflowEngine(orchestrator, llmProvider);
adapter.

loadConfiguration(
    "yaml/workflows/book-creation.yaml",
            "yaml/agents/book-agents.yaml",
            "yaml/context/book-context.yaml"
);

Map<String, Object> context = Map.of("topic", "Your Book Topic");
WorkflowResult result = adapter.executeWorkflow(context);

if(result.

isSuccess()){
        System.out.

println("Workflow completed with "+result.getStageResults().

size() +" stages");
        }

// LEGACY: Using deprecated YamlWorkflowEngine (for backward compatibility)
        try(
YamlWorkflowEngine engine = new YamlWorkflowEngine()){
WorkflowExecutionResult result = engine
        .loadWorkflow("yaml/workflows/book-creation.yaml")
        .loadAgents("yaml/agents/book-agents.yaml")
        .loadContext("yaml/context/book-context.yaml")
        .withOrchestrator(orchestrator, memoryStore)
        .execute("Your Book Topic");

    if(result.

isSuccess()){
        System.out.

println("Workflow completed: "+result.getSummary());
        }
        }
```

## 🎯 Benefits Demonstrated

### No-Code Advantages
✅ **Rapid Iteration**: Modify workflows without code recompilation
✅ **Non-Technical Access**: Product managers can configure workflows
✅ **A/B Testing**: Easy testing of different agent configurations
✅ **Environment Customization**: Dev/staging/prod workflow variations
✅ **Template Reuse**: Shareable workflow and agent templates

### 🆕 Unified Architecture Benefits
✅ **Identical Behavior**: YAML and code-based workflows produce identical outputs (verified by file diff)
✅ **Shared Validation**: Both use same validation logic and retry patterns
✅ **Unified Maintenance**: Single codebase for core execution logic
✅ **Consistent Debugging**: Same error messages and logging patterns
✅ **Migration Safety**: Zero behavior changes when converting between approaches

### Comparison with Hardcoded Approach
| Aspect | Hardcoded Java | No-Code YAML |
|--------|----------------|--------------|
| **Modification Speed** | Slow (recompile + deploy) | Fast (edit config) |
| **Technical Barrier** | High (Java knowledge) | Low (YAML editing) |
| **Type Safety** | Compile-time | Runtime validation |
| **Debugging** | Full IDE support | Configuration tracing |
| **Performance** | Maximum | Slight overhead |
| **Flexibility** | Limited | Highly configurable |

## 🔄 Workflow Execution Flow

1. **Configuration Loading**
   - Parse YAML workflow definition
   - Load agent configurations and prompt templates
   - Apply variable substitution and validation

2. **Agent Creation**
   - Create agents dynamically from configurations
   - Cache agents for reuse across stages
   - Apply prompt templates and context variables

3. **Stage Execution**
   - Execute stages sequentially (or in parallel if configured)
   - Apply agent prompts with runtime variable substitution
   - Collect results and execution metrics

4. **Result Assembly**
   - Aggregate stage results and timing information
   - Generate execution summary and performance metrics
   - Provide detailed success/failure reporting

## 🛠️ Implementation Status

### ✅ Completed Features
- [x] YAML configuration parsing and validation
- [x] Variable substitution with environment support
- [x] Workflow execution engine with stage management
- [x] Agent factory with prompt template rendering
- [x] Comprehensive error handling and reporting
- [x] Maven execution profiles for demos
- [x] Side-by-side comparison framework
- [x] **CLI configuration options for external YAML files**
- [x] **Real LLM provider integration (OpenAI, Anthropic, Gemini, etc.)**
- [x] **Standalone JAR deployment support**
- [x] **Custom workflow, agent, and context file specification**
- [x] **Human approval workflow with interactive console interface**
- [x] **Stage-level approval configuration and timeout support**
- [x] **Approval feedback collection and result tracking**
- [x] **File output generation with structured content formatting**
- [x] **Automatic directory creation and file management**
- [x] **Variable substitution in file paths and content**
- [x] **Parallel stage execution with configurable wave-based processing**
- [x] **Thread pool management with timeout controls and error recovery**
- [x] **Stage dependency resolution and execution planning**

### 🔄 Known Limitations (Future Enhancements)
- [ ] Tool-based agents not yet supported
- [ ] Configuration hot-reloading not available
- [ ] Web-based approval interface
- [ ] Multi-reviewer approval workflows
- [ ] Approval delegation and escalation
- [ ] Multiple output format support (PDF, DOCX, HTML)
- [ ] Template-based content formatting
- [ ] Output file versioning and history

## 🎪 Demo Scenarios

### 1. No-Code Book Creation
Demonstrates complete book creation using only YAML configuration:
- Dynamic agent creation from configuration
- Template-based prompt generation
- Multi-stage workflow execution
- Result aggregation and reporting

### 2. Workflow Comparison
Side-by-side comparison of code-based vs YAML-based approaches:
- Same functional output from both approaches
- Performance and maintainability analysis
- Benefits and trade-offs demonstration

### 3. Configuration Flexibility
Shows the power of configuration-driven workflows:
- Easy agent reconfiguration
- Prompt template modification
- Variable substitution examples
- Environment-specific customization

### 4. External Configuration Support
Demonstrates deployment flexibility with CLI options:
- **Custom YAML Files**: Use external workflow, agent, and context configurations
- **Standalone JAR**: Full functionality in standalone deployments
- **Containerized Deployment**: Mount external configs in Docker/Kubernetes
- **Environment Separation**: Different configs for dev/staging/production
- **Version Control**: Track workflow changes independently of code

### 5. Human Approval Workflow
Shows quality control and oversight capabilities:
- **Interactive Console Approval**: Review generated content in real-time
- **Stage-level Control**: Configure approval requirements per workflow stage
- **Quality Gates**: Prevent poor content from proceeding to next stages
- **Feedback Collection**: Gather human insights for continuous improvement
- **Compliance Support**: Ensure content meets regulatory or brand standards

### 6. File Output Generation
Demonstrates automatic file creation and organization:
- **Structured Content Files**: Generate well-formatted markdown files with headers, metadata, and content sections
- **Dynamic File Naming**: Use variable substitution for timestamped and contextualized file names
- **Directory Management**: Automatic creation of organized directory structures
- **Content Preservation**: Capture generated content, reviews, and approval status in persistent files
- **Multi-Stage Organization**: Separate files for each workflow stage with consistent formatting

## 🚀 Deployment Benefits

### Standalone JAR Deployment
```bash
# Package with all dependencies
mvn clean package

# Deploy anywhere with Java runtime
java -jar target/conductor-1.0.0.jar --config=/opt/config/prod.config \
                                     --workflow=/opt/workflows/book-creation.yaml \
                                     "Technical Documentation"
```

### Container Deployment
```dockerfile
FROM openjdk:17-jre
COPY target/conductor-1.0.0.jar /app/conductor.jar
VOLUME ["/config", "/workflows"]
ENTRYPOINT ["java", "-jar", "/app/conductor.jar"]
CMD ["--config=/config/app.config", "--workflow=/workflows/default.yaml"]
```

### Configuration Management
- **GitOps**: Store YAML configs in separate repositories
- **Hot Updates**: Change workflows without rebuilding JAR
- **Multi-Tenant**: Different configs per customer/environment
- **A/B Testing**: Easy workflow variant testing

## 🎉 Conclusion

The no-code workflow system successfully demonstrates how complex AI workflows can be made configurable and accessible to non-technical users while maintaining the power and flexibility of the underlying Conductor framework. The implementation provides a solid foundation for future enhancements and production deployment.

**🔄 With the new unified architecture**, both YAML-based workflows and code-based workflows now use **identical underlying execution primitives**, ensuring 100% behavioral consistency. This means you can confidently choose between configuration-driven and code-driven approaches based on your team's needs, knowing both will produce identical results.

This system bridges the gap between programmatic flexibility and configuration simplicity, enabling rapid iteration and broader team participation in workflow design and optimization.

## 📚 Related Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)**: Detailed technical documentation about the unified workflow execution system
- **[README.md](README.md)**: General framework overview and getting started guide
- **[AGENTS.md](AGENTS.md)**: Agent architecture and implementation details
- **[DEMOS.md](DEMOS.md)**: Comprehensive demo applications and examples
- **[TECHNICAL_FEATURES.md](TECHNICAL_FEATURES.md)**: Advanced technical features and implementation details