<template>
  <div class="chart-config-panel">
    <div class="config-section">
      <div class="section-title">
        <n-icon size="16" style="margin-right: 6px;"><CloudOutline /></n-icon>
        词云图配置
      </div>
      
      <div class="config-item">
        <label>词语字段</label>
        <n-select
          v-model:value="config.wordField"
          :options="fieldOptions"
          placeholder="选择词语字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>权重字段</label>
        <n-select
          v-model:value="config.weightField"
          :options="numericFieldOptions"
          placeholder="选择权重/频率字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">形状设置</div>
      
      <div class="config-item">
        <label>词云形状</label>
        <n-select
          v-model:value="config.shape"
          :options="shapeOptions"
          size="small"
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">字体设置</div>
      
      <div class="config-row">
        <div class="config-item half">
          <label>最小字号</label>
          <n-input-number
            v-model:value="config.minFontSize"
            :min="8"
            :max="100"
            size="small"
            @update:value="emitChange"
          />
        </div>
        <div class="config-item half">
          <label>最大字号</label>
          <n-input-number
            v-model:value="config.maxFontSize"
            :min="12"
            :max="200"
            size="small"
            @update:value="emitChange"
          />
        </div>
      </div>

      <div class="config-item">
        <label>旋转角度范围</label>
        <n-slider
          v-model:value="rotationRangeValue"
          range
          :min="-90"
          :max="90"
          :step="15"
          :marks="rotationMarks"
          @update:value="handleRotationChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">颜色设置</div>
      
      <div class="config-item">
        <label>颜色方案</label>
        <n-select
          v-model:value="selectedColorScheme"
          :options="colorSchemeOptions"
          size="small"
          @update:value="handleColorSchemeChange"
        />
      </div>

      <div class="config-item">
        <label>自定义颜色</label>
        <div class="color-tags">
          <n-tag
            v-for="(color, index) in config.colorRange"
            :key="index"
            :color="{ color: color, textColor: getContrastColor(color) }"
            size="small"
            closable
            @close="removeColor(index)"
          >
            {{ color }}
          </n-tag>
          <n-button size="tiny" quaternary @click="showColorPicker = true">
            <template #icon><n-icon size="12"><AddOutline /></n-icon></template>
            添加
          </n-button>
        </div>
      </div>

      <n-modal v-model:show="showColorPicker" preset="dialog" title="添加颜色" style="width: 300px;">
        <n-color-picker v-model:value="newColor" :show-alpha="false" />
        <template #action>
          <n-button size="small" @click="showColorPicker = false">取消</n-button>
          <n-button type="primary" size="small" @click="addColor">添加</n-button>
        </template>
      </n-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { NSelect, NInputNumber, NSlider, NIcon, NTag, NButton, NModal, NColorPicker } from 'naive-ui'
import { CloudOutline, AddOutline } from '@vicons/ionicons5'
import type { WordCloudChartConfig } from '@/types/chart'

interface FieldOption {
  label: string
  value: string
  dataType?: string
}

const props = defineProps<{
  modelValue: WordCloudChartConfig
  fieldOptions: FieldOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: WordCloudChartConfig): void
}>()

const config = reactive<WordCloudChartConfig>({
  shape: 'circle',
  colorRange: ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96'],
  minFontSize: 12,
  maxFontSize: 60,
  rotationRange: [-45, 45],
  wordField: '',
  weightField: '',
  ...props.modelValue
})

const showColorPicker = ref(false)
const newColor = ref('#1890ff')
const selectedColorScheme = ref('default')

const rotationRangeValue = ref<[number, number]>(config.rotationRange || [-45, 45])

const shapeOptions = [
  { label: '圆形', value: 'circle' },
  { label: '心形', value: 'cardioid' },
  { label: '菱形', value: 'diamond' },
  { label: '三角形', value: 'triangle' },
  { label: '正三角', value: 'triangle-forward' },
  { label: '五边形', value: 'pentagon' },
  { label: '星形', value: 'star' }
]

const colorSchemeOptions = [
  { label: '默认配色', value: 'default' },
  { label: '蓝色系', value: 'blue' },
  { label: '绿色系', value: 'green' },
  { label: '暖色系', value: 'warm' },
  { label: '冷色系', value: 'cool' },
  { label: '彩虹色', value: 'rainbow' }
]

const colorSchemes: Record<string, string[]> = {
  default: ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96'],
  blue: ['#e6f7ff', '#91d5ff', '#40a9ff', '#1890ff', '#096dd9', '#0050b3', '#003a8c'],
  green: ['#f6ffed', '#b7eb8f', '#73d13d', '#52c41a', '#389e0d', '#237804', '#135200'],
  warm: ['#fff2e8', '#ffd591', '#ffa940', '#fa8c16', '#d46b08', '#ad4e00', '#873800'],
  cool: ['#e6fffb', '#87e8de', '#36cfc9', '#13c2c2', '#08979c', '#006d75', '#00474f'],
  rainbow: ['#f5222d', '#fa8c16', '#fadb14', '#52c41a', '#1890ff', '#722ed1', '#eb2f96']
}

const rotationMarks = {
  '-90': '-90°',
  '-45': '-45°',
  '0': '0°',
  '45': '45°',
  '90': '90°'
}

const numericFieldOptions = computed(() => {
  return props.fieldOptions.filter(f => {
    const dt = (f.dataType || '').toUpperCase()
    return /INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|BIGINT|SMALLINT|TINYINT|NUMBER|REAL/.test(dt)
  })
})

watch(() => props.modelValue, (newVal) => {
  Object.assign(config, newVal)
  if (newVal.rotationRange) {
    rotationRangeValue.value = newVal.rotationRange
  }
}, { deep: true })

function handleRotationChange(value: [number, number]) {
  config.rotationRange = value
  emitChange()
}

function handleColorSchemeChange(scheme: string) {
  if (scheme !== 'default') {
    config.colorRange = [...colorSchemes[scheme]]
  }
  emitChange()
}

function addColor() {
  if (newColor.value && !config.colorRange?.includes(newColor.value)) {
    config.colorRange = [...(config.colorRange || []), newColor.value]
    emitChange()
  }
  showColorPicker.value = false
}

function removeColor(index: number) {
  config.colorRange = config.colorRange?.filter((_, i) => i !== index)
  emitChange()
}

function getContrastColor(hexColor: string): string {
  const hex = hexColor.replace('#', '')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000
  return brightness > 128 ? '#000000' : '#ffffff'
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

.config-row {
  display: flex;
  gap: 12px;
}

.config-item.half {
  flex: 1;
}

.color-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
</style>
