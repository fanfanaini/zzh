<template>
  <header class="app-header">
    <div class="header-left">
      <h1 class="page-title">{{ pageTitle }}</h1>
    </div>
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-info" type="button">
          <el-avatar :size="28" class="user-avatar">{{ auth.username?.charAt(0)?.toUpperCase() }}</el-avatar>
          <span class="user-name">{{ auth.username }}</span>
          <span class="user-role">{{ auth.displayRole }}</span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人信息
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>

  <el-dialog v-model="profileOpen" title="个人信息" width="460px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="用户 ID">{{ auth.userId ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ auth.username }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ auth.displayRole || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider content-position="left">修改密码</el-divider>
    <el-form label-position="top">
      <el-form-item label="当前密码">
        <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="profileOpen = false">关闭</el-button>
      <el-button type="primary" :loading="changingPassword" @click="changePassword">更新密码</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, SwitchButton, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const profileOpen = ref(false)
const changingPassword = ref(false)
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pageTitle = computed(() => (route.meta?.title as string) || 'RecruitAgent')

function handleCommand(command: string) {
  if (command === 'profile') {
    profileOpen.value = true
    return
  }
  if (command === 'logout') {
    auth.logout()
    router.push('/login')
  }
}

async function changePassword() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写当前密码和新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少需要 6 个字符')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await authApi.changePassword(passwordForm.currentPassword, passwordForm.newPassword)
    Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    profileOpen.value = false
    ElMessage.success('密码已更新，请使用新密码重新登录')
    auth.logout()
    await router.push('/login')
  } finally {
    changingPassword.value = false
  }
}
</script>

<style scoped>
.app-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.page-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  letter-spacing: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  min-height: 44px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  transition: background 0.15s ease;
}

.user-info:hover {
  background: var(--color-bg);
}

.user-info:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.user-avatar {
  background: var(--color-accent);
  color: white;
  font-weight: 600;
}

.user-name {
  font-size: 0.9rem;
  color: var(--color-text-regular);
}

.user-role {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}
</style>
