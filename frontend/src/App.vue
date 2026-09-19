<script setup lang="ts">
import { NConfigProvider, NDialogProvider, NMessageProvider, NNotificationProvider } from 'naive-ui'
import { morandiTheme } from './theme/morandi'
</script>

<template>
  <NConfigProvider :theme-overrides="morandiTheme">
    <NMessageProvider>
      <NDialogProvider>
        <NNotificationProvider>
          <div class="app-shell">
            <router-view />
          </div>
        </NNotificationProvider>
      </NDialogProvider>
    </NMessageProvider>
  </NConfigProvider>
</template>

<style scoped>
.app-shell {
  min-height: 100dvh;
}
</style>
