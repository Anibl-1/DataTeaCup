<template>
  <div class="mobile-mode-switcher" :class="{ 'is-expanded': expanded }">
    <div class="switcher-trigger" @click="expanded = !expanded">
      <n-icon :size="18">
        <PhonePortraitOutline v-if="isMobileView" />
        <DesktopOutline v-else />
      </n-icon>
    </div>
    <transition name="switcher-menu">
      <div v-if="expanded" class="switcher-menu">
        <div
          class="switcher-option"
          :class="{ active: mobileMode === 'auto' }"
          @click="handleSelect('auto')"
        >
          <n-icon :size="16"><SyncOutline /></n-icon>
          <span>自动</span>
        </div>
        <div
          class="switcher-option"
          :class="{ active: mobileMode === 'mobile' }"
          @click="handleSelect('mobile')"
        >
          <n-icon :size="16"><PhonePortraitOutline /></n-icon>
          <span>移动端</span>
        </div>
        <div
          class="switcher-option"
          :class="{ active: mobileMode === 'desktop' }"
          @click="handleSelect('desktop')"
        >
          <n-icon :size="16"><DesktopOutline /></n-icon>
          <span>桌面端</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { NIcon } from 'naive-ui'
import { PhonePortraitOutline, DesktopOutline, SyncOutline } from '@vicons/ionicons5'
import { useMobileMode } from '@/composables/useMobileMode'

const { isMobileView, mobileMode, setMode } = useMobileMode()
const expanded = ref(false)

function handleSelect(mode: 'auto' | 'mobile' | 'desktop') {
  setMode(mode)
  expanded.value = false
}
</script>

<style scoped>
.mobile-mode-switcher {
  position: fixed;
  left: 12px;
  bottom: calc(72px + env(safe-area-inset-bottom, 0px));
  z-index: 999;
  transition: opacity 0.3s ease;
}

.switcher-trigger {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(37, 99, 235, 0.85);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.3);
  transition: all 0.3s ease;
  -webkit-tap-highlight-color: transparent;
  opacity: 0.7;
}

.switcher-trigger:hover {
  opacity: 1;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
}

.switcher-trigger:active {
  transform: scale(0.95);
}

.switcher-menu {
  position: absolute;
  bottom: 50px;
  left: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  padding: 6px;
  min-width: 120px;
  overflow: hidden;
}

.switcher-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #475569;
  transition: all 0.2s;
  white-space: nowrap;
}

.switcher-option:hover {
  background: #f1f5f9;
  color: #1e293b;
}

.switcher-option.active {
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-weight: 600;
}

.switcher-menu-enter-active,
.switcher-menu-leave-active {
  transition: all 0.2s ease;
}
.switcher-menu-enter-from,
.switcher-menu-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}

</style>

<style>
/* MobileModeSwitcher 深色模式（非 scoped） */
html.dark .switcher-menu {
  background: #1e293b !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.4) !important;
}
html.dark .switcher-option {
  color: #94a3b8 !important;
}
html.dark .switcher-option:hover {
  background: #334155 !important;
  color: #e2e8f0 !important;
}
html.dark .switcher-option.active {
  background: rgba(96, 165, 250, 0.12) !important;
  color: #60a5fa !important;
}
</style>
