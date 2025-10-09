package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;

import java.util.EnumSet;
import java.util.Set;

/**
 * Runtime capability detection for LLM providers.
 * <p>
 * This utility class allows querying what capabilities a provider supports
 * without having to check instanceof for each capability interface. It's
 * particularly useful for:
 * </p>
 * <ul>
 * <li>UI/CLI tools that need to show available features</li>
 * <li>Workflow engines that adapt based on provider capabilities</li>
 * <li>Testing and debugging provider implementations</li>
 * <li>Documentation generation</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * LLMProvider provider = new OpenAiLLMProvider(...);
 *
 * // Check individual capabilities
 * if (ProviderCapabilities.supportsStreaming(provider)) {
 *     // Use streaming
 * }
 *
 * // Get all capabilities
 * Set<Capability> caps = ProviderCapabilities.getCapabilities(provider);
 * System.out.println("Provider supports: " + caps);
 *
 * // Check if provider has all required capabilities
 * if (ProviderCapabilities.hasAll(provider, Capability.STREAMING, Capability.VISION)) {
 *     // Provider supports both streaming and vision
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public class ProviderCapabilities {

    /**
     * Enumeration of all possible provider capabilities.
     */
    public enum Capability {
        /** Basic text generation (all providers have this) */
        GENERATION,

        /** Streaming response generation */
        STREAMING,

        /** Embedding/vector generation */
        EMBEDDING,

        /** Vision/image understanding */
        VISION
    }

    /**
     * Returns all capabilities supported by the provider.
     *
     * @param provider the LLM provider to check
     * @return set of supported capabilities
     */
    public static Set<Capability> getCapabilities(LLMProvider provider) {
        EnumSet<Capability> capabilities = EnumSet.of(Capability.GENERATION);

        if (provider instanceof StreamingLLMProvider) {
            capabilities.add(Capability.STREAMING);
        }
        if (provider instanceof EmbeddingLLMProvider) {
            capabilities.add(Capability.EMBEDDING);
        }
        if (provider instanceof VisionLLMProvider) {
            capabilities.add(Capability.VISION);
        }

        return capabilities;
    }

    /**
     * Checks if provider supports streaming.
     *
     * @param provider the provider to check
     * @return true if streaming is supported
     */
    public static boolean supportsStreaming(LLMProvider provider) {
        return provider instanceof StreamingLLMProvider;
    }

    /**
     * Checks if provider supports embeddings.
     *
     * @param provider the provider to check
     * @return true if embeddings are supported
     */
    public static boolean supportsEmbedding(LLMProvider provider) {
        return provider instanceof EmbeddingLLMProvider;
    }

    /**
     * Checks if provider supports vision.
     *
     * @param provider the provider to check
     * @return true if vision is supported
     */
    public static boolean supportsVision(LLMProvider provider) {
        return provider instanceof VisionLLMProvider;
    }

    /**
     * Checks if provider has all specified capabilities.
     *
     * @param provider the provider to check
     * @param required capabilities that must be present
     * @return true if all required capabilities are supported
     */
    public static boolean hasAll(LLMProvider provider, Capability... required) {
        Set<Capability> available = getCapabilities(provider);
        for (Capability cap : required) {
            if (!available.contains(cap)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if provider has any of the specified capabilities.
     *
     * @param provider the provider to check
     * @param capabilities capabilities to check for
     * @return true if at least one capability is supported
     */
    public static boolean hasAny(LLMProvider provider, Capability... capabilities) {
        Set<Capability> available = getCapabilities(provider);
        for (Capability cap : capabilities) {
            if (available.contains(cap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a human-readable description of provider capabilities.
     *
     * @param provider the provider to describe
     * @return formatted capability description
     */
    public static String describeCapabilities(LLMProvider provider) {
        Set<Capability> caps = getCapabilities(provider);
        StringBuilder sb = new StringBuilder();
        sb.append("Provider capabilities:");

        for (Capability cap : caps) {
            sb.append("\n  ✓ ").append(cap.name().toLowerCase());
            switch (cap) {
                case GENERATION -> sb.append(" - Basic text generation");
                case STREAMING -> sb.append(" - Streaming responses");
                case EMBEDDING -> sb.append(" - Vector embeddings");
                case VISION -> sb.append(" - Image understanding");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a capability-aware provider adapter.
     * <p>
     * This method wraps a provider with adapters that check capabilities
     * before attempting to use them, providing graceful fallbacks.
     * </p>
     *
     * @param provider the base provider
     * @return capability-aware wrapper
     */
    public static LLMProvider createCapabilityAwareProvider(LLMProvider provider) {
        return new CapabilityAwareProviderAdapter(provider);
    }

    /**
     * Adapter that provides safe capability access with fallbacks.
     */
    private static class CapabilityAwareProviderAdapter implements
            LLMProvider, StreamingLLMProvider, EmbeddingLLMProvider, VisionLLMProvider {

        private final LLMProvider delegate;

        CapabilityAwareProviderAdapter(LLMProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public String generate(String prompt) throws ConductorException.LLMProviderException {
            return delegate.generate(prompt);
        }

        @Override
        public String generateStreaming(String prompt, java.util.function.Consumer<String> tokenConsumer)
                throws ConductorException.LLMProviderException {
            if (delegate instanceof StreamingLLMProvider streaming) {
                return streaming.generateStreaming(prompt, tokenConsumer);
            }
            // Fallback: generate normally and call consumer once with full response
            String response = delegate.generate(prompt);
            tokenConsumer.accept(response);
            return response;
        }

        @Override
        public double[] generateEmbedding(String text) throws ConductorException.LLMProviderException {
            if (delegate instanceof EmbeddingLLMProvider embedding) {
                return embedding.generateEmbedding(text);
            }
            throw new UnsupportedOperationException(
                "Provider " + delegate.getClass().getSimpleName() + " does not support embeddings"
            );
        }

        @Override
        public java.util.List<double[]> generateEmbeddings(java.util.List<String> texts)
                throws ConductorException.LLMProviderException {
            if (delegate instanceof EmbeddingLLMProvider embedding) {
                return embedding.generateEmbeddings(texts);
            }
            throw new UnsupportedOperationException(
                "Provider " + delegate.getClass().getSimpleName() + " does not support embeddings"
            );
        }

        @Override
        public int getEmbeddingDimensions() {
            if (delegate instanceof EmbeddingLLMProvider embedding) {
                return embedding.getEmbeddingDimensions();
            }
            return 0;
        }

        @Override
        public String generateWithImage(String prompt, String imageUrl)
                throws ConductorException.LLMProviderException {
            if (delegate instanceof VisionLLMProvider vision) {
                return vision.generateWithImage(prompt, imageUrl);
            }
            throw new UnsupportedOperationException(
                "Provider " + delegate.getClass().getSimpleName() + " does not support vision"
            );
        }

        @Override
        public String generateWithImages(String prompt, java.util.List<String> imageUrls)
                throws ConductorException.LLMProviderException {
            if (delegate instanceof VisionLLMProvider vision) {
                return vision.generateWithImages(prompt, imageUrls);
            }
            throw new UnsupportedOperationException(
                "Provider " + delegate.getClass().getSimpleName() + " does not support vision"
            );
        }
    }
}
