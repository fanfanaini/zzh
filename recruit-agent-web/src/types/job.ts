export type EducationLevel = 'UNKNOWN' | 'HIGH_SCHOOL' | 'COLLEGE' | 'BACHELOR' | 'MASTER' | 'DOCTOR'

export interface Job {
  id: number
  name: string
  description: string
  requiredSkills: string[]
  preferredSkills: string[]
  educationRequirement: EducationLevel
  minExperienceYears: number
  maxExperienceYears: number
  status?: 'OPEN' | 'CLOSED' | 'DELETED'
  updatedAt?: string
  createdAt: string
}

export interface CreateJobRequest {
  name: string
  description: string
  requiredSkills?: string[]
  preferredSkills?: string[]
  educationRequirement?: EducationLevel
  minExperienceYears?: number
  maxExperienceYears?: number
}
