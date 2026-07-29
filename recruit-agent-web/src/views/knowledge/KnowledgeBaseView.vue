<template>
  <div class="page-container">
    <div class="page-header">
      <div><h2>知识库</h2><p>维护招聘制度、岗位说明和沟通规范</p></div>
      <div v-if="auth.canWriteKnowledge" class="header-actions">
        <el-upload :show-file-list="false" :before-upload="uploadFile" accept=".txt,.md,.pdf,.doc,.docx">
          <el-button :loading="uploading"><el-icon><Upload /></el-icon>上传文档</el-button>
        </el-upload>
        <el-button type="primary" @click="editorOpen = true"><el-icon><Plus /></el-icon>新建文本</el-button>
      </div>
    </div>

    <div class="search-band">
      <el-input v-model="query" clearable placeholder="输入问题测试向量检索" @keyup.enter="search">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" :loading="searching" @click="search">检索</el-button>
    </div>

    <section v-if="matches.length" class="results-band">
      <h3>检索结果</h3>
      <div v-for="match in matches" :key="match.chunkId" class="match-row">
        <div class="match-meta"><strong>{{ match.sourceName }}</strong><span>相似度 {{ displayScore(match.score) }}%</span></div>
        <p>{{ match.content }}</p>
      </div>
    </section>

    <el-table v-loading="loading" :data="documents" class="document-table">
      <el-table-column prop="sourceName" label="文档名称" min-width="260" />
      <el-table-column prop="chunkCount" label="分块数" width="110" />
      <el-table-column label="入库时间" width="190">
        <template #default="scope">{{ formatDate(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column v-if="auth.canDeleteKnowledge" label="操作" width="90" align="right">
        <template #default="scope">
          <el-popconfirm title="确认删除该文档及全部向量？" @confirm="remove(scope.row.documentId)">
            <template #reference>
              <el-button text type="danger" title="删除"><el-icon><Delete /></el-icon></el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty><el-empty description="还没有知识文档" /></template>
    </el-table>

    <el-dialog v-model="editorOpen" title="新建知识文本" width="620px">
      <el-form label-position="top">
        <el-form-item label="资料名称" required><el-input v-model="draft.sourceName" /></el-form-item>
        <el-form-item label="正文" required><el-input v-model="draft.content" type="textarea" :rows="12" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorOpen = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveText">分块并入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Delete, Plus, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import { useAuthStore } from '@/stores/auth'
import type { KnowledgeDocument, KnowledgeMatch } from '@/types/knowledge'

const auth = useAuthStore()
const documents = ref<KnowledgeDocument[]>([])
const matches = ref<KnowledgeMatch[]>([])
const query = ref('')
const loading = ref(false)
const saving = ref(false)
const searching = ref(false)
const uploading = ref(false)
const editorOpen = ref(false)
const draft = reactive({ sourceName: '', content: '' })

onMounted(load)

async function load() {
  loading.value = true
  try { documents.value = (await knowledgeApi.listDocuments()).data } finally { loading.value = false }
}

async function saveText() {
  if (!draft.sourceName.trim() || !draft.content.trim()) return ElMessage.warning('请填写资料名称和正文')
  saving.value = true
  try {
    await knowledgeApi.indexText(draft.sourceName.trim(), draft.content.trim())
    Object.assign(draft, { sourceName: '', content: '' })
    editorOpen.value = false
    ElMessage.success('知识已完成分块和向量化')
    await load()
  } finally { saving.value = false }
}

async function uploadFile(file: File) {
  uploading.value = true
  try { await knowledgeApi.upload(file); ElMessage.success('文档已入库'); await load() }
  finally { uploading.value = false }
  return false
}

async function remove(id: number) {
  await knowledgeApi.remove(id)
  ElMessage.success('文档已删除')
  await load()
}

async function search() {
  if (!query.value.trim()) return ElMessage.warning('请输入检索问题')
  searching.value = true
  try { matches.value = (await knowledgeApi.search(query.value.trim())).data }
  finally { searching.value = false }
}

function formatDate(value: string) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }

function displayScore(score: number) {
  return (score > 1 ? score : score * 100).toFixed(2)
}
</script>

<style scoped>
.page-header p { margin-top: 4px; color: var(--color-text-muted); font-size: 0.85rem; }
.header-actions { display: flex; gap: 8px; }
.search-band { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; margin-bottom: 20px; }
.results-band { padding: 16px 0 20px; border-top: 1px solid var(--color-border); }
.results-band h3 { font-size: 1rem; margin-bottom: 10px; color: var(--color-text-primary); }
.match-row { padding: 12px 0; border-bottom: 1px solid var(--color-border); }
.match-meta { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 6px; }
.match-meta span { color: var(--color-success); font-size: 0.8rem; }
.match-row p { color: var(--color-text-secondary); line-height: 1.6; white-space: pre-wrap; }
.document-table { width: 100%; }
@media (max-width: 720px) { .page-header { align-items: flex-start; gap: 12px; } .header-actions { flex-wrap: wrap; justify-content: flex-end; } }
</style>
