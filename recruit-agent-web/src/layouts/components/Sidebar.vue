<template>
  <el-menu
    :default-active="activeRoute"
    :collapse="collapsed"
    class="app-sidebar"
    background-color="var(--color-bg-deep)"
    text-color="var(--color-text-secondary)"
    active-text-color="var(--color-accent)"
    router
  >
    <div class="sidebar-header">
      <div class="brand" :class="{ collapsed }">
        <span class="brand-icon">R</span>
        <span v-show="!collapsed" class="brand-text">RecruitAgent</span>
      </div>
    </div>

    <el-menu-item index="/jobs">
      <el-icon><Briefcase /></el-icon>
      <template #title>岗位管理</template>
    </el-menu-item>

    <el-menu-item index="/resumes">
      <el-icon><Document /></el-icon>
      <template #title>简历解析</template>
    </el-menu-item>

    <el-menu-item index="/candidates">
      <el-icon><User /></el-icon>
      <template #title>候选人</template>
    </el-menu-item>

    <el-menu-item index="/screening">
      <el-icon><DataAnalysis /></el-icon>
      <template #title>匹配排行</template>
    </el-menu-item>

    <el-menu-item index="/conversations">
      <el-icon><ChatDotSquare /></el-icon>
      <template #title>AI 沟通</template>
    </el-menu-item>

    <el-menu-item index="/tasks">
      <el-icon><Monitor /></el-icon>
      <template #title>Agent 任务</template>
    </el-menu-item>

    <el-menu-item index="/knowledge">
      <el-icon><Collection /></el-icon>
      <template #title>知识库</template>
    </el-menu-item>

    <el-menu-item index="/job-applications">
      <el-icon><List /></el-icon>
      <template #title>投递流水线</template>
    </el-menu-item>

    <!-- Admin Section -->
    <div class="menu-divider" v-if="auth.isAdmin && !collapsed">
      <span class="divider-text">高级功能</span>
    </div>

    <el-menu-item v-if="auth.isAdmin" index="/agent-ops">
      <el-icon><Odometer /></el-icon>
      <template #title>Agent 运维中心</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/skills">
      <el-icon><Setting /></el-icon>
      <template #title>Skills 中心</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/rag-governance">
      <el-icon><DataBoard /></el-icon>
      <template #title>RAG 治理</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/traces">
      <el-icon><Connection /></el-icon>
      <template #title>Trace 追踪</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/evaluation-center">
      <el-icon><DataAnalysis /></el-icon>
      <template #title>Agent 评测</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/model-ops">
      <el-icon><Cpu /></el-icon>
      <template #title>模型与 Prompt</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/mcp">
      <el-icon><Tools /></el-icon>
      <template #title>MCP 调试</template>
    </el-menu-item>

    <el-menu-item v-if="auth.isAdmin" index="/audit">
      <el-icon><Tickets /></el-icon>
      <template #title>审计日志</template>
    </el-menu-item>

    <el-menu-item index="/vector">
      <el-icon><Search /></el-icon>
      <template #title>向量调试</template>
    </el-menu-item>

    <button class="sidebar-collapse-trigger" type="button" @click="collapsed = !collapsed">
      <el-icon>
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
    </button>
  </el-menu>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  Briefcase,
  ChatDotSquare,
  Collection,
  List,
  Connection,
  Cpu,
  DataAnalysis,
  DataBoard,
  Document,
  Expand,
  Fold,
  Monitor,
  Odometer,
  Search,
  Setting,
  Tickets,
  Tools,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()
const auth = useAuthStore()
const collapsed = ref(false)

const activeRoute = computed(() => {
  const path = route.path
  if (path === '/resumes') return '/resumes'
  if (path.startsWith('/jobs')) return '/jobs'
  if (path.startsWith('/candidates')) return '/candidates'
  if (path.startsWith('/screening') || path.startsWith('/evaluations')) return '/screening'
  if (path.startsWith('/conversations')) return '/conversations'
  if (path.startsWith('/tasks')) return '/tasks'
  if (path.startsWith('/knowledge')) return '/knowledge'
  if (path.startsWith('/agent-ops')) return '/agent-ops'
  if (path.startsWith('/skills')) return '/skills'
  if (path.startsWith('/rag-governance')) return '/rag-governance'
  if (path.startsWith('/traces')) return '/traces'
  if (path.startsWith('/evaluation-center')) return '/evaluation-center'
  if (path.startsWith('/model-ops')) return '/model-ops'
  if (path.startsWith('/mcp')) return '/mcp'
  if (path.startsWith('/audit')) return '/audit'
  if (path.startsWith('/vector')) return '/vector'
  if (path.startsWith('/job-applications')) return '/job-applications'
  return path
})
</script>

<style scoped>
.app-sidebar {
  height: 100vh;
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
  width: 220px;
  overflow: hidden;
}

.app-sidebar:not(.el-menu--collapse) {
  width: 220px;
}

.app-sidebar.el-menu--collapse {
  width: 64px;
}

.sidebar-header {
  padding: 16px 16px 8px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 8px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand.collapsed {
  justify-content: center;
}

.brand-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-accent);
  color: white;
  border-radius: var(--radius-sm);
  font-weight: 700;
  font-size: 1rem;
  flex-shrink: 0;
}

.brand-text {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
}

.menu-divider {
  padding: 12px 16px 4px;
  font-size: 0.72rem;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.el-menu--collapse .menu-divider {
  display: none;
}

.sidebar-collapse-trigger {
  margin-top: auto;
  min-height: 44px;
  padding: 12px 20px;
  border: 0;
  border-top: 1px solid var(--color-border);
  cursor: pointer;
  color: var(--color-text-secondary);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s ease, background 0.15s ease;
}

.sidebar-collapse-trigger:hover {
  color: var(--color-text-primary);
  background: var(--color-surface);
}

.sidebar-collapse-trigger:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: -2px;
}

.el-menu-item {
  margin: 2px 8px;
  border-radius: var(--radius-sm);
}
</style>
