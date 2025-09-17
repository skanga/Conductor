package com.skanga.conductor.orchestration;

/**
 * Represents a single planned task definition for sub-agent creation.
 * <p>
 * This class is used by the LLM planner to define tasks that should be
 * executed by dynamically created sub-agents. The planner returns a list
 * of these definitions in JSON format, which are then used to create
 * and configure appropriate sub-agents for task execution.
 * </p>
 * <p>
 * Each task definition contains the essential information needed to
 * create a functional sub-agent:
 * </p>
 * <ul>
 * <li>A unique name for agent identification</li>
 * <li>A description of the agent's purpose and capabilities</li>
 * <li>A prompt template for LLM interactions</li>
 * </ul>
 *
 * @since 1.0.0
 * @see LLMPlanner
 * @see PlannerOrchestrator
 */
public class TaskDefinition {
    /**
     * The unique name for the sub-agent to be created.
     * Used for agent identification and registry lookup.
     */
    public String name;

    /**
     * A human-readable description of the agent's purpose and capabilities.
     * Used for documentation and debugging purposes.
     */
    public String description;

    /**
     * The prompt template that will be used by the sub-agent for LLM interactions.
     * May contain placeholders that will be filled with runtime data.
     */
    public String promptTemplate;

    /**
     * Default constructor for JSON deserialization and programmatic construction.
     */
    public TaskDefinition() {
    }

    /**
     * Creates a new task definition with the specified parameters.
     *
     * @param name the unique name for the sub-agent
     * @param description the description of the agent's purpose
     * @param promptTemplate the template for LLM interactions
     */
    public TaskDefinition(String name, String description, String promptTemplate) {
        this.name = name;
        this.description = description;
        this.promptTemplate = promptTemplate;
    }

    @Override
    public String toString() {
        return "TaskDefinition{name='%s', description='%s'}".formatted(name, description);
    }
}
