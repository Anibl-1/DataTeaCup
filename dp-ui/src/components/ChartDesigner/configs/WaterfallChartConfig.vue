<template>
  <div class="chart-config-panel">
    <div class="config-section">
      <div class="section-title">
        <n-icon size="16" style="margin-right: 6px;"><BarChartOutline /></n-icon>
        瀑布图配置
      </div>
      
      <div class="config-item">
        <label>名称字段</label>
        <n-select
          v-model:value="config.nameField"
          :options="fieldOptions"
          placeholder="选择名称字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>数值字段</label>
        <n-select
          v-model:value="config.valueField"
          :options="numericFieldOptions"
          placeholder="选择数值字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>总计标记字段（可选）</label>
        <n-select
          v-model:value="config.isTotalField"
          :options="fieldOptions"
          placeholder="选择标记总计的字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
        <div class="config-hint">
          该字段值为 true 或 1 时，该行显示为总计
        </div>
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">起始值设置</div>
      
      <div class="config-item">
        <label>起始值</label>
        <n-input-number
          v-model:value="config.startValue"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>起始标签</label>
        <n-input
          v-model:value="config.startLabel"
          placeholder="起始"
          size="small"
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">总计设置</div>
      
      <div class="config-item">
        <label>显示总计</label>
        <n-switch v-model:value="config.showTotal" @update:value="emitChange" />
      </div>

      <div v-if="config.showTotal" class="config-item">
        <label>总计标签</label>
        <n-input
          v-model:value="config.totalLabel"
          placeholder="总计"
          size="small"
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">颜色设置</div>
      
      <div class="config-item">
        <label>正值颜色（增加）</label>
        <div class="color-picker-row">
          <n-color-picker
            v-model:value="config.positiveColor"
            :show-alpha="false"
            size="small"
            @update:value="emitChange"
          />
          <span class="color-preview" :style="{ backgroundColor: config.positiveColor }"></span>
        </div>
      </div>

      <div class="config-item">
        <label>负值颜色（减少）</label>
        <div class="color-picker-row">
          <n-color-picker
            v-model:value="config.negativeColor"
            :show-alpha="false"
            size="small"
            @update:value="emitChange"
          />
          <span class="color-preview" :style="{ backgroundColor: config.negativeColor }"></span>
        </div>
      </div>

      <div class="config-item">
        <label>总计颜色</label>
        <div class="color-picker-row">
          <n-color-picker
            v-model:value="config.totalColor"
            :show-alpha="false"
            size="small"
            @update:value="emitChange"
          />
          <span class="color-preview" :style="{ backgroundColor: config.totalColor }"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import { NSelect, NSwitch, NInputNumber, NInput, NIcon, NColorPicker } from 'naive-ui'
import { BarChartOutline } from '@vicons/ionicons5'
import type { WaterfallChartConfig } from '@/types/chart'

interface FieldOption {
  label: string
  value: string
  dataType?: string
}

const props = defineProps<{
  modelValue: WaterfallChartConfig
  fieldOptions: FieldOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: WaterfallChartConfig): void
}>()

const config = reactive<WaterfallChartConfig>({
  showTotal: true,
  totalLabel: '总计',
  startValue: 0,
  startLabel: '起始',
  positiveColor: '#52c41a',
  negativeColor: '#ff4d4f',
  totalColor: '#1890ff',
  nameField: '',
  valueField: '',
  isTotalField: '',
  ...props.modelValue
})

const numericFieldOptions = computed(() => {
  return props.fieldOptions.filter(f => {
    const dt = (f.dataType || '').toUpperCase()
    return /INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|BIGINT|SMALLINT|TINYINT|NUMBER|REAL/.test(dt)
  })
})

watch(() => props.modelValue, (newVal) => {
  Object.assign(config, newVal)
}, { deep: true })

function emitChange() {
  emit('update:modelValue', { ...config })
}
</script>

<style scoped>
.chart-config-panel {
  padding: 8px 0;
}

.config-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.config-item {
  margin-bottom: 12px;
}

.config-item label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.config-hint {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-preview {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}
</style>
