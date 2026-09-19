<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton, NIcon, NSpin } from 'naive-ui'
import {
  FlagOutline,
  ChatbubblesOutline,
  ChevronForwardOutline,
} from '@vicons/ionicons5'
import request from '../../api/request'
import { getUnreadCount } from '../../api/message'
import { getAllRequests } from '../../api/friend'
import { pendingIncomingFriendRequestsNotAcknowledged } from '../../utils/friendRequestReminder'
import { useDashboardPolling } from '../../composables/useDashboardPolling'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const stats = reactive({
  pendingReports: 0,
  unreadMessageCount: 0,
  pendingFriendRequestCount: 0,
  totalPendingReports: 0,
})

function getLastRecordedPendingReports(): number | null {
  const stored = localStorage.getItem('admin_last_pending_reports_count')
  return stored ? parseInt(stored, 10) : null
}

function recordCurrentPendingReports(count: number) {
  localStorage.setItem('admin_last_pending_reports_count', count.toString())
}

function getNewPendingReports(currentCount: number): number {
  const lastCount = getLastRecordedPendingReports()
  if (lastCount === null) return currentCount
  if (currentCount < lastCount) {
    recordCurrentPendingReports(currentCount)
    return 0
  }
  return Math.max(0, currentCount - lastCount)
}

async function loadDashboard() {
  loading.value = true
  try {
    const res = await request.get<{ success: boolean; data: { stats?: { pendingReports?: number } } }>(
      '/admin/overview',
    )
    const totalPendingReports = res.data?.stats?.pendingReports || 0
    stats.totalPendingReports = totalPendingReports
    stats.pendingReports = getNewPendingReports(totalPendingReports)

    try {
      const messageRes: any = await getUnreadCount()
      stats.unreadMessageCount = messageRes.data || 0
    } catch {
      stats.unreadMessageCount = 0
    }

    try {
      const reqRes = await getAllRequests()
      stats.pendingFriendRequestCount = pendingIncomingFriendRequestsNotAcknowledged(
        reqRes.data || [],
        'admin',
      )
    } catch {
      stats.pendingFriendRequestCount = 0
    }
  } catch (error) {
    console.warn('获取概览数据失败', error)
  } finally {
    loading.value = false
  }
}

useDashboardPolling(loadDashboard)

watch(
  () => route.path,
  (path, previousPath) => {
    if (path === '/admin' && previousPath && previousPath !== '/admin') {
      void loadDashboard()
    }
  },
)

const messageTotal = computed(
  () => stats.unreadMessageCount + stats.pendingFriendRequestCount,
)

const todos = computed(() => {
  const items: Array<{
    key: string
    label: string
    desc: string
    count: number
    path: string
    icon: typeof FlagOutline
    primary?: boolean
  }> = []
  if (stats.pendingReports > 0 || stats.totalPendingReports > 0) {
    items.push({
      key: 'reports',
      label: '举报待处理',
      desc:
        stats.pendingReports > 0
          ? `新增 ${stats.pendingReports} 条，总量 ${stats.totalPendingReports}`
          : `当前共 ${stats.totalPendingReports} 条待审`,
      count: stats.pendingReports || stats.totalPendingReports,
      path: '/admin/reports',
      icon: FlagOutline,
      primary: true,
    })
  }
  if (messageTotal.value > 0) {
    items.push({
      key: 'messages',
      label: '消息与申请',
      desc: '未读私信或好友申请',
      count: messageTotal.value,
      path: '/admin/messages',
      icon: ChatbubblesOutline,
    })
  }
  return items
})
</script>

<template>
  <div class="dash">
    <NSpin :show="loading">
      <div class="hero glass-panel-strong">
        <div>
          <p class="section-label">Overview</p>
          <h2 class="page-title">工作台</h2>
          <p class="page-sub">
            这里只汇总待办。资料、学生、公告等请从左侧侧栏进入，避免入口重复。
          </p>
        </div>
        <NButton
          v-if="stats.pendingReports > 0 || stats.totalPendingReports > 0"
          type="primary"
          @click="router.push('/admin/reports')"
        >
          去处理举报
        </NButton>
      </div>

      <div v-if="todos.length" class="inbox glass-panel">
        <div class="inbox-head">
          <h3>今日待办</h3>
          <p class="muted">有数字才出现，处理完会自动收起。</p>
        </div>
        <div class="todo-list">
          <button
            v-for="item in todos"
            :key="item.key"
            type="button"
            class="todo surface-card"
            :class="{ primary: item.primary }"
            @click="router.push(item.path)"
          >
            <div class="todo-icon">
              <NIcon :size="20" :component="item.icon" />
            </div>
            <div class="todo-copy">
              <div class="todo-title">
                {{ item.label }}
                <span class="count">{{ item.count }}</span>
              </div>
              <div class="todo-desc">{{ item.desc }}</div>
            </div>
            <NIcon :size="18" :component="ChevronForwardOutline" class="chevron" />
          </button>
        </div>
      </div>

      <div v-else class="empty-inbox glass-panel">
        <p class="muted">暂无待办。侧栏可管理资料、学生与系统公告。</p>
      </div>
    </NSpin>
  </div>
</template>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-end;
  padding: 28px;
  flex-wrap: wrap;
}

.inbox,
.empty-inbox {
  padding: 22px 24px;
}

.inbox-head {
  margin-bottom: 14px;
}

.inbox-head h3 {
  margin: 0 0 4px;
  font-size: 17px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo {
  border: none;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  text-align: left;
  cursor: pointer;
  font: inherit;
  color: inherit;
  transition: transform 0.15s ease, border-color 0.2s ease;
}

.todo:hover {
  transform: translateY(-1px);
  border-color: rgba(122, 158, 142, 0.35);
}

.todo-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(145deg, #8ba3a8, #6f858a);
  flex-shrink: 0;
}

.todo.primary .todo-icon {
  background: linear-gradient(145deg, #c4897e, #a86f66);
}

.todo-copy {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.count {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  background: rgba(122, 158, 142, 0.18);
  color: var(--m-sage-deep);
  font-size: 12px;
  display: inline-grid;
  place-items: center;
}

.todo-desc {
  margin-top: 2px;
  font-size: 13px;
  color: var(--m-ink-soft);
}

.chevron {
  color: var(--m-ink-muted);
}

.empty-inbox {
  text-align: center;
}
</style>
