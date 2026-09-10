import { onMounted, onUnmounted } from 'vue'

export function useDashboardPolling(refresh: () => void | Promise<void>, intervalMs = 10000) {
  let pollTimer: ReturnType<typeof setInterval> | null = null

  function stopPolling() {
    if (pollTimer !== null) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function startPolling() {
    stopPolling()
    pollTimer = setInterval(() => {
      void refresh()
    }, intervalMs)
  }

  function handleVisibilityChange() {
    if (document.visibilityState === 'visible') {
      void refresh()
    }
  }

  onMounted(() => {
    void refresh()
    startPolling()
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onUnmounted(() => {
    stopPolling()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return { refresh }
}
