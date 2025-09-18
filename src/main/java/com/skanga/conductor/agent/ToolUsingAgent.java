package com.skanga.conductor.agent;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.memory.MemoryStore;
import com.skanga.conductor.metrics.MetricsRegistry;
import com.skanga.conductor.metrics.TimerContext;
import com.skanga.conductor.orchestration.TaskInput;
import com.skanga.conductor.orchestration.TaskResult;
import com.skanga.conductor.tools.Tool;
import com.skanga.conductor.tools.ToolInput;
import com.skanga.conductor.tools.ToolRegistry;
import com.skanga.conductor.tools.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates an agent that invokes tools directly from Java logic (no LLM involvement).
 */
public class ToolUsingAgent implements SubAgent {

    private final String name;
    private final String description;
    private final ToolRegistry tools;
    private final MemoryStore memoryStore; // optional: persist tool outputs
    private final MetricsRegistry metricsRegistry;

    public ToolUsingAgent(String name, String description, ToolRegistry tools, MemoryStore memoryStore) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or empty");
        }
        if (tools == null) {
            throw new IllegalArgumentException("tools cannot be null");
        }
        if (memoryStore == null) {
            throw new IllegalArgumentException("memoryStore cannot be null");
        }
        this.name = name;
        this.description = description;
        this.tools = tools;
        this.memoryStore = memoryStore;
        this.metricsRegistry = MetricsRegistry.getInstance();
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
    public TaskResult execute(TaskInput input) throws ConductorException.ToolExecutionException {
        if (input == null || input.prompt() == null || input.prompt().isBlank()) {
            throw new IllegalArgumentException("input and prompt cannot be null or empty");
        }

        // Start timing execution
        try (TimerContext timer = metricsRegistry.startTimer(
                "agent.execution.duration",
                Map.of("agent", name, "type", "tool_using"))) {

            try {
                String prompt = input.prompt();
                List<String> stepOutputs = new ArrayList<>();

                // Example: if prompt contains "research:", call web_search
                if (prompt.toLowerCase().contains("research:")) {
                    String query = prompt.substring(prompt.indexOf("research:") + "research:".length()).trim();
                    Tool t = tools.get("web_search");
                    if (t != null) {
                        try {
                            ToolResult tr = t.run(new ToolInput(query, null));
                            stepOutputs.add("web_search => " + tr.output());
                            memoryStore.addMemory(name(), "web_search result: " + tr.output());
                        } catch (Exception e) {
                            throw new ConductorException.ToolExecutionException("Error executing tool: web_search", e);
                        }
                    }
                }

                // Example: if prompt contains "readfile:PATH", call file_read
                if (prompt.toLowerCase().contains("readfile:")) {
                    String rel = prompt.substring(prompt.indexOf("readfile:") + "readfile:".length()).trim().split("\\s+")[0];
                    Tool t = tools.get("file_read");
                    if (t != null) {
                        try {
                            ToolResult tr = t.run(new ToolInput(rel, null));
                            stepOutputs.add("file_read => " + tr.output());
                            memoryStore.addMemory(name(), "file_read result (len=" + tr.output().length() + ")");
                        } catch (Exception e) {
                            throw new ConductorException.ToolExecutionException("Error executing tool: file_read", e);
                        }
                    }
                }

                // Example: if prompt contains "run:", call code_runner
                if (prompt.toLowerCase().contains("run:")) {
                    String cmd = prompt.substring(prompt.indexOf("run:") + "run:".length()).trim();
                    Tool t = tools.get("code_runner");
                    if (t != null) {
                        try {
                            ToolResult tr = t.run(new ToolInput(cmd, null));
                            stepOutputs.add("code_runner => " + tr.output());
                            memoryStore.addMemory(name(), "code_runner output: " + tr.output().substring(0, Math.min(200, tr.output().length())));
                        } catch (Exception e) {
                            throw new ConductorException.ToolExecutionException("Error executing tool: code_runner", e);
                        }
                    }
                }

                // Example: if prompt contains "speak:", call audio_gen
                if (prompt.toLowerCase().contains("speak:")) {
                    String text = prompt.substring(prompt.indexOf("speak:") + "speak:".length()).trim();
                    Tool t = tools.get("audio_gen");
                    if (t != null) {
                        try {
                            ToolResult tr = t.run(new ToolInput(text, null));
                            stepOutputs.add("audio_gen => " + tr.output());
                            memoryStore.addMemory(name(), "audio generated at: " + tr.output());
                        } catch (Exception e) {
                            throw new ConductorException.ToolExecutionException("Error executing tool: audio_gen", e);
                        }
                    }
                }

                String out = String.join("\n\n", stepOutputs);
                timer.recordWithSuccess(true);
                return new TaskResult(out, true, null);

            } catch (Exception e) {
                timer.recordWithSuccess(false);
                metricsRegistry.recordError(name, e.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }
    }
}
