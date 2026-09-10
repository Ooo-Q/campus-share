<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, ChatDotRound, UserFilled, Delete, Warning, Edit, View, Hide, Document } from '@element-plus/icons-vue'
import { getUserProfile, sendFriendRequest, deleteFriend, blockFriend, type UserProfile } from '../api/friend'
import { useUserStore } from '../stores/user'
import type { Resource } from '../api/resource'
import { createUserReport } from '../api/userReport'
import { createUserPunishment } from '../api/punishment'
import { getAvatarUrl } from '../utils/resource'
import request from '../api/request'
import { updateResourceVisibility } from '../api/resource'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => Number(route.params.userId))
const profile = ref<UserProfile | null>(null)
const loading = ref(false)
const resources = ref<Resource[]>([])
const resourcesLoading = ref(false)
const isAdmin = computed(() => userStore.user?.role === 'ADMIN')
const reportDialogVisible = ref(false)
const reportReason = ref('')
const punishmentDialogVisible = ref(false)
const punishmentType = ref<'WARNING' | 'MUTE' | ''>('')
const punishmentDuration = ref<number | null>(null)
const punishmentReason = ref('')
const submitting = ref(false)

async function loadProfile() {
  loading.value = true
  try {
    const res: any = await getUserProfile(userId.value)
    profile.value = res.data
    if (profile.value) {
      await loadResources()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
    router.back()
  } finally {
    loading.value = false
  }
}

async function loadResources() {
  if (!profile.value) return
  resourcesLoading.value = true
  try {
    const res: any = await request.get(`/users/${userId.value}/resources`, {
      params: { page: 1, size: 100 },
    })
    const pageData = res.data || {}
    resources.value = pageData.records || []
  } catch (e) {
    console.error('加载资料失败', e)
  } finally {
    resourcesLoading.value = false
  }
}

async function handleAddFriend() {
  if (!profile.value) return
  try {
    const { value: message } = await ElMessageBox.prompt('请输入申请说明（可选）', '添加好友', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请输入申请说明...',
    })
    await sendFriendRequest(profile.value.id, message || '')
    ElMessage.success('好友申请已发送')
    await loadProfile()
  } catch (e: any) {
    if (e === 'cancel') {
    }
  }
}

async function handleDeleteFriend() {
  if (!profile.value) return
  try {
    await ElMessageBox.confirm('确定要删除该好友吗？', '删除好友', {
      type: 'warning',
    })
    await deleteFriend(profile.value.id)
    ElMessage.success('已删除好友')
    await loadProfile()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

async function handleBlockFriend() {
  if (!profile.value) return
  try {
    await ElMessageBox.confirm('确定要拉黑该用户吗？拉黑后将无法接收对方的消息。', '拉黑用户', {
      type: 'warning',
    })
    await blockFriend(profile.value.id)
    ElMessage.success('已拉黑')
    await loadProfile()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

function handleSendMessage() {
  const basePath = isAdmin.value ? '/admin' : '/student'
  router.push(`${basePath}/messages/chat/${userId.value}`)
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function handleEditResource(resource: Resource) {
  const basePath = route.path.startsWith('/admin') ? '/admin' : '/student'
  router.push(`${basePath}/upload?id=${resource.id}`)
}

async function handleDeleteResource(resource: Resource) {
  try {
    await ElMessageBox.confirm(`确定要删除资料 "${resource.title}" 吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await request.delete(`/resources/${resource.id}`)
    ElMessage.success('删除成功')
    await loadResources()
    if (profile.value) {
      profile.value.resourceCount = (profile.value.resourceCount || 0) - 1
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

async function handleToggleVisibility(resource: Resource) {
  const target = resource.visibility === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  try {
    await updateResourceVisibility(resource.id, target)
    ElMessage.success(target === 'HIDDEN' ? '已隐藏该资料' : '已恢复可见')
    await loadResources()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function handleReport() {
  if (!profile.value) return
  reportDialogVisible.value = true
}

async function confirmReport() {
  if (!profile.value || !reportReason.value.trim()) {
    ElMessage.warning('请输入举报原因')
    return
  }
  try {
    await createUserReport(profile.value.id, reportReason.value.trim())
    ElMessage.success('举报已提交')
    reportDialogVisible.value = false
    reportReason.value = ''
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '举报失败')
  }
}

function handlePunish() {
  if (!profile.value) return
  punishmentDialogVisible.value = true
  punishmentType.value = ''
  punishmentDuration.value = null
  punishmentReason.value = ''
}

async function confirmPunish() {
  if (!profile.value || !punishmentType.value) {
    ElMessage.warning('请选择处罚类型')
    return
  }
  submitting.value = true
  try {
    await createUserPunishment(
      profile.value.id,
      punishmentType.value,
      punishmentDuration.value,
      punishmentReason.value || '管理员直接处罚'
    )
    ElMessage.success('处罚已创建')
    punishmentDialogVisible.value = false
    punishmentType.value = ''
    punishmentDuration.value = null
    punishmentReason.value = ''
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '处罚失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="user-home-page" v-loading="loading">
    <el-card v-if="profile" class="profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <el-avatar :size="120" :src="getAvatarUrl(profile.avatar)" class="user-avatar">
            <el-icon :size="60"><User /></el-icon>
          </el-avatar>
        </div>
        <div class="info-section">
          <h1 class="user-name">{{ profile.nickname || profile.username }}</h1>
          <div class="user-meta">
            <span class="meta-item">用户名：{{ profile.username }}</span>
            <span class="meta-item" v-if="profile.gender">性别：{{ profile.gender }}</span>
            <span class="meta-item">注册时间：{{ formatDate(profile.createdAt) }}</span>
          </div>
          <div class="stats-section">
            <div class="stat-item">
              <span class="stat-value">{{ profile.resourceCount || 0 }}</span>
              <span class="stat-label">发布的资料</span>
            </div>
          </div>
        </div>
        <div class="action-section" v-if="userStore.token">
          <template v-if="profile.friendStatus === 'SELF'">
            <el-button type="primary" @click="router.push('/student/settings')">
              <el-icon><UserFilled /></el-icon>
              编辑资料
            </el-button>
          </template>
          <template v-else-if="profile.friendStatus === 'NONE'">
            <el-button type="primary" @click="handleAddFriend">
              <el-icon><UserFilled /></el-icon>
              添加好友
            </el-button>
          </template>
          <template v-else-if="profile.friendStatus === 'PENDING'">
            <el-button disabled>已发送申请</el-button>
          </template>
          <template v-else-if="profile.friendStatus === 'FRIEND'">
            <el-button type="primary" @click="handleSendMessage">
              <el-icon><ChatDotRound /></el-icon>
              发送消息
            </el-button>
            <el-button @click="handleDeleteFriend">
              <el-icon><Delete /></el-icon>
              删除好友
            </el-button>
            <el-button type="danger" plain @click="handleBlockFriend">
              <el-icon><Warning /></el-icon>
              拉黑
            </el-button>
          </template>
          <template v-if="!isAdmin && profile.friendStatus !== 'SELF'">
            <el-button type="danger" plain @click="handleReport">
              <el-icon><Warning /></el-icon>
              举报
            </el-button>
          </template>
          <template v-if="isAdmin && profile.friendStatus !== 'SELF'">
            <el-button type="danger" @click="handlePunish">
              <el-icon><Warning /></el-icon>
              处罚
            </el-button>
          </template>
        </div>
      </div>
    </el-card>

    <el-card class="resources-card" v-if="profile">
      <template #header>
        <span>发布的资料</span>
      </template>
      <div v-loading="resourcesLoading">
        <div v-if="resources.length === 0" class="empty-resources">暂无发布的资料</div>
        <div v-else class="resources-list">
          <div v-for="resource in resources" :key="resource.id" class="resource-item">
            <el-icon class="resource-icon"><Document /></el-icon>
            <div class="resource-info" @click="router.push({ path: route.path.startsWith('/admin') ? `/admin/resources/${resource.id}` : `/student/resources/${resource.id}`, query: { from: 'profile' } })">
              <div class="resource-title">
                {{ resource.title }}
                <el-tag v-if="resource.visibility === 'HIDDEN'" type="warning" size="small" style="margin-left: 8px">已隐藏</el-tag>
              </div>
              <div class="resource-meta">
                <span>浏览 {{ resource.viewCount || 0 }}</span>
                <span>下载 {{ resource.downloadCount || 0 }}</span>
                <span>点赞 {{ resource.likeCount || 0 }}</span>
              </div>
            </div>
            <div class="resource-right" v-if="isAdmin">
              <div class="resource-actions">
                <el-button
                  size="small"
                  text
                  type="info"
                  :icon="Edit"
                  @click.stop="handleEditResource(resource)"
                >
                  修改
                </el-button>
                <el-button
                  size="small"
                  text
                  type="warning"
                  :icon="resource.visibility === 'HIDDEN' ? View : Hide"
                  @click.stop="handleToggleVisibility(resource)"
                >
                  {{ resource.visibility === 'HIDDEN' ? '恢复可见' : '隐藏' }}
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleDeleteResource(resource)"
                >
                  删除
                </el-button>
              </div>
              <div class="resource-time">{{ formatDate(resource.createdAt) }}</div>
            </div>
          </div>
        </div>
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

    <el-dialog v-model="punishmentDialogVisible" title="处罚用户" width="600px">
      <el-form label-width="120px">
        <el-form-item label="处罚类型" required>
          <el-select v-model="punishmentType" placeholder="选择处罚类型" style="width: 100%">
            <el-option label="警告" value="WARNING" />
            <el-option label="禁言" value="MUTE" />
          </el-select>
        </el-form-item>
        <el-form-item label="处罚时长(天)">
          <el-input-number
            v-model="punishmentDuration"
            :min="1"
            :max="365"
            placeholder="留空为永久"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="处罚原因">
          <el-input
            v-model="punishmentReason"
            type="textarea"
            :rows="3"
            placeholder="请输入处罚原因（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="punishmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmPunish">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-home-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.profile-card {
  margin-bottom: 24px;
}

.profile-header {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.avatar-section {
  flex-shrink: 0;
}

.user-avatar {
  border: 4px solid #f0f0f0;
}

.info-section {
  flex: 1;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: #1f2937;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
  color: #6b7280;
}

.meta-item {
  font-size: 14px;
}

.stats-section {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #667eea;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 120px;
}

.resources-card {
  margin-bottom: 24px;
}

.empty-resources {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.resources-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.resource-item:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.resource-icon {
  font-size: 32px;
  color: #667eea;
  flex-shrink: 0;
}

.resource-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.resource-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.resource-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #6b7280;
}

.resource-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
  flex-direction: column;
  align-items: flex-end;
}

.resource-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.resource-actions :deep(.el-button) {
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

.resource-actions :deep(.el-button__icon) {
  margin-right: 4px;
  display: inline-flex;
  align-items: center;
}

.resource-actions :deep(.el-button:first-child) {
  min-width: 60px;
  width: 60px;
}

.resource-actions :deep(.el-button:nth-child(2)) {
  min-width: 90px;
  width: 90px;
}

.resource-time {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .action-section {
    width: 100%;
  }
}
</style>

