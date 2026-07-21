export interface Candidate {
  id: number
  name: string
  phoneHash: string
  emailHash: string
  educationLevel: string
  totalExperienceYears: number
  expectedCity: string
  skills: string[]
  status?: 'ACTIVE' | 'DISABLED' | 'DELETED'
  updatedAt?: string
  createdAt: string
}

export interface RegisterCandidateRequest {
  name: string
  phoneHash?: string
  emailHash?: string
  educationLevel?: string
  totalExperienceYears?: number
  expectedCity?: string
  skills?: string[]
}

export type ResumeParseStatus = 'UPLOADED' | 'PARSING' | 'PARSED' | 'FAILED'

export interface ResumeDocument {
  id: number
  candidateId: number
  originalFilename: string
  contentType: string
  fileSize: number
  storageObjectName: string
  parseStatus: ResumeParseStatus
  rawText?: string
  parseErrorMessage?: string
  parsedAt?: string
  updatedAt?: string
  uploadedAt: string
}

export interface ResumeStructuredDetail {
  educationExperiences: EducationExperience[]
  workExperiences: WorkExperience[]
  projectExperiences: ProjectExperience[]
  certificates: string[]
  languages: string[]
  awards: string[]
  fieldSources: Record<string, string>
  fieldConfidence: Record<string, number>
}

export interface EducationExperience {
  school: string
  degree: string
  major: string
  startDate: string
  endDate: string
  sourceText: string
  confidence: number | null
}

export interface WorkExperience {
  company: string
  title: string
  startDate: string
  endDate: string
  description: string
  sourceText: string
  confidence: number | null
}

export interface ProjectExperience {
  name: string
  role: string
  startDate: string
  endDate: string
  description: string
  sourceText: string
  confidence: number | null
}

export interface ResumeDetail {
  resume: ResumeDocument
  structuredDetail: ResumeStructuredDetail
}
