<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import {
  NButton,
  NInput,
  NSelect,
  NTag,
  NPagination,
  NEmpty,
  NSpin,
  NIcon,
  NAvatar,
} from 'naive-ui'
import {
  DocumentOutline,
  SearchOutline,
  EyeOutline,
  DownloadOutline,
  StarOutline,
  HeartOutline,
} from '@vicons/ionicons5'
import { fetchResources, type Resource } from '../api/resource'
import { fetchCategories, type Category } from '../api/category'
import { useRouter } from 'vue-router'
import { message } from '../utils/feedback'
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
const categoryId = ref<number | null>(null)
const categories = ref<Category[]>([])

const categoryOptions = computed(() =>
  categories.value.map((c) => ({ label: c.name, value: c.id })),
)

async function loadData() {
  loading.value = true
  try {
    const res = await fetchResources({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value ?? undefined,
    })
    const pageData = res.data
    tableData.value = pageData.records || []
    total.value = pageData.total || 0
  } catch {
    message.error('加载资料失败')
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
      const ta = va ? new Date(va as string).getTime() : 0
      const tb = vb ? new Date(vb as string).getTime() : 0
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

function handleView(resource: Resource) {
  if (!resource?.id) {
    message.warning('未找到资源')
    return
  }
  router.push({
    name: 'StudentResourceDetail',
    params: { id: resource.id },
    query: { from: 'resources' },
  })
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
  <div class="page">
    <PageHeader title="资料广场" subtitle="按分类与关键词浏览同学分享的学习资料" :show-back="false" />

    <div class="filter-bar glass-panel-strong">
      <NSelect
        v-model:value="categoryId"
        :options="categoryOptions"
        placeholder="全部分类"
        clearable
        class="category-select"
        @update:value="loadData"
      />
      <NInput
        v-model:value="keyword"
        placeholder="搜索资料标题或简介..."
        clearable
        class="search-input"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <NIcon :component="DocumentOutline" />
        </template>
      </NInput>
      <NButton type="primary" @click="loadData">
        <template #icon>
          <NIcon :component="SearchOutline" />
        </template>
        搜索
      </NButton>
    </div>

    <NSpin :show="loading">
      <div class="resource-list glass-panel">
        <div
          v-for="item in sortedResources"
          :key="item.id"
          class="resource-row surface-card"
          @click="handleView(item)"
        >
          <div class="row-icon">
            <NIcon :size="22" :component="DocumentOutline" />
          </div>
          <div class="row-main">
            <div class="row-top">
              <h3 class="row-title">{{ item.title }}</h3>
              <div class="row-tags">
                <NTag v-if="item.categoryName" size="small" :bordered="false" type="success">
                  {{ item.categoryName }}
                </NTag>
                <span class="file-badge">{{ getFileType(item.fileUrl) }}</span>
              </div>
            </div>
            <p class="row-desc muted">{{ item.description || '暂无描述' }}</p>
            <div class="row-meta">
              <div class="author">
                <NAvatar :size="22" round>
                  {{ (item.ownerName || 'U').slice(0, 1).toUpperCase() }}
                </NAvatar>
                <span>{{ item.ownerName }}</span>
              </div>
              <span class="muted">{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <div class="row-stats">
            <span><NIcon :component="EyeOutline" /> {{ item.viewCount || 0 }}</span>
            <span><NIcon :component="DownloadOutline" /> {{ item.downloadCount || 0 }}</span>
            <span><NIcon :component="StarOutline" /> {{ item.favoriteCount || 0 }}</span>
            <span><NIcon :component="HeartOutline" /> {{ item.likeCount || 0 }}</span>
          </div>
        </div>
        <NEmpty v-if="!loading && tableData.length === 0" description="暂无资料" />
      </div>
    </NSpin>

    <div class="pagination">
      <NPagination
        v-model:page="page"
        v-model:page-size="size"
        :item-count="total"
        :page-sizes="[12, 24, 48, 96]"
        show-size-picker
        show-quick-jumper
        @update:page="handlePageChange"
        @update:page-size="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
  padding: clamp(12px, 2vw, 24px);
  box-sizing: border-box;
}

.filter-bar {
  position: sticky;
  top: 8px;
  z-index: 5;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  padding: 14px 16px;
  margin-bottom: 18px;
}

.category-select {
  width: 160px;
}

.search-input {
  flex: 1;
  min-width: 180px;
}

.resource-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 200px;
}

.resource-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.resource-row:hover {
  transform: translateY(-2px);
  border-color: rgba(122, 158, 142, 0.35);
  box-shadow: 0 10px 28px rgba(61, 69, 64, 0.1);
}

.row-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(122, 158, 142, 0.16);
  color: var(--m-sage-deep);
  flex-shrink: 0;
}

.row-main {
  min-width: 0;
}

.row-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.row-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--m-ink);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.file-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--m-sage-deep);
  background: rgba(122, 158, 142, 0.14);
  border-radius: 999px;
  padding: 2px 10px;
}

.row-desc {
  margin: 6px 0 8px;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.row-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
}

.author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.row-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--m-ink-soft);
  min-width: 72px;
}

.row-stats span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@media (max-width: 720px) {
  .filter-bar {
    position: static;
  }

  .category-select,
  .search-input,
  .filter-bar :deep(.n-button) {
    width: 100%;
  }

  .resource-row {
    grid-template-columns: auto 1fr;
  }

  .row-stats {
    grid-column: 1 / -1;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 12px;
    padding-top: 4px;
  }
}
</style>
