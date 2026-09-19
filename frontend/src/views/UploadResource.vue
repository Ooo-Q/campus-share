<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSwitch,
  NUpload,
  NIcon,
  NSpace,
  type UploadFileInfo,
} from 'naive-ui'
import {
  CheckmarkCircleOutline,
  DocumentOutline,
  DocumentTextOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import { fetchCategories, type Category } from '../api/category'
import { createResource, uploadFile, updateResource, fetchResourceDetail } from '../api/resource'
import { message } from '../utils/feedback'
import { resolveResourceTitle } from '../utils/resource'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const form = reactive({
  title: '',
  categoryId: null as number | null,
  description: '',
  fileUrl: '',
  allowDownload: true,
})

const fileName = ref('')
const uploading = ref(false)
const saving = ref(false)
const categories = ref<Category[]>([])
const pendingFile = ref<File | null>(null)
const resourceId = ref<number | null>(null)
const isEditMode = ref(false)
const fileChanged = ref(false)
const originalFileUrl = ref('')

const categoryOptions = ref<{ label: string; value: number }[]>([])

async function loadCategories() {
  const res = await fetchCategories()
  categories.value = res.data
  categoryOptions.value = (res.data || []).map((c) => ({ label: c.name, value: c.id }))
}

function handleFileChange(options: { file: UploadFileInfo; fileList: UploadFileInfo[] }) {
  const raw = options.file.file
  if (!raw) return
  pendingFile.value = raw
  fileName.value = raw.name || ''
  fileChanged.value = true
}

function handleRemoveFile() {
  form.fileUrl = ''
  fileName.value = ''
  pendingFile.value = null
  fileChanged.value = true
  message.info('已标记删除文件，保存后将移除文件')
}

async function loadResource(id: number) {
  try {
    const res = await fetchResourceDetail(id)
    const resource = res.data
    form.title = resource.title
    form.categoryId = resource.categoryId
    form.description = resource.description || ''
    form.fileUrl = resource.fileUrl || ''
    form.allowDownload = resource.allowDownload ?? true
    originalFileUrl.value = resource.fileUrl || ''

    if (resource.fileUrl) {
      const urlParts = resource.fileUrl.split('/')
      fileName.value = urlParts[urlParts.length - 1] || ''
    } else {
      fileName.value = ''
    }

    fileChanged.value = false
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载资料失败')
    router.back()
  }
}

async function handleSubmit() {
  if (!form.categoryId) {
    message.warning('请选择分类')
    return
  }

  if (!isEditMode.value) {
    const hasFile = !!form.fileUrl || !!pendingFile.value
    const hasDesc = !!form.description
    if (!hasFile && !hasDesc) {
      message.warning('请上传文件或填写资料说明')
      return
    }
  }

  saving.value = true
  try {
    if (!form.fileUrl && pendingFile.value) {
      uploading.value = true
      const res = await uploadFile(pendingFile.value)
      form.fileUrl = res.data
      uploading.value = false
    }

    const title = resolveResourceTitle(form.title, fileName.value)

    if (!isEditMode.value) {
      await createResource({
        title,
        categoryId: form.categoryId,
        description: form.description || '',
        fileUrl: form.fileUrl,
        allowDownload: form.allowDownload,
      })
      message.success('上传成功')
      Object.assign(form, {
        title: '',
        categoryId: null,
        description: '',
        fileUrl: '',
        allowDownload: true,
      })
      fileName.value = ''
      pendingFile.value = null
      fileChanged.value = false
    } else {
      const updateData: any = {
        title,
        categoryId: form.categoryId,
        description: form.description || '',
        allowDownload: form.allowDownload,
      }

      if (fileChanged.value) {
        if (form.fileUrl === '') {
          updateData.fileUrl = null
        } else {
          updateData.fileUrl = form.fileUrl
        }
      }

      await updateResource(resourceId.value!, updateData)
      message.success('修改成功')
      router.back()
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || (isEditMode.value ? '修改失败' : '上传失败'))
  } finally {
    uploading.value = false
    saving.value = false
  }
}

onMounted(async () => {
  await loadCategories()

  const id = route.query.id
  if (id) {
    resourceId.value = Number(id)
    isEditMode.value = true
    await loadResource(resourceId.value)
  }
})
</script>

<template>
  <div class="page">
    <PageHeader :title="isEditMode ? '修改资料' : '上传学习资料'" />

    <div class="glass-panel form-panel">
      <NForm label-placement="top">
        <NFormItem label="资料标题">
          <NInput
            v-model:value="form.title"
            maxlength="100"
            show-count
            placeholder="可不填，默认用文件名"
          />
        </NFormItem>
        <NFormItem label="分类" required>
          <NSelect
            v-model:value="form.categoryId"
            :options="categoryOptions"
            placeholder="请选择分类"
            class="category-select"
          />
        </NFormItem>
        <NFormItem label="资料说明">
          <NInput
            v-model:value="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入资料说明"
          />
        </NFormItem>
        <NFormItem label="文件">
          <div class="file-area">
            <NUpload
              :default-upload="false"
              :show-file-list="false"
              accept=".pdf,.doc,.docx,.ppt,.pptx,.zip,.rar,.txt,.xls,.xlsx"
              @change="handleFileChange"
            >
              <NButton :loading="uploading || saving" type="primary" secondary>
                选择{{ isEditMode ? '新' : '' }}文件
              </NButton>
            </NUpload>

            <div v-if="fileName && fileChanged" class="file-status ok">
              <NIcon :component="CheckmarkCircleOutline" />
              <span>新文件: {{ fileName }}</span>
            </div>
            <div v-else-if="form.fileUrl && !fileChanged && isEditMode" class="file-status muted">
              <NIcon :component="DocumentOutline" />
              <span>当前文件: {{ form.fileUrl.split('/').pop() }}</span>
            </div>
            <div v-else-if="!form.fileUrl && fileChanged && isEditMode" class="file-status danger">
              <NIcon :component="DocumentTextOutline" />
              <span>已标记删除文件</span>
            </div>
            <div v-else-if="!form.fileUrl && !fileName && !isEditMode" class="file-status muted">
              <NIcon :component="DocumentTextOutline" />
              <span>未选择文件</span>
            </div>

            <NButton
              v-if="form.fileUrl || fileName"
              type="error"
              secondary
              size="small"
              @click="handleRemoveFile"
            >
              <template #icon><NIcon :component="TrashOutline" /></template>
              删除文件
            </NButton>
          </div>
          <p v-if="isEditMode" class="hint muted">提示：不选择新文件将保持原有文件不变</p>
        </NFormItem>
        <NFormItem label="允许下载">
          <NSwitch v-model:value="form.allowDownload" />
        </NFormItem>
        <NFormItem>
          <NSpace>
            <NButton type="primary" :loading="saving" @click="handleSubmit">
              {{ isEditMode ? '保存修改' : '提交' }}
            </NButton>
            <NButton v-if="isEditMode" @click="router.back()">取消</NButton>
          </NSpace>
        </NFormItem>
      </NForm>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
  padding: clamp(12px, 2vw, 24px);
  box-sizing: border-box;
}

.form-panel {
  padding: 24px;
}

.category-select {
  width: 100%;
  max-width: 300px;
}

.file-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  width: 100%;
}

.file-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.file-status.ok {
  color: var(--m-sage-deep);
}

.file-status.danger {
  color: var(--m-terracotta);
}

.hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 480px) {
  .category-select {
    max-width: 100%;
  }
}
</style>
