import request from './request'

export const vectorApi = {
  indexJobDescription(jobId: number, content: string) {
    return request.post(`/vector/jobs/${jobId}/description`, { content })
  },
  indexResumeProject(candidateId: number, projectId: number, content: string) {
    return request.post(`/vector/candidates/${candidateId}/projects`, { projectId, content })
  },
  findSimilarity(jobId: number, candidateId: number) {
    return request.get<{ candidateId: number; content: string; similarity: number }>(
      `/vector/jobs/${jobId}/candidates/${candidateId}/similarity`,
    )
  },
}
