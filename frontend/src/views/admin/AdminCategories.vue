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
  NInputNumber,
} from 'naive-ui'
import { AddOutline, CreateOutline, TrashOutline, FolderOutline } from '@vicons/ionicons5'
import request from '../../api/request'
import type { Category } from '../../api/category'
import PageHeader from '../../components/PageHeader.vue'
import { message, dialog } from '../../utils/feedback'

const list = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<Category | null>(null)
const sortProp = ref<string>('id')
const sortOrder = ref<'ascending' | 'descending' | null>('ascending')
const form = reactive({
  name: '',
  sortOrder: 1,
})
const selectedCategories = ref<number[]>([])

async function load() {
  loading.value = true
  try {
    const res = await request.get<{ success: boolean; data: Category[] }>('/categories')
    list.value = res.data
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载分类列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.sortOrder = 1
  dialogVisible.value = true
}

function openEdit(row: Category) {
  editing.value = row
  form.name = row.name
  form.sortOrder = row.sortOrder || 1
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name) {
    message.warning('请输入分类名称')
    return
  }
  try {
    if (editing.value) {
      await request.put(`/categories/${editing.value.id}`, form)
      message.success('修改成功')
    } else {
      await request.post('/categories', form)
      message.success('创建成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  }
}

function handleDelete(row: Category) {
  dialog.warning({
    title: '删除确认',
    content: `确定要删除分类"${row.name}"吗？删除后无法恢复。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.delete(`/categories/${row.id}`)
        message.success('删除成功')
        load()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    },
  })
}

const sortedList = computed(() => {
  const arr = [...list.value]
  if (!sortProp.value || !sortOrder.value) return arr
  const factor = sortOrder.value === 'ascending' ? 1 : -1
  const prop = sortProp.value as keyof Category
  return arr.sort((a, b) => {
    const va = a[prop]
    const vb = b[prop]
    if (typeof va === 'number' && typeof vb === 'number') {
      return (va - vb) * factor
    }
    return String(va ?? '').localeCompare(String(vb ?? '')) * factor
  })
})

function toggleSelect(id: number) {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) selectedCategories.value.splice(index, 1)
  else selectedCategories.value.push(id)
}

function toggleSelectAll() {
  if (selectedCategories.value.length === list.value.length) {
    selectedCategories.value = []
  } else {
    selectedCategories.value = list.value.map((c) => c.id)
  }
}

function handleBatchDelete() {
  if (selectedCategories.value.length === 0) {
    message.warning('请选择要删除的分类')
    return
  }
  dialog.warning({
    title: '删除确认',
    content: `确定要删除选中的 ${selectedCategories.value.length} 个分类吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        for (const id of selectedCategories.value) {
          await request.delete(`/categories/${id}`)
        }
        message.success('删除成功')
        selectedCategories.value = []
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
    <PageHeader title="分类管理" subtitle="管理平台所有资料分类" :show-back="false">
      <template #extra>
        <NButton type="primary" @click="openCreate">
          <template #icon>
            <NIcon :component="AddOutline" />
          </template>
          新增分类
        </NButton>
      </template>
    </PageHeader>

    <div class="glass-panel toolbar">
      <NButton
        v-if="selectedCategories.length > 0"
        type="error"
        secondary
        @click="handleBatchDelete"
      >
        批量删除 ({{ selectedCategories.length }})
      </NButton>
      <span v-else class="muted">选择分类后可批量删除</span>
    </div>

    <div class="glass-panel table-wrap">
      <NSpin :show="loading">
        <div v-if="list.length === 0" class="empty-wrap">
          <NEmpty description="暂无分类" />
        </div>
        <div v-else class="item-list">
          <div class="list-header">
            <NCheckbox
              :checked="selectedCategories.length === list.length && list.length > 0"
              :indeterminate="
                selectedCategories.length > 0 && selectedCategories.length < list.length
              "
              @update:checked="toggleSelectAll"
            />
            <span class="muted">全选</span>
          </div>
          <div v-for="category in sortedList" :key="category.id" class="surface-card list-item">
            <NCheckbox
              :checked="selectedCategories.includes(category.id)"
              @update:checked="() => toggleSelect(category.id)"
            />
            <div class="icon-wrap">
              <NIcon :component="FolderOutline" :size="24" />
            </div>
            <div class="item-info">
              <div class="item-title">{{ category.name }}</div>
              <div class="item-meta muted">
                <span>排序：{{ category.sortOrder || 1 }}</span>
              </div>
            </div>
            <div class="item-right">
              <NSpace :size="8">
                <NButton size="small" quaternary @click="openEdit(category)">
                  <template #icon>
                    <NIcon :component="CreateOutline" />
                  </template>
                  编辑
                </NButton>
                <NButton size="small" quaternary type="error" @click="handleDelete(category)">
                  <template #icon>
                    <NIcon :component="TrashOutline" />
                  </template>
                  删除
                </NButton>
              </NSpace>
            </div>
          </div>
        </div>
      </NSpin>
    </div>

    <NModal
      v-model:show="dialogVisible"
      preset="card"
      :title="editing ? '编辑分类' : '新增分类'"
      style="width: 420px; max-width: 94vw"
      :bordered="false"
    >
      <NForm label-placement="left" label-width="80">
        <NFormItem label="名称">
          <NInput v-model:value="form.name" placeholder="请输入分类名称" />
        </NFormItem>
        <NFormItem label="排序">
          <NInputNumber v-model:value="form.sortOrder" :min="1" style="width: 100%" />
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
  background: linear-gradient(145deg, var(--m-peach), var(--m-sage));
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
  font-size: 16px;
  font-weight: 600;
  color: var(--m-ink);
  margin-bottom: 4px;
}

.item-meta {
  font-size: 13px;
}

.item-right {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .list-item {
    flex-wrap: wrap;
  }

  .item-right {
    width: 100%;
    margin-top: 4px;
  }
}
</style>
