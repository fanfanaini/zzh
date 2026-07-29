export type RecruitmentSkillStatus = 'DRAFT' | 'ENABLED' | 'DISABLED'

export interface RecruitmentSkill {
  id: number
  name: string
  category: string
  description: string
  scenario: string
  triggerKeywords: string[]
  priority: number
  status: RecruitmentSkillStatus
  currentVersionId: number | null
  createdAt: string
  updatedAt: string
}

export interface RecruitmentSkillCard {
  id: number
  name: string
  category: string
  description: string
  scenario: string
  triggerKeywords: string[]
  priority: number
  version: string
}

export interface RecruitmentSkillVersion {
  id: number
  skillId: number
  version: string
  instruction: string
  promptFragment: string | null
  fewShots: string | null
  guardrails: string | null
  allowedTools: string[]
  riskTags: string[]
  handoffPolicy: string | null
  published: boolean
  createdAt: string
  publishedAt: string | null
}

export interface SkillMatch {
  skillId: number
  name: string
  category: string
  version: string
  score: number
  reason: string
  card: RecruitmentSkillCard
}

export interface CreateSkillRequest {
  name: string
  category: string
  description: string
  scenario: string
  triggerKeywords?: string[]
  priority: number
  version: string
  instruction: string
  promptFragment?: string
  fewShots?: string
  guardrails?: string
  allowedTools?: string[]
  riskTags?: string[]
  handoffPolicy?: string
  publish?: boolean
}

export interface CreateSkillVersionRequest {
  version: string
  instruction: string
  promptFragment?: string
  fewShots?: string
  guardrails?: string
  allowedTools?: string[]
  riskTags?: string[]
  handoffPolicy?: string
  publish?: boolean
}

export interface SkillMatchRequest {
  query: string
  limit?: number
}

export interface ImportSkillMarkdownRequest {
  markdown: string
}
