import request from './request'
import type { PageResponse } from '@/types/api'
import type {
  BatchScreenRequest,
  CandidateRanking,
  Evaluation,
  HumanReviewRequest,
  ScreenCandidateRequest,
  ScreeningResult,
} from '@/types/screening'
import type { AgentTask } from '@/types/task'

export const screeningApi = {
  screen(jobId: number, data: ScreenCandidateRequest) {
    return request.post<ScreeningResult>(`/jobs/${jobId}/screen`, data)
  },
  batchScreen(jobId: number, data: BatchScreenRequest, idempotencyKey?: string) {
    return request.post<AgentTask>(`/jobs/${jobId}/batch-screen`, data, {
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined,
    })
  },
  rankings(jobId: number, params?: {
    candidateId?: number
    keyword?: string
    recommendation?: string
    page?: number
    size?: number
  }) {
    return request.get<CandidateRanking[] | PageResponse<CandidateRanking>>(`/jobs/${jobId}/candidates`, { params })
  },
  getEvaluation(id: number) {
    return request.get<Evaluation>(`/evaluations/${id}`)
  },
  humanReview(id: number, data: HumanReviewRequest) {
    return request.post<Evaluation>(`/evaluations/${id}/human-review`, data)
  },
}
