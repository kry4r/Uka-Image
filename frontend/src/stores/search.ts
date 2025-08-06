import { defineStore } from 'pinia'
import { ref } from 'vue'
import { searchApi, type SearchResponse, type SearchResult } from '@/api/search'

export const useSearchStore = defineStore('search', () => {
  // State
  const searchResults = ref<SearchResult[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastQuery = ref<string>('')
  const lastSearchType = ref<string>('')
  const searchTime = ref<number>(0)
  const totalCount = ref<number>(0)
  const suggestions = ref<string[]>([])

  // Actions
  const performSemanticSearch = async (query: string, page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await searchApi.semanticSearch(query, page, size) as SearchResponse
      searchResults.value = response.results
      lastQuery.value = query
      lastSearchType.value = 'semantic'
      searchTime.value = response.searchTime
      totalCount.value = response.totalCount
    } catch (err: any) {
      error.value = err.message || 'Search failed'
      console.error('Error performing semantic search:', err)
    } finally {
      loading.value = false
    }
  }

  const performVisualSearch = async (imageId: number, page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await searchApi.visualSearch(imageId, page, size) as SearchResponse
      searchResults.value = response.results
      lastQuery.value = `Image ID: ${imageId}`
      lastSearchType.value = 'visual'
      searchTime.value = response.searchTime
      totalCount.value = response.totalCount
    } catch (err: any) {
      error.value = err.message || 'Visual search failed'
      console.error('Error performing visual search:', err)
    } finally {
      loading.value = false
    }
  }

  const performColorSearch = async (color: string, page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await searchApi.colorSearch(color, page, size) as SearchResponse
      searchResults.value = response.results
      lastQuery.value = color
      lastSearchType.value = 'color'
      searchTime.value = response.searchTime
      totalCount.value = response.totalCount
    } catch (err: any) {
      error.value = err.message || 'Color search failed'
      console.error('Error performing color search:', err)
    } finally {
      loading.value = false
    }
  }

  const performSceneSearch = async (scene: string, page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await searchApi.sceneSearch(scene, page, size) as SearchResponse
      searchResults.value = response.results
      lastQuery.value = scene
      lastSearchType.value = 'scene'
      searchTime.value = response.searchTime
      totalCount.value = response.totalCount
    } catch (err: any) {
      error.value = err.message || 'Scene search failed'
      console.error('Error performing scene search:', err)
    } finally {
      loading.value = false
    }
  }

  const fetchSuggestions = async (query: string) => {
    if (!query.trim()) {
      suggestions.value = []
      return
    }
    
    try {
      const response = await searchApi.getSearchSuggestions(query) as string[]
      suggestions.value = response
    } catch (err: any) {
      console.error('Error fetching suggestions:', err)
      suggestions.value = []
    }
  }

  const clearResults = () => {
    searchResults.value = []
    lastQuery.value = ''
    lastSearchType.value = ''
    searchTime.value = 0
    totalCount.value = 0
    error.value = null
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // State
    searchResults,
    loading,
    error,
    lastQuery,
    lastSearchType,
    searchTime,
    totalCount,
    suggestions,
    // Actions
    performSemanticSearch,
    performVisualSearch,
    performColorSearch,
    performSceneSearch,
    fetchSuggestions,
    clearResults,
    clearError
  }
})