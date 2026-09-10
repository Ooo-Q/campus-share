<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Folder } from '@element-plus/icons-vue'
import request from '../../api/request'
import type { Category } from '../../api/category'

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
    ElMessage.error(e?.response?.data?.message || '加载分类列表失败')
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
    ElMessage.warning('请输入分类名称')
    return
  }
  if (editing.value) {
    await request.put(`/categories/${editing.value.id}`, form)
    ElMessage.success('修改成功')
  } else {
    await request.post('/categories', form)
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(row: Category) {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类"${row.name}"吗？删除后无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      }
    )
    await request.delete(`/categories/${row.id}`)
    ElMessage.success('删除成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }
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
    const sa = String(va ?? '')
    const sb = String(vb ?? '')
    return sa.localeCompare(sb) * factor
  })
})

function toggleSelect(id: number) {
  const index = selectedCategories.value.indexOf(id)
  if (index > -1) {
    selectedCategories.value.splice(index, 1)
  } else {
    selectedCategories.value.push(id)
  }
}

function toggleSelectAll() {
  if (selectedCategories.value.length === list.value.length) {
    selectedCategories.value = []
  } else {
    selectedCategories.value = list.value.map(c => c.id)
  }
}

async function handleBatchDelete() {
  if (selectedCategories.value.length === 0) {
    ElMessage.warning('请选择要删除的分类')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedCategories.value.length} 个分类吗？`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      }
    )
    for (const id of selectedCategories.value) {
      await request.delete(`/categories/${id}`)
    }
    ElMessage.success('删除成功')
    selectedCategories.value = []
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
  <div class="admin-categories-page">
    <el-card class="categories-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>分类管理</h3>
            <p class="card-subtitle">管理平台所有资料分类</p>
          </div>
          <div class="search-bar">
            <el-button
              v-if="selectedCategories.length > 0"
              type="danger"
              @click="handleBatchDelete"
            >
              批量删除 ({{ selectedCategories.length }})
            </el-button>
            <el-button type="primary" :icon="Plus" @click="openCreate" class="add-category-btn">新增分类</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="list.length === 0" class="empty-categories">
          <el-empty description="暂无分类" />
        </div>
        <div v-else class="categories-list">
          <div class="list-header">
            <el-checkbox
              :model-value="selectedCategories.length === list.length && list.length > 0"
              @change="toggleSelectAll"
              class="select-all-checkbox"
            />
            <span class="list-header-text">全选</span>
          </div>
          <div v-for="category in sortedList" :key="category.id" class="category-item">
            <el-checkbox
              :model-value="selectedCategories.includes(category.id)"
              @change="toggleSelect(category.id)"
              class="category-checkbox"
            />
            <div class="category-icon-wrapper">
              <el-icon class="category-icon"><Folder /></el-icon>
            </div>
            <div class="category-info">
              <div class="category-title">
                {{ category.name }}
              </div>
              <div class="category-meta">
                <span>排序：{{ category.sortOrder || 1 }}</span>
              </div>
            </div>
            <div class="category-right">
              <div class="category-actions">
                <el-button
                  size="small"
                  text
                  type="info"
                  :icon="Edit"
                  @click.stop="openEdit(category)"
                >
                  编辑
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleDelete(category)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新增分类'" width="400px">
      <el-form label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="1" />
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
.admin-categories-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.categories-card {
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

.add-category-btn {
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

.empty-categories {
  padding: 40px;
  text-align: center;
}

.categories-list {
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

.category-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.category-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.category-checkbox {
  flex-shrink: 0;
}

.category-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(250, 112, 154, 0.25);
}

.category-icon {
  font-size: 24px;
  color: #ffffff;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.category-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.category-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.category-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.category-actions :deep(.el-button) {
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

.category-actions :deep(.el-button__icon) {
  margin-right: 4px;
  display: inline-flex;
  align-items: center;
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

  .category-item {
    flex-wrap: wrap;
  }

  .category-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }
}
</style>


