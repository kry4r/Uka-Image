import { request } from './index'

export interface DatabaseConfig {
  host: string
  port: number
  database: string
  username: string
  password: string
  useSSL: boolean
  serverTimezone: string
  minIdle?: number
  maxActive?: number
  maxWait?: number
  validationQuery?: string
  connectionTimeout?: number
  idleTimeout?: number
  maxLifetime?: number
}

export interface DatabaseStatus {
  connected: boolean
  status: string
  message: string
  database?: string
  url?: string
  driver?: string
  driverVersion?: string
  poolInfo?: string
}

export const databaseConfigApi = {
  /**
   * Get current database configuration
   */
  getConfig(): Promise<ApiResponse<DatabaseConfig>> {
    return request.get('/admin/config/database')
  },

  /**
   * Test database connection
   */
  testConnection(config: DatabaseConfig): Promise<ApiResponse<string>> {
    return request.post('/admin/config/database/test', config)
  },

  /**
   * Update database configuration
   */
  updateConfig(config: DatabaseConfig): Promise<ApiResponse<string>> {
    return request.post('/admin/config/database', config)
  },

  /**
   * Get database connection status
   */
  getStatus(): Promise<ApiResponse<DatabaseStatus>> {
    return request.get('/admin/config/database/status')
  }
}

export interface StorageConfig {
  provider: 'tencent_cos' | 'aws_s3' | 'alibaba_oss'
  region: string
  accessKeyId: string
  accessKeySecret: string
  bucketName: string
  baseUrl: string
  customDomain?: string
  useHttps: boolean
  enableCdn: boolean
}

export const storageConfigApi = {
  /**
   * Get current storage configuration
   */
  getConfig(): Promise<ApiResponse<StorageConfig>> {
    return request.get('/admin/config/storage')
  },

  /**
   * Test storage connection
   */
  testConnection(config: StorageConfig): Promise<ApiResponse<string>> {
    return request.post('/admin/config/storage/test', config)
  },

  /**
   * Update storage configuration
   */
  updateConfig(config: StorageConfig): Promise<ApiResponse<string>> {
    return request.post('/admin/config/storage', config)
  },

  /**
   * Get supported storage providers
   */
  getProviders(): Promise<ApiResponse<Array<{
    key: string
    name: string
    description: string
    regions: Array<{ key: string; name: string }>
  }>>> {
    return request.get('/admin/config/storage/providers')
  }
}