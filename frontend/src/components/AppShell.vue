<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NBadge, NButton, NDrawer, NIcon, NTooltip } from 'naive-ui'
import {
  MenuOutline,
  LogOutOutline,
  CloseOutline,
  ChatbubblesOutline,
  ArrowBackOutline,
} from '@vicons/ionicons5'
import { useUserStore } from '../stores/user'
import { getProfile } from '../api/auth'
import UserAvatar from './UserAvatar.vue'
import { getUnreadCount } from '../api/message'
import { getAllRequests } from '../api/friend'
import { getMyPunishments } from '../api/punishment'
import request from '../api/request'
import { pendingIncomingFriendRequestsNotAcknowledged } from '../utils/friendRequestReminder'
import { getNewAnnouncementCount } from '../utils/studentAnnouncementReminder'
import { useDashboardPolling } from '../composables/useDashboardPolling'
import {
  adminNav,
  studentNav,
  adminMobileTabs,
  studentMobileTabs,
  matchNavPath,
  type NavGroup,
  type NavItem,
} from '../config/navigation'

const props = defineProps<{
  role: 'ADMIN' | 'STUDENT'
}>()

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const mobileOpen = ref(false)

const badges = reactive({
  messages: 0,
  reports: 0,
  punishments: 0,
  announcements: 0,
})

const navGroups = computed<NavGroup[]>(() => (props.role === 'ADMIN' ? adminNav : studentNav))
const mobileTabs = computed<NavItem[]>(() => (props.role === 'ADMIN' ? adminMobileTabs : studentMobileTabs))
const homePath = computed(() => (props.role === 'ADMIN' ? '/admin' : '/student'))
const roleLabel = computed(() => (props.role === 'ADMIN' ? '管理端' : '学生端'))
const pageTitle = computed(() => (route.meta.title as string) || '')
const isHome = computed(() => route.path === homePath.value)
const messagesPath = computed(() => (props.role === 'ADMIN' ? '/admin/messages' : '/student/messages'))

const displayName = computed(
  () => userStore.user?.nickname || userStore.user?.username || '用户',
)

const profilePath = computed(() =>
  props.role === 'ADMIN' ? '/admin/profile' : '/student/profile',
)

function handleBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  go(homePath.value)
}

async function syncCurrentUser() {
  try {
    const res: any = await getProfile()
    if (res?.success && res.data) {
      userStore.patchUser({
        nickname: res.data.nickname,
        username: res.data.username,
        avatar: res.data.avatar || undefined,
        role: res.data.role,
      })
    }
  } catch {
    /* ignore */
  }
}

function badgeValue(key?: NavItem['badgeKey']): number {
  if (!key) return 0
  return badges[key] || 0
}

function isActive(path: string) {
  return matchNavPath(route.path, path)
}

function go(path: string) {
  mobileOpen.value = false
  if (route.path !== path) router.push(path)
}

function handleLogout() {
  userStore.clear()
  router.push('/login')
}

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

async function refreshBadges() {
  const scope = props.role === 'ADMIN' ? 'admin' : 'student'
  try {
    const messageRes: any = await getUnreadCount()
    const unread = messageRes.data || 0
    let friendPending = 0
    try {
      const reqRes = await getAllRequests()
      friendPending = pendingIncomingFriendRequestsNotAcknowledged(reqRes.data || [], scope)
    } catch {
      friendPending = 0
    }
    badges.messages = unread + friendPending
  } catch {
    badges.messages = 0
  }

  if (props.role === 'ADMIN') {
    try {
      const res = await request.get<{ success: boolean; data: { stats?: { pendingReports?: number } } }>(
        '/admin/overview',
      )
      const total = res.data?.stats?.pendingReports || 0
      badges.reports = getNewPendingReports(total)
    } catch {
      badges.reports = 0
    }
  } else {
    try {
      const punishmentRes: any = await getMyPunishments()
      badges.punishments = getNewPunishmentCount(punishmentRes.data?.length || 0)
    } catch {
      badges.punishments = 0
    }
    try {
      const announcementRes = await request.get<{ success: boolean; data: unknown[] }>('/announcements')
      badges.announcements = getNewAnnouncementCount(announcementRes.data?.length || 0)
    } catch {
      badges.announcements = 0
    }
  }
}

useDashboardPolling(refreshBadges, 12000)

watch(
  () => route.path,
  () => {
    mobileOpen.value = false
  },
)

onMounted(() => {
  void refreshBadges()
  void syncCurrentUser()
})
</script>

<template>
  <div class="shell">
    <div class="app-wash" aria-hidden="true" />

    <aside class="sidebar glass-panel-strong">
      <div class="brand" @click="go(homePath)">
        <div class="brand-mark">CS</div>
        <div class="brand-copy">
          <div class="brand-name">Campus Share</div>
          <div class="brand-role">{{ roleLabel }}</div>
        </div>
      </div>

      <nav class="nav">
        <div v-for="(group, gi) in navGroups" :key="gi" class="nav-group">
          <div v-if="group.label" class="nav-group-label">{{ group.label }}</div>
          <button
            v-for="item in group.items"
            :key="item.key"
            type="button"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            @click="go(item.path)"
          >
            <NIcon :size="20" :component="item.icon" />
            <span class="nav-label">{{ item.label }}</span>
            <NBadge
              v-if="badgeValue(item.badgeKey) > 0"
              :value="badgeValue(item.badgeKey)"
              :max="99"
              class="nav-badge"
            />
          </button>
        </div>
      </nav>

      <div class="sidebar-foot">
        <div class="user-chip" @click="go(profilePath)">
          <UserAvatar :src="userStore.user?.avatar" :name="displayName" :size="36" />
          <div class="user-meta">
            <div class="user-name">{{ displayName }}</div>
            <div class="user-mail">{{ userStore.user?.username }}</div>
          </div>
        </div>
        <NTooltip trigger="hover">
          <template #trigger>
            <NButton quaternary circle @click="handleLogout">
              <template #icon>
                <NIcon :component="LogOutOutline" />
              </template>
            </NButton>
          </template>
          退出登录
        </NTooltip>
      </div>
    </aside>

    <div class="main-wrap">
      <header class="topbar glass-panel">
        <div class="topbar-left">
          <NButton class="menu-btn" quaternary circle @click="mobileOpen = true">
            <template #icon>
              <NIcon :component="MenuOutline" :size="22" />
            </template>
          </NButton>
          <NButton
            v-if="!isHome"
            quaternary
            class="back-btn"
            @click="handleBack"
          >
            <template #icon>
              <NIcon :component="ArrowBackOutline" :size="18" />
            </template>
            返回
          </NButton>
          <div>
            <div class="greeting">{{ pageTitle || (role === 'ADMIN' ? '工作台' : '首页') }}</div>
            <div v-if="isHome" class="greeting-sub">你好，{{ displayName }}</div>
          </div>
        </div>
        <div class="topbar-right">
          <NTooltip trigger="hover">
            <template #trigger>
              <NButton quaternary circle class="msg-btn" @click="go(messagesPath)">
                <NBadge :value="badges.messages" :max="99" :show-zero="false">
                  <NIcon :size="20" :component="ChatbubblesOutline" />
                </NBadge>
              </NButton>
            </template>
            消息
          </NTooltip>
          <button type="button" class="top-avatar" @click="go(profilePath)" :title="displayName">
            <UserAvatar :src="userStore.user?.avatar" :name="displayName" :size="36" />
          </button>
          <NButton quaternary @click="handleLogout" class="logout-desktop">退出</NButton>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>

    <nav class="mobile-tabs glass-panel-strong">
      <button
        v-for="tab in mobileTabs"
        :key="tab.key"
        type="button"
        class="tab-item"
        :class="{ active: isActive(tab.path) }"
        @click="go(tab.path)"
      >
        <NBadge :value="badgeValue(tab.badgeKey)" :max="99" :show-zero="false">
          <NIcon :size="22" :component="tab.icon" />
        </NBadge>
        <span>{{ tab.label }}</span>
      </button>
    </nav>

    <NDrawer v-model:show="mobileOpen" placement="left" :width="280" :show-mask="true">
      <div class="drawer-inner">
        <div class="drawer-head">
          <div class="brand" @click="go(homePath)">
            <div class="brand-mark">CS</div>
            <div class="brand-copy">
              <div class="brand-name">Campus Share</div>
              <div class="brand-role">{{ roleLabel }}</div>
            </div>
          </div>
          <NButton quaternary circle @click="mobileOpen = false">
            <template #icon>
              <NIcon :component="CloseOutline" />
            </template>
          </NButton>
        </div>
        <nav class="nav">
          <div v-for="(group, gi) in navGroups" :key="gi" class="nav-group">
            <div v-if="group.label" class="nav-group-label">{{ group.label }}</div>
            <button
              v-for="item in group.items"
              :key="item.key"
              type="button"
              class="nav-item"
              :class="{ active: isActive(item.path) }"
              @click="go(item.path)"
            >
              <NIcon :size="20" :component="item.icon" />
              <span class="nav-label">{{ item.label }}</span>
              <NBadge
                v-if="badgeValue(item.badgeKey) > 0"
                :value="badgeValue(item.badgeKey)"
                :max="99"
                class="nav-badge"
              />
            </button>
          </div>
        </nav>
      </div>
    </NDrawer>
  </div>
</template>

<style scoped>
.shell {
  position: relative;
  min-height: 100dvh;
  display: grid;
  grid-template-columns: var(--sidebar-w) 1fr;
  gap: 18px;
  padding: 18px;
  z-index: 1;
}

.sidebar {
  position: sticky;
  top: 18px;
  height: calc(100dvh - 36px);
  display: flex;
  flex-direction: column;
  padding: 22px 16px;
  z-index: 2;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px 8px 18px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #fff;
  background: linear-gradient(145deg, var(--m-sage) 0%, var(--m-sage-deep) 100%);
  box-shadow: 0 10px 24px rgba(92, 128, 112, 0.28);
}

.brand-name {
  font-size: 15px;
  font-weight: 650;
  letter-spacing: -0.01em;
}

.brand-role {
  font-size: 12px;
  color: var(--m-ink-soft);
  margin-top: 2px;
}

.nav {
  flex: 1;
  overflow-y: auto;
  padding: 4px 4px 12px;
}

.nav-group + .nav-group {
  margin-top: 14px;
}

.nav-group-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--m-ink-muted);
  padding: 0 12px 8px;
}

.nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  color: var(--m-ink-soft);
  border-radius: 16px;
  padding: 11px 12px;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.15s ease;
  font: inherit;
  text-align: left;
}

.nav-item:hover {
  background: rgba(122, 158, 142, 0.12);
  color: var(--m-ink);
}

.nav-item.active {
  background: rgba(122, 158, 142, 0.22);
  color: var(--m-sage-deep);
  font-weight: 600;
}

.nav-item:active {
  transform: scale(0.98);
}

.nav-label {
  flex: 1;
  font-size: 14px;
}

.nav-badge {
  margin-left: auto;
}

.sidebar-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--m-stroke);
}

.user-chip {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 6px;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.user-chip:hover {
  background: rgba(122, 158, 142, 0.1);
}

.user-meta {
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-mail {
  font-size: 11px;
  color: var(--m-ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.main-wrap {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 8px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: var(--header-h);
  padding: 14px 22px;
  position: sticky;
  top: 18px;
  z-index: 5;
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.greeting {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.greeting-sub {
  font-size: 13px;
  color: var(--m-ink-soft);
  margin-top: 2px;
}

.menu-btn {
  display: none;
}

.back-btn {
  background: rgba(255, 255, 255, 0.5) !important;
  border-radius: 999px !important;
  padding: 0 14px !important;
  color: var(--m-ink) !important;
  font-weight: 560;
}

.top-avatar {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  border-radius: 999px;
  line-height: 0;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

.top-avatar:hover {
  transform: scale(1.04);
  box-shadow: 0 0 0 3px rgba(122, 158, 142, 0.28);
}

.content {
  flex: 1;
  min-height: 0;
  padding-bottom: 12px;
}

.mobile-tabs {
  display: none;
}

.drawer-inner {
  padding: 18px 12px;
  height: 100%;
  background: rgba(244, 241, 235, 0.92);
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

@media (max-width: 1024px) {
  .shell {
    grid-template-columns: 1fr;
    padding: 12px 12px 88px;
    gap: 12px;
  }

  .sidebar {
    display: none;
  }

  .menu-btn {
    display: inline-flex;
  }

  .logout-desktop {
    display: none;
  }

  .topbar {
    top: 12px;
    padding: 12px 14px;
  }

  .mobile-tabs {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    position: fixed;
    left: 12px;
    right: 12px;
    bottom: 12px;
    z-index: 40;
    padding: 8px 6px;
    gap: 2px;
  }

  .tab-item {
    border: none;
    background: transparent;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 8px 4px;
    color: var(--m-ink-muted);
    font-size: 11px;
    border-radius: 14px;
    cursor: pointer;
  }

  .tab-item.active {
    color: var(--m-sage-deep);
    background: rgba(122, 158, 142, 0.16);
    font-weight: 600;
  }
}
</style>
