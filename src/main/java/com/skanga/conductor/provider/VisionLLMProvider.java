package com.skanga.conductor.provider;

import com.skanga.conductor.exception.ConductorException;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Interface for LLM providers that support vision/image understanding.
 * <p>
 * Vision-capable models can analyze and understand images in addition to text.
 * Use cases include:
 * </p>
 * <ul>
 * <li>Image captioning and description</li>
 * <li>Visual question answering</li>
 * <li>OCR and document understanding</li>
 * <li>Object detection and recognition</li>
 * <li>Screenshot analysis and UI understanding</li>
 * </ul>
 * <p>
 * Providers that support vision should implement this interface in addition
 * to the base {@link LLMProvider} interface.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * LLMProvider provider = new OpenAiLLMProvider(apiKey, "gpt-4-vision", endpoint);
 *
 * if (provider instanceof VisionLLMProvider visionProvider) {
 *     // Analyze a single image
 *     String description = visionProvider.generateWithImage(
 *         "What's in this image?",
 *         "https://example.com/image.jpg"
 *     );
 *
 *     // Analyze from file
 *     String analysis = visionProvider.generateWithImageFile(
 *         "Describe this diagram",
 *         Path.of("diagram.png")
 *     );
 *
 *     // Analyze multiple images
 *     String comparison = visionProvider.generateWithImages(
 *         "Compare these two images",
 *         List.of("image1.jpg", "image2.jpg")
 *     );
 * }
 * }</pre>
 *
 * @since 1.1.0
 * @see LLMProvider
 */
public interface VisionLLMProvider extends LLMProvider {

    /**
     * Generates a response based on text prompt and a single image.
     *
     * @param prompt the text prompt/question about the image
     * @param imageUrl URL of the image to analyze (http://, https://, or data: URL)
     * @return the model's response describing or answering about the image
     * @throws ConductorException.LLMProviderException if the vision call fails
     */
    String generateWithImage(String prompt, String imageUrl)
        throws ConductorException.LLMProviderException;

    /**
     * Generates a response based on text prompt and multiple images.
     * <p>
     * This allows analyzing and comparing multiple images in a single request.
     * </p>
     *
     * @param prompt the text prompt/question about the images
     * @param imageUrls list of image URLs to analyze
     * @return the model's response about all the images
     * @throws ConductorException.LLMProviderException if the vision call fails
     */
    String generateWithImages(String prompt, List<String> imageUrls)
        throws ConductorException.LLMProviderException;

    /**
     * Generates a response for an image loaded from a file.
     * <p>
     * The image is read from the filesystem and encoded appropriately
     * for the provider (typically as base64).
     * </p>
     *
     * @param prompt the text prompt/question about the image
     * @param imagePath path to the image file
     * @return the model's response
     * @throws ConductorException.LLMProviderException if the vision call fails
     */
    default String generateWithImageFile(String prompt, Path imagePath)
        throws ConductorException.LLMProviderException {
        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imagePath);
            String mimeType = detectMimeType(imagePath);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
            return generateWithImage(prompt, dataUrl);
        } catch (java.io.IOException e) {
            throw new ConductorException.LLMProviderException(
                "Failed to read image file: " + imagePath, e
            );
        }
    }

    /**
     * Generates a response for an image provided as bytes.
     *
     * @param prompt the text prompt/question about the image
     * @param imageBytes the raw image bytes
     * @param mimeType the MIME type of the image (e.g., "image/png", "image/jpeg")
     * @return the model's response
     * @throws ConductorException.LLMProviderException if the vision call fails
     */
    default String generateWithImageBytes(String prompt, byte[] imageBytes, String mimeType)
        throws ConductorException.LLMProviderException {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;
        return generateWithImage(prompt, dataUrl);
    }

    /**
     * Returns the maximum image size supported by this provider (in bytes).
     * <p>
     * For example:
     * </p>
     * <ul>
     * <li>OpenAI GPT-4 Vision: 20 MB</li>
     * <li>Anthropic Claude 3: 5 MB per image</li>
     * <li>Google Gemini Pro Vision: 4 MB</li>
     * </ul>
     *
     * @return maximum image size in bytes, or -1 if unlimited
     */
    default long getMaxImageSizeBytes() {
        return 20 * 1024 * 1024; // Default: 20 MB
    }

    /**
     * Returns the supported image formats for this provider.
     *
     * @return list of supported MIME types (e.g., "image/png", "image/jpeg")
     */
    default List<String> getSupportedImageFormats() {
        return List.of("image/png", "image/jpeg", "image/gif", "image/webp");
    }

    /**
     * Detects MIME type from file extension.
     */
    private String detectMimeType(Path imagePath) {
        String fileName = imagePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".webp")) return "image/webp";
        if (fileName.endsWith(".bmp")) return "image/bmp";
        return "image/png"; // Default
    }

    /**
     * Configuration for vision generation requests.
     */
    class VisionConfig {
        private final Integer maxTokens;
        private final Double temperature;
        private final ImageDetail detail;

        /**
         * Image detail level for vision analysis.
         */
        public enum ImageDetail {
            /** Low detail - faster and cheaper, good for simple tasks */
            LOW,
            /** High detail - more accurate but slower and more expensive */
            HIGH,
            /** Auto - provider chooses based on image complexity */
            AUTO
        }

        private VisionConfig(Builder builder) {
            this.maxTokens = builder.maxTokens;
            this.temperature = builder.temperature;
            this.detail = builder.detail;
        }

        public Integer getMaxTokens() { return maxTokens; }
        public Double getTemperature() { return temperature; }
        public ImageDetail getDetail() { return detail; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Integer maxTokens;
            private Double temperature;
            private ImageDetail detail = ImageDetail.AUTO;

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder temperature(double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder detail(ImageDetail detail) {
                this.detail = detail;
                return this;
            }

            public VisionConfig build() {
                return new VisionConfig(this);
            }
        }
    }
}
