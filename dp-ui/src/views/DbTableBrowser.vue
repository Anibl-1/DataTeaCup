<template>
  <div>
    <!-- 表结构标签页 -->
    <div v-if="activeTab === 'structure'" class="tab-content">
      <div v-if="!currentTable" class="empty-content">
        <div class="empty-icon">🏗️</div>
        <h3>请从左侧选择一个表</h3>
        <p>查看表的字段结构和约束信息</p>
      </div>
      <div v-else>
        <div class="result-header">
          <span class="result-title">🏗️ {{ currentTable }} 表结构</span>
        </div>
        <n-spin :show="loadingStructure">
          <n-data-table :columns="structureColumns" :data="tableStructure" size="small" striped />
        </n-spin>
      </div>
    </div>

    <!-- 视图标签页 -->
    <div v-if="activeTab === 'view'" class="tab-content">
      <div v-if="!viewDefinition" class="empty-content">
        <div class="empty-icon">👁️</div>
        <h3>请从左侧选择一个视图</h3>
        <p>查看视图的定义和结构</p>
      </div>
      <div v-else>
        <div class="result-header">
          <span class="result-title">👁️ {{ currentView }} 视图定义</span>
          <n-space>
            <n-button size="small" @click="formatViewDefinition">🎨 格式化</n-button>
            <n-button size="small" @click="copyToClipboard(viewDefinition)">📋 复制</n-button>
          </n-space>
        </div>
        <SqlEditor :model-value="formattedViewDefinition" height="300px" read-only />
        <n-button type="primary" style="margin-top: 15px;" @click="$emit('queryViewData')">📊 查询视图数据</n-button>
      </div>
    </div>

    <!-- 存储过程标签页 -->
    <div v-if="activeTab === 'procedure'" class="tab-content">
      <div v-if="!procedureDefinition" class="empty-content">
        <div class="empty-icon">⚙️</div>
        <h3>请从左侧选择一个存储过程</h3>
        <p>查看存储过程的定义和参数</p>
      </div>
      <div v-else>
        <div class="result-header">
          <span class="result-title">⚙️ {{ currentProcedure }} 存储过程定义</span>
          <n-space>
            <n-button size="small" @click="formatProcedureDefinition">🎨 格式化</n-button>
            <n-button size="small" @click="copyToClipboard(procedureDefinition)">📋 复制</n-button>
          </n-space>
        </div>
        <SqlEditor :model-value="formattedProcedureDefinition" height="300px" read-only />
        <div class="procedure-hint">
          <div class="hint-title">💡 执行提示</div>
          <div class="hint-content">可以在SQL执行器中调用此存储过程，例如：<br><code>CALL {{ currentProcedure }}(参数1, 参数2);</code></div>
        </div>
      </div>
    </div>

    <!-- SQL历史标签页 -->
    <div v-if="activeTab === 'history'" class="tab-content">
      <div class="toolbar">
        <n-input v-model:value="historyKeyword" placeholder="搜索SQL..." style="width: 250px;" clearable @update:value="loadSqlHistory" />
        <n-select v-model:value="historyStatus" :options="historyStatusOptions" placeholder="状态" style="width: 120px;" clearable @update:value="loadSqlHistory" />
        <n-button @click="loadSqlHistory">🔍 查询</n-button>
        <n-button type="error" :disabled="!sessionId" @click="handleClearHistory">🗑️ 清空历史</n-button>
      </div>
      <n-spin :show="loadingHistory">
        <n-data-table
          :columns="historyColumns"
          :data="historyList"
          :max-height="500" size="small" striped
          :pagination="{ pageSize: 20 }"
        />
      </n-spin>
      <div v-if="historyTotal > 0" class="form-hint" style="text-align: right; margin-top: 8px;">共 {{ historyTotal }} 条记录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch, h } from 'vue'
import { useMessage } from 'naive-ui'
import * as dbManagerApi from '@/api/dbManager'
import SqlEditor from '@/components/SqlEditor.vue'

const props = defineProps<{
  sessionId: string | null
  activeTab: string
  currentTable: string | null
  currentView: string | null
  currentProcedure: string | null
}>()

const emit = defineEmits<{
  queryViewData: []
  useSql: [sql: string]
}>()

const message = useMessage()

// ---- 表结构 ----
const loadingStructure = ref(false)
const tableStructure = ref<any[]>([])
const structureColumns = [
  { title: '字段名', key: 'columnName', width: 150, render: (row: any) => h('strong', null, row.columnName) },
  { title: '数据类型', key: 'dataType', width: 120 },
  { title: '长度', key: 'columnSize', width: 80, render: (row: any) => row.columnSize || '-' },
  { title: '允许空值', key: 'nullable', width: 90, render: (row: any) => row.nullable === 'YES' ? '✅' : '❌' },
  { title: '默认值', key: 'defaultValue', width: 100, render: (row: any) => row.defaultValue || '-' },
  { title: '主键', key: 'isPrimaryKey', width: 60, render: (row: any) => row.isPrimaryKey ? '🔑' : '-' },
  { title: '注释', key: 'remarks', ellipsis: { tooltip: true }, render: (row: any) => row.remarks || '-' },
]

const loadTableStructure = async () => {
  if (!props.sessionId || !props.currentTable) return
  loadingStructure.value = true
  try {
    const res = await dbManagerApi.getTableStructure(props.sessionId, props.currentTable)
    tableStructure.value = res.data || []
  } catch {
    message.error('加载表结构失败')
  } finally {
    loadingStructure.value = false
  }
}

watch(() => props.currentTable, (val) => {
  if (val) loadTableStructure()
})

// ---- 视图定义 ----
const viewDefinition = ref('')
const isViewFormatted = ref(false)

const formattedViewDefinition = computed(() => {
  if (!viewDefinition.value) return ''
  return isViewFormatted.value ? formatSqlCode(viewDefinition.value) : viewDefinition.value
})

const formatViewDefinition = () => {
  isViewFormatted.value = !isViewFormatted.value
  message.success(isViewFormatted.value ? '已格式化' : '已还原')
}

const loadViewDefinition = async (viewName: string) => {
  if (!props.sessionId) return
  isViewFormatted.value = false
  try {
    const res = await dbManagerApi.getViewDefinition(props.sessionId, viewName)
    viewDefinition.value = res.data || ''
  } catch {
    message.error('获取视图定义失败')
  }
}

// ---- 存储过程定义 ----
const procedureDefinition = ref('')
const isProcedureFormatted = ref(false)

const formattedProcedureDefinition = computed(() => {
  if (!procedureDefinition.value) return ''
  return isProcedureFormatted.value ? formatSqlCode(procedureDefinition.value) : procedureDefinition.value
})

const formatProcedureDefinition = () => {
  isProcedureFormatted.value = !isProcedureFormatted.value
  message.success(isProcedureFormatted.value ? '已格式化' : '已还原')
}

const loadProcedureDefinition = async (procedureName: string) => {
  if (!props.sessionId) return
  isProcedureFormatted.value = false
  try {
    const res = await dbManagerApi.getProcedureDefinition(props.sessionId, procedureName)
    procedureDefinition.value = res.data || ''
  } catch {
    message.error('获取存储过程定义失败')
  }
}

// ---- SQL 格式化工具 ----
const formatSqlCode = (sql: string): string => {
  if (!sql) return ''
  let formatted = sql
  const keywords = [
    'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'ORDER BY', 'GROUP BY', 'HAVING',
    'LIMIT', 'OFFSET', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'OUTER JOIN',
    'CROSS JOIN', 'ON', 'AS', 'INSERT INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE FROM',
    'CREATE', 'ALTER', 'DROP', 'TABLE', 'VIEW', 'INDEX', 'PROCEDURE', 'FUNCTION',
    'BEGIN', 'END', 'IF', 'THEN', 'ELSE', 'ELSEIF', 'CASE', 'WHEN', 'DECLARE',
    'RETURNS', 'RETURN', 'WHILE', 'DO', 'LOOP', 'CURSOR', 'FETCH', 'INTO',
    'UNION', 'UNION ALL', 'EXCEPT', 'INTERSECT', 'IN', 'NOT IN', 'EXISTS', 'NOT EXISTS',
    'LIKE', 'BETWEEN', 'IS NULL', 'IS NOT NULL', 'ASC', 'DESC', 'DISTINCT',
    'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'COALESCE', 'IFNULL', 'NULLIF',
  ]
  keywords.forEach((kw) => {
    const regex = new RegExp(`\\b${kw.replace(/ /g, '\\s+')}\\b`, 'gi')
    formatted = formatted.replace(regex, '\n' + kw.toUpperCase())
  })
  formatted = formatted.replace(/,\s*/g, ',\n    ')
  formatted = formatted.replace(/\(\s*/g, '(\n    ')
  formatted = formatted.replace(/\s*\)/g, '\n)')
  formatted = formatted.replace(/\n\s*\n/g, '\n')
  return formatted.trim()
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// ---- SQL 历史 ----
const historyKeyword = ref('')
const historyStatus = ref<string | null>(null)
const historyStatusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'error' },
]
const loadingHistory = ref(false)
const historyList = ref<any[]>([])
const historyTotal = ref(0)
const historyColumns = [
  {
    title: 'SQL', key: 'sqlContent', ellipsis: { tooltip: true }, width: 300,
    render: (row: any) => h('span', { style: 'cursor: pointer; color: var(--color-primary, #2563eb);', onClick: () => emit('useSql', row.sqlContent) }, row.sqlContent),
  },
  {
    title: '状态', key: 'status', width: 70, render: (row: any) => {
      if (row.status === 'success') return '✅'
      if (row.status === 'saved') return h('span', { style: 'color: var(--color-primary);' }, '💾')
      return '❌'
    },
  },
  { title: '耗时(ms)', key: 'executeTime', width: 90 },
  { title: '影响行数', key: 'affectedRows', width: 90 },
  { title: '执行时间', key: 'executeAt', width: 160 },
]

const loadSqlHistory = async () => {
  loadingHistory.value = true
  try {
    const res = await dbManagerApi.getSqlHistory({
      sessionId: props.sessionId || undefined,
      keyword: historyKeyword.value || undefined,
      status: historyStatus.value || undefined,
      page: 1,
      pageSize: 100,
    })
    const data = res.data || {}
    historyList.value = data.records || (data as any).list || []
    historyTotal.value = data.total || 0
  } catch {
    message.error('加载SQL历史失败')
  } finally {
    loadingHistory.value = false
  }
}

const handleClearHistory = async () => {
  if (!props.sessionId) return
  try {
    await dbManagerApi.clearSqlHistory(props.sessionId)
    message.success('历史记录已清空')
    await loadSqlHistory()
  } catch {
    message.error('清空失败')
  }
}

// 清除视图/存储过程定义
const clearDefinitions = () => {
  viewDefinition.value = ''
  procedureDefinition.value = ''
  tableStructure.value = []
}

defineExpose({ loadTableStructure, loadViewDefinition, loadProcedureDefinition, loadSqlHistory, clearDefinitions })
</script>

<style scoped>
.tab-content { flex: 1; padding: 20px; overflow: auto; }

/* 工具栏 */
.toolbar { display: flex; gap: var(--dp-spacing-sm); margin-bottom: 15px; padding: 15px; background: var(--bg-secondary); border-radius: var(--dp-radius-md); align-items: center; flex-wrap: wrap; }

/* 空内容 */
.empty-content { text-align: center; color: #999; padding: 60px 20px; }
.empty-content .empty-icon { font-size: 48px; margin-bottom: 20px; }
.empty-content h3 { margin: 0 0 10px; color: #666; }
.empty-content p { margin: 0; }

/* 结果头部 */
.result-header { margin-bottom: 15px; display: flex; align-items: center; gap: var(--dp-spacing-sm); }
.result-title { font-weight: 600; color: #333; }

/* 存储过程提示 */
.procedure-hint { margin-top: 15px; padding: var(--dp-spacing-sm); background: #f0f9ff; border-left: 4px solid #1890ff; border-radius: var(--dp-radius-sm); }
.hint-title { font-weight: 600; color: #1890ff; margin-bottom: 5px; }
.hint-content { color: #666; font-size: var(--dp-font-sm); }
.hint-content code { background: #f5f5f5; padding: 2px 4px; border-radius: 2px; }

</style>

<style>
/* DbTableBrowser 深色模式（非 scoped） */
html.dark .table-info { background: #1a2536 !important; }
html.dark .info-row .label { color: #64748b !important; }
html.dark .info-row .value { color: #e2e8f0 !important; }
html.dark .column-header { background: #1a2536 !important; color: #e2e8f0 !important; }
html.dark .column-name { color: #e2e8f0 !important; }
html.dark .column-type { color: #64748b !important; }
html.dark .view-definition { background: #0f172a !important; }
html.dark .no-data { color: #64748b !important; }
</style>
