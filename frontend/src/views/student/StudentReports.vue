<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, ChatDotRound, Edit, Delete } from '@element-plus/icons-vue'
import request from '../../api/request'
import { getMyUserReports, type UserReport } from '../../api/userReport'

interface Report {
  id: number
  resourceId: number
  resourceTitle?: string
  userId: number
  reason: string
  status: string
  reviewReply?: string
  cancelled?: boolean
  resourceVisibility?: string
  createdAt: string
  reviewedAt?: string
  updatedAt?: string
}

const activeTab = ref('resource')
const list = ref<Report[]>([])
const userReportList = ref<UserReport[]>([])
const loading = ref(false)
const userReportLoading = ref(false)
const editDialogVisible = ref(false)
const editReport = ref<Report | null>(null)
const editReason = ref('')

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待处理', type: 'warning' },
  RESOLVED: { label: '已处理', type: 'success' },
  REJECTED: { label: '已驳回', type: 'info' },
  CANCELLED: { label: '已撤销', type: '' },
}

async function loadResourceReports() {
  loading.value = true
  try {
    const res: any = await request.get('/reports/my')
    list.value = res.data || []
  } catch {
    ElMessage.error('加载资料举报列表失败')
  } finally {
    loading.value = false
  }
}

async function loadUserReports() {
  userReportLoading.value = true
  try {
    const res: any = await getMyUserReports()
    userReportList.value = res.data || []
  } catch {
    ElMessage.error('加载用户举报列表失败')
  } finally {
    userReportLoading.value = false
  }
}

function handleTabChange(tab: string) {
  activeTab.value = tab
  if (tab === 'resource') {
    if (list.value.length === 0) {
      loadResourceReports()
    }
  } else if (tab === 'user') {
    if (userReportList.value.length === 0) {
      loadUserReports()
    }
  }
}

async function handleCancel(row: Report) {
  try {
    await ElMessageBox.confirm('确定要撤销此举报吗？撤销后将无法再次修改。', '确认撤销', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    await request.put(`/reports/cancel?reportId=${row.id}`)
    ElMessage.success('撤销成功')
    loadResourceReports()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.response?.data?.message || '撤销失败')
    }
  }
}

function handleEdit(row: Report) {
  if (row.status !== 'PENDING') {
    ElMessage.warning('只能修改待处理的举报')
    return
  }
  if (row.cancelled) {
    ElMessage.warning('已撤销的举报不能修改')
    return
  }
  editReport.value = row
  editReason.value = row.reason
  editDialogVisible.value = true
}

const sortedResourceReports = computed(() => {
  return [...list.value].sort((a, b) => {
    const ta = new Date(a.createdAt).getTime()
    const tb = new Date(b.createdAt).getTime()
    return tb - ta
  })
})

const sortedUserReports = computed(() => {
  return [...userReportList.value].sort((a, b) => {
    const ta = new Date(a.createdAt).getTime()
    const tb = new Date(b.createdAt).getTime()
    return tb - ta
  })
})

async function handleUpdate() {
  if (!editReport.value) return
  if (!editReason.value.trim()) {
    ElMessage.warning('请输入举报原因')
    return
  }
  try {
    await request.put('/reports/update', {
      reportId: editReport.value.id,
      reason: editReason.value,
    })
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    loadResourceReports()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '修改失败')
  }
}

function formatDate(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  loadResourceReports()
})
</script>

<template>
  <div class="student-reports-page">
    <el-card class="reports-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>我的举报</h3>
            <p class="card-subtitle">查看我提交的所有举报记录</p>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="资料举报" name="resource">
          <div v-loading="loading">
            <div v-if="list.length === 0" class="empty-reports">
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
                    <el-tag :type="statusMap[report.status]?.type" size="small" style="margin-left: 8px">
                      {{ statusMap[report.status]?.label || report.status }}
                    </el-tag>
                    <el-tag v-if="report.cancelled" type="info" size="small" style="margin-left: 8px">已撤销</el-tag>
                    <el-tag v-if="report.resourceVisibility === 'HIDDEN'" type="warning" size="small" style="margin-left: 8px">已隐藏</el-tag>
                  </div>
                  <div class="report-meta">
                    <span>资料ID：{{ report.resourceId }}</span>
                    <span>举报原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">管理员回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="report-right">
                  <div class="report-actions">
                    <el-button
                      v-if="report.status === 'PENDING' && !report.cancelled"
                      size="small"
                      text
                      type="info"
                      :icon="Edit"
                      @click.stop="handleEdit(report)"
                    >
                      修改
                    </el-button>
                    <el-button
                      v-if="report.status === 'PENDING' && !report.cancelled"
                      size="small"
                      text
                      type="danger"
                      :icon="Delete"
                      @click.stop="handleCancel(report)"
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
          <div v-loading="userReportLoading">
            <div v-if="userReportList.length === 0" class="empty-reports">
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
                    <el-tag :type="statusMap[report.status]?.type" size="small" style="margin-left: 8px">
                      {{ statusMap[report.status]?.label || report.status }}
                    </el-tag>
                    <el-tag v-if="report.cancelled" type="info" size="small" style="margin-left: 8px">已撤销</el-tag>
                  </div>
                  <div class="report-meta">
                    <span>举报原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">管理员回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="report-right">
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
    </el-card>

    <el-dialog v-model="editDialogVisible" title="修改举报原因" width="500px">
      <el-form>
        <el-form-item label="举报原因">
          <el-input
            v-model="editReason"
            type="textarea"
            :rows="4"
            placeholder="请输入举报原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.student-reports-page {
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

