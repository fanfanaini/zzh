import request from './request'
import type {
  RecruitmentSkill,
  RecruitmentSkillCard,
  RecruitmentSkillVersion,
  SkillMatch,
  CreateSkillRequest,
  CreateSkillVersionRequest,
  SkillMatchRequest,
  ImportSkillMarkdownRequest,
} from '@/types/skill'

export const skillApi = {
  list() {
    return request.get<RecruitmentSkill[]>('/skills')
  },
  cards() {
    return request.get<RecruitmentSkillCard[]>('/skills/cards')
  },
  detail(skillId: number) {
    return request.get<RecruitmentSkill>(`/skills/${skillId}`)
  },
  versions(skillId: number) {
    return request.get<RecruitmentSkillVersion[]>(`/skills/${skillId}/versions`)
  },
  create(data: CreateSkillRequest) {
    return request.post<RecruitmentSkill>('/skills', data)
  },
  addVersion(skillId: number, data: CreateSkillVersionRequest) {
    return request.post<RecruitmentSkillVersion>(`/skills/${skillId}/versions`, data)
  },
  publish(skillId: number, versionId: number) {
    return request.post<RecruitmentSkill>(`/skills/${skillId}/versions/${versionId}/publish`)
  },
  disable(skillId: number) {
    return request.post<RecruitmentSkill>(`/skills/${skillId}/disable`)
  },
  match(data: SkillMatchRequest) {
    return request.post<SkillMatch[]>('/skills/match', data)
  },
  importMarkdown(data: ImportSkillMarkdownRequest) {
    return request.post<RecruitmentSkill>('/skills/import-markdown', data)
  },
}
