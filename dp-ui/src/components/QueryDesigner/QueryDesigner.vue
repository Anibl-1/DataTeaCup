<template>
  <div class="query-designer">
    <!-- 上部: 表浏览 + 画布 -->
    <div class="designer-top">
      <!-- 左侧: 表/字段浏览树 -->
      <div class="table-browser">
        <div class="browser-header">
          <span>表/字段浏览</span>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-icon size="14" color="#94a3b8" style="cursor: help;"><HelpCircleOutline /></n-icon>
            </template>
            <div style="max-width: 200px; font-size: 12px;">
              <p style="margin: 0 0 4px 0;"><b>拖拽操作：</b></p>
              <p style="margin: 0;">• 拖拽表名到画布添加表</p>
              <p style="margin: 0;">• 拖拽字段到画布添加字段</p>
              <p style="margin: 0;">• 双击表名快速添加</p>
            </div>
          </n-tooltip>
        </div>
        <n-input
          v-model:value="tableSearchKeyword"
          placeholder="搜索表名"
          clearable
          size="small"
          style="margin-bottom: 8px;"
        />
        <div v-if="tablesLoading" style="text-align: center; padding: 20px;">
          <n-spin size="small" />
        </div>
        <div v-else-if="filteredTables.length === 0" class="empty-hint">
          {{ dataSourceId ? '无匹配表' : '请先选择数据源' }}
        </div>
        <div v-else class="table-list">
          <div
            v-for="table in filteredTables"
            :key="table.tableName"
            class="table-item"
          >
            <div
              class="table-item-header"
              draggable="true"
              @dragstart="handleTableDragStart($event, table.tableName)"
              @dragend="handleDragEnd"
              @dblclick="addTableToCanvas(table.tableName)"
              @click="toggleTableExpand(table.tableName)"
            >
              <n-icon size="14" color="var(--color-primary)"><GridOutline /></n-icon>
              <span class="table-name">{{ table.tableName }}</span>
              <n-icon size="12" color="#94a3b8" class="drag-hint-icon"><MoveOutline /></n-icon>
              <n-button
                text size="tiny" type="primary"
                title="添加到画布"
                @click.stop="addTableToCanvas(table.tableName)"
              >+</n-button>
            </div>
            <div v-if="expandedTables.has(table.tableName)" class="field-list">
              <div
                v-for="col in tableColumnsCache[table.tableName] || []"
                :key="col.columnName"
                class="field-item"
                draggable="true"
                @dragstart="handleFieldDragStart($event, table.tableName, col)"
                @dragend="handleDragEnd"
              >
                <n-icon size="10" color="#94a3b8" class="field-drag-handle"><MoveOutline /></n-icon>
                <span class="field-name">{{ col.columnName }}</span>
                <n-tag size="tiny" :bordered="false" type="info">{{ col.dataType }}</n-tag>
              </div>
              <div v-if="!tableColumnsCache[table.tableName]" style="padding: 4px 8px;">
                <n-spin size="tiny" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间: 可视化画布 -->
      <div class="canvas-area">
        <div class="canvas-header">
          <span>查询画布</span>
          <n-space size="small">
            <n-tag v-if="config.tables.length > 0" type="info" size="small">
              {{ config.tables.length }} 张表
            </n-tag>
            <n-button v-if="config.tables.length > 1" size="tiny" quaternary @click="autoLayout">
              自动布局
            </n-button>
          </n-space>
        </div>
        <div 
          ref="canvasRef" 
          class="canvas-content" 
          :class="{ 'joining-mode': joiningState, 'drag-over': isDragOver }" 
          @mousemove="onCanvasMouseMove"
          @dragover="handleCanvasDragOver"
          @dragleave="handleCanvasDragLeave"
          @drop="handleCanvasDrop"
        >
          <div
            v-if="config.tables.length === 0"
            class="canvas-placeholder"
          >
            <div class="placeholder-content">
              <n-icon size="48" color="#cbd5e1"><GridOutline /></n-icon>
              <p>拖拽左侧表名到此处</p>
              <p class="placeholder-hint">或双击表名 / 点击 + 按钮添加</p>
            </div>
          </div>
          <!-- 拖拽提示覆盖层 -->
          <div v-if="isDragOver" class="drop-overlay">
            <div class="drop-hint">
              <n-icon size="32" color="var(--color-primary)"><AddCircleOutline /></n-icon>
              <span>{{ dragType === 'table' ? '释放以添加表' : '释放以添加字段' }}</span>
            </div>
          </div>
          <!-- 关联创建状态提示 -->
          <div v-if="joiningState" class="join-status-bar">
            <span class="join-status-pulse"></span>
            <span>从 <b>{{ joiningState.alias }}.{{ joiningState.field }}</b> → 点击目标表的字段完成关联</span>
            <n-button size="tiny" quaternary style="margin-left: 8px; color: #fff;" @click="cancelJoining">取消 (Esc)</n-button>
          </div>
          <!-- 表卡片 -->
          <div
            v-for="(table, idx) in config.tables"
            :key="table.alias"
            class="table-card"
            :style="{ left: tablePositions[table.alias]?.x + 'px', top: tablePositions[table.alias]?.y + 'px' }"
            @mousedown="startDragCard($event, table.alias)"
          >
            <div class="table-card-header">
              <span class="table-card-name" :title="table.tableName">
                {{ table.tableName }}
                <span v-if="table.alias !== table.tableName" class="table-alias">({{ table.alias }})</span>
              </span>
              <n-button text size="tiny" type="error" @click="removeTable(idx)">✕</n-button>
            </div>
            <div class="table-card-fields">
              <div
                v-for="col in tableColumnsCache[table.tableName] || []"
                :key="col.columnName"
                class="table-card-field"
                :class="{
                  'join-source-field': joiningState?.alias === table.alias && joiningState?.field === col.columnName,
                  'join-target-field': joiningState && joiningState.alias !== table.alias
                }"
              >
                <n-checkbox
                  :checked="table.selectedFields.includes(col.columnName)"
                  size="small"
                  @update:checked="(val: boolean) => toggleField(table, col.columnName, val)"
                />
                <span
                  class="field-name-in-card"
                  @mousedown.stop="startJoinDrag($event, table.alias, table.tableName, col.columnName)"
                >{{ col.columnName }}</span>
                <n-tag size="tiny" :bordered="false" type="info" style="font-size: 10px;">{{ col.dataType }}</n-tag>
                <span
                  v-if="isFieldJoined(table.alias, col.columnName)"
                  class="field-join-dot"
                  :style="{ background: getFieldJoinColor(table.alias, col.columnName) }"
                ></span>
              </div>
            </div>
          </div>

          <!-- JOIN 连线 (SVG bezier) -->
          <svg v-if="config.joins.length > 0 || joiningState" class="join-lines">
            <!-- 已有关联 -->
            <g v-for="join in config.joins" :key="join.id" class="join-line-group">
              <path
                :d="getJoinPath(join)"
                :stroke="joinTypeColor(join.joinType)"
                stroke-width="2.5"
                fill="none"
                class="join-curve"
              />
              <circle
                :cx="getJoinEndpoints(join).start.x"
                :cy="getJoinEndpoints(join).start.y"
                r="5" :fill="joinTypeColor(join.joinType)"
                stroke="#fff" stroke-width="1.5"
              />
              <circle
                :cx="getJoinEndpoints(join).end.x"
                :cy="getJoinEndpoints(join).end.y"
                r="5" :fill="joinTypeColor(join.joinType)"
                stroke="#fff" stroke-width="1.5"
              />
              <g :transform="`translate(${getJoinMidpoint(join).x}, ${getJoinMidpoint(join).y})`" class="join-badge-group">
                <rect
                  x="-26" y="-11" width="52" height="22" rx="11"
                  :fill="joinTypeColor(join.joinType)"
                  class="join-type-badge"
                  @click.stop="cycleJoinType(join)"
                />
                <text
x="0" y="4" font-size="10" fill="#fff" text-anchor="middle" font-weight="600"
                  style="pointer-events: none; user-select: none;"
                >{{ join.joinType }}</text>
                <g style="cursor: pointer;" class="join-delete-btn" @click.stop="removeJoin(join.id)">
                  <circle cx="34" cy="0" r="8" fill="#ff4d4f" opacity="0" />
                  <text
x="34" y="3.5" font-size="10" fill="#ff4d4f" text-anchor="middle"
                    style="pointer-events: none;"
                  >✕</text>
                </g>
              </g>
            </g>
            <!-- 创建关联时的临时线 -->
            <g v-if="joiningState">
              <path
                :d="getTempJoinPath()"
                stroke="var(--color-primary)" stroke-width="2"
                stroke-dasharray="8 4" fill="none" opacity="0.7"
              />
              <circle
                :cx="getJoiningSourcePos().x" :cy="getJoiningSourcePos().y"
                r="5" fill="var(--color-primary)" stroke="#fff" stroke-width="1.5"
              />
            </g>
          </svg>
        </div>
      </div>
    </div>

    <!-- 下部: 查询配置 Tabs -->
    <div class="config-panel">
      <n-tabs v-model:value="activeTab" type="line" size="small">
        <n-tab-pane name="fields" tab="选择字段">
          <div class="config-section">
            <div v-if="config.selectedFields.length === 0" class="empty-hint">
              <p>在表卡片中勾选字段，或从左侧拖拽字段到画布</p>
            </div>
            <draggable
              v-else
              v-model="config.selectedFields"
              item-key="id"
              handle=".field-drag-handle"
              class="selected-fields-list"
              ghost-class="field-ghost"
              chosen-class="field-chosen"
              animation="200"
            >
              <template #item="{ element, index }">
                <div class="selected-field-row">
                  <n-icon class="field-drag-handle" size="14" color="#94a3b8"><ReorderThreeOutline /></n-icon>
                  <span class="field-table-alias">{{ element.tableAlias }}</span>
                  <span class="field-dot">.</span>
                  <span class="field-name-text">{{ element.fieldName }}</span>
                  <n-input
                    v-model:value="element.alias"
                    size="small"
                    placeholder="别名"
                    style="width: 100px;"
                  />
                  <n-select
                    v-model:value="element.aggregate"
                    :options="aggregateOptions"
                    size="small"
                    style="width: 90px;"
                    placeholder="聚合"
                  />
                  <n-button text type="error" size="small" @click="removeSelectedField(index)">
                    <n-icon><TrashOutline /></n-icon>
                  </n-button>
                </div>
              </template>
            </draggable>
          </div>
        </n-tab-pane>

        <n-tab-pane name="conditions" tab="筛选条件">
          <div class="config-section">
            <ConditionBuilder
              v-model="config.conditions"
              :available-fields="allFieldOptions"
            />
          </div>
        </n-tab-pane>

        <n-tab-pane name="groupBy" tab="分组汇总">
          <AggregationWizard
            :available-fields="availableFieldsForWizard"
            :model-group-by="config.groupBy"
            :model-selected-fields="config.selectedFields"
            :model-having="config.having"
            @update:group-by="handleGroupByUpdate"
            @update:selected-fields="handleSelectedFieldsUpdate"
            @update:having="handleHavingUpdate"
          />
        </n-tab-pane>

        <n-tab-pane name="orderBy" tab="排序">
          <div class="config-section">
            <n-button size="small" type="primary" dashed @click="addOrderBy">
              + 添加排序字段
            </n-button>
            <div v-for="(o, idx) in config.orderBy" :key="idx" class="condition-row">
              <n-select
                v-model:value="o.field"
                :options="allFieldOptions"
                size="small"
                filterable
                style="flex: 1;"
                placeholder="排序字段"
              />
              <n-select
                v-model:value="o.direction"
                :options="[{ label: '升序 ASC', value: 'ASC' }, { label: '降序 DESC', value: 'DESC' }]"
                size="small"
                style="width: 120px;"
              />
              <n-button text type="error" size="small" @click="config.orderBy.splice(idx, 1)">
                <n-icon><TrashOutline /></n-icon>
              </n-button>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </div>

    <!-- SQL 预览 -->
    <div class="sql-preview">
      <div class="sql-preview-header">
        <span>生成的 SQL</span>
        <n-space size="small">
          <n-button size="tiny" text type="primary" @click="toggleFormatSQL">
            {{ isFormatted ? '压缩' : '格式化' }}
          </n-button>
          <n-button size="tiny" text type="primary" @click="copySQL">复制</n-button>
        </n-space>
      </div>
      <pre class="sql-code">{{ displaySQL || '-- 请在画布中添加表并选择字段' }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, watch, h, onMounted, onUnmounted } from 'vue'
import { NInput, NSelect, NButton, NTag, NSpace, useMessage } from 'naive-ui'
import { GridOutline, TrashOutline, HelpCircleOutline, MoveOutline, AddCircleOutline, ReorderThreeOutline } from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import { getDataSourceTables, getTableColumns } from '@/api/dataSource'
import AggregationWizard from './AggregationWizard.vue'
import ConditionBuilder from './ConditionBuilder.vue'
import {
  generateSQL, createEmptyConfig,
  type QueryConfig, type SelectedField, type JoinConfig, type ConditionItem, type SortItem, type GroupByItem, type TableConfig
} from './sqlGenerator'
import { formatSQL, compressSQL } from './sqlFormatter'

const props = defineProps<{
  dataSourceId: number | null
}>()

const emit = defineEmits<{
  (e: 'update:sql', sql: string): void
}>()

const message = useMessage()
const config = reactive<QueryConfig>(createEmptyConfig())
const tablesLoading = ref(false)
const tableSearchKeyword = ref('')
const expandedTables = ref<Set<string>>(new Set())
const activeTab = ref('fields')
const canvasRef = ref<HTMLDivElement | null>(null)

// 表列表
const allTables = ref<Array<{ tableName: string; tableType: string }>>([])
const tableColumnsCache = ref<Record<string, Array<{ columnName: string; dataType: string; nullable: boolean }>>>({})

// 表卡片位置
const tablePositions = reactive<Record<string, { x: number; y: number }>>({})

// 拖拽状态
let dragState: { alias: string; startX: number; startY: number; origX: number; origY: number } | null = null

// 关联创建状态
const joiningState = ref<{ alias: string; tableName: string; field: string } | null>(null)
const mousePos = ref({ x: 0, y: 0 })

// 拖拽到画布状态
const isDragOver = ref(false)
const dragType = ref<'table' | 'field' | null>(null)
const dragData = ref<{ tableName: string; column?: { columnName: string; dataType: string } } | null>(null)

// 表卡片布局常量
const CARD_WIDTH = 210
const HEADER_HEIGHT = 30
const FIELD_PADDING_TOP = 4
const FIELD_ROW_HEIGHT = 24

// 生成的SQL
const generatedSQL = computed(() => generateSQL(config))

// SQL 格式化状态
const isFormatted = ref(true)  // 默认显示格式化后的 SQL

// 显示的 SQL（根据格式化状态）
const displaySQL = computed(() => {
  if (!generatedSQL.value) return ''
  return isFormatted.value ? formatSQL(generatedSQL.value) : compressSQL(generatedSQL.value)
})

// 切换格式化状态
function toggleFormatSQL() {
  isFormatted.value = !isFormatted.value
}

// 当SQL变化时 emit
watch(generatedSQL, (sql) => {
  emit('update:sql', sql)
})

// 数据源变化时加载表列表
watch(() => props.dataSourceId, async (newId) => {
  // 重置
  Object.assign(config, createEmptyConfig())
  allTables.value = []
  tableColumnsCache.value = {}
  expandedTables.value = new Set()
  Object.keys(tablePositions).forEach(k => delete tablePositions[k])

  if (!newId) return
  tablesLoading.value = true
  try {
    const res = await getDataSourceTables(newId)
    allTables.value = (res as any).data || []
  } catch {
    allTables.value = []
  } finally {
    tablesLoading.value = false
  }
}, { immediate: true })

const filteredTables = computed(() => {
  if (!tableSearchKeyword.value) return allTables.value
  const kw = tableSearchKeyword.value.toLowerCase()
  return allTables.value.filter(t => t.tableName.toLowerCase().includes(kw))
})

// 所有可选字段（table.field 格式）
const allFieldOptions = computed(() => {
  const opts: Array<{ label: string; value: string }> = []
  for (const table of config.tables) {
    const cols = tableColumnsCache.value[table.tableName] || []
    for (const col of cols) {
      opts.push({ label: `${table.alias}.${col.columnName}`, value: `${table.alias}.${col.columnName}` })
    }
  }
  return opts
})

// 为聚合向导提供的字段列表（包含更多元数据）
const availableFieldsForWizard = computed(() => {
  const fields: Array<{ 
    label: string
    value: string
    tableAlias: string
    tableName: string
    fieldName: string
    dataType?: string 
  }> = []
  for (const table of config.tables) {
    const cols = tableColumnsCache.value[table.tableName] || []
    for (const col of cols) {
      fields.push({
        label: `${table.alias}.${col.columnName}`,
        value: `${table.alias}.${col.columnName}`,
        tableAlias: table.alias,
        tableName: table.tableName,
        fieldName: col.columnName,
        dataType: col.dataType
      })
    }
  }
  return fields
})

// 聚合函数选项
const aggregateOptions = [
  { label: '无', value: '' },
  { label: 'SUM', value: 'SUM' },
  { label: 'COUNT', value: 'COUNT' },
  { label: 'AVG', value: 'AVG' },
  { label: 'MAX', value: 'MAX' },
  { label: 'MIN', value: 'MIN' }
]

const operatorOptions = [
  { label: '=', value: '=' },
  { label: '!=', value: '!=' },
  { label: '>', value: '>' },
  { label: '<', value: '<' },
  { label: '>=', value: '>=' },
  { label: '<=', value: '<=' },
  { label: 'LIKE', value: 'LIKE' },
  { label: 'IN', value: 'IN' },
  { label: 'BETWEEN', value: 'BETWEEN' },
  { label: 'IS NULL', value: 'IS NULL' },
  { label: 'IS NOT NULL', value: 'IS NOT NULL' }
]

const logicOptions = [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' }
]

// 字段配置表格列
const fieldColumns = [
  { title: '表', key: 'tableAlias', width: 100 },
  { title: '字段', key: 'fieldName', width: 140 },
  {
    title: '别名',
    key: 'alias',
    width: 140,
    render: (row: SelectedField, idx: number) => h(NInput, {
      value: row.alias || '',
      size: 'small',
      placeholder: row.fieldName,
      onUpdateValue: (v: string) => { config.selectedFields[idx].alias = v }
    })
  },
  {
    title: '聚合',
    key: 'aggregate',
    width: 110,
    render: (row: SelectedField, idx: number) => h(NSelect, {
      value: row.aggregate || '',
      options: aggregateOptions,
      size: 'small',
      onUpdateValue: (v: string) => { config.selectedFields[idx].aggregate = v as any }
    })
  },
  {
    title: '',
    key: 'actions',
    width: 40,
    render: (_: any, idx: number) => h(NButton, {
      text: true, type: 'error', size: 'small',
      onClick: () => { config.selectedFields.splice(idx, 1) }
    }, { default: () => '✕' })
  }
]

// === 操作函数 ===

async function loadTableColumns(tableName: string) {
  if (tableColumnsCache.value[tableName] || !props.dataSourceId) return
  try {
    const res = await getTableColumns(props.dataSourceId, tableName)
    tableColumnsCache.value[tableName] = (res as any).data || []
  } catch {
    tableColumnsCache.value[tableName] = []
  }
}

function toggleTableExpand(tableName: string) {
  if (expandedTables.value.has(tableName)) {
    expandedTables.value.delete(tableName)
  } else {
    expandedTables.value.add(tableName)
    loadTableColumns(tableName)
  }
  expandedTables.value = new Set(expandedTables.value) // trigger reactivity
}

function addTableToCanvas(tableName: string) {
  // 生成别名
  let alias = tableName
  const existing = config.tables.filter(t => t.tableName === tableName)
  if (existing.length > 0) {
    alias = `${tableName}_${existing.length + 1}`
  }

  const table: TableConfig = { tableName, alias, selectedFields: [] }
  config.tables.push(table)

  // 设置位置
  const idx = config.tables.length - 1
  tablePositions[alias] = { x: 20 + (idx % 3) * 240, y: 20 + Math.floor(idx / 3) * 200 }

  // 加载字段
  loadTableColumns(tableName)
  
  message.success(`已添加表 ${tableName}`)
}

function removeTable(idx: number) {
  const table = config.tables[idx]
  // 移除相关 joins
  config.joins = config.joins.filter(j => j.leftAlias !== table.alias && j.rightAlias !== table.alias)
  // 移除相关 selected fields
  config.selectedFields = config.selectedFields.filter(f => f.tableAlias !== table.alias)
  // 移除条件中引用
  config.conditions = config.conditions.filter(c => !c.field.startsWith(table.alias + '.'))
  config.groupBy = config.groupBy.filter(g => !g.field.startsWith(table.alias + '.'))
  config.orderBy = config.orderBy.filter(o => !o.field.startsWith(table.alias + '.'))
  // 移除位置
  delete tablePositions[table.alias]
  config.tables.splice(idx, 1)
}

function toggleField(table: TableConfig, fieldName: string, checked: boolean) {
  if (checked) {
    table.selectedFields.push(fieldName)
    config.selectedFields.push({
      tableAlias: table.alias,
      tableName: table.tableName,
      fieldName,
      alias: '',
      aggregate: '',
      id: `field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    })
  } else {
    table.selectedFields = table.selectedFields.filter(f => f !== fieldName)
    config.selectedFields = config.selectedFields.filter(
      f => !(f.tableAlias === table.alias && f.fieldName === fieldName)
    )
  }
}

// === 拖拽表和字段到画布 ===
function handleTableDragStart(e: DragEvent, tableName: string) {
  if (!e.dataTransfer) return
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/json', JSON.stringify({ type: 'table', tableName }))
  dragType.value = 'table'
  dragData.value = { tableName }
  
  // 设置拖拽图像
  const dragImage = document.createElement('div')
  dragImage.className = 'drag-ghost'
  dragImage.innerHTML = `<span style="background: var(--color-primary); color: white; padding: 4px 12px; border-radius: 4px; font-size: 12px; white-space: nowrap;">📊 ${tableName}</span>`
  dragImage.style.position = 'absolute'
  dragImage.style.top = '-1000px'
  document.body.appendChild(dragImage)
  e.dataTransfer.setDragImage(dragImage, 0, 0)
  setTimeout(() => document.body.removeChild(dragImage), 0)
}

function handleFieldDragStart(e: DragEvent, tableName: string, column: { columnName: string; dataType: string }) {
  if (!e.dataTransfer) return
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/json', JSON.stringify({ type: 'field', tableName, column }))
  dragType.value = 'field'
  dragData.value = { tableName, column }
  
  // 设置拖拽图像
  const dragImage = document.createElement('div')
  dragImage.className = 'drag-ghost'
  dragImage.innerHTML = `<span style="background: #18a058; color: white; padding: 4px 12px; border-radius: 4px; font-size: 12px; white-space: nowrap;">📋 ${column.columnName}</span>`
  dragImage.style.position = 'absolute'
  dragImage.style.top = '-1000px'
  document.body.appendChild(dragImage)
  e.dataTransfer.setDragImage(dragImage, 0, 0)
  setTimeout(() => document.body.removeChild(dragImage), 0)
}

function handleDragEnd() {
  isDragOver.value = false
  dragType.value = null
  dragData.value = null
}

function handleCanvasDragOver(e: DragEvent) {
  e.preventDefault()
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'copy'
  }
  isDragOver.value = true
}

function handleCanvasDragLeave(e: DragEvent) {
  // 只有当离开画布区域时才重置状态
  const rect = canvasRef.value?.getBoundingClientRect()
  if (rect && (e.clientX < rect.left || e.clientX > rect.right || e.clientY < rect.top || e.clientY > rect.bottom)) {
    isDragOver.value = false
  }
}

function handleCanvasDrop(e: DragEvent) {
  e.preventDefault()
  isDragOver.value = false
  
  if (!e.dataTransfer) return
  
  try {
    const data = JSON.parse(e.dataTransfer.getData('application/json'))
    
    if (data.type === 'table') {
      // 添加表到画布
      addTableToCanvasAtPosition(data.tableName, e)
      message.success(`已添加表 ${data.tableName}`)
    } else if (data.type === 'field') {
      // 添加字段
      addFieldFromDrop(data.tableName, data.column)
      message.success(`已添加字段 ${data.column.columnName}`)
    }
  } catch (err) {
    console.error('Drop error:', err)
  }
  
  dragType.value = null
  dragData.value = null
}

function addTableToCanvasAtPosition(tableName: string, e: DragEvent) {
  // 生成别名
  let alias = tableName
  const existing = config.tables.filter(t => t.tableName === tableName)
  if (existing.length > 0) {
    alias = `${tableName}_${existing.length + 1}`
  }

  const table: TableConfig = { tableName, alias, selectedFields: [] }
  config.tables.push(table)

  // 计算放置位置（相对于画布）
  if (canvasRef.value) {
    const rect = canvasRef.value.getBoundingClientRect()
    const x = Math.max(20, e.clientX - rect.left - CARD_WIDTH / 2 + canvasRef.value.scrollLeft)
    const y = Math.max(20, e.clientY - rect.top - HEADER_HEIGHT / 2 + canvasRef.value.scrollTop)
    tablePositions[alias] = { x, y }
  } else {
    const idx = config.tables.length - 1
    tablePositions[alias] = { x: 20 + (idx % 3) * 240, y: 20 + Math.floor(idx / 3) * 200 }
  }

  // 加载字段
  loadTableColumns(tableName)
}

function addFieldFromDrop(tableName: string, column: { columnName: string; dataType: string }) {
  // 首先确保表已添加到画布
  let tableConfig = config.tables.find(t => t.tableName === tableName)
  
  if (!tableConfig) {
    // 如果表不在画布上，先添加表
    let alias = tableName
    const existing = config.tables.filter(t => t.tableName === tableName)
    if (existing.length > 0) {
      alias = `${tableName}_${existing.length + 1}`
    }
    
    tableConfig = { tableName, alias, selectedFields: [] }
    config.tables.push(tableConfig)
    
    const idx = config.tables.length - 1
    tablePositions[tableConfig.alias] = { x: 20 + (idx % 3) * 240, y: 20 + Math.floor(idx / 3) * 200 }
    
    loadTableColumns(tableName)
  }
  
  // 检查字段是否已选中
  const fieldExists = config.selectedFields.some(
    f => f.tableAlias === tableConfig!.alias && f.fieldName === column.columnName
  )
  
  if (!fieldExists) {
    tableConfig.selectedFields.push(column.columnName)
    config.selectedFields.push({
      tableAlias: tableConfig.alias,
      tableName: tableConfig.tableName,
      fieldName: column.columnName,
      alias: '',
      aggregate: '',
      id: `field_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
    })
  }
}

function removeSelectedField(index: number) {
  const field = config.selectedFields[index]
  if (field) {
    // 同步更新表卡片中的选中状态
    const tableConfig = config.tables.find(t => t.alias === field.tableAlias)
    if (tableConfig) {
      tableConfig.selectedFields = tableConfig.selectedFields.filter(f => f !== field.fieldName)
    }
    config.selectedFields.splice(index, 1)
  }
}

// 卡片拖拽
function startDragCard(e: MouseEvent, alias: string) {
  if ((e.target as HTMLElement).closest('.table-card-field') ||
      (e.target as HTMLElement).closest('button')) return
  const pos = tablePositions[alias]
  if (!pos) return
  dragState = { alias, startX: e.clientX, startY: e.clientY, origX: pos.x, origY: pos.y }
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e: MouseEvent) {
  if (!dragState) return
  const dx = e.clientX - dragState.startX
  const dy = e.clientY - dragState.startY
  tablePositions[dragState.alias] = {
    x: Math.max(0, dragState.origX + dx),
    y: Math.max(0, dragState.origY + dy)
  }
}

function onDragEnd() {
  dragState = null
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// === 关联创建 ===
function startJoinDrag(e: MouseEvent, alias: string, tableName: string, field: string) {
  e.preventDefault()
  if (!joiningState.value) {
    joiningState.value = { alias, tableName, field }
    // 初始化鼠标位置
    if (canvasRef.value) {
      const rect = canvasRef.value.getBoundingClientRect()
      mousePos.value = {
        x: e.clientX - rect.left + canvasRef.value.scrollLeft,
        y: e.clientY - rect.top + canvasRef.value.scrollTop
      }
    }
  } else {
    if (joiningState.value.alias === alias) {
      joiningState.value = null
      return
    }
    const join: JoinConfig = {
      id: `join_${Date.now()}`,
      leftTable: joiningState.value.tableName,
      leftAlias: joiningState.value.alias,
      leftField: joiningState.value.field,
      rightTable: tableName,
      rightAlias: alias,
      rightField: field,
      joinType: 'INNER'
    }
    config.joins.push(join)
    joiningState.value = null
    message.success('关联已创建')
  }
}

function cancelJoining() {
  joiningState.value = null
}

function onCanvasMouseMove(e: MouseEvent) {
  if (!joiningState.value || !canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  mousePos.value = {
    x: e.clientX - rect.left + canvasRef.value.scrollLeft,
    y: e.clientY - rect.top + canvasRef.value.scrollTop
  }
}

function cycleJoinType(join: JoinConfig) {
  const types: JoinConfig['joinType'][] = ['INNER', 'LEFT', 'RIGHT', 'FULL']
  const idx = types.indexOf(join.joinType)
  join.joinType = types[(idx + 1) % types.length]
}

function removeJoin(id: string) {
  config.joins = config.joins.filter(j => j.id !== id)
}

// === 关联线绘制辅助 ===
function getFieldIndex(tableName: string, fieldName: string): number {
  const cols = tableColumnsCache.value[tableName] || []
  const idx = cols.findIndex(c => c.columnName === fieldName)
  return idx >= 0 ? idx : 0
}

function getFieldPosition(alias: string, tableName: string, fieldName: string, side: 'left' | 'right') {
  const pos = tablePositions[alias]
  if (!pos) return { x: 0, y: 0 }
  const fieldIdx = getFieldIndex(tableName, fieldName)
  const y = pos.y + HEADER_HEIGHT + FIELD_PADDING_TOP + fieldIdx * FIELD_ROW_HEIGHT + FIELD_ROW_HEIGHT / 2
  const x = side === 'right' ? pos.x + CARD_WIDTH : pos.x
  return { x, y }
}

function getJoinEndpoints(join: JoinConfig) {
  const leftPos = tablePositions[join.leftAlias]
  const rightPos = tablePositions[join.rightAlias]
  if (!leftPos || !rightPos) return { start: { x: 0, y: 0 }, end: { x: 0, y: 0 } }
  const leftCenterX = leftPos.x + CARD_WIDTH / 2
  const rightCenterX = rightPos.x + CARD_WIDTH / 2
  const leftSide: 'left' | 'right' = leftCenterX <= rightCenterX ? 'right' : 'left'
  const rightSide: 'left' | 'right' = leftCenterX <= rightCenterX ? 'left' : 'right'
  return {
    start: getFieldPosition(join.leftAlias, join.leftTable, join.leftField, leftSide),
    end: getFieldPosition(join.rightAlias, join.rightTable, join.rightField, rightSide)
  }
}

function buildBezierPath(start: { x: number; y: number }, end: { x: number; y: number }) {
  const dx = Math.abs(end.x - start.x)
  const cpOffset = Math.max(50, dx * 0.4)
  const cp1x = start.x + (start.x < end.x ? cpOffset : -cpOffset)
  const cp2x = end.x + (start.x < end.x ? -cpOffset : cpOffset)
  return `M ${start.x} ${start.y} C ${cp1x} ${start.y} ${cp2x} ${end.y} ${end.x} ${end.y}`
}

function getJoinPath(join: JoinConfig) {
  const { start, end } = getJoinEndpoints(join)
  return buildBezierPath(start, end)
}

function getJoinMidpoint(join: JoinConfig) {
  const { start, end } = getJoinEndpoints(join)
  // 贝塞尔曲线中点近似
  return { x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 }
}

function joinTypeColor(type: string) {
  switch (type) {
    case 'INNER': return 'var(--color-primary)'
    case 'LEFT': return '#18a058'
    case 'RIGHT': return '#f0a020'
    case 'FULL': return '#8b5cf6'
    default: return 'var(--color-primary)'
  }
}

function isFieldJoined(alias: string, fieldName: string) {
  return config.joins.some(j =>
    (j.leftAlias === alias && j.leftField === fieldName) ||
    (j.rightAlias === alias && j.rightField === fieldName)
  )
}

function getFieldJoinColor(alias: string, fieldName: string) {
  const join = config.joins.find(j =>
    (j.leftAlias === alias && j.leftField === fieldName) ||
    (j.rightAlias === alias && j.rightField === fieldName)
  )
  return join ? joinTypeColor(join.joinType) : 'var(--color-primary)'
}

function getJoiningSourcePos() {
  if (!joiningState.value) return { x: 0, y: 0 }
  return getFieldPosition(
    joiningState.value.alias, joiningState.value.tableName,
    joiningState.value.field, 'right'
  )
}

function getTempJoinPath() {
  if (!joiningState.value) return ''
  const sourcePos = getJoiningSourcePos()
  return buildBezierPath(sourcePos, mousePos.value)
}

function autoLayout() {
  config.tables.forEach((t, i) => {
    tablePositions[t.alias] = { x: 20 + (i % 3) * 240, y: 20 + Math.floor(i / 3) * 200 }
  })
}

function addCondition(arr: ConditionItem[]) {
  arr.push({ id: `cond_${Date.now()}`, field: '', operator: '=', value: '', logic: 'AND' })
}

function addGroupBy() {
  config.groupBy.push({ field: '' })
}

// 聚合向导的更新处理函数
function handleGroupByUpdate(newGroupBy: GroupByItem[]) {
  config.groupBy = newGroupBy
}

function handleSelectedFieldsUpdate(newFields: SelectedField[]) {
  config.selectedFields = newFields
  // 同步更新表卡片中的选中状态
  for (const table of config.tables) {
    table.selectedFields = newFields
      .filter(f => f.tableAlias === table.alias)
      .map(f => f.fieldName)
  }
}

function handleHavingUpdate(newHaving: ConditionItem[]) {
  config.having = newHaving
}

function addOrderBy() {
  config.orderBy.push({ field: '', direction: 'ASC' })
}

function copySQL() {
  if (!displaySQL.value) return
  navigator.clipboard.writeText(displaySQL.value)
  message.success('SQL 已复制')
}

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape' && joiningState.value) {
    joiningState.value = null
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeyDown)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.query-designer {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

/* 上部: 表浏览 + 画布 */
.designer-top {
  display: flex;
  gap: 12px;
  height: 360px;
}

/* 左侧表浏览 */
.table-browser {
  width: 220px;
  flex-shrink: 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 10px;
  overflow-y: auto;
}

.browser-header {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #334155;
}

.table-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.table-item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.15s;
}

.table-item-header:hover {
  background: #e2e8f0;
}

.table-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 拖拽提示图标 */
.drag-hint-icon {
  opacity: 0;
  transition: opacity 0.15s;
  margin-left: auto;
  margin-right: 4px;
}

.table-item-header:hover .drag-hint-icon {
  opacity: 1;
}

.table-item-header[draggable="true"] {
  cursor: grab;
}

.table-item-header[draggable="true"]:active {
  cursor: grabbing;
}

.field-list {
  padding-left: 20px;
  border-left: 2px solid #e2e8f0;
  margin-left: 10px;
}

.field-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 4px;
  font-size: 11px;
  color: #64748b;
  cursor: grab;
  border-radius: 4px;
  transition: background 0.15s;
}

.field-item:hover {
  background: #e2e8f0;
}

.field-item:active {
  cursor: grabbing;
}

.field-drag-handle {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.field-item:hover .field-drag-handle {
  opacity: 1;
}

.field-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 画布 */
.canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.canvas-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.canvas-content {
  flex: 1;
  position: relative;
  overflow: auto;
  background:
    linear-gradient(rgba(0,0,0,.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,.02) 1px, transparent 1px);
  background-size: 20px 20px;
  transition: background-color 0.2s, border-color 0.2s;
}

.canvas-content.drag-over {
  background-color: rgba(0, 102, 255, 0.03);
  border: 2px dashed var(--color-primary);
  border-radius: 0 0 8px 8px;
}

.canvas-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.placeholder-content p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.placeholder-hint {
  font-size: 12px !important;
  color: #94a3b8 !important;
}

/* 拖拽放置覆盖层 */
.drop-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 102, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  pointer-events: none;
}

.drop-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 32px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 102, 255, 0.2);
  border: 2px dashed var(--color-primary);
}

.drop-hint span {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary);
}

/* 表卡片 */
.table-card {
  position: absolute;
  width: 210px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  cursor: grab;
  z-index: 1;
}

.table-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,.1);
}

.table-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 8px 8px 0 0;
  font-size: 12px;
  font-weight: 600;
}

.table-alias {
  font-weight: 400;
  opacity: 0.7;
  font-size: 10px;
}

.table-card-fields {
  max-height: 160px;
  overflow-y: auto;
  padding: 4px 0;
}

.table-card-field {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-size: 11px;
  cursor: pointer;
}

.table-card-field:hover {
  background: #f0f5ff;
}

.field-name-in-card {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}

/* JOIN 连线 */
.join-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 2;
  overflow: visible;
}

.join-curve {
  transition: stroke-width 0.15s;
}

.join-line-group:hover .join-curve {
  stroke-width: 3.5;
}

.join-badge-group {
  pointer-events: all;
  cursor: pointer;
}

.join-type-badge {
  cursor: pointer;
  transition: filter 0.15s;
}

.join-type-badge:hover {
  filter: brightness(1.15);
}

.join-delete-btn {
  opacity: 0;
  transition: opacity 0.15s;
}

.join-line-group:hover .join-delete-btn {
  opacity: 1;
}

.join-delete-btn circle {
  transition: opacity 0.15s;
}

.join-line-group:hover .join-delete-btn circle {
  opacity: 1;
}

/* 关联创建模式 */
.joining-mode {
  cursor: crosshair;
}

.join-status-bar {
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(0, 102, 255, 0.9);
  color: #fff;
  border-radius: 20px;
  font-size: 12px;
  box-shadow: 0 2px 12px rgba(0, 102, 255, 0.3);
  white-space: nowrap;
  backdrop-filter: blur(4px);
}

.join-status-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fff;
  animation: pulse-dot 1.2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.7); }
}

/* 关联源字段高亮 */
.join-source-field {
  background: rgba(0, 102, 255, 0.12) !important;
  box-shadow: inset 3px 0 0 var(--color-primary);
}

/* 关联目标字段提示 */
.join-target-field {
  cursor: crosshair !important;
  transition: background 0.15s, box-shadow 0.15s;
}

.join-target-field:hover {
  background: rgba(24, 160, 88, 0.1) !important;
  box-shadow: inset 3px 0 0 #18a058;
}

/* 已关联字段标记圆点 */
.field-join-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-left: auto;
}

/* 配置面板 */
.config-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fff;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0;
}

.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 选中字段列表 - 可拖拽排序 */
.selected-fields-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.selected-field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 12px;
  transition: background 0.15s, box-shadow 0.15s;
}

.selected-field-row:hover {
  background: #f1f5f9;
}

.selected-field-row .field-drag-handle {
  cursor: grab;
  opacity: 0.5;
  transition: opacity 0.15s;
}

.selected-field-row:hover .field-drag-handle {
  opacity: 1;
}

.selected-field-row .field-drag-handle:active {
  cursor: grabbing;
}

.field-table-alias {
  color: var(--color-primary);
  font-weight: 500;
}

.field-dot {
  color: #94a3b8;
}

.field-name-text {
  color: #334155;
  flex: 1;
  min-width: 80px;
}

/* 拖拽排序样式 */
.field-ghost {
  opacity: 0.5;
  background: #e0f2fe !important;
  border-color: var(--color-primary) !important;
}

.field-chosen {
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.2);
}

/* SQL 预览 */
.sql-preview {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.sql-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #1e293b;
  color: #e2e8f0;
  font-size: 12px;
  font-weight: 600;
}

.sql-code {
  margin: 0;
  padding: 10px 12px;
  background: #0f172a;
  color: #a5f3fc;
  font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  max-height: 120px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.empty-hint {
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
  padding: 16px 0;
}

.empty-hint p {
  margin: 0;
}

.table-card-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 浏览器头部样式 */
.browser-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #334155;
}
</style>
