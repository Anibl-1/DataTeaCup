<template>
  <div class="system-config-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><SettingsOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalConfigCount }}</span>
          <span class="stat-label">配置总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><FolderOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ groups.length }}</span>
          <span class="stat-label">配置分组</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><LockClosedOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ systemConfigCount }}</span>
          <span class="stat-label">系统配置</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><CreateOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ customConfigCount }}</span>
          <span class="stat-label">自定义配置</span>
        </div>
      </div>
    </div>

    <!-- 主内容区: 左侧分组导航 + 右侧配置列表 -->
    <div class="content-layout">
      <!-- 左侧配置分组导航 -->
      <n-card class="group-tree-card" :bordered="true" size="small">
        <template #header>
          <div style="display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600;">
            <n-icon size="16" color="var(--color-primary)"><FolderOutline /></n-icon>
            <span>配置分组</span>
          </div>
        </template>
        <n-spin :show="groupLoading">
          <n-menu
            :options="groupMenuOptions"
            :value="activeGroup"
            @update:value="handleGroupChange"
          />
        </n-spin>
      </n-card>

      <!-- 右侧配置项列表 -->
      <n-card class="main-card" style="flex: 1; min-width: 0;">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="22" color="var(--color-primary)" class="header-icon"><SettingsOutline /></n-icon>
            <span>{{ activeGroupLabel }} - 配置项</span>
            <n-tag v-if="activeGroup && activeGroup !== '__all__'" type="info" size="small" closable @close="handleGroupChange('__all__')">
              {{ activeGroup }}
            </n-tag>
          </div>
        </template>
        <template #header-extra>
          <n-space>
            <n-button type="primary" @click="handleCreate">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              新增配置
            </n-button>
          </n-space>
        </template>

        <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
        <n-form class="query-form" inline>
          <n-form-item>
            <n-input
              v-model:value="searchKeyword"
              placeholder="搜索配置键/描述..."
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearchReset"
            />
          </n-form-item>
          <n-form-item class="query-form-actions">
            <n-button type="primary" @click="handleSearch">
              <template #icon><n-icon><SearchOutline /></n-icon></template>
              搜索
            </n-button>
            <n-button @click="handleSearchReset">重置</n-button>
          </n-form-item>
        </n-form>

        <!-- Empty State (Req 16.7) -->
        <n-empty v-if="!tableLoading && filteredTableData.length === 0" description="暂无配置项" style="margin: 32px 0;" />

        <n-data-table
          v-else
          :columns="columns"
          :data="filteredTableData"
          :loading="tableLoading"
          :row-key="(row: any) => row.id"
          :scroll-x="1100"
          :pagination="false"
          striped
          class="custom-table"
        />
        <div class="pagination-wrapper">
          <div class="pagination-info">
            <n-tag type="info" size="small" round>
              共 {{ filteredTableData.length }} 条记录
            </n-tag>
          </div>
        </div>
      </n-card>
    </div>

    <!-- 编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="card" :title="editMode === 'add' ? '新增配置' : '编辑配置'" style="width: 600px; border-radius: 16px;">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-form-item label="配置键" path="configKey">
          <n-input v-model:value="formData.configKey" placeholder="如 system.title" :disabled="editMode === 'edit'" maxlength="100" />
        </n-form-item>
        <n-form-item label="配置值" path="configValue">
          <!-- 动态控件：根据 configType 渲染不同编辑控件 -->
          <div style="width: 100%">
            <!-- string → n-input -->
            <n-input
              v-if="currentControlType === 'input'"
              v-model:value="formData.configValue"
              placeholder="配置值"
            />
            <!-- number → n-input-number -->
            <n-input-number
              v-else-if="currentControlType === 'input-number'"
              v-model:value="numberValue"
              placeholder="请输入数字"
              style="width: 100%"
            />
            <!-- boolean → n-switch -->
            <n-switch
              v-else-if="currentControlType === 'switch'"
              :value="booleanValue"
              @update:value="handleBooleanChange"
            />
            <!-- json → n-input textarea with validation -->
            <div v-else-if="currentControlType === 'textarea'" style="width: 100%">
              <n-input
                v-model:value="formData.configValue"
                type="textarea"
                :rows="5"
                placeholder="请输入有效的 JSON"
                @input="handleJsonInput"
              />
              <div v-if="jsonError" class="json-error-tip">{{ jsonError }}</div>
            </div>
            <!-- password → n-input password -->
            <n-input
              v-else-if="currentControlType === 'password'"
              v-model:value="formData.configValue"
              type="password"
              show-password-on="click"
              placeholder="配置值（密码类型）"
            />
          </div>
        </n-form-item>
        <n-form-item label="配置类型">
          <n-select v-model:value="formData.configType" :options="typeOptions" style="width: 200px" />
        </n-form-item>
        <n-form-item label="配置分组" path="configGroup">
          <n-select
            v-model:value="formData.configGroup"
            :options="groupSelectOptions"
            filterable
            tag
            placeholder="选择或输入分组"
            style="width: 200px"
          />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="formData.configDesc" placeholder="配置描述" maxlength="500" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditModal = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, onMounted, computed, watch } from 'vue'
import { NButton, NTag, NSpace, NIcon, NEllipsis, useMessage, useDialog } from 'naive-ui'
import type { FormInst, MenuOption } from 'naive-ui'
import { SettingsOutline, SearchOutline, AddOutline, CreateOutline, TrashOutline, FolderOutline, ListOutline, CheckmarkCircleOutline, CloseCircleOutline, LockClosedOutline } from '@vicons/ionicons5'
import * as configApi from '@/api/system/systemConfig'
import type { SystemConfig } from '@/api/system/systemConfig'
import { formatDateTime } from '@/utils/format'
import { getConfigTypeColor, getConfigControlType, validateJsonFormat, filterConfigs } from '@/utils/configSerializer'

const message = useMessage()
const dialog = useDialog()

// 分组相关
const groupLoading = ref(false)
const groups = ref<string[]>([])
const activeGroup = ref<string | null>(null)

// 表格相关
const tableLoading = ref(false)
const tableData = ref<SystemConfig[]>([])
const searchKeyword = ref('')
const showEditModal = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const saving = ref(false)
const formRef = ref<FormInst | null>(null)
const currentEditId = ref<number | null>(null)

// JSON 校验错误
const jsonError = ref<string | null>(null)

// 防抖搜索相关
const debouncedKeyword = ref('')
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(searchKeyword, (val) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedKeyword.value = val
  }, 300)
})

// 统计数据
const totalConfigCount = computed(() => tableData.value.length)
const systemConfigCount = computed(() => tableData.value.filter(c => c.isSystem).length)
const customConfigCount = computed(() => tableData.value.filter(c => !c.isSystem).length)

// 当前分组标签
const activeGroupLabel = computed(() => {
  if (!activeGroup.value || activeGroup.value === '__all__') return '全部'
  return activeGroup.value
})

const formData = ref<SystemConfig>({
  configKey: '', configValue: '', configType: 'string', configDesc: '', configGroup: '默认'
})

const formRules = {
  configKey: { required: true, message: '请输入配置键', trigger: 'blur' },
  configValue: { required: true, message: '请输入配置值', trigger: 'blur' }
}

const typeOptions = [
  { label: '字符串', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: 'JSON', value: 'json' },
  { label: '密码', value: 'password' }
]

// 当前编辑控件类型（根据 configType 动态计算）
const currentControlType = computed(() => getConfigControlType(formData.value.configType || 'string'))

// number 类型的双向绑定
const numberValue = computed({
  get: () => {
    const v = Number(formData.value.configValue)
    return isNaN(v) ? null : v
  },
  set: (val: number | null) => {
    formData.value.configValue = val !== null ? String(val) : ''
  }
})

// boolean 类型的值
const booleanValue = computed(() => formData.value.configValue?.toLowerCase() === 'true')

function handleBooleanChange(val: boolean) {
  formData.value.configValue = String(val)
}

// JSON 实时校验
function handleJsonInput(val: string) {
  if (!val || !val.trim()) {
    jsonError.value = null
    return
  }
  const result = validateJsonFormat(val)
  jsonError.value = result.valid ? null : (result.error || 'JSON 格式错误')
}

// 左侧分组菜单选项
const groupMenuOptions = computed<MenuOption[]>(() => {
  const allOption: MenuOption = {
    label: '全部配置',
    key: '__all__',
    icon: () => h(NIcon, null, { default: () => h(ListOutline) })
  }
  const groupItems: MenuOption[] = groups.value.map((g: string) => ({
    label: `${g}`,
    key: g,
    icon: () => h(NIcon, null, { default: () => h(FolderOutline) })
  }))
  return [allOption, ...groupItems]
})

// 分组下拉选项（用于表单）
const groupSelectOptions = computed(() =>
  groups.value.map((g: string) => ({ label: g, value: g }))
)

// 使用 filterConfigs 工具函数 + 防抖关键词进行前端实时过滤
const filteredTableData = computed(() => {
  return filterConfigs(tableData.value, debouncedKeyword.value)
})

// 类型列使用不同颜色 n-tag
function renderTypeTag(configType: string) {
  const color = getConfigTypeColor(configType || 'string')
  if (color === 'default') {
    return h(NTag, { size: 'small' }, { default: () => configType || 'string' })
  }
  return h(NTag, { size: 'small', color: { color, textColor: '#fff', borderColor: color } }, { default: () => configType || 'string' })
}

const columns = [
  { title: '配置键', key: 'configKey', width: 220,
    render: (row: SystemConfig) => h('code', { style: 'font-size: 13px' }, row.configKey)
  },
  { title: '配置值', key: 'configValue', minWidth: 200,
    render: (row: SystemConfig) => {
      // Boolean type: show toggle icon
      if (row.configType === 'boolean') {
        const isTrue = row.configValue?.toLowerCase() === 'true'
        return h('span', { style: 'display: inline-flex; align-items: center; gap: 6px' }, [
          h(NIcon, { size: 18, color: isTrue ? '#18a058' : '#d03050' }, {
            default: () => h(isTrue ? CheckmarkCircleOutline : CloseCircleOutline)
          }),
          h('span', { style: `color: ${isTrue ? '#18a058' : '#d03050'}; font-size: 13px` }, isTrue ? 'true' : 'false')
        ])
      }
      // Color value: show color swatch
      if (/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(row.configValue?.trim() || '')) {
        const color = row.configValue.trim()
        return h('span', { style: 'display: inline-flex; align-items: center; gap: 8px' }, [
          h('span', {
            style: `display: inline-block; width: 16px; height: 16px; border-radius: 3px; background: ${color}; border: 1px solid rgba(128,128,128,0.3); flex-shrink: 0`
          }),
          h('code', { style: 'font-size: 13px' }, color)
        ])
      }
      // Password type: mask the value
      if (row.configType === 'password') {
        return h('span', { style: 'color: #999; font-size: 13px' }, '••••••••')
      }
      // Default: show text with ellipsis
      return h(NEllipsis, { style: 'max-width: 300px', tooltip: true }, { default: () => row.configValue })
    }
  },
  { title: '类型', key: 'configType', width: 100,
    render: (row: SystemConfig) => renderTypeTag(row.configType || 'string')
  },
  { title: '分组', key: 'configGroup', width: 100,
    render: (row: SystemConfig) => h(NTag, { size: 'small', type: 'info' }, { default: () => row.configGroup || '默认' })
  },
  { title: '描述', key: 'configDesc', width: 180, ellipsis: { tooltip: true } },
  { title: '系统配置', key: 'isSystem', width: 90,
    render: (row: SystemConfig) => h(NTag, { type: row.isSystem ? 'warning' : 'default', size: 'small' },
      { default: () => row.isSystem ? '系统' : '自定义' })
  },
  { title: '更新时间', key: 'updateTime', width: 170,
    render: (row: SystemConfig) => formatDateTime(row.updateTime)
  },
  { title: '操作', key: 'actions', width: 150, fixed: 'right' as const,
    render: (row: SystemConfig) =>
      h(NSpace, { size: 4 }, {
        default: () => [
          h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) },
            {
              icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
              default: () => '编辑'
            }),
          h(NButton, { size: 'small', type: 'error', quaternary: true, disabled: !!row.isSystem, onClick: () => handleDelete(row) },
            {
              icon: () => h(NIcon, null, { default: () => h(TrashOutline) }),
              default: () => '删除'
            })
        ]
      })
  }
]

/** 搜索 */
function handleSearch() {
  debouncedKeyword.value = searchKeyword.value
}

/** 重置搜索 */
function handleSearchReset() {
  searchKeyword.value = ''
  debouncedKeyword.value = ''
}

/** 加载分组列表 */
async function loadGroups() {
  groupLoading.value = true
  try {
    const res = await configApi.getConfigGroups()
    groups.value = (res as any).data || []
  } catch (e: any) {
    message.error(e.message || '加载分组失败')
  } finally {
    groupLoading.value = false
  }
}

/** 加载配置数据（按分组或全部） */
async function loadConfigData() {
  tableLoading.value = true
  try {
    if (activeGroup.value && activeGroup.value !== '__all__') {
      const res = await configApi.getConfigByGroup(activeGroup.value)
      tableData.value = (res as any).data || []
    } else {
      const res = await configApi.getAllSystemConfigs()
      tableData.value = (res as any).data || []
    }
  } catch (e: any) {
    message.error(e.message || '加载配置失败')
  } finally {
    tableLoading.value = false
  }
}

function handleGroupChange(key: string) {
  activeGroup.value = key
  searchKeyword.value = ''
  debouncedKeyword.value = ''
  loadConfigData()
}

function handleCreate() {
  editMode.value = 'add'
  currentEditId.value = null
  jsonError.value = null
  formData.value = {
    configKey: '', configValue: '', configType: 'string', configDesc: '',
    configGroup: (activeGroup.value && activeGroup.value !== '__all__') ? activeGroup.value : '默认'
  }
  showEditModal.value = true
}

function handleEdit(row: SystemConfig) {
  editMode.value = 'edit'
  currentEditId.value = row.id!
  jsonError.value = null
  formData.value = { ...row }
  // 打开编辑时，如果是 JSON 类型，立即校验
  if (row.configType === 'json' && row.configValue) {
    handleJsonInput(row.configValue)
  }
  showEditModal.value = true
}

function handleDelete(row: SystemConfig) {
  if (row.isSystem) { message.warning('系统配置不允许删除'); return }
  dialog.warning({
    title: '确认删除',
    content: `确定删除配置「${row.configKey}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await configApi.deleteSystemConfig(row.id!)
        message.success('删除成功')
        loadGroups()
        loadConfigData()
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

async function handleSave() {
  await formRef.value?.validate()
  // JSON 类型保存前校验
  if (formData.value.configType === 'json' && formData.value.configValue) {
    const result = validateJsonFormat(formData.value.configValue)
    if (!result.valid) {
      jsonError.value = result.error || 'JSON 格式错误'
      message.error('JSON 格式不正确，请修正后再保存')
      return
    }
  }
  saving.value = true
  try {
    if (editMode.value === 'add') {
      await configApi.createSystemConfig(formData.value)
      message.success('创建成功')
    } else {
      await configApi.updateSystemConfig(currentEditId.value!, formData.value)
      message.success('更新成功')
    }
    showEditModal.value = false
    loadGroups()
    loadConfigData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadGroups()
  loadConfigData()
})
</script>

<style scoped>
.system-config-page {
  padding: 0;
}

/* 主内容区: 左侧分组导航 + 右侧配置列表 */
.content-layout {
  display: flex;
  gap: 16px;
}

.group-tree-card {
  width: 220px;
  flex-shrink: 0;
}

/* JSON 校验错误提示 */
.json-error-tip {
  color: #d03050;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}







/* 响应式 */
@media (max-width: 768px) {
  .content-layout { flex-direction: column; }
  .group-tree-card { width: 100%; max-height: 200px; }
  .main-card { border-radius: 14px !important; }
}
</style>

<style>
/* index 深色模式（非 scoped） */
html.dark .system-config-page .group-tree-card .n-card {
  background-color: #1f2937 !important;
  border-color: #374151 !important;
}
html.dark .system-config-page .main-card .n-card {
  background-color: #1f2937 !important;
  border-color: #374151 !important;
}
html.dark .system-config-page .n-data-table {
  --n-td-color: #1f2937 !important;
  --n-th-color: #111827 !important;
  --n-td-color-striped: #1a2332 !important;
}
html.dark .system-config-page .n-menu {
  --n-item-color-hover: #374151 !important;
  --n-item-color-active: #1e3a5f !important;
}
html.dark .json-error-tip {
  color: #e88080 !important;
}
html.dark .system-config-page .n-modal {
  --n-color: #1f2937 !important;
  --n-border-color: #374151 !important;
}
</style>
