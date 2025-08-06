<template>
  <div class="space-y-2">
    <label class="block text-sm font-medium text-gray-700">
      {{ label }}
    </label>
    
    <!-- Tag display -->
    <div v-if="tags.length > 0" class="flex flex-wrap gap-2 mb-2">
      <span
        v-for="(tag, index) in tags"
        :key="index"
        class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
      >
        {{ tag }}
        <button
          @click="removeTag(index)"
          class="ml-1 text-blue-600 hover:text-blue-800"
        >
          ×
        </button>
      </span>
    </div>
    
    <!-- Input field -->
    <div class="relative">
      <input
        v-model="inputValue"
        type="text"
        :placeholder="placeholder"
        class="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        @keydown.enter.prevent="addTag"
        @keydown.comma.prevent="addTag"
        @keydown.space.prevent="addTag"
        @keydown.backspace="handleBackspace"
      />
      <div class="absolute inset-y-0 right-0 flex items-center pr-3">
        <button
          v-if="inputValue.trim()"
          @click="addTag"
          class="text-blue-600 hover:text-blue-800 text-sm font-medium"
        >
          Add
        </button>
      </div>
    </div>
    
    <!-- Help text -->
    <p class="text-xs text-gray-500">
      Press Enter, Space, or Comma to add tags
    </p>
    
    <!-- Suggested tags -->
    <div v-if="suggestedTags.length > 0" class="space-y-2">
      <label class="text-xs font-medium text-gray-600">Suggested tags:</label>
      <div class="flex flex-wrap gap-1">
        <button
          v-for="tag in suggestedTags"
          :key="tag"
          @click="addSuggestedTag(tag)"
          class="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors"
        >
          + {{ tag }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  modelValue?: string[]
  label?: string
  placeholder?: string
  suggestedTags?: string[]
}

interface Emits {
  (e: 'update:modelValue', value: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  label: 'Tags',
  placeholder: 'Add tags...',
  suggestedTags: () => []
})

const emit = defineEmits<Emits>()

const inputValue = ref('')

const tags = computed({
  get: () => props.modelValue || [],
  set: (value) => emit('update:modelValue', value)
})

const addTag = () => {
  const tag = inputValue.value.trim()
  if (tag && !tags.value.includes(tag)) {
    tags.value = [...tags.value, tag]
    inputValue.value = ''
  }
}

const addSuggestedTag = (tag: string) => {
  if (!tags.value.includes(tag)) {
    tags.value = [...tags.value, tag]
  }
}

const removeTag = (index: number) => {
  tags.value = tags.value.filter((_, i) => i !== index)
}

const handleBackspace = () => {
  if (!inputValue.value && tags.value.length > 0) {
    removeTag(tags.value.length - 1)
  }
}
</script>
