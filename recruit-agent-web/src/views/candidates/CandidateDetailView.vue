<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="router.push('/candidates')">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2>候选人详情</h2>
        <span v-if="candidate" class="candidate-name-title">{{ candidate.name }}</span>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" @click="router.push(`/candidates/${candidateId}/resumes`)">
          <el-icon><Upload /></el-icon>简历管理
        </el-button>
      </div>
    </div>

    <div v-loading="loading" v-if="candidate">
      <!-- Candidate Info Card -->
      <el-card class="detail-card">
        <template #header><span class="section-title">基本信息</span></template>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="姓名">{{ candidate.name }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ educationLabel(candidate.educationLevel) }}</el-descriptions-item>
          <el-descriptions-item label="经验">{{ candidate.totalExperienceYears }} 年</el-descriptions-item>
          <el-descriptions-item label="期望城市">{{ candidate.expectedCity || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="candidate.status === 'DISABLED' ? 'info' : 'success'">
              {{ candidate.status === 'DISABLED' ? '已停用' : '活跃' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(candidate.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <div class="skills-row" v-if="candidate.skills?.length">
          <span class="skills-label">技能：</span>
          <el-tag v-for="s in candidate.skills" :key="s" size="small" class="skill-tag">{{ s }}</el-tag>
        </div>
      </el-card>

      <!-- Resume List Card -->
      <el-card class="detail-card">
        <template #header><span class="section-title">简历文件</span></template>
        <el-table :data="resumes" stripe style="width:100%">
          <el-table-column prop="originalFilename" label="文件名" min-width="200" />
          <el-table-column prop="fileSize" label="大小" width="100">
            <template #default="{ row }">{{ (row.fileSize / 1024).toFixed(1) }} KB</template>
          </el-table-column>
          <el-table-column prop="parseStatus" label="解析状态" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.parseStatus === 'PARSED' ? 'success' : row.parseStatus === 'FAILED' ? 'danger' : 'warning'">
                {{ statusLabel(row.parseStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="uploadedAt" label="上传时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.uploadedAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!resumes.length" description="暂无简历文件" />
      </el-card>

      <!-- Long-term Memory Card -->
      <el-card class="detail-card">
        <template #header>
          <span class="section-title">长期记忆</span>
          <el-tag size="small" class="section-tag">AI 自动提取 + 人工维护</el-tag>
          <el-button size="small" text type="primary" @click="showAddMemory = true">添加记忆</el-button>
          <el-button size="small" text @click="showInactive = !showInactive">
            {{ showInactive ? '隐藏' : '显示' }}已停用
          </el-button>
        </template>

        <div v-if="memories.length === 0" class="empty-hint">
          暂无长期记忆。AI 会在沟通中自动提取候选人的偏好、状态等信息。
        </div>

        <div v-for="m in filteredMemories" :key="m.id" :class="['memory-item', { inactive: !m.active }]">
          <div class="memory-header">
            <el-tag size="small" :type="memoryTypeTag(m.type)">{{ memoryTypeLabel(m.type) }}</el-tag>
            <span class="memory-key">{{ m.factKey }}</span>
            <el-tag size="small" :type="m.sourceType === 'MANUAL' ? 'primary' : 'info'" class="source-tag">
              {{ sourceTypeLabel(m.sourceType) }}
            </el-tag>
            <span class="memory-confidence" v-if="m.confidence > 0">
              置信度: {{ (m.confidence * 100).toFixed(0) }}%
            </span>
            <span class="memory-time">{{ formatDateTime(m.updatedAt) }}</span>
            <el-button
              v-if="m.active"
              size="small"
              text
              type="warning"
              @click="handleDeactivate(m)"
            >停用</el-button>
            <el-tag v-else size="small" type="info">已停用</el-tag>
          </div>
          <div class="memory-value">{{ m.factValue }}</div>
          <div class="memory-evidence" v-if="m.evidence">
            <span class="evidence-label">证据：</span>{{ m.evidence }}
          </div>
        </div>
      </el-card>

      <!-- Job Applications Card -->
      <el-card class="detail-card">
        <template #header><span class="section-title">投递记录</span></template>
        <el-table :data="jobApplications" stripe v-loading="appLoading" style="width:100%">
          <el-table-column label="ID" width="70" align="center">
            <template #default="{ row }">#{{ row.id }}</template>
          </el-table-column>
          <el-table-column label="岗位" min-width="100">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="router.push(`/jobs/${row.jobId}`)">
                岗位 #{{ row.jobId }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="当前阶段" width="140">
            <template #default="{ row }">
              <el-tag :type="appStageTagType(row.stage)" effect="dark" size="small">
                {{ appStageLabel(row.stage) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="router.push(`/job-applications/${row.id}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!jobApplications.length" description="暂无投递记录" />
      </el-card>
    </div>
    <el-empty v-else-if="!loading" description="候选人不存在" />

    <!-- Add Memory Dialog -->
    <el-dialog v-model="showAddMemory" title="添加长期记忆" width="560px" :close-on-click-modal="false">
      <el-form :model="memoryForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="memoryForm.type" style="width:100%">
                <el-option v-for="opt in memoryTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="置信度">
              <el-slider v-model="memoryForm.confidence" :min="0" :max="1" :step="0.05" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="事实 Key">
          <el-input v-model="memoryForm.factKey" placeholder="例如：expected_salary" />
        </el-form-item>
        <el-form-item label="事实值">
          <el-input v-model="memoryForm.factValue" type="textarea" :rows="3" placeholder="例如：期望薪资 25K-30K" />
        </el-form-item>
        <el-form-item label="证据（可选）">
          <el-input v-model="memoryForm.evidence" type="textarea" :rows="2" placeholder="记录来源或说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMemory = false">取消</el-button>
        <el-button type="primary" :loading="addingMemory" @click="handleAddMemory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { candidateApi, resumeApi } from '@/api/candidates'
import { memoryApi } from '@/api/memories'
import { jobApplicationApi } from '@/api/job-applications'
import type { Candidate } from '@/types/candidate'
import type { ResumeDocument } from '@/types/candidate'
import type { CandidateMemory, CandidateMemoryType, CandidateMemorySourceType, CreateCandidateMemoryRequest } from '@/types/memory'
import type { JobApplicationResponse, JobApplicationStage } from '@/types/job-application'
import { educationLabel, formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const candidateId = Number(route.params.id)

const loading = ref(false)
const candidate = ref<Candidate | null>(null)
const resumes = ref<ResumeDocument[]>([])
const memories = ref<CandidateMemory[]>([])
const showInactive = ref(false)
const showAddMemory = ref(false)
const addingMemory = ref(false)
const appLoading = ref(false)
const jobApplications = ref<JobApplicationResponse[]>([])

function appStageLabel(stage: JobApplicationStage): string {
  const map: Record<string, string> = {
    APPLIED: '已投递', SCREENING: '筛选中', HR_REVIEW: 'HR 审核',
    COMMUNICATION: '沟通中', INTERVIEW: '面试', OFFER: 'Offer',
    HIRED: '已录用', REJECTED: '已拒绝', WITHDRAWN: '已撤回', CLOSED: '已关闭',
  }
  return map[stage] || stage
}

function appStageTagType(stage: JobApplicationStage): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    APPLIED: 'info', SCREENING: 'primary', HR_REVIEW: 'warning',
    COMMUNICATION: 'primary', INTERVIEW: 'warning', OFFER: 'success',
    HIRED: 'success', REJECTED: 'danger', WITHDRAWN: 'info', CLOSED: 'info',
  }
  return map[stage] || 'info'
}

async function loadJobApplications() {
  appLoading.value = true
  try {
    const res = await jobApplicationApi.list({ candidateId })
    if (res.data && Array.isArray(res.data)) {
      jobApplications.value = res.data as JobApplicationResponse[]
    }
  } finally {
    appLoading.value = false
  }
}


const memoryForm = reactive({
  type: 'PREFERENCE' as CandidateMemoryType,
  factKey: '',
  factValue: '',
  confidence: 0.8,
  evidence: '',
})

const memoryTypeOptions = [
  { value: 'PREFERENCE', label: '偏好' },
  { value: 'STATUS', label: '状态' },
  { value: 'RISK', label: '风险' },
  { value: 'NOTE', label: '备注' },
  { value: 'FACT', label: '事实' },
]

const filteredMemories = computed(() => {
  if (showInactive.value) return memories.value
  return memories.value.filter((m) => m.active)
})

onMounted(loadAll)

function statusLabel(status: string) {
  const map: Record<string, string> = { UPLOADED: '已上传', PARSING: '解析中', PARSED: '已解析', FAILED: '失败' }
  return map[status] || status
}

function memoryTypeTag(t: CandidateMemoryType) {
  const map: Record<string, 'success' | 'primary' | 'danger' | 'warning' | 'info'> = {
    PREFERENCE: 'success', STATUS: 'primary', RISK: 'danger', NOTE: 'warning', FACT: 'info',
  }
  return map[t] || 'info'
}

function memoryTypeLabel(t: CandidateMemoryType) {
  const map: Record<string, string> = { PREFERENCE: '偏好', STATUS: '状态', RISK: '风险', NOTE: '备注', FACT: '事实' }
  return map[t] || t
}

function sourceTypeLabel(t: CandidateMemorySourceType) {
  const map: Record<string, string> = {
    CONVERSATION_MESSAGE: 'AI对话', REPLY_APPROVAL: 'AI回复', MANUAL: '人工录入', RESUME: '简历', SYSTEM: '系统',
  }
  return map[t] || t
}

async function loadAll() {
  loading.value = true
  try {
    const [candidateRes, resumeRes, memoryRes] = await Promise.all([
      candidateApi.get(candidateId),
      candidateApi.listResumes(candidateId),
      memoryApi.list(candidateId, true),
    ])
    candidate.value = candidateRes.data
    resumes.value = resumeRes.data
    memories.value = memoryRes.data
    loadJobApplications()
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleDeactivate(memory: CandidateMemory) {
  await ElMessageBox.confirm(`确认停用这条记忆？\n${memory.factKey}: ${memory.factValue}`, '停用记忆', { type: 'warning' })
  await memoryApi.deactivate(candidateId, memory.id)
  ElMessage.success('记忆已停用')
  const res = await memoryApi.list(candidateId, true)
  memories.value = res.data
}

async function handleAddMemory() {
  if (!memoryForm.factKey.trim() || !memoryForm.factValue.trim()) {
    ElMessage.warning('请填写事实 Key 和值')
    return
  }
  addingMemory.value = true
  try {
    const data: CreateCandidateMemoryRequest = {
      type: memoryForm.type,
      factKey: memoryForm.factKey.trim(),
      factValue: memoryForm.factValue.trim(),
      confidence: memoryForm.confidence,
      evidence: memoryForm.evidence.trim() || undefined,
    }
    await memoryApi.create(candidateId, data)
    ElMessage.success('记忆已添加')
    showAddMemory.value = false
    Object.assign(memoryForm, { type: 'PREFERENCE', factKey: '', factValue: '', confidence: 0.8, evidence: '' })
    const res = await memoryApi.list(candidateId, true)
    memories.value = res.data
  } finally {
    addingMemory.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.candidate-name-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-accent);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-card {
  margin-bottom: 20px;
}

.section-tag {
  margin-left: 8px;
  vertical-align: middle;
}

.skills-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.skills-label {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.skill-tag {
  margin: 2px 0;
}

.memory-item {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
  background: var(--color-surface);

  &.inactive {
    opacity: 0.55;
  }
}

.memory-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.memory-key {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-text-primary);
}

.source-tag {
  font-size: 0.7rem;
}

.memory-confidence {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.memory-time {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-left: auto;
}

.memory-value {
  font-size: 0.9rem;
  color: var(--color-text-regular);
  line-height: 1.6;
  white-space: pre-wrap;
  padding: 6px 0;
}

.memory-evidence {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  padding: 6px 8px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
}

.evidence-label {
  font-weight: 500;
  color: var(--color-text-secondary);
}

.empty-hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px 0;
}
</style>
