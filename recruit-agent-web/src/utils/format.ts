import type { EducationLevel } from '@/types/job'
import type { ScreeningRecommendation } from '@/types/screening'

export function formatDateTime(value?: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

export function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleDateString('zh-CN') : '-'
}

export function educationLabel(value?: EducationLevel | string | null) {
  const map: Record<string, string> = {
    UNKNOWN: '不限',
    HIGH_SCHOOL: '高中',
    COLLEGE: '大专',
    BACHELOR: '本科',
    MASTER: '硕士',
    DOCTOR: '博士',
  }
  return value ? map[value] || value : '-'
}

export function recommendationLabel(value?: ScreeningRecommendation | string | null) {
  const map: Record<string, string> = {
    INTERVIEW: '推荐面试',
    MANUAL_REVIEW: '待人工判断',
    NOT_MATCH: '不匹配',
  }
  return value ? map[value] || value : '-'
}

export function recommendationClass(value?: ScreeningRecommendation | string | null) {
  const map: Record<string, string> = {
    INTERVIEW: 'interview',
    MANUAL_REVIEW: 'manual-review',
    NOT_MATCH: 'not-match',
  }
  return value ? map[value] || String(value).toLowerCase().replace(/_/g, '-') : ''
}

export function resumeStatusLabel(value?: string | null) {
  const map: Record<string, string> = {
    UPLOADED: '待解析',
    PARSING: '解析中',
    PARSED: '已解析',
    FAILED: '失败',
  }
  return value ? map[value] || value : '-'
}

export function taskStatusLabel(value?: string | null) {
  const map: Record<string, string> = {
    PENDING: '待执行',
    RUNNING: '执行中',
    SUCCEEDED: '已完成',
    FAILED: '失败',
  }
  return value ? map[value] || value : '-'
}
