<template>
  <div class="query-component-config">
    <n-form :model="localComponent" label-placement="left" label-width="80" size="small">
      <n-form-item label="参数名称">
        <n-input v-model:value="localComponent.name" placeholder="参数名称" @update:value="handleChange" />
      </n-form-item>
      
      <n-form-item label="显示标签">
        <n-input v-model:value="localComponent.label" placeholder="显示标签" @update:value="handleChange" />
      </n-form-item>
      
      <n-form-item label="占位提示">
        <n-input v-model:value="localComponent.placeholder" placeholder="占位提示" @update:value="handleChange" />
      </n-form-item>
      
      <n-form-item label="组件宽度">
        <n-input-number 
          v-model:value="localComponent.width" 
          :min="100" 
          :max="500"
          placeholder="宽度(px)"
          style="width: 100%"
          @update:value="handleChange"
        />
      </n-form-item>
      
      <n-form-item label="是否必填">
        <n-switch v-model:value="localComponent.required" @update:value="handleChange" />
      </n-form-item>
      
      <!-- 数字类型特有配置 -->
      <template v-if="localComponent.type === 'number'">
        <n-form-item label="最小值">
          <n-input-number v-model:value="localComponent.min" style="width: 100%" @update:value="handleChange" />
        </n-form-item>
        <n-form-item label="最大值">
          <n-input-number v-model:value="localComponent.max" style="width: 100%" @update:value="handleChange" />
        </n-form-item>
      </template>
      
      <!-- 日期类型特有配置 -->
      <template v-if="localComponent.type === 'date' || localComponent.type === 'dateRange'">
        <n-form-item label="日期格式">
          <n-select 
            v-model:value="localComponent.dateFormat" 
            :options="dateFormatOptions"
            @update:value="handleChange"
          />
        </n-form-item>
      </template>
      
      <!-- 下拉类型特有配置 -->
      <template v-if="localComponent.type === 'select' || localComponent.type === 'multiSelect' || localComponent.type === 'cascader'">
        <n-form-item label="数据来源">
          <n-radio-group v-model:value="localComponent.optionSource" @update:value="handleChange">
            <n-radio value="static">静态配置</n-radio>
            <n-radio value="sql">SQL查询</n-radio>
          </n-radio-group>
        </n-form-item>
        
        <!-- 静态选项配置 -->
        <template v-if="localComponent.optionSource === 'static'">
          <n-form-item label="选项配置">
            <div class="options-editor">
              <div v-for="(opt, index) in localComponent.options" :key="index" class="option-row">
                <n-input v-model:value="opt.label" placeholder="显示文本" size="small" style="flex: 1" @update:value="handleChange" />
                <n-input v-model:value="opt.value" placeholder="值" size="small" style="flex: 1" @update:value="handleChange" />
                <n-button size="small" quaternary @click="removeOption(index)">
                  <template #icon><n-icon><CloseOutline /></n-icon></template>
                </n-button>
              </div>
              <n-button size="small" dashed block @click="addOption">
                <template #icon><n-icon><AddOutline /></n-icon></template>
                添加选项
              </n-button>
            </div>
          </n-form-item>
        </template>
        
        <!-- SQL查询配置 -->
        <template v-if="localComponent.optionSource === 'sql'">
          <n-form-item label="数据源">
            <n-select 
              v-model:value="localComponent.optionDataSourceId" 
              :options="dataSourceOptions"
              placeholder="选择数据源"
              @update:value="handleChange"
            />
          </n-form-item>
          <n-form-item label="SQL语句">
            <n-input 
              v-model:value="localComponent.optionSql" 
              type="textarea" 
              :rows="3"
              placeholder="SELECT label, value FROM table"
              @update:value="handleChange"
            />
          </n-form-item>
        </template>
      </template>
      
      <n-divider />
      
      <!-- 参数关联说明 -->
      <div class="param-hint-section">
        <div class="param-hint-title">参数关联</div>
        <div class="param-hint-box">
          <div class="param-hint-code">{{ paramHintCode }}</div>
          <div class="param-hint-desc">在图表SQL中使用上述变量即可自动关联，无需手动配置</div>
          <div class="param-hint-example">
            <span class="hint-label">示例:</span>
            <code>{{ paramSqlExample }}</code>
          </div>
        </div>
      </div>
    </n-form>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { NForm, NFormItem, NInput, NInputNumber, NSwitch, NSelect, NRadioGroup, NRadio, NButton, NIcon, NDivider } from 'naive-ui'
import { CloseOutline, AddOutline } from '@vicons/ionicons5'
import type { QueryComponent } from '@/types/pageParameter'

const props = defineProps<{
  component: QueryComponent
  charts: Array<{ id: number; chartName: string; fields?: string[] }>
  dataSources?: Array<{ id: number; name: string }>
}>()

const emit = defineEmits<{
  (e: 'update:component', component: QueryComponent): void
}>()

// 本地组件副本
const localComponent = ref<QueryComponent>({ ...props.component })

// 监听外部变化
watch(() => props.component, (newVal) => {
  localComponent.value = { ...newVal }
}, { deep: true })

// 日期格式选项
const dateFormatOptions = [
  { label: 'yyyy-MM-dd', value: 'yyyy-MM-dd' },
  { label: 'yyyy/MM/dd', value: 'yyyy/MM/dd' },
  { label: 'yyyy-MM-dd HH:mm:ss', value: 'yyyy-MM-dd HH:mm:ss' },
  { label: 'yyyy年MM月dd日', value: 'yyyy年MM月dd日' }
]

// 参数提示
const paramHintCode = computed(() => '${' + localComponent.value.name + '}')
const paramSqlExample = computed(() => "SELECT * FROM t WHERE col = '${" + localComponent.value.name + "}'")

// 数据源选项
const dataSourceOptions = computed(() => {
  return (props.dataSources || []).map(ds => ({
    label: ds.name,
    value: ds.id
  }))
})

// 处理变化
const handleChange = () => {
  emit('update:component', { ...localComponent.value })
}

// 添加选项
const addOption = () => {
  if (!localComponent.value.options) {
    localComponent.value.options = []
  }
  localComponent.value.options.push({ label: '', value: '' })
  handleChange()
}

// 删除选项
const removeOption = (index: number) => {
  localComponent.value.options?.splice(index, 1)
  handleChange()
}

</script>

<style scoped>
.query-component-config {
  padding: 8px 0;
}

.options-editor {
  width: 100%;
}

.option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.param-hint-section {
  margin-top: 8px;
}

.param-hint-title {
  font-weight: 500;
  font-size: 13px;
  margin-bottom: 8px;
  color: #333;
}

.param-hint-box {
  padding: 10px 12px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e8f4f8 100%);
  border-radius: 8px;
  border: 1px solid #bae6fd;
}

.param-hint-code {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 15px;
  font-weight: 600;
  color: #0369a1;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 6px;
}

.param-hint-desc {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.param-hint-example {
  font-size: 11px;
  color: #94a3b8;
}

.param-hint-example .hint-label {
  font-weight: 500;
  margin-right: 4px;
}

.param-hint-example code {
  font-family: 'Consolas', 'Monaco', monospace;
  background: rgba(255, 255, 255, 0.6);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
  word-break: break-all;
}
</style>
