<script setup lang="ts">
import { onMounted, ref, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NAvatar,
  NBadge,
  NButton,
  NEmpty,
  NIcon,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
} from 'naive-ui'
import {
  ChatbubbleEllipsesOutline,
  CheckmarkOutline,
  CloseOutline,
  PersonOutline,
  PeopleOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { getConversations, getUnreadCount, type Conversation } from '../../api/message'
import {
  getAllRequests,
  acceptFriendRequest,
  rejectFriendRequest,
  deleteRequest,
  type FriendRequest,
} from '../../api/friend'
import { getAvatarUrl } from '../../utils/resource'
import {
  acknowledgeIncomingFriendRequestIds,
  pendingIncomingFriendRequestsNotAcknowledged,
} from '../../utils/friendRequestReminder'
import { message } from '../../utils/feedback'
import PageHeader from '../../components/PageHeader.vue'

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
  return (
    route.meta.admin ||
    route.matched.some((record: any) => record.meta.admin) ||
    route.path.startsWith('/admin')
  )
})

const reminderRole = computed(() => (isAdminRoute.value ? 'admin' : 'student') as 'admin' | 'student')

const pendingRequestsCount = computed(() =>
  pendingIncomingFriendRequestsNotAcknowledged(friendRequests.value, reminderRole.value),
)

function noteFriendRequestsTabSeen() {
  const ids = friendRequests.value.filter((r) => r.status === 'PENDING' && !r.isFromMe).map((r) => r.id)
  acknowledgeIncomingFriendRequestIds(ids, reminderRole.value)
}

async function loadConversations() {
  loading.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data || []
    await loadUnreadCount()
  } catch (e: any) {
    message.error(e.response?.data?.message || '加载失败')
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
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    requestsLoading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data || 0
  } catch {
    // ignore
  }
}

async function handleAccept(request: FriendRequest) {
  try {
    await acceptFriendRequest(request.id)
    message.success('已接受')
    await loadFriendRequests()
  } catch (e: any) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

async function handleReject(request: FriendRequest) {
  try {
    await rejectFriendRequest(request.id)
    message.success('已拒绝')
    await loadFriendRequests()
  } catch (e: any) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

async function handleDelete(request: FriendRequest) {
  try {
    await deleteRequest(request.id)
    message.success('已删除')
    await loadFriendRequests()
  } catch (e: any) {
    message.error(e.response?.data?.message || '删除失败')
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

function handleTabChange(tab: string | number) {
  const name = String(tab)
  activeTab.value = name
  router.replace({ query: { tab: name } })

  if (timer) {
    clearInterval(timer)
    timer = null
  }

  if (name === 'chat') {
    if (conversations.value.length === 0) {
      loadConversations()
    }
    timer = window.setInterval(loadConversations, 30000)
  } else if (name === 'requests') {
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
  void loadFriendRequests().then(() => {
    if (activeTab.value === 'requests') {
      noteFriendRequestsTabSeen()
    }
  })
  if (activeTab.value === 'chat') {
    loadConversations()
    timer = window.setInterval(loadConversations, 30000)
  } else if (activeTab.value === 'requests') {
    // list already loading
  } else {
    loadConversations()
    timer = window.setInterval(loadConversations, 30000)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="message-list-page">
    <PageHeader title="消息" subtitle="会话列表与好友申请" />

    <div class="panel glass-panel-strong">
      <NTabs v-model:value="activeTab" type="segment" animated @update:value="handleTabChange">
        <NTabPane name="chat">
          <template #tab>
            <span class="tab-label">
              <NIcon :component="ChatbubbleEllipsesOutline" :size="16" />
              聊天
              <NBadge v-if="unreadCount > 0" :value="unreadCount" :max="99" />
            </span>
          </template>
          <NSpin :show="loading">
            <div v-if="conversations.length === 0" class="empty-wrap">
              <NEmpty description="暂无消息" />
            </div>
            <div v-else class="conversations-list">
              <button
                v-for="conv in conversations"
                :key="conv.userId"
                type="button"
                class="conversation-item surface-card"
                @click="handleChat(conv)"
              >
                <NAvatar round :size="52" :src="getAvatarUrl(conv.avatar) || undefined">
                  <NIcon :size="24" :component="PersonOutline" />
                </NAvatar>
                <div class="conversation-info">
                  <div class="conversation-header">
                    <span class="conversation-name">{{ conv.nickname || conv.username }}</span>
                    <span class="conversation-time">{{ formatTime(conv.lastMessageTime) }}</span>
                  </div>
                  <div class="conversation-preview">
                    <span class="preview-text">{{ conv.lastMessage || '暂无消息' }}</span>
                    <NBadge v-if="conv.unreadCount > 0" :value="conv.unreadCount" :max="99" />
                  </div>
                </div>
              </button>
            </div>
          </NSpin>
        </NTabPane>

        <NTabPane name="requests">
          <template #tab>
            <span class="tab-label">
              <NIcon :component="PeopleOutline" :size="16" />
              好友申请
              <NBadge v-if="pendingRequestsCount > 0" :value="pendingRequestsCount" :max="99" />
            </span>
          </template>
          <NSpin :show="requestsLoading">
            <div v-if="friendRequests.length === 0" class="empty-wrap">
              <NEmpty description="暂无好友申请" />
            </div>
            <div v-else class="requests-list">
              <div v-for="request in friendRequests" :key="request.id" class="request-item surface-card">
                <NAvatar
                  round
                  :size="52"
                  :src="
                    getAvatarUrl(request.isFromMe ? request.toAvatar : request.fromAvatar) || undefined
                  "
                  class="clickable"
                  @click="handleViewProfile(request)"
                >
                  <NIcon :size="24" :component="PersonOutline" />
                </NAvatar>
                <div class="request-info">
                  <div class="request-header">
                    <div class="request-name clickable" @click="handleViewProfile(request)">
                      {{
                        request.isFromMe
                          ? request.toNickname || request.toUsername
                          : request.fromNickname || request.fromUsername
                      }}
                    </div>
                    <NTag v-if="request.status === 'PENDING'" type="warning" size="small" round>
                      {{ request.isFromMe ? '待对方处理' : '待处理' }}
                    </NTag>
                    <NTag v-else-if="request.status === 'ACCEPTED'" type="success" size="small" round>
                      已通过
                    </NTag>
                    <NTag v-else-if="request.status === 'REJECTED'" type="error" size="small" round>
                      已拒绝
                    </NTag>
                  </div>
                  <div class="request-username">
                    {{ request.isFromMe ? request.toUsername : request.fromUsername }}
                    <span class="request-direction">
                      {{ request.isFromMe ? '（我发出的）' : '（收到的）' }}
                    </span>
                  </div>
                  <div v-if="request.message" class="request-message">{{ request.message }}</div>
                  <div class="request-time">{{ new Date(request.createdAt).toLocaleString('zh-CN') }}</div>
                </div>
                <div class="request-actions">
                  <template v-if="request.status === 'PENDING' && !request.isFromMe">
                    <NButton type="success" size="small" @click="handleAccept(request)">
                      <template #icon>
                        <NIcon :component="CheckmarkOutline" />
                      </template>
                      接受
                    </NButton>
                    <NButton type="error" size="small" @click="handleReject(request)">
                      <template #icon>
                        <NIcon :component="CloseOutline" />
                      </template>
                      拒绝
                    </NButton>
                  </template>
                  <NButton secondary size="small" @click="handleDelete(request)">
                    <template #icon>
                      <NIcon :component="TrashOutline" />
                    </template>
                    删除
                  </NButton>
                </div>
              </div>
            </div>
          </NSpin>
        </NTabPane>
      </NTabs>
    </div>
  </div>
</template>

<style scoped>
.message-list-page {
  max-width: 900px;
  margin: 0 auto;
  padding: clamp(12px, 2vw, 28px);
}

.panel {
  padding: 18px 20px 22px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.empty-wrap {
  padding: 48px 16px;
}

.conversations-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--m-stroke);
  background: rgba(255, 255, 255, 0.72);
  border-radius: var(--m-radius-sm);
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.conversation-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--m-shadow-soft);
  border-color: rgba(122, 158, 142, 0.35);
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.conversation-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--m-ink);
}

.conversation-time {
  font-size: 12px;
  color: var(--m-ink-muted);
  flex-shrink: 0;
}

.conversation-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.preview-text {
  font-size: 14px;
  color: var(--m-ink-soft);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.requests-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.request-item:hover {
  border-color: rgba(122, 158, 142, 0.35);
  box-shadow: var(--m-shadow-soft);
}

.request-info {
  flex: 1;
  min-width: 0;
}

.request-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.request-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--m-ink);
}

.request-username {
  font-size: 13px;
  color: var(--m-ink-soft);
  margin-bottom: 8px;
}

.request-direction {
  color: var(--m-ink-muted);
  font-size: 12px;
}

.request-message {
  font-size: 14px;
  color: var(--m-ink);
  margin-bottom: 8px;
  padding: 10px 12px;
  background: rgba(216, 230, 222, 0.45);
  border-radius: 14px;
}

.request-time {
  font-size: 12px;
  color: var(--m-ink-muted);
}

.request-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.clickable {
  cursor: pointer;
}

@media (max-width: 640px) {
  .request-item {
    flex-direction: column;
    align-items: stretch;
  }

  .request-actions {
    justify-content: flex-start;
  }
}
</style>
