<template>
  <div class="page-container">
    <div class="page-header">
      <h2>向量检索</h2>
      <div />
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="detail-card">
          <template #header>
            <span class="section-title">岗位描述索引</span>
          </template>
          <p class="hint-text">将岗位描述转换为向量，存入 pgvector 用于语义匹配</p>
          <el-form :model="jobForm" label-position="top">
            <el-form-item label="选择岗位">
              <el-select v-model="jobForm.jobId" filterable placeholder="选择岗位" style="width:100%">
                <el-option v-for="j in jobStore.jobs" :key="j.id" :label="j.name" :value="j.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="indexingJob" @click="handleIndexJob">索引岗位描述</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="detail-card">
          <template #header>
            <span class="section-title">简历项目索引</span>
          </template>
          <p class="hint-text">将候选人项目经历转换为向量，用于语义匹配</p>
          <el-form :model="projectForm" label-position="top">
            <el-form-item label="选择候选人">
              <el-select v-model="projectForm.candidateId" filterable placeholder="选择候选人" style="width:100%" @change="loadCandidateProjects">
                <el-option v-for="c in candidateStore.candidates" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="选择已解析项目">
              <el-select
                v-model="projectForm.projectId"
                filterable
                clearable
                :loading="loadingProjects"
                placeholder="从简历项目经历中选择"
                style="width:100%"
                @change="applyProjectContent"
              >
                <el-option
                  v-for="project in projectOptions"
                  :key="project.id"
                  :label="project.label"
                  :value="project.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="项目内容">
              <el-input v-model="projectForm.content" type="textarea" :rows="3" placeholder="输入项目经历描述文本" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="indexingProject" @click="handleIndexProject">索引项目经历</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 相似度查询 -->
    <el-card class="detail-card">
      <template #header>
        <span class="section-title">相似度查询</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="岗位">
            <el-select v-model="queryJobId" filterable placeholder="选择岗位" style="width:100%">
              <el-option v-for="j in jobStore.jobs" :key="j.id" :label="j.name" :value="j.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="候选人">
            <el-select v-model="queryCandidateId" filterable placeholder="选择候选人" style="width:100%">
              <el-option v-for="c in candidateStore.candidates" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8" style="display:flex;align-items:flex-end;">
          <el-button type="primary" :loading="querying" @click="handleQuery">查询相似度</el-button>
        </el-col>
      </el-row>

      <div v-if="similarity" class="similarity-result">
        <div class="similarity-score">
          <div :class="['score-gauge', similarity.similarity >= 80 ? 'high' : similarity.similarity >= 60 ? 'mid' : 'low']" style="width:56px;height:56px;font-size:1.1rem;">
            {{ Math.round(similarity.similarity) }}
          </div>
          <div>
            <div class="similarity-label">语义相似度</div>
            <div class="similarity-detail">{{ similarity.content?.slice(0, 100) }}…</div>
          </div>
        </div>
      </div>
      <el-empty v-if="queried && !similarity" description="未找到匹配的项目向量" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useJobStore } from '@/stores/jobs'
import { useCandidateStore } from '@/stores/candidates'
import { vectorApi } from '@/api/vector'
import { candidateApi, resumeApi } from '@/api/candidates'

const jobStore = useJobStore()
const candidateStore = useCandidateStore()

const indexingJob = ref(false)
const indexingProject = ref(false)
const querying = ref(false)
const queried = ref(false)
const loadingProjects = ref(false)
const similarity = ref<{ candidateId: number; content: string; similarity: number } | null>(null)
const projectOptions = ref<Array<{ id: number; label: string; content: string }>>([])

const jobForm = reactive({ jobId: null as number | null })
const projectForm = reactive({
  candidateId: null as number | null,
  projectId: null as number | null,
  content: '',
})
const queryJobId = ref<number | null>(null)
const queryCandidateId = ref<number | null>(null)

onMounted(async () => {
  await jobStore.fetchJobs()
  await candidateStore.fetchCandidates()
})

async function handleIndexJob() {
  if (!jobForm.jobId) return ElMessage.warning('请选择岗位')
  const job = jobStore.jobs.find((j) => j.id === jobForm.jobId)
  if (!job) return
  indexingJob.value = true
  try {
    await vectorApi.indexJobDescription(job.id, job.description)
    ElMessage.success('岗位描述已加入向量库')
  } catch {
    ElMessage.error('索引失败')
  } finally {
    indexingJob.value = false
  }
}

async function handleIndexProject() {
  if (!projectForm.candidateId) return ElMessage.warning('请选择候选人')
  if (!projectForm.projectId) return ElMessage.warning('请选择一个简历项目')
  if (!projectForm.content.trim()) return ElMessage.warning('请输入项目内容')
  indexingProject.value = true
  try {
    await vectorApi.indexResumeProject(projectForm.candidateId, projectForm.projectId, projectForm.content)
    ElMessage.success('项目经历已加入向量库')
  } catch {
    ElMessage.error('索引失败')
  } finally {
    indexingProject.value = false
  }
}

async function loadCandidateProjects(candidateId?: number) {
  projectOptions.value = []
  projectForm.projectId = null
  projectForm.content = ''
  if (!candidateId) return

  loadingProjects.value = true
  try {
    const resumes = (await candidateApi.listResumes(candidateId)).data
    const parsedResumes = resumes.filter((resume) => resume.parseStatus === 'PARSED')
    const details = await Promise.all(parsedResumes.map((resume) => resumeApi.detail(resume.id)))
    projectOptions.value = details.flatMap((response, resumeIndex) =>
      response.data.structuredDetail.projectExperiences.map((project, index) => ({
        id: (resumeIndex + 1) * 1000 + index + 1,
        label: project.name || `项目 ${index + 1}（${response.data.resume.originalFilename}）`,
        content: [
          project.name,
          project.role,
          project.startDate && project.endDate ? `${project.startDate} - ${project.endDate}` : '',
          project.description,
        ].filter(Boolean).join('\n'),
      })),
    )
    if (!projectOptions.value.length) {
      ElMessage.info('该候选人的已解析简历中暂无项目经历')
    }
  } finally {
    loadingProjects.value = false
  }
}

function applyProjectContent(projectId?: number) {
  const project = projectOptions.value.find((item) => item.id === projectId)
  projectForm.content = project?.content || ''
}

async function handleQuery() {
  if (!queryJobId.value || !queryCandidateId.value) return ElMessage.warning('请选择岗位和候选人')
  querying.value = true
  queried.value = true
  try {
    const res = await vectorApi.findSimilarity(queryJobId.value, queryCandidateId.value)
    similarity.value = res.data as unknown as { candidateId: number; content: string; similarity: number }
  } catch {
    similarity.value = null
  } finally {
    querying.value = false
  }
}
</script>

<style scoped>
.detail-card {
  margin-bottom: 20px;
}

.hint-text {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  margin-bottom: 14px;
}

.similarity-result {
  margin-top: 16px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.similarity-score {
  display: flex;
  align-items: center;
  gap: 16px;
}

.similarity-label {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.similarity-detail {
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
