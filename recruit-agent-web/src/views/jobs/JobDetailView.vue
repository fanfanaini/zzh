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
    </template>

    <el-dialog v-model="editOpen" title="编辑岗位" width="640px" :close-on-click-modal="false">
      <JobForm :initial-value="job" submit-label="保存修改" @submit="handleUpdate" @cancel="editOpen = false" />
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

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    job.value = await jobStore.fetchJob(id)
    await candidateStore.fetchCandidates()
    candidates.value = candidateStore.candidates
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
