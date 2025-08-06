package com.uka.image.dto;

import lombok.Data;

/**
 * Storage Configuration DTO
 * Contains all storage provider configuration parameters
 */
@Data
public class StorageConfig {
    
    /**
     * Storage provider type
     */
    private StorageProviderType provider;
    
    /**
     * Storage region
     */
    private String region;
    
    /**
     * Access key ID
     */
    private String accessKeyId;
    
    /**
     * Access key secret (masked in responses for security)
     */
    private String accessKeySecret;
    
    /**
     * Bucket name
     */
    private String bucketName;
    
    /**
     * Base URL for storage service
     */
    private String baseUrl;
    
    /**
     * Custom domain for CDN
     */
    private String customDomain;
    
    /**
     * Whether to use HTTPS
     */
    private boolean useHttps;
    
    /**
     * Whether to enable CDN
     */
    private boolean enableCdn;
    
    /**
     * Whether this configuration is active
     */
    private boolean isActive;
    
    /**
     * Additional provider-specific settings
     */
    private String additionalSettings;
    
    /**
     * Default constructor
     */
    public StorageConfig() {
        this.useHttps = true;
        this.enableCdn = false;
        this.isActive = false;
    }
    
    /**
     * Storage provider types
     */
    public enum StorageProviderType {
        TENCENT_COS("tencent_cos", "Tencent Cloud COS"),
        AWS_S3("aws_s3", "Amazon S3"),
        ALIBABA_OSS("alibaba_oss", "Alibaba Cloud OSS");
        
        private final String key;
        private final String name;
        
        StorageProviderType(String key, String name) {
            this.key = key;
            this.name = name;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getName() {
            return name;
        }
        
        public static StorageProviderType fromKey(String key) {
            for (StorageProviderType type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown storage provider: " + key);
        }
    }
    
    /**
     * Get masked configuration for security
     * @return Configuration with masked sensitive data
     */
    public StorageConfig getMaskedConfig() {
        StorageConfig masked = new StorageConfig();
        masked.setProvider(this.provider);
        masked.setRegion(this.region);
        masked.setAccessKeyId(this.accessKeyId);
        masked.setAccessKeySecret("****"); // Mask secret
        masked.setBucketName(this.bucketName);
        masked.setBaseUrl(this.baseUrl);
        masked.setCustomDomain(this.customDomain);
        masked.setUseHttps(this.useHttps);
        masked.setEnableCdn(this.enableCdn);
        masked.setActive(this.isActive);
        masked.setAdditionalSettings(this.additionalSettings);
        return masked;
    }
    
    /**
     * Validate configuration parameters
     * @return true if configuration is valid
     */
    public boolean isValid() {
        if (provider == null) {
            return false;
        }
        if (region == null || region.trim().isEmpty()) {
            return false;
        }
        if (accessKeyId == null || accessKeyId.trim().isEmpty()) {
            return false;
        }
        if (accessKeySecret == null || accessKeySecret.trim().isEmpty()) {
            return false;
        }
        if (bucketName == null || bucketName.trim().isEmpty()) {
            return false;
        }
        return true;
    }
    
    /**
     * Get provider-specific endpoint URL
     * @return Endpoint URL based on provider and region
     */
    public String getEndpointUrl() {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            return baseUrl;
        }
        
        switch (provider) {
            case TENCENT_COS:
                return String.format("https://%s.cos.%s.myqcloud.com", bucketName, region);
            case AWS_S3:
                if ("us-east-1".equals(region)) {
                    return String.format("https://%s.s3.amazonaws.com", bucketName);
                } else {
                    return String.format("https://%s.s3.%s.amazonaws.com", bucketName, region);
                }
            case ALIBABA_OSS:
                return String.format("https://%s.%s.aliyuncs.com", bucketName, region);
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }
    
    /**
     * Get public access URL for a file
     * @param fileName File name
     * @return Public URL
     */
    public String getPublicUrl(String fileName) {
        String baseUrl = enableCdn && customDomain != null && !customDomain.trim().isEmpty() 
            ? customDomain 
            : getEndpointUrl();
            
        String protocol = useHttps ? "https://" : "http://";
        if (!baseUrl.startsWith("http")) {
            baseUrl = protocol + baseUrl;
        }
        
        return baseUrl + "/" + fileName;
    }
}