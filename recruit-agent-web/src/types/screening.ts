export type ScreeningRecommendation = 'INTERVIEW' | 'MANUAL_REVIEW' | 'NOT_MATCH'

export interface ScoreBreakdown {
  skillScore: number
  projectScore: number
  experienceScore: number
  educationScore: number
  bonusScore: number
  finalScore: number
}

export interface ScreeningResult {
  recommendation: ScreeningRecommendation
  score: ScoreBreakdown
  advantages: string[]
  risks: string[]
  missingInformation: string[]
}

export interface Evaluation {
  id: number
  jobId: number
  candidateId: number
  ruleScore: number | null
  semanticScore: number | null
  finalScore: number | null
  recommendation: ScreeningRecommendation
  confidence: number | null
  advantages: string[]
  risks: string[]
  missingInformation: string[]
  modelName: string | null
  promptVersion: string | null
  humanResult: string | null
  createdAt: string
}

export interface ScreenCandidateRequest {
  candidateId: number
}

export interface HumanReviewRequest {
  humanResult: 'ACCEPT' | 'REJECT'
}

export interface BatchScreenRequest {
  candidateIds: number[]
}

export interface CandidateRanking {
  evaluationId: number
  candidateId: number
  finalScore: number | null
  recommendation: ScreeningRecommendation
  humanResult: string | null
}
