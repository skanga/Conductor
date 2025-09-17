package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;

import java.util.Map;

/**
 * Returns a deterministic JSON plan for simple demo purposes.
 * Use this when you want to run the planner locally without calling an external LLM.
 */
public class MockPlannerProvider implements LLMProvider {

    private final Map<String, String> plans;
    private static final String DEFAULT_PLAN = """
            [
              {
                "name":"outline",
                "description":"Create a chapter-by-chapter outline from the user's summary",
                "promptTemplate":"Produce a chapter-by-chapter outline for the following summary: {{user_request}}"
              },
              {
                "name":"writer",
                "description":"Write each chapter based on the outline (one chapter per call)",
                "promptTemplate":"Write a 400-600 word chapter using the following outline segment and the original request: {{user_request}}"
              },
              {
                "name":"editor",
                "description":"Edit/revise the chapter for clarity and grammar",
                "promptTemplate":"Edit the chapter text provided. Keep meaning, improve clarity and concision."
              }
            ]
            """;

    /**
     * Creates a mock planner provider with default plans.
     * <p>
     * This constructor creates a provider that will return the default JSON plan
     * for any request, making it suitable for simple demo scenarios.
     * </p>
     */
    public MockPlannerProvider() {
        this.plans = null;
    }

    /**
     * Creates a mock planner provider with custom plans.
     * <p>
     * This constructor allows providing custom plans mapped to specific prompts,
     * enabling more sophisticated testing scenarios where different inputs
     * should produce different planning outputs.
     * </p>
     *
     * @param plans a map of prompts to their corresponding JSON plan responses
     */
    public MockPlannerProvider(Map<String, String> plans) {
        this.plans = plans;
    }

    @Override
    public String generate(String prompt) throws ConductorException.LLMProviderException {
        if (plans != null && plans.containsKey(prompt)) {
            return plans.get(prompt);
        }
        return DEFAULT_PLAN;
    }
}
