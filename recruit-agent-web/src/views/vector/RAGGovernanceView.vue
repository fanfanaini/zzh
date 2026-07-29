<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>RAG 治理中心</h2>
        <p class="page-subtitle">Embedding 版本管理、索引重建、检索质量评测。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1: Embedding Versions -->
      <el-tab-pane label="Embedding 版本" name="versions">
        <div class="tab-toolbar">
          <el-button type="primary" size="small" @click="showRegister = true"><el-icon><Plus /></el-icon>注册新版本</el-button>
        </div>
        <el-table :data="embeddingVersions" stripe v-loading="loading" style="width:100%">
          <el-table-column prop="versionId" label="版本 ID" min-width="140" />
          <el-table-column prop="modelName" label="模型" width="140" />
          <el-table-column prop="dimensions" label="维度" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="激活" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.active ? 'success' : 'info'">{{ row.active ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="notes" label="备注" min-width="120" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="activatedAt" label="激活时间" width="170">
            <template #default="{ row }">{{ row.activatedAt ? formatDateTime(row.activatedAt) : '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" :disabled="(row as any).active" @click="handleActivate(row as any)">激活</el-button>
              <el-button size="small" text type="warning" :disabled="!(row as any).active" @click="handleRollback(row as any)">回滚</el-button>
              <el-button size="small" text :loading="rebuilding[versionKey(row as any)]" @click="handleRebuild(row as any)">重建索引</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!embeddingVersions.length" description="暂无 Embedding 版本" />
      </el-tab-pane>

      <!-- Tab 2: Rebuild Jobs -->
      <el-tab-pane label="重建任务" name="jobs">
        <el-table :data="rebuildJobs" stripe v-loading="loading" style="width:100%">
          <el-table-column prop="jobId" label="任务 ID" width="200" />
          <el-table-column prop="targetVersion" label="目标版本" width="120" />
          <el-table-column label="进度" min-width="180">
            <template #default="{ row }">
              <el-progress
                :percentage="jobProgress(row as any)"
                :status="(row as any).status === 'FAILED' ? 'exception' : (row as any).status === 'FINISHED' ? 'success' : undefined"
              />
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'FINISHED' ? 'success' : row.status === 'FAILED' ? 'danger' : 'warning'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalChunks" label="总 Chunks" width="100" align="center" />
          <el-table-column prop="startedAt" label="开始时间" width="170">
            <template #default="{ row }">{{ row.startedAt ? formatDateTime(row.startedAt) : '-' }}</template>
          </el-table-column>
          <el-table-column label="错误" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!rebuildJobs.length" description="暂无重建任务" />
      </el-tab-pane>

      <!-- Tab 3: Retrieval Quality Report -->
      <el-tab-pane label="检索质量" name="quality">
        <div class="tab-toolbar">
          <el-button type="primary" :loading="reportLoading" @click="loadReport">运行评测</el-button>
          <span class="report-k">
            k =
            <el-input-number v-model="reportK" :min="1" :max="20" size="small" style="width:80px;margin-left:4px;" />
          </span>
        </div>

        <div v-if="report" class="report-container">
          <el-alert :title="`评测数据集: ${report.datasetVersion}`" type="info" :closable="false" show-icon class="report-header" />
          <el-alert
            :title="`评测结论: ${report.verdict}`"
            :type="(report.verdict === 'PASS' ? 'success' : report.verdict === 'DEGRADED' ? 'warning' : 'error')"
            :closable="false"
            show-icon
          />

          <el-row :gutter="16" class="metric-row">
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Baseline Recall@K</div>
                <div class="metric-value">{{ (report.comparison.baseline.recallAtK * 100).toFixed(1) }}%</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Reranked Recall@K</div>
                <div class="metric-value success">{{ (report.comparison.reranked.recallAtK * 100).toFixed(1) }}%</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">MRR@K</div>
                <div class="metric-value">{{ (report.comparison.reranked.mrrAtK * 100).toFixed(1) }}%</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">NDCG@K</div>
                <div class="metric-value">{{ (report.comparison.reranked.ndcgAtK * 100).toFixed(1) }}%</div>
              </div>
            </el-col>
          </el-row>

          <el-row :gutter="16" class="metric-row">
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Delta Recall@K</div>
                <div :class="['metric-value', deltaClass(report.delta.recallAtK)]">
                  {{ (report.delta.recallAtK * 100).toFixed(1) }}%
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Delta MRR@K</div>
                <div :class="['metric-value', deltaClass(report.delta.mrrAtK)]">
                  {{ (report.delta.mrrAtK * 100).toFixed(1) }}%
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">Delta NDCG@K</div>
                <div :class="['metric-value', deltaClass(report.delta.ndcgAtK)]">
                  {{ (report.delta.ndcgAtK * 100).toFixed(1) }}%
                </div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="metric-card">
                <div class="metric-label">劣化率</div>
                <div :class="['metric-value', report.comparison.degradedRate > 0.1 ? 'danger' : 'success']">
                  {{ (report.comparison.degradedRate * 100).toFixed(1) }}%
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- Weak Cases -->
          <el-card class="detail-card" v-if="report.weakCases.length > 0">
            <template #header><span class="section-title">弱案例分析</span></template>
            <el-table :data="report.weakCases" stripe style="width:100%">
              <el-table-column prop="caseId" label="Case ID" width="140" />
              <el-table-column prop="baselineReciprocalRank" label="Baseline RR" width="120" align="center">
                <template #default="{ row }">{{ row.baselineReciprocalRank.toFixed(3) }}</template>
              </el-table-column>
              <el-table-column prop="rerankedReciprocalRank" label="Reranked RR" width="120" align="center">
                <template #default="{ row }">{{ row.rerankedReciprocalRank.toFixed(3) }}</template>
              </el-table-column>
              <el-table-column prop="baselineNdcg" label="Baseline NDCG" width="120" align="center">
                <template #default="{ row }">{{ row.baselineNdcg.toFixed(3) }}</template>
              </el-table-column>
              <el-table-column prop="rerankedNdcg" label="Reranked NDCG" width="120" align="center">
                <template #default="{ row }">{{ row.rerankedNdcg.toFixed(3) }}</template>
              </el-table-column>
              <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
            </el-table>
          </el-card>
        </div>
        <el-empty v-else-if="!reportLoading" description="点击「运行评测」生成检索质量报告" />
      </el-tab-pane>
    </el-tabs>

    <!-- Register Version Dialog -->
    <el-dialog v-model="showRegister" title="注册 Embedding 版本" width="480px" :close-on-click-modal="false">
      <el-form :model="registerForm" label-position="top">
        <el-form-item label="版本 ID" required>
          <el-input v-model="registerForm.versionId" placeholder="例如：v2" />
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input v-model="registerForm.modelName" placeholder="例如：text-embedding-3-small" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="向量维度" required>
              <el-input-number v-model="registerForm.dimensions" :min="64" :max="4096" :step="128" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="registerForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="handleRegister">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { vectorApi } from '@/api/vector'
import { evaluationApi } from '@/api/evaluation'
import type { EmbeddingVersionResponse, EmbeddingRebuildJobResponse } from '@/types/vector'
import type { RetrievalQualityReport } from '@/types/evaluation'
import { formatDateTime } from '@/utils/format'

const activeTab = ref('versions')
const loading = ref(false)
const reportLoading = ref(false)
const reportK = ref(5)

const embeddingVersions = ref<EmbeddingVersionResponse[]>([])
const rebuildJobs = ref<EmbeddingRebuildJobResponse[]>([])
const report = ref<RetrievalQualityReport | null>(null)

const showRegister = ref(false)
const registering = ref(false)
const rebuilding = reactive<Record<string, boolean>>({})

const registerForm = reactive({
  versionId: '',
  modelName: '',
  dimensions: 768,
  notes: '',
})

onMounted(loadAll)

function versionKey(v: EmbeddingVersionResponse) {
  return v.versionId
}

function jobProgress(job: EmbeddingRebuildJobResponse) {
  if (job.totalChunks === 0) return 0
  return Math.round(((job.succeededChunks + job.failedChunks) / job.totalChunks) * 100)
}

function deltaClass(val: number) {
  if (val > 0.01) return 'success'
  if (val < -0.01) return 'danger'
  return ''
}

async function loadAll() {
  loading.value = true
  try {
    const [verRes, jobRes] = await Promise.all([
      vectorApi.embeddingVersions().catch(() => ({ data: [] })),
      vectorApi.embeddingRebuildJobs().catch(() => ({ data: [] })),
    ])
    embeddingVersions.value = verRes.data
    rebuildJobs.value = jobRes.data
  } finally {
    loading.value = false
  }
}

async function handleActivate(v: EmbeddingVersionResponse) {
  await ElMessageBox.confirm(`确认激活版本 ${v.versionId}？激活后将使用此版本进行向量化。`, '激活版本', { type: 'info' })
  await vectorApi.activateEmbeddingVersion(v.versionId)
  ElMessage.success('版本已激活')
  await loadAll()
}

async function handleRollback(v: EmbeddingVersionResponse) {
  await ElMessageBox.confirm(`确认从此版本 ${v.versionId} 回滚？回滚后将停用此版本。`, '回滚版本', { type: 'warning' })
  await vectorApi.rollbackEmbeddingVersion(v.versionId)
  ElMessage.success('版本已回滚')
  await loadAll()
}

async function handleRebuild(v: EmbeddingVersionResponse) {
  const key = v.versionId
  rebuilding[key] = true
  try {
    await vectorApi.rebuildEmbeddingVersion(v.versionId)
    ElMessage.success(`版本 ${v.versionId} 索引重建任务已提交`)
    await loadAll()
  } finally {
    rebuilding[key] = false
  }
}

async function handleRegister() {
  if (!registerForm.versionId || !registerForm.modelName) {
    ElMessage.warning('请填写版本 ID 和模型名称')
    return
  }
  registering.value = true
  try {
    await vectorApi.registerEmbeddingVersion({ ...registerForm })
    ElMessage.success('版本已注册')
    showRegister.value = false
    Object.assign(registerForm, { versionId: '', modelName: '', dimensions: 768, notes: '' })
    await loadAll()
  } finally {
    registering.value = false
  }
}

async function loadReport() {
  reportLoading.value = true
  try {
    const res = await evaluationApi.retrievalQualityReport(reportK.value)
    report.value = res.data
  } catch {
    report.value = null
  } finally {
    reportLoading.value = false
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

.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.report-k {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
}

.report-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-header {
  margin-bottom: 0;
}

.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  text-align: center;
}

.metric-label {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  margin-bottom: 6px;
}

.metric-value {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-primary);

  &.success { color: var(--color-success); }
  &.danger { color: var(--color-danger); }
}

.detail-card {
  margin-top: 0;
}
</style>
