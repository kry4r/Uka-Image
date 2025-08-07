<template>
  <AppLayout>
    <div class="max-w-4xl mx-auto space-y-6">
      <!-- Header -->
      <div class="text-center">
        <h1 class="text-3xl font-bold text-gray-900">Upload Images</h1>
        <p class="mt-2 text-gray-600">Drag and drop your images or click to browse</p>
      </div>

      <!-- Upload Area -->
      <div
        class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-primary-400 transition-colors duration-200"
        :class="{ 'border-primary-400 bg-primary-50': isDragOver }"
        @drop="handleDrop"
        @dragover="handleDragOver"
        @dragenter="handleDragEnter"
        @dragleave="handleDragLeave"
      >
        <div class="space-y-4">
          <div class="mx-auto h-12 w-12 text-gray-400">
            <svg fill="none" stroke="currentColor" viewBox="0 0 48 48">
              <path
                d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <div>
            <p class="text-lg font-medium text-gray-900">
              Drop your images here, or
              <button
                @click="$refs.fileInput.click()"
                class="text-primary-600 hover:text-primary-500 font-medium"
              >
                browse
              </button>
            </p>
            <p class="text-sm text-gray-500 mt-1">
              Supports JPG, PNG, GIF up to 10MB each
            </p>
          </div>
        </div>
        
        <input
          ref="fileInput"
          type="file"
          multiple
          accept="image/*"
          class="hidden"
          @change="handleFileSelect"
        />
      </div>

      <!-- Selected Files -->
      <div v-if="selectedFiles.length > 0" class="space-y-4">
        <h3 class="text-lg font-medium text-gray-900">
          Selected Files ({{ selectedFiles.length }})
        </h3>
        
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="(file, index) in selectedFiles"
            :key="index"
            class="relative bg-white rounded-lg border border-gray-200 p-4"
          >
            <div class="flex items-start space-x-3">
              <img
                :src="file.preview"
                :alt="file.file.name"
                class="w-16 h-16 object-cover rounded-lg"
              />
              <div class="flex-1 min-w-0">
                <!-- Editable Image Name -->
                <div class="mb-2">
                  <label class="text-xs text-gray-500 block mb-1">Image Name</label>
                  <input
                    v-model="file.customName"
                    type="text"
                    placeholder="Enter custom name..."
                    class="w-full text-sm px-2 py-1 border border-gray-200 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 font-medium"
                  />
                  <p class="text-xs text-gray-400 mt-1">
                    Final name: {{ file.customName || getFileNameWithoutExt(file.file.name) }}_{{ file.hashId }}
                  </p>
                </div>
                
                <p class="text-xs text-gray-500 mb-2">
                  Original: {{ file.file.name }} ({{ formatFileSize(file.file.size) }})
                </p>
                
                <!-- Description Input -->
                <div class="mt-2">
                  <input
                    v-model="file.description"
                    type="text"
                    placeholder="Add description..."
                    class="w-full text-xs px-2 py-1 border border-gray-200 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
                  />
                </div>
                
                <!-- Tags Input -->
                <div class="mt-1">
                  <TagInput
                    v-model="file.tagList"
                    placeholder="Add tags..."
                    :suggested-tags="suggestedTags"
                    class="text-xs"
                  />
                </div>
              </div>
              
              <button
                @click="removeFile(index)"
                class="text-gray-400 hover:text-red-500 transition-colors duration-200"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <!-- Upload Progress -->
            <div v-if="file.uploading" class="mt-3">
              <div class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="bg-primary-600 h-2 rounded-full transition-all duration-300"
                  :style="{ width: file.progress + '%' }"
                ></div>
              </div>
              <p class="text-xs text-gray-500 mt-1">{{ file.progress }}% uploaded</p>
            </div>
            
            <!-- Upload Status -->
            <div v-if="file.uploaded" class="mt-2 flex items-center text-green-600">
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              <span class="text-xs">Uploaded successfully</span>
            </div>
            
            <div v-if="file.error" class="mt-2 flex items-center text-red-600">
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
              <span class="text-xs">{{ file.error }}</span>
            </div>
          </div>
        </div>

        <!-- Upload Actions -->
        <div class="flex justify-between items-center pt-4 border-t border-gray-200">
          <button
            @click="clearFiles"
            class="btn-secondary"
            :disabled="uploading"
          >
            Clear All
          </button>
          
          <div class="flex space-x-3">
            <button
              @click="uploadFiles"
              :disabled="uploading || selectedFiles.length === 0"
              class="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ uploading ? 'Uploading...' : `Upload ${selectedFiles.length} Files` }}
            </button>
          </div>
        </div>
      </div>

      <!-- Upload Results -->
      <div v-if="uploadResults.length > 0" class="mt-8">
        <h3 class="text-lg font-medium text-gray-900 mb-4">Upload Results</h3>
        <div class="bg-white rounded-lg border border-gray-200 p-4">
          <p class="text-sm text-gray-600">
            Successfully uploaded {{ uploadResults.filter(r => r.success).length }} of {{ uploadResults.length }} files
          </p>
          <div class="mt-4 flex space-x-3">
            <router-link to="/gallery" class="btn-primary">
              View Gallery
            </router-link>
            <button @click="resetUpload" class="btn-secondary">
              Upload More
            </button>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useImageStore } from '@/stores/image'
import AppLayout from '@/components/Layout/AppLayout.vue'
import TagInput from '@/components/common/TagInput.vue'

interface FileWithMetadata {
  file: File
  preview: string
  customName: string
  hashId: string
  description: string
  tagList: string[]
  uploading: boolean
  uploaded: boolean
  progress: number
  error: string | null
}

const imageStore = useImageStore()

const selectedFiles = ref<FileWithMetadata[]>([])
const isDragOver = ref(false)
const uploading = ref(false)
const uploadResults = ref<Array<{ success: boolean; filename: string; error?: string }>>([])

// Suggested tags based on common image categories
const suggestedTags = ref([
  'nature', 'landscape', 'portrait', 'architecture', 'food', 'travel',
  'art', 'photography', 'design', 'technology', 'business', 'lifestyle',
  'animals', 'flowers', 'sunset', 'city', 'beach', 'mountains'
])

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = true
}

const handleDragEnter = (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = true
}

const handleDragLeave = (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = false
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDragOver.value = false
  
  const files = Array.from(e.dataTransfer?.files || [])
  addFiles(files)
}

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement
  const files = Array.from(target.files || [])
  addFiles(files)
}

const addFiles = (files: File[]) => {
  const imageFiles = files.filter(file => file.type.startsWith('image/'))
  
  imageFiles.forEach(file => {
    if (file.size > 10 * 1024 * 1024) { // 10MB limit
      alert(`File ${file.name} is too large. Maximum size is 10MB.`)
      return
    }
    
    const reader = new FileReader()
    reader.onload = (e) => {
      selectedFiles.value.push({
        file,
        preview: e.target?.result as string,
        customName: getFileNameWithoutExt(file.name),
        hashId: generateHashId(),
        description: '',
        tagList: [],
        uploading: false,
        uploaded: false,
        progress: 0,
        error: null
      })
    }
    reader.readAsDataURL(file)
  })
}

const generateHashId = (): string => {
  const timestamp = Date.now().toString(36)
  const randomStr = Math.random().toString(36).substring(2, 8)
  return `${timestamp}${randomStr}`
}

const getFileNameWithoutExt = (filename: string): string => {
  const lastDotIndex = filename.lastIndexOf('.')
  return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename
}

const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
}

const clearFiles = () => {
  selectedFiles.value = []
  uploadResults.value = []
}

const uploadFiles = async () => {
  uploading.value = true
  uploadResults.value = []
  
  for (const fileData of selectedFiles.value) {
    if (fileData.uploaded) continue
    
    fileData.uploading = true
    fileData.progress = 0
    fileData.error = null
    
    try {
      // Simulate progress
      const progressInterval = setInterval(() => {
        if (fileData.progress < 90) {
          fileData.progress += Math.random() * 20
        }
      }, 200)
      
      // Convert tag list to comma-separated string
      const tagsString = fileData.tagList.join(',')
      
      // Use custom name with hash ID
      const finalName = fileData.customName || getFileNameWithoutExt(fileData.file.name)
      
      await imageStore.uploadImage(fileData.file, fileData.description, tagsString, finalName, fileData.hashId)
      
      clearInterval(progressInterval)
      fileData.progress = 100
      fileData.uploading = false
      fileData.uploaded = true
      
      uploadResults.value.push({
        success: true,
        filename: fileData.file.name
      })
    } catch (error: any) {
      fileData.uploading = false
      fileData.error = error.message || 'Upload failed'
      
      uploadResults.value.push({
        success: false,
        filename: fileData.file.name,
        error: error.message
      })
    }
  }
  
  uploading.value = false
}

const resetUpload = () => {
  selectedFiles.value = []
  uploadResults.value = []
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>