<script setup lang="ts">
import { onMounted, ref, nextTick, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, ArrowLeft, User, MoreFilled, Delete, Warning, View, ChatDotRound } from '@element-plus/icons-vue'
import { getMessages, sendMessage, markAsRead, deleteConversation, type Message } from '../../api/message'
import { getUserProfile, deleteFriend, blockFriend, type UserProfile } from '../../api/friend'
import { createUserReport } from '../../api/userReport'
import { useUserStore } from '../../stores/user'
import { getAvatarUrl } from '../../utils/resource'

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
const menuVisible = ref(false)
const reportDialogVisible = ref(false)
const reportReason = ref('')

async function loadOtherUser() {
  try {
    const res = await getUserProfile(userId.value)
    otherUser.value = res.data
  } catch (e: any) {
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
  } catch (e: any) {
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
    ElMessage.warning('请输入消息内容')
    return
  }
  sending.value = true
  try {
    const res = await sendMessage(userId.value, messageContent.value.trim())
    messages.value.push(res.data)
    messageContent.value = ''
    await nextTick()
    await scrollToBottom()
  } catch (e: any) {
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

async function handleDeleteConversation() {
  try {
    await ElMessageBox.confirm('确定要删除与该用户的所有聊天记录吗？此操作只删除您这边的记录，对方仍可看到。', '删除聊天记录', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await deleteConversation(userId.value)
    ElMessage.success('聊天记录已删除')
    messages.value = []
    router.back()
  } catch (e: any) {
    if (e !== 'cancel') {
    }
  }
}

async function handleDeleteFriend() {
  try {
    await ElMessageBox.confirm('确定要删除该好友吗？删除后将无法发送消息。', '删除好友', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await deleteFriend(userId.value)
    ElMessage.success('已删除好友')
    router.back()
  } catch (e: any) {
    if (e !== 'cancel') {
    }
  }
}

async function handleBlockFriend() {
  try {
    await ElMessageBox.confirm('确定要拉黑该用户吗？拉黑后将无法发送消息。', '拉黑用户', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await blockFriend(userId.value)
    ElMessage.success('已拉黑')
    router.back()
  } catch (e: any) {
    if (e !== 'cancel') {
    }
  }
}

function handleViewProfile() {
  router.push(`/student/user/${userId.value}`)
}

function handleReport() {
  reportDialogVisible.value = true
  reportReason.value = ''
}

async function confirmReport() {
  if (!reportReason.value.trim()) {
    ElMessage.warning('请输入举报原因')
    return
  }
  try {
    await createUserReport(userId.value, reportReason.value.trim())
    ElMessage.success('举报已提交，管理员将尽快处理')
    reportDialogVisible.value = false
  } catch (e: any) {
  }
}

function handleMenuCommand(command: string) {
  menuVisible.value = false
  switch (command) {
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
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div class="chat-window-page">
    <el-card class="chat-card" :body-style="{ padding: 0, display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }">
      <template #header>
        <div class="chat-header">
          <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
          <div class="chat-user-info" v-if="otherUser">
            <el-avatar :size="32" :src="getAvatarUrl(otherUser.avatar)">
              <el-icon :size="16"><User /></el-icon>
            </el-avatar>
            <span class="chat-user-name">{{ otherUser.nickname || otherUser.username }}</span>
          </div>
          <div class="chat-header-actions">
            <el-dropdown trigger="click" v-model:visible="menuVisible" @command="handleMenuCommand">
              <el-button text :icon="MoreFilled" circle />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="deleteConversation" :icon="Delete">删除聊天记录</el-dropdown-item>
                  <el-dropdown-item command="deleteFriend" :icon="ChatDotRound">删除好友</el-dropdown-item>
                  <el-dropdown-item command="block" :icon="Warning">拉黑</el-dropdown-item>
                  <el-dropdown-item command="viewProfile" :icon="View">查看主页</el-dropdown-item>
                  <el-dropdown-item command="report" :icon="Warning" divided>举报</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>
      <div class="chat-messages" ref="messagesContainer" v-loading="loading">
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ 'is-self': msg.fromUserId === userStore.user?.userId }">
          <el-avatar :size="36" :src="getAvatarUrl(msg.fromAvatar)">
            <el-icon :size="18"><User /></el-icon>
          </el-avatar>
          <div class="message-content">
            <div class="message-text">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>
      </div>
      <div class="chat-input">
        <el-input
          v-model="messageContent"
          type="textarea"
          :rows="3"
          placeholder="输入消息..."
          @keyup.ctrl.enter="handleSend"
        />
        <el-button type="primary" :icon="Promotion" @click="handleSend" :loading="sending" :disabled="!messageContent.trim()">
          发送
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="reportDialogVisible" title="举报用户" width="500px">
      <el-input
        v-model="reportReason"
        type="textarea"
        :rows="4"
        placeholder="请输入举报原因"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.chat-window-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.chat-header-actions {
  margin-left: auto;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-user-name {
  font-size: 16px;
  font-weight: 600;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  max-height: 100%;
}

.message-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.is-self .message-content {
  align-items: flex-end;
}

.message-text {
  padding: 10px 14px;
  background: #f3f4f6;
  border-radius: 12px;
  word-wrap: break-word;
}

.message-item.is-self .message-text {
  background: #667eea;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #9ca3af;
  padding: 0 4px;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-shrink: 0;
  background: white;
  min-height: fit-content;
}

.chat-input :deep(.el-textarea) {
  flex: 1;
}

.chat-input :deep(.el-textarea__inner) {
  resize: none;
  max-height: 150px;
  overflow-y: auto;
}
</style>

