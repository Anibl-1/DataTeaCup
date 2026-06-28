<template>
  <DesktopOnlyTip v-if="isMobileView" title="流程设计器" desc="流程设计器需要更大的屏幕空间进行节点拖拽和连线操作，请在电脑端打开。" />
  <div v-else class="pipeline-designer">
    <!-- 顶部工具栏 -->
    <div class="designer-toolbar">
      <div class="toolbar-left">
        <n-button quaternary size="large" @click="goBack">
          <template #icon><n-icon :component="ArrowBackOutline" size="20" /></template>
        </n-button>
        <div class="toolbar-title">
          <h3>{{ pipeline?.pipelineName || '流程设计器' }}</h3>
          <n-tag v-if="pipeline" :type="getStatusType(pipeline.pipelineStatus)" size="small" round>
            {{ getStatusText(pipeline.pipelineStatus) }}
          </n-tag>
        </div>
      </div>
      <div class="toolbar-center">
        <n-button-group>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button :type="isConnecting ? 'warning' : 'default'" @click="toggleConnectMode">
                <template #icon><n-icon :component="LinkOutline" /></template>
              </n-button>
            </template>
            {{ isConnecting ? '取消连线' : '连线模式' }}
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button @click="autoLayout">
                <template #icon><n-icon :component="GridOutline" /></template>
              </n-button>
            </template>
            自动布局
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button @click="zoomIn">
                <template #icon><n-icon :component="AddOutline" /></template>
              </n-button>
            </template>
            放大
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button @click="zoomOut">
                <template #icon><n-icon :component="RemoveOutline" /></template>
              </n-button>
            </template>
            缩小
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button @click="resetZoom">
                <template #icon><n-icon :component="ScanOutline" /></template>
              </n-button>
            </template>
            重置缩放
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button type="error" quaternary @click="clearCanvas">
                <template #icon><n-icon :component="TrashOutline" /></template>
              </n-button>
            </template>
            清空画布
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button quaternary>
                <template #icon><n-icon :component="HelpCircleOutline" /></template>
              </n-button>
            </template>
            <div style="font-size: 12px; line-height: 1.8;">
              <div><b>快捷键:</b></div>
              <div>ESC - 取消连线/取消选择</div>
              <div>Delete - 删除选中节点</div>
              <div>Ctrl+S - 保存</div>
              <div>双击节点 - 编辑名称</div>
            </div>
          </n-tooltip>
        </n-button-group>
      </div>
      <div class="toolbar-right">
        <n-space>
          <n-button :loading="saving" @click="handleSave">
            <template #icon><n-icon :component="SaveOutline" /></template>
            保存
          </n-button>
          <n-button type="primary" :disabled="pipeline?.pipelineStatus !== 1" @click="handleExecute">
            <template #icon><n-icon :component="PlayOutline" /></template>
            执行
          </n-button>
          <n-button secondary @click="showExecHistory = true">
            <template #icon><n-icon :component="TimeOutline" /></template>
            执行记录
          </n-button>
          <n-button quaternary @click="showFlowVars = true">
            <template #icon><n-icon :component="CodeWorkingOutline" /></template>
            流程变量
          </n-button>
        </n-space>
      </div>
    </div>

    <div class="designer-main">
      <!-- 左侧节点面板 -->
      <div class="node-panel" :class="{ collapsed: leftPanelCollapsed }">
        <div class="panel-header" @click="leftPanelCollapsed = !leftPanelCollapsed">
          <n-icon :component="AppsOutline" />
          <span v-if="!leftPanelCollapsed">节点类型</span>
          <n-icon :component="leftPanelCollapsed ? ChevronForwardOutline : ChevronBackOutline" class="collapse-icon" />
        </div>
        <div v-show="!leftPanelCollapsed" class="node-list">
          <!-- 数据节点 -->
          <div class="node-group-title">
            <n-icon :component="ServerOutline" size="14" />
            <span>数据节点</span>
          </div>
          <div 
            v-for="nodeType in dataNodeTypes" 
            :key="nodeType.type"
            class="node-item"
            draggable="true"
            @dragstart="onDragStart($event, nodeType)"
          >
            <div class="node-icon" :style="{ background: nodeType.gradient }">
              <n-icon :component="getIcon(nodeType.icon)" color="#fff" size="18" />
            </div>
            <div class="node-info">
              <div class="node-name">{{ nodeType.name }}</div>
              <div class="node-desc">{{ nodeType.description }}</div>
            </div>
          </div>
          
          <!-- 脚本任务 -->
          <div class="node-group-title">
            <n-icon :component="CodeOutline" size="14" />
            <span>脚本任务</span>
          </div>
          <div 
            v-for="nodeType in scriptNodeTypes" 
            :key="nodeType.type"
            class="node-item"
            draggable="true"
            @dragstart="onDragStart($event, nodeType)"
          >
            <div class="node-icon" :style="{ background: nodeType.gradient }">
              <n-icon :component="getIcon(nodeType.icon)" color="#fff" size="18" />
            </div>
            <div class="node-info">
              <div class="node-name">{{ nodeType.name }}</div>
              <div class="node-desc">{{ nodeType.description }}</div>
            </div>
          </div>
          
          <!-- 流程控制 -->
          <div class="node-group-title">
            <n-icon :component="GitNetworkOutline" size="14" />
            <span>流程控制</span>
          </div>
          <div 
            v-for="nodeType in controlNodeTypes" 
            :key="nodeType.type"
            class="node-item"
            draggable="true"
            @dragstart="onDragStart($event, nodeType)"
          >
            <div class="node-icon" :style="{ background: nodeType.gradient }">
              <n-icon :component="getIcon(nodeType.icon)" color="#fff" size="18" />
            </div>
            <div class="node-info">
              <div class="node-name">{{ nodeType.name }}</div>
              <div class="node-desc">{{ nodeType.description }}</div>
            </div>
          </div>
          
          <!-- 通知节点 -->
          <div class="node-group-title">
            <n-icon :component="NotificationsOutline" size="14" />
            <span>通知节点</span>
          </div>
          <div 
            v-for="nodeType in notifyNodeTypes" 
            :key="nodeType.type"
            class="node-item"
            draggable="true"
            @dragstart="onDragStart($event, nodeType)"
          >
            <div class="node-icon" :style="{ background: nodeType.gradient }">
              <n-icon :component="getIcon(nodeType.icon)" color="#fff" size="18" />
            </div>
            <div class="node-info">
              <div class="node-name">{{ nodeType.name }}</div>
              <div class="node-desc">{{ nodeType.description }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间画布 -->
      <div class="canvas-container">
        <!-- 加载状态 -->
        <div v-if="loading" class="canvas-loading">
          <n-spin size="large" />
          <p>加载中...</p>
        </div>
        
        <!-- 缩放指示器 -->
        <div class="zoom-indicator">{{ Math.round(canvasScale * 100) }}%</div>
        
        <div 
          ref="canvasRef" 
          class="canvas-area"
          :style="{ transform: `scale(${canvasScale})`, transformOrigin: 'top left' }" 
          @drop="onDrop"
          @dragover.prevent
          @click="handleCanvasClick"
        >
          <!-- 网格背景 -->
          <div class="canvas-grid"></div>
          
          <!-- 提示文字 -->
          <div v-if="nodes.length === 0" class="canvas-hint">
            <n-icon :component="CloudUploadOutline" size="64" />
            <p>拖拽左侧节点到此处开始设计流程</p>
            <p class="hint-sub">支持数据源、SQL脚本、Shell、HTTP等多种任务类型</p>
          </div>

          <!-- 节点 -->
          <div 
            v-for="(node, index) in nodes" 
            :key="node.nodeCode"
            class="canvas-node"
            :class="{ 
              selected: selectedNode?.nodeCode === node.nodeCode,
              connecting: isConnecting && connectSourceNode?.nodeCode === node.nodeCode,
              disabled: node.isEnabled === 0
            }"
            :style="{ 
              left: node.positionX + 'px', 
              top: node.positionY + 'px',
              '--node-color': getNodeColor(node.nodeType)
            }"
            @click.stop="selectNode(node)"
            @mousedown="startDrag($event, node)"
            @dblclick="editNodeName(node)"
          >
            <div class="node-header" :style="{ background: getNodeGradient(node.nodeType) }">
              <n-icon :component="getIcon(getNodeIcon(node.nodeType))" color="#fff" size="16" />
              <!-- 内联编辑节点名称 -->
              <template v-if="editingNodeCode === node.nodeCode">
                <n-input
                  v-model:value="editingNodeName"
                  size="tiny"
                  style="flex: 1; min-width: 0; font-size: 12px;"
                  autofocus
                  @blur="confirmEditNodeName(node)"
                  @keyup.enter="confirmEditNodeName(node)"
                  @keyup.escape="cancelEditNodeName"
                  @click.stop
                />
              </template>
              <span v-else class="node-title">{{ node.nodeName }}</span>
              <n-badge v-if="node.isEnabled === 0" dot type="warning" />
            </div>
            <div class="node-content">
              <span class="node-type-label">{{ getNodeTypeName(node.nodeType) }}</span>
              <div v-if="hasNodeBadges(node)" class="node-badges">
                <n-tag v-if="node.priority !== undefined && node.priority !== 2" size="tiny" :type="getPriorityType(node.priority)">
                  {{ getPriorityLabel(node.priority) }}
                </n-tag>
                <n-tag v-if="node.preTaskCodes && node.preTaskCodes.length > 0" size="tiny" type="info">
                  {{ node.preTaskCodes.length }}前置
                </n-tag>
                <n-tag v-if="node.retryTimes > 0" size="tiny" type="warning">
                  重试×{{ node.retryTimes }}
                </n-tag>
                <n-tag v-if="node.timeoutFlag === 1" size="tiny" type="error">
                  {{ node.timeoutSeconds }}s
                </n-tag>
              </div>
            </div>
            <!-- 连接点 -->
            <div class="node-port port-top" @click.stop="handlePortClick(node, 'top')"></div>
            <div class="node-port port-bottom" @click.stop="handlePortClick(node, 'bottom')"></div>
            <!-- 删除按钮 -->
            <n-button 
              class="node-delete" 
              size="tiny" 
              circle 
              type="error"
              @click.stop="removeNode(index)"
            >
              <template #icon><n-icon :component="CloseOutline" size="12" /></template>
            </n-button>
          </div>

          <!-- 连线 SVG -->
          <svg class="connections-svg">
            <defs>
              <!-- 箭头 -->
              <marker id="arrowhead" markerWidth="20" markerHeight="20" refX="18" refY="10" orient="auto" markerUnits="userSpaceOnUse">
                <polygon points="6 4, 18 10, 6 16" fill="#10b981"/>
              </marker>
            </defs>
            <g v-for="(edge, idx) in edges" :key="idx" class="edge-group">
              <!-- 主线条 -->
              <path 
                :d="getEdgePath(edge)"
                fill="none"
                stroke="#10b981"
                stroke-width="2"
                stroke-linecap="round"
                marker-end="url(#arrowhead)"
                class="edge-path"
              />
              <circle 
                :cx="getEdgeCenter(edge).x"
                :cy="getEdgeCenter(edge).y"
                r="12"
                fill="#ff4d4f"
                class="edge-delete"
                @click.stop="removeEdge(idx)"
              />
              <text
                :x="getEdgeCenter(edge).x"
                :y="getEdgeCenter(edge).y + 4"
                fill="#fff"
                font-size="14"
                font-weight="bold"
                text-anchor="middle"
                class="edge-delete-text"
                @click.stop="removeEdge(idx)"
              >×</text>
            </g>
            <!-- 正在连线时的临时线 -->
            <path 
              v-if="isConnecting && connectSourceNode && tempLineEnd"
              :d="getTempLinePath()"
              fill="none"
              stroke="#f0a020"
              stroke-width="2"
              stroke-dasharray="5,5"
            />
          </svg>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="property-panel" :class="{ collapsed: rightPanelCollapsed }">
        <div class="panel-header" @click="rightPanelCollapsed = !rightPanelCollapsed">
          <n-icon :component="SettingsOutline" />
          <span v-if="!rightPanelCollapsed">{{ selectedNode ? '节点属性' : '流程配置' }}</span>
          <n-icon :component="rightPanelCollapsed ? ChevronBackOutline : ChevronForwardOutline" class="collapse-icon" />
        </div>
        
        <div v-show="!rightPanelCollapsed" class="panel-content">
          <template v-if="selectedNode">
            <n-form label-placement="top" size="small">
              <n-form-item label="节点名称">
                <n-input v-model:value="selectedNode.nodeName" />
              </n-form-item>
              
              <n-form-item label="执行顺序">
                <n-input-number v-model:value="selectedNode.sortOrder" :min="0" style="width: 100%" />
              </n-form-item>
              
              <n-form-item label="启用状态">
                <n-switch v-model:value="selectedNode.isEnabled" :checked-value="1" :unchecked-value="0">
                  <template #checked>启用</template>
                  <template #unchecked>禁用</template>
                </n-switch>
              </n-form-item>

              <n-form-item label="节点描述">
                <n-input v-model:value="selectedNode.description" type="textarea" :rows="2" placeholder="可选" />
              </n-form-item>

              <n-divider>前置任务</n-divider>
              
              <n-form-item label="前置节点">
                <n-select 
                  v-model:value="selectedNode.preTaskCodes" 
                  :options="preTaskOptions" 
                  multiple 
                  placeholder="选择前置节点（可多选）"
                  clearable
                />
              </n-form-item>

              <n-divider>高级配置</n-divider>

              <n-form-item label="失败策略">
                <n-select v-model:value="selectedNode.failStrategy" :options="failStrategyOptions" />
              </n-form-item>

              <n-form-item label="任务优先级">
                <n-select v-model:value="selectedNode.priority" :options="priorityOptions" />
              </n-form-item>

              <n-form-item label="超时设置">
                <n-switch v-model:value="selectedNode.timeoutFlag" :checked-value="1" :unchecked-value="0">
                  <template #checked>启用</template>
                  <template #unchecked>禁用</template>
                </n-switch>
              </n-form-item>

              <template v-if="selectedNode.timeoutFlag === 1">
                <n-form-item label="超时时间(秒)">
                  <n-input-number v-model:value="selectedNode.timeoutSeconds" :min="1" style="width: 100%" />
                </n-form-item>
                <n-form-item label="超时策略">
                  <n-select v-model:value="selectedNode.timeoutStrategy" :options="timeoutStrategyOptions" />
                </n-form-item>
              </template>

              <n-form-item label="重试次数">
                <n-input-number v-model:value="selectedNode.retryTimes" :min="0" :max="10" style="width: 100%" />
              </n-form-item>

              <template v-if="selectedNode.retryTimes > 0">
                <n-form-item label="重试间隔(秒)">
                  <n-input-number v-model:value="selectedNode.retryInterval" :min="1" style="width: 100%" />
                </n-form-item>
              </template>

              <n-divider>{{ getNodeTypeName(selectedNode.nodeType) }}配置</n-divider>
              
              <!-- 数据节点（数据同步） -->
              <template v-if="selectedNode.nodeType === 'data'">
                <n-form-item label="操作类型">
                  <n-radio-group v-model:value="nodeConfig.operation" @update:value="onOperationChange">
                    <n-radio-button value="sync">数据同步</n-radio-button>
                    <n-radio-button value="read">仅读取</n-radio-button>
                    <n-radio-button value="write">仅写入</n-radio-button>
                  </n-radio-group>
                </n-form-item>
                
                <!-- 数据同步模式：源+目标 -->
                <template v-if="nodeConfig.operation === 'sync'">
                  <n-divider title-placement="left" style="margin: 12px 0 8px">
                    <span class="text-secondary" style="font-size: 12px">源数据</span>
                  </n-divider>
                  <n-form-item label="源数据源">
                    <n-select v-model:value="nodeConfig.sourceDataSourceId" :options="dataSourceOptions" placeholder="选择源数据源" />
                  </n-form-item>
                  <n-form-item label="读取方式">
                    <n-radio-group v-model:value="nodeConfig.readMode">
                      <n-radio value="table">表名</n-radio>
                      <n-radio value="sql">SQL查询</n-radio>
                    </n-radio-group>
                  </n-form-item>
                  <n-form-item :label="nodeConfig.readMode === 'sql' ? 'SQL语句' : '源表名'">
                    <n-input v-model:value="nodeConfig.sourceTable" type="textarea" :rows="2" :placeholder="nodeConfig.readMode === 'sql' ? 'SELECT * FROM table_name' : '输入源表名'" />
                  </n-form-item>
                  
                  <n-divider title-placement="left" style="margin: 12px 0 8px">
                    <span class="text-secondary" style="font-size: 12px">目标数据</span>
                  </n-divider>
                  <n-form-item label="目标数据源">
                    <n-select v-model:value="nodeConfig.targetDataSourceId" :options="dataSourceOptions" placeholder="选择目标数据源" />
                  </n-form-item>
                  <n-form-item label="目标表">
                    <n-input v-model:value="nodeConfig.targetTable" placeholder="输入目标表名" />
                  </n-form-item>
                  <n-form-item label="写入模式">
                    <n-select v-model:value="nodeConfig.writeMode" :options="writeModeOptions" />
                  </n-form-item>

                  <!-- 执行引擎选择 -->
                  <n-divider title-placement="left" style="margin: 12px 0 8px">
                    <span class="text-secondary" style="font-size: 12px">执行引擎</span>
                  </n-divider>
                  <n-form-item label="引擎类型">
                    <n-radio-group v-model:value="nodeConfig.engineType">
                      <n-radio-button value="jdbc">
                        <n-space :size="4" align="center">
                          <n-icon :component="FlashOutline" />
                          JDBC（流式）
                        </n-space>
                      </n-radio-button>
                      <n-radio-button value="datax">
                        <n-space :size="4" align="center">
                          <n-icon :component="RocketOutline" />
                          DataX（进程）
                        </n-space>
                      </n-radio-button>
                    </n-radio-group>
                  </n-form-item>
                  <n-alert
                    v-if="nodeConfig.engineType === 'jdbc' || !nodeConfig.engineType"
                    type="info"
                    :show-icon="false"
                    style="margin: 0 0 12px; font-size: 12px;"
                  >
                    <strong>JDBC 引擎</strong>：轻量、启动快，使用数据库流式游标 + 批量提交。
                    适用于 <strong>中小数据量（&lt; 100万行）</strong>、频繁调度、异构表同步。
                  </n-alert>
                  <n-alert
                    v-else-if="nodeConfig.engineType === 'datax'"
                    type="warning"
                    :show-icon="false"
                    style="margin: 0 0 12px; font-size: 12px;"
                  >
                    <strong>DataX 引擎</strong>：阿里巴巴成熟框架，支持多通道并行、限速、脏数据处理。
                    适用于 <strong>大数据量（百万级以上）、需要高稳定性</strong>的场景。
                    需要在服务器上安装 DataX 并配置路径。
                  </n-alert>
                  <n-form-item label="批次大小">
                    <n-input-number
                      v-model:value="nodeConfig.batchSize"
                      :min="100"
                      :max="50000"
                      :step="500"
                      placeholder="每批提交的记录数"
                      style="width: 200px;"
                    >
                      <template #suffix>条/批</template>
                    </n-input-number>
                  </n-form-item>
                  <template v-if="nodeConfig.engineType === 'datax'">
                    <n-form-item label="并发通道">
                      <n-input-number
                        v-model:value="nodeConfig.channelCount"
                        :min="1"
                        :max="32"
                        style="width: 200px;"
                      >
                        <template #suffix>个</template>
                      </n-input-number>
                      <span class="text-secondary" style="margin-left: 8px; font-size: 12px;">DataX 并发读写通道数</span>
                    </n-form-item>
                    <n-form-item label="DataX 路径">
                      <n-input
                        v-model:value="nodeConfig.dataxHome"
                        placeholder="留空则使用服务器全局配置（datax.home）"
                        clearable
                      />
                    </n-form-item>
                  </template>
                </template>
                
                <!-- 仅读取模式 -->
                <template v-if="nodeConfig.operation === 'read'">
                  <n-form-item label="数据源">
                    <n-select v-model:value="nodeConfig.dataSourceId" :options="dataSourceOptions" placeholder="选择数据源" />
                  </n-form-item>
                  <n-form-item label="读取方式">
                    <n-radio-group v-model:value="nodeConfig.readMode">
                      <n-radio value="table">表名</n-radio>
                      <n-radio value="sql">SQL查询</n-radio>
                    </n-radio-group>
                  </n-form-item>
                  <n-form-item :label="nodeConfig.readMode === 'sql' ? 'SQL语句' : '表名'">
                    <n-input v-model:value="nodeConfig.tableName" type="textarea" :rows="3" :placeholder="nodeConfig.readMode === 'sql' ? 'SELECT * FROM table_name' : '输入表名'" />
                  </n-form-item>
                </template>
                
                <!-- 仅写入模式（接收上游数据） -->
                <template v-if="nodeConfig.operation === 'write'">
                  <n-form-item label="目标数据源">
                    <n-select v-model:value="nodeConfig.dataSourceId" :options="dataSourceOptions" placeholder="选择目标数据源" />
                  </n-form-item>
                  <n-form-item label="目标表">
                    <n-input v-model:value="nodeConfig.tableName" placeholder="输入目标表名" />
                  </n-form-item>
                  <n-form-item label="写入模式">
                    <n-select v-model:value="nodeConfig.writeMode" :options="writeModeOptions" />
                  </n-form-item>
                  <n-alert type="info" style="margin-top: 8px">
                    写入模式将接收上游节点传递的数据
                  </n-alert>
                </template>
              </template>

              <!-- 脚本节点 -->
              <template v-if="selectedNode.nodeType === 'script'">
                <n-form-item label="数据源">
                  <n-select v-model:value="nodeConfig.dataSourceId" :options="dataSourceOptions" placeholder="选择数据源" />
                </n-form-item>
                <n-form-item label="脚本内容">
                  <n-input v-model:value="nodeConfig.script" type="textarea" :rows="6" placeholder="输入SQL脚本" />
                </n-form-item>
              </template>

              <!-- Shell脚本节点 -->
              <template v-if="selectedNode.nodeType === 'shell'">
                <n-form-item label="脚本内容">
                  <n-input v-model:value="nodeConfig.script" type="textarea" :rows="8" placeholder="#!/bin/bash&#10;echo 'Hello World'" />
                </n-form-item>
                <n-form-item label="运行用户">
                  <n-input v-model:value="nodeConfig.runUser" placeholder="可选，默认当前用户" />
                </n-form-item>
              </template>

              <!-- Python脚本节点 -->
              <template v-if="selectedNode.nodeType === 'python'">
                <n-alert type="info" style="margin-bottom: 12px">
                  <template #icon><n-icon :component="LogoPython" /></template>
                  需要服务器已安装 Python 3.x，脚本通过 subprocess 执行
                </n-alert>
                <n-form-item label="Python版本">
                  <n-select v-model:value="nodeConfig.pythonVersion" :options="[{label:'python3',value:'python3'},{label:'python',value:'python'},{label:'python3.11',value:'python3.11'}]" />
                </n-form-item>
                <n-form-item label="脚本内容">
                  <n-input v-model:value="nodeConfig.script" type="textarea" :rows="10" placeholder="# -*- coding: utf-8 -*-&#10;import sys&#10;print('Hello from Python')" />
                </n-form-item>
                <n-form-item label="依赖包">
                  <n-dynamic-tags v-model:value="nodeConfig.pythonRequirements" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">执行前自动 pip install，多个包按回车分隔</n-text>
                  </template>
                </n-form-item>
                <n-form-item label="运行用户">
                  <n-input v-model:value="nodeConfig.runUser" placeholder="可选，默认当前用户" />
                </n-form-item>
              </template>

              <!-- HTTP请求节点 -->
              <template v-if="selectedNode.nodeType === 'http'">
                <n-form-item label="请求方法">
                  <n-select v-model:value="nodeConfig.httpMethod" :options="httpMethodOptions" />
                </n-form-item>
                <n-form-item label="请求URL">
                  <n-input v-model:value="nodeConfig.url" placeholder="https://api.example.com/data" />
                </n-form-item>
                <n-form-item label="请求头(JSON)">
                  <n-input v-model:value="nodeConfig.headers" type="textarea" :rows="3" placeholder='{"Content-Type": "application/json"}' />
                </n-form-item>
                <n-form-item label="请求体(JSON)">
                  <n-input v-model:value="nodeConfig.body" type="textarea" :rows="4" placeholder='{"key": "value"}' />
                </n-form-item>
                <n-form-item label="超时时间(秒)">
                  <n-input-number v-model:value="nodeConfig.connectTimeout" :min="1" :max="300" style="width: 100%" />
                </n-form-item>
              </template>

              <!-- 条件分支节点 -->
              <template v-if="selectedNode.nodeType === 'condition'">
                <n-form-item label="条件类型">
                  <n-select v-model:value="nodeConfig.conditionType" :options="conditionTypeOptions" />
                </n-form-item>
                <n-form-item label="条件表达式">
                  <n-input v-model:value="nodeConfig.conditionExpression" type="textarea" :rows="3" placeholder="${param} > 100" />
                </n-form-item>
                <n-form-item label="成功分支节点">
                  <n-select v-model:value="nodeConfig.successNode" :options="otherNodeOptions" placeholder="条件为真时执行" clearable />
                </n-form-item>
                <n-form-item label="失败分支节点">
                  <n-select v-model:value="nodeConfig.failedNode" :options="otherNodeOptions" placeholder="条件为假时执行" clearable />
                </n-form-item>
              </template>

              <!-- 子流程节点 -->
              <template v-if="selectedNode.nodeType === 'sub_process'">
                <n-form-item label="子流程">
                  <n-select v-model:value="nodeConfig.subProcessId" :options="pipelineOptions" placeholder="选择要调用的流程" />
                </n-form-item>
                <n-form-item label="传递参数(JSON)">
                  <n-input v-model:value="nodeConfig.subParams" type="textarea" :rows="3" placeholder='{"key": "value"}' />
                </n-form-item>
              </template>

              <!-- 等待节点 -->
              <template v-if="selectedNode.nodeType === 'wait'">
                <n-form-item label="等待时间">
                  <n-input-number v-model:value="nodeConfig.waitSeconds" :min="1" :max="86400" style="width: 100%">
                    <template #suffix>秒</template>
                  </n-input-number>
                </n-form-item>
                <n-alert type="info" style="margin-bottom: 12px">
                  等待指定时间后继续执行后续节点，最长支持24小时(86400秒)
                </n-alert>
              </template>

              <!-- 前置任务节点 - 等待其他流程完成 -->
              <template v-if="selectedNode.nodeType === 'depend'">
                <n-alert type="info" style="margin-bottom: 16px">
                  <template #icon><n-icon><GitMergeOutline /></n-icon></template>
                  前置任务节点用于等待其他流程执行完成后再继续执行当前流程
                </n-alert>
                
                <n-form-item label="依赖流程" required>
                  <n-select
                    v-model:value="nodeConfig.dependPipelineIds"
                    :options="dependPipelineOptions"
                    placeholder="选择要等待的流程"
                    multiple
                    filterable
                    max-tag-count="responsive"
                  />
                </n-form-item>
                
                <n-form-item label="依赖关系">
                  <n-radio-group v-model:value="nodeConfig.dependRelation">
                    <n-space vertical>
                      <n-radio value="AND">AND - 所有依赖流程都必须成功完成</n-radio>
                      <n-radio value="OR">OR - 任意一个依赖流程成功完成即可</n-radio>
                    </n-space>
                  </n-radio-group>
                </n-form-item>
                
                <n-form-item label="检查周期">
                  <n-select v-model:value="nodeConfig.checkCycle" :options="checkCycleOptions" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">检查依赖流程在哪个时间周期内的执行结果</n-text>
                  </template>
                </n-form-item>
                
                <n-divider style="margin: 12px 0">高级配置</n-divider>
                
                <n-form-item label="检查间隔">
                  <n-input-number v-model:value="nodeConfig.checkInterval" :min="5" :max="300" style="width: 100%">
                    <template #suffix>秒</template>
                  </n-input-number>
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">轮询检查依赖流程状态的时间间隔，默认10秒</n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="超时时间">
                  <n-input-number v-model:value="nodeConfig.dependTimeout" :min="0" :max="86400" style="width: 100%">
                    <template #suffix>秒 (0=不超时)</template>
                  </n-input-number>
                </n-form-item>
                
                <n-form-item label="超时策略">
                  <n-radio-group v-model:value="nodeConfig.timeoutStrategy">
                    <n-space vertical>
                      <n-radio value="WARN">仅告警 - 超时后继续等待</n-radio>
                      <n-radio value="FAIL">失败 - 超时后标记当前节点失败</n-radio>
                    </n-space>
                  </n-radio-group>
                </n-form-item>
              </template>

              <!-- 错误处理节点 -->
              <template v-if="selectedNode.nodeType === 'error_handler'">
                <n-alert type="warning" style="margin-bottom: 16px">
                  <template #icon><n-icon><ShieldCheckmarkOutline /></n-icon></template>
                  错误处理节点用于捕获上游节点的异常，支持重试和告警
                </n-alert>
                
                <n-form-item label="重试次数">
                  <n-input-number v-model:value="nodeConfig.retryCount" :min="0" :max="10" style="width: 100%" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">上游节点失败后自动重试的次数，0表示不重试</n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="重试间隔">
                  <n-input-number v-model:value="nodeConfig.retryInterval" :min="1" :max="600" style="width: 100%">
                    <template #suffix>秒</template>
                  </n-input-number>
                </n-form-item>
                
                <n-form-item label="失败策略">
                  <n-radio-group v-model:value="nodeConfig.errorStrategy">
                    <n-space vertical>
                      <n-radio value="stop">终止流程 - 重试耗尽后停止整个流程</n-radio>
                      <n-radio value="skip">跳过继续 - 忽略错误继续执行下游节点</n-radio>
                      <n-radio value="fallback">回退分支 - 执行指定的回退节点</n-radio>
                    </n-space>
                  </n-radio-group>
                </n-form-item>
                
                <n-form-item v-if="nodeConfig.errorStrategy === 'fallback'" label="回退节点">
                  <n-select v-model:value="nodeConfig.fallbackNode" :options="otherNodeOptions" placeholder="选择回退执行的节点" clearable />
                </n-form-item>
                
                <n-form-item label="告警通知">
                  <n-switch v-model:value="nodeConfig.alertOnError" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">失败时发送告警通知</n-text>
                  </template>
                </n-form-item>
              </template>

              <!-- 企业微信通知节点 -->
              <template v-if="selectedNode.nodeType === 'wecom'">
                <n-alert type="info" style="margin-bottom: 16px">
                  <template #icon><n-icon><ChatbubblesOutline /></n-icon></template>
                  通过企业微信发送通知消息，可选择已配置的通道或自定义Webhook
                </n-alert>
                
                <n-form-item label="通道配置">
                  <n-select v-model:value="nodeConfig.channelId" :options="wecomChannelOptions" placeholder="选择已配置的通道（可选）" clearable />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">选择通道配置后无需填写Webhook地址，<n-button text type="primary" size="tiny" @click="router.push('/message-channel')">管理通道</n-button></n-text>
                  </template>
                </n-form-item>
                
                <n-form-item v-if="!nodeConfig.channelId" label="Webhook地址">
                  <n-input v-model:value="nodeConfig.webhookUrl" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">未选择通道时使用此地址，留空则使用系统默认配置</n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="消息类型">
                  <n-select v-model:value="nodeConfig.msgType" :options="wecomMsgTypeOptions" />
                </n-form-item>
                
                <n-form-item label="@成员">
                  <n-dynamic-tags v-model:value="nodeConfig.mentionUsers" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">输入企微用户ID，按回车添加。输入 @all 通知所有人</n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="消息内容">
                  <n-input v-model:value="nodeConfig.content" type="textarea" :rows="4" placeholder="支持变量: ${pipeline.name}, ${node.name}, ${node.status}, ${timestamp}" />
                </n-form-item>
                
                <n-form-item label="发送条件">
                  <n-select v-model:value="nodeConfig.sendCondition" :options="notifySendConditionOptions" />
                </n-form-item>
              </template>

              <!-- 邮件通知节点 -->
              <template v-if="selectedNode.nodeType === 'email'">
                <n-alert type="info" style="margin-bottom: 16px">
                  <template #icon><n-icon><MailOutline /></n-icon></template>
                  通过 SMTP 发送邮件通知，可选择已配置的邮件通道
                </n-alert>
                
                <n-form-item label="邮件通道">
                  <n-select v-model:value="nodeConfig.channelId" :options="emailChannelOptions" placeholder="选择邮件通道（留空使用默认）" clearable />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">选择已配置的邮件通道，<n-button text type="primary" size="tiny" @click="router.push('/message-channel')">管理通道</n-button></n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="收件人">
                  <n-dynamic-tags v-model:value="nodeConfig.toList" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">输入邮箱地址，按回车添加</n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="抄送">
                  <n-dynamic-tags v-model:value="nodeConfig.ccList" />
                </n-form-item>
                
                <n-form-item label="邮件主题">
                  <n-input v-model:value="nodeConfig.subject" placeholder="支持变量: ${pipeline.name}, ${timestamp}" />
                </n-form-item>
                
                <n-form-item label="正文格式">
                  <n-radio-group v-model:value="nodeConfig.contentType">
                    <n-radio-button value="text">纯文本</n-radio-button>
                    <n-radio-button value="html">HTML</n-radio-button>
                  </n-radio-group>
                </n-form-item>
                
                <n-form-item label="邮件正文">
                  <n-input v-model:value="nodeConfig.content" type="textarea" :rows="5" placeholder="支持变量: ${pipeline.name}, ${node.name}, ${node.status}, ${timestamp}" />
                </n-form-item>
                
                <n-form-item label="发送条件">
                  <n-select v-model:value="nodeConfig.sendCondition" :options="notifySendConditionOptions" />
                </n-form-item>
              </template>

              <!-- 短信通知节点 -->
              <template v-if="selectedNode.nodeType === 'sms'">
                <n-alert type="info" style="margin-bottom: 16px">
                  <template #icon><n-icon><PhonePortraitOutline /></n-icon></template>
                  通过短信 API 发送通知，可选择已配置的短信通道
                </n-alert>
                
                <n-form-item label="短信通道">
                  <n-select v-model:value="nodeConfig.channelId" :options="smsChannelOptions" placeholder="选择短信通道（留空使用默认）" clearable />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">选择已配置的短信通道，<n-button text type="primary" size="tiny" @click="router.push('/message-channel')">管理通道</n-button></n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="手机号列表">
                  <n-dynamic-tags v-model:value="nodeConfig.phoneNumbers" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">输入手机号，按回车添加</n-text>
                  </template>
                </n-form-item>
                
                <n-form-item label="发送方式">
                  <n-radio-group v-model:value="nodeConfig.smsMode">
                    <n-radio-button value="content">直接内容</n-radio-button>
                    <n-radio-button value="template">模板发送</n-radio-button>
                  </n-radio-group>
                </n-form-item>
                
                <!-- 直接内容模式 -->
                <n-form-item v-if="nodeConfig.smsMode !== 'template'" label="短信内容">
                  <n-input v-model:value="nodeConfig.content" type="textarea" :rows="3" placeholder="短信内容，支持变量：${pipeline.name} ${node.name} ${timestamp}" />
                </n-form-item>
                
                <!-- 模板模式（云服务商） -->
                <template v-if="nodeConfig.smsMode === 'template'">
                  <n-form-item label="模板ID">
                    <n-input v-model:value="nodeConfig.templateId" placeholder="云服务商的短信模板ID" />
                  </n-form-item>
                  
                  <n-form-item label="模板参数">
                    <div style="width: 100%">
                      <div v-for="(param, index) in (nodeConfig.templateParams || [])" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px">
                        <n-input v-model:value="param.key" placeholder="参数名" size="small" style="width: 120px" />
                        <n-input v-model:value="param.value" placeholder="参数值（支持变量）" size="small" style="flex: 1" />
                        <n-button size="tiny" quaternary @click="nodeConfig.templateParams.splice(index, 1)">
                          <template #icon><n-icon :component="CloseOutline" size="14" /></template>
                        </n-button>
                      </div>
                      <n-button size="small" dashed block @click="if (!nodeConfig.templateParams) nodeConfig.templateParams = []; nodeConfig.templateParams.push({ key: '', value: '' })">
                        <template #icon><n-icon :component="AddOutline" /></template>
                        添加参数
                      </n-button>
                    </div>
                  </n-form-item>
                </template>
                
                <n-form-item label="发送条件">
                  <n-select v-model:value="nodeConfig.sendCondition" :options="notifySendConditionOptions" />
                </n-form-item>
              </template>

              <!-- 钉钉通知节点 -->
              <template v-if="selectedNode.nodeType === 'dingtalk'">
                <n-alert type="info" style="margin-bottom: 16px">
                  <template #icon><n-icon><MegaphoneOutline /></n-icon></template>
                  通过钉钉发送通知，可选择已配置的通道或自定义Webhook
                </n-alert>
                
                <n-form-item label="通道配置">
                  <n-select v-model:value="nodeConfig.channelId" :options="dingtalkChannelOptions" placeholder="选择已配置的通道（可选）" clearable />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">选择通道配置后无需填写Webhook地址，<n-button text type="primary" size="tiny" @click="router.push('/message-channel')">管理通道</n-button></n-text>
                  </template>
                </n-form-item>
                
                <n-form-item v-if="!nodeConfig.channelId" label="Webhook地址">
                  <n-input v-model:value="nodeConfig.webhookUrl" placeholder="钉钉机器人Webhook地址" />
                </n-form-item>
                
                <n-form-item v-if="!nodeConfig.channelId" label="加签密钥">
                  <n-input v-model:value="nodeConfig.secret" placeholder="安全设置中的加签密钥（可选）" type="password" show-password-on="click" />
                </n-form-item>
                
                <n-form-item label="消息类型">
                  <n-select v-model:value="nodeConfig.msgType" :options="dingtalkMsgTypeOptions" />
                </n-form-item>
                
                <n-form-item v-if="nodeConfig.msgType === 'markdown' || nodeConfig.msgType === 'actionCard'" label="标题">
                  <n-input v-model:value="nodeConfig.title" placeholder="消息标题" />
                </n-form-item>
                
                <n-form-item label="消息内容">
                  <n-input v-model:value="nodeConfig.content" type="textarea" :rows="4" placeholder="消息内容，支持变量：${pipeline.name} ${node.name} ${timestamp}" />
                </n-form-item>
                
                <n-form-item label="@手机号">
                  <n-dynamic-tags v-model:value="nodeConfig.mentionMobiles" />
                  <template #feedback>
                    <n-text depth="3" style="font-size: 12px">输入手机号按回车添加，可@指定成员</n-text>
                  </template>
                </n-form-item>
                
                <template v-if="nodeConfig.msgType === 'actionCard'">
                  <n-form-item label="按钮标题">
                    <n-input v-model:value="nodeConfig.btnTitle" placeholder="按钮文字，如：查看详情" />
                  </n-form-item>
                  <n-form-item label="按钮链接">
                    <n-input v-model:value="nodeConfig.btnUrl" placeholder="点击按钮跳转的URL" />
                  </n-form-item>
                </template>
                
                <n-form-item label="发送条件">
                  <n-select v-model:value="nodeConfig.sendCondition" :options="notifySendConditionOptions" />
                </n-form-item>
              </template>

              <div v-if="['wecom', 'email', 'sms', 'dingtalk'].includes(selectedNode?.nodeType)" style="margin-bottom: 12px">
                <n-button block :loading="testSending" @click="handleTestNotify">
                  <template #icon><n-icon :component="PlayOutline" /></template>
                  测试发送
                </n-button>
              </div>
              <n-button type="primary" block @click="saveNodeConfig">
                <template #icon><n-icon :component="SaveOutline" /></template>
                保存配置
              </n-button>
            </n-form>
          </template>

          <template v-else-if="pipeline">
            <div class="pipeline-info">
              <div class="info-item">
                <label>流程名称</label>
                <span>{{ pipeline.pipelineName }}</span>
              </div>
              <div class="info-item">
                <label>流程编码</label>
                <span class="code">{{ pipeline.pipelineCode }}</span>
              </div>
              <div class="info-item">
                <label>调度方式</label>
                <n-tag size="small">{{ getScheduleText(pipeline.scheduleType) }}</n-tag>
              </div>
              <div v-if="pipeline.cronExpression" class="info-item">
                <label>Cron表达式</label>
                <span class="code">{{ pipeline.cronExpression }}</span>
              </div>
              <div class="info-item">
                <label>节点数量</label>
                <span>{{ nodes.length }} 个</span>
              </div>
              <div class="info-item">
                <label>连线数量</label>
                <span>{{ edges.length }} 条</span>
              </div>
              
              <n-divider>全局参数</n-divider>
              <div class="global-params">
                <div v-for="(param, index) in globalParams" :key="index" class="param-row">
                  <n-input v-model:value="param.name" placeholder="参数名" size="small" style="width: 80px" />
                  <n-input v-model:value="param.value" placeholder="默认值" size="small" style="flex: 1" />
                  <n-button size="tiny" quaternary @click="removeGlobalParam(index)">
                    <template #icon><n-icon :component="CloseOutline" size="14" /></template>
                  </n-button>
                </div>
                <n-button size="small" dashed block @click="addGlobalParam">
                  <template #icon><n-icon :component="AddOutline" /></template>
                  添加参数
                </n-button>
              </div>
              
              <n-divider>执行策略</n-divider>
              <n-form label-placement="left" label-width="80" size="small">
                <n-form-item label="并行度">
                  <n-input-number v-model:value="pipelineConfig.parallelism" :min="1" :max="10" size="small" style="width: 100%" />
                </n-form-item>
                <n-form-item label="失败策略">
                  <n-select v-model:value="pipelineConfig.failureStrategy" :options="pipelineFailStrategyOptions" size="small" />
                </n-form-item>
                <n-form-item label="告警通知">
                  <n-switch v-model:value="pipelineConfig.alertEnabled" size="small" />
                </n-form-item>
              </n-form>
            </div>
          </template>
        </div>
      </div>
    </div>
    <!-- 流程变量抽屉 -->
    <n-drawer v-model:show="showFlowVars" :width="480" placement="right">
      <n-drawer-content title="流程变量" closable>
        <template #header-extra>
          <n-button size="small" type="primary" @click="addFlowVar">
            <template #icon><n-icon :component="AddOutline" /></template>
            添加
          </n-button>
        </template>
        <n-empty v-if="flowVars.length === 0" description="暂无流程变量，点击添加创建" style="padding:40px 0;" />
        <n-list v-else hoverable>
          <n-list-item v-for="(v, idx) in flowVars" :key="idx">
            <n-space vertical :size="4" style="width:100%;">
              <n-space :size="8" align="center">
                <n-input v-model:value="v.name" placeholder="变量名" size="small" style="width:140px;" />
                <n-select v-model:value="v.type" :options="flowVarTypeOptions" size="small" style="width:100px;" />
                <n-button size="tiny" quaternary type="error" @click="flowVars.splice(idx, 1)">
                  <template #icon><n-icon :component="TrashOutline" /></template>
                </n-button>
              </n-space>
              <n-input v-model:value="v.defaultValue" placeholder="默认值（可选）" size="small" />
              <n-input v-model:value="v.description" placeholder="描述（可选）" size="small" />
            </n-space>
          </n-list-item>
        </n-list>
        <template #footer>
          <n-text depth="3" style="font-size:12px;">变量可在节点配置中通过 {'{'}variableName{'}'} 语法引用</n-text>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 执行记录抽屉 -->
    <n-drawer v-model:show="showExecHistory" :width="520" placement="right">
      <n-drawer-content title="执行记录" closable>
        <template #header-extra>
          <n-button text size="small" @click="fetchExecHistory">刷新</n-button>
        </template>
        <n-spin :show="execHistoryLoading">
          <n-empty v-if="execHistory.length === 0" description="暂无执行记录" style="padding:40px 0;" />
          <n-list v-else hoverable>
            <n-list-item v-for="item in execHistory" :key="item.id">
              <n-thing>
                <template #header>
                  <n-space align="center" :size="8">
                    <span>#{{ item.id }}</span>
                    <n-tag :type="item.status === 'success' ? 'success' : item.status === 'running' ? 'info' : item.status === 'failed' ? 'error' : 'default'" size="small">
                      {{ item.status === 'success' ? '成功' : item.status === 'running' ? '运行中' : item.status === 'failed' ? '失败' : item.status }}
                    </n-tag>
                  </n-space>
                </template>
                <template #description>
                  <n-space :size="12" style="font-size:12px;color:#94a3b8;">
                    <span>开始: {{ item.startTime || '-' }}</span>
                    <span>耗时: {{ item.duration || '-' }}</span>
                    <span>触发: {{ item.triggerType === 'manual' ? '手动' : '调度' }}</span>
                  </n-space>
                </template>
              </n-thing>
            </n-list-item>
          </n-list>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import DesktopOnlyTip from '@/components/mobile/DesktopOnlyTip.vue'
import { useAppStore } from '@/stores/app'
import { 
  ArrowBackOutline, SaveOutline, PlayOutline, CloseOutline, LinkOutline,
  ServerOutline, CodeOutline, DownloadOutline, AppsOutline, SettingsOutline, GridOutline,
  TrashOutline, CloudUploadOutline, TerminalOutline, GlobeOutline, GitBranchOutline, GitNetworkOutline,
  AddOutline, RemoveOutline, ScanOutline, ChevronBackOutline, ChevronForwardOutline, HelpCircleOutline, TimeOutline, GitMergeOutline,
  ShieldCheckmarkOutline, MailOutline, ChatbubblesOutline, PhonePortraitOutline, NotificationsOutline,
  MegaphoneOutline, CodeWorkingOutline, LogoPython,
  FlashOutline, RocketOutline
} from '@vicons/ionicons5'
import { getPipeline, getPipelineDesign, savePipelineDesign, executePipeline, getPipelines, testNotify } from '@/api/pipeline'
import request from '@/api/request'
import { getDataSourceList } from '@/api/dataSource'
import { getEnabledChannels } from '@/api/messageChannel'

const appStore = useAppStore()
const isMobileView = computed(() => appStore.isMobileView)
const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const canvasRef = ref<HTMLElement>()
const pipelineId = computed(() => Number(route.params["id"]))
const pipeline = ref<any>(null)
const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const selectedNode = ref<any>(null)
const nodeConfig = ref<any>({})
const dataSources = ref<any[]>([])
const saving = ref(false)
const loading = ref(false)
const testSending = ref(false)
const showExecHistory = ref(false)
const execHistory = ref<any[]>([])
const execHistoryLoading = ref(false)

// 流程变量
const showFlowVars = ref(false)
const flowVars = ref<Array<{ name: string; type: string; defaultValue: string; description: string }>>([])
const flowVarTypeOptions = [
  { label: 'String', value: 'string' },
  { label: 'Number', value: 'number' },
  { label: 'Boolean', value: 'boolean' },
  { label: 'Date', value: 'date' },
  { label: 'JSON', value: 'json' }
]
const addFlowVar = () => {
  flowVars.value.push({ name: '', type: 'string', defaultValue: '', description: '' })
}

const fetchExecHistory = async () => {
  if (!pipelineId.value) return
  execHistoryLoading.value = true
  try {
    const res = await request.get(`/pipelines/${pipelineId.value}/executions`, { params: { page: 1, pageSize: 20 } })
    execHistory.value = (res as any).data?.records || (res as any).data || []
  } catch { execHistory.value = [] }
  finally { execHistoryLoading.value = false }
}

// 面板折叠状态
const leftPanelCollapsed = ref(false)
const rightPanelCollapsed = ref(false)

// 画布缩放
const canvasScale = ref(1)
const tempLineEnd = ref<{x: number, y: number} | null>(null)

// 全局参数
const globalParams = ref<{name: string, value: string}[]>([])
const pipelineConfig = ref({
  parallelism: 1,
  failureStrategy: 'end',
  alertEnabled: false
})

const pipelineFailStrategyOptions = [
  { label: '结束流程', value: 'end' },
  { label: '继续执行', value: 'continue' }
]

const addGlobalParam = () => {
  globalParams.value.push({ name: '', value: '' })
}

const removeGlobalParam = (index: number) => {
  globalParams.value.splice(index, 1)
}

const nodeTypes = [
  { type: 'data', name: '数据节点', icon: 'ServerOutline', color: '#18a058', gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)', description: '读取/写入数据库' },
  { type: 'script', name: 'SQL脚本', icon: 'CodeOutline', color: '#d03050', gradient: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)', description: '执行SQL语句' },
  { type: 'shell', name: 'Shell脚本', icon: 'TerminalOutline', color: '#2080f0', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', description: '执行Shell命令' },
  { type: 'python', name: 'Python脚本', icon: 'LogoPython', color: '#3776ab', gradient: 'linear-gradient(135deg, #3776ab 0%, #ffd343 100%)', description: '执行Python代码' },
  { type: 'http', name: 'HTTP请求', icon: 'GlobeOutline', color: '#f0a020', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)', description: '调用HTTP接口' },
  { type: 'condition', name: '条件分支', icon: 'GitBranchOutline', color: '#722ed1', gradient: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)', description: '条件判断分支' },
  { type: 'sub_process', name: '子流程', icon: 'GitNetworkOutline', color: '#13c2c2', gradient: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)', description: '调用其他流程' },
  { type: 'wait', name: '等待', icon: 'TimeOutline', color: '#8c8c8c', gradient: 'linear-gradient(135deg, #bdc3c7 0%, #2c3e50 100%)', description: '等待指定时间' },
  { type: 'depend', name: '前置任务', icon: 'GitMergeOutline', color: '#eb2f96', gradient: 'linear-gradient(135deg, #f5576c 0%, #f093fb 100%)', description: '等待其他流程完成' },
  { type: 'error_handler', name: '错误处理', icon: 'ShieldCheckmarkOutline', color: '#cf1322', gradient: 'linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%)', description: '异常捕获与重试' },
  { type: 'wecom', name: '企业微信', icon: 'ChatbubblesOutline', color: '#07c160', gradient: 'linear-gradient(135deg, #11998e 0%, #07c160 100%)', description: '企微Webhook通知' },
  { type: 'email', name: '邮件通知', icon: 'MailOutline', color: '#1890ff', gradient: 'linear-gradient(135deg, #667eea 0%, #1890ff 100%)', description: 'SMTP邮件发送' },
  { type: 'sms', name: '短信通知', icon: 'PhonePortraitOutline', color: '#fa8c16', gradient: 'linear-gradient(135deg, #f5af19 0%, #f12711 100%)', description: '短信API通知' },
  { type: 'dingtalk', name: '钉钉通知', icon: 'MegaphoneOutline', color: '#1677ff', gradient: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)', description: '钉钉Webhook通知' }
]

const iconMap: any = { 
  ServerOutline, CodeOutline, DownloadOutline, LinkOutline, 
  TerminalOutline, GlobeOutline, GitBranchOutline, GitNetworkOutline, TimeOutline, GitMergeOutline,
  ShieldCheckmarkOutline, MailOutline, ChatbubblesOutline, PhonePortraitOutline, MegaphoneOutline, LogoPython
}

// 节点分组
const dataNodeTypes = computed(() => nodeTypes.filter(n => ['data'].includes(n.type)))
const scriptNodeTypes = computed(() => nodeTypes.filter(n => ['script', 'shell', 'python', 'http'].includes(n.type)))
const controlNodeTypes = computed(() => nodeTypes.filter(n => ['condition', 'sub_process', 'wait', 'depend', 'error_handler'].includes(n.type)))
const notifyNodeTypes = computed(() => nodeTypes.filter(n => ['wecom', 'email', 'sms', 'dingtalk'].includes(n.type)))

// 可依赖的流程列表（排除当前流程）
const dependPipelineOptions = computed(() => {
  return pipelineOptions.value.filter(p => p.value !== pipeline.value?.id)
})

// 检查周期选项
const checkCycleOptions = [
  { label: '当天 - 检查今天的执行结果', value: 'day' },
  { label: '本周 - 检查本周的执行结果', value: 'week' },
  { label: '本月 - 检查本月的执行结果', value: 'month' },
  { label: '最近一次 - 检查最近一次执行结果', value: 'last' }
]

const writeModeOptions = [
  { label: '插入(INSERT)', value: 'insert' },
  { label: '更新(UPDATE)', value: 'update' },
  { label: '替换(REPLACE)', value: 'replace' }
]

const failStrategyOptions = [
  { label: '停止流程', value: 0 },
  { label: '继续执行', value: 1 }
]

const priorityOptions = [
  { label: '最高', value: 0 },
  { label: '高', value: 1 },
  { label: '中', value: 2 },
  { label: '低', value: 3 },
  { label: '最低', value: 4 }
]

const timeoutStrategyOptions = [
  { label: '仅告警', value: 0 },
  { label: '超时失败', value: 1 }
]

const httpMethodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' }
]

const conditionTypeOptions = [
  { label: '表达式判断', value: 'expression' },
  { label: '前置任务状态', value: 'task_status' }
]

// 企微消息类型
const wecomMsgTypeOptions = [
  { label: '文本(text)', value: 'text' },
  { label: 'Markdown', value: 'markdown' },
  { label: '图片(image)', value: 'image' }
]

// 钉钉消息类型
const dingtalkMsgTypeOptions = [
  { label: '文本消息', value: 'text' },
  { label: 'Markdown', value: 'markdown' },
  { label: 'ActionCard', value: 'actionCard' }
]

// 通知发送条件
const notifySendConditionOptions = [
  { label: '始终发送', value: 'always' },
  { label: '仅在流程成功时', value: 'success' },
  { label: '仅在流程失败时', value: 'failure' }
]

// 通道配置选项
const emailChannelOptions = ref<Array<{ label: string; value: number }>>([])
const wecomChannelOptions = ref<Array<{ label: string; value: number }>>([])
const dingtalkChannelOptions = ref<Array<{ label: string; value: number }>>([])
const smsChannelOptions = ref<Array<{ label: string; value: number }>>([])

const loadChannelOptions = async () => {
  try {
    const res = await getEnabledChannels() as any
    const grouped = res.data || {}
    emailChannelOptions.value = (grouped.email || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ' (默认)' : ''), value: c.id }))
    wecomChannelOptions.value = (grouped.wecom || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ' (默认)' : ''), value: c.id }))
    dingtalkChannelOptions.value = (grouped.dingtalk || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ' (默认)' : ''), value: c.id }))
    smsChannelOptions.value = (grouped.sms || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ' (默认)' : ''), value: c.id }))
  } catch (error) {
    console.error('加载通道配置失败', error)
  }
}

// 其他节点选项（用于条件分支）
const otherNodeOptions = computed(() => {
  if (!selectedNode.value) return []
  return nodes.value
    .filter(n => n.nodeCode !== selectedNode.value.nodeCode)
    .map(n => ({ label: n.nodeName, value: n.nodeCode }))
})

// 流程列表（用于子流程）
const pipelineOptions = ref<any[]>([])

// 前置任务选项（排除当前节点）
const preTaskOptions = computed(() => {
  if (!selectedNode.value) return []
  return nodes.value
    .filter(n => n.nodeCode !== selectedNode.value.nodeCode)
    .map(n => ({ label: n.nodeName, value: n.nodeCode }))
})

const dataSourceOptions = computed(() => dataSources.value.map(ds => ({ label: ds.name, value: ds.id })))

const isConnecting = ref(false)
const connectSourceNode = ref<any>(null)

const getIcon = (name: string) => iconMap[name] || ServerOutline
const getNodeColor = (type: string) => nodeTypes.find(n => n.type === type)?.color || '#999'
const getNodeGradient = (type: string) => nodeTypes.find(n => n.type === type)?.gradient || '#999'
const getNodeIcon = (type: string) => nodeTypes.find(n => n.type === type)?.icon || 'ServerOutline'
const getNodeTypeName = (type: string) => nodeTypes.find(n => n.type === type)?.name || type

const getStatusType = (status: number) => ({ 0: 'default', 1: 'success', 2: 'warning' }[status] || 'default')
const getStatusText = (status: number) => ({ 0: '草稿', 1: '已发布', 2: '已停用' }[status] || '未知')
const getScheduleText = (type: number) => ({ 0: '手动执行', 1: '定时执行', 2: '事件触发' }[type] || '未知')
const getPriorityType = (p: number) => ({ 0: 'error', 1: 'warning', 2: 'default', 3: 'info', 4: 'default' }[p] || 'default') as any
const getPriorityLabel = (p: number) => ({ 0: '最高', 1: '高', 2: '中', 3: '低', 4: '最低' }[p] || '')

// 检查节点是否有徽章
const hasNodeBadges = (node: any) => {
  return (node.priority !== undefined && node.priority !== 2) ||
         (node.preTaskCodes && node.preTaskCodes.length > 0) ||
         node.retryTimes > 0 ||
         node.timeoutFlag === 1
}

// 缩放功能
const zoomIn = () => {
  canvasScale.value = Math.min(canvasScale.value + 0.1, 2)
}
const zoomOut = () => {
  canvasScale.value = Math.max(canvasScale.value - 0.1, 0.5)
}
const resetZoom = () => {
  canvasScale.value = 1
}

// 临时连线路径
const getTempLinePath = () => {
  if (!connectSourceNode.value || !tempLineEnd.value) return ''
  const source = getNodeCenter(connectSourceNode.value.nodeCode)
  return `M ${source.x} ${source.y + 40} L ${tempLineEnd.value.x} ${tempLineEnd.value.y}`
}

// 端口点击处理
const handlePortClick = (node: any, _port: string) => {
  if (!isConnecting.value) {
    isConnecting.value = true
    connectSourceNode.value = node
    message.info('点击目标节点完成连线')
  }
}

// 内联编辑节点名称（替换 prompt()）
const editingNodeCode = ref<string | null>(null)
const editingNodeName = ref('')

const editNodeName = (node: any) => {
  editingNodeCode.value = node.nodeCode
  editingNodeName.value = node.nodeName
}

const confirmEditNodeName = (node: any) => {
  if (editingNodeName.value.trim()) {
    node.nodeName = editingNodeName.value.trim()
  }
  editingNodeCode.value = null
}

const cancelEditNodeName = () => {
  editingNodeCode.value = null
}

const getNodeCenter = (nodeCode: string) => {
  const node = nodes.value.find(n => n.nodeCode === nodeCode)
  if (!node) return { x: 0, y: 0 }
  return { x: node.positionX + 90, y: node.positionY + 40 }
}

const getEdgePath = (edge: any) => {
  const source = getNodeCenter(edge.source)
  const target = getNodeCenter(edge.target)
  // 从源节点底部到目标节点顶部，留出箭头空间
  const startY = source.y + 45
  const endY = target.y - 50  // 留出箭头空间
  const midY = (startY + endY) / 2
  return `M ${source.x} ${startY} C ${source.x} ${midY}, ${target.x} ${midY}, ${target.x} ${endY}`
}

const getEdgeCenter = (edge: any) => {
  const source = getNodeCenter(edge.source)
  const target = getNodeCenter(edge.target)
  return { x: (source.x + target.x) / 2, y: (source.y + target.y) / 2 }
}

const toggleConnectMode = () => {
  isConnecting.value = !isConnecting.value
  connectSourceNode.value = null
  if (isConnecting.value) {
    message.info('点击源节点，再点击目标节点完成连线')
  }
}

const onDragStart = (e: DragEvent, nodeType: any) => {
  e.dataTransfer?.setData('nodeType', JSON.stringify(nodeType))
}

const onDrop = (e: DragEvent) => {
  const data = e.dataTransfer?.getData('nodeType')
  if (!data) return
  
  const nodeType = JSON.parse(data)
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return
  
  // 修复：考虑容器滚动偏移，确保位置准确
  const container = canvasRef.value?.parentElement as HTMLElement | null
  const scrollLeft = container?.scrollLeft || 0
  const scrollTop = container?.scrollTop || 0
  
  const newNode = {
    nodeCode: `node_${Date.now()}`,
    nodeName: `${nodeType.name}_${nodes.value.length + 1}`,
    nodeType: nodeType.type,
    nodeConfig: '{}',
    positionX: Math.max(0, (e.clientX - rect.left + scrollLeft) / canvasScale.value - 90),
    positionY: Math.max(0, (e.clientY - rect.top + scrollTop) / canvasScale.value - 40),
    sortOrder: nodes.value.length,
    isEnabled: 1,
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
  nodes.value.push(newNode)
  // 移除自动连线：用户通过连线模式手动连接，避免不必要的默认连接
}

let dragNode: any = null
let dragOffset = { x: 0, y: 0 }

const startDrag = (e: MouseEvent, node: any) => {
  if (isConnecting.value) return
  dragNode = node
  dragOffset = { x: e.offsetX, y: e.offsetY }
  
  const onMove = (me: MouseEvent) => {
    if (!dragNode) return
    const rect = canvasRef.value?.getBoundingClientRect()
    if (!rect) return
    dragNode.positionX = Math.max(0, me.clientX - rect.left - dragOffset.x)
    dragNode.positionY = Math.max(0, me.clientY - rect.top - dragOffset.y)
  }
  
  const onUp = () => {
    dragNode = null
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

const selectNode = (node: any) => {
  if (isConnecting.value) {
    if (!connectSourceNode.value) {
      connectSourceNode.value = node
      message.info(`已选择: ${node.nodeName}`)
    } else if (connectSourceNode.value.nodeCode !== node.nodeCode) {
      const exists = edges.value.some(e => e.source === connectSourceNode.value.nodeCode && e.target === node.nodeCode)
      if (!exists) {
        edges.value.push({ source: connectSourceNode.value.nodeCode, target: node.nodeCode })
        // 自动设置前置任务
        if (!node.preTaskCodes) node.preTaskCodes = []
        if (!node.preTaskCodes.includes(connectSourceNode.value.nodeCode)) {
          node.preTaskCodes.push(connectSourceNode.value.nodeCode)
        }
        message.success('连线成功')
      } else {
        message.warning('连线已存在')
      }
      connectSourceNode.value = null
    }
    return
  }
  
  selectedNode.value = node
  // 初始化默认值
  if (node.failStrategy === undefined) node.failStrategy = 0
  if (node.priority === undefined) node.priority = 2
  if (node.timeoutFlag === undefined) node.timeoutFlag = 0
  if (node.timeoutSeconds === undefined) node.timeoutSeconds = 300
  if (node.timeoutStrategy === undefined) node.timeoutStrategy = 0
  if (node.retryTimes === undefined) node.retryTimes = 0
  if (node.retryInterval === undefined) node.retryInterval = 1
  if (!node.preTaskCodes) node.preTaskCodes = []
  
  try {
    nodeConfig.value = node.nodeConfig ? JSON.parse(node.nodeConfig) : {}
  } catch {
    nodeConfig.value = {}
  }
  
  // 数据节点默认操作类型
  if (node.nodeType === 'data' && !nodeConfig.value.operation) {
    nodeConfig.value.operation = 'read'
  }
  
  // 前置任务节点：设置默认值
  if (node.nodeType === 'depend') {
    if (!nodeConfig.value.dependRelation) nodeConfig.value.dependRelation = 'AND'
    if (!nodeConfig.value.checkCycle) nodeConfig.value.checkCycle = 'day'
    if (!nodeConfig.value.checkInterval) nodeConfig.value.checkInterval = 10
    if (nodeConfig.value.dependTimeout === undefined) nodeConfig.value.dependTimeout = 0
    if (!nodeConfig.value.timeoutStrategy) nodeConfig.value.timeoutStrategy = 'FAIL'
  }
  
  // Python 节点默认值
  if (node.nodeType === 'python') {
    if (!nodeConfig.value.pythonVersion) nodeConfig.value.pythonVersion = 'python3'
    if (!nodeConfig.value.pythonRequirements) nodeConfig.value.pythonRequirements = []
  }
  
  // 通知节点默认值
  if (node.nodeType === 'wecom') {
    if (!nodeConfig.value.msgType) nodeConfig.value.msgType = 'text'
    if (!nodeConfig.value.sendCondition) nodeConfig.value.sendCondition = 'always'
    if (!nodeConfig.value.mentionUsers) nodeConfig.value.mentionUsers = []
  }
  if (node.nodeType === 'email') {
    if (!nodeConfig.value.contentType) nodeConfig.value.contentType = 'text'
    if (!nodeConfig.value.sendCondition) nodeConfig.value.sendCondition = 'always'
    if (!nodeConfig.value.toList) nodeConfig.value.toList = []
    if (!nodeConfig.value.ccList) nodeConfig.value.ccList = []
  }
  if (node.nodeType === 'sms') {
    if (!nodeConfig.value.sendCondition) nodeConfig.value.sendCondition = 'always'
    if (!nodeConfig.value.phoneNumbers) nodeConfig.value.phoneNumbers = []
    if (!nodeConfig.value.templateParams) nodeConfig.value.templateParams = []
  }
}

// 操作类型切换时 - 不重置已填写的数据，只设置默认值
const onOperationChange = (newOperation: string) => {
  // 只设置当前模式需要的默认值，不清空已有数据
  if (!nodeConfig.value.readMode) nodeConfig.value.readMode = 'table'
  if (!nodeConfig.value.writeMode) nodeConfig.value.writeMode = 'insert'
  // sync 模式的引擎默认值
  if (newOperation === 'sync') {
    if (!nodeConfig.value.engineType) nodeConfig.value.engineType = 'jdbc'
    if (nodeConfig.value.batchSize == null) nodeConfig.value.batchSize = 1000
    if (nodeConfig.value.channelCount == null) nodeConfig.value.channelCount = 3
  }
}

const handleCanvasClick = () => {
  if (!isConnecting.value) {
    selectedNode.value = null
  }
}

const removeNode = (index: number) => {
  const node = nodes.value[index]
  edges.value = edges.value.filter(e => e.source !== node.nodeCode && e.target !== node.nodeCode)
  nodes.value.splice(index, 1)
  if (selectedNode.value?.nodeCode === node.nodeCode) {
    selectedNode.value = null
  }
}

const removeEdge = (index: number) => {
  const edge = edges.value[index]
  // 同时移除目标节点的前置任务依赖
  const targetNode = nodes.value.find(n => n.nodeCode === edge.target)
  if (targetNode && targetNode.preTaskCodes) {
    const idx = targetNode.preTaskCodes.indexOf(edge.source)
    if (idx > -1) {
      targetNode.preTaskCodes.splice(idx, 1)
    }
  }
  edges.value.splice(index, 1)
  message.success('连线已删除')
}

const handleTestNotify = async () => {
  if (!selectedNode.value) return
  const channel = selectedNode.value.nodeType
  if (!['wecom', 'email', 'sms', 'dingtalk'].includes(channel)) {
    message.warning('当前节点不支持测试发送')
    return
  }
  testSending.value = true
  try {
    const res = await testNotify(channel, { ...nodeConfig.value })
    if (res.data?.code === 200) {
      message.success(res.data.data || '测试发送成功')
    } else {
      message.error(res.data?.message || '测试发送失败')
    }
  } catch (e: any) {
    message.error('测试发送失败: ' + (e.response?.data?.message || e.message))
  } finally {
    testSending.value = false
  }
}

const saveNodeConfig = async () => {
  if (!selectedNode.value) return
  
  const nodeType = selectedNode.value.nodeType
  const config = nodeConfig.value
  
  // 校验必填项
  if (nodeType === 'script') {
    if (!config.dataSourceId) {
      message.warning('请选择数据源')
      return
    }
    if (!config.script || !config.script.trim()) {
      message.warning('请输入SQL脚本')
      return
    }
  }
  
  if (nodeType === 'data') {
    if (config.operation === 'sync') {
      if (!config.sourceDataSourceId) {
        message.warning('请选择源数据源')
        return
      }
      if (!config.targetDataSourceId) {
        message.warning('请选择目标数据源')
        return
      }
      if (!config.targetTable || !config.targetTable.trim()) {
        message.warning('请输入目标表名')
        return
      }
      if (!config.sourceTable || !config.sourceTable.trim()) {
        message.warning('请输入源表名或 SQL')
        return
      }
    } else if (config.operation === 'read') {
      if (!config.dataSourceId) {
        message.warning('请选择数据源')
        return
      }
    } else if (config.operation === 'write') {
      if (!config.dataSourceId) {
        message.warning('请选择目标数据源')
        return
      }
      if (!config.tableName || !config.tableName.trim()) {
        message.warning('请输入目标表名')
        return
      }
    }
  }
  
  if (nodeType === 'http') {
    if (!config.url || !config.url.trim()) {
      message.warning('请输入请求URL')
      return
    }
  }
  
  if (nodeType === 'wait') {
    if (!config.waitSeconds || config.waitSeconds < 1) {
      message.warning('请设置等待时间')
      return
    }
  }
  
  if (nodeType === 'depend') {
    if (!config.dependPipelineIds || config.dependPipelineIds.length === 0) {
      message.warning('请选择要等待的依赖流程')
      return
    }
    // 设置默认值
    if (!config.dependRelation) {
      config.dependRelation = 'AND'
    }
    if (!config.checkCycle) {
      config.checkCycle = 'day'
    }
    if (!config.checkInterval) {
      config.checkInterval = 10
    }
    if (config.dependTimeout === undefined) {
      config.dependTimeout = 0
    }
    if (!config.timeoutStrategy) {
      config.timeoutStrategy = 'FAIL'
    }
  }
  
  if (nodeType === 'sub_process') {
    if (!config.subProcessId) {
      message.warning('请选择子流程')
      return
    }
  }
  
  // 通知节点校验
  if (nodeType === 'email') {
    if (!config.toList || config.toList.length === 0) {
      message.warning('请添加至少一个收件人')
      return
    }
    if (!config.subject || !config.subject.trim()) {
      message.warning('请输入邮件主题')
      return
    }
  }
  
  if (nodeType === 'sms') {
    if (!config.phoneNumbers || config.phoneNumbers.length === 0) {
      message.warning('请添加至少一个手机号')
      return
    }
  }
  
  if (nodeType === 'wecom') {
    if (!config.content || !config.content.trim()) {
      message.warning('请输入消息内容')
      return
    }
  }
  
  selectedNode.value.nodeConfig = JSON.stringify(config)
  // 自动保存整个流程
  saving.value = true
  try {
    const flowJson = JSON.stringify({ 
      nodes: nodes.value, 
      edges: edges.value,
      globalParams: globalParams.value,
      config: pipelineConfig.value,
      flowVars: flowVars.value
    })
    await savePipelineDesign(pipelineId.value, { flowJson, nodes: nodes.value })
    message.success('配置已保存')
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const autoLayout = () => {
  if (nodes.value.length === 0) return

  // 建立有向图
  const outEdges: Map<string, string[]> = new Map()
  const inDegree: Map<string, number> = new Map()
  nodes.value.forEach(n => { outEdges.set(n.nodeCode, []); inDegree.set(n.nodeCode, 0) })
  edges.value.forEach(e => {
    const out = outEdges.get(e.source)
    if (out) out.push(e.target)
    const deg = inDegree.get(e.target)
    if (deg !== undefined) inDegree.set(e.target, deg + 1)
  })

  // BFS 拓扑排序，计算每个节点层级
  const level: Map<string, number> = new Map()
  const queue: string[] = []
  nodes.value.forEach(n => {
    if ((inDegree.get(n.nodeCode) ?? 0) === 0) { queue.push(n.nodeCode); level.set(n.nodeCode, 0) }
  })
  if (queue.length === 0) {
    // 有环：所有节点从第0层开始
    nodes.value.forEach(n => { queue.push(n.nodeCode); level.set(n.nodeCode, 0) })
  }
  for (let i = 0; i < queue.length; i++) {
    const code = queue[i]!
    const curLv = level.get(code) ?? 0
    ;(outEdges.get(code) ?? []).forEach((target: string) => {
      const existing = level.get(target)
      if (existing === undefined || existing <= curLv) {
        level.set(target, curLv + 1)
        queue.push(target)
      }
    })
  }
  nodes.value.forEach(n => { if (!level.has(n.nodeCode)) level.set(n.nodeCode, 0) })

  // 按层分组
  const groups: Map<number, string[]> = new Map()
  let maxLv = 0
  nodes.value.forEach(n => {
    const lv = level.get(n.nodeCode) ?? 0
    if (!groups.has(lv)) groups.set(lv, [])
    groups.get(lv)!.push(n.nodeCode)
    if (lv > maxLv) maxLv = lv
  })

  // 定位：左→右分层，同层垂直顺排
  const xGap = 220; const yGap = 130; const startX = 80; const startY = 80
  for (let lv = 0; lv <= maxLv; lv++) {
    const group = groups.get(lv) ?? []
    group.forEach((code, idx) => {
      const node = nodes.value.find(n => n.nodeCode === code)
      if (node) {
        node.positionX = startX + lv * xGap
        node.positionY = startY + idx * yGap
      }
    })
  }
  message.success('自动布局完成（拓扑分层）')
}

const clearCanvas = () => {
  dialog.warning({
    title: '确认清空',
    content: '确定要清空画布吗？所有节点和连线将被删除。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      nodes.value = []
      edges.value = []
      selectedNode.value = null
      message.success('画布已清空')
    }
  })
}

const goBack = () => router.push('/pipeline/manage')

const handleSave = async () => {
  saving.value = true
  try {
    const flowJson = JSON.stringify({ 
      nodes: nodes.value, 
      edges: edges.value,
      globalParams: globalParams.value,
      config: pipelineConfig.value,
      flowVars: flowVars.value
    })
    await savePipelineDesign(pipelineId.value, { flowJson, nodes: nodes.value })
    message.success('保存成功')
  } catch (e) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleExecute = async () => {
  try {
    await executePipeline(pipelineId.value)
    message.success('执行已启动')
  } catch (e: any) {
    message.error(e?.message || '执行失败')
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const [pipelineRes, designRes, dsRes] = await Promise.all([
      getPipeline(pipelineId.value),
      getPipelineDesign(pipelineId.value),
      getDataSourceList({ page: 1, pageSize: 100 })
    ])
    
    pipeline.value = pipelineRes.data
    nodes.value = designRes.data?.nodes || []
    edges.value = designRes.data?.edges || []
    
    // 加载全局参数和配置
    if (pipeline.value?.flowJson) {
      try {
        const flowData = JSON.parse(pipeline.value.flowJson)
        if (flowData.globalParams) globalParams.value = flowData.globalParams
        if (flowData.config) pipelineConfig.value = { ...pipelineConfig.value, ...flowData.config }
        if (flowData.flowVars) flowVars.value = flowData.flowVars
      } catch (_) { /* flowJson parse failed, use defaults */ }
    }
    
    const dsData = dsRes.data as any
    dataSources.value = dsData?.list || dsData?.data?.list || []
    
    // 加载流程列表（用于子流程选择）
    try {
      const plRes = await getPipelines({ page: 1, pageSize: 100 })
      const plData = plRes.data as any
      pipelineOptions.value = (plData?.list || plData || [])
        .filter((p: any) => p.id !== pipelineId.value)
        .map((p: any) => ({ label: p.pipelineName, value: p.id }))
    } catch (_) { /* pipeline list load failed, sub-process select unavailable */ }
  } catch (e) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (pipelineId.value) loadData()
  loadChannelOptions()
  
  // 监听鼠标移动，更新临时连线
  const handleMouseMove = (e: MouseEvent) => {
    if (isConnecting.value && connectSourceNode.value && canvasRef.value) {
      const rect = canvasRef.value.getBoundingClientRect()
      tempLineEnd.value = {
        x: (e.clientX - rect.left) / canvasScale.value,
        y: (e.clientY - rect.top) / canvasScale.value
      }
    }
  }
  
  // 键盘快捷键
  const handleKeyDown = (e: KeyboardEvent) => {
    // ESC 取消连线模式
    if (e.key === 'Escape') {
      if (isConnecting.value) {
        isConnecting.value = false
        connectSourceNode.value = null
        tempLineEnd.value = null
      }
      selectedNode.value = null
    }
    // Delete 删除选中节点
    if (e.key === 'Delete' && selectedNode.value) {
      const index = nodes.value.findIndex(n => n.nodeCode === selectedNode.value.nodeCode)
      if (index > -1) {
        removeNode(index)
      }
    }
    // Ctrl+S 保存
    if (e.ctrlKey && e.key === 's') {
      e.preventDefault()
      handleSave()
    }
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('keydown', handleKeyDown)
  
  // 清理函数
  onUnmounted(() => {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('keydown', handleKeyDown)
  })
})
</script>

<style scoped>
.pipeline-designer {
  height: calc(100vh - 178px);
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  border-radius: 8px;
}

/* 工具栏 */
.designer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: linear-gradient(135deg, #fff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.toolbar-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.toolbar-center {
  display: flex;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

/* 主体区域 */
.designer-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 节点面板 */
.node-panel {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.node-panel.collapsed {
  width: 48px;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  font-weight: 600;
  color: #1e293b;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  cursor: pointer;
  user-select: none;
}

.panel-header:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
}

.collapse-icon {
  margin-left: auto;
  opacity: 0.5;
}

.node-list {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.node-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  padding: 10px 4px 6px;
  margin-top: 8px;
  border-top: 1px solid #e2e8f0;
}

.node-group-title:first-child {
  margin-top: 0;
  border-top: none;
  padding-top: 4px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f8fafc;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.node-item:hover {
  background: #fff;
  border-color: #2080f0;
  box-shadow: 0 4px 12px rgba(32, 128, 240, 0.15);
  transform: translateX(4px);
}

.node-item:active {
  cursor: grabbing;
  transform: scale(0.98);
}

.node-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.node-info {
  flex: 1;
  min-width: 0;
}

.node-name {
  font-weight: 600;
  font-size: 13px;
  color: #1e293b;
}

.node-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 画布容器 */
.canvas-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.canvas-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #64748b;
  z-index: 100;
}

.zoom-indicator {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #64748b;
  z-index: 100;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.canvas-area {
  width: 3000px;
  height: 2000px;
  position: relative;
  transition: transform 0.2s ease;
}

.canvas-grid {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle, #d1d5db 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

.canvas-hint {
  position: absolute;
  top: 200px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
  color: #94a3b8;
}

.canvas-hint p {
  margin-top: 16px;
  font-size: 16px;
  font-weight: 500;
}

.canvas-hint .hint-sub {
  font-size: 13px;
  margin-top: 8px;
  opacity: 0.7;
}

/* 画布节点 */
.canvas-node {
  position: absolute;
  width: 180px;
  background: #fff;
  border-radius: 10px;
  border: 2px solid #e2e8f0;
  cursor: move;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s, border-color 0.2s, transform 0.15s;
  z-index: 10;
}

.canvas-node:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: var(--node-color);
  transform: translateY(-2px);
}

.canvas-node.selected {
  border-color: #2080f0;
  box-shadow: 0 0 0 3px rgba(32, 128, 240, 0.2), 0 8px 24px rgba(0, 0, 0, 0.12);
}

.canvas-node.connecting {
  border-color: #f0a020;
  animation: pulse 1s infinite;
}

.canvas-node.disabled {
  opacity: 0.6;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(240, 160, 32, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(240, 160, 32, 0); }
}

.node-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px 8px 0 0;
  color: #fff;
}

.node-title {
  flex: 1;
  font-weight: 600;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-content {
  padding: 10px 12px;
  background: #fff;
  border-radius: 0 0 8px 8px;
}

.node-type-label {
  font-size: 12px;
  color: #64748b;
}

.node-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
}

.node-port {
  position: absolute;
  width: 14px;
  height: 14px;
  background: #fff;
  border: 2px solid #94a3b8;
  border-radius: 50%;
  left: 50%;
  transform: translateX(-50%);
  transition: all 0.2s;
  cursor: crosshair;
  z-index: 20;
}

.port-top { top: -7px; }
.port-bottom { bottom: -7px; }

.canvas-node:hover .node-port,
.node-port:hover {
  border-color: #2080f0;
  background: #2080f0;
  transform: translateX(-50%) scale(1.2);
}

.node-delete {
  position: absolute;
  top: -10px;
  right: -10px;
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 30;
}

.canvas-node:hover .node-delete {
  opacity: 1;
}

/* 连线 */
.connections-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 5;
}

.edge-path {
  pointer-events: stroke;
  cursor: pointer;
  transition: all 0.2s;
}

.edge-group:hover .edge-path {
  stroke: #059669;
  stroke-width: 3;
}

.edge-delete, .edge-delete-text {
  pointer-events: auto;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.edge-group:hover .edge-delete,
.edge-group:hover .edge-delete-text {
  opacity: 1;
}

/* 属性面板 */
.property-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.04);
}

.property-panel.collapsed {
  width: 48px;
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.pipeline-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.info-item span {
  font-size: 14px;
  color: #1e293b;
}

.info-item .code {
  font-family: 'Monaco', 'Menlo', monospace;
  background: #f1f5f9;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* 全局参数 */
.global-params {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 滚动条美化 */
.node-list::-webkit-scrollbar,
.panel-content::-webkit-scrollbar {
  width: 6px;
}

.node-list::-webkit-scrollbar-track,
.panel-content::-webkit-scrollbar-track {
  background: transparent;
}

.node-list::-webkit-scrollbar-thumb,
.panel-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.node-list::-webkit-scrollbar-thumb:hover,
.panel-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}



























</style>

<style>
/* PipelineDesigner 深色模式（非 scoped） */
html.dark .pipeline-designer {
  background: #0f172a !important;
}
html.dark .designer-toolbar {
  background: linear-gradient(135deg, #1e293b 0%, #1a2536 100%) !important;
  border-bottom-color: #334155 !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3) !important;
}
html.dark .toolbar-title h3 {
  color: #f1f5f9 !important;
}
html.dark .node-panel {
  background: #1e293b !important;
  border-right-color: #334155 !important;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.2) !important;
}
html.dark .panel-header {
  color: #e2e8f0 !important;
  border-bottom-color: #334155 !important;
  background: linear-gradient(135deg, #1a2536 0%, #1e293b 100%) !important;
}
html.dark .panel-header:hover {
  background: linear-gradient(135deg, #243044 0%, #2a3649 100%) !important;
}
html.dark .node-group-title {
  color: #94a3b8 !important;
  border-top-color: #334155 !important;
}
html.dark .node-item {
  background: #243044 !important;
}
html.dark .node-item:hover {
  background: #2a3649 !important;
  border-color: #818cf8 !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15) !important;
}
html.dark .node-name {
  color: #e2e8f0 !important;
}
html.dark .node-desc {
  color: #64748b !important;
}
html.dark .canvas-container {
  background: linear-gradient(135deg, #0f172a 0%, #1a2536 100%) !important;
}
html.dark .canvas-grid {
  background-image: radial-gradient(circle, #334155 1px, transparent 1px) !important;
}
html.dark .zoom-indicator {
  background: rgba(30, 41, 59, 0.9) !important;
  color: #94a3b8 !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3) !important;
}
html.dark .canvas-node {
  background: #1e293b !important;
  border-color: #334155 !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
}
html.dark .canvas-node:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4) !important;
}
html.dark .canvas-node.selected {
  box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.3), 0 8px 24px rgba(0, 0, 0, 0.4) !important;
}
html.dark .node-content {
  background: #1e293b !important;
}
html.dark .node-type-label {
  color: #94a3b8 !important;
}
html.dark .node-port {
  background: #1e293b !important;
  border-color: #64748b !important;
}
html.dark .property-panel {
  background: #1e293b !important;
  border-left-color: #334155 !important;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.2) !important;
}
html.dark .info-item label {
  color: #64748b !important;
}
html.dark .info-item span {
  color: #e2e8f0 !important;
}
html.dark .info-item .code {
  background: #0f172a !important;
  color: #e2e8f0 !important;
}
html.dark .canvas-loading {
  color: #94a3b8 !important;
}
html.dark .node-list::-webkit-scrollbar-thumb,
html.dark .node-list::-webkit-scrollbar-thumb,
html.dark .panel-content::-webkit-scrollbar-thumb {
  background: #334155 !important;
}
html.dark .node-list::-webkit-scrollbar-thumb:hover,
html.dark .node-list::-webkit-scrollbar-thumb:hover,
html.dark .panel-content::-webkit-scrollbar-thumb:hover {
  background: #475569 !important;
}
</style>
