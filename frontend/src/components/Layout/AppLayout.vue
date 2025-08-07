<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Navigation Header -->
    <nav class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between h-16">
          <div class="flex items-center">
            <router-link to="/" class="flex items-center space-x-2">
              <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                <span class="text-white font-bold text-lg">U</span>
              </div>
              <span class="text-xl font-semibold text-gray-900">Uka Image</span>
            </router-link>
          </div>
          
          <div class="flex items-center space-x-4">
            <router-link
              v-for="item in navigationItems"
              :key="item.name"
              :to="item.path"
              class="px-3 py-2 rounded-md text-sm font-medium transition-colors duration-200"
              :class="isActiveRoute(item.path) 
                ? 'bg-blue-100 text-blue-700' 
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'"
            >
              {{ item.name }}
            </router-link>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const navigationItems = [
  { name: '图库', path: '/gallery' },
  { name: '上传', path: '/upload' },
]

const isActiveRoute = (path: string) => {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>
