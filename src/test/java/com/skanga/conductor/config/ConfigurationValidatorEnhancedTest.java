package com.skanga.conductor.config;

import com.skanga.conductor.testbase.ConductorTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Configuration Validator Enhanced Security Tests")
class ConfigurationValidatorEnhancedTest extends ConductorTestBase {

    @Test
    @DisplayName("Should block injection pattern attacks in configuration paths")
    void testInjectionPatternBlocking() {
        String[] injectionPaths = {
            "${java.runtime.version}",             // Java EL injection
            "#{user.home}/config",                 // EL injection
            "%{#context.runtime}",                 // Struts injection
            "$(whoami)/config",                    // Shell command substitution
            "{{7*7}}/path",                        // Handlebars injection
            "{%for x in range(10)%}",              // Django/Jinja2 injection
            "<%=System.getProperty('os.name')%>",  // JSP injection
            "[%SET x=system('id')%]",              // Template Toolkit
            "javascript:alert(1)",                 // JavaScript protocol
            "vbscript:msgbox(1)",                  // VBScript protocol
            "data:text/plain;base64,test",         // Data URI scheme
            "config`whoami`.txt",                  // Backtick execution
            "path;rm -rf /",                       // Command separator
            "config && echo evil",                 // Command chaining
            "file | cat /etc/passwd"               // Pipe operation
        };

        for (String injectionPath : injectionPaths) {
            assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
                ConfigurationValidator.validateSecurePath(injectionPath, "test.path");
            }, "Injection pattern should be blocked: " + injectionPath);
        }
    }

    @Test
    @DisplayName("Should block encoded traversal patterns in configuration paths")
    void testEncodedTraversalBlocking() {
        String[] encodedPaths = {
            "%2e%2e/etc/passwd",                   // URL encoded ..
            "%252e%252e/system/config",            // Double URL encoded ..
            "\\u002e\\u002e/sensitive/data",      // Unicode escaped ..
            "\\x2e\\x2e/boot/config",             // Hex escaped ..
            "%u002e%u002e/system/file",           // Unicode URL encoded ..
            "%c0%ae%c0%ae/overlong/path",          // Overlong UTF-8 ..
            "%e0%80%ae%e0%80%ae/another/path",    // Another overlong UTF-8 ..
            "..%2fetc%2fpasswd",                  // Mixed encoded/unencoded
            "..%5cwindows%5csystem32",            // Mixed with backslash
            "%2f../etc/passwd",                    // Forward slash with ..
            "%5c../windows/system32",              // Backslash with ..
            "...../etc/passwd",                    // Multiple dots (3+)
            "....//system/config"                  // Multiple dots with separator
        };

        for (String encodedPath : encodedPaths) {
            assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
                ConfigurationValidator.validateSecurePath(encodedPath, "test.path");
            }, "Encoded traversal should be blocked: " + encodedPath);
        }
    }

    @Test
    @DisplayName("Should block platform-specific security threats")
    void testPlatformSpecificThreats() {
        String[] threatPaths = {
            // Windows device names
            "CON",
            "PRN.config",
            "AUX.properties",
            "NUL.txt",
            "COM1.ini",
            "COM9.cfg",
            "LPT1.dat",
            "LPT9.tmp",
            "path/CON/config.txt",
            "folder\\PRN\\settings.xml",
            "config/AUX.properties",
            "temp\\COM1.settings",

            // UNC paths
            "\\\\server\\share\\config.txt",
            "\\\\evil.com\\malicious\\settings",

            // Drive letter access
            "C:",
            "D:/config",
            "E:\\settings",
            "path/C:/evil",

            // System directory access
            "/system32/config.txt",
            "\\system32\\settings.ini",
            "/windows/explorer.exe",
            "\\windows\\system.ini",
            "/etc/passwd",
            "/etc/shadow",
            "/boot/grub.cfg",
            "/proc/cpuinfo",
            "/sys/kernel"
        };

        for (String threatPath : threatPaths) {
            assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
                ConfigurationValidator.validateSecurePath(threatPath, "test.path");
            }, "Platform threat should be blocked: " + threatPath);
        }
    }

    @Test
    @DisplayName("Should block dangerous characters in configuration paths")
    void testDangerousCharacterBlocking() {
        String[] dangerousPaths = {
            // Control characters
            "config\u0000.txt",                   // Null byte
            "file\u0001.properties",              // Start of Heading
            "data\u001F.json",                    // Unit Separator
            "config\u007F.ini",                   // Delete character
            "file\u0080.cfg",                     // High control character

            // Zero-width and invisible characters
            "file\u200B.txt",                     // Zero Width Space
            "config\u200C.properties",            // Zero Width Non-Joiner
            "data\u200D.json",                    // Zero Width Joiner
            "\uFEFFconfig.txt",                   // Zero Width No-Break Space (BOM)
            "file\u2060.log",                     // Word Joiner
            "test\u202D.txt",                     // Left-to-Right Override
            "evil\u202E.config",                  // Right-to-Left Override
            "file\u2066name.txt",                 // Left-to-Right Isolate
            "config\u2067.properties",            // Right-to-Left Isolate
            "data\u2068.json",                    // First Strong Isolate
            "file\u2069.txt",                     // Pop Directional Isolate

            // Forbidden filename characters
            "file<name>.txt",                     // Less than
            "config>file.properties",             // Greater than
            "data:stream.json",                   // Colon (in wrong context)
            "file\"name.txt",                     // Quote
            "config|pipe.properties",             // Pipe
            "file?query.txt",                     // Question mark
            "data*wildcard.json"                  // Asterisk
        };

        for (String dangerousPath : dangerousPaths) {
            assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
                ConfigurationValidator.validateSecurePath(dangerousPath, "test.path");
            }, "Dangerous character should be blocked in: " +
               dangerousPath.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F\\u200B-\\u200D\\uFEFF\\u2060\\u202D-\\u2069]", "<?>"));
        }
    }

    @Test
    @DisplayName("Should block excessively long configuration paths")
    void testLongPathBlocking() {
        // Create path longer than 4096 characters
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 85; i++) { // Reduced from 200 to 85 for faster testing - still long enough to trigger validation
            longPath.append("very_long_directory_name_that_exceeds_reasonable_limits_");
        }
        longPath.append("config.properties");

        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath(longPath.toString(), "test.path");
        }, "Excessively long path should be blocked");

        try {
            ConfigurationValidator.validateSecurePath(longPath.toString(), "test.path");
            fail("Should have thrown ConfigurationException");
        } catch (com.skanga.conductor.exception.ConfigurationException e) {
            assertTrue(e.getMessage().contains("maximum length"),
                      "Error should mention length limit");
        }
    }

    @Test
    @DisplayName("Should block excessively nested configuration paths")
    void testExcessiveNestingBlocking() {
        // Create path with more than 50 directory separators
        StringBuilder deepPath = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            deepPath.append("level").append(i).append("/");
        }
        deepPath.append("config.properties");

        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath(deepPath.toString(), "test.path");
        }, "Excessively nested path should be blocked");

        try {
            ConfigurationValidator.validateSecurePath(deepPath.toString(), "test.path");
            fail("Should have thrown ConfigurationException");
        } catch (com.skanga.conductor.exception.ConfigurationException e) {
            assertTrue(e.getMessage().contains("nesting"),
                      "Error should mention nesting issue");
        }
    }

    @Test
    @DisplayName("Should allow safe configuration paths")
    void testSafeConfigurationPaths() {
        String[] safePaths = {
            "config.properties",
            "application.yml",
            "settings.xml",
            "data/database.config",
            "conf/application.properties",
            "config/dev/settings.yml",
            "resources/application-prod.properties",
            "config/conductor.conf",
            "configuration/app.config",
            "settings/user.preferences",
            "normal-config-file.properties",
            "config_with_underscores.yml",
            "CamelCaseConfig.xml",
            "123numeric-config.properties",
            "./relative/path/config.yml",
            "subfolder/nested/deep/config.properties"
        };

        for (String safePath : safePaths) {
            assertDoesNotThrow(() -> {
                ConfigurationValidator.validateSecurePath(safePath, "test.path");
            }, "Safe path should be allowed: " + safePath);
        }
    }

    @Test
    @DisplayName("Should provide meaningful error messages")
    void testErrorMessages() {
        // Test different types of errors have specific messages

        // Injection patterns
        try {
            ConfigurationValidator.validateSecurePath("${malicious}", "test.injection");
            fail("Should have thrown exception");
        } catch (com.skanga.conductor.exception.ConfigurationException e) {
            assertTrue(e.getMessage().contains("dangerous expressions"));
            assertTrue(e.getMessage().contains("test.injection"));
        }

        // Encoded traversal
        try {
            ConfigurationValidator.validateSecurePath("%2e%2e/etc/passwd", "test.encoded");
            fail("Should have thrown exception");
        } catch (com.skanga.conductor.exception.ConfigurationException e) {
            assertTrue(e.getMessage().contains("encoded path traversal"));
            assertTrue(e.getMessage().contains("test.encoded"));
        }

        // Platform threats
        try {
            ConfigurationValidator.validateSecurePath("CON.txt", "test.platform");
            fail("Should have thrown exception");
        } catch (com.skanga.conductor.exception.ConfigurationException e) {
            assertTrue(e.getMessage().contains("platform-specific security threats"));
            assertTrue(e.getMessage().contains("test.platform"));
        }

        // Dangerous characters
        try {
            ConfigurationValidator.validateSecurePath("file\u0000.txt", "test.chars");
            fail("Should have thrown exception");
        } catch (com.skanga.conductor.exception.ConfigurationException e) {
            assertTrue(e.getMessage().contains("dangerous characters"));
            assertTrue(e.getMessage().contains("test.chars"));
        }
    }

    @Test
    @DisplayName("Should handle edge cases correctly")
    void testEdgeCases() {
        // Empty string (should fail on validateNotEmpty)
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath("", "empty.path");
        });

        // Null string (should fail on validateNotEmpty)
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath(null, "null.path");
        });

        // Single character paths
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateSecurePath("a", "single.char");
        });

        // Path at exactly the length limit (4096 chars)
        StringBuilder exactLengthPath = new StringBuilder();
        while (exactLengthPath.length() < 4096) {
            exactLengthPath.append("a");
        }
        // Should be exactly 4096 chars and allowed
        assertEquals(4096, exactLengthPath.length());
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateSecurePath(exactLengthPath.toString(), "exact.length");
        });

        // Path just over the length limit (4097 chars)
        exactLengthPath.append("x");
        assertEquals(4097, exactLengthPath.length());
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath(exactLengthPath.toString(), "over.length");
        });

        // Path with exactly 50 separators (should be allowed)
        StringBuilder exactNestingPath = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            exactNestingPath.append("a/");
        }
        exactNestingPath.append("config.txt");
        assertDoesNotThrow(() -> {
            ConfigurationValidator.validateSecurePath(exactNestingPath.toString(), "exact.nesting");
        });

        // Path with 51 separators (should be blocked)
        exactNestingPath.append("/b");
        assertThrows(com.skanga.conductor.exception.ConfigurationException.class, () -> {
            ConfigurationValidator.validateSecurePath(exactNestingPath.toString(), "over.nesting");
        });
    }
}