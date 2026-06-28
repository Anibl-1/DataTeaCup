<template>
  <div class="page-designer-container" :class="{ 'fullscreen-mode': isFullscreenDesign, 'mobile-designer': form.layoutMode === 'mobile' }">
    <!-- 顶部工具栏 - 简洁版 -->
    <div v-show="!isFullscreenDesign" class="designer-header">
      <div class="header-left">
        <n-space align="center" :size="16">
          <n-button text @click="handleCancel">
            <template #icon><n-icon size="20"><ArrowBackOutline /></n-icon></template>
          </n-button>
          <span class="title-text">{{ form.id ? '编辑页面' : '新建页面' }}</span>
        </n-space>
      </div>
      <div class="header-center">
        <n-space :size="12">
          <n-input
            v-model:value="form.pageName"
            placeholder="页面名称"
            style="width: 180px"
          />
          <n-select
            v-model:value="form.theme"
            :options="themeOptions"
            placeholder="主题"
            style="width: 120px"
          />
          <n-tag v-if="form.layoutMode === 'mobile'" type="success" size="small" round>📱 移动端</n-tag>
          <n-tag v-else-if="form.layoutMode === 'bigscreen'" type="warning" size="small" round>🖥️ 大屏</n-tag>
          <n-tag v-else type="info" size="small" round>💻 桌面端</n-tag>
        </n-space>
      </div>
      <div class="header-right">
        <n-space :size="12">
          <n-button :disabled="!form.id" @click="handlePreview">预览</n-button>
          <n-button @click="handleCancel">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSave">保存</n-button>
        </n-space>
      </div>
    </div>

    <!-- 工具条 -->
    <div class="toolbar-row" :class="{ 'fullscreen-toolbar': isFullscreenDesign, 'mobile-mode-toolbar': form.layoutMode === 'mobile', 'bigscreen-mode-toolbar': form.layoutMode === 'bigscreen' }">
      <n-space align="center">
        <n-button size="small" :type="showGrid ? 'primary' : 'default'" @click="showGrid = !showGrid">
          <template #icon><n-icon><GridOutline /></n-icon></template>
          网格
        </n-button>
        <n-divider vertical />
        <!-- 画布尺寸控制 -->
        <span class="toolbar-label">画布:</span>
        <n-input-number
          v-model:value="canvasWidth"
          :min="form.layoutMode === 'mobile' ? 320 : 400"
          :max="form.layoutMode === 'bigscreen' ? 5120 : 4000"
          :step="form.layoutMode === 'mobile' ? 5 : 50"
          size="small"
          style="width: 100px"
          :show-button="false"
          @update:value="handleCanvasSizeInputChange"
        />
        <span class="toolbar-label">×</span>
        <n-input-number
          v-model:value="canvasHeight"
          :min="form.layoutMode === 'mobile' ? 480 : 300"
          :max="form.layoutMode === 'bigscreen' ? 3840 : 3000"
          :step="form.layoutMode === 'mobile' ? 5 : 50"
          size="small"
          style="width: 100px"
          :show-button="false"
          @update:value="handleCanvasSizeInputChange"
        />
        <n-select
          v-model:value="canvasSize"
          :options="canvasSizeOptions"
          style="width: 140px"
          size="small"
          placeholder="预设"
          @update:value="handleCanvasSizeChange"
        />
        <n-divider vertical />
        <n-button-group size="small">
          <n-button :disabled="zoomLevel <= 30" @click="handleZoomOut">-</n-button>
          <n-button @click="handleZoomReset">{{ zoomLevel }}%</n-button>
          <n-button :disabled="zoomLevel >= 200" @click="handleZoomIn">+</n-button>
        </n-button-group>
        <n-button size="small" @click="handleFitToView">
          <template #icon><n-icon><ScanOutline /></n-icon></template>
          适配
        </n-button>
        <n-divider vertical />
        <n-button size="small" type="error" ghost @click="handleClear">清空</n-button>
        <n-divider vertical />
        <n-button size="small" :disabled="!form.id" @click="loadPageVersions">
          <template #icon><n-icon><TimeOutline /></n-icon></template>
          版本历史
        </n-button>
        <n-divider vertical />
        <!-- 全屏设计模式切换 -->
        <n-button 
          size="small" 
          :type="isFullscreenDesign ? 'primary' : 'default'"
          @click="toggleFullscreenDesign"
        >
          <template #icon><n-icon><ExpandOutline /></n-icon></template>
          {{ isFullscreenDesign ? '退出全屏' : '全屏设计' }}
        </n-button>
      </n-space>
      <!-- 全屏模式下的额外操作 -->
      <n-space v-if="isFullscreenDesign" align="center">
        <n-button size="small" type="primary" :loading="submitting" @click="handleSave">
          <template #icon><n-icon><SaveOutline /></n-icon></template>
          保存
        </n-button>
        <n-button size="small" @click="toggleFullscreenDesign">
          <template #icon><n-icon><CloseOutline /></n-icon></template>
          退出
        </n-button>
      </n-space>
    </div>

    <div class="designer-content">
      <!-- 全屏模式下的浮动展开按钮 -->
      <div v-if="isFullscreenDesign && leftPanelCollapsed" class="floating-panel-btn left" @click="leftPanelCollapsed = false">
        <n-icon size="16"><AppsOutline /></n-icon>
        <span>组件</span>
      </div>
      <div v-if="isFullscreenDesign && rightPanelCollapsed" class="floating-panel-btn right" @click="rightPanelCollapsed = false">
        <n-icon size="16"><SettingsOutline /></n-icon>
        <span>属性</span>
      </div>
      
      <!-- 左侧：四 Tab面板（组件库 + 图表资源库 + 模板 + AI助手） -->
      <div class="designer-left-panel" :class="{ 'panel-collapsed': leftPanelCollapsed }">
        <!-- 折叠按钮 -->
        <div class="panel-collapse-btn left" @click="leftPanelCollapsed = !leftPanelCollapsed">
          <n-icon size="16">
            <ChevronBackOutline v-if="!leftPanelCollapsed" />
            <ChevronForwardOutline v-else />
          </n-icon>
        </div>
        <n-card size="small" class="panel-card">
          <n-tabs v-model:value="leftPanelTab" type="segment" size="small" style="margin-bottom: 12px;">
            <n-tab name="components">组件库</n-tab>
            <n-tab name="charts">图表库</n-tab>
            <n-tab name="templates">模板</n-tab>
            <n-tab name="ai">AI助手</n-tab>
          </n-tabs>
          
          <!-- Tab 1: 组件库（图表类型 + 查询组件） -->
          <template v-if="leftPanelTab === 'components'">
            <ComponentLibrary
              :layout-mode="form.layoutMode"
              @add-chart-type="handleClickAddInlineChartType"
              @add-query-component="handleClickAddQueryComponent"
              @add-static-component="handleClickAddStaticComponent"
            />
          </template>
          
          <!-- Tab 2: 已有图表资源库 -->
          <template v-else-if="leftPanelTab === 'charts'">
            <ChartLibraryPanel :charts="charts" :loading="chartsLoading" @add-chart="handleClickAddReferencedChart" />
          </template>
          
          <!-- Tab 3: 页面模板 -->
          <template v-else-if="leftPanelTab === 'templates'">
            <div class="template-panel">
              <!-- 大屏专属模板 -->
              <template v-if="form.layoutMode === 'bigscreen'">
                <div class="template-section-title">大屏布局模板</div>
                <div v-for="bsTpl in bigscreenTemplateList" :key="bsTpl.id" class="tpl-card" @click="applyBigscreenTemplate(bsTpl)">
                  <div class="tpl-preview" :style="{ background: bsTpl.thumbnail, aspectRatio: bsTpl.width + '/' + bsTpl.height }">
                    <div class="tpl-preview-grid">
                      <div
v-for="(item, idx) in bsTpl.items" :key="idx"
                        :class="['tpl-block', item.type === 'static' || isDesktopMetricPlaceholder(item) ? 'tpl-block-static' : 'tpl-block-chart']"
                        :style="{
                          left: (item.left / bsTpl.width * 100) + '%',
                          top: (item.top / bsTpl.height * 100) + '%',
                          width: (item.width / bsTpl.width * 100) + '%',
                          height: (item.height / bsTpl.height * 100) + '%'
                        }" />
                    </div>
                  </div>
                  <div class="tpl-meta">
                    <span class="tpl-title">{{ bsTpl.name }}</span>
                    <span class="tpl-subtitle">{{ bsTpl.description }}</span>
                  </div>
                </div>
              </template>

              <!-- 移动端专属模板 -->
              <template v-if="form.layoutMode === 'mobile'">
                <div class="template-section-title">移动端布局模板</div>
                <div class="tpl-mobile-grid">
                  <div v-for="mbTpl in mobileTemplateList" :key="mbTpl.id" class="tpl-card tpl-card-mobile" @click="applyMobileTemplate(mbTpl)">
                    <div class="tpl-preview tpl-preview-mobile" :style="{ background: mbTpl.thumbnail }">
                      <div class="tpl-preview-grid">
                        <div
v-for="(item, idx) in mbTpl.items" :key="idx"
                          :class="['tpl-block', item.type === 'static' ? 'tpl-block-static' : 'tpl-block-chart']"
                          :style="{
                            left: (item.left / mbTpl.width * 100) + '%',
                            top: (item.top / mbTpl.height * 100) + '%',
                            width: (item.width / mbTpl.width * 100) + '%',
                            height: (item.height / mbTpl.height * 100) + '%'
                          }" />
                      </div>
                    </div>
                    <span class="tpl-title">{{ mbTpl.name }}</span>
                  </div>
                </div>
              </template>

              <!-- 桌面端专属模板 -->
              <template v-if="form.layoutMode !== 'bigscreen' && form.layoutMode !== 'mobile'">
                <div class="template-filter-row">
                  <n-select
                    v-model:value="selectedTemplateCategory"
                    :options="templateCategoryOptions"
                    size="small"
                    style="width: 100%"
                    placeholder="选择模板分类"
                  />
                </div>
                <div class="template-count">共 {{ filteredDesktopTemplates.length }} 个模板</div>
                <div v-for="dkTpl in filteredDesktopTemplates" :key="dkTpl.id" class="tpl-card" @click="applyDesktopTemplate(dkTpl)">
                  <div class="tpl-preview" :style="{ background: dkTpl.thumbnail, aspectRatio: dkTpl.width + '/' + dkTpl.height }">
                    <div class="tpl-preview-grid">
                      <div
                        v-for="(item, idx) in dkTpl.items" :key="idx"
                        :class="['tpl-block', item.type === 'static' ? 'tpl-block-static' : 'tpl-block-chart']"
                        :style="{
                          left: (item.left / dkTpl.width * 100) + '%',
                          top: (item.top / dkTpl.height * 100) + '%',
                          width: (item.width / dkTpl.width * 100) + '%',
                          height: (item.height / dkTpl.height * 100) + '%'
                        }" />
                    </div>
                  </div>
                  <div class="tpl-meta">
                    <span class="tpl-title">{{ dkTpl.name }}</span>
                    <span class="tpl-subtitle">{{ dkTpl.description }}</span>
                  </div>
                </div>
              </template>

              <!-- 自定义模板 -->
              <div class="template-section-title">自定义模板</div>
              <n-spin :show="templatesLoading">
                <div v-if="pageTemplates.length === 0 && !templatesLoading" style="text-align: center; padding: 20px 0; color: #999; font-size: 13px;">
                  暂无自定义模板
                </div>
                <div v-for="tpl in pageTemplates" :key="tpl.id" class="template-card" @click="applyTemplate(tpl.id)">
                  <div class="template-icon">
                    <n-icon size="28" color="#6366f1"><AppsOutline /></n-icon>
                  </div>
                  <div class="template-info">
                    <div class="template-name">{{ tpl.name }}</div>
                    <div class="template-desc">{{ tpl.description }}</div>
                  </div>
                </div>
              </n-spin>
            </div>
          </template>
          
          <!-- Tab 4: AI 设计助手 -->
          <template v-else>
            <AiDesignAssistant
              :data-sources="dataSources"
              @add-inline-chart="handleAddInlineChartFromAi"
            />
          </template>
        </n-card>
      </div>

      <!-- 中间：自由布局画布 -->
      <div class="designer-center-panel">
        <n-card :title="form.layoutMode === 'bigscreen' ? '🖥️ 大屏可视化设计' : form.layoutMode === 'mobile' ? '📱 移动端页面设计' : '页面设计'" size="small" class="panel-card">
          <template #header-extra>
            <n-space>
              <n-button 
                size="small" 
                :type="showParameterPanel ? 'primary' : 'default'"
                @click="showParameterPanel = !showParameterPanel"
              >
                <template #icon><n-icon><FilterOutline /></n-icon></template>
                {{ showParameterPanel ? '隐藏参数区' : '显示参数区' }}
              </n-button>
              <n-tag v-if="selectedItem" type="info" size="small">
                已选中: {{ getChartName(selectedItem.chartId, selectedItem) }}
              </n-tag>
              <n-tag v-if="selectedQueryComponent" type="success" size="small">
                已选中参数: {{ selectedQueryComponent.label }}
              </n-tag>
            </n-space>
          </template>
          
          <!-- 参数面板区域 -->
          <ParameterPanel
            v-if="showParameterPanel"
            :design-mode="true"
            :components="parameterPanel.components"
            :show-query-button="parameterPanel.showQueryButton ?? true"
            :show-reset-button="parameterPanel.showResetButton ?? true"
            @update:components="handleParameterComponentsUpdate"
            @select="handleSelectQueryComponent"
            @query="handleParameterQuery"
            @reset="handleParameterReset"
          />
          
          <div
            ref="canvasWrapperRef"
            class="canvas-wrapper"
            :class="{ 'is-panning': isPanning, 'mobile-mode': form.layoutMode === 'mobile', 'bigscreen-mode': form.layoutMode === 'bigscreen' }"
            @wheel="handleCanvasWheel"
            @mousedown="handleCanvasPanStart"
            @mousemove="handleCanvasPanMove"
            @mouseup="handleCanvasPanEnd"
            @mouseleave="handleCanvasPanEnd"
          >
            <div 
              class="canvas-scale-wrapper"
              :class="{ 'mobile-frame-wrapper': form.layoutMode === 'mobile' }"
              :style="{
                width: `${(canvasWidth + canvasExtraPadding * 2) * zoomLevel / 100}px`,
                height: `${(canvasHeight + canvasExtraPadding * 2) * zoomLevel / 100}px`,
                transformOrigin: 'top left',
                minWidth: `${(canvasWidth + canvasExtraPadding * 2) * zoomLevel / 100}px`,
                minHeight: `${(canvasHeight + canvasExtraPadding * 2) * zoomLevel / 100}px`
              }"
            >
              <div
                ref="canvasRef"
                class="design-canvas"
                :class="{
                  'drag-over': isDraggingOver,
                  'show-grid': showGrid,
                  'mobile-canvas': form.layoutMode === 'mobile'
                }"
                :style="{
                  transform: `scale(${zoomLevel / 100})`,
                  transformOrigin: 'top left',
                  width: `${canvasWidth}px`,
                  height: `${canvasHeight}px`,
                  minHeight: `${canvasHeight}px`,
                  backgroundSize: showGrid ? `${currentGridSize}px ${currentGridSize}px` : 'none',
                  backgroundColor: currentThemeBackground
                }"
              @drop="handleDrop"
              @dragover.prevent="handleDragOver"
              @dragenter.prevent="handleDragEnter"
              @dragleave.prevent="handleDragLeave"
              @click="handleCanvasClick"
            >
              <!-- 对齐辅助线 -->
              <svg v-if="snapLines.x.length > 0 || snapLines.y.length > 0" class="snap-lines-overlay">
                <line v-for="(sx, i) in snapLines.x" :key="'sx-' + i" :x1="sx" y1="0" :x2="sx" :y2="canvasHeight" class="snap-line-v" />
                <line v-for="(sy, i) in snapLines.y" :key="'sy-' + i" x1="0" :y1="sy" :x2="canvasWidth" :y2="sy" class="snap-line-h" />
              </svg>
              <!-- 使用 vue3-draggable-resizable 组件 -->
              <VueDraggableResizable
                v-for="item in validLayout"
                :key="item.i"
                :x="item.left"
                :y="item.top"
                :w="item.width"
                :h="item.height"
                :min-width="getItemMinWidth(item)"
                :min-height="getItemMinHeight(item)"
                :parent="true"
                :snap="true"
                :snap-tolerance="5"
                :grid="[5, 5]"
                :active="selectedItem?.i === item.i"
                :resizable="true"
                :draggable="!isSpaceDown"
                :lock-aspect-ratio="false"
                class="chart-item-wrapper"
                @activated="handleItemActivated(item)"
                @deactivated="handleItemDeactivated"
                @resizing="handleResizing($event, item)"
                @resizestop="handleResizeStop($event, item)"
                @dragging="handleDragging($event, item)"
                @dragstop="handleDragStop($event, item)"
              >
                <div class="chart-item-content">
                  <div class="item-header">
                    <div class="chart-header-left">
                      <div class="chart-icon" :class="{ 'inline-icon': item.mode === 'inline', 'static-icon': item.mode === 'static' }">
                        <n-icon size="12"><component :is="getChartIconComponent(item.mode === 'inline' ? (item.inlineConfig?.chartType || 'bar') : item.mode === 'static' ? 'bar' : getChartType(item.chartId))" /></n-icon>
                      </div>
                      <n-text strong style="font-size: 13px;">{{ getChartName(item.chartId, item) }}</n-text>
                      <n-tag v-if="item.mode === 'inline'" size="tiny" type="warning" :bordered="false">内联</n-tag>
                      <n-tag v-if="item.mode === 'static'" size="tiny" type="success" :bordered="false">静态</n-tag>
                    </div>
                    <n-button
                      size="tiny"
                      type="error"
                      text
                      style="opacity: 0.7"
                      @click.stop="handleRemoveChart(item.i)"
                    >
                      <template #icon>
                        <n-icon><CloseOutline /></n-icon>
                      </template>
                    </n-button>
                  </div>
                  <div class="item-content">
                    <!-- 静态组件渲染 -->
                    <template v-if="item.mode === 'static'">
                      <div class="static-component-render" style="width: 100%; height: 100%; padding: 8px; overflow: hidden;">
                        <template v-if="item.staticType === 'title'">
                          <textarea
                            v-if="editingItemId === item.i"
                            :data-inline-edit="item.i"
                            class="inline-edit-input"
                            v-model="item.staticConfig.text"
                            :style="{ fontSize: (item.staticConfig?.fontSize || 24) + 'px', fontWeight: item.staticConfig?.fontWeight || 'bold', color: item.staticConfig?.color || '#333', textAlign: item.staticConfig?.align || 'left', lineHeight: 1.3 }"
                            @mousedown.stop
                            @blur="finishInlineEdit"
                            @keydown.enter.prevent="finishInlineEdit"
                          ></textarea>
                          <div v-else class="inline-editable" :style="{ fontSize: (item.staticConfig?.fontSize || 24) + 'px', fontWeight: item.staticConfig?.fontWeight || 'bold', color: item.staticConfig?.color || '#333', textAlign: item.staticConfig?.align || 'left', lineHeight: 1.3 }" title="双击编辑文字" @dblclick.stop="startInlineEdit(item)">
                            {{ item.staticConfig?.text || '标题文本' }}
                          </div>
                        </template>
                        <template v-else-if="item.staticType === 'text'">
                          <textarea
                            v-if="editingItemId === item.i"
                            :data-inline-edit="item.i"
                            class="inline-edit-input"
                            v-model="item.staticConfig.text"
                            :style="{ fontSize: (item.staticConfig?.fontSize || 14) + 'px', color: item.staticConfig?.color || '#666', textAlign: item.staticConfig?.align || 'left', lineHeight: item.staticConfig?.lineHeight || 1.6 }"
                            @mousedown.stop
                            @blur="finishInlineEdit"
                          ></textarea>
                          <div v-else class="inline-editable" :style="{ fontSize: (item.staticConfig?.fontSize || 14) + 'px', color: item.staticConfig?.color || '#666', textAlign: item.staticConfig?.align || 'left', lineHeight: item.staticConfig?.lineHeight || 1.6, whiteSpace: 'pre-wrap' }" title="双击编辑文字" @dblclick.stop="startInlineEdit(item)">
                            {{ item.staticConfig?.text || '文本内容' }}
                          </div>
                        </template>
                        <template v-else-if="item.staticType === 'divider'">
                          <div style="display: flex; align-items: center; height: 100%;">
                            <hr :style="{ width: '100%', border: 'none', borderTop: `${item.staticConfig?.thickness || 1}px ${item.staticConfig?.style || 'solid'} ${item.staticConfig?.color || '#e0e0e0'}` }" />
                          </div>
                        </template>
                        <template v-else-if="item.staticType === 'image'">
                          <div style="display: flex; align-items: center; justify-content: center; height: 100%;">
                            <img v-if="item.staticConfig?.src" :src="item.staticConfig.src" :alt="item.staticConfig?.alt || ''" :style="{ maxWidth: '100%', maxHeight: '100%', objectFit: item.staticConfig?.objectFit || 'contain' }" />
                            <div v-else style="color: #ccc; text-align: center;"><n-icon size="32"><GridOutline /></n-icon><div>请设置图片地址</div></div>
                          </div>
                        </template>
                        <!-- 大屏: KPI卡片 -->
                        <template v-else-if="item.staticType === 'kpiCard'">
                          <div class="bs-kpi-card" :style="{ background: item.staticConfig?.bgColor || 'rgba(6,30,60,0.8)', borderColor: item.staticConfig?.borderColor || 'rgba(64,158,255,0.4)' }">
                            <input v-if="editingItemId === item.i" :data-inline-edit="item.i" class="inline-edit-input inline-edit-kpi" v-model="item.staticConfig.label" :style="{ color: item.staticConfig?.labelColor || '#8ca8c8' }" @mousedown.stop @blur="finishInlineEdit" @keydown.enter.prevent="finishInlineEdit" />
                            <div v-else class="bs-kpi-label inline-editable" :style="{ color: item.staticConfig?.labelColor || '#8ca8c8' }" title="双击编辑指标名" @dblclick.stop="startInlineEdit(item)">{{ item.staticConfig?.label || 'KPI指标' }}</div>
                            <div class="bs-kpi-value" :style="{ color: item.staticConfig?.valueColor || '#00d4ff', fontSize: (item.staticConfig?.valueFontSize || 32) + 'px' }">
                              {{ item.staticConfig?.prefix || '' }}{{ item.staticConfig?.value || '0' }}{{ item.staticConfig?.suffix || '' }}
                            </div>
                          </div>
                        </template>
                        <!-- 大屏: 数字翻牌器 -->
                        <template v-else-if="item.staticType === 'numberFlipper'">
                          <div class="bs-number-flipper" :style="{ background: item.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                            <div class="bs-flipper-label" :style="{ color: item.staticConfig?.labelColor || '#8ca8c8' }">{{ item.staticConfig?.label || '数据指标' }}</div>
                            <div class="bs-flipper-digits">
                              <span v-for="(digit, di) in String(item.staticConfig?.value || '00000').split('')" :key="di" class="bs-digit" :style="{ color: item.staticConfig?.digitColor || '#00ffcc', fontSize: (item.staticConfig?.digitFontSize || 48) + 'px' }">{{ digit }}</span>
                            </div>
                            <div v-if="item.staticConfig?.unit" class="bs-flipper-unit" :style="{ color: item.staticConfig?.unitColor || '#5b8fae' }">{{ item.staticConfig.unit }}</div>
                          </div>
                        </template>
                        <!-- 大屏: 装饰边框 -->
                        <template v-else-if="item.staticType === 'decorBorder'">
                          <div class="bs-decor-border" :style="{ borderColor: item.staticConfig?.borderColor || 'rgba(64,158,255,0.6)', background: item.staticConfig?.bgColor || 'rgba(6,30,60,0.5)' }">
                            <div class="bs-decor-corner tl"></div><div class="bs-decor-corner tr"></div>
                            <div class="bs-decor-corner bl"></div><div class="bs-decor-corner br"></div>
                            <div v-if="item.staticConfig?.title" class="bs-decor-title" :style="{ color: item.staticConfig?.titleColor || '#00d4ff' }">{{ item.staticConfig.title }}</div>
                          </div>
                        </template>
                        <!-- 大屏: 标题栏 -->
                        <template v-else-if="item.staticType === 'headerBar'">
                          <div class="bs-header-bar" :style="{ background: item.staticConfig?.bgColor || 'linear-gradient(90deg, rgba(6,30,60,0.9) 0%, rgba(12,21,39,0.95) 50%, rgba(6,30,60,0.9) 100%)' }">
                            <div class="bs-header-deco-left"></div>
                            <div class="bs-header-title" :style="{ color: item.staticConfig?.titleColor || '#00d4ff', fontSize: (item.staticConfig?.titleFontSize || 28) + 'px' }">
                              {{ item.staticConfig?.title || '数据可视化大屏' }}
                            </div>
                            <div class="bs-header-deco-right"></div>
                            <div v-if="item.staticConfig?.showTime !== false" class="bs-header-time" :style="{ color: item.staticConfig?.timeColor || '#5b8fae' }">{{ new Date().toLocaleString() }}</div>
                          </div>
                        </template>
                        <!-- 大屏: 进度条 -->
                        <template v-else-if="item.staticType === 'progressBar'">
                          <div class="bs-progress-bar" :style="{ background: item.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                            <div class="bs-progress-label" :style="{ color: item.staticConfig?.labelColor || '#8ca8c8' }">{{ item.staticConfig?.label || '完成进度' }}</div>
                            <div class="bs-progress-track">
                              <div class="bs-progress-fill" :style="{ width: (item.staticConfig?.percent || 60) + '%', background: item.staticConfig?.barColor || 'linear-gradient(90deg, #00d4ff, #00ffcc)' }"></div>
                            </div>
                            <div class="bs-progress-value" :style="{ color: item.staticConfig?.valueColor || '#00d4ff' }">{{ item.staticConfig?.percent || 60 }}%</div>
                          </div>
                        </template>
                        <!-- 大屏: 倒计时/滚动列表/跑马灯 - 占位 -->
                        <template v-else-if="item.staticType === 'countdown' || item.staticType === 'scrollList' || item.staticType === 'marquee'">
                          <div style="display: flex; align-items: center; justify-content: center; height: 100%; background: rgba(6,30,60,0.6); border: 1px dashed rgba(64,158,255,0.3); border-radius: 4px; color: #5b8fae; font-size: 13px;">
                            <n-icon size="20" style="margin-right: 6px;"><GridOutline /></n-icon>
                            {{ item.staticLabel || item.staticType }}
                          </div>
                        </template>
                        <!-- 移动端: 状态栏 -->
                        <template v-else-if="item.staticType === 'statusBar'">
                          <div class="mb-status-bar">
                            <span class="mb-status-time">{{ new Date().toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) }}</span>
                            <div class="mb-status-icons">
                              <span style="font-size:11px;">●●●●</span>
                              <span style="font-size:11px;">WiFi</span>
                              <span style="font-size:11px;">🔋</span>
                            </div>
                          </div>
                        </template>
                        <!-- 移动端: 导航栏 -->
                        <template v-else-if="item.staticType === 'navbar'">
                          <div class="mb-navbar" :style="{ background: item.staticConfig?.bgColor || '#fff' }">
                            <span class="mb-navbar-back" :style="{ color: item.staticConfig?.color || '#333' }">‹</span>
                            <span class="mb-navbar-title" :style="{ color: item.staticConfig?.color || '#333', fontSize: (item.staticConfig?.fontSize || 17) + 'px' }">{{ item.staticConfig?.title || '页面标题' }}</span>
                            <span class="mb-navbar-action"></span>
                          </div>
                        </template>
                        <!-- 移动端: 底部标签栏 -->
                        <template v-else-if="item.staticType === 'tabBar'">
                          <div class="mb-tabbar">
                            <div v-for="(tab, ti) in (item.staticConfig?.tabs || [{icon:'🏠',label:'首页'},{icon:'📊',label:'数据'},{icon:'👤',label:'我的'}])" :key="ti" class="mb-tabbar-item" :class="{ active: ti === (item.staticConfig?.activeIndex || 0) }">
                              <span class="mb-tabbar-icon">{{ tab.icon }}</span>
                              <span class="mb-tabbar-label">{{ tab.label }}</span>
                            </div>
                          </div>
                        </template>
                        <!-- 移动端: 卡片容器 -->
                        <template v-else-if="item.staticType === 'mobileCard'">
                          <div class="mb-card" :style="{ background: item.staticConfig?.bgColor || '#fff', borderRadius: (item.staticConfig?.radius || 12) + 'px' }">
                            <div v-if="item.staticConfig?.title" class="mb-card-title" :style="{ color: item.staticConfig?.titleColor || '#333' }">{{ item.staticConfig.title }}</div>
                            <div class="mb-card-body" :style="{ color: item.staticConfig?.textColor || '#666' }">{{ item.staticConfig?.content || '卡片内容区域' }}</div>
                          </div>
                        </template>
                        <!-- 移动端: 列表项 -->
                        <template v-else-if="item.staticType === 'listItem'">
                          <div class="mb-list-item" :style="{ background: item.staticConfig?.bgColor || '#fff' }">
                            <span v-if="item.staticConfig?.icon" class="mb-list-icon">{{ item.staticConfig.icon }}</span>
                            <div class="mb-list-content">
                              <div class="mb-list-title" :style="{ color: item.staticConfig?.titleColor || '#333' }">{{ item.staticConfig?.title || '列表项标题' }}</div>
                              <div v-if="item.staticConfig?.desc" class="mb-list-desc">{{ item.staticConfig.desc }}</div>
                            </div>
                            <span class="mb-list-arrow">›</span>
                          </div>
                        </template>
                        <!-- 移动端: 搜索栏 -->
                        <template v-else-if="item.staticType === 'searchBar'">
                          <div class="mb-search-bar" :style="{ background: item.staticConfig?.bgColor || '#f5f5f5' }">
                            <div class="mb-search-input" :style="{ background: item.staticConfig?.inputBg || '#fff', borderRadius: (item.staticConfig?.radius || 20) + 'px' }">
                              <span style="color:#999;margin-right:6px;">🔍</span>
                              <span style="color:#bbb;">{{ item.staticConfig?.placeholder || '搜索...' }}</span>
                            </div>
                          </div>
                        </template>
                      </div>
                    </template>
                    <!-- 表格类型预览 -->
                    <template v-else-if="isTableType(item.chartId, item)">
                      <div class="table-preview-placeholder">
                        <n-icon size="32" color="#36cfc9"><GridOutline /></n-icon>
                        <span>{{ getTableTypeLabel(item.chartId, item) }}</span>
                      </div>
                    </template>
                    <!-- 图表预览容器 -->
                    <template v-else>
                      <div
                        :ref="el => setChartPreviewRef(el as HTMLElement | null, item.chartId || 0, item.i)"
                        class="chart-preview-container"
                      ></div>
                    </template>
                    <!-- 加载状态 -->
                    <div v-if="item.chartId && chartLoadingMap[item.chartId]" class="chart-loading">
                      <n-spin size="small" />
                    </div>
                    <!-- 尺寸信息 -->
                    <div class="item-size-info">
                      <n-text depth="3" style="font-size: 10px">
                        {{ Math.round(item.width) }} × {{ Math.round(item.height) }}px
                      </n-text>
                    </div>
                  </div>
                </div>
              </VueDraggableResizable>
              
              <div v-if="layout.length === 0" class="canvas-placeholder" @click="handleCanvasClick">
                <n-empty :description="form.layoutMode === 'bigscreen' ? '拖拽大屏组件到画布，构建数据可视化大屏' : form.layoutMode === 'mobile' ? '拖拽移动端组件到画布，设计移动端页面' : '从左侧拖拽或点击 + 添加图表到画布'">
                  <template #extra>
                    <n-text depth="3" style="font-size: 12px">
                      {{ form.layoutMode === 'bigscreen' ? '推荐使用大屏标题栏 + 装饰边框 + KPI卡片组合布局' : form.layoutMode === 'mobile' ? '推荐先添加状态栏和导航栏，再填充内容组件' : 'Ctrl+滚轮缩放 | 中键/空格+拖拽平移 | 点击选中后可拖动和调整大小' }}
                    </n-text>
                  </template>
                </n-empty>
              </div>
            </div>
            </div>
          </div>
        </n-card>
      </div>

      <!-- 右侧：属性面板 -->
      <div class="designer-right-panel" :class="{ 'panel-collapsed': rightPanelCollapsed }">
        <!-- 折叠按钮 -->
        <div class="panel-collapse-btn right" @click="rightPanelCollapsed = !rightPanelCollapsed">
          <n-icon size="16">
            <ChevronForwardOutline v-if="!rightPanelCollapsed" />
            <ChevronBackOutline v-else />
          </n-icon>
        </div>
        <n-card v-show="!rightPanelCollapsed" :title="form.layoutMode === 'bigscreen' ? '🎛️ 大屏属性' : form.layoutMode === 'mobile' ? '📐 组件属性' : '属性设置'" size="small" class="panel-card">
          <!-- 查询组件配置 -->
          <template v-if="selectedQueryComponent">
            <div class="config-section-title">
              <n-icon color="#18a058"><FilterOutline /></n-icon>
              <span>查询组件配置</span>
            </div>
            <QueryComponentConfig
              :component="selectedQueryComponent"
              :charts="chartsWithFields"
              :data-sources="dataSources"
              @update:component="handleQueryComponentUpdate"
            />
          </template>
          
          <!-- 静态组件属性编辑 -->
          <template v-else-if="selectedItem && selectedItem.mode === 'static'">
            <div class="config-section-title">
              <n-icon color="#f0a020"><GridOutline /></n-icon>
              <span>{{ getChartName(undefined, selectedItem) }} 配置</span>
            </div>
            <n-form label-placement="left" label-width="70" size="small" style="padding: 8px 0;">
              <template v-if="selectedItem.staticType === 'title' || selectedItem.staticType === 'text'">
                <n-form-item label="内容">
                  <n-input v-model:value="selectedItem.staticConfig.text" type="textarea" :rows="selectedItem.staticType === 'title' ? 2 : 4" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="字号">
                  <n-input-number v-model:value="selectedItem.staticConfig.fontSize" :min="10" :max="72" :step="1" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.color" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="对齐">
                  <n-select v-model:value="selectedItem.staticConfig.align" :options="[{label:'左对齐',value:'left'},{label:'居中',value:'center'},{label:'右对齐',value:'right'}]" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item v-if="selectedItem.staticType === 'title'" label="粗细">
                  <n-select v-model:value="selectedItem.staticConfig.fontWeight" :options="[{label:'正常',value:'normal'},{label:'粗体',value:'bold'},{label:'更粗',value:'800'}]" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <template v-else-if="selectedItem.staticType === 'divider'">
                <n-form-item label="样式">
                  <n-select v-model:value="selectedItem.staticConfig.style" :options="[{label:'实线',value:'solid'},{label:'虚线',value:'dashed'},{label:'点线',value:'dotted'}]" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.color" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="粗细">
                  <n-input-number v-model:value="selectedItem.staticConfig.thickness" :min="1" :max="10" :step="1" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <template v-else-if="selectedItem.staticType === 'image'">
                <n-form-item label="图片URL">
                  <n-input v-model:value="selectedItem.staticConfig.src" placeholder="https://..." @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="替代文本">
                  <n-input v-model:value="selectedItem.staticConfig.alt" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="适配">
                  <n-select v-model:value="selectedItem.staticConfig.objectFit" :options="[{label:'包含',value:'contain'},{label:'覆盖',value:'cover'},{label:'拉伸',value:'fill'},{label:'无',value:'none'}]" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: KPI卡片配置 -->
              <template v-else-if="selectedItem.staticType === 'kpiCard'">
                <n-form-item label="指标名">
                  <n-input v-model:value="selectedItem.staticConfig.label" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数值">
                  <n-input v-model:value="selectedItem.staticConfig.value" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="前缀">
                  <n-input v-model:value="selectedItem.staticConfig.prefix" placeholder="如 ¥" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="后缀">
                  <n-input v-model:value="selectedItem.staticConfig.suffix" placeholder="如 万" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数值字号">
                  <n-input-number v-model:value="selectedItem.staticConfig.valueFontSize" :min="16" :max="72" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数值颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.valueColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标签颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.labelColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="边框色">
                  <n-input v-model:value="selectedItem.staticConfig.borderColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 数字翻牌器配置 -->
              <template v-else-if="selectedItem.staticType === 'numberFlipper'">
                <n-form-item label="标签">
                  <n-input v-model:value="selectedItem.staticConfig.label" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数值">
                  <n-input v-model:value="selectedItem.staticConfig.value" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="单位">
                  <n-input v-model:value="selectedItem.staticConfig.unit" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数字字号">
                  <n-input-number v-model:value="selectedItem.staticConfig.digitFontSize" :min="20" :max="96" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数字颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.digitColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 装饰边框配置 -->
              <template v-else-if="selectedItem.staticType === 'decorBorder'">
                <n-form-item label="标题">
                  <n-input v-model:value="selectedItem.staticConfig.title" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.titleColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="边框颜色">
                  <n-input v-model:value="selectedItem.staticConfig.borderColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 标题栏配置 -->
              <template v-else-if="selectedItem.staticType === 'headerBar'">
                <n-form-item label="标题">
                  <n-input v-model:value="selectedItem.staticConfig.title" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题字号">
                  <n-input-number v-model:value="selectedItem.staticConfig.titleFontSize" :min="16" :max="60" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.titleColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="显示时间">
                  <n-select :value="selectedItem.staticConfig.showTime ? 'yes' : 'no'" :options="[{label:'是',value:'yes'},{label:'否',value:'no'}]" @update:value="(v: string) => { selectedItem!.staticConfig.showTime = v === 'yes'; syncStaticConfigToLayout() }" />
                </n-form-item>
                <n-form-item label="背景">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 进度条配置 -->
              <template v-else-if="selectedItem.staticType === 'progressBar'">
                <n-form-item label="标签">
                  <n-input v-model:value="selectedItem.staticConfig.label" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="百分比">
                  <n-input-number v-model:value="selectedItem.staticConfig.percent" :min="0" :max="100" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数值颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.valueColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 移动端: 导航栏配置 -->
              <template v-else-if="selectedItem.staticType === 'navbar'">
                <n-form-item label="标题">
                  <n-input v-model:value="selectedItem.staticConfig.title" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="字号">
                  <n-input-number v-model:value="selectedItem.staticConfig.fontSize" :min="12" :max="24" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="文字颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.color" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.bgColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 移动端: 卡片容器配置 -->
              <template v-else-if="selectedItem.staticType === 'mobileCard'">
                <n-form-item label="标题">
                  <n-input v-model:value="selectedItem.staticConfig.title" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="内容">
                  <n-input v-model:value="selectedItem.staticConfig.content" type="textarea" :rows="3" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="圆角">
                  <n-input-number v-model:value="selectedItem.staticConfig.radius" :min="0" :max="24" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.bgColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.titleColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="文字颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.textColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 移动端: 列表项配置 -->
              <template v-else-if="selectedItem.staticType === 'listItem'">
                <n-form-item label="图标">
                  <n-input v-model:value="selectedItem.staticConfig.icon" placeholder="emoji或图标" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题">
                  <n-input v-model:value="selectedItem.staticConfig.title" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="描述">
                  <n-input v-model:value="selectedItem.staticConfig.desc" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.bgColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题颜色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.titleColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 移动端: 搜索栏配置 -->
              <template v-else-if="selectedItem.staticType === 'searchBar'">
                <n-form-item label="占位文本">
                  <n-input v-model:value="selectedItem.staticConfig.placeholder" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="圆角">
                  <n-input-number v-model:value="selectedItem.staticConfig.radius" :min="0" :max="30" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.bgColor" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="输入框色">
                  <n-color-picker v-model:value="selectedItem.staticConfig.inputBg" :modes="['hex']" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 倒计时配置 -->
              <template v-else-if="selectedItem.staticType === 'countdown'">
                <n-form-item label="标签文字">
                  <n-input v-model:value="selectedItem.staticConfig.label" placeholder="如：距活动结束" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="目标秒数" :feedback="'从现在开始计数的秒数，0=不限'">
                  <n-input-number v-model:value="selectedItem.staticConfig.seconds" :min="0" style="width:100%" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数字大小">
                  <n-input-number v-model:value="selectedItem.staticConfig.digitFontSize" :min="16" :max="80" style="width:100%" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="数值颜色">
                  <n-input v-model:value="selectedItem.staticConfig.valueColor" placeholder="#00d4ff" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标签颜色">
                  <n-input v-model:value="selectedItem.staticConfig.labelColor" placeholder="#8ca8c8" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" placeholder="rgba(6,30,60,0.8)" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 滚动列表配置 -->
              <template v-else-if="selectedItem.staticType === 'scrollList'">
                <n-form-item label="标题">
                  <n-input v-model:value="selectedItem.staticConfig.title" placeholder="如：实时排行" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="列表项（每行一条）">
                  <n-input
                    :value="(selectedItem.staticConfig.items || []).join('\n')"
                    type="textarea"
                    :rows="6"
                    placeholder="数据项 1
数据项 2
数据项 3"
                    @update:value="(v) => { selectedItem.staticConfig.items = v.split('\n').filter(Boolean); syncStaticConfigToLayout() }"
                  />
                </n-form-item>
                <n-form-item label="滚动速度(秒/轮)">
                  <n-input-number v-model:value="selectedItem.staticConfig.speed" :min="3" :max="60" style="width:100%" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="文字颜色">
                  <n-input v-model:value="selectedItem.staticConfig.textColor" placeholder="#c8d8e8" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="标题颜色">
                  <n-input v-model:value="selectedItem.staticConfig.titleColor" placeholder="#00d4ff" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" placeholder="rgba(6,30,60,0.8)" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
              <!-- 大屏: 跑马灯配置 -->
              <template v-else-if="selectedItem.staticType === 'marquee'">
                <n-form-item label="滚动文字">
                  <n-input v-model:value="selectedItem.staticConfig.text" type="textarea" :rows="3" placeholder="输入要滚动的文字内容" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="字号">
                  <n-input-number v-model:value="selectedItem.staticConfig.fontSize" :min="12" :max="48" style="width:100%" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="滚动速度(秒/轮)">
                  <n-input-number v-model:value="selectedItem.staticConfig.speed" :min="4" :max="120" style="width:100%" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="文字颜色">
                  <n-input v-model:value="selectedItem.staticConfig.textColor" placeholder="#00d4ff" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
                <n-form-item label="背景色">
                  <n-input v-model:value="selectedItem.staticConfig.bgColor" placeholder="rgba(6,30,60,0.8)" @update:value="syncStaticConfigToLayout" />
                </n-form-item>
              </template>
            </n-form>
          </template>
          
          <!-- 图表配置（内联/引用统一使用 InlineChartEditor） -->
          <template v-else-if="selectedItem">
          <InlineChartEditor
              :item="selectedItem"
              :data-sources="dataSources"
              @update:config="handleInlineConfigUpdate"
              @update:position="handleEditorPositionChange"
              @update:size="handleEditorSizeChange"
              @layer="handleEditorLayerAction"
              @remove="handleRemoveChart(selectedItem.i)"
              @convert-to-public="handleConvertToPublic"
              @preview-data="handleInlinePreviewData"
              @params-detected="handleInlineParamsDetected"
            />
          </template>
          
          <!-- 空状态 -->
          <n-empty v-else description="请选择图表或查询组件" size="small" />
        </n-card>
      </div>
    </div>
    <!-- 版本历史弹窗 -->
    <n-modal v-model:show="showVersionsModal" preset="card" title="版本历史" style="width: 600px">
      <n-spin :show="loadingVersions">
        <n-empty v-if="pageVersions.length === 0 && !loadingVersions" description="暂无版本记录" />
        <n-list v-else bordered>
          <n-list-item v-for="ver in pageVersions" :key="ver.id">
            <n-thing :title="`版本 ${ver.versionNumber || ver.id}`" :description="ver.createdAt || ver.createTime">
              <template #header-extra>
                <n-button size="small" type="primary" :loading="restoringVersion" @click="handleRestoreVersion(ver.id)">恢复</n-button>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </n-spin>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, nextTick, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NInput,
  NCard,
  NSpace,
  NText,
  NIcon,
  NEmpty,
  NSpin,
  NSelect,
  NTag,
  NTabs,
  NTab,
  NForm,
  NFormItem,
  NInputNumber,
  NColorPicker,
  useMessage,
  useDialog
} from 'naive-ui'
import {
  BarChartOutline,
  CloseOutline,
  PieChartOutline,
  TrendingUpOutline,
  StatsChartOutline,
  PulseOutline,
  SaveOutline,
  ArrowBackOutline,
  GridOutline,
  AppsOutline,
  FilterOutline,
  ExpandOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  SettingsOutline,
  ScanOutline
} from '@vicons/ionicons5'
import VueDraggableResizable from 'vue3-draggable-resizable'
import 'vue3-draggable-resizable/dist/Vue3DraggableResizable.css'
import { getChartDefinitionList, getChartData, getChartDefinitionById } from '@/api/chart'
import { getPageDefinitionById, createPageDefinition, updatePageDefinition } from '@/api/page'
import { getPageVersions, restorePageVersion } from '@/api/pageDesigner'
import { getDataSourceList } from '@/api/dataSource'
import { TimeOutline } from '@vicons/ionicons5'
import { handleApiError } from '@/utils/error'
import { buildChartOption } from '@/utils/chartRenderer'
import { getPageData } from '@/types/api'
import type { ChartDefinition } from '@/types/chart'
import type { PageDefinition, PageChart, InlineChartConfig, PageLayoutMode, BigscreenConfig } from '@/types/page'
import { PAGE_THEMES, createDefaultInlineConfig, createDefaultBigscreenConfig } from '@/types/page'
import { BIGSCREEN_TEMPLATES, type BigscreenTemplate } from './bigscreenTemplates'
import { getSampleChartConfig } from './bigscreenSampleData'
import { MOBILE_TEMPLATES, type MobileTemplate } from './mobileTemplates'
import { DESKTOP_TEMPLATES, TEMPLATE_CATEGORIES, type DesktopTemplate, type TemplateCategory } from './desktopTemplates'
import { convertInlineToPublic, testInlineChartSql, getPageTemplates } from '@/api/pageDesigner'
import { buildInlineChartOption } from '@/utils/chartRenderer'
import type { QueryComponent, PageParameterPanel } from '@/types/pageParameter'
import { createDefaultParameterPanel } from '@/types/pageParameter'
import ParameterPanel from './ParameterPanel.vue'
import QueryComponentConfig from './QueryComponentConfig.vue'
import ComponentLibrary from './ComponentLibrary.vue'
import ChartLibraryPanel from './ChartLibraryPanel.vue'
import AiDesignAssistant from './AiDesignAssistant.vue'
import InlineChartEditor from './InlineChartEditor.vue'
import echarts from '@/utils/echarts'
import { useTabsStore } from '@/stores/tabs'

const message = useMessage()
const dialog = useDialog()
const router = useRouter()
const route = useRoute()
const tabsStore = useTabsStore()

// 图标组件映射
const iconComponents: Record<string, any> = {
  BarChartOutline,
  PieChartOutline,
  TrendingUpOutline,
  StatsChartOutline,
  PulseOutline
}

const getChartIconComponent = (chartType: string) => {
  const iconMap: Record<string, string> = {
    bar: 'BarChartOutline',
    pie: 'PieChartOutline',
    line: 'TrendingUpOutline',
    scatter: 'StatsChartOutline',
    radar: 'PulseOutline',
    table: 'GridOutline'
  }
  return iconComponents[iconMap[chartType] || 'BarChartOutline'] || BarChartOutline
}

// 获取图表类型（支持内联图表）
const getChartType = (chartId: number | undefined, item?: any): string => {
  if (item?.mode === 'inline' && item?.inlineConfig) {
    return item.inlineConfig.chartType || 'bar'
  }
  if (chartId) {
    const chart = charts.value.find(c => c.id === chartId)
    return chart?.chartType || 'bar'
  }
  return 'bar'
}

// 判断是否为表格类型
const isTableType = (chartId: number | undefined, item?: any): boolean => {
  const chartType = getChartType(chartId, item)
  return chartType === 'table' || chartType === 'summaryTable' || chartType === 'pivotTable'
}

// 获取表格类型标签
const getTableTypeLabel = (chartId: number | undefined, item?: any): string => {
  const chartType = getChartType(chartId, item)
  if (chartType === 'summaryTable') return '汇总表'
  if (chartType === 'pivotTable') return '透视表'
  return '数据表格'
}

// 版本历史相关
const showVersionsModal = ref(false)
const loadingVersions = ref(false)
const restoringVersion = ref(false)
const pageVersions = ref<any[]>([])

const loadPageVersions = async () => {
  if (!form.id) { message.warning('请先保存页面'); return }
  showVersionsModal.value = true
  loadingVersions.value = true
  try {
    const res = await getPageVersions(form.id)
    pageVersions.value = res.data || []
  } catch (error) {
    console.error('加载版本历史失败', error)
  } finally {
    loadingVersions.value = false
  }
}

const handleRestoreVersion = async (versionId: number) => {
  if (!form.id) return
  restoringVersion.value = true
  try {
    await restorePageVersion(form.id, versionId)
    message.success('版本已恢复，即将重新加载')
    showVersionsModal.value = false
    // 重新加载页面数据
    window.location.reload()
  } catch (error: any) {
    message.error(error.message || '恢复失败')
  } finally {
    restoringVersion.value = false
  }
}

// 模板相关
const pageTemplates = ref<Array<{ id: string; name: string; description: string; layoutConfig?: any; charts?: any[] }>>([])
const templatesLoading = ref(false)

const loadTemplates = async () => {
  templatesLoading.value = true
  try {
    const res = await getPageTemplates()
    pageTemplates.value = res.data || []
  } catch (e) {
    // 静默失败
  } finally {
    templatesLoading.value = false
  }
}

const applyTemplate = async (templateId: string) => {
  // 找到选中的模板
  const tpl = pageTemplates.value.find((t: any) => t.id === templateId)
  if (!tpl) {
    message.error('模板不存在')
    return
  }
  
  // 如果当前是新建页面且画布为空，直接应用模板到当前画布
  if (!form.id && layout.value.length === 0) {
    try {
      // 应用模板的布局配置
      if (tpl.layoutConfig) {
        const layoutCfg = typeof tpl.layoutConfig === 'string' ? JSON.parse(tpl.layoutConfig) : tpl.layoutConfig
        if (layoutCfg.width) canvasWidth.value = layoutCfg.width
        if (layoutCfg.height) canvasHeight.value = layoutCfg.height
      }
      // 应用模板的图表到画布
      if (tpl.charts && Array.isArray(tpl.charts)) {
        const newItems: any[] = []
        for (const chart of tpl.charts) {
          const itemId = `tpl_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
          const item: any = {
            i: itemId,
            mode: chart.mode || 'inline',
            left: chart.left || 0,
            top: chart.top || 0,
            width: chart.width || 300,
            height: chart.height || 200,
            sortOrder: newItems.length
          }
          if (chart.inlineConfig) {
            item.inlineConfig = typeof chart.inlineConfig === 'string' ? JSON.parse(chart.inlineConfig) : chart.inlineConfig
          }
          if (chart.chartId) {
            item.chartId = chart.chartId
          }
          newItems.push(item)
        }
        layout.value = newItems
      }
      message.success(`已应用模板「${tpl.name}」`)
      nextTick(() => handleFitToView())
    } catch (error: any) {
      message.error('模板应用失败: ' + (error.message || '未知错误'))
    }
    return
  }
  
  // 如果画布已有内容，提示用户选择
  dialog.warning({
    title: '使用模板',
    content: '当前画布已有内容，使用模板将替换现有内容。是否继续？',
    positiveText: '确定替换',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 应用模板的布局配置
        if (tpl.layoutConfig) {
          const layoutCfg = typeof tpl.layoutConfig === 'string' ? JSON.parse(tpl.layoutConfig) : tpl.layoutConfig
          if (layoutCfg.width) canvasWidth.value = layoutCfg.width
          if (layoutCfg.height) canvasHeight.value = layoutCfg.height
        }
        // 应用模板的图表到画布
        if (tpl.charts && Array.isArray(tpl.charts)) {
          const newItems: any[] = []
          for (const chart of tpl.charts) {
            const itemId = `tpl_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
            const item: any = {
              i: itemId,
              mode: chart.mode || 'inline',
              left: chart.left || 0,
              top: chart.top || 0,
              width: chart.width || 300,
              height: chart.height || 200,
              sortOrder: newItems.length
            }
            if (chart.inlineConfig) {
              item.inlineConfig = typeof chart.inlineConfig === 'string' ? JSON.parse(chart.inlineConfig) : chart.inlineConfig
            }
            if (chart.chartId) {
              item.chartId = chart.chartId
            }
            newItems.push(item)
          }
          layout.value = newItems
        }
        selectedItem.value = null
        message.success(`已应用模板「${tpl.name}」`)
        nextTick(() => handleFitToView())
      } catch (error: any) {
        message.error('模板应用失败: ' + (error.message || '未知错误'))
      }
    }
  })
}

// ==================== 大屏布局模板 ====================
const bigscreenTemplateList = BIGSCREEN_TEMPLATES

const createTemplateKpiConfig = (
  label?: string,
  options: { dark?: boolean; compact?: boolean } = {}
) => {
  const isPercentMetric = /率|比|ROI|完成|达成|成功|负载|使用|满意/.test(label || '')
  const value = isPercentMetric
    ? String(Math.floor(Math.random() * 45 + 50))
    : String(Math.floor(Math.random() * 9000 + 1000).toLocaleString())
  const dark = options.dark === true
  return {
    label: label || 'KPI指标',
    value,
    prefix: '',
    suffix: isPercentMetric ? '%' : '',
    valueFontSize: options.compact ? 24 : 30,
    bgColor: dark ? 'rgba(6,30,60,0.8)' : '#ffffff',
    borderColor: dark ? 'rgba(64,158,255,0.4)' : '#e5e7eb',
    labelColor: dark ? '#8ca8c8' : '#64748b',
    valueColor: dark ? '#00d4ff' : '#1f2937'
  }
}

const getTemplateStaticData = (chartType?: string) => {
  if (chartType === 'table') {
    return [
      { name: '华东一区', category: '重点客户', amount: 128, rate: '32%' },
      { name: '华南一区', category: '增长客户', amount: 116, rate: '29%' },
      { name: '华北一区', category: '稳定客户', amount: 104, rate: '26%' },
      { name: '西南一区', category: '潜力客户', amount: 88, rate: '22%' },
      { name: '海外业务', category: '新增客户', amount: 64, rate: '16%' }
    ]
  }
  return undefined
}

const createTemplateInlineConfig = (chartType: string, label?: string) => {
  const inlineConfig = createDefaultInlineConfig(chartType)
  inlineConfig.chartName = label || inlineConfig.chartName
  inlineConfig.chartConfig = getSampleChartConfig(chartType, label)
  const staticData = getTemplateStaticData(chartType)
  if (staticData) {
    ;(inlineConfig as any).staticData = staticData
    ;(inlineConfig as any).chartConfig = {
      _isTable: true,
      tableStyle: {
        striped: true,
        bordered: true,
        showPagination: true,
        pageSize: 5,
        showIndex: true,
        displayColumns: ['name', 'category', 'amount', 'rate'],
        columnLabels: {
          name: '名称',
          category: '分类',
          amount: '数值',
          rate: '占比'
        }
      }
    }
  }
  return inlineConfig
}

const isBigscreenMetricPlaceholder = (item: BigscreenTemplate['items'][number]) => (
  item.type === 'inline' &&
  item.chartType === 'gauge' &&
  item.height <= 180 &&
  (/KPI|指标|收入|产量|完成|达成|率|数|利润|订单|客户|满意|库存/.test(item.label || '') || item.top <= 120)
)

const applyBigscreenTemplate = (tpl: BigscreenTemplate) => {
  const doApply = () => {
    canvasWidth.value = tpl.width
    canvasHeight.value = tpl.height
    canvasSize.value = `${tpl.width}x${tpl.height}`
    
    const newItems: any[] = []
    for (const item of tpl.items) {
      const itemId = `bs_${Date.now()}_${Math.random().toString(36).slice(2, 8)}_${newItems.length}`
      if (isBigscreenMetricPlaceholder(item)) {
        newItems.push({
          i: itemId,
          mode: 'static',
          staticType: 'kpiCard',
          staticLabel: item.label,
          staticConfig: createTemplateKpiConfig(item.label, { dark: true }),
          left: item.left,
          top: item.top,
          width: item.width,
          height: Math.max(100, item.height),
          sortOrder: newItems.length
        })
      } else if (item.type === 'inline' && item.chartType) {
        const inlineConfig = createTemplateInlineConfig(item.chartType, item.label)
        newItems.push({
          i: itemId,
          mode: 'inline',
          inlineConfig,
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      } else if (item.type === 'static' && item.staticType) {
        const bsStaticDefaults: Record<string, any> = {
          headerBar: { title: item.label || '数据可视化大屏', titleFontSize: 28, titleColor: '#00d4ff', timeColor: '#5b8fae', showTime: true, bgColor: 'linear-gradient(90deg, rgba(6,30,60,0.9) 0%, rgba(12,21,39,0.95) 50%, rgba(6,30,60,0.9) 100%)' },
          kpiCard: createTemplateKpiConfig(item.label, { dark: true }),
          numberFlipper: { label: item.label || '数据指标', value: String(Math.floor(Math.random() * 90000 + 10000)), unit: '件', digitFontSize: 48, bgColor: 'rgba(6,30,60,0.8)', labelColor: '#8ca8c8', digitColor: '#00ffcc', unitColor: '#5b8fae' },
          decorBorder: { title: item.label || '模块', borderColor: 'rgba(64,158,255,0.6)', bgColor: 'rgba(6,30,60,0.5)', titleColor: '#00d4ff' },
          progressBar: { label: item.label || '进度', percent: Math.floor(Math.random() * 40 + 50), bgColor: 'rgba(6,30,60,0.8)', labelColor: '#8ca8c8', valueColor: '#00d4ff', barColor: 'linear-gradient(90deg, #00d4ff, #00ffcc)' }
        }
        newItems.push({
          i: itemId,
          mode: 'static',
          staticType: item.staticType,
          staticLabel: item.label,
          staticConfig: bsStaticDefaults[item.staticType] || {},
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      }
    }
    layout.value = newItems
    selectedItem.value = null
    message.success(`已应用大屏模板「${tpl.name}」`)
    nextTick(() => { debouncedResizeCharts(); handleFitToView() })
  }
  
  if (layout.value.length > 0) {
    dialog.warning({
      title: '应用大屏模板',
      content: '当前画布已有内容，应用模板将替换现有内容。是否继续？',
      positiveText: '确定替换',
      negativeText: '取消',
      onPositiveClick: doApply
    })
  } else {
    doApply()
  }
}

// ==================== 移动端布局模板 ====================
const mobileTemplateList = MOBILE_TEMPLATES

const applyMobileTemplate = (tpl: MobileTemplate) => {
  const doApply = () => {
    canvasWidth.value = tpl.width
    canvasHeight.value = tpl.height
    canvasSize.value = `${tpl.width}x${tpl.height}`
    
    const newItems: any[] = []
    for (const item of tpl.items) {
      const itemId = `mb_${Date.now()}_${Math.random().toString(36).slice(2, 8)}_${newItems.length}`
      if (item.type === 'inline' && item.chartType === 'kpi') {
        newItems.push({
          i: itemId,
          mode: 'static',
          staticType: 'kpiCard',
          staticLabel: item.label,
          staticConfig: createTemplateKpiConfig(item.label, { compact: true }),
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      } else if (item.type === 'inline' && item.chartType) {
        const inlineConfig = createTemplateInlineConfig(item.chartType, item.label)
        newItems.push({
          i: itemId,
          mode: 'inline',
          inlineConfig,
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      } else if (item.type === 'static' && item.staticType) {
        const staticDefaults: Record<string, any> = {
          title: { text: item.label || '标题', fontSize: 18, fontWeight: 'bold', color: '#333', align: 'left' },
          divider: { style: 'solid', color: '#e0e0e0', thickness: 1 }
        }
        newItems.push({
          i: itemId,
          mode: 'static',
          staticType: item.staticType,
          staticLabel: item.label,
          staticConfig: staticDefaults[item.staticType] || {},
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      }
    }
    layout.value = newItems
    selectedItem.value = null
    message.success(`已应用移动端模板「${tpl.name}」`)
    nextTick(() => { debouncedResizeCharts(); handleFitToView() })
  }
  
  if (layout.value.length > 0) {
    dialog.warning({
      title: '应用移动端模板',
      content: '当前画布已有内容，应用模板将替换现有内容。是否继续？',
      positiveText: '确定替换',
      negativeText: '取消',
      onPositiveClick: doApply
    })
  } else {
    doApply()
  }
}

// ==================== 桌面端布局模板 ====================
const selectedTemplateCategory = ref<TemplateCategory | 'all'>('all')
const templateCategoryOptions = TEMPLATE_CATEGORIES
const filteredDesktopTemplates = computed(() => {
  if (selectedTemplateCategory.value === 'all') return DESKTOP_TEMPLATES
  return DESKTOP_TEMPLATES.filter(t => t.category === selectedTemplateCategory.value)
})

const isDesktopMetricPlaceholder = (item: DesktopTemplate['items'][number]) => (
  item.type === 'inline' &&
  item.chartType === 'bar' &&
  item.top <= 100 &&
  item.height <= 120 &&
  item.width <= 420
)

const createDesktopKpiConfig = (label?: string) => createTemplateKpiConfig(label)

const applyDesktopTemplate = (tpl: DesktopTemplate) => {
  const doApply = () => {
    canvasWidth.value = tpl.width
    canvasHeight.value = tpl.height
    canvasSize.value = `${tpl.width}x${tpl.height}`
    
    const newItems: any[] = []
    for (const item of tpl.items) {
      const itemId = `dk_${Date.now()}_${Math.random().toString(36).slice(2, 8)}_${newItems.length}`
      if (isDesktopMetricPlaceholder(item)) {
        newItems.push({
          i: itemId,
          mode: 'static',
          staticType: 'kpiCard',
          staticLabel: item.label,
          staticConfig: createDesktopKpiConfig(item.label),
          left: item.left,
          top: item.top,
          width: item.width,
          height: Math.max(100, item.height),
          sortOrder: newItems.length
        })
      } else if (item.type === 'inline' && item.chartType) {
        const inlineConfig = createTemplateInlineConfig(item.chartType, item.label)
        newItems.push({
          i: itemId,
          mode: 'inline',
          inlineConfig,
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      } else if (item.type === 'static' && item.staticType) {
        const staticDefaults: Record<string, any> = {
          title: { text: item.label || '标题', fontSize: 20, fontWeight: 'bold', color: '#333', align: 'left' },
          divider: { style: 'solid', color: '#e0e0e0', thickness: 1 },
          kpiCard: createDesktopKpiConfig(item.label)
        }
        newItems.push({
          i: itemId,
          mode: 'static',
          staticType: item.staticType,
          staticLabel: item.label,
          staticConfig: staticDefaults[item.staticType] || {},
          left: item.left,
          top: item.top,
          width: item.width,
          height: item.height,
          sortOrder: newItems.length
        })
      }
    }
    layout.value = newItems
    selectedItem.value = null
    message.success(`已应用桌面端模板「${tpl.name}」`)
    nextTick(() => { debouncedResizeCharts(); handleFitToView() })
  }
  
  if (layout.value.length > 0) {
    dialog.warning({
      title: '应用桌面端模板',
      content: '当前画布已有内容，应用模板将替换现有内容。是否继续？',
      positiveText: '确定替换',
      negativeText: '取消',
      onPositiveClick: doApply
    })
  } else {
    doApply()
  }
}

// 主题选项
const themeOptions = PAGE_THEMES.map(theme => ({
  label: theme.name,
  value: theme.value
}))

// 当前主题的背景色（计算属性）- 大屏默认深色背景
const currentThemeBackground = computed(() => {
  const theme = PAGE_THEMES.find(t => t.value === form.theme)
  return theme ? theme.backgroundColor : '#ffffff'
})

// 表单数据
const form = reactive({
  id: undefined as number | undefined,
  pageName: '',
  pageCode: '',
  description: '',
  theme: 'default' as string,
  layoutMode: 'desktop' as PageLayoutMode,
  bigscreenConfig: undefined as BigscreenConfig | undefined
})

// 参数面板相关
const showParameterPanel = ref(false)
const parameterPanel = reactive<PageParameterPanel>(createDefaultParameterPanel())
const selectedQueryComponent = ref<QueryComponent | null>(null)
const dataSources = ref<Array<{ id: number; name: string }>>([])

// 更新参数组件列表
const handleParameterComponentsUpdate = (components: QueryComponent[]) => {
  parameterPanel.components = components
}

// 选中查询组件
const handleSelectQueryComponent = (comp: QueryComponent | null) => {
  selectedQueryComponent.value = comp
  if (comp) {
    selectedItem.value = null // 取消图表选中
  }
}

// 更新查询组件配置
const handleQueryComponentUpdate = (comp: QueryComponent) => {
  const index = parameterPanel.components.findIndex(c => c.id === comp.id)
  if (index !== -1) {
    parameterPanel.components[index] = comp
  }
  selectedQueryComponent.value = comp
}

// 图表及其字段（用于关联配置）
const chartsWithFields = computed(() => {
  return charts.value.map(chart => {
    // 从图表配置中提取字段
    const fields: string[] = []
    if (chart.chartConfig) {
      try {
        const config = JSON.parse(chart.chartConfig)
        // 从字段映射中提取
        if (config.metadata?.fieldMapping) {
          const mapping = config.metadata.fieldMapping
          if (mapping.xAxis) fields.push(mapping.xAxis)
          if (mapping.yAxis) {
            const yAxisFields = Array.isArray(mapping.yAxis) ? mapping.yAxis : [mapping.yAxis]
            fields.push(...yAxisFields.filter(Boolean))
          }
          if (mapping.group) fields.push(mapping.group)
        }
        // 从查询参数中提取字段
        if (config.queryParameters && Array.isArray(config.queryParameters)) {
          config.queryParameters.forEach((param: any) => {
            if (param.field) fields.push(param.field)
          })
        }
        // 从 metadata.chartParameters 中提取字段
        if (config.metadata?.chartParameters && Array.isArray(config.metadata.chartParameters)) {
          config.metadata.chartParameters.forEach((param: any) => {
            if (param.field) fields.push(param.field)
          })
        }
      } catch (e) {
        console.warn('解析图表配置失败:', chart.chartName, e)
      }
    }
    
    // 如果没有从配置中获取到字段，尝试从 SQL 中提取
    if (fields.length === 0 && chart.sqlContent) {
      try {
        // 简单的 SQL 字段提取：匹配 SELECT 和 FROM 之间的字段
        const sqlUpper = chart.sqlContent.toUpperCase()
        const selectIndex = sqlUpper.indexOf('SELECT')
        const fromIndex = sqlUpper.indexOf('FROM')
        if (selectIndex !== -1 && fromIndex !== -1 && fromIndex > selectIndex) {
          const selectPart = chart.sqlContent.substring(selectIndex + 6, fromIndex).trim()
          // 分割字段，处理别名
          const fieldParts = selectPart.split(',').map(f => f.trim())
          fieldParts.forEach(part => {
            // 移除聚合函数和别名，提取字段名
            let fieldName = part
            // 处理 AS 别名
            const asMatch = part.match(/\s+AS\s+[`']?(\w+)[`']?$/i)
            if (asMatch) {
              fieldName = asMatch[1]!
            } else {
              // 处理聚合函数如 SUM(`field`)
              const funcMatch = part.match(/\w+\s*\(\s*[`']?(\w+)[`']?\s*\)/i)
              if (funcMatch) {
                fieldName = funcMatch[1]!
              } else {
                // 处理简单字段 `field` 或 field
                const simpleMatch = part.match(/[`']?(\w+)[`']?$/i)
                if (simpleMatch) {
                  fieldName = simpleMatch[1]!
                }
              }
            }
            if (fieldName && fieldName !== '*') {
              fields.push(fieldName)
            }
          })
        }
      } catch (e) {
        console.warn('从 SQL 提取字段失败:', chart.chartName, e)
      }
    }
    
    return {
      id: chart.id,
      chartName: chart.chartName,
      fields: [...new Set(fields)] // 去重
    }
  })
})

// 加载数据源列表
const loadDataSources = async () => {
  try {
    const res = await getDataSourceList({ pageSize: 100 })
    if (res?.data?.list) {
      dataSources.value = res.data.list.map((ds: any) => ({
        id: ds.id,
        name: ds.name
      }))
    }
  } catch (e) {
    console.warn('加载数据源失败:', e)
  }
}

// 画布设置
const showGrid = ref(true)
const canvasSize = ref('1920x1080')
const canvasWidth = ref(1920)
const canvasHeight = ref(1080)

// 模式感知的画布参数
const canvasExtraPadding = computed(() => {
  if (form.layoutMode === 'mobile') return 40
  if (form.layoutMode === 'bigscreen') return 30
  return 20
})
const currentGridSize = computed(() => {
  if (form.layoutMode === 'mobile') return 10
  if (form.layoutMode === 'bigscreen') return 40
  return 20
})

// 面板折叠状态
const leftPanelCollapsed = ref(false)
const rightPanelCollapsed = ref(false)
const isFullscreenDesign = ref(false)

// 计算自适应画布尺寸
const calculateAutoCanvasSize = () => {
  const container = document.querySelector('.canvas-wrapper')
  if (container) {
    const rect = container.getBoundingClientRect()
    // 减去一些边距
    const newWidth = Math.floor(rect.width - 40)
    const newHeight = Math.floor(rect.height - 40)
    if (newWidth > 0 && newHeight > 0) {
      canvasWidth.value = newWidth
      canvasHeight.value = newHeight
      return true
    }
  }
  return false
}

// 切换全屏设计模式
const toggleFullscreenDesign = () => {
  isFullscreenDesign.value = !isFullscreenDesign.value
  if (isFullscreenDesign.value) {
    // 进入全屏模式时，自动折叠左右面板，设置缩放为100%
    leftPanelCollapsed.value = true
    rightPanelCollapsed.value = true
    zoomLevel.value = 100
    
    // 延迟计算画布尺寸，等待面板折叠动画完成
    setTimeout(() => {
      calculateAutoCanvasSize()
      nextTick(() => {
        resizeAllCharts()
      })
    }, 350)
  } else {
    // 退出全屏模式时，展开面板，恢复默认尺寸
    leftPanelCollapsed.value = false
    rightPanelCollapsed.value = false
    canvasWidth.value = 1200
    canvasHeight.value = 800
    canvasSize.value = '1200x800'
    
    nextTick(() => {
      resizeAllCharts()
    })
  }
}

// 按布局模式提供不同的画布尺寸选项
const canvasSizeOptions = computed(() => {
  const mode = form.layoutMode
  if (mode === 'mobile') {
    return [
      { label: '375×812 (iPhone X)', value: '375x812' },
      { label: '390×844 (iPhone 14)', value: '390x844' },
      { label: '414×896 (iPhone XR)', value: '414x896' },
      { label: '360×780 (Android)', value: '360x780' },
      { label: '768×1024 (iPad竖屏)', value: '768x1024' },
      { label: '1024×768 (iPad横屏)', value: '1024x768' },
      { label: '自定义', value: 'custom' }
    ]
  }
  if (mode === 'bigscreen') {
    return [
      { label: '1920×1080 (全高清)', value: '1920x1080' },
      { label: '2560×1440 (2K)', value: '2560x1440' },
      { label: '3840×2160 (4K)', value: '3840x2160' },
      { label: '2560×1080 (超宽屏)', value: '2560x1080' },
      { label: '3440×1440 (超宽2K)', value: '3440x1440' },
      { label: '自适应 (当前窗口)', value: 'auto' },
      { label: '自定义', value: 'custom' }
    ]
  }
  // 桌面端
  return [
    { label: '自适应 (当前窗口)', value: 'auto' },
    { label: '1920×1080 (全高清)', value: '1920x1080' },
    { label: '1366×768 (笔记本)', value: '1366x768' },
    { label: '1440×900 (宽屏)', value: '1440x900' },
    { label: '1200×800 (经典)', value: '1200x800' },
    { label: '自定义', value: 'custom' }
  ]
})

// 撤销/重做历史
const historyStack = ref<any[][]>([])
const historyIndex = ref(-1)
const maxHistorySize = 50

const submitting = ref(false)
const leftPanelTab = ref<'components' | 'charts' | 'templates' | 'ai'>('components')  // 四Tab面板
const charts = ref<ChartDefinition[]>([])
const layout = ref<any[]>([])
const selectedItem = ref<any>(null)
const editingItemId = ref<string | null>(null)
const zoomLevel = ref(100)
const isDraggingOver = ref(false)
const canvasRef = ref<HTMLElement | null>(null)
const canvasWrapperRef = ref<HTMLElement | null>(null)

// 图表预览相关 (使用string作为key，因为布局项的i是string类型)
const chartInstances = new Map<string, echarts.ECharts>()
const chartLoadingMap = ref<Record<string, boolean>>({})
const chartDefinitions = ref<Map<number, ChartDefinition>>(new Map())
const chartInstanceTimers = new Map<string, NodeJS.Timeout>()
const chartDataCache = new Map<number, any[]>() // 图表数据缓存
const chartLoadingPromises = new Map<number, Promise<any>>() // 正在加载的Promise，防止重复请求
const inlineChartDataMap = new Map<string, any[]>() // 内联图表数据缓存
const inlineChartParamsMap = new Map<string, string[]>() // 内联图表参数映射

// 防止频繁触发的标志
const isDraggingOrResizing = ref(false)

// 对齐辅助线（拖拽时显示）
const snapLines = ref<{ x: number[]; y: number[] }>({ x: [], y: [] })
const SNAP_THRESHOLD = 6

// 验证布局项
const validateLayoutItem = (item: any): boolean => {
  if (!item || typeof item !== 'object') return false
  if (typeof item.i !== 'string' && typeof item.i !== 'number') return false
  // 内联图表无 chartId，引用图表有 chartId，静态组件也无 chartId
  if (item.mode !== 'inline' && item.mode !== 'static' && typeof item.chartId !== 'number') return false
  if (typeof item.left !== 'number' || item.left < 0) return false
  if (typeof item.top !== 'number' || item.top < 0) return false
  // 静态组件（标题/分割线/文本等）可以很小很矮，不能套用图表的最小尺寸限制
  const minW = item.mode === 'static' ? 20 : 100
  const minH = item.mode === 'static' ? 16 : 100
  if (typeof item.width !== 'number' || item.width < minW) return false
  if (typeof item.height !== 'number' || item.height < minH) return false
  return true
}

// 清理布局数据
const cleanLayout = (items: any[]): any[] => {
  return items
    .filter(validateLayoutItem)
    .map(item => ({
      i: String(item.i),
      chartId: item.chartId != null ? Number(item.chartId) : undefined,
      mode: item.mode || (item.chartId ? 'referenced' : 'inline'),
      inlineConfig: item.inlineConfig || undefined,
      staticType: item.staticType || undefined,
      staticConfig: item.staticConfig || undefined,
      left: Number(item.left) || 0,
      top: Number(item.top) || 0,
      width: Number(item.width) || 300,
      height: Number(item.height) || 200
    }))
}

// 有效的布局项
const validLayout = computed(() => {
  return cleanLayout(layout.value)
})

const getItemMinWidth = (item?: any) => {
  if (item?.mode === 'static') {
    if (item.staticType === 'title' || item.staticType === 'divider' || item.staticType === 'marquee') return 50
    if (item.staticType === 'kpiCard' || item.staticType === 'progressBar' || item.staticType === 'countdown') return 120
    if (['statusBar', 'navbar', 'tabBar', 'mobileCard', 'listItem', 'searchBar'].includes(item.staticType)) return 80
  }
  return 150
}

const getItemMinHeight = (item?: any) => {
  if (item?.mode === 'static') {
    if (item.staticType === 'divider') return 20
    if (item.staticType === 'title' || item.staticType === 'marquee') return 30
    if (item.staticType === 'statusBar' || item.staticType === 'navbar' || item.staticType === 'searchBar') return 36
    if (item.staticType === 'tabBar' || item.staticType === 'listItem') return 44
    if (item.staticType === 'kpiCard' || item.staticType === 'progressBar') return 80
  }
  return 120
}

const normalizeItemSize = (item: any, width?: number, height?: number) => ({
  width: Math.max(getItemMinWidth(item), Math.round(Number(width ?? item?.width) || 300)),
  height: Math.max(getItemMinHeight(item), Math.round(Number(height ?? item?.height) || 200))
})

// 加载图表列表
const chartsLoading = ref(true)
const loadCharts = async () => {
  chartsLoading.value = true
  try {
    const res = await getChartDefinitionList({ pageSize: 1000 })
    const chartList = getPageData<ChartDefinition>(res)
    charts.value = chartList.filter(c => c.status === 1)
  } catch (error) {
    handleApiError(error, '加载图表列表')
    charts.value = []
  } finally {
    chartsLoading.value = false
  }
}

// 加载页面定义
const loadPageDefinition = async () => {
  const pageId = route.params["id"] as string
  if (!pageId || pageId === 'new') {
    form.id = undefined
    form.pageName = ''
    form.pageCode = ''
    form.description = ''
    layout.value = []
    // 从查询参数读取布局模式，并设置对应的默认画布尺寸
    const queryLayoutMode = route.query['layoutMode'] as string
    if (queryLayoutMode === 'mobile' || queryLayoutMode === 'bigscreen') {
      form.layoutMode = queryLayoutMode
      if (queryLayoutMode === 'mobile') {
        canvasWidth.value = 390
        canvasHeight.value = 844
        canvasSize.value = '390x844'
      } else {
        form.bigscreenConfig = createDefaultBigscreenConfig()
        form.theme = 'bigscreen-dark'
        canvasWidth.value = 1920
        canvasHeight.value = 1080
        canvasSize.value = '1920x1080'
      }
    } else {
      form.layoutMode = 'desktop'
      form.bigscreenConfig = undefined
      canvasWidth.value = 1200
      canvasHeight.value = 800
      canvasSize.value = '1200x800'
    }
    // 重置参数面板
    showParameterPanel.value = false
    Object.assign(parameterPanel, createDefaultParameterPanel())
    isLayoutLoading.value = false
    return
  }

  isLayoutLoading.value = true
  try {
    const res = await getPageDefinitionById(Number(pageId))
    if (res && res.data) {
      const page = res.data as PageDefinition
      form.id = page.id
      form.pageName = page.pageName || ''
      form.pageCode = page.pageCode || ''
      form.description = page.description || ''
      form.theme = page.theme || 'default'
      form.layoutMode = page.layoutMode || 'desktop'
      // 恢复画布尺寸
      if (page.layoutConfig) {
        try {
          const cfg = typeof page.layoutConfig === 'string' ? JSON.parse(page.layoutConfig) : page.layoutConfig
          if (cfg.width > 0 && cfg.height > 0) {
            canvasWidth.value = cfg.width
            canvasHeight.value = cfg.height
            canvasSize.value = `${cfg.width}x${cfg.height}`
          }
        } catch { /* use defaults */ }
      } else {
        // 没有 layoutConfig 时根据布局模式设置默认值
        if (form.layoutMode === 'mobile') {
          canvasWidth.value = 390; canvasHeight.value = 844
        } else if (form.layoutMode === 'bigscreen') {
          canvasWidth.value = 1920; canvasHeight.value = 1080
        } else {
          canvasWidth.value = 1200; canvasHeight.value = 800
        }
        canvasSize.value = `${canvasWidth.value}x${canvasHeight.value}`
      }
      if (page.bigscreenConfig) {
        form.bigscreenConfig = typeof page.bigscreenConfig === 'string'
          ? JSON.parse(page.bigscreenConfig)
          : page.bigscreenConfig
      } else {
        form.bigscreenConfig = undefined
      }
      
      // 加载参数面板配置
      let paramPanelConfig = null
      if (page.parameterPanel) {
        // 如果是字符串，需要解析
        if (typeof page.parameterPanel === 'string') {
          try {
            paramPanelConfig = JSON.parse(page.parameterPanel)
          } catch (e) {
            console.warn('解析参数面板配置失败:', e)
            paramPanelConfig = null
          }
        } else {
          paramPanelConfig = page.parameterPanel
        }
      }
      
      if (paramPanelConfig) {
        showParameterPanel.value = paramPanelConfig.visible !== false
        parameterPanel.height = paramPanelConfig.height || 60
        parameterPanel.components = paramPanelConfig.components || []
        parameterPanel.showQueryButton = paramPanelConfig.showQueryButton !== false
        parameterPanel.showResetButton = paramPanelConfig.showResetButton !== false
        parameterPanel.autoQuery = paramPanelConfig.autoQuery || false
      } else {
        showParameterPanel.value = false
        Object.assign(parameterPanel, createDefaultParameterPanel())
      }
      
      // 加载图表布局
      if (page.charts && Array.isArray(page.charts) && page.charts.length > 0) {
        // 先清理所有图表实例，避免重复加载
        chartInstances.forEach((instance) => {
          try {
            if (isChartInstanceValid(instance)) {
              instance.dispose()
            }
          } catch (e) {
            console.warn('清理图表实例失败:', e)
          }
        })
        chartInstances.clear()
        chartLoadingMap.value = {}
        
        layout.value = page.charts.map((chart: PageChart, index: number) => {
          // 兼容旧格式（网格布局）和新格式（像素布局）
          let left = 0
          let top = 0
          let width = 300
          let height = 200
          
          // 优先使用新格式的像素布局字段
          if (chart.left !== undefined && chart.left !== null) {
            left = Number(chart.left) || 0
          } else if (chart.x !== undefined && chart.x !== null) {
            // 旧格式：网格布局，转换为像素
            const gridSize = 50 // 每个网格单位50px
            left = Number(chart.x) * gridSize
          }
          
          if (chart.top !== undefined && chart.top !== null) {
            top = Number(chart.top) || 0
          } else if (chart.y !== undefined && chart.y !== null) {
            const gridSize = 50
            top = Number(chart.y) * gridSize
          }
          
          if (chart.width !== undefined && chart.width !== null) {
            width = Number(chart.width) || 300
          } else if (chart.w !== undefined && chart.w !== null) {
            const gridSize = 50
            width = Number(chart.w) * gridSize
          }
          
          if (chart.height !== undefined && chart.height !== null) {
            height = Number(chart.height) || 200
          } else if (chart.h !== undefined && chart.h !== null) {
            const gridSize = 50
            height = Number(chart.h) * gridSize
          }
          
          // 解析 inlineConfig（后端返回的可能是 JSON 字符串）
          let parsedInlineConfig = (chart as any).inlineConfig
          if (typeof parsedInlineConfig === 'string') {
            try { parsedInlineConfig = JSON.parse(parsedInlineConfig) } catch { /* keep as-is */ }
          }

          if (chart.mode === 'static' && parsedInlineConfig && height === 120) {
            if (parsedInlineConfig.staticType === 'title') height = 40
            else if (parsedInlineConfig.staticType === 'divider') height = 30
            else if (parsedInlineConfig.staticType === 'marquee') height = 44
          }

          const sizeSource = chart.mode === 'static' && parsedInlineConfig
            ? { ...chart, staticType: parsedInlineConfig.staticType }
            : chart
          
          // 确保位置和大小都是有效值
          left = Math.max(0, Math.round(left))
          top = Math.max(0, Math.round(top))
          const normalizedSize = normalizeItemSize(sizeSource, width, height)
          width = normalizedSize.width
          height = normalizedSize.height
          
          // 静态组件：从 inlineConfig 恢复 staticType 和 staticConfig
          if (chart.mode === 'static' && parsedInlineConfig) {
            return {
              i: `static-${index}-${Date.now()}`,
              mode: 'static' as const,
              staticType: parsedInlineConfig.staticType,
              staticConfig: parsedInlineConfig.staticConfig || {},
              staticLabel: parsedInlineConfig.staticConfig?.label || parsedInlineConfig.staticType,
              left,
              top,
              width,
              height
            }
          }
          
          return {
            i: chart.mode === 'inline'
              ? `inline-${index}-${Date.now()}`
              : `chart-${chart.chartId}-${index}-${Date.now()}`,
            chartId: chart.chartId != null ? Number(chart.chartId) : undefined,
            mode: chart.mode || (chart.chartId ? 'referenced' : 'inline'),
            inlineConfig: parsedInlineConfig || undefined,
            left,
            top,
            width,
            height
          }
        })
        
        // 延迟渲染图表，避免同时加载导致闪烁
        await nextTick()
        setTimeout(() => {
          isLayoutLoading.value = false
          // 触发图表渲染，但使用防抖避免重复
          debouncedResizeCharts()
        }, 200)
      } else {
        layout.value = []
        isLayoutLoading.value = false
      }
    } else {
      layout.value = []
      isLayoutLoading.value = false
    }
  } catch (error) {
    handleApiError(error, '加载页面定义')
    layout.value = []
    isLayoutLoading.value = false
  }
}

// 拖拽进入画布
const handleDragEnter = (event: DragEvent) => {
  event.preventDefault()
  const types = event.dataTransfer?.types || []
  // 支持所有拖拽类型：内联图表、引用图表、查询组件
  if (types.some(t => ['text/plain', 'inlinecharttype', 'chart', 'querycomponent'].includes(t.toLowerCase()))) {
    isDraggingOver.value = true
  }
}

// 拖拽在画布上
const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
}

// 拖拽离开画布
const handleDragLeave = (event: DragEvent) => {
  event.preventDefault()
  const relatedTarget = event.relatedTarget as HTMLElement
  if (!relatedTarget || !canvasRef.value?.contains(relatedTarget)) {
    isDraggingOver.value = false
  }
}

// 放置图表
const handleDrop = async (event: DragEvent) => {
  event.preventDefault()
  event.stopPropagation()
  
  isDraggingOver.value = false
  
  if (!event.dataTransfer || !canvasRef.value) return
  
  const canvasRect = canvasRef.value.getBoundingClientRect()
  const scale = zoomLevel.value / 100
  const dropX = (event.clientX - canvasRect.left) / scale
  const dropY = (event.clientY - canvasRect.top) / scale
  
  // 检查是否为内联图表类型拖入（从组件库）
  const inlineChartType = event.dataTransfer.getData('inlineChartType')
  if (inlineChartType) {
    const isMb = form.layoutMode === 'mobile'
    const isBs = form.layoutMode === 'bigscreen'
    const w = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 500 : 400)
    const h = isMb ? 240 : (isBs ? 360 : 300)
    let x = Math.max(0, dropX - w / 2)
    let y = Math.max(0, dropY - h / 2)
    
    // 碰撞检测：找到不重叠的位置
    if (hasCollision(x, y, w, h)) {
      const validPos = findValidDropPosition(x, y, w, h)
      x = validPos.x
      y = validPos.y
    }
    
    const inlineConfig = createDefaultInlineConfig(inlineChartType)
    const newItem = {
      i: `inline-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
      chartId: undefined,
      mode: 'inline' as const,
      inlineConfig,
      left: x,
      top: y,
      width: w,
      height: h
    }
    layout.value.push(newItem)
    layout.value = [...layout.value]
    saveToHistory()
    setTimeout(() => message.success('内联图表已添加到画布'), 100)
    return
  }
  
  // 检查是否为静态组件拖入（从组件库）
  const staticComponentData = event.dataTransfer.getData('staticComponent')
  if (staticComponentData) {
    try {
      const parsed = JSON.parse(staticComponentData)
      const isBs = form.layoutMode === 'bigscreen'
      const isMb = form.layoutMode === 'mobile'
      const cw = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 400 : 400)
      const defaults: Record<string, { w: number; h: number; config: any }> = {
        title: { w: isMb ? 358 : 400, h: 60, config: { text: '标题文本', fontSize: isBs ? 28 : (isMb ? 18 : 24), fontWeight: 'bold', color: isBs ? '#e0ecff' : '#333', align: 'left' } },
        text: { w: cw, h: isMb ? 80 : 120, config: { text: '这是一段文本内容，可以在右侧属性面板中编辑。', fontSize: isMb ? 14 : 14, color: isBs ? '#8ca8c8' : '#666', align: 'left', lineHeight: 1.6 } },
        divider: { w: isMb ? 358 : 600, h: 30, config: { style: 'solid', color: isBs ? 'rgba(64,158,255,0.4)' : '#e0e0e0', thickness: 1 } },
        image: { w: isMb ? 358 : 300, h: 200, config: { src: '', alt: '图片', objectFit: 'contain' } },
        statusBar: { w: 390, h: 44, config: {} },
        navbar: { w: 390, h: 44, config: { title: '页面标题', bgColor: '#fff', color: '#333', fontSize: 17 } },
        tabBar: { w: 390, h: 50, config: { tabs: [{icon:'🏠',label:'首页'},{icon:'📊',label:'数据'},{icon:'⚙️',label:'设置'},{icon:'👤',label:'我的'}], activeIndex: 0 } },
        mobileCard: { w: 358, h: 120, config: { title: '卡片标题', content: '卡片内容区域，可编辑', bgColor: '#fff', titleColor: '#333', textColor: '#666', radius: 12 } },
        listItem: { w: 358, h: 56, config: { icon: '📋', title: '列表项标题', desc: '描述信息', bgColor: '#fff', titleColor: '#333' } },
        searchBar: { w: 358, h: 44, config: { placeholder: '搜索...', bgColor: '#f5f5f5', inputBg: '#fff', radius: 20 } },
        kpiCard: { w: 300, h: 120, config: { label: 'KPI指标', value: '1,234', prefix: '', suffix: '', valueFontSize: 32, bgColor: 'rgba(6,30,60,0.8)', borderColor: 'rgba(64,158,255,0.4)', labelColor: '#8ca8c8', valueColor: '#00d4ff' } },
        numberFlipper: { w: 500, h: 180, config: { label: '数据指标', value: '09876', unit: '万元', digitFontSize: 48, bgColor: 'rgba(6,30,60,0.8)', labelColor: '#8ca8c8', digitColor: '#00ffcc', unitColor: '#5b8fae' } },
        decorBorder: { w: 460, h: 320, config: { title: '模块标题', borderColor: 'rgba(64,158,255,0.6)', bgColor: 'rgba(6,30,60,0.5)', titleColor: '#00d4ff' } },
        headerBar: { w: 1920, h: 64, config: { title: '数据可视化大屏', titleFontSize: 28, titleColor: '#00d4ff', timeColor: '#5b8fae', showTime: true, bgColor: 'linear-gradient(90deg, rgba(6,30,60,0.9) 0%, rgba(12,21,39,0.95) 50%, rgba(6,30,60,0.9) 100%)' } },
        progressBar: { w: isMb ? 358 : 400, h: 80, config: isBs ? { label: '完成进度', percent: 72, bgColor: 'rgba(6,30,60,0.8)', labelColor: '#8ca8c8', valueColor: '#00d4ff', barColor: 'linear-gradient(90deg, #00d4ff, #00ffcc)' } : { label: '完成进度', percent: 72, bgColor: '#fff', labelColor: '#666', valueColor: '#1890ff', barColor: '#1890ff' } },
        countdown: { w: 360, h: 120, config: { label: '距活动开始', seconds: 86400, digitFontSize: 36, valueColor: '#00d4ff', labelColor: '#8ca8c8', bgColor: 'rgba(6,30,60,0.8)' } },
        scrollList: { w: 420, h: 320, config: { title: '实时排行', items: ['数据项 1', '数据项 2', '数据项 3', '数据项 4', '数据项 5'], speed: 10, textColor: '#c8d8e8', titleColor: '#00d4ff', bgColor: 'rgba(6,30,60,0.8)' } },
        marquee: { w: 720, h: 44, config: { text: '数据可视化大屏 · 实时数据接入 · 多维度分析', fontSize: 16, speed: 12, textColor: '#00d4ff', bgColor: 'rgba(6,30,60,0.8)' } }
      }
      const d = defaults[parsed.type] || defaults['text']!
      let x = Math.max(0, dropX - d.w / 2)
      let y = Math.max(0, dropY - d.h / 2)
      
      // 碰撞检测：找到不重叠的位置
      if (hasCollision(x, y, d.w, d.h)) {
        const validPos = findValidDropPosition(x, y, d.w, d.h)
        x = validPos.x
        y = validPos.y
      }
      
      const newItem = {
        i: `static-${parsed.type}-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
        chartId: undefined,
        mode: 'static' as const,
        staticType: parsed.type,
        staticConfig: d.config,
        left: x,
        top: y,
        width: d.w,
        height: d.h
      }
      layout.value.push(newItem)
      layout.value = [...layout.value]
      saveToHistory()
      setTimeout(() => message.success(`已添加${parsed.label || '静态组件'}到画布`), 100)
    } catch {
      message.error('添加静态组件失败')
    }
    return
  }
  
  // 检查是否为查询组件拖入（从组件库）
  const queryComponentData = event.dataTransfer.getData('queryComponent')
  if (queryComponentData) {
    if (!showParameterPanel.value) {
      showParameterPanel.value = true
    }
    try {
      const comp = JSON.parse(queryComponentData)
      parameterPanel.components.push(comp)
      message.success('查询组件已添加到参数栏')
    } catch {
      message.error('添加查询组件失败')
    }
    return
  }
  
  // 检查是否为引用图表拖入（从图表资源库）
  const chartData = event.dataTransfer.getData('chart')
  if (!chartData) return
  
  let chart: any = null
  try {
    chart = JSON.parse(chartData)
  } catch (parseError) {
    console.error('解析拖拽数据失败:', parseError)
    message.error('解析图表数据失败')
    return
  }
  
  if (!chart || typeof chart !== 'object' || !chart.id) {
    message.error('无效的图表数据')
    return
  }
  
  try {
    await nextTick()
    
    const isMb = form.layoutMode === 'mobile'
    const isBs = form.layoutMode === 'bigscreen'
    const w = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 500 : 400)
    const h = isMb ? 240 : (isBs ? 360 : 300)
    let x = Math.max(0, dropX - w / 2)
    let y = Math.max(0, dropY - h / 2)
    
    // 碰撞检测：找到不重叠的位置
    if (hasCollision(x, y, w, h)) {
      const validPos = findValidDropPosition(x, y, w, h)
      x = validPos.x
      y = validPos.y
    }
    
    const newItem = {
      i: `chart-${chart.id}-${Date.now()}`,
      chartId: chart.id,
      mode: 'referenced' as const,
      left: x,
      top: y,
      width: w,
      height: h
    }
    
    layout.value.push(newItem)
    layout.value = [...layout.value]
    saveToHistory()
    
    setTimeout(() => {
      message.success('图表已添加到画布', { duration: 2000 })
    }, 100)
  } catch (error) {
    console.error('添加图表失败:', error)
    message.error('添加图表失败')
  }
}

// 碰撞检测：判断两个矩形是否重叠
const isOverlapping = (ax: number, ay: number, aw: number, ah: number, bx: number, by: number, bw: number, bh: number) => {
  return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by
}

// 检查位置是否与现有布局项冲突
const hasCollision = (x: number, y: number, w: number, h: number, excludeId?: string) => {
  return layout.value.some(item => {
    if (excludeId && item.i === excludeId) return false
    const ix = Number(item.left) || 0
    const iy = Number(item.top) || 0
    const iw = Number(item.width) || 200
    const ih = Number(item.height) || 200
    return isOverlapping(x, y, w, h, ix, iy, iw, ih)
  })
}

// 为拖放找到有效的不重叠位置
const findValidDropPosition = (x: number, y: number, w: number, h: number): { x: number; y: number } => {
  // 先尝试原位置
  if (!hasCollision(x, y, w, h)) {
    return { x, y }
  }
  
  // 级联偏移寻找空位（向右下方偏移，每次30px）
  const step = 30
  for (let i = 1; i <= 20; i++) {
    const ox = x + step * i
    const oy = y + step * i
    if (ox + w <= canvasWidth.value && oy + h <= canvasHeight.value && !hasCollision(ox, oy, w, h)) {
      return { x: ox, y: oy }
    }
  }
  
  // 仍有碰撞 → 放在所有组件下方
  const gap = 12
  const maxBottom = layout.value.length > 0
    ? Math.max(...layout.value.map(it => (it.top || 0) + (it.height || 0)))
    : 0
  return {
    x: Math.max(0, Math.round((canvasWidth.value - w) / 2)),
    y: maxBottom + gap
  }
}

// 计算画布可见区域中心位置（带碰撞检测，自动寻找空位）
const getCanvasViewportCenter = (itemWidth = 400, itemHeight = 300) => {
  const wrapper = canvasWrapperRef.value
  if (!wrapper) return { x: 100, y: 100 }
  const scale = zoomLevel.value / 100
  const gap = 12

  // 移动端: 水平居中、垂直堆叠在最底部已有组件下方
  if (form.layoutMode === 'mobile') {
    const maxBottom = layout.value.length > 0
      ? Math.max(...layout.value.map(it => (it.top || 0) + (it.height || 0)))
      : 0
    return {
      x: Math.max(0, Math.round((canvasWidth.value - itemWidth) / 2)),
      y: maxBottom > 0 ? maxBottom + gap : 44
    }
  }

  // 桌面/大屏: 先尝试视口中心
  const scrollX = wrapper.scrollLeft / scale
  const scrollY = wrapper.scrollTop / scale
  const viewW = wrapper.clientWidth / scale
  const viewH = wrapper.clientHeight / scale
  const centerX = Math.max(0, Math.round(scrollX + viewW / 2 - itemWidth / 2))
  const centerY = Math.max(0, Math.round(scrollY + viewH / 2 - itemHeight / 2))

  if (!hasCollision(centerX, centerY, itemWidth, itemHeight)) {
    return { x: centerX, y: centerY }
  }

  // 中心有碰撞 → 级联偏移寻找空位（向右下方偏移，每次30px）
  const step = 30
  for (let i = 1; i <= 20; i++) {
    const ox = centerX + step * i
    const oy = centerY + step * i
    if (ox + itemWidth <= canvasWidth.value && !hasCollision(ox, oy, itemWidth, itemHeight)) {
      return { x: ox, y: oy }
    }
  }

  // 仍有碰撞 → 放在所有组件下方
  const maxBottom = layout.value.length > 0
    ? Math.max(...layout.value.map(it => (it.top || 0) + (it.height || 0)))
    : 0
  return {
    x: Math.max(0, Math.round((canvasWidth.value - itemWidth) / 2)),
    y: maxBottom + gap
  }
}

// 从组件库点击添加内联图表
const handleClickAddInlineChartType = (chartType: string) => {
  const isMb = form.layoutMode === 'mobile'
  const isBs = form.layoutMode === 'bigscreen'
  // 模式感知的默认尺寸
  const w = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 500 : 400)
  const h = isMb ? 240 : (isBs ? 360 : 300)
  const pos = getCanvasViewportCenter(w, h)
  const inlineConfig = createDefaultInlineConfig(chartType)
  const newItem = {
    i: `inline-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
    chartId: undefined,
    mode: 'inline' as const,
    inlineConfig,
    left: pos.x,
    top: pos.y,
    width: w,
    height: h
  }
  layout.value.push(newItem)
  layout.value = [...layout.value]
  saveToHistory()
  message.success('已添加到画布')
}

// 从组件库点击添加静态组件（标题/文本/分割线/图片）
const handleClickAddStaticComponent = (data: string) => {
  try {
    const parsed = JSON.parse(data)
    const isBs = form.layoutMode === 'bigscreen'
    const isMb = form.layoutMode === 'mobile'
    const cw = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 400 : 400)
    const defaults: Record<string, { w: number; h: number; config: any }> = {
      title: { w: isMb ? 358 : 400, h: 60, config: { text: '标题文本', fontSize: isBs ? 28 : (isMb ? 18 : 24), fontWeight: 'bold', color: isBs ? '#e0ecff' : '#333', align: 'left' } },
      text: { w: cw, h: isMb ? 80 : 120, config: { text: '这是一段文本内容，可以在右侧属性面板中编辑。', fontSize: isMb ? 14 : 14, color: isBs ? '#8ca8c8' : '#666', align: 'left', lineHeight: 1.6 } },
      divider: { w: isMb ? 358 : 600, h: 30, config: { style: 'solid', color: isBs ? 'rgba(64,158,255,0.4)' : '#e0e0e0', thickness: 1 } },
      image: { w: isMb ? 358 : 300, h: 200, config: { src: '', alt: '图片', objectFit: 'contain' } },
      // 移动端专属组件
      statusBar: { w: 390, h: 44, config: {} },
      navbar: { w: 390, h: 44, config: { title: '页面标题', bgColor: '#fff', color: '#333', fontSize: 17 } },
      tabBar: { w: 390, h: 50, config: { tabs: [{icon:'🏠',label:'首页'},{icon:'📊',label:'数据'},{icon:'⚙️',label:'设置'},{icon:'👤',label:'我的'}], activeIndex: 0 } },
      mobileCard: { w: 358, h: 120, config: { title: '卡片标题', content: '卡片内容区域，可编辑', bgColor: '#fff', titleColor: '#333', textColor: '#666', radius: 12 } },
      listItem: { w: 358, h: 56, config: { icon: '📋', title: '列表项标题', desc: '描述信息', bgColor: '#fff', titleColor: '#333' } },
      searchBar: { w: 358, h: 44, config: { placeholder: '搜索...', bgColor: '#f5f5f5', inputBg: '#fff', radius: 20 } },
      // 大屏专属组件
      kpiCard: { w: 300, h: 120, config: { label: 'KPI指标', value: '1,234', prefix: '', suffix: '', valueFontSize: 32, bgColor: 'rgba(6,30,60,0.8)', borderColor: 'rgba(64,158,255,0.4)', labelColor: '#8ca8c8', valueColor: '#00d4ff' } },
      numberFlipper: { w: 500, h: 180, config: { label: '数据指标', value: '09876', unit: '万元', digitFontSize: 48, bgColor: 'rgba(6,30,60,0.8)', labelColor: '#8ca8c8', digitColor: '#00ffcc', unitColor: '#5b8fae' } },
      decorBorder: { w: 460, h: 320, config: { title: '模块标题', borderColor: 'rgba(64,158,255,0.6)', bgColor: 'rgba(6,30,60,0.5)', titleColor: '#00d4ff' } },
      headerBar: { w: 1920, h: 64, config: { title: '数据可视化大屏', titleFontSize: 28, titleColor: '#00d4ff', timeColor: '#5b8fae', showTime: true, bgColor: 'linear-gradient(90deg, rgba(6,30,60,0.9) 0%, rgba(12,21,39,0.95) 50%, rgba(6,30,60,0.9) 100%)' } },
      progressBar: { w: isMb ? 358 : 400, h: 80, config: isBs ? { label: '完成进度', percent: 72, bgColor: 'rgba(6,30,60,0.8)', labelColor: '#8ca8c8', valueColor: '#00d4ff', barColor: 'linear-gradient(90deg, #00d4ff, #00ffcc)' } : { label: '完成进度', percent: 72, bgColor: '#fff', labelColor: '#666', valueColor: '#1890ff', barColor: '#1890ff' } },
      countdown: { w: 360, h: 120, config: { label: '距活动开始', seconds: 86400, digitFontSize: 36, valueColor: '#00d4ff', labelColor: '#8ca8c8', bgColor: 'rgba(6,30,60,0.8)' } },
      scrollList: { w: 420, h: 320, config: { title: '实时排行', items: ['数据项 1', '数据项 2', '数据项 3', '数据项 4', '数据项 5'], speed: 10, textColor: '#c8d8e8', titleColor: '#00d4ff', bgColor: 'rgba(6,30,60,0.8)' } },
      marquee: { w: 720, h: 44, config: { text: '数据可视化大屏 · 实时数据接入 · 多维度分析', fontSize: 16, speed: 12, textColor: '#00d4ff', bgColor: 'rgba(6,30,60,0.8)' } }
    }
    const d = defaults[parsed.type] || defaults['text']!
    const pos = getCanvasViewportCenter(d.w, d.h)
    const newItem = {
      i: `static-${parsed.type}-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
      chartId: undefined,
      mode: 'static' as const,
      staticType: parsed.type,
      staticConfig: d.config,
      left: pos.x,
      top: pos.y,
      width: d.w,
      height: d.h
    }
    layout.value.push(newItem)
    layout.value = [...layout.value]
    saveToHistory()
    message.success(`已添加${parsed.label || '静态组件'}到画布`)
  } catch {
    message.error('添加失败')
  }
}

// 从组件库点击添加查询组件
const handleClickAddQueryComponent = (_data: string) => {
  if (!showParameterPanel.value) {
    showParameterPanel.value = true
  }
  try {
    const comp = JSON.parse(_data)
    parameterPanel.components.push(comp)
    message.success('查询组件已添加到参数栏')
  } catch {
    message.error('添加失败')
  }
}

// 从图表库点击添加引用图表
const handleClickAddReferencedChart = (chart: ChartDefinition) => {
  const isMb = form.layoutMode === 'mobile'
  const isBs = form.layoutMode === 'bigscreen'
  const w = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 500 : 400)
  const h = isMb ? 240 : (isBs ? 360 : 300)
  const pos = getCanvasViewportCenter(w, h)
  const newItem = {
    i: `chart-${chart.id}-${Date.now()}`,
    chartId: chart.id,
    mode: 'referenced' as const,
    left: pos.x,
    top: pos.y,
    width: w,
    height: h
  }
  layout.value.push(newItem)
  layout.value = [...layout.value]
  saveToHistory()
  message.success(`图表「${chart.chartName}」已添加到画布`)
}

// 从AI助手添加内联图表（带碰撞检测）
const handleAddInlineChartFromAi = (config: InlineChartConfig) => {
  const isMb = form.layoutMode === 'mobile'
  const isBs = form.layoutMode === 'bigscreen'
  const w = isMb ? Math.min(358, canvasWidth.value - 16) : (isBs ? 500 : 400)
  const h = isMb ? 240 : (isBs ? 360 : 300)
  const pos = getCanvasViewportCenter(w, h)
  
  const newItem = {
    i: `inline-ai-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
    chartId: undefined,
    mode: 'inline' as const,
    inlineConfig: config,
    left: pos.x,
    top: pos.y,
    width: w,
    height: h
  }
  layout.value.push(newItem)
  layout.value = [...layout.value]
  saveToHistory()
}

// 同步静态组件配置到布局
const syncStaticConfigToLayout = () => {
  if (!selectedItem.value || selectedItem.value.mode !== 'static') return
  const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
  if (layoutItem) {
    layoutItem.staticConfig = { ...selectedItem.value.staticConfig }
    layout.value = [...layout.value]
  }
}

// 内联图表配置更新（从 InlineChartEditor）
const handleInlineConfigUpdate = (config: InlineChartConfig) => {
  if (!selectedItem.value) return
  const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
  if (layoutItem) {
    layoutItem.inlineConfig = config
    selectedItem.value = { ...selectedItem.value, inlineConfig: config }
    layout.value = [...layout.value]
    // 触发重新渲染（含“图表名称”作为标题的更新）
    nextTick(() => renderInlineChart(layoutItem))
  }
}

// 接收内联图表预览数据
const handleInlinePreviewData = (itemId: string, data: any[]) => {
  inlineChartDataMap.set(itemId, data)
  const layoutItem = layout.value.find(l => l.i === itemId)
  if (layoutItem) {
    nextTick(() => renderInlineChart(layoutItem))
  }
}

// 接收内联图表参数检测
const handleInlineParamsDetected = (itemId: string, params: string[]) => {
  inlineChartParamsMap.set(itemId, params)
}

// 参数面板查询 → 替换内联图表SQL参数并重新渲染
const handleParameterQuery = async (paramValues: Record<string, any>) => {
  const tasks: Promise<void>[] = []

  for (const [itemId, params] of inlineChartParamsMap.entries()) {
    if (params.length === 0) continue
    const layoutItem = layout.value.find(l => l.i === itemId)
    if (!layoutItem?.inlineConfig?.sqlContent || !layoutItem.inlineConfig.dataSourceId) continue

    // 替换 SQL 中的 ${param} 占位符
    let sql = layoutItem.inlineConfig.sqlContent
    let allResolved = true
    params.forEach(p => {
      const val = paramValues[p]
      if (val !== undefined && val !== null && val !== '') {
        sql = sql.replace(new RegExp(`\\$\\{${p}\\}`, 'g'), String(val))
      } else {
        allResolved = false
      }
    })

    // 如果SQL中仍有未解析的参数占位符，跳过执行
    if (!allResolved && /\$\{\w+\}/.test(sql)) continue

    // 并行执行所有图表的SQL查询
    tasks.push((async () => {
      try {
        const res = await testInlineChartSql({
          dataSourceId: layoutItem.inlineConfig!.dataSourceId!,
          sqlContent: sql,
          limit: layoutItem.inlineConfig!.dataLimit || 100
        })
        if (res?.data && Array.isArray(res.data)) {
          inlineChartDataMap.set(itemId, res.data)
          renderInlineChart(layoutItem)
        }
      } catch (e) {
        console.warn(`参数查询内联图表 ${itemId} 失败:`, e)
      }
    })())
  }

  await Promise.allSettled(tasks)
}

// 参数面板重置
const handleParameterReset = async () => {
  // 用原始SQL重新执行查询并渲染
  for (const [itemId, params] of inlineChartParamsMap.entries()) {
    if (params.length === 0) continue
    const layoutItem = layout.value.find(l => l.i === itemId)
    if (!layoutItem?.inlineConfig?.sqlContent || !layoutItem.inlineConfig.dataSourceId) continue

    try {
      const res = await testInlineChartSql({
        dataSourceId: layoutItem.inlineConfig.dataSourceId,
        sqlContent: layoutItem.inlineConfig.sqlContent,
        limit: layoutItem.inlineConfig.dataLimit || 100
      })
      if (res?.data && Array.isArray(res.data)) {
        inlineChartDataMap.set(itemId, res.data)
        renderInlineChart(layoutItem)
      }
    } catch (e) {
      console.warn(`重置内联图表 ${itemId} 失败:`, e)
    }
  }
}

// 渲染内联图表
// 让“图表名称”直接作为图表可见标题（内联图表）
const applyInlineTitle = (option: any, config: any) => {
  if (!option || !config) return option
  const name = (config.chartName || '').toString().trim()
  if (name) {
    option.title = { ...(option.title || {}), text: name, show: true }
  }
  return option
}

const renderInlineChart = (item: any) => {
  const instanceKey = item.i
  const instance = chartInstances.get(instanceKey)
  if (!instance || !isChartInstanceValid(instance)) return

  const config = item.inlineConfig
  if (!config) return

  // 使用缓存数据或空数据
  const data = inlineChartDataMap.get(instanceKey) || []

  try {
    const option = buildInlineChartOption(
      config.chartType || 'bar',
      data,
      config.fieldMapping as any,
      config.colorScheme,
      config.chartConfig
    )
    applyInlineTitle(option, config)
    instance.setOption(option, true)
  } catch (e) {
    console.warn('渲染内联图表失败:', e)
  }
}

// 加载并渲染内联图表（用于初始化）
const loadAndRenderInlineChart = async (item: any, instance: echarts.ECharts) => {
  const config = item.inlineConfig
  if (!config) {
    // 显示空占位
    try {
      instance.setOption({
        graphic: [{
          type: 'group', left: 'center', top: 'middle',
          children: [
            { type: 'text', style: { text: '📊 图表', fontSize: 16, fill: '#bbb', textAlign: 'center' } },
            { type: 'text', top: 24, style: { text: '请配置数据源', fontSize: 12, fill: '#ccc', textAlign: 'center' } }
          ]
        }]
      }, true)
    } catch { /* ignore */ }
    return
  }

  // 如果有缓存数据，直接渲染
  const cachedData = inlineChartDataMap.get(item.i)
  if (cachedData && cachedData.length > 0) {
    try {
      const option = buildInlineChartOption(
        config.chartType || 'bar',
        cachedData,
        config.fieldMapping as any,
        config.colorScheme,
        config.chartConfig
      )
      applyInlineTitle(option, config)
      instance.setOption(option, true)
    } catch (e) {
      console.warn('渲染内联图表失败:', e)
    }
    return
  }

  // 如果有 chartConfig 但没有数据，尝试加载
  if (config.dataSourceId && config.sqlContent) {
    try {
      const res = await testInlineChartSql({
        dataSourceId: config.dataSourceId,
        sqlContent: config.sqlContent,
        limit: config.dataLimit || 100
      })
      if (res?.data && Array.isArray(res.data)) {
        inlineChartDataMap.set(item.i, res.data)
        const option = buildInlineChartOption(
          config.chartType || 'bar',
          res.data,
          config.fieldMapping as any,
          config.colorScheme,
          config.chartConfig
        )
        applyInlineTitle(option, config)
        if (isChartInstanceValid(instance)) {
          instance.setOption(option, true)
        }
        return
      }
    } catch (e) {
      console.warn('加载内联图表数据失败:', e)
    }
  }

  // 如果有 chartConfig，尝试渲染空数据的 option
  if (config.chartConfig) {
    try {
      const option = buildInlineChartOption(
        config.chartType || 'bar',
        [],
        config.fieldMapping as any,
        config.colorScheme,
        config.chartConfig
      )
      applyInlineTitle(option, config)
      instance.setOption(option, true)
    } catch { /* ignore */ }
    return
  }

  // 显示空状态
  try {
    const option = buildInlineChartOption(config.chartType || 'bar', [], undefined, config.colorScheme)
    instance.setOption(option, true)
  } catch { /* ignore */ }
}

// 从编辑器更新位置
const handleEditorPositionChange = (pos: { left: number; top: number }) => {
  if (!selectedItem.value) return
  selectedItem.value.left = Math.max(0, Math.round(Number(pos.left) || 0))
  selectedItem.value.top = Math.max(0, Math.round(Number(pos.top) || 0))
  handlePositionChange()
}

// 从编辑器更新尺寸
const handleEditorSizeChange = (size: { width: number; height: number }) => {
  if (!selectedItem.value) return
  const normalizedSize = normalizeItemSize(selectedItem.value, size.width, size.height)
  selectedItem.value.width = normalizedSize.width
  selectedItem.value.height = normalizedSize.height
  handleSizeChange()
}

// 从编辑器执行图层操作
const handleEditorLayerAction = (action: string) => {
  if (action === 'up') handleLayerUp()
  else if (action === 'down') handleLayerDown()
  else if (action === 'top') handleLayerTop()
  else if (action === 'bottom') handleLayerBottom()
}

// 内联图表转为公共图表
const handleConvertToPublic = async () => {
  if (!selectedItem.value?.inlineConfig) return
  try {
    const res = await convertInlineToPublic({
      inlineConfig: selectedItem.value.inlineConfig
    })
    if (res?.data?.chartId) {
      const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
      if (layoutItem) {
        layoutItem.mode = 'referenced'
        layoutItem.chartId = res.data.chartId
        layoutItem.inlineConfig = undefined
        selectedItem.value = { ...layoutItem }
        layout.value = [...layout.value]
        // 重新加载图表列表
        await loadCharts()
      }
      message.success('已转为公共图表')
    }
  } catch (e: any) {
    message.error(e?.message || '转换失败')
  }
}

// 图表项激活
const handleItemActivated = (item: any) => {
  const layoutItem = layout.value.find(l => l.i === item.i)
  const source = layoutItem || item
  const left = Number(source.left)
  const top = Number(source.top)
  const width = Number(source.width)
  const height = Number(source.height)
  
  selectedQueryComponent.value = null // 取消查询组件选中
  selectedItem.value = {
    ...source,
    left: (!isNaN(left) && left >= 0) ? left : 0,
    top: (!isNaN(top) && top >= 0) ? top : 0,
    width: (!isNaN(width) && width >= 150) ? width : 300,
    height: (!isNaN(height) && height >= 120) ? height : 200,
    mode: source.mode || (source.chartId ? 'referenced' : 'inline'),
    inlineConfig: source.inlineConfig,
    chartName: source.chartId ? getChartName(source.chartId) : undefined
  }
}

// 图表项取消激活
const handleItemDeactivated = () => {
  // 点击画布空白处时取消选中
  if (canvasRef.value) {
    canvasRef.value.addEventListener('click', handleCanvasClick, { once: true })
  }
}

// 画布上双击静态文字组件 → 就地编辑文字（标题/文本/KPI指标名）
const startInlineEdit = (item: any) => {
  if (!item || item.mode !== 'static') return
  if (!['title', 'text', 'kpiCard'].includes(item.staticType)) return
  const li = layout.value.find(l => String(l.i) === String(item.i))
  if (!li) return
  if (!li.staticConfig) li.staticConfig = {}
  editingItemId.value = String(item.i)
  nextTick(() => {
    const el = document.querySelector(`[data-inline-edit="${item.i}"]`) as HTMLInputElement | HTMLTextAreaElement | null
    if (el) { el.focus(); try { (el as HTMLInputElement).select() } catch { /* noop */ } }
  })
}

const finishInlineEdit = () => {
  const id = editingItemId.value
  editingItemId.value = null
  if (id == null) return
  const li = layout.value.find(l => String(l.i) === id)
  if (li) {
    // 触发布局更新以标记可保存状态
    layout.value = [...layout.value]
    if (selectedItem.value && String(selectedItem.value.i) === id && li.staticConfig) {
      selectedItem.value = { ...selectedItem.value, staticConfig: { ...li.staticConfig } }
    }
  }
}

// 画布点击处理
const handleCanvasClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  // 如果点击的是画布本身（不是图表项），取消选中
  if (target === canvasRef.value || target.classList.contains('design-canvas')) {
    selectedItem.value = null
    selectedQueryComponent.value = null
  }
}

// 拖拽中（带对齐吸附）
const handleDragging = (event: any, item: any) => {
  isDraggingOrResizing.value = true
  
  const layoutItem = layout.value.find(l => l.i === item.i)
  if (layoutItem) {
    const eventLeft = event.left !== undefined ? event.left : (event.x !== undefined ? event.x : layoutItem.left)
    const eventTop = event.top !== undefined ? event.top : (event.y !== undefined ? event.y : layoutItem.top)
    
    let left = Math.max(0, Math.round(Number(eventLeft) || 0))
    let top = Math.max(0, Math.round(Number(eventTop) || 0))
    const w = Number(layoutItem.width) || 200
    const h = Number(layoutItem.height) || 200

    // 收集其他组件的边缘位置用于吸附（边缘对齐）
    const xSnaps: number[] = [0, canvasWidth.value]
    const ySnaps: number[] = [0, canvasHeight.value]
    layout.value.forEach(other => {
      if (other.i === item.i) return
      const ol = Number(other.left) || 0
      const ot = Number(other.top) || 0
      const ow = Number(other.width) || 200
      const oh = Number(other.height) || 200
      xSnaps.push(ol, ol + ow)
      ySnaps.push(ot, ot + oh)
    })

    // 吸附到边缘
    const activeX: number[] = []
    const activeY: number[] = []
    
    for (const sx of xSnaps) {
      if (Math.abs(left - sx) <= SNAP_THRESHOLD) {
        left = sx
        activeX.push(sx)
        break
      }
      if (Math.abs((left + w) - sx) <= SNAP_THRESHOLD) {
        left = sx - w
        activeX.push(sx)
        break
      }
    }
    
    for (const sy of ySnaps) {
      if (Math.abs(top - sy) <= SNAP_THRESHOLD) {
        top = sy
        activeY.push(sy)
        break
      }
      if (Math.abs((top + h) - sy) <= SNAP_THRESHOLD) {
        top = sy - h
        activeY.push(sy)
        break
      }
    }
    
    snapLines.value = { x: [...new Set(activeX)], y: [...new Set(activeY)] }

    left = Math.max(0, left)
    top = Math.max(0, top)
    
    if (!isNaN(left) && !isNaN(top)) {
      layoutItem.left = left
      layoutItem.top = top
      if (selectedItem.value && selectedItem.value.i === item.i) {
        selectedItem.value.left = left
        selectedItem.value.top = top
      }
    }
  }
}

// 检查与其他组件的碰撞
const checkCollisionWithItems = (x: number, y: number, w: number, h: number, excludeId: string) => {
  for (const other of layout.value) {
    if (other.i === excludeId) continue
    const ox = Number(other.left) || 0
    const oy = Number(other.top) || 0
    const ow = Number(other.width) || 200
    const oh = Number(other.height) || 200
    
    // 检查是否重叠（允许边缘接触）
    if (x < ox + ow && x + w > ox && y < oy + oh && y + h > oy) {
      return { itemId: other.i, left: ox, top: oy, width: ow, height: oh }
    }
  }
  return null
}

// 找到最近的不重叠位置
const findNonOverlappingPosition = (x: number, y: number, w: number, h: number, excludeId: string, collision: any) => {
  const ox = collision.left
  const oy = collision.top
  const ow = collision.width
  const oh = collision.height
  
  // 计算四个方向的推开距离
  const pushRight = ox + ow - x  // 推到碰撞组件右侧
  const pushLeft = x + w - ox    // 推到碰撞组件左侧
  const pushDown = oy + oh - y   // 推到碰撞组件下方
  const pushUp = y + h - oy      // 推到碰撞组件上方
  
  // 找最小推动距离的方向
  const options = [
    { dir: 'right', dist: pushRight, pos: { left: ox + ow, top: y } },
    { dir: 'left', dist: pushLeft, pos: { left: ox - w, top: y } },
    { dir: 'down', dist: pushDown, pos: { left: x, top: oy + oh } },
    { dir: 'up', dist: pushUp, pos: { left: x, top: oy - h } }
  ].filter(opt => {
    // 过滤掉超出画布的选项
    const newX = opt.pos.left
    const newY = opt.pos.top
    return newX >= 0 && newY >= 0 && newX + w <= canvasWidth.value && newY + h <= canvasHeight.value
  }).filter(opt => {
    // 过滤掉仍然会与其他组件碰撞的选项
    return !checkCollisionWithItems(opt.pos.left, opt.pos.top, w, h, excludeId)
  })
  
  if (options.length === 0) return null
  
  // 选择推动距离最小的方向
  options.sort((a, b) => a.dist - b.dist)
  return options[0]?.pos || null
}

// 拖拽停止
const handleDragStop = (event: any, item: any) => {
  const layoutItem = layout.value.find(l => l.i === item.i)
  if (layoutItem) {
    const eventLeft = event.left !== undefined ? event.left : (event.x !== undefined ? event.x : layoutItem.left)
    const eventTop = event.top !== undefined ? event.top : (event.y !== undefined ? event.y : layoutItem.top)
    
    const maxLeft = Math.max(0, canvasWidth.value - 20)
    const maxTop = Math.max(0, canvasHeight.value - 20)
    const left = Math.min(maxLeft, Math.max(0, Math.round(Number(eventLeft) || 0)))
    const top = Math.min(maxTop, Math.max(0, Math.round(Number(eventTop) || 0)))
    
    if (!isNaN(left) && !isNaN(top)) {
      layoutItem.left = left
      layoutItem.top = top
      if (selectedItem.value && selectedItem.value.i === item.i) {
        selectedItem.value = { ...selectedItem.value, left, top }
      }
    }
  }
  
  snapLines.value = { x: [], y: [] }
  
  setTimeout(() => {
    isDraggingOrResizing.value = false
    layout.value = [...layout.value]
    saveToHistory()
  }, 50)
}

// 调整大小中
const handleResizing = (event: any, item: any) => {
  isDraggingOrResizing.value = true
  
  const layoutItem = layout.value.find(l => l.i === item.i)
  if (layoutItem) {
    const eventWidth = event.width !== undefined ? event.width : (event.w !== undefined ? event.w : layoutItem.width)
    const eventHeight = event.height !== undefined ? event.height : (event.h !== undefined ? event.h : layoutItem.height)
    const eventLeft = event.left !== undefined ? event.left : (event.x !== undefined ? event.x : layoutItem.left)
    const eventTop = event.top !== undefined ? event.top : (event.y !== undefined ? event.y : layoutItem.top)
    
    const normalizedSize = normalizeItemSize(layoutItem, eventWidth, eventHeight)
    const width = normalizedSize.width
    const height = normalizedSize.height
    const left = Math.max(0, Math.round(Number(eventLeft) || 0))
    const top = Math.max(0, Math.round(Number(eventTop) || 0))
    
    if (!isNaN(width) && !isNaN(height) && !isNaN(left) && !isNaN(top)) {
      layoutItem.width = width
      layoutItem.height = height
      layoutItem.left = left
      layoutItem.top = top
      
      if (selectedItem.value && selectedItem.value.i === item.i) {
        selectedItem.value.width = width
        selectedItem.value.height = height
        selectedItem.value.left = left
        selectedItem.value.top = top
      }
    }
  }
}

// 调整大小停止
const handleResizeStop = (event: any, item: any) => {
  const layoutItem = layout.value.find(l => l.i === item.i)
  if (layoutItem) {
    const eventWidth = event.width !== undefined ? event.width : (event.w !== undefined ? event.w : layoutItem.width)
    const eventHeight = event.height !== undefined ? event.height : (event.h !== undefined ? event.h : layoutItem.height)
    const eventLeft = event.left !== undefined ? event.left : (event.x !== undefined ? event.x : layoutItem.left)
    const eventTop = event.top !== undefined ? event.top : (event.y !== undefined ? event.y : layoutItem.top)
    
    const left = Math.max(0, Math.round(Number(eventLeft) || 0))
    const top = Math.max(0, Math.round(Number(eventTop) || 0))
    const minW = getItemMinWidth(layoutItem)
    const minH = getItemMinHeight(layoutItem)
    const maxW = Math.max(minW, canvasWidth.value - left)
    const maxH = Math.max(minH, canvasHeight.value - top)
    const normalizedSize = normalizeItemSize(layoutItem, eventWidth, eventHeight)
    const width = Math.min(maxW, normalizedSize.width)
    const height = Math.min(maxH, normalizedSize.height)
    
    if (!isNaN(width) && !isNaN(height) && !isNaN(left) && !isNaN(top)) {
      layoutItem.width = width
      layoutItem.height = height
      layoutItem.left = left
      layoutItem.top = top
      
      if (selectedItem.value && selectedItem.value.i === item.i) {
        selectedItem.value = { ...selectedItem.value, left, top, width, height }
      }
    }
  }
  
  // 延迟重置标志
  setTimeout(() => {
    isDraggingOrResizing.value = false
    layout.value = [...layout.value]
    saveToHistory()
    nextTick(() => {
      if (layoutItem) {
        const instance = chartInstances.get(layoutItem.i)
        if (instance && isChartInstanceValid(instance)) {
          try {
            instance.resize()
          } catch (e) {
            console.warn('调整图表大小失败:', e)
          }
        }
      }
    })
  }, 100)
}

// 位置变化（带碰撞检测）
const handlePositionChange = () => {
  if (selectedItem.value) {
    let left = Math.max(0, Math.round(Number(selectedItem.value.left) || 0))
    let top = Math.max(0, Math.round(Number(selectedItem.value.top) || 0))
    const w = Number(selectedItem.value.width) || 200
    const h = Number(selectedItem.value.height) || 200
    
    // 碰撞检测
    const collision = checkCollisionWithItems(left, top, w, h, selectedItem.value.i)
    if (collision) {
      const adjustedPos = findNonOverlappingPosition(left, top, w, h, selectedItem.value.i, collision)
      if (adjustedPos) {
        left = adjustedPos.left
        top = adjustedPos.top
      }
    }
    
    selectedItem.value.left = left
    selectedItem.value.top = top
    
    const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
    if (layoutItem) {
      layoutItem.left = left
      layoutItem.top = top
      layout.value = [...layout.value]
      saveToHistory()
    }
  }
}

// 大小变化（带碰撞检测）
const handleSizeChange = () => {
  if (selectedItem.value) {
    const left = Number(selectedItem.value.left) || 0
    const top = Number(selectedItem.value.top) || 0
    const minW = getItemMinWidth(selectedItem.value)
    const minH = getItemMinHeight(selectedItem.value)
    const normalizedSize = normalizeItemSize(selectedItem.value, selectedItem.value.width, selectedItem.value.height)
    let width = normalizedSize.width
    let height = normalizedSize.height
    
    // 碰撞检测：限制尺寸不超过碰撞边界
    const collision = checkCollisionWithItems(left, top, width, height, selectedItem.value.i)
    if (collision) {
      const ox = collision.left
      const oy = collision.top
      // 限制尺寸到碰撞边界
      if (left + width > ox && left < ox) width = Math.max(minW, ox - left)
      if (top + height > oy && top < oy) height = Math.max(minH, oy - top)
    }
    
    selectedItem.value.width = width
    selectedItem.value.height = height
    
    const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
    if (layoutItem) {
      layoutItem.width = width
      layoutItem.height = height
      layout.value = [...layout.value]
      saveToHistory()
      nextTick(() => {
        const instance = chartInstances.get(selectedItem.value.i)
        if (instance && isChartInstanceValid(instance)) {
          setTimeout(() => {
            instance.resize()
          }, 50)
        }
      })
    }
  }
}

// 移除图表
const handleRemoveChart = (itemId: string) => {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这个图表吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      saveToHistory()
      const index = layout.value.findIndex(item => item.i === itemId)
      if (index > -1) {
        // 清理图表实例（使用itemId作为key）
        const instance = chartInstances.get(itemId)
        if (instance) {
          try {
            if (isChartInstanceValid(instance)) {
              instance.dispose()
            }
          } catch (e) {
            console.warn('清理图表实例失败:', e)
          }
          chartInstances.delete(itemId)
        }
        // 清理内联图表缓存
        inlineChartDataMap.delete(itemId)
        inlineChartParamsMap.delete(itemId)
        layout.value.splice(index, 1)
        layout.value = [...layout.value]
        if (selectedItem.value?.i === itemId) {
          selectedItem.value = null
        }
        message.success('图表已删除')
      }
    }
  })
}

// 清空画布
const handleClear = () => {
  dialog.warning({
    title: '确认清空',
    content: '确定要清空所有图表吗？此操作不可恢复。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      saveToHistory()
      // 清理所有图表实例
      chartInstances.forEach((instance) => {
        try {
          if ((instance as any)._resizeHandler) {
            window.removeEventListener('resize', (instance as any)._resizeHandler)
          }
          if (isChartInstanceValid(instance)) {
            instance.dispose()
          }
        } catch (e) {
          console.warn('清理图表实例失败:', e)
        }
      })
      chartInstances.clear()
      inlineChartDataMap.clear()
      inlineChartParamsMap.clear()
      layout.value = []
      selectedItem.value = null
      message.success('画布已清空')
    }
  })
}

// 缩放
const handleZoomIn = () => {
  if (zoomLevel.value < 200) {
    zoomLevel.value = Math.min(200, zoomLevel.value + 10)
  }
}

const handleZoomOut = () => {
  if (zoomLevel.value > 30) {
    zoomLevel.value = Math.max(30, zoomLevel.value - 10)
  }
}

const handleZoomReset = () => {
  zoomLevel.value = 100
  message.success('缩放已重置')
}

// 适配画布（Fit to View）
const handleFitToView = () => {
  const wrapper = canvasWrapperRef.value
  if (!wrapper) return
  // 计算画布完全显示所需的缩放比例
  const padded = 40 // canvas margin
  const wrapperW = wrapper.clientWidth
  const wrapperH = wrapper.clientHeight
  const fitW = wrapperW / (canvasWidth.value + padded)
  const fitH = wrapperH / (canvasHeight.value + padded)
  const fit = Math.min(fitW, fitH, 1) // 不超过100%
  zoomLevel.value = Math.max(30, Math.min(200, Math.round(fit * 100)))
  // 滚动到左上角
  nextTick(() => {
    wrapper.scrollLeft = 0
    wrapper.scrollTop = 0
  })
}

// Ctrl+滚轮缩放
const handleCanvasWheel = (e: WheelEvent) => {
  if (e.ctrlKey || e.metaKey) {
    e.preventDefault()
    const delta = e.deltaY > 0 ? -5 : 5
    zoomLevel.value = Math.max(30, Math.min(200, zoomLevel.value + delta))
  }
  // 未Ctrl时不拦截，使用浏览器原生滚动
}

// 中键/Space 拖拽平移
const isPanning = ref(false)
const isSpaceDown = ref(false)
const panStartX = ref(0)
const panStartY = ref(0)
const panScrollX = ref(0)
const panScrollY = ref(0)

const handleCanvasPanStart = (e: MouseEvent) => {
  // 中键(button=1)或 Space+左键
  if (e.button === 1 || (isSpaceDown.value && e.button === 0)) {
    e.preventDefault()
    isPanning.value = true
    panStartX.value = e.clientX
    panStartY.value = e.clientY
    const wrapper = canvasWrapperRef.value
    if (wrapper) {
      panScrollX.value = wrapper.scrollLeft
      panScrollY.value = wrapper.scrollTop
    }
  }
}

const handleCanvasPanMove = (e: MouseEvent) => {
  if (!isPanning.value) return
  e.preventDefault()
  const wrapper = canvasWrapperRef.value
  if (wrapper) {
    wrapper.scrollLeft = panScrollX.value - (e.clientX - panStartX.value)
    wrapper.scrollTop = panScrollY.value - (e.clientY - panStartY.value)
  }
}

const handleCanvasPanEnd = () => {
  isPanning.value = false
}

const handleSpaceKeyDown = (e: KeyboardEvent) => {
  if (e.code === 'Space' && !isSpaceDown.value) {
    const tag = (e.target as HTMLElement)?.tagName?.toUpperCase()
    if (tag === 'INPUT' || tag === 'TEXTAREA' || (e.target as HTMLElement)?.isContentEditable) return
    e.preventDefault()
    isSpaceDown.value = true
  }
}

const handleSpaceKeyUp = (e: KeyboardEvent) => {
  if (e.code === 'Space') {
    isSpaceDown.value = false
    isPanning.value = false
  }
}

// 保存到历史记录
const saveToHistory = () => {
  const currentState = JSON.parse(JSON.stringify(layout.value))
  // 移除当前索引之后的历史
  historyStack.value = historyStack.value.slice(0, historyIndex.value + 1)
  // 添加新状态
  historyStack.value.push(currentState)
  // 限制历史记录数量
  if (historyStack.value.length > maxHistorySize) {
    historyStack.value.shift()
  } else {
    historyIndex.value++
  }
}

// 撤销
const canUndo = computed(() => historyIndex.value > 0)
const handleUndo = () => {
  if (!canUndo.value) return
  historyIndex.value--
  layout.value = JSON.parse(JSON.stringify(historyStack.value[historyIndex.value]))
  message.success('已撤销')
}

// 重做
const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1)
const handleRedo = () => {
  if (!canRedo.value) return
  historyIndex.value++
  layout.value = JSON.parse(JSON.stringify(historyStack.value[historyIndex.value]))
  message.success('已重做')
}

// 画布尺寸变化
const handleCanvasSizeChange = (value: string) => {
  if (value === 'custom') {
    // 自定义模式，不做任何操作，用户直接在输入框中修改
    return
  }
  
  if (value === 'auto') {
    // 自适应：获取当前画布容器的实际尺寸
    calculateAutoCanvasSize()
    message.success(`画布尺寸已设置为 ${canvasWidth.value}×${canvasHeight.value}（自适应当前窗口）`)
    return
  }
  
  const [width, height] = value.split('x').map(Number)
  if (width && height) {
    canvasWidth.value = width
    canvasHeight.value = height
    message.success(`画布尺寸已设置为 ${width}×${height}`)
  }
}

// 画布尺寸输入变化
const handleCanvasSizeInputChange = () => {
  // 更新预设选择为"自定义"
  canvasSize.value = 'custom'
  // 触发图表重新渲染
  nextTick(() => {
    resizeAllCharts()
  })
}

// 图层操作
const canLayerUp = computed(() => {
  if (!selectedItem.value) return false
  const index = layout.value.findIndex(item => item.i === selectedItem.value?.i)
  return index < layout.value.length - 1
})

const canLayerDown = computed(() => {
  if (!selectedItem.value) return false
  const index = layout.value.findIndex(item => item.i === selectedItem.value?.i)
  return index > 0
})

const handleLayerUp = () => {
  if (!selectedItem.value || !canLayerUp.value) return
  saveToHistory()
  const index = layout.value.findIndex(item => item.i === selectedItem.value?.i)
  const item = layout.value.splice(index, 1)[0]
  layout.value.splice(index + 1, 0, item)
  layout.value = [...layout.value]
  message.success('已上移')
}

const handleLayerDown = () => {
  if (!selectedItem.value || !canLayerDown.value) return
  saveToHistory()
  const index = layout.value.findIndex(item => item.i === selectedItem.value?.i)
  const item = layout.value.splice(index, 1)[0]
  layout.value.splice(index - 1, 0, item)
  layout.value = [...layout.value]
  message.success('已下移')
}

const handleLayerTop = () => {
  if (!selectedItem.value || !canLayerUp.value) return
  saveToHistory()
  const index = layout.value.findIndex(item => item.i === selectedItem.value?.i)
  const item = layout.value.splice(index, 1)[0]
  layout.value.push(item)
  layout.value = [...layout.value]
  message.success('已置顶')
}

const handleLayerBottom = () => {
  if (!selectedItem.value || !canLayerDown.value) return
  saveToHistory()
  const index = layout.value.findIndex(item => item.i === selectedItem.value?.i)
  const item = layout.value.splice(index, 1)[0]
  layout.value.unshift(item)
  layout.value = [...layout.value]
  message.success('已置底')
}

// 获取图表名称（支持内联图表和静态组件）
const getChartName = (chartId: number | undefined, item?: any) => {
  if (item?.mode === 'static') {
    const labels: Record<string, string> = { title: '标题', text: '文本段落', divider: '分割线', image: '图片', statusBar: '状态栏', navbar: '导航栏', tabBar: '底部标签栏', mobileCard: '卡片容器', listItem: '列表项', searchBar: '搜索栏', kpiCard: 'KPI卡片', numberFlipper: '数字翻牌器', decorBorder: '装饰边框', headerBar: '大屏标题栏', progressBar: '进度条', countdown: '倒计时', scrollList: '滚动列表', marquee: '跑马灯' }
    return labels[item.staticType] || '静态组件'
  }
  if (item?.mode === 'inline' && item?.inlineConfig) {
    return item.inlineConfig.chartName || '未命名内联图表'
  }
  if (chartId) {
    const chart = charts.value.find(c => c.id === chartId)
    return chart ? chart.chartName : '未知图表'
  }
  return '未知图表'
}

// 设置图表预览引用（使用itemId作为唯一标识，避免相同chartId的多个实例冲突）
const setChartPreviewRef = (el: HTMLElement | null, chartId: number, itemId: string) => {
  // 使用 itemId 作为唯一标识，因为可能有多个相同图表的实例
  const instanceKey = itemId
  
  // 清理之前的定时器
  const existingTimer = chartInstanceTimers.get(instanceKey)
  if (existingTimer) {
    clearTimeout(existingTimer)
    chartInstanceTimers.delete(instanceKey)
  }
  
  if (!el) {
    // 清理图表实例（但保留数据缓存，因为数据是按chartId缓存的）
    const instance = chartInstances.get(instanceKey)
    if (instance) {
      try {
        if (isChartInstanceValid(instance)) {
          instance.dispose()
        }
      } catch (e) {
        console.warn('清理图表实例失败:', e)
      }
      chartInstances.delete(instanceKey)
    }
    // 注意：chartLoadingMap 仍然使用 chartId，因为数据加载是按 chartId 缓存的
    return
  }
  
  // 如果已经存在有效的实例，检查是否是同一个DOM元素
  const existingInstance = chartInstances.get(instanceKey)
  if (existingInstance && isChartInstanceValid(existingInstance)) {
    const existingDom = existingInstance.getDom()
    if (existingDom === el) {
      // 同一个元素，不需要重新创建
      return
    }
    // 如果不是同一个元素，先清理旧实例
    try {
      if (isChartInstanceValid(existingInstance)) {
        existingInstance.dispose()
      }
    } catch (e) {
      console.warn('清理旧图表实例失败:', e)
    }
    chartInstances.delete(instanceKey)
  }
  
  // 等待DOM更新和容器大小确定
  nextTick(() => {
    const timer = setTimeout(() => {
      // 再次检查元素是否存在
      if (!el || !el.parentElement) {
        return
      }
      
      // 检查容器大小
      if (el.offsetWidth === 0 || el.offsetHeight === 0) {
        const retryTimer = setTimeout(() => {
          if (el && el.parentElement) {
            setChartPreviewRef(el, chartId, itemId)
          }
        }, 100)
        chartInstanceTimers.set(instanceKey, retryTimer)
        return
      }
      
      // 创建新的图表实例
      try {
        // 确保元素仍然存在
        if (!el || !el.parentElement) {
          return
        }
        
        // 检查 DOM 元素是否已经有 ECharts 实例（防止重复初始化）
        if ((el as any).__echarts_instance__) {
          // 如果已有实例，先销毁它
          try {
            const existingInstance = echarts.getInstanceByDom(el)
            if (existingInstance) {
              existingInstance.dispose()
            }
          } catch (e) {
            console.warn('清理已有实例失败:', e)
          }
        }
        
        const chartInstance = echarts.init(el, null, {
          renderer: 'canvas',
          width: el.offsetWidth || 300,
          height: el.offsetHeight || 200
        })
        // 使用 itemId 作为 key，而不是 chartId
        chartInstances.set(instanceKey, chartInstance)
        
        // 判断是内联图表还是引用图表
        const layoutItem = layout.value.find(l => l.i === itemId)
        if (layoutItem?.mode === 'inline') {
          loadAndRenderInlineChart(layoutItem, chartInstance)
        } else if (chartId > 0) {
          loadAndRenderChart(chartId, chartInstance)
        } else {
          // 空的内联图表
          loadAndRenderInlineChart(layoutItem || { i: itemId, mode: 'inline' }, chartInstance)
        }
      } catch (e) {
        console.error('初始化图表实例失败:', e)
        if (chartId > 0) chartLoadingMap.value[chartId] = false
      }
    }, 50)
    
    chartInstanceTimers.set(instanceKey, timer)
  })
}

// 检查图表实例是否有效
const isChartInstanceValid = (instance: echarts.ECharts | undefined): boolean => {
  if (!instance) return false
  try {
    // 尝试获取DOM，如果实例已被销毁会抛出错误
    const dom = instance.getDom()
    return dom !== null && dom !== undefined && dom.parentElement !== null
  } catch (e) {
    return false
  }
}

// 加载图表数据（带缓存和防重复请求）
const loadChartData = async (chartId: number): Promise<any[]> => {
  // 如果已有缓存，直接返回
  if (chartDataCache.has(chartId)) {
    return chartDataCache.get(chartId)!
  }
  
  // 如果正在加载，等待现有请求完成
  if (chartLoadingPromises.has(chartId)) {
    const data = await chartLoadingPromises.get(chartId)
    return data || []
  }
  
  // 创建新的加载Promise
  const loadPromise = (async () => {
    try {
      const dataRes = await getChartData(chartId)
      const data = (dataRes.data as any[]) || []
      // 缓存数据
      chartDataCache.set(chartId, data)
      return data
    } catch (error) {
      console.error(`加载图表 ${chartId} 数据失败:`, error)
      // 缓存空数组，避免重复请求失败的数据
      chartDataCache.set(chartId, [])
      throw error
    } finally {
      // 加载完成后移除Promise
      chartLoadingPromises.delete(chartId)
    }
  })()
  
  chartLoadingPromises.set(chartId, loadPromise)
  return await loadPromise
}

// 加载并渲染图表
const loadAndRenderChart = async (chartId: number, chartInstance: echarts.ECharts) => {
  // 检查实例是否仍然有效
  if (!isChartInstanceValid(chartInstance)) {
    console.warn(`图表实例 ${chartId} 已失效，取消加载`)
    chartLoadingMap.value[chartId] = false
    return
  }
  
  // 如果正在加载，直接返回（防止重复请求）
  if (chartLoadingMap.value[chartId]) {
    return
  }
  
  // 如果正在加载布局，延迟执行
  if (isLayoutLoading.value) {
    setTimeout(() => {
      loadAndRenderChart(chartId, chartInstance)
    }, 300)
    return
  }
  
  chartLoadingMap.value[chartId] = true
  
  try {
    let chartDef = chartDefinitions.value.get(chartId)
    if (!chartDef) {
      const chart = charts.value.find(c => c.id === chartId)
      if (chart) {
        chartDef = chart
        chartDefinitions.value.set(chartId, chart)
      } else {
        try {
          const res = await getChartDefinitionById(chartId)
          if (res && res.data) {
            chartDef = res.data as ChartDefinition
            chartDefinitions.value.set(chartId, chartDef)
          }
        } catch (e) {
          console.warn('获取图表定义失败:', e)
        }
      }
    }
    
    // 再次检查实例有效性
    if (!isChartInstanceValid(chartInstance)) {
      console.warn(`图表实例 ${chartId} 在加载过程中已失效`)
      chartLoadingMap.value[chartId] = false
      return
    }
    
    if (!chartDef) {
      try {
        chartInstance.setOption({
          graphic: [{
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: '图表配置不存在',
              fontSize: 14,
              fill: '#999'
            }
          }]
        }, true)
      } catch (e) {
        console.warn('设置空状态失败:', e)
      }
      chartLoadingMap.value[chartId] = false
      return
    }
    
    try {
      // 使用带缓存的加载函数
      const data = await loadChartData(chartId)
      
      // 再次检查实例有效性
      if (!isChartInstanceValid(chartInstance)) {
        console.warn(`图表实例 ${chartId} 在获取数据后已失效`)
        chartLoadingMap.value[chartId] = false
        return
      }
      
      const option = buildChartOption(chartDef, data)
      
      // 再次检查实例有效性
      if (!isChartInstanceValid(chartInstance)) {
        console.warn(`图表实例 ${chartId} 在设置选项前已失效`)
        chartLoadingMap.value[chartId] = false
        return
      }
      
      const container = chartInstance.getDom()
      if (container && (container.offsetWidth === 0 || container.offsetHeight === 0)) {
        // 如果容器大小为0，等待一下再设置
        setTimeout(() => {
          // 再次检查实例有效性
          if (!isChartInstanceValid(chartInstance)) {
            chartLoadingMap.value[chartId] = false
            return
          }
          try {
            chartInstance.resize()
            chartInstance.setOption(option, true)
            chartLoadingMap.value[chartId] = false
          } catch (e) {
            console.warn('延迟设置图表选项失败:', e)
            chartLoadingMap.value[chartId] = false
          }
        }, 150)
      } else {
        try {
          chartInstance.setOption(option, true)
          chartLoadingMap.value[chartId] = false
        } catch (e) {
          console.warn('设置图表选项失败:', e)
          chartLoadingMap.value[chartId] = false
        }
      }
      
      const resizeHandler = () => {
        if (isChartInstanceValid(chartInstance)) {
          try {
            chartInstance.resize()
          } catch (e) {
            console.warn('调整图表大小失败:', e)
          }
        }
      }
      window.addEventListener('resize', resizeHandler)
      ;(chartInstance as any)._resizeHandler = resizeHandler
    } catch (dataError) {
      console.error('加载图表数据失败:', dataError)
      // 再次检查实例有效性
      if (isChartInstanceValid(chartInstance)) {
        try {
          chartInstance.setOption({
            graphic: [{
              type: 'text',
              left: 'center',
              top: 'middle',
              style: {
                text: '加载数据失败',
                fontSize: 14,
                fill: '#d03050'
              }
            }]
          }, true)
        } catch (e) {
          console.warn('设置错误状态失败:', e)
        }
      }
    }
  } catch (error) {
    console.error('渲染图表失败:', error)
    // 再次检查实例有效性
    if (isChartInstanceValid(chartInstance)) {
      try {
        chartInstance.setOption({
          graphic: [{
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: '渲染失败',
              fontSize: 14,
              fill: '#d03050'
            }
          }]
        }, true)
      } catch (e) {
        console.warn('设置渲染失败状态失败:', e)
      }
    }
  } finally {
    chartLoadingMap.value[chartId] = false
  }
}

// 防抖调整图表大小
let resizeTimer: NodeJS.Timeout | null = null
const debouncedResizeCharts = () => {
  if (resizeTimer) {
    clearTimeout(resizeTimer)
  }
  resizeTimer = setTimeout(() => {
    nextTick(() => {
      chartInstances.forEach((instance, itemId) => {
        if (isChartInstanceValid(instance)) {
          try {
            const container = instance.getDom()
            if (container && container.offsetWidth > 0 && container.offsetHeight > 0) {
              instance.resize()
            }
          } catch (e) {
            console.warn(`调整图表 ${itemId} 大小失败:`, e)
            // 如果实例已失效，从Map中移除
            chartInstances.delete(itemId)
          }
        } else {
          // 移除无效实例
          chartInstances.delete(itemId)
        }
      })
    })
  }, 300) // 增加防抖延迟到300ms
}

// 立即调整所有图表大小
const resizeAllCharts = () => {
  chartInstances.forEach((instance, itemId) => {
    if (isChartInstanceValid(instance)) {
      try {
        const container = instance.getDom()
        if (container && container.offsetWidth > 0 && container.offsetHeight > 0) {
          instance.resize()
        }
      } catch (e) {
        console.warn(`调整图表 ${itemId} 大小失败:`, e)
        chartInstances.delete(itemId)
      }
    } else {
      chartInstances.delete(itemId)
    }
  })
}

// 监听布局变化，重新调整图表大小（使用防抖，但跳过拖拽/调整大小过程中）
// 防止重复触发的标志
const isLayoutLoading = ref(false)

watch(() => layout.value, () => {
  // 如果正在拖拽、调整大小或加载中，不触发 resize
  if (isDraggingOrResizing.value || isLayoutLoading.value) {
    return
  }
  // 使用防抖，避免频繁触发
  debouncedResizeCharts()
}, { deep: true })

// 保存页面
const handleSave = async () => {
  if (!form.pageName || !form.pageName.trim()) {
    message.warning('请输入页面名称')
    return
  }
  
  // 如果pageCode为空，自动生成唯一编码
  if (!form.pageCode || !form.pageCode.trim()) {
    form.pageCode = `page_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`
  }
  
  submitting.value = true
  try {
    // 确保使用有效的布局数据，从layout.value获取最新数据
    // 先同步 selectedItem 的更改到 layout.value
    if (selectedItem.value) {
      const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
      if (layoutItem) {
        // 同步属性面板的更改
        const normalizedSize = normalizeItemSize(selectedItem.value, selectedItem.value.width, selectedItem.value.height)
        layoutItem.left = Number(selectedItem.value.left) || 0
        layoutItem.top = Number(selectedItem.value.top) || 0
        layoutItem.width = normalizedSize.width
        layoutItem.height = normalizedSize.height
      }
    }
    
    const chartsToSave = layout.value
      .filter(item => item && (item.mode === 'inline' || item.mode === 'static' || (item.chartId && Number(item.chartId) > 0)))
      .map((item, index) => {
        const left = Math.max(0, Math.round(Number(item.left) || 0))
        const top = Math.max(0, Math.round(Number(item.top) || 0))
        const normalizedSize = normalizeItemSize(item, item.width, item.height)
        const width = normalizedSize.width
        const height = normalizedSize.height
        
        if (isNaN(left) || isNaN(top) || isNaN(width) || isNaN(height)) {
          console.error('无效的位置或大小数据:', item)
          throw new Error(`图表位置或大小数据无效`)
        }
        
        const chartSave: any = {
          left, top, width, height,
          sortOrder: index,
          mode: item.mode || 'referenced'
        }
        
        if (item.mode === 'inline') {
          chartSave.inlineConfig = item.inlineConfig
        } else if (item.mode === 'static') {
          chartSave.inlineConfig = JSON.stringify({
            staticType: item.staticType,
            staticConfig: item.staticConfig
          })
        } else {
          chartSave.chartId = Number(item.chartId)
        }
        
        return chartSave
      })
    
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const pageData: any = {
      id: form.id,
      pageName: form.pageName.trim(),
      pageCode: form.pageCode.trim(),
      description: form.description?.trim() || '',
      theme: form.theme || 'default',
      status: 1,
      charts: chartsToSave,
      layoutMode: form.layoutMode || 'desktop',
      layoutConfig: JSON.stringify({ width: canvasWidth.value, height: canvasHeight.value }),
      bigscreenConfig: form.bigscreenConfig,
      // 保存参数面板配置
      parameterPanel: showParameterPanel.value ? {
        visible: true,
        height: parameterPanel.height,
        components: parameterPanel.components,
        showQueryButton: parameterPanel.showQueryButton ?? true,
        showResetButton: parameterPanel.showResetButton ?? true,
        autoQuery: parameterPanel.autoQuery ?? false
      } as PageParameterPanel : undefined
    }
    
    let result
    if (form.id) {
      result = await updatePageDefinition(form.id, pageData)
    } else {
      result = await createPageDefinition(pageData)
    }
    
    if (result && result.data) {
      message.success('保存成功')
      if (!form.id && result.data.id) {
        form.id = result.data.id
        // 使用路由名称跳转
        await router.replace({
          name: 'PageDesigner',
          params: { id: String(form.id) }
        })
        // 等待路由更新后重新加载
        await nextTick()
        await loadPageDefinition()
      } else if (form.id) {
        // 保存后不重新加载，保持当前布局状态
        // 只更新 form 数据，避免布局被重置
        const savedPage = result.data as PageDefinition
        if (savedPage) {
          form.pageName = savedPage.pageName || form.pageName
          form.pageCode = savedPage.pageCode || form.pageCode
          form.description = savedPage.description || form.description
        }
        // 不调用 loadPageDefinition()，保持当前布局
      }
    } else {
      message.error('保存失败：未返回数据')
    }
  } catch (error) {
    console.error('保存页面失败:', error)
    handleApiError(error, '保存页面')
    message.error('保存失败，请检查网络连接或稍后重试')
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  // 关闭当前标签并跳转到页面管理
  tabsStore.closeAndNavigate(route.fullPath, '/page-manage', '页面管理')
  router.push('/page-manage')
}

// 预览
const handlePreview = async () => {
  if (!form.id) {
    // 如果没有ID，先保存
    if (layout.value.length === 0) {
      message.warning('请先添加图表并保存页面')
      return
    }
    
    // 尝试保存
    await handleSave()
    
    // 如果保存后仍然没有ID，提示错误
    if (!form.id) {
      message.error('保存失败，无法预览')
      return
    }
  }
  
  // 确保数据已保存
  try {
    // 先同步 selectedItem 的更改到 layout.value
    if (selectedItem.value) {
      const layoutItem = layout.value.find(l => l.i === selectedItem.value.i)
      if (layoutItem) {
        const normalizedSize = normalizeItemSize(selectedItem.value, selectedItem.value.width, selectedItem.value.height)
        layoutItem.left = Number(selectedItem.value.left) || 0
        layoutItem.top = Number(selectedItem.value.top) || 0
        layoutItem.width = normalizedSize.width
        layoutItem.height = normalizedSize.height
      }
    }
    
    // 先保存当前布局
    await handleSave()
    
    // 等待保存完成
    await nextTick()
    
    // 根据布局模式选择预览路由
    const isBigscreen = form.layoutMode === 'bigscreen'
    const routeName = isBigscreen ? 'BigscreenView' : 'PageView'
    const targetPath = isBigscreen ? `/bigscreen-view/${form.id}` : `/page-view/${form.id}`
    const existingTab = tabsStore.tabs.find(t => t.key === targetPath)
    
    if (existingTab) {
      tabsStore.closeTab(targetPath)
      await nextTick()
    }
    
    router.push({
      name: routeName,
      params: { id: String(form.id) },
      query: { preview: '1' }
    }).catch((err) => {
      // 忽略重复导航错误
      if (err.name !== 'NavigationDuplicated') {
        console.error('路由跳转失败:', err)
        message.error('跳转失败，请稍后重试')
      }
    })
  } catch (error) {
    console.error('预览失败:', error)
    message.error('预览失败，请稍后重试')
  }
}

// 键盘快捷键支持
const handleKeyDown = (event: KeyboardEvent) => {
  // Ctrl+Z 撤销
  if (event.ctrlKey && event.key === 'z' && !event.shiftKey) {
    event.preventDefault()
    handleUndo()
    return
  }
  // Ctrl+Y 或 Ctrl+Shift+Z 重做
  if ((event.ctrlKey && event.key === 'y') || (event.ctrlKey && event.shiftKey && event.key === 'z')) {
    event.preventDefault()
    handleRedo()
    return
  }
  // Delete 或 Backspace 删除选中的图表（输入框内不拦截）
  if ((event.key === 'Delete' || event.key === 'Backspace') && selectedItem.value) {
    const tag = (event.target as HTMLElement)?.tagName?.toUpperCase()
    if (tag === 'INPUT' || tag === 'TEXTAREA' || (event.target as HTMLElement)?.isContentEditable) {
      return
    }
    event.preventDefault()
    handleRemoveChart(selectedItem.value.i)
  }
  // Esc 取消选中
  if (event.key === 'Escape') {
    selectedItem.value = null
  }
  // 方向键微调选中组件位置（Shift+方向键 = 10px步进，带碰撞检测）
  if (selectedItem.value && ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
    const tag = (event.target as HTMLElement)?.tagName?.toUpperCase()
    if (tag === 'INPUT' || tag === 'TEXTAREA') return
    event.preventDefault()
    const step = event.shiftKey ? 10 : 1
    const layoutItem = layout.value.find(l => l.i === selectedItem.value!.i)
    if (layoutItem) {
      let newLeft = layoutItem.left || 0
      let newTop = layoutItem.top || 0
      const w = Number(layoutItem.width) || 200
      const h = Number(layoutItem.height) || 200
      
      if (event.key === 'ArrowLeft') newLeft = Math.max(0, newLeft - step)
      if (event.key === 'ArrowRight') newLeft = Math.min(canvasWidth.value - w, newLeft + step)
      if (event.key === 'ArrowUp') newTop = Math.max(0, newTop - step)
      if (event.key === 'ArrowDown') newTop = Math.min(canvasHeight.value - h, newTop + step)
      
      // 碰撞检测
      const collision = checkCollisionWithItems(newLeft, newTop, w, h, layoutItem.i)
      if (!collision) {
        layoutItem.left = newLeft
        layoutItem.top = newTop
        selectedItem.value = { ...selectedItem.value, left: newLeft, top: newTop }
        layout.value = [...layout.value]
        saveToHistory()
      }
    }
  }
  // Ctrl+D 复制选中组件（带碰撞检测）
  if (event.ctrlKey && event.key === 'd' && selectedItem.value) {
    event.preventDefault()
    const src = layout.value.find(l => l.i === selectedItem.value!.i)
    if (src) {
      const w = Number(src.width) || 200
      const h = Number(src.height) || 200
      let cloneLeft = (src.left || 0) + 30
      let cloneTop = (src.top || 0) + 30
      
      // 碰撞检测：找到不重叠的位置
      if (hasCollision(cloneLeft, cloneTop, w, h)) {
        const validPos = findValidDropPosition(cloneLeft, cloneTop, w, h)
        cloneLeft = validPos.x
        cloneTop = validPos.y
      }
      
      const clone = {
        ...JSON.parse(JSON.stringify(src)),
        i: `clone-${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
        left: cloneLeft,
        top: cloneTop
      }
      layout.value.push(clone)
      layout.value = [...layout.value]
      selectedItem.value = clone
      saveToHistory()
      message.success('已复制组件')
    }
  }
}

// 监听路由参数变化，重新加载页面定义
watch(() => route.params["id"], async (newId, oldId) => {
  // 如果ID变化了，重新加载
  if (newId !== oldId) {
    await loadPageDefinition()
  }
})

// 窗口大小变化处理（全屏模式下自动调整画布）
const handleWindowResize = () => {
  if (isFullscreenDesign.value) {
    calculateAutoCanvasSize()
    nextTick(() => {
      resizeAllCharts()
    })
  }
}

// 初始化
onMounted(async () => {
  try {
    await Promise.all([
      loadCharts(),
      loadDataSources(),
      loadTemplates()
    ])
    await loadPageDefinition()
    
    // 画布自适应容器
    nextTick(() => handleFitToView())
    
    // 初始化历史记录
    saveToHistory()
    
    // 添加键盘事件监听
    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('keydown', handleSpaceKeyDown)
    window.addEventListener('keyup', handleSpaceKeyUp)
    
    // 添加窗口大小变化监听
    window.addEventListener('resize', handleWindowResize)
  } catch (error) {
    handleApiError(error, '初始化页面设计器')
    layout.value = []
  }
})

// 清理
onBeforeUnmount(() => {
  // 移除键盘事件监听
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keydown', handleSpaceKeyDown)
  window.removeEventListener('keyup', handleSpaceKeyUp)
  
  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleWindowResize)
  
  // 清理防抖定时器
  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
  
  // 清理所有定时器
  chartInstanceTimers.forEach((timer) => {
    clearTimeout(timer)
  })
  chartInstanceTimers.clear()
  
  // 清理图表实例
  chartInstances.forEach((instance, chartId) => {
    try {
      if ((instance as any)._resizeHandler) {
        window.removeEventListener('resize', (instance as any)._resizeHandler)
      }
      if (isChartInstanceValid(instance)) {
        instance.dispose()
      }
    } catch (e) {
      console.warn(`清理图表实例 ${chartId} 失败:`, e)
    }
  })
  chartInstances.clear()
  
  // 清理加载Promise
  chartLoadingPromises.clear()
  
  // 清理画布点击事件
  if (canvasRef.value) {
    canvasRef.value.removeEventListener('click', handleCanvasClick)
  }
  
  // 重置加载状态（但保留数据缓存，以便下次快速加载）
  chartLoadingMap.value = {}
})
</script>

<style scoped>
.page-designer-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.designer-header {
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.header-center {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.toolbar-row {
  align-items: center;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
}
.toolbar-row::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: transparent;
  transition: background 0.3s;
}
/* 移动端模式工具栏底部绿色条 */
.mobile-mode-toolbar::after {
  background: linear-gradient(90deg, #18a058, #36d399);
}
/* 大屏模式工具栏底部蓝色条 */
.bigscreen-mode-toolbar::after {
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-hover));
}

.toolbar-label {
  font-size: 12px;
  color: #666;
}

.toolbar-row.fullscreen-toolbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 全屏设计模式 */
.page-designer-container.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  background: #f5f7fa;
}

.page-designer-container.fullscreen-mode .designer-content {
  padding-top: 50px;
  height: calc(100vh - 50px);
}

.page-designer-container.fullscreen-mode .designer-center-panel {
  background: #f0f2f5;
  flex: 1;
}

.page-designer-container.fullscreen-mode .canvas-wrapper {
  padding: 10px;
  height: 100%;
  overflow: auto;
}

/* 全屏模式下隐藏中间面板的卡片标题和边距 */
.page-designer-container.fullscreen-mode .designer-center-panel .panel-card :deep(.n-card__header) {
  display: none;
}

.page-designer-container.fullscreen-mode .designer-center-panel .panel-card :deep(.n-card__content) {
  padding: 0;
  height: 100%;
}

.designer-content {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 0;
  padding: 0;
  overflow: hidden;
  background: #fff;
}

.designer-left-panel {
  width: 280px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #fafbfc;
  border-right: 1px solid #e8e8e8;
  position: relative;
  transition: width 0.3s ease, min-width 0.3s ease;
}

.designer-left-panel.panel-collapsed {
  width: 40px;
  min-width: 40px;
  overflow: hidden;
}

.designer-left-panel.panel-collapsed > .panel-card {
  display: none;
}

/* 全屏模式下折叠的面板完全隐藏 */
.fullscreen-mode .designer-left-panel.panel-collapsed {
  width: 0;
  min-width: 0;
  border: none;
}

.fullscreen-mode .designer-left-panel.panel-collapsed .panel-collapse-btn {
  display: none;
}

.designer-center-panel {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.designer-right-panel {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #fafbfc;
  border-left: 1px solid #e8e8e8;
  position: relative;
  transition: width 0.3s ease, min-width 0.3s ease;
}

.designer-right-panel.panel-collapsed {
  width: 40px;
  min-width: 40px;
  overflow: hidden;
}

.designer-right-panel.panel-collapsed > .panel-card {
  display: none;
}

/* 全屏模式下折叠的面板完全隐藏 */
.fullscreen-mode .designer-right-panel.panel-collapsed {
  width: 0;
  min-width: 0;
  border: none;
}

.fullscreen-mode .designer-right-panel.panel-collapsed .panel-collapse-btn {
  display: none;
}

/* 全屏模式下的浮动展开按钮 */
.floating-panel-btn {
  position: fixed;
  top: 70px;
  z-index: 1001;
  background: #fff;
  border: 1px solid #e8e8e8;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s;
  font-size: 12px;
  color: #666;
}

.floating-panel-btn:hover {
  background: #f0f9f4;
  border-color: #18a058;
  color: #18a058;
}

.floating-panel-btn.left {
  left: 0;
  border-radius: 0 8px 8px 0;
  border-left: none;
}

.floating-panel-btn.right {
  right: 0;
  border-radius: 8px 0 0 8px;
  border-right: none;
}

/* 面板折叠按钮 */
.panel-collapse-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  background: #fff;
  border: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: all 0.2s;
}

.panel-collapse-btn:hover {
  background: #f0f9f4;
  border-color: #18a058;
  color: #18a058;
}

.panel-collapse-btn.left {
  right: -12px;
  border-radius: 0 4px 4px 0;
}

.panel-collapse-btn.right {
  left: -12px;
  border-radius: 4px 0 0 4px;
}

.panel-collapsed .panel-collapse-btn.left {
  right: 8px;
}

.panel-collapsed .panel-collapse-btn.right {
  left: 8px;
}

.panel-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 确保 n-card 内容区域也有正确的高度 */
:deep(.n-card) {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: none;
  border-radius: 0;
}

:deep(.n-card__content) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  overflow-y: auto;
  padding: 16px;
}

.chart-item {
  cursor: move;
  transition: all 0.2s;
  border-radius: 4px;
  padding: 8px;
}

.chart-item:hover {
  background: #f0f9f4;
  border-color: #18a058;
}

.canvas-wrapper {
  flex: 1;
  overflow: auto;
  overflow-x: auto;
  overflow-y: auto;
  background: #f5f7fa;
  position: relative;
  min-height: 0;
  width: 100%;
  transition: background 0.4s ease;
}

.canvas-wrapper.is-panning {
  cursor: grabbing;
  user-select: none;
}

.canvas-wrapper::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}

.canvas-wrapper::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 6px;
}

.canvas-wrapper::-webkit-scrollbar-thumb {
  background: #c0c0c0;
  border-radius: 6px;
}

.canvas-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}

/* 水平滚动条 */
.canvas-wrapper::-webkit-scrollbar:horizontal {
  height: 12px;
}

/* 垂直滚动条 */
.canvas-wrapper::-webkit-scrollbar:vertical {
  width: 12px;
}

/* 缩放包装器，确保缩放后的尺寸能够触发滚动 */
.canvas-scale-wrapper {
  position: relative;
  flex-shrink: 0;
  /* 确保包装器有明确的尺寸，能够触发滚动 */
  display: inline-block;
}

.design-canvas {
  position: relative;
  /* width and min-width are set via inline style from canvasWidth */
  min-height: 400px;
  background: #fff;
  background-image: 
    linear-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 20px 20px;
  margin: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-sizing: border-box;
  flex-shrink: 0;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  transition: box-shadow 0.3s, border-color 0.3s;
}

.design-canvas.drag-over {
  border-color: #18a058;
  background-color: #f0f9f4;
}

/* 对齐辅助线 */
.snap-lines-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 9999;
}
.snap-line-v,
.snap-line-h {
  stroke: #f5222d;
  stroke-width: 1;
  stroke-dasharray: 4 3;
  opacity: 0.7;
}

.canvas-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
}

.chart-item-wrapper {
  box-sizing: border-box;
  border: 2px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.chart-item-wrapper:hover {
  border-color: #18a058;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.2);
  z-index: 20 !important;
}

.chart-item-wrapper.active {
  border-color: #18a058;
  border-width: 3px;
  box-shadow: 0 4px 16px rgba(24, 160, 88, 0.3);
  z-index: 10;
}

/* 碰撞状态样式 - 正在拖拽的组件 */
.chart-item-wrapper.is-colliding {
  border-color: #ff4d4f !important;
  border-width: 3px;
  box-shadow: 0 0 0 4px rgba(255, 77, 79, 0.3), 0 4px 16px rgba(255, 77, 79, 0.4) !important;
  animation: collision-pulse 0.5s ease-in-out infinite;
}

/* 碰撞状态样式 - 被碰撞的目标组件 */
.chart-item-wrapper.collision-target {
  border-color: #faad14 !important;
  border-width: 2px;
  box-shadow: 0 0 0 3px rgba(250, 173, 20, 0.25) !important;
}

@keyframes collision-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.85; }
}

.chart-item-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  position: relative;
}

.chart-item-wrapper:hover .chart-item-content {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(0,0,0,0.08);
  flex-shrink: 0;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  min-height: 24px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
}

.chart-item-wrapper:hover .item-header,
.chart-item-wrapper.active .item-header {
  opacity: 1;
  pointer-events: auto;
}

.item-header .chart-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-header .chart-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(24, 160, 88, 0.2) 0%, rgba(24, 160, 88, 0.1) 100%);
  color: #18a058;
  transition: transform 0.3s ease;
}

.chart-item-wrapper:hover .chart-icon {
  transform: scale(1.1);
}

.item-content {
  box-sizing: border-box;
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 8px;
  min-height: 100px;
  position: relative;
  overflow: hidden;
}

.chart-preview-container {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  min-height: 100px;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 25px;
}

.table-preview-placeholder {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(135deg, rgba(54, 207, 201, 0.08) 0%, rgba(54, 207, 201, 0.15) 100%);
  border-radius: 6px;
  color: #36cfc9;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.inline-chart-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(135deg, rgba(250, 200, 88, 0.08) 0%, rgba(250, 200, 88, 0.15) 100%);
  border-radius: 6px;
  color: #d4a017;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.chart-icon.inline-icon {
  background: linear-gradient(135deg, rgba(250, 200, 88, 0.2) 0%, rgba(250, 200, 88, 0.1) 100%) !important;
  color: #d4a017 !important;
}

.chart-item-wrapper:hover .table-preview-placeholder {
  background: linear-gradient(135deg, rgba(54, 207, 201, 0.12) 0%, rgba(54, 207, 201, 0.2) 100%);
}

.chart-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 5;
}

.item-size-info {
  position: absolute;
  bottom: 4px;
  right: 8px;
  pointer-events: none;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 8px;
  border-radius: 4px;
  z-index: 5;
  font-size: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

/* 查询参数样式 */
.params-section {
  padding: 0 4px;
}

.params-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-item {
  padding: 8px 12px;
  background: #f9f9f9;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
}

.param-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.param-info {
  display: flex;
  gap: 6px;
}

/* 查询组件列表样式 */
.query-components-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.query-component-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: grab;
  transition: all 0.2s;
  font-size: 13px;
}

.query-component-item:hover {
  border-color: #18a058;
  background: #f0faf4;
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.15);
}

.query-component-item:active {
  cursor: grabbing;
}

/* 配置区域标题 */
.config-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

/* 模板面板 */
.template-panel {
  padding: 4px 0;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}
.template-filter-row {
  margin-bottom: 12px;
}
.template-count {
  font-size: 11px;
  color: #8b8fa3;
  margin-bottom: 8px;
  text-align: right;
}
.template-section-title {
  font-size: 11px;
  font-weight: 700;
  color: #8b8fa3;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  padding: 10px 4px 6px;
  margin-bottom: 4px;
}
/* --- 模板卡片 --- */
.tpl-card {
  display: flex;
  flex-direction: column;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}
.tpl-card:hover {
  border-color: #818cf8;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.13);
  transform: translateY(-2px);
}
.tpl-card:active { transform: translateY(0); }
/* --- 预览区域 --- */
.tpl-preview {
  width: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 9px 9px 0 0;
}
.tpl-preview-grid {
  position: absolute;
  inset: 0;
}
.tpl-block {
  position: absolute;
  border-radius: 2px;
  box-sizing: border-box;
}
.tpl-block-chart {
  background: rgba(255, 255, 255, 0.32);
  border: 1px solid rgba(255, 255, 255, 0.5);
}
.tpl-block-static {
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.65);
}
/* --- 卡片信息区 --- */
.tpl-meta {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 8px 10px 9px;
}
.tpl-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  line-height: 1.3;
}
.tpl-subtitle {
  font-size: 11px;
  color: #a0a3b1;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* --- 移动端模板网格 --- */
.tpl-mobile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.tpl-card-mobile {
  align-items: center;
}
.tpl-card-mobile .tpl-title {
  font-size: 12px;
  padding: 6px 0 7px;
  text-align: center;
}
.tpl-preview-mobile {
  aspect-ratio: 9 / 16;
  border-radius: 9px 9px 0 0;
}
/* --- 自定义模板旧样式兼容 --- */
.template-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 10px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.template-card:hover {
  border-color: #818cf8;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.13);
}
.template-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(99, 102, 241, 0.08);
  flex-shrink: 0;
}
.template-info { flex: 1; min-width: 0; }
.template-name { font-size: 13px; font-weight: 600; color: #303133; }
.template-desc { font-size: 11px; color: #a0a3b1; margin-top: 1px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* ==================== 大屏组件样式 ==================== */
.bs-kpi-card {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; border-radius: 8px;
  border: 1px solid rgba(64,158,255,0.4); padding: 16px;
  box-shadow: 0 0 16px rgba(0,212,255,0.08), inset 0 1px 0 rgba(255,255,255,0.03);
  backdrop-filter: blur(4px);
}
.bs-kpi-label { font-size: 13px; margin-bottom: 8px; letter-spacing: 2px; text-transform: uppercase; opacity: 0.85; }

/* 画布静态文字双击就地编辑 */
.inline-editable { cursor: text; }
.inline-editable:hover { outline: 1px dashed rgba(24,160,88,0.6); outline-offset: 2px; border-radius: 2px; }
.inline-edit-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #18a058;
  border-radius: 4px;
  background: #ffffff;
  padding: 2px 6px;
  margin: 0;
  outline: none;
  resize: none;
  font-family: inherit;
  overflow: hidden;
}
textarea.inline-edit-input { height: 100%; min-height: 28px; }
.inline-edit-kpi { text-align: center; letter-spacing: 2px; text-transform: uppercase; }
.bs-kpi-value { font-weight: 700; letter-spacing: 3px; font-family: 'DIN', 'Roboto Mono', monospace; text-shadow: 0 0 20px currentColor; }

.bs-number-flipper {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; border-radius: 8px; padding: 16px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.03);
}
.bs-flipper-label { font-size: 14px; margin-bottom: 12px; letter-spacing: 2px; text-transform: uppercase; opacity: 0.85; }
.bs-flipper-digits { display: flex; gap: 6px; }
.bs-digit {
  background: linear-gradient(180deg, rgba(0,20,40,0.9) 0%, rgba(0,10,25,0.95) 100%);
  border: 1px solid rgba(0,212,255,0.35);
  border-radius: 6px; padding: 6px 10px; font-weight: 700;
  font-family: 'DIN', 'Roboto Mono', monospace; min-width: 40px; text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3), inset 0 1px 0 rgba(0,212,255,0.1);
  text-shadow: 0 0 12px currentColor;
}
.bs-flipper-unit { font-size: 13px; margin-top: 10px; letter-spacing: 1px; }

.bs-decor-border {
  width: 100%; height: 100%; border: 1px solid; border-radius: 4px;
  position: relative; padding: 30px 14px 14px;
  box-shadow: 0 0 12px rgba(0,212,255,0.06);
}
.bs-decor-corner {
  position: absolute; width: 16px; height: 16px;
  border-color: inherit;
}
.bs-decor-corner.tl { top: -1px; left: -1px; border-top: 2px solid; border-left: 2px solid; }
.bs-decor-corner.tr { top: -1px; right: -1px; border-top: 2px solid; border-right: 2px solid; }
.bs-decor-corner.bl { bottom: -1px; left: -1px; border-bottom: 2px solid; border-left: 2px solid; }
.bs-decor-corner.br { bottom: -1px; right: -1px; border-bottom: 2px solid; border-right: 2px solid; }
.bs-decor-title {
  position: absolute; top: 6px; left: 14px; font-size: 14px;
  font-weight: 600; letter-spacing: 2px;
  text-shadow: 0 0 10px currentColor;
}

.bs-header-bar {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: center; position: relative; overflow: hidden;
  border-bottom: 1px solid rgba(0,212,255,0.15);
}
.bs-header-bar::before {
  content: ''; position: absolute; bottom: 0; left: 50%;
  transform: translateX(-50%); width: 60%; height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0,212,255,0.5), transparent);
}
.bs-header-deco-left, .bs-header-deco-right {
  position: absolute; top: 50%; width: 22%; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0,212,255,0.5), transparent);
}
.bs-header-deco-left { left: 5%; transform: translateY(-50%); }
.bs-header-deco-right { right: 5%; transform: translateY(-50%); }
.bs-header-title {
  font-weight: 700; letter-spacing: 6px; text-shadow: 0 0 30px rgba(0,212,255,0.6), 0 2px 4px rgba(0,0,0,0.3);
  z-index: 1;
}
.bs-header-time {
  position: absolute; right: 24px; font-size: 13px; letter-spacing: 2px;
  font-family: 'DIN', 'Roboto Mono', monospace;
}

.bs-progress-bar {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  justify-content: center; border-radius: 8px; padding: 14px 18px;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.03);
}
.bs-progress-label { font-size: 13px; margin-bottom: 10px; letter-spacing: 1px; }
.bs-progress-track {
  width: 100%; height: 12px; background: rgba(255,255,255,0.06);
  border-radius: 6px; overflow: hidden; margin-bottom: 8px;
  border: 1px solid rgba(255,255,255,0.04);
}
.bs-progress-fill {
  height: 100%; border-radius: 6px; transition: width 0.6s ease;
  box-shadow: 0 0 12px rgba(0,212,255,0.4), 0 0 4px rgba(0,212,255,0.2);
  position: relative;
}
.bs-progress-fill::after {
  content: ''; position: absolute; top: 0; left: 0; right: 0;
  height: 50%; background: linear-gradient(180deg, rgba(255,255,255,0.2), transparent);
  border-radius: 6px 6px 0 0;
}
.bs-progress-value { font-size: 20px; font-weight: 700; font-family: 'DIN', 'Roboto Mono', monospace; text-shadow: 0 0 10px currentColor; }

/* ==================== 移动端组件样式 ==================== */
.mb-status-bar {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: space-between; padding: 0 20px;
  background: linear-gradient(180deg, #f8f8f8 0%, #f2f2f2 100%); font-size: 12px; color: #1a1a1a;
  font-family: -apple-system, 'SF Pro Text', sans-serif;
}
.mb-status-time { font-weight: 600; font-size: 14px; }
.mb-status-icons { display: flex; gap: 6px; align-items: center; font-size: 12px; }

.mb-navbar {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: space-between; padding: 0 14px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 1px 0 rgba(0,0,0,0.03);
}
.mb-navbar-back { font-size: 26px; font-weight: 300; cursor: pointer; width: 36px; color: #007aff; }
.mb-navbar-title { font-weight: 600; flex: 1; text-align: center; letter-spacing: 0.5px; }
.mb-navbar-action { width: 36px; }

.mb-tabbar {
  width: 100%; height: 100%; display: flex; align-items: center;
  justify-content: space-around; background: rgba(255,255,255,0.95);
  border-top: 1px solid rgba(0,0,0,0.06); padding: 4px 0;
  backdrop-filter: blur(10px);
}
.mb-tabbar-item {
  display: flex; flex-direction: column; align-items: center;
  gap: 3px; font-size: 10px; color: #8e8e93; cursor: pointer;
  transition: color 0.2s;
}
.mb-tabbar-item.active { color: #007aff; }
.mb-tabbar-icon { font-size: 22px; line-height: 1; }
.mb-tabbar-label { font-size: 10px; font-weight: 500; }

.mb-card {
  width: 100%; height: 100%; padding: 16px 18px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06), 0 0 1px rgba(0,0,0,0.08); overflow: hidden;
}
.mb-card-title { font-size: 16px; font-weight: 600; margin-bottom: 10px; letter-spacing: 0.3px; }
.mb-card-body { font-size: 14px; line-height: 1.6; }

.mb-list-item {
  width: 100%; height: 100%; display: flex; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid rgba(0,0,0,0.04);
}
.mb-list-icon { font-size: 24px; margin-right: 14px; flex-shrink: 0; }
.mb-list-content { flex: 1; min-width: 0; }
.mb-list-title { font-size: 16px; font-weight: 500; letter-spacing: 0.2px; }
.mb-list-desc { font-size: 13px; color: #8e8e93; margin-top: 3px; }
.mb-list-arrow { font-size: 20px; color: #c7c7cc; flex-shrink: 0; margin-left: 8px; }

.mb-search-bar {
  width: 100%; height: 100%; display: flex; align-items: center;
  padding: 8px 14px;
}
.mb-search-input {
  flex: 1; display: flex; align-items: center; padding: 9px 16px;
  border: none; font-size: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

/* ==================== 移动端画布外壳样式 ==================== */
.canvas-wrapper.mobile-mode {
  background: linear-gradient(135deg, #e2e2ea 0%, #d5d5de 100%);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 24px;
}
.canvas-scale-wrapper.mobile-frame-wrapper {
  padding: 40px;
  display: flex;
  justify-content: center;
}
.mobile-canvas {
  border: none !important;
  border-radius: 40px !important;
  box-shadow:
    0 0 0 12px #1a1a1a,
    0 0 0 14px #444,
    0 0 0 16px rgba(0,0,0,0.1),
    0 20px 60px rgba(0,0,0,0.35),
    inset 0 0 2px rgba(255,255,255,0.1) !important;
  position: relative;
  overflow: hidden;
  margin: 0 !important;
}
.mobile-canvas::before {
  content: '';
  position: absolute;
  top: 0; left: 50%;
  transform: translateX(-50%);
  width: 150px; height: 30px;
  background: #1a1a1a;
  border-radius: 0 0 20px 20px;
  z-index: 9999;
  pointer-events: none;
}
.mobile-canvas::after {
  content: '';
  position: absolute;
  bottom: 8px; left: 50%;
  transform: translateX(-50%);
  width: 130px; height: 5px;
  background: rgba(0,0,0,0.25);
  border-radius: 3px;
  z-index: 9999;
  pointer-events: none;
}
/* 移动端拖入高亮 */
.mobile-canvas.drag-over {
  background-color: #f0fff5 !important;
}
.mobile-canvas.show-grid {
  background-image:
    linear-gradient(rgba(0,0,0,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.04) 1px, transparent 1px) !important;
}



/* ══════════════════════════════════════════════════════════════
   移动端设计器 — 柔和绿色主题
   ══════════════════════════════════════════════════════════════ */
.mobile-designer {
  background: #f0f7f4 !important;
}
.mobile-designer .designer-header {
  background: linear-gradient(180deg, #f8fdf9 0%, #f0f9f4 100%);
  border-bottom: 1px solid #d4ede0;
}
.mobile-designer .toolbar-row {
  background: #f6fbf8;
  border-bottom: 1px solid #d4ede0;
}
.mobile-designer .designer-content {
  background: #eef6f1;
}
.mobile-designer .designer-left-panel {
  background: #f4faf6;
  border-right: 1px solid #d4ede0;
}
.mobile-designer .designer-right-panel {
  background: #f4faf6;
  border-left: 1px solid #d4ede0;
}
.mobile-designer .designer-center-panel {
  background: #eef6f1;
}
.mobile-designer .panel-collapse-btn {
  background: #f4faf6;
  border-color: #c8e6d5;
}
.mobile-designer .panel-collapse-btn:hover {
  background: #e0f5e8;
  border-color: #18a058;
  color: #18a058;
}
/* 移动端 — 表单标签 */
.mobile-designer :deep(.n-form-item-label) {
  color: #2d6a4f !important;
}
/* 移动端 — 画布空状态 */
.mobile-designer :deep(.n-empty__description) {
  color: #588c6c !important;
}
/* 移动端 — 选中态绿色 */
.mobile-designer .chart-item-wrapper.active {
  border-color: #18a058 !important;
  box-shadow: 0 4px 16px rgba(24,160,88,0.25) !important;
}
/* 移动端 — N-Tag 柔和绿色 */
.mobile-designer :deep(.n-tag--info-type) {
  --n-color: rgba(24,160,88,0.08) !important;
  --n-text-color: #18a058 !important;
}
/* 移动端 — 模板面板 */
.mobile-designer .tpl-card,
.mobile-designer .template-card {
  background: #f8fdf9;
  border-color: #d4ede0;
}
.mobile-designer .tpl-card:hover,
.mobile-designer .template-card:hover {
  background: #e8f5ee;
  border-color: #18a058;
  box-shadow: 0 2px 12px rgba(24,160,88,0.15);
}
.mobile-designer .tpl-title { color: #2d6a4f; }
.mobile-designer .tpl-block-chart {
  background: rgba(24, 160, 88, 0.2);
  border-color: rgba(24, 160, 88, 0.35);
}
.mobile-designer .tpl-block-static {
  background: rgba(24, 160, 88, 0.35);
  border-color: rgba(24, 160, 88, 0.5);
}
/* 移动端 — 配置区域 */
.mobile-designer .config-section-title {
  color: #2d6a4f;
  border-bottom-color: #d4ede0;
}
/* 移动端 — 浮动按钮 */
.mobile-designer .floating-panel-btn {
  background: #f4faf6;
  border-color: #c8e6d5;
}
.mobile-designer .floating-panel-btn:hover {
  background: #e0f5e8;
  border-color: #18a058;
  color: #18a058;
}
</style>

