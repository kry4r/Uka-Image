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
      <div v-if="imageStore.loading" class="flex justify-center py-12">
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
      <div v-else-if="!imageStore.hasImages" class="text-center py-12">
        <div class="max-w-md mx-auto">
          <h3 class="text-lg font-medium text-gray-900 mb-2">
            {{ isSearching ? 'No images found' : 'No images yet' }}
          </h3>
          <p class="text-gray-500 mb-6">
            {{ isSearching ? 'Try adjusting your search criteria' : 'Start by uploading your first image' }}
          </p>
          <router-link v-if="!isSearching" to="/upload" class="btn-primary">
            Upload Images
          </router-link>
        </div>
      </div>

      <!-- Image Grid -->
      <div v-else>
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
                {{ image.originalName }}
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

        <!-- Pagination -->
        <div class="flex justify-center items-center space-x-4 mt-8">
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
import { imageApi, type Image } from '@/api/image'
import AppLayout from '@/components/Layout/AppLayout.vue'
import ImageDetailModal from '@/components/common/ImageDetailModal.vue'

const router = useRouter()
const imageStore = useImageStore()

// Search and filter state
const searchType = ref<'normal' | 'ai'>('normal')
const searchQuery = ref('')
const selectedTags = ref<string[]>([])
const isSearching = ref(false)

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

const performSearch = async (page: number = 0) => {
  if (!searchQuery.value.trim() && selectedTags.value.length === 0) {
    return
  }

  isSearching.value = true
  
  try {
    let keyword = searchQuery.value.trim()
    
    // Add selected tags to search query
    if (selectedTags.value.length > 0) {
      const tagQuery = selectedTags.value.join(',')
      keyword = keyword ? `${keyword} ${tagQuery}` : tagQuery
    }

    if (searchType.value === 'normal') {
      await imageStore.searchImages(keyword, page, 20)
    } else {
      // AI search - placeholder for future implementation
      console.log('AI search not implemented yet')
      await imageStore.searchImages(keyword, page, 20)
    }
  } catch (error) {
    console.error('Search failed:', error)
  }
}

const clearSearch = async () => {
  searchQuery.value = ''
  selectedTags.value = []
  isSearching.value = false
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

onMounted(() => {
  loadImages()
})
</script>
