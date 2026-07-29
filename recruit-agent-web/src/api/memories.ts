import request from './request'
import type { CandidateMemory, CreateCandidateMemoryRequest } from '@/types/memory'

export const memoryApi = {
  list(candidateId: number, includeInactive = false) {
    return request.get<CandidateMemory[]>(`/candidates/${candidateId}/memories`, {
      params: { includeInactive },
    })
  },
  create(candidateId: number, data: CreateCandidateMemoryRequest) {
    return request.post<CandidateMemory>(`/candidates/${candidateId}/memories`, data)
  },
  deactivate(candidateId: number, memoryId: number) {
    return request.post<CandidateMemory>(`/candidates/${candidateId}/memories/${memoryId}/deactivate`)
  },
}
