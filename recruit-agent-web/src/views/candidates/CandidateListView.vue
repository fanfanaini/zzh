<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>候选人</h2>
        <p class="page-subtitle">维护候选人画像与简历解析状态。</p>
      </div>
      <el-button type="primary" @click="showCreate = true">
        <el-icon><Plus /></el-icon>添加候选人
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" clearable placeholder="搜索姓名、城市、技能" @keyup.enter="fetchCandidates" />
      <el-select v-model="filters.status" clearable placeholder="状态" style="width: 150px">
        <el-option label="活跃" value="ACTIVE" />
        <el-option label="已停用" value="DISABLED" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="fetchCandidates">查询</el-button>
    </div>

    <el-card v-loading="loading" class="table-card">
      <el-table :data="candidates" stripe style="width: 100%" @row-click="goDetail">
        <el-table-column prop="name" label="姓名" width="130">
          <template #default="{ row }">
            <span class="candidate-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="educationLevel" label="学历" width="90" align="center">
          <template #default="{ row }">{{ educationLabel(row.educationLevel) }}</template>
        </el-table-column>
        <el-table-column prop="totalExperienceYears" label="经验" width="90" align="center">
          <template #default="{ row }">{{ row.totalExperienceYears }} 年</template>
        </el-table-column>
        <el-table-column prop="expectedCity" label="期望城市" width="120" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'DISABLED' ? 'info' : 'success'">
              {{ row.status === 'DISABLED' ? '已停用' : '活跃' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="skills" label="技能" min-width="240">
          <template #default="{ row }">
            <el-tag v-for="skill in row.skills" :key="skill" size="small" class="skill-tag">
              {{ skill }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click.stop="openEdit(row)">编辑</el-button>
            <el-button size="small" text @click.stop="disableCandidate(row)" :disabled="row.status === 'DISABLED'">停用</el-button>
            <el-button size="small" text type="danger" @click.stop="deleteCandidate(row)">删除</el-button>
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
          @change="fetchCandidates"
        />
      </div>
      <template #empty>
        <el-empty description="还没有候选人，可先添加候选人或上传简历解析画像。" />
      </template>
    </el-card>

    <el-dialog v-model="showCreate" :title="editingId ? '编辑候选人' : '添加候选人'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="姓名" prop="name" :rules="[{ required: true, message: '请输入候选人姓名', trigger: 'blur' }]">
          <el-input v-model="form.name" placeholder="候选人姓名" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="form.educationLevel" style="width: 100%">
                <el-option label="不限" value="UNKNOWN" />
                <el-option label="大专" value="COLLEGE" />
                <el-option label="本科" value="BACHELOR" />
                <el-option label="硕士" value="MASTER" />
                <el-option label="博士" value="DOCTOR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经验（年）">
              <el-input-number v-model="form.totalExperienceYears" :min="0" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="期望城市">
          <el-input v-model="form.expectedCity" placeholder="例如：成都" />
        </el-form-item>
        <el-form-item label="技能">
          <el-select
            v-model="form.skills"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入技能后回车"
            style="width: 100%"
          >
            <el-option v-for="skill in skillSuggestions" :key="skill" :label="skill" :value="skill" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitCandidate">{{ editingId ? '保存' : '创建' }}</el-button>
          <el-button @click="closeDialog">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { candidateApi } from '@/api/candidates'
import type { PageResponse } from '@/types/api'
import type { Candidate } from '@/types/candidate'
import { educationLabel, formatDate } from '@/utils/format'

const router = useRouter()
const showCreate = ref(false)
const submitting = ref(false)
const loading = ref(false)
const candidates = ref<Candidate[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const filters = reactive({ keyword: '', status: '', page: 1, size: 10 })

const form = reactive({
  name: '',
  educationLevel: 'BACHELOR',
  totalExperienceYears: 2,
  expectedCity: '',
  skills: [] as string[],
})

const skillSuggestions = ['Java', 'Spring Boot', 'MySQL', 'Redis', 'RabbitMQ', 'Kafka', 'Docker', 'Kubernetes', 'Vue 3', 'TypeScript', 'Python', 'Go']

onMounted(fetchCandidates)

async function fetchCandidates() {
  loading.value = true
  try {
    const res = await candidateApi.list({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      page: filters.page,
      size: filters.size,
    })
    const data = res.data as Candidate[] | PageResponse<Candidate>
    candidates.value = Array.isArray(data) ? data : data.items
    total.value = Array.isArray(data) ? data.length : data.total
  } finally {
    loading.value = false
  }
}

function goDetail(row: { id: number }) {
  router.push(`/candidates/${row.id}`)
}

function openEdit(candidate: any) {
  editingId.value = candidate.id
  Object.assign(form, {
    name: candidate.name,
    educationLevel: candidate.educationLevel || 'BACHELOR',
    totalExperienceYears: candidate.totalExperienceYears || 0,
    expectedCity: candidate.expectedCity || '',
    skills: [...(candidate.skills || [])],
  })
  showCreate.value = true
}

function closeDialog() {
  showCreate.value = false
  editingId.value = null
  Object.assign(form, { name: '', educationLevel: 'BACHELOR', totalExperienceYears: 2, expectedCity: '', skills: [] })
}

async function submitCandidate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingId.value) {
      await candidateApi.update(editingId.value, form)
      ElMessage.success('候选人已更新')
    } else {
      await candidateApi.register(form)
      ElMessage.success('候选人添加成功')
    }
    closeDialog()
    await fetchCandidates()
  } finally {
    submitting.value = false
  }
}

async function disableCandidate(candidate: any) {
  await ElMessageBox.confirm(`确认停用候选人 ${candidate.name}？`, '停用候选人', { type: 'warning' })
  await candidateApi.disable(candidate.id)
  ElMessage.success('候选人已停用')
  await fetchCandidates()
}

async function deleteCandidate(candidate: any) {
  await ElMessageBox.confirm(`确认删除候选人 ${candidate.name}？相关简历也可能不可再访问。`, '删除候选人', { type: 'warning' })
  await candidateApi.delete(candidate.id)
  ElMessage.success('候选人已删除')
  await fetchCandidates()
}
</script>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.candidate-name {
  color: var(--color-text-primary);
  font-weight: 500;
}

.skill-tag {
  margin: 2px 4px 2px 0;
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
</style>
