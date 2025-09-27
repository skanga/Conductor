# Conductor Framework - Deployment and Production Guide

This guide provides comprehensive deployment strategies, production configurations, and operational best practices for the Conductor AI framework.

## Table of Contents

- [Deployment Strategies](#deployment-strategies)
- [Production Configuration](#production-configuration)
- [Infrastructure Setup](#infrastructure-setup)
- [Monitoring and Observability](#monitoring-and-observability)
- [Security Hardening](#security-hardening)
- [Performance Tuning](#performance-tuning)
- [Disaster Recovery](#disaster-recovery)
- [Operational Runbooks](#operational-runbooks)

---

## Deployment Strategies

### 1. Containerized Deployment (Recommended)

#### Docker Configuration
```dockerfile
# Dockerfile
FROM openjdk:21-jdk-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN useradd -r -s /bin/false conductor

# Set working directory
WORKDIR /app

# Copy application
COPY target/conductor-*.jar app.jar
COPY config/ config/
COPY scripts/ scripts/

# Set permissions
RUN chown -R conductor:conductor /app
RUN chmod +x scripts/*.sh

# Switch to non-root user
USER conductor

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Expose port
EXPOSE 8080

# JVM options for production
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose for Development
```yaml
# docker-compose.yml
version: '3.8'

services:
  conductor-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - CONDUCTOR_DATABASE_URL=jdbc:postgresql://postgres:5432/conductor
      - CONDUCTOR_REDIS_URL=redis://redis:6379
      - CONDUCTOR_LLM_API_KEY=${OPENAI_API_KEY}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./logs:/app/logs
      - ./output:/app/output
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: conductor
      POSTGRES_USER: conductor
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U conductor"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/ssl:/etc/nginx/ssl
    depends_on:
      - conductor-app

volumes:
  postgres_data:
  redis_data:
```

### 2. Kubernetes Deployment

#### Deployment Configuration
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: conductor-app
  labels:
    app: conductor
    version: v1.0.0
spec:
  replicas: 3
  selector:
    matchLabels:
      app: conductor
  template:
    metadata:
      labels:
        app: conductor
        version: v1.0.0
    spec:
      containers:
      - name: conductor
        image: conductor:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: CONDUCTOR_DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: conductor-secrets
              key: database-url
        - name: CONDUCTOR_LLM_API_KEY
          valueFrom:
            secretKeyRef:
              name: conductor-secrets
              key: llm-api-key
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
        - name: logs-volume
          mountPath: /app/logs
      volumes:
      - name: config-volume
        configMap:
          name: conductor-config
      - name: logs-volume
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: conductor-service
spec:
  selector:
    app: conductor
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: conductor-ingress
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  tls:
  - hosts:
    - conductor.example.com
    secretName: conductor-tls
  rules:
  - host: conductor.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: conductor-service
            port:
              number: 80
```

#### Configuration Management
```yaml
# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: conductor-config
data:
  application.yml: |
    conductor:
      database:
        pool:
          maximum-pool-size: 20
          minimum-idle: 5
      llm:
        timeout: 30000
        max-retries: 3
      workflow:
        max-concurrent-stages: 10
        stage-timeout: 300000
      security:
        enabled: true
        rate-limit: 1000

    logging:
      level:
        com.skanga.conductor: INFO
        root: WARN
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
---
apiVersion: v1
kind: Secret
metadata:
  name: conductor-secrets
type: Opaque
data:
  database-url: <base64-encoded-url>
  llm-api-key: <base64-encoded-key>
```

### 3. Cloud-Native Deployment

#### AWS ECS Configuration
```json
{
  "family": "conductor-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::account:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::account:role/conductorTaskRole",
  "containerDefinitions": [
    {
      "name": "conductor-app",
      "image": "your-ecr-repo/conductor:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "essential": true,
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "aws"
        }
      ],
      "secrets": [
        {
          "name": "CONDUCTOR_DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:conductor/database-url"
        },
        {
          "name": "CONDUCTOR_LLM_API_KEY",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:conductor/llm-api-key"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/conductor",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

---

## Production Configuration

### 1. Application Configuration

#### Production Properties
```properties
# application-production.properties

# Server Configuration
server.port=8080
server.tomcat.max-threads=200
server.tomcat.min-spare-threads=50

# Database Configuration
conductor.database.url=jdbc:postgresql://prod-db:5432/conductor
conductor.database.username=${DB_USERNAME}
conductor.database.password=${DB_PASSWORD}
conductor.database.pool.maximum-pool-size=50
conductor.database.pool.minimum-idle=10
conductor.database.pool.connection-timeout=20000
conductor.database.pool.idle-timeout=300000
conductor.database.pool.max-lifetime=1800000

# Redis Configuration (for caching)
conductor.redis.url=${REDIS_URL}
conductor.redis.timeout=5000
conductor.redis.pool.max-active=50
conductor.redis.pool.max-idle=20

# LLM Provider Configuration
conductor.llm.provider=openai
conductor.llm.api.key=${OPENAI_API_KEY}
conductor.llm.timeout=60000
conductor.llm.max-retries=5
conductor.llm.rate-limit=100

# Security Configuration
conductor.security.enabled=true
conductor.security.api-key-required=true
conductor.security.rate-limit=1000
conductor.security.cors.enabled=true
conductor.security.cors.allowed-origins=https://conductor.example.com

# Monitoring Configuration
conductor.metrics.enabled=true
conductor.metrics.export.prometheus.enabled=true
conductor.health.disk-space.threshold=1GB

# Logging Configuration
logging.level.com.skanga.conductor=INFO
logging.level.org.springframework.security=WARN
logging.level.org.hibernate.SQL=WARN
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.file.name=logs/conductor.log
logging.logback.rollingpolicy.max-file-size=100MB
logging.logback.rollingpolicy.max-history=30

# JVM Configuration
conductor.jvm.heap.initial=1g
conductor.jvm.heap.maximum=4g
conductor.jvm.gc=G1GC
```

#### Environment-Specific Configurations
```yaml
# config/application-aws.yml
conductor:
  database:
    url: ${RDS_ENDPOINT}
    ssl: true
  storage:
    type: s3
    bucket: ${S3_BUCKET}
  monitoring:
    cloudwatch:
      enabled: true
      namespace: Conductor/Production

---
# config/application-gcp.yml
conductor:
  database:
    url: ${CLOUD_SQL_CONNECTION_STRING}
  storage:
    type: gcs
    bucket: ${GCS_BUCKET}
  monitoring:
    stackdriver:
      enabled: true
      project-id: ${GCP_PROJECT_ID}
```

### 2. JVM Tuning

#### Production JVM Arguments
```bash
# scripts/start-production.sh
#!/bin/bash

JAVA_OPTS="
-Xms2g
-Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/logs/heapdump.hprof
-XX:+ExitOnOutOfMemoryError
-Dfile.encoding=UTF-8
-Djava.security.egd=file:/dev/./urandom
-Djava.awt.headless=true
"

# GC Logging
GC_OPTS="
-Xlog:gc*:logs/gc.log:time,tags
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M
"

# Security Options
SECURITY_OPTS="
-Djava.security.properties=config/security.properties
-Dnetworkaddress.cache.ttl=60
"

# Start application
exec java $JAVA_OPTS $GC_OPTS $SECURITY_OPTS -jar conductor.jar
```

### 3. Database Configuration

#### Production Database Schema
```sql
-- scripts/production-schema.sql
CREATE DATABASE conductor_prod;

\c conductor_prod;

-- Create optimized indexes
CREATE INDEX CONCURRENTLY idx_agent_memory_agent_id ON agent_memory(agent_id);
CREATE INDEX CONCURRENTLY idx_agent_memory_timestamp ON agent_memory(created_at);
CREATE INDEX CONCURRENTLY idx_workflow_results_status ON workflow_results(status);

-- Partitioning for large tables
CREATE TABLE agent_memory_2024 PARTITION OF agent_memory
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

-- Connection pooling
ALTER SYSTEM SET max_connections = 200;
ALTER SYSTEM SET shared_buffers = '1GB';
ALTER SYSTEM SET effective_cache_size = '3GB';
ALTER SYSTEM SET work_mem = '4MB';
```

---

## Infrastructure Setup

### 1. Load Balancing

#### NGINX Configuration
```nginx
# nginx/nginx.conf
upstream conductor_backend {
    least_conn;
    server conductor-app-1:8080 max_fails=3 fail_timeout=30s;
    server conductor-app-2:8080 max_fails=3 fail_timeout=30s;
    server conductor-app-3:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name conductor.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name conductor.example.com;

    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req zone=api burst=20 nodelay;

    location / {
        proxy_pass http://conductor_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 300s;

        # Health check
        proxy_next_upstream error timeout http_500 http_502 http_503 http_504;
    }

    location /health {
        proxy_pass http://conductor_backend;
        access_log off;
    }

    location /metrics {
        proxy_pass http://conductor_backend;
        allow 10.0.0.0/8;  # Internal monitoring network
        deny all;
    }
}
```

### 2. Database High Availability

#### PostgreSQL Primary-Replica Setup
```yaml
# docker-compose-ha.yml
version: '3.8'

services:
  postgres-primary:
    image: postgres:15-alpine
    environment:
      POSTGRES_REPLICATION_USER: replicator
      POSTGRES_REPLICATION_PASSWORD: ${REPLICATION_PASSWORD}
    volumes:
      - postgres_primary_data:/var/lib/postgresql/data
      - ./scripts/primary-init.sh:/docker-entrypoint-initdb.d/init.sh
    command: |
      postgres
      -c wal_level=replica
      -c max_wal_senders=3
      -c max_replication_slots=3
      -c synchronous_commit=on
      -c synchronous_standby_names='replica1'

  postgres-replica:
    image: postgres:15-alpine
    environment:
      POSTGRES_MASTER_SERVICE: postgres-primary
      POSTGRES_REPLICA_USER: replicator
      POSTGRES_REPLICA_PASSWORD: ${REPLICATION_PASSWORD}
    volumes:
      - postgres_replica_data:/var/lib/postgresql/data
      - ./scripts/replica-init.sh:/docker-entrypoint-initdb.d/init.sh
    depends_on:
      - postgres-primary

volumes:
  postgres_primary_data:
  postgres_replica_data:
```

### 3. Caching Strategy

#### Redis Cluster Configuration
```conf
# redis/redis.conf
# Network
bind 0.0.0.0
port 6379
timeout 300

# Memory
maxmemory 2gb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000

# Security
requirepass ${REDIS_PASSWORD}

# Logging
loglevel notice
logfile /var/log/redis/redis-server.log

# Performance
tcp-keepalive 300
tcp-backlog 511
```

---

## Monitoring and Observability

### 1. Metrics Collection

#### Prometheus Configuration
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "conductor_rules.yml"

scrape_configs:
  - job_name: 'conductor-app'
    static_configs:
      - targets: ['conductor-app:8080']
    metrics_path: /actuator/prometheus
    scrape_interval: 30s

  - job_name: 'conductor-database'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'conductor-redis'
    static_configs:
      - targets: ['redis-exporter:9121']

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

#### Custom Metrics
```java
@Component
public class ConductorMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter workflowExecutions;
    private final Timer workflowDuration;
    private final Gauge activeAgents;

    public ConductorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.workflowExecutions = Counter.builder("conductor.workflows.executed")
            .description("Total number of workflows executed")
            .tag("status", "success")
            .register(meterRegistry);

        this.workflowDuration = Timer.builder("conductor.workflows.duration")
            .description("Workflow execution duration")
            .register(meterRegistry);

        this.activeAgents = Gauge.builder("conductor.agents.active")
            .description("Number of active agents")
            .register(meterRegistry, this, ConductorMetrics::getActiveAgentCount);
    }

    public void recordWorkflowExecution(boolean success, Duration duration) {
        workflowExecutions.increment(
            Tags.of("status", success ? "success" : "failure")
        );
        workflowDuration.record(duration);
    }

    private double getActiveAgentCount() {
        return orchestrator.getActiveAgentCount();
    }
}
```

### 2. Logging Strategy

#### Structured Logging Configuration
```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="production">
        <!-- JSON logging for production -->
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeContext>true</includeContext>
                <includeMdc>true</includeMdc>
                <customFields>{"service":"conductor","version":"${app.version}"}</customFields>
            </encoder>
        </appender>

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/conductor.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/conductor.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        </appender>

        <!-- Error-only appender for alerts -->
        <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/conductor-errors.log</file>
            <filter class="ch.qos.logback.classic.filter.LevelFilter">
                <level>ERROR</level>
                <onMatch>ACCEPT</onMatch>
                <onMismatch>DENY</onMismatch>
            </filter>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/conductor-errors.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>90</maxHistory>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        </appender>

        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>
</configuration>
```

### 3. Health Checks

#### Comprehensive Health Check Implementation
```java
@Component
public class ConductorHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final List<LLMProvider> llmProviders;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // Database health
        checkDatabase(builder);

        // Redis health
        checkRedis(builder);

        // LLM provider health
        checkLLMProviders(builder);

        // Memory usage
        checkMemoryUsage(builder);

        // Disk space
        checkDiskSpace(builder);

        return builder.build();
    }

    private void checkDatabase(Health.Builder builder) {
        try {
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                builder.withDetail("database", isValid ? "UP" : "DOWN");
                if (!isValid) {
                    builder.down();
                }
            }
        } catch (SQLException e) {
            builder.down().withException(e);
        }
    }

    private void checkLLMProviders(Health.Builder builder) {
        Map<String, String> providerStatus = new HashMap<>();
        for (LLMProvider provider : llmProviders) {
            try {
                boolean healthy = provider.isHealthy();
                providerStatus.put(provider.getName(), healthy ? "UP" : "DOWN");
                if (!healthy) {
                    builder.down();
                }
            } catch (Exception e) {
                providerStatus.put(provider.getName(), "ERROR: " + e.getMessage());
                builder.down();
            }
        }
        builder.withDetail("llmProviders", providerStatus);
    }
}
```

---

## Security Hardening

### 1. Application Security

#### Security Configuration
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/metrics").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
            );

        return http.build();
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter(1000, Duration.ofHours(1));
    }
}
```

#### API Key Authentication
```java
@Component
public class ApiKeyAuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = httpRequest.getHeader("X-API-Key");

        if (!isValidApiKey(apiKey)) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpResponse.getWriter().write("{\"error\": \"Invalid API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && apiKeyService.validateApiKey(apiKey);
    }
}
```

### 2. Infrastructure Security

#### Network Security
```yaml
# k8s/network-policy.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: conductor-network-policy
spec:
  podSelector:
    matchLabels:
      app: conductor
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: nginx-ingress
    - podSelector:
        matchLabels:
          app: monitoring
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - podSelector:
        matchLabels:
          app: redis
    ports:
    - protocol: TCP
      port: 6379
  - to: []  # Allow external LLM API calls
    ports:
    - protocol: TCP
      port: 443
```

---

## Performance Tuning

### 1. JVM Performance Tuning

#### G1GC Optimization
```bash
# G1GC tuning for production workloads
JAVA_OPTS="
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1HeapRegionSize=16m
-XX:G1ReservePercent=25
-XX:InitiatingHeapOccupancyPercent=45
-XX:G1MixedGCCountTarget=8
-XX:G1HeapWastePercent=5
"
```

### 2. Database Performance

#### Connection Pool Tuning
```properties
# HikariCP optimal settings for production
conductor.database.pool.maximum-pool-size=50
conductor.database.pool.minimum-idle=20
conductor.database.pool.connection-timeout=20000
conductor.database.pool.idle-timeout=300000
conductor.database.pool.max-lifetime=1800000
conductor.database.pool.leak-detection-threshold=60000

# Database-specific optimizations
conductor.database.pool.data-source-properties.cachePrepStmts=true
conductor.database.pool.data-source-properties.prepStmtCacheSize=250
conductor.database.pool.data-source-properties.prepStmtCacheSqlLimit=2048
conductor.database.pool.data-source-properties.useServerPrepStmts=true
```

---

## Disaster Recovery

### 1. Backup Strategy

#### Automated Backup Script
```bash
#!/bin/bash
# scripts/backup.sh

set -e

BACKUP_DIR="/backups/conductor"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

echo "Starting Conductor backup at $(date)"

# Database backup
pg_dump -h $DB_HOST -U $DB_USER -d conductor_prod | gzip > "$BACKUP_DIR/database_$TIMESTAMP.sql.gz"

# Configuration backup
tar -czf "$BACKUP_DIR/config_$TIMESTAMP.tar.gz" /app/config

# Upload to S3
aws s3 cp "$BACKUP_DIR/database_$TIMESTAMP.sql.gz" "s3://$BACKUP_BUCKET/database/"
aws s3 cp "$BACKUP_DIR/config_$TIMESTAMP.tar.gz" "s3://$BACKUP_BUCKET/config/"

# Cleanup old backups
find "$BACKUP_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup completed at $(date)"
```

### 2. Recovery Procedures

#### Database Recovery Script
```bash
#!/bin/bash
# scripts/restore.sh

BACKUP_FILE="$1"
TARGET_DB="$2"

if [[ -z "$BACKUP_FILE" || -z "$TARGET_DB" ]]; then
    echo "Usage: $0 <backup_file> <target_database>"
    exit 1
fi

echo "Restoring database from $BACKUP_FILE to $TARGET_DB"

# Create new database
createdb -h $DB_HOST -U $DB_USER "$TARGET_DB"

# Restore from backup
gunzip -c "$BACKUP_FILE" | psql -h $DB_HOST -U $DB_USER -d "$TARGET_DB"

echo "Database restore completed"
```

---

## Operational Runbooks

### 1. Common Operations

#### Scaling Application
```bash
# Kubernetes scaling
kubectl scale deployment conductor-app --replicas=5

# Docker Swarm scaling
docker service scale conductor_conductor-app=5

# Monitor scaling progress
watch kubectl get pods -l app=conductor
```

#### Rolling Updates
```bash
# Update image
kubectl set image deployment/conductor-app conductor=conductor:v1.1.0

# Monitor rollout
kubectl rollout status deployment/conductor-app

# Rollback if needed
kubectl rollout undo deployment/conductor-app
```

### 2. Troubleshooting Runbooks

#### High Memory Usage
```bash
# Check memory usage
kubectl top pods -l app=conductor

# Get heap dump
kubectl exec -it conductor-app-pod -- jcmd 1 GC.run_finalization
kubectl exec -it conductor-app-pod -- jcmd 1 VM.gc

# Analyze with Eclipse MAT
kubectl cp conductor-app-pod:/app/logs/heapdump.hprof ./heapdump.hprof
```

#### Database Connection Issues
```bash
# Check connection pool metrics
curl -s http://conductor-app:8080/actuator/metrics/hikaricp.connections.active

# Check database connectivity
kubectl exec -it conductor-app-pod -- pg_isready -h postgres -p 5432

# Review connection logs
kubectl logs conductor-app-pod | grep -i "connection"
```

This comprehensive deployment guide provides production-ready configurations and operational procedures for successfully deploying and maintaining the Conductor framework in various environments.