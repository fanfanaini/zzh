<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div class="header-title">
        <el-button text @click="router.go(-1)">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <div>
          <h2>评估详情</h2>
          <p v-if="evaluation" class="page-subtitle">评估 #{{ evaluation.id }} · {{ formatDateTime(evaluation.createdAt) }}</p>
        </div>
      </div>
    </div>

    <el-empty v-if="!evaluation && !loading" description="评估记录不存在" />

    <el-row :gutter="20" v-if="evaluation">
      <el-col :span="16">
        <el-card class="detail-card">
          <template #header>
            <span class="section-title">评分总览</span>
          </template>
          <div class="eval-header">
            <div :class="['score-gauge', scoreLevel]" style="width: 72px; height: 72px; font-size: 1.4rem;">
              {{ Math.round(evaluation.finalScore || 0) }}
            </div>
            <div class="eval-meta">
              <div :class="['badge-rec', recommendationClass(evaluation.recommendation)]" style="font-size: 1rem; padding: 4px 16px;">
                {{ recommendationLabel(evaluation.recommendation) }}
              </div>
              <div class="eval-confidence" v-if="evaluation.confidence">
                置信度 {{ (evaluation.confidence * 100).toFixed(1) }}%
              </div>
              <div class="eval-time">岗位 #{{ evaluation.jobId }} · 候选人 #{{ evaluation.candidateId }}</div>
            </div>
          </div>

          <div class="score-grid">
            <div class="score-item">
              <div class="score-item-label">规则评分</div>
              <div class="score-item-value">{{ evaluation.ruleScore ?? '-' }}</div>
            </div>
            <div class="score-item">
              <div class="score-item-label">语义评分</div>
              <div class="score-item-value">{{ evaluation.semanticScore ?? '-' }}</div>
            </div>
            <div class="score-item">
              <div class="score-item-label">最终评分</div>
              <div class="score-item-value highlight">{{ Math.round(evaluation.finalScore || 0) }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="detail-card" v-if="evaluation.advantages?.length">
          <template #header>
            <span class="section-title"><el-icon color="var(--color-success)"><CircleCheck /></el-icon>优势</span>
          </template>
          <ul class="analysis-list">
            <li v-for="item in evaluation.advantages" :key="item">{{ item }}</li>
          </ul>
        </el-card>

        <el-card class="detail-card" v-if="evaluation.risks?.length">
          <template #header>
            <span class="section-title"><el-icon color="var(--color-danger)"><WarningFilled /></el-icon>风险</span>
          </template>
          <ul class="analysis-list">
            <li v-for="item in evaluation.risks" :key="item">{{ item }}</li>
          </ul>
        </el-card>

        <el-card class="detail-card" v-if="evaluation.missingInformation?.length">
          <template #header>
            <span class="section-title"><el-icon color="var(--color-warning)"><InfoFilled /></el-icon>缺失信息</span>
          </template>
          <ul class="analysis-list">
            <li v-for="item in evaluation.missingInformation" :key="item">{{ item }}</li>
          </ul>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="detail-card">
          <template #header>
            <span class="section-title">评估元信息</span>
          </template>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="评估 ID">{{ evaluation.id }}</el-descriptions-item>
            <el-descriptions-item label="岗位 ID">
              <router-link :to="`/jobs/${evaluation.jobId}`">{{ evaluation.jobId }}</router-link>
            </el-descriptions-item>
            <el-descriptions-item label="候选人 ID">
              <router-link :to="`/candidates/${evaluation.candidateId}/resumes`">{{ evaluation.candidateId }}</router-link>
            </el-descriptions-item>
            <el-descriptions-item label="模型">{{ evaluation.modelName || '规则引擎' }}</el-descriptions-item>
            <el-descriptions-item label="Prompt 版本">{{ evaluation.promptVersion || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="detail-card">
          <template #header>
            <span class="section-title">人工复核</span>
          </template>
          <template v-if="evaluation.humanResult">
            <div :class="['human-result', evaluation.humanResult.toLowerCase()]">
              {{ humanResultLabel(evaluation.humanResult) }}
            </div>
            <p class="hint-text">该评估已由 HR 复核确认。</p>
          </template>
          <template v-else>
            <p class="hint-text">AI 已给出建议，请由人工确认最终处理结果。</p>
            <div class="review-actions">
              <el-button type="success" :loading="reviewing" @click="humanReview('ACCEPT')">
                <el-icon><CircleCheck /></el-icon>通过，推荐面试
              </el-button>
              <el-button type="danger" :loading="reviewing" @click="humanReview('REJECT')">
                <el-icon><CloseBold /></el-icon>淘汰，不匹配
              </el-button>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CircleCheck, CloseBold, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { screeningApi } from '@/api/screening'
import type { Evaluation } from '@/types/screening'
import { formatDateTime, recommendationClass, recommendationLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const evaluation = ref<Evaluation | null>(null)
const loading = ref(true)
const reviewing = ref(false)

onMounted(loadEvaluation)

async function loadEvaluation() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    evaluation.value = (await screeningApi.getEvaluation(id)).data
  } catch {
    evaluation.value = null
  } finally {
    loading.value = false
  }
}

const scoreLevel = computed(() => {
  const score = evaluation.value?.finalScore || 0
  if (score >= 80) return 'high'
  if (score >= 60) return 'mid'
  return 'low'
})

function humanResultLabel(value: string) {
  return value === 'ACCEPT' ? '已通过' : value === 'REJECT' ? '已淘汰' : '待复核'
}

async function humanReview(result: 'ACCEPT' | 'REJECT') {
  if (!evaluation.value) return
  reviewing.value = true
  try {
    await screeningApi.humanReview(evaluation.value.id, { humanResult: result })
    ElMessage.success(result === 'ACCEPT' ? '已通过该候选人' : '已淘汰该候选人')
    await loadEvaluation()
  } finally {
    reviewing.value = false
  }
}
</script>

<style scoped>
.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-subtitle,
.hint-text,
.eval-confidence,
.eval-time {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.detail-card {
  margin-bottom: 20px;
}

.eval-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.eval-meta,
.review-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.score-item {
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  text-align: center;
}

.score-item-label {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.score-item-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.score-item-value.highlight {
  color: var(--color-accent);
}

.analysis-list {
  margin: 0;
  padding-left: 20px;
}

.analysis-list li {
  font-size: 0.9rem;
  color: var(--color-text-regular);
  line-height: 2;
}

.human-result {
  text-align: center;
  font-size: 1rem;
  font-weight: 600;
  padding: 14px;
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
}

.human-result.accept {
  background: var(--color-success-bg);
  color: oklch(35% 0.12 150);
}

.human-result.reject {
  background: var(--color-danger-bg);
  color: oklch(40% 0.14 30);
}
</style>
