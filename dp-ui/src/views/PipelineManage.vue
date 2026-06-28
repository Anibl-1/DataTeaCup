<template>
  <div class="pipeline-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">
          <n-icon :component="GitNetworkOutline" class="title-icon" />
          {{ t('pipelineManage.title') }}
        </h2>
        <p class="page-desc">{{ t('pipelineManage.desc') }}</p>
      </div>
      <n-button type="primary" size="large" @click="showCreateModal = true">
        <template #icon><n-icon :component="AddOutline" /></template>
        {{ t('pipelineManage.create') }}
      </n-button>
    </div>

    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" class="stats-row">
      <n-gi>
        <div class="stat-card stat-total">
          <div class="stat-icon"><n-icon :component="LayersOutline" size="28" /></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.total || 0 }}</div>
            <div class="stat-label">{{ t('pipelineManage.totalPipelines') }}</div>
          </div>
        </div>
      </n-gi>
      <n-gi>
        <div class="stat-card stat-published">
          <div class="stat-icon"><n-icon :component="CheckmarkCircleOutline" size="28" /></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.published || 0 }}</div>
            <div class="stat-label">{{ t('pipelineManage.published') }}</div>
          </div>
        </div>
      </n-gi>
      <n-gi>
        <div class="stat-card stat-draft">
          <div class="stat-icon"><n-icon :component="DocumentOutline" size="28" /></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.draft || 0 }}</div>
            <div class="stat-label">{{ t('pipelineManage.draft') }}</div>
          </div>
        </div>
      </n-gi>
      <n-gi>
        <div class="stat-card stat-scheduled">
          <div class="stat-icon"><n-icon :component="TimeOutline" size="28" /></div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.scheduled || 0 }}</div>
            <div class="stat-label">{{ t('pipelineManage.scheduled') }}</div>
          </div>
        </div>
      </n-gi>
    </n-grid>

    <!-- 搜索和筛选 -->
    <n-card class="filter-card">
      <n-space align="center" justify="space-between">
        <n-space>
          <n-input 
            v-model:value="searchKeyword" 
            :placeholder="t('pipelineManage.searchPlaceholder')" 
            clearable 
            style="width: 280px"
          >
            <template #prefix><n-icon :component="SearchOutline" /></template>
          </n-input>
          <n-select 
            v-model:value="searchType" 
            :placeholder="t('pipelineManage.pipelineType')" 
            clearable 
            style="width: 140px" 
            :options="typeOptions" 
          />
          <n-select 
            v-model:value="searchStatus" 
            :placeholder="t('common.status')" 
            clearable 
            style="width: 120px" 
            :options="statusOptions" 
          />
          <n-select 
            v-model:value="searchSchedule" 
            :placeholder="t('pipelineManage.scheduleType')" 
            clearable 
            style="width: 130px" 
            :options="scheduleOptions" 
          />
          <n-button type="primary" @click="loadPipelines">
            <template #icon><n-icon :component="SearchOutline" /></template>
            {{ t('common.search') }}
          </n-button>
          <n-button @click="resetSearch">{{ t('common.reset') }}</n-button>
        </n-space>
        <n-space>
          <n-button @click="loadPipelines">
            <template #icon><n-icon :component="RefreshOutline" /></template>
          </n-button>
        </n-space>
      </n-space>
    </n-card>

    <!-- 流程列表 - 卡片模式 -->
    <div v-if="viewMode === 'card'" class="pipeline-grid">
      <n-spin :show="loading">
        <n-empty v-if="pipelines.length === 0" :description="t('pipelineManage.noData')" />
        <n-grid v-else :cols="3" :x-gap="16" :y-gap="16">
          <n-gi v-for="pipeline in pipelines" :key="pipeline.id">
            <div class="pipeline-card" :class="{ 'is-published': pipeline.pipelineStatus === 1 }">
              <div class="card-header">
                <div class="card-title-row">
                  <span class="card-title">{{ pipeline.pipelineName }}</span>
                  <n-tag :type="getStatusTag(pipeline.pipelineStatus).type" size="small" round>
                    {{ getStatusTag(pipeline.pipelineStatus).text }}
                  </n-tag>
                </div>
                <div class="card-code">{{ pipeline.pipelineCode }}</div>
              </div>
              
              <div class="card-body">
                <div class="card-meta">
                  <div class="meta-item">
                    <n-icon :component="AppsOutline" />
                    <span>{{ getTypeText(pipeline.pipelineType) }}</span>
                  </div>
                  <div class="meta-item">
                    <n-icon :component="TimeOutline" />
                    <span v-if="pipeline.scheduleType === 1" :style="{ color: pipeline.pipelineStatus === 1 ? '#18a058' : '#f0a020' }">
                      {{ pipeline.pipelineStatus === 1 ? t('pipelineManage.cronRunning') : t('pipelineManage.cronStopped') }}
                    </span>
                    <span v-else>{{ getScheduleText(pipeline.scheduleType) }}</span>
                  </div>
                </div>
                <div class="card-desc">{{ pipeline.pipelineDesc || t('pipelineManage.noDesc') }}</div>
                <div class="card-time">
                  <span v-if="pipeline.lastExecuteTime">
                    {{ t('pipelineManage.lastExecute') }}: {{ pipeline.lastExecuteTime }}
                  </span>
                  <span v-else>{{ t('pipelineManage.notExecuted') }}</span>
                </div>
              </div>
              
              <div class="card-footer">
                <n-space :size="6" align="center">
                  <n-button size="small" @click="goDesign(pipeline.id)">
                    <template #icon><n-icon :component="CreateOutline" /></template>
                    {{ t('pipelineManage.design') }}
                  </n-button>
                  <n-button 
                    v-if="pipeline.pipelineStatus === 1" 
                    size="small"
                    type="info"
                    secondary
                    @click="handleExecute(pipeline)"
                  >
                    <template #icon><n-icon :component="FlashOutline" /></template>
                    {{ t('pipelineManage.runOnce') }}
                  </n-button>
                  <n-button 
                    v-if="pipeline.scheduleType === 1 && pipeline.pipelineStatus === 2"
                    size="small"
                    type="success"
                    @click="handleEnable(pipeline)"
                  >
                    <template #icon><n-icon :component="PowerOutline" /></template>
                    {{ t('pipelineManage.startSchedule') }}
                  </n-button>
                  <n-button 
                    v-if="pipeline.scheduleType === 1 && pipeline.pipelineStatus === 1"
                    size="small"
                    type="warning"
                    secondary
                    @click="handleDisable(pipeline)"
                  >
                    <template #icon><n-icon :component="PauseOutline" /></template>
                    {{ t('pipelineManage.stopSchedule') }}
                  </n-button>
                  <n-button 
                    v-if="pipeline.scheduleType !== 1 && pipeline.pipelineStatus === 2"
                    size="small"
                    type="success"
                    @click="handleEnable(pipeline)"
                  >
                    <template #icon><n-icon :component="CheckmarkOutline" /></template>
                    {{ t('common.enable') }}
                  </n-button>
                  <n-button 
                    v-if="pipeline.scheduleType !== 1 && pipeline.pipelineStatus === 1"
                    size="small"
                    type="warning"
                    secondary
                    @click="handleDisable(pipeline)"
                  >
                    <template #icon><n-icon :component="BanOutline" /></template>
                    {{ t('common.disable') }}
                  </n-button>
                  <n-button 
                    v-if="pipeline.pipelineStatus === 0"
                    size="small"
                    type="primary"
                    @click="handlePublish(pipeline)"
                  >
                    <template #icon><n-icon :component="CheckmarkCircleOutline" /></template>
                    {{ t('pipelineManage.publish') }}
                  </n-button>
                  <n-dropdown :options="getMoreOptions(pipeline)" @select="(key: string) => handleMoreAction(key, pipeline)">
                    <n-button size="small" quaternary>
                      <template #icon><n-icon :component="EllipsisHorizontalOutline" /></template>
                    </n-button>
                  </n-dropdown>
                </n-space>
              </div>
            </div>
          </n-gi>
        </n-grid>
      </n-spin>
    </div>

    <!-- 新建/编辑流程弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" :title="editingPipeline ? t('pipelineManage.editPipeline') : t('pipelineManage.create')" style="width: 640px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-grid :cols="2" :x-gap="16">
          <n-gi :span="2">
            <n-form-item :label="t('pipeline.name')" path="pipelineName">
              <n-input v-model:value="formData.pipelineName" :placeholder="t('pipelineManage.namePlaceholder')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('pipelineManage.pipelineCode')" path="pipelineCode">
              <n-input v-model:value="formData.pipelineCode" :placeholder="t('pipelineManage.codeAutoGenerate')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('pipelineManage.pipelineType')" path="pipelineType">
              <n-select v-model:value="formData.pipelineType" :options="typeOptions" :placeholder="t('common.select')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('pipelineManage.scheduleType')" path="scheduleType">
              <n-select v-model:value="formData.scheduleType" :options="scheduleOptions" :placeholder="t('common.select')" />
            </n-form-item>
          </n-gi>
          <n-gi :span="formData.scheduleType === 1 ? 2 : 1">
            <n-form-item v-if="formData.scheduleType === 1" :label="t('pipelineManage.cronExpression')" path="cronExpression">
              <CronEditor v-model="formData.cronExpression" />
            </n-form-item>
            <n-form-item v-else :label="t('pipelineManage.timeout')" path="timeoutSeconds">
              <n-input-number v-model:value="formData.timeoutSeconds" :min="60" :max="86400" style="width: 100%">
                <template #suffix>{{ t('pipelineManage.seconds') }}</template>
              </n-input-number>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('pipelineManage.retryCount')" path="retryCount">
              <n-input-number v-model:value="formData.retryCount" :min="0" :max="5" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi :span="2">
            <n-form-item :label="t('pipelineManage.pipelineDesc')" path="pipelineDesc">
              <n-input v-model:value="formData.pipelineDesc" type="textarea" :placeholder="t('pipelineManage.descPlaceholder')" :rows="3" />
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" @click="handleSubmit">
            {{ editingPipeline ? t('common.save') : t('common.create') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, useMessage, useDialog } from 'naive-ui'
import { 
  SearchOutline, AddOutline, GitNetworkOutline, LayersOutline, 
  CheckmarkCircleOutline, DocumentOutline, TimeOutline, RefreshOutline,
  CreateOutline, PlayOutline, EllipsisHorizontalOutline, AppsOutline,
  CopyOutline, TrashOutline, CheckmarkOutline, CloseOutline,
  FlashOutline, PowerOutline, PauseOutline, BanOutline
} from '@vicons/ionicons5'
import { getPipelines, createPipeline, updatePipeline, deletePipeline, updatePipelineStatus, executePipeline, copyPipeline } from '@/api/pipeline'
import CronEditor from '@/components/CronEditor.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const pipelines = ref<any[]>([])
const showCreateModal = ref(false)
const editingPipeline = ref<any>(null)
const formRef = ref()
const viewMode = ref('card')

const searchKeyword = ref('')
const searchType = ref(null)
const searchStatus = ref(null)
const searchSchedule = ref(null)

const stats = computed(() => {
  const total = pipelines.value.length
  const published = pipelines.value.filter(p => p.pipelineStatus === 1).length
  const draft = pipelines.value.filter(p => p.pipelineStatus === 0).length
  const scheduled = pipelines.value.filter(p => p.scheduleType === 1).length
  return { total, published, draft, scheduled }
})

const typeOptions = computed(() => [
  { label: t('pipelineManage.typeEtl'), value: 1 },
  { label: t('pipelineManage.typeClean'), value: 2 },
  { label: t('pipelineManage.typeSync'), value: 3 },
  { label: t('pipelineManage.typeAggregate'), value: 4 }
])

const statusOptions = computed(() => [
  { label: t('pipelineManage.draft'), value: 0 },
  { label: t('pipelineManage.published'), value: 1 },
  { label: t('pipelineManage.disabled'), value: 2 }
])

const scheduleOptions = computed(() => [
  { label: t('pipelineManage.scheduleManual'), value: 0 },
  { label: t('pipelineManage.scheduleCron'), value: 1 },
  { label: t('pipelineManage.scheduleEvent'), value: 2 }
])

const formData = ref({
  pipelineName: '',
  pipelineCode: '',
  pipelineType: 1,
  scheduleType: 0,
  cronExpression: '',
  timeoutSeconds: 3600,
  retryCount: 0,
  pipelineDesc: ''
})

const formRules = computed(() => ({
  pipelineName: { required: true, message: t('pipelineManage.nameRequired'), trigger: 'blur' },
  pipelineType: { required: true, type: 'number' as const, message: t('pipelineManage.typeRequired'), trigger: 'change' }
}))

const getTypeText = (type: number) => {
  const map: any = { 1: t('pipelineManage.typeEtl'), 2: t('pipelineManage.typeClean'), 3: t('pipelineManage.typeSync'), 4: t('pipelineManage.typeAggregate') }
  return map[type] || t('pipelineManage.unknown')
}

const getStatusTag = (status: number) => {
  const map: any = { 0: 'default', 1: 'success', 2: 'warning' }
  const text: any = { 0: t('pipelineManage.draft'), 1: t('pipelineManage.published'), 2: t('pipelineManage.disabled') }
  return { type: map[status] || 'default', text: text[status] || t('pipelineManage.unknown') }
}

const getScheduleText = (type: number) => {
  const text: any = { 0: t('pipelineManage.scheduleManual'), 1: t('pipelineManage.scheduleCron'), 2: t('pipelineManage.scheduleEvent') }
  return text[type] || t('pipelineManage.unknown')
}

const getMoreOptions = (pipeline: any) => {
  const options: any[] = [
    { label: t('common.edit'), key: 'edit', icon: () => h(NIcon, { component: CreateOutline }) },
    { label: t('common.copy'), key: 'copy', icon: () => h(NIcon, { component: CopyOutline }) }
  ]
  
  options.push({ type: 'divider', key: 'd1' })
  options.push({ label: t('common.delete'), key: 'delete', icon: () => h(NIcon, { component: TrashOutline }) })
  
  return options
}

const handleMoreAction = async (key: string, pipeline: any) => {
  switch (key) {
    case 'edit':
      handleEdit(pipeline)
      break
    case 'copy':
      handleCopy(pipeline)
      break
    case 'publish':
      handlePublish(pipeline)
      break
    case 'disable':
      handleDisable(pipeline)
      break
    case 'enable':
      handleEnable(pipeline)
      break
    case 'delete':
      confirmDelete(pipeline)
      break
  }
}

const loadPipelines = async () => {
  loading.value = true
  try {
    const res = await getPipelines({
      keyword: searchKeyword.value || undefined,
      pipelineType: searchType.value,
      pipelineStatus: searchStatus.value,
      scheduleType: searchSchedule.value
    })
    pipelines.value = res.data || []
  } catch (e) {
    message.error(t('pipelineManage.loadFailed'))
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchKeyword.value = ''
  searchType.value = null
  searchStatus.value = null
  searchSchedule.value = null
  loadPipelines()
}

const goDesign = (id: number) => {
  router.push(`/pipeline/designer/${id}`)
}

const handleEdit = (row: any) => {
  editingPipeline.value = row
  formData.value = { ...row }
  showCreateModal.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    if (editingPipeline.value) {
      await updatePipeline(editingPipeline.value.id, formData.value)
      message.success(t('pipelineManage.updateSuccess'))
    } else {
      await createPipeline(formData.value)
      message.success(t('pipelineManage.createSuccess'))
    }
    showCreateModal.value = false
    editingPipeline.value = null
    resetForm()
    loadPipelines()
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  }
}

const handlePublish = async (row: any) => {
  await updatePipelineStatus(row.id, 1)
  message.success(t('pipelineManage.publishSuccess'))
  loadPipelines()
}

const handleDisable = async (row: any) => {
  await updatePipelineStatus(row.id, 2)
  message.success(t('pipelineManage.disableSuccess'))
  loadPipelines()
}

const handleEnable = async (row: any) => {
  await updatePipelineStatus(row.id, 1)
  message.success(t('pipelineManage.enableSuccess'))
  loadPipelines()
}

const handleExecute = async (row: any) => {
  try {
    await executePipeline(row.id)
    message.success(t('pipelineManage.executeStarted'))
  } catch (e: any) {
    message.error(e?.message || t('pipelineManage.executeFailed'))
  }
}

const handleCopy = async (row: any) => {
  try {
    await copyPipeline(row.id)
    message.success(t('pipelineManage.copySuccess'))
    loadPipelines()
  } catch (e: any) {
    message.error(e?.message || t('pipelineManage.copyFailed'))
  }
}

const confirmDelete = (row: any) => {
  dialog.warning({
    title: t('pipelineManage.deleteConfirmTitle'),
    content: t('pipelineManage.deleteConfirmContent', { name: row.pipelineName }),
    positiveText: t('common.delete'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      await deletePipeline(row.id)
      message.success(t('common.deleteSuccess'))
      loadPipelines()
    }
  })
}

const resetForm = () => {
  formData.value = {
    pipelineName: '',
    pipelineCode: '',
    pipelineType: 1,
    scheduleType: 0,
    cronExpression: '',
    timeoutSeconds: 3600,
    retryCount: 0,
    pipelineDesc: ''
  }
}

onMounted(() => {
  loadPipelines()
})
</script>

<style scoped>
.pipeline-manage {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1e293b;
}

.title-icon {
  color: #2080f0;
}

.page-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-primary);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.stat-total .stat-icon { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); color: #fff; }
.stat-published .stat-icon { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: #fff; }
.stat-draft .stat-icon { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: #fff; }
.stat-scheduled .stat-icon { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); color: #fff; }

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}

/* 筛选卡片 */
.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

/* 流程卡片 */
.pipeline-grid {
  min-height: 300px;
}

.pipeline-card {
  background: var(--bg-primary);
  border-radius: 12px;
  border: 1px solid var(--border-light);
  overflow: hidden;
  transition: all 0.3s ease;
}

.pipeline-card:hover {
  border-color: #2080f0;
  box-shadow: 0 8px 24px rgba(32, 128, 240, 0.15);
  transform: translateY(-4px);
}

.pipeline-card.is-published {
  border-left: 4px solid #18a058;
}

.card-header {
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.card-code {
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
}

.card-body {
  padding: 16px 20px;
}

.card-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.meta-item .n-icon {
  font-size: 16px;
}

.card-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.card-time {
  margin-top: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.card-footer {
  padding: 12px 20px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; padding: 12px 14px; }
  .stat-value { font-size: 22px !important; }
  .stat-label { font-size: 12px !important; }
  .filter-card { border-radius: 14px !important; margin-bottom: 12px; }
  .pipeline-card { border-radius: 12px !important; }
  .card-footer { padding: 10px 14px; }
}
















</style>

<style>
/* PipelineManage 深色模式（非 scoped） */
html.dark .page-title {
  color: #f1f5f9 !important;
}
html.dark .page-desc {
  color: #94a3b8 !important;
}
html.dark .stat-card {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4) !important;
}
html.dark .stat-value {
  color: #f1f5f9 !important;
}
html.dark .stat-label {
  color: #94a3b8 !important;
}
html.dark .pipeline-card {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .pipeline-card:hover {
  border-color: #818cf8 !important;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15) !important;
}
html.dark .pipeline-card.is-published {
  border-left-color: #34d399 !important;
}
html.dark .card-header {
  background: linear-gradient(135deg, #1a2536 0%, #1e293b 100%) !important;
  border-bottom-color: #334155 !important;
}
html.dark .card-title {
  color: #f1f5f9 !important;
}
html.dark .card-code {
  color: #64748b !important;
}
html.dark .card-desc {
  color: #94a3b8 !important;
}
html.dark .card-time {
  color: #64748b !important;
}
html.dark .meta-item {
  color: #94a3b8 !important;
}
html.dark .card-footer {
  background: #1a2536 !important;
  border-top-color: #334155 !important;
}
</style>
