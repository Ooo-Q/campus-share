<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getAvatarUrl } from '../utils/resource'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isHomePage = computed(() => route.path === '/student')

function handleLogout() {
  userStore.clear()
  router.push('/login')
}

function goHome() {
  router.push('/student')
}
</script>

<template>
  <div class="student-layout">
    <header class="top-header">
      <div class="header-content">
        <div class="header-left">
          <div class="logo" @click="goHome">
            <span class="logo-icon">📚</span>
            <div class="logo-text-container">
              <div class="logo-text-main">校园资料分享平台</div>
              <div class="logo-text-sub">学生端</div>
            </div>
          </div>
          <el-button
            v-if="!isHomePage"
            text
            :icon="ArrowLeft"
            @click="router.back()"
            class="back-btn"
          >
            返回上一级
          </el-button>
        </div>
        <div class="header-right">
          <div class="user-info">
            <el-avatar :size="32" :src="getAvatarUrl(userStore.user?.avatar)">
              {{ (userStore.user?.nickname || userStore.user?.username || 'U').slice(0, 1).toUpperCase() }}
            </el-avatar>
            <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          </div>
          <el-button text type="danger" @click="handleLogout">退出</el-button>
        </div>
      </div>
    </header>

    <main class="student-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.student-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f0f8 100%);
  display: flex;
  flex-direction: column;
}

.top-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.03);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: clamp(12px, 2vw, 32px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: clamp(8px, 2vw, 24px);
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  font-size: clamp(20px, 3vw, 28px);
  line-height: 1;
  flex-shrink: 0;
}

.logo-text-container {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.logo-text-main {
  font-size: clamp(14px, 2vw, 18px);
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.logo-text-sub {
  font-size: clamp(12px, 1.5vw, 14px);
  font-weight: 500;
  color: #666;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.back-btn {
  color: #666;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1a1a1a;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.02);
}

.user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.student-main {
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: clamp(20px, 4vw, 40px) clamp(16px, 3vw, 32px);
  overflow-y: auto;
  box-sizing: border-box;
}

@media (max-width: 480px) {
  .header-content {
    padding: 10px 12px;
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-left {
    width: 100%;
    gap: 8px;
    flex-wrap: wrap;
  }
  
  .header-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }
  
  .logo-text-main {
    font-size: 14px;
  }
  
  .logo-text-sub {
    font-size: 12px;
  }
  
  .back-btn {
    padding: 6px 10px;
    font-size: 12px;
  }
  
  .student-main {
    padding: 16px 12px;
  }
  
  .user-name {
    display: none;
  }
  
  .user-info {
    padding: 4px 8px;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .header-content {
    padding: 12px 16px;
  }
  
  .logo-text-main {
    font-size: 16px;
  }
  
  .logo-text-sub {
    font-size: 13px;
  }
  
  .header-left {
    gap: 12px;
  }
  
  .back-btn {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .student-main {
    padding: 20px 16px;
  }
  
  .user-name {
    display: none;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .header-content {
    padding: 14px 24px;
  }
  
  .student-main {
    padding: 32px 24px;
  }
}

@media (min-width: 1920px) {
  .header-content {
    padding: 20px 40px;
  }
  
  .student-main {
    padding: 48px 40px;
  }
}
</style>

