package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a collection of agent definitions and prompt templates loaded from YAML.
 * This is the root object for agent configuration files.
 */
public class AgentConfigCollection {

    private Map<String, AgentDefinition> agents;

    @JsonProperty("prompt_templates")
    private Map<String, PromptTemplate> promptTemplates;

    // Default constructor for Jackson
    public AgentConfigCollection() {}

    public Map<String, AgentDefinition> getAgents() {
        return agents;
    }

    public void setAgents(Map<String, AgentDefinition> agents) {
        this.agents = agents;
    }

    public Map<String, PromptTemplate> getPromptTemplates() {
        return promptTemplates;
    }

    public void setPromptTemplates(Map<String, PromptTemplate> promptTemplates) {
        this.promptTemplates = promptTemplates;
    }

    /**
     * Gets an agent definition by ID.
     *
     * @param agentId the unique identifier of the agent
     * @return Optional containing the agent definition, or empty if not found
     */
    public Optional<AgentDefinition> getAgent(String agentId) {
        if (agents == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.get(agentId));
    }

    /**
     * Gets a prompt template by ID.
     *
     * @param templateId the unique identifier of the prompt template
     * @return Optional containing the prompt template, or empty if not found
     */
    public Optional<PromptTemplate> getPromptTemplate(String templateId) {
        if (promptTemplates == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(promptTemplates.get(templateId));
    }

    /**
     * Validates that all agent definitions and prompt templates are valid.
     */
    public void validate() throws IllegalArgumentException {
        if (agents == null || agents.isEmpty()) {
            throw new IllegalArgumentException("At least one agent definition is required");
        }

        // Validate each agent
        for (Map.Entry<String, AgentDefinition> entry : agents.entrySet()) {
            String agentId = entry.getKey();
            AgentDefinition agent = entry.getValue();

            if (agentId == null || agentId.trim().isEmpty()) {
                throw new IllegalArgumentException("Agent ID cannot be empty");
            }

            try {
                agent.validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid agent '" + agentId + "': " + e.getMessage());
            }

            // Check that referenced prompt template exists
            String templateId = agent.getPromptTemplate();
            if (templateId != null && !templateId.isEmpty()) {
                if (promptTemplates == null || !promptTemplates.containsKey(templateId)) {
                    throw new IllegalArgumentException("Agent '" + agentId + "' references unknown prompt template: " + templateId);
                }
            }
        }

        // Validate each prompt template
        if (promptTemplates != null) {
            for (Map.Entry<String, PromptTemplate> entry : promptTemplates.entrySet()) {
                String templateId = entry.getKey();
                PromptTemplate template = entry.getValue();

                if (templateId == null || templateId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Prompt template ID cannot be empty");
                }

                try {
                    template.validate();
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid prompt template '" + templateId + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Represents a prompt template with system and user messages.
     */
    public static class PromptTemplate {
        private String system;
        private String user;
        private String assistant;

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getAssistant() {
            return assistant;
        }

        public void setAssistant(String assistant) {
            this.assistant = assistant;
        }

        /**
         * Validates that this prompt template has required content.
         */
        public void validate() throws IllegalArgumentException {
            if ((system == null || system.trim().isEmpty()) &&
                (user == null || user.trim().isEmpty())) {
                throw new IllegalArgumentException("Prompt template must have at least system or user content");
            }
        }

        /**
         * Checks if this template has a system message.
         */
        public boolean hasSystem() {
            return system != null && !system.trim().isEmpty();
        }

        /**
         * Checks if this template has a user message.
         */
        public boolean hasUser() {
            return user != null && !user.trim().isEmpty();
        }

        /**
         * Checks if this template has an assistant message.
         */
        public boolean hasAssistant() {
            return assistant != null && !assistant.trim().isEmpty();
        }
    }
}