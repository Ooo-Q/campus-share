<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, User, Lock, Message, Phone, Loading, Plus, UserFilled } from '@element-plus/icons-vue'
import request from '../../api/request'
import { uploadAvatar } from '../../api/auth'
import type { PageResponse } from '../../api/resource'

const router = useRouter()

interface Student {
  id: number
  username: string
  nickname?: string
  gender?: string
  phone?: string
  email?: string
  avatar?: string
  createdAt: string
}

const loading = ref(false)
const tableData = ref<Student[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const selectedRows = ref<Student[]>([])
const selectedStudentIds = computed(() => selectedRows.value.map(s => s.id))
const sortProp = ref<string>('createdAt')
const sortOrder = ref<'ascending' | 'descending' | null>('descending')

const addVisible = ref(false)
const addForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  gender: '',
  phone: '',
  email: '',
  avatar: '',
})
const addAvatarUploading = ref(false)
const addAvatarPreview = ref('')

const editVisible = ref(false)
const editForm = reactive({
  id: 0,
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  gender: '',
  phone: '',
  email: '',
  avatar: '',
})

const avatarUploading = ref(false)
const avatarPreview = ref('')

async function handleAvatarUpload(file: File) {
  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.success && res.data) {
      editForm.avatar = res.data
      avatarPreview.value = `/api${res.data}`
      ElMessage.success('头像上传成功')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function handleAddAvatarUpload(file: File) {
  addAvatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    if (res.success && res.data) {
      addForm.avatar = res.data
      addAvatarPreview.value = `/api${res.data}`
      ElMessage.success('头像上传成功')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '头像上传失败')
  } finally {
    addAvatarUploading.value = false
  }
}

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB!')
    return false
  }
  return true
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get<PageResponse<Student>>('/admin/students', {
      params: {
        page: page.value,
        size: size.value,
        keyword: keyword.value || undefined,
        role: 'STUDENT',
      },
    })
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error('加载学生列表失败')
  } finally {
    loading.value = false
  }
}

const sortedTableData = computed(() => {
  const arr = [...tableData.value]
  if (!sortProp.value || !sortOrder.value) return arr
  const factor = sortOrder.value === 'ascending' ? 1 : -1
  const prop = sortProp.value as keyof Student
  return arr.sort((a, b) => {
    const va = a[prop]
    const vb = b[prop]
    if (prop === 'createdAt') {
      const ta = va ? new Date(va as any).getTime() : 0
      const tb = vb ? new Date(vb as any).getTime() : 0
      return (ta - tb) * factor
    }
    if (typeof va === 'number' && typeof vb === 'number') {
      return (va - vb) * factor
    }
    const sa = String(va ?? '')
    const sb = String(vb ?? '')
    return sa.localeCompare(sb) * factor
  })
})

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

function toggleSelect(studentId: number) {
  const student = tableData.value.find(s => s.id === studentId)
  if (!student) return
  const index = selectedRows.value.findIndex(s => s.id === studentId)
  if (index > -1) {
    selectedRows.value.splice(index, 1)
  } else {
    selectedRows.value.push(student)
  }
}

function toggleSelectAll() {
  if (selectedRows.value.length === tableData.value.length) {
    selectedRows.value = []
  } else {
    selectedRows.value = [...tableData.value]
  }
}

function handleView(row: Student) {
  router.push(`/admin/user/${row.id}`)
}

function handleAdd() {
  addForm.username = ''
  addForm.password = ''
  addForm.confirmPassword = ''
  addForm.nickname = ''
  addForm.gender = ''
  addForm.phone = ''
  addForm.email = ''
  addForm.avatar = ''
  addAvatarPreview.value = ''
  addVisible.value = true
}

async function handleCreate() {
  if (!addForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!addForm.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (!addForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (addForm.password.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }
  if (addForm.password !== addForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  try {
    const payload: any = {
      username: addForm.username,
      password: addForm.password,
      nickname: addForm.nickname,
      gender: addForm.gender || null,
      phone: addForm.phone || null,
      email: addForm.email || null,
      avatar: addForm.avatar || null,
    }
    
    await request.post('/admin/students', payload)
    ElMessage.success('添加成功')
    addVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '添加失败')
  }
}

function handleEdit(row: Student) {
  editForm.id = row.id
  editForm.username = row.username || ''
  editForm.password = ''
  editForm.confirmPassword = ''
  editForm.nickname = row.nickname || ''
  editForm.gender = row.gender || ''
  editForm.phone = row.phone || ''
  editForm.email = row.email || ''
  editForm.avatar = row.avatar || ''
  avatarPreview.value = row.avatar ? `/api${row.avatar}` : ''
  editVisible.value = true
}

async function handleUpdate() {
  if (!editForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!editForm.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }

  if (editForm.password) {
    if (editForm.password.length < 6) {
      ElMessage.warning('密码长度至少6位')
      return
    }
    if (editForm.password !== editForm.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }
  }
  
  try {
    const payload: any = {
      username: editForm.username,
      nickname: editForm.nickname,
      gender: editForm.gender || null,
      phone: editForm.phone || null,
      email: editForm.email || null,
      avatar: editForm.avatar || null,
    }

    if (editForm.password) {
      payload.password = editForm.password
    }
    
    await request.put(`/admin/students/${editForm.id}`, payload)
    ElMessage.success('修改成功')
    editVisible.value = false
    loadData()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  }
}

function handleDelete(row: Student) {
  ElMessageBox.confirm(`确定要删除学生 "${row.nickname || row.username}" 吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        await request.delete(`/admin/students/${row.id}`)
        ElMessage.success('删除成功')
        loadData()
      } catch (error: any) {
        ElMessage.error(error.response?.data?.message || '删除失败')
      }
    })
    .catch(() => {})
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的学生')
    return
  }
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个学生吗？`, '确认删除', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        const ids = selectedRows.value.map((row) => row.id)
        await request.delete('/admin/students/batch', { data: { ids } })
        ElMessage.success('删除成功')
        selectedRows.value = []
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

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-students-page">
    <el-card class="students-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>学生管理</h3>
            <p class="card-subtitle">管理所有注册学生信息</p>
          </div>
          <div class="search-bar">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名、昵称"
              class="admin-search-input"
              clearable
              @keyup.enter="handleSearch"
            />
            <el-button type="primary" @click="handleSearch" class="admin-search-btn">搜索</el-button>
            <el-button
              v-if="selectedRows.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedRows.length }})
            </el-button>
            <el-button type="primary" :icon="Plus" @click="handleAdd" class="add-student-btn">添加学生</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="tableData.length === 0" class="empty-students">
          <el-empty description="暂无学生" />
        </div>
        <div v-else class="students-list">
          <div class="list-header">
            <el-checkbox
              :model-value="selectedRows.length === tableData.length && tableData.length > 0"
              @change="toggleSelectAll"
              class="select-all-checkbox"
            />
            <span class="list-header-text">全选</span>
          </div>
          <div v-for="student in sortedTableData" :key="student.id" class="student-item">
            <el-checkbox
              :model-value="selectedStudentIds.includes(student.id)"
              @change="toggleSelect(student.id)"
              class="student-checkbox"
            />
            <div class="student-icon-wrapper">
              <el-icon class="student-icon"><UserFilled /></el-icon>
            </div>
            <div class="student-info" @click="handleView(student)">
              <div class="student-title">
                {{ student.nickname || student.username }}
                <el-tag v-if="student.gender === 'MALE'" size="small" style="margin-left: 8px">男</el-tag>
                <el-tag v-else-if="student.gender === 'FEMALE'" size="small" style="margin-left: 8px">女</el-tag>
              </div>
              <div class="student-meta">
                <span>用户名：{{ student.username }}</span>
                <span v-if="student.phone">手机：{{ student.phone }}</span>
                <span v-if="student.email">邮箱：{{ student.email }}</span>
              </div>
            </div>
            <div class="student-right">
              <div class="student-actions">
                <el-button
                  size="small"
                  text
                  type="info"
                  :icon="Edit"
                  @click.stop="handleEdit(student)"
                >
                  修改
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleDelete(student)"
                >
                  删除
                </el-button>
              </div>
              <div class="student-time">{{ formatDate(student.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="addVisible" title="添加学生" width="600px">
      <el-form :model="addForm" label-width="100px" class="add-student-form">
        <el-form-item label="用户名" required>
          <el-input
            v-model="addForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            maxlength="50"
            clearable
          />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input
            v-model="addForm.nickname"
            placeholder="请输入昵称"
            :prefix-icon="User"
            maxlength="50"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input
            v-model="addForm.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            :prefix-icon="Lock"
            show-password
            maxlength="50"
            clearable
          />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input
            v-model="addForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            show-password
            maxlength="50"
            clearable
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="addForm.gender" placeholder="请选择性别" style="width: 100%" clearable>
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="addForm.phone"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            maxlength="11"
            clearable
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="addForm.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
            type="email"
            clearable
          />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload-section">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="(options: any) => handleAddAvatarUpload(options.file)"
              accept="image/*"
              class="avatar-uploader"
            >
              <el-avatar
                v-if="addAvatarPreview || addForm.avatar"
                :src="addAvatarPreview || (addForm.avatar ? `/api${addForm.avatar}` : '')"
                :size="80"
                shape="square"
                class="avatar-preview"
              />
              <el-avatar v-else :size="80" shape="square" class="avatar-placeholder">
                {{ (addForm.nickname || addForm.username || 'U').slice(0, 1).toUpperCase() }}
              </el-avatar>
            </el-upload>
            <div class="avatar-actions">
              <div class="avatar-tip">点击头像上传，支持JPG/PNG格式，大小不超过5MB</div>
              <el-button
                v-if="addAvatarPreview || addForm.avatar"
                link
                type="danger"
                size="small"
                @click="addForm.avatar = ''; addAvatarPreview = ''"
              >
                清除头像
              </el-button>
              <div v-if="addAvatarUploading" class="upload-status">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>上传中...</span>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="修改学生信息" width="600px">
      <el-form :model="editForm" label-width="100px" class="edit-form">
        <el-form-item label="用户名" required>
          <el-input
            v-model="editForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input
            v-model="editForm.nickname"
            placeholder="请输入昵称"
            :prefix-icon="User"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="editForm.gender" placeholder="请选择性别" style="width: 100%">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="editForm.phone"
            placeholder="请输入手机号"
            :prefix-icon="Phone"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="editForm.email"
            placeholder="请输入邮箱"
            :prefix-icon="Message"
            type="email"
          />
        </el-form-item>
        <el-form-item label="头像">
          <div style="display: flex; align-items: center; gap: 16px">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="(options: any) => handleAvatarUpload(options.file)"
              accept="image/*"
            >
              <el-avatar
                v-if="avatarPreview || editForm.avatar"
                :src="avatarPreview || (editForm.avatar ? `/api${editForm.avatar}` : '')"
                :size="80"
                shape="square"
              />
              <el-avatar v-else :size="80" shape="square">
                {{ (editForm.nickname || editForm.username || 'U').slice(0, 1).toUpperCase() }}
              </el-avatar>
              <template #tip>
                <div class="el-upload__tip">点击头像上传，支持JPG/PNG格式，大小不超过5MB</div>
              </template>
            </el-upload>
            <el-button
              v-if="avatarPreview || editForm.avatar"
              link
              type="danger"
              @click="editForm.avatar = ''; avatarPreview = ''"
            >
              清除头像
            </el-button>
          </div>
          <div v-if="avatarUploading" style="margin-top: 8px; color: #409eff">
            <el-icon class="is-loading"><Loading /></el-icon>
            上传中...
          </div>
        </el-form-item>
        <el-divider />
        <el-form-item label="重置密码">
          <el-input
            v-model="editForm.password"
            type="password"
            placeholder="留空则不修改密码，填写则重置密码（至少6位）"
            :prefix-icon="Lock"
            show-password
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="确认密码" v-if="editForm.password">
          <el-input
            v-model="editForm.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :prefix-icon="Lock"
            show-password
            maxlength="50"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-students-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.students-card {
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

.add-student-btn {
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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  padding: 8px 0;
}

.edit-form,
.add-student-form {
  padding: 8px 0;
}

.add-student-form {
  padding: 0;
}

.add-student-form .el-form-item {
  margin-bottom: 22px;
}

.add-student-form .el-form-item:last-child {
  margin-bottom: 0;
}

.avatar-upload-section {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.avatar-uploader {
  flex-shrink: 0;
}

.avatar-preview,
.avatar-placeholder {
  cursor: pointer;
  transition: opacity 0.3s;
  border: 1px solid #dcdfe6;
}

.avatar-preview:hover,
.avatar-placeholder:hover {
  opacity: 0.8;
  border-color: #409eff;
}

.avatar-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
}

.avatar-tip {
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}

.upload-status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-size: 14px;
}

.empty-students {
  padding: 40px;
  text-align: center;
}

.students-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 4px;
}

.select-all-checkbox {
  flex-shrink: 0;
}

.list-header-text {
  font-size: 14px;
  color: #6b7280;
}

.student-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.student-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.student-checkbox {
  flex-shrink: 0;
}

.student-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(240, 147, 251, 0.25);
}

.student-icon {
  font-size: 24px;
  color: #ffffff;
}

.student-info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.student-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.student-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.student-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.student-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.student-actions :deep(.el-button) {
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

.student-actions :deep(.el-button__icon) {
  margin-right: 4px;
  display: inline-flex;
  align-items: center;
}

.student-time {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
  min-width: 160px;
  text-align: right;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 12px;
  }

  .search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar > * {
    width: 100%;
  }

  .student-item {
    flex-wrap: wrap;
  }

  .student-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }

  .student-time {
    min-width: auto;
    text-align: left;
  }
}
</style>
