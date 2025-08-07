import api from './index'

export interface EnhancedSearchFilter {
  fileFormats?: string[]
  orientation?: string
  resolutionCategory?: string
  contentCategory?: string
  minBrightness?: number
  maxBrightness?: number
  minScore?: number
  hasTransparency?: boolean
}

export interface ScoredResult {
  imageId: number
  image: any
  totalScore: number
  descriptionScore: number
  tagScore: number
  filenameScore: number
  metadataScore: number
  bonusScore: number
  penaltyScore: number
  explanation: string
  confidenceLevel: string
}

export interface SearchCriteria {
  originalQuery: string
  normalizedQuery: string
  primaryType: string
  complexity: string
  searchTerms: string[]
  keywords: string[]
  phrases: string[]
  hasNegation: boolean
  hasComparison: boolean
  hasTimeReference: boolean
  technicalFilters: any
  visualFilters: any
  contentFilters: any
  searchWeights: any
}

export interface EnhancedSearchResponse {
  results: ScoredResult[]
  totalResults: number
  currentPage: number
  pageSize: number
  totalPages: number
  searchCriteria: SearchCriteria
  searchStrategy: string
  aiSearchUsed: boolean
  searchInsights: Record<string, any>
}

export interface EnhancedSuggestionsResponse {
  popularTags: string[]
  categorySuggestions: Record<string, string[]>
  technicalFilters: Record<string, string[]>
  recentSearches: string[]
}

export interface SearchAnalyticsResponse {
  totalImages: number
  totalSearches: number
  successfulSearches: number
  averageResultsPerSearch: number
  averageSearchTime: number
  popularSearchTerms: Record<string, number>
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export const aiSearchApi = {
  // Enhanced AI search with filters
  enhancedSearch: (
    query: string,
    page: number = 1,
    size: number = 20,
    filters?: EnhancedSearchFilter
  ) => {
    let url = `/ai-search/search?query=${encodeURIComponent(query)}&pageNum=${page}&pageSize=${size}`
    
    if (filters) {
      if (filters.fileFormats && filters.fileFormats.length > 0) {
        url += `&fileFormats=${filters.fileFormats.join(',')}`
      }
      
      if (filters.orientation) {
        url += `&orientation=${filters.orientation}`
      }
      
      if (filters.minScore !== undefined) {
        url += `&minScore=${filters.minScore}`
      }
    }
    
    return api.get<ApiResponse<EnhancedSearchResponse>>(url)
  },
  
  // Legacy AI search for backward compatibility
  legacySearch: (query: string, page: number = 1, size: number = 20) => {
    return api.get<ApiResponse<any[]>>(`/ai-search/search/legacy?query=${encodeURIComponent(query)}&pageNum=${page}&pageSize=${size}`)
  },
  
  // Check AI service health
  checkHealth: () => {
    return api.get<ApiResponse<string>>('/ai-search/health')
  },
  
  // Get enhanced search suggestions
  getEnhancedSuggestions: () => {
    return api.get<ApiResponse<EnhancedSuggestionsResponse>>('/ai-search/suggestions')
  },
  
  // Get search analytics
  getSearchAnalytics: () => {
    return api.get<ApiResponse<SearchAnalyticsResponse>>('/ai-search/analytics')
  }
}