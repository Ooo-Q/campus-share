<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Check, DocumentDelete, Delete } from '@element-plus/icons-vue'
import { fetchCategories, type Category } from '../api/category'
import { createResource, uploadFile, updateResource, fetchResourceDetail } from '../api/resource'
import PageHeader from '../components/PageHeader.vue'

const route = useRoute()
const router = useRouter()

const form = reactive({
  title: '',
  categoryId: undefined as number | undefined,
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

async function loadCategories() {
  const res = await fetchCategories()
  categories.value = res.data
}

function handleFileChange(file: any) {
  pendingFile.value = file.raw || file
  fileName.value = pendingFile.value?.name || ''
  fileChanged.value = true
}

function handleRemoveFile() {
  form.fileUrl = ''
  fileName.value = ''
  pendingFile.value = null
  fileChanged.value = true
  ElMessage.info('已标记删除文件，保存后将移除文件')
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
    ElMessage.error(error.response?.data?.message || '加载资料失败')
    router.back()
  }
}

async function handleSubmit() {
  if (!form.title || !form.categoryId) {
    ElMessage.warning('请填写标题和选择分类')
    return
  }

  if (!isEditMode.value) {
    const hasFile = !!form.fileUrl || !!pendingFile.value
    const hasDesc = !!form.description
    if (!hasFile && !hasDesc) {
      ElMessage.warning('请上传文件或填写资料说明')
      return
    }

    if (form.categoryId == null && categories.value.length > 0) {
      form.categoryId = categories.value[0]!.id
    }
    if (!form.categoryId) {
      ElMessage.warning('请选择分类')
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

    if (!isEditMode.value) {
      if (!form.title) {
        form.title = fileName.value || '未命名资料'
      }
      const categoryId = form.categoryId!
      await createResource({
        title: form.title,
        categoryId,
        description: form.description || '',
        fileUrl: form.fileUrl,
        allowDownload: form.allowDownload,
      })
      ElMessage.success('上传成功')
      Object.assign(form, {
        title: '',
        categoryId: undefined,
        description: '',
        fileUrl: '',
        allowDownload: true,
      })
      fileName.value = ''
      pendingFile.value = null
      fileChanged.value = false
    } else {
      const updateData: any = {
        title: form.title,
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
      ElMessage.success('修改成功')
      router.back()
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || (isEditMode.value ? '修改失败' : '上传失败'))
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
  <div class="upload-page">
    <PageHeader :title="isEditMode ? '修改资料' : '上传学习资料'" />
    <el-card>
      <el-form label-width="90px">
      <el-form-item :label="isEditMode ? '资料标题' : '资料标题'" :required="isEditMode">
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入资料标题" />
      </el-form-item>
      <el-form-item label="分类" required>
        <el-select v-model="form.categoryId" placeholder="请选择分类" class="category-select">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="资料说明">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入资料说明" />
      </el-form-item>
      <el-form-item label="文件">
        <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
          <el-upload
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".pdf,.doc,.docx,.ppt,.pptx,.zip,.rar,.txt,.xls,.xlsx"
          >
            <el-button :loading="uploading || saving" type="primary" size="small">选择{{ isEditMode ? '新' : '' }}文件</el-button>
          </el-upload>
          <template v-if="fileName && fileChanged">
            <span style="color: #67c23a; display: inline-flex; align-items: center; gap: 4px;">
              <el-icon><Check /></el-icon>
              <span>新文件: {{ fileName }}</span>
            </span>
          </template>
          <template v-else-if="form.fileUrl && !fileChanged && isEditMode">
            <span style="color: #909399; display: inline-flex; align-items: center; gap: 4px;">
              <el-icon><Document /></el-icon>
              <span>当前文件: {{ form.fileUrl.split('/').pop() }}</span>
            </span>
          </template>
          <template v-else-if="!form.fileUrl && fileChanged && isEditMode">
            <span style="color: #f56c6c; display: inline-flex; align-items: center; gap: 4px;">
              <el-icon><DocumentDelete /></el-icon>
              <span>已标记删除文件</span>
            </span>
          </template>
          <template v-else-if="!form.fileUrl && !fileName && !isEditMode">
            <span style="color: #909399; display: inline-flex; align-items: center; gap: 4px;">
              <el-icon><DocumentDelete /></el-icon>
              <span>未选择文件</span>
            </span>
          </template>
          <el-button 
            v-if="form.fileUrl || fileName" 
            type="danger" 
            size="small" 
            plain
            @click="handleRemoveFile"
          >
            <el-icon><Delete /></el-icon> 删除文件
          </el-button>
        </div>
        <div v-if="isEditMode" style="margin-top: 8px; font-size: 12px; color: #909399; line-height: 1.5;">
          提示：不选择新文件将保持原有文件不变
        </div>
      </el-form-item>
      <el-form-item label="允许下载">
        <el-switch v-model="form.allowDownload" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSubmit">{{ isEditMode ? '保存修改' : '提交' }}</el-button>
        <el-button v-if="isEditMode" @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.upload-page {
  max-width: min(800px, 100%);
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.category-select {
  width: 100%;
  max-width: 300px;
}

.el-form {
  width: 100%;
}

.el-form-item {
  width: 100%;
}

.el-input,
.el-textarea,
.el-select {
  width: 100%;
  max-width: 100%;
}

@media (max-width: 480px) {
  .upload-page {
    padding: 0;
  }
  
  .category-select {
    max-width: 100%;
  }
  
  :deep(.el-form-item__label) {
    font-size: 14px;
    width: 80px !important;
  }
  
  :deep(.el-form-item__content) {
    margin-left: 80px !important;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .category-select {
    max-width: 250px;
  }
}

@media (min-width: 1920px) {
  .upload-page {
    max-width: 900px;
  }
}
</style>


