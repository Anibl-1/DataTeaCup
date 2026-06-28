/**
 * 权限检查组合式函数
 * 用于在组件中进行权限检查和导航控制
 */

import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMessage } from 'naive-ui'
import { initMessage } from '@/utils/message'

/**
 * 权限检查组合式函数
 * @param requiredPermission 需要的权限（从路由 meta 中获取或手动传入）
 * @param options 配置选项
 */
export function usePermission(
  requiredPermission?: string,
  options: {
    /** 没有权限时是否自动跳转到 dashboard，默认 true */
    redirectOnDenied?: boolean
    /** 没有权限时显示的消息，默认 '您没有权限访问此页面' */
    deniedMessage?: string
    /** 是否等待权限加载完成后再检查，默认 true */
    waitForPermissions?: boolean
  } = {}
) {
  const router = useRouter()
  const route = useRoute()
  const userStore = useUserStore()
  const message = useMessage()
  initMessage(message)

  const {
    redirectOnDenied = true,
    deniedMessage = '您没有权限访问此页面',
    waitForPermissions = true
  } = options

  // 从路由 meta 中获取权限要求（如果未传入）
  const permission = requiredPermission || (route.meta['permission'] as string | undefined)

  // 检查是否有权限
  const hasPermission = computed(() => {
    if (!permission) {
      return true // 没有权限要求，允许访问
    }

    // 如果权限未加载，且需要等待，返回 true（等待加载完成）
    if (waitForPermissions && !userStore.permissionsLoaded) {
      return true
    }

    return userStore.hasPermission(permission)
  })

  // 检查权限并处理无权限情况
  const checkPermission = (): boolean => {
    if (!permission) {
      return true
    }

    // 如果权限未加载，且需要等待，返回 true（等待加载完成）
    if (waitForPermissions && !userStore.permissionsLoaded) {
      return true
    }

    if (!userStore.hasPermission(permission)) {
      if (redirectOnDenied) {
        message.warning(deniedMessage)
        router.push('/dashboard')
      } else {
        message.warning(deniedMessage)
      }
      return false
    }

    return true
  }

  // 在组件挂载时检查权限
  onMounted(() => {
    if (!permission) {
      return // 没有权限要求，不需要检查
    }

    // 等待权限加载完成
    if (waitForPermissions && !userStore.permissionsLoaded) {
      // 监听权限加载状态
      const stopWatcher = watch(
        () => userStore.permissionsLoaded,
        (loaded) => {
          if (loaded) {
            checkPermission()
            stopWatcher()
          }
        },
        { immediate: true }
      )
    } else {
      // 如果权限已加载或不需要等待，立即检查
      if (userStore.permissionsLoaded || !waitForPermissions) {
        checkPermission()
      }
    }
  })

  return {
    hasPermission,
    checkPermission
  }
}

