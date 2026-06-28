<template>
  <DesktopOnlyTip v-if="isMobileView" title="SQL编辑器" desc="SQL编辑器需要更大的屏幕空间进行代码编辑和结果查看，请在电脑端打开。" />
  <div v-else class="tab-content">
    <div class="toolbar">
      <n-button
        type="primary"
        secondary
        :disabled="!hasExportableSqlResult"
        :loading="frontendExporting"
        @click="handleFrontendExport"
      >前端导出</n-button>
      <n-button
        type="primary"
        ghost
        :disabled="!hasExportableSqlResult"
        :loading="backendExporting"
        @click="handleBackendExport"
      >后端导出</n-button>
      <span class="toolbar-divider"></span>
      <n-button @click="formatSql">🎨 格式化</n-button>
      <n-button @click="sqlContent = ''">🗑️ 清空</n-button>
      <n-button :disabled="!sqlContent.trim()" @click="openSaveDialog">💾 保存</n-button>
      <n-button @click="openSnippetDrawer">📂 已保存SQL</n-button>
      <span class="toolbar-divider"></span>
      <n-button type="primary" @click="executeSelectedSql">▶️ 执行选中 (F9)</n-button>
      <n-button type="success" @click="executeSql">⏩ 执行全部 (Ctrl+Enter)</n-button>
      <n-button type="warning" :loading="explainLoading" @click="handleExplainSql">📊 EXPLAIN</n-button>
      <span class="toolbar-hint">💡 SQL语法高亮 | Ctrl+Enter 执行全部 | F9 执行选中</span>
    </div>
    <SqlEditor
      ref="sqlEditorRef"
      v-model="sqlContent"
      height="320px"
      :tables="editorTableSchema"
      @execute="executeSql"
      @execute-selected="executeSqlText"
    />
    <div v-if="sqlResult" class="sql-result">
      <n-alert :type="sqlResult.success ? 'success' : 'error'" style="margin-bottom: 12px;">
        {{ sqlResult.message }}
        <span v-if="sqlResult.executeTime" class="text-muted" style="margin-left: 12px;">耗时: {{ sqlResult.executeTime }}ms</span>
      </n-alert>
      <div v-if="sqlResult.type === 'SELECT' && sqlResult.data">
        <n-data-table :columns="sqlResultColumns" :data="sqlResult.data" :max-height="300" size="small" striped :pagination="{ pageSize: 50 }" />
      </div>
    </div>
    <!-- EXPLAIN 结果 -->
    <div v-if="explainResult" class="sql-result" style="margin-top: 12px;">
      <n-alert type="info" style="margin-bottom: 12px;">📊 EXPLAIN 执行计划</n-alert>
      <n-data-table
        v-if="explainResult.planData && explainResult.planData.length > 0"
        :columns="explainColumns"
        :data="explainResult.planData"
        :max-height="300" size="small" striped
      />
    </div>

    <!-- SQL保存对话框 -->
    <n-modal v-model:show="showSaveDialog" preset="dialog" title="💾 保存SQL" positive-text="保存" negative-text="取消" :loading="savingSnippet" @positive-click="handleSaveSnippet">
      <n-form :model="saveForm" label-placement="left" label-width="60">
        <n-form-item label="名称" required>
          <n-input v-model:value="saveForm.name" placeholder="给这条SQL起个名字" maxlength="200" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="saveForm.description" type="textarea" placeholder="可选描述" :rows="2" maxlength="500" />
        </n-form-item>
      </n-form>
      <div class="sql-preview-block">{{ saveForm.sqlContent }}</div>
    </n-modal>

    <!-- 已保存SQL抽屉 -->
    <n-drawer v-model:show="showSnippetDrawer" :width="520" placement="right">
      <n-drawer-content title="📂 已保存的SQL" closable>
        <template #header-extra>
          <n-input v-model:value="snippetKeyword" placeholder="搜索..." size="small" style="width: 180px;" clearable @update:value="loadSnippets" />
        </template>
        <n-spin :show="loadingSnippets">
          <div v-if="snippetList.length === 0" class="text-muted" style="text-align: center; padding: 40px;">暂无保存的SQL</div>
          <div v-else class="snippet-list">
            <div v-for="item in snippetList" :key="item.id" class="snippet-item">
              <div class="snippet-header">
                <span class="snippet-name">{{ item.name }}</span>
                <n-space size="small">
                  <n-button text size="tiny" type="primary" @click="loadSnippetToEditor(item)">使用</n-button>
                  <n-popconfirm @positive-click="handleDeleteSnippet(item.id)">
                    <template #trigger><n-button text size="tiny" type="error">删除</n-button></template>
                    确定删除这条SQL吗？
                  </n-popconfirm>
                </n-space>
              </div>
              <div v-if="item.description" class="snippet-desc">{{ item.description }}</div>
              <pre class="snippet-sql">{{ item.sqlContent }}</pre>
              <div class="snippet-time">{{ item.dbType ? item.dbType + ' · ' : '' }}{{ item.createTime }}</div>
            </div>
          </div>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h } from 'vue'
import { useMessage } from 'naive-ui'
import DesktopOnlyTip from '@/components/mobile/DesktopOnlyTip.vue'
import { useAppStore } from '@/stores/app'
import * as dbManagerApi from '@/api/dbManager'
import SqlEditor from '@/components/SqlEditor.vue'
import { exportToExcel } from '@/utils/export'

const props = defineProps<{
  sessionId: string | null
  editorTableSchema: Record<string, string[]>
}>()

const emit = defineEmits<{
  refreshObjects: []
}>()

const appStore = useAppStore()
const isMobileView = computed(() => appStore.isMobileView)
const message = useMessage()

// SQL 内容
const sqlContent = ref('')
const sqlResult = ref<any>(null)
const executingSql = ref(false)
const sqlEditorRef = ref<any>(null)

const hasExportableSqlResult = computed(() =>
  sqlResult.value?.type === 'SELECT' &&
  Array.isArray(sqlResult.value?.data) &&
  sqlResult.value.data.length > 0
)

const sqlResultColumns = computed(() => {
  if (!sqlResult.value?.columns) return []
  return sqlResult.value.columns.map((col: string) => ({
    title: col,
    key: col,
    width: 120,
    ellipsis: { tooltip: true },
    render: (row: any) => {
      const val = row[col]
      if (val === null || val === undefined) return h('span', { style: 'color: #999; font-style: italic;' }, 'NULL')
      return String(val)
    },
  }))
})

// SQL 执行
const executeSql = async () => {
  if (!props.sessionId) { message.warning('请先连接数据库'); return }
  if (!sqlContent.value.trim()) { message.warning('请输入SQL语句'); return }
  executingSql.value = true
  sqlResult.value = null
  try {
    const res = await dbManagerApi.executeSql(props.sessionId, sqlContent.value)
    sqlResult.value = res.data
    if (res.data?.success) {
      const upperSql = sqlContent.value.toUpperCase()
      if (upperSql.includes('CREATE ') || upperSql.includes('DROP ') || upperSql.includes('ALTER ')) {
        emit('refreshObjects')
        message.success('对象列表已刷新')
      }
    }
  } catch (error: any) {
    sqlResult.value = { success: false, message: error.message || 'SQL执行失败' }
  } finally {
    executingSql.value = false
  }
}

const executeSelectedSql = () => {
  const selectedText = sqlEditorRef.value?.getSelectedText?.()?.trim()
  if (selectedText) {
    executeSqlText(selectedText)
  } else {
    executeSql()
  }
}

const executeSqlText = async (sql: string) => {
  if (!props.sessionId) { message.warning('请先连接数据库'); return }
  if (!sql.trim()) { message.warning('请输入SQL语句'); return }
  executingSql.value = true
  sqlResult.value = null
  try {
    const res = await dbManagerApi.executeSql(props.sessionId, sql)
    sqlResult.value = res.data
    if (res.data?.success) {
      const upperSql = sql.toUpperCase()
      if (upperSql.includes('CREATE ') || upperSql.includes('DROP ') || upperSql.includes('ALTER ')) {
        emit('refreshObjects')
        message.success('对象列表已刷新')
      }
    }
  } catch (error: any) {
    sqlResult.value = { success: false, message: error.message || 'SQL执行失败' }
  } finally {
    executingSql.value = false
  }
}

const formatSql = () => {
  let sql = sqlContent.value
  const keywords = ['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'ORDER BY', 'GROUP BY', 'HAVING', 'LIMIT', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'ON', 'INSERT INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE FROM']
  keywords.forEach((kw) => {
    sql = sql.replace(new RegExp(`\\b${kw}\\b`, 'gi'), '\n' + kw.toUpperCase())
  })
  sqlContent.value = sql.trim()
}

// EXPLAIN
const explainLoading = ref(false)
const explainResult = ref<any>(null)
const explainColumns = computed(() => {
  if (!explainResult.value?.planData?.[0]) return []
  return Object.keys(explainResult.value.planData[0]).map((key) => ({
    title: key, key, width: 120, ellipsis: { tooltip: true },
    render: (row: any) => row[key] != null ? String(row[key]) : '-',
  }))
})

const handleExplainSql = async () => {
  if (!props.sessionId) { message.warning('请先连接数据库'); return }
  const sql = sqlContent.value.trim()
  if (!sql) { message.warning('请输入SQL语句'); return }
  explainLoading.value = true
  explainResult.value = null
  try {
    const res = await dbManagerApi.explainSql(props.sessionId, sql)
    explainResult.value = res.data
  } catch (error: any) {
    message.error(error.message || 'EXPLAIN执行失败')
  } finally {
    explainLoading.value = false
  }
}

// 导出结果
const frontendExporting = ref(false)
const backendExporting = ref(false)

const handleFrontendExport = async () => {
  if (!hasExportableSqlResult.value) {
    message.warning('暂无可导出的查询结果')
    return
  }
  frontendExporting.value = true
  try {
    exportToExcel(sqlResult.value.data, 'sql_query_result')
    message.success('前端导出成功')
  } catch (error: any) {
    message.error(error.message || '前端导出失败')
  } finally {
    frontendExporting.value = false
  }
}

const handleBackendExport = async () => {
  if (!props.sessionId || !sqlContent.value.trim()) return
  backendExporting.value = true
  try {
    const res = await dbManagerApi.exportQueryResult(props.sessionId, sqlContent.value) as any
    const blob = new Blob([res.data || res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'query_result.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    message.success('后端导出成功')
  } catch (error: any) {
    message.error(error.message || '后端导出失败')
  } finally {
    backendExporting.value = false
  }
}

// SQL 收藏
const showSaveDialog = ref(false)
const saveForm = ref({ name: '', description: '', sqlContent: '' })
const savingSnippet = ref(false)
const showSnippetDrawer = ref(false)
const snippetList = ref<any[]>([])
const snippetKeyword = ref('')
const loadingSnippets = ref(false)

const openSaveDialog = () => {
  saveForm.value = { name: '', description: '', sqlContent: sqlContent.value }
  showSaveDialog.value = true
}

const handleSaveSnippet = async () => {
  if (!saveForm.value.name.trim()) { message.warning('请输入名称'); return false }
  savingSnippet.value = true
  try {
    await dbManagerApi.saveSnippet({
      name: saveForm.value.name,
      sqlContent: saveForm.value.sqlContent,
      description: saveForm.value.description || undefined,
    })
    message.success('保存成功')
    showSaveDialog.value = false
  } catch (error: any) {
    message.error(error.message || '保存失败')
    return false
  } finally {
    savingSnippet.value = false
  }
}

const openSnippetDrawer = () => {
  showSnippetDrawer.value = true
  loadSnippets()
}

const loadSnippets = async () => {
  loadingSnippets.value = true
  try {
    const res = await dbManagerApi.listSnippets({ keyword: snippetKeyword.value || undefined })
    snippetList.value = (res.data as any)?.list || []
  } catch {
    message.error('加载失败')
  } finally {
    loadingSnippets.value = false
  }
}

const loadSnippetToEditor = (item: any) => {
  sqlContent.value = item.sqlContent
  showSnippetDrawer.value = false
  message.success(`已加载: ${item.name}`)
}

const handleDeleteSnippet = async (id: number) => {
  try {
    await dbManagerApi.deleteSnippet(id)
    message.success('已删除')
    loadSnippets()
  } catch {
    message.error('删除失败')
  }
}

/** 公开方法：从外部设置 SQL 内容 */
const setSqlContent = (sql: string) => {
  sqlContent.value = sql
}

defineExpose({ setSqlContent })
</script>

<style scoped>
.tab-content { flex: 1; padding: 20px; overflow: auto; }

/* 工具栏 */
.toolbar { display: flex; gap: var(--dp-spacing-sm); margin-bottom: 15px; padding: 15px; background: var(--bg-secondary); border-radius: var(--dp-radius-md); align-items: center; flex-wrap: wrap; }
.toolbar-hint { color: var(--text-secondary); font-size: var(--dp-font-sm); }
.toolbar-divider { border-left: 1px solid var(--border-light); height: 20px; margin: 0 5px; }

/* SQL结果 */
.sql-result { margin-top: 20px; }

/* SQL收藏抽屉 */
.snippet-list { display: flex; flex-direction: column; gap: var(--dp-spacing-md); }
.snippet-item { padding: var(--dp-spacing-md); background: var(--bg-secondary); border-radius: var(--dp-radius-md); border: 1px solid var(--border-light); transition: border-color 0.15s; }
.snippet-item:hover { border-color: var(--color-primary); }
.snippet-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.snippet-name { font-weight: 600; color: var(--text-primary); font-size: var(--dp-font-md); }
.snippet-desc { color: var(--text-tertiary); font-size: var(--dp-font-xs); margin-bottom: 6px; }
.snippet-sql { margin: 0; padding: var(--dp-spacing-sm); background: var(--bg-primary); border-radius: var(--dp-radius-sm); font-family: 'Consolas', monospace; font-size: var(--dp-font-xs); color: var(--text-secondary); max-height: 80px; overflow: auto; white-space: pre-wrap; word-break: break-all; border: 1px solid var(--border-light); }
.snippet-time { color: var(--text-tertiary); font-size: 11px; margin-top: 6px; }


/* SQL预览块 */
.sql-preview-block { background: var(--bg-secondary); border-radius: 6px; padding: 10px; max-height: 150px; overflow: auto; font-family: 'Consolas', monospace; font-size: 12px; color: var(--text-secondary); white-space: pre-wrap; border: 1px solid var(--border-light); }

</style>

<style>
/* DbSqlEditor 深色模式（非 scoped） */
html.dark .editor-toolbar { background: #1a2536 !important; border-color: #334155 !important; }
html.dark .toolbar-label { color: #94a3b8 !important; }
html.dark .history-item { background: #1a2536 !important; }
html.dark .history-item:hover { background: #243044 !important; }
html.dark .history-sql { color: #e2e8f0 !important; }
html.dark .history-time { color: #64748b !important; }
html.dark .history-empty { color: #64748b !important; }
html.dark .toolbar { background: #1a2536 !important; }
html.dark .snippet-item { background: #1a2536 !important; border-color: #334155 !important; }
html.dark .snippet-sql { background: #0f172a !important; border-color: #334155 !important; }
html.dark .sql-preview-block { background: #0f172a !important; border-color: #334155 !important; }
</style>
