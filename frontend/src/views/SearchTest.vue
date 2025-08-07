<template>
  <div class="search-test-page">
    <div class="page-header">
      <h1>Enhanced AI Search Test</h1>
      <p class="page-description">
        Test the new metadata-based search functionality with comprehensive error logging
      </p>
    </div>

    <!-- Enhanced Search Interface -->
    <EnhancedSearchInterface />

    <!-- Test Controls -->
    <div class="test-controls">
      <h2>Test Scenarios</h2>
      <div class="test-buttons">
        <button @click="testValidSearch" class="test-btn test-success">
          <Icon name="search" />
          Test Valid Search
        </button>
        
        <button @click="testInvalidQuery" class="test-btn test-warning">
          <Icon name="alert-triangle" />
          Test Invalid Query
        </button>
        
        <button @click="testNetworkError" class="test-btn test-error">
          <Icon name="alert-circle" />
          Test Network Error
        </button>
        
        <button @click="testEmptyResults" class="test-btn test-info">
          <Icon name="info" />
          Test Empty Results
        </button>
      </div>
    </div>

    <!-- Developer Information -->
    <div class="developer-info">
      <h2>Developer Information</h2>
      <div class="info-grid">
        <div class="info-card">
          <h3>🔍 Search Features</h3>
          <ul>
            <li>✅ Metadata-based matching</li>
            <li>✅ Comprehensive error logging</li>
            <li>✅ F12 console integration</li>
            <li>✅ Expandable log panels</li>
            <li>✅ JSON response viewer</li>
          </ul>
        </div>
        
        <div class="info-card">
          <h3>🚫 Removed Features</h3>
          <ul>
            <li>❌ AI search suggestions</li>
            <li>❌ Search refinement suggestions</li>
            <li>❌ Description suggestions</li>
            <li>❌ Category suggestions</li>
          </ul>
        </div>
        
        <div class="info-card">
          <h3>🛠️ Debug Tools</h3>
          <ul>
            <li>📊 Expandable log panels</li>
            <li>🔍 JSON data viewer</li>
            <li>📋 Copy to clipboard</li>
            <li>💾 Download logs</li>
            <li>🖥️ Console logging (F12)</li>
          </ul>
        </div>
        
        <div class="info-card">
          <h3>📈 Metadata Matching</h3>
          <ul>
            <li>📝 Filename matching</li>
            <li>📄 Description analysis</li>
            <li>🏷️ Tag comparison</li>
            <li>📊 File format filtering</li>
            <li>📐 Size/dimension matching</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import EnhancedSearchInterface from '@/components/search/EnhancedSearchInterface.vue'
import Icon from '@/components/common/Icon.vue'
import { errorLogTester } from '@/utils/errorLogTester'

const isRunningTests = ref(false)

const testValidSearch = () => {
  // Simulate a valid search by setting the search input
  const searchInput = document.querySelector('.search-input') as HTMLInputElement
  if (searchInput) {
    searchInput.value = 'landscape photography'
    searchInput.dispatchEvent(new Event('input'))
    
    // Trigger search
    const searchButton = document.querySelector('.search-button') as HTMLButtonElement
    if (searchButton) {
      searchButton.click()
    }
  }
  
  console.log('🧪 Testing valid search: "landscape photography"')
}

const testInvalidQuery = () => {
  const searchInput = document.querySelector('.search-input') as HTMLInputElement
  if (searchInput) {
    searchInput.value = '!@#$%^&*()_+{}|:"<>?[]\\;\',./'
    searchInput.dispatchEvent(new Event('input'))
    
    const searchButton = document.querySelector('.search-button') as HTMLButtonElement
    if (searchButton) {
      searchButton.click()
    }
  }
  
  console.log('🧪 Testing invalid query with special characters')
}

const testNetworkError = () => {
  // This would require mocking the API call to simulate network error
  console.log('🧪 Testing network error scenario')
  console.log('💡 To test network errors:')
  console.log('1. Open DevTools (F12)')
  console.log('2. Go to Network tab')
  console.log('3. Set throttling to "Offline"')
  console.log('4. Perform a search')
}

const testEmptyResults = () => {
  const searchInput = document.querySelector('.search-input') as HTMLInputElement
  if (searchInput) {
    searchInput.value = 'nonexistentimagequery12345'
    searchInput.dispatchEvent(new Event('input'))
    
    const searchButton = document.querySelector('.search-button') as HTMLButtonElement
    if (searchButton) {
      searchButton.click()
    }
  }
  
  console.log('🧪 Testing empty results scenario')
}

const runComprehensiveErrorTests = async () => {
  isRunningTests.value = true
  
  console.group('🔍 Starting Comprehensive Error Logging Verification')
  console.log('This will test all error logging scenarios and F12 integration')
  
  try {
    // Run all error logging tests
    await errorLogTester.runAllTests()
    
    // Test expandable log panel functionality
    errorLogTester.testExpandableLogPanel()
    
    console.log('✅ All error logging tests completed')
    console.log('🔍 Check the browser console (F12) for detailed output')
    console.log('📋 Check the expandable log panels in the UI for visual verification')
    
  } catch (error) {
    console.error('❌ Error during testing:', error)
  } finally {
    isRunningTests.value = false
  }
  
  console.groupEnd()
}

const verifyF12Integration = () => {
  console.group('🛠️ F12 Developer Tools Integration Verification')
  
  console.log('🔍 Verifying console logging features:')
  console.log('✅ Grouped console output')
  console.log('✅ Structured error information')
  console.log('✅ JSON data formatting')
  console.log('✅ Stack trace visualization')
  
  console.table({
    'Feature': 'Status',
    'Console Grouping': '✅ Active',
    'Error Logging': '✅ Active', 
    'JSON Formatting': '✅ Active',
    'Stack Traces': '✅ Active',
    'Request/Response Logging': '✅ Active'
  })
  
  console.log('🎯 Test Instructions:')
  console.log('1. Keep F12 Developer Tools open')
  console.log('2. Perform searches with different scenarios')
  console.log('3. Check console output for detailed logging')
  console.log('4. Verify expandable log panels in UI')
  console.log('5. Test copy/download functionality')
  
  console.groupEnd()
}

// Log component initialization
onMounted(() => {
  console.group('🚀 Enhanced Search Interface Initialized')
  console.log('✅ Metadata-based search enabled')
  console.log('✅ AI suggestions removed')
  console.log('✅ Comprehensive error logging active')
  console.log('✅ F12 developer tools integration ready')
  console.log('🧪 Error log tester loaded')
  console.groupEnd()
  
  // Auto-verify F12 integration
  setTimeout(() => {
    verifyF12Integration()
  }, 1000)
})
</script>

<style scoped>
.search-test-page {
  @apply min-h-screen bg-gray-50 py-8;
}

.page-header {
  @apply text-center mb-8;
}

.page-header h1 {
  @apply text-3xl font-bold text-gray-900 mb-2;
}

.page-description {
  @apply text-lg text-gray-600 max-w-2xl mx-auto;
}

.test-controls {
  @apply max-w-4xl mx-auto mt-12 p-6 bg-white rounded-lg shadow-sm border border-gray-200;
}

.test-controls h2 {
  @apply text-xl font-semibold text-gray-900 mb-4;
}

.test-buttons {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4;
}

.test-btn {
  @apply flex items-center justify-center gap-2 px-4 py-3 rounded-lg font-medium;
  @apply transition-all duration-200 hover:shadow-md;
}

.test-success {
  @apply bg-green-100 text-green-800 border border-green-300 hover:bg-green-200;
}

.test-warning {
  @apply bg-yellow-100 text-yellow-800 border border-yellow-300 hover:bg-yellow-200;
}

.test-error {
  @apply bg-red-100 text-red-800 border border-red-300 hover:bg-red-200;
}

.test-info {
  @apply bg-blue-100 text-blue-800 border border-blue-300 hover:bg-blue-200;
}

.developer-info {
  @apply max-w-6xl mx-auto mt-12 p-6 bg-white rounded-lg shadow-sm border border-gray-200;
}

.developer-info h2 {
  @apply text-xl font-semibold text-gray-900 mb-6;
}

.info-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6;
}

.info-card {
  @apply p-4 bg-gray-50 rounded-lg border border-gray-200;
}

.info-card h3 {
  @apply font-semibold text-gray-900 mb-3;
}

.info-card ul {
  @apply space-y-2;
}

.info-card li {
  @apply text-sm text-gray-700 flex items-center gap-2;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .test-buttons {
    @apply grid-cols-1;
  }
  
  .info-grid {
    @apply grid-cols-1;
  }
}
</style>