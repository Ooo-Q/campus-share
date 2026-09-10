<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Tools } from '@element-plus/icons-vue'
import request from '../../api/request'

interface Punishment {
  id: number
  userId: number
  reportId?: number
  resourceId?: number
  resourceTitle?: string
  reportedUserId?: number
  reportedUsername?: string
  punishmentType: string
  reason: string
  duration?: number
  startDate: string
  endDate?: string
  status: string
  adminId?: number
  createdAt: string
}

const list = ref<Punishment[]>([])
const loading = ref(false)

const typeMap: Record<string, { label: string; color: string }> = {
  WARNING: { label: '警告', color: '#e6a23c' },
  SUSPENSION: { label: '禁止上传资料', color: '#f56c6c' },
  MUTE: { label: '禁言', color: '#f56c6c' },
}

const statusMap: Record<string, { label: string; type: string }> = {
  ACTIVE: { label: '生效中', type: 'danger' },
}

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/punishments/my')
    list.value = res.data || []
    localStorage.setItem('student_last_punishment_count', (list.value.length || 0).toString())
  } catch {
    ElMessage.error('加载处罚记录失败')
  } finally {
    loading.value = false
  }
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '永久'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function isExpired(punishment: Punishment) {
  if (!punishment.endDate) return false
  return new Date(punishment.endDate) < new Date()
}

function isHideOnly(p: Punishment) {
  return (
    !!p.resourceId &&
    p.punishmentType === 'WARNING' &&
    (!p.duration || p.duration <= 0) &&
    (p.reason?.includes('隐藏') || p.reason?.includes('已隐藏') || p.reason?.includes('违规'))
  )
}


function renderCardTitle(p: Punishment) {
  if (p.resourceTitle) return p.resourceTitle
  if (p.resourceId) return `资源 #${p.resourceId}`
  if (p.reportedUsername) return `用户：${p.reportedUsername}`
  if (p.reportedUserId) return `用户 #${p.reportedUserId}`
  return '管理员直接处罚'
}

onMounted(load)
</script>

<template>
  <div class="student-punishments-page">
    <el-card class="punishments-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>处罚记录</h3>
            <p class="card-subtitle">查看所有处罚记录和状态</p>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <div v-if="list.length === 0" class="empty-punishments">
          <el-empty description="暂无处罚记录" />
        </div>
        <div v-else class="punishments-list">
          <div v-for="punishment in list" :key="punishment.id" class="punishment-item">
            <div class="punishment-icon-wrapper">
              <el-icon class="punishment-icon"><Tools /></el-icon>
            </div>
            <div class="punishment-info">
              <div class="punishment-title">
                {{ renderCardTitle(punishment) }}
                <el-tag v-if="punishment.resourceId && isHideOnly(punishment)" type="warning" size="small" style="margin-left: 8px">已隐藏</el-tag>
                <el-tag v-else-if="punishment.punishmentType" :color="typeMap[punishment.punishmentType]?.color" effect="dark" size="small" style="margin-left: 8px">
                  {{ typeMap[punishment.punishmentType]?.label || punishment.punishmentType }}
                </el-tag>
                <el-tag :type="statusMap[punishment.status]?.type || (isExpired(punishment) ? '' : 'danger')" size="small" style="margin-left: 8px">
                  {{ isExpired(punishment) ? '已过期' : statusMap[punishment.status]?.label || punishment.status }}
                </el-tag>
              </div>
              <div class="punishment-meta">
                <span>原因：{{ punishment.reason || '处罚记录' }}</span>
                <span v-if="punishment.duration">时长：{{ punishment.duration }} 天</span>
                <span v-else-if="punishment.punishmentType">时长：永久</span>
                <span>开始：{{ formatDate(punishment.startDate) }}</span>
                <span>结束：{{ formatDate(punishment.endDate) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.student-punishments-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.punishments-card {
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

.empty-punishments {
  padding: 40px;
  text-align: center;
}

.punishments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.punishment-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.punishment-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.punishment-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(255, 236, 210, 0.25);
}

.punishment-icon {
  font-size: 24px;
  color: #ffffff;
}

.punishment-info {
  flex: 1;
  min-width: 0;
}

.punishment-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.punishment-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 12px;
  }

  .punishment-item {
    flex-wrap: wrap;
  }
}
</style>


