<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete, Warning, User, Plus, Search, Unlock } from '@element-plus/icons-vue'
import { getFriendList, getBlockedList, deleteFriend, blockFriend, unblockFriend, searchUsers, sendFriendRequest, type Friend, type UserProfile } from '../../api/friend'
import { createUserReport } from '../../api/userReport'
import { getAvatarUrl } from '../../utils/resource'
import PageHeader from '../../components/PageHeader.vue'

const router = useRouter()
const route = useRoute()

const isAdminRoute = computed(() => {
  return route.meta.admin || route.matched.some(record => record.meta.admin) || route.path.startsWith('/admin')
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

async function loadFriends() {
  loading.value = true
  try {
    const res = await getFriendList()
    friends.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleDelete(friend: Friend) {
  try {
    const isFromBlockedList = activeTab.value === 'blocked'
    const confirmMessage = isFromBlockedList ? '确定要删除该用户吗？删除后将无法恢复，且对方无法再发送好友申请。' : '确定要删除该好友吗？'
    await ElMessageBox.confirm(confirmMessage, '删除好友', { type: 'warning' })
    await deleteFriend(friend.userId)
    ElMessage.success('已删除')
    if (isFromBlockedList) {
      await loadBlockedFriends()
    } else {
      await loadFriends()
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

async function handleBlock(friend: Friend) {
  try {
    await ElMessageBox.confirm('确定要拉黑该好友吗？', '拉黑好友', { type: 'warning' })
    await blockFriend(friend.userId)
    ElMessage.success('已拉黑')
    await loadFriends()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

async function loadBlockedFriends() {
  blockedLoading.value = true
  try {
    const res = await getBlockedList()
    blockedFriends.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    blockedLoading.value = false
  }
}

async function handleUnblock(friend: Friend) {
  try {
    await ElMessageBox.confirm('确定要取消拉黑该用户吗？', '取消拉黑', { type: 'warning' })
    await unblockFriend(friend.userId)
    ElMessage.success('已取消拉黑')
    await loadBlockedFriends()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
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
    ElMessage.warning('请输入搜索关键词')
    return
  }
  searching.value = true
  try {
    const res = await searchUsers(keyword.value.trim(), 1, 20)
    searchResults.value = res.data || []
    if (searchResults.value.length === 0) {
      ElMessage.info('未找到相关用户')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '搜索失败')
  } finally {
    searching.value = false
  }
}

async function handleAddFriend(user: UserProfile) {
  try {
    const { value: message } = await ElMessageBox.prompt('请输入申请说明（可选）', '添加好友', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入申请说明...',
    })
    await sendFriendRequest(user.id, message || '')
    ElMessage.success('好友申请已发送')
    await handleSearch()
  } catch (e: any) {
    if (e === 'cancel') {
    }
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
    ElMessage.warning('请输入举报原因')
    return
  }
  if (!reportTargetUser.value) {
    return
  }
  try {
    const targetUserId = 'userId' in reportTargetUser.value 
      ? reportTargetUser.value.userId 
      : reportTargetUser.value.id
    await createUserReport(targetUserId, reportReason.value.trim())
    ElMessage.success('举报已提交，管理员将尽快处理')
    reportDialogVisible.value = false
  } catch (e: any) {
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
  if (!searchKeyword.value) {
    return friends.value
  }
  const kw = searchKeyword.value.toLowerCase()
  return friends.value.filter(friend => 
    friend.username.toLowerCase().includes(kw) || 
    (friend.nickname && friend.nickname.toLowerCase().includes(kw))
  )
})

function handleTabChange(tab: string) {
  activeTab.value = tab
  router.replace({ query: { tab } })
  keyword.value = ''
  searchKeyword.value = ''
  if (tab === 'friends') {
    loadFriends()
  } else if (tab === 'blocked') {
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
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的好友" name="friends">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><User /></el-icon>
              我的好友
            </span>
          </template>
          <div class="friends-tab-content">
            <div class="friends-search-toolbar">
              <el-input
                v-model="keyword"
                placeholder="搜索已添加的好友（用户名或昵称）"
                class="friend-search-input"
                clearable
                @keyup.enter="handleSearchFriends"
                @clear="handleClearSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" @click="handleSearchFriends" class="friend-search-btn">搜索</el-button>
            </div>
            <div v-loading="loading">
              <div v-if="filteredFriends.length === 0" class="empty-state">
                <el-empty :description="searchKeyword ? '未找到匹配的好友' : '暂无好友'" />
              </div>
              <div v-else class="friends-grid">
                <div v-for="friend in filteredFriends" :key="friend.id" class="friend-card">
                  <el-avatar :size="64" :src="getAvatarUrl(friend.avatar)" @click="handleViewProfile(friend)" style="cursor: pointer">
                    <el-icon :size="32"><User /></el-icon>
                  </el-avatar>
                  <div class="friend-info">
                    <div class="friend-name" @click="handleViewProfile(friend)" style="cursor: pointer">
                      {{ friend.nickname || friend.username }}
                    </div>
                    <div class="friend-username">{{ friend.username }}</div>
                  </div>
                  <div class="friend-actions">
                    <el-button size="small" type="primary" @click="handleChat(friend)">
                      <el-icon><ChatDotRound /></el-icon>
                      消息
                    </el-button>
                    <el-button size="small" @click="handleViewProfile(friend)">
                      <el-icon><User /></el-icon>
                      主页
                    </el-button>
                    <el-button size="small" type="danger" plain @click="handleDelete(friend)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                    <el-button size="small" type="warning" plain @click="handleBlock(friend)">
                      <el-icon><Warning /></el-icon>
                      拉黑
                    </el-button>
                    <el-button size="small" type="danger" plain @click="handleReport(friend)">
                      <el-icon><Warning /></el-icon>
                      举报
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="搜索添加" name="search">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><Search /></el-icon>
              搜索添加
            </span>
          </template>
          <div class="page-header-with-toolbar">
            <PageHeader title="搜索添加" />
            <div class="toolbar">
              <el-input
                v-model="keyword"
                placeholder="输入用户名或昵称搜索用户"
                class="friend-search-input"
                @keyup.enter="handleSearch"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" @click="handleSearch" :loading="searching" class="friend-search-btn">搜索</el-button>
            </div>
          </div>
          <div v-loading="searching">
            <div v-if="searchResults.length === 0 && !searching" class="empty-state">
              <el-empty description="请输入关键词搜索用户" />
            </div>
            <div v-else class="users-list">
              <div v-for="user in searchResults" :key="user.id" class="user-item">
                <el-avatar :size="56" :src="getAvatarUrl(user.avatar)" @click="handleViewUserProfile(user)" style="cursor: pointer">
                  <el-icon :size="28"><User /></el-icon>
                </el-avatar>
                <div class="user-info">
                  <div class="user-name" @click="handleViewUserProfile(user)" style="cursor: pointer">
                    {{ user.nickname || user.username }}
                  </div>
                  <div class="user-username">{{ user.username }}</div>
                  <div class="user-stats">发布了 {{ user.resourceCount || 0 }} 个资料</div>
                </div>
                <div class="user-actions">
                  <el-button v-if="user.friendStatus === 'NONE'" type="primary" @click="handleAddFriend(user)">
                    <el-icon><Plus /></el-icon>
                    添加好友
                  </el-button>
                  <el-button v-else-if="user.friendStatus === 'PENDING'" disabled>已发送申请</el-button>
                  <el-button v-else-if="user.friendStatus === 'FRIEND'" disabled>已是好友</el-button>
                  <el-button v-else-if="user.friendStatus === 'SELF'" disabled>这是你自己</el-button>
                  <el-button size="small" type="danger" plain @click="handleReport(user)" style="margin-left: 8px">
                    <el-icon><Warning /></el-icon>
                    举报
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="拉黑列表" name="blocked">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><Warning /></el-icon>
              拉黑列表
            </span>
          </template>
          <div v-loading="blockedLoading">
            <div v-if="blockedFriends.length === 0" class="empty-state">
              <el-empty description="暂无拉黑的用户" />
            </div>
            <div v-else class="friends-grid">
              <div v-for="friend in blockedFriends" :key="friend.id" class="friend-card blocked-card">
                <el-avatar :size="64" :src="getAvatarUrl(friend.avatar)" @click="handleViewProfile(friend)" style="cursor: pointer">
                  <el-icon :size="32"><User /></el-icon>
                </el-avatar>
                <div class="friend-info">
                  <div class="friend-name" @click="handleViewProfile(friend)" style="cursor: pointer">
                    {{ friend.nickname || friend.username }}
                  </div>
                  <div class="friend-username">{{ friend.username }}</div>
                  <el-tag type="warning" size="small" style="margin-top: 8px">已拉黑</el-tag>
                </div>
                <div class="friend-actions">
                  <el-button size="small" type="success" @click="handleUnblock(friend)">
                    <el-icon><Unlock /></el-icon>
                    取消拉黑
                  </el-button>
                  <el-button v-if="friend.hasFriendRelation === true" size="small" type="danger" plain @click="handleDelete(friend)">
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                  <el-button size="small" @click="handleViewProfile(friend)">
                    <el-icon><User /></el-icon>
                    主页
                  </el-button>
                  <el-button size="small" type="danger" plain @click="handleReport(friend)">
                    <el-icon><Warning /></el-icon>
                    举报
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="reportDialogVisible" title="举报用户" width="500px">
      <div v-if="reportTargetUser" style="margin-bottom: 16px">
        <el-tag>被举报用户：{{ (reportTargetUser as any).nickname || (reportTargetUser as any).username }}</el-tag>
      </div>
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
.friend-list-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.friends-tab-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.friends-search-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 0;
}

.page-header-with-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: clamp(20px, 4vw, 32px);
  padding-bottom: clamp(16px, 2.5vw, 20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.page-header-with-toolbar :deep(.page-header) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.friend-search-input {
  width: 300px;
  height: 40px;
}

.friend-search-input :deep(.el-input) {
  height: 40px;
}

.friend-search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  box-sizing: border-box;
}

.friend-search-input :deep(.el-input__inner) {
  height: 38px !important;
  line-height: 38px;
}

.friend-search-btn {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  padding: 0 20px;
  flex-shrink: 0;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  padding: 40px;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.friend-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
}

.friend-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
  border-color: rgba(64, 158, 255, 0.3);
}

.blocked-card {
  opacity: 0.8;
  border-color: #fbbf24;
}

.friend-info {
  text-align: center;
  flex: 1;
}

.friend-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.friend-username {
  font-size: 14px;
  color: #6b7280;
}

.friend-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}


.users-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
}

.user-item:hover {
  background: #f9fafb;
  border-color: rgba(64, 158, 255, 0.3);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.user-username {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.user-stats {
  font-size: 13px;
  color: #9ca3af;
}

.user-actions {
  flex-shrink: 0;
}

@media (max-width: 480px) {
  .friends-search-toolbar {
    width: 100%;
    justify-content: stretch;
  }
  
  .friends-search-toolbar .friend-search-input {
    width: 100%;
    flex: 1;
  }
  
  .friends-search-toolbar .friend-search-btn {
    width: 100%;
  }
  
  .page-header-with-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .page-header-with-toolbar :deep(.page-header) {
    width: 100%;
  }
  
  .toolbar {
    width: 100%;
    justify-content: stretch;
  }
  
  .friend-search-input {
    width: 100%;
    flex: 1;
  }
  
  .friend-search-btn {
    width: 100%;
  }
  
  .friends-grid {
    grid-template-columns: 1fr;
  }
  
  .friend-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .friend-actions .el-button {
    width: 100%;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .page-header-with-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .page-header-with-toolbar :deep(.page-header) {
    width: 100%;
  }
  
  .toolbar {
    width: 100%;
    justify-content: stretch;
  }
  
  .friend-search-input {
    flex: 1;
    min-width: 200px;
  }
  
  .friends-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}
</style>

