export type JobApplicationStage =
  | 'APPLIED'
  | 'SCREENING'
  | 'HR_REVIEW'
  | 'COMMUNICATION'
  | 'INTERVIEW'
  | 'OFFER'
  | 'HIRED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'CLOSED'

export interface JobApplicationResponse {
  id: number
  jobId: number
  candidateId: number
  stage: JobApplicationStage
  ownerUserId: number | null
  source: string | null
  latestEvaluationId: number | null
  latestConversationId: number | null
  rejectionReason: string | null
  note: string | null
  createdAt: string
  updatedAt: string
  closedAt: string | null
}

export interface CreateJobApplicationRequest {
  jobId: number
  candidateId: number
  ownerUserId?: number | null
  source?: string | null
  note?: string | null
}

export interface AdvanceJobApplicationRequest {
  stage: JobApplicationStage
  operatorUserId?: number | null
  reason?: string | null
  evaluationId?: number | null
  conversationId?: number | null
  note?: string | null
}

export interface JobApplicationHistoryResponse {
  id: number
  applicationId: number
  fromStage: JobApplicationStage | null
  toStage: JobApplicationStage
  operatorUserId: number | null
  reason: string | null
  createdAt: string
}
