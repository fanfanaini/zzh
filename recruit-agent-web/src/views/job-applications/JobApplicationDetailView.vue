<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="router.push('/job-applications')">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2>投递详情</h2>
        <span class="app-id">#{{ route.params.id }}</span>
      </div>
      <div class="header-actions" v-if="app">
        <el-button :loading="histLoading" @click="loadHistory">刷新</el-button>
        <el-button v-if="!isTerminal" type="primary" @click="showAdvance = true">推进阶段</el-button>
        <el-tag v-if="isTerminal" type="info" effect="dark" size="large">已终态</el-tag>
      </div>
    </div>

    <el-empty v-if="!app && !loading" description="投递记录不存在" />

    <template v-if="app">
      <!-- Application Info -->
      <el-card class="detail-card">
        <template #header><span class="section-title">基本信息</span></template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="岗位">
            <el-button text type="primary" size="small" @click="router.push(`/jobs/${app.jobId}`)">
              #{{ app.jobId }}
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="候选人">
            <el-button text type="primary" size="small" @click="router.push(`/candidates/${app.candidateId}`)">
              #{{ app.candidateId }}
            </el-button>
          </el-descriptions-item>
          <el-descriptions-item label="当前阶段">
            <el-tag :type="stageTagType(app.stage)" effect="dark" size="small">
              {{ stageLabel(app.stage) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ app.ownerUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ app.source || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(app.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(app.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ app.closedAt ? formatDateTime(app.closedAt) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="拒绝原因" :span="2">{{ app.rejectionReason || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="note-block" v-if="app.note">
          <span class="desc-label">备注</span>
          <p>{{ app.note }}</p>
        </div>
      </el-card>

      <!-- Stage Flow -->
      <el-card class="detail-card">
        <template #header><span class="section-title">阶段流转</span></template>
        <div class="stage-flow">
          <div
            v-for="(s, idx) in stageOptions"
            :key="s.value"
            :class="['stage-node', {
              active: s.value === app.stage,
              passed: isPassed(s.value),
              terminal: s.terminal,
            }]"
          >
            <div class="stage-indicator">
              <el-icon v-if="isPassed(s.value)"><Check /></el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="stage-name">{{ s.label }}</div>
            <div v-if="idx < stageOptions.length - 1" class="stage-connector" />
          </div>
        </div>
      </el-card>

      <!-- Related references -->
      <el-row :gutter="16" class="ref-row">
        <el-col :span="12">
          <el-card class="detail-card">
            <template #header><span class="section-title">关联评估</span></template>
            <div v-if="app.latestEvaluationId">
              <el-button text type="primary" @click="router.push(`/evaluations/${app.latestEvaluationId}`)">
                查看评估 #{{ app.latestEvaluationId }}
              </el-button>
            </div>
            <el-empty v-else description="暂无关联评估" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="detail-card">
            <template #header><span class="section-title">关联会话</span></template>
            <div v-if="app.latestConversationId">
              <el-button text type="primary" @click="router.push(`/conversations/${app.latestConversationId}`)">
                查看会话 #{{ app.latestConversationId }}
              </el-button>
            </div>
            <el-empty v-else description="暂无关联会话" />
          </el-card>
        </el-col>
      </el-row>

      <!-- History Timeline -->
      <el-card class="detail-card">
        <template #header><span class="section-title">阶段变更历史</span></template>
        <div v-if="history.length > 0">
          <el-timeline>
            <el-timeline-item
              v-for="h in sortedHistory"
              :key="h.id"
              :timestamp="formatDateTime(h.createdAt)"
              :type="h.toStage === 'REJECTED' || h.toStage === 'CLOSED' ? 'danger' : 'primary'"
            >
              <div class="history-item">
                <div class="history-transition">
                  <el-tag size="small" type="info" effect="plain">
                    {{ h.fromStage ? stageLabel(h.fromStage) : '开始' }}
                  </el-tag>
                  <el-icon><ArrowRightBold /></el-icon>
                  <el-tag size="small" :type="stageTagType(h.toStage)" effect="dark">
                    {{ stageLabel(h.toStage) }}
                  </el-tag>
                </div>
                <div class="history-meta">
                  <span v-if="h.operatorUserId">操作人：{{ h.operatorUserId }}</span>
                  <span v-if="h.reason">原因：{{ h.reason }}</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
        <el-empty v-else description="暂无变更历史" />
      </el-card>
    </template>

    <!-- Advance Stage Dialog -->
    <el-dialog v-model="showAdvance" title="推进阶段" width="480px" :close-on-click-modal="false">
      <el-form :model="advanceForm" label-position="top">
        <el-form-item label="目标阶段" required>
          <el-select v-model="advanceForm.stage" style="width:100%">
            <el-option
              v-for="s in availableNextStages"
              :key="s.value"
              :label="s.label"
              :value="s.value"
              :disabled="s.value === app?.stage"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人 ID">
          <el-input-number v-model="advanceForm.operatorUserId" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="原因/备注">
          <el-input v-model="advanceForm.reason" type="textarea" :rows="2" placeholder="变更原因" />
        </el-form-item>
        <el-form-item label="关联评估 ID">
          <el-input-number v-model="advanceForm.evaluationId" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联会话 ID">
          <el-input-number v-model="advanceForm.conversationId" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="advanceForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdvance = false">取消</el-button>
        <el-button type="primary" :loading="advancing" @click="handleAdvance">确认推进</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRightBold, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { jobApplicationApi } from '@/api/job-applications'
import type { JobApplicationResponse, JobApplicationStage, JobApplicationHistoryResponse } from '@/types/job-application'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const appId = Number(route.params.id)

const loading = ref(false)
const histLoading = ref(false)
const advancing = ref(false)
const showAdvance = ref(false)
const app = ref<JobApplicationResponse | null>(null)
const history = ref<JobApplicationHistoryResponse[]>([])

const advanceForm = reactive({
  stage: '' as JobApplicationStage | '',
  operatorUserId: null as number | null,
  reason: '',
  evaluationId: null as number | null,
  conversationId: null as number | null,
  note: '',
})

const stageOptions = [
  { value: 'APPLIED' as JobApplicationStage, label: '已投递', terminal: false },
  { value: 'SCREENING' as JobApplicationStage, label: '筛选中', terminal: false },
  { value: 'HR_REVIEW' as JobApplicationStage, label: 'HR 审核', terminal: false },
  { value: 'COMMUNICATION' as JobApplicationStage, label: '沟通中', terminal: false },
  { value: 'INTERVIEW' as JobApplicationStage, label: '面试', terminal: false },
  { value: 'OFFER' as JobApplicationStage, label: 'Offer', terminal: false },
  { value: 'HIRED' as JobApplicationStage, label: '已录用', terminal: true },
  { value: 'REJECTED' as JobApplicationStage, label: '已拒绝', terminal: true },
  { value: 'WITHDRAWN' as JobApplicationStage, label: '已撤回', terminal: true },
  { value: 'CLOSED' as JobApplicationStage, label: '已关闭', terminal: true },
]

const terminalStages: JobApplicationStage[] = ['HIRED', 'REJECTED', 'WITHDRAWN', 'CLOSED']

const isTerminal = computed(() => app.value && terminalStages.includes(app.value.stage))

const sortedHistory = computed(() =>
  [...history.value].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
)

const availableNextStages = computed(() => {
  if (!app.value) return stageOptions
  return stageOptions.filter((s) => s.value !== app.value!.stage)
})

function stageLabel(stage: JobApplicationStage): string {
  const map: Record<string, string> = {
    APPLIED: '已投递', SCREENING: '筛选中', HR_REVIEW: 'HR 审核',
    COMMUNICATION: '沟通中', INTERVIEW: '面试', OFFER: 'Offer',
    HIRED: '已录用', REJECTED: '已拒绝', WITHDRAWN: '已撤回', CLOSED: '已关闭',
  }
  return map[stage] || stage
}

function stageTagType(stage: JobApplicationStage): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    APPLIED: 'info', SCREENING: 'primary', HR_REVIEW: 'warning',
    COMMUNICATION: 'primary', INTERVIEW: 'warning', OFFER: 'success',
    HIRED: 'success', REJECTED: 'danger', WITHDRAWN: 'info', CLOSED: 'info',
  }
  return map[stage] || 'info'
}

function isPassed(stage: JobApplicationStage): boolean {
  if (!app.value) return false
  const order = stageOptions.map((s) => s.value)
  const currentIdx = order.indexOf(app.value.stage)
  const targetIdx = order.indexOf(stage)
  return targetIdx < currentIdx
}

onMounted(() => {
  loadDetail()
  loadHistory()
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await jobApplicationApi.get(appId)
    app.value = res.data
    advanceForm.stage = '' as JobApplicationStage | ''
  } catch {
    app.value = null
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  histLoading.value = true
  try {
    const res = await jobApplicationApi.history(appId)
    history.value = res.data
  } finally {
    histLoading.value = false
  }
}

async function handleAdvance() {
  if (!advanceForm.stage) {
    ElMessage.warning('请选择目标阶段')
    return
  }
  advancing.value = true
  try {
    await jobApplicationApi.advance(appId, {
      stage: advanceForm.stage as JobApplicationStage,
      operatorUserId: advanceForm.operatorUserId || undefined,
      reason: advanceForm.reason || undefined,
      evaluationId: advanceForm.evaluationId || undefined,
      conversationId: advanceForm.conversationId || undefined,
      note: advanceForm.note || undefined,
    })
    ElMessage.success('阶段已推进')
    showAdvance.value = false
    Object.assign(advanceForm, {
      stage: '', operatorUserId: null, reason: '',
      evaluationId: null, conversationId: null, note: '',
    })
    await Promise.all([loadDetail(), loadHistory()])
  } finally {
    advancing.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-id {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  font-family: monospace;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-card {
  margin-bottom: 20px;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.note-block {
  margin-top: 14px;
}

.desc-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  display: block;
  margin-bottom: 4px;
}

.note-block p {
  font-size: 0.9rem;
  color: var(--color-text-regular);
  line-height: 1.6;
  margin: 0;
}

/* Stage Flow */
.stage-flow {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 16px 8px;
  overflow-x: auto;
}

.stage-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-width: 60px;
}

.stage-indicator {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  background: var(--color-bg);
  border: 2px solid var(--color-border);
  color: var(--color-text-muted);
  z-index: 1;
  transition: all 0.2s ease;
}

.stage-node.passed .stage-indicator {
  background: var(--color-success);
  border-color: var(--color-success);
  color: white;
}

.stage-node.active .stage-indicator {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
  box-shadow: 0 0 0 4px var(--color-accent-bg, oklch(0.9 0.05 250));
}

.stage-node.terminal .stage-indicator {
  width: 28px;
  height: 28px;
  font-size: 0.7rem;
}

.stage-name {
  font-size: 0.72rem;
  color: var(--color-text-secondary);
  margin-top: 6px;
  white-space: nowrap;
}

.stage-node.active .stage-name {
  color: var(--color-accent);
  font-weight: 600;
}

.stage-node.passed .stage-name {
  color: var(--color-success);
}

.stage-connector {
  position: absolute;
  top: 16px;
  left: calc(50% + 16px);
  width: calc(100% - 32px);
  height: 2px;
  background: var(--color-border);
}

.stage-node.passed .stage-connector {
  background: var(--color-success);
}

.stage-node.active .stage-connector {
  background: var(--color-accent);
}

.ref-row {
  display: flex;
}

/* History */
.history-item {
  padding: 4px 0;
}

.history-transition {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.history-meta {
  display: flex;
  gap: 16px;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}
</style>
