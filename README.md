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

The framework provides a `SubAgent` interface and several implementations, including:

*   `LLMSubAgent`: A subagent that uses a Large Language Model (LLM) to perform its task.
*   `ToolUsingAgent`: A subagent that can use a variety of tools to interact with its environment.
*   `LLMToolAgent`: A subagent that uses an LLM to decide which tools to use to accomplish a task.

## Features

*   **Subagent Architecture**: A clean and powerful implementation of the subagent architecture.
*   **LLM-based Planning**: An `LLMPlanner` that can dynamically decompose user requests into a sequence of tasks.
*   **Explicit and Implicit Agents**: Support for both pre-defined and on-the-fly agents.
*   **Memory Persistence**: A `MemoryStore` that allows agents to maintain state across sessions.
*   **Tool Use**: A flexible system for both programmatic and LLM-assisted tool use.
*   **Extensible**: The framework is designed to be extensible, allowing you to create your own custom agents, tools, and providers.
*   **Java-based**: Built on Java 21 and `langchain4j`, providing a modern and robust foundation.

## Getting Started

### Prerequisites

*   Java 21 or higher
*   Apache Maven

### Building the Project

To build the project, run the following command from the root directory:

```bash
mvn clean install
```

### Running the Demos

The project includes several demo applications that showcase the framework's capabilities.

**1. Mock Demo (`DemoMock`)**

This demo demonstrates the core orchestration features, including the creation of a book writing workflow with implicit agents and the use of an explicit agent with memory persistence.

To run the mock demo:

```bash
mvn exec:java -Dexec.mainClass="com.skanga.conductor.demo.DemoMock"
```

**2. Tools Demo (`DemoTools`)**

This demo showcases the tool-using capabilities of the framework, with both programmatic and LLM-assisted tool use.

To run the tools demo:

```bash
mvn exec:java -Dexec.mainClass="com.skanga.conductor.demo.DemoTools"
```

## Project Structure

The project is organized into the following key packages:

*   `com.skanga.conductor.agent`: Contains the `SubAgent` interface and its implementations.
*   `com.skanga.conductor.orchestration`: Contains the `Orchestrator` and `LLMPlanner`.
*   `com.skanga.conductor.tools`: Contains the `Tool` interface and related classes for tool use.
*   `com.skanga.conductor.memory`: Contains the `MemoryStore` for agent state persistence.
*   `com.skanga.conductor.provider`: Contains the `LLMProvider` interface for integrating with different LLM providers.
*   `com.skanga.conductor.demo`: Contains the demo applications.

## Usage

### Creating an Orchestrator

```java
SubAgentRegistry registry = new SubAgentRegistry();
MemoryStore memoryStore = new H2MemoryStore("jdbc:h2:./data/subagentsdb");
Orchestrator orchestrator = new Orchestrator(registry, memoryStore);
```

### Creating an Explicit Agent

```java
LLMSubAgent explicitAgent = new LLMSubAgent(
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

## Configuration

The demo applications can be configured via the `demo.properties` file located in `src/main/resources`. You can also override these properties using environment variables.

## Dependencies

*   [langchain4j](https://github.com/langchain4j/langchain4j): A Java library for building applications with LLMs.
*   [H2 Database](https://www.h2database.com): An in-memory and persistent database.
*   [SLF4J](https://www.slf4j.org/) and [Logback](https://logback.qos.ch/): For logging.
*   [Gson](https://github.com/google/gson): For JSON serialization and deserialization.
*   [JUnit 5](https://junit.org/junit5/): For testing.

## Contributing

Contributions are welcome! Please feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
