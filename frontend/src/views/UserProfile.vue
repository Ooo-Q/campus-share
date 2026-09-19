<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NEmpty,
  NIcon,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
} from 'naive-ui'
import {
  CreateOutline,
  DocumentTextOutline,
  EyeOutline,
  EyeOffOutline,
  HeartOutline,
  StarOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { getUserProfile, type UserProfile } from '../api/friend'
import { useUserStore } from '../stores/user'
import request from '../api/request'
import { updateResourceVisibility } from '../api/resource'
import type { Resource } from '../api/resource'
import { message, dialog } from '../utils/feedback'
import PageHeader from '../components/PageHeader.vue'
import UserAvatar from '../components/UserAvatar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const profile = ref<UserProfile | null>(null)
const resources = ref<Resource[]>([])
const favorites = ref<any[]>([])
const activeTab = ref((route.query.tab as string) || 'resources')
const loading = ref(false)
const resourcesLoading = ref(false)
const favoritesLoading = ref(false)

const basePath = () => (route.path.startsWith('/admin') ? '/admin' : '/student')

async function loadProfile() {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  loading.value = true
  try {
    const res = await getUserProfile(userStore.user.userId)
    profile.value = res.data
    if (res.data?.avatar) {
      userStore.patchUser({ avatar: res.data.avatar })
    }
    if (activeTab.value === 'resources') {
      await loadResources()
    } else if (activeTab.value === 'favorites') {
      await loadFavorites()
    }
  } catch (e: any) {
    message.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadResources() {
  if (!userStore.user) return
  resourcesLoading.value = true
  try {
    const res: any = await request.get('/resources/my', {
      params: { page: 1, size: 10 },
    })
    const pageData = res.data || {}
    resources.value = pageData.records || []
  } catch (e) {
    console.error('加载资料失败', e)
  } finally {
    resourcesLoading.value = false
  }
}

async function loadFavorites() {
  if (!userStore.user) return
  favoritesLoading.value = true
  try {
    const res = await request.get<{ success: boolean; data: any[] }>('/favorites')
    const favs = res.data || []
    const results = await Promise.allSettled(
      favs.map(async (f: any) => {
        try {
          const detail = await request.get<{ success: boolean; data: Resource }>(`/resources/${f.resourceId}`)
          const visibility = detail.data.visibility || 'VISIBLE'
          return {
            ...f,
            resource: detail.data,
            title: detail.data.title,
            visibility,
            status: visibility === 'HIDDEN' ? 'hidden' : 'ok',
          }
        } catch {
          return {
            ...f,
            status: 'missing' as const,
            title: '关联资料已删除或不可访问',
          }
        }
      }),
    )
    favorites.value = results
      .map((r) => (r.status === 'fulfilled' ? r.value : null))
      .filter(Boolean) as any[]
  } catch {
    message.error('加载收藏失败')
  } finally {
    favoritesLoading.value = false
  }
}

async function toggleFavorite(resourceId: number) {
  try {
    await request.post(`/favorites/${resourceId}`)
    message.success('已取消收藏')
    await loadFavorites()
    if (profile.value) {
      profile.value.favoriteCount = (profile.value.favoriteCount || 0) - 1
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  }
}

function handleTabChange(tab: string | number) {
  const name = String(tab)
  activeTab.value = name
  router.replace({ query: { tab: name } })
  if (name === 'resources' && resources.value.length === 0) {
    loadResources()
  } else if (name === 'favorites' && favorites.value.length === 0) {
    loadFavorites()
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function handleEditResource(resource: Resource) {
  router.push(`${basePath()}/upload?id=${resource.id}`)
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

onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab as string
  }
  loadProfile()
})
</script>

<template>
  <div class="user-profile-page">
    <PageHeader title="个人中心" subtitle="查看资料、发布与收藏" />

    <NSpin :show="loading">
      <div v-if="profile" class="profile-hero glass-panel-strong">
        <div class="avatar-wrap">
          <UserAvatar
            :src="profile.avatar || userStore.user?.avatar"
            :name="profile.nickname || profile.username"
            :size="112"
          />
        </div>
        <div class="info-section">
          <h2 class="user-name">{{ profile.nickname || profile.username }}</h2>
          <div class="user-meta">
            <span>用户名：{{ profile.username }}</span>
            <span v-if="profile.gender">性别：{{ profile.gender }}</span>
            <span v-if="profile.email">邮箱：{{ profile.email }}</span>
            <span>注册时间：{{ formatDate(profile.createdAt) }}</span>
          </div>
          <div class="stats-section">
            <div class="stat-item surface-card">
              <div class="stat-icon">
                <NIcon :size="22" :component="DocumentTextOutline" />
              </div>
              <div>
                <div class="stat-value">{{ profile.resourceCount || 0 }}</div>
                <div class="stat-label">发布的资料</div>
              </div>
            </div>
            <div class="stat-item surface-card">
              <div class="stat-icon star">
                <NIcon :size="22" :component="StarOutline" />
              </div>
              <div>
                <div class="stat-value">{{ profile.favoriteCount || 0 }}</div>
                <div class="stat-label">收藏数</div>
              </div>
            </div>
            <div class="stat-item surface-card">
              <div class="stat-icon heart">
                <NIcon :size="22" :component="HeartOutline" />
              </div>
              <div>
                <div class="stat-value">{{ profile.likeCount || 0 }}</div>
                <div class="stat-label">点赞数</div>
              </div>
            </div>
          </div>
        </div>
        <div class="action-section">
          <NButton type="primary" @click="router.push(`${basePath()}/settings`)">
            <template #icon>
              <NIcon :component="CreateOutline" />
            </template>
            编辑资料
          </NButton>
          <NButton secondary @click="router.push(`${basePath()}/friends`)">我的好友</NButton>
          <NButton secondary @click="router.push(`${basePath()}/messages`)">我的消息</NButton>
        </div>
      </div>

      <div v-if="profile" class="content-panel glass-panel">
        <NTabs v-model:value="activeTab" type="segment" animated @update:value="handleTabChange">
          <NTabPane name="resources" tab="我发布的资料">
            <div class="tab-header">
              <NButton type="primary" @click="router.push(`${basePath()}/upload`)">上传资料</NButton>
            </div>
            <NSpin :show="resourcesLoading">
              <div v-if="resources.length === 0" class="empty-wrap">
                <NEmpty description="暂无发布的资料">
                  <template #extra>
                    <NButton type="primary" @click="router.push(`${basePath()}/upload`)">上传资料</NButton>
                  </template>
                </NEmpty>
              </div>
              <div v-else class="resources-list">
                <div v-for="resource in resources" :key="resource.id" class="resource-item surface-card">
                  <div class="item-icon">
                    <NIcon :size="28" :component="DocumentTextOutline" />
                  </div>
                  <div
                    class="resource-info"
                    @click="
                      router.push({
                        path: `${basePath()}/resources/${resource.id}`,
                        query: { from: 'profile' },
                      })
                    "
                  >
                    <div class="resource-title">
                      {{ resource.title }}
                      <NTag v-if="resource.visibility === 'HIDDEN'" size="small" type="warning" round>
                        已隐藏
                      </NTag>
                    </div>
                    <div class="resource-meta">
                      <span>浏览 {{ resource.viewCount || 0 }}</span>
                      <span>下载 {{ resource.downloadCount || 0 }}</span>
                      <span>点赞 {{ resource.likeCount || 0 }}</span>
                    </div>
                  </div>
                  <div class="resource-right">
                    <div class="resource-actions">
                      <NButton size="small" quaternary type="primary" @click.stop="handleEditResource(resource)">
                        <template #icon>
                          <NIcon :component="CreateOutline" />
                        </template>
                        修改
                      </NButton>
                      <NButton size="small" quaternary type="warning" @click.stop="handleToggleVisibility(resource)">
                        <template #icon>
                          <NIcon :component="resource.visibility === 'HIDDEN' ? EyeOutline : EyeOffOutline" />
                        </template>
                        {{ resource.visibility === 'HIDDEN' ? '恢复可见' : '隐藏' }}
                      </NButton>
                      <NButton size="small" quaternary type="error" @click.stop="handleDeleteResource(resource)">
                        <template #icon>
                          <NIcon :component="TrashOutline" />
                        </template>
                        删除
                      </NButton>
                    </div>
                    <div class="resource-time">{{ formatDate(resource.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </NSpin>
          </NTabPane>

          <NTabPane name="favorites" tab="我的收藏">
            <NSpin :show="favoritesLoading">
              <div v-if="favorites.length === 0" class="empty-wrap">
                <NEmpty description="暂无收藏的资料" />
              </div>
              <div v-else class="favorites-list">
                <div v-for="item in favorites" :key="item.id" class="favorite-item surface-card">
                  <div class="item-icon star">
                    <NIcon :size="28" :component="StarOutline" />
                  </div>
                  <div class="favorite-info">
                    <div class="favorite-header">
                      <div class="favorite-title" :class="{ muted: item.status === 'missing' }">
                        {{ item.title }}
                      </div>
                      <div class="favorite-actions">
                        <NButton
                          v-if="item.status !== 'missing'"
                          size="small"
                          type="primary"
                          @click.stop="router.push(`${basePath()}/resources/${item.resourceId}`)"
                        >
                          查看
                        </NButton>
                        <NButton size="small" secondary type="error" @click.stop="toggleFavorite(item.resourceId)">
                          取消收藏
                        </NButton>
                      </div>
                    </div>
                    <div class="favorite-meta">
                      <NTag v-if="item.visibility === 'HIDDEN'" size="small" type="warning" round>已隐藏</NTag>
                      <NTag v-else-if="item.status === 'missing'" size="small" round>不可访问</NTag>
                      <span v-if="item.resource">分类：{{ item.resource.categoryName || '-' }}</span>
                      <span v-if="item.resource">上传者：{{ item.resource.ownerName || '-' }}</span>
                      <span>收藏时间：{{ formatDate(item.createdAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </NSpin>
          </NTabPane>
        </NTabs>
      </div>
    </NSpin>
  </div>
</template>

<style scoped>
.user-profile-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: clamp(12px, 2vw, 28px);
}

.profile-hero {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 28px;
  margin-bottom: 20px;
}

.avatar-wrap {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}

.info-section {
  flex: 1;
  min-width: 0;
}

.user-name {
  margin: 0 0 12px;
  font-size: clamp(26px, 3vw, 34px);
  font-weight: 650;
  letter-spacing: -0.03em;
  color: var(--m-ink);
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-bottom: 20px;
  color: var(--m-ink-soft);
  font-size: 14px;
}

.stats-section {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  min-width: 140px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(145deg, var(--m-sage), var(--m-sage-deep));
}

.stat-icon.star {
  background: linear-gradient(145deg, var(--m-peach), #b8926e);
}

.stat-icon.heart {
  background: linear-gradient(145deg, var(--m-terracotta), #a86f65);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  color: var(--m-ink);
}

.stat-label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--m-ink-muted);
}

.action-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 132px;
}

.content-panel {
  padding: 20px 22px 24px;
}

.tab-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.empty-wrap {
  padding: 40px 16px;
}

.resources-list,
.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-item,
.favorite-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
}

.resource-item:hover,
.favorite-item:hover {
  border-color: rgba(122, 158, 142, 0.35);
  box-shadow: var(--m-shadow-soft);
  transform: translateY(-1px);
}

.item-icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  color: var(--m-sage-deep);
  background: var(--m-sage-wash);
}

.item-icon.star {
  color: #a67c52;
  background: rgba(196, 164, 132, 0.28);
}

.resource-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.resource-title,
.favorite-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--m-ink);
  margin-bottom: 4px;
}

.favorite-title.muted {
  color: var(--m-ink-muted);
}

.resource-meta,
.favorite-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  align-items: center;
  font-size: 13px;
  color: var(--m-ink-soft);
}

.resource-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.resource-actions,
.favorite-actions {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-wrap: wrap;
}

.resource-time {
  font-size: 12px;
  color: var(--m-ink-muted);
  white-space: nowrap;
  min-width: 140px;
  text-align: right;
}

.favorite-info {
  flex: 1;
  min-width: 0;
}

.favorite-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .profile-hero {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .user-meta,
  .stats-section {
    justify-content: center;
  }

  .action-section {
    width: 100%;
  }

  .resource-item,
  .favorite-item {
    flex-direction: column;
    align-items: stretch;
  }

  .resource-right {
    flex-direction: column;
    align-items: stretch;
  }

  .resource-time {
    text-align: left;
    min-width: 0;
  }
}
</style>
