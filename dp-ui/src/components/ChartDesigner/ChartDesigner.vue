<template>
  <div class="chart-designer-container">
    <!-- 顶部工具栏 -->
    <div class="designer-header">
      <div class="header-left">
        <n-button text class="header-back-btn" @click="handleCancel">
          <template #icon><n-icon size="18"><ArrowBackOutline /></n-icon></template>
        </n-button>
        <span class="header-divider-dot">•</span>
        <span class="title-text">{{ form.id ? '编辑图表' : '新建图表' }}</span>
      </div>
      <div class="header-center">
        <n-input
          v-model:value="form.chartName"
          placeholder="图表名称"
          style="width: 220px"
          size="small"
        />
        <span class="header-sep"></span>
        <n-select
          v-model:value="form.chartType"
          :options="chartTypeOptions"
          placeholder="图表类型"
          style="width: 130px"
          size="small"
          @update:value="handleChartTypeChange"
        />
        <n-button size="small" quaternary @click="showTemplateModal = true">
          <template #icon><n-icon size="15"><ColorPaletteOutline /></n-icon></template>
          模板
        </n-button>
      </div>
      <div class="header-right">
        <n-button size="small" @click="handleCancel">取消</n-button>
        <n-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
          保存图表
        </n-button>
      </div>
    </div>

    <div class="designer-content">
      <!-- 左侧：数据源和字段配置 - 折叠面板版 -->
      <div class="designer-left-panel">
        <n-card size="small" class="panel-card" :bordered="false">
          <!-- 数据源选择 - 始终显示 -->
          <div class="datasource-section">
            <div class="config-section">
              <div class="section-title">
                <n-icon size="16" style="margin-right: 6px;"><BarChartOutline /></n-icon>
                数据源
              </div>
              <n-select
                v-model:value="form.dataSourceId"
                :options="dataSourceOptions"
                placeholder="选择数据源"
                :loading="dataSourceLoading"
                filterable
                clearable
                size="small"
                @update:value="handleDataSourceChange"
              />
            </div>
            
            <!-- 🆕 数据配置类型选择（必选，编辑时锁定） -->
            <div v-if="form.dataSourceId" class="config-section" style="margin-top: 12px;">
              <div class="section-title">
                配置类型
                <span style="color: #d03050; margin-left: 2px;">*</span>
                <n-tag v-if="form.id" size="tiny" type="warning" style="margin-left: 8px;">已锁定</n-tag>
              </div>
              <n-select
                v-model:value="dataConfigMode"
                :options="dataConfigModeOptions"
                placeholder="请选择配置类型"
                size="small"
                :disabled="!!form.id"
              />
              <div class="mode-hint">
                <template v-if="form.id">
                  {{ dataConfigMode === 'field' ? '自定义字段模式（创建后不可更改类型）' : 'SQL模式（创建后不可更改类型）' }}
                </template>
                <template v-else>
                  {{ dataConfigMode === 'field' ? '选择数据表和字段，自动生成SQL' : '直接编写或编辑SQL语句' }}
                </template>
              </div>
            </div>
            
            <!-- 字段模式：选择数据表 -->
            <div v-if="form.dataSourceId && dataConfigMode === 'field'" class="config-section" style="margin-top: 12px;">
              <div class="section-title">数据表</div>
              <n-select
                v-model:value="selectedTable"
                :options="tableOptions"
                placeholder="选择数据表"
                :loading="tablesLoading"
                filterable
                size="small"
                @update:value="handleTableChange"
              />
            </div>
          </div>

          <!-- 折叠面板区域 - 仅字段模式显示 -->
          <n-collapse 
            v-if="dataConfigMode === 'field' && selectedTable" 
            :default-expanded-names="['fields', 'mapping', 'filter', 'params']"
            class="config-collapse"
          >
            <!-- 可用字段 -->
            <n-collapse-item name="fields">
              <template #header>
                <div class="collapse-header">
                  <span>可用字段</span>
                  <n-badge :value="columnOptions.length" :max="99" type="info" />
                </div>
              </template>
              <n-spin :show="columnsLoading" size="small">
                <div v-if="columnOptions.length > 0" class="field-list-compact">
                  <n-dropdown
                    trigger="manual"
                    placement="bottom-start"
                    :show="showFieldContextMenu"
                    :x="fieldContextMenuX"
                    :y="fieldContextMenuY"
                    :options="fieldContextMenuOptions"
                    @select="handleFieldContextMenuSelect"
                    @clickoutside="showFieldContextMenu = false"
                  />
                  <n-tooltip v-for="col in columnOptions" :key="col.value" trigger="hover" placement="right">
                    <template #trigger>
                      <div
                        class="field-item-compact"
                        draggable="true"
                        @dragstart="handleFieldDragStart($event, col)"
                        @contextmenu.prevent="handleFieldContextMenu($event, col)"
                      >
                        <span class="field-name">{{ col.label }}</span>
                        <n-tag size="tiny" :bordered="false" :type="getFieldTagType(col.dataType)">{{ col.dataType }}</n-tag>
                      </div>
                    </template>
                    右键添加为参数或设置轴
                  </n-tooltip>
                </div>
                <n-empty v-else-if="!columnsLoading" description="暂无字段" size="small" />
              </n-spin>
            </n-collapse-item>

            <!-- 字段映射 - 默认展开 -->
            <n-collapse-item name="mapping">
              <template #header>
                <div class="collapse-header">
                  <span>字段映射</span>
                  <n-badge v-if="fieldMapping.xAxis || fieldMapping.yAxis.length > 0 || (['table', 'summaryTable'].includes(form.chartType || '') && tableStyleConfig.displayColumns.length > 0) || (form.chartType === 'pivotTable' && tableStyleConfig.pivotRowField)" dot type="success" />
                </div>
              </template>
              <div class="mapping-content">
                <!-- 表格类型：字段选择 -->
                <template v-if="form.chartType === 'table' || form.chartType === 'summaryTable'">
                  <div class="field-config-item">
                    <label>
                      显示字段
                      <n-tag size="tiny" :bordered="false" type="info" style="margin-left: 8px;">
                        {{ tableStyleConfig.displayColumns.length > 0 ? tableStyleConfig.displayColumns.length + '个' : '全部' }}
                      </n-tag>
                    </label>
                    <n-select
                      v-model:value="tableStyleConfig.displayColumns"
                      :options="columnOptions"
                      placeholder="留空显示全部字段，或选择要显示的字段"
                      multiple
                      filterable
                      clearable
                      size="small"
                      max-tag-count="responsive"
                    />
                    <div class="field-tip">提示：拖拽调整字段顺序，留空则显示所有字段</div>
                  </div>
                  
                  <!-- 字段别名配置 -->
                  <div v-if="tableStyleConfig.displayColumns.length > 0" class="field-config-item">
                    <label>字段别名（可选）</label>
                    <div class="column-alias-list">
                      <div v-for="col in tableStyleConfig.displayColumns" :key="col" class="column-alias-item">
                        <n-tag size="small" :bordered="false" type="success">{{ col }}</n-tag>
                        <n-input
                          v-model:value="tableStyleConfig.columnLabels[col]"
                          :placeholder="col"
                          size="tiny"
                          style="width: 100px"
                        />
                      </div>
                    </div>
                  </div>
                  
                  <!-- 默认排序 -->
                  <div class="field-config-item">
                    <label>默认排序</label>
                    <n-space :size="8" style="width: 100%">
                      <n-select
                        v-model:value="tableStyleConfig.defaultSortField"
                        :options="tableStyleConfig.displayColumns.length > 0 
                          ? columnOptions.filter(c => tableStyleConfig.displayColumns.includes(c.value as string))
                          : columnOptions"
                        placeholder="选择排序字段"
                        clearable
                        filterable
                        size="small"
                        style="flex: 1"
                      />
                      <n-select
                        v-model:value="tableStyleConfig.defaultSortOrder"
                        :options="[
                          { label: '升序', value: 'ASC' },
                          { label: '降序', value: 'DESC' }
                        ]"
                        size="small"
                        style="width: 80px"
                        :disabled="!tableStyleConfig.defaultSortField"
                      />
                    </n-space>
                  </div>
                </template>
                
                <!-- 透视表类型：行列值字段配置 -->
                <template v-else-if="form.chartType === 'pivotTable'">
                  <div class="field-config-item">
                    <label>行字段（分组维度）</label>
                    <n-select
                      v-model:value="tableStyleConfig.pivotRowField"
                      :options="columnOptions"
                      placeholder="选择行字段"
                      filterable
                      clearable
                      size="small"
                    />
                  </div>
                  <div class="field-config-item">
                    <label>列字段（展开维度）</label>
                    <n-select
                      v-model:value="tableStyleConfig.pivotColField"
                      :options="columnOptions"
                      placeholder="选择列字段"
                      filterable
                      clearable
                      size="small"
                    />
                  </div>
                  <div class="field-config-item">
                    <label>值字段（数值）</label>
                    <n-select
                      v-model:value="tableStyleConfig.pivotValueField"
                      :options="numericColumnOptions"
                      placeholder="选择值字段"
                      filterable
                      clearable
                      size="small"
                    />
                  </div>
                  <div class="field-config-item">
                    <label>聚合方式</label>
                    <n-select
                      v-model:value="tableStyleConfig.pivotAggType"
                      :options="[
                        { label: '求和', value: 'sum' },
                        { label: '平均', value: 'avg' },
                        { label: '最大', value: 'max' },
                        { label: '最小', value: 'min' },
                        { label: '计数', value: 'count' }
                      ]"
                      size="small"
                    />
                  </div>
                </template>
                
                <!-- 其他图表类型：原有字段映射 -->
                <template v-else>
                  <div class="field-config-item">
                    <label>维度（X轴）</label>
                    <n-select
                      v-model:value="fieldMapping.xAxis"
                      :options="dimensionFields"
                      placeholder="选择维度字段"
                      clearable
                      filterable
                      size="small"
                      @update:value="handleFieldMappingChange"
                    />
                  </div>
                  <div class="field-config-item">
                    <label>指标（Y轴）</label>
                    <n-select
                      v-model:value="fieldMapping.yAxis"
                      :options="measureFields"
                      placeholder="选择指标字段"
                      multiple
                      filterable
                      size="small"
                      @update:value="handleYAxisChange"
                    />
                  </div>
                  <div v-if="fieldMapping.yAxis.length > 0" class="field-config-item">
                    <label>聚合方式 & 别名</label>
                    <div class="aggregate-list-vertical">
                      <div v-for="field in fieldMapping.yAxis" :key="field" class="aggregate-item-vertical">
                        <div class="aggregate-field-name">
                          <n-tag size="small" :bordered="false" type="success">{{ field }}</n-tag>
                        </div>
                        <div class="aggregate-controls">
                          <n-select
                            v-model:value="fieldAggregates[field]"
                            :options="aggregateFunctionOptions"
                            size="tiny"
                            style="flex: 1"
                            @update:value="handleAggregateChange(field)"
                          />
                          <n-input
                            v-model:value="yAxisLabels[field]"
                            placeholder="别名"
                            size="tiny"
                            style="flex: 1"
                          />
                        </div>
                      </div>
                    </div>
                </div>
                <div class="field-config-item">
                  <label>X轴排序</label>
                  <n-space :size="8">
                    <n-select
                      v-model:value="fieldMapping.sortField"
                      :options="[{ label: '按X轴字段', value: 'x' }, { label: '按Y轴数值', value: 'y' }]"
                      placeholder="排序字段"
                      size="small"
                      style="width: 120px"
                    />
                    <n-select
                      v-model:value="fieldMapping.sortOrder"
                      :options="[{ label: '升序', value: 'ASC' }, { label: '降序', value: 'DESC' }]"
                      placeholder="排序方式"
                      size="small"
                      style="width: 100px"
                    />
                  </n-space>
                </div>
                <div v-if="form.chartType === 'bar' || form.chartType === 'line'" class="field-config-item">
                  <label>分组（可选）</label>
                  <n-select
                    v-model:value="fieldMapping.group"
                    :options="dimensionFields"
                    placeholder="选择分组字段"
                    clearable
                    filterable
                    size="small"
                    @update:value="handleFieldMappingChange"
                  />
                </div>
                </template>
              </div>
            </n-collapse-item>

            <!-- 数据筛选 -->
            <n-collapse-item name="filter">
              <template #header>
                <div class="collapse-header">
                  <span>数据筛选</span>
                  <n-badge v-if="queryConditions.length > 0" :value="queryConditions.length" type="warning" />
                </div>
              </template>
              <div class="filter-content">
                <!-- 数据限制 -->
                <div class="field-config-item">
                  <label>
                    数据限制
                    <n-tag size="tiny" :bordered="false" style="margin-left: 8px;">{{ dataLimit }} 行</n-tag>
                  </label>
                  <n-slider v-model:value="dataLimit" :min="100" :max="10000" :step="100" />
                  <div class="limit-presets-compact">
                    <n-button size="tiny" quaternary @click="dataLimit = 100">100</n-button>
                    <n-button size="tiny" quaternary @click="dataLimit = 500">500</n-button>
                    <n-button size="tiny" quaternary @click="dataLimit = 1000">1K</n-button>
                    <n-button size="tiny" quaternary @click="dataLimit = 5000">5K</n-button>
                  </div>
                </div>
                <!-- 筛选条件 -->
                <div class="field-config-item">
                  <label>
                    筛选条件
                    <n-button size="tiny" quaternary type="primary" style="margin-left: auto;" @click="addQueryCondition">
                      <template #icon><n-icon size="12"><AddOutline /></n-icon></template>
                      添加
                    </n-button>
                  </label>
                  <div v-if="queryConditions.length > 0" class="filter-list-compact">
                    <div v-for="(condition, index) in queryConditions" :key="index" class="filter-item-compact">
                      <n-select
                        v-model:value="condition.field"
                        :options="columnOptions"
                        placeholder="字段"
                        size="tiny"
                        style="width: 80px"
                      />
                      <n-select
                        v-model:value="condition.operator"
                        :options="operatorOptions"
                        size="tiny"
                        style="width: 60px"
                      />
                      <n-input
                        v-if="condition.operator !== 'IS NULL' && condition.operator !== 'IS NOT NULL'"
                        v-model:value="condition.value"
                        placeholder="值"
                        size="tiny"
                        style="flex: 1; min-width: 50px;"
                      />
                      <n-button size="tiny" quaternary @click="removeQueryCondition(index)">
                        <template #icon><n-icon size="12"><CloseOutline /></n-icon></template>
                      </n-button>
                    </div>
                  </div>
                  <div v-else class="empty-tip">暂无筛选条件</div>
                </div>
              </div>
            </n-collapse-item>

            <!-- 查询参数 -->
            <n-collapse-item name="params">
              <template #header>
                <div class="collapse-header">
                  <span>查询参数</span>
                  <n-badge v-if="chartParameters.length > 0" :value="chartParameters.length" type="info" />
                </div>
              </template>
              <div class="params-content">
                <n-button size="small" quaternary type="primary" block style="margin-bottom: 8px;" @click="addChartParameter">
                  <template #icon><n-icon size="14"><AddOutline /></n-icon></template>
                  添加查询参数
                </n-button>
                <div v-if="chartParameters.length === 0" class="param-empty-tip-compact">
                  <n-icon size="20" color="#ccc"><FilterOutline /></n-icon>
                  <span>定义参数后，可在页面设计器中关联使用</span>
                </div>
                <div v-else class="param-list-compact">
                  <div v-for="(param, index) in chartParameters" :key="index" class="param-item-compact">
                    <div class="param-item-header" @click="expandedParamIndex = expandedParamIndex === index ? -1 : index">
                      <div class="param-item-summary">
                        <n-tag size="tiny" :type="getParamTagType(param.type)">{{ getParamTypeLabel(param.type) }}</n-tag>
                        <span class="param-item-label">{{ param.label || param.field || '未配置' }}</span>
                      </div>
                      <div class="param-item-actions">
                        <n-icon size="14" :style="{ transform: expandedParamIndex === index ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }">
                          <AddOutline />
                        </n-icon>
                        <n-button size="tiny" quaternary type="error" @click.stop="removeChartParameter(index)">
                          <template #icon><n-icon size="12"><CloseOutline /></n-icon></template>
                        </n-button>
                      </div>
                    </div>
                    <n-collapse-transition :show="expandedParamIndex === index">
                      <div class="param-item-detail">
                        <div class="param-field-row">
                          <label>字段</label>
                          <n-select
                            v-model:value="param.field"
                            :options="columnOptions"
                            placeholder="选择字段"
                            size="tiny"
                            filterable
                            @update:value="(v: string) => handleParamFieldChange(param, v)"
                          />
                        </div>
                        <div class="param-field-row">
                          <label>显示名称</label>
                          <n-input v-model:value="param.label" placeholder="输入显示名称" size="tiny" />
                        </div>
                        <div class="param-field-row">
                          <label>输入类型</label>
                          <n-select
                            v-model:value="param.type"
                            :options="getParamTypeOptionsForField(param.field)"
                            size="tiny"
                            @update:value="() => handleParamTypeChangeNew(param)"
                          />
                        </div>
                        <!-- 日期类型：快捷默认值 -->
                        <div v-if="param.type === 'date'" class="param-field-row">
                          <label>默认值</label>
                          <n-select
                            v-model:value="param.datePreset"
                            :options="datePresetOptions"
                            placeholder="选择日期快捷值"
                            size="tiny"
                            clearable
                            @update:value="(v: string) => handleDatePresetChange(param, v)"
                          />
                        </div>
                        <!-- 日期范围类型：快捷默认值 -->
                        <div v-if="param.type === 'dateRange'" class="param-field-row">
                          <label>默认值</label>
                          <n-select
                            v-model:value="param.datePreset"
                            :options="dateRangePresetOptions"
                            placeholder="选择日期范围快捷值"
                            size="tiny"
                            clearable
                            @update:value="(v: string) => handleDateRangePresetChange(param, v)"
                          />
                        </div>
                        <!-- 数字类型：默认值 -->
                        <div v-if="param.type === 'number'" class="param-field-row">
                          <label>默认值</label>
                          <n-input-number
                            :value="typeof param.defaultValue === 'number' ? param.defaultValue : null"
                            placeholder="默认值"
                            size="tiny"
                            style="width: 100%"
                            @update:value="(v: number | null) => param.defaultValue = v"
                          />
                        </div>
                        <!-- 文本类型：默认值 -->
                        <div v-if="param.type === 'text'" class="param-field-row">
                          <label>默认值</label>
                          <n-input
                            :value="typeof param.defaultValue === 'string' ? param.defaultValue : ''"
                            placeholder="默认值"
                            size="tiny"
                            @update:value="(v: string) => param.defaultValue = v || null"
                          />
                        </div>
                        <!-- 下拉选择类型：选项配置 -->
                        <template v-if="param.type === 'select' || param.type === 'multiSelect'">
                          <div class="param-field-row">
                            <label>选项来源</label>
                            <n-radio-group v-model:value="param.optionSource" size="small">
                              <n-radio-button value="manual">手动</n-radio-button>
                              <n-radio-button value="sql">SQL</n-radio-button>
                            </n-radio-group>
                          </div>
                          <div v-if="param.optionSource === 'manual'" class="param-field-row">
                            <label>选项</label>
                            <n-dynamic-tags
                              v-model:value="param.optionTags"
                              size="small"
                              @update:value="(tags: string[]) => updateParamOptions(param, tags)"
                            />
                          </div>
                          <div v-if="param.optionSource === 'sql'" class="param-field-row">
                            <label>SQL</label>
                            <n-input
                              v-model:value="param.optionSql"
                              type="textarea"
                              :rows="2"
                              placeholder="SELECT value, label FROM table"
                              size="tiny"
                            />
                          </div>
                        </template>
                      </div>
                    </n-collapse-transition>
                  </div>
                </div>
              </div>
            </n-collapse-item>
          </n-collapse>

          <!-- 字段模式：生成按钮（不显示SQL） -->
          <div v-if="form.dataSourceId && dataConfigMode === 'field' && selectedTable" class="field-mode-actions">
            <n-button 
              type="primary" 
              block 
              @click="handleGenerateSql"
            >
              <template #icon><n-icon size="14"><CreateOutline /></n-icon></template>
              生成图表
            </n-button>
          </div>
          
          <!-- SQL模式：完整SQL编辑区域 -->
          <div v-if="form.dataSourceId && dataConfigMode === 'sql'" class="sql-editor-section">
            <div class="sql-editor-header">
              <div class="sql-editor-title">
                <n-icon size="14" color="#60a5fa"><CodeOutline /></n-icon>
                <span>SQL 编辑器</span>
              </div>
              <n-space :size="6">
                <n-button size="tiny" quaternary @click="showQueryBuilder = true">
                  <template #icon><n-icon size="12"><GridOutline /></n-icon></template>
                  可视化构建
                </n-button>
                <n-button size="tiny" quaternary type="primary" @click="handleGenerateSql">
                  <template #icon><n-icon size="12"><PlayOutline /></n-icon></template>
                  执行
                </n-button>
              </n-space>
            </div>
            <n-input
              v-model:value="form.sqlContent"
              type="textarea"
              :rows="10"
              placeholder="SELECT column1, column2&#10;FROM table_name&#10;WHERE condition&#10;-- 支持 ${参数名} 占位符"
              style="font-family: 'Fira Code', 'Cascadia Code', Consolas, 'Courier New', monospace; font-size: 13px; letter-spacing: 0.3px;"
              @blur="handleSqlBlur"
            />
            
            <!-- SQL模式：字段映射配置 -->
            <n-collapse style="margin-top: 10px;" :default-expanded-names="['sql-field-mapping', 'sql-params']">
              <n-collapse-item name="sql-field-mapping">
                <template #header>
                  <div class="collapse-header">
                    <span>📊 字段映射</span>
                    <n-badge v-if="fieldMapping.xAxis || fieldMapping.yAxis.length > 0" dot type="success" />
                  </div>
                </template>
                <div class="sql-field-mapping-content">
                  <n-grid :cols="2" :x-gap="12" :y-gap="8">
                    <n-gi>
                      <div class="sql-field-item">
                        <label class="sql-field-label">维度（X轴）</label>
                        <n-select
                          v-model:value="fieldMapping.xAxis"
                          :options="sqlFieldOptions"
                          placeholder="选择或输入字段"
                          clearable
                          filterable
                          tag
                          size="small"
                        />
                      </div>
                    </n-gi>
                    <n-gi>
                      <div class="sql-field-item">
                        <label class="sql-field-label">指标（Y轴）</label>
                        <n-select
                          v-model:value="fieldMapping.yAxis"
                          :options="sqlFieldOptions"
                          placeholder="选择或输入字段"
                          multiple
                          filterable
                          tag
                          size="small"
                        />
                      </div>
                    </n-gi>
                  </n-grid>
                  <n-text depth="3" style="font-size: 11px; margin-top: 6px; display: block;">
                    💡 执行SQL后会自动解析字段，也可手动输入SQL中的字段名或别名
                  </n-text>
                </div>
              </n-collapse-item>
              
              <!-- SQL模式：查询参数配置 - 优化版 -->
              <n-collapse-item name="sql-params">
                <template #header>
                  <div class="collapse-header">
                    <span>🔧 查询参数</span>
                    <n-badge :value="chartParameters.length" :max="99" type="info" />
                  </div>
                </template>
                <div class="sql-params-content-new">
                  <div v-if="chartParameters.length === 0" class="sql-params-empty-new">
                    <n-icon size="32" color="#cbd5e1"><FilterOutline /></n-icon>
                    <n-text depth="3">暂无查询参数</n-text>
                    <n-text depth="3" style="font-size: 12px;">在SQL中使用 ${'${'}参数名${'}'} 引用参数</n-text>
                  </div>
                  
                  <div v-for="(param, idx) in chartParameters" :key="idx" class="sql-param-card-new">
                    <div class="sql-param-header-new">
                      <div class="sql-param-title">
                        <n-tag size="small" type="info" :bordered="false">#{{ idx + 1 }}</n-tag>
                        <span class="sql-param-name">{{ param.name || '未命名参数' }}</span>
                        <n-tag v-if="param.required" size="tiny" type="error" :bordered="false">必填</n-tag>
                      </div>
                      <n-button text type="error" size="small" @click="chartParameters.splice(idx, 1)">
                        <n-icon size="16"><CloseOutline /></n-icon>
                      </n-button>
                    </div>
                    <div class="sql-param-body-new">
                      <n-grid :cols="2" :x-gap="12" :y-gap="12">
                        <n-gi>
                          <div class="sql-param-field-new">
                            <label>参数名 <span class="required-mark">*</span></label>
                            <n-input 
                              v-model:value="param.name" 
                              placeholder="如: deptId" 
                              size="small"
                              @update:value="(v: string) => { param.field = v; if (!param.label) param.label = v }"
                            />
                          </div>
                        </n-gi>
                        <n-gi>
                          <div class="sql-param-field-new">
                            <label>显示名称</label>
                            <n-input v-model:value="param.label" placeholder="如: 机构" size="small" />
                          </div>
                        </n-gi>
                        <n-gi>
                          <div class="sql-param-field-new">
                            <label>输入类型</label>
                            <n-select 
                              v-model:value="param.type" 
                              size="small"
                              :options="[
                                {label:'📝 文本输入',value:'text'},
                                {label:'🔢 数字输入',value:'number'},
                                {label:'📅 日期选择',value:'date'},
                                {label:'📅 日期范围',value:'dateRange'},
                                {label:'📋 下拉单选',value:'select'},
                                {label:'📋 下拉多选',value:'multiSelect'}
                              ]"
                            />
                          </div>
                        </n-gi>
                        <n-gi>
                          <div class="sql-param-field-new">
                            <label>默认值</label>
                            <n-input :value="String(param.defaultValue ?? '')" placeholder="可选" size="small" @update:value="(v: string) => param.defaultValue = v" />
                          </div>
                        </n-gi>
                        <n-gi :span="2">
                          <div class="sql-param-field-new">
                            <n-checkbox v-model:checked="param.required" size="small">设为必填参数</n-checkbox>
                          </div>
                        </n-gi>
                      </n-grid>
                    </div>
                  </div>
                  
                  <n-button 
                    dashed 
                    block 
                    size="small"
                    style="margin-top: 12px;"
                    @click="addSqlParameter"
                  >
                    <template #icon><n-icon><AddOutline /></n-icon></template>
                    添加查询参数
                  </n-button>
                </div>
              </n-collapse-item>
            </n-collapse>
            
            <!-- SQL模式的执行按钮 -->
            <n-button 
              type="primary" 
              block 
              :disabled="!form.sqlContent"
              :loading="previewLoading"
              style="margin-top: 10px;"
              @click="handlePreviewChart"
            >
              <template #icon><n-icon size="14"><PlayOutline /></n-icon></template>
              执行SQL并预览
            </n-button>
          </div>
        </n-card>
      </div>

      <!-- 中间：图表预览和配置 -->
      <div class="designer-center-panel">
        <n-card size="small" class="panel-card" :bordered="false">
          <template #header>
            <div class="preview-card-header">
              <div class="preview-title-area">
                <div class="preview-icon">
                  <n-icon size="14"><BarChartOutline /></n-icon>
                </div>
                <span class="preview-title">{{ form.chartName || '图表预览' }}</span>
              </div>
              <div class="preview-actions">
                <n-dropdown :options="previewMenuOptions" placement="bottom-end" @select="handlePreviewMenu">
                  <n-button size="small" quaternary>
                    <template #icon><n-icon><EllipsisVerticalOutline /></n-icon></template>
                  </n-button>
                </n-dropdown>
                <!-- 表格类型：导出按钮 -->
                <n-button 
                  v-if="['table', 'summaryTable', 'pivotTable'].includes(form.chartType || '') && tableStyleConfig.enableExport && previewData.length > 0" 
                  size="small" 
                  quaternary
                  @click="handleExportTableData"
                >
                  <template #icon><n-icon><DownloadOutline /></n-icon></template>
                  导出Excel
                </n-button>
              </div>
            </div>
          </template>
          
          <div class="chart-preview-area">
            <!-- 表格类型：显示数据表格 -->
            <template v-if="form.chartType === 'table'">
              <div class="table-preview-wrapper custom-header-table" :style="tablePreviewWrapperStyle">
                <n-data-table
                  :columns="tablePreviewColumns"
                  :data="previewData"
                  :bordered="true"
                  :single-line="false"
                  :striped="tableStyleConfig.striped"
                  :size="tableStyleConfig.size"
                  :max-height="tablePreviewMaxHeight"
                  :scroll-x="tablePreviewColumns.length > 5 ? 1000 : undefined"
                  :pagination="tablePreviewPagination"
                  :loading="previewLoading"
                >
                  <template #empty>
                    <n-empty description="点击「生成图表」按钮加载数据" size="small" />
                  </template>
                </n-data-table>
              </div>
            </template>
            <!-- 汇总表类型：显示带汇总行的表格 -->
            <template v-else-if="form.chartType === 'summaryTable'">
              <div class="table-preview-wrapper custom-header-table" :style="tablePreviewWrapperStyle">
                <n-data-table
                  :columns="tablePreviewColumns"
                  :data="previewData"
                  :bordered="true"
                  :single-line="false"
                  :striped="tableStyleConfig.striped"
                  :size="tableStyleConfig.size"
                  :max-height="summaryTablePreviewMaxHeight"
                  :scroll-x="tablePreviewColumns.length > 5 ? 1000 : undefined"
                  :pagination="tablePreviewPagination"
                  :loading="previewLoading"
                  :summary="tableStyleConfig.showSummary ? renderSummary : undefined"
                >
                  <template #empty>
                    <n-empty description="点击「生成图表」按钮加载数据" size="small" />
                  </template>
                </n-data-table>
              </div>
            </template>
            <!-- 透视表类型 -->
            <template v-else-if="form.chartType === 'pivotTable'">
              <div class="table-preview-wrapper custom-header-table" :style="tablePreviewWrapperStyle">
                <n-data-table
                  :columns="pivotTableData.columns"
                  :data="pivotTableData.data"
                  :bordered="true"
                  :single-line="false"
                  :striped="tableStyleConfig.striped"
                  :size="tableStyleConfig.size"
                  :max-height="tablePreviewMaxHeight"
                  :scroll-x="pivotTableData.columns.length > 5 ? 1000 : undefined"
                  :loading="previewLoading"
                >
                  <template #empty>
                    <n-empty description="请配置行字段、列字段和值字段后生成透视表" size="small" />
                  </template>
                </n-data-table>
              </div>
            </template>
            <!-- 其他图表类型：显示 ECharts -->
            <template v-else>
              <div
                ref="chartPreviewRef"
                class="chart-preview"
                :style="chartPreviewStyle"
              ></div>
            </template>
            <div v-if="previewData.length === 0 && !previewLoading && !['table', 'summaryTable', 'pivotTable'].includes(form.chartType || '')" class="preview-placeholder">
              <n-empty description="请配置数据源和字段映射后预览图表" />
            </div>
          </div>

          <!-- 数据预览表格 -->
          <div class="data-preview-section">
            <div class="data-preview-header" @click="showDataPreview = !showDataPreview">
              <div class="data-preview-title">
                <n-icon size="14"><CodeOutline /></n-icon>
                <span>数据预览</span>
                <n-tag v-if="previewData.length > 0" size="small" :bordered="false" round type="info">
                  {{ previewData.length }} 条
                </n-tag>
              </div>
              <div class="data-preview-actions">
                <n-button
                  size="tiny"
                  text
                  :disabled="previewData.length === 0"
                  @click.stop="handleExportPreviewData"
                >
                  <template #icon><n-icon size="13"><DownloadOutline /></n-icon></template>
                  导出
                </n-button>
                <n-icon size="14" :style="{ transform: showDataPreview ? 'rotate(0)' : 'rotate(180deg)', transition: 'transform 0.2s' }">
                  <ArrowBackOutline style="transform: rotate(-90deg)" />
                </n-icon>
              </div>
            </div>
            <n-collapse-transition :show="showDataPreview">
              <div class="data-preview-body">
                <n-data-table
                  :columns="dataPreviewColumns"
                  :data="previewData"
                  size="small"
                  :max-height="250"
                  :loading="previewLoading"
                  :bordered="false"
                  :scroll-x="dataPreviewColumns.length > 5 ? 800 : undefined"
                  :pagination="false"
                >
                  <template #empty>
                    <n-empty description="暂无数据" size="small" />
                  </template>
                </n-data-table>
              </div>
            </n-collapse-transition>
          </div>
          
          <!-- 🆕 预览状态栏 - 显示实时预览状态 (需求 3.1) -->
          <PreviewStatusBar
            :is-updating="previewState.isUpdating.value"
            :is-ready="previewState.isReady.value"
            :update-latency="previewState.updateLatency.value"
            :last-update-time="previewState.lastUpdateTime.value"
            :show-latency="true"
          />
        </n-card>
      </div>

      <!-- 右侧：样式和属性配置 -->
      <div class="designer-right-panel">
        <n-card title="样式配置" size="small" class="panel-card">
          <n-tabs type="segment" size="small">
            <!-- 基础配置 -->
            <n-tab-pane name="basic" tab="基础">
              <n-space vertical :size="12" style="margin-top: 12px">
                <n-form-item>
                  <template #label>
                    <span class="form-label-with-tooltip">
                      图表标题
                      <ConfigTooltip :content="configTooltips.chartTitle" />
                    </span>
                  </template>
                  <n-input v-model:value="styleConfig.title" placeholder="图表标题" @update:value="handleStyleChange" />
                </n-form-item>
                <n-form-item>
                  <template #label>
                    <span class="form-label-with-tooltip">
                      标题位置
                      <ConfigTooltip :content="configTooltips.titlePosition" />
                    </span>
                  </template>
                  <n-select
                    v-model:value="styleConfig.titlePosition"
                    :options="titlePositionOptions"
                    @update:value="handleStyleChange"
                  />
                </n-form-item>
                <n-form-item>
                  <template #label>
                    <span class="form-label-with-tooltip">
                      图表描述
                      <ConfigTooltip :content="configTooltips.chartDescription" />
                    </span>
                  </template>
                  <n-input
                    v-model:value="form.description"
                    type="textarea"
                    :rows="2"
                    placeholder="图表描述"
                  />
                </n-form-item>
              </n-space>
            </n-tab-pane>

            <!-- 颜色配置 -->
            <n-tab-pane name="color" tab="颜色">
              <n-space vertical :size="12" style="margin-top: 12px">
                <!-- 🆕 推荐配色方案 -->
                <n-form-item>
                  <template #label>
                    <span class="form-label-with-tooltip">
                      推荐搭配
                      <ConfigTooltip :content="configTooltips.colorPresets" />
                    </span>
                  </template>
                  <div class="color-presets">
                    <div 
                      v-for="preset in colorPresets" 
                      :key="preset.name"
                      class="color-preset-item"
                      :class="{ active: styleConfig.colorScheme === preset.scheme && styleConfig.backgroundColor === preset.background }"
                      @click="applyColorPreset(preset)"
                    >
                      <div class="preset-preview" :style="{ backgroundColor: preset.background }">
                        <div class="preset-colors">
                          <span 
                            v-for="(color, idx) in preset.previewColors" 
                            :key="idx" 
                            class="preset-color-dot"
                            :style="{ backgroundColor: color }"
                          ></span>
                        </div>
                      </div>
                      <span class="preset-name">{{ preset.label }}</span>
                    </div>
                  </div>
                </n-form-item>
                
                <n-divider style="margin: 8px 0;" />
                
                <n-form-item>
                  <template #label>
                    <span class="form-label-with-tooltip">
                      颜色方案
                      <ConfigTooltip :content="configTooltips.colorScheme" />
                    </span>
                  </template>
                  <n-select
                    v-model:value="styleConfig.colorScheme"
                    :options="colorSchemeOptions"
                    :render-label="renderColorSchemeLabel"
                    @update:value="handleColorSchemeChange"
                  />
                  <!-- 🆕 颜色方案预览 -->
                  <div v-if="styleConfig.colorScheme !== 'custom'" class="color-scheme-preview">
                    <span 
                      v-for="(color, idx) in getCurrentSchemeColors()" 
                      :key="idx" 
                      class="scheme-color-block"
                      :style="{ backgroundColor: color }"
                    ></span>
                  </div>
                </n-form-item>
                <n-form-item v-if="styleConfig.colorScheme === 'custom'">
                  <template #label>
                    <span class="form-label-with-tooltip">
                      主题颜色
                      <ConfigTooltip :content="configTooltips.customColor" />
                    </span>
                  </template>
                  <n-color-picker
                    v-model:value="styleConfig.color"
                    :modes="['hex']"
                    :swatches="colorSwatches"
                    @update:value="handleColorChange"
                  />
                  <template #feedback>
                    <div style="font-size: 12px; color: #999; margin-top: 4px;">
                      自定义颜色将作为第一个系列的颜色，其他系列使用默认配色
                    </div>
                  </template>
                </n-form-item>
                
                <n-form-item label="背景颜色">
                  <n-color-picker
                    v-model:value="styleConfig.backgroundColor"
                    :modes="['hex']"
                    :swatches="[
                      '#ffffff', '#f5f5f5', '#fafafa', '#ecf0f1',
                      '#2c3e50', '#34495e', '#1a1a1a', '#000000'
                    ]"
                    @update:value="handleStyleChange"
                  />
                  <template #feedback>
                    <div style="font-size: 12px; color: #999; margin-top: 4px;">
                      浅色背景适合打印，深色背景适合演示。默认白色
                    </div>
                  </template>
                </n-form-item>
                <n-alert v-if="styleConfig.colorScheme !== 'custom'" type="info" :bordered="false" style="margin-top: 8px;">
                  当前使用 {{ colorSchemeOptions.find(opt => opt.value === styleConfig.colorScheme)?.label || '默认' }} 颜色方案
                </n-alert>
              </n-space>
            </n-tab-pane>

            <!-- 图表配置 -->
            <n-tab-pane name="chart" tab="图表">
              <n-space vertical :size="12" style="margin-top: 12px">
                <n-form-item label="显示图例">
                  <n-switch v-model:value="styleConfig.showLegend" @update:value="handleStyleChange" />
                </n-form-item>
                <n-form-item v-if="styleConfig.showLegend" label="图例位置">
                  <n-select
                    v-model:value="styleConfig.legendPosition"
                    :options="legendPositionOptions"
                    @update:value="handleStyleChange"
                  />
                </n-form-item>
                <n-form-item label="显示网格">
                  <n-switch v-model:value="styleConfig.showGrid" @update:value="handleStyleChange" />
                </n-form-item>
                <n-form-item label="显示标签">
                  <n-switch v-model:value="styleConfig.showLabel" @update:value="handleStyleChange" />
                </n-form-item>
                
                <!-- 折线图特殊配置 -->
                <template v-if="form.chartType === 'line'">
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">折线图配置</n-text>
                  <n-form-item label="平滑曲线">
                    <n-switch v-model:value="styleConfig.smooth" @update:value="handleStyleChange" />
                  </n-form-item>
                  <n-form-item label="显示数据点">
                    <n-switch v-model:value="styleConfig.showSymbol" @update:value="handleStyleChange" />
                  </n-form-item>
                  <n-form-item v-if="styleConfig.showSymbol" label="数据点大小">
                    <n-input-number
                      v-model:value="styleConfig.symbolSize"
                      :min="2"
                      :max="20"
                      style="width: 100%"
                      @update:value="handleStyleChange"
                    />
                  </n-form-item>
                </template>
                
                <!-- 柱状图特殊配置 -->
                <template v-if="form.chartType === 'bar'">
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">柱状图配置</n-text>
                  <n-form-item label="柱宽度">
                    <n-input
                      :value="String(styleConfig.barWidth)"
                      placeholder="auto 或数字（像素）"
                      @update:value="(v: string) => { styleConfig.barWidth = v; handleStyleChange() }"
                    />
                  </n-form-item>
                  <n-form-item label="最大柱宽度">
                    <n-input-number
                      v-model:value="styleConfig.barMaxWidth"
                      :min="0"
                      placeholder="留空为无限制"
                      style="width: 100%"
                      @update:value="handleStyleChange"
                    />
                  </n-form-item>
                </template>
                
                <!-- 表格特殊配置 -->
                <template v-if="form.chartType === 'table' || form.chartType === 'summaryTable'">
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">表格样式</n-text>
                  
                  <n-form-item label="斑马纹">
                    <n-switch v-model:value="tableStyleConfig.striped" />
                  </n-form-item>
                  <n-form-item label="显示序号">
                    <n-switch v-model:value="tableStyleConfig.showIndex" />
                  </n-form-item>
                  <n-form-item label="表格尺寸">
                    <n-select
                      v-model:value="tableStyleConfig.size"
                      :options="[
                        { label: '小', value: 'small' },
                        { label: '中', value: 'medium' },
                        { label: '大', value: 'large' }
                      ]"
                    />
                  </n-form-item>
                  <n-form-item label="显示分页">
                    <n-switch v-model:value="tableStyleConfig.showPagination" />
                  </n-form-item>
                  <n-form-item v-if="tableStyleConfig.showPagination" label="每页条数">
                    <n-select
                      v-model:value="tableStyleConfig.pageSize"
                      :options="[
                        { label: '10条/页', value: 10 },
                        { label: '20条/页', value: 20 },
                        { label: '50条/页', value: 50 },
                        { label: '100条/页', value: 100 }
                      ]"
                    />
                  </n-form-item>
                  
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">表头样式</n-text>
                  <n-form-item label="表头背景色">
                    <n-color-picker 
                      v-model:value="tableStyleConfig.headerBgColor" 
                      :swatches="['#f5f7fa', '#e6f7ff', '#f6ffed', '#fff7e6', '#fff1f0', '#f0f5ff', '#f9f0ff']"
                      :modes="['hex']"
                      :show-alpha="false"
                    />
                  </n-form-item>
                  <n-form-item label="表头文字色">
                    <n-color-picker 
                      v-model:value="tableStyleConfig.headerTextColor"
                      :swatches="['#303133', '#1890ff', '#52c41a', '#fa8c16', '#f5222d', '#722ed1', '#13c2c2']"
                      :modes="['hex']"
                      :show-alpha="false"
                    />
                  </n-form-item>
                  <n-form-item label="表头字体">
                    <n-select
                      v-model:value="tableStyleConfig.headerFontWeight"
                      :options="[
                        { label: '正常', value: 'normal' },
                        { label: '加粗', value: 'bold' }
                      ]"
                    />
                  </n-form-item>
                  <n-form-item label="表头对齐">
                    <n-select
                      v-model:value="tableStyleConfig.headerAlign"
                      :options="[
                        { label: '左对齐', value: 'left' },
                        { label: '居中', value: 'center' },
                        { label: '右对齐', value: 'right' }
                      ]"
                    />
                  </n-form-item>
                  
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">导出配置</n-text>
                  <n-form-item label="允许导出">
                    <n-switch v-model:value="tableStyleConfig.enableExport" />
                  </n-form-item>
                  <n-form-item v-if="tableStyleConfig.enableExport" label="导出文件名">
                    <n-input
                      v-model:value="tableStyleConfig.exportFileName"
                      placeholder="留空使用图表名称"
                    />
                  </n-form-item>
                  
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">条件格式化</n-text>
                  <n-form-item label="启用条件格式">
                    <n-switch v-model:value="tableStyleConfig.enableConditionalFormat" />
                  </n-form-item>
                  <template v-if="tableStyleConfig.enableConditionalFormat">
                    <n-form-item label="格式化字段">
                      <n-select
                        v-model:value="tableStyleConfig.conditionalFormatField"
                        :options="numericColumnOptions"
                        placeholder="选择数值字段"
                        filterable
                        clearable
                      />
                    </n-form-item>
                    <n-form-item label="格式化类型">
                      <n-select
                        v-model:value="tableStyleConfig.conditionalFormatType"
                        :options="[
                          { label: '颜色渐变', value: 'colorScale' },
                          { label: '数据条', value: 'dataBar' }
                        ]"
                      />
                    </n-form-item>
                    <n-form-item v-if="tableStyleConfig.conditionalFormatType === 'colorScale'" label="颜色设置">
                      <n-space :size="8">
                        <n-color-picker 
                          v-model:value="tableStyleConfig.conditionalFormatColors[0]" 
                          :swatches="['#f5222d', '#fa541c', '#fa8c16']"
                          :modes="['hex']"
                          :show-alpha="false"
                          size="small"
                        />
                        <n-color-picker 
                          v-model:value="tableStyleConfig.conditionalFormatColors[1]" 
                          :swatches="['#faad14', '#fadb14', '#a0d911']"
                          :modes="['hex']"
                          :show-alpha="false"
                          size="small"
                        />
                        <n-color-picker 
                          v-model:value="tableStyleConfig.conditionalFormatColors[2]" 
                          :swatches="['#52c41a', '#13c2c2', '#1890ff']"
                          :modes="['hex']"
                          :show-alpha="false"
                          size="small"
                        />
                      </n-space>
                      <div class="field-tip">低值 → 中值 → 高值</div>
                    </n-form-item>
                  </template>
                </template>
                
                <!-- 汇总表特殊配置 -->
                <template v-if="form.chartType === 'summaryTable'">
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">汇总配置</n-text>
                  <n-form-item label="显示汇总行">
                    <n-switch v-model:value="tableStyleConfig.showSummary" />
                  </n-form-item>
                  <template v-if="tableStyleConfig.showSummary">
                    <n-form-item label="汇总行标签">
                      <n-input v-model:value="tableStyleConfig.summaryLabel" placeholder="合计" />
                    </n-form-item>
                    <n-form-item label="汇总字段">
                      <n-select
                        v-model:value="tableStyleConfig.summaryColumns"
                        :options="numericColumnOptions"
                        placeholder="选择需要汇总的数值字段"
                        multiple
                        filterable
                      />
                    </n-form-item>
                    <div v-if="tableStyleConfig.summaryColumns.length > 0" class="summary-type-list">
                      <div v-for="col in tableStyleConfig.summaryColumns" :key="col" class="summary-type-item">
                        <n-tag size="small" :bordered="false" type="info">{{ col }}</n-tag>
                        <n-select
                          v-model:value="tableStyleConfig.summaryTypes[col]"
                          :options="[
                            { label: '求和', value: 'sum' },
                            { label: '平均', value: 'avg' },
                            { label: '最大', value: 'max' },
                            { label: '最小', value: 'min' },
                            { label: '计数', value: 'count' }
                          ]"
                          size="tiny"
                          style="width: 80px"
                          :default-value="'sum'"
                        />
                      </div>
                    </div>
                  </template>
                </template>
                
                <!-- 透视表特殊配置 -->
                <template v-if="form.chartType === 'pivotTable'">
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">透视表配置</n-text>
                  <n-form-item label="行字段">
                    <n-select
                      v-model:value="tableStyleConfig.pivotRowField"
                      :options="columnOptions"
                      placeholder="选择行字段（分组维度）"
                      filterable
                      clearable
                    />
                  </n-form-item>
                  <n-form-item label="列字段">
                    <n-select
                      v-model:value="tableStyleConfig.pivotColField"
                      :options="columnOptions"
                      placeholder="选择列字段（展开维度）"
                      filterable
                      clearable
                    />
                  </n-form-item>
                  <n-form-item label="值字段">
                    <n-select
                      v-model:value="tableStyleConfig.pivotValueField"
                      :options="numericColumnOptions"
                      placeholder="选择值字段（数值）"
                      filterable
                      clearable
                    />
                  </n-form-item>
                  <n-form-item label="聚合方式">
                    <n-select
                      v-model:value="tableStyleConfig.pivotAggType"
                      :options="[
                        { label: '求和', value: 'sum' },
                        { label: '平均', value: 'avg' },
                        { label: '最大', value: 'max' },
                        { label: '最小', value: 'min' },
                        { label: '计数', value: 'count' }
                      ]"
                    />
                  </n-form-item>
                  <n-form-item label="显示行合计">
                    <n-switch v-model:value="tableStyleConfig.pivotShowRowTotal" />
                  </n-form-item>
                  <n-form-item label="显示列合计">
                    <n-switch v-model:value="tableStyleConfig.pivotShowColTotal" />
                  </n-form-item>
                  
                  <n-divider style="margin: 8px 0;" />
                  <n-text depth="3" style="font-size: 12px;">表头样式</n-text>
                  <n-form-item label="表头背景色">
                    <n-color-picker 
                      v-model:value="tableStyleConfig.headerBgColor" 
                      :swatches="['#f5f7fa', '#e6f7ff', '#f6ffed', '#fff7e6', '#fff1f0', '#f0f5ff', '#f9f0ff']"
                      :modes="['hex']"
                      :show-alpha="false"
                    />
                  </n-form-item>
                  <n-form-item label="表头文字色">
                    <n-color-picker 
                      v-model:value="tableStyleConfig.headerTextColor"
                      :swatches="['#303133', '#1890ff', '#52c41a', '#fa8c16', '#f5222d', '#722ed1', '#13c2c2']"
                      :modes="['hex']"
                      :show-alpha="false"
                    />
                  </n-form-item>
                </template>
                
                <n-divider style="margin: 8px 0;" />
                <n-form-item label="图表宽度 (px)">
                  <n-input-number
                    v-model:value="styleConfig.width"
                    :min="200"
                    :max="4000"
                    :step="10"
                    clearable
                    placeholder="自适应"
                    style="width: 100%"
                    @update:value="handleStyleChange"
                  />
                </n-form-item>
                <n-form-item label="图表高度 (px)">
                  <n-input-number
                    v-model:value="styleConfig.height"
                    :min="200"
                    :max="3000"
                    :step="10"
                    clearable
                    placeholder="默认 400"
                    style="width: 100%"
                    @update:value="handleStyleChange"
                  />
                </n-form-item>
                <n-form-item label="动画效果">
                  <n-switch v-model:value="styleConfig.animation" @update:value="handleStyleChange" />
                </n-form-item>
                <n-form-item v-if="styleConfig.animation" label="动画时长（ms）">
                  <n-input-number
                    v-model:value="styleConfig.animationDuration"
                    :min="0"
                    :max="5000"
                    style="width: 100%"
                    @update:value="handleStyleChange"
                  />
                </n-form-item>
              </n-space>
            </n-tab-pane>

            <!-- 交互配置 -->
            <n-tab-pane name="interaction" tab="交互">
              <n-space vertical :size="12" style="margin-top: 12px">
                <n-form-item label="启用缩放">
                  <n-switch v-model:value="interactionConfig.enableZoom" />
                </n-form-item>
                <n-form-item v-if="interactionConfig.enableZoom" label="启用数据缩放">
                  <n-switch v-model:value="interactionConfig.enableDataZoom" />
                </n-form-item>
                <n-form-item label="启用图例筛选">
                  <n-switch v-model:value="interactionConfig.enableLegendSelect" />
                </n-form-item>
                <n-form-item label="启用工具提示">
                  <n-switch v-model:value="interactionConfig.enableTooltip" />
                </n-form-item>
                <n-form-item label="启用工具箱">
                  <n-switch v-model:value="interactionConfig.enableToolbox" />
                </n-form-item>
                <n-form-item v-if="interactionConfig.enableToolbox" label="工具箱功能">
                  <n-checkbox-group v-model:value="interactionConfig.toolboxFeatures">
                    <n-space vertical>
                      <n-checkbox value="saveAsImage">保存图片</n-checkbox>
                      <n-checkbox value="dataView">数据视图</n-checkbox>
                      <n-checkbox value="dataZoom">数据缩放</n-checkbox>
                      <n-checkbox value="restore">还原</n-checkbox>
                    </n-space>
                  </n-checkbox-group>
                </n-form-item>
                <n-form-item label="启用点击事件">
                  <n-switch v-model:value="interactionConfig.enableClick" />
                </n-form-item>
                <n-form-item label="启用悬停高亮">
                  <n-switch v-model:value="interactionConfig.enableHover" />
                </n-form-item>
              </n-space>
            </n-tab-pane>

            <!-- 图表专属配置 -->
            <n-tab-pane 
              v-if="hasChartSpecificConfig" 
              name="chartSpecific" 
              tab="专属"
            >
              <n-space vertical :size="12" style="margin-top: 12px">
                <!-- 地图图表配置 -->
                <MapChartConfigPanel
                  v-if="form.chartType === 'map' || form.chartType === 'chinaMap' || form.chartType === 'worldMap'"
                  :model-value="mapChartConfig"
                  :field-options="columnOptions"
                  @update:model-value="(v: any) => { Object.assign(mapChartConfig, v); handleChartSpecificConfigChange() }"
                />
                
                <!-- KPI 卡片配置 -->
                <KpiChartConfigPanel
                  v-if="form.chartType === 'kpi'"
                  :model-value="kpiChartConfig"
                  :field-options="columnOptions"
                  @update:model-value="(v: any) => { Object.assign(kpiChartConfig, v); handleChartSpecificConfigChange() }"
                />
                
                <!-- 瀑布图配置 -->
                <WaterfallChartConfigPanel
                  v-if="form.chartType === 'waterfall'"
                  :model-value="waterfallChartConfig"
                  :field-options="columnOptions"
                  @update:model-value="(v: any) => { Object.assign(waterfallChartConfig, v); handleChartSpecificConfigChange() }"
                />
                
                <!-- 词云图配置 -->
                <WordCloudChartConfigPanel
                  v-if="form.chartType === 'wordCloud'"
                  :model-value="wordCloudChartConfig"
                  :field-options="columnOptions"
                  @update:model-value="(v: any) => { Object.assign(wordCloudChartConfig, v); handleChartSpecificConfigChange() }"
                />
                
                <!-- 组合图配置 -->
                <ComboChartConfigPanel
                  v-if="form.chartType === 'combo'"
                  :model-value="comboChartConfig"
                  :field-options="columnOptions"
                  @update:model-value="(v: any) => { Object.assign(comboChartConfig, v); handleChartSpecificConfigChange() }"
                />
              </n-space>
            </n-tab-pane>

            <!-- 高级配置 -->
            <n-tab-pane name="advanced" tab="高级">
              <n-space vertical :size="12" style="margin-top: 12px">
                <n-form-item label="ECharts配置（JSON）">
                  <n-input
                    v-model:value="chartConfigJson"
                    type="textarea"
                    :rows="10"
                    placeholder="高级ECharts配置JSON"
                    @update:value="handleChartConfigChange"
                  />
                </n-form-item>
                <n-button size="small" @click="handleLoadDefaultConfig">加载默认配置</n-button>
              </n-space>
            </n-tab-pane>
          </n-tabs>
        </n-card>
      </div>
    </div>

    <!-- 模板选择模态框 -->
    <n-modal v-model:show="showTemplateModal" preset="card" title="选择图表模板" style="width: 800px; max-width: 90vw;">
      <div class="template-modal-content">
        <n-tabs type="line" animated>
          <n-tab-pane v-for="category in templateCategories" :key="category" :name="category" :tab="category">
            <div class="template-grid">
              <div 
                v-for="template in getTemplatesByCategory(category)" 
                :key="template.id"
                class="template-card"
                :class="{ selected: selectedTemplateId === template.id }"
                @click="selectedTemplateId = template.id"
              >
                <div class="template-preview" :style="{ background: getTemplatePreviewBg(template.chartType) }">
                  <n-icon size="32" color="rgba(255,255,255,0.9)">
                    <component :is="getChartIcon(template.chartType)" />
                  </n-icon>
                </div>
                <div class="template-info">
                  <div class="template-name">{{ template.name }}</div>
                  <div class="template-desc">{{ template.description }}</div>
                </div>
              </div>
            </div>
          </n-tab-pane>
        </n-tabs>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTemplateModal = false">取消</n-button>
          <n-button type="primary" :disabled="!selectedTemplateId" @click="handleApplyTemplate">
            应用模板
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 可视化查询构建器抽屉 -->
    <n-drawer v-model:show="showQueryBuilder" :width="680" placement="right">
      <n-drawer-content title="可视化查询构建器" closable>
        <template #header-extra>
          <n-button size="small" type="primary" @click="applyBuilderSql">应用到SQL</n-button>
        </template>
        <n-space vertical :size="12">
          <n-form-item label="主表" label-placement="left">
            <n-select v-model:value="builderMainTable" :options="tableOptions" placeholder="选择主表" filterable clearable @update:value="loadBuilderColumns" />
          </n-form-item>
          <n-form-item label="查询字段" label-placement="left">
            <n-select v-model:value="builderFields" :options="builderColumnOptions" placeholder="选择字段（留空=全选）" multiple filterable clearable />
          </n-form-item>
          <n-form-item label="WHERE 条件" label-placement="left">
            <n-input v-model:value="builderWhere" type="textarea" :rows="3" placeholder="例: status = 1 AND create_time > '2024-01-01'" />
          </n-form-item>
          <n-form-item label="ORDER BY" label-placement="left">
            <n-input v-model:value="builderOrderBy" placeholder="例: id DESC" />
          </n-form-item>
          <n-form-item label="LIMIT" label-placement="left">
            <n-input-number v-model:value="builderLimit" :min="1" :max="100000" style="width:150px" />
          </n-form-item>
          <n-card size="small" title="生成的 SQL">
            <n-code :code="builderPreviewSql" language="sql" word-wrap />
          </n-card>
        </n-space>
      </n-drawer-content>
    </n-drawer>

  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount, watch, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NSelect,
  NInput,
  NInputNumber,
  NCard,
  NSpace,
  NText,
  NTag,
  NFormItem,
  NDivider,
  NTabs,
  NTabPane,
  NColorPicker,
  NSpin,
  NSwitch,
  NDataTable,
  NEmpty,
  NCheckboxGroup,
  NCheckbox,
  NAlert,
  NIcon,
  NSlider,
  NDropdown,
  NModal,
  NDynamicTags,
  NRadioGroup,
  NRadioButton,
  NCollapse,
  NCollapseItem,
  NCollapseTransition,
  NBadge,
  NTooltip,
  useMessage
} from 'naive-ui'
import { ArrowBackOutline, CloseOutline, AddOutline, ColorPaletteOutline, BarChartOutline, PieChartOutline, TrendingUpOutline, StatsChartOutline, PulseOutline, SpeedometerOutline, FilterOutline, EllipsisVerticalOutline, SyncOutline, ImageOutline, ExpandOutline, DownloadOutline, PlayOutline, CreateOutline, CodeOutline } from '@vicons/ionicons5'
import echarts from '@/utils/echarts'
import { getDataSourceList, getDataSourceTables, getTableColumns } from '@/api/dataSource'
import { getAvailableReports, getReportDefinitionById } from '@/api/reportDefinition'
import {
  createChartDefinition,
  updateChartDefinition,
  getChartDefinitionById,
  testChartSql
} from '@/api/chart'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { logger } from '@/utils/logger'
import { exportToExcel } from '@/utils/export'
import { getColorSchemeOptions, getColorScheme } from '@/utils/chartColorSchemes'
import { getTemplatesByCategory, getTemplateCategories, getTemplateById } from '@/utils/chartTemplates'
import type { DataSource } from '@/types/dataSource'
import type { ChartDefinition, ChartParameter, MapChartConfig, KpiChartConfig, WaterfallChartConfig, WordCloudChartConfig, ComboChartConfig } from '@/types/chart'
import { CHART_TYPES } from '@/types/chart'
import { useTabsStore } from '@/stores/tabs'
import { MapChartConfig as MapChartConfigPanel, KpiChartConfig as KpiChartConfigPanel, WaterfallChartConfig as WaterfallChartConfigPanel, WordCloudChartConfig as WordCloudChartConfigPanel, ComboChartConfig as ComboChartConfigPanel, hasChartConfigPanel } from './configs'
// 🆕 WYSIWYG 增强组件
import ConfigTooltip from './ConfigTooltip.vue'
import PreviewStatusBar from './PreviewStatusBar.vue'
import { useChartPreview, CONFIG_TOOLTIPS, PREVIEW_UPDATE_CONFIG } from '@/composables/useChartPreview'
import { useShortcuts, COMMON_SHORTCUTS } from '@/composables/useShortcuts'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const tabsStore = useTabsStore()
initMessage(message)

// 🆕 实时预览 Hook - 实现所见即所得的配置体验 (需求 3.1, 3.4)
const {
  state: previewState,
  triggerUpdate: triggerPreviewUpdate,
  updateNow: updatePreviewNow,
  isPaused: isPreviewPaused
} = useChartPreview(
  async () => {
    if (previewData.value.length > 0) {
      generateChartConfig()
      await nextTick()
      renderChart()
    }
  },
  {
    debounceDelay: PREVIEW_UPDATE_CONFIG.debounceDelay,
    enableRealtime: true,
    onPreviewUpdate: () => {
      // 预览更新完成回调
    },
    onPreviewError: (error) => {
      console.error('[ChartPreview] Error:', error)
    }
  }
)

// 🆕 配置提示文本
const configTooltips = CONFIG_TOOLTIPS

// 🆕 快捷键支持 (需求 3.5)
const { register: registerShortcut, getShortcutList } = useShortcuts({
  scope: 'chart-designer',
  autoEnable: true
})

// 注册图表设计器快捷键 - 延迟注册以确保 handleSubmit 等函数已定义
const setupShortcuts = () => {
  registerShortcut({
    key: COMMON_SHORTCUTS.SAVE,
    handler: (e) => {
      e.preventDefault()
      if (!submitting.value) {
        handleSubmit()
      }
    },
    description: '保存图表'
  })

  registerShortcut({
    key: COMMON_SHORTCUTS.ESCAPE,
    handler: (e) => {
      e.preventDefault()
      // 如果有模态框打开，关闭模态框
      if (showTemplateModal.value) {
        showTemplateModal.value = false
      } else {
        // 否则取消编辑
        handleCancel()
      }
    },
    description: '取消/关闭'
  })

  registerShortcut({
    key: COMMON_SHORTCUTS.PREVIEW,
    handler: (e) => {
      e.preventDefault()
      // 刷新预览
      handleRefreshPreview()
    },
    description: '刷新预览'
  })

  registerShortcut({
    key: COMMON_SHORTCUTS.EXPORT,
    handler: (e) => {
      e.preventDefault()
      // 导出数据
      if (previewData.value.length > 0) {
        handleExportPreviewData()
      }
    },
    description: '导出数据'
  })
}

// 可视化查询构建器
const showQueryBuilder = ref(false)
const builderMainTable = ref<string | null>(null)
const builderFields = ref<string[]>([])
const builderColumnOptions = ref<Array<{ label: string; value: string }>>([])
const builderWhere = ref('')
const builderOrderBy = ref('')
const builderLimit = ref<number | null>(1000)
const loadBuilderColumns = async (tableName: string | null) => {
  builderColumnOptions.value = []
  builderFields.value = []
  if (!tableName || !form.dataSourceId) return
  try {
    const res = await getTableColumns(form.dataSourceId, tableName)
    const cols = ((res?.data as any) || []) as any[]
    builderColumnOptions.value = cols.map((c: any) => ({ label: c.columnName || c.name, value: c.columnName || c.name }))
  } catch { /* ignore */ }
}
const builderPreviewSql = computed(() => {
  if (!builderMainTable.value) return '-- 请选择主表'
  const fields = builderFields.value.length > 0 ? builderFields.value.join(', ') : '*'
  let sql = `SELECT ${fields}\nFROM ${builderMainTable.value}`
  if (builderWhere.value.trim()) sql += `\nWHERE ${builderWhere.value.trim()}`
  if (builderOrderBy.value.trim()) sql += `\nORDER BY ${builderOrderBy.value.trim()}`
  if (builderLimit.value) sql += `\nLIMIT ${builderLimit.value}`
  return sql
})
const applyBuilderSql = () => {
  if (!builderMainTable.value) return
  form.sqlContent = builderPreviewSql.value
  showQueryBuilder.value = false
}

// 模板相关
const showTemplateModal = ref(false)
const selectedTemplateId = ref<string | null>(null)
const templateCategories = computed(() => getTemplateCategories())

const TEMPLATE_VISUAL_SIZE_BY_TYPE: Record<string, { width: number | null; height: number }> = {
  line: { width: null, height: 420 },
  bar: { width: null, height: 420 },
  pie: { width: null, height: 420 },
  scatter: { width: null, height: 420 },
  radar: { width: null, height: 440 },
  gauge: { width: null, height: 360 },
  funnel: { width: null, height: 420 },
  heatmap: { width: null, height: 460 }
}

const parseTemplateVisualSize = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') return null
  const num = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

const normalizeTemplateConfig = (templateConfig: any, chartType: string) => {
  const config = JSON.parse(JSON.stringify(templateConfig || {}))
  const fallbackSize = TEMPLATE_VISUAL_SIZE_BY_TYPE[chartType] || { width: null, height: 400 }
  const metadataSize = config?.metadata?.visualSize
  const styleSize = config?.styleConfig || config?.metadata?.styleConfig
  const width = parseTemplateVisualSize(metadataSize?.width ?? styleSize?.width ?? config?.width) ?? fallbackSize.width
  const height = parseTemplateVisualSize(metadataSize?.height ?? styleSize?.height ?? config?.height) ?? fallbackSize.height
  const visualSize = { width, height }

  config.metadata = {
    ...(config.metadata || {}),
    chartType,
    visualSize
  }
  config.styleConfig = {
    ...(config.styleConfig || {}),
    width,
    height
  }

  return { config, visualSize }
}

// 获取图表图标
const getChartIcon = (chartType: string) => {
  const iconMap: Record<string, any> = {
    line: TrendingUpOutline,
    bar: BarChartOutline,
    pie: PieChartOutline,
    scatter: StatsChartOutline,
    radar: PulseOutline,
    gauge: SpeedometerOutline,
    funnel: BarChartOutline,
    heatmap: BarChartOutline
  }
  return iconMap[chartType] || BarChartOutline
}

// 获取模板预览背景色
const getTemplatePreviewBg = (chartType: string) => {
  const colors: Record<string, string> = {
    line: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    bar: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    pie: 'linear-gradient(135deg, #ee0979 0%, #ff6a00 100%)',
    scatter: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    radar: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    gauge: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    funnel: 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
    heatmap: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  }
  return colors[chartType] || 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
}

// 应用模板
const handleApplyTemplate = () => {
  if (!selectedTemplateId.value) return
  
  const template = getTemplateById(selectedTemplateId.value)
  if (!template) return
  
  // 更新图表类型
  form.chartType = template.chartType
  
  // 更新样式配置
  const { config, visualSize } = normalizeTemplateConfig(template.config, template.chartType)
  if (config.title?.text !== undefined) {
    styleConfig.title = config.title.text
  }
  if (config.legend?.show !== undefined) {
    styleConfig.showLegend = config.legend.show !== false
  }
  if (config.series?.[0]?.smooth !== undefined) {
    styleConfig.smooth = config.series[0].smooth
  }
  styleConfig.width = visualSize.width
  styleConfig.height = visualSize.height
  
  // 存储模板配置用于后续生成
  chartConfigJson.value = JSON.stringify(config, null, 2)
  form.chartConfig = chartConfigJson.value
  
  showTemplateModal.value = false
  selectedTemplateId.value = null
  message.success(`已应用模板: ${template.name}`)
}

// 表单数据
const form = reactive<Partial<ChartDefinition>>({
  id: undefined,
  chartName: '',
  chartCode: '',
  chartType: 'line',
  dataSourceId: 0,
  sqlContent: '',
  description: '',
  status: 1,
  chartConfig: ''
})

const submitting = ref(false)
const previewLoading = ref(false)
const showDataPreview = ref(true)
const chartPreviewRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

// 数据源类型和相关选项
const sourceType = ref<'database' | 'report'>('database')  // 数据来源类型
const dataConfigMode = ref<'field' | 'sql'>('field')  // 🆕 数据配置模式：字段模式/SQL模式

// 🆕 配置类型下拉选项
const dataConfigModeOptions = [
  { label: '自定义字段', value: 'field' },
  { label: 'SQL', value: 'sql' }
]

const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const selectedTable = ref<string | null>(null)
const tableOptions = ref<Array<{ label: string; value: string }>>([])
const tablesLoading = ref(false)
const columnOptions = ref<Array<{ label: string; value: string; dataType: string }>>([])

// 数值类型字段选项（用于汇总表和透视表）
const numericColumnOptions = computed(() => {
  return columnOptions.value.filter(col => {
    const type = col.dataType.toLowerCase()
    return type.includes('int') || type.includes('decimal') || type.includes('float') || 
           type.includes('double') || type.includes('number') || type.includes('numeric')
  })
})

// 表头样式（通过CSS变量）
const tableHeaderStyle = computed(() => {
  return {
    '--header-bg-color': tableStyleConfig.headerBgColor || '#f5f7fa',
    '--header-text-color': tableStyleConfig.headerTextColor || '#303133',
    '--header-font-weight': tableStyleConfig.headerFontWeight === 'bold' ? '600' : '400'
  }
})

// 报表相关
const reportOptions = ref<Array<{ label: string; value: number }>>([])
const selectedReportId = ref<number | null>(null)
const reportsLoading = ref(false)

// 🆕 数据筛选条件
const queryConditions = ref<Array<{ field: string; operator: string; value: string }>>([])
const dataLimit = ref(1000) // 默认1000行，避免数据量过大导致卡顿

// 🆕 图表查询参数
const chartParameters = ref<ChartParameter[]>([])

// 🆕 展开的参数索引（用于折叠参数详细配置）
const expandedParamIndex = ref(-1)

// 🆕 获取字段类型标签颜色
const getFieldTagType = (dataType: string): 'default' | 'info' | 'success' | 'warning' | 'error' => {
  const type = dataType.toLowerCase()
  if (type.includes('int') || type.includes('decimal') || type.includes('float') || type.includes('double') || type.includes('number')) {
    return 'success'
  } else if (type.includes('date') || type.includes('time')) {
    return 'info'
  } else if (type.includes('char') || type.includes('text') || type.includes('varchar')) {
    return 'warning'
  }
  return 'default'
}

// 🆕 获取参数类型标签颜色
const getParamTagType = (paramType: string): 'default' | 'info' | 'success' | 'warning' | 'error' => {
  const tagMap: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    text: 'default',
    number: 'success',
    date: 'info',
    dateRange: 'info',
    select: 'warning',
    multiSelect: 'warning'
  }
  return tagMap[paramType] || 'default'
}


// 🆕 日期快捷选项
const datePresetOptions = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '前天', value: 'dayBeforeYesterday' },
  { label: '本周一', value: 'thisMonday' },
  { label: '上周一', value: 'lastMonday' },
  { label: '本月初', value: 'thisMonthStart' },
  { label: '上月初', value: 'lastMonthStart' },
  { label: '上月末', value: 'lastMonthEnd' },
  { label: '本季度初', value: 'thisQuarterStart' },
  { label: '上季度初', value: 'lastQuarterStart' },
  { label: '本年初', value: 'thisYearStart' },
  { label: '去年初', value: 'lastYearStart' },
  { label: '去年末', value: 'lastYearEnd' }
]

// 🆕 日期范围快捷选项
const dateRangePresetOptions = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '本周', value: 'thisWeek' },
  { label: '上周', value: 'lastWeek' },
  { label: '本月', value: 'thisMonth' },
  { label: '上月', value: 'lastMonth' },
  { label: '本季度', value: 'thisQuarter' },
  { label: '上季度', value: 'lastQuarter' },
  { label: '本年', value: 'thisYear' },
  { label: '去年', value: 'lastYear' },
  { label: '最近7天', value: 'last7Days' },
  { label: '最近30天', value: 'last30Days' },
  { label: '最近90天', value: 'last90Days' }
]

// 🆕 计算日期快捷值
const calculateDatePreset = (preset: string): string => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const date = now.getDate()
  const day = now.getDay()
  
  const formatDate = (d: Date) => {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }
  
  switch (preset) {
    case 'today':
      return formatDate(now)
    case 'yesterday':
      return formatDate(new Date(year, month, date - 1))
    case 'dayBeforeYesterday':
      return formatDate(new Date(year, month, date - 2))
    case 'thisMonday':
      const daysToMonday = day === 0 ? 6 : day - 1
      return formatDate(new Date(year, month, date - daysToMonday))
    case 'lastMonday':
      const daysToLastMonday = day === 0 ? 13 : day + 6
      return formatDate(new Date(year, month, date - daysToLastMonday))
    case 'thisMonthStart':
      return formatDate(new Date(year, month, 1))
    case 'lastMonthStart':
      return formatDate(new Date(year, month - 1, 1))
    case 'lastMonthEnd':
      return formatDate(new Date(year, month, 0))
    case 'thisQuarterStart':
      const thisQuarter = Math.floor(month / 3)
      return formatDate(new Date(year, thisQuarter * 3, 1))
    case 'lastQuarterStart':
      const lastQuarter = Math.floor(month / 3) - 1
      const lastQuarterYear = lastQuarter < 0 ? year - 1 : year
      const lastQuarterMonth = lastQuarter < 0 ? 9 : lastQuarter * 3
      return formatDate(new Date(lastQuarterYear, lastQuarterMonth, 1))
    case 'thisYearStart':
      return formatDate(new Date(year, 0, 1))
    case 'lastYearStart':
      return formatDate(new Date(year - 1, 0, 1))
    case 'lastYearEnd':
      return formatDate(new Date(year - 1, 11, 31))
    default:
      return formatDate(now)
  }
}

// 🆕 计算日期范围快捷值
const calculateDateRangePreset = (preset: string): [string, string] => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  const date = now.getDate()
  const day = now.getDay()
  
  const formatDate = (d: Date) => {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  }
  
  switch (preset) {
    case 'today':
      const today = formatDate(now)
      return [today, today]
    case 'yesterday':
      const yesterday = formatDate(new Date(year, month, date - 1))
      return [yesterday, yesterday]
    case 'thisWeek':
      const daysToMonday = day === 0 ? 6 : day - 1
      const monday = new Date(year, month, date - daysToMonday)
      const sunday = new Date(year, month, date - daysToMonday + 6)
      return [formatDate(monday), formatDate(sunday)]
    case 'lastWeek':
      const daysToLastMonday = day === 0 ? 13 : day + 6
      const lastMonday = new Date(year, month, date - daysToLastMonday)
      const lastSunday = new Date(year, month, date - daysToLastMonday + 6)
      return [formatDate(lastMonday), formatDate(lastSunday)]
    case 'thisMonth':
      return [formatDate(new Date(year, month, 1)), formatDate(new Date(year, month + 1, 0))]
    case 'lastMonth':
      return [formatDate(new Date(year, month - 1, 1)), formatDate(new Date(year, month, 0))]
    case 'thisQuarter':
      const thisQuarter = Math.floor(month / 3)
      return [formatDate(new Date(year, thisQuarter * 3, 1)), formatDate(new Date(year, thisQuarter * 3 + 3, 0))]
    case 'lastQuarter':
      const lastQuarter = Math.floor(month / 3) - 1
      const lqYear = lastQuarter < 0 ? year - 1 : year
      const lqMonth = lastQuarter < 0 ? 9 : lastQuarter * 3
      return [formatDate(new Date(lqYear, lqMonth, 1)), formatDate(new Date(lqYear, lqMonth + 3, 0))]
    case 'thisYear':
      return [formatDate(new Date(year, 0, 1)), formatDate(new Date(year, 11, 31))]
    case 'lastYear':
      return [formatDate(new Date(year - 1, 0, 1)), formatDate(new Date(year - 1, 11, 31))]
    case 'last7Days':
      return [formatDate(new Date(year, month, date - 6)), formatDate(now)]
    case 'last30Days':
      return [formatDate(new Date(year, month, date - 29)), formatDate(now)]
    case 'last90Days':
      return [formatDate(new Date(year, month, date - 89)), formatDate(now)]
    default:
      return [formatDate(now), formatDate(now)]
  }
}

// 🆕 处理日期快捷值变化
const handleDatePresetChange = (param: ChartParameter, preset: string) => {
  if (preset) {
    param.defaultValue = calculateDatePreset(preset)
  } else {
    param.defaultValue = null
  }
}

// 🆕 处理日期范围快捷值变化
const handleDateRangePresetChange = (param: ChartParameter, preset: string) => {
  if (preset) {
    param.defaultValue = calculateDateRangePreset(preset)
  } else {
    param.defaultValue = null
  }
}

// 🆕 处理参数类型变化（新版）
const handleParamTypeChangeNew = (param: ChartParameter) => {
  // 重置默认值和相关配置
  param.defaultValue = null
  param.datePreset = undefined
  param.optionSource = 'manual'
  param.optionTags = []
  param.optionSql = ''
  param.min = undefined
  param.max = undefined
  
  // 根据类型设置默认运算符
  if (param.type === 'dateRange') {
    param.operator = 'BETWEEN'
  } else if (param.type === 'multiSelect') {
    param.operator = 'IN'
  } else {
    param.operator = '='
  }
  
  // 如果是下拉类型，初始化选项数组
  if (param.type === 'select' || param.type === 'multiSelect') {
    if (!param.options) {
      param.options = []
    }
  }
}

// 🆕 更新参数选项（从标签转换）
const updateParamOptions = (param: ChartParameter, tags: string[]) => {
  param.options = tags.map(tag => ({ label: tag, value: tag }))
}

// 🆕 字段右键菜单
const showFieldContextMenu = ref(false)
const fieldContextMenuX = ref(0)
const fieldContextMenuY = ref(0)
const contextMenuField = ref<{ label: string; value: string; dataType: string } | null>(null)

const fieldContextMenuOptions = [
  { label: '添加为查询参数', key: 'addAsParam' },
  { label: '设为X轴（维度）', key: 'setAsXAxis' },
  { label: '添加到Y轴（指标）', key: 'addToYAxis' }
]

// 处理字段右键菜单
const handleFieldContextMenu = (e: MouseEvent, col: { label: string; value: string; dataType: string }) => {
  e.preventDefault()
  contextMenuField.value = col
  fieldContextMenuX.value = e.clientX
  fieldContextMenuY.value = e.clientY
  showFieldContextMenu.value = true
}

// 处理右键菜单选择
const handleFieldContextMenuSelect = (key: string) => {
  showFieldContextMenu.value = false
  if (!contextMenuField.value) return
  
  const col = contextMenuField.value
  
  switch (key) {
    case 'addAsParam':
      // 根据字段类型推断参数类型
      const dataType = col.dataType.toLowerCase()
      let paramType: 'text' | 'number' | 'date' | 'dateRange' | 'select' | 'multiSelect' = 'text'
      if (dataType.includes('int') || dataType.includes('decimal') || dataType.includes('float') || dataType.includes('double') || dataType.includes('number')) {
        paramType = 'number'
      } else if (dataType.includes('date') || dataType.includes('time')) {
        paramType = 'date'
      }
      
      chartParameters.value.push({
        field: col.value,
        operator: '=',
        name: col.value,
        label: col.label.replace(/\s*\(.*\)$/, ''), // 移除括号中的注释
        type: paramType,
        required: false,
        defaultValue: null,
        placeholder: `请输入${col.label.replace(/\s*\(.*\)$/, '')}`,
        options: []
      })
      message.success(`已添加参数: ${col.value}`)
      break
    case 'setAsXAxis':
      fieldMapping.xAxis = col.value
      message.success(`已设置X轴: ${col.value}`)
      break
    case 'addToYAxis':
      if (!fieldMapping.yAxis.includes(col.value)) {
        fieldMapping.yAxis.push(col.value)
        // 设置默认聚合方式
        if (!fieldAggregates.value[col.value]) {
          fieldAggregates.value[col.value] = 'NONE'
        }
        message.success(`已添加到Y轴: ${col.value}`)
      } else {
        message.warning(`${col.value} 已在Y轴中`)
      }
      break
  }
  
  contextMenuField.value = null
}

// 参数类型选项
const parameterTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '日期范围', value: 'dateRange' },
  { label: '下拉单选', value: 'select' },
  { label: '下拉多选', value: 'multiSelect' }
]

// 根据字段类型获取可用的参数类型
const getParamTypeOptionsForField = (fieldName: string) => {
  const field = columnOptions.value.find(c => c.value === fieldName)
  if (!field) return parameterTypeOptions
  
  const dataType = field.dataType.toLowerCase()
  
  // 日期类型字段
  if (dataType.includes('date') || dataType.includes('time')) {
    return [
      { label: '日期', value: 'date' },
      { label: '日期范围', value: 'dateRange' }
    ]
  }
  
  // 数字类型字段
  if (dataType.includes('int') || dataType.includes('decimal') || dataType.includes('float') || dataType.includes('double') || dataType.includes('number')) {
    return [
      { label: '数字', value: 'number' },
      { label: '下拉单选', value: 'select' }
    ]
  }
  
  // 文本类型字段
  return [
    { label: '文本', value: 'text' },
    { label: '下拉单选', value: 'select' },
    { label: '下拉多选', value: 'multiSelect' }
  ]
}

// 获取参数类型标签
const getParamTypeLabel = (type: string) => {
  const opt = parameterTypeOptions.find(o => o.value === type)
  return opt?.label || type
}


// 🆕 指标字段的聚合配置（每个字段独立配置）
const fieldAggregates = ref<Record<string, string>>({}) // { fieldName: 'SUM' | 'AVG' | 'COUNT' | 'NONE' }

// 🆕 Y轴指标字段的别名配置
const yAxisLabels = ref<Record<string, string>>({}) // { fieldName: '显示名称' }

// 聚合函数选项
const aggregateFunctionOptions = [
  { label: '不聚合（明细数据）', value: 'NONE' },
  { label: '求和 (SUM)', value: 'SUM' },
  { label: '平均值 (AVG)', value: 'AVG' },
  { label: '计数 (COUNT)', value: 'COUNT' },
  { label: '最大值 (MAX)', value: 'MAX' },
  { label: '最小值 (MIN)', value: 'MIN' }
]

// 运算符选项
const operatorOptions = [
  { label: '=', value: '=' },
  { label: '!=', value: '!=' },
  { label: '>', value: '>' },
  { label: '>=', value: '>=' },
  { label: '<', value: '<' },
  { label: '<=', value: '<=' },
  { label: 'LIKE', value: 'LIKE' },
  { label: 'NOT LIKE', value: 'NOT LIKE' },
  { label: 'IN', value: 'IN' },
  { label: 'NOT IN', value: 'NOT IN' },
  { label: 'IS NULL', value: 'IS NULL' },
  { label: 'IS NOT NULL', value: 'IS NOT NULL' }
]

// 字段映射
const fieldMapping = reactive({
  xAxis: null as string | null,
  yAxis: [] as string[],
  group: null as string | null,
  sortField: 'x' as string,  // 排序字段：x=按X轴, y=按Y轴数值
  sortOrder: 'ASC' as string, // 排序方式：ASC=升序, DESC=降序
  // 特殊字段
  value: null as string | null,
  region: null as string | null,
  parent: null as string | null,
  name: null as string | null,
  source: null as string | null,
  target: null as string | null,
  open: null as string | null,
  close: null as string | null,
  low: null as string | null,
  high: null as string | null
})

// 🆕 维度字段（日期、文本、分类字段 - 排除主键ID）
const dimensionFields = computed(() => {
  return columnOptions.value.filter(col => {
    const type = col.dataType.toLowerCase()
    const name = col.value.toLowerCase()
    
    // 排除主键ID（通常就叫id或以_id结尾但很短）
    if (name === 'id' || (name.endsWith('_id') && name.length < 10)) {
      return false
    }
    
    // 包含：日期时间、文本、枚举类型字段
    return type.includes('date') || type.includes('time') || 
           type.includes('char') || type.includes('text') ||
           type.includes('varchar') || type.includes('enum') ||
           name.includes('type') || name.includes('status') ||
           name.includes('category') || name.includes('name')
  })
})

// 🆕 指标字段（数值字段 - 用于统计计算）
const measureFields = computed(() => {
  const result = columnOptions.value.filter(col => {
    const type = col.dataType.toLowerCase()
    const name = col.value.toLowerCase()
    
    // 排除主键、外键、状态码
    if (name === 'id' || name.endsWith('_id') || name === 'status' || name === 'type') {
      return false
    }
    
    // 包含所有数值类型
    return type.includes('int') || type.includes('decimal') || 
           type.includes('float') || type.includes('double') ||
           type.includes('number') || type.includes('numeric') ||
           name.includes('amount') || name.includes('price') ||
           name.includes('count') || name.includes('total') ||
           name.includes('sum') || name.includes('qty')
  })
  
  return result
})

// 🆕 SQL模式字段选项（从SQL解析或预览数据中获取）
const sqlFieldOptions = computed(() => {
  const options: Array<{ label: string; value: string }> = []
  
  // 1. 从预览数据中提取字段
  if (previewData.value.length > 0) {
    const firstRow = previewData.value[0]
    Object.keys(firstRow).forEach(key => {
      if (!options.some(o => o.value === key)) {
        options.push({ label: key, value: key })
      }
    })
  }
  
  // 2. 从SQL中解析字段别名
  if (form.sqlContent) {
    const aliases = extractSqlAliases(form.sqlContent)
    aliases.forEach(a => {
      if (!options.some(o => o.value === a.alias)) {
        options.push({ label: `${a.alias} (SQL)`, value: a.alias })
      }
    })
  }
  
  // 3. 从columnOptions中获取（数据库表字段）
  columnOptions.value.forEach(col => {
    if (!options.some(o => o.value === col.value)) {
      options.push({ label: col.label, value: col.value })
    }
  })
  
  return options
})

// 样式配置
const styleConfig = reactive({
  title: '',
  titlePosition: 'center',
  color: '#18a058',
  colorScheme: 'default',
  backgroundColor: '#ffffff',  // 新增：背景颜色
  showLegend: true,
  legendPosition: 'top',
  showGrid: true,
  showLabel: false,
  width: null as number | null,
  height: null as number | null,
  animation: true,
  animationDuration: 1000,
  // 折线图配置
  smooth: true,
  showSymbol: true,
  symbolSize: 4,
  // 柱状图配置
  barWidth: 'auto' as string | number,
  barMaxWidth: null as number | null
})

const DEFAULT_CHART_HEIGHT = 400

const parseVisualSizeValue = (value: unknown): number | null => {
  if (value === null || value === undefined || value === '') return null
  const num = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(num) && num > 0 ? num : null
}

const getChartVisualSize = () => ({
  width: parseVisualSizeValue(styleConfig.width),
  height: parseVisualSizeValue(styleConfig.height) ?? DEFAULT_CHART_HEIGHT
})

const chartPreviewStyle = computed(() => {
  const size = getChartVisualSize()
  return {
    width: size.width ? `${size.width}px` : '100%',
    height: `${size.height}px`
  }
})

const tablePreviewWrapperStyle = computed(() => [
  chartPreviewStyle.value,
  tableHeaderStyle.value
])

const tablePreviewMaxHeight = computed(() => Math.max(getChartVisualSize().height - 50, 150))
const summaryTablePreviewMaxHeight = computed(() => Math.max(getChartVisualSize().height - 100, 150))

const applyVisualSizeFromConfig = (config: any, echartsConfig: any, resetMissing = true) => {
  const metadataSize = config?.metadata?.visualSize
  const styleSize = config?.styleConfig || config?.metadata?.styleConfig
  const width = parseVisualSizeValue(metadataSize?.width ?? styleSize?.width ?? echartsConfig?.width ?? config?.width)
  const height = parseVisualSizeValue(metadataSize?.height ?? styleSize?.height ?? echartsConfig?.height ?? config?.height)
  
  if (resetMissing || width !== null) {
    styleConfig.width = width
  }
  if (resetMissing || height !== null) {
    styleConfig.height = height
  }
}

// 表格样式配置
const tableStyleConfig = reactive({
  striped: true,
  size: 'medium' as 'small' | 'medium' | 'large',
  showPagination: true,
  pageSize: 10,
  showIndex: true,
  bordered: true,
  // 新增配置
  displayColumns: [] as string[],  // 要显示的字段列表，空表示全部
  columnLabels: {} as Record<string, string>,  // 字段别名
  defaultSortField: '' as string,  // 默认排序字段
  defaultSortOrder: 'ASC' as 'ASC' | 'DESC',  // 默认排序方向
  enableExport: true,  // 是否允许导出
  exportFileName: '',  // 导出文件名
  // 表头样式配置
  headerBgColor: '#f5f7fa',  // 表头背景色
  headerTextColor: '#303133',  // 表头文字颜色
  headerFontWeight: 'bold' as 'normal' | 'bold',  // 表头字体粗细
  headerAlign: 'center' as 'left' | 'center' | 'right',  // 表头对齐方式
  // 汇总表配置
  showSummary: false,  // 是否显示汇总行
  summaryColumns: [] as string[],  // 需要汇总的列
  summaryTypes: {} as Record<string, 'sum' | 'avg' | 'max' | 'min' | 'count'>,  // 汇总方式
  summaryLabel: '合计',  // 汇总行标签
  // 透视表配置
  pivotRowField: '' as string,  // 行字段
  pivotColField: '' as string,  // 列字段
  pivotValueField: '' as string,  // 值字段
  pivotAggType: 'sum' as 'sum' | 'avg' | 'max' | 'min' | 'count',  // 聚合方式
  pivotShowRowTotal: true,  // 显示行合计
  pivotShowColTotal: true,  // 显示列合计
  // 条件格式化
  enableConditionalFormat: false,  // 是否启用条件格式化
  conditionalFormatField: '' as string,  // 条件格式化字段
  conditionalFormatType: 'colorScale' as 'colorScale' | 'dataBar' | 'iconSet',  // 格式化类型
  conditionalFormatColors: ['#f5222d', '#faad14', '#52c41a'] as string[]  // 颜色梯度（低-中-高）
})

// 图表专属配置 - 地图图表
const mapChartConfig = reactive<MapChartConfig>({
  mapType: 'china',
  showLabel: true,
  enableDrillDown: false,
  regionField: '',
  valueField: '',
  visualMapConfig: {
    min: undefined,
    max: undefined,
    inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695'] },
    text: ['高', '低']
  }
})

// 图表专属配置 - KPI 卡片
const kpiChartConfig = reactive<KpiChartConfig>({
  format: 'number',
  currencySymbol: '¥',
  decimals: 2,
  unit: '',
  prefix: '',
  suffix: '',
  showTrend: true,
  positiveIsGood: true,
  showSparkline: false,
  sparklineType: 'line',
  valueField: '',
  previousValueField: '',
  periodPreviousField: ''
})

// 图表专属配置 - 瀑布图
const waterfallChartConfig = reactive<WaterfallChartConfig>({
  showTotal: true,
  totalLabel: '总计',
  startValue: 0,
  startLabel: '起始',
  positiveColor: '#52c41a',
  negativeColor: '#ff4d4f',
  totalColor: '#1890ff',
  nameField: '',
  valueField: '',
  isTotalField: ''
})

// 图表专属配置 - 词云图
const wordCloudChartConfig = reactive<WordCloudChartConfig>({
  shape: 'circle',
  colorRange: ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96'],
  minFontSize: 12,
  maxFontSize: 60,
  rotationRange: [-45, 45],
  wordField: '',
  weightField: ''
})

// 图表专属配置 - 组合图
const comboChartConfig = reactive<ComboChartConfig>({
  categoryField: '',
  barFields: [],
  lineFields: [],
  leftAxisLabel: '',
  rightAxisLabel: '',
  seriesConfig: []
})

// 是否有图表专属配置面板
const hasChartSpecificConfig = computed(() => {
  return hasChartConfigPanel(form.chartType || '')
})

// 处理图表专属配置变更
const handleChartSpecificConfigChange = () => {
  generateChartConfig()
  handlePreviewChart()
}

// 获取条件格式化的颜色
const getConditionalFormatColor = (value: number, min: number, max: number): string => {
  if (max === min) return tableStyleConfig.conditionalFormatColors[1]
  const ratio = (value - min) / (max - min)
  const colors = tableStyleConfig.conditionalFormatColors
  
  if (ratio <= 0.5) {
    // 低值到中值
    const r = ratio * 2
    return interpolateColor(colors[0], colors[1], r)
  } else {
    // 中值到高值
    const r = (ratio - 0.5) * 2
    return interpolateColor(colors[1], colors[2], r)
  }
}

// 颜色插值
const interpolateColor = (color1: string, color2: string, ratio: number): string => {
  const hex2rgb = (hex: string) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : { r: 0, g: 0, b: 0 }
  }
  
  const rgb2hex = (r: number, g: number, b: number) => {
    return '#' + [r, g, b].map(x => {
      const hex = Math.round(x).toString(16)
      return hex.length === 1 ? '0' + hex : hex
    }).join('')
  }
  
  const c1 = hex2rgb(color1)
  const c2 = hex2rgb(color2)
  
  return rgb2hex(
    c1.r + (c2.r - c1.r) * ratio,
    c1.g + (c2.g - c1.g) * ratio,
    c1.b + (c2.b - c1.b) * ratio
  )
}

// 获取条件格式化字段的最小最大值
const conditionalFormatRange = computed(() => {
  if (!tableStyleConfig.enableConditionalFormat || !tableStyleConfig.conditionalFormatField) {
    return { min: 0, max: 0 }
  }
  const field = tableStyleConfig.conditionalFormatField
  const values = previewData.value.map(row => parseFloat(row[field]) || 0)
  return {
    min: Math.min(...values),
    max: Math.max(...values)
  }
})

// 表格预览列
const tablePreviewColumns = computed(() => {
  if (previewData.value.length === 0) return []
  const firstRow = previewData.value[0]
  const columns: any[] = []
  
  // 添加序号列 - 使用全局索引
  if (tableStyleConfig.showIndex) {
    columns.push({
      title: '#',
      key: '_globalIndex',
      width: 60,
      align: 'center' as const
    })
  }
  
  // 获取要显示的字段
  const displayFields = tableStyleConfig.displayColumns && tableStyleConfig.displayColumns.length > 0
    ? tableStyleConfig.displayColumns
    : Object.keys(firstRow)
  
  // 根据数据字段生成列
  displayFields.forEach(key => {
    if (firstRow.hasOwnProperty(key)) {
      const title = tableStyleConfig.columnLabels[key] || key
      const column: any = {
        title: title,
        key: key,
        ellipsis: { tooltip: true },
        resizable: true,
        sorter: true,  // 启用排序
        align: tableStyleConfig.headerAlign as 'left' | 'center' | 'right'
      }
      
      // 条件格式化
      if (tableStyleConfig.enableConditionalFormat && tableStyleConfig.conditionalFormatField === key) {
        const { min, max } = conditionalFormatRange.value
        if (tableStyleConfig.conditionalFormatType === 'colorScale') {
          column.render = (row: any) => {
            const value = parseFloat(row[key]) || 0
            const bgColor = getConditionalFormatColor(value, min, max)
            return h('div', {
              style: {
                backgroundColor: bgColor,
                padding: '4px 8px',
                borderRadius: '4px',
                color: '#fff',
                textShadow: '0 1px 2px rgba(0,0,0,0.3)'
              }
            }, row[key])
          }
        } else if (tableStyleConfig.conditionalFormatType === 'dataBar') {
          column.render = (row: any) => {
            const value = parseFloat(row[key]) || 0
            const percentage = max === min ? 50 : ((value - min) / (max - min)) * 100
            return h('div', { style: { position: 'relative', padding: '4px 8px' } }, [
              h('div', {
                style: {
                  position: 'absolute',
                  left: 0,
                  top: 0,
                  bottom: 0,
                  width: `${percentage}%`,
                  backgroundColor: tableStyleConfig.conditionalFormatColors[2],
                  opacity: 0.3,
                  borderRadius: '4px'
                }
              }),
              h('span', { style: { position: 'relative', zIndex: 1 } }, row[key])
            ])
          }
        }
      }
      
      columns.push(column)
    }
  })
  
  return columns
})

// 汇总行数据
const tableSummaryData = computed(() => {
  if (!tableStyleConfig.showSummary || previewData.value.length === 0) return null
  
  const summary: Record<string, any> = {}
  const summaryColumns = tableStyleConfig.summaryColumns.length > 0 
    ? tableStyleConfig.summaryColumns 
    : []
  
  summaryColumns.forEach(col => {
    const aggType = tableStyleConfig.summaryTypes[col] || 'sum'
    const values = previewData.value.map(row => parseFloat(row[col]) || 0)
    
    switch (aggType) {
      case 'sum':
        summary[col] = values.reduce((a, b) => a + b, 0)
        break
      case 'avg':
        summary[col] = values.length > 0 ? values.reduce((a, b) => a + b, 0) / values.length : 0
        break
      case 'max':
        summary[col] = Math.max(...values)
        break
      case 'min':
        summary[col] = Math.min(...values)
        break
      case 'count':
        summary[col] = values.length
        break
    }
    // 格式化数字
    if (typeof summary[col] === 'number') {
      summary[col] = Number.isInteger(summary[col]) ? summary[col] : summary[col].toFixed(2)
    }
  })
  
  return summary
})

// 透视表数据
const pivotTableData = computed(() => {
  if (form.chartType !== 'pivotTable' || previewData.value.length === 0) return { columns: [], data: [] }
  
  const { pivotRowField, pivotColField, pivotValueField, pivotAggType, pivotShowRowTotal, pivotShowColTotal } = tableStyleConfig
  if (!pivotRowField || !pivotColField || !pivotValueField) return { columns: [], data: [] }
  
  // 获取所有唯一的列值
  const colValues = [...new Set(previewData.value.map(row => row[pivotColField]))].sort()
  
  // 按行字段分组
  const rowGroups: Record<string, Record<string, number[]>> = {}
  previewData.value.forEach(row => {
    const rowKey = row[pivotRowField]
    const colKey = row[pivotColField]
    const value = parseFloat(row[pivotValueField]) || 0
    
    if (!rowGroups[rowKey]) rowGroups[rowKey] = {}
    if (!rowGroups[rowKey][colKey]) rowGroups[rowKey][colKey] = []
    rowGroups[rowKey][colKey].push(value)
  })
  
  // 聚合计算
  const aggregate = (values: number[]): number => {
    if (values.length === 0) return 0
    switch (pivotAggType) {
      case 'sum': return values.reduce((a, b) => a + b, 0)
      case 'avg': return values.reduce((a, b) => a + b, 0) / values.length
      case 'max': return Math.max(...values)
      case 'min': return Math.min(...values)
      case 'count': return values.length
      default: return values.reduce((a, b) => a + b, 0)
    }
  }
  
  // 格式化数值
  const formatValue = (val: number): string | number => {
    return Number.isInteger(val) ? val : parseFloat(val.toFixed(2))
  }
  
  // 生成透视表数据
  const pivotData = Object.keys(rowGroups).map(rowKey => {
    const row: Record<string, any> = { [pivotRowField]: rowKey }
    let rowTotal = 0
    colValues.forEach(colKey => {
      const values = rowGroups[rowKey][colKey] || []
      const result = aggregate(values)
      row[String(colKey)] = formatValue(result)
      rowTotal += result
    })
    // 添加行合计
    if (pivotShowRowTotal) {
      row['_rowTotal'] = formatValue(rowTotal)
    }
    return row
  })
  
  // 添加列合计行
  if (pivotShowColTotal && pivotData.length > 0) {
    const totalRow: Record<string, any> = { [pivotRowField]: '合计' }
    let grandTotal = 0
    colValues.forEach(colKey => {
      const colTotal = pivotData.reduce((sum, row) => {
        const val = parseFloat(row[String(colKey)]) || 0
        return sum + val
      }, 0)
      totalRow[String(colKey)] = formatValue(colTotal)
      grandTotal += colTotal
    })
    if (pivotShowRowTotal) {
      totalRow['_rowTotal'] = formatValue(grandTotal)
    }
    pivotData.push(totalRow)
  }
  
  // 生成列定义
  const columns: any[] = [
    {
      title: tableStyleConfig.columnLabels[pivotRowField] || pivotRowField,
      key: pivotRowField,
      fixed: 'left' as const,
      width: 120,
      render: (row: any) => {
        // 合计行加粗显示
        if (row[pivotRowField] === '合计') {
          return h('span', { style: { fontWeight: 'bold' } }, row[pivotRowField])
        }
        return row[pivotRowField]
      }
    },
    ...colValues.map(colKey => ({
      title: String(colKey),
      key: String(colKey),
      align: 'right' as const
    }))
  ]
  
  // 添加行合计列
  if (pivotShowRowTotal) {
    columns.push({
      title: '合计',
      key: '_rowTotal',
      align: 'right' as const,
      width: 100,
      render: (row: any) => {
        return h('span', { style: { fontWeight: 'bold', color: '#1890ff' } }, row['_rowTotal'])
      }
    })
  }
  
  return { columns, data: pivotData }
})

// 表格预览分页状态
const tablePreviewPage = ref(1)
const tablePreviewPageSize = ref(10)

// 表格预览分页配置
const tablePreviewPagination = computed((): any => {
  if (!tableStyleConfig.showPagination) return false
  
  return {
    page: tablePreviewPage.value,
    pageSize: tablePreviewPageSize.value,
    showSizePicker: true,
    pageSizes: [
      { label: '10 条/页', value: 10 },
      { label: '20 条/页', value: 20 },
      { label: '50 条/页', value: 50 },
      { label: '100 条/页', value: 100 }
    ],
    prefix: ({ itemCount }: { itemCount: number | undefined }) => `共 ${itemCount || 0} 条`,
    onChange: (page: number) => {
      tablePreviewPage.value = page
    },
    onUpdatePageSize: (pageSize: number) => {
      tablePreviewPageSize.value = pageSize
      tablePreviewPage.value = 1
      // 同步更新配置
      tableStyleConfig.pageSize = pageSize
    }
  }
})

// 汇总表渲染函数
const renderSummary = (): any => {
  if (!tableStyleConfig.showSummary || !tableSummaryData.value) return undefined
  
  const summaryData = tableSummaryData.value
  const displayFields = tableStyleConfig.displayColumns.length > 0 
    ? tableStyleConfig.displayColumns 
    : (previewData.value.length > 0 ? Object.keys(previewData.value[0]) : [])
  
  if (displayFields.length === 0) return undefined
  
  const firstField = displayFields[0] || '_globalIndex'
  
  return {
    [firstField]: {
      value: h('span', { style: { fontWeight: 'bold', color: '#1890ff' } }, tableStyleConfig.summaryLabel || '合计'),
      colSpan: tableStyleConfig.showIndex ? 2 : 1
    },
    ...Object.fromEntries(
      displayFields.slice(tableStyleConfig.showIndex ? 0 : 1).map(field => [
        field,
        {
          value: summaryData[field] !== undefined 
            ? h('span', { style: { fontWeight: 'bold', color: '#52c41a' } }, String(summaryData[field]))
            : ''
        }
      ])
    )
  }
}

// 交互配置
const interactionConfig = reactive({
  enableZoom: false,
  enableDataZoom: false,
  enableLegendSelect: true,
  enableTooltip: true,
  enableToolbox: false,
  toolboxFeatures: ['saveAsImage'] as string[],
  enableClick: false,
  enableHover: true
})

// 预览数据
const previewData = ref<any[]>([])
const dataPreviewColumns = ref<Array<{ title: string; key: string }>>([])

const chartConfigJson = ref('')

// 选项配置
const chartTypeOptions = CHART_TYPES.map(t => ({ label: t.label, value: t.value }))
const titlePositionOptions = [
  { label: '居中', value: 'center' },
  { label: '左侧', value: 'left' },
  { label: '右侧', value: 'right' }
]
const legendPositionOptions = [
  { label: '顶部', value: 'top' },
  { label: '底部', value: 'bottom' },
  { label: '左侧', value: 'left' },
  { label: '右侧', value: 'right' }
]

// 🆕 推荐配色方案（背景+颜色方案的搭配）
const colorPresets = [
  { 
    name: 'light-default', 
    label: '经典白底', 
    scheme: 'default', 
    background: '#ffffff',
    previewColors: ['#5470c6', '#91cc75', '#fac858', '#ee6666']
  },
  { 
    name: 'light-business', 
    label: '商务蓝', 
    scheme: 'business', 
    background: '#ffffff',
    previewColors: ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728']
  },
  { 
    name: 'light-cool', 
    label: '清新冷调', 
    scheme: 'cool', 
    background: '#f5f5f5',
    previewColors: ['#3498DB', '#2ECC71', '#1ABC9C', '#9B59B6']
  },
  { 
    name: 'dark-neon', 
    label: '霓虹暗夜', 
    scheme: 'neon', 
    background: '#1a1a1a',
    previewColors: ['#FF00FF', '#00FFFF', '#00FF00', '#FFFF00']
  },
  { 
    name: 'dark-tech', 
    label: '科技深蓝', 
    scheme: 'tech', 
    background: '#0d1117',
    previewColors: ['#0D47A1', '#1976D2', '#2196F3', '#64B5F6']
  },
  { 
    name: 'dark-vibrant', 
    label: '活力暗黑', 
    scheme: 'vibrant', 
    background: '#2c3e50',
    previewColors: ['#FF1744', '#00E676', '#00B0FF', '#FFEA00']
  },
  { 
    name: 'warm-sunset', 
    label: '日落暖调', 
    scheme: 'sunset', 
    background: '#fff8f0',
    previewColors: ['#FF5722', '#FF7043', '#FF8A65', '#FFAB91']
  },
  { 
    name: 'nature-forest', 
    label: '森林绿意', 
    scheme: 'forest', 
    background: '#f0f5f0',
    previewColors: ['#1B5E20', '#388E3C', '#4CAF50', '#81C784']
  }
]

// 应用颜色预设
const applyColorPreset = (preset: typeof colorPresets[0]) => {
  styleConfig.colorScheme = preset.scheme
  styleConfig.backgroundColor = preset.background
  handleStyleChange()
}

// 获取当前颜色方案的颜色
const getCurrentSchemeColors = () => {
  const scheme = getColorScheme(styleConfig.colorScheme)
  return scheme.colors.slice(0, 6)
}

// 渲染颜色方案选项标签（带颜色预览）
const renderColorSchemeLabel = (option: any) => {
  const scheme = getColorScheme(option.value)
  return h('div', { style: 'display: flex; align-items: center; gap: 8px;' }, [
    h('span', option.label),
    h('div', { style: 'display: flex; gap: 2px; margin-left: auto;' }, 
      scheme.colors.slice(0, 4).map(color => 
        h('span', { 
          style: `width: 12px; height: 12px; border-radius: 2px; background-color: ${color};` 
        })
      )
    )
  ])
}

const handleColorSchemeChange = () => {
  // 🆕 颜色方案改变时，使用防抖更新预览 (需求 3.3 - 200ms 内更新)
  if (previewData.value.length > 0) {
    triggerPreviewUpdate()
  }
}

const handleColorChange = () => {
  // 🆕 主题颜色改变时，使用防抖更新预览
  if (previewData.value.length > 0 && styleConfig.colorScheme === 'custom') {
    triggerPreviewUpdate()
  }
}

const handleStyleChange = () => {
  // 🆕 样式配置改变时，使用防抖更新预览 (需求 3.4 - 300ms 内更新)
  if (previewData.value.length > 0) {
    triggerPreviewUpdate()
  } else if (chartInstance) {
    // 无数据时也需要同步尺寸
    nextTick(() => chartInstance?.resize())
  }
}
// 使用导入的颜色方案选项
const baseColorSchemeOptions = getColorSchemeOptions()
const colorSchemeOptions = [
  ...baseColorSchemeOptions,
  { label: '自定义', value: 'custom', colors: [] }
]
const colorSwatches = [
  '#18a058',
  '#2080f0',
  '#f0a020',
  '#d03050',
  '#722ed1',
  '#eb2f96',
  '#fa541c',
  '#faad14'
]

onMounted(async () => {
  await loadDataSources()
  
  const id = route.params["id"]
  if (id && id !== 'new') {
    await loadChartDefinition(Number(id))
  } else {
    setDefaultChartConfig()
  }
  
  // 🆕 设置快捷键 (需求 3.5)
  setupShortcuts()
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

// 数据源加载状态
const dataSourceLoading = ref(false)

// 加载数据源
const loadDataSources = async () => {
  dataSourceLoading.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 1000 })
    const responseData = res?.data as any
    const list = responseData?.list || responseData || []
    dataSourceOptions.value = list.map((ds: DataSource) => ({
      label: `${ds.name} (${ds.dbType})`,
      value: ds.id
    }))
    logger.info(`成功加载 ${dataSourceOptions.value.length} 个数据源`)
    if (dataSourceOptions.value.length === 0) {
      message.warning('未找到可用的数据源，请先添加数据源')
    }
  } catch (error) {
    message.error('加载数据源失败：' + handleApiError(error, '加载数据源列表', ''))
    dataSourceOptions.value = []
  } finally {
    dataSourceLoading.value = false
  }
}

// 加载表列表
const loadTables = async (keepSelection = false) => {
  if (!form.dataSourceId) {
    tableOptions.value = []
    return
  }
  
  tablesLoading.value = true
  
  // 🔧 保存当前选中的表（编辑模式需要保留）
  const currentTable = keepSelection ? selectedTable.value : null
  
  tableOptions.value = []
  if (!keepSelection) {
    selectedTable.value = null
    columnOptions.value = []
  }
  
  try {
    const res = await getDataSourceTables(form.dataSourceId)
    const tables = ((res?.data as any) || []) as any[]
    tableOptions.value = tables.map(t => ({
      label: `${t.tableName}${t.tableComment ? ` (${t.tableComment})` : ''}`,
      value: t.tableName
    }))
    
    // 🔧 恢复之前选中的表
    if (keepSelection && currentTable) {
      selectedTable.value = currentTable
    }
    
    logger.info(`数据源 ${form.dataSourceId} 加载了 ${tableOptions.value.length} 个表`)
    if (tableOptions.value.length === 0) {
      message.warning('该数据源没有可用的表')
    }
  } catch (error) {
    message.error('加载表列表失败：' + handleApiError(error, '加载表列表', ''))
    tableOptions.value = []
  } finally {
    tablesLoading.value = false
  }
}

// 加载报表列表
const loadReports = async (keepSelection = false) => {
  reportsLoading.value = true
  
  // 🔧 保存当前选中的报表（编辑模式需要保留）
  const currentReportId = keepSelection ? selectedReportId.value : null
  
  reportOptions.value = []
  if (!keepSelection) {
    selectedReportId.value = null
    columnOptions.value = []
  }
  
  try {
    const res = await getAvailableReports()
    const reports = ((res?.data as any) || []) as any[]
    reportOptions.value = reports.map((r: any) => ({
      label: r.reportName,
      value: r.id
    }))
    
    // 🔧 恢复之前选中的报表
    if (keepSelection && currentReportId) {
      selectedReportId.value = currentReportId
    }
    
    logger.info(`加载了 ${reportOptions.value.length} 个可用报表`)
    if (reportOptions.value.length === 0) {
      message.warning('暂无可用报表')
    }
  } catch (error) {
    message.error('加载报表列表失败：' + handleApiError(error, '加载报表列表', ''))
    reportOptions.value = []
  } finally {
    reportsLoading.value = false
  }
}

// 字段加载状态
const columnsLoading = ref(false)

// 🆕 从SQL中提取字段映射（用于兼容AI生成的图表）
const extractFieldMappingFromSql = (sql: string, _chartType?: string): { xAxis: string | null, yAxis: string[] } => {
  const result = { xAxis: null as string | null, yAxis: [] as string[] }
  
  if (!sql) return result
  
  try {
    // 提取SELECT和FROM之间的字段列表
    const selectMatch = sql.match(/SELECT\s+(.+?)\s+FROM/is)
    if (!selectMatch) return result
    
    const fieldsStr = selectMatch[1]
    const fields = fieldsStr.split(',')
    
    const aliases: string[] = []
    for (const field of fields) {
      const trimmed = field.trim()
      // 提取AS后面的别名，或者字段本身
      const aliasMatch = trimmed.match(/(?:AS\s+)?`?(\w+)`?\s*$/i)
      if (aliasMatch) {
        aliases.push(aliasMatch[1])
      }
    }
    
    // 根据图表类型设置字段映射
    if (aliases.length >= 2) {
      result.xAxis = aliases[0]  // 第一个字段作为X轴
      result.yAxis = aliases.slice(1)  // 其余字段作为Y轴
    } else if (aliases.length === 1) {
      result.xAxis = aliases[0]
    }
  } catch (e) {
    console.warn('解析SQL字段映射失败:', e)
  }
  
  return result
}

// 🆕 从SQL中提取所有字段别名（用于将别名添加到可用字段列表）
const extractSqlAliases = (sql: string): Array<{ alias: string, original: string, hasAggregation: boolean }> => {
  const aliases: Array<{ alias: string, original: string, hasAggregation: boolean }> = []
  
  if (!sql) return aliases
  
  try {
    const selectMatch = sql.match(/SELECT\s+(.+?)\s+FROM/is)
    if (!selectMatch) return aliases
    
    const fieldsStr = selectMatch[1]
    // 按逗号分割，但要考虑括号内的逗号
    const fields: string[] = []
    let depth = 0
    let current = ''
    for (const char of fieldsStr) {
      if (char === '(') depth++
      else if (char === ')') depth--
      else if (char === ',' && depth === 0) {
        fields.push(current.trim())
        current = ''
        continue
      }
      current += char
    }
    if (current.trim()) fields.push(current.trim())
    
    for (const field of fields) {
      const trimmed = field.trim()
      // 检测是否有聚合函数
      const hasAggregation = /^(SUM|COUNT|AVG|MAX|MIN|GROUP_CONCAT)\s*\(/i.test(trimmed)
      
      // 匹配 "xxx AS alias" 或 "FUNC(xxx) AS alias" 格式
      const asMatch = trimmed.match(/(.+?)\s+AS\s+`?(\w+)`?\s*$/i)
      if (asMatch) {
        const original = asMatch[1].trim()
        const alias = asMatch[2]
        // 提取原始字段名（去掉聚合函数）
        const originalFieldMatch = original.match(/\(?`?(\w+)`?\)?/)
        aliases.push({
          alias,
          original: originalFieldMatch ? originalFieldMatch[1] : original,
          hasAggregation
        })
      } else {
        // 没有AS的情况，字段名就是别名
        const fieldMatch = trimmed.match(/`?(\w+)`?\s*$/)
        if (fieldMatch) {
          aliases.push({
            alias: fieldMatch[1],
            original: fieldMatch[1],
            hasAggregation
          })
        }
      }
    }
  } catch (e) {
    console.warn('解析SQL别名失败:', e)
  }
  
  return aliases
}

// 加载字段列表
const handleTableChange = async (keepFieldMapping = false) => {
  if (!form.dataSourceId || !selectedTable.value) {
    columnOptions.value = []
    return
  }
  
  columnsLoading.value = true
  
  // 保存当前字段映射（编辑模式需要保留）
  const savedXAxis = keepFieldMapping ? fieldMapping.xAxis : null
  const savedYAxis = keepFieldMapping ? [...fieldMapping.yAxis] : []
  // 保存表格配置（编辑模式需要保留）
  const savedDisplayColumns = keepFieldMapping ? [...tableStyleConfig.displayColumns] : []
  const savedColumnLabels = keepFieldMapping ? { ...tableStyleConfig.columnLabels } : {}
  const savedDefaultSortField = keepFieldMapping ? tableStyleConfig.defaultSortField : ''
  const savedDefaultSortOrder = keepFieldMapping ? tableStyleConfig.defaultSortOrder : 'ASC'
  
  columnOptions.value = []
  // 只在非保留模式下重置字段映射
  if (!keepFieldMapping) {
    fieldMapping.xAxis = null
    fieldMapping.yAxis = []
    fieldMapping.group = null
    // 重置表格配置
    tableStyleConfig.displayColumns = []
    tableStyleConfig.columnLabels = {}
    tableStyleConfig.defaultSortField = ''
    tableStyleConfig.defaultSortOrder = 'ASC'
  }
  
  try {
    const res = await getTableColumns(form.dataSourceId, selectedTable.value)
    const columns = ((res?.data as any) || []) as any[]
    columnOptions.value = columns.map(col => ({
      label: `${col.columnName}${col.columnComment ? ` (${col.columnComment})` : ''}`,
      value: col.columnName,
      dataType: col.dataType
    }))
    
    // 恢复字段映射
    if (keepFieldMapping) {
      fieldMapping.xAxis = savedXAxis
      fieldMapping.yAxis = savedYAxis
      
      // 恢复表格配置，并验证displayColumns是否有效
      const validColumnNames = columnOptions.value.map(c => String(c.value))
      tableStyleConfig.displayColumns = savedDisplayColumns.filter(col => validColumnNames.includes(String(col)))
      tableStyleConfig.columnLabels = savedColumnLabels
      tableStyleConfig.defaultSortField = validColumnNames.includes(String(savedDefaultSortField)) ? savedDefaultSortField : ''
      tableStyleConfig.defaultSortOrder = savedDefaultSortOrder
    }
    
    // 🆕 如果有SQL，提取SQL中的别名并添加到可用字段列表（支持AI生成的图表编辑）
    if (keepFieldMapping && form.sqlContent) {
      const sqlAliases = extractSqlAliases(form.sqlContent)
      const existingValues = new Set(columnOptions.value.map(c => c.value.toLowerCase()))
      
      for (const aliasInfo of sqlAliases) {
        // 如果别名不在现有字段列表中，添加为虚拟字段
        if (!existingValues.has(aliasInfo.alias.toLowerCase())) {
          columnOptions.value.push({
            label: `${aliasInfo.alias} (SQL别名${aliasInfo.hasAggregation ? ', 聚合' : ''})`,
            value: aliasInfo.alias,
            dataType: aliasInfo.hasAggregation ? 'DECIMAL' : 'VARCHAR'
          })
          existingValues.add(aliasInfo.alias.toLowerCase())
          logger.info(`添加SQL别名字段: ${aliasInfo.alias} <- ${aliasInfo.original}`)
        }
      }
    }
    
    logger.info(`表 ${selectedTable.value} 加载了 ${columnOptions.value.length} 个字段`)
    if (columnOptions.value.length === 0) {
      message.warning('该表没有可用的字段')
    } else {
      message.success(`成功加载 ${columnOptions.value.length} 个字段`)
      // 🔧 只在非保留模式下智能推荐字段
      if (!keepFieldMapping) {
        autoRecommendFields()
      }
    }
  } catch (error) {
    message.error('加载字段失败：' + handleApiError(error, '加载字段列表', ''))
    columnOptions.value = []
  } finally {
    columnsLoading.value = false
  }
}

// 报表改变
const handleReportChange = async (keepFieldMapping = false) => {
  if (!selectedReportId.value) {
    columnOptions.value = []
    return
  }
  
  // 🔧 保存当前字段映射（编辑模式需要保留）
  const savedXAxis = keepFieldMapping ? fieldMapping.xAxis : null
  const savedYAxis = keepFieldMapping ? [...fieldMapping.yAxis] : []
  
  columnOptions.value = []
  // 只在非保留模式下重置字段映射
  if (!keepFieldMapping) {
    fieldMapping.xAxis = null
    fieldMapping.yAxis = []
  }
  
  try {
    const res = await getReportDefinitionById(selectedReportId.value)
    const report = res.data as any
    
    if (report && report.fields) {
      columnOptions.value = report.fields.map((field: any) => ({
        label: field.fieldLabel || field.fieldName,
        value: field.fieldName,
        dataType: field.fieldType || 'varchar'
      }))
      
      // 🔧 恢复字段映射
      if (keepFieldMapping) {
        fieldMapping.xAxis = savedXAxis
        fieldMapping.yAxis = savedYAxis
      }
      
      // 设置数据源ID（报表关联的数据源）
      if (report.dataSourceId) {
        form.dataSourceId = report.dataSourceId
      }
      
      // 将报表的SQL作为基础（可用于进一步筛选）
      selectedTable.value = `(${report.sqlContent}) AS report_data`
      
      logger.info(`报表 ${selectedReportId.value} 加载了 ${columnOptions.value.length} 个字段`)
      if (columnOptions.value.length === 0) {
        message.warning('该报表没有可用的字段')
      } else {
        message.success(`成功加载 ${columnOptions.value.length} 个字段`)
        // 🔧 只在非保留模式下智能推荐字段
        if (!keepFieldMapping) {
          autoRecommendFields()
        }
      }
    }
  } catch (error) {
    message.error('加载报表字段失败：' + handleApiError(error, '加载报表字段', ''))
    columnOptions.value = []
  }
}

// 智能推荐字段（已禁用自动推荐，用户需要手动选择）
const autoRecommendFields = () => {
  // ❌ 禁用自动推荐功能
  // 用户反馈：新建时不要给默认值，让用户自己选择
  return
  
  /* 保留原有逻辑供参考
  if (columnOptions.value.length === 0) return
  
  const dateFields = columnOptions.value.filter(col => 
    /date|time|创建|更新|日期|时间/i.test(col.label.toLowerCase())
  )
  const numericFields = columnOptions.value.filter(col =>
    /int|decimal|float|double|numeric|金额|数量|价格|销量/i.test(col.dataType.toLowerCase()) ||
    /金额|数量|价格|销量|总额|count|sum/i.test(col.label)
  )
  
  // 如果有日期字段，推荐作为X轴
  if (dateFields.length > 0 && !fieldMapping.xAxis) {
    fieldMapping.xAxis = dateFields[0].value
  }
  
  // 如果有数值字段，推荐作为Y轴
  if (numericFields.length > 0 && fieldMapping.yAxis.length === 0) {
    fieldMapping.yAxis = [numericFields[0].value]
  }
  
  if (fieldMapping.xAxis || fieldMapping.yAxis.length > 0) {
    message.info('已自动推荐字段，您可以修改后预览')
  }
  */
}


// 数据源改变
const handleDataSourceChange = async () => {
  // 重置所有相关状态
  selectedTable.value = null
  tableOptions.value = []
  columnOptions.value = []
  fieldMapping.xAxis = null
  fieldMapping.yAxis = []
  fieldMapping.group = null
  previewData.value = []
  
  if (form.dataSourceId) {
    message.loading('正在加载表列表...')
    await loadTables()
    message.destroyAll()
  }
}

// 字段拖拽开始
const handleFieldDragStart = (event: DragEvent, col: any) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('field', JSON.stringify(col))
  }
}


// 添加筛选条件
const addQueryCondition = () => {
  queryConditions.value.push({
    field: '',
    operator: '=',
    value: ''
  })
}

// 删除筛选条件
const removeQueryCondition = (index: number) => {
  queryConditions.value.splice(index, 1)
}

// 🆕 添加图表参数
const addChartParameter = () => {
  const paramIndex = chartParameters.value.length + 1
  chartParameters.value.push({
    field: '',
    operator: '=',
    name: `param${paramIndex}`,
    label: '',
    type: 'text',
    required: false,
    defaultValue: null,
    datePreset: undefined,
    placeholder: '',
    options: [],
    optionSource: 'manual',
    optionTags: [],
    optionSql: '',
    min: undefined,
    max: undefined
  })
}

// 🆕 SQL模式添加参数（简化版）
const addSqlParameter = () => {
  const paramIndex = chartParameters.value.length + 1
  chartParameters.value.push({
    field: `param${paramIndex}`,
    operator: '=',
    name: `param${paramIndex}`,
    label: '',
    type: 'text',
    required: false,
    defaultValue: '',
    datePreset: undefined,
    placeholder: '',
    options: [],
    optionSource: 'manual',
    optionTags: [],
    optionSql: '',
    min: undefined,
    max: undefined
  })
}

// 🆕 删除图表参数
const removeChartParameter = (index: number) => {
  chartParameters.value.splice(index, 1)
}

// 🆕 参数字段变化处理
const handleParamFieldChange = (param: ChartParameter, fieldName: string) => {
  const field = columnOptions.value.find(c => c.value === fieldName)
  if (field) {
    // 自动设置参数名和显示名称
    param.name = fieldName
    param.label = field.label.replace(/\s*\(.*\)$/, '') // 移除括号中的注释
    
    // 根据字段类型自动设置参数类型
    const dataType = field.dataType.toLowerCase()
    if (dataType.includes('date') || dataType.includes('time')) {
      param.type = 'date'
    } else if (dataType.includes('int') || dataType.includes('decimal') || dataType.includes('float') || dataType.includes('double') || dataType.includes('number')) {
      param.type = 'number'
    } else {
      param.type = 'text'
    }
  }
}


// 生成WHERE子句
const generateWhereClause = () => {
  if (queryConditions.value.length === 0) return ''
  
  const conditions = queryConditions.value
    .filter(c => c.field && c.operator && (c.operator === 'IS NULL' || c.operator === 'IS NOT NULL' || c.value))
    .map(c => {
      if (c.operator === 'IS NULL' || c.operator === 'IS NOT NULL') {
        return `\`${c.field}\` ${c.operator}`
      }
      if (c.operator === 'LIKE' || c.operator === 'NOT LIKE') {
        return `\`${c.field}\` ${c.operator} '%${c.value}%'`
      }
      if (c.operator === 'IN' || c.operator === 'NOT IN') {
        return `\`${c.field}\` ${c.operator} (${c.value})`
      }
      // 判断值是否为数字
      const numValue = Number(c.value)
      if (!isNaN(numValue) && c.value.trim() !== '') {
        return `\`${c.field}\` ${c.operator} ${numValue}`
      }
      return `\`${c.field}\` ${c.operator} '${c.value}'`
    })
  
  return conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : ''
}

// 字段映射改变
const handleFieldMappingChange = () => {
  // 字段映射改变时可以自动生成SQL
}

// Y轴字段变化处理（确保始终是数组格式）
const handleYAxisChange = (value: string | string[] | null) => {
  if (value === null || value === undefined) {
    fieldMapping.yAxis = []
  } else if (Array.isArray(value)) {
    fieldMapping.yAxis = value
  } else {
    // 单选时转换为数组
    fieldMapping.yAxis = [value]
  }
  
  // 为新添加的字段设置默认聚合方式（NONE - 不聚合）
  if (Array.isArray(value) && value.length > 0) {
    value.forEach(field => {
      if (!fieldAggregates.value[field]) {
        fieldAggregates.value[field] = 'NONE'
      }
    })
  }
  
  // 移除已删除字段的聚合配置和别名
  Object.keys(fieldAggregates.value).forEach(field => {
    if (!fieldMapping.yAxis.includes(field)) {
      delete fieldAggregates.value[field]
      delete yAxisLabels.value[field]
    }
  })
}

// 聚合方式变化处理
const handleAggregateChange = (_field: string) => {
  // 聚合方式变化时的处理逻辑（可用于刷新预览等）
}

// 🆕 生成SQL（带筛选条件和数据限制）
const handleGenerateSql = () => {
  if (!selectedTable.value) {
    message.warning('请先选择数据表')
    return
  }
  
  // 表格类型：选择所有字段或指定字段
  if (form.chartType === 'table' || form.chartType === 'summaryTable') {
    let sql = ''
    
    // 如果配置了显示字段，只选择这些字段
    if (tableStyleConfig.displayColumns && tableStyleConfig.displayColumns.length > 0) {
      const selectFields = tableStyleConfig.displayColumns.map(f => `\`${f}\``).join(', ')
      sql = `SELECT ${selectFields}\nFROM \`${selectedTable.value}\``
    } else {
      // 否则选择所有字段
      sql = `SELECT *\nFROM \`${selectedTable.value}\``
    }
    
    // 添加筛选条件
    const staticWhereClause = generateWhereClause()
    if (staticWhereClause) {
      const conditionPart = staticWhereClause.replace(/^WHERE\s+/i, '')
      if (conditionPart) {
        sql += `\nWHERE ${conditionPart}`
      }
    }
    
    // 添加排序
    if (tableStyleConfig.defaultSortField) {
      sql += `\nORDER BY \`${tableStyleConfig.defaultSortField}\` ${tableStyleConfig.defaultSortOrder || 'ASC'}`
    }
    
    // 添加LIMIT
    sql += `\nLIMIT ${dataLimit.value}`
    
    form.sqlContent = sql
    message.success('表格SQL已生成')
    handlePreviewChart()
    return
  }
  
  // 透视表类型：需要选择行、列、值字段
  if (form.chartType === 'pivotTable') {
    const { pivotRowField, pivotColField, pivotValueField } = tableStyleConfig
    
    if (!pivotRowField || !pivotColField || !pivotValueField) {
      message.warning('请配置透视表的行字段、列字段和值字段')
      return
    }
    
    // 透视表SQL：选择行、列、值三个字段
    let sql = `SELECT \`${pivotRowField}\`, \`${pivotColField}\`, \`${pivotValueField}\`\nFROM \`${selectedTable.value}\``
    
    // 添加筛选条件
    const staticWhereClause = generateWhereClause()
    if (staticWhereClause) {
      const conditionPart = staticWhereClause.replace(/^WHERE\s+/i, '')
      if (conditionPart) {
        sql += `\nWHERE ${conditionPart}`
      }
    }
    
    // 添加LIMIT
    sql += `\nLIMIT ${dataLimit.value}`
    
    form.sqlContent = sql
    message.success('透视表SQL已生成')
    handlePreviewChart()
    return
  }
  
  if (!fieldMapping.xAxis) {
    message.warning('请选择维度字段（X轴）')
    return
  }
  
  // 确保 yAxis 是数组格式
  let yAxisArray: string[] = []
  if (Array.isArray(fieldMapping.yAxis)) {
    yAxisArray = fieldMapping.yAxis
  } else if (fieldMapping.yAxis) {
    yAxisArray = [fieldMapping.yAxis as any]
  }
  
  if (yAxisArray.length === 0) {
    message.warning('请至少选择一个指标字段（Y轴）')
    return
  }
  
  // 🔧 验证字段是否存在于可用字段列表中
  const availableFieldNames = columnOptions.value.map(c => c.value.toLowerCase())
  const invalidFields: string[] = []
  
  // 🆕 提取当前SQL中的别名（用于验证时跳过SQL别名字段）
  const sqlAliases = form.sqlContent ? extractSqlAliases(form.sqlContent) : []
  const sqlAliasNames = new Set(sqlAliases.map(a => a.alias.toLowerCase()))
  
  // 验证X轴字段
  if (fieldMapping.xAxis && !availableFieldNames.includes(fieldMapping.xAxis.toLowerCase())) {
    // 🆕 如果字段是SQL别名，跳过验证（允许使用）
    if (sqlAliasNames.has(fieldMapping.xAxis.toLowerCase())) {
      // SQL别名字段，无需验证
    } else {
      // 尝试模糊匹配（忽略大小写）
      const matchedField = columnOptions.value.find(c => 
        c.value.toLowerCase() === fieldMapping.xAxis!.toLowerCase()
      )
      if (matchedField) {
        fieldMapping.xAxis = matchedField.value
      } else {
        invalidFields.push(fieldMapping.xAxis)
      }
    }
  }
  
  // 验证Y轴字段
  yAxisArray = yAxisArray.map(field => {
    if (!availableFieldNames.includes(field.toLowerCase())) {
      // 🆕 如果字段是SQL别名，跳过验证
      if (sqlAliasNames.has(field.toLowerCase())) {
        return field
      }
      const matchedField = columnOptions.value.find(c => 
        c.value.toLowerCase() === field.toLowerCase()
      )
      if (matchedField) {
        return matchedField.value
      } else {
        invalidFields.push(field)
        return field
      }
    }
    return field
  })
  
  // 如果有无效字段，提示用户
  if (invalidFields.length > 0) {
    message.error(`以下字段在数据表中不存在: ${invalidFields.join(', ')}，请重新选择字段`)
    return
  }
  
  // 更新fieldMapping.yAxis为验证后的值
  fieldMapping.yAxis = yAxisArray
  
  // 🔧 生成完整SQL：智能判断是否需要聚合和分组
  const selectParts: string[] = []
  const groupByFields: string[] = []
  
  // 检查是否有任何字段使用了聚合函数
  const hasAggregation = yAxisArray.some(field => {
    const aggFunc = fieldAggregates.value[field]
    return aggFunc && aggFunc !== 'NONE'
  })
  
  // 添加维度字段
  selectParts.push(`\`${fieldMapping.xAxis}\``)
  if (hasAggregation) {
    // 如果有聚合，维度字段用于GROUP BY
    groupByFields.push(`\`${fieldMapping.xAxis}\``)
  }
  
  // 添加指标字段（根据聚合配置）
  yAxisArray.forEach(field => {
    const aggFunc = fieldAggregates.value[field] || 'NONE'
    if (aggFunc === 'NONE') {
      // 不聚合，直接选择字段
      selectParts.push(`\`${field}\``)
    } else {
      // 使用聚合函数
      selectParts.push(`${aggFunc}(\`${field}\`) AS \`${field}\``)
    }
  })
  
  const sql = `SELECT ${selectParts.join(', ')}\nFROM \`${selectedTable.value}\``
  
  // 收集所有WHERE条件
  const whereConditions: string[] = []
  
  // 添加静态筛选条件
  const staticWhereClause = generateWhereClause()
  if (staticWhereClause) {
    // 移除 "WHERE " 前缀，只保留条件部分
    const conditionPart = staticWhereClause.replace(/^WHERE\s+/i, '')
    if (conditionPart) {
      whereConditions.push(conditionPart)
    }
  }
  
  // 组合WHERE子句（不包含参数条件，用于预览）
  let previewSql = sql
  if (whereConditions.length > 0) {
    previewSql += `\nWHERE ${whereConditions.join('\n  AND ')}`
  }
  
  // 只有在有聚合函数时才添加GROUP BY
  if (hasAggregation && groupByFields.length > 0) {
    previewSql += `\nGROUP BY ${groupByFields.join(', ')}`
  }
  
  // 添加排序（根据用户选择的排序方式）
  if (fieldMapping.sortField && fieldMapping.sortOrder) {
    const sortColumn = fieldMapping.sortField === 'x' ? fieldMapping.xAxis : yAxisArray[0]
    if (sortColumn) {
      previewSql += `\nORDER BY \`${sortColumn}\` ${fieldMapping.sortOrder}`
    }
  }
  
  // 添加LIMIT（强制限制）
  previewSql += `\nLIMIT ${dataLimit.value}`
  
  // 参数条件不直接写入 SQL，而是保存在 chartConfig 中
  // 后端在执行查询时会根据参数配置动态构建 WHERE 条件
  
  form.sqlContent = previewSql
  
  // 生成成功提示
  if (hasAggregation) {
    message.success('SQL已生成（带聚合分组）')
  } else {
    message.success('SQL已生成（明细数据）')
  }
  
  // 如果有查询参数，显示参数信息
  if (chartParameters.value.length > 0) {
    const paramInfo = chartParameters.value.map(p => `${p.label || p.field}`).join(', ')
    message.info(`已配置 ${chartParameters.value.length} 个查询参数: ${paramInfo}（参数将在图表查看时生效）`, { duration: 5000 })
  }
  
  // 自动预览
  handlePreviewChart()
}

// 图表类型改变
const handleChartTypeChange = () => {
  // 🔧 SQL模式下保留基本字段映射（X轴和Y轴）
  const savedXAxis = dataConfigMode.value === 'sql' ? fieldMapping.xAxis : null
  const savedYAxis = dataConfigMode.value === 'sql' ? [...fieldMapping.yAxis] : []
  
  // 重置字段映射
  fieldMapping.xAxis = null
  fieldMapping.yAxis = []
  fieldMapping.group = null
  fieldMapping.value = null
  fieldMapping.region = null
  fieldMapping.parent = null
  fieldMapping.name = null
  fieldMapping.source = null
  fieldMapping.target = null
  fieldMapping.open = null
  fieldMapping.close = null
  fieldMapping.low = null
  fieldMapping.high = null
  
  // 🔧 SQL模式下恢复基本字段映射
  if (dataConfigMode.value === 'sql') {
    fieldMapping.xAxis = savedXAxis
    fieldMapping.yAxis = savedYAxis
  }
  
  // 清空聚合配置和Y轴别名（非SQL模式）
  if (dataConfigMode.value !== 'sql') {
    fieldAggregates.value = {}
    yAxisLabels.value = {}
  }
  
  // 🔧 如果切换到非表格类型，重置表格配置
  if (form.chartType !== 'table') {
    tableStyleConfig.displayColumns = []
    tableStyleConfig.columnLabels = {}
    tableStyleConfig.defaultSortField = ''
    tableStyleConfig.defaultSortOrder = 'ASC'
  }
  
  // 重新生成图表配置
  generateChartConfig()
}

// 生成图表配置
const generateChartConfig = () => {
  if (!form.chartType) return
  const visualSize = getChartVisualSize()
  
  const option: any = {
    backgroundColor: styleConfig.backgroundColor || '#ffffff',  // 背景颜色
    tooltip: {
      trigger: form.chartType === 'pie' ? 'item' : 'axis',
      show: interactionConfig.enableTooltip
    },
    animation: styleConfig.animation,
    animationDuration: styleConfig.animationDuration
  }
  
  // 标题配置
  if (styleConfig.title) {
    // 根据背景色自动计算标题颜色
    const isDark = isDarkColor(styleConfig.backgroundColor || '#ffffff')
    const titleColor = isDark ? '#ffffff' : '#333333'
    option.title = {
      text: styleConfig.title,
      left: styleConfig.titlePosition || 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold',
        color: titleColor
      }
    }
  }
  
  // 图例配置
  if (styleConfig.showLegend) {
    // 🔧 修复：当 legend 在顶部时，设置具体数值避免与标题重叠
    const legendTop = styleConfig.legendPosition === 'top' ? 35 :
                      styleConfig.legendPosition === 'bottom' ? 'bottom' : 'auto'
    option.legend = {
      show: true,
      orient: 'horizontal',
      left: styleConfig.legendPosition === 'left' ? 'left' : 
            styleConfig.legendPosition === 'right' ? 'right' :
            styleConfig.legendPosition === 'bottom' ? 'center' : 'center',
      top: legendTop,
      selectedMode: interactionConfig.enableLegendSelect ? 'multiple' : false
    }
  }
  
  // 工具箱配置
  if (interactionConfig.enableToolbox) {
    option.toolbox = {
      show: true,
      right: 10,  // 🔧 固定在右侧
      top: 5,     // 🔧 固定在顶部
      feature: {
        saveAsImage: {
          show: interactionConfig.toolboxFeatures.includes('saveAsImage'),
          title: '保存为图片'
        },
        dataView: {
          show: interactionConfig.toolboxFeatures.includes('dataView'),
          title: '数据视图',
          readOnly: false
        },
        dataZoom: {
          show: interactionConfig.toolboxFeatures.includes('dataZoom'),
          title: {
            zoom: '区域缩放',
            back: '区域缩放还原'
          }
        },
        restore: {
          show: interactionConfig.toolboxFeatures.includes('restore'),
          title: '还原'
        }
      }
    }
    
    // 🔧 如果有图例，调整图例位置避免与工具箱重叠
    if (option.legend) {
      option.legend.right = 100
    }
  }
  
  // 数据缩放配置
  if (interactionConfig.enableDataZoom && (form.chartType === 'line' || form.chartType === 'bar')) {
    option.dataZoom = [
      {
        type: 'slider',
        show: true,
        xAxisIndex: [0],
        start: 0,
        end: 100
      },
      {
        type: 'inside',
        xAxisIndex: [0],
        start: 0,
        end: 100
      }
    ]
  }
  
  // 图表类型特定配置
  if (form.chartType === 'table' || form.chartType === 'summaryTable' || form.chartType === 'pivotTable') {
    // 表格类型：保存表格配置
    const tableConfig = {
      _isTable: true,
      _tableType: form.chartType,  // 区分普通表格、汇总表、透视表
      tableStyle: {
        striped: tableStyleConfig.striped,
        size: tableStyleConfig.size,
        showPagination: tableStyleConfig.showPagination,
        pageSize: tableStyleConfig.pageSize,
        showIndex: tableStyleConfig.showIndex,
        bordered: tableStyleConfig.bordered,
        displayColumns: tableStyleConfig.displayColumns,
        columnLabels: tableStyleConfig.columnLabels,
        defaultSortField: tableStyleConfig.defaultSortField,
        defaultSortOrder: tableStyleConfig.defaultSortOrder,
        enableExport: tableStyleConfig.enableExport,
        exportFileName: tableStyleConfig.exportFileName,
        // 表头样式
        headerBgColor: tableStyleConfig.headerBgColor,
        headerTextColor: tableStyleConfig.headerTextColor,
        headerFontWeight: tableStyleConfig.headerFontWeight,
        headerAlign: tableStyleConfig.headerAlign,
        // 汇总表配置
        showSummary: tableStyleConfig.showSummary,
        summaryColumns: tableStyleConfig.summaryColumns,
        summaryTypes: tableStyleConfig.summaryTypes,
        summaryLabel: tableStyleConfig.summaryLabel,
        // 透视表配置
        pivotRowField: tableStyleConfig.pivotRowField,
        pivotColField: tableStyleConfig.pivotColField,
        pivotValueField: tableStyleConfig.pivotValueField,
        pivotAggType: tableStyleConfig.pivotAggType,
        pivotShowRowTotal: tableStyleConfig.pivotShowRowTotal,
        pivotShowColTotal: tableStyleConfig.pivotShowColTotal,
        // 条件格式化
        enableConditionalFormat: tableStyleConfig.enableConditionalFormat,
        conditionalFormatField: tableStyleConfig.conditionalFormatField,
        conditionalFormatType: tableStyleConfig.conditionalFormatType,
        conditionalFormatColors: tableStyleConfig.conditionalFormatColors
      }
    }
    
    // 保存配置 - 包含完整的 metadata
    const configObj = {
      echarts: tableConfig,
      metadata: {
        chartType: form.chartType,
        sourceType: sourceType.value,
        dataConfigMode: dataConfigMode.value,
        selectedTable: selectedTable.value,
        selectedReportId: selectedReportId.value,
        dataLimit: dataLimit.value,
        queryConditions: queryConditions.value,
        chartParameters: chartParameters.value,
        visualSize,
        fieldMapping: {
          xAxis: fieldMapping.xAxis,
          yAxis: fieldMapping.yAxis
        }
      },
      queryParameters: chartParameters.value
    }
    chartConfigJson.value = JSON.stringify(configObj, null, 2)
    form.chartConfig = chartConfigJson.value
    return
  } else if (form.chartType === 'pie') {
    // 饼图使用颜色方案
    const pieColors = getColorArray(styleConfig.colorScheme, 8)
    option.color = pieColors
    option.series = [{
      type: 'pie',
      radius: '60%',
      data: [],
      itemStyle: {
        color: (params: any) => {
          return pieColors[params.dataIndex % pieColors.length]
        }
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      label: {
        show: styleConfig.showLabel
      }
    }]
  } else if (form.chartType === 'bar' || form.chartType === 'line') {
    // 柱状图和折线图使用颜色方案
    const chartColors = getColorArray(styleConfig.colorScheme, 8)
    option.color = chartColors
    // 🔧 添加 grid 配置，确保图表内容不与标题/图例重叠
    option.grid = {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 60, // 留出足够空间给标题和图例
      containLabel: true
    }
    option.xAxis = {
      type: 'category',
      data: [],
      axisLine: {
        show: true
      },
      axisTick: {
        show: true
      },
      splitLine: {
        show: styleConfig.showGrid
      }
    }
    option.yAxis = {
      type: 'value',
      axisLine: {
        show: true
      },
      axisTick: {
        show: true
      },
      splitLine: {
        show: styleConfig.showGrid
      }
    }
    // 🔧 支持多系列（多折线/多柱状图），保存折线图配置
    option.series = [{
      type: form.chartType,
      data: [],
      // 折线图特殊配置
      ...(form.chartType === 'line' ? {
        smooth: styleConfig.smooth === true,
        symbol: styleConfig.showSymbol !== false ? 'circle' : 'none',
        symbolSize: styleConfig.symbolSize || 4
      } : {}),
      // 柱状图特殊配置
      ...(form.chartType === 'bar' ? {
        barWidth: styleConfig.barWidth || 'auto',
        barMaxWidth: styleConfig.barMaxWidth || undefined
      } : {}),
      label: {
        show: styleConfig.showLabel
      }
    }]
  } else if (form.chartType === 'scatter') {
    const scatterColors = getColorArray(styleConfig.colorScheme, 8)
    option.color = scatterColors
    option.xAxis = {
      type: 'value',
      scale: interactionConfig.enableZoom
    }
    option.yAxis = {
      type: 'value',
      scale: interactionConfig.enableZoom
    }
    option.series = [{
      type: 'scatter',
      data: [],
      symbolSize: 10,
      itemStyle: {
        color: scatterColors[0] || styleConfig.color
      }
    }]
  } else if (form.chartType === 'radar') {
    const radarColors = getColorArray(styleConfig.colorScheme, 8)
    option.color = radarColors
    option.radar = {
      indicator: []
    }
    option.series = [{
      type: 'radar',
      data: [],
      itemStyle: {
        color: radarColors[0] || styleConfig.color
      }
    }]
  }
  
  // 🔧 保存完整配置（包含ECharts配置和元数据）
  const fullConfig = {
    echarts: option,  // ECharts配置
    metadata: {  // 额外的元数据，用于编辑时恢复
      sourceType: sourceType.value,  // 数据源类型
      dataConfigMode: dataConfigMode.value,  // 🆕 数据配置模式
      selectedReportId: selectedReportId.value,  // 选中的报表ID
      fieldMapping: {
        xAxis: fieldMapping.xAxis,
        yAxis: fieldMapping.yAxis
      },
      queryConditions: queryConditions.value,
      fieldAggregates: fieldAggregates.value,
      yAxisLabels: yAxisLabels.value,  // 🆕 Y轴指标别名
      selectedTable: selectedTable.value,
      dataLimit: dataLimit.value,
      // 🆕 保存查询参数配置
      chartParameters: chartParameters.value,
      visualSize
    }
  }
  
  chartConfigJson.value = JSON.stringify(fullConfig, null, 2)
  form.chartConfig = chartConfigJson.value
}

// 🆕 SQL输入框失焦时，自动解析字段映射和参数
const handleSqlBlur = () => {
  if (!form.sqlContent) return
  
  // 尝试从SQL解析字段映射
  const extracted = extractFieldMappingFromSql(form.sqlContent, form.chartType || 'bar')
  
  // 只有当字段映射为空时才自动填充
  if (!fieldMapping.xAxis && extracted.xAxis) {
    fieldMapping.xAxis = extracted.xAxis
  }
  if (fieldMapping.yAxis.length === 0 && extracted.yAxis.length > 0) {
    fieldMapping.yAxis = extracted.yAxis
  }
  
  // 🆕 自动解析SQL中的参数占位符 ${paramName}
  const paramRegex = /\$\{(\w+)\}/g
  const foundParams: string[] = []
  let match
  while ((match = paramRegex.exec(form.sqlContent)) !== null) {
    const paramName = match[1]
    if (!foundParams.includes(paramName)) {
      foundParams.push(paramName)
    }
  }
  
  // 添加尚未存在的参数
  for (const paramName of foundParams) {
    const exists = chartParameters.value.some(p => p.name === paramName)
    if (!exists) {
      chartParameters.value.push({
        field: paramName,
        operator: '=',
        name: paramName,
        label: paramName,
        type: 'text',
        required: false,
        defaultValue: '',
        datePreset: undefined,
        placeholder: '',
        options: [],
        optionSource: 'manual',
        optionTags: [],
        optionSql: '',
        min: undefined,
        max: undefined
      })
    }
  }
  
  // 如果有发现参数，提示用户
  if (foundParams.length > 0) {
    message.info(`已识别 ${foundParams.length} 个SQL参数: ${foundParams.join(', ')}`)
  }
}

// 预览图表
const handlePreviewChart = async () => {
  if (!form.dataSourceId) {
    message.warning('请先选择数据源')
    return
  }
  
  if (!form.sqlContent || !form.sqlContent.trim()) {
    message.warning('请先生成SQL')
    return
  }
  
  previewLoading.value = true
  try {
    // 🔧 处理SQL中的参数占位符
    let processedSql = form.sqlContent.trim()
    
    // 替换已定义的参数
    for (const param of chartParameters.value) {
      const placeholder = '${' + param.name + '}'
      const value = param.defaultValue || ''
      
      const strValue = value ? String(value) : ''
      
      if (!strValue.trim()) {
        // 参数为空时，移除包含该参数的条件
        // 模式: AND (${param} IS NULL OR field = ${param})
        processedSql = processedSql.replace(new RegExp(`\\s+AND\\s*\\(\\s*\\$\\{${param.name}\\}\\s+IS\\s+NULL\\s+OR\\s+[\\w.\`]+\\s*=\\s*'?\\$\\{${param.name}\\}'?\\s*\\)`, 'gi'), '')
        // 模式: AND field = ${param}
        processedSql = processedSql.replace(new RegExp(`\\s+AND\\s+[\\w.\`]+\\s*[=<>!]+\\s*'?\\$\\{${param.name}\\}'?`, 'gi'), '')
        processedSql = processedSql.replace(new RegExp(`\\s+AND\\s+[\\w.\`]+\\s+IN\\s*\\([^)]*\\$\\{${param.name}\\}[^)]*\\)`, 'gi'), '')
      } else {
        // 有值时替换
        processedSql = processedSql.split(placeholder).join(strValue)
      }
    }
    
    // 移除任何残留的未定义参数条件
    processedSql = processedSql.replace(/\s+AND\s*\(\s*\$\{[^}]+\}\s+IS\s+NULL\s+OR\s+[\w.`]+\s*=\s*'?\$\{[^}]+\}'?\s*\)/gi, '')
    processedSql = processedSql.replace(/\s+AND\s+[\w.`]+\s*[=<>!]+\s*'?\$\{[^}]+\}'?/gi, '')
    processedSql = processedSql.replace(/\s+AND\s+[\w.`]+\s+IN\s*\([^)]*\$\{[^}]+\}[^)]*\)/gi, '')
    
    // 清理语法
    processedSql = processedSql.replace(/WHERE\s+AND\s+/gi, 'WHERE ')
    processedSql = processedSql.replace(/WHERE\s+OR\s+/gi, 'WHERE ')
    processedSql = processedSql.replace(/WHERE\s+$/gi, '')
    processedSql = processedSql.replace(/WHERE\s+(ORDER|GROUP|LIMIT)/gi, '$1')
    
    const testRes = await testChartSql({
      dataSourceId: form.dataSourceId,
      sqlContent: processedSql
    })
    const rawData = ((testRes?.data as any) || []) as any[]
    // 添加全局索引用于分页时显示正确序号
    previewData.value = rawData.map((row, index) => ({
      ...row,
      _globalIndex: index + 1
    }))
    
    // 生成列配置
    if (previewData.value.length > 0) {
      const keys = Object.keys(rawData[0] || {})
      dataPreviewColumns.value = keys.map(key => ({
        title: key,
        key: key
      }))
    } else {
      dataPreviewColumns.value = []
      message.warning('查询结果为空，请检查SQL语句')
    }
    
    // 渲染图表
    await nextTick()
    renderChart()
  } catch (error) {
    handleApiError(error, '预览图表')
    previewData.value = []
    dataPreviewColumns.value = []
  } finally {
    previewLoading.value = false
  }
}

// 渲染图表
const renderChart = async () => {
  // 表格类型不需要渲染 ECharts
  if (['table', 'summaryTable', 'pivotTable'].includes(form.chartType || '')) {
    return
  }
  
  if (!chartPreviewRef.value) return
  
  // 等待 DOM 更新（确保 inline style 的 width/height 已生效）
  await nextTick()
  
  try {
    if (!chartInstance) {
      chartInstance = echarts.init(chartPreviewRef.value)
      
      // 监听窗口大小变化
      resizeHandler = () => {
        chartInstance?.resize()
      }
      window.addEventListener('resize', resizeHandler)
      
      // 监听点击事件
      if (interactionConfig.enableClick) {
        chartInstance.on('click', (params: any) => {
          message.info(`点击了: ${params.name || params.seriesName}`)
        })
      }
    } else {
      // 容器尺寸变化后同步 resize
      chartInstance.resize()
    }
    
    let option: any = {}
    if (chartConfigJson.value) {
      try {
        const parsed = JSON.parse(chartConfigJson.value)
        // 🔧 修复：提取 echarts 配置，兼容新旧格式
        option = parsed.echarts || parsed
      } catch (e) {
        console.error('解析配置失败', e)
        message.error('图表配置JSON格式错误')
        return
      }
    } else {
      // 如果没有配置，生成默认配置
      generateChartConfig()
      const parsed = JSON.parse(chartConfigJson.value)
      option = parsed.echarts || parsed
    }
    
    // 🔧 根据背景色自动调整文字颜色
    const isDarkBackground = isDarkColor(styleConfig.backgroundColor || '#ffffff')
    const textColor = isDarkBackground ? '#ffffff' : '#333333'
    const subTextColor = isDarkBackground ? '#cccccc' : '#666666'
    const axisLineColor = isDarkBackground ? '#555555' : '#cccccc'
    
    // 如果没有数据，显示空状态
    if (previewData.value.length === 0) {
      option.graphic = [{
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无数据',
          fontSize: 16,
          fill: subTextColor
        }
      }]
      chartInstance.setOption(option, true)
      return
    }
    
    // 🔧 调整标题颜色
    if (option.title) {
      option.title.textStyle = {
        ...option.title.textStyle,
        color: textColor
      }
    }
    
    // 🔧 调整图例颜色
    if (option.legend) {
      option.legend.textStyle = {
        color: textColor
      }
    }
    
    // 🔧 调整工具箱位置，避免与图例重叠
    if (option.toolbox) {
      option.toolbox.right = 10
      option.toolbox.top = 5
      option.toolbox.iconStyle = {
        borderColor: textColor
      }
    }
    
    // 🔧 调整坐标轴颜色
    if (option.xAxis) {
      option.xAxis.axisLabel = {
        ...option.xAxis.axisLabel,
        color: subTextColor
      }
      option.xAxis.axisLine = {
        ...option.xAxis.axisLine,
        lineStyle: { color: axisLineColor }
      }
      option.xAxis.splitLine = {
        ...option.xAxis.splitLine,
        lineStyle: { color: axisLineColor }
      }
    }
    if (option.yAxis) {
      option.yAxis.axisLabel = {
        ...option.yAxis.axisLabel,
        color: subTextColor
      }
      option.yAxis.axisLine = {
        ...option.yAxis.axisLine,
        lineStyle: { color: axisLineColor }
      }
      option.yAxis.splitLine = {
        ...option.yAxis.splitLine,
        lineStyle: { color: axisLineColor }
      }
    }
    
    // 确保 yAxis 是数组
    const yAxisArray = Array.isArray(fieldMapping.yAxis) ? fieldMapping.yAxis : []
    
    // 填充数据
    if (form.chartType === 'pie') {
      const xField = fieldMapping.xAxis
      const yField = yAxisArray[0]
      if (xField && yField) {
        // 确保 series 存在
        if (!option.series || !Array.isArray(option.series) || option.series.length === 0) {
          option.series = [{ type: 'pie', data: [] }]
        }
        const pieColors = getColorArray(styleConfig.colorScheme, previewData.value.length)
        option.color = pieColors
        option.series[0].data = previewData.value.map((item, index) => ({
          value: item[yField] || 0,
          name: String(item[xField] || ''),
          itemStyle: {
            color: pieColors[index % pieColors.length]
          }
        }))
      }
    } else if (form.chartType === 'scatter') {
      const xField = fieldMapping.xAxis
      const yField = yAxisArray[0]
      if (xField && yField) {
        // 确保 xAxis 和 yAxis 存在且配置完整
        if (!option.xAxis) {
          option.xAxis = {
            type: 'value',
            scale: interactionConfig.enableZoom || false,
            axisLine: { show: true },
            axisTick: { show: true },
            splitLine: { show: styleConfig.showGrid }
          }
        } else {
          // 确保 scale 属性存在
          if (option.xAxis.scale === undefined) {
            option.xAxis.scale = interactionConfig.enableZoom || false
          }
        }
        
        if (!option.yAxis) {
          option.yAxis = {
            type: 'value',
            scale: interactionConfig.enableZoom || false,
            axisLine: { show: true },
            axisTick: { show: true },
            splitLine: { show: styleConfig.showGrid }
          }
        } else {
          // 确保 scale 属性存在
          if (option.yAxis.scale === undefined) {
            option.yAxis.scale = interactionConfig.enableZoom || false
          }
        }
        
        // 确保 series 存在
        if (!option.series || !Array.isArray(option.series) || option.series.length === 0) {
          option.series = [{ type: 'scatter', data: [] }]
        }
        const scatterColors = getColorArray(styleConfig.colorScheme, 8)
        option.color = scatterColors
        option.series[0].data = previewData.value.map(item => [
          Number(item[xField]) || 0,
          Number(item[yField]) || 0
        ])
      }
    } else if (form.chartType === 'radar') {
      // 雷达图需要特殊处理
      if (yAxisArray.length === 0) return
      
      // 确保 radar 和 series 存在
      if (!option.radar) {
        option.radar = { indicator: [] }
      }
      if (!option.series || !Array.isArray(option.series) || option.series.length === 0) {
        option.series = [{ type: 'radar', data: [] }]
      }
      
      const radarColors = getColorArray(styleConfig.colorScheme, 8)
      option.color = radarColors
      // 🆕 使用别名作为指标名称
      const indicators = yAxisArray.map(field => ({
        name: yAxisLabels.value[field] || field,
        max: Math.max(...previewData.value.map(item => Number(item[field]) || 0), 100)
      }))
      option.radar.indicator = indicators
      option.series[0].data = [{
        value: yAxisArray.map(field => 
          previewData.value.reduce((sum, item) => sum + (Number(item[field]) || 0), 0) / previewData.value.length
        ),
        name: '平均值',
        itemStyle: {
          color: radarColors[0] || styleConfig.color
        }
      }]
    } else {
      const xField = fieldMapping.xAxis
      const yFields = yAxisArray
      
      // 确保 xAxis 存在且配置完整
      if (!option.xAxis) {
        option.xAxis = {
          type: 'category',
          data: [],
          axisLine: { show: true },
          axisTick: { show: true },
          splitLine: { show: styleConfig.showGrid }
        }
      } else {
        if (!option.xAxis.data) {
          option.xAxis.data = []
        }
        // 确保必要的属性存在
        if (!option.xAxis.type) {
          option.xAxis.type = 'category'
        }
      }
      
      // 确保 yAxis 存在且配置完整
      if (!option.yAxis) {
        option.yAxis = {
          type: 'value',
          axisLine: { show: true },
          axisTick: { show: true },
          splitLine: { show: styleConfig.showGrid }
        }
      } else {
        // 确保必要的属性存在
        if (!option.yAxis.type) {
          option.yAxis.type = 'value'
        }
        if (option.yAxis.scale === undefined) {
          option.yAxis.scale = false
        }
      }
      
      if (xField) {
        option.xAxis.data = previewData.value.map(item => String(item[xField] || ''))
      }
      
      if (yFields.length > 0) {
        // 使用颜色方案
        const colors = getColorArray(styleConfig.colorScheme, yFields.length)
        option.color = colors // 设置全局颜色方案
        
        // 如果有分组字段，按分组创建系列
        if (fieldMapping.group) {
          const groupField = fieldMapping.group
          const groups = [...new Set(previewData.value.map(item => String(item[groupField] || '')))]
          
          option.series = groups.map((group, groupIndex) => {
            const seriesConfig: any = {
              type: form.chartType,
              name: group,
              data: previewData.value
                .filter(item => String(item[groupField] || '') === group)
                .map(item => Number(item[yFields[0]]) || 0),
              itemStyle: {
                color: colors[groupIndex % colors.length] || styleConfig.color
              },
              label: {
                show: styleConfig.showLabel
              },
              emphasis: {
                focus: interactionConfig.enableHover ? 'series' : 'none'
              }
            }
            
            // 折线图特殊配置
            if (form.chartType === 'line') {
              seriesConfig.smooth = styleConfig.smooth === true
              seriesConfig.symbol = styleConfig.showSymbol !== false ? 'circle' : 'none'
              seriesConfig.symbolSize = styleConfig.symbolSize || 4
            }
            
            // 柱状图特殊配置
            if (form.chartType === 'bar') {
              if (styleConfig.barWidth && styleConfig.barWidth !== 'auto') {
                const barWidthNum = typeof styleConfig.barWidth === 'number' 
                  ? styleConfig.barWidth 
                  : Number(styleConfig.barWidth)
                if (!isNaN(barWidthNum)) {
                  seriesConfig.barWidth = barWidthNum
                } else {
                  seriesConfig.barWidth = 'auto'
                }
              } else {
                seriesConfig.barWidth = 'auto'
              }
              if (styleConfig.barMaxWidth) {
                seriesConfig.barMaxWidth = styleConfig.barMaxWidth
              }
            }
            
            return seriesConfig
          })
        } else {
          // 多Y轴字段，每个字段一个系列（支持多折线/多柱状图）
          option.series = yFields.map((yField, index) => {
            // 🆕 使用别名，如果没有别名则使用原字段名
            const seriesName = yAxisLabels.value[yField] || yField
            
            const seriesConfig: any = {
              type: form.chartType,
              name: seriesName,
              data: previewData.value.map(item => Number(item[yField]) || 0),
              itemStyle: {
                color: colors[index % colors.length] || styleConfig.color
              },
              label: {
                show: styleConfig.showLabel
              },
              emphasis: {
                focus: interactionConfig.enableHover ? 'series' : 'none'
              }
            }
            
            // 折线图特殊配置
            if (form.chartType === 'line') {
              seriesConfig.smooth = styleConfig.smooth === true
              seriesConfig.symbol = styleConfig.showSymbol !== false ? 'circle' : 'none'
              seriesConfig.symbolSize = styleConfig.symbolSize || 4
            }
            
            // 柱状图特殊配置
            if (form.chartType === 'bar') {
              if (styleConfig.barWidth && styleConfig.barWidth !== 'auto') {
                const barWidthNum = typeof styleConfig.barWidth === 'number' 
                  ? styleConfig.barWidth 
                  : Number(styleConfig.barWidth)
                if (!isNaN(barWidthNum)) {
                  seriesConfig.barWidth = barWidthNum
                } else {
                  seriesConfig.barWidth = 'auto'
                }
              } else {
                seriesConfig.barWidth = 'auto'
              }
              if (styleConfig.barMaxWidth) {
                seriesConfig.barMaxWidth = styleConfig.barMaxWidth
              }
            }
            
            return seriesConfig
          })
        }
      }
    }
    
    // 🔧 确保背景颜色生效
    if (styleConfig.backgroundColor) {
      option.backgroundColor = styleConfig.backgroundColor
    }
    
    // 应用尺寸配置
    if (styleConfig.width) {
      option.width = styleConfig.width
    }
    if (styleConfig.height) {
      option.height = styleConfig.height
    }
    
    chartInstance.setOption(option, true)
  } catch (error) {
    console.error('渲染图表失败', error)
    message.error('渲染图表失败: ' + (error as Error).message)
  }
}

// 判断是否为深色背景
const isDarkColor = (color: string): boolean => {
  const hex = color.replace('#', '')
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000
  return brightness < 128
}

// 获取颜色数组（兼容旧代码）
const getColorArray = (schemeName: string, count: number): string[] => {
  // 如果是自定义颜色
  if (schemeName === 'custom') {
    return [styleConfig.color || '#18a058']
  }
  
  // 使用导入的getColorScheme获取颜色方案
  const scheme = getColorScheme(schemeName)
  const colors = scheme.colors
  
  // 循环填充到指定数量
  const result: string[] = []
  for (let i = 0; i < count; i++) {
    result.push(colors[i % colors.length])
  }
  return result
}

// 预览菜单选项
const previewMenuOptions = [
  { label: '刷新预览', key: 'refresh', icon: () => h(NIcon, null, { default: () => h(SyncOutline) }) },
  { label: '保存为图片', key: 'saveImage', icon: () => h(NIcon, null, { default: () => h(ImageOutline) }) },
  { label: '全屏预览', key: 'fullscreen', icon: () => h(NIcon, null, { default: () => h(ExpandOutline) }) },
]

// 处理预览菜单
const handlePreviewMenu = (key: string) => {
  switch (key) {
    case 'refresh':
      handleRefreshPreview()
      break
    case 'saveImage':
      handleSaveChartImage()
      break
    case 'fullscreen':
      handleFullscreenPreview()
      break
  }
}

// 保存图表为图片
const handleSaveChartImage = () => {
  if (!chartInstance) {
    message.warning('请先预览图表')
    return
  }
  const chartOption = chartInstance.getOption() as any
  const rawBg = chartOption?.backgroundColor
  const bgColor = (rawBg && rawBg !== 'transparent') ? rawBg : '#fff'
  const url = chartInstance.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: bgColor })
  const link = document.createElement('a')
  link.download = `${form.chartName || '图表'}.png`
  link.href = url
  link.click()
  message.success('图片已保存')
}

// 全屏预览
const handleFullscreenPreview = () => {
  if (!chartInstance) {
    message.warning('请先预览图表')
    return
  }
  // 使用浏览器全屏 API
  const previewArea = document.querySelector('.chart-preview-area') as HTMLElement
  if (previewArea) {
    if (previewArea.requestFullscreen) {
      previewArea.requestFullscreen()
    }
  }
}

// 刷新预览
const handleRefreshPreview = () => {
  if (!form.dataSourceId) {
    message.warning('请先选择数据源')
    return
  }
  
  if (!form.sqlContent || !form.sqlContent.trim()) {
    message.warning('请先生成SQL')
    return
  }
  
  // 强制重新渲染
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  
  handlePreviewChart()
}

// 导出预览数据
const handleExportPreviewData = () => {
  if (previewData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  
  try {
    const filename = `${form.chartName || 'chart'}_preview`
    exportToExcel(previewData.value, filename, 100000, (warning) => {
      message.warning(warning)
    })
    message.success('导出成功')
  } catch (error) {
    handleApiError(error, '导出数据')
    message.error('导出失败')
  }
}

// 导出表格数据（Excel）
const handleExportTableData = () => {
  if (previewData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  
  try {
    // 使用配置的文件名或图表名称
    const filename = tableStyleConfig.exportFileName || form.chartName || '数据表格'
    
    // 如果配置了显示字段，只导出这些字段
    let exportData = previewData.value
    if (tableStyleConfig.displayColumns && tableStyleConfig.displayColumns.length > 0) {
      exportData = previewData.value.map(row => {
        const newRow: Record<string, any> = {}
        tableStyleConfig.displayColumns.forEach(col => {
          // 使用别名作为列标题
          const label = tableStyleConfig.columnLabels[col] || col
          newRow[label] = row[col]
        })
        return newRow
      })
    }
    
    exportToExcel(exportData, filename, 100000, (warning) => {
      message.warning(warning)
    })
    message.success(`导出成功：${filename}.xlsx`)
  } catch (error) {
    handleApiError(error, '导出数据')
    message.error('导出失败')
  }
}

// 加载图表定义
const loadChartDefinition = async (id: number) => {
  try {
    const res = await getChartDefinitionById(id)
    const chart = (res?.data as any) as ChartDefinition
    
    // 🔧 检查图表是否存在
    if (!chart || !chart.id) {
      message.error('图表不存在或已被删除')
      // 替换当前标签页返回图表管理
      tabsStore.replaceTab(route.fullPath, {
        key: '/chart-manage',
        title: '图表管理',
        closable: true
      })
      router.replace('/chart-manage')
      return
    }
    
    form.id = chart.id
    form.chartName = chart.chartName
    form.chartCode = chart.chartCode
    form.chartType = chart.chartType
    form.dataSourceId = chart.dataSourceId
    form.sqlContent = chart.sqlContent
    form.description = chart.description || ''
    form.status = chart.status
    form.chartConfig = chart.chartConfig || ''
    
    let config: any = null
    if (chart.chartConfig) {
      chartConfigJson.value = chart.chartConfig
      try {
        config = JSON.parse(chart.chartConfig)
        
        // 🔧 恢复元数据（优先使用新格式）
        if (config.metadata) {
          // 恢复数据源类型（默认为database，兼容旧数据）
          sourceType.value = config.metadata.sourceType || 'database'
          
          // 🆕 恢复数据配置模式（字段模式/SQL模式）
          // AI生成的图表默认使用SQL模式
          if (config.metadata.aiGenerated) {
            dataConfigMode.value = config.metadata.dataConfigMode || 'sql'
          } else {
            dataConfigMode.value = config.metadata.dataConfigMode || 'field'
          }
          
          // 恢复报表ID
          if (config.metadata.selectedReportId) {
            selectedReportId.value = config.metadata.selectedReportId
          }
          // 恢复字段映射
          if (config.metadata.fieldMapping) {
            fieldMapping.xAxis = config.metadata.fieldMapping.xAxis || null
            fieldMapping.yAxis = config.metadata.fieldMapping.yAxis || []
          }
          // 恢复筛选条件
          if (config.metadata.queryConditions) {
            queryConditions.value = config.metadata.queryConditions
          }
          // 恢复聚合配置
          if (config.metadata.fieldAggregates) {
            fieldAggregates.value = config.metadata.fieldAggregates
          }
          // 🆕 恢复Y轴指标别名
          if (config.metadata.yAxisLabels) {
            yAxisLabels.value = config.metadata.yAxisLabels
          }
          // 恢复选中的表
          if (config.metadata.selectedTable) {
            selectedTable.value = config.metadata.selectedTable
          }
          // 恢复数据限制
          if (config.metadata.dataLimit) {
            dataLimit.value = config.metadata.dataLimit
          }
          // 🆕 恢复查询参数配置
          if (config.metadata.chartParameters) {
            chartParameters.value = config.metadata.chartParameters
          }
        }
        
        // 🆕 从 queryParameters 恢复查询参数（新格式）
        if (config.queryParameters && config.queryParameters.length > 0) {
          chartParameters.value = config.queryParameters
        } else {
          // 🔧 没有metadata的旧数据，默认为database模式
          sourceType.value = 'database'
        }
        
        // 获取ECharts配置（兼容新旧格式）
        const echartsConfig = config.echarts || config
        applyVisualSizeFromConfig(config, echartsConfig)
        
        // 解析标题配置
        if (echartsConfig.title) {
          styleConfig.title = echartsConfig.title.text || ''
          styleConfig.titlePosition = echartsConfig.title.left || 'center'
        }
        
        // 解析图例配置
        if (echartsConfig.legend) {
          styleConfig.showLegend = echartsConfig.legend.show !== false
          if (echartsConfig.legend.top) {
            styleConfig.legendPosition = echartsConfig.legend.top === 'top' ? 'top' :
                                        echartsConfig.legend.top === 'bottom' ? 'bottom' :
                                        echartsConfig.legend.left === 'left' ? 'left' : 'right'
          }
        }
        
        // 解析动画配置
        if (echartsConfig.animation !== undefined) {
          styleConfig.animation = echartsConfig.animation
        }
        if (echartsConfig.animationDuration) {
          styleConfig.animationDuration = echartsConfig.animationDuration
        }
        
        // 解析背景色和主题色
        if (echartsConfig.backgroundColor) {
          styleConfig.backgroundColor = echartsConfig.backgroundColor
        }
        if (echartsConfig.color && Array.isArray(echartsConfig.color)) {
          // 根据颜色数组推断主题
          const colorStr = JSON.stringify(echartsConfig.color)
          if (colorStr.includes('#5470c6')) styleConfig.colorScheme = 'default'
          else if (colorStr.includes('#ee6666')) styleConfig.colorScheme = 'warm'
          else if (colorStr.includes('#73c0de')) styleConfig.colorScheme = 'cool'
          else if (colorStr.includes('#91cc75')) styleConfig.colorScheme = 'green'
          else if (colorStr.includes('#fac858')) styleConfig.colorScheme = 'yellow'
        }
        
        // 解析交互配置
        if (echartsConfig.toolbox) {
          interactionConfig.enableToolbox = echartsConfig.toolbox.show !== false
          if (echartsConfig.toolbox.feature) {
            const features: string[] = []
            if (echartsConfig.toolbox.feature.saveAsImage?.show) features.push('saveAsImage')
            if (echartsConfig.toolbox.feature.dataView?.show) features.push('dataView')
            if (echartsConfig.toolbox.feature.dataZoom?.show) features.push('dataZoom')
            if (echartsConfig.toolbox.feature.restore?.show) features.push('restore')
            interactionConfig.toolboxFeatures = features
          }
        }
        
        if (echartsConfig.dataZoom) {
          interactionConfig.enableDataZoom = true
          interactionConfig.enableZoom = true
        }
        
        if (echartsConfig.tooltip) {
          interactionConfig.enableTooltip = echartsConfig.tooltip.show !== false
        }
        
        // 解析系列配置获取颜色
        if (echartsConfig.series && echartsConfig.series.length > 0) {
          if (echartsConfig.series[0].itemStyle?.color) {
            styleConfig.color = echartsConfig.series[0].itemStyle.color
          }
          if (echartsConfig.series[0].label) {
            styleConfig.showLabel = echartsConfig.series[0].label.show === true
          }
        }
        
        // 解析网格配置
        if (echartsConfig.xAxis?.splitLine) {
          styleConfig.showGrid = echartsConfig.xAxis.splitLine.show !== false
        }
        
        // 恢复表格配置
        if (echartsConfig._isTable && echartsConfig.tableStyle) {
          const ts = echartsConfig.tableStyle
          tableStyleConfig.striped = ts.striped ?? true
          tableStyleConfig.size = ts.size ?? 'medium'
          tableStyleConfig.showPagination = ts.showPagination ?? true
          tableStyleConfig.pageSize = ts.pageSize ?? 10
          tableStyleConfig.showIndex = ts.showIndex ?? true
          tableStyleConfig.bordered = ts.bordered ?? true
          tableStyleConfig.displayColumns = ts.displayColumns ?? []
          tableStyleConfig.columnLabels = ts.columnLabels ?? {}
          tableStyleConfig.defaultSortField = ts.defaultSortField ?? ''
          tableStyleConfig.defaultSortOrder = ts.defaultSortOrder ?? 'ASC'
          tableStyleConfig.enableExport = ts.enableExport ?? true
          tableStyleConfig.exportFileName = ts.exportFileName ?? ''
          // 表头样式
          tableStyleConfig.headerBgColor = ts.headerBgColor ?? '#f5f7fa'
          tableStyleConfig.headerTextColor = ts.headerTextColor ?? '#303133'
          tableStyleConfig.headerFontWeight = ts.headerFontWeight ?? 'bold'
          tableStyleConfig.headerAlign = ts.headerAlign ?? 'center'
          // 汇总表配置
          tableStyleConfig.showSummary = ts.showSummary ?? false
          tableStyleConfig.summaryColumns = ts.summaryColumns ?? []
          tableStyleConfig.summaryTypes = ts.summaryTypes ?? {}
          tableStyleConfig.summaryLabel = ts.summaryLabel ?? '合计'
          // 透视表配置
          tableStyleConfig.pivotRowField = ts.pivotRowField ?? ''
          tableStyleConfig.pivotColField = ts.pivotColField ?? ''
          tableStyleConfig.pivotValueField = ts.pivotValueField ?? ''
          tableStyleConfig.pivotAggType = ts.pivotAggType ?? 'sum'
          tableStyleConfig.pivotShowRowTotal = ts.pivotShowRowTotal ?? true
          tableStyleConfig.pivotShowColTotal = ts.pivotShowColTotal ?? true
          // 条件格式化
          tableStyleConfig.enableConditionalFormat = ts.enableConditionalFormat ?? false
          tableStyleConfig.conditionalFormatField = ts.conditionalFormatField ?? ''
          tableStyleConfig.conditionalFormatType = ts.conditionalFormatType ?? 'colorScale'
          tableStyleConfig.conditionalFormatColors = ts.conditionalFormatColors ?? ['#f5222d', '#faad14', '#52c41a']
          
          // 同步分页配置到预览状态
          tablePreviewPageSize.value = ts.pageSize ?? 10
        }
      } catch (e) {
        console.error('解析配置失败', e)
        // 解析失败时使用默认配置
        sourceType.value = 'database'  // 默认数据库模式
        setDefaultChartConfig()
      }
    } else {
      sourceType.value = 'database'  // 默认数据库模式
      setDefaultChartConfig()
    }
    
    // 🔧 根据数据源类型加载数据
    if (sourceType.value === 'report') {
      // 报表模式：加载报表列表（保留选中状态）
      await loadReports(true)
      
      // 如果有selectedReportId，加载报表字段（保留字段映射）
      if (selectedReportId.value) {
        await handleReportChange(true)  // ✅ 保留字段映射
      }
    } else if (form.dataSourceId) {
      // 数据库表模式：加载表列表（保留选中状态）
      await loadTables(true)
      
      // 如果没有selectedTable但有SQL，尝试从SQL解析表名（兼容旧数据和AI生成的图表）
      if (!selectedTable.value && form.sqlContent) {
        const sqlMatch = form.sqlContent.match(/FROM\s+`?(\w+)`?/i)
        if (sqlMatch && sqlMatch[1]) {
          selectedTable.value = sqlMatch[1]
        }
      }
      
      // 🆕 如果没有字段映射但有SQL，尝试从SQL解析字段映射（兼容AI生成的图表）
      if ((!fieldMapping.xAxis || fieldMapping.yAxis.length === 0) && form.sqlContent) {
        const extractedMapping = extractFieldMappingFromSql(form.sqlContent, form.chartType || 'bar')
        if (extractedMapping.xAxis && !fieldMapping.xAxis) {
          fieldMapping.xAxis = extractedMapping.xAxis
        }
        if (extractedMapping.yAxis.length > 0 && fieldMapping.yAxis.length === 0) {
          fieldMapping.yAxis = extractedMapping.yAxis
        }
      }
      
      // 如果有selectedTable，加载该表的列（保留字段映射）
      if (selectedTable.value) {
        await handleTableChange(true)
      }
    }
    
    await handlePreviewChart()
  } catch (error) {
    handleApiError(error, '加载图表定义')
    message.error('加载图表定义失败')
  }
}

// 设置默认配置
const setDefaultChartConfig = () => {
  if (!form.chartType) {
    form.chartType = 'line'
  }
  generateChartConfig()
}

// 图表配置改变
const handleChartConfigChange = (value: string) => {
  form.chartConfig = value
}

// 加载默认配置
const handleLoadDefaultConfig = () => {
  generateChartConfig()
}

// 保存
const handleSubmit = async () => {
  if (!form.chartName) {
    message.warning('请填写图表名称')
    return
  }
  
  // 自动生成图表编码
  if (!form.chartCode) {
    form.chartCode = 'CHT_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8).toUpperCase()
  }
  
  if (!form.dataSourceId) {
    message.warning('请选择数据源')
    return
  }
  
  // 🆕 验证配置类型
  if (!dataConfigMode.value) {
    message.warning('请选择配置类型')
    return
  }
  
  // 🆕 根据配置类型验证
  if (dataConfigMode.value === 'field') {
    // 自定义字段模式：需要选择表和字段
    if (!selectedTable.value) {
      message.warning('请选择数据表')
      return
    }
    // 如果有字段映射但没有SQL，自动生成SQL
    if (!form.sqlContent && fieldMapping.xAxis && fieldMapping.yAxis.length > 0) {
      handleGenerateSql()
    }
  } else {
    // SQL模式：必须有SQL语句
    if (!form.sqlContent || !form.sqlContent.trim()) {
      message.warning('SQL模式下请输入SQL语句')
      return
    }
  }
  
  if (!form.sqlContent) {
    message.warning('请生成或输入SQL语句')
    return
  }
  
  // 保存前重新生成配置，确保最新的配置被保存
  generateChartConfig()
  
  // 验证图表配置JSON格式
  let configObj: any = {}
  try {
    configObj = JSON.parse(chartConfigJson.value)
  } catch (e) {
    message.error('图表配置JSON格式错误，请检查配置')
    return
  }
  
  // 将查询参数配置添加到 chartConfig 中（简化版，不包含运算符和范围）
  if (chartParameters.value.length > 0) {
    configObj.queryParameters = chartParameters.value.map(p => ({
      field: p.field,
      name: p.name || p.field,
      label: p.label,
      type: p.type,
      required: p.required,
      defaultValue: p.defaultValue,
      datePreset: p.datePreset,
      // 下拉选项相关
      options: p.options,
      optionSource: p.optionSource,
      optionTags: p.optionTags,
      optionSql: p.optionSql
    }))
  } else {
    delete configObj.queryParameters
  }
  
  submitting.value = true
  try {
    // 使用更新后的配置
    form.chartConfig = JSON.stringify(configObj)
    
    if (form.id) {
      await updateChartDefinition(form.id, form as ChartDefinition)
      message.success('更新成功')
    } else {
      await createChartDefinition(form as ChartDefinition)
      message.success('创建成功')
    }
    
    setTimeout(() => {
      // 替换当前标签页返回图表管理
      tabsStore.replaceTab(route.fullPath, {
        key: '/chart-manage',
        title: '图表管理',
        closable: true
      })
      router.replace('/chart-manage')
    }, 500)
  } catch (error: any) {
    const errorMsg = handleApiError(error, form.id ? '更新图表' : '创建图表')
    message.error(errorMsg)
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  // 替换当前标签页返回图表管理
  tabsStore.replaceTab(route.fullPath, {
    key: '/chart-manage',
    title: '图表管理',
    closable: true
  })
  router.replace('/chart-manage')
}

// 监听样式配置变化，自动更新图表
watch(() => [
  styleConfig.title,
  styleConfig.color,
  styleConfig.showLegend,
  styleConfig.showGrid,
  styleConfig.showLabel,
  styleConfig.width,
  styleConfig.height,
  styleConfig.animation,
  styleConfig.animationDuration,
  styleConfig.colorScheme
], () => {
  generateChartConfig()
  if (previewData.value.length > 0 && chartInstance) {
    renderChart()
  }
}, { deep: true })

// 监听交互配置变化
watch(() => [
  interactionConfig.enableZoom,
  interactionConfig.enableDataZoom,
  interactionConfig.enableLegendSelect,
  interactionConfig.enableTooltip,
  interactionConfig.enableToolbox,
  interactionConfig.toolboxFeatures,
  interactionConfig.enableHover
], () => {
  generateChartConfig()
  if (previewData.value.length > 0 && chartInstance) {
    renderChart()
  }
}, { deep: true })

// 监听字段映射变化，自动更新图表
watch(() => [fieldMapping.xAxis, fieldMapping.yAxis, fieldMapping.group], () => {
  if (previewData.value.length > 0 && form.sqlContent && chartInstance) {
    renderChart()
  }
}, { deep: true })

// 🆕 监听Y轴别名变化，自动更新图表
watch(yAxisLabels, () => {
  if (previewData.value.length > 0 && chartInstance) {
    renderChart()
  }
}, { deep: true })
</script>

<style scoped>
.chart-designer-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
}

/* 🆕 表单标签带提示图标样式 */
.form-label-with-tooltip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* ==================== 顶部工具栏 ==================== */
.designer-header {
  padding: 0 16px;
  height: 48px;
  background: #fff;
  border-bottom: 1px solid #e8eaed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-back-btn {
  color: #64748b !important;
}

.header-back-btn:hover {
  color: #3b82f6 !important;
}

.header-divider-dot {
  color: #cbd5e1;
  font-size: 10px;
}

.title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-sep {
  width: 1px;
  height: 20px;
  background: #e2e8f0;
  margin: 0 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ==================== 三栏内容区 ==================== */
.designer-content {
  flex: 1;
  display: flex;
  gap: 1px;
  padding: 0;
  overflow: hidden;
  background: #e8eaed;
}

.designer-left-panel {
  width: 300px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #fff;
}

/* 新的左侧面板卡片样式 */
.panel-card-new {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 数据源选择区域 */
.data-source-section {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.source-row {
  margin-bottom: 8px;
}

.source-row:last-child {
  margin-bottom: 0;
}

/* 折叠面板样式 */
.config-collapse {
  flex: 1;
  overflow-y: auto;
}

.config-collapse :deep(.n-collapse-item) {
  border-bottom: 1px solid #f0f0f0;
}

.config-collapse :deep(.n-collapse-item__header) {
  padding: 10px 12px;
  font-size: 13px;
}

.config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 8px 12px 12px;
}

.collapse-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 字段列表紧凑样式 */
.field-list.compact {
  max-height: 180px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 2px;
}

.field-list.compact .field-item {
  padding: 4px 6px;
  font-size: 12px;
}

/* 映射区域 */
.mapping-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.required-mark {
  color: #d03050;
  margin-left: 2px;
}

/* 筛选区域 */
.filter-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.limit-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.limit-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.filter-list-new {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item-new {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  background: #fafafa;
  border-radius: 4px;
}

/* 参数区域 */
.params-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-empty-tip-new {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 8px;
  color: #999;
  font-size: 12px;
  text-align: center;
}

.parameter-list-compact {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-item-compact {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
}

.param-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f2f5 100%);
  border-bottom: 1px solid #e8e8e8;
}

.param-item-body {
  display: flex;
  gap: 6px;
  padding: 8px;
}

.param-detail {
  padding: 8px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
}

.param-detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}

.param-detail-row:last-child {
  margin-bottom: 0;
}

.param-detail-row label {
  font-size: 11px;
  color: #666;
}

.param-item-footer {
  display: flex;
  justify-content: center;
  padding: 4px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

/* 生成按钮 */
.generate-btn-wrapper {
  padding: 12px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.designer-center-panel {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: #f5f7fa;
}

.designer-right-panel {
  width: 290px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #fff;
}

.panel-card {
  height: 100%;
  background: #fff;
}

.panel-card :deep(.n-card) {
  border: none;
  border-radius: 0;
}

.panel-card :deep(.n-card__content) {
  padding: 16px;
}

/* 左侧面板特殊样式 */
.designer-left-panel .panel-card :deep(.n-card__content) {
  padding: 0;
}

/* 配置区块样式 */
.config-section {
  margin-bottom: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
  padding-left: 8px;
  border-left: 2px solid #3b82f6;
}

/* 字段列表样式 */
.field-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 4px;
}

.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: grab;
  transition: background 0.2s;
}

.field-item:hover {
  background: #f0f9f4;
}

.field-item:active {
  cursor: grabbing;
}

.field-name {
  font-size: 12px;
  color: #333;
}

/* 字段配置项样式 */
.field-config-item {
  margin-bottom: 12px;
}

.field-config-item label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

/* 聚合列表样式 */
.aggregate-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aggregate-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-tag {
  font-size: 12px;
  color: #18a058;
  background: #f0f9f4;
  padding: 2px 8px;
  border-radius: 4px;
  min-width: 60px;
}

/* 数据限制预设按钮 */
.limit-presets {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

/* 筛选条件样式 */
.filter-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 🆕 参数列表样式 - 优化版 */
.param-empty-tip {
  padding: 20px 12px;
  text-align: center;
  color: #999;
  font-size: 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px dashed #e8e8e8;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.parameter-list-simple {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.param-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
  transition: all 0.2s;
}

.param-card:hover {
  border-color: #18a058;
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.1);
}

.param-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f0faf4 0%, #e8f5e9 100%);
  border-bottom: 1px solid #e8e8e8;
}

.param-index {
  font-size: 12px;
  font-weight: 600;
  color: #18a058;
}

.param-card-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.param-field-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-field-row label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.param-field-row :deep(.n-select),
.param-field-row :deep(.n-input) {
  width: 100%;
}

/* 范围输入行 */
.param-range-row {
  flex-direction: row;
  align-items: center;
}

.param-range-row label {
  min-width: 32px;
}

.param-range-row :deep(.n-input-number) {
  flex: 1;
}

.range-separator {
  color: #999;
  padding: 0 4px;
}

/* 单选按钮组样式 */
.param-field-row :deep(.n-radio-group) {
  width: 100%;
}

.param-field-row :deep(.n-radio-button) {
  flex: 1;
  text-align: center;
}

/* 动态标签样式 */
.param-field-row :deep(.n-dynamic-tags) {
  width: 100%;
}

.param-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.param-row:hover {
  border-color: #18a058;
}

/* 选项预览列表样式 */
.options-preview-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fafafa;
}

.options-preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.options-preview-item:last-child {
  border-bottom: none;
}

.options-preview-item .opt-value {
  color: #18a058;
  font-family: monospace;
  background: #f0faf4;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.options-preview-item .opt-label {
  color: #333;
}

/* 图表预览区域 - 画布工作区 */
.chart-preview-area {
  position: relative;
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  overflow: auto;
  background-color: #f8f9fa;
  background-image: radial-gradient(circle, #e0e2e6 0.8px, transparent 0.8px);
  background-size: 16px 16px;
  padding: 24px;
}

/* 图表渲染画布 */
.chart-preview {
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
  border: 1px solid #e8eaed;
}

/* 表格类型画布 */
.chart-preview-area .table-preview-wrapper {
  flex-shrink: 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #e8eaed;
  border-radius: 6px;
}

.preview-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: auto;
  max-width: 80%;
  background: rgba(255, 255, 255, 0.92);
  padding: 40px 48px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  text-align: center;
}

/* 预览卡片头部 */
.preview-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.preview-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #eff6ff;
  color: #3b82f6;
}

.preview-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 表格预览 */
.table-preview-wrapper {
  width: 100%;
  overflow: auto;
  background: #fff;
  border-radius: 4px;
}

/* 自定义表头颜色 */
.custom-header-table :deep(.n-data-table-th) {
  background-color: var(--header-bg-color, #f5f7fa) !important;
  color: var(--header-text-color, #303133) !important;
  font-weight: var(--header-font-weight, 600) !important;
}

.custom-header-table :deep(.n-data-table-th .n-data-table-th__title) {
  color: var(--header-text-color, #303133) !important;
}

.custom-header-table :deep(.n-data-table-th .n-data-table-sorter) {
  color: var(--header-text-color, #303133) !important;
}

/* 模板选择样式 */
.template-modal-content {
  max-height: 60vh;
  overflow-y: auto;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.template-card {
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.template-card:hover {
  border-color: #18a058;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.15);
}

.template-card.selected {
  border-color: #18a058;
  background: rgba(24, 160, 88, 0.05);
}

.template-preview {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.template-info {
  padding: 12px;
}

.template-name {
  font-weight: 600;
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.template-desc {
  font-size: 12px;
  color: #999;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 数据预览折叠区 */
.data-preview-section {
  margin-top: 12px;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  overflow: hidden;
}

.data-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e8eaed;
  cursor: pointer;
  user-select: none;
}

.data-preview-header:hover {
  background: #f1f5f9;
}

.data-preview-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.data-preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.data-preview-body {
  padding: 0;
}

/* ========== 折叠面板样式 ========== */
.datasource-section {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.config-collapse {
  margin-top: 0;
}

.config-collapse :deep(.n-collapse-item) {
  margin-top: 0;
  border: none;
  background: transparent;
}

.config-collapse :deep(.n-collapse-item__header) {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 500;
  background: #f8fafc;
  border-radius: 6px;
  margin-bottom: 4px;
}

.config-collapse :deep(.n-collapse-item__header:hover) {
  background: #f1f5f9;
}

.config-collapse :deep(.n-collapse-item__content-inner) {
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 8px;
}

.collapse-header {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.collapse-header span {
  flex: 1;
}

/* 紧凑字段列表 */
.field-list-compact {
  max-height: 180px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-item-compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.15s;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.field-item-compact:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.field-item-compact:active {
  cursor: grabbing;
}

/* 映射内容区 */
.mapping-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 紧凑聚合列表 */
.aggregate-list-compact {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.aggregate-item-compact {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 🆕 垂直布局的聚合配置 */
.aggregate-list-vertical {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.aggregate-item-vertical {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  background: #f9f9f9;
  border-radius: 6px;
}

.aggregate-field-name {
  display: flex;
  align-items: center;
}

.aggregate-controls {
  display: flex;
  gap: 8px;
}

/* 字段别名列表 */
.column-alias-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.column-alias-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.column-alias-item .n-tag {
  min-width: 60px;
  justify-content: center;
}

/* 汇总类型列表 */
.summary-type-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.summary-type-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-type-item .n-tag {
  min-width: 80px;
  justify-content: center;
}

.field-tip {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

/* 筛选内容区 */
.filter-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.limit-presets-compact {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.filter-list-compact {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item-compact {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.empty-tip {
  padding: 12px;
  text-align: center;
  color: #999;
  font-size: 12px;
  background: #fafafa;
  border-radius: 4px;
}

/* 参数内容区 */
.params-content {
  display: flex;
  flex-direction: column;
}

/* 模式提示 */
.mode-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 6px;
  padding: 4px 8px;
  background: #f8fafc;
  border-radius: 4px;
  border-left: 2px solid #e2e8f0;
}

/* 字段模式操作区 */
.field-mode-actions {
  margin-top: 12px;
  padding: 0 4px;
}

/* SQL编辑区 */
.sql-editor-section {
  margin-top: 12px;
  padding: 0;
  background: #ffffff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.sql-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.sql-editor-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
}

.sql-editor-section :deep(.n-input) {
  background: #ffffff;
  border: none;
  border-radius: 0;
}

.sql-editor-section :deep(.n-input:focus-within) {
  background: #ffffff;
  border-color: transparent;
  box-shadow: none;
}

.sql-editor-section :deep(.n-input__textarea-el) {
  line-height: 1.7;
  font-size: 13px;
  color: #1e293b !important;
  caret-color: #2563eb;
  padding: 12px !important;
  tab-size: 2;
}

.sql-editor-section :deep(.n-input__placeholder) {
  color: #9ca3af !important;
  padding: 12px !important;
}

/* SQL模式字段映射样式 */
.sql-field-mapping-content {
  padding: 12px;
  background: #ffffff;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.sql-field-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sql-field-label {
  font-size: 12px;
  color: #4b5563;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.sql-field-label::before {
  content: '';
  width: 3px;
  height: 12px;
  background: #3b82f6;
  border-radius: 2px;
}

/* SQL模式查询参数样式 */
.sql-params-content {
  padding: 8px 0;
}

.sql-params-empty {
  padding: 16px;
  text-align: center;
  background: #fff;
  border-radius: 6px;
  border: 1px dashed #cbd5e1;
}

.sql-param-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-bottom: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.sql-param-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.sql-param-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.sql-param-index {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.sql-param-body {
  padding: 10px;
}

.sql-param-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.sql-param-row:last-child {
  margin-bottom: 0;
}

.sql-param-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sql-param-label {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
}

.sql-config-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 8px;
}

/* SQL模式查询参数 - 新样式 */
.sql-params-content-new {
  padding: 12px 0;
}

.sql-params-empty-new {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  border: 2px dashed #e2e8f0;
}

.sql-param-card-new {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  margin-bottom: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.sql-param-card-new:hover {
  border-color: #18a058;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.1);
}

.sql-param-header-new {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border-bottom: 1px solid #e2e8f0;
}

.sql-param-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sql-param-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.sql-param-body-new {
  padding: 14px;
}

.sql-param-field-new {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sql-param-field-new label {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}

.sql-param-field-new .required-mark {
  color: #ef4444;
  font-weight: 600;
}

.param-empty-tip-compact {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  text-align: center;
  color: #999;
  font-size: 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px dashed #e8e8e8;
}

.param-list-compact {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-item-compact {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.2s;
}

.param-item-compact:hover {
  border-color: #18a058;
}

.param-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: linear-gradient(135deg, #f8f9fa 0%, #f0f2f5 100%);
  cursor: pointer;
  transition: background 0.2s;
}

.param-item-header:hover {
  background: linear-gradient(135deg, #f0faf4 0%, #e8f5e9 100%);
}

.param-item-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.param-item-label {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.param-item-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.param-item-detail {
  padding: 10px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-field-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-field-row label {
  font-size: 11px;
  color: #666;
  display: flex;
  align-items: center;
}

.param-range-row {
  flex-direction: row;
  align-items: center;
}

.param-range-row label {
  min-width: 28px;
}

.param-range-row :deep(.n-input-number) {
  flex: 1;
}

.range-separator {
  color: #999;
  padding: 0 4px;
  font-size: 12px;
}

/* 生成按钮包装 */
.generate-btn-wrapper {
  padding: 12px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  position: sticky;
  bottom: 0;
}

/* 🆕 颜色预设样式 */
.color-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  width: 100%;
}

.color-preset-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.color-preset-item:hover {
  border-color: #18a058;
  background: #f0faf4;
}

.color-preset-item.active {
  border-color: #18a058;
  background: #e8f5e9;
}

.preset-preview {
  width: 100%;
  height: 32px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e8e8e8;
}

.preset-colors {
  display: flex;
  gap: 3px;
}

.preset-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.preset-name {
  font-size: 10px;
  color: #666;
  text-align: center;
  white-space: nowrap;
}

/* 颜色方案预览 */
.color-scheme-preview {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

.scheme-color-block {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
}

</style>

<style>
/* ========== 深色主题适配（非scoped） ========== */
html.dark .designer-left-panel,
html.dark .designer-right-panel,
html.dark .panel-card,
html.dark .generate-btn-wrapper {
  background: #1f1f1f;
}

html.dark .designer-center-panel {
  background: #141414;
}

html.dark .param-item-compact {
  border-color: #3a3a3a;
}

html.dark .param-item-header {
  background: linear-gradient(135deg, #2a2a2a 0%, #252525 100%);
  border-bottom-color: #3a3a3a;
}

html.dark .param-item-header:hover {
  background: linear-gradient(135deg, #1a3a2a 0%, #1a3528 100%);
}

html.dark .param-item-label {
  color: #e0e0e0;
}

html.dark .param-item-detail {
  background: #252525;
  border-top-color: #3a3a3a;
}

html.dark .param-field-row label {
  color: #aaa;
}

html.dark .param-detail {
  background: #252525;
  border-top-color: #3a3a3a;
}

html.dark .param-detail-row label {
  color: #aaa;
}

html.dark .param-item-footer {
  background: #252525;
  border-top-color: #3a3a3a;
}

html.dark .range-separator {
  color: #888;
}

html.dark .preset-name {
  color: #aaa;
}

html.dark .color-scheme-preview {
  background: #2a2a2a;
}

html.dark .color-preset-item:hover {
  background: #1a3a2a;
}

html.dark .color-preset-item.active {
  background: #1a3528;
}

html.dark .preset-preview {
  border-color: #3a3a3a;
}

html.dark .generate-btn-wrapper {
  border-top-color: #3a3a3a;
}

html.dark .param-empty-tip-compact {
  color: #888;
}

html.dark .sql-params-empty-new {
  color: #888;
}
</style>

