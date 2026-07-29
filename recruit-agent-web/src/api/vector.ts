import request from './request'
import type { EmbeddingVersionResponse, EmbeddingRebuildJobResponse } from '@/types/vector'

export const vectorApi = {
  indexJobDescription(jobId: number, content: string) {
    return request.post(`/vector/jobs/${jobId}/description`, { content })
  },
  indexResumeProject(candidateId: number, projectId: number, content: string) {
    return request.post(`/vector/candidates/${candidateId}/projects`, { projectId, content })
  },
  findSimilarity(jobId: number, candidateId: number) {
    return request.get<{ candidateId: number; content: string; score: number }>(
      `/vector/jobs/${jobId}/candidates/${candidateId}/similarity`,
    )
  },
  // Embedding version governance
  embeddingVersions() {
    return request.get<EmbeddingVersionResponse[]>('/vector/embedding-versions')
  },
  registerEmbeddingVersion(data: { versionId: string; modelName: string; dimensions: number; notes?: string }) {
    return request.post<EmbeddingVersionResponse>('/vector/embedding-versions', data)
  },
  rebuildEmbeddingVersion(versionId: string) {
    return request.post<EmbeddingRebuildJobResponse>(`/vector/embedding-versions/${versionId}/rebuild`)
  },
  activateEmbeddingVersion(versionId: string) {
    return request.post(`/vector/embedding-versions/${versionId}/activate`)
  },
  rollbackEmbeddingVersion(versionId: string) {
    return request.post(`/vector/embedding-versions/${versionId}/rollback`)
  },
  embeddingRebuildJobs() {
    return request.get<EmbeddingRebuildJobResponse[]>('/vector/embedding-rebuild-jobs')
  },
}
