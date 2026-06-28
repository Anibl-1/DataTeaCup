<template>
  <div class="data-view-page">
    <n-spin :show="loading" description="加载中...">
      <div v-if="!viewConfig && !loading" class="empty-state">
        <n-icon size="64" color="#ccc"><AlertCircleOutline /></n-icon>
        <p>数据管理不存在或已被禁用</p>
        <n-button @click="goBack">返回</n-button>
      </div>

      <template v-else-if="viewConfig">
        <!-- 页面头部：标题 + 搜索 + 按钮 在同一行 -->
        <n-card size="small" class="header-card">
          <div class="header-row">
            <!-- 左侧：标题 -->
            <div class="page-title">
              <n-icon :component="getIconComponent(viewConfig.menuIcon)" size="22" color="var(--color-primary)" />
              <span class="title-text">{{ viewConfig.name }}</span>
              <span v-if="viewConfig.description" class="title-desc">{{ viewConfig.description }}</span>
            </div>
            <!-- 右侧：搜索和按钮 -->
            <div class="header-actions">
              <FilterPanel
                :fields="filterFields"
                :model-value="activeFilters"
                @apply="handleFilterApply"
              />
              <n-input 
                v-model:value="searchKeyword" 
                placeholder="搜索..." 
                clearable 
                style="width: 180px" 
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix><n-icon><SearchOutline /></n-icon></template>
              </n-input>
              <n-button quaternary @click="resetSearch">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
              </n-button>
              <n-divider vertical style="height: 24px; margin: 0 8px;" />
              <n-button v-if="viewConfig.allowExport" :loading="exporting" @click="handleExport">
                <template #icon><n-icon><DownloadOutline /></n-icon></template>
                导出
              </n-button>
              <n-button v-if="viewConfig.allowImport" @click="showImportModal = true">
                <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
                导入
              </n-button>
              <n-button v-if="viewConfig.allowInsert" type="primary" @click="handleAdd">
                <template #icon><n-icon><AddOutline /></n-icon></template>
                新增数据
              </n-button>
            </div>
          </div>
          <!-- 筛选标签 -->
          <div v-if="activeFilters.length > 0 || searchKeyword" class="filter-tags">
            <span class="filter-label">当前筛选：</span>
            <n-tag 
              v-if="searchKeyword"
              closable 
              size="small"
              type="success"
              @close="searchKeyword = ''; handleSearch()"
            >
              关键词: {{ searchKeyword }}
            </n-tag>
            <n-tag 
              v-for="(filter, index) in activeFilters" 
              :key="index" 
              closable 
              size="small"
              type="info"
              @close="removeFilter(index)"
            >
              {{ getFilterLabel(filter) }}
            </n-tag>
            <n-button size="tiny" text type="error" @click="resetSearch">
              <template #icon><n-icon><CloseCircleOutline /></n-icon></template>
              清除全部
            </n-button>
          </div>
        </n-card>

        <!-- 数据表格 -->
        <n-card class="table-card">
          <template #header>
            <div class="table-header">
              <div class="table-header-left">
                <span class="table-title">数据列表</span>
                <n-tag type="info" size="small" round>共 {{ paginationItemCount }} 条</n-tag>
              </div>
              <!-- 批量操作 -->
              <div v-if="checkedKeys.length > 0 && viewConfig.allowDelete" class="batch-actions">
                <n-tag type="warning" size="small">已选 {{ checkedKeys.length }} 条</n-tag>
                <n-popconfirm @positive-click="handleBatchDelete">
                  <template #trigger>
                    <n-button size="small" type="error">
                      <template #icon><n-icon><TrashOutline /></n-icon></template>
                      批量删除
                    </n-button>
                  </template>
                  确定要删除选中的 {{ checkedKeys.length }} 条数据吗？
                </n-popconfirm>
              </div>
            </div>
          </template>
          
          <n-data-table
            :columns="tableColumns"
            :data="tableData"
            :loading="loadingData"
            :pagination="false"
            :row-key="getRowKey"
            :checked-row-keys="checkedKeys"
            :bordered="false"
            :scroll-x="tableScrollX"
            striped
            size="medium"
            @update:checked-row-keys="handleCheck"
          />
          
          <div class="pagination-wrapper">
            <n-pagination
              v-model:page="paginationPage"
              v-model:page-size="paginationPageSize"
              :item-count="paginationItemCount"
              :page-sizes="[10, 20, 50, 100]"
              show-size-picker
              show-quick-jumper
              :prefix="() => `共 ${paginationItemCount} 条`"
              @update:page="handlePageChange"
              @update:page-size="handlePageSizeChange"
            />
          </div>
        </n-card>
      </template>
    </n-spin>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="card" :title="editTitle" :bordered="false" class="edit-modal" style="width: 680px; max-width: 95vw; border-radius: 12px;">
      <template #header>
        <div class="edit-modal-header">
          <div class="edit-modal-icon" :class="isEdit ? 'icon-edit' : 'icon-add'">
            <n-icon :size="18"><CreateOutline v-if="isEdit" /><AddOutline v-else /></n-icon>
          </div>
          <div>
            <div class="edit-modal-title">{{ editTitle }}</div>
            <div class="edit-modal-desc">{{ isEdit ? '修改字段内容后点击保存' : '填写各字段内容后点击确认' }}</div>
          </div>
        </div>
      </template>
      <n-scrollbar style="max-height: 60vh;">
        <!-- 主键区域 -->
        <div v-if="isEdit && primaryKeyColumns.length > 0" class="edit-pk-section">
          <div class="edit-section-badge">
            <n-icon :size="12" color="#d97706"><AlertCircleOutline /></n-icon>
            <span>主键字段（不可编辑）</span>
          </div>
          <div class="edit-pk-grid" :class="{ 'pk-single': primaryKeyColumns.length === 1 }">
            <div v-for="pkCol in primaryKeyColumns" :key="pkCol.columnName" class="edit-pk-item">
              <span class="edit-pk-label">{{ pkCol.displayName || pkCol.columnName }}</span>
              <span class="edit-pk-value">{{ editForm[pkCol.columnName] ?? '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 可编辑字段 -->
        <n-form ref="formRef" :model="editForm" label-placement="left" label-width="auto" require-mark-placement="right-hanging" class="edit-form">
          <n-form-item 
            v-for="col in editableColumns" 
            :key="col.columnName"
          >
            <template #label>
              <span class="edit-field-label">{{ col.displayName || col.columnName }}</span>
              <n-tag v-if="col.dictType" size="tiny" :bordered="false" type="primary" class="edit-type-tag">字典</n-tag>
              <n-tag v-else-if="getFieldType(col.dataType) === 'number'" size="tiny" :bordered="false" type="info" class="edit-type-tag">数字</n-tag>
              <n-tag v-else-if="getFieldType(col.dataType) === 'date'" size="tiny" :bordered="false" type="success" class="edit-type-tag">日期</n-tag>
            </template>
            <!-- 字典类型：下拉选择 -->
            <n-select
              v-if="col.dictType && dictOptionsMap[col.dictType]"
              v-model:value="editForm[col.columnName]"
              :options="dictOptionsMap[col.dictType]"
              :placeholder="'请选择' + (col.displayName || col.columnName)"
              clearable
              filterable
              style="width: 100%"
            />
            <!-- 数字类型 -->
            <n-input-number 
              v-else-if="getFieldType(col.dataType) === 'number'" 
              v-model:value="editForm[col.columnName]" 
              :placeholder="'请输入' + (col.displayName || col.columnName)"
              style="width: 100%"
              clearable
            />
            <!-- 日期类型 -->
            <n-date-picker
              v-else-if="getFieldType(col.dataType) === 'date'"
              v-model:value="editForm[col.columnName]"
              type="datetime"
              :placeholder="'请选择' + (col.displayName || col.columnName)"
              style="width: 100%"
              clearable
            />
            <!-- 长文本类型 -->
            <n-input 
              v-else-if="isLongText(col.dataType)"
              v-model:value="editForm[col.columnName]" 
              :placeholder="'请输入' + (col.displayName || col.columnName)"
              type="textarea"
              :rows="4"
              :maxlength="getMaxLength(col.dataType)"
              show-count
            />
            <!-- 普通文本 -->
            <n-input 
              v-else
              v-model:value="editForm[col.columnName]" 
              :placeholder="'请输入' + (col.displayName || col.columnName)"
              clearable
              :maxlength="getMaxLength(col.dataType)"
            />
          </n-form-item>
        </n-form>
      </n-scrollbar>
      <template #footer>
        <div class="edit-modal-footer">
          <span class="edit-modal-footer-hint">{{ editableColumns.length }} 个可编辑字段</span>
          <n-space :size="12">
            <n-button @click="showEditModal = false">取消</n-button>
            <n-button type="primary" :loading="saving" @click="handleSave">
              <template #icon><n-icon><CreateOutline v-if="isEdit" /><AddOutline v-else /></n-icon></template>
              {{ isEdit ? '保存修改' : '确认新增' }}
            </n-button>
          </n-space>
        </div>
      </template>
    </n-modal>

    <!-- 导入弹窗 -->
    <n-modal v-model:show="showImportModal" preset="card" title="导入数据" style="width: 560px;">
      <n-form label-placement="left" label-width="100px">
        <n-form-item label="导入模式">
          <n-radio-group v-model:value="importMode">
            <n-space>
              <n-radio value="append">
                <div class="import-mode-option">
                  <span class="mode-title">追加导入</span>
                  <span class="mode-desc">直接新增数据，不检查重复</span>
                </div>
              </n-radio>
              <n-radio value="increment">
                <div class="import-mode-option">
                  <span class="mode-title">增量导入</span>
                  <span class="mode-desc">根据主键更新已有数据，新增不存在的数据</span>
                </div>
              </n-radio>
              <n-radio value="replace">
                <div class="import-mode-option">
                  <span class="mode-title">全量替换</span>
                  <span class="mode-desc">清空表后重新导入所有数据</span>
                </div>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="importMode === 'increment'" label="唯一标识字段">
          <n-select
            v-model:value="importUniqueFields"
            :options="columnSelectOptions"
            multiple
            placeholder="选择用于判断数据是否存在的字段"
          />
        </n-form-item>
      </n-form>
      
      <n-alert v-if="importMode === 'replace'" type="warning" style="margin-bottom: 16px;">
        <template #header>警告</template>
        全量替换将清空表中所有现有数据，请谨慎操作！
      </n-alert>
      <n-alert v-else type="info" style="margin-bottom: 16px;">
        <template #header>导入说明</template>
        请确保 Excel 文件的列名与数据表字段对应
      </n-alert>
      
      <n-upload
        ref="uploadRef"
        :action="importUrl"
        :headers="uploadHeaders"
        :data="importParams"
        accept=".xlsx,.xls,.csv"
        :max="1"
        @finish="handleImportFinish"
      >
        <n-upload-dragger>
          <div class="upload-content">
            <n-icon size="48" color="var(--color-primary)"><CloudUploadOutline /></n-icon>
            <p class="upload-title">点击或拖拽文件到此处上传</p>
            <p class="upload-hint">支持 Excel (.xlsx, .xls) 和 CSV 格式</p>
          </div>
        </n-upload-dragger>
      </n-upload>
      <template #footer>
        <n-button @click="showImportModal = false">关闭</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NSpace, NPopconfirm, NTag, NDivider, useMessage } from 'naive-ui'
import {
  AddOutline, SearchOutline, DownloadOutline, CloudUploadOutline,
  CreateOutline, TrashOutline, AlertCircleOutline, GridOutline,
  ListOutline, DocumentTextOutline, FolderOutline, BarChartOutline,
  ServerOutline, SettingsOutline, PeopleOutline, HomeOutline,
  RefreshOutline, CloseCircleOutline
} from '@vicons/ionicons5'
import * as dataViewApi from '@/api/dataView'
import * as tableDataApi from '@/api/tableData'
import { getDictByType } from '@/api/system/dataDictionary'
import type { DataDictionary } from '@/api/system/dataDictionary'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import type { FilterCondition } from '@/types/api'
import { formatDateTime, formatCellValueSmart } from '@/utils/format'
import { exportToExcel } from '@/utils/export'
import { useTabsStore } from '@/stores/tabs'

const tabsStore = useTabsStore()

const route = useRoute()
const message = useMessage()

const loading = ref(true)
const loadingData = ref(false)
const saving = ref(false)
const exporting = ref(false)

const viewConfig = ref<any>(null)
const columns = ref<any[]>([])
const tableData = ref<any[]>([])
const searchKeyword = ref('')
const primaryKeys = ref<string[]>(['id'])  // 支持复合主键
const checkedKeys = ref<any[]>([])
const activeFilters = ref<FilterCondition[]>([])

const showEditModal = ref(false)
const showImportModal = ref(false)
const editTitle = ref('新增数据')
const editForm = reactive<Record<string, any>>({})
const isEdit = ref(false)

// 导入相关
const importMode = ref<'append' | 'increment' | 'replace'>('append')
const importUniqueFields = ref<string[]>([])

// 分页状态
const paginationPage = ref(1)
const paginationPageSize = ref(20)
const paginationItemCount = ref(0)

const iconMap: Record<string, any> = {
  GridOutline, ListOutline, DocumentTextOutline, FolderOutline,
  BarChartOutline, ServerOutline, SettingsOutline, PeopleOutline, HomeOutline
}

const router = useRouter()
const goBack = () => router.back()

const getIconComponent = (name?: string) => iconMap[name || 'GridOutline'] || GridOutline

// 生成复合主键的唯一标识（用于 row-key）
const getRowKey = (row: any): string => {
  if (primaryKeys.value.length === 1) {
    return String(row[primaryKeys.value[0]!] ?? '')
  }
  // 复合主键：将多个主键值用特殊分隔符连接
  return primaryKeys.value.map(pk => String(row[pk] ?? '')).join('|||')
}

// 从复合主键标识解析出各主键值
const parseRowKey = (key: string): Record<string, any> => {
  const values = key.split('|||')
  const result: Record<string, any> = {}
  primaryKeys.value.forEach((pk, index) => {
    result[pk] = values[index]
  })
  return result
}

// 获取行的主键值对象
const getRowPrimaryValues = (row: any): Record<string, any> => {
  const result: Record<string, any> = {}
  primaryKeys.value.forEach(pk => {
    result[pk] = row[pk]
  })
  return result
}

// 筛选字段（根据可见列动态生成）
const filterFields = computed<FilterField[]>(() => {
  return visibleColumns.value.map(col => ({
    label: col.displayName || col.columnName,
    value: col.columnName,
    type: getFieldType(col.dataType)
  }))
})

// 列选择选项（用于导入唯一字段选择）
const columnSelectOptions = computed(() => 
  visibleColumns.value.map(c => ({ 
    label: c.displayName || c.columnName, 
    value: c.columnName 
  }))
)

// 根据数据类型判断筛选类型
const getFieldType = (dataType: string): 'string' | 'number' | 'date' => {
  const type = dataType?.toLowerCase() || ''
  if (type.includes('int') || type.includes('decimal') || type.includes('float') || type.includes('double') || type.includes('number') || type.includes('numeric')) {
    return 'number'
  }
  if (type.includes('date') || type.includes('time')) {
    return 'date'
  }
  return 'string'
}

// 判断是否为长文本
const isLongText = (dataType: string): boolean => {
  const type = dataType?.toLowerCase() || ''
  return type.includes('text') || type.includes('blob') || type.includes('clob')
}

// 获取最大长度
const getMaxLength = (dataType: string): number | undefined => {
  const match = dataType?.match(/\((\d+)\)/)
  return match ? parseInt(match[1]!) : undefined
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const importUrl = computed(() => `/api/tabledata/import`)

const importParams = computed(() => ({
  dataSourceId: viewConfig.value?.dataSourceId,
  tableName: viewConfig.value?.tableName,
  format: 'excel',
  mode: importMode.value,
  uniqueFields: importMode.value === 'increment' ? importUniqueFields.value.join(',') : undefined
}))

// 可见列
const visibleColumns = computed(() => {
  if (!columns.value.length) return []
  return columns.value.filter(c => c.visible)
})

// 可编辑列（排除主键）
const editableColumns = computed(() => {
  return visibleColumns.value.filter(c => !c.isPrimaryKey)
})

// 主键列（支持多主键）
const primaryKeyColumns = computed(() => {
  return columns.value.filter(c => c.isPrimaryKey)
})

// 表格列定义
const tableColumns = computed(() => {
  const cols: any[] = []
  
  // 添加选择列（如果允许删除）
  if (viewConfig.value?.allowDelete) {
    cols.push({
      type: 'selection',
      fixed: 'left',
      width: 50
    })
  }
  
  // 数据列
  visibleColumns.value.forEach(col => {
    cols.push({
      title: col.displayName || col.columnName,
      key: col.columnName,
      ellipsis: { tooltip: true },
      width: 150,
      render: (row: any) => {
        const val = row[col.columnName]
        if (val === null || val === undefined) {
          return h('span', { style: 'color: #999; font-style: italic' }, '-')
        }
        
        // 字典翻译：显示 dictLabel 而不是原始值
        if (col.dictType && dictOptionsMap[col.dictType]) {
          const dictItem = dictOptionsMap[col.dictType]!.find((d: any) => d.value === String(val))
          if (dictItem) {
            return h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => dictItem.label })
          }
        }
        
        // 使用智能格式化函数
        return formatCellValueSmart(val, { 
          fieldName: col.columnName, 
          fieldType: col.dataType,
          fieldTitle: col.displayName 
        })
      }
    })
  })

  // 添加操作列
  if (viewConfig.value?.allowUpdate || viewConfig.value?.allowDelete) {
    cols.push({
      title: '操作',
      key: 'actions',
      width: 180,
      fixed: 'right',
      render: (row: any) => h('div', { style: 'display: flex; gap: 4px; flex-wrap: nowrap; align-items: center;' }, [
        viewConfig.value?.allowUpdate && h(NButton, { 
          size: 'small', 
          type: 'primary',
          secondary: true,
          onClick: () => handleEdit(row)
        }, { 
          icon: () => h(NIcon, { size: 14 }, () => h(CreateOutline)),
          default: () => '编辑'
        }),
        viewConfig.value?.allowDelete && h(NPopconfirm, { 
          onPositiveClick: () => handleDelete(row)
        }, {
          trigger: () => h(NButton, { size: 'small', type: 'error', secondary: true }, { 
            icon: () => h(NIcon, { size: 14 }, () => h(TrashOutline)),
            default: () => '删除'
          }),
          default: () => '确定要删除这条数据吗？'
        })
      ].filter(Boolean))
    })
  }

  return cols
})

// 计算表格滚动宽度
const tableScrollX = computed(() => {
  let totalWidth = 0
  
  // 选择列
  if (viewConfig.value?.allowDelete) {
    totalWidth += 50
  }
  
  // 数据列
  totalWidth += visibleColumns.value.length * 150
  
  // 操作列
  if (viewConfig.value?.allowUpdate || viewConfig.value?.allowDelete) {
    totalWidth += 180
  }
  
  return totalWidth
})

// ==================== 数据字典集成 ====================
const dictOptionsMap: Record<string, Array<{ label: string; value: string }>> = reactive({})

const loadDictOptions = async () => {
  // 找出所有配置了 dictType 的列
  const dictTypes = new Set<string>()
  columns.value.forEach(col => {
    if (col.dictType) dictTypes.add(col.dictType)
  })
  // 批量加载字典数据
  for (const dt of dictTypes) {
    try {
      const res: any = await getDictByType(dt)
      const list = res?.data || []
      dictOptionsMap[dt] = list.map((d: any) => ({
        label: d.dictLabel || d.dictCode,
        value: d.dictValue || d.dictCode,
      }))
    } catch { /* ignore */ }
  }
}

// 加载视图配置
const loadViewConfig = async () => {
  const code = route.params["code"] as string
  if (!code) {
    loading.value = false
    return
  }

  try {
    const res = await dataViewApi.getDataViewByCode(code)
    viewConfig.value = res.data
    
    if (viewConfig.value) {
      // 更新标签页标题
      tabsStore.updateTabTitle(route.fullPath, viewConfig.value.name || '数据管理')
      
      // 解析列配置
      if (viewConfig.value.columnsConfig) {
        columns.value = JSON.parse(viewConfig.value.columnsConfig)
      }
      
      // 找主键（支持复合主键）
      const pkCols = columns.value.filter(c => c.isPrimaryKey)
      if (pkCols.length > 0) {
        primaryKeys.value = pkCols.map(c => c.columnName)
      }
      
      // 加载字典选项（如果列配置了 dictType）
      await loadDictOptions()
      
      // 设置分页
      paginationPageSize.value = viewConfig.value.pageSize || 20
      
      // 加载数据
      await loadData()
    }
  } catch (error) {
    message.error('加载视图配置失败')
  } finally {
    loading.value = false
  }
}

// 已知列名白名单（防止SQL注入）
const isValidColumnName = (field: string): boolean => {
  return columns.value.some(c => c.columnName === field)
}

// 构建筛选条件的 WHERE 子句（仅处理FilterPanel条件，关键词搜索走参数化API）
const buildWhereClause = (): string | undefined => {
  const conditions: string[] = []
  
  // 处理筛选条件
  activeFilters.value.forEach(filter => {
    const { field, operator, value } = filter
    if (!field || !operator) return
    
    // 校验字段名是否在已知列中，防止注入
    if (!isValidColumnName(field)) return
    
    // 转义值中的单引号
    const safeValue = String(value).replace(/'/g, "''")
    
    switch (operator) {
      case 'eq':
        conditions.push(`${field} = '${safeValue}'`)
        break
      case 'ne':
        conditions.push(`${field} != '${safeValue}'`)
        break
      case 'contains':
        conditions.push(`${field} LIKE '%${safeValue}%'`)
        break
      case 'startsWith':
        conditions.push(`${field} LIKE '${safeValue}%'`)
        break
      case 'endsWith':
        conditions.push(`${field} LIKE '%${safeValue}'`)
        break
      case 'gt': {
        const num = Number(safeValue)
        if (!isNaN(num) && isFinite(num)) conditions.push(`${field} > ${num}`)
        break
      }
      case 'gte': {
        const num = Number(safeValue)
        if (!isNaN(num) && isFinite(num)) conditions.push(`${field} >= ${num}`)
        break
      }
      case 'lt': {
        const num = Number(safeValue)
        if (!isNaN(num) && isFinite(num)) conditions.push(`${field} < ${num}`)
        break
      }
      case 'lte': {
        const num = Number(safeValue)
        if (!isNaN(num) && isFinite(num)) conditions.push(`${field} <= ${num}`)
        break
      }
      case 'isNull':
        conditions.push(`${field} IS NULL`)
        break
      case 'isNotNull':
        conditions.push(`${field} IS NOT NULL`)
        break
    }
  })
  
  return conditions.length > 0 ? conditions.join(' AND ') : undefined
}

// 加载数据
const loadData = async () => {
  if (!viewConfig.value || !viewConfig.value.tableName) return
  
  loadingData.value = true
  try {
    const whereClause = buildWhereClause()
    
    // 关键词搜索使用后端参数化查询，避免前端拼接SQL
    const searchCols = searchKeyword.value
      ? visibleColumns.value.filter(col => getFieldType(col.dataType) === 'string').map(col => col.columnName)
      : undefined
    
    const res = await tableDataApi.getTableData({
      dataSourceId: viewConfig.value.dataSourceId,
      tableName: viewConfig.value.tableName,
      page: paginationPage.value,
      pageSize: paginationPageSize.value,
      where: whereClause || '',
      orderBy: viewConfig.value.defaultOrderBy 
        ? `${viewConfig.value.defaultOrderBy} ${viewConfig.value.defaultOrderDir || 'DESC'}`
        : '',
      ...(searchKeyword.value ? { searchKeyword: searchKeyword.value, searchColumns: searchCols as string[] } : {})
    })
    
    tableData.value = res.data?.list || []
    paginationItemCount.value = res.data?.total || 0
  } catch (error: any) {
    message.error(error.message || '加载数据失败')
  } finally {
    loadingData.value = false
  }
}

// 分页处理
const handlePageChange = (page: number) => {
  paginationPage.value = page
  loadData()
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  loadData()
}

// 搜索处理
const handleSearch = () => {
  paginationPage.value = 1
  loadData()
}

// 筛选处理
const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  paginationPage.value = 1
  loadData()
}

const resetSearch = () => {
  searchKeyword.value = ''
  activeFilters.value = []
  paginationPage.value = 1
  loadData()
}

// 移除单个筛选
const removeFilter = (index: number) => {
  activeFilters.value.splice(index, 1)
  paginationPage.value = 1
  loadData()
}

// 获取筛选标签文本
const getFilterLabel = (filter: FilterCondition): string => {
  const col = columns.value.find(c => c.columnName === filter.field)
  const fieldLabel = col?.displayName || filter.field
  const operatorMap: Record<string, string> = {
    'eq': '=', 'ne': '≠', 'contains': '包含', 'startsWith': '开始于',
    'endsWith': '结束于', 'gt': '>', 'gte': '≥', 'lt': '<', 'lte': '≤',
    'isNull': '为空', 'isNotNull': '不为空'
  }
  const op = operatorMap[filter.operator] || filter.operator
  if (filter.operator === 'isNull' || filter.operator === 'isNotNull') {
    return `${fieldLabel} ${op}`
  }
  return `${fieldLabel} ${op} ${filter.value}`
}

const handleCheck = (keys: any[]) => {
  checkedKeys.value = keys
}

const handleAdd = () => {
  isEdit.value = false
  editTitle.value = '新增数据'
  // 清空表单
  Object.keys(editForm).forEach(key => delete editForm[key])
  // 初始化可编辑字段的默认值
  editableColumns.value.forEach(col => {
    editForm[col.columnName] = null
  })
  showEditModal.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  editTitle.value = '编辑数据'
  // 深拷贝避免直接修改原数据
  Object.keys(editForm).forEach(key => delete editForm[key])
  
  // 首先确保所有主键值被复制（无论是否可见，支持复合主键）
  for (const pk of primaryKeys.value) {
    const pkValue = row[pk]
    if (pkValue === undefined || pkValue === null) {
      message.error(`无法编辑：主键"${pk}"值不存在，请确保数据包含主键字段`)
      return
    }
    editForm[pk] = pkValue
  }
  
  // 复制所有可见字段
  visibleColumns.value.forEach(col => {
    const value = row[col.columnName]
    const dataType = col.dataType?.toLowerCase() || ''
    
    // 如果是日期类型，需要特殊处理
    if ((dataType.includes('date') || dataType.includes('time')) && value !== null && value !== undefined) {
      // 如果是数字（时间戳），直接使用
      if (typeof value === 'number') {
        editForm[col.columnName] = value
      } 
      // 如果是字符串，尝试转换为时间戳
      else if (typeof value === 'string') {
        const timestamp = new Date(value).getTime()
        editForm[col.columnName] = isNaN(timestamp) ? null : timestamp
      } 
      else {
        editForm[col.columnName] = null
      }
    } else {
      editForm[col.columnName] = value !== undefined ? value : null
    }
  })
  
  // 再次确保所有主键值存在（以防主键被可见列处理覆盖）
  for (const pk of primaryKeys.value) {
    if (editForm[pk] === undefined || editForm[pk] === null) {
      editForm[pk] = row[pk]
    }
  }
  
  showEditModal.value = true
}

const handleSave = async () => {
  if (!viewConfig.value?.tableName) {
    message.error('配置错误：表名为空')
    return
  }
  
  saving.value = true
  try {
    // 构建提交数据
    const submitData: Record<string, any> = {}
    
    // 收集可编辑字段的值
    editableColumns.value.forEach(col => {
      const value = editForm[col.columnName]
      const dataType = col.dataType?.toLowerCase() || ''
      
      // 处理日期类型：将时间戳转换为字符串
      if ((dataType.includes('date') || dataType.includes('time')) && value !== null && value !== undefined && value !== '') {
        if (typeof value === 'number') {
          // 时间戳转换为字符串
          submitData[col.columnName] = formatDateTime(value, dataType.includes('datetime') || dataType.includes('timestamp') ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
        } else {
          submitData[col.columnName] = value
        }
      }
      // 只提交非空值（但允许数字0和布尔false）
      else if (value !== undefined && value !== null && value !== '') {
        submitData[col.columnName] = value
      } else if (value === 0 || value === false) {
        submitData[col.columnName] = value
      }
    })
    
    if (isEdit.value) {
      // 编辑模式：需要主键（支持复合主键）
      const pkValues: Record<string, any> = {}
      for (const pk of primaryKeys.value) {
        const pkValue = editForm[pk]
        if (pkValue === undefined || pkValue === null || pkValue === '') {
          message.error(`主键"${pk}"值不能为空`)
          return
        }
        pkValues[pk] = pkValue
      }
      
      await tableDataApi.updateRow({
        dataSourceId: viewConfig.value.dataSourceId,
        tableName: viewConfig.value.tableName,
        data: submitData,
        primaryKeys: primaryKeys.value,
        primaryValues: pkValues
      })
      message.success('更新成功')
    } else {
      // 新增模式
      if (Object.keys(submitData).length === 0) {
        message.error('请至少填写一个字段')
        return
      }
      
      await tableDataApi.insertRow({
        dataSourceId: viewConfig.value.dataSourceId,
        tableName: viewConfig.value.tableName,
        data: submitData
      })
      message.success('新增成功')
    }
    showEditModal.value = false
    await loadData()
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row: any) => {
  if (!viewConfig.value?.tableName) {
    message.error('配置错误：表名为空')
    return
  }
  
  // 支持复合主键
  const pkValues: Record<string, any> = {}
  for (const pk of primaryKeys.value) {
    const pkValue = row[pk]
    if (pkValue === undefined || pkValue === null) {
      message.error(`无法删除：主键"${pk}"值为空`)
      return
    }
    pkValues[pk] = pkValue
  }
  
  try {
    await tableDataApi.deleteRow({
      dataSourceId: viewConfig.value.dataSourceId,
      tableName: viewConfig.value.tableName,
      primaryKeys: primaryKeys.value,
      primaryValues: pkValues
    })
    message.success('删除成功')
    await loadData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 批量删除（支持复合主键）
const handleBatchDelete = async () => {
  if (!viewConfig.value?.tableName) {
    message.error('配置错误：表名为空')
    return
  }
  
  if (checkedKeys.value.length === 0) {
    message.warning('请先选择要删除的数据')
    return
  }
  
  try {
    // 将复合主键标识解析为主键值对象数组
    const primaryValuesArray = checkedKeys.value.map(key => parseRowKey(key))
    
    await tableDataApi.batchDeleteRows({
      dataSourceId: viewConfig.value.dataSourceId,
      tableName: viewConfig.value.tableName,
      primaryKeys: primaryKeys.value,
      primaryValuesArray: primaryValuesArray
    })
    message.success(`成功删除 ${checkedKeys.value.length} 条数据`)
    checkedKeys.value = []
    await loadData()
  } catch (error: any) {
    message.error(error.message || '批量删除失败')
  }
}

const handleExport = async () => {
  if (!viewConfig.value?.tableName) {
    message.error('配置错误：表名为空')
    return
  }
  
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  
  exporting.value = true
  try {
    // 只导出可见列，并格式化日期
    const exportData = tableData.value.map(row => {
      const newRow: Record<string, any> = {}
      visibleColumns.value.forEach(col => {
        const val = row[col.columnName]
        const label = col.displayName || col.columnName
        const dataType = col.dataType?.toLowerCase() || ''
        
        // 格式化日期类型
        if ((dataType.includes('date') || dataType.includes('time')) && val !== null && val !== undefined) {
          newRow[label] = formatDateTime(val, dataType.includes('datetime') || dataType.includes('timestamp') ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
        } else {
          newRow[label] = val
        }
      })
      return newRow
    })
    
    const filename = viewConfig.value.name || viewConfig.value.tableName || '数据导出'
    exportToExcel(exportData, filename, 100000, (warning) => {
      message.warning(warning)
    })
    message.success('导出成功')
  } catch (error: any) {
    message.error(error.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const handleImportFinish = ({ event }: any) => {
  try {
    const res = JSON.parse(event.target.response)
    if (res.code === 200) {
      message.success(`导入成功：新增 ${res.data?.insertCount || 0} 条数据`)
      showImportModal.value = false
      loadData()
    } else {
      message.error(res.message || '导入失败')
    }
  } catch (e) {
    message.error('导入失败，请检查文件格式')
  }
}

onMounted(() => {
  loadViewConfig()
})
</script>


<style scoped>
.data-view-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-state p {
  margin: 16px 0 24px;
  color: #64748b;
  font-size: 15px;
}

/* 头部卡片 */
.header-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.title-desc {
  font-size: 13px;
  color: #64748b;
  margin-left: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}

.filter-label {
  font-size: 13px;
  color: #64748b;
  margin-right: 4px;
}

/* 表格卡片 */
.table-card {
  border-radius: 12px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-title {
  font-weight: 600;
  font-size: 16px;
  color: #1e293b;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

/* 上传区域 */
.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
}

.upload-title {
  margin: 12px 0 4px;
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
}

.upload-hint {
  font-size: 13px;
  color: #64748b;
}

/* 导入模式选项 */
.import-mode-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mode-title {
  font-weight: 500;
  color: #1e293b;
}

.mode-desc {
  font-size: 12px;
  color: #64748b;
}

/* === 编辑弹窗美化 === */
.edit-modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.edit-modal-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.edit-modal-icon.icon-edit {
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
  color: #2563eb;
}
.edit-modal-icon.icon-add {
  background: linear-gradient(135deg, #d1fae5, #ecfdf5);
  color: #059669;
}
.edit-modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.edit-modal-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}
.edit-pk-section {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 20px;
}
.edit-section-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: #b45309;
  font-weight: 500;
  margin-bottom: 8px;
}
.edit-pk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 8px;
}
.edit-pk-grid.pk-single {
  grid-template-columns: 1fr;
}
.edit-pk-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 6px;
  padding: 6px 10px;
  border: 1px solid #fde68a;
}
.edit-pk-label {
  font-size: 12px;
  color: #92400e;
  font-weight: 500;
  white-space: nowrap;
}
.edit-pk-value {
  font-size: 13px;
  color: #1e293b;
  font-weight: 600;
  font-family: 'SF Mono', Monaco, monospace;
}
.edit-form {
  padding: 4px 0;
}
.edit-field-label {
  font-weight: 500;
  color: #334155;
}
.edit-type-tag {
  margin-left: 4px;
  transform: scale(0.85);
}
.edit-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.edit-modal-footer-hint {
  font-size: 12px;
  color: #94a3b8;
}

/* 表单样式优化 */
:deep(.n-form-item) {
  margin-bottom: 20px;
}

:deep(.n-form-item-label) {
  font-weight: 500;
}

.edit-form :deep(.n-form-item) {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #f1f5f9;
}
.edit-form :deep(.n-form-item:last-child) {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

/* 表格样式优化 */
:deep(.n-data-table) {
  border-radius: 10px;
  overflow: hidden;
}

:deep(.n-data-table-th) {
  background: #f8fafc !important;
  font-weight: 600;
  color: #475569;
}

:deep(.n-data-table-td) {
  color: #334155;
}

/* 按钮样式 */
:deep(.n-button--primary-type) {
  background: linear-gradient(135deg, var(--color-primary) 0%, #3b82f6 100%);
}

:deep(.n-button--primary-type:hover) {
  background: linear-gradient(135deg, #0052cc 0%, #2563eb 100%);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
  
  .header-actions :deep(.n-input) {
    width: 100% !important;
  }
  
  .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

</style>

<style>
/* DataViewPage 深色模式（非 scoped） */
html.dark .title-text { color: #f1f5f9 !important; }
html.dark .title-desc { color: #94a3b8 !important; }
html.dark .table-title { color: #f1f5f9 !important; }
html.dark .filter-tags { border-top-color: #334155 !important; }
html.dark .filter-label { color: #94a3b8 !important; }
html.dark .pagination-wrapper { border-top-color: #334155 !important; }
html.dark .upload-title { color: #f1f5f9 !important; }
html.dark .upload-hint { color: #94a3b8 !important; }
html.dark .mode-title { color: #f1f5f9 !important; }
html.dark .mode-desc { color: #94a3b8 !important; }
html.dark .edit-modal-title { color: #f1f5f9 !important; }
html.dark .edit-modal-icon.icon-edit {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.2), rgba(37, 99, 235, 0.1)) !important;
  color: #60a5fa !important;
}
html.dark .edit-modal-icon.icon-add {
  background: linear-gradient(135deg, rgba(5, 150, 105, 0.2), rgba(5, 150, 105, 0.1)) !important;
  color: #34d399 !important;
}
html.dark .edit-pk-section {
  background: rgba(245, 158, 11, 0.08) !important;
  border-color: rgba(245, 158, 11, 0.25) !important;
}
html.dark .edit-section-badge { color: #fbbf24 !important; }
html.dark .edit-pk-item {
  background: #1a2536 !important;
  border-color: rgba(245, 158, 11, 0.2) !important;
}
html.dark .edit-pk-label { color: #fbbf24 !important; }
html.dark .edit-pk-value { color: #f1f5f9 !important; }
html.dark .edit-field-label { color: #cbd5e1 !important; }
html.dark .edit-form .n-form-item { border-bottom-color: #334155 !important; }
html.dark .n-data-table-th {
  background: #263348 !important;
  color: #e2e8f0 !important;
}
html.dark .n-data-table-td { color: #cbd5e1 !important; }
</style>
