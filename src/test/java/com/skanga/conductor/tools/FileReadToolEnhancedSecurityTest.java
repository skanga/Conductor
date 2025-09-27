package com.skanga.conductor.tools;

import com.skanga.conductor.testbase.ConductorTestBase;
import com.skanga.conductor.execution.ExecutionInput;
import com.skanga.conductor.execution.ExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FileReadTool Enhanced Security Tests")
class FileReadToolEnhancedSecurityTest extends ConductorTestBase {

    private FileReadTool fileReadTool;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        fileReadTool = new FileReadTool(tempDir.toString(), false, 1024 * 1024); // 1MB limit
    }

    @Test
    @DisplayName("Should block URL scheme attacks")
    void testUrlSchemeAttacks() {
        String[] urlSchemes = {
            "http://evil.com/file.txt",
            "https://malicious.site/data",
            "file:///etc/passwd",
            "ftp://server/file",
            "ldap://server/query",
            "javascript:alert(1)",
            "vbscript:msgbox(1)",
            "data:text/plain;base64,SGVsbG8=",
            "mailto:test@evil.com",
            "tel:+1234567890"
        };

        for (String scheme : urlSchemes) {
            ExecutionInput input = ExecutionInput.of(scheme);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "URL scheme should be blocked: " + scheme);
            assertTrue(result.output().contains("suspicious patterns") ||
                      result.output().contains("security"),
                      "Error should mention security issue for: " + scheme);
        }
    }

    @Test
    @DisplayName("Should block encoded path traversal attacks")
    void testEncodedPathTraversalAttacks() {
        String[] encodedPaths = {
            "%2e%2e/etc/passwd",                    // URL encoded ..
            "%252e%252e/sensitive/data",            // Double URL encoded ..
            "\\u002e\\u002e/etc/shadow",           // Unicode escaped ..
            "\\x2e\\x2e/boot/config",              // Hex escaped ..
            "%u002e%u002e/system/file",            // Unicode URL encoded ..
            "%c0%ae%c0%ae/overlong/path",           // Overlong UTF-8 ..
            "%e0%80%ae%e0%80%ae/another/path",     // Another overlong UTF-8 ..
            "..%2fetc%2fpasswd",                   // Mixed encoded/unencoded
            "..%5cwindows%5csystem32",             // Mixed with backslash
            "...../etc/passwd",                     // Multiple dots
            "....//system/config"                   // Multiple dots with separator
        };

        for (String encodedPath : encodedPaths) {
            ExecutionInput input = ExecutionInput.of(encodedPath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Encoded traversal should be blocked: " + encodedPath);
            assertTrue(result.output().toLowerCase().contains("suspicious") ||
                      result.output().toLowerCase().contains("traversal") ||
                      result.output().toLowerCase().contains("encoded"),
                      "Error should mention traversal/encoding issue for: " + encodedPath);
        }
    }

    @Test
    @DisplayName("Should block Windows device name attacks")
    void testWindowsDeviceNameAttacks() {
        String[] devicePaths = {
            "CON",
            "PRN.txt",
            "AUX.log",
            "NUL.dat",
            "COM1.config",
            "COM9.ini",
            "LPT1.tmp",
            "LPT9.bak",
            "path/CON/file.txt",
            "folder\\PRN\\data.xml",
            "config/AUX.properties",
            "temp\\COM1.settings"
        };

        for (String devicePath : devicePaths) {
            ExecutionInput input = ExecutionInput.of(devicePath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Device name should be blocked: " + devicePath);
        }
    }

    @Test
    @DisplayName("Should block invisible and zero-width character attacks")
    void testInvisibleCharacterAttacks() {
        String[] invisiblePaths = {
            "file\u200B.txt",                      // Zero Width Space
            "config\u200C.properties",             // Zero Width Non-Joiner
            "data\u200D.json",                     // Zero Width Joiner
            "\uFEFFmalicious.txt",                  // Zero Width No-Break Space (BOM)
            "file\u2060.log",                      // Word Joiner
            "test\u202D.txt",                      // Left-to-Right Override
            "evil\u202E.config",                   // Right-to-Left Override
            "file\u2066name.txt",                  // Left-to-Right Isolate
            "config\u2067.properties",             // Right-to-Left Isolate
            "data\u2068.json",                     // First Strong Isolate
            "file\u2069.txt",                      // Pop Directional Isolate
            "normal\u0000file.txt",                // Null byte
            "config\u0001.properties",             // Start of Heading
            "data\u001F.json"                      // Unit Separator
        };

        for (String invisiblePath : invisiblePaths) {
            ExecutionInput input = ExecutionInput.of(invisiblePath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Invisible characters should be blocked: " +
                       invisiblePath.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\u200B-\\u200D\\uFEFF\\u2060\\u202D-\\u2069]", "<?>"));
        }
    }

    @Test
    @DisplayName("Should block template injection attacks")
    void testTemplateInjectionAttacks() {
        String[] templatePaths = {
            "${java.runtime}",                      // Java EL injection
            "#{user.home}/evil.txt",               // EL injection
            "%{#context['xwork.MethodAccessor.denyMethodExecution']=false}", // Struts injection
            "$(whoami).txt",                       // Shell command substitution
            "{{7*7}}.txt",                         // Handlebars injection
            "{%for x in range(10)%}",              // Jinja2 injection
            "<%=System.getProperty('user.dir')%>", // JSP injection
            "[%SET x=7*7%]",                       // Template Toolkit
            "[[#{expression}]]",                   // Lua template
            "file`whoami`.txt",                    // Backtick execution
            "config;rm -rf /.txt",                 // Command separator
            "data&&echo evil.txt",                 // Command chaining
            "file|cat /etc/passwd.txt"             // Pipe operation
        };

        for (String templatePath : templatePaths) {
            ExecutionInput input = ExecutionInput.of(templatePath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Template injection should be blocked: " + templatePath);
        }
    }

    @Test
    @DisplayName("Should block UNC path attacks")
    void testUncPathAttacks() {
        String[] uncPaths = {
            "\\\\server\\share\\file.txt",
            "\\\\evil.com\\malicious\\data.txt",
            "\\\\192.168.1.100\\admin$\\config.txt",
            "\\\\localhost\\c$\\windows\\system32\\config\\sam"
        };

        for (String uncPath : uncPaths) {
            ExecutionInput input = ExecutionInput.of(uncPath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "UNC path should be blocked: " + uncPath);
        }
    }

    @Test
    @DisplayName("Should block mixed separator attacks")
    void testMixedSeparatorAttacks() {
        String[] mixedPaths = {
            "folder/subfolder\\file.txt",
            "config\\settings/database.properties",
            "path/with\\mixed/separators\\file.log",
            "..\\windows/system32/config.txt"
        };

        for (String mixedPath : mixedPaths) {
            ExecutionInput input = ExecutionInput.of(mixedPath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Mixed separator path should be blocked: " + mixedPath);
        }
    }

    @Test
    @DisplayName("Should block case variation attacks")
    void testCaseVariationAttacks() {
        String[] casePaths = {
            "/System32/config.txt",
            "\\Windows\\explorer.exe",
            "/ETC/passwd",
            "/USR/bin/bash",
            "/VAR/log/system.log",
            "/BIN/sh",
            "/SBIN/init",
            ".A.b../etc/passwd",                   // Mixed case dots
            ".a.B../windows/system.ini"           // Mixed case dots
        };

        for (String casePath : casePaths) {
            ExecutionInput input = ExecutionInput.of(casePath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Case variation attack should be blocked: " + casePath);
        }
    }

    @Test
    @DisplayName("Should block excessively long paths")
    void testLongPathAttacks() {
        // Create very long path (exceed 32767 characters)
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 1500; i++) {
            longPath.append("very_long_directory_name_that_exceeds_windows_extended_path_limit_");
        }
        longPath.append("file.txt");

        ExecutionInput input = ExecutionInput.of(longPath.toString());
        ExecutionResult result = fileReadTool.runTool(input);

        assertFalse(result.success(), "Excessively long path should be blocked");
        assertTrue(result.output().contains("too long") ||
                  result.output().contains("length") ||
                  result.output().contains("limit") ||
                  result.output().contains("maximum"),
                  "Error should mention length limit. Actual error: " + result.output());
    }

    @Test
    @DisplayName("Should block deeply nested paths")
    void testDeeplyNestedPathAttacks() {
        // Create deeply nested path (exceed 100 separators)
        StringBuilder deepPath = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            deepPath.append("level").append(i).append("/");
        }
        deepPath.append("file.txt");

        ExecutionInput input = ExecutionInput.of(deepPath.toString());
        ExecutionResult result = fileReadTool.runTool(input);

        assertFalse(result.success(), "Deeply nested path should be blocked");
        assertTrue(result.output().contains("nesting") ||
                  result.output().contains("deep") ||
                  result.output().contains("too long") ||
                  result.output().contains("suspicious"),
                  "Error should mention nesting issue. Actual error: " + result.output());
    }

    @Test
    @DisplayName("Should allow safe paths")
    void testSafePaths() {
        String[] safePaths = {
            "file.txt",
            "config.properties",
            "data/file.json",
            "logs/application.log",
            "temp/cache.dat",
            "subfolder/document.pdf",
            "normal-file-name.txt",
            "file_with_underscores.log",
            "CamelCaseFile.xml",
            "123numeric.txt"
        };

        for (String safePath : safePaths) {
            // Note: These tests assume the files don't exist, so we expect a "file not found" rather than a security error
            ExecutionInput input = ExecutionInput.of(safePath);
            ExecutionResult result = fileReadTool.runTool(input);

            // File won't exist, but it shouldn't be blocked for security reasons
            if (!result.success()) {
                // If it fails, it should be due to file not found, not security
                assertTrue(result.output().contains("not found") ||
                          result.output().contains("does not exist") ||
                          result.output().contains("No such file"),
                          "Safe path should only fail due to file not found: " + safePath +
                          " - Error: " + result.output());
            }
        }
    }

    @Test
    @DisplayName("Should block drive letter access attempts")
    void testDriveLetterAttacks() {
        String[] drivePaths = {
            "C:",
            "D:/windows",
            "E:\\data",
            "Z:/system",
            "c:/windows/system32",
            "path/C:/evil"
        };

        for (String drivePath : drivePaths) {
            ExecutionInput input = ExecutionInput.of(drivePath);
            ExecutionResult result = fileReadTool.runTool(input);

            assertFalse(result.success(), "Drive letter access should be blocked: " + drivePath);
        }
    }
}