# MySQL Remote Database Connection Guide

## Overview
This guide provides comprehensive instructions for connecting the Image Hosting System to a remote MySQL database, including configuration management and a web-based configuration interface.

## Table of Contents
1. [Database Requirements](#database-requirements)
2. [Connection Configuration](#connection-configuration)
3. [Configuration Interface Implementation](#configuration-interface-implementation)
4. [Security Considerations](#security-considerations)
5. [Troubleshooting](#troubleshooting)

## Database Requirements

### Minimum MySQL Version
- MySQL 8.0+ (recommended)
- MySQL 5.7+ (minimum supported)

### Required Database Privileges
```sql
-- Create a dedicated user for the application
CREATE USER 'uka_image_user'@'%' IDENTIFIED BY 'secure_password_here';

-- Grant necessary privileges
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX ON uka_image_db.* TO 'uka_image_user'@'%';

-- Apply changes
FLUSH PRIVILEGES;
```

### Database Schema
```sql
-- Create database
CREATE DATABASE IF NOT EXISTS uka_image_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE uka_image_db;

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- Images table
CREATE TABLE images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    width INT,
    height INT,
    storage_path VARCHAR(500) NOT NULL,
    storage_provider VARCHAR(50) DEFAULT 'tencent_cos',
    thumbnail_path VARCHAR(500),
    description TEXT,
    tags JSON,
    upload_ip VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_filename (filename),
    INDEX idx_created_at (created_at),
    INDEX idx_storage_provider (storage_provider)
);

-- Image search metadata table
CREATE TABLE image_search_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id BIGINT NOT NULL,
    search_keywords TEXT,
    ai_description TEXT,
    color_palette JSON,
    objects_detected JSON,
    faces_detected JSON,
    text_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE CASCADE,
    INDEX idx_image_id (image_id),
    FULLTEXT idx_search_keywords (search_keywords),
    FULLTEXT idx_ai_description (ai_description),
    FULLTEXT idx_text_content (text_content)
);

-- System configuration table
CREATE TABLE system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_type VARCHAR(50) DEFAULT 'string',
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
);
```

## Connection Configuration

### Environment Variables
Create a `.env` file in the backend directory:

```properties
# Database Configuration
DB_HOST=your-mysql-host.com
DB_PORT=3306
DB_NAME=uka_image_db
DB_USERNAME=uka_image_user
DB_PASSWORD=secure_password_here

# Connection Pool Settings
DB_INITIAL_SIZE=5
DB_MIN_IDLE=5
DB_MAX_ACTIVE=20
DB_MAX_WAIT=60000
DB_VALIDATION_QUERY=SELECT 1

# SSL Configuration (optional)
DB_USE_SSL=true
DB_SSL_MODE=REQUIRED
DB_SERVER_TIMEZONE=UTC
```

### Application Configuration
Update `application.yml`:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:uka_image_db}?useSSL=${DB_USE_SSL:false}&serverTimezone=${DB_SERVER_TIMEZONE:UTC}&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8mb4
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    
    # HikariCP Connection Pool Configuration
    hikari:
      pool-name: UkaImageHikariCP
      minimum-idle: ${DB_MIN_IDLE:5}
      maximum-pool-size: ${DB_MAX_ACTIVE:20}
      connection-timeout: ${DB_MAX_WAIT:60000}
      idle-timeout: 600000
      max-lifetime: 1800000
      validation-timeout: 5000
      connection-test-query: ${DB_VALIDATION_QUERY:SELECT 1}
      
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        use_sql_comments: true
        
# MyBatis Plus Configuration
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### Maven Dependencies
Add to `pom.xml`:

```xml
<dependencies>
    <!-- MySQL Connector -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    
    <!-- HikariCP Connection Pool -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>5.0.1</version>
    </dependency>
    
    <!-- Database Migration -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>9.22.3</version>
    </dependency>
    
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
        <version>9.22.3</version>
    </dependency>
</dependencies>
```

## Configuration Interface Implementation

### Backend Configuration Controller

```java
@RestController
@RequestMapping("/api/admin/config")
@CrossOrigin(origins = "*")
public class DatabaseConfigController {
    
    @Autowired
    private DatabaseConfigService databaseConfigService;
    
    @GetMapping("/database")
    public ApiResponse<DatabaseConfig> getDatabaseConfig() {
        try {
            DatabaseConfig config = databaseConfigService.getCurrentConfig();
            return ApiResponse.success(config);
        } catch (Exception e) {
            return ApiResponse.error("Failed to get database configuration: " + e.getMessage());
        }
    }
    
    @PostMapping("/database/test")
    public ApiResponse<String> testDatabaseConnection(@RequestBody DatabaseConfig config) {
        try {
            boolean isConnected = databaseConfigService.testConnection(config);
            if (isConnected) {
                return ApiResponse.success("Database connection successful");
            } else {
                return ApiResponse.error("Database connection failed");
            }
        } catch (Exception e) {
            return ApiResponse.error("Connection test failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/database")
    public ApiResponse<String> updateDatabaseConfig(@RequestBody DatabaseConfig config) {
        try {
            databaseConfigService.updateConfig(config);
            return ApiResponse.success("Database configuration updated successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to update configuration: " + e.getMessage());
        }
    }
}
```

### Database Configuration Service

```java
@Service
public class DatabaseConfigService {
    
    @Autowired
    private SystemConfigMapper systemConfigMapper;
    
    @Autowired
    private Environment environment;
    
    public DatabaseConfig getCurrentConfig() {
        DatabaseConfig config = new DatabaseConfig();
        config.setHost(getConfigValue("db.host", environment.getProperty("DB_HOST", "localhost")));
        config.setPort(Integer.parseInt(getConfigValue("db.port", environment.getProperty("DB_PORT", "3306"))));
        config.setDatabase(getConfigValue("db.name", environment.getProperty("DB_NAME", "uka_image_db")));
        config.setUsername(getConfigValue("db.username", environment.getProperty("DB_USERNAME", "root")));
        // Don't return password for security
        config.setPassword("****");
        config.setUseSSL(Boolean.parseBoolean(getConfigValue("db.use_ssl", "false")));
        config.setServerTimezone(getConfigValue("db.server_timezone", "UTC"));
        return config;
    }
    
    public boolean testConnection(DatabaseConfig config) {
        String testUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=%s&allowPublicKeyRetrieval=true",
            config.getHost(), config.getPort(), config.getDatabase(),
            config.isUseSSL(), config.getServerTimezone()
        );
        
        try (Connection connection = DriverManager.getConnection(testUrl, config.getUsername(), config.getPassword())) {
            return connection.isValid(5);
        } catch (SQLException e) {
            log.error("Database connection test failed", e);
            return false;
        }
    }
    
    public void updateConfig(DatabaseConfig config) {
        saveConfigValue("db.host", config.getHost());
        saveConfigValue("db.port", String.valueOf(config.getPort()));
        saveConfigValue("db.name", config.getDatabase());
        saveConfigValue("db.username", config.getUsername());
        if (!"****".equals(config.getPassword())) {
            saveEncryptedConfigValue("db.password", config.getPassword());
        }
        saveConfigValue("db.use_ssl", String.valueOf(config.isUseSSL()));
        saveConfigValue("db.server_timezone", config.getServerTimezone());
    }
    
    private String getConfigValue(String key, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectOne(
            new QueryWrapper<SystemConfig>().eq("config_key", key)
        );
        return config != null ? config.getConfigValue() : defaultValue;
    }
    
    private void saveConfigValue(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType("string");
        config.setDescription("Database configuration: " + key);
        
        SystemConfig existing = systemConfigMapper.selectOne(
            new QueryWrapper<SystemConfig>().eq("config_key", key)
        );
        
        if (existing != null) {
            config.setId(existing.getId());
            systemConfigMapper.updateById(config);
        } else {
            systemConfigMapper.insert(config);
        }
    }
    
    private void saveEncryptedConfigValue(String key, String value) {
        // Implement encryption logic here
        String encryptedValue = encryptValue(value);
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(encryptedValue);
        config.setConfigType("encrypted");
        config.setIsEncrypted(true);
        config.setDescription("Encrypted database configuration: " + key);
        
        SystemConfig existing = systemConfigMapper.selectOne(
            new QueryWrapper<SystemConfig>().eq("config_key", key)
        );
        
        if (existing != null) {
            config.setId(existing.getId());
            systemConfigMapper.updateById(config);
        } else {
            systemConfigMapper.insert(config);
        }
    }
    
    private String encryptValue(String value) {
        // Implement AES encryption
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec("MySecretKey12345".getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
}
```

### Database Configuration DTO

```java
@Data
public class DatabaseConfig {
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    private boolean useSSL;
    private String serverTimezone;
    private Integer minIdle;
    private Integer maxActive;
    private Long maxWait;
    private String validationQuery;
}
```

### Frontend Configuration Interface

```vue
<template>
  <div class="database-config">
    <div class="config-header">
      <h2>Database Configuration</h2>
      <p>Configure remote MySQL database connection</p>
    </div>
    
    <form @submit.prevent="saveConfiguration" class="config-form">
      <div class="form-group">
        <label for="host">Database Host</label>
        <input
          id="host"
          v-model="config.host"
          type="text"
          placeholder="e.g., mysql.example.com"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="port">Port</label>
        <input
          id="port"
          v-model.number="config.port"
          type="number"
          placeholder="3306"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="database">Database Name</label>
        <input
          id="database"
          v-model="config.database"
          type="text"
          placeholder="uka_image_db"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="username">Username</label>
        <input
          id="username"
          v-model="config.username"
          type="text"
          placeholder="database username"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="password">Password</label>
        <input
          id="password"
          v-model="config.password"
          type="password"
          placeholder="database password"
          required
        />
      </div>
      
      <div class="form-group">
        <label class="checkbox-label">
          <input
            v-model="config.useSSL"
            type="checkbox"
          />
          Use SSL Connection
        </label>
      </div>
      
      <div class="form-group">
        <label for="timezone">Server Timezone</label>
        <select id="timezone" v-model="config.serverTimezone">
          <option value="UTC">UTC</option>
          <option value="Asia/Shanghai">Asia/Shanghai</option>
          <option value="America/New_York">America/New_York</option>
          <option value="Europe/London">Europe/London</option>
        </select>
      </div>
      
      <div class="form-actions">
        <button
          type="button"
          @click="testConnection"
          :disabled="testing"
          class="btn-test"
        >
          {{ testing ? 'Testing...' : 'Test Connection' }}
        </button>
        
        <button
          type="submit"
          :disabled="saving"
          class="btn-save"
        >
          {{ saving ? 'Saving...' : 'Save Configuration' }}
        </button>
      </div>
    </form>
    
    <div v-if="testResult" class="test-result" :class="testResult.type">
      {{ testResult.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { databaseConfigApi } from '@/api/config'

interface DatabaseConfig {
  host: string
  port: number
  database: string
  username: string
  password: string
  useSSL: boolean
  serverTimezone: string
}

const config = ref<DatabaseConfig>({
  host: '',
  port: 3306,
  database: 'uka_image_db',
  username: '',
  password: '',
  useSSL: false,
  serverTimezone: 'UTC'
})

const testing = ref(false)
const saving = ref(false)
const testResult = ref<{ type: 'success' | 'error', message: string } | null>(null)

onMounted(async () => {
  try {
    const response = await databaseConfigApi.getConfig()
    if (response.success) {
      config.value = { ...config.value, ...response.data }
    }
  } catch (error) {
    console.error('Failed to load configuration:', error)
  }
})

const testConnection = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    const response = await databaseConfigApi.testConnection(config.value)
    testResult.value = {
      type: response.success ? 'success' : 'error',
      message: response.message || (response.success ? 'Connection successful' : 'Connection failed')
    }
  } catch (error) {
    testResult.value = {
      type: 'error',
      message: 'Connection test failed: ' + (error as Error).message
    }
  } finally {
    testing.value = false
  }
}

const saveConfiguration = async () => {
  saving.value = true
  
  try {
    const response = await databaseConfigApi.updateConfig(config.value)
    if (response.success) {
      testResult.value = {
        type: 'success',
        message: 'Configuration saved successfully'
      }
    } else {
      testResult.value = {
        type: 'error',
        message: response.message || 'Failed to save configuration'
      }
    }
  } catch (error) {
    testResult.value = {
      type: 'error',
      message: 'Save failed: ' + (error as Error).message
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.database-config {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.config-header {
  margin-bottom: 30px;
  text-align: center;
}

.config-header h2 {
  color: #333;
  margin-bottom: 10px;
}

.config-form {
  background: #f9f9f9;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #555;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
  margin-right: 8px;
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 30px;
}

.btn-test,
.btn-save {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-test {
  background-color: #17a2b8;
  color: white;
}

.btn-test:hover:not(:disabled) {
  background-color: #138496;
}

.btn-save {
  background-color: #28a745;
  color: white;
}

.btn-save:hover:not(:disabled) {
  background-color: #218838;
}

.btn-test:disabled,
.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.test-result {
  margin-top: 20px;
  padding: 10px;
  border-radius: 4px;
  text-align: center;
}

.test-result.success {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.test-result.error {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}
</style>
```

## Security Considerations

### 1. Connection Security
- Always use SSL/TLS for production connections
- Implement connection encryption
- Use strong passwords and rotate them regularly

### 2. Access Control
- Create dedicated database users with minimal required privileges
- Restrict database access by IP address when possible
- Use connection pooling to limit concurrent connections

### 3. Configuration Security
- Encrypt sensitive configuration values
- Store passwords securely using environment variables
- Implement configuration access logging

### 4. Network Security
- Use VPN or private networks when possible
- Configure firewall rules to restrict database access
- Monitor connection attempts and failures

## Troubleshooting

### Common Connection Issues

1. **Connection Timeout**
   ```
   Solution: Check network connectivity and firewall settings
   Increase connection timeout values in configuration
   ```

2. **Authentication Failed**
   ```
   Solution: Verify username and password
   Check user privileges and host restrictions
   ```

3. **SSL Connection Issues**
   ```
   Solution: Verify SSL certificates
   Check SSL mode configuration
   Update MySQL connector version
   ```

4. **Character Encoding Issues**
   ```
   Solution: Ensure UTF-8 encoding in connection string
   Set proper character set in database
   ```

### Performance Optimization

1. **Connection Pool Tuning**
   ```yaml
   hikari:
     minimum-idle: 10
     maximum-pool-size: 50
     connection-timeout: 30000
     idle-timeout: 600000
     max-lifetime: 1800000
   ```

2. **Query Optimization**
   - Use proper indexes
   - Optimize slow queries
   - Enable query caching

3. **Monitoring**
   - Monitor connection pool metrics
   - Track query performance
   - Set up alerts for connection failures

## Migration from Local to Remote

### Step-by-Step Migration

1. **Backup Local Database**
   ```bash
   mysqldump -u root -p uka_image_db > backup.sql
   ```

2. **Create Remote Database**
   ```sql
   CREATE DATABASE uka_image_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Import Data**
   ```bash
   mysql -h remote-host -u username -p uka_image_db < backup.sql
   ```

4. **Update Configuration**
   - Update environment variables
   - Test connection
   - Deploy application

5. **Verify Migration**
   - Test all application features
   - Verify data integrity
   - Monitor performance

This guide provides a complete solution for connecting to remote MySQL databases with a user-friendly configuration interface.