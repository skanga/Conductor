package com.skanga.conductor.agent;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.metrics.TimerContext;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.provider.LLMProvider;
import com.skanga.conductor.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LLM-powered sub-agent implementation with persistent memory.
 * <p>
 * This class implements a sub-agent that uses a Large Language Model (LLM)
 * to process tasks and maintain conversational memory. Key features include:
 * </p>
 * <ul>
 * <li>Thread-safe memory management using {@link CopyOnWriteArrayList}</li>
 * <li>Persistent memory storage via {@link MemoryStore}</li>
 * <li>Automatic memory rehydration on agent creation</li>
 * <li>Configurable memory limits to prevent unbounded growth</li>
 * <li>Read/write locking for memory consistency</li>
 * </ul>
 * <p>
 * The agent maintains both in-memory and persistent storage of conversation
 * history, allowing for stateful interactions across multiple executions.
 * Memory is automatically persisted to the database and can be retrieved
 * for debugging or analysis purposes.
 * </p>
 * <p>
 * Thread Safety: This class is thread-safe for concurrent execution.
 * Multiple threads can safely execute tasks and access memory simultaneously.
 * </p>
 *
 * @since 1.0.0
 * @see SubAgent
 * @see LLMProvider
 * @see MemoryStore
 */
public class LLMSubAgent implements SubAgent {

    private static final Logger logger = LoggerFactory.getLogger(LLMSubAgent.class);

    private final String name;
    private final String description;
    private final LLMProvider llm;
    private final String promptTemplate;
    private final List<String> memory = new CopyOnWriteArrayList<>();
    private final MemoryStore memoryStore;
    private final ReadWriteLock memoryLock = new ReentrantReadWriteLock();
    private final MetricsRegistry metricsRegistry;

    /**
     * Creates a new LLM sub-agent with persistent memory.
     * <p>
     * The agent is automatically rehydrated from the memory store if previous
     * conversations exist. Memory is loaded up to the configured limit and
     * becomes immediately available for context in subsequent interactions.
     * </p>
     *
     * @param name the unique identifier for this agent, used as the database key
     * @param description a human-readable description of the agent's purpose
     * @param llm the LLM provider to use for text generation
     * @param promptTemplate the template for formatting prompts to the LLM
     * @param memoryStore the shared memory store for persistence across agents
     * @throws SQLException if database error occurs during memory rehydration
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public LLMSubAgent(String name, String description, LLMProvider llm, String promptTemplate, MemoryStore memoryStore) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or empty");
        }
        if (llm == null) {
            throw new IllegalArgumentException("llm cannot be null");
        }
        if (promptTemplate == null || promptTemplate.isBlank()) {
            throw new IllegalArgumentException("promptTemplate cannot be null or empty");
        }
        if (memoryStore == null) {
            throw new IllegalArgumentException("memoryStore cannot be null");
        }
        this.name = name;
        this.description = description;
        this.llm = llm;
        this.promptTemplate = promptTemplate;
        this.memoryStore = memoryStore;
        this.metricsRegistry = MetricsRegistry.getInstance();
        rehydrateMemory();
    }

    private void rehydrateMemory() throws SQLException {
        // load previous memory from disk (H2)
        List<String> persisted = memoryStore.loadMemory(name);
        memoryLock.writeLock().lock();
        try {
            memory.clear();
            memory.addAll(persisted);
        } finally {
            memoryLock.writeLock().unlock();
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public TaskResult execute(TaskInput input) throws ConductorException.LLMProviderException {
        if (input == null || input.prompt() == null || input.prompt().isBlank()) {
            throw new IllegalArgumentException("input and prompt cannot be null or empty");
        }

        // Start timing execution
        try (TimerContext timer = metricsRegistry.startTimer(
                "agent.execution.duration",
                java.util.Map.of("agent", name, "type", "llm"))) {

            boolean success = false;
            try {
                // build prompt with template + rehydrated memory
                StringBuilder sb = new StringBuilder();
                sb.append("System: ").append(description).append("\n\n");

                // Thread-safe memory access for reading
                memoryLock.readLock().lock();
                try {
                    if (!memory.isEmpty()) {
                        sb.append("Memory (most recent first):\n");
                        // show configurable amount of memory for prompt
                        ApplicationConfig.MemoryConfig memoryConfig = ApplicationConfig.getInstance().getMemoryConfig();
                        int limit = memoryConfig.getDefaultMemoryLimit();
                        int start = Math.max(0, memory.size() - limit);
                        for (int i = start; i < memory.size(); i++) {
                            sb.append("- ").append(memory.get(i)).append("\n");
                        }
                        sb.append("\n");
                    }
                } finally {
                    memoryLock.readLock().unlock();
                }

                sb.append("User Input:\n").append(input.prompt()).append("\n\n");
                sb.append("Prompt Template:\n").append(promptTemplate).append("\n\n");
                sb.append("Produce the best output now.\n");

                String fullPrompt = sb.toString();

                String out = llm.generate(fullPrompt);

                // persist to memory store and update local memory
                try {
                    memoryStore.addMemory(name, out);
                } catch (SQLException e) {
                    // Log and continue (we still keep memory in-memory)
                    logger.warn("Failed to persist memory for agent {}: {}", name, e.getMessage());
                }

                // Thread-safe memory access for writing
                memoryLock.writeLock().lock();
                try {
                    memory.add(out);
                } finally {
                    memoryLock.writeLock().unlock();
                }

                success = true;
                timer.recordWithSuccess(true);
                return new TaskResult(out, true, null);

            } catch (Exception e) {
                timer.recordWithSuccess(false);
                metricsRegistry.recordError(name, e.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }
    }

    /**
     * expose memory for inspection (not all entries returned if very large)
     */
    public List<String> getMemorySnapshot(int limit) {
        memoryLock.readLock().lock();
        try {
            int start = Math.max(0, memory.size() - limit);
            return new ArrayList<>(memory.subList(start, memory.size()));
        } finally {
            memoryLock.readLock().unlock();
        }
    }
}
