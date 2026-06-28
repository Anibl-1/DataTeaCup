<template>
  <div class="ai-assistant">
    <!-- 头部状态栏 - 紧凑单行版 -->
    <div class="ai-header-compact">
      <div class="ai-header-left">
        <div class="ai-logo-sm">
          <n-icon size="18"><SparklesOutline /></n-icon>
        </div>
        <span class="ai-title-sm">{{ t('aiAssist.title') }}</span>
        <div :class="['ai-status-dot-sm', aiStatus.enabled ? 'active' : 'inactive']">
          <span class="dot"></span>
          <span class="label">{{ aiStatus.enabled ? t('aiAssist.running') : t('aiAssist.disabled') }}</span>
        </div>
      </div>
      <div class="ai-header-center">
        <div class="ai-meta-item">
          <n-icon size="13"><ServerOutline /></n-icon>
          <span>{{ providerNames[aiStatus.provider] || t('aiAssist.notConfigured') }}</span>
        </div>
        <div class="ai-meta-sep">·</div>
        <div class="ai-meta-item">
          <n-icon size="13"><HardwareChipOutline /></n-icon>
          <span>{{ aiStatus.model || t('aiAssist.defaultModel') }}</span>
        </div>
        <div class="ai-meta-sep">·</div>
        <div class="ai-meta-item" :class="{ 'text-warning': !aiStatus.apiKeyConfigured }">
          <n-icon size="13"><KeyOutline /></n-icon>
          <span>{{ aiStatus.apiKeyConfigured ? 'Key ✓' : 'Key ✗' }}</span>
        </div>
        <div class="ai-meta-sep">·</div>
        <div class="ai-meta-item" :class="{ 'text-warning': aiUsageStats.dailyLimit > 0 && aiUsageStats.todayCalls / aiUsageStats.dailyLimit > 0.8 }">
          <n-icon size="13"><StatsChartOutline /></n-icon>
          <span>{{ aiUsageStats.todayCalls }}{{ aiUsageStats.dailyLimit > 0 ? '/' + aiUsageStats.dailyLimit : '' }} {{ t('aiAssist.times') }}</span>
        </div>
      </div>
      <div class="ai-header-right">
        <n-button size="small" quaternary circle @click="refreshStatus">
          <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
        </n-button>
        <n-button size="small" type="primary" @click="aiConfigStore.openConfig()">
          <template #icon><n-icon size="14"><SettingsOutline /></n-icon></template>
          {{ t('aiAssist.configure') }}
        </n-button>
      </div>
    </div>
    
    <!-- 功能选项卡 - 美化版 -->
    <n-tabs v-model:value="activeTab" type="line" class="ai-tabs" animated>
      <!-- SQL生成 -->
      <n-tab-pane name="sql">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><CodeOutline /></n-icon>
            <span>{{ t('aiAssist.sqlGen') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon sql">
                <n-icon size="20"><CodeOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.sqlGen') }}</h4>
                <p>{{ t('aiAssist.sqlGenDesc') }}</p>
              </div>
            </div>
            
            <div class="feature-body">
              <n-form-item :label="t('aiAssist.selectDataSource')" :show-feedback="false" style="margin-bottom: 12px;">
                <div style="display: flex; gap: 8px; align-items: center; width: 100%;">
                  <n-select
                    v-model:value="sqlForm.dataSourceId"
                    :options="dataSourceOptions"
                    :placeholder="t('aiAssist.selectDsAutoSchema')"
                    filterable
                    clearable
                    :loading="loadingDataSources"
                    style="flex: 1;"
                    @update:value="onSqlDataSourceChange"
                  />
                  <n-button
                    size="small"
                    :loading="loadingSqlSchema"
                    :disabled="!sqlForm.dataSourceId"
                    @click="fetchTableSchemaForSql"
                  >
                    <template #icon><n-icon><RefreshOutline /></n-icon></template>
                    {{ t('aiAssist.loadSchema') }}
                  </n-button>
                </div>
              </n-form-item>

              <n-input
                v-model:value="sqlForm.query"
                type="textarea"
                :placeholder="t('aiAssist.sqlQueryPlaceholder')"
                :rows="3"
                class="input-area"
              />
              
              <n-collapse :default-expanded-names="sqlForm.tableSchema ? ['schema'] : []">
                <n-collapse-item name="schema">
                  <template #header>
                    <span>{{ t('aiAssist.schemaInfo') }}</span>
                    <n-tag v-if="sqlForm.tableSchema" size="tiny" type="success" :bordered="false" style="margin-left: 8px;">{{ t('aiAssist.loaded') }}</n-tag>
                  </template>
                  <n-input
                    v-model:value="sqlForm.tableSchema"
                    type="textarea"
                    :placeholder="t('aiAssist.schemaPlaceholder')"
                    :rows="6"
                  />
                </n-collapse-item>
              </n-collapse>
              
              <div class="action-bar">
                <n-button type="primary" :loading="sqlLoading" @click="generateSql">
                  <template #icon><n-icon><SparklesOutline /></n-icon></template>
                  {{ t('aiAssist.generateSql') }}
                </n-button>
              </div>
            </div>
            
            <div v-if="sqlResult" class="result-area">
              <div class="result-header">
                <span>{{ t('aiAssist.result') }}</span>
                <n-space :size="8">
                  <n-button text size="small" @click="copySql">
                    <template #icon><n-icon><CopyOutline /></n-icon></template>
                    {{ t('common.copy') }}
                  </n-button>
                  <n-button text size="small" type="primary" @click="sendResultToChat('请帮我优化或解释这段SQL:\n```sql\n' + sqlResult + '\n```', 'sql')">
                    <template #icon><n-icon><ChatbubblesOutline /></n-icon></template>
                    {{ t('aiAssist.discussInChat') }}
                  </n-button>
                </n-space>
              </div>
              <n-code :code="sqlResult" language="sql" word-wrap />
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- 数据分析 -->
      <n-tab-pane name="analyze">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><AnalyticsOutline /></n-icon>
            <span>{{ t('aiAssist.dataAnalysis') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon analyze">
                <n-icon size="20"><AnalyticsOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.smartAnalysis') }}</h4>
                <p>{{ t('aiAssist.analysisDesc') }}</p>
              </div>
            </div>
            <div class="feature-body">
              <n-input
                v-model:value="analyzeForm.dataContext"
                type="textarea"
                :placeholder="t('aiAssist.dataContextPlaceholder')"
                :rows="3"
                class="input-area"
              />
              
              <n-input
                v-model:value="analyzeForm.question"
                type="textarea"
                :placeholder="t('aiAssist.questionPlaceholder')"
                :rows="2"
                class="input-area"
              />
              
              <div class="action-bar">
                <n-button type="primary" :loading="analyzeLoading" @click="analyzeData">
                  <template #icon><n-icon><AnalyticsOutline /></n-icon></template>
                  {{ t('aiAssist.startAnalysis') }}
                </n-button>
              </div>
            </div>
            
            <div v-if="analyzeResult" class="result-area">
              <div class="result-header">
                <span>{{ t('aiAssist.analysisResult') }}</span>
                <n-button text size="small" type="primary" @click="sendResultToChat('请基于以下分析结果进一步讨论:\n' + analyzeResult, 'analysis')">
                  <template #icon><n-icon><ChatbubblesOutline /></n-icon></template>
                  {{ t('aiAssist.discussInChat') }}
                </n-button>
              </div>
              <div class="markdown-content" v-html="renderMarkdown(analyzeResult)"></div>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- SQL优化 -->
      <n-tab-pane name="optimize">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><FlashOutline /></n-icon>
            <span>{{ t('aiAssist.sqlOptimize') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon optimize">
                <n-icon size="20"><FlashOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.sqlPerfOptimize') }}</h4>
                <p>{{ t('aiAssist.optimizeDesc') }}</p>
              </div>
            </div>
            <div class="feature-body">
              <n-input
                v-model:value="optimizeForm.sql"
                type="textarea"
                :placeholder="t('aiAssist.enterSqlToOptimize')"
                :rows="5"
                class="input-area"
              />
              
              <n-select
                v-model:value="optimizeForm.dbType"
                :options="dbTypeOptions"
                :placeholder="t('aiAssist.selectDbType')"
                style="width: 200px; margin-bottom: 16px;"
              />
              
              <div class="action-bar">
                <n-button type="primary" :loading="optimizeLoading" @click="optimizeSql">
                  <template #icon><n-icon><FlashOutline /></n-icon></template>
                  {{ t('aiAssist.optimizeSql') }}
                </n-button>
              </div>
            </div>
            
            <div v-if="optimizeResult" class="result-area">
              <div class="result-header">
                <span>{{ t('aiAssist.optimizeSuggestion') }}</span>
                <n-button text size="small" type="primary" @click="sendResultToChat('请基于以下优化建议继续讨论:\n' + optimizeResult, 'optimize')">
                  <template #icon><n-icon><ChatbubblesOutline /></n-icon></template>
                  {{ t('aiAssist.discussInChat') }}
                </n-button>
              </div>
              <div class="markdown-content" v-html="renderMarkdown(optimizeResult)"></div>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- 智能图表 -->
      <n-tab-pane name="chart">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><BarChartOutline /></n-icon>
            <span>{{ t('aiAssist.smartChart') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon chart">
                <n-icon size="20"><BarChartOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.smartChartGen') }}</h4>
                <p>{{ t('aiAssist.chartGenDesc') }}</p>
              </div>
            </div>
            <div class="feature-body">
              <n-form-item :label="t('aiAssist.selectDataSource')" :show-feedback="false">
                <n-select
                  v-model:value="chartForm.dataSourceId"
                  :options="dataSourceOptions"
                  :placeholder="t('aiAssist.pleaseSelectDs')"
                  filterable
                  :loading="loadingDataSources"
                  style="width: 300px;"
                />
              </n-form-item>
              
              <n-input
                v-model:value="chartForm.requirement"
                type="textarea"
                :placeholder="t('aiAssist.chartRequirementPlaceholder')"
                :rows="4"
                class="input-area"
              />
              
              <n-grid :cols="2" :x-gap="16">
                <n-gi>
                  <n-form-item :label="t('aiAssist.preferChartType')" :show-feedback="false">
                    <n-select
                      v-model:value="chartForm.chartType"
                      :options="chartTypeOptions"
                      :placeholder="t('aiAssist.aiAutoRecommend')"
                      clearable
                    />
                  </n-form-item>
                </n-gi>
                <n-gi>
                  <n-form-item :label="t('aiAssist.colorTheme')" :show-feedback="false">
                    <n-select
                      v-model:value="chartForm.colorTheme"
                      :options="colorThemeOptions"
                      :placeholder="t('aiAssist.defaultColor')"
                      clearable
                    />
                  </n-form-item>
                </n-gi>
              </n-grid>
              
              <div class="action-bar">
                <n-button type="primary" :loading="chartLoading" :disabled="!canGenerateChart" @click="generateChart">
                  <template #icon><n-icon><SparklesOutline /></n-icon></template>
                  {{ t('aiAssist.aiGenerateChart') }}
                </n-button>
                <n-button v-if="chartResult" @click="openChartDesigner">
                  <template #icon><n-icon><CreateOutline /></n-icon></template>
                  {{ t('aiAssist.editInDesigner') }}
                </n-button>
              </div>
            </div>
            
            <div v-if="chartResult" class="result-area">
              <div class="result-header">
                <span>{{ t('aiAssist.chartPreview') }}</span>
                <n-space>
                  <n-tag type="success" size="small">{{ chartResult.chartType }}</n-tag>
                  <n-button text size="small" @click="saveGeneratedChart">
                    <template #icon><n-icon><SaveOutline /></n-icon></template>
                    {{ t('aiAssist.saveChart') }}
                  </n-button>
                </n-space>
              </div>
              <div class="chart-preview-box">
                <div ref="chartPreviewRef" class="chart-preview"></div>
              </div>
              <n-collapse style="margin-top: 12px;">
                <n-collapse-item :title="t('aiAssist.viewGeneratedSql')" name="sql">
                  <n-code :code="chartResult.sql || ''" language="sql" word-wrap />
                </n-collapse-item>
                <n-collapse-item :title="t('aiAssist.aiAnalysisNote')" name="analysis">
                  <div class="markdown-content" v-html="renderMarkdown(chartResult.aiResponse || '')"></div>
                </n-collapse-item>
              </n-collapse>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- 文件识别 -->
      <n-tab-pane name="file">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><CloudUploadOutline /></n-icon>
            <span>{{ t('aiAssist.fileRecognition') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon file">
                <n-icon size="20"><CloudUploadOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.smartFileRecognition') }}</h4>
                <p>{{ t('aiAssist.fileRecognitionDesc') }}</p>
              </div>
            </div>
            <div class="feature-body">
              <n-spin :show="fileLoading" :description="t('aiAssist.analyzingFile')">
                <n-upload
                  :custom-request="handleFileUpload"
                  :max="1"
                  accept=".xlsx,.xls,.csv"
                  :show-file-list="true"
                  :disabled="fileLoading"
                  @change="handleFileChange"
                >
                  <n-upload-dragger>
                    <div style="margin-bottom: 12px;">
                      <n-icon size="48" :depth="3"><CloudUploadOutline /></n-icon>
                    </div>
                    <n-text style="font-size: 16px;">{{ t('aiAssist.uploadDragText') }}</n-text>
                    <n-p depth="3" style="margin: 8px 0 0 0;">
                      {{ t('aiAssist.uploadFormatHint') }}
                    </n-p>
                  </n-upload-dragger>
                </n-upload>
              </n-spin>
            </div>
            
            <div v-if="fileAnalysisResult" class="result-area" style="margin-top: 16px;">
              <div class="result-header">
                <span>{{ t('aiAssist.recognitionResult') }}</span>
                <n-space size="small">
                  <n-tag :type="fileAnalysisResult.success ? 'success' : 'error'" size="small">
                    {{ fileAnalysisResult.success ? t('aiAssist.recognitionSuccess') : t('aiAssist.recognitionFailed') }}
                  </n-tag>
                  <n-tag v-if="fileAnalysisResult.totalRows" type="info" size="small">
                    {{ fileAnalysisResult.totalRows }} {{ t('aiAssist.rows') }}
                  </n-tag>
                  <n-tag v-if="fileAnalysisResult.columns?.length" type="default" size="small">
                    {{ fileAnalysisResult.columns.length }} {{ t('aiAssist.columns') }}
                  </n-tag>
                </n-space>
              </div>
              
              <div v-if="fileAnalysisResult.dataPreview?.length" class="data-preview">
                <n-data-table
                  :columns="fileAnalysisResult.columns"
                  :data="fileAnalysisResult.dataPreview"
                  :max-height="300"
                  size="small"
                  bordered
                />
              </div>
              
              <div v-if="fileAnalysisResult.analysis" class="markdown-content" style="margin-top: 12px;">
                <div v-html="renderMarkdown(fileAnalysisResult.analysis)"></div>
              </div>
              
              <n-space style="margin-top: 16px;">
                <n-button type="primary" :disabled="!fileAnalysisResult.dataPreview" @click="createChartFromFile">
                  <template #icon><n-icon><BarChartOutline /></n-icon></template>
                  {{ t('aiAssist.generateChartFromData') }}
                </n-button>
                <n-button :disabled="!fileAnalysisResult.dataPreview" @click="exportAnalyzedData">
                  <template #icon><n-icon><DownloadOutline /></n-icon></template>
                  {{ t('aiAssist.exportData') }}
                </n-button>
              </n-space>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- AI对话（增强版） -->
      <n-tab-pane name="chat">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><ChatbubblesOutline /></n-icon>
            <span>{{ t('aiAssist.aiChat') }}</span>
          </div>
        </template>
        <div class="chat-container enhanced-chat">
          <!-- 顶部工具栏 -->
          <div class="chat-toolbar">
            <div class="toolbar-left">
              <n-select
                v-model:value="chatDataSourceId"
                :options="dataSourceOptions"
                :placeholder="t('aiAssist.selectDsOptional')"
                size="small"
                clearable
                filterable
                style="width: 220px;"
              />
              <n-button size="small" type="primary" ghost @click="openInChatDrawer">
                <template #icon><n-icon><ChatbubblesOutline /></n-icon></template>
                {{ t('aiAssist.openInSidebar') }}
              </n-button>
            </div>
            <div class="toolbar-right">
              <n-button v-if="chatMessages.length > 0" text size="small" @click="clearChat">
                <template #icon><n-icon><TrashOutline /></n-icon></template>
                {{ t('aiAssist.clear') }}
              </n-button>
            </div>
          </div>

          <!-- 快捷指令区 -->
          <div v-if="chatMessages.length === 0" class="quick-actions enhanced">
            <div class="quick-action-title">{{ t('aiAssist.quickActions') }}</div>
            <div class="quick-action-grid">
              <div class="quick-action-item" @click="useQuickPrompt('Write a SQL query for user orders')">
                <n-icon size="20"><CodeOutline /></n-icon>
                <span>{{ t('aiAssist.writeSQL') }}</span>
              </div>
              <div class="quick-action-item" @click="useQuickPrompt('Analyze data characteristics and trends')">
                <n-icon size="20"><AnalyticsOutline /></n-icon>
                <span>{{ t('aiAssist.dataAnalysis') }}</span>
              </div>
              <div class="quick-action-item" @click="useQuickPrompt('Optimize this SQL query for performance')">
                <n-icon size="20"><FlashOutline /></n-icon>
                <span>{{ t('aiAssist.optimizeSql') }}</span>
              </div>
              <div class="quick-action-item" @click="useQuickPrompt('Design a user management table schema')">
                <n-icon size="20"><LayersOutline /></n-icon>
                <span>{{ t('aiAssist.designSchema') }}</span>
              </div>
              <div class="quick-action-item" @click="useQuickPrompt('Create a data sync ETL task')">
                <n-icon size="20"><RefreshOutline /></n-icon>
                <span>{{ t('aiAssist.createETL') }}</span>
              </div>
              <div class="quick-action-item" @click="useQuickPrompt('Analyze data quality issues and suggest improvements')">
                <n-icon size="20"><BarChartOutline /></n-icon>
                <span>{{ t('aiAssist.qualityAnalysis') }}</span>
              </div>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div ref="chatMessagesRef" class="chat-messages" @click="handleCodeBlockClick">
            <div v-if="chatMessages.length === 0" class="chat-empty">
              <n-icon size="48" color="#ccc"><ChatbubblesOutline /></n-icon>
              <p>{{ t('aiAssist.startChatWithAI') }}</p>
              <p class="chat-tips">{{ t('aiAssist.chatSupportTopics') }}</p>
            </div>
            <div
              v-for="(msg, index) in visibleMessages"
              :key="index"
              :class="['chat-message', msg.role]"
            >
              <div class="message-avatar">
                <n-icon size="20">
                  <PersonOutline v-if="msg.role === 'user'" />
                  <SparklesOutline v-else />
                </n-icon>
              </div>
              <div class="message-bubble-wrapper">
                <div class="message-content">
                  <div v-if="msg.role === 'assistant'" class="markdown-content" v-html="renderEnhancedMarkdown(msg.content)"></div>
                  <div v-else class="user-text">{{ msg.content }}</div>
                </div>
                <div v-if="msg.role === 'assistant'" class="message-actions">
                  <n-button text size="tiny" @click="copyMessageContent(msg.content)">
                    <template #icon><n-icon size="14"><CopyOutline /></n-icon></template>
                    {{ t('common.copy') }}
                  </n-button>
                  <n-button v-if="index === visibleMessages.length - 1" text size="tiny" @click="regenerateChat(chatMessages.length - 1)">
                    <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
                    {{ t('aiAssist.regenerate') }}
                  </n-button>
                </div>
              </div>
            </div>
            <div v-if="chatSending" class="chat-message assistant">
              <div class="message-avatar">
                <n-icon size="20"><SparklesOutline /></n-icon>
              </div>
              <div class="message-content thinking-bubble">
                <div class="thinking-dots"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
          
          <!-- 输入区域 -->
          <div class="chat-input-area">
            <div class="chat-input-card">
              <n-input
                v-model:value="chatInput"
                type="textarea"
                :placeholder="chatInputPlaceholder"
                :autosize="{ minRows: 2, maxRows: 6 }"
                :bordered="false"
                class="chat-input-inner"
                @keydown="handleChatKeydown"
              />
              <div class="chat-input-footer">
                <div class="footer-left">
                  <span v-if="chatDataSourceId" class="context-tag">
                    <n-icon size="12"><ServerOutline /></n-icon>
                    {{ getDataSourceLabel(chatDataSourceId) }}
                  </span>
                  <span class="char-count" :class="{ warning: chatInput.length > 1800 }">{{ chatInput.length }} / 2000</span>
                </div>
                <n-button
                  type="primary"
                  :loading="chatSending"
                  :disabled="!chatInput.trim()"
                  class="send-btn"
                  @click="sendChat"
                >
                  <template #icon><n-icon><SendOutline /></n-icon></template>
                </n-button>
              </div>
            </div>
            <div class="input-tips">{{ t('aiAssist.chatInputTips') }}</div>
          </div>
        </div>
      </n-tab-pane>

      <!-- 数据洞察 -->
      <n-tab-pane name="insight">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><BulbOutline /></n-icon>
            <span>{{ t('aiAssist.dataInsight') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon analyze">
                <n-icon size="20"><BulbOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.insightAnalysis') }}</h4>
                <p>{{ t('aiAssist.insightDesc') }}</p>
              </div>
            </div>

            <div class="feature-body">
              <n-form-item :label="t('aiAssist.selectDataSource')" :show-feedback="false">
                <n-select
                  v-model:value="insightForm.dataSourceId"
                  :options="dataSourceOptions"
                  :placeholder="t('aiAssist.pleaseSelectDs')"
                  filterable
                  :loading="loadingDataSources"
                  style="width: 300px;"
                />
              </n-form-item>

              <n-input
                v-model:value="insightForm.sql"
                type="textarea"
                :placeholder="t('aiAssist.insightSqlPlaceholder')"
                :autosize="{ minRows: 2, maxRows: 5 }"
                class="input-area"
              />

              <div class="action-bar">
                <n-button type="primary" :loading="insightLoading" :disabled="!insightForm.dataSourceId || !insightForm.sql" @click="runInsightAnalysis">
                  <template #icon><n-icon><SparklesOutline /></n-icon></template>
                  {{ t('aiAssist.startAnalysis') }}
                </n-button>
                <n-button :loading="anomalyLoading" :disabled="!insightForm.dataSourceId || !insightForm.sql" @click="runAnomalyDetection">
                  <template #icon><n-icon><WarningOutline /></n-icon></template>
                  {{ t('aiAssist.detectAnomaly') }}
                </n-button>
                <n-button :loading="trendLoading" :disabled="!insightForm.dataSourceId || !insightForm.sql" @click="runTrendAnalysis">
                  <template #icon><n-icon><TrendingUpOutline /></n-icon></template>
                  {{ t('aiAssist.trendAnalysis') }}
                </n-button>
              </div>
            </div>

            <!-- 统计摘要 -->
            <div v-if="insightSummary" class="result-area">
              <div class="result-header">
                <span>{{ t('aiAssist.statsSummary') }}</span>
                <n-tag type="info" size="small">{{ insightSummary.totalRows }} {{ t('aiAssist.rowsData') }}</n-tag>
              </div>
              <n-grid :cols="4" :x-gap="16" :y-gap="16">
                <n-gi v-for="field in insightSummary.fields" :key="field.field">
                  <n-statistic :label="field.field">
                    <template #default>
                      <span v-if="field.type === 'numeric'">{{ field.mean?.toLocaleString(undefined, { maximumFractionDigits: 2 }) }}</span>
                      <span v-else>{{ field.uniqueCount }} {{ t('aiAssist.types') }}</span>
                    </template>
                    <template #suffix>
                      <n-tag size="small" :type="field.type === 'numeric' ? 'success' : 'info'">
                        {{ field.type === 'numeric' ? t('aiAssist.numeric') : t('aiAssist.categorical') }}
                      </n-tag>
                    </template>
                  </n-statistic>
                </n-gi>
              </n-grid>
            </div>

            <!-- 异常检测结果 -->
            <div v-if="insightAnomalies.length > 0" class="result-area" style="margin-top: 12px;">
              <div class="result-header">
                <span>{{ t('aiAssist.anomalyDetection') }}</span>
                <n-tag type="warning" size="small">{{ insightAnomalies.length }} {{ t('aiAssist.anomalies') }}</n-tag>
              </div>
              <n-data-table
                :columns="insightAnomalyColumns"
                :data="insightAnomalies"
                :max-height="300"
                size="small"
              />
            </div>

            <!-- 趋势分析结果 -->
            <div v-if="insightTrends" class="result-area" style="margin-top: 12px;">
              <div class="result-header">
                <span>{{ t('aiAssist.trendAnalysis') }}</span>
                <n-tag :type="insightTrends.trend === 'increasing' ? 'success' : insightTrends.trend === 'decreasing' ? 'warning' : 'info'" size="small">{{ insightTrends.trend }}</n-tag>
              </div>
              <n-descriptions :column="2" label-placement="left">
                <n-descriptions-item :label="t('aiAssist.overallTrend')">{{ insightTrends.trend }}</n-descriptions-item>
                <n-descriptions-item :label="t('aiAssist.changeRate')">{{ (insightTrends.changeRate * 100).toFixed(2) }}%</n-descriptions-item>
                <n-descriptions-item :label="t('aiAssist.cyclicPattern')">{{ insightTrends.hasCyclicPattern ? t('common.yes') : t('common.no') }}</n-descriptions-item>
              </n-descriptions>
              <n-divider v-if="insightTrends.fieldTrends?.length" />
              <n-space v-if="insightTrends.fieldTrends?.length" vertical>
                <div v-for="ft in insightTrends.fieldTrends" :key="ft.field" style="display: flex; align-items: center; gap: 8px;">
                  <span>{{ ft.field }}</span>
                  <n-tag size="small" :type="ft.trend === 'increasing' ? 'success' : ft.trend === 'decreasing' ? 'warning' : 'info'">{{ ft.trend }}</n-tag>
                  <span class="form-hint-secondary">{{ (ft.changeRate * 100).toFixed(2) }}%</span>
                </div>
              </n-space>
            </div>

            <!-- AI 洞察文本 -->
            <div v-if="insightAiText" class="result-area" style="margin-top: 12px;">
              <div class="result-header">
                <span>{{ t('aiAssist.aiInsight') }}</span>
                <n-icon size="20" color="#18a058"><SparklesOutline /></n-icon>
              </div>
              <div class="markdown-content" v-html="renderMarkdown(insightAiText)"></div>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- Prompt模板管理 -->
      <n-tab-pane name="templates">
        <template #tab>
          <div class="tab-label">
            <n-icon size="16"><LayersOutline /></n-icon>
            <span>{{ t('aiAssist.promptTemplates') }}</span>
          </div>
        </template>
        <div class="tab-content">
          <div class="feature-card">
            <div class="feature-header">
              <div class="feature-icon sql">
                <n-icon size="20"><LayersOutline /></n-icon>
              </div>
              <div class="feature-info">
                <h4>{{ t('aiAssist.promptTemplateManage') }}</h4>
                <p>{{ t('aiAssist.promptTemplateDesc') }}</p>
              </div>
            </div>
            <div class="feature-body">
              <n-space style="margin-bottom: 12px">
                <n-button type="primary" size="small" @click="showTemplateEditModal = true; editingTemplate = null; templateForm = { name: '', content: '', category: 'sql' }">
                  <template #icon><n-icon><CreateOutline /></n-icon></template>
                  {{ t('aiAssist.newTemplate') }}
                </n-button>
                <n-button size="small" @click="loadPromptTemplates">
                  <template #icon><n-icon><RefreshOutline /></n-icon></template>
                  {{ t('common.refresh') }}
                </n-button>
              </n-space>
              <n-spin :show="loadingTemplates">
                <n-empty v-if="promptTemplates.length === 0 && !loadingTemplates" :description="t('aiAssist.noTemplates')" />
                <div v-else style="display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px;">
                  <n-card v-for="tpl in promptTemplates" :key="tpl.id" size="small" hoverable>
                    <template #header>
                      <div style="display: flex; align-items: center; gap: 8px">
                        <n-tag size="tiny" :type="tpl.category === 'sql' ? 'info' : tpl.category === 'analysis' ? 'success' : 'warning'">{{ tpl.category }}</n-tag>
                        <span style="font-size: 14px; font-weight: 600">{{ tpl.name }}</span>
                      </div>
                    </template>
                    <template #header-extra>
                      <n-space :size="4">
                        <n-button size="tiny" type="primary" text @click="applyPromptTemplate(tpl)">{{ t('aiAssist.use') }}</n-button>
                        <n-button size="tiny" text @click="editPromptTemplate(tpl)">{{ t('common.edit') }}</n-button>
                        <n-button size="tiny" type="error" text @click="deletePromptTemplate(tpl.id)">{{ t('common.delete') }}</n-button>
                      </n-space>
                    </template>
                    <n-text depth="3" style="font-size: 12px; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">{{ tpl.content }}</n-text>
                  </n-card>
                </div>
              </n-spin>
            </div>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>

    <!-- 模板编辑弹窗 -->
    <n-modal v-model:show="showTemplateEditModal" preset="card" :title="editingTemplate ? t('aiAssist.editTemplate') : t('aiAssist.newTemplate')" style="width: 550px">
      <n-form label-placement="left" label-width="80px">
        <n-form-item :label="t('aiAssist.templateName')">
          <n-input v-model:value="templateForm.name" :placeholder="t('aiAssist.enterTemplateName')" />
        </n-form-item>
        <n-form-item :label="t('aiAssist.category')">
          <n-select v-model:value="templateForm.category" :options="templateCategoryOptions" style="width: 200px" />
        </n-form-item>
        <n-form-item :label="t('aiAssist.templateContent')">
          <n-input v-model:value="templateForm.content" type="textarea" :rows="6" :placeholder="t('aiAssist.enterTemplateContent')" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTemplateEditModal = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="savingTemplate" @click="savePromptTemplate">{{ t('common.save') }}</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useMessage } from 'naive-ui'
import { useRouter } from 'vue-router'
import {
  SparklesOutline,
  RefreshOutline,
  CopyOutline,
  AnalyticsOutline,
  FlashOutline,
  ChatbubblesOutline,
  PersonOutline,
  SendOutline,
  TrashOutline,
  CodeOutline,
  LayersOutline,
  ServerOutline,
  HardwareChipOutline,
  KeyOutline,
  SettingsOutline,
  CreateOutline,
  SaveOutline,
  CloudUploadOutline,
  BarChartOutline,
  DownloadOutline,
  BulbOutline,
  WarningOutline,
  TrendingUpOutline
} from '@vicons/ionicons5'
import { marked } from 'marked'
import request from '@/api/request'
import { useI18n } from '@/i18n'
import { StatsChartOutline } from '@vicons/ionicons5'
import { DB_TYPE_OPTIONS } from '@/constants'
import { useAiConfigStore } from '@/stores/aiConfig'
import { getDataSourceList, getDataSourceTables, getTableColumns } from '@/api/dataSource'
import { aiGenerateChart, aiCreateChart, testChartSql } from '@/api/chart'
import { generateInsightReport, detectAnomalies, analyzeTrend } from '@/api/aiInsight'
import echarts from '@/utils/echarts'

const message = useMessage()
const { t } = useI18n()
const aiConfigStore = useAiConfigStore()

// ==================== Prompt模板管理 ====================
const loadingTemplates = ref(false)
const promptTemplates = ref<any[]>([])
const showTemplateEditModal = ref(false)
const savingTemplate = ref(false)
const editingTemplate = ref<any>(null)
const templateForm = ref({ name: '', content: '', category: 'sql' })
const templateCategoryOptions = computed(() => [
  { label: t('aiAssist.categorySql'), value: 'sql' },
  { label: t('aiAssist.categoryAnalysis'), value: 'analysis' },
  { label: t('aiAssist.categoryOptimize'), value: 'optimize' },
  { label: t('aiAssist.categoryGeneral'), value: 'general' }
])

const loadPromptTemplates = async () => {
  loadingTemplates.value = true
  try {
    const res = await request.get('/ai/prompt-templates')
    promptTemplates.value = res.data || []
  } catch (error) {
    console.error('加载模板失败', error)
  } finally {
    loadingTemplates.value = false
  }
}

const savePromptTemplate = async () => {
  if (!templateForm.value.name || !templateForm.value.content) {
    message.warning(t('aiAssist.fillNameAndContent'))
    return
  }
  savingTemplate.value = true
  try {
    if (editingTemplate.value) {
      await request.put(`/ai/prompt-templates/${editingTemplate.value.id}`, templateForm.value)
      message.success(t('aiAssist.templateUpdated'))
    } else {
      await request.post('/ai/prompt-templates', templateForm.value)
      message.success(t('aiAssist.templateCreated'))
    }
    showTemplateEditModal.value = false
    await loadPromptTemplates()
  } catch (error: any) {
    message.error(error.message || t('aiAssist.saveFailed'))
  } finally {
    savingTemplate.value = false
  }
}

const editPromptTemplate = (tpl: any) => {
  editingTemplate.value = tpl
  templateForm.value = { name: tpl.name, content: tpl.content, category: tpl.category || 'general' }
  showTemplateEditModal.value = true
}

const deletePromptTemplate = async (id: number) => {
  try {
    await request.delete(`/ai/prompt-templates/${id}`)
    message.success(t('aiAssist.templateDeleted'))
    await loadPromptTemplates()
  } catch (error: any) {
    message.error(error.message || t('aiAssist.deleteFailed'))
  }
}

const applyPromptTemplate = (tpl: any) => {
  chatInput.value = tpl.content
  activeTab.value = 'chat'
  message.success(t('aiAssist.templateApplied'))
}

// AI状态 - 使用共享 store
const aiStatus = computed(() => aiConfigStore.aiStatus)
const aiUsageStats = computed(() => aiConfigStore.usageStats)
const providerNames = aiConfigStore.providerNames

const sqlLoading = ref(false)
const analyzeLoading = ref(false)
const optimizeLoading = ref(false)
const chatSending = ref(false)
const activeTab = ref('sql')

// SQL生成
const sqlForm = ref({
  query: '',
  tableSchema: '',
  dataSourceId: null as number | null
})
const sqlResult = ref('')
const loadingSqlSchema = ref(false)

/** 数据源变更时自动加载表结构 */
const onSqlDataSourceChange = (dsId: number | null) => {
  if (dsId) {
    fetchTableSchemaForSql()
  }
}

/** 从数据源自动获取表结构信息 */
const fetchTableSchemaForSql = async () => {
  if (!sqlForm.value.dataSourceId) return
  loadingSqlSchema.value = true
  try {
    const tablesRes = await getDataSourceTables(sqlForm.value.dataSourceId)
    const tables = (tablesRes as any)?.data || (tablesRes as any) || []
    if (!Array.isArray(tables) || tables.length === 0) {
      message.warning(t('aiAssist.noTableInfo'))
      loadingSqlSchema.value = false
      return
    }
    // 前50张表加载字段结构，其余仅列出表名
    const structureLimit = 50
    const schemaLines: string[] = []
    for (let i = 0; i < tables.length; i++) {
      const table = tables[i]
      const tName = table.tableName || table.name || table
      if (i < structureLimit) {
        try {
          const colRes = await getTableColumns(sqlForm.value.dataSourceId!, String(tName))
          const cols = (colRes as any)?.data || (colRes as any) || []
          if (Array.isArray(cols) && cols.length > 0) {
            const colNames = cols.map((c: any) => {
              const name = c.columnName || c.name || c
              const type = c.dataType || c.type || ''
              return type ? `${name} ${type}` : name
            }).join(', ')
            schemaLines.push(`${tName}(${colNames})`)
          } else {
            schemaLines.push(String(tName))
          }
        } catch {
          schemaLines.push(String(tName))
        }
      } else {
        schemaLines.push(String(tName))
      }
    }
    const dbType = getDataSourceDbType(sqlForm.value.dataSourceId)
    sqlForm.value.tableSchema = [
      dbType ? `数据库类型: ${dbType}` : '',
      ...schemaLines
    ].filter(Boolean).join('\n')
    message.success(t('aiAssist.schemaLoaded', { count: tables.length }))
  } catch (error) {
    message.error(t('aiAssist.loadSchemaFailed'))
    logger.error('加载表结构失败:', error)
  } finally {
    loadingSqlSchema.value = false
  }
}

// 数据分析
const analyzeForm = ref({
  question: '',
  dataContext: ''
})
const analyzeResult = ref('')

// SQL优化
const optimizeForm = ref({
  sql: '',
  dbType: 'mysql'
})
const optimizeResult = ref('')

const dbTypeOptions = DB_TYPE_OPTIONS.map(item => ({ label: item.label, value: item.value }))

// 智能图表相关
const router = useRouter()
const chartForm = ref({
  dataSourceId: null as number | null,
  requirement: '',
  chartType: null as string | null,
  colorTheme: null as string | null
})
const chartLoading = ref(false)
const loadingDataSources = ref(false)
const dataSourceOptions = ref<{ label: string; value: number; dbType?: string }[]>([])
const chartResult = ref<{
  chartType: string
  sql: string
  chartName: string
  chartConfig: any
  aiResponse: string
  chartId?: number
} | null>(null)
const chartPreviewRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const chartTypeOptions = [
  { label: 'Line', value: 'line' },
  { label: 'Bar', value: 'bar' },
  { label: 'Pie', value: 'pie' },
  { label: 'Scatter', value: 'scatter' },
  { label: 'Gauge', value: 'gauge' }
]

const colorThemeOptions = [
  { label: 'Professional Blue', value: 'professional' },
  { label: 'Vibrant', value: 'vibrant' },
  { label: 'Dark', value: 'dark' },
  { label: 'Pastel', value: 'pastel' }
]

const canGenerateChart = computed(() => {
  return chartForm.value.dataSourceId && chartForm.value.requirement.trim().length > 5
})

const getDataSourceDbType = (dataSourceId?: number | null) => {
  if (!dataSourceId) return ''
  return dataSourceOptions.value.find(item => item.value === dataSourceId)?.dbType || ''
}

// 加载数据源
const loadDataSources = async () => {
  loadingDataSources.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    // 兼容多种返回格式
    const list = (res as any).list || (res as any).data?.list || res
    if (Array.isArray(list)) {
      dataSourceOptions.value = list.map((ds: any) => ({
        label: `${ds.name} (${ds.dbType})`,
        value: ds.id,
        dbType: ds.dbType
      }))
    }
  } catch (error) {
    logger.error('加载数据源失败:', error)
  } finally {
    loadingDataSources.value = false
  }
}

// ==================== 数据洞察 Tab ====================
const insightForm = ref({ dataSourceId: null as number | null, sql: '' })
const insightLoading = ref(false)
const insightSummary = ref<any>(null)
const insightAnomalies = ref<any[]>([])
const insightTrends = ref<any>(null)
const insightAiText = ref('')
const anomalyLoading = ref(false)
const trendLoading = ref(false)

const insightAnomalyColumns = [
  { title: 'Field', key: 'field', width: 120 },
  { title: 'Value', key: 'value', width: 100 },
  { title: 'Mean', key: 'mean', width: 100 },
  { title: 'Std Dev', key: 'stdDev', width: 100 },
  { title: 'Deviation', key: 'deviation', width: 100 },
  { title: 'Row', key: 'rowIndex', width: 80 }
]

async function runInsightAnalysis() {
  if (!insightForm.value.dataSourceId || !insightForm.value.sql) return
  insightLoading.value = true
  try {
    const res = await generateInsightReport(insightForm.value.dataSourceId, insightForm.value.sql)
    const data = (res as any).data
    insightSummary.value = data.summary || null
    insightAnomalies.value = data.anomalies || []
    insightTrends.value = data.trends || null
    insightAiText.value = data.aiInsight || ''
    message.success(t('aiAssist.analysisComplete'))
  } catch (error) {
    message.error(t('aiAssist.analysisFailed'))
  } finally {
    insightLoading.value = false
  }
}

async function runAnomalyDetection() {
  if (!insightForm.value.dataSourceId || !insightForm.value.sql) return
  anomalyLoading.value = true
  try {
    const res = await detectAnomalies(insightForm.value.dataSourceId, insightForm.value.sql)
    insightAnomalies.value = (res as any).data || []
    message.success(t('aiAssist.anomaliesDetected', { count: insightAnomalies.value.length }))
  } catch (error) {
    message.error(t('aiAssist.anomalyDetectionFailed'))
  } finally {
    anomalyLoading.value = false
  }
}

async function runTrendAnalysis() {
  if (!insightForm.value.dataSourceId || !insightForm.value.sql) return
  trendLoading.value = true
  try {
    const res = await analyzeTrend(insightForm.value.dataSourceId, insightForm.value.sql)
    insightTrends.value = (res as any).data || null
    message.success(t('aiAssist.trendAnalysisComplete'))
  } catch (error) {
    message.error(t('aiAssist.trendAnalysisFailed'))
  } finally {
    trendLoading.value = false
  }
}

// 生成图表
const generateChart = async () => {
  if (!canGenerateChart.value) return
  
  // 检查AI是否配置
  if (!aiStatus.value.enabled) {
    message.warning(t('aiAssist.configureAiFirst'))
    aiConfigStore.openConfig()
    return
  }
  
  chartLoading.value = true
  chartResult.value = null
  
  try {
    const res = await aiGenerateChart({
      requirement: chartForm.value.requirement,
      dataSourceId: chartForm.value.dataSourceId!,
      dbType: getDataSourceDbType(chartForm.value.dataSourceId),
      context: {
        dbType: getDataSourceDbType(chartForm.value.dataSourceId),
        ...(chartForm.value.chartType ? { preferredChartType: chartForm.value.chartType } : {}),
        ...(chartForm.value.colorTheme ? { colorTheme: chartForm.value.colorTheme } : {})
      }
    })
    
    const data = res as any
    if (data?.success && data.chartConfig) {
      const cfg = data.chartConfig
      chartResult.value = {
        chartType: cfg.chartType || 'bar',
        sql: cfg.sql || '',
        chartName: cfg.chartName || t('aiAssist.aiGeneratedChart'),
        chartConfig: cfg.chartConfig || {},
        aiResponse: data.content || ''
      }
      // 预览图表
      await nextTick()
      previewGeneratedChart()
    } else {
      message.error(data?.error || t('aiAssist.chartGenFailed'))
    }
  } catch (error: any) {
    message.error(error.message || t('aiAssist.chartGenFailed'))
  } finally {
    chartLoading.value = false
  }
}

// 预览生成的图表
const previewGeneratedChart = async () => {
  if (!chartResult.value || !chartPreviewRef.value) return
  
  try {
    const res = await testChartSql({
      dataSourceId: chartForm.value.dataSourceId!,
      sqlContent: chartResult.value.sql,
      limit: 100
    })
    
    const data = res as any
    if (Array.isArray(data) && data.length > 0) {
      renderChartPreview(data)
    } else if (data?.data && Array.isArray(data.data)) {
      renderChartPreview(data.data)
    }
  } catch (error) {
    logger.error('预览失败:', error)
  }
}

// 渲染图表预览 - 支持AI返回的chartConfig优先渲染，多系列自动检测
const renderChartPreview = (data: any[]) => {
  if (!chartPreviewRef.value || !chartResult.value) return
  
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  chartInstance = echarts.init(chartPreviewRef.value)
  
  // 优先使用 AI 返回的 chartConfig（参照 AiChartDesign 的模式）
  const aiConfig = chartResult.value.chartConfig
  if (aiConfig && typeof aiConfig === 'object' && (aiConfig.series || aiConfig.xAxis)) {
    try {
      // AI返回了完整的ECharts配置，直接使用
      const aiOption = { ...aiConfig }
      // 注入实际数据到配置中
      if (aiOption.xAxis && !aiOption.xAxis.data) {
        const keys = Object.keys(data[0]!)
        aiOption.xAxis.data = data.map((row: any) => String(row[keys[0]!]))
      }
      if (aiOption.series && Array.isArray(aiOption.series)) {
        const keys = Object.keys(data[0]!)
        aiOption.series.forEach((s: any, i: number) => {
          if (!s.data || s.data.length === 0) {
            const fieldIdx = Math.min(i + 1, keys.length - 1)
            if (s.type === 'pie') {
              s.data = data.map((row: any) => ({ name: String(row[keys[0]!]), value: Number(row[keys[fieldIdx]!]) || 0 }))
            } else {
              s.data = data.map((row: any) => Number(row[keys[fieldIdx]!]) || 0)
            }
          }
        })
      }
      if (!aiOption.title) {
        aiOption.title = { text: chartResult.value.chartName, left: 'center' }
      }
      chartInstance.setOption(aiOption)
      return
    } catch (e) {
      logger.warn('AI配置渲染失败，回退到自动推断:', e)
    }
  }
  
  // 回退逻辑：自动推断图表配置（支持多系列）
  const keys = Object.keys(data[0]!)
  const chartType = chartResult.value.chartType
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#fc8452', '#9a60b4']
  
  // 区分维度列和数值列
  const xField = keys[0]!
  const numericFields = keys.slice(1).filter(k => {
    return data.some(row => typeof row[k] === 'number' || (!isNaN(Number(row[k])) && row[k] !== null && row[k] !== ''))
  })
  const yFields = numericFields.length > 0 ? numericFields : (keys.length > 1 ? [keys[1]!] : [keys[0]!])
  
  let option: any
  
  if (chartType === 'pie') {
    const yField = yFields[0]!
    option = {
      title: { text: chartResult.value.chartName, left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 10, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        label: { show: true, formatter: '{b}: {d}%' },
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.2)' } },
        data: data.map(row => ({ name: String(row[xField]), value: Number(row[yField]) || 0 }))
      }],
      color: colors
    }
  } else {
    // 多系列支持
    const series = yFields.map((field, _idx) => ({
      name: field,
      type: chartType as any,
      data: data.map(row => Number(row[field]) || 0),
      smooth: chartType === 'line',
      emphasis: { focus: 'series' as const },
      ...(chartType === 'bar' && yFields.length > 1 ? { barMaxWidth: 40 } : {})
    }))
    
    option = {
      title: { text: chartResult.value.chartName, left: 'center', textStyle: { fontSize: 15 } },
      tooltip: { trigger: 'axis' },
      legend: yFields.length > 1 ? { bottom: 0, type: 'scroll' } : undefined,
      grid: { left: '3%', right: '4%', bottom: yFields.length > 1 ? '15%' : '10%', containLabel: true },
      xAxis: { type: 'category', data: data.map(row => String(row[xField])), axisLabel: { rotate: data.length > 8 ? 30 : 0 } },
      yAxis: { type: 'value' },
      series,
      color: colors
    }
  }
  
  chartInstance.setOption(option)
}

// 保存生成的图表
const saveGeneratedChart = async () => {
  if (!chartResult.value) return
  
  try {
    const res = await aiCreateChart({
      chartName: chartResult.value.chartName,
      chartCode: `ai_chart_${Date.now()}`,
      chartType: chartResult.value.chartType,
      description: t('aiAssist.aiAutoGenerated'),
      dataSourceId: chartForm.value.dataSourceId!,
      sql: chartResult.value.sql,
      chartConfig: JSON.stringify(chartResult.value.chartConfig)
    })
    
    const data = res as any
    if (data?.success) {
      message.success(t('aiAssist.chartSaved'))
      chartResult.value.chartId = data.chartId
    } else {
      message.error(data?.error || t('aiAssist.saveFailed'))
    }
  } catch (error: any) {
    message.error(error.message || t('aiAssist.saveFailed'))
  }
}

// 在设计器中编辑
const openChartDesigner = () => {
  if (chartResult.value?.chartId) {
    router.push(`/chart-manage/edit/${chartResult.value.chartId}`)
  } else {
    message.info(t('aiAssist.saveChartFirst'))
  }
}

// 文件识别相关
const fileLoading = ref(false)
const fileAnalysisResult = ref<{
  success: boolean
  columns: { title: string; key: string }[]
  dataPreview: any[]
  analysis: string
  totalRows?: number
  fileName?: string
} | null>(null)

const handleFileUpload = async ({ file, onFinish, onError }: any) => {
  fileLoading.value = true
  fileAnalysisResult.value = null
  
  const formData = new FormData()
  formData.append('file', file.file)
  
  try {
    const res = await request.post('/ai/analyze-file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000 // 60秒超时
    })
    
    const data = (res as any).data || res
    if (data?.success) {
      // 构建列配置
      const columns = data.columns?.map((col: string) => ({
        title: col,
        key: col
      })) || []
      
      fileAnalysisResult.value = {
        success: true,
        columns,
        dataPreview: data.data || [],
        analysis: data.analysis || '',
        totalRows: data.totalRows || data.data?.length || 0,
        fileName: file.name
      }
      message.success(t('aiAssist.fileParseSuccess', { count: fileAnalysisResult.value.totalRows }))
      onFinish()
    } else {
      fileAnalysisResult.value = {
        success: false,
        columns: [],
        dataPreview: [],
        analysis: data?.error || t('aiAssist.fileParseFailed')
      }
      message.error(data?.error || t('aiAssist.fileParseFailed'))
      onError()
    }
  } catch (error: any) {
    fileAnalysisResult.value = {
      success: false,
      columns: [],
      dataPreview: [],
      analysis: error.message || t('aiAssist.fileUploadFailed')
    }
    message.error(error.message || t('aiAssist.fileUploadFailed'))
    onError()
  } finally {
    fileLoading.value = false
  }
}

const handleFileChange = () => {
  // 文件变化时重置结果
  fileAnalysisResult.value = null
}

const createChartFromFile = () => {
  if (!fileAnalysisResult.value?.dataPreview?.length) return
  
  // 切换到图表生成标签
  activeTab.value = 'chart'
  
  // 预填充需求描述
  const columns = fileAnalysisResult.value.columns.map(c => c.key).join(', ')
  chartForm.value.requirement = t('aiAssist.chartFromFileReq', { columns })
  
  message.info(t('aiAssist.selectDsAndAdjust'))
}

const exportAnalyzedData = () => {
  if (!fileAnalysisResult.value?.dataPreview?.length) return
  
  // 导出为CSV
  const data = fileAnalysisResult.value.dataPreview
  const columns = fileAnalysisResult.value.columns.map(c => c.key)
  
  let csv = columns.join(',') + '\n'
  data.forEach(row => {
    csv += columns.map(col => row[col] ?? '').join(',') + '\n'
  })
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'analyzed_data.csv'
  link.click()
}

// AI对话（增强版）
const chatInput = ref('')
const chatMessages = ref<Array<{ role: string; content: string }>>([])
const chatMessagesRef = ref<HTMLElement | null>(null)
const sessionId = ref<string | null>(null)
const chatDataSourceId = ref<number | null>(null)

const chatInputPlaceholder = computed(() => {
  if (chatDataSourceId.value) {
    const ds = dataSourceOptions.value.find(d => d.value === chatDataSourceId.value)
    return t('aiAssist.askAboutDs', { ds: ds?.label || t('aiAssist.dataSource') })
  }
  return t('aiAssist.chatInputPlaceholder')
})

const getDataSourceLabel = (id: number) => {
  return dataSourceOptions.value.find(d => d.value === id)?.label || `DS ${id}`
}

// 初始化会话
const initSession = async () => {
  try {
    const res = await request.post('/ai/session/create')
    if (res.code === 200) {
      sessionId.value = res.data.sessionId
    }
  } catch (e) {
    logger.error('创建会话失败', e)
  }
}

// 清空对话
const clearChat = async () => {
  if (sessionId.value) {
    try {
      await request.post('/ai/session/clear', { sessionId: sessionId.value })
    } catch (e) {
      logger.error('清除会话失败', e)
    }
  }
  chatMessages.value = []
  await initSession()
  message.success(t('aiAssist.chatCleared'))
}

// 使用快捷提示
const useQuickPrompt = (prompt: string) => {
  chatInput.value = prompt
}

// 获取AI状态 - 使用共享 store
const refreshStatus = async () => {
  await aiConfigStore.refreshStatus()
  await aiConfigStore.refreshUsageStats()
}

// 生成SQL
const generateSql = async () => {
  if (!sqlForm.value.query.trim()) {
    message.warning(t('aiAssist.enterQueryDesc'))
    return
  }
  if (!aiStatus.value.enabled) {
    message.warning(t('aiAssist.configureAiFirst'))
    aiConfigStore.openConfig()
    return
  }
  
  sqlLoading.value = true
  sqlResult.value = ''
  
  try {
    const res = await request.post('/ai/generate-sql', {
      query: sqlForm.value.query,
      tableSchema: sqlForm.value.tableSchema,
      dataSourceId: sqlForm.value.dataSourceId,
      dbType: getDataSourceDbType(sqlForm.value.dataSourceId)
    })
    
    if (res.code === 200 && res.data?.success) {
      sqlResult.value = res.data.content
    } else {
      message.error(res.message || t('aiAssist.sqlGenFailed'))
    }
  } catch (e: any) {
    message.error(e.message || t('aiAssist.sqlGenFailed'))
  } finally {
    sqlLoading.value = false
  }
}

// 数据分析
const analyzeData = async () => {
  if (!analyzeForm.value.question.trim()) {
    message.warning(t('aiAssist.enterAnalysisQuestion'))
    return
  }
  if (!aiStatus.value.enabled) {
    message.warning(t('aiAssist.configureAiFirst'))
    aiConfigStore.openConfig()
    return
  }
  
  analyzeLoading.value = true
  analyzeResult.value = ''
  
  try {
    const res = await request.post('/ai/analyze', {
      question: analyzeForm.value.question,
      dataContext: analyzeForm.value.dataContext
    })
    
    if (res.code === 200 && res.data?.success) {
      analyzeResult.value = res.data.content
    } else {
      message.error(res.message || t('aiAssist.analysisFailed'))
    }
  } catch (e: any) {
    message.error(e.message || t('aiAssist.analysisFailed'))
  } finally {
    analyzeLoading.value = false
  }
}

// SQL优化
const optimizeSql = async () => {
  if (!optimizeForm.value.sql.trim()) {
    message.warning(t('aiAssist.enterSqlStatement'))
    return
  }
  if (!aiStatus.value.enabled) {
    message.warning(t('aiAssist.configureAiFirst'))
    aiConfigStore.openConfig()
    return
  }
  
  optimizeLoading.value = true
  optimizeResult.value = ''
  
  try {
    const res = await request.post('/ai/optimize-sql', {
      sql: optimizeForm.value.sql,
      dbType: optimizeForm.value.dbType
    })
    
    if (res.code === 200 && res.data?.success) {
      optimizeResult.value = res.data.content
    } else {
      message.error(res.message || t('aiAssist.optimizeFailed'))
    }
  } catch (e: any) {
    message.error(e.message || t('aiAssist.optimizeFailed'))
  } finally {
    optimizeLoading.value = false
  }
}

// 处理对话输入框键盘事件：Enter发送，Shift+Enter换行
const handleChatKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendChat()
  }
}

// 构建对话上下文
const buildChatContext = () => {
  const ctx: any = {
    platform: 'DataTeaCup',
    capabilities: ['Data Source Management', 'SQL Query', 'ETL Data Sync', 'Report Analytics', 'Data Import/Export']
  }
  if (chatDataSourceId.value) {
    const ds = dataSourceOptions.value.find(d => d.value === chatDataSourceId.value)
    if (ds) {
      ctx.selectedDataSource = { id: chatDataSourceId.value, name: ds.label }
    }
  }
  return JSON.stringify(ctx)
}

// 发送对话（同步模式）
const sendChat = async () => {
  const input = chatInput.value.trim()
  if (!input) return

  if (!aiStatus.value.enabled) {
    message.warning(t('aiAssist.configureAiFirst'))
    aiConfigStore.openConfig()
    return
  }

  if (!sessionId.value) {
    await initSession()
  }

  chatMessages.value.push({ role: 'user', content: input })
  chatInput.value = ''
  chatSending.value = true

  await nextTick()
  scrollToBottom()

  await sendChatSync(input)
}

// 同步对话
const sendChatSync = async (input: string) => {
  try {
    const res = await request.post('/ai/chat', {
      message: input,
      sessionId: sessionId.value,
      context: buildChatContext()
    }, { timeout: 180000 })

    if (res.code === 200 && res.data?.success) {
      chatMessages.value.push({ role: 'assistant', content: res.data.content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '⚠️ **Error**\n\n' + (res.data?.error || res.message || t('aiAssist.unknownError')) })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '⚠️ **Error**\n\n' + (e.message || t('aiAssist.networkError')) })
  } finally {
    chatSending.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 在侧边栏AI对话中打开
const openInChatDrawer = () => {
  if (chatInput.value.trim()) {
    aiConfigStore.openChatWithMessage(chatInput.value.trim())
  } else {
    aiConfigStore.showChatDrawer = true
  }
}

// 将结果发送到AI对话侧边栏继续讨论
const sendResultToChat = (content: string, context: string) => {
  aiConfigStore.openChatWithMessage(content, context)
}

// 重新生成最后一条回复
const regenerateChat = async (index: number) => {
  let lastUserMsgIndex = -1
  for (let i = index; i >= 0; i--) {
    if (chatMessages.value[i]?.role === 'user') {
      lastUserMsgIndex = i
      break
    }
  }
  if (lastUserMsgIndex < 0) return

  const lastUserText = chatMessages.value[lastUserMsgIndex]!.content
  // 移除从用户消息之后的所有消息
  chatMessages.value.splice(lastUserMsgIndex)

  chatInput.value = lastUserText
  await nextTick()
  await sendChat()
}

// 复制消息内容
const copyMessageContent = (content: string) => {
  // 清理 Markdown 标记
  const cleanText = content
    .replace(/```[\s\S]*?```/g, (m) => m.replace(/```\w*\n?/g, '').replace(/```/g, ''))
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/\*([^*]+)\*/g, '$1')
    .replace(/^#+\s+/gm, '')
  navigator.clipboard.writeText(cleanText)
  message.success(t('common.copiedToClipboard'))
}

// 处理代码块点击（复制）
const handleCodeBlockClick = (event: Event) => {
  const target = event.target as HTMLElement
  if (target.classList.contains('code-copy-btn')) {
    const encodedCode = target.getAttribute('data-code')
    if (encodedCode) {
      try {
        const code = decodeURIComponent(escape(atob(encodedCode)))
        navigator.clipboard.writeText(code).then(() => {
          target.textContent = t('common.copied')
          setTimeout(() => { target.textContent = t('common.copy') }, 1500)
        })
      } catch {
        message.error(t('common.copyFailed'))
      }
    }
  }
}

// HTML 转义
const escapeHtml = (text: string): string => {
  const map: Record<string, string> = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }
  return text.replace(/[&<>"']/g, m => map[m]!)
}

// Markdown 渲染缓存 (避免每次重渲染都做正则)
const _mdCache = new Map<string, string>()
let _mdCacheSize = 0
const MAX_MD_CACHE = 200

// 增强版 Markdown 渲染（带代码块复制按钮 + 缓存）
const renderEnhancedMarkdown = (text: string) => {
  if (!text) return ''
  const cached = _mdCache.get(text)
  if (cached) return cached

  // 代码块 - 添加语言标签和复制按钮
  let html = text.replace(/```(\w*)\n?([\s\S]*?)```/g, (_match, lang, code) => {
    const langLabel = lang || 'code'
    const cleanCode = code.trim().replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\r/g, '').replace(/\r\n/g, '\n')
    const encodedCode = btoa(unescape(encodeURIComponent(cleanCode)))
    const displayCode = escapeHtml(cleanCode)
    return `<div class="code-block"><div class="code-header"><span class="code-lang">${escapeHtml(langLabel)}</span><button class="code-copy-btn" data-code="${encodedCode}">${t('common.copy')}</button></div><pre><code class="lang-${escapeHtml(langLabel)}">${displayCode}</code></pre></div>`
  })

  // 行内代码
  html = html.replace(/`([^`]+)`/g, (_m, code) => `<code class="inline-code">${escapeHtml(code)}</code>`)
  // 粗体
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  // 斜体
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  // 标题
  html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^# (.+)$/gm, '<h2>$1</h2>')
  // 列表
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
  // 换行
  html = html.replace(/\n/g, '<br>')

  // 写入缓存
  if (_mdCacheSize >= MAX_MD_CACHE) {
    // 清理最旧的一半缓存
    const keys = Array.from(_mdCache.keys())
    for (let i = 0; i < keys.length / 2; i++) _mdCache.delete(keys[i]!)
    _mdCacheSize = _mdCache.size
  }
  _mdCache.set(text, html)
  _mdCacheSize++

  return html
}

// 滚动到底部（RAF 去抖）
let _scrollRafId = 0
const scrollToBottom = () => {
  if (_scrollRafId) cancelAnimationFrame(_scrollRafId)
  _scrollRafId = requestAnimationFrame(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
    _scrollRafId = 0
  })
}

// 可见消息（限制渲染数量，超过100条只显示最近100条）
const MAX_VISIBLE_MESSAGES = 100
const visibleMessages = computed(() => {
  const msgs = chatMessages.value
  if (msgs.length <= MAX_VISIBLE_MESSAGES) return msgs
  return msgs.slice(msgs.length - MAX_VISIBLE_MESSAGES)
})

// 复制SQL
const copySql = () => {
  navigator.clipboard.writeText(sqlResult.value)
  message.success(t('common.copiedToClipboard'))
}

// 渲染Markdown
const renderMarkdown = (content: string) => {
  return marked(content)
}

onMounted(() => {
  aiConfigStore.init()
  initSession()
  loadDataSources()
  loadPromptTemplates()
})

onUnmounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.ai-assistant {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* AI头部 - 紧凑单行 */
.ai-header-compact {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af));
  border-radius: 10px;
  color: white;
  min-height: 44px;
}

.ai-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.ai-logo-sm {
  width: 28px;
  height: 28px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-title-sm {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.ai-status-dot-sm {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(255,255,255,0.15);
  border-radius: 12px;
  font-size: 12px;
}

.ai-status-dot-sm .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6b7280;
  flex-shrink: 0;
}

.ai-status-dot-sm.active .dot {
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.7);
}

.ai-header-center {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.ai-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  opacity: 0.85;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.ai-meta-item.text-warning {
  color: #fcd34d;
  opacity: 1;
}

.ai-meta-sep {
  opacity: 0.4;
  font-size: 14px;
  flex-shrink: 0;
}

.ai-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.ai-header-right :deep(.n-button) {
  color: white !important;
}

.ai-header-right :deep(.n-button--primary-type) {
  background: rgba(255,255,255,0.2) !important;
  border-color: rgba(255,255,255,0.4) !important;
}

.ai-header-right :deep(.n-button--primary-type:hover) {
  background: rgba(255,255,255,0.3) !important;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-title {
  font-size: 16px;
  font-weight: 600;
}

.ai-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ai-tabs :deep(.n-tab-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tip-alert {
  margin-bottom: 8px;
}

.input-area {
  margin-bottom: 8px;
}

.action-bar {
  display: flex;
  gap: 12px;
}

.result-area {
  margin-top: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #374151;
}

.markdown-content {
  line-height: 1.6;
}

.markdown-content :deep(pre) {
  background: #1f2937;
  color: #f9fafb;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.markdown-content :deep(code) {
  background: #e5e7eb;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
}

/* 对话样式 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 280px);
  min-height: 400px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 16px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9ca3af;
}

.chat-tips {
  font-size: 12px;
  margin-top: 4px;
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-message.user .message-avatar {
  background: #3b82f6;
  color: white;
}

.chat-message.assistant .message-avatar {
  background: #10b981;
  color: white;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 16px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  line-height: 1.6;
  font-size: 14px;
  animation: msgFadeIn 0.3s ease-out;
}

@keyframes msgFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-message.user .message-content {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
  border-bottom-right-radius: 4px;
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.25);
}

.chat-message.assistant .message-content {
  border-bottom-left-radius: 4px;
  border: 1px solid #f0f0f0;
}

/* === 输入框卡片样式 === */
.chat-input-area {
  padding: 0;
}

.chat-input-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 8px 12px 8px 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: border-color 0.3s, box-shadow 0.3s;
}

.chat-input-card:focus-within {
  border-color: #818cf8;
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.12);
}

.chat-input-inner :deep(.n-input__textarea-el) {
  font-size: 14px;
  line-height: 1.6;
  padding: 4px 0 !important;
}

.chat-input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 4px;
}

.char-count {
  font-size: 11px;
  color: #9ca3af;
  user-select: none;
}

.char-count.warning {
  color: #ef4444;
}

.send-btn {
  width: 40px !important;
  height: 40px !important;
  border-radius: 12px !important;
  padding: 0 !important;
  min-width: unset !important;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  border: none !important;
  box-shadow: 0 3px 10px rgba(99, 102, 241, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.08);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.5);
}

.send-btn:disabled {
  opacity: 0.45;
  background: #d1d5db !important;
  box-shadow: none;
}

/* === 快捷指令样式 === */
.quick-actions {
  margin-bottom: 16px;
  padding: 20px;
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af));
  border-radius: 16px;
  color: white;
}

.quick-action-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 14px;
  opacity: 0.9;
}

.quick-action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 13px;
  font-weight: 500;
  backdrop-filter: blur(4px);
}

.quick-action-item:hover {
  background: rgba(255, 255, 255, 0.28);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
}

/* 对话头部 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f3f4f6;
  border-radius: 8px 8px 0 0;
  margin-bottom: -1px;
}

.session-info {
  font-size: 12px;
  color: #6b7280;
  font-family: monospace;
}

/* 图表预览样式 */
.chart-preview-box {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  min-height: 300px;
  position: relative;
}

.chart-preview {
  width: 100%;
  height: 280px;
}

/* 选项卡标签样式 */
.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}

/* 功能卡片样式 */
.feature-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.feature-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e5e7eb;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.feature-icon.sql {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.feature-icon.analyze {
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
}

.feature-icon.optimize {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.feature-icon.chart {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.feature-icon.file {
  background: linear-gradient(135deg, #ec4899 0%, #be185d 100%);
}

.feature-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.feature-info p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.feature-body {
  padding: 20px;
}

.feature-card .result-area {
  margin: 0;
  border-top: 1px solid #e5e7eb;
  border-radius: 0;
}

/* 数据预览样式 */
.data-preview {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

/* AI配置弹窗样式 */
.ai-config-modal :deep(.n-card) {
  max-width: 720px;
  border-radius: 16px;
  overflow: hidden;
}

.config-modal-container {
  background: #fff;
  border-radius: 16px;
  width: 720px;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.config-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af));
  color: white;
}

.config-modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.title-text h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.title-text p {
  margin: 4px 0 0;
  font-size: 12px;
  opacity: 0.85;
}

.config-modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

/* 提供商选择网格 */
.provider-section {
  margin-bottom: 20px;
}

.section-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.provider-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 10px;
  background: #f9fafb;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.provider-card:hover {
  border-color: #a5b4fc;
  background: #f0f4ff;
}

.provider-card.active {
  border-color: #6366f1;
  background: #eef2ff;
}

.provider-icon {
  font-size: 24px;
  line-height: 1;
}

.provider-name {
  font-size: 12px;
  font-weight: 500;
  color: #4b5563;
  text-align: center;
}

.provider-check {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 18px;
  height: 18px;
  background: #6366f1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

/* 提供商说明卡片 */
.provider-info-card {
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 20px;
  border-left: 4px solid;
}

.provider-info-card.info {
  background: #eff6ff;
  border-color: #3b82f6;
}

.provider-info-card.success {
  background: #f0fdf4;
  border-color: #22c55e;
}

.provider-info-card.warning {
  background: #fefce8;
  border-color: #eab308;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.info-title {
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
}

.doc-link {
  font-size: 12px;
  color: #6366f1;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.doc-link:hover {
  text-decoration: underline;
}

.info-desc {
  margin: 0;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
}

/* 配置表单 */
.config-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.form-label .required {
  color: #ef4444;
}

.form-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

/* 高级配置 */
.advanced-collapse {
  margin-top: 8px;
}

.advanced-collapse :deep(.n-collapse-item__header-main) {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
}

.advanced-params {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 8px 0;
}

.param-item label {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.param-item .param-desc {
  font-size: 11px;
  color: #9ca3af;
  margin: 4px 0 10px;
}

.param-control {
  display: flex;
  align-items: center;
  gap: 16px;
}

.param-control .n-slider {
  flex: 1;
}

/* 弹窗底部 */
.config-modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

/* === AI思考动画 === */
.thinking-bubble {
  display: flex;
  align-items: center;
  padding: 16px 20px !important;
}

.thinking-dots {
  display: flex;
  gap: 6px;
}

.thinking-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a5b4fc;
  animation: thinkingPulse 1.4s ease-in-out infinite;
}

.thinking-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes thinkingPulse {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ========== 增强对话样式 ========== */
.enhanced-chat .chat-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 10px;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
}

.enhanced-chat .toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.enhanced-chat .toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.enhanced-chat .quick-actions.enhanced .quick-action-grid {
  grid-template-columns: repeat(3, 1fr);
}

.enhanced-chat .message-bubble-wrapper {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.enhanced-chat .message-bubble-wrapper .message-content {
  max-width: 100%;
}

.enhanced-chat .message-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.enhanced-chat .chat-message:hover .message-actions {
  opacity: 1;
}

.enhanced-chat .streaming-avatar {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.enhanced-chat .streaming-controls {
  margin-top: 6px;
}

.enhanced-chat .user-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.enhanced-chat .footer-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.enhanced-chat .context-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 10px;
  font-size: 11px;
  border: 1px solid #bfdbfe;
}

.enhanced-chat .input-tips {
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 6px;
}

/* 代码块样式 */
.enhanced-chat .code-block {
  margin: 10px 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #1e1e2e;
}

.enhanced-chat .code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #2d2d3f;
  border-bottom: 1px solid #3d3d50;
}

.enhanced-chat .code-lang {
  font-size: 11px;
  color: #a0a0b8;
  text-transform: uppercase;
  font-weight: 500;
}

.enhanced-chat .code-copy-btn {
  padding: 2px 10px;
  font-size: 11px;
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.enhanced-chat .code-copy-btn:hover {
  background: #059669;
}

.enhanced-chat .code-block pre {
  margin: 0;
  padding: 12px 16px;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.enhanced-chat .code-block code {
  color: #e2e8f0;
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
}

.enhanced-chat .inline-code {
  padding: 2px 6px;
  background: #f1f5f9;
  color: #e11d48;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'Fira Code', 'Consolas', monospace;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .ai-assistant {
    padding: 0;
    height: calc(100vh - var(--mobile-header-height, 48px) - var(--mobile-tab-bar-height, 56px) - var(--mobile-safe-top, 0px) - var(--mobile-safe-bottom, 0px));
    display: flex;
    flex-direction: column;
  }

  .ai-header-card {
    border-radius: 14px !important;
    padding: 12px !important;
    margin-bottom: 10px;
  }

  .ai-brand-title {
    font-size: 16px !important;
  }

  .ai-config-panel {
    flex-direction: column;
    gap: 8px;
  }

  .config-item {
    padding: 8px 10px !important;
  }
  
  .config-actions {
    margin-left: 0;
    margin-top: 8px;
  }

  .quick-action-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px !important;
  }

  .enhanced-chat .quick-actions.enhanced .quick-action-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .enhanced-chat {
    border-radius: 14px !important;
    flex: 1;
    min-height: 0;
  }

  .chat-messages {
    padding: 8px !important;
  }

  .message-bubble {
    max-width: 90% !important;
    font-size: 14px;
  }

  .chat-input-container {
    padding: 8px 10px !important;
  }
  
  .config-modal-container {
    width: 95vw;
    max-height: 90vh;
  }
  
  .provider-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

</style>

<style>
/* AiAssistant 深色模式（非 scoped） */
html.dark .grid-item { background: #1e293b !important; border-color: #334155 !important; }
html.dark .grid-item:hover { border-color: var(--color-primary) !important; }
html.dark .item-name { color: #e2e8f0 !important; }
html.dark .item-desc { color: #64748b !important; }
html.dark .item-meta { color: #475569 !important; }
html.dark .item-thumb { background: #1a2536 !important; }

/* 功能卡片 */
html.dark .ai-assistant .feature-card { background: #1e293b; border-color: #334155; }
html.dark .ai-assistant .feature-header { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); border-color: #334155; }
html.dark .ai-assistant .feature-info h4 { color: #e2e8f0; }
html.dark .ai-assistant .feature-info p { color: #64748b; }

/* 结果区域 */
html.dark .ai-assistant .result-area { background: #0f172a; border-color: #334155; }
html.dark .ai-assistant .result-header { color: #e2e8f0; }
html.dark .ai-assistant .feature-card .result-area { border-top-color: #334155; }

/* 对话区域 */
html.dark .ai-assistant .chat-messages { background: #0f172a; }
html.dark .ai-assistant .chat-empty { color: #475569; }
html.dark .ai-assistant .chat-message.assistant .message-content { background: #1e293b; border-color: #334155; color: #e2e8f0; }
html.dark .ai-assistant .chat-input-card { background: #1e293b; border-color: #334155; box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2); }
html.dark .ai-assistant .chat-input-card:focus-within { border-color: #818cf8; box-shadow: 0 4px 20px rgba(99, 102, 241, 0.2); }
html.dark .ai-assistant .chat-input-inner .n-input__textarea-el { color: #e2e8f0 !important; }

/* 工具栏 */
html.dark .ai-assistant .enhanced-chat .chat-toolbar { background: #1e293b; border-color: #334155; }
html.dark .ai-assistant .enhanced-chat .context-tag { background: #1e3a5f; color: #93c5fd; border-color: #1e3a5f; }

/* Markdown内容 */
html.dark .ai-assistant .markdown-content { color: #cbd5e1; }
html.dark .ai-assistant .markdown-content code { background: #334155; color: #f472b6; }
html.dark .ai-assistant .markdown-content pre { background: #0f172a; }
html.dark .ai-assistant .markdown-content pre code { background: transparent; color: #f9fafb; }

/* 图表预览 */
html.dark .ai-assistant .chart-preview-box { background: #1e293b; border-color: #334155; }

/* 数据预览 */
html.dark .ai-assistant .data-preview { background: #1e293b; }
</style>
