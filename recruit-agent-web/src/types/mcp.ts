export interface McpToolDescriptor {
  name: string
  description: string
  inputSchema: Record<string, unknown>
  requiredPermissions: string[]
  idempotent: boolean
}

export interface McpToolCallRequest {
  arguments?: Record<string, unknown>
  idempotencyKey?: string
  conversationId?: number
}

export interface McpToolCallResponse {
  traceId: string
  success: boolean
  data: unknown
  error: ToolError | null
  attempts: number
  idempotencyReplay: boolean
}

export interface ToolError {
  code: string
  message: string
  details: unknown
}
