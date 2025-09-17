package com.skanga.conductor.orchestration;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.provider.LLMProvider;

/**
 * Uses an LLMProvider to decompose a user request into an array of TaskDefinition JSON.
 * <p>
 * Expected LLM output format (JSON array):
 * [
 * { "name": "outline", "description": "Create chapter outline", "promptTemplate": "Given summary: {{summary}} produce an outline..." },
 * { "name": "writer",  "description": "Write chapter", "promptTemplate": "Write chapter based on: {{outline}} ..."}
 * ]
 * <p>
 * The prompt we send asks the model to reply ONLY with JSON.
 */
public class LLMPlanner {

    private final LLMProvider plannerLLM;
    private final Gson gson = new Gson();

    public LLMPlanner(LLMProvider plannerLLM) {
        if (plannerLLM == null) {
            throw new IllegalArgumentException("plannerLLM cannot be null");
        }
        this.plannerLLM = plannerLLM;
    }

    public TaskDefinition[] plan(String userRequest) throws ConductorException.PlannerException {
        if (userRequest == null || userRequest.isBlank()) {
            throw new IllegalArgumentException("userRequest cannot be null or empty");
        }
        String prompt = buildPrompt(userRequest);

        try {
            String llmOut = plannerLLM.generate(prompt);

            // Try to parse JSON from the LLM output — tolerant to surrounding text by extracting first '['..']' chunk
            String json = extractJsonArray(llmOut);
            try {
                TaskDefinition[] tasks = gson.fromJson(json, TaskDefinition[].class);
                if (tasks == null) throw new ConductorException.PlannerException("Planner returned null tasks");
                return tasks;
            } catch (JsonSyntaxException e) {
                throw new ConductorException.PlannerException("Failed to parse JSON from planner output. Raw output:\n" + llmOut, e);
            }
        } catch (ConductorException.LLMProviderException e) {
            throw new ConductorException.PlannerException("LLM planner failed", e);
        }
    }

    private String buildPrompt(String userRequest) {
        return """
                You are a planner. Decompose the user's request into a sequence of discrete tasks that can be executed by specialized subagents.
                Respond ONLY with a JSON array of objects. Each object must have:
                  - name: short task name (no spaces)
                  - description: one-sentence description of what the subagent should do
                  - promptTemplate: the template prompt that will be given to the subagent (use {{user_request}} as placeholder where appropriate)
                Example response:
                [
                  {"name":"outline","description":"Produce chapter outline","promptTemplate":"Create outline for: {{user_request}}"},
                  {"name":"writer","description":"Write chapter from outline","promptTemplate":"Write chapter using outline: {{outline}}"}
                ]
                
                User request:
                %s
                
                IMPORTANT: Output only the JSON array (no extra commentary).
                """.formatted(userRequest);
    }

    // Extract the first JSON array seen in the model output (naive but robust enough for many LLM outputs)
    private String extractJsonArray(String s) {
        if (s == null) return "[]";
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        // fallback: return the entire content (will likely fail parse)
        return s;
    }
}
