<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { View, Download, Star, Document, Search } from '@element-plus/icons-vue'
import { fetchResources, type Resource } from '../api/resource'
import { fetchCategories, type Category } from '../api/category'
import { useRouter } from 'vue-router'
import PageHeader from '../components/PageHeader.vue'

const router = useRouter()
const loading = ref(false)
const tableData = ref<Resource[]>([])
const sortProp = ref<string>('createdAt')
const sortOrder = ref<'ascending' | 'descending' | null>('descending')
const total = ref(0)
const page = ref(1)
const size = ref(12)
const keyword = ref('')
const categoryId = ref<number | undefined>()
const categories = ref<Category[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await fetchResources({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value,
    })
    const pageData = res.data
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch (e) {
    ElMessage.error('加载资料失败')
  } finally {
    loading.value = false
  }
}

const sortedResources = computed(() => {
  const arr = [...tableData.value]
  if (!sortProp.value || !sortOrder.value) return arr
  const factor = sortOrder.value === 'ascending' ? 1 : -1
  const prop = sortProp.value as keyof Resource
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

async function loadCategories() {
  const res = await fetchCategories()
  categories.value = res.data || []
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

async function handleView(resource: Resource) {
  if (!resource?.id) {
    ElMessage.warning('未找到资源')
    return
  }
  router.push({ name: 'StudentResourceDetail', params: { id: resource.id }, query: { from: 'resources' } })
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

function getFileType(fileUrl: string) {
  if (!fileUrl) return 'file'
  const ext = fileUrl.split('.').pop()?.toLowerCase() || ''
  const typeMap: Record<string, string> = {
    pdf: 'PDF',
    doc: 'Word',
    docx: 'Word',
    xls: 'Excel',
    xlsx: 'Excel',
    ppt: 'PPT',
    pptx: 'PPT',
    txt: '文本',
    zip: '压缩',
    rar: '压缩',
    jpg: '图片',
    jpeg: '图片',
    png: '图片',
    gif: '图片',
  }
  return typeMap[ext] || '文件'
}

onMounted(() => {
  loadCategories()
  loadData()
})

watch(size, () => {
  page.value = 1
  loadData()
})
</script>

<template>
  <div class="resource-list">
    <div class="page-header-with-toolbar">
      <PageHeader title="资料广场" />
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select
            v-model="categoryId"
            placeholder="全部分类"
            clearable
            class="category-select"
            size="default"
            @change="loadData"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="搜索资料标题或简介..."
            class="search-input"
            size="default"
            clearable
            @keyup.enter="loadData"
          >
            <template #prefix>
              <el-icon><Document /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" :icon="Search" @click="loadData" class="search-btn" size="default">搜索</el-button>
        </div>
      </div>
    </div>

    <div class="resource-grid" v-loading="loading">
      <div
        v-for="item in sortedResources"
        :key="item.id"
        class="resource-card"
        @click="handleView(item)"
      >
        <div class="card-header">
          <div class="card-icon-wrapper">
            <el-icon class="card-icon"><Document /></el-icon>
          </div>
          <div class="card-badges">
            <el-tag v-if="item.categoryName" size="small" type="primary" effect="dark">{{ item.categoryName }}</el-tag>
            <span class="file-type-badge">{{ getFileType(item.fileUrl) }}</span>
          </div>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ item.title }}</h3>
          <p class="card-description">{{ item.description || '暂无描述' }}</p>
        </div>
        <div class="card-stats">
          <div class="stat-item stat-view">
            <el-icon class="stat-icon"><View /></el-icon>
            <span>{{ item.viewCount || 0 }}</span>
          </div>
          <div class="stat-item stat-download">
            <el-icon class="stat-icon"><Download /></el-icon>
            <span>{{ item.downloadCount || 0 }}</span>
          </div>
          <div class="stat-item stat-favorite">
            <el-icon class="stat-icon"><Star /></el-icon>
            <span>{{ item.favoriteCount || 0 }}</span>
          </div>
          <div class="stat-item stat-like">
            <span class="heart-icon">♡</span>
            <span>{{ item.likeCount || 0 }}</span>
          </div>
        </div>
        <div class="card-footer">
          <div class="card-author-info">
            <el-avatar :size="24" class="author-avatar">
              {{ (item.ownerName || 'U').slice(0, 1).toUpperCase() }}
            </el-avatar>
            <span class="card-author">{{ item.ownerName }}</span>
          </div>
          <span class="card-date">{{ formatDate(item.createdAt) }}</span>
        </div>
      </div>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无资料" />
    </div>

    <div class="pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-size="size"
        :current-page="page"
        :total="total"
        :page-sizes="[12, 24, 48, 96]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.resource-list {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.page-header-with-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: clamp(20px, 4vw, 32px);
  padding-bottom: clamp(16px, 2.5vw, 20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.page-header-with-toolbar :deep(.page-header) {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 300px;
}

.search-input {
  flex: 1;
  max-width: 400px;
  height: 40px;
}

.search-input :deep(.el-input) {
  height: 40px !important;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  box-sizing: border-box;
}

.search-input :deep(.el-input__inner) {
  height: 38px !important;
  line-height: 38px;
}

.search-btn {
  border-radius: 10px;
  padding: 0 20px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  flex-shrink: 0;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.category-select {
  width: 160px;
  height: 40px;
}

.category-select :deep(.el-select) {
  height: 40px !important;
}

.category-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  min-height: 40px !important;
  max-height: 40px !important;
  box-sizing: border-box;
}

.category-select :deep(.el-input__inner) {
  height: 38px !important;
  line-height: 38px;
}

.category-select :deep(.el-select__wrapper) {
  border-radius: 10px;
  height: 40px !important;
  box-sizing: border-box;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(clamp(320px, 32vw, 400px), 1fr));
  gap: clamp(20px, 3vw, 32px);
  margin-bottom: clamp(24px, 4vw, 40px);
}

.resource-card {
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.resource-card:hover {
  border-color: rgba(64, 158, 255, 0.4);
  box-shadow: 0 8px 32px rgba(64, 158, 255, 0.15);
  transform: translateY(-4px);
}

.card-header {
  background: rgba(0, 0, 0, 0.02);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  position: relative;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.card-icon-wrapper {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.card-icon {
  color: #ffffff;
  font-size: 24px;
}

.card-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.file-type-badge {
  padding: 4px 12px;
  background: rgba(102, 126, 234, 0.15);
  color: #667eea;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.card-body {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  letter-spacing: -0.2px;
}

.card-description {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-stats {
  display: flex;
  gap: 0;
  padding: 0 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  position: relative;
}

.stat-item:not(:last-child)::after {
  content: '';
  position: absolute;
  right: 0;
  top: 20%;
  bottom: 20%;
  width: 1px;
  background: rgba(0, 0, 0, 0.06);
}

.stat-icon {
  color: #999;
  font-size: 18px;
}

.stat-item.stat-view .stat-icon {
  color: #409eff;
}

.stat-item.stat-download .stat-icon {
  color: #67c23a;
}

.stat-item.stat-favorite .stat-icon {
  color: #e6a23c;
}

.stat-item.stat-like .heart-icon {
  color: #f56c6c;
  font-size: 18px;
  line-height: 1;
}

.stat-item span:not(.heart-icon) {
  color: #666;
  font-size: 13px;
  font-weight: 500;
}

.heart-icon {
  font-size: 18px;
  line-height: 1;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.02);
}

.card-author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-weight: 600;
  font-size: 12px;
}

.card-author {
  color: #333;
  font-weight: 500;
  font-size: 13px;
}

.card-date {
  color: #999;
  font-size: 12px;
}

.card-actions {
  display: flex;
  gap: 6px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-start;
  white-space: nowrap;
}

.card-actions .el-button {
  flex: 1 1 0;
  min-width: 0;
  padding: 6px 8px;
}

.inline-actions {
  justify-content: flex-start;
}


.heart-symbol {
  color: #f56c6c;
  font-size: 14px;
  line-height: 1;
}

.heart-outline {
  color: #909399;
  font-size: 14px;
  line-height: 1;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding-left: 10px;
  padding-right: 10px;
}

.like-btn .like-label {
  min-width: 28px;
}

.like-btn.active {
  color: #f56c6c;
}

.favorite-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding-left: 10px;
  padding-right: 10px;
}

.favorite-btn .favorite-label {
  min-width: 28px;
}

.favorite-btn.active {
  color: #f59e0b;
}

.favorite-icon {
  font-size: 14px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.report-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-title {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

@media (max-width: 480px) {
  .resource-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .page-header-with-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .page-header-with-toolbar :deep(.page-header) {
    width: 100%;
  }
  
  .toolbar {
    width: 100%;
    justify-content: stretch;
  }
  
  .toolbar-left {
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }
  
  .search-input,
  .search-btn,
  .category-select {
    width: 100%;
    max-width: 100%;
  }
  
  .search-btn {
    height: 40px;
  }
  
  .resource-card {
    padding: 16px;
  }
  
  .card-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .card-actions {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .card-title {
    font-size: 16px;
  }
  
  .card-description {
    font-size: 13px;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .resource-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .page-header-with-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }
  
  .page-header-with-toolbar :deep(.page-header) {
    width: 100%;
  }
  
  .toolbar {
    width: 100%;
    justify-content: stretch;
  }
  
  .toolbar-left {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 12px;
    width: 100%;
  }
  
  .search-input {
    flex: 1;
    min-width: 200px;
  }
  
  .category-select {
    width: auto;
    min-width: 150px;
  }
  
  .card-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .card-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .resource-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
  }
}

@media (min-width: 1920px) {
  .resource-grid {
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 32px;
  }
}
</style>
