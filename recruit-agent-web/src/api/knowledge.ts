import request from './request'
import type { IndexedKnowledgeDocument, KnowledgeDocument, KnowledgeMatch } from '@/types/knowledge'

export const knowledgeApi = {
  listDocuments: () => request.get<KnowledgeDocument[]>('/knowledge/documents'),
  indexText: (sourceName: string, content: string) =>
    request.post<IndexedKnowledgeDocument>('/knowledge/documents', { sourceName, content }),
  upload(file: File) {
    const form = new FormData()
    form.append('file', file)
    return request.post<IndexedKnowledgeDocument>('/knowledge/documents/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  remove: (documentId: number) => request.delete(`/knowledge/documents/${documentId}`),
  search: (query: string, limit = 5) =>
    request.get<KnowledgeMatch[]>('/knowledge/search', { params: { query, limit } }),
}
