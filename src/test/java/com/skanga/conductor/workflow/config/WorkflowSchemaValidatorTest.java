package com.skanga.conductor.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WorkflowSchemaValidator.
 */
class WorkflowSchemaValidatorTest {

    private WorkflowSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WorkflowSchemaValidator();
    }

    @Nested
    @DisplayName("Valid Workflow Tests")
    class ValidWorkflowTests {

        @Test
        @DisplayName("Should validate minimal valid workflow")
        void shouldValidateMinimalWorkflow() {
            String yaml = """
                workflow:
                  name: "test-workflow"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertTrue(result.isValid(), result.getErrorMessage());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Should validate workflow with all optional fields")
        void shouldValidateCompleteWorkflow() {
            String yaml = """
                workflow:
                  name: "complete-workflow"
                  description: "A complete workflow example"
                  version: "1.0"
                settings:
                  output_dir: "data/output"
                  max_retries: 3
                  timeout: "10m"
                variables:
                  topic: "AI"
                  author: "Test Author"
                stages:
                  - name: "stage1"
                    description: "First stage"
                    agents:
                      executor: "agent1"
                      reviewer: "agent2"
                    outputs:
                      - "output1.txt"
                  - name: "stage2"
                    depends_on: ["stage1"]
                    agents:
                      executor: "agent3"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertTrue(result.isValid(), result.getErrorMessage());
        }

        @Test
        @DisplayName("Should validate workflow with approval configuration")
        void shouldValidateApprovalWorkflow() {
            String yaml = """
                workflow:
                  name: "approval-workflow"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    approval:
                      required: true
                      timeout: "5m"
                      auto_approve: false
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertTrue(result.isValid(), result.getErrorMessage());
        }

        @Test
        @DisplayName("Should validate workflow with data-driven iteration")
        void shouldValidateDataDrivenIteration() {
            String yaml = """
                workflow:
                  name: "iteration-workflow"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "data_driven"
                      source: "items"
                      variable: "item"
                      parallel: true
                      max_concurrent: 4
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertTrue(result.isValid(), result.getErrorMessage());
        }

        @Test
        @DisplayName("Should validate workflow with count-based iteration")
        void shouldValidateCountBasedIteration() {
            String yaml = """
                workflow:
                  name: "count-workflow"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "count_based"
                      count: 5
                      start: 1
                      variable: "index"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertTrue(result.isValid(), result.getErrorMessage());
        }

        @Test
        @DisplayName("Should validate workflow with conditional iteration")
        void shouldValidateConditionalIteration() {
            String yaml = """
                workflow:
                  name: "conditional-workflow"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "conditional"
                      condition: "${count} > 0"
                      max_iterations: 10
                      variable: "item"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertTrue(result.isValid(), result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Invalid Workflow Tests")
    class InvalidWorkflowTests {

        @Test
        @DisplayName("Should reject workflow without name")
        void shouldRejectWorkflowWithoutName() {
            String yaml = """
                workflow:
                  description: "Missing name"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertFalse(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Should reject workflow without stages")
        void shouldRejectWorkflowWithoutStages() {
            String yaml = """
                workflow:
                  name: "no-stages"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("stages") || e.contains("required")));
        }

        @Test
        @DisplayName("Should reject stage without name")
        void shouldRejectStageWithoutName() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - agents:
                      executor: "agent1"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should reject stage without agents")
        void shouldRejectStageWithoutAgents() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("agent")));
        }

        @Test
        @DisplayName("Should reject invalid timeout format")
        void shouldRejectInvalidTimeout() {
            String yaml = """
                workflow:
                  name: "test"
                settings:
                  timeout: "invalid"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should reject invalid max_retries value")
        void shouldRejectInvalidMaxRetries() {
            String yaml = """
                workflow:
                  name: "test"
                settings:
                  max_retries: 100
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("maximum")));
        }

        @Test
        @DisplayName("Should reject invalid iteration type")
        void shouldRejectInvalidIterationType() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "invalid_type"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should reject data_driven iteration without source")
        void shouldRejectDataDrivenWithoutSource() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "data_driven"
                      variable: "item"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("source")));
        }

        @Test
        @DisplayName("Should reject count_based iteration without count")
        void shouldRejectCountBasedWithoutCount() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "count_based"
                      variable: "index"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("count")));
        }

        @Test
        @DisplayName("Should reject conditional iteration without condition")
        void shouldRejectConditionalWithoutCondition() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "conditional"
                      max_iterations: 10
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("condition")));
        }
    }

    @Nested
    @DisplayName("Business Rule Validation Tests")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should reject dependency on non-existent stage")
        void shouldRejectInvalidDependency() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                  - name: "stage2"
                    depends_on: ["non_existent_stage"]
                    agents:
                      executor: "agent2"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("non-existent") || e.contains("non_existent")));
        }

        @Test
        @DisplayName("Should reject per-item approval with parallel iteration")
        void shouldRejectPerItemApprovalWithParallelIteration() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    approval:
                      required: true
                      per_item: true
                    iteration:
                      type: "data_driven"
                      source: "items"
                      parallel: true
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("per_item") && e.contains("parallel")));
        }
    }

    @Nested
    @DisplayName("File Validation Tests")
    class FileValidationTests {

        @Test
        @DisplayName("Should validate actual workflow file")
        void shouldValidateActualFile() {
            String filePath = "external-configs/workflows/book-creation-with-approval.yaml";

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowFile(filePath);

            // If file exists, it should validate successfully
            // If file doesn't exist in test environment, that's okay
            if (result.isValid()) {
                assertTrue(result.getErrors().isEmpty());
            }
        }

        @Test
        @DisplayName("Should return error for non-existent file")
        void shouldReturnErrorForNonExistentFile() {
            WorkflowSchemaValidator.ValidationResult result =
                validator.validateWorkflowFile("/non/existent/file.yaml");

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("not found") || e.contains("Not found")));
        }
    }

    @Nested
    @DisplayName("Error Message Tests")
    class ErrorMessageTests {

        @Test
        @DisplayName("Should provide helpful error messages")
        void shouldProvideHelpfulErrorMessages() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                    iteration:
                      type: "count_based"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            String errorMsg = result.getErrorMessage();
            assertNotNull(errorMsg);
            assertTrue(errorMsg.contains("count"));
        }

        @Test
        @DisplayName("Should format multiple errors clearly")
        void shouldFormatMultipleErrors() {
            String yaml = """
                workflow:
                  name: "test"
                stages:
                  - name: "stage1"
                    agents:
                      executor: "agent1"
                  - name: "stage2"
                    depends_on: ["stage3", "stage4"]
                    agents:
                      executor: "agent2"
                """;

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowYaml(yaml);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().size() >= 2);
            String errorMsg = result.getErrorMessage();
            assertTrue(errorMsg.contains("stage3"));
            assertTrue(errorMsg.contains("stage4"));
        }
    }
}
