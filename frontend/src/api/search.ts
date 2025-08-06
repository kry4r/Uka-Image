import api from './index'
import type { Image } from './image'

export interface SearchRequest {
  query: string
  searchType: 'semantic' | 'visual' | 'color' | 'scene'
  page?: number
  size?: number
}

export interface SearchResult {
  image: Image
  score: number
  matchType: string
}

export interface SearchResponse {
  results: SearchResult[]
  totalCount: number
  searchTime: number
  query: string
  searchType: string
}

export const searchApi = {
  // Semantic search
  semanticSearch: (query: string, page: number = 0, size: number = 20) => {
    return api.post<SearchResponse>('/search/semantic', {
      query,
      page,
      size
    })
  },

  // Visual similarity search
  visualSearch: (imageId: number, page: number = 0, size: number = 20) => {
    return api.post<SearchResponse>('/search/visual', {
      imageId,
      page,
      size
    })
  },

  // Color-based search
  colorSearch: (color: string, page: number = 0, size: number = 20) => {
    return api.post<SearchResponse>('/search/color', {
      color,
      page,
      size
    })
  },

  // Scene-based search
  sceneSearch: (scene: string, page: number = 0, size: number = 20) => {
    return api.post<SearchResponse>('/search/scene', {
      scene,
      page,
      size
    })
  },

  // Get search suggestions
  getSearchSuggestions: (query: string) => {
    return api.get<string[]>(`/search/suggestions?q=${encodeURIComponent(query)}`)
  }
}