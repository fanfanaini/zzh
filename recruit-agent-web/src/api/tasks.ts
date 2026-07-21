import request from './request'
import type { AgentTask, AgentStep } from '@/types/task'

export const taskApi = {
  list() {
    return request.get<AgentTask[]>('/agent-tasks')
  },
  create(data: { taskType: string; targetId?: number; totalCount?: number }) {
    return request.post<AgentTask>('/agent-tasks', data)
  },
  get(id: number) {
    return request.get<AgentTask>(`/agent-tasks/${id}`)
  },
  listSteps(taskId: number) {
    return request.get<AgentStep[]>(`/agent-tasks/${taskId}/steps`)
  },
  retry(id: number) {
    return request.post<AgentTask>(`/agent-tasks/${id}/retry`)
  },
}
