<template>
  <AppLayout>
    <div class="text-center">
      <!-- Hero Section -->
      <div class="max-w-4xl mx-auto">
        <h1 class="text-4xl font-bold text-gray-900 sm:text-5xl md:text-6xl">
          Welcome to
          <span class="text-blue-600">Uka Image</span>
        </h1>
        <p class="mt-3 max-w-md mx-auto text-base text-gray-500 sm:text-lg md:mt-5 md:text-xl md:max-w-3xl">
          A powerful image hosting system with AI-powered search capabilities. Upload, organize, and discover your images like never before.
        </p>
        <div class="mt-5 max-w-md mx-auto sm:flex sm:justify-center md:mt-8">
          <div class="rounded-md shadow">
            <router-link
              to="/upload"
              class="w-full flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 md:py-4 md:text-lg md:px-10 transition-colors duration-200"
            >
              Start Uploading
            </router-link>
          </div>
          <div class="mt-3 rounded-md shadow sm:mt-0 sm:ml-3">
            <router-link
              to="/gallery"
              class="w-full flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-md text-blue-600 bg-white hover:bg-gray-50 md:py-4 md:text-lg md:px-10 transition-colors duration-200"
            >
              Browse Gallery
            </router-link>
          </div>
        </div>
      </div>

      <!-- Features Section -->
      <div class="mt-16">
        <div class="max-w-7xl mx-auto">
          <div class="text-center">
            <h2 class="text-3xl font-extrabold text-gray-900 sm:text-4xl">
              Powerful Features
            </h2>
            <p class="mt-4 max-w-2xl mx-auto text-xl text-gray-500">
              Everything you need to manage and discover your images
            </p>
          </div>

          <div class="mt-10">
            <div class="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-3">
              <div v-for="feature in features" :key="feature.name" class="text-center">
                <div class="flex items-center justify-center h-12 w-12 rounded-md bg-blue-500 text-white mx-auto">
                  <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="feature.iconPath" />
                  </svg>
                </div>
                <div class="mt-5">
                  <h3 class="text-lg font-medium text-gray-900">{{ feature.name }}</h3>
                  <p class="mt-2 text-base text-gray-500">{{ feature.description }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Getting Started -->
      <div class="mt-16">
        <div class="text-center">
          <h2 class="text-3xl font-extrabold text-gray-900 sm:text-4xl">
            Get Started
          </h2>
          <p class="mt-4 max-w-2xl mx-auto text-xl text-gray-500">
            Start using Uka Image in just a few clicks
          </p>
        </div>
        
        <div class="mt-10 grid grid-cols-1 gap-8 sm:grid-cols-3">
          <div class="text-center">
            <div class="flex items-center justify-center h-16 w-16 rounded-full bg-blue-100 text-blue-600 mx-auto mb-4">
              <span class="text-2xl font-bold">1</span>
            </div>
            <h3 class="text-lg font-medium text-gray-900">Upload Images</h3>
            <p class="mt-2 text-gray-500">Drag and drop your images or click to browse</p>
          </div>
          
          <div class="text-center">
            <div class="flex items-center justify-center h-16 w-16 rounded-full bg-blue-100 text-blue-600 mx-auto mb-4">
              <span class="text-2xl font-bold">2</span>
            </div>
            <h3 class="text-lg font-medium text-gray-900">Organize</h3>
          </div>
          
          <div class="text-center">
            <div class="flex items-center justify-center h-16 w-16 rounded-full bg-blue-100 text-blue-600 mx-auto mb-4">
              <span class="text-2xl font-bold">3</span>
            </div>
            <h3 class="text-lg font-medium text-gray-900">Search & Discover</h3>
            <p class="mt-2 text-gray-500">Use AI-powered search to find your images instantly</p>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup lang="ts">
import AppLayout from '@/components/Layout/AppLayout.vue'

const features = [
  {
    name: 'Smart Upload',
    description: 'Drag and drop multiple images with automatic metadata extraction',
    iconPath: 'M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12'
  },
  {
    name: 'AI Search',
    description: 'Find images using natural language, colors, or visual similarity',
    iconPath: 'm21 21-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z'
  },
  {
    name: 'Organization',
    iconPath: 'M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z'
  },
  {
    name: 'Cloud Storage',
    description: 'Secure cloud storage with CDN delivery for fast access',
    iconPath: 'M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 12l2 2 4-4'
  },
  {
    name: 'Responsive Design',
    description: 'Works perfectly on desktop, tablet, and mobile devices',
    iconPath: 'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z'
  },
  {
    name: 'Batch Operations',
    description: 'Upload multiple images and perform batch operations efficiently',
    iconPath: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10'
  }
]
</script>
