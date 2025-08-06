// API Configuration
export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  TIMEOUT: 30000,
  MAX_FILE_SIZE: parseInt(import.meta.env.VITE_MAX_FILE_SIZE) || 10 * 1024 * 1024, // 10MB
  ALLOWED_FILE_TYPES: (import.meta.env.VITE_ALLOWED_FILE_TYPES || 'image/jpeg,image/png,image/gif,image/webp').split(',')
}

// Application Configuration
export const APP_CONFIG = {
  NAME: import.meta.env.VITE_APP_NAME || 'Uka Image Hosting System',
  VERSION: import.meta.env.VITE_APP_VERSION || '1.0.0'
}

// Feature Flags
export const FEATURES = {
  AI_SEARCH: import.meta.env.VITE_ENABLE_AI_SEARCH === 'true',
  BATCH_UPLOAD: import.meta.env.VITE_ENABLE_BATCH_UPLOAD === 'true',
  ALBUMS: import.meta.env.VITE_ENABLE_ALBUMS === 'true'
}

// UI Constants
export const UI_CONSTANTS = {
  PAGINATION: {
    DEFAULT_PAGE_SIZE: 20,
    PAGE_SIZE_OPTIONS: [10, 20, 50, 100]
  },
  SEARCH: {
    MIN_QUERY_LENGTH: 2,
    DEBOUNCE_DELAY: 300,
    MAX_SUGGESTIONS: 10
  },
  UPLOAD: {
    MAX_FILES_PER_BATCH: 50,
    SUPPORTED_FORMATS: ['JPEG', 'PNG', 'GIF', 'WebP'],
    THUMBNAIL_SIZE: 200
  }
}

// Search Types
export const SEARCH_TYPES = {
  SEMANTIC: 'semantic',
  VISUAL: 'visual',
  COLOR: 'color',
  SCENE: 'scene'
} as const

// Popular Colors for Color Search
export const POPULAR_COLORS = [
  '#ef4444', '#f97316', '#f59e0b', '#eab308',
  '#84cc16', '#22c55e', '#10b981', '#14b8a6',
  '#06b6d4', '#0ea5e9', '#3b82f6', '#6366f1',
  '#8b5cf6', '#a855f7', '#d946ef', '#ec4899',
  '#f43f5e', '#64748b', '#374151', '#000000'
]

// Popular Scene Categories
export const POPULAR_SCENES = [
  'nature', 'landscape', 'portrait', 'city', 'beach', 'mountain',
  'forest', 'sunset', 'architecture', 'food', 'animals', 'flowers',
  'street', 'indoor', 'outdoor', 'night', 'day', 'abstract'
]

// File Size Formatting
export const FILE_SIZE_UNITS = ['Bytes', 'KB', 'MB', 'GB', 'TB']

// Date Formatting Options
export const DATE_FORMAT_OPTIONS: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit'
}

// Error Messages
export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error. Please check your connection.',
  FILE_TOO_LARGE: 'File size exceeds the maximum limit.',
  INVALID_FILE_TYPE: 'Invalid file type. Please select an image file.',
  UPLOAD_FAILED: 'Upload failed. Please try again.',
  SEARCH_FAILED: 'Search failed. Please try again.',
  LOAD_FAILED: 'Failed to load data. Please refresh the page.',
  DELETE_FAILED: 'Failed to delete item. Please try again.',
  UPDATE_FAILED: 'Failed to update item. Please try again.'
}

// Success Messages
export const SUCCESS_MESSAGES = {
  UPLOAD_SUCCESS: 'Images uploaded successfully!',
  DELETE_SUCCESS: 'Item deleted successfully!',
  UPDATE_SUCCESS: 'Item updated successfully!',
  SAVE_SUCCESS: 'Changes saved successfully!'
}