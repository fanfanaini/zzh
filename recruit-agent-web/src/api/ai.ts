import request from './request'

export interface ModelCallLog {
  id: number
  useCase: string
  modelName: string
  promptVersion: string | null
  systemPrompt: string | null
  userPrompt: string | null
  responseBody: string | null
  latencyMillis: number | null
  promptTokens: number | null
  completionTokens: number | null
  status: 'SUCCEEDED' | 'FAILED'
  errorMessage: string | null
  createdAt: string
}

export interface ModelCallSummary {
  totalCalls: number
  succeededCalls: number
  failedCalls: number
  promptTokens: number
  completionTokens: number
  averageLatencyMillis: number
}

export interface PromptVersion {
  id: number
  templateCode: string
  version: string
  systemPrompt: string
  userPromptTemplate: string
  enabled: boolean
  createdAt: string
}

export interface CreatePromptVersionRequest {
  version: string
  systemPrompt: string
  userPromptTemplate: string
  enabled: boolean
}

export const modelCallApi = {
  list() {
    return request.get<ModelCallLog[]>('/model-calls')
  },
  summary() {
    return request.get<ModelCallSummary>('/model-calls/summary')
  },
}

export const promptApi = {
  list(code: string) {
    return request.get<PromptVersion[]>(`/prompts/${code}/versions`)
  },
  active(code: string) {
    return request.get<PromptVersion>(`/prompts/${code}/active`)
  },
  create(code: string, data: CreatePromptVersionRequest) {
    return request.post<PromptVersion>(`/prompts/${code}/versions`, data)
  },
}
