<script setup lang="ts">
import { onMounted, ref, nextTick, onUnmounted, computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NAvatar,
  NButton,
  NDropdown,
  NIcon,
  NInput,
  NModal,
  NSpin,
} from 'naive-ui'
import type { DropdownOption } from 'naive-ui'
import {
  ArrowBackOutline,
  EllipsisVertical,
  EyeOutline,
  PaperPlaneOutline,
  PersonOutline,
  TrashOutline,
  WarningOutline,
} from '@vicons/ionicons5'
import { getMessages, sendMessage, markAsRead, deleteConversation, type Message } from '../../api/message'
import { getUserProfile, deleteFriend, blockFriend, type UserProfile } from '../../api/friend'
import { createUserReport } from '../../api/userReport'
import { useUserStore } from '../../stores/user'
import { getAvatarUrl } from '../../utils/resource'
import { message, dialog } from '../../utils/feedback'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.userId))
const otherUser = ref<UserProfile | null>(null)
const messages = ref<Message[]>([])
const messageContent = ref('')
const loading = ref(false)
const sending = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
let timer: number | null = null
let loadErrorCount = 0
const reportDialogVisible = ref(false)
const reportReason = ref('')

function renderMenuIcon(comp: any) {
  return () => h(NIcon, null, { default: () => h(comp) })
}

const dropdownOptions = computed<DropdownOption[]>(() => [
  { label: '删除聊天记录', key: 'deleteConversation', icon: renderMenuIcon(TrashOutline) },
  { label: '删除好友', key: 'deleteFriend', icon: renderMenuIcon(PersonOutline) },
  { label: '拉黑', key: 'block', icon: renderMenuIcon(WarningOutline) },
  { label: '查看主页', key: 'viewProfile', icon: renderMenuIcon(EyeOutline) },
  { type: 'divider', key: 'd1' },
  { label: '举报', key: 'report', icon: renderMenuIcon(WarningOutline) },
])

async function loadOtherUser() {
  try {
    const res = await getUserProfile(userId.value)
    otherUser.value = res.data
  } catch {
    // ignore
  }
}

async function loadMessages(silent = false) {
  loading.value = true
  try {
    const res = await getMessages(userId.value, 1, 50, silent)
    messages.value = (res.data || []).reverse()
    await markAsRead(userId.value)
    await scrollToBottom()
    loadErrorCount = 0
  } catch {
    loadErrorCount++
    if (silent && loadErrorCount >= 3) {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }
  } finally {
    loading.value = false
  }
}

async function handleSend() {
  if (!messageContent.value.trim()) {
    message.warning('请输入消息内容')
    return
  }
  sending.value = true
  try {
    const res = await sendMessage(userId.value, messageContent.value.trim())
    messages.value.push(res.data)
    messageContent.value = ''
    await nextTick()
    await scrollToBottom()
  } catch {
    // ignore
  } finally {
    sending.value = false
  }
}

async function scrollToBottom() {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function handleDeleteConversation() {
  dialog.warning({
    title: '删除聊天记录',
    content: '确定要删除与该用户的所有聊天记录吗？此操作只删除您这边的记录，对方仍可看到。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteConversation(userId.value)
        message.success('聊天记录已删除')
        messages.value = []
        router.back()
      } catch {
        // ignore
      }
    },
  })
}

function handleDeleteFriend() {
  dialog.warning({
    title: '删除好友',
    content: '确定要删除该好友吗？删除后将无法发送消息。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteFriend(userId.value)
        message.success('已删除好友')
        router.back()
      } catch {
        // ignore
      }
    },
  })
}

function handleBlockFriend() {
  dialog.warning({
    title: '拉黑用户',
    content: '确定要拉黑该用户吗？拉黑后将无法发送消息。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await blockFriend(userId.value)
        message.success('已拉黑')
        router.back()
      } catch {
        // ignore
      }
    },
  })
}

function handleViewProfile() {
  const isAdmin = route.path.startsWith('/admin')
  router.push(isAdmin ? `/admin/user/${userId.value}` : `/student/user/${userId.value}`)
}

function handleReport() {
  reportDialogVisible.value = true
  reportReason.value = ''
}

async function confirmReport() {
  if (!reportReason.value.trim()) {
    message.warning('请输入举报原因')
    return
  }
  try {
    await createUserReport(userId.value, reportReason.value.trim())
    message.success('举报已提交，管理员将尽快处理')
    reportDialogVisible.value = false
  } catch {
    // ignore
  }
}

function handleMenuSelect(key: string | number) {
  switch (String(key)) {
    case 'deleteConversation':
      handleDeleteConversation()
      break
    case 'deleteFriend':
      handleDeleteFriend()
      break
    case 'block':
      handleBlockFriend()
      break
    case 'viewProfile':
      handleViewProfile()
      break
    case 'report':
      handleReport()
      break
  }
}

onMounted(async () => {
  await loadOtherUser()
  await loadMessages(false)
  timer = window.setInterval(() => loadMessages(true), 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="chat-window-page">
    <div class="chat-shell glass-panel-strong">
      <header class="chat-header">
        <NButton quaternary circle @click="router.back()">
          <template #icon>
            <NIcon :component="ArrowBackOutline" :size="18" />
          </template>
        </NButton>
        <div v-if="otherUser" class="chat-user-info">
          <NAvatar round :size="36" :src="getAvatarUrl(otherUser.avatar) || undefined">
            <NIcon :size="18" :component="PersonOutline" />
          </NAvatar>
          <span class="chat-user-name">{{ otherUser.nickname || otherUser.username }}</span>
        </div>
        <div class="chat-header-actions">
          <NDropdown trigger="click" :options="dropdownOptions" @select="handleMenuSelect">
            <NButton quaternary circle>
              <template #icon>
                <NIcon :component="EllipsisVertical" :size="18" />
              </template>
            </NButton>
          </NDropdown>
        </div>
      </header>

      <NSpin :show="loading && messages.length === 0" class="chat-spin">
        <div ref="messagesContainer" class="chat-messages">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-item"
            :class="{ 'is-self': msg.fromUserId === userStore.user?.userId }"
          >
            <NAvatar round :size="36" :src="getAvatarUrl(msg.fromAvatar) || undefined">
              <NIcon :size="18" :component="PersonOutline" />
            </NAvatar>
            <div class="message-content">
              <div class="message-bubble">{{ msg.content }}</div>
              <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>
      </NSpin>

      <div class="chat-input">
        <NInput
          v-model:value="messageContent"
          type="textarea"
          :rows="2"
          placeholder="输入消息…（Ctrl+Enter 发送）"
          @keydown="
            (e: KeyboardEvent) => {
              if (e.ctrlKey && e.key === 'Enter') handleSend()
            }
          "
        />
        <NButton
          type="primary"
          round
          :loading="sending"
          :disabled="!messageContent.trim()"
          @click="handleSend"
        >
          <template #icon>
            <NIcon :component="PaperPlaneOutline" />
          </template>
          发送
        </NButton>
      </div>
    </div>

    <NModal
      v-model:show="reportDialogVisible"
      preset="card"
      title="举报用户"
      style="width: min(480px, 92vw)"
    >
      <NInput
        v-model:value="reportReason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-count
        placeholder="请输入举报原因"
      />
      <template #footer>
        <div class="modal-footer">
          <NButton @click="reportDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="confirmReport">提交举报</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.chat-window-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: clamp(8px, 1.5vw, 16px);
  box-sizing: border-box;
}

.chat-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 28px;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--m-stroke);
  background: rgba(255, 255, 255, 0.35);
  flex-shrink: 0;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chat-user-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--m-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-header-actions {
  margin-left: auto;
}

.chat-spin {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-spin :deep(.n-spin-container),
.chat-spin :deep(.n-spin-content) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  background:
    radial-gradient(ellipse 50% 40% at 10% 20%, rgba(168, 196, 182, 0.18), transparent 60%),
    radial-gradient(ellipse 40% 35% at 90% 80%, rgba(196, 164, 132, 0.12), transparent 55%);
}

.message-item {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  max-width: 100%;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

.message-content {
  max-width: min(68%, 520px);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.is-self .message-content {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 22px 22px 22px 8px;
  word-wrap: break-word;
  white-space: pre-wrap;
  font-size: 15px;
  line-height: 1.5;
  color: var(--m-ink);
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 6px 18px rgba(61, 69, 64, 0.06);
}

.message-item.is-self .message-bubble {
  border-radius: 22px 22px 8px 22px;
  color: #fff;
  background: linear-gradient(145deg, var(--m-sage), var(--m-sage-deep));
  border-color: transparent;
  box-shadow: 0 10px 24px rgba(92, 128, 112, 0.28);
}

.message-time {
  font-size: 11px;
  color: var(--m-ink-muted);
  padding: 0 6px;
}

.chat-input {
  padding: 14px 16px 16px;
  border-top: 1px solid var(--m-stroke);
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(12px);
}

.chat-input :deep(.n-input) {
  flex: 1;
  border-radius: 18px;
}

.chat-input :deep(.n-input__textarea-el) {
  resize: none;
  max-height: 140px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
