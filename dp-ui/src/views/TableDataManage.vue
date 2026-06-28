<template>
  <div class="table-data-manage-page">
    <div class="app-header">
      <div class="app-title">📊 数据管理中心</div>
      <div class="app-actions">
        <n-tag :type="selectedDataSourceId ? 'success' : 'warning'" round>
          {{ selectedDataSourceId ? '● 已选择数据源' : '● 请选择数据源' }}
        </n-tag>
      </div>
    </div>

    <div class="app-body">
      <!-- 侧边栏 -->
      <div class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <n-button class="sidebar-toggle" circle size="small" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? '›' : '‹' }}
        </n-button>

        <!-- 数据源选择面板 -->
        <div v-if="!sidebarCollapsed" class="connection-panel">
          <div class="connection-title">
            <n-icon :component="ServerOutline" :color="'var(--color-primary, #2563eb)'" />
            <span>数据源</span>
          </div>
          <n-select
            v-model:value="selectedDataSourceId"
            :options="dataSourceOptions"
            placeholder="选择数据源"
            @update:value="handleDataSourceChange"
          />
          <div v-if="selectedDataSourceId" class="connected-panel">
            <div class="connected-info">
              <span class="connected-icon">🗄️</span>
              {{ currentDataSourceName }}
            </div>
          </div>
        </div>

        <!-- 表列表面板 -->
        <div v-if="!sidebarCollapsed" class="objects-panel">
          <div class="search-box">
            <n-input v-model:value="tableSearch" placeholder="搜索表名..." clearable size="small">
              <template #suffix><n-icon :component="SearchOutline" /></template>
            </n-input>
          </div>
          <div v-if="selectedDataSourceId" style="padding: 0 8px 8px; display: flex; gap: 4px;">
            <n-button size="tiny" type="primary" @click="showCreateTableModal = true">建表</n-button>
            <n-button size="tiny" :disabled="!selectedTable" @click="openAlterTableModal">改表</n-button>
          </div>

          <div v-if="!selectedDataSourceId" class="empty-objects">
            <div class="empty-icon">🗄️</div>
            <p>请先选择数据源</p>
          </div>

          <div v-else>
            <!-- 表列表 -->
            <div class="object-group">
              <div class="group-header" @click="tablesExpanded = !tablesExpanded">
                <span>📊 数据表 ({{ filteredTables.length }})</span>
                <span class="group-icon">{{ tablesExpanded ? '▼' : '▶' }}</span>
              </div>
              <n-spin :show="loadingTables" size="small">
                <div v-show="tablesExpanded" class="group-content">
                  <div
                    v-for="table in filteredTables"
                    :key="table.tableName"
                    class="tree-item"
                    :class="{ active: selectedTable === table.tableName }"
                    @click="handleTableSelect(table.tableName)"
                  >
                    <span class="tree-item-icon">📋</span>
                    <span class="object-name">{{ table.tableName }}</span>
                    <span v-if="table.remarks" class="object-comment">({{ table.remarks }})</span>
                  </div>
                  <div v-if="filteredTables.length === 0 && !loadingTables" class="empty-list">
                    暂无数据表
                  </div>
                </div>
              </n-spin>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <div class="content-header">
          <div class="content-title">
            <span v-if="selectedTable">📋 {{ selectedTable }}</span>
            <span v-else>请从左侧选择数据表</span>
          </div>
          <div v-if="selectedTable" class="content-actions">
            <n-button size="small" :loading="loadingData" @click="loadTableData">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              刷新
            </n-button>
          </div>
        </div>

        <div class="content-body">
          <!-- 标签页 -->
          <div v-if="selectedTable" class="custom-tabs">
            <div class="tab-item" :class="{ active: activeTab === 'data' }" @click="activeTab = 'data'">📋 数据查询</div>
            <div class="tab-item" :class="{ active: activeTab === 'structure' }" @click="activeTab = 'structure'">🏗️ 表结构</div>
            <div class="tab-item" :class="{ active: activeTab === 'sql' }" @click="activeTab = 'sql'">⚡ SQL执行</div>
          </div>

          <!-- 数据查询标签页 -->
          <div v-show="activeTab === 'data' && selectedTable" class="tab-content">
            <div class="toolbar">
              <n-space align="center" :wrap="false">
                <n-button :type="filterCount > 0 ? 'primary' : 'default'" @click="showFilterModal = true">
                  <template #icon><n-icon><FilterOutline /></n-icon></template>
                  筛选 {{ filterCount > 0 ? `(${filterCount})` : '' }}
                </n-button>
                <n-select
                  v-model:value="orderByColumn"
                  :options="columnOptions"
                  placeholder="排序字段"
                  clearable
                  style="width: 140px"
                />
                <n-select
                  v-model:value="orderByDirection"
                  :options="[{ label: '升序', value: 'ASC' }, { label: '降序', value: 'DESC' }]"
                  style="width: 90px"
                />
                <n-button type="primary" @click="loadTableData">
                  <template #icon><n-icon><SearchOutline /></n-icon></template>
                  查询
                </n-button>
                <n-button @click="resetQuery">重置</n-button>
              </n-space>
              <n-space align="center" :wrap="false">
                <n-button type="success" @click="handleAddRow">
                  <template #icon><n-icon><AddOutline /></n-icon></template>
                  新增
                </n-button>
                <n-button @click="showImportModal = true">
                  <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
                  导入
                </n-button>
                <n-button type="error" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
                  <template #icon><n-icon><TrashOutline /></n-icon></template>
                  删除 {{ selectedRows.length > 0 ? `(${selectedRows.length})` : '' }}
                </n-button>
              </n-space>
            </div>

            <!-- 筛选条件标签 -->
            <div v-if="filterConditions.length > 0" class="filter-tags">
              <n-tag 
                v-for="(cond, idx) in filterConditions" 
                :key="idx" 
                closable 
                type="info"
                size="small"
                @close="removeFilterCondition(idx)"
              >
                {{ cond.field }} {{ getOperatorLabel(cond.operator) }} {{ cond.value }}
              </n-tag>
              <n-button size="tiny" quaternary @click="clearAllFilters">清除全部</n-button>
            </div>

            <div class="query-result">
              <div class="result-header">
                <span class="result-count">共 {{ totalRows }} 条记录{{ totalRows > tableData.length ? `，当前显示 ${tableData.length} 条` : '' }}</span>
              </div>
              <n-spin :show="loadingData">
                <n-data-table
                  :columns="dataColumns"
                  :data="tableData"
                  :row-key="(row: any) => row[primaryKey] || JSON.stringify(row)"
                  :checked-row-keys="selectedRows"
                  :max-height="400"
                  :scroll-x="Math.max(dataColumns.length * 150, 800)"
                  striped
                  size="small"
                  @update:checked-row-keys="handleRowSelect"
                />
              </n-spin>
              <div class="pagination-wrapper">
                <n-pagination
                  v-model:page="currentPage"
                  v-model:page-size="pageSize"
                  :item-count="totalRows"
                  :page-sizes="[20, 50, 100, 200, 500]"
                  show-size-picker
                  show-quick-jumper
                  @update:page="loadTableData"
                  @update:page-size="handlePageSizeChange"
                />
              </div>
            </div>
          </div>

          <!-- 表结构标签页 -->
          <div v-show="activeTab === 'structure' && selectedTable" class="tab-content">
            <div class="result-header">
              <span class="result-title">🏗️ {{ selectedTable }} 表结构</span>
              <n-button size="small" @click="copyTableDDL">
                <template #icon><n-icon><CopyOutline /></n-icon></template>
                复制DDL
              </n-button>
            </div>
            <n-spin :show="loadingStructure">
              <n-data-table :columns="structureColumns" :data="tableStructure" size="small" striped :max-height="500" />
            </n-spin>
          </div>

          <!-- SQL执行标签页 -->
          <div v-show="activeTab === 'sql' && selectedTable" class="tab-content">
            <div class="toolbar">
              <n-button @click="formatSql">🎨 格式化</n-button>
              <n-button @click="sqlContent = ''">🗑️ 清空</n-button>
              <span class="toolbar-divider"></span>
              <n-button type="primary" @click="executeSql">
                <template #icon><n-icon><PlayOutline /></n-icon></template>
                执行 (Ctrl+Enter)
              </n-button>
              <span class="toolbar-hint">💡 支持SELECT/INSERT/UPDATE/DELETE语句</span>
            </div>
            <div class="sql-editor-container">
              <n-input
                ref="sqlEditorRef"
                v-model:value="sqlContent"
                type="textarea"
                :rows="8"
                class="sql-editor"
                placeholder="-- 请输入SQL语句&#10;SELECT * FROM table_name WHERE condition"
                @keydown="handleSqlKeydown"
              />
            </div>
            <div v-if="sqlResult" class="sql-result">
              <n-alert :type="sqlResult.success ? 'success' : 'error'" style="margin-bottom: 12px;">
                {{ sqlResult.message }}
                <span v-if="sqlResult.executeTime" class="text-muted" style="margin-left: 12px;">耗时: {{ sqlResult.executeTime }}ms</span>
              </n-alert>
              <div v-if="sqlResult.type === 'SELECT' && sqlResult.data">
                <n-data-table :columns="sqlResultColumns" :data="sqlResult.data" :max-height="300" size="small" striped :pagination="{ pageSize: 50 }" />
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!selectedTable" class="empty-content">
            <div class="empty-icon">👈</div>
            <h3>请从左侧选择一个数据表</h3>
            <p>选择表后可以查看和编辑数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="card" :bordered="false" class="edit-modal dp-modal-lg" style="max-height: 85vh;">
      <template #header>
        <div class="edit-modal-header">
          <div class="edit-modal-icon" :class="editMode === 'edit' ? 'icon-edit' : 'icon-add'">
            <n-icon :size="18"><CreateOutline v-if="editMode === 'edit'" /><AddOutline v-else /></n-icon>
          </div>
          <div>
            <div class="edit-modal-title">{{ editMode === 'add' ? '新增数据' : '编辑数据' }}</div>
            <div class="edit-modal-desc">{{ selectedTable }} · {{ editableColumns.length }} 个字段</div>
          </div>
        </div>
      </template>
      <n-scrollbar style="max-height: 60vh;">
        <!-- 主键区域 -->
        <div v-if="editMode === 'edit' && pkColumns.length > 0" class="edit-pk-section">
          <div class="edit-section-badge">
            <n-icon :size="12" color="#d97706"><InformationCircleOutline /></n-icon>
            <span>主键字段（不可修改）</span>
          </div>
          <div class="edit-pk-grid" :class="{ 'pk-single': pkColumns.length === 1 }">
            <div v-for="col in pkColumns" :key="col.columnName" class="edit-pk-item">
              <span class="edit-pk-label">{{ col.remarks || col.columnName }}</span>
              <span class="edit-pk-value">{{ editForm[col.columnName] ?? '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 可编辑字段 -->
        <n-form ref="editFormRef" :model="editForm" label-placement="left" label-width="140px" class="edit-form">
          <n-form-item
            v-for="col in nonPkColumns"
            :key="col.columnName"
            :path="col.columnName"
          >
            <template #label>
              <div class="edit-field-label-wrap">
                <span class="edit-field-label">{{ col.remarks || col.columnName }}</span>
                <span v-if="col.remarks" class="edit-field-col-name">{{ col.columnName }}</span>
              </div>
            </template>
            <n-input
              v-if="isTextType(col.dataType)"
              v-model:value="editForm[col.columnName]"
              :placeholder="`请输入${col.remarks || col.columnName}`"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 3 }"
              clearable
            />
            <n-input-number
              v-else-if="isNumberType(col.dataType)"
              v-model:value="editForm[col.columnName]"
              :placeholder="`请输入${col.remarks || col.columnName}`"
              style="width: 100%"
              clearable
            />
            <n-date-picker
              v-else-if="isDateType(col.dataType)"
              v-model:value="editForm[col.columnName]"
              :type="col.dataType.includes('datetime') || col.dataType.includes('timestamp') ? 'datetime' : 'date'"
              :placeholder="`请选择${col.remarks || col.columnName}`"
              style="width: 100%"
              clearable
            />
            <n-input
              v-else
              v-model:value="editForm[col.columnName]"
              :placeholder="`请输入${col.remarks || col.columnName}`"
              clearable
            />
          </n-form-item>
        </n-form>
      </n-scrollbar>
      <template #footer>
        <div class="edit-modal-footer">
          <span class="edit-modal-footer-hint">{{ nonPkColumns.length }} 个可编辑字段</span>
          <n-space :size="12">
            <n-button @click="showEditModal = false">取消</n-button>
            <n-button type="primary" :loading="saving" @click="handleSaveRow">
              <template #icon><n-icon><CreateOutline v-if="editMode === 'edit'" /><AddOutline v-else /></n-icon></template>
              {{ editMode === 'edit' ? '保存修改' : '确认新增' }}
            </n-button>
          </n-space>
        </div>
      </template>
    </n-modal>

    <!-- 导入弹窗 -->
    <n-modal v-model:show="showImportModal" preset="card" title="导入数据" class="dp-modal-lg">
      <n-form label-placement="left" label-width="120px">
        <n-form-item label="文件类型">
          <n-radio-group v-model:value="importFormat">
            <n-radio value="excel">Excel (.xlsx)</n-radio>
            <n-radio value="csv">CSV (.csv)</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="导入模板">
          <n-button type="info" size="small" :loading="downloadingTemplate" @click="handleDownloadTemplate">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            下载导入模板
          </n-button>
          <span class="form-hint" style="margin-left: 12px;">含表头字段名，可直接填写数据后导入</span>
        </n-form-item>
        <n-form-item label="选择文件">
          <n-upload
            :accept="importFormat === 'excel' ? '.xlsx,.xls' : '.csv'"
            :max="1"
            :default-upload="false"
            @change="handleFileChange"
          >
            <n-button>📁 选择文件</n-button>
          </n-upload>
          <span v-if="importFile" class="text-secondary" style="margin-left: 12px;">{{ importFile.name }}</span>
        </n-form-item>
        <n-form-item label="导入模式">
          <n-radio-group v-model:value="importMode" @update:value="handleImportModeChange">
            <n-space vertical>
              <n-radio value="append">
                <span>追加导入</span>
                <span class="text-tertiary" style="font-size: 12px; margin-left: 8px;">直接插入新数据，不检查重复</span>
              </n-radio>
              <n-radio value="increment">
                <span class="text-primary">增量导入</span>
                <span class="text-tertiary" style="font-size: 12px; margin-left: 8px;">根据唯一字段判断：存在则更新，不存在则插入</span>
              </n-radio>
              <n-radio value="replace">
                <span class="text-error">全量导入</span>
                <span class="text-tertiary" style="font-size: 12px; margin-left: 8px;">清空表后导入（危险操作）</span>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        
        <!-- 增量导入时选择唯一字段 -->
        <n-form-item v-if="importMode === 'increment'" label="唯一标识字段">
          <n-select
            v-model:value="importUniqueFields"
            :options="importFieldOptions"
            multiple
            placeholder="选择用于判断数据是否存在的字段（可多选）"
            style="width: 100%"
          />
          <template #feedback>
            <span class="form-hint">
              根据选择的字段判断数据是否已存在，存在则更新，不存在则插入
            </span>
          </template>
        </n-form-item>

        <!-- 增量导入时选择更新字段 -->
        <n-form-item v-if="importMode === 'increment'" label="更新字段">
          <n-radio-group v-model:value="importUpdateFieldMode">
            <n-space vertical>
              <n-radio value="all">
                <span>更新所有字段</span>
              </n-radio>
              <n-radio value="custom">
                <span>自定义更新字段</span>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        
        <n-form-item v-if="importMode === 'increment' && importUpdateFieldMode === 'custom'" label="选择更新字段">
          <n-checkbox-group v-model:value="importUpdateFields">
            <n-space wrap>
              <n-checkbox 
                v-for="col in tableStructure" 
                :key="col.columnName" 
                :value="col.columnName"
                :disabled="importUniqueFields.includes(col.columnName)"
              >
                {{ col.columnName }}
                <n-tag v-if="col.isPrimaryKey" type="warning" size="tiny" style="margin-left: 4px;">PK</n-tag>
              </n-checkbox>
            </n-space>
          </n-checkbox-group>
        </n-form-item>

        <n-alert :type="importMode === 'replace' ? 'warning' : 'info'" style="margin-top: 12px;">
          <template #header>{{ importMode === 'replace' ? '⚠️ 警告' : '📋 导入说明' }}</template>
          <ul style="margin: 0; padding-left: 20px; font-size: 13px;">
            <li v-if="importMode === 'replace'" style="color: var(--color-error);">全量导入将清空表中所有现有数据！</li>
            <li>Excel/CSV文件第一行必须是字段名（与表字段名一致）</li>
            <li>只会导入与表字段名匹配的列</li>
            <li v-if="importMode === 'increment'">增量导入会根据唯一字段判断：存在则UPDATE，不存在则INSERT</li>
            <li>日期格式建议使用: yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss</li>
          </ul>
        </n-alert>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showImportModal = false">取消</n-button>
          <n-button type="primary" :loading="importing" :disabled="importMode === 'increment' && importUniqueFields.length === 0" @click="handleImport">
            开始导入
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 建表弹窗 -->
    <n-modal v-model:show="showCreateTableModal" preset="card" title="新建数据表" class="dp-modal-lg">
      <n-form label-placement="left" label-width="100px">
        <n-form-item label="表名">
          <n-input v-model:value="createTableForm.tableName" placeholder="输入表名" />
        </n-form-item>
        <n-form-item label="表注释">
          <n-input v-model:value="createTableForm.tableComment" placeholder="可选：表注释" />
        </n-form-item>
        <n-form-item label="字段定义">
          <div style="width: 100%">
            <n-data-table :columns="ddlFieldColumns" :data="createTableForm.columns" size="small" :max-height="300" />
            <n-button size="small" dashed type="primary" style="margin-top: 8px" @click="addDdlColumn">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              添加字段
            </n-button>
          </div>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateTableModal = false">取消</n-button>
          <n-button type="primary" :loading="creatingTable" @click="handleCreateTable">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 改表弹窗 -->
    <n-modal v-model:show="showAlterTableModal" preset="card" :title="`修改表结构: ${selectedTable}`" class="dp-modal-lg">
      <n-form label-placement="left" label-width="100px">
        <n-form-item label="操作类型">
          <n-radio-group v-model:value="alterTableForm.action">
            <n-radio value="ADD">添加字段</n-radio>
            <n-radio value="MODIFY">修改字段</n-radio>
            <n-radio value="DROP">删除字段</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="字段名">
          <n-input v-model:value="alterTableForm.columnName" placeholder="字段名" />
        </n-form-item>
        <n-form-item v-if="alterTableForm.action !== 'DROP'" label="数据类型">
          <n-select v-model:value="alterTableForm.dataType" :options="ddlDataTypeOptions" placeholder="选择数据类型" style="width: 200px" />
        </n-form-item>
        <n-form-item v-if="alterTableForm.action !== 'DROP'" label="长度">
          <n-input-number v-model:value="alterTableForm.length" :min="0" placeholder="可选" style="width: 150px" />
        </n-form-item>
        <n-form-item v-if="alterTableForm.action !== 'DROP'" label="注释">
          <n-input v-model:value="alterTableForm.comment" placeholder="可选" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showAlterTableModal = false">取消</n-button>
          <n-button :type="alterTableForm.action === 'DROP' ? 'error' : 'primary'" :loading="alteringTable" @click="handleAlterTable">执行</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 筛选弹窗 -->
    <n-modal v-model:show="showFilterModal" preset="card" title="筛选条件" class="dp-modal-lg">
      <n-form label-placement="left" label-width="80px">
        <n-form-item
          v-for="(filter, index) in tempFilters"
          :key="index"
          :label="`条件 ${index + 1}`"
        >
          <n-space style="width: 100%" align="center">
            <n-select
              v-model:value="filter.field"
              :options="filterFieldOptions"
              placeholder="选择字段"
              style="width: 150px"
              clearable
              filterable
            />
            <n-select
              v-model:value="filter.operator"
              :options="getOperatorOptions(filter.field)"
              placeholder="操作符"
              style="width: 120px"
              :disabled="!filter.field"
            />
            <n-input
              v-model:value="filter.value"
              placeholder="输入值"
              style="flex: 1; min-width: 150px"
              :disabled="!filter.field || filter.operator === 'isNull' || filter.operator === 'isNotNull'"
              clearable
            />
            <n-button
              v-if="tempFilters.length > 1"
              quaternary
              type="error"
              size="small"
              @click="removeTempFilter(index)"
            >
              <template #icon><n-icon><CloseOutline /></n-icon></template>
            </n-button>
          </n-space>
        </n-form-item>
        <n-form-item label=" ">
          <n-button dashed type="primary" size="small" @click="addTempFilter">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            添加条件
          </n-button>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="resetTempFilters">重置</n-button>
          <n-button @click="showFilterModal = false">取消</n-button>
          <n-button type="primary" @click="applyFilters">应用筛选</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, NIcon, useMessage, useDialog } from 'naive-ui'
import type { UploadFileInfo } from 'naive-ui'
import {
  ServerOutline,
  SearchOutline,
  RefreshOutline,
  AddOutline,
  CloudUploadOutline,
  DownloadOutline,
  TrashOutline,
  CopyOutline,
  PlayOutline,
  InformationCircleOutline,
  FilterOutline,
  CloseOutline,
  CreateOutline
} from '@vicons/ionicons5'
import * as tableDataApi from '@/api/tableData'
import { NRadio, NRadioGroup, NInputNumber } from 'naive-ui'
import { initMessage } from '@/utils/message'
import { formatDateTime, formatCellValueSmart } from '@/utils/format'
import { useTabsStore } from '@/stores/tabs'

const tabsStore = useTabsStore()

const message = useMessage()
const dialog = useDialog()
initMessage(message)

// 侧边栏
const sidebarCollapsed = ref(false)

// 数据源相关
const dataSources = ref<any[]>([])
const selectedDataSourceId = ref<number | null>(null)
const dataSourceOptions = computed(() =>
  dataSources.value.map(ds => ({ label: ds.name, value: ds.id }))
)
const currentDataSourceName = computed(() => {
  const ds = dataSources.value.find(d => d.id === selectedDataSourceId.value)
  return ds ? ds.name : ''
})

// 表列表相关
const tables = ref<any[]>([])
const tableSearch = ref('')
const selectedTable = ref<string | null>(null)
const loadingTables = ref(false)
const tablesExpanded = ref(true)
const filteredTables = computed(() => {
  if (!tableSearch.value) return tables.value
  const kw = tableSearch.value.toLowerCase()
  return tables.value.filter(t =>
    t.tableName.toLowerCase().includes(kw) ||
    (t.remarks && t.remarks.toLowerCase().includes(kw))
  )
})

// 标签页
const activeTab = ref('data')

// 表结构相关
const tableStructure = ref<any[]>([])
const loadingStructure = ref(false)
const primaryKey = ref('id')
const structureColumns = [
  { title: '字段名', key: 'columnName', width: 150, render: (row: any) => h('strong', null, row.columnName) },
  { title: '数据类型', key: 'dataType', width: 120 },
  { title: '长度', key: 'columnSize', width: 80, render: (row: any) => row.columnSize || '-' },
  { title: '允许空', key: 'nullable', width: 80, render: (row: any) => row.nullable === 'YES' ? '✅' : '❌' },
  { title: '主键', key: 'isPrimaryKey', width: 60, render: (row: any) => row.isPrimaryKey ? '🔑' : '-' },
  { title: '默认值', key: 'defaultValue', width: 120, render: (row: any) => row.defaultValue || '-' },
  { title: '注释', key: 'remarks', ellipsis: { tooltip: true }, render: (row: any) => row.remarks || '-' }
]

// 数据查询相关
const tableData = ref<any[]>([])
const loadingData = ref(false)
const currentPage = ref(1)
const pageSize = ref(50)
const totalRows = ref(0)
const orderByColumn = ref<string | null>(null)
const orderByDirection = ref('ASC')
const selectedRows = ref<any[]>([])

// 筛选相关
interface FilterCondition {
  field: string | null
  operator: string | null
  value: string | null
}
const showFilterModal = ref(false)
const filterConditions = ref<FilterCondition[]>([])
const tempFilters = ref<FilterCondition[]>([{ field: null, operator: null, value: null }])

const filterCount = computed(() => filterConditions.value.length)

const filterFieldOptions = computed(() =>
  tableStructure.value.map(col => ({
    label: col.columnName + (col.remarks ? ` (${col.remarks})` : ''),
    value: col.columnName,
    type: getFieldType(col.dataType)
  }))
)

const getFieldType = (dataType: string): string => {
  const t = dataType.toLowerCase()
  if (t.includes('int') || t.includes('decimal') || t.includes('float') || t.includes('double') || t.includes('number')) {
    return 'number'
  }
  if (t.includes('date') || t.includes('time')) {
    return 'date'
  }
  return 'string'
}

const getOperatorOptions = (field: string | null) => {
  if (!field) return []
  const col = tableStructure.value.find(c => c.columnName === field)
  const type = col ? getFieldType(col.dataType) : 'string'
  
  if (type === 'number') {
    return [
      { label: '等于', value: 'eq' },
      { label: '不等于', value: 'ne' },
      { label: '大于', value: 'gt' },
      { label: '大于等于', value: 'gte' },
      { label: '小于', value: 'lt' },
      { label: '小于等于', value: 'lte' },
      { label: '为空', value: 'isNull' },
      { label: '不为空', value: 'isNotNull' }
    ]
  }
  return [
    { label: '等于', value: 'eq' },
    { label: '不等于', value: 'ne' },
    { label: '包含', value: 'contains' },
    { label: '开始于', value: 'startsWith' },
    { label: '结束于', value: 'endsWith' },
    { label: '为空', value: 'isNull' },
    { label: '不为空', value: 'isNotNull' }
  ]
}

const getOperatorLabel = (op: string | null): string => {
  const map: Record<string, string> = {
    eq: '=', ne: '≠', gt: '>', gte: '≥', lt: '<', lte: '≤',
    contains: '包含', startsWith: '开始于', endsWith: '结束于',
    isNull: '为空', isNotNull: '不为空'
  }
  return map[op || ''] || op || ''
}

const addTempFilter = () => {
  tempFilters.value.push({ field: null, operator: null, value: null })
}

const removeTempFilter = (index: number) => {
  tempFilters.value.splice(index, 1)
  if (tempFilters.value.length === 0) {
    tempFilters.value.push({ field: null, operator: null, value: null })
  }
}

const resetTempFilters = () => {
  tempFilters.value = [{ field: null, operator: null, value: null }]
}

const applyFilters = () => {
  filterConditions.value = tempFilters.value
    .filter(f => f.field && f.operator && (f.operator === 'isNull' || f.operator === 'isNotNull' || f.value))
    .map(f => ({ ...f }))
  showFilterModal.value = false
  currentPage.value = 1
  loadTableData()
}

const removeFilterCondition = (index: number) => {
  filterConditions.value.splice(index, 1)
  loadTableData()
}

const clearAllFilters = () => {
  filterConditions.value = []
  tempFilters.value = [{ field: null, operator: null, value: null }]
  loadTableData()
}

// 构建 WHERE 子句
const buildWhereClause = (): string => {
  if (filterConditions.value.length === 0) return ''
  
  const clauses = filterConditions.value.map(c => {
    const field = `\`${c.field}\``
    const value = c.value
    
    switch (c.operator) {
      case 'eq': return `${field} = '${value}'`
      case 'ne': return `${field} != '${value}'`
      case 'gt': return `${field} > '${value}'`
      case 'gte': return `${field} >= '${value}'`
      case 'lt': return `${field} < '${value}'`
      case 'lte': return `${field} <= '${value}'`
      case 'contains': return `${field} LIKE '%${value}%'`
      case 'startsWith': return `${field} LIKE '${value}%'`
      case 'endsWith': return `${field} LIKE '%${value}'`
      case 'isNull': return `${field} IS NULL`
      case 'isNotNull': return `${field} IS NOT NULL`
      default: return ''
    }
  }).filter(Boolean)
  
  return clauses.join(' AND ')
}

const columnOptions = computed(() =>
  tableStructure.value.map(col => ({ label: col.columnName, value: col.columnName }))
)

// 数据表格列
const dataColumns = computed(() => {
  if (tableStructure.value.length === 0) return []

  const cols: any[] = [{ type: 'selection', fixed: 'left', width: 50 }]

  tableStructure.value.forEach(col => {
    cols.push({
      title: col.columnName,
      key: col.columnName,
      width: Math.max(col.columnName.length * 12, 100),
      ellipsis: { tooltip: true },
      render: (row: any) => {
        const val = row[col.columnName]
        if (val === null || val === undefined) {
          return h('span', { style: 'color: #999; font-style: italic' }, 'NULL')
        }
        
        // 使用智能格式化函数处理日期等类型
        const formatted = formatCellValueSmart(val, { 
          fieldName: col.columnName, 
          fieldType: col.dataType 
        })
        
        // 如果格式化后的值过长，截断显示
        return formatted.length > 50 ? formatted.substring(0, 50) + '...' : formatted
      }
    })
  })

  cols.push({
    title: '操作',
    key: 'actions',
    width: 150,
    fixed: 'right',
    render: (row: any) => {
      return h(NSpace, { size: 4, wrap: false, align: 'center' }, () => [
        h(NButton, { size: 'tiny', type: 'primary', onClick: () => handleEditRow(row) }, { 
          icon: () => h(NIcon, { size: 14 }, () => h(CreateOutline)),
          default: () => '编辑'
        }),
        h(NButton, { size: 'tiny', type: 'error', onClick: () => handleDeleteRow(row) }, { 
          icon: () => h(NIcon, { size: 14 }, () => h(TrashOutline)),
          default: () => '删除'
        })
      ])
    }
  })

  return cols
})

// 编辑相关
const showEditModal = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const editForm = ref<Record<string, any>>({})
const editFormRef = ref<any>(null)
const saving = ref(false)
const editableColumns = computed(() => tableStructure.value)
const pkColumns = computed(() => tableStructure.value.filter(c => c.isPrimaryKey))
const nonPkColumns = computed(() => editMode.value === 'add' ? tableStructure.value : tableStructure.value.filter(c => !c.isPrimaryKey))

// 导入相关
const showImportModal = ref(false)
const importFormat = ref('excel')
const importMode = ref('append')
const importFile = ref<File | null>(null)
const importing = ref(false)
const downloadingTemplate = ref(false)
const importUniqueFields = ref<string[]>([])
const importUpdateFieldMode = ref('all')
const importUpdateFields = ref<string[]>([])

// 导入字段选项
const importFieldOptions = computed(() =>
  tableStructure.value.map(col => ({
    label: col.columnName + (col.isPrimaryKey ? ' (主键)' : '') + (col.remarks ? ` - ${col.remarks}` : ''),
    value: col.columnName
  }))
)

// 导入模式变更时重置
const handleImportModeChange = () => {
  if (importMode.value === 'increment') {
    // 默认选择主键作为唯一字段
    const pkFields = tableStructure.value.filter(col => col.isPrimaryKey).map(col => col.columnName)
    importUniqueFields.value = pkFields.length > 0 ? pkFields : []
    importUpdateFieldMode.value = 'all'
    importUpdateFields.value = []
  } else {
    importUniqueFields.value = []
    importUpdateFields.value = []
  }
}

// SQL执行相关
const sqlContent = ref('')
const sqlResult = ref<any>(null)
const sqlEditorRef = ref<any>(null)

// SQL结果列
const sqlResultColumns = computed(() => {
  if (!sqlResult.value?.columns) return []
  return sqlResult.value.columns.map((col: string) => ({
    title: col,
    key: col,
    width: 120,
    ellipsis: { tooltip: true },
    render: (row: any) => {
      const val = row[col]
      if (val === null || val === undefined) return h('span', { style: 'color: #999; font-style: italic;' }, 'NULL')
      
      // 使用智能格式化函数
      return formatCellValueSmart(val, { fieldName: col })
    }
  }))
})

// 类型判断辅助函数
const isTextType = (dataType: string) => {
  const t = dataType.toLowerCase()
  return t.includes('char') || t.includes('text') || t.includes('blob') || t.includes('clob')
}

const isNumberType = (dataType: string) => {
  const t = dataType.toLowerCase()
  return t.includes('int') || t.includes('decimal') || t.includes('float') || t.includes('double') || t.includes('number') || t.includes('numeric')
}

const isDateType = (dataType: string) => {
  const t = dataType.toLowerCase()
  return t.includes('date') || t.includes('time')
}

// 加载数据源
const loadDataSources = async () => {
  try {
    const res = await tableDataApi.getDataSources()
    // API 返回格式是 { data: { list: [...], total: n } }
    const pageData = res.data || {}
    dataSources.value = pageData.list || pageData || []
    if (dataSources.value.length > 0 && !selectedDataSourceId.value) {
      selectedDataSourceId.value = dataSources.value[0].id
      await loadTables()
    }
  } catch (error: any) {
    message.error('加载数据源失败: ' + (error.message || '未知错误'))
  }
}

// 数据源变更
const handleDataSourceChange = async () => {
  selectedTable.value = null
  tableData.value = []
  tableStructure.value = []
  totalRows.value = 0
  activeTab.value = 'data'
  await loadTables()
}

// 加载表列表
const loadTables = async () => {
  if (!selectedDataSourceId.value) return
  loadingTables.value = true
  try {
    const res = await tableDataApi.getTables(selectedDataSourceId.value)
    tables.value = res.data || []
  } catch (error: any) {
    message.error('加载表列表失败: ' + (error.message || '未知错误'))
  } finally {
    loadingTables.value = false
  }
}

// 选择表
const handleTableSelect = async (tableName: string) => {
  selectedTable.value = tableName
  currentPage.value = 1
  selectedRows.value = []
  activeTab.value = 'data'
  // 更新标签页标题
  tabsStore.updateTabTitle('/table-data-manage', `数据管理 - ${tableName}`)
  await Promise.all([loadTableStructure(), loadTableData()])
}

// 加载表结构
const loadTableStructure = async () => {
  if (!selectedDataSourceId.value || !selectedTable.value) return
  loadingStructure.value = true
  try {
    const res = await tableDataApi.getTableStructure(selectedDataSourceId.value, selectedTable.value)
    tableStructure.value = res.data || []
    const pk = tableStructure.value.find(col => col.isPrimaryKey)
    primaryKey.value = pk ? pk.columnName : (tableStructure.value[0]?.columnName || 'id')
  } catch (error: any) {
    message.error('加载表结构失败: ' + (error.message || '未知错误'))
  } finally {
    loadingStructure.value = false
  }
}

// 加载表数据
const loadTableData = async () => {
  if (!selectedDataSourceId.value || !selectedTable.value) return
  loadingData.value = true
  try {
    let orderBy = ''
    if (orderByColumn.value) {
      orderBy = `${orderByColumn.value} ${orderByDirection.value}`
    }
    const whereClause = buildWhereClause()
    const res = await tableDataApi.getTableData({
      dataSourceId: selectedDataSourceId.value,
      tableName: selectedTable.value,
      page: currentPage.value,
      pageSize: pageSize.value,
      where: whereClause || undefined,
      orderBy: orderBy || undefined
    })
    tableData.value = res.data?.list || []
    totalRows.value = res.data?.total || 0
  } catch (error: any) {
    message.error('加载数据失败: ' + (error.message || '未知错误'))
  } finally {
    loadingData.value = false
  }
}

// 分页大小变更
const handlePageSizeChange = () => {
  currentPage.value = 1
  loadTableData()
}

// 重置查询
const resetQuery = () => {
  filterConditions.value = []
  tempFilters.value = [{ field: null, operator: null, value: null }]
  orderByColumn.value = null
  orderByDirection.value = 'ASC'
  currentPage.value = 1
  loadTableData()
}

// 行选择
const handleRowSelect = (keys: any[]) => {
  selectedRows.value = keys
}

// 新增行
const handleAddRow = () => {
  editMode.value = 'add'
  editForm.value = {}
  tableStructure.value.forEach(col => {
    editForm.value[col.columnName] = col.defaultValue || null
  })
  showEditModal.value = true
}

// 编辑行
const handleEditRow = (row: any) => {
  editMode.value = 'edit'
  // 复制数据，并将日期时间戳转换为格式化字符串
  const formData: Record<string, any> = {}
  Object.keys(row).forEach(key => {
    const col = tableStructure.value.find(c => c.columnName === key)
    if (col) {
      const dataType = col.dataType.toLowerCase()
      const val = row[key]
      
      // 如果是日期类型且值是数字（时间戳），转换为格式化字符串
      if ((dataType.includes('date') || dataType.includes('time')) && val !== null && val !== undefined) {
        if (typeof val === 'number' || !isNaN(Number(val))) {
          formData[key] = formatDateTime(val, dataType.includes('datetime') || dataType.includes('timestamp') ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
        } else {
          formData[key] = val
        }
      } else {
        formData[key] = val
      }
    } else {
      formData[key] = row[key]
    }
  })
  editForm.value = formData
  showEditModal.value = true
}

// 保存行
const handleSaveRow = async () => {
  if (!selectedDataSourceId.value || !selectedTable.value) return
  saving.value = true
  try {
    // 处理日期字段：将时间戳转换为字符串
    const submitData: Record<string, any> = {}
    Object.keys(editForm.value).forEach(key => {
      const value = editForm.value[key]
      const col = tableStructure.value.find(c => c.columnName === key)
      
      if (col) {
        const dataType = col.dataType?.toLowerCase() || ''
        // 如果是日期类型且值是时间戳数字，转换为字符串
        if ((dataType.includes('date') || dataType.includes('time')) && typeof value === 'number') {
          submitData[key] = formatDateTime(value, dataType.includes('datetime') || dataType.includes('timestamp') ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
        } else {
          submitData[key] = value
        }
      } else {
        submitData[key] = value
      }
    })
    
    if (editMode.value === 'add') {
      await tableDataApi.insertRow({
        dataSourceId: selectedDataSourceId.value,
        tableName: selectedTable.value,
        data: submitData
      })
      message.success('新增成功')
    } else {
      // 编辑模式：确保主键值存在
      const pkValue = submitData[primaryKey.value]
      if (pkValue === undefined || pkValue === null) {
        message.error('主键值不能为空')
        saving.value = false
        return
      }
      await tableDataApi.updateRow({
        dataSourceId: selectedDataSourceId.value,
        tableName: selectedTable.value,
        data: submitData,
        primaryKey: primaryKey.value,
        primaryValue: pkValue
      })
      message.success('更新成功')
    }
    showEditModal.value = false
    await loadTableData()
  } catch (error: any) {
    message.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 删除行
const handleDeleteRow = (row: any) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除这条记录吗？此操作不可恢复！`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      if (!selectedDataSourceId.value || !selectedTable.value) return
      try {
        await tableDataApi.deleteRow({
          dataSourceId: selectedDataSourceId.value,
          tableName: selectedTable.value,
          primaryKey: primaryKey.value,
          primaryValue: row[primaryKey.value]
        })
        message.success('删除成功')
        await loadTableData()
      } catch (error: any) {
        message.error('删除失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return
  dialog.warning({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 条记录吗？此操作不可恢复！`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      if (!selectedDataSourceId.value || !selectedTable.value) return
      try {
        await tableDataApi.batchDeleteRows({
          dataSourceId: selectedDataSourceId.value,
          tableName: selectedTable.value,
          primaryKey: primaryKey.value,
          primaryValues: selectedRows.value
        })
        message.success('批量删除成功')
        selectedRows.value = []
        await loadTableData()
      } catch (error: any) {
        message.error('批量删除失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

// 下载导入模板
const handleDownloadTemplate = async () => {
  if (!selectedDataSourceId.value || !selectedTable.value) {
    message.warning('请先选择数据表')
    return
  }
  downloadingTemplate.value = true
  try {
    const res = await tableDataApi.downloadImportTemplate(selectedDataSourceId.value, selectedTable.value)
    const blob = new Blob([res.data || res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${selectedTable.value}_导入模板.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success('模板下载成功')
  } catch (error: any) {
    message.error('模板下载失败: ' + (error.message || '未知错误'))
  } finally {
    downloadingTemplate.value = false
  }
}

// 文件选择
const handleFileChange = (options: { fileList: UploadFileInfo[] }) => {
  if (options.fileList.length > 0) {
    importFile.value = options.fileList[0].file as File
  } else {
    importFile.value = null
  }
}

// 导入
const handleImport = async () => {
  if (!selectedDataSourceId.value || !selectedTable.value) {
    message.warning('请先选择数据表')
    return
  }
  if (!importFile.value) {
    message.warning('请选择要导入的文件')
    return
  }
  if (importMode.value === 'increment' && importUniqueFields.value.length === 0) {
    message.warning('增量导入模式请选择唯一标识字段')
    return
  }
  
  if (importMode.value === 'replace') {
    dialog.warning({
      title: '⚠️ 危险操作',
      content: '全量导入将清空表中所有现有数据，此操作不可恢复！确定要继续吗？',
      positiveText: '确定清空并导入',
      negativeText: '取消',
      onPositiveClick: () => doImport()
    })
  } else {
    doImport()
  }
}

const doImport = async () => {
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value!)
    formData.append('dataSourceId', String(selectedDataSourceId.value))
    formData.append('tableName', selectedTable.value!)
    formData.append('format', importFormat.value)
    formData.append('mode', importMode.value)
    
    // 增量导入参数
    if (importMode.value === 'increment') {
      formData.append('uniqueFields', importUniqueFields.value.join(','))
      formData.append('updateFieldMode', importUpdateFieldMode.value)
      if (importUpdateFieldMode.value === 'custom') {
        formData.append('updateFields', importUpdateFields.value.join(','))
      }
    }

    const res = await tableDataApi.importData(formData)
    const result = res.data || res
    
    let msg = `导入完成！`
    if (result.insertCount !== undefined) {
      msg += `新增: ${result.insertCount} 条`
    }
    if (result.updateCount !== undefined) {
      msg += `，更新: ${result.updateCount} 条`
    }
    if (result.successCount !== undefined && result.insertCount === undefined) {
      msg += `成功: ${result.successCount} 条`
    }
    if (result.failCount !== undefined && result.failCount > 0) {
      msg += `，失败: ${result.failCount} 条`
    }
    
    message.success(msg)
    showImportModal.value = false
    importFile.value = null
    await loadTableData()
  } catch (error: any) {
    message.error('导入失败: ' + (error.message || '未知错误'))
  } finally {
    importing.value = false
  }
}

// 复制表DDL
const copyTableDDL = () => {
  if (!selectedTable.value || tableStructure.value.length === 0) return
  
  let ddl = `CREATE TABLE \`${selectedTable.value}\` (\n`
  const columns: string[] = []
  const pks: string[] = []
  
  tableStructure.value.forEach(col => {
    let colDef = `  \`${col.columnName}\` ${col.dataType}`
    if (col.columnSize && !col.dataType.toLowerCase().includes('text')) {
      colDef += `(${col.columnSize})`
    }
    if (col.nullable !== 'YES') {
      colDef += ' NOT NULL'
    }
    if (col.defaultValue) {
      colDef += ` DEFAULT ${col.defaultValue}`
    }
    if (col.remarks) {
      colDef += ` COMMENT '${col.remarks}'`
    }
    columns.push(colDef)
    if (col.isPrimaryKey) {
      pks.push(`\`${col.columnName}\``)
    }
  })
  
  ddl += columns.join(',\n')
  if (pks.length > 0) {
    ddl += `,\n  PRIMARY KEY (${pks.join(', ')})`
  }
  ddl += '\n);'
  
  navigator.clipboard.writeText(ddl).then(() => {
    message.success('DDL已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}

// SQL执行
const executeSql = async () => {
  if (!selectedDataSourceId.value) {
    message.warning('请先选择数据源')
    return
  }
  if (!sqlContent.value.trim()) {
    message.warning('请输入SQL语句')
    return
  }
  
  try {
    const res = await tableDataApi.executeSql({
      dataSourceId: selectedDataSourceId.value,
      sql: sqlContent.value
    })
    sqlResult.value = res.data || res
    
    // 如果是修改数据的语句，刷新表数据
    if (sqlResult.value?.success) {
      const upperSql = sqlContent.value.toUpperCase()
      if (upperSql.includes('INSERT') || upperSql.includes('UPDATE') || upperSql.includes('DELETE')) {
        await loadTableData()
      }
    }
  } catch (error: any) {
    sqlResult.value = { success: false, message: error.message || 'SQL执行失败' }
  }
}

const handleSqlKeydown = (e: KeyboardEvent) => {
  if (e.ctrlKey && e.key === 'Enter') {
    e.preventDefault()
    executeSql()
  }
}

const formatSql = () => {
  let sql = sqlContent.value
  const keywords = ['SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'ORDER BY', 'GROUP BY', 'HAVING', 'LIMIT', 'JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'ON', 'INSERT INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE FROM']
  keywords.forEach(kw => {
    sql = sql.replace(new RegExp(`\\b${kw}\\b`, 'gi'), '\n' + kw.toUpperCase())
  })
  sqlContent.value = sql.trim()
}

// ==================== DDL 建表/改表 ====================
const showCreateTableModal = ref(false)
const showAlterTableModal = ref(false)
const creatingTable = ref(false)
const alteringTable = ref(false)

const ddlDataTypeOptions = [
  { label: 'VARCHAR', value: 'VARCHAR' },
  { label: 'INT', value: 'INT' },
  { label: 'BIGINT', value: 'BIGINT' },
  { label: 'TEXT', value: 'TEXT' },
  { label: 'DECIMAL', value: 'DECIMAL' },
  { label: 'DATETIME', value: 'DATETIME' },
  { label: 'DATE', value: 'DATE' },
  { label: 'FLOAT', value: 'FLOAT' },
  { label: 'DOUBLE', value: 'DOUBLE' },
  { label: 'TINYINT', value: 'TINYINT' },
  { label: 'BOOLEAN', value: 'BOOLEAN' },
  { label: 'TIMESTAMP', value: 'TIMESTAMP' }
]

interface DdlColumn { name: string; type: string; length: number | null; nullable: boolean; comment: string; primaryKey: boolean }

const createTableForm = ref({
  tableName: '',
  tableComment: '',
  columns: [{ name: 'id', type: 'BIGINT', length: null, nullable: false, comment: '主键', primaryKey: true }] as DdlColumn[]
})

const alterTableForm = ref({
  action: 'ADD' as 'ADD' | 'MODIFY' | 'DROP',
  columnName: '',
  dataType: 'VARCHAR',
  length: 255 as number | null,
  comment: ''
})

const ddlFieldColumns = [
  { title: '字段名', key: 'name', width: 120, render: (row: DdlColumn, idx: number) =>
    h(NButton, { text: true, type: 'info', size: 'small', onClick: () => {
      const val = prompt('字段名', row.name)
      if (val) createTableForm.value.columns[idx].name = val
    }}, { default: () => row.name || '点击编辑' }) },
  { title: '类型', key: 'type', width: 110, render: (row: DdlColumn) => row.type },
  { title: '长度', key: 'length', width: 80, render: (row: DdlColumn) => row.length || '-' },
  { title: '主键', key: 'primaryKey', width: 60, render: (row: DdlColumn) => row.primaryKey ? '🔑' : '-' },
  { title: '操作', key: 'actions', width: 60, render: (_row: DdlColumn, idx: number) =>
    h(NButton, { size: 'tiny', type: 'error', text: true, onClick: () => createTableForm.value.columns.splice(idx, 1) }, { default: () => '删除' }) }
]

const addDdlColumn = () => {
  createTableForm.value.columns.push({ name: '', type: 'VARCHAR', length: 255, nullable: true, comment: '', primaryKey: false })
}

const handleCreateTable = async () => {
  if (!selectedDataSourceId.value) { message.warning('请先选择数据源'); return }
  if (!createTableForm.value.tableName) { message.warning('请输入表名'); return }
  if (createTableForm.value.columns.length === 0) { message.warning('请至少添加一个字段'); return }
  creatingTable.value = true
  try {
    await tableDataApi.createTable({
      dataSourceId: selectedDataSourceId.value,
      tableName: createTableForm.value.tableName,
      tableComment: createTableForm.value.tableComment,
      columns: createTableForm.value.columns
    })
    message.success('建表成功')
    showCreateTableModal.value = false
    createTableForm.value = { tableName: '', tableComment: '', columns: [{ name: 'id', type: 'BIGINT', length: null, nullable: false, comment: '主键', primaryKey: true }] }
    await loadTables()
  } catch (error: any) {
    message.error('建表失败: ' + (error.message || '未知错误'))
  } finally {
    creatingTable.value = false
  }
}

const openAlterTableModal = () => {
  if (!selectedTable.value) { message.warning('请先选择表'); return }
  alterTableForm.value = { action: 'ADD', columnName: '', dataType: 'VARCHAR', length: 255, comment: '' }
  showAlterTableModal.value = true
}

const handleAlterTable = async () => {
  if (!selectedDataSourceId.value || !selectedTable.value) return
  if (!alterTableForm.value.columnName) { message.warning('请输入字段名'); return }
  alteringTable.value = true
  try {
    await tableDataApi.alterTable({
      dataSourceId: selectedDataSourceId.value,
      tableName: selectedTable.value,
      action: alterTableForm.value.action,
      columnName: alterTableForm.value.columnName,
      dataType: alterTableForm.value.action !== 'DROP' ? alterTableForm.value.dataType : undefined,
      length: alterTableForm.value.action !== 'DROP' ? alterTableForm.value.length : undefined,
      comment: alterTableForm.value.action !== 'DROP' ? alterTableForm.value.comment : undefined
    })
    message.success('表结构修改成功')
    showAlterTableModal.value = false
    await loadTableStructure()
  } catch (error: any) {
    message.error('修改失败: ' + (error.message || '未知错误'))
  } finally {
    alteringTable.value = false
  }
}

onMounted(() => {
  loadDataSources()
})
</script>

<style scoped>
/* 主应用 */
.table-data-manage-page { height: calc(100vh - 178px); display: flex; flex-direction: column; background: #f5f7fa; border-radius: 8px; }
.app-header { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); color: white; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center; border-radius: 8px 8px 0 0; }
.app-title { font-size: 18px; font-weight: 600; }
.app-actions { display: flex; gap: 10px; align-items: center; }
.app-body { display: flex; flex: 1; overflow: hidden; background: white; border-radius: 0 0 8px 8px; }

/* 侧边栏 */
.sidebar { width: 300px; min-width: 300px; background: white; border-right: 1px solid #e6e6e6; display: flex; flex-direction: column; position: relative; transition: all 0.3s; }
.sidebar.collapsed { width: 50px; min-width: 50px; }
.sidebar-toggle { position: absolute; right: -15px; top: 20px; z-index: 10; }

.connection-panel { padding: 15px; border-bottom: 1px solid #e6e6e6; background: linear-gradient(135deg, #f8f9ff 0%, #e8f0ff 100%); }
.connection-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.connected-panel { margin-top: 12px; padding: 10px; background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); border-radius: 6px; }
.connected-info { font-size: 13px; color: #1565c0; display: flex; align-items: center; gap: 6px; }
.connected-icon { font-size: 16px; }

/* 对象树 */
.objects-panel { flex: 1; padding: 12px; overflow-y: auto; }
.search-box { margin-bottom: 12px; }
.empty-objects { text-align: center; color: #999; padding: 30px 15px; }
.empty-objects .empty-icon { font-size: 32px; margin-bottom: 10px; }
.empty-list { text-align: center; color: #999; padding: 15px; font-size: 13px; }

.object-group { margin-bottom: 10px; }
.group-header { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; background: linear-gradient(135deg, #f8f9ff 0%, #e8f0ff 100%); border-radius: 6px; cursor: pointer; font-weight: 600; font-size: 13px; color: #333; }
.group-header:hover { background: linear-gradient(135deg, #e8f0ff 0%, #d8e8ff 100%); }
.group-content { max-height: 400px; overflow-y: auto; }

.tree-item { padding: 8px 12px; cursor: pointer; border-radius: 5px; margin: 2px 0; color: #555; display: flex; align-items: center; font-size: 13px; }
.tree-item:hover { background: #f0f2f5; color: #333; }
.tree-item.active { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); color: white; }
.tree-item-icon { margin-right: 6px; }
.object-name { font-weight: 500; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.object-comment { color: #999; font-size: 11px; margin-left: 4px; }
.tree-item.active .object-comment { color: rgba(255,255,255,0.8); }

/* 主内容区 */
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.content-header { padding: 12px 20px; border-bottom: 1px solid #e6e6e6; display: flex; justify-content: space-between; align-items: center; background: #fafbfc; }
.content-title { font-size: 16px; font-weight: 600; color: #333; }
.content-body { flex: 1; overflow: hidden; display: flex; flex-direction: column; }

/* 标签页 */
.custom-tabs { display: flex; border-bottom: 1px solid #e6e6e6; background: #fafbfc; flex-shrink: 0; }
.tab-item { padding: 10px 18px; cursor: pointer; border-bottom: 3px solid transparent; color: #666; font-weight: 500; font-size: 13px; }
.tab-item:hover { color: #333; background: #f0f2f5; }
.tab-item.active { color: var(--color-primary, #2563eb); border-bottom-color: var(--color-primary, #2563eb); background: white; }

.tab-content { flex: 1; padding: 16px; overflow: auto; }

/* 工具栏 */
.toolbar { display: flex; gap: 12px; margin-bottom: 12px; padding: 14px 16px; background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%); border-radius: 10px; align-items: center; justify-content: space-between; flex-wrap: wrap; border: 1px solid #e8ecf4; }
.toolbar-hint { color: #666; font-size: 12px; }
.toolbar-divider { border-left: 1px solid #e0e0e0; height: 20px; margin: 0 4px; }

/* 筛选标签 */
.filter-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; padding: 10px 14px; background: #f0f7ff; border-radius: 8px; align-items: center; border: 1px dashed #b3d4fc; }

/* 空内容 */
.empty-content { text-align: center; color: #999; padding: 50px 20px; }
.empty-content .empty-icon { font-size: 42px; margin-bottom: 15px; }
.empty-content h3 { margin: 0 0 8px; color: #666; font-size: 16px; }
.empty-content p { margin: 0; font-size: 13px; }

/* 查询结果 */
.query-result { border-radius: 8px; background: white; }
.result-header { margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; padding: 8px 0; }
.result-title { font-weight: 600; color: #333; }
.result-count { color: #64748b; font-size: 13px; background: #f1f5f9; padding: 4px 12px; border-radius: 20px; }
.pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; padding: 12px 0; border-top: 1px solid #e2e8f0; }

/* SQL编辑器 */
.sql-editor-container { position: relative; margin-bottom: 12px; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
.sql-editor { font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important; font-size: 13px !important; line-height: 1.6 !important; }
.sql-result { margin-top: 16px; }

/* 数据表格优化 */
:deep(.n-data-table) { border-radius: 8px; border: 1px solid #e2e8f0; }
:deep(.n-data-table-th) { background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important; font-weight: 600; color: #475569; }
:deep(.n-data-table-td) { color: #334155; }
:deep(.n-data-table-tr:hover .n-data-table-td) { background: #f8fafc !important; }

/* 弹窗优化 */
:deep(.n-card-header) { padding: 16px 20px !important; background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%); border-bottom: 1px solid #e2e8f0; }
:deep(.n-card__content) { padding: 20px !important; }
:deep(.n-card__footer) { padding: 12px 20px !important; background: #f8fafc; border-top: 1px solid #e2e8f0; }

/* 编辑弹窗美化 */
.edit-modal-header { display: flex; align-items: center; gap: 12px; }
.edit-modal-icon { width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.edit-modal-icon.icon-edit { background: linear-gradient(135deg, #dbeafe, #eff6ff); color: #2563eb; }
.edit-modal-icon.icon-add { background: linear-gradient(135deg, #d1fae5, #ecfdf5); color: #059669; }
.edit-modal-title { font-size: 16px; font-weight: 600; color: #1e293b; }
.edit-modal-desc { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.edit-pk-section { background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 12px 14px; margin-bottom: 20px; }
.edit-section-badge { display: flex; align-items: center; gap: 5px; font-size: 11px; color: #b45309; font-weight: 500; margin-bottom: 8px; }
.edit-pk-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 8px; }
.edit-pk-grid.pk-single { grid-template-columns: 1fr; }
.edit-pk-item { display: flex; align-items: center; gap: 8px; background: #fff; border-radius: 6px; padding: 6px 10px; border: 1px solid #fde68a; }
.edit-pk-label { font-size: 12px; color: #92400e; font-weight: 500; white-space: nowrap; }
.edit-pk-value { font-size: 13px; color: #1e293b; font-weight: 600; font-family: 'SF Mono', Monaco, monospace; }
.edit-form { padding: 4px 0; }
.edit-field-label-wrap { display: flex; flex-direction: column; gap: 1px; }
.edit-field-label { font-weight: 500; color: #334155; font-size: 13px; }
.edit-field-col-name { font-size: 10px; color: #94a3b8; font-family: monospace; }
.edit-modal-footer { display: flex; align-items: center; justify-content: space-between; }
.edit-modal-footer-hint { font-size: 12px; color: #94a3b8; }
.edit-form :deep(.n-form-item) { margin-bottom: 14px; padding-bottom: 14px; border-bottom: 1px dashed #f1f5f9; }
.edit-form :deep(.n-form-item:last-child) { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }

/* 按钮优化 */
:deep(.n-button--primary-type) { background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af)); border: none; }
:deep(.n-button--primary-type:hover) { filter: brightness(0.9); }
:deep(.n-button--success-type) { background: linear-gradient(135deg, #10b981 0%, #059669 100%); border: none; }
:deep(.n-button--error-type) { background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); border: none; }

/* 输入框优化 */
:deep(.n-input) { border-radius: 6px; }
:deep(.n-select .n-base-selection) { border-radius: 6px; }

/* 标签优化 */
:deep(.n-tag) { border-radius: 4px; }

/* 响应式 */
@media (max-width: 1200px) {
  .toolbar { flex-direction: column; gap: 12px; }
  .toolbar > :deep(.n-space) { width: 100%; justify-content: flex-start; flex-wrap: wrap; }
}

@media (max-width: 768px) {
  .sidebar { width: 250px; min-width: 250px; }
  .sidebar.collapsed { width: 0; min-width: 0; overflow: hidden; }
}

</style>

<style>
/* TableDataManage 深色模式（非 scoped） */
html.dark .table-data-manage-page { background: #0f172a !important; }
html.dark .app-body { background: #1e293b !important; }
html.dark .sidebar { background: #1e293b !important; border-right-color: #334155 !important; }
html.dark .connection-panel { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; border-bottom-color: #334155 !important; }
html.dark .connection-title { color: #e2e8f0 !important; }
html.dark .connected-panel { background: linear-gradient(135deg, rgba(59, 130, 246, 0.12) 0%, rgba(59, 130, 246, 0.06) 100%) !important; }
html.dark .connected-info { color: #60a5fa !important; }
html.dark .empty-objects { color: #64748b !important; }
html.dark .empty-list { color: #64748b !important; }
html.dark .group-header { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; color: #e2e8f0 !important; }
html.dark .group-header:hover { background: linear-gradient(135deg, #243044 0%, #2a3649 100%) !important; }
html.dark .tree-item { color: #cbd5e1 !important; }
html.dark .tree-item:hover { background: #243044 !important; color: #f1f5f9 !important; }
html.dark .object-comment { color: #64748b !important; }
html.dark .content-header { background: #1a2536 !important; border-bottom-color: #334155 !important; }
html.dark .content-title { color: #f1f5f9 !important; }
html.dark .custom-tabs { background: #1a2536 !important; border-bottom-color: #334155 !important; }
html.dark .tab-item { color: #94a3b8 !important; }
html.dark .tab-item:hover { color: #e2e8f0 !important; background: #243044 !important; }
html.dark .tab-item.active { color: #818cf8 !important; border-bottom-color: #818cf8 !important; background: #1e293b !important; }
html.dark .toolbar { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; border-color: #334155 !important; }
html.dark .toolbar-hint { color: #94a3b8 !important; }
html.dark .toolbar-divider { border-left-color: #334155 !important; }
html.dark .filter-tags { background: rgba(59, 130, 246, 0.06) !important; border-color: rgba(59, 130, 246, 0.2) !important; }
html.dark .empty-content { color: #64748b !important; }
html.dark .empty-content h3 { color: #94a3b8 !important; }
html.dark .query-result { background: #1e293b !important; }
html.dark .result-title { color: #f1f5f9 !important; }
html.dark .result-count { color: #94a3b8 !important; background: #243044 !important; }
html.dark .pagination-wrapper { border-top-color: #334155 !important; }
html.dark .sql-editor-container { border-color: #334155 !important; }
html.dark .n-data-table { border-color: #334155 !important; }
html.dark .n-data-table-th { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; color: #e2e8f0 !important; }
html.dark .n-data-table-td { color: #cbd5e1 !important; }
html.dark .n-data-table-tr:hover .n-data-table-td { background: #243044 !important; }
html.dark .n-card-header { background: linear-gradient(135deg, #1a2536 0%, #1e293b 100%) !important; border-bottom-color: #334155 !important; }
html.dark .n-card__footer { background: #1a2536 !important; border-top-color: #334155 !important; }
html.dark .edit-modal-title { color: #f1f5f9 !important; }
html.dark .edit-modal-icon.icon-edit { background: linear-gradient(135deg, rgba(37, 99, 235, 0.2), rgba(37, 99, 235, 0.1)) !important; color: #60a5fa !important; }
html.dark .edit-modal-icon.icon-add { background: linear-gradient(135deg, rgba(5, 150, 105, 0.2), rgba(5, 150, 105, 0.1)) !important; color: #34d399 !important; }
html.dark .edit-pk-section { background: rgba(245, 158, 11, 0.08) !important; border-color: rgba(245, 158, 11, 0.25) !important; }
html.dark .edit-section-badge { color: #fbbf24 !important; }
html.dark .edit-pk-item { background: #1a2536 !important; border-color: rgba(245, 158, 11, 0.2) !important; }
html.dark .edit-pk-label { color: #fbbf24 !important; }
html.dark .edit-pk-value { color: #f1f5f9 !important; }
html.dark .edit-field-label { color: #cbd5e1 !important; }
html.dark .edit-form .n-form-item { border-bottom-color: #334155 !important; }
</style>
