package com.skanga.conductor.testbase;

import com.skanga.conductor.config.ApplicationConfig;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

/**
 * Base test class for configuration-related tests providing configuration utilities and isolation.
 * <p>
 * This class extends ConductorTestBase and adds configuration-specific functionality including:
 * </p>
 * <ul>
 * <li>System property management and isolation</li>
 * <li>Configuration reset and restoration</li>
 * <li>Test-specific property setting utilities</li>
 * <li>Configuration validation helpers</li>
 * <li>Profile-based testing support</li>
 * </ul>
 *
 * @since 2.0.0
 */
public abstract class ConfigTestBase extends ConductorTestBase {

    private final Map<String, String> originalSystemProperties = new HashMap<>();

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();

        // Backup original system properties that might be modified by tests
        backupSystemProperties();

        logger.debug("Configuration test setup complete");
    }

    @Override
    protected void doTearDown() throws Exception {
        try {
            // Restore original system properties
            restoreSystemProperties();
        } finally {
            super.doTearDown();
        }
    }

    /**
     * Sets a system property for the duration of the test.
     * The original value will be restored after the test.
     *
     * @param key the property key
     * @param value the property value
     */
    protected void setTestProperty(String key, String value) {
        // Backup original value if not already backed up
        if (!originalSystemProperties.containsKey(key)) {
            String originalValue = System.getProperty(key);
            originalSystemProperties.put(key, originalValue);
        }

        // Set the new value
        System.setProperty(key, value);
        logger.debug("Set test property: {} = {}", key, value);
    }

    /**
     * Sets multiple system properties for the duration of the test.
     *
     * @param properties the properties to set
     */
    protected void setTestProperties(Properties properties) {
        properties.forEach((key, value) -> setTestProperty(key.toString(), value.toString()));
    }

    /**
     * Sets multiple system properties from a map.
     *
     * @param properties the properties to set
     */
    protected void setTestProperties(Map<String, String> properties) {
        properties.forEach(this::setTestProperty);
    }

    /**
     * Clears a system property for the duration of the test.
     *
     * @param key the property key to clear
     */
    protected void clearTestProperty(String key) {
        // Backup original value if not already backed up
        if (!originalSystemProperties.containsKey(key)) {
            String originalValue = System.getProperty(key);
            originalSystemProperties.put(key, originalValue);
        }

        // Clear the property
        System.clearProperty(key);
        logger.debug("Cleared test property: {}", key);
    }

    /**
     * Creates a test configuration with database isolation.
     *
     * @return a fresh ApplicationConfig instance with test properties
     */
    protected ApplicationConfig createTestConfig() {
        // Reset singleton to pick up new properties
        ApplicationConfig.resetInstance();
        return ApplicationConfig.getInstance();
    }

    /**
     * Creates a test configuration with custom properties.
     *
     * @param testProperties the custom properties to set
     * @return a fresh ApplicationConfig instance with test properties
     */
    protected ApplicationConfig createTestConfig(Map<String, String> testProperties) {
        setTestProperties(testProperties);
        return createTestConfig();
    }

    /**
     * Creates a test configuration with a specific database URL.
     *
     * @param databaseUrl the database URL to use
     * @return a fresh ApplicationConfig instance with test database
     */
    protected ApplicationConfig createTestConfigWithDatabase(String databaseUrl) {
        setTestProperty("conductor.database.url", databaseUrl);
        return createTestConfig();
    }

    /**
     * Creates properties for an in-memory database configuration.
     *
     * @param dbName the database name
     * @return properties map for in-memory database
     */
    protected Map<String, String> createInMemoryDatabaseProperties(String dbName) {
        Map<String, String> properties = new HashMap<>();
        properties.put("conductor.database.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        properties.put("conductor.database.user", "sa");
        properties.put("conductor.database.password", "");
        return properties;
    }

    /**
     * Creates properties for testing invalid configurations.
     *
     * @return properties map with invalid configuration values
     */
    protected Map<String, String> createInvalidConfigProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("conductor.database.url", "invalid-url");
        properties.put("conductor.database.max.connections", "-1");
        properties.put("conductor.tools.coderunner.timeout", "invalid-duration");
        return properties;
    }

    /**
     * Asserts that a configuration property has the expected value.
     *
     * @param config the configuration to check
     * @param propertyPath the property path (dot-separated)
     * @param expectedValue the expected value
     */
    protected void assertConfigProperty(ApplicationConfig config, String propertyPath, Object expectedValue) {
        Object actualValue = getConfigPropertyValue(config, propertyPath);
        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError(String.format(
                "Configuration property '%s' expected: <%s> but was: <%s>",
                propertyPath, expectedValue, actualValue));
        }
    }

    /**
     * Asserts that creating a configuration with the given properties throws an exception.
     *
     * @param testProperties the properties to set
     * @param expectedExceptionType the expected exception type
     * @return the thrown exception
     */
    protected <T extends Throwable> T assertConfigCreationThrows(Map<String, String> testProperties,
                                                                Class<T> expectedExceptionType) {
        setTestProperties(testProperties);
        return assertThrowsType(expectedExceptionType, () -> {
            ApplicationConfig.resetInstance();
            ApplicationConfig.getInstance();
        });
    }

    /**
     * Tests configuration with a specific profile.
     *
     * @param profile the profile name
     * @param testAction the test action to perform with the profile
     */
    protected void withProfile(String profile, Runnable testAction) {
        setTestProperty("conductor.profile", profile);
        try {
            testAction.run();
        } finally {
            // Profile cleanup is handled by property restoration
        }
    }

    /**
     * Gets a configuration property value using reflection or direct access.
     * This is a helper method for configuration validation.
     *
     * @param config the configuration instance
     * @param propertyPath the property path
     * @return the property value
     */
    private Object getConfigPropertyValue(ApplicationConfig config, String propertyPath) {
        // This is a simplified implementation - in practice, you'd use reflection
        // or add getter methods to access nested configuration properties
        try {
            String[] parts = propertyPath.split("\\.");
            if (parts.length == 2) {
                String section = parts[0];
                String property = parts[1];

                switch (section) {
                    case "database":
                        var dbConfig = config.getDatabaseConfig();
                        switch (property) {
                            case "jdbcUrl": return dbConfig.getJdbcUrl();
                            case "maxConnections": return dbConfig.getMaxConnections();
                            default: break;
                        }
                        break;
                    case "llm":
                        var llmConfig = config.getLLMConfig();
                        switch (property) {
                            case "openAiModel": return llmConfig.getOpenAiModel();
                            default: break;
                        }
                        break;
                    case "memory":
                        var memoryConfig = config.getMemoryConfig();
                        switch (property) {
                            case "defaultMemoryLimit": return memoryConfig.getDefaultMemoryLimit();
                            default: break;
                        }
                        break;
                    default: break;
                }
            }
            throw new IllegalArgumentException("Unsupported property path: " + propertyPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get config property: " + propertyPath, e);
        }
    }

    /**
     * Backs up system properties that might be modified by tests.
     */
    private void backupSystemProperties() {
        // Common properties that tests might modify
        String[] conductorProperties = {
            "conductor.database.url",
            "conductor.database.user",
            "conductor.database.password",
            "conductor.database.max.connections",
            "conductor.profile",
            "conductor.tools.coderunner.timeout",
            "conductor.memory.default.limit"
        };

        for (String property : conductorProperties) {
            String value = System.getProperty(property);
            originalSystemProperties.put(property, value);
        }
    }

    /**
     * Restores original system properties.
     */
    private void restoreSystemProperties() {
        for (Map.Entry<String, String> entry : originalSystemProperties.entrySet()) {
            String key = entry.getKey();
            String originalValue = entry.getValue();

            if (originalValue == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, originalValue);
            }
        }
        originalSystemProperties.clear();
    }
}