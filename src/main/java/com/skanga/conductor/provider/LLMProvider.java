package com.skanga.conductor.provider;

import com.skanga.conductor.agent.ConversationalAgent;
import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.execution.ExecutionInput;

/**
 * Interface for Large Language Model (LLM) providers in the Conductor framework.
 * <p>
 * LLM providers abstract the interaction with various language models, allowing
 * the framework to work with different providers (OpenAI, Anthropic, local models, etc.)
 * through a standard interface. This enables easy switching between providers and
 * supports mock implementations for testing.
 * </p>
 * <p>
 * Implementations should handle authentication, rate limiting, retries, and error
 * handling internally. They should be thread-safe for concurrent usage across
 * multiple agents.
 * </p>
 *
 * @since 1.0.0
 * @see OpenAiLLMProvider
 * @see DemoMockLLMProvider
 */
public interface LLMProvider {

    /**
     * Generates text synchronously for the given prompt.
     * <p>
     * This method sends the prompt to the underlying LLM service and returns
     * the generated response. The implementation should handle:
     * </p>
     * <ul>
     * <li>Authentication and authorization</li>
     * <li>Request formatting and API communication</li>
     * <li>Response parsing and validation</li>
     * <li>Error handling and retries</li>
     * <li>Rate limiting and quota management</li>
     * </ul>
     * <p>
     * The method blocks until the LLM responds or an error occurs.
     * For streaming or asynchronous operations, consider extending this interface
     * or creating additional methods.
     * </p>
     *
     * @param prompt the text prompt to send to the LLM, should not be null or empty
     * @return the generated text response from the LLM, never null
     * @throws Exception if the LLM request fails due to network issues, authentication errors,
     *                   quota exceeded, invalid prompt, or other service-related problems
     *
     * @see ConversationalAgent#execute(ExecutionInput)
     */
    String generate(String prompt) throws ConductorException.LLMProviderException;
}