package com.uka.image.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tencent Cloud COS configuration
 * 
 * @author Uka Team
 */
@Configuration
@ConfigurationProperties(prefix = "cos")
@Data
public class CosConfig {
    
    private String region;
    private String secretId;
    private String secretKey;
    private String bucketName;
    private String baseUrl;

    @Bean
    public COSClient cosClient() {
        // Initialize COS credentials
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        
        // Configure COS region
        Region regionConfig = new Region(region);
        ClientConfig clientConfig = new ClientConfig(regionConfig);
        
        // Generate COS client
        return new COSClient(cred, clientConfig);
    }
}