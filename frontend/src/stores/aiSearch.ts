import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { aiSearchApi, type EnhancedSearchResponse, type EnhancedSearchFilter, type EnhancedSuggestionsResponse, type ScoredResult } from '@/api/aiSearch'

export const useAiSearchStore = defineStore('aiSearch', () => {
  // State
  const searchResults = ref<ScoredResult[]>([])
  const searchResponse = ref<EnhancedSearchResponse | null>(null)
  const suggestions = ref<EnhancedSuggestionsResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastQuery = ref<string>('')
  const currentPage = ref(1)
  const pageSize = ref(20)
  const filters = ref<EnhancedSearchFilter>({
    fileFormats: [],
    minScore: 0.1
  })

  // Getters
  const hasResults = computed(() => searchResults.value.length > 0)
  const totalResults = computed(() => searchResponse.value?.totalResults || 0)
  const totalPages = computed(() => searchResponse.value?.totalPages || 0)
  const hasNextPage = computed(() => currentPage.value < totalPages.value)
  const hasPrevPage = computed(() => currentPage.value > 1)
  const isAiSearchUsed = computed(() => searchResponse.value?.aiSearchUsed || false)

  // Actions
  const performSearch = async (query: string, page: number = 1, size: number = 20, searchFilters?: EnhancedSearchFilter) => {
    loading.value = true
    error.value = null
    
    try {
      const appliedFilters = searchFilters || filters.value
      const response = await aiSearchApi.enhancedSearch(query, page, size, appliedFilters)
      
      searchResponse.value = response.data.data
      searchResults.value = searchResponse.value.results
      lastQuery.value = query
      currentPage.value = page
      pageSize.value = size
      
      return searchResponse.value
    } catch (err: any) {
      error.value = err.message || 'Search failed'
      console.error('Error performing AI search:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const loadSuggestions = async () => {
    try {
      const response = await aiSearchApi.getEnhancedSuggestions()
      suggestions.value = response.data.data
      return suggestions.value
    } catch (err: any) {
      console.error('Failed to load search suggestions:', err)
      return null
    }
  }

  const checkAIHealth = async () => {
    try {
      const response = await aiSearchApi.checkHealth()
      return response.data.data === 'OK'
    } catch (err) {
      console.error('AI health check failed:', err)
      return false
    }
  }

  const getSearchAnalytics = async () => {
    try {
      const response = await aiSearchApi.getSearchAnalytics()
      return response.data.data
    } catch (err) {
      console.error('Failed to get search analytics:', err)
      return null
    }
  }

  const goToPage = async (page: number) => {
    if (page < 1 || (totalPages.value > 0 && page > totalPages.value)) {
      return
    }
    
    return performSearch(lastQuery.value, page, pageSize.value, filters.value)
  }

  const updateFilters = (newFilters: Partial<EnhancedSearchFilter>) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const resetFilters = () => {
    filters.value = {
      fileFormats: [],
      orientation: undefined,
      resolutionCategory: undefined,
      contentCategory: undefined,
      minScore: 0.1
    }
  }

  const clearSearch = () => {
    searchResults.value = []
    searchResponse.value = null
    lastQuery.value = ''
    currentPage.value = 1
    error.value = null
    resetFilters()
  }

  return {
    // State
    searchResults,
    searchResponse,
    suggestions,
    loading,
    error,
    lastQuery,
    currentPage,
    pageSize,
    filters,
    // Getters
    hasResults,
    totalResults,
    totalPages,
    hasNextPage,
    hasPrevPage,
    isAiSearchUsed,
    // Actions
    performSearch,
    loadSuggestions,
    checkAIHealth,
    getSearchAnalytics,
    goToPage,
    updateFilters,
    resetFilters,
    clearSearch
  }
})