<template>
  <div class="data-quality">
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary"><n-icon size="24"><ShieldCheckmarkOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ rules.length }}</span><span class="stat-label">{{ t('dataQuality.qualityRules') }}</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success"><n-icon size="24"><CheckmarkCircleOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ enabledRuleCount }}</span><span class="stat-label">{{ t('dataQuality.enabled') }}</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning"><n-icon size="24"><AnalyticsOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ report ? report.score.toFixed(0) : '-' }}</span><span class="stat-label">{{ t('dataQuality.latestScore') }}</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info"><n-icon size="24"><SparklesOutline /></n-icon></div>
        <div class="stat-info"><span class="stat-value">{{ reportHistory.length }}</span><span class="stat-label">{{ t('dataQuality.historyReports') }}</span></div>
      </div>
    </div>

    <!-- 质量检查 -->
    <n-card style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><SearchOutline /></n-icon>
          <span>{{ t('dataQuality.qualityCheck') }}</span>
        </div>
      </template>
      <n-form :model="checkForm" inline class="query-form">
        <n-form-item :label="t('dataQuality.dataSource')">
          <n-select v-model:value="checkForm.dataSourceId" :options="dataSourceOptions" :placeholder="t('dataQuality.dataSourcePlaceholder')" style="width: 220px;" :loading="loadingDataSources" @update:value="loadTables" />
        </n-form-item>
        <n-form-item :label="t('dataQuality.tableName')">
          <n-select v-model:value="checkForm.tableName" :options="tableOptions" :placeholder="t('dataQuality.tableNamePlaceholder')" style="width: 220px;" filterable />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" :loading="checking" @click="runCheck">
            <template #icon><n-icon><SearchOutline /></n-icon></template>
            {{ t('dataQuality.checkQuality') }}
          </n-button>
        </n-form-item>
      </n-form>
    </n-card>

    <!-- 质量报告 -->
    <n-card v-if="report" style="margin-bottom: 16px;">
      <template #header>
        <n-space align="center" :size="8">
          <span class="card-title-text">{{ t('dataQuality.qualityReportTitle', { table: report.tableName }) }}</span>
          <StatusTag :status="getScoreLevel(report.score)" :status-map="scoreStatusMap" />
        </n-space>
      </template>
      <template #header-extra>
        <n-button size="small" type="info" @click="generateReport">
          <template #icon><n-icon><DocumentTextOutline /></n-icon></template>
          {{ t('dataQuality.generateReport') }}
        </n-button>
      </template>
      <div class="score-dashboard">
        <n-progress type="dashboard" :percentage="report.score" :color="getScoreColor(report.score)">
          <div class="score-text">
            <span class="score-value">{{ report.score.toFixed(0) }}</span>
            <span class="score-label">{{ t('dataQuality.qualityScore') }}</span>
          </div>
        </n-progress>
      </div>
      <n-data-table :columns="detailColumns" :data="report.details || []" size="small" :max-height="300" :scroll-x="600" striped class="custom-table" />
    </n-card>

    <!-- 报告历史 -->
    <n-card v-if="reportHistory.length > 0" style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="#059669" class="header-icon"><TimeOutline /></n-icon>
          <span>{{ t('dataQuality.checkHistory') }}</span>
        </div>
      </template>
      <n-data-table :columns="historyColumns" :data="reportHistory" size="small" :max-height="250" :scroll-x="500" striped class="custom-table" />
    </n-card>

    <!-- 规则管理 -->
    <n-card>
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="#F59E0B" class="header-icon"><ListOutline /></n-icon>
          <span>{{ t('dataQuality.qualityRules') }}</span>
        </div>
      </template>
      <template #header-extra>
        <n-button type="primary" size="small" @click="ruleModal.openCreate()">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          {{ t('dataQuality.addRule') }}
        </n-button>
      </template>
      <n-data-table :columns="ruleColumns" :data="rules" :loading="loadingRules" size="small" :scroll-x="900" striped class="custom-table" />
    </n-card>

    <!-- 添加/编辑规则弹窗（使用 useFormModal）(Req 9.2) -->
    <n-modal v-model:show="ruleModal.visible.value" preset="card" :title="ruleModal.mode.value === 'create' ? t('dataQuality.addQualityRule') : t('dataQuality.editRule')" class="dp-modal-md">
      <n-form ref="ruleFormRef" :model="ruleModal.formData.value" :rules="ruleFormRules" label-placement="left" label-width="100px">
        <n-form-item :label="t('dataQuality.ruleName')" path="ruleName"><n-input v-model:value="ruleModal.formData.value.ruleName" :placeholder="t('dataQuality.ruleNamePlaceholder')" /></n-form-item>
        <n-form-item :label="t('dataQuality.ruleType')" path="ruleType"><n-select v-model:value="ruleModal.formData.value.ruleType" :options="ruleTypeOptions" :placeholder="t('dataQuality.ruleTypePlaceholder')" /></n-form-item>
        <n-form-item :label="t('dataQuality.dataSourceLabel')" path="dataSourceId"><n-select v-model:value="ruleModal.formData.value.dataSourceId" :options="dataSourceOptions" :placeholder="t('dataQuality.dataSourcePlaceholder')" @update:value="loadTablesForRule" /></n-form-item>
        <n-form-item :label="t('dataQuality.tableLabel')" path="tableName"><n-select v-model:value="ruleModal.formData.value.tableName" :options="ruleTableOptions" :placeholder="t('dataQuality.tableNamePlaceholder')" filterable /></n-form-item>
        <n-form-item :label="t('dataQuality.columnName')"><n-input v-model:value="ruleModal.formData.value.columnName" :placeholder="t('dataQuality.columnNamePlaceholder')" /></n-form-item>
        <n-form-item :label="t('dataQuality.alertThreshold')">
          <n-slider v-model:value="ruleModal.formData.value.threshold" :min="0" :max="100" :step="5" />
          <span style="margin-left: 12px; min-width: 40px;">{{ ruleModal.formData.value.threshold }}{{ t('dataQuality.points') }}</span>
        </n-form-item>
        <n-form-item :label="t('dataQuality.severityLevel')"><n-select v-model:value="ruleModal.formData.value.severity" :options="severityOptions" :placeholder="t('dataQuality.severityPlaceholder')" /></n-form-item>
        <n-form-item :label="t('dataQuality.description')"><n-input v-model:value="ruleModal.formData.value.description" type="textarea" :rows="2" :placeholder="t('dataQuality.descriptionPlaceholder')" /></n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="ruleModal.close()">{{ t('dataQuality.cancel') }}</n-button>
            <n-button type="primary" :loading="ruleModal.submitting.value" @click="handleSaveRule">{{ t('dataQuality.save') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 生成报告弹窗 -->
    <n-modal v-model:show="showReportModal" preset="card" :title="t('dataQuality.dataQualityReport')" class="dp-modal-lg">
      <div v-if="generatedReport" class="generated-report">
        <!-- 标题行 -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <h3 style="margin: 0;">{{ t('dataQuality.qualityReportTitle', { table: generatedReport.tableName }) }}</h3>
          <n-space>
            <StatusTag :status="getScoreLevel(generatedReport.score)" :status-map="scoreStatusMap" size="medium" />
            <n-tag size="small">{{ generatedReport.createTime || t('dataQuality.justGenerated') }}</n-tag>
          </n-space>
        </div>

        <!-- 总分仪表盘 -->
        <div class="score-dashboard">
          <n-progress type="dashboard" :percentage="generatedReport.score" :color="getScoreColor(generatedReport.score)">
            <div class="score-text">
              <span class="score-value">{{ generatedReport.score.toFixed(0) }}</span>
              <span class="score-label">{{ t('dataQuality.qualityScore') }}</span>
            </div>
          </n-progress>
        </div>

        <!-- 概览统计 -->
        <div v-if="reportStats" class="report-section">
          <h4>{{ t('dataQuality.overviewStats') }}</h4>
          <n-grid :cols="4" :x-gap="12" :y-gap="12">
            <n-gi>
              <n-statistic :label="t('dataQuality.totalFields')" :value="reportStats.totalFields" />
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dataQuality.avgNullRate')">
                <template #default>
                  <span :style="{ color: reportStats.avgNull > 0.3 ? '#d03050' : reportStats.avgNull > 0.1 ? '#f0a020' : '#18a058' }">
                    {{ (reportStats.avgNull * 100).toFixed(1) }}%
                  </span>
                </template>
              </n-statistic>
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dataQuality.avgDuplicateRate')">
                <template #default>
                  <span :style="{ color: reportStats.avgDup > 0.5 ? '#d03050' : reportStats.avgDup > 0.1 ? '#f0a020' : '#18a058' }">
                    {{ (reportStats.avgDup * 100).toFixed(1) }}%
                  </span>
                </template>
              </n-statistic>
            </n-gi>
            <n-gi>
              <n-statistic :label="t('dataQuality.avgTypeConsistency')">
                <template #default>
                  <span :style="{ color: reportStats.avgConsistency < 0.8 ? '#d03050' : '#18a058' }">
                    {{ (reportStats.avgConsistency * 100).toFixed(0) }}%
                  </span>
                </template>
              </n-statistic>
            </n-gi>
          </n-grid>
        </div>

        <!-- 质量维度评分 -->
        <div v-if="reportStats" class="report-section">
          <h4>{{ t('dataQuality.dimensionScores') }}</h4>
          <n-grid :cols="3" :x-gap="12" :y-gap="12">
            <n-gi>
              <n-card size="small" :bordered="true">
                <div style="text-align: center;">
                  <div style="font-size: 28px; font-weight: 700;" :style="{ color: getScoreColor(reportStats.completeness) }">
                    {{ reportStats.completeness.toFixed(0) }}
                  </div>
                  <div style="font-size: 13px; color: #6b7280; margin: 4px 0 8px;">{{ t('dataQuality.completenessScore') }}</div>
                  <n-progress :percentage="reportStats.completeness" :show-indicator="false" :color="getScoreColor(reportStats.completeness)" :rail-color="'#e5e7eb'" :height="6" />
                </div>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card size="small" :bordered="true">
                <div style="text-align: center;">
                  <div style="font-size: 28px; font-weight: 700;" :style="{ color: getScoreColor(reportStats.uniqueness) }">
                    {{ reportStats.uniqueness.toFixed(0) }}
                  </div>
                  <div style="font-size: 13px; color: #6b7280; margin: 4px 0 8px;">{{ t('dataQuality.uniquenessScore') }}</div>
                  <n-progress :percentage="reportStats.uniqueness" :show-indicator="false" :color="getScoreColor(reportStats.uniqueness)" :rail-color="'#e5e7eb'" :height="6" />
                </div>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card size="small" :bordered="true">
                <div style="text-align: center;">
                  <div style="font-size: 28px; font-weight: 700;" :style="{ color: getScoreColor(reportStats.consistency) }">
                    {{ reportStats.consistency.toFixed(0) }}
                  </div>
                  <div style="font-size: 13px; color: #6b7280; margin: 4px 0 8px;">{{ t('dataQuality.consistencyScore') }}</div>
                  <n-progress :percentage="reportStats.consistency" :show-indicator="false" :color="getScoreColor(reportStats.consistency)" :rail-color="'#e5e7eb'" :height="6" />
                </div>
              </n-card>
            </n-gi>
          </n-grid>
        </div>

        <!-- 问题字段 -->
        <div v-if="reportStats" class="report-section">
          <h4>{{ t('dataQuality.problemFields') }}</h4>
          <div v-if="reportStats.problemFields.length > 0">
            <p style="color: #6b7280; font-size: 13px; margin: 0 0 8px;">{{ t('dataQuality.problemFieldsDesc') }}</p>
            <n-space :size="8" style="margin-bottom: 8px;">
              <n-tag v-for="pf in reportStats.problemFields" :key="pf.fieldName" :type="pf.nullRate > 0.3 || pf.duplicateRate > 0.5 ? 'error' : 'warning'" size="small">
                {{ pf.fieldName }}
                <template #icon>
                  <span v-if="pf.nullRate > 0.3">{{ t('dataQuality.highNullRate') }}</span>
                  <span v-else-if="pf.duplicateRate > 0.5">{{ t('dataQuality.highDuplicateRate') }}</span>
                </template>
              </n-tag>
            </n-space>
          </div>
          <n-tag v-else type="success" size="small">{{ t('dataQuality.noProblemFields') }}</n-tag>
        </div>

        <n-divider />

        <!-- 字段质量明细 -->
        <h4>{{ t('dataQuality.fieldQualityDetail') }}</h4>
        <n-data-table :columns="detailColumns" :data="generatedReport.details || []" size="small" :max-height="300" :scroll-x="600" striped class="custom-table" />

        <!-- 建议 -->
        <div v-if="reportStats" class="report-section" style="margin-top: 16px;">
          <h4>{{ t('dataQuality.recommendations') }}</h4>
          <div v-if="reportStats.problemFields.length > 0">
            <n-alert v-if="reportStats.avgNull > 0.1" type="warning" style="margin-bottom: 8px;" :show-icon="true">
              {{ t('dataQuality.recReduceNull') }}
            </n-alert>
            <n-alert v-if="reportStats.avgDup > 0.1" type="warning" style="margin-bottom: 8px;" :show-icon="true">
              {{ t('dataQuality.recReduceDuplicate') }}
            </n-alert>
          </div>
          <n-alert v-else type="success" :show-icon="true">
            {{ t('dataQuality.recAllGood') }}
          </n-alert>
        </div>

        <n-divider />

        <!-- AI 一键总结 -->
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
          <n-button type="primary" :loading="reportAiLoading" :disabled="!!reportAiSummary" @click="aiSummarizeReport">
            <template #icon><n-icon><SparklesOutline /></n-icon></template>
            {{ reportAiLoading ? t('dataQuality.aiSummarizing') : t('dataQuality.aiSummarize') }}
          </n-button>
          <span v-if="!reportAiSummary && !reportAiLoading" style="color: #9ca3af; font-size: 13px;">{{ t('dataQuality.aiAnalyzing').replace('...', '') }}</span>
        </div>
        <div v-if="reportAiSummary">
          <h4><n-icon size="18" color="#8B5CF6"><SparklesOutline /></n-icon> {{ t('dataQuality.aiAnalysisSummary') }}</h4>
          <div class="ai-result-content" v-html="renderMarkdown(reportAiSummary)" />
        </div>
        <n-spin v-if="reportAiLoading" size="small" style="display: block; margin: 16px auto;">
          <template #description>{{ t('dataQuality.aiAnalyzing') }}</template>
        </n-spin>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showReportModal = false">{{ t('dataQuality.close') }}</n-button>
          <n-button @click="exportReportAsText">{{ t('dataQuality.exportReport') }}</n-button>
          <n-button type="primary" @click="exportReportAsHtml">{{ t('dataQuality.exportAsHtml') }}</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, h } from 'vue'
import request from '@/api/request'
import { NButton, NTag, NSpace, useMessage, type DataTableColumns, type FormInst } from 'naive-ui'
import { ShieldCheckmarkOutline, CheckmarkCircleOutline, AnalyticsOutline, SearchOutline, ListOutline, AddOutline, SparklesOutline, DocumentTextOutline, TimeOutline } from '@vicons/ionicons5'
import type { DataQualityRule, QualityReport, FieldQualityDetail } from '@/types/dataQuality'
import { checkQuality, getRules, saveRule, deleteRule, getReportHistory } from '@/api/dataQuality'
import { getDataSourceList, getDataSourceTables } from '@/api/dataSource'
import { handleApiError } from '@/utils/error'
import { initMessage } from '@/utils/message'
import { useFormModal } from '@/composables/useFormModal'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
initMessage(message)

// --- 状态映射（用于 StatusTag）(Req 9.3) ---
const ruleStatusMap = computed(() => ({
  1: { label: t('dataQuality.statusEnabled'), type: 'success' as const },
  0: { label: t('dataQuality.statusDisabled'), type: 'default' as const }
}))

const severityStatusMap = computed(() => ({
  low: { label: t('dataQuality.severityLow'), type: 'info' as const },
  medium: { label: t('dataQuality.severityMedium'), type: 'warning' as const },
  high: { label: t('dataQuality.severityHigh'), type: 'error' as const }
}))

const ruleTypeStatusMap = computed(() => ({
  completeness: { label: t('dataQuality.typeCompleteness'), type: 'default' as const },
  accuracy: { label: t('dataQuality.typeAccuracy'), type: 'default' as const },
  consistency: { label: t('dataQuality.typeConsistency'), type: 'default' as const },
  timeliness: { label: t('dataQuality.typeTimeliness'), type: 'default' as const },
  uniqueness: { label: t('dataQuality.typeUniqueness'), type: 'default' as const }
}))

const scoreStatusMap = computed(() => ({
  good: { label: t('dataQuality.scoreGood'), type: 'success' as const },
  fair: { label: t('dataQuality.scoreFair'), type: 'warning' as const },
  poor: { label: t('dataQuality.scorePoor'), type: 'error' as const }
}))

function getScoreLevel(score: number): string {
  if (score >= 80) return 'good'
  if (score >= 60) return 'fair'
  return 'poor'
}

const checkForm = ref({ dataSourceId: null as number | null, tableName: '' })
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const tableOptions = ref<Array<{ label: string; value: string }>>([])
const ruleTableOptions = ref<Array<{ label: string; value: string }>>([])
const loadingDataSources = ref(false)
const checking = ref(false)
const loadingRules = ref(false)
const ruleFormRef = ref<FormInst | null>(null)

const report = ref<QualityReport | null>(null)
const rules = ref<DataQualityRule[]>([])
const reportHistory = ref<any[]>([])

// 报告生成
const showReportModal = ref(false)
const generatedReport = ref<QualityReport | null>(null)
const reportAiSummary = ref('')
const reportAiLoading = ref(false)

const enabledRuleCount = computed(() => rules.value.filter(r => r.status === 1).length)

// ========== 报告增强统计 ==========
const reportStats = computed(() => {
  const r = generatedReport.value
  if (!r?.details?.length) return null
  const details = r.details
  const totalFields = details.length
  const avgNull = details.reduce((s, d) => s + d.nullRate, 0) / totalFields
  const avgDup = details.reduce((s, d) => s + d.duplicateRate, 0) / totalFields
  const avgConsistency = details.reduce((s, d) => s + d.typeConsistency, 0) / totalFields
  // 维度评分 (0-100)
  const completeness = ((1 - avgNull) * 100)
  const uniqueness = ((1 - avgDup) * 100)
  const consistency = (avgConsistency * 100)
  // 问题字段
  const problemFields = details.filter(d => d.nullRate > 0.3 || d.duplicateRate > 0.5)
  return { totalFields, avgNull, avgDup, avgConsistency, completeness, uniqueness, consistency, problemFields }
})

// --- 使用 useFormModal 管理规则弹窗 (Req 9.2) ---
interface RuleFormData extends Record<string, unknown> {
  ruleName: string
  ruleType: string
  dataSourceId: number | null
  tableName: string
  columnName: string
  threshold: number
  severity: string
  description: string
}

const ruleModal = useFormModal<RuleFormData>({
  defaultFormData: () => ({
    ruleName: '', ruleType: 'completeness', dataSourceId: null,
    tableName: '', columnName: '', threshold: 80, severity: 'medium', description: ''
  }),
  createFn: async (data) => {
    await saveRule({
      ruleName: data.ruleName, ruleType: data.ruleType,
      dataSourceId: data.dataSourceId!, tableName: data.tableName,
      columnName: data.columnName || undefined, threshold: data.threshold,
      severity: data.severity, description: data.description || undefined
    } as DataQualityRule)
    message.success(t('dataQuality.ruleSaved'))
  },
  updateFn: async (data) => {
    await saveRule({
      ruleName: data.ruleName, ruleType: data.ruleType,
      dataSourceId: data.dataSourceId!, tableName: data.tableName,
      columnName: data.columnName || undefined, threshold: data.threshold,
      severity: data.severity, description: data.description || undefined
    } as DataQualityRule)
    message.success(t('dataQuality.ruleUpdated'))
  },
  onSuccess: () => { loadRules() },
  onError: (error) => { message.error(handleApiError(error, t('dataQuality.saveRuleError'))) }
})

const ruleFormRules = computed(() => ({
  ruleName: { required: true, message: t('dataQuality.ruleNameRequired'), trigger: 'blur' },
  ruleType: { required: true, message: t('dataQuality.ruleTypeRequired'), trigger: 'change' },
  dataSourceId: { required: true, type: 'number' as const, message: t('dataQuality.dataSourceRequired'), trigger: 'change' },
  tableName: { required: true, message: t('dataQuality.tableRequired'), trigger: 'change' }
}))

const ruleTypeOptions = computed(() => [
  { label: t('dataQuality.typeCompleteness'), value: 'completeness' }, { label: t('dataQuality.typeAccuracy'), value: 'accuracy' },
  { label: t('dataQuality.typeConsistency'), value: 'consistency' }, { label: t('dataQuality.typeTimeliness'), value: 'timeliness' },
  { label: t('dataQuality.typeUniqueness'), value: 'uniqueness' }
])
const severityOptions = computed(() => [
  { label: t('dataQuality.severityLow'), value: 'low' }, { label: t('dataQuality.severityMedium'), value: 'medium' }, { label: t('dataQuality.severityHigh'), value: 'high' }
])

/** 简单 Markdown 渲染 */
function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    .replace(/### (.*)/g, '<h4>$1</h4>')
    .replace(/## (.*)/g, '<h3>$1</h3>')
    .replace(/# (.*)/g, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/^- (.*)/gm, '<li>$1</li>')
    .replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>')
    .replace(/\n\n/g, '<br/><br/>')
    .replace(/\n/g, '<br/>')
}

const detailColumns = computed<DataTableColumns<FieldQualityDetail>>(() => [
  { title: t('dataQuality.fieldName'), key: 'fieldName', width: 150 },
  { title: t('dataQuality.nullRate'), key: 'nullRate', width: 100, render: (row) => `${(row.nullRate * 100).toFixed(1)}%` },
  { title: t('dataQuality.duplicateRate'), key: 'duplicateRate', width: 100, render: (row) => `${(row.duplicateRate * 100).toFixed(1)}%` },
  { title: t('dataQuality.typeConsistencyCol'), key: 'typeConsistency', width: 100, render: (row) => `${(row.typeConsistency * 100).toFixed(0)}%` },
  { title: t('dataQuality.sampleValues'), key: 'sampleValues', render: (row) => row.sampleValues?.slice(0, 3).join(', ') || '-' }
])

const historyColumns = computed<DataTableColumns<any>>(() => [
  { title: t('dataQuality.tableName'), key: 'tableName', width: 150 },
  { title: t('dataQuality.score'), key: 'score', width: 80,
    render: (row) => h(StatusTag, { status: getScoreLevel(row.score || 0), statusMap: { good: { label: `${(row.score || 0).toFixed(0)}${t('dataQuality.points')}`, type: 'success' }, fair: { label: `${(row.score || 0).toFixed(0)}${t('dataQuality.points')}`, type: 'warning' }, poor: { label: `${(row.score || 0).toFixed(0)}${t('dataQuality.points')}`, type: 'error' } } })
  },
  { title: t('dataQuality.checkTime'), key: 'createTime', width: 180 },
  { title: t('dataQuality.actions'), key: 'actions', width: 80,
    render: (row) => h(ActionButtons, {
      actions: [{ label: t('dataQuality.view'), type: 'info', onClick: () => viewHistoryReport(row) }],
      row
    })
  }
])

// --- 规则表格列定义（使用 StatusTag 和 ActionButtons）(Req 9.3, 9.4) ---
const getRuleActions = (row: DataQualityRule): ActionConfig[] => [
  { label: t('dataQuality.edit'), type: 'info', onClick: () => ruleModal.openEdit({
    ruleName: row.ruleName || '',
    ruleType: row.ruleType || 'completeness',
    dataSourceId: row.dataSourceId,
    tableName: row.tableName,
    columnName: row.columnName || '',
    threshold: row.threshold ?? 80,
    severity: row.severity || 'medium',
    description: row.description || ''
  } as unknown as Partial<RuleFormData>) },
  { label: row.status === 1 ? t('dataQuality.disable') : t('dataQuality.enable'), type: row.status === 1 ? 'warning' : 'success', onClick: () => handleToggleRule(row) },
  { label: t('dataQuality.delete'), type: 'error', confirm: t('dataQuality.confirmDelete'), onClick: () => handleDeleteRule(row.id!) }
]

const ruleColumns = computed<DataTableColumns<DataQualityRule>>(() => [
  { title: t('dataQuality.ruleName'), key: 'ruleName', width: 160, render: (row) => row.ruleName || '-' },
  { title: t('dataQuality.type'), key: 'ruleType', width: 90,
    render: (row) => h(StatusTag, { status: row.ruleType || '', statusMap: ruleTypeStatusMap.value })
  },
  { title: t('dataQuality.tableName'), key: 'tableName', width: 150 },
  { title: t('dataQuality.column'), key: 'columnName', width: 120, render: (row) => row.columnName || t('dataQuality.allTable') },
  { title: t('dataQuality.threshold'), key: 'threshold', width: 80, render: (row) => row.threshold != null ? `${row.threshold}` : '-' },
  { title: t('dataQuality.severityLevel'), key: 'severity', width: 90,
    render: (row) => h(StatusTag, { status: row.severity || 'medium', statusMap: severityStatusMap.value })
  },
  { title: t('dataQuality.status'), key: 'status', width: 80,
    render: (row) => h(StatusTag, { status: row.status ?? 0, statusMap: ruleStatusMap.value })
  },
  { title: t('dataQuality.actions'), key: 'actions', width: 180,
    render: (row) => h(ActionButtons, { actions: getRuleActions(row), row })
  }
])

function _getScoreType(score: number): 'success' | 'warning' | 'error' {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'error'
}
function getScoreColor(score: number): string {
  if (score >= 80) return '#18a058'
  if (score >= 60) return '#f0a020'
  return '#d03050'
}

async function loadDataSources() {
  loadingDataSources.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    const data = (res as any).data
    const list = data?.list || data?.records || (Array.isArray(data) ? data : [])
    dataSourceOptions.value = list.map((ds: any) => ({ label: ds.name, value: ds.id }))
  } catch (error) { console.error('加载数据源失败:', error) }
  finally { loadingDataSources.value = false }
}

async function loadTables() {
  if (!checkForm.value.dataSourceId) return
  try {
    const res = await getDataSourceTables(checkForm.value.dataSourceId)
    const data = (res as any).data || []
    tableOptions.value = (Array.isArray(data) ? data : []).map((t: any) => ({ label: t.tableName || t.name, value: t.tableName || t.name }))
  } catch { tableOptions.value = [] }
}

async function loadTablesForRule() {
  if (!ruleModal.formData.value.dataSourceId) return
  try {
    const res = await getDataSourceTables(ruleModal.formData.value.dataSourceId)
    const data = (res as any).data || []
    ruleTableOptions.value = (Array.isArray(data) ? data : []).map((t: any) => ({ label: t.tableName || t.name, value: t.tableName || t.name }))
  } catch { ruleTableOptions.value = [] }
}

async function loadRules() {
  loadingRules.value = true
  try {
    const res = await getRules()
    rules.value = (res as any).data || []
  } catch (error) { console.error('加载规则失败:', error) }
  finally { loadingRules.value = false }
}

async function loadHistory() {
  try {
    const res = await getReportHistory({ page: 1, size: 20 })
    const data = (res as any).data || res
    reportHistory.value = data?.list || data?.records || (Array.isArray(data) ? data : [])
  } catch { reportHistory.value = [] }
}

async function runCheck() {
  if (!checkForm.value.dataSourceId || !checkForm.value.tableName) {
    message.warning(t('dataQuality.selectDataSourceAndTable')); return
  }
  checking.value = true
  try {
    const res = await checkQuality(checkForm.value.dataSourceId, checkForm.value.tableName)
    report.value = (res as any).data || res
    message.success(t('dataQuality.checkComplete'))
    loadHistory()
  } catch (error) {
    message.error(handleApiError(error, t('dataQuality.qualityCheckError')))
  } finally {
    checking.value = false
  }
}

/** 生成报告弹窗 */
function generateReport() {
  if (!report.value) return
  generatedReport.value = { ...report.value }
  reportAiSummary.value = ''
  showReportModal.value = true
}

/** AI 一键总结 */
async function aiSummarizeReport() {
  if (!generatedReport.value) return
  reportAiLoading.value = true
  reportAiSummary.value = ''
  try {
    const r = generatedReport.value
    const stats = reportStats.value
    const dataContext = [
      `表名: ${r.tableName}`,
      `总体评分: ${r.score.toFixed(1)}/100`,
      `字段数: ${stats?.totalFields || 0}`,
      `平均空值率: ${((stats?.avgNull || 0) * 100).toFixed(1)}%`,
      `平均重复率: ${((stats?.avgDup || 0) * 100).toFixed(1)}%`,
      `平均类型一致性: ${((stats?.avgConsistency || 1) * 100).toFixed(0)}%`,
      '字段明细:',
      ...(r.details || []).map(d =>
        `  ${d.fieldName}: 空值率=${(d.nullRate * 100).toFixed(1)}%, 重复率=${(d.duplicateRate * 100).toFixed(1)}%, 类型一致性=${(d.typeConsistency * 100).toFixed(0)}%`
      )
    ].join('\n')
    const question = '请根据以上数据质量报告，用中文给出：1.总体评价 2.主要问题分析 3.具体优化建议 4.优先级排序。请使用Markdown格式。'
    const res = await request.post('/ai/analyze', { question, dataContext })
    if ((res as any).code === 200 && (res as any).data?.success) {
      reportAiSummary.value = (res as any).data.content
      message.success(t('dataQuality.aiSummaryGenerated'))
    } else {
      message.error((res as any).message || t('dataQuality.aiSummaryFailed'))
    }
  } catch (e: any) {
    message.error(e.message || t('dataQuality.aiSummaryFailed'))
  } finally {
    reportAiLoading.value = false
  }
}

/** 构建字段行HTML */
function buildFieldRow(d: FieldQualityDetail): string {
  const tags: string[] = []
  if (d.nullRate > 0.3) tags.push('<span class="problem-tag tag-error">高空值</span>')
  else if (d.nullRate > 0.1) tags.push('<span class="problem-tag tag-warning">空值偏高</span>')
  if (d.duplicateRate > 0.5) tags.push('<span class="problem-tag tag-error">高重复</span>')
  else if (d.duplicateRate > 0.1) tags.push('<span class="problem-tag tag-warning">重复偏高</span>')
  const status = tags.length ? tags.join(' ') : '✅ 正常'
  return '<tr><td><strong>' + d.fieldName + '</strong></td><td>' + (d.nullRate * 100).toFixed(1) + '%</td><td>' + (d.duplicateRate * 100).toFixed(1) + '%</td><td>' + (d.typeConsistency * 100).toFixed(0) + '%</td><td>' + status + '</td></tr>'
}

/** 构建维度卡片HTML */
function buildDimCard(score: number, label: string): string {
  const c = getScoreColor(score)
  const s = score.toFixed(0)
  return '<div class="dim-card"><div class="dim-score" style="color:' + c + '">' + s + '</div><div class="dim-label">' + label + '</div><div class="dim-bar"><div class="fill" style="width:' + s + '%;background:' + c + '"></div></div></div>'
}

/** 导出HTML报告 */
function exportReportAsHtml() {
  if (!generatedReport.value) return
  const r = generatedReport.value
  const stats = reportStats.value
  const scoreColor = getScoreColor(r.score)

  const css = 'body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:900px;margin:0 auto;padding:40px 20px;color:#1f2937;background:#f9fafb}'
    + 'h1{text-align:center;color:#111827;border-bottom:2px solid #e5e7eb;padding-bottom:16px}'
    + '.score-box{text-align:center;margin:24px 0;padding:24px;background:#fff;border-radius:12px;box-shadow:0 1px 3px rgba(0,0,0,.1)}'
    + '.score-value{font-size:56px;font-weight:700;color:' + scoreColor + '}'
    + '.score-label{font-size:14px;color:#6b7280;margin-top:4px}'
    + '.stats-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin:24px 0}'
    + '.stat-card{background:#fff;border-radius:8px;padding:16px;text-align:center;box-shadow:0 1px 2px rgba(0,0,0,.06)}'
    + '.stat-card .value{font-size:24px;font-weight:600;color:#111827}'
    + '.stat-card .label{font-size:12px;color:#6b7280;margin-top:4px}'
    + '.dimension-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin:24px 0}'
    + '.dim-card{background:#fff;border-radius:8px;padding:20px;text-align:center;box-shadow:0 1px 2px rgba(0,0,0,.06)}'
    + '.dim-card .dim-score{font-size:32px;font-weight:700}'
    + '.dim-card .dim-label{font-size:13px;color:#6b7280;margin-top:4px}'
    + '.dim-bar{height:6px;background:#e5e7eb;border-radius:3px;margin-top:12px;overflow:hidden}'
    + '.dim-bar .fill{height:100%;border-radius:3px}'
    + 'table{width:100%;border-collapse:collapse;margin:16px 0;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 1px 2px rgba(0,0,0,.06)}'
    + 'th{background:#f3f4f6;padding:10px 12px;text-align:left;font-size:13px;color:#374151;border-bottom:1px solid #e5e7eb}'
    + 'td{padding:10px 12px;font-size:13px;border-bottom:1px solid #f3f4f6}'
    + 'tr:last-child td{border-bottom:none}'
    + '.problem-tag{display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:500}'
    + '.tag-warning{background:#fef3c7;color:#92400e}'
    + '.tag-error{background:#fee2e2;color:#991b1b}'
    + 'section{margin:32px 0}'
    + 'h2{color:#111827;font-size:18px;border-left:4px solid #6366f1;padding-left:12px}'
    + '.ai-summary{background:#fff;border-radius:8px;padding:20px;box-shadow:0 1px 2px rgba(0,0,0,.06);line-height:1.8}'
    + '.footer{text-align:center;color:#9ca3af;font-size:12px;margin-top:40px;padding-top:20px;border-top:1px solid #e5e7eb}'

  const fieldRows = (r.details || []).map(buildFieldRow).join('')
  const aiSection = reportAiSummary.value
    ? '<section><h2>🤖 AI 分析总结</h2><div class="ai-summary">' + renderMarkdown(reportAiSummary.value) + '</div></section>'
    : ''

  const html = '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><title>' + r.tableName + ' - 数据质量报告</title><style>' + css + '</style></head><body>'
    + '<h1>📊 ' + r.tableName + ' - 数据质量报告</h1>'
    + '<div class="score-box"><div class="score-value">' + r.score.toFixed(0) + '</div><div class="score-label">质量评分（满分100）</div></div>'
    + '<section><h2>概览统计</h2><div class="stats-grid">'
    + '<div class="stat-card"><div class="value">' + (stats?.totalFields || 0) + '</div><div class="label">总字段数</div></div>'
    + '<div class="stat-card"><div class="value">' + ((stats?.avgNull || 0) * 100).toFixed(1) + '%</div><div class="label">平均空值率</div></div>'
    + '<div class="stat-card"><div class="value">' + ((stats?.avgDup || 0) * 100).toFixed(1) + '%</div><div class="label">平均重复率</div></div>'
    + '<div class="stat-card"><div class="value">' + ((stats?.avgConsistency || 1) * 100).toFixed(0) + '%</div><div class="label">类型一致性</div></div>'
    + '</div></section>'
    + '<section><h2>质量维度评分</h2><div class="dimension-grid">'
    + buildDimCard(stats?.completeness || 0, '完整性')
    + buildDimCard(stats?.uniqueness || 0, '唯一性')
    + buildDimCard(stats?.consistency || 0, '一致性')
    + '</div></section>'
    + '<section><h2>字段质量明细</h2><table><tr><th>字段名</th><th>空值率</th><th>重复率</th><th>类型一致性</th><th>状态</th></tr>' + fieldRows + '</table></section>'
    + aiSection
    + '<div class="footer">报告生成时间: ' + (r.createTime || new Date().toLocaleString()) + ' | 由数据平台自动生成</div>'
    + '</body></html>'

  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'quality_report_' + r.tableName + '_' + new Date().toISOString().slice(0, 10) + '.html'
  a.click()
  URL.revokeObjectURL(url)
  message.success(t('dataQuality.reportExported'))
}

function viewHistoryReport(row: any) {
  generatedReport.value = {
    dataSourceId: row.dataSourceId, tableName: row.tableName,
    score: row.score || 0, details: row.details || [], createTime: row.createTime
  }
  reportAiSummary.value = ''
  showReportModal.value = true
}

function exportReportAsText() {
  if (!generatedReport.value) return
  const r = generatedReport.value
  let text = `${t('dataQuality.reportTitle')}\n${'='.repeat(40)}\n`
  text += `${t('dataQuality.reportTableName')}: ${r.tableName}\n${t('dataQuality.reportScore')}: ${r.score.toFixed(1)}\n${t('dataQuality.reportTime')}: ${r.createTime || new Date().toLocaleString()}\n\n`
  text += `${t('dataQuality.reportFieldDetail')}\n${'-'.repeat(40)}\n`
  ;(r.details || []).forEach(d => {
    text += `${d.fieldName}: ${t('dataQuality.reportNullRate')}=${(d.nullRate * 100).toFixed(1)}%, ${t('dataQuality.reportDuplicateRate')}=${(d.duplicateRate * 100).toFixed(1)}%, ${t('dataQuality.reportTypeConsistency')}=${(d.typeConsistency * 100).toFixed(0)}%\n`
  })
  if (reportAiSummary.value) {
    text += `\n${t('dataQuality.reportAiSummary')}\n${'-'.repeat(40)}\n${reportAiSummary.value}\n`
  }
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `quality_report_${r.tableName}_${new Date().toISOString().slice(0, 10)}.txt`
  a.click(); URL.revokeObjectURL(url)
  message.success(t('dataQuality.reportExported'))
}

async function handleSaveRule() {
  if (!ruleFormRef.value) return
  try { await ruleFormRef.value.validate() } catch { return }
  await ruleModal.submit()
}

async function handleToggleRule(row: DataQualityRule) {
  try {
    await saveRule({ ...row, status: row.status === 1 ? 0 : 1 })
    message.success(row.status === 1 ? t('dataQuality.ruleDisabled') : t('dataQuality.ruleEnabled'))
    loadRules()
  } catch (error) { message.error(handleApiError(error, t('dataQuality.toggleRuleError'))) }
}

async function handleDeleteRule(id: number) {
  try { await deleteRule(id); message.success(t('dataQuality.ruleDeleted')); loadRules() }
  catch (error) { message.error(handleApiError(error, t('dataQuality.deleteRuleError'))) }
}

onMounted(() => { loadDataSources(); loadRules(); loadHistory() })
</script>

<style scoped>
/* 使用全局 page-common.css 统一样式 */

.data-quality { padding: 0; }
.score-dashboard { display: flex; justify-content: center; padding: var(--dp-spacing-lg) 0; }
.score-text { display: flex; flex-direction: column; align-items: center; }
.score-value { font-size: 32px; font-weight: bold; }
.score-label { font-size: var(--dp-font-xs); color: var(--text-secondary); }
.ai-result-content { padding: var(--dp-spacing-sm) var(--dp-spacing-md); background: #FAFAFA; border-radius: var(--dp-radius-md); line-height: 1.8; font-size: var(--dp-font-md); max-height: 500px; overflow-y: auto; }
.ai-result-content :deep(h2), .ai-result-content :deep(h3), .ai-result-content :deep(h4) { margin: 12px 0 6px; color: var(--text-primary); }
.ai-result-content :deep(code) { background: #E5E7EB; padding: 2px 6px; border-radius: var(--dp-radius-sm); font-size: var(--dp-font-sm); }
.ai-result-content :deep(ul) { padding-left: 20px; margin: 4px 0; }
.ai-result-content :deep(li) { margin: 2px 0; }
.ai-result-content :deep(strong) { color: #1F2937; }
.report-section { margin: 16px 0; }
.generated-report h3 { color: var(--text-primary); }
.generated-report h4 { color: var(--text-primary); margin: 8px 0; display: flex; align-items: center; gap: 6px; }
@media (max-width: 768px) { }

</style>

<style>
/* DataQuality 深色模式（非 scoped） */
html.dark .ai-result-content { background: #1a2536 !important; }
html.dark .ai-result-content code { background: #334155 !important; color: #e2e8f0 !important; }
html.dark .ai-result-content strong { color: #e2e8f0 !important; }
</style>
