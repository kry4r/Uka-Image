<template>
  <AppLayout>
    <div v-if="imageStore.loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
    </div>

    <div v-else-if="imageStore.error" class="text-center py-12">
      <p class="text-red-600 mb-4">{{ imageStore.error }}</p>
      <router-link to="/gallery" class="btn-primary">
        Back to Gallery
      </router-link>
    </div>

    <div v-else-if="image" class="max-w-6xl mx-auto space-y-6">
      <!-- Header -->
      <div class="flex items-center justify-between">
        <button
          @click="$router.go(-1)"
          class="flex items-center text-gray-600 hover:text-gray-900 transition-colors duration-200"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          Back
        </button>
        
        <div class="flex space-x-2">
          <button
            @click="downloadImage"
            class="btn-secondary"
          >
            Download
          </button>
          <button
            @click="showDeleteModal = true"
            class="bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-200"
          >
            Delete
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Image Display -->
        <div class="lg:col-span-2">
          <div class="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <img
              :src="image.cosUrl"
              :alt="image.originalName"
              class="w-full h-auto max-h-screen object-contain"
              @load="imageLoaded = true"
            />
          </div>
        </div>

        <!-- Image Information -->
        <div class="space-y-6">
          <!-- Basic Info -->
          <div class="card p-6">
            <h1 class="text-2xl font-bold text-gray-900 mb-4">{{ image.originalName }}</h1>
            
            <div class="space-y-3">
              <div class="flex justify-between">
                <span class="text-gray-600">File Size:</span>
                <span class="font-medium">{{ formatFileSize(image.fileSize) }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-600">Dimensions:</span>
                <span class="font-medium">{{ image.width }}×{{ image.height }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-600">Format:</span>
                <span class="font-medium">{{ image.mimeType }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-gray-600">Uploaded:</span>
                <span class="font-medium">{{ formatDate(image.uploadTime) }}</span>
              </div>
            </div>
          </div>

          <!-- Description -->
          <div class="card p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-lg font-semibold text-gray-900">Description</h3>
              <button
                @click="editingDescription = !editingDescription"
                class="text-primary-600 hover:text-primary-700 text-sm font-medium"
              >
                {{ editingDescription ? 'Cancel' : 'Edit' }}
              </button>
            </div>
            
            <div v-if="!editingDescription">
              <p class="text-gray-700">
                {{ image.description || 'No description available' }}
              </p>
            </div>
            
            <div v-else class="space-y-3">
              <textarea
                v-model="newDescription"
                rows="4"
                class="input-field resize-none"
                placeholder="Enter image description..."
              ></textarea>
              <div class="flex space-x-2">
                <button
                  @click="updateDescription"
                  :disabled="updatingDescription"
                  class="btn-primary text-sm disabled:opacity-50"
                >
                  {{ updatingDescription ? 'Saving...' : 'Save' }}
                </button>
                <button
                  @click="generateAIDescription"
                  :disabled="generatingDescription"
                  class="btn-secondary text-sm disabled:opacity-50"
                >
                  {{ generatingDescription ? 'Generating...' : 'AI Generate' }}
                </button>
              </div>
            </div>
          </div>

          <!-- Tags -->
          <div class="card p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Tags</h3>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="tag in imageTags"
                :key="tag"
                class="px-3 py-1 bg-primary-100 text-primary-700 text-sm rounded-full"
              >
                {{ tag }}
              </span>
              <span v-if="imageTags.length === 0" class="text-gray-500 text-sm">
                No tags available
              </span>
            </div>
          </div>

          <!-- Color Palette -->
          <div v-if="image.colorPalette" class="card p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Color Palette</h3>
            <div class="flex space-x-2">
              <div
                v-for="color in colorPalette"
                :key="color"
                class="w-8 h-8 rounded border border-gray-300"
                :style="{ backgroundColor: color }"
                :title="color"
              ></div>
            </div>
          </div>

          <!-- Similar Images -->
          <div class="card p-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Similar Images</h3>
            <button
              @click="findSimilarImages"
              :disabled="loadingSimilar"
              class="btn-secondary w-full disabled:opacity-50"
            >
              {{ loadingSimilar ? 'Finding...' : 'Find Similar Images' }}
            </button>
            
            <div v-if="similarImages.length > 0" class="mt-4 grid grid-cols-2 gap-2">
              <div
                v-for="similar in similarImages"
                :key="similar.image.id"
                class="cursor-pointer"
                @click="$router.push(`/image/${similar.image.id}`)"
              >
                <img
                  :src="similar.image.thumbnailUrl || similar.image.cosUrl"
                  :alt="similar.image.originalName"
                  class="w-full h-20 object-cover rounded border hover:border-primary-300 transition-colors duration-200"
                />
                <p class="text-xs text-gray-500 mt-1 truncate">
                  {{ Math.round(similar.score * 100) }}% match
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div
      v-if="showDeleteModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click="showDeleteModal = false"
    >
      <div
        class="bg-white rounded-lg p-6 max-w-md w-full mx-4"
        @click.stop
      >
        <h3 class="text-lg font-semibold text-gray-900 mb-4">Delete Image</h3>
        <p class="text-gray-600 mb-6">
          Are you sure you want to delete this image? This action cannot be undone.
        </p>
        <div class="flex space-x-3">
          <button
            @click="deleteImage"
            :disabled="deleting"
            class="flex-1 bg-red-600 hover:bg-red-700 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-200 disabled:opacity-50"
          >
            {{ deleting ? 'Deleting...' : 'Delete' }}
          </button>
          <button
            @click="showDeleteModal = false"
            class="flex-1 btn-secondary"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useImageStore } from '@/stores/image'
import { useSearchStore } from '@/stores/search'
import AppLayout from '@/components/Layout/AppLayout.vue'
import type { SearchResult } from '@/api/search'

const route = useRoute()
const router = useRouter()
const imageStore = useImageStore()
const searchStore = useSearchStore()

const imageId = computed(() => parseInt(route.params.id as string))
const image = computed(() => imageStore.currentImage)

const imageLoaded = ref(false)
const editingDescription = ref(false)
const newDescription = ref('')
const updatingDescription = ref(false)
const generatingDescription = ref(false)
const showDeleteModal = ref(false)
const deleting = ref(false)
const loadingSimilar = ref(false)
const similarImages = ref<SearchResult[]>([])

const imageTags = computed(() => {
  if (!image.value?.tags) return []
  return image.value.tags.split(',').map(tag => tag.trim()).filter(tag => tag)
})

const colorPalette = computed(() => {
  if (!image.value?.colorPalette) return []
  try {
    return JSON.parse(image.value.colorPalette)
  } catch {
    return []
  }
})

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const updateDescription = async () => {
  if (!image.value) return
  
  updatingDescription.value = true
  try {
    await imageStore.updateImageDescription(image.value.id, newDescription.value)
    editingDescription.value = false
  } catch (error) {
    console.error('Failed to update description:', error)
  } finally {
    updatingDescription.value = false
  }
}

const generateAIDescription = async () => {
  if (!image.value) return
  
  generatingDescription.value = true
  try {
    // Simulate AI description generation
    await new Promise(resolve => setTimeout(resolve, 2000))
    newDescription.value = `AI-generated description for ${image.value.originalName}: This image appears to contain visual elements that suggest...`
  } catch (error) {
    console.error('Failed to generate AI description:', error)
  } finally {
    generatingDescription.value = false
  }
}

const downloadImage = () => {
  if (!image.value) return
  
  const link = document.createElement('a')
  link.href = image.value.cosUrl
  link.download = image.value.originalName
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const deleteImage = async () => {
  if (!image.value) return
  
  deleting.value = true
  try {
    await imageStore.deleteImage(image.value.id)
    router.push('/gallery')
  } catch (error) {
    console.error('Failed to delete image:', error)
  } finally {
    deleting.value = false
    showDeleteModal.value = false
  }
}

const findSimilarImages = async () => {
  if (!image.value) return
  
  loadingSimilar.value = true
  try {
    await searchStore.performVisualSearch(image.value.id, 0, 6)
    similarImages.value = searchStore.searchResults.slice(0, 6)
  } catch (error) {
    console.error('Failed to find similar images:', error)
  } finally {
    loadingSimilar.value = false
  }
}

onMounted(async () => {
  try {
    const fetchedImage = await imageStore.fetchImageById(imageId.value)
    newDescription.value = fetchedImage.description || ''
  } catch (error) {
    console.error('Failed to load image:', error)
  }
})
</script>