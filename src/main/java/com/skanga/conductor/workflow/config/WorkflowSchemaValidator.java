package com.skanga.conductor.workflow.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates workflow YAML files against the JSON schema.
 * <p>
 * This validator performs comprehensive validation of workflow definitions
 * at load time, catching errors before execution. This prevents runtime
 * failures due to malformed or invalid workflow configurations.
 * </p>
 * <p>
 * Key validations:
 * </p>
 * <ul>
 * <li>Required fields presence (workflow name, stages, agents)</li>
 * <li>Field type correctness (integers, booleans, enums)</li>
 * <li>Value constraints (min/max values, patterns, ranges)</li>
 * <li>Structural correctness (dependencies, iteration configs)</li>
 * <li>Naming conventions (stage names, variable names)</li>
 * </ul>
 * <p>
 * Usage example:
 * </p>
 * <pre>{@code
 * WorkflowSchemaValidator validator = new WorkflowSchemaValidator();
 *
 * // Validate from file path
 * ValidationResult result = validator.validateWorkflowFile("workflows/my-workflow.yaml");
 * if (!result.isValid()) {
 *     for (String error : result.getErrors()) {
 *         System.err.println("Validation error: " + error);
 *     }
 * }
 *
 * // Validate from YAML string
 * ValidationResult result2 = validator.validateWorkflowYaml(yamlContent);
 * }</pre>
 *
 * @since 1.1.0
 */
public class WorkflowSchemaValidator {

    private static final String SCHEMA_PATH = "/schemas/workflow-schema.json";
    private final JsonSchema schema;
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;

    /**
     * Creates a new workflow schema validator.
     * <p>
     * Loads the JSON schema from classpath resources.
     * </p>
     *
     * @throws IllegalStateException if the schema file cannot be loaded
     */
    public WorkflowSchemaValidator() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
        this.schema = loadSchema();
    }

    /**
     * Validates a workflow file.
     *
     * @param filePath path to the workflow YAML file
     * @return validation result with any errors found
     */
    public ValidationResult validateWorkflowFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return ValidationResult.error("File not found: " + filePath);
            }

            String yamlContent = Files.readString(path);
            return validateWorkflowYaml(yamlContent);
        } catch (IOException e) {
            return ValidationResult.error("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Validates a workflow YAML string.
     *
     * @param yamlContent the YAML content to validate
     * @return validation result with any errors found
     */
    public ValidationResult validateWorkflowYaml(String yamlContent) {
        try {
            // Parse YAML to JSON node
            JsonNode yamlNode = yamlMapper.readTree(yamlContent);

            // Validate against schema
            Set<ValidationMessage> validationMessages = schema.validate(yamlNode);

            // Additional business logic validations
            List<String> businessErrors = validateBusinessRules(yamlNode);

            // Combine schema and business errors
            List<String> allErrors = new ArrayList<>();
            allErrors.addAll(validationMessages.stream()
                .map(ValidationMessage::getMessage)
                .collect(Collectors.toList()));
            allErrors.addAll(businessErrors);

            return new ValidationResult(allErrors.isEmpty(), allErrors);

        } catch (IOException e) {
            return ValidationResult.error("Failed to parse YAML: " + e.getMessage());
        }
    }

    /**
     * Validates business rules not covered by JSON schema.
     * <p>
     * These include:
     * </p>
     * <ul>
     * <li>Stage dependency cycles</li>
     * <li>Referenced stages exist</li>
     * <li>Per-item approval not compatible with parallel iteration</li>
     * <li>Iteration configuration consistency</li>
     * </ul>
     *
     * @param workflow the parsed workflow JSON node
     * @return list of business rule violations
     */
    private List<String> validateBusinessRules(JsonNode workflow) {
        List<String> errors = new ArrayList<>();

        JsonNode stages = workflow.get("stages");
        if (stages == null || !stages.isArray()) {
            return errors;  // Schema validation will catch this
        }

        // Collect all stage names
        List<String> stageNames = new ArrayList<>();
        for (JsonNode stage : stages) {
            JsonNode nameNode = stage.get("name");
            if (nameNode != null) {
                stageNames.add(nameNode.asText());
            }
        }

        // Validate each stage
        for (JsonNode stage : stages) {
            String stageName = stage.has("name") ? stage.get("name").asText() : "unknown";

            // Validate dependencies reference existing stages
            JsonNode dependsOn = stage.get("depends_on");
            if (dependsOn != null && dependsOn.isArray()) {
                for (JsonNode dep : dependsOn) {
                    String depName = dep.asText();
                    if (!stageNames.contains(depName)) {
                        errors.add(String.format(
                            "Stage '%s' depends on non-existent stage '%s'",
                            stageName, depName
                        ));
                    }
                }
            }

            // Validate per-item approval with parallel iteration
            JsonNode approval = stage.get("approval");
            JsonNode iteration = stage.get("iteration");
            if (approval != null && iteration != null) {
                JsonNode perItem = approval.get("per_item");
                JsonNode parallel = iteration.get("parallel");

                if (perItem != null && perItem.asBoolean() &&
                    parallel != null && parallel.asBoolean()) {
                    errors.add(String.format(
                        "Stage '%s': per_item approval is not compatible with parallel iteration",
                        stageName
                    ));
                }
            }

            // Validate iteration configuration completeness
            if (iteration != null) {
                JsonNode type = iteration.get("type");
                if (type != null) {
                    String iterType = type.asText();
                    switch (iterType) {
                        case "data_driven":
                            if (!iteration.has("source")) {
                                errors.add(String.format(
                                    "Stage '%s': data_driven iteration requires 'source' field",
                                    stageName
                                ));
                            }
                            break;
                        case "count_based":
                            if (!iteration.has("count")) {
                                errors.add(String.format(
                                    "Stage '%s': count_based iteration requires 'count' field",
                                    stageName
                                ));
                            }
                            break;
                        case "conditional":
                            if (!iteration.has("condition")) {
                                errors.add(String.format(
                                    "Stage '%s': conditional iteration requires 'condition' field",
                                    stageName
                                ));
                            }
                            if (!iteration.has("max_iterations")) {
                                errors.add(String.format(
                                    "Stage '%s': conditional iteration requires 'max_iterations' field",
                                    stageName
                                ));
                            }
                            break;
                    }
                }
            }

            // Validate agents are specified
            JsonNode agents = stage.get("agents");
            if (agents == null || agents.size() == 0) {
                errors.add(String.format(
                    "Stage '%s': at least one agent must be specified",
                    stageName
                ));
            }
        }

        // Check for circular dependencies
        List<String> cycles = detectDependencyCycles(stages);
        errors.addAll(cycles);

        return errors;
    }

    /**
     * Detects circular dependencies in stage dependency graph using topological sort.
     * <p>
     * This implementation uses Depth-First Search (DFS) with three colors:
     * - WHITE: unvisited node
     * - GRAY: currently being processed (in DFS stack)
     * - BLACK: fully processed
     * <p>
     * A cycle exists if we encounter a GRAY node during DFS traversal.
     *
     * @param stages the workflow stages
     * @return list of detected circular dependency errors
     */
    private List<String> detectDependencyCycles(JsonNode stages) {
        List<String> errors = new ArrayList<>();

        if (!stages.isArray()) {
            return errors;
        }

        // Build dependency graph: stage name -> list of dependencies
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, String> stageNames = new HashMap<>();  // Track actual stage names

        for (JsonNode stage : stages) {
            String stageName = stage.path("name").asText();
            if (stageName.isEmpty()) {
                continue;  // Skip stages without names (will be caught by schema validation)
            }

            stageNames.put(stageName, stageName);
            List<String> dependencies = new ArrayList<>();

            JsonNode dependsOn = stage.path("depends_on");
            if (dependsOn.isArray()) {
                for (JsonNode dep : dependsOn) {
                    String depName = dep.asText();
                    if (!depName.isEmpty()) {
                        dependencies.add(depName);
                    }
                }
            }

            graph.put(stageName, dependencies);
        }

        // Check for invalid dependencies (stages that depend on non-existent stages)
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            String stageName = entry.getKey();
            for (String dependency : entry.getValue()) {
                if (!stageNames.containsKey(dependency)) {
                    errors.add("Stage '" + stageName + "' depends on non-existent stage '" + dependency + "'");
                }
            }
        }

        // Detect cycles using DFS with three colors
        Map<String, NodeColor> colors = new HashMap<>();
        List<String> currentPath = new ArrayList<>();

        // Initialize all nodes as WHITE (unvisited)
        for (String stage : graph.keySet()) {
            colors.put(stage, NodeColor.WHITE);
        }

        // Run DFS from each unvisited node
        for (String stage : graph.keySet()) {
            if (colors.get(stage) == NodeColor.WHITE) {
                List<String> cycle = detectCycleDFS(stage, graph, colors, currentPath);
                if (cycle != null && !cycle.isEmpty()) {
                    errors.add("Circular dependency detected: " + String.join(" -> ", cycle));
                }
            }
        }

        return errors;
    }

    /**
     * DFS helper for cycle detection.
     *
     * @param node        current node
     * @param graph       dependency graph
     * @param colors      node colors
     * @param currentPath current DFS path
     * @return cycle path if found, null otherwise
     */
    private List<String> detectCycleDFS(String node, Map<String, List<String>> graph,
                                        Map<String, NodeColor> colors, List<String> currentPath) {
        // Mark current node as being processed
        colors.put(node, NodeColor.GRAY);
        currentPath.add(node);

        // Visit all dependencies
        List<String> dependencies = graph.getOrDefault(node, new ArrayList<>());
        for (String dependency : dependencies) {
            NodeColor depColor = colors.getOrDefault(dependency, NodeColor.WHITE);

            if (depColor == NodeColor.GRAY) {
                // Found a cycle - build the cycle path
                List<String> cycle = new ArrayList<>();
                boolean inCycle = false;
                for (String pathNode : currentPath) {
                    if (pathNode.equals(dependency)) {
                        inCycle = true;
                    }
                    if (inCycle) {
                        cycle.add(pathNode);
                    }
                }
                cycle.add(dependency);  // Close the cycle
                return cycle;
            } else if (depColor == NodeColor.WHITE) {
                // Recursively visit unvisited node
                List<String> cycle = detectCycleDFS(dependency, graph, colors, currentPath);
                if (cycle != null) {
                    return cycle;
                }
            }
            // If BLACK, node is already fully processed, skip
        }

        // Mark current node as fully processed
        colors.put(node, NodeColor.BLACK);
        currentPath.remove(currentPath.size() - 1);

        return null;  // No cycle found
    }

    /**
     * Node colors for cycle detection DFS algorithm.
     */
    private enum NodeColor {
        WHITE,  // Unvisited
        GRAY,   // Being processed (in DFS stack)
        BLACK   // Fully processed
    }

    /**
     * Loads the JSON schema from classpath resources.
     *
     * @return the loaded JSON schema
     * @throws IllegalStateException if schema cannot be loaded
     */
    private JsonSchema loadSchema() {
        try (InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_PATH)) {
            if (schemaStream == null) {
                throw new IllegalStateException("Schema file not found: " + SCHEMA_PATH);
            }

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonNode schemaNode = jsonMapper.readTree(schemaStream);
            return factory.getSchema(schemaNode);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load schema: " + e.getMessage(), e);
        }
    }

    /**
     * Result of a validation operation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }

        /**
         * Creates an error result with a single error message.
         */
        public static ValidationResult error(String message) {
            List<String> errors = new ArrayList<>();
            errors.add(message);
            return new ValidationResult(false, errors);
        }

        /**
         * Creates a successful validation result.
         */
        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        /**
         * Returns whether the validation passed.
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * Returns the list of validation errors.
         * Empty if validation passed.
         */
        public List<String> getErrors() {
            return errors;
        }

        /**
         * Returns a formatted error message with all errors.
         */
        public String getErrorMessage() {
            if (valid) {
                return "Validation successful";
            }
            return "Validation failed:\n" +
                errors.stream()
                    .map(e -> "  - " + e)
                    .collect(Collectors.joining("\n"));
        }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid (" + errors.size() + " errors)";
        }
    }
}
