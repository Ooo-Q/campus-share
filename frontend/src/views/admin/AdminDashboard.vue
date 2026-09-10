<script setup lang="ts">
import { reactive, ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  User,
  School,
  Document,
  Folder,
  ChatDotRound,
  Setting,
  UserFilled,
  Message,
  Bell,
} from '@element-plus/icons-vue'
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

  if (lastCount === null) {
    return currentCount
  }

  if (currentCount < lastCount) {
    recordCurrentPendingReports(currentCount)
    return 0
  }

  return Math.max(0, currentCount - lastCount)
}

async function loadDashboard() {
  loading.value = true
  try {
    const res = await request.get<{ success: boolean; data: { stats?: { pendingReports?: number } } }>('/admin/overview')
    const overviewStats = res.data?.stats || {}

    const totalPendingReports = overviewStats.pendingReports || 0
    stats.pendingReports = getNewPendingReports(totalPendingReports)

    try {
      const messageRes: any = await getUnreadCount()
      stats.unreadMessageCount = messageRes.data || 0
    } catch (error) {
      console.warn('获取未读消息数失败', error)
      stats.unreadMessageCount = 0
    }

    try {
      const reqRes = await getAllRequests()
      const requests = reqRes.data || []
      stats.pendingFriendRequestCount = pendingIncomingFriendRequestsNotAcknowledged(requests, 'admin')
    } catch (error) {
      console.warn('获取好友申请失败', error)
      stats.pendingFriendRequestCount = 0
    }
  } catch (error) {
    console.warn('获取概览数据失败', error)
  } finally {
    loading.value = false
  }
}

const menuCards = computed(() => [
  {
    title: '个人中心',
    desc: '查看个人信息',
    icon: User,
    path: '/admin/profile',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  },
  {
    title: '学生管理',
    desc: '管理学生账号',
    icon: School,
    path: '/admin/students',
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  },
  {
    title: '资料管理',
    desc: '管理所有资料',
    icon: Document,
    path: '/admin/resources',
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  },
  {
    title: '分类管理',
    desc: '管理资料分类',
    icon: Folder,
    path: '/admin/categories',
    color: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  },
  {
    title: '举报管理',
    desc: '处理用户举报',
    icon: ChatDotRound,
    path: '/admin/reports',
    color: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
    badge: stats.pendingReports > 0 ? stats.pendingReports : 0,
  },
  {
    title: '系统公告',
    desc: '发布和管理公告',
    icon: Bell,
    path: '/admin/announcements',
    color: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  },
  {
    title: '好友列表',
    desc: '管理我的好友',
    icon: UserFilled,
    path: '/admin/friends',
    color: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  },
  {
    title: '消息',
    desc: '查看好友消息',
    icon: Message,
    path: '/admin/messages',
    color: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    badge:
      stats.unreadMessageCount + stats.pendingFriendRequestCount > 0
        ? stats.unreadMessageCount + stats.pendingFriendRequestCount
        : 0,
  },
  {
    title: '设置',
    desc: '系统设置',
    icon: Setting,
    path: '/admin/settings',
    color: 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
  },
])

function handleCardClick(path: string) {
  router.push(path)
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
</script>

<template>
  <div class="dashboard-container">
    <div class="menu-grid">
      <div
        v-for="(card, index) in menuCards"
        :key="card.path"
        class="menu-card"
        :style="{ '--delay': index * 0.05 + 's' }"
        @click="handleCardClick(card.path)"
      >
        <div class="card-icon" :style="{ background: card.color }">
          <el-icon :size="32">
            <component :is="card.icon" />
          </el-icon>
        </div>
        <div class="card-content">
          <h3 class="card-title">{{ card.title }}</h3>
          <p class="card-desc">{{ card.desc }}</p>
          <div v-if="card.badge !== undefined && card.badge > 0" class="card-badge">
            {{ card.badge }}
          </div>
        </div>
        <div class="card-arrow">→</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  min-height: calc(100vh - 140px);
  box-sizing: border-box;
  box-sizing: border-box;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(clamp(240px, 25vw, 320px), 1fr));
  gap: clamp(16px, 3vw, 24px);
  animation: fadeIn 0.8s ease;
}

.menu-card {
  background: #ffffff;
  border-radius: clamp(16px, 2.5vw, 20px);
  padding: clamp(20px, 3.5vw, 28px);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 2vw, 16px);
  animation: slideUp 0.5s ease var(--delay, 0s) both;
  min-width: 0;
}

.menu-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--card-color, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
}

.menu-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  border-color: rgba(0, 0, 0, 0.1);
}

.menu-card:hover::before {
  transform: scaleX(1);
}

.card-icon {
  width: clamp(48px, 8vw, 64px);
  height: clamp(48px, 8vw, 64px);
  border-radius: clamp(12px, 2vw, 16px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.card-icon .el-icon {
  font-size: clamp(24px, 4vw, 32px);
}

.menu-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

.card-content {
  flex: 1;
  position: relative;
}

.card-title {
  font-size: clamp(16px, 2.5vw, 20px);
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 clamp(6px, 1vw, 8px) 0;
  letter-spacing: -0.3px;
  word-break: break-word;
}

.card-desc {
  font-size: clamp(12px, 1.75vw, 14px);
  color: #666;
  margin: 0;
  line-height: 1.5;
  word-break: break-word;
}

.card-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(245, 108, 108, 0.4);
  z-index: 1;
}

.card-arrow {
  position: absolute;
  bottom: 28px;
  right: 28px;
  font-size: 20px;
  color: #999;
  transition: all 0.3s ease;
  opacity: 0;
}

.menu-card:hover .card-arrow {
  opacity: 1;
  transform: translateX(4px);
  color: #667eea;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .menu-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .menu-card {
    padding: 16px;
  }
  
  .card-icon {
    width: 48px;
    height: 48px;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .menu-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
  }
  
  .menu-card {
    padding: 20px;
  }
  
  .card-icon {
    width: 56px;
    height: 56px;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .menu-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 20px;
  }
}

@media (min-width: 1920px) {
  .menu-grid {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 32px;
  }
  
  .menu-card {
    padding: 32px;
  }
}
</style>


