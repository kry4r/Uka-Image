<template>
  <div class="expandable-log-panel">
    <!-- Log Panel Header -->
    <div 
      class="log-header"
      :class="{ 'error': logLevel === 'error', 'warning': logLevel === 'warning', 'info': logLevel === 'info' }"
      @click="toggleExpanded"
    >
      <div class="log-header-content">
        <div class="log-title">
          <Icon :name="getLogIcon()" class="log-icon" />
          <span class="log-title-text">{{ title }}</span>
          <span class="log-timestamp">{{ formatTimestamp(timestamp) }}</span>
        </div>
        <div class="log-summary">
          {{ summary }}
        </div>
      </div>
      <Icon 
        :name="isExpanded ? 'chevron-up' : 'chevron-down'" 
        class="expand-icon"
      />
    </div>

    <!-- Expandable Content -->
    <Transition name="expand">
      <div v-if="isExpanded" class="log-content">
        <!-- Quick Actions -->
        <div class="log-actions">
          <button @click="copyToClipboard" class="action-btn copy-btn">
            <Icon name="copy" />
            Copy to Clipboard
          </button>
          <button @click="logToConsole" class="action-btn console-btn">
            <Icon name="terminal" />
            Log to Console (F12)
          </button>
          <button @click="downloadLog" class="action-btn download-btn">
            <Icon name="download" />
            Download Log
          </button>
        </div>

        <!-- Tabbed Content -->
        <div class="log-tabs">
          <div class="tab-headers">
            <button 
              v-for="tab in availableTabs" 
              :key="tab.key"
              :class="['tab-header', { active: activeTab === tab.key }]"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
              <span v-if="tab.count" class="tab-count">{{ tab.count }}</span>
            </button>
          </div>

          <div class="tab-content">
            <!-- Overview Tab -->
            <div v-if="activeTab === 'overview'" class="tab-panel">
              <div class="overview-grid">
                <div class="overview-item">
                  <label>Request Path:</label>
                  <code>{{ logData.requestPath || 'N/A' }}</code>
                </div>
                <div class="overview-item">
                  <label>HTTP Method:</label>
                  <code>{{ logData.httpMethod || 'N/A' }}</code>
                </div>
                <div class="overview-item">
                  <label>Status Code:</label>
                  <code :class="getStatusClass(logData.statusCode)">
                    {{ logData.statusCode || 'N/A' }}
                  </code>
                </div>
                <div class="overview-item">
                  <label>Response Time:</label>
                  <code>{{ formatDuration(logData.responseTime) }}</code>
                </div>
                <div class="overview-item">
                  <label>Error Type:</label>
                  <code>{{ logData.errorType || 'N/A' }}</code>
                </div>
                <div class="overview-item">
                  <label>Client IP:</label>
                  <code>{{ logData.clientIp || 'N/A' }}</code>
                </div>
              </div>
            </div>

            <!-- Request Tab -->
            <div v-if="activeTab === 'request'" class="tab-panel">
              <div class="json-section">
                <h4>Request Parameters</h4>
                <JsonViewer 
                  :data="logData.requestParameters || {}" 
                  :expanded="true"
                />
              </div>
              <div class="json-section">
                <h4>Request Headers</h4>
                <JsonViewer 
                  :data="logData.requestHeaders || {}" 
                  :expanded="false"
                />
              </div>
              <div v-if="logData.requestBody" class="json-section">
                <h4>Request Body</h4>
                <JsonViewer 
                  :data="logData.requestBody" 
                  :expanded="true"
                />
              </div>
            </div>

            <!-- Response Tab -->
            <div v-if="activeTab === 'response'" class="tab-panel">
              <div class="json-section">
                <h4>Response Data</h4>
                <JsonViewer 
                  :data="logData.responseData || {}" 
                  :expanded="true"
                />
              </div>
              <div v-if="logData.apiResponseHeaders" class="json-section">
                <h4>API Response Headers</h4>
                <JsonViewer 
                  :data="logData.apiResponseHeaders" 
                  :expanded="false"
                />
              </div>
              <div v-if="logData.apiResponseBody" class="json-section">
                <h4>Raw API Response</h4>
                <pre class="raw-response">{{ logData.apiResponseBody }}</pre>
              </div>
            </div>

            <!-- Error Tab -->
            <div v-if="activeTab === 'error'" class="tab-panel">
              <div class="error-section">
                <h4>Exception Details</h4>
                <div class="error-details">
                  <div class="error-item">
                    <label>Exception Class:</label>
                    <code>{{ logData.exceptionClass || 'N/A' }}</code>
                  </div>
                  <div class="error-item">
                    <label>Exception Message:</label>
                    <code class="error-message">{{ logData.exceptionMessage || 'N/A' }}</code>
                  </div>
                  <div class="error-item">
                    <label>Root Cause:</label>
                    <code class="error-message">{{ logData.rootCauseMessage || 'N/A' }}</code>
                  </div>
                </div>
              </div>
              
              <div v-if="logData.stackTrace && logData.stackTrace.length > 0" class="stack-trace-section">
                <h4>Stack Trace</h4>
                <div class="stack-trace">
                  <div 
                    v-for="(frame, index) in logData.stackTrace" 
                    :key="index"
                    class="stack-frame"
                    :class="{ 'app-frame': isApplicationFrame(frame) }"
                  >
                    <div class="frame-class">{{ frame.className }}</div>
                    <div class="frame-method">{{ frame.methodName }}</div>
                    <div class="frame-location">
                      {{ frame.fileName }}:{{ frame.lineNumber }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Search Context Tab -->
            <div v-if="activeTab === 'search'" class="tab-panel">
              <div v-if="logData.searchContext" class="search-context">
                <div class="json-section">
                  <h4>Search Parameters</h4>
                  <JsonViewer 
                    :data="logData.searchContext.searchParameters || {}" 
                    :expanded="true"
                  />
                </div>
                
                <div class="processing-steps">
                  <h4>Processing Steps</h4>
                  <ol class="steps-list">
                    <li 
                      v-for="(step, index) in logData.searchContext.processingSteps || []" 
                      :key="index"
                      class="step-item"
                    >
                      {{ step }}
                    </li>
                  </ol>
                </div>

                <div class="search-metrics">
                  <h4>Search Metrics</h4>
                  <div class="metrics-grid">
                    <div class="metric-item">
                      <label>Total Images:</label>
                      <span>{{ logData.searchContext.totalImagesAnalyzed || 'N/A' }}</span>
                    </div>
                    <div class="metric-item">
                      <label>Search Duration:</label>
                      <span>{{ formatDuration(logData.searchContext.searchDuration) }}</span>
                    </div>
                    <div class="metric-item">
                      <label>Search Strategy:</label>
                      <span>{{ logData.searchContext.searchStrategy || 'N/A' }}</span>
                    </div>
                    <div class="metric-item">
                      <label>Failure Point:</label>
                      <span class="failure-point">{{ logData.searchContext.failurePoint || 'N/A' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- System Tab -->
            <div v-if="activeTab === 'system'" class="tab-panel">
              <div v-if="logData.systemContext" class="system-context">
                <div class="system-grid">
                  <div class="system-item">
                    <label>Java Version:</label>
                    <span>{{ logData.systemContext.javaVersion || 'N/A' }}</span>
                  </div>
                  <div class="system-item">
                    <label>Spring Boot Version:</label>
                    <span>{{ logData.systemContext.springBootVersion || 'N/A' }}</span>
                  </div>
                  <div class="system-item">
                    <label>Operating System:</label>
                    <span>{{ logData.systemContext.operatingSystem || 'N/A' }}</span>
                  </div>
                  <div class="system-item">
                    <label>Available Memory:</label>
                    <span>{{ formatBytes(logData.systemContext.availableMemory) }}</span>
                  </div>
                  <div class="system-item">
                    <label>Total Memory:</label>
                    <span>{{ formatBytes(logData.systemContext.totalMemory) }}</span>
                  </div>
                  <div class="system-item">
                    <label>Used Memory:</label>
                    <span>{{ formatBytes(logData.systemContext.usedMemory) }}</span>
                  </div>
                  <div class="system-item">
                    <label>Active Threads:</label>
                    <span>{{ logData.systemContext.activeThreads || 'N/A' }}</span>
                  </div>
                  <div class="system-item">
                    <label>Server:</label>
                    <span>{{ logData.systemContext.serverName || 'N/A' }}:{{ logData.systemContext.serverPort || 'N/A' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import JsonViewer from './JsonViewer.vue'
import Icon from './Icon.vue'

interface Props {
  title: string
  summary: string
  logLevel: 'info' | 'warning' | 'error'
  logData: any
  timestamp?: number
  autoExpand?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  timestamp: () => Date.now(),
  autoExpand: false
})

const isExpanded = ref(props.autoExpand)
const activeTab = ref('overview')

const availableTabs = computed(() => {
  const tabs = [
    { key: 'overview', label: 'Overview' },
    { key: 'request', label: 'Request', count: Object.keys(props.logData.requestParameters || {}).length },
    { key: 'response', label: 'Response' },
  ]

  if (props.logLevel === 'error') {
    tabs.push({ 
      key: 'error', 
      label: 'Error Details', 
      count: props.logData.stackTrace?.length || 0 
    })
  }

  if (props.logData.searchContext) {
    tabs.push({ 
      key: 'search', 
      label: 'Search Context',
      count: props.logData.searchContext.processingSteps?.length || 0
    })
  }

  if (props.logData.systemContext) {
    tabs.push({ key: 'system', label: 'System Info' })
  }

  return tabs
})

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const getLogIcon = () => {
  switch (props.logLevel) {
    case 'error': return 'alert-circle'
    case 'warning': return 'alert-triangle'
    case 'info': return 'info'
    default: return 'info'
  }
}

const formatTimestamp = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

const formatDuration = (duration: number | undefined) => {
  if (!duration) return 'N/A'
  return `${duration}ms`
}

const formatBytes = (bytes: number | undefined) => {
  if (!bytes) return 'N/A'
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${sizes[i]}`
}

const getStatusClass = (status: number | undefined) => {
  if (!status) return ''
  if (status >= 200 && status < 300) return 'status-success'
  if (status >= 400 && status < 500) return 'status-client-error'
  if (status >= 500) return 'status-server-error'
  return ''
}

const isApplicationFrame = (frame: any) => {
  return frame.className?.includes('com.uka.image')
}

const copyToClipboard = async () => {
  try {
    const logText = JSON.stringify(props.logData, null, 2)
    await navigator.clipboard.writeText(logText)
    // Show success notification
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
  }
}

const logToConsole = () => {
  console.group(`🔍 ${props.title}`)
  console.log('Summary:', props.summary)
  console.log('Log Level:', props.logLevel)
  console.log('Timestamp:', new Date(props.timestamp))
  console.log('Full Log Data:', props.logData)
  console.groupEnd()
}

const downloadLog = () => {
  const logText = JSON.stringify({
    title: props.title,
    summary: props.summary,
    logLevel: props.logLevel,
    timestamp: props.timestamp,
    data: props.logData
  }, null, 2)
  
  const blob = new Blob([logText], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `log-${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

onMounted(() => {
  // Auto-expand error logs
  if (props.logLevel === 'error' && props.autoExpand) {
    isExpanded.value = true
    activeTab.value = 'error'
  }
})
</script>

<style scoped>
.expandable-log-panel {
  @apply border border-gray-200 rounded-lg mb-4 overflow-hidden;
}

.log-header {
  @apply p-4 cursor-pointer transition-colors duration-200;
  @apply bg-gray-50 hover:bg-gray-100;
}

.log-header.error {
  @apply bg-red-50 border-red-200 hover:bg-red-100;
}

.log-header.warning {
  @apply bg-yellow-50 border-yellow-200 hover:bg-yellow-100;
}

.log-header.info {
  @apply bg-blue-50 border-blue-200 hover:bg-blue-100;
}

.log-header-content {
  @apply flex-1;
}

.log-title {
  @apply flex items-center gap-2 mb-1;
}

.log-icon {
  @apply w-5 h-5;
}

.log-title-text {
  @apply font-semibold text-gray-900;
}

.log-timestamp {
  @apply text-sm text-gray-500 ml-auto;
}

.log-summary {
  @apply text-sm text-gray-600;
}

.expand-icon {
  @apply w-5 h-5 text-gray-400 transition-transform duration-200;
}

.log-content {
  @apply border-t border-gray-200 bg-white;
}

.log-actions {
  @apply p-4 border-b border-gray-100 flex gap-2;
}

.action-btn {
  @apply px-3 py-1 text-sm border border-gray-300 rounded-md;
  @apply hover:bg-gray-50 transition-colors duration-200;
  @apply flex items-center gap-1;
}

.copy-btn:hover {
  @apply bg-blue-50 border-blue-300;
}

.console-btn:hover {
  @apply bg-green-50 border-green-300;
}

.download-btn:hover {
  @apply bg-purple-50 border-purple-300;
}

.log-tabs {
  @apply flex flex-col;
}

.tab-headers {
  @apply flex border-b border-gray-200 bg-gray-50;
}

.tab-header {
  @apply px-4 py-2 text-sm font-medium text-gray-600;
  @apply hover:text-gray-900 hover:bg-gray-100;
  @apply border-b-2 border-transparent transition-all duration-200;
  @apply flex items-center gap-1;
}

.tab-header.active {
  @apply text-blue-600 border-blue-500 bg-white;
}

.tab-count {
  @apply bg-gray-200 text-gray-600 px-1 py-0 rounded text-xs;
}

.tab-header.active .tab-count {
  @apply bg-blue-100 text-blue-600;
}

.tab-content {
  @apply p-4;
}

.tab-panel {
  @apply space-y-4;
}

.overview-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-4;
}

.overview-item {
  @apply flex flex-col gap-1;
}

.overview-item label {
  @apply text-sm font-medium text-gray-700;
}

.overview-item code {
  @apply text-sm bg-gray-100 px-2 py-1 rounded;
}

.status-success {
  @apply bg-green-100 text-green-800;
}

.status-client-error {
  @apply bg-yellow-100 text-yellow-800;
}

.status-server-error {
  @apply bg-red-100 text-red-800;
}

.json-section {
  @apply space-y-2;
}

.json-section h4 {
  @apply text-sm font-semibold text-gray-700;
}

.raw-response {
  @apply bg-gray-100 p-3 rounded text-sm overflow-x-auto;
  @apply border border-gray-200;
}

.error-section {
  @apply space-y-4;
}

.error-details {
  @apply space-y-3;
}

.error-item {
  @apply flex flex-col gap-1;
}

.error-item label {
  @apply text-sm font-medium text-gray-700;
}

.error-message {
  @apply text-sm bg-red-50 text-red-800 px-2 py-1 rounded;
}

.stack-trace-section {
  @apply space-y-2;
}

.stack-trace {
  @apply bg-gray-50 border border-gray-200 rounded max-h-96 overflow-y-auto;
}

.stack-frame {
  @apply p-2 border-b border-gray-100 text-sm font-mono;
}

.stack-frame.app-frame {
  @apply bg-blue-50 border-blue-100;
}

.frame-class {
  @apply text-gray-600;
}

.frame-method {
  @apply text-blue-600 font-semibold;
}

.frame-location {
  @apply text-gray-500 text-xs;
}

.search-context {
  @apply space-y-4;
}

.processing-steps {
  @apply space-y-2;
}

.steps-list {
  @apply bg-gray-50 border border-gray-200 rounded p-3;
  @apply list-decimal list-inside space-y-1;
}

.step-item {
  @apply text-sm text-gray-700;
}

.search-metrics {
  @apply space-y-2;
}

.metrics-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-3;
}

.metric-item {
  @apply flex justify-between items-center p-2 bg-gray-50 rounded;
}

.metric-item label {
  @apply text-sm font-medium text-gray-700;
}

.failure-point {
  @apply text-red-600 font-medium;
}

.system-context {
  @apply space-y-4;
}

.system-grid {
  @apply grid grid-cols-1 md:grid-cols-2 gap-3;
}

.system-item {
  @apply flex justify-between items-center p-2 bg-gray-50 rounded;
}

.system-item label {
  @apply text-sm font-medium text-gray-700;
}

/* Transition animations */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 1000px;
  opacity: 1;
}
</style>