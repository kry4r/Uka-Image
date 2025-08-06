<template>
  <div class="database-config">
    <div class="config-header">
      <h2>Database Configuration</h2>
      <p>Configure remote MySQL database connection</p>
    </div>
    
    <div class="config-content">
      <!-- Connection Status -->
      <div class="status-card" v-if="connectionStatus">
        <div class="status-header">
          <h3>Connection Status</h3>
          <button @click="refreshStatus" class="btn-refresh" :disabled="refreshing">
            <i class="icon-refresh" :class="{ spinning: refreshing }"></i>
            Refresh
          </button>
        </div>
        <div class="status-info" :class="connectionStatus.connected ? 'connected' : 'disconnected'">
          <div class="status-indicator">
            <span class="status-dot" :class="connectionStatus.connected ? 'green' : 'red'"></span>
            <span class="status-text">{{ connectionStatus.status }}</span>
          </div>
          <p class="status-message">{{ connectionStatus.message }}</p>
          <div v-if="connectionStatus.connected" class="connection-details">
            <div class="detail-item">
              <span class="label">Database:</span>
              <span class="value">{{ connectionStatus.database }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Driver:</span>
              <span class="value">{{ connectionStatus.driver }} {{ connectionStatus.driverVersion }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Configuration Form -->
      <form @submit.prevent="saveConfiguration" class="config-form">
        <div class="form-section">
          <h3>Basic Configuration</h3>
          
          <div class="form-row">
            <div class="form-group">
              <label for="host">Database Host *</label>
              <input
                id="host"
                v-model="config.host"
                type="text"
                placeholder="e.g., mysql.example.com"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="port">Port *</label>
              <input
                id="port"
                v-model.number="config.port"
                type="number"
                placeholder="3306"
                min="1"
                max="65535"
                required
              />
            </div>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label for="database">Database Name *</label>
              <input
                id="database"
                v-model="config.database"
                type="text"
                placeholder="uka_image_db"
                required
              />
            </div>
            
            <div class="form-group">
              <label for="username">Username *</label>
              <input
                id="username"
                v-model="config.username"
                type="text"
                placeholder="database username"
                required
              />
            </div>
          </div>
          
          <div class="form-group">
            <label for="password">Password *</label>
            <input
              id="password"
              v-model="config.password"
              type="password"
              placeholder="database password"
              required
            />
          </div>
          
          <div class="form-row">
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
                <option value="Asia/Tokyo">Asia/Tokyo</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Advanced Configuration -->
        <div class="form-section">
          <div class="section-header">
            <h3>Connection Pool Settings</h3>
            <button
              type="button"
              @click="showAdvanced = !showAdvanced"
              class="btn-toggle"
            >
              {{ showAdvanced ? 'Hide' : 'Show' }} Advanced
            </button>
          </div>
          
          <div v-show="showAdvanced" class="advanced-config">
            <div class="form-row">
              <div class="form-group">
                <label for="minIdle">Minimum Idle Connections</label>
                <input
                  id="minIdle"
                  v-model.number="config.minIdle"
                  type="number"
                  min="1"
                  max="100"
                />
              </div>
              
              <div class="form-group">
                <label for="maxActive">Maximum Active Connections</label>
                <input
                  id="maxActive"
                  v-model.number="config.maxActive"
                  type="number"
                  min="1"
                  max="200"
                />
              </div>
            </div>
            
            <div class="form-row">
              <div class="form-group">
                <label for="maxWait">Max Wait Time (ms)</label>
                <input
                  id="maxWait"
                  v-model.number="config.maxWait"
                  type="number"
                  min="1000"
                />
              </div>
              
              <div class="form-group">
                <label for="connectionTimeout">Connection Timeout (ms)</label>
                <input
                  id="connectionTimeout"
                  v-model.number="config.connectionTimeout"
                  type="number"
                  min="1000"
                />
              </div>
            </div>
            
            <div class="form-group">
              <label for="validationQuery">Validation Query</label>
              <input
                id="validationQuery"
                v-model="config.validationQuery"
                type="text"
                placeholder="SELECT 1"
              />
            </div>
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
import { databaseConfigApi, type DatabaseConfig, type DatabaseStatus } from '@/api/config'

const config = ref<DatabaseConfig>({
  host: '',
  port: 3306,
  database: 'uka_image_db',
  username: '',
  password: '',
  useSSL: false,
  serverTimezone: 'UTC',
  minIdle: 5,
  maxActive: 20,
  maxWait: 60000,
  validationQuery: 'SELECT 1',
  connectionTimeout: 30000,
  idleTimeout: 600000,
  maxLifetime: 1800000
})

const connectionStatus = ref<DatabaseStatus | null>(null)
const showAdvanced = ref(false)
const testing = ref(false)
const saving = ref(false)
const refreshing = ref(false)
const testResult = ref<{ type: 'success' | 'error', message: string } | null>(null)

onMounted(async () => {
  await loadConfiguration()
  await loadConnectionStatus()
})

const loadConfiguration = async () => {
  try {
    const response = await databaseConfigApi.getConfig()
    if (response.success && response.data) {
      config.value = { ...config.value, ...response.data }
    }
  } catch (error) {
    console.error('Failed to load configuration:', error)
  }
}

const loadConnectionStatus = async () => {
  try {
    const response = await databaseConfigApi.getStatus()
    if (response.success && response.data) {
      connectionStatus.value = response.data
    }
  } catch (error) {
    console.error('Failed to load connection status:', error)
  }
}

const refreshStatus = async () => {
  refreshing.value = true
  await loadConnectionStatus()
  refreshing.value = false
}

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
        message: response.message || 'Configuration saved successfully'
      }
      // Refresh status after saving
      setTimeout(() => {
        loadConnectionStatus()
      }, 1000)
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
  max-width: 800px;
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
  margin-bottom: 15px;
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

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.status-info {
  padding: 15px;
  border-radius: 6px;
}

.status-info.connected {
  background: #d4edda;
  border: 1px solid #c3e6cb;
}

.status-info.disconnected {
  background: #f8d7da;
  border: 1px solid #f5c6cb;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
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

.status-text {
  font-weight: 600;
  font-size: 16px;
}

.status-message {
  margin: 0 0 15px 0;
  color: #666;
}

.connection-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  gap: 10px;
}

.detail-item .label {
  font-weight: 500;
  min-width: 80px;
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

.form-section:last-child {
  margin-bottom: 0;
}

.form-section h3 {
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e9ecef;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  padding: 0;
  border: none;
}

.btn-toggle {
  padding: 6px 12px;
  background: #17a2b8;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-toggle:hover {
  background: #138496;
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

.advanced-config {
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
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

@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .status-header {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }
}
</style>