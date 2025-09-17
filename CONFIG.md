# Configuration Management

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
| `conductor.memory.compression.enabled` | `false` | Enable memory compression |

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
conductor.database.url=jdbc:h2:./data/custom_db;FILE_LOCK=FS
conductor.tools.coderunner.allowed.commands=echo,pwd,ls,cat,grep
conductor.memory.default.limit=15
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