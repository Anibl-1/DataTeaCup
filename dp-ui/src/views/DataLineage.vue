<template>
  <div class="data-lineage-page">
    <!-- Page_Header_Stats: 统计卡片区域 (Req 1.1, 3.8) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><GitNetworkOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ lineageList.length }}</span>
          <span class="stat-label">{{ t('lineage.lineageRelations') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><ServerOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tableCount }}</span>
          <span class="stat-label">{{ t('lineage.involvedTables') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><SparklesOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ graphData.nodes.length }}</span>
          <span class="stat-label">{{ t('lineage.graphNodes') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><ScanOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ graphData.edges.length }}</span>
          <span class="stat-label">{{ t('lineage.graphEdges') }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索卡片 -->
    <n-card :bordered="false" class="search-card">
      <div class="search-bar">
        <div class="search-fields">
          <n-select
            v-model:value="searchDsId"
            :options="dataSourceOptions"
            :placeholder="t('lineage.selectDataSource')"
            clearable
            filterable
            style="width: 200px"
            @update:value="onSearchDsChange"
          />
          <n-select
            v-model:value="searchTable"
            :options="searchTableOptions"
            :placeholder="t('lineage.selectTable')"
            filterable
            tag
            clearable
            style="width: 240px"
            :loading="loadingSearchTables"
          />
          <n-input-number v-model:value="searchDepth" :min="1" :max="5" style="width: 110px">
            <template #prefix>{{ t('lineage.depth') }}</template>
          </n-input-number>
          <n-button type="primary" :loading="loading" @click="searchLineage">
            <template #icon><n-icon :component="SearchOutline" /></template>
            查询血缘
          </n-button>
          <n-button @click="searchDsId = null; searchTable = ''; searchTableOptions = []; loadLineageList()">{{ t('lineage.reset') }}</n-button>
        </div>
        <div class="search-actions">
          <n-button type="primary" secondary @click="addModal.openCreate()">
            <template #icon><n-icon :component="AddOutline" /></template>
            添加血缘
          </n-button>
          <n-dropdown :options="analysisMenuOptions" @select="onAnalysisMenuSelect">
            <n-button secondary>
              <template #icon><n-icon :component="SparklesOutline" /></template>
              智能分析
            </n-button>
          </n-dropdown>
          <n-button secondary :loading="discovering" @click="autoDiscoverLineage">
            <template #icon><n-icon :component="ScanOutline" /></template>
            自动发现
          </n-button>
        </div>
      </div>
    </n-card>

    <!-- 血缘图卡片 -->
    <n-card :bordered="false" class="graph-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="20" color="var(--color-primary)" class="header-icon"><GitNetworkOutline /></n-icon>
          <span>{{ t('lineage.lineageGraph') }}</span>
        </div>
      </template>
      <template #header-extra>
        <n-space v-if="graphData.nodes.length > 0" :size="8">
          <n-tag size="small" :bordered="false" type="info">{{ t('lineage.nodes') }} {{ graphData.nodes.length }}</n-tag>
          <n-tag size="small" :bordered="false" type="success">{{ t('lineage.edges') }} {{ graphData.edges.length }}</n-tag>
          <n-tag size="small" :bordered="false" type="warning">{{ graphData.centerTable }}</n-tag>
          <n-button size="small" quaternary @click="resetGraphZoom">{{ t('lineage.resetView') }}</n-button>
        </n-space>
      </template>
      <div v-if="graphData.nodes.length > 0" ref="echartsContainer" class="echarts-graph"></div>
      <n-empty v-else :description="t('lineage.selectToQuery')" style="padding: 80px 0">
        <template #icon>
          <n-icon size="52" color="#cbd5e1"><GitNetworkOutline /></n-icon>
        </template>
      </n-empty>
    </n-card>

    <!-- 血缘列表卡片 -->
    <n-card :bordered="false" class="list-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="20" color="var(--color-primary)" class="header-icon"><ListOutline /></n-icon>
          <span>{{ t('lineage.lineageList') }}</span>
        </div>
      </template>
      <n-data-table
        :columns="columns" :data="lineageList" :loading="listLoading" 
        :pagination="{ pageSize: 10 }" size="small" :row-props="lineageRowProps"
        :scroll-x="1000"
        striped
        class="custom-table"
      />
    </n-card>
    
    <!-- 添加血缘弹窗（使用 useFormModal）(Req 9.2) -->
    <n-modal v-model:show="addModal.visible.value" preset="card" :title="t('lineage.addLineageRelation')" class="dp-modal-lg">
      <n-tabs v-model:value="addModalTab" type="line" animated>
        <n-tab-pane name="manual" :tab="t('lineage.manualAdd')">
          <div class="lineage-form-row">
            <div class="lineage-form-panel source-panel">
              <div class="panel-title"><span class="panel-dot source-dot"></span> {{ t('lineage.sourceUpstream') }}</div>
              <n-form-item :label="t('lineage.dataSource')" label-placement="left" label-width="60px">
                <n-select
                  v-model:value="addModal.formData.value.sourceDataSourceId" :options="dataSourceOptions"
                  :placeholder="t('lineage.selectDataSource')" clearable filterable size="small"
                  @update:value="onSourceDsChange" />
              </n-form-item>
              <n-form-item :label="t('lineage.tableName')" label-placement="left" label-width="60px">
                <n-select
                  v-if="sourceTableOptions.length > 0" v-model:value="addModal.formData.value.sourceTable"
                  :options="sourceTableOptions" :placeholder="t('lineage.selectTable')" filterable tag size="small" />
                <n-input v-else v-model:value="addModal.formData.value.sourceTable" :placeholder="t('lineage.autoLoadAfterDs')" size="small" />
              </n-form-item>
            </div>
            <div class="lineage-arrow">→</div>
            <div class="lineage-form-panel target-panel">
              <div class="panel-title"><span class="panel-dot target-dot"></span> {{ t('lineage.targetDownstream') }}</div>
              <n-form-item :label="t('lineage.dataSource')" label-placement="left" label-width="60px">
                <n-select
                  v-model:value="addModal.formData.value.targetDataSourceId" :options="dataSourceOptions"
                  :placeholder="t('lineage.selectDataSource')" clearable filterable size="small"
                  @update:value="onTargetDsChange" />
              </n-form-item>
              <n-form-item :label="t('lineage.tableName')" label-placement="left" label-width="60px">
                <n-select
                  v-if="targetTableOptions.length > 0" v-model:value="addModal.formData.value.targetTable"
                  :options="targetTableOptions" :placeholder="t('lineage.selectTable')" filterable tag size="small" />
                <n-input v-else v-model:value="addModal.formData.value.targetTable" placeholder="选数据源后自动加载" size="small" />
              </n-form-item>
            </div>
          </div>
          <n-divider style="margin: 12px 0" />
          <div style="display: flex; gap: 16px;">
            <n-form-item :label="t('lineage.lineageType')" label-placement="left" label-width="70px" style="flex: 1">
              <n-select v-model:value="addModal.formData.value.lineageType" :options="lineageTypeOptions" size="small" />
            </n-form-item>
            <n-form-item :label="t('lineage.description')" label-placement="left" label-width="40px" style="flex: 2">
              <n-input v-model:value="addModal.formData.value.transformLogic" :placeholder="t('lineage.transformLogicOptional')" size="small" />
            </n-form-item>
          </div>
        </n-tab-pane>
        <n-tab-pane name="sql" :tab="t('lineage.parseSql')">
          <n-space vertical size="large">
            <n-form-item :label="t('lineage.targetTableName')" label-placement="left" label-width="70px">
              <n-input v-model:value="parseSqlForm.targetTable" :placeholder="t('lineage.sqlTargetPlaceholder')" size="small" />
            </n-form-item>
            <n-form-item :label="t('lineage.sqlStatement')" label-placement="left" label-width="70px">
              <n-input
                v-model:value="parseSqlForm.sql" type="textarea" :rows="6"
                :placeholder="t('lineage.sqlPlaceholder')" size="small" />
            </n-form-item>
            <n-button type="primary" :loading="parsingSql" block @click="parseSqlLineage">{{ t('lineage.parseAndSave') }}</n-button>
            <div v-if="parsedLineages.length > 0">
              <n-alert type="success" :title="t('lineage.parsedCount', { count: parsedLineages.length })" />
              <n-list bordered size="small" style="margin-top: 8px">
                <n-list-item v-for="(l, idx) in parsedLineages" :key="idx">
                  {{ l.sourceTable }} → {{ l.targetTable }}
                </n-list-item>
              </n-list>
            </div>
          </n-space>
        </n-tab-pane>
      </n-tabs>
      <template #footer>
        <n-space justify="end">
          <n-button @click="addModal.close()">{{ t('common.cancel') }}</n-button>
          <n-button v-if="addModalTab === 'manual'" type="primary" :loading="addModal.submitting.value" @click="saveLineage">{{ t('common.save') }}</n-button>
        </n-space>
      </template>
    </n-modal>
    
    <!-- 统计信息弹窗 -->
    <n-modal v-model:show="showStatsModal" preset="card" :title="t('lineage.lineageStats')" class="dp-modal-sm">
      <n-descriptions :column="1" bordered>
        <n-descriptions-item :label="t('lineage.totalLineages')">{{ statistics.totalCount || 0 }}</n-descriptions-item>
        <n-descriptions-item :label="t('lineage.involvedTables')">{{ statistics.tableCount || 0 }}</n-descriptions-item>
      </n-descriptions>
      <n-divider>{{ t('lineage.byType') }}</n-divider>
      <n-space vertical>
        <n-tag v-for="(count, type) in statistics.byType" :key="type" type="info">
          {{ type }}: {{ count }}
        </n-tag>
      </n-space>
    </n-modal>
    
    <!-- 影响分析弹窗 -->
    <n-modal v-model:show="showImpactModal" preset="card" :title="t('lineage.impactAnalysis')" class="dp-modal-lg">
      <n-space vertical size="large">
        <n-form-item :label="t('lineage.analyzeTable')">
          <n-input v-model:value="impactTable" :placeholder="t('lineage.enterTableName')" />
        </n-form-item>
        <n-button type="warning" :loading="analyzingImpact" @click="runImpactAnalysis">
          分析影响范围
        </n-button>
        <div v-if="impactResult">
          <n-alert type="warning" :title="t('lineage.impactSummary', { table: impactResult.tableName, count: impactResult.impactedCount })" />
          <n-list bordered size="small" style="margin-top: 12px; max-height: 300px; overflow-y: auto;">
            <n-list-item v-for="(item, idx) in impactResult.impactChain" :key="idx">
              <n-thing :title="`${item.source} → ${item.target}`" :description="`${t('lineage.type')}: ${item.type} | ${t('lineage.depth')}: ${item.depth} | ${item.logic || ''}`" />
            </n-list-item>
          </n-list>
          <n-space style="margin-top: 12px">
            <n-tag v-for="t in impactResult.impactedTables" :key="t" type="warning" size="small">{{ t }}</n-tag>
          </n-space>
        </div>
      </n-space>
    </n-modal>
    
    <!-- 编辑血缘弹窗（使用 useFormModal）(Req 9.2) -->
    <n-modal v-model:show="editModal.visible.value" preset="card" :title="t('lineage.editLineageRelation')" class="dp-modal-lg">
      <div class="lineage-form-row">
        <div class="lineage-form-panel source-panel">
          <div class="panel-title"><span class="panel-dot source-dot"></span> 数据来源（上游）</div>
          <n-form-item :label="t('lineage.dataSource')" label-placement="left" label-width="60px">
            <n-select
              v-model:value="editModal.formData.value.sourceDataSourceId" :options="dataSourceOptions"
              :placeholder="t('lineage.optional')" clearable filterable size="small"
              @update:value="onEditSourceDsChange" />
          </n-form-item>
          <n-form-item :label="t('lineage.tableName')" label-placement="left" label-width="60px">
            <n-select
              v-if="editSourceTableOptions.length > 0" v-model:value="editModal.formData.value.sourceTable"
              :options="editSourceTableOptions" :placeholder="t('lineage.selectTable')" filterable tag size="small" />
            <n-input v-else v-model:value="editModal.formData.value.sourceTable" :placeholder="t('lineage.sourceTableName')" size="small" />
          </n-form-item>
          <n-form-item :label="t('lineage.column')" label-placement="left" label-width="60px">
            <n-input v-model:value="editModal.formData.value.sourceColumn" :placeholder="t('lineage.optionalCommaSep')" size="small" />
          </n-form-item>
        </div>
        <div class="lineage-arrow">→</div>
        <div class="lineage-form-panel target-panel">
          <div class="panel-title"><span class="panel-dot target-dot"></span> 数据去向（下游）</div>
          <n-form-item :label="t('lineage.dataSource')" label-placement="left" label-width="60px">
            <n-select
              v-model:value="editModal.formData.value.targetDataSourceId" :options="dataSourceOptions"
              :placeholder="t('lineage.optional')" clearable filterable size="small"
              @update:value="onEditTargetDsChange" />
          </n-form-item>
          <n-form-item label="表名" label-placement="left" label-width="60px">
            <n-select
              v-if="editTargetTableOptions.length > 0" v-model:value="editModal.formData.value.targetTable"
              :options="editTargetTableOptions" :placeholder="t('lineage.selectTable')" filterable tag size="small" />
            <n-input v-else v-model:value="editModal.formData.value.targetTable" :placeholder="t('lineage.targetTableName')" size="small" />
          </n-form-item>
          <n-form-item :label="t('lineage.column')" label-placement="left" label-width="60px">
            <n-input v-model:value="editModal.formData.value.targetColumn" :placeholder="t('lineage.optionalCommaSep')" size="small" />
          </n-form-item>
        </div>
      </div>
      <n-divider style="margin: 12px 0" />
      <div style="display: flex; gap: 16px;">
        <n-form-item :label="t('lineage.lineageType')" label-placement="left" label-width="70px" style="flex: 1">
          <n-select v-model:value="editModal.formData.value.lineageType" :options="lineageTypeOptions" size="small" />
        </n-form-item>
        <n-form-item :label="t('lineage.description')" label-placement="left" label-width="40px" style="flex: 2">
          <n-input v-model:value="editModal.formData.value.transformLogic" :placeholder="t('lineage.transformLogicOptional')" size="small" />
        </n-form-item>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="editModal.close()">取消</n-button>
          <n-button type="primary" :loading="editModal.submitting.value" @click="updateLineage">保存</n-button>
        </n-space>
      </template>
    </n-modal>
    
    <!-- AI分析弹窗 -->
    <n-modal v-model:show="showAiAnalysis" preset="card" :title="t('lineage.aiAnalysis')" class="dp-modal-lg">
      <n-space vertical size="large">
        <n-alert type="info" :title="t('lineage.aiSmartAnalysis')">
          AI将分析选中表的数据血缘关系，包括上下游依赖、字段级血缘和潜在的数据质量问题。
        </n-alert>
        
        <n-form-item label="数据源">
          <n-select
            v-model:value="aiAnalysisDataSource" :options="dataSourceOptions" placeholder="选择数据源"
            filterable @update:value="onAiDsChange" />
        </n-form-item>
        
        <n-form-item :label="t('lineage.analyzeTable')">
          <n-select
            v-if="aiTableOptions.length > 0" v-model:value="aiAnalysisTable"
            :options="aiTableOptions" placeholder="选择表" filterable tag />
          <n-input v-else v-model:value="aiAnalysisTable" :placeholder="t('lineage.selectDsOrEnterTable')" />
        </n-form-item>
        
        <n-button type="primary" block :loading="aiAnalyzing" @click="runAiAnalysis">
          <template #icon><n-icon><SparklesOutline /></n-icon></template>
          开始AI分析
        </n-button>
        
        <div v-if="aiAnalysisResult" class="ai-result">
          <n-divider>{{ t('lineage.analysisResult') }}</n-divider>
          <div class="markdown-content" v-html="renderMarkdown(aiAnalysisResult)"></div>
        </div>
      </n-space>
    </n-modal>
    
    <!-- 健康报告弹窗 -->
    <n-modal v-model:show="showHealthModal" preset="card" :title="t('lineage.healthReport')" class="dp-modal-lg">
      <div v-if="healthReport">
        <n-space vertical size="large">
          <n-card size="small">
            <n-statistic :label="t('lineage.healthScore')" :value="healthReport.healthScore || 0">
              <template #suffix>
                <StatusTag :status="healthReport.healthLevel || 'unknown'" :status-map="healthLevelMap" />
              </template>
            </n-statistic>
          </n-card>
          
          <n-descriptions :column="2" bordered size="small">
            <n-descriptions-item :label="t('lineage.totalLineages')">{{ healthReport.totalLineages || 0 }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.totalTables')">{{ healthReport.totalTables || 0 }}</n-descriptions-item>
          </n-descriptions>
          
          <div>
            <div style="font-weight: 500; margin-bottom: 8px;">{{ t('lineage.typeDistribution') }}</div>
            <n-space>
              <n-tag v-for="(count, type) in healthReport.lineageTypeDistribution" :key="type" type="info" size="small">
                {{ type }}: {{ count }}
              </n-tag>
            </n-space>
          </div>
          
          <div v-if="healthReport.issues?.length > 0">
            <div style="font-weight: 500; margin-bottom: 8px; color: var(--color-warning);">{{ t('lineage.detectedIssues') }}</div>
            <n-list bordered size="small">
              <n-list-item v-for="(issue, idx) in healthReport.issues" :key="idx">
                <n-thing>
                  <template #header>
                    <StatusTag :status="issue.severity || 'warning'" :status-map="issueSeverityMap" />
                    {{ issue.tableName || issue.type }}
                  </template>
                  <template #description>{{ issue.description }}</template>
                </n-thing>
              </n-list-item>
            </n-list>
          </div>
          
          <div v-if="healthReport.recommendations?.length > 0">
            <div style="font-weight: 500; margin-bottom: 8px; color: var(--color-success);">{{ t('lineage.recommendations') }}</div>
            <n-list bordered size="small">
              <n-list-item v-for="(rec, idx) in healthReport.recommendations" :key="idx">{{ rec }}</n-list-item>
            </n-list>
          </div>
        </n-space>
      </div>
    </n-modal>
    
    <!-- 热点分析弹窗 -->
    <n-modal v-model:show="showHotspotModal" preset="card" :title="t('lineage.hotspotAnalysis')" class="dp-modal-lg">
      <div v-if="hotspotData">
        <n-space vertical size="large">
          <n-descriptions :column="2" bordered size="small">
            <n-descriptions-item :label="t('lineage.analyzedTables')">{{ hotspotData.totalTables || 0 }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.avgDependencies')">{{ (hotspotData.averageDependencies || 0).toFixed(1) }}</n-descriptions-item>
          </n-descriptions>
          
          <n-data-table :columns="hotspotColumns" :data="hotspotData.hotspots || []" size="small" :pagination="{ pageSize: 5 }" class="custom-table" />
        </n-space>
      </div>
    </n-modal>
    
    <!-- 孤岛检测弹窗 -->
    <n-modal v-model:show="showOrphanModal" preset="card" :title="t('lineage.orphanDetection')" class="dp-modal-lg">
      <div v-if="orphanData">
        <n-space vertical size="large">
          <n-descriptions :column="2" bordered size="small">
            <n-descriptions-item :label="t('lineage.coveredTables')">{{ orphanData.totalTablesInLineage || 0 }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.sourceTableCount')">{{ orphanData.sourceTableCount || 0 }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.deadEndTableCount')">{{ orphanData.deadEndTableCount || 0 }}</n-descriptions-item>
          </n-descriptions>
          
          <n-tabs type="line" animated>
            <n-tab-pane name="source" :tab="t('lineage.sourceTables')">
              <n-alert type="info" :title="t('lineage.sourceTablesDesc')" style="margin-bottom: 12px" />
              <n-space>
                <n-tag v-for="t in orphanData.sourceTables" :key="t" type="success" size="small" style="cursor: pointer" @click="searchTable = t; searchLineage()">
                  {{ t }}
                </n-tag>
              </n-space>
              <n-empty v-if="!orphanData.sourceTables?.length" :description="t('lineage.noSourceTables')" />
            </n-tab-pane>
            <n-tab-pane name="deadend" :tab="t('lineage.deadEndTables')">
              <n-alert type="warning" :title="t('lineage.deadEndTablesDesc')" style="margin-bottom: 12px" />
              <n-space>
                <n-tag v-for="t in orphanData.deadEndTables" :key="t" type="warning" size="small" style="cursor: pointer" @click="searchTable = t; searchLineage()">
                  {{ t }}
                </n-tag>
              </n-space>
              <n-empty v-if="!orphanData.deadEndTables?.length" :description="t('lineage.noDeadEndTables')" />
            </n-tab-pane>
          </n-tabs>
        </n-space>
      </div>
    </n-modal>
    
    <!-- 全链路追溯弹窗 -->
    <n-modal v-model:show="showFullChainModal" preset="card" :title="t('lineage.fullChainTrace')" class="dp-modal-lg">
      <n-space vertical size="large">
        <n-input-group>
          <n-input v-model:value="fullChainTable" :placeholder="t('lineage.enterTraceTable')" style="width: 300px" />
          <n-button type="primary" @click="loadFullChainAnalysis">{{ t('lineage.startTrace') }}</n-button>
        </n-input-group>
        
        <div v-if="fullChainData">
          <n-descriptions :column="3" bordered size="small" style="margin-bottom: 16px">
            <n-descriptions-item :label="t('lineage.centerTable')">{{ fullChainData.tableName }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.upstreamLayers')">{{ fullChainData.totalUpstreamLayers || 0 }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.downstreamLayers')">{{ fullChainData.totalDownstreamLayers || 0 }}</n-descriptions-item>
            <n-descriptions-item :label="t('lineage.totalNodes')">{{ fullChainData.totalNodes || 0 }}</n-descriptions-item>
          </n-descriptions>
          
          <div v-if="fullChainData.criticalPath?.length > 0" style="margin-bottom: 16px">
            <div style="font-weight: 500; margin-bottom: 8px;">{{ t('lineage.criticalPath') }}</div>
            <n-space align="center">
              <template v-for="(t, idx) in fullChainData.criticalPath" :key="t">
                <n-tag :type="t === fullChainData.tableName ? 'primary' : 'default'" size="small">{{ t }}</n-tag>
                <span v-if="Number(idx) < fullChainData.criticalPath.length - 1" class="text-muted">→</span>
              </template>
            </n-space>
          </div>
          
          <n-tabs type="line" animated>
            <n-tab-pane name="upstream" :tab="`${t('lineage.upstream')} (${fullChainData.upstreamChain?.length || 0})`">
              <n-list bordered size="small" style="max-height: 300px; overflow-y: auto">
                <n-list-item v-for="(node, idx) in fullChainData.upstreamChain" :key="idx">
                  <n-thing>
                    <template #header>
                      <n-tag type="success" size="tiny">L{{ node.layer }}</n-tag>
                      {{ node.tableName }}
                    </template>
                    <template #description>
                      {{ t('lineage.type') }}: {{ node.lineageType }} | {{ t('lineage.parentTable') }}: {{ node.parentTable }}
                      <span v-if="node.transformLogic"> | {{ node.transformLogic }}</span>
                    </template>
                  </n-thing>
                </n-list-item>
              </n-list>
              <n-empty v-if="!fullChainData.upstreamChain?.length" :description="t('lineage.noUpstream')" />
            </n-tab-pane>
            <n-tab-pane name="downstream" :tab="`${t('lineage.downstream')} (${fullChainData.downstreamChain?.length || 0})`">
              <n-list bordered size="small" style="max-height: 300px; overflow-y: auto">
                <n-list-item v-for="(node, idx) in fullChainData.downstreamChain" :key="idx">
                  <n-thing>
                    <template #header>
                      <n-tag type="warning" size="tiny">L{{ node.layer }}</n-tag>
                      {{ node.tableName }}
                    </template>
                    <template #description>
                      {{ t('lineage.type') }}: {{ node.lineageType }} | {{ t('lineage.parentTable') }}: {{ node.parentTable }}
                      <span v-if="node.transformLogic"> | {{ node.transformLogic }}</span>
                    </template>
                  </n-thing>
                </n-list-item>
              </n-list>
              <n-empty v-if="!fullChainData.downstreamChain?.length" :description="t('lineage.noDownstream')" />
            </n-tab-pane>
          </n-tabs>
        </div>
      </n-space>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, h, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useMessage } from 'naive-ui'
import { AddOutline, SearchOutline, GitNetworkOutline, SparklesOutline, ScanOutline, ServerOutline, ListOutline } from '@vicons/ionicons5'
import { marked } from 'marked'
import { dataLineageApi } from '@/api/dataLineage'
import { useI18n } from '@/i18n'
import request from '@/api/request'
import echarts from '@/utils/echarts'
import { useFormModal } from '@/composables/useFormModal'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'

const { t } = useI18n()
const message = useMessage()

// --- 状态映射（用于 StatusTag）(Req 9.3) ---
const lineageTypeStatusMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  sql: { label: t('lineage.typeSql'), type: 'info' },
  etl: { label: t('lineage.typeEtl'), type: 'success' },
  report: { label: t('lineage.typeReport'), type: 'warning' },
  collect: { label: t('lineage.typeCollect'), type: 'default' },
  manual: { label: t('lineage.typeManual'), type: 'default' }
}

const healthLevelMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  healthy: { label: t('lineage.healthy'), type: 'success' },
  warning: { label: t('lineage.warning'), type: 'warning' },
  critical: { label: t('lineage.critical'), type: 'error' }
}

const issueSeverityMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  critical: { label: '严重', type: 'error' },
  warning: { label: '警告', type: 'warning' },
  info: { label: t('lineage.info'), type: 'info' }
}

const riskLevelMap: Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }> = {
  high: { label: t('lineage.high'), type: 'error' },
  medium: { label: t('lineage.medium'), type: 'warning' },
  low: { label: t('lineage.low'), type: 'default' }
}

// 状态
const loading = ref(false)
const listLoading = ref(false)
const showStatsModal = ref(false)
const showAiAnalysis = ref(false)
const aiAnalyzing = ref(false)
const discovering = ref(false)
const aiAnalysisTable = ref('')
const aiAnalysisDataSource = ref<number | null>(null)
const aiAnalysisResult = ref('')
const dataSourceOptions = ref<Array<{label: string, value: number}>>([])
const dataSources = ref<any[]>([])
const addModalTab = ref('manual')

// 渲染Markdown
const renderMarkdown = (text: string) => {
  try {
    return marked(text)
  } catch {
    return text
  }
}

// 搜索
const searchDsId = ref<number | null>(null)
const searchTable = ref('')
const searchDepth = ref(3)
const searchTableOptions = ref<Array<{label: string, value: string}>>([])
const loadingSearchTables = ref(false)

const onSearchDsChange = async (dsId: number | null) => {
  searchTableOptions.value = []
  searchTable.value = ''
  if (dsId) {
    loadingSearchTables.value = true
    searchTableOptions.value = await loadTablesForDs(dsId)
    loadingSearchTables.value = false
  }
}

// 图数据
const graphData = reactive<{
  nodes: Array<{ id: string; label: string; type: string }>
  edges: Array<{ source: string; target: string; lineageType: string }>
  centerTable: string
}>({
  nodes: [],
  edges: [],
  centerTable: ''
})

// 列表数据
const lineageList = ref<any[]>([])

// 涉及表数（从列表数据中提取唯一表名）
const tableCount = computed(() => {
  const tables = new Set<string>()
  lineageList.value.forEach((item: any) => {
    if (item.sourceTable) tables.add(item.sourceTable)
    if (item.targetTable) tables.add(item.targetTable)
  })
  return tables.size
})

// 统计
const statistics = ref<any>({})

// ECharts 实例
const echartsContainer = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// --- 使用 useFormModal 管理添加弹窗 (Req 9.2) ---
interface LineageFormData extends Record<string, unknown> {
  sourceDataSourceId: number | null
  sourceTable: string
  sourceDatabase: string
  targetDataSourceId: number | null
  targetTable: string
  targetDatabase: string
  lineageType: string
  transformLogic: string
  sqlContent: string
}

const addModal = useFormModal<LineageFormData>({
  defaultFormData: () => ({
    sourceDataSourceId: null,
    sourceTable: '',
    sourceDatabase: '',
    targetDataSourceId: null,
    targetTable: '',
    targetDatabase: '',
    lineageType: 'sql',
    transformLogic: '',
    sqlContent: ''
  }),
  createFn: async (data) => {
    await dataLineageApi.create({
      sourceTable: data.sourceTable,
      sourceDatabase: data.sourceDatabase,
      sourceId: data.sourceDataSourceId ?? undefined,
      targetTable: data.targetTable,
      targetDatabase: data.targetDatabase,
      targetId: data.targetDataSourceId ?? undefined,
      lineageType: data.lineageType,
      transformLogic: data.transformLogic,
      sqlContent: data.sqlContent
    })
    message.success(t('common.saveSuccess'))
  },
  onSuccess: () => {
    sourceTableOptions.value = []
    targetTableOptions.value = []
    addModalTab.value = 'manual'
    loadLineageList()
  },
  onError: (error) => { message.error(t('lineage.saveFailed') + ': ' + (error.message || '')) }
})

// --- 使用 useFormModal 管理编辑弹窗 (Req 9.2) ---
interface EditLineageFormData extends Record<string, unknown> {
  id: number | null
  sourceDataSourceId: number | null
  sourceTable: string
  sourceDatabase: string
  sourceColumn: string
  targetDataSourceId: number | null
  targetTable: string
  targetDatabase: string
  targetColumn: string
  lineageType: string
  transformLogic: string
}

const editModal = useFormModal<EditLineageFormData>({
  defaultFormData: () => ({
    id: null,
    sourceDataSourceId: null,
    sourceTable: '',
    sourceDatabase: '',
    sourceColumn: '',
    targetDataSourceId: null,
    targetTable: '',
    targetDatabase: '',
    targetColumn: '',
    lineageType: 'sql',
    transformLogic: ''
  }),
  updateFn: async (data) => {
    if (!data.id) return
    await dataLineageApi.update(data.id, {
      sourceTable: data.sourceTable,
      sourceDatabase: data.sourceDatabase,
      sourceColumn: data.sourceColumn,
      sourceId: data.sourceDataSourceId ?? undefined,
      targetTable: data.targetTable,
      targetDatabase: data.targetDatabase,
      targetColumn: data.targetColumn,
      targetId: data.targetDataSourceId ?? undefined,
      lineageType: data.lineageType,
      transformLogic: data.transformLogic
    })
    message.success(t('common.updateSuccess'))
  },
  onSuccess: () => {
    editSourceTableOptions.value = []
    editTargetTableOptions.value = []
    loadLineageList()
  },
  onError: (error) => { message.error(t('lineage.updateFailed') + ': ' + (error.message || '')) }
})

// 表选项（按数据源加载）
const sourceTableOptions = ref<Array<{label: string, value: string}>>([])
const targetTableOptions = ref<Array<{label: string, value: string}>>([])
const editSourceTableOptions = ref<Array<{label: string, value: string}>>([])
const editTargetTableOptions = ref<Array<{label: string, value: string}>>([])
const aiTableOptions = ref<Array<{label: string, value: string}>>([])

// 加载指定数据源的表列表
const loadTablesForDs = async (dsId: number): Promise<Array<{label: string, value: string}>> => {
  try {
    const res: any = await request.get(`/data-source/${dsId}/tables`)
    const tables = res?.data || []
    return tables.map((t: any) => ({
      label: t.tableName || t.TABLE_NAME || t.name || String(t),
      value: t.tableName || t.TABLE_NAME || t.name || String(t)
    }))
  } catch (e) {
    console.error('[loadTables]', e)
    return []
  }
}

// 添加弹窗：源数据源变更
const onSourceDsChange = async (dsId: number | null) => {
  sourceTableOptions.value = []
  addModal.formData.value.sourceTable = ''
  addModal.formData.value.sourceDatabase = ''
  if (dsId) {
    sourceTableOptions.value = await loadTablesForDs(dsId)
    const ds = dataSources.value.find((d: any) => d.id === dsId)
    if (ds) addModal.formData.value.sourceDatabase = ds.database || ds.name
  }
}

// 添加弹窗：目标数据源变更
const onTargetDsChange = async (dsId: number | null) => {
  targetTableOptions.value = []
  addModal.formData.value.targetTable = ''
  addModal.formData.value.targetDatabase = ''
  if (dsId) {
    targetTableOptions.value = await loadTablesForDs(dsId)
    const ds = dataSources.value.find((d: any) => d.id === dsId)
    if (ds) addModal.formData.value.targetDatabase = ds.database || ds.name
  }
}

// 编辑弹窗：源数据源变更
const onEditSourceDsChange = async (dsId: number | null) => {
  editSourceTableOptions.value = []
  if (dsId) {
    editSourceTableOptions.value = await loadTablesForDs(dsId)
    const ds = dataSources.value.find((d: any) => d.id === dsId)
    if (ds) editModal.formData.value.sourceDatabase = ds.database || ds.name
  }
}

// 编辑弹窗：目标数据源变更
const onEditTargetDsChange = async (dsId: number | null) => {
  editTargetTableOptions.value = []
  if (dsId) {
    editTargetTableOptions.value = await loadTablesForDs(dsId)
    const ds = dataSources.value.find((d: any) => d.id === dsId)
    if (ds) editModal.formData.value.targetDatabase = ds.database || ds.name
  }
}

// AI分析：数据源变更
const onAiDsChange = async (dsId: number | null) => {
  aiTableOptions.value = []
  aiAnalysisTable.value = ''
  if (dsId) {
    aiTableOptions.value = await loadTablesForDs(dsId)
  }
}

const lineageTypeOptions = [
  { label: t('lineage.typeSql'), value: 'sql' },
  { label: t('lineage.typeEtl'), value: 'etl' },
  { label: t('lineage.typeReport'), value: 'report' },
  { label: t('lineage.typeManualEntry'), value: 'manual' }
]

// 节点颜色配置
const NODE_COLORS: Record<string, { bg: string; border: string }> = {
  center:     { bg: '#3b82f6', border: '#1d4ed8' },
  upstream:   { bg: '#10b981', border: '#059669' },
  downstream: { bg: '#f59e0b', border: '#d97706' },
}
const EDGE_COLORS: Record<string, string> = {
  sql: '#3b82f6', etl: '#10b981', report: '#f59e0b', collect: '#8b5cf6', manual: '#6b7280'
}

// 渲染 ECharts 图
const renderGraph = () => {
  if (!echartsContainer.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(echartsContainer.value)
  }

  const eNodes = graphData.nodes.map(n => {
    const colors = NODE_COLORS[n.type] || NODE_COLORS.center
    const upstream = graphData.nodes.filter(x => x.type === 'upstream')
    const downstream = graphData.nodes.filter(x => x.type === 'downstream')
    let x = 400, y = 300
    if (n.type === 'center') { x = 400; y = 300 }
    else if (n.type === 'upstream') {
      const idx = upstream.indexOf(n)
      x = 100
      y = (600 / (upstream.length + 1)) * (idx + 1)
    } else if (n.type === 'downstream') {
      const idx = downstream.indexOf(n)
      x = 700
      y = (600 / (downstream.length + 1)) * (idx + 1)
    }
    return {
      name: n.id,
      x, y,
      symbolSize: n.type === 'center' ? 60 : 46,
      symbol: 'roundRect',
      label: {
        show: true,
        formatter: n.label.length > 16 ? n.label.substring(0, 16) + '...' : n.label,
        fontSize: 11,
        color: '#fff',
        fontWeight: n.type === 'center' ? 'bold' as const : 'normal' as const,
      },
      itemStyle: {
        color: colors.bg,
        borderColor: colors.border,
        borderWidth: 2,
        shadowBlur: n.type === 'center' ? 12 : 4,
        shadowColor: 'rgba(0,0,0,0.15)',
      },
      emphasis: {
        itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0,0,0,0.3)' },
        label: { fontSize: 13 },
      },
    }
  })

  const eLinks = graphData.edges.map(e => ({
    source: e.source,
    target: e.target,
    lineStyle: {
      color: EDGE_COLORS[e.lineageType] || '#999',
      width: 2,
      curveness: 0.2,
    },
    label: {
      show: true,
      formatter: e.lineageType,
      fontSize: 10,
      color: '#666',
    },
  }))

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') return `<b>${params.name}</b>`
        if (params.dataType === 'edge') return `${params.data.source} → ${params.data.target}`
        return ''
      }
    },
    legend: [{
      data: [
        { name: t('lineage.upstream'), icon: 'roundRect', itemStyle: { color: '#10b981' } },
        { name: t('lineage.center'), icon: 'roundRect', itemStyle: { color: '#3b82f6' } },
        { name: t('lineage.downstream'), icon: 'roundRect', itemStyle: { color: '#f59e0b' } },
      ],
      top: 8, left: 'center', show: false,
    }],
    series: [{
      type: 'graph',
      layout: 'none',
      roam: true,
      draggable: true,
      zoom: 0.9,
      nodeScaleRatio: 0,
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 10],
      data: eNodes,
      links: eLinks,
      lineStyle: { opacity: 0.8 },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 4 },
      },
      animationDuration: 600,
      animationEasingUpdate: 'quinticInOut',
    }]
  }, true)

  chartInstance.off('click')
  chartInstance.on('click', (params: any) => {
    if (params.dataType === 'node') {
      onNodeClick({ id: params.name })
    }
  })
}

const resetGraphZoom = () => {
  if (chartInstance) {
    chartInstance.dispatchAction({ type: 'restore' })
  }
}

watch(() => [graphData.nodes.length, graphData.edges.length], () => {
  if (graphData.nodes.length > 0) {
    nextTick(() => renderGraph())
  }
})

const handleResize = () => { chartInstance?.resize() }
onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})

// --- 表格列定义（使用 StatusTag 和 ActionButtons）(Req 9.3, 9.4) ---
const getLineageActions = (row: any): ActionConfig[] => [
  { label: t('common.edit'), type: 'info', onClick: () => openEditModal(row) },
  { label: t('common.delete'), type: 'error', confirm: t('lineage.confirmDeleteLineage'), onClick: () => deleteLineage(row.id) }
]

const columns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: t('lineage.sourceTable'), key: 'sourceTable', ellipsis: { tooltip: true } },
  { title: t('lineage.sourceColumn'), key: 'sourceColumn', width: 100, ellipsis: { tooltip: true },
    render: (row: any) => row.sourceColumn || '-'
  },
  { title: t('lineage.targetTable'), key: 'targetTable', ellipsis: { tooltip: true } },
  { title: t('lineage.targetColumn'), key: 'targetColumn', width: 100, ellipsis: { tooltip: true },
    render: (row: any) => row.targetColumn || '-'
  },
  { title: t('lineage.type'), key: 'lineageType', width: 80,
    render: (row: any) => h(StatusTag, { status: row.lineageType || '', statusMap: lineageTypeStatusMap })
  },
  { title: t('lineage.transformLogic'), key: 'transformLogic', width: 150, ellipsis: { tooltip: true },
    render: (row: any) => row.transformLogic || '-'
  },
  { title: t('common.actions'), key: 'actions', width: 120,
    render: (row: any) => h(ActionButtons, { actions: getLineageActions(row), row })
  }
]

// 搜索血缘
const searchLineage = async () => {
  if (!searchTable.value) {
    message.warning(t('lineage.enterTableWarning'))
    return
  }
  
  loading.value = true
  try {
    const res: any = await dataLineageApi.getGraph(searchTable.value, searchDepth.value)
    if (res?.data) {
      graphData.nodes = res.data.nodes || []
      graphData.edges = res.data.edges || []
      graphData.centerTable = res.data.centerTable || searchTable.value
    }
  } catch (e: any) {
    message.error(t('lineage.queryFailed') + ': ' + (e.message || ''))
  } finally {
    loading.value = false
  }
  
  loadLineageList()
}

// 加载血缘列表
const loadLineageList = async () => {
  listLoading.value = true
  try {
    if (searchTable.value) {
      const res: any = await dataLineageApi.getByTable(searchTable.value)
      lineageList.value = res?.data || []
    } else {
      const res: any = await dataLineageApi.getAll()
      lineageList.value = res?.data || []
    }
  } catch (e) {
    lineageList.value = []
  } finally {
    listLoading.value = false
  }
}

// 保存血缘（通过 addModal.submit 调用 createFn）
const saveLineage = async () => {
  if (!addModal.formData.value.sourceTable || !addModal.formData.value.targetTable) {
    message.warning(t('lineage.sourceTargetRequired'))
    return
  }
  await addModal.submit()
}

// 删除血缘
const deleteLineage = async (id: number) => {
  try {
    await dataLineageApi.delete(id)
    message.success(t('common.deleteSuccess'))
    loadLineageList()
  } catch (e: any) {
    message.error(t('lineage.deleteFailed') + ': ' + (e.message || ''))
  }
}

// 加载统计
const loadStatistics = async () => {
  try {
    const res: any = await dataLineageApi.getStatistics()
    statistics.value = res?.data || {}
    showStatsModal.value = true
  } catch (e) {
    message.error(t('lineage.loadStatsFailed'))
  }
}

// 节点点击
const onNodeClick = (node: any) => {
  searchTable.value = node.id
  searchLineage()
}

// 血缘列表行点击
const lineageRowProps = (row: any) => ({
  style: 'cursor: pointer;',
  onClick: (e: MouseEvent) => {
    if ((e.target as HTMLElement).closest('.n-button')) return
    const table = row.sourceTable || row.targetTable
    if (table) {
      searchTable.value = table
      searchLineage()
      nextTick(() => {
        document.querySelector('.graph-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
    }
  }
})

// 打开编辑弹窗
const openEditModal = (row: any) => {
  editSourceTableOptions.value = []
  editTargetTableOptions.value = []
  editModal.openEdit({
    id: row.id,
    sourceDataSourceId: null,
    sourceTable: row.sourceTable || '',
    sourceDatabase: row.sourceDatabase || '',
    sourceColumn: row.sourceColumn || '',
    targetDataSourceId: null,
    targetTable: row.targetTable || '',
    targetDatabase: row.targetDatabase || '',
    targetColumn: row.targetColumn || '',
    lineageType: row.lineageType || 'sql',
    transformLogic: row.transformLogic || ''
  })
}

// 更新血缘
const updateLineage = async () => {
  await editModal.submit()
}

// AI分析血缘
const runAiAnalysis = async () => {
  if (!aiAnalysisTable.value) {
    message.warning(t('lineage.enterTableWarning'))
    return
  }
  
  aiAnalyzing.value = true
  aiAnalysisResult.value = ''
  
  try {
    const res: any = await request.post('/ai/analyze-lineage', {
      tableName: aiAnalysisTable.value,
      dataSourceId: aiAnalysisDataSource.value
    })
    if (res?.data?.success) {
      aiAnalysisResult.value = res.data.response || res.data.content || t('lineage.analysisComplete')
    } else {
      message.error(t('lineage.analysisFailed') + ': ' + (res?.data?.error || ''))
    }
  } catch (e: any) {
    message.error(t('lineage.aiAnalysisFailed') + ': ' + (e.message || ''))
  } finally {
    aiAnalyzing.value = false
  }
}

// 自动发现血缘
const autoDiscoverLineage = async () => {
  discovering.value = true
  try {
    const res: any = await dataLineageApi.autoDiscover()
    if (res?.data?.success) {
      message.success(t('lineage.discoveredCount', { count: res.data.discoveredCount || 0 }))
      loadLineageList()
      if (searchTable.value) searchLineage()
    } else {
      message.info(res?.data?.message || t('lineage.noNewLineage'))
    }
  } catch (e: any) {
    message.error(t('lineage.autoDiscoverFailed') + ': ' + (e.message || ''))
  } finally {
    discovering.value = false
  }
}

// ==================== 影响分析 ====================
const showImpactModal = ref(false)
const analyzingImpact = ref(false)
const impactTable = ref('')
const impactResult = ref<any>(null)

const runImpactAnalysis = async () => {
  if (!impactTable.value) { message.warning(t('lineage.enterTableWarning')); return }
  analyzingImpact.value = true
  impactResult.value = null
  try {
    const res: any = await dataLineageApi.getImpactAnalysis(impactTable.value)
    impactResult.value = res?.data || null
  } catch (e: any) {
    message.error(t('lineage.impactAnalysisFailed') + ': ' + (e.message || ''))
  } finally {
    analyzingImpact.value = false
  }
}

// ==================== SQL解析血缘 ====================
const parsingSql = ref(false)
const parsedLineages = ref<any[]>([])
const parseSqlForm = reactive({ sql: '', targetTable: '' })

const parseSqlLineage = async () => {
  if (!parseSqlForm.sql || !parseSqlForm.targetTable) {
    message.warning(t('lineage.enterSqlAndTable'))
    return
  }
  parsingSql.value = true
  try {
    const res: any = await dataLineageApi.parseSql(parseSqlForm)
    parsedLineages.value = res?.data || []
    if (parsedLineages.value.length > 0) {
      await dataLineageApi.batchSave(parsedLineages.value)
      message.success(t('lineage.parsedAndSaved', { count: parsedLineages.value.length }))
      loadLineageList()
    } else {
      message.info(t('lineage.noTableDeps'))
    }
  } catch (e: any) {
    message.error(t('lineage.parseFailed') + ': ' + (e.message || ''))
  } finally {
    parsingSql.value = false
  }
}

// 加载数据源列表
const loadDataSources = async () => {
  try {
    const res: any = await request.get('/data-source/list', { params: { page: 1, pageSize: 100 } })
    const list = res?.data?.list || res?.data || []
    dataSources.value = list
    dataSourceOptions.value = list.map((ds: any) => ({
      label: `${ds.name} (${ds.dbType})`,
      value: ds.id
    }))
  } catch (e) {
    console.error('[loadDataSources]', e)
  }
}

// ==================== 智能分析功能 ====================
const loadingHealth = ref(false)
const showHealthModal = ref(false)
const healthReport = ref<any>(null)
const showHotspotModal = ref(false)
const hotspotData = ref<any>(null)
const showOrphanModal = ref(false)
const orphanData = ref<any>(null)
const showFullChainModal = ref(false)
const fullChainData = ref<any>(null)
const fullChainTable = ref('')

const analysisMenuOptions = [
  { label: t('lineage.menuHealthReport'), key: 'health' },
  { label: t('lineage.menuHotspot'), key: 'hotspot' },
  { label: t('lineage.menuOrphan'), key: 'orphan' },
  { label: t('lineage.menuFullChain'), key: 'fullchain' },
  { label: t('lineage.menuImpact'), key: 'impact' },
  { label: t('lineage.menuAi'), key: 'ai' },
  { label: t('lineage.menuStats'), key: 'stats' }
]

const onAnalysisMenuSelect = (key: string) => {
  switch (key) {
    case 'health': loadHealthReport(); break
    case 'hotspot': loadHotspotAnalysis(); break
    case 'orphan': loadOrphanAnalysis(); break
    case 'fullchain': showFullChainModal.value = true; fullChainTable.value = searchTable.value; break
    case 'impact': showImpactModal.value = true; impactTable.value = searchTable.value; break
    case 'ai': showAiAnalysis.value = true; break
    case 'stats': loadStatistics(); break
  }
}

const loadHealthReport = async () => {
  loadingHealth.value = true
  try {
    const res: any = await dataLineageApi.getHealthReport()
    healthReport.value = res?.data || {}
    showHealthModal.value = true
  } catch (e: any) {
    message.error(t('lineage.loadHealthFailed') + ': ' + (e.message || ''))
  } finally {
    loadingHealth.value = false
  }
}

const loadHotspotAnalysis = async () => {
  try {
    const res: any = await dataLineageApi.getHotspotAnalysis(10)
    hotspotData.value = res?.data || {}
    showHotspotModal.value = true
  } catch (e: any) {
    message.error(t('lineage.loadHotspotFailed') + ': ' + (e.message || ''))
  }
}

const loadOrphanAnalysis = async () => {
  try {
    const res: any = await dataLineageApi.getOrphanAnalysis()
    orphanData.value = res?.data || {}
    showOrphanModal.value = true
  } catch (e: any) {
    message.error(t('lineage.loadOrphanFailed') + ': ' + (e.message || ''))
  }
}

const loadFullChainAnalysis = async () => {
  if (!fullChainTable.value) { message.warning(t('lineage.enterTableWarning')); return }
  try {
    const res: any = await dataLineageApi.getFullChain(fullChainTable.value, 5)
    fullChainData.value = res?.data || {}
  } catch (e: any) {
    message.error(t('lineage.loadFullChainFailed') + ': ' + (e.message || ''))
  }
}

// 热点表列定义（使用 StatusTag）(Req 9.3)
const hotspotColumns = [
  { title: t('lineage.tableName'), key: 'tableName', ellipsis: { tooltip: true } },
  { title: t('lineage.upstreamCount'), key: 'upstreamCount', width: 80 },
  { title: t('lineage.downstreamCount'), key: 'downstreamCount', width: 80 },
  { title: t('lineage.hotScore'), key: 'hotScore', width: 80,
    render: (row: any) => row.hotScore?.toFixed(1) || '-'
  },
  { title: t('lineage.risk'), key: 'riskLevel', width: 80,
    render: (row: any) => h(StatusTag, { status: row.riskLevel || 'low', statusMap: riskLevelMap })
  },
  { title: t('lineage.suggestion'), key: 'suggestion', ellipsis: { tooltip: true } }
]

// 初始化
onMounted(() => {
  loadDataSources()
  loadLineageList()
})
</script>

<style scoped>
.data-lineage-page {
  display: flex;
  flex-direction: column;
  gap: var(--dp-spacing-md);
}

.search-card {
  border-radius: var(--dp-card-radius);
  box-shadow: var(--dp-shadow-sm);
}

.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--dp-spacing-sm);
}

.search-fields {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
  flex-wrap: wrap;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
}

.graph-card {
  border-radius: var(--dp-card-radius);
  box-shadow: var(--dp-shadow-sm);
}

.echarts-graph {
  width: 100%;
  height: 480px;
  border-radius: var(--dp-radius-sm);
  background: #fafbfc;
}

.list-card {
  border-radius: var(--dp-card-radius);
  box-shadow: var(--dp-shadow-sm);
}

.ai-result {
  background: #f8fafc;
  border-radius: var(--dp-radius-md);
  padding: var(--dp-spacing-md);
  max-height: 400px;
  overflow-y: auto;
}

.markdown-content {
  font-size: var(--dp-font-md);
  line-height: 1.6;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3 {
  margin-top: var(--dp-spacing-md);
  margin-bottom: var(--dp-spacing-sm);
  color: #1e293b;
}

.markdown-content ul,
.markdown-content ol {
  padding-left: 20px;
}

.markdown-content code {
  background: #e2e8f0;
  padding: 2px 6px;
  border-radius: var(--dp-radius-sm);
  font-size: var(--dp-font-sm);
}

.markdown-content pre {
  background: #1e293b;
  color: #e2e8f0;
  padding: var(--dp-spacing-sm);
  border-radius: var(--dp-radius-md);
  overflow-x: auto;
}

.markdown-content pre code {
  background: transparent;
  padding: 0;
}

.lineage-form-row {
  display: flex;
  gap: var(--dp-spacing-sm);
  align-items: flex-start;
}

.lineage-form-panel {
  flex: 1;
  padding: 14px var(--dp-spacing-md) 6px;
  border-radius: var(--dp-radius-md);
  background: #fafbfc;
}

.source-panel {
  border-left: 3px solid #18a058;
}

.target-panel {
  border-left: 3px solid #f0a020;
}

.panel-title {
  font-weight: 600;
  font-size: var(--dp-font-sm);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #333;
}

.panel-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.source-dot { background: #18a058; }
.target-dot { background: #f0a020; }

.lineage-arrow {
  display: flex;
  align-items: center;
  font-size: 22px;
  color: #bbb;
  padding-top: 36px;
  font-weight: bold;
  user-select: none;
}

</style>

<style>
/* DataLineage 深色模式（非 scoped） */
html.dark .echarts-graph { background: #1a2536 !important; }
html.dark .ai-result { background: #1a2536 !important; }
html.dark .markdown-content h1,
html.dark .markdown-content h1,
html.dark .markdown-content h2,
html.dark .markdown-content h1,
html.dark .markdown-content h2,
html.dark .markdown-content h3 { color: #e2e8f0 !important; }
html.dark .markdown-content code { background: #334155 !important; color: #e2e8f0 !important; }
html.dark .lineage-form-panel { background: #1a2536 !important; }
html.dark .panel-title { color: #e2e8f0 !important; }
html.dark .lineage-arrow { color: #64748b !important; }
</style>
