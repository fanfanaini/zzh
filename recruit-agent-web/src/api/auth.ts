import request from './request'
import type { AuthToken, CurrentUser, LoginRequest, RegisterRequest, UserAccount } from '@/types/auth'

export const authApi = {
  login(data: LoginRequest) {
    return request.post<AuthToken>('/auth/login', data)
  },
  refresh(refreshToken: string) {
    return request.post<AuthToken>('/auth/refresh', { refreshToken })
  },
  register(data: RegisterRequest) {
    return request.post<UserAccount>('/auth/register', data)
  },
  me() {
    return request.get<CurrentUser>('/auth/me')
  },
  changePassword(currentPassword: string, newPassword: string) {
    return request.put('/auth/password', { currentPassword, newPassword })
  },
}
