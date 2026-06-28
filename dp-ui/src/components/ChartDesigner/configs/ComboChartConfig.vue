<template>
  <div class="chart-config-panel">
    <div class="config-section">
      <div class="section-title">
        <n-icon size="16" style="margin-right: 6px;"><LayersOutline /></n-icon>
        组合图配置
      </div>
      
      <div class="config-item">
        <label>类别字段（X轴）</label>
        <n-select
          v-model:value="config.categoryField"
          :options="fieldOptions"
          placeholder="选择类别字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>柱状图字段（左Y轴）</label>
        <n-select
          v-model:value="config.barFields"
          :options="numericFieldOptions"
          placeholder="选择柱状图字段"
          size="small"
          filterable
          multiple
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>折线图字段（右Y轴）</label>
        <n-select
          v-model:value="config.lineFields"
          :options="numericFieldOptions"
          placeholder="选择折线图字段"
          size="small"
          filterable
          multiple
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">Y轴设置</div>
      
      <div class="config-item">
        <label>左Y轴标签</label>
        <n-input
          v-model:value="config.leftAxisLabel"
          placeholder="左Y轴名称"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>右Y轴标签</label>
        <n-input
          v-model:value="config.rightAxisLabel"
          placeholder="右Y轴名称"
          size="small"
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">系列配置</div>
      
      <div v-if="config.seriesConfig && config.seriesConfig.length > 0" class="series-list">
        <div v-for="(series, index) in config.seriesConfig" :key="index" class="series-item">
          <div class="series-header">
            <n-tag :type="series.type === 'bar' ? 'info' : 'success'" size="small">
              {{ series.type === 'bar' ? '柱状' : '折线' }}
            </n-tag>
            <span class="series-name">{{ series.name }}</span>
            <n-tag size="tiny" :bordered="false">
              {{ series.yAxisIndex === 0 ? '左轴' : '右轴' }}
            </n-tag>
          </div>
          <div class="series-config">
            <div class="series-config-item">
              <label>颜色</label>
              <n-color-picker
                v-model:value="series.color"
                :show-alpha="false"
                size="small"
                @update:value="emitChange"
              />
            </div>
            <div v-if="series.type === 'line'" class="series-config-item">
              <label>平滑曲线</label>
              <n-switch v-model:value="series.smooth" size="small" @update:value="emitChange" />
            </div>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-series">
        <n-empty description="请先选择柱状图或折线图字段" size="small" />
      </div>

      <n-button 
        size="small" 
        quaternary 
        type="primary" 
        block 
        style="margin-top: 8px;"
        @click="regenerateSeriesConfig"
      >
        <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
        重新生成系列配置
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import { NSelect, NInput, NIcon, NTag, NColorPicker, NSwitch, NButton, NEmpty } from 'naive-ui'
import { LayersOutline, RefreshOutline } from '@vicons/ionicons5'
import type { ComboChartConfig } from '@/types/chart'

interface FieldOption {
  label: string
  value: string
  dataType?: string
}

const props = defineProps<{
  modelValue: ComboChartConfig
  fieldOptions: FieldOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ComboChartConfig): void
}>()

const defaultColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

const config = reactive<ComboChartConfig>({
  categoryField: '',
  barFields: [],
  lineFields: [],
  leftAxisLabel: '',
  rightAxisLabel: '',
  seriesConfig: [],
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

watch([() => config.barFields, () => config.lineFields], () => {
  regenerateSeriesConfig()
}, { deep: true })

function regenerateSeriesConfig() {
  const newSeriesConfig: ComboChartConfig['seriesConfig'] = []
  let colorIndex = 0

  // 添加柱状图系列
  for (const field of (config.barFields || [])) {
    newSeriesConfig.push({
      name: field,
      type: 'bar',
      yAxisIndex: 0,
      color: defaultColors[colorIndex % defaultColors.length]
    })
    colorIndex++
  }

  // 添加折线图系列
  for (const field of (config.lineFields || [])) {
    newSeriesConfig.push({
      name: field,
      type: 'line',
      yAxisIndex: 1,
      color: defaultColors[colorIndex % defaultColors.length],
      smooth: true
    })
    colorIndex++
  }

  config.seriesConfig = newSeriesConfig
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

.series-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.series-item {
  padding: 10px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #f0f0f0;
}

.series-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.series-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
}

.series-config {
  display: flex;
  gap: 16px;
  align-items: center;
}

.series-config-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.series-config-item label {
  font-size: 11px;
  color: #666;
  margin: 0;
}

.empty-series {
  padding: 20px;
  text-align: center;
}
</style>
