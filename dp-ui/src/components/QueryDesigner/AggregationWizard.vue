<template>
  <div class="aggregation-wizard">
    <!-- 向导头部 -->
    <div class="wizard-header">
      <div class="wizard-title">
        <n-icon size="18" color="var(--color-primary)"><StatsChartOutline /></n-icon>
        <span>聚合配置向导</span>
      </div>
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-icon size="14" color="#94a3b8" style="cursor: help;"><HelpCircleOutline /></n-icon>
        </template>
        <div style="max-width: 280px; font-size: 12px;">
          <p style="margin: 0 0 8px 0;"><b>聚合查询说明：</b></p>
          <p style="margin: 0 0 4px 0;">• <b>分组字段</b>：用于对数据进行分组（GROUP BY）</p>
          <p style="margin: 0 0 4px 0;">• <b>聚合字段</b>：对分组后的数据进行统计计算</p>
          <p style="margin: 0 0 4px 0;">• <b>聚合函数</b>：COUNT（计数）、SUM（求和）、AVG（平均）、MAX（最大）、MIN（最小）</p>
          <p style="margin: 0;">• <b>HAVING</b>：对聚合结果进行过滤</p>
        </div>
      </n-tooltip>
    </div>

    <!-- 分组字段区域 -->
    <div class="wizard-section">
      <div class="section-header">
        <span class="section-title">
          <n-icon size="14"><LayersOutline /></n-icon>
          分组字段 (GROUP BY)
        </span>
        <n-button size="tiny" type="primary" dashed @click="showGroupBySelector = true">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          添加分组
        </n-button>
      </div>
      
      <div v-if="groupByFields.length === 0" class="empty-section">
        <n-icon size="24" color="#cbd5e1"><LayersOutline /></n-icon>
        <span>拖拽字段到此处或点击添加分组</span>
      </div>
      
      <draggable
        v-else
        v-model="groupByFields"
        item-key="id"
        handle=".drag-handle"
        class="group-by-list"
        ghost-class="drag-ghost"
        animation="200"
        @change="emitUpdate"
      >
        <template #item="{ element, index }">
          <div class="group-by-item">
            <n-icon class="drag-handle" size="14" color="#94a3b8"><ReorderThreeOutline /></n-icon>
            <div class="field-info">
              <span class="field-table">{{ getTableAlias(element.field) }}</span>
              <span class="field-dot">.</span>
              <span class="field-name">{{ getFieldName(element.field) }}</span>
            </div>
            <n-tag size="tiny" :bordered="false" type="info">分组</n-tag>
            <n-button text type="error" size="tiny" @click="removeGroupBy(index)">
              <n-icon><CloseOutline /></n-icon>
            </n-button>
          </div>
        </template>
      </draggable>
    </div>

    <!-- 聚合字段区域 -->
    <div class="wizard-section">
      <div class="section-header">
        <span class="section-title">
          <n-icon size="14"><CalculatorOutline /></n-icon>
          聚合字段
        </span>
        <n-button size="tiny" type="primary" dashed @click="showAggregateSelector = true">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          添加聚合
        </n-button>
      </div>
      
      <div v-if="aggregateFields.length === 0" class="empty-section">
        <n-icon size="24" color="#cbd5e1"><CalculatorOutline /></n-icon>
        <span>添加需要聚合计算的字段</span>
      </div>
      
      <div v-else class="aggregate-list">
        <div v-for="(agg, index) in aggregateFields" :key="agg.id" class="aggregate-item">
          <div class="aggregate-config">
            <n-select
              v-model:value="agg.aggregate"
              :options="aggregateFunctionOptions"
              size="small"
              style="width: 100px;"
              @update:value="emitUpdate"
            />
            <span class="aggregate-paren">(</span>
            <div class="field-info">
              <span class="field-table">{{ agg.tableAlias }}</span>
              <span class="field-dot">.</span>
              <span class="field-name">{{ agg.fieldName }}</span>
            </div>
            <span class="aggregate-paren">)</span>
          </div>
          <n-input
            v-model:value="agg.alias"
            size="small"
            placeholder="别名"
            style="width: 100px;"
            @update:value="emitUpdate"
          />
          <n-button text type="error" size="tiny" @click="removeAggregate(index)">
            <n-icon><CloseOutline /></n-icon>
          </n-button>
        </div>
      </div>
    </div>

    <!-- HAVING 条件区域 -->
    <div class="wizard-section">
      <div class="section-header">
        <span class="section-title">
          <n-icon size="14"><FilterOutline /></n-icon>
          聚合过滤 (HAVING)
        </span>
        <n-button size="tiny" type="primary" dashed @click="addHavingCondition">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          添加条件
        </n-button>
      </div>
      
      <div v-if="havingConditions.length === 0" class="empty-section small">
        <span>对聚合结果进行过滤（可选）</span>
      </div>
      
      <div v-else class="having-list">
        <div v-for="(cond, index) in havingConditions" :key="cond.id" class="having-item">
          <n-select
            v-if="index > 0"
            v-model:value="havingConditions[index - 1].logic"
            :options="logicOptions"
            size="small"
            style="width: 70px;"
            @update:value="emitUpdate"
          />
          <n-select
            v-model:value="cond.field"
            :options="havingFieldOptions"
            size="small"
            filterable
            style="flex: 1; min-width: 140px;"
            placeholder="聚合表达式"
            @update:value="emitUpdate"
          />
          <n-select
            v-model:value="cond.operator"
            :options="operatorOptions"
            size="small"
            style="width: 90px;"
            @update:value="emitUpdate"
          />
          <n-input
            v-if="!['IS NULL', 'IS NOT NULL'].includes(cond.operator)"
            v-model:value="cond.value"
            size="small"
            placeholder="值"
            style="width: 80px;"
            @update:value="emitUpdate"
          />
          <n-button text type="error" size="tiny" @click="removeHaving(index)">
            <n-icon><CloseOutline /></n-icon>
          </n-button>
        </div>
      </div>
    </div>

    <!-- 快速模板 -->
    <div class="wizard-section">
      <div class="section-header">
        <span class="section-title">
          <n-icon size="14"><FlashOutline /></n-icon>
          快速模板
        </span>
      </div>
      <div class="template-grid">
        <div 
          v-for="template in quickTemplates" 
          :key="template.id" 
          class="template-card"
          @click="applyTemplate(template)"
        >
          <n-icon size="20" :color="template.color"><component :is="template.icon" /></n-icon>
          <span class="template-name">{{ template.name }}</span>
          <span class="template-desc">{{ template.description }}</span>
        </div>
      </div>
    </div>

    <!-- 分组字段选择器弹窗 -->
    <n-modal v-model:show="showGroupBySelector" preset="card" title="选择分组字段" style="width: 400px;">
      <div class="field-selector">
        <n-input v-model:value="fieldSearchKeyword" placeholder="搜索字段" clearable size="small" style="margin-bottom: 12px;" />
        <div class="field-selector-list">
          <div
            v-for="field in filteredAvailableFields"
            :key="field.value"
            class="field-selector-item"
            :class="{ disabled: isFieldInGroupBy(field.value) }"
            @click="addGroupByField(field.value)"
          >
            <span class="field-label">{{ field.label }}</span>
            <n-tag v-if="isFieldInGroupBy(field.value)" size="tiny" type="info">已添加</n-tag>
          </div>
        </div>
      </div>
    </n-modal>

    <!-- 聚合字段选择器弹窗 -->
    <n-modal v-model:show="showAggregateSelector" preset="card" title="添加聚合字段" style="width: 480px;">
      <div class="aggregate-selector">
        <n-form label-placement="left" label-width="80">
          <n-form-item label="聚合函数">
            <n-select
              v-model:value="newAggregate.function"
              :options="aggregateFunctionOptions"
              placeholder="选择聚合函数"
            />
          </n-form-item>
          <n-form-item label="字段">
            <n-select
              v-model:value="newAggregate.field"
              :options="availableFieldOptions"
              filterable
              placeholder="选择字段"
            />
          </n-form-item>
          <n-form-item label="别名">
            <n-input v-model:value="newAggregate.alias" placeholder="可选，设置显示名称" />
          </n-form-item>
        </n-form>
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px;">
          <n-button @click="showAggregateSelector = false">取消</n-button>
          <n-button type="primary" :disabled="!canAddAggregate" @click="confirmAddAggregate">添加</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch } from 'vue'
import { NIcon, NButton, NTag, NInput, NSelect, NTooltip, NModal, NForm, NFormItem } from 'naive-ui'
import {
  StatsChartOutline,
  HelpCircleOutline,
  LayersOutline,
  CalculatorOutline,
  FilterOutline,
  FlashOutline,
  AddOutline,
  CloseOutline,
  ReorderThreeOutline,
  PieChartOutline,
  TrendingUpOutline,
  BarChartOutline,
  PeopleOutline
} from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import type { SelectedField, GroupByItem, ConditionItem } from './sqlGenerator'

interface AggregateField extends SelectedField {
  id: string
}

interface QuickTemplate {
  id: string
  name: string
  description: string
  icon: any
  color: string
  groupByFields: string[]
  aggregates: Array<{ function: string; fieldPattern: string; alias: string }>
}

const props = defineProps<{
  availableFields: Array<{ label: string; value: string; tableAlias: string; tableName: string; fieldName: string; dataType?: string }>
  modelGroupBy: GroupByItem[]
  modelSelectedFields: SelectedField[]
  modelHaving: ConditionItem[]
}>()

const emit = defineEmits<{
  (e: 'update:groupBy', value: GroupByItem[]): void
  (e: 'update:selectedFields', value: SelectedField[]): void
  (e: 'update:having', value: ConditionItem[]): void
}>()

// 本地状态
const groupByFields = ref<Array<{ id: string; field: string }>>([])
const aggregateFields = ref<AggregateField[]>([])
const havingConditions = ref<ConditionItem[]>([])

// 弹窗状态
const showGroupBySelector = ref(false)
const showAggregateSelector = ref(false)
const fieldSearchKeyword = ref('')

// 新增聚合字段表单
const newAggregate = ref({
  function: 'COUNT' as string,
  field: '' as string,
  alias: '' as string
})

// 聚合函数选项
const aggregateFunctionOptions = [
  { label: 'COUNT - 计数', value: 'COUNT' },
  { label: 'SUM - 求和', value: 'SUM' },
  { label: 'AVG - 平均值', value: 'AVG' },
  { label: 'MAX - 最大值', value: 'MAX' },
  { label: 'MIN - 最小值', value: 'MIN' }
]

const operatorOptions = [
  { label: '=', value: '=' },
  { label: '!=', value: '!=' },
  { label: '>', value: '>' },
  { label: '<', value: '<' },
  { label: '>=', value: '>=' },
  { label: '<=', value: '<=' }
]

const logicOptions = [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' }
]

// 快速模板
const quickTemplates: QuickTemplate[] = [
  {
    id: 'count-by-category',
    name: '分类计数',
    description: '按分类统计数量',
    icon: PieChartOutline,
    color: 'var(--color-primary)',
    groupByFields: [],
    aggregates: [{ function: 'COUNT', fieldPattern: '*', alias: '数量' }]
  },
  {
    id: 'sum-by-group',
    name: '分组求和',
    description: '按分组汇总金额',
    icon: BarChartOutline,
    color: '#18a058',
    groupByFields: [],
    aggregates: [{ function: 'SUM', fieldPattern: 'amount|total|price', alias: '合计' }]
  },
  {
    id: 'avg-analysis',
    name: '平均值分析',
    description: '计算平均值',
    icon: TrendingUpOutline,
    color: '#f0a020',
    groupByFields: [],
    aggregates: [{ function: 'AVG', fieldPattern: '', alias: '平均值' }]
  },
  {
    id: 'group-stats',
    name: '综合统计',
    description: '计数+求和+平均',
    icon: PeopleOutline,
    color: '#8b5cf6',
    groupByFields: [],
    aggregates: [
      { function: 'COUNT', fieldPattern: '*', alias: '数量' },
      { function: 'SUM', fieldPattern: '', alias: '合计' },
      { function: 'AVG', fieldPattern: '', alias: '平均' }
    ]
  }
]

// 计算属性
const availableFieldOptions = computed(() => {
  return props.availableFields.map(f => ({
    label: f.label,
    value: f.value
  }))
})

const filteredAvailableFields = computed(() => {
  if (!fieldSearchKeyword.value) return props.availableFields
  const kw = fieldSearchKeyword.value.toLowerCase()
  return props.availableFields.filter(f => f.label.toLowerCase().includes(kw))
})

const havingFieldOptions = computed(() => {
  // HAVING 可以使用聚合表达式
  const options: Array<{ label: string; value: string }> = []
  
  // 添加已配置的聚合字段
  aggregateFields.value.forEach(agg => {
    const expr = `${agg.aggregate}(${agg.tableAlias}.${agg.fieldName})`
    options.push({
      label: agg.alias || expr,
      value: expr
    })
  })
  
  // 添加常用聚合表达式
  props.availableFields.forEach(f => {
    aggregateFunctionOptions.forEach(func => {
      options.push({
        label: `${func.value}(${f.label})`,
        value: `${func.value}(${f.value})`
      })
    })
  })
  
  return options
})

const canAddAggregate = computed(() => {
  return newAggregate.value.function && newAggregate.value.field
})

// 监听外部数据变化，同步到本地
watch(() => props.modelGroupBy, (newVal) => {
  groupByFields.value = newVal.map((g, i) => ({
    id: `group_${i}_${Date.now()}`,
    field: g.field
  }))
}, { immediate: true, deep: true })

watch(() => props.modelSelectedFields, (newVal) => {
  // 只同步有聚合函数的字段
  aggregateFields.value = newVal
    .filter(f => f.aggregate)
    .map(f => ({
      ...f,
      id: f.id || `agg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    }))
}, { immediate: true, deep: true })

watch(() => props.modelHaving, (newVal) => {
  havingConditions.value = [...newVal]
}, { immediate: true, deep: true })

// 方法
function getTableAlias(field: string): string {
  const parts = field.split('.')
  return parts[0] || ''
}

function getFieldName(field: string): string {
  const parts = field.split('.')
  return parts[1] || field
}

function isFieldInGroupBy(field: string): boolean {
  return groupByFields.value.some(g => g.field === field)
}

function addGroupByField(field: string) {
  if (isFieldInGroupBy(field)) return
  
  groupByFields.value.push({
    id: `group_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    field
  })
  showGroupBySelector.value = false
  fieldSearchKeyword.value = ''
  emitUpdate()
}

function removeGroupBy(index: number) {
  groupByFields.value.splice(index, 1)
  emitUpdate()
}

function confirmAddAggregate() {
  if (!canAddAggregate.value) return
  
  const fieldInfo = props.availableFields.find(f => f.value === newAggregate.value.field)
  if (!fieldInfo) return
  
  aggregateFields.value.push({
    id: `agg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    tableAlias: fieldInfo.tableAlias,
    tableName: fieldInfo.tableName,
    fieldName: fieldInfo.fieldName,
    alias: newAggregate.value.alias || `${newAggregate.value.function}_${fieldInfo.fieldName}`,
    aggregate: newAggregate.value.function as any
  })
  
  // 重置表单
  newAggregate.value = { function: 'COUNT', field: '', alias: '' }
  showAggregateSelector.value = false
  emitUpdate()
}

function removeAggregate(index: number) {
  aggregateFields.value.splice(index, 1)
  emitUpdate()
}

function addHavingCondition() {
  havingConditions.value.push({
    id: `having_${Date.now()}`,
    field: '',
    operator: '>',
    value: '',
    logic: 'AND'
  })
  emitUpdate()
}

function removeHaving(index: number) {
  havingConditions.value.splice(index, 1)
  emitUpdate()
}

function applyTemplate(template: QuickTemplate) {
  // 应用模板 - 这里只是示例，实际需要根据可用字段智能匹配
  // 清空现有配置
  if (template.aggregates.length > 0) {
    // 找到第一个数值类型字段作为聚合字段
    const numericField = props.availableFields.find(f => 
      f.dataType?.toLowerCase().includes('int') || 
      f.dataType?.toLowerCase().includes('decimal') ||
      f.dataType?.toLowerCase().includes('number') ||
      f.dataType?.toLowerCase().includes('float') ||
      f.dataType?.toLowerCase().includes('double')
    ) || props.availableFields[0]
    
    if (numericField) {
      template.aggregates.forEach(agg => {
        if (agg.fieldPattern === '*') {
          // COUNT(*) 特殊处理
          aggregateFields.value.push({
            id: `agg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            tableAlias: numericField.tableAlias,
            tableName: numericField.tableName,
            fieldName: '*',
            alias: agg.alias,
            aggregate: agg.function as any
          })
        } else {
          aggregateFields.value.push({
            id: `agg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            tableAlias: numericField.tableAlias,
            tableName: numericField.tableName,
            fieldName: numericField.fieldName,
            alias: agg.alias,
            aggregate: agg.function as any
          })
        }
      })
    }
  }
  
  emitUpdate()
}

function emitUpdate() {
  // 发送 GROUP BY 更新
  emit('update:groupBy', groupByFields.value.map(g => ({ field: g.field })))
  
  // 发送聚合字段更新 - 合并到 selectedFields
  const nonAggregateFields = props.modelSelectedFields.filter(f => !f.aggregate)
  emit('update:selectedFields', [...nonAggregateFields, ...aggregateFields.value])
  
  // 发送 HAVING 更新
  emit('update:having', havingConditions.value)
}
</script>

<style scoped>
.aggregation-wizard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wizard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.wizard-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.wizard-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.empty-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  background: #f8fafc;
  border: 2px dashed #e2e8f0;
  border-radius: 8px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

.empty-section.small {
  padding: 12px;
}

/* 分组字段列表 */
.group-by-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-by-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  transition: all 0.15s;
}

.group-by-item:hover {
  background: #e0f2fe;
}

.drag-handle {
  cursor: grab;
  opacity: 0.5;
  transition: opacity 0.15s;
}

.group-by-item:hover .drag-handle {
  opacity: 1;
}

.field-info {
  display: flex;
  align-items: center;
  flex: 1;
  font-size: 12px;
}

.field-table {
  color: var(--color-primary);
  font-weight: 500;
}

.field-dot {
  color: #94a3b8;
  margin: 0 2px;
}

.field-name {
  color: #334155;
}

/* 聚合字段列表 */
.aggregate-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.aggregate-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
}

.aggregate-config {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.aggregate-paren {
  color: #64748b;
  font-weight: 500;
}

/* HAVING 条件列表 */
.having-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.having-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: #fefce8;
  border: 1px solid #fef08a;
  border-radius: 6px;
}

/* 快速模板 */
.template-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.template-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.template-card:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.template-name {
  font-size: 12px;
  font-weight: 500;
  color: #334155;
}

.template-desc {
  font-size: 10px;
  color: #94a3b8;
  text-align: center;
}

/* 字段选择器 */
.field-selector-list {
  max-height: 300px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-selector-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.field-selector-item:hover:not(.disabled) {
  background: #e0f2fe;
  border-color: #7dd3fc;
}

.field-selector-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.field-label {
  font-size: 12px;
  color: #334155;
}

/* 拖拽样式 */
.drag-ghost {
  opacity: 0.5;
  background: #dbeafe !important;
}

/* 聚合选择器 */
.aggregate-selector {
  padding: 8px 0;
}
</style>
