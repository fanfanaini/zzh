export type HealthStatus = 'HEALTHY' | 'DEGRADED' | 'CRITICAL'

export type AlertSeverity = 'INFO' | 'WARN' | 'CRITICAL'

export interface AgentOpsDashboard {
  generatedAt: string
  healthStatus: HealthStatus
  model: ModelOps
  traces: TraceOps
  traceComponents: Record<string, number>
  modelUseCases: Record<string, number>
  activeAlerts: AgentOpsAlert[]
}

export interface ModelOps {
  totalCalls: number
  succeededCalls: number
  failedCalls: number
  failureRate: number
  averageLatencyMillis: number
  promptTokens: number
  completionTokens: number
  latestCallAt: string
}

export interface TraceOps {
  totalEvents: number
  failedEvents: number
  failureRate: number
  toolEvents: number
  retrievalEvents: number
  modelEvents: number
  memoryEvents: number
  skillEvents: number
}

export interface AgentOpsAlert {
  code: string
  severity: AlertSeverity
  title: string
  message: string
  details: Record<string, unknown>
  triggeredAt: string
}
