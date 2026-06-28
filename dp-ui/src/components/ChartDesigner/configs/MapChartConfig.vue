<template>
  <div class="chart-config-panel">
    <div class="config-section">
      <div class="section-title">
        <n-icon size="16" style="margin-right: 6px;"><MapOutline /></n-icon>
        地图配置
      </div>
      
      <div class="config-item">
        <label>地图类型</label>
        <n-select
          v-model:value="config.mapType"
          :options="mapTypeOptions"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>区域名称字段</label>
        <n-select
          v-model:value="config.regionField"
          :options="fieldOptions"
          placeholder="选择区域名称字段"
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
        <label>显示标签</label>
        <n-switch v-model:value="config.showLabel" @update:value="emitChange" />
      </div>

      <div class="config-item">
        <label>启用下钻</label>
        <n-switch 
          v-model:value="config.enableDrillDown" 
          :disabled="config.mapType !== 'china'"
          @update:value="emitChange" 
        />
        <div v-if="config.mapType !== 'china'" class="config-hint">
          仅中国地图支持下钻到省级
        </div>
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">视觉映射</div>
      
      <div class="config-item">
        <label>最小值</label>
        <n-input-number
          v-model:value="config.visualMapConfig.min"
          size="small"
          placeholder="自动"
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>最大值</label>
        <n-input-number
          v-model:value="config.visualMapConfig.max"
          size="small"
          placeholder="自动"
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>颜色范围</label>
        <n-select
          v-model:value="selectedColorScheme"
          :options="colorSchemeOptions"
          size="small"
          @update:value="handleColorSchemeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { NSelect, NSwitch, NInputNumber, NIcon } from 'naive-ui'
import { MapOutline } from '@vicons/ionicons5'
import type { MapChartConfig } from '@/types/chart'

interface FieldOption {
  label: string
  value: string
  dataType?: string
}

const props = defineProps<{
  modelValue: MapChartConfig
  fieldOptions: FieldOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: MapChartConfig): void
}>()

const config = reactive<MapChartConfig>({
  mapType: 'china',
  showLabel: true,
  enableDrillDown: false,
  regionField: '',
  valueField: '',
  visualMapConfig: {
    min: undefined,
    max: undefined,
    inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695'] },
    text: ['高', '低']
  },
  ...props.modelValue
})

const selectedColorScheme = ref('blue')

const mapTypeOptions = [
  { label: '中国地图', value: 'china' },
  { label: '世界地图', value: 'world' }
]

const colorSchemeOptions = [
  { label: '蓝色渐变', value: 'blue' },
  { label: '绿色渐变', value: 'green' },
  { label: '红色渐变', value: 'red' },
  { label: '紫色渐变', value: 'purple' },
  { label: '橙色渐变', value: 'orange' }
]

const colorSchemes: Record<string, string[]> = {
  blue: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695'],
  green: ['#edf8e9', '#bae4b3', '#74c476', '#31a354', '#006d2c'],
  red: ['#fee5d9', '#fcae91', '#fb6a4a', '#de2d26', '#a50f15'],
  purple: ['#f2f0f7', '#cbc9e2', '#9e9ac8', '#756bb1', '#54278f'],
  orange: ['#feedde', '#fdbe85', '#fd8d3c', '#e6550d', '#a63603']
}

const numericFieldOptions = computed(() => {
  return props.fieldOptions.filter(f => {
    const dt = (f.dataType || '').toUpperCase()
    return /INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|BIGINT|SMALLINT|TINYINT|NUMBER|REAL/.test(dt)
  })
})

watch(() => props.modelValue, (newVal) => {
  Object.assign(config, newVal)
}, { deep: true })

function handleColorSchemeChange(scheme: string) {
  config.visualMapConfig = {
    ...config.visualMapConfig,
    inRange: { color: colorSchemes[scheme] || colorSchemes.blue }
  }
  emitChange()
}

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
</style>
