import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    redirect: '/jobs',
    children: [
      {
        path: 'jobs',
        name: 'JobList',
        component: () => import('@/views/jobs/JobListView.vue'),
        meta: { title: '岗位管理', icon: 'Briefcase' },
      },
      {
        path: 'jobs/:id',
        name: 'JobDetail',
        component: () => import('@/views/jobs/JobDetailView.vue'),
        meta: { title: '岗位详情' },
      },
      {
        path: 'resumes',
        name: 'ResumeParsingCenter',
        component: () => import('@/views/resumes/ResumeParsingCenter.vue'),
        meta: { title: '简历解析', icon: 'Document' },
      },
      {
        path: 'candidates',
        name: 'CandidateList',
        component: () => import('@/views/candidates/CandidateListView.vue'),
        meta: { title: '候选人', icon: 'User' },
      },
      {
        path: 'candidates/:id',
        name: 'CandidateDetail',
        component: () => import('@/views/candidates/CandidateDetailView.vue'),
        meta: { title: '候选人详情' },
      },
      {
        path: 'candidates/:id/resumes',
        name: 'ResumeUpload',
        component: () => import('@/views/candidates/ResumeUploadView.vue'),
        meta: { title: '候选人简历' },
      },
      {
        path: 'screening',
        name: 'Screening',
        component: () => import('@/views/screening/ScreeningResultView.vue'),
        meta: { title: '匹配排行', icon: 'DataAnalysis' },
      },
      {
        path: 'evaluations/:id',
        name: 'EvaluationDetail',
        component: () => import('@/views/screening/EvaluationDetailView.vue'),
        meta: { title: '评估详情' },
      },
      {
        path: 'conversations',
        name: 'ConversationList',
        component: () => import('@/views/conversations/ConversationListView.vue'),
        meta: { title: 'AI 沟通', icon: 'ChatDotSquare' },
      },
      {
        path: 'conversations/:id',
        name: 'ConversationWorkbench',
        component: () => import('@/views/conversations/ConversationWorkbench.vue'),
        meta: { title: '沟通工作台' },
      },
      {
        path: 'tasks',
        name: 'AgentTasks',
        component: () => import('@/views/tasks/AgentTaskListView.vue'),
        meta: { title: 'Agent 任务', icon: 'Monitor' },
      },
      {
        path: 'knowledge',
        name: 'KnowledgeBase',
        component: () => import('@/views/knowledge/KnowledgeBaseView.vue'),
        meta: { title: '知识库', icon: 'Collection' },
      },
      {
        path: 'model-ops',
        name: 'ModelOps',
        component: () => import('@/views/ops/ModelOpsView.vue'),
        meta: { title: '模型与 Prompt', icon: 'Cpu', requiresAdmin: true },
      },
      {
        path: 'audit',
        name: 'AuditLog',
        component: () => import('@/views/ops/AuditLogView.vue'),
        meta: { title: '审计日志', icon: 'Tickets', requiresAdmin: true },
      },
      {
        path: 'vector',
        name: 'VectorOps',
        component: () => import('@/views/vector/VectorOpsView.vue'),
        meta: { title: '向量调试', icon: 'Search' },
      },
      {
        path: 'agent-ops',
        name: 'AgentOps',
        component: () => import('@/views/ops/AgentOpsView.vue'),
        meta: { title: 'Agent 运维中心', icon: 'Monitor', requiresAdmin: true },
      },
      {
        path: 'skills',
        name: 'SkillsList',
        component: () => import('@/views/skills/SkillsListView.vue'),
        meta: { title: 'Skills 中心', icon: 'Setting', requiresAdmin: true },
      },
      {
        path: 'skills/:id',
        name: 'SkillDetail',
        component: () => import('@/views/skills/SkillDetailView.vue'),
        meta: { title: 'Skill 详情', requiresAdmin: true },
      },
      {
        path: 'rag-governance',
        name: 'RAGGovernance',
        component: () => import('@/views/vector/RAGGovernanceView.vue'),
        meta: { title: 'RAG 治理', icon: 'DataBoard', requiresAdmin: true },
      },
      {
        path: 'traces',
        name: 'Traces',
        component: () => import('@/views/trace/TraceView.vue'),
        meta: { title: 'Trace 追踪', icon: 'Connection', requiresAdmin: true },
      },
      {
        path: 'evaluation-center',
        name: 'EvaluationCenter',
        component: () => import('@/views/evaluation/EvaluationCenterView.vue'),
        meta: { title: 'Agent 评测', icon: 'DataAnalysis', requiresAdmin: true },
      },
      {
        path: 'mcp',
        name: 'MCPDebug',
        component: () => import('@/views/mcp/McpDebugView.vue'),
        meta: { title: 'MCP 调试', icon: 'Tools', requiresAdmin: true },
      },
      {
        path: 'job-applications',
        name: 'JobApplicationList',
        component: () => import('@/views/job-applications/JobApplicationListView.vue'),
        meta: { title: '投递流水线', icon: 'List' },
      },
      {
        path: 'job-applications/:id',
        name: 'JobApplicationDetail',
        component: () => import('@/views/job-applications/JobApplicationDetailView.vue'),
        meta: { title: '投递详情' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth !== false && !auth.isAuthenticated) {
    next('/login')
    return
  }
  if (to.meta.requiresAuth !== false && auth.isAuthenticated && !auth.profileLoaded) {
    try {
      await auth.fetchMe()
    } catch {
      next('/login')
      return
    }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    next('/jobs')
    return
  }
  next()
})

export default router
