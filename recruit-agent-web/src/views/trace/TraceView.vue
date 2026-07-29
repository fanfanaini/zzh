<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>Agent Trace 链路追踪</h2>
        <p class="page-subtitle">查看 Agent 的执行链路：节点、检索、模型调用、工具使用、记忆与 Skill。</p>
      </div>
      <el-button :loading="loading" @click="handleSearch">刷新</el-button>
    </div>

    <!-- Search -->
    <el-card class="detail-card">
      <div class="search-row">
        <el-input v-model="searchTraceId" placeholder="按 Trace ID 查询" clearable style="width:260px" @keyup.enter="handleSearch" />
        <el-input v-model="searchConversationId" placeholder="按会话 ID 查询" clearable style="width:180px" type="number" @keyup.enter="handleSearch" />
        <el-select v-model="searchComponent" clearable placeholder="组件类型" style="width:140px">
          <el-option v-for="c in componentOptions" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-select v-model="searchStatus" clearable placeholder="状态" style="width:110px">
          <el-option label="成功" value="SUCCEEDED" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已启动" value="STARTED" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button v-if="!showTimeline" text @click="fetchFromUrl">解析 TraceId 查询</el-button>
      </div>
      <div class="search-hint" v-if="!searchTraceId && !searchConversationId">
        请输入 Trace ID 或会话 ID 查询链路
      </div>
    </el-card>

    <!-- Timeline View -->
    <div v-if="traces.length > 0" class="timeline-container" v-loading="loading">
      <!-- Summary Stats -->
      <el-row :gutter="16" class="stat-row">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">事件总数</div>
            <div class="stat-value">{{ traces.length }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">失败事件</div>
            <div class="stat-value danger">{{ failedCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">Trace ID</div>
            <div class="stat-value trace-id" @click="copyText(currentTraceId)">{{ currentTraceId }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-label">时间范围</div>
            <div class="stat-value time-range">{{ timeRange }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- Trace Event Timeline -->
      <el-timeline>
        <el-timeline-item
          v-for="(event, idx) in sortedTraces"
          :key="idx"
          :timestamp="formatTime(event.occurredAt)"
          :color="event.status === 'FAILED' ? 'var(--color-danger)' : event.status === 'STARTED' ? 'var(--color-warning)' : 'var(--color-success)'"
        >
          <div :class="['timeline-event', event.status.toLowerCase()]">
            <div class="event-header">
              <el-tag size="small" :type="componentTagType(event.component)" class="event-component">
                {{ event.component || 'UNKNOWN' }}
              </el-tag>
              <span class="event-node">{{ event.node }}</span>
              <el-tag size="small" :type="event.status === 'FAILED' ? 'danger' : event.status === 'STARTED' ? 'warning' : 'success'" effect="plain">
                {{ event.status === 'FAILED' ? '失败' : event.status === 'STARTED' ? '进行中' : '成功' }}
              </el-tag>
            </div>
            <div class="event-operation" v-if="event.operation">
              <span class="op-label">操作：</span>{{ event.operation }}
            </div>
            <div class="event-span" v-if="event.spanId">
              <span class="op-label">Span：</span>{{ event.spanId }}
              <span v-if="event.parentSpanId"> | Parent：{{ event.parentSpanId }}</span>
            </div>
            <div class="event-details" v-if="hasDetails(event.details)">
              <el-button size="small" text @click="toggleDetail(idx)">{{ expandedDetail[idx] ? '收起' : '详情' }}</el-button>
              <pre v-if="expandedDetail[idx]">{{ JSON.stringify(event.details, null, 2) }}</pre>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <el-empty v-else-if="!loading && searched" description="未找到 Trace 事件" />
    <el-empty v-else-if="!loading && !searched" description="输入查询条件后查看链路" />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { traceApi } from '@/api/trace'
import type { AgentTraceEvent, TraceComponent } from '@/types/trace'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const searched = ref(false)
const traces = ref<AgentTraceEvent[]>([])
const expandedDetail = reactive<Record<number, boolean>>({})

const searchTraceId = ref('')
const searchConversationId = ref('')
const searchComponent = ref('')
const searchStatus = ref('')
const showTimeline = ref(false)

const componentOptions = [
  { value: 'AGENT', label: 'Agent' },
  { value: 'RETRIEVAL', label: '检索' },
  { value: 'TOOL', label: '工具' },
  { value: 'MODEL', label: '模型' },
  { value: 'MEMORY', label: '记忆' },
  { value: 'SKILL', label: 'Skill' },
]

const failedCount = computed(() => traces.value.filter((t) => t.status === 'FAILED').length)

const currentTraceId = computed(() => {
  if (traces.value.length === 0) return '-'
  return traces.value[0].traceId
})

const timeRange = computed(() => {
  if (traces.value.length === 0) return '-'
  const times = traces.value.map((t) => new Date(t.occurredAt).getTime())
  const min = new Date(Math.min(...times))
  const max = new Date(Math.max(...times))
  return `${min.toLocaleTimeString('zh-CN')} - ${max.toLocaleTimeString('zh-CN')}`
})

const sortedTraces = computed(() => {
  return [...traces.value].sort((a, b) => new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime())
})

function componentTagType(c: TraceComponent | null) {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    AGENT: 'primary', RETRIEVAL: 'success', TOOL: 'warning',
    MODEL: 'info', MEMORY: 'info', SKILL: 'danger',
  }
  return map[c || ''] || 'info'
}

function hasDetails(details: Record<string, unknown>): boolean {
  return details && Object.keys(details).length > 0
}

function toggleDetail(idx: number) {
  expandedDetail[idx] = !expandedDetail[idx]
}

function formatTime(t: string) {
  return formatDateTime(t)
}

function copyText(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制')
}

function fetchFromUrl() {
  if (searchTraceId.value) handleSearch()
}

async function handleSearch() {
  const traceId = searchTraceId.value.trim() || undefined
  const convId = searchConversationId.value ? Number(searchConversationId.value) : undefined

  if (!traceId && !convId) {
    ElMessage.warning('请输入 Trace ID 或会话 ID')
    return
  }

  loading.value = true
  searched.value = true
  try {
    const res = await traceApi.find({
      traceId,
      conversationId: convId,
      component: (searchComponent.value as TraceComponent) || undefined,
      status: (searchStatus.value as any) || undefined,
    })
    traces.value = res.data
    showTimeline.value = traces.value.length > 0
  } catch {
    traces.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.detail-card {
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.search-hint {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin-top: 8px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-value.danger {
  color: var(--color-danger);
}

.trace-id {
  font-size: 0.75rem;
  font-family: monospace;
  cursor: pointer;
  color: var(--color-accent);
}

.trace-id:hover {
  text-decoration: underline;
}

.time-range {
  font-size: 0.75rem;
}

.timeline-event {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);

  &.failed {
    border-left: 3px solid var(--color-danger);
  }

  &.succeeded {
    border-left: 3px solid var(--color-success);
  }

  &.started {
    border-left: 3px solid var(--color-warning);
  }
}

.event-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.event-component {
  min-width: 60px;
  text-align: center;
}

.event-node {
  font-weight: 600;
  font-size: 0.85rem;
  color: var(--color-text-primary);
  flex: 1;
}

.event-operation,
.event-span {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.op-label {
  color: var(--color-text-muted);
}

.event-details {
  margin-top: 6px;
}

.event-details pre {
  max-height: 200px;
  overflow: auto;
  padding: 8px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 0.78rem;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}
</style>
