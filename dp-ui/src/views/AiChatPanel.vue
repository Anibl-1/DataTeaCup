<template>
  <n-card title="📝 需求配置" size="small" :segmented="{ content: true }">
    <!-- 数据源选择 -->
    <n-form-item label="数据源" required>
      <n-select
        :value="localFormData.dataSourceId"
        :options="dataSourceOptions"
        placeholder="请选择数据源"
        filterable
        :loading="loadingDataSources"
        @update:value="(v: number) => { emit('updateField', 'dataSourceId', v); emit('dataSourceChange', v) }"
      />
    </n-form-item>

    <!-- 表选择 -->
    <n-form-item label="选择数据表">
      <n-select
        :value="localFormData.selectedTables"
        :options="tableOptions"
        placeholder="可选择相关表（AI会自动分析）"
        multiple
        filterable
        :loading="loadingTables"
        :max-tag-count="3"
        @update:value="(v: string[]) => emit('updateField', 'selectedTables', v)"
      />
    </n-form-item>

    <!-- 需求描述 -->
    <n-form-item label="图表需求" required>
      <n-input
        :value="localFormData.requirement"
        type="textarea"
        placeholder="请用自然语言描述您想要的图表，例如：&#10;• 展示各部门的销售额对比&#10;• 按月份显示订单趋势&#10;• 分析产品类别占比"
        :rows="5"
        :maxlength="500"
        show-count
        @update:value="(v: string) => emit('updateField', 'requirement', v)"
      />
    </n-form-item>

    <!-- 图表偏好 -->
    <n-collapse>
      <n-collapse-item title="高级设置" name="advanced">
        <n-form-item label="偏好图表类型">
          <n-select
            :value="localFormData.preferredChartType"
            :options="chartTypeOptions"
            placeholder="AI自动推荐"
            clearable
            @update:value="(v: string | null) => emit('updateField', 'preferredChartType', v)"
          />
        </n-form-item>
        <n-form-item label="配色主题">
          <n-radio-group :value="localFormData.colorTheme" @update:value="(v: string) => emit('updateField', 'colorTheme', v)">
            <n-space>
              <n-radio value="professional">商务蓝</n-radio>
              <n-radio value="vibrant">活力彩</n-radio>
              <n-radio value="dark">深色系</n-radio>
              <n-radio value="pastel">柔和色</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
      </n-collapse-item>
    </n-collapse>

    <!-- 快捷模板 -->
    <n-divider>快捷模板</n-divider>
    <n-space vertical>
      <n-button 
        v-for="tpl in quickTemplates" 
        :key="tpl.type"
        block
        secondary
        :type="localFormData.requirement.includes(tpl.keyword) ? 'primary' : 'default'"
        @click="applyTemplate(tpl)"
      >
        {{ tpl.icon }} {{ tpl.label }}
      </n-button>
    </n-space>

    <!-- 生成按钮 -->
    <n-divider />
    
    <!-- 修改模式提示 -->
    <n-alert v-if="hasGeneratedConfig" type="info" style="margin-bottom: 12px;" :bordered="false">
      <template #icon><n-icon><ChatbubblesOutline /></n-icon></template>
      <span style="font-size: 12px;">连续对话模式：输入修改要求，AI会基于当前图表进行调整</span>
    </n-alert>
    
    <n-space vertical :size="8">
      <n-button
        type="primary"
        block
        size="large"
        :loading="generating"
        :disabled="!canGenerate"
        @click="$emit('generate')"
      >
        <template #icon>
          <n-icon><SparklesOutline /></n-icon>
        </template>
        {{ generating ? 'AI 分析中...' : (hasGeneratedConfig ? '🔄 修改图表' : '✨ AI 生成图表') }}
      </n-button>
      
      <!-- 新建对话按钮 -->
      <n-button
        v-if="hasGeneratedConfig"
        block
        quaternary
        @click="$emit('newConversation')"
      >
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        新建对话（重新生成）
      </n-button>
    </n-space>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { SparklesOutline, ChatbubblesOutline, AddOutline } from '@vicons/ionicons5'

export interface AiChatFormData {
  dataSourceId: number | null
  selectedTables: string[]
  requirement: string
  preferredChartType: string | null
  colorTheme: string
}

interface QuickTemplate {
  type: string
  icon: string
  label: string
  keyword: string
  template: string
}

const props = defineProps<{
  formData: AiChatFormData
  dataSourceOptions: { label: string; value: number }[]
  tableOptions: { label: string; value: string }[]
  chartTypeOptions: { label: string; value: string }[]
  quickTemplates: QuickTemplate[]
  loadingDataSources: boolean
  loadingTables: boolean
  generating: boolean
  hasGeneratedConfig: boolean
}>()

const emit = defineEmits<{
  (e: 'generate'): void
  (e: 'newConversation'): void
  (e: 'dataSourceChange', value: number): void
  (e: 'updateField', field: string, value: unknown): void
}>()

// Use the parent's reactive formData directly for v-model bindings
const localFormData = computed(() => props.formData)

const canGenerate = computed(() => {
  return localFormData.value.dataSourceId && localFormData.value.requirement.trim().length > 5
})

const applyTemplate = (tpl: QuickTemplate) => {
  emit('updateField', 'requirement', tpl.template)
}
</script>
