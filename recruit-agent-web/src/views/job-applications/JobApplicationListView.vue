<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>投递流水线</h2>
        <p class="page-subtitle">跟踪候选人在各招聘阶段的流转情况。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" @click="showCreate = true">
          <el-icon><Plus /></el-icon>新建投递
        </el-button>
      </div>
    </div>

    <!-- Filters -->
    <el-card class="detail-card">
      <div class="filter-row">
        <el-input v-model="filters.jobId" placeholder="岗位 ID" clearable style="width:130px" type="number" @change="loadAll" />
        <el-input v-model="filters.candidateId" placeholder="候选人 ID" clearable style="width:140px" type="number" @change="loadAll" />
        <el-select v-model="filters.stage" clearable placeholder="阶段" style="width:140px" @change="loadAll">
          <el-option v-for="s in stageOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <span class="filter-hint">共 {{ totalCount }} 条</span>
      </div>
    </el-card>

    <!-- Application List -->
    <el-card v-loading="loading" class="detail-card">
      <el-table :data="applications" stripe style="width:100%">
        <el-table-column label="ID" width="70" align="center">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column label="岗位" min-width="120">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/jobs/${row.jobId}`)">
              岗位 #{{ row.jobId }}
            </el-button>
          </template>
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
            <el-tag :type="stageTagType(row.stage)" effect="dark" size="small">
              {{ stageLabel(row.stage) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="80" align="center">
          <template #default="{ row }">{{ row.ownerUserId || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.note || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="router.push(`/job-applications/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!applications.length" description="暂无投递记录" />

      <!-- Pagination -->
      <div class="pagination-wrap" v-if="totalPages > 1">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalCount"
          layout="prev, pager, next"
          @current-change="loadAll"
        />
      </div>
    </el-card>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreate" title="新建投递" width="480px" :close-on-click-modal="false">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="岗位 ID" required>
          <el-input-number v-model="createForm.jobId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="候选人 ID" required>
          <el-input-number v-model="createForm.candidateId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="负责人 ID">
          <el-input-number v-model="createForm.ownerUserId" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="createForm.source" placeholder="例如：手动、内部推荐" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { jobApplicationApi } from '@/api/job-applications'
import type { JobApplicationResponse, JobApplicationStage } from '@/types/job-application'
import type { PageResponse } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const applications = ref<JobApplicationResponse[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize.value)))

const filters = reactive({
  jobId: '' as number | string,
  candidateId: '' as number | string,
  stage: '' as JobApplicationStage | '',
})

const createForm = reactive({
  jobId: 1,
  candidateId: 1,
  ownerUserId: null as number | null,
  source: '',
  note: '',
})

const stageOptions = [
  { value: 'APPLIED', label: '已投递' },
  { value: 'SCREENING', label: '筛选中' },
  { value: 'HR_REVIEW', label: 'HR 审核' },
  { value: 'COMMUNICATION', label: '沟通中' },
  { value: 'INTERVIEW', label: '面试' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'HIRED', label: '已录用' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'WITHDRAWN', label: '已撤回' },
  { value: 'CLOSED', label: '已关闭' },
]

import { computed } from 'vue'

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

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: currentPage.value, size: pageSize.value }
    if (filters.jobId) params.jobId = Number(filters.jobId)
    if (filters.candidateId) params.candidateId = Number(filters.candidateId)
    if (filters.stage) params.stage = filters.stage
    const res = await jobApplicationApi.list(params)
    if (res.data && Array.isArray(res.data)) {
      applications.value = res.data as JobApplicationResponse[]
      totalCount.value = applications.value.length
    } else {
      const paged = res.data as PageResponse<JobApplicationResponse>
      applications.value = paged.items || []
      totalCount.value = paged.total || applications.value.length
    }
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.jobId || !createForm.candidateId) {
    ElMessage.warning('请填写岗位 ID 和候选人 ID')
    return
  }
  creating.value = true
  try {
    await jobApplicationApi.create({
      jobId: createForm.jobId,
      candidateId: createForm.candidateId,
      ownerUserId: createForm.ownerUserId || undefined,
      source: createForm.source || undefined,
      note: createForm.note || undefined,
    })
    ElMessage.success('投递记录已创建')
    showCreate.value = false
    Object.assign(createForm, { jobId: 1, candidateId: 1, ownerUserId: null, source: '', note: '' })
    await loadAll()
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-hint {
  font-size: 0.82rem;
  color: var(--color-text-muted);
  margin-left: auto;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
