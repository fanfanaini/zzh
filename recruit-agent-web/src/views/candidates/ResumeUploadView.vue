<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-title">
        <el-button text @click="router.push('/candidates')">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <div>
          <h2>简历管理：{{ candidate?.name || '候选人' }}</h2>
          <p class="page-subtitle">上传原始简历，提取文本并生成候选人画像。</p>
        </div>
      </div>
      <el-button v-if="candidate" type="primary" @click="router.push({ path: '/screening', query: { candidateId: candidateId } })">
        <el-icon><DataAnalysis /></el-icon>前往筛选
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="detail-card">
          <template #header>
            <span class="section-title">上传简历</span>
          </template>
          <el-upload
            drag
            multiple
            accept=".pdf,.doc,.docx,.txt,.md"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="upload-icon" :size="40"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽简历文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="upload-hint">支持 PDF、DOCX、DOC、TXT、MD，可一次选择多份。</div>
            </template>
          </el-upload>

          <div class="upload-actions" v-if="selectedFiles.length">
            <el-button type="primary" :loading="uploading" @click="handleUpload(false)">上传</el-button>
            <el-button :loading="uploading" @click="handleUpload(true)">上传并解析</el-button>
          </div>
        </el-card>

        <el-card class="detail-card">
          <template #header>
            <span class="section-title">简历文件</span>
          </template>
          <div v-if="store.resumes.length === 0" class="empty-hint">
            还没有上传简历。
          </div>
          <div v-for="resume in store.resumes" :key="resume.id" class="resume-item">
            <div class="resume-info">
              <el-icon><Document /></el-icon>
              <span class="resume-name">{{ resume.originalFilename }}</span>
              <el-tooltip :disabled="!resume.parseErrorMessage" :content="resume.parseErrorMessage" placement="top">
                <span :class="['status-tag', statusClass(resume.parseStatus)]">{{ statusLabel(resume.parseStatus) }}</span>
              </el-tooltip>
              <span class="resume-size">{{ (resume.fileSize / 1024).toFixed(1) }} KB</span>
            </div>
            <div class="resume-actions">
              <el-button size="small" @click="viewResumeDetail(resume)">
                详情
              </el-button>
              <el-button size="small" @click="downloadResume(resume)">
                下载
              </el-button>
              <el-button size="small" :loading="parsingId === resume.id" @click="parseResume(resume.id)">
                一键解析
              </el-button>
              <el-button
                v-if="resume.parseStatus === 'UPLOADED' || resume.parseStatus === 'FAILED'"
                size="small"
                @click="extractText(resume.id)"
              >
                仅提取文本
              </el-button>
              <el-button v-if="resume.parseStatus === 'PARSED'" size="small" type="primary" @click="extractProfile(resume.id)">
                重新生成画像
              </el-button>
              <el-button size="small" type="danger" text @click="deleteResume(resume.id)">
                删除
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="detail-card">
          <template #header>
            <span class="section-title">候选人画像</span>
          </template>
          <el-descriptions v-if="candidate" :column="1" border size="small">
            <el-descriptions-item label="姓名">{{ candidate.name }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ educationLabel(candidate.educationLevel) }}</el-descriptions-item>
            <el-descriptions-item label="经验">{{ candidate.totalExperienceYears }} 年</el-descriptions-item>
            <el-descriptions-item label="城市">{{ candidate.expectedCity || '-' }}</el-descriptions-item>
            <el-descriptions-item label="技能">
              <el-tag v-for="skill in candidate.skills" :key="skill" size="small" class="skill-tag">
                {{ skill }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="rawTextDrawer" title="简历详情" size="620px">
      <template v-if="activeResume">
        <el-alert
          v-if="activeResume.parseErrorMessage"
          :title="activeResume.parseErrorMessage"
          type="error"
          :closable="false"
          show-icon
        />
        <el-tabs>
          <el-tab-pane label="结构化详情">
            <el-empty v-if="!activeDetail" description="暂无结构化解析结果" />
            <template v-else>
              <section class="structured-block">
                <h4>教育经历</h4>
                <el-descriptions
                  v-for="(item, index) in activeDetail.educationExperiences"
                  :key="`education-${index}`"
                  :column="1"
                  border
                  class="detail-descriptions"
                >
                  <el-descriptions-item label="学校">{{ item.school || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="学历/专业">{{ [item.degree, item.major].filter(Boolean).join(' / ') || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="时间">{{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="置信度">{{ confidenceLabel(item.confidence) }}</el-descriptions-item>
                  <el-descriptions-item label="原文依据">{{ item.sourceText || '-' }}</el-descriptions-item>
                </el-descriptions>
              </section>
              <section class="structured-block">
                <h4>工作经历</h4>
                <el-descriptions
                  v-for="(item, index) in activeDetail.workExperiences"
                  :key="`work-${index}`"
                  :column="1"
                  border
                  class="detail-descriptions"
                >
                  <el-descriptions-item label="公司/职位">{{ [item.company, item.title].filter(Boolean).join(' / ') || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="时间">{{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="描述">{{ item.description || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="置信度">{{ confidenceLabel(item.confidence) }}</el-descriptions-item>
                  <el-descriptions-item label="原文依据">{{ item.sourceText || '-' }}</el-descriptions-item>
                </el-descriptions>
              </section>
              <section class="structured-block">
                <h4>项目经历</h4>
                <el-descriptions
                  v-for="(item, index) in activeDetail.projectExperiences"
                  :key="`project-${index}`"
                  :column="1"
                  border
                  class="detail-descriptions"
                >
                  <el-descriptions-item label="项目/角色">{{ [item.name, item.role].filter(Boolean).join(' / ') || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="时间">{{ [item.startDate, item.endDate].filter(Boolean).join(' - ') || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="描述">{{ item.description || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="置信度">{{ confidenceLabel(item.confidence) }}</el-descriptions-item>
                  <el-descriptions-item label="原文依据">{{ item.sourceText || '-' }}</el-descriptions-item>
                </el-descriptions>
              </section>
            </template>
          </el-tab-pane>
          <el-tab-pane label="原文">
            <div class="raw-title">{{ activeResume.originalFilename }}</div>
            <pre class="raw-text">{{ activeResume.rawText || '暂无提取文本' }}</pre>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, DataAnalysis, Document, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { candidateApi, resumeApi } from '@/api/candidates'
import { useCandidateStore } from '@/stores/candidates'
import type { ResumeDocument, ResumeStructuredDetail } from '@/types/candidate'
import { educationLabel } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const store = useCandidateStore()

const candidateId = computed(() => Number(route.params.id))
const candidate = computed(() => store.currentCandidate)
const selectedFiles = ref<File[]>([])
const uploading = ref(false)
const parsingId = ref<number | null>(null)
const rawTextDrawer = ref(false)
const activeResume = ref<ResumeDocument | null>(null)
const activeDetail = ref<ResumeStructuredDetail | null>(null)

onMounted(async () => {
  await store.fetchCandidate(candidateId.value)
  await store.fetchResumes(candidateId.value)
})

function handleFileChange(uploadFile: { raw?: File }) {
  if (uploadFile.raw && !selectedFiles.value.some((file) => file.name === uploadFile.raw?.name)) {
    selectedFiles.value.push(uploadFile.raw)
  }
}

function handleFileRemove(uploadFile: { name: string }) {
  selectedFiles.value = selectedFiles.value.filter((file) => file.name !== uploadFile.name)
}

async function handleUpload(parseAfterUpload: boolean) {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请选择简历文件')
    return
  }
  uploading.value = true
  try {
    const res = selectedFiles.value.length === 1
      ? await candidateApi.uploadResume(candidateId.value, selectedFiles.value[0])
      : await candidateApi.batchUploadResumes(candidateId.value, selectedFiles.value)
    const uploaded = Array.isArray(res.data) ? res.data : [res.data]
    selectedFiles.value = []
    if (parseAfterUpload) {
      for (const resume of uploaded) {
        await resumeApi.submitParseTask(resume.id)
      }
      ElMessage.success('上传完成，解析任务已提交')
    } else {
      ElMessage.success('上传成功')
    }
    await store.fetchResumes(candidateId.value)
  } finally {
    uploading.value = false
  }
}

async function extractText(resumeId: number) {
  parsingId.value = resumeId
  try {
    await resumeApi.extractText(resumeId)
    ElMessage.success('文本提取完成')
    await store.fetchResumes(candidateId.value)
  } finally {
    parsingId.value = null
  }
}

async function extractProfile(resumeId: number) {
  parsingId.value = resumeId
  try {
    const res = await resumeApi.extractProfile(resumeId)
    store.currentCandidate = res.data
    ElMessage.success('候选人画像已更新')
  } finally {
    parsingId.value = null
  }
}

async function parseResume(resumeId: number) {
  parsingId.value = resumeId
  try {
    await resumeApi.submitParseTask(resumeId)
    await store.fetchResumes(candidateId.value)
    ElMessage.success('解析任务已提交，可在 Agent 任务中查看进度')
  } finally {
    parsingId.value = null
  }
}

async function viewResumeDetail(resume: ResumeDocument) {
  const response = await resumeApi.detail(resume.id)
  activeResume.value = response.data.resume
  activeDetail.value = response.data.structuredDetail
  rawTextDrawer.value = true
}

function confidenceLabel(confidence: number | null) {
  return confidence == null ? '-' : `${Math.round(confidence * 100)}%`
}

async function downloadResume(resume: ResumeDocument) {
  const response = await resumeApi.download(resume.id)
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = resume.originalFilename
  anchor.click()
  URL.revokeObjectURL(url)
}

async function deleteResume(resumeId: number) {
  await ElMessageBox.confirm('确认删除该简历文件？', '删除简历', { type: 'warning' })
  await resumeApi.delete(resumeId)
  ElMessage.success('简历已删除')
  await store.fetchResumes(candidateId.value)
}

function statusClass(status: string) {
  return { UPLOADED: 'uploaded', PARSING: 'parsing', PARSED: 'parsed', FAILED: 'failed' }[status] || ''
}

function statusLabel(status: string) {
  return { UPLOADED: '待解析', PARSING: '解析中', PARSED: '已解析', FAILED: '失败' }[status] || status
}
</script>

<style scoped>
.header-title,
.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.detail-card {
  margin-bottom: 20px;
}

.structured-block {
  margin-bottom: 24px;
}

.structured-block h4 {
  margin-bottom: 10px;
  color: var(--color-text-primary);
}

.detail-descriptions {
  margin-bottom: 12px;
}

.upload-icon {
  margin-bottom: 12px;
  color: var(--color-accent);
}

.upload-text {
  font-size: 0.95rem;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.upload-text em {
  color: var(--color-accent);
  font-style: normal;
}

.upload-hint,
.empty-hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.upload-actions {
  margin-top: 16px;
}

.resume-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.resume-item:last-child {
  border-bottom: none;
}

.resume-info,
.resume-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.resume-name {
  font-size: 0.9rem;
  color: var(--color-text-regular);
}

.resume-size {
  font-size: 0.8rem;
  color: var(--color-text-muted);
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

.raw-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.raw-text {
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
