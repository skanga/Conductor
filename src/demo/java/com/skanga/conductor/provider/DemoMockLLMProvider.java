package com.skanga.conductor.provider;

/**
 * Simple mock LLM provider for demo purposes.
 * <p>
 * This is a lightweight mock provider intended only for demo applications.
 * For comprehensive testing, use the MockLLMProvider in the test package.
 * </p>
 * <p>
 * This provider extends AbstractLLMProvider to benefit from:
 * </p>
 * <ul>
 * <li>Automatic retry logic and configuration</li>
 * <li>Consistent exception handling and classification</li>
 * <li>Logging and metrics integration</li>
 * <li>Application configuration integration</li>
 * </ul>
 *
 * @since 1.0.0
 * @see AbstractLLMProvider
 */
public class DemoMockLLMProvider extends AbstractLLMProvider {

    /**
     * Creates a demo mock LLM provider with the specified name.
     *
     * @param providerName the name to identify this provider instance
     */
    public DemoMockLLMProvider(String providerName) {
        super(generateProviderName(providerName), "mock-model");
    }

    /**
     * Generates a provider name, creating a unique one if the input is null/blank.
     */
    private static String generateProviderName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "llm-provider-" + java.util.UUID.randomUUID().toString();
        }
        return standardizeProviderName(name);
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "Mock response from " + getProviderName() + ": Please provide a valid prompt.";
        }

        // Generate contextually appropriate mock responses based on prompt content
        String lowerPrompt = prompt.toLowerCase();

        // Check for specific domain keywords and generate appropriate responses
        if (lowerPrompt.contains("book") || lowerPrompt.contains("chapter") || lowerPrompt.contains("artificial intelligence")) {
            return generateBookResponse(prompt);
        } else if (lowerPrompt.contains("code") || lowerPrompt.contains("python") || lowerPrompt.contains("java") || lowerPrompt.contains("function") || lowerPrompt.contains("program")) {
            return generateCodeResponse(prompt);
        } else if (lowerPrompt.contains("microservices") || lowerPrompt.contains("architecture") || lowerPrompt.contains("technical") || lowerPrompt.contains("system")) {
            return generateTechnicalResponse(prompt);
        } else if (lowerPrompt.contains("business") || lowerPrompt.contains("startup") || lowerPrompt.contains("plan") || lowerPrompt.contains("market")) {
            return generateBusinessResponse(prompt);
        } else if (lowerPrompt.contains("education") || lowerPrompt.contains("explain") || lowerPrompt.contains("student") || lowerPrompt.contains("quantum") || lowerPrompt.contains("physics")) {
            return generateEducationResponse(prompt);
        } else if (lowerPrompt.contains("creative") || lowerPrompt.contains("story") || lowerPrompt.contains("time travel") || lowerPrompt.contains("novel")) {
            return generateCreativeResponse(prompt);
        } else if (lowerPrompt.contains("analytics") || lowerPrompt.contains("analyze") || lowerPrompt.contains("data") || lowerPrompt.contains("sales") || lowerPrompt.contains("metrics")) {
            return generateAnalyticsResponse(prompt);
        } else {
            return generateGenericResponse(prompt);
        }
    }

    private String generateBookResponse(String prompt) {
        return """
               # Chapter: Understanding Artificial Intelligence

               ## Introduction
               Artificial intelligence represents one of the most transformative technologies of our time.
               This chapter explores the fundamental concepts and real-world applications of AI systems.

               ## Core Concepts
               AI systems are designed to perform tasks that typically require human intelligence,
               including learning, reasoning, and problem-solving. The field encompasses machine learning,
               neural networks, and deep learning methodologies.

               ## Applications
               Modern AI applications span across industries, from healthcare diagnostics to autonomous
               vehicles, demonstrating the versatile nature of these technologies.
               """;
    }

    private String generateCodeResponse(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("java")) {
            return """
                   public class SortArray {
                       public static int[] sort(int[] arr) {
                           if (arr == null || arr.length == 0) {
                               return new int[0];
                           }

                           Arrays.sort(arr);
                           return arr;
                       }
                   }

                   // Java code example with class definition
                   """;
        } else {
            return """
                   def sort_array(arr):
                       '''
                       Sorts an array using Python's built-in sorted function
                       '''
                       if not arr:
                           return []

                       return sorted(arr)

                   # Example usage - this is code that demonstrates the function:
                   numbers = [64, 34, 25, 12, 22, 11, 90]
                   sorted_numbers = sort_array(numbers)
                   print(f"Sorted array: {sorted_numbers}")
                   """;
        }
    }

    private String generateTechnicalResponse(String prompt) {
        return """
               # Microservices Architecture Design

               ## Overview
               Microservices architecture breaks down applications into smaller, independent services
               that communicate over well-defined APIs. This distributed system approach offers
               significant advantages for scalability and maintainability.

               ## Key Components
               - Service Discovery
               - Load Balancing
               - API Gateway
               - Circuit Breakers
               - Distributed Monitoring

               ## Best Practices
               Each microservice should have a single responsibility and be independently deployable.
               """;
    }

    private String generateBusinessResponse(String prompt) {
        return """
               # Business Plan for Tech Startup

               ## Executive Summary
               Our startup aims to revolutionize the market through innovative technology solutions.
               We identify a significant market opportunity and propose a comprehensive business model.

               ## Market Analysis
               The target market shows strong growth potential with increasing demand for our services.
               Competition analysis reveals opportunities for differentiation.

               ## Financial Projections
               Year 1: Break-even expected
               Year 2-3: Profitable growth phase
               Year 4-5: Market expansion and scaling
               """;
    }

    private String generateEducationResponse(String prompt) {
        return """
               # Understanding Quantum Physics

               ## Introduction for Students
               Quantum physics is the study of matter and energy at the smallest scales.
               Unlike classical physics, quantum mechanics reveals the strange behavior
               of particles at the atomic level.

               ## Key Principles
               - Wave-particle duality
               - Uncertainty principle
               - Quantum entanglement
               - Superposition

               ## Learning Approach
               Each student should start with basic concepts before advancing to complex theories.
               Visual demonstrations and analogies help make abstract concepts more concrete.
               This helps students learn more effectively.
               """;
    }

    private String generateCreativeResponse(String prompt) {
        return """
               # The Time Travel Adventure

               Once upon a time, Sarah stood before the gleaming time machine, her heart racing with anticipation.
               The year 2045 had been harsh to humanity, but she held the key to changing everything.

               ## Chapter 1: The Journey Begins
               With trembling hands, she entered the coordinates: October 15th, 1985.
               The machine hummed to life, reality bending around her as she traveled
               through the streams of time on this incredible adventure.

               ## The Character's Dilemma
               But every story of time travel carries a warning - change the past,
               and the future becomes uncertain. Sarah would soon learn this lesson
               in ways she never imagined during her travel through time.
               """;
    }

    private String generateAnalyticsResponse(String prompt) {
        return """
               # Sales Data Analysis Report

               ## Executive Summary
               Analysis of Q3 sales data reveals significant trends and actionable insights
               for strategic decision-making.

               ## Key Metrics
               - Total Revenue: 15% increase YoY
               - Customer Acquisition: 23% growth
               - Average Order Value: $342 (+8%)
               - Conversion Rate: 4.2% (+0.5%)

               ## Trend Analysis
               The data shows strong performance in digital channels with mobile sales
               leading growth. Regional analysis indicates expansion opportunities
               in the Southeast market.

               ## Recommendations
               1. Increase investment in mobile optimization
               2. Expand Southeast operations
               3. Focus on high-value customer segments
               """;
    }

    private String generateGenericResponse(String prompt) {
        return """
               Mock response: Here's an interesting fact - the human brain contains approximately
               86 billion neurons, each connecting to thousands of others, creating a network
               more complex than any computer system we've built. This fascinating neural network
               enables everything from basic motor functions to complex creative thinking.

               In a real implementation, this would be generated by an actual LLM that could
               provide much more detailed and contextually appropriate responses.
               """;
    }

}