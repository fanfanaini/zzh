<template>
  <div class="page-container">
    <div class="page-header">
      <h2>AI 沟通</h2>
      <el-button type="primary" @click="showCreate = true">
        <el-icon><Plus /></el-icon>发起新会话
      </el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">岗位数</div>
          <div class="stat-value">{{ jobStore.jobs.length }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">候选人</div>
          <div class="stat-value">{{ candidateStore.candidates.length }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">沟通阶段</div>
          <div class="stat-value" style="font-size:1rem;">初联 → 意向 → 薪资 → 面试</div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="16" style="margin-bottom:20px;">
      <el-col :span="12">
        <el-card class="quick-card" @click="showCreate = true">
          <div class="quick-icon"><el-icon :size="28"><ChatDotSquare /></el-icon></div>
          <div class="quick-content">
            <h4>发起招聘会话</h4>
            <p>选择岗位和候选人，开始 AI 辅助沟通</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="quick-card" @click="router.push('/screening')">
          <div class="quick-icon"><el-icon :size="28"><DataAnalysis /></el-icon></div>
          <div class="quick-content">
            <h4>查看匹配结果</h4>
            <p>从匹配排行的候选人中发起沟通</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="history-card" v-loading="loadingConversations">
      <template #header>
        <div class="history-header">
          <span class="section-title">历史会话</span>
          <div class="history-filters">
            <el-select v-model="filters.jobId" clearable filterable placeholder="全部岗位" @change="loadConversations">
              <el-option v-for="job in jobStore.jobs" :key="job.id" :label="job.name" :value="job.id" />
            </el-select>
            <el-select v-model="filters.candidateId" clearable filterable placeholder="全部候选人" @change="loadConversations">
              <el-option v-for="candidate in candidateStore.candidates" :key="candidate.id" :label="candidate.name" :value="candidate.id" />
            </el-select>
            <el-select v-model="filters.stage" clearable placeholder="全部阶段">
              <el-option v-for="stage in conversationStages" :key="stage" :label="stage" :value="stage" />
            </el-select>
            <el-select v-model="filters.status" clearable placeholder="全部状态">
              <el-option label="进行中" value="OPEN" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
            <el-button :loading="loadingConversations" @click="loadConversations">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredConversations" stripe style="width: 100%">
        <el-table-column label="ID" width="90">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="180">
          <template #default="{ row }">{{ jobName(row.jobId) }}</template>
        </el-table-column>
        <el-table-column label="候选人" min-width="160">
          <template #default="{ row }">{{ candidateName(row.candidateId) }}</template>
        </el-table-column>
        <el-table-column prop="stage" label="阶段" width="150" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ new Date(row.createdAt).toLocaleString('zh-CN') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="router.push(`/conversations/${row.id}`)">打开</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #empty>
        <el-empty description="暂无历史会话" />
      </template>
    </el-card>

    <!-- 沟通工作流说明 -->
    <el-card class="guide-card">
      <template #header>
        <span class="section-title">招聘沟通流程</span>
      </template>
      <el-steps :active="3" align-center process-status="finish">
        <el-step title="初次联系" description="打招呼，介绍岗位" />
        <el-step title="询问意向" description="了解候选人求职意愿" />
        <el-step title="确认时间" description="协商面试或沟通时间" />
        <el-step title="薪资沟通" description="讨论薪资期望" />
        <el-step title="邀约面试" description="发送面试邀请" />
      </el-steps>
      <div class="guide-footer">
        <p>AI 会自动识别候选人意图并生成回复建议，敏感场景（薪资承诺、录用决定）需人工确认后发送。</p>
      </div>
    </el-card>

    <!-- 创建会话弹窗 -->
    <el-dialog v-model="showCreate" title="发起招聘会话" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="岗位" prop="jobId">
          <el-select v-model="createForm.jobId" filterable placeholder="选择岗位" style="width:100%">
            <el-option v-for="j in jobStore.jobs" :key="j.id" :label="j.name" :value="j.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="候选人" prop="candidateId">
          <el-select v-model="createForm.candidateId" filterable placeholder="选择候选人" style="width:100%">
            <el-option v-for="c in candidateStore.candidates" :key="c.id" :label="`${c.name} (${c.skills?.slice(0,3).join(', ')})`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="handleCreate">发起会话</el-button>
          <el-button @click="showCreate = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, ChatDotSquare, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useJobStore } from '@/stores/jobs'
import { useCandidateStore } from '@/stores/candidates'
import { conversationApi } from '@/api/conversations'
import type { Conversation, ConversationStage } from '@/types/conversation'

const router = useRouter()
const jobStore = useJobStore()
const candidateStore = useCandidateStore()

const showCreate = ref(false)
const creating = ref(false)
const loadingConversations = ref(false)
const conversations = ref<Conversation[]>([])
const filters = reactive({
  jobId: null as number | null,
  candidateId: null as number | null,
  stage: '' as ConversationStage | '',
  status: '',
})
const conversationStages: ConversationStage[] = [
  'INITIAL_CONTACT',
  'ASK_INTENTION',
  'ASK_AVAILABILITY',
  'ASK_SALARY',
  'INVITE_INTERVIEW',
  'REJECT',
  'MANUAL_REVIEW',
]
const filteredConversations = computed(() =>
  conversations.value.filter((conversation) =>
    (!filters.stage || conversation.stage === filters.stage)
    && (!filters.status || conversation.status === filters.status),
  ),
)
const formRef = ref<FormInstance>()
const createForm = ref({ jobId: null as number | null, candidateId: null as number | null })
const createRules: FormRules = {
  jobId: [{ required: true, message: '请选择岗位', trigger: 'change' }],
  candidateId: [{ required: true, message: '请选择候选人', trigger: 'change' }],
}

onMounted(async () => {
  await jobStore.fetchJobs()
  await candidateStore.fetchCandidates()
  await loadConversations()
})

async function loadConversations() {
  loadingConversations.value = true
  try {
    conversations.value = (await conversationApi.list({
      jobId: filters.jobId || undefined,
      candidateId: filters.candidateId || undefined,
      limit: 200,
    })).data
  } finally {
    loadingConversations.value = false
  }
}

function jobName(jobId: number) {
  return jobStore.jobs.find((job) => job.id === jobId)?.name || `岗位 #${jobId}`
}

function candidateName(candidateId: number) {
  return candidateStore.candidates.find((candidate) => candidate.id === candidateId)?.name || `候选人 #${candidateId}`
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (createForm.value.jobId == null || createForm.value.candidateId == null) return
  creating.value = true
  try {
    const res = await conversationApi.create({
      jobId: createForm.value.jobId,
      candidateId: createForm.value.candidateId,
    })
    const conv = res.data as any
    ElMessage.success('会话创建成功')
    showCreate.value = false
    await loadConversations()
    router.push(`/conversations/${conv.id}`)
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.stat-row {
  margin-bottom: 20px;
}

.history-header,
.history-filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-header {
  justify-content: space-between;
}

.history-filters {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.history-filters :deep(.el-select) {
  width: 150px;
}

.quick-card {
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  border-radius: var(--radius-md);

  &:hover {
    border-color: var(--color-accent);
    box-shadow: var(--shadow-md);
  }

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px 24px;
  }
}

.quick-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--color-accent-bg);
  color: var(--color-accent);
  flex-shrink: 0;
}

.quick-content {
  h4 {
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 2px;
  }
  p {
    font-size: 0.8rem;
    color: var(--color-text-secondary);
    margin: 0;
  }
}

.guide-card {
  border-radius: var(--radius-md);
}

.guide-footer {
  margin-top: 20px;
  padding: 12px 16px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);

  p {
    font-size: 0.85rem;
    color: var(--color-text-secondary);
    margin: 0;
  }
}
</style>
