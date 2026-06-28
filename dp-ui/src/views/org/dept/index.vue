<template>
  <div class="department-page">
    <!-- 页面头部统计 (Req 1.1) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><BusinessOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ deptStats.total }}</span>
          <span class="stat-label">部门总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ deptStats.enabled }}</span>
          <span class="stat-label">已启用</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ deptStats.disabled }}</span>
          <span class="stat-label">已禁用</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><GitBranchOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ deptStats.maxDepth }}</span>
          <span class="stat-label">最大层级</span>
        </div>
      </div>
    </div>

    <!-- 主内容卡片 (Req 1.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><BusinessOutline /></n-icon>
            <span>部门列表</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button size="small" @click="toggleExpandAll">
            {{ isAllExpanded ? '全部折叠' : '全部展开' }}
          </n-button>
          <n-button
            v-if="hasSortChanges"
            size="small"
            type="success"
            :loading="savingSortOrder"
            @click="handleSaveSortOrder"
          >
            <template #icon><n-icon><SaveOutline /></n-icon></template>
            保存排序
          </n-button>
          <n-divider vertical />
          <n-button type="primary" @click="handleCreate(0)">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新增部门
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索部门名称..."
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
            @clear="handleReset"
          >
            <template #prefix><n-icon :component="SearchOutline" /></template>
          </n-input>
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="loadData">
            <template #icon><n-icon :component="SearchOutline" /></template>
            搜索
          </n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- Empty State (Req 16.7) -->
      <n-empty v-if="!loading && tableData.length === 0" description="暂无部门数据" style="margin: 32px 0;" />

      <!-- 树形表格 (Req 1.6) -->
      <n-data-table
        v-else
        :columns="columns"
        :data="tableData"
        :row-key="(row: Department) => row.id!"
        :loading="loading"
        :default-expanded-row-keys="expandedRowKeys"
        :expanded-row-keys="expandedRowKeys"
        :indent="24"
        :cascade="false"
        :scroll-x="1100"
        size="medium"
        striped
        class="custom-table"
        @update:expanded-row-keys="onExpandedChange"
      />
    </n-card>

    <!-- 新增/编辑弹窗 (Req 1.7) -->
    <n-modal
      v-model:show="showModal"
      preset="dialog"
      :title="editMode === 'add' ? '新增部门' : '编辑部门'"
      :positive-text="undefined"
      :negative-text="undefined"
      style="width: 640px; border-radius: 16px;"
      :show-icon="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="80px"
        style="margin-top: 16px"
      >
        <n-form-item label="上级部门" path="parentId">
          <n-tree-select
            v-model:value="formData.parentId"
            :options="parentTreeOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            placeholder="选择上级（空为顶级）"
            clearable
            default-expand-all
          />
        </n-form-item>
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <n-form-item label="部门名称" path="deptName">
              <n-input v-model:value="formData.deptName" placeholder="请输入" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="部门编码" path="deptCode">
              <n-input v-model:value="formData.deptCode" placeholder="如 TECH" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <n-form-item label="负责人">
              <n-input v-model:value="formData.leader" placeholder="请输入" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="联系电话">
              <n-input v-model:value="formData.phone" placeholder="请输入" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="邮箱">
          <n-input v-model:value="formData.email" placeholder="请输入" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="24">
          <n-gi>
            <n-form-item label="显示排序">
              <n-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="状态">
              <n-radio-group v-model:value="formData.status">
                <n-radio :value="1">启用</n-radio>
                <n-radio :value="0">禁用</n-radio>
              </n-radio-group>
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>
      <template #action>
        <n-button @click="showModal = false">取 消</n-button>
        <n-button type="primary" :loading="saving" @click="handleSave">确 定</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, onMounted, computed, reactive } from 'vue'
import {
  NButton, NTag, NSpace, NIcon, NPopconfirm,
  useMessage, useDialog
} from 'naive-ui'
import type { FormInst, DataTableColumns } from 'naive-ui'
import {
  GitBranchOutline, SearchOutline, AddOutline, CreateOutline,
  TrashOutline, BusinessOutline, CheckmarkCircleOutline,
  CloseCircleOutline, ArrowUpOutline, ArrowDownOutline, SaveOutline
} from '@vicons/ionicons5'
import * as departmentApi from '@/api/org/department'
import type { Department } from '@/api/org/department'
import { formatDateTime } from '@/utils/format'

const message = useMessage()
const dialog = useDialog()
const loading = ref(false)
const saving = ref(false)
const treeData = ref<Department[]>([])
const searchKeyword = ref('')
const showModal = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInst | null>(null)
const currentEditId = ref<number | null>(null)

// ==================== 统计 ====================
const deptStats = reactive({ total: 0, enabled: 0, disabled: 0, maxDepth: 0 })

function calcStats(nodes: Department[], depth = 1) {
  for (const n of nodes) {
    deptStats.total++
    n.status === 1 ? deptStats.enabled++ : deptStats.disabled++
    if (depth > deptStats.maxDepth) deptStats.maxDepth = depth
    if (n.children?.length) calcStats(n.children, depth + 1)
  }
}

// ==================== 展开控制 ====================
const expandedRowKeys = ref<number[]>([])
const isAllExpanded = ref(true)

function collectAllIds(nodes: Department[]): number[] {
  const ids: number[] = []
  for (const n of nodes) {
    if (n.id != null && n.children?.length) ids.push(n.id)
    if (n.children?.length) ids.push(...collectAllIds(n.children))
  }
  return ids
}

function toggleExpandAll() {
  if (isAllExpanded.value) {
    expandedRowKeys.value = []
    isAllExpanded.value = false
  } else {
    expandedRowKeys.value = collectAllIds(treeData.value)
    isAllExpanded.value = true
  }
}

function onExpandedChange(keys: number[]) {
  expandedRowKeys.value = keys
}

// ==================== 搜索重置 ====================
function handleReset() {
  searchKeyword.value = ''
  loadData()
}

// ==================== 表格列定义 ====================
const tableData = computed(() => treeData.value)

const columns = computed<DataTableColumns<Department>>(() => [
  {
    title: '部门名称',
    key: 'deptName',
    minWidth: 220,
    tree: true,
    ellipsis: { tooltip: true },
  },
  {
    title: '部门编码',
    key: 'deptCode',
    width: 120,
    align: 'center',
    render(row) {
      return row.deptCode
        ? h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => row.deptCode })
        : h('span', { style: 'color: #ccc' }, '-')
    }
  },
  {
    title: '负责人',
    key: 'leader',
    width: 120,
    align: 'center',
    render(row) {
      return row.leader || h('span', { style: 'color: #ccc' }, '-')
    }
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 120,
    align: 'center',
    render(row: Department) {
      return h('div', { style: 'display: flex; align-items: center; justify-content: center; gap: 4px;' }, [
        h(NButton, {
          size: 'tiny', quaternary: true, circle: true,
          disabled: !canMoveUp(row),
          onClick: () => moveDept(row, 'up')
        }, { icon: () => h(NIcon, { size: 14 }, { default: () => h(ArrowUpOutline) }) }),
        h('span', { style: 'min-width: 28px; text-align: center;' }, String(row.sortOrder ?? 0)),
        h(NButton, {
          size: 'tiny', quaternary: true, circle: true,
          disabled: !canMoveDown(row),
          onClick: () => moveDept(row, 'down')
        }, { icon: () => h(NIcon, { size: 14 }, { default: () => h(ArrowDownOutline) }) }),
      ])
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    align: 'center',
    render(row) {
      return h(NTag, {
        size: 'small',
        type: row.status === 1 ? 'success' : 'error',
        bordered: false,
      }, { default: () => row.status === 1 ? '正常' : '停用' })
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
    align: 'center',
    render(row) {
      return row.createTime ? formatDateTime(row.createTime) : '-'
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    align: 'center',
    render(row) {
      return h(NSpace, { justify: 'center', size: 4, wrap: false }, {
        default: () => [
          h(NButton, {
            size: 'small', quaternary: true,
            onClick: () => handleEdit(row)
          }, {
            icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
            default: () => '编辑',
          }),
          h(NButton, {
            size: 'small', quaternary: true, type: 'info',
            onClick: () => handleCreate(row.id!)
          }, {
            icon: () => h(NIcon, null, { default: () => h(AddOutline) }),
            default: () => '新增',
          }),
          h(NButton, {
            size: 'small', quaternary: true, type: 'error',
            onClick: () => handleDelete(row)
          }, {
            icon: () => h(NIcon, null, { default: () => h(TrashOutline) }),
            default: () => '删除',
          }),
        ]
      })
    }
  }
])

// ==================== 表单 ====================
const formData = ref<Department>({
  deptName: '', deptCode: '', parentId: 0, leader: '', phone: '', email: '', sortOrder: 0, status: 1
})

const formRules = {
  deptName: { required: true, message: '请输入部门名称', trigger: 'blur' },
}

const parentTreeOptions = computed(() => {
  return [{ id: 0, deptName: '顶级部门', children: treeData.value } as any]
})

// ==================== API ====================
async function loadData() {
  loading.value = true
  try {
    const res = await departmentApi.getDepartmentTree({ keyword: searchKeyword.value || undefined })
    treeData.value = (res as any).data || []
    deptStats.total = 0; deptStats.enabled = 0; deptStats.disabled = 0; deptStats.maxDepth = 0
    calcStats(treeData.value)
    expandedRowKeys.value = collectAllIds(treeData.value)
    isAllExpanded.value = true
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleCreate(parentId: number) {
  editMode.value = 'add'
  currentEditId.value = null
  formData.value = {
    deptName: '', deptCode: '', parentId, leader: '', phone: '', email: '', sortOrder: 0, status: 1
  }
  showModal.value = true
}

function handleEdit(row: Department) {
  editMode.value = 'edit'
  currentEditId.value = row.id!
  formData.value = { ...row }
  showModal.value = true
}

function handleDelete(row: Department) {
  dialog.warning({
    title: '删除确认',
    content: `确定要删除「${row.deptName}」及其所有子部门吗？`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await departmentApi.deleteDepartment(row.id!)
        message.success('删除成功')
        loadData()
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editMode.value === 'add') {
      await departmentApi.createDepartment(formData.value)
      message.success('新增成功')
    } else {
      await departmentApi.updateDepartment(currentEditId.value!, formData.value)
      message.success('修改成功')
    }
    showModal.value = false
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== 拖拽排序 ====================
const savingSortOrder = ref(false)
const sortChanges = ref<Map<number, number>>(new Map())
const hasSortChanges = computed(() => sortChanges.value.size > 0)

function getSiblings(node: Department): Department[] {
  const parentId = node.parentId
  const findInTree = (nodes: Department[]): Department[] => {
    for (const n of nodes) {
      if (n.children?.length) {
        if (n.id === parentId) return n.children
        const result = findInTree(n.children)
        if (result.length) return result
      }
    }
    return []
  }
  if (parentId === 0) return treeData.value
  return findInTree(treeData.value)
}

function canMoveUp(node: Department): boolean {
  const siblings = getSiblings(node)
  const idx = siblings.findIndex(n => n.id === node.id)
  return idx > 0
}

function canMoveDown(node: Department): boolean {
  const siblings = getSiblings(node)
  const idx = siblings.findIndex(n => n.id === node.id)
  return idx >= 0 && idx < siblings.length - 1
}

function moveDept(node: Department, direction: 'up' | 'down') {
  const siblings = getSiblings(node)
  const idx = siblings.findIndex(n => n.id === node.id)
  if (direction === 'up' && idx > 0) {
    const prevOrder = siblings[idx - 1].sortOrder ?? 0
    const curOrder = node.sortOrder ?? 0
    siblings[idx - 1].sortOrder = curOrder
    node.sortOrder = prevOrder
    ;[siblings[idx - 1], siblings[idx]] = [siblings[idx], siblings[idx - 1]]
    sortChanges.value.set(node.id!, node.sortOrder!)
    sortChanges.value.set(siblings[idx].id!, siblings[idx].sortOrder!)
  } else if (direction === 'down' && idx < siblings.length - 1) {
    const nextOrder = siblings[idx + 1].sortOrder ?? 0
    const curOrder = node.sortOrder ?? 0
    siblings[idx + 1].sortOrder = curOrder
    node.sortOrder = nextOrder
    ;[siblings[idx], siblings[idx + 1]] = [siblings[idx + 1], siblings[idx]]
    sortChanges.value.set(node.id!, node.sortOrder!)
    sortChanges.value.set(siblings[idx].id!, siblings[idx].sortOrder!)
  }
  treeData.value = [...treeData.value]
}

async function handleSaveSortOrder() {
  savingSortOrder.value = true
  try {
    const items = Array.from(sortChanges.value.entries()).map(([id, sortOrder]) => ({ id, sortOrder }))
    await departmentApi.batchUpdateDepartmentSort(items)
    message.success('排序保存成功')
    sortChanges.value = new Map()
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存排序失败')
  } finally {
    savingSortOrder.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.department-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Card header */
.card-header-custom {
  display: flex;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary, #1f2937);
}

/* 树形表格样式优化 */
:deep(.n-data-table-th) {
  font-weight: 600 !important;
  background: var(--bg-tertiary, #fafbfc) !important;
  color: var(--text-secondary) !important;
}

:deep(.n-data-table-td) {
  transition: background 0.15s;
}

:deep(.n-data-table-tr:hover > .n-data-table-td) {
  background: var(--bg-hover, #f8fafc) !important;
}

:deep(.n-data-table-indent) {
  height: 100%;
}

:deep(.n-data-table-expand-trigger) {
  margin-right: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: column;
  }
}
</style>
