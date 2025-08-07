import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { imageApi, type Image, type PaginatedResponse, type ApiResponse } from '@/api/image'

export const useImageStore = defineStore('image', () => {
  // State
  const images = ref<Image[]>([])
  const currentImage = ref<Image | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0
  })

  // Getters
  const hasImages = computed(() => images.value && images.value.length > 0)
  const hasNextPage = computed(() => pagination.value.page < pagination.value.totalPages - 1)
  const hasPrevPage = computed(() => pagination.value.page > 0)

  // Actions
  const fetchImages = async (page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await imageApi.getImages(page, size)
      // The API returns data wrapped in ApiResponse format
      const data = response.data || response
      images.value = data.records || []
      pagination.value = {
        page: (data.current || 1) - 1,
        size: data.size || size,
        totalElements: data.total || 0,
        totalPages: data.pages || 1
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch images'
      console.error('Error fetching images:', err)
    } finally {
      loading.value = false
    }
  }

  const fetchImageById = async (id: number) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await imageApi.getImageById(id)
      const apiData = response.data as ApiResponse<Image>
      const image = apiData.data
      currentImage.value = image
      return image
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch image'
      console.error('Error fetching image:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const searchImages = async (keyword: string, page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await imageApi.searchImages(keyword, page, size)
      // Handle the response structure properly
      const apiData = response.data
      const data = apiData.data || apiData
      
      images.value = data.records || []
      pagination.value = {
        page: (data.current || 1) - 1,
        size: data.size || size,
        totalElements: data.total || 0,
        totalPages: data.pages || 1
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to search images'
      console.error('Error searching images:', err)
      // If search fails, clear the images to show empty state
      images.value = []
      pagination.value = {
        page: 0,
        size: size,
        totalElements: 0,
        totalPages: 0
      }
    } finally {
      loading.value = false
    }
  }

  const aiSearchImages = async (query: string, page: number = 0, size: number = 20) => {
    loading.value = true
    error.value = null
    
    try {
      const response = await imageApi.aiSearch(query, page, size)
      const apiData = response.data
      
      // AI search returns data directly in the data field (not nested)
      images.value = apiData.data || apiData || []
      
      // For AI search, we don't have traditional pagination info
      // So we estimate based on the returned results
      pagination.value = {
        page: page,
        size: size,
        totalElements: images.value.length,
        totalPages: Math.ceil(images.value.length / size)
      }
      
      console.log('AI search completed:', images.value.length, 'images found')
    } catch (err: any) {
      error.value = err.message || 'AI search failed'
      console.error('Error in AI search:', err)
      // If AI search fails, clear the images to show empty state
      images.value = []
      pagination.value = {
        page: 0,
        size: size,
        totalElements: 0,
        totalPages: 0
      }
    } finally {
      loading.value = false
    }
  }

  const checkAIHealth = async () => {
    try {
      const response = await imageApi.checkAIHealth()
      return response.data.data === 'OK'
    } catch (err: any) {
      console.error('AI health check failed:', err)
      return false
    }
  }

  const getAISearchSuggestions = async () => {
    try {
      const response = await imageApi.getAISearchSuggestions()
      return response.data.data || []
    } catch (err: any) {
      console.error('Failed to get AI search suggestions:', err)
      return []
    }
  }

  const uploadImage = async (file: File, description?: string, tags?: string, customName?: string, hashId?: string) => {
    loading.value = true
    error.value = null
    
    try {
      const formData = new FormData()
      formData.append('file', file)
      if (description) formData.append('description', description)
      if (tags) formData.append('tags', tags)
      if (customName) formData.append('customName', customName)
      if (hashId) formData.append('hashId', hashId)
      
      const response = await imageApi.uploadImage(formData)
      await fetchImages() // Refresh the list
      return response
    } catch (err: any) {
      error.value = err.message || 'Failed to upload image'
      console.error('Error uploading image:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateImageDescription = async (id: number, description: string) => {
    try {
      await imageApi.updateDescription(id, description)
      // Update local state
      if (currentImage.value?.id === id) {
        currentImage.value.description = description
      }
      const imageIndex = images.value.findIndex(img => img.id === id)
      if (imageIndex !== -1) {
        images.value[imageIndex].description = description
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to update description'
      throw err
    }
  }

  const deleteImage = async (id: number) => {
    try {
      await imageApi.deleteImage(id)
      images.value = images.value.filter(img => img.id !== id)
      if (currentImage.value?.id === id) {
        currentImage.value = null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to delete image'
      throw err
    }
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // State
    images,
    currentImage,
    loading,
    error,
    pagination,
    // Getters
    hasImages,
    hasNextPage,
    hasPrevPage,
    // Actions
    fetchImages,
    fetchImageById,
    searchImages,
    aiSearchImages,
    checkAIHealth,
    getAISearchSuggestions,
    uploadImage,
    updateImageDescription,
    deleteImage,
    clearError
  }
})