<template>
  <div></div>
</template>

<script setup lang="ts">
/**
 * 重定向视图 - 用于刷新当前页面
 * 通过先跳转到此页面再跳回原页面实现刷新效果
 */
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

onMounted(() => {
  // 获取原始路径（去掉 /redirect 前缀）
  const { params, query } = route
  const path = params.path as string
  
  // 立即跳转回原页面
  router.replace({
    path: '/' + path,
    query
  })
})
</script>
