<template>
  <n-space v-if="visibleActions.length > 0" :size="6" align="center" :wrap="false">
    <!-- Directly visible buttons -->
    <template v-for="action in directActions">
      <n-popconfirm
        v-if="action.confirm"
        :key="'confirm-' + action.label"
        @positive-click="() => action.onClick(row)"
      >
        <template #trigger>
          <n-button size="tiny" :type="action.type ?? 'default'" quaternary>
            <template v-if="action.icon" #icon>
              <n-icon :component="action.icon" />
            </template>
            {{ action.label }}
          </n-button>
        </template>
        {{ confirmText(action.confirm) }}
      </n-popconfirm>
      <n-button
        v-else
        :key="action.label"
        size="tiny"
        :type="action.type ?? 'default'"
        quaternary
        @click="() => action.onClick(row)"
      >
        <template v-if="action.icon" #icon>
          <n-icon :component="action.icon" />
        </template>
        {{ action.label }}
      </n-button>
    </template>

    <!-- Dropdown for overflow actions -->
    <n-dropdown
      v-if="overflowActions.length > 0"
      :options="dropdownOptions"
      @select="handleDropdownSelect"
    >
      <n-button size="tiny" quaternary>
        更多
        <template #icon>
          <n-icon :component="EllipsisHorizontalOutline" />
        </template>
      </n-button>
    </n-dropdown>
  </n-space>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { computed, type Component } from 'vue'
import { NSpace, NButton, NIcon, NPopconfirm, NDropdown } from 'naive-ui'
import { EllipsisHorizontalOutline } from '@vicons/ionicons5'
import { hasPermission } from '@/utils/permission'

export interface ActionConfig {
  label: string
  type?: 'primary' | 'info' | 'success' | 'warning' | 'error'
  icon?: Component
  onClick: (row: any) => void
  permission?: string
  confirm?: string | boolean
  show?: (row: any) => boolean
}

export interface ActionButtonsProps {
  actions: ActionConfig[]
  row: any
  maxVisible?: number
}

const props = withDefaults(defineProps<ActionButtonsProps>(), {
  maxVisible: 3
})

const visibleActions = computed(() =>
  props.actions.filter((action) => {
    if (action.show && !action.show(props.row)) return false
    if (action.permission && !hasPermission(action.permission)) return false
    return true
  })
)

const directActions = computed(() => {
  if (visibleActions.value.length <= props.maxVisible) {
    return visibleActions.value
  }
  return visibleActions.value.slice(0, props.maxVisible - 1)
})

const overflowActions = computed(() => {
  if (visibleActions.value.length <= props.maxVisible) {
    return []
  }
  return visibleActions.value.slice(props.maxVisible - 1)
})

const dropdownOptions = computed(() =>
  overflowActions.value.map((action, index) => ({
    label: action.label,
    key: index
  }))
)

function confirmText(confirm: string | boolean): string {
  return typeof confirm === 'string' ? confirm : '确认执行此操作？'
}

function handleDropdownSelect(key: number) {
  const action = overflowActions.value[key]
  if (action) {
    // For dropdown items with confirm, we call onClick directly
    // since NDropdown doesn't support inline popconfirm
    action.onClick(props.row)
  }
}
</script>
