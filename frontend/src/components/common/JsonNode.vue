<template>
  <div class="json-node" :style="{ marginLeft: `${depth * 16}px` }">
    <!-- Object/Array with children -->
    <div v-if="isExpandable" class="expandable-node">
      <div class="node-header" @click="toggleExpanded">
        <Icon 
          :name="isExpanded ? 'chevron-down' : 'chevron-right'" 
          class="expand-icon"
        />
        <span class="node-key" v-if="showKey">{{ displayKey }}:</span>
        <span class="node-type">{{ getTypeDisplay() }}</span>
        <span class="node-count">{{ getItemCount() }}</span>
      </div>
      
      <div v-if="isExpanded" class="node-children">
        <JsonNode
          v-for="(value, key) in data"
          :key="key"
          :data="value"
          :expanded="childExpanded"
          :depth="depth + 1"
          :path="`${path}.${key}`"
          :node-key="key"
        />
      </div>
    </div>

    <!-- Primitive values -->
    <div v-else class="primitive-node">
      <span class="node-key" v-if="showKey">{{ displayKey }}:</span>
      <span :class="getValueClass()">{{ getDisplayValue() }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Icon from './Icon.vue'

interface Props {
  data: any
  expanded?: boolean
  depth: number
  path: string
  nodeKey?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  expanded: false,
  nodeKey: undefined
})

const isExpanded = ref(props.expanded)

const isExpandable = computed(() => {
  return props.data !== null && 
         typeof props.data === 'object' && 
         (Array.isArray(props.data) || Object.keys(props.data).length > 0)
})

const showKey = computed(() => {
  return props.nodeKey !== undefined && props.path !== 'root'
})

const displayKey = computed(() => {
  if (typeof props.nodeKey === 'string') {
    return `"${props.nodeKey}"`
  }
  return props.nodeKey
})

const childExpanded = computed(() => {
  return props.depth < 2 // Auto-expand first 2 levels
})

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
}

const getTypeDisplay = () => {
  if (Array.isArray(props.data)) {
    return '['
  }
  return '{'
}

const getItemCount = () => {
  if (Array.isArray(props.data)) {
    return `${props.data.length} items]`
  }
  const keys = Object.keys(props.data)
  return `${keys.length} keys}`
}

const getDisplayValue = () => {
  if (props.data === null) {
    return 'null'
  }
  
  if (typeof props.data === 'string') {
    return `"${props.data}"`
  }
  
  if (typeof props.data === 'boolean') {
    return props.data.toString()
  }
  
  if (typeof props.data === 'number') {
    return props.data.toString()
  }
  
  if (typeof props.data === 'undefined') {
    return 'undefined'
  }
  
  return String(props.data)
}

const getValueClass = () => {
  const baseClass = 'node-value'
  
  if (props.data === null) {
    return `${baseClass} value-null`
  }
  
  if (typeof props.data === 'string') {
    return `${baseClass} value-string`
  }
  
  if (typeof props.data === 'boolean') {
    return `${baseClass} value-boolean`
  }
  
  if (typeof props.data === 'number') {
    return `${baseClass} value-number`
  }
  
  if (typeof props.data === 'undefined') {
    return `${baseClass} value-undefined`
  }
  
  return baseClass
}
</script>

<style scoped>
.json-node {
  @apply font-mono text-sm;
}

.expandable-node {
  @apply space-y-1;
}

.node-header {
  @apply flex items-center gap-1 cursor-pointer hover:bg-gray-100 rounded px-1 py-0.5;
  @apply transition-colors duration-150;
}

.expand-icon {
  @apply w-4 h-4 text-gray-400 flex-shrink-0;
}

.node-key {
  @apply text-blue-600 font-medium;
}

.node-type {
  @apply text-gray-600;
}

.node-count {
  @apply text-gray-400 text-xs ml-1;
}

.node-children {
  @apply space-y-0.5;
}

.primitive-node {
  @apply flex items-center gap-2 px-1 py-0.5;
}

.node-value {
  @apply break-all;
}

.value-string {
  @apply text-green-600;
}

.value-number {
  @apply text-purple-600;
}

.value-boolean {
  @apply text-orange-600;
}

.value-null {
  @apply text-gray-400 italic;
}

.value-undefined {
  @apply text-gray-400 italic;
}

/* Hover effects */
.primitive-node:hover {
  @apply bg-gray-50 rounded;
}

/* Selection highlighting */
.node-key:hover {
  @apply text-blue-700;
}

.value-string:hover {
  @apply text-green-700;
}

.value-number:hover {
  @apply text-purple-700;
}

.value-boolean:hover {
  @apply text-orange-700;
}
</style>