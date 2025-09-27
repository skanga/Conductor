package com.skanga.conductor.demo;

import com.skanga.conductor.provider.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

// Note: For advanced templating features like conditionals, loops, and filters,
// use com.skanga.conductor.workflow.templates.PromptTemplateEngine instead
// of manual string replacement methods.
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Consolidated utility methods for book creation demos.
 * Replaces multiple helper classes with simple static methods.
 */
public class BookCreationUtils {

    private static final Logger logger = LoggerFactory.getLogger(BookCreationUtils.class);
    private static final Scanner scanner = new Scanner(System.in);

    // File I/O utilities (replaces FileSystemService hierarchy)

    public static void writeFile(String filePath, String content) throws Exception {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        logger.debug("Written file: {}", filePath);
    }

    public static void createDirectories(String dirPath) throws Exception {
        Files.createDirectories(Paths.get(dirPath));
        logger.debug("Created directory: {}", dirPath);
    }

    // User input utilities (replaces UserInputService hierarchy)

    public static boolean getUserApproval(String stageName, String content) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROVAL REQUIRED: " + stageName.toUpperCase());
        System.out.println("=".repeat(60));
        System.out.println(content.length() > 500 ?
            content.substring(0, 500) + "..." : content);
        System.out.println("=".repeat(60));

        System.out.print("Approve this content? (y/n): ");
        try {
            if (System.in.available() > 0) {
                String response = scanner.nextLine().trim().toLowerCase();
                return response.equals("y") || response.equals("yes");
            }
        } catch (Exception e) {
            logger.debug("Input not available, auto-approving");
        }

        System.out.println("y [Auto-approved in non-interactive mode]");
        return true;
    }

    // Prompt generation utilities
    // Note: The generatePrompt() method has been removed in favor of using
    // PromptTemplateEngine for consistent templating across the application

    public static String generateTitlePrompt(String topic) {
        return String.format("""
            You are an expert book title generator and marketing specialist.
            Create compelling, marketable titles that capture attention and clearly communicate the book's value proposition.
            Generate exactly one title and one subtitle.
            Respond in the exact format: 'Title: [TITLE]\\nSubtitle: [SUBTITLE]'

            Generate a compelling title and subtitle for a book about: %s

            Requirements:
            - Title should be 3-8 words, memorable and impactful
            - Subtitle should explain the benefit/approach
            - Both should work together to convey expertise and value
            - Target audience: professionals and practitioners

            Respond with exactly this format:
            Title: [Your title here]
            Subtitle: [Your subtitle here]
            """, topic);
    }

    public static String generateTocPrompt(String topic, String titleInfo) {
        return String.format("""
            You are a professional book outlining specialist and content strategist.
            Create comprehensive, well-structured table of contents that logically progress from basic concepts to advanced applications.
            Focus on practical, actionable content that delivers real value to readers.

            Create a detailed table of contents for the book:
            %s
            Topic: %s

            Requirements:
            - 4-8 chapters with descriptive titles
            - Each chapter should have 3-5 detailed bullet points
            - Logical progression from fundamentals to advanced topics
            - Practical, actionable content focus
            - Avoid meta-analysis or recommendation chapters

            Use this exact JSON format:
            ```json
            {
              "chapters": [
                {
                  "title": "Chapter Title",
                  "description": "Brief description",
                  "key_points": ["Point 1", "Point 2", "Point 3"]
                }
              ]
            }
            ```
            """, titleInfo, topic);
    }

    public static String generateChapterPrompt(String topic, String titleInfo, String chapterTitle) {
        return String.format("""
            You are an expert technical writer and subject matter specialist.
            Write comprehensive, well-structured chapters that provide practical value and actionable insights.
            Use clear, professional language appropriate for practitioners and professionals.

            Write a comprehensive chapter for the book:
            %s
            Topic: %s
            Chapter to write: %s

            Requirements:
            - 800-1200 words minimum
            - Clear section headers and structure
            - Practical examples and actionable insights
            - Professional tone appropriate for practitioners
            - Include specific techniques, methods, or frameworks
            - Conclude with key takeaways

            Write in markdown format with proper headers (##, ###, etc.)
            """, titleInfo, topic, chapterTitle);
    }

    // Response validation utilities (replaces ResponseValidator class)

    public static boolean validateTitleResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        return response.contains("Title:") && response.contains("Subtitle:");
    }

    public static boolean validateTocResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }

        // Check for forbidden meta-analysis keywords
        String[] forbiddenKeywords = {"recommendation", "conclusion", "summary", "next steps"};
        String lowerResponse = response.toLowerCase();
        for (String keyword : forbiddenKeywords) {
            if (lowerResponse.contains(keyword)) {
                logger.warn("TOC contains forbidden keyword: {}", keyword);
                return false;
            }
        }

        return true;
    }

    public static boolean validateChapterResponse(String response, int minLength) {
        if (response == null) {
            return false;
        }

        if (response.length() < minLength) {
            logger.warn("Chapter too short: {} characters", response.length());
            return false;
        }

        return true;
    }

    // Enhanced LLM provider utilities (replaces EnhancedLLMWrapper class)

    public static LLMProvider createEnhancedProvider(LLMProvider baseProvider, String prefix) {
        // For demo purposes, just return the base provider
        // In a real implementation, this could add monitoring, caching, etc.
        logger.debug("Creating enhanced provider with prefix: {}", prefix);
        return baseProvider;
    }

    // Display utilities

    public static void displayHeader(String title) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(title);
        System.out.println("=".repeat(80));
    }

    public static void displaySubHeader(String title) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println(title);
        System.out.println("-".repeat(60));
    }

    public static String truncateOutput(String output, int maxLength) {
        if (output == null) return "null";
        if (output.length() <= maxLength) return output.replace("\n", " ");
        return output.substring(0, maxLength).replace("\n", " ") + "...";
    }

    // Command line argument utilities

    public static String extractTopic(String[] args) {
        if (args.length == 0) {
            return "Modern Software Architecture";
        }

        // Filter out mode flags and return remaining args as topic
        StringBuilder topic = new StringBuilder();
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                if (topic.length() > 0) topic.append(" ");
                topic.append(arg);
            }
        }

        return topic.length() > 0 ? topic.toString() : "Modern Software Architecture";
    }

    public static String extractMode(String[] args) {
        for (String arg : args) {
            if (arg.equals("--code")) return "code";
            if (arg.equals("--yaml")) return "yaml";
        }
        return "code"; // default mode
    }

    // Private constructor to prevent instantiation
    private BookCreationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
}