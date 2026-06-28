<template>
  <div class="user-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><PeopleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ paginationItemCount }}</span>
          <span class="stat-label">用户总数</span>
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
          <n-icon size="24"><ShieldCheckmarkOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ allRoles.length }}</span>
          <span class="stat-label">角色数量</span>
        </div>
      </div>
    </div>

    <!-- 主内容区: 左侧部门树 + 右侧表格 -->
    <div class="content-layout">
      <!-- 左侧部门树筛选 -->
      <n-card class="dept-tree-card" :bordered="true" size="small">
        <template #header>
          <div style="display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600;">
            <n-icon size="16" color="var(--color-primary)"><BusinessOutline /></n-icon>
            <span>部门筛选</span>
          </div>
        </template>
        <n-input
          v-model:value="deptSearchKeyword"
          placeholder="搜索部门"
          clearable
          size="small"
          style="margin-bottom: 8px;"
        />
        <n-tree
          :data="filteredDeptTree"
          :selected-keys="selectedDeptKeys"
          key-field="id"
          label-field="deptName"
          children-field="children"
          selectable
          default-expand-all
          block-line
          @update:selected-keys="handleDeptSelect"
        />
        <n-button
          v-if="selectedDeptId !== null"
          text
          type="primary"
          size="small"
          style="margin-top: 8px;"
          @click="handleDeptSelect([])"
        >
          清除筛选
        </n-button>
      </n-card>

      <!-- 右侧表格区 -->
      <n-card class="main-card" style="flex: 1; min-width: 0;">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="22" color="var(--color-primary)" class="header-icon"><PersonOutline /></n-icon>
            <span>用户管理</span>
            <n-tag v-if="selectedDeptId !== null" type="info" size="small" closable @close="handleDeptSelect([])">
              {{ deptMap[selectedDeptId] || '部门筛选' }}
            </n-tag>
          </div>
        </template>
        <template #header-extra>
          <n-space>
            <FilterPanel
              :fields="filterFields"
              :model-value="activeFilters"
              @apply="handleFilterApply"
            />
            <n-button @click="handleExport">
              <template #icon><n-icon><DownloadOutline /></n-icon></template>
              导出
            </n-button>
            <n-button v-permission="'system:user:add'" type="primary" @click="handleCreate">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              新增用户
            </n-button>
          </n-space>
        </template>

        <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
        <n-form class="query-form" inline>
          <n-form-item>
            <n-input
              v-model:value="searchKeyword"
              placeholder="搜索用户名/昵称..."
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearchReset"
            />
          </n-form-item>
          <n-form-item>
            <n-select
              v-model:value="searchStatus"
              :options="USER_STATUS_FILTER_OPTIONS"
              placeholder="用户状态"
              clearable
              style="min-width: 140px"
              @update:value="handleSearch"
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

        <!-- 批量操作栏 -->
        <div v-if="checkedRowKeys.length > 0" class="batch-bar">
          <span>已选择 <b>{{ checkedRowKeys.length }}</b> 项</span>
          <n-space size="small">
            <n-button size="small" type="info" @click="handleBatchStatus(1)">批量启用</n-button>
            <n-button size="small" type="warning" @click="handleBatchStatus(0)">批量禁用</n-button>
            <n-button size="small" type="error" @click="handleBatchDelete">批量删除</n-button>
          </n-space>
        </div>

        <!-- Empty State (Req 16.7) -->
        <n-empty v-if="!loading && tableData.length === 0" description="暂无用户数据" style="margin: 32px 0;" />

        <n-data-table
          v-else
          :columns="columns"
          :data="tableData"
          :loading="loading"
          :pagination="false"
          :row-key="(row: User) => row.id"
          :checked-row-keys="checkedRowKeys"
          :scroll-x="1200"
          striped
          class="custom-table"
          @update:checked-row-keys="onCheckedRowKeysChange"
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
      </n-card>
    </div>
    
    <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 600px; border-radius: 16px;">
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="100px"
      >
        <div style="display: flex; justify-content: center; margin-bottom: 16px;">
          <AvatarSelector :model-value="form.avatar ?? null" :size="64" @update:model-value="(v) => form.avatar = v" />
        </div>
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
        </n-form-item>
        <n-form-item v-if="!form.id" label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            placeholder="请输入密码"
            show-password-on="click"
          />
        </n-form-item>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="form.nickname" placeholder="请输入昵称" />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="form.email" placeholder="请输入邮箱" />
        </n-form-item>
        <n-form-item label="所属部门" path="deptId">
          <n-tree-select
            v-model:value="form.deptId"
            :options="deptTreeOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            placeholder="请选择所属部门"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-form-item label="岗位" path="postId">
          <n-select
            v-model:value="form.postId"
            :options="postOptions"
            placeholder="请选择岗位"
            clearable
          />
        </n-form-item>
        <n-form-item label="手机号" path="phone">
          <n-input v-model:value="form.phone" placeholder="请输入手机号" maxlength="11" />
        </n-form-item>
        <n-form-item label="性别" path="gender">
          <n-select v-model:value="form.gender" :options="GENDER_OPTIONS" placeholder="请选择性别" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-select v-model:value="form.status" :options="USER_STATUS_OPTIONS" />
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showModal = false">取消</n-button>
            <n-button type="primary" @click="handleSubmit">保存</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>
    
    <!-- 角色分配对话框 -->
    <n-modal v-model:show="showRoleModal" preset="card" title="分配角色" style="width: 480px; border-radius: 16px;">
      <n-checkbox-group v-model:value="userRoles">
        <n-space vertical :size="12">
          <div 
            v-for="role in allRoles" 
            :key="role.id" 
            class="role-item"
          >
            <n-checkbox :value="role.id">
              <div class="role-info">
                <span class="role-name">{{ role.roleName }}</span>
                <n-tag size="small" type="info" round>{{ role.roleCode }}</n-tag>
              </div>
            </n-checkbox>
          </div>
        </n-space>
      </n-checkbox-group>
      <template #footer>
        <n-space style="width: 100%; justify-content: flex-end;">
          <n-button @click="showRoleModal = false">取消</n-button>
          <n-button type="primary" @click="handleSaveRoles">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 用户详情抽屉 -->
    <n-drawer v-model:show="showDetailDrawer" :width="480" placement="right">
      <n-drawer-content :title="detailUser?.nickname || '用户详情'" closable>
        <div v-if="detailUser" class="detail-content">
          <!-- 顶部头像展示区域 -->
          <div class="detail-avatar-section">
            <div
              class="detail-avatar"
              :style="{
                background: detailUserPreset?.gradient || 'var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af))'
              }"
            >
              <span v-if="detailUserPreset" class="detail-avatar-icon">
                {{ detailUserPreset.icon }}
              </span>
              <span v-else class="detail-avatar-letter">
                {{ (detailUser.nickname || detailUser.username || '?').charAt(0) }}
              </span>
            </div>
            <div class="detail-avatar-name">{{ detailUser.nickname || detailUser.username }}</div>
            <n-tag :type="detailUser.status === 1 ? 'success' : 'error'" size="small" round>
              {{ detailUser.status === 1 ? '启用' : '禁用' }}
            </n-tag>
          </div>
          <div class="detail-section">
            <div class="detail-section-title">基本信息</div>
            <div class="detail-row">
              <span class="detail-label">用户名</span>
              <span class="detail-value">{{ detailUser.username }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">昵称</span>
              <span class="detail-value">{{ detailUser.nickname }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">性别</span>
              <span class="detail-value">{{ { 0: '未知', 1: '男', 2: '女' }[detailUser.gender ?? 0] }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态</span>
              <n-tag :type="detailUser.status === 1 ? 'success' : 'error'" size="small">
                {{ detailUser.status === 1 ? '启用' : '禁用' }}
              </n-tag>
            </div>
          </div>
          <div class="detail-section">
            <div class="detail-section-title">联系信息</div>
            <div class="detail-row">
              <span class="detail-label">邮箱</span>
              <span class="detail-value">{{ detailUser.email || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">手机号</span>
              <span class="detail-value">{{ detailUser.phone || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">部门</span>
              <span class="detail-value">{{ detailUser.deptId ? (deptMap[detailUser.deptId] || '-') : '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">岗位</span>
              <span class="detail-value">{{ detailUser.postName || '-' }}</span>
            </div>
          </div>
          <div class="detail-section">
            <div class="detail-section-title">角色信息</div>
            <div class="detail-row">
              <span class="detail-label">角色</span>
              <span class="detail-value">{{ detailUser.roles?.join(', ') || '-' }}</span>
            </div>
          </div>
          <div class="detail-section">
            <div class="detail-section-title">时间信息</div>
            <div class="detail-row">
              <span class="detail-label">创建时间</span>
              <span class="detail-value">{{ formatDateTime(detailUser.createTime) }}</span>
            </div>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { NButton, NSpace, NIcon, NTag, NTooltip, NEllipsis, NPopconfirm, useMessage, useDialog, type FormInst } from 'naive-ui'
import { PeopleOutline, PersonOutline, CheckmarkCircleOutline, ShieldCheckmarkOutline, AddOutline, DownloadOutline, BusinessOutline, CreateOutline, TrashOutline, KeyOutline, SearchOutline } from '@vicons/ionicons5'
import { getUserList, createUser, updateUser, deleteUser, getUserRoles, assignUserRoles, resetPassword, batchDeleteUsers, batchUpdateUserStatus, exportUsers } from '@/api/system/user'
import { getRoleList } from '@/api/system/role'
import { getDepartmentTree } from '@/api/org/department'
import type { Department } from '@/api/org/department'
import { listPosts } from '@/api/org/post'
import type { Post } from '@/api/org/post'
import { initMessage } from '@/utils/message'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES, USER_STATUS_OPTIONS } from '@/constants'
import type { User, UserForm } from '@/types/user'
import type { PageResult } from '@/types/api'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import AvatarSelector from '@/components/common/AvatarSelector.vue'
import { avatarPresets } from '@/constants/avatarPresets'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import type { FilterCondition } from '@/types/api'
import { filtersToApiParam } from '@/utils/filterParams'
import { hasPermission } from '@/utils/permission'
import { useExport } from '@/composables/useExport'

const message = useMessage()
const dialog = useDialog()
initMessage(message)
const { exportExcel: exportExcelFn } = useExport({ defaultFilename: '用户列表' })

const loading = ref(false)
const showModal = ref(false)
const showRoleModal = ref(false)
const modalTitle = ref('新增用户')
const formRef = ref<FormInst | null>(null)
const tableData = ref<User[]>([])
const allRoles = ref<any[]>([])
const userRoles = ref<number[]>([])
const currentUserId = ref<number | null>(null)
const activeFilters = ref<FilterCondition[]>([])
const departmentTree = ref<Department[]>([])
const deptMap = ref<Record<number, string>>({})
const allPosts = ref<Post[]>([])

// 搜索筛选
const searchKeyword = ref('')
const searchStatus = ref<number | null>(null)

// 用户状态筛选选项
const USER_STATUS_FILTER_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 部门树筛选
const deptSearchKeyword = ref('')
const selectedDeptId = ref<number | null>(null)
const selectedDeptKeys = computed(() => selectedDeptId.value !== null ? [selectedDeptId.value] : [])

// 批量操作
const checkedRowKeys = ref<number[]>([])

// 用户详情抽屉
const showDetailDrawer = ref(false)
const detailUser = ref<User | null>(null)

// 详情用户的头像预设
const detailUserPreset = computed(() => {
  if (!detailUser.value?.avatar) return null
  return avatarPresets.find(p => p.id === detailUser.value!.avatar) || null
})

// 筛选后的部门树
const filteredDeptTree = computed(() => {
  if (!deptSearchKeyword.value) return departmentTree.value
  const keyword = deptSearchKeyword.value.toLowerCase()
  const filterNodes = (nodes: Department[]): Department[] => {
    const result: Department[] = []
    for (const node of nodes) {
      if (node.deptName.toLowerCase().includes(keyword)) {
        result.push(node)
      } else if (node.children?.length) {
        const filteredChildren = filterNodes(node.children)
        if (filteredChildren.length) {
          result.push({ ...node, children: filteredChildren })
        }
      }
    }
    return result
  }
  return filterNodes(departmentTree.value)
})

const GENDER_OPTIONS = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

// 计算启用用户数量
const enabledCount = computed(() => {
  return tableData.value.filter(user => user.status === 1).length
})

// 部门树选择选项
const deptTreeOptions = computed(() => departmentTree.value)

const postOptions = computed(() =>
  allPosts.value.map(p => ({ label: p.postName, value: p.id }))
)

const form = reactive<UserForm>({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  deptId: null,
  postId: null,
  phone: '',
  gender: 0,
  status: 1,
  avatar: null
})

const rules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: {
    required: true,
    message: '请输入密码',
    trigger: 'blur',
    validator: (_rule: any, value: string) => {
      if (!form.id && !value) {
        return new Error('请输入密码')
      }
      return true
    }
  },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  email: {
    required: false,
    trigger: 'blur',
    type: 'email',
    message: '请输入正确的邮箱格式'
  }
}

// 分页状态
const paginationPage = ref(DEFAULT_PAGE)
const paginationPageSize = ref(DEFAULT_PAGE_SIZE)
const paginationItemCount = ref(0)

const filterFields: FilterField[] = [
  { label: 'ID', value: 'id', type: 'number' },
  { label: '用户名', value: 'username', type: 'string' },
  { label: '昵称', value: 'nickname', type: 'string' },
  { label: '邮箱', value: 'email', type: 'string' },
  { label: '手机号', value: 'phone', type: 'string' },
  { label: '状态', value: 'status', type: 'number' },
  { label: '创建时间', value: 'createTime', type: 'string' }
]

const columns = [
  { type: 'selection', width: 50 },
  { title: 'ID', key: 'id', width: 80 },
  {
    title: '用户名',
    key: 'username',
    width: 120,
    render: (row: User) => h('a', {
      class: 'link-text',
      onClick: (e: Event) => { e.preventDefault(); detailUser.value = row; showDetailDrawer.value = true }
    }, row.username)
  },
  { title: '昵称', key: 'nickname', width: 100 },
  {
    title: '部门',
    key: 'deptId',
    width: 100,
    render: (row: User) => row.deptId ? (deptMap.value[row.deptId] || '-') : '-'
  },
  { title: '手机号', key: 'phone', width: 120, render: (row: User) => row.phone || '-' },
  {
    title: '角色',
    key: 'roles',
    width: 140,
    ellipsis: { tooltip: true },
    render: (row: any) => {
      const roles = row.roles?.join(', ') || '-'
      return h(NEllipsis, { style: 'max-width: 130px' }, { default: () => roles })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: any) => h(NTag, {
      type: row.status === 1 ? 'success' : 'error',
      size: 'small'
    }, { default: () => row.status === 1 ? '启用' : '禁用' })
  },
  { 
    title: '创建时间', 
    key: 'createTime',
    width: 180,
    render: (row: User) => formatDateTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row: User) => {
      const buttons: any[] = []
      
      if (hasPermission('system:user:edit')) {
        buttons.push(
          h(NTooltip, null, {
            trigger: () => h(NButton, {
              size: 'small',
              quaternary: true,
              onClick: () => handleEdit(row)
            }, {
              icon: () => h(NIcon, null, { default: () => h(CreateOutline) })
            }),
            default: () => '编辑'
          })
        )
      }
      
      if (hasPermission('system:user:assign')) {
        buttons.push(
          h(NTooltip, null, {
            trigger: () => h(NButton, {
              size: 'small',
              type: 'info',
              quaternary: true,
              onClick: () => handleAssignRoles(row)
            }, {
              icon: () => h(NIcon, null, { default: () => h(ShieldCheckmarkOutline) })
            }),
            default: () => '分配角色'
          })
        )
      }
      
      if (hasPermission('system:user:resetpwd') && row.username !== 'admin') {
        buttons.push(
          h(NTooltip, null, {
            trigger: () => h(NButton, {
              size: 'small',
              type: 'warning',
              quaternary: true,
              onClick: () => handleResetPassword(row)
            }, {
              icon: () => h(NIcon, null, { default: () => h(KeyOutline) })
            }),
            default: () => '重置密码'
          })
        )
      }
      
      if (hasPermission('system:user:delete') && row.username !== 'admin') {
        buttons.push(
          h(NPopconfirm, {
            onPositiveClick: () => handleDelete(row.id)
          }, {
            trigger: () => h(NButton, {
              size: 'small',
              type: 'error',
              quaternary: true
            }, {
              icon: () => h(NIcon, null, { default: () => h(TrashOutline) })
            }),
            default: () => '确定删除该用户吗？'
          })
        )
      }
      
      return h(NSpace, { size: 4, wrap: false }, { default: () => buttons })
    }
  }
]

const onCheckedRowKeysChange = (keys: number[]) => {
  checkedRowKeys.value = keys
}

const handleDeptSelect = (keys: Array<string | number>) => {
  if (keys.length > 0) {
    selectedDeptId.value = keys[0] as number
  } else {
    selectedDeptId.value = null
  }
  paginationPage.value = 1
  fetchData()
}

/** 搜索 */
const handleSearch = () => {
  paginationPage.value = 1
  fetchData()
}

/** 重置搜索 */
const handleSearchReset = () => {
  searchKeyword.value = ''
  searchStatus.value = null
  paginationPage.value = 1
  fetchData()
}

const handleBatchDelete = () => {
  if (checkedRowKeys.value.length === 0) return
  dialog.warning({
    title: '确认批量删除',
    content: `确定要删除选中的 ${checkedRowKeys.value.length} 个用户吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await batchDeleteUsers(checkedRowKeys.value)
        const data = (res as any).data || res
        message.success(`成功删除 ${data.success || checkedRowKeys.value.length} 个用户`)
        checkedRowKeys.value = []
        fetchData()
      } catch (error) {
        message.error(handleApiError(error, '批量删除'))
      }
    }
  })
}

const handleBatchStatus = async (status: number) => {
  if (checkedRowKeys.value.length === 0) return
  try {
    await batchUpdateUserStatus(checkedRowKeys.value, status)
    message.success(status === 1 ? '批量启用成功' : '批量禁用成功')
    checkedRowKeys.value = []
    fetchData()
  } catch (error) {
    message.error(handleApiError(error, '批量更新状态'))
  }
}

const handleExport = async () => {
  try {
    const filtersParam = filtersToApiParam(activeFilters.value)
    const res = await exportUsers(filtersParam || undefined)
    const data = (res as any).data || res
    const list = Array.isArray(data) ? data : (data.list || data)
    await exportExcelFn(list, '用户列表')
  } catch (error) {
    message.error(handleApiError(error, '导出用户'))
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    // 合并部门筛选、搜索筛选和高级筛选
    const allFilters = [...activeFilters.value]
    if (selectedDeptId.value !== null) {
      allFilters.push({ field: 'deptId', operator: 'eq', value: String(selectedDeptId.value) })
    }
    if (searchKeyword.value) {
      allFilters.push({ field: 'keyword', operator: 'like', value: searchKeyword.value })
    }
    if (searchStatus.value !== null) {
      allFilters.push({ field: 'status', operator: 'eq', value: String(searchStatus.value) })
    }
    const filtersParam = filtersToApiParam(allFilters)
    const res = await getUserList({
      page: paginationPage.value,
      pageSize: paginationPageSize.value,
      ...(filtersParam ? { filters: filtersParam } : {})
    })
    
    let pageResult: PageResult<User> | null = null
    
    if (res && typeof res === 'object') {
      if (res.data && typeof res.data === 'object' && 'list' in res.data && 'total' in res.data) {
        pageResult = res.data as PageResult<User>
      } else if ('list' in res && 'total' in res) {
        pageResult = res as PageResult<User>
      }
    }
    
    const list = pageResult?.list || []
    tableData.value = list
    
    if (pageResult && pageResult.total !== undefined && pageResult.total !== null) {
      const total = typeof pageResult.total === 'number' 
        ? Math.max(0, pageResult.total)
        : (() => {
            const num = Number(pageResult.total)
            return isNaN(num) ? 0 : Math.max(0, num)
          })()
      paginationItemCount.value = total
    } else {
      paginationItemCount.value = 0
    }
  } catch (error) {
    const errorMsg = handleApiError(error, '获取用户列表')
    message.error(errorMsg)
    if (tableData.value.length === 0) {
      paginationItemCount.value = 0
    }
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  modalTitle.value = '新增用户'
  Object.assign(form, {
    id: null,
    username: '',
    password: '',
    nickname: '',
    email: '',
    deptId: null,
    postId: null,
    phone: '',
    gender: 0,
    status: 1,
    avatar: null
  })
  showModal.value = true
}

const handleEdit = (row: User) => {
  modalTitle.value = '编辑用户'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email,
    deptId: row.deptId || null,
    postId: row.postId || null,
    phone: row.phone || '',
    gender: row.gender ?? 0,
    status: row.status,
    avatar: row.avatar || null
  })
  showModal.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    try {
      if (form.id) {
        const { password, ...updateData } = form
        const payload = password ? { ...updateData, password } : updateData
        await updateUser(payload as any)
        message.success('更新成功')
      } else {
        const { id, ...createData } = form
        await createUser(createData as any)
        message.success('创建成功')
      }
      showModal.value = false
      fetchData()
    } catch (error) {
      const errorMsg = handleApiError(error, form.id ? '更新用户' : '创建用户')
      message.error(errorMsg)
    }
  } catch (error) {
    // 验证失败，不处理
  }
}

const handleDelete = async (id: number) => {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除该用户吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteUser(id)
        message.success('删除成功')
        fetchData()
      } catch (error) {
        const errorMsg = handleApiError(error, '删除用户')
        message.error(errorMsg)
      }
    }
  })
}

const handleAssignRoles = async (row: User) => {
  currentUserId.value = row.id
  try {
    const rolesRes = await getRoleList({ page: 1, pageSize: 100 })
    allRoles.value = (rolesRes.data as unknown as PageResult<any>).list
    
    const userRolesRes = await getUserRoles(row.id)
    const roles = userRolesRes.data || []
    userRoles.value = Array.isArray(roles) ? roles.map((r: any) => r.id) : []
    
    showRoleModal.value = true
  } catch (error) {
    handleApiError(error, '加载角色信息')
  }
}

const handleSaveRoles = async () => {
  if (currentUserId.value === null) return
  
  try {
    await assignUserRoles(currentUserId.value, userRoles.value)
    message.success('角色分配成功')
    showRoleModal.value = false
    fetchData()
  } catch (error) {
    const errorMsg = handleApiError(error, '分配角色')
    message.error(errorMsg)
  }
}

const handleResetPassword = async (row: User) => {
  dialog.warning({
    title: '确认重置密码',
    content: `确定要重置用户"${row.username}"的密码为默认密码吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await resetPassword(row.id)
        message.success('密码重置成功，请使用系统配置的默认密码登录')
        fetchData()
      } catch (error) {
        const errorMsg = handleApiError(error, '重置密码')
        message.error(errorMsg)
      }
    }
  })
}

const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  paginationPage.value = 1
  fetchData()
}

const handlePageChange = (page: number) => {
  paginationPage.value = page
  fetchData()
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  fetchData()
}

onMounted(() => {
  fetchData()
  loadAllRoles()
  loadDepartments()
  loadPosts()
})

const loadAllRoles = async () => {
  try {
    const res = await getRoleList({ page: 1, pageSize: 100 })
    allRoles.value = (res.data as unknown as PageResult<any>).list
  } catch (error) {
    handleApiError(error, '加载角色列表')
  }
}

/** 加载岗位列表 */
const loadPosts = async () => {
  try {
    const res = await listPosts({ status: 1 })
    allPosts.value = (res as any).data || []
  } catch (error) {
    handleApiError(error, '加载岗位列表')
  }
}

/** 加载部门树并构建 id->name 映射 */
const loadDepartments = async () => {
  try {
    const res = await getDepartmentTree()
    departmentTree.value = (res as any).data || []
    const map: Record<number, string> = {}
    const flatten = (nodes: Department[]) => {
      for (const node of nodes) {
        if (node.id) map[node.id] = node.deptName
        if (node.children?.length) flatten(node.children)
      }
    }
    flatten(departmentTree.value)
    deptMap.value = map
  } catch (error) {
    handleApiError(error, '加载部门列表')
  }
}
</script>

<style scoped>
/* 页面容器 */
.user-page {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 布局 */
.content-layout {
  display: flex;
  gap: 16px;
  min-width: 0;
}

.dept-tree-card {
  width: 240px;
  flex-shrink: 0;
  border-radius: var(--radius-lg);
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

/* 用户名链接样式 */
:deep(.link-text) {
  color: var(--primary-color, var(--color-primary));
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
  transition: opacity 0.2s;
}
:deep(.link-text:hover) {
  opacity: 0.7;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  margin-bottom: 12px;
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-radius: var(--radius-md);
  font-size: 13px;
  color: #1E40AF;
}

/* 角色分配样式 */
.role-item {
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.role-item:hover {
  background: var(--bg-tertiary);
}

.role-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-name {
  font-weight: 500;
  color: var(--text-primary);
}

/* 用户详情抽屉 */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.detail-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-avatar-icon {
  font-size: 32px;
  line-height: 1;
}

.detail-avatar-letter {
  font-size: 24px;
  font-weight: 600;
  color: #fff;
  line-height: 1;
}

.detail-avatar-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.detail-section {
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 16px;
}

.detail-section:last-child {
  border-bottom: none;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.detail-row {
  display: flex;
  align-items: center;
  padding: 6px 0;
}

.detail-label {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.detail-value {
  font-size: 13px;
  color: var(--text-primary);
}

/* 响应式 */
@media (max-width: 768px) {
  .user-page { padding: 0; }
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 130px; flex: 0 0 auto; }
  .content-layout { flex-direction: column; }
  .dept-tree-card { width: 100%; max-height: 200px; }
  .main-card { border-radius: 14px !important; }
  .pagination-wrapper { flex-direction: column; gap: 8px; }
}

</style>

<style>
/* index 深色模式（非 scoped） */
html.dark .batch-bar {
  background: rgba(59, 130, 246, 0.1) !important;
  border-color: rgba(59, 130, 246, 0.2) !important;
  color: #93c5fd !important;
}
html.dark .role-item {
  background: #1e293b !important;
  border: 1px solid rgba(255, 255, 255, 0.06) !important;
}
html.dark .role-item:hover {
  background: #334155 !important;
  border-color: rgba(59, 130, 246, 0.2) !important;
}
html.dark .detail-avatar-section {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}
html.dark .detail-section {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}
</style>
