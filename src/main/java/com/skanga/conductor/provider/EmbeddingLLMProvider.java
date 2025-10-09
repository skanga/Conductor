package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;

import java.util.List;

/**
 * Interface for LLM providers that support generating embeddings.
 * <p>
 * Embeddings are dense vector representations of text that capture semantic
 * meaning. They are useful for:
 * </p>
 * <ul>
 * <li>Semantic search and similarity matching</li>
 * <li>Clustering and classification</li>
 * <li>Recommendation systems</li>
 * <li>Retrieval-augmented generation (RAG)</li>
 * </ul>
 * <p>
 * Providers that support embeddings should implement this interface in addition
 * to the base {@link LLMProvider} interface.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * LLMProvider provider = new OpenAiLLMProvider(apiKey, "text-embedding-ada-002", endpoint);
 *
 * if (provider instanceof EmbeddingLLMProvider embeddingProvider) {
 *     // Generate embedding for a single text
 *     double[] embedding = embeddingProvider.generateEmbedding("Hello world");
 *     System.out.println("Embedding dimension: " + embedding.length);
 *
 *     // Generate embeddings for multiple texts
 *     List<double[]> embeddings = embeddingProvider.generateEmbeddings(
 *         List.of("Hello", "World", "AI")
 *     );
 * } else {
 *     System.out.println("Provider does not support embeddings");
 * }
 * }</pre>
 *
 * @since 1.1.0
 * @see LLMProvider
 */
public interface EmbeddingLLMProvider extends LLMProvider {

    /**
     * Generates an embedding vector for the given text.
     * <p>
     * The embedding is a dense vector of floating-point numbers that
     * represents the semantic meaning of the input text.
     * </p>
     *
     * @param text the input text to embed
     * @return embedding vector (dimensions depend on the model)
     * @throws ConductorException.LLMProviderException if embedding generation fails
     */
    double[] generateEmbedding(String text) throws ConductorException.LLMProviderException;

    /**
     * Generates embeddings for multiple texts in a single batch.
     * <p>
     * Batch processing is more efficient than generating embeddings one at a time.
     * The order of embeddings in the result matches the order of input texts.
     * </p>
     *
     * @param texts list of texts to embed
     * @return list of embedding vectors, one per input text
     * @throws ConductorException.LLMProviderException if embedding generation fails
     */
    List<double[]> generateEmbeddings(List<String> texts)
        throws ConductorException.LLMProviderException;

    /**
     * Returns the dimensionality of embeddings produced by this provider.
     * <p>
     * For example:
     * </p>
     * <ul>
     * <li>OpenAI text-embedding-ada-002: 1536 dimensions</li>
     * <li>Cohere embed-english-v3.0: 1024 dimensions</li>
     * <li>Sentence-transformers all-MiniLM-L6-v2: 384 dimensions</li>
     * </ul>
     *
     * @return the number of dimensions in embedding vectors
     */
    int getEmbeddingDimensions();

    /**
     * Calculates cosine similarity between two embedding vectors.
     * <p>
     * Cosine similarity ranges from -1 to 1:
     * </p>
     * <ul>
     * <li>1.0: Identical/very similar</li>
     * <li>0.0: Unrelated</li>
     * <li>-1.0: Opposite meaning (rare in practice)</li>
     * </ul>
     * <p>
     * Typical similarity thresholds:
     * </p>
     * <ul>
     * <li>&gt; 0.9: Very similar</li>
     * <li>0.7-0.9: Related</li>
     * <li>&lt; 0.7: Different topics</li>
     * </ul>
     *
     * @param embedding1 first embedding vector
     * @param embedding2 second embedding vector
     * @return cosine similarity score (-1 to 1)
     * @throws IllegalArgumentException if vectors have different dimensions
     */
    default double cosineSimilarity(double[] embedding1, double[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException(
                "Embeddings must have same dimensions: " +
                embedding1.length + " vs " + embedding2.length
            );
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.length; i++) {
            dotProduct += embedding1[i] * embedding2[i];
            norm1 += embedding1[i] * embedding1[i];
            norm2 += embedding2[i] * embedding2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Finds the most similar text from a list of candidates.
     *
     * @param queryText the query text
     * @param candidates list of candidate texts to compare against
     * @return index of the most similar candidate
     * @throws ConductorException.LLMProviderException if embedding generation fails
     */
    default int findMostSimilar(String queryText, List<String> candidates)
        throws ConductorException.LLMProviderException {
        double[] queryEmbedding = generateEmbedding(queryText);
        List<double[]> candidateEmbeddings = generateEmbeddings(candidates);

        int mostSimilarIndex = 0;
        double maxSimilarity = -1.0;

        for (int i = 0; i < candidateEmbeddings.size(); i++) {
            double similarity = cosineSimilarity(queryEmbedding, candidateEmbeddings.get(i));
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilarIndex = i;
            }
        }

        return mostSimilarIndex;
    }
}
