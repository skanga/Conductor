# Conductor Configuration Guide

This comprehensive guide covers all aspects of configuring the Conductor AI framework, from basic setup to advanced production patterns.

## Table of Contents

- [Basic Configuration](#basic-configuration)
- [Advanced Configuration Patterns](#advanced-configuration-patterns)
- [Environment-Specific Setup](#environment-specific-setup)
- [Security and Secret Management](#security-and-secret-management)
- [Performance Tuning](#performance-tuning)
- [Multi-Tenant Configuration](#multi-tenant-configuration)
- [Configuration Validation and Testing](#configuration-validation-and-testing)
- [Troubleshooting](#troubleshooting)

---

## Basic Configuration

### Overview

Conductor uses a layered configuration approach that prioritizes security and ease of deployment. Sensitive values like API keys are provided via environment variables, while non-sensitive configuration remains in properties files.

## Configuration Sources

Configuration is loaded in this priority order:

1. **Environment Variables** (highest priority)
2. **External configuration files** (via --config parameter)
3. **Profile-specific properties** (application-{profile}.properties)
4. **Base properties** (application.properties)
5. **System properties** (lowest priority)

## Sensitive Properties

### Security Approach

For security, **never store sensitive values in configuration files**. Instead, use environment variables:

| Property Key | Environment Variable | Description |
|--------------|---------------------|-------------|
| `conductor.llm.openai.api.key` | `CONDUCTOR_LLM_OPENAI_API_KEY` | OpenAI API key |
| `conductor.llm.anthropic.api.key` | `CONDUCTOR_LLM_ANTHROPIC_API_KEY` | Anthropic API key |
| `conductor.llm.google.api.key` | `CONDUCTOR_LLM_GOOGLE_API_KEY` | Google/Gemini API key |
| `conductor.database.password` | `CONDUCTOR_DATABASE_PASSWORD` | Database password |

### Setting Environment Variables

#### Linux/macOS:
```bash
export CONDUCTOR_LLM_OPENAI_API_KEY="your-openai-api-key"
export CONDUCTOR_LLM_ANTHROPIC_API_KEY="your-anthropic-api-key"
```

#### Windows:
```cmd
set CONDUCTOR_LLM_OPENAI_API_KEY=your-openai-api-key
set CONDUCTOR_LLM_ANTHROPIC_API_KEY=your-anthropic-api-key
```

#### Docker:
```bash
docker run -e CONDUCTOR_LLM_OPENAI_API_KEY="your-key" conductor-app
```

#### Docker Compose:
```yaml
services:
  conductor:
    image: conductor-app
    environment:
      - CONDUCTOR_LLM_OPENAI_API_KEY=${OPENAI_API_KEY}
      - CONDUCTOR_LLM_ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
```

## Configuration Validation

Use the built-in configuration tool to validate your setup:

```bash
# Validate configuration
java -cp conductor.jar com.skanga.conductor.config.ConfigurationTool validate

# Check configuration status
java -cp conductor.jar com.skanga.conductor.config.ConfigurationTool status
```

The tool will:
- ✅ Verify configuration files load correctly
- 🔍 Check for sensitive properties in environment variables vs. files
- ⚠️ Warn about potential security issues

## Common Configuration

### LLM Providers

Configure your preferred LLM provider:

```properties
# OpenAI
conductor.llm.openai.model=gpt-4
conductor.llm.openai.base.url=https://api.openai.com/v1
conductor.llm.openai.timeout=30s

# Anthropic
conductor.llm.anthropic.model=claude-3-5-sonnet-20241022
conductor.llm.anthropic.timeout=30s

# Google/Gemini
conductor.llm.gemini.model=gemini-pro
conductor.llm.gemini.timeout=30s
```

### Database

```properties
conductor.database.url=jdbc:h2:./data/conductor;FILE_LOCK=FS
conductor.database.username=sa
conductor.database.driver=org.h2.Driver
# Password via CONDUCTOR_DATABASE_PASSWORD environment variable
```

### Tools

```properties
# Code Runner Tool
conductor.tools.coderunner.timeout=5s
conductor.tools.coderunner.allowed.commands=echo,ls,pwd,date

# File Read Tool
conductor.tools.fileread.basedir=./data
conductor.tools.fileread.max.size.bytes=10485760
```

## Profile-Based Configuration

Use profiles for different environments:

```bash
# Development
java -Dconductor.profile=dev -jar conductor.jar

# Production
java -Dconductor.profile=prod -jar conductor.jar
```

Create environment-specific files:
- `application-dev.properties`
- `application-prod.properties`
- `application-test.properties`

## External Configuration

Override settings with external configuration files:

```bash
java --config=/path/to/prod.config -jar conductor.jar
```

External config files use the same format as application.properties but take precedence.

## Best Practices

### Security
- ✅ **Always** use environment variables for API keys and passwords
- ✅ Use external configuration files for deployment-specific settings
- ✅ Keep sensitive values out of version control
- ❌ Never commit API keys or passwords to git

### Deployment
- ✅ Use profile-specific configurations for different environments
- ✅ Validate configuration before deployment using the configuration tool
- ✅ Use secret management systems (AWS Secrets Manager, Azure Key Vault) in production
- ✅ Rotate API keys regularly

### Development
- ✅ Use `.env` files locally (not committed to git)
- ✅ Document required environment variables in README
- ✅ Provide default values for non-sensitive properties
- ✅ Use the configuration tool to debug setup issues

## Migration from Encrypted Properties

If you're migrating from the old encrypted property system:

1. **Remove encrypted values** from properties files
2. **Set environment variables** instead:
   ```bash
   # Old: conductor.llm.openai.api.key=ENC(encrypted-value)
   # New:
   export CONDUCTOR_LLM_OPENAI_API_KEY="your-actual-api-key"
   ```
3. **Remove encryption keys** - no longer needed
4. **Validate** with the configuration tool

## Troubleshooting

### Configuration Tool
```bash
# Check what's configured
java -cp conductor.jar com.skanga.conductor.config.ConfigurationTool status

# Validate all configuration
java -cp conductor.jar com.skanga.conductor.config.ConfigurationTool validate
```

### Common Issues

**Issue**: "Required sensitive property not found"
**Solution**: Set the appropriate environment variable:
```bash
export CONDUCTOR_LLM_OPENAI_API_KEY="your-key"
```

**Issue**: Configuration not loading
**Solution**: Check file paths and permissions, validate with configuration tool

**Issue**: Wrong configuration values
**Solution**: Check configuration priority order - environment variables override properties files

### Debug Mode
Enable verbose logging to see configuration loading:
```bash
java -Dverbose=true -jar conductor.jar
```

---
# Legacy Configuration Management

This document describes the configuration system implemented in the Conductor framework.

## Overview

The Conductor framework uses a hierarchical configuration system that supports:
- Properties files (application.properties)
- Environment-specific properties (application-dev.properties, application-prod.properties)
- System properties (-Dconductor.property.name=value)
- Environment variables (CONDUCTOR_PROPERTY_NAME=value)

Configuration precedence (highest to lowest):
1. Environment variables
2. System properties
3. Environment-specific properties files
4. Default application.properties
5. Hardcoded defaults

## Configuration Categories

### Database Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `conductor.database.url` | `jdbc:h2:./data/subagentsdb;FILE_LOCK=FS` | JDBC URL |
| `conductor.database.username` | `sa` | Database username |
| `conductor.database.password` | `` | Database password |
| `conductor.database.driver` | `org.h2.Driver` | JDBC driver class |
| `conductor.database.max.connections` | `10` | Maximum connection pool size |

### Tool Configuration

#### Code Runner Tool
| Property | Default | Description |
|----------|---------|-------------|
| `conductor.tools.coderunner.timeout` | `5s` | Command execution timeout |
| `conductor.tools.coderunner.allowed.commands` | `echo,ls,pwd,date,whoami` | Comma-separated allowed commands |

#### File Read Tool
| Property | Default | Description |
|----------|---------|-------------|
| `conductor.tools.fileread.basedir` | `./sample_data` | Base directory for file reading |
| `conductor.tools.fileread.allow.symlinks` | `false` | Allow reading symbolic links |
| `conductor.tools.fileread.max.size.bytes` | `10485760` | Maximum file size (10MB) |
| `conductor.tools.fileread.max.path.length` | `260` | Maximum path length |

#### Audio Tool
| Property | Default | Description |
|----------|---------|-------------|
| `conductor.tools.audio.output.dir` | `./out_audio` | Output directory for audio files |

### LLM Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `conductor.llm.openai.api.key` | `` | OpenAI API key |
| `conductor.llm.openai.model` | `gpt-3.5-turbo` | OpenAI model name |
| `conductor.llm.openai.base.url` | `https://api.openai.com/v1` | OpenAI base URL |
| `conductor.llm.openai.timeout` | `30s` | OpenAI API timeout |
| `conductor.llm.openai.max.retries` | `3` | Maximum retry attempts |

### Memory Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `conductor.memory.default.limit` | `10` | Default memory entries shown in prompts |
| `conductor.memory.max.entries` | `1000` | Maximum memory entries to load |
| `conductor.memory.retention.days` | `30` | Number of days to retain memory entries |
| `conductor.memory.compression.enabled` | `false` | Enable memory compression |

#### Memory Manager Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `conductor.memory.threshold.warning` | `0.75` | Warning threshold (0.0-1.0) for memory usage |
| `conductor.memory.threshold.critical` | `0.85` | Critical threshold (0.0-1.0) for memory usage |
| `conductor.memory.threshold.emergency` | `0.95` | Emergency threshold (0.0-1.0) for memory usage |
| `conductor.memory.monitoring.interval.seconds` | `30` | Interval for memory monitoring in seconds |
| `conductor.memory.cleanup.interval.minutes` | `5` | Interval for memory cleanup in minutes |
| `conductor.memory.resource.expiry.hours` | `1` | Time in hours before resources expire |
| `conductor.memory.threadpool.size` | `2` | Thread pool size for memory manager (1-10) |

### Logging Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `conductor.logging.max.file.size` | `10MB` | Maximum log file size |
| `conductor.logging.max.history` | `30` | Number of days to keep logs |
| `conductor.logging.total.size.cap` | `300MB` | Total log storage limit |
| `conductor.logging.async.queue.size` | `512` | Async logging queue size |

## Environment-Specific Configurations

### Development Environment (`application-dev.properties`)
- Uses in-memory H2 database
- More permissive tool settings
- Verbose logging
- Higher memory limits

### Production Environment (`application-prod.properties`)
- Persistent database with performance tuning
- Restrictive security settings
- Optimized logging
- Conservative memory limits

## Usage Examples

### Using Environment Variables
```bash
export CONDUCTOR_DATABASE_URL="jdbc:postgresql://localhost:5432/conductor"
export CONDUCTOR_DATABASE_USERNAME="conductor_user"
export CONDUCTOR_DATABASE_PASSWORD="secure_password"
export CONDUCTOR_TOOLS_CODERUNNER_TIMEOUT="3s"
java -jar conductor.jar
```

### Using System Properties
```bash
java -Dconductor.database.url="jdbc:h2:./data/prod_db" \
     -Dconductor.memory.default.limit=20 \
     -Dconductor.tools.fileread.max.size.bytes=52428800 \
     -jar conductor.jar
```

### Using Configuration Files
Create `application-custom.properties`:
```properties
# Database
conductor.database.url=jdbc:h2:./data/custom_db;FILE_LOCK=FS

# Tools
conductor.tools.coderunner.allowed.commands=echo,pwd,ls,cat,grep

# Memory Store
conductor.memory.default.limit=15

# Memory Manager - Automatic memory monitoring and cleanup
conductor.memory.threshold.warning=0.70
conductor.memory.threshold.critical=0.80
conductor.memory.threshold.emergency=0.90
conductor.memory.monitoring.interval.seconds=60
conductor.memory.cleanup.interval.minutes=10
conductor.memory.resource.expiry.hours=2
conductor.memory.threadpool.size=3
```

## Implementation Details

### Configuration Class Structure
- `ApplicationConfig`: Main configuration singleton
- `DatabaseConfig`: Database-specific settings
- `ToolConfig`: Tool-specific settings
- `LLMConfig`: LLM provider settings
- `MemoryConfig`: Memory management settings

### Data Type Support
- **String**: Direct property values
- **Integer/Long**: Numeric parsing with fallback
- **Boolean**: Standard boolean parsing
- **Duration**: Supports `5s`, `30m`, `1h`, `500ms` formats
- **Set<String>**: Comma-separated values

### Error Handling
- Invalid values fall back to defaults with warnings
- Missing configuration files are ignored silently
- Malformed properties log warnings but don't fail startup

## Migration from Hardcoded Values

All previously hardcoded values have been externalized:
- Database connection strings → `conductor.database.*`
- Tool timeouts and limits → `conductor.tools.*`
- Memory limits → `conductor.memory.*`
- File size limits → `conductor.tools.fileread.*`
- Command timeouts → `conductor.tools.coderunner.*`

Classes maintain backward compatibility with deprecated constructors.

---

## Advanced Configuration Patterns

### Configuration Architecture Patterns

#### Layered Configuration Strategy

Configuration hierarchy from highest to lowest priority:
```
┌─────────────────────────┐  Highest Priority
│   Environment Variables │  (Runtime secrets)
├─────────────────────────┤
│   External Config Files │  (Deployment-specific)
├─────────────────────────┤
│   Profile Properties    │  (Environment-specific)
├─────────────────────────┤
│   Application Properties│  (Application defaults)
└─────────────────────────┘  Lowest Priority
```

#### Configuration Factory Pattern

```java
@Component
public class ConfigurationFactory {

    public <T> T createConfiguration(String profile, Class<T> configClass) {
        try {
            ConfigurationBuilder<T> builder = ConfigurationBuilder.forClass(configClass);
            applyProfileOverrides(builder, profile);
            applyEnvironmentOverrides(builder);
            T config = builder.build();
            validateConfiguration(config);
            return config;
        } catch (Exception e) {
            throw new ConfigurationException("Failed to create configuration for " + configClass.getName(), e);
        }
    }
}
```

#### Template-Based Configuration

```yaml
# templates/conductor-config.template.yml
conductor:
  database:
    url: "jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:conductor}"
    username: "${DB_USERNAME:conductor}"
    password: "${DB_PASSWORD}"
    pool:
      maximum-pool-size: ${DB_POOL_MAX:${CALCULATED_POOL_SIZE}}

  llm:
    providers:
      {{#each LLM_PROVIDERS}}
      - name: "{{name}}"
        type: "{{type}}"
        api-key: "${{{env_var}}}"
        model: "{{default_model}}"
      {{/each}}
```

---

## Environment-Specific Setup

### Development Environment Configuration

```yaml
# config/application-dev.yml
conductor:
  database:
    url: "jdbc:h2:mem:conductor-dev;DB_CLOSE_DELAY=-1"
    username: "sa"
    password: ""

  llm:
    provider: "mock"
    mock:
      enabled: true
      response-delay: 100ms
      failure-rate: 0.05  # 5% simulated failures

  logging:
    level:
      com.skanga.conductor: DEBUG
    file:
      name: "logs/conductor-dev.log"

  tools:
    validation:
      strict: false  # Allow experimental tools in dev
    security:
      sandbox: true  # Enable sandboxing for safety
```

### Staging Environment Configuration

```yaml
# config/application-staging.yml
conductor:
  database:
    url: "${STAGING_DATABASE_URL}"
    username: "${STAGING_DB_USER}"
    password: "${STAGING_DB_PASSWORD}"
    pool:
      maximum-pool-size: 10  # Smaller pool for staging

  llm:
    providers:
      - name: "openai-staging"
        type: "openai"
        api-key: "${OPENAI_STAGING_API_KEY}"
        model: "gpt-3.5-turbo"  # Cost-effective model
        rate-limit: 50

  monitoring:
    enabled: true
    metrics:
      export:
        prometheus:
          enabled: true
          step: PT1M
```

### Production Environment Configuration

```yaml
# config/application-production.yml
conductor:
  database:
    url: "${PRODUCTION_DATABASE_URL}"
    username: "${PRODUCTION_DB_USER}"
    password: "${PRODUCTION_DB_PASSWORD}"
    pool:
      maximum-pool-size: 50
      minimum-idle: 20
      connection-timeout: 20000
      idle-timeout: 300000

  llm:
    providers:
      - name: "openai-primary"
        type: "openai"
        api-key: "${OPENAI_PRODUCTION_API_KEY}"
        model: "gpt-4"
        timeout: 60000
        max-retries: 5
        rate-limit: 1000

      - name: "anthropic-fallback"
        type: "anthropic"
        api-key: "${ANTHROPIC_PRODUCTION_API_KEY}"
        model: "claude-3-sonnet-20240229"

    failover:
      enabled: true
      strategy: "round-robin"

  security:
    enabled: true
    rate-limiting:
      enabled: true
      requests-per-minute: 1000
    api-key-auth:
      required: true
```

---

## Security and Secret Management

### HashiCorp Vault Integration

```java
@Configuration
@EnableVaultRepositories
public class VaultConfiguration extends AbstractVaultConfiguration {

    @Override
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.create(
            vaultProperties.getHost(),
            vaultProperties.getPort()
        );
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(vaultProperties.getToken());
    }

    @Bean
    public VaultPropertySource vaultPropertySource() {
        VaultOperations vaultOperations = vaultTemplate();
        return VaultPropertySource.builder()
            .vaultOperations(vaultOperations)
            .path("secret/conductor")
            .propertyNamePrefix("conductor.")
            .build();
    }
}
```

### Database Encryption Configuration

```java
@Configuration
public class EncryptionConfiguration {

    @Bean
    @ConditionalOnProperty("conductor.security.database.encryption.enabled")
    public EncryptorService encryptorService() {
        String encryptionKey = System.getenv("CONDUCTOR_ENCRYPTION_KEY");
        if (encryptionKey == null || encryptionKey.length() < 32) {
            throw new IllegalStateException("Encryption key must be at least 32 characters");
        }
        return new AESEncryptorService(encryptionKey);
    }

    @Bean
    @ConditionalOnBean(EncryptorService.class)
    public EncryptedMemoryStore encryptedMemoryStore(
            MemoryStore delegate, EncryptorService encryptor) {
        return new EncryptedMemoryStore(delegate, encryptor);
    }
}
```

---

## Performance Tuning

### Connection Pool Optimization

```yaml
conductor:
  database:
    hikari:
      # Connection pool sizing
      maximum-pool-size: ${DB_POOL_MAX:50}
      minimum-idle: ${DB_POOL_MIN:10}

      # Connection timing
      connection-timeout: 20000        # 20 seconds
      idle-timeout: 300000            # 5 minutes
      max-lifetime: 1800000           # 30 minutes

      # Leak detection
      leak-detection-threshold: 60000  # 1 minute

      # Performance optimizations
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        useLocalSessionState: true
        rewriteBatchedStatements: true
```

### Multi-Level Caching Setup

```java
@Configuration
@EnableCaching
public class CachingConfiguration {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new CompositeCacheManager(
            l1CacheManager(),  // Local in-memory cache
            l2CacheManager()   // Distributed Redis cache
        );
    }

    @Bean
    public CacheManager l1CacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats());
        return cacheManager;
    }

    @Bean
    public CacheManager l2CacheManager() {
        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration())
            .build();
    }
}
```

---

## Multi-Tenant Configuration

### Tenant-Aware Configuration System

```java
@Service
public class MultiTenantConfigurationManager {

    private final TenantConfigurationResolver resolver;
    private final Map<String, TenantConfiguration> configCache = new ConcurrentHashMap<>();

    public TenantConfiguration getConfiguration(String tenantId) {
        return configCache.computeIfAbsent(tenantId, this::loadTenantConfiguration);
    }

    private TenantConfiguration loadTenantConfiguration(String tenantId) {
        try {
            TenantConfiguration baseConfig = loadBaseConfiguration();
            TenantConfiguration tenantOverrides = resolver.resolve(tenantId);
            return mergeConfigurations(baseConfig, tenantOverrides);
        } catch (Exception e) {
            logger.error("Failed to load configuration for tenant: {}", tenantId, e);
            return getDefaultConfiguration();
        }
    }

    @EventListener
    public void onTenantConfigurationChange(TenantConfigurationChangeEvent event) {
        configCache.remove(event.getTenantId());
        applicationEventPublisher.publishEvent(
            new ConfigurationRefreshEvent(event.getTenantId()));
    }
}
```

### Tenant Resource Configuration

```yaml
# config/tenant-template.yml
tenant:
  id: "${TENANT_ID}"
  name: "${TENANT_NAME}"

  database:
    schema: "tenant_${TENANT_ID}"
    pool:
      maximum-size: ${TENANT_DB_POOL_SIZE:10}

  storage:
    bucket: "conductor-tenant-${TENANT_ID}"
    encryption:
      enabled: true
      key-id: "tenant-${TENANT_ID}-key"

  llm:
    quota:
      daily-tokens: ${TENANT_LLM_QUOTA:100000}
      rate-limit: ${TENANT_RATE_LIMIT:50}

  workflow:
    max-concurrent: ${TENANT_MAX_CONCURRENT:5}
    timeout: "${TENANT_TIMEOUT:PT10M}"
```

---

## Configuration Validation and Testing

### Configuration Validation Framework

```java
@Component
public class ConfigurationValidator {

    private final List<ConfigurationRule> validationRules;

    public ConfigurationValidationResult validate(Configuration config) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        for (ConfigurationRule rule : validationRules) {
            try {
                ValidationResult result = rule.validate(config);
                errors.addAll(result.getErrors());
                warnings.addAll(result.getWarnings());
            } catch (Exception e) {
                errors.add(new ValidationError(
                    rule.getName(),
                    "Validation rule failed: " + e.getMessage()
                ));
            }
        }

        return new ConfigurationValidationResult(errors, warnings);
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        Configuration config = getCurrentConfiguration();
        ConfigurationValidationResult result = validate(config);

        if (!result.getErrors().isEmpty()) {
            logger.error("Configuration validation failed: {}", result.getErrors());
            throw new ConfigurationValidationException("Configuration validation failed", result);
        }

        if (!result.getWarnings().isEmpty()) {
            logger.warn("Configuration validation warnings: {}", result.getWarnings());
        }

        logger.info("Configuration validation successful");
    }
}
```

### Configuration Integration Tests

```java
@SpringBootTest
@TestPropertySource(properties = {
    "conductor.database.url=jdbc:h2:mem:config-test",
    "conductor.llm.provider=mock"
})
class ConfigurationIntegrationTest {

    @Test
    @DisplayName("Should validate production-like configuration")
    void testProductionConfiguration() {
        Configuration prodConfig = loadConfigurationFromProfile("production");
        ConfigurationValidationResult result = validator.validate(prodConfig);

        assertThat(result.getErrors())
            .filteredOn(error -> error.getSeverity() == Severity.CRITICAL)
            .isEmpty();
    }

    @Test
    @DisplayName("Should support environment variable substitution")
    void testEnvironmentVariableSubstitution() {
        System.setProperty("TEST_DB_URL", "jdbc:h2:mem:test");
        System.setProperty("TEST_API_KEY", "test-key-123");

        try {
            String configYaml = """
                conductor:
                  database:
                    url: ${TEST_DB_URL}
                  llm:
                    api-key: ${TEST_API_KEY}
                """;

            Configuration config = parseConfiguration(configYaml);

            assertThat(config.getDatabase().getUrl()).isEqualTo("jdbc:h2:mem:test");
            assertThat(config.getLlm().getApiKey()).isEqualTo("test-key-123");

        } finally {
            System.clearProperty("TEST_DB_URL");
            System.clearProperty("TEST_API_KEY");
        }
    }
}
```

---

## Troubleshooting

### Configuration Health Check

```java
@RestController
@RequestMapping("/admin/config")
public class ConfigurationDiagnosticsController {

    @GetMapping("/health")
    public ResponseEntity<ConfigurationHealthReport> getConfigurationHealth() {
        ConfigurationHealthReport report = ConfigurationHealthReport.builder()
            .timestamp(Instant.now())
            .overallStatus(determineOverallHealth())
            .databaseHealth(checkDatabaseConfiguration())
            .llmProviderHealth(checkLLMProviderConfiguration())
            .cacheHealth(checkCacheConfiguration())
            .securityHealth(checkSecurityConfiguration())
            .build();

        HttpStatus status = report.getOverallStatus() == HealthStatus.UP
            ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(report);
    }

    @GetMapping("/properties")
    public ResponseEntity<Map<String, Object>> getResolvedProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        Environment env = applicationContext.getEnvironment();

        for (PropertySource<?> propertySource : ((AbstractEnvironment) env).getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) propertySource;
                for (String propertyName : enumerable.getPropertyNames()) {
                    if (!isSensitiveProperty(propertyName)) {
                        properties.put(propertyName, env.getProperty(propertyName));
                    }
                }
            }
        }

        return ResponseEntity.ok(properties);
    }
}
```

### Configuration Problem Detection

```java
@Component
public class ConfigurationProblemDetector {

    @EventListener
    public void onApplicationFailed(ApplicationFailedEvent event) {
        Throwable cause = event.getException();

        if (cause instanceof BeanCreationException) {
            detectConfigurationProblems((BeanCreationException) cause);
        } else if (cause instanceof DataSourceInitializationException) {
            detectDatabaseConfigurationProblems((DataSourceInitializationException) cause);
        }
    }

    private void detectConfigurationProblems(BeanCreationException exception) {
        String message = exception.getMessage();

        if (message.contains("Could not resolve placeholder")) {
            String missingProperty = extractMissingProperty(message);
            logger.error("""
                Configuration Problem Detected: Missing Property

                Missing property: {}

                Possible solutions:
                1. Set environment variable: {}
                2. Add to application.properties: {}=your-value
                3. Pass as system property: -D{}=your-value
                """, missingProperty,
                propertyToEnvVar(missingProperty),
                missingProperty,
                missingProperty);
        }
    }
}
```

### Common Configuration Issues and Solutions

| Issue | Symptoms | Solution |
|-------|----------|----------|
| **Missing API Key** | `Required sensitive property not found` | Set environment variable: `export CONDUCTOR_LLM_OPENAI_API_KEY="your-key"` |
| **Database Connection Failed** | `Cannot connect to database` | Verify URL, credentials, and database server status |
| **Configuration Not Loading** | Application uses defaults | Check file paths, permissions, and configuration precedence |
| **Property Override Not Working** | Values not applying | Check configuration hierarchy - env vars override properties |
| **Memory Issues** | OutOfMemoryError | Increase JVM heap: `export JAVA_OPTS="-Xmx4g"` |

### Debug Configuration Loading

Enable verbose logging to see configuration loading process:

```bash
# Debug mode
java -Dlogging.level.org.springframework.core.env=DEBUG -jar conductor.jar

# Configuration-specific debug
java -Dlogging.level.com.skanga.conductor.config=TRACE -jar conductor.jar

# Verbose output
java -Dverbose=true -Dconductor.config.debug=true -jar conductor.jar
```

This comprehensive configuration guide covers everything from basic setup to advanced enterprise patterns, providing complete configuration management for the Conductor framework.
