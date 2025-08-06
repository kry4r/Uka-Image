import api from './index'

export interface Image {
  id: number
  userId: number
  originalName: string
  fileName: string
  filePath: string
  cosUrl: string
  thumbnailUrl?: string
  fileSize: number
  fileType: string
  width?: number
  height?: number
  description?: string
  tags?: string
  uploadIp?: string
  downloadCount: number
  viewCount: number
  status: number
  createdAt: string
  updatedAt: string
  deleted: number
  uploaderUsername?: string
  searchMetadata?: any
}

export interface ImageUploadRequest {
  file: File
  description?: string
  tags?: string
}

export interface PaginatedResponse<T> {
  records: T[]
  total: number
  pages: number
  size: number
  current: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export const imageApi = {
  // Upload single image
  uploadImage: (data: FormData) => {
    return api.post<any>('/images/upload', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  // Batch upload images
  batchUpload: (data: FormData) => {
    return api.post<any>('/images/batch-upload', data, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  // Get image list with pagination
  getImages: (page: number = 0, size: number = 20) => {
    return api.get<PaginatedResponse<Image>>(`/images/list?pageNum=${page + 1}&pageSize=${size}`)
  },

  // Get image by ID
  getImageById: (id: number) => {
    return api.get<Image>(`/images/${id}`)
  },

  // Update image description
  updateDescription: (id: number, description: string) => {
    return api.put<any>(`/images/${id}/description`, { description })
  },

  // Delete image
  deleteImage: (id: number) => {
    return api.delete<any>(`/images/${id}`)
  },

  // Search images
  searchImages: (keyword: string, page: number = 0, size: number = 20) => {
    return api.get<ApiResponse<PaginatedResponse<Image>>>(`/images/search?keyword=${encodeURIComponent(keyword)}&pageNum=${page + 1}&pageSize=${size}`)
  },

  // Download image (increment download count)
  downloadImage: (id: number) => {
    return api.post<ApiResponse<string>>(`/images/${id}/download`)
  },

  // Get image metadata
  getImageMetadata: (id: number) => {
    return api.get<any>(`/images/${id}/metadata`)
  }
}