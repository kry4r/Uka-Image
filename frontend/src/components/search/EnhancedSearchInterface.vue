<template>
  <div class="enhanced-search-interface">
    <!-- Search Input Section -->
    <div class="search-section">
      <div class="search-input-container">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search images by description, tags, filename, or file type..."
          class="search-input"
          @keyup.enter="performSearch"
          @input="onSearchInput"
        />
        <button 
          @click="performSearch"
          :disabled="isSearching || !searchQuery.trim()"
          class="search-button"
        >
          <Icon v-if="isSearching" name="loading" class="animate-spin" />
          <Icon v-else name="search" />
          Search
        </button>
      </div>

      <!-- Advanced Filters -->
      <div class="filters-container" v-if="showAdvancedFilters">
        <div class="filter-group">
          <label>File Formats:</label>
          <select v-model="filters.fileFormats" multiple class="filter-select">
            <option value="JPEG">JPEG</option>
            <option value="PNG">PNG</option>
            <option value="GIF">GIF</option>
            <option value="WEBP">WEBP</option>
            <option value="BMP">BMP</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>Orientation:</label>
          <select v-model="filters.orientation" class="filter-select">
            <option value="">Any</option>
            <option value="LANDSCAPE">Landscape</option>
            <option value="PORTRAIT">Portrait</option>
            <option value="SQUARE">Square</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>Min Score:</label>
          <input 
            v-model.number="filters.minScore" 
            type="range" 
            min="0" 
            max="1" 
            step="0.1"
            class="filter-range"
          />
          <span class="score-display">{{ filters.minScore }}</span>
        </div>
      </div>

      <button 
        @click="showAdvancedFilters = !showAdvancedFilters"
        class="toggle-filters-btn"
      >
        <Icon :name="showAdvancedFilters ? 'chevron-up' : 'chevron-down'" />
        {{ showAdvancedFilters ? 'Hide' : 'Show' }} Advanced Filters
      </button>
    </div>

    <!-- Search Logs Section -->
    <div v-if="searchLogs.length > 0" class="logs-section">
      <div class="logs-header">
        <h3>Search Activity Log</h3>
        <button @click="clearLogs" class="clear-logs-btn">
          <Icon name="trash" />
          Clear Logs
        </button>
      </div>
      
      <div class="logs-container">
        <ExpandableLogPanel
          v-for="log in searchLogs"
          :key="log.id"
          :title="log.title"
          :summary="log.summary"
          :log-level="log.level"
          :log-data="log.data"
          :timestamp="log.timestamp"
          :auto-expand="log.level === 'error'"
        />
      </div>
    </div>

    <!-- Search Results Section -->
    <div v-if="searchResults.length > 0" class="results-section">
      <div class="results-header">
        <h3>Search Results</h3>
        <div class="results-info">
          <span>{{ searchResults.length }} images found</span>
          <span v-if="searchMetrics.searchDuration">
            in {{ searchMetrics.searchDuration }}ms
          </span>
        </div>
      </div>

      <!-- Results Grid -->
      <div class="results-grid">
        <SearchResultCard
          v-for="result in searchResults"
          :key="result.id"
          :image="result"
          :search-query="searchQuery"
          :metadata-matches="result.metadataMatches"
          @click="openImageDetail(result)"
        />
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <button 
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage <= 1"
          class="pagination-btn"
        >
          <Icon name="chevron-left" />
          Previous
        </button>
        
        <span class="page-info">
          Page {{ currentPage }} of {{ totalPages }}
        </span>
        
        <button 
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage >= totalPages"
          class="pagination-btn"
        >
          Next
          <Icon name="chevron-right" />
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!isSearching && searchResults.length === 0 && hasSearched" class="empty-state">
      <Icon name="search" size="lg" class="empty-icon" />
      <h3>No Results Found</h3>
      <p>Try adjusting your search terms or filters</p>
    </div>

    <!-- Loading State -->
    <div v-if="isSearching" class="loading-state">
      <Icon name="loading" size="lg" class="animate-spin" />
      <p>Searching images...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { imageApi } from '@/api/image'
import ExpandableLogPanel from '@/components/common/ExpandableLogPanel.vue'
import SearchResultCard from '@/components/search/SearchResultCard.vue'
import Icon from '@/components/common/Icon.vue'

interface SearchLog {
  id: string
  title: string
  summary: string
  level: 'info' | 'warning' | 'error'
  data: any
  timestamp: number
}

interface SearchFilters {
  fileFormats: string[]
  orientation: string
  minScore: number
}

const searchQuery = ref('')
const isSearching = ref(false)
const hasSearched = ref(false)
const showAdvancedFilters = ref(false)
const searchResults = ref<any[]>([])
const searchLogs = ref<SearchLog[]>([])
const currentPage = ref(1)
const totalPages = ref(1)
const pageSize = ref(20)

const filters = reactive<SearchFilters>({
  fileFormats: [],
  orientation: '',
  minScore: 0.1
})

const searchMetrics = reactive({
  searchDuration: 0,
  totalImagesAnalyzed: 0,
  searchStrategy: ''
})

const performSearch = async () => {
  if (!searchQuery.value.trim() || isSearching.value) return

  isSearching.value = true
  hasSearched.value = true
  const searchStartTime = Date.now()

  try {
    // Log search initiation
    addSearchLog({
      title: 'Search Initiated',
      summary: `Starting metadata-based search for: "${searchQuery.value}"`,
      level: 'info',
      data: {
        searchQuery: searchQuery.value,
        filters: { ...filters },
        timestamp: Date.now(),
        searchType: 'METADATA_BASED'
      }
    })

    // Prepare search options
    const searchOptions = {
      fileFormats: filters.fileFormats.length > 0 ? filters.fileFormats.join(',') : undefined,
      orientation: filters.orientation || undefined,
      minScore: filters.minScore
    }

    // Perform search
    const response = await imageApi.aiSearch(
      searchQuery.value,
      currentPage.value - 1,
      pageSize.value,
      searchOptions
    )

    const searchDuration = Date.now() - searchStartTime

    if (response.data.code === 200) {
      // Successful search
      const searchData = response.data.data
      searchResults.value = searchData.results || []
      
      // Update metrics
      searchMetrics.searchDuration = searchDuration
      searchMetrics.totalImagesAnalyzed = searchData.searchInsights?.searchInfo?.totalImagesAnalyzed || 0
      searchMetrics.searchStrategy = searchData.searchInsights?.searchInfo?.searchStrategy || 'Unknown'

      // Update pagination
      totalPages.value = searchData.totalPages || 1

      // Log successful search
      addSearchLog({
        title: 'Search Completed Successfully',
        summary: `Found ${searchResults.value.length} results in ${searchDuration}ms`,
        level: 'info',
        data: {
          searchQuery: searchQuery.value,
          resultsCount: searchResults.value.length,
          searchDuration,
          searchStrategy: searchMetrics.searchStrategy,
          searchInsights: searchData.searchInsights,
          processingSteps: searchData.searchInsights?.searchInfo?.processingSteps || [],
          requestPath: '/ai-search/search',
          httpMethod: 'GET',
          statusCode: 200,
          responseData: searchData
        }
      })

      // Log to console for F12 debugging
      console.group('🔍 Search Results')
      console.log('Query:', searchQuery.value)
      console.log('Results:', searchResults.value)
      console.log('Search Insights:', searchData.searchInsights)
      console.log('Full Response:', response.data)
      console.groupEnd()

    } else {
      // API returned error
      throw new Error(response.data.message || 'Search failed')
    }

  } catch (error: any) {
    console.error('Search error:', error)
    
    const searchDuration = Date.now() - searchStartTime
    
    // Create detailed error log
    const errorData = {
      searchQuery: searchQuery.value,
      filters: { ...filters },
      searchDuration,
      errorMessage: error.message,
      errorType: error.constructor.name,
      requestPath: '/ai-search/search',
      httpMethod: 'GET',
      timestamp: Date.now(),
      
      // Extract detailed error information if available
      detailedError: error.response?.data?.detailedError || null,
      searchContext: error.response?.data?.searchContext || null,
      processingSteps: error.response?.data?.processingSteps || [],
      
      // API response details
      apiResponseStatus: error.response?.status,
      apiResponseHeaders: error.response?.headers,
      apiResponseBody: error.response?.data,
      
      // Stack trace if available
      stackTrace: error.stack ? error.stack.split('\n').map((line: string) => ({
        line: line.trim()
      })) : [],
      
      // System context
      userAgent: navigator.userAgent,
      clientTimestamp: Date.now(),
      searchParameters: {
        query: searchQuery.value,
        page: currentPage.value,
        pageSize: pageSize.value,
        ...searchOptions
      }
    }

    // Add comprehensive error log
    addSearchLog({
      title: 'Search Failed',
      summary: `Search failed: ${error.message}`,
      level: 'error',
      data: errorData
    })

    // Log detailed error to console for F12 debugging
    console.group('❌ Search Error')
    console.error('Error:', error)
    console.log('Search Query:', searchQuery.value)
    console.log('Error Details:', errorData)
    console.log('Full Error Response:', error.response)
    console.groupEnd()

    // Clear results on error
    searchResults.value = []
    
  } finally {
    isSearching.value = false
  }
}

const addSearchLog = (logData: Omit<SearchLog, 'id'>) => {
  const log: SearchLog = {
    id: `log-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    ...logData
  }
  
  searchLogs.value.unshift(log)
  
  // Keep only last 10 logs to prevent memory issues
  if (searchLogs.value.length > 10) {
    searchLogs.value = searchLogs.value.slice(0, 10)
  }
}

const clearLogs = () => {
  searchLogs.value = []
}

const onSearchInput = () => {
  // Reset pagination when search query changes
  currentPage.value = 1
}

const goToPage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    performSearch()
  }
}

const openImageDetail = (image: any) => {
  // Emit event or navigate to image detail
  console.log('Opening image detail:', image)
}

// Auto-focus search input on mount
onMounted(() => {
  // Focus search input
  const searchInput = document.querySelector('.search-input') as HTMLInputElement
  if (searchInput) {
    searchInput.focus()
  }
})
</script>

<style scoped>
.enhanced-search-interface {
  @apply max-w-6xl mx-auto p-6 space-y-6;
}

.search-section {
  @apply bg-white rounded-lg shadow-sm border border-gray-200 p-6 space-y-4;
}

.search-input-container {
  @apply flex gap-3;
}

.search-input {
  @apply flex-1 px-4 py-3 border border-gray-300 rounded-lg;
  @apply focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
  @apply text-lg placeholder-gray-500;
}

.search-button {
  @apply px-6 py-3 bg-blue-600 text-white rounded-lg;
  @apply hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed;
  @apply flex items-center gap-2 font-medium transition-colors duration-200;
}

.filters-container {
  @apply grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-gray-50 rounded-lg;
}

.filter-group {
  @apply space-y-2;
}

.filter-group label {
  @apply block text-sm font-medium text-gray-700;
}

.filter-select {
  @apply w-full px-3 py-2 border border-gray-300 rounded-md;
  @apply focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.filter-range {
  @apply w-full;
}

.score-display {
  @apply text-sm text-gray-600 font-mono;
}

.toggle-filters-btn {
  @apply flex items-center gap-2 text-blue-600 hover:text-blue-700;
  @apply font-medium transition-colors duration-200;
}

.logs-section {
  @apply space-y-4;
}

.logs-header {
  @apply flex justify-between items-center;
}

.logs-header h3 {
  @apply text-lg font-semibold text-gray-900;
}

.clear-logs-btn {
  @apply flex items-center gap-2 px-3 py-1 text-red-600 hover:text-red-700;
  @apply border border-red-300 rounded-md hover:bg-red-50;
  @apply transition-colors duration-200;
}

.logs-container {
  @apply space-y-3;
}

.results-section {
  @apply space-y-4;
}

.results-header {
  @apply flex justify-between items-center;
}

.results-header h3 {
  @apply text-lg font-semibold text-gray-900;
}

.results-info {
  @apply text-sm text-gray-600 space-x-2;
}

.results-grid {
  @apply grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4;
}

.pagination {
  @apply flex justify-center items-center gap-4 mt-6;
}

.pagination-btn {
  @apply flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-md;
  @apply hover:bg-gray-50 disabled:bg-gray-100 disabled:cursor-not-allowed;
  @apply transition-colors duration-200;
}

.page-info {
  @apply text-sm text-gray-600;
}

.empty-state {
  @apply text-center py-12 space-y-4;
}

.empty-icon {
  @apply mx-auto text-gray-400;
}

.empty-state h3 {
  @apply text-lg font-medium text-gray-900;
}

.empty-state p {
  @apply text-gray-600;
}

.loading-state {
  @apply text-center py-12 space-y-4;
}

.loading-state p {
  @apply text-gray-600;
}

/* Animation for loading spinner */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>