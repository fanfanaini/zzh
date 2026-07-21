<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>模型与 Prompt</h2>
        <p class="page-subtitle">查看模型调用质量，维护 Prompt 版本。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">调用总数</div>
          <div class="stat-value">{{ summary?.totalCalls ?? 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">失败调用</div>
          <div class="stat-value danger">{{ summary?.failedCalls ?? 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">Prompt Tokens</div>
          <div class="stat-value">{{ summary?.promptTokens ?? 0 }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">平均延迟</div>
          <div class="stat-value">{{ Math.round(summary?.averageLatencyMillis ?? 0) }}ms</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card class="detail-card" v-loading="loading">
          <template #header><span class="section-title">模型调用日志</span></template>
          <el-table :data="logs" stripe style="width: 100%">
            <el-table-column prop="useCase" label="操作" min-width="130" />
            <el-table-column prop="modelName" label="模型" min-width="130" />
            <el-table-column prop="latencyMillis" label="延迟" width="90">
              <template #default="{ row }">{{ row.latencyMillis ?? 0 }}ms</template>
            </el-table-column>
            <el-table-column label="Token" width="120">
              <template #default="{ row }">{{ (row.promptTokens ?? 0) + (row.completionTokens ?? 0) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'SUCCEEDED' ? 'success' : 'danger'">{{ row.status === 'SUCCEEDED' ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button size="small" text type="primary" @click="openLogDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card class="detail-card">
          <template #header><span class="section-title">Prompt 版本</span></template>
          <el-form label-position="top">
            <el-form-item label="Prompt Code">
              <el-input v-model="promptCode" placeholder="例如 RESUME_PROFILE_EXTRACTION" @keyup.enter="loadPrompts" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="promptLoading" @click="loadPrompts">查询版本</el-button>
              <el-button @click="createOpen = true">新增版本</el-button>
            </el-form-item>
          </el-form>
          <el-alert
            v-if="activePrompt"
            :title="`当前生效版本：${activePrompt.version}`"
            type="success"
            :closable="false"
            show-icon
            class="active-prompt"
          />
          <el-table :data="prompts" stripe style="width: 100%">
            <el-table-column prop="version" label="版本" width="110" />
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="createOpen" title="新增 Prompt 版本" width="720px">
      <el-form :model="draft" label-position="top">
        <el-form-item label="版本号"><el-input v-model="draft.version" /></el-form-item>
        <el-form-item label="System Prompt"><el-input v-model="draft.systemPrompt" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="User Prompt Template"><el-input v-model="draft.userPromptTemplate" type="textarea" :rows="6" /></el-form-item>
        <el-form-item><el-checkbox v-model="draft.enabled">创建后启用</el-checkbox></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createPrompt">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logDetailOpen" title="模型调用详情" size="min(760px, 92vw)">
      <template v-if="selectedLog">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="调用场景">{{ selectedLog.useCase }}</el-descriptions-item>
          <el-descriptions-item label="模型">{{ selectedLog.modelName }}</el-descriptions-item>
          <el-descriptions-item label="Prompt 版本">{{ selectedLog.promptVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedLog.status }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ selectedLog.latencyMillis ?? 0 }}ms</el-descriptions-item>
          <el-descriptions-item label="Token">{{ (selectedLog.promptTokens ?? 0) + (selectedLog.completionTokens ?? 0) }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="selectedLog.errorMessage"
          :title="selectedLog.errorMessage"
          type="error"
          :closable="false"
          show-icon
          class="log-section"
        />
        <section class="log-section">
          <h4>System Prompt</h4>
          <pre>{{ selectedLog.systemPrompt || '-' }}</pre>
        </section>
        <section class="log-section">
          <h4>User Prompt</h4>
          <pre>{{ selectedLog.userPrompt || '-' }}</pre>
        </section>
        <section class="log-section">
          <h4>模型响应</h4>
          <pre>{{ selectedLog.responseBody || '-' }}</pre>
        </section>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { modelCallApi, promptApi } from '@/api/ai'
import type { ModelCallLog, ModelCallSummary, PromptVersion } from '@/api/ai'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const promptLoading = ref(false)
const creating = ref(false)
const createOpen = ref(false)
const summary = ref<ModelCallSummary | null>(null)
const logs = ref<ModelCallLog[]>([])
const prompts = ref<PromptVersion[]>([])
const activePrompt = ref<PromptVersion | null>(null)
const selectedLog = ref<ModelCallLog | null>(null)
const logDetailOpen = ref(false)
const promptCode = ref('RESUME_PROFILE_EXTRACTION')
const draft = reactive({
  version: '',
  systemPrompt: '',
  userPromptTemplate: '',
  enabled: true,
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [summaryRes, logsRes] = await Promise.all([modelCallApi.summary(), modelCallApi.list()])
    summary.value = summaryRes.data
    logs.value = logsRes.data
  } finally {
    loading.value = false
  }
}

async function loadPrompts() {
  if (!promptCode.value.trim()) return
  promptLoading.value = true
  try {
    const code = promptCode.value.trim()
    const [versionsResult, activeResult] = await Promise.allSettled([
      promptApi.list(code),
      promptApi.active(code),
    ])
    prompts.value = versionsResult.status === 'fulfilled' ? versionsResult.value.data : []
    activePrompt.value = activeResult.status === 'fulfilled' ? activeResult.value.data : null
  } finally {
    promptLoading.value = false
  }
}

function openLogDetail(log: unknown) {
  selectedLog.value = log as ModelCallLog
  logDetailOpen.value = true
}

async function createPrompt() {
  if (!promptCode.value.trim() || !draft.version.trim()) {
    ElMessage.warning('请填写 Prompt Code 和版本号')
    return
  }
  creating.value = true
  try {
    await promptApi.create(promptCode.value.trim(), { ...draft })
    createOpen.value = false
    Object.assign(draft, { version: '', systemPrompt: '', userPromptTemplate: '', enabled: true })
    await loadPrompts()
    ElMessage.success('Prompt 版本已创建')
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

.stat-row,
.detail-card {
  margin-bottom: 20px;
}

.stat-value.danger {
  color: var(--color-danger);
}

.active-prompt {
  margin-bottom: 12px;
}

.log-section {
  margin-top: 18px;
}

.log-section h4 {
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.log-section pre {
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-family: inherit;
}
</style>
