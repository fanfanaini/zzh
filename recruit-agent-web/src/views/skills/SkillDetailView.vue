<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="router.push('/skills')">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2>Skill 详情</h2>
        <span v-if="skill" class="skill-name-title">{{ skill.name }}</span>
      </div>
      <div class="header-actions" v-if="skill && auth.isAdmin">
        <el-button type="primary" :loading="publishing" @click="handlePublish" v-if="hasUnpublishedVersion">发布最新版本</el-button>
        <el-button type="warning" plain @click="handleDisable" v-if="skill.status === 'ENABLED'">停用</el-button>
        <el-button @click="showAddVersion = true">新增版本</el-button>
      </div>
    </div>

    <div v-loading="loading" v-if="skill">
      <el-card class="detail-card">
        <template #header><span class="section-title">基本信息</span></template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="名称">{{ skill.name }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ skill.category }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ skill.priority }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="skill.status === 'ENABLED' ? 'success' : skill.status === 'DISABLED' ? 'info' : 'warning'">
              {{ skill.status === 'ENABLED' ? '已发布' : skill.status === 'DISABLED' ? '已停用' : '草稿' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(skill.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(skill.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
        <div class="skill-desc-section">
          <div class="desc-block">
            <span class="desc-label">描述</span>
            <p>{{ skill.description }}</p>
          </div>
          <div class="desc-block">
            <span class="desc-label">场景</span>
            <p>{{ skill.scenario }}</p>
          </div>
          <div class="desc-block" v-if="skill.triggerKeywords?.length">
            <span class="desc-label">触发关键词</span>
            <el-tag v-for="kw in skill.triggerKeywords" :key="kw" size="small" class="kw-tag">{{ kw }}</el-tag>
          </div>
        </div>
      </el-card>

      <!-- Version History -->
      <el-card class="detail-card">
        <template #header><span class="section-title">版本历史</span></template>
        <el-table :data="versions" stripe style="width:100%">
          <el-table-column prop="version" label="版本" width="100" />
          <el-table-column label="已发布" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.published ? 'success' : 'info'">
                {{ row.published ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="允许工具" min-width="150">
            <template #default="{ row }">
              <el-tag v-for="t in row.allowedTools" :key="t" size="small" class="kw-tag">{{ t }}</el-tag>
              <span v-if="!row.allowedTools?.length" class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="风险标签" min-width="120">
            <template #default="{ row }">
              <el-tag v-for="t in row.riskTags" :key="t" size="small" type="danger" class="kw-tag">{{ t }}</el-tag>
              <span v-if="!row.riskTags?.length" class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="handoffPolicy" label="交接策略" min-width="120" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="viewVersion(row as any)">查看</el-button>
              <el-button
                v-if="!(row as any).published && auth.isAdmin"
                size="small"
                text
                type="success"
                @click="handlePublishVersion(row as any)"
              >发布</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!versions.length" description="暂无版本" />
      </el-card>
    </div>
    <el-empty v-else-if="!loading" description="Skill 不存在" />

    <!-- Version Detail Drawer -->
    <el-drawer v-model="versionDrawer" title="版本详情" size="min(700px, 92vw)">
      <template v-if="selectedVersion">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="版本号">{{ selectedVersion.version }}</el-descriptions-item>
          <el-descriptions-item label="已发布">
            <el-tag size="small" :type="selectedVersion.published ? 'success' : 'info'">{{ selectedVersion.published ? '是' : '否' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(selectedVersion.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <section class="version-section">
          <h4>Instruction（模型指令）</h4>
          <pre>{{ selectedVersion.instruction }}</pre>
        </section>
        <section class="version-section" v-if="selectedVersion.promptFragment">
          <h4>Prompt 片段</h4>
          <pre>{{ selectedVersion.promptFragment }}</pre>
        </section>
        <section class="version-section" v-if="selectedVersion.fewShots">
          <h4>Few-shots 示例</h4>
          <pre>{{ selectedVersion.fewShots }}</pre>
        </section>
        <section class="version-section" v-if="selectedVersion.guardrails">
          <h4>护栏规则</h4>
          <pre>{{ selectedVersion.guardrails }}</pre>
        </section>
        <div class="version-tags" v-if="selectedVersion.allowedTools?.length || selectedVersion.riskTags?.length">
          <div>
            <strong>允许工具：</strong>
            <el-tag v-for="t in selectedVersion.allowedTools" :key="t" size="small" class="kw-tag">{{ t }}</el-tag>
            <span v-if="!selectedVersion.allowedTools?.length" class="text-muted">无</span>
          </div>
          <div>
            <strong>风险标签：</strong>
            <el-tag v-for="t in selectedVersion.riskTags" :key="t" size="small" type="danger" class="kw-tag">{{ t }}</el-tag>
            <span v-if="!selectedVersion.riskTags?.length" class="text-muted">无</span>
          </div>
          <div v-if="selectedVersion.handoffPolicy">
            <strong>交接策略：</strong>{{ selectedVersion.handoffPolicy }}
          </div>
        </div>
      </template>
    </el-drawer>

    <!-- Add Version Dialog -->
    <el-dialog v-model="showAddVersion" title="新增版本" width="640px" :close-on-click-modal="false">
      <el-form :model="versionForm" label-position="top">
        <el-form-item label="版本号" required>
          <el-input v-model="versionForm.version" placeholder="例如：2.0.0" />
        </el-form-item>
        <el-form-item label="模型指令" required>
          <el-input v-model="versionForm.instruction" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="Prompt 片段">
          <el-input v-model="versionForm.promptFragment" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Few-shots 示例">
          <el-input v-model="versionForm.fewShots" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="护栏规则">
          <el-input v-model="versionForm.guardrails" type="textarea" :rows="2" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="允许工具">
              <el-input v-model="versionForm.allowedToolsStr" placeholder="逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险标签">
              <el-input v-model="versionForm.riskTagsStr" placeholder="逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交接策略">
              <el-input v-model="versionForm.handoffPolicy" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-checkbox v-model="versionForm.publish">创建后发布</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddVersion = false">取消</el-button>
        <el-button type="primary" :loading="creatingVersion" @click="handleAddVersion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { skillApi } from '@/api/skills'
import { useAuthStore } from '@/stores/auth'
import type { RecruitmentSkill, RecruitmentSkillVersion } from '@/types/skill'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const skillId = Number(route.params.id)

const loading = ref(false)
const publishing = ref(false)
const skill = ref<RecruitmentSkill | null>(null)
const versions = ref<RecruitmentSkillVersion[]>([])
const versionDrawer = ref(false)
const selectedVersion = ref<RecruitmentSkillVersion | null>(null)
const showAddVersion = ref(false)
const creatingVersion = ref(false)

const versionForm = reactive({
  version: '',
  instruction: '',
  promptFragment: '',
  fewShots: '',
  guardrails: '',
  allowedToolsStr: '',
  riskTagsStr: '',
  handoffPolicy: '',
  publish: true,
})

const hasUnpublishedVersion = computed(() => versions.value.some((v) => !v.published))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [skillRes, versionsRes] = await Promise.all([
      skillApi.detail(skillId),
      skillApi.versions(skillId),
    ])
    skill.value = skillRes.data
    versions.value = versionsRes.data
  } finally {
    loading.value = false
  }
}

function viewVersion(v: RecruitmentSkillVersion) {
  selectedVersion.value = v
  versionDrawer.value = true
}

async function handlePublish() {
  const unpub = versions.value.find((v) => !v.published)
  if (!unpub) return ElMessage.warning('没有待发布的版本')
  await handlePublishVersion(unpub)
}

async function handlePublishVersion(v: RecruitmentSkillVersion) {
  publishing.value = true
  try {
    await skillApi.publish(skillId, v.id)
    ElMessage.success('版本已发布')
    await loadAll()
  } finally {
    publishing.value = false
  }
}

async function handleDisable() {
  if (!skill.value) return
  await ElMessageBox.confirm(`确认停用 Skill「${skill.value.name}」？`, '停用', { type: 'warning' })
  await skillApi.disable(skill.value.id)
  ElMessage.success('已停用')
  await loadAll()
}

function parseList(str: string): string[] {
  return str.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
}

async function handleAddVersion() {
  if (!versionForm.version || !versionForm.instruction) {
    ElMessage.warning('请填写版本号和指令')
    return
  }
  creatingVersion.value = true
  try {
    await skillApi.addVersion(skillId, {
      version: versionForm.version,
      instruction: versionForm.instruction,
      promptFragment: versionForm.promptFragment || undefined,
      fewShots: versionForm.fewShots || undefined,
      guardrails: versionForm.guardrails || undefined,
      allowedTools: parseList(versionForm.allowedToolsStr),
      riskTags: parseList(versionForm.riskTagsStr),
      handoffPolicy: versionForm.handoffPolicy || undefined,
      publish: versionForm.publish,
    })
    ElMessage.success('版本已添加')
    showAddVersion.value = false
    Object.assign(versionForm, {
      version: '', instruction: '', promptFragment: '', fewShots: '',
      guardrails: '', allowedToolsStr: '', riskTagsStr: '', handoffPolicy: '', publish: true,
    })
    await loadAll()
  } finally {
    creatingVersion.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.skill-name-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-accent);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.detail-card {
  margin-bottom: 20px;
}

.skill-desc-section {
  margin-top: 14px;
}

.desc-block {
  margin-bottom: 12px;
}

.desc-label {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  display: block;
  margin-bottom: 4px;
}

.desc-block p {
  font-size: 0.9rem;
  color: var(--color-text-regular);
  line-height: 1.6;
  margin: 0;
}

.kw-tag {
  margin: 2px 4px 2px 0;
}

.text-muted {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.version-section {
  margin-top: 18px;
}

.version-section h4 {
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.version-section pre {
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
}

.version-tags {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 0.85rem;
}
</style>
