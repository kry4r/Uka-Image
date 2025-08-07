<template>
  <div class="search-result-card" @click="$emit('click', image)">
    <!-- Image Preview -->
    <div class="image-preview">
      <img 
        :src="image.thumbnailUrl || image.cosUrl" 
        :alt="image.description || image.originalName"
        class="preview-image"
        @error="handleImageError"
      />
      
      <!-- Overlay with metadata matches -->
      <div class="metadata-overlay" v-if="metadataMatches && metadataMatches.length > 0">
        <div class="match-indicators">
          <span 
            v-for="match in metadataMatches.slice(0, 3)" 
            :key="match.type"
            :class="['match-badge', `match-${match.type}`]"
            :title="match.explanation"
          >
            {{ match.type }}
          </span>
        </div>
      </div>
      
      <!-- Score indicator -->
      <div class="score-indicator" v-if="props.result.totalScore !== undefined">
        <div class="score-bar">
          <div 
            class="score-fill" 
            :style="{ width: `${Math.round(props.result.totalScore * 100)}%` }"
            :class="getScoreClass(props.result.totalScore)"
          ></div>
        </div>
        <span class="score-text">{{ Math.round(props.result.totalScore * 100) }}%</span>
      </div>
    </div>

    <!-- Image Information -->
    <div class="image-info">
      <!-- Title -->
      <h4 class="image-title" :title="image.originalName">
        {{ image.originalName || image.fileName }}
      </h4>
      
      <!-- Description -->
      <p class="image-description" v-if="image.description">
        <span class="description-text">{{ truncateText(image.description, 100) }}</span>
      </p>
      
      <!-- Tags -->
      <div class="tags-container" v-if="image.tags">
        <div class="tags-list">
          <span 
            v-for="tag in getTagList(image.tags).slice(0, 4)" 
            :key="tag"
            class="tag"
            :class="{ 'tag-matched': isTagMatched(tag) }"
          >
            {{ tag }}
          </span>
          <span v-if="getTagList(image.tags).length > 4" class="tag-more">
            +{{ getTagList(image.tags).length - 4 }}
          </span>
        </div>
      </div>
      
      <!-- Technical Info -->
      <div class="technical-info">
        <div class="info-row">
          <span class="info-label">Format:</span>
          <span class="info-value" :class="{ 'value-matched': isFormatMatched() }">
            {{ image.fileType || 'Unknown' }}
          </span>
        </div>
        
        <div class="info-row">
          <span class="info-label">Size:</span>
          <span class="info-value">{{ formatFileSize(image.fileSize) }}</span>
        </div>
        
        <div class="info-row" v-if="image.width && image.height">
          <span class="info-label">Dimensions:</span>
          <span class="info-value">{{ image.width }}×{{ image.height }}</span>
        </div>
      </div>
      
      <!-- Match Explanation -->
      <div class="match-explanation" v-if="props.result.explanation">
        <Icon name="info" size="sm" />
        <span class="explanation-text">{{ props.result.explanation }}</span>
      </div>
      
      <!-- Confidence Level -->
      <div class="confidence-level" v-if="props.result.confidenceLevel">
        <span :class="['confidence-badge', `confidence-${props.result.confidenceLevel.toLowerCase()}`]">
          {{ formatConfidenceLevel(props.result.confidenceLevel) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import Icon from '@/components/common/Icon.vue'

interface Props {
  result: any
  searchQuery?: string
  metadataMatches?: Array<{
    type: string
    explanation: string
    score: number
  }>
}

const props = withDefaults(defineProps<Props>(), {
  searchQuery: ''
})

// Extract image data from result - handle different response structures
const image = computed(() => {
  // If result has an image property, use it
  if (props.result.image) {
    return props.result.image
  }
  // If result itself is the image data, use it directly
  if (props.result.thumbnailUrl || props.result.cosUrl) {
    return props.result
  }
  // Fallback to empty object to prevent errors
  return {
    thumbnailUrl: '',
    cosUrl: '',
    originalName: 'Unknown',
    description: '',
    tags: '',
    fileType: '',
    fileSize: 0,
    width: 0,
    height: 0
  }
})

const emit = defineEmits<{
  click: [image: any]
}>()

const truncateText = (text: string, maxLength: number): string => {
  if (!text || text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

const getTagList = (tags: string): string[] => {
  if (!tags) return []
  return tags.split(/[,;]/).map(tag => tag.trim()).filter(tag => tag.length > 0)
}

const formatFileSize = (size: number): string => {
  if (!size) return 'Unknown'
  
  const units = ['B', 'KB', 'MB', 'GB']
  let fileSize = size
  let unitIndex = 0
  
  while (fileSize >= 1024 && unitIndex < units.length - 1) {
    fileSize /= 1024
    unitIndex++
  }
  
  return `${fileSize.toFixed(1)} ${units[unitIndex]}`
}

const formatConfidenceLevel = (level: string): string => {
  return level.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
}

const getScoreClass = (score: number): string => {
  if (score >= 0.8) return 'score-excellent'
  if (score >= 0.6) return 'score-good'
  if (score >= 0.4) return 'score-fair'
  if (score >= 0.2) return 'score-poor'
  return 'score-very-poor'
}

const isTagMatched = (tag: string): boolean => {
  if (!props.searchQuery) return false
  return tag.toLowerCase().includes(props.searchQuery.toLowerCase()) ||
         props.searchQuery.toLowerCase().includes(tag.toLowerCase())
}

const isFormatMatched = (): boolean => {
  if (!props.searchQuery || !image.value.fileType) return false
  return image.value.fileType.toLowerCase().includes(props.searchQuery.toLowerCase()) ||
         props.searchQuery.toLowerCase().includes(image.value.fileType.toLowerCase())
}

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = '/placeholder-image.svg' // Fallback image
}
</script>

<style scoped>
.search-result-card {
  @apply bg-white rounded-lg shadow-sm border border-gray-200;
  @apply hover:shadow-md hover:border-gray-300 cursor-pointer;
  @apply transition-all duration-200 overflow-hidden;
}

.search-result-card:hover {
  @apply transform -translate-y-1;
}

.image-preview {
  @apply relative aspect-square overflow-hidden bg-gray-100;
}

.preview-image {
  @apply w-full h-full object-cover;
}

.metadata-overlay {
  @apply absolute top-2 left-2;
}

.match-indicators {
  @apply flex flex-wrap gap-1;
}

.match-badge {
  @apply px-2 py-1 text-xs font-medium rounded-full;
  @apply bg-blue-100 text-blue-800 border border-blue-200;
}

.match-filename {
  @apply bg-green-100 text-green-800 border-green-200;
}

.match-description {
  @apply bg-purple-100 text-purple-800 border-purple-200;
}

.match-tags {
  @apply bg-orange-100 text-orange-800 border-orange-200;
}

.match-metadata {
  @apply bg-indigo-100 text-indigo-800 border-indigo-200;
}

.score-indicator {
  @apply absolute bottom-2 right-2 flex items-center gap-2;
  @apply bg-white bg-opacity-90 rounded-full px-2 py-1;
}

.score-bar {
  @apply w-12 h-2 bg-gray-200 rounded-full overflow-hidden;
}

.score-fill {
  @apply h-full transition-all duration-300;
}

.score-excellent {
  @apply bg-green-500;
}

.score-good {
  @apply bg-blue-500;
}

.score-fair {
  @apply bg-yellow-500;
}

.score-poor {
  @apply bg-orange-500;
}

.score-very-poor {
  @apply bg-red-500;
}

.score-text {
  @apply text-xs font-medium text-gray-700;
}

.image-info {
  @apply p-4 space-y-3;
}

.image-title {
  @apply font-semibold text-gray-900 text-sm leading-tight;
  @apply truncate;
}

.image-description {
  @apply text-sm text-gray-600 leading-relaxed;
}

.description-text {
  @apply break-words;
}

.tags-container {
  @apply space-y-2;
}

.tags-list {
  @apply flex flex-wrap gap-1;
}

.tag {
  @apply px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded-md;
  @apply border border-gray-200;
}

.tag-matched {
  @apply bg-yellow-100 text-yellow-800 border-yellow-300;
  @apply font-medium;
}

.tag-more {
  @apply px-2 py-1 text-xs text-gray-500 italic;
}

.technical-info {
  @apply space-y-1 text-xs;
}

.info-row {
  @apply flex justify-between items-center;
}

.info-label {
  @apply text-gray-500 font-medium;
}

.info-value {
  @apply text-gray-700;
}

.value-matched {
  @apply text-blue-600 font-medium;
}

.match-explanation {
  @apply flex items-start gap-2 p-2 bg-blue-50 rounded-md;
  @apply border border-blue-200;
}

.explanation-text {
  @apply text-xs text-blue-700 leading-relaxed;
}

.confidence-level {
  @apply flex justify-center;
}

.confidence-badge {
  @apply px-2 py-1 text-xs font-medium rounded-full;
}

.confidence-very_high {
  @apply bg-green-100 text-green-800;
}

.confidence-high {
  @apply bg-blue-100 text-blue-800;
}

.confidence-medium {
  @apply bg-yellow-100 text-yellow-800;
}

.confidence-low {
  @apply bg-orange-100 text-orange-800;
}

.confidence-very_low {
  @apply bg-red-100 text-red-800;
}

/* Responsive adjustments */
@media (max-width: 640px) {
  .image-info {
    @apply p-3 space-y-2;
  }
  
  .image-title {
    @apply text-xs;
  }
  
  .image-description {
    @apply text-xs;
  }
  
  .technical-info {
    @apply text-xs;
  }
}
</style>