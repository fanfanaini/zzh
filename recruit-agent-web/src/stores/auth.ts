import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { AuthToken, LoginRequest, UserRole } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('auth_refresh_token'))
  const username = ref<string | null>(localStorage.getItem('auth_username'))
  const userId = ref<number | null>(Number(localStorage.getItem('auth_userId')) || null)
  const roles = ref<UserRole[]>(JSON.parse(localStorage.getItem('auth_roles') || '[]'))
  const permissions = ref<string[]>(JSON.parse(localStorage.getItem('auth_permissions') || '[]'))
  const tokenType = ref<string>('Bearer')
  const profileLoaded = ref(false)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const canWriteKnowledge = computed(() => hasAnyRole(['ADMIN', 'HR', 'RECRUITING_MANAGER']))
  const canDeleteKnowledge = computed(() => hasAnyRole(['ADMIN', 'RECRUITING_MANAGER']))
  const canApproveReplies = computed(() => hasAnyRole(['ADMIN', 'HR', 'RECRUITING_MANAGER']))
  const displayRole = computed(() => {
    const labels: Record<string, string> = {
      ADMIN: '管理员',
      HR: 'HR',
      RECRUITING_MANAGER: '招聘负责人',
      INTERVIEWER: '面试官',
    }
    return roles.value.map((role) => labels[role] || role).join(', ')
  })
  const hasPermission = (permission: string) => permissions.value.includes(permission)

  async function login(payload: LoginRequest) {
    const res = await authApi.login(payload)
    const data: AuthToken = res.data as unknown as AuthToken
    applyToken(data)
    return data
  }

  function applyToken(data: AuthToken) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    username.value = data.username
    userId.value = data.userId
    roles.value = data.roles
    tokenType.value = data.tokenType

    localStorage.setItem('auth_token', data.accessToken)
    localStorage.setItem('auth_refresh_token', data.refreshToken)
    localStorage.setItem('auth_username', data.username)
    localStorage.setItem('auth_userId', String(data.userId))
    localStorage.setItem('auth_roles', JSON.stringify(data.roles))
  }

  async function fetchMe() {
    if (!token.value) return null
    const res = await authApi.me()
    userId.value = res.data.userId
    username.value = res.data.username
    roles.value = res.data.roles
    permissions.value = res.data.permissions
    localStorage.setItem('auth_username', res.data.username)
    localStorage.setItem('auth_userId', String(res.data.userId))
    localStorage.setItem('auth_roles', JSON.stringify(res.data.roles))
    localStorage.setItem('auth_permissions', JSON.stringify(res.data.permissions))
    profileLoaded.value = true
    return res.data
  }

  function hasAnyRole(allowed: UserRole[]) {
    return roles.value.some((role) => allowed.includes(role))
  }

  function logout() {
    token.value = null
    refreshToken.value = null
    username.value = null
    userId.value = null
    roles.value = []
    permissions.value = []
    profileLoaded.value = false

    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_refresh_token')
    localStorage.removeItem('auth_username')
    localStorage.removeItem('auth_userId')
    localStorage.removeItem('auth_roles')
    localStorage.removeItem('auth_permissions')
  }

  return {
    token,
    refreshToken,
    username,
    userId,
    roles,
    permissions,
    tokenType,
    isAuthenticated,
    isAdmin,
    canWriteKnowledge,
    canDeleteKnowledge,
    canApproveReplies,
    profileLoaded,
    displayRole,
    hasAnyRole,
    hasPermission,
    fetchMe,
    applyToken,
    login,
    logout,
  }
})
