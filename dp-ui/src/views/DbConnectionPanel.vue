<template>
  <div class="sidebar" :class="{ collapsed: sidebarCollapsed }">
    <n-button class="sidebar-toggle" circle size="small" @click="sidebarCollapsed = !sidebarCollapsed">
      {{ sidebarCollapsed ? '›' : '‹' }}
    </n-button>

    <!-- 连接面板 -->
    <div v-if="!sidebarCollapsed" class="connection-panel">
      <div class="connection-title">
        <n-icon :component="FlashOutline" :color="'var(--color-primary, #2563eb)'" />
        <span>数据库连接</span>
      </div>

      <!-- 系统库连接区域 -->
      <div v-if="!sessionId" class="system-db-panel">
        <div class="system-db-box">
          <div class="system-db-title">🏠 系统库（推荐）</div>
          <div class="system-db-desc">直接连接程序配置的数据库，无需输入任何信息</div>
          <n-button type="success" block :loading="connecting" @click="$emit('connectSystem')">⚡ 一键连接</n-button>
        </div>
      </div>

      <!-- 已连接状态显示 -->
      <div v-if="sessionId" class="connected-panel">
        <div class="connected-title">✅ 已连接</div>
        <div class="connected-info">
          {{ isSystemDb ? '🏠 系统库' : `📡 ${connectForm.dbType}` }}
          <br v-if="!isSystemDb" />
          <span v-if="!isSystemDb" class="connected-detail">{{ connectForm.host }}:{{ connectForm.port }}/{{ connectForm.dbName }}</span>
        </div>
        <n-button type="error" block @click="$emit('disconnect')">✕ 断开连接</n-button>
      </div>

      <!-- 手动连接区域 -->
      <div v-if="!sessionId" class="manual-connect-panel">
        <div class="toggle-manual" @click="$emit('update:showManualConnect', !showManualConnect)">
          ─────── {{ showManualConnect ? '▲' : '▼' }} 手动连接其他数据库 ───────
        </div>
        <div v-if="showManualConnect" class="manual-connect-form">
          <div class="form-grid">
            <div class="form-item">
              <label>📦 数据库类型</label>
              <n-select :value="connectForm.dbType" :options="dbTypeOptions" placeholder="请选择" @update:value="updateField('dbType', $event)" />
            </div>
            <div class="form-item">
              <label># 端口</label>
              <n-input :value="connectForm.port" placeholder="3306" @update:value="updateField('port', $event)" />
            </div>
            <div class="form-item full-width">
              <label>🖥️ 主机地址</label>
              <n-input :value="connectForm.host" placeholder="localhost" @update:value="updateField('host', $event)" />
            </div>
            <div class="form-item full-width">
              <label>📁 数据库名</label>
              <n-input :value="connectForm.dbName" placeholder="数据库名" @update:value="updateField('dbName', $event)" />
            </div>
            <div class="form-item">
              <label>👤 用户名</label>
              <n-input :value="connectForm.username" placeholder="用户名" @update:value="updateField('username', $event)" />
            </div>
            <div class="form-item">
              <label>🔒 密码</label>
              <n-input :value="connectForm.password" type="password" placeholder="密码" @update:value="updateField('password', $event)" />
            </div>
          </div>
          <n-button type="primary" block :loading="connecting" @click="$emit('connect')">🔗 连接</n-button>
        </div>
      </div>
    </div>

    <!-- 对象树面板 -->
    <div v-if="!sidebarCollapsed" class="objects-panel">
      <div class="search-box">
        <n-input v-model:value="objectSearch" placeholder="搜索数据库对象..." clearable>
          <template #suffix><n-icon :component="SearchOutline" /></template>
        </n-input>
      </div>

      <div v-if="!sessionId" class="empty-objects">
        <div class="empty-icon">🗄️</div>
        <p>请先连接数据库</p>
      </div>

      <div v-else id="dbObjects">
        <!-- 表 -->
        <div v-if="filteredTables.length > 0" class="object-group">
          <div class="group-header" @click="tablesExpanded = !tablesExpanded">
            <span>📊 表 ({{ filteredTables.length }})</span>
            <span class="group-icon">{{ tablesExpanded ? '▼' : '▶' }}</span>
          </div>
          <div v-show="tablesExpanded" class="group-content">
            <div
              v-for="item in filteredTables"
              :key="item.tableName"
              class="tree-item"
              :class="{ active: currentTable === item.tableName }"
              @click="$emit('selectTable', item.tableName)"
            >
              <span class="tree-item-icon">📋</span>
              <span class="object-name">{{ item.tableName }}</span>
              <span v-if="item.remarks" class="object-comment">({{ item.remarks }})</span>
            </div>
          </div>
        </div>

        <!-- 视图 -->
        <div v-if="filteredViews.length > 0" class="object-group">
          <div class="group-header" @click="viewsExpanded = !viewsExpanded">
            <span>👁️ 视图 ({{ filteredViews.length }})</span>
            <span class="group-icon">{{ viewsExpanded ? '▼' : '▶' }}</span>
          </div>
          <div v-show="viewsExpanded" class="group-content">
            <div
              v-for="item in filteredViews"
              :key="item.viewName"
              class="tree-item"
              :class="{ active: currentView === item.viewName }"
              @click="$emit('selectView', item.viewName)"
            >
              <span class="tree-item-icon">👀</span>
              <span class="object-name">{{ item.viewName }}</span>
            </div>
          </div>
        </div>

        <!-- 存储过程 -->
        <div v-if="filteredProcedures.length > 0" class="object-group">
          <div class="group-header" @click="proceduresExpanded = !proceduresExpanded">
            <span>⚙️ 存储过程 ({{ filteredProcedures.length }})</span>
            <span class="group-icon">{{ proceduresExpanded ? '▼' : '▶' }}</span>
          </div>
          <div v-show="proceduresExpanded" class="group-content">
            <div
              v-for="item in filteredProcedures"
              :key="item.procedureName"
              class="tree-item"
              :class="{ active: currentProcedure === item.procedureName }"
              @click="$emit('selectProcedure', item.procedureName)"
            >
              <span class="tree-item-icon">{{ item.procedureType === 1 ? '🔧' : '⚡' }}</span>
              <span class="object-name">{{ item.procedureName }}</span>
              <span class="object-type">[{{ item.procedureType === 1 ? '存储过程' : '函数' }}]</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed } from 'vue'
import { FlashOutline, SearchOutline } from '@vicons/ionicons5'
import { DB_TYPE_OPTIONS, type ConnectFormData } from '@/composables/useDbConnection'

const props = defineProps<{
  sessionId: string | null
  connecting: boolean
  isSystemDb: boolean
  showManualConnect: boolean
  connectForm: ConnectFormData
  tables: any[]
  views: any[]
  procedures: any[]
  currentTable: string | null
  currentView: string | null
  currentProcedure: string | null
}>()

const emit = defineEmits<{
  connectSystem: []
  connect: []
  disconnect: []
  selectTable: [tableName: string]
  selectView: [viewName: string]
  selectProcedure: [procedureName: string]
  'update:showManualConnect': [value: boolean]
  'update:connectForm': [value: ConnectFormData]
}>()

const updateField = (field: keyof ConnectFormData, value: string) => {
  emit('update:connectForm', { ...props.connectForm, [field]: value })
}

const dbTypeOptions = DB_TYPE_OPTIONS
const sidebarCollapsed = ref(false)
const objectSearch = ref('')
const tablesExpanded = ref(true)
const viewsExpanded = ref(true)
const proceduresExpanded = ref(true)

const filteredTables = computed(() => {
  if (!objectSearch.value) return props.tables
  const kw = objectSearch.value.toLowerCase()
  return props.tables.filter((t) => t.tableName.toLowerCase().includes(kw))
})

const filteredViews = computed(() => {
  if (!objectSearch.value) return props.views
  const kw = objectSearch.value.toLowerCase()
  return props.views.filter((v) => v.viewName.toLowerCase().includes(kw))
})

const filteredProcedures = computed(() => {
  if (!objectSearch.value) return props.procedures
  const kw = objectSearch.value.toLowerCase()
  return props.procedures.filter((p) => p.procedureName.toLowerCase().includes(kw))
})
</script>

<style scoped>
/* 侧边栏 */
.sidebar { width: 320px; min-width: 320px; background: var(--bg-primary); border-right: 1px solid var(--border-light); display: flex; flex-direction: column; position: relative; transition: all 0.3s; }
.sidebar.collapsed { width: 60px; min-width: 60px; }
.sidebar-toggle { position: absolute; right: -15px; top: 20px; z-index: 10; }

.connection-panel { padding: 20px; border-bottom: 1px solid var(--border-light); background: linear-gradient(135deg, #f8f9ff 0%, #e8f0ff 100%); }
.connection-title { font-size: var(--dp-font-lg); font-weight: 600; color: var(--text-primary); margin-bottom: 15px; display: flex; align-items: center; gap: var(--dp-spacing-sm); }

.system-db-box { background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%); padding: 15px; border-radius: var(--dp-radius-md); margin-bottom: 15px; }
.system-db-title { font-weight: 600; color: #2e7d32; margin-bottom: var(--dp-spacing-sm); }
.system-db-desc { font-size: var(--dp-font-xs); color: #666; margin-bottom: var(--dp-spacing-md); }

.connected-panel { background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); padding: 15px; border-radius: var(--dp-radius-md); margin-bottom: 15px; }
.connected-title { font-weight: 600; color: #1565c0; margin-bottom: var(--dp-spacing-sm); }
.connected-info { font-size: var(--dp-font-sm); color: var(--text-secondary); margin-bottom: var(--dp-spacing-md); }
.connected-detail { color: #888; font-size: var(--dp-font-xs); }

.toggle-manual { text-align: center; color: var(--text-tertiary); margin: var(--dp-spacing-sm) 0; font-size: var(--dp-font-xs); cursor: pointer; }
.manual-connect-form { margin-top: 15px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--dp-spacing-sm); margin-bottom: 15px; }
.form-item { margin-bottom: var(--dp-spacing-md); }
.form-item.full-width { grid-column: 1 / -1; }
.form-item label { display: block; margin-bottom: 5px; color: #555; font-size: var(--dp-font-sm); font-weight: 500; }

/* 对象树 */
.objects-panel { flex: 1; padding: 15px; overflow-y: auto; }
.search-box { margin-bottom: 15px; }
.empty-objects { text-align: center; color: var(--text-tertiary); padding: 40px 20px; }
.empty-objects .empty-icon { font-size: 36px; margin-bottom: 15px; }

.object-group { margin-bottom: 15px; }
.group-header { display: flex; align-items: center; justify-content: space-between; padding: var(--dp-spacing-sm) var(--dp-spacing-md); background: linear-gradient(135deg, #f8f9ff 0%, #e8f0ff 100%); border-radius: var(--dp-radius-sm); cursor: pointer; font-weight: 600; color: #333; }
.group-header:hover { background: linear-gradient(135deg, #e8f0ff 0%, #d8e8ff 100%); }
.group-content { max-height: 400px; overflow-y: auto; }

.tree-item { padding: var(--dp-spacing-sm) 15px; cursor: pointer; border-radius: var(--dp-radius-sm); margin: 2px 0; color: #555; display: flex; align-items: center; flex-wrap: wrap; }
.tree-item:hover { background: #f0f2f5; color: #333; }
.tree-item.active { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); color: white; }
.tree-item-icon { margin-right: var(--dp-spacing-sm); }
.object-name { font-weight: 500; }
.object-type { font-weight: 600; color: #1890ff; font-size: 11px; margin-left: 5px; }
.object-comment { color: #999; font-size: var(--dp-font-xs); margin-left: 5px; font-style: italic; }
.tree-item.active .object-comment, .tree-item.active .object-type { color: rgba(255,255,255,0.8); }

</style>

<style>
/* DbConnectionPanel 深色模式（非 scoped） */
html.dark .sidebar-title { color: #e2e8f0 !important; }
html.dark .conn-item { background: #1a2536 !important; }
html.dark .conn-item:hover { background: #243044 !important; }
html.dark .conn-name { color: #e2e8f0 !important; }
html.dark .conn-info { color: #64748b !important; }
html.dark .conn-url { color: #94a3b8 !important; }
html.dark .conn-status { color: #64748b !important; }
html.dark .group-title { color: #e2e8f0 !important; }
html.dark .group-count { color: #64748b !important; }
html.dark .tree-item { color: #94a3b8 !important; }
html.dark .tree-item:hover { background: #243044 !important; }
html.dark .no-data { color: #64748b !important; }
html.dark .form-label { color: #94a3b8 !important; }
</style>
