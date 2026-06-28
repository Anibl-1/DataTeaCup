<template>
  <div class="menu-manage-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><MenuOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ menuStats.total }}</span>
          <span class="stat-label">菜单总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><FolderOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ menuStats.directory }}</span>
          <span class="stat-label">目录</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><DocumentTextOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ menuStats.menu }}</span>
          <span class="stat-label">菜单</span>
        </div>
      </div>
    </div>

    <!-- 主内容卡片 -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><MenuOutline /></n-icon>
            <span>菜单列表</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button size="small" @click="handleExpandAll">
            <template #icon><n-icon><ChevronDownOutline /></n-icon></template>
            展开全部
          </n-button>
          <n-button size="small" @click="handleCollapseAll">
            <template #icon><n-icon><ChevronUpOutline /></n-icon></template>
            折叠全部
          </n-button>
          <n-divider vertical />
          <n-button v-permission="'system:menu:add'" type="primary" @click="handleCreate">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增菜单
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchForm.menuName"
            placeholder="搜索菜单名称..."
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
            @clear="handleReset"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchForm.isVisible"
            :options="visibleOptions"
            placeholder="状态"
            clearable
            style="width: 120px"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchForm.menuType"
            :options="menuTypeOptions"
            placeholder="类型"
            clearable
            style="width: 120px"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">
            <template #icon><n-icon><SearchOutline /></n-icon></template>
            搜索
          </n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- Empty State (Req 16.7) -->
      <n-empty v-if="!loading && filteredMenuTree.length === 0" description="暂无菜单数据" style="margin: 32px 0;" />

      <n-data-table
        v-else
        :columns="columns"
        :data="filteredMenuTree"
        :row-key="(row: any) => row.id"
        children-key="children"
        :expanded-row-keys="expandedKeys"
        :loading="loading"
        size="small"
        :bordered="false"
        :scroll-x="900"
        striped
        class="custom-table"
        @update:expanded-row-keys="handleExpandedChange"
      />
    </n-card>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 800px; max-width: 95vw; border-radius: 16px;">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100px">
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="上级菜单" path="parentId">
              <n-tree-select
                v-model:value="form.parentId"
                :options="menuTreeOptions"
                placeholder="选择上级菜单"
                clearable
                default-expand-all
                :render-label="renderTreeLabel"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="菜单类型" path="menuType">
              <n-radio-group v-model:value="form.menuType">
                <n-radio value="directory">目录</n-radio>
                <n-radio value="menu">菜单</n-radio>
              </n-radio-group>
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="菜单名称" path="menuName">
              <n-input v-model:value="form.menuName" placeholder="请输入菜单名称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="显示排序" path="sortOrder">
              <n-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-grid v-if="form.menuType !== 'button'" :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="菜单图标">
              <n-popover trigger="click" placement="bottom" :width="400">
                <template #trigger>
                  <n-input v-model:value="form.icon" placeholder="点击选择图标" readonly>
                    <template #prefix>
                      <n-icon v-if="form.icon" :component="getIconComponent(form.icon)" />
                    </template>
                    <template #suffix>
                      <n-button v-if="form.icon" text size="tiny" @click.stop="form.icon = ''">清除</n-button>
                    </template>
                  </n-input>
                </template>
                <div class="icon-picker">
                  <n-input v-model:value="iconSearch" placeholder="搜索图标" clearable size="small" style="margin-bottom: 12px;">
                    <template #prefix><n-icon><SearchOutline /></n-icon></template>
                  </n-input>
                  <n-scrollbar style="max-height: 280px;">
                    <div class="icon-grid">
                      <div
                        v-for="icon in filteredIcons"
                        :key="icon.value"
                        class="icon-item"
                        :class="{ active: form.icon === icon.value }"
                        @click="form.icon = icon.value"
                      >
                        <n-icon :component="getIconComponent(icon.value)" size="20" />
                        <span>{{ icon.label }}</span>
                      </div>
                    </div>
                  </n-scrollbar>
                </div>
              </n-popover>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="菜单状态">
              <n-radio-group v-model:value="form.isVisible">
                <n-radio :value="1">显示</n-radio>
                <n-radio :value="0">隐藏</n-radio>
              </n-radio-group>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="移动端可见">
              <n-switch v-model:value="mobileVisibleBool" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <template v-if="form.menuType === 'directory'">
          <n-grid :cols="2" :x-gap="16">
            <n-gi>
              <n-form-item label="路由地址">
                <n-input v-model:value="form.routePath" placeholder="可选，一般留空">
                  <template #suffix>
                    <n-tooltip>
                      <template #trigger>
                        <n-icon><HelpCircleOutline /></n-icon>
                      </template>
                      目录一般不需要路由，留空即可
                    </n-tooltip>
                  </template>
                </n-input>
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="权限标识">
                <n-input v-model:value="form.permissionCode" placeholder="可选，如：system:manage" />
              </n-form-item>
            </n-gi>
          </n-grid>
        </template>

        <template v-if="form.menuType === 'menu'">
          <n-grid :cols="2" :x-gap="16">
            <n-gi>
              <n-form-item label="路由地址" path="routePath">
                <n-input v-model:value="form.routePath" placeholder="如：/user 或 /system/menu" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="组件路径">
                <n-input v-model:value="form.componentPath" placeholder="如：@/views/system/user/index.vue" />
              </n-form-item>
            </n-gi>
          </n-grid>
          <n-form-item label="权限标识">
            <n-input v-model:value="form.permissionCode" placeholder="如：system:user:list" disabled>
              <template #suffix>
                <n-tooltip>
                  <template #trigger>
                    <n-icon><HelpCircleOutline /></n-icon>
                  </template>
                  根据路由地址自动生成
                </n-tooltip>
              </template>
            </n-input>
          </n-form-item>
          <n-divider style="margin: 8px 0 12px;">
            <span style="font-size: 13px; color: var(--text-secondary, #94a3b8);">关联资源（可选）</span>
          </n-divider>
          <n-grid :cols="3" :x-gap="16">
            <n-gi>
              <n-form-item label="关联报表">
                <n-select
                  v-model:value="form.reportId"
                  :options="reportOptions"
                  placeholder="选择报表"
                  clearable
                  filterable
                  style="width: 100%"
                />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="关联图表">
                <n-select
                  v-model:value="form.chartId"
                  :options="chartOptions"
                  placeholder="选择图表"
                  clearable
                  filterable
                  style="width: 100%"
                />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="关联页面">
                <n-select
                  v-model:value="form.pageId"
                  :options="pageOptions"
                  placeholder="选择页面"
                  clearable
                  filterable
                  style="width: 100%"
                />
              </n-form-item>
            </n-gi>
          </n-grid>
          <n-grid :cols="3" :x-gap="16">
            <n-gi>
              <n-form-item label="数据视图">
                <n-input v-model:value="form.dataViewCode" placeholder="数据视图编码" clearable />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="打开方式">
                <n-select v-model:value="form.openMode" :options="openModeOptions" placeholder="选择打开方式" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="角标">
                <n-input v-model:value="form.badge" placeholder="如 New、Beta" clearable />
              </n-form-item>
            </n-gi>
          </n-grid>
        </template>

        <template v-if="form.menuType === 'button'">
          <n-grid :cols="2" :x-gap="16">
            <n-gi>
              <n-form-item label="权限标识" path="permissionCode">
                <n-input v-model:value="form.permissionCode" placeholder="如：system:user:add" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="按钮状态">
                <n-radio-group v-model:value="form.isVisible">
                  <n-radio :value="1">启用</n-radio>
                  <n-radio :value="0">禁用</n-radio>
                </n-radio-group>
              </n-form-item>
            </n-gi>
          </n-grid>
        </template>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, h, watch } from 'vue'
import { NButton, NIcon, NTag, NSpace, NPopconfirm, NTooltip, useMessage, type FormInst } from 'naive-ui'
import {
  AddOutline, CreateOutline, TrashOutline, SearchOutline,
  ChevronDownOutline, ChevronUpOutline, HelpCircleOutline,
  MenuOutline, FolderOutline, DocumentTextOutline
} from '@vicons/ionicons5'
import { getAllMenus, createMenu, updateMenu, deleteMenu } from '@/api/system/menu'
import { getPageDefinitionList } from '@/api/page'
import { getChartDefinitionList } from '@/api/chart'
import { getReportDefinitionList } from '@/api/reportDefinition'
import type { Menu, MenuForm } from '@/types/menu'
import { hasPermission } from '@/utils/permission'
import { iconMap as centralIconMap, getIconComponent } from '@/utils/iconMap'

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const modalTitle = ref('新增菜单')
const expandedKeys = ref<number[]>([])
const formRef = ref<FormInst | null>(null)
const iconSearch = ref('')

const menuList = ref<Menu[]>([])
const pageOptions = ref<{ label: string, value: number }[]>([])
const chartOptions = ref<{ label: string, value: number }[]>([])
const reportOptions = ref<{ label: string, value: number }[]>([])
const searchForm = reactive({
  menuName: '',
  isVisible: null as number | null,
  menuType: null as string | null
})

const visibleOptions = [
  { label: '显示', value: 1 },
  { label: '隐藏', value: 0 }
]

const menuTypeOptions = [
  { label: '目录', value: 'directory' },
  { label: '菜单', value: 'menu' }
]

const openModeOptions = [
  { label: '标签页', value: 'tab' },
  { label: '新窗口', value: 'window' },
  { label: '抽屉', value: 'drawer' }
]

// 菜单统计
const menuStats = computed(() => {
  const stats = { total: 0, directory: 0, menu: 0, button: 0 }
  menuList.value.forEach(m => {
    stats.total++
    if (m.menuType === 'directory') stats.directory++
    else if (m.menuType === 'button') stats.button++
    else stats.menu++
  })
  return stats
})

const form = reactive<MenuForm>({
  id: null,
  menuName: '',
  menuCode: '',
  parentId: 0,
  menuType: 'menu',
  routePath: '',
  componentPath: '',
  icon: '',
  sortOrder: 0,
  isVisible: 1,
  mobileVisible: 1,
  permissionCode: '',
  reportId: undefined,
  chartId: undefined,
  pageId: undefined,
  dataViewCode: undefined,
  openMode: 'tab',
  badge: undefined
})

const mobileVisibleBool = computed({
  get: () => form.mobileVisible === 1,
  set: (val: boolean) => { form.mobileVisible = val ? 1 : 0 }
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

// 从中央iconMap生成图标选项列表
const iconOptions = computed(() => {
  return Object.keys(centralIconMap).map(name => ({
    label: name.replace(/Outline$/, ''),
    value: name
  }))
})

const filteredIcons = computed(() => {
  if (!iconSearch.value) return iconOptions.value
  const keyword = iconSearch.value.toLowerCase()
  return iconOptions.value.filter(i => i.label.toLowerCase().includes(keyword) || i.value.toLowerCase().includes(keyword))
})

// 构建菜单树
const buildMenuTree = (list: Menu[], parentId: number = 0): Menu[] => {
  return list
    .filter(item => item.parentId === parentId)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map(item => ({
      ...item,
      children: buildMenuTree(list, item.id!)
    }))
}

// 获取所有菜单ID（用于展开全部）
const getAllMenuIds = (tree: Menu[]): number[] => {
  const ids: number[] = []
  const collect = (items: Menu[]) => {
    items.forEach(item => {
      if (item.id) ids.push(item.id)
      if (item.children && item.children.length > 0) {
        collect(item.children)
      }
    })
  }
  collect(tree)
  return ids
}

// 过滤菜单树（保持树结构完整）
const filterMenuTree = (tree: Menu[], keyword: string, status: number | null, menuType: string | null): Menu[] => {
  return tree.map(item => {
    const children = item.children && item.children.length > 0 
      ? filterMenuTree(item.children, keyword, status, menuType) 
      : []
    
    const nameMatch = !keyword || item.menuName.toLowerCase().includes(keyword.toLowerCase())
    const statusMatch = status === null || item.isVisible === status
    const typeMatch = menuType === null || item.menuType === menuType
    const selfMatch = nameMatch && statusMatch && typeMatch
    
    if (children.length > 0 || selfMatch) {
      return { ...item, children }
    }
    return null
  }).filter(Boolean) as Menu[]
}

// 过滤后的菜单树
const filteredMenuTree = computed(() => {
  const tree = buildMenuTree(menuList.value)
  
  if (!searchForm.menuName && searchForm.isVisible === null && searchForm.menuType === null) {
    return tree
  }
  
  return filterMenuTree(tree, searchForm.menuName, searchForm.isVisible, searchForm.menuType)
})

// 上级菜单选项（只显示目录类型或有子菜单的菜单）
const menuTreeOptions = computed(() => {
  const tree = buildMenuTree(menuList.value)
  
  const hasChildren = (item: Menu): boolean => {
    return !!(item.children && item.children.length > 0)
  }
  
  const buildOptions = (items: Menu[]): any[] => {
    return items
      .filter(item => {
        if (item.menuType === 'directory') return true
        if (hasChildren(item)) return true
        return false
      })
      .map(item => {
        const children = hasChildren(item) ? buildOptions(item.children!) : undefined
        return {
          key: item.id,
          label: item.menuName,
          value: item.id,
          children: children && children.length > 0 ? children : undefined
        }
      })
  }
  
  return [
    { key: 0, label: '顶级菜单', value: 0 },
    ...buildOptions(tree)
  ]
})

// 获取指定父菜单下的最大排序值
const getMaxSortOrder = (parentId: number): number => {
  const children = menuList.value.filter(m => m.parentId === parentId)
  if (children.length === 0) return 0
  return Math.max(...children.map(m => m.sortOrder || 0))
}

// 渲染树选择标签
const renderTreeLabel = ({ option }: { option: any }) => {
  return h('span', {}, option.label)
}

// 表格列定义
const columns = computed(() => [
  {
    title: '菜单名称',
    key: 'menuName',
    minWidth: 220,
    tree: true,
    render: (row: Menu) => {
      return h('div', { style: 'display: inline-flex; align-items: center; gap: 8px;' }, [
        row.icon ? h(NIcon, { component: getIconComponent(row.icon), size: 16, color: '#666' }) : null,
        h('span', {}, row.menuName)
      ])
    }
  },
  {
    title: '类型',
    key: 'menuType',
    width: 80,
    align: 'center',
    render: (row: Menu) => {
      const typeMap: Record<string, { label: string; type: 'info' | 'success' | 'warning' }> = {
        directory: { label: '目录', type: 'info' },
        menu: { label: '菜单', type: 'success' },
        button: { label: '按钮', type: 'warning' }
      }
      const config = typeMap[row.menuType || 'menu'] || typeMap['menu']!
      return h(NTag, { size: 'small', type: config!.type }, () => config!.label)
    }
  },
  {
    title: '图标',
    key: 'icon',
    width: 70,
    align: 'center',
    render: (row: Menu) => {
      if (!row.icon) return h('span', { style: 'color: #ccc;' }, '-')
      const comp = getIconComponent(row.icon)
      if (!comp) return h('span', { style: 'font-size: 12px; color: #999;' }, row.icon)
      return h(NTooltip, {}, {
        trigger: () => h(NIcon, { component: comp, size: 18, color: '#666' }),
        default: () => row.icon
      })
    }
  },
  {
    title: '路由地址',
    key: 'routePath',
    minWidth: 150,
    ellipsis: { tooltip: true },
    render: (row: Menu) => row.routePath 
      ? h('span', { style: 'font-size: 12px; color: #666;' }, row.routePath) 
      : h('span', { style: 'color: #ccc;' }, '-')
  },
  {
    title: '权限标识',
    key: 'permissionCode',
    minWidth: 150,
    ellipsis: { tooltip: true },
    render: (row: Menu) => row.permissionCode 
      ? h('span', { style: 'font-size: 12px; color: #666;' }, row.permissionCode) 
      : h('span', { style: 'color: #ccc;' }, '-')
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 60,
    align: 'center'
  },
  {
    title: '状态',
    key: 'isVisible',
    width: 70,
    align: 'center',
    render: (row: Menu) => h(NTag, { 
      size: 'small', 
      type: row.isVisible === 1 ? 'success' : 'default',
      bordered: false
    }, () => row.isVisible === 1 ? '显示' : '隐藏')
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    fixed: 'right' as const,
    render: (row: Menu) => {
      const buttons: any[] = []
      
      if (hasPermission('system:menu:edit')) {
        buttons.push(
          h(NTooltip, {}, {
            trigger: () => h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) }, {
              icon: () => h(NIcon, null, () => h(CreateOutline))
            }),
            default: () => '编辑'
          })
        )
      }
      
      if (row.menuType === 'directory' && hasPermission('system:menu:add')) {
        buttons.push(
          h(NTooltip, {}, {
            trigger: () => h(NButton, { size: 'small', type: 'info', quaternary: true, onClick: () => handleAddChild(row) }, {
              icon: () => h(NIcon, null, () => h(AddOutline))
            }),
            default: () => '新增子菜单'
          })
        )
      }
      
      if (hasPermission('system:menu:delete')) {
        buttons.push(
          h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id!) }, {
            trigger: () => h(NTooltip, {}, {
              trigger: () => h(NButton, { size: 'small', type: 'error', quaternary: true }, {
                icon: () => h(NIcon, null, () => h(TrashOutline))
              }),
              default: () => '删除'
            }),
            default: () => '确定删除此菜单吗？子菜单也会被删除！'
          })
        )
      }
      
      return h(NSpace, { size: 4, wrap: false }, { default: () => buttons })
    }
  }
])

// 根据路由地址生成权限标识
const generatePermissionCodeFromRoute = (routePath: string): string => {
  if (!routePath) return ''
  return routePath
    .replace(/^\//, '')
    .replace(/\//g, ':')
    .replace(/-/g, '_')
    .toLowerCase()
}

// 监听路由地址变化，自动生成权限标识
watch(() => form.routePath, (newPath) => {
  if (form.menuType === 'menu' && newPath) {
    form.permissionCode = generatePermissionCodeFromRoute(newPath)
  }
})

// 加载菜单数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllMenus()
    menuList.value = (res as any).data || []
    expandedKeys.value = []
  } catch (error: any) {
    message.error('加载菜单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  expandedKeys.value = getAllMenuIds(filteredMenuTree.value)
}

// 重置搜索
const handleReset = () => {
  searchForm.menuName = ''
  searchForm.isVisible = null
  searchForm.menuType = null
}

// 展开所有
const handleExpandAll = () => {
  expandedKeys.value = getAllMenuIds(filteredMenuTree.value)
}

// 折叠所有
const handleCollapseAll = () => {
  expandedKeys.value = []
}

// 处理展开变化
const handleExpandedChange = (keys: number[]) => {
  expandedKeys.value = keys
}

// 重置表单
const resetForm = () => {
  Object.assign(form, {
    id: null,
    menuName: '',
    menuCode: '',
    parentId: 0,
    menuType: 'menu',
    routePath: '',
    componentPath: '',
    icon: '',
    sortOrder: 0,
    isVisible: 1,
    mobileVisible: 1,
    permissionCode: '',
    reportId: undefined,
    chartId: undefined,
    pageId: undefined,
    dataViewCode: undefined,
    openMode: 'tab',
    badge: undefined
  })
  iconSearch.value = ''
}

// 新增菜单
const handleCreate = () => {
  resetForm()
  form.sortOrder = getMaxSortOrder(0) + 1
  modalTitle.value = '新增菜单'
  showModal.value = true
}

// 新增子菜单
const handleAddChild = (parent: Menu) => {
  resetForm()
  form.parentId = parent.id!
  form.menuType = 'menu'
  form.sortOrder = getMaxSortOrder(parent.id!) + 1
  modalTitle.value = '新增子菜单'
  showModal.value = true
}

// 编辑菜单
const handleEdit = (row: Menu) => {
  modalTitle.value = '编辑菜单'
  Object.assign(form, {
    id: row.id,
    menuName: row.menuName,
    menuCode: row.menuCode,
    parentId: row.parentId || 0,
    menuType: row.menuType || 'menu',
    routePath: row.routePath || '',
    componentPath: row.componentPath || '',
    icon: row.icon || '',
    sortOrder: row.sortOrder || 0,
    isVisible: row.isVisible ?? 1,
    mobileVisible: row.mobileVisible ?? 1,
    permissionCode: row.permissionCode || '',
    reportId: row.reportId,
    chartId: row.chartId,
    pageId: row.pageId,
    dataViewCode: row.dataViewCode || undefined,
    openMode: row.openMode || 'tab',
    badge: row.badge || undefined
  })
  showModal.value = true
}

// 删除菜单
const handleDelete = async (id: number) => {
  try {
    await deleteMenu(id)
    message.success('删除成功')
    await loadData()
    window.dispatchEvent(new CustomEvent('menu-updated'))
  } catch (error: any) {
    message.error('删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    submitting.value = true
    
    if (!form.menuCode) {
      form.menuCode = form.menuName.toLowerCase().replace(/[\u4e00-\u9fa5]/g, '').replace(/[^a-z0-9]/g, '_') || `menu_${Date.now()}`
    }
    
    if (form.id) {
      await updateMenu(form)
      message.success('更新成功')
    } else {
      const createData = {
        menuName: form.menuName,
        menuCode: form.menuCode,
        parentId: form.parentId,
        menuType: form.menuType,
        routePath: form.routePath,
        componentPath: form.componentPath,
        icon: form.icon,
        sortOrder: form.sortOrder,
        isVisible: form.isVisible,
        mobileVisible: form.mobileVisible,
        permissionCode: form.permissionCode,
        reportId: form.reportId,
        chartId: form.chartId,
        pageId: form.pageId,
        dataViewCode: form.dataViewCode,
        openMode: form.openMode,
        badge: form.badge
      }
      await createMenu(createData as any)
      message.success('创建成功')
    }
    
    showModal.value = false
    await loadData()
    window.dispatchEvent(new CustomEvent('menu-updated'))
  } catch (error: any) {
    if (error?.message) {
      message.error(error.message)
    }
  } finally {
    submitting.value = false
  }
}

// 加载页面/图表/报表选项
async function loadResourceOptions() {
  try {
    // 加载页面列表
    const pageRes = await getPageDefinitionList({ page: 1, pageSize: 500 }) as any
    const pageData = pageRes?.data?.data || pageRes?.data
    const pages = pageData?.records || pageData?.list || []
    pageOptions.value = pages.map((p: any) => ({
      label: `${p.pageName} (ID: ${p.id})`,
      value: p.id
    }))
    
    // 加载图表列表
    const chartRes = await getChartDefinitionList({ page: 1, pageSize: 500 }) as any
    const chartData = chartRes?.data?.data || chartRes?.data
    const charts = chartData?.records || chartData?.list || []
    chartOptions.value = charts.map((c: any) => ({
      label: `${c.chartName} (ID: ${c.id})`,
      value: c.id
    }))
    
    // 加载报表列表
    const reportRes = await getReportDefinitionList({ page: 1, pageSize: 500 }) as any
    const reportData = reportRes?.data?.data || reportRes?.data
    const reports = reportData?.records || reportData?.list || []
    reportOptions.value = reports.map((r: any) => ({
      label: `${r.reportName} (ID: ${r.id})`,
      value: r.id
    }))
  } catch (e) {
    console.error('加载资源选项失败', e)
  }
}

onMounted(() => {
  loadData()
  loadResourceOptions()
})
</script>

<style scoped>
.menu-manage-page {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 树形表格样式优化 */
:deep(.n-data-table) {
  .n-data-table-indent {
    width: 16px !important;
  }
  
  .n-data-table-expand-trigger {
    margin-right: 4px;
  }
  
  .n-data-table-td {
    padding: 8px 12px !important;
  }
}

/* 表格表头样式 */
:deep(.n-data-table-th) {
  background: var(--bg-tertiary) !important;
  font-weight: 600 !important;
  color: var(--text-secondary) !important;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background-color: var(--bg-hover) !important;
}

/* 图标选择器 */
.icon-picker {
  padding: 8px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.icon-item:hover {
  background: rgba(0, 102, 255, 0.08);
  border-color: var(--color-primary);
}

.icon-item.active {
  background: rgba(0, 102, 255, 0.12);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.icon-item span {
  font-size: 10px;
  color: var(--text-secondary, #6b7280);
  text-align: center;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-item.active span {
  color: var(--color-primary);
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 130px; flex: 0 0 auto; }
  .main-card { border-radius: 14px !important; }
}
</style>
