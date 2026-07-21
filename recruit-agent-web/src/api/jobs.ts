import request from './request'
import type { PageResponse } from '@/types/api'
import type { Job, CreateJobRequest } from '@/types/job'

export const jobApi = {
  list(params?: { keyword?: string; status?: string; page?: number; size?: number }) {
    return request.get<Job[] | PageResponse<Job>>('/jobs', { params })
  },
  get(id: number) {
    return request.get<Job>(`/jobs/${id}`)
  },
  create(data: CreateJobRequest) {
    return request.post<Job>('/jobs', data)
  },
  update(id: number, data: CreateJobRequest) {
    return request.put<Job>(`/jobs/${id}`, data)
  },
  analyze(id: number) {
    return request.post<Job>(`/jobs/${id}/analyze`)
  },
  close(id: number) {
    return request.patch<Job>(`/jobs/${id}/close`)
  },
  delete(id: number) {
    return request.delete(`/jobs/${id}`)
  },
}
