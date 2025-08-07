<template>
  <svg 
    :class="['icon', sizeClass]" 
    :viewBox="viewBox" 
    fill="none" 
    stroke="currentColor" 
    stroke-width="2" 
    stroke-linecap="round" 
    stroke-linejoin="round"
  >
    <!-- Chevron Down -->
    <path v-if="name === 'chevron-down'" d="m6 9 6 6 6-6" />
    
    <!-- Chevron Right -->
    <path v-if="name === 'chevron-right'" d="m9 18 6-6-6-6" />
    
    <!-- Chevron Up -->
    <path v-if="name === 'chevron-up'" d="m18 15-6-6-6 6" />
    
    <!-- Alert Circle -->
    <g v-if="name === 'alert-circle'">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </g>
    
    <!-- Alert Triangle -->
    <g v-if="name === 'alert-triangle'">
      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
      <line x1="12" y1="9" x2="12" y2="13" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </g>
    
    <!-- Info -->
    <g v-if="name === 'info'">
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="16" x2="12" y2="12" />
      <line x1="12" y1="8" x2="12.01" y2="8" />
    </g>
    
    <!-- Copy -->
    <g v-if="name === 'copy'">
      <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
      <path d="M4 16c-1.1 0-2-.9-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
    </g>
    
    <!-- Terminal -->
    <g v-if="name === 'terminal'">
      <polyline points="4,17 10,11 4,5" />
      <line x1="12" y1="19" x2="20" y2="19" />
    </g>
    
    <!-- Download -->
    <g v-if="name === 'download'">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <polyline points="7,10 12,15 17,10" />
      <line x1="12" y1="15" x2="12" y2="3" />
    </g>
    
    <!-- Default fallback -->
    <circle v-if="!['chevron-down', 'chevron-right', 'chevron-up', 'alert-circle', 'alert-triangle', 'info', 'copy', 'terminal', 'download'].includes(name)" cx="12" cy="12" r="10" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  name: string
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md'
})

const sizeClass = computed(() => {
  switch (props.size) {
    case 'sm': return 'w-4 h-4'
    case 'lg': return 'w-6 h-6'
    default: return 'w-5 h-5'
  }
})

const viewBox = '0 0 24 24'
</script>

<style scoped>
.icon {
  @apply inline-block flex-shrink-0;
}
</style>