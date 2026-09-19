<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import {
  NButton,
  NInput,
  NModal,
  NForm,
  NFormItem,
  NSpace,
  NEmpty,
  NSpin,
  NCheckbox,
  NIcon,
  NSwitch,
  NTag,
} from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline, NotificationsOutline } from '@vicons/ionicons5'
import request from '../../api/request'
import PageHeader from '../../components/PageHeader.vue'
import { message, dialog } from '../../utils/feedback'

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
    return String(va ?? '').localeCompare(String(vb ?? '')) * factor
  })
})

async function load() {
  loading.value = true
  try {
    const res = await request.get<{ success: boolean; data: Announcement[] }>('/announcements')
    list.value = res.data
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载公告列表失败')
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
    message.warning('请填写标题和简介')
    return
  }
  try {
    if (editing.value) {
      await request.put(`/announcements/${editing.value.id}`, form)
      message.success('修改成功')
    } else {
      await request.post('/announcements', form)
      message.success('创建成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  }
}

function handleDelete(row: Announcement) {
  dialog.warning({
    title: '删除确认',
    content: `确定要删除公告"${row.title}"吗？删除后无法恢复。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/announcements/${row.id}`)
        message.success('删除成功')
        load()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    },
  })
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const selectedAnnouncements = ref<number[]>([])

function toggleSelect(id: number) {
  const index = selectedAnnouncements.value.indexOf(id)
  if (index > -1) selectedAnnouncements.value.splice(index, 1)
  else selectedAnnouncements.value.push(id)
}

function toggleSelectAll() {
  if (selectedAnnouncements.value.length === list.value.length) {
    selectedAnnouncements.value = []
  } else {
    selectedAnnouncements.value = list.value.map((a) => a.id)
  }
}

function handleBatchDelete() {
  if (selectedAnnouncements.value.length === 0) {
    message.warning('请选择要删除的公告')
    return
  }
  dialog.warning({
    title: '删除确认',
    content: `确定要删除选中的 ${selectedAnnouncements.value.length} 个公告吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        for (const id of selectedAnnouncements.value) {
          await request.delete(`/announcements/${id}`)
        }
        message.success('删除成功')
        selectedAnnouncements.value = []
        load()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    },
  })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="系统公告" subtitle="管理平台所有系统公告" :show-back="false">
      <template #extra>
        <NButton type="primary" @click="openCreate">
          <template #icon>
            <NIcon :component="AddOutline" />
          </template>
          发布公告
        </NButton>
      </template>
    </PageHeader>

    <div class="glass-panel toolbar">
      <NButton
        v-if="selectedAnnouncements.length > 0"
        type="error"
        secondary
        @click="handleBatchDelete"
      >
        批量删除 ({{ selectedAnnouncements.length }})
      </NButton>
      <span v-else class="muted">选择公告后可批量删除</span>
    </div>

    <div class="glass-panel table-wrap">
      <NSpin :show="loading">
        <div v-if="list.length === 0" class="empty-wrap">
          <NEmpty description="暂无公告" />
        </div>
        <div v-else class="item-list">
          <div class="list-header">
            <NCheckbox
              :checked="selectedAnnouncements.length === list.length && list.length > 0"
              :indeterminate="
                selectedAnnouncements.length > 0 && selectedAnnouncements.length < list.length
              "
              @update:checked="toggleSelectAll"
            />
            <span class="muted">全选</span>
          </div>
          <div
            v-for="announcement in sortedList"
            :key="announcement.id"
            class="surface-card list-item"
          >
            <NCheckbox
              :checked="selectedAnnouncements.includes(announcement.id)"
              @update:checked="() => toggleSelect(announcement.id)"
            />
            <div class="icon-wrap">
              <NIcon :component="NotificationsOutline" :size="24" />
            </div>
            <div class="item-info">
              <div class="item-title">
                {{ announcement.title }}
                <NTag v-if="announcement.pinned" type="warning" size="small" :bordered="false">
                  置顶
                </NTag>
              </div>
              <div class="item-meta muted">
                <span>简介：{{ announcement.summary || '-' }}</span>
              </div>
            </div>
            <div class="item-right">
              <NSpace :size="8">
                <NButton size="small" quaternary @click="openEdit(announcement)">
                  <template #icon>
                    <NIcon :component="CreateOutline" />
                  </template>
                  编辑
                </NButton>
                <NButton size="small" quaternary type="error" @click="handleDelete(announcement)">
                  <template #icon>
                    <NIcon :component="TrashOutline" />
                  </template>
                  删除
                </NButton>
              </NSpace>
              <div class="item-time muted">{{ formatDate(announcement.publishAt) }}</div>
            </div>
          </div>
        </div>
      </NSpin>
    </div>

    <NModal
      v-model:show="dialogVisible"
      preset="card"
      :title="editing ? '编辑公告' : '发布公告'"
      style="width: 560px; max-width: 94vw"
      :bordered="false"
    >
      <NForm label-placement="left" label-width="80">
        <NFormItem label="标题">
          <NInput v-model:value="form.title" placeholder="请输入标题" />
        </NFormItem>
        <NFormItem label="简介">
          <NInput v-model:value="form.summary" placeholder="请输入简介" />
        </NFormItem>
        <NFormItem label="内容">
          <NInput v-model:value="form.content" type="textarea" :rows="6" placeholder="请输入内容" />
        </NFormItem>
        <NFormItem label="置顶">
          <NSwitch v-model:value="form.pinned" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="dialogVisible = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">保存</NButton>
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
  min-height: 56px;
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
  background: linear-gradient(145deg, var(--m-sage-wash), var(--m-sage));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
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

@media (max-width: 768px) {
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


