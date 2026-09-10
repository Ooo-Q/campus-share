<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, ChatDotRound, View, Edit, Delete } from '@element-plus/icons-vue'
import request from '../../api/request'
import type { UserReport } from '../../api/userReport'

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

const statusMap: Record<
  string,
  {
    label: string
    type: 'primary' | 'success' | 'warning' | 'info' | 'danger'
  }
> = {
  PENDING: { label: '待处理', type: 'warning' },
  RESOLVED: { label: '已处理', type: 'success' },
  REJECTED: { label: '已驳回', type: 'info' },
  CANCELLED: { label: '已撤销', type: 'info' },
}

async function syncPendingReportsBaselineFromOverview() {
  try {
    const res = await request.get<{ success: boolean; data: { stats?: { pendingReports?: number } } }>(
      '/admin/overview',
    )
    const n = res.data?.stats?.pendingReports ?? 0
    localStorage.setItem('admin_last_pending_reports_count', String(n))
  } catch {
  }
}

async function loadResourceReports() {
  loading.value = true
  try {
    const res: any = await request.get<{ success: boolean; data: Report[] }>('/reports')
    resourceReports.value = res.data || []
  } catch {
    ElMessage.error('加载资料举报列表失败')
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
    ElMessage.error('加载用户举报列表失败')
  } finally {
    userReportsLoading.value = false
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab
  if (tab === 'resource') {
    if (resourceReports.value.length === 0) {
      loadResourceReports()
    }
  } else if (tab === 'user') {
    if (userReports.value.length === 0) {
      loadUserReports()
    }
  }
}

function openProcess(row: Report) {
  if (row.status !== 'PENDING') {
    ElMessage.warning('举报已处理或已撤销')
    return
  }
  currentReport.value = row
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
      ElMessage.warning('请选择处罚类型')
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
    ElMessage.success('处理完成')
    processDialogVisible.value = false
    loadResourceReports()
  } catch (e: any) {
    const msg = e?.response?.data?.message || '处理失败'
    ElMessage.error(msg)
    if (msg.includes('已处理') || msg.includes('已撤销')) {
      loadResourceReports()
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

async function handleReopen(row: Report) {
  if (row.status === 'PENDING') {
    ElMessage.info('当前举报尚未处理，无需撤销')
    return
  }
  try {
    await ElMessageBox.confirm(
      '撤销后将清理本次处理产生的处罚记录，并将举报恢复为待处理状态，确认撤销？',
      '撤销确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await request.put(`/reports/reopen`, null, { params: { reportId: row.id } })
    ElMessage.success('已撤销本次处理，可重新操作')
    loadResourceReports()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.response?.data?.message || '撤销失败')
  }
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
    const sa = String(va ?? '')
    const sb = String(vb ?? '')
    return sa.localeCompare(sb) * factor
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
    const sa = String(va ?? '')
    const sb = String(vb ?? '')
    return sa.localeCompare(sb) * factor
  })
})

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function openUserReportProcess(row: UserReport) {
  if (row.status !== 'PENDING') {
    ElMessage.warning('举报已处理或已撤销')
    return
  }
  currentUserReport.value = row
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
      ElMessage.warning('请选择处罚类型')
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
    ElMessage.success('处理完成')
    processDialogVisible.value = false
    loadUserReports()
  } catch (e: any) {
    const msg = e?.response?.data?.message || '处理失败'
    ElMessage.error(msg)
    if (msg.includes('已处理') || msg.includes('已撤销')) {
      loadUserReports()
    }
  } finally {
    submitting.value = false
  }
}

async function handleReopenUserReport(row: UserReport) {
  if (row.status === 'PENDING') {
    ElMessage.info('当前举报尚未处理，无需撤销')
    return
  }
  try {
    await ElMessageBox.confirm(
      '撤销后将清理本次处理产生的处罚记录，并将举报恢复为待处理状态，确认撤销？',
      '撤销确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await request.put(`/user-reports/reopen`, null, { params: { reportId: row.id } })
    ElMessage.success('已撤销本次处理，可重新操作')
    loadUserReports()
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.response?.data?.message || '撤销失败')
  }
}

onMounted(async () => {
  await Promise.all([loadResourceReports(), loadUserReports()])
  await syncPendingReportsBaselineFromOverview()
})
</script>

<template>
  <div class="admin-reports-page">
    <el-card class="reports-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>举报管理</h3>
            <p class="card-subtitle">管理系统所有举报记录</p>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="资料举报" name="resource">
          <div v-loading="loading">
            <div v-if="resourceReports.length === 0" class="empty-reports">
              <el-empty description="暂无资料举报" />
            </div>
            <div v-else class="reports-list">
              <div v-for="report in sortedResourceReports" :key="report.id" class="report-item">
                <div class="report-icon-wrapper">
                  <el-icon class="report-icon"><Document /></el-icon>
                </div>
                <div class="report-info">
                  <div class="report-title">
                    {{ report.resourceTitle || '（无标题/已删除）' }}
                    <el-tag :type="statusMap[report.status]?.type || 'info'" size="small" style="margin-left: 8px">
                      {{ statusMap[report.status]?.label || report.status }}
                    </el-tag>
                    <el-tag v-if="report.resourceVisibility === 'HIDDEN'" type="warning" size="small" style="margin-left: 8px">已隐藏</el-tag>
                  </div>
                  <div class="report-meta">
                    <span>资料ID：{{ report.resourceId }}</span>
                    <span>举报人ID：{{ report.userId }}</span>
                    <span>举报原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">管理员回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="report-right">
                  <div class="report-actions">
                    <el-button
                      size="small"
                      text
                      type="info"
                      :icon="View"
                      @click.stop="handleViewResource(report)"
                    >
                      查看
                    </el-button>
                    <el-button
                      size="small"
                      text
                      type="warning"
                      :icon="Edit"
                      :disabled="report.status !== 'PENDING'"
                      @click.stop="openProcess(report)"
                    >
                      处理
                    </el-button>
                    <el-button
                      size="small"
                      text
                      type="danger"
                      :icon="Delete"
                      :disabled="report.status === 'PENDING'"
                      @click.stop="handleReopen(report)"
                    >
                      撤销
                    </el-button>
                  </div>
                  <div class="report-time">
                    <div>创建：{{ formatDate(report.createdAt) }}</div>
                    <div v-if="report.reviewedAt">处理：{{ formatDate(report.reviewedAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="用户举报" name="user">
          <div v-loading="userReportsLoading">
            <div v-if="userReports.length === 0" class="empty-reports">
              <el-empty description="暂无用户举报" />
            </div>
            <div v-else class="reports-list">
              <div v-for="report in sortedUserReports" :key="report.id" class="report-item">
                <div class="report-icon-wrapper user-report-icon">
                  <el-icon class="report-icon"><ChatDotRound /></el-icon>
                </div>
                <div class="report-info">
                  <div class="report-title">
                    被举报用户：{{ report.reportedUsername || `ID: ${report.reportedUserId}` }}
                    <el-tag :type="statusMap[report.status]?.type || 'info'" size="small" style="margin-left: 8px">
                      {{ statusMap[report.status]?.label || report.status }}
                    </el-tag>
                  </div>
                  <div class="report-meta">
                    <span>举报人ID：{{ report.userId }}</span>
                    <span>举报原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">管理员回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="report-right">
                  <div class="report-actions">
                    <el-button
                      size="small"
                      text
                      type="warning"
                      :icon="Edit"
                      :disabled="report.status !== 'PENDING'"
                      @click.stop="openUserReportProcess(report)"
                    >
                      处理
                    </el-button>
                    <el-button
                      size="small"
                      text
                      type="danger"
                      :icon="Delete"
                      :disabled="report.status === 'PENDING'"
                      @click.stop="handleReopenUserReport(report)"
                    >
                      撤销
                    </el-button>
                  </div>
                  <div class="report-time">
                    <div>创建：{{ formatDate(report.createdAt) }}</div>
                    <div v-if="report.reviewedAt">处理：{{ formatDate(report.reviewedAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

    <el-dialog v-model="processDialogVisible" :title="activeTab === 'resource' ? '处理资料举报' : '处理用户举报'" width="640px">
      <div class="dialog-body" v-if="(activeTab === 'resource' && currentReport) || (activeTab === 'user' && currentUserReport)">
        <p class="dialog-title" v-if="activeTab === 'resource' && currentReport">
          举报 ID：{{ currentReport.id }}，资料 ID：{{ currentReport.resourceId }}
        </p>
        <p class="dialog-title" v-if="activeTab === 'user' && currentUserReport">
          举报 ID：{{ currentUserReport.id }}，被举报用户：{{ currentUserReport.reportedUsername }}
        </p>
        <el-form label-width="110px">
          <el-form-item label="处理动作">
            <el-radio-group v-model="actionOption" class="action-radio-group">
              <el-radio-button label="REJECT">驳回</el-radio-button>
              <el-radio-button label="REPLY">仅回复</el-radio-button>
              <el-radio-button v-if="activeTab === 'resource'" label="HIDE">隐藏资料</el-radio-button>
              <el-radio-button v-if="activeTab === 'resource'" label="HIDE_PUNISH">隐藏并处罚</el-radio-button>
              <el-radio-button v-if="activeTab === 'user'" label="PUNISH">处罚</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处罚类型" v-if="(activeTab === 'resource' && actionOption === 'HIDE_PUNISH') || (activeTab === 'user' && actionOption === 'PUNISH')">
            <el-select v-model="punishmentType" placeholder="选择处罚类型" style="width: 260px">
              <el-option v-if="activeTab === 'resource'" label="警告" value="WARNING" />
              <el-option v-if="activeTab === 'resource'" label="禁止上传资料" value="SUSPENSION" />
              <el-option v-if="activeTab === 'user'" label="警告" value="WARNING" />
              <el-option v-if="activeTab === 'user'" label="禁言" value="MUTE" />
            </el-select>
          </el-form-item>
          <el-form-item label="处罚时长(天)" v-if="(activeTab === 'resource' && actionOption === 'HIDE_PUNISH') || (activeTab === 'user' && actionOption === 'PUNISH')">
            <el-input-number
              v-model="punishmentDuration"
              :min="1"
              :max="365"
              placeholder="留空为永久"
              class="punish-duration"
            />
          </el-form-item>
          <el-form-item label="处罚说明" v-if="(activeTab === 'resource' && actionOption === 'HIDE_PUNISH') || (activeTab === 'user' && actionOption === 'PUNISH')">
            <el-input v-model="punishmentReason" placeholder='可选，默认写入"举报处理处罚"' />
          </el-form-item>
        </el-form>
        <el-input
          v-model="reviewReply"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="填写管理员回复（最多500字）"
        />
      </div>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProcessDialog">提交</el-button>
      </template>
    </el-dialog>
    </el-card>
  </div>
</template>

<style scoped>
.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dialog-title {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.muted {
  color: #909399;
}

:deep(.action-radio-group) {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  align-items: center;
}

:deep(.action-radio-group .el-radio-button__inner) {
  padding: 2px 8px;
  min-width: 74px;
  font-size: 12px;
  border-radius: 14px !important;
  border: 1px solid #dcdfe6;
  background: #f6f7fb;
  color: #606266;
  box-sizing: border-box;
}

:deep(.action-radio-group .el-radio-button.is-active .el-radio-button__inner) {
  background: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}

:deep(.action-radio-group .el-radio-button:nth-child(4) .el-radio-button__inner) {
  border-color: #f56c6c;
  color: #f56c6c;
  background: #fff5f5;
}

:deep(.action-radio-group .el-radio-button:nth-child(4).is-active .el-radio-button__inner) {
  background: #f56c6c;
  color: #fff;
  border-color: #f56c6c;
}

:deep(.punish-duration) {
  width: 200px;
}

.admin-reports-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
}

.reports-card {
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

.empty-reports {
  padding: 40px;
  text-align: center;
}

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.report-item:hover {
  background: #f9fafb;
  border-color: #667eea;
}

.report-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(48, 207, 208, 0.25);
}

.report-icon-wrapper.user-report-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  box-shadow: 0 4px 12px rgba(240, 147, 251, 0.25);
}

.report-icon {
  font-size: 24px;
  color: #ffffff;
}

.report-info {
  flex: 1;
  min-width: 0;
}

.report-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.report-meta {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.report-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.report-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.report-actions :deep(.el-button) {
  height: 28px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

.report-actions :deep(.el-button__icon) {
  margin-right: 4px;
  display: inline-flex;
  align-items: center;
}

.report-time {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
  min-width: 160px;
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.muted {
  color: #909399;
}

@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    gap: 12px;
  }

  .report-item {
    flex-wrap: wrap;
  }

  .report-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }

  .report-time {
    min-width: auto;
    text-align: left;
  }
}

</style>

