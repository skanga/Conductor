package com.skanga.conductor.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a book's table of contents with chapter extraction capabilities.
 * <p>
 * This record holds the json-formatted table of contents and
 * provides utilities to extract individual chapter specifications for
 * subsequent chapter generation.
 * </p>
 *
 * @param content the json-formatted table of contents
 * @since 1.0.0
 * @see BookCreationWorkflow
 * @see ChapterSpec
 */
public record TableOfContents(String content) {

    /**
     * Canonical constructor that cleans up JSON fences from the content.
     */
    public TableOfContents(String content) {
        this.content = cleanJsonFences(content);
    }

    /**
     * Removes JSON code fences (```json, ```, etc.) from the content to ensure clean JSON parsing.
     */
    private static String cleanJsonFences(String content) {
        if (content == null) return "";

        String cleaned = content.trim();

        // Remove opening fences like ```json, ```JSON, or just ```
        cleaned = cleaned.replaceFirst("^```(?:json|JSON)?\\s*", "");

        // Remove closing fences ```
        cleaned = cleaned.replaceFirst("\\s*```\\s*$", "");

        return cleaned.trim();
    }

    private static final Logger logger = LoggerFactory.getLogger(TableOfContents.class);
    private static final Gson gson = new Gson();

    // Thread-safe cache for chapter specifications
    private static final java.util.concurrent.ConcurrentHashMap<String, List<ChapterSpec>> chapterSpecCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * JSON structure for table of contents.
     */
    public static class TocJson {
        @SerializedName("chapters")
        public List<ChapterJson> chapters = new ArrayList<>();
    }

    /**
     * JSON structure for individual chapters.
     */
    public static class ChapterJson {
        @SerializedName("number")
        public int number;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("keyPoints")
        public List<String> keyPoints = new ArrayList<>();
    }

    /**
     * Extracts chapter specifications from the table of contents.
     * <p>
     * Parses the markdown content to identify chapter titles and descriptions,
     * creating ChapterSpec objects for each chapter found.
     * Results are cached for performance on subsequent calls.
     * </p>
     *
     * @return list of chapter specifications extracted from the TOC
     */
    public List<ChapterSpec> extractChapterSpecs() {
        // Use content hash as cache key for thread-safe caching
        String contentKey = String.valueOf(content.hashCode());
        return chapterSpecCache.computeIfAbsent(contentKey, k -> extractChapterSpecsInternal());
    }

    /**
     * Internal method that performs the actual chapter extraction.
     */
    private List<ChapterSpec> extractChapterSpecsInternal() {
        List<ChapterSpec> specs = new ArrayList<>();

        logger.info("=== CHAPTER EXTRACTION DIAGNOSTICS ===");
        logger.info("TOC content length: {} characters", content.length());
        logger.info("TOC content (first 500 chars): {}", content.substring(0, Math.min(500, content.length())));

        if (content.length() > 500) {
            logger.info("TOC content (last 200 chars): {}", content.substring(Math.max(0, content.length() - 200)));
        }

        // Priority 1: Try to parse as JSON
        logger.info("STEP 1: Attempting JSON parsing...");
        specs = extractChaptersFromJson();
        if (!specs.isEmpty()) {
            logger.info("✅ SUCCESS: Extracted {} chapters from JSON", specs.size());
            logExtractedChapters(specs, "JSON");
            return specs;
        }
        logger.info("❌ JSON parsing failed");

        // Priority 2: Look for JSON within the content (in case there's extra text)
        logger.info("STEP 2: Attempting embedded JSON extraction...");
        specs = extractJsonFromMixedContent();
        if (!specs.isEmpty()) {
            logger.info("✅ SUCCESS: Extracted {} chapters from embedded JSON", specs.size());
            logExtractedChapters(specs, "Embedded JSON");
            return specs;
        }
        logger.info("❌ Embedded JSON parsing failed");

        // Priority 3: Fallback to markdown parsing
        logger.info("STEP 3: Attempting markdown parsing...");
        specs = extractChaptersFromMarkdown();
        if (!specs.isEmpty()) {
            logger.info("✅ SUCCESS: Extracted {} chapters from markdown", specs.size());
            logExtractedChapters(specs, "Markdown");
            return specs;
        }
        logger.info("❌ Markdown parsing failed");

        // Priority 4: Try numbered list patterns
        logger.info("STEP 4: Attempting numbered list parsing...");
        specs = extractChaptersFromNumberedList();
        if (!specs.isEmpty()) {
            logger.info("✅ SUCCESS: Extracted {} chapters from numbered lists", specs.size());
            logExtractedChapters(specs, "Numbered Lists");
            return specs;
        }
        logger.info("❌ Numbered list parsing failed");

        // Priority 5: Aggressive line-by-line extraction
        logger.info("STEP 5: Attempting aggressive line-by-line extraction...");
        specs = extractChaptersAggressive();
        if (!specs.isEmpty()) {
            logger.info("✅ FALLBACK SUCCESS: Extracted {} chapters using aggressive extraction", specs.size());
            logExtractedChapters(specs, "Aggressive Extraction");
        } else {
            logger.error("❌ ALL PARSING METHODS FAILED - No chapters extracted!");
        }

        logger.info("=== END CHAPTER EXTRACTION DIAGNOSTICS ===");
        return specs;
    }

    /**
     * Logs the details of extracted chapters for diagnostics.
     */
    private void logExtractedChapters(List<ChapterSpec> specs, String method) {
        logger.info("Chapters extracted using {}: ", method);
        for (int i = 0; i < specs.size(); i++) {
            ChapterSpec spec = specs.get(i);
            String title = spec.title();
            String truncatedTitle = title.length() > 50 ? title.substring(0, 50) + "..." : title;
            logger.info("  {}. \"{}\"", i + 1, truncatedTitle);
        }
    }

    /**
     * Gets the number of chapters in the table of contents.
     * This method is optimized and uses cached results for performance.
     *
     * @return number of chapters
     */
    public int getChapterCount() {
        return extractChapterSpecs().size();
    }

    /**
     * Checks if the content appears to be a valid table of contents.
     *
     * @return true if the content contains recognizable chapter structures
     */
    public boolean isValidTableOfContents() {
        String lowerContent = content.toLowerCase();
        return lowerContent.contains("chapter") &&
               (lowerContent.contains("#") || lowerContent.contains("1.") || lowerContent.contains("- "));
    }

    /**
     * Returns a summary of the table of contents.
     *
     * @return brief summary including chapter count and first few chapter titles
     */
    public String getSummary() {
        List<ChapterSpec> specs = extractChapterSpecs();
        int count = specs.size();

        if (count == 0) {
            return "Table of contents with no identifiable chapters";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Table of contents with %d chapters:\n", count));

        int previewCount = Math.min(3, count);
        for (int i = 0; i < previewCount; i++) {
            summary.append(String.format("- %s\n", specs.get(i).title()));
        }

        if (count > previewCount) {
            summary.append(String.format("... and %d more chapters", count - previewCount));
        }

        return summary.toString().trim();
    }

    /**
     * Converts the table of contents to markdown format for display and saving.
     * This is useful when the original content is JSON and needs to be converted for the final book.
     *
     * @return markdown-formatted table of contents
     */
    public String toMarkdown() {
        return "# Table of Contents\n\n" + toMarkdownContent();
    }

    /**
     * Returns just the chapter content without the "Table of Contents" header.
     * Useful when the header is added elsewhere to avoid duplication.
     *
     * @return markdown-formatted chapter list without header
     */
    public String toMarkdownContent() {
        List<ChapterSpec> specs = extractChapterSpecs();
        if (specs.isEmpty()) {
            return content; // Return original content if no chapters could be extracted
        }

        StringBuilder markdown = new StringBuilder();

        for (int i = 0; i < specs.size(); i++) {
            ChapterSpec spec = specs.get(i);
            markdown.append(String.format("## Chapter %d: %s\n\n", i + 1, spec.title()));

            // Add description if available
            if (spec.spec() != null && !spec.spec().trim().isEmpty() &&
                !spec.spec().equals("Chapter content to be generated.")) {
                markdown.append(spec.spec()).append("\n\n");
            }
        }

        return markdown.toString();
    }

    /**
     * Checks if a title appears to be a valid chapter title.
     */
    private boolean isValidChapterTitle(String title) {
        String lowerTitle = title.toLowerCase().trim();

        // Skip only very obvious non-chapter headers - be more permissive
        return !lowerTitle.isEmpty() &&
               !lowerTitle.equals("table of contents") &&
               !lowerTitle.equals("contents") &&
               !lowerTitle.equals("toc") &&
               !lowerTitle.equals("index") &&
               !lowerTitle.equals("bibliography") &&
               !lowerTitle.equals("references") &&
               !lowerTitle.startsWith("review comments") &&
               !lowerTitle.startsWith("generated at") &&
               title.length() > 2 &&
               !title.matches("^[\\d\\s\\-:]+$"); // Skip lines that are just numbers/separators
    }

    /**
     * Extracts the description/content that follows a chapter title.
     */
    private String extractChapterDescription(String section) {
        String[] lines = section.split("\n");
        StringBuilder description = new StringBuilder();

        boolean foundTitle = false;
        for (String line : lines) {
            String trimmedLine = line.trim();

            if (!foundTitle && trimmedLine.matches("^#{1,3}\\s.*")) {
                foundTitle = true;
                continue;
            }

            if (foundTitle && !trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                if (description.length() > 0) {
                    description.append(" ");
                }
                description.append(trimmedLine);
            }
        }

        String result = description.toString().trim();
        return result.isEmpty() ? "Chapter content to be generated." : result;
    }

    /**
     * Try to parse the entire content as JSON with comprehensive error handling.
     */
    private List<ChapterSpec> extractChaptersFromJson() {
        String trimmedContent = content.trim();

        // Pre-validation checks
        if (trimmedContent.isEmpty()) {
            logger.warn("JSON extraction failed: Content is empty");
            return new ArrayList<>();
        }

        if (!trimmedContent.startsWith("{") || !trimmedContent.endsWith("}")) {
            logger.warn("JSON extraction failed: Content doesn't appear to be JSON (missing braces)");
            return new ArrayList<>();
        }

        try {
            TocJson tocJson = gson.fromJson(trimmedContent, TocJson.class);
            return validateAndExtractChapters(tocJson, "direct JSON parsing");
        } catch (JsonSyntaxException e) {
            logger.warn("JSON syntax error during direct parsing: {} | Content preview: {}",
                e.getMessage(),
                getContentPreview(trimmedContent));
            return tryJsonRecovery(trimmedContent, e);
        } catch (Exception e) {
            logger.error("Unexpected error during JSON parsing: {} | Content preview: {}",
                e.getMessage(),
                getContentPreview(trimmedContent));
        }
        return new ArrayList<>();
    }

    /**
     * Validates parsed JSON and extracts chapters with detailed error reporting.
     */
    private List<ChapterSpec> validateAndExtractChapters(TocJson tocJson, String parseMethod) {
        if (tocJson == null) {
            logger.warn("JSON parsing ({}): Parsed object is null", parseMethod);
            return new ArrayList<>();
        }

        if (tocJson.chapters == null) {
            logger.warn("JSON parsing ({}): 'chapters' field is null", parseMethod);
            return new ArrayList<>();
        }

        if (tocJson.chapters.isEmpty()) {
            logger.warn("JSON parsing ({}): 'chapters' array is empty", parseMethod);
            return new ArrayList<>();
        }

        List<ChapterSpec> specs = new ArrayList<>();
        int validChapters = 0;
        int invalidChapters = 0;

        for (int i = 0; i < tocJson.chapters.size(); i++) {
            ChapterJson chapter = tocJson.chapters.get(i);
            if (chapter == null) {
                logger.warn("JSON parsing ({}): Chapter at index {} is null", parseMethod, i);
                invalidChapters++;
                continue;
            }

            if (chapter.title == null || chapter.title.trim().isEmpty()) {
                logger.warn("JSON parsing ({}): Chapter at index {} has null/empty title", parseMethod, i);
                invalidChapters++;
                continue;
            }

            try {
                String description = buildChapterDescription(chapter);
                specs.add(new ChapterSpec(chapter.title.trim(), description));
                validChapters++;
                logger.debug("Successfully parsed chapter {}: '{}'", i + 1, chapter.title);
            } catch (Exception e) {
                logger.warn("JSON parsing ({}): Error building description for chapter '{}': {}",
                    parseMethod, chapter.title, e.getMessage());
                invalidChapters++;
            }
        }

        logger.info("JSON parsing ({}) completed: {} valid chapters, {} invalid chapters",
            parseMethod, validChapters, invalidChapters);

        return specs;
    }

    /**
     * Attempts to recover from JSON syntax errors using common fixes.
     */
    private List<ChapterSpec> tryJsonRecovery(String content, JsonSyntaxException originalError) {
        logger.info("Attempting JSON recovery from syntax error...");

        // Recovery attempt 1: Fix common trailing comma issues
        String fixedContent = content.replaceAll(",\\s*([}\\]])", "$1");
        if (!fixedContent.equals(content)) {
            logger.debug("Recovery attempt 1: Removing trailing commas");
            try {
                TocJson tocJson = gson.fromJson(fixedContent, TocJson.class);
                List<ChapterSpec> result = validateAndExtractChapters(tocJson, "JSON recovery (trailing commas)");
                if (!result.isEmpty()) {
                    logger.info("Successfully recovered from JSON error by fixing trailing commas");
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Recovery attempt 1 failed: {}", e.getMessage());
            }
        }

        // Recovery attempt 2: Fix unescaped quotes in strings
        String quoteFixes = content.replaceAll("(?<!\\\\)\"(?=.*\".*:)", "\\\\\"");
        if (!quoteFixes.equals(content)) {
            logger.debug("Recovery attempt 2: Escaping internal quotes");
            try {
                TocJson tocJson = gson.fromJson(quoteFixes, TocJson.class);
                List<ChapterSpec> result = validateAndExtractChapters(tocJson, "JSON recovery (quote escaping)");
                if (!result.isEmpty()) {
                    logger.info("Successfully recovered from JSON error by escaping quotes");
                    return result;
                }
            } catch (Exception e) {
                logger.debug("Recovery attempt 2 failed: {}", e.getMessage());
            }
        }

        logger.warn("All JSON recovery attempts failed. Original error: {}", originalError.getMessage());
        return new ArrayList<>();
    }

    /**
     * Gets a safe preview of content for logging.
     */
    private String getContentPreview(String content) {
        if (content == null) return "null";
        if (content.length() <= 100) return content;
        return content.substring(0, 100) + "... (truncated)";
    }

    /**
     * Extract JSON from mixed content with enhanced error handling.
     */
    private List<ChapterSpec> extractJsonFromMixedContent() {
        // Look for JSON object boundaries
        int jsonStart = content.indexOf('{');
        int jsonEnd = content.lastIndexOf('}');

        if (jsonStart < 0 || jsonEnd <= jsonStart) {
            logger.warn("Mixed content extraction failed: No valid JSON boundaries found");
            return new ArrayList<>();
        }

        String jsonPart = content.substring(jsonStart, jsonEnd + 1);
        logger.info("Extracted potential JSON from mixed content (length: {} chars): {}",
            jsonPart.length(),
            getContentPreview(jsonPart));

        // Validate extracted JSON structure
        if (jsonPart.trim().isEmpty()) {
            logger.warn("Mixed content extraction failed: Extracted JSON is empty");
            return new ArrayList<>();
        }

        try {
            TocJson tocJson = gson.fromJson(jsonPart, TocJson.class);
            return validateAndExtractChapters(tocJson, "mixed content JSON extraction");
        } catch (JsonSyntaxException e) {
            logger.warn("JSON syntax error in mixed content: {} | Extracted JSON preview: {}",
                e.getMessage(),
                getContentPreview(jsonPart));
            return tryJsonRecovery(jsonPart, e);
        } catch (Exception e) {
            logger.error("Unexpected error parsing extracted JSON: {} | JSON preview: {}",
                e.getMessage(),
                getContentPreview(jsonPart));
        }
        return new ArrayList<>();
    }

    /**
     * Build a comprehensive description from JSON chapter data with validation.
     */
    private String buildChapterDescription(ChapterJson chapter) {
        if (chapter == null) {
            throw new IllegalArgumentException("Chapter object cannot be null");
        }

        StringBuilder desc = new StringBuilder();

        // Handle description field
        if (chapter.description != null && !chapter.description.trim().isEmpty()) {
            String cleanDescription = sanitizeText(chapter.description.trim());
            desc.append(cleanDescription);
        } else {
            desc.append("Chapter content to be generated.");
        }

        // Handle key points field
        if (chapter.keyPoints != null && !chapter.keyPoints.isEmpty()) {
            desc.append("\n\nKey points to cover:\n");
            int validPoints = 0;
            for (String point : chapter.keyPoints) {
                if (point != null && !point.trim().isEmpty()) {
                    String cleanPoint = sanitizeText(point.trim());
                    if (!cleanPoint.isEmpty()) {
                        desc.append("- ").append(cleanPoint).append("\n");
                        validPoints++;
                    }
                }
            }

            if (validPoints == 0) {
                // Remove the "Key points to cover:" header if no valid points were found
                int headerStart = desc.lastIndexOf("\n\nKey points to cover:\n");
                if (headerStart > 0) {
                    desc.setLength(headerStart);
                }
            }
        }

        String result = desc.toString().trim();
        return result.isEmpty() ? "Chapter content to be generated." : result;
    }

    /**
     * Sanitizes text by removing dangerous characters and limiting length.
     */
    private String sanitizeText(String text) {
        if (text == null) return "";

        // Remove potentially problematic characters
        String sanitized = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                              .replaceAll("\\s+", " ")
                              .trim();

        // Limit length to prevent extremely long descriptions
        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 997) + "...";
            logger.debug("Truncated overly long text field to 1000 characters");
        }

        return sanitized;
    }

    /**
     * Extract chapters using markdown parsing (original logic).
     */
    private List<ChapterSpec> extractChaptersFromMarkdown() {
        List<ChapterSpec> specs = new ArrayList<>();

        Pattern markdownPattern = Pattern.compile(
            "^#{1,4}\\s*(?:Chapter\\s+\\d+\\s*[:.-]?\\s*)?(.+?)$",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
        );

        String[] sections = content.split("(?=^#{1,4}\\s)", Pattern.MULTILINE);
        logger.debug("Split TOC into {} sections using markdown pattern", sections.length);

        for (String section : sections) {
            if (section.trim().isEmpty()) continue;

            Matcher matcher = markdownPattern.matcher(section);
            if (matcher.find()) {
                String title = matcher.group(1).trim();
                logger.debug("Found potential markdown chapter title: '{}'", title);

                if (isValidChapterTitle(title)) {
                    String description = extractChapterDescription(section);
                    specs.add(new ChapterSpec(title, description));
                    logger.debug("Added markdown chapter: '{}'", title);
                }
            }
        }
        return specs;
    }

    /**
     * Extract chapters from numbered list patterns.
     */
    private List<ChapterSpec> extractChaptersFromNumberedList() {
        List<ChapterSpec> specs = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // Look for various numbered/bulleted list patterns
            if (trimmed.matches("^\\d+[\\.\\):]\\s+.+") ||           // 1. Title, 1) Title, 1: Title
                trimmed.matches("^Chapter\\s+\\d+[:.\\s]+.+") ||     // Chapter 1: Title
                trimmed.matches("^\\d+\\s*[-–—]\\s+.+")) {           // 1 - Title, 1 — Title

                String title = trimmed.replaceFirst("^(\\d+[\\.\\):]?\\s*|Chapter\\s+\\d+[:.\\s]*|\\d+\\s*[-–—]\\s*)", "").trim();
                if (isValidChapterTitle(title)) {
                    specs.add(new ChapterSpec(title, "Chapter content to be generated."));
                    logger.debug("Found numbered chapter: '{}'", title);
                }
            }
        }

        return specs;
    }

    /**
     * Simple fallback method to extract chapters when pattern matching fails.
     */
    private List<ChapterSpec> extractChaptersSimple() {
        List<ChapterSpec> specs = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // Look for numbered items or bullet points that might be chapters
            if (trimmed.matches("^\\d+\\.\\s+.+") ||
                trimmed.matches("^-\\s+.+") ||
                trimmed.matches("^\\*\\s+.+")) {

                String title = trimmed.replaceFirst("^[\\d\\-\\*\\.\\s]+", "").trim();
                if (isValidChapterTitle(title)) {
                    specs.add(new ChapterSpec(title, "Chapter content to be generated."));
                    logger.debug("Found simple list chapter: '{}'", title);
                }
            }
        }

        return specs;
    }

    /**
     * Aggressive extraction that looks for any potential chapter-like content.
     */
    private List<ChapterSpec> extractChaptersAggressive() {
        List<ChapterSpec> specs = new ArrayList<>();
        String[] lines = content.split("\n");

        logger.info("Aggressive extraction: analyzing {} lines", lines.length);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Skip empty lines and very short lines
            if (trimmed.length() < 10) {
                logger.info("Skipping line {} (too short): '{}'", i + 1, trimmed);
                continue;
            }

            // Skip lines that look like descriptions, metadata, or mock output
            if (trimmed.toLowerCase().contains("description:") ||
                trimmed.toLowerCase().contains("summary:") ||
                trimmed.toLowerCase().contains("overview:") ||
                trimmed.toLowerCase().contains("generated at") ||
                trimmed.toLowerCase().contains("mock output") ||
                trimmed.toLowerCase().contains("prompt length") ||
                trimmed.toLowerCase().contains("first 120 chars") ||
                trimmed.toLowerCase().contains("user input:") ||
                trimmed.toLowerCase().contains("system:") ||
                trimmed.startsWith("**") ||
                trimmed.startsWith("---") ||
                trimmed.startsWith("[") ||
                trimmed.contains("...")) {
                logger.info("Skipping line {} (metadata/mock): '{}'", i + 1, trimmed.length() > 60 ? trimmed.substring(0, 60) + "..." : trimmed);
                continue;
            }

            // Extract potential title - remove leading punctuation/numbers
            String title = trimmed.replaceFirst("^[\\d\\s\\-\\*\\.\\#:]+", "").trim();

            // Remove trailing punctuation if it exists
            title = title.replaceFirst("[\\s\\-:]+$", "");

            if (isValidChapterTitle(title) && title.length() > 10) {
                specs.add(new ChapterSpec(title, "Chapter content to be generated."));
                logger.info("Found aggressive extraction chapter {}: '{}'", specs.size(), title);

                // Limit aggressive extraction to prevent too many false positives
                if (specs.size() >= 8) {
                    logger.info("Limiting aggressive extraction to 8 chapters");
                    break;
                }
            } else {
                logger.debug("Rejecting line {} as chapter: '{}'", i + 1, title.length() > 40 ? title.substring(0, 40) + "..." : title);
            }
        }

        logger.info("Aggressive extraction completed: found {} potential chapters", specs.size());
        return specs;
    }
}