import request from './request'
import type { PageResponse } from '@/types/api'
import type { Candidate, RegisterCandidateRequest, ResumeDetail, ResumeDocument, ResumeStructuredDetail } from '@/types/candidate'
import type { AgentTask } from '@/types/task'

export const candidateApi = {
  list(params?: { keyword?: string; status?: string; page?: number; size?: number }) {
    return request.get<Candidate[] | PageResponse<Candidate>>('/candidates', { params })
  },
  get(id: number) {
    return request.get<Candidate>(`/candidates/${id}`)
  },
  register(data: RegisterCandidateRequest) {
    return request.post<Candidate>('/candidates', data)
  },
  update(id: number, data: RegisterCandidateRequest) {
    return request.put<Candidate>(`/candidates/${id}`, data)
  },
  disable(id: number) {
    return request.patch<Candidate>(`/candidates/${id}/disable`)
  },
  delete(id: number) {
    return request.delete(`/candidates/${id}`)
  },
  listResumes(candidateId: number) {
    return request.get<ResumeDocument[]>(`/candidates/${candidateId}/resumes`)
  },
  uploadResume(candidateId: number, file: File) {
    const form = new FormData()
    form.append('file', file)
    return request.post<ResumeDocument>('/resumes/upload', form, {
      params: { candidateId },
    })
  },
  batchUploadResumes(candidateId: number, files: File[]) {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    return request.post<ResumeDocument[]>('/resumes/batch-upload', form, {
      params: { candidateId },
    })
  },
}

export const resumeApi = {
  get(resumeId: number) {
    return request.get<ResumeDocument>(`/resumes/${resumeId}`)
  },
  detail(resumeId: number) {
    return request.get<ResumeDetail>(`/resumes/${resumeId}/detail`)
  },
  updateStructuredDetail(resumeId: number, data: ResumeStructuredDetail) {
    return request.put<ResumeDetail>(`/resumes/${resumeId}/structured-detail`, data)
  },
  extractText(resumeId: number) {
    return request.post<ResumeDocument>(`/resumes/${resumeId}/extract-text`)
  },
  extractProfile(resumeId: number) {
    return request.post<Candidate>(`/resumes/${resumeId}/extract-profile`)
  },
  parse(resumeId: number) {
    return request.post<Candidate>(`/resumes/${resumeId}/parse`)
  },
  submitParseTask(resumeId: number) {
    return request.post<AgentTask>(`/resumes/${resumeId}/parse-task`)
  },
  download(resumeId: number) {
    return request.get<Blob>(`/resumes/${resumeId}/download`, { responseType: 'blob' })
  },
  delete(resumeId: number) {
    return request.delete(`/resumes/${resumeId}`)
  },
}
