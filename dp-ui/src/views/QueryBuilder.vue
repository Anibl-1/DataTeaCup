<template>
  <div class="query-builder">
    <!-- 统计卡片 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary"><n-icon size="24"><CodeWorkingOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ animatedTableCount }}</span><span class="stat-label">{{ t('queryBuilder.tables') }}</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success"><n-icon size="24"><LayersOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ animatedFieldCount }}</span><span class="stat-label">{{ t('queryBuilder.queryFields') }}</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning"><n-icon size="24"><GitMergeOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ animatedJoinCount }}</span><span class="stat-label">{{ t('queryBuilder.joinTables') }}</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info"><n-icon size="24"><FilterOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ animatedConditionCount }}</span><span class="stat-label">{{ t('queryBuilder.filterConditions') }}</span></div>
      </div>
    </div>

    <!-- 顶部工具栏 -->
    <n-card size="small" style="margin-bottom: 12px;">
      <div class="qb-toolbar">
        <div class="toolbar-left">
          <n-select
            v-model:value="dataSourceId"
            :options="dataSourceOptions"
            :placeholder="t('queryBuilder.selectDataSource')"
            style="width: 240px"
            :loading="loadingDataSources"
            @update:value="loadTables"
          />
          <n-checkbox v-model:checked="model.distinct" class="toolbar-check">DISTINCT</n-checkbox>
        </div>
        <div class="toolbar-right">
          <n-button size="small" quaternary @click="showSavedQueries = true">
            <template #icon><n-icon size="16"><BookmarkOutline /></n-icon></template>
            {{ t('queryBuilder.saved') }}
            <n-badge v-if="savedQueries.length > 0" :value="savedQueries.length" :max="9" style="margin-left:4px;" />
          </n-button>
          <n-button size="small" quaternary @click="saveCurrentQuery">
            <template #icon><n-icon size="16"><SaveOutline /></n-icon></template>
            {{ t('queryBuilder.saveQuery') }}
          </n-button>
          <n-button size="small" secondary @click="router.push('/rls-config')">
            <template #icon><n-icon size="14"><LockClosedOutline /></n-icon></template>
            RLS
            <n-badge v-if="rlsRuleCount > 0" :value="rlsRuleCount" type="warning" style="margin-left:4px;" />
          </n-button>
        </div>
      </div>
    </n-card>

    <div class="qb-content">
      <!-- 左侧：表列表 -->
      <div class="tables-panel">
        <n-card size="small" style="height: 100%;">
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="#10b981" class="header-icon"><ListOutline /></n-icon>
              <span>{{ t('queryBuilder.tables') }} ({{ tableNames.length }})</span>
            </div>
          </template>
          <template #header-extra>
            <n-input v-model:value="tableSearch" :placeholder="t('queryBuilder.search')" size="tiny" clearable style="width:100px;" />
          </template>
          <n-spin :show="loadingTables">
            <n-empty v-if="!dataSourceId" :description="t('queryBuilder.selectDataSourceFirst')" style="padding:20px 0;" />
            <n-tree
              v-else
              :data="filteredTableTreeData"
              block-line
              expand-on-click
              selectable
              :on-load="handleTreeLoad"
              @update:selected-keys="onTableSelect"
            />
          </n-spin>
        </n-card>
      </div>

      <!-- 中间：查询配置 -->
      <div class="config-panel">
        <!-- SELECT 字段 -->
        <n-card size="small" class="config-section">
          <template #header>
            <n-space align="center" justify="space-between" style="width: 100%">
              <div class="card-header-custom">
                <n-icon size="18" color="#3b82f6"><CheckboxOutline /></n-icon>
                <span>{{ t('queryBuilder.selectFields') }}</span>
                <n-tag v-if="model.selectFields.length > 0" size="tiny" round>{{ model.selectFields.length }}</n-tag>
              </div>
              <n-space :size="4">
                <n-button size="tiny" quaternary @click="addAllFields">{{ t('queryBuilder.addAll') }}</n-button>
                <n-button size="tiny" type="primary" @click="addSelectField">{{ t('queryBuilder.add') }}</n-button>
              </n-space>
            </n-space>
          </template>
          <div v-if="model.selectFields.length === 0" class="empty-hint">{{ t('queryBuilder.selectFieldsHint') }}</div>
          <div v-for="(field, index) in model.selectFields" :key="index" class="field-row">
            <n-select v-model:value="field.table" :options="tableOptions" :placeholder="t('queryBuilder.tablePlaceholder')" style="width: 120px" size="small" />
            <n-select v-model:value="field.field" :options="getFieldOptions(field.table)" :placeholder="t('queryBuilder.fieldPlaceholder')" style="width: 120px" size="small" />
            <n-select v-model:value="field.aggregate" :options="aggregateOptions" :placeholder="t('queryBuilder.aggregatePlaceholder')" style="width: 100px" size="small" clearable />
            <n-input v-model:value="field.alias" :placeholder="t('queryBuilder.aliasPlaceholder')" style="width: 100px" size="small" />
            <n-button size="tiny" quaternary type="error" @click="removeSelectField(index)">
              <template #icon><n-icon><CloseOutline /></n-icon></template>
            </n-button>
          </div>
        </n-card>

        <!-- FROM 表 -->
        <n-card size="small" class="config-section">
          <template #header>
            <div class="card-header-custom">
              <n-icon size="18" color="#f59e0b"><GridOutline /></n-icon>
              <span>{{ t('queryBuilder.fromTable') }}</span>
            </div>
          </template>
          <n-select
            v-model:value="mainTable"
            :options="tableOptions"
            :placeholder="t('queryBuilder.selectMainTable')"
            @update:value="onMainTableChange"
          />
        </n-card>

        <!-- JOIN -->
        <n-card size="small" class="config-section">
          <template #header>
            <n-space align="center" justify="space-between" style="width: 100%">
              <div class="card-header-custom">
                <n-icon size="18" color="#8b5cf6"><GitMergeOutline /></n-icon>
                <span>JOIN</span>
                <n-tag v-if="model.joins.length > 0" size="tiny" round>{{ model.joins.length }}</n-tag>
              </div>
              <n-button size="tiny" type="primary" @click="addJoin">{{ t('queryBuilder.add') }}</n-button>
            </n-space>
          </template>
          <div v-if="model.joins.length === 0" class="empty-hint">{{ t('queryBuilder.noJoins') }}</div>
          <div v-for="(join, index) in model.joins" :key="index" class="join-row">
            <n-select v-model:value="join.type" :options="joinTypeOptions" style="width: 100px" size="small" />
            <n-select v-model:value="join.rightTable" :options="tableOptions" :placeholder="t('queryBuilder.tablePlaceholder')" style="width: 120px" size="small" />
            <span class="sql-keyword">ON</span>
            <n-select v-model:value="join.leftField" :options="getFieldOptions(join.leftTable)" :placeholder="t('queryBuilder.leftFieldPlaceholder')" style="width: 100px" size="small" />
            <span class="sql-keyword">=</span>
            <n-select v-model:value="join.rightField" :options="getFieldOptions(join.rightTable)" :placeholder="t('queryBuilder.rightFieldPlaceholder')" style="width: 100px" size="small" />
            <n-button size="tiny" quaternary type="error" @click="removeJoin(index)">
              <template #icon><n-icon><CloseOutline /></n-icon></template>
            </n-button>
          </div>
        </n-card>

        <!-- WHERE -->
        <n-card size="small" class="config-section">
          <template #header>
            <n-space align="center" justify="space-between" style="width: 100%">
              <div class="card-header-custom">
                <n-icon size="18" color="#ef4444"><FilterOutline /></n-icon>
                <span>{{ t('queryBuilder.whereConditions') }}</span>
                <n-tag v-if="model.conditions.length > 0" size="tiny" round>{{ model.conditions.length }}</n-tag>
              </div>
              <n-button size="tiny" type="primary" @click="addCondition">{{ t('queryBuilder.add') }}</n-button>
            </n-space>
          </template>
          <div v-if="model.conditions.length === 0" class="empty-hint">{{ t('queryBuilder.noConditions') }}</div>
          <div v-for="(cond, index) in model.conditions" :key="index" class="condition-row">
            <n-select v-if="index > 0" v-model:value="cond.logic" :options="logicOptions" style="width: 80px" size="small" />
            <n-select v-model:value="cond.field" :options="allFieldOptions" :placeholder="t('queryBuilder.fieldPlaceholder')" style="width: 160px" size="small" filterable tag />
            <n-select v-model:value="cond.operator" :options="operatorOptions" style="width: 100px" size="small" />
            <n-input v-model:value="cond.value" :placeholder="t('queryBuilder.valuePlaceholder')" style="width: 150px" size="small" />
            <n-button size="tiny" quaternary type="error" @click="removeCondition(index)">
              <template #icon><n-icon><CloseOutline /></n-icon></template>
            </n-button>
          </div>
        </n-card>

        <!-- GROUP BY, HAVING, ORDER BY 折叠区域 -->
        <n-grid :cols="2" :x-gap="12">
          <n-gi>
            <n-card size="small" class="config-section">
              <template #header>
                <div class="card-header-custom">
                  <n-icon size="18" color="#06b6d4"><LayersOutline /></n-icon>
                  <span>GROUP BY</span>
                </div>
              </template>
              <n-select v-model:value="model.groupBy" :options="allFieldOptions" multiple :placeholder="t('queryBuilder.selectGroupBy')" size="small" />
            </n-card>
          </n-gi>
          <n-gi>
            <n-card size="small" class="config-section">
              <template #header>
                <div class="card-header-custom">
                  <n-icon size="18" color="#f97316"><FilterOutline /></n-icon>
                  <span>HAVING</span>
                </div>
              </template>
              <n-input v-model:value="havingClause" :placeholder="t('queryBuilder.havingPlaceholder')" size="small" />
            </n-card>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="12">
          <n-gi>
            <n-card size="small" class="config-section">
              <template #header>
                <n-space align="center" justify="space-between" style="width: 100%">
                  <div class="card-header-custom">
                    <n-icon size="18" color="#06b6d4"><SwapVerticalOutline /></n-icon>
                    <span>ORDER BY</span>
                  </div>
                  <n-button size="tiny" type="primary" @click="addOrderBy">+</n-button>
                </n-space>
              </template>
              <div v-if="model.orderBy.length === 0" class="empty-hint">{{ t('queryBuilder.noOrderBy') }}</div>
              <div v-for="(ob, index) in model.orderBy" :key="index" class="field-row compact">
                <n-select v-model:value="ob.field" :options="allFieldOptions" :placeholder="t('queryBuilder.fieldPlaceholder')" style="flex:1" size="small" filterable />
                <n-select v-model:value="ob.direction" :options="orderDirectionOptions" style="width: 80px" size="small" />
                <n-button size="tiny" quaternary type="error" @click="removeOrderBy(index)">
                  <template #icon><n-icon><CloseOutline /></n-icon></template>
                </n-button>
              </div>
            </n-card>
          </n-gi>
          <n-gi>
            <n-card size="small" class="config-section">
              <template #header>
                <div class="card-header-custom">
                  <n-icon size="18" color="#64748b"><StopwatchOutline /></n-icon>
                  <span>LIMIT</span>
                </div>
              </template>
              <n-input-number v-model:value="model.limit" :min="1" :max="10000" :placeholder="t('queryBuilder.limitPlaceholder')" style="width: 100%" size="small" />
            </n-card>
          </n-gi>
        </n-grid>

        <!-- 操作按钮（固定底部） -->
        <div class="action-bar">
          <n-button type="primary" :loading="previewing" @click="generateAndPreview">
            <template #icon><n-icon><PlayOutline /></n-icon></template>
            {{ t('queryBuilder.generateAndPreview') }}
          </n-button>
          <n-button @click="copySql">
            <template #icon><n-icon><CopyOutline /></n-icon></template>
            {{ t('queryBuilder.copySql') }}
          </n-button>
          <n-button :disabled="!generatedSql || !dataSourceId" @click="createAsChart">
            <template #icon><n-icon><BarChartOutline /></n-icon></template>
            {{ t('queryBuilder.createChart') }}
          </n-button>
          <n-button :disabled="!generatedSql || !dataSourceId" @click="createAsReport">
            <template #icon><n-icon><DocumentTextOutline /></n-icon></template>
            {{ t('queryBuilder.createReport') }}
          </n-button>
          <n-button quaternary @click="resetModel">{{ t('queryBuilder.reset') }}</n-button>
        </div>
      </div>

      <!-- 右侧：SQL 和结果 -->
      <div class="result-panel">
        <n-card size="small" class="sql-card">
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="#6366f1" class="header-icon"><CodeOutline /></n-icon>
              <span>{{ t('queryBuilder.generatedSql') }}</span>
            </div>
          </template>
          <template #header-extra>
            <n-space :size="8" align="center">
              <n-tag v-if="rlsRuleCount > 0" type="warning" size="small" round>
                <template #icon><n-icon size="12"><LockClosedOutline /></n-icon></template>
                {{ t('queryBuilder.rlsRules', { n: rlsRuleCount }) }}
              </n-tag>
            </n-space>
          </template>
          <n-code :code="generatedSql || t('queryBuilder.sqlPlaceholder')" language="sql" word-wrap />
        </n-card>

        <n-card v-if="previewData.length > 0" size="small" class="data-card">
          <template #header>
            <n-space align="center">
              <div class="card-header-custom">
                <n-icon size="20" color="#10b981" class="header-icon"><ReaderOutline /></n-icon>
                <span>{{ t('queryBuilder.queryResult') }}</span>
              </div>
              <n-tag type="info" size="small">{{ t('queryBuilder.rows', { n: previewData.length }) }}</n-tag>
            </n-space>
          </template>
          <template #header-extra>
            <n-button size="tiny" quaternary @click="exportCsv">
              <template #icon><n-icon size="14"><DownloadOutline /></n-icon></template>
              {{ t('queryBuilder.export') }}
            </n-button>
          </template>
          <n-data-table
            :columns="previewColumns"
            :data="previewData"
            :max-height="400"
            :scroll-x="800"
            size="small"
            bordered
            striped
            class="custom-table"
          />
        </n-card>

        <n-empty v-else-if="!previewing && generatedSql" :description="t('queryBuilder.previewHint')" style="padding:40px 0;" />
      </div>
    </div>

    <!-- 保存查询弹窗 -->
    <n-modal v-model:show="showSaveModal" preset="card" :title="t('queryBuilder.saveQueryTitle')" style="width:420px;border-radius:16px;">
      <n-form label-placement="left" label-width="80px">
        <n-form-item :label="t('queryBuilder.queryName')">
          <n-input v-model:value="saveQueryName" :placeholder="t('queryBuilder.queryNamePlaceholder')" />
        </n-form-item>
        <n-form-item>
          <n-space style="width:100%;justify-content:flex-end;">
            <n-button @click="showSaveModal = false">{{ t('queryBuilder.cancel') }}</n-button>
            <n-button type="primary" @click="confirmSaveQuery">{{ t('queryBuilder.save') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 已保存查询抽屉 -->
    <n-drawer v-model:show="showSavedQueries" :width="360" placement="right">
      <n-drawer-content :title="t('queryBuilder.savedQueries')">
        <n-empty v-if="savedQueries.length === 0" :description="t('queryBuilder.noSavedQueries')" />
        <n-list v-else hoverable clickable>
          <n-list-item v-for="(sq, idx) in savedQueries" :key="idx" @click="loadSavedQuery(sq)">
            <n-thing :title="sq.name" :description="sq.sql?.substring(0, 80) + '...'">
              <template #header-extra>
                <n-button text size="tiny" type="error" @click.stop="removeSavedQuery(idx)">{{ t('queryBuilder.delete') }}</n-button>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { CodeWorkingOutline, CloseOutline, PlayOutline, CopyOutline, LayersOutline, GitMergeOutline, FilterOutline, ListOutline, CheckboxOutline, GridOutline, StopwatchOutline, CodeOutline, ReaderOutline, SwapVerticalOutline, LockClosedOutline, BookmarkOutline, SaveOutline, DownloadOutline, BarChartOutline, DocumentTextOutline } from '@vicons/ionicons5'
import { useCountAnimation } from '@/utils/countAnimation'
import type { DataTableColumns, TreeOption } from 'naive-ui'
import type { QueryModel, TableMeta, SelectField, JoinConfig, WhereCondition } from '@/types/queryBuilder'
import { generateSql, previewQuery, getTableNames, getColumnMeta } from '@/api/queryBuilder'
import { getDataSourceList } from '@/api/dataSource'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const router = useRouter()
const message = useMessage()

const rlsRuleCount = ref(0)
const dataSourceId = ref<number | null>(null)
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const loadingDataSources = ref(false)
const loadingTables = ref(false)
const previewing = ref(false)

const tables = ref<TableMeta[]>([])
const tableNames = ref<string[]>([])
const mainTable = ref<string>('')
const generatedSql = ref('')
const previewData = ref<Record<string, any>[]>([])
const previewColumns = ref<DataTableColumns<Record<string, any>>>([])

const model = ref<QueryModel & { distinct?: boolean }>({
  tables: [],
  joins: [],
  selectFields: [],
  conditions: [],
  groupBy: [],
  orderBy: [],
  limit: 100,
  distinct: false
})

const havingClause = ref('')
const tableSearch = ref('')
const showSaveModal = ref(false)
const showSavedQueries = ref(false)
const saveQueryName = ref('')

interface SavedQuery { name: string; sql: string; model: any; dataSourceId: number | null }
const savedQueries = ref<SavedQuery[]>(loadSavedQueriesFromStorage())

function loadSavedQueriesFromStorage(): SavedQuery[] {
  try {
    return JSON.parse(localStorage.getItem('dp_saved_queries') || '[]')
  } catch { return [] }
}

function persistSavedQueries() {
  localStorage.setItem('dp_saved_queries', JSON.stringify(savedQueries.value))
}

function saveCurrentQuery() {
  if (!generatedSql.value) {
    message.warning(t('queryBuilder.generateSqlFirst'))
    return
  }
  saveQueryName.value = ''
  showSaveModal.value = true
}

function confirmSaveQuery() {
  if (!saveQueryName.value.trim()) {
    message.warning(t('queryBuilder.enterQueryName'))
    return
  }
  savedQueries.value.push({
    name: saveQueryName.value.trim(),
    sql: generatedSql.value,
    model: JSON.parse(JSON.stringify(model.value)),
    dataSourceId: dataSourceId.value
  })
  persistSavedQueries()
  showSaveModal.value = false
  message.success(t('queryBuilder.querySaved'))
}

function loadSavedQuery(sq: SavedQuery) {
  if (sq.model) {
    model.value = { ...sq.model }
    mainTable.value = sq.model.tables?.[0]?.name || ''
  }
  if (sq.dataSourceId) dataSourceId.value = sq.dataSourceId
  generatedSql.value = sq.sql || ''
  showSavedQueries.value = false
  message.success(t('queryBuilder.queryLoaded'))
}

function removeSavedQuery(idx: number) {
  savedQueries.value.splice(idx, 1)
  persistSavedQueries()
  message.success(t('queryBuilder.deleted'))
}

/** 将当前查询创建为图表 */
function createAsChart() {
  if (!generatedSql.value || !dataSourceId.value) {
    message.warning(t('queryBuilder.selectDataSourceAndSql'))
    return
  }
  // 导航到图表管理页面并携带SQL和数据源参数
  router.push({
    path: '/chart/manage',
    query: {
      action: 'create',
      sql: encodeURIComponent(generatedSql.value),
      dataSourceId: String(dataSourceId.value)
    }
  })
}

/** 将当前查询创建为报表 */
function createAsReport() {
  if (!generatedSql.value || !dataSourceId.value) {
    message.warning(t('queryBuilder.selectDataSourceAndSql'))
    return
  }
  // 导航到报表管理页面并携带SQL和数据源参数
  router.push({
    path: '/report/manage',
    query: {
      action: 'create',
      sql: encodeURIComponent(generatedSql.value),
      dataSourceId: String(dataSourceId.value)
    }
  })
}

// Animated stat values for Page_Header_Stats
const animatedTableCount = useCountAnimation(computed(() => tableNames.value.length))
const animatedFieldCount = useCountAnimation(computed(() => model.value.selectFields.length))
const animatedJoinCount = useCountAnimation(computed(() => model.value.joins.length))
const animatedConditionCount = useCountAnimation(computed(() => model.value.conditions.length))

const tableOptions = computed(() => tableNames.value.map(t => ({ label: t, value: t })))
const allFieldOptions = computed(() => {
  const options: Array<{ label: string; value: string }> = []
  tables.value.forEach(t => {
    t.columns.forEach(c => {
      options.push({ label: `${t.tableName}.${c.name}`, value: `${t.tableName}.${c.name}` })
    })
  })
  return options
})

const tableTreeData = computed<TreeOption[]>(() => {
  return tableNames.value.map(name => {
    const loaded = tables.value.find(t => t.tableName === name)
    const result: TreeOption = {
      key: name,
      label: name,
      isLeaf: false
    }
    if (loaded) {
      result.children = loaded.columns.map(c => ({
        key: `${name}.${c.name}`,
        label: `${c.name} (${c.type})`,
        isLeaf: true
      }))
    }
    return result
  })
})

const filteredTableTreeData = computed<TreeOption[]>(() => {
  if (!tableSearch.value.trim()) return tableTreeData.value
  const kw = tableSearch.value.toLowerCase()
  return tableTreeData.value.filter(t => String(t.label || '').toLowerCase().includes(kw))
})

const aggregateOptions = [
  { label: 'COUNT', value: 'COUNT' },
  { label: 'SUM', value: 'SUM' },
  { label: 'AVG', value: 'AVG' },
  { label: 'MAX', value: 'MAX' },
  { label: 'MIN', value: 'MIN' }
]

const joinTypeOptions = [
  { label: 'INNER', value: 'INNER' },
  { label: 'LEFT', value: 'LEFT' },
  { label: 'RIGHT', value: 'RIGHT' }
]

const operatorOptions = [
  { label: '=', value: '=' },
  { label: '!=', value: '!=' },
  { label: '>', value: '>' },
  { label: '>=', value: '>=' },
  { label: '<', value: '<' },
  { label: '<=', value: '<=' },
  { label: 'LIKE', value: 'LIKE' },
  { label: 'IN', value: 'IN' },
  { label: 'BETWEEN', value: 'BETWEEN' }
]

const logicOptions = [
  { label: 'AND', value: 'AND' },
  { label: 'OR', value: 'OR' }
]

const orderDirectionOptions = [
  { label: 'ASC', value: 'ASC' },
  { label: 'DESC', value: 'DESC' }
]

function getFieldOptions(tableName: string) {
  const table = tables.value.find(t => t.tableName === tableName)
  if (!table && tableName && dataSourceId.value) {
    // 懒加载：如果列信息还没加载，触发加载
    getColumnMeta(dataSourceId.value, tableName).then(res => {
      if (res.data && !tables.value.find(t => t.tableName === tableName)) {
        tables.value.push(res.data)
      }
    })
  }
  return table?.columns.map(c => ({ label: c.name, value: c.name })) || []
}

async function loadDataSources() {
  loadingDataSources.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    dataSourceOptions.value = (res.data?.list || []).map((ds: any) => ({
      label: ds.name,
      value: ds.id
    }))
  } finally {
    loadingDataSources.value = false
  }
}

async function loadTables() {
  if (!dataSourceId.value) return
  loadingTables.value = true
  try {
    const res = await getTableNames(dataSourceId.value)
    tableNames.value = res.data || []
    tables.value = []
  } catch (e) {
    message.error(t('queryBuilder.loadTablesFailed'))
  } finally {
    loadingTables.value = false
  }
}

async function handleTreeLoad(node: TreeOption) {
  const tableName = node.key as string
  if (!dataSourceId.value || tables.value.find(t => t.tableName === tableName)) return
  try {
    const res = await getColumnMeta(dataSourceId.value, tableName)
    if (res.data) {
      tables.value.push(res.data)
    }
  } catch (e) {
    message.error(t('queryBuilder.loadColumnsFailed', { name: tableName }))
  }
}

function onMainTableChange(tableName: string) {
  model.value.tables = [{ name: tableName }]
}

function onTableSelect(keys: string[]) {
  if (keys.length > 0 && keys[0] && !keys[0].includes('.')) {
    mainTable.value = keys[0] as string
    onMainTableChange(keys[0] as string)
  }
}

function addSelectField() {
  model.value.selectFields.push({ table: '', field: '', alias: '' } as SelectField)
}

function addAllFields() {
  if (!mainTable.value) {
    message.warning(t('queryBuilder.selectMainTableFirst'))
    return
  }
  const table = tables.value.find(t => t.tableName === mainTable.value)
  if (table) {
    table.columns.forEach(c => {
      if (!model.value.selectFields.find(f => f.table === mainTable.value && f.field === c.name)) {
        model.value.selectFields.push({ table: mainTable.value, field: c.name } as SelectField)
      }
    })
  } else {
    message.info(t('queryBuilder.loadFieldsFirst'))
  }
}

function removeSelectField(index: number) {
  model.value.selectFields.splice(index, 1)
}

function addJoin() {
  model.value.joins.push({
    type: 'LEFT',
    leftTable: mainTable.value,
    leftField: '',
    rightTable: '',
    rightField: ''
  } as JoinConfig)
}

function removeJoin(index: number) {
  model.value.joins.splice(index, 1)
}

function addCondition() {
  model.value.conditions.push({
    field: '',
    operator: '=',
    value: '',
    logic: 'AND'
  } as WhereCondition)
}

function removeCondition(index: number) {
  model.value.conditions.splice(index, 1)
}

function addOrderBy() {
  model.value.orderBy.push({ field: '', direction: 'ASC' })
}

function removeOrderBy(index: number) {
  model.value.orderBy.splice(index, 1)
}

async function generateAndPreview() {
  if (!dataSourceId.value) {
    message.warning(t('queryBuilder.selectDataSourceWarning'))
    return
  }
  if (model.value.tables.length === 0) {
    message.warning(t('queryBuilder.selectMainTableWarning'))
    return
  }

  previewing.value = true
  try {
    // 生成 SQL
    const sqlRes = await generateSql(model.value)
    generatedSql.value = sqlRes.data || ''

    // 预览数据
    const dataRes = await previewQuery(dataSourceId.value, model.value, model.value.limit || 100)
    previewData.value = dataRes.data || []

    // 生成列定义
    if (previewData.value.length > 0 && previewData.value[0]) {
      const keys = Object.keys(previewData.value[0]!)
      previewColumns.value = keys.map(key => ({
        title: key,
        key: key,
        ellipsis: { tooltip: true }
      }))
    }

    message.success(t('queryBuilder.querySuccess'))
  } catch (e) {
    message.error(t('queryBuilder.queryFailed'))
  } finally {
    previewing.value = false
  }
}

function copySql() {
  if (generatedSql.value) {
    navigator.clipboard.writeText(generatedSql.value)
    message.success(t('queryBuilder.copiedToClipboard'))
  }
}

function exportCsv() {
  if (previewData.value.length === 0) return
  const keys = Object.keys(previewData.value[0]!)
  let csv = keys.join(',') + '\n'
  previewData.value.forEach(row => {
    csv += keys.map(k => {
      const val = String(row[k] ?? '')
      return val.includes(',') ? `"${val}"` : val
    }).join(',') + '\n'
  })
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'query_result.csv'
  link.click()
  message.success(t('queryBuilder.exportedCsv'))
}

function resetModel() {
  model.value = {
    tables: [],
    joins: [],
    selectFields: [],
    conditions: [],
    groupBy: [],
    orderBy: [],
    limit: 100
  }
  mainTable.value = ''
  generatedSql.value = ''
  previewData.value = []
  tables.value = []
}

async function loadRlsRuleCount() {
  try {
    const { getAllRules } = await import('@/api/rls')
    const res = await getAllRules()
    rlsRuleCount.value = ((res.data || []) as any[]).filter((r: any) => r.enabled !== false).length
  } catch { rlsRuleCount.value = 0 }
}

onMounted(() => {
  loadDataSources()
  loadRlsRuleCount()
})
</script>

<style scoped>
.query-builder {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.qb-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-check {
  font-size: 13px;
}

.qb-content {
  display: grid;
  grid-template-columns: 200px 1fr 380px;
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.tables-panel {
  min-height: 0;
  overflow: hidden;
}

.tables-panel :deep(.n-card__content) {
  overflow: auto;
  max-height: calc(100vh - 300px);
}

.config-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  padding-bottom: 8px;
}

.config-section {
  flex-shrink: 0;
}

.field-row,
.join-row,
.condition-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.field-row.compact {
  margin-bottom: 4px;
}

.sql-keyword {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 13px;
  flex-shrink: 0;
}

.empty-hint {
  color: var(--text-tertiary);
  font-size: 13px;
  padding: 6px 0;
}

.action-bar {
  display: flex;
  gap: 8px;
  padding: 10px 0 4px;
  border-top: 1px solid var(--border-light, #e5e7eb);
  margin-top: 4px;
  position: sticky;
  bottom: 0;
  background: var(--card-bg, #fff);
}

.result-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.sql-card {
  flex-shrink: 0;
}

.data-card {
  flex: 1;
  min-height: 200px;
}

@media (max-width: 1200px) {
  .qb-content {
    grid-template-columns: 1fr;
  }
}
</style>
