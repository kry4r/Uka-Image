package com.uka.image.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.uka.image.dto.StorageConfig;
import com.uka.image.dto.StorageConfig.StorageProviderType;
import com.uka.image.mapper.StorageConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Storage Configuration Service
 * Manages storage provider configurations and testing
 */
@Slf4j
@Service
public class StorageConfigService {
    
    @Autowired
    private StorageConfigMapper storageConfigMapper;
    
    private static final String ENCRYPTION_KEY = "UkaImageSecretKey16"; // 16 bytes for AES-128
    
    /**
     * Get all storage configurations
     * @return List of storage configurations with masked secrets
     */
    public List<StorageConfig> getAllConfigurations() {
        try {
            List<com.uka.image.entity.StorageConfig> entities = storageConfigMapper.selectList(null);
            return entities.stream()
                .map(this::convertToDto)
                .map(StorageConfig::getMaskedConfig)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get storage configurations", e);
            throw new RuntimeException("Failed to get storage configurations: " + e.getMessage());
        }
    }
    
    /**
     * Get configuration by provider type
     * @param providerType Storage provider type
     * @return Storage configuration with masked secret
     */
    public StorageConfig getConfiguration(StorageProviderType providerType) {
        try {
            com.uka.image.entity.StorageConfig entity = storageConfigMapper.selectOne(
                new QueryWrapper<com.uka.image.entity.StorageConfig>()
                    .eq("provider_type", providerType.getKey())
            );
            
            if (entity != null) {
                return convertToDto(entity).getMaskedConfig();
            }
            
            // Return default configuration if not found
            return getDefaultConfiguration(providerType);
            
        } catch (Exception e) {
            log.error("Failed to get storage configuration for provider: {}", providerType, e);
            throw new RuntimeException("Failed to get storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get active storage configuration
     * @return Active storage configuration
     */
    public StorageConfig getActiveConfiguration() {
        try {
            com.uka.image.entity.StorageConfig entity = storageConfigMapper.selectOne(
                new QueryWrapper<com.uka.image.entity.StorageConfig>()
                    .eq("is_active", true)
                    .last("LIMIT 1")
            );
            
            if (entity != null) {
                return convertToDto(entity);
            }
            
            // Return default Tencent COS configuration if no active config
            return getDefaultConfiguration(StorageProviderType.TENCENT_COS);
            
        } catch (Exception e) {
            log.error("Failed to get active storage configuration", e);
            throw new RuntimeException("Failed to get active storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Test storage connection
     * @param config Storage configuration to test
     * @return true if connection successful
     */
    public boolean testConnection(StorageConfig config) {
        try {
            // Create a temporary test file name
            String testFileName = "test-connection-" + System.currentTimeMillis() + ".txt";
            String testContent = "Connection test from Uka Image System";
            
            switch (config.getProvider()) {
                case TENCENT_COS:
                    return testTencentCosConnection(config, testFileName, testContent);
                case AWS_S3:
                    return testAwsS3Connection(config, testFileName, testContent);
                case ALIBABA_OSS:
                    return testAlibabaOssConnection(config, testFileName, testContent);
                default:
                    log.warn("Unsupported storage provider for testing: {}", config.getProvider());
                    return false;
            }
            
        } catch (Exception e) {
            log.error("Storage connection test failed for provider: {}", config.getProvider(), e);
            return false;
        }
    }
    
    /**
     * Update storage configuration
     * @param config New storage configuration
     */
    public void updateConfiguration(StorageConfig config) {
        try {
            // Validate configuration
            if (!config.isValid()) {
                throw new IllegalArgumentException("Invalid storage configuration");
            }
            
            com.uka.image.entity.StorageConfig entity = storageConfigMapper.selectOne(
                new QueryWrapper<com.uka.image.entity.StorageConfig>()
                    .eq("provider_type", config.getProvider().getKey())
            );
            
            if (entity == null) {
                entity = new com.uka.image.entity.StorageConfig();
                entity.setProviderType(config.getProvider().getKey());
            }
            
            // Update entity fields
            entity.setRegion(config.getRegion());
            entity.setAccessKeyId(config.getAccessKeyId());
            
            // Only update secret if it's not masked
            if (!"****".equals(config.getAccessKeySecret()) && config.getAccessKeySecret() != null) {
                entity.setAccessKeySecret(encryptValue(config.getAccessKeySecret()));
                entity.setIsEncrypted(true);
            }
            
            entity.setBucketName(config.getBucketName());
            entity.setBaseUrl(config.getBaseUrl());
            entity.setCustomDomain(config.getCustomDomain());
            entity.setUseHttps(config.isUseHttps());
            entity.setEnableCdn(config.isEnableCdn());
            entity.setAdditionalSettings(config.getAdditionalSettings());
            
            // If this config is set to active, deactivate others
            if (config.isActive()) {
                deactivateAllConfigurations();
                entity.setIsActive(true);
            }
            
            // Save or update
            if (entity.getId() == null) {
                storageConfigMapper.insert(entity);
            } else {
                storageConfigMapper.updateById(entity);
            }
            
            log.info("Storage configuration updated for provider: {}", config.getProvider());
            
        } catch (Exception e) {
            log.error("Failed to update storage configuration", e);
            throw new RuntimeException("Failed to update storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get supported storage providers
     * @return List of supported providers with their regions
     */
    public List<Map<String, Object>> getSupportedProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        
        // Tencent COS
        Map<String, Object> tencentCos = new HashMap<>();
        tencentCos.put("key", "tencent_cos");
        tencentCos.put("name", "Tencent Cloud COS");
        tencentCos.put("description", "Tencent Cloud Object Storage Service");
        tencentCos.put("regions", Arrays.asList(
            Map.of("key", "ap-beijing", "name", "Beijing"),
            Map.of("key", "ap-shanghai", "name", "Shanghai"),
            Map.of("key", "ap-guangzhou", "name", "Guangzhou"),
            Map.of("key", "ap-chengdu", "name", "Chengdu"),
            Map.of("key", "ap-singapore", "name", "Singapore"),
            Map.of("key", "ap-hongkong", "name", "Hong Kong")
        ));
        providers.add(tencentCos);
        
        // AWS S3
        Map<String, Object> awsS3 = new HashMap<>();
        awsS3.put("key", "aws_s3");
        awsS3.put("name", "Amazon S3");
        awsS3.put("description", "Amazon Simple Storage Service");
        awsS3.put("regions", Arrays.asList(
            Map.of("key", "us-east-1", "name", "US East (N. Virginia)"),
            Map.of("key", "us-west-2", "name", "US West (Oregon)"),
            Map.of("key", "eu-west-1", "name", "Europe (Ireland)"),
            Map.of("key", "ap-southeast-1", "name", "Asia Pacific (Singapore)"),
            Map.of("key", "ap-northeast-1", "name", "Asia Pacific (Tokyo)")
        ));
        providers.add(awsS3);
        
        // Alibaba OSS
        Map<String, Object> alibabaOss = new HashMap<>();
        alibabaOss.put("key", "alibaba_oss");
        alibabaOss.put("name", "Alibaba Cloud OSS");
        alibabaOss.put("description", "Alibaba Cloud Object Storage Service");
        alibabaOss.put("regions", Arrays.asList(
            Map.of("key", "oss-cn-hangzhou", "name", "China (Hangzhou)"),
            Map.of("key", "oss-cn-shanghai", "name", "China (Shanghai)"),
            Map.of("key", "oss-cn-beijing", "name", "China (Beijing)"),
            Map.of("key", "oss-cn-shenzhen", "name", "China (Shenzhen)"),
            Map.of("key", "oss-ap-southeast-1", "name", "Singapore")
        ));
        providers.add(alibabaOss);
        
        return providers;
    }
    
    /**
     * Convert entity to DTO
     */
    private StorageConfig convertToDto(com.uka.image.entity.StorageConfig entity) {
        StorageConfig dto = new StorageConfig();
        dto.setProvider(StorageProviderType.fromKey(entity.getProviderType()));
        dto.setRegion(entity.getRegion());
        dto.setAccessKeyId(entity.getAccessKeyId());
        
        // Decrypt secret if encrypted
        if (entity.getIsEncrypted() != null && entity.getIsEncrypted()) {
            dto.setAccessKeySecret(decryptValue(entity.getAccessKeySecret()));
        } else {
            dto.setAccessKeySecret(entity.getAccessKeySecret());
        }
        
        dto.setBucketName(entity.getBucketName());
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setCustomDomain(entity.getCustomDomain());
        dto.setUseHttps(entity.getUseHttps() != null ? entity.getUseHttps() : true);
        dto.setEnableCdn(entity.getEnableCdn() != null ? entity.getEnableCdn() : false);
        dto.setActive(entity.getIsActive() != null ? entity.getIsActive() : false);
        dto.setAdditionalSettings(entity.getAdditionalSettings());
        
        return dto;
    }
    
    /**
     * Get default configuration for provider
     */
    private StorageConfig getDefaultConfiguration(StorageProviderType providerType) {
        StorageConfig config = new StorageConfig();
        config.setProvider(providerType);
        
        switch (providerType) {
            case TENCENT_COS:
                config.setRegion("ap-beijing");
                config.setBaseUrl("https://your-bucket.cos.ap-beijing.myqcloud.com");
                break;
            case AWS_S3:
                config.setRegion("us-east-1");
                config.setBaseUrl("https://your-bucket.s3.amazonaws.com");
                break;
            case ALIBABA_OSS:
                config.setRegion("oss-cn-hangzhou");
                config.setBaseUrl("https://oss-cn-hangzhou.aliyuncs.com");
                break;
        }
        
        return config;
    }
    
    /**
     * Deactivate all storage configurations
     */
    private void deactivateAllConfigurations() {
        UpdateWrapper<com.uka.image.entity.StorageConfig> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("is_active", false);
        storageConfigMapper.update(null, updateWrapper);
    }
    
    /**
     * Test Tencent COS connection
     */
    private boolean testTencentCosConnection(StorageConfig config, String testFileName, String testContent) {
        // Implementation would use Tencent COS SDK
        // For now, return true as placeholder
        log.info("Testing Tencent COS connection for bucket: {}", config.getBucketName());
        return true;
    }
    
    /**
     * Test AWS S3 connection
     */
    private boolean testAwsS3Connection(StorageConfig config, String testFileName, String testContent) {
        // Implementation would use AWS S3 SDK
        // For now, return true as placeholder
        log.info("Testing AWS S3 connection for bucket: {}", config.getBucketName());
        return true;
    }
    
    /**
     * Test Alibaba OSS connection
     */
    private boolean testAlibabaOssConnection(StorageConfig config, String testFileName, String testContent) {
        // Implementation would use Alibaba OSS SDK
        // For now, return true as placeholder
        log.info("Testing Alibaba OSS connection for bucket: {}", config.getBucketName());
        return true;
    }
    
    /**
     * Encrypt value using AES
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
     * Decrypt value using AES
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