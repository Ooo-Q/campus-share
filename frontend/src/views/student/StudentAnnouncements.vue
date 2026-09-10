<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import request from '../../api/request'
import { acknowledgeStudentAnnouncements } from '../../utils/studentAnnouncementReminder'

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
    ElMessage.error('加载公告列表失败')
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
  <div class="student-announcements-page">
    <el-card class="announcements-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>系统公告</h3>
            <p class="card-subtitle">查看平台最新公告信息</p>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="list.length === 0" class="empty-announcements">
          <el-empty description="暂无公告" />
        </div>
        <div v-else class="announcements-list">
          <div
            v-for="item in list"
            :key="item.id"
            class="announcement-item"
            @click="viewDetail(item)"
          >
            <div class="announcement-icon-wrapper">
              <el-icon class="announcement-icon"><Bell /></el-icon>
            </div>
            <div class="announcement-info">
              <div class="announcement-title">
                {{ item.title }}
                <el-tag v-if="item.pinned" type="warning" size="small" style="margin-left: 8px">置顶</el-tag>
              </div>
              <div class="announcement-meta">
                <span>简介：{{ item.summary || '-' }}</span>
              </div>
            </div>
            <div class="announcement-right">
              <div class="announcement-time">{{ formatDate(item.publishAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="公告详情" width="800px">
      <div v-if="selectedAnnouncement" class="announcement-detail">
        <div class="detail-header">
          <h2>{{ selectedAnnouncement.title }}</h2>
          <div class="detail-meta">
            <el-tag v-if="selectedAnnouncement.pinned" type="warning" size="small">置顶</el-tag>
            <span class="detail-date">{{ formatDate(selectedAnnouncement.publishAt) }}</span>
          </div>
        </div>
        <div class="detail-content" v-html="selectedAnnouncement.content"></div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-announcements-page {
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

.empty-announcements {
  padding: 40px;
  text-align: center;
}

.announcements-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.announcement-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
  cursor: pointer;
}

.announcement-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.announcement-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(161, 196, 253, 0.25);
}

.announcement-icon {
  font-size: 24px;
  color: #ffffff;
}

.announcement-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
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

.announcement-time {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
  min-width: 160px;
  text-align: right;
}

.announcement-detail {
  padding: 20px 0;
}

.detail-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.detail-header h2 {
  font-size: clamp(20px, 3vw, 24px);
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 12px 0;
  line-height: 1.3;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-date {
  font-size: 14px;
  color: #6b7280;
}

.detail-content {
  line-height: 1.8;
  color: #374151;
  font-size: 15px;
}

.detail-content :deep(p) {
  margin: 12px 0;
}

.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
}

.detail-content :deep(h1),
.detail-content :deep(h2),
.detail-content :deep(h3) {
  margin-top: 24px;
  margin-bottom: 12px;
  color: #1f2937;
  font-weight: 600;
}

.detail-content :deep(ul),
.detail-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.detail-content :deep(li) {
  margin: 6px 0;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 12px;
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
