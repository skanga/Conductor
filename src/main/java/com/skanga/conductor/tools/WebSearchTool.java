package com.skanga.conductor.tools;

import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web search tool that performs real web searches using multiple fallback strategies.
 * <p>
 * This tool attempts real web searches using the following approach:
 * </p>
 * <ol>
 * <li>Try DuckDuckGo Instant Answer API (free, no keys needed)</li>
 * <li>Fallback to DuckDuckGo HTML scraping</li>
 * <li>Final fallback to mock results if all else fails</li>
 * </ol>
 * <p>
 * The tool is designed to be resilient and will always return some result,
 * even if real web search fails.
 * </p>
 * <p>
 * Thread Safety: This class uses immutable HttpClient and is thread-safe.
 * </p>
 *
 * @since 2.0.0
 * @see Tool
 * @see ExecutionInput
 * @see ExecutionResult
 */
public class WebSearchTool implements Tool {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DUCKDUCKGO_API = "https://api.duckduckgo.com/";
    private static final String DUCKDUCKGO_SEARCH = "https://html.duckduckgo.com/html/";

    private static final Pattern RESULT_PATTERN = Pattern.compile(
        "<h2 class=\"result__title\">.*?<a.*?href=\"([^\"]*?)\".*?>(.*?)</a>.*?</h2>.*?" +
        "<a.*?class=\"result__snippet\".*?>(.*?)</a>",
        Pattern.DOTALL
    );

    /**
     * Returns the name identifier for this tool.
     *
     * @return "web_search" as the tool identifier
     */
    @Override
    public String toolName() {
        return "web_search";
    }

    /**
     * Returns a description of this tool's functionality.
     *
     * @return description explaining this tool performs real web searches
     */
    @Override
    public String toolDescription() {
        return "Performs real web search using DuckDuckGo API and fallback strategies";
    }

    /**
     * Executes real web search with multiple fallback strategies.
     * <p>
     * Attempts to perform real web search using:
     * 1. DuckDuckGo Instant Answer API
     * 2. DuckDuckGo HTML scraping
     * 3. Mock results as final fallback
     * </p>
     *
     * @param toolInput the tool input containing the search query
     * @return a ExecutionResult containing search results
     */
    @Override
    public ExecutionResult runTool(ExecutionInput toolInput) {
        // Validate input parameters
        ValidationResult inputValidation = validateInput(toolInput);
        if (!inputValidation.isValid()) {
            return new ExecutionResult(false, inputValidation.getErrorMessage(), null);
        }

        String query = toolInput.content().trim();

        // Try multiple search strategies with fallbacks
        try {
            // Strategy 1: DuckDuckGo Instant Answer API
            String instantResults = tryDuckDuckGoInstantAPI(query);
            if (instantResults != null && !instantResults.trim().isEmpty()) {
                return new ExecutionResult(true, instantResults, null);
            }

            // Strategy 2: DuckDuckGo HTML scraping
            String htmlResults = tryDuckDuckGoHTMLScraping(query);
            if (htmlResults != null && !htmlResults.trim().isEmpty()) {
                return new ExecutionResult(true, htmlResults, null);
            }

            // Strategy 3: Final fallback to mock results
            return new ExecutionResult(true, createMockResults(query), null);

        } catch (Exception e) {
            // If all strategies fail, return mock results with warning
            String mockResults = createMockResults(query);
            String resultWithWarning = "⚠️ Real web search failed, showing mock results:\n\n" + mockResults;
            return new ExecutionResult(true, resultWithWarning, null);
        }
    }

    /**
     * Attempts to get instant answers from DuckDuckGo API.
     *
     * @param query the search query
     * @return formatted results or null if no results
     */
    private String tryDuckDuckGoInstantAPI(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = DUCKDUCKGO_API + "?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Conductor-WebSearch/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = OBJECT_MAPPER.readTree(response.body());
                return formatDuckDuckGoInstantResults(json, query);
            }
        } catch (Exception e) {
            // Silently fail and try next strategy
        }
        return null;
    }

    /**
     * Attempts to scrape search results from DuckDuckGo HTML.
     *
     * @param query the search query
     * @return formatted results or null if scraping fails
     */
    private String tryDuckDuckGoHTMLScraping(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = DUCKDUCKGO_SEARCH + "?q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseDuckDuckGoHTML(response.body(), query);
            }
        } catch (Exception e) {
            // Silently fail and try next strategy
        }
        return null;
    }

    /**
     * Formats DuckDuckGo instant answer results.
     *
     * @param json the JSON response from DuckDuckGo API
     * @param query the original query
     * @return formatted search results
     */
    private String formatDuckDuckGoInstantResults(JsonNode json, String query) {
        StringBuilder results = new StringBuilder();
        results.append("🔍 Search results for: ").append(query).append("\n\n");

        // Check for instant answer
        JsonNode answer = json.get("Answer");
        if (answer != null && !answer.asText().isEmpty()) {
            results.append("📋 Instant Answer:\n");
            results.append(answer.asText()).append("\n\n");
        }

        // Check for abstract
        JsonNode abstractText = json.get("Abstract");
        if (abstractText != null && !abstractText.asText().isEmpty()) {
            results.append("📄 Summary:\n");
            results.append(abstractText.asText()).append("\n\n");

            JsonNode abstractURL = json.get("AbstractURL");
            if (abstractURL != null && !abstractURL.asText().isEmpty()) {
                results.append("🔗 Source: ").append(abstractURL.asText()).append("\n\n");
            }
        }

        // Check for definition
        JsonNode definition = json.get("Definition");
        if (definition != null && !definition.asText().isEmpty()) {
            results.append("📖 Definition:\n");
            results.append(definition.asText()).append("\n\n");
        }

        // Check for related topics
        JsonNode relatedTopics = json.get("RelatedTopics");
        if (relatedTopics != null && relatedTopics.isArray() && relatedTopics.size() > 0) {
            results.append("🔗 Related Topics:\n");
            for (int i = 0; i < Math.min(3, relatedTopics.size()); i++) {
                JsonNode topic = relatedTopics.get(i);
                JsonNode text = topic.get("Text");
                JsonNode firstURL = topic.get("FirstURL");

                if (text != null && !text.asText().isEmpty()) {
                    results.append((i + 1)).append(") ").append(text.asText());
                    if (firstURL != null && !firstURL.asText().isEmpty()) {
                        results.append("\n   🔗 ").append(firstURL.asText());
                    }
                    results.append("\n\n");
                }
            }
        }

        return results.length() > 50 ? results.toString() : null; // Return null if minimal content
    }

    /**
     * Parses DuckDuckGo HTML search results.
     *
     * @param html the HTML content from DuckDuckGo
     * @param query the original query
     * @return formatted search results
     */
    private String parseDuckDuckGoHTML(String html, String query) {
        StringBuilder results = new StringBuilder();
        results.append("🔍 Web search results for: ").append(query).append("\n\n");

        Matcher matcher = RESULT_PATTERN.matcher(html);
        int count = 0;

        while (matcher.find() && count < 5) {
            String url = matcher.group(1);
            String title = cleanHtml(matcher.group(2));
            String snippet = cleanHtml(matcher.group(3));

            count++;
            results.append(count).append(") ").append(title).append("\n");
            results.append("   ").append(snippet).append("\n");
            results.append("   🔗 ").append(url).append("\n\n");
        }

        return count > 0 ? results.toString() : null;
    }

    /**
     * Cleans HTML tags and entities from text.
     *
     * @param html the HTML text to clean
     * @return clean text
     */
    private String cleanHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&quot;", "\"")
                   .replaceAll("&#39;", "'")
                   .trim();
    }

    /**
     * Creates mock search results as final fallback.
     *
     * @param query the search query
     * @return mock search results
     */
    private String createMockResults(String query) {
        return "🔍 Search results for: " + query + "\n\n" +
               "1) Example result about '" + query + "'\n" +
               "   This is a mock result for demonstration purposes.\n" +
               "   🔗 https://example.com/result1\n\n" +
               "2) More information on '" + query + "'\n" +
               "   Another mock result with relevant information.\n" +
               "   🔗 https://example.com/result2\n\n";
    }

    /**
     * Validates the tool input for completeness and security.
     *
     * @param input the tool input to validate
     * @return validation result indicating success or specific error
     */
    private ValidationResult validateInput(ExecutionInput input) {
        if (input == null) {
            return ValidationResult.invalid("Tool input cannot be null");
        }

        if (input.content() == null) {
            return ValidationResult.invalid("Search query cannot be null");
        }

        String query = input.content().trim();
        if (query.isEmpty()) {
            return ValidationResult.invalid("Search query cannot be empty");
        }

        // Check for extremely long queries that could cause issues
        if (query.length() > 1000) {
            return ValidationResult.invalid("Search query is too long (max 1000 characters)");
        }

        // Check for control characters that might cause formatting issues
        for (char c : query.toCharArray()) {
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return ValidationResult.invalid("Search query contains invalid control characters");
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Result class for validation operations.
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
