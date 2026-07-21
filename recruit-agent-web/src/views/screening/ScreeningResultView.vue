<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>匹配排行</h2>
        <p class="page-subtitle">基于后端评估记录展示岗位候选人排序。</p>
      </div>
      <el-select v-model="selectedJobId" placeholder="选择岗位" @change="handleJobChange" style="width: 280px">
        <el-option v-for="job in jobStore.jobs" :key="job.id" :label="job.name" :value="job.id" />
      </el-select>
    </div>

    <template v-if="selectedJobId">
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">候选人总数</div>
            <div class="stat-value">{{ scopedCandidates.length }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">已有评估</div>
            <div class="stat-value">{{ rankings.length }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">推荐面试</div>
            <div class="stat-value success">{{ countByRecommendation('INTERVIEW') }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">待人工判断</div>
            <div class="stat-value warning">{{ countByRecommendation('MANUAL_REVIEW') }}</div>
          </div>
        </el-col>
      </el-row>

      <div class="action-bar">
        <el-input v-model="rankingFilters.keyword" clearable placeholder="关键词" style="width: 180px" @keyup.enter="reloadRankings" />
        <el-select v-model="rankingFilters.recommendation" clearable placeholder="推荐结果" style="width: 150px" @change="reloadRankings">
          <el-option label="推荐面试" value="INTERVIEW" />
          <el-option label="人工判断" value="MANUAL_REVIEW" />
          <el-option label="不匹配" value="NOT_MATCH" />
        </el-select>
        <el-button type="primary" :loading="batchSubmitting" @click="batchScreenAll">
          <el-icon><DataAnalysis /></el-icon>提交后端批量筛选
        </el-button>
        <el-button :loading="loadingRankings" @click="loadRankings">刷新排名</el-button>
        <el-tag v-if="focusedCandidateId" type="info" closable @close="clearCandidateFocus">
          仅看候选人 #{{ focusedCandidateId }}
        </el-tag>
        <span v-if="latestTaskId" class="batch-hint">
          已创建任务 #{{ latestTaskId }}，可到 Agent 任务页查看进度。
        </span>
      </div>

      <el-card class="table-card" v-loading="loadingRankings">
        <el-table :data="tableData" stripe style="width: 100%" :default-sort="{ prop: 'finalScore', order: 'descending' }">
          <el-table-column label="排名" width="64" align="center">
            <template #default="{ $index }">
              <span class="rank-num">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="candidateName" label="候选人" min-width="130">
            <template #default="{ row }">
              <span class="candidate-link" @click="router.push(`/candidates/${row.candidateId}/resumes`)">
                {{ row.candidateName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="education" label="学历" width="90" align="center" />
          <el-table-column prop="experience" label="经验" width="90" align="center">
            <template #default="{ row }">{{ row.experience }} 年</template>
          </el-table-column>
          <el-table-column prop="skills" label="技能" min-width="220">
            <template #default="{ row }">
              <el-tag v-for="skill in row.skills" :key="skill" size="small" class="skill-tag">
                {{ skill }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="finalScore" label="综合评分" width="120" align="center" sortable>
            <template #default="{ row }">
              <span v-if="row.finalScore != null" :class="['score-gauge', scoreLevel(row.finalScore)]">
                {{ Math.round(row.finalScore) }}
              </span>
              <span v-else class="pending-tag">待匹配</span>
            </template>
          </el-table-column>
          <el-table-column label="推荐" width="130" align="center">
            <template #default="{ row }">
              <span v-if="row.recommendation" :class="['badge-rec', recommendationClass(row.recommendation)]">
                {{ recommendationLabel(row.recommendation) }}
              </span>
              <span v-else class="pending-tag">-</span>
            </template>
          </el-table-column>
          <el-table-column label="人工复核" width="110" align="center">
            <template #default="{ row }">
              {{ row.humanResult || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.evaluationId" size="small" type="primary" plain :loading="screeningId === row.candidateId" @click="screenOne(row.candidateId)">
                单人匹配
              </el-button>
              <template v-else>
                <el-button size="small" type="primary" @click="router.push(`/evaluations/${row.evaluationId}`)">
                  详情
                </el-button>
                <el-button size="small" @click="startConversation(row)">
                  沟通
                </el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-row">
          <el-pagination
            v-model:current-page="rankingFilters.page"
            v-model:page-size="rankingFilters.size"
            layout="total, sizes, prev, pager, next"
            :total="rankingTotal"
            :page-sizes="[10, 20, 50]"
            @change="loadRankings"
          />
        </div>
      </el-card>
    </template>

    <el-empty v-else description="请先选择一个岗位进行匹配。" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { conversationApi } from '@/api/conversations'
import { screeningApi } from '@/api/screening'
import { useCandidateStore } from '@/stores/candidates'
import { useJobStore } from '@/stores/jobs'
import type { Candidate } from '@/types/candidate'
import type { PageResponse } from '@/types/api'
import type { CandidateRanking, ScreeningRecommendation } from '@/types/screening'
import { educationLabel, recommendationClass, recommendationLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const jobStore = useJobStore()
const candidateStore = useCandidateStore()

const selectedJobId = ref<number | null>(null)
const focusedCandidateId = ref<number | null>(null)
const rankings = ref<CandidateRanking[]>([])
const loadingRankings = ref(false)
const batchSubmitting = ref(false)
const screeningId = ref<number | null>(null)
const latestTaskId = ref<number | null>(null)
const rankingTotal = ref(0)
const rankingFilters = reactive({ keyword: '', recommendation: '', page: 1, size: 10 })
const candidates = computed(() => candidateStore.candidates)
const scopedCandidates = computed(() => {
  if (!focusedCandidateId.value) return candidates.value
  return candidates.value.filter((candidate) => candidate.id === focusedCandidateId.value)
})

const rankingMap = computed(() => new Map(rankings.value.map((ranking) => [ranking.candidateId, ranking])))
const candidateMap = computed(() => new Map(candidates.value.map((candidate) => [candidate.id, candidate])))
const hasRankingFilter = computed(() => !!rankingFilters.keyword || !!rankingFilters.recommendation || !!focusedCandidateId.value)
const tableData = computed(() => {
  if (hasRankingFilter.value) {
    return rankings.value.map((ranking) => {
      const candidate = candidateMap.value.get(ranking.candidateId)
      return {
        candidateId: ranking.candidateId,
        candidateName: candidate?.name || `候选人 #${ranking.candidateId}`,
        skills: candidate?.skills || [],
        education: candidate ? educationLabel(candidate.educationLevel) : '-',
        experience: candidate?.totalExperienceYears ?? 0,
        evaluationId: ranking.evaluationId,
        finalScore: ranking.finalScore,
        recommendation: ranking.recommendation,
        humanResult: ranking.humanResult || null,
      }
    })
  }
  return scopedCandidates.value.map((candidate: Candidate) => {
    const ranking = rankingMap.value.get(candidate.id)
    return {
      candidateId: candidate.id,
      candidateName: candidate.name,
      skills: candidate.skills,
      education: educationLabel(candidate.educationLevel),
      experience: candidate.totalExperienceYears,
      evaluationId: ranking?.evaluationId || null,
      finalScore: ranking?.finalScore ?? null,
      recommendation: ranking?.recommendation || null,
      humanResult: ranking?.humanResult || null,
    }
  }).sort((left, right) => {
    if (focusedCandidateId.value) {
      if (left.candidateId === focusedCandidateId.value) return -1
      if (right.candidateId === focusedCandidateId.value) return 1
    }
    return (right.finalScore ?? -1) - (left.finalScore ?? -1)
  })
})

onMounted(async () => {
  await jobStore.fetchJobs()
  await candidateStore.fetchCandidates()
  const queryJobId = Number(route.query.jobId)
  const queryCandidateId = Number(route.query.candidateId)
  focusedCandidateId.value = queryCandidateId || null
  selectedJobId.value = queryJobId || jobStore.jobs[0]?.id || null
  await loadRankings()
})

async function loadRankings() {
  if (!selectedJobId.value) return
  loadingRankings.value = true
  try {
    const res = await screeningApi.rankings(selectedJobId.value, {
      candidateId: focusedCandidateId.value || undefined,
      keyword: rankingFilters.keyword || undefined,
      recommendation: rankingFilters.recommendation || undefined,
      page: rankingFilters.page,
      size: rankingFilters.size,
    })
    const data = res.data as CandidateRanking[] | PageResponse<CandidateRanking>
    rankings.value = Array.isArray(data) ? data : data.items
    rankingTotal.value = Array.isArray(data) ? data.length : data.total
  } finally {
    loadingRankings.value = false
  }
}

function handleJobChange() {
  latestTaskId.value = null
  rankingFilters.page = 1
  loadRankings()
}

function reloadRankings() {
  rankingFilters.page = 1
  loadRankings()
}

function countByRecommendation(recommendation: ScreeningRecommendation) {
  return rankings.value.filter((ranking) => ranking.recommendation === recommendation).length
}

async function screenOne(candidateId: number) {
  if (!selectedJobId.value) return
  screeningId.value = candidateId
  try {
    await screeningApi.screen(selectedJobId.value, { candidateId })
    await loadRankings()
    ElMessage.success('单人匹配完成')
  } finally {
    screeningId.value = null
  }
}

async function batchScreenAll() {
  if (!selectedJobId.value) return
  if (!candidates.value.length) {
    ElMessage.warning('暂无候选人可匹配')
    return
  }
  const targets = scopedCandidates.value
  if (!targets.length) {
    ElMessage.warning('当前候选人不存在或未加载')
    return
  }
  batchSubmitting.value = true
  try {
    const key = `batch-screen-${selectedJobId.value}-${Date.now()}`
    const res = await screeningApi.batchScreen(selectedJobId.value, {
      candidateIds: targets.map((candidate) => candidate.id),
    }, key)
    latestTaskId.value = res.data.id
    ElMessage.success('批量筛选任务已提交')
  } finally {
    batchSubmitting.value = false
  }
}

function clearCandidateFocus() {
  focusedCandidateId.value = null
  rankingFilters.page = 1
  router.replace({ path: '/screening', query: selectedJobId.value ? { jobId: selectedJobId.value } : {} })
  loadRankings()
}

async function startConversation(row: any) {
  if (!selectedJobId.value) return
  const res = await conversationApi.create({
    jobId: selectedJobId.value,
    candidateId: row.candidateId,
  })
  ElMessage.success('已发起沟通会话')
  router.push(`/conversations/${res.data.id}`)
}

function scoreLevel(score: number) {
  if (score >= 80) return 'high'
  if (score >= 60) return 'mid'
  return 'low'
}
</script>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.stat-row,
.action-bar {
  margin-bottom: 20px;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-value.success {
  color: var(--color-success);
}

.stat-value.warning {
  color: var(--color-warning);
}

.table-card {
  border-radius: var(--radius-md);
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.candidate-link {
  color: var(--color-accent);
  font-weight: 500;
  cursor: pointer;
}

.skill-tag {
  margin: 2px 4px 2px 0;
}

.rank-num {
  font-weight: 700;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.pending-tag,
.batch-hint {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
