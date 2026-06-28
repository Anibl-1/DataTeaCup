<template>
  <n-card title="数据量控制" size="small" class="limit-config">
    <template #header-extra>
      <n-tag 
        :type="config.enabled ? 'success' : 'default'" 
        :bordered="false"
        size="small"
      >
        {{ config.enabled ? '已启用' : '未启用' }}
      </n-tag>
    </template>

    <n-space vertical :size="16">
      <!-- 启用/禁用限制 -->
      <n-alert type="info" :bordered="false">
        <template #icon>
          <n-icon><InformationCircleOutline /></n-icon>
        </template>
        数据量限制可以防止查询过多数据导致系统崩溃或响应缓慢
      </n-alert>

      <n-checkbox v-model:checked="config.enabled" @update:checked="handleEnabledChange">
        <n-space align="center" :size="4">
          <n-text strong>启用查询结果限制</n-text>
          <n-text depth="3" style="font-size: 12px">（强烈推荐）</n-text>
        </n-space>
      </n-checkbox>

      <!-- 配置区域 -->
      <n-space v-show="config.enabled" vertical :size="16" class="config-section">
        <!-- 最大行数 -->
        <n-form-item label="最大查询行数" label-placement="top">
          <n-input-number
            v-model:value="config.maxRows"
            :min="1"
            :max="100000"
            :step="1000"
            style="width: 100%"
            @update:value="handleConfigChange"
          >
            <template #prefix>
              <n-icon><ListOutline /></n-icon>
            </template>
            <template #suffix>行</template>
          </n-input-number>
          <template #feedback>
            <n-text depth="3" style="font-size: 12px">
              限制单次查询最多返回的行数。建议值：1000-10000
            </n-text>
          </template>
        </n-form-item>

        <!-- 预设快捷选项 -->
        <n-space>
          <n-text depth="3" style="font-size: 13px">快速设置：</n-text>
          <n-button size="tiny" @click="setMaxRows(100)">100</n-button>
          <n-button size="tiny" @click="setMaxRows(1000)">1000</n-button>
          <n-button size="tiny" @click="setMaxRows(5000)">5000</n-button>
          <n-button size="tiny" @click="setMaxRows(10000)">10000</n-button>
        </n-space>

        <n-divider style="margin: 12px 0" />

        <!-- 警告阈值 -->
        <n-form-item label="数据量警告阈值" label-placement="top">
          <n-slider
            v-model:value="config.warningThreshold"
            :min="100"
            :max="config.maxRows"
            :step="100"
            :marks="warningMarks"
            @update:value="handleConfigChange"
          />
          <template #feedback>
            <n-space justify="space-between" style="margin-top: 8px">
              <n-text depth="3" style="font-size: 12px">
                当前阈值：{{ config.warningThreshold }} 行
              </n-text>
              <n-text depth="3" style="font-size: 12px">
                超过此数量将显示警告
              </n-text>
            </n-space>
          </template>
        </n-form-item>

        <n-divider style="margin: 12px 0" />

        <!-- 是否显示总数 -->
        <n-checkbox v-model:checked="config.showTotal" @update:checked="handleConfigChange">
          <n-text>查询时显示符合条件的总数</n-text>
        </n-checkbox>

        <!-- SQL LIMIT 子句预览 -->
        <n-alert type="success" title="生成的SQL限制" closable>
          <n-code 
            :code="`LIMIT ${config.maxRows}`" 
            language="sql"
            :word-wrap="false"
          />
          <template #footer>
            <n-text depth="3" style="font-size: 12px">
              此LIMIT子句将自动添加到SQL查询末尾
            </n-text>
          </template>
        </n-alert>
      </n-space>

      <!-- 数据量估算警告 -->
      <n-alert 
        v-if="showDataWarning" 
        type="warning"
        closable
        @close="dismissWarning"
      >
        <template #header>
          <n-space align="center">
            <n-icon size="20" color="#faad14"><WarningOutline /></n-icon>
            <n-text strong>数据量警告</n-text>
          </n-space>
        </template>
        
        <n-space vertical :size="8">
          <n-text>
            预计查询结果约 <n-text strong type="warning">{{ estimatedRows?.toLocaleString() }}</n-text> 行，
            可能影响性能和加载速度。
          </n-text>
          
          <n-divider style="margin: 8px 0" />
          
          <n-text strong>建议优化方案：</n-text>
          <ul style="margin: 8px 0 0 0; padding-left: 20px">
            <li>添加查询条件缩小数据范围（如：时间范围、状态筛选）</li>
            <li>减小最大行数限制（当前：{{ config.maxRows }}）</li>
            <li>使用数据聚合（GROUP BY）代替明细查询</li>
            <li>添加索引优化查询性能</li>
          </ul>
        </n-space>

        <template #action>
          <n-space>
            <n-button size="small" type="primary" @click="$emit('openConditionBuilder')">
              <template #icon>
                <n-icon><FilterOutline /></n-icon>
              </template>
              添加查询条件
            </n-button>
            <n-button size="small" @click="adjustLimit">
              <template #icon>
                <n-icon><SettingsOutline /></n-icon>
              </template>
              调整限制
            </n-button>
          </n-space>
        </template>
      </n-alert>

      <!-- 性能提示 -->
      <n-collapse v-if="config.enabled" arrow-placement="right" style="margin-top: 8px">
        <n-collapse-item title="性能优化建议" name="tips">
          <n-space vertical :size="12">
            <n-alert type="info" :bordered="false" size="small">
              <template #icon>
                <n-icon><SpeedometerOutline /></n-icon>
              </template>
              <n-text depth="2">
                <strong>最佳实践：</strong>
              </n-text>
              <ul style="margin: 8px 0 0 0; padding-left: 20px; font-size: 13px">
                <li>小于1000行：响应极快，适合实时刷新</li>
                <li>1000-5000行：响应良好，推荐范围</li>
                <li>5000-10000行：可接受，但避免频繁查询</li>
                <li>大于10000行：谨慎使用，建议分页或聚合</li>
              </ul>
            </n-alert>

            <n-alert type="warning" :bordered="false" size="small">
              <template #icon>
                <n-icon><AlertCircleOutline /></n-icon>
              </template>
              <n-text depth="2">
                <strong>注意事项：</strong>
              </n-text>
              <ul style="margin: 8px 0 0 0; padding-left: 20px; font-size: 13px">
                <li>图表渲染会消耗大量内存，数据越多越慢</li>
                <li>浏览器可能在处理大数据集时卡顿或崩溃</li>
                <li>网络传输大量数据会增加等待时间</li>
                <li>务必添加合理的查询条件和索引</li>
              </ul>
            </n-alert>
          </n-space>
        </n-collapse-item>
      </n-collapse>
    </n-space>
  </n-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import {
  InformationCircleOutline,
  WarningOutline,
  ListOutline,
  FilterOutline,
  SettingsOutline,
  SpeedometerOutline,
  AlertCircleOutline
} from '@vicons/ionicons5'

interface LimitConfig {
  enabled: boolean
  maxRows: number
  warningThreshold: number
  showTotal: boolean
}

interface Props {
  estimatedRows?: number  // 预估行数
  tableRowCount?: number  // 表总行数
}

const props = withDefaults(defineProps<Props>(), {
  estimatedRows: 0,
  tableRowCount: 0
})

const emit = defineEmits<{
  (e: 'update:config', config: LimitConfig): void
  (e: 'change', config: LimitConfig): void
  (e: 'openConditionBuilder'): void
}>()

const message = useMessage()

// 配置对象
const config = ref<LimitConfig>({
  enabled: true,
  maxRows: 10000,
  warningThreshold: 5000,
  showTotal: true
})

// 警告标记
const warningMarks = computed(() => ({
  [Math.floor(config.value.maxRows * 0.25)]: '25%',
  [Math.floor(config.value.maxRows * 0.5)]: '50%',
  [Math.floor(config.value.maxRows * 0.75)]: '75%',
  [config.value.maxRows]: '100%'
}))

// 是否显示数据量警告
const showDataWarning = computed(() => {
  if (!config.value.enabled) return false
  if (!props.estimatedRows) return false
  return props.estimatedRows > config.value.warningThreshold
})

// 是否已忽略警告
const warningDismissed = ref(false)

// 设置最大行数
const setMaxRows = (rows: number) => {
  config.value.maxRows = rows
  // 自动调整警告阈值为最大行数的50%
  config.value.warningThreshold = Math.floor(rows * 0.5)
  handleConfigChange()
  message.success(`已设置最大查询行数为 ${rows}`)
}

// 调整限制
const adjustLimit = () => {
  const suggested = Math.max(1000, Math.floor(props.estimatedRows / 2))
  config.value.maxRows = suggested
  config.value.warningThreshold = Math.floor(suggested * 0.5)
  handleConfigChange()
  message.success(`已根据数据量调整限制为 ${suggested} 行`)
}

// 忽略警告
const dismissWarning = () => {
  warningDismissed.value = true
  message.info('已忽略警告，但仍建议优化查询条件')
}

// 启用状态变化
const handleEnabledChange = () => {
  if (!config.value.enabled) {
    message.warning('已禁用查询限制，查询大量数据可能导致性能问题')
  } else {
    message.success('已启用查询限制')
  }
  handleConfigChange()
}

// 配置变化
const handleConfigChange = () => {
  // 确保警告阈值不超过最大行数
  if (config.value.warningThreshold > config.value.maxRows) {
    config.value.warningThreshold = config.value.maxRows
  }
  
  emit('update:config', config.value)
  emit('change', config.value)
}

// 获取LIMIT子句
const getLimitClause = (): string => {
  if (!config.value.enabled) {
    return ''
  }
  return `LIMIT ${config.value.maxRows}`
}

// 获取配置
const getConfig = (): LimitConfig => {
  return { ...config.value }
}

// 重置配置
const resetConfig = () => {
  config.value = {
    enabled: true,
    maxRows: 10000,
    warningThreshold: 5000,
    showTotal: true
  }
  handleConfigChange()
  message.success('已重置为默认配置')
}

// 监听预估行数变化
watch(() => props.estimatedRows, (newValue) => {
  if (newValue && newValue > config.value.warningThreshold) {
    warningDismissed.value = false
  }
})

// 暴露方法
defineExpose({
  getLimitClause,
  getConfig,
  resetConfig,
  setMaxRows
})
</script>

<style scoped>
.limit-config {
  max-width: 100%;
}

.config-section {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #dee2e6;
}

:deep(.n-alert) {
  border-radius: 8px;
}

:deep(.n-slider .n-slider-mark) {
  font-size: 11px;
  color: #999;
}

:deep(.n-code) {
  background: #2d2d2d !important;
  color: #f8f8f2 !important;
  padding: 8px 12px !important;
  border-radius: 6px !important;
  font-size: 13px !important;
}

:deep(.n-collapse-item__header) {
  font-weight: 500;
}

ul {
  line-height: 1.8;
}

ul li {
  margin-bottom: 4px;
}
</style>
