<template>
  <MobilePageShell>
  <div class="mobile-profile-page">
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <div class="profile-avatar">
        <n-avatar
          v-if="userAvatarPreset"
          round
          :size="64"
          :style="{ background: userAvatarPreset.gradient }"
        >
          {{ userAvatarPreset.icon }}
        </n-avatar>
        <n-avatar v-else round :size="64" color="#2563eb">
          {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
        </n-avatar>
      </div>
      <div class="profile-info">
        <div class="profile-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</div>
        <div class="profile-role">{{ userStore.userInfo?.roles?.join(', ') || '普通用户' }}</div>
      </div>
    </div>

    <!-- 功能入口 -->
    <div class="section-title">常用功能</div>
    <div class="menu-list">
      <div class="menu-item" @click="router.push('/m/pages')">
        <div class="menu-icon" style="background: linear-gradient(135deg, #6366f1, #8b5cf6)">
          <n-icon :size="20"><GridOutline /></n-icon>
        </div>
        <span class="menu-label">仪表板页面</span>
        <n-icon :size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
      </div>
      <div class="menu-item" @click="router.push('/m/charts')">
        <div class="menu-icon" style="background: linear-gradient(135deg, #3b82f6, #2563eb)">
          <n-icon :size="20"><BarChartOutline /></n-icon>
        </div>
        <span class="menu-label">图表中心</span>
        <n-icon :size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
      </div>
      <div class="menu-item" @click="router.push('/m/reports')">
        <div class="menu-icon" style="background: linear-gradient(135deg, #8b5cf6, #7c3aed)">
          <n-icon :size="20"><DocumentTextOutline /></n-icon>
        </div>
        <span class="menu-label">报表中心</span>
        <n-icon :size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
      </div>
    </div>

    <div class="section-title">账号</div>
    <div class="menu-list">
      <div class="menu-item" @click="router.push('/change-password')">
        <div class="menu-icon" :style="{ background: 'var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af))' }">
          <n-icon :size="20"><LockClosedOutline /></n-icon>
        </div>
        <span class="menu-label">修改密码</span>
        <n-icon :size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
      </div>
    </div>

    <div class="section-title">数据</div>
    <div class="menu-list">
      <div class="menu-item" @click="handleClearCache">
        <div class="menu-icon" style="background: linear-gradient(135deg, #ef4444, #dc2626)">
          <n-icon :size="20"><TrashOutline /></n-icon>
        </div>
        <span class="menu-label">清除缓存</span>
        <span class="menu-hint">{{ cacheSize }}</span>
        <n-icon :size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section">
      <n-button type="error" ghost block size="large" @click="handleLogout">
        <template #icon><n-icon><LogOutOutline /></n-icon></template>
        退出登录
      </n-button>
    </div>

    <div class="footer-info">
      <div class="footer-version">DataTeaCup v2.1</div>
      <span>© {{ new Date().getFullYear() }} DataTeaCup</span>
    </div>
  </div>
  </MobilePageShell>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import { useRouter } from 'vue-router'
import { NAvatar, NIcon, NButton, useDialog, useMessage } from 'naive-ui'
import {
  LockClosedOutline,
  LogOutOutline,
  ChevronForwardOutline,
  GridOutline,
  BarChartOutline,
  DocumentTextOutline,
  TrashOutline
} from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { avatarPresets } from '@/constants/avatarPresets'
import { resetRouter } from '@/router'

const router = useRouter()
const dialog = useDialog()
const message = useMessage()
const userStore = useUserStore()
const cacheSize = ref('计算中...')

const userAvatarPreset = computed(() => {
  const avatarId = userStore.userInfo?.avatar
  if (!avatarId) return null
  return avatarPresets.find(p => p.id === avatarId) || null
})

function estimateCacheSize(): string {
  let total = 0
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key) total += (localStorage.getItem(key) || '').length
    }
  } catch { /* ignore */ }
  if (total < 1024) return `${total} B`
  if (total < 1024 * 1024) return `${(total / 1024).toFixed(1)} KB`
  return `${(total / 1024 / 1024).toFixed(1)} MB`
}

function handleClearCache() {
  dialog.warning({
    title: '清除缓存',
    content: '将清除本地缓存数据（最近访问记录等），不会影响账号数据。',
    positiveText: '清除',
    negativeText: '取消',
    onPositiveClick: () => {
      const keysToKeep = ['dp_token', 'dp_user', 'dp_theme', 'dp_locale']
      const allKeys: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && !keysToKeep.includes(key)) allKeys.push(key)
      }
      allKeys.forEach(k => localStorage.removeItem(k))
      cacheSize.value = estimateCacheSize()
      message.success('缓存已清除')
    }
  })
}

onMounted(() => {
  cacheSize.value = estimateCacheSize()
})

function handleLogout() {
  dialog.warning({
    title: '确认退出',
    content: '确定要退出登录吗？',
    positiveText: '退出',
    negativeText: '取消',
    onPositiveClick: async () => {
      userStore.logout()
      resetRouter()
      router.push('/login')
    }
  })
}
</script>

<style scoped>
.mobile-profile-page {
  padding: 4px 0;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 20px;
  background: linear-gradient(135deg, #2563eb 0%, #6366f1 50%, #8b5cf6 100%);
  border-radius: 20px;
  color: #fff;
  margin-bottom: 22px;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.2);
}

.profile-info {
  flex: 1;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.2px;
}

.profile-role {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 5px;
  line-height: 1.4;
}

.profile-dept {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 2px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  padding: 0 4px;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.menu-list {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  cursor: pointer;
  transition: transform 0.15s ease, background 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.menu-item:not(:last-child) {
  border-bottom: 1px solid #f1f5f9;
}

.menu-item:active {
  background: #f8fafc;
  transform: scale(0.97);
}

.menu-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.menu-label {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
}

.menu-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-right: 4px;
}

.logout-section {
  margin-top: 12px;
  margin-bottom: 16px;
}

.footer-info {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding: 12px 0;
}

.footer-version {
  font-size: 11px;
  font-weight: 500;
  color: #cbd5e1;
  margin-bottom: 2px;
  letter-spacing: 0.5px;
}

/* 深色模式 */

</style>

<style>
/* MobileProfilePage 深色模式（非 scoped） */
html.dark .mobile-profile-page {
  background: #0f172a !important;
}
html.dark .menu-list {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .menu-item:not(:last-child) {
  border-bottom-color: rgba(255, 255, 255, 0.06) !important;
}
html.dark .menu-item:active {
  background: #334155 !important;
}
html.dark .menu-label {
  color: #e2e8f0 !important;
}
html.dark .menu-hint {
  color: #64748b !important;
}
html.dark .section-title {
  color: #64748b !important;
}
html.dark .footer-info {
  color: #64748b !important;
}
html.dark .footer-version {
  color: #475569 !important;
}
html.dark .profile-card {
  background: linear-gradient(135deg, #1e40af 0%, #4338ca 50%, #6d28d9 100%) !important;
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.3) !important;
}
</style>
