<template>
  <div class="report-version-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><GitCommitOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ versions.length }}</span>
          <span class="stat-label">版本总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><DocumentTextOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ reportOptions.length }}</span>
          <span class="stat-label">报表数量</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><GitCompareOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ checkedVersions.length }}</span>
          <span class="stat-label">已选版本</span>
        </div>
      </div>
    </div>

    <!-- 操作区 -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="20" color="#6366f1"><GitBranchOutline /></n-icon>
            <span>报表版本管理</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-select
            v-model:value="selectedReportId"
            :options="reportOptions"
            placeholder="选择报表"
            filterable
            style="width: 280px"
            :loading="loadingReports"
            @update:value="loadVersions"
          />
          <n-button
            v-if="selectedReportId"
            type="primary"
            @click="showCreateModal = true"
          >
            <template #icon><n-icon><AddOutline /></n-icon></template>
            创建版本
          </n-button>
          <n-button
            type="info"
            :disabled="checkedVersions.length !== 2"
            @click="handleCompare"
          >
            <template #icon><n-icon><GitCompareOutline /></n-icon></template>
            对比选中版本
          </n-button>
        </n-space>
      </template>

      <!-- 版本列表 -->
      <n-data-table
        :columns="versionColumns"
        :data="versions"
        :loading="loadingVersions"
        :row-key="(row) => row.id"
        :scroll-x="900"
        size="small"
        striped
        class="custom-table"
        @update:checked-row-keys="handleCheck"
      />
    </n-card>

    <!-- 版本对比 -->
    <n-card v-if="compareResult" class="main-card" style="margin-top: 16px">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="20" color="#f59e0b"><GitCompareOutline /></n-icon>
            <span>版本对比：v{{ compareResult.version1.versionNo }} ↔ v{{ compareResult.version2.versionNo }}</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-button size="small" quaternary @click="compareResult = null">关闭</n-button>
      </template>

      <n-empty v-if="compareResult.diffs.length === 0" description="两个版本没有差异" />
      <n-data-table
        v-else
        :columns="diffColumns"
        :data="compareResult.diffs"
        size="small"
        striped
        class="custom-table"
      />
    </n-card>

    <!-- 创建版本弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="dialog" title="创建版本" style="width: 480px; border-radius: 16px;">
      <n-form-item label="版本说明">
        <n-input
          v-model:value="versionSummary"
          type="textarea"
          placeholder="描述本次修改内容"
          :autosize="{ minRows: 2, maxRows: 4 }"
        />
      </n-form-item>
      <template #action>
        <n-button @click="showCreateModal = false">取消</n-button>
        <n-button type="primary" :loading="creating" @click="handleCreate">创建</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, onMounted } from 'vue'
import { useMessage, NButton, NPopconfirm, NTag, NSpace } from 'naive-ui'
import {
  GitBranchOutline, GitCompareOutline, GitCommitOutline,
  AddOutline, DocumentTextOutline
} from '@vicons/ionicons5'
import type { DataTableColumns, DataTableRowKey } from 'naive-ui'
import type { ReportVersion, VersionCompareResult, VersionDiff } from '@/types/version'
import {
  getVersionHistory, createVersion, compareVersions,
  rollbackToVersion, deleteVersion
} from '@/api/reportVersion'
import { getReportList } from '@/api/report'

const message = useMessage()

const selectedReportId = ref<number | null>(null)
const reportOptions = ref<Array<{ label: string; value: number }>>([])
const loadingReports = ref(false)
const loadingVersions = ref(false)
const creating = ref(false)
const showCreateModal = ref(false)
const versionSummary = ref('')

const versions = ref<ReportVersion[]>([])
const checkedVersions = ref<number[]>([])
const compareResult = ref<VersionCompareResult | null>(null)

const changeTypeMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' }> = {
  added: { label: '新增', type: 'success' },
  removed: { label: '删除', type: 'error' },
  modified: { label: '修改', type: 'warning' }
}

const versionColumns: DataTableColumns<ReportVersion> = [
  { type: 'selection' },
  {
    title: '版本号',
    key: 'versionNo',
    width: 100,
    render: (row) => h(NTag, { type: 'info', size: 'small', round: true }, { default: () => `v${row.versionNo}` })
  },
  {
    title: '修改说明',
    key: 'summary',
    ellipsis: { tooltip: true },
    render: (row) => row.summary || '-'
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: (row) => h(NSpace, { size: 'small' }, {
      default: () => [
        h(NPopconfirm, {
          onPositiveClick: () => handleRollback(row)
        }, {
          trigger: () => h(NButton, { size: 'tiny', type: 'primary', quaternary: true }, { default: () => '回滚' }),
          default: () => `确定回滚到 v${row.versionNo} 吗？`
        }),
        h(NPopconfirm, {
          onPositiveClick: () => handleDelete(row.id!)
        }, {
          trigger: () => h(NButton, { size: 'tiny', type: 'error', quaternary: true }, { default: () => '删除' }),
          default: () => '确定要删除此版本吗？'
        })
      ]
    })
  }
]

const diffColumns: DataTableColumns<VersionDiff> = [
  { title: '字段', key: 'field', width: 180 },
  {
    title: '变更类型',
    key: 'changeType',
    width: 100,
    render: (row) => {
      const info = changeTypeMap[row.changeType] || { label: row.changeType, type: 'default' as const }
      return h(NTag, { type: info.type, size: 'small' }, { default: () => info.label })
    }
  },
  {
    title: '旧值',
    key: 'oldValue',
    ellipsis: { tooltip: true },
    render: (row) => String(row.oldValue ?? '-')
  },
  {
    title: '新值',
    key: 'newValue',
    ellipsis: { tooltip: true },
    render: (row) => String(row.newValue ?? '-')
  }
]

async function loadReports() {
  loadingReports.value = true
  try {
    const res = await getReportList({ page: 1, pageSize: 200 })
    reportOptions.value = (res.data?.list || []).map((r: any) => ({
      label: r.reportName || r.name,
      value: r.id
    }))
  } catch {
    message.error('加载报表列表失败')
  } finally {
    loadingReports.value = false
  }
}

async function loadVersions() {
  if (!selectedReportId.value) return
  loadingVersions.value = true
  compareResult.value = null
  checkedVersions.value = []
  try {
    const res = await getVersionHistory(selectedReportId.value)
    versions.value = res.data || []
  } catch {
    message.error('加载版本历史失败')
  } finally {
    loadingVersions.value = false
  }
}

function handleCheck(keys: DataTableRowKey[]) {
  if (keys.length > 2) {
    message.warning('最多选择两个版本进行对比')
    return
  }
  checkedVersions.value = keys as number[]
}

async function handleCreate() {
  if (!selectedReportId.value) return
  creating.value = true
  try {
    await createVersion(selectedReportId.value, versionSummary.value)
    message.success('版本创建成功')
    showCreateModal.value = false
    versionSummary.value = ''
    loadVersions()
  } catch {
    message.error('创建版本失败')
  } finally {
    creating.value = false
  }
}

async function handleCompare() {
  if (checkedVersions.value.length !== 2) {
    message.warning('请选择两个版本进行对比')
    return
  }
  try {
    const res = await compareVersions(checkedVersions.value[0], checkedVersions.value[1])
    compareResult.value = res.data
  } catch {
    message.error('版本对比失败')
  }
}

async function handleRollback(version: ReportVersion) {
  if (!selectedReportId.value || !version.id) return
  try {
    await rollbackToVersion(selectedReportId.value, version.id)
    message.success(`已回滚到 v${version.versionNo}`)
  } catch {
    message.error('回滚失败')
  }
}

async function handleDelete(versionId: number) {
  try {
    await deleteVersion(versionId)
    message.success('删除成功')
    loadVersions()
  } catch {
    message.error('删除失败')
  }
}

onMounted(() => {
  loadReports()
})
</script>

<style scoped>
.report-version-page {
  padding: 16px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.main-card {
  margin-top: 16px;
}
</style>
