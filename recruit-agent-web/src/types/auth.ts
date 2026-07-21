export type UserRole = 'ADMIN' | 'HR' | 'RECRUITING_MANAGER' | 'INTERVIEWER'

export interface AuthToken {
  tokenType: string
  accessToken: string
  refreshToken: string
  expiresInSeconds: number
  refreshExpiresInSeconds: number
  userId: number
  username: string
  roles: UserRole[]
}

export interface UserAccount {
  id: number
  username: string
  roles: UserRole[]
  enabled: boolean
  createdAt: string
}

export interface CurrentUser {
  userId: number
  username: string
  roles: UserRole[]
  permissions: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  roles?: UserRole[]
}
