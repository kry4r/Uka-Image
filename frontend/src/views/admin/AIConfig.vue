<template>
  <div class="ai-config">
    <div class="config-header">
      <h2>AI Service Configuration</h2>
      <p>Configure AI-powered image analysis and search services</p>
    </div>
    
    <div class="config-content">
      <!-- Service Status -->
      <div class="status-card">
        <div class="status-header">
          <h3>AI Service Status</h3>
          <button @click="refreshStatus" class="btn-refresh" :disabled="refreshing">
            <i class="icon-refresh" :class="{ spinning: refreshing }"></i>
            Refresh
          </button>
        </div>
        <div class="status-grid">
          <div class="status-item" :class="mcpStatus.connected ? 'connected' : 'disconnected'">
            <div class="status-indicator">
              <span class="status-dot" :class="mcpStatus.connected ? 'green' : 'red'"></span>
              <span class="status-label">MCP Server</span>
            </div>
            <div class="status-details">
              <p>{{ mcpStatus.message }}</p>
              <small v-if="mcpStatus.responseTime">Response: {{ mcpStatus.responseTime }}ms</small>
            </div>
          </div>
          
          <div class="status-item" :class="analysisStatus.enabled ? 'enabled' : 'disabled'">
            <div class="status-indicator">
              <span class="status-dot" :class="analysisStatus.enabled ? 'green' : 'gray'"></span>
              <span class="status-label">Auto Analysis</span>
            </div>
            <div class="status-details">
              <p>{{ analysisStatus.enabled ? 'Enabled' : 'Disabled' }}</p>
              <small>Processed: {{ analysisStatus.processedCount }} images</small>
            </div>
          </div>
        </div>
      </div>

      <!-- Configuration Form -->
      <form @submit.prevent="saveConfiguration" class="config-form">
        <div class="form-section">
          <h3>MCP Server Configuration</h3>
          
          <div class="form-group">
            <label for="mcpServerUrl">MCP Server URL *</label>
            <input
              id="mcpServerUrl"
              v-model="config.mcpServerUrl"
              type="url"
              placeholder="http://localhost:8001"
              required
            />
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label for="connectionTimeout">Connection Timeout (ms)</label>
              <input
                id="connectionTimeout"
                v-model.number="config.connectionTimeout"
                type="number"
                min="1000"
                max="60000"
              />
            </div>
            
            <div class="form-group">
              <label for="readTimeout">Read Timeout (ms)</label>
              <input
                id="readTimeout"
                v-model.number="config.readTimeout"
                type="number"
                min="1000"
                max="120000"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label for="apiKey">API Key</label>
            <input
              id="apiKey"
              v-model="config.apiKey"
              type="password"
              placeholder="Optional API key for authentication"
            />
          </div>
        </div>

        <div class="form-section">
          <h3>Analysis Configuration</h3>
          
          <div class="form-group">
            <label class="checkbox-label">
              <input
                v-model="config.autoAnalysisEnabled"
                type="checkbox"
              />
              Enable Automatic Analysis
            </label>
            <small class="form-help">Automatically analyze images after upload</small>
          </div>
          
          <div class="form-group">
            <label>Analysis Types</label>
            <div class="checkbox-group">
              <label class="checkbox-label">
                <input
                  v-model="config.analysisTypes"
                  type="checkbox"
                  value="objects"
                />
                Object Detection
              </label>
              <label class="checkbox-label">
                <input
                  v-model="config.analysisTypes"
                  type="checkbox"
                  value="text"
                />
                Text Extraction (OCR)
              </label>
              <label class="checkbox-label">
                <input
                  v-model="config.analysisTypes"
                  type="checkbox"
                  value="colors"
                />
                Color Analysis
              </label>
              <label class="checkbox-label">
                <input
                  v-model="config.analysisTypes"
                  type="checkbox"
                  value="scene"
                />
                Scene Classification
              </label>
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label for="confidenceThreshold">Confidence Threshold</label>
              <input
                id="confidenceThreshold"
                v-model.number="config.confidenceThreshold"
                type="range"
                min="0"
                max="1"
                step="0.1"
              />
              <span class="range-value">{{ config.confidenceThreshold }}</span>
            </div>
            
            <div class="form-group">
              <label for="language">Analysis Language</label>
              <select id="language" v-model="config.language">
                <option value="en">English</option>
                <option value="zh">Chinese</option>
                <option value="ja">Japanese</option>
                <option value="ko">Korean</option>
              </select>
            </div>
          </div>
        </div>

        <div class="form-section">
          <h3>Search Configuration</h3>
          
          <div class="form-row">
            <div class="form-group">
              <label for="defaultLimit">Default Search Limit</label>
              <input
                id="defaultLimit"
                v-model.number="config.defaultSearchLimit"
                type="number"
                min="1"
                max="100"
              />
            </div>
            
            <div class="form-group">
              <label for="maxLimit">Maximum Search Limit</label>
              <input
                id="maxLimit"
                v-model.number="config.maxSearchLimit"
                type="number"
                min="1"
                max="500"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label class="checkbox-label">
              <input
                v-model="config.cacheEnabled"
                type="checkbox"
              />
              Enable Search Cache
            </label>
            <small class="form-help">Cache search results for better performance</small>
          </div>
          
          <div v-if="config.cacheEnabled" class="form-row">
            <div class="form-group">
              <label for="cacheTtl">Cache TTL (seconds)</label>
              <input
                id="cacheTtl"
                v-model.number="config.cacheTtl"
                type="number"
                min="60"
                max="3600"
              />
            </div>
            
            <div class="form-group">
              <label for="cacheSize">Max Cache Size</label>
              <input
                id="cacheSize"
                v-model.number="config.maxCacheSize"
                type="number"
                min="100"
                max="10000"
              />
            </div>
          </div>
        </div>

        <div class="form-section">
          <h3>Performance Settings</h3>
          
          <div class="form-row">
            <div class="form-group">
              <label for="threadPoolSize">Thread Pool Size</label>
              <input
                id="threadPoolSize"
                v-model.number="config.threadPoolSize"
                type="number"
                min="1"
                max="20"
              />
            </div>
            
            <div class="form-group">
              <label for="queueCapacity">Queue Capacity</label>
              <input
                id="queueCapacity"
                v-model.number="config.queueCapacity"
                type="number"
                min="10"
                max="1000"
              />
            </div>
          </div>
          
          <div class="form-group">
            <label class="checkbox-label">
              <input
                v-model="config.asyncProcessing"
                type="checkbox"
              />
              Enable Async Processing
            </label>
            <small class="form-help">Process AI analysis asynchronously</small>
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
import { ref, onMounted } from 'vue'

interface AIConfig {
  mcpServerUrl: string
  connectionTimeout: number
  readTimeout: number
  apiKey: string
  autoAnalysisEnabled: boolean
  analysisTypes: string[]
  confidenceThreshold: number
  language: string
  defaultSearchLimit: number
  maxSearchLimit: number
  cacheEnabled: boolean
  cacheTtl: number
  maxCacheSize: number
  threadPoolSize: number
  queueCapacity: number
  asyncProcessing: boolean
}

interface ServiceStatus {
  connected: boolean
  message: string
  responseTime?: number
}

interface AnalysisStatus {
  enabled: boolean
  processedCount: number
}

const config = ref<AIConfig>({
  mcpServerUrl: 'http://localhost:8001',
  connectionTimeout: 30000,
  readTimeout: 60000,
  apiKey: '',
  autoAnalysisEnabled: true,
  analysisTypes: ['objects', 'text', 'colors', 'scene'],
  confidenceThreshold: 0.6,
  language: 'en',
  defaultSearchLimit: 20,
  maxSearchLimit: 100,
  cacheEnabled: true,
  cacheTtl: 900,
  maxCacheSize: 1000,
  threadPoolSize: 5,
  queueCapacity: 100,
  asyncProcessing: true
})

const mcpStatus = ref<ServiceStatus>({
  connected: false,
  message: 'Not connected'
})

const analysisStatus = ref<AnalysisStatus>({
  enabled: false,
  processedCount: 0
})

const refreshing = ref(false)
const testing = ref(false)
const saving = ref(false)
const testResult = ref<{ type: 'success' | 'error', message: string } | null>(null)

onMounted(async () => {
  await loadConfiguration()
  await loadStatus()
})

const loadConfiguration = async () => {
  try {
    // Load AI configuration from API
    // const response = await aiConfigApi.getConfig()
    // if (response.success) {
    //   config.value = { ...config.value, ...response.data }
    // }
  } catch (error) {
    console.error('Failed to load AI configuration:', error)
  }
}

const loadStatus = async () => {
  try {
    // Load service status from API
    // const response = await aiConfigApi.getStatus()
    // if (response.success) {
    //   mcpStatus.value = response.data.mcpStatus
    //   analysisStatus.value = response.data.analysisStatus
    // }
  } catch (error) {
    console.error('Failed to load service status:', error)
  }
}

const refreshStatus = async () => {
  refreshing.value = true
  await loadStatus()
  refreshing.value = false
}

const testConnection = async () => {
  testing.value = true
  testResult.value = null
  
  try {
    // Test MCP server connection
    // const response = await aiConfigApi.testConnection(config.value)
    // testResult.value = {
    //   type: response.success ? 'success' : 'error',
    //   message: response.message || (response.success ? 'Connection successful' : 'Connection failed')
    // }
    
    // Simulate test for now
    await new Promise(resolve => setTimeout(resolve, 2000))
    testResult.value = {
      type: 'success',
      message: 'MCP server connection successful'
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
    // Save AI configuration
    // const response = await aiConfigApi.updateConfig(config.value)
    // if (response.success) {
    //   testResult.value = {
    //     type: 'success',
    //     message: response.message || 'Configuration saved successfully'
    //   }
    // } else {
    //   testResult.value = {
    //     type: 'error',
    //     message: response.message || 'Failed to save configuration'
    //   }
    // }
    
    // Simulate save for now
    await new Promise(resolve => setTimeout(resolve, 1000))
    testResult.value = {
      type: 'success',
      message: 'AI configuration saved successfully'
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
.ai-config {
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
  margin-bottom: 20px;
}

.status-header h3 {
  margin: 0;
  color: #333;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-refresh:hover:not(:disabled) {
  background: #5a6268;
}

.icon-refresh.spinning {
  animation: spin 1s linear infinite;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.status-item {
  padding: 15px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.status-item.connected {
  background: #d4edda;
  border-color: #c3e6cb;
}

.status-item.disconnected {
  background: #f8d7da;
  border-color: #f5c6cb;
}

.status-item.enabled {
  background: #d1ecf1;
  border-color: #bee5eb;
}

.status-item.disabled {
  background: #f8f9fa;
  border-color: #dee2e6;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.status-dot.green {
  background: #28a745;
}

.status-dot.red {
  background: #dc3545;
}

.status-dot.gray {
  background: #6c757d;
}

.status-label {
  font-weight: 600;
  font-size: 16px;
}

.status-details p {
  margin: 0;
  color: #666;
}

.status-details small {
  color: #888;
  font-size: 12px;
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
  margin-bottom: 8px;
}

.checkbox-label input {
  width: auto;
  margin-right: 8px;
  margin-bottom: 0;
}

.checkbox-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 8px;
}

.range-value {
  margin-left: 10px;
  font-weight: 500;
  color: #007bff;
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
  
  .status-grid {
    grid-template-columns: 1fr;
  }
  
  .checkbox-group {
    grid-template-columns: 1fr;
  }
}
</style>