import request from './request'
import type { AgentTraceEvent, TraceFilter } from '@/types/trace'

export const traceApi = {
  find(params: TraceFilter) {
    return request.get<AgentTraceEvent[]>('/agent-traces', { params })
  },
}
