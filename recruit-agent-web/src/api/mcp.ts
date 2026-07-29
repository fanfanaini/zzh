import request from './request'
import type { McpToolDescriptor, McpToolCallRequest, McpToolCallResponse } from '@/types/mcp'

export const mcpApi = {
  listTools() {
    return request.get<McpToolDescriptor[]>('/mcp/tools')
  },
  callTool(name: string, data?: McpToolCallRequest) {
    return request.post<McpToolCallResponse>(`/mcp/tools/${name}/call`, data)
  },
}
