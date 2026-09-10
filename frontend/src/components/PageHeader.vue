<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const props = defineProps<{
  title: string
  backPath?: string
  showBack?: boolean
}>()

const router = useRouter()

function handleBack() {
  if (props.backPath) {
    router.push(props.backPath)
  } else {
    const path = router.currentRoute.value.path
    if (path.startsWith('/student')) {
      router.push('/student')
    } else if (path.startsWith('/admin')) {
      router.push('/admin')
    } else {
      router.push('/login')
    }
  }
}
</script>

<template>
  <div class="page-header">
    <div v-if="showBack !== false" class="back-button" @click="handleBack">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回</span>
    </div>
    <h1 class="page-title">{{ title }}</h1>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  gap: clamp(12px, 2vw, 16px);
  margin-bottom: clamp(20px, 4vw, 32px);
  padding-bottom: clamp(16px, 2.5vw, 20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.back-button {
  display: flex;
  align-items: center;
  gap: clamp(4px, 1vw, 6px);
  padding: clamp(6px, 1.5vw, 16px);
  border-radius: clamp(8px, 1.25vw, 10px);
  background: rgba(0, 0, 0, 0.03);
  color: #666;
  font-size: clamp(12px, 1.75vw, 14px);
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  white-space: nowrap;
  flex-shrink: 0;
}

.back-button:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1a1a1a;
  border-color: rgba(0, 0, 0, 0.08);
  transform: translateX(-2px);
}

.page-title {
  font-size: clamp(20px, 3.5vw, 28px);
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.5px;
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

@media (max-width: 480px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .page-title {
    font-size: 20px;
    width: 100%;
  }
  
  .back-button {
    padding: 6px 12px;
    font-size: 12px;
  }
}

@media (min-width: 481px) and (max-width: 768px) {
  .page-title {
    font-size: 24px;
  }
  
  .back-button {
    padding: 6px 12px;
    font-size: 13px;
  }
}
</style>

