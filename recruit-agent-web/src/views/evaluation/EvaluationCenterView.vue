<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>Agent 评测中心</h2>
        <p class="page-subtitle">意图识别、路由、工具选择、端到端成功率评测。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="running" @click="runCoreBaseline">运行核心基线评测</el-button>
        <el-button @click="showUpload = true">上传审核数据集</el-button>
      </div>
    </div>

    <div v-loading="running">
      <!-- Stats Cards -->
      <el-row :gutter="16" class="stat-row" v-if="report">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">数据集</div>
            <div class="stat-value" style="font-size:0.9rem;">{{ report.datasetVersion }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">端到端成功率</div>
            <div :class="['stat-value', pctClass(report.endToEnd.successRate)]">
              {{ (report.endToEnd.successRate * 100).toFixed(1) }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">期望结果匹配率</div>
            <div :class="['stat-value', pctClass(report.endToEnd.expectedOutcomeMatchRate)]">
              {{ (report.endToEnd.expectedOutcomeMatchRate * 100).toFixed(1) }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">总 Token</div>
            <div class="stat-value">{{ (report.runtime.totalTokens).toLocaleString() }}</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" v-if="report">
        <!-- Main Metrics -->
        <el-col :span="14">
          <el-card class="detail-card">
            <template #header><span class="section-title">核心指标</span></template>
            <el-table :data="metricRows" stripe style="width:100%">
              <el-table-column prop="dimension" label="维度" width="110" />
              <el-table-column prop="metric" label="指标" width="100" />
              <el-table-column label="值" width="100" align="center">
                <template #default="{ row }">
                  <span :class="['metric-value-inline', pctClass(row.value)]">
                    {{ (row.value * 100).toFixed(1) }}%
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="cases" label="评估样本数" width="100" align="center" />
            </el-table>
          </el-card>

          <!-- Runtime Metrics -->
          <el-card class="detail-card">
            <template #header><span class="section-title">运行时指标</span></template>
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="延迟样本">{{ report.runtime.latencySamples }}</el-descriptions-item>
              <el-descriptions-item label="平均延迟">{{ report.runtime.averageLatencyMillis.toFixed(0) }}ms</el-descriptions-item>
              <el-descriptions-item label="P50 延迟">{{ report.runtime.p50LatencyMillis.toFixed(0) }}ms</el-descriptions-item>
              <el-descriptions-item label="P95 延迟">{{ report.runtime.p95LatencyMillis.toFixed(0) }}ms</el-descriptions-item>
              <el-descriptions-item label="Prompt Tokens">{{ report.runtime.promptTokens.toLocaleString() }}</el-descriptions-item>
              <el-descriptions-item label="Completion Tokens">{{ report.runtime.completionTokens.toLocaleString() }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- Failure Analysis -->
        <el-col :span="10">
          <el-card class="detail-card">
            <template #header><span class="section-title">失败类型分布</span></template>
            <div ref="failureChartRef" style="height:250px;" v-if="hasFailures"></div>
            <el-empty v-else description="无失败" />
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!report && !running" description="点击「运行核心基线评测」开始评估" />
    </div>

    <!-- Upload Reviewed Baseline Dialog -->
    <el-dialog v-model="showUpload" title="上传人工审核数据集 (NDJSON)" width="560px" :close-on-click-modal="false">
      <p class="upload-hint">上传 .ndjson 格式的人工审核对话数据集，系统将运行基线评估。</p>
      <el-input v-model="jsonlContent" type="textarea" :rows="10" placeholder="粘贴 NDJSON 内容…" />
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">评测</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluationApi } from '@/api/evaluation'
import type { EvaluationReport } from '@/types/evaluation'

const running = ref(false)
const uploading = ref(false)
const showUpload = ref(false)
const jsonlContent = ref('')
const report = ref<EvaluationReport | null>(null)
const failureChartRef = ref<HTMLElement | null>(null)

const metricRows = computed(() => {
  if (!report.value) return []
  const r = report.value
  return [
    { dimension: '意图识别', metric: '准确率', value: r.intent.accuracy, cases: r.intent.evaluatedCases },
    { dimension: '意图识别', metric: 'Macro F1', value: r.intent.macroF1, cases: r.intent.evaluatedCases },
    { dimension: '路由', metric: '精确匹配率', value: r.routing.exactMatchRate, cases: r.routing.evaluatedCases },
    { dimension: '工具选择', metric: '精确匹配率', value: r.toolSelection.exactMatchRate, cases: r.toolSelection.evaluatedCases },
    { dimension: '检索', metric: 'Recall@K', value: r.retrieval.recallAtK, cases: r.retrieval.evaluatedCases },
    { dimension: '检索', metric: 'MRR', value: r.retrieval.meanReciprocalRank, cases: r.retrieval.evaluatedCases },
    { dimension: '端到端', metric: '成功率', value: r.endToEnd.successRate, cases: r.endToEnd.evaluatedCases },
    { dimension: '交接', metric: 'F1', value: r.handoff.f1, cases: r.handoff.evaluatedCases },
  ]
})

const hasFailures = computed(() => {
  if (!report.value?.failureCounts) return false
  return Object.keys(report.value.failureCounts).length > 0
})

function pctClass(val: number) {
  if (val >= 0.8) return 'success'
  if (val >= 0.6) return 'warning'
  return 'danger'
}

async function runCoreBaseline() {
  running.value = true
  try {
    const res = await evaluationApi.coreBaseline()
    report.value = res.data
    ElMessage.success('核心基线评测完成')
    nextTick(renderFailureChart)
  } catch {
    report.value = null
  } finally {
    running.value = false
  }
}

async function handleUpload() {
  if (!jsonlContent.value.trim()) {
    ElMessage.warning('请粘贴 NDJSON 内容')
    return
  }
  uploading.value = true
  try {
    const res = await evaluationApi.reviewedBaseline(jsonlContent.value)
    report.value = res.data
    showUpload.value = false
    jsonlContent.value = ''
    ElMessage.success('人工审核基线评测完成')
    nextTick(renderFailureChart)
  } finally {
    uploading.value = false
  }
}

async function renderFailureChart() {
  if (!report.value?.failureCounts || !failureChartRef.value) return
  try {
    const echarts = await import('echarts')
    const chart = echarts.init(failureChartRef.value)
    const entries = Object.entries(report.value.failureCounts)
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      legend: { show: false },
      series: [{
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '50%'],
        label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
        data: entries.map(([name, count]) => ({ name, value: count })),
      }],
    })
  } catch { /* echarts not available */ }
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
}

.header-actions {
  display: flex;
  gap: 8px;
}

.stat-row,
.detail-card {
  margin-bottom: 20px;
}

.stat-value.success { color: var(--color-success); }
.stat-value.warning { color: var(--color-warning); }
.stat-value.danger { color: var(--color-danger); }

.metric-value-inline {
  font-weight: 600;
  font-size: 0.9rem;

  &.success { color: var(--color-success); }
  &.warning { color: var(--color-warning); }
  &.danger { color: var(--color-danger); }
}

.upload-hint {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}
</style>
