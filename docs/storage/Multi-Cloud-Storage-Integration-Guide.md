# Multi-Cloud Storage Integration Guide

## Overview
This guide provides comprehensive instructions for integrating multiple cloud storage providers with the Image Hosting System, including Tencent Cloud COS, AWS S3, and Alibaba Cloud OSS. The system supports dynamic provider switching and unified configuration management.

## Table of Contents
1. [Supported Storage Providers](#supported-storage-providers)
2. [Storage Architecture](#storage-architecture)
3. [Configuration Management](#configuration-management)
4. [Provider Implementation](#provider-implementation)
5. [Performance Optimization](#performance-optimization)
6. [Security Considerations](#security-considerations)
7. [Migration Between Providers](#migration-between-providers)

## Supported Storage Providers

### 1. Tencent Cloud COS (Cloud Object Storage)
- **Regions**: Global coverage with 60+ availability zones
- **Features**: CDN acceleration, image processing, lifecycle management
- **Best for**: China mainland users, integrated ecosystem

### 2. AWS S3 (Simple Storage Service)
- **Regions**: Global coverage with 80+ availability zones
- **Features**: Advanced analytics, machine learning integration, extensive API
- **Best for**: Global users, enterprise applications

### 3. Alibaba Cloud OSS (Object Storage Service)
- **Regions**: Asia-Pacific focus with global expansion
- **Features**: Image processing, video transcoding, data lake analytics
- **Best for**: Asia-Pacific users, cost-effective solutions

## Storage Architecture

### Unified Storage Interface
```java
public interface CloudStorageProvider {
    String upload(InputStream inputStream, String fileName, String contentType);
    InputStream download(String fileName);
    boolean delete(String fileName);
    String getPublicUrl(String fileName);
    boolean exists(String fileName);
    StorageMetadata getMetadata(String fileName);
}
```

### Provider Factory Pattern
```java
@Component
public class StorageProviderFactory {
    
    @Autowired
    private TencentCosProvider tencentCosProvider;
    
    @Autowired
    private AwsS3Provider awsS3Provider;
    
    @Autowired
    private AlibabaOssProvider alibabaOssProvider;
    
    public CloudStorageProvider getProvider(StorageProviderType type) {
        switch (type) {
            case TENCENT_COS:
                return tencentCosProvider;
            case AWS_S3:
                return awsS3Provider;
            case ALIBABA_OSS:
                return alibabaOssProvider;
            default:
                throw new IllegalArgumentException("Unsupported storage provider: " + type);
        }
    }
}
```

### Storage Configuration Schema
```sql
-- Storage provider configurations
CREATE TABLE storage_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_type VARCHAR(50) NOT NULL,
    region VARCHAR(100) NOT NULL,
    access_key_id VARCHAR(255) NOT NULL,
    access_key_secret VARCHAR(500) NOT NULL,
    bucket_name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500),
    custom_domain VARCHAR(500),
    use_https BOOLEAN DEFAULT TRUE,
    enable_cdn BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT FALSE,
    is_encrypted BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_provider_type (provider_type),
    INDEX idx_is_active (is_active)
);
```

## Configuration Management

### Environment Variables
```properties
# Default Storage Provider
STORAGE_PROVIDER=tencent_cos

# Tencent Cloud COS Configuration
TENCENT_COS_REGION=ap-beijing
TENCENT_COS_SECRET_ID=your-secret-id
TENCENT_COS_SECRET_KEY=your-secret-key
TENCENT_COS_BUCKET_NAME=your-bucket-name
TENCENT_COS_BASE_URL=https://your-bucket.cos.ap-beijing.myqcloud.com
TENCENT_COS_CDN_DOMAIN=https://your-cdn-domain.com

# AWS S3 Configuration
AWS_S3_REGION=us-east-1
AWS_S3_ACCESS_KEY_ID=your-access-key-id
AWS_S3_SECRET_ACCESS_KEY=your-secret-access-key
AWS_S3_BUCKET_NAME=your-bucket-name
AWS_S3_BASE_URL=https://your-bucket.s3.amazonaws.com
AWS_S3_CLOUDFRONT_DOMAIN=https://your-cloudfront-domain.com

# Alibaba Cloud OSS Configuration
ALIBABA_OSS_REGION=oss-cn-hangzhou
ALIBABA_OSS_ACCESS_KEY_ID=your-access-key-id
ALIBABA_OSS_ACCESS_KEY_SECRET=your-access-key-secret
ALIBABA_OSS_BUCKET_NAME=your-bucket-name
ALIBABA_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
ALIBABA_OSS_CDN_DOMAIN=https://your-cdn-domain.com
```

### Application Configuration
```yaml
storage:
  # Default provider
  default-provider: ${STORAGE_PROVIDER:tencent_cos}
  
  # Upload settings
  upload:
    max-file-size: 50MB
    allowed-types: jpg,jpeg,png,gif,webp,bmp
    generate-thumbnail: true
    thumbnail-size: 300x300
  
  # Provider configurations
  providers:
    tencent-cos:
      region: ${TENCENT_COS_REGION:ap-beijing}
      secret-id: ${TENCENT_COS_SECRET_ID}
      secret-key: ${TENCENT_COS_SECRET_KEY}
      bucket-name: ${TENCENT_COS_BUCKET_NAME}
      base-url: ${TENCENT_COS_BASE_URL}
      cdn-domain: ${TENCENT_COS_CDN_DOMAIN}
      use-https: true
      enable-cdn: true
      
    aws-s3:
      region: ${AWS_S3_REGION:us-east-1}
      access-key-id: ${AWS_S3_ACCESS_KEY_ID}
      secret-access-key: ${AWS_S3_SECRET_ACCESS_KEY}
      bucket-name: ${AWS_S3_BUCKET_NAME}
      base-url: ${AWS_S3_BASE_URL}
      cloudfront-domain: ${AWS_S3_CLOUDFRONT_DOMAIN}
      use-https: true
      enable-cdn: true
      
    alibaba-oss:
      region: ${ALIBABA_OSS_REGION:oss-cn-hangzhou}
      access-key-id: ${ALIBABA_OSS_ACCESS_KEY_ID}
      access-key-secret: ${ALIBABA_OSS_ACCESS_KEY_SECRET}
      bucket-name: ${ALIBABA_OSS_BUCKET_NAME}
      endpoint: ${ALIBABA_OSS_ENDPOINT}
      cdn-domain: ${ALIBABA_OSS_CDN_DOMAIN}
      use-https: true
      enable-cdn: true
```

## Provider Implementation

### Tencent Cloud COS Provider
```java
@Component
@ConditionalOnProperty(name = "storage.providers.tencent-cos.enabled", havingValue = "true", matchIfMissing = true)
public class TencentCosProvider implements CloudStorageProvider {
    
    private COSClient cosClient;
    private String bucketName;
    private String baseUrl;
    private String cdnDomain;
    
    @PostConstruct
    public void init() {
        StorageConfig config = getStorageConfig(StorageProviderType.TENCENT_COS);
        
        COSCredentials credentials = new BasicCOSCredentials(
            config.getAccessKeyId(), 
            config.getAccessKeySecret()
        );
        
        Region region = new Region(config.getRegion());
        ClientConfig clientConfig = new ClientConfig(region);
        clientConfig.setHttpProtocol(config.isUseHttps() ? HttpProtocol.https : HttpProtocol.http);
        
        this.cosClient = new COSClient(credentials, clientConfig);
        this.bucketName = config.getBucketName();
        this.baseUrl = config.getBaseUrl();
        this.cdnDomain = config.getCustomDomain();
    }
    
    @Override
    public String upload(InputStream inputStream, String fileName, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());
            
            PutObjectRequest request = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
            PutObjectResult result = cosClient.putObject(request);
            
            log.info("File uploaded to Tencent COS: {}, ETag: {}", fileName, result.getETag());
            return getPublicUrl(fileName);
            
        } catch (Exception e) {
            log.error("Failed to upload file to Tencent COS: {}", fileName, e);
            throw new StorageException("Upload failed: " + e.getMessage());
        }
    }
    
    @Override
    public InputStream download(String fileName) {
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, fileName);
            COSObject cosObject = cosClient.getObject(request);
            return cosObject.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to download file from Tencent COS: {}", fileName, e);
            throw new StorageException("Download failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean delete(String fileName) {
        try {
            cosClient.deleteObject(bucketName, fileName);
            log.info("File deleted from Tencent COS: {}", fileName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from Tencent COS: {}", fileName, e);
            return false;
        }
    }
    
    @Override
    public String getPublicUrl(String fileName) {
        if (cdnDomain != null && !cdnDomain.isEmpty()) {
            return cdnDomain + "/" + fileName;
        }
        return baseUrl + "/" + fileName;
    }
    
    @Override
    public boolean exists(String fileName) {
        try {
            return cosClient.doesObjectExist(bucketName, fileName);
        } catch (Exception e) {
            log.error("Failed to check file existence in Tencent COS: {}", fileName, e);
            return false;
        }
    }
    
    @Override
    public StorageMetadata getMetadata(String fileName) {
        try {
            ObjectMetadata metadata = cosClient.getObjectMetadata(bucketName, fileName);
            return StorageMetadata.builder()
                .fileName(fileName)
                .contentType(metadata.getContentType())
                .contentLength(metadata.getContentLength())
                .lastModified(metadata.getLastModified())
                .etag(metadata.getETag())
                .build();
        } catch (Exception e) {
            log.error("Failed to get metadata from Tencent COS: {}", fileName, e);
            throw new StorageException("Get metadata failed: " + e.getMessage());
        }
    }
}
```

### AWS S3 Provider
```java
@Component
@ConditionalOnProperty(name = "storage.providers.aws-s3.enabled", havingValue = "true")
public class AwsS3Provider implements CloudStorageProvider {
    
    private AmazonS3 s3Client;
    private String bucketName;
    private String baseUrl;
    private String cloudfrontDomain;
    
    @PostConstruct
    public void init() {
        StorageConfig config = getStorageConfig(StorageProviderType.AWS_S3);
        
        AWSCredentials credentials = new BasicAWSCredentials(
            config.getAccessKeyId(),
            config.getAccessKeySecret()
        );
        
        this.s3Client = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion(config.getRegion())
            .build();
            
        this.bucketName = config.getBucketName();
        this.baseUrl = config.getBaseUrl();
        this.cloudfrontDomain = config.getCustomDomain();
    }
    
    @Override
    public String upload(InputStream inputStream, String fileName, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());
            
            PutObjectRequest request = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
            request.setCannedAcl(CannedAccessControlList.PublicRead);
            
            PutObjectResult result = s3Client.putObject(request);
            log.info("File uploaded to AWS S3: {}, ETag: {}", fileName, result.getETag());
            return getPublicUrl(fileName);
            
        } catch (Exception e) {
            log.error("Failed to upload file to AWS S3: {}", fileName, e);
            throw new StorageException("Upload failed: " + e.getMessage());
        }
    }
    
    @Override
    public InputStream download(String fileName) {
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, fileName);
            S3Object s3Object = s3Client.getObject(request);
            return s3Object.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to download file from AWS S3: {}", fileName, e);
            throw new StorageException("Download failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean delete(String fileName) {
        try {
            s3Client.deleteObject(bucketName, fileName);
            log.info("File deleted from AWS S3: {}", fileName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from AWS S3: {}", fileName, e);
            return false;
        }
    }
    
    @Override
    public String getPublicUrl(String fileName) {
        if (cloudfrontDomain != null && !cloudfrontDomain.isEmpty()) {
            return cloudfrontDomain + "/" + fileName;
        }
        return baseUrl + "/" + fileName;
    }
    
    @Override
    public boolean exists(String fileName) {
        try {
            return s3Client.doesObjectExist(bucketName, fileName);
        } catch (Exception e) {
            log.error("Failed to check file existence in AWS S3: {}", fileName, e);
            return false;
        }
    }
    
    @Override
    public StorageMetadata getMetadata(String fileName) {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, fileName);
            return StorageMetadata.builder()
                .fileName(fileName)
                .contentType(metadata.getContentType())
                .contentLength(metadata.getContentLength())
                .lastModified(metadata.getLastModified())
                .etag(metadata.getETag())
                .build();
        } catch (Exception e) {
            log.error("Failed to get metadata from AWS S3: {}", fileName, e);
            throw new StorageException("Get metadata failed: " + e.getMessage());
        }
    }
}
```

### Alibaba Cloud OSS Provider
```java
@Component
@ConditionalOnProperty(name = "storage.providers.alibaba-oss.enabled", havingValue = "true")
public class AlibabaOssProvider implements CloudStorageProvider {
    
    private OSS ossClient;
    private String bucketName;
    private String endpoint;
    private String cdnDomain;
    
    @PostConstruct
    public void init() {
        StorageConfig config = getStorageConfig(StorageProviderType.ALIBABA_OSS);
        
        this.ossClient = new OSSClientBuilder().build(
            config.getBaseUrl(),
            config.getAccessKeyId(),
            config.getAccessKeySecret()
        );
        
        this.bucketName = config.getBucketName();
        this.endpoint = config.getBaseUrl();
        this.cdnDomain = config.getCustomDomain();
    }
    
    @Override
    public String upload(InputStream inputStream, String fileName, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(inputStream.available());
            
            PutObjectRequest request = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
            PutObjectResult result = ossClient.putObject(request);
            
            log.info("File uploaded to Alibaba OSS: {}, ETag: {}", fileName, result.getETag());
            return getPublicUrl(fileName);
            
        } catch (Exception e) {
            log.error("Failed to upload file to Alibaba OSS: {}", fileName, e);
            throw new StorageException("Upload failed: " + e.getMessage());
        }
    }
    
    @Override
    public InputStream download(String fileName) {
        try {
            GetObjectRequest request = new GetObjectRequest(bucketName, fileName);
            OSSObject ossObject = ossClient.getObject(request);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to download file from Alibaba OSS: {}", fileName, e);
            throw new StorageException("Download failed: " + e.getMessage());
        }
    }
    
    @Override
    public boolean delete(String fileName) {
        try {
            ossClient.deleteObject(bucketName, fileName);
            log.info("File deleted from Alibaba OSS: {}", fileName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from Alibaba OSS: {}", fileName, e);
            return false;
        }
    }
    
    @Override
    public String getPublicUrl(String fileName) {
        if (cdnDomain != null && !cdnDomain.isEmpty()) {
            return cdnDomain + "/" + fileName;
        }
        return endpoint + "/" + bucketName + "/" + fileName;
    }
    
    @Override
    public boolean exists(String fileName) {
        try {
            return ossClient.doesObjectExist(bucketName, fileName);
        } catch (Exception e) {
            log.error("Failed to check file existence in Alibaba OSS: {}", fileName, e);
            return false;
        }
    }
    
    @Override
    public StorageMetadata getMetadata(String fileName) {
        try {
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucketName, fileName);
            return StorageMetadata.builder()
                .fileName(fileName)
                .contentType(metadata.getContentType())
                .contentLength(metadata.getContentLength())
                .lastModified(metadata.getLastModified())
                .etag(metadata.getETag())
                .build();
        } catch (Exception e) {
            log.error("Failed to get metadata from Alibaba OSS: {}", fileName, e);
            throw new StorageException("Get metadata failed: " + e.getMessage());
        }
    }
}
```

## Performance Optimization

### 1. Connection Pooling
```yaml
storage:
  connection-pool:
    max-connections: 50
    connection-timeout: 30000
    socket-timeout: 30000
    max-error-retry: 3
```

### 2. CDN Integration
- Enable CDN for faster content delivery
- Configure custom domains for better branding
- Implement cache headers for optimal performance

### 3. Multipart Upload
```java
public String uploadLargeFile(InputStream inputStream, String fileName, long fileSize) {
    if (fileSize > MULTIPART_THRESHOLD) {
        return uploadMultipart(inputStream, fileName, fileSize);
    } else {
        return upload(inputStream, fileName, "application/octet-stream");
    }
}
```

### 4. Async Operations
```java
@Async
public CompletableFuture<String> uploadAsync(InputStream inputStream, String fileName, String contentType) {
    return CompletableFuture.completedFuture(upload(inputStream, fileName, contentType));
}
```

## Security Considerations

### 1. Access Control
- Use IAM roles and policies for fine-grained access control
- Implement bucket policies to restrict access
- Enable server-side encryption

### 2. Credential Management
- Store credentials securely using encryption
- Rotate access keys regularly
- Use temporary credentials when possible

### 3. Network Security
- Enable HTTPS for all communications
- Use VPC endpoints for private network access
- Implement request signing for API calls

### 4. Data Protection
```java
// Enable server-side encryption
ObjectMetadata metadata = new ObjectMetadata();
metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
```

## Migration Between Providers

### Migration Strategy
1. **Dual Write Phase**: Write to both old and new providers
2. **Data Sync Phase**: Sync existing data to new provider
3. **Read Switch Phase**: Switch reads to new provider
4. **Cleanup Phase**: Remove old provider data

### Migration Tool
```java
@Service
public class StorageMigrationService {
    
    public void migrateProvider(StorageProviderType from, StorageProviderType to) {
        CloudStorageProvider sourceProvider = storageProviderFactory.getProvider(from);
        CloudStorageProvider targetProvider = storageProviderFactory.getProvider(to);
        
        List<String> files = getAllFiles(sourceProvider);
        
        for (String fileName : files) {
            try (InputStream inputStream = sourceProvider.download(fileName)) {
                StorageMetadata metadata = sourceProvider.getMetadata(fileName);
                targetProvider.upload(inputStream, fileName, metadata.getContentType());
                
                // Update database records
                updateImageStorageProvider(fileName, to);
                
                log.info("Migrated file: {} from {} to {}", fileName, from, to);
            } catch (Exception e) {
                log.error("Failed to migrate file: {}", fileName, e);
            }
        }
    }
}
```

## Monitoring and Logging

### Metrics Collection
```java
@Component
public class StorageMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordUpload(StorageProviderType provider, long duration, boolean success) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("storage.upload")
            .tag("provider", provider.name())
            .tag("success", String.valueOf(success))
            .register(meterRegistry));
    }
}
```

### Health Checks
```java
@Component
public class StorageHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            CloudStorageProvider provider = getCurrentProvider();
            boolean isHealthy = provider.exists("health-check.txt");
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("provider", getCurrentProviderType())
                    .withDetail("status", "Connected")
                    .build();
            } else {
                return Health.down()
                    .withDetail("provider", getCurrentProviderType())
                    .withDetail("status", "Connection failed")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

This comprehensive guide provides everything needed to implement multi-cloud storage integration with proper configuration management, security, and performance optimization.