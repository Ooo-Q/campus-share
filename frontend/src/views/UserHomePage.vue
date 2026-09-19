<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NTag,
  NSpin,
  NEmpty,
  NModal,
  NInput,
  NSelect,
  NInputNumber,
  NForm,
  NFormItem,
  NSpace,
} from 'naive-ui'
import {
  ChatbubbleOutline,
  PersonAddOutline,
  TrashOutline,
  WarningOutline,
  CreateOutline,
  EyeOutline,
  EyeOffOutline,
  DocumentOutline,
} from '@vicons/ionicons5'
import { getUserProfile, sendFriendRequest, deleteFriend, blockFriend, type UserProfile } from '../api/friend'
import { useUserStore } from '../stores/user'
import type { Resource } from '../api/resource'
import { createUserReport } from '../api/userReport'
import { createUserPunishment } from '../api/punishment'
import request from '../api/request'
import { updateResourceVisibility } from '../api/resource'
import { message, dialog } from '../utils/feedback'
import PageHeader from '../components/PageHeader.vue'
import UserAvatar from '../components/UserAvatar.vue'

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
const punishmentType = ref<'WARNING' | 'MUTE' | null>(null)
const punishmentDuration = ref<number | null>(null)
const punishmentReason = ref('')
const submitting = ref(false)
const friendRequestVisible = ref(false)
const friendRequestMessage = ref('')

const punishmentOptions = [
  { label: '警告', value: 'WARNING' },
  { label: '禁言', value: 'MUTE' },
]

async function loadProfile() {
  loading.value = true
  try {
    const res: any = await getUserProfile(userId.value)
    profile.value = res.data
    if (profile.value) {
      await loadResources()
    }
  } catch (e: any) {
    message.error(e.response?.data?.message || '加载失败')
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

function handleAddFriend() {
  if (!profile.value) return
  friendRequestMessage.value = ''
  friendRequestVisible.value = true
}

async function confirmAddFriend() {
  if (!profile.value) return
  try {
    await sendFriendRequest(profile.value.id, friendRequestMessage.value || '')
    message.success('好友申请已发送')
    friendRequestVisible.value = false
    friendRequestMessage.value = ''
    await loadProfile()
  } catch (e: any) {
    message.error(e.response?.data?.message || '发送失败')
  }
}

function handleDeleteFriend() {
  if (!profile.value) return
  dialog.warning({
    title: '删除好友',
    content: '确定要删除该好友吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteFriend(profile.value!.id)
        message.success('已删除好友')
        await loadProfile()
      } catch (e: any) {
        message.error(e.response?.data?.message || '删除失败')
      }
    },
  })
}

function handleBlockFriend() {
  if (!profile.value) return
  dialog.warning({
    title: '拉黑用户',
    content: '确定要拉黑该用户吗？拉黑后将无法接收对方的消息。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await blockFriend(profile.value!.id)
        message.success('已拉黑')
        await loadProfile()
      } catch (e: any) {
        message.error(e.response?.data?.message || '操作失败')
      }
    },
  })
}

function handleSendMessage() {
  const basePath = isAdmin.value ? '/admin' : '/student'
  router.push(`${basePath}/messages/chat/${userId.value}`)
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function handleEditResource(resource: Resource) {
  const basePath = route.path.startsWith('/admin') ? '/admin' : '/student'
  router.push(`${basePath}/upload?id=${resource.id}`)
}

function handleDeleteResource(resource: Resource) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除资料 "${resource.title}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/resources/${resource.id}`)
        message.success('删除成功')
        await loadResources()
        if (profile.value) {
          profile.value.resourceCount = (profile.value.resourceCount || 0) - 1
        }
      } catch (e: any) {
        message.error(e.response?.data?.message || '删除失败')
      }
    },
  })
}

async function handleToggleVisibility(resource: Resource) {
  const target = resource.visibility === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  try {
    await updateResourceVisibility(resource.id, target)
    message.success(target === 'HIDDEN' ? '已隐藏该资料' : '已恢复可见')
    await loadResources()
  } catch (e: any) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

function handleReport() {
  if (!profile.value) return
  reportDialogVisible.value = true
}

async function confirmReport() {
  if (!profile.value || !reportReason.value.trim()) {
    message.warning('请输入举报原因')
    return
  }
  try {
    await createUserReport(profile.value.id, reportReason.value.trim())
    message.success('举报已提交')
    reportDialogVisible.value = false
    reportReason.value = ''
  } catch (e: any) {
    message.error(e.response?.data?.message || '举报失败')
  }
}

function handlePunish() {
  if (!profile.value) return
  punishmentDialogVisible.value = true
  punishmentType.value = null
  punishmentDuration.value = null
  punishmentReason.value = ''
}

async function confirmPunish() {
  if (!profile.value || !punishmentType.value) {
    message.warning('请选择处罚类型')
    return
  }
  submitting.value = true
  try {
    await createUserPunishment(
      profile.value.id,
      punishmentType.value,
      punishmentDuration.value,
      punishmentReason.value || '管理员直接处罚',
    )
    message.success('处罚已创建')
    punishmentDialogVisible.value = false
    punishmentType.value = null
    punishmentDuration.value = null
    punishmentReason.value = ''
  } catch (e: any) {
    message.error(e.response?.data?.message || '处罚失败')
  } finally {
    submitting.value = false
  }
}

function openResource(resource: Resource) {
  router.push({
    path: route.path.startsWith('/admin')
      ? `/admin/resources/${resource.id}`
      : `/student/resources/${resource.id}`,
    query: { from: 'profile' },
  })
}

onMounted(loadProfile)
</script>

<template>
  <div class="page">
    <PageHeader title="用户主页" />

    <NSpin :show="loading">
      <template v-if="profile">
        <div class="profile glass-panel-strong">
          <UserAvatar
            :src="profile.avatar"
            :name="profile.nickname || profile.username"
            :size="96"
          />
          <div class="profile-info">
            <h2 class="name">{{ profile.nickname || profile.username }}</h2>
            <div class="meta muted">
              <span>用户名：{{ profile.username }}</span>
              <span v-if="profile.gender">性别：{{ profile.gender }}</span>
              <span>注册时间：{{ formatDate(profile.createdAt) }}</span>
            </div>
            <div class="stat-pill">
              <span class="stat-value">{{ profile.resourceCount || 0 }}</span>
              <span class="muted">发布的资料</span>
            </div>
          </div>
          <div v-if="userStore.token" class="actions">
            <template v-if="profile.friendStatus === 'SELF'">
              <NButton type="primary" @click="router.push('/student/settings')">
                <template #icon><NIcon :component="PersonAddOutline" /></template>
                编辑资料
              </NButton>
            </template>
            <template v-else-if="profile.friendStatus === 'NONE'">
              <NButton type="primary" @click="handleAddFriend">
                <template #icon><NIcon :component="PersonAddOutline" /></template>
                添加好友
              </NButton>
            </template>
            <template v-else-if="profile.friendStatus === 'PENDING'">
              <NButton disabled>已发送申请</NButton>
            </template>
            <template v-else-if="profile.friendStatus === 'FRIEND'">
              <NButton type="primary" @click="handleSendMessage">
                <template #icon><NIcon :component="ChatbubbleOutline" /></template>
                发送消息
              </NButton>
              <NButton @click="handleDeleteFriend">
                <template #icon><NIcon :component="TrashOutline" /></template>
                删除好友
              </NButton>
              <NButton type="error" secondary @click="handleBlockFriend">
                <template #icon><NIcon :component="WarningOutline" /></template>
                拉黑
              </NButton>
            </template>
            <NButton
              v-if="!isAdmin && profile.friendStatus !== 'SELF'"
              type="error"
              secondary
              @click="handleReport"
            >
              <template #icon><NIcon :component="WarningOutline" /></template>
              举报
            </NButton>
            <NButton
              v-if="isAdmin && profile.friendStatus !== 'SELF'"
              type="error"
              @click="handlePunish"
            >
              <template #icon><NIcon :component="WarningOutline" /></template>
              处罚
            </NButton>
          </div>
        </div>

        <div class="glass-panel resources-panel">
          <h3 class="section-title">发布的资料</h3>
          <NSpin :show="resourcesLoading">
            <NEmpty v-if="resources.length === 0" description="暂无发布的资料" />
            <div v-else class="resources">
              <div
                v-for="resource in resources"
                :key="resource.id"
                class="resource-item surface-card"
              >
                <div class="res-icon">
                  <NIcon :size="22" :component="DocumentOutline" />
                </div>
                <div class="res-info" @click="openResource(resource)">
                  <div class="res-title">
                    {{ resource.title }}
                    <NTag
                      v-if="resource.visibility === 'HIDDEN'"
                      type="warning"
                      size="small"
                      :bordered="false"
                    >
                      已隐藏
                    </NTag>
                  </div>
                  <div class="res-meta muted">
                    <span>浏览 {{ resource.viewCount || 0 }}</span>
                    <span>下载 {{ resource.downloadCount || 0 }}</span>
                    <span>点赞 {{ resource.likeCount || 0 }}</span>
                  </div>
                </div>
                <div v-if="isAdmin" class="res-admin">
                  <NSpace size="small">
                    <NButton size="tiny" quaternary @click.stop="handleEditResource(resource)">
                      <template #icon><NIcon :component="CreateOutline" /></template>
                      修改
                    </NButton>
                    <NButton size="tiny" quaternary type="warning" @click.stop="handleToggleVisibility(resource)">
                      <template #icon>
                        <NIcon :component="resource.visibility === 'HIDDEN' ? EyeOutline : EyeOffOutline" />
                      </template>
                      {{ resource.visibility === 'HIDDEN' ? '恢复可见' : '隐藏' }}
                    </NButton>
                    <NButton size="tiny" quaternary type="error" @click.stop="handleDeleteResource(resource)">
                      <template #icon><NIcon :component="TrashOutline" /></template>
                      删除
                    </NButton>
                  </NSpace>
                  <div class="muted time">{{ formatDate(resource.createdAt) }}</div>
                </div>
              </div>
            </div>
          </NSpin>
        </div>
      </template>
    </NSpin>

    <NModal
      v-model:show="friendRequestVisible"
      preset="card"
      title="添加好友"
      style="width: min(420px, 92vw)"
      :bordered="false"
    >
      <NInput
        v-model:value="friendRequestMessage"
        type="textarea"
        :rows="3"
        placeholder="请输入申请说明（可选）..."
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="friendRequestVisible = false">取消</NButton>
          <NButton type="primary" @click="confirmAddFriend">发送</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal
      v-model:show="reportDialogVisible"
      preset="card"
      title="举报用户"
      style="width: min(500px, 92vw)"
      :bordered="false"
    >
      <NInput
        v-model:value="reportReason"
        type="textarea"
        :rows="4"
        placeholder="请输入举报原因"
        maxlength="500"
        show-count
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="reportDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="confirmReport">提交举报</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal
      v-model:show="punishmentDialogVisible"
      preset="card"
      title="处罚用户"
      style="width: min(560px, 92vw)"
      :bordered="false"
    >
      <NForm label-placement="top">
        <NFormItem label="处罚类型" required>
          <NSelect
            v-model:value="punishmentType"
            :options="punishmentOptions"
            placeholder="选择处罚类型"
          />
        </NFormItem>
        <NFormItem label="处罚时长(天)">
          <NInputNumber
            v-model:value="punishmentDuration"
            :min="1"
            :max="365"
            placeholder="留空为永久"
            style="width: 100%"
          />
        </NFormItem>
        <NFormItem label="处罚原因">
          <NInput
            v-model:value="punishmentReason"
            type="textarea"
            :rows="3"
            placeholder="请输入处罚原因（可选）"
            maxlength="500"
            show-count
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="punishmentDialogVisible = false">取消</NButton>
          <NButton type="primary" :loading="submitting" @click="confirmPunish">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page {
  max-width: 980px;
  margin: 0 auto;
  width: 100%;
  padding: clamp(12px, 2vw, 24px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 24px;
  flex-wrap: wrap;
}

.profile-info {
  flex: 1;
  min-width: 200px;
}

.name {
  margin: 0 0 10px;
  font-size: clamp(22px, 3vw, 28px);
  font-weight: 600;
  letter-spacing: -0.02em;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  font-size: 13px;
  margin-bottom: 16px;
}

.stat-pill {
  display: inline-flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 16px;
  border-radius: var(--m-radius-sm);
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid var(--m-stroke);
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: var(--m-sage-deep);
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 140px;
}

.resources-panel {
  padding: 20px 22px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}

.resources {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resource-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.resource-item:hover {
  border-color: rgba(122, 158, 142, 0.35);
  transform: translateY(-1px);
}

.res-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(122, 158, 142, 0.16);
  color: var(--m-sage-deep);
  flex-shrink: 0;
}

.res-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.res-title {
  font-weight: 600;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.res-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
}

.res-admin {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.time {
  font-size: 12px;
}

@media (max-width: 768px) {
  .profile {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .meta {
    justify-content: center;
  }

  .actions {
    width: 100%;
  }

  .resource-item {
    flex-wrap: wrap;
  }

  .res-admin {
    width: 100%;
    align-items: flex-start;
  }
}
</style>
