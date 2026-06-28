<template>
  <div class="cron-editor">
    <!-- 快捷预设 -->
    <div class="preset-row">
      <n-select
        v-model:value="selectedPreset"
        :options="presetOptions"
        placeholder="选择常用周期"
        size="small"
        style="width: 200px;"
        clearable
        @update:value="handlePresetChange"
      />
      <n-input
        v-model:value="cronValue"
        placeholder="Cron 表达式，如: 0 0 2 * * ?"
        size="small"
        style="flex: 1;"
        @update:value="handleManualInput"
      />
    </div>

    <!-- 分段编辑器 -->
    <div class="segments">
      <div v-for="(seg, idx) in segments" :key="seg.label" class="segment">
        <div class="segment-label">{{ seg.label }}</div>
        <n-select
          v-model:value="segmentValues[idx]"
          :options="seg.options"
          size="small"
          style="width: 100%;"
          filterable
          @update:value="buildCronFromSegments"
        />
      </div>
    </div>

    <!-- 人类可读描述 + 下次执行时间 -->
    <div class="cron-info">
      <div class="cron-desc">
        <n-icon size="14" color="var(--color-primary)"><TimeOutline /></n-icon>
        <span>{{ humanReadable }}</span>
      </div>
      <div v-if="nextRuns.length > 0" class="next-runs">
        <span class="next-runs-title">接下来 {{ nextRuns.length }} 次执行：</span>
        <n-tag v-for="(run, i) in nextRuns" :key="i" size="tiny" type="info" :bordered="false" style="margin: 2px;">
          {{ run }}
        </n-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { TimeOutline } from '@vicons/ionicons5'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const cronValue = ref(props.modelValue || '')
const selectedPreset = ref<string | null>(null)

// 同步外部值
watch(() => props.modelValue, (v) => {
  if (v !== cronValue.value) {
    cronValue.value = v || ''
    parseToSegments()
  }
})

// 预设选项
const presetOptions = [
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每5分钟', value: '0 */5 * * * ?' },
  { label: '每30分钟', value: '0 */30 * * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每天凌晨6点', value: '0 0 6 * * ?' },
  { label: '每天中午12点', value: '0 0 12 * * ?' },
  { label: '每周一凌晨1点', value: '0 0 1 ? * MON' },
  { label: '每月1号凌晨3点', value: '0 0 3 1 * ?' },
  { label: '工作日每天8点', value: '0 0 8 ? * MON-FRI' }
]

// 段定义
const secondOptions = [{ label: '每秒 (*)', value: '*' }, { label: '0秒', value: '0' }]
const minuteOptions = [
  { label: '每分钟 (*)', value: '*' },
  ...[0, 5, 10, 15, 20, 30].map(v => ({ label: `${v}分`, value: String(v) })),
  { label: '每5分钟', value: '*/5' },
  { label: '每10分钟', value: '*/10' },
  { label: '每30分钟', value: '*/30' }
]
const hourOptions = [
  { label: '每小时 (*)', value: '*' },
  ...Array.from({ length: 24 }, (_, i) => ({ label: `${i}时`, value: String(i) })),
  { label: '每2小时', value: '*/2' },
  { label: '每6小时', value: '*/6' }
]
const dayOptions = [
  { label: '每天 (*)', value: '*' },
  { label: '不指定 (?)', value: '?' },
  ...Array.from({ length: 31 }, (_, i) => ({ label: `${i + 1}号`, value: String(i + 1) }))
]
const monthOptions = [
  { label: '每月 (*)', value: '*' },
  ...Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: String(i + 1) }))
]
const weekOptions = [
  { label: '不指定 (?)', value: '?' },
  { label: '每天 (*)', value: '*' },
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
  { label: '工作日 (周一到周五)', value: 'MON-FRI' }
]

const segments = [
  { label: '秒', options: secondOptions },
  { label: '分', options: minuteOptions },
  { label: '时', options: hourOptions },
  { label: '日', options: dayOptions },
  { label: '月', options: monthOptions },
  { label: '周', options: weekOptions }
]

const segmentValues = ref<string[]>(['0', '0', '2', '*', '*', '?'])

function parseToSegments() {
  const parts = cronValue.value.trim().split(/\s+/)
  if (parts.length >= 6) {
    segmentValues.value = parts.slice(0, 6)
  }
}

function buildCronFromSegments() {
  cronValue.value = segmentValues.value.join(' ')
  selectedPreset.value = null
  emit('update:modelValue', cronValue.value)
}

function handlePresetChange(val: string) {
  if (val) {
    cronValue.value = val
    parseToSegments()
    emit('update:modelValue', val)
  }
}

function handleManualInput(val: string) {
  cronValue.value = val
  parseToSegments()
  emit('update:modelValue', val)
}

// 人类可读描述
const humanReadable = computed(() => {
  const parts = cronValue.value.trim().split(/\s+/)
  if (parts.length < 6) return '请输入有效的 Cron 表达式'

  const [, min, hour, day, month, week] = parts
  const pieces: string[] = []
  const _min = min ?? '*'
  const _hour = hour ?? '*'
  const _day = day ?? '*'
  const _month = month ?? '*'
  const _week = week ?? '?'

  if (_week !== '?' && _week !== '*') pieces.push(`每${weekLabel(_week)}`)
  else if (_month !== '*') pieces.push(`${_month}月`)

  if (_day !== '*' && _day !== '?') pieces.push(`${_day}号`)
  else if (_week === '?' || _week === '*') {
    if (_day === '*') pieces.push('每天')
  }

  if (_hour.startsWith('*/')) pieces.push(`每${_hour.slice(2)}小时`)
  else if (_hour !== '*') pieces.push(`${_hour}时`)
  else pieces.push('每小时')

  if (_min.startsWith('*/')) pieces.push(`每${_min.slice(2)}分钟`)
  else if (_min !== '*' && _min !== '0') pieces.push(`${_min}分`)

  return pieces.join(' ') || cronValue.value
})

function weekLabel(w: string): string {
  const map: Record<string, string> = { MON: '周一', TUE: '周二', WED: '周三', THU: '周四', FRI: '周五', SAT: '周六', SUN: '周日', 'MON-FRI': '工作日' }
  return map[w] || w
}

// 简单的下次执行时间计算 (前端近似)
const nextRuns = computed(() => {
  try {
    const parts = cronValue.value.trim().split(/\s+/)
    if (parts.length < 6) return []
    // 简化: 只对简单表达式计算
    const results: string[] = []
    const now = new Date()
    const _min2 = parts[1] ?? '*'
    const _hour2 = parts[2] ?? '*'

    for (let i = 0; i < 3; i++) {
      const d = new Date(now)
      d.setDate(d.getDate() + i)
      const h = _hour2 === '*' ? d.getHours() : (_hour2.startsWith('*/') ? Math.ceil(d.getHours() / parseInt(_hour2.slice(2))) * parseInt(_hour2.slice(2)) : parseInt(_hour2))
      const m = _min2 === '*' ? 0 : (_min2.startsWith('*/') ? 0 : parseInt(_min2))
      d.setHours(h, m, 0, 0)
      if (d > now) {
        results.push(d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }))
      }
    }
    return results.slice(0, 3)
  } catch {
    return []
  }
})

onMounted(() => {
  if (cronValue.value) parseToSegments()
})
</script>

<style scoped>
.cron-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preset-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.segments {
  display: flex;
  gap: 8px;
}

.segment {
  flex: 1;
  min-width: 0;
}

.segment-label {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
  text-align: center;
  font-weight: 600;
}

.cron-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 10px;
  background: #f0f9ff;
  border-radius: 6px;
  border: 1px solid #bae6fd;
}

.cron-desc {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #0369a1;
  font-weight: 500;
}

.next-runs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 11px;
}

.next-runs-title {
  color: #64748b;
  font-size: 11px;
}




</style>

<style>
/* CronEditor 深色模式（非 scoped） */
html.dark .segment-label {
  color: #94a3b8 !important;
}
html.dark .cron-info {
  background: rgba(14, 165, 233, 0.08) !important;
  border-color: rgba(14, 165, 233, 0.2) !important;
}
html.dark .cron-desc {
  color: #38bdf8 !important;
}
html.dark .next-runs-title {
  color: #94a3b8 !important;
}
</style>
