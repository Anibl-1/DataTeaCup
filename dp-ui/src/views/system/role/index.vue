<template>
  <div class="role-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ShieldCheckmarkOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ paginationItemCount }}</span>
          <span class="stat-label">角色总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><KeyOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ menuTreeData.length }}</span>
          <span class="stat-label">权限菜单</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><PeopleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tableData.length }}</span>
          <span class="stat-label">当前页显示</span>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><ShieldOutline /></n-icon>
            <span>角色管理</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <FilterPanel
            :fields="filterFields"
            :model-value="activeFilters"
            @apply="handleFilterApply"
          />
          <n-button v-permission="'system:role:add'" type="primary" @click="handleCreate">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            新建角色
          </n-button>
        </n-space>
      </template>
      <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索角色名称/编码..."
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
      <n-empty v-if="!loading && tableData.length === 0" description="暂无角色数据" style="margin: 32px 0;" />

      <n-data-table
        v-else
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :scroll-x="1200"
        striped
        class="custom-table"
      />
    <div class="pagination-wrapper">
      <div class="pagination-info">
        <n-tag type="info" size="small" round>
          共 {{ paginationItemCount }} 条记录
        </n-tag>
      </div>
      <n-pagination
        v-model:page="paginationPage"
        v-model:page-size="paginationPageSize"
        :item-count="paginationItemCount"
        :page-sizes="PAGE_SIZES"
        show-size-picker
        show-quick-jumper
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>
    
    <!-- 角色编辑对话框 -->
    <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 600px; border-radius: 16px;">
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="100px"
      >
        <n-form-item label="角色名称" path="roleName">
          <n-input v-model:value="form.roleName" placeholder="请输入角色名称" />
        </n-form-item>
        <n-form-item label="角色编码" path="roleCode">
          <n-input v-model:value="form.roleCode" placeholder="请输入角色编码（如：admin）" :disabled="isEdit" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="form.description"
            type="textarea"
            placeholder="请输入角色描述"
            :rows="3"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 权限分配对话框 -->
    <n-modal v-model:show="showPermissionModal" preset="card" title="权限分配" style="width: 800px; border-radius: 16px;">
      <div style="display: flex; gap: 16px; align-items: flex-start;">
        <!-- 左侧：菜单树 -->
        <div style="flex: 1; min-width: 0;">
          <div style="margin-bottom: 8px;">
            <n-input
              v-model:value="menuSearchKeyword"
              placeholder="搜索菜单名称或权限编码"
              clearable
              size="small"
            >
              <template #prefix>
                <n-icon><SearchOutline /></n-icon>
              </template>
            </n-input>
          </div>
          <div style="margin-bottom: 8px; display: flex; gap: 8px;">
            <n-button size="small" @click="handleSelectAllMenus">全选</n-button>
            <n-button size="small" @click="handleDeselectAllMenus">全不选</n-button>
          </div>
          <n-tree
            v-model:checked-keys="selectedMenus"
            checkable
            cascade
            :data="filteredMenuTreeData"
            :render-label="renderMenuLabel"
            style="max-height: 450px; overflow-y: auto; border: 1px solid var(--border-light); border-radius: 4px; padding: 12px; background: var(--bg-secondary);"
          />
        </div>
        <!-- 右侧：提示信息 -->
        <div style="width: 240px; flex-shrink: 0;">
          <n-alert type="info" :show-icon="true" style="margin-bottom: 0;">
            <template #header>
              <span style="font-weight: 500;">提示</span>
            </template>
            <div class="text-secondary" style="font-size: 13px; line-height: 1.8;">
              <div style="margin-bottom: 8px;">
                • 选择菜单后，角色将自动拥有该菜单对应的页面访问权限
              </div>
              <div>
                • 选择父菜单会自动勾选所有子菜单，取消父菜单会取消所有子菜单
              </div>
            </div>
          </n-alert>
        </div>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPermissionModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmitPermission">保存</n-button>
        </n-space>
      </template>
    </n-modal>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { NButton, NIcon, NSpace, NTree, NAlert, NTag, NInput, NTooltip, NPopconfirm, useMessage, useDialog, type FormInst } from 'naive-ui'
import { AddOutline, ShieldCheckmarkOutline, KeyOutline, PeopleOutline, ShieldOutline, CreateOutline, TrashOutline, SearchOutline } from '@vicons/ionicons5'
import type { PageResult } from '@/types/api'
import { 
  getRoleList, 
  createRole, 
  updateRole, 
  deleteRole,
  assignMenus,
  getRoleMenus
} from '@/api/system/role'
import { getAllMenus } from '@/api/system/menu'
import type { Menu } from '@/types/menu'
import { initMessage } from '@/utils/message'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import type { Role, RoleForm } from '@/types/role'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import type { FilterCondition } from '@/types/api'
import { filtersToApiParam } from '@/utils/filterParams'
import { hasPermission } from '@/utils/permission'

const message = useMessage()
const dialog = useDialog()
initMessage(message)

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Role[]>([])
const showModal = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInst | null>(null)
const allMenus = ref<Menu[]>([])
const menuTreeData = ref<any[]>([])
const selectedMenus = ref<number[]>([])
const activeFilters = ref<FilterCondition[]>([])
const searchKeyword = ref('')
const menuSearchKeyword = ref('')
const showPermissionModal = ref(false)
// 使用独立的 ref 存储分页状态，直接绑定到 n-pagination 组件
const paginationPage = ref(DEFAULT_PAGE)
const paginationPageSize = ref(DEFAULT_PAGE_SIZE)
const paginationItemCount = ref(0)

const filterFields: FilterField[] = [
  { label: 'ID', value: 'id', type: 'number' },
  { label: '角色名称', value: 'roleName', type: 'string' },
  { label: '角色编码', value: 'roleCode', type: 'string' },
  { label: '描述', value: 'description', type: 'string' },
  { label: '创建时间', value: 'createTime', type: 'string' }
]

const form = reactive<RoleForm>({
  roleName: '',
  roleCode: '',
  description: ''
})

const rules = {
  roleName: {
    required: true,
    message: '请输入角色名称',
    trigger: 'blur'
  },
  roleCode: {
    required: true,
    message: '请输入角色编码',
    trigger: 'blur'
  }
}

const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新建角色')

// 不再需要前端筛选，使用后端筛选

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '角色名称', key: 'roleName', width: 120 },
  { title: '角色编码', key: 'roleCode', width: 120 },
  { title: '描述', key: 'description', ellipsis: true, width: 160 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: Role) => h(NTag, {
      type: (row.status ?? 1) === 1 ? 'success' : 'error',
      size: 'small'
    }, { default: () => (row.status ?? 1) === 1 ? '启用' : '禁用' })
  },
  { 
    title: '创建时间', 
    key: 'createTime',
    width: 180,
    render: (row: Role) => row.createTime ? formatDateTime(row.createTime) : '-'
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right' as const,
    render: (row: Role) => {
      const buttons: any[] = []
      
      if (hasPermission('system:role:edit')) {
        buttons.push(
          h(NTooltip, null, {
            trigger: () => h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) },
              { icon: () => h(NIcon, null, { default: () => h(CreateOutline) }) }),
            default: () => '编辑'
          })
        )
      }
      
      if (hasPermission('system:role:edit')) {
        buttons.push(
          h(NTooltip, null, {
            trigger: () => h(NButton, { size: 'small', type: 'info', quaternary: true, onClick: () => handleAssignPermission(row) },
              { icon: () => h(NIcon, null, { default: () => h(ShieldCheckmarkOutline) }) }),
            default: () => '权限分配'
          })
        )
      }
      
      if (hasPermission('system:role:delete') && row.roleCode !== 'admin') {
        buttons.push(
          h(NPopconfirm, { onPositiveClick: () => handleDelete(row) }, {
            trigger: () => h(NButton, { size: 'small', type: 'error', quaternary: true },
              { icon: () => h(NIcon, null, { default: () => h(TrashOutline) }) }),
            default: () => '确定删除该角色吗？'
          })
        )
      }
      
      return h(NSpace, { size: 4, wrap: false }, { default: () => buttons })
    }
  }
]

const loadRoleList = async () => {
  loading.value = true
  try {
    const allFilters = [...activeFilters.value]
    if (searchKeyword.value) {
      allFilters.push({ field: 'keyword', operator: 'like', value: searchKeyword.value })
    }
    const filtersParam = filtersToApiParam(allFilters)
    const res = await getRoleList({
      page: paginationPage.value,
      pageSize: paginationPageSize.value,
      filters: filtersParam
    })
    
    // 根据后端实际返回结构解析数据
    // 后端返回的是 Result<PageResult<T>> 结构
    let pageResult: PageResult<Role> | null = null
    
    // 正确解析响应结构：res.data 应该是 PageResult 对象
    if (res && typeof res === 'object') {
      // 情况1: res.data 直接是 PageResult 对象
      if (res.data && typeof res.data === 'object' && 'list' in res.data && 'total' in res.data) {
        pageResult = res.data as PageResult<Role>
      }
      // 情况2: res 本身就是 PageResult 对象（极少情况）
      else if ('list' in res && 'total' in res) {
        pageResult = res as PageResult<Role>
      }
    }
    
    // 获取列表数据
    const list = pageResult?.list || []
    tableData.value = list
    
    // 关键修复：正确设置总记录数
    // 必须使用后端返回的 total，绝对不能使用列表长度（tableData.length）
    if (pageResult && pageResult.total !== undefined && pageResult.total !== null) {
      // 确保 total 是有效的数字
      const total = typeof pageResult.total === 'number' 
        ? Math.max(0, pageResult.total) // 确保非负
        : (() => {
            const num = Number(pageResult.total)
            return isNaN(num) ? 0 : Math.max(0, num)
          })()
      
      // 直接设置 paginationItemCount.value 为后端返回的 total
      paginationItemCount.value = total
    } else {
      paginationItemCount.value = 0
    }
  } catch (error) {
    const errorMsg = handleApiError(error, '获取角色列表')
    message.error(errorMsg)
    // 加载失败时，如果列表为空，重置 itemCount 为 0
    if (tableData.value.length === 0) {
      paginationItemCount.value = 0
    }
  } finally {
    loading.value = false
  }
}

const loadAllMenus = async () => {
  try {
    const res = await getAllMenus()
    const menus: Menu[] = (res as any).data || []
    allMenus.value = Array.isArray(menus) ? menus : []
    // 构建菜单树
    menuTreeData.value = buildMenuTree(allMenus.value, 0)
  } catch (error) {
    handleApiError(error, '获取菜单列表')
  }
}

// 构建菜单树
const buildMenuTree = (menus: Menu[], parentId: number | null = 0): any[] => {
  if (!Array.isArray(menus) || menus.length === 0) {
    return []
  }
  
  const filtered = menus.filter(menu => {
    if (!menu || typeof menu !== 'object') {
      return false
    }
    const menuParentId = menu.parentId ?? 0
    return menuParentId === parentId
  })
  
  // 按排序顺序排序
  filtered.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  
  return filtered.map(menu => {
    const children = buildMenuTree(menus, menu.id)
    // 构建菜单标签，显示菜单名称和权限代码
    const label = menu.permissionCode 
      ? `${menu.menuName} (${menu.permissionCode})`
      : menu.menuName
    return {
      key: menu.id,
      label: label,
      menuName: menu.menuName,
      permissionCode: menu.permissionCode,
      children: children.length > 0 ? children : undefined
    }
  })
}

// 渲染菜单标签，显示菜单名称和权限代码
const renderMenuLabel = ({ option }: any) => {
  return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
    h('span', { style: 'flex: 1;' }, option.menuName),
    option.permissionCode && h('span', {
      style: 'font-size: 12px; color: var(--text-tertiary); font-family: monospace;'
    }, option.permissionCode)
  ])
}

// 根据搜索关键词过滤菜单树
const filteredMenuTreeData = computed(() => {
  if (!menuSearchKeyword.value) return menuTreeData.value
  const keyword = menuSearchKeyword.value.toLowerCase()
  const filterNodes = (nodes: any[]): any[] => {
    const result: any[] = []
    for (const node of nodes) {
      const nameMatch = node.menuName?.toLowerCase().includes(keyword)
      const codeMatch = node.permissionCode?.toLowerCase().includes(keyword)
      const filteredChildren = node.children ? filterNodes(node.children) : []
      if (nameMatch || codeMatch || filteredChildren.length > 0) {
        result.push({
          ...node,
          children: filteredChildren.length > 0 ? filteredChildren : node.children
        })
      }
    }
    return result
  }
  return filterNodes(menuTreeData.value)
})

// 收集菜单树所有叶子节点的key
const collectAllKeys = (nodes: any[]): number[] => {
  const keys: number[] = []
  const traverse = (items: any[]) => {
    for (const item of items) {
      keys.push(item.key)
      if (item.children?.length) traverse(item.children)
    }
  }
  traverse(nodes)
  return keys
}

// 全选菜单
const handleSelectAllMenus = () => {
  selectedMenus.value = collectAllKeys(menuTreeData.value)
}

// 全不选菜单
const handleDeselectAllMenus = () => {
  selectedMenus.value = []
}

// 权限分配（独立对话框）
const handleAssignPermission = async (row: Role) => {
  isEdit.value = true
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.description = row.description || ''
  menuSearchKeyword.value = ''
  
  try {
    const res = await getRoleMenus(row.id)
    selectedMenus.value = (res.data as unknown) as number[] || []
  } catch (error) {
    handleApiError(error, '获取角色菜单')
  }
  
  showPermissionModal.value = true
}

const handleCreate = () => {
  isEdit.value = false
  form.roleName = ''
  form.roleCode = ''
  form.description = ''
  selectedMenus.value = []
  showModal.value = true
}

const handleEdit = async (row: Role) => {
  isEdit.value = true
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.description = row.description || ''
  
  showModal.value = true
}

const handleDelete = async (row: Role) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除角色"${row.roleName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteRole(row.id)
        message.success('删除成功')
        loadRoleList()
      } catch (error) {
        const errorMsg = handleApiError(error, '删除角色')
        message.error(errorMsg)
      }
    }
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (errors) => {
    if (!errors) {
      submitting.value = true
      try {
        if (isEdit.value) {
          await updateRole(form)
          message.success('更新成功')
        } else {
          await createRole(form)
          message.success('创建成功')
        }
        showModal.value = false
        loadRoleList()
      } catch (error) {
        const errorMsg = handleApiError(error, isEdit.value ? '更新角色' : '创建角色')
        message.error(errorMsg)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleSubmitPermission = async () => {
  if (!form.id) return
  submitting.value = true
  try {
    await assignMenus(form.id, selectedMenus.value)
    message.success('权限分配成功')
    showPermissionModal.value = false
    loadRoleList()
  } catch (error) {
    const errorMsg = handleApiError(error, '权限分配')
    message.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

const handlePageChange = (page: number) => {
  paginationPage.value = page
  loadRoleList()
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  loadRoleList()
}

const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  // 重置到第一页并重新加载数据（后端筛选）
  paginationPage.value = 1
  loadRoleList()
}

const handleSearch = () => {
  paginationPage.value = 1
  loadRoleList()
}

const handleSearchReset = () => {
  searchKeyword.value = ''
  paginationPage.value = 1
  loadRoleList()
}

onMounted(() => {
  loadRoleList()
  loadAllMenus()
})
</script>

<style scoped>
/* 页面容器 */
.role-page {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 130px; flex: 0 0 auto; }
  .main-card { border-radius: 14px !important; }
  .pagination-wrapper { flex-direction: column; gap: 8px; }
}
</style>

