<template>
  <AppLayout>
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex justify-between items-center">
        <h1 class="text-3xl font-bold text-gray-900">Image Gallery</h1>
        <router-link
          to="/upload"
          class="btn-primary"
        >
          Upload Images
        </router-link>
      </div>

      <!-- Search and Filter Section -->
      <div class="bg-white rounded-lg shadow-sm p-6 space-y-4">
        <!-- Search Type Toggle -->
        <div class="flex space-x-4">
          <button
            @click="searchType = 'normal'"
            :class="[
              'px-4 py-2 rounded-md text-sm font-medium transition-colors',
              searchType === 'normal' 
                ? 'bg-blue-600 text-white' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            Normal Search
          </button>
          <button
            @click="searchType = 'ai'"
            :class="[
              'px-4 py-2 rounded-md text-sm font-medium transition-colors',
              searchType === 'ai' 
                ? 'bg-purple-600 text-white' 
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            ]"
          >
            AI Search
          </button>
        </div>

        <!-- Search Input -->
        <div class="flex space-x-4">
          <div class="flex-1">
            <input
              v-model="searchQuery"
              type="text"
              :placeholder="searchType === 'normal' ? 'Search by keywords or tags...' : 'Describe what you\'re looking for...'"
              class="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @keyup.enter="handleSearchEnter"
            />
          </div>
          <button
            @click="handleSearchClick"
            :disabled="!searchQuery.trim()"
            class="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            Search
          </button>
          <button
            v-if="isSearching"
            @click="clearSearch"
            class="px-4 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 transition-colors"
          >
            Clear
          </button>
        </div>

        <!-- Tag Filter -->
        <div v-if="availableTags.length > 0" class="space-y-2">
          <label class="text-sm font-medium text-gray-700">Filter by Tags:</label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="tag in availableTags"
              :key="tag"
              @click="toggleTagFilter(tag)"
              :class="[
                'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium transition-colors',
                selectedTags.includes(tag)
                  ? 'bg-blue-100 text-blue-800 border-2 border-blue-300'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200 border-2 border-transparent'
              ]"
            >
              {{ tag }}
              <span v-if="selectedTags.includes(tag)" class="ml-1">×</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="imageStore.loading || aiSearchLoading" class="flex justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>

      <!-- Error State -->
      <div v-else-if="imageStore.error" class="text-center py-12">
        <p class="text-red-600 mb-4">{{ imageStore.error }}</p>
        <button @click="loadImages" class="btn-primary">
          Try Again
        </button>
      </div>

      <!-- Empty State -->
      <div v-else-if="!hasResults" class="text-center py-12">
        <div class="max-w-md mx-auto">
          <h3 class="text-lg font-medium text-gray-900 mb-2">
            {{ getEmptyStateTitle() }}
          </h3>
          <p class="text-gray-500 mb-6">
            {{ getEmptyStateMessage() }}
          </p>
          <router-link v-if="shouldShowUploadButton()" to="/upload" class="btn-primary">
            Upload Images
          </router-link>
        </div>
      </div>

      <!-- Results Grid -->
      <div v-else>
        <!-- AI Search Results -->
        <div v-if="searchType === 'ai' && aiSearchResults.length > 0">
          <div class="mb-4">
            <div class="flex justify-between items-center mb-2">
              <h2 class="text-lg font-medium text-gray-900">
                AI Search Results ({{ aiSearchResults.length }} found)
              </h2>
              <span class="text-sm text-purple-600 bg-purple-100 px-2 py-1 rounded">
                AI-Powered
              </span>
            </div>
            
            <!-- Threshold warning when showing fallback results -->
            <div v-if="showThresholdWarning" class="mb-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
              <div class="flex items-center">
                <svg class="w-4 h-4 text-yellow-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"/>
                </svg>
                <span class="text-sm text-yellow-800 font-medium">Lower Relevance Results</span>
              </div>
              <p class="text-xs text-yellow-700 mt-1">
                No results met the 75% relevance threshold. Showing all available results with lower relevance scores.
              </p>
            </div>
            
            <!-- AI Model Information Display -->
            <div v-if="aiModelInfo" :class="[
              'border rounded-lg p-3 text-sm',
              aiModelInfo.aiSearchUsed 
                ? 'bg-purple-50 border-purple-200' 
                : 'bg-yellow-50 border-yellow-200'
            ]">
              <div class="flex items-center mb-2">
                <svg v-if="aiModelInfo.aiSearchUsed" class="w-4 h-4 text-purple-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <svg v-else class="w-4 h-4 text-yellow-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"/>
                </svg>
                <span :class="[
                  'font-medium',
                  aiModelInfo.aiSearchUsed ? 'text-purple-800' : 'text-yellow-800'
                ]">
                  AI Model Status: {{ aiModelInfo.aiSearchUsed ? 'Active & Used' : 'Available but Not Used' }}
                </span>
              </div>
              <div :class="[
                'grid grid-cols-1 md:grid-cols-2 gap-2',
                aiModelInfo.aiSearchUsed ? 'text-purple-700' : 'text-yellow-700'
              ]">
                <div><strong>Strategy:</strong> {{ aiModelInfo.searchStrategy || 'Enhanced AI Search' }}</div>
                <div><strong>AI Used:</strong> {{ aiModelInfo.aiSearchUsed ? 'Yes' : 'No' }}</div>
                <div><strong>Query Type:</strong> {{ aiModelInfo.searchCriteria?.primaryType || 'N/A' }}</div>
                <div><strong>Complexity:</strong> {{ aiModelInfo.searchCriteria?.complexity || 'N/A' }}</div>
              </div>
              <div v-if="!aiModelInfo.aiSearchUsed" class="mt-2 text-xs text-yellow-600">
                ⚠️ AI model may not be properly configured or connected. Check backend AI service configuration.
              </div>
            </div>
          </div>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            <SearchResultCard
              v-for="result in aiSearchResults"
              :key="result.imageId"
              :result="result"
              :search-query="searchQuery"
              @click="openImageDetail(result.image)"
              class="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow duration-200"
            />
          </div>
        </div>

        <!-- AI Search Mode - Show all images when no search performed -->
        <div v-else-if="searchType === 'ai' && !isSearching && imageStore.images.length > 0">
          <div class="mb-4">
            <h2 class="text-lg font-medium text-gray-900">
              All Images - Use AI Search Above
            </h2>
            <p class="text-sm text-gray-600 mt-1">
              Enter a search query above to use AI-powered search
            </p>
          </div>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            <div
              v-for="image in imageStore.images"
              :key="image.id"
              class="group relative bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow duration-200 cursor-pointer"
              @click="openImageDetail(image)"
            >
              <div class="aspect-square overflow-hidden rounded-t-lg">
                <img
                  :src="image.thumbnailUrl || image.cosUrl"
                  :alt="image.originalName"
                  class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                />
              </div>
              <div class="p-4">
                <h3 class="text-sm font-medium text-gray-900 truncate">
                  {{ getDisplayName(image) }}
                </h3>
                <p class="text-xs text-gray-500 mt-1">
                  {{ formatFileSize(image.fileSize) }}
                </p>
                <p class="text-xs text-gray-400 mt-1">
                  {{ formatDate(image.createdAt) }}
                </p>
                <!-- Tags preview -->
                <div v-if="image.tags" class="flex flex-wrap gap-1 mt-2">
                  <span
                    v-for="tag in parseTags(image.tags).slice(0, 2)"
                    :key="tag"
                    class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800"
                  >
                    {{ tag }}
                  </span>
                  <span
                    v-if="parseTags(image.tags).length > 2"
                    class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600"
                  >
                    +{{ parseTags(image.tags).length - 2 }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Normal Search Results -->
        <div v-else-if="searchType === 'normal' && imageStore.images.length > 0">
          <div class="mb-4">
            <h2 class="text-lg font-medium text-gray-900">
              {{ isSearching ? `Search Results (${imageStore.images.length} found)` : 'All Images' }}
            </h2>
          </div>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            <div
              v-for="image in imageStore.images"
              :key="image.id"
              class="group relative bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow duration-200 cursor-pointer"
              @click="openImageDetail(image)"
            >
              <div class="aspect-square overflow-hidden rounded-t-lg">
                <img
                  :src="image.thumbnailUrl || image.cosUrl"
                  :alt="image.originalName"
                  class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-200"
                />
              </div>
              <div class="p-4">
                <h3 class="text-sm font-medium text-gray-900 truncate">
                  {{ getDisplayName(image) }}
                </h3>
                <p class="text-xs text-gray-500 mt-1">
                  {{ formatFileSize(image.fileSize) }}
                </p>
                <p class="text-xs text-gray-400 mt-1">
                  {{ formatDate(image.createdAt) }}
                </p>
                <!-- Tags preview -->
                <div v-if="image.tags" class="flex flex-wrap gap-1 mt-2">
                  <span
                    v-for="tag in parseTags(image.tags).slice(0, 2)"
                    :key="tag"
                    class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800"
                  >
                    {{ tag }}
                  </span>
                  <span
                    v-if="parseTags(image.tags).length > 2"
                    class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600"
                  >
                    +{{ parseTags(image.tags).length - 2 }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="hasResults" class="flex justify-center items-center space-x-4 mt-8">
          <!-- Normal search pagination -->
          <template v-if="searchType === 'normal' && imageStore.images.length > 0">
            <button
              @click="previousPage"
              :disabled="!imageStore.hasPrevPage"
              class="btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span class="text-sm text-gray-600">
              Page {{ imageStore.pagination.page + 1 }} of {{ imageStore.pagination.totalPages }}
            </span>
            <button
              @click="nextPage"
              :disabled="!imageStore.hasNextPage"
              class="btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </template>
          
          <!-- AI search pagination -->
          <template v-if="searchType === 'ai' && aiSearchResults.length > 0 && aiModelInfo">
            <button
              @click="previousAiPage"
              :disabled="aiCurrentPage <= 1"
              class="btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span class="text-sm text-gray-600">
              Page {{ aiCurrentPage }} of {{ aiTotalPages }}
            </span>
            <button
              @click="nextAiPage"
              :disabled="aiCurrentPage >= aiTotalPages"
              class="btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- Image Detail Modal -->
    <ImageDetailModal
      :visible="showDetailModal"
      :image="selectedImage"
      @close="closeImageDetail"
      @delete="deleteImage"
      @download="downloadImage"
    />
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useImageStore } from '@/stores/image'
import { useAiSearchStore } from '@/stores/aiSearch'
import { imageApi, type Image } from '@/api/image'
import { aiSearchApi, type ScoredResult } from '@/api/aiSearch'
import AppLayout from '@/components/Layout/AppLayout.vue'
import ImageDetailModal from '@/components/common/ImageDetailModal.vue'
import SearchResultCard from '@/components/search/SearchResultCard.vue'

const router = useRouter()
const imageStore = useImageStore()
const aiSearchStore = useAiSearchStore()

// Search and filter state
const searchType = ref<'normal' | 'ai'>('normal')
const searchQuery = ref('')
const selectedTags = ref<string[]>([])
const isSearching = ref(false)
const aiSearchResults = ref<ScoredResult[]>([])
const aiSearchLoading = ref(false)
const aiModelInfo = ref<any>(null)

// AI search pagination state
const aiCurrentPage = ref(1)
const aiPageSize = ref(20)

// Threshold warning state
const showThresholdWarning = ref(false)

// Modal state
const showDetailModal = ref(false)
const selectedImage = ref<Image | null>(null)

// Available tags computed from all images
const availableTags = computed(() => {
  const tags = new Set<string>()
  imageStore.images.forEach(image => {
    if (image.tags) {
      parseTags(image.tags).forEach(tag => tags.add(tag))
    }
  })
  return Array.from(tags).sort()
})

// AI pagination computed properties
const aiTotalPages = computed(() => {
  if (!aiModelInfo.value || !aiModelInfo.value.totalResults) return 1
  return Math.ceil(aiModelInfo.value.totalResults / aiPageSize.value)
})

// Check if we have any results to show
const hasResults = computed(() => {
  if (searchType.value === 'ai') {
    // For AI search, show results if we have AI search results OR if we haven't searched yet and there are images
    return aiSearchResults.value.length > 0 || (!isSearching.value && imageStore.images.length > 0)
  }
  return imageStore.images.length > 0
})

const loadImages = async (page: number = 0) => {
  if (isSearching.value) {
    await performSearch(page)
  } else {
    await imageStore.fetchImages(page)
  }
}

const handleSearchEnter = (event: KeyboardEvent) => {
  event.preventDefault()
  performSearch(0)
}

const handleSearchClick = () => {
  performSearch(0)
}

// Helper function to check if any attribute relevance exceeds 75% threshold
const isValidSearchResult = (result: ScoredResult): boolean => {
  const threshold = 0.75
  return (
    result.aiRelevanceScore >= threshold ||
    result.formatMatchScore >= threshold ||
    result.temporalRelevanceScore >= threshold ||
    result.contentQualityScore >= threshold
  )
}

// Store all results for fallback display
const allAiSearchResults = ref<ScoredResult[]>([])

const performSearch = async (page: number = 0) => {
  if (!searchQuery.value.trim() && selectedTags.value.length === 0) {
    return
  }

  isSearching.value = true
  
  // Reset AI pagination when starting a new search (page 0)
  if (page === 0 && searchType.value === 'ai') {
    aiCurrentPage.value = 1
  }
  
  try {
    let keyword = searchQuery.value.trim()
    
    // Add selected tags to search query
    if (selectedTags.value.length > 0) {
      const tagQuery = selectedTags.value.join(',')
      keyword = keyword ? `${keyword} ${tagQuery}` : tagQuery
    }

    if (searchType.value === 'normal') {
      await imageStore.searchImages(keyword, page, 20)
      aiSearchResults.value = []
    } else {
      // Enhanced AI search - use page + 1 for API call (1-based pagination)
      const apiPage = page + 1
      aiSearchLoading.value = true
      try {
        console.log('🤖 Starting AI Search...')
        console.log('🔍 Search Query:', keyword)
        console.log('📄 Page:', apiPage, 'Size:', aiPageSize.value)
        
        const response = await aiSearchApi.enhancedSearch(keyword, apiPage, aiPageSize.value)
        
        console.log('✅ AI Search Response:', response.data)
        
        // The response.data directly contains the search results
        if (response.data && response.data.results) {
          const searchData = response.data
          
          // Get all results from the response
          const allResults = searchData.results || []
          
          // Store all results for potential fallback display
          allAiSearchResults.value = allResults
          
          // Filter results based on 75% attribute relevance threshold
          const validResults = allResults.filter(isValidSearchResult)
          
          console.log(`🎯 Filtered results: ${validResults.length}/${allResults.length} passed 75% threshold`)
          
          // If no results meet the threshold, show all results with a warning
          if (validResults.length === 0 && allResults.length > 0) {
            console.log('⚠️ No results met 75% threshold, showing all results with lower relevance')
            aiSearchResults.value = allResults
            showThresholdWarning.value = true
          } else {
            aiSearchResults.value = validResults
            showThresholdWarning.value = false
          }
          // Store AI model information for UI display
          aiModelInfo.value = {
            ...searchData,
            totalResults: validResults.length, // Update total to reflect filtered count
            searchStrategy: searchData.searchStrategy || 'Enhanced AI Search',
            aiSearchUsed: searchData.aiSearchUsed !== undefined ? searchData.aiSearchUsed : true,
            searchCriteria: searchData.searchCriteria || null,
            searchInsights: searchData.searchInsights || {}
          }
          // Clear normal search results when using AI search
          imageStore.images = []
          
          // Log AI model information
          console.log('🧠 AI Model Information:')
          console.log('- Search Strategy:', aiModelInfo.value.searchStrategy)
          console.log('- AI Search Used:', aiModelInfo.value.aiSearchUsed)
          console.log('- Total Results (after filtering):', validResults.length)
          console.log('- Search Criteria:', aiModelInfo.value.searchCriteria)
          console.log('- Search Insights:', aiModelInfo.value.searchInsights)
          
          // Check if AI was actually used
          if (!aiModelInfo.value.aiSearchUsed) {
            console.warn('⚠️ AI Search was not used - AI model may not be configured')
            console.log('📋 Fallback strategy applied:', aiModelInfo.value.searchStrategy)
            console.log('🔧 Backend AI Configuration Required:')
            console.log('  - Check SparkAI service configuration')
            console.log('  - Verify AI model API keys')
            console.log('  - Ensure AI service is running')
          } else {
            console.log('✅ AI Search successfully used!')
          }
          
          // Display AI model info in UI
          if (aiModelInfo.value.searchInsights && Object.keys(aiModelInfo.value.searchInsights).length > 0) {
            console.log('🎯 AI Model Details:')
            Object.entries(aiModelInfo.value.searchInsights).forEach(([key, value]) => {
              console.log(`  - ${key}:`, value)
            })
          } else {
            console.log('ℹ️ No AI insights available - AI model not active')
          }
          
          // If no results found after filtering, show helpful message
          if (validResults.length === 0) {
            console.log('🔍 No results passed 75% relevance threshold for query:', keyword)
            if (allResults.length > 0) {
              console.log(`💡 ${allResults.length} results found but filtered out due to low relevance scores`)
            }
          }
          
          // Success case - no error should be thrown
          console.log('✅ AI Search API call completed successfully')
        } else {
          console.log('✅ AI Search API returned successful response with empty or no results')
          aiSearchResults.value = []
          aiModelInfo.value = {
            searchStrategy: 'No results found',
            aiSearchUsed: false,
            totalResults: 0,
            searchCriteria: null,
            searchInsights: {}
          }
          imageStore.images = []
        }
      } catch (aiError) {
        console.error('💥 AI search encountered an error:', aiError)
        
        // Only show error message if it's a real API error, not a parsing issue
        if (aiError.message && !aiError.message.includes('AI Search API failed: Unknown API error')) {
          console.log('🔄 Falling back to normal search...')
          // Fallback to normal search only if there's a real error
          try {
            await imageStore.searchImages(keyword, page, 20)
            aiSearchResults.value = []
            aiModelInfo.value = null
            console.log('✅ Fallback to normal search completed')
          } catch (fallbackError) {
            console.error('❌ Fallback search also failed:', fallbackError)
          }
        } else {
          // For parsing errors, just clear the results but don't fallback
          console.log('🔧 Response parsing issue - clearing results')
          aiSearchResults.value = []
          aiModelInfo.value = {
            searchStrategy: 'Response parsing failed',
            aiSearchUsed: false,
            totalResults: 0,
            searchCriteria: null,
            searchInsights: {}
          }
        }
      } finally {
        aiSearchLoading.value = false
      }
    }
  } catch (error) {
    console.error('Search failed:', error)
  }
}

const clearSearch = async () => {
  searchQuery.value = ''
  selectedTags.value = []
  isSearching.value = false
  aiSearchResults.value = []
  allAiSearchResults.value = []
  aiModelInfo.value = null
  aiCurrentPage.value = 1
  showThresholdWarning.value = false
  await imageStore.fetchImages(0)
}

const toggleTagFilter = (tag: string) => {
  const index = selectedTags.value.indexOf(tag)
  if (index > -1) {
    selectedTags.value.splice(index, 1)
  } else {
    selectedTags.value.push(tag)
  }
  
  // Auto-search when tags change
  if (selectedTags.value.length > 0 || searchQuery.value.trim()) {
    performSearch(0)
  } else {
    clearSearch()
  }
}

const openImageDetail = (image: Image) => {
  selectedImage.value = image
  showDetailModal.value = true
}

const closeImageDetail = () => {
  showDetailModal.value = false
  selectedImage.value = null
}

const deleteImage = async (image: Image) => {
  try {
    // Use the store's delete method which handles both API call and state update
    await imageStore.deleteImage(image.id)
    
    // Close modal
    closeImageDetail()
    
    // Show success message
    alert('Image deleted successfully')
  } catch (error) {
    console.error('Delete failed:', error)
    alert('Failed to delete image')
  }
}

const downloadImage = async (image: Image) => {
  try {
    // Increment download count
    await imageApi.downloadImage(image.id)
    
    // Create download link
    const link = document.createElement('a')
    link.href = image.cosUrl
    link.download = image.originalName
    link.target = '_blank'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    closeImageDetail()
  } catch (error) {
    console.error('Download failed:', error)
  }
}

const nextPage = () => {
  if (imageStore.hasNextPage) {
    loadImages(imageStore.pagination.page + 1)
  }
}

const previousPage = () => {
  if (imageStore.hasPrevPage) {
    loadImages(imageStore.pagination.page - 1)
  }
}

// AI search pagination functions
const nextAiPage = () => {
  if (aiCurrentPage.value < aiTotalPages.value) {
    aiCurrentPage.value++
    performSearch(aiCurrentPage.value - 1) // Convert to 0-based index
  }
}

const previousAiPage = () => {
  if (aiCurrentPage.value > 1) {
    aiCurrentPage.value--
    performSearch(aiCurrentPage.value - 1) // Convert to 0-based index
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString()
}

const parseTags = (tags: string): string[] => {
  return tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0)
}

const getDisplayName = (image: Image): string => {
  // If fileName exists and is different from originalName, extract custom name
  if (image.fileName && image.fileName !== image.originalName) {
    // Remove file extension and hash ID to get custom name
    const nameWithoutExt = image.fileName.substring(0, image.fileName.lastIndexOf('.'))
    const lastUnderscoreIndex = nameWithoutExt.lastIndexOf('_')
    if (lastUnderscoreIndex > 0) {
      return nameWithoutExt.substring(0, lastUnderscoreIndex)
    }
  }
  // Fallback to original name without extension
  const lastDotIndex = image.originalName.lastIndexOf('.')
  return lastDotIndex > 0 ? image.originalName.substring(0, lastDotIndex) : image.originalName
}

// Helper methods for empty state
const getEmptyStateTitle = (): string => {
  if (searchType.value === 'ai' && !isSearching.value) {
    // AI search mode but no search performed yet
    return 'Enter a search query'
  }
  if (isSearching.value) {
    return 'No images found'
  }
  // Check if there are any images in the store at all
  return imageStore.images.length === 0 ? 'No images yet' : 'No images to display'
}

const getEmptyStateMessage = (): string => {
  if (searchType.value === 'ai' && !isSearching.value) {
    return 'Use AI search to find images with natural language descriptions'
  }
  if (isSearching.value) {
    return 'Try adjusting your search criteria'
  }
  return imageStore.images.length === 0 ? 'Start by uploading your first image' : 'Try a different search or filter'
}

const shouldShowUploadButton = (): boolean => {
  // Only show upload button if there are truly no images and not searching
  return !isSearching.value && imageStore.images.length === 0
}

onMounted(() => {
  loadImages()
})
</script>