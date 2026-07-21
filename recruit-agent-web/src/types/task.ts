export type AgentTaskType = 'RESUME_PARSE' | 'SCREENING' | 'REPLY_GENERATION' | 'BATCH_SCREENING'
export type AgentTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

export interface AgentTask {
  id: number
  taskType: AgentTaskType
  status: AgentTaskStatus
  targetId: number | null
  totalCount: number
  successCount: number
  failureCount: number
  createdAt: string
  updatedAt: string
}

export interface AgentStep {
  id: number
  taskId: number
  stepName: string
  status: AgentTaskStatus
  message: string
  createdAt: string
}
