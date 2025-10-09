package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;
import com.skanga.conductor.provider.ProviderCapabilities.Capability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for provider capability interfaces and detection.
 */
class ProviderCapabilitiesTest {

    @Nested
    @DisplayName("Capability Detection Tests")
    class CapabilityDetectionTests {

        @Test
        @DisplayName("Should detect basic generation capability")
        void shouldDetectBasicGeneration() {
            LLMProvider basic = new BasicProvider();

            Set<Capability> caps = ProviderCapabilities.getCapabilities(basic);

            assertTrue(caps.contains(Capability.GENERATION));
            assertEquals(1, caps.size());
        }

        @Test
        @DisplayName("Should detect streaming capability")
        void shouldDetectStreaming() {
            LLMProvider streaming = new StreamingProvider();

            assertTrue(ProviderCapabilities.supportsStreaming(streaming));
            Set<Capability> caps = ProviderCapabilities.getCapabilities(streaming);
            assertTrue(caps.contains(Capability.STREAMING));
        }

        @Test
        @DisplayName("Should detect embedding capability")
        void shouldDetectEmbedding() {
            LLMProvider embedding = new EmbeddingProvider();

            assertTrue(ProviderCapabilities.supportsEmbedding(embedding));
            Set<Capability> caps = ProviderCapabilities.getCapabilities(embedding);
            assertTrue(caps.contains(Capability.EMBEDDING));
        }

        @Test
        @DisplayName("Should detect vision capability")
        void shouldDetectVision() {
            LLMProvider vision = new VisionProvider();

            assertTrue(ProviderCapabilities.supportsVision(vision));
            Set<Capability> caps = ProviderCapabilities.getCapabilities(vision);
            assertTrue(caps.contains(Capability.VISION));
        }

        @Test
        @DisplayName("Should detect all capabilities for full-featured provider")
        void shouldDetectAllCapabilities() {
            LLMProvider fullFeatured = new FullFeaturedProvider();

            Set<Capability> caps = ProviderCapabilities.getCapabilities(fullFeatured);

            assertEquals(4, caps.size());
            assertTrue(caps.contains(Capability.GENERATION));
            assertTrue(caps.contains(Capability.STREAMING));
            assertTrue(caps.contains(Capability.EMBEDDING));
            assertTrue(caps.contains(Capability.VISION));
        }
    }

    @Nested
    @DisplayName("Capability Query Tests")
    class CapabilityQueryTests {

        @Test
        @DisplayName("Should check if provider has all required capabilities")
        void shouldCheckHasAll() {
            LLMProvider full = new FullFeaturedProvider();
            LLMProvider basic = new BasicProvider();

            assertTrue(ProviderCapabilities.hasAll(full,
                Capability.GENERATION, Capability.STREAMING));
            assertTrue(ProviderCapabilities.hasAll(full,
                Capability.EMBEDDING, Capability.VISION));

            assertFalse(ProviderCapabilities.hasAll(basic,
                Capability.STREAMING));
        }

        @Test
        @DisplayName("Should check if provider has any of specified capabilities")
        void shouldCheckHasAny() {
            LLMProvider streaming = new StreamingProvider();

            assertTrue(ProviderCapabilities.hasAny(streaming,
                Capability.STREAMING, Capability.EMBEDDING));
            assertFalse(ProviderCapabilities.hasAny(streaming,
                Capability.EMBEDDING, Capability.VISION));
        }
    }

    @Nested
    @DisplayName("Capability Description Tests")
    class CapabilityDescriptionTests {

        @Test
        @DisplayName("Should provide readable capability description")
        void shouldDescribeCapabilities() {
            LLMProvider full = new FullFeaturedProvider();

            String description = ProviderCapabilities.describeCapabilities(full);

            assertNotNull(description);
            assertTrue(description.contains("generation"));
            assertTrue(description.contains("streaming"));
            assertTrue(description.contains("embedding"));
            assertTrue(description.contains("vision"));
        }
    }

    @Nested
    @DisplayName("Streaming Provider Tests")
    class StreamingProviderTests {

        @Test
        @DisplayName("Should stream tokens to consumer")
        void shouldStreamTokens() throws ConductorException.LLMProviderException {
            StreamingLLMProvider provider = new StreamingProvider();
            StringBuilder received = new StringBuilder();

            String result = provider.generateStreaming(
                "test",
                token -> received.append(token)
            );

            assertEquals("Token1Token2Token3", result);
            assertEquals("Token1Token2Token3", received.toString());
        }
    }

    @Nested
    @DisplayName("Embedding Provider Tests")
    class EmbeddingProviderTests {

        @Test
        @DisplayName("Should generate embedding vector")
        void shouldGenerateEmbedding() throws ConductorException.LLMProviderException {
            EmbeddingLLMProvider provider = new EmbeddingProvider();

            double[] embedding = provider.generateEmbedding("test");

            assertNotNull(embedding);
            assertEquals(384, embedding.length); // Test dimension
        }

        @Test
        @DisplayName("Should generate embeddings for multiple texts")
        void shouldGenerateBatchEmbeddings() throws ConductorException.LLMProviderException {
            EmbeddingLLMProvider provider = new EmbeddingProvider();

            List<double[]> embeddings = provider.generateEmbeddings(
                List.of("text1", "text2", "text3")
            );

            assertEquals(3, embeddings.size());
            for (double[] emb : embeddings) {
                assertEquals(384, emb.length);
            }
        }

        @Test
        @DisplayName("Should calculate cosine similarity")
        void shouldCalculateCosineSimilarity() throws ConductorException.LLMProviderException {
            EmbeddingLLMProvider provider = new EmbeddingProvider();

            double[] emb1 = provider.generateEmbedding("hello");
            double[] emb2 = provider.generateEmbedding("hello");  // Same text
            double[] emb3 = provider.generateEmbedding("different");

            double sameSimilarity = provider.cosineSimilarity(emb1, emb2);
            double diffSimilarity = provider.cosineSimilarity(emb1, emb3);

            assertTrue(sameSimilarity > 0.99);  // Very similar
            assertTrue(diffSimilarity < sameSimilarity);  // Different is less similar
        }

        @Test
        @DisplayName("Should find most similar text")
        void shouldFindMostSimilar() throws ConductorException.LLMProviderException {
            EmbeddingLLMProvider provider = new EmbeddingProvider();

            // Use identical text to ensure deterministic similarity
            List<String> candidates = List.of("hello", "world", "test", "example");
            int mostSimilar = provider.findMostSimilar("hello", candidates);

            // "hello" should match itself exactly (index 0)
            assertEquals(0, mostSimilar);
        }
    }

    @Nested
    @DisplayName("Vision Provider Tests")
    class VisionProviderTests {

        @Test
        @DisplayName("Should generate response for single image")
        void shouldGenerateWithImage() throws ConductorException.LLMProviderException {
            VisionLLMProvider provider = new VisionProvider();

            String result = provider.generateWithImage(
                "What's in this image?",
                "https://example.com/image.jpg"
            );

            assertTrue(result.contains("image"));
        }

        @Test
        @DisplayName("Should generate response for multiple images")
        void shouldGenerateWithMultipleImages() throws ConductorException.LLMProviderException {
            VisionLLMProvider provider = new VisionProvider();

            String result = provider.generateWithImages(
                "Compare these images",
                List.of("img1.jpg", "img2.jpg")
            );

            assertTrue(result.contains("images"));
        }

        @Test
        @DisplayName("Should report supported image formats")
        void shouldReportSupportedFormats() {
            VisionLLMProvider provider = new VisionProvider();

            List<String> formats = provider.getSupportedImageFormats();

            assertTrue(formats.contains("image/png"));
            assertTrue(formats.contains("image/jpeg"));
        }
    }

    // Test Provider Implementations

    private static class BasicProvider implements LLMProvider {
        @Override
        public String generate(String prompt) throws ConductorException.LLMProviderException {
            return "Basic response";
        }
    }

    private static class StreamingProvider implements StreamingLLMProvider {
        @Override
        public String generate(String prompt) throws ConductorException.LLMProviderException {
            return "Response";
        }

        @Override
        public String generateStreaming(String prompt, Consumer<String> tokenConsumer)
                throws ConductorException.LLMProviderException {
            tokenConsumer.accept("Token1");
            tokenConsumer.accept("Token2");
            tokenConsumer.accept("Token3");
            return "Token1Token2Token3";
        }
    }

    private static class EmbeddingProvider implements EmbeddingLLMProvider {
        @Override
        public String generate(String prompt) throws ConductorException.LLMProviderException {
            return "Response";
        }

        @Override
        public double[] generateEmbedding(String text) throws ConductorException.LLMProviderException {
            // Generate simple test embedding based on text hash
            double[] embedding = new double[384];
            int hash = text.hashCode();
            for (int i = 0; i < 384; i++) {
                embedding[i] = Math.sin(hash + i) * 0.1;
            }
            return embedding;
        }

        @Override
        public List<double[]> generateEmbeddings(List<String> texts)
                throws ConductorException.LLMProviderException {
            return texts.stream()
                .map(text -> {
                    try {
                        return generateEmbedding(text);
                    } catch (ConductorException.LLMProviderException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        }

        @Override
        public int getEmbeddingDimensions() {
            return 384;
        }
    }

    private static class VisionProvider implements VisionLLMProvider {
        @Override
        public String generate(String prompt) throws ConductorException.LLMProviderException {
            return "Response";
        }

        @Override
        public String generateWithImage(String prompt, String imageUrl)
                throws ConductorException.LLMProviderException {
            return "Vision response about image: " + imageUrl;
        }

        @Override
        public String generateWithImages(String prompt, List<String> imageUrls)
                throws ConductorException.LLMProviderException {
            return "Vision response about " + imageUrls.size() + " images";
        }
    }

    private static class FullFeaturedProvider implements
            StreamingLLMProvider, EmbeddingLLMProvider, VisionLLMProvider {

        @Override
        public String generate(String prompt) throws ConductorException.LLMProviderException {
            return "Full featured response";
        }

        @Override
        public String generateStreaming(String prompt, Consumer<String> tokenConsumer)
                throws ConductorException.LLMProviderException {
            return "Streaming response";
        }

        @Override
        public double[] generateEmbedding(String text) throws ConductorException.LLMProviderException {
            return new double[768];
        }

        @Override
        public List<double[]> generateEmbeddings(List<String> texts)
                throws ConductorException.LLMProviderException {
            return texts.stream().map(t -> new double[768]).toList();
        }

        @Override
        public int getEmbeddingDimensions() {
            return 768;
        }

        @Override
        public String generateWithImage(String prompt, String imageUrl)
                throws ConductorException.LLMProviderException {
            return "Vision response";
        }

        @Override
        public String generateWithImages(String prompt, List<String> imageUrls)
                throws ConductorException.LLMProviderException {
            return "Multi-vision response";
        }
    }
}
