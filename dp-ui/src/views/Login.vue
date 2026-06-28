<template>
  <div class="login-container">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <!-- 装饰性背景元素 -->
      <div class="bg-decoration">
        <div class="bg-circle bg-circle-1"></div>
        <div class="bg-circle bg-circle-2"></div>
        <div class="bg-circle bg-circle-3"></div>
      </div>
      
      <div class="brand-section">
        <div class="brand-logo">
          <div class="logo-icon">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <path d="M24 4L4 14v20l20 10 20-10V14L24 4z" stroke="currentColor" stroke-width="2" fill="none"/>
              <path d="M4 14l20 10 20-10" stroke="currentColor" stroke-width="2"/>
              <path d="M24 44V24" stroke="currentColor" stroke-width="2"/>
              <circle cx="24" cy="24" r="6" fill="currentColor" opacity="0.3"/>
              <circle cx="24" cy="24" r="3" fill="currentColor"/>
            </svg>
          </div>
          <span class="logo-text">DataTeaCup</span>
        </div>
        
        <h1 class="brand-headline">{{ t('login.brandSlogan') }}</h1>
        
        <!-- 功能亮点 -->
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon feature-icon-ai">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 017 7h1.27c.34-.6.99-1 1.73-1a2 2 0 110 4c-.74 0-1.39-.4-1.73-1H21a7 7 0 01-7 7v1.27c.6.34 1 .99 1 1.73a2 2 0 11-4 0c0-.74.4-1.39 1-1.73V23a7 7 0 01-7-7H3.73c-.34.6-.99 1-1.73 1a2 2 0 110-4c.74 0 1.39.4 1.73 1H5a7 7 0 017-7V5.73c-.6-.34-1-.99-1-1.73a2 2 0 012-2z" stroke-width="1.5"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>{{ t('login.feature1Title') }}</h3>
              <p>{{ t('login.feature1Desc') }}</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon feature-icon-design">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <rect x="3" y="3" width="7" height="7" rx="1" stroke-width="1.5"/>
                <rect x="14" y="3" width="7" height="7" rx="1" stroke-width="1.5"/>
                <rect x="3" y="14" width="7" height="7" rx="1" stroke-width="1.5"/>
                <rect x="14" y="14" width="7" height="7" rx="1" stroke-width="1.5"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>{{ t('login.feature2Title') }}</h3>
              <p>{{ t('login.feature2Desc') }}</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon feature-icon-data">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M21 12c0 1.66-4.03 3-9 3s-9-1.34-9-3" stroke-width="1.5"/>
                <path d="M3 5v14c0 1.66 4.03 3 9 3s9-1.34 9-3V5" stroke-width="1.5"/>
                <ellipse cx="12" cy="5" rx="9" ry="3" stroke-width="1.5"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>{{ t('login.feature3Title') }}</h3>
              <p>{{ t('login.feature3Desc') }}</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon feature-icon-security">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke-width="1.5"/>
                <path d="M9 12l2 2 4-4" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="feature-content">
              <h3>{{ t('login.feature4Title') }}</h3>
              <p>{{ t('login.feature4Desc') }}</p>
            </div>
          </div>
        </div>
        
        <!-- 统计数字 -->
        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-value">10+</span>
            <span class="stat-label">{{ t('login.stats1') }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">50+</span>
            <span class="stat-label">{{ t('login.stats2') }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">100+</span>
            <span class="stat-label">{{ t('login.stats3') }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">20+</span>
            <span class="stat-label">{{ t('login.stats4') }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="login-card-wrapper">
        <div class="login-card">
          <div class="login-header">
            <div class="header-top">
              <div class="language-switcher">
                <n-button
                  text
                  size="small"
                  :type="locale === 'zh-CN' ? 'primary' : 'default'"
                  @click="setLocale('zh-CN')"
                >
                  中文
                </n-button>
                <span class="divider">|</span>
                <n-button
                  text
                  size="small"
                  :type="locale === 'en-US' ? 'primary' : 'default'"
                  @click="setLocale('en-US')"
                >
                  EN
                </n-button>
              </div>
            </div>
            <div class="welcome-icon">
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="#18a058" stroke-width="2"/>
                <path d="M12 6v6l4 2" stroke="#18a058" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <h2>{{ t('login.title') }}</h2>
            <p>{{ t('login.subtitle') }}</p>
          </div>
          
          <!-- 登录失败错误提示 -->
          <div v-if="loginError" class="login-error">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="15" y1="9" x2="9" y2="15"/>
              <line x1="9" y1="9" x2="15" y2="15"/>
            </svg>
            <span>{{ loginError }}</span>
          </div>

          <n-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            @submit.prevent="handleSubmit"
          >
            <n-form-item path="username">
              <n-input
                v-model:value="form.username"
                :placeholder="t('login.username')"
                @keydown.enter="handleEnterKey"
              >
                <template #prefix>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" opacity="0.5">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <circle cx="12" cy="7" r="4" stroke-width="2"/>
                  </svg>
                </template>
              </n-input>
            </n-form-item>
            
            <n-form-item path="password">
              <n-input
                v-model:value="form.password"
                type="password"
                :placeholder="t('login.password')"
                show-password-on="click"
                @keydown.enter="handleEnterKey"
              >
                <template #prefix>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" opacity="0.5">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" stroke-width="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </template>
              </n-input>
            </n-form-item>
            
            <n-form-item>
              <n-button
                type="primary"
                attr-type="submit"
                block
                :loading="loading"
                size="large"
                class="login-button"
              >
                {{ t('login.signIn') }}
              </n-button>
            </n-form-item>
          </n-form>
          
          <div class="login-footer">
            <p>{{ t('login.copyright') }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { useI18n, initLocale } from '@/i18n'

const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
// initMessage 已在 SessionManager.vue 中初始化，此处无需重复

const { locale, t, setLocale } = useI18n()

const formRef = ref<FormInst | null>(null)
const loading = ref(false)
const loginError = ref('')

const form = reactive({
  username: '',
  password: ''
})

const rules = computed(() => ({
  username: {
    required: true,
    message: t('login.usernameRequired'),
    trigger: 'blur'
  },
  password: {
    required: true,
    message: t('login.passwordRequired'),
    trigger: 'blur'
  }
}))

// 回车键登录
const handleEnterKey = () => {
  handleSubmit()
}

const handleSubmit = async () => {
  if (!formRef.value || loading.value) return // 防止重复提交
  
  try {
    await formRef.value.validate()
  } catch {
    return // 验证失败，直接返回
  }
  
  loading.value = true
  loginError.value = ''
  try {
    await userStore.loginAction(form)
    
    // 检查是否需要强制修改密码
    if (userStore.mustChangePassword) {
      message.warning('首次登录或密码已被重置，请先修改密码')
      router.replace('/change-password')
      return
    }
    
    message.success(t('login.loginSuccess'))
        
    // 获取重定向路径，验证其安全性
    const redirect = router.currentRoute.value.query['redirect'] as string
    // 统一设备判断：优先读取用户手动设定的模式
    const mobileMode = localStorage.getItem('dp-mobile-mode')
    const isMobileDevice = mobileMode === 'mobile' ? true : mobileMode === 'desktop' ? false : window.innerWidth < 768
    let targetPath = isMobileDevice ? '/m/pages' : '/dashboard'
    
    // 验证redirect参数的安全性，防止开放重定向漏洞
    if (redirect) {
      if (redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.startsWith('http')) {
        targetPath = redirect
      }
    }
    
    router.replace(targetPath)
  } catch (error: any) {
    loginError.value = error?.message || error?.msg || t('login.loginFailed')
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initLocale()
})
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #FFFFFF;
}

/* ========== 左侧品牌区域 ========== */
.login-left {
  flex: 1.2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1628 0%, #0d2137 25%, #163a5f 50%, #0d2137 75%, #0a1628 100%);
  background-size: 400% 400%;
  animation: gradientFlow 15s ease infinite;
  color: white;
  padding: 60px;
  position: relative;
  overflow: hidden;
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 装饰性背景 */
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(24, 160, 88, 0.15) 0%, transparent 70%);
}

.bg-circle-1 {
  width: 600px;
  height: 600px;
  top: -200px;
  right: -100px;
  animation: float1 20s ease-in-out infinite;
}

.bg-circle-2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -100px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.12) 0%, transparent 70%);
  animation: float2 25s ease-in-out infinite;
}

.bg-circle-3 {
  width: 300px;
  height: 300px;
  top: 50%;
  left: 30%;
  background: radial-gradient(circle, rgba(24, 160, 88, 0.08) 0%, transparent 70%);
  animation: float3 18s ease-in-out infinite;
}

@keyframes float1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 30px) scale(1.1); }
}

@keyframes float2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(40px, -20px) scale(1.05); }
}

@keyframes float3 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.5; }
  50% { transform: translate(-20px, 40px) scale(1.2); opacity: 1; }
}

.brand-section {
  max-width: 520px;
  z-index: 1;
  position: relative;
}

/* Logo */
.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.logo-icon {
  color: #18a058;
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  color: white;
  letter-spacing: -0.5px;
}

/* 标题 */
.brand-headline {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 48px 0;
  line-height: 1.3;
  background: linear-gradient(135deg, #FFFFFF 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 功能特性 */
.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 40px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(8px);
  border-color: rgba(24, 160, 88, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.feature-icon {
  width: 44px;
  height: 44px;
  min-width: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.feature-icon-ai {
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.feature-icon-design {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.feature-icon-data {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.feature-icon-security {
  background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);
}

.feature-content h3 {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: rgba(255, 255, 255, 0.95);
  letter-spacing: 0.01em;
}

.feature-content p {
  font-size: 13px;
  margin: 0;
  color: rgba(255, 255, 255, 0.55);
  line-height: 1.6;
}

/* 统计数字栏 */
.stats-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 28px;
  background: rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(10px);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #4ade80;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.1);
}

/* ========== 右侧登录区域 ========== */
.login-right {
  flex: 0.8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

.login-card-wrapper {
  width: 100%;
  max-width: 420px;
}

.login-card {
  padding: 44px;
  border-radius: 24px;
  box-shadow: 0 20px 60px -12px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(0, 0, 0, 0.03);
  background: #FFFFFF;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.header-top {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20px;
}

.language-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
}

.language-switcher .divider {
  color: #CBD5E1;
  font-size: 12px;
}

.language-switcher :deep(.n-button) {
  font-size: 12px;
  padding: 4px 8px;
}

.welcome-icon {
  margin-bottom: 16px;
}

.login-header h2 {
  margin: 0 0 8px 0;
  font-size: 26px;
  color: #0F172A;
  font-weight: 700;
}

.login-header p {
  margin: 0;
  color: #64748B;
  font-size: 14px;
}

/* ========== 表单样式 ========== */
:deep(.n-form-item) {
  margin-bottom: 18px;
}

:deep(.n-input) {
  border-radius: 10px;
  height: 46px;
  font-size: 14px;
}

:deep(.n-input .n-input-wrapper) {
  padding-left: 0 !important;
}

:deep(.n-input .n-input__input-el) {
  padding-left: 0 !important;
  margin-left: 0 !important;
}

:deep(.n-input .n-input__prefix) {
  margin-right: 10px;
  padding-left: 12px;
}

:deep(.n-input__input) {
  padding-left: 0 !important;
}

.login-button {
  height: 50px;
  margin-top: 8px;
  border-radius: 14px !important;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #18a058 0%, #16a34a 50%, #22c55e 100%) !important;
  box-shadow: 0 8px 24px -6px rgba(24, 160, 88, 0.45) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  letter-spacing: 0.5px;
  border: none !important;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px -6px rgba(24, 160, 88, 0.55) !important;
  background: linear-gradient(135deg, #15803d 0%, #16a34a 50%, #22c55e 100%) !important;
}

.login-button:active {
  transform: translateY(0);
}

.login-footer {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #E2E8F0;
  text-align: center;
}

.login-footer p {
  margin: 0;
  font-size: 12px;
  color: #94A3B8;
}

/* ========== 登录错误提示 ========== */
.login-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-size: 13px;
  line-height: 1.4;
  animation: errorFadeIn 0.3s ease;
}

.login-error svg {
  flex-shrink: 0;
  color: #dc2626;
}

@keyframes errorFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

















/* ========== 响应式设计 ========== */
@media (max-width: 1200px) {
  .login-left {
    padding: 40px;
  }
  
  .brand-headline {
    font-size: 30px;
  }
  
  .features {
    gap: 12px;
  }
  
  .feature-item {
    padding: 12px;
  }
}

@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
  }

  .login-left {
    flex: none;
    padding: 24px 20px;
    min-height: auto;
  }

  .login-left .bg-decoration,
  .login-left .features,
  .login-left .stats-bar {
    display: none;
  }

  .login-left .brand-section {
    max-width: 100%;
  }

  .login-left .brand-logo {
    margin-bottom: 12px;
  }

  .login-left .brand-headline {
    font-size: 18px;
    margin: 0;
    -webkit-line-clamp: 1;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .login-right {
    flex: 1;
    background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 100%);
    padding: 20px 20px 40px;
  }
  
  .login-card {
    box-shadow: 0 20px 40px -12px rgba(0, 0, 0, 0.3);
  }
}

@media (max-width: 480px) {
  .login-card-wrapper {
    max-width: 100%;
  }
  
  .login-card {
    padding: 28px;
  }
  
  .login-header h2 {
    font-size: 24px;
  }
  
  .login-button {
    height: 44px;
  }
}
</style>

<style>
/* Login 深色模式（非 scoped） */
html.dark .login-left {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 25%, #0f172a 50%, #1e293b 75%, #0f172a 100%) !important;
  background-size: 400% 400% !important;
  animation: gradientFlow 15s ease infinite !important;
}
html.dark .login-right {
  background: #111827 !important;
}
html.dark .login-card {
  background: #1f2937 !important;
  box-shadow: 0 20px 40px -12px rgba(0, 0, 0, 0.4) !important;
}
html.dark .login-header h2 {
  color: #f1f5f9 !important;
}
html.dark .login-header p {
  color: #94a3b8 !important;
}
html.dark .login-footer {
  border-top-color: #374151 !important;
}
html.dark .login-footer p {
  color: #64748b !important;
}
html.dark .login-error {
  background: rgba(220, 38, 38, 0.1) !important;
  border-color: rgba(220, 38, 38, 0.3) !important;
  color: #f87171 !important;
}
html.dark .login-error svg {
  color: #f87171 !important;
}
html.dark .login-container {
  background: #0f172a !important;
}
html.dark .n-input {
  background: #111827 !important;
  border-color: #334155 !important;
}
html.dark .n-input:focus-within {
  border-color: #22c55e !important;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.12) !important;
}
html.dark .n-input .n-input__input-el {
  color: #e2e8f0 !important;
  caret-color: #22c55e !important;
}
html.dark .n-input .n-input__placeholder {
  color: #475569 !important;
}
html.dark .n-input .n-input__prefix .n-icon {
  color: #64748b !important;
}
html.dark .language-switcher .divider {
  color: #475569 !important;
}
</style>
