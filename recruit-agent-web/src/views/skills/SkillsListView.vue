<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>Skills 管理中心</h2>
        <p class="page-subtitle">管理招聘 Agent 的技能：编写、版本控制、发布与匹配测试。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" v-if="auth.isAdmin" @click="showCreate = true">
          <el-icon><Plus /></el-icon>新建 Skill
        </el-button>
      </div>
    </div>

    <!-- Match test row -->
    <el-card class="detail-card">
      <template #header><span class="section-title">匹配测试</span></template>
      <div class="match-test-row">
        <el-input v-model="matchQuery" placeholder="输入一句招聘相关问题，测试会命中哪些 Skill" @keyup.enter="handleMatch" />
        <el-input-number v-model="matchLimit" :min="1" :max="20" style="width:100px" />
        <el-button type="primary" :loading="matching" @click="handleMatch">测试</el-button>
      </div>
      <div v-if="matchResults.length > 0" class="match-results">
        <div v-for="m in matchResults" :key="m.skillId" class="match-item">
          <div class="match-header">
            <el-tag size="small" type="success">{{ (m.score * 100).toFixed(0) }}%</el-tag>
            <span class="match-name">{{ m.name }}</span>
            <span class="match-category">{{ m.category }}</span>
            <span class="match-version">v{{ m.version }}</span>
          </div>
          <div class="match-reason">{{ m.reason }}</div>
        </div>
        <el-empty v-if="matchResults.length === 0 && matched" description="未匹配到任何 Skill" />
      </div>
    </el-card>

    <!-- Skills List -->
    <el-card v-loading="loading" class="detail-card">
      <template #header><span class="section-title">已注册 Skills</span></template>
      <el-table :data="skills" stripe style="width:100%">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="category" label="类别" width="110" />
        <el-table-column prop="scenario" label="场景" min-width="160" show-overflow-tooltip />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.priority >= 80 ? 'danger' : row.priority >= 50 ? 'warning' : 'info'">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.currentVersionId">v{{ row.currentVersionId }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="router.push(`/skills/${(row as any).id}`)">详情</el-button>
            <el-button v-if="(row as any).status === 'ENABLED' && auth.isAdmin" size="small" text type="warning" @click="handleDisable(row as any)">停用</el-button>
            <el-button v-if="(row as any).status === 'DISABLED' && auth.isAdmin" size="small" text type="success" @click="handlePublishLatest(row as any)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!skills.length" description="暂无 Skill" />
    </el-card>

    <!-- Import Markdown Dialog -->
    <el-dialog v-model="showImport" title="从 Markdown 导入 Skill" width="640px" :close-on-click-modal="false">
      <el-alert title="粘贴标准的招聘技能 Markdown 文档，系统将自动解析为 Skill。" type="info" :closable="false" show-icon class="import-hint" />
      <el-input v-model="importMarkdown" type="textarea" :rows="12" placeholder="粘贴 Markdown 内容…" />
      <template #footer>
        <el-button @click="showImport = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- Create Skill Dialog -->
    <el-dialog v-model="showCreate" title="新建 Skill" width="720px" :close-on-click-modal="false">
      <el-form :model="createForm" label-position="top">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" required>
              <el-input v-model="createForm.name" placeholder="例如：薪资谈判" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别" required>
              <el-select v-model="createForm.category" style="width:100%">
                <el-option label="沟通" value="沟通" />
                <el-option label="筛选" value="筛选" />
                <el-option label="面试" value="面试" />
                <el-option label="薪资" value="薪资" />
                <el-option label="关系维护" value="关系维护" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" required>
          <el-input v-model="createForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="适用场景" required>
          <el-input v-model="createForm.scenario" type="textarea" :rows="2" placeholder="描述什么场景下使用此 Skill" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="版本号" required>
              <el-input v-model="createForm.version" placeholder="例如：1.0.0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-input-number v-model="createForm.priority" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="触发关键词（逗号分隔）">
          <el-input v-model="createForm.triggerKeywordsStr" placeholder="薪资, 待遇, 期望工资" />
        </el-form-item>
        <el-form-item label="模型指令" required>
          <el-input v-model="createForm.instruction" type="textarea" :rows="4" placeholder="给模型的系统指令" />
        </el-form-item>
        <el-form-item label="Prompt 片段（可选）">
          <el-input v-model="createForm.promptFragment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Few-shots 示例（可选）">
          <el-input v-model="createForm.fewShots" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="护栏规则（可选）">
          <el-input v-model="createForm.guardrails" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="允许工具（逗号分隔）">
              <el-input v-model="createForm.allowedToolsStr" placeholder="tool1, tool2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险标签（逗号分隔）">
              <el-input v-model="createForm.riskTagsStr" placeholder="风险标签1, 风险标签2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交接策略">
              <el-input v-model="createForm.handoffPolicy" placeholder="人工交接条件" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-checkbox v-model="createForm.publish">创建后发布</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { skillApi } from '@/api/skills'
import { useAuthStore } from '@/stores/auth'
import type { RecruitmentSkill, RecruitmentSkillStatus, SkillMatch } from '@/types/skill'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const skills = ref<RecruitmentSkill[]>([])
const matching = ref(false)
const matched = ref(false)
const matchQuery = ref('')
const matchLimit = ref(5)
const matchResults = ref<SkillMatch[]>([])
const showCreate = ref(false)
const showImport = ref(false)
const creating = ref(false)
const importing = ref(false)
const importMarkdown = ref('')

const createForm = reactive({
  name: '',
  category: '沟通',
  description: '',
  scenario: '',
  version: '1.0.0',
  priority: 50,
  triggerKeywordsStr: '',
  instruction: '',
  promptFragment: '',
  fewShots: '',
  guardrails: '',
  allowedToolsStr: '',
  riskTagsStr: '',
  handoffPolicy: '',
  publish: true,
})

onMounted(loadAll)

function statusTag(s: RecruitmentSkillStatus) {
  if (s === 'ENABLED') return 'success'
  if (s === 'DISABLED') return 'info'
  return 'warning'
}

function statusLabel(s: RecruitmentSkillStatus) {
  if (s === 'ENABLED') return '已发布'
  if (s === 'DISABLED') return '已停用'
  return '草稿'
}

async function loadAll() {
  loading.value = true
  try {
    const res = await skillApi.list()
    skills.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleMatch() {
  if (!matchQuery.value.trim()) return ElMessage.warning('请输入查询内容')
  matching.value = true
  matched.value = true
  try {
    const res = await skillApi.match({ query: matchQuery.value.trim(), limit: matchLimit.value })
    matchResults.value = res.data
  } finally {
    matching.value = false
  }
}

async function handleDisable(skill: RecruitmentSkill) {
  await ElMessageBox.confirm(`确认停用 Skill「${skill.name}」？`, '停用', { type: 'warning' })
  await skillApi.disable(skill.id)
  ElMessage.success('已停用')
  await loadAll()
}

async function handlePublishLatest(skill: RecruitmentSkill) {
  const versionsRes = await skillApi.versions(skill.id)
  const published = versionsRes.data.find((v) => v.published)
  if (!published) {
    ElMessage.warning('没有已发布的版本')
    return
  }
  await skillApi.publish(skill.id, published.id)
  ElMessage.success('已启用')
  await loadAll()
}

function parseCommaList(str: string): string[] {
  return str.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
}

async function handleCreate() {
  if (!createForm.name || !createForm.description || !createForm.scenario || !createForm.version || !createForm.instruction) {
    ElMessage.warning('请填写必填字段')
    return
  }
  creating.value = true
  try {
    await skillApi.create({
      name: createForm.name,
      category: createForm.category,
      description: createForm.description,
      scenario: createForm.scenario,
      version: createForm.version,
      priority: createForm.priority,
      triggerKeywords: parseCommaList(createForm.triggerKeywordsStr),
      instruction: createForm.instruction,
      promptFragment: createForm.promptFragment || undefined,
      fewShots: createForm.fewShots || undefined,
      guardrails: createForm.guardrails || undefined,
      allowedTools: parseCommaList(createForm.allowedToolsStr),
      riskTags: parseCommaList(createForm.riskTagsStr),
      handoffPolicy: createForm.handoffPolicy || undefined,
      publish: createForm.publish,
    })
    ElMessage.success('Skill 创建成功')
    showCreate.value = false
    Object.assign(createForm, {
      name: '', category: '沟通', description: '', scenario: '', version: '1.0.0',
      priority: 50, triggerKeywordsStr: '', instruction: '', promptFragment: '',
      fewShots: '', guardrails: '', allowedToolsStr: '', riskTagsStr: '', handoffPolicy: '', publish: true,
    })
    await loadAll()
  } finally {
    creating.value = false
  }
}

async function handleImport() {
  if (!importMarkdown.value.trim()) return ElMessage.warning('请粘贴 Markdown 内容')
  importing.value = true
  try {
    await skillApi.importMarkdown({ markdown: importMarkdown.value })
    ElMessage.success('从 Markdown 导入成功')
    showImport.value = false
    importMarkdown.value = ''
    await loadAll()
  } finally {
    importing.value = false
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
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-card {
  margin-bottom: 20px;
}

.match-test-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.match-test-row .el-input {
  flex: 1;
}

.match-results {
  margin-top: 14px;
}

.match-item {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
  background: var(--color-surface);
}

.match-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.match-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.match-category {
  font-size: 0.78rem;
  color: var(--color-text-muted);
  background: var(--color-bg);
  padding: 1px 8px;
  border-radius: 100px;
}

.match-version {
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.match-reason {
  font-size: 0.82rem;
  color: var(--color-text-secondary);
  padding-left: 4px;
}

.text-muted {
  color: var(--color-text-muted);
}

.import-hint {
  margin-bottom: 12px;
}
</style>
