<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NInput,
  NModal,
  NForm,
  NFormItem,
  NSelect,
  NTag,
  NSpace,
  NPagination,
  NEmpty,
  NSpin,
  NCheckbox,
  NSwitch,
  NUpload,
  NIcon,
  NInputNumber,
  NRadioGroup,
  NRadioButton,
  type UploadCustomRequestOptions,
} from 'naive-ui'
import {
  CreateOutline,
  TrashOutline,
  DocumentOutline,
  EyeOutline,
  EyeOffOutline,
  WarningOutline,
  CheckmarkCircleOutline,
  DocumentAttachOutline,
} from '@vicons/ionicons5'
import request from '../../api/request'
import { fetchCategories, type Category } from '../../api/category'
import { uploadFile, updateResource, updateResourceVisibility } from '../../api/resource'
import type { PageResponse } from '../../api/resource'
import { createResourcePunishment } from '../../api/punishment'
import PageHeader from '../../components/PageHeader.vue'
import { message, dialog } from '../../utils/feedback'
import { resolveResourceTitle } from '../../utils/resource'

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
const categoryId = ref<number | null>(null)
const categories = ref<Category[]>([])
const selectedResources = ref<number[]>([])

const categoryOptions = computed(() =>
  categories.value.map((c) => ({ label: c.name, value: c.id })),
)

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
const punishmentType = ref<'WARNING' | 'SUSPENSION' | null>(null)
const punishmentDuration = ref<number | null>(null)
const punishmentReason = ref('')
const punishmentSubmitting = ref(false)

const punishTypeOptions = [
  { label: '警告', value: 'WARNING' },
  { label: '禁止上传资料', value: 'SUSPENSION' },
]

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
        categoryId: categoryId.value ?? undefined,
      },
    })
    resources.value = res.data?.records || []
    total.value = res.data?.total || 0
    selectedResources.value = []
  } catch {
    message.error('加载资料失败')
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
  if (index > -1) selectedResources.value.splice(index, 1)
  else selectedResources.value.push(resourceId)
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
      message.success('文件上传成功')
    })
    .catch(() => {
      message.error('文件上传失败')
    })
    .finally(() => {
      editUploading.value = false
    })
}

function editFileRequest({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const raw = file.file as File
  handleEditUpload(raw)
  // uploadFile manages its own promise; mark finish after kickoff path
  Promise.resolve()
    .then(() => onFinish())
    .catch(() => onError())
}

function handleRemoveFile() {
  editForm.fileUrl = ''
  editFileName.value = ''
  fileChanged.value = true
  message.info('已标记删除文件，保存后将移除文件')
}

async function handleSubmitEdit() {
  if (!editForm.categoryId) {
    message.warning('请选择分类')
    return
  }

  editSaving.value = true
  try {
    const updateData: any = {
      title: resolveResourceTitle(editForm.title, editFileName.value),
      categoryId: editForm.categoryId,
      description: editForm.description || '',
      allowDownload: editForm.allowDownload,
    }
    if (fileChanged.value) {
      updateData.fileUrl = editForm.fileUrl === '' ? null : editForm.fileUrl
    }
    await updateResource(editForm.id!, updateData)
    message.success('修改成功')
    editVisible.value = false
    loadData()
  } catch (error: any) {
    message.error(error.response?.data?.message || '修改失败')
  } finally {
    editSaving.value = false
  }
}

function handleDelete(resource: ResourceItem) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除资料 "${resource.title}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/resources/${resource.id}`)
        message.success('删除成功')
        await loadData()
      } catch (e: any) {
        message.error(e.response?.data?.message || '删除失败')
      }
    },
  })
}

async function handleToggleVisibility(resource: ResourceItem) {
  const target = resource.visibility === 'HIDDEN' ? 'VISIBLE' : 'HIDDEN'
  try {
    await updateResourceVisibility(resource.id, target)
    message.success(target === 'HIDDEN' ? '已隐藏该资料' : '已恢复可见')
    await loadData()
  } catch (e: any) {
    message.error(e.response?.data?.message || '操作失败')
  }
}

function handleBatchDelete() {
  if (selectedResources.value.length === 0) {
    message.warning('请选择要删除的资料')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedResources.value.length} 个资料吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete('/resources/batch', { data: { ids: selectedResources.value } })
        message.success('删除成功')
        selectedResources.value = []
        loadData()
      } catch (error: any) {
        message.error(error.response?.data?.message || '删除失败')
      }
    },
  })
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function handlePunish(resource: ResourceItem) {
  currentResource.value = resource
  punishmentDialogVisible.value = true
  actionOption.value = 'HIDE'
  punishmentType.value = null
  punishmentDuration.value = null
  punishmentReason.value = ''
}

async function confirmPunish() {
  if (!currentResource.value) return

  if (actionOption.value === 'HIDE_PUNISH' && !punishmentType.value) {
    message.warning('请选择处罚类型')
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
        punishmentType.value!,
        punishmentDuration.value,
        punishmentReason.value || '资料违规处罚',
      )
      message.success('资料已隐藏并已创建处罚')
    } else {
      message.success('资料已隐藏')
    }

    punishmentDialogVisible.value = false
    currentResource.value = null
    actionOption.value = 'HIDE'
    punishmentType.value = null
    punishmentDuration.value = null
    punishmentReason.value = ''
    await loadData()
  } catch (e: any) {
    message.error(e.response?.data?.message || '操作失败')
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
  <div class="page">
    <PageHeader title="资料管理" subtitle="管理平台所有上传的资料" :show-back="false" />

    <div class="glass-panel toolbar">
      <NSelect
        v-model:value="categoryId"
        :options="categoryOptions"
        placeholder="选择分类"
        clearable
        class="cat-select"
        @update:value="loadData"
      />
      <NInput
        v-model:value="keyword"
        placeholder="搜索标题"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <NButton type="primary" @click="handleSearch">搜索</NButton>
      <NButton v-if="selectedResources.length > 0" type="error" secondary @click="handleBatchDelete">
        批量删除 ({{ selectedResources.length }})
      </NButton>
    </div>

    <div class="glass-panel table-wrap">
      <NSpin :show="loading">
        <div v-if="resources.length === 0" class="empty-wrap">
          <NEmpty description="暂无资料" />
        </div>
        <div v-else class="item-list">
          <div v-for="resource in resources" :key="resource.id" class="surface-card list-item">
            <NCheckbox
              :checked="selectedResources.includes(resource.id)"
              @update:checked="() => toggleSelect(resource.id)"
            />
            <div class="icon-wrap">
              <NIcon :component="DocumentOutline" :size="24" />
            </div>
            <div
              class="item-info"
              @click="
                router.push({
                  path: `/admin/resources/${resource.id}`,
                  query: { from: 'resources' },
                })
              "
            >
              <div class="item-title">
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
              <div class="item-meta muted">
                <span>分类：{{ resource.categoryName || '-' }}</span>
                <span>上传者：{{ resource.ownerName || '-' }}</span>
                <span>浏览 {{ resource.viewCount || 0 }}</span>
                <span>下载 {{ resource.downloadCount || 0 }}</span>
                <span>点赞 {{ resource.likeCount || 0 }}</span>
                <span>收藏 {{ resource.favoriteCount || 0 }}</span>
              </div>
            </div>
            <div class="item-right">
              <NSpace :size="4" :wrap="false">
                <NButton size="small" quaternary @click="handleEdit(resource)">
                  <template #icon>
                    <NIcon :component="CreateOutline" />
                  </template>
                  修改
                </NButton>
                <NButton size="small" quaternary type="warning" @click="handleToggleVisibility(resource)">
                  <template #icon>
                    <NIcon
                      :component="resource.visibility === 'HIDDEN' ? EyeOutline : EyeOffOutline"
                    />
                  </template>
                  {{ resource.visibility === 'HIDDEN' ? '恢复可见' : '隐藏' }}
                </NButton>
                <NButton size="small" quaternary type="error" @click="handlePunish(resource)">
                  <template #icon>
                    <NIcon :component="WarningOutline" />
                  </template>
                  处罚
                </NButton>
                <NButton size="small" quaternary type="error" @click="handleDelete(resource)">
                  <template #icon>
                    <NIcon :component="TrashOutline" />
                  </template>
                  删除
                </NButton>
              </NSpace>
              <div class="item-time muted">{{ formatDate(resource.createdAt) }}</div>
            </div>
          </div>
        </div>
      </NSpin>

      <div v-if="total > 0" class="pagination">
        <NPagination
          v-model:page="page"
          v-model:page-size="size"
          :item-count="total"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handleSizeChange"
        />
      </div>
    </div>

    <NModal
      v-model:show="editVisible"
      preset="card"
      title="修改资料"
      style="width: 600px; max-width: 94vw"
      :bordered="false"
    >
      <NForm label-placement="left" label-width="90">
        <NFormItem label="资料标题">
          <NInput
            v-model:value="editForm.title"
            maxlength="100"
            show-count
            placeholder="可不填，默认用文件名"
          />
        </NFormItem>
        <NFormItem label="分类" required>
          <NSelect
            v-model:value="editForm.categoryId"
            :options="categoryOptions"
            placeholder="请选择分类"
          />
        </NFormItem>
        <NFormItem label="资料说明">
          <NInput
            v-model:value="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入资料说明"
          />
        </NFormItem>
        <NFormItem label="文件">
          <div class="file-row">
            <NUpload
              :show-file-list="false"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.zip,.rar,.txt,.xls,.xlsx"
              :custom-request="editFileRequest"
            >
              <NButton size="small" type="primary" :loading="editUploading">选择新文件</NButton>
            </NUpload>
            <span v-if="editFileName" class="file-ok">
              <NIcon :component="CheckmarkCircleOutline" /> 新文件: {{ editFileName }}
            </span>
            <span v-else-if="editForm.fileUrl" class="muted">
              <NIcon :component="DocumentOutline" />
              当前文件: {{ editForm.fileUrl.split('/').pop() }}
            </span>
            <span v-else class="muted">
              <NIcon :component="DocumentAttachOutline" /> 无文件
            </span>
            <NButton
              v-if="editForm.fileUrl"
              size="small"
              type="error"
              secondary
              @click="handleRemoveFile"
            >
              删除文件
            </NButton>
          </div>
          <p class="file-hint muted">提示：不选择新文件将保持原有文件不变</p>
        </NFormItem>
        <NFormItem label="允许下载">
          <NSwitch v-model:value="editForm.allowDownload" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="editVisible = false">取消</NButton>
          <NButton type="primary" :loading="editSaving" @click="handleSubmitEdit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal
      v-model:show="punishmentDialogVisible"
      preset="card"
      title="处理资料"
      style="width: 600px; max-width: 94vw"
      :bordered="false"
    >
      <div v-if="currentResource">
        <p class="modal-lead muted">
          资料：{{ currentResource.title }}<br />
          上传者：{{ currentResource.ownerName }}
        </p>
        <NForm label-placement="left" label-width="120">
          <NFormItem label="处理动作">
            <NRadioGroup v-model:value="actionOption" size="small">
              <NRadioButton value="HIDE" label="隐藏资料" />
              <NRadioButton value="HIDE_PUNISH" label="隐藏并处罚" />
            </NRadioGroup>
          </NFormItem>
          <NFormItem v-if="actionOption === 'HIDE_PUNISH'" label="处罚类型" required>
            <NSelect
              v-model:value="punishmentType"
              :options="punishTypeOptions"
              placeholder="选择处罚类型"
            />
          </NFormItem>
          <NFormItem v-if="actionOption === 'HIDE_PUNISH'" label="处罚时长(天)">
            <NInputNumber
              v-model:value="punishmentDuration"
              :min="1"
              :max="365"
              clearable
              placeholder="留空为永久"
              style="width: 100%"
            />
          </NFormItem>
          <NFormItem v-if="actionOption === 'HIDE_PUNISH'" label="处罚原因">
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
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="punishmentDialogVisible = false">取消</NButton>
          <NButton type="primary" :loading="punishmentSubmitting" @click="confirmPunish">
            确定
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.page {
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.cat-select {
  width: 160px;
  max-width: 100%;
}

.search-input {
  width: 220px;
  max-width: 100%;
}

.table-wrap {
  padding: 16px 20px 20px;
}

.empty-wrap {
  padding: 48px 0;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.list-item:hover {
  border-color: rgba(122, 158, 142, 0.35);
  box-shadow: var(--m-shadow-soft);
}

.icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(145deg, var(--m-sage-soft), var(--m-sage-deep));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--m-ink);
  margin-bottom: 4px;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.item-time {
  font-size: 13px;
  white-space: nowrap;
  min-width: 150px;
  text-align: right;
}

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.file-ok {
  color: var(--m-sage-deep);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.file-hint {
  margin: 8px 0 0;
  font-size: 12px;
}

.modal-lead {
  margin: 0 0 16px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .cat-select,
  .search-input {
    width: 100%;
  }

  .list-item {
    flex-wrap: wrap;
  }

  .item-right {
    width: 100%;
    flex-wrap: wrap;
    justify-content: space-between;
    margin-top: 4px;
  }

  .item-time {
    min-width: auto;
    text-align: left;
  }
}
</style>
