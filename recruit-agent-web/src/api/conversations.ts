import request from './request'
import type {
  Conversation,
  ConversationMessage,
  ReplySuggestion,
  CreateConversationRequest,
  AddMessageRequest,
  ConversationStage,
} from '@/types/conversation'

export const conversationApi = {
  list(params?: { jobId?: number; candidateId?: number; limit?: number }) {
    return request.get<Conversation[]>('/conversations', { params })
  },
  create(data: CreateConversationRequest) {
    return request.post<Conversation>('/conversations', data)
  },
  get(id: number) {
    return request.get<Conversation>(`/conversations/${id}`)
  },
  listMessages(conversationId: number) {
    return request.get<ConversationMessage[]>(`/conversations/${conversationId}/messages`)
  },
  addMessage(conversationId: number, data: AddMessageRequest) {
    return request.post<ConversationMessage>(`/conversations/${conversationId}/messages`, data)
  },
  generateReply(conversationId: number) {
    return request.post<ReplySuggestion>(`/conversations/${conversationId}/generate-reply`)
  },
  listReplySuggestions(conversationId: number) {
    return request.get<ReplySuggestion[]>(`/conversations/${conversationId}/reply-suggestions`)
  },
  approveReply(suggestionId: number) {
    return request.post<ReplySuggestion>(`/replies/${suggestionId}/approve`)
  },
  rejectReply(suggestionId: number) {
    return request.post<ReplySuggestion>(`/replies/${suggestionId}/reject`)
  },
  updateStage(conversationId: number, stage: ConversationStage) {
    return request.patch<Conversation>(`/conversations/${conversationId}/stage`, { stage })
  },
  close(conversationId: number) {
    return request.post<Conversation>(`/conversations/${conversationId}/close`)
  },
  markCandidateReplied(conversationId: number) {
    return request.post<Conversation>(`/conversations/${conversationId}/mark-candidate-replied`)
  },
  scheduleFollowUp(conversationId: number, nextFollowUpAt: string) {
    return request.patch<Conversation>(`/conversations/${conversationId}/follow-up`, { nextFollowUpAt })
  },
}
