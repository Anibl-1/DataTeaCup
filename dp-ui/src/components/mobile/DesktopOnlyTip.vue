<template>
  <div class="desktop-only-tip">
    <div class="desktop-only-tip-anim">
      <div class="tip-screen">
        <div class="tip-screen-inner">
          <div class="tip-bar"></div>
          <div class="tip-bar short"></div>
          <div class="tip-bar"></div>
        </div>
      </div>
      <div class="tip-phone">
        <n-icon :size="20" color="#94a3b8"><PhonePortraitOutline /></n-icon>
      </div>
      <div class="tip-arrow">
        <n-icon :size="24" color="#2563eb"><ArrowForwardOutline /></n-icon>
      </div>
      <div class="tip-desktop">
        <n-icon :size="28" color="#2563eb"><DesktopOutline /></n-icon>
      </div>
    </div>
    <div class="desktop-only-tip-title">{{ title || '请使用桌面端访问' }}</div>
    <div class="desktop-only-tip-desc">{{ desc || '此功能需要更大的屏幕空间，请在电脑端打开以获得最佳体验。' }}</div>
    <div class="desktop-only-tip-actions">
      <n-button type="primary" round @click="goHome">
        <template #icon><n-icon size="16"><HomeOutline /></n-icon></template>
        返回首页
      </n-button>
      <n-button quaternary round @click="handleSwitchDesktop">
        切换到桌面模式
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { NIcon, NButton } from 'naive-ui'
import { DesktopOutline, PhonePortraitOutline, ArrowForwardOutline, HomeOutline } from '@vicons/ionicons5'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

defineProps<{
  title?: string
  desc?: string
}>()

const router = useRouter()
const appStore = useAppStore()
const goHome = () => router.push('/m/pages')
function handleSwitchDesktop() {
  appStore.setMobileMode('desktop')
  router.replace('/dashboard')
}
</script>

<style scoped>
.desktop-only-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  padding: 40px 24px;
  text-align: center;
  animation: tipFadeIn 0.5s ease;
}

@keyframes tipFadeIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.desktop-only-tip-anim {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  padding: 20px 28px;
  background: #f1f5f9;
  border-radius: 20px;
}

.tip-screen {
  width: 80px;
  height: 56px;
  background: #fff;
  border-radius: 8px;
  border: 2px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.tip-screen::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
}

.tip-screen-inner {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  width: 100%;
}

.tip-bar {
  height: 4px;
  background: #e2e8f0;
  border-radius: 2px;
}

.tip-bar.short { width: 60%; }

.tip-phone {
  width: 36px;
  height: 52px;
  border: 2px solid #cbd5e1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
}

.tip-arrow {
  animation: arrowBounce 1.5s ease-in-out infinite;
}

@keyframes arrowBounce {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(6px); }
}

.tip-desktop {
  width: 52px;
  height: 52px;
  background: rgba(37, 99, 235, 0.08);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(37, 99, 235, 0.2);
}

.desktop-only-tip-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.desktop-only-tip-desc {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  max-width: 300px;
  margin-bottom: 4px;
}

.desktop-only-tip-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 24px;
  width: 100%;
  max-width: 240px;
}

</style>

<style>
/* DesktopOnlyTip 深色模式（非 scoped） */
html.dark .desktop-only-tip-anim { background: #1e293b !important; }
html.dark .tip-screen { background: #334155 !important; border-color: #475569 !important; }
html.dark .tip-screen::after { background: #475569 !important; }
html.dark .tip-bar { background: #475569 !important; }
html.dark .tip-phone { border-color: #475569 !important; }
html.dark .tip-desktop { background: rgba(96, 165, 250, 0.1) !important; border-color: rgba(96, 165, 250, 0.25) !important; }
html.dark .desktop-only-tip-title { color: #e2e8f0 !important; }
html.dark .desktop-only-tip-desc { color: #94a3b8 !important; }
</style>
