<template>
  <div class="rls-config-page">
    <!-- Page_Header_Stats: 规则统计概览 (Req 1.1, 13.3) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><LockClosedOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ rules.length }}</span>
          <span class="stat-label">规则总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ enabledCount }}</span>
          <span class="stat-label">已启用</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ disabledCount }}</span>
          <span class="stat-label">已禁用</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 规则列表 (Req 1.1, 13.3) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><LockClosedOutline /></n-icon>
          <span>行级安全规则管理</span>
        </div>
      </template>
      <template #header-extra>
        <n-space :size="8">
          <n-button size="small" secondary @click="router.push('/query-builder')">
            <template #icon><n-icon><CodeWorkingOutline /></n-icon></template>
            查询构建器
          </n-button>
          <n-button type="primary" size="small" @click="ruleModal.openCreate()">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            添加规则
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 1.4) -->
      <div class="rls-toolbar">
        <n-space :size="8" align="center">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索表名/过滤字段"
            clearable
            style="width: 220px;"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
          <n-select
            v-model:value="filterStatus"
            placeholder="状态筛选"
            clearable
            style="width: 140px;"
            :options="statusFilterOptions"
            @update:value="handleSearch"
          />
          <n-button type="primary" size="small" @click="handleSearch">搜索</n-button>
          <n-button size="small" @click="handleReset">重置</n-button>
        </n-space>
        <n-space :size="8" align="center">
          <n-button v-if="checkedRowKeys.length > 0" size="small" type="error" @click="handleBatchDelete">
            <template #icon><n-icon><TrashOutline /></n-icon></template>
            批量删除 ({{ checkedRowKeys.length }})
          </n-button>
        </n-space>
      </div>

      <!-- Data Table (Req 1.6) -->
      <n-data-table
        v-if="filteredRules.length > 0 || loading"
        v-model:checked-row-keys="checkedRowKeys"
        :columns="ruleColumns"
        :data="paginatedRules"
        :loading="loading"
        :pagination="false"
        :row-key="rowKey"
        :scroll-x="1100"
        striped
        class="custom-table"
      />

      <!-- Empty State (Req 16.7) -->
      <n-empty v-if="!loading && filteredRules.length === 0" description="暂无行级安全规则" style="padding: 48px 24px;">
        <template #extra>
          <n-button type="primary" size="small" @click="ruleModal.openCreate()">添加规则</n-button>
        </template>
      </n-empty>

      <!-- Pagination_Wrapper (Req 1.5) -->
      <div v-if="filteredRules.length > 0" class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ filteredRules.length }} 条记录
          </n-tag>
        </div>
        <n-pagination
          v-model:page="currentPage"
          :page-size="pageSize"
          :item-count="filteredRules.length"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          show-quick-jumper
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 规则预览卡片 -->
    <n-card v-if="enabledCount > 0" class="main-card" style="margin-top: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="#10b981" class="header-icon"><EyeOutline /></n-icon>
          <span>规则预览</span>
        </div>
      </template>
      <template #header-extra>
        <n-tag type="success" size="small">{{ enabledCount }} 条规则生效中</n-tag>
      </template>
      <div class="rule-preview-list">
        <div v-for="rule in enabledRules" :key="rule.id" class="rule-preview-item">
          <n-tag :type="rule.enabled !== false ? 'success' : 'default'" size="small" style="margin-right:8px;">{{ getRoleName(rule.roleId) }}</n-tag>
          <code class="rule-sql-preview">{{ getDataSourceName(rule.dataSourceId) }}.{{ rule.tableName }} → WHERE {{ rule.filterField }} {{ rule.filterOperator }} {{ rule.filterValue }}</code>
        </div>
      </div>
    </n-card>

    <!-- SQL 注入测试卡片 (Req 13.4) -->
    <n-card class="main-card" style="margin-top: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><BugOutline /></n-icon>
          <span>SQL 注入测试</span>
        </div>
      </template>

      <div class="test-templates">
        <n-tag v-for="tpl in testTemplates" :key="tpl" size="small" round clickable style="cursor:pointer;" @click="testSql = tpl">{{ tpl }}</n-tag>
      </div>
      <n-input
        v-model:value="testSql"
        type="textarea"
        placeholder="输入 SQL 语句测试 RLS 过滤效果，或点击上方模板快速填入"
        :autosize="{ minRows: 2, maxRows: 4 }"
        style="margin-top: 8px;"
      />
      <n-space style="margin-top: 12px;">
        <n-button type="primary" :loading="testing" @click="handleTestInject">
          <template #icon><n-icon><FlashOutline /></n-icon></template>
          执行测试
        </n-button>
        <n-button v-if="injectedSql" @click="clearTestResult">清除结果</n-button>
      </n-space>
      <div v-if="injectedSql" class="injected-sql-result">
        <n-divider />
        <div class="result-comparison">
          <div class="result-col">
            <p class="result-label">原始 SQL：</p>
            <n-code :code="testSql" language="sql" word-wrap />
          </div>
          <div class="result-col">
            <p class="result-label">注入后的 SQL：</p>
            <n-code :code="injectedSql" language="sql" word-wrap />
          </div>
        </div>
      </div>
    </n-card>

    <!-- 添加/编辑规则弹窗 (Req 13.3) -->
    <n-modal
      v-model:show="ruleModal.visible.value"
      preset="dialog"
      :title="ruleModal.mode.value === 'create' ? '添加 RLS 规则' : '编辑 RLS 规则'"
      style="width: 600px; border-radius: 16px;"
    >
      <n-form :model="ruleModal.formData.value" label-placement="left" label-width="100">
        <n-form-item label="角色">
          <n-select
            v-model:value="ruleModal.formData.value.roleId"
            :options="roleOptions"
            placeholder="选择角色"
          />
        </n-form-item>
        <n-form-item label="数据源">
          <n-select
            v-model:value="ruleModal.formData.value.dataSourceId"
            :options="dataSourceOptions"
            placeholder="选择数据源"
          />
        </n-form-item>
        <n-form-item label="表名">
          <n-input v-model:value="ruleModal.formData.value.tableName" placeholder="输入表名" />
        </n-form-item>
        <n-form-item label="过滤字段">
          <n-input v-model:value="ruleModal.formData.value.filterField" placeholder="如 department_id" />
        </n-form-item>
        <n-form-item label="操作符">
          <n-select
            v-model:value="ruleModal.formData.value.filterOperator"
            :options="operatorOptions"
          />
        </n-form-item>
        <n-form-item label="过滤值">
          <n-input v-model:value="ruleModal.formData.value.filterValue" placeholder="支持变量如 ${user.deptId}" />
        </n-form-item>
        <n-form-item label="启用状态">
          <n-switch v-model:value="ruleModal.formData.value.enabled">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>
      </n-form>
      <n-alert type="info" title="变量说明" style="margin-top: 12px">
        支持的变量：${user.id}、${user.username}、${user.deptId}、${user.roleId}
      </n-alert>
      <template #action>
        <n-button @click="ruleModal.close()">取消</n-button>
        <n-button type="primary" :loading="ruleModal.submitting.value" @click="handleSaveRule">保存</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NSwitch, NIcon, NTag, useMessage, useDialog } from 'naive-ui'
import {
  LockClosedOutline,
  AddOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  SearchOutline,
  BugOutline,
  FlashOutline,
  CodeWorkingOutline,
  EyeOutline,
  TrashOutline
} from '@vicons/ionicons5'
import type { DataTableColumns } from 'naive-ui'
import type { RlsRule } from '@/types/rls'
import { getAllRules, saveRule, updateRule, deleteRule, testInject } from '@/api/rls'
import { getDataSourceList } from '@/api/dataSource'
import { getRoleList } from '@/api/system/role'
import { useFormModal } from '@/composables/useFormModal'
import { handleApiError } from '@/utils/error'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const testing = ref(false)
const checkedRowKeys = ref<number[]>([])

const rules = ref<RlsRule[]>([])
const roleOptions = ref<Array<{ label: string; value: number }>>([])
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])

// Lookup maps for role and data source names
const roleNameMap = ref<Record<number, string>>({})
const dataSourceNameMap = ref<Record<number, string>>({})

// Search & filter
const searchKeyword = ref('')
const filterStatus = ref<string | null>(null)

const statusFilterOptions = [
  { label: '已启用', value: 'enabled' },
  { label: '已禁用', value: 'disabled' }
]

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)

// Row key helper
const rowKey = (row: RlsRule) => row.id

// Computed stats
const enabledCount = computed(() => rules.value.filter(r => r.enabled !== false).length)
const disabledCount = computed(() => rules.value.filter(r => r.enabled === false).length)
const enabledRules = computed(() => rules.value.filter(r => r.enabled !== false))

const testTemplates = [
  'SELECT * FROM users WHERE id = 1',
  'SELECT * FROM orders WHERE status = \'active\'',
  'SELECT name, email FROM employees'
]

// Filtered rules
const filteredRules = computed(() => {
  let list = rules.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(r =>
      r.tableName.toLowerCase().includes(kw) ||
      r.filterField.toLowerCase().includes(kw)
    )
  }
  if (filterStatus.value === 'enabled') {
    list = list.filter(r => r.enabled !== false)
  } else if (filterStatus.value === 'disabled') {
    list = list.filter(r => r.enabled === false)
  }
  return list
})

// Paginated rules
const paginatedRules = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRules.value.slice(start, start + pageSize.value)
})

const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  searchKeyword.value = ''
  filterStatus.value = null
  currentPage.value = 1
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

// --- useFormModal for rule CRUD ---
interface RuleFormData extends Record<string, unknown> {
  id: number | undefined
  roleId: number | undefined
  dataSourceId: number | undefined
  tableName: string
  filterField: string
  filterOperator: string
  filterValue: string
  enabled: boolean
}

const ruleModal = useFormModal<RuleFormData>({
  defaultFormData: () => ({
    id: undefined,
    roleId: undefined,
    dataSourceId: undefined,
    tableName: '',
    filterField: '',
    filterOperator: '=',
    filterValue: '',
    enabled: true
  }),
  createFn: async (formData) => {
    await saveRule(formData as unknown as RlsRule)
  },
  updateFn: async (formData) => {
    await updateRule(formData as unknown as RlsRule)
  },
  onSuccess: (mode) => {
    message.success(mode === 'create' ? '添加成功' : '更新成功')
    loadRules()
  },
  onError: (error) => {
    message.error(handleApiError(error, '保存规则'))
  }
})

// SQL injection test
const testSql = ref('')
const injectedSql = ref('')

const operatorOptions = [
  { label: '等于 (=)', value: '=' },
  { label: '不等于 (!=)', value: '!=' },
  { label: '大于 (>)', value: '>' },
  { label: '小于 (<)', value: '<' },
  { label: '包含 (IN)', value: 'IN' },
  { label: '模糊匹配 (LIKE)', value: 'LIKE' }
]

// Helper to get role name
function getRoleName(roleId: number): string {
  return roleNameMap.value[roleId] || String(roleId)
}

// Helper to get data source name
function getDataSourceName(dataSourceId: number): string {
  return dataSourceNameMap.value[dataSourceId] || String(dataSourceId)
}

// Toggle enable/disable (Req 13.3)
async function handleToggleEnabled(row: RlsRule) {
  const newEnabled = row.enabled === false
  try {
    await updateRule({ ...row, enabled: newEnabled })
    message.success(newEnabled ? '已启用' : '已禁用')
    loadRules()
  } catch (error) {
    message.error(handleApiError(error, '切换状态'))
  }
}

// Table columns
const ruleColumns: DataTableColumns<RlsRule> = [
  {
    title: '角色',
    key: 'roleId',
    width: 120,
    render: (row) => getRoleName(row.roleId)
  },
  {
    title: '数据源',
    key: 'dataSourceId',
    width: 140,
    render: (row) => getDataSourceName(row.dataSourceId)
  },
  { title: '表名', key: 'tableName', width: 150, ellipsis: { tooltip: true } },
  { title: '过滤字段', key: 'filterField', width: 120 },
  {
    title: '操作符',
    key: 'filterOperator',
    width: 80,
    render: (row) => h(NTag, { size: 'small', type: 'info' }, { default: () => row.filterOperator })
  },
  { title: '过滤值', key: 'filterValue', width: 150, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'enabled',
    width: 100,
    render: (row) => h(NSwitch, {
      value: row.enabled !== false,
      size: 'small',
      onUpdateValue: () => handleToggleEnabled(row)
    }, {
      checked: () => '启用',
      unchecked: () => '禁用'
    })
  },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right' as const,
    render: (row) => {
      return h('div', { style: 'display: flex; gap: 4px; align-items: center;' }, [
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          onClick: () => ruleModal.openEdit({
            id: row.id,
            roleId: row.roleId,
            dataSourceId: row.dataSourceId,
            tableName: row.tableName,
            filterField: row.filterField,
            filterOperator: row.filterOperator,
            filterValue: row.filterValue,
            enabled: row.enabled !== false
          } as RuleFormData)
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'tiny',
          quaternary: true,
          type: 'error',
          onClick: () => handleDelete(row)
        }, { default: () => '删除' })
      ])
    }
  }
]

async function loadRules() {
  loading.value = true
  try {
    const res = await getAllRules()
    rules.value = res.data || []
  } catch (error) {
    message.error(handleApiError(error, '获取规则列表'))
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const res = await getRoleList({ page: 1, pageSize: 100 })
    const list = res.data?.list || []
    roleOptions.value = list.map((r: any) => ({ label: r.name, value: r.id }))
    roleNameMap.value = {}
    list.forEach((r: any) => { roleNameMap.value[r.id] = r.name })
  } catch {
    // ignore
  }
}

async function loadDataSources() {
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    const list = res.data?.list || []
    dataSourceOptions.value = list.map((ds: any) => ({ label: ds.name, value: ds.id }))
    dataSourceNameMap.value = {}
    list.forEach((ds: any) => { dataSourceNameMap.value[ds.id] = ds.name })
  } catch {
    // ignore
  }
}

async function handleSaveRule() {
  if (!ruleModal.formData.value.roleId || !ruleModal.formData.value.dataSourceId || !ruleModal.formData.value.tableName) {
    message.warning('请填写完整信息')
    return
  }
  await ruleModal.submit()
}

function handleDelete(row: RlsRule) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除表「${row.tableName}」的行级安全规则吗？此操作不可恢复。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteRule(row.id!)
        message.success('删除成功')
        loadRules()
      } catch (error) {
        message.error(handleApiError(error, '删除规则'))
      }
    }
  })
}

async function handleTestInject() {
  if (!testSql.value) {
    message.warning('请输入 SQL 语句')
    return
  }
  testing.value = true
  try {
    const res = await testInject(testSql.value)
    injectedSql.value = res.data || testSql.value
  } catch (error) {
    message.error(handleApiError(error, 'SQL 注入测试'))
  } finally {
    testing.value = false
  }
}

function clearTestResult() {
  injectedSql.value = ''
}

async function handleBatchDelete() {
  if (checkedRowKeys.value.length === 0) return
  dialog.warning({
    title: '批量删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 条规则吗？此操作不可恢复。`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        for (const id of checkedRowKeys.value) {
          await deleteRule(id)
        }
        message.success(`已删除 ${checkedRowKeys.value.length} 条规则`)
        checkedRowKeys.value = []
        loadRules()
      } catch (error) {
        message.error(handleApiError(error, '批量删除'))
      }
    }
  })
}

onMounted(() => {
  loadRules()
  loadRoles()
  loadDataSources()
})
</script>

<style scoped>
.rls-config-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.rls-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.injected-sql-result {
  margin-top: 12px;
}

.result-label {
  margin: 0 0 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.result-comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.result-col {
  min-width: 0;
}

.rule-preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rule-preview-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.rule-sql-preview {
  font-size: 13px;
  color: #374151;
  font-family: 'Consolas', 'Monaco', monospace;
  background: transparent;
}

.test-templates {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: column;
  }
  .result-comparison {
    grid-template-columns: 1fr;
  }
}


</style>

<style>
/* RlsConfig 深色模式（非 scoped） */
html.dark .config-label { color: #94a3b8 !important; }
html.dark .config-value { color: #e2e8f0 !important; }
</style>
