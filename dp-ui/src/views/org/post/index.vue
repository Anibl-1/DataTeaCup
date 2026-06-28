<template>
  <div class="post-page">
    <!-- 页面头部统计 (Req 1.1) -->
    <PageHeaderStats :items="statsItems" />

    <!-- 主内容卡片 (Req 1.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><BriefcaseOutline /></n-icon>
            <span>岗位列表</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-button type="primary" @click="handleCreate">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新增岗位
        </n-button>
      </template>

      <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索岗位名称..."
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
            @clear="handleSearchReset"
          >
            <template #prefix><n-icon><SearchOutline /></n-icon></template>
          </n-input>
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchStatus"
            :options="statusOptions"
            placeholder="岗位状态"
            clearable
            style="width: 120px"
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
      <n-empty v-if="!tableLoading && tableData.length === 0" description="暂无岗位数据" style="margin: 32px 0;" />

      <!-- 数据表格 (Req 1.6) -->
      <n-data-table
        v-else
        :columns="columns"
        :data="tableData"
        :loading="tableLoading"
        :row-key="(row: any) => row.id"
        :scroll-x="800"
        striped
        class="custom-table"
      />

      <!-- 分页区域 (Req 1.5) -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ tableData.length }} 条记录
          </n-tag>
        </div>
      </div>
    </n-card>

    <!-- 新增/编辑弹窗 (Req 1.7) -->
    <n-modal v-model:show="showEditModal" preset="card" :title="editMode === 'add' ? '新增岗位' : '编辑岗位'" style="width: 480px; border-radius: 16px;">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-form-item label="岗位编码" path="postCode">
          <n-input v-model:value="formData.postCode" placeholder="如 ceo、hr" maxlength="64" :disabled="editMode === 'edit'" />
        </n-form-item>
        <n-form-item label="岗位名称" path="postName">
          <n-input v-model:value="formData.postName" placeholder="如 总经理、人事专员" maxlength="100" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="排序号">
              <n-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="状态">
              <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
                <template #checked>启用</template>
                <template #unchecked>禁用</template>
              </n-switch>
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="备注">
          <n-input v-model:value="formData.remark" type="textarea" :rows="2" placeholder="备注" />
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
import { ref, h, onMounted, computed } from 'vue'
import { NButton, NTag, NSpace, NIcon, NTooltip, useMessage, useDialog } from 'naive-ui'
import type { FormInst, SelectOption } from 'naive-ui'
import {
  BriefcaseOutline, AddOutline, CreateOutline, TrashOutline, SearchOutline,
  CheckmarkCircleOutline, CloseCircleOutline, ReorderFourOutline
} from '@vicons/ionicons5'
import PageHeaderStats from '@/components/common/PageHeaderStats.vue'
import type { StatItem } from '@/components/common/PageHeaderStats.vue'
import * as postApi from '@/api/org/post'
import type { Post } from '@/api/org/post'
import { formatDateTime } from '@/utils/format'

const message = useMessage()
const dialog = useDialog()

// ==================== 状态 ====================
const tableLoading = ref(false)
const tableData = ref<Post[]>([])
const showEditModal = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const saving = ref(false)
const formRef = ref<FormInst | null>(null)
const currentEditId = ref<number | null>(null)
const searchKeyword = ref('')
const searchStatus = ref<number | null>(null)

const formData = ref<Post>({
  postCode: '', postName: '', sortOrder: 0, status: 1, remark: ''
})

const formRules = {
  postCode: { required: true, message: '请输入岗位编码', trigger: 'blur' },
  postName: { required: true, message: '请输入岗位名称', trigger: 'blur' }
}

/** 状态筛选选项 */
const statusOptions: SelectOption[] = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// ==================== 统计 ====================
const statsItems = computed<StatItem[]>(() => {
  const total = tableData.value.length
  const enabled = tableData.value.filter((p: Post) => p.status === 1).length
  const disabled = total - enabled
  return [
    { value: total, label: '岗位总数', icon: BriefcaseOutline, type: 'primary' },
    { value: enabled, label: '启用岗位', icon: CheckmarkCircleOutline, type: 'success' },
    { value: disabled, label: '禁用岗位', icon: CloseCircleOutline, type: 'warning' },
    { value: total, label: '排序范围', icon: ReorderFourOutline, type: 'info' }
  ]
})

// ==================== 表格列 ====================
const columns = [
  { title: '岗位编码', key: 'postCode', width: 140,
    render: (row: Post) => h('code', { style: 'font-size: 13px' }, row.postCode)
  },
  { title: '岗位名称', key: 'postName', minWidth: 150 },
  { title: '排序号', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 80,
    render: (row: Post) => h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small', bordered: false },
      { default: () => row.status === 1 ? '启用' : '禁用' })
  },
  { title: '创建时间', key: 'createTime', width: 170,
    render: (row: Post) => formatDateTime(row.createTime)
  },
  { title: '操作', key: 'actions', width: 100, fixed: 'right' as const,
    render: (row: Post) =>
      h(NSpace, { justify: 'center', size: 4 }, {
        default: () => [
          h(NTooltip, null, {
            trigger: () => h(NButton, { size: 'small', quaternary: true, onClick: () => handleEdit(row) },
              { icon: () => h(NIcon, null, { default: () => h(CreateOutline) }) }),
            default: () => '编辑',
          }),
          h(NTooltip, null, {
            trigger: () => h(NButton, { size: 'small', type: 'error', quaternary: true, onClick: () => handleDelete(row) },
              { icon: () => h(NIcon, null, { default: () => h(TrashOutline) }) }),
            default: () => '删除',
          })
        ]
      })
  }
]

// ==================== 数据加载 ====================
async function loadPosts() {
  tableLoading.value = true
  try {
    const params: any = {}
    if (searchKeyword.value) params.postName = searchKeyword.value
    if (searchStatus.value !== null) params.status = searchStatus.value
    const res = await postApi.listPosts(params)
    tableData.value = (res as any).data || []
  } catch (e: any) {
    message.error(e.message || '加载岗位列表失败')
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  loadPosts()
}

function handleSearchReset() {
  searchKeyword.value = ''
  searchStatus.value = null
  loadPosts()
}

// ==================== CRUD ====================
function handleCreate() {
  editMode.value = 'add'
  currentEditId.value = null
  formData.value = { postCode: '', postName: '', sortOrder: 0, status: 1, remark: '' }
  showEditModal.value = true
}

function handleEdit(row: Post) {
  editMode.value = 'edit'
  currentEditId.value = row.id!
  formData.value = { ...row }
  showEditModal.value = true
}

function handleDelete(row: Post) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除岗位「${row.postName}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await postApi.deletePost(row.id!)
        message.success('删除成功')
        loadPosts()
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
      await postApi.createPost(formData.value)
      message.success('创建成功')
    } else {
      await postApi.updatePost(currentEditId.value!, formData.value)
      message.success('更新成功')
    }
    showEditModal.value = false
    loadPosts()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.post-page {
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

/* 表格样式 */
:deep(.n-data-table-th) {
  font-weight: 600 !important;
  background: var(--bg-tertiary, #fafbfc) !important;
  color: var(--text-secondary) !important;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background-color: var(--bg-hover, #f8fafc) !important;
}

/* 响应式 */
@media (max-width: 768px) {
  :deep(.page-header-stats) {
    flex-direction: column;
  }
}
</style>
