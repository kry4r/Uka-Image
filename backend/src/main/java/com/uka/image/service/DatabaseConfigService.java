package com.uka.image.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.uka.image.dto.DatabaseConfig;
import com.uka.image.entity.SystemConfig;
import com.uka.image.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Database Configuration Service
 * Manages database connection configuration and testing
 */
@Slf4j
@Service
public class DatabaseConfigService {
    
    @Autowired
    private SystemConfigMapper systemConfigMapper;
    
    @Autowired
    private Environment environment;
    
    @Autowired
    private DataSource dataSource;
    
    private static final String ENCRYPTION_KEY = "UkaImageSecretKey16"; // 16 bytes for AES-128
    
    /**
     * Get current database configuration
     * @return Current configuration with masked password
     */
    public DatabaseConfig getCurrentConfig() {
        DatabaseConfig config = new DatabaseConfig();
        
        // Load configuration from database or environment variables
        config.setHost(getConfigValue("db.host", environment.getProperty("DB_HOST", "localhost")));
        config.setPort(Integer.parseInt(getConfigValue("db.port", environment.getProperty("DB_PORT", "3306"))));
        config.setDatabase(getConfigValue("db.name", environment.getProperty("DB_NAME", "uka_image_db")));
        config.setUsername(getConfigValue("db.username", environment.getProperty("DB_USERNAME", "root")));
        config.setUseSSL(Boolean.parseBoolean(getConfigValue("db.use_ssl", "false")));
        config.setServerTimezone(getConfigValue("db.server_timezone", "UTC"));
        
        // Connection pool settings
        config.setMinIdle(Integer.parseInt(getConfigValue("db.min_idle", "5")));
        config.setMaxActive(Integer.parseInt(getConfigValue("db.max_active", "20")));
        config.setMaxWait(Long.parseLong(getConfigValue("db.max_wait", "60000")));
        config.setValidationQuery(getConfigValue("db.validation_query", "SELECT 1"));
        config.setConnectionTimeout(Long.parseLong(getConfigValue("db.connection_timeout", "30000")));
        config.setIdleTimeout(Long.parseLong(getConfigValue("db.idle_timeout", "600000")));
        config.setMaxLifetime(Long.parseLong(getConfigValue("db.max_lifetime", "1800000")));
        
        // Return masked configuration for security
        return config.getMaskedConfig();
    }
    
    /**
     * Test database connection with provided configuration
     * @param config Database configuration to test
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection(DatabaseConfig config) {
        String testUrl = config.getJdbcUrl();
        
        try (Connection connection = DriverManager.getConnection(testUrl, config.getUsername(), config.getPassword())) {
            boolean isValid = connection.isValid(5);
            if (isValid) {
                log.info("Database connection test successful for host: {}", config.getHost());
            } else {
                log.warn("Database connection test failed for host: {}", config.getHost());
            }
            return isValid;
        } catch (SQLException e) {
            log.error("Database connection test failed for host: {}, error: {}", config.getHost(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Update database configuration
     * @param config New database configuration
     */
    public void updateConfig(DatabaseConfig config) {
        try {
            // Validate configuration before saving
            if (!config.isValid()) {
                throw new IllegalArgumentException("Invalid database configuration");
            }
            
            // Save configuration values
            saveConfigValue("db.host", config.getHost());
            saveConfigValue("db.port", String.valueOf(config.getPort()));
            saveConfigValue("db.name", config.getDatabase());
            saveConfigValue("db.username", config.getUsername());
            
            // Only update password if it's not masked
            if (!"****".equals(config.getPassword()) && config.getPassword() != null) {
                saveEncryptedConfigValue("db.password", config.getPassword());
            }
            
            saveConfigValue("db.use_ssl", String.valueOf(config.isUseSSL()));
            saveConfigValue("db.server_timezone", config.getServerTimezone());
            
            // Connection pool settings
            if (config.getMinIdle() != null) {
                saveConfigValue("db.min_idle", String.valueOf(config.getMinIdle()));
            }
            if (config.getMaxActive() != null) {
                saveConfigValue("db.max_active", String.valueOf(config.getMaxActive()));
            }
            if (config.getMaxWait() != null) {
                saveConfigValue("db.max_wait", String.valueOf(config.getMaxWait()));
            }
            if (config.getValidationQuery() != null) {
                saveConfigValue("db.validation_query", config.getValidationQuery());
            }
            if (config.getConnectionTimeout() != null) {
                saveConfigValue("db.connection_timeout", String.valueOf(config.getConnectionTimeout()));
            }
            if (config.getIdleTimeout() != null) {
                saveConfigValue("db.idle_timeout", String.valueOf(config.getIdleTimeout()));
            }
            if (config.getMaxLifetime() != null) {
                saveConfigValue("db.max_lifetime", String.valueOf(config.getMaxLifetime()));
            }
            
            log.info("Database configuration updated successfully");
            
        } catch (Exception e) {
            log.error("Failed to update database configuration", e);
            throw new RuntimeException("Failed to update database configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get current database connection status and statistics
     * @return Connection status information
     */
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test current connection
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                status.put("connected", isValid);
                status.put("database", connection.getCatalog());
                status.put("url", connection.getMetaData().getURL());
                status.put("driver", connection.getMetaData().getDriverName());
                status.put("driverVersion", connection.getMetaData().getDriverVersion());
                
                if (isValid) {
                    status.put("status", "Connected");
                    status.put("message", "Database connection is healthy");
                } else {
                    status.put("status", "Disconnected");
                    status.put("message", "Database connection is not valid");
                }
            }
            
            // Add connection pool information if available
            // This would require access to HikariCP MBean or similar
            status.put("poolInfo", "Connection pool information not available");
            
        } catch (SQLException e) {
            status.put("connected", false);
            status.put("status", "Error");
            status.put("message", "Failed to get connection: " + e.getMessage());
            log.error("Failed to get database connection status", e);
        }
        
        return status;
    }
    
    /**
     * Get configuration value from database or return default
     * @param key Configuration key
     * @param defaultValue Default value if not found
     * @return Configuration value
     */
    private String getConfigValue(String key, String defaultValue) {
        try {
            SystemConfig config = systemConfigMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key)
            );
            
            if (config != null) {
                String value = config.getConfigValue();
                
                // Decrypt if encrypted
                if (config.getIsEncrypted() != null && config.getIsEncrypted()) {
                    value = decryptValue(value);
                }
                
                return value;
            }
        } catch (Exception e) {
            log.warn("Failed to get config value for key: {}, using default", key, e);
        }
        
        return defaultValue;
    }
    
    /**
     * Save configuration value to database
     * @param key Configuration key
     * @param value Configuration value
     */
    private void saveConfigValue(String key, String value) {
        try {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("string");
            config.setDescription("Database configuration: " + key);
            config.setIsEncrypted(false);
            
            SystemConfig existing = systemConfigMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key)
            );
            
            if (existing != null) {
                config.setId(existing.getId());
                systemConfigMapper.updateById(config);
            } else {
                systemConfigMapper.insert(config);
            }
            
        } catch (Exception e) {
            log.error("Failed to save config value for key: {}", key, e);
            throw new RuntimeException("Failed to save configuration: " + e.getMessage());
        }
    }
    
    /**
     * Save encrypted configuration value to database
     * @param key Configuration key
     * @param value Configuration value to encrypt
     */
    private void saveEncryptedConfigValue(String key, String value) {
        try {
            String encryptedValue = encryptValue(value);
            
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(encryptedValue);
            config.setConfigType("encrypted");
            config.setDescription("Encrypted database configuration: " + key);
            config.setIsEncrypted(true);
            
            SystemConfig existing = systemConfigMapper.selectOne(
                new QueryWrapper<SystemConfig>().eq("config_key", key)
            );
            
            if (existing != null) {
                config.setId(existing.getId());
                systemConfigMapper.updateById(config);
            } else {
                systemConfigMapper.insert(config);
            }
            
        } catch (Exception e) {
            log.error("Failed to save encrypted config value for key: {}", key, e);
            throw new RuntimeException("Failed to save encrypted configuration: " + e.getMessage());
        }
    }
    
    /**
     * Encrypt configuration value using AES
     * @param value Value to encrypt
     * @return Encrypted value (Base64 encoded)
     */
    private String encryptValue(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encrypt value", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage());
        }
    }
    
    /**
     * Decrypt configuration value using AES
     * @param encryptedValue Encrypted value (Base64 encoded)
     * @return Decrypted value
     */
    private String decryptValue(String encryptedValue) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(decrypted);
        } catch (Exception e) {
            log.error("Failed to decrypt value", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage());
        }
    }
}