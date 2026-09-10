<script setup lang="ts">
import { onMounted, ref, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, User, UserFilled, Check, Close, Delete } from '@element-plus/icons-vue'
import { getConversations, getUnreadCount, type Conversation } from '../../api/message'
import { getAllRequests, acceptFriendRequest, rejectFriendRequest, deleteRequest, type FriendRequest } from '../../api/friend'
import { getAvatarUrl } from '../../utils/resource'
import {
  acknowledgeIncomingFriendRequestIds,
  pendingIncomingFriendRequestsNotAcknowledged,
} from '../../utils/friendRequestReminder'

const router = useRouter()
const route = useRoute()
const conversations = ref<Conversation[]>([])
const friendRequests = ref<FriendRequest[]>([])
const activeTab = ref((route.query.tab as string) || 'chat')
const loading = ref(false)
const requestsLoading = ref(false)
const unreadCount = ref(0)
let timer: number | null = null

const isAdminRoute = computed(() => {
  return route.meta.admin || route.matched.some((record: any) => record.meta.admin) || route.path.startsWith('/admin')
})

const reminderRole = computed(() => (isAdminRoute.value ? 'admin' : 'student') as 'admin' | 'student')

const pendingRequestsCount = computed(() =>
  pendingIncomingFriendRequestsNotAcknowledged(friendRequests.value, reminderRole.value),
)

function noteFriendRequestsTabSeen() {
  const ids = friendRequests.value.filter(r => r.status === 'PENDING' && !r.isFromMe).map(r => r.id)
  acknowledgeIncomingFriendRequestIds(ids, reminderRole.value)
}

async function loadConversations() {
  loading.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data || []
    await loadUnreadCount()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadFriendRequests() {
  requestsLoading.value = true
  try {
    const res = await getAllRequests()
    friendRequests.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    requestsLoading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) {
  }
}

async function handleAccept(request: FriendRequest) {
  try {
    await acceptFriendRequest(request.id)
    ElMessage.success('已接受')
    await loadFriendRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function handleReject(request: FriendRequest) {
  try {
    await rejectFriendRequest(request.id)
    ElMessage.success('已拒绝')
    await loadFriendRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function handleDelete(request: FriendRequest) {
  try {
    await deleteRequest(request.id)
    ElMessage.success('已删除')
    await loadFriendRequests()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function handleViewProfile(request: FriendRequest) {
  const userId = request.isFromMe ? request.toUserId : request.fromUserId
  if (userId) {
    if (isAdminRoute.value) {
      router.push({ name: 'AdminUserHomePage', params: { userId } })
    } else {
      router.push({ name: 'UserHomePage', params: { userId } })
    }
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab
  router.replace({ query: { tab } })

  if (timer) {
    clearInterval(timer)
    timer = null
  }
  
  if (tab === 'chat') {
    if (conversations.value.length === 0) {
      loadConversations()
    }
    timer = window.setInterval(loadConversations, 30000)
  } else if (tab === 'requests') {
    void loadFriendRequests().then(() => noteFriendRequestsTabSeen())
  }
}

function handleChat(conversation: Conversation) {
  if (isAdminRoute.value) {
    router.push({ name: 'AdminChatWindow', params: { userId: conversation.userId } })
  } else {
    router.push({ name: 'ChatWindow', params: { userId: conversation.userId } })
  }
}

function formatTime(dateStr?: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab as string
  }
  // 无论当前在哪个 Tab，都拉取好友申请，这样「好友申请」角标在聊天页也能显示
  void loadFriendRequests().then(() => {
    if (activeTab.value === 'requests') {
      noteFriendRequestsTabSeen()
    }
  })
  if (activeTab.value === 'chat') {
    loadConversations()
    timer = window.setInterval(loadConversations, 30000)
  } else if (activeTab.value === 'requests') {
    // 列表已在上面加载
  } else {
    loadConversations()
    timer = window.setInterval(loadConversations, 30000)
  }
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<template>
  <div class="message-list-page">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="聊天" name="chat">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><ChatDotRound /></el-icon>
              聊天
              <el-badge v-if="unreadCount > 0" :value="unreadCount" class="tab-badge" />
            </span>
          </template>
          <div v-loading="loading">
            <div v-if="conversations.length === 0" class="empty-state">
              <el-empty description="暂无消息" />
            </div>
            <div v-else class="conversations-list">
              <div
                v-for="conv in conversations"
                :key="conv.userId"
                class="conversation-item"
                @click="handleChat(conv)"
              >
                <el-avatar :size="56" :src="getAvatarUrl(conv.avatar)">
                  <el-icon :size="28"><User /></el-icon>
                </el-avatar>
                <div class="conversation-info">
                  <div class="conversation-header">
                    <span class="conversation-name">{{ conv.nickname || conv.username }}</span>
                    <span class="conversation-time">{{ formatTime(conv.lastMessageTime) }}</span>
                  </div>
                  <div class="conversation-preview">
                    <span class="preview-text">{{ conv.lastMessage || '暂无消息' }}</span>
                    <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" class="unread-badge" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="好友申请" name="requests">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><UserFilled /></el-icon>
              好友申请
              <el-badge v-if="pendingRequestsCount > 0" :value="pendingRequestsCount" class="tab-badge" />
            </span>
          </template>
          <div v-loading="requestsLoading">
            <div v-if="friendRequests.length === 0" class="empty-state">
              <el-empty description="暂无好友申请" />
            </div>
            <div v-else class="requests-list">
              <div v-for="request in friendRequests" :key="request.id" class="request-item">
                <el-avatar :size="56" :src="getAvatarUrl(request.isFromMe ? request.toAvatar : request.fromAvatar)" @click="handleViewProfile(request)" style="cursor: pointer">
                  <el-icon :size="28"><User /></el-icon>
                </el-avatar>
                <div class="request-info">
                  <div class="request-header">
                    <div class="request-name" @click="handleViewProfile(request)" style="cursor: pointer">
                      {{ request.isFromMe ? (request.toNickname || request.toUsername) : (request.fromNickname || request.fromUsername) }}
                    </div>
                    <el-tag v-if="request.status === 'PENDING'" type="warning" size="small">
                      {{ request.isFromMe ? '待对方处理' : '待处理' }}
                    </el-tag>
                    <el-tag v-else-if="request.status === 'ACCEPTED'" type="success" size="small">已通过</el-tag>
                    <el-tag v-else-if="request.status === 'REJECTED'" type="danger" size="small">已拒绝</el-tag>
                  </div>
                  <div class="request-username">
                    {{ request.isFromMe ? request.toUsername : request.fromUsername }}
                    <span v-if="request.isFromMe" class="request-direction">（我发出的）</span>
                    <span v-else class="request-direction">（收到的）</span>
                  </div>
                  <div v-if="request.message" class="request-message">{{ request.message }}</div>
                  <div class="request-time">{{ new Date(request.createdAt).toLocaleString('zh-CN') }}</div>
                </div>
                <div class="request-actions">
                  <template v-if="request.status === 'PENDING' && !request.isFromMe">
                    <el-button type="success" @click="handleAccept(request)">
                      <el-icon><Check /></el-icon>
                      接受
                    </el-button>
                    <el-button type="danger" @click="handleReject(request)">
                      <el-icon><Close /></el-icon>
                      拒绝
                    </el-button>
                  </template>
                  <el-button type="info" plain @click="handleDelete(request)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.message-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-state {
  padding: 40px;
}

.conversations-list {
  display: flex;
  flex-direction: column;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: #f9fafb;
}

.conversation-item:last-child {
  border-bottom: none;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.conversation-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.conversation-time {
  font-size: 12px;
  color: #9ca3af;
}

.conversation-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-text {
  font-size: 14px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.unread-badge {
  margin-left: 8px;
}

.tab-badge {
  margin-left: 4px;
}

.requests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  transition: all 0.2s;
}

.request-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.request-info {
  flex: 1;
}

.request-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.request-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.request-username {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.request-direction {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 4px;
}

.request-message {
  font-size: 14px;
  color: #374151;
  margin-bottom: 8px;
  padding: 8px;
  background: #f9fafb;
  border-radius: 6px;
}

.request-time {
  font-size: 12px;
  color: #9ca3af;
}

.request-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}
</style>

