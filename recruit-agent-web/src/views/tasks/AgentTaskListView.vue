<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Agent 任务</h2>
      <div>
        <el-button :loading="loading" @click="fetchTasks">刷新</el-button>
        <el-button type="primary" @click="showCreate = true">
          <el-icon><Plus /></el-icon>新建任务
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">全部任务</div>
          <div class="stat-value">{{ tasks.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">进行中</div>
          <div class="stat-value" style="color:var(--color-warning)">{{ countByStatus('RUNNING') + countByStatus('PENDING') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">已完成</div>
          <div class="stat-value" style="color:var(--color-success)">{{ countByStatus('SUCCEEDED') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">失败</div>
          <div class="stat-value" style="color:var(--color-danger)">{{ countByStatus('FAILED') }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 任务列表 -->
    <el-card class="table-card" v-loading="loading">
      <el-table :data="tasks" stripe style="width: 100%">
        <el-table-column label="ID" width="80" align="center">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column label="任务类型" width="140">
          <template #default="{ row }">
            <el-tag :type="taskTypeTag(row.taskType)" size="small">{{ taskTypeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span :class="['status-tag', row.status.toLowerCase()]">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="总数" width="60" align="center">
          <template #default="{ row }">{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="成功" width="60" align="center">
          <template #default="{ row }">{{ row.successCount }}</template>
        </el-table-column>
        <el-table-column label="失败" width="60" align="center">
          <template #default="{ row }">{{ row.failureCount }}</template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress
              :percentage="row.totalCount > 0 ? Math.round((row.successCount + row.failureCount) / row.totalCount * 100) : 0"
              :status="row.status === 'FAILED' ? 'exception' : row.status === 'SUCCEEDED' ? 'success' : undefined"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150" align="center">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewSteps(row)">步骤</el-button>
            <el-button size="small" text type="warning" @click="handleRetry(row)" :disabled="row.status !== 'FAILED'">重试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 步骤详情抽屉 -->
    <el-drawer v-model="stepsDrawer" :title="`任务步骤 — #${currentTask?.id}`" size="400px">
      <template v-if="steps.length === 0">
        <el-empty description="暂无步骤记录" />
      </template>
      <el-timeline v-else>
        <el-timeline-item
          v-for="step in steps"
          :key="step.id"
          :timestamp="formatTime(step.createdAt)"
          :type="step.status === 'SUCCEEDED' ? 'success' : step.status === 'FAILED' ? 'danger' : 'primary'"
        >
          <h4>{{ step.stepName }}</h4>
          <p v-if="step.message">{{ step.message }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>

    <!-- 新建任务弹窗 -->
    <el-dialog v-model="showCreate" title="新建 Agent 任务" width="450px" :close-on-click-modal="false">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="任务类型">
          <el-select v-model="createForm.taskType" style="width:100%">
            <el-option label="简历解析" value="RESUME_PARSE" />
            <el-option label="回复生成" value="REPLY_GENERATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标 ID">
          <el-input-number v-model="createForm.targetId" :min="0" style="width:100%" placeholder="关联的岗位或候选人 ID" />
        </el-form-item>
        <el-form-item label="总数量">
          <el-input-number v-model="createForm.totalCount" :min="1" :max="10000" style="width:100%" />
        </el-form-item>
        <p class="create-hint">筛选任务请从匹配排行页提交，那里会带上岗位和候选人参数。</p>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
          <el-button @click="showCreate = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { taskApi } from '@/api/tasks'
import type { AgentTask, AgentStep } from '@/types/task'

const tasks = ref<AgentTask[]>([])
const steps = ref<AgentStep[]>([])
const currentTask = ref<AgentTask | null>(null)
const loading = ref(false)
const creating = ref(false)
const showCreate = ref(false)
const stepsDrawer = ref(false)

const createForm = ref({ taskType: 'RESUME_PARSE', targetId: 0, totalCount: 1 })

let refreshTimer: number | undefined

onMounted(() => {
  fetchTasks()
  refreshTimer = window.setInterval(fetchTasks, 5000)
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})

async function fetchTasks() {
  loading.value = true
  try {
    tasks.value = (await taskApi.list()).data
  } finally {
    loading.value = false
  }
}

function countByStatus(status: string) {
  return tasks.value.filter((t) => t.status === status).length
}

function taskTypeLabel(type: string) {
  const map: Record<string, string> = { RESUME_PARSE: '简历解析', SCREENING: '简历筛选', REPLY_GENERATION: '回复生成', BATCH_SCREENING: '批量筛选' }
  return map[type] || type
}

function taskTypeTag(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' | undefined {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info'> = {
    RESUME_PARSE: 'info', SCREENING: 'primary', REPLY_GENERATION: 'success', BATCH_SCREENING: 'warning',
  }
  return map[type]
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待执行', RUNNING: '执行中', SUCCEEDED: '已完成', FAILED: '失败' }
  return map[s] || s
}

function formatTime(t: string) {
  return t ? new Date(t).toLocaleString('zh-CN') : '—'
}

async function viewSteps(task: any) {
  stepsDrawer.value = true
  try {
    const [taskResult, stepsResult] = await Promise.all([
      taskApi.get(task.id),
      taskApi.listSteps(task.id),
    ])
    currentTask.value = taskResult.data
    steps.value = stepsResult.data as unknown as AgentStep[]
  } catch {
    currentTask.value = task
    steps.value = []
  }
}

async function handleRetry(task: any) {
  try {
    await taskApi.retry(task.id)
    ElMessage.success('任务已重新调度')
    await fetchTasks()
  } catch {
    ElMessage.error('重试失败')
  }
}

async function handleCreate() {
  creating.value = true
  try {
    await taskApi.create({ taskType: createForm.value.taskType as any, targetId: createForm.value.targetId || undefined, totalCount: createForm.value.totalCount })
    ElMessage.success('任务创建成功')
    showCreate.value = false
    await fetchTasks()
  } catch {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.stat-row {
  margin-bottom: 20px;
}

.table-card {
  border-radius: var(--radius-md);
}

.create-hint {
  margin: -4px 0 14px;
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.status-tag {
  font-size: 0.8rem;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 100px;
  &.pending { background: var(--color-bg-deep); color: var(--color-text-secondary); }
  &.running { background: oklch(94% 0.04 220); color: oklch(40% 0.12 220); }
  &.succeeded { background: var(--color-success-bg); color: oklch(35% 0.12 150); }
  &.failed { background: var(--color-danger-bg); color: oklch(40% 0.14 30); }
}
</style>
