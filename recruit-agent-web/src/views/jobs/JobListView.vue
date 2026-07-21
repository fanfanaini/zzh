<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>岗位管理</h2>
        <p class="page-subtitle">维护 JD、技能画像和筛选入口。</p>
      </div>
      <el-button type="primary" @click="showCreate = true">
        <el-icon><Plus /></el-icon>新建岗位
      </el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">岗位总数</div>
          <div class="stat-value">{{ store.jobs.length }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">必须技能项</div>
          <div class="stat-value">{{ requiredSkillCount }}</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-label">加分技能项</div>
          <div class="stat-value">{{ preferredSkillCount }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索岗位名称、描述或技能" @keyup.enter="fetchJobs" />
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 150px">
        <el-option label="开放" value="OPEN" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="fetchJobs">查询</el-button>
    </div>

    <el-card v-loading="loading" class="table-card">
      <el-table :data="jobs" stripe style="width: 100%" @row-click="goDetail">
        <el-table-column prop="name" label="岗位名称" min-width="180">
          <template #default="{ row }">
            <span class="job-name-link">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="requiredSkills" label="必须技能" min-width="220">
          <template #default="{ row }">
            <el-tag v-for="skill in row.requiredSkills" :key="skill" size="small" class="skill-tag">
              {{ skill }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="preferredSkills" label="加分技能" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="skill in row.preferredSkills"
              :key="skill"
              size="small"
              type="success"
              class="skill-tag"
            >
              {{ skill }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="educationRequirement" label="学历" width="100" align="center">
          <template #default="{ row }">{{ educationLabel(row.educationRequirement) }}</template>
        </el-table-column>
        <el-table-column label="经验" width="120" align="center">
          <template #default="{ row }">{{ row.minExperienceYears }}-{{ row.maxExperienceYears }} 年</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'CLOSED' ? 'info' : 'success'">
              {{ row.status === 'CLOSED' ? '已关闭' : '开放' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150" align="center">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click.stop="closeJob(row)" :disabled="row.status === 'CLOSED'">关闭</el-button>
            <el-button size="small" text type="danger" @click.stop="deleteJob(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.size"
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @change="fetchJobs"
        />
      </div>
      <template #empty>
        <el-empty description="还没有岗位，先创建一个 JD 用于筛选候选人。" />
      </template>
    </el-card>

    <el-dialog v-model="showCreate" title="新建岗位" width="640px" :close-on-click-modal="false">
      <JobForm submit-label="创建岗位" @submit="handleCreate" @cancel="showCreate = false" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { jobApi } from '@/api/jobs'
import { useJobStore } from '@/stores/jobs'
import type { PageResponse } from '@/types/api'
import type { CreateJobRequest, Job } from '@/types/job'
import { educationLabel, formatDate } from '@/utils/format'
import JobForm from './components/JobForm.vue'

const router = useRouter()
const store = useJobStore()
const showCreate = ref(false)
const loading = ref(false)
const jobs = ref<Job[]>([])
const total = ref(0)
const filters = reactive({ keyword: '', status: '', page: 1, size: 10 })

const requiredSkillCount = computed(() =>
  jobs.value.reduce((sum, job) => sum + (job.requiredSkills?.length || 0), 0),
)
const preferredSkillCount = computed(() =>
  jobs.value.reduce((sum, job) => sum + (job.preferredSkills?.length || 0), 0),
)

onMounted(fetchJobs)

async function fetchJobs() {
  loading.value = true
  try {
    const res = await jobApi.list({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page: filters.page,
      size: filters.size,
    })
    const data = res.data as Job[] | PageResponse<Job>
    jobs.value = Array.isArray(data) ? data : data.items
    total.value = Array.isArray(data) ? data.length : data.total
    store.jobs = jobs.value
  } finally {
    loading.value = false
  }
}

function goDetail(row: { id: number }) {
  router.push(`/jobs/${row.id}`)
}

async function handleCreate(data: CreateJobRequest) {
  await jobApi.create(data)
  ElMessage.success('岗位创建成功')
  showCreate.value = false
  await fetchJobs()
}

async function closeJob(job: any) {
  await ElMessageBox.confirm(`确认关闭岗位 ${job.name}？`, '关闭岗位', { type: 'warning' })
  await jobApi.close(job.id)
  ElMessage.success('岗位已关闭')
  await fetchJobs()
}

async function deleteJob(job: any) {
  await ElMessageBox.confirm(`确认删除岗位 ${job.name}？`, '删除岗位', { type: 'warning' })
  await jobApi.delete(job.id)
  ElMessage.success('岗位已删除')
  await fetchJobs()
}
</script>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.stat-row {
  margin-bottom: 20px;
}

.table-card {
  border-radius: var(--radius-md);
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  margin-bottom: 16px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.job-name-link {
  color: var(--color-accent);
  font-weight: 500;
  cursor: pointer;
}

.skill-tag {
  margin: 2px 4px 2px 0;
}
</style>
