<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
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
  NAvatar,
  NUpload,
  NIcon,
  NDivider,
  type UploadCustomRequestOptions,
} from 'naive-ui'
import {
  CreateOutline,
  TrashOutline,
  PersonOutline,
  AddOutline,
  PersonCircleOutline,
} from '@vicons/ionicons5'
import request from '../../api/request'
import { uploadAvatar } from '../../api/auth'
import type { PageResponse } from '../../api/resource'
import PageHeader from '../../components/PageHeader.vue'
import { message, dialog } from '../../utils/feedback'

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
const selectedStudentIds = computed(() => selectedRows.value.map((s) => s.id))
const sortProp = ref<string>('createdAt')
const sortOrder = ref<'ascending' | 'descending' | null>('descending')

const genderOptions = [
  { label: '男', value: 'MALE' },
  { label: '女', value: 'FEMALE' },
  { label: '其他', value: 'OTHER' },
]

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
      message.success('头像上传成功')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '头像上传失败')
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
      message.success('头像上传成功')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '头像上传失败')
  } finally {
    addAvatarUploading.value = false
  }
}

function beforeAvatarUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    message.error('图片大小不能超过5MB!')
    return false
  }
  return true
}

function addAvatarRequest({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const raw = file.file as File
  if (!beforeAvatarUpload(raw)) {
    onError()
    return
  }
  handleAddAvatarUpload(raw).then(onFinish).catch(onError)
}

function editAvatarRequest({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const raw = file.file as File
  if (!beforeAvatarUpload(raw)) {
    onError()
    return
  }
  handleAvatarUpload(raw).then(onFinish).catch(onError)
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
  } catch {
    message.error('加载学生列表失败')
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
    return String(va ?? '').localeCompare(String(vb ?? '')) * factor
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
  const student = tableData.value.find((s) => s.id === studentId)
  if (!student) return
  const index = selectedRows.value.findIndex((s) => s.id === studentId)
  if (index > -1) selectedRows.value.splice(index, 1)
  else selectedRows.value.push(student)
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

function clearAddAvatar() {
  addForm.avatar = ''
  addAvatarPreview.value = ''
}

function clearEditAvatar() {
  editForm.avatar = ''
  avatarPreview.value = ''
}

async function handleCreate() {
  if (!addForm.username) {
    message.warning('请输入用户名')
    return
  }
  if (!addForm.nickname) {
    message.warning('请输入昵称')
    return
  }
  if (!addForm.password) {
    message.warning('请输入密码')
    return
  }
  if (addForm.password.length < 6) {
    message.warning('密码长度至少6位')
    return
  }
  if (addForm.password !== addForm.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }

  try {
    await request.post('/admin/students', {
      username: addForm.username,
      password: addForm.password,
      nickname: addForm.nickname,
      gender: addForm.gender || null,
      phone: addForm.phone || null,
      email: addForm.email || null,
      avatar: addForm.avatar || null,
    })
    message.success('添加成功')
    addVisible.value = false
    loadData()
  } catch (error: any) {
    message.error(error.response?.data?.message || '添加失败')
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
    message.warning('请输入用户名')
    return
  }
  if (!editForm.nickname) {
    message.warning('请输入昵称')
    return
  }
  if (editForm.password) {
    if (editForm.password.length < 6) {
      message.warning('密码长度至少6位')
      return
    }
    if (editForm.password !== editForm.confirmPassword) {
      message.warning('两次输入的密码不一致')
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
    if (editForm.password) payload.password = editForm.password

    await request.put(`/admin/students/${editForm.id}`, payload)
    message.success('修改成功')
    editVisible.value = false
    loadData()
  } catch (error: any) {
    message.error(error.response?.data?.message || '修改失败')
  }
}

function handleDelete(row: Student) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除学生 "${row.nickname || row.username}" 吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/admin/students/${row.id}`)
        message.success('删除成功')
        loadData()
      } catch (error: any) {
        message.error(error.response?.data?.message || '删除失败')
      }
    },
  })
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    message.warning('请选择要删除的学生')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 个学生吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const ids = selectedRows.value.map((row) => row.id)
        await request.delete('/admin/students/batch', { data: { ids } })
        message.success('删除成功')
        selectedRows.value = []
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

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page">
    <PageHeader title="学生管理" subtitle="管理所有注册学生信息" :show-back="false">
      <template #extra>
        <NButton type="primary" @click="handleAdd">
          <template #icon>
            <NIcon :component="AddOutline" />
          </template>
          添加学生
        </NButton>
      </template>
    </PageHeader>

    <div class="glass-panel toolbar">
      <NInput
        v-model:value="keyword"
        placeholder="搜索用户名、昵称"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <NButton type="primary" @click="handleSearch">搜索</NButton>
      <NButton v-if="selectedRows.length > 0" type="error" secondary @click="handleBatchDelete">
        批量删除 ({{ selectedRows.length }})
      </NButton>
    </div>

    <div class="glass-panel table-wrap">
      <NSpin :show="loading">
        <div v-if="tableData.length === 0" class="empty-wrap">
          <NEmpty description="暂无学生" />
        </div>
        <div v-else class="item-list">
          <div class="list-header">
            <NCheckbox
              :checked="selectedRows.length === tableData.length && tableData.length > 0"
              :indeterminate="selectedRows.length > 0 && selectedRows.length < tableData.length"
              @update:checked="toggleSelectAll"
            />
            <span class="muted">全选</span>
          </div>
          <div v-for="student in sortedTableData" :key="student.id" class="surface-card list-item">
            <NCheckbox
              :checked="selectedStudentIds.includes(student.id)"
              @update:checked="() => toggleSelect(student.id)"
            />
            <div class="icon-wrap">
              <NIcon :component="PersonCircleOutline" :size="24" />
            </div>
            <div class="item-info" @click="handleView(student)">
              <div class="item-title">
                {{ student.nickname || student.username }}
                <NTag v-if="student.gender === 'MALE'" size="small" :bordered="false">男</NTag>
                <NTag v-else-if="student.gender === 'FEMALE'" size="small" :bordered="false">女</NTag>
              </div>
              <div class="item-meta muted">
                <span>用户名：{{ student.username }}</span>
                <span v-if="student.phone">手机：{{ student.phone }}</span>
                <span v-if="student.email">邮箱：{{ student.email }}</span>
              </div>
            </div>
            <div class="item-right">
              <NSpace :size="8">
                <NButton size="small" quaternary @click="handleEdit(student)">
                  <template #icon>
                    <NIcon :component="CreateOutline" />
                  </template>
                  修改
                </NButton>
                <NButton size="small" quaternary type="error" @click="handleDelete(student)">
                  <template #icon>
                    <NIcon :component="TrashOutline" />
                  </template>
                  删除
                </NButton>
              </NSpace>
              <div class="item-time muted">{{ formatDate(student.createdAt) }}</div>
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
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handleSizeChange"
        />
      </div>
    </div>

    <NModal
      v-model:show="addVisible"
      preset="card"
      title="添加学生"
      style="width: 600px; max-width: 94vw"
      :bordered="false"
      class="glass-modal"
    >
      <NForm label-placement="left" label-width="100">
        <NFormItem label="用户名" required>
          <NInput v-model:value="addForm.username" placeholder="请输入用户名" maxlength="50" clearable>
            <template #prefix>
              <NIcon :component="PersonOutline" />
            </template>
          </NInput>
        </NFormItem>
        <NFormItem label="昵称" required>
          <NInput v-model:value="addForm.nickname" placeholder="请输入昵称" maxlength="50" clearable />
        </NFormItem>
        <NFormItem label="密码" required>
          <NInput
            v-model:value="addForm.password"
            type="password"
            show-password-on="click"
            placeholder="请输入密码（至少6位）"
            maxlength="50"
          />
        </NFormItem>
        <NFormItem label="确认密码" required>
          <NInput
            v-model:value="addForm.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="请再次输入密码"
            maxlength="50"
          />
        </NFormItem>
        <NFormItem label="性别">
          <NSelect
            v-model:value="addForm.gender"
            :options="genderOptions"
            placeholder="请选择性别"
            clearable
          />
        </NFormItem>
        <NFormItem label="手机号">
          <NInput v-model:value="addForm.phone" placeholder="请输入手机号" maxlength="11" clearable />
        </NFormItem>
        <NFormItem label="邮箱">
          <NInput v-model:value="addForm.email" placeholder="请输入邮箱" clearable />
        </NFormItem>
        <NFormItem label="头像">
          <div class="avatar-row">
            <NUpload :show-file-list="false" accept="image/*" :custom-request="addAvatarRequest">
              <NAvatar
                :size="80"
                :src="addAvatarPreview || (addForm.avatar ? `/api${addForm.avatar}` : undefined)"
                round
              >
                {{ (addForm.nickname || addForm.username || 'U').slice(0, 1).toUpperCase() }}
              </NAvatar>
            </NUpload>
            <div class="avatar-meta">
              <p class="muted">点击头像上传，支持 JPG/PNG，不超过 5MB</p>
              <NButton
                v-if="addAvatarPreview || addForm.avatar"
                text
                type="error"
                size="small"
                @click="clearAddAvatar"
              >
                清除头像
              </NButton>
              <span v-if="addAvatarUploading" class="muted">上传中...</span>
            </div>
          </div>
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="addVisible = false">取消</NButton>
          <NButton type="primary" @click="handleCreate">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal
      v-model:show="editVisible"
      preset="card"
      title="修改学生信息"
      style="width: 600px; max-width: 94vw"
      :bordered="false"
    >
      <NForm label-placement="left" label-width="100">
        <NFormItem label="用户名" required>
          <NInput v-model:value="editForm.username" placeholder="请输入用户名" maxlength="50" />
        </NFormItem>
        <NFormItem label="昵称" required>
          <NInput v-model:value="editForm.nickname" placeholder="请输入昵称" maxlength="50" />
        </NFormItem>
        <NFormItem label="性别">
          <NSelect v-model:value="editForm.gender" :options="genderOptions" placeholder="请选择性别" clearable />
        </NFormItem>
        <NFormItem label="手机号">
          <NInput v-model:value="editForm.phone" placeholder="请输入手机号" maxlength="11" />
        </NFormItem>
        <NFormItem label="邮箱">
          <NInput v-model:value="editForm.email" placeholder="请输入邮箱" />
        </NFormItem>
        <NFormItem label="头像">
          <div class="avatar-row">
            <NUpload :show-file-list="false" accept="image/*" :custom-request="editAvatarRequest">
              <NAvatar
                :size="80"
                :src="avatarPreview || (editForm.avatar ? `/api${editForm.avatar}` : undefined)"
                round
              >
                {{ (editForm.nickname || editForm.username || 'U').slice(0, 1).toUpperCase() }}
              </NAvatar>
            </NUpload>
            <div class="avatar-meta">
              <p class="muted">点击头像上传，支持 JPG/PNG，不超过 5MB</p>
              <NButton
                v-if="avatarPreview || editForm.avatar"
                text
                type="error"
                size="small"
                @click="clearEditAvatar"
              >
                清除头像
              </NButton>
              <span v-if="avatarUploading" class="muted">上传中...</span>
            </div>
          </div>
        </NFormItem>
        <NDivider />
        <NFormItem label="重置密码">
          <NInput
            v-model:value="editForm.password"
            type="password"
            show-password-on="click"
            placeholder="留空则不修改密码（至少6位）"
            maxlength="50"
          />
        </NFormItem>
        <NFormItem v-if="editForm.password" label="确认密码">
          <NInput
            v-model:value="editForm.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="请再次输入密码"
            maxlength="50"
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="editVisible = false">取消</NButton>
          <NButton type="primary" @click="handleUpdate">确定</NButton>
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

.list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--m-radius-sm);
  background: rgba(255, 255, 255, 0.35);
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
  background: linear-gradient(145deg, var(--m-sage-soft), var(--m-sage));
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
  gap: 14px;
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

.avatar-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 4px;
}

.avatar-meta p {
  margin: 0;
  font-size: 13px;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .list-item {
    flex-wrap: wrap;
  }

  .item-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 4px;
  }

  .item-time {
    min-width: auto;
    text-align: left;
  }
}
</style>
