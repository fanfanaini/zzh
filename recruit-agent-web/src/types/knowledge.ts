export interface KnowledgeDocument {
  documentId: number
  sourceName: string
  chunkCount: number
  createdAt: string
}

export interface KnowledgeMatch {
  documentId: number
  chunkId: number
  sourceName: string
  content: string
  score: number
}

export interface IndexedKnowledgeDocument {
  documentId: number
  sourceName: string
  chunkCount: number
}
