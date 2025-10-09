package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;

import java.util.function.Consumer;

/**
 * Interface for LLM providers that support streaming responses.
 * <p>
 * Streaming allows receiving the LLM response incrementally as it's generated,
 * rather than waiting for the complete response. This is useful for:
 * </p>
 * <ul>
 * <li>Providing real-time feedback to users</li>
 * <li>Reducing perceived latency in interactive applications</li>
 * <li>Handling very long responses efficiently</li>
 * <li>Implementing token-by-token processing</li>
 * </ul>
 * <p>
 * Providers that support streaming should implement this interface in addition
 * to the base {@link LLMProvider} interface.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * LLMProvider provider = new OpenAiLLMProvider(apiKey, "gpt-4", endpoint);
 *
 * if (provider instanceof StreamingLLMProvider streamingProvider) {
 *     // Stream the response token by token
 *     streamingProvider.generateStreaming(
 *         "Write a story",
 *         token -> System.out.print(token)
 *     );
 * } else {
 *     // Fall back to non-streaming
 *     String response = provider.generate("Write a story");
 *     System.out.println(response);
 * }
 * }</pre>
 *
 * @since 1.1.0
 * @see LLMProvider
 */
public interface StreamingLLMProvider extends LLMProvider {

    /**
     * Generates a response to the prompt with streaming support.
     * <p>
     * The response is provided incrementally through the consumer callback
     * as tokens are generated. This method blocks until the entire response
     * is generated or an error occurs.
     * </p>
     *
     * @param prompt the input prompt
     * @param tokenConsumer callback invoked for each token/chunk as it's generated
     * @return the complete response (same as concatenating all tokens)
     * @throws ConductorException.LLMProviderException if the LLM call fails
     */
    String generateStreaming(String prompt, Consumer<String> tokenConsumer)
        throws ConductorException.LLMProviderException;

    /**
     * Generates a response with streaming and custom configuration.
     * <p>
     * This variant allows passing additional configuration options such as
     * temperature, max tokens, etc.
     * </p>
     *
     * @param prompt the input prompt
     * @param tokenConsumer callback invoked for each token/chunk
     * @param config streaming configuration options
     * @return the complete response
     * @throws ConductorException.LLMProviderException if the LLM call fails
     */
    default String generateStreaming(String prompt, Consumer<String> tokenConsumer,
                                     StreamingConfig config)
        throws ConductorException.LLMProviderException {
        // Default implementation ignores config and delegates to basic streaming
        return generateStreaming(prompt, tokenConsumer);
    }

    /**
     * Configuration options for streaming generation.
     */
    class StreamingConfig {
        private final Integer maxTokens;
        private final Double temperature;
        private final Integer chunkSize;
        private final Long timeoutMs;

        private StreamingConfig(Builder builder) {
            this.maxTokens = builder.maxTokens;
            this.temperature = builder.temperature;
            this.chunkSize = builder.chunkSize;
            this.timeoutMs = builder.timeoutMs;
        }

        public Integer getMaxTokens() { return maxTokens; }
        public Double getTemperature() { return temperature; }
        public Integer getChunkSize() { return chunkSize; }
        public Long getTimeoutMs() { return timeoutMs; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Integer maxTokens;
            private Double temperature;
            private Integer chunkSize;
            private Long timeoutMs;

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder temperature(double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder chunkSize(int chunkSize) {
                this.chunkSize = chunkSize;
                return this;
            }

            public Builder timeoutMs(long timeoutMs) {
                this.timeoutMs = timeoutMs;
                return this;
            }

            public StreamingConfig build() {
                return new StreamingConfig(this);
            }
        }
    }
}
