<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NTabs,
  NTabPane,
  NTag,
  NEmpty,
  NSpin,
  NModal,
  NForm,
  NFormItem,
  NSelect,
  NInput,
  NInputNumber,
  NSpace,
  NIcon,
  NRadioGroup,
  NRadioButton,
} from 'naive-ui'
import {
  DocumentOutline,
  ChatbubbleOutline,
  EyeOutline,
  CreateOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import request from '../../api/request'
import type { UserReport } from '../../api/userReport'
import PageHeader from '../../components/PageHeader.vue'
import { message, dialog } from '../../utils/feedback'

const router = useRouter()

interface Report {
  id: number
  resourceId: number
  resourceTitle?: string
  userId: number
  reason: string
  status: string
  reviewReply?: string
  resourceVisibility?: string
  resourceOwnerId?: number
  createdAt: string
  reviewedAt?: string
}

const activeTab = ref('resource')
const resourceReports = ref<Report[]>([])
const userReports = ref<UserReport[]>([])
const loading = ref(false)
const userReportsLoading = ref(false)
const sortProp = ref<string>('createdAt')
const sortOrder = ref<'ascending' | 'descending' | null>('descending')
const processDialogVisible = ref(false)
const actionOption = ref<'REJECT' | 'REPLY' | 'HIDE' | 'HIDE_PUNISH' | 'PUNISH'>('REPLY')
const punishmentType = ref<'WARNING' | 'SUSPENSION' | 'MUTE' | ''>('')
const punishmentDuration = ref<number | null>(null)
const punishmentReason = ref('')
const reviewReply = ref('')
const currentReport = ref<Report | null>(null)
const currentUserReport = ref<UserReport | null>(null)
const submitting = ref(false)

const statusMap: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'error' | 'default' }> = {
  PENDING: { label: '待处理', type: 'warning' },
  RESOLVED: { label: '已处理', type: 'success' },
  REJECTED: { label: '已驳回', type: 'info' },
  CANCELLED: { label: '已撤销', type: 'default' },
}

const resourcePunishmentOptions = [
  { label: '警告', value: 'WARNING' },
  { label: '禁止上传资料', value: 'SUSPENSION' },
]

const userPunishmentOptions = [
  { label: '警告', value: 'WARNING' },
  { label: '禁言', value: 'MUTE' },
]

async function syncPendingReportsBaselineFromOverview() {
  try {
    const res = await request.get<{ success: boolean; data: { stats?: { pendingReports?: number } } }>(
      '/admin/overview',
    )
    const n = res.data?.stats?.pendingReports ?? 0
    localStorage.setItem('admin_last_pending_reports_count', String(n))
  } catch {
    /* ignore */
  }
}

async function loadResourceReports() {
  loading.value = true
  try {
    const res: any = await request.get<{ success: boolean; data: Report[] }>('/reports')
    resourceReports.value = res.data || []
  } catch {
    message.error('加载资料举报列表失败')
  } finally {
    loading.value = false
  }
}

async function loadUserReports() {
  userReportsLoading.value = true
  try {
    const res: any = await request.get<{ success: boolean; data: UserReport[] }>('/user-reports')
    userReports.value = res.data || []
  } catch {
    message.error('加载用户举报列表失败')
  } finally {
    userReportsLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  activeTab.value = String(tab)
  if (activeTab.value === 'resource' && resourceReports.value.length === 0) {
    void loadResourceReports()
  } else if (activeTab.value === 'user' && userReports.value.length === 0) {
    void loadUserReports()
  }
}

function openProcess(row: Report) {
  if (row.status !== 'PENDING') {
    message.warning('举报已处理或已撤销')
    return
  }
  currentReport.value = row
  currentUserReport.value = null
  reviewReply.value = row.reviewReply || ''
  actionOption.value = 'REPLY'
  punishmentType.value = ''
  punishmentDuration.value = null
  punishmentReason.value = ''
  processDialogVisible.value = true
}

async function submitProcess() {
  if (!currentReport.value) return
  submitting.value = true
  try {
    const act = actionOption.value
    let status: 'RESOLVED' | 'REJECTED' = 'RESOLVED'
    let actionPayload: 'NONE' | 'HIDE' | 'HIDE_PUNISH' = 'NONE'
    let needPunish = false

    if (act === 'REJECT') {
      status = 'REJECTED'
      actionPayload = 'NONE'
    } else if (act === 'REPLY') {
      status = 'RESOLVED'
      actionPayload = 'NONE'
    } else if (act === 'HIDE') {
      status = 'RESOLVED'
      actionPayload = 'HIDE'
    } else if (act === 'HIDE_PUNISH') {
      status = 'RESOLVED'
      actionPayload = 'HIDE_PUNISH'
      needPunish = true
    }

    if (needPunish && !punishmentType.value) {
      message.warning('请选择处罚类型')
      submitting.value = false
      return
    }

    await request.put('/reports/review', {
      reportId: currentReport.value.id,
      status,
      reviewReply: reviewReply.value?.trim() || undefined,
      action: actionPayload,
      punishmentType: needPunish ? punishmentType.value : undefined,
      punishmentDuration: needPunish ? punishmentDuration.value : undefined,
      punishmentReason: needPunish ? punishmentReason.value : undefined,
    })
    message.success('处理完成')
    processDialogVisible.value = false
    void loadResourceReports()
  } catch (e: any) {
    const msg = e?.response?.data?.message || '处理失败'
    message.error(msg)
    if (msg.includes('已处理') || msg.includes('已撤销')) {
      void loadResourceReports()
    }
  } finally {
    submitting.value = false
  }
}

async function submitProcessDialog() {
  if (activeTab.value === 'resource' && currentReport.value) {
    await submitProcess()
  } else if (activeTab.value === 'user' && currentUserReport.value) {
    await submitUserReportProcess()
  }
}

function handleReopen(row: Report) {
  if (row.status === 'PENDING') {
    message.info('当前举报尚未处理，无需撤销')
    return
  }
  dialog.warning({
    title: '撤销确认',
    content: '撤销后将清理本次处理产生的处罚记录，并将举报恢复为待处理状态，确认撤销？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.put(`/reports/reopen`, null, { params: { reportId: row.id } })
        message.success('已撤销本次处理，可重新操作')
        void loadResourceReports()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '撤销失败')
      }
    },
  })
}

function handleViewResource(row: Report) {
  router.push({ path: `/admin/resources/${row.resourceId}`, query: { from: 'reports' } })
}

const sortedResourceReports = computed(() => {
  const arr = [...resourceReports.value]
  if (!sortProp.value || !sortOrder.value) return arr
  const factor = sortOrder.value === 'ascending' ? 1 : -1
  const prop = sortProp.value as keyof Report
  return arr.sort((a, b) => {
    const va = a[prop]
    const vb = b[prop]
    if (prop === 'createdAt' || prop === 'reviewedAt') {
      const ta = va ? new Date(va as any).getTime() : 0
      const tb = vb ? new Date(vb as any).getTime() : 0
      return (ta - tb) * factor
    }
    if (typeof va === 'number' && typeof vb === 'number') {
      return (va - vb) * factor
    }
    return String(va ?? '').localeCompare(String(vb ?? '')) * factor
  })
})

const sortedUserReports = computed(() => {
  const arr = [...userReports.value]
  if (!sortProp.value || !sortOrder.value) return arr
  const factor = sortOrder.value === 'ascending' ? 1 : -1
  const prop = sortProp.value as keyof UserReport
  return arr.sort((a, b) => {
    const va = a[prop]
    const vb = b[prop]
    if (prop === 'createdAt' || prop === 'reviewedAt') {
      const ta = va ? new Date(va as any).getTime() : 0
      const tb = vb ? new Date(vb as any).getTime() : 0
      return (ta - tb) * factor
    }
    if (typeof va === 'number' && typeof vb === 'number') {
      return (va - vb) * factor
    }
    return String(va ?? '').localeCompare(String(vb ?? '')) * factor
  })
})

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function openUserReportProcess(row: UserReport) {
  if (row.status !== 'PENDING') {
    message.warning('举报已处理或已撤销')
    return
  }
  currentUserReport.value = row
  currentReport.value = null
  reviewReply.value = row.reviewReply || ''
  actionOption.value = 'REPLY'
  punishmentType.value = ''
  punishmentDuration.value = null
  punishmentReason.value = ''
  processDialogVisible.value = true
}

async function submitUserReportProcess() {
  if (!currentUserReport.value) return
  submitting.value = true
  try {
    const act = actionOption.value
    let status: 'RESOLVED' | 'REJECTED' = 'RESOLVED'
    let actionPayload: 'NONE' | 'PUNISH' = 'NONE'
    let needPunish = false

    if (act === 'REJECT') {
      status = 'REJECTED'
      actionPayload = 'NONE'
    } else if (act === 'REPLY') {
      status = 'RESOLVED'
      actionPayload = 'NONE'
    } else if (act === 'PUNISH') {
      status = 'RESOLVED'
      actionPayload = 'PUNISH'
      needPunish = true
    }

    if (needPunish && !punishmentType.value) {
      message.warning('请选择处罚类型')
      submitting.value = false
      return
    }

    await request.put('/user-reports/review', {
      reportId: currentUserReport.value.id,
      status,
      reviewReply: reviewReply.value?.trim() || undefined,
      action: actionPayload,
      punishmentType: needPunish ? punishmentType.value : undefined,
      punishmentDuration: needPunish ? punishmentDuration.value : undefined,
      punishmentReason: needPunish ? punishmentReason.value : undefined,
    })
    message.success('处理完成')
    processDialogVisible.value = false
    void loadUserReports()
  } catch (e: any) {
    const msg = e?.response?.data?.message || '处理失败'
    message.error(msg)
    if (msg.includes('已处理') || msg.includes('已撤销')) {
      void loadUserReports()
    }
  } finally {
    submitting.value = false
  }
}

function handleReopenUserReport(row: UserReport) {
  if (row.status === 'PENDING') {
    message.info('当前举报尚未处理，无需撤销')
    return
  }
  dialog.warning({
    title: '撤销确认',
    content: '撤销后将清理本次处理产生的处罚记录，并将举报恢复为待处理状态，确认撤销？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.put(`/user-reports/reopen`, null, { params: { reportId: row.id } })
        message.success('已撤销本次处理，可重新操作')
        void loadUserReports()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '撤销失败')
      }
    },
  })
}

const showPunishFields = computed(
  () =>
    (activeTab.value === 'resource' && actionOption.value === 'HIDE_PUNISH') ||
    (activeTab.value === 'user' && actionOption.value === 'PUNISH'),
)

onMounted(async () => {
  await Promise.all([loadResourceReports(), loadUserReports()])
  await syncPendingReportsBaselineFromOverview()
})
</script>

<template>
  <div class="page">
    <PageHeader title="举报处理" subtitle="审核资料举报与用户举报" :show-back="false" />

    <div class="glass-panel panel">
      <NTabs v-model:value="activeTab" type="segment" animated @update:value="handleTabChange">
        <NTabPane name="resource" tab="资料举报">
          <NSpin :show="loading">
            <NEmpty v-if="resourceReports.length === 0" description="暂无资料举报" />
            <div v-else class="list">
              <div v-for="report in sortedResourceReports" :key="report.id" class="item surface-card">
                <div class="icon resource">
                  <NIcon :size="22" :component="DocumentOutline" />
                </div>
                <div class="info">
                  <div class="title">
                    {{ report.resourceTitle || '（无标题/已删除）' }}
                    <NTag
                      :type="statusMap[report.status]?.type || 'info'"
                      size="small"
                      :bordered="false"
                    >
                      {{ statusMap[report.status]?.label || report.status }}
                    </NTag>
                    <NTag
                      v-if="report.resourceVisibility === 'HIDDEN'"
                      type="warning"
                      size="small"
                      :bordered="false"
                    >
                      已隐藏
                    </NTag>
                  </div>
                  <div class="meta muted">
                    <span>资料 ID：{{ report.resourceId }}</span>
                    <span>举报人 ID：{{ report.userId }}</span>
                    <span>原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="right">
                  <NSpace :size="6">
                    <NButton size="small" quaternary @click="handleViewResource(report)">
                      <template #icon><NIcon :component="EyeOutline" /></template>
                      查看
                    </NButton>
                    <NButton
                      size="small"
                      quaternary
                      type="warning"
                      :disabled="report.status !== 'PENDING'"
                      @click="openProcess(report)"
                    >
                      <template #icon><NIcon :component="CreateOutline" /></template>
                      处理
                    </NButton>
                    <NButton
                      size="small"
                      quaternary
                      type="error"
                      :disabled="report.status === 'PENDING'"
                      @click="handleReopen(report)"
                    >
                      <template #icon><NIcon :component="TrashOutline" /></template>
                      撤销
                    </NButton>
                  </NSpace>
                  <div class="time muted">
                    <div>创建：{{ formatDate(report.createdAt) }}</div>
                    <div v-if="report.reviewedAt">处理：{{ formatDate(report.reviewedAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </NSpin>
        </NTabPane>

        <NTabPane name="user" tab="用户举报">
          <NSpin :show="userReportsLoading">
            <NEmpty v-if="userReports.length === 0" description="暂无用户举报" />
            <div v-else class="list">
              <div v-for="report in sortedUserReports" :key="report.id" class="item surface-card">
                <div class="icon user">
                  <NIcon :size="22" :component="ChatbubbleOutline" />
                </div>
                <div class="info">
                  <div class="title">
                    被举报用户：{{ report.reportedUsername || `ID: ${report.reportedUserId}` }}
                    <NTag
                      :type="statusMap[report.status]?.type || 'info'"
                      size="small"
                      :bordered="false"
                    >
                      {{ statusMap[report.status]?.label || report.status }}
                    </NTag>
                  </div>
                  <div class="meta muted">
                    <span>举报人 ID：{{ report.userId }}</span>
                    <span>原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="right">
                  <NSpace :size="6">
                    <NButton
                      size="small"
                      quaternary
                      type="warning"
                      :disabled="report.status !== 'PENDING'"
                      @click="openUserReportProcess(report)"
                    >
                      <template #icon><NIcon :component="CreateOutline" /></template>
                      处理
                    </NButton>
                    <NButton
                      size="small"
                      quaternary
                      type="error"
                      :disabled="report.status === 'PENDING'"
                      @click="handleReopenUserReport(report)"
                    >
                      <template #icon><NIcon :component="TrashOutline" /></template>
                      撤销
                    </NButton>
                  </NSpace>
                  <div class="time muted">
                    <div>创建：{{ formatDate(report.createdAt) }}</div>
                    <div v-if="report.reviewedAt">处理：{{ formatDate(report.reviewedAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </NSpin>
        </NTabPane>
      </NTabs>
    </div>

    <NModal
      v-model:show="processDialogVisible"
      preset="card"
      :title="activeTab === 'resource' ? '处理资料举报' : '处理用户举报'"
      style="width: min(640px, 94vw)"
      :bordered="false"
    >
      <div
        v-if="(activeTab === 'resource' && currentReport) || (activeTab === 'user' && currentUserReport)"
        class="dialog-body"
      >
        <p v-if="activeTab === 'resource' && currentReport" class="dialog-title muted">
          举报 ID：{{ currentReport.id }}，资料 ID：{{ currentReport.resourceId }}
        </p>
        <p v-if="activeTab === 'user' && currentUserReport" class="dialog-title muted">
          举报 ID：{{ currentUserReport.id }}，被举报用户：{{ currentUserReport.reportedUsername }}
        </p>

        <NForm label-placement="left" label-width="110">
          <NFormItem label="处理动作">
            <NRadioGroup v-model:value="actionOption" size="small">
              <NRadioButton value="REJECT">驳回</NRadioButton>
              <NRadioButton value="REPLY">仅回复</NRadioButton>
              <NRadioButton v-if="activeTab === 'resource'" value="HIDE">隐藏资料</NRadioButton>
              <NRadioButton v-if="activeTab === 'resource'" value="HIDE_PUNISH">隐藏并处罚</NRadioButton>
              <NRadioButton v-if="activeTab === 'user'" value="PUNISH">处罚</NRadioButton>
            </NRadioGroup>
          </NFormItem>
          <NFormItem v-if="showPunishFields" label="处罚类型">
            <NSelect
              v-model:value="punishmentType"
              :options="activeTab === 'resource' ? resourcePunishmentOptions : userPunishmentOptions"
              placeholder="选择处罚类型"
              style="width: 260px"
            />
          </NFormItem>
          <NFormItem v-if="showPunishFields" label="处罚时长(天)">
            <NInputNumber
              v-model:value="punishmentDuration"
              :min="1"
              :max="365"
              placeholder="留空为永久"
              clearable
              style="width: 200px"
            />
          </NFormItem>
          <NFormItem v-if="showPunishFields" label="处罚说明">
            <NInput v-model:value="punishmentReason" placeholder='可选，默认写入"举报处理处罚"' />
          </NFormItem>
        </NForm>

        <NInput
          v-model:value="reviewReply"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-count
          placeholder="填写管理员回复（最多500字）"
        />
      </div>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="processDialogVisible = false">取消</NButton>
          <NButton type="primary" :loading="submitting" @click="submitProcessDialog">提交</NButton>
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

.panel {
  padding: 18px 20px 22px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  transition: border-color 0.2s ease, transform 0.15s ease;
}

.item:hover {
  border-color: rgba(122, 158, 142, 0.35);
  transform: translateY(-1px);
}

.icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  flex-shrink: 0;
}

.icon.resource {
  background: linear-gradient(145deg, #7a9e8e, #5c8070);
}

.icon.user {
  background: linear-gradient(145deg, #c4897e, #a86f66);
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.meta {
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.time {
  font-size: 12px;
  text-align: right;
  min-width: 150px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dialog-title {
  margin: 0;
  font-size: 13px;
}

@media (max-width: 768px) {
  .item {
    flex-wrap: wrap;
  }

  .right {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .time {
    text-align: left;
    min-width: 0;
  }
}
</style>
