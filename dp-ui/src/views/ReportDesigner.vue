<template>
  <DesktopOnlyTip v-if="isMobileView" title="报表设计器" desc="报表设计器需要更大的屏幕空间进行SQL编辑和字段配置，请在电脑端打开。" />
  <div v-else class="report-designer-page">
    <!-- 工具栏：头部 + 统计 -->
    <ReportDesignerToolbar
      :is-edit-mode="isEditMode"
      :report-name="form.reportName"
      :can-undo="canUndo"
      :can-redo="canRedo"
      :submitting="submitting"
      :data-source-count="dataSourceOptions.length"
      :fields-count="form.fields?.length || 0"
      :visible-fields-count="visibleFieldsCount"
      :sql-validated="sqlValidated"
      @undo="handleUndo"
      @redo="handleRedo"
      @cancel="handleCancel"
      @submit="handleSubmit"
    />

    <!-- 主要内容区 -->
    <div class="designer-content">
      <!-- 属性面板：基本信息 + SQL编辑 + 参数配置 -->
      <ReportDesignerProperties
        ref="propertiesRef"
        :form="form"
        :query-mode="queryMode"
        :sql-validated="sqlValidated"
        :testing-sql="testingSQL"
        :field-loading="fieldLoading"
        :data-source-options="dataSourceOptions"
        :data-source-loading="dataSourceLoading"
        :dict-type-options="dictTypeOptions"
        @update:query-mode="handleQueryModeChange"
        @data-source-change="handleDataSourceChange"
        @test-sql="handleTestSql"
        @auto-get-fields="handleAutoGetFields"
        @visual-sql-update="handleVisualSqlUpdate"
      />

      <!-- 画布区域：字段配置 -->
      <ReportDesignerCanvas
        :fields="form.fields || []"
        :field-loading="fieldLoading"
        :dict-type-options="dictTypeOptions"
        @update:fields="handleFieldsUpdate"
        @auto-get-fields="handleAutoGetFields"
        @field-drag-end="handleFieldDragEnd"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMessage, type FormInst } from 'naive-ui'
import DesktopOnlyTip from '@/components/mobile/DesktopOnlyTip.vue'
import { useAppStore } from '@/stores/app'
import ReportDesignerToolbar from './ReportDesignerToolbar.vue'
import ReportDesignerProperties from './ReportDesignerProperties.vue'
import ReportDesignerCanvas from './ReportDesignerCanvas.vue'
import { getDataSourceList } from '@/api/dataSource'
import {
  createReportDefinition,
  updateReportDefinition,
  getReportDefinitionById,
  testSqlAndGetFields
} from '@/api/reportDefinition'
import { getDictTypes } from '@/api/system/dataDictionary'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { logger } from '@/utils/logger'
import type { DataSource } from '@/types/dataSource'
import type { ReportDefinitionForm, ReportField, ReportParam } from '@/types/reportDefinition'
import { useTabsStore } from '@/stores/tabs'
import { useHistory } from '@/composables/useHistory'

const appStore = useAppStore()
const isMobileView = computed(() => appStore.isMobileView)
const router = useRouter()
const route = useRoute()
const message = useMessage()
const tabsStore = useTabsStore()
initMessage(message)

const propertiesRef = ref<InstanceType<typeof ReportDesignerProperties> | null>(null)
const submitting = ref(false)
const fieldLoading = ref(false)
const testingSQL = ref(false)
const sqlValidated = ref(false)
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const dataSourceList = ref<DataSource[]>([])
const dataSourceLoading = ref(false)
const queryMode = ref<'sql' | 'visual'>('sql')
const dictTypeOptions = ref<Array<{ label: string; value: string }>>([])

// ==================== 表单数据 ====================
const form = reactive<ReportDefinitionForm>({
  id: null,
  reportName: '',
  reportCode: '',
  dataSourceId: 0,
  sqlContent: '',
  description: '',
  status: 1,
  reportType: 'sql',
  allowExportExcel: 1,
  allowExportPdf: 1,
  allowPrint: 1,
  watermarkType: 'none',
  pdfWatermark: '',
  fields: [],
  params: []
})

// ==================== 计算属性 ====================
const isEditMode = computed(() => {
  const id = route.params["id"]
  return !!(id && id !== 'new')
})

const visibleFieldsCount = computed(() => {
  return (form.fields || []).filter(f => f.isVisible !== 0).length
})

// ==================== 撤销/重做功能 ====================
interface FormSnapshot {
  reportName: string
  dataSourceId: number
  sqlContent: string
  description: string
  reportType: string
  allowExportExcel: number
  allowExportPdf: number
  allowPrint: number
  watermarkType: string
  pdfWatermark: string
  fields: ReportField[]
  params: ReportParam[]
}

const {
  canUndo,
  canRedo,
  push: pushHistory,
  undo: undoHistory,
  redo: redoHistory,
} = useHistory<FormSnapshot>({ maxSize: 50 })

const isApplyingHistory = ref(false)

const createFormSnapshot = (): FormSnapshot => ({
  reportName: form.reportName,
  dataSourceId: form.dataSourceId,
  sqlContent: form.sqlContent,
  description: form.description,
  reportType: form.reportType,
  allowExportExcel: form.allowExportExcel,
  allowExportPdf: form.allowExportPdf,
  allowPrint: form.allowPrint,
  watermarkType: form.watermarkType || 'none',
  pdfWatermark: form.pdfWatermark || '',
  fields: form.fields ? [...form.fields.map(f => ({ ...f }))] : [],
  params: form.params ? [...form.params.map(p => ({ ...p }))] : []
} as FormSnapshot)

const applyFormSnapshot = (snapshot: FormSnapshot): void => {
  isApplyingHistory.value = true
  form.reportName = snapshot.reportName
  form.dataSourceId = snapshot.dataSourceId
  form.sqlContent = snapshot.sqlContent
  form.description = snapshot.description
  form.reportType = snapshot.reportType
  form.allowExportExcel = snapshot.allowExportExcel
  form.allowExportPdf = snapshot.allowExportPdf
  form.allowPrint = snapshot.allowPrint
  form.watermarkType = snapshot.watermarkType || 'none'
  form.pdfWatermark = snapshot.pdfWatermark || ''
  form.fields = snapshot.fields ? [...snapshot.fields.map(f => ({ ...f }))] : []
  form.params = snapshot.params ? [...snapshot.params.map(p => ({ ...p }))] : []
  queryMode.value = form.reportType === 'visual' ? 'visual' : 'sql'
  setTimeout(() => { isApplyingHistory.value = false }, 0)
}

const recordHistory = (description?: string): void => {
  if (!isApplyingHistory.value) {
    pushHistory(createFormSnapshot(), description)
  }
}

const handleUndo = (): void => {
  if (!canUndo.value) return
  const snapshot = undoHistory()
  if (snapshot) {
    applyFormSnapshot(snapshot)
    message.info('已撤销')
  }
}

const handleRedo = (): void => {
  if (!canRedo.value) return
  const snapshot = redoHistory()
  if (snapshot) {
    applyFormSnapshot(snapshot)
    message.info('已重做')
  }
}

// ==================== Watchers ====================
watch(queryMode, (newMode) => {
  form.reportType = newMode
})

watch(() => form.watermarkType, (newType) => {
  if (newType === 'none' || newType === 'user_ip') {
    form.pdfWatermark = ''
  }
})

let sqlChangeTimeout: ReturnType<typeof setTimeout> | null = null

const parseSqlParams = (sql: string): string[] => {
  const regex = /\$\{(\w+)\}/g
  const names: string[] = []
  let match: RegExpExecArray | null
  while ((match = regex.exec(sql)) !== null) {
    if (match[1] && !names.includes(match[1])) names.push(match[1])
  }
  return names
}

watch(() => form.sqlContent, (newSql, oldSql) => {
  const names = parseSqlParams(newSql || '')
  const existing = form.params || []
  form.params = names.map(name => {
    const found = existing.find(p => p.name === name)
    if (found) return found
    return {
      name,
      label: name,
      inputType: 'text' as const,
      required: false,
      defaultValue: '',
      dictType: '' as string,
      options: [] as { label: string; value: string }[]
    }
  })
  if (oldSql !== undefined && newSql !== oldSql) {
    if (sqlChangeTimeout) clearTimeout(sqlChangeTimeout)
    sqlChangeTimeout = setTimeout(() => {
      if (!isApplyingHistory.value) recordHistory('修改SQL语句')
    }, 1000)
  }
})

// ==================== 事件处理 ====================
const handleQueryModeChange = (mode: 'sql' | 'visual') => {
  queryMode.value = mode
}

const handleVisualSqlUpdate = (sql: string) => {
  form.sqlContent = sql
  sqlValidated.value = false
}

const handleDataSourceChange = () => {
  // 数据源改变时的处理
}

const handleFieldsUpdate = (fields: ReportField[]) => {
  form.fields = fields
}

const handleFieldDragEnd = () => {
  recordHistory('调整字段顺序')
  if (form.fields) {
    form.fields.forEach((field, index) => {
      field.sortOrder = index
    })
  }
}

// ==================== 键盘快捷键 ====================
const handleKeyDown = (event: KeyboardEvent): void => {
  const target = event.target as HTMLElement
  const isInInput = target.tagName === 'INPUT' ||
                    target.tagName === 'TEXTAREA' ||
                    target.isContentEditable
  if (isInInput) return

  if ((event.ctrlKey || event.metaKey) && event.key === 'z' && !event.shiftKey) {
    event.preventDefault()
    handleUndo()
    return
  }
  if ((event.ctrlKey || event.metaKey) && (event.key === 'y' || (event.key === 'z' && event.shiftKey))) {
    event.preventDefault()
    handleRedo()
    return
  }
}

// ==================== 数据加载 ====================
const loadDataSources = async () => {
  dataSourceLoading.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 1000 })
    let dataList: DataSource[] = []
    if (res && typeof res === 'object') {
      const resData = (res as any).data
      if (resData) {
        if (Array.isArray(resData)) {
          dataList = resData
        } else if (resData.list && Array.isArray(resData.list)) {
          dataList = resData.list
        }
      }
    }
    dataSourceList.value = dataList
    dataSourceOptions.value = dataList.map(ds => ({
      label: `${ds.name} (${ds.dbType || '未知'})`,
      value: ds.id
    }))
    if (dataSourceOptions.value.length === 0) {
      message.warning('暂无可用数据源，请先在"数据源管理"中添加数据源')
    }
  } catch (error) {
    const errorMsg = handleApiError(error, '加载数据源列表', '加载数据源列表失败，请检查网络连接')
    message.error(errorMsg)
    dataSourceOptions.value = []
  } finally {
    dataSourceLoading.value = false
  }
}

const loadDictTypes = async () => {
  try {
    const res: any = await getDictTypes()
    const types = res?.data || []
    dictTypeOptions.value = types.map((t: string) => ({ label: t, value: t }))
  } catch { /* ignore */ }
}

const loadReportDefinition = async (id: number) => {
  try {
    const res = await getReportDefinitionById(id)
    const report = (res as any).data
    form.id = report.id
    form.reportName = report.reportName
    form.reportCode = report.reportCode
    form.dataSourceId = report.dataSourceId
    form.sqlContent = report.sqlContent
    form.description = report.description || ''
    form.status = report.status
    form.reportType = report.reportType || 'sql'
    form.allowExportExcel = report.allowExportExcel ?? 1
    form.allowExportPdf = report.allowExportPdf ?? 1
    form.allowPrint = report.allowPrint ?? 1
    form.watermarkType = report.watermarkType || 'none'
    form.pdfWatermark = report.pdfWatermark || ''
    queryMode.value = form.reportType === 'visual' ? 'visual' : 'sql'
    form.fields = report.fields || []
    if (report.params && typeof report.params === 'string') {
      try { form.params = JSON.parse(report.params) } catch { form.params = [] }
    } else {
      form.params = report.params || []
    }
    sqlValidated.value = true
  } catch (error) {
    message.error('加载报表定义失败')
  }
}

// ==================== SQL 测试与字段获取 ====================
const handleTestSql = async () => {
  if (!form.dataSourceId) {
    message.warning('请先选择数据源')
    return
  }
  if (!form.sqlContent || !form.sqlContent.trim()) {
    message.warning('请输入SQL查询语句')
    return
  }
  testingSQL.value = true
  try {
    message.loading('正在测试SQL...', { duration: 0 })
    const res = await testSqlAndGetFields(form.dataSourceId, form.sqlContent.trim())
    const fields = Array.isArray(res.data) ? res.data : []
    message.destroyAll()
    if (fields.length > 0) {
      sqlValidated.value = true
      message.success(`SQL测试成功！检测到${fields.length}个字段`)
    } else {
      message.warning('SQL测试完成，但未检测到字段，请检查SQL语句')
    }
  } catch (error: any) {
    message.destroyAll()
    sqlValidated.value = false
    const errorMsg = handleApiError(error, 'SQL测试', 'SQL测试失败，请检查SQL语句和数据源配置')
    message.error(errorMsg)
  } finally {
    testingSQL.value = false
  }
}

const handleAutoGetFields = async () => {
  if (!form.dataSourceId) {
    message.warning('请先选择数据源')
    return
  }
  if (!form.sqlContent || !form.sqlContent.trim()) {
    message.warning('请输入SQL查询语句')
    return
  }
  recordHistory('获取字段前')
  fieldLoading.value = true
  try {
    message.loading('正在获取字段信息...', { duration: 0 })
    const res = await testSqlAndGetFields(form.dataSourceId, form.sqlContent.trim())
    const fields = Array.isArray(res.data) ? res.data : []
    if (fields.length === 0) {
      message.destroyAll()
      fieldLoading.value = false
      message.warning('未能获取到字段信息，请检查SQL语句是否正确')
      return
    }
    form.fields = fields.map((field: ReportField, index: number) => ({
      ...field,
      sortOrder: field.sortOrder ?? index,
      isVisible: field.isVisible ?? 1,
      fieldLabel: field.fieldLabel || field.fieldName,
      align: field.align || 'left'
    }))
    sqlValidated.value = true
    message.destroyAll()
    fieldLoading.value = false
    message.success(`成功获取${fields.length}个字段信息！字段配置已自动填充，您可以在表格中修改显示名称。`)
  } catch (error: any) {
    message.destroyAll()
    fieldLoading.value = false
    const errorMsg = handleApiError(error, '获取字段信息', '获取字段信息失败，请检查SQL语句和数据源配置')
    message.error(errorMsg)
  }
}

// ==================== 提交与取消 ====================
const handleSubmit = async () => {
  const formInst = propertiesRef.value?.getFormRef?.() as FormInst | null
  if (!formInst) return

  try {
    await formInst.validate()
    submitting.value = true
    if (!form.reportCode) {
      form.reportCode = 'RPT_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8).toUpperCase()
    }
    const submitData = {
      ...form,
      params: form.params && form.params.length > 0 ? JSON.stringify(form.params) : null,
      watermarkType: form.watermarkType || 'none',
      pdfWatermark: form.watermarkType === 'custom' ? (form.pdfWatermark || '') : ''
    }
    if (form.id) {
      await updateReportDefinition(submitData as any)
      message.success('更新成功')
    } else {
      await createReportDefinition(submitData as any)
      message.success('创建成功')
    }
    setTimeout(() => {
      router.replace('/report-manage').catch((err) => {
        if (err.name !== 'NavigationDuplicated') {
          logger.error('路由跳转失败', err)
        }
      })
    }, 500)
  } catch (error: any) {
    const errorMsg = handleApiError(error, form.id ? '更新报表' : '创建报表', '保存失败')
    message.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  tabsStore.replaceTab(route.fullPath, {
    key: '/report-manage',
    title: '报表管理',
    closable: true
  })
  router.replace('/report-manage')
}

// ==================== 生命周期 ====================
onMounted(async () => {
  window.addEventListener('keydown', handleKeyDown)
  await loadDataSources()
  await loadDictTypes()
  const id = route.params["id"]
  if (id && id !== 'new') {
    await loadReportDefinition(Number(id))
  }
  setTimeout(() => {
    pushHistory(createFormSnapshot(), '初始状态')
  }, 100)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.report-designer-page {
  padding: 0;
}

.designer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>
