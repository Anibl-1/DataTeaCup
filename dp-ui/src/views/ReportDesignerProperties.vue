<template>
  <div class="designer-properties">
    <!-- 基本信息卡片 -->
    <n-card class="designer-card" size="small">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="20" color="var(--color-primary)"><InformationCircleOutline /></n-icon>
            <span>基本信息</span>
          </div>
        </div>
      </template>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="100px"
      >
        <n-grid :cols="2" :x-gap="24">
          <n-grid-item>
            <n-form-item label="报表名称" path="reportName">
              <n-input v-model:value="form.reportName" placeholder="请输入报表名称">
                <template #prefix>
                  <n-icon color="#999"><DocumentTextOutline /></n-icon>
                </template>
              </n-input>
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="数据源" path="dataSourceId">
              <n-select
                v-model:value="form.dataSourceId"
                :options="dataSourceOptions"
                placeholder="请选择数据源"
                :loading="dataSourceLoading"
                filterable
                clearable
                @update:value="$emit('dataSourceChange')"
              />
            </n-form-item>
          </n-grid-item>
        </n-grid>
        
        <n-form-item label="报表描述">
          <n-input 
            v-model:value="form.description" 
            type="textarea" 
            :rows="2" 
            placeholder="请输入报表描述（可选）"
          />
        </n-form-item>
        <n-grid :cols="3" :x-gap="24">
          <n-grid-item>
            <n-form-item label="报表类型">
              <n-radio-group :value="queryMode" @update:value="$emit('update:queryMode', $event)">
                <n-radio value="sql">SQL模式</n-radio>
                <n-radio value="visual">可视化模式</n-radio>
              </n-radio-group>
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="允许导出Excel">
              <n-switch :value="form.allowExportExcel === 1" @update:value="handleExcelSwitch" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="允许导出PDF">
              <n-switch :value="form.allowExportPdf === 1" @update:value="handlePdfSwitch" />
            </n-form-item>
          </n-grid-item>
          <n-grid-item>
            <n-form-item label="允许打印">
              <n-switch :value="form.allowPrint === 1" @update:value="handlePrintSwitch" />
            </n-form-item>
          </n-grid-item>
        </n-grid>
        <n-form-item v-if="form.allowExportPdf === 1" label="PDF水印配置">
          <n-space :size="8" align="center" style="flex-wrap: wrap; width: 100%;">
            <n-select
              v-model:value="form.watermarkType"
              :options="watermarkTypeOptions"
              placeholder="选择水印类型"
              size="medium"
              style="width: 200px"
            />
            <n-input
              v-if="form.watermarkType === 'custom'"
              v-model:value="form.pdfWatermark"
              placeholder="输入自定义水印文字"
              clearable
              style="flex: 1; min-width: 300px;"
            />
          </n-space>
        </n-form-item>
      </n-form>
    </n-card>

    <!-- SQL查询卡片 -->
    <n-card class="designer-card sql-card" size="small">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="20" color="#18a058"><CodeOutline /></n-icon>
            <span>{{ queryMode === 'sql' ? 'SQL查询语句' : '可视化查询设计器' }}</span>
          </div>
          <n-radio-group :value="queryMode" size="small" style="margin-left: 12px;" @update:value="$emit('update:queryMode', $event)">
            <n-radio-button value="sql">SQL 模式</n-radio-button>
            <n-radio-button value="visual">可视化模式</n-radio-button>
          </n-radio-group>
          <n-tag v-if="sqlValidated" type="success" size="small" style="margin-left: 8px;">
            <template #icon><n-icon><CheckmarkOutline /></n-icon></template>
            已验证
          </n-tag>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button size="small" :loading="testingSql" @click="$emit('testSql')">
            <template #icon><n-icon><PlayOutline /></n-icon></template>
            测试SQL
          </n-button>
          <n-button size="small" type="primary" :loading="fieldLoading" @click="$emit('autoGetFields')">
            <template #icon><n-icon><FlashOutline /></n-icon></template>
            自动获取字段
          </n-button>
        </n-space>
      </template>
      
      <!-- SQL 模式 -->
      <div v-if="queryMode === 'sql'" class="sql-editor-wrapper">
        <SqlEditor
          v-model="form.sqlContent"
          height="280px"
          :data-source-id="form.dataSourceId || undefined"
          :enable-auto-complete="true"
          placeholder="请输入SELECT查询语句&#10;&#10;示例：&#10;SELECT id, name, create_time FROM users WHERE status = 1&#10;&#10;注意：只允许SELECT查询，不允许使用DROP、DELETE等危险操作"
          @execute="$emit('testSql')"
        />
        <div class="sql-tips">
          <n-icon size="14" color="#999"><InformationCircleOutline /></n-icon>
          <span>支持标准SQL语法，可使用JOIN、GROUP BY、ORDER BY等。使用 ${参数名} 定义查询参数。输入时自动补全表名、字段名和SQL关键字。</span>
        </div>
      </div>

      <!-- 可视化模式 -->
      <QueryDesigner
        v-else
        :data-source-id="form.dataSourceId || null"
        @update:sql="$emit('visualSqlUpdate', $event)"
      />
    </n-card>

    <!-- 参数配置卡片 -->
    <n-card v-if="form.params && form.params.length > 0" class="designer-card params-card" size="small">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="20" color="#a855f7"><SettingsOutline /></n-icon>
            <span>查询参数配置</span>
          </div>
          <n-tag type="warning" size="small">{{ form.params.length }} 个参数</n-tag>
        </div>
      </template>
      <n-alert type="info" style="margin-bottom: 12px;" :show-icon="true">
        在SQL中使用 <n-text code>${参数名}</n-text> 语法定义查询参数，系统将自动解析并在此处展示。用户查询报表时可按参数条件筛选数据。
        <br />提示：日期范围建议使用两个独立日期参数，如 <n-text code>WHERE date >= ${start_date} AND date &lt;= ${end_date}</n-text>
      </n-alert>
      <n-data-table
        :columns="paramColumns"
        :data="form.params"
        :bordered="true"
        size="small"
        striped
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { h, ref } from 'vue'
import { NButton, NInput, NSelect, NSwitch, type FormInst } from 'naive-ui'
import QueryDesigner from '@/components/QueryDesigner/QueryDesigner.vue'
import SqlEditor from '@/components/SqlEditor.vue'
import {
  InformationCircleOutline,
  DocumentTextOutline,
  CodeOutline,
  CheckmarkOutline,
  PlayOutline,
  FlashOutline,
  SettingsOutline
} from '@vicons/ionicons5'
import type { ReportDefinitionForm, ReportParam } from '@/types/reportDefinition'

const props = defineProps<{
  form: ReportDefinitionForm
  queryMode: 'sql' | 'visual'
  sqlValidated: boolean
  testingSql: boolean
  fieldLoading: boolean
  dataSourceOptions: Array<{ label: string; value: number }>
  dataSourceLoading: boolean
  dictTypeOptions: Array<{ label: string; value: string }>
}>()

defineEmits<{
  'update:queryMode': [value: 'sql' | 'visual']
  dataSourceChange: []
  testSql: []
  autoGetFields: []
  visualSqlUpdate: [sql: string]
}>()

const formRef = ref<FormInst | null>(null)

const handleExcelSwitch = (val: boolean) => { props.form.allowExportExcel = val ? 1 : 0 }
const handlePdfSwitch = (val: boolean) => { props.form.allowExportPdf = val ? 1 : 0 }
const handlePrintSwitch = (val: boolean) => { props.form.allowPrint = val ? 1 : 0 }

// Expose formRef for parent validation
defineExpose({
  getFormRef: () => formRef.value
})

const rules = {
  reportName: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  dataSourceId: [{ required: true, type: 'number' as const, message: '请选择数据源', trigger: 'change' }],
  sqlContent: [{ required: true, message: '请输入SQL查询语句', trigger: 'blur' }]
}

const watermarkTypeOptions = [
  { label: '无水印', value: 'none' },
  { label: '用户名_IP地址', value: 'user_ip' },
  { label: '自定义文本', value: 'custom' }
]

const inputTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '日期范围', value: 'daterange' },
  { label: '下拉选择', value: 'select' },
  { label: '部门', value: 'department' }
]

const paramColumns = [
  {
    title: '参数名',
    key: 'name',
    width: 140,
    ellipsis: { tooltip: true }
  },
  {
    title: '显示标签',
    key: 'label',
    width: 150,
    render: (row: ReportParam) => {
      return h(NInput, {
        value: row.label,
        placeholder: '请输入标签',
        onUpdateValue: (val: string) => { row.label = val }
      })
    }
  },
  {
    title: '输入类型',
    key: 'inputType',
    width: 130,
    render: (row: ReportParam) => {
      return h(NSelect, {
        value: row.inputType,
        options: inputTypeOptions,
        onUpdateValue: (val: string) => { (row as any).inputType = val }
      })
    }
  },
  {
    title: '必填',
    key: 'required',
    width: 70,
    render: (row: ReportParam) => {
      return h(NSwitch, {
        value: row.required,
        onUpdateValue: (val: boolean) => { row.required = val }
      })
    }
  },
  {
    title: '默认值',
    key: 'defaultValue',
    width: 150,
    render: (row: ReportParam) => {
      return h(NInput, {
        value: row.defaultValue || '',
        placeholder: '默认值',
        onUpdateValue: (val: string) => { row.defaultValue = val }
      })
    }
  },
  {
    title: '字典类型',
    key: 'dictType',
    width: 140,
    render: (row: ReportParam) => {
      if (row.inputType !== 'select') return h('span', { style: 'color:#ccc' }, '—')
      return h(NSelect, {
        value: row.dictType || null,
        options: props.dictTypeOptions,
        placeholder: '选择字典',
        clearable: true,
        filterable: true,
        onUpdateValue: (val: string | null) => { row.dictType = val || undefined }
      })
    }
  }
]
</script>

<style scoped>
.designer-properties {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.designer-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.designer-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header-custom {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.sql-editor-wrapper {
  position: relative;
}

.sql-tips {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}


</style>

<style>
/* ReportDesignerProperties 深色模式（非 scoped） */
html.dark .prop-label { color: #94a3b8 !important; }
</style>
