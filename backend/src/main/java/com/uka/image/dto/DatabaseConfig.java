package com.uka.image.dto;

import lombok.Data;

/**
 * Database Configuration DTO
 * Contains all database connection parameters
 */
@Data
public class DatabaseConfig {
    
    /**
     * Database host address
     */
    private String host;
    
    /**
     * Database port number
     */
    private Integer port;
    
    /**
     * Database name
     */
    private String database;
    
    /**
     * Database username
     */
    private String username;
    
    /**
     * Database password (masked in responses for security)
     */
    private String password;
    
    /**
     * Whether to use SSL connection
     */
    private boolean useSSL;
    
    /**
     * Server timezone setting
     */
    private String serverTimezone;
    
    /**
     * Connection pool minimum idle connections
     */
    private Integer minIdle;
    
    /**
     * Connection pool maximum active connections
     */
    private Integer maxActive;
    
    /**
     * Connection pool maximum wait time (milliseconds)
     */
    private Long maxWait;
    
    /**
     * Connection validation query
     */
    private String validationQuery;
    
    /**
     * Connection timeout (milliseconds)
     */
    private Long connectionTimeout;
    
    /**
     * Idle timeout (milliseconds)
     */
    private Long idleTimeout;
    
    /**
     * Maximum connection lifetime (milliseconds)
     */
    private Long maxLifetime;
    
    /**
     * Default constructor
     */
    public DatabaseConfig() {
        // Set default values
        this.port = 3306;
        this.useSSL = false;
        this.serverTimezone = "UTC";
        this.minIdle = 5;
        this.maxActive = 20;
        this.maxWait = 60000L;
        this.validationQuery = "SELECT 1";
        this.connectionTimeout = 30000L;
        this.idleTimeout = 600000L;
        this.maxLifetime = 1800000L;
    }
    
    /**
     * Get JDBC URL from configuration
     * @return Complete JDBC URL
     */
    public String getJdbcUrl() {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=%s&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8mb4",
            this.host, this.port, this.database, this.useSSL, this.serverTimezone
        );
    }
    
    /**
     * Mask password for security when returning configuration
     * @return Configuration with masked password
     */
    public DatabaseConfig getMaskedConfig() {
        DatabaseConfig masked = new DatabaseConfig();
        masked.setHost(this.host);
        masked.setPort(this.port);
        masked.setDatabase(this.database);
        masked.setUsername(this.username);
        masked.setPassword("****"); // Mask password
        masked.setUseSSL(this.useSSL);
        masked.setServerTimezone(this.serverTimezone);
        masked.setMinIdle(this.minIdle);
        masked.setMaxActive(this.maxActive);
        masked.setMaxWait(this.maxWait);
        masked.setValidationQuery(this.validationQuery);
        masked.setConnectionTimeout(this.connectionTimeout);
        masked.setIdleTimeout(this.idleTimeout);
        masked.setMaxLifetime(this.maxLifetime);
        return masked;
    }
    
    /**
     * Validate configuration parameters
     * @return true if configuration is valid
     */
    public boolean isValid() {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        if (port == null || port < 1 || port > 65535) {
            return false;
        }
        if (database == null || database.trim().isEmpty()) {
            return false;
        }
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return true;
    }
}