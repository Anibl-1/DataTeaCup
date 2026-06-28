<template>
  <div class="change-password-page" :class="{ 'is-mobile': isMobile }">
    <!-- 移动端顶部导航 -->
    <MobileHeader v-if="isMobile" :title="t('changePassword.title')" show-back />

    <MobilePageShell v-if="isMobile" no-tab-bar>
      <div class="mobile-pwd-form">
        <n-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="top"
          class="password-form"
        >
          <n-form-item :label="t('changePassword.oldPassword')" path="oldPassword">
            <n-input
              v-model:value="form.oldPassword"
              type="password"
              :placeholder="t('changePassword.oldPasswordPlaceholder')"
              show-password-on="click"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><KeyOutline /></n-icon>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item :label="t('changePassword.newPassword')" path="newPassword">
            <n-input
              v-model:value="form.newPassword"
              type="password"
              :placeholder="t('changePassword.newPasswordPlaceholder')"
              show-password-on="click"
              @input="checkPasswordStrength"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><LockOpenOutline /></n-icon>
              </template>
            </n-input>
            <template #feedback>
              <div v-if="form.newPassword" class="password-strength" :style="{ borderColor: passwordLevel.borderColor }">
                <div class="strength-header">
                  <span class="strength-label" :style="{ color: passwordLevel.color }">{{ t('changePassword.strengthLabel') }}</span>
                  <n-tag :type="passwordLevel.type" size="small" round>{{ passwordLevel.text }}</n-tag>
                </div>
                <div class="strength-bar">
                  <div 
                    class="strength-fill" 
                    :class="passwordLevel.class"
                    :style="{ width: passwordLevel.width }"
                  ></div>
                </div>
                <div class="password-tips">
                  <div 
                    v-for="(tip, index) in passwordTips" 
                    :key="index" 
                    class="tip-item"
                    :class="{ 'tip-checked': tip.checked }"
                  >
                    <n-icon size="14">
                      <CheckmarkCircleOutline v-if="tip.checked" />
                      <EllipseOutline v-else />
                    </n-icon>
                    <span>{{ tip.text }}</span>
                  </div>
                </div>
              </div>
            </template>
          </n-form-item>
          <n-form-item :label="t('changePassword.confirmPassword')" path="confirmPassword">
            <n-input
              v-model:value="form.confirmPassword"
              type="password"
              :placeholder="t('changePassword.confirmPasswordPlaceholder')"
              show-password-on="click"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><ShieldCheckmarkOutline /></n-icon>
              </template>
            </n-input>
          </n-form-item>
          <div class="mobile-pwd-actions">
            <n-button type="primary" :loading="submitting" size="large" block @click="handleSubmit">
              <template #icon>
                <n-icon><SaveOutline /></n-icon>
              </template>
              {{ t('changePassword.saveChanges') }}
            </n-button>
            <n-button size="large" block @click="handleReset">
              <template #icon>
                <n-icon><RefreshOutline /></n-icon>
              </template>
              {{ t('common.reset') }}
            </n-button>
          </div>
        </n-form>
      </div>
    </MobilePageShell>

    <!-- 桌面端布局 -->
    <div v-else class="page-container">
      <n-card class="password-card">
        <template #header>
          <div class="card-header-custom">
            <div class="card-title">
              <n-icon size="24" color="var(--color-primary)"><LockClosedOutline /></n-icon>
              <span>{{ t('changePassword.title') }}</span>
            </div>
            <n-popconfirm
              :positive-text="t('common.confirm')"
              :negative-text="t('common.cancel')"
              @positive-click="handleLogout"
            >
              <template #trigger>
                <button class="logout-btn">
                  <n-icon size="15"><LogOutOutline /></n-icon>
                  <span>{{ t('login.signOut') }}</span>
                </button>
              </template>
              {{ t('user.logoutConfirm') }}
            </n-popconfirm>
          </div>
        </template>
        <n-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-placement="left"
          label-width="120px"
          class="password-form"
        >
          <n-form-item :label="t('changePassword.oldPassword')" path="oldPassword">
            <n-input
              v-model:value="form.oldPassword"
              type="password"
              :placeholder="t('changePassword.oldPasswordPlaceholder')"
              show-password-on="click"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><KeyOutline /></n-icon>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item :label="t('changePassword.newPassword')" path="newPassword">
            <n-input
              v-model:value="form.newPassword"
              type="password"
              :placeholder="t('changePassword.newPasswordPlaceholder')"
              show-password-on="click"
              @input="checkPasswordStrength"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><LockOpenOutline /></n-icon>
              </template>
            </n-input>
            <template #feedback>
              <div v-if="form.newPassword" class="password-strength" :style="{ borderColor: passwordLevel.borderColor }">
                <div class="strength-header">
                  <span class="strength-label" :style="{ color: passwordLevel.color }">{{ t('changePassword.strengthLabel') }}</span>
                  <n-tag :type="passwordLevel.type" size="small" round>{{ passwordLevel.text }}</n-tag>
                </div>
                <div class="strength-bar">
                  <div 
                    class="strength-fill" 
                    :class="passwordLevel.class"
                    :style="{ width: passwordLevel.width }"
                  ></div>
                </div>
                <div class="password-tips">
                  <div 
                    v-for="(tip, index) in passwordTips" 
                    :key="index" 
                    class="tip-item"
                    :class="{ 'tip-checked': tip.checked }"
                  >
                    <n-icon size="14">
                      <CheckmarkCircleOutline v-if="tip.checked" />
                      <EllipseOutline v-else />
                    </n-icon>
                    <span>{{ tip.text }}</span>
                  </div>
                </div>
              </div>
            </template>
          </n-form-item>
          <n-form-item :label="t('changePassword.confirmPassword')" path="confirmPassword">
            <n-input
              v-model:value="form.confirmPassword"
              type="password"
              :placeholder="t('changePassword.confirmPasswordPlaceholder')"
              show-password-on="click"
            >
              <template #prefix>
                <n-icon color="#94a3b8"><ShieldCheckmarkOutline /></n-icon>
              </template>
            </n-input>
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" :loading="submitting" size="large" @click="handleSubmit">
                <template #icon>
                  <n-icon><SaveOutline /></n-icon>
                </template>
                {{ t('changePassword.saveChanges') }}
              </n-button>
              <n-button size="large" @click="handleReset">
                <template #icon>
                  <n-icon><RefreshOutline /></n-icon>
                </template>
                {{ t('common.reset') }}
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </n-card>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed } from 'vue'
import { NButton, NSpace, NTag, NIcon, NPopconfirm, useMessage, type FormInst } from 'naive-ui'
import { 
  LockClosedOutline, 
  LockOpenOutline, 
  KeyOutline, 
  ShieldCheckmarkOutline,
  SaveOutline,
  RefreshOutline,
  CheckmarkCircleOutline,
  EllipseOutline,
  LogOutOutline
} from '@vicons/ionicons5'
import MobileHeader from '@/components/mobile/MobileHeader.vue'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import { changePassword } from '@/api/system/user'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useRouter } from 'vue-router'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const isMobile = computed(() => appStore.isMobileView)
initMessage(message)

const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/**
 * 检查密码强度
 * 返回密码强度等级：weak, medium, strong
 */
const checkPasswordStrength = (): { level: 'weak' | 'medium' | 'strong', score: number } => {
  const password = form.newPassword
  if (!password) {
    return { level: 'weak', score: 0 }
  }

  let score = 0
  
  // 长度检查
  if (password.length >= 8) score += 1
  if (password.length >= 12) score += 1
  
  // 包含小写字母
  if (/[a-z]/.test(password)) score += 1
  
  // 包含大写字母
  if (/[A-Z]/.test(password)) score += 1
  
  // 包含数字
  if (/[0-9]/.test(password)) score += 1
  
  // 包含特殊字符
  if (/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password)) score += 1

  if (score <= 2) {
    return { level: 'weak', score }
  } else if (score <= 4) {
    return { level: 'medium', score }
  } else {
    return { level: 'strong', score }
  }
}

/**
 * 密码强度等级显示
 */
const passwordLevel = computed(() => {
  const strength = checkPasswordStrength()
  const levelMap = {
    weak: { type: 'error' as const, text: t('changePassword.weak'), class: 'weak', width: '33%', color: '#ef4444', borderColor: '#fecaca' },
    medium: { type: 'warning' as const, text: t('changePassword.medium'), class: 'medium', width: '66%', color: '#f59e0b', borderColor: '#fde68a' },
    strong: { type: 'success' as const, text: t('changePassword.strong'), class: 'strong', width: '100%', color: '#10b981', borderColor: '#a7f3d0' }
  }
  return levelMap[strength.level]
})

/**
 * 密码提示信息
 */
const passwordTips = computed(() => {
  const password = form.newPassword
  return [
    {
      text: t('changePassword.tipMinLength'),
      checked: password.length >= 8
    },
    {
      text: t('changePassword.tipLowerCase'),
      checked: /[a-z]/.test(password)
    },
    {
      text: t('changePassword.tipUpperCase'),
      checked: /[A-Z]/.test(password)
    },
    {
      text: t('changePassword.tipDigit'),
      checked: /[0-9]/.test(password)
    },
    {
      text: t('changePassword.tipSpecialChar'),
      checked: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(password)
    }
  ]
})

const rules = {
  oldPassword: { required: true, message: t('changePassword.oldPasswordRequired'), trigger: 'blur' },
  newPassword: { 
    required: true, 
    message: t('changePassword.newPasswordRequired'), 
    trigger: ['blur', 'input'],
    validator: (_rule: any, value: string) => {
      if (!value) {
        return new Error(t('changePassword.newPasswordRequired'))
      }
      if (value.length < 8) {
        return new Error(t('changePassword.minLength'))
      }
      // 检查是否至少包含两种类型的字符
      const hasLowerCase = /[a-z]/.test(value)
      const hasUpperCase = /[A-Z]/.test(value)
      const hasDigit = /[0-9]/.test(value)
      const hasSpecialChar = /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(value)
      
      let typeCount = 0
      if (hasLowerCase) typeCount++
      if (hasUpperCase) typeCount++
      if (hasDigit) typeCount++
      if (hasSpecialChar) typeCount++
      
      if (typeCount < 2) {
        return new Error(t('changePassword.tooWeak'))
      }
      
      // 检查新密码是否与旧密码相同
      if (value === form.oldPassword) {
        return new Error(t('changePassword.sameAsOld'))
      }
      
      return true
    }
  },
  confirmPassword: {
    required: true,
    message: t('changePassword.confirmPasswordRequired'),
    trigger: ['blur', 'input'],
    validator: (_rule: any, value: string) => {
      if (!value) {
        return new Error(t('changePassword.confirmPasswordRequired'))
      }
      if (value !== form.newPassword) {
        return new Error(t('changePassword.notMatch'))
      }
      return true
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    submitting.value = true
    try {
      await changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword
      })
      message.success(t('changePassword.success'))
      // 清除登录状态并跳转到登录页
      setTimeout(() => {
        userStore.logout()
        router.push('/login')
      }, 1500)
    } catch (error) {
      const errorMsg = handleApiError(error, t('changePassword.changeFailed'))
      message.error(errorMsg)
    } finally {
      submitting.value = false
    }
  } catch (error) {
    // 验证失败，不处理
  }
}

const handleReset = () => {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.restoreValidation()
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.change-password-page {
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 40px 20px;
}

.change-password-page.is-mobile {
  min-height: 100vh;
  padding: 0;
  align-items: stretch;
}

.page-container {
  width: 100%;
  max-width: 560px;
}

.password-card {
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.card-header-custom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

.logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 8px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.logout-btn:hover {
  background: #fee2e2;
  border-color: #fca5a5;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.15);
}

.logout-btn:active {
  background: #fecaca;
  transform: scale(0.97);
}

.password-form {
  padding: 12px 0;
}

:deep(.n-input) {
  border-radius: 10px;
}

/* 移动端表单 */
.mobile-pwd-form {
  background: #fff;
  border-radius: 14px;
  padding: 20px 16px;
  margin-top: 4px;
}

.mobile-pwd-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

/* 密码强度样式 */
.password-strength {
  margin-top: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: border-color 0.3s ease;
}

.strength-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.strength-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  transition: color 0.3s ease;
}

.strength-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 16px;
}

.strength-fill {
  height: 100%;
  border-radius: 3px;
  transition: all 0.3s ease;
}

.strength-fill.weak {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.strength-fill.medium {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.strength-fill.strong {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.password-tips {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  transition: all 0.2s ease;
}

.tip-item.tip-checked {
  color: #10b981;
}

.tip-item.tip-checked .n-icon {
  color: #10b981;
}

/* 响应式 */
@media (max-width: 640px) {
  .password-tips {
    grid-template-columns: 1fr;
  }
}









</style>

<style>
/* ChangePassword 深色模式（非 scoped） */
html.dark .change-password-page {
  background: transparent !important;
}
html.dark .password-card {
  background: #1e293b !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3) !important;
}
html.dark .card-title {
  color: #e2e8f0 !important;
}
html.dark .password-strength {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .strength-label {
  color: #94a3b8 !important;
}
html.dark .strength-bar {
  background: #334155 !important;
}
html.dark .tip-item {
  color: #64748b !important;
}
html.dark .tip-item.tip-checked {
  color: #34d399 !important;
}
html.dark .mobile-pwd-form {
  background: #1e293b !important;
}
html.dark .logout-btn {
  background: rgba(220, 38, 38, 0.1) !important;
  border-color: rgba(220, 38, 38, 0.25) !important;
  color: #f87171 !important;
}
html.dark .logout-btn:hover {
  background: rgba(220, 38, 38, 0.18) !important;
  border-color: rgba(220, 38, 38, 0.35) !important;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.2) !important;
}
</style>
