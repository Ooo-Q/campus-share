<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, Download, Star, StarFilled, ChatDotRound, User, Clock, Folder, Document } from '@element-plus/icons-vue'
import { fetchResourceDetail, likeResource, getLikeStatus, fetchComments, createComment, deleteComment, type Resource, type Comment } from '../api/resource'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { downloadResource, getAvatarUrl } from '../utils/resource'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const resource = ref<Resource | null>(null)
const loading = ref(false)
const isFavorited = ref(false)
const isLiked = ref(false)
const reportDialogVisible = ref(false)
const reportReason = ref('')
const reporting = ref(false)
const comments = ref<Comment[]>([])
const commentLoading = ref(false)
const commentSubmitting = ref(false)
const commentContent = ref('')
const commentAnonymous = ref(false)
const replyingTo = ref<Comment | null>(null)
const commentPage = ref({ page: 1, size: 10, total: 0 })
const defaultAnonAvatar =
  "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64'><path fill='%23000' d='M7 57c0-14 7-26 25-26s25 12 25 26H7Z'/><circle cx='25' cy='29' r='6' fill='%23fff'/><circle cx='39' cy='29' r='6' fill='%23fff'/><circle cx='32' cy='44' r='4' fill='%23000'/><path d='M7 57c0-14 7-26 25-26s25 12 25 26' stroke='%23000' fill='none' stroke-width='2'/></svg>"

async function loadDetail() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const res = await fetchResourceDetail(id)
    resource.value = res.data
    if (userStore.token) {
      await checkFavorite()
      await checkLike()
    }
    await loadComments(1)
  } catch (e) {
    ElMessage.error('加载详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

async function checkFavorite() {
  if (!resource.value || !userStore.token) return
  try {
    const res = await request.get<{ success: boolean; data: Array<{ resourceId: number }> }>('/favorites')
    const favorites = res.data || []
    isFavorited.value = favorites.some(fav => fav.resourceId === resource.value!.id)
  } catch (e) {
  }
}

async function checkLike() {
  if (!resource.value || !userStore.token) return
  try {
    const res = await getLikeStatus(resource.value.id)
    isLiked.value = res.data || false
  } catch (e) {
  }
}

async function handleLike() {
  if (!resource.value) return
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await likeResource(resource.value.id)
    const updatedResource = res.data as Resource
    if (updatedResource && resource.value) {
      resource.value.likeCount = updatedResource.likeCount || 0
      resource.value.favoriteCount = updatedResource.favoriteCount || 0
      let isLikedNow = false
      try {
        isLikedNow = (await getLikeStatus(resource.value.id)).data
      } catch {
        isLikedNow = false
      }
      isLiked.value = isLikedNow
      ElMessage.success(isLikedNow ? '点赞成功' : '取消点赞')
    } else {
      ElMessage.error('操作失败：未返回资源数据')
    }
  } catch (e: any) {
    console.error('点赞操作错误:', e)
    ElMessage.error(e.response?.data?.message || e.message || '操作失败')
  }
}

async function handleFavorite() {
  if (!resource.value) return
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await request.post<{ success: boolean; data: Resource; message?: string }>(`/favorites/${resource.value.id}`)
    const updatedResource = res.data as Resource
    if (updatedResource && resource.value) {
      resource.value.favoriteCount = updatedResource.favoriteCount || 0
      resource.value.likeCount = updatedResource.likeCount || 0
      const wasFavorited = isFavorited.value
      isFavorited.value = !wasFavorited
      ElMessage.success(wasFavorited ? '已取消收藏' : '收藏成功')
    } else {
      ElMessage.error('操作失败：未返回资源数据')
    }
  } catch (e: any) {
    console.error('收藏操作错误:', e)
    ElMessage.error(e.response?.data?.message || e.message || '操作失败')
  }
}

function handleDownload() {
  if (!resource.value) return
  downloadResource(resource.value, {
    updateCount: (count) => {
      if (resource.value) {
        resource.value.downloadCount = count
      }
    },
  })
}

function handleReport() {
  if (!resource.value) return
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  reportReason.value = ''
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!resource.value) return
  if (!reportReason.value.trim()) {
    ElMessage.warning('请输入举报原因')
    return
  }
  reporting.value = true
  try {
    await request.post('/reports', {
      resourceId: resource.value.id,
      reason: reportReason.value,
    })
    ElMessage.success('举报提交成功，我们会尽快处理')
    reportDialogVisible.value = false
    reportReason.value = ''
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '举报失败')
  } finally {
    reporting.value = false
  }
}

const flattenedComments = computed(() => {
  const allComments: Comment[] = []

  function flatten(commentList: Comment[]) {
    for (const comment of commentList) {
      allComments.push(comment)
      if (comment.children && comment.children.length > 0) {
        flatten(comment.children)
      }
    }
  }

  flatten(comments.value)

  const rootMap = new Map<number, Comment>()
  for (const comment of allComments) {
    const rootId = comment.rootId || comment.id
    if (!rootMap.has(rootId)) {
      const root = allComments.find(c => c.id === rootId && (c.rootId === null || c.rootId === c.id))
      if (root) {
        rootMap.set(rootId, root)
      }
    }
  }

  const groups = new Map<number, Comment[]>()
  for (const comment of allComments) {
    const rootId = comment.rootId || comment.id
    if (!groups.has(rootId)) {
      groups.set(rootId, [])
    }
    groups.get(rootId)!.push(comment)
  }

  for (const [, group] of groups.entries()) {
    group.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
  }

  const sortedGroups = Array.from(groups.entries()).sort(([rootIdA], [rootIdB]) => {
    const rootA = rootMap.get(rootIdA)
    const rootB = rootMap.get(rootIdB)
    if (rootA && rootB) {
      return new Date(rootA.createdAt).getTime() - new Date(rootB.createdAt).getTime()
    }
    return rootIdA - rootIdB
  })

  const result: Comment[] = []
  for (const [, group] of sortedGroups) {
    result.push(...group)
  }

  return result
})

async function loadComments(page: number) {
  if (!resource.value) return
  commentLoading.value = true
  try {
    const res = await fetchComments(resource.value.id, { page, size: commentPage.value.size })
    comments.value = res.data.records || []
    commentPage.value = {
      page: res.data.page || page,
      size: res.data.size || commentPage.value.size,
      total: res.data.total || 0,
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载评论失败')
  } finally {
    commentLoading.value = false
  }
}

function startReply(comment: Comment) {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  replyingTo.value = comment
  commentContent.value = ''
}

function cancelReply() {
  replyingTo.value = null
  commentContent.value = ''
}

async function handleSubmitComment() {
  if (!resource.value) return
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await createComment(resource.value.id, {
      content,
      parentId: replyingTo.value?.id,
      anonymous: commentAnonymous.value,
    })
    ElMessage.success('发布成功')
    commentContent.value = ''
    replyingTo.value = null
    await loadComments(1)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

function canDeleteComment(comment: Comment) {
  if (!userStore.user) return false
  if (userStore.user.role === 'ADMIN') return true
  if (resource.value && resource.value.ownerId === userStore.user.userId) return true
  return comment.userId === userStore.user.userId
}

async function handleDeleteComment(comment: Comment) {
  if (!resource.value) return
  if (!canDeleteComment(comment)) {
    ElMessage.warning('无权删除')
    return
  }
  try {
    await deleteComment(resource.value.id, comment.id)
    ElMessage.success('已删除')
    await loadComments(commentPage.value.page)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function getFileType(fileUrl: string) {
  if (!fileUrl) return '未知'
  const ext = fileUrl.split('.').pop()?.toLowerCase() || ''
  const typeMap: Record<string, string> = {
    pdf: 'PDF文档',
    doc: 'Word文档',
    docx: 'Word文档',
    xls: 'Excel表格',
    xlsx: 'Excel表格',
    ppt: 'PPT演示',
    pptx: 'PPT演示',
    txt: '文本文件',
    zip: '压缩文件',
    rar: '压缩文件',
    jpg: '图片文件',
    jpeg: '图片文件',
    png: '图片文件',
    gif: '图片文件',
  }
  return typeMap[ext] || '文件'
}

function getFileSize(_fileUrl: string) {
  return '未知大小'
}

onMounted(loadDetail)
</script>

<template>
  <div class="resource-detail-page" v-loading="loading">
    <div v-if="resource" class="detail-container">
      <div class="header-section">
        <div class="title-wrapper">
          <h1 class="main-title">{{ resource.title }}</h1>
          <div class="meta-tags">
            <el-tag v-if="resource.categoryName" type="primary" size="large" effect="plain">
              <el-icon><Folder /></el-icon>
              {{ resource.categoryName }}
            </el-tag>
            <el-tag type="info" size="large" effect="plain">
              <el-icon><Document /></el-icon>
              {{ getFileType(resource.fileUrl) }}
            </el-tag>
          </div>
        </div>
        <div class="action-buttons">
          <div class="primary-actions">
            <el-button
              v-if="resource.allowDownload"
              class="primary-btn download-btn"
              size="large"
              @click="handleDownload"
            >
              <el-icon class="btn-icon"><Download /></el-icon>
              下载
            </el-button>
            <el-button
              v-if="userStore.token"
              class="primary-btn report-btn"
              size="large"
              @click="handleReport"
            >
              <el-icon class="btn-icon"><ChatDotRound /></el-icon>
              举报
            </el-button>
          </div>
          <div class="secondary-actions" v-if="userStore.token">
            <el-button
              class="icon-btn favorite-btn"
              :class="{ active: isFavorited }"
              circle
              @click="handleFavorite"
            >
              <el-icon class="btn-icon">
                <component :is="isFavorited ? StarFilled : Star" />
              </el-icon>
            </el-button>
            <el-button
              class="icon-btn like-btn"
              :class="{ active: isLiked }"
              circle
              @click="handleLike"
            >
              <span class="btn-icon like-icon">{{ isLiked ? '❤' : '♡' }}</span>
            </el-button>
          </div>
        </div>
      </div>

      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon-wrapper view">
            <el-icon><View /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ resource.viewCount || 0 }}</div>
            <div class="stat-label">浏览</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrapper download">
            <el-icon><Download /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ resource.downloadCount || 0 }}</div>
            <div class="stat-label">下载</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrapper favorite">
            <el-icon><Star /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ resource.favoriteCount || 0 }}</div>
            <div class="stat-label">收藏</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrapper like">
            <span class="like-heart-icon">❤</span>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ resource.likeCount || 0 }}</div>
            <div class="stat-label">点赞</div>
          </div>
        </div>
      </div>

      <div class="info-cards">
        <div class="info-card">
          <el-icon class="info-icon"><User /></el-icon>
          <div class="info-content">
            <div class="info-label">上传者</div>
            <div class="info-value">
              <el-link type="primary" :underline="false" @click="router.push(route.path.startsWith('/admin') ? `/admin/user/${resource.ownerId}` : `/student/user/${resource.ownerId}`)" style="cursor: pointer">
                {{ resource.ownerName }}
              </el-link>
            </div>
          </div>
        </div>
        <div class="info-card">
          <el-icon class="info-icon"><Clock /></el-icon>
          <div class="info-content">
            <div class="info-label">上传时间</div>
            <div class="info-value">{{ formatDate(resource.createdAt) }}</div>
          </div>
        </div>
        <div class="info-card">
          <el-icon class="info-icon"><Document /></el-icon>
          <div class="info-content">
            <div class="info-label">文件类型</div>
            <div class="info-value">{{ getFileType(resource.fileUrl) }}</div>
          </div>
        </div>
      </div>

      <div class="content-card">
        <h2 class="card-title">资料说明</h2>
        <div class="card-content">
          <p v-if="resource.description" class="description-text">{{ resource.description }}</p>
          <p v-else class="empty-text">暂无说明</p>
        </div>
      </div>

      <div class="content-card" v-if="resource.fileUrl">
        <h2 class="card-title">文件</h2>
        <div class="file-card">
          <div class="file-header">
            <div class="file-icon-wrapper">
              <el-icon class="file-icon"><Document /></el-icon>
            </div>
            <div class="file-info">
              <div class="file-name">{{ resource.fileUrl.split('/').pop() }}</div>
              <div class="file-meta">{{ getFileType(resource.fileUrl) }} · {{ getFileSize(resource.fileUrl) }}</div>
            </div>
          </div>
          <el-button
            type="primary"
            :icon="Download"
            @click="handleDownload"
            :disabled="!resource.allowDownload"
            class="download-btn"
          >
            下载文件
          </el-button>
        </div>
      </div>

      <div class="content-card comment-card">
        <h2 class="card-title">评论 <span class="comment-count">({{ commentPage.total }})</span></h2>

        <div class="comment-input-wrapper">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="留下你的想法..."
            class="comment-input"
          />
          <div class="comment-toolbar">
            <div class="toolbar-left">
              <el-checkbox v-model="commentAnonymous">匿名评论</el-checkbox>
              <div class="reply-hint" v-if="replyingTo">
                回复 <span class="reply-name">{{ replyingTo.displayName }}</span>
                <el-button link type="primary" size="small" @click="cancelReply">取消</el-button>
              </div>
            </div>
            <el-button 
              type="primary" 
              :loading="commentSubmitting" 
              @click="handleSubmitComment"
              :disabled="!commentContent.trim()"
            >
              发布评论
            </el-button>
          </div>
        </div>

        <div class="comment-list" v-loading="commentLoading">
          <div v-if="flattenedComments.length === 0" class="empty-state">
            <el-empty description="暂无评论，快来发表第一条评论吧" :image-size="120" />
          </div>
          <div v-else>
            <div v-for="item in flattenedComments" :key="item.id" class="comment-item" :class="{ 'is-reply': item.parentId !== null }">
              <img class="comment-avatar" :src="getAvatarUrl(item.avatar) || defaultAnonAvatar" alt="avatar" />
              <div class="comment-content">
                <div class="comment-header">
                  <span class="comment-author" v-if="!item.replyToName">{{ item.displayName }}</span>
                  <span class="comment-reply-hint" v-else>{{ item.displayName }}回复{{ item.replyToName }}</span>
                  <span class="comment-time">{{ formatDate(item.createdAt) }}</span>
                </div>
                <div class="comment-text">{{ item.content }}</div>
                <div class="comment-actions">
                  <el-button text size="small" @click="startReply(item)">回复</el-button>
                  <el-button v-if="canDeleteComment(item)" text size="small" type="danger" @click="handleDeleteComment(item)">删除</el-button>
                </div>
              </div>
            </div>

            <div class="comment-pagination" v-if="commentPage.total > commentPage.size">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="commentPage.total"
                :page-size="commentPage.size"
                :current-page="commentPage.page"
                @current-change="loadComments"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="reportDialogVisible" title="举报资料" width="500px">
      <el-form>
        <el-form-item label="资料标题">
          <el-input :value="resource?.title" disabled />
        </el-form-item>
        <el-form-item label="举报原因" required>
          <el-input
            v-model="reportReason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明举报原因，例如：内容违规、侵权、虚假信息等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reporting" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resource-detail-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.detail-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.header-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px;
  color: white;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.3);
}

.title-wrapper {
  margin-bottom: 24px;
}

.main-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 16px 0;
  line-height: 1.3;
  color: white;
}

.meta-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-tags :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.primary-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.secondary-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-left: 16px;
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}

.primary-btn {
  min-width: 120px;
  height: 44px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 15px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 20px;
  line-height: 1;
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.primary-btn.download-btn {
  background: white;
  color: #667eea;
}

.primary-btn.download-btn:hover {
  background: #f8f9ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.primary-btn.report-btn {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
}

.primary-btn.report-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
}

.icon-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.2);
}

.icon-btn.active {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
}

.icon-btn.favorite-btn {
  color: #ffd700;
}

.icon-btn.favorite-btn.active {
  background: rgba(255, 215, 0, 0.2);
  border-color: #ffd700;
  box-shadow: 0 0 0 3px rgba(255, 215, 0, 0.15);
}

.icon-btn.like-btn {
  color: #ff6b9d;
}

.icon-btn.like-btn.active {
  background: rgba(255, 107, 157, 0.2);
  border-color: #ff6b9d;
  box-shadow: 0 0 0 3px rgba(255, 107, 157, 0.15);
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin: 0;
}

.icon-btn .btn-icon {
  font-size: 20px;
}

.like-icon {
  font-size: 20px;
  line-height: 1;
  display: inline-block;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon-wrapper.view {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.stat-icon-wrapper.download {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.stat-icon-wrapper.favorite {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
  color: white;
}

.stat-icon-wrapper.like {
  background: linear-gradient(135deg, #fb7185 0%, #f43f5e 100%);
  color: white;
}

.like-heart-icon {
  font-size: 28px;
  line-height: 1;
  display: inline-block;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
}

.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.info-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.info-icon {
  font-size: 32px;
  color: #667eea;
  flex-shrink: 0;
}

.info-content {
  flex: 1;
}

.info-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.content-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #f3f4f6;
}

.comment-count {
  font-size: 16px;
  font-weight: 400;
  color: #6b7280;
}

.card-content {
  line-height: 1.8;
}

.description-text {
  color: #374151;
  white-space: pre-wrap;
  margin: 0;
}

.empty-text {
  color: #9ca3af;
  font-style: italic;
  margin: 0;
}

.file-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.file-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.file-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-icon {
  font-size: 32px;
  color: white;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  word-break: break-all;
}

.file-meta {
  font-size: 14px;
  color: #6b7280;
}

.download-btn {
  flex-shrink: 0;
}

.comment-card {
  margin-top: 0;
}

.comment-input-wrapper {
  margin-bottom: 32px;
}

.comment-input :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  font-size: 14px;
  line-height: 1.6;
}

.comment-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.reply-hint {
  color: #6b7280;
  font-size: 14px;
}

.reply-name {
  color: #667eea;
  font-weight: 600;
}

.comment-list {
  border-top: 1px solid #f3f4f6;
  padding-top: 24px;
}

.empty-state {
  padding: 40px 0;
}

.comment-item {
  display: flex;
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid #f3f4f6;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-item.is-reply {
  padding-left: 20px;
  background-color: #fafafa;
  border-left: 3px solid #e5e7eb;
  margin-top: 8px;
}

.comment-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: #f3f4f6;
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.comment-author {
  font-weight: 600;
  color: #1f2937;
  font-size: 15px;
}

.comment-reply-hint {
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.comment-time {
  color: #9ca3af;
  font-size: 13px;
  margin-left: auto;
}

.comment-text {
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  margin-bottom: 12px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0 0 0;
}

@media (max-width: 768px) {
  .resource-detail-page {
    padding: 16px;
  }

  .header-section {
    padding: 24px 20px;
  }

  .main-title {
    font-size: 24px;
  }

  .action-buttons {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .primary-actions {
    width: 100%;
    flex-direction: column;
  }

  .primary-actions .primary-btn {
    width: 100%;
    min-width: 0;
  }

  .secondary-actions {
    width: 100%;
    justify-content: center;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.2);
    padding-top: 12px;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .info-cards {
    grid-template-columns: 1fr;
  }

  .file-card {
    flex-direction: column;
    align-items: stretch;
  }

  .download-btn {
    width: 100%;
  }
}
</style>
