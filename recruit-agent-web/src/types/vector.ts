export interface EmbeddingVersionResponse {
  versionId: string
  modelName: string
  dimensions: number
  status: string
  active: boolean
  createdAt: string
  activatedAt: string | null
  notes: string | null
}

export interface EmbeddingRebuildJobResponse {
  jobId: string
  targetVersion: string
  status: string
  totalChunks: number
  succeededChunks: number
  failedChunks: number
  startedAt: string
  finishedAt: string | null
  errorMessage: string | null
}
