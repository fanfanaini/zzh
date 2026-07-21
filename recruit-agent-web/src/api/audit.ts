import request from './request'

export interface AuditLog {
  id: number
  userId: number | null
  username: string | null
  method: string
  path: string
  statusCode: number
  durationMillis: number
  clientIp: string | null
  createdAt: string
}

export const auditApi = {
  list(limit = 50) {
    return request.get<AuditLog[]>('/audit/logs', { params: { limit } })
  },
}
