<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Bell } from '@element-plus/icons-vue'
import request from '../../api/request'

interface Announcement {
  id: number
  title: string
  summary: string
  content: string
  pinned: boolean
  publishAt: string
}

const list = ref<Announcement[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<Announcement | null>(null)
const sortProp = ref<string>('publishAt')
const sortOrder = ref<'ascending' | 'descending' | null>('descending')
const form = reactive({
  title: '',
  summary: '',
  content: '',
  pinned: false,
})

const sortedList = computed(() => {
  const arr = [...list.value]
  if (!sortProp.value || !sortOrder.value) return arr
  const factor = sortOrder.value === 'ascending' ? 1 : -1
  return arr.sort((a, b) => {
    const prop = sortProp.value as keyof Announcement
    const va = a[prop]
    const vb = b[prop]
    if (prop === 'publishAt') {
      const ta = va ? new Date(va as string).getTime() : 0
      const tb = vb ? new Date(vb as string).getTime() : 0
      return (ta - tb) * factor
    }
    if (prop === 'pinned') {
      const ta = (va as boolean) ? 1 : 0
      const tb = (vb as boolean) ? 1 : 0
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

async function load() {
  loading.value = true
  try {
    const res = await request.get<{ success: boolean; data: Announcement[] }>('/announcements')
    list.value = res.data
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载公告列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { title: '', summary: '', content: '', pinned: false })
  dialogVisible.value = true
}

function openEdit(row: Announcement) {
  editing.value = row
  Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.title || !form.summary) {
    ElMessage.warning('请填写标题和简介')
    return
  }
  if (editing.value) {
    await request.put(`/announcements/${editing.value.id}`, form)
    ElMessage.success('修改成功')
  } else {
    await request.post('/announcements', form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(row: Announcement) {
  try {
    await ElMessageBox.confirm(
      `确定要删除公告"${row.title}"吗？删除后无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      }
    )
    await request.delete(`/announcements/${row.id}`)
    ElMessage.success('删除成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const selectedAnnouncements = ref<number[]>([])

function toggleSelect(id: number) {
  const index = selectedAnnouncements.value.indexOf(id)
  if (index > -1) {
    selectedAnnouncements.value.splice(index, 1)
  } else {
    selectedAnnouncements.value.push(id)
  }
}

function toggleSelectAll() {
  if (selectedAnnouncements.value.length === list.value.length) {
    selectedAnnouncements.value = []
  } else {
    selectedAnnouncements.value = list.value.map(a => a.id)
  }
}

async function handleBatchDelete() {
  if (selectedAnnouncements.value.length === 0) {
    ElMessage.warning('请选择要删除的公告')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedAnnouncements.value.length} 个公告吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      }
    )
    for (const id of selectedAnnouncements.value) {
      await request.delete(`/announcements/${id}`)
    }
    ElMessage.success('删除成功')
    selectedAnnouncements.value = []
    load()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-announcements-page">
    <el-card class="announcements-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>系统公告</h3>
            <p class="card-subtitle">管理平台所有系统公告</p>
          </div>
          <div class="search-bar">
            <el-button
              v-if="selectedAnnouncements.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedAnnouncements.length }})
            </el-button>
            <el-button type="primary" :icon="Plus" @click="openCreate" class="add-announcement-btn">发布公告</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="list.length === 0" class="empty-announcements">
          <el-empty description="暂无公告" />
        </div>
        <div v-else class="announcements-list">
          <div class="list-header">
            <el-checkbox
              :model-value="selectedAnnouncements.length === list.length && list.length > 0"
              @change="toggleSelectAll"
              class="select-all-checkbox"
            />
            <span class="list-header-text">全选</span>
          </div>
          <div v-for="announcement in sortedList" :key="announcement.id" class="announcement-item">
            <el-checkbox
              :model-value="selectedAnnouncements.includes(announcement.id)"
              @change="toggleSelect(announcement.id)"
              class="announcement-checkbox"
            />
            <div class="announcement-icon-wrapper">
              <el-icon class="announcement-icon"><Bell /></el-icon>
            </div>
            <div class="announcement-info">
              <div class="announcement-title">
                {{ announcement.title }}
                <el-tag v-if="announcement.pinned" type="warning" size="small" style="margin-left: 8px">置顶</el-tag>
              </div>
              <div class="announcement-meta">
                <span>简介：{{ announcement.summary || '-' }}</span>
              </div>
            </div>
            <div class="announcement-right">
              <div class="announcement-actions">
                <el-button
                  size="small"
                  text
                  type="info"
                  :icon="Edit"
                  @click.stop="openEdit(announcement)"
                >
                  编辑
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleDelete(announcement)"
                >
                  删除
                </el-button>
              </div>
              <div class="announcement-time">{{ formatDate(announcement.publishAt) }}</div>
            </div>
          </div>
        </div>
      </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑公告' : '发布公告'" width="560px">
      <el-form label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.summary" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
    </el-card>
  </div>
</template>

<style scoped>
.admin-announcements-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.announcements-card {
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

.add-announcement-btn {
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

.empty-announcements {
  padding: 40px;
  text-align: center;
}

.announcements-list {
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

.announcement-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.announcement-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.announcement-checkbox {
  flex-shrink: 0;
}

.announcement-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(168, 237, 234, 0.25);
}

.announcement-icon {
  font-size: 24px;
  color: #ffffff;
}

.announcement-info {
  flex: 1;
  min-width: 0;
}

.announcement-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.announcement-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.announcement-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.announcement-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.announcement-actions :deep(.el-button) {
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

.announcement-actions :deep(.el-button__icon) {
  margin-right: 4px;
  display: inline-flex;
  align-items: center;
}

.announcement-time {
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

  .announcement-item {
    flex-wrap: wrap;
  }

  .announcement-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }

  .announcement-time {
    min-width: auto;
    text-align: left;
  }
}
</style>


