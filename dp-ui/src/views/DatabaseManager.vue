<template>
  <div class="db-manager-wrapper">
    <!-- 登录页面 -->
    <div v-if="!isAuthenticated" class="login-container">
      <div class="login-box">
        <div class="login-title">
          <n-icon :component="LockClosedOutline" size="24" :color="'var(--color-primary, #2563eb)'" />
          {{ t('dbManagerView.title') }}
        </div>
        <div class="login-form">
          <div class="form-item">
            <label>🔑 {{ t('dbManagerView.accessPassword') }}</label>
            <n-input v-model:value="accessPassword" type="password" :placeholder="t('dbManagerView.passwordPlaceholder')" @keyup.enter="handleLogin" />
          </div>
          <n-button type="primary" block :loading="loginLoading" @click="handleLogin">→ {{ t('dbManagerView.enterSystem') }}</n-button>
        </div>
      </div>
    </div>

    <!-- 主应用界面 -->
    <div v-else class="main-app">
    <div class="app-header">
      <div class="app-title">⚙️ {{ t('dbManagerView.title') }}</div>
      <div class="app-actions">
        <n-tag :type="sessionId ? 'success' : 'error'" round>
          {{ sessionId ? '● ' + t('dbManagerView.connected') + (isSystemDb ? ' - ' + t('dbManagerView.systemDb') : '') : '● ' + t('dbManagerView.disconnected') }}
        </n-tag>
        <n-button type="error" size="small" @click="handleLogout">↩ {{ t('dbManagerView.logout') }}</n-button>
      </div>
    </div>

    <div class="app-body">
      <!-- 侧边栏：连接面板 + 对象树 -->
      <DbConnectionPanel
        :session-id="sessionId"
        :connecting="connecting"
        :is-system-db="isSystemDb"
        :show-manual-connect="showManualConnect"
        :connect-form="connectForm"
        :tables="tables"
        :views="views"
        :procedures="procedures"
        :current-table="currentTable"
        :current-view="currentView"
        :current-procedure="currentProcedure"
        @connect-system="connectSystemDb"
        @connect="handleConnect"
        @disconnect="handleFullDisconnect"
        @select-table="selectTable"
        @select-view="selectView"
        @select-procedure="selectProcedure"
        @update:show-manual-connect="showManualConnect = $event"
        @update:connect-form="Object.assign(connectForm, $event)"
      />

      <!-- 主内容区 -->
      <div class="main-content">
        <div class="content-header">
          <div class="content-title">📊 {{ t('dbManagerView.dbManagement') }}</div>
          <div class="content-actions">
            <n-button type="primary" size="small" :disabled="!sessionId" :loading="loadingObjects" @click="refreshObjects">🔄 {{ t('dbManagerView.refresh') }}</n-button>
          </div>
        </div>

        <div class="content-body">
          <!-- 标签页 -->
          <div class="custom-tabs">
            <div class="tab-item" :class="{ active: activeTab === 'query', disabled: !sessionId }" @click="sessionId && (activeTab = 'query')">📋 {{ t('dbManagerView.dataQuery') }}</div>
            <div class="tab-item" :class="{ active: activeTab === 'sql', disabled: !sessionId }" @click="sessionId && (activeTab = 'sql')">⚡ {{ t('dbManagerView.sqlExecutor') }}</div>
            <div class="tab-item" :class="{ active: activeTab === 'structure', disabled: !sessionId }" @click="sessionId && (activeTab = 'structure')">🏗️ {{ t('dbManagerView.tableStructure') }}</div>
            <div class="tab-item" :class="{ active: activeTab === 'view', disabled: !sessionId }" @click="sessionId && (activeTab = 'view')">👁️ {{ t('dbManagerView.view') }}</div>
            <div class="tab-item" :class="{ active: activeTab === 'procedure', disabled: !sessionId }" @click="sessionId && (activeTab = 'procedure')">⚙️ {{ t('dbManagerView.storedProcedure') }}</div>
          </div>

          <!-- 未连接时显示提示 -->
          <div v-if="!sessionId" class="disconnected-placeholder">
            <div class="disconnected-icon">🔌</div>
            <div class="disconnected-text">{{ t('dbManagerView.connectFirst') }}</div>
          </div>

          <!-- 数据查询标签页 -->
          <DbQueryResult
            v-if="sessionId"
            v-show="activeTab === 'query'"
            ref="queryResultRef"
            :session-id="sessionId"
            :current-table="currentTable"
          />

          <!-- SQL执行器标签页 -->
          <DbSqlEditor
            v-if="sessionId"
            v-show="activeTab === 'sql'"
            ref="sqlEditorRef"
            :session-id="sessionId"
            :editor-table-schema="editorTableSchema"
            @refresh-objects="refreshObjects"
          />

          <!-- 表结构 / 视图 / 存储过程 / SQL历史 标签页 -->
          <DbTableBrowser
            v-if="sessionId"
            v-show="activeTab === 'structure' || activeTab === 'view' || activeTab === 'procedure' || activeTab === 'history'"
            ref="tableBrowserRef"
            :session-id="sessionId"
            :active-tab="activeTab"
            :current-table="currentTable"
            :current-view="currentView"
            :current-procedure="currentProcedure"
            @query-view-data="queryViewData"
            @use-sql="handleUseSql"
          />
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed } from 'vue'
import { LockClosedOutline } from '@vicons/ionicons5'
import { useDbConnection } from '@/composables/useDbConnection'
import { useI18n } from '@/i18n'
import DbConnectionPanel from './DbConnectionPanel.vue'
import DbSqlEditor from './DbSqlEditor.vue'
import DbQueryResult from './DbQueryResult.vue'
import DbTableBrowser from './DbTableBrowser.vue'

const { t } = useI18n()

// ---- 组合式函数：连接状态管理 ----
const {
  isAuthenticated,
  accessPassword,
  loginLoading,
  handleLogin,
  handleLogout,
  sessionId,
  connecting,
  isSystemDb,
  showManualConnect,
  connectForm,
  connectSystemDb,
  handleConnect,
  handleDisconnect,
  tables,
  views,
  procedures,
  loadingObjects,
  tableColumnsCache,
  refreshObjects,
} = useDbConnection()

// ---- 子组件引用 ----
const queryResultRef = ref<InstanceType<typeof DbQueryResult> | null>(null)
const sqlEditorRef = ref<InstanceType<typeof DbSqlEditor> | null>(null)
const tableBrowserRef = ref<InstanceType<typeof DbTableBrowser> | null>(null)

// ---- 标签页 ----
const activeTab = ref('query')

// ---- 当前选中对象 ----
const currentTable = ref<string | null>(null)
const currentView = ref<string | null>(null)
const currentProcedure = ref<string | null>(null)

// ---- CodeMirror 表补全 schema ----
const editorTableSchema = computed(() => {
  const schema: Record<string, string[]> = {}
  for (const t of tables.value) {
    const cols = tableColumnsCache.value[t.tableName]
    schema[t.tableName] = cols ? cols.map((c: any) => c.name) : []
  }
  return schema
})

// ---- 断开连接（清除本地选中状态） ----
const handleFullDisconnect = () => {
  handleDisconnect()
  currentTable.value = null
  currentView.value = null
  currentProcedure.value = null
  tableBrowserRef.value?.clearDefinitions()
}

// ---- 选择表 ----
const selectTable = async (tableName: string) => {
  currentTable.value = tableName
  currentView.value = null
  currentProcedure.value = null
  tableBrowserRef.value?.clearDefinitions()
  activeTab.value = 'query'
}

// ---- 选择视图 ----
const selectView = async (viewName: string) => {
  currentView.value = viewName
  currentTable.value = null
  currentProcedure.value = null
  activeTab.value = 'view'
  tableBrowserRef.value?.loadViewDefinition(viewName)
}

// ---- 选择存储过程 ----
const selectProcedure = async (procedureName: string) => {
  currentProcedure.value = procedureName
  currentTable.value = null
  currentView.value = null
  activeTab.value = 'procedure'
  tableBrowserRef.value?.loadProcedureDefinition(procedureName)
}

// ---- 查询视图数据 ----
const queryViewData = () => {
  if (!currentView.value) return
  currentTable.value = currentView.value
  activeTab.value = 'query'
}

// ---- 从历史加载 SQL 到编辑器 ----
const handleUseSql = (sql: string) => {
  sqlEditorRef.value?.setSqlContent(sql)
  activeTab.value = 'sql'
}
</script>

<style scoped>
/* 外层包裹 */
.db-manager-wrapper { min-height: calc(100vh - 60px - 42px - 36px - 32px); display: flex; flex-direction: column; }

/* 登录页面 */
.login-container { display: flex; justify-content: center; align-items: center; flex: 1; background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); }
.login-box { background: rgba(255,255,255,0.95); padding: var(--dp-spacing-xl) 40px; border-radius: var(--dp-radius-lg); box-shadow: var(--dp-shadow-lg); width: 400px; }
.login-title { text-align: center; font-size: var(--dp-font-xl); font-weight: 600; color: #333; margin-bottom: 30px; display: flex; align-items: center; justify-content: center; gap: var(--dp-spacing-sm); }
.login-form .form-item { margin-bottom: 20px; }
.login-form label { display: block; margin-bottom: var(--dp-spacing-sm); color: #555; font-weight: 500; }

/* 主应用 */
.main-app { flex: 1; display: flex; flex-direction: column; background: #f5f7fa; overflow: hidden; }
.app-header { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; }
.app-title { font-size: 20px; font-weight: 600; }
.app-actions { display: flex; gap: var(--dp-spacing-sm); align-items: center; }
.app-body { display: flex; flex: 1; overflow: hidden; }

/* 主内容区 */
.main-content { flex: 1; display: flex; flex-direction: column; background: white; overflow: hidden; }
.content-header { padding: 15px 20px; border-bottom: 1px solid #e6e6e6; display: flex; justify-content: space-between; align-items: center; }
.content-title { font-size: var(--dp-font-xl); font-weight: 600; color: #333; }
.content-body { flex: 1; overflow: hidden; display: flex; flex-direction: column; }

/* 标签页 */
.custom-tabs { display: flex; border-bottom: 1px solid #e6e6e6; background: #fafbfc; flex-shrink: 0; }
.tab-item { padding: var(--dp-spacing-md) 20px; cursor: pointer; border-bottom: 3px solid transparent; color: #666; font-weight: 500; }
.tab-item:hover { color: #333; background: #f0f2f5; }
.tab-item.active { color: var(--color-primary, #2563eb); border-bottom-color: var(--color-primary, #2563eb); background: white; }
.tab-item.disabled { color: #bbb; cursor: not-allowed; opacity: 0.6; }
.tab-item.disabled:hover { color: #bbb; background: #fafbfc; }

/* 未连接提示 */
.disconnected-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; }
.disconnected-icon { font-size: 48px; margin-bottom: 16px; }
.disconnected-text { font-size: 16px; }

</style>

<style>
/* DatabaseManager 深色模式（非 scoped） */
html.dark .login-box { background: rgba(30, 41, 59, 0.95) !important; }
html.dark .login-title { color: #e2e8f0 !important; }
html.dark .login-form label { color: #94a3b8 !important; }
html.dark .main-app { background: #0f172a !important; }
html.dark .main-content { background: #1e293b !important; }
html.dark .content-header { border-bottom-color: #334155 !important; }
html.dark .content-title { color: #e2e8f0 !important; }
html.dark .custom-tabs { background: #1a2536 !important; border-bottom-color: #334155 !important; }
html.dark .tab-item { color: #94a3b8 !important; }
html.dark .tab-item:hover { color: #e2e8f0 !important; background: #243044 !important; }
html.dark .tab-item.active { color: #818cf8 !important; background: #1e293b !important; }
html.dark .tab-item.disabled { color: #475569 !important; }
html.dark .tab-item.disabled:hover { color: #475569 !important; background: #1a2536 !important; }
html.dark .disconnected-placeholder { color: #64748b !important; }
</style>
