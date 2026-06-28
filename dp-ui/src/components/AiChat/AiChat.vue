<template>
  <div v-if="showFab && hasAiPermission" class="ai-chat-wrapper">
    <!-- AI悬浮按钮 -->
    <Transition name="fab">
      <div v-show="!showDrawer" class="ai-fab" title="AI助手" @click="showDrawer = true">
        <n-icon size="22"><SparklesOutline /></n-icon>
        <div class="fab-close" title="隐藏" @click.stop="hideFab">
          <n-icon size="12"><CloseOutline /></n-icon>
        </div>
      </div>
    </Transition>
    
    <!-- AI抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="isFullscreen ? '100%' : 640" placement="right" :mask-closable="true">
      <n-drawer-content :native-scrollbar="false">
        <template #header>
          <div class="drawer-header">
            <div class="header-left">
              <div class="header-logo">
                <n-icon size="18"><SparklesOutline /></n-icon>
              </div>
              <span class="header-title">AI 助手</span>
              <n-tag :type="aiStatus.enabled ? 'success' : 'warning'" size="small" round>
                {{ aiDisplayLabel }}
              </n-tag>
            </div>
            <div class="header-actions">
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button text size="small" @click="isFullscreen = !isFullscreen">
                    <template #icon><n-icon size="18"><ExpandOutline v-if="!isFullscreen" /><ContractOutline v-else /></n-icon></template>
                  </n-button>
                </template>
                {{ isFullscreen ? '退出全屏' : '全屏模式' }}
              </n-tooltip>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button text size="small" @click="createNewSession">
                    <template #icon><n-icon size="18"><AddCircleOutline /></n-icon></template>
                  </n-button>
                </template>
                新建对话
              </n-tooltip>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button text size="small" @click="showHistoryPanel = !showHistoryPanel">
                    <template #icon><n-icon size="18"><ChatbubblesOutline /></n-icon></template>
                  </n-button>
                </template>
                历史对话
              </n-tooltip>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button text size="small" @click="clearChat">
                    <template #icon><n-icon size="18"><TrashOutline /></n-icon></template>
                  </n-button>
                </template>
                清空对话
              </n-tooltip>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button text size="small" @click="showConfigModal = true">
                    <template #icon><n-icon size="18"><SettingsOutline /></n-icon></template>
                  </n-button>
                </template>
                AI配置
              </n-tooltip>
            </div>
          </div>
        </template>
        
        <!-- 历史会话面板 -->
        <n-collapse-transition :show="showHistoryPanel">
          <div class="history-panel">
            <div class="history-header">
              <div class="history-header-left">
                <n-icon size="16" color="#6366f1"><ChatbubblesOutline /></n-icon>
                <span>历史对话</span>
              </div>
              <div class="history-header-right">
                <n-button size="tiny" quaternary :loading="loadingSessions" @click="loadSessionList">
                  <template #icon><n-icon size="14"><SyncOutline /></n-icon></template>
                  刷新
                </n-button>
                <n-button size="tiny" type="primary" @click="createNewSession">
                  <template #icon><n-icon size="14"><AddCircleOutline /></n-icon></template>
                  新对话
                </n-button>
              </div>
            </div>
            <n-scrollbar v-if="sessionList.length > 0" style="max-height: 200px;">
              <div class="history-list">
                <div 
                  v-for="s in sessionList" 
                  :key="s.sessionId" 
                  class="history-item"
                  :class="{ active: s.sessionId === sessionId }"
                  @click="switchToSession(s.sessionId)"
                >
                  <div class="history-item-icon">
                    <n-icon size="16"><ChatbubblesOutline /></n-icon>
                  </div>
                  <div class="history-item-content">
                    <div class="history-title">{{ s.title || '新对话' }}</div>
                    <div v-if="s.lastTime" class="history-time">{{ formatTime(s.lastTime) }}</div>
                  </div>
                  <n-icon size="14" class="history-arrow"><ChevronDownOutline /></n-icon>
                </div>
              </div>
            </n-scrollbar>
            <div v-else class="history-empty">
              <div class="empty-icon-wrapper">
                <n-icon size="32" color="#cbd5e1"><ChatbubblesOutline /></n-icon>
              </div>
              <span class="empty-text">暂无历史对话</span>
              <span class="empty-hint">开始新对话后将自动保存</span>
            </div>
          </div>
        </n-collapse-transition>
        
        <!-- 数据源选择器 - 固定在顶部 -->
        <div v-if="systemContext.dataSources.length > 0" class="context-selector-fixed">
          <div class="context-selector">
            <n-icon size="14" color="#6366f1"><ServerOutline /></n-icon>
            <n-select 
              v-model:value="selectedDataSourceId"
              :options="dataSourceOptions"
              placeholder="选择数据源..."
              size="small"
              clearable
              style="flex: 1;"
              @update:value="onDataSourceSelect"
            />
            <n-select 
              v-if="currentDataSourceTables.length > 0"
              v-model:value="selectedTableName"
              :options="tableSelectOptions"
              placeholder="选择表..."
              size="small"
              clearable
              filterable
              style="flex: 1;"
              @update:value="onTableSelect"
            />
          </div>
          
          <!-- 已加载的表结构信息 -->
          <div v-if="currentDataSourceTables.length > 0" class="loaded-tables">
            <div class="tables-header" @click="showContextDetail = !showContextDetail">
              <n-icon size="14" color="#16a34a"><TableOutline /></n-icon>
              <span>已加载 {{ currentDataSourceTables.length }} 个表结构</span>
              <span class="view-detail">{{ showContextDetail ? '收起' : '查看详情' }}</span>
              <n-icon size="12">
                <ChevronDownOutline v-if="!showContextDetail" />
                <ChevronUpOutline v-else />
              </n-icon>
            </div>
            <n-collapse-transition :show="showContextDetail">
              <div class="schema-panel">
                <div class="schema-hint">
                  <n-icon size="14"><InformationCircleOutline /></n-icon>
                  AI已读取以下表结构，可直接询问相关SQL问题
                </div>
                <div class="table-cards">
                  <div 
                    v-for="t in currentDataSourceTables" 
                    :key="t.tableName" 
                    class="table-card"
                    :class="{ expanded: expandedTable === t.tableName }"
                    @click="toggleTableExpand(t.tableName)"
                  >
                    <div class="table-card-header">
                      <span class="table-name">{{ t.tableName }}</span>
                      <span class="column-count">{{ t.columns?.length || 0 }} 字段</span>
                    </div>
                    <div v-if="expandedTable === t.tableName && t.columns" class="table-columns">
                      <div v-for="col in t.columns" :key="col.columnName" class="column-item">
                        <span class="col-name" :class="{ pk: col.isPrimaryKey }">
                          {{ col.columnName }}
                          <span v-if="col.isPrimaryKey" class="pk-badge">PK</span>
                        </span>
                        <span class="col-type">{{ col.dataType }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </n-collapse-transition>
          </div>
        </div>
        
        <div class="chat-container">
          <!-- 对话式开发 - 快捷操作 -->
          <div v-if="chatMessages.length === 0" class="quick-actions">
            <div class="quick-header">
              <span class="quick-title">🚀 对话式开发</span>
              <span class="quick-desc">通过对话自动创建报表、ETL和智能分析</span>
            </div>
            
            <!-- 创建类操作 -->
            <div class="action-group">
              <div class="action-group-title">创建</div>
              <div class="quick-grid-4">
                <div class="quick-item-compact" @click="smartPrompt('createReport')">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #f59e0b, #d97706);"><n-icon size="16"><AddCircleOutline /></n-icon></div>
                  <span>创建报表</span>
                </div>
                <div class="quick-item-compact" @click="smartPrompt('createChart')">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #10b981, #059669);"><n-icon size="16"><BarChartOutline /></n-icon></div>
                  <span>创建图表</span>
                </div>
                <div class="quick-item-compact" @click="smartPrompt('createEtl')">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #06b6d4, #0891b2);"><n-icon size="16"><SyncOutline /></n-icon></div>
                  <span>创建ETL</span>
                </div>
                <div class="quick-item-compact" @click="smartPrompt('sql')">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #6366f1, #4f46e5);"><n-icon size="16"><CodeOutline /></n-icon></div>
                  <span>生成SQL</span>
                </div>
              </div>
            </div>
            
            <!-- 分析类操作 -->
            <div class="action-group">
              <div class="action-group-title">分析</div>
              <div class="quick-grid-3">
                <div class="quick-item-compact" @click="analyzeDataQuality">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #ec4899, #db2777);"><n-icon size="16"><StatsChartOutline /></n-icon></div>
                  <span>质量分析</span>
                </div>
                <div class="quick-item-compact" @click="generateDataDictionary">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #8b5cf6, #7c3aed);"><n-icon size="16"><BookOutline /></n-icon></div>
                  <span>数据字典</span>
                </div>
                <div class="quick-item-compact" @click="getSmartSuggestions">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #14b8a6, #0d9488);"><n-icon size="16"><BulbOutline /></n-icon></div>
                  <span>智能建议</span>
                </div>
              </div>
            </div>
            
            <!-- 查看类操作 -->
            <div class="action-group">
              <div class="action-group-title">查看</div>
              <div class="quick-grid-3">
                <div class="quick-item-compact" @click="viewReportsList">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #f43f5e, #e11d48);"><n-icon size="16"><DocumentTextOutline /></n-icon></div>
                  <span>报表列表</span>
                </div>
                <div class="quick-item-compact" @click="viewEtlJobs">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #0ea5e9, #0284c7);"><n-icon size="16"><GitBranchOutline /></n-icon></div>
                  <span>ETL任务</span>
                </div>
                <div class="quick-item-compact" @click="runSystemDiagnosis">
                  <div class="quick-icon-sm" style="background: linear-gradient(135deg, #10b981, #059669);"><n-icon size="16"><MedkitOutline /></n-icon></div>
                  <span>系统诊断</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div ref="messagesRef" class="messages" @click="handleCodeCopy">
            <!-- 未配置 AI 引导 -->
            <div v-if="!aiStatus.enabled" class="unconfigured-state">
              <div class="unconfig-icon">
                <n-icon size="56" color="#d1d5db"><SettingsOutline /></n-icon>
              </div>
              <p class="unconfig-title">AI 服务尚未配置</p>
              <p class="unconfig-desc">请先配置 AI 模型后再使用助手功能</p>
              <n-button type="primary" size="large" style="margin-top: 16px;" @click="showConfigModal = true">
                <template #icon><n-icon :component="SettingsOutline" /></template>
                前往配置
              </n-button>
              <div class="unconfig-steps">
                <div class="step-item">
                  <span class="step-num">1</span>
                  <span>选择 AI 服务商（如 DeepSeek、OpenAI）</span>
                </div>
                <div class="step-item">
                  <span class="step-num">2</span>
                  <span>填入 API 密钥和地址</span>
                </div>
                <div class="step-item">
                  <span class="step-num">3</span>
                  <span>测试连接并保存</span>
                </div>
              </div>
            </div>
            <div v-else-if="chatMessages.length === 0" class="empty-state">
              <div class="empty-icon">
                <div class="empty-icon-glow"></div>
                <n-icon size="48"><SparklesOutline /></n-icon>
              </div>
              <p class="empty-title">Hi，我是您的AI数据助手</p>
              <p class="empty-desc">可以帮您生成SQL、分析数据、创建报表和ETL流程</p>
              
              <!-- 智能示例问题 -->
              <div class="smart-examples">
                <div class="example-title">💡 试试这些问题</div>
                <div class="example-list">
                  <div class="example-item" @click="useExample('查询最近7天每天的数据量变化趋势')">
                    <n-icon size="14"><SearchOutline /></n-icon>
                    <span>查询最近7天数据量趋势</span>
                  </div>
                  <div class="example-item" @click="useExample('分析当前表的慢查询并给出索引优化建议')">
                    <n-icon size="14"><AnalyticsOutline /></n-icon>
                    <span>慢查询分析与索引优化</span>
                  </div>
                  <div class="example-item" @click="useExample('基于当前表创建一个数据看板，包含核心指标和趋势图')">
                    <n-icon size="14"><StatsChartOutline /></n-icon>
                    <span>创建数据看板</span>
                  </div>
                  <div class="example-item" @click="useExample('帮我写一个增量同步任务，按更新时间字段每小时同步')">
                    <n-icon size="14"><SyncOutline /></n-icon>
                    <span>创建增量同步任务</span>
                  </div>
                </div>
                
                <!-- 快捷命令提示 -->
                <div class="command-hint">
                  <span class="hint-icon">⌨️</span>
                  <span>输入 <code>/</code> 查看快捷命令，<code>/help</code> 获取帮助</span>
                </div>
              </div>
            </div>
            
            <div v-for="(msg, i) in chatMessages" :key="i" :class="['message', msg.role]">
              <div class="avatar">
                <n-icon size="14">
                  <PersonOutline v-if="msg.role === 'user'" />
                  <SparklesOutline v-else />
                </n-icon>
              </div>
              <div class="bubble-wrapper">
                <span v-if="msg.timestamp" class="msg-time">{{ formatMsgTime(msg.timestamp) }}</span>
                <div class="bubble">
                  <div v-if="msg.role === 'assistant'" class="markdown" v-html="renderMd(msg.content)"></div>
                  <div v-else class="user-text">{{ msg.content }}</div>
                </div>
                <!-- SQL执行结果展示 -->
                <div v-if="msg.sqlResult" class="sql-result">
                  <div class="result-header">
                    <span class="result-title">查询结果 ({{ msg.sqlResult.rowCount }} 行)</span>
                    <div class="result-actions">
                      <n-button size="tiny" type="primary" @click="exportSqlResult(msg.sqlResult, 'csv')">
                        <template #icon><n-icon size="12"><DownloadOutline /></n-icon></template>
                        导出CSV
                      </n-button>
                      <n-button size="tiny" @click="exportSqlResult(msg.sqlResult, 'excel')">
                        <template #icon><n-icon size="12"><DocumentOutline /></n-icon></template>
                        导出Excel
                      </n-button>
                    </div>
                  </div>
                  <div class="result-table-wrapper">
                    <table class="result-table">
                      <thead>
                        <tr>
                          <th v-for="col in msg.sqlResult.columns" :key="col">{{ col }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, ri) in msg.sqlResult.data?.slice(0, 10)" :key="ri">
                          <td v-for="col in msg.sqlResult.columns" :key="col">{{ row[col] }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <div v-if="msg.sqlResult.rowCount > 10" class="result-hint">
                    仅显示前10行，共 {{ msg.sqlResult.rowCount }} 行数据
                  </div>
                </div>
                <div v-if="msg.role === 'assistant'" class="bubble-actions">
                  <!-- 请求失败时显示重试按钮 -->
                  <n-button v-if="msg.content.includes('请求失败')" text size="tiny" type="primary" @click="retryLastMessage">
                    <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
                    重试
                  </n-button>
                  <n-button text size="tiny" @click="copyContent(msg.content)">
                    <template #icon><n-icon size="14"><CopyOutline /></n-icon></template>
                  </n-button>
                  <n-button 
                    v-if="extractSqlFromMessage(msg.content) && selectedDataSourceId" 
                    text size="tiny" 
                    :loading="msg.executing"
                    @click="executeSqlFromMessage(msg.content, i)"
                  >
                    <template #icon><n-icon size="14"><PlayOutline /></n-icon></template>
                    执行SQL
                  </n-button>
                  <n-button 
                    v-if="extractSqlFromMessage(msg.content) && selectedDataSourceId" 
                    text size="tiny" 
                    type="warning"
                    @click="showCreateReportModal(msg.content)"
                  >
                    <template #icon><n-icon size="14"><AddCircleOutline /></n-icon></template>
                    创建报表
                  </n-button>
                  <n-button 
                    v-if="extractSqlFromMessage(msg.content) && selectedDataSourceId" 
                    text size="tiny" 
                    type="success"
                    @click="showCreateChartModal(msg.content)"
                  >
                    <template #icon><n-icon size="14"><BarChartOutline /></n-icon></template>
                    创建图表
                  </n-button>
                  <n-button 
                    v-if="hasEtlConfig(msg.content)" 
                    text size="tiny" 
                    type="info"
                    @click="showCreateEtlModal(msg.content)"
                  >
                    <template #icon><n-icon size="14"><SyncOutline /></n-icon></template>
                    创建ETL
                  </n-button>
                  <n-button v-if="i === chatMessages.length - 1" text size="tiny" @click="regenerate(i)">
                    <template #icon><n-icon size="14"><RefreshOutline /></n-icon></template>
                  </n-button>
                </div>
              </div>
            </div>
            
            <div v-if="loading" class="message assistant">
              <div class="avatar thinking">
                <n-icon size="14"><SparklesOutline /></n-icon>
              </div>
              <div class="bubble-wrapper">
                <div class="bubble typing">
                  <div class="typing-indicator">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </div>
                  <span class="typing-text">AI正在思考...</span>
                  <button class="stop-gen-btn" @click="abortRequest">
                    <n-icon size="14"><StopCircleOutline /></n-icon>
                    停止生成
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <template #footer>
          <!-- 未配置时显示禁用提示 -->
          <div v-if="!aiStatus.enabled" class="input-box-disabled">
            <div class="disabled-hint">
              <n-icon size="16" color="#9ca3af"><SettingsOutline /></n-icon>
              <span>AI 服务未配置，请先</span>
              <n-button text type="primary" size="small" @click="showConfigModal = true">配置 AI 模型</n-button>
            </div>
          </div>
          <div v-else class="input-box">
            <!-- 斜杠命令提示 -->
            <div v-if="showSlashMenu" class="slash-commands">
              <div class="slash-title">快捷命令</div>
              <div 
                v-for="cmd in filteredCommands" 
                :key="cmd.command" 
                class="slash-item"
                :class="{ active: selectedCommandIndex === filteredCommands.indexOf(cmd) }"
                @click="selectCommand(cmd)"
              >
                <span class="cmd-icon">{{ cmd.icon }}</span>
                <div class="cmd-info">
                  <span class="cmd-name">{{ cmd.command }}</span>
                  <span class="cmd-desc">{{ cmd.description }}</span>
                </div>
              </div>
            </div>
            
            <!-- 上下文标签 -->
            <div v-if="selectedDataSourceId || selectedTableName" class="context-tags">
              <n-tag v-if="selectedDataSourceId" type="info" size="small" closable @close="clearDataSource">
                <template #icon><n-icon size="12"><ServerOutline /></n-icon></template>
                {{ getDataSourceName(selectedDataSourceId) }}
              </n-tag>
              <n-tag v-if="selectedTableName" type="success" size="small" closable @close="clearTable">
                <template #icon><n-icon size="12"><TableOutline /></n-icon></template>
                {{ selectedTableName }}
              </n-tag>
            </div>
            
            <!-- 文件上传预览 -->
            <div v-if="uploadedFile" class="file-preview">
              <div class="file-info">
                <n-icon size="16" color="#10b981"><DocumentOutline /></n-icon>
                <span class="file-name">{{ uploadedFile.name }}</span>
                <span class="file-size">({{ formatFileSize(uploadedFile.size) }})</span>
              </div>
              <n-button text size="tiny" @click="clearUploadedFile">
                <n-icon size="14"><CloseOutline /></n-icon>
              </n-button>
            </div>
            
            <div class="input-row" :class="{ 'has-context': selectedDataSourceId || selectedTableName }">
              <textarea 
                ref="textareaRef"
                v-model="inputText"
                :placeholder="inputPlaceholder"
                rows="5"
                class="text-input"
                @keydown.ctrl.enter="sendMessage"
                @keydown.up.prevent="navigateCommand(-1)"
                @keydown.down.prevent="navigateCommand(1)"
                @keydown.tab.prevent="confirmCommand"
                @keydown.enter.exact="handleEnterKey"
                @keydown.escape="handleEscapeKey"
                @input="handleInput"
              ></textarea>
              <div class="input-actions">
                <label class="upload-button" title="上传文件 (Excel/CSV)">
                  <input 
                    ref="fileInputRef" 
                    type="file" 
                    accept=".xlsx,.xls,.csv"
                    style="display: none;"
                    @change="handleFileSelect"
                  />
                  <n-icon size="18"><CloudUploadOutline /></n-icon>
                </label>
                <button 
                  class="send-button"
                  :class="{ active: inputText.trim() || uploadedFile }"
                  :disabled="(!inputText.trim() && !uploadedFile) || loading || !aiStatus.enabled"
                  @click="sendMessage"
                >
                  <n-spin v-if="loading" size="small" />
                  <n-icon v-else size="18"><SendOutline /></n-icon>
                </button>
              </div>
            </div>
            <div class="input-tips">
              <span class="hint">Enter 发送 · Shift+Enter 换行 · 输入 / 查看快捷命令</span>
              <!-- 智能上下文提示 -->
              <div v-if="selectedTableName && !inputText" class="context-suggestions">
                <span class="suggestion-label">💡 建议：</span>
                <span class="suggestion-chip" @click="useExample(`查询 ${selectedTableName} 表的前100条数据`)">查询数据</span>
                <span class="suggestion-chip" @click="useExample(`分析 ${selectedTableName} 表的字段分布情况`)">分析字段</span>
                <span class="suggestion-chip" @click="useExample(`为 ${selectedTableName} 表创建索引优化建议`)">索引建议</span>
              </div>
            </div>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
    
    <!-- AI配置弹窗 -->
    <n-modal v-model:show="showConfigModal" :mask-closable="false" class="ai-config-modal">
      <div class="config-modal-wrapper">
        <div class="config-modal-head">
          <div class="config-modal-title-row">
            <n-icon size="20" color="#18a058"><SettingsOutline /></n-icon>
            <span class="config-modal-title">AI 模型配置</span>
          </div>
          <n-button quaternary circle size="small" @click="showConfigModal = false">
            <template #icon><n-icon :component="CloseOutline" /></template>
          </n-button>
        </div>
        
        <!-- 服务商卡片选择 -->
        <div class="provider-cards">
          <div 
            v-for="p in providerOptions" :key="p.value"
            :class="['provider-card', { active: editingProvider === p.value, configured: providerConfigs[p.value]?.configured }]"
            @click="switchEditingProvider(p.value)"
          >
            <span class="provider-card-icon">{{ p.icon }}</span>
            <span class="provider-card-name">{{ p.label }}</span>
            <span v-if="activeProvider === p.value" class="provider-badge-active">使用中</span>
            <span v-else-if="providerConfigs[p.value]?.configured" class="provider-badge-ok">✓</span>
          </div>
        </div>
        
        <n-alert v-if="providerTip" :type="providerTip.type" :show-icon="false" style="margin-bottom: 14px; font-size: 12px; border-radius: 8px;">
          {{ providerTip.message }}
        </n-alert>
        
        <n-form label-placement="left" label-width="90" size="medium">
          <n-form-item label="API密钥">
            <n-input 
              v-model:value="configForm.apiKey" 
              type="password" 
              show-password-on="click" 
              :placeholder="isLocalProvider ? '本地部署可留空' : '输入API密钥'" 
            />
          </n-form-item>
          
          <n-form-item label="API地址">
            <n-input v-model:value="configForm.baseUrl" :placeholder="baseUrlPlaceholder" />
          </n-form-item>
          
          <n-form-item label="模型">
            <n-input-group>
              <n-select v-model:value="configForm.model" :options="modelOptions" filterable tag style="flex: 1;" />
              <n-button :loading="testingConnection" type="primary" ghost style="width: 90px;" @click="testConnection">
                测试连接
              </n-button>
            </n-input-group>
          </n-form-item>
          
          <n-form-item label="记忆轮数">
            <div class="form-field-wrap">
              <div style="display: flex; align-items: center; width: 100%;">
<n-slider v-model:value="configForm.maxHistory" :min="1" :max="20" :step="1" style="flex: 1;" />
                <n-tag size="small" type="info" round style="margin-left: 12px; min-width: 42px; text-align: center;">{{ configForm.maxHistory }} 轮</n-tag>
              </div>
              <div class="form-hint">AI 记住最近几轮对话，轮数越多回答越连贯，但消耗更多额度</div>
            </div>
          </n-form-item>
          
          <n-collapse style="margin-top: 4px;">
            <n-collapse-item title="高级设置" name="advanced">
              <n-form-item label="回答风格">
                <div class="form-field-wrap">
                  <div style="display: flex; align-items: center; width: 100%; gap: 10px;">
                    <span class="slider-label">精确</span>
                    <n-slider v-model:value="configForm.temperature" :min="0" :max="1" :step="0.1" style="flex: 1;" />
                    <span class="slider-label">创意</span>
                  </div>
                  <div class="form-hint">{{ temperatureHint }}</div>
                </div>
              </n-form-item>
              <n-form-item label="回答长度">
                <div class="form-field-wrap">
                  <n-select v-model:value="configForm.maxTokens" :options="maxTokensOptions" style="width: 100%;" />
                  <div class="form-hint">AI 单次回答的最大字数限制</div>
                </div>
              </n-form-item>
            </n-collapse-item>
          </n-collapse>
        </n-form>
        
        <div class="config-modal-footer">
          <n-button @click="showConfigModal = false">取消</n-button>
          <n-button :loading="savingConfig" @click="saveConfig(false)">仅保存</n-button>
          <n-button type="primary" :loading="savingConfig" @click="saveConfig(true)">
            {{ editingProvider === activeProvider ? '保存配置' : '保存并使用' }}
          </n-button>
        </div>
      </div>
    </n-modal>
    
    <!-- 创建报表弹窗（增强版） -->
    <CreateReportModal
      v-model:show="showReportModal"
      :data-source-id="selectedDataSourceId"
      :initial-sql="reportInitialSql"
      :available-sqls="availableSqls"
      :menu-tree-options="menuTreeOptions"
      @success="handleReportCreated"
    />
    
    <!-- 创建ETL流程弹窗 -->
    <n-modal v-model:show="showEtlModal" preset="card" title="🔄 创建ETL流程" style="width: 600px;">
      <n-alert type="info" style="margin-bottom: 16px;">
        AI已为您生成ETL配置，请确认后创建流程
      </n-alert>
      <n-form label-placement="left" label-width="100">
        <n-grid :cols="2" :x-gap="16">
          <n-gi :span="2">
            <n-form-item label="任务名称">
              <n-input v-model:value="etlForm.taskName" placeholder="输入任务名称" />
            </n-form-item>
          </n-gi>
          <n-gi :span="2">
            <n-form-item label="任务描述">
              <n-input v-model:value="etlForm.description" type="textarea" rows="2" placeholder="描述任务用途" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="源数据源">
              <n-select v-model:value="etlForm.sourceDataSourceId" :options="dataSourceOptions" placeholder="选择源数据源" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="源表">
              <n-input v-model:value="etlForm.sourceTable" placeholder="源表名" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="目标数据源">
              <n-select v-model:value="etlForm.targetDataSourceId" :options="dataSourceOptions" placeholder="选择目标数据源" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="目标表">
              <n-input v-model:value="etlForm.targetTable" placeholder="目标表名" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="同步方式">
              <n-select v-model:value="etlForm.syncType" :options="syncTypeOptions" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="调度方式">
              <n-select v-model:value="etlForm.scheduleType" :options="scheduleTypeOptions" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="etlForm.scheduleType === 1" :span="2">
            <n-form-item label="Cron表达式">
              <n-input v-model:value="etlForm.cronExpression" placeholder="如: 0 0 2 * * ? (每天凌晨2点)" />
            </n-form-item>
          </n-gi>
          <n-gi :span="2">
            <n-form-item label="SQL脚本">
              <n-input v-model:value="etlForm.sqlScript" type="textarea" rows="4" placeholder="数据同步SQL" />
            </n-form-item>
          </n-gi>
          <n-gi v-if="etlForm.createTargetTableSql" :span="2">
            <n-form-item label="建表语句">
              <n-input v-model:value="etlForm.createTargetTableSql" type="textarea" rows="3" placeholder="目标表建表SQL（可选）" />
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 12px;">
          <n-button @click="showEtlModal = false">取消</n-button>
          <n-button type="primary" :loading="creatingEtl" @click="createEtlFromAi">
            创建ETL流程
          </n-button>
        </div>
      </template>
    </n-modal>
    
    <!-- 创建图表弹窗（增强版） -->
    <CreateChartModal
      v-model:show="showChartModal"
      :data-source-id="selectedDataSourceId"
      :initial-sql="chartInitialSql"
      :available-sqls="availableSqls"
      :menu-tree-options="menuTreeOptions"
      @success="handleChartCreated"
    />
  </div>
  
  <!-- 显示按钮（当隐藏时） -->
  <div v-else class="ai-show-btn" title="显示AI助手" @click="showFab = true">
    <n-icon size="14"><SparklesOutline /></n-icon>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import {
  SparklesOutline,
  CodeOutline,
  AnalyticsOutline,
  ChatbubblesOutline,
  PersonOutline,
  SendOutline,
  CloseOutline,
  SettingsOutline,
  SyncOutline,
  ServerOutline,
  SearchOutline,
  CopyOutline,
  RefreshOutline,
  ChevronDownOutline,
  ChevronUpOutline,
  GridOutline as TableOutline,
  InformationCircleOutline,
  PlayOutline,
  AddCircleOutline,
  MedkitOutline,
  BookOutline,
  DocumentTextOutline,
  GitBranchOutline,
  StatsChartOutline,
  BulbOutline,
  ExpandOutline,
  ContractOutline,
  DownloadOutline,
  DocumentOutline,
  BarChartOutline,
  CloudUploadOutline,
  StopCircleOutline,
  TrashOutline
} from '@vicons/ionicons5'
import request from '@/api/request'
import { useUserStore } from '@/stores/user'
import { useAiConfigStore } from '@/stores/aiConfig'
import CreateReportModal from './components/CreateReportModal.vue'
import CreateChartModal from './components/CreateChartModal.vue'

const message = useMessage()
const userStore = useUserStore()
const aiConfigStore = useAiConfigStore()

// 获取当前用户ID
const currentUserId = computed(() => userStore.userInfo?.id || 0)

// AI助手权限检查 - 需要 ai:assistant 权限才能访问
const hasAiPermission = computed(() => {
  // 管理员拥有所有权限
  if (userStore.isAdmin()) return true
  // 检查是否有AI助手权限
  return userStore.hasPermission('ai:assistant')
})

// 基础状态
const showFab = ref(true)
const showDrawer = ref(false)
const showConfigModal = ref(false)
const showContextDetail = ref(false)
const expandedTable = ref<string | null>(null)
const showHistoryPanel = ref(false) // 默认收起历史面板
const sessionList = ref<any[]>([])
const loadingSessions = ref(false)
const isFullscreen = ref(false) // 全屏模式

// 切换表展开状态
const toggleTableExpand = (tableName: string) => {
  expandedTable.value = expandedTable.value === tableName ? null : tableName
}

// 创建报表相关（增强版）
const showReportModal = ref(false)
const reportInitialSql = ref('')
const menuTree = ref<any[]>([])

// 创建图表相关（增强版）
const showChartModal = ref(false)
const chartInitialSql = ref('')

// 报表创建成功回调
const handleReportCreated = (data: { reportId: number; reportCode: string; menuId?: number }) => {
  chatMessages.value.push({
    role: 'assistant',
    content: `✅ **报表创建成功！**\n\n- 报表ID：${data.reportId}\n- 报表编码：${data.reportCode}\n${data.menuId ? `- 菜单ID：${data.menuId}\n` : ''}\n刷新页面后可在左侧菜单中看到新创建的报表。`
  })
  scrollToBottom()
}

// 图表创建成功回调
const handleChartCreated = (data: { chartId: number; chartCode: string; menuId?: number }) => {
  chatMessages.value.push({
    role: 'assistant',
    content: `✅ **图表创建成功！**\n\n- 图表ID：${data.chartId}\n- 图表编码：${data.chartCode}\n${data.menuId ? `- 菜单ID：${data.menuId}\n` : ''}\n刷新页面后可在左侧菜单中看到新创建的图表。`
  })
  scrollToBottom()
}

// 文件上传相关
const uploadedFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    const file = input.files[0]!
    const maxSize = 10 * 1024 * 1024 // 10MB
    if (file.size > maxSize) {
      message.error('文件大小不能超过10MB')
      return
    }
    uploadedFile.value = file
    message.success(`已选择文件: ${file.name}`)
  }
}

// 清除已上传文件
const clearUploadedFile = () => {
  uploadedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

// 加载菜单树
const loadMenuTree = async () => {
  try {
    const res: any = await request.get('/menu/tree')
    if (res?.data) {
      menuTree.value = res.data
    }
  } catch (e) {
    console.error('加载菜单树失败', e)
  }
}

// 菜单树选项（只显示目录类型）
const menuTreeOptions = computed(() => {
  const filterDirs = (menus: any[]): any[] => {
    return menus
      .filter((m: any) => m.menuType === 'directory' || m.children?.length > 0)
      .map((m: any) => ({
        id: m.id,
        label: m.menuName || m.name,
        children: m.children ? filterDirs(m.children) : []
      }))
  }
  return filterDirs(menuTree.value)
})

// 可选SQL列表（当AI返回多个SQL时使用）
const availableSqls = ref<string[]>([])

// 显示创建图表弹窗（增强版）
const showCreateChartModal = (content: string) => {
  const sqls = extractAllSqlFromMessage(content)
  if (sqls.length > 0) {
    // 清理所有SQL
    const cleanedSqls = sqls.map(sql => {
      let cleanSql = sql.replace(/--[^\r\n]*/g, '').replace(/\/\*[\s\S]*?\*\//g, '').trim()
      if (cleanSql.endsWith(';')) {
        cleanSql = cleanSql.slice(0, -1).trim()
      }
      return cleanSql
    }).filter(sql => sql.length > 0)
    
    availableSqls.value = cleanedSqls
    // 默认使用最后一个SQL（通常是最终优化版本）
    chartInitialSql.value = cleanedSqls[cleanedSqls.length - 1] ?? ''
    loadMenuTree()
    showChartModal.value = true
  }
}

// 显示创建报表弹窗（增强版）
const showCreateReportModal = (content: string) => {
  const sqls = extractAllSqlFromMessage(content)
  if (sqls.length > 0) {
    // 清理所有SQL
    const cleanedSqls = sqls.map(sql => {
      let cleanSql = sql.replace(/--[^\r\n]*/g, '').replace(/\/\*[\s\S]*?\*\//g, '').trim()
      if (cleanSql.endsWith(';')) {
        cleanSql = cleanSql.slice(0, -1).trim()
      }
      return cleanSql
    }).filter(sql => sql.length > 0)
    
    availableSqls.value = cleanedSqls
    // 默认使用最后一个SQL（通常是最终优化版本）
    reportInitialSql.value = cleanedSqls[cleanedSqls.length - 1] ?? ''
    loadMenuTree()
    showReportModal.value = true
  }
}

// 创建ETL流程相关
const showEtlModal = ref(false)
const creatingEtl = ref(false)
const etlForm = reactive({
  taskName: '',
  description: '',
  sourceDataSourceId: null as number | null,
  sourceTable: '',
  targetDataSourceId: null as number | null,
  targetTable: '',
  syncType: 'FULL',
  scheduleType: 0,
  cronExpression: '',
  sqlScript: '',
  createTargetTableSql: ''
})

// 同步方式选项
const syncTypeOptions = [
  { label: '全量同步', value: 'FULL' },
  { label: '增量同步', value: 'INCREMENTAL' },
  { label: '先清空再插入', value: 'TRUNCATE_AND_INSERT' }
]

// 调度方式选项
const scheduleTypeOptions = [
  { label: '手动执行', value: 0 },
  { label: '定时执行', value: 1 }
]

// 数据源选项
const dataSourceOptions = computed(() => {
  return systemContext.dataSources.map((ds: any) => ({
    label: `${ds.name || ds.dataSourceName} (${ds.dbType || ''})`,
    value: ds.id
  }))
})

// 从AI消息中提取ETL配置
const extractEtlConfig = (content: string): any | null => {
  // 匹配JSON代码块
  const jsonMatch = content.match(/```json\s*\n?([\s\S]*?)```/i)
  if (!jsonMatch) return null
  
  try {
    const jsonStr = (jsonMatch[1] ?? '').trim()
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '\t')
    
    const rawConfig = JSON.parse(jsonStr)
    
    // 支持两种格式：
    // 1. 嵌套格式: { taskConfig: { name, source, target, sync } }
    // 2. 扁平格式: { taskName, sourceTable, syncType }
    
    if (rawConfig.taskConfig) {
      // 嵌套格式 - 转换为统一格式
      const tc = rawConfig.taskConfig
      return {
        taskName: tc.name,
        description: tc.description,
        sourceDataSourceId: tc.source?.dataSourceId,
        sourceDataSourceName: tc.source?.dataSourceName,
        sourceTable: tc.source?.tableName,
        targetDataSourceId: tc.target?.dataSourceId,
        targetDataSourceName: tc.target?.dataSourceName,
        targetTable: tc.target?.tableName,
        syncType: tc.sync?.type || tc.sync?.strategy,
        syncStrategy: tc.sync?.strategy,
        cronExpression: tc.sync?.cron,
        sqlScript: tc.sync?.sql,
        createTargetTableSql: tc.target?.createTableSql,
        enabled: tc.sync?.enabled,
        fieldMappings: tc.fieldMappings
      }
    } else if (rawConfig.taskName || rawConfig.sourceTable || rawConfig.syncType) {
      // 扁平格式 - 直接返回
      return rawConfig
    }
  } catch (e) {
    // ETL配置解析失败，静默处理
    void e
  }
  
  return null
}

// 显示创建ETL弹窗
const showCreateEtlModal = (content: string) => {
  const config = extractEtlConfig(content)
  if (config) {
    // 填充表单
    etlForm.taskName = config.taskName || ''
    etlForm.description = config.description || '由AI助手生成'
    etlForm.sourceDataSourceId = config.sourceDataSourceId || selectedDataSourceId.value
    etlForm.sourceTable = config.sourceTable || ''
    etlForm.targetDataSourceId = config.targetDataSourceId || null
    etlForm.targetTable = config.targetTable || ''
    etlForm.syncType = config.syncType || 'FULL'
    etlForm.scheduleType = config.cronExpression ? 1 : 0
    etlForm.cronExpression = config.cronExpression || ''
    etlForm.sqlScript = config.sqlScript || ''
    etlForm.createTargetTableSql = config.createTargetTableSql || ''
    
    showEtlModal.value = true
  } else {
    message.warning('未找到有效的ETL配置')
  }
}

// 检查消息是否包含ETL配置
const hasEtlConfig = (content: string): boolean => {
  return extractEtlConfig(content) !== null
}

// 创建ETL流程
const createEtlFromAi = async () => {
  if (!etlForm.taskName.trim()) {
    message.warning('请输入任务名称')
    return
  }
  if (!etlForm.sourceDataSourceId) {
    message.warning('请选择源数据源')
    return
  }
  if (!etlForm.targetDataSourceId) {
    message.warning('请选择目标数据源')
    return
  }
  
  creatingEtl.value = true
  try {
    // 生成节点（包含完整字段）
    const timestamp = Date.now()
    const defaultNodeFields = {
      preTaskCodes: [],
      failStrategy: 0,
      timeoutFlag: 0,
      timeoutSeconds: 300,
      timeoutStrategy: 0,
      retryTimes: 0,
      retryInterval: 1,
      priority: 2,
      description: ''
    }
    
    const nodes = [
      {
        nodeCode: `source_${timestamp}`,
        nodeName: `读取源表: ${etlForm.sourceTable}`,
        nodeType: 'data',
        nodeConfig: JSON.stringify({
          dataSourceId: etlForm.sourceDataSourceId,
          tableName: etlForm.sourceTable,
          operationType: 'read'
        }),
        positionX: 100,
        positionY: 150,
        sortOrder: 0,
        isEnabled: 1,
        ...defaultNodeFields
      },
      {
        nodeCode: `transform_${timestamp}`,
        nodeName: '数据转换',
        nodeType: 'script',
        nodeConfig: JSON.stringify({
          scriptType: 'sql',
          dataSourceId: etlForm.targetDataSourceId,
          script: etlForm.sqlScript || `SELECT * FROM ${etlForm.sourceTable}`,
          createTableSql: etlForm.createTargetTableSql
        }),
        positionX: 350,
        positionY: 150,
        sortOrder: 1,
        isEnabled: 1,
        ...defaultNodeFields
      },
      {
        nodeCode: `target_${timestamp}`,
        nodeName: `写入目标表: ${etlForm.targetTable}`,
        nodeType: 'data',
        nodeConfig: JSON.stringify({
          dataSourceId: etlForm.targetDataSourceId,
          tableName: etlForm.targetTable,
          operationType: 'write',
          syncType: etlForm.syncType
        }),
        positionX: 600,
        positionY: 150,
        sortOrder: 2,
        isEnabled: 1,
        ...defaultNodeFields
      }
    ]
    
    // 生成连线
    const edges = [
      { source: `source_${timestamp}`, target: `transform_${timestamp}` },
      { source: `transform_${timestamp}`, target: `target_${timestamp}` }
    ]
    
    // 创建Pipeline
    const pipelineData = {
      pipelineName: etlForm.taskName,
      pipelineCode: 'ETL_' + timestamp,
      pipelineType: 3, // 数据同步
      scheduleType: etlForm.scheduleType,
      cronExpression: etlForm.cronExpression,
      timeoutSeconds: 3600,
      retryCount: 0,
      pipelineDesc: etlForm.description,
      pipelineStatus: 1, // 已发布
      // flowJson包含完整的流程设计
      flowJson: JSON.stringify({
        nodes: nodes,
        edges: edges,
        globalParams: [],
        config: {
          sourceDataSourceId: etlForm.sourceDataSourceId,
          sourceTable: etlForm.sourceTable,
          targetDataSourceId: etlForm.targetDataSourceId,
          targetTable: etlForm.targetTable,
          syncType: etlForm.syncType
        }
      })
    }
    
    const res: any = await request.post('/pipeline', pipelineData)
    
    if (res?.code === 200 || res?.data) {
      // 获取创建的Pipeline ID
      const pipelineId = res.data?.id || res.data
      
      // 保存节点设计到design接口
      if (pipelineId) {
        await request.post(`/pipeline/${pipelineId}/design`, {
          flowJson: pipelineData.flowJson,
          nodes: nodes
        })
      }
      
      message.success('ETL流程创建成功！')
      showEtlModal.value = false
      
      // 在对话中添加成功消息
      chatMessages.value.push({
        role: 'assistant',
        content: `✅ **ETL流程创建成功！**\n\n- 任务名称：${etlForm.taskName}\n- 源表：${etlForm.sourceTable}\n- 目标表：${etlForm.targetTable}\n- 同步方式：${etlForm.syncType}\n\n可以在 **流程管理** 页面查看和管理此流程。`
      })
      scrollToBottom()
    } else {
      message.error(res?.msg || '创建失败')
    }
  } catch (e: any) {
    message.error('创建失败：' + (e.message || '网络错误'))
  } finally {
    creatingEtl.value = false
  }
}

const loading = ref(false)
const loadingContext = ref(false)
const savingConfig = ref(false)
const testingConnection = ref(false)
const inputText = ref('')
const chatMessages = ref<Array<{ role: string; content: string; sqlResult?: any; executing?: boolean; sqlError?: string; timestamp?: number }>>([])

// AbortController for cancelling AI requests
let currentAbortController: AbortController | null = null

const abortRequest = () => {
  if (currentAbortController) {
    currentAbortController.abort()
    currentAbortController = null
  }
  loading.value = false
  chatMessages.value.push({ role: 'assistant', content: '⏹️ 已停止生成', timestamp: Date.now() })
  scrollToBottom()
}
const messagesRef = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const sessionId = ref<string | null>(null)

// 自动调整输入框高度（最小60px，最大200px）
const autoResize = () => {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto' // 先收缩以获取真实 scrollHeight
    const scrollHeight = el.scrollHeight
    // 限制高度：最小60px，最大200px（约8行）
    el.style.height = Math.min(Math.max(scrollHeight, 60), 200) + 'px'
  }
}

// AI状态
const aiStatus = ref({
  enabled: false,
  provider: ''
})

// AI 状态显示标签（服务商 + 模型）
const providerLabelMap: Record<string, string> = {
  openai: 'OpenAI',
  qwen: '通义千问',
  deepseek: 'DeepSeek',
  'deepseek-local': 'DeepSeek本地',
  deepseekLocal: 'DeepSeek本地',
  ollama: 'Ollama',
  claude: 'Claude',
  gemini: 'Gemini',
  zhipu: '智谱GLM',
  wenxin: '百度文心',
  spark: '讯飞星火'
}
const aiDisplayLabel = computed(() => {
  if (!aiStatus.value.enabled) return '未配置'
  const p = activeProvider.value
  const pLabel = providerLabelMap[p] || p || ''
  const mLabel = providerConfigs[p]?.model || ''
  if (pLabel && mLabel) return `${pLabel} / ${mLabel}`
  return pLabel || '已配置'
})

// 系统上下文
const systemContext = reactive({
  dataSources: [] as any[],
  currentTable: '',
  tableStructure: '',
  fullContext: null as any
})

// 数据源选择
const selectedDataSourceId = ref<number | null>(null)
const selectedTableName = ref<string | null>(null)

// 当前数据源的表列表
const currentDataSourceTables = computed(() => {
  if (!selectedDataSourceId.value) return []
  const ds = systemContext.dataSources.find((d: any) => d.id === selectedDataSourceId.value)
  return ds?.tables || []
})

// 表选择选项
const tableSelectOptions = computed(() => {
  return currentDataSourceTables.value.map((t: any) => ({
    label: t.name || t.tableName,
    value: t.name || t.tableName
  }))
})

// 获取数据源名称
const getDataSourceName = (dsId: number) => {
  const ds = systemContext.dataSources.find((d: any) => d.id === dsId)
  return ds?.name || `数据源${dsId}`
}

// 清除数据源选择
const clearDataSource = () => {
  selectedDataSourceId.value = null
  selectedTableName.value = null
}

// 清除表选择
const clearTable = () => {
  selectedTableName.value = null
}

// 动态placeholder
const inputPlaceholder = computed(() => {
  if (selectedTableName.value) {
    return `针对表 ${selectedTableName.value} 提问...`
  } else if (selectedDataSourceId.value) {
    return `针对 ${getDataSourceName(selectedDataSourceId.value)} 提问...`
  }
  return '发消息或输入 / 选择技能...'
})

// 选择表时，如果该表没有字段信息则按需加载
const onTableSelect = async (tableName: string | null) => {
  if (!tableName || !selectedDataSourceId.value) return
  const ds = systemContext.dataSources.find((d: any) => d.id === selectedDataSourceId.value)
  if (!ds?.tables) return
  const tableInfo = ds.tables.find((t: any) => (t.tableName || t.name) === tableName)
  if (tableInfo && !tableInfo.columns) {
    try {
      const res: any = await request.get(`/data-source/${selectedDataSourceId.value}/tables/${tableName}/columns`)
      const cols = res?.data || res || []
      if (Array.isArray(cols)) {
        tableInfo.columns = cols
      }
    } catch (e) {
      console.warn('按需加载表结构失败:', e)
    }
  }
}

// 选择数据源时加载表结构
const onDataSourceSelect = (dsId: number | null) => {
  if (dsId) {
    loadSystemContext(dsId)
    selectedTableName.value = null
  }
}

// 斜杠命令定义
const slashCommands = [
  // 创建类命令
  { command: '/sql', icon: '📝', description: '生成SQL查询语句', template: '请帮我编写SQL：', category: 'create' },
  { command: '/report', icon: '📈', description: '设计数据报表', template: '请帮我设计一个报表，需求：', category: 'create' },
  { command: '/chart', icon: '📊', description: '创建数据图表', template: '请帮我创建一个图表，展示：', category: 'create' },
  { command: '/etl', icon: '🔄', description: '设计数据同步任务', template: '请帮我设计数据同步方案，源表：目标表：', category: 'create' },
  { command: '/table', icon: '📋', description: '设计表结构', template: '请帮我设计表结构，需求：', category: 'create' },
  
  // 分析类命令
  { command: '/analyze', icon: '🔬', description: '分析数据特征', template: '请帮我分析这些数据的特征和分布：', category: 'analyze' },
  { command: '/quality', icon: '✅', description: '数据质量检查', template: '请帮我检查数据质量问题：', category: 'analyze' },
  { command: '/trend', icon: '📉', description: '趋势分析', template: '请帮我分析数据趋势，时间字段：指标字段：', category: 'analyze' },
  
  // 优化类命令
  { command: '/optimize', icon: '⚡', description: '优化SQL性能', template: '请帮我优化这段SQL的性能：\n```sql\n\n```', category: 'optimize' },
  { command: '/index', icon: '🔍', description: '索引优化建议', template: '请帮我分析表的索引优化建议，表名：', category: 'optimize' },
  { command: '/slow', icon: '🐢', description: '慢查询分析', template: '请帮我分析这个慢查询的原因：\n```sql\n\n```', category: 'optimize' },
  
  // 解释类命令
  { command: '/explain', icon: '💡', description: '解释SQL语句', template: '请帮我解释这段SQL的含义和逻辑：\n```sql\n\n```', category: 'explain' },
  { command: '/diff', icon: '🔀', description: '对比两个SQL', template: '请帮我对比分析这两个SQL的区别：\n```sql\n-- SQL 1\n\n-- SQL 2\n\n```', category: 'explain' },
  
  // 快捷模板
  { command: '/join', icon: '🔗', description: 'JOIN语句模板', template: '请帮我编写多表关联查询，表：', category: 'template' },
  { command: '/agg', icon: '🧮', description: '聚合统计模板', template: '请帮我编写聚合统计SQL，按照___分组，统计___', category: 'template' },
  { command: '/page', icon: '📄', description: '分页查询模板', template: '请帮我编写分页查询SQL，表名：', category: 'template' },
  
  // 帮助
  { command: '/help', icon: '❓', description: '查看所有命令', template: '', category: 'help' }
]

// 斜杠命令状态
const showSlashMenu = ref(false)
const selectedCommandIndex = ref(0)

// 过滤后的命令列表
const filteredCommands = computed(() => {
  if (!inputText.value.startsWith('/')) return []
  const query = inputText.value.toLowerCase()
  return slashCommands.filter(cmd => cmd.command.includes(query))
})

// 处理输入
const handleInput = () => {
  autoResize()
  // 检测斜杠命令
  if (inputText.value.startsWith('/') && !inputText.value.includes(' ')) {
    showSlashMenu.value = filteredCommands.value.length > 0
    selectedCommandIndex.value = 0
  } else {
    showSlashMenu.value = false
  }
}

// 导航命令
const navigateCommand = (delta: number) => {
  if (!showSlashMenu.value) return
  const len = filteredCommands.value.length
  selectedCommandIndex.value = (selectedCommandIndex.value + delta + len) % len
}

// 确认选择命令
const confirmCommand = () => {
  if (showSlashMenu.value && filteredCommands.value.length > 0) {
    selectCommand(filteredCommands.value[selectedCommandIndex.value])
  }
}

// 选择命令
const selectCommand = (cmd: any) => {
  // 处理帮助命令
  if (cmd.command === '/help') {
    showSlashMenu.value = false
    inputText.value = ''
    // 显示所有命令帮助
    const helpContent = generateHelpContent()
    chatMessages.value.push({
      role: 'assistant',
      content: helpContent
    })
    scrollToBottom()
    return
  }
  
  inputText.value = cmd.template
  showSlashMenu.value = false
  textareaRef.value?.focus()
}

// 生成帮助内容
const generateHelpContent = () => {
  const categories: Record<string, string> = {
    'create': '📝 **创建类命令**',
    'analyze': '🔬 **分析类命令**',
    'optimize': '⚡ **优化类命令**',
    'explain': '💡 **解释类命令**',
    'template': '📋 **快捷模板**'
  }
  
  let content = '# 🚀 AI助手命令帮助\n\n输入 `/` 开头的命令可快速执行常用操作：\n\n'
  
  const groupedCmds: Record<string, typeof slashCommands> = {}
  slashCommands.forEach(cmd => {
    if (cmd.category !== 'help') {
      if (!groupedCmds[cmd.category]) groupedCmds[cmd.category] = []
      groupedCmds[cmd.category]!.push(cmd)
    }
  })
  
  Object.entries(groupedCmds).forEach(([cat, cmds]) => {
    content += `${categories[cat] || cat}\n\n`
    cmds.forEach(cmd => {
      content += `- \`${cmd.command}\` ${cmd.icon} ${cmd.description}\n`
    })
    content += '\n'
  })
  
  content += '---\n\n💡 **使用技巧：**\n'
  content += '- 输入 `/` 后可用方向键选择命令，Tab或Enter确认\n'
  content += '- 选择数据源后，AI可自动获取表结构辅助生成SQL\n'
  content += '- 点击AI回复中的SQL代码块可直接执行或创建报表/图表\n'
  
  return content
}

// 处理回车键
const handleEnterKey = (e: KeyboardEvent) => {
  if (showSlashMenu.value) {
    e.preventDefault()
    confirmCommand()
  } else if (!e.shiftKey) {
    // Enter 发送消息，Shift+Enter 换行
    e.preventDefault()
    sendMessage()
  }
}

// 处理Escape键 - 关闭斜杠菜单
const handleEscapeKey = () => {
  if (showSlashMenu.value) {
    showSlashMenu.value = false
  }
}

// 格式化消息时间
const formatMsgTime = (ts: number) => {
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 从消息中提取SQL语句（返回第一个SQL）
const extractSqlFromMessage = (content: string): string | null => {
  const sqlMatch = content.match(/```sql\n([\s\S]*?)```/i)
  return sqlMatch ? (sqlMatch[1] ?? '').trim() : null
}

// 从消息中提取所有SQL语句（用于多选）
const extractAllSqlFromMessage = (content: string): string[] => {
  const sqlMatches = content.matchAll(/```sql\n([\s\S]*?)```/gi)
  const sqls: string[] = []
  for (const match of sqlMatches) {
    const sql = (match[1] ?? '').trim()
    if (sql) sqls.push(sql)
  }
  return sqls
}

// SQL安全检查
const validateSqlSafety = (sql: string): { safe: boolean; warning?: string } => {
  const upperSql = sql.toUpperCase().trim()
  
  // 危险操作检测
  const dangerousPatterns = [
    { pattern: /^\s*DROP\s+/i, msg: '检测到DROP语句，可能会删除表或数据库' },
    { pattern: /^\s*TRUNCATE\s+/i, msg: '检测到TRUNCATE语句，将清空整个表' },
    { pattern: /^\s*DELETE\s+(?!.*WHERE)/i, msg: '检测到DELETE语句无WHERE条件，可能删除所有数据' },
    { pattern: /^\s*UPDATE\s+(?!.*WHERE)/i, msg: '检测到UPDATE语句无WHERE条件，可能更新所有数据' },
    { pattern: /;\s*(DROP|TRUNCATE|DELETE|UPDATE)/i, msg: '检测到多语句执行，存在风险' }
  ]
  
  for (const { pattern, msg } of dangerousPatterns) {
    if (pattern.test(upperSql)) {
      return { safe: false, warning: msg }
    }
  }
  
  // 警告类操作（允许执行但提示）
  if (/^\s*(INSERT|UPDATE|DELETE)/i.test(upperSql)) {
    return { safe: true, warning: '此操作将修改数据，请确认后执行' }
  }
  
  return { safe: true }
}

// 执行消息中的SQL
const executeSqlFromMessage = async (content: string, msgIndex: number) => {
  const sql = extractSqlFromMessage(content)
  if (!sql) {
    message.warning('未找到可执行的SQL语句')
    return
  }
  if (!selectedDataSourceId.value) {
    message.warning('请先选择数据源')
    return
  }
  
  // 安全检查
  const { safe, warning } = validateSqlSafety(sql)
  if (!safe) {
    message.error(`⚠️ 安全警告：${warning}，已阻止执行`)
    return
  }
  if (warning) {
    message.warning(warning)
  }
  
  const msgRef = chatMessages.value[msgIndex]
  if (!msgRef) return
  msgRef.executing = true
  
  try {
    const res: any = await request.post('/ai/execute-sql', {
      dataSourceId: selectedDataSourceId.value,
      sql: sql,
      limit: 100
    })
    
    if (res?.data?.success) {
      msgRef.sqlResult = {
        columns: res.data.columns,
        data: res.data.data,
        rowCount: res.data.rowCount,
        sql: res.data.sql,
        executionTime: res.data.executionTime
      }
      const timeInfo = res.data.executionTime ? ` (${res.data.executionTime}ms)` : ''
      message.success(`✅ 查询成功，返回 ${res.data.rowCount} 条数据${timeInfo}`)
    } else {
      // 智能错误提示
      const errorMsg = res?.data?.error || res?.msg || '未知错误'
      msgRef.sqlError = errorMsg
      message.error('执行失败：' + errorMsg)
      
      // 添加错误修复建议到对话
      if (errorMsg.includes('doesn\'t exist') || errorMsg.includes('不存在')) {
        chatMessages.value.push({
          role: 'assistant',
          content: `❌ **SQL执行错误**\n\n\`${errorMsg}\`\n\n**可能原因：**\n- 表名或字段名拼写错误\n- 表不存在于当前数据源\n\n**建议：** 请检查表名是否正确，或选择正确的数据源。`
        })
        scrollToBottom()
      }
    }
  } catch (e: any) {
    const errorMsg = e.message || '网络错误'
    msgRef.sqlError = errorMsg
    message.error('执行失败：' + errorMsg)
  } finally {
    msgRef.executing = false
  }
}

// 导出SQL查询结果
const exportSqlResult = (sqlResult: any, format: 'csv' | 'excel') => {
  if (!sqlResult || !sqlResult.data || sqlResult.data.length === 0) {
    message.warning('没有数据可导出')
    return
  }
  
  const columns = sqlResult.columns || []
  const data = sqlResult.data || []
  
  if (format === 'csv') {
    // 导出CSV
    const csvContent = [
      columns.join(','),
      ...data.map((row: any) => columns.map((col: string) => {
        const val = row[col]
        if (val === null || val === undefined) return ''
        const str = String(val)
        // 如果包含逗号、引号或换行，需要用引号包裹
        if (str.includes(',') || str.includes('"') || str.includes('\n')) {
          return `"${str.replace(/"/g, '""')}"`
        }
        return str
      }).join(','))
    ].join('\n')
    
    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `query_result_${Date.now()}.csv`
    link.click()
    URL.revokeObjectURL(url)
    message.success('CSV导出成功')
  } else {
    // 导出Excel (简单HTML表格格式)
    const tableHtml = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel">
      <head><meta charset="UTF-8"></head>
      <body>
        <table border="1">
          <thead><tr>${columns.map((col: string) => `<th>${col}</th>`).join('')}</tr></thead>
          <tbody>${data.map((row: any) => `<tr>${columns.map((col: string) => `<td>${row[col] ?? ''}</td>`).join('')}</tr>`).join('')}</tbody>
        </table>
      </body>
      </html>
    `
    const blob = new Blob([tableHtml], { type: 'application/vnd.ms-excel;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `query_result_${Date.now()}.xls`
    link.click()
    URL.revokeObjectURL(url)
    message.success('Excel导出成功')
  }
}

// ========== 多服务商独立配置 ==========

// 当前正在编辑的服务商（标签页）
const editingProvider = ref('deepseek')
// 当前实际使用的服务商
const activeProvider = ref('')
// 各服务商缓存（后端返回的脱敏数据）
const providerConfigs = reactive<Record<string, { apiKey: string; maskedApiKey: string; baseUrl: string; model: string; configured: boolean }>>({})

// 当前编辑表单
const configForm = reactive({
  apiKey: '',
  baseUrl: '',
  model: 'deepseek-v4-flash',
  maxHistory: 5,
  temperature: 0.7,
  maxTokens: 4096
})

// 服务商默认值
const providerDefaults: Record<string, { url: string; model: string }> = {
  openai: { url: '', model: 'gpt-4o' },
  claude: { url: '', model: 'claude-sonnet-4-20250514' },
  gemini: { url: '', model: 'gemini-2.5-flash' },
  qwen: { url: '', model: 'qwen-max' },
  deepseek: { url: 'https://api.deepseek.com', model: 'deepseek-v4-flash' },
  zhipu: { url: '', model: 'glm-4-plus' },
  'deepseek-local': { url: 'http://localhost:8080/v1', model: 'deepseek-v4-flash' },
  ollama: { url: 'http://localhost:11434', model: 'llama3.2' }
}

// 服务商选项
const providerOptions = [
  { label: 'OpenAI', value: 'openai', icon: '🤖' },
  { label: 'Claude', value: 'claude', icon: '🟠' },
  { label: 'Gemini', value: 'gemini', icon: '💎' },
  { label: '通义千问', value: 'qwen', icon: '🧠' },
  { label: 'DeepSeek', value: 'deepseek', icon: '🔮' },
  { label: '智谱GLM', value: 'zhipu', icon: '🧊' },
  { label: 'DeepSeek本地', value: 'deepseek-local', icon: '🏠' },
  { label: 'Ollama', value: 'ollama', icon: '🦙' }
]

// 切换编辑标签（不影响当前使用的服务商）
const switchEditingProvider = (provider: string) => {
  // 先保存当前编辑到缓存
  saveFormToCache(editingProvider.value)
  // 切换到新标签
  editingProvider.value = provider
  // 从缓存加载到表单
  loadFormFromCache(provider)
}

// 保存表单到缓存
const saveFormToCache = (provider: string) => {
  if (!providerConfigs[provider]) {
    providerConfigs[provider] = { apiKey: '', maskedApiKey: '', baseUrl: '', model: '', configured: false }
  }
  providerConfigs[provider].apiKey = configForm.apiKey
  providerConfigs[provider].baseUrl = configForm.baseUrl
  providerConfigs[provider].model = configForm.model
}

// 从缓存加载到表单
const loadFormFromCache = (provider: string) => {
  const cached = providerConfigs[provider]
  if (cached && (cached.model || cached.baseUrl || cached.apiKey)) {
    configForm.apiKey = cached.apiKey
    configForm.baseUrl = cached.baseUrl
    configForm.model = cached.model
  } else {
    // 无缓存，使用默认值
    const def = providerDefaults[provider] || { url: '', model: '' }
    configForm.apiKey = ''
    configForm.baseUrl = def.url
    configForm.model = def.model
  }
}

// 模型选项
const modelOptions = computed(() => {
  const models: Record<string, any[]> = {
    openai: [
      { label: 'GPT-4o (推荐)', value: 'gpt-4o' },
      { label: 'GPT-4o Mini', value: 'gpt-4o-mini' },
      { label: 'GPT-4.1 (最新)', value: 'gpt-4.1' },
      { label: 'GPT-4.1 Mini', value: 'gpt-4.1-mini' },
      { label: 'GPT-4.1 Nano', value: 'gpt-4.1-nano' },
      { label: 'GPT-4.5 Preview', value: 'gpt-4.5-preview' },
      { label: 'GPT-4 Turbo', value: 'gpt-4-turbo' },
      { label: 'o3 (深度推理)', value: 'o3' },
      { label: 'o3 Mini', value: 'o3-mini' },
      { label: 'o4-mini (推理)', value: 'o4-mini' },
      { label: 'o1 (推理)', value: 'o1' },
      { label: 'o1-mini', value: 'o1-mini' },
      { label: 'GPT-3.5 Turbo', value: 'gpt-3.5-turbo' }
    ],
    claude: [
      { label: 'Claude Sonnet 4 (最新推荐)', value: 'claude-sonnet-4-20250514' },
      { label: 'Claude Opus 4', value: 'claude-opus-4-20250514' },
      { label: 'Claude 3.5 Sonnet', value: 'claude-3-5-sonnet-20241022' },
      { label: 'Claude 3.5 Haiku (快速)', value: 'claude-3-5-haiku-20241022' },
      { label: 'Claude 3 Opus', value: 'claude-3-opus-20240229' },
      { label: 'Claude 3 Haiku', value: 'claude-3-haiku-20240307' }
    ],
    gemini: [
      { label: 'Gemini 2.5 Flash (推荐)', value: 'gemini-2.5-flash' },
      { label: 'Gemini 2.5 Pro', value: 'gemini-2.5-pro' },
      { label: 'Gemini 2.0 Flash', value: 'gemini-2.0-flash' },
      { label: 'Gemini 1.5 Pro', value: 'gemini-1.5-pro' },
      { label: 'Gemini 1.5 Flash', value: 'gemini-1.5-flash' }
    ],
    qwen: [
      { label: 'Qwen-Max (最强)', value: 'qwen-max' },
      { label: 'Qwen-Plus', value: 'qwen-plus' },
      { label: 'Qwen-Turbo (快速)', value: 'qwen-turbo' },
      { label: 'Qwen3-235B (开源旗舰)', value: 'qwen3-235b-a22b' },
      { label: 'Qwen3-32B', value: 'qwen3-32b' },
      { label: 'Qwen-Long (长文本)', value: 'qwen-long' },
      { label: 'Qwen-Coder-Plus', value: 'qwen-coder-plus' },
      { label: 'Qwen-Coder-Turbo', value: 'qwen-coder-turbo' },
      { label: 'Qwen-VL-Max (多模态)', value: 'qwen-vl-max' }
    ],
    deepseek: [
      { label: 'DeepSeek V4 Flash (最新推荐)', value: 'deepseek-v4-flash' },
      { label: 'DeepSeek V4 Pro (深度推理)', value: 'deepseek-v4-pro' }
    ],
    zhipu: [
      { label: 'GLM-4-Plus (推荐)', value: 'glm-4-plus' },
      { label: 'GLM-4-Long (长文本)', value: 'glm-4-long' },
      { label: 'GLM-4-Air (快速)', value: 'glm-4-air' },
      { label: 'GLM-4-AirX (极速)', value: 'glm-4-airx' },
      { label: 'GLM-4-Flash (免费)', value: 'glm-4-flash' },
      { label: 'GLM-4-FlashX', value: 'glm-4-flashx' },
      { label: 'CodeGeeX-4', value: 'codegeex-4' }
    ],
    'deepseek-local': [
      { label: 'DeepSeek V4 Flash', value: 'deepseek-v4-flash' },
      { label: 'DeepSeek V4 Pro', value: 'deepseek-v4-pro' },
      { label: '自定义模型', value: 'custom' }
    ],
    ollama: [
      { label: 'Llama 3.3 (最新)', value: 'llama3.3' },
      { label: 'Llama 3.2', value: 'llama3.2' },
      { label: 'Llama 3.1', value: 'llama3.1' },
      { label: 'Qwen3 (最新)', value: 'qwen3' },
      { label: 'Qwen 2.5', value: 'qwen2.5' },
      { label: 'Qwen 2.5 Coder', value: 'qwen2.5-coder' },
      { label: 'DeepSeek-R1', value: 'deepseek-r1' },
      { label: 'DeepSeek-V3', value: 'deepseek-v3' },
      { label: 'DeepSeek-Coder-V2', value: 'deepseek-coder-v2' },
      { label: 'Gemma 3', value: 'gemma3' },
      { label: 'Phi-4', value: 'phi4' },
      { label: 'Mistral', value: 'mistral' },
      { label: 'CodeLlama', value: 'codellama' },
      { label: '自定义模型', value: 'custom' }
    ]
  }
  return models[editingProvider.value] || []
})

// 是否是本地服务商
const isLocalProvider = computed(() => {
  return ['deepseek-local', 'ollama'].includes(editingProvider.value)
})

// 服务商提示信息
const providerTip = computed(() => {
  const tips: Record<string, { type: 'info' | 'warning' | 'success'; message: string }> = {
    openai: { type: 'info', message: '需要OpenAI API密钥，访问 platform.openai.com 获取' },
    claude: { type: 'info', message: '需要Anthropic API密钥，访问 console.anthropic.com 获取' },
    gemini: { type: 'info', message: '需要Google AI API密钥，访问 aistudio.google.com 获取' },
    qwen: { type: 'info', message: '需要通义千问API密钥，访问 dashscope.console.aliyun.com 获取' },
    deepseek: { type: 'info', message: '需要DeepSeek API密钥，访问 platform.deepseek.com 获取' },
    zhipu: { type: 'info', message: '需要智谱API密钥，访问 open.bigmodel.cn 获取' },
    'deepseek-local': { type: 'success', message: '本地/内网部署的DeepSeek服务，支持兼容OpenAI格式的API，API密钥可选' },
    ollama: { type: 'success', message: '本地Ollama服务，无需API密钥。请确保Ollama已启动并监听在指定端口' }
  }
  return tips[editingProvider.value]
})

// API地址占位符
const baseUrlPlaceholder = computed(() => {
  const placeholders: Record<string, string> = {
    openai: 'https://api.openai.com/v1（默认）',
    claude: 'https://api.anthropic.com（默认）',
    gemini: 'https://generativelanguage.googleapis.com（默认）',
    qwen: 'https://dashscope.aliyuncs.com/api/v1（默认）',
    deepseek: 'https://api.deepseek.com（默认）',
    zhipu: 'https://open.bigmodel.cn/api/paas/v4（默认）',
    'deepseek-local': '例如：http://192.168.1.100:8080/v1',
    ollama: '例如：http://192.168.1.100:11434'
  }
  return placeholders[editingProvider.value] || ''
})

// 温度描述
const temperatureHint = computed(() => {
  const t = configForm.temperature
  if (t <= 0.2) return '回答非常稳定一致，适合代码生成和SQL查询'
  if (t <= 0.5) return '回答较为稳定，偶尔有变化，适合日常使用'
  if (t <= 0.7) return '平衡稳定性和创造性，推荐默认值'
  return '回答更具创意和多样性，适合头脑风暴和文案写作'
})

// 回答长度选项
const maxTokensOptions = [
  { label: '简短回答（约 500 字）', value: 1024 },
  { label: '标准回答（约 2000 字）', value: 4096 },
  { label: '详细回答（约 4000 字）', value: 8192 },
  { label: '超长回答（约 8000 字）', value: 16384 },
  { label: '最大长度（约 16000 字）', value: 32000 }
]

// 测试连接
const testConnection = async () => {
  testingConnection.value = true
  try {
    const cached = providerConfigs[editingProvider.value]
    // 如果apiKey没改（仍是脱敏值），不发送
    const apiKeyToSend = (cached && configForm.apiKey === cached.maskedApiKey) ? '' : configForm.apiKey
    const res: any = await request.post('/ai/test-connection', {
      provider: editingProvider.value,
      apiKey: apiKeyToSend,
      baseUrl: configForm.baseUrl,
      model: configForm.model
    })
    if (res?.data?.success) {
      message.success('连接成功！' + (res.data.message || ''))
    } else {
      message.error('连接失败：' + (res?.data?.error || res?.msg || '未知错误'))
    }
  } catch (e: any) {
    message.error('测试失败：' + (e.message || '网络错误'))
  } finally {
    testingConnection.value = false
  }
}

// 隐藏悬浮按钮
const hideFab = () => {
  showFab.value = false
  message.info('AI助手已隐藏，点击右下角小图标可重新显示')
}


// HTML转义函数，防止XSS攻击
const escapeHtml = (text: string): string => {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  return text.replace(/[&<>"']/g, m => map[m] ?? m)
}

// Markdown渲染（增强版，支持代码块复制、表格、有序列表、引用、删除线）
const renderMd = (text: string) => {
  if (!text) return ''
  
  // 占位符存储，避免后续替换破坏已处理的块
  const placeholders: string[] = []
  const ph = (html: string) => {
    const idx = placeholders.length
    placeholders.push(html)
    return `\x00PH${idx}\x00`
  }
  
  let html = text

  // 1. 代码块 - 添加语言标签和复制按钮
  html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, (_match, lang, code) => {
    const langLabel = lang || 'code'
    const cleanCode = code.trim()
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '\t')
      .replace(/\\r/g, '')
      .replace(/\r\n/g, '\n')
    
    const encodedCode = btoa(unescape(encodeURIComponent(cleanCode)))
    const displayCode = escapeHtml(cleanCode)
    
    let isEtlConfig = false
    if (langLabel.toLowerCase() === 'json') {
      try {
        const parsed = JSON.parse(cleanCode)
        if (parsed.taskConfig || parsed.taskName || parsed.sourceTable || parsed.syncType) {
          isEtlConfig = true
        }
      } catch (e) { /* 忽略解析错误 */ }
    }
    
    const etlButton = isEtlConfig 
      ? `<button class="code-etl-btn" data-config="${encodedCode}">创建ETL</button>` 
      : ''
    
    return ph(`<div class="code-block">
      <div class="code-header">
        <span class="code-lang">${escapeHtml(langLabel)}</span>
        <div class="code-actions">
          ${etlButton}
          <button class="code-copy-btn" data-code="${encodedCode}">复制</button>
        </div>
      </div>
      <pre><code class="lang-${escapeHtml(langLabel)}">${displayCode}</code></pre>
    </div>`)
  })
  
  // 2. 行内代码
  html = html.replace(/`([^`]+)`/g, (_match, code) => {
    const cleanCode = code.replace(/\\n/g, ' ').replace(/\\t/g, ' ')
    return ph(`<code class="inline-code">${escapeHtml(cleanCode)}</code>`)
  })
  
  // 3. 表格
  html = html.replace(/((?:^\|.+\|[ \t]*\n)+)/gm, (tableBlock) => {
    const rows = tableBlock.trim().split('\n')
    if (rows.length < 2) return tableBlock
    // 检查第二行是否是分隔行 |---|---|
    const sepRow = rows[1] as string
    if (!/^\|[\s:]*-+[\s:]*/.test(sepRow)) return tableBlock
    
    const parseRow = (row: string) => row.replace(/^\||\|$/g, '').split('|').map(c => c.trim())
    const headers = parseRow(rows[0] as string)
    const dataRows = rows.slice(2)
    
    let tableHtml = '<div class="md-table-wrap"><table><thead><tr>'
    headers.forEach(h => { tableHtml += `<th>${h}</th>` })
    tableHtml += '</tr></thead><tbody>'
    dataRows.forEach(row => {
      if (!row.trim()) return
      const cells = parseRow(row)
      tableHtml += '<tr>'
      cells.forEach(c => { tableHtml += `<td>${c}</td>` })
      tableHtml += '</tr>'
    })
    tableHtml += '</tbody></table></div>'
    return ph(tableHtml)
  })
  
  // 4. 引用块
  html = html.replace(/(^> .+(?:\n> .+)*)/gm, (block) => {
    const content = block.replace(/^> /gm, '')
    return ph(`<blockquote>${content}</blockquote>`)
  })
  
  // 5. 标题
  html = html.replace(/^#### (.+)$/gm, (_m, t) => ph(`<h5>${t}</h5>`))
  html = html.replace(/^### (.+)$/gm, (_m, t) => ph(`<h4>${t}</h4>`))
  html = html.replace(/^## (.+)$/gm, (_m, t) => ph(`<h3>${t}</h3>`))
  html = html.replace(/^# (.+)$/gm, (_m, t) => ph(`<h2>${t}</h2>`))
  
  // 6. 有序列表
  html = html.replace(/((?:^\d+\. .+\n?)+)/gm, (block) => {
    const items = block.trim().split('\n')
    let listHtml = '<ol>'
    items.forEach(item => {
      const content = item.replace(/^\d+\.\s+/, '')
      listHtml += `<li>${content}</li>`
    })
    listHtml += '</ol>'
    return ph(listHtml)
  })
  
  // 7. 无序列表
  html = html.replace(/((?:^[-*] .+\n?)+)/gm, (block) => {
    const items = block.trim().split('\n')
    let listHtml = '<ul>'
    items.forEach(item => {
      const content = item.replace(/^[-*]\s+/, '')
      listHtml += `<li>${content}</li>`
    })
    listHtml += '</ul>'
    return ph(listHtml)
  })
  
  // 8. 分隔线
  html = html.replace(/^---+$/gm, () => ph('<hr>'))
  
  // 9. 行内格式
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
  html = html.replace(/~~([^~]+)~~/g, '<del>$1</del>')
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
  
  // 10. 换行（仅对非块级内容）
  html = html.replace(/\n/g, '<br>')
  
  // 11. 还原占位符
  html = html.replace(/\x00PH(\d+)\x00/g, (_m, idx) => placeholders[parseInt(idx || '0')] || '')
  
  return html
}

// 处理代码块按钮点击（复制和创建ETL）
const handleCodeCopy = (event: Event) => {
  const target = event.target as HTMLElement
  
  // 处理复制按钮
  if (target.classList.contains('code-copy-btn')) {
    const encodedCode = target.getAttribute('data-code')
    if (encodedCode) {
      try {
        const code = decodeURIComponent(escape(atob(encodedCode)))
        navigator.clipboard.writeText(code).then(() => {
          target.textContent = '已复制!'
          target.style.background = '#059669'
          setTimeout(() => { 
            target.textContent = '复制'
            target.style.background = '#10b981'
          }, 1500)
        }).catch((err) => {
          console.error('复制失败:', err)
          message.error('复制失败')
        })
      } catch (err) {
        console.error('解码失败:', err)
        message.error('复制失败')
      }
    }
  }
  
  // 处理创建ETL按钮
  if (target.classList.contains('code-etl-btn')) {
    const encodedConfig = target.getAttribute('data-config')
    if (encodedConfig) {
      try {
        const configStr = decodeURIComponent(escape(atob(encodedConfig)))
        const rawConfig = JSON.parse(configStr)
        
        // 转换嵌套格式为统一格式
        let config = rawConfig
        if (rawConfig.taskConfig) {
          const tc = rawConfig.taskConfig
          config = {
            taskName: tc.name,
            description: tc.description,
            sourceDataSourceId: tc.source?.dataSourceId,
            sourceTable: tc.source?.tableName,
            targetDataSourceId: tc.target?.dataSourceId,
            targetTable: tc.target?.tableName,
            syncType: tc.sync?.type || tc.sync?.strategy,
            cronExpression: tc.sync?.cron,
            sqlScript: tc.sync?.sql,
            createTargetTableSql: tc.target?.createTableSql
          }
        }
        
        // 填充ETL表单
        etlForm.taskName = config.taskName || ''
        etlForm.description = config.description || '由AI助手生成'
        etlForm.sourceDataSourceId = config.sourceDataSourceId || selectedDataSourceId.value
        etlForm.sourceTable = config.sourceTable || ''
        etlForm.targetDataSourceId = config.targetDataSourceId || null
        etlForm.targetTable = config.targetTable || ''
        etlForm.syncType = config.syncType || config.syncStrategy || 'FULL'
        etlForm.scheduleType = config.cronExpression ? 1 : 0
        etlForm.cronExpression = config.cronExpression || ''
        etlForm.sqlScript = config.sqlScript || ''
        etlForm.createTargetTableSql = config.createTargetTableSql || ''
        
        showEtlModal.value = true
      } catch (err) {
        console.error('解析ETL配置失败:', err)
        message.error('解析ETL配置失败')
      }
    }
  }
}

// 加载系统上下文（使用AI专用接口，包含表结构）
const loadSystemContext = async (dataSourceId?: number | null) => {
  loadingContext.value = true
  try {
    const params: any = {}
    if (dataSourceId) params.dataSourceId = dataSourceId
    
    const res: any = await request.get('/ai/system-context', { params, __silent: true } as any)
    if (res?.data?.success) {
      systemContext.dataSources = res.data.dataSources || []
      systemContext.fullContext = res.data
    } else {
      // 降级：使用简单的数据源列表
      const listRes: any = await request.get('/data-source/list', { params: { pageSize: 100 } })
      const list = listRes?.data?.list || listRes?.data
      if (list && Array.isArray(list)) {
        systemContext.dataSources = list.map((ds: any) => ({
          id: ds.id,
          name: ds.name,
          dbType: ds.dbType,
          database: ds.database
        }))
      }
    }
  } catch {
    systemContext.dataSources = []
  } finally {
    loadingContext.value = false
  }
}

// 构建详细的系统上下文（包含表结构信息）
const buildDetailedContext = () => {
  const ctx: any = {
    platform: 'DataTeaCup',
    currentPage: window.location.pathname
  }
  
  // 添加选中的数据源信息（含已加载的表结构）
  if (selectedDataSourceId.value) {
    const selectedDs = systemContext.dataSources.find((ds: any) => ds.id === selectedDataSourceId.value)
    if (selectedDs) {
      // 发送所有已加载列信息的表结构（前端已加载的），其余只发表名
      const tablesWithSchema = selectedDs.tables?.filter((t: any) => t.columns && t.columns.length > 0) || []
      const tablesWithoutSchema = selectedDs.tables?.filter((t: any) => !t.columns || t.columns.length === 0) || []
      
      ctx.ds = {
        id: selectedDs.id,
        name: selectedDs.name,
        dbType: selectedDs.dbType,
        // 已加载结构的表：含完整字段信息
        schema: tablesWithSchema.map((t: any) => ({
          table: t.tableName,
          remarks: t.remarks || undefined,
          columns: t.columns.map((c: any) => ({
            name: c.columnName,
            type: c.dataType,
            pk: c.isPrimaryKey || undefined,
            nullable: c.nullable,
            remarks: c.remarks || undefined
          }))
        })),
        // 未加载结构的表：仅表名
        otherTables: tablesWithoutSchema.map((t: any) => t.tableName)
      }
      
      // 标记当前选中的表
      if (selectedTableName.value) {
        ctx.focusTable = selectedTableName.value
      }
    }
  }
  
  // 数据源概览（仅名称+类型）
  if (systemContext.dataSources.length > 0 && !selectedDataSourceId.value) {
    ctx.dataSources = systemContext.dataSources.map((ds: any) => `${ds.name}(${ds.dbType})`)
  }
  
  return JSON.stringify(ctx)
}

// 智能提示 - 结合系统上下文生成更智能的提示
const smartPrompt = (type: string) => {
  const table = selectedTableName.value
  const ds = selectedDataSourceId.value
    ? systemContext.dataSources.find((d: any) => d.id === selectedDataSourceId.value)
    : null
  const dsLabel = ds ? ds.name : ''
  
  const prompts: Record<string, string> = {
    sql: table
      ? `查询 ${table} 表，`
      : '编写SQL查询：',
    analyze: table
      ? `分析 ${table} 表的数据分布和特征，给出洞察`
      : '分析数据特征和分布：',
    optimize: '优化以下SQL性能：\n```sql\n\n```',
    report: table
      ? `基于 ${table} 表创建报表，展示`
      : '创建报表，展示',
    etl: table
      ? `同步 ${dsLabel ? dsLabel + '.' : ''}${table} → `
      : '创建数据同步任务：源表 → 目标表，',
    index: table
      ? `分析 ${table} 表的索引优化建议`
      : '分析索引优化建议，表名：',
    createReport: table
      ? `基于 ${table} 创建报表「${table}报表」，要求：\n1. 生成一个最优SQL，用 \${startDate} \${endDate} 做参数\n2. 给出推荐的展示维度和指标`
      : '创建报表，要求：\n1. 生成一个最优SQL，用 ${startDate} ${endDate} 做参数\n2. 给出推荐的展示维度和指标\n\n报表名称：',
    createChart: table
      ? `基于 ${table} 创建图表，自动选合适的图表类型和分组字段，生成一个最优SQL`
      : '创建图表，要求：\n1. 自动选合适图表类型\n2. 生成一个最优SQL\n\n数据描述：',
    createMenu: '添加菜单，名称：',
    createEtl: table
      ? `创建ETL同步 ${dsLabel ? dsLabel + '.' : ''}${table} → [目标库.目标表]\n同步方式：增量`
      : '创建ETL同步任务：\n源表：\n目标表：\n同步方式：增量'
  }
  inputText.value = prompts[type] || ''
  textareaRef.value?.focus()
}

// 使用示例问题
const useExample = (example: string) => {
  inputText.value = example
  textareaRef.value?.focus()
}

// 生成数据字典
const generateDataDictionary = async () => {
  if (!selectedDataSourceId.value) {
    message.warning('请先选择数据源')
    return
  }
  
  loading.value = true
  chatMessages.value.push({ role: 'user', content: '请生成当前数据源的数据字典' })
  scrollToBottom()
  
  try {
    const res: any = await request.get(`/ai/data-dictionary/${selectedDataSourceId.value}`)
    if (res?.data?.success) {
      const d = res.data
      let content = `## 📖 数据字典\n\n**数据源ID：** ${d.dataSourceId}\n**表数量：** ${d.tableCount}\n\n`
      
      d.tables?.forEach((t: any) => {
        content += `### 📋 ${t.tableName}\n`
        if (t.remarks) content += `> ${t.remarks}\n\n`
        content += `| 字段名 | 类型 | 主键 | 可空 | 备注 |\n|--------|------|------|------|------|\n`
        t.columns?.forEach((c: any) => {
          content += `| ${c.columnName} | ${c.dataType} | ${c.isPrimaryKey ? '✅' : ''} | ${c.nullable === 'YES' ? '是' : '否'} | ${c.remarks || ''} |\n`
        })
        content += '\n'
      })
      
      chatMessages.value.push({ role: 'assistant', content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '生成失败：' + (res?.data?.error || '未知错误') })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '生成失败：' + (e.message || '网络错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 查看报表列表
const viewReportsList = async () => {
  loading.value = true
  chatMessages.value.push({ role: 'user', content: '查看系统中的所有报表' })
  scrollToBottom()
  
  try {
    const res: any = await request.get('/ai/reports')
    if (res?.data?.success) {
      const reports = res.data.reports || []
      let content = `## 📊 报表列表\n\n**共 ${reports.length} 个报表**\n\n`
      
      if (reports.length === 0) {
        content += '暂无报表，点击"创建报表"开始创建吧！'
      } else {
        content += `| 名称 | 编码 | 描述 | 状态 |\n|------|------|------|------|\n`
        reports.forEach((r: any) => {
          content += `| ${r.name} | ${r.code} | ${r.description || '-'} | ${r.status === 1 ? '✅启用' : '⏸禁用'} |\n`
        })
      }
      
      chatMessages.value.push({ role: 'assistant', content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '获取失败：' + (res?.data?.error || '未知错误') })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '获取失败：' + (e.message || '网络错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 查看ETL任务列表
const viewEtlJobs = async () => {
  loading.value = true
  chatMessages.value.push({ role: 'user', content: '查看系统中的所有ETL同步任务' })
  scrollToBottom()
  
  try {
    const res: any = await request.get('/ai/etl-jobs')
    if (res?.data?.success) {
      const jobs = res.data.jobs || []
      let content = `## 🔄 ETL任务列表\n\n**共 ${jobs.length} 个任务**\n\n`
      
      if (jobs.length === 0) {
        content += '暂无ETL任务，点击"创建ETL"开始创建吧！'
      } else {
        content += `| 名称 | 源表 | 目标表 | 模式 | 状态 |\n|------|------|--------|------|------|\n`
        jobs.forEach((j: any) => {
          content += `| ${j.name} | ${j.sourceTable || '-'} | ${j.targetTable || '-'} | ${j.writeMode || 'insert'} | ${j.status === 1 ? '🟢运行' : '⏸停止'} |\n`
        })
      }
      
      chatMessages.value.push({ role: 'assistant', content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '获取失败：' + (res?.data?.error || '未知错误') })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '获取失败：' + (e.message || '网络错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 数据质量分析
const analyzeDataQuality = async () => {
  if (!selectedDataSourceId.value || !selectedTableName.value) {
    message.warning('请先选择数据源和表')
    return
  }
  
  loading.value = true
  chatMessages.value.push({ role: 'user', content: `分析表 ${selectedTableName.value} 的数据质量` })
  scrollToBottom()
  
  try {
    const res: any = await request.get(`/ai/data-quality/${selectedDataSourceId.value}/${selectedTableName.value}`)
    if (res?.data?.success) {
      const d = res.data
      let content = `## 📊 数据质量分析报告\n\n`
      content += `**表名：** ${d.tableName}\n`
      content += `**质量评分：** ${d.qualityScore}/100\n`
      content += `**字段数：** ${d.columnCount}\n\n`
      content += d.summary + '\n\n'
      
      content += `### 字段详情\n\n`
      content += `| 字段名 | 类型 | 空值率 | 问题级别 | 建议 |\n|--------|------|--------|----------|------|\n`
      d.columns?.forEach((c: any) => {
        const severity = c.severity === 'high' ? '🔴严重' : c.severity === 'medium' ? '🟡中等' : '🟢良好'
        content += `| ${c.columnName} | ${c.dataType} | ${c.nullRate || 0}% | ${severity} | ${c.suggestion || '-'} |\n`
      })
      
      chatMessages.value.push({ role: 'assistant', content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '分析失败：' + (res?.data?.error || '未知错误') })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '分析失败：' + (e.message || '网络错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 获取智能建议
const getSmartSuggestions = async () => {
  if (!selectedDataSourceId.value || !selectedTableName.value) {
    message.warning('请先选择数据源和表')
    return
  }
  
  loading.value = true
  chatMessages.value.push({ role: 'user', content: `获取表 ${selectedTableName.value} 的智能建议` })
  scrollToBottom()
  
  try {
    const res: any = await request.get(`/ai/suggestions/${selectedDataSourceId.value}/${selectedTableName.value}`)
    if (res?.data?.success) {
      const suggestions = res.data.suggestions || []
      let content = `## 💡 智能建议\n\n**表名：** ${res.data.tableName}\n\n`
      
      if (suggestions.length === 0) {
        content += '暂无建议'
      } else {
        suggestions.forEach((s: any, _index: number) => {
          const icon = s.type === 'index' ? '🔍' : s.type === 'report' ? '📊' : '🔄'
          content += `### ${icon} ${s.title}\n\n`
          content += `${s.description}\n\n`
          if (s.columns) {
            content += `**相关字段：** ${s.columns.join(', ')}\n\n`
          }
          if (s.sampleSql) {
            content += `**示例SQL：**\n\`\`\`sql\n${s.sampleSql}\n\`\`\`\n\n`
          }
        })
      }
      
      chatMessages.value.push({ role: 'assistant', content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '获取失败：' + (res?.data?.error || '未知错误') })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '获取失败：' + (e.message || '网络错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 运行系统诊断
const runSystemDiagnosis = async () => {
  loading.value = true
  chatMessages.value.push({ role: 'user', content: '请帮我诊断系统健康状态' })
  scrollToBottom()
  
  try {
    const res: any = await request.get('/ai/system-diagnosis')
    if (res?.data?.success) {
      const d = res.data
      let content = '## 🏥 系统健康诊断报告\n\n'
      
      // 数据源状态
      if (d.dataSourceSummary) {
        const s = d.dataSourceSummary
        content += `### 数据源状态\n`
        content += `- 总数：**${s.total}** 个\n`
        content += `- 正常：**${s.healthy}** 个 ✅\n`
        content += `- 异常：**${s.unhealthy}** 个 ${s.unhealthy > 0 ? '⚠️' : ''}\n\n`
      }
      
      // ETL任务状态
      if (d.etlJobSummary) {
        const e = d.etlJobSummary
        content += `### ETL任务状态\n`
        content += `- 总数：**${e.total}** 个\n`
        content += `- 运行中：**${e.running}** 个\n`
        content += `- 已停止：**${e.stopped}** 个\n\n`
      }
      
      // 系统信息
      if (d.systemInfo) {
        const sys = d.systemInfo
        content += `### 服务器信息\n`
        content += `- Java版本：${sys.javaVersion}\n`
        content += `- 最大内存：${sys.maxMemory}\n`
        content += `- 已用内存：${sys.totalMemory}\n`
        content += `- 空闲内存：${sys.freeMemory}\n`
        content += `- CPU核心：${sys.processors}\n\n`
      }
      
      // 诊断建议
      if (d.suggestions && d.suggestions.length > 0) {
        content += `### ⚠️ 诊断建议\n`
        d.suggestions.forEach((s: string) => {
          content += `- ${s}\n`
        })
      } else {
        content += `### ✅ 诊断结论\n系统运行正常，未发现问题。`
      }
      
      chatMessages.value.push({ role: 'assistant', content })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '诊断失败：' + (res?.data?.error || '未知错误') })
    }
  } catch (e: any) {
    chatMessages.value.push({ role: 'assistant', content: '诊断失败：' + (e.message || '网络错误') })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 初始化会话
const initSession = async (): Promise<boolean> => {
  try {
    const res: any = await request.post('/ai/session/create', {}, { timeout: 15000, __silent: true } as any)
    if (res?.data?.sessionId) {
      sessionId.value = res.data.sessionId
      return true
    }
    console.warn('创建会话：响应中无 sessionId', res)
    return false
  } catch (e: any) {
    console.error('创建会话失败', e)
    return false
  }
}

// 获取AI状态 - 同步到共享 store
const fetchStatus = async () => {
  try {
    const res: any = await request.get('/ai/status', { __silent: true } as any)
    if (res?.data) {
      aiStatus.value = res.data
      // 同步到共享 store，让 AiAssistant 页面也能获取最新状态
      aiConfigStore.aiStatus.enabled = res.data.enabled ?? false
      aiConfigStore.aiStatus.provider = res.data.provider ?? ''
      aiConfigStore.aiStatus.model = res.data.model ?? ''
      aiConfigStore.aiStatus.apiKeyConfigured = res.data.apiKeyConfigured ?? false
    }
  } catch (e) {
    console.error('获取AI状态失败', e)
  }
}

// 加载配置（多服务商）
const loadConfig = async () => {
  try {
    const res: any = await request.get('/ai/config', { __silent: true } as any)
    if (res?.data) {
      // 设置当前活跃服务商
      if (res.data.activeProvider) activeProvider.value = res.data.activeProvider
      if (res.data.maxHistory != null) configForm.maxHistory = res.data.maxHistory
      // 加载所有服务商配置到缓存
      if (res.data.providers) {
        for (const [key, val] of Object.entries(res.data.providers as Record<string, any>)) {
          providerConfigs[key] = {
            apiKey: val.apiKey || '',
            maskedApiKey: val.apiKey || '',
            baseUrl: val.baseUrl || '',
            model: val.model || '',
            configured: val.configured || false
          }
        }
      }
      // 默认编辑标签切到当前活跃服务商
      if (activeProvider.value) {
        editingProvider.value = activeProvider.value
      }
      loadFormFromCache(editingProvider.value)
    }
  } catch (e) {
    // 使用默认配置
  }
}

// 保存配置（setActive: 是否同时设为当前使用）
const saveConfig = async (setActive: boolean = true) => {
  savingConfig.value = true
  try {
    const provider = editingProvider.value
    const cached = providerConfigs[provider]
    // apiKey 未修改（仍是脱敏值）则不发送
    const apiKeyToSend = (cached && configForm.apiKey === cached.maskedApiKey) ? undefined : configForm.apiKey
    const payload: any = {
      provider,
      baseUrl: configForm.baseUrl,
      model: configForm.model,
      maxHistory: configForm.maxHistory
    }
    if (apiKeyToSend !== undefined) payload.apiKey = apiKeyToSend
    if (setActive) payload.setActive = true

    await request.post('/ai/config', payload)
    if (setActive) {
      activeProvider.value = provider
      message.success(`已保存并切换到 ${providerLabelMap[provider] || provider}`)
    } else {
      message.success(`${providerLabelMap[provider] || provider} 配置已保存`)
    }
    await fetchStatus()
    await loadConfig()
    if (setActive) showConfigModal.value = false
  } catch (e: any) {
    message.error('保存失败：' + (e.message || '未知错误'))
  } finally {
    savingConfig.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  const text = inputText.value.trim()
  const hasFile = uploadedFile.value !== null
  
  if ((!text && !hasFile) || loading.value) return
  
  // 检查AI是否已配置
  if (!aiStatus.value.enabled) {
    message.warning('请先配置AI服务')
    showConfigModal.value = true
    return
  }
  
  if (!sessionId.value) {
    const ok = await initSession()
    if (!ok || !sessionId.value) {
      message.error('无法创建AI会话，请检查后端服务是否启动')
      return
    }
  }
  
  // 如果有文件，先上传文件分析
  if (hasFile && uploadedFile.value) {
    const userContent = text || `请分析上传的文件: ${uploadedFile.value.name}`
    chatMessages.value.push({ role: 'user', content: userContent, timestamp: Date.now() })
    inputText.value = ''
    showSlashMenu.value = false
    loading.value = true
    
    await nextTick()
    scrollToBottom()
    
    try {
      const formData = new FormData()
      formData.append('file', uploadedFile.value)
      if (text) {
        formData.append('question', text)
      }
      
      const res: any = await request.post('/ai/analyze-file', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 180000
      })
      
      if (res?.data?.success) {
        let content = res.data.analysis || res.data.content || '文件分析完成'
        if (res.data.preview) {
          content += `\n\n**数据预览 (前5行):**\n\`\`\`\n${JSON.stringify(res.data.preview, null, 2)}\n\`\`\``
        }
        chatMessages.value.push({ role: 'assistant', content, timestamp: Date.now() })
        saveChatMessage('user', userContent)
        saveChatMessage('assistant', content)
      } else {
        chatMessages.value.push({ 
          role: 'assistant', 
          content: `⚠️ **文件分析失败**\n\n${res?.data?.error || res?.msg || '未知错误'}`,
          timestamp: Date.now()
        })
      }
    } catch (e: any) {
      console.error('[AI Chat] 文件分析异常:', e)
      chatMessages.value.push({ 
        role: 'assistant', 
        content: `⚠️ **文件分析失败**\n\n${e.message || '网络请求失败'}`,
        timestamp: Date.now()
      })
    } finally {
      loading.value = false
      clearUploadedFile()
      await nextTick()
      scrollToBottom()
    }
    return
  }
  
  // 发送文本消息
  const now = Date.now()
  chatMessages.value.push({ role: 'user', content: text, timestamp: now })
  inputText.value = ''
  showSlashMenu.value = false
  loading.value = true
  
  // 创建 AbortController 支持取消
  currentAbortController = new AbortController()
  const signal = currentAbortController.signal
  
  await nextTick()
  scrollToBottom()
  
  try {
    const res: any = await request.post('/ai/chat', {
      message: text,
      sessionId: sessionId.value,
      context: buildDetailedContext()
    }, {
      timeout: 180000,
      signal
    })
    
    if (res?.data?.success) {
      chatMessages.value.push({ role: 'assistant', content: res.data.content, timestamp: Date.now() })
      saveChatMessage('user', text)
      saveChatMessage('assistant', res.data.content)
    } else {
      const errorMsg = res?.data?.error || res?.msg || '请求失败'
      console.error('[AI Chat] 请求失败:', errorMsg)
      chatMessages.value.push({ 
        role: 'assistant', 
        content: `⚠️ **请求失败**\n\n${getErrorHint(errorMsg)}`,
        timestamp: Date.now()
      })
    }
  } catch (e: any) {
    if (e.name === 'AbortError' || signal.aborted) {
      // 用户主动取消，已在 abortRequest 中处理
      return
    }
    console.error('[AI Chat] 异常:', e)
    const errorMsg = e.message || '网络请求失败'
    chatMessages.value.push({ 
      role: 'assistant', 
      content: `⚠️ **请求失败**\n\n${getErrorHint(errorMsg)}`,
      timestamp: Date.now()
    })
  } finally {
    currentAbortController = null
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 重试最后一条失败的消息
const retryLastMessage = () => {
  // 找到最后一条用户消息
  let lastUserMsgIndex = -1
  for (let i = chatMessages.value.length - 1; i >= 0; i--) {
    if (chatMessages.value[i]?.role === 'user') {
      lastUserMsgIndex = i
      break
    }
  }
  if (lastUserMsgIndex < 0) return
  
  const lastUserText = chatMessages.value[lastUserMsgIndex]?.content ?? ''
  // 移除失败的 assistant 消息
  while (chatMessages.value.length > lastUserMsgIndex + 1) {
    chatMessages.value.pop()
  }
  // 也移除用户消息，因为 sendMessage 会重新添加
  chatMessages.value.pop()
  
  inputText.value = lastUserText
  nextTick(() => sendMessage())
}

// 获取错误提示
const getErrorHint = (error: string) => {
  // 如果后端已返回中文的具体错误信息，直接使用
  if (error.includes('无法连接') || error.includes('请检查') || error.includes('请稍后') || 
      error.includes('API密钥') || error.includes('服务暂时不可用') || error.includes('响应超时')) {
    return error
  }
  
  const errorLower = error.toLowerCase()
  
  // 处理底层网络错误（英文错误信息）
  if (errorLower.includes('timeout') || errorLower.includes('timed out')) {
    return 'AI服务响应超时，请检查网络或稍后重试。可在设置中增加超时时间。'
  }
  if (errorLower.includes('401') || errorLower.includes('unauthorized')) {
    return 'API密钥无效或已过期，请在设置中更新API密钥。'
  }
  if (errorLower.includes('429') || errorLower.includes('rate limit')) {
    return '请求过于频繁，请等待几秒后再试。'
  }
  if (errorLower.includes('502') || errorLower.includes('503') || errorLower.includes('504')) {
    return 'AI服务暂时不可用，请稍后重试。'
  }
  if (errorLower.includes('econnrefused') || errorLower.includes('connection refused')) {
    return '无法连接到AI服务，请检查：\n1. AI服务是否已启动\n2. API地址配置是否正确\n3. 网络是否通畅'
  }
  if (errorLower.includes('failed to fetch') || errorLower.includes('network')) {
    return '网络请求失败，请检查网络连接。'
  }
  if (error.includes('AI对话失败')) {
    // 提取实际错误信息
    const match = error.match(/AI对话失败[：:]\s*(.+)/)
    if (match) {
      return match[1]  // 直接返回后端的错误信息
    }
  }
  // 默认返回原始错误
  return error || '未知错误，请稍后重试'
}

// 清空对话
const clearChat = async () => {
  if (sessionId.value) {
    try {
      await request.post('/ai/session/clear', { sessionId: sessionId.value })
    } catch {
      // 静默处理
    }
  }
  chatMessages.value = []
  const ok = await initSession()
  if (ok) {
    message.success('对话已清空')
  } else {
    message.warning('对话已清空，但重建会话失败')
  }
}

// 加载用户会话列表
const loadSessionList = async () => {
  // 确保用户ID有效
  const userId = currentUserId.value
  if (!userId || userId <= 0) {
    console.warn('用户ID无效，跳过加载会话列表')
    return
  }
  
  loadingSessions.value = true
  try {
    const res: any = await request.get('/ai/chat/sessions', {
      params: { userId, limit: 20 }
    })
    if (res?.data) {
      sessionList.value = res.data
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  } finally {
    loadingSessions.value = false
  }
}

// 创建新会话
const createNewSession = async () => {
  try {
    const res: any = await request.post('/ai/session/create', {}, { timeout: 15000 })
    if (res?.data?.sessionId) {
      sessionId.value = res.data.sessionId
      chatMessages.value = []
      showHistoryPanel.value = false
      message.success('已创建新对话')
    } else {
      message.warning('创建会话失败：服务未返回有效会话')
    }
  } catch (e: any) {
    console.error('创建会话失败', e)
    message.error('创建会话失败：' + (e.message || '网络错误'))
  }
}

// 切换到历史会话
const switchToSession = async (sid: string) => {
  sessionId.value = sid
  showHistoryPanel.value = false
  await loadSessionHistory(sid)
}

// 加载会话历史消息
const loadSessionHistory = async (sid: string) => {
  try {
    const res: any = await request.get(`/ai/chat/history/${sid}`)
    if (res?.data?.success && res.data.messages) {
      chatMessages.value = res.data.messages.map((m: any) => ({
        role: m.role,
        content: m.content
      }))
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('加载会话历史失败', e)
  }
}

// 保存消息到历史
const saveChatMessage = async (role: string, content: string) => {
  if (!sessionId.value) {
    console.warn('sessionId为空，跳过保存')
    return
  }
  try {
    const res = await request.post('/ai/chat/save', {
      sessionId: sessionId.value,
      userId: currentUserId.value,
      role,
      content,
      dataSourceId: selectedDataSourceId.value,
      messageType: 'text'
    })
    void res // 消息保存成功
  } catch (e) {
    console.error('保存消息失败', e)
  }
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}

// 监听历史面板打开
watch(showHistoryPanel, (newVal) => {
  if (newVal) {
    loadSessionList()
  }
})

// 监听抽屉打开时加载会话列表
watch(showDrawer, (newVal) => {
  // 同步到 store
  aiConfigStore.showChatDrawer = newVal
  if (newVal && showHistoryPanel.value) {
    loadSessionList()
  }
})

// 监听 store 的 showChatDrawer（由 AiAssistant 触发打开）
watch(() => aiConfigStore.showChatDrawer, (newVal) => {
  if (newVal !== showDrawer.value) {
    showDrawer.value = newVal
  }
  // 打开时检查是否有预填消息或配置请求
  if (newVal) {
    nextTick(() => {
      // 处理配置打开请求
      if (aiConfigStore.consumeConfigRequest()) {
        showConfigModal.value = true
        return
      }
      // 处理预填消息
      const { message: pendingMsg } = aiConfigStore.consumePendingMessage()
      if (pendingMsg) {
        inputText.value = pendingMsg
        // 自动发送
        nextTick(() => sendMessage())
      }
    })
  }
})

// 监听用户ID变化时加载会话列表
watch(currentUserId, (newVal) => {
  if (newVal && newVal > 0 && showHistoryPanel.value) {
    loadSessionList()
  }
})

// 复制内容（清理HTML标签）
const copyContent = async (content: string) => {
  try {
    // 清理HTML标签，将<br>转换为换行符
    const cleanContent = content
      .replace(/<br\s*\/?>/gi, '\n')  // <br> -> 换行
      .replace(/<\/p>/gi, '\n')       // </p> -> 换行
      .replace(/<\/div>/gi, '\n')     // </div> -> 换行
      .replace(/<\/li>/gi, '\n')      // </li> -> 换行
      .replace(/<[^>]*>/g, '')        // 移除其他HTML标签
      .replace(/&nbsp;/g, ' ')        // &nbsp; -> 空格
      .replace(/&lt;/g, '<')          // HTML实体还原
      .replace(/&gt;/g, '>')
      .replace(/&amp;/g, '&')
      .replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'")
      .replace(/\n{3,}/g, '\n\n')     // 多个连续换行合并为两个
      .trim()
    
    await navigator.clipboard.writeText(cleanContent)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

// 重新生成
const regenerate = async (index: number) => {
  if (index < 1 || loading.value) return
  // 获取上一条用户消息
  const userMsg = chatMessages.value[index - 1]
  if (userMsg?.role !== 'user') return
  // 删除最后一条AI回复
  chatMessages.value.pop()
  // 重新发送
  loading.value = true
  currentAbortController = new AbortController()
  const signal = currentAbortController.signal
  await nextTick()
  scrollToBottom()
  
  try {
    const res: any = await request.post('/ai/chat', {
      message: userMsg.content,
      sessionId: sessionId.value,
      context: buildDetailedContext()
    }, { timeout: 180000, signal })
    if (res?.data?.success) {
      chatMessages.value.push({ role: 'assistant', content: res.data.content, timestamp: Date.now() })
    } else {
      chatMessages.value.push({ role: 'assistant', content: '重新生成失败：' + (res?.msg || '未知错误'), timestamp: Date.now() })
    }
  } catch (e: any) {
    if (e.name === 'AbortError' || signal.aborted) return
    chatMessages.value.push({ role: 'assistant', content: '请求失败：' + (e.message || '网络错误'), timestamp: Date.now() })
  } finally {
    currentAbortController = null
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = (instant = false) => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTo({
        top: messagesRef.value.scrollHeight,
        behavior: instant ? 'instant' : 'smooth'
      })
    }
  })
}

onMounted(() => {
  fetchStatus()
  initSession()
  loadSystemContext()
  loadConfig()
  // 默认加载会话列表
  if (showHistoryPanel.value) {
    loadSessionList()
  }
})

</script>

<style scoped>
/* 悬浮按钮 */
.ai-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(99, 102, 241, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
}

.ai-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.5);
}

.fab-close {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.2s;
}

.ai-fab:hover .fab-close {
  opacity: 1;
  transform: scale(1);
}

.fab-enter-active, .fab-leave-active {
  transition: all 0.3s ease;
}
.fab-enter-from, .fab-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* 显示按钮（隐藏后） */
.ai-show-btn {
  position: fixed;
  right: 8px;
  bottom: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.2s;
  z-index: 1000;
}

.ai-show-btn:hover {
  opacity: 1;
  transform: scale(1.1);
}

/* 抽屉头部 */
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.header-title {
  font-weight: 600;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 4px;
}

/* 数据源选择器固定容器 */
.context-selector-fixed {
  position: sticky;
  top: 0;
  z-index: 10;
  background: white;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 8px;
}

/* 数据源选择器 */
.context-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #eff6ff, #e0e7ff);
  border: 1px solid #c7d2fe;
  border-radius: 12px;
}

/* 已加载的表信息 */
.loaded-tables {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
}

.tables-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #166534;
  cursor: pointer;
}

.tables-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.view-detail {
  margin-left: auto;
  font-size: 11px;
  color: #22c55e;
}

.schema-panel {
  margin-top: 10px;
}

.schema-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 10px;
  padding: 6px 10px;
  background: #f8fafc;
  border-radius: 6px;
}

.table-cards {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}

.table-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.table-card:hover {
  border-color: #6366f1;
}

.table-card.expanded {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.table-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
}

.table-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.column-count {
  font-size: 11px;
  color: #94a3b8;
}

.table-columns {
  border-top: 1px solid #f1f5f9;
  padding: 8px 12px;
  background: #f8fafc;
}

.column-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 12px;
}

.col-name {
  color: #334155;
}

.col-name.pk {
  font-weight: 500;
  color: #6366f1;
}

.pk-badge {
  display: inline-block;
  font-size: 9px;
  background: #6366f1;
  color: white;
  padding: 1px 4px;
  border-radius: 3px;
  margin-left: 4px;
}

.col-type {
  color: #94a3b8;
  font-family: monospace;
  font-size: 11px;
}

/* SQL执行结果 */
.sql-result {
  margin-top: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f0fdf4, #ecfdf5);
  border-bottom: 1px solid #e2e8f0;
}

.result-title {
  font-size: 12px;
  font-weight: 500;
  color: #166534;
}

.result-actions {
  display: flex;
  gap: 6px;
}

.result-table-wrapper {
  max-height: 250px;
  overflow: auto;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.result-table th {
  background: #f8fafc;
  padding: 8px 10px;
  text-align: left;
  font-weight: 500;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
}

.result-table td {
  padding: 6px 10px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-table tr:hover td {
  background: #f8fafc;
}

.result-hint {
  padding: 6px 12px;
  font-size: 11px;
  color: #94a3b8;
  background: #f8fafc;
  text-align: center;
}

/* 操作分组 */
.action-group {
  margin-bottom: 8px;
}

.action-group:last-child {
  margin-bottom: 0;
}

.action-group-title {
  font-size: 10px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
  padding-left: 2px;
}

/* 3列网格 */
.quick-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

/* 4列网格 */
.quick-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

/* 紧凑型快捷项 */
.quick-item-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
}

.quick-item-compact:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.quick-icon-sm {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

/* 历史会话面板 */
.history-panel {
  background: linear-gradient(180deg, #f8fafc 0%, #ffffff 100%);
  border-bottom: 1px solid #e2e8f0;
  padding: 10px 12px;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.history-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.history-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-right: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid #e2e8f0;
}

.history-item:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  transform: translateX(2px);
}

.history-item.active {
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.history-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
  flex-shrink: 0;
}

.history-item.active .history-item-icon {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
}

.history-item-content {
  flex: 1;
  min-width: 0;
}

.history-title {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.history-arrow {
  color: #94a3b8;
  transform: rotate(-90deg);
  transition: transform 0.2s;
}

.history-item:hover .history-arrow {
  color: #6366f1;
  transform: rotate(-90deg) translateY(2px);
}

.history-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 32px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px dashed #e2e8f0;
}

.empty-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.empty-hint {
  font-size: 12px;
  color: #94a3b8;
}

/* 聊天容器 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* 快捷指令 */
.quick-actions {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
}

.quick-header {
  margin-bottom: 10px;
}

.quick-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.quick-desc {
  font-size: 11px;
  color: #64748b;
  margin-left: 6px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.quick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
  transform: translateY(-1px);
}

.quick-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.quick-icon.sql { background: linear-gradient(135deg, #3b82f6, #1d4ed8); }
.quick-icon.analyze { background: linear-gradient(135deg, #10b981, #059669); }
.quick-icon.optimize { background: linear-gradient(135deg, #f59e0b, #d97706); }
.quick-icon.design { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

.quick-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quick-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.quick-hint {
  font-size: 11px;
  color: #94a3b8;
}

/* 消息列表 */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  padding-right: 8px;
}

/* 美化滚动条 */
.messages::-webkit-scrollbar {
  width: 8px;
}

.messages::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

.messages::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #a5b4fc, #818cf8);
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.messages::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #818cf8, #6366f1);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  color: #94a3b8;
}

.empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: #6366f1;
  position: relative;
  animation: pulse-glow 2s ease-in-out infinite;
}

.empty-icon-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  opacity: 0.2;
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { transform: scale(1); opacity: 0.2; }
  50% { transform: scale(1.15); opacity: 0.1; }
}

@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.3); }
  50% { box-shadow: 0 0 20px 5px rgba(99, 102, 241, 0.15); }
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.empty-desc {
  font-size: 13px;
  color: #64748b;
  margin: 8px 0 0;
}

/* 智能示例 */
.smart-examples {
  margin-top: 24px;
  width: 100%;
  max-width: 400px;
}

.example-title {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 12px;
  text-align: center;
}

.example-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #475569;
}

.example-item:hover {
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
  border-color: #c7d2fe;
  transform: translateX(4px);
  color: #4f46e5;
}

.example-item .n-icon {
  color: #6366f1;
  flex-shrink: 0;
}

/* 命令提示 */
.command-hint {
  margin-top: 16px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border: 1px solid #fcd34d;
  border-radius: 10px;
  font-size: 12px;
  color: #92400e;
  display: flex;
  align-items: center;
  gap: 8px;
}

.command-hint .hint-icon {
  font-size: 14px;
}

.command-hint code {
  background: rgba(255, 255, 255, 0.6);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
  color: #78350f;
}

/* 消息气泡 */
.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transition: transform 0.2s ease;
}

.message:hover .avatar {
  transform: scale(1.05);
}

.avatar.thinking {
  animation: pulse-breathe 2s ease-in-out infinite;
}

@keyframes pulse-breathe {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.95); }
}

.message.user .avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.message.assistant .avatar {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
}

.bubble-wrapper {
  max-width: 85%;
  display: flex;
  flex-direction: column;
}

.bubble {
  padding: 14px 18px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.75;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s ease;
}

.message:hover .bubble {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.message.user .bubble {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%);
  color: white;
  border-bottom-right-radius: 6px;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.25);
}

.message.assistant .bubble {
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.bubble-actions {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message:hover .bubble-actions {
  opacity: 1;
}

/* 打字动画 */
.typing {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
}

.typing-indicator {
  display: flex;
  gap: 4px;
}

.typing-text {
  font-size: 12px;
  color: #64748b;
  margin-left: 4px;
}

.stop-gen-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 12px;
  padding: 3px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  color: #ef4444;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.stop-gen-btn:hover {
  background: #fef2f2;
  border-color: #fca5a5;
}

.msg-time {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 2px;
}
.message.user .msg-time {
  text-align: right;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  animation: bounce 1.4s infinite;
}

.dot:nth-child(2) { animation-delay: 0.15s; }
.dot:nth-child(3) { animation-delay: 0.3s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* Markdown样式 */
.markdown :deep(.code-block) {
  margin: 12px 0;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  border: 1px solid #334155;
}

.markdown :deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: linear-gradient(180deg, #334155 0%, #1e293b 100%);
  font-size: 12px;
  border-bottom: 1px solid #475569;
}

.markdown :deep(.code-lang) {
  color: #94a3b8;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.markdown :deep(.code-lang::before) {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 6px #10b981;
}

.markdown :deep(.code-actions) {
  display: flex;
  gap: 8px;
  align-items: center;
}

.markdown :deep(.code-copy-btn) {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  color: #fff;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.markdown :deep(.code-copy-btn:hover) {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.markdown :deep(.code-etl-btn) {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.markdown :deep(.code-etl-btn:hover) {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.markdown :deep(pre) {
  background: transparent;
  color: #e2e8f0;
  padding: 14px 16px;
  margin: 0;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
}

.markdown :deep(pre code) {
  font-family: inherit;
}

.markdown :deep(.inline-code) {
  background: #e2e8f0;
  color: #6366f1;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
}

.markdown :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
}

.markdown :deep(h2),
.markdown :deep(h3),
.markdown :deep(h4) {
  margin: 12px 0 8px;
  color: #1e293b;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown :deep(ol) {
  list-style-type: decimal;
}

.markdown :deep(li) {
  margin: 4px 0;
}

.markdown :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 14px;
  border-left: 3px solid #6366f1;
  background: #f1f5f9;
  border-radius: 0 8px 8px 0;
  color: #475569;
  font-style: italic;
}

.markdown :deep(.md-table-wrap) {
  overflow-x: auto;
  margin: 10px 0;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.markdown :deep(th) {
  background: #f1f5f9;
  padding: 8px 12px;
  text-align: left;
  font-weight: 600;
  color: #334155;
  border-bottom: 2px solid #e2e8f0;
}

.markdown :deep(td) {
  padding: 6px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #475569;
}

.markdown :deep(tr:hover td) {
  background: #f8fafc;
}

.markdown :deep(del) {
  color: #94a3af;
  text-decoration: line-through;
}

.markdown :deep(a) {
  color: #6366f1;
  text-decoration: none;
  border-bottom: 1px dotted #6366f1;
}

.markdown :deep(a:hover) {
  color: #4f46e5;
  border-bottom-style: solid;
}

.markdown :deep(hr) {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 12px 0;
}

.markdown :deep(h5) {
  margin: 10px 0 6px;
  color: #1e293b;
  font-size: 14px;
}

/* 覆盖抽屉footer内边距 */
:deep(.n-drawer-body-content-wrapper) {
  padding: 0 16px !important;
}

:deep(.n-drawer-footer) {
  padding: 0 16px 12px !important;
  border-top: none !important;
}

/* 输入区域 - 全宽样式 */
.input-box {
  background: #fff;
  padding: 8px 0;
  width: 100%;
  box-sizing: border-box;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  border: 1.5px solid #e5e7eb;
  border-radius: 16px;
  padding: 10px 10px 10px 16px;
  background: #fafafa;
  transition: all 0.2s;
  width: 100%;
  box-sizing: border-box;
}

.input-row:focus-within {
  border-color: #6366f1;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.08);
}

.text-input {
  flex: 1;
  width: 100%;
  border: none;
  background: transparent;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  line-height: 1.6;
  min-height: 100px;
  max-height: 200px;
  padding: 8px 0;
  overflow-y: auto;
}

.text-input::placeholder {
  color: #a0aec0;
}

.send-button {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: #e2e8f0;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.send-button.active {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
}

.send-button.active:hover {
  transform: scale(1.08);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 输入操作区域 */
.input-actions {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

/* 文件上传按钮 */
.upload-button {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.upload-button:hover {
  background: #e2e8f0;
  color: #6366f1;
}

/* 文件预览 */
.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  margin-bottom: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: #065f46;
}

.file-size {
  font-size: 12px;
  color: #6b7280;
}

.input-tips {
  padding: 4px 4px 0;
  text-align: right;
}

.hint {
  font-size: 11px;
  color: #cbd5e1;
}

/* 智能上下文建议 */
.context-suggestions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.suggestion-label {
  font-size: 12px;
  color: #64748b;
}

.suggestion-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
  border: 1px solid #c7d2fe;
  border-radius: 16px;
  font-size: 12px;
  color: #4f46e5;
  cursor: pointer;
  transition: all 0.2s ease;
}

.suggestion-chip:hover {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-color: #a5b4fc;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

/* 上下文标签 */
.context-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.context-tags :deep(.n-tag) {
  border-radius: 16px;
}

.input-row.has-context {
  border-color: #6366f1;
  background: #fefefe;
}

/* 斜杠命令菜单 */
.slash-commands {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
  overflow: hidden;
}

.slash-title {
  font-size: 11px;
  color: #94a3b8;
  padding: 8px 12px 4px;
  font-weight: 500;
}

.slash-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.15s;
}

.slash-item:hover,
.slash-item.active {
  background: #f1f5f9;
}

.cmd-icon {
  font-size: 18px;
}

.cmd-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cmd-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.cmd-desc {
  font-size: 11px;
  color: #64748b;
}

/* AI配置弹窗样式 */
.config-modal-wrapper {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 560px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.config-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.config-modal-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.config-modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.config-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}
.provider-cards {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.provider-card {
  flex: 1;
  min-width: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 8px 6px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
  background: #fafafa;
  position: relative;
}
.provider-card:hover {
  border-color: #94a3b8;
  background: #f1f5f9;
}
.provider-card.active {
  border-color: #6366f1;
  background: #eef2ff;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.12);
}
.provider-card-icon {
  font-size: 22px;
  line-height: 1;
}
.provider-card-name {
  font-size: 11px;
  color: #475569;
  text-align: center;
  white-space: nowrap;
}
.provider-card.active .provider-card-name {
  color: #4f46e5;
  font-weight: 600;
}
.provider-badge-active {
  font-size: 9px;
  padding: 1px 6px;
  border-radius: 8px;
  background: #18a058;
  color: white;
  font-weight: 600;
  line-height: 1.4;
}
.provider-badge-ok {
  font-size: 10px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #10b981;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  line-height: 1;
}
.form-field-wrap {
  width: 100%;
}
.form-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  line-height: 1.4;
}
.slider-label {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  flex-shrink: 0;
}

/* 未配置 AI 引导状态 */
.unconfigured-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px 40px;
  text-align: center;
  height: 100%;
  min-height: 400px;
}

.unconfig-icon {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.unconfig-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.unconfig-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.unconfig-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 32px;
  text-align: left;
  width: 100%;
  max-width: 280px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #475569;
}

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 未配置时输入框禁用 */
.input-box-disabled {
  padding: 16px 0;
  width: 100%;
}

.disabled-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1.5px dashed #d1d5db;
  border-radius: 16px;
  font-size: 13px;
  color: #9ca3af;
}




















































































</style>

<style>
/* AiChat 深色模式（非 scoped） */
html.dark .context-selector-fixed {
  background: #1e293b !important;
  border-bottom-color: #334155 !important;
}
html.dark .context-selector {
  background: linear-gradient(135deg, #1e3a5f, #1e293b) !important;
  border-color: #334155 !important;
}
html.dark .loaded-tables {
  background: rgba(16, 185, 129, 0.08) !important;
  border-color: rgba(16, 185, 129, 0.2) !important;
}
html.dark .tables-header {
  color: #34d399 !important;
}
html.dark .view-detail {
  color: #34d399 !important;
}
html.dark .schema-hint {
  background: #0f172a !important;
  color: #94a3b8 !important;
}
html.dark .table-card {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .table-card:hover {
  border-color: #818cf8 !important;
}
html.dark .table-card.expanded {
  border-color: #818cf8 !important;
  box-shadow: 0 2px 8px rgba(129, 140, 248, 0.15) !important;
}
html.dark .table-name {
  color: #e2e8f0 !important;
}
html.dark .column-count {
  color: #64748b !important;
}
html.dark .table-columns {
  border-top-color: #334155 !important;
  background: #0f172a !important;
}
html.dark .col-name {
  color: #cbd5e1 !important;
}
html.dark .col-name.pk {
  color: #a5b4fc !important;
}
html.dark .pk-badge {
  background: #818cf8 !important;
}
html.dark .col-type {
  color: #64748b !important;
}
html.dark .sql-result {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .result-header {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05)) !important;
  border-bottom-color: #334155 !important;
}
html.dark .result-title {
  color: #34d399 !important;
}
html.dark .result-table th {
  background: #0f172a !important;
  color: #94a3b8 !important;
  border-bottom-color: #334155 !important;
}
html.dark .result-table td {
  color: #cbd5e1 !important;
  border-bottom-color: #1e293b !important;
}
html.dark .result-table tr:hover td {
  background: #263449 !important;
}
html.dark .result-hint {
  background: #0f172a !important;
  color: #64748b !important;
}
html.dark .quick-actions {
  background: linear-gradient(135deg, #1a2536 0%, #0f172a 100%) !important;
  border-color: #334155 !important;
}
html.dark .quick-title {
  color: #f1f5f9 !important;
}
html.dark .quick-desc {
  color: #94a3b8 !important;
}
html.dark .action-group-title {
  color: #94a3b8 !important;
}
html.dark .quick-item-compact {
  background: #1e293b !important;
  border-color: #334155 !important;
  color: #e2e8f0 !important;
}
html.dark .quick-item-compact:hover {
  background: #263449 !important;
  border-color: #475569 !important;
}
html.dark .quick-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .quick-item:hover {
  border-color: #818cf8 !important;
}
html.dark .quick-name {
  color: #e2e8f0 !important;
}
html.dark .history-panel {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%) !important;
  border-bottom-color: #334155 !important;
}
html.dark .history-header-left {
  color: #e2e8f0 !important;
}
html.dark .history-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .history-item:hover {
  background: #263449 !important;
  border-color: #475569 !important;
}
html.dark .history-item.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(99, 102, 241, 0.08)) !important;
  border-color: #818cf8 !important;
}
html.dark .history-title {
  color: #e2e8f0 !important;
}
html.dark .history-empty {
  background: linear-gradient(135deg, #1a2536 0%, #0f172a 100%) !important;
  border-color: #334155 !important;
}
html.dark .empty-text {
  color: #94a3b8 !important;
}
html.dark .messages::-webkit-scrollbar-track {
  background: #0f172a !important;
}
html.dark .messages::-webkit-scrollbar-thumb {
  border-color: #334155 !important;
}
html.dark .message.assistant .bubble {
  background: linear-gradient(180deg, #1e293b 0%, #1a2536 100%) !important;
  color: #e2e8f0 !important;
  border-color: #334155 !important;
}
html.dark .markdown h2,
html.dark .markdown h3,
html.dark .markdown h4,
html.dark .markdown h5 {
  color: #f1f5f9 !important;
}
html.dark .markdown .inline-code {
  background: #334155 !important;
  color: #a5b4fc !important;
}
html.dark .markdown blockquote {
  background: #1a2536 !important;
  border-left-color: #818cf8 !important;
  color: #94a3b8 !important;
}
html.dark .markdown .md-table-wrap {
  border-color: #334155 !important;
}
html.dark .markdown th {
  background: #0f172a !important;
  color: #94a3b8 !important;
  border-bottom-color: #334155 !important;
}
html.dark .markdown td {
  color: #cbd5e1 !important;
  border-bottom-color: #1e293b !important;
}
html.dark .markdown tr:hover td {
  background: #263449 !important;
}
html.dark .markdown hr {
  border-top-color: #334155 !important;
}
html.dark .markdown a {
  color: #a5b4fc !important;
  border-bottom-color: #a5b4fc !important;
}
html.dark .markdown a:hover {
  color: #c4b5fd !important;
}
html.dark .markdown del {
  color: #64748b !important;
}
html.dark .typing {
  background: linear-gradient(135deg, #1a2536, #0f172a) !important;
}
html.dark .typing-text {
  color: #94a3b8 !important;
}
html.dark .stop-gen-btn {
  background: #1e293b !important;
  border-color: #334155 !important;
  color: #f87171 !important;
}
html.dark .stop-gen-btn:hover {
  background: #2d1b1b !important;
  border-color: #7f1d1d !important;
}
html.dark .msg-time {
  color: #64748b !important;
}
html.dark .input-box {
  background: #1e293b !important;
}
html.dark .input-row {
  border-color: #334155 !important;
  background: #0f172a !important;
}
html.dark .input-row:focus-within {
  border-color: #818cf8 !important;
  background: #1e293b !important;
  box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.12) !important;
}
html.dark .input-row.has-context {
  border-color: #818cf8 !important;
  background: #1a2536 !important;
}
html.dark .text-input {
  color: #f1f5f9 !important;
}
html.dark .text-input::placeholder {
  color: #64748b !important;
}
html.dark .send-button {
  background: #334155 !important;
  color: #64748b !important;
}
html.dark .upload-button {
  background: #1e293b !important;
  color: #94a3b8 !important;
}
html.dark .upload-button:hover {
  background: #334155 !important;
  color: #a5b4fc !important;
}
html.dark .slash-commands {
  background: #1e293b !important;
  border-color: #334155 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4) !important;
}
html.dark .slash-item:hover,
html.dark .slash-item:hover,
html.dark .slash-item.active {
  background: #263449 !important;
}
html.dark .cmd-name {
  color: #e2e8f0 !important;
}
html.dark .cmd-desc {
  color: #94a3b8 !important;
}
html.dark .example-item {
  background: linear-gradient(135deg, #1a2536 0%, #0f172a 100%) !important;
  border-color: #334155 !important;
  color: #cbd5e1 !important;
}
html.dark .example-item:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(99, 102, 241, 0.06)) !important;
  border-color: #818cf8 !important;
  color: #a5b4fc !important;
}
html.dark .example-title {
  color: #94a3b8 !important;
}
html.dark .command-hint {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(245, 158, 11, 0.05)) !important;
  border-color: rgba(245, 158, 11, 0.3) !important;
  color: #fbbf24 !important;
}
html.dark .command-hint code {
  background: rgba(0, 0, 0, 0.3) !important;
  color: #fbbf24 !important;
}
html.dark .config-modal-wrapper {
  background: #1e293b !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5) !important;
}
html.dark .config-modal-title {
  color: #f1f5f9 !important;
}
html.dark .config-modal-footer {
  border-top-color: #334155 !important;
}
html.dark .provider-card {
  border-color: #334155 !important;
  background: #0f172a !important;
}
html.dark .provider-card:hover {
  border-color: #475569 !important;
  background: #1a2536 !important;
}
html.dark .provider-card.active {
  border-color: #818cf8 !important;
  background: rgba(99, 102, 241, 0.1) !important;
}
html.dark .provider-card-name {
  color: #cbd5e1 !important;
}
html.dark .provider-card.active .provider-card-name {
  color: #a5b4fc !important;
}
html.dark .unconfig-title {
  color: #f1f5f9 !important;
}
html.dark .unconfig-desc {
  color: #94a3b8 !important;
}
html.dark .step-item {
  color: #cbd5e1 !important;
}
html.dark .disabled-hint {
  background: #0f172a !important;
  border-color: #334155 !important;
  color: #64748b !important;
}
html.dark .file-preview {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05)) !important;
  border-color: rgba(16, 185, 129, 0.2) !important;
}
html.dark .file-name {
  color: #34d399 !important;
}
html.dark .suggestion-chip {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(99, 102, 241, 0.06)) !important;
  border-color: rgba(99, 102, 241, 0.3) !important;
  color: #a5b4fc !important;
}
html.dark .suggestion-chip:hover {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(99, 102, 241, 0.1)) !important;
  border-color: #818cf8 !important;
}
html.dark .suggestion-label {
  color: #94a3b8 !important;
}
html.dark .slider-label {
  color: #94a3b8 !important;
}
</style>
