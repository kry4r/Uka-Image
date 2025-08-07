<template>
  <div class="json-viewer">
    <div class="json-content">
      <JsonNode 
        :data="data" 
        :expanded="expanded" 
        :depth="0"
        :path="'root'"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import JsonNode from './JsonNode.vue'

interface Props {
  data: any
  expanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  expanded: false
})
</script>

<style scoped>
.json-viewer {
  @apply bg-gray-50 border border-gray-200 rounded-lg overflow-hidden;
}

.json-content {
  @apply p-3 max-h-96 overflow-auto;
}
</style>