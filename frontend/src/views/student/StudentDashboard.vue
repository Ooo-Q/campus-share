<script setup lang="ts">
import { reactive, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  User,
  Document,
  UserFilled,
  Message,
  ChatDotRound,
  Setting,
  Bell,
  Tools,
} from '@element-plus/icons-vue'
import request from '../../api/request'
import { getUnreadCount } from '../../api/message'
import { getAllRequests } from '../../api/friend'
import { getMyPunishments } from '../../api/punishment'
import { pendingIncomingFriendRequestsNotAcknowledged } from '../../utils/friendRequestReminder'
import { getNewAnnouncementCount } from '../../utils/studentAnnouncementReminder'
import { useDashboardPolling } from '../../composables/useDashboardPolling'

const router = useRouter()
const route = useRoute()

const stats = reactive({
  unreadMessageCount: 0,
  pendingFriendRequestCount: 0,
  newPunishmentCount: 0,
  newAnnouncementCount: 0,
})

function getLastRecordedPunishmentCount(): number | null {
  const stored = localStorage.getItem('student_last_punishment_count')
  return stored ? parseInt(stored, 10) : null
}

function getNewPunishmentCount(currentCount: number): number {
  const lastCount = getLastRecordedPunishmentCount()

  if (lastCount === null) {
    return currentCount
  }

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
    } catch (error) {
      console.warn('获取未读消息数失败', error)
      stats.unreadMessageCount = 0
    }

    try {
      const reqRes = await getAllRequests()
      const requests = reqRes.data || []
      stats.pendingFriendRequestCount = pendingIncomingFriendRequestsNotAcknowledged(requests, 'student')
    } catch (error) {
      console.warn('获取好友申请失败', error)
      stats.pendingFriendRequestCount = 0
    }

    try {
      const punishmentRes: any = await getMyPunishments()
      const totalPunishmentCount = punishmentRes.data?.length || 0
      stats.newPunishmentCount = getNewPunishmentCount(totalPunishmentCount)
    } catch (error) {
      console.warn('获取处罚记录数失败', error)
      stats.newPunishmentCount = 0
    }

    try {
      const announcementRes = await request.get<{ success: boolean; data: unknown[] }>('/announcements')
      const totalAnnouncementCount = announcementRes.data?.length || 0
      stats.newAnnouncementCount = getNewAnnouncementCount(totalAnnouncementCount)
    } catch (error) {
      console.warn('获取系统公告数失败', error)
      stats.newAnnouncementCount = 0
    }
  } catch (error) {
    console.error('加载首页统计失败', error)
  }
}

const menuCards = computed(() => [
  {
    title: '个人中心',
    desc: '查看个人信息、上传的资料',
    icon: User,
    path: '/student/profile',
    color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  },
  {
    title: '资料广场',
    desc: '浏览和搜索学习资料',
    icon: Document,
    path: '/student/resources',
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  },
  {
    title: '好友列表',
    desc: '管理我的好友',
    icon: UserFilled,
    path: '/student/friends',
    color: 'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
  },
  {
    title: '消息',
    desc: '查看好友消息和申请',
    icon: Message,
    path: '/student/messages',
    color: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    badge:
      stats.unreadMessageCount + stats.pendingFriendRequestCount > 0
        ? stats.unreadMessageCount + stats.pendingFriendRequestCount
        : 0,
  },
  {
    title: '我的举报',
    desc: '查看举报记录',
    icon: ChatDotRound,
    path: '/student/reports',
    color: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  },
  {
    title: '处罚记录',
    desc: '查看处罚信息',
    icon: Tools,
    path: '/student/punishments',
    color: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    badge: stats.newPunishmentCount > 0 ? stats.newPunishmentCount : 0,
  },
  {
    title: '系统公告',
    desc: '查看平台公告',
    icon: Bell,
    path: '/student/announcements',
    color: 'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
    badge: stats.newAnnouncementCount > 0 ? stats.newAnnouncementCount : 0,
  },
  {
    title: '设置',
    desc: '账户和系统设置',
    icon: Setting,
    path: '/student/settings',
    color: 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
  },
])

function handleCardClick(path: string) {
  router.push(path)
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


