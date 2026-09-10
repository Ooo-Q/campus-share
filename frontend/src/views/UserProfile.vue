<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Document, Star, View, User, StarFilled, Delete, Hide } from '@element-plus/icons-vue'
import { getUserProfile, type UserProfile } from '../api/friend'
import { useUserStore } from '../stores/user'
import request from '../api/request'
import { updateResourceVisibility } from '../api/resource'
import type { Resource } from '../api/resource'
import { getAvatarUrl } from '../utils/resource'
import PageHeader from '../components/PageHeader.vue'

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

async function loadProfile() {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  loading.value = true
  try {
    const res = await getUserProfile(userStore.user.userId)
    profile.value = res.data
    if (activeTab.value === 'resources') {
      await loadResources()
    } else if (activeTab.value === 'favorites') {
      await loadFavorites()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
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
        } catch (e) {
          return {
            ...f,
            status: 'missing' as const,
            title: '关联资料已删除或不可访问',
          }
        }
      })
    )
    favorites.value = results
      .map((r) => (r.status === 'fulfilled' ? r.value : null))
      .filter(Boolean) as any[]
  } catch (e) {
    ElMessage.error('加载收藏失败')
  } finally {
    favoritesLoading.value = false
  }
}

async function toggleFavorite(resourceId: number) {
  try {
    await request.post(`/favorites/${resourceId}`)
    ElMessage.success('已取消收藏')
    await loadFavorites()
    if (profile.value) {
      profile.value.favoriteCount = (profile.value.favoriteCount || 0) - 1
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab
  router.replace({ query: { tab } })
  if (tab === 'resources' && resources.value.length === 0) {
    loadResources()
  } else if (tab === 'favorites' && favorites.value.length === 0) {
    loadFavorites()
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function handleEditResource(resource: Resource) {
  const isAdmin = route.path.startsWith('/admin')
  const basePath = isAdmin ? '/admin' : '/student'
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

onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab as string
  }
  loadProfile()
})
</script>

<template>
  <div class="user-profile-page" v-loading="loading">
    <PageHeader title="个人中心" />
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
            <span class="meta-item" v-if="profile.email">邮箱：{{ profile.email }}</span>
            <span class="meta-item">注册时间：{{ formatDate(profile.createdAt) }}</span>
          </div>
          <div class="stats-section">
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ profile.resourceCount || 0 }}</div>
                <div class="stat-label">发布的资料</div>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><Star /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ profile.favoriteCount || 0 }}</div>
                <div class="stat-label">收藏数</div>
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon><View /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ profile.likeCount || 0 }}</div>
                <div class="stat-label">点赞数</div>
              </div>
            </div>
          </div>
        </div>
        <div class="action-section">
          <el-button type="primary" @click="router.push(route.path.startsWith('/admin') ? '/admin/settings' : '/student/settings')">
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
          <el-button @click="router.push(route.path.startsWith('/admin') ? '/admin/friends' : '/student/friends')">我的好友</el-button>
          <el-button @click="router.push(route.path.startsWith('/admin') ? '/admin/messages' : '/student/messages')">我的消息</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="content-card" v-if="profile">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我发布的资料" name="resources">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><Document /></el-icon>
              我发布的资料
            </span>
          </template>
          <div class="tab-header">
            <el-button type="primary" @click="router.push(route.path.startsWith('/admin') ? '/admin/upload' : '/student/upload')">上传资料</el-button>
          </div>
          <div v-loading="resourcesLoading">
            <div v-if="resources.length === 0" class="empty-resources">
              <el-empty description="暂无发布的资料">
                <el-button type="primary" @click="router.push(route.path.startsWith('/admin') ? '/admin/upload' : '/student/upload')">上传资料</el-button>
              </el-empty>
            </div>
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
                <div class="resource-right">
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
        </el-tab-pane>
        <el-tab-pane label="我的收藏" name="favorites">
          <template #label>
            <span style="display: flex; align-items: center; gap: 8px">
              <el-icon><StarFilled /></el-icon>
              我的收藏
            </span>
          </template>
          <div v-loading="favoritesLoading">
            <div v-if="favorites.length === 0" class="empty-resources">
              <el-empty description="暂无收藏的资料" />
            </div>
            <div v-else class="favorites-list">
              <div v-for="item in favorites" :key="item.id" class="favorite-item">
                <el-icon class="favorite-icon"><Document /></el-icon>
                <div class="favorite-info">
                  <div class="favorite-header">
                    <div class="favorite-title" :class="{ muted: item.status === 'missing' }">
                      {{ item.title }}
                    </div>
                    <div class="favorite-actions">
                      <el-button
                        v-if="item.status !== 'missing'"
                        size="small"
                        type="primary"
                        @click.stop="router.push(route.path.startsWith('/admin') ? `/admin/resources/${item.resourceId}` : `/student/resources/${item.resourceId}`)"
                      >
                        查看
                      </el-button>
                      <el-button size="small" type="danger" plain @click.stop="toggleFavorite(item.resourceId)">
                        取消收藏
                      </el-button>
                    </div>
                  </div>
                  <div class="favorite-meta">
                    <el-tag v-if="item.visibility === 'HIDDEN'" type="warning" size="small">已隐藏</el-tag>
                    <el-tag v-else-if="item.status === 'missing'" type="info" size="small">不可访问</el-tag>
                    <span v-if="item.resource">分类：{{ item.resource.categoryName || '-' }}</span>
                    <span v-if="item.resource">上传者：{{ item.resource.ownerName || '-' }}</span>
                    <span>收藏时间：{{ formatDate(item.createdAt) }}</span>
                  </div>
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
.user-profile-page {
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
  font-size: 32px;
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
  font-size: 14px;
}

.stats-section {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
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

.content-card {
  margin-bottom: 24px;
}

.tab-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.empty-resources {
  padding: 40px;
  text-align: center;
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
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.resource-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.resource-icon {
  font-size: 32px;
  color: #667eea;
}

.resource-info {
  flex: 1;
  cursor: pointer;
}

.resource-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.resource-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
}

.resource-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
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
  color: #409eff !important;
}

.resource-actions :deep(.el-button:first-child:hover) {
  color: #66b1ff !important;
}

.resource-actions :deep(.el-button:first-child .el-icon) {
  color: #409eff !important;
}

.resource-actions :deep(.el-button:nth-child(2)) {
  min-width: 90px;
  width: 90px;
}

.resource-time {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
  min-width: 160px;
  text-align: right;
}

.favorites-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.favorite-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.favorite-icon {
  font-size: 32px;
  color: #fbbf24;
  flex-shrink: 0;
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

.favorite-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  min-width: 0;
}

.favorite-title.muted {
  color: #9ca3af;
}

.favorite-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.favorite-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  font-size: 13px;
  color: #6b7280;
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

