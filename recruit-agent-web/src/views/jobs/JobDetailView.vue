<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div class="header-title">
        <el-button text @click="router.push('/jobs')">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <div>
          <h2>{{ job?.name || '岗位详情' }}</h2>
          <p v-if="job" class="page-subtitle">创建于 {{ formatDateTime(job.createdAt) }}</p>
        </div>
      </div>
      <div class="header-actions" v-if="job">
        <el-button :loading="analyzing" @click="handleAnalyze">
          <el-icon><MagicStick /></el-icon>AI 分析 JD
        </el-button>
        <el-button type="primary" @click="editOpen = true">
          <el-icon><Edit /></el-icon>编辑岗位
        </el-button>
      </div>
    </div>

    <el-empty v-if="!job && !loading" description="岗位不存在" />

    <template v-if="job">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card class="detail-card">
            <template #header>
              <span class="section-title">岗位描述</span>
            </template>
            <p class="job-desc">{{ job.description }}</p>
          </el-card>

          <el-card class="detail-card">
            <template #header>
              <span class="section-title">快速筛选</span>
            </template>
            <el-form :model="screenForm" label-width="96px">
              <el-form-item label="候选人">
                <el-select
                  v-model="screenForm.candidateId"
                  filterable
                  placeholder="选择候选人进行单人匹配"
                  style="width: 100%"
                >
                  <el-option
                    v-for="candidate in candidates"
                    :key="candidate.id"
                    :label="`${candidate.name}（${candidate.totalExperienceYears} 年 / ${educationLabel(candidate.educationLevel)}）`"
                    :value="candidate.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="screening" @click="handleScreen">开始匹配</el-button>
                <el-button @click="screenForm.candidateId = null">清除</el-button>
              </el-form-item>
            </el-form>
            <ScreeningResultCard v-if="screenResult" :result="screenResult" />
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card class="detail-card">
            <template #header>
              <span class="section-title">岗位画像</span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="学历要求">{{ educationLabel(job.educationRequirement) }}</el-descriptions-item>
              <el-descriptions-item label="经验要求">
                {{ job.minExperienceYears }}-{{ job.maxExperienceYears }} 年
              </el-descriptions-item>
              <el-descriptions-item label="必须技能">
                <el-tag v-for="skill in job.requiredSkills" :key="skill" size="small" class="skill-tag">
                  {{ skill }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="加分技能">
                <el-tag v-for="skill in job.preferredSkills" :key="skill" size="small" type="success" class="skill-tag">
                  {{ skill }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="detail-card">
            <template #header>
              <span class="section-title">筛选工作流</span>
            </template>
            <div class="action-group">
              <el-button type="primary" @click="router.push({ path: '/screening', query: { jobId: job.id } })">
                <el-icon><DataAnalysis /></el-icon>查看匹配排行
              </el-button>
              <el-button :loading="indexing" @click="handleIndexJob">
                <el-icon><Search /></el-icon>索引岗位向量
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Job Applications Section -->
      <el-card class="detail-card">
        <template #header>
          <span class="section-title">投递记录</span>
          <el-button size="small" text type="primary" @click="showCreateApp = true">
            <el-icon><Plus /></el-icon>新建投递
          </el-button>
        </template>
        <el-table :data="jobApplications" stripe v-loading="appLoading" style="width:100%">
          <el-table-column label="ID" width="70" align="center">
            <template #default="{ row }">#{{ row.id }}</template>
          </el-table-column>
          <el-table-column label="候选人" min-width="120">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="router.push(`/candidates/${row.candidateId}`)">
                候选人 #{{ row.candidateId }}
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
    </template>

    <el-dialog v-model="editOpen" title="编辑岗位" width="640px" :close-on-click-modal="false">
      <JobForm :initial-value="job" submit-label="保存修改" @submit="handleUpdate" @cancel="editOpen = false" />
    </el-dialog>

    <!-- Create Application Dialog -->
    <el-dialog v-model="showCreateApp" title="新建投递" width="460px" :close-on-click-modal="false">
      <el-form :model="createAppForm" label-position="top">
        <el-form-item label="候选人 ID" required>
          <el-input-number v-model="createAppForm.candidateId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="负责人 ID">
          <el-input-number v-model="createAppForm.ownerUserId" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="createAppForm.source" placeholder="例如：手动、内部推荐" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createAppForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateApp = false">取消</el-button>
        <el-button type="primary" @click="handleCreateApp">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, DataAnalysis, Edit, MagicStick, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { jobApi } from '@/api/jobs'
import { screeningApi } from '@/api/screening'
import { vectorApi } from '@/api/vector'
import { jobApplicationApi } from '@/api/job-applications'
import type { JobApplicationResponse, JobApplicationStage } from '@/types/job-application'
import ScreeningResultCard from '@/components/ScreeningResultCard.vue'
import { useCandidateStore } from '@/stores/candidates'
import { useJobStore } from '@/stores/jobs'
import type { Candidate } from '@/types/candidate'
import type { CreateJobRequest, Job } from '@/types/job'
import type { ScreeningResult } from '@/types/screening'
import { educationLabel, formatDateTime } from '@/utils/format'
import JobForm from './components/JobForm.vue'

const route = useRoute()
const router = useRouter()
const jobStore = useJobStore()
const candidateStore = useCandidateStore()

const job = ref<Job | null>(null)
const candidates = ref<Candidate[]>([])
const loading = ref(true)
const editOpen = ref(false)
const analyzing = ref(false)
const indexing = ref(false)
const screening = ref(false)
const screenResult = ref<ScreeningResult | null>(null)
const screenForm = reactive({ candidateId: null as number | null })
const appLoading = ref(false)
const jobApplications = ref<JobApplicationResponse[]>([])
const showCreateApp = ref(false)
const createAppForm = reactive({
  candidateId: null as number | null,
  ownerUserId: null as number | null,
  source: '',
  note: '',
})

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
  if (!job.value) return
  appLoading.value = true
  try {
    const res = await jobApplicationApi.list({ jobId: job.value.id })
    if (res.data && Array.isArray(res.data)) {
      jobApplications.value = res.data as JobApplicationResponse[]
    }
  } finally {
    appLoading.value = false
  }
}

async function handleCreateApp() {
  if (!job.value || !createAppForm.candidateId) {
    ElMessage.warning('请选择候选人')
    return
  }
  await jobApplicationApi.create({
    jobId: job.value.id,
    candidateId: createAppForm.candidateId,
    ownerUserId: createAppForm.ownerUserId || undefined,
    source: createAppForm.source || undefined,
    note: createAppForm.note || undefined,
  })
  ElMessage.success('投递记录已创建')
  showCreateApp.value = false
  Object.assign(createAppForm, { candidateId: null, ownerUserId: null, source: '', note: '' })
  await loadJobApplications()
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    job.value = await jobStore.fetchJob(id)
    await candidateStore.fetchCandidates()
    candidates.value = candidateStore.candidates
    loadJobApplications()
  } finally {
    loading.value = false
  }
})

async function handleUpdate(data: CreateJobRequest) {
  if (!job.value) return
  const res = await jobApi.update(job.value.id, data)
  job.value = res.data
  editOpen.value = false
  ElMessage.success('岗位已更新')
  loadJobApplications()
}

async function handleAnalyze() {
  if (!job.value) return
  analyzing.value = true
  try {
    const res = await jobApi.analyze(job.value.id)
    job.value = res.data
    ElMessage.success('AI 已更新岗位画像')
  } finally {
    analyzing.value = false
  }
}

async function handleScreen() {
  if (!job.value || !screenForm.candidateId) {
    ElMessage.warning('请选择候选人')
    return
  }
  screening.value = true
  try {
    const res = await screeningApi.screen(job.value.id, { candidateId: screenForm.candidateId })
    screenResult.value = res.data
    ElMessage.success('匹配完成')
  } finally {
    screening.value = false
  }
}

async function handleIndexJob() {
  if (!job.value) return
  indexing.value = true
  try {
    await vectorApi.indexJobDescription(job.value.id, job.value.description)
    ElMessage.success('岗位描述已加入向量库')
  } finally {
    indexing.value = false
  }
}
</script>

<style scoped>
.header-title,
.header-actions,
.action-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.detail-card {
  margin-bottom: 20px;
}

.job-desc {
  white-space: pre-wrap;
  color: var(--color-text-regular);
  line-height: 1.7;
}

.skill-tag {
  margin: 2px 4px 2px 0;
}

.action-group {
  flex-direction: column;
  align-items: stretch;
}
</style>
