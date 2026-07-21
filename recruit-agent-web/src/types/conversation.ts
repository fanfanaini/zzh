export type ConversationStage =
  | 'INITIAL_CONTACT'
  | 'ASK_INTENTION'
  | 'ASK_AVAILABILITY'
  | 'ASK_SALARY'
  | 'INVITE_INTERVIEW'
  | 'REJECT'
  | 'MANUAL_REVIEW'

export type ConversationStatus = 'OPEN' | 'CLOSED'

export interface Conversation {
  id: number
  jobId: number
  candidateId: number
  stage: ConversationStage
  status: ConversationStatus
  updatedAt?: string
  lastCandidateReplyAt?: string
  nextFollowUpAt?: string
  createdAt: string
}

export type MessageSenderType = 'HR' | 'CANDIDATE' | 'SYSTEM'

export interface ConversationMessage {
  id: number
  conversationId: number
  senderType: MessageSenderType
  content: string
  createdAt: string
}

export type ReplyIntent =
  | 'GREETING'
  | 'ASK_INTENTION'
  | 'ASK_AVAILABILITY'
  | 'ASK_SALARY'
  | 'INVITE_INTERVIEW'
  | 'REJECT_CANDIDATE'
  | 'ANSWER_JOB_QUESTION'
  | 'UNKNOWN'

export type ReplySuggestionStatus = 'DRAFT' | 'APPROVED' | 'REJECTED'

export interface ReplySuggestion {
  id: number
  conversationId: number
  intent: ReplyIntent
  confidence: number
  needHuman: boolean
  suggestedReply: string
  status: ReplySuggestionStatus
  createdAt: string
}

export interface CreateConversationRequest {
  jobId: number
  candidateId: number
  stage?: ConversationStage
}

export interface AddMessageRequest {
  senderType: MessageSenderType
  content: string
}
