<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>Agent 运维中心</h2>
        <p class="page-subtitle">系统健康状态、模型调用与组件监控。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-tag v-if="dashboard" :type="healthTagType" size="large" effect="dark" class="health-badge">
          {{ healthLabel }}
        </el-tag>
      </div>
    </div>

    <div v-loading="loading">
      <!-- Stats Row -->
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">健康状态</div>
            <div :class="['stat-value', healthClass]">{{ healthLabel }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">模型失败率</div>
            <div :class="['stat-value', rateClass(dashboard?.model.failureRate)]">
              {{ (dashboard?.model.failureRate ?? 0 * 100).toFixed(1) }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">平均延迟</div>
            <div class="stat-value">{{ Math.round(dashboard?.model.averageLatencyMillis ?? 0) }}ms</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">Trace 失败率</div>
            <div :class="['stat-value', rateClass(dashboard?.traces.failureRate)]">
              {{ (dashboard?.traces.failureRate ?? 0 * 100).toFixed(1) }}%
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- Token & Events -->
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">Prompt Tokens</div>
            <div class="stat-value">{{ (dashboard?.model.promptTokens ?? 0).toLocaleString() }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">Completion Tokens</div>
            <div class="stat-value">{{ (dashboard?.model.completionTokens ?? 0).toLocaleString() }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">模型调用</div>
            <div class="stat-value">{{ (dashboard?.model.totalCalls ?? 0).toLocaleString() }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">Trace 事件</div>
            <div class="stat-value">{{ (dashboard?.traces.totalEvents ?? 0).toLocaleString() }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- Charts Row -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card class="detail-card">
            <template #header><span class="section-title">模型用途分布</span></template>
            <div ref="modelUseCaseChartRef" style="height:260px;"></div>
            <el-empty v-if="!hasModelUseCases" description="暂无数据" />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="detail-card">
            <template #header><span class="section-title">Trace 组件分布</span></template>
            <div ref="traceComponentChartRef" style="height:260px;"></div>
            <el-empty v-if="!hasTraceComponents" description="暂无数据" />
          </el-card>
        </el-col>
      </el-row>

      <!-- Alert List -->
      <el-card class="detail-card">
        <template #header>
          <span class="section-title">当前告警</span>
          <el-tag v-if="alerts.length" type="danger" size="small" class="alert-count">{{ alerts.length }}</el-tag>
        </template>
        <el-table :data="alerts" stripe style="width:100%" v-if="alerts.length > 0">
          <el-table-column label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="severityTagType(row.severity)" effect="dark">
                {{ severityLabel(row.severity) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="code" label="代码" width="120" />
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="message" label="消息" min-width="240" show-overflow-tooltip />
          <el-table-column prop="triggeredAt" label="触发时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.triggeredAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="showAlertDetail(row as any)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无告警" />
      </el-card>
    </div>

    <!-- Alert Detail Drawer -->
    <el-drawer v-model="alertDrawerOpen" title="告警详情" size="min(600px, 88vw)">
      <template v-if="selectedAlert">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="代码">{{ selectedAlert.code }}</el-descriptions-item>
          <el-descriptions-item label="严重程度">{{ severityLabel(selectedAlert.severity) }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ selectedAlert.title }}</el-descriptions-item>
          <el-descriptions-item label="消息">{{ selectedAlert.message }}</el-descriptions-item>
          <el-descriptions-item label="触发时间">{{ formatDateTime(selectedAlert.triggeredAt) }}</el-descriptions-item>
        </el-descriptions>
        <section class="detail-section">
          <h4>详细信息</h4>
          <pre>{{ JSON.stringify(selectedAlert.details, null, 2) || '无' }}</pre>
        </section>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { opsApi } from '@/api/ops'
import type { AgentOpsDashboard, AgentOpsAlert, AlertSeverity } from '@/types/ops'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const dashboard = ref<AgentOpsDashboard | null>(null)
const alerts = ref<AgentOpsAlert[]>([])
const selectedAlert = ref<AgentOpsAlert | null>(null)
const alertDrawerOpen = ref(false)

const modelUseCaseChartRef = ref<HTMLElement | null>(null)
const traceComponentChartRef = ref<HTMLElement | null>(null)

onMounted(loadAll)

const healthTagType = computed(() => {
  if (!dashboard.value) return 'info'
  const h = dashboard.value.healthStatus
  if (h === 'HEALTHY') return 'success'
  if (h === 'DEGRADED') return 'warning'
  return 'danger'
})

const healthLabel = computed(() => {
  if (!dashboard.value) return '未知'
  const h = dashboard.value.healthStatus
  if (h === 'HEALTHY') return '健康'
  if (h === 'DEGRADED') return '亚健康'
  return '严重'
})

const healthClass = computed(() => {
  if (!dashboard.value) return ''
  const h = dashboard.value.healthStatus
  if (h === 'HEALTHY') return 'success'
  if (h === 'DEGRADED') return 'warning'
  return 'danger'
})

const hasModelUseCases = computed(() => {
  const m = dashboard.value?.modelUseCases
  return m && Object.keys(m).length > 0
})

const hasTraceComponents = computed(() => {
  const m = dashboard.value?.traceComponents
  return m && Object.keys(m).length > 0
})

function rateClass(rate?: number) {
  if (rate == null) return ''
  if (rate < 0.05) return 'success'
  if (rate < 0.15) return 'warning'
  return 'danger'
}

function severityTagType(s: AlertSeverity) {
  if (s === 'CRITICAL') return 'danger'
  if (s === 'WARN') return 'warning'
  return 'info'
}

function severityLabel(s: AlertSeverity) {
  const map: Record<string, string> = { INFO: '信息', WARN: '警告', CRITICAL: '严重' }
  return map[s] || s
}

async function loadAll() {
  loading.value = true
  try {
    const [dashboardRes, alertsRes] = await Promise.all([
      opsApi.dashboard(),
      opsApi.alerts(),
    ])
    dashboard.value = dashboardRes.data
    alerts.value = alertsRes.data
    nextTick(renderCharts)
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function showAlertDetail(alert: AgentOpsAlert) {
  selectedAlert.value = alert
  alertDrawerOpen.value = true
}

async function renderCharts() {
  if (!dashboard.value) return
  try {
    const echarts = await import('echarts')
    if (hasModelUseCases.value && modelUseCaseChartRef.value) {
      const chart = echarts.init(modelUseCaseChartRef.value)
      const entries = Object.entries(dashboard.value!.modelUseCases).sort((a, b) => b[1] - a[1])
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { type: 'scroll', orient: 'vertical', right: 10, top: 20 },
        series: [{
          type: 'pie',
          radius: ['35%', '60%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: true,
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
          data: entries.map(([name, value]) => ({ name, value })),
        }],
      })
    }
    if (hasTraceComponents.value && traceComponentChartRef.value) {
      const chart = echarts.init(traceComponentChartRef.value)
      const entries = Object.entries(dashboard.value!.traceComponents).sort((a, b) => b[1] - a[1])
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: entries.map(([n]) => n) },
        yAxis: { type: 'value' },
        series: [{
          type: 'bar',
          data: entries.map(([, v]) => v),
          itemStyle: { borderRadius: [4, 4, 0, 0] },
        }],
      })
    }
  } catch { /* echarts not available */ }
}
</script>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.stat-row,
.detail-card,
.chart-row {
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.health-badge {
  font-size: 0.85rem;
}

.stat-value.success { color: var(--color-success); }
.stat-value.warning { color: var(--color-warning); }
.stat-value.danger { color: var(--color-danger); }

.alert-count {
  margin-left: 8px;
  vertical-align: middle;
}

.detail-section {
  margin-top: 18px;
}

.detail-section h4 {
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.detail-section pre {
  max-height: 400px;
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
