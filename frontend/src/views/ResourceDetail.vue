<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NTag,
  NIcon,
  NInput,
  NCheckbox,
  NEmpty,
  NSpin,
  NPagination,
  NModal,
  NForm,
  NFormItem,
  NSpace,
} from 'naive-ui'
import {
  DownloadOutline,
  StarOutline,
  Star,
  DocumentOutline,
  FolderOutline,
  HeartOutline,
  Heart,
  FlagOutline,
} from '@vicons/ionicons5'
import {
  fetchResourceDetail,
  likeResource,
  getLikeStatus,
  fetchComments,
  createComment,
  deleteComment,
  type Resource,
  type Comment,
} from '../api/resource'
import request from '../api/request'
import { useUserStore } from '../stores/user'
import { downloadResource, getAvatarUrl } from '../utils/resource'
import { message } from '../utils/feedback'
import PageHeader from '../components/PageHeader.vue'

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
  } catch {
    message.error('加载详情失败')
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
    isFavorited.value = favorites.some((fav) => fav.resourceId === resource.value!.id)
  } catch {
    /* ignore */
  }
}

async function checkLike() {
  if (!resource.value || !userStore.token) return
  try {
    const res = await getLikeStatus(resource.value.id)
    isLiked.value = res.data || false
  } catch {
    /* ignore */
  }
}

async function handleLike() {
  if (!resource.value) return
  if (!userStore.token) {
    message.warning('请先登录')
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
      message.success(isLikedNow ? '点赞成功' : '取消点赞')
    } else {
      message.error('操作失败：未返回资源数据')
    }
  } catch (e: any) {
    console.error('点赞操作错误:', e)
    message.error(e.response?.data?.message || e.message || '操作失败')
  }
}

async function handleFavorite() {
  if (!resource.value) return
  if (!userStore.token) {
    message.warning('请先登录')
    return
  }
  try {
    const res = await request.post<{ success: boolean; data: Resource; message?: string }>(
      `/favorites/${resource.value.id}`,
    )
    const updatedResource = res.data as Resource
    if (updatedResource && resource.value) {
      resource.value.favoriteCount = updatedResource.favoriteCount || 0
      resource.value.likeCount = updatedResource.likeCount || 0
      const wasFavorited = isFavorited.value
      isFavorited.value = !wasFavorited
      message.success(wasFavorited ? '已取消收藏' : '收藏成功')
    } else {
      message.error('操作失败：未返回资源数据')
    }
  } catch (e: any) {
    console.error('收藏操作错误:', e)
    message.error(e.response?.data?.message || e.message || '操作失败')
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
    message.warning('请先登录')
    return
  }
  reportReason.value = ''
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!resource.value) return
  if (!reportReason.value.trim()) {
    message.warning('请输入举报原因')
    return
  }
  reporting.value = true
  try {
    await request.post('/reports', {
      resourceId: resource.value.id,
      reason: reportReason.value,
    })
    message.success('举报提交成功，我们会尽快处理')
    reportDialogVisible.value = false
    reportReason.value = ''
  } catch (e: any) {
    message.error(e.response?.data?.message || '举报失败')
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
      const root = allComments.find((c) => c.id === rootId && (c.rootId === null || c.rootId === c.id))
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
    message.error(e.response?.data?.message || '加载评论失败')
  } finally {
    commentLoading.value = false
  }
}

function startReply(comment: Comment) {
  if (!userStore.token) {
    message.warning('请先登录')
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
    message.warning('请先登录')
    return
  }
  const content = commentContent.value.trim()
  if (!content) {
    message.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await createComment(resource.value.id, {
      content,
      parentId: replyingTo.value?.id,
      anonymous: commentAnonymous.value,
    })
    message.success('发布成功')
    commentContent.value = ''
    replyingTo.value = null
    await loadComments(1)
  } catch (e: any) {
    message.error(e.response?.data?.message || '发布失败')
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
    message.warning('无权删除')
    return
  }
  try {
    await deleteComment(resource.value.id, comment.id)
    message.success('已删除')
    await loadComments(commentPage.value.page)
  } catch (e: any) {
    message.error(e.response?.data?.message || '删除失败')
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

function goOwner() {
  if (!resource.value) return
  const path = route.path.startsWith('/admin')
    ? `/admin/user/${resource.value.ownerId}`
    : `/student/user/${resource.value.ownerId}`
  router.push(path)
}

onMounted(loadDetail)
</script>

<template>
  <div class="page">
    <PageHeader title="资料详情" />

    <NSpin :show="loading">
      <template v-if="resource">
        <div class="sheet glass-panel-strong">
          <div class="sheet-top">
            <div class="hero-tags">
              <NTag v-if="resource.categoryName" :bordered="false" type="success" size="small">
                <template #icon><NIcon :component="FolderOutline" /></template>
                {{ resource.categoryName }}
              </NTag>
              <NTag :bordered="false" size="small">
                <template #icon><NIcon :component="DocumentOutline" /></template>
                {{ getFileType(resource.fileUrl) }}
              </NTag>
            </div>
            <button
              v-if="userStore.token"
              type="button"
              class="report-link"
              @click="handleReport"
            >
              <NIcon :component="FlagOutline" :size="14" />
              举报
            </button>
          </div>

          <h2 class="hero-title">{{ resource.title }}</h2>

          <div class="meta-line muted">
            <button type="button" class="owner-link" @click="goOwner">{{ resource.ownerName }}</button>
            <span>·</span>
            <span>{{ formatDate(resource.createdAt) }}</span>
            <span>·</span>
            <span>{{ resource.viewCount || 0 }} 浏览</span>
            <span v-if="resource.allowDownload">· {{ resource.downloadCount || 0 }} 下载</span>
          </div>

          <p v-if="resource.description" class="desc">{{ resource.description }}</p>
          <p v-else class="muted desc-empty">暂无资料说明</p>

          <div class="action-bar">
            <NButton
              v-if="resource.allowDownload"
              type="primary"
              size="large"
              @click="handleDownload"
            >
              <template #icon><NIcon :component="DownloadOutline" /></template>
              下载
            </NButton>
            <NButton
              v-if="userStore.token"
              :type="isFavorited ? 'warning' : 'default'"
              :secondary="!isFavorited"
              size="large"
              @click="handleFavorite"
            >
              <template #icon>
                <NIcon :component="isFavorited ? Star : StarOutline" />
              </template>
              {{ isFavorited ? '已收藏' : '收藏' }}
              <span class="btn-count">{{ resource.favoriteCount || 0 }}</span>
            </NButton>
            <NButton
              v-if="userStore.token"
              :type="isLiked ? 'error' : 'default'"
              :secondary="!isLiked"
              size="large"
              @click="handleLike"
            >
              <template #icon>
                <NIcon :component="isLiked ? Heart : HeartOutline" />
              </template>
              {{ isLiked ? '已赞' : '点赞' }}
              <span class="btn-count">{{ resource.likeCount || 0 }}</span>
            </NButton>
          </div>

          <div v-if="resource.fileUrl" class="file-chip">
            <NIcon :size="18" :component="DocumentOutline" />
            <span class="file-name">{{ resource.fileUrl.split('/').pop() }}</span>
            <span class="muted">{{ getFileType(resource.fileUrl) }}</span>
          </div>
        </div>

        <div class="glass-panel section">
          <h3 class="section-title">
            评论
            <span class="muted count">({{ commentPage.total }})</span>
          </h3>

          <div class="comment-composer">
            <NInput
              v-model:value="commentContent"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-count
              placeholder="留下你的想法..."
            />
            <div class="composer-bar">
              <div class="composer-left">
                <NCheckbox v-model:checked="commentAnonymous">匿名评论</NCheckbox>
                <div v-if="replyingTo" class="reply-hint muted">
                  回复 <span class="reply-name">{{ replyingTo.displayName }}</span>
                  <NButton text type="primary" size="tiny" @click="cancelReply">取消</NButton>
                </div>
              </div>
              <NButton
                type="primary"
                :loading="commentSubmitting"
                :disabled="!commentContent.trim()"
                @click="handleSubmitComment"
              >
                发布评论
              </NButton>
            </div>
          </div>

          <NSpin :show="commentLoading">
            <NEmpty
              v-if="flattenedComments.length === 0"
              description="暂无评论"
            />
            <div v-else class="comment-list">
              <div
                v-for="item in flattenedComments"
                :key="item.id"
                class="comment-item"
                :class="{ 'is-reply': item.parentId !== null }"
              >
                <img
                  class="comment-avatar"
                  :src="getAvatarUrl(item.avatar) || defaultAnonAvatar"
                  alt="avatar"
                />
                <div class="comment-body">
                  <div class="comment-head">
                    <span v-if="!item.replyToName" class="author">{{ item.displayName }}</span>
                    <span v-else class="author">{{ item.displayName }}回复{{ item.replyToName }}</span>
                    <span class="muted time">{{ formatDate(item.createdAt) }}</span>
                  </div>
                  <div class="comment-text">{{ item.content }}</div>
                  <div class="comment-actions">
                    <NButton text size="tiny" @click="startReply(item)">回复</NButton>
                    <NButton
                      v-if="canDeleteComment(item)"
                      text
                      size="tiny"
                      type="error"
                      @click="handleDeleteComment(item)"
                    >
                      删除
                    </NButton>
                  </div>
                </div>
              </div>

              <div v-if="commentPage.total > commentPage.size" class="comment-pagination">
                <NPagination
                  :page="commentPage.page"
                  :page-size="commentPage.size"
                  :item-count="commentPage.total"
                  @update:page="loadComments"
                />
              </div>
            </div>
          </NSpin>
        </div>
      </template>
    </NSpin>

    <NModal
      v-model:show="reportDialogVisible"
      preset="card"
      title="举报资料"
      style="width: min(500px, 92vw)"
      :bordered="false"
    >
      <NForm label-placement="top">
        <NFormItem label="资料标题">
          <NInput :value="resource?.title" disabled />
        </NFormItem>
        <NFormItem label="举报原因" required>
          <NInput
            v-model:value="reportReason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明举报原因"
            maxlength="500"
            show-count
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="reportDialogVisible = false">取消</NButton>
          <NButton type="primary" :loading="reporting" @click="submitReport">提交举报</NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page {
  max-width: 860px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sheet {
  padding: 26px 28px;
}

.sheet-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.report-link {
  border: none;
  background: transparent;
  color: var(--m-ink-muted);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 999px;
}

.report-link:hover {
  color: var(--m-terracotta);
  background: rgba(196, 137, 126, 0.12);
}

.hero-title {
  margin: 0 0 10px;
  font-size: clamp(24px, 3vw, 32px);
  font-weight: 650;
  letter-spacing: -0.03em;
  color: var(--m-ink);
  line-height: 1.25;
}

.meta-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.owner-link {
  border: none;
  background: none;
  padding: 0;
  color: var(--m-sage-deep);
  font-weight: 600;
  cursor: pointer;
  font: inherit;
}

.owner-link:hover {
  text-decoration: underline;
}

.desc {
  margin: 0 0 20px;
  white-space: pre-wrap;
  line-height: 1.7;
  color: var(--m-ink);
}

.desc-empty {
  margin: 0 0 20px;
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}

.btn-count {
  margin-left: 4px;
  opacity: 0.75;
  font-weight: 500;
}

.file-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.45);
  border: 1px solid var(--m-stroke);
  color: var(--m-sage-deep);
  font-size: 13px;
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--m-ink);
  font-weight: 500;
}

.section {
  padding: 22px 24px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 600;
}

.count {
  font-weight: 400;
  font-size: 14px;
}

.comment-composer {
  margin-bottom: 18px;
}

.composer-bar {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.composer-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.reply-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.reply-name {
  color: var(--m-sage-deep);
  font-weight: 600;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-item.is-reply {
  margin-left: 36px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
  background: rgba(122, 158, 142, 0.15);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 4px;
}

.author {
  font-weight: 600;
  font-size: 14px;
}

.time {
  font-size: 12px;
}

.comment-text {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.comment-actions {
  margin-top: 4px;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 640px) {
  .sheet {
    padding: 20px 18px;
  }

  .comment-item.is-reply {
    margin-left: 16px;
  }
}
</style>
