<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NTag, NEmpty, NSpin, NModal, NIcon } from 'naive-ui'
import { MegaphoneOutline } from '@vicons/ionicons5'
import request from '../../api/request'
import { acknowledgeStudentAnnouncements } from '../../utils/studentAnnouncementReminder'
import { message } from '../../utils/feedback'
import PageHeader from '../../components/PageHeader.vue'

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
const detailDialogVisible = ref(false)
const selectedAnnouncement = ref<Announcement | null>(null)

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/announcements')
    const data = res.data || []
    list.value = data.sort((a: Announcement, b: Announcement) => {
      if (a.pinned && !b.pinned) return -1
      if (!a.pinned && b.pinned) return 1
      return new Date(b.publishAt).getTime() - new Date(a.publishAt).getTime()
    })
    acknowledgeStudentAnnouncements(list.value.length)
  } catch {
    message.error('加载公告列表失败')
  } finally {
    loading.value = false
  }
}

function viewDetail(row: Announcement) {
  selectedAnnouncement.value = row
  detailDialogVisible.value = true
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="系统公告" subtitle="查看平台最新公告信息" :show-back="false" />

    <div class="glass-panel list-panel">
      <NSpin :show="loading">
        <NEmpty v-if="list.length === 0" description="暂无公告" />
        <div v-else class="list">
          <button
            v-for="item in list"
            :key="item.id"
            type="button"
            class="item surface-card"
            @click="viewDetail(item)"
          >
            <div class="icon">
              <NIcon :size="22" :component="MegaphoneOutline" />
            </div>
            <div class="info">
              <div class="title">
                {{ item.title }}
                <NTag v-if="item.pinned" type="warning" size="small" :bordered="false">置顶</NTag>
              </div>
              <div class="muted summary">简介：{{ item.summary || '-' }}</div>
            </div>
            <div class="muted time">{{ formatDate(item.publishAt) }}</div>
          </button>
        </div>
      </NSpin>
    </div>

    <NModal
      v-model:show="detailDialogVisible"
      preset="card"
      title="公告详情"
      style="width: min(800px, 94vw)"
      :bordered="false"
    >
      <div v-if="selectedAnnouncement" class="detail">
        <div class="detail-header">
          <h2>{{ selectedAnnouncement.title }}</h2>
          <div class="detail-meta">
            <NTag v-if="selectedAnnouncement.pinned" type="warning" size="small" :bordered="false">
              置顶
            </NTag>
            <span class="muted">{{ formatDate(selectedAnnouncement.publishAt) }}</span>
          </div>
        </div>
        <div class="detail-content" v-html="selectedAnnouncement.content"></div>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.page {
  max-width: 980px;
  margin: 0 auto;
  width: 100%;
  padding: clamp(12px, 2vw, 24px);
  box-sizing: border-box;
}

.list-panel {
  padding: 14px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  width: 100%;
  text-align: left;
  cursor: pointer;
  font: inherit;
  color: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.item:hover {
  border-color: rgba(122, 158, 142, 0.35);
  transform: translateY(-1px);
}

.icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(122, 158, 142, 0.16);
  color: var(--m-sage-deep);
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary {
  font-size: 13px;
}

.time {
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.detail-header {
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--m-stroke);
}

.detail-header h2 {
  margin: 0 0 10px;
  font-size: clamp(18px, 2.5vw, 22px);
  font-weight: 600;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-content {
  line-height: 1.8;
  color: var(--m-ink);
  font-size: 15px;
}

.detail-content :deep(p) {
  margin: 12px 0;
}

.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 12px;
  margin: 16px 0;
}

.detail-content :deep(h1),
.detail-content :deep(h2),
.detail-content :deep(h3) {
  margin-top: 20px;
  margin-bottom: 10px;
  font-weight: 600;
}

.detail-content :deep(ul),
.detail-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

@media (max-width: 640px) {
  .item {
    flex-wrap: wrap;
  }

  .time {
    width: 100%;
    padding-left: 58px;
  }
}
</style>
