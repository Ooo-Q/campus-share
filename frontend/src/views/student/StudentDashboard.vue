<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton, NIcon } from 'naive-ui'
import {
  LibraryOutline,
  ChatbubblesOutline,
  MegaphoneOutline,
  AlertCircleOutline,
  ChevronForwardOutline,
} from '@vicons/ionicons5'
import request from '../../api/request'
import { getUnreadCount } from '../../api/message'
import { getAllRequests } from '../../api/friend'
import { getMyPunishments } from '../../api/punishment'
import { pendingIncomingFriendRequestsNotAcknowledged } from '../../utils/friendRequestReminder'
import { getNewAnnouncementCount } from '../../utils/studentAnnouncementReminder'
import { useDashboardPolling } from '../../composables/useDashboardPolling'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const stats = reactive({
  unreadMessageCount: 0,
  pendingFriendRequestCount: 0,
  newPunishmentCount: 0,
  newAnnouncementCount: 0,
})

const displayName = computed(
  () => userStore.user?.nickname || userStore.user?.username || '同学',
)

const messageTotal = computed(
  () => stats.unreadMessageCount + stats.pendingFriendRequestCount,
)

const alerts = computed(() => {
  const items: Array<{
    key: string
    label: string
    desc: string
    count: number
    path: string
    icon: typeof ChatbubblesOutline
    tone: string
  }> = []
  if (messageTotal.value > 0) {
    items.push({
      key: 'messages',
      label: '消息待处理',
      desc: '未读私信或好友申请',
      count: messageTotal.value,
      path: '/student/messages',
      icon: ChatbubblesOutline,
      tone: 'sage',
    })
  }
  if (stats.newAnnouncementCount > 0) {
    items.push({
      key: 'announcements',
      label: '新公告',
      desc: '平台有未读通知',
      count: stats.newAnnouncementCount,
      path: '/student/announcements',
      icon: MegaphoneOutline,
      tone: 'mist',
    })
  }
  if (stats.newPunishmentCount > 0) {
    items.push({
      key: 'punishments',
      label: '处罚提醒',
      desc: '请查看最新处罚记录',
      count: stats.newPunishmentCount,
      path: '/student/punishments',
      icon: AlertCircleOutline,
      tone: 'peach',
    })
  }
  return items
})

function getLastRecordedPunishmentCount(): number | null {
  const stored = localStorage.getItem('student_last_punishment_count')
  return stored ? parseInt(stored, 10) : null
}

function getNewPunishmentCount(currentCount: number): number {
  const lastCount = getLastRecordedPunishmentCount()
  if (lastCount === null) return currentCount
  if (currentCount < lastCount) {
    localStorage.setItem('student_last_punishment_count', currentCount.toString())
    return 0
  }
  return Math.max(0, currentCount - lastCount)
}

async function loadDashboardStats() {
  try {
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
        'student',
      )
    } catch {
      stats.pendingFriendRequestCount = 0
    }

    try {
      const punishmentRes: any = await getMyPunishments()
      stats.newPunishmentCount = getNewPunishmentCount(punishmentRes.data?.length || 0)
    } catch {
      stats.newPunishmentCount = 0
    }

    try {
      const announcementRes = await request.get<{ success: boolean; data: unknown[] }>('/announcements')
      stats.newAnnouncementCount = getNewAnnouncementCount(announcementRes.data?.length || 0)
    } catch {
      stats.newAnnouncementCount = 0
    }
  } catch (error) {
    console.error('加载首页统计失败', error)
  }
}

useDashboardPolling(loadDashboardStats)

watch(
  () => route.path,
  (path, previousPath) => {
    if (path === '/student' && previousPath && previousPath !== '/student') {
      void loadDashboardStats()
    }
  },
)
</script>

<template>
  <div class="dash">
    <div class="hero glass-panel-strong">
      <div>
        <p class="section-label">Campus Share</p>
        <h2 class="page-title">你好，{{ displayName }}</h2>
        <p class="page-sub">从资料广场浏览同学分享，需要发布时用左侧「上传资料」。</p>
      </div>
      <NButton type="primary" size="large" @click="router.push('/student/resources')">
        <template #icon><NIcon :component="LibraryOutline" /></template>
        进入资料广场
      </NButton>
    </div>

    <div v-if="alerts.length" class="inbox glass-panel">
      <div class="inbox-head">
        <h3>需要关注</h3>
        <p class="muted">只展示有待办的通知，其它入口在侧栏。</p>
      </div>
      <div class="alert-list">
        <button
          v-for="item in alerts"
          :key="item.key"
          type="button"
          class="alert surface-card"
          :class="item.tone"
          @click="router.push(item.path)"
        >
          <div class="alert-icon">
            <NIcon :size="20" :component="item.icon" />
          </div>
          <div class="alert-copy">
            <div class="alert-title">
              {{ item.label }}
              <span class="count">{{ item.count }}</span>
            </div>
            <div class="alert-desc">{{ item.desc }}</div>
          </div>
          <NIcon :size="18" :component="ChevronForwardOutline" class="chevron" />
        </button>
      </div>
    </div>

    <div v-else class="empty-inbox glass-panel">
      <p class="muted">暂时没有待办。去资料广场看看有没有新分享吧。</p>
    </div>
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

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert {
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

.alert:hover {
  transform: translateY(-1px);
  border-color: rgba(122, 158, 142, 0.35);
}

.alert-icon {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  flex-shrink: 0;
}

.alert.sage .alert-icon {
  background: linear-gradient(145deg, #7a9e8e, #5c8070);
}

.alert.mist .alert-icon {
  background: linear-gradient(145deg, #8ba3a8, #6f858a);
}

.alert.peach .alert-icon {
  background: linear-gradient(145deg, #c4a484, #a88968);
}

.alert-copy {
  flex: 1;
  min-width: 0;
}

.alert-title {
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

.alert-desc {
  margin-top: 2px;
  font-size: 13px;
  color: var(--m-ink-soft);
}

.chevron {
  color: var(--m-ink-muted);
  flex-shrink: 0;
}

.empty-inbox {
  text-align: center;
}
</style>
