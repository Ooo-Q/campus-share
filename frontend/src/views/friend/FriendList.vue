<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NAvatar,
  NButton,
  NEmpty,
  NIcon,
  NInput,
  NModal,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
} from 'naive-ui'
import {
  ChatbubbleEllipsesOutline,
  LockOpenOutline,
  PersonAddOutline,
  PersonOutline,
  SearchOutline,
  TrashOutline,
  WarningOutline,
} from '@vicons/ionicons5'
import {
  getFriendList,
  getBlockedList,
  deleteFriend,
  blockFriend,
  unblockFriend,
  searchUsers,
  sendFriendRequest,
  type Friend,
  type UserProfile,
} from '../../api/friend'
import { createUserReport } from '../../api/userReport'
import { getAvatarUrl } from '../../utils/resource'
import { message, dialog } from '../../utils/feedback'
import PageHeader from '../../components/PageHeader.vue'

const router = useRouter()
const route = useRoute()

const isAdminRoute = computed(() => {
  return (
    route.meta.admin ||
    route.matched.some((record) => record.meta.admin) ||
    route.path.startsWith('/admin')
  )
})
const friends = ref<Friend[]>([])
const blockedFriends = ref<Friend[]>([])
const searchResults = ref<UserProfile[]>([])
const keyword = ref('')
const searchKeyword = ref('')
const loading = ref(false)
const blockedLoading = ref(false)
const searching = ref(false)
const activeTab = ref((route.query.tab as string) || 'friends')
const reportDialogVisible = ref(false)
const reportReason = ref('')
const reportTargetUser = ref<Friend | UserProfile | null>(null)

const addFriendVisible = ref(false)
const addFriendTarget = ref<UserProfile | null>(null)
const addFriendMessage = ref('')
const addFriendSubmitting = ref(false)

async function loadFriends() {
  loading.value = true
  try {
    const res = await getFriendList()
    friends.value = res.data || []
  } catch (e: any) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleDelete(friend: Friend) {
  const isFromBlockedList = activeTab.value === 'blocked'
  const confirmMessage = isFromBlockedList
    ? '确定要删除该用户吗？删除后将无法恢复，且对方无法再发送好友申请。'
    : '确定要删除该好友吗？'
  dialog.warning({
    title: '删除好友',
    content: confirmMessage,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteFriend(friend.userId)
        message.success('已删除')
        if (isFromBlockedList) {
          await loadBlockedFriends()
        } else {
          await loadFriends()
        }
      } catch (e: any) {
        message.error(e.response?.data?.message || '删除失败')
      }
    },
  })
}

function handleBlock(friend: Friend) {
  dialog.warning({
    title: '拉黑好友',
    content: '确定要拉黑该好友吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await blockFriend(friend.userId)
        message.success('已拉黑')
        await loadFriends()
      } catch (e: any) {
        message.error(e.response?.data?.message || '操作失败')
      }
    },
  })
}

async function loadBlockedFriends() {
  blockedLoading.value = true
  try {
    const res = await getBlockedList()
    blockedFriends.value = res.data || []
  } catch (e: any) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    blockedLoading.value = false
  }
}

function handleUnblock(friend: Friend) {
  dialog.warning({
    title: '取消拉黑',
    content: '确定要取消拉黑该用户吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await unblockFriend(friend.userId)
        message.success('已取消拉黑')
        await loadBlockedFriends()
      } catch (e: any) {
        message.error(e.response?.data?.message || '操作失败')
      }
    },
  })
}

function handleChat(friend: Friend) {
  if (isAdminRoute.value) {
    router.push({ name: 'AdminChatWindow', params: { userId: friend.userId } })
  } else {
    router.push({ name: 'ChatWindow', params: { userId: friend.userId } })
  }
}

function handleViewProfile(friend: Friend) {
  if (isAdminRoute.value) {
    router.push({ name: 'AdminUserHomePage', params: { userId: friend.userId } })
  } else {
    router.push({ name: 'UserHomePage', params: { userId: friend.userId } })
  }
}

async function handleSearch() {
  if (!keyword.value.trim()) {
    message.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  try {
    const res = await searchUsers(keyword.value.trim(), 1, 20)
    searchResults.value = res.data || []
    if (searchResults.value.length === 0) {
      message.info('未找到相关用户')
    }
  } catch (e: any) {
    message.error(e.response?.data?.message || '搜索失败')
  } finally {
    searching.value = false
  }
}

function handleAddFriend(user: UserProfile) {
  addFriendTarget.value = user
  addFriendMessage.value = ''
  addFriendVisible.value = true
}

async function confirmAddFriend() {
  if (!addFriendTarget.value) return
  addFriendSubmitting.value = true
  try {
    await sendFriendRequest(addFriendTarget.value.id, addFriendMessage.value || '')
    message.success('好友申请已发送')
    addFriendVisible.value = false
    await handleSearch()
  } catch {
    // API interceptor may surface errors
  } finally {
    addFriendSubmitting.value = false
  }
}

function handleViewUserProfile(user: UserProfile) {
  if (isAdminRoute.value) {
    router.push({ name: 'AdminUserHomePage', params: { userId: user.id } })
  } else {
    router.push({ name: 'UserHomePage', params: { userId: user.id } })
  }
}

function handleReport(friend: Friend | UserProfile) {
  reportTargetUser.value = friend
  reportDialogVisible.value = true
  reportReason.value = ''
}

async function confirmReport() {
  if (!reportReason.value.trim()) {
    message.warning('请输入举报原因')
    return
  }
  if (!reportTargetUser.value) return
  try {
    const targetUserId =
      'userId' in reportTargetUser.value
        ? reportTargetUser.value.userId
        : reportTargetUser.value.id
    await createUserReport(targetUserId, reportReason.value.trim())
    message.success('举报已提交，管理员将尽快处理')
    reportDialogVisible.value = false
  } catch {
    // handled upstream
  }
}

function handleSearchFriends() {
  searchKeyword.value = keyword.value.trim()
}

function handleClearSearch() {
  keyword.value = ''
  searchKeyword.value = ''
}

const filteredFriends = computed(() => {
  if (!searchKeyword.value) return friends.value
  const kw = searchKeyword.value.toLowerCase()
  return friends.value.filter(
    (friend) =>
      friend.username.toLowerCase().includes(kw) ||
      (friend.nickname && friend.nickname.toLowerCase().includes(kw)),
  )
})

function handleTabChange(tab: string | number) {
  const name = String(tab)
  activeTab.value = name
  router.replace({ query: { tab: name } })
  keyword.value = ''
  searchKeyword.value = ''
  if (name === 'friends') {
    loadFriends()
  } else if (name === 'blocked') {
    loadBlockedFriends()
  }
}

onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab as string
  }
  if (activeTab.value === 'blocked') {
    loadBlockedFriends()
  } else {
    loadFriends()
  }
})
</script>

<template>
  <div class="friend-list-page">
    <PageHeader title="好友" subtitle="管理好友、搜索新同学与拉黑列表" />

    <div class="panel glass-panel-strong">
      <NTabs v-model:value="activeTab" type="segment" animated @update:value="handleTabChange">
        <NTabPane name="friends" tab="我的好友">
          <div class="friends-tab-content">
            <div class="toolbar">
              <NInput
                v-model:value="keyword"
                clearable
                round
                placeholder="搜索已添加的好友（用户名或昵称）"
                class="search-input"
                @keyup.enter="handleSearchFriends"
                @clear="handleClearSearch"
              >
                <template #prefix>
                  <NIcon :component="SearchOutline" />
                </template>
              </NInput>
              <NButton type="primary" @click="handleSearchFriends">搜索</NButton>
            </div>

            <NSpin :show="loading">
              <div v-if="filteredFriends.length === 0" class="empty-wrap">
                <NEmpty :description="searchKeyword ? '未找到匹配的好友' : '暂无好友'" />
              </div>
              <div v-else class="friends-grid">
                <div v-for="friend in filteredFriends" :key="friend.id" class="friend-card surface-card">
                  <NAvatar
                    round
                    :size="64"
                    :src="getAvatarUrl(friend.avatar) || undefined"
                    class="clickable"
                    @click="handleViewProfile(friend)"
                  >
                    <NIcon :size="30" :component="PersonOutline" />
                  </NAvatar>
                  <div class="friend-info">
                    <div class="friend-name clickable" @click="handleViewProfile(friend)">
                      {{ friend.nickname || friend.username }}
                    </div>
                    <div class="friend-username">{{ friend.username }}</div>
                  </div>
                  <div class="friend-actions">
                    <NButton size="small" type="primary" @click="handleChat(friend)">
                      <template #icon>
                        <NIcon :component="ChatbubbleEllipsesOutline" />
                      </template>
                      消息
                    </NButton>
                    <NButton size="small" secondary @click="handleViewProfile(friend)">
                      <template #icon>
                        <NIcon :component="PersonOutline" />
                      </template>
                      主页
                    </NButton>
                    <NButton size="small" secondary type="error" @click="handleDelete(friend)">
                      <template #icon>
                        <NIcon :component="TrashOutline" />
                      </template>
                      删除
                    </NButton>
                    <NButton size="small" secondary type="warning" @click="handleBlock(friend)">
                      <template #icon>
                        <NIcon :component="WarningOutline" />
                      </template>
                      拉黑
                    </NButton>
                    <NButton size="small" quaternary type="error" @click="handleReport(friend)">
                      举报
                    </NButton>
                  </div>
                </div>
              </div>
            </NSpin>
          </div>
        </NTabPane>

        <NTabPane name="search" tab="搜索添加">
          <div class="friends-tab-content">
            <div class="toolbar">
              <NInput
                v-model:value="keyword"
                clearable
                round
                placeholder="输入用户名或昵称搜索用户"
                class="search-input"
                @keyup.enter="handleSearch"
              >
                <template #prefix>
                  <NIcon :component="SearchOutline" />
                </template>
              </NInput>
              <NButton type="primary" :loading="searching" @click="handleSearch">搜索</NButton>
            </div>

            <NSpin :show="searching">
              <div v-if="searchResults.length === 0 && !searching" class="empty-wrap">
                <NEmpty description="请输入关键词搜索用户" />
              </div>
              <div v-else class="users-list">
                <div v-for="user in searchResults" :key="user.id" class="user-item surface-card">
                  <NAvatar
                    round
                    :size="56"
                    :src="getAvatarUrl(user.avatar) || undefined"
                    class="clickable"
                    @click="handleViewUserProfile(user)"
                  >
                    <NIcon :size="26" :component="PersonOutline" />
                  </NAvatar>
                  <div class="user-info">
                    <div class="user-name clickable" @click="handleViewUserProfile(user)">
                      {{ user.nickname || user.username }}
                    </div>
                    <div class="user-username">{{ user.username }}</div>
                    <div class="user-stats">发布了 {{ user.resourceCount || 0 }} 个资料</div>
                  </div>
                  <div class="user-actions">
                    <NButton
                      v-if="user.friendStatus === 'NONE'"
                      type="primary"
                      @click="handleAddFriend(user)"
                    >
                      <template #icon>
                        <NIcon :component="PersonAddOutline" />
                      </template>
                      添加好友
                    </NButton>
                    <NButton v-else-if="user.friendStatus === 'PENDING'" disabled>已发送申请</NButton>
                    <NButton v-else-if="user.friendStatus === 'FRIEND'" disabled>已是好友</NButton>
                    <NButton v-else-if="user.friendStatus === 'SELF'" disabled>这是你自己</NButton>
                    <NButton size="small" quaternary type="error" @click="handleReport(user)">
                      举报
                    </NButton>
                  </div>
                </div>
              </div>
            </NSpin>
          </div>
        </NTabPane>

        <NTabPane name="blocked" tab="拉黑列表">
          <NSpin :show="blockedLoading">
            <div v-if="blockedFriends.length === 0" class="empty-wrap">
              <NEmpty description="暂无拉黑的用户" />
            </div>
            <div v-else class="friends-grid">
              <div
                v-for="friend in blockedFriends"
                :key="friend.id"
                class="friend-card surface-card blocked-card"
              >
                <NAvatar
                  round
                  :size="64"
                  :src="getAvatarUrl(friend.avatar) || undefined"
                  class="clickable"
                  @click="handleViewProfile(friend)"
                >
                  <NIcon :size="30" :component="PersonOutline" />
                </NAvatar>
                <div class="friend-info">
                  <div class="friend-name clickable" @click="handleViewProfile(friend)">
                    {{ friend.nickname || friend.username }}
                  </div>
                  <div class="friend-username">{{ friend.username }}</div>
                  <NTag type="warning" size="small" round style="margin-top: 8px">已拉黑</NTag>
                </div>
                <div class="friend-actions">
                  <NButton size="small" type="success" @click="handleUnblock(friend)">
                    <template #icon>
                      <NIcon :component="LockOpenOutline" />
                    </template>
                    取消拉黑
                  </NButton>
                  <NButton
                    v-if="friend.hasFriendRelation === true"
                    size="small"
                    secondary
                    type="error"
                    @click="handleDelete(friend)"
                  >
                    <template #icon>
                      <NIcon :component="TrashOutline" />
                    </template>
                    删除
                  </NButton>
                  <NButton size="small" secondary @click="handleViewProfile(friend)">主页</NButton>
                  <NButton size="small" quaternary type="error" @click="handleReport(friend)">
                    举报
                  </NButton>
                </div>
              </div>
            </div>
          </NSpin>
        </NTabPane>
      </NTabs>
    </div>

    <NModal
      v-model:show="reportDialogVisible"
      preset="card"
      title="举报用户"
      style="width: min(480px, 92vw)"
    >
      <div v-if="reportTargetUser" class="report-target">
        <NTag round>
          被举报用户：{{ (reportTargetUser as any).nickname || (reportTargetUser as any).username }}
        </NTag>
      </div>
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

    <NModal
      v-model:show="addFriendVisible"
      preset="card"
      title="添加好友"
      style="width: min(480px, 92vw)"
    >
      <p class="muted add-hint">
        向 {{ addFriendTarget?.nickname || addFriendTarget?.username }} 发送好友申请
      </p>
      <NInput
        v-model:value="addFriendMessage"
        type="textarea"
        :rows="3"
        placeholder="请输入申请说明（可选）"
      />
      <template #footer>
        <div class="modal-footer">
          <NButton @click="addFriendVisible = false">取消</NButton>
          <NButton type="primary" :loading="addFriendSubmitting" @click="confirmAddFriend">
            发送
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.friend-list-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: clamp(12px, 2vw, 28px);
}

.panel {
  padding: 20px 22px 24px;
}

.friends-tab-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: min(320px, 100%);
}

.empty-wrap {
  padding: 40px 16px;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.friend-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 22px 18px;
  gap: 14px;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.friend-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--m-shadow);
  border-color: rgba(122, 158, 142, 0.35);
}

.blocked-card {
  opacity: 0.9;
  border-color: rgba(196, 164, 132, 0.45);
}

.friend-info {
  text-align: center;
}

.friend-name,
.user-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--m-ink);
  margin-bottom: 4px;
}

.friend-username,
.user-username {
  font-size: 13px;
  color: var(--m-ink-soft);
}

.friend-actions,
.user-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.user-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--m-shadow-soft);
  border-color: rgba(122, 158, 142, 0.35);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-stats {
  margin-top: 4px;
  font-size: 12px;
  color: var(--m-ink-muted);
}

.clickable {
  cursor: pointer;
}

.report-target {
  margin-bottom: 14px;
}

.add-hint {
  margin: 0 0 12px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 640px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .user-item {
    flex-direction: column;
    align-items: stretch;
    text-align: center;
  }

  .user-actions {
    justify-content: center;
  }
}
</style>
