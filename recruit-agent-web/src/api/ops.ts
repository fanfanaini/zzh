import request from './request'
import type { AgentOpsDashboard, AgentOpsAlert } from '@/types/ops'

export const opsApi = {
  dashboard() {
    return request.get<AgentOpsDashboard>('/ops/dashboard')
  },
  alerts() {
    return request.get<AgentOpsAlert[]>('/ops/alerts')
  },
}
