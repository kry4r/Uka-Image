<template>
  <div class="storage-config">
    <div class="config-header">
      <h2>Storage Configuration</h2>
      <p>Configure multi-cloud storage providers (Tencent COS, AWS S3, Alibaba OSS)</p>
    </div>
    
    <div class="config-content">
      <!-- Provider Selection -->
      <div class="provider-selection">
        <h3>Select Storage Provider</h3>
        <div class="provider-tabs">
          <button
            v-for="provider in supportedProviders"
            :key="provider.key"
            @click="selectProvider(provider.key)"
            :class="['provider-tab', { active: selectedProvider === provider.key }]"
          >
            <div class="provider-info">
              <span class="provider-name">{{ provider.name }}</span>
              <span class="provider-desc">{{ provider.description }}</span>
            </div>
          </button>
        </div>
      </div>

      <!-- Active Configuration Status -->
      <div v-if="activeConfig" class="status-card">
        <div class="status-header">
          <h3>Current Active Provider</h3>
          <span class="status-badge active">{{ getProviderName(activeConfig.provider) }}</span>
        </div>
        <div class="status-info">
          <div class="status-details">
            <div class="detail-item">
              <span class="label">Region:</span>
              <span class="value">{{ activeConfig.region }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Bucket:</span>
              <span class="value">{{ activeConfig.bucketName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">CDN:</span>
              <span class="value">{{ activeConfig.enableCdn ? 'Enabled' : 'Disabled' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Configuration Form -->
      <form @submit.prevent="saveConfiguration" class="config-form">
        <div class="form-section">
          <h3>{{ getProviderName(selectedProvider) }} Configuration</h3>
          
          <div class="form-row">
            <div class="form-group">
              <label for="region">Region *</label>
              <select id="region" v-model="config.region" required>
                <option value="">Select Region</option>
                <option
                  v-for="region in currentProviderRegions"
                  :key="region.key"
                  :value="region.key"
                >
                  {{ region.name }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label for="bucketName">Bucket Name *</label>
              <input
                id="bucketName"
                v-model="config.bucketName"
                type="text"
                placeholder="your-bucket-name"
                required
              />
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label for="accessKeyId">Access Key ID *</label>
              <input
                id="accessKeyId"
                v-model="config.accessKeyId"
                type="text"
                placeholder="Your access key ID"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="accessKeySecret">Access Key Secret *</label>
              <input
                id="accessKeySecret"
                v-model="config.accessKeySecret"
                type="password"
                placeholder="Your access key secret"
                required
              />
            </div>
          </div>
          
          <div class="form-group">
            <label for="baseUrl">Base URL</label>
            <input
              id="baseUrl"
              v-model="config.baseUrl"
              type="url"
              placeholder="https://your-bucket.provider-domain.com"
            />
            <small class="form-help">Leave empty to use default provider URL</small>
          </div>
          
          <div class="form-group">
            <label for="customDomain">Custom Domain (CDN)</label>
            <input
              id="customDomain"
              v-model="config.customDomain"
              type="url"
              placeholder="https://cdn.yourdomain.com"
            />
            <small class="form-help">Optional: Custom domain for CDN acceleration</small>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label class="checkbox-label">
                <input
                  v-model="config.useHttps"
                  type="checkbox"
                />
                Use HTTPS
              </label>
            </div>
            
            <div class="form-group">
              <label class="checkbox-label">
                <input
                  v-model="config.enableCdn"
                  type="checkbox"
                />
                Enable CDN
              </label>
            </div>
          </div>
          
          <div class="form-group">
            <label class="checkbox-label">
              <input
                v-model="config.isActive"
                type="checkbox"
              />
              Set as Active Provider
            </label>
            <small class="form-help">This will deactivate other providers</small>
          </div>
        </div>
        
        <div class="form-actions">
          <button
            type="button"
            @click="testConnection"
            :disabled="testing"
            class="btn-test"
          >
            <i v-if="testing" class="icon-loading"></i>
            {{ testing ? 'Testing...' : 'Test Connection' }}
          </button>
          
          <button
            type="submit"
            :disabled="saving"
            class="btn-save"
          >
            <i v-if="saving" class="icon-loading"></i>
            {{ saving ? 'Saving...' : 'Save Configuration' }}
          </button>
        </div>
      </form>
      
      <div v-if="testResult" class="test-result" :class="testResult.type">
        <i :class="testResult.type === 'success' ? 'icon-check' : 'icon-error'"></i>
        {{ testResult.message }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { storageConfigApi, type StorageConfig } from '@/api/config'

interface StorageProvider {
  key: string
  name: string
  description: string
  regions: Array<{ key: string; name: string }>
}

const selectedProvider = ref<string>('tencent_cos')
const supportedProviders = ref<StorageProvider[]>([])
const activeConfig = ref<StorageConfig | null>(null)
const testing = ref(false)
const saving = ref(false)
const testResult = ref<{ type: 'success' | 'error', message: string } | null>(null)

const config = ref<StorageConfig>({
  provider: 'tencent_cos' as any,
  region: '',
  accessKeyId: '',
  accessKeySecret: '',
  bucketName: '',
  baseUrl: '',
  customDomain: '',
  useHttps: true,
  enableCdn: false,
  isActive: false
})

const currentProviderRegions = computed(() => {
  const provider = supportedProviders.value.find(p => p.key === selectedProvider.value)
  return provider?.regions || []
})

onMounted(async () => {
  await loadSupportedProviders()
  await loadActiveConfiguration()
  await loadConfiguration(selectedProvider.value)
})

watch(selectedProvider, async (newProvider) => {
  await loadConfiguration(newProvider)
  config.value.provider = newProvider as any
  config.value.region = ''
})

const loadSupportedProviders = async () => {
  try {
    const response = await storageConfigApi.getProviders()
    if (response.success && response.data) {
      supportedProviders.value = response.data
    }
  } catch (error) {
    console.error('Failed to load supported providers:', error)
  }
}

const loadActiveConfiguration = async () => {
  try {
    const response = await storageConfigApi.getConfig()
    if (response.success && response.data) {
      // Assuming the API returns the active configuration
      const configs = Array.isArray(response.data) ? response.data : [response.data]
      activeConfig.value = configs.find(c => c.isActive) || null
    }
  } catch (error) {
    console.error('Failed to load active configuration:', error)
  }
}

const loadConfiguration = async (providerKey: string) => {
  try {
    const response = await storageConfigApi.getConfig()
    if (response.success && response.data) {
      const configs = Array.isArray(response.data) ? response.data : [response.data]
      const providerConfig = configs.find(c => c.provider === providerKey)
      
      if (providerConfig) {
        config.value = { ...config.value, ...providerConfig }
      } else {
        // Reset to default values for new provider
        config.value = {
          provider: providerKey as any,
          region: '',
          accessKeyId: '',
          accessKeySecret: '',
          bucketName: '',
          baseUrl: '',
          customDomain: '',
          useHttps: true,
          enableCdn: false,
          isActive: false
        }
      }
    }
  } catch (error) {
    console.error('Failed to load configuration:', error)
  }
}

const selectProvider = (providerKey: string) => {
  selectedProvider.value = providerKey
}

const getProviderName = (providerKey: string) => {
  const provider = supportedProviders.value.find(p => p.key === providerKey)
  return provider?.name || providerKey
}

const testConnection = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    const response = await storageConfigApi.testConnection(config.value)
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
    const response = await storageConfigApi.updateConfig(config.value)
    if (response.success) {
      testResult.value = {
        type: 'success',
        message: response.message || 'Configuration saved successfully'
      }
      
      // Reload active configuration if this was set as active
      if (config.value.isActive) {
        await loadActiveConfiguration()
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
.storage-config {
  max-width: 900px;
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
  font-size: 28px;
}

.config-header p {
  color: #666;
  font-size: 16px;
}

.provider-selection {
  margin-bottom: 30px;
}

.provider-selection h3 {
  color: #333;
  margin-bottom: 15px;
}

.provider-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 15px;
}

.provider-tab {
  padding: 20px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s;
  text-align: left;
}

.provider-tab:hover {
  border-color: #007bff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.15);
}

.provider-tab.active {
  border-color: #007bff;
  background: #f8f9ff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.25);
}

.provider-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.provider-name {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.provider-desc {
  font-size: 14px;
  color: #666;
}

.status-card {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.status-header h3 {
  margin: 0;
  color: #333;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.status-badge.active {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.detail-item {
  display: flex;
  gap: 10px;
}

.detail-item .label {
  font-weight: 500;
  min-width: 60px;
}

.detail-item .value {
  color: #666;
}

.config-form {
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.form-section {
  margin-bottom: 30px;
}

.form-section h3 {
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e9ecef;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

.form-group input,
.form-group select {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.form-help {
  margin-top: 5px;
  font-size: 12px;
  color: #666;
}

.checkbox-label {
  flex-direction: row !important;
  align-items: center;
  cursor: pointer;
  margin-top: 8px;
}

.checkbox-label input {
  width: auto;
  margin-right: 8px;
  margin-bottom: 0;
}

.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
}

.btn-test,
.btn-save {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
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
  padding: 15px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
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

.icon-loading {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top: 2px solid transparent;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .provider-tabs {
    grid-template-columns: 1fr;
  }
  
  .status-details {
    grid-template-columns: 1fr;
  }
}
</style>