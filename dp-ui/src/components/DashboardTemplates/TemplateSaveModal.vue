<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    title="保存为模板"
    style="width: 500px"
    :mask-closable="false"
  >
    <n-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-placement="left"
      label-width="80"
    >
      <n-form-item label="模板名称" path="name">
        <n-input
          v-model:value="formData.name"
          placeholder="请输入模板名称"
          maxlength="50"
          show-count
        />
      </n-form-item>
      
      <n-form-item label="分类" path="category">
        <n-select
          v-model:value="formData.category"
          :options="categoryOptions"
          placeholder="请选择分类"
        />
      </n-form-item>
      
      <n-form-item label="描述" path="description">
        <n-input
          v-model:value="formData.description"
          type="textarea"
          placeholder="请输入模板描述"
          :rows="3"
          maxlength="200"
          show-count
        />
      </n-form-item>
      
      <!-- 预览区域 -->
      <n-form-item label="布局预览">
        <div class="preview-container">
          <div class="preview-grid">
            <div
              v-for="widget in previewWidgets"
              :key="widget.i"
              class="preview-widget"
              :style="getWidgetStyle(widget)"
            >
              <div class="widget-type-icon">
                <n-icon size="16">
                  <component :is="getWidgetIcon(widget.type)" />
                </n-icon>
              </div>
            </div>
          </div>
          <div class="preview-info">
            <n-text depth="3">共 {{ previewWidgets.length }} 个组件</n-text>
          </div>
        </div>
      </n-form-item>
    </n-form>
    
    <template #footer>
      <n-space justify="end">
        <n-button @click="handleCancel">取消</n-button>
        <n-button type="primary" :loading="saving" @click="handleSave">
          保存模板
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch } from 'vue'
import {
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NButton,
  NSpace,
  NIcon,
  NText,
  useMessage,
  type FormInst,
  type FormRules
} from 'naive-ui'
import {
  BarChartOutline,
  SpeedometerOutline,
  TextOutline,
  FilterOutline
} from '@vicons/ionicons5'
import type { DashboardWidget } from '@/types/dashboard'
import { saveDashboardAsTemplate } from '@/api/dashboardDesigner'

interface Props {
  dashboardId?: number
  widgets?: DashboardWidget[]
}

const props = withDefaults(defineProps<Props>(), {
  widgets: () => []
})

const emit = defineEmits<{
  (e: 'saved', template: any): void
}>()

const message = useMessage()

// 状态
const visible = ref(false)
const saving = ref(false)
const formRef = ref<FormInst | null>(null)

// 表单数据
const formData = ref({
  name: '',
  category: '',
  description: ''
})

// 分类选项
const categoryOptions = [
  { label: '销售分析', value: '销售分析' },
  { label: '运营监控', value: '运营监控' },
  { label: '财务概览', value: '财务概览' },
  { label: '其他', value: '其他' }
]

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度为2-50个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ]
}

// 预览组件
const previewWidgets = computed(() => props.widgets || [])

// 获取组件样式（用于预览）
const getWidgetStyle = (widget: DashboardWidget) => {
  const gridWidth = 200
  const gridHeight = 120
  const colWidth = gridWidth / 12
  const rowHeight = gridHeight / 8
  
  return {
    left: `${widget.x * colWidth}px`,
    top: `${widget.y * rowHeight}px`,
    width: `${widget.w * colWidth}px`,
    height: `${widget.h * rowHeight}px`
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

// 打开弹窗
const open = (defaultName?: string) => {
  formData.value = {
    name: defaultName || '',
    category: '',
    description: ''
  }
  visible.value = true
}

// 关闭弹窗
const close = () => {
  visible.value = false
}

// 取消
const handleCancel = () => {
  close()
}

// 保存
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  if (!props.dashboardId) {
    message.warning('请先保存仪表盘')
    return
  }
  
  saving.value = true
  try {
    const res = await saveDashboardAsTemplate(
      props.dashboardId, 
      formData.value.name,
      formData.value.category,
      formData.value.description
    )
    message.success('模板保存成功')
    emit('saved', res.data)
    close()
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 暴露方法
defineExpose({ open, close })
</script>

<style scoped>
.preview-container {
  width: 100%;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
}

.preview-grid {
  position: relative;
  width: 200px;
  height: 120px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  margin: 0 auto;
}

.preview-widget {
  position: absolute;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.widget-type-icon {
  color: #1890ff;
}

.preview-info {
  text-align: center;
  margin-top: 8px;
}
</style>
