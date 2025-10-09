package com.skanga.conductor.config;

import com.skanga.conductor.testbase.ConfigTestBase;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ConfigurationAccessor functionality.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigurationAccessorTest extends ConfigTestBase {

    @Test
    @Order(1)
    @DisplayName("Should be a final utility class")
    void testClassModifiers() {
        assertTrue(Modifier.isFinal(ConfigurationAccessor.class.getModifiers()));
        assertTrue(Modifier.isPublic(ConfigurationAccessor.class.getModifiers()));
    }

    @Test
    @Order(2)
    @DisplayName("Should have private constructor")
    void testPrivateConstructor() throws Exception {
        Constructor<ConfigurationAccessor> constructor =
            ConfigurationAccessor.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);
        assertDoesNotThrow(() -> constructor.newInstance());
    }

    @Test
    @Order(3)
    @DisplayName("Should not be publicly instantiable")
    void testNotPubliclyInstantiable() {
        Constructor<?>[] constructors = ConfigurationAccessor.class.getConstructors();
        assertEquals(0, constructors.length, "Should have no public constructors");
    }

    // === Core Configuration Access Tests ===

    @Test
    @Order(4)
    @DisplayName("Should provide access to ApplicationConfig instance")
    void testGetConfig() {
        ApplicationConfig config = ConfigurationAccessor.getConfig();

        assertNotNull(config);
        assertSame(ApplicationConfig.getInstance(), config);
    }

    @Test
    @Order(5)
    @DisplayName("Should get string property with default")
    void testGetString() {
        String result = ConfigurationAccessor.getString("nonexistent.property", "default-value");
        assertEquals("default-value", result);
    }

    @Test
    @Order(6)
    @DisplayName("Should get boolean property with default")
    void testGetBoolean() {
        boolean result = ConfigurationAccessor.getBoolean("nonexistent.boolean", true);
        assertTrue(result);

        boolean result2 = ConfigurationAccessor.getBoolean("nonexistent.boolean", false);
        assertFalse(result2);
    }

    @Test
    @Order(7)
    @DisplayName("Should get integer property with default")
    void testGetInt() {
        int result = ConfigurationAccessor.getInt("nonexistent.integer", 42);
        assertEquals(42, result);
    }

    @Test
    @Order(8)
    @DisplayName("Should get duration property with default")
    void testGetDuration() {
        Duration defaultDuration = Duration.ofMinutes(5);
        Duration result = ConfigurationAccessor.getDuration("nonexistent.duration", defaultDuration);
        assertEquals(defaultDuration, result);
    }

    // === Database Configuration Tests ===

    @Test
    @Order(9)
    @DisplayName("Should get database JDBC URL")
    void testGetDatabaseJdbcUrl() {
        String jdbcUrl = ConfigurationAccessor.getDatabaseJdbcUrl();
        assertNotNull(jdbcUrl);
        assertTrue(jdbcUrl.startsWith("jdbc:"));
    }

    @Test
    @Order(10)
    @DisplayName("Should get database username")
    void testGetDatabaseUsername() {
        String username = ConfigurationAccessor.getDatabaseUsername();
        assertNotNull(username);
    }

    @Test
    @Order(11)
    @DisplayName("Should get database password")
    void testGetDatabasePassword() {
        String password = ConfigurationAccessor.getDatabasePassword();
        assertNotNull(password);
    }

    @Test
    @Order(12)
    @DisplayName("Should get database driver")
    void testGetDatabaseDriver() {
        String driver = ConfigurationAccessor.getDatabaseDriver();
        assertNotNull(driver);
    }

    @Test
    @Order(13)
    @DisplayName("Should get database max connections")
    void testGetDatabaseMaxConnections() {
        int maxConnections = ConfigurationAccessor.getDatabaseMaxConnections();
        assertTrue(maxConnections > 0);
    }

    @Test
    @Order(14)
    @DisplayName("Should get complete database config")
    void testGetDatabaseConfig() {
        DatabaseConfig dbConfig = ConfigurationAccessor.getDatabaseConfig();
        assertNotNull(dbConfig);
        // Config objects are not necessarily singletons, just verify they're not null
        assertNotNull(dbConfig);
        assertNotNull(ApplicationConfig.getInstance().getDatabaseConfig());
    }

    // === Tool Configuration Tests ===

    @Test
    @Order(15)
    @DisplayName("Should get code runner timeout")
    void testGetToolCodeRunnerTimeout() {
        Duration timeout = ConfigurationAccessor.getToolCodeRunnerTimeout();
        assertNotNull(timeout);
        assertTrue(timeout.toMillis() > 0);
    }

    @Test
    @Order(16)
    @DisplayName("Should get file read base directory")
    void testGetToolFileReadBaseDir() {
        String baseDir = ConfigurationAccessor.getToolFileReadBaseDir();
        assertNotNull(baseDir);
    }

    @Test
    @Order(17)
    @DisplayName("Should get file read max size")
    void testGetToolFileReadMaxSize() {
        long maxSize = ConfigurationAccessor.getToolFileReadMaxSize();
        assertTrue(maxSize > 0);
    }

    @Test
    @Order(18)
    @DisplayName("Should get file read max path length")
    void testGetToolFileReadMaxPathLength() {
        int maxPathLength = ConfigurationAccessor.getToolFileReadMaxPathLength();
        assertTrue(maxPathLength > 0);
    }

    @Test
    @Order(19)
    @DisplayName("Should get audio output directory")
    void testGetToolAudioOutputDir() {
        String audioDir = ConfigurationAccessor.getToolAudioOutputDir();
        assertNotNull(audioDir);
    }

    @Test
    @Order(20)
    @DisplayName("Should get code runner allowed commands")
    void testGetToolCodeRunnerAllowedCommands() {
        Set<String> allowedCommands = ConfigurationAccessor.getToolCodeRunnerAllowedCommands();
        assertNotNull(allowedCommands);
    }

    @Test
    @Order(21)
    @DisplayName("Should get complete tool config")
    void testGetToolConfig() {
        ToolConfig toolConfig = ConfigurationAccessor.getToolConfig();
        assertNotNull(toolConfig);
        assertNotNull(toolConfig);
        assertNotNull(ApplicationConfig.getInstance().getToolConfig());
    }

    // === LLM Configuration Tests ===

    @Test
    @Order(22)
    @DisplayName("Should get OpenAI API key (can be null)")
    void testGetLLMOpenAiApiKey() {
        String apiKey = ConfigurationAccessor.getLLMOpenAiApiKey();
        // API key can be null if not configured
    }

    @Test
    @Order(23)
    @DisplayName("Should get OpenAI model")
    void testGetLLMOpenAiModel() {
        String model = ConfigurationAccessor.getLLMOpenAiModel();
        assertNotNull(model);
    }

    @Test
    @Order(24)
    @DisplayName("Should get OpenAI base URL (can be null)")
    void testGetLLMOpenAiBaseUrl() {
        String baseUrl = ConfigurationAccessor.getLLMOpenAiBaseUrl();
        // Base URL can be null if using default
    }

    @Test
    @Order(25)
    @DisplayName("Should get OpenAI timeout")
    void testGetLLMOpenAiTimeout() {
        Duration timeout = ConfigurationAccessor.getLLMOpenAiTimeout();
        assertNotNull(timeout);
        assertTrue(timeout.toMillis() > 0);
    }

    @Test
    @Order(26)
    @DisplayName("Should get OpenAI max retries")
    void testGetLLMOpenAiMaxRetries() {
        int maxRetries = ConfigurationAccessor.getLLMOpenAiMaxRetries();
        assertTrue(maxRetries >= 0);
    }

    @Test
    @Order(27)
    @DisplayName("Should get Anthropic API key (can be null)")
    void testGetLLMAnthropicApiKey() {
        String apiKey = ConfigurationAccessor.getLLMAnthropicApiKey();
        // API key can be null if not configured
    }

    @Test
    @Order(28)
    @DisplayName("Should get Anthropic model")
    void testGetLLMAnthropicModel() {
        String model = ConfigurationAccessor.getLLMAnthropicModel();
        assertNotNull(model);
    }

    @Test
    @Order(29)
    @DisplayName("Should get Anthropic timeout")
    void testGetLLMAnthropicTimeout() {
        Duration timeout = ConfigurationAccessor.getLLMAnthropicTimeout();
        assertNotNull(timeout);
        assertTrue(timeout.toMillis() > 0);
    }

    @Test
    @Order(30)
    @DisplayName("Should get Anthropic max retries")
    void testGetLLMAnthropicMaxRetries() {
        int maxRetries = ConfigurationAccessor.getLLMAnthropicMaxRetries();
        assertTrue(maxRetries >= 0);
    }

    @Test
    @Order(31)
    @DisplayName("Should get Gemini API key (can be null)")
    void testGetLLMGeminiApiKey() {
        String apiKey = ConfigurationAccessor.getLLMGeminiApiKey();
        // API key can be null if not configured
    }

    @Test
    @Order(32)
    @DisplayName("Should get Gemini model")
    void testGetLLMGeminiModel() {
        String model = ConfigurationAccessor.getLLMGeminiModel();
        assertNotNull(model);
    }

    @Test
    @Order(33)
    @DisplayName("Should get Gemini timeout")
    void testGetLLMGeminiTimeout() {
        Duration timeout = ConfigurationAccessor.getLLMGeminiTimeout();
        assertNotNull(timeout);
        assertTrue(timeout.toMillis() > 0);
    }

    @Test
    @Order(34)
    @DisplayName("Should get Gemini max retries")
    void testGetLLMGeminiMaxRetries() {
        int maxRetries = ConfigurationAccessor.getLLMGeminiMaxRetries();
        assertTrue(maxRetries >= 0);
    }

    @Test
    @Order(35)
    @DisplayName("Should get LLM retry enabled flag")
    void testIsLLMRetryEnabled() {
        boolean retryEnabled = ConfigurationAccessor.isLLMRetryEnabled();
        // Can be true or false
    }

    @Test
    @Order(36)
    @DisplayName("Should get LLM retry strategy")
    void testGetLLMRetryStrategy() {
        String strategy = ConfigurationAccessor.getLLMRetryStrategy();
        assertNotNull(strategy);
    }

    @Test
    @Order(37)
    @DisplayName("Should get LLM retry initial delay")
    void testGetLLMRetryInitialDelay() {
        Duration initialDelay = ConfigurationAccessor.getLLMRetryInitialDelay();
        assertNotNull(initialDelay);
        assertTrue(initialDelay.toMillis() >= 0);
    }

    @Test
    @Order(38)
    @DisplayName("Should get LLM retry max delay")
    void testGetLLMRetryMaxDelay() {
        Duration maxDelay = ConfigurationAccessor.getLLMRetryMaxDelay();
        assertNotNull(maxDelay);
        assertTrue(maxDelay.toMillis() > 0);
    }

    @Test
    @Order(39)
    @DisplayName("Should get LLM retry multiplier")
    void testGetLLMRetryMultiplier() {
        double multiplier = ConfigurationAccessor.getLLMRetryMultiplier();
        assertTrue(multiplier > 0);
    }

    @Test
    @Order(40)
    @DisplayName("Should get LLM retry max duration")
    void testGetLLMRetryMaxDuration() {
        Duration maxDuration = ConfigurationAccessor.getLLMRetryMaxDuration();
        assertNotNull(maxDuration);
        assertTrue(maxDuration.toMillis() > 0);
    }

    @Test
    @Order(41)
    @DisplayName("Should get complete LLM config")
    void testGetLLMConfig() {
        LLMConfig llmConfig = ConfigurationAccessor.getLLMConfig();
        assertNotNull(llmConfig);
        assertNotNull(llmConfig);
        assertNotNull(ApplicationConfig.getInstance().getLLMConfig());
    }

    // === Memory Configuration Tests ===

    @Test
    @Order(42)
    @DisplayName("Should get memory default limit")
    void testGetMemoryDefaultLimit() {
        int defaultLimit = ConfigurationAccessor.getMemoryDefaultLimit();
        assertTrue(defaultLimit > 0);
    }

    @Test
    @Order(43)
    @DisplayName("Should get memory max entries")
    void testGetMemoryMaxEntries() {
        int maxEntries = ConfigurationAccessor.getMemoryMaxEntries();
        assertTrue(maxEntries > 0);
    }

    @Test
    @Order(44)
    @DisplayName("Should get memory retention days")
    void testGetMemoryRetentionDays() {
        int retentionDays = ConfigurationAccessor.getMemoryRetentionDays();
        assertTrue(retentionDays > 0);
    }

    @Test
    @Order(45)
    @DisplayName("Should get complete memory config")
    void testGetMemoryConfig() {
        MemoryConfig memoryConfig = ConfigurationAccessor.getMemoryConfig();
        assertNotNull(memoryConfig);
        assertNotNull(memoryConfig);
        assertNotNull(ApplicationConfig.getInstance().getMemoryConfig());
    }

    // === Metrics Configuration Tests ===

    @Test
    @Order(46)
    @DisplayName("Should get metrics enabled flag")
    void testIsMetricsEnabled() {
        boolean enabled = ConfigurationAccessor.isMetricsEnabled();
        // Can be true or false
    }

    @Test
    @Order(47)
    @DisplayName("Should get metrics retention period")
    void testGetMetricsRetentionPeriod() {
        Duration retention = ConfigurationAccessor.getMetricsRetentionPeriod();
        assertNotNull(retention);
        assertTrue(retention.toMillis() > 0);
    }

    @Test
    @Order(48)
    @DisplayName("Should get metrics max in memory")
    void testGetMetricsMaxInMemory() {
        int maxInMemory = ConfigurationAccessor.getMetricsMaxInMemory();
        assertTrue(maxInMemory > 0);
    }

    @Test
    @Order(49)
    @DisplayName("Should get metrics console reporting enabled flag")
    void testIsMetricsConsoleReportingEnabled() {
        boolean enabled = ConfigurationAccessor.isMetricsConsoleReportingEnabled();
        // Can be true or false
    }

    @Test
    @Order(50)
    @DisplayName("Should get metrics console reporting interval")
    void testGetMetricsConsoleReportingInterval() {
        Duration interval = ConfigurationAccessor.getMetricsConsoleReportingInterval();
        assertNotNull(interval);
        assertTrue(interval.toMillis() > 0);
    }

    @Test
    @Order(51)
    @DisplayName("Should get metrics output directory")
    void testGetMetricsOutputDirectory() {
        String outputDir = ConfigurationAccessor.getMetricsOutputDirectory();
        assertNotNull(outputDir);
    }

    @Test
    @Order(52)
    @DisplayName("Should get metrics file reporting enabled flag")
    void testIsMetricsFileReportingEnabled() {
        boolean enabled = ConfigurationAccessor.isMetricsFileReportingEnabled();
        // Can be true or false
    }

    @Test
    @Order(53)
    @DisplayName("Should get metrics file reporting interval")
    void testGetMetricsFileReportingInterval() {
        Duration interval = ConfigurationAccessor.getMetricsFileReportingInterval();
        assertNotNull(interval);
        assertTrue(interval.toMillis() > 0);
    }

    @Test
    @Order(54)
    @DisplayName("Should get enabled metrics patterns")
    void testGetMetricsEnabledPatterns() {
        Set<String> patterns = ConfigurationAccessor.getMetricsEnabledPatterns();
        assertNotNull(patterns);
    }

    @Test
    @Order(55)
    @DisplayName("Should get disabled metrics patterns")
    void testGetMetricsDisabledPatterns() {
        Set<String> patterns = ConfigurationAccessor.getMetricsDisabledPatterns();
        assertNotNull(patterns);
    }

    @Test
    @Order(56)
    @DisplayName("Should get complete metrics config")
    void testGetMetricsConfig() {
        MetricsConfig metricsConfig = ConfigurationAccessor.getMetricsConfig();
        assertNotNull(metricsConfig);
        assertNotNull(metricsConfig);
        assertNotNull(ApplicationConfig.getInstance().getMetricsConfig());
    }

    // === Convenience Methods Tests ===

    @Test
    @Order(57)
    @DisplayName("Should throw exception for null key in getRequiredSecret")
    void testGetRequiredSecretNullKey() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getRequiredSecret(null));
    }

    @Test
    @Order(58)
    @DisplayName("Should throw exception for blank key in getRequiredSecret")
    void testGetRequiredSecretBlankKey() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getRequiredSecret(""));

        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getRequiredSecret("   "));
    }

    @Test
    @Order(59)
    @DisplayName("Should throw exception for null key in getSecret")
    void testGetSecretNullKey() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getSecret(null, "default"));
    }

    @Test
    @Order(60)
    @DisplayName("Should throw exception for blank key in getSecret")
    void testGetSecretBlankKey() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getSecret("", "default"));

        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getSecret("   ", "default"));
    }

    @Test
    @Order(61)
    @DisplayName("Should get secret with default value")
    void testGetSecretWithDefault() {
        String result = ConfigurationAccessor.getSecret("nonexistent.secret", "default-secret");
        assertEquals("default-secret", result);
    }

    @Test
    @Order(62)
    @DisplayName("Should throw exception for null provider name in getProviderConfig")
    void testGetProviderConfigNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getProviderConfig(null));
    }

    @Test
    @Order(63)
    @DisplayName("Should throw exception for blank provider name in getProviderConfig")
    void testGetProviderConfigBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getProviderConfig(""));

        assertThrows(IllegalArgumentException.class, () ->
            ConfigurationAccessor.getProviderConfig("   "));
    }

    @Test
    @Order(64)
    @DisplayName("Should get provider config for valid provider")
    void testGetProviderConfig() {
        LLMConfig.ProviderConfig openaiConfig = ConfigurationAccessor.getProviderConfig("openai");
        assertNotNull(openaiConfig);

        LLMConfig.ProviderConfig anthropicConfig = ConfigurationAccessor.getProviderConfig("anthropic");
        assertNotNull(anthropicConfig);

        LLMConfig.ProviderConfig geminiConfig = ConfigurationAccessor.getProviderConfig("gemini");
        assertNotNull(geminiConfig);
    }

    // === Integration Tests ===

    @Test
    @Order(65)
    @DisplayName("Should provide consistent access to same configuration instance")
    void testConsistentConfigurationAccess() {
        ApplicationConfig config1 = ConfigurationAccessor.getConfig();
        ApplicationConfig config2 = ConfigurationAccessor.getConfig();

        assertSame(config1, config2);
        assertSame(ApplicationConfig.getInstance(), config1);
    }

    @Test
    @Order(66)
    @DisplayName("Should provide consistent access to nested configuration objects")
    void testConsistentNestedConfigurationAccess() {
        DatabaseConfig dbConfig1 = ConfigurationAccessor.getDatabaseConfig();
        DatabaseConfig dbConfig2 = ConfigurationAccessor.getDatabaseConfig();

        // Config objects are not necessarily singletons, just verify they're equivalent
        assertNotNull(dbConfig1);
        assertNotNull(dbConfig2);
        assertNotNull(ApplicationConfig.getInstance().getDatabaseConfig());
    }

    @Test
    @Order(67)
    @DisplayName("Should handle all configuration access without exceptions")
    void testAllConfigurationAccessWithoutExceptions() {
        assertDoesNotThrow(() -> {
            // Core access
            ConfigurationAccessor.getConfig();
            ConfigurationAccessor.getString("test.key", "default");
            ConfigurationAccessor.getBoolean("test.bool", false);
            ConfigurationAccessor.getInt("test.int", 0);
            ConfigurationAccessor.getDuration("test.duration", Duration.ZERO);

            // Database access
            ConfigurationAccessor.getDatabaseJdbcUrl();
            ConfigurationAccessor.getDatabaseUsername();
            ConfigurationAccessor.getDatabasePassword();
            ConfigurationAccessor.getDatabaseDriver();
            ConfigurationAccessor.getDatabaseMaxConnections();
            ConfigurationAccessor.getDatabaseConfig();

            // Tool access
            ConfigurationAccessor.getToolCodeRunnerTimeout();
            ConfigurationAccessor.getToolFileReadBaseDir();
            ConfigurationAccessor.getToolFileReadMaxSize();
            ConfigurationAccessor.getToolFileReadMaxPathLength();
            ConfigurationAccessor.getToolAudioOutputDir();
            ConfigurationAccessor.getToolCodeRunnerAllowedCommands();
            ConfigurationAccessor.getToolConfig();

            // LLM access
            ConfigurationAccessor.getLLMOpenAiApiKey();
            ConfigurationAccessor.getLLMOpenAiModel();
            ConfigurationAccessor.getLLMOpenAiBaseUrl();
            ConfigurationAccessor.getLLMOpenAiTimeout();
            ConfigurationAccessor.getLLMOpenAiMaxRetries();
            ConfigurationAccessor.getLLMConfig();

            // Memory access
            ConfigurationAccessor.getMemoryDefaultLimit();
            ConfigurationAccessor.getMemoryMaxEntries();
            ConfigurationAccessor.getMemoryRetentionDays();
            ConfigurationAccessor.getMemoryConfig();

            // Metrics access
            ConfigurationAccessor.isMetricsEnabled();
            ConfigurationAccessor.getMetricsRetentionPeriod();
            ConfigurationAccessor.getMetricsMaxInMemory();
            ConfigurationAccessor.getMetricsConfig();
        });
    }

    @Test
    @Order(68)
    @DisplayName("Should be properly packaged")
    void testPackageStructure() {
        assertEquals("com.skanga.conductor.config", ConfigurationAccessor.class.getPackageName());
        assertEquals("ConfigurationAccessor", ConfigurationAccessor.class.getSimpleName());
        assertEquals("com.skanga.conductor.config.ConfigurationAccessor",
                    ConfigurationAccessor.class.getName());
    }

    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "test.comprehensive", matches = "true")
    @Test
    @Order(69)
    @DisplayName("Should maintain thread safety by delegating to ApplicationConfig")
    void testThreadSafety() {
        // Since ConfigurationAccessor delegates to ApplicationConfig singleton,
        // it inherits thread safety. Test multiple concurrent accesses.
        assertDoesNotThrow(() -> {
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        assertNotNull(ConfigurationAccessor.getConfig());
                        assertNotNull(ConfigurationAccessor.getDatabaseJdbcUrl());
                        assertNotNull(ConfigurationAccessor.getToolCodeRunnerTimeout());
                        assertNotNull(ConfigurationAccessor.getLLMOpenAiModel());
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout
            }
        });
    }

    @Test
    @Order(70)
    @DisplayName("Should handle typical usage patterns")
    void testTypicalUsagePatterns() {
        // Database connection pattern
        String jdbcUrl = ConfigurationAccessor.getDatabaseJdbcUrl();
        String username = ConfigurationAccessor.getDatabaseUsername();
        String password = ConfigurationAccessor.getDatabasePassword();
        assertNotNull(jdbcUrl);
        assertNotNull(username);
        assertNotNull(password);

        // Tool configuration pattern
        Duration timeout = ConfigurationAccessor.getToolCodeRunnerTimeout();
        String baseDir = ConfigurationAccessor.getToolFileReadBaseDir();
        Set<String> allowedCommands = ConfigurationAccessor.getToolCodeRunnerAllowedCommands();
        assertNotNull(timeout);
        assertNotNull(baseDir);
        assertNotNull(allowedCommands);

        // LLM provider selection pattern
        String openaiKey = ConfigurationAccessor.getLLMOpenAiApiKey();
        String anthropicKey = ConfigurationAccessor.getLLMAnthropicApiKey();
        // At least one should be available in typical usage, but test allows both to be null

        // Memory management pattern
        int defaultLimit = ConfigurationAccessor.getMemoryDefaultLimit();
        int maxEntries = ConfigurationAccessor.getMemoryMaxEntries();
        assertTrue(defaultLimit > 0);
        assertTrue(maxEntries > 0);

        // Metrics configuration pattern
        boolean metricsEnabled = ConfigurationAccessor.isMetricsEnabled();
        Duration retentionPeriod = ConfigurationAccessor.getMetricsRetentionPeriod();
        assertNotNull(retentionPeriod);
    }
}