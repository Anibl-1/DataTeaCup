<template>
  <div class="public-share-view">
    <!-- 密码验证 -->
    <div v-if="needPassword && !verified" class="password-form">
      <n-card title="访问受保护的分享" style="max-width: 400px; margin: 100px auto">
        <n-form @submit.prevent="verifyPassword">
          <n-form-item label="请输入访问密码">
            <n-input v-model:value="password" type="password" placeholder="输入密码" />
          </n-form-item>
          <n-button type="primary" :loading="loading" @click="verifyPassword">验证</n-button>
        </n-form>
        <n-alert v-if="error" type="error" style="margin-top: 12px">{{ error }}</n-alert>
      </n-card>
    </div>

    <!-- 分享内容 -->
    <div v-else-if="shareData" class="share-content">
      <div v-if="shareData.watermarkEnabled" class="watermark-layer">
        {{ watermarkText }}
      </div>
      <div class="content-wrapper">
        <n-alert type="info" style="margin-bottom: 12px">
          分享内容: {{ shareData.resourceType }} - {{ shareData.resourceName }}
        </n-alert>
        <!-- 实际内容根据resourceType动态渲染 -->
        <component :is="contentComponent" v-if="contentComponent" :resource-id="shareData.resourceId" />
      </div>
    </div>

    <!-- 加载/错误状态 -->
    <div v-else-if="loading" class="loading-state">
      <n-spin size="large" />
    </div>
    <div v-else-if="error" class="error-state">
      <n-result status="error" :title="error" />
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { NCard, NForm, NFormItem, NInput, NButton, NAlert, NSpin, NResult } from 'naive-ui'
import request from '@/api/request'

const route = useRoute()
const token = computed(() => route.params["token"] as string)

const shareData = ref<any>(null)
const needPassword = ref(false)
const verified = ref(false)
const password = ref('')
const loading = ref(false)
const error = ref('')
const watermarkText = ref('')

const contentComponent = computed(() => {
  if (!shareData.value) return null
  // 根据资源类型返回对应组件
  return null
})

async function loadShare() {
  loading.value = true
  error.value = ''
  try {
    const res = await request.post(`/api/share/access/${token.value}`, {})
    if (res.data?.allowed) {
      shareData.value = res.data.shareLink
      verified.value = true
    } else if (res.data?.reason?.includes('密码')) {
      needPassword.value = true
    } else {
      error.value = res.data?.reason || '无法访问'
    }
  } catch (e: any) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function verifyPassword() {
  loading.value = true
  error.value = ''
  try {
    const res = await request.post(`/api/share/access/${token.value}`, { password: password.value })
    if (res.data?.allowed) {
      shareData.value = res.data.shareLink
      verified.value = true
      needPassword.value = false
    } else {
      error.value = res.data?.reason || '密码错误'
    }
  } catch (e: any) {
    error.value = e.message || '验证失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadShare)
</script>

<style scoped>
.public-share-view {
  min-height: 100vh;
  background: var(--n-body-color, #f5f5f5);
}
.watermark-layer {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  z-index: 1000;
  opacity: 0.08;
  font-size: 20px;
  transform: rotate(-30deg);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
}
.content-wrapper {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
.loading-state, .error-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style>
