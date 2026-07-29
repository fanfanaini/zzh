export type CandidateMemoryType = 'PREFERENCE' | 'STATUS' | 'RISK' | 'NOTE' | 'FACT'

export type CandidateMemorySourceType = 'CONVERSATION_MESSAGE' | 'REPLY_APPROVAL' | 'MANUAL' | 'RESUME' | 'SYSTEM'

export interface CandidateMemory {
  id: number
  candidateId: number
  type: CandidateMemoryType
  factKey: string
  factValue: string
  confidence: number
  sourceType: CandidateMemorySourceType
  sourceId: number | null
  evidence: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateCandidateMemoryRequest {
  type: CandidateMemoryType
  factKey: string
  factValue: string
  confidence?: number
  evidence?: string
}
