<script setup lang="ts">
import { NButton, NIcon } from 'naive-ui'
import { ArrowBackOutline } from '@vicons/ionicons5'
import { useRoute, useRouter } from 'vue-router'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    backPath?: string
    /** 默认 false：返回统一放在顶栏，避免重复；需要时再显式打开 */
    showBack?: boolean
  }>(),
  {
    showBack: false,
  },
)

const router = useRouter()
const route = useRoute()

function handleBack() {
  if (props.backPath) {
    router.push(props.backPath)
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  const path = route.path
  if (path.startsWith('/student')) router.push('/student')
  else if (path.startsWith('/admin')) router.push('/admin')
  else router.push('/login')
}
</script>

<template>
  <div class="page-header">
    <div class="left">
      <NButton v-if="showBack" quaternary class="back" @click="handleBack">
        <template #icon>
          <NIcon :component="ArrowBackOutline" :size="18" />
        </template>
        返回
      </NButton>
      <div>
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-sub">{{ subtitle }}</p>
      </div>
    </div>
    <div v-if="$slots.extra" class="extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.back {
  margin-top: 2px;
  background: rgba(255, 255, 255, 0.55) !important;
  border-radius: 999px !important;
  padding: 0 14px !important;
  font-weight: 560;
  color: var(--m-ink) !important;
  flex-shrink: 0;
}

.extra {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
