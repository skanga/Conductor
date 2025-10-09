package com.skanga.conductor.workflow.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command-line tool for validating workflow YAML files.
 * <p>
 * This utility allows developers to validate their workflow configurations
 * before deployment, catching errors early in the development process.
 * </p>
 * <p>
 * Usage:
 * </p>
 * <pre>
 * # Validate a single workflow file
 * java -cp conductor.jar com.skanga.conductor.workflow.config.WorkflowValidator \
 *     path/to/workflow.yaml
 *
 * # Validate multiple files
 * java -cp conductor.jar com.skanga.conductor.workflow.config.WorkflowValidator \
 *     workflow1.yaml workflow2.yaml workflow3.yaml
 *
 * # Validate all workflows in a directory
 * java -cp conductor.jar com.skanga.conductor.workflow.config.WorkflowValidator \
 *     workflows/*.yaml
 * </pre>
 *
 * @since 1.1.0
 */
public class WorkflowValidator {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        WorkflowSchemaValidator validator = new WorkflowSchemaValidator();
        int totalFiles = 0;
        int validFiles = 0;
        int invalidFiles = 0;

        System.out.println("Conductor Workflow Validator");
        System.out.println("============================");
        System.out.println();

        for (String filePath : args) {
            totalFiles++;
            System.out.println("Validating: " + filePath);

            WorkflowSchemaValidator.ValidationResult result = validator.validateWorkflowFile(filePath);

            if (result.isValid()) {
                validFiles++;
                System.out.println("  ✓ Valid");
            } else {
                invalidFiles++;
                System.out.println("  ✗ Invalid");
                System.out.println();
                System.out.println("  Errors:");
                for (String error : result.getErrors()) {
                    System.out.println("    - " + error);
                }
            }
            System.out.println();
        }

        System.out.println("Summary");
        System.out.println("-------");
        System.out.println("Total files:   " + totalFiles);
        System.out.println("Valid:         " + validFiles);
        System.out.println("Invalid:       " + invalidFiles);

        if (invalidFiles > 0) {
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp conductor.jar com.skanga.conductor.workflow.config.WorkflowValidator <file1.yaml> [file2.yaml ...]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Validate a single file");
        System.out.println("  java ... WorkflowValidator workflows/my-workflow.yaml");
        System.out.println();
        System.out.println("  # Validate multiple files");
        System.out.println("  java ... WorkflowValidator workflow1.yaml workflow2.yaml");
        System.out.println();
        System.out.println("  # Validate all YAML files in a directory (using shell glob)");
        System.out.println("  java ... WorkflowValidator workflows/*.yaml");
    }
}
