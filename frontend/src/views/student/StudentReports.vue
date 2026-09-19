<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import {
  NTabs,
  NTabPane,
  NTag,
  NEmpty,
  NSpin,
  NButton,
  NIcon,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSpace,
} from 'naive-ui'
import { DocumentOutline, ChatbubbleOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import request from '../../api/request'
import { getMyUserReports, type UserReport } from '../../api/userReport'
import { message, dialog } from '../../utils/feedback'
import PageHeader from '../../components/PageHeader.vue'

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

const statusMap: Record<string, { label: string; type: 'warning' | 'success' | 'info' | 'default' }> = {
  PENDING: { label: '待处理', type: 'warning' },
  RESOLVED: { label: '已处理', type: 'success' },
  REJECTED: { label: '已驳回', type: 'info' },
  CANCELLED: { label: '已撤销', type: 'default' },
}

async function loadResourceReports() {
  loading.value = true
  try {
    const res: any = await request.get('/reports/my')
    list.value = res.data || []
  } catch {
    message.error('加载资料举报列表失败')
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
    message.error('加载用户举报列表失败')
  } finally {
    userReportLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  activeTab.value = String(tab)
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

function handleCancel(row: Report) {
  dialog.warning({
    title: '确认撤销',
    content: '确定要撤销此举报吗？撤销后将无法再次修改。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request.put(`/reports/cancel?reportId=${row.id}`)
        message.success('撤销成功')
        loadResourceReports()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '撤销失败')
      }
    },
  })
}

function handleEdit(row: Report) {
  if (row.status !== 'PENDING') {
    message.warning('只能修改待处理的举报')
    return
  }
  if (row.cancelled) {
    message.warning('已撤销的举报不能修改')
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
    message.warning('请输入举报原因')
    return
  }
  try {
    await request.put('/reports/update', {
      reportId: editReport.value.id,
      reason: editReason.value,
    })
    message.success('修改成功')
    editDialogVisible.value = false
    loadResourceReports()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '修改失败')
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
  <div class="page">
    <PageHeader title="我的举报" subtitle="查看我提交的所有举报记录" :show-back="false" />

    <div class="glass-panel panel">
      <NTabs v-model:value="activeTab" type="segment" @update:value="handleTabChange">
        <NTabPane name="resource" tab="资料举报">
          <NSpin :show="loading">
            <NEmpty v-if="list.length === 0" description="暂无资料举报" />
            <div v-else class="list">
              <div v-for="report in sortedResourceReports" :key="report.id" class="item surface-card">
                <div class="icon">
                  <NIcon :size="22" :component="DocumentOutline" />
                </div>
                <div class="info">
                  <div class="title">
                    {{ report.resourceTitle || '（无标题/已删除）' }}
                    <NTag
                      :type="statusMap[report.status]?.type || 'default'"
                      size="small"
                      :bordered="false"
                    >
                      {{ statusMap[report.status]?.label || report.status }}
                    </NTag>
                    <NTag v-if="report.cancelled" size="small" :bordered="false">已撤销</NTag>
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
                    <span>资料ID：{{ report.resourceId }}</span>
                    <span>举报原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">管理员回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="right">
                  <NSpace v-if="report.status === 'PENDING' && !report.cancelled" size="small">
                    <NButton size="tiny" quaternary @click="handleEdit(report)">
                      <template #icon><NIcon :component="CreateOutline" /></template>
                      修改
                    </NButton>
                    <NButton size="tiny" quaternary type="error" @click="handleCancel(report)">
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
          <NSpin :show="userReportLoading">
            <NEmpty v-if="userReportList.length === 0" description="暂无用户举报" />
            <div v-else class="list">
              <div v-for="report in sortedUserReports" :key="report.id" class="item surface-card">
                <div class="icon user">
                  <NIcon :size="22" :component="ChatbubbleOutline" />
                </div>
                <div class="info">
                  <div class="title">
                    被举报用户：{{ report.reportedUsername || `ID: ${report.reportedUserId}` }}
                    <NTag
                      :type="statusMap[report.status]?.type || 'default'"
                      size="small"
                      :bordered="false"
                    >
                      {{ statusMap[report.status]?.label || report.status }}
                    </NTag>
                    <NTag v-if="report.cancelled" size="small" :bordered="false">已撤销</NTag>
                  </div>
                  <div class="meta muted">
                    <span>举报原因：{{ report.reason }}</span>
                    <span v-if="report.reviewReply">管理员回复：{{ report.reviewReply }}</span>
                  </div>
                </div>
                <div class="right">
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
      v-model:show="editDialogVisible"
      preset="card"
      title="修改举报原因"
      style="width: min(500px, 92vw)"
      :bordered="false"
    >
      <NForm label-placement="top">
        <NFormItem label="举报原因">
          <NInput
            v-model:value="editReason"
            type="textarea"
            :rows="4"
            placeholder="请输入举报原因"
            maxlength="500"
            show-count
          />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="editDialogVisible = false">取消</NButton>
          <NButton type="primary" @click="handleUpdate">确定</NButton>
        </NSpace>
      </template>
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

.panel {
  padding: 18px 20px 20px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-top: 8px;
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
  background: rgba(122, 158, 142, 0.16);
  color: var(--m-sage-deep);
  flex-shrink: 0;
}

.icon.user {
  background: rgba(196, 137, 126, 0.16);
  color: var(--m-terracotta);
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

.right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.time {
  font-size: 12px;
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 4px;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .item {
    flex-wrap: wrap;
  }

  .right {
    width: 100%;
    justify-content: space-between;
  }

  .time {
    text-align: left;
  }
}
</style>
