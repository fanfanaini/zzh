<template>
  <div class="screening-result-card">
    <div class="result-header">
      <div class="result-score">
        <div :class="['score-gauge', scoreLevel]" style="width: 56px; height: 56px; font-size: 1.05rem;">
          {{ Math.round(result.score.finalScore) }}
        </div>
        <div class="rec-badge-wrapper">
          <div :class="['badge-rec', recommendationClass(result.recommendation)]" style="font-size: 0.85rem;">
            <span>{{ recommendationLabel(result.recommendation) }}</span>
          </div>
          <div class="confidence-text">综合评分 {{ Math.round(result.score.finalScore) }}/100</div>
        </div>
      </div>
    </div>

    <div class="breakdown">
      <div class="breakdown-item" v-for="item in breakdownItems" :key="item.label">
        <div class="breakdown-label">
          <span>{{ item.label }}</span>
          <span class="breakdown-pct">权重 {{ item.weight }}%</span>
        </div>
        <el-progress :percentage="Math.round(item.score)" :color="progressColor(item.score)" :stroke-width="6" />
      </div>
    </div>

    <div class="analysis-section" v-if="result.advantages?.length">
      <div class="analysis-title">
        <el-icon color="var(--color-success)"><CircleCheck /></el-icon>优势
      </div>
      <ul>
        <li v-for="item in result.advantages" :key="item">{{ item }}</li>
      </ul>
    </div>

    <div class="analysis-section" v-if="result.risks?.length">
      <div class="analysis-title">
        <el-icon color="var(--color-danger)"><WarningFilled /></el-icon>风险
      </div>
      <ul>
        <li v-for="item in result.risks" :key="item">{{ item }}</li>
      </ul>
    </div>

    <div class="analysis-section" v-if="result.missingInformation?.length">
      <div class="analysis-title">
        <el-icon color="var(--color-warning)"><InfoFilled /></el-icon>缺失信息
      </div>
      <ul>
        <li v-for="item in result.missingInformation" :key="item">{{ item }}</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import type { ScreeningResult } from '@/types/screening'
import { recommendationClass, recommendationLabel } from '@/utils/format'

const props = defineProps<{ result: ScreeningResult }>()

const scoreLevel = computed(() => {
  const score = props.result.score.finalScore
  if (score >= 80) return 'high'
  if (score >= 60) return 'mid'
  return 'low'
})

const breakdownItems = computed(() => [
  { label: '必须技能', score: props.result.score.skillScore, weight: 40 },
  { label: '项目经历', score: props.result.score.projectScore, weight: 25 },
  { label: '工作经验', score: props.result.score.experienceScore, weight: 15 },
  { label: '学历', score: props.result.score.educationScore, weight: 10 },
  { label: '加分技能', score: props.result.score.bonusScore, weight: 10 },
])

function progressColor(score: number) {
  if (score >= 80) return 'var(--color-success)'
  if (score >= 60) return 'var(--color-warning)'
  return 'var(--color-danger)'
}
</script>

<style scoped>
.screening-result-card {
  margin-top: 16px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.result-header {
  margin-bottom: 16px;
}

.result-score {
  display: flex;
  align-items: center;
  gap: 16px;
}

.rec-badge-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.confidence-text {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.breakdown {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
}

.breakdown-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.breakdown-label {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  color: var(--color-text-regular);
}

.breakdown-pct {
  color: var(--color-text-muted);
  font-size: 0.8rem;
}

.analysis-section {
  margin-bottom: 12px;
}

.analysis-section:last-child {
  margin-bottom: 0;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

ul {
  margin: 0;
  padding-left: 20px;
}

li {
  font-size: 0.85rem;
  color: var(--color-text-regular);
  line-height: 1.8;
}
</style>
