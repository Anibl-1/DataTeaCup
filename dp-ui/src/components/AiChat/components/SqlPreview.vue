<template>
  <div class="sql-preview">
    <div class="preview-header">
      <n-space align="center" :size="12">
        <n-button 
          type="primary" 
          size="small" 
          :loading="loading"
          :disabled="!sql || !dataSourceId"
          @click="executePreview"
        >
          <template #icon>
            <n-icon :component="PlayOutline" />
          </template>
          预览数据
        </n-button>
        <n-text v-if="previewData.length > 0" depth="3" style="font-size: 12px;">
          共 {{ totalCount }} 条，显示前 {{ previewData.length }} 条
        </n-text>
      </n-space>
    </div>
    
    <div v-if="previewData.length > 0 || loading || error" class="preview-content">
      <n-spin :show="loading">
        <n-alert v-if="error" type="error" :title="error" style="margin-bottom: 12px;" closable @close="error = ''" />
        
        <n-data-table
          v-if="previewData.length > 0"
          :columns="previewColumns"
          :data="previewData"
          :max-height="280"
          :scroll-x="previewColumns.length * 150"
          size="small"
          striped
          :bordered="false"
        />
        
        <!-- 加载中占位 -->
        <div v-else-if="loading" class="loading-placeholder">
          <n-text depth="3">正在加载数据...</n-text>
        </div>
        
        <n-empty v-else-if="!error" description="暂无数据" size="small" />
      </n-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { PlayOutline } from '@vicons/ionicons5'
import request from '@/api/request'

const props = defineProps<{
  sql: string
  dataSourceId: number | null
  params?: Record<string, any>
  limit?: number
}>()

const emit = defineEmits<{
  (e: 'preview-success', data: { columns: string[], data: any[], total: number }): void
  (e: 'preview-error', error: string): void
}>()

const message = useMessage()
const loading = ref(false)
const error = ref('')
const previewData = ref<any[]>([])
const previewColumns = ref<any[]>([])
const totalCount = ref(0)

const executePreview = async () => {
  if (!props.sql || !props.dataSourceId) {
    message.warning('请先填写SQL语句并选择数据源')
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 构建预览SQL（添加LIMIT）
    let previewSql = props.sql.trim()
    if (previewSql.endsWith(';')) {
      previewSql = previewSql.slice(0, -1)
    }
    
    // 替换参数为默认值进行预览（跳过空值）
    if (props.params) {
      for (const [key, value] of Object.entries(props.params)) {
        // 🔧 跳过空值参数，避免DATE类型字段报错
        if (value === null || value === undefined || value === '') {
          continue
        }
        const placeholder = new RegExp(`\\$\\{${key}\\}`, 'g')
        previewSql = previewSql.replace(placeholder, `'${value}'`)
      }
    }
    
    // 🔧 移除未替换的参数条件（AI生成的可选参数模式）
    // 模式: (${param} IS NULL OR ${param} = '' OR field = ${param})
    previewSql = previewSql.replace(/\s*\(\s*\$\{[^}]+\}\s+IS\s+NULL\s+OR\s+\$\{[^}]+\}\s*=\s*''\s+OR\s+[`\w\u4e00-\u9fa5.]+\s*=\s*\$\{[^}]+\}\s*\)/gi, '')
    // 带AND前缀
    previewSql = previewSql.replace(/\s+AND\s*\(\s*\$\{[^}]+\}\s+IS\s+NULL\s+OR\s+\$\{[^}]+\}\s*=\s*''\s+OR\s+[`\w\u4e00-\u9fa5.]+\s*=\s*\$\{[^}]+\}\s*\)/gi, '')
    // 简化版: AND (${param} IS NULL OR field = ${param})
    previewSql = previewSql.replace(/\s+AND\s*\(\s*\$\{[^}]+\}\s+IS\s+NULL\s+OR\s+[`\w\u4e00-\u9fa5.]+\s*=\s*'?\$\{[^}]+\}'?\s*\)/gi, '')
    // 简单条件: AND field = ${param}
    previewSql = previewSql.replace(/\s+AND\s+[`\w\u4e00-\u9fa5.]+`?\s*[=<>!]+\s*'?\$\{[^}]+\}'?/gi, '')
    // 清理空WHERE
    previewSql = previewSql.replace(/WHERE\s+ORDER/gi, 'ORDER')
    previewSql = previewSql.replace(/WHERE\s+LIMIT/gi, 'LIMIT')
    previewSql = previewSql.replace(/WHERE\s+GROUP/gi, 'GROUP')
    previewSql = previewSql.replace(/WHERE\s*$/gi, '')
    
    // 检查是否已有LIMIT
    if (!/\bLIMIT\s+\d+/i.test(previewSql)) {
      previewSql += ` LIMIT ${props.limit || 10}`
    }
    
    const res: any = await request.post('/ai/execute-sql', {
      dataSourceId: props.dataSourceId,
      sql: previewSql,
      limit: props.limit || 10
    }, { timeout: 30000 }) // 30秒超时
    
    // 处理API响应格式: { success, data, columns, rowCount }
    const responseData = res.data || res
    
    if (responseData.success === false) {
      throw new Error(responseData.error || '查询失败')
    }
    
    const dataRows = responseData.data || []
    const columnNames = responseData.columns || (dataRows.length > 0 ? Object.keys(dataRows[0]) : [])
    
    previewData.value = dataRows
    totalCount.value = responseData.rowCount || dataRows.length
    
    // 动态生成列配置
    previewColumns.value = columnNames.map((col: string) => ({
      title: col,
      key: col,
      ellipsis: { tooltip: true },
      width: 120
    }))
    
    emit('preview-success', {
      columns: columnNames,
      data: dataRows,
      total: totalCount.value
    })
  } catch (e: any) {
    error.value = e.message || e.msg || '执行SQL失败'
    emit('preview-error', error.value)
  } finally {
    loading.value = false
  }
}

// 暴露方法给父组件
defineExpose({
  executePreview,
  previewData,
  previewColumns
})
</script>

<style scoped>
.sql-preview {
  margin-top: 12px;
}

.preview-header {
  margin-bottom: 8px;
}

.preview-content {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
  max-height: 320px;
  overflow: auto;
}

.loading-placeholder {
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
