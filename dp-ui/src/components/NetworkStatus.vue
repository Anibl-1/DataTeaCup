<template>
  <div class="network-status">
    <Transition name="slide">
      <div v-if="showOffline" class="network-offline">
        <n-icon size="18"><CloudOfflineOutline /></n-icon>
        <span>网络已断开，请检查网络连接</span>
      </div>
    </Transition>
    <Transition name="slide">
      <div v-if="showReconnected" class="network-reconnected">
        <n-icon size="18"><CloudDoneOutline /></n-icon>
        <span>网络已恢复</span>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { NIcon } from 'naive-ui'
import { CloudOfflineOutline, CloudDoneOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()

const showOffline = ref(!navigator.onLine)
const showReconnected = ref(false)
let reconnectedTimer: ReturnType<typeof setTimeout> | null = null

function handleOffline() {
  appStore.isOnline = false
  showOffline.value = true
  showReconnected.value = false
  if (reconnectedTimer) {
    clearTimeout(reconnectedTimer)
    reconnectedTimer = null
  }
}

function handleOnline() {
  appStore.isOnline = true
  showOffline.value = false
  showReconnected.value = true
  reconnectedTimer = setTimeout(() => {
    showReconnected.value = false
  }, 3000)
}

onMounted(() => {
  showOffline.value = !navigator.onLine
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  if (reconnectedTimer) {
    clearTimeout(reconnectedTimer)
  }
})
</script>

<style scoped>
.network-offline {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #ff6b6b, #ee5a5a);
  color: white;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  z-index: 10000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.network-reconnected {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #51cf66, #40c057);
  color: white;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  z-index: 10000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateY(-100%);
}
</style>
