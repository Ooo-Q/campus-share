<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Edit, Delete, Hide, View, Warning, Check, DocumentDelete } from '@element-plus/icons-vue'
import request from '../../api/request'
import { fetchCategories, type Category } from '../../api/category'
import { uploadFile, updateResource, updateResourceVisibility } from '../../api/resource'
import type { PageResponse } from '../../api/resource'
import { createResourcePunishment } from '../../api/punishment'

interface ResourceItem {
  id: number
  title: string
  categoryId: number
  categoryName?: string
  description: string
  fileUrl: string
  ownerId: number
  ownerName: string
  likeCount: number
  downloadCount: number
  viewCount: number
  favoriteCount?: number
  visibility?: string
  allowDownload: boolean
  createdAt: string
}

const router = useRouter()
const loading = ref(false)
const resources = ref<ResourceItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const categoryId = ref<number | undefined>()
const categories = ref<Category[]>([])
const selectedResources = ref<number[]>([])

const editVisible = ref(false)
const editForm = reactive({
  id: undefined as number | undefined,
  title: '',
  categoryId: undefined as number | undefined,
  description: '',
  fileUrl: '',
  allowDownload: true,
})
const editFileName = ref('')
const editUploading = ref(false)
const editSaving = ref(false)
const originalFileUrl = ref('')
const fileChanged = ref(false)
const punishmentDialogVisible = ref(false)
const currentResource = ref<ResourceItem | null>(null)
const actionOption = ref<'HIDE' | 'HIDE_PUNISH'>('HIDE')
const punishmentType = ref<'WARNING' | 'SUSPENSION' | ''>('')
const punishmentDuration = ref<number | null>(null)
const punishmentReason = ref('')
const punishmentSubmitting = ref(false)

async function loadCategories() {
  const res = await fetchCategories()
  categories.value = res.data
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get<PageResponse<ResourceItem>>('/resources/manage', {
      params: {
        page: page.value,
        size: size.value,
        keyword: keyword.value || undefined,
        categoryId: categoryId.value,
      },
    })
    resources.value = res.data?.records || []
    total.value = res.data?.total || 0
    selectedResources.value = []
  } catch (e) {
    ElMessage.error('加载资料失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

function handlePageChange(p: number) {
  page.value = p
  loadData()
}

function handleSizeChange(s: number) {
  size.value = s
  page.value = 1
  loadData()
}

function toggleSelect(resourceId: number) {
  const index = selectedResources.value.indexOf(resourceId)
  if (index > -1) {
    selectedResources.value.splice(index, 1)
  } else {
    selectedResources.value.push(resourceId)
  }
}

function handleEdit(resource: ResourceItem) {
  editForm.id = resource.id
  editForm.title = resource.title
  editForm.categoryId = resource.categoryId
  editForm.description = resource.description || ''
  editForm.fileUrl = resource.fileUrl || ''
  editForm.allowDownload = resource.allowDownload ?? true

  originalFileUrl.value = resource.fileUrl || ''

  if (resource.fileUrl) {
    const urlParts = resource.fileUrl.split('/')
    editFileName.value = urlParts[urlParts.length - 1] || ''
  } else {
    editFileName.value = ''
  }

  fileChanged.value = false

  editVisible.value = true
}

function handleEditUpload(file: File) {
  editUploading.value = true
  uploadFile(file)
    .then((res: any) => {
      editForm.fileUrl = res.data || res
      editFileName.value = file.name
      fileChanged.value = true
      ElMessage.success('文件上传成功')
    })
    .catch(() => {
      ElMessage.error('文件上传失败')
    })
    .finally(() => {
      editUploading.value = false
    })
}

function handleRemoveFile() {
  editForm.fileUrl = ''
  editFileName.value = ''
  fileChanged.value = true
  ElMessage.info('已标记删除文件，保存后将移除文件')
}

async function handleSubmitEdit() {
  if (!editForm.title || !editForm.categoryId) {
    ElMessage.warning('请填写标题和选择分类')
    return
  }

  editSaving.value = true
  try {
    const updateData: any = {
      title: editForm.title,
      categoryId: editForm.categoryId,
      description: editForm.description || '',
      allowDownload: editForm.allowDownload,
    }

    if (fileChanged.value) {
      if (editForm.fileUrl === '') {
        updateData.fileUrl = null
      } else {
        updateData.fileUrl = editForm.fileUrl
      }
    }

    await updateResource(editForm.id!, updateData)
    ElMessage.success('修改成功')
    editVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    editSaving.value = false
  }
}

async function handleDelete(resource: ResourceItem) {
  try {
    await ElMessageBox.confirm(`确定要删除资料 "${resource.title}" 吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await request.delete(`/resources/${resource.id}`)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

async function handleToggleVisibility(resource: ResourceItem) {
  const target = resource.visibility === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  try {
    await updateResourceVisibility(resource.id, target)
    ElMessage.success(target === 'HIDDEN' ? '已隐藏该资料' : '已恢复可见')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function handleBatchDelete() {
  if (selectedResources.value.length === 0) {
    ElMessage.warning('请选择要删除的资料')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedResources.value.length} 个资料吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        await request.delete('/resources/batch', { data: { ids: selectedResources.value } })
        ElMessage.success('删除成功')
        selectedResources.value = []
        loadData()
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '删除失败')
      }
    })
    .catch(() => {})
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function handlePunish(resource: ResourceItem) {
  currentResource.value = resource
  punishmentDialogVisible.value = true
  actionOption.value = 'HIDE'
  punishmentType.value = ''
  punishmentDuration.value = null
  punishmentReason.value = ''
}

async function confirmPunish() {
  if (!currentResource.value) {
    return
  }

  if (actionOption.value === 'HIDE_PUNISH' && !punishmentType.value) {
    ElMessage.warning('请选择处罚类型')
    return
  }
  
  punishmentSubmitting.value = true
  try {
    await updateResourceVisibility(currentResource.value.id, 'HIDDEN')

    if (actionOption.value === 'HIDE_PUNISH') {
      await createResourcePunishment(
        currentResource.value.ownerId,
        currentResource.value.id,
        currentResource.value.title,
        punishmentType.value,
        punishmentDuration.value,
        punishmentReason.value || '资料违规处罚'
      )
      ElMessage.success('资料已隐藏并已创建处罚')
    } else {
      ElMessage.success('资料已隐藏')
    }
    
    punishmentDialogVisible.value = false
    currentResource.value = null
    actionOption.value = 'HIDE'
    punishmentType.value = ''
    punishmentDuration.value = null
    punishmentReason.value = ''
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    punishmentSubmitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<template>
  <div class="admin-resources-page">
    <el-card class="resources-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>资料管理</h3>
            <p class="card-subtitle">管理平台所有上传的资料</p>
          </div>
          <div class="search-bar">
            <el-select
              v-model="categoryId"
              placeholder="选择分类"
              clearable
              class="admin-category-select"
              @change="loadData"
            >
              <el-option v-for="c in categories" :key="c.id" :value="c.id" :label="c.name" />
            </el-select>
            <el-input
              v-model="keyword"
              placeholder="搜索标题"
              class="admin-search-input"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" @click="handleSearch" class="admin-search-btn">搜索</el-button>
            <el-button
              v-if="selectedResources.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedResources.length }})
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="resources.length === 0" class="empty-resources">
          <el-empty description="暂无资料" />
        </div>
        <div v-else class="resources-list">
          <div v-for="resource in resources" :key="resource.id" class="resource-item">
            <el-checkbox
              :model-value="selectedResources.includes(resource.id)"
              @change="toggleSelect(resource.id)"
              class="resource-checkbox"
            />
            <div class="resource-icon-wrapper">
              <el-icon class="resource-icon"><Document /></el-icon>
            </div>
            <div class="resource-info" @click="router.push({ path: `/admin/resources/${resource.id}`, query: { from: 'resources' } })">
              <div class="resource-title">
                {{ resource.title }}
                <el-tag v-if="resource.visibility === 'HIDDEN'" type="warning" size="small" style="margin-left: 8px">已隐藏</el-tag>
              </div>
              <div class="resource-meta">
                <span>分类：{{ resource.categoryName || '-' }}</span>
                <span>上传者：{{ resource.ownerName || '-' }}</span>
                <span>浏览 {{ resource.viewCount || 0 }}</span>
                <span>下载 {{ resource.downloadCount || 0 }}</span>
                <span>点赞 {{ resource.likeCount || 0 }}</span>
                <span>收藏 {{ resource.favoriteCount || 0 }}</span>
              </div>
            </div>
            <div class="resource-right">
              <div class="resource-actions">
                <el-button
                  size="small"
                  text
                  type="info"
                  :icon="Edit"
                  @click.stop="handleEdit(resource)"
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
                  :icon="Warning"
                  @click.stop="handlePunish(resource)"
                >
                  处罚
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleDelete(resource)"
                >
                  删除
                </el-button>
              </div>
              <div class="resource-time">{{ formatDate(resource.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="editVisible" title="修改资料" width="600px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="资料标题" required>
          <el-input v-model="editForm.title" maxlength="100" show-word-limit placeholder="请输入资料标题" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="资料说明">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入资料说明" />
        </el-form-item>
        <el-form-item label="文件">
          <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
            <el-upload
              :show-file-list="false"
              :http-request="(options: any) => handleEditUpload(options.file)"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.zip,.rar,.txt,.xls,.xlsx"
            >
              <el-button :loading="editUploading" type="primary" size="small">选择新文件</el-button>
            </el-upload>
            <span v-if="editFileName" style="color: #67c23a">
              <el-icon><Check /></el-icon> 新文件: {{ editFileName }}
            </span>
            <span v-else-if="editForm.fileUrl" style="color: #909399">
              <el-icon><Document /></el-icon> 当前文件: {{ editForm.fileUrl.split('/').pop() }}
            </span>
            <span v-else style="color: #909399">
              <el-icon><DocumentDelete /></el-icon> 无文件
            </span>
            <el-button 
              v-if="editForm.fileUrl" 
              type="danger" 
              size="small" 
              plain
              @click="handleRemoveFile"
            >
              <el-icon><Delete /></el-icon> 删除文件
            </el-button>
          </div>
          <div style="margin-top: 8px; font-size: 12px; color: #909399;">
            提示：不选择新文件将保持原有文件不变
          </div>
        </el-form-item>
        <el-form-item label="允许下载">
          <el-switch v-model="editForm.allowDownload" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSubmitEdit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="punishmentDialogVisible" title="处理资料" width="600px">
      <div v-if="currentResource">
        <p style="margin-bottom: 16px; color: #606266">
          资料：{{ currentResource.title }}<br />
          上传者：{{ currentResource.ownerName }}
        </p>
        <el-form label-width="120px">
          <el-form-item label="处理动作">
            <el-radio-group v-model="actionOption" class="action-radio-group">
              <el-radio-button label="HIDE">隐藏资料</el-radio-button>
              <el-radio-button label="HIDE_PUNISH">隐藏并处罚</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处罚类型" v-if="actionOption === 'HIDE_PUNISH'" required>
            <el-select v-model="punishmentType" placeholder="选择处罚类型" style="width: 100%">
              <el-option label="警告" value="WARNING" />
              <el-option label="禁止上传资料" value="SUSPENSION" />
            </el-select>
          </el-form-item>
          <el-form-item label="处罚时长(天)" v-if="actionOption === 'HIDE_PUNISH'">
            <el-input-number
              v-model="punishmentDuration"
              :min="1"
              :max="365"
              placeholder="留空为永久"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="处罚原因" v-if="actionOption === 'HIDE_PUNISH'">
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
      </div>
      <template #footer>
        <el-button @click="punishmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="punishmentSubmitting" @click="confirmPunish">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.action-radio-group {
  display: flex;
  gap: 8px;
}

.action-radio-group :deep(.el-radio-button__inner) {
  padding: 8px 16px;
}
.admin-resources-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.resources-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.header-left h3 {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 4px 0;
  color: #1f2937;
}

.card-subtitle {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.admin-search-input {
  width: 200px;
  height: 40px;
}

.admin-search-input :deep(.el-input) {
  height: 40px;
}

.admin-search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  box-sizing: border-box;
}

.admin-search-input :deep(.el-input__inner) {
  height: 38px !important;
  line-height: 38px;
}

.admin-search-btn {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  padding: 0 20px;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.admin-category-select {
  width: 160px;
  height: 40px;
}

.admin-category-select :deep(.el-select) {
  height: 40px !important;
}

.admin-category-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  box-sizing: border-box;
}

.admin-category-select :deep(.el-input__inner) {
  height: 38px !important;
  line-height: 38px;
}

.admin-category-select :deep(.el-select__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  box-sizing: border-box;
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

.resource-checkbox {
  flex-shrink: 0;
}

.resource-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.25);
}

.resource-icon {
  font-size: 24px;
  color: #ffffff;
}

.resource-info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.resource-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.resource-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
  }

  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar > * {
    width: 100%;
  }

  .resource-item {
    flex-wrap: wrap;
  }

  .resource-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }

  .resource-time {
    min-width: auto;
    text-align: left;
  }
}
</style>
