<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NTag, NEmpty, NSpin, NIcon } from 'naive-ui'
import { ConstructOutline } from '@vicons/ionicons5'
import request from '../../api/request'
import { message } from '../../utils/feedback'
import PageHeader from '../../components/PageHeader.vue'

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

const typeMap: Record<string, { label: string; type: 'warning' | 'error' | 'info' }> = {
  WARNING: { label: '警告', type: 'warning' },
  SUSPENSION: { label: '禁止上传资料', type: 'error' },
  MUTE: { label: '禁言', type: 'error' },
}

const statusMap: Record<string, { label: string; type: 'error' | 'default' }> = {
  ACTIVE: { label: '生效中', type: 'error' },
}

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/punishments/my')
    list.value = res.data || []
    localStorage.setItem('student_last_punishment_count', (list.value.length || 0).toString())
  } catch {
    message.error('加载处罚记录失败')
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
  <div class="page">
    <PageHeader title="处罚记录" subtitle="查看所有处罚记录和状态" :show-back="false" />

    <div class="glass-panel list-panel">
      <NSpin :show="loading">
        <NEmpty v-if="list.length === 0" description="暂无处罚记录" />
        <div v-else class="list">
          <div v-for="punishment in list" :key="punishment.id" class="item surface-card">
            <div class="icon">
              <NIcon :size="22" :component="ConstructOutline" />
            </div>
            <div class="info">
              <div class="title">
                {{ renderCardTitle(punishment) }}
                <NTag
                  v-if="punishment.resourceId && isHideOnly(punishment)"
                  type="warning"
                  size="small"
                  :bordered="false"
                >
                  已隐藏
                </NTag>
                <NTag
                  v-else-if="punishment.punishmentType"
                  :type="typeMap[punishment.punishmentType]?.type || 'info'"
                  size="small"
                  :bordered="false"
                >
                  {{ typeMap[punishment.punishmentType]?.label || punishment.punishmentType }}
                </NTag>
                <NTag
                  :type="isExpired(punishment) ? 'default' : statusMap[punishment.status]?.type || 'error'"
                  size="small"
                  :bordered="false"
                >
                  {{
                    isExpired(punishment)
                      ? '已过期'
                      : statusMap[punishment.status]?.label || punishment.status
                  }}
                </NTag>
              </div>
              <div class="meta muted">
                <span>原因：{{ punishment.reason || '处罚记录' }}</span>
                <span v-if="punishment.duration">时长：{{ punishment.duration }} 天</span>
                <span v-else-if="punishment.punishmentType">时长：永久</span>
                <span>开始：{{ formatDate(punishment.startDate) }}</span>
                <span>结束：{{ formatDate(punishment.endDate) }}</span>
              </div>
            </div>
          </div>
        </div>
      </NSpin>
    </div>
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
}

.icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(196, 164, 132, 0.22);
  color: var(--m-peach);
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.meta {
  font-size: 13px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .item {
    align-items: flex-start;
  }
}
</style>
