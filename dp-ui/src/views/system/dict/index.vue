<template>
  <div class="dict-page">
    <!-- 顶部统计 -->
    <PageHeaderStats :items="statsItems" />

    <div class="dict-layout">
      <!-- 左侧字典类型面板 -->
      <div class="dict-sidebar">
        <n-card size="small" :bordered="true">
          <template #header>
            <div class="sidebar-header">
              <n-icon size="18" color="#2563EB"><BookOutline /></n-icon>
              <span>字典类型</span>
            </div>
          </template>
          <template #header-extra>
            <n-button size="tiny" type="primary" quaternary @click="handleCreateType">
              <template #icon><n-icon size="16"><AddOutline /></n-icon></template>
            </n-button>
          </template>

          <n-input
            v-model:value="typeSearch"
            placeholder="搜索类型..."
            size="small"
            clearable
            style="margin-bottom: 8px"
          >
            <template #prefix><n-icon size="14"><SearchOutline /></n-icon></template>
          </n-input>

          <n-spin :show="typeLoading">
            <div class="type-list">
              <div
                v-for="item in filteredTypes"
                :key="item.id"
                class="type-item"
                :class="{ active: selectedType?.id === item.id }"
                @click="selectType(item)"
              >
                <div class="type-item-info">
                  <span class="type-name">{{ item.dictName }}</span>
                  <span class="type-code">{{ item.dictCode }}</span>
                </div>
                <div class="type-item-actions">
                  <n-tag size="tiny" :type="item.status === 1 ? 'success' : 'error'">
                    {{ item.status === 1 ? '启用' : '禁用' }}
                  </n-tag>
                  <n-dropdown :options="typeActionOptions" trigger="click" @select="(key: string) => handleTypeAction(key, item)">
                    <n-button size="tiny" quaternary style="padding: 0 2px;">
                      <n-icon size="14"><EllipsisVerticalOutline /></n-icon>
                    </n-button>
                  </n-dropdown>
                </div>
              </div>
              <div v-if="filteredTypes.length === 0 && !typeLoading" class="type-empty">暂无字典类型</div>
            </div>
          </n-spin>
        </n-card>
      </div>

      <!-- 右侧字典数据面板 -->
      <div class="dict-content">
        <n-card size="small" :bordered="true">
          <template #header>
            <div class="card-header-custom">
              <n-icon size="20" color="var(--color-primary)" class="header-icon"><ListOutline /></n-icon>
              <span>{{ selectedType ? `${selectedType.dictName}（${selectedType.dictCode}）` : '请选择字典类型' }}</span>
            </div>
          </template>
          <template #header-extra>
            <n-space v-if="selectedType">
              <n-button type="primary" @click="handleCreateData">
                <template #icon><n-icon><AddOutline /></n-icon></template>
                新增数据项
              </n-button>
            </n-space>
          </template>

          <!-- Query_Form: 搜索筛选 -->
          <n-form v-if="selectedType" class="query-form" inline>
            <n-form-item>
              <n-input
                v-model:value="dataSearch"
                placeholder="搜索标签/值..."
                clearable
                style="width: 200px"
                @keyup.enter="handleDataSearch"
                @clear="handleDataSearchReset"
              >
                <template #prefix><n-icon><SearchOutline /></n-icon></template>
              </n-input>
            </n-form-item>
            <n-form-item class="query-form-actions">
              <n-button type="primary" @click="handleDataSearch">搜索</n-button>
              <n-button @click="handleDataSearchReset">重置</n-button>
            </n-form-item>
          </n-form>

          <div v-if="!selectedType" class="empty-placeholder">
            <n-empty description="请在左侧选择一个字典类型" />
          </div>

          <n-data-table
            v-else
            :columns="dataColumns"
            :data="filteredDataList"
            :loading="dataLoading"
            :row-key="(row: any) => row.id"
            :scroll-x="800"
            striped
            class="custom-table"
          />

          <!-- Pagination_Wrapper -->
          <div v-if="selectedType" class="pagination-wrapper">
            <div class="pagination-info">
              <n-tag type="info" size="small" round>
                共 {{ filteredDataList.length }} 条记录
              </n-tag>
            </div>
          </div>
        </n-card>
      </div>
    </div>

    <!-- 字典类型编辑弹窗 -->
    <n-modal v-model:show="showTypeModal" preset="card" :title="typeEditMode === 'add' ? '新增字典类型' : '编辑字典类型'" style="width: 480px; border-radius: 16px;">
      <n-form ref="typeFormRef" :model="typeFormData" :rules="typeFormRules" label-placement="left" label-width="100px">
        <n-form-item label="类型编码" path="dictCode">
          <n-input v-model:value="typeFormData.dictCode" placeholder="如 sys_status" maxlength="100" :disabled="typeEditMode === 'edit'" />
        </n-form-item>
        <n-form-item label="类型名称" path="dictName">
          <n-input v-model:value="typeFormData.dictName" placeholder="如 系统状态" maxlength="100" />
        </n-form-item>
        <n-form-item label="状态">
          <n-switch v-model:value="typeFormData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="typeFormData.remark" type="textarea" :rows="2" placeholder="备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTypeModal = false">取消</n-button>
          <n-button type="primary" :loading="typeSaving" @click="handleSaveType">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 字典数据编辑弹窗 -->
    <n-modal v-model:show="showDataModal" preset="card" :title="dataEditMode === 'add' ? '新增数据项' : '编辑数据项'" style="width: 600px; border-radius: 16px;">
      <n-form ref="dataFormRef" :model="dataFormData" :rules="dataFormRules" label-placement="left" label-width="100px">
        <n-form-item label="字典标签" path="label">
          <n-input v-model:value="dataFormData.label" placeholder="显示名称" maxlength="100" />
        </n-form-item>
        <n-form-item label="字典值" path="value">
          <n-input v-model:value="dataFormData.value" placeholder="实际值" maxlength="100" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="排序号">
              <n-input-number v-model:value="dataFormData.sortOrder" :min="0" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="样式类名">
              <n-input v-model:value="dataFormData.cssClass" placeholder="可选" maxlength="100" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="状态">
          <n-switch v-model:value="dataFormData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="dataFormData.remark" type="textarea" :rows="2" placeholder="备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showDataModal = false">取消</n-button>
          <n-button type="primary" :loading="dataSaving" @click="handleSaveData">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, onMounted, computed } from 'vue'
import { NButton, NTag, NSpace, NIcon, useMessage, useDialog } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import {
  BookOutline, SearchOutline, AddOutline, CreateOutline, TrashOutline,
  ListOutline, EllipsisVerticalOutline,
  CheckmarkCircleOutline, CloseCircleOutline, DocumentTextOutline
} from '@vicons/ionicons5'
import PageHeaderStats from '@/components/common/PageHeaderStats.vue'
import type { StatItem } from '@/components/common/PageHeaderStats.vue'
import * as dictApi from '@/api/system/dict'
import type { DictType, DictData } from '@/api/system/dict'
import { formatDateTime } from '@/utils/format'

const message = useMessage()
const dialog = useDialog()

// ==================== 字典类型相关 ====================
const typeLoading = ref(false)
const typeList = ref<DictType[]>([])
const typeSearch = ref('')
const selectedType = ref<DictType | null>(null)
const showTypeModal = ref(false)
const typeEditMode = ref<'add' | 'edit'>('add')
const typeSaving = ref(false)
const typeFormRef = ref<FormInst | null>(null)
const currentTypeEditId = ref<number | null>(null)

const typeFormData = ref<DictType>({
  dictCode: '', dictName: '', status: 1, remark: ''
})

const typeFormRules = {
  dictCode: { required: true, message: '请输入类型编码', trigger: 'blur' },
  dictName: { required: true, message: '请输入类型名称', trigger: 'blur' }
}

// ==================== 字典数据相关 ====================
const dataLoading = ref(false)
const dataList = ref<DictData[]>([])
const dataSearch = ref('')
const showDataModal = ref(false)
const dataEditMode = ref<'add' | 'edit'>('add')
const dataSaving = ref(false)
const dataFormRef = ref<FormInst | null>(null)
const currentDataEditId = ref<number | null>(null)

const dataFormData = ref<DictData>({
  dictCode: '', label: '', value: '', sortOrder: 0, cssClass: '', status: 1, remark: ''
})

const dataFormRules = {
  label: { required: true, message: '请输入字典标签', trigger: 'blur' },
  value: { required: true, message: '请输入字典值', trigger: 'blur' }
}

// ==================== 统计 ====================
const statsItems = computed<StatItem[]>(() => {
  const enabledTypes = typeList.value.filter((t: DictType) => t.status === 1).length
  const disabledTypes = typeList.value.length - enabledTypes
  return [
    { value: typeList.value.length, label: '字典类型', icon: BookOutline, type: 'primary' },
    { value: dataList.value.length, label: '当前数据项', icon: DocumentTextOutline, type: 'info' },
    { value: enabledTypes, label: '启用类型', icon: CheckmarkCircleOutline, type: 'success' },
    { value: disabledTypes, label: '禁用类型', icon: CloseCircleOutline, type: 'warning' }
  ]
})

// ==================== 左侧类型列表 ====================
const filteredTypes = computed(() => {
  if (!typeSearch.value) return typeList.value
  const kw = typeSearch.value.toLowerCase()
  return typeList.value.filter((t: DictType) =>
    t.dictCode.toLowerCase().includes(kw) || t.dictName.toLowerCase().includes(kw)
  )
})

// 类型操作下拉菜单
const typeActionOptions = [
  { label: '编辑', key: 'edit', icon: () => h(NIcon, null, { default: () => h(CreateOutline) }) },
  { label: '删除', key: 'delete', icon: () => h(NIcon, null, { default: () => h(TrashOutline) }) }
]

// ==================== 右侧数据列表 ====================
const filteredDataList = computed(() => {
  if (!dataSearch.value) return dataList.value
  const kw = dataSearch.value.toLowerCase()
  return dataList.value.filter((d: DictData) =>
    d.label.toLowerCase().includes(kw) || d.value.toLowerCase().includes(kw)
  )
})

const dataColumns = [
  { title: '字典标签', key: 'label', minWidth: 120 },
  { title: '字典值', key: 'value', width: 120,
    render: (row: DictData) => h('code', { style: 'font-size: 13px' }, row.value)
  },
  { title: '排序号', key: 'sortOrder', width: 80 },
  { title: '样式类名', key: 'cssClass', width: 120,
    render: (row: DictData) => row.cssClass || '-'
  },
  { title: '状态', key: 'status', width: 80,
    render: (row: DictData) => h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' },
      { default: () => row.status === 1 ? '启用' : '禁用' })
  },
  { title: '创建时间', key: 'createTime', width: 170,
    render: (row: DictData) => formatDateTime(row.createTime)
  },
  { title: '操作', key: 'actions', width: 100, fixed: 'right' as const,
    render: (row: DictData) =>
      h(NSpace, { size: 4 }, {
        default: () => [
          h(NButton, { size: 'small', quaternary: true, onClick: () => handleEditData(row) },
            { icon: () => h(NIcon, null, { default: () => h(CreateOutline) }) }),
          h(NButton, { size: 'small', type: 'error', quaternary: true, onClick: () => handleDeleteData(row) },
            { icon: () => h(NIcon, null, { default: () => h(TrashOutline) }) })
        ]
      })
  }
]

// ==================== 数据加载 ====================

async function loadDictTypes() {
  typeLoading.value = true
  try {
    const res = await dictApi.listDictTypes()
    typeList.value = (res as any).data || []
  } catch (e: any) {
    message.error(e.message || '加载字典类型失败')
  } finally {
    typeLoading.value = false
  }
}

async function loadDictData() {
  if (!selectedType.value) {
    dataList.value = []
    return
  }
  dataLoading.value = true
  try {
    const res = await dictApi.listDictData(selectedType.value.dictCode)
    dataList.value = (res as any).data || []
  } catch (e: any) {
    message.error(e.message || '加载字典数据失败')
  } finally {
    dataLoading.value = false
  }
}

function selectType(item: DictType) {
  selectedType.value = item
  dataSearch.value = ''
  loadDictData()
}

/** 搜索数据项 */
function handleDataSearch() {
  // filteredDataList is computed, just triggers reactivity
}

/** 重置数据项搜索 */
function handleDataSearchReset() {
  dataSearch.value = ''
}

// ==================== 字典类型 CRUD ====================

function handleCreateType() {
  typeEditMode.value = 'add'
  currentTypeEditId.value = null
  typeFormData.value = { dictCode: '', dictName: '', status: 1, remark: '' }
  showTypeModal.value = true
}

function handleEditType(item: DictType) {
  typeEditMode.value = 'edit'
  currentTypeEditId.value = item.id!
  typeFormData.value = { ...item }
  showTypeModal.value = true
}

function handleDeleteType(item: DictType) {
  dialog.warning({
    title: '确认删除',
    content: `删除字典类型「${item.dictName}」将同时删除其下所有数据项，确定继续？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await dictApi.deleteDictType(item.id!)
        message.success('删除成功')
        if (selectedType.value?.id === item.id) {
          selectedType.value = null
          dataList.value = []
        }
        loadDictTypes()
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

function handleTypeAction(key: string, item: DictType) {
  if (key === 'edit') handleEditType(item)
  else if (key === 'delete') handleDeleteType(item)
}

async function handleSaveType() {
  await typeFormRef.value?.validate()
  typeSaving.value = true
  try {
    if (typeEditMode.value === 'add') {
      await dictApi.createDictType(typeFormData.value)
      message.success('创建成功')
    } else {
      await dictApi.updateDictType(currentTypeEditId.value!, typeFormData.value)
      message.success('更新成功')
    }
    showTypeModal.value = false
    loadDictTypes()
    // 如果编辑的是当前选中的类型，刷新右侧数据
    if (selectedType.value && currentTypeEditId.value === selectedType.value.id) {
      selectedType.value = { ...selectedType.value, ...typeFormData.value }
    }
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    typeSaving.value = false
  }
}

// ==================== 字典数据 CRUD ====================

function handleCreateData() {
  if (!selectedType.value) return
  dataEditMode.value = 'add'
  currentDataEditId.value = null
  dataFormData.value = {
    dictCode: selectedType.value.dictCode,
    label: '', value: '', sortOrder: 0, cssClass: '', status: 1, remark: ''
  }
  showDataModal.value = true
}

function handleEditData(row: DictData) {
  dataEditMode.value = 'edit'
  currentDataEditId.value = row.id!
  dataFormData.value = { ...row }
  showDataModal.value = true
}

function handleDeleteData(row: DictData) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除数据项「${row.label}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await dictApi.deleteDictData(row.id!)
        message.success('删除成功')
        loadDictData()
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    }
  })
}

async function handleSaveData() {
  await dataFormRef.value?.validate()
  dataSaving.value = true
  try {
    if (dataEditMode.value === 'add') {
      await dictApi.createDictData(dataFormData.value)
      message.success('创建成功')
    } else {
      await dictApi.updateDictData(currentDataEditId.value!, dataFormData.value)
      message.success('更新成功')
    }
    showDataModal.value = false
    loadDictData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    dataSaving.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadDictTypes()
})
</script>

<style scoped>
.dict-page {
  padding: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

.dict-layout {
  display: flex;
  gap: 16px;
  height: 100%;
}

.dict-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.type-list {
  max-height: calc(100vh - 340px);
  overflow-y: auto;
}

.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.type-item:hover {
  background: var(--color-primary-light, rgba(0, 102, 255, 0.06));
}

.type-item.active {
  background: var(--color-primary-light, rgba(0, 102, 255, 0.1));
  border-left: 3px solid var(--color-primary, #2563EB);
  padding-left: 9px;
}

.type-item-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}

.type-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-item.active .type-name {
  color: var(--color-primary, #2563EB);
}

.type-code {
  font-size: 11px;
  color: var(--text-tertiary, #999);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-item-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.type-empty {
  text-align: center;
  color: var(--text-tertiary, #999);
  padding: 20px;
  font-size: 13px;
}

.dict-content {
  flex: 1;
  min-width: 0;
}

.empty-placeholder {
  padding: 60px 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .dict-layout { flex-direction: column; }
  .dict-sidebar { width: 100%; max-height: 200px; }
  .main-card { border-radius: 14px !important; }
  .pagination-wrapper { flex-direction: column; gap: 8px; }
}
</style>
