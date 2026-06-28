<template>
  <!-- 新建/编辑采集任务模态框 -->
  <n-modal v-model:show="showModal" preset="card" :title="modalTitle" style="width: 800px">
    <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100px">
      <n-form-item label="任务名称" path="taskName">
        <n-input v-model:value="form.taskName" placeholder="请输入任务名称" />
      </n-form-item>

      <n-divider title-placement="left">源配置</n-divider>

      <n-form-item label="源数据源" path="dataSourceId">
        <n-select
v-model:value="form.dataSourceId" :options="dataSourceOptions" placeholder="请选择源数据源"
          :loading="dataSourceLoading" filterable clearable @update:value="handleSourceDataSourceChange" />
        <template #feedback>
          <div class="form-hint">
            如果数据源列表为空，请先在"数据源管理"中添加数据源
          </div>
        </template>
      </n-form-item>

      <n-form-item label="采集模式" path="collectMode">
        <n-select v-model:value="form.collectMode" :options="collectModeOptions" placeholder="请选择采集模式" />
      </n-form-item>

      <n-form-item v-if="form.collectMode === 'full' || form.collectMode === 'incremental'" label="源表名" path="tableName">
        <n-select
v-model:value="form.tableName" :options="sourceTableOptions" placeholder="请选择源表"
          filterable tag :loading="loadingSourceTables" :disabled="!form.dataSourceId" />
        <template #feedback>
          <div v-if="!form.dataSourceId" class="form-hint">请先选择源数据源</div>
        </template>
      </n-form-item>

      <n-form-item v-if="form.collectMode === 'custom'" label="自定义SQL" path="customSql">
        <n-input v-model:value="form.customSql" type="textarea" placeholder="请输入自定义SQL查询语句" :rows="4" />
      </n-form-item>

      <template v-if="form.collectMode === 'incremental'">
        <n-form-item label="增量字段" path="incrementalField">
          <n-input v-model:value="form.incrementalField" placeholder="如: update_time 或 id" />
        </n-form-item>
        <n-form-item label="增量字段类型" path="incrementalType">
          <n-select v-model:value="form.incrementalType" :options="incrementalTypeOptions" placeholder="请选择增量字段类型" />
        </n-form-item>
      </template>

      <n-divider title-placement="left">目标配置</n-divider>

      <n-form-item label="目标数据源" path="targetDataSourceId">
        <n-select
v-model:value="form.targetDataSourceId" :options="dataSourceOptions" placeholder="不选择则导入到本地数据库"
          :loading="dataSourceLoading" filterable clearable @update:value="handleTargetDataSourceChange" />
        <template #feedback>
          <div class="form-hint">为空时数据将导入到本地数据库（表名前缀：collected_）</div>
        </template>
      </n-form-item>

      <n-form-item label="目标表名" path="targetTableName">
        <n-select
v-model:value="form.targetTableName" :options="targetTableOptions" placeholder="选择现有表或输入新表名"
          filterable tag clearable :loading="loadingTargetTables" @update:value="checkTargetTableExists" />
        <template #feedback>
          <div class="form-hint">
            <span v-if="targetTableExists === true" style="color: var(--color-success);">✓ 表已存在，数据将追加到现有表</span>
            <span v-else-if="targetTableExists === false && form.targetTableName" style="color: var(--color-warning);">
              ⚠ 表不存在{{ form.autoCreateTable ? '，将自动创建' : '，请启用自动建表或手动创建' }}
            </span>
            <span v-else class="text-muted">可选择现有表或输入新表名</span>
          </div>
        </template>
      </n-form-item>

      <n-form-item label="自动建表">
        <n-switch v-model:value="form.autoCreateTable" />
        <template #feedback>
          <div class="form-hint">启用后如果目标表不存在将自动创建</div>
        </template>
      </n-form-item>

      <n-form-item label="批量大小">
        <n-input-number v-model:value="form.batchSize" placeholder="默认1000" :min="100" :max="10000" :step="100" style="width: 100%" />
      </n-form-item>

      <n-divider title-placement="left">重试配置</n-divider>

      <n-form-item label="最大重试">
        <n-input-number v-model:value="form.maxRetryCount" placeholder="0表示不重试" :min="0" :max="10" :step="1" style="width: 100%" />
        <template #feedback>
          <div class="form-hint">任务失败后自动重试的最大次数，0表示不重试</div>
        </template>
      </n-form-item>

      <n-form-item v-if="form.maxRetryCount && form.maxRetryCount > 0" label="重试间隔(秒)">
        <n-input-number v-model:value="form.retryInterval" placeholder="默认30秒" :min="5" :max="3600" :step="5" style="width: 100%" />
        <template #feedback>
          <div class="form-hint">重试之间的等待时间（秒）</div>
        </template>
      </n-form-item>

      <n-divider title-placement="left">定时任务配置</n-divider>

      <n-form-item label="启用定时">
        <n-switch v-model:value="form.scheduleEnabled" />
        <template #feedback>
          <div class="form-hint">启用后任务将按照设定的时间自动执行</div>
        </template>
      </n-form-item>

      <template v-if="form.scheduleEnabled">
        <n-form-item label="执行周期" path="cronExpression">
          <n-select
v-model:value="selectedCronPreset" :options="cronPresetOptions" placeholder="选择预设周期或自定义"
            style="width: 100%" @update:value="handleCronPresetChange" />
        </n-form-item>
        <n-form-item v-if="selectedCronPreset === 'custom'" label="Cron表达式" path="cronExpression">
          <n-input v-model:value="form.cronExpression" placeholder="如: 0 0 2 * * ? (每天凌晨2点)" />
          <template #feedback>
            <div class="form-hint">格式: 秒 分 时 日 月 周 [年]</div>
          </template>
        </n-form-item>
        <n-form-item label="任务描述">
          <n-input v-model:value="form.scheduleDescription" placeholder="如: 每天凌晨2点同步数据" />
        </n-form-item>
      </template>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="showModal = false">取消</n-button>
        <n-button type="primary" @click="handleSubmit">保存</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, watch } from 'vue'
import { useMessage, type FormInst } from 'naive-ui'
import { createCollectTask, updateCollectTask } from '@/api/dataCollect'
import { getDataSourceTables } from '@/api/dataSource'
import type { CollectTask, CollectTaskForm } from '@/types/collectTask'
import { handleApiError } from '@/utils/error'

const props = defineProps<{
  dataSourceOptions: Array<{ label: string; value: number }>
  dataSourceLoading: boolean
}>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const message = useMessage()
const showModal = ref(false)
const modalTitle = ref('新建采集任务')
const formRef = ref<FormInst | null>(null)

// 源表和目标表选项
const sourceTableOptions = ref<Array<{ label: string; value: string }>>([])
const targetTableOptions = ref<Array<{ label: string; value: string }>>([])
const loadingSourceTables = ref(false)
const loadingTargetTables = ref(false)
const targetTableExists = ref<boolean | null>(null)

const collectModeOptions = [
  { label: '全量采集', value: 'full' },
  { label: '增量采集', value: 'incremental' },
  { label: '自定义SQL', value: 'custom' }
]

const incrementalTypeOptions = [
  { label: '时间戳', value: 'timestamp' },
  { label: '自增ID', value: 'id' }
]

const selectedCronPreset = ref<string>('')
const cronPresetOptions = [
  { label: '每分钟执行（测试用）', value: '0 * * * * ?', description: '每分钟执行一次' },
  { label: '每5分钟执行', value: '0 */5 * * * ?', description: '每5分钟执行一次' },
  { label: '每30分钟执行', value: '0 */30 * * * ?', description: '每30分钟执行一次' },
  { label: '每小时执行', value: '0 0 * * * ?', description: '每小时整点执行' },
  { label: '每天凌晨2点', value: '0 0 2 * * ?', description: '每天凌晨2:00执行' },
  { label: '每天凌晨6点', value: '0 0 6 * * ?', description: '每天凌晨6:00执行' },
  { label: '每天中午12点', value: '0 0 12 * * ?', description: '每天中午12:00执行' },
  { label: '每周一凌晨2点', value: '0 0 2 ? * MON', description: '每周一凌晨2:00执行' },
  { label: '每月1号凌晨2点', value: '0 0 2 1 * ?', description: '每月1号凌晨2:00执行' },
  { label: '自定义', value: 'custom', description: '自定义Cron表达式' }
]

const form = reactive<CollectTaskForm>({
  id: null,
  taskName: '',
  dataSourceId: null,
  targetDataSourceId: null,
  tableName: '',
  targetTableName: '',
  collectMode: 'full',
  customSql: '',
  incrementalField: '',
  incrementalType: 'timestamp',
  fieldMapping: '',
  transformRules: '',
  batchSize: 1000,
  autoCreateTable: true,
  scheduleEnabled: false,
  cronExpression: '',
  scheduleDescription: '',
  maxRetryCount: 0,
  retryInterval: 30
})

const rules = {
  taskName: { required: true, message: '请输入任务名称', trigger: 'blur' },
  dataSourceId: [{
    required: true, message: '请选择数据源', trigger: ['change', 'blur'],
    validator: (_rule: any, value: any) => {
      if (!value || value === null) return new Error('请选择数据源')
      if (typeof value !== 'number' || value <= 0) return new Error('数据源选择无效，请重新选择')
      const exists = props.dataSourceOptions.some(opt => opt.value === value)
      if (!exists) return new Error('数据源不存在，请重新选择')
      return true
    }
  }],
  tableName: { required: true, message: '请输入表名', trigger: 'blur' }
}

const handleCronPresetChange = (value: string) => {
  if (value && value !== 'custom') {
    form.cronExpression = value
    const preset = cronPresetOptions.find(p => p.value === value)
    if (preset) form.scheduleDescription = preset.description
  } else if (value === 'custom') {
    form.cronExpression = ''
    form.scheduleDescription = ''
  }
}

const handleSourceDataSourceChange = async (value: number | null) => {
  form.tableName = ''
  sourceTableOptions.value = []
  if (!value) return
  loadingSourceTables.value = true
  try {
    const res = await getDataSourceTables(value)
    const tables = res.data || []
    sourceTableOptions.value = tables.map((t: any) => ({ label: t.tableName || t, value: t.tableName || t }))
  } catch { message.error('加载源表列表失败') }
  finally { loadingSourceTables.value = false }
}

const handleTargetDataSourceChange = async (value: number | null) => {
  form.targetTableName = ''
  targetTableOptions.value = []
  targetTableExists.value = null
  if (!value) return
  loadingTargetTables.value = true
  try {
    const res = await getDataSourceTables(value)
    const tables = res.data || []
    targetTableOptions.value = tables.map((t: any) => ({ label: t.tableName || t, value: t.tableName || t }))
  } catch { message.error('加载目标表列表失败') }
  finally { loadingTargetTables.value = false }
}

const checkTargetTableExists = (tableName: string | null) => {
  if (!tableName) { targetTableExists.value = null; return }
  targetTableExists.value = targetTableOptions.value.some(t => t.value === tableName)
}

const resetForm = () => {
  Object.assign(form, {
    id: null, taskName: '', dataSourceId: null, targetDataSourceId: null,
    tableName: '', targetTableName: '', collectMode: 'full', customSql: '',
    incrementalField: '', incrementalType: 'timestamp', fieldMapping: '',
    transformRules: '', batchSize: 1000, autoCreateTable: true,
    scheduleEnabled: false, cronExpression: '', scheduleDescription: '',
    maxRetryCount: 0, retryInterval: 30
  })
  targetTableExists.value = null
  sourceTableOptions.value = []
  targetTableOptions.value = []
  selectedCronPreset.value = ''
}

const openCreate = () => {
  modalTitle.value = '新建采集任务'
  resetForm()
  if (props.dataSourceOptions.length === 0) {
    message.warning('暂无可用数据源，请先在"数据源管理"中添加数据源')
  }
  showModal.value = true
}

const openEdit = (row: CollectTask) => {
  modalTitle.value = '编辑采集任务'
  targetTableExists.value = null
  Object.assign(form, {
    id: row.id, taskName: row.taskName, dataSourceId: row.dataSourceId,
    targetDataSourceId: row.targetDataSourceId || null, tableName: row.tableName,
    targetTableName: row.targetTableName || '', collectMode: row.collectMode || 'full',
    customSql: row.customSql || '', incrementalField: row.incrementalField || '',
    incrementalType: row.incrementalType || 'timestamp', fieldMapping: row.fieldMapping || '',
    transformRules: row.transformRules || '', batchSize: row.batchSize || 1000,
    autoCreateTable: row.autoCreateTable !== undefined ? row.autoCreateTable : true,
    scheduleEnabled: row.scheduleEnabled || false, cronExpression: row.cronExpression || '',
    scheduleDescription: row.scheduleDescription || '', maxRetryCount: row.maxRetryCount || 0,
    retryInterval: row.retryInterval || 30
  })
  if (row.cronExpression) {
    const preset = cronPresetOptions.find(p => p.value === row.cronExpression)
    selectedCronPreset.value = preset ? row.cronExpression : 'custom'
  } else { selectedCronPreset.value = '' }

  if (row.dataSourceId && !props.dataSourceOptions.some(opt => opt.value === row.dataSourceId)) {
    message.warning(`原数据源"${row.dataSourceName || row.dataSourceId}"已不存在，请重新选择数据源`)
    form.dataSourceId = null
  }
  if (row.targetDataSourceId && !props.dataSourceOptions.some(opt => opt.value === row.targetDataSourceId)) {
    message.warning(`原目标数据源"${row.targetDataSourceName || row.targetDataSourceId}"已不存在，请重新选择`)
    form.targetDataSourceId = null
  }
  showModal.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (errors: any) => {
    if (!errors) {
      try {
        if (form.id) {
          await updateCollectTask(form)
          message.success('更新成功')
        } else {
          await createCollectTask(form)
          message.success('创建成功')
        }
        showModal.value = false
        emit('saved')
      } catch (error) {
        const errorMsg = handleApiError(error, form.id ? '更新采集任务' : '创建采集任务')
        message.error(errorMsg)
      }
    }
  })
}

defineExpose({ openCreate, openEdit })
</script>
