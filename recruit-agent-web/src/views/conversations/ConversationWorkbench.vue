<template>
  <div class="workbench-container">
    <div class="workbench-header">
      <el-button text @click="router.push('/conversations')">
        <el-icon><ArrowLeft /></el-icon>返回
      </el-button>
      <div class="workbench-title">
        <h2>招聘沟通工作台</h2>
        <span v-if="conversation" class="stage-badge">{{ stageLabel(conversation.stage) }}</span>
        <span v-if="conversation" :class="['status-badge', conversation.status.toLowerCase()]">{{ conversation.status === 'CLOSED' ? '已关闭' : '进行中' }}</span>
        <span v-if="conversation" class="conv-meta">岗位 #{{ conversation.jobId }} · 候选人 #{{ conversation.candidateId }}</span>
      </div>
      <el-button :loading="generating" type="primary" :disabled="conversation?.status === 'CLOSED'" @click="handleGenerateReply">
        <el-icon><MagicStick /></el-icon>AI 生成回复
      </el-button>
    </div>

    <div class="workbench-body" v-loading="loading" element-loading-text="加载会话中…" v-if="conversation">
      <div class="chat-panel">
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="chat-empty">
            <el-icon :size="40" color="var(--color-text-disabled)"><ChatDotSquare /></el-icon>
            <p>暂无消息，开始招聘沟通吧</p>
          </div>

          <div v-for="msg in messages" :key="msg.id" :class="['msg-bubble', msg.senderType.toLowerCase()]">
            <div class="msg-sender">
              {{ msg.senderType === 'HR' ? 'HR' : msg.senderType === 'CANDIDATE' ? '候选人' : '系统' }}
            </div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>

        <div class="chat-input">
          <el-select v-model="sendAs" class="send-as-select" style="width: 100px;" :disabled="conversation.status === 'CLOSED'">
            <el-option label="HR" value="HR" />
            <el-option label="候选人" value="CANDIDATE" />
          </el-select>
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="2"
            placeholder="输入消息…"
            :disabled="conversation.status === 'CLOSED'"
            @keyup.ctrl.enter="handleSendMessage"
          />
          <el-button type="primary" :disabled="!newMessage.trim() || conversation.status === 'CLOSED'" @click="handleSendMessage">
            发送
          </el-button>
        </div>
      </div>

      <div class="side-panel">
        <el-card class="side-card">
          <template #header>
            <span class="section-title">会话管理</span>
          </template>
          <el-form label-position="top" class="conversation-form">
            <el-form-item label="阶段">
              <el-select v-model="stageDraft" style="width: 100%" :disabled="conversation.status === 'CLOSED'" @change="handleStageChange">
                <el-option v-for="stage in stageOptions" :key="stage.value" :label="stage.label" :value="stage.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="下次跟进">
              <div class="follow-up-row">
                <el-date-picker
                  v-model="followUpDraft"
                  type="datetime"
                  placeholder="选择跟进时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 100%"
                  :disabled="conversation.status === 'CLOSED'"
                />
                <el-button :loading="savingFollowUp" :disabled="conversation.status === 'CLOSED'" @click="saveFollowUp">保存</el-button>
              </div>
            </el-form-item>
          </el-form>
          <div class="conversation-actions">
            <el-button size="small" :disabled="conversation.status === 'CLOSED'" @click="markCandidateReplied">标记已回复</el-button>
            <el-button size="small" type="danger" plain :disabled="conversation.status === 'CLOSED'" @click="closeConversation">关闭会话</el-button>
          </div>
          <div class="conversation-meta">
            <span>候选人回复：{{ formatDateTime(conversation.lastCandidateReplyAt) }}</span>
            <span>下次跟进：{{ formatDateTime(conversation.nextFollowUpAt) }}</span>
          </div>
        </el-card>

        <!-- Reply suggestions -->
        <el-card class="side-card">
          <template #header>
            <span class="section-title">AI 回复建议</span>
          </template>
          <div v-if="suggestions.length === 0" class="empty-hint">点击「AI 生成回复」获取建议</div>
          <div v-for="s in suggestions" :key="s.id" :class="['suggestion-item', s.status.toLowerCase()]">
            <div class="suggestion-intent">
              <el-tag size="small" type="warning" v-if="s.needHuman">需人工确认</el-tag>
              <span class="intent-label">{{ intentLabel(s.intent) }}</span>
              <span class="confidence">({{ (s.confidence * 100).toFixed(0) }}%)</span>
            </div>
            <div class="suggestion-text">{{ s.suggestedReply }}</div>
            <div class="suggestion-actions" v-if="s.status === 'DRAFT' && auth.canApproveReplies && conversation.status !== 'CLOSED'">
              <el-button size="small" type="primary" @click="handleApprove(s.id)">采纳</el-button>
              <el-button size="small" @click="handleReject(s.id)">拒绝</el-button>
              <el-button size="small" text @click="copyText(s.suggestedReply)">复制</el-button>
            </div>
            <div class="suggestion-actions" v-else-if="s.status === 'DRAFT'">
              <el-button size="small" text @click="copyText(s.suggestedReply)">复制</el-button>
            </div>
            <div v-else class="suggestion-status">
              {{ s.status === 'APPROVED' ? '✓ 已采纳' : '✕ 已拒绝' }}
            </div>
          </div>
        </el-card>

        <!-- Conversation context -->
        <el-card class="side-card" v-if="jobInfo">
          <template #header>
            <span class="section-title">岗位信息</span>
          </template>
          <div class="context-item">
            <div class="context-label">岗位</div>
            <div class="context-value">{{ jobInfo.name }}</div>
          </div>
          <div class="context-item">
            <div class="context-label">必须技能</div>
            <div class="context-value">{{ jobInfo.requiredSkills?.join(', ') }}</div>
          </div>
        </el-card>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="会话不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, MagicStick, ChatDotSquare } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { conversationApi } from '@/api/conversations'
import { jobApi } from '@/api/jobs'
import { useAuthStore } from '@/stores/auth'
import type { Conversation, ConversationMessage, ConversationStage, ReplySuggestion } from '@/types/conversation'
import type { Job } from '@/types/job'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const conversation = ref<Conversation | null>(null)
const messages = ref<ConversationMessage[]>([])
const suggestions = ref<ReplySuggestion[]>([])
const jobInfo = ref<Job | null>(null)
const loading = ref(true)
const generating = ref(false)
const savingFollowUp = ref(false)
const newMessage = ref('')
const sendAs = ref('HR')
const stageDraft = ref<ConversationStage>('INITIAL_CONTACT')
const followUpDraft = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const stageOptions: Array<{ label: string; value: ConversationStage }> = [
  { label: '初次联系', value: 'INITIAL_CONTACT' },
  { label: '询问意向', value: 'ASK_INTENTION' },
  { label: '确认时间', value: 'ASK_AVAILABILITY' },
  { label: '薪资沟通', value: 'ASK_SALARY' },
  { label: '邀约面试', value: 'INVITE_INTERVIEW' },
  { label: '婉拒', value: 'REJECT' },
  { label: '人工处理', value: 'MANUAL_REVIEW' },
]

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await conversationApi.get(id)
    conversation.value = res.data as unknown as Conversation
    stageDraft.value = conversation.value.stage
    followUpDraft.value = toLocalDateTimeValue(conversation.value.nextFollowUpAt)
    const msgRes = await conversationApi.listMessages(id)
    messages.value = msgRes.data as unknown as ConversationMessage[]
    const suggestionRes = await conversationApi.listReplySuggestions(id)
    suggestions.value = suggestionRes.data as unknown as ReplySuggestion[]

    if (conversation.value?.jobId) {
      const jobRes = await jobApi.get(conversation.value.jobId)
      jobInfo.value = jobRes.data as unknown as Job
    }
  } finally {
    loading.value = false
  }
  scrollToBottom()
})

function stageLabel(stage: string) {
  const map: Record<string, string> = {
    INITIAL_CONTACT: '初次联系',
    ASK_INTENTION: '询问意向',
    ASK_AVAILABILITY: '确认时间',
    ASK_SALARY: '薪资沟通',
    INVITE_INTERVIEW: '邀约面试',
    REJECT: '婉拒',
    MANUAL_REVIEW: '人工处理',
  }
  return map[stage] || stage
}

function intentLabel(intent: string) {
  const map: Record<string, string> = {
    GREETING: '打招呼',
    ASK_INTENTION: '询问意向',
    ASK_AVAILABILITY: '确认到岗时间',
    ASK_SALARY: '询问薪资',
    INVITE_INTERVIEW: '邀约面试',
    REJECT_CANDIDATE: '婉拒',
    ANSWER_JOB_QUESTION: '回答岗位问题',
    UNKNOWN: '无法判断',
  }
  return map[intent] || intent
}

function formatTime(t: string) {
  if (!t) return ''
  return new Date(t).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatDateTime(t?: string) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

function toLocalDateTimeValue(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function toIsoDateTime(value: string) {
  return new Date(value).toISOString()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

async function handleSendMessage() {
  if (!newMessage.value.trim() || !conversation.value) return
  try {
    await conversationApi.addMessage(conversation.value.id, {
      senderType: sendAs.value as any,
      content: newMessage.value.trim(),
    })
    newMessage.value = ''
    const res = await conversationApi.listMessages(conversation.value.id)
    messages.value = res.data as unknown as ConversationMessage[]
    scrollToBottom()
  } catch {
    // handled by interceptor
  }
}

async function handleGenerateReply() {
  if (!conversation.value) return
  generating.value = true
  try {
    const res = await conversationApi.generateReply(conversation.value.id)
    const suggestion = res.data as unknown as ReplySuggestion
    suggestions.value.unshift(suggestion)
    ElMessage.success('回复建议已生成')
  } catch {
    // handled by interceptor
  } finally {
    generating.value = false
  }
}

async function handleApprove(id: number) {
  const res = await conversationApi.approveReply(id)
  const index = suggestions.value.findIndex((s) => s.id === id)
  if (index >= 0) suggestions.value[index] = res.data as unknown as ReplySuggestion
  if (conversation.value) {
    const msgRes = await conversationApi.listMessages(conversation.value.id)
    messages.value = msgRes.data as unknown as ConversationMessage[]
    scrollToBottom()
  }
  ElMessage.success('已采纳回复')
}

async function handleReject(id: number) {
  const res = await conversationApi.rejectReply(id)
  const index = suggestions.value.findIndex((s) => s.id === id)
  if (index >= 0) suggestions.value[index] = res.data as unknown as ReplySuggestion
  ElMessage.info('已拒绝回复')
}

function copyText(text: string) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

async function handleStageChange(stage: ConversationStage) {
  if (!conversation.value) return
  conversation.value = (await conversationApi.updateStage(conversation.value.id, stage)).data
  ElMessage.success('会话阶段已更新')
}

async function saveFollowUp() {
  if (!conversation.value || !followUpDraft.value) {
    ElMessage.warning('请选择跟进时间')
    return
  }
  savingFollowUp.value = true
  try {
    conversation.value = (await conversationApi.scheduleFollowUp(conversation.value.id, toIsoDateTime(followUpDraft.value))).data
    ElMessage.success('跟进时间已保存')
  } finally {
    savingFollowUp.value = false
  }
}

async function markCandidateReplied() {
  if (!conversation.value) return
  conversation.value = (await conversationApi.markCandidateReplied(conversation.value.id)).data
  ElMessage.success('已标记候选人回复')
}

async function closeConversation() {
  if (!conversation.value) return
  await ElMessageBox.confirm('确认关闭该会话？', '关闭会话', { type: 'warning' })
  conversation.value = (await conversationApi.close(conversation.value.id)).data
  ElMessage.success('会话已关闭')
}
</script>

<style scoped>
.workbench-container {
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
}

.workbench-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.workbench-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;

  h2 {
    font-size: 1rem;
    font-weight: 600;
  }
}

.stage-badge {
  font-size: 0.75rem;
  padding: 2px 10px;
  border-radius: 100px;
  background: var(--color-accent-bg);
  color: var(--color-accent);
  font-weight: 500;
}

.status-badge {
  font-size: 0.75rem;
  padding: 2px 10px;
  border-radius: 100px;
  font-weight: 500;
}

.status-badge.open {
  background: var(--color-success-bg);
  color: oklch(35% 0.12 150);
}

.status-badge.closed {
  background: var(--color-bg-deep);
  color: var(--color-text-muted);
}

.conv-meta {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.workbench-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.msg-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: var(--radius-md);

  &.hr {
    align-self: flex-end;
    background: var(--color-accent);
    color: white;
    border-bottom-right-radius: 4px;

    .msg-sender { color: oklch(85% 0.06 255); }
  }

  &.candidate {
    align-self: flex-start;
    background: var(--color-bg);
    border: 1px solid var(--color-border);
    border-bottom-left-radius: 4px;
  }

  &.system {
    align-self: center;
    background: var(--color-bg-deep);
    font-size: 0.8rem;
    color: var(--color-text-muted);
    max-width: 60%;
    text-align: center;
  }
}

.msg-sender {
  font-size: 0.75rem;
  font-weight: 600;
  margin-bottom: 4px;
  opacity: 0.8;
}

.msg-content {
  font-size: 0.9rem;
  line-height: 1.6;
  white-space: pre-wrap;
}

.msg-time {
  font-size: 0.7rem;
  margin-top: 4px;
  opacity: 0.6;
  text-align: right;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 16px;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.send-as-select {
  flex-shrink: 0;
}

.chat-input .el-input {
  flex: 1;
}

.side-panel {
  width: 360px;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.side-card {
  border-radius: var(--radius-md);
}

.conversation-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.follow-up-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
}

.conversation-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.conversation-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.suggestion-item {
  padding: 12px;
  border-radius: var(--radius-sm);
  margin-bottom: 12px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);

  &:last-child { margin-bottom: 0; }

  &.approved {
    border-color: var(--color-success);
    background: var(--color-success-bg);
  }

  &.rejected {
    opacity: 0.6;
  }
}

.suggestion-intent {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.intent-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.confidence {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.suggestion-text {
  font-size: 0.85rem;
  color: var(--color-text-regular);
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 8px;
  padding: 8px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
}

.suggestion-actions {
  display: flex;
  gap: 6px;
}

.suggestion-status {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--color-text-muted);
  text-align: right;
}

.context-item {
  margin-bottom: 10px;

  &:last-child { margin-bottom: 0; }
}

.context-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-bottom: 2px;
}

.context-value {
  font-size: 0.85rem;
  color: var(--color-text-regular);
}

.empty-hint {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  text-align: center;
  padding: 20px 0;
}
</style>
