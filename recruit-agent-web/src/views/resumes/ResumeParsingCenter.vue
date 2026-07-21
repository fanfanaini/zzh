<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>简历解析</h2>
        <p class="page-subtitle">集中上传、解析和追踪所有候选人的简历处理状态。</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadAll">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
        <el-button type="primary" :disabled="!selectedRows.length" :loading="batchParsing" @click="parseSelected">
          <el-icon><MagicStick /></el-icon>解析选中
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">简历总数</div>
          <div class="stat-value">{{ resumes.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">待解析</div>
          <div class="stat-value warning">{{ countByStatus('UPLOADED') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">已解析</div>
          <div class="stat-value success">{{ countByStatus('PARSED') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-label">失败</div>
          <div class="stat-value danger">{{ countByStatus('FAILED') }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="panel-card">
          <template #header>
            <span class="section-title">上传入口</span>
          </template>

          <el-segmented v-model="uploadMode" :options="uploadModeOptions" class="mode-switch" />

          <el-form label-position="top" class="upload-form">
            <template v-if="uploadMode === 'existing'">
              <el-form-item label="绑定候选人">
                <el-select v-model="selectedCandidateId" filterable placeholder="选择已有候选人" style="width: 100%">
                  <el-option
                    v-for="candidate in candidates"
                    :key="candidate.id"
                    :label="`${candidate.name} · ${educationLabel(candidate.educationLevel)} · ${candidate.totalExperienceYears} 年`"
                    :value="candidate.id"
                  />
                </el-select>
              </el-form-item>
            </template>

            <template v-else>
              <el-form-item label="候选人姓名">
                <el-input v-model="quickCandidate.name" placeholder="例如：张三" />
              </el-form-item>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="学历">
                    <el-select v-model="quickCandidate.educationLevel" style="width: 100%">
                      <el-option label="不限" value="UNKNOWN" />
                      <el-option label="大专" value="COLLEGE" />
                      <el-option label="本科" value="BACHELOR" />
                      <el-option label="硕士" value="MASTER" />
                      <el-option label="博士" value="DOCTOR" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="经验">
                    <el-input-number v-model="quickCandidate.totalExperienceYears" :min="0" :max="50" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="期望城市">
                <el-input v-model="quickCandidate.expectedCity" placeholder="例如：成都" />
              </el-form-item>
            </template>

            <el-form-item label="简历文件">
              <el-upload
                drag
                multiple
                accept=".pdf,.doc,.docx,.txt,.md"
                :auto-upload="false"
                :show-file-list="true"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
              >
                <el-icon class="upload-icon" :size="36"><UploadFilled /></el-icon>
                <div class="upload-text">拖拽文件到此处，或 <em>点击选择</em></div>
                <template #tip>
                  <div class="upload-hint">支持 PDF、DOCX、DOC、TXT、MD。ZIP 导入需要后端另增接口。</div>
                </template>
              </el-upload>
            </el-form-item>

            <el-checkbox v-model="parseAfterUpload">上传后立即解析</el-checkbox>
            <el-button type="primary" class="upload-button" :loading="uploading" @click="upload">
              上传{{ parseAfterUpload ? '并解析' : '' }}
            </el-button>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card class="panel-card" v-loading="loading">
          <template #header>
            <div class="table-header">
              <span class="section-title">解析队列</span>
              <el-input v-model="keyword" clearable placeholder="搜索候选人或文件名" style="width: 240px" />
            </div>
          </template>

          <el-table
            :data="filteredRows"
            stripe
            style="width: 100%"
            @selection-change="selectedRows = $event"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column prop="originalFilename" label="文件" min-width="220">
              <template #default="{ row }">
                <div class="file-cell">
                  <el-icon><Document /></el-icon>
                  <div>
                    <div class="file-name">{{ row.originalFilename }}</div>
                    <div class="file-meta">{{ (row.fileSize / 1024).toFixed(1) }} KB · {{ formatDateTime(row.uploadedAt) }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="candidateName" label="候选人" width="150">
              <template #default="{ row }">
                <router-link :to="`/candidates/${row.candidateId}/resumes`">{{ row.candidateName }}</router-link>
              </template>
            </el-table-column>
            <el-table-column prop="parseStatus" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tooltip :disabled="!row.parseErrorMessage" :content="row.parseErrorMessage" placement="top">
                  <span :class="['status-tag', statusClass(row.parseStatus)]">{{ resumeStatusLabel(row.parseStatus) }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="310" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :loading="parsingId === row.id" @click="parseOne(row)">解析</el-button>
                <el-button size="small" @click="openProfile(row)">详情</el-button>
                <el-button size="small" @click="downloadResume(row)">下载</el-button>
                <el-button size="small" type="primary" @click="goScreening(row)">筛选</el-button>
                <el-button size="small" type="danger" text @click="deleteResume(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <template #empty>
            <el-empty description="还没有简历，先在左侧上传一份。" />
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="drawerOpen" title="简历详情" size="min(760px, 92vw)">
      <template v-if="activeCandidate">
        <el-alert
          v-if="activeResume?.parseStatus === 'FAILED' && activeResume.parseErrorMessage"
          :title="activeResume.parseErrorMessage"
          type="error"
          :closable="false"
          show-icon
          class="parse-error"
        >
          <template #default>
            <el-button
              size="small"
              type="danger"
              plain
              :loading="parsingId === activeResume.id"
              @click="parseOne(activeResume)"
            >
              重新解析
            </el-button>
          </template>
        </el-alert>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="姓名">{{ activeCandidate.name }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ educationLabel(activeCandidate.educationLevel) }}</el-descriptions-item>
          <el-descriptions-item label="经验">{{ activeCandidate.totalExperienceYears }} 年</el-descriptions-item>
          <el-descriptions-item label="期望城市">{{ activeCandidate.expectedCity || '-' }}</el-descriptions-item>
          <el-descriptions-item label="技能">
            <el-tag v-for="skill in activeCandidate.skills" :key="skill" size="small" class="skill-tag">
              {{ skill }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="activeResume" class="resume-detail-actions">
          <el-button size="small" type="primary" :loading="savingDetail" @click="saveStructuredDetail">保存人工修正</el-button>
          <el-button size="small" @click="downloadResume(activeResume)">下载原文件</el-button>
          <el-button size="small" type="danger" text @click="deleteResume(activeResume)">删除简历</el-button>
        </div>
        <el-tabs v-if="activeResume" class="resume-tabs">
          <el-tab-pane label="结构化详情">
            <section class="structured-section">
              <div class="detail-section-title">
                <span>教育经历</span>
                <el-button size="small" text type="primary" @click="structuredDetail.educationExperiences.push(emptyEducation())">添加</el-button>
              </div>
              <div v-for="(item, index) in structuredDetail.educationExperiences" :key="`edu-${index}`" class="detail-editor-row">
                <el-input v-model="item.school" placeholder="学校" />
                <el-input v-model="item.degree" placeholder="学历" />
                <el-input v-model="item.major" placeholder="专业" />
                <el-input v-model="item.startDate" placeholder="开始时间" />
                <el-input v-model="item.endDate" placeholder="结束时间" />
                <el-button text type="danger" @click="structuredDetail.educationExperiences.splice(index, 1)">删除</el-button>
                <div class="extraction-meta">
                  <el-tag size="small" type="info">置信度 {{ confidenceLabel(item.confidence) }}</el-tag>
                  <span>{{ item.sourceText || '暂无原文依据' }}</span>
                </div>
              </div>

              <div class="detail-section-title">
                <span>工作经历</span>
                <el-button size="small" text type="primary" @click="structuredDetail.workExperiences.push(emptyWork())">添加</el-button>
              </div>
              <div v-for="(item, index) in structuredDetail.workExperiences" :key="`work-${index}`" class="detail-editor-block">
                <div class="detail-editor-row">
                  <el-input v-model="item.company" placeholder="公司" />
                  <el-input v-model="item.title" placeholder="职位" />
                  <el-input v-model="item.startDate" placeholder="开始时间" />
                  <el-input v-model="item.endDate" placeholder="结束时间" />
                  <el-button text type="danger" @click="structuredDetail.workExperiences.splice(index, 1)">删除</el-button>
                </div>
                <el-input v-model="item.description" type="textarea" :rows="2" placeholder="工作描述" />
                <div class="extraction-meta">
                  <el-tag size="small" type="info">置信度 {{ confidenceLabel(item.confidence) }}</el-tag>
                  <span>{{ item.sourceText || '暂无原文依据' }}</span>
                </div>
              </div>

              <div class="detail-section-title">
                <span>项目经历</span>
                <el-button size="small" text type="primary" @click="structuredDetail.projectExperiences.push(emptyProject())">添加</el-button>
              </div>
              <div v-for="(item, index) in structuredDetail.projectExperiences" :key="`project-${index}`" class="detail-editor-block">
                <div class="detail-editor-row">
                  <el-input v-model="item.name" placeholder="项目名称" />
                  <el-input v-model="item.role" placeholder="角色" />
                  <el-input v-model="item.startDate" placeholder="开始时间" />
                  <el-input v-model="item.endDate" placeholder="结束时间" />
                  <el-button text type="danger" @click="structuredDetail.projectExperiences.splice(index, 1)">删除</el-button>
                </div>
                <el-input v-model="item.description" type="textarea" :rows="2" placeholder="项目描述" />
                <div class="extraction-meta">
                  <el-tag size="small" type="info">置信度 {{ confidenceLabel(item.confidence) }}</el-tag>
                  <span>{{ item.sourceText || '暂无原文依据' }}</span>
                </div>
              </div>

              <el-form label-position="top" class="tag-edit-form">
                <el-form-item label="证书">
                  <el-select
                    v-model="structuredDetail.certificates"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="输入证书名称后按回车添加"
                    no-data-text="输入证书名称后按回车添加"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="语言">
                  <el-select
                    v-model="structuredDetail.languages"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="输入语言能力后按回车添加"
                    no-data-text="输入语言能力后按回车添加"
                    style="width: 100%"
                  />
                </el-form-item>
                <el-form-item label="奖项">
                  <el-select
                    v-model="structuredDetail.awards"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="输入奖项名称后按回车添加"
                    no-data-text="输入奖项名称后按回车添加"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-form>

              <div v-if="Object.keys(structuredDetail.fieldSources).length" class="field-evidence">
                <div class="detail-section-title"><span>字段来源与置信度</span></div>
                <el-table :data="fieldEvidenceRows" size="small" border>
                  <el-table-column prop="field" label="字段" min-width="140" />
                  <el-table-column prop="source" label="原文依据" min-width="240" show-overflow-tooltip />
                  <el-table-column label="置信度" width="100">
                    <template #default="{ row }">{{ confidenceLabel(row.confidence) }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </section>
          </el-tab-pane>
          <el-tab-pane label="原文">
            <section class="raw-section">
              <div class="raw-title">{{ activeResume.originalFilename }}</div>
              <pre class="raw-text">{{ activeResume.rawText || '暂无提取文本，可先执行解析或仅提取文本。' }}</pre>
            </section>
          </el-tab-pane>
        </el-tabs>
      </template>
      <el-empty v-else description="暂无画像，请先解析简历。" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Document, MagicStick, Refresh, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { candidateApi, resumeApi } from '@/api/candidates'
import type { Candidate, ResumeDocument, ResumeParseStatus, ResumeStructuredDetail } from '@/types/candidate'
import type { PageResponse } from '@/types/api'
import { educationLabel, formatDateTime, resumeStatusLabel } from '@/utils/format'

interface ResumeRow extends ResumeDocument {
  candidateName: string
}

const router = useRouter()
const candidates = ref<Candidate[]>([])
const resumes = ref<ResumeRow[]>([])
const selectedRows = ref<ResumeRow[]>([])
const selectedFiles = ref<File[]>([])
const uploadMode = ref<'existing' | 'quick'>('existing')
const selectedCandidateId = ref<number | null>(null)
const parseAfterUpload = ref(true)
const keyword = ref('')
const loading = ref(false)
const uploading = ref(false)
const batchParsing = ref(false)
const parsingId = ref<number | null>(null)
const drawerOpen = ref(false)
const activeCandidate = ref<Candidate | null>(null)
const activeResume = ref<ResumeRow | null>(null)
const savingDetail = ref(false)
const structuredDetail = reactive<ResumeStructuredDetail>(emptyStructuredDetail())
const fieldEvidenceRows = computed(() =>
  Object.entries(structuredDetail.fieldSources).map(([field, source]) => ({
    field,
    source,
    confidence: structuredDetail.fieldConfidence[field] ?? null,
  })),
)

const quickCandidate = reactive({
  name: '',
  educationLevel: 'BACHELOR',
  totalExperienceYears: 0,
  expectedCity: '',
})

const uploadModeOptions = [
  { label: '绑定已有候选人', value: 'existing' },
  { label: '快速创建候选人', value: 'quick' },
]

const filteredRows = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return resumes.value
  return resumes.value.filter((row) =>
    row.originalFilename.toLowerCase().includes(term) || row.candidateName.toLowerCase().includes(term),
  )
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const candidateData = (await candidateApi.list()).data as Candidate[] | PageResponse<Candidate>
    candidates.value = Array.isArray(candidateData) ? candidateData : candidateData.items
    const grouped = await Promise.all(
      candidates.value.map(async (candidate) => {
        const list = (await candidateApi.listResumes(candidate.id)).data
        return list.map((resume) => ({ ...resume, candidateName: candidate.name }))
      }),
    )
    resumes.value = grouped.flat().sort((left, right) =>
      new Date(right.uploadedAt).getTime() - new Date(left.uploadedAt).getTime(),
    )
    if (!selectedCandidateId.value && candidates.value.length) {
      selectedCandidateId.value = candidates.value[0].id
    }
  } finally {
    loading.value = false
  }
}

function countByStatus(status: ResumeParseStatus) {
  return resumes.value.filter((resume) => resume.parseStatus === status).length
}

function confidenceLabel(confidence: number | null | undefined) {
  return confidence == null ? '-' : `${Math.round(confidence * 100)}%`
}

function handleFileChange(uploadFile: { raw?: File }) {
  if (uploadFile.raw && !selectedFiles.value.some((file) => file.name === uploadFile.raw?.name)) {
    selectedFiles.value.push(uploadFile.raw)
  }
}

function handleFileRemove(uploadFile: { name: string }) {
  selectedFiles.value = selectedFiles.value.filter((file) => file.name !== uploadFile.name)
}

async function resolveCandidateId() {
  if (uploadMode.value === 'existing') {
    if (!selectedCandidateId.value) throw new Error('请选择候选人')
    return selectedCandidateId.value
  }
  if (!quickCandidate.name.trim()) throw new Error('请输入候选人姓名')
  const created = await candidateApi.register({
    name: quickCandidate.name.trim(),
    educationLevel: quickCandidate.educationLevel,
    totalExperienceYears: quickCandidate.totalExperienceYears,
    expectedCity: quickCandidate.expectedCity,
    skills: [],
  })
  candidates.value.unshift(created.data)
  selectedCandidateId.value = created.data.id
  return created.data.id
}

async function upload() {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请选择简历文件')
    return
  }
  uploading.value = true
  try {
    const candidateId = await resolveCandidateId()
    const res = selectedFiles.value.length === 1
      ? await candidateApi.uploadResume(candidateId, selectedFiles.value[0])
      : await candidateApi.batchUploadResumes(candidateId, selectedFiles.value)
    const uploaded = Array.isArray(res.data) ? res.data : [res.data]
    selectedFiles.value = []
    if (parseAfterUpload.value) {
      for (const resume of uploaded) {
        await resumeApi.submitParseTask(resume.id)
      }
      ElMessage.success('上传完成，解析任务已提交')
    } else {
      ElMessage.success('上传成功')
    }
    await loadAll()
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function parseOne(row: any) {
  parsingId.value = row.id
  try {
    await resumeApi.submitParseTask(row.id)
    const currentResume = activeResume.value
    if (currentResume && currentResume.id === row.id) {
      currentResume.parseStatus = 'PARSING'
      currentResume.parseErrorMessage = undefined
    }
    ElMessage.success(`${row.originalFilename} 已提交后台解析`)
    await loadAll()
  } finally {
    parsingId.value = null
  }
}

async function parseSelected() {
  if (!selectedRows.value.length) return
  batchParsing.value = true
  try {
    for (const row of selectedRows.value) {
      await resumeApi.submitParseTask(row.id)
    }
    ElMessage.success('选中简历已提交后台解析')
    await loadAll()
  } finally {
    batchParsing.value = false
  }
}

async function openProfile(row: any) {
  const detail = (await resumeApi.detail(row.id)).data
  activeResume.value = { ...row, ...detail.resume }
  Object.assign(structuredDetail, normalizeStructuredDetail(detail.structuredDetail))
  activeCandidate.value = (await candidateApi.get(row.candidateId)).data
  drawerOpen.value = true
}

function goScreening(row: any) {
  router.push({ path: '/screening', query: { candidateId: row.candidateId } })
}

async function downloadResume(row: any) {
  const response = await resumeApi.download(row.id)
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = row.originalFilename
  anchor.click()
  URL.revokeObjectURL(url)
}

async function deleteResume(row: any) {
  await ElMessageBox.confirm(`确认删除 ${row.originalFilename}？`, '删除简历', { type: 'warning' })
  await resumeApi.delete(row.id)
  ElMessage.success('简历已删除')
  if (activeResume.value?.id === row.id) {
    drawerOpen.value = false
    activeResume.value = null
  }
  await loadAll()
}

async function saveStructuredDetail() {
  if (!activeResume.value) return
  savingDetail.value = true
  try {
    const res = await resumeApi.updateStructuredDetail(activeResume.value.id, structuredDetail)
    Object.assign(structuredDetail, normalizeStructuredDetail(res.data.structuredDetail))
    ElMessage.success('简历结构化详情已保存')
  } finally {
    savingDetail.value = false
  }
}

function emptyStructuredDetail(): ResumeStructuredDetail {
  return {
    educationExperiences: [],
    workExperiences: [],
    projectExperiences: [],
    certificates: [],
    languages: [],
    awards: [],
    fieldSources: {},
    fieldConfidence: {},
  }
}

function normalizeStructuredDetail(detail?: ResumeStructuredDetail): ResumeStructuredDetail {
  return {
    educationExperiences: detail?.educationExperiences || [],
    workExperiences: detail?.workExperiences || [],
    projectExperiences: detail?.projectExperiences || [],
    certificates: detail?.certificates || [],
    languages: detail?.languages || [],
    awards: detail?.awards || [],
    fieldSources: detail?.fieldSources || {},
    fieldConfidence: detail?.fieldConfidence || {},
  }
}

function emptyEducation() {
  return { school: '', degree: '', major: '', startDate: '', endDate: '', sourceText: '', confidence: null }
}

function emptyWork() {
  return { company: '', title: '', startDate: '', endDate: '', description: '', sourceText: '', confidence: null }
}

function emptyProject() {
  return { name: '', role: '', startDate: '', endDate: '', description: '', sourceText: '', confidence: null }
}

function statusClass(status: string) {
  return { UPLOADED: 'uploaded', PARSING: 'parsing', PARSED: 'parsed', FAILED: 'failed' }[status] || ''
}
</script>

<style scoped>
.page-subtitle,
.upload-hint,
.file-meta,
.drawer-hint {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.header-actions,
.table-header,
.file-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-row,
.panel-card {
  margin-bottom: 20px;
}

.stat-value.success {
  color: var(--color-success);
}

.stat-value.warning {
  color: var(--color-warning);
}

.stat-value.danger {
  color: var(--color-danger);
}

.mode-switch {
  margin-bottom: 16px;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.upload-icon {
  margin-bottom: 8px;
  color: var(--color-accent);
}

.upload-text {
  color: var(--color-text-secondary);
}

.upload-text em {
  color: var(--color-accent);
  font-style: normal;
}

.upload-button {
  width: 100%;
  margin-top: 12px;
}

.table-header {
  justify-content: space-between;
}

.file-cell {
  align-items: flex-start;
}

.file-name {
  color: var(--color-text-primary);
  font-weight: 500;
}

.status-tag {
  display: inline-block;
  font-size: 0.75rem;
  padding: 1px 8px;
  border-radius: 100px;
  font-weight: 500;
}

.status-tag.uploaded {
  background: oklch(94% 0.04 220);
  color: oklch(40% 0.12 220);
}

.status-tag.parsing {
  background: var(--color-warning-bg);
  color: oklch(40% 0.1 80);
}

.status-tag.parsed {
  background: var(--color-success-bg);
  color: oklch(35% 0.12 150);
}

.status-tag.failed {
  background: var(--color-danger-bg);
  color: oklch(40% 0.14 30);
}

.skill-tag {
  margin: 2px 4px 2px 0;
}

.drawer-hint {
  margin-top: 16px;
  line-height: 1.7;
}

.resume-detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.parse-error {
  margin-bottom: 12px;
}

.resume-tabs {
  margin-top: 12px;
}

.structured-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.detail-editor-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr)) auto;
  gap: 8px;
  align-items: center;
}

.extraction-meta {
  grid-column: 1 / -1;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: var(--color-text-muted);
  font-size: 0.8rem;
  line-height: 1.5;
}

.field-evidence {
  margin-top: 8px;
}

.detail-editor-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.tag-edit-form {
  margin-top: 4px;
}

.raw-section {
  margin-top: 18px;
}

.raw-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 10px;
}

.raw-text {
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text-regular);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 0.86rem;
}
</style>
