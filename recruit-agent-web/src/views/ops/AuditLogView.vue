<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>审计日志</h2>
        <p class="page-subtitle">追踪修改型接口和关键业务操作。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="limit" style="width: 120px" @change="load">
          <el-option label="最近 50 条" :value="50" />
          <el-option label="最近 100 条" :value="100" />
          <el-option label="最近 200 条" :value="200" />
        </el-select>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="logs" stripe style="width: 100%">
        <el-table-column prop="method" label="动作" width="90" />
        <el-table-column prop="path" label="资源" min-width="220" show-overflow-tooltip />
        <el-table-column prop="statusCode" label="状态码" width="100" />
        <el-table-column prop="userId" label="用户 ID" width="110" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="durationMillis" label="耗时" width="100">
          <template #default="{ row }">{{ row.durationMillis }}ms</template>
        </el-table-column>
        <el-table-column prop="clientIp" label="IP" width="140" />
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <template #empty>
        <el-empty description="暂无审计日志" />
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { auditApi } from '@/api/audit'
import type { AuditLog } from '@/api/audit'
import { formatDateTime } from '@/utils/format'

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const limit = ref(50)

onMounted(load)

async function load() {
  loading.value = true
  try {
    logs.value = (await auditApi.list(limit.value)).data
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

.table-card {
  border-radius: var(--radius-md);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
