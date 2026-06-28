<template>
  <div class="data-transfer-page">
    <!-- Page_Header_Stats: 传输任务统计概览 (Req 1.1, 10.2) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><SwapHorizontalOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">任务总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><PlayOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.running }}</span>
          <span class="stat-label">运行中</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><StopOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.stopped }}</span>
          <span class="stat-label">已停止</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.scheduled }}</span>
          <span class="stat-label">定时任务</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 任务列表 (Req 1.1, 10.2, 10.3) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><ListOutline /></n-icon>
          <span>传输任务列表</span>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button v-permission="'datax:job:add'" type="primary" @click="handleAdd">
            <template #icon><n-icon :component="AddOutline" /></template>
            新建任务
          </n-button>
          <n-button type="info" @click="showQueueDrawer = true; loadQueue()">
            <template #icon><n-icon :component="SpeedometerOutline" /></template>
            任务队列
          </n-button>
          <n-button @click="load">
            <template #icon><n-icon :component="RefreshOutline" /></template>
            刷新
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 10.2, 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchName"
            placeholder="搜索任务名称"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleResetSearch"
          >
            <template #prefix><n-icon :component="SearchOutline" /></template>
          </n-input>
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchStatus"
            placeholder="全部状态"
            clearable
            style="min-width: 120px"
            :options="statusOptions"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchIncrType"
            placeholder="全部类型"
            clearable
            style="min-width: 120px"
            :options="incrTypeOptions"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">搜索</n-button>
          <n-button @click="handleResetSearch">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- Batch Operations Toolbar (Req 10.2) -->
      <div class="batch-toolbar">
        <div class="batch-toolbar-left">
          <n-button type="success" size="small" :disabled="checkedRowKeys.length === 0" @click="batchExecute">
            <template #icon><n-icon :component="PlayOutline" /></template>
            批量执行
          </n-button>
          <n-button type="info" size="small" :disabled="checkedRowKeys.length === 0" @click="batchStart">
            <template #icon><n-icon :component="RocketOutline" /></template>
            批量启动
          </n-button>
          <n-button type="warning" size="small" :disabled="checkedRowKeys.length === 0" @click="batchStop">
            <template #icon><n-icon :component="StopOutline" /></template>
            批量停止
          </n-button>
          <n-button type="error" size="small" :disabled="checkedRowKeys.length === 0" @click="batchDelete">
            <template #icon><n-icon :component="TrashOutline" /></template>
            批量删除
          </n-button>
        </div>
        <div class="batch-toolbar-right">
          <span v-if="checkedRowKeys.length > 0" class="selected-info">
            已选中 <strong>{{ checkedRowKeys.length }}</strong> 项
          </span>
        </div>
      </div>

      <!-- Empty State (Req 16.7) -->
      <n-empty v-if="!loading && data.length === 0" description="暂无传输任务数据" class="empty-state" />

      <!-- Data Table (Req 1.6) -->
      <n-data-table
        v-else
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="false"
        :row-key="(row: DataxJob) => row.id"
        :checked-row-keys="checkedRowKeys"
        :scroll-x="1200"
        remote
        striped
        class="custom-table"
        @update:checked-row-keys="handleCheck"
      />

      <!-- Pagination_Wrapper (Req 1.5) -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>共 {{ total }} 条记录</n-tag>
        </div>
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 25, 50]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 执行进度弹窗 -->
    <n-modal v-model:show="showProgressModal" :mask-closable="false" preset="card" title="任务执行中" style="width: 480px; border-radius: 16px;">
      <div class="progress-content">
        <n-progress type="line" :percentage="progressData.percent" :indicator-placement="'inside'" :processing="progressData.status === 2" />
        <p class="progress-msg">{{ progressData.message || '正在执行...' }}</p>
        <p class="progress-stats">
          读取: <span class="read-count">{{ progressData.readCount || 0 }}</span> 条 |
          写入: <span class="write-count">{{ progressData.writeCount || 0 }}</span> 条
        </p>
      </div>
      <template #footer>
        <n-space justify="center">
          <n-button @click="closeProgressModal">关闭窗口</n-button>
          <span class="progress-hint">关闭窗口不会停止任务执行</span>
        </n-space>
      </template>
    </n-modal>

    <!-- 新建/编辑弹窗 -->
    <n-modal v-model:show="modal.visible.value" preset="card" :title="modalTitle" style="width: 750px; border-radius: 16px;" :mask-closable="false">
      <n-form ref="formRef" :model="modal.formData.value" :rules="formRules" label-placement="left" label-width="100px">
        <n-form-item label="任务名称" path="jobName">
          <n-input v-model:value="modal.formData.value.jobName" placeholder="请输入任务名称" />
        </n-form-item>
        <n-form-item label="任务描述" path="jobDesc">
          <n-input v-model:value="modal.formData.value.jobDesc" type="textarea" placeholder="请输入任务描述" :rows="2" />
        </n-form-item>
        <n-divider>源端配置</n-divider>
        <n-form-item label="源数据源" path="sourceDataSourceId">
          <n-select v-model:value="modal.formData.value.sourceDataSourceId" :options="dataSourceOptions" placeholder="请选择源数据源" filterable @update:value="handleSourceDsChange" />
        </n-form-item>
        <n-form-item label="源表名" path="sourceTable">
          <n-select v-model:value="modal.formData.value.sourceTable" :options="sourceTableOptions" placeholder="请选择源表" filterable :loading="loadingSourceTables" @update:value="handleSourceTableChange" />
        </n-form-item>
        <n-form-item label="查询SQL">
          <n-input v-model:value="modal.formData.value.sourceQuerySql" type="textarea" placeholder="可选，自定义查询SQL（留空则查询整表）" :rows="3" />
        </n-form-item>
        <n-divider>目标端配置</n-divider>
        <n-form-item label="目标数据源" path="targetDataSourceId">
          <n-select v-model:value="modal.formData.value.targetDataSourceId" :options="dataSourceOptions" placeholder="请选择目标数据源" filterable @update:value="handleTargetDsChange" />
        </n-form-item>
        <n-form-item label="目标表名" path="targetTable">
          <n-select v-model:value="modal.formData.value.targetTable" :options="targetTableOptions" placeholder="请选择目标表" filterable :loading="loadingTargetTables" allow-input @update:value="handleTargetTableChange" />
        </n-form-item>
        <n-form-item label="写入模式">
          <n-radio-group v-model:value="modal.formData.value.writeMode">
            <n-radio value="insert">INSERT</n-radio>
            <n-radio value="replace">REPLACE</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-divider>同步配置</n-divider>
        <!-- 字段映射区域 -->
        <n-form-item label="字段映射">
          <div class="column-mapping-container">
            <div class="mapping-toolbar">
              <n-button size="small" @click="clearMapping">
                <template #icon><n-icon :component="SquareOutline" /></template>
                清空
              </n-button>
              <n-button size="small" @click="selectAllColumns">
                <template #icon><n-icon :component="CheckboxOutline" /></template>
                全选
              </n-button>
              <n-button size="small" type="info" @click="autoMapping">
                <template #icon><n-icon :component="FlashOutline" /></template>
                自动映射
              </n-button>
            </div>
            <n-alert type="info" style="margin-bottom: 12px; font-size: 12px;">
              <strong>提示：</strong>源字段和目标字段按勾选顺序一一对应映射。如果不配置映射，将使用源表全部字段（要求目标表字段名相同）。
            </n-alert>
            <div class="mapping-columns">
              <div class="mapping-column">
                <div class="mapping-header">
                  <span>📊 源字段</span>
                  <span class="col-count">{{ sourceColumns.length }} 个字段</span>
                </div>
                <div class="mapping-body">
                  <div v-if="sourceColumns.length === 0" class="mapping-empty">请先选择源表</div>
                  <div v-else class="mapping-list">
                    <div v-for="col in sourceColumns" :key="col.columnName" class="mapping-item">
                      <n-checkbox v-model:checked="col.checked" @update:checked="updateColumnMapping">
                        <span class="col-name">{{ col.columnName }}</span>
                        <span class="col-type">({{ col.dataType }})</span>
                      </n-checkbox>
                    </div>
                  </div>
                </div>
              </div>
              <div class="mapping-arrow">
                <n-icon :component="ArrowForwardOutline" size="24" :color="'var(--color-primary, #2563eb)'" />
                <p>字段映射</p>
              </div>
              <div class="mapping-column">
                <div class="mapping-header">
                  <span>📊 目标字段</span>
                  <span class="col-count">{{ targetColumns.length }} 个字段</span>
                </div>
                <div class="mapping-body">
                  <div v-if="targetColumns.length === 0" class="mapping-empty">请先选择目标表</div>
                  <div v-else class="mapping-list">
                    <div v-for="col in targetColumns" :key="col.columnName" class="mapping-item">
                      <n-checkbox v-model:checked="col.checked" @update:checked="updateColumnMapping">
                        <span class="col-name">{{ col.columnName }}</span>
                        <span class="col-type">({{ col.dataType }})</span>
                      </n-checkbox>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </n-form-item>
        <n-form-item label="批次大小">
          <n-input-number v-model:value="modal.formData.value.batchSize" :min="100" :max="50000" :step="500" placeholder="每批提交的记录数" style="width: 200px;">
            <template #suffix>条/批</template>
          </n-input-number>
          <span class="form-hint" style="margin-left: 12px;">较大值提升速度但占用更多内存</span>
        </n-form-item>
        <n-form-item label="同步类型">
          <n-radio-group v-model:value="modal.formData.value.incrementType">
            <n-radio :value="0">全量同步</n-radio>
            <n-radio :value="1">增量同步</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="modal.formData.value.incrementType === 1" label="增量字段">
          <n-select v-model:value="modal.formData.value.incrementColumn" :options="incrementColumnOptions" placeholder="请选择增量字段" filterable clearable />
        </n-form-item>
        <n-form-item label="定时表达式">
          <div class="cron-builder">
            <n-space vertical>
              <n-radio-group v-model:value="cronType" @update:value="updateCronExpression">
                <n-space>
                  <n-radio value="none">不启用定时</n-radio>
                  <n-radio value="preset">预设方案</n-radio>
                  <n-radio value="custom">自定义</n-radio>
                </n-space>
              </n-radio-group>
              <n-select
                v-if="cronType === 'preset'"
                v-model:value="cronPreset"
                :options="cronPresetOptions"
                placeholder="选择执行频率"
                style="width: 300px;"
                @update:value="applyCronPreset"
              />
              <div v-if="cronType === 'custom'" class="cron-custom">
                <n-space>
                  <n-input-group>
                    <n-input-group-label>每</n-input-group-label>
                    <n-input-number v-model:value="cronConfig.interval" :min="1" :max="60" style="width: 80px;" @update:value="buildCronExpression" />
                    <n-select v-model:value="cronConfig.unit" :options="cronUnitOptions" style="width: 80px;" @update:value="buildCronExpression" />
                  </n-input-group>
                  <n-input-group v-if="cronConfig.unit === 'day'">
                    <n-input-group-label>在</n-input-group-label>
                    <n-time-picker v-model:value="cronConfig.time" format="HH:mm" :hours="[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23]" @update:value="buildCronExpression" />
                    <n-input-group-label>执行</n-input-group-label>
                  </n-input-group>
                </n-space>
              </div>
              <n-input
                v-model:value="modal.formData.value.cronExpression"
                placeholder="Cron表达式"
                :disabled="cronType !== 'custom'"
                style="width: 300px;"
              >
                <template #prefix><n-icon :component="TimeOutline" /></template>
              </n-input>
              <span v-if="modal.formData.value.cronExpression" class="cron-desc">{{ getCronDescription(modal.formData.value.cronExpression) }}</span>
            </n-space>
          </div>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="handlePreviewJson">预览JSON</n-button>
          <n-button @click="modal.close()">取消</n-button>
          <n-button type="primary" :loading="modal.submitting.value" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- JSON预览弹窗 -->
    <n-modal v-model:show="showJsonModal" preset="card" title="DataX JSON配置预览" style="width: 700px; border-radius: 16px;">
      <n-code :code="jsonPreview" language="json" />
    </n-modal>

    <!-- 任务队列抽屉 (Req 10.3) -->
    <n-drawer v-model:show="showQueueDrawer" :width="520" placement="right">
      <n-drawer-content title="任务队列">
        <template #header-extra>
          <n-button size="small" @click="loadQueue">
            <template #icon><n-icon :component="RefreshOutline" /></template>
            刷新
          </n-button>
        </template>

        <!-- Queued tasks section -->
        <div class="queue-section">
          <div class="queue-section-title">
            <n-icon :component="TimeOutline" size="16" />
            <span>排队中 ({{ queuedTasks.length }})</span>
          </div>
          <div v-if="queuedTasks.length === 0" class="queue-section-empty">暂无排队任务</div>
          <div v-else class="queue-list">
            <div v-for="item in queuedTasks" :key="'q-' + item.logId" class="queue-item queue-item-queued">
              <div class="queue-item-header">
                <StatusTag status="queued" :status-map="queueStatusMap" />
                <span class="queue-log-id">#{{ item.logId }}</span>
              </div>
              <p class="queue-item-msg">{{ item.jobName || '等待执行...' }}</p>
            </div>
          </div>
        </div>

        <n-divider />

        <!-- Running tasks section -->
        <div class="queue-section">
          <div class="queue-section-title">
            <n-icon :component="SpeedometerOutline" size="16" />
            <span>执行中 ({{ runningTasks.length }})</span>
          </div>
          <div v-if="runningTasks.length === 0" class="queue-section-empty">暂无执行中任务</div>
          <div v-else class="queue-list">
            <div v-for="item in runningTasks" :key="'r-' + item.logId" class="queue-item queue-item-running">
              <div class="queue-item-header">
                <StatusTag status="running" :status-map="queueStatusMap" />
                <span class="queue-log-id">#{{ item.logId }}</span>
              </div>
              <n-progress type="line" :percentage="item.percent" :indicator-placement="'inside'" processing />
              <div class="queue-item-stats">
                <span>读取: <strong style="color: var(--color-success);">{{ item.readCount || 0 }}</strong></span>
                <span>写入: <strong style="color: var(--color-primary);">{{ item.writeCount || 0 }}</strong></span>
              </div>
              <p class="queue-item-msg">{{ item.message || '正在执行...' }}</p>
            </div>
          </div>
        </div>

        <!-- Empty state when both are empty -->
        <div v-if="queuedTasks.length === 0 && runningTasks.length === 0" class="queue-empty">
          <n-icon :component="CheckmarkCircleOutline" size="48" color="#10b981" />
          <p>当前没有排队或执行中的任务</p>
        </div>
      </n-drawer-content>
    </n-drawer>

    <!-- 执行对比弹窗 -->
    <n-modal v-model:show="showCompareModal" preset="card" title="执行记录对比" style="width: 700px; border-radius: 16px;">
      <n-space vertical>
        <n-space>
          <n-input-number v-model:value="compareLogId1" placeholder="日志ID 1" :min="1" style="width: 160px;" />
          <span style="line-height: 34px;">vs</span>
          <n-input-number v-model:value="compareLogId2" placeholder="日志ID 2" :min="1" style="width: 160px;" />
          <n-button type="primary" :loading="comparing" @click="handleCompare">对比</n-button>
        </n-space>
        <div v-if="compareResult" class="compare-result">
          <div class="compare-grid">
            <div class="compare-header">
              <span>指标</span>
              <span>执行 #{{ compareLogId1 }}</span>
              <span>执行 #{{ compareLogId2 }}</span>
              <span>差异</span>
            </div>
            <div class="compare-row">
              <span>读取记录</span>
              <span>{{ compareResult.execution1?.readCount || 0 }}</span>
              <span>{{ compareResult.execution2?.readCount || 0 }}</span>
              <span :class="diffClass(compareResult.diff?.readCountDiff)">{{ formatDiff(compareResult.diff?.readCountDiff) }}</span>
            </div>
            <div class="compare-row">
              <span>写入记录</span>
              <span>{{ compareResult.execution1?.writeCount || 0 }}</span>
              <span>{{ compareResult.execution2?.writeCount || 0 }}</span>
              <span :class="diffClass(compareResult.diff?.writeCountDiff)">{{ formatDiff(compareResult.diff?.writeCountDiff) }}</span>
            </div>
            <div class="compare-row">
              <span>执行耗时(ms)</span>
              <span>{{ compareResult.execution1?.duration || 0 }}</span>
              <span>{{ compareResult.execution2?.duration || 0 }}</span>
              <span :class="diffClass(-(compareResult.diff?.durationDiff || 0))">{{ formatDiff(compareResult.diff?.durationDiff) }}</span>
            </div>
            <div class="compare-row">
              <span>吞吐量(条/s)</span>
              <span>{{ (compareResult.diff?.throughput1 || 0).toFixed(1) }}</span>
              <span>{{ (compareResult.diff?.throughput2 || 0).toFixed(1) }}</span>
              <span :class="diffClass((compareResult.diff?.throughput2 || 0) - (compareResult.diff?.throughput1 || 0))">{{ formatDiff(((compareResult.diff?.throughput2 || 0) - (compareResult.diff?.throughput1 || 0))) }}</span>
            </div>
          </div>
        </div>
      </n-space>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, h, onUnmounted } from 'vue'
import { useMessage, useDialog, NTag, NSpace } from 'naive-ui'
import {
  SwapHorizontalOutline, AddOutline, SearchOutline, PlayOutline, StopOutline,
  CreateOutline, TrashOutline, CopyOutline, RefreshOutline, ListOutline,
  RocketOutline, DocumentTextOutline, SquareOutline,
  CheckboxOutline, FlashOutline, ArrowForwardOutline, TimeOutline,
  SpeedometerOutline, GitCompareOutline, CheckmarkCircleOutline
} from '@vicons/ionicons5'
import {
  getJobList, createJob, updateJob, deleteJob, executeJob, startJob, stopJob,
  copyJob, previewJson, getProgress, batchDeleteJobs, batchExecuteJobs,
  getRunningQueue, compareExecutions,
  type DataxJob, type ExecuteProgress
} from '@/api/dataxJob'
import { getDataSourceList, getDataSourceTables, getTableColumns } from '@/api/dataSource'
import { useRouter } from 'vue-router'
import { useDataTable } from '@/composables/useDataTable'
import { useFormModal } from '@/composables/useFormModal'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'

const message = useMessage()
const dialog = useDialog()
const router = useRouter()

// --- 状态映射（用于 StatusTag）(Req 10.2) ---
const jobStatusMap: Record<string | number, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  1: { label: '运行中', type: 'success' },
  0: { label: '已停止', type: 'default' }
}

const queueStatusMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  running: { label: '执行中', type: 'warning' },
  queued: { label: '排队中', type: 'info' }
}

// --- 搜索状态 ---
const searchName = ref('')
const searchStatus = ref<number | null>(null)
const searchIncrType = ref<number | null>(null)

const statusOptions = [
  { label: '运行中', value: 1 },
  { label: '已停止', value: 0 }
]

const incrTypeOptions = [
  { label: '全量同步', value: 0 },
  { label: '增量同步', value: 1 }
]

// --- 使用 useDataTable 管理表格数据 (Req 10.2) ---
const {
  data,
  loading,
  total,
  pagination,
  checkedRowKeys,
  load,
  handlePageChange,
  handlePageSizeChange,
  handleCheck
} = useDataTable<DataxJob & Record<string, unknown>>({
  apiFn: async (params) => {
    return getJobList({
      page: params.page,
      pageSize: params.pageSize,
      keyword: searchName.value || undefined,
      jobStatus: searchStatus.value ?? undefined,
      incrementType: searchIncrType.value ?? undefined
    })
  },
  defaultPageSize: 10,
  immediate: true,
  rowKey: 'id'
})

// --- 统计 (Req 1.1) ---
const stats = computed(() => {
  const allData = data.value
  const totalCount = total.value || allData.length
  const running = allData.filter((j: any) => j.jobStatus === 1).length
  const stopped = allData.filter((j: any) => j.jobStatus === 0).length
  const scheduled = allData.filter((j: any) => j.cronExpression).length
  return { total: totalCount, running, stopped, scheduled }
})

const handleSearch = () => { load() }
const handleResetSearch = () => {
  searchName.value = ''
  searchStatus.value = null
  searchIncrType.value = null
  load()
}

// --- 使用 useFormModal 管理弹窗 (Req 10.2) ---
const defaultFormData: DataxJob & Record<string, unknown> = {
  jobName: '',
  jobDesc: '',
  jobType: 1,
  sourceDataSourceId: undefined as unknown as number,
  sourceTable: '',
  sourceQuerySql: '',
  targetDataSourceId: undefined as unknown as number,
  targetTable: '',
  writeMode: 'insert',
  incrementType: 0,
  incrementColumn: '',
  cronExpression: '',
  channelCount: 3,
  batchSize: 1000,
  columnMapping: ''
}

const modal = useFormModal<DataxJob & Record<string, unknown>>({
  defaultFormData: () => ({ ...defaultFormData }),
  createFn: async (formData) => {
    await createJob(formData as DataxJob)
    message.success('创建成功')
  },
  updateFn: async (formData) => {
    await updateJob(formData as DataxJob)
    message.success('更新成功')
  },
  onSuccess: () => { load() },
  onError: (error) => { message.error(error.message || '操作失败') }
})

const modalTitle = computed(() => modal.mode.value === 'create' ? '新建任务' : '编辑任务')

// --- 数据源选项 ---
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const sourceTableOptions = ref<Array<{ label: string; value: string }>>([])
const targetTableOptions = ref<Array<{ label: string; value: string }>>([])
const loadingSourceTables = ref(false)
const loadingTargetTables = ref(false)

// --- 字段映射 ---
interface ColumnInfo { columnName: string; dataType: string; checked: boolean }
const sourceColumns = ref<ColumnInfo[]>([])
const targetColumns = ref<ColumnInfo[]>([])
const incrementColumnOptions = ref<Array<{ label: string; value: string }>>([])
const loadingSourceColumns = ref(false)
const loadingTargetColumns = ref(false)

// --- 表单相关 ---
const formRef = ref()
const showJsonModal = ref(false)
const jsonPreview = ref('')

// --- Cron 表达式配置 ---
const cronType = ref<'none' | 'preset' | 'custom'>('none')
const cronPreset = ref('')
const cronConfig = ref({ interval: 1, unit: 'day', time: Date.now() })

const cronPresetOptions = [
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每5分钟', value: '0 */5 * * * ?' },
  { label: '每10分钟', value: '0 */10 * * * ?' },
  { label: '每30分钟', value: '0 */30 * * * ?' },
  { label: '每小时', value: '0 0 * * * ?' },
  { label: '每2小时', value: '0 0 */2 * * ?' },
  { label: '每天凌晨2点', value: '0 0 2 * * ?' },
  { label: '每天凌晨3点', value: '0 0 3 * * ?' },
  { label: '每天早上6点', value: '0 0 6 * * ?' },
  { label: '每天中午12点', value: '0 0 12 * * ?' },
  { label: '每天晚上22点', value: '0 0 22 * * ?' },
  { label: '每周一凌晨2点', value: '0 0 2 ? * MON' },
  { label: '每月1号凌晨2点', value: '0 0 2 1 * ?' }
]

const cronUnitOptions = [
  { label: '分钟', value: 'minute' },
  { label: '小时', value: 'hour' },
  { label: '天', value: 'day' }
]

const formRules = {
  jobName: { required: true, message: '请输入任务名称', trigger: 'blur' },
  sourceDataSourceId: { required: true, type: 'number', min: 1, message: '请选择源数据源', trigger: 'change' },
  sourceTable: { required: true, message: '请选择源表', trigger: 'change' },
  targetDataSourceId: { required: true, type: 'number', min: 1, message: '请选择目标数据源', trigger: 'change' }
}

// --- 进度弹窗 ---
const showProgressModal = ref(false)
const progressData = ref<ExecuteProgress>({ status: 2, percent: 0, readCount: 0, writeCount: 0, message: '' })
let progressTimer: ReturnType<typeof setInterval> | null = null

// --- 任务队列 (Req 10.3) ---
const showQueueDrawer = ref(false)
const queueData = ref<any[]>([])
const queueTimer: ReturnType<typeof setInterval> | null = null

const queuedTasks = computed(() => queueData.value.filter((t: any) => t.status === 'queued' || t.status === 0))
const runningTasks = computed(() => queueData.value.filter((t: any) => t.status === 'running' || t.status === 2 || t.status === 1))

// --- 执行对比 ---
const showCompareModal = ref(false)
const compareLogId1 = ref<number | null>(null)
const compareLogId2 = ref<number | null>(null)
const comparing = ref(false)
const compareResult = ref<any>(null)

// --- 表格行操作（使用 ActionButtons）(Req 10.2, 10.3) ---
const getRowActions = (row: DataxJob): ActionConfig[] => {
  const actions: ActionConfig[] = [
    {
      label: '执行',
      type: 'success',
      icon: PlayOutline,
      permission: 'datax:job:execute',
      onClick: () => handleExecute(row)
    },
    {
      label: row.jobStatus === 0 ? '启动' : '停止',
      type: row.jobStatus === 0 ? 'info' : 'warning',
      icon: row.jobStatus === 0 ? RocketOutline : StopOutline,
      permission: 'datax:job:execute',
      onClick: () => row.jobStatus === 0 ? handleStart(row) : handleStop(row)
    },
    {
      label: '编辑',
      type: 'primary',
      icon: CreateOutline,
      permission: 'datax:job:edit',
      onClick: () => handleEdit(row)
    },
    {
      label: '复制',
      icon: CopyOutline,
      permission: 'datax:job:add',
      confirm: '确定复制该任务？',
      onClick: () => handleCopy(row)
    },
    {
      label: '对比',
      icon: GitCompareOutline,
      onClick: () => { showCompareModal.value = true; compareResult.value = null }
    },
    {
      label: '日志',
      icon: DocumentTextOutline,
      onClick: () => viewLog(row)
    },
    {
      label: '删除',
      type: 'error',
      icon: TrashOutline,
      permission: 'datax:job:delete',
      confirm: '确定删除该任务？删除后不可恢复！',
      onClick: () => handleDeleteConfirmed(row)
    }
  ]
  return actions
}

// --- 表格列定义（使用 StatusTag 和 ActionButtons）(Req 10.2, 10.3) ---
const columns = [
  { type: 'selection' as const, width: 50 },
  {
    title: '任务名称', key: 'jobName', width: 180,
    render: (row: DataxJob) => h('div', [
      h('span', { style: 'font-weight: 500;' }, row.jobName),
      row.jobDesc ? h('br') : null,
      row.jobDesc ? h('span', { style: 'font-size: 12px; color: #9ca3af;' }, row.jobDesc) : null
    ])
  },
  {
    title: '任务类型', key: 'jobType', width: 140,
    render: (row: DataxJob) => h(NSpace, { size: 4 }, () => [
      h(NTag, { type: row.jobType === 1 ? 'info' : 'default', size: 'small' }, () => row.jobType === 1 ? '数据库' : '文件'),
      h(NTag, { type: row.incrementType === 1 ? 'success' : 'warning', size: 'small' }, () => row.incrementType === 1 ? '增量' : '全量')
    ])
  },
  { title: '源数据源', key: 'sourceDataSourceName', width: 120, ellipsis: { tooltip: true } },
  { title: '源表', key: 'sourceTable', width: 120, ellipsis: { tooltip: true } },
  { title: '目标数据源', key: 'targetDataSourceName', width: 120, ellipsis: { tooltip: true } },
  { title: '目标表', key: 'targetTable', width: 120, ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'jobStatus', width: 90,
    render: (row: DataxJob) => h(StatusTag, { status: row.jobStatus ?? 0, statusMap: jobStatusMap })
  },
  { title: '最后执行', key: 'lastExecuteTime', width: 160 },
  {
    title: '操作', key: 'actions', width: 280, fixed: 'right' as const,
    render: (row: DataxJob) => h(ActionButtons, { actions: getRowActions(row), row, maxVisible: 4 })
  }
]

// --- Cron 表达式方法 ---
const updateCronExpression = (type: string) => {
  if (type === 'none') { modal.formData.value.cronExpression = '' }
  else if (type === 'preset') { cronPreset.value = ''; modal.formData.value.cronExpression = '' }
}
const applyCronPreset = (value: string) => { modal.formData.value.cronExpression = value }
const buildCronExpression = () => {
  const { interval, unit, time } = cronConfig.value
  const date = new Date(time)
  const hour = date.getHours()
  const minute = date.getMinutes()
  switch (unit) {
    case 'minute': modal.formData.value.cronExpression = `0 */${interval} * * * ?`; break
    case 'hour': modal.formData.value.cronExpression = `0 0 */${interval} * * ?`; break
    case 'day': modal.formData.value.cronExpression = `0 ${minute} ${hour} */${interval} * ?`; break
  }
}
const getCronDescription = (cron: string): string => {
  if (!cron) return ''
  const preset = cronPresetOptions.find(p => p.value === cron)
  if (preset) return `📅 ${preset.label}`
  const parts = cron.split(' ')
  if (parts.length < 6) return ''
  const [, min, hour, day] = parts
  if (min.startsWith('*/')) return `📅 每${min.slice(2)}分钟执行`
  if (hour.startsWith('*/')) return `📅 每${hour.slice(2)}小时执行`
  if (day.startsWith('*/')) return `📅 每${day.slice(2)}天的${hour}:${min.padStart(2, '0')}执行`
  if (day === '*' && hour !== '*' && min !== '*') return `📅 每天${hour}:${min.padStart(2, '0')}执行`
  return `📅 Cron: ${cron}`
}

// --- 数据源加载 ---
const loadDataSources = async () => {
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    dataSourceOptions.value = (res.data?.list || []).map((ds: any) => ({
      label: `${ds.name} (${ds.dbType})`,
      value: ds.id
    }))
  } catch (error) {
    console.error('加载数据源失败:', error)
  }
}

// --- 源数据源变化 ---
const handleSourceDsChange = async (value: number) => {
  modal.formData.value.sourceTable = ''
  incrementColumnOptions.value = []
  if (!value) { sourceTableOptions.value = []; return }
  loadingSourceTables.value = true
  try {
    const res = await getDataSourceTables(value)
    sourceTableOptions.value = (res.data || []).map((t: any) => ({ label: t.tableName, value: t.tableName }))
  } catch (error) { message.error('加载表列表失败') }
  finally { loadingSourceTables.value = false }
}

// --- 源表变化 ---
const handleSourceTableChange = async (tableName: string) => {
  sourceColumns.value = []
  incrementColumnOptions.value = []
  if (!tableName || !modal.formData.value.sourceDataSourceId) return
  loadingSourceColumns.value = true
  try {
    const res = await getTableColumns(modal.formData.value.sourceDataSourceId as number, tableName)
    sourceColumns.value = (res.data || []).map((col: any) => ({ columnName: col.columnName, dataType: col.dataType, checked: true }))
    incrementColumnOptions.value = sourceColumns.value.map(col => ({ label: `${col.columnName} (${col.dataType})`, value: col.columnName }))
    restoreColumnMapping()
  } catch (error) { console.error('加载源字段失败:', error) }
  finally { loadingSourceColumns.value = false }
}

// --- 目标数据源变化 ---
const handleTargetDsChange = async (value: number) => {
  modal.formData.value.targetTable = ''
  targetColumns.value = []
  if (!value) { targetTableOptions.value = []; return }
  loadingTargetTables.value = true
  try {
    const res = await getDataSourceTables(value)
    targetTableOptions.value = (res.data || []).map((t: any) => ({ label: t.tableName, value: t.tableName }))
  } catch (error) { message.error('加载表列表失败') }
  finally { loadingTargetTables.value = false }
}

// --- 目标表变化 ---
const handleTargetTableChange = async (tableName: string) => {
  targetColumns.value = []
  if (!tableName || !modal.formData.value.targetDataSourceId) return
  loadingTargetColumns.value = true
  try {
    const res = await getTableColumns(modal.formData.value.targetDataSourceId as number, tableName)
    targetColumns.value = (res.data || []).map((col: any) => ({ columnName: col.columnName, dataType: col.dataType, checked: true }))
    restoreColumnMapping()
  } catch (error) { console.error('加载目标字段失败:', error) }
  finally { loadingTargetColumns.value = false }
}

// --- 字段映射操作 ---
const restoreColumnMapping = () => {
  if (!modal.formData.value.columnMapping) return
  try {
    const mapping = JSON.parse(modal.formData.value.columnMapping as string)
    if (!Array.isArray(mapping) || mapping.length === 0) return
    sourceColumns.value.forEach(col => col.checked = false)
    targetColumns.value.forEach(col => col.checked = false)
    mapping.forEach((m: any) => {
      const srcCol = sourceColumns.value.find(c => c.columnName === m.sourceColumn)
      const tgtCol = targetColumns.value.find(c => c.columnName === m.targetColumn)
      if (srcCol) srcCol.checked = true
      if (tgtCol) tgtCol.checked = true
    })
  } catch (e) { /* ignore */ }
}

const clearMapping = () => {
  sourceColumns.value.forEach(col => col.checked = false)
  targetColumns.value.forEach(col => col.checked = false)
  updateColumnMapping()
  message.success('已清空字段映射')
}

const selectAllColumns = () => {
  sourceColumns.value.forEach(col => col.checked = true)
  targetColumns.value.forEach(col => col.checked = true)
  updateColumnMapping()
  message.success('已全选所有字段')
}

const autoMapping = () => {
  sourceColumns.value.forEach(col => col.checked = false)
  targetColumns.value.forEach(col => col.checked = false)
  let matchCount = 0
  sourceColumns.value.forEach(srcCol => {
    const tgtCol = targetColumns.value.find(t => t.columnName.toLowerCase() === srcCol.columnName.toLowerCase())
    if (tgtCol) { srcCol.checked = true; tgtCol.checked = true; matchCount++ }
  })
  updateColumnMapping()
  message.success(`已自动映射 ${matchCount} 个同名字段`)
}

const updateColumnMapping = () => {
  const sourceCols = sourceColumns.value.filter(c => c.checked).map(c => c.columnName)
  const targetCols = targetColumns.value.filter(c => c.checked).map(c => c.columnName)
  const len = Math.min(sourceCols.length, targetCols.length)
  const mapping = []
  for (let i = 0; i < len; i++) { mapping.push({ sourceColumn: sourceCols[i], targetColumn: targetCols[i] }) }
  modal.formData.value.columnMapping = JSON.stringify(mapping)
}

// --- 弹窗操作 ---
const handleAdd = () => {
  sourceTableOptions.value = []
  targetTableOptions.value = []
  sourceColumns.value = []
  targetColumns.value = []
  incrementColumnOptions.value = []
  cronType.value = 'none'
  cronPreset.value = ''
  modal.openCreate()
}

const handleEdit = async (row: DataxJob) => {
  sourceColumns.value = []
  targetColumns.value = []
  incrementColumnOptions.value = []
  const savedSourceTable = row.sourceTable
  const savedTargetTable = row.targetTable

  if (row.cronExpression) {
    const preset = cronPresetOptions.find(p => p.value === row.cronExpression)
    cronType.value = preset ? 'preset' : 'custom'
    if (preset) cronPreset.value = row.cronExpression
  } else { cronType.value = 'none' }

  modal.openEdit(row as DataxJob & Record<string, unknown>)

  if (row.sourceDataSourceId != null && row.sourceDataSourceId > 0) {
    loadingSourceTables.value = true
    try {
      const res = await getDataSourceTables(row.sourceDataSourceId)
      sourceTableOptions.value = (res.data || []).map((t: any) => ({ label: t.tableName, value: t.tableName }))
      modal.formData.value.sourceTable = savedSourceTable || ''
      if (savedSourceTable) await handleSourceTableChange(savedSourceTable)
    } catch (error) { message.error('加载源表列表失败') }
    finally { loadingSourceTables.value = false }
  }

  if (row.targetDataSourceId != null && row.targetDataSourceId > 0) {
    loadingTargetTables.value = true
    try {
      const res = await getDataSourceTables(row.targetDataSourceId)
      targetTableOptions.value = (res.data || []).map((t: any) => ({ label: t.tableName, value: t.tableName }))
      modal.formData.value.targetTable = savedTargetTable || ''
      if (savedTargetTable) await handleTargetTableChange(savedTargetTable)
    } catch (error) { message.error('加载目标表列表失败') }
    finally { loadingTargetTables.value = false }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    await modal.submit()
  } catch (error: any) {
    if (error.message) message.error(error.message)
  }
}

// --- 任务操作 ---
const handleDeleteConfirmed = async (row: DataxJob) => {
  try {
    await deleteJob(row.id!)
    message.success('删除成功')
    load()
  } catch (error) { message.error('删除失败') }
}

const handleExecute = async (row: DataxJob) => {
  dialog.info({
    title: '确认执行',
    content: '确定立即执行该任务？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await executeJob(row.id!)
        const logId = res.data
        if (logId) {
          message.success('任务已提交，正在执行...')
          showProgressModal.value = true
          progressData.value = { status: 2, percent: 0, readCount: 0, writeCount: 0, message: '正在执行...' }
          startProgressPolling(logId)
        } else { message.success('任务已提交'); load() }
      } catch (error: any) { message.error(error.message || '执行失败') }
    }
  })
}

const startProgressPolling = (logId: number) => {
  if (progressTimer) clearInterval(progressTimer)
  let checkCount = 0
  const maxChecks = 300
  progressTimer = setInterval(async () => {
    checkCount++
    if (checkCount > maxChecks) {
      clearInterval(progressTimer!); showProgressModal.value = false
      message.warning('任务执行超时，请查看日志'); load(); return
    }
    try {
      const res = await getProgress(logId)
      if (res.data) {
        progressData.value = res.data
        if (res.data.status === 1) {
          clearInterval(progressTimer!)
          setTimeout(() => { showProgressModal.value = false; message.success(`执行完成！读取:${res.data.readCount || 0} 写入:${res.data.writeCount || 0}`); load() }, 1500)
        } else if (res.data.status === 0) {
          clearInterval(progressTimer!)
          setTimeout(() => { showProgressModal.value = false; message.error(`执行失败: ${res.data.message || '未知错误'}`); load() }, 2000)
        }
      }
    } catch (error) { console.error('获取进度失败:', error) }
  }, 1000)
}

const closeProgressModal = () => {
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
  showProgressModal.value = false; load()
}

const handleStart = async (row: DataxJob) => {
  try { await startJob(row.id!); message.success('任务调度已启动'); load() }
  catch (error: any) { message.error(error.message || '启动失败，请检查Cron表达式配置') }
}

const handleStop = async (row: DataxJob) => {
  try { await stopJob(row.id!); message.success('任务调度已停止'); load() }
  catch (error: any) { message.error(error.message || '停止失败') }
}

const handleCopy = async (row: DataxJob) => {
  try { await copyJob(row.id!); message.success('复制成功'); load() }
  catch (error) { message.error('复制失败') }
}

const viewLog = (row: DataxJob) => { router.push({ path: '/datax/log', query: { jobId: row.id } }) }

const handlePreviewJson = async () => {
  if (!modal.formData.value.sourceDataSourceId || !modal.formData.value.targetDataSourceId) {
    message.warning('请先选择源数据源和目标数据源'); return
  }
  try {
    const res = await previewJson(modal.formData.value as DataxJob)
    jsonPreview.value = res.data || '{}'; showJsonModal.value = true
  } catch (error) { message.error('预览失败') }
}

// --- 队列与对比 (Req 10.3) ---
const loadQueue = async () => {
  try { const res = await getRunningQueue(); queueData.value = res.data || [] }
  catch (error) { console.error('加载队列失败:', error) }
}

const handleCompare = async () => {
  if (!compareLogId1.value || !compareLogId2.value) { message.warning('请输入两个日志ID'); return }
  comparing.value = true
  try { const res = await compareExecutions(compareLogId1.value, compareLogId2.value); compareResult.value = res.data }
  catch (error: any) { message.error(error.message || '对比失败') }
  finally { comparing.value = false }
}

const diffClass = (val: number) => val > 0 ? 'diff-positive' : val < 0 ? 'diff-negative' : ''
const formatDiff = (val: number | undefined) => {
  if (val === undefined || val === null) return '-'
  const num = typeof val === 'number' ? val : 0
  if (num > 0) return '+' + (Number.isInteger(num) ? num : num.toFixed(1))
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

// --- 批量操作 (Req 10.2) ---
const batchExecute = () => {
  if (checkedRowKeys.value.length === 0) { message.warning('请先选择任务'); return }
  dialog.info({
    title: '确认批量执行',
    content: `确定批量执行选中的 ${checkedRowKeys.value.length} 个任务？`,
    positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => {
      try { await batchExecuteJobs(checkedRowKeys.value as number[]); message.success('批量执行已启动'); load() }
      catch (error) { message.error('批量执行失败') }
    }
  })
}

const batchStart = async () => {
  if (checkedRowKeys.value.length === 0) { message.warning('请先选择任务'); return }
  dialog.info({
    title: '确认批量启动',
    content: `确定批量启动选中的 ${checkedRowKeys.value.length} 个任务的调度？`,
    positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => {
      for (const id of checkedRowKeys.value) { try { await startJob(id as number) } catch (e) { /* ignore */ } }
      message.success('批量启动完成'); load()
    }
  })
}

const batchStop = async () => {
  if (checkedRowKeys.value.length === 0) { message.warning('请先选择任务'); return }
  dialog.info({
    title: '确认批量停止',
    content: `确定批量停止选中的 ${checkedRowKeys.value.length} 个任务的调度？`,
    positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => {
      for (const id of checkedRowKeys.value) { try { await stopJob(id as number) } catch (e) { /* ignore */ } }
      message.success('批量停止完成'); load()
    }
  })
}

const batchDelete = () => {
  if (checkedRowKeys.value.length === 0) { message.warning('请先选择任务'); return }
  dialog.warning({
    title: '确认批量删除',
    content: `确定批量删除选中的 ${checkedRowKeys.value.length} 个任务？此操作不可恢复！`,
    positiveText: '确定', negativeText: '取消',
    onPositiveClick: async () => {
      try { await batchDeleteJobs(checkedRowKeys.value as number[]); message.success('批量删除成功'); handleCheck([]); load() }
      catch (error) { message.error('批量删除失败') }
    }
  })
}

onMounted(() => { loadDataSources() })
onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
  if (queueTimer) clearInterval(queueTimer)
})
</script>

<style scoped>
.data-transfer-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Batch toolbar */
.batch-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  margin-bottom: 12px;
}
.batch-toolbar-left { display: flex; gap: 8px; }
.batch-toolbar-right { color: #6b7280; font-size: 13px; }
.selected-info strong { color: #3b82f6; }

/* Progress modal */
.progress-content { text-align: center; padding: 20px 0; }
.progress-msg { color: #6b7280; margin: 16px 0 8px; }
.progress-stats { font-size: 13px; color: #9ca3af; }
.progress-stats .read-count { color: #10b981; font-weight: 600; }
.progress-stats .write-count { color: #3b82f6; font-weight: 600; }
.progress-hint { color: #9ca3af; font-size: 12px; margin-left: 10px; }

/* Column mapping */
.column-mapping-container { width: 100%; }
.mapping-toolbar { display: flex; gap: 8px; margin-bottom: 8px; }
.mapping-columns { display: flex; gap: 16px; align-items: stretch; }
.mapping-column { flex: 1; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }
.mapping-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; background: linear-gradient(135deg, #f8f9ff 0%, #e8f0ff 100%); font-weight: 600; color: #1e293b; }
.mapping-header .col-count { font-size: 12px; color: #9ca3af; font-weight: normal; }
.mapping-body { max-height: 200px; overflow-y: auto; padding: 8px; }
.mapping-empty { text-align: center; color: #9ca3af; padding: 30px 10px; }
.mapping-list { display: flex; flex-direction: column; gap: 4px; }
.mapping-item { padding: 6px 8px; border-radius: 4px; }
.mapping-item:hover { background: #f5f7fa; }
.mapping-item .col-name { font-weight: 500; color: #1e293b; }
.mapping-item .col-type { color: #9ca3af; font-size: 12px; margin-left: 4px; }
.mapping-arrow { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 0 10px; }
.mapping-arrow p { color: #9ca3af; font-size: 12px; margin: 8px 0 0 0; }

/* Cron builder */
.cron-builder { width: 100%; }
.cron-custom { padding: 10px; background: #f8fafc; border-radius: 8px; }
.cron-desc { color: #10b981; font-size: 13px; font-weight: 500; }

/* Task queue drawer (Req 10.3) */
.queue-section { margin-bottom: 8px; }
.queue-section-title { display: flex; align-items: center; gap: 6px; font-weight: 600; color: #1e293b; margin-bottom: 12px; font-size: 14px; }
.queue-section-empty { text-align: center; color: #9ca3af; padding: 16px; font-size: 13px; }
.queue-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; color: #9ca3af; }
.queue-empty p { margin: 12px 0 0 0; font-size: 14px; }
.queue-list { display: flex; flex-direction: column; gap: 8px; }
.queue-item { background: #f8fafc; border-radius: 12px; padding: 16px; border: 1px solid #e5e7eb; }
.queue-item-queued { border-left: 3px solid #3b82f6; }
.queue-item-running { border-left: 3px solid #f59e0b; }
.queue-item-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.queue-log-id { font-size: 13px; color: #6b7280; font-weight: 500; }
.queue-item-stats { display: flex; gap: 20px; margin-top: 10px; font-size: 13px; color: #6b7280; }
.queue-item-msg { color: #9ca3af; font-size: 12px; margin: 6px 0 0 0; }

/* Compare modal */
.compare-result { margin-top: 8px; }
.compare-grid { border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; }
.compare-header { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; background: linear-gradient(135deg, #f0f4ff 0%, #e8f0ff 100%); padding: 12px 16px; font-weight: 600; font-size: 13px; color: #1e293b; }
.compare-row { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; padding: 10px 16px; border-top: 1px solid #f1f5f9; font-size: 13px; color: #4b5563; }
.compare-row:hover { background: #f8fafc; }
.compare-row span:first-child { font-weight: 500; color: #1e293b; }
.diff-positive { color: #10b981 !important; font-weight: 600; }
.diff-negative { color: #ef4444 !important; font-weight: 600; }

@media (max-width: 768px) {
  .page-header-stats { flex-direction: column; }
  .batch-toolbar { flex-direction: column; gap: 8px; }
  .batch-toolbar-left { flex-wrap: wrap; }
}






</style>

<style>
/* DataTransfer 深色模式（非 scoped） */
html.dark .batch-toolbar-right { color: #94a3b8 !important; }
html.dark .selected-info strong { color: #60a5fa !important; }
html.dark .progress-msg { color: #94a3b8 !important; }
html.dark .progress-stats { color: #64748b !important; }
html.dark .progress-hint { color: #64748b !important; }
html.dark .mapping-column { border-color: #334155 !important; }
html.dark .mapping-header { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; color: #e2e8f0 !important; }
html.dark .mapping-header .col-count { color: #64748b !important; }
html.dark .mapping-empty { color: #64748b !important; }
html.dark .mapping-item:hover { background: #243044 !important; }
html.dark .mapping-item .col-name { color: #e2e8f0 !important; }
html.dark .mapping-item .col-type { color: #64748b !important; }
html.dark .mapping-arrow p { color: #64748b !important; }
html.dark .cron-custom { background: #1a2536 !important; }
html.dark .queue-section-title { color: #e2e8f0 !important; }
html.dark .queue-section-empty { color: #64748b !important; }
html.dark .queue-empty { color: #64748b !important; }
html.dark .queue-item { background: #1a2536 !important; border-color: #334155 !important; }
html.dark .queue-log-id { color: #94a3b8 !important; }
html.dark .queue-item-stats { color: #94a3b8 !important; }
html.dark .queue-item-msg { color: #64748b !important; }
html.dark .compare-grid { border-color: #334155 !important; }
html.dark .compare-header { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; color: #e2e8f0 !important; }
html.dark .compare-row { border-top-color: #334155 !important; color: #cbd5e1 !important; }
html.dark .compare-row:hover { background: #243044 !important; }
html.dark .compare-row span:first-child { color: #e2e8f0 !important; }
</style>
