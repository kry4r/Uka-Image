<template>
  <div v-if="visible" class="fixed inset-0 z-50 overflow-y-auto" @click="closeModal">
    <div class="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
      <!-- Background overlay -->
      <div class="fixed inset-0 transition-opacity bg-gray-500 bg-opacity-75" aria-hidden="true"></div>
      
      <!-- Modal panel -->
      <div 
        class="inline-block w-full max-w-4xl p-6 my-8 overflow-hidden text-left align-middle transition-all transform bg-white shadow-xl rounded-lg"
        @click.stop
      >
        <!-- Close button -->
        <button
          @click="closeModal"
          class="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <!-- Image preview -->
          <div class="flex items-center justify-center bg-gray-50 rounded-lg overflow-hidden">
            <img
              :src="image.cosUrl"
              :alt="image.originalName"
              class="max-w-full max-h-96 object-contain"
            />
          </div>

          <!-- Image details -->
          <div class="space-y-4">
            <div>
              <h3 class="text-xl font-semibold text-gray-900 mb-2">{{ getDisplayName(image) }}</h3>
              <p class="text-sm text-gray-500">Original: {{ image.originalName }}</p>
              <p class="text-sm text-gray-500">Uploaded {{ formatDate(image.createdAt) }}</p>
            </div>

            <!-- Basic info -->
            <div class="space-y-3">
              <div class="flex justify-between items-center py-2 border-b border-gray-100">
                <span class="text-sm font-medium text-gray-600">File Size</span>
                <span class="text-sm text-gray-900">{{ formatFileSize(image.fileSize) }}</span>
              </div>
              
              <div class="flex justify-between items-center py-2 border-b border-gray-100">
                <span class="text-sm font-medium text-gray-600">Format</span>
                <span class="text-sm text-gray-900">{{ image.fileType }}</span>
              </div>
              
              <div class="flex justify-between items-center py-2 border-b border-gray-100">
                <span class="text-sm font-medium text-gray-600">Resolution</span>
                <span class="text-sm text-gray-900">{{ image.width }} × {{ image.height }}</span>
              </div>
              
              <div class="flex justify-between items-center py-2 border-b border-gray-100">
                <span class="text-sm font-medium text-gray-600">Views</span>
                <span class="text-sm text-gray-900">{{ image.viewCount }}</span>
              </div>
              
              <div class="flex justify-between items-center py-2 border-b border-gray-100">
                <span class="text-sm font-medium text-gray-600">Downloads</span>
                <span class="text-sm text-gray-900">{{ image.downloadCount }}</span>
              </div>
            </div>

            <!-- Storage URL -->
            <div class="space-y-2">
              <label class="text-sm font-medium text-gray-600">Storage URL</label>
              <div class="flex items-center space-x-2">
                <input
                  :value="image.cosUrl"
                  readonly
                  class="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md bg-gray-50 focus:outline-none"
                />
              </div>
            </div>

            <!-- Description -->
            <div v-if="image.description" class="space-y-2">
              <label class="text-sm font-medium text-gray-600">Description</label>
              <p class="text-sm text-gray-900 p-3 bg-gray-50 rounded-md">{{ image.description }}</p>
            </div>

            <!-- Tags -->
            <div v-if="image.tags" class="space-y-2">
              <label class="text-sm font-medium text-gray-600">Tags</label>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="tag in parseTags(image.tags)"
                  :key="tag"
                  class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                >
                  {{ tag }}
                </span>
              </div>
            </div>

            <!-- Action buttons -->
            <div class="flex space-x-3 pt-4">
              <button
                @click="downloadImage"
                class="flex-1 px-4 py-2 bg-green-600 text-white text-sm font-medium rounded-md hover:bg-green-700 transition-colors"
              >
                Download
              </button>
              <button
                @click="deleteImage"
                class="flex-1 px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-md hover:bg-red-700 transition-colors"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Image } from '@/api/image'

interface Props {
  visible: boolean
  image: Image | null
}

interface Emits {
  (e: 'close'): void
  (e: 'delete', image: Image): void
  (e: 'download', image: Image): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const closeModal = () => {
  emit('close')
}

const deleteImage = () => {
  if (props.image) {
    if (confirm('Are you sure you want to delete this image? This action cannot be undone.')) {
      emit('delete', props.image)
    }
  }
}

const downloadImage = () => {
  if (props.image) {
    emit('download', props.image)
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    // You can add a toast notification here
    alert('URL copied to clipboard!')
  } catch (err) {
    console.error('Failed to copy: ', err)
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
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
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
</script>
