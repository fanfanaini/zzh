export type TraceComponent = 'AGENT' | 'RETRIEVAL' | 'TOOL' | 'MODEL' | 'MEMORY' | 'SKILL'

export type AgentTraceStatus = 'STARTED' | 'SUCCEEDED' | 'FAILED'

export interface AgentTraceEvent {
  traceId: string
  conversationId: number | null
  node: string
  component: TraceComponent | null
  operation: string | null
  spanId: string | null
  parentSpanId: string | null
  status: AgentTraceStatus
  details: Record<string, unknown>
  occurredAt: string
}

export interface TraceFilter {
  conversationId?: number
  traceId?: string
  component?: TraceComponent
  status?: AgentTraceStatus
}
