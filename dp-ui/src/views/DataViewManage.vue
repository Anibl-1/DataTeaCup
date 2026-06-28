<template>
  <div class="data-view-manage-page">
    <!-- 页面头部 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><GridOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dataViewList.length }}</span>
          <span class="stat-label">数据表总数</span>
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
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><MenuOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ menuCount }}</span>
          <span class="stat-label">已生成菜单</span>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><AppsOutline /></n-icon>
            <span>数据表管理</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-button type="primary" @click="handleCreate">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新建数据表
        </n-button>
      </template>

      <div class="dv-toolbar">
        <n-space :size="8" align="center">
          <FilterPanel
            :fields="filterFields"
            :model-value="activeFilters"
            @apply="handleFilterApply"
          />
          <n-input v-model:value="searchKeyword" placeholder="搜索名称..." clearable style="width: 200px" size="small">
            <template #prefix><n-icon><SearchOutline /></n-icon></template>
          </n-input>
        </n-space>
        <n-space v-if="checkedRowKeys.length > 0" :size="6" align="center">
          <n-tag type="info" size="small">已选 {{ checkedRowKeys.length }} 项</n-tag>
          <n-button size="small" type="success" @click="handleBatchEnable">批量启用</n-button>
          <n-button size="small" type="warning" @click="handleBatchDisable">批量禁用</n-button>
          <n-popconfirm @positive-click="handleBatchDelete">
            <template #trigger>
              <n-button size="small" type="error">批量删除</n-button>
            </template>
            确定删除选中的 {{ checkedRowKeys.length }} 个数据表吗？
          </n-popconfirm>
          <n-button size="small" quaternary @click="checkedRowKeys = []">取消</n-button>
        </n-space>
      </div>

      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="filteredList"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: DataView) => row.id"
        :scroll-x="1000"
      />
    </n-card>

    <!-- 新建/编辑弹窗 -->
    <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 800px; max-width: 90vw;">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100px">
        <n-tabs v-model:value="activeTab" type="line">
          <!-- 基本信息 -->
          <n-tab-pane name="basic" tab="基本信息">
            <n-form-item label="名称" path="name">
              <n-input v-model:value="form.name" placeholder="请输入名称" />
            </n-form-item>
            <n-form-item label="编码" path="code">
              <n-input v-model:value="form.code" placeholder="自动生成，可手动修改" :disabled="!!form.id">
                <template #suffix>
                  <n-tooltip trigger="hover">
                    <template #trigger>
                      <span class="text-muted" style="cursor: help;">?</span>
                    </template>
                    根据名称自动生成，用于URL路由
                  </n-tooltip>
                </template>
              </n-input>
            </n-form-item>
            <n-form-item label="数据源" path="dataSourceId">
              <n-select
                v-model:value="form.dataSourceId"
                :options="dataSourceOptions"
                placeholder="请选择数据源"
                @update:value="handleDataSourceChange"
              />
            </n-form-item>
            <n-form-item label="数据表" path="tableName">
              <n-select
                v-model:value="form.tableName"
                :options="tableOptions"
                placeholder="请先选择数据源"
                :disabled="!form.dataSourceId"
                :loading="loadingTables"
                filterable
                @update:value="handleTableChange"
              />
            </n-form-item>
            <n-form-item label="主键字段" path="primaryKeys">
              <n-select
                v-model:value="form.primaryKeys"
                :options="primaryKeyOptions"
                placeholder="选择主键字段（支持复合主键，不选则使用表默认主键）"
                :disabled="!form.tableName"
                multiple
                clearable
                filterable
              />
              <template #feedback>
                <span v-if="tablePrimaryKeys.length > 0" style="color: var(--color-success); font-size: 12px;">
                  表默认主键: {{ tablePrimaryKeys.join(', ') }}
                </span>
                <span v-else-if="form.tableName && tablePrimaryKeys.length === 0" style="color: var(--color-warning); font-size: 12px;">
                  该表无默认主键，请选择至少一个字段作为主键
                </span>
              </template>
            </n-form-item>
            <n-form-item label="描述">
              <n-input v-model:value="form.description" type="textarea" placeholder="可选，描述此数据表的用途" :rows="2" />
            </n-form-item>
            <n-form-item label="状态">
              <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0">
                <template #checked>启用</template>
                <template #unchecked>禁用</template>
              </n-switch>
            </n-form-item>
          </n-tab-pane>

          <!-- 显示列配置 -->
          <n-tab-pane name="columns" tab="显示列配置">
            <div v-if="!form.tableName" class="empty-tip">
              <n-icon size="48" color="#ccc"><GridOutline /></n-icon>
              <p>请先选择数据表</p>
            </div>
            <div v-else>
              <div class="column-config-header">
                <n-checkbox v-model:checked="selectAllColumns" @update:checked="handleSelectAllColumns">全选</n-checkbox>
                <n-button size="small" quaternary @click="resetColumnOrder">重置顺序</n-button>
              </div>
              <n-spin :show="loadingColumns">
                <draggable v-model="form.columns" item-key="columnName" handle=".drag-handle" class="column-list">
                  <template #item="{ element }">
                    <div class="column-item">
                      <span class="drag-handle">⋮⋮</span>
                      <n-checkbox v-model:checked="element.visible" />
                      <span class="column-name">{{ element.columnName }}</span>
                      <n-tag v-if="element.isPrimaryKey" size="small" type="warning">主键</n-tag>
                      <n-tag size="small">{{ element.dataType }}</n-tag>
                      <n-input v-model:value="element.displayName" placeholder="显示名称" size="small" style="width: 120px" />
                      <n-select
                        v-model:value="element.dictType"
                        :options="dictTypeOptions"
                        placeholder="字典类型"
                        size="small"
                        clearable
                        filterable
                        style="width: 120px"
                      />
                    </div>
                  </template>
                </draggable>
              </n-spin>
            </div>
          </n-tab-pane>

          <!-- 功能权限配置 -->
          <n-tab-pane name="permissions" tab="功能权限">
            <div class="permission-section">
              <div class="permission-title">数据操作权限</div>
              <n-grid :cols="2" :x-gap="16" :y-gap="12">
                <n-gi>
                  <div class="permission-item">
                    <n-switch v-model:value="form.allowQuery" :checked-value="1" :unchecked-value="0" />
                    <span class="permission-label">允许查询</span>
                    <span class="permission-desc">用户可以查看数据</span>
                  </div>
                </n-gi>
                <n-gi>
                  <div class="permission-item">
                    <n-switch v-model:value="form.allowInsert" :checked-value="1" :unchecked-value="0" />
                    <span class="permission-label">允许新增</span>
                    <span class="permission-desc">用户可以添加新数据</span>
                  </div>
                </n-gi>
                <n-gi>
                  <div class="permission-item">
                    <n-switch v-model:value="form.allowUpdate" :checked-value="1" :unchecked-value="0" />
                    <span class="permission-label">允许编辑</span>
                    <span class="permission-desc">用户可以修改数据</span>
                  </div>
                </n-gi>
                <n-gi>
                  <div class="permission-item">
                    <n-switch v-model:value="form.allowDelete" :checked-value="1" :unchecked-value="0" />
                    <span class="permission-label">允许删除</span>
                    <span class="permission-desc">用户可以删除数据</span>
                  </div>
                </n-gi>
                <n-gi>
                  <div class="permission-item">
                    <n-switch v-model:value="form.allowImport" :checked-value="1" :unchecked-value="0" />
                    <span class="permission-label">允许导入</span>
                    <span class="permission-desc">用户可以批量导入数据</span>
                  </div>
                </n-gi>
                <n-gi>
                  <div class="permission-item">
                    <n-switch v-model:value="form.allowExport" :checked-value="1" :unchecked-value="0" />
                    <span class="permission-label">允许导出</span>
                    <span class="permission-desc">用户可以导出数据</span>
                  </div>
                </n-gi>
              </n-grid>
            </div>

            <div class="permission-section">
              <div class="permission-title">查询配置</div>
              <n-form-item label="默认排序字段">
                <n-select
                  v-model:value="form.defaultOrderBy"
                  :options="columnSelectOptions"
                  placeholder="选择默认排序字段"
                  clearable
                />
              </n-form-item>
              <n-form-item label="排序方式">
                <n-radio-group v-model:value="form.defaultOrderDir">
                  <n-radio value="ASC">升序</n-radio>
                  <n-radio value="DESC">降序</n-radio>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="每页条数">
                <n-input-number v-model:value="form.pageSize" :min="10" :max="100" :step="10" />
              </n-form-item>
            </div>
          </n-tab-pane>

          <!-- 菜单配置 - 移除，改为外部创建菜单按钮 -->
        </n-tabs>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 创建菜单对话框 -->
    <n-modal v-model:show="showMenuModal" preset="card" title="创建菜单" style="width: 600px">
      <n-form
        ref="menuFormRef"
        :model="menuForm"
        :rules="menuRules"
        label-placement="left"
        label-width="100px"
      >
        <n-form-item label="菜单名称" path="menuName">
          <n-input v-model:value="menuForm.menuName" placeholder="请输入菜单名称" />
        </n-form-item>
        <n-form-item label="父菜单">
          <n-select
            v-model:value="menuForm.parentId"
            :options="parentMenuOptions"
            placeholder="请选择父菜单（不选则为顶级菜单）"
            clearable
            filterable
          />
        </n-form-item>
        <n-form-item label="图标">
          <div class="icon-selector-mini">
            <div 
              v-for="icon in commonIcons" 
              :key="icon.value"
              class="icon-option"
              :class="{ selected: menuForm.icon === icon.value }"
              @click="menuForm.icon = icon.value"
            >
              <n-icon :component="getIconComponent(icon.value)" size="20" />
            </div>
          </div>
        </n-form-item>
        <n-form-item label="排序">
          <n-input-number v-model:value="menuForm.sortOrder" :min="0" style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showMenuModal = false">取消</n-button>
          <n-button type="primary" :loading="creatingMenu" @click="handleCreateMenu">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 表结构预览抽屉 -->
    <n-drawer v-model:show="showStructDrawer" :width="520" placement="right">
      <n-drawer-content :title="structDrawerTitle" closable>
        <template #header-extra>
          <n-tag type="info" size="small">{{ structColumns.length }} 个字段</n-tag>
        </template>
        <n-spin :show="loadingStruct">
          <n-empty v-if="structColumns.length === 0 && !loadingStruct" description="暂无字段信息" style="padding:40px 0;" />
          <n-data-table
            v-else
            :columns="structTableColumns"
            :data="structColumns"
            :pagination="false"
            size="small"
            :max-height="500"
            striped
          />
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, h, watch } from 'vue'
import { NButton, NIcon, NTag, NSpace, NPopconfirm, NTooltip, useMessage, type FormInst } from 'naive-ui'
import draggable from 'vuedraggable'
import {
  GridOutline, AddOutline, SearchOutline, AppsOutline, MenuOutline,
  CheckmarkCircleOutline, CreateOutline, TrashOutline, EyeOutline,
  ServerOutline, DocumentTextOutline, FolderOutline, BarChartOutline,
  SettingsOutline, PeopleOutline, HomeOutline, ListOutline
} from '@vicons/ionicons5'
import * as dataViewApi from '@/api/dataView'
import * as tableDataApi from '@/api/tableData'
import { getAllMenus, getVisibleMenus, createMenu, deleteMenu } from '@/api/system/menu'
import { getDictTypes } from '@/api/system/dataDictionary'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import type { FilterCondition } from '@/types/api'

// 生成编码的工具函数
const generateCode = (name: string): string => {
  if (!name) return ''
  // 转换为拼音或直接使用英文，这里简化处理
  return name
    .toLowerCase()
    .replace(/[\u4e00-\u9fa5]/g, '') // 移除中文
    .replace(/[^a-z0-9]/g, '-') // 非字母数字转为横线
    .replace(/-+/g, '-') // 多个横线合并
    .replace(/^-|-$/g, '') // 移除首尾横线
    || 'view-' + Date.now() // 如果全是中文，用时间戳
}

const message = useMessage()

interface ColumnConfig {
  columnName: string
  displayName: string
  dataType: string
  visible: boolean
  isPrimaryKey: boolean
  sortOrder: number
  dictType?: string
}

interface DataView {
  id?: number
  name: string
  code: string
  dataSourceId: number
  tableName: string
  primaryKeys?: string[]  // 支持复合主键
  description?: string
  status: number
  columns: ColumnConfig[]
  allowQuery: number
  allowInsert: number
  allowUpdate: number
  allowDelete: number
  allowImport: number
  allowExport: number
  defaultOrderBy?: string
  defaultOrderDir: string
  pageSize: number
  generateMenu: number
  menuName?: string
  menuParentId?: number
  menuIcon?: string
  menuSort?: number
  menuId?: number
  createTime?: string
}

// 菜单表单
interface MenuForm {
  menuName: string
  parentId: number
  icon: string
  sortOrder: number
  routePath: string
  componentPath: string
  menuCode: string
  menuType: string
  isVisible: number
  dataViewId?: number
}

const loading = ref(false)
const submitting = ref(false)
const checkedRowKeys = ref<number[]>([])
const showModal = ref(false)
const modalTitle = ref('新建数据管理')
const activeTab = ref('basic')
const formRef = ref<FormInst | null>(null)

const dataViewList = ref<DataView[]>([])
const searchKeyword = ref('')
const dataSources = ref<any[]>([])
const tables = ref<any[]>([])
const loadingTables = ref(false)
const loadingColumns = ref(false)
const menuList = ref<any[]>([])
const showMenuModal = ref(false)
const creatingMenu = ref(false)
const menuFormRef = ref<FormInst | null>(null)
const currentDataView = ref<DataView | null>(null)
const activeFilters = ref<FilterCondition[]>([])
const dictTypeOptions = ref<Array<{ label: string; value: string }>>([])

// 菜单表单
const menuForm = reactive<MenuForm>({
  menuName: '',
  parentId: 0,
  icon: 'GridOutline',
  sortOrder: 0,
  routePath: '',
  componentPath: '@/views/DataViewPage.vue',
  menuCode: '',
  menuType: 'menu',
  isVisible: 1
})

const menuRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

// 筛选字段配置
const filterFields: FilterField[] = [
  { label: '名称', value: 'name', type: 'string' },
  { label: '编码', value: 'code', type: 'string' },
  { label: '数据表', value: 'tableName', type: 'string' },
  { label: '状态', value: 'status', type: 'number' }
]

const form = reactive<DataView>({
  name: '',
  code: '',
  dataSourceId: 0,
  tableName: '',
  primaryKeys: [] as string[],
  description: '',
  status: 1,
  columns: [],
  allowQuery: 1,
  allowInsert: 1,
  allowUpdate: 1,
  allowDelete: 1,
  allowImport: 1,
  allowExport: 1,
  defaultOrderBy: '',
  defaultOrderDir: 'DESC',
  pageSize: 20,
  generateMenu: 0,
  menuName: '',
  menuParentId: 0,
  menuIcon: 'GridOutline',
  menuSort: 0
})

const rules = {
  name: [{ required: true, message: '请输入视图名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入视图编码', trigger: 'blur' }],
  dataSourceId: [{ required: true, type: 'number', message: '请选择数据源', trigger: 'change' }],
  tableName: [{ required: true, message: '请选择数据表', trigger: 'change' }]
}

const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page: number) => { pagination.page = page },
  onUpdatePageSize: (pageSize: number) => { pagination.pageSize = pageSize; pagination.page = 1 }
})

// 常用图标
const commonIcons = [
  { value: 'GridOutline', label: '网格' },
  { value: 'ListOutline', label: '列表' },
  { value: 'DocumentTextOutline', label: '文档' },
  { value: 'FolderOutline', label: '文件夹' },
  { value: 'BarChartOutline', label: '图表' },
  { value: 'ServerOutline', label: '服务器' },
  { value: 'SettingsOutline', label: '设置' },
  { value: 'PeopleOutline', label: '用户' },
  { value: 'HomeOutline', label: '首页' }
]

const iconMap: Record<string, any> = {
  GridOutline, ListOutline, DocumentTextOutline, FolderOutline,
  BarChartOutline, ServerOutline, SettingsOutline, PeopleOutline, HomeOutline
}

const getIconComponent = (name: string) => iconMap[name] || GridOutline

// 计算属性
const enabledCount = computed(() => dataViewList.value.filter(v => v.status === 1).length)
const menuCount = computed(() => dataViewList.value.filter(v => v.menuId).length)

const filteredList = computed(() => {
  let list = dataViewList.value
  
  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(v => 
      v.name.toLowerCase().includes(keyword) || 
      v.code.toLowerCase().includes(keyword)
    )
  }
  
  // 应用筛选条件
  if (activeFilters.value.length > 0) {
    list = list.filter(item => {
      return activeFilters.value.every(filter => {
        const value = (item as any)[filter.field]
        const filterValue = filter.value
        
        switch (filter.operator) {
          case 'eq': return value == filterValue
          case 'ne': return value != filterValue
          case 'contains': return String(value).toLowerCase().includes(String(filterValue).toLowerCase())
          case 'startsWith': return String(value).toLowerCase().startsWith(String(filterValue).toLowerCase())
          case 'endsWith': return String(value).toLowerCase().endsWith(String(filterValue).toLowerCase())
          case 'gt': return Number(value) > Number(filterValue)
          case 'gte': return Number(value) >= Number(filterValue)
          case 'lt': return Number(value) < Number(filterValue)
          case 'lte': return Number(value) <= Number(filterValue)
          case 'isNull': return value === null || value === undefined || value === ''
          case 'isNotNull': return value !== null && value !== undefined && value !== ''
          default: return true
        }
      })
    })
  }
  
  return list
})

const dataSourceOptions = computed(() => 
  dataSources.value.map(ds => ({ label: ds.name, value: ds.id }))
)

const tableOptions = computed(() => 
  tables.value.map(t => ({ label: t.tableName + (t.remarks ? ` (${t.remarks})` : ''), value: t.tableName }))
)

const columnSelectOptions = computed(() => 
  form.columns.filter(c => c.visible).map(c => ({ label: c.displayName || c.columnName, value: c.columnName }))
)

// 主键选项（所有列都可以作为主键）
const primaryKeyOptions = computed(() => 
  form.columns.map(c => ({ 
    label: c.columnName + (c.isPrimaryKey ? ' (表主键)' : ''), 
    value: c.columnName 
  }))
)

// 表的默认主键（支持复合主键）
const tablePrimaryKeys = computed(() => {
  return form.columns.filter(c => c.isPrimaryKey).map(c => c.columnName)
})

const selectAllColumns = computed({
  get: () => form.columns.length > 0 && form.columns.every(c => c.visible),
  set: (val) => form.columns.forEach(c => c.visible = val)
})

// 扁平化的父菜单选项（只显示目录类型或有子菜单的菜单）
const parentMenuOptions = computed(() => {
  if (!menuList.value || menuList.value.length === 0) return []
  
  // 扁平化菜单列表
  const flattenMenus = (items: any[]): any[] => {
    const result: any[] = []
    const flatten = (list: any[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          flatten(item.children)
        }
      })
    }
    flatten(items)
    return result
  }
  
  const flatList = flattenMenus(menuList.value)
  
  // 先找出所有有子菜单的菜单ID
  const parentIds = new Set(flatList.filter(m => m && m.parentId).map(m => m.parentId))
  
  // 只保留目录类型或有子菜单的菜单
  const directoryMenus = flatList.filter(m => {
    if (m.menuType === 'directory') return true
    if (parentIds.has(m.id)) return true
    return false
  })
  
  return directoryMenus
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map(m => ({
      label: m.menuName,
      value: m.id
    }))
})

// 获取当前最大排序值
const maxMenuSort = computed(() => {
  if (!menuList.value || menuList.value.length === 0) return 0
  
  const flattenMenus = (items: any[]): any[] => {
    const result: any[] = []
    const flatten = (list: any[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          flatten(item.children)
        }
      })
    }
    flatten(items)
    return result
  }
  
  const flatList = flattenMenus(menuList.value)
  return Math.max(0, ...flatList.map(m => m.sortOrder || 0))
})

// 获取指定父菜单下的最大排序值
const getMaxSortOrder = (parentId: number): number => {
  const flattenMenus = (items: any[]): any[] => {
    const result: any[] = []
    const flatten = (list: any[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          flatten(item.children)
        }
      })
    }
    flatten(items)
    return result
  }
  const flatList = flattenMenus(menuList.value)
  const children = flatList.filter(m => m.parentId === parentId)
  if (children.length === 0) return 0
  return Math.max(...children.map(m => m.sortOrder || 0))
}

// 监听父菜单变化，自动更新排序值
watch(() => menuForm.parentId, (newParentId) => {
  if (newParentId !== undefined) {
    menuForm.sortOrder = getMaxSortOrder(newParentId || 0) + 1
  }
})

// 表格列定义
const columns = [
  { type: 'selection' as const, width: 48 },
  { title: '名称', key: 'name', width: 150 },
  { title: '编码', key: 'code', width: 150 },
  { 
    title: '数据表', 
    key: 'tableName',
    width: 150,
    render: (row: DataView) => h('span', {}, row.tableName)
  },
  {
    title: '主键',
    key: 'primaryKeys',
    width: 150,
    render: (row: DataView) => {
      // 从列配置中找所有主键（支持复合主键）
      const pks = row.columns?.filter(c => c.isPrimaryKey).map(c => c.columnName) || []
      if (pks.length === 0) {
        return h(NTag, { size: 'small', type: 'default' }, () => '无')
      }
      if (pks.length === 1) {
        return h(NTag, { size: 'small', type: 'info' }, () => pks[0])
      }
      // 多个主键用逗号分隔显示
      return h(NTooltip, {}, {
        trigger: () => h(NTag, { size: 'small', type: 'warning' }, () => `复合主键(${pks.length})`),
        default: () => pks.join(', ')
      })
    }
  },
  {
    title: '功能权限',
    key: 'permissions',
    width: 180,
    render: (row: DataView) => {
      const tags: any[] = []
      if (row.allowQuery) tags.push(h(NTag, { size: 'small', type: 'info' }, () => '查'))
      if (row.allowInsert) tags.push(h(NTag, { size: 'small', type: 'success' }, () => '增'))
      if (row.allowUpdate) tags.push(h(NTag, { size: 'small', type: 'warning' }, () => '改'))
      if (row.allowDelete) tags.push(h(NTag, { size: 'small', type: 'error' }, () => '删'))
      if (row.allowImport) tags.push(h(NTag, { size: 'small' }, () => '导入'))
      if (row.allowExport) tags.push(h(NTag, { size: 'small' }, () => '导出'))
      return tags.length > 0 ? h(NSpace, { size: 4 }, () => tags) : h('span', { style: 'color: #999' }, '无')
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: DataView) => h(NTag, { type: row.status === 1 ? 'success' : 'default', size: 'small' }, 
      () => row.status === 1 ? '启用' : '禁用')
  },
  {
    title: '菜单',
    key: 'menuId',
    width: 80,
    render: (row: DataView) => {
      const existingMenu = getDataViewMenu(row.code)
      return h(NTag, { type: existingMenu ? 'info' : 'default', size: 'small' }, 
        () => existingMenu ? '已生成' : '未生成')
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 380,
    fixed: 'right',
    render: (row: DataView) => {
      const existingMenu = getDataViewMenu(row.code)
      const buttons = []
      
      // 表结构按钮
      buttons.push(
        h(NTooltip, {}, {
          trigger: () => h(NButton, { size: 'small', quaternary: true, onClick: () => handleViewStruct(row) }, {
            icon: () => h(NIcon, null, () => h(ListOutline)),
          }),
          default: () => '查看表结构'
        })
      )
      
      // 预览按钮
      buttons.push(
        h(NButton, { size: 'small', onClick: () => handlePreview(row) }, { 
          icon: () => h(NIcon, null, () => h(EyeOutline)),
          default: () => '预览'
        })
      )
      
      // 编辑按钮
      buttons.push(
        h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { 
          icon: () => h(NIcon, null, () => h(CreateOutline)),
          default: () => '编辑'
        })
      )
      
      // 菜单操作按钮
      if (existingMenu) {
        buttons.push(
          h(NPopconfirm, { onPositiveClick: () => handleDeleteMenuById(existingMenu.id) }, {
            trigger: () => h(NButton, { size: 'small', type: 'warning' }, { 
              icon: () => h(NIcon, null, () => h(MenuOutline)),
              default: () => '删除菜单'
            }),
            default: () => '确定删除关联的菜单吗？'
          })
        )
      } else {
        buttons.push(
          h(NButton, { size: 'small', type: 'info', onClick: () => handleCreateMenuForDataView(row) }, { 
            icon: () => h(NIcon, null, () => h(MenuOutline)),
            default: () => '创建菜单'
          })
        )
      }
      
      // 删除按钮
      buttons.push(
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id!) }, {
          trigger: () => h(NButton, { size: 'small', type: 'error' }, { 
            icon: () => h(NIcon, null, () => h(TrashOutline)),
            default: () => '删除'
          }),
          default: () => '确定删除此数据表配置吗？'
        })
      )
      
      return h('div', { 
        style: 'display: flex; gap: 4px; flex-wrap: nowrap; align-items: center;' 
      }, buttons)
    }
  }
]

// 方法
const loadDataViews = async () => {
  loading.value = true
  try {
    const res = await dataViewApi.getDataViewList()
    const list = res.data || []
    // 解析 columnsConfig 字符串为 columns 数组
    dataViewList.value = list.map((item: any) => {
      if (item.columnsConfig && typeof item.columnsConfig === 'string') {
        try {
          item.columns = JSON.parse(item.columnsConfig)
        } catch (e) {
          item.columns = []
        }
      }
      return item
    })
  } catch (error: any) {
    message.error('加载数据管理失败')
  } finally {
    loading.value = false
  }
}

const loadDataSources = async () => {
  try {
    const res = await tableDataApi.getDataSources()
    const pageData = res.data || {}
    dataSources.value = pageData.list || pageData || []
  } catch (error) {
    message.error('加载数据源失败')
  }
}

const loadMenus = async () => {
  try {
    let res: any
    try {
      res = await getAllMenus(true)
    } catch {
      res = await getVisibleMenus()
    }
    // res 是 ApiResponse<Menu[]>，需要取 data 属性
    menuList.value = (res as any).data || []
  } catch (error) {
    console.warn('菜单加载失败，不影响主功能', error)
  }
}

const handleDataSourceChange = async () => {
  form.tableName = ''
  form.columns = []
  tables.value = []
  
  if (!form.dataSourceId) return
  
  loadingTables.value = true
  try {
    const res = await tableDataApi.getTables(form.dataSourceId)
    tables.value = res.data || []
  } catch (error) {
    message.error('加载表列表失败')
  } finally {
    loadingTables.value = false
  }
}

const handleTableChange = async () => {
  form.columns = []
  if (!form.tableName || !form.dataSourceId) return
  
  loadingColumns.value = true
  try {
    const res = await tableDataApi.getTableStructure(form.dataSourceId, form.tableName)
    const cols = res.data || []
    form.columns = cols.map((col: any, index: number) => ({
      columnName: col.columnName,
      displayName: col.remarks || col.columnName,
      dataType: col.dataType,
      visible: true,
      isPrimaryKey: col.isPrimaryKey,
      sortOrder: index
    }))
    
    // 自动设置菜单名称
    if (!form.menuName) {
      form.menuName = form.name || form.tableName
    }
  } catch (error) {
    message.error('加载表结构失败')
  } finally {
    loadingColumns.value = false
  }
}

const handleSelectAllColumns = (checked: boolean) => {
  form.columns.forEach(c => c.visible = checked)
}

const resetColumnOrder = () => {
  form.columns.sort((a, b) => {
    if (a.isPrimaryKey && !b.isPrimaryKey) return -1
    if (!a.isPrimaryKey && b.isPrimaryKey) return 1
    return 0
  })
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    name: '',
    code: '',
    dataSourceId: 0,
    tableName: '',
    primaryKeys: [],
    description: '',
    status: 1,
    columns: [],
    allowQuery: 1,
    allowInsert: 1,
    allowUpdate: 1,
    allowDelete: 1,
    allowImport: 1,
    allowExport: 1,
    defaultOrderBy: '',
    defaultOrderDir: 'DESC',
    pageSize: 20,
    generateMenu: 0,
    menuName: '',
    menuParentId: 0,
    menuIcon: 'GridOutline',
    menuSort: 0
  })
  tables.value = []
  activeTab.value = 'basic'
}

const handleCreate = () => {
  resetForm()
  modalTitle.value = '新建数据表'
  showModal.value = true
}

const handleEdit = async (row: DataView) => {
  modalTitle.value = '编辑数据表'
  
  // 先保存原始值
  const savedTableName = row.tableName
  const savedColumns = row.columns
  // 获取所有主键字段（支持复合主键）
  const savedPrimaryKeys = row.columns?.filter(c => c.isPrimaryKey).map(c => c.columnName) || []
  
  // 复制数据到表单，排除日期字段避免解析错误
  const { createTime, updateTime, ...rowData } = row as any
  Object.assign(form, { ...rowData })
  
  // 加载表列表
  if (form.dataSourceId) {
    loadingTables.value = true
    try {
      const res = await tableDataApi.getTables(form.dataSourceId)
      tables.value = res.data || []
    } catch (error) {
      message.error('加载表列表失败')
    } finally {
      loadingTables.value = false
    }
    
    // 恢复表名
    form.tableName = savedTableName
    
    // 恢复已保存的列配置
    if (savedColumns && savedColumns.length > 0) {
      form.columns = savedColumns
      // 恢复主键选择（支持复合主键）
      if (savedPrimaryKeys.length > 0) {
        form.primaryKeys = savedPrimaryKeys
      }
    }
  }
  
  activeTab.value = 'basic'
  showModal.value = true
}

const handlePreview = (row: DataView) => {
  window.open(`/data-view/${row.code}`, '_blank')
}

// 表结构预览抽屉
const showStructDrawer = ref(false)
const structDrawerTitle = ref('')
const structColumns = ref<any[]>([])
const loadingStruct = ref(false)
const structTableColumns = [
  { title: '字段名', key: 'columnName', width: 140 },
  { title: '显示名', key: 'displayName', width: 120 },
  { title: '数据类型', key: 'dataType', width: 100 },
  { title: '主键', key: 'isPrimaryKey', width: 60, render: (row: any) => row.isPrimaryKey ? h(NTag, { size: 'small', type: 'warning' }, () => '是') : '-' },
  { title: '可见', key: 'visible', width: 60, render: (row: any) => row.visible ? h(NTag, { size: 'small', type: 'success' }, () => '是') : h(NTag, { size: 'small', type: 'default' }, () => '否') }
]
const handleViewStruct = async (row: DataView) => {
  structDrawerTitle.value = `表结构 - ${row.tableName}`
  showStructDrawer.value = true
  if (row.columns && row.columns.length > 0) {
    structColumns.value = row.columns
  } else {
    loadingStruct.value = true
    try {
      const { getTableColumns } = await import('@/api/dataSource')
      const res = await getTableColumns(row.dataSourceId, row.tableName)
      structColumns.value = (res as any).data || res || []
    } catch {
      structColumns.value = []
    } finally {
      loadingStruct.value = false
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    // 验证主键：必须有自定义主键或表默认主键
    const effectivePrimaryKeys = (form.primaryKeys && form.primaryKeys.length > 0) 
      ? form.primaryKeys 
      : tablePrimaryKeys.value
    if (!effectivePrimaryKeys || effectivePrimaryKeys.length === 0) {
      message.error('请选择至少一个主键字段，或确保数据表有默认主键')
      activeTab.value = 'basic'
      return
    }
    
    submitting.value = true
    
    // 将主键信息保存到列配置中（不存数据库单独字段）
    // 如果用户选择了自定义主键，更新列配置中的 isPrimaryKey 标记
    if (form.primaryKeys && form.primaryKeys.length > 0) {
      form.columns.forEach(col => {
        col.isPrimaryKey = form.primaryKeys!.includes(col.columnName)
      })
    }
    
    // 创建提交数据，移除 primaryKeys 字段（数据库没有这个字段，主键信息保存在 columnsConfig 中）
    const submitData = { ...form }
    delete (submitData as any).primaryKeys
    
    if (form.id) {
      await dataViewApi.updateDataView(form.id, submitData)
      message.success('更新成功')
    } else {
      await dataViewApi.createDataView(submitData)
      message.success('创建成功')
    }
    
    showModal.value = false
    await loadDataViews()
  } catch (error: any) {
    if (error.message) {
      message.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await dataViewApi.deleteDataView(id)
    message.success('删除成功')
    await loadDataViews()
  } catch (error) {
    message.error('删除失败')
  }
}

const handleBatchEnable = async () => {
  let ok = 0
  for (const id of checkedRowKeys.value) {
    const dv = dataViewList.value.find(v => v.id === id)
    if (dv && dv.status !== 1) {
      try { await dataViewApi.updateDataView(id, { ...dv, status: 1 }); ok++ } catch { /* skip */ }
    }
  }
  message.success(`已启用 ${ok} 项`)
  checkedRowKeys.value = []
  await loadDataViews()
}

const handleBatchDisable = async () => {
  let ok = 0
  for (const id of checkedRowKeys.value) {
    const dv = dataViewList.value.find(v => v.id === id)
    if (dv && dv.status !== 0) {
      try { await dataViewApi.updateDataView(id, { ...dv, status: 0 }); ok++ } catch { /* skip */ }
    }
  }
  message.success(`已禁用 ${ok} 项`)
  checkedRowKeys.value = []
  await loadDataViews()
}

const handleBatchDelete = async () => {
  let ok = 0
  for (const id of checkedRowKeys.value) {
    try { await dataViewApi.deleteDataView(id); ok++ } catch { /* skip */ }
  }
  message.success(`已删除 ${ok} 项`)
  checkedRowKeys.value = []
  await loadDataViews()
}

// 筛选应用
const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
}

// 创建菜单相关
const handleCreateMenuForDataView = (dataView: DataView) => {
  currentDataView.value = dataView
  menuForm.menuName = dataView.name
  menuForm.menuCode = `dataview_${dataView.code}`
  menuForm.routePath = `/data-view/${dataView.code}`
  menuForm.componentPath = '@/views/DataViewPage.vue'
  menuForm.dataViewId = dataView.id ?? 0
  menuForm.parentId = 0
  menuForm.sortOrder = maxMenuSort.value + 1
  menuForm.icon = 'GridOutline'
  showMenuModal.value = true
}

const handleCreateMenu = async () => {
  if (!menuFormRef.value || !currentDataView.value) return
  
  try {
    await menuFormRef.value.validate()
    creatingMenu.value = true
    
    await createMenu({
      menuName: menuForm.menuName,
      menuCode: menuForm.menuCode,
      parentId: menuForm.parentId,
      menuType: 'menu',
      routePath: menuForm.routePath,
      componentPath: menuForm.componentPath,
      icon: menuForm.icon,
      sortOrder: menuForm.sortOrder,
      isVisible: 1
    })
    
    message.success('菜单创建成功！')
    showMenuModal.value = false
    
    // 重置表单
    menuForm.menuName = ''
    menuForm.menuCode = ''
    menuForm.routePath = ''
    menuForm.parentId = 0
    menuForm.sortOrder = 0
    menuForm.icon = 'GridOutline'
    menuForm.dataViewId = 0
    currentDataView.value = null
    
    await loadMenus()
    await loadDataViews()
  } catch (error: any) {
    message.error(error.message || '创建菜单失败')
  } finally {
    creatingMenu.value = false
  }
}

// 通过菜单ID删除菜单
const handleDeleteMenuById = async (menuId: number) => {
  try {
    await deleteMenu(menuId)
    message.success('菜单删除成功')
    await loadMenus()
    await loadDataViews()
  } catch (error: any) {
    message.error(error.message || '删除菜单失败')
  }
}

// 获取数据管理对应的菜单（通过 routePath 匹配）
const getDataViewMenu = (code: string): any | undefined => {
  const routePath = `/data-view/${code}`
  
  // 扁平化菜单列表
  const flattenMenus = (items: any[]): any[] => {
    const result: any[] = []
    const flatten = (list: any[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          flatten(item.children)
        }
      })
    }
    flatten(items)
    return result
  }
  
  const flatList = flattenMenus(menuList.value)
  return flatList.find(m => m.routePath === routePath)
}

// 监听视图名称变化，自动生成编码
watch(() => form.name, (newName) => {
  // 只在新建时自动生成，编辑时不覆盖
  if (!form.id && newName) {
    form.code = generateCode(newName) || 'view-' + Date.now()
    // 同时设置菜单名称
    if (!form.menuName) {
      form.menuName = newName
    }
  }
})

const loadDictTypes = async () => {
  try {
    const res: any = await getDictTypes()
    const types = res?.data || []
    dictTypeOptions.value = types.map((t: string) => ({ label: t, value: t }))
  } catch { /* ignore */ }
}

onMounted(() => {
  loadDataViews()
  loadDataSources()
  loadMenus()
  loadDictTypes()
})
</script>

<style scoped>
/* 使用全局 page-common.css 统一样式 */

.dv-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-tip { text-align: center; padding: 40px; color: var(--text-secondary, #999); }
.empty-tip p { margin-top: 12px; }

.column-config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light, #eee);
}

.column-list { max-height: 300px; overflow-y: auto; }

.column-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--bg-tertiary, #f9fafb);
  border-radius: 8px;
  margin-bottom: 8px;
}

.drag-handle { cursor: move; color: var(--text-secondary, #999); user-select: none; }
.column-name { flex: 1; font-weight: 500; }

.permission-section {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--bg-tertiary, #f9fafb);
  border-radius: 12px;
}

.permission-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 16px;
  color: var(--text-primary, #1e293b);
}

.permission-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-radius: 8px;
}

.permission-label { font-weight: 500; min-width: 70px; }
.permission-desc { font-size: 12px; color: var(--text-secondary, #64748b); }

.icon-selector-mini {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.icon-option {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border-light, #e2e8f0);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-option:hover { border-color: var(--color-primary); background: rgba(0, 102, 255, 0.05); }
.icon-option.selected { border-color: var(--color-primary); background: rgba(0, 102, 255, 0.1); color: var(--color-primary); }

</style>

<style>
/* DataViewManage 深色模式（非 scoped） */
html.dark .view-card.selected { border-color: #818cf8 !important; }
html.dark .view-name { color: #818cf8 !important; }
</style>
