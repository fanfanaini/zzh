<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>MCP 工具调试台</h2>
        <p class="page-subtitle">浏览可用的工具，手工调用并查看结果。</p>
      </div>
      <el-button :loading="loading" @click="loadTools">刷新工具列表</el-button>
    </div>

    <el-row :gutter="20">
      <!-- Tool List -->
      <el-col :span="10">
        <el-card class="detail-card" v-loading="loading">
          <template #header><span class="section-title">可用工具</span></template>
          <div v-if="tools.length === 0" class="empty-hint">暂无可用工具</div>
          <div
            v-for="tool in tools"
            :key="tool.name"
            :class="['tool-item', { selected: selectedTool?.name === tool.name }]"
            @click="selectTool(tool)"
          >
            <div class="tool-name">{{ tool.name }}</div>
            <div class="tool-desc">{{ tool.description || '无描述' }}</div>
            <div class="tool-meta">
              <el-tag size="small" :type="tool.idempotent ? 'success' : 'info'" class="tool-tag">
                {{ tool.idempotent ? '幂等' : '非幂等' }}
              </el-tag>
              <span v-if="tool.requiredPermissions?.length" class="tool-perms">
                {{ tool.requiredPermissions.join(', ') }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Tool Detail & Call -->
      <el-col :span="14">
        <el-card class="detail-card" v-if="selectedTool">
          <template #header>
            <span class="section-title">{{ selectedTool.name }}</span>
            <el-tag size="small" class="section-tag">{{ selectedTool.idempotent ? '幂等' : '非幂等' }}</el-tag>
          </template>

          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="描述">{{ selectedTool.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所需权限">
              {{ selectedTool.requiredPermissions?.join(', ') || '无' }}
            </el-descriptions-item>
          </el-descriptions>

          <section class="tool-section">
            <h4>参数 Schema</h4>
            <pre>{{ JSON.stringify(selectedTool.inputSchema, null, 2) }}</pre>
          </section>

          <!-- Dynamic Argument Form -->
          <section class="tool-section" v-if="argFields.length > 0">
            <h4>调用参数</h4>
            <el-form label-position="top">
              <el-form-item v-for="field in argFields" :key="field.name" :label="field.label">
                <el-input
                  v-if="field.type === 'string' || field.type === 'number'"
                  v-model="field.value"
                  :type="field.type === 'number' ? 'number' : 'text'"
                  :placeholder="field.placeholder"
                />
                <el-switch v-else-if="field.type === 'boolean'" v-model="field.value" />
                <el-input
                  v-else
                  v-model="field.value"
                  type="textarea"
                  :rows="2"
                  :placeholder="field.placeholder"
                />
              </el-form-item>
            </el-form>
          </section>

          <section class="tool-section">
            <el-button type="primary" :loading="calling" @click="handleCallTool">调用</el-button>
            <el-checkbox v-model="callWithIdempotency" label="幂等 Key" border size="small" style="margin-left:8px" />
            <el-input v-if="callWithIdempotency" v-model="idempotencyKey" placeholder="幂等 Key" style="width:200px;margin-left:4px;" />
          </section>

          <!-- Response -->
          <section class="tool-section" v-if="callResult">
            <h4>调用结果</h4>
            <div class="result-meta">
              <el-tag size="small" :type="callResult.success ? 'success' : 'danger'">
                {{ callResult.success ? '成功' : '失败' }}
              </el-tag>
              <span>Trace: {{ callResult.traceId }}</span>
              <span>重试: {{ callResult.attempts }} 次</span>
              <el-tag v-if="callResult.idempotencyReplay" size="small" type="warning">幂等回放</el-tag>
            </div>
            <div v-if="callResult.error" class="tool-error">
              <strong>{{ callResult.error.code }}</strong>: {{ callResult.error.message }}
            </div>
            <pre>{{ JSON.stringify(callResult.data, null, 2) }}</pre>
          </section>
        </el-card>
        <el-empty v-else description="从左侧选择一个工具" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { mcpApi } from '@/api/mcp'
import type { McpToolDescriptor } from '@/types/mcp'

const loading = ref(false)
const calling = ref(false)
const tools = ref<McpToolDescriptor[]>([])
const selectedTool = ref<McpToolDescriptor | null>(null)
const callWithIdempotency = ref(false)
const idempotencyKey = ref('')
const callResult = ref<{ success: boolean; traceId: string; attempts: number; idempotencyReplay: boolean; data: unknown; error?: { code: string; message: string } } | null>(null)

interface ArgField {
  name: string
  label: string
  type: string
  value: any
  placeholder: string
}

const argFields = computed<ArgField[]>(() => {
  if (!selectedTool.value?.inputSchema) return []
  const schema = selectedTool.value.inputSchema as Record<string, any>
  const properties = schema.properties || {}
  const required = new Set<string>(schema.required || [])
  return Object.entries(properties).map(([name, prop]: [string, any]) => ({
    name,
    label: `${name}${required.has(name) ? ' *' : ''}`,
    type: prop.type || 'string',
    value: prop.type === 'boolean' ? false : prop.type === 'number' ? 0 : '',
    placeholder: prop.description || name,
  }))
})

async function loadTools() {
  loading.value = true
  try {
    const res = await mcpApi.listTools()
    tools.value = res.data
  } finally {
    loading.value = false
  }
}

function selectTool(tool: McpToolDescriptor) {
  selectedTool.value = tool
  callResult.value = null
}

async function handleCallTool() {
  if (!selectedTool.value) return
  calling.value = true
  try {
    const args: Record<string, any> = {}
    for (const field of argFields.value) {
      if (field.value !== '' && field.value !== undefined) {
        args[field.name] = field.value
      }
    }
    const res = await mcpApi.callTool(selectedTool.value.name, {
      arguments: Object.keys(args).length > 0 ? args : undefined,
      idempotencyKey: callWithIdempotency.value ? idempotencyKey.value || undefined : undefined,
    })
    callResult.value = res.data as any
  } finally {
    calling.value = false
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

.section-tag {
  margin-left: 8px;
  vertical-align: middle;
}

.tool-item {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
  background: var(--color-surface);

  &:hover {
    border-color: var(--color-accent);
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  }

  &.selected {
    border-color: var(--color-accent);
    background: var(--color-accent-bg);
  }
}

.tool-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.tool-desc {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-tag {
  font-size: 0.7rem;
}

.tool-perms {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.tool-section {
  margin-top: 16px;
}

.tool-section h4 {
  margin-bottom: 8px;
  font-size: 0.85rem;
  color: var(--color-text-primary);
}

.tool-section pre {
  max-height: 300px;
  overflow: auto;
  padding: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-family: inherit;
  font-size: 0.82rem;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}

.tool-error {
  color: var(--color-danger);
  font-size: 0.85rem;
  padding: 8px 10px;
  background: var(--color-danger-bg, #fef2f2);
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
}

.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}
</style>
