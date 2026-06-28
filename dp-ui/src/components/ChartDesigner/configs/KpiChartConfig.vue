<template>
  <div class="chart-config-panel">
    <div class="config-section">
      <div class="section-title">
        <n-icon size="16" style="margin-right: 6px;"><SpeedometerOutline /></n-icon>
        KPI 卡片配置
      </div>
      
      <div class="config-item">
        <label>当前值字段</label>
        <n-select
          v-model:value="config.valueField"
          :options="numericFieldOptions"
          placeholder="选择当前值字段"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>上期值字段（同比）</label>
        <n-select
          v-model:value="config.previousValueField"
          :options="numericFieldOptions"
          placeholder="选择上期值字段（可选）"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>上周期值字段（环比）</label>
        <n-select
          v-model:value="config.periodPreviousField"
          :options="numericFieldOptions"
          placeholder="选择上周期值字段（可选）"
          size="small"
          filterable
          clearable
          @update:value="emitChange"
        />
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">格式设置</div>
      
      <div class="config-item">
        <label>数值格式</label>
        <n-select
          v-model:value="config.format"
          :options="formatOptions"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div v-if="config.format === 'currency'" class="config-item">
        <label>货币符号</label>
        <n-input
          v-model:value="config.currencySymbol"
          placeholder="¥"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>小数位数</label>
        <n-input-number
          v-model:value="config.decimals"
          :min="0"
          :max="6"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div class="config-item">
        <label>单位</label>
        <n-input
          v-model:value="config.unit"
          placeholder="如：元、%、人"
          size="small"
          @update:value="emitChange"
        />
      </div>

      <div class="config-row">
        <div class="config-item half">
          <label>前缀</label>
          <n-input
            v-model:value="config.prefix"
            placeholder="前缀"
            size="small"
            @update:value="emitChange"
          />
        </div>
        <div class="config-item half">
          <label>后缀</label>
          <n-input
            v-model:value="config.suffix"
            placeholder="后缀"
            size="small"
            @update:value="emitChange"
          />
        </div>
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">趋势设置</div>
      
      <div class="config-item">
        <label>显示趋势</label>
        <n-switch v-model:value="config.showTrend" @update:value="emitChange" />
      </div>

      <div v-if="config.showTrend" class="config-item">
        <label>正向趋势为好</label>
        <n-switch v-model:value="config.positiveIsGood" @update:value="emitChange" />
        <div class="config-hint">
          {{ config.positiveIsGood ? '上涨显示绿色，下跌显示红色' : '上涨显示红色，下跌显示绿色（如成本类指标）' }}
        </div>
      </div>
    </div>

    <div class="config-section">
      <div class="section-title">迷你图设置</div>
      
      <div class="config-item">
        <label>显示迷你图</label>
        <n-switch v-model:value="config.showSparkline" @update:value="emitChange" />
      </div>

      <div v-if="config.showSparkline" class="config-item">
        <label>迷你图类型</label>
        <n-radio-group v-model:value="config.sparklineType" size="small" @update:value="emitChange">
          <n-radio-button value="line">折线</n-radio-button>
          <n-radio-button value="bar">柱状</n-radio-button>
        </n-radio-group>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed } from 'vue'
import { NSelect, NSwitch, NInputNumber, NInput, NIcon, NRadioGroup, NRadioButton } from 'naive-ui'
import { SpeedometerOutline } from '@vicons/ionicons5'
import type { KpiChartConfig } from '@/types/chart'

interface FieldOption {
  label: string
  value: string
  dataType?: string
}

const props = defineProps<{
  modelValue: KpiChartConfig
  fieldOptions: FieldOption[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: KpiChartConfig): void
}>()

const config = reactive<KpiChartConfig>({
  format: 'number',
  currencySymbol: '¥',
  decimals: 2,
  unit: '',
  prefix: '',
  suffix: '',
  showTrend: true,
  positiveIsGood: true,
  showSparkline: false,
  sparklineType: 'line',
  valueField: '',
  previousValueField: '',
  periodPreviousField: '',
  ...props.modelValue
})

const formatOptions = [
  { label: '数字', value: 'number' },
  { label: '货币', value: 'currency' },
  { label: '百分比', value: 'percentage' },
  { label: '紧凑格式 (K/M/B)', value: 'compact' }
]

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

.config-row {
  display: flex;
  gap: 12px;
}

.config-item.half {
  flex: 1;
}

.config-hint {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
</style>
