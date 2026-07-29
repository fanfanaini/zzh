import request from './request'
import type { PageResponse } from '@/types/api'
import type {
  JobApplicationResponse,
  CreateJobApplicationRequest,
  AdvanceJobApplicationRequest,
  JobApplicationHistoryResponse,
  JobApplicationStage,
} from '@/types/job-application'

export const jobApplicationApi = {
  create(data: CreateJobApplicationRequest) {
    return request.post<JobApplicationResponse>('/job-applications', data)
  },

  get(id: number) {
    return request.get<JobApplicationResponse>(`/job-applications/${id}`)
  },

  list(params?: {
    jobId?: number
    candidateId?: number
    stage?: JobApplicationStage
    page?: number
    size?: number
  }) {
    return request.get<JobApplicationResponse[] | PageResponse<JobApplicationResponse>>('/job-applications', {
      params,
    })
  },

  advance(id: number, data: AdvanceJobApplicationRequest) {
    return request.patch<JobApplicationResponse>(`/job-applications/${id}/stage`, data)
  },

  history(id: number) {
    return request.get<JobApplicationHistoryResponse[]>(`/job-applications/${id}/history`)
  },
}
