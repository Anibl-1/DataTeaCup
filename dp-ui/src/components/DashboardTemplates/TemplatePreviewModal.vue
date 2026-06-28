<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="template?.name || '模板预览'"
    style="width: 800px"
  >
    <div class="template-preview">
      <!-- 模板信息 -->
      <div class="preview-header">
        <div class="template-meta">
          <n-tag :type="getCategoryType(template?.category)">
            {{ template?.category }}
          </n-tag>
          <n-text depth="3" style="margin-left: 12px">
            {{ widgetCount }} 个组件
          </n-text>
        </div>
        <p v-if="template?.description" class="template-description">
          {{ template.description }}
        </p>
      </div>

      <!-- 布局预览 -->
      <div class="preview-canvas">
        <div class="preview-grid" :style="gridStyle">
          <div
            v-for="widget in layoutWidgets"
            :key="widget.i"
            class="preview-widget"
            :style="getWidgetStyle(widget)"
          >
            <div class="widget-content">
              <n-icon size="24" class="widget-icon">
                <component :is="getWidgetIcon(widget.type)" />
              </n-icon>
              <span class="widget-label">{{ getWidgetLabel(widget) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 组件列表 -->
      <div class="widget-list">
        <n-text strong>组件列表</n-text>
        <n-data-table
          :columns="columns"
          :data="layoutWidgets"
          :bordered="false"
          size="small"
          :max-height="200"
        />
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleCancel">取消</n-button>
        <n-button type="primary" :loading="creating" @click="handleCreate">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          使用此模板
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h } from 'vue'
import {
  NModal,
  NButton,
  NSpace,
  NIcon,
  NText,
  NTag,
  NDataTable,
  NInput,
  useMessage,
  useDialog,
  type DataTableColumns
} from 'naive-ui'
import {
  BarChartOutline,
  SpeedometerOutline,
  TextOutline,
  FilterOutline,
  AddOutline
} from '@vicons/ionicons5'
import type { DashboardTemplate, DashboardWidget } from '@/types/dashboard'
import { createDashboardFromTemplate } from '@/api/dashboardDesigner'

const emit = defineEmits<{
  (e: 'created', dashboard: any): void
}>()

const message = useMessage()
const dialog = useDialog()

// 状态
const visible = ref(false)
const creating = ref(false)
const template = ref<DashboardTemplate | null>(null)

// 解析布局
const layoutWidgets = computed<DashboardWidget[]>(() => {
  if (!template.value?.layoutJson) return []
  try {
    return JSON.parse(template.value.layoutJson)
  } catch {
    return []
  }
})

// 组件数量
const widgetCount = computed(() => layoutWidgets.value.length)

// 计算网格尺寸
const gridStyle = computed(() => {
  if (layoutWidgets.value.length === 0) return { width: '100%', height: '300px' }
  
  const maxX = Math.max(...layoutWidgets.value.map(w => w.x + w.w))
  const maxY = Math.max(...layoutWidgets.value.map(w => w.y + w.h))
  
  return {
    width: '100%',
    height: `${Math.max(300, maxY * 25)}px`
  }
})

// 获取组件样式
const getWidgetStyle = (widget: DashboardWidget) => {
  const colPercent = 100 / 12
  return {
    left: `${widget.x * colPercent}%`,
    top: `${widget.y * 25}px`,
    width: `${widget.w * colPercent}%`,
    height: `${widget.h * 25}px`
  }
}

// 获取组件图标
const getWidgetIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    chart: BarChartOutline,
    kpi: SpeedometerOutline,
    text: TextOutline,
    filter: FilterOutline
  }
  return iconMap[type] || BarChartOutline
}

// 获取组件标签
const getWidgetLabel = (widget: DashboardWidget) => {
  const labelMap: Record<string, string> = {
    chart: '图表',
    kpi: 'KPI',
    text: '文本',
    filter: '筛选器'
  }
  return labelMap[widget.type] || '组件'
}

// 获取分类标签类型
const getCategoryType = (category?: string): 'default' | 'info' | 'success' | 'warning' => {
  if (!category) return 'default'
  const typeMap: Record<string, 'default' | 'info' | 'success' | 'warning'> = {
    '销售分析': 'info',
    '运营监控': 'success',
    '财务概览': 'warning'
  }
  return typeMap[category] || 'default'
}

// 表格列定义
const columns: DataTableColumns<DashboardWidget> = [
  {
    title: '类型',
    key: 'type',
    width: 100,
    render: (row) => h('div', { style: { display: 'flex', alignItems: 'center', gap: '4px' } }, [
      h(NIcon, { size: 16 }, { default: () => h(getWidgetIcon(row.type)) }),
      h('span', getWidgetLabel(row))
    ])
  },
  {
    title: '位置',
    key: 'position',
    width: 100,
    render: (row) => `(${row.x}, ${row.y})`
  },
  {
    title: '尺寸',
    key: 'size',
    width: 100,
    render: (row) => `${row.w} × ${row.h}`
  },
  {
    title: '配置',
    key: 'config',
    ellipsis: { tooltip: true },
    render: (row) => {
      const config = row.config as any
      return config?.title || config?.label || config?.content?.substring(0, 20) || '-'
    }
  }
]

// 打开弹窗
const open = (tpl: DashboardTemplate) => {
  template.value = tpl
  visible.value = true
}

// 关闭弹窗
const close = () => {
  visible.value = false
  template.value = null
}

// 取消
const handleCancel = () => {
  close()
}

// 创建仪表盘
const handleCreate = () => {
  if (!template.value) return
  
  const templateId = template.value.id
  const templateName = template.value.name
  
  dialog.create({
    title: '创建仪表盘',
    content: () => h('div', [
      h('p', { style: { marginBottom: '12px' } }, '请输入新仪表盘名称：'),
      h(NInput, {
        id: 'dashboard-name-input',
        placeholder: '仪表盘名称',
        defaultValue: `${templateName} - 副本`
      })
    ]),
    positiveText: '创建',
    negativeText: '取消',
    onPositiveClick: async () => {
      const input = document.getElementById('dashboard-name-input') as HTMLInputElement
      const name = input?.value || `${templateName} - 副本`
      
      creating.value = true
      try {
        const res = await createDashboardFromTemplate(templateId, name)
        message.success('仪表盘创建成功')
        emit('created', res.data)
        close()
      } catch (error: any) {
        message.error(error.message || '创建失败')
      } finally {
        creating.value = false
      }
    }
  })
}

// 暴露方法
defineExpose({ open, close })
</script>

<style scoped>
.template-preview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview-header {
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.template-meta {
  display: flex;
  align-items: center;
}

.template-description {
  margin: 8px 0 0;
  color: #666;
  font-size: 14px;
}

.preview-canvas {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  overflow: auto;
}

.preview-grid {
  position: relative;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  min-height: 200px;
}

.preview-widget {
  position: absolute;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  box-sizing: border-box;
}

.widget-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.widget-icon {
  color: #1890ff;
}

.widget-label {
  font-size: 10px;
  color: #666;
}

.widget-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
