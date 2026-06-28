<template>
  <div v-if="isLoading" class="page-progress">
    <div class="progress-bar" :style="{ width: progress + '%' }"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isLoading = ref(false)
const progress = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const startProgress = () => {
  isLoading.value = true
  progress.value = 0
  
  timer = setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 10
    }
  }, 100)
}

const finishProgress = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  progress.value = 100
  
  setTimeout(() => {
    isLoading.value = false
    progress.value = 0
  }, 300)
}

onMounted(() => {
  router.beforeEach(() => {
    startProgress()
    return true
  })
  
  router.afterEach(() => {
    finishProgress()
  })
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.page-progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 10001;
  background: transparent;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #66b1ff);
  transition: width 0.2s ease;
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.5);
}
</style>
