package com.uka.image.controller;

import com.uka.image.dto.ApiResponse;
import com.uka.image.dto.StorageConfig;
import com.uka.image.dto.StorageConfig.StorageProviderType;
import com.uka.image.service.StorageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Storage Configuration Controller
 * Provides REST API endpoints for managing storage provider configurations
 */
@RestController
@RequestMapping("/api/admin/config")
@CrossOrigin(origins = "*")
public class StorageConfigController {
    
    @Autowired
    private StorageConfigService storageConfigService;
    
    /**
     * Get all storage configurations
     * @return List of storage configurations
     */
    @GetMapping("/storage")
    public ApiResponse<List<StorageConfig>> getAllStorageConfigurations() {
        try {
            List<StorageConfig> configurations = storageConfigService.getAllConfigurations();
            return ApiResponse.success(configurations);
        } catch (Exception e) {
            return ApiResponse.error("Failed to get storage configurations: " + e.getMessage());
        }
    }
    
    /**
     * Get storage configuration by provider type
     * @param providerType Storage provider type
     * @return Storage configuration
     */
    @GetMapping("/storage/{providerType}")
    public ApiResponse<StorageConfig> getStorageConfiguration(@PathVariable String providerType) {
        try {
            StorageProviderType type = StorageProviderType.fromKey(providerType);
            StorageConfig configuration = storageConfigService.getConfiguration(type);
            return ApiResponse.success(configuration);
        } catch (Exception e) {
            return ApiResponse.error("Failed to get storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get active storage configuration
     * @return Active storage configuration
     */
    @GetMapping("/storage/active")
    public ApiResponse<StorageConfig> getActiveStorageConfiguration() {
        try {
            StorageConfig configuration = storageConfigService.getActiveConfiguration();
            return ApiResponse.success(configuration);
        } catch (Exception e) {
            return ApiResponse.error("Failed to get active storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Test storage connection
     * @param config Storage configuration to test
     * @return Connection test result
     */
    @PostMapping("/storage/test")
    public ApiResponse<String> testStorageConnection(@RequestBody StorageConfig config) {
        try {
            boolean isConnected = storageConfigService.testConnection(config);
            if (isConnected) {
                return ApiResponse.success("Storage connection successful");
            } else {
                return ApiResponse.error("Storage connection failed");
            }
        } catch (Exception e) {
            return ApiResponse.error("Connection test failed: " + e.getMessage());
        }
    }
    
    /**
     * Update storage configuration
     * @param config New storage configuration
     * @return Update result
     */
    @PostMapping("/storage")
    public ApiResponse<String> updateStorageConfiguration(@RequestBody StorageConfig config) {
        try {
            storageConfigService.updateConfiguration(config);
            return ApiResponse.success("Storage configuration updated successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to update storage configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get supported storage providers
     * @return List of supported providers with regions
     */
    @GetMapping("/storage/providers")
    public ApiResponse<List<Map<String, Object>>> getSupportedProviders() {
        try {
            List<Map<String, Object>> providers = storageConfigService.getSupportedProviders();
            return ApiResponse.success(providers);
        } catch (Exception e) {
            return ApiResponse.error("Failed to get supported providers: " + e.getMessage());
        }
    }
    
    /**
     * Activate storage provider
     * @param providerType Provider type to activate
     * @return Activation result
     */
    @PostMapping("/storage/{providerType}/activate")
    public ApiResponse<String> activateStorageProvider(@PathVariable String providerType) {
        try {
            StorageProviderType type = StorageProviderType.fromKey(providerType);
            StorageConfig config = storageConfigService.getConfiguration(type);
            config.setActive(true);
            storageConfigService.updateConfiguration(config);
            return ApiResponse.success("Storage provider activated successfully");
        } catch (Exception e) {
            return ApiResponse.error("Failed to activate storage provider: " + e.getMessage());
        }
    }
}