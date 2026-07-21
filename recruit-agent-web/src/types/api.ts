// ── 通用 API 类型 ──

export interface ApiResponse<T = unknown> {
  message?: string
  data?: T
}

// ── 分页 ──
export interface PageRequest {
  page?: number
  size?: number
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}
