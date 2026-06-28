<template>
  <div class="page-view-container" :class="themeClass" :style="containerStyle">
    <!-- 预览模式工具栏（仅从设计器预览时显示） -->
    <div v-if="isPreviewMode" class="preview-mode-bar">
      <div class="preview-bar-content">
        <div class="preview-bar-left">
          <n-icon size="16" color="#f59e0b"><CreateOutline /></n-icon>
          <span class="preview-label">预览模式</span>
        </div>
        <div class="preview-bar-right">
          <n-button size="small" type="primary" @click="returnToDesigner">
            <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
            返回设计
          </n-button>
        </div>
      </div>
    </div>
    <!-- 背景装饰 -->
    <div v-if="currentTheme?.glowEffect" class="bg-decoration">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
    </div>

    <!-- 可折叠的参数面板 -->
    <section v-if="hasParameterPanel" class="parameter-section" :class="{ collapsed: isParamCollapsed }">
      <div class="parameter-panel" :style="panelStyle">
        <div class="parameter-header" :style="paramHeaderStyle" @click="toggleParamPanel">
          <div class="param-header-left">
            <div class="param-icon" :style="paramIconStyle">
              <n-icon size="14"><FilterOutline /></n-icon>
            </div>
            <span class="param-title" :style="{ color: currentTheme?.textColor }">{{ t('pageView.queryConditions') }}</span>
            <n-tag v-if="activeParamCount > 0" size="tiny" :bordered="false" type="info" round>
              {{ activeParamCount }}
            </n-tag>
          </div>
          <n-icon size="16" :style="{ color: currentTheme?.subTextColor, transform: isParamCollapsed ? 'rotate(-90deg)' : 'rotate(0)', transition: 'transform 0.3s' }">
            <ChevronDownOutline />
          </n-icon>
        </div>
        <n-collapse-transition :show="!isParamCollapsed">
          <div class="parameter-body">
            <div class="param-row">
              <div
                v-for="comp in sortedParamComponents"
                :key="comp.id"
                class="param-item"
                :style="getParamItemStyle(comp)"
              >
                <label class="param-label" :class="{ required: comp.required }" :style="labelStyle">
                  {{ comp.label }}
                </label>
                <div class="param-input-wrapper">
                  <n-input v-if="comp.type === 'text'" v-model:value="paramValues[comp.name]" :placeholder="comp.placeholder || t('pageView.inputPlaceholder')" size="small" clearable />
                  <n-input-number v-else-if="comp.type === 'number'" v-model:value="paramValues[comp.name]" :placeholder="comp.placeholder || t('pageView.inputPlaceholder')" :min="comp.min ?? 0" :max="comp.max ?? 999999999" size="small" clearable style="width: 100%" />
                  <n-date-picker v-else-if="comp.type === 'date'" v-model:formatted-value="paramValues[comp.name]" type="date" :placeholder="comp.placeholder || t('pageView.selectDate')" :value-format="comp.dateFormat || 'yyyy-MM-dd'" size="small" clearable style="width: 100%" />
                  <n-date-picker v-else-if="comp.type === 'dateRange'" v-model:formatted-value="paramValues[comp.name]" type="daterange" :start-placeholder="t('pageView.dateStart')" :end-placeholder="t('pageView.dateEnd')" :value-format="comp.dateFormat || 'yyyy-MM-dd'" size="small" clearable style="width: 100%" />
                  <n-select v-else-if="comp.type === 'select'" v-model:value="paramValues[comp.name]" :options="comp.options || []" :placeholder="comp.placeholder || t('pageView.selectPlaceholder')" size="small" clearable />
                  <n-select v-else-if="comp.type === 'multiSelect'" v-model:value="paramValues[comp.name]" :options="comp.options || []" :placeholder="comp.placeholder || t('pageView.selectPlaceholder')" size="small" multiple clearable max-tag-count="responsive" />
                </div>
              </div>
              <div class="param-actions">
                <button v-if="parameterPanelConfig.showQueryButton" class="action-btn primary" :style="primaryBtnStyle" @click="handleParameterQuery">
                  <n-icon size="14"><SearchOutline /></n-icon>
                  <span>{{ t('pageView.query') }}</span>
                </button>
                <button v-if="parameterPanelConfig.showResetButton" class="action-btn" :style="secondaryBtnStyle" @click="handleParameterReset">
                  <n-icon size="14"><RefreshOutline /></n-icon>
                  <span>{{ t('pageView.reset') }}</span>
                </button>
              </div>
            </div>
          </div>
        </n-collapse-transition>
      </div>
    </section>

    <!-- 主内容区 -->
    <main ref="contentRef" class="page-content">
      <!-- 页面未发布提示 -->
      <div v-if="pageNotPublished" class="empty-state">
        <div class="empty-icon" :style="emptyIconStyle">
          <n-icon size="48"><LockClosedOutline /></n-icon>
        </div>
        <h3 :style="{ color: currentTheme?.subTextColor }">{{ t('pageView.pageNotPublished') }}</h3>
        <p :style="{ color: currentTheme?.subTextColor, opacity: 0.6 }">{{ t('pageView.pageNotPublishedDesc') }}</p>
        <n-button style="margin-top: 16px" @click="handleBack">{{ t('pageView.goBack') }}</n-button>
      </div>

      <n-spin v-else :show="loading" class="content-spin" :stroke-width="20" :size="60">
        <template #description>
          <div class="loading-description">
            <span :style="{ color: currentTheme?.subTextColor }">{{ t('pageView.dataLoading') }}</span>
          </div>
        </template>
        
        <div v-if="pageCharts.length > 0" class="charts-canvas-frame" :style="getCanvasFrameStyle()">
        <div class="charts-canvas" :style="getCanvasStyle()">
          <!-- 静态组件渲染 -->
          <template v-for="chartItem in sortedCharts" :key="chartItem.id || chartItem.chartId || ('s-' + chartItem.sortOrder)">
          <div
            v-if="chartItem.mode === 'static'"
            class="static-item"
            :style="getStaticItemStyle(chartItem)"
          >
            <!-- 标题 -->
            <template v-if="chartItem.staticType === 'title'">
              <div :style="{ fontSize: (chartItem.staticConfig?.fontSize || 24) + 'px', fontWeight: chartItem.staticConfig?.fontWeight || 'bold', color: chartItem.staticConfig?.color || currentTheme?.textColor || '#333', textAlign: chartItem.staticConfig?.align || 'left', lineHeight: 1.3, height: '100%', display: 'flex', alignItems: 'center' }">
                {{ chartItem.staticConfig?.text || '' }}
              </div>
            </template>
            <!-- KPI卡片 -->
            <template v-else-if="chartItem.staticType === 'kpiCard'">
              <div class="pv-kpi-card" :style="{ background: chartItem.staticConfig?.bgColor || '#fff', borderColor: chartItem.staticConfig?.borderColor || '#e5e7eb' }">
                <div class="pv-kpi-label" :style="{ color: chartItem.staticConfig?.labelColor || '#6b7280' }">{{ chartItem.staticConfig?.label || 'KPI' }}</div>
                <div class="pv-kpi-value" :style="{ color: chartItem.staticConfig?.valueColor || '#111827', fontSize: (chartItem.staticConfig?.valueFontSize || 32) + 'px' }">
                  {{ chartItem.staticConfig?.prefix || '' }}{{ chartItem.staticConfig?.value || '0' }}{{ chartItem.staticConfig?.suffix || '' }}
                </div>
              </div>
            </template>
            <!-- 文本 -->
            <template v-else-if="chartItem.staticType === 'text'">
              <div :style="{ fontSize: (chartItem.staticConfig?.fontSize || 14) + 'px', color: chartItem.staticConfig?.color || currentTheme?.textColor || '#333', textAlign: chartItem.staticConfig?.align || 'left', lineHeight: chartItem.staticConfig?.lineHeight || 1.6, whiteSpace: 'pre-wrap', height: '100%', display: 'flex', alignItems: 'center' }">
                {{ chartItem.staticConfig?.text || '' }}
              </div>
            </template>
            <!-- 分割线 -->
            <template v-else-if="chartItem.staticType === 'divider'">
              <div style="display: flex; align-items: center; height: 100%;">
                <hr :style="{ width: '100%', border: 'none', borderTop: (chartItem.staticConfig?.thickness || 1) + 'px ' + (chartItem.staticConfig?.style || 'solid') + ' ' + (chartItem.staticConfig?.color || '#e0e0e0') }" />
              </div>
            </template>
            <!-- 图片 -->
            <template v-else-if="chartItem.staticType === 'image'">
              <div style="display: flex; align-items: center; justify-content: center; height: 100%;">
                <img v-if="chartItem.staticConfig?.src" :src="chartItem.staticConfig.src" :alt="chartItem.staticConfig?.alt || ''" :style="{ maxWidth: '100%', maxHeight: '100%', objectFit: chartItem.staticConfig?.objectFit || 'contain' }" />
              </div>
            </template>
            <!-- 大屏: 标题栏 -->
            <template v-else-if="chartItem.staticType === 'headerBar'">
              <div class="pv-header-bar" :style="{ background: chartItem.staticConfig?.bgColor || 'linear-gradient(90deg, rgba(6,30,60,0.9) 0%, rgba(12,21,39,0.95) 50%, rgba(6,30,60,0.9) 100%)' }">
                <div class="pv-header-title" :style="{ color: chartItem.staticConfig?.titleColor || '#00d4ff', fontSize: (chartItem.staticConfig?.titleFontSize || 28) + 'px' }">
                  {{ chartItem.staticConfig?.title || '' }}
                </div>
                <div v-if="chartItem.staticConfig?.showTime !== false" class="pv-header-time" :style="{ color: chartItem.staticConfig?.timeColor || '#5b8fae' }">{{ lastUpdateTime }}</div>
              </div>
            </template>
            <!-- 大屏: 数字翻牌器 -->
            <template v-else-if="chartItem.staticType === 'numberFlipper'">
              <div class="pv-number-flipper" :style="{ background: chartItem.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div class="pv-flipper-label" :style="{ color: chartItem.staticConfig?.labelColor || '#8ca8c8' }">{{ chartItem.staticConfig?.label || '' }}</div>
                <div class="pv-flipper-digits">
                  <span v-for="(digit, di) in String(chartItem.staticConfig?.value || '00000').split('')" :key="di" class="pv-digit" :style="{ color: chartItem.staticConfig?.digitColor || '#00ffcc', fontSize: (chartItem.staticConfig?.digitFontSize || 48) + 'px' }">{{ digit }}</span>
                </div>
                <div v-if="chartItem.staticConfig?.unit" class="pv-flipper-unit" :style="{ color: chartItem.staticConfig?.unitColor || '#5b8fae' }">{{ chartItem.staticConfig.unit }}</div>
              </div>
            </template>
            <!-- 大屏: 装饰边框 -->
            <template v-else-if="chartItem.staticType === 'decorBorder'">
              <div class="pv-decor-border" :style="{ borderColor: chartItem.staticConfig?.borderColor || 'rgba(64,158,255,0.6)', background: chartItem.staticConfig?.bgColor || 'rgba(6,30,60,0.5)' }">
                <div class="pv-decor-corner tl"></div><div class="pv-decor-corner tr"></div>
                <div class="pv-decor-corner bl"></div><div class="pv-decor-corner br"></div>
                <div v-if="chartItem.staticConfig?.title" class="pv-decor-title" :style="{ color: chartItem.staticConfig?.titleColor || '#00d4ff' }">{{ chartItem.staticConfig.title }}</div>
              </div>
            </template>
            <!-- 大屏: 进度条 -->
            <template v-else-if="chartItem.staticType === 'progressBar'">
              <div class="pv-progress-bar" :style="{ background: chartItem.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div class="pv-progress-label" :style="{ color: chartItem.staticConfig?.labelColor || '#8ca8c8' }">{{ chartItem.staticConfig?.label || '' }}</div>
                <div class="pv-progress-track">
                  <div class="pv-progress-fill" :style="{ width: (chartItem.staticConfig?.percent || 60) + '%', background: chartItem.staticConfig?.barColor || 'linear-gradient(90deg, #00d4ff, #00ffcc)' }"></div>
                </div>
                <div class="pv-progress-value" :style="{ color: chartItem.staticConfig?.valueColor || '#00d4ff' }">{{ chartItem.staticConfig?.percent || 60 }}%</div>
              </div>
            </template>
            <!-- 移动端: 状态栏 -->
            <template v-else-if="chartItem.staticType === 'statusBar'">
              <div class="pv-status-bar">
                <span class="pv-status-time">{{ new Date().toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) }}</span>
                <div class="pv-status-icons">
                  <span>●●●●</span>
                  <span>WiFi</span>
                  <span>🔋</span>
                </div>
              </div>
            </template>
            <!-- 移动端: 导航栏 -->
            <template v-else-if="chartItem.staticType === 'navbar'">
              <div class="pv-navbar" :style="{ background: chartItem.staticConfig?.bgColor || '#fff' }">
                <span class="pv-navbar-back" :style="{ color: chartItem.staticConfig?.color || '#333' }">‹</span>
                <span class="pv-navbar-title" :style="{ color: chartItem.staticConfig?.color || '#333', fontSize: (chartItem.staticConfig?.fontSize || 17) + 'px' }">{{ chartItem.staticConfig?.title || '页面标题' }}</span>
                <span class="pv-navbar-action"></span>
              </div>
            </template>
            <!-- 移动端: 底部标签栏 -->
            <template v-else-if="chartItem.staticType === 'tabBar'">
              <div class="pv-tabbar">
                <div v-for="(tab, ti) in (chartItem.staticConfig?.tabs || [{icon:'🏠',label:'首页'},{icon:'📊',label:'数据'},{icon:'👤',label:'我的'}])" :key="ti" class="pv-tabbar-item" :class="{ active: ti === (chartItem.staticConfig?.activeIndex || 0) }">
                  <span class="pv-tabbar-icon">{{ tab.icon }}</span>
                  <span class="pv-tabbar-label">{{ tab.label }}</span>
                </div>
              </div>
            </template>
            <!-- 移动端: 卡片容器 -->
            <template v-else-if="chartItem.staticType === 'mobileCard'">
              <div class="pv-mobile-card" :style="{ background: chartItem.staticConfig?.bgColor || '#fff', borderRadius: (chartItem.staticConfig?.radius || 12) + 'px' }">
                <div v-if="chartItem.staticConfig?.title" class="pv-mobile-card-title" :style="{ color: chartItem.staticConfig?.titleColor || '#333' }">{{ chartItem.staticConfig.title }}</div>
                <div class="pv-mobile-card-body" :style="{ color: chartItem.staticConfig?.textColor || '#666' }">{{ chartItem.staticConfig?.content || '' }}</div>
              </div>
            </template>
            <!-- 移动端: 列表项 -->
            <template v-else-if="chartItem.staticType === 'listItem'">
              <div class="pv-list-item" :style="{ background: chartItem.staticConfig?.bgColor || '#fff' }">
                <span v-if="chartItem.staticConfig?.icon" class="pv-list-icon">{{ chartItem.staticConfig.icon }}</span>
                <div class="pv-list-content">
                  <div class="pv-list-title" :style="{ color: chartItem.staticConfig?.titleColor || '#333' }">{{ chartItem.staticConfig?.title || '' }}</div>
                  <div v-if="chartItem.staticConfig?.desc" class="pv-list-desc">{{ chartItem.staticConfig.desc }}</div>
                </div>
                <span class="pv-list-arrow">›</span>
              </div>
            </template>
            <!-- 移动端: 搜索栏 -->
            <template v-else-if="chartItem.staticType === 'searchBar'">
              <div class="pv-search-bar" :style="{ background: chartItem.staticConfig?.bgColor || '#f5f5f5' }">
                <div class="pv-search-input" :style="{ background: chartItem.staticConfig?.inputBg || '#fff', borderRadius: (chartItem.staticConfig?.radius || 20) + 'px' }">
                  <span style="color:#999;margin-right:6px;">🔍</span>
                  <span style="color:#bbb;">{{ chartItem.staticConfig?.placeholder || '搜索...' }}</span>
                </div>
              </div>
            </template>
            <!-- 大屏: 倒计时 -->
            <template v-else-if="chartItem.staticType === 'countdown'">
              <div class="pv-countdown" :style="{ background: chartItem.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div class="pv-countdown-label" :style="{ color: chartItem.staticConfig?.labelColor || '#8ca8c8' }">{{ chartItem.staticConfig?.label || '' }}</div>
                <div class="pv-countdown-digits">
                  <template v-for="(seg, si) in getCountdownSegments(getCountdownRemaining(chartItem))" :key="si">
                    <div class="pv-countdown-block" :style="{ color: chartItem.staticConfig?.valueColor || '#00d4ff', fontSize: (chartItem.staticConfig?.digitFontSize || 36) + 'px' }">
                      <span class="pv-countdown-num">{{ seg.value }}</span>
                      <span class="pv-countdown-unit">{{ seg.unit }}</span>
                    </div>
                    <span v-if="si < 3" class="pv-countdown-colon" :style="{ color: chartItem.staticConfig?.valueColor || '#00d4ff' }">:</span>
                  </template>
                </div>
              </div>
            </template>
            <!-- 大屏: 滚动列表 -->
            <template v-else-if="chartItem.staticType === 'scrollList'">
              <div class="pv-scroll-list" :style="{ background: chartItem.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div v-if="chartItem.staticConfig?.title" class="pv-scroll-list-header" :style="{ color: chartItem.staticConfig?.titleColor || '#00d4ff' }">
                  {{ chartItem.staticConfig.title }}
                </div>
                <div class="pv-scroll-list-body">
                  <div class="pv-scroll-list-track" :style="{ animationDuration: (chartItem.staticConfig?.speed || 10) + 's' }">
                    <div v-for="(item, ii) in (chartItem.staticConfig?.items || ['数据项 1', '数据项 2', '数据项 3', '数据项 4', '数据项 5'])" :key="ii" class="pv-scroll-list-item" :style="{ color: chartItem.staticConfig?.textColor || '#c8d8e8' }">
                      <span class="pv-scroll-rank" :style="{ background: Number(ii) < 3 ? (['#ff6b6b','#ffd43b','#69db7c'][Number(ii)] || 'rgba(255,255,255,0.1)') : 'rgba(255,255,255,0.1)', color: Number(ii) < 3 ? '#fff' : (chartItem.staticConfig?.textColor || '#c8d8e8') }">{{ Number(ii) + 1 }}</span>
                      <span class="pv-scroll-text">{{ item }}</span>
                    </div>
                    <!-- 重复一次实现无缝滚动 -->
                    <div v-for="(item, ii) in (chartItem.staticConfig?.items || ['数据项 1', '数据项 2', '数据项 3', '数据项 4', '数据项 5'])" :key="'r-' + ii" class="pv-scroll-list-item" aria-hidden="true" :style="{ color: chartItem.staticConfig?.textColor || '#c8d8e8' }">
                      <span class="pv-scroll-rank" :style="{ background: Number(ii) < 3 ? (['#ff6b6b','#ffd43b','#69db7c'][Number(ii)] || 'rgba(255,255,255,0.1)') : 'rgba(255,255,255,0.1)', color: Number(ii) < 3 ? '#fff' : (chartItem.staticConfig?.textColor || '#c8d8e8') }">{{ Number(ii) + 1 }}</span>
                      <span class="pv-scroll-text">{{ item }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <!-- 大屏: 跑马灯 -->
            <template v-else-if="chartItem.staticType === 'marquee'">
              <div class="pv-marquee" :style="{ background: chartItem.staticConfig?.bgColor || 'rgba(6,30,60,0.8)' }">
                <div class="pv-marquee-track" :style="{ animationDuration: (chartItem.staticConfig?.speed || 12) + 's', color: chartItem.staticConfig?.textColor || '#00d4ff', fontSize: (chartItem.staticConfig?.fontSize || 16) + 'px' }">
                  <span>{{ chartItem.staticConfig?.text || '数据可视化大屏 实时数据接入 多维度分析' }}</span>
                  <span style="margin: 0 60px; opacity: 0.5;">|</span>
                  <span>{{ chartItem.staticConfig?.text || '数据可视化大屏 实时数据接入 多维度分析' }}</span>
                </div>
              </div>
            </template>
          </div>
          <!-- 图表/表格卡片渲染 -->
          <article
            v-else
            class="chart-card"
            :style="getChartCardStyle(chartItem)"
          >
            <div v-if="currentTheme?.glowEffect" class="card-glow"></div>
            <header class="card-header" :style="cardHeaderStyle">
              <div class="card-actions">
                <n-dropdown :options="getChartMenuOptions(chartItem)" placement="bottom-end" @select="(key: string) => handleChartMenu(key, chartItem)">
                  <button class="card-menu-btn" :style="cardMenuBtnStyle">
                    <n-icon size="16"><EllipsisVerticalOutline /></n-icon>
                  </button>
                </n-dropdown>
              </div>
            </header>
            <div class="card-body" :style="cardBodyStyle">
              <!-- 表格类型 -->
              <template v-if="isTableChartType(getEffectiveChartType(chartItem))">
                <div class="table-container custom-header-table" :style="getTableHeaderStyle(chartItem)">
                  <n-data-table
                    :columns="getTableColumns(chartItem)"
                    :data="getPaginatedTableData(chartItem)"
                    :bordered="getTableStyle(chartItem).bordered"
                    :single-line="false"
                    :striped="getTableStyle(chartItem).striped"
                    :size="getTableStyle(chartItem).size"
                    :max-height="getTableMaxHeight(chartItem)"
                    :scroll-x="getTableScrollX(chartItem)"
                    :loading="chartItem.chartId ? (chartLoadingStates.get(chartItem.chartId) ?? false) : false"
                    :theme-overrides="getTableThemeOverrides(chartItem)"
                    :style="getTableHeaderStyle(chartItem)"
                    class="custom-header-table"
                  >
                    <template #empty>
                      <n-empty :description="t('pageView.noData')" size="small" />
                    </template>
                  </n-data-table>
                </div>
                <div v-if="getShowTablePagination(chartItem)" class="card-table-pagination">
                  <n-pagination
                    :page="getTablePaginationState(chartItem).page"
                    :page-size="getTablePaginationState(chartItem).pageSize"
                    :item-count="getTableDataCount(chartItem)"
                    :page-sizes="[10, 20, 50]"
                    show-size-picker
                    size="small"
                    :prefix="() => t('pageView.totalRecords', { count: getTableDataCount(chartItem) })"
                    @update:page="(p) => handleCardTablePageChange(chartItem, p)"
                    @update:page-size="(s) => handleCardTablePageSizeChange(chartItem, s)"
                  />
                </div>
              </template>
              <!-- ECharts 图表 -->
              <template v-else>
                <div 
                  :ref="(el: any) => setChartRef(el as HTMLElement | null, chartItem)" 
                  :data-chart-id="chartItem.chartId"
                  :data-chart-item-id="getChartItemId(chartItem)"
                  class="chart-container"
                ></div>
              </template>
            </div>
          </article>
          </template>
        </div>
        </div>
        
        <div v-else class="empty-state">
          <div class="empty-icon" :style="emptyIconStyle">
            <n-icon size="48"><BarChartOutline /></n-icon>
          </div>
          <h3 :style="{ color: currentTheme?.subTextColor }">{{ t('pageView.noChartData') }}</h3>
          <p :style="{ color: currentTheme?.subTextColor, opacity: 0.6 }">{{ t('pageView.noChartDataDesc') }}</p>
        </div>
      </n-spin>
    </main>

    <!-- 大屏控制工具栏（右上角，可隐藏） -->
    <div class="screen-toolbar" :class="{ 'is-fullscreen': isFullscreen, 'is-visible': toolbarVisible }">
      <!-- 工具栏主体 -->
      <div class="toolbar-panel">
        <!-- 自动刷新 -->
        <n-tooltip trigger="hover" placement="bottom">
          <template #trigger>
            <button 
              class="toolbar-btn" 
              :class="{ active: autoRefreshEnabled }"
              @click="toggleAutoRefresh"
            >
              <n-icon size="16"><TimeOutline /></n-icon>
              <span v-if="autoRefreshEnabled" class="countdown-text">{{ autoRefreshCountdown }}s</span>
            </button>
          </template>
          <span>{{ autoRefreshEnabled ? t('pageView.autoRefreshOn') : t('pageView.autoRefreshOff') }}</span>
        </n-tooltip>

        <!-- 刷新间隔选择 -->
        <n-dropdown 
          v-if="autoRefreshEnabled" 
          :options="refreshIntervalOptions" 
          placement="bottom"
          @select="setAutoRefreshInterval"
        >
          <button class="toolbar-btn interval-btn">
            {{ autoRefreshInterval }}s
            <n-icon size="12"><ChevronDownOutline /></n-icon>
          </button>
        </n-dropdown>

        <!-- 手动刷新 -->
        <n-tooltip trigger="hover" placement="bottom">
          <template #trigger>
            <button class="toolbar-btn" :class="{ 'is-loading': isRefreshing }" @click="refreshAllCharts">
              <n-icon size="16" :class="{ 'spin-animation': isRefreshing }"><RefreshOutline /></n-icon>
            </button>
          </template>
          <span>{{ t('pageView.refreshAll') }}</span>
        </n-tooltip>

        <!-- 分隔线 -->
        <span class="toolbar-divider"></span>

        <!-- 全屏按钮 -->
        <n-tooltip trigger="hover" placement="bottom">
          <template #trigger>
            <button class="toolbar-btn fullscreen-btn" @click="toggleFullscreen">
              <n-icon size="18">
                <ContractOutline v-if="isFullscreen" />
                <ExpandOutline v-else />
              </n-icon>
              <span class="btn-text">{{ isFullscreen ? '退出' : '全屏' }}</span>
            </button>
          </template>
          <span>{{ isFullscreen ? t('pageView.exitFullscreen') : t('pageView.fullscreen') }}</span>
        </n-tooltip>

        <!-- 隐藏按钮 -->
        <button class="toolbar-btn hide-btn" @click="toolbarVisible = false">
          <n-icon size="14"><ChevronDownOutline /></n-icon>
        </button>
      </div>
    </div>

    <!-- 显示工具栏的触发器（工具栏隐藏时显示，右侧居中偏上） -->
    <div 
      v-if="!toolbarVisible" 
      class="toolbar-trigger" 
      :class="{ 'is-fullscreen': isFullscreen }"
      @click="toolbarVisible = true"
    >
      <n-icon size="14"><ChevronDownOutline /></n-icon>
    </div>

    <!-- 全屏模式下的页面标题栏 -->
    <div v-if="isFullscreen" class="fullscreen-header-bar">
      <div class="header-left">
        <span class="header-title">{{ pageName }}</span>
      </div>
      <div class="header-right">
        <span class="header-time">{{ lastUpdateTime }}</span>
      </div>
    </div>

    <!-- 全屏查看模态框 -->
    <n-modal v-model:show="showFullscreenModal" preset="card" :style="fullscreenModalStyle" :bordered="false" :segmented="{ content: true }" @after-leave="handleFullscreenClose">
      <template #header>
        <div class="fullscreen-header">
          <div class="fullscreen-title-area">
            <div class="fullscreen-icon">
              <n-icon size="16">
                <GridOutline v-if="isTableChartType(fullscreenChartType)" />
                <StatsChartOutline v-else />
              </n-icon>
            </div>
            <span>{{ fullscreenChartName }}</span>
            <n-tag v-if="isTableChartType(fullscreenChartType)" size="small" type="info">{{ t('pageView.dataTable') }}</n-tag>
          </div>
          <div class="fullscreen-actions">
            <n-button size="small" quaternary @click="handleFullscreenRefresh">
              <template #icon><n-icon><SyncOutline /></n-icon></template>
              {{ t('pageView.refreshBtn') }}
            </n-button>
            <n-button v-if="fullscreenChartType !== 'table'" size="small" quaternary @click="handleFullscreenSaveImage">
              <template #icon><n-icon><ImageOutline /></n-icon></template>
              {{ t('pageView.saveImage') }}
            </n-button>
            <n-button v-if="isTableChartType(fullscreenChartType)" size="small" quaternary @click="handleFullscreenExportTable">
              <template #icon><n-icon><DownloadOutline /></n-icon></template>
              {{ t('pageView.exportExcel') }}
            </n-button>
          </div>
        </div>
      </template>
      <!-- 表格全屏 -->
      <div v-if="isTableChartType(fullscreenChartType)" class="fullscreen-table-wrapper">
        <div class="fullscreen-table-container custom-header-table" :style="fullscreenTableHeaderStyle">
          <n-data-table
            :columns="fullscreenTableColumns"
            :data="fullscreenPaginatedData"
            :bordered="true"
            :single-line="false"
            striped
            size="medium"
            :scroll-x="1200"
            :loading="fullscreenLoading"
            :max-height="fullscreenTableMaxHeight"
          >
            <template #empty>
              <n-empty :description="t('pageView.noData')" />
            </template>
          </n-data-table>
        </div>
        <div v-if="fullscreenTableData.length > 0" class="fullscreen-table-pagination">
          <n-pagination
            v-model:page="fullscreenPage"
            :page-size="fullscreenPageSize"
            :item-count="fullscreenTableData.length"
            :page-sizes="[10, 20, 50, 100]"
            show-size-picker
            :prefix="() => t('pageView.totalRecords', { count: fullscreenTableData.length })"
            @update:page-size="(s) => { fullscreenPageSize = s; fullscreenPage = 1 }"
          />
        </div>
      </div>
      <!-- 图表全屏 -->
      <div v-else ref="fullscreenChartRef" class="fullscreen-chart-container"></div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, nextTick, onBeforeUnmount, computed, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NDropdown, NInput, NInputNumber, NDatePicker, NSelect, NSpin, NTag, NCollapseTransition, NModal, NButton, NDataTable, NEmpty, NPagination, NTooltip, useMessage } from 'naive-ui'
import { 
  RefreshOutline, ExpandOutline, DownloadOutline, 
  BarChartOutline, FilterOutline, SearchOutline, StatsChartOutline,
  GridOutline, ChevronDownOutline, EllipsisVerticalOutline,
  ImageOutline, SyncOutline, ExpandOutline as FullscreenOutline,
  LockClosedOutline, TimeOutline, ContractOutline, ArrowBackOutline, CreateOutline
} from '@vicons/ionicons5'
import echarts, { ensureExtendedChartsForOption } from '@/utils/echarts'
import { getPageDefinitionById } from '@/api/page'
import { getChartData, getChartDefinitionById } from '@/api/chart'
import { testInlineChartSql } from '@/api/pageDesigner'
import { handleApiError } from '@/utils/error'
import { buildChartOption, buildInlineChartOption } from '@/utils/chartRenderer'
import { exportToExcel } from '@/utils/export'
import { formatCellValueSmart } from '@/utils/format'
import type { PageChart, PageTheme } from '@/types/page'
import { PAGE_THEMES } from '@/types/page'
import type { ChartDefinition } from '@/types/chart'
import type { QueryComponent, PageParameterPanel } from '@/types/pageParameter'
import { createDefaultParameterPanel } from '@/types/pageParameter'
import { useTabsStore } from '@/stores/tabs'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const tabsStore = useTabsStore()

const route = useRoute()
const router = useRouter()
const message = useMessage()

const pageName = ref('')
const pageCharts = ref<PageChart[]>([])
const pageTheme = ref<PageTheme | null>(null)
const chartInstances = new Map<string, echarts.ECharts>()
const chartDefinitions = reactive(new Map<number, ChartDefinition>())
const chartDataCache = new Map<number, any[]>()
const chartLoadingPromises = new Map<number, Promise<any>>()
const chartLoadingStates = ref(new Map<number, boolean>())
const tableDataCache = new Map<string, any[]>()
const inlineChartDataCache = new Map<string, any[]>()
const loading = ref(false)
const lastUpdateTime = ref('')
const isParamCollapsed = ref(false)
const pageNotPublished = ref(false)
const contentRef = ref<HTMLElement | null>(null)
const pageLayoutConfig = ref<{ width: number; height: number } | null>(null)
const contentWidth = ref(0)

// ===== Countdown timer logic =====
const countdownRemaining = reactive(new Map<string, number>()) // key = chartItem unique id
let countdownInterval: ReturnType<typeof setInterval> | null = null

const getChartItemUniqueId = (item: PageChart): string => {
  return String(item.id ?? item.chartId ?? item.sortOrder ?? Math.random())
}

const getCountdownRemaining = (item: PageChart): number => {
  const id = getChartItemUniqueId(item)
  if (countdownRemaining.has(id)) return countdownRemaining.get(id)!
  const target = item.staticConfig?.targetTimestamp as number | undefined
  if (target && target > Date.now()) {
    return Math.floor((target - Date.now()) / 1000)
  }
  return item.staticConfig?.seconds as number ?? 86400
}

const getCountdownSegments = (remaining: number) => {
  const r = Math.max(0, remaining)
  const d = Math.floor(r / 86400)
  const h = Math.floor((r % 86400) / 3600)
  const m = Math.floor((r % 3600) / 60)
  const s = r % 60
  return [
    { value: String(d).padStart(2, '0'), unit: '天' },
    { value: String(h).padStart(2, '0'), unit: '时' },
    { value: String(m).padStart(2, '0'), unit: '分' },
    { value: String(s).padStart(2, '0'), unit: '秒' }
  ]
}

const initCountdownTimers = () => {
  const countdownItems = pageCharts.value.filter(c => c.staticType === 'countdown')
  countdownItems.forEach(item => {
    const id = getChartItemUniqueId(item)
    const target = item.staticConfig?.targetTimestamp as number | undefined
    const initial = target && target > Date.now()
      ? Math.floor((target - Date.now()) / 1000)
      : (item.staticConfig?.seconds as number ?? 86400)
    countdownRemaining.set(id, initial)
  })
  if (countdownItems.length > 0 && !countdownInterval) {
    countdownInterval = setInterval(() => {
      pageCharts.value.filter(c => c.staticType === 'countdown').forEach(item => {
        const id = getChartItemUniqueId(item)
        const cur = countdownRemaining.get(id) ?? 0
        if (cur > 0) countdownRemaining.set(id, cur - 1)
      })
    }, 1000)
  }
}

// 全屏查看相关
const showFullscreenModal = ref(false)
const fullscreenChartRef = ref<HTMLElement | null>(null)
const fullscreenChartName = ref('')
const fullscreenChartItem = ref<PageChart | null>(null)
const fullscreenChartType = ref('')
const fullscreenTableColumns = ref<any[]>([])
const fullscreenTableData = ref<any[]>([])
const fullscreenLoading = ref(false)
const fullscreenPage = ref(1)
const fullscreenPageSize = ref(20)
let fullscreenChartInstance: echarts.ECharts | null = null

// 全屏表格表头样式
const fullscreenTableHeaderStyle = computed(() => {
  if (!fullscreenChartItem.value) {
    return {
      '--header-bg-color': '#f5f7fa',
      '--header-text-color': '#303133',
      '--header-font-weight': '600'
    }
  }
  const tableStyle = getTableStyle(fullscreenChartItem.value)
  return {
    '--header-bg-color': tableStyle.headerBgColor || '#f5f7fa',
    '--header-text-color': tableStyle.headerTextColor || '#303133',
    '--header-font-weight': tableStyle.headerFontWeight === 'bold' ? '600' : '400'
  }
})

// 全屏表格手动分页数据
const fullscreenPaginatedData = computed(() => {
  if (fullscreenTableData.value.length === 0) return []
  const start = (fullscreenPage.value - 1) * fullscreenPageSize.value
  return fullscreenTableData.value.slice(start, start + fullscreenPageSize.value)
})

// 全屏表格最大高度（替代 flex-height，避免 flex 布局下高度无法解析）
const fullscreenTableMaxHeight = computed(() => {
  const wrapperHeight = window.innerHeight * 0.85 - 100
  const paginationHeight = fullscreenTableData.value.length > 0 ? 42 : 0
  const tableHeaderHeight = 46 // medium size 表头高度
  return Math.max(200, wrapperHeight - paginationHeight - tableHeaderHeight)
})

const fullscreenModalStyle = computed(() => ({
  width: '90vw',
  height: '85vh',
  maxWidth: '1600px'
}))

// 参数面板
const parameterPanelConfig = reactive<PageParameterPanel>(createDefaultParameterPanel())
const parameterComponents = ref<QueryComponent[]>([])
const paramValues = ref<Record<string, any>>({})

// 当前主题
const currentTheme = computed(() => pageTheme.value || PAGE_THEMES[0])
const themeClass = computed(() => `theme-${currentTheme.value?.value || 'default'}`)

// 活跃参数数量
const activeParamCount = computed(() => {
  return Object.values(paramValues.value).filter(v => v !== undefined && v !== null && v !== '').length
})

// 切换参数面板
const toggleParamPanel = () => {
  isParamCollapsed.value = !isParamCollapsed.value
}

// 图表菜单选项
const getChartMenuOptions = (chartItem: PageChart) => {
  const isTable = isTableChartType(getEffectiveChartType(chartItem))
  const tableStyle = isTable ? getTableStyle(chartItem) : null
  
  const options = [
    { label: t('pageView.refreshChart'), key: 'refresh', icon: () => h(NIcon, null, { default: () => h(SyncOutline) }) },
  ]
  
  if (isTable) {
    // 表格类型：导出Excel
    if (tableStyle?.enableExport) {
      options.push({ label: t('pageView.exportExcel'), key: 'exportExcel', icon: () => h(NIcon, null, { default: () => h(DownloadOutline) }) })
    }
  } else {
    // 图表类型：保存图片
    options.push({ label: t('pageView.saveImage'), key: 'saveImage', icon: () => h(NIcon, null, { default: () => h(ImageOutline) }) })
  }
  
  options.push({ label: t('pageView.fullscreenView'), key: 'fullscreen', icon: () => h(NIcon, null, { default: () => h(FullscreenOutline) }) })
  
  return options
}

// 处理图表菜单
const handleChartMenu = async (key: string, chartItem: PageChart) => {
  const instanceKey = getChartInstanceKey(chartItem)
  const instance = chartInstances.get(instanceKey)
  
  switch (key) {
    case 'refresh':
      if (isInlineChart(chartItem) && instance) {
        // 内联图表刷新
        inlineChartDataCache.delete(getChartItemId(chartItem))
        await loadAndRenderInlineChartView(chartItem, instance)
        message.success(t('pageView.refreshChart'))
      } else if (isTableChartType(chartItem.chart?.chartType) && chartItem.chartId) {
        tableDataCache.delete(String(chartItem.chartId))
        await loadTableData(chartItem.chartId)
        message.success(t('pageView.refreshChart'))
      } else if (instance && chartItem.chartId) {
        chartDataCache.delete(chartItem.chartId)
        await loadAndRenderChart(chartItem.chartId, instance)
        message.success(t('pageView.refreshChart'))
      }
      break
    case 'saveImage':
      if (instance) {
        const instanceOption = instance.getOption() as any
        const rawBg = instanceOption?.backgroundColor
        const bgColor = (rawBg && rawBg !== 'transparent') ? rawBg : '#fff'
        const url = instance.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: bgColor })
        const link = document.createElement('a')
        link.download = `${chartItem.chart?.chartName || 'chart'}.png`
        link.href = url
        link.click()
        message.success(t('pageView.imageSaved'))
      }
      break
    case 'exportExcel':
      handleExportTableExcel(chartItem)
      break
    case 'fullscreen':
      openFullscreenChart(chartItem)
      break
  }
}

// 导出表格数据为Excel
const handleExportTableExcel = (chartItem: PageChart) => {
  const data = getResolvedTableData(chartItem)
  if (data.length === 0) {
    message.warning(t('pageView.noData'))
    return
  }
  
  const tableStyle = getTableStyle(chartItem)
  const filename = tableStyle.exportFileName || chartItem.chart?.chartName || t('pageView.dataTable')
  
  // 如果配置了显示字段，只导出这些字段
  let exportData = data
  if (tableStyle.displayColumns && tableStyle.displayColumns.length > 0) {
    exportData = data.map(row => {
      const newRow: Record<string, any> = {}
      tableStyle.displayColumns.forEach((col: string) => {
        const label = tableStyle.columnLabels?.[col] || col
        newRow[label] = row[col]
      })
      return newRow
    })
  }
  
  try {
    exportToExcel(exportData, filename, 100000, (warning) => {
      message.warning(warning)
    })
    message.success(t('pageView.exportSuccess'))
  } catch (error) {
    console.error('导出失败:', error)
    message.error(t('common.operationFailed'))
  }
}

// 打开全屏查看
const openFullscreenChart = async (chartItem: PageChart) => {
  fullscreenChartName.value = getEffectiveChartName(chartItem)
  fullscreenChartItem.value = chartItem
  fullscreenChartType.value = getEffectiveChartType(chartItem)
  showFullscreenModal.value = true
  fullscreenPage.value = 1
  
  await nextTick()
  
  // 表格类型
  if (isTableChartType(getEffectiveChartType(chartItem))) {
    fullscreenLoading.value = true
    try {
      const data = isInlineChart(chartItem)
        ? getResolvedTableData(chartItem)
        : chartItem.chartId
          ? await loadChartData(chartItem.chartId)
          : []
      fullscreenTableData.value = data
      fullscreenTableColumns.value = []
      
      // 生成列定义
      if (data.length > 0) {
        const firstRow = data[0]
        const tableStyle = getTableStyle(chartItem)
        
        const columns: any[] = []
        
        // 序号列
        if (tableStyle.showIndex !== false) {
          columns.push({ title: '#', key: '_index', width: 60, align: 'center', render: (_: any, index: number) => index + 1 })
        }
        
        // 获取要显示的字段
        const displayFields = tableStyle.displayColumns && tableStyle.displayColumns.length > 0
          ? tableStyle.displayColumns
          : Object.keys(firstRow)
        
        displayFields.forEach((key: string) => {
          if (Object.prototype.hasOwnProperty.call(firstRow, key)) {
            const label = tableStyle.columnLabels && tableStyle.columnLabels[key] ? tableStyle.columnLabels[key] : key
            columns.push({
              title: label,
              key: key,
              ellipsis: { tooltip: true },
              resizable: true,
              sorter: 'default',
              align: tableStyle.headerAlign || 'left',
              render: (row: any) => {
                const value = row[key]
                if (value === null || value === undefined) return '-'
                return formatCellValueSmart(value, { fieldName: key, fieldTitle: label })
              }
            })
          }
        })
        
        fullscreenTableColumns.value = columns
      }
    } catch (error) {
      handleApiError(error, 'loadTableData')
    } finally {
      fullscreenLoading.value = false
    }
    return
  }
  
  // ECharts 图表
  setTimeout(async () => {
    if (fullscreenChartRef.value) {
      if (fullscreenChartInstance) {
        fullscreenChartInstance.dispose()
        fullscreenChartInstance = null
      }
      
      fullscreenChartInstance = echarts.init(fullscreenChartRef.value, currentTheme.value?.isDark ? 'dark' : undefined)
      if (isInlineChart(chartItem)) {
        await loadAndRenderInlineChartView(chartItem, fullscreenChartInstance)
      } else if (chartItem.chartId) {
        await loadAndRenderFullscreenChart(chartItem.chartId)
      }
    }
  }, 100)
}

// 加载全屏图表数据
const loadAndRenderFullscreenChart = async (chartId: number) => {
  if (!fullscreenChartInstance) return
  
  try {
    let chartDef: ChartDefinition | undefined = chartDefinitions.get(chartId)
    if (!chartDef) {
      const chartItem = pageCharts.value.find(c => c.chartId === chartId)
      if (chartItem?.chart) {
        chartDef = chartItem.chart
        chartDefinitions.set(chartId, chartDef!)
      } else {
        const res = await getChartDefinitionById(chartId)
        if ((res as any)?.data) {
          chartDef = (res as any).data as ChartDefinition
          chartDefinitions.set(chartId, chartDef!)
        }
      }
    }
    
    if (!chartDef) {
      fullscreenChartInstance.setOption({
        graphic: [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: { text: '图表配置不存在', fontSize: 14, fill: '#999' }
        }]
      }, true)
      return
    }
    
    const data = await loadChartData(chartId)
    const option = buildChartOption(chartDef!, data)
    option.toolbox = { show: false }
    if (currentTheme.value?.chartColorScheme) {
      option.color = currentTheme.value.chartColorScheme
    }
    fullscreenChartInstance.setOption(option, true)
  } catch (error) {
    handleApiError(error, 'loadChartData')
  }
}

// 全屏刷新
const handleFullscreenRefresh = async () => {
  if (!fullscreenChartItem.value) return
  const item = fullscreenChartItem.value
  
  if (isInlineChart(item) && isTableChartType(fullscreenChartType.value)) {
    fullscreenTableData.value = getResolvedTableData(item)
    fullscreenPage.value = 1
    message.success(t('pageView.refreshChart'))
    return
  }

  if (isInlineChart(item)) {
    inlineChartDataCache.delete(getChartItemId(item))
    if (fullscreenChartInstance) {
      await loadAndRenderInlineChartView(item, fullscreenChartInstance)
      message.success(t('pageView.refreshChart'))
    }
    return
  }
  
  if (item.chartId) {
    chartDataCache.delete(item.chartId)
    tableDataCache.delete(String(item.chartId))
  }
  
  if (isTableChartType(fullscreenChartType.value) && item.chartId) {
    fullscreenLoading.value = true
    try {
      const data = await loadChartData(item.chartId)
      fullscreenTableData.value = data
      fullscreenPage.value = 1
      message.success(t('pageView.refreshChart'))
    } catch (error) {
      handleApiError(error, 'refreshData')
    } finally {
      fullscreenLoading.value = false
    }
  } else if (item.chartId) {
    await loadAndRenderFullscreenChart(item.chartId)
    message.success(t('pageView.refreshChart'))
  }
}

// 全屏导出表格
const handleFullscreenExportTable = () => {
  if (fullscreenTableData.value.length === 0) {
    message.warning(t('pageView.noData'))
    return
  }
  
  // 简单的 CSV 导出
  const headers = Object.keys(fullscreenTableData.value[0])
  const csvContent = [
    headers.join(','),
    ...fullscreenTableData.value.map(row => 
      headers.map(h => {
        const val = row[h]
        // 处理包含逗号或引号的值
        if (typeof val === 'string' && (val.includes(',') || val.includes('"'))) {
          return `"${val.replace(/"/g, '""')}"`
        }
        return val ?? ''
      }).join(',')
    )
  ].join('\n')
  
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${fullscreenChartName.value}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  message.success(t('pageView.exportSuccess'))
}

// 全屏保存图片
const handleFullscreenSaveImage = () => {
  if (fullscreenChartInstance) {
    const bgColor = currentTheme.value?.isDark ? '#1e293b' : '#fff'
    const url = fullscreenChartInstance.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: bgColor })
    const link = document.createElement('a')
    link.download = `${fullscreenChartName.value}.png`
    link.href = url
    link.click()
    message.success(t('pageView.imageSaved'))
  }
}

// 关闭全屏
const handleFullscreenClose = () => {
  if (fullscreenChartInstance) {
    fullscreenChartInstance.dispose()
    fullscreenChartInstance = null
  }
  fullscreenChartItem.value = null
  fullscreenChartType.value = ''
  fullscreenTableColumns.value = []
  fullscreenTableData.value = []
}

// 动态样式
const containerStyle = computed(() => ({
  background: currentTheme.value?.backgroundGradient || currentTheme.value?.backgroundColor,
  color: currentTheme.value?.textColor
}))


const panelStyle = computed(() => ({
  background: currentTheme.value?.glassEffect ? 'rgba(255, 255, 255, 0.03)' : currentTheme.value?.cardBackgroundColor,
  borderColor: currentTheme.value?.borderColor,
  backdropFilter: currentTheme.value?.glassEffect ? 'blur(20px)' : 'none'
}))

const paramHeaderStyle = computed(() => ({
  background: currentTheme.value?.glassEffect ? `linear-gradient(90deg, ${currentTheme.value?.primaryColor}15 0%, transparent 100%)` : `linear-gradient(90deg, ${currentTheme.value?.primaryColor}10 0%, transparent 100%)`,
  borderColor: currentTheme.value?.borderColor
}))

const paramIconStyle = computed(() => ({
  background: `linear-gradient(135deg, ${currentTheme.value?.primaryColor} 0%, ${currentTheme.value?.primaryColor}cc 100%)`,
  color: '#fff'
}))

const labelStyle = computed(() => ({ color: currentTheme.value?.subTextColor }))

const getParamItemStyle = (comp: QueryComponent) => ({
  width: comp.width ? `${comp.width}px` : '220px',
  minWidth: comp.type === 'dateRange' ? '280px' : '180px',
  flex: '1 1 auto',
  maxWidth: comp.type === 'dateRange' ? '380px' : '320px'
})

const primaryBtnStyle = computed(() => ({
  background: `linear-gradient(135deg, ${currentTheme.value?.primaryColor} 0%, ${currentTheme.value?.primaryColor}cc 100%)`,
  color: '#fff', border: 'none', boxShadow: `0 4px 15px ${currentTheme.value?.primaryColor}40`
}))

const secondaryBtnStyle = computed(() => ({
  background: currentTheme.value?.isDark ? 'rgba(255, 255, 255, 0.08)' : currentTheme.value?.secondaryColor,
  color: currentTheme.value?.textColor, borderColor: currentTheme.value?.borderColor
}))

const cardHeaderStyle = computed(() => ({
  background: 'transparent',
  borderColor: 'transparent'
}))

const cardMenuBtnStyle = computed(() => ({
  background: 'transparent', border: 'none', color: currentTheme.value?.subTextColor, cursor: 'pointer'
}))

const cardBodyStyle = computed(() => ({ background: 'transparent' }))

const emptyIconStyle = computed(() => ({
  background: currentTheme.value?.glassEffect ? 'rgba(255, 255, 255, 0.03)' : currentTheme.value?.secondaryColor,
  borderColor: currentTheme.value?.borderColor, color: currentTheme.value?.subTextColor
}))


const hasParameterPanel = computed(() => parameterPanelConfig.visible && parameterComponents.value.length > 0)
const sortedParamComponents = computed(() => [...parameterComponents.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)))

const handleParameterQuery = async () => {
  chartDataCache.clear()
  chartLoadingPromises.clear()
  tableDataCache.clear()
  // chartDefinitions 不清除：图表配置不随参数变化
  // 重新加载所有图表数据
  pageCharts.value.forEach(chartItem => {
    if (chartItem.mode === 'static') return
    const effectiveType = getEffectiveChartType(chartItem)
    if (isTableChartType(effectiveType) && !isInlineChart(chartItem) && chartItem.chartId) {
      loadTableData(chartItem.chartId)
    } else {
      const instanceKey = getChartInstanceKey(chartItem)
      const instance = chartInstances.get(instanceKey)
      if (instance) {
        if (isInlineChart(chartItem)) {
          inlineChartDataCache.delete(getChartItemId(chartItem))
          loadAndRenderInlineChartView(chartItem, instance)
        } else if (chartItem.chartId) {
          chartDataCache.delete(chartItem.chartId)
          loadAndRenderChart(chartItem.chartId, instance)
        }
      }
    }
  })
}

const handleParameterReset = () => {
  // 清空所有参数值
  paramValues.value = {}
  // 重新初始化默认值
  parameterComponents.value.forEach(comp => { 
    if (comp.defaultValue !== undefined && comp.defaultValue !== null) {
      paramValues.value[comp.name] = comp.defaultValue 
    } else {
      // 根据类型设置空值
      paramValues.value[comp.name] = null
    }
  })
  // 清空数据缓存并刷新
  chartDataCache.clear()
  chartLoadingPromises.clear()
  tableDataCache.clear()
  inlineChartDataCache.clear()
  // 重新加载所有图表
  pageCharts.value.forEach(chartItem => {
    if (chartItem.mode === 'static') return
    const effectiveType = getEffectiveChartType(chartItem)
    if (isTableChartType(effectiveType) && !isInlineChart(chartItem) && chartItem.chartId) {
      loadTableData(chartItem.chartId)
    } else {
      const instanceKey = getChartInstanceKey(chartItem)
      const instance = chartInstances.get(instanceKey)
      if (instance) {
        if (isInlineChart(chartItem)) {
          loadAndRenderInlineChartView(chartItem, instance)
        } else if (chartItem.chartId) {
          loadAndRenderChart(chartItem.chartId, instance)
        }
      }
    }
  })
}

const getChartQueryParams = (_chartId: number): Record<string, any> => {
  const params: Record<string, any> = {}
  parameterComponents.value.forEach(comp => {
    const value = paramValues.value[comp.name]
    if (value === undefined || value === null || value === '') return
    // 简化模式：参数名直接对应SQL ${name} 占位符，广播到所有图表
    params[comp.name] = value
  })
  return params
}

const sortedCharts = computed(() => {
  return [...pageCharts.value].sort((a, b) => {
    const topA = a.top !== undefined ? a.top : (a.y || 0) * 50
    const topB = b.top !== undefined ? b.top : (b.y || 0) * 50
    if (topA !== topB) return topA - topB
    return (a.left || 0) - (b.left || 0)
  })
})

const canvasDesignSize = computed(() => {
  if (pageCharts.value.length === 0) return { width: 800, height: 500 }
  // Use saved layoutConfig if available (accurate design dimensions)
  if (pageLayoutConfig.value?.width && pageLayoutConfig.value?.height) {
    return { width: pageLayoutConfig.value.width, height: pageLayoutConfig.value.height }
  }
  // Fallback: compute from chart positions with symmetric padding
  let maxBottom = 0, maxRight = 0, minLeft = Infinity
  pageCharts.value.forEach(chart => {
    const l = Number(chart.left) || 0
    const t = Number(chart.top) || 0
    const w = Number(chart.width) || 300
    const h = Number(chart.height) || 300
    minLeft = Math.min(minLeft, l)
    maxRight = Math.max(maxRight, l + w)
    maxBottom = Math.max(maxBottom, t + h)
  })
  const pad = minLeft > 0 ? minLeft : 16
  return { width: Math.max(800, maxRight + pad), height: Math.max(500, maxBottom + pad) }
})

const canvasScale = computed(() => {
  const ds = canvasDesignSize.value
  const availableWidth = contentWidth.value > 0 ? Math.max(0, contentWidth.value - 24) : ds.width
  if (!ds.width || availableWidth <= 0) return 1
  return Math.min(1, availableWidth / ds.width)
})

const updateContentSize = () => {
  contentWidth.value = contentRef.value?.clientWidth || 0
}

const getCanvasFrameStyle = () => {
  const ds = canvasDesignSize.value
  const scale = canvasScale.value
  return {
    position: 'relative' as const,
    width: `${ds.width * scale}px`,
    height: `${ds.height * scale}px`,
    maxWidth: '100%',
    marginLeft: 'auto',
    marginRight: 'auto',
  }
}

const getCanvasStyle = () => {
  const ds = canvasDesignSize.value
  const scale = canvasScale.value
  return {
    position: 'relative' as const,
    width: `${ds.width}px`,
    height: `${ds.height}px`,
    transform: `scale(${scale})`,
    transformOrigin: 'top left',
  }
}

const getStaticItemStyle = (chartItem: PageChart) => {
  return {
    position: 'absolute' as const,
    left: `${Math.max(0, Number(chartItem.left) || 0)}px`,
    top: `${Math.max(0, Number(chartItem.top) || 0)}px`,
    width: `${Math.max(50, Number(chartItem.width) || 200)}px`,
    height: `${Math.max(30, Number(chartItem.height) || 100)}px`,
  }
}

const getChartCardStyle = (chartItem: PageChart) => {
  return {
    position: 'absolute' as const,
    left: `${Math.max(0, Number(chartItem.left) || 0)}px`,
    top: `${Math.max(0, Number(chartItem.top) || 0)}px`,
    width: `${Math.max(200, Number(chartItem.width) || 400)}px`,
    height: `${Math.max(150, Number(chartItem.height) || 300)}px`,
    background: currentTheme.value?.glassEffect ? 'rgba(255, 255, 255, 0.03)' : currentTheme.value?.cardBackgroundColor,
    borderColor: currentTheme.value?.cardBorderColor,
    backdropFilter: currentTheme.value?.glassEffect ? 'blur(20px)' : 'none'
  }
}

const getChartItemId = (chartItem: PageChart): string => {
  if (chartItem.id) return String(chartItem.id)
  if (chartItem.mode === 'inline') return `inline-${chartItem.left || 0}-${chartItem.top || 0}`
  return `${chartItem.chartId}-${chartItem.left || 0}-${chartItem.top || 0}`
}
const getChartInstanceKey = (chartItem: PageChart): string => getChartItemId(chartItem)

const getTableCacheKey = (chartItem: PageChart): string => chartItem.chartId ? String(chartItem.chartId) : getChartItemId(chartItem)

const defaultTableStyle = () => ({
  striped: true,
  size: 'small' as const,
  showPagination: true,
  pageSize: 10,
  showIndex: true,
  bordered: true,
  displayColumns: [] as string[],
  columnLabels: {} as Record<string, string>,
  enableExport: true,
  exportFileName: '',
  headerBgColor: '#f5f7fa',
  headerTextColor: '#303133',
  headerFontWeight: 'normal',
  headerAlign: 'left'
})

const getInlineSampleTableData = () => [
  { name: 'Product A', category: 'North', amount: 1280, rate: '32%' },
  { name: 'Product B', category: 'East', amount: 960, rate: '24%' },
  { name: 'Product C', category: 'South', amount: 760, rate: '19%' },
  { name: 'Product D', category: 'West', amount: 520, rate: '13%' }
]

const parseMaybeJson = <T = any>(value: unknown): T | undefined => {
  if (!value) return undefined
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as T
    } catch {
      return undefined
    }
  }
  return value as T
}

const resolveInlineConfig = (chartItem: PageChart): any => {
  return parseMaybeJson((chartItem as any).inlineConfig)
}

const normalizeArrayPayload = (payload: any): any[] => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.rows)) return payload.rows
  if (Array.isArray(payload?.records)) return payload.records
  if (Array.isArray(payload?.list)) return payload.list
  return []
}

const getInlineStaticData = (chartItem: PageChart): any[] => {
  const config = resolveInlineConfig(chartItem)
  if (!config) return []
  return normalizeArrayPayload(parseMaybeJson(config.staticData) ?? config.staticData)
}

const getInlineChartConfigObject = (chartItem: PageChart): any => {
  const config = resolveInlineConfig(chartItem)
  if (!config?.chartConfig) return {}
  return parseMaybeJson(config.chartConfig) || {}
}

const getResolvedTableData = (chartItem: PageChart) => {
  const cached = tableDataCache.get(getTableCacheKey(chartItem))
  if (cached && cached.length > 0) return cached
  const staticData = getInlineStaticData(chartItem)
  if (staticData.length > 0) return staticData
  return isInlineChart(chartItem) ? getInlineSampleTableData() : []
}

const normalizeLoadedChartSize = (chart: PageChart, parsedInlineConfig?: any) => {
  const width = Number(chart.width) || 400
  let height = Number(chart.height) || 300

  if (chart.mode === 'static' && parsedInlineConfig && height === 120) {
    const type = parsedInlineConfig.staticType
    if (type === 'title') height = 40
    else if (type === 'divider') height = 30
    else if (type === 'marquee') height = 44
  }

  return { width, height }
}

// 获取图表有效类型（兼容内联/引用）
const getEffectiveChartType = (chartItem: PageChart): string => {
  if (chartItem.mode === 'inline') return resolveInlineConfig(chartItem)?.chartType || 'bar'
  return chartItem.chart?.chartType || ''
}
// 获取图表有效名称
const getEffectiveChartName = (chartItem: PageChart): string => {
  if (chartItem.mode === 'inline') return resolveInlineConfig(chartItem)?.chartName || '内联图表'
  return chartItem.chart?.chartName || '图表'
}
// 是否为内联图表
const isInlineChart = (chartItem: PageChart): boolean => chartItem.mode === 'inline'

// 判断是否为表格类型（包括普通表格、汇总表、透视表）
const isTableChartType = (chartType?: string): boolean => {
  return chartType === 'table' || chartType === 'summaryTable' || chartType === 'pivotTable'
}

// 获取表格样式配置
const getTableStyle = (chartItem: PageChart) => {
  const inlineCfg = resolveInlineConfig(chartItem)
  const inlineChartConfig = getInlineChartConfigObject(chartItem)
  const inlineTableStyle = inlineChartConfig.tableStyle || inlineChartConfig.echarts?.tableStyle || inlineChartConfig.echarts
  if (isInlineChart(chartItem) && inlineTableStyle) {
    const defaults = defaultTableStyle()
    return {
      ...defaults,
      striped: inlineTableStyle.striped !== false,
      size: inlineTableStyle.size || defaults.size,
      showPagination: inlineTableStyle.showPagination !== false,
      pageSize: inlineTableStyle.pageSize || defaults.pageSize,
      showIndex: inlineTableStyle.showIndex !== false,
      bordered: inlineTableStyle.bordered !== false,
      displayColumns: inlineTableStyle.displayColumns || defaults.displayColumns,
      columnLabels: inlineTableStyle.columnLabels || defaults.columnLabels,
      enableExport: inlineTableStyle.enableExport !== false,
      exportFileName: inlineTableStyle.exportFileName || inlineCfg?.chartName || defaults.exportFileName,
      headerBgColor: inlineTableStyle.headerBgColor || defaults.headerBgColor,
      headerTextColor: inlineTableStyle.headerTextColor || defaults.headerTextColor,
      headerFontWeight: inlineTableStyle.headerFontWeight || defaults.headerFontWeight,
      headerAlign: inlineTableStyle.headerAlign || defaults.headerAlign
    }
  }

  // 优先从 chartDefinitions 获取，如果没有则从 chartItem.chart 获取
  let chartDef = chartDefinitions.get(chartItem.chartId!)
  if (!chartDef && chartItem.chart) {
    chartDef = chartItem.chart
    // 缓存到 chartDefinitions
    chartDefinitions.set(chartItem.chartId!, chartDef!)
  }
  
  if (!chartDef?.chartConfig) {
    return defaultTableStyle()
  }
  
  try {
    const parsed = JSON.parse(chartDef.chartConfig)
    // 配置存储在 echarts.tableStyle 中，与ChartCenter保持一致
    const tableStyle = parsed.tableStyle || parsed.echarts?.tableStyle || parsed.echarts || {}
    const defaults = defaultTableStyle()
    return {
      striped: tableStyle.striped !== false,
      size: tableStyle.size || defaults.size,
      showPagination: tableStyle.showPagination !== false,
      pageSize: tableStyle.pageSize || defaults.pageSize,
      showIndex: tableStyle.showIndex !== false,
      bordered: tableStyle.bordered !== false,
      displayColumns: tableStyle.displayColumns || defaults.displayColumns,
      columnLabels: tableStyle.columnLabels || defaults.columnLabels,
      enableExport: tableStyle.enableExport !== false,
      exportFileName: tableStyle.exportFileName || '',
      headerBgColor: tableStyle.headerBgColor || defaults.headerBgColor,
      headerTextColor: tableStyle.headerTextColor || defaults.headerTextColor,
      headerFontWeight: tableStyle.headerFontWeight || defaults.headerFontWeight,
      headerAlign: tableStyle.headerAlign || defaults.headerAlign
    }
  } catch {
    return defaultTableStyle()
  }
}

// 获取表格列定义
const getTableColumns = (chartItem: PageChart) => {
  const data = getResolvedTableData(chartItem)
  if (data.length === 0) return []
  
  const tableStyle = getTableStyle(chartItem)
  const firstRow = data[0]
  const columns: any[] = []
  
  // 序号列 - 使用全局索引
  if (tableStyle.showIndex) {
    columns.push({ 
      title: '#', 
      key: '_globalIndex', 
      width: 60, 
      align: 'center'
    })
  }
  
  // 获取要显示的字段
  const displayFields = tableStyle.displayColumns && tableStyle.displayColumns.length > 0
    ? tableStyle.displayColumns
    : Object.keys(firstRow)
  
  displayFields.forEach((key: string) => {
    if (Object.prototype.hasOwnProperty.call(firstRow, key)) {
      // 使用配置的别名，如果别名存在且不为空则使用别名，否则使用原字段名
      const label = tableStyle.columnLabels && tableStyle.columnLabels[key] ? tableStyle.columnLabels[key] : key
      columns.push({
        title: label,
        key: key,
        minWidth: getTableColumnMinWidth(key, label),
        ellipsis: { tooltip: true },
        resizable: true,
        sorter: true,
        align: tableStyle.headerAlign || 'left',
        render: (row: any) => {
          const value = row[key]
          if (value === null || value === undefined) return '-'
          return formatCellValueSmart(value, { fieldName: key, fieldTitle: label })
        }
      })
    }
  })
  
  return columns
}

// 获取表头样式（CSS变量）- 同时设置自定义变量和Naive UI内部变量
const getTableColumnMinWidth = (key: string, label: string): number => {
  const text = String(label || key || '')
  const lowerKey = String(key || '').toLowerCase()
  if (lowerKey.includes('time') || lowerKey.includes('date')) return 150
  if (lowerKey.includes('amount') || lowerKey.includes('count') || lowerKey.includes('rate') || lowerKey.includes('value')) return 96
  return Math.min(Math.max(text.length * 14 + 40, 96), 150)
}

const getTableScrollX = (chartItem: PageChart) => {
  const columns = getTableColumns(chartItem)
  if (columns.length === 0) return undefined
  const estimatedWidth = columns.reduce((total: number, column: any) => {
    return total + Number(column.width || column.minWidth || 120)
  }, 0)
  const cardWidth = Number(chartItem.width) || 400
  const availableWidth = Math.max(180, cardWidth - 16)
  return estimatedWidth > availableWidth ? estimatedWidth : undefined
}

const getTableHeaderStyle = (chartItem: PageChart) => {
  const tableStyle = getTableStyle(chartItem)
  const bgColor = tableStyle.headerBgColor || '#f5f7fa'
  const textColor = tableStyle.headerTextColor || '#303133'
  const fontWeight = tableStyle.headerFontWeight === 'bold' ? '600' : '400'
  return {
    // 自定义CSS变量
    '--header-bg-color': bgColor,
    '--header-text-color': textColor,
    '--header-font-weight': fontWeight,
    // Naive UI内部CSS变量
    '--n-th-color': bgColor,
    '--n-th-color-hover': bgColor,
    '--n-th-text-color': textColor,
    '--n-th-font-weight': fontWeight,
    '--n-th-icon-color': textColor,
    '--n-th-icon-color-active': textColor
  }
}

// 获取表格主题覆盖配置 - 使用Naive UI的theme-overrides直接设置表头颜色
const getTableThemeOverrides = (chartItem: PageChart) => {
  const tableStyle = getTableStyle(chartItem)
  const bgColor = tableStyle.headerBgColor || '#f5f7fa'
  const textColor = tableStyle.headerTextColor || '#303133'
  const fontWeight = tableStyle.headerFontWeight === 'bold' ? '600' : '400'
  
  return {
    // 表头背景色 - 所有状态都使用相同颜色
    thColor: bgColor,
    thColorHover: bgColor,
    thColorSorting: bgColor,
    thColorModal: bgColor,
    thColorPopover: bgColor,
    // 表头文字颜色
    thTextColor: textColor,
    // 表头字体粗细
    thFontWeight: fontWeight,
    // 排序图标颜色
    thIconColor: textColor,
    thIconColorActive: textColor,
    // 边框颜色
    thButtonColorHover: bgColor
  }
}

// 获取表格数据
const getTableData = (chartItem: PageChart) => {
  const data = getResolvedTableData(chartItem)
  // 添加全局索引
  return data.map((row, index) => ({
    ...row,
    _globalIndex: index + 1
  }))
}

// 获取表格分页配置
const tablePaginationStates = reactive(new Map<string, { page: number, pageSize: number }>())

// 计算表格最大高度（基于卡片尺寸）
// max-height 只作用于表格 body（不含表头），所以还要扣除表头高度
// card-body 使用 flex 布局，此值用于启用 n-data-table 的固定表头 + 可滚动数据区
const getTableMaxHeight = (chartItem: PageChart) => {
  const cardHeight = Number(chartItem.height) || 300
  // 扣除：card-body padding(16), pagination(~40), table header(~42), buffer(4)
  const paginationHeight = getShowTablePagination(chartItem) ? 40 : 0
  return Math.max(60, cardHeight - 16 - paginationHeight - 42)
}

// 获取表格分页状态
const getTablePaginationState = (chartItem: PageChart) => {
  const tableStyle = getTableStyle(chartItem)
  const cacheKey = getTableCacheKey(chartItem)
  if (!tablePaginationStates.has(cacheKey)) {
    tablePaginationStates.set(cacheKey, {
      page: 1,
      pageSize: tableStyle.pageSize || 10
    })
  }
  return tablePaginationStates.get(cacheKey)!
}

// 是否显示表格分页
const getShowTablePagination = (chartItem: PageChart) => {
  const tableStyle = getTableStyle(chartItem)
  return tableStyle.showPagination && getResolvedTableData(chartItem).length > 0
}

// 获取表格数据总数
const getTableDataCount = (chartItem: PageChart) => {
  return getResolvedTableData(chartItem).length
}

// 获取分页后的表格数据
const getPaginatedTableData = (chartItem: PageChart) => {
  const allData = getTableData(chartItem)
  const tableStyle = getTableStyle(chartItem)
  if (!tableStyle.showPagination) return allData
  const state = getTablePaginationState(chartItem)
  const start = (state.page - 1) * state.pageSize
  return allData.slice(start, start + state.pageSize)
}

// 卡片表格分页事件
const handleCardTablePageChange = (chartItem: PageChart, page: number) => {
  const state = getTablePaginationState(chartItem)
  state.page = page
}

const handleCardTablePageSizeChange = (chartItem: PageChart, pageSize: number) => {
  const state = getTablePaginationState(chartItem)
  state.pageSize = pageSize
  state.page = 1
}

// 加载表格数据
const loadTableData = async (chartId: number) => {
  chartLoadingStates.value.set(chartId, true)
  try {
    // 总是从API获取完整的图表配置（确保包含chartConfig中的表头颜色等配置）
    if (!chartDefinitions.has(chartId)) {
      try {
        const res = await getChartDefinitionById(chartId)
        if ((res as any)?.data) {
          chartDefinitions.set(chartId, (res as any).data)
        }
      } catch (e) {
        // API获取失败时，尝试使用chartItem.chart
        const chartItem = pageCharts.value.find(c => c.chartId === chartId)
        if (chartItem?.chart) {
          chartDefinitions.set(chartId, chartItem.chart)
        }
      }
    }
    
    const data = await loadChartData(chartId)
    tableDataCache.set(String(chartId), data)
    // 重置分页到第一页
    if (tablePaginationStates.has(String(chartId))) {
      tablePaginationStates.get(String(chartId))!.page = 1
    }
  } catch (error) {
    console.error('加载表格数据失败:', error)
    tableDataCache.set(String(chartId), [])
  } finally {
    chartLoadingStates.value.set(chartId, false)
  }
}

const setChartRef = (el: HTMLElement | null, chartItem: PageChart) => {
  const instanceKey = getChartInstanceKey(chartItem)

  // 元素移除时清理旧实例
  if (!el) {
    const instance = chartInstances.get(instanceKey)
    if (instance) {
      try { instance.dispose() } catch (_) { /* dispose cleanup */ }
      chartInstances.delete(instanceKey)
    }
    return
  }

  nextTick(() => {
    // 如果已有实例且绑定同一 DOM，跳过；否则先释放旧实例
    if (chartInstances.has(instanceKey)) {
      const existing = chartInstances.get(instanceKey)
      if (existing?.getDom() === el) return
      try { existing?.dispose() } catch (_) { /* dispose cleanup */ }
      chartInstances.delete(instanceKey)
    }

    // DOM 尚未布局完成时延迟重试
    if (el.offsetWidth === 0 || el.offsetHeight === 0) {
      setTimeout(() => setChartRef(el, chartItem), 100)
      return
    }

    // 清理 DOM 上残留的 ECharts 实例
    if ((el as any).__echarts_instance__) {
      try { echarts.getInstanceByDom(el)?.dispose() } catch (_) { /* dispose cleanup */ }
    }

    try {
      const chartInstance = echarts.init(el, currentTheme.value?.isDark ? 'dark' : undefined, {
        renderer: 'canvas',
        width: el.offsetWidth,
        height: el.offsetHeight
      })
      chartInstances.set(instanceKey, chartInstance)

      if (isInlineChart(chartItem)) {
        loadAndRenderInlineChartView(chartItem, chartInstance)
      } else if (chartItem.chartId) {
        loadAndRenderChart(chartItem.chartId, chartInstance)
      }
    } catch (e) {
      console.error('初始化图表失败:', e)
    }
  })
}

const loadChartData = async (chartId: number): Promise<any[]> => {
  const chartParams = getChartQueryParams(chartId)
  if (Object.keys(chartParams).length > 0) {
    try { const dataRes = await getChartData(chartId, { parameters: chartParams, useCache: false }); return ((dataRes as any)?.data as any[]) || [] }
    catch (error) { console.error(`加载图表 ${chartId} 数据失败:`, error); return [] }
  }
  if (chartDataCache.has(chartId)) return chartDataCache.get(chartId)!
  if (chartLoadingPromises.has(chartId)) return (await chartLoadingPromises.get(chartId)) || []
  const loadPromise = (async () => {
    try { const dataRes = await getChartData(chartId); const data = ((dataRes as any)?.data as any[]) || []; chartDataCache.set(chartId, data); return data }
    catch (error) { chartDataCache.set(chartId, []); throw error }
    finally { chartLoadingPromises.delete(chartId) }
  })()
  chartLoadingPromises.set(chartId, loadPromise)
  return await loadPromise
}

// 加载并渲染内联图表
const loadAndRenderInlineChartView = async (chartItem: PageChart, chartInstance: echarts.ECharts) => {
  const config = resolveInlineConfig(chartItem)
  if (!config) {
    try {
      chartInstance.setOption({
        graphic: [{ type: 'group', left: 'center', top: 'middle', children: [
          { type: 'text', style: { text: '📊 图表', fontSize: 16, fill: '#bbb', textAlign: 'center' } },
          { type: 'text', top: 24, style: { text: '请配置数据源', fontSize: 12, fill: '#ccc', textAlign: 'center' } }
        ] }]
      }, true)
    } catch { /* ignore */ }
    return
  }

  const itemKey = getChartItemId(chartItem)
  let data: any[] = []
  const staticData = getInlineStaticData(chartItem)

  if (staticData.length > 0) {
    data = staticData
  } else if (inlineChartDataCache.has(itemKey)) {
    data = inlineChartDataCache.get(itemKey)!
  } else if (config.dataSourceId && config.sqlContent) {
    try {
      const res = await testInlineChartSql({
        dataSourceId: config.dataSourceId,
        sqlContent: config.sqlContent,
        limit: config.dataLimit || 100
      })
      if (res?.data && Array.isArray(res.data)) {
        data = res.data
        inlineChartDataCache.set(itemKey, data)
      }
    } catch (e) {
      console.warn('加载内联图表数据失败:', e)
    }
  }

  try {
    const chartConfig = typeof config.chartConfig === 'string'
      ? config.chartConfig
      : config.chartConfig
        ? JSON.stringify(config.chartConfig)
        : undefined
    const option = buildInlineChartOption(
      config.chartType || 'bar',
      data,
      config.fieldMapping as any,
      config.colorScheme,
      chartConfig
    )
    if (currentTheme.value?.chartColorScheme && !config.colorScheme) {
      option.color = currentTheme.value.chartColorScheme
    }
    option.toolbox = { show: false }
    await ensureExtendedChartsForOption(option, config.chartType || getEffectiveChartType(chartItem))
    chartInstance.setOption(option, true)
  } catch (e) {
    console.warn('渲染内联图表失败:', e)
  }
}

const loadAndRenderChart = async (chartId: number, chartInstance: echarts.ECharts) => {
  try {
    // 获取图表定义（优先缓存 → pageCharts → API）
    let chartDef: ChartDefinition | undefined = chartDefinitions.get(chartId)
    if (!chartDef) {
      const chartItem = pageCharts.value.find(c => c.chartId === chartId)
      if (chartItem?.chart) {
        chartDef = chartItem.chart
        chartDefinitions.set(chartId, chartDef!)
      } else {
        const res = await getChartDefinitionById(chartId)
        if ((res as any)?.data) {
          chartDef = (res as any).data as ChartDefinition
          chartDefinitions.set(chartId, chartDef!)
        }
      }
    }

    if (!chartDef) {
      chartInstance.setOption({
        graphic: [{ type: 'text', left: 'center', top: 'middle', style: { text: '图表配置不存在', fontSize: 14, fill: '#999' } }]
      }, true)
      return
    }

    const data = await loadChartData(chartId)
    const option = buildChartOption(chartDef!, data)
    option.toolbox = { show: false }
    if (currentTheme.value?.chartColorScheme) {
      option.color = currentTheme.value.chartColorScheme
    }
    await ensureExtendedChartsForOption(option, chartDef.chartType)
    chartInstance.setOption(option, true)
  } catch (error) {
    handleApiError(error, 'loadChartData')
  }
}

// ===== 预览模式（从设计器跳转时） =====
const isPreviewMode = computed(() => route.query['preview'] === '1')
const returnToDesigner = () => {
  const pageId = route.params['id']
  router.push({ name: 'PageDesigner', params: { id: String(pageId) } })
}

// ===== 全屏与自动刷新功能 =====
const isFullscreen = ref(false)
const isRefreshing = ref(false)
const toolbarVisible = ref(false) // 默认隐藏工具栏
const autoRefreshEnabled = ref(false)
const autoRefreshInterval = ref(30) // 默认30秒
const autoRefreshCountdown = ref(30)
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

const refreshIntervalOptions = [
  { label: '10秒', key: 10 },
  { label: '30秒', key: 30 },
  { label: '60秒', key: 60 },
  { label: '2分钟', key: 120 },
  { label: '5分钟', key: 300 }
]

const toggleFullscreen = async () => {
  try {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
      isFullscreen.value = true
    } else {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  } catch (e) {
    console.warn('Fullscreen not supported:', e)
  }
}

// 监听全屏状态变化
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
  // 全屏时触发resize以适应新尺寸
  setTimeout(() => handleResize(), 100)
}

// 刷新所有图表
const refreshAllCharts = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  
  try {
    // 清除所有缓存
    chartDataCache.clear()
    tableDataCache.clear()
    inlineChartDataCache.clear()
    
    // 重新加载所有图表
    const promises: Promise<any>[] = []
    
    for (const chartItem of pageCharts.value) {
      if (chartItem.mode === 'static') continue
      
      const instanceKey = getChartInstanceKey(chartItem)
      const instance = chartInstances.get(instanceKey)
      
      if (isInlineChart(chartItem) && instance) {
        promises.push(loadAndRenderInlineChartView(chartItem, instance))
      } else if (isTableChartType(getEffectiveChartType(chartItem)) && chartItem.chartId) {
        promises.push(loadTableData(chartItem.chartId))
      } else if (instance && chartItem.chartId) {
        promises.push(loadAndRenderChart(chartItem.chartId, instance))
      }
    }
    
    await Promise.all(promises)
    lastUpdateTime.value = new Date().toLocaleString()
    message.success(t('pageView.refreshSuccess'))
  } catch (error) {
    console.error('刷新失败:', error)
  } finally {
    isRefreshing.value = false
  }
}

// 切换自动刷新
const toggleAutoRefresh = () => {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

// 设置刷新间隔
const setAutoRefreshInterval = (key: number) => {
  autoRefreshInterval.value = key
  autoRefreshCountdown.value = key
  if (autoRefreshEnabled.value) {
    stopAutoRefresh()
    startAutoRefresh()
  }
}

// 启动自动刷新
const startAutoRefresh = () => {
  autoRefreshCountdown.value = autoRefreshInterval.value
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  
  autoRefreshTimer = setInterval(() => {
    autoRefreshCountdown.value--
    if (autoRefreshCountdown.value <= 0) {
      refreshAllCharts()
      autoRefreshCountdown.value = autoRefreshInterval.value
    }
  }, 1000)
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

const handleResize = () => {
  updateContentSize()
  nextTick(() => chartInstances.forEach(chart => chart.resize()))
}
const handleBack = () => router.push('/page-manage')

const loadPage = async () => {
  const id = route.params["id"]
  if (!id) { message.error(t('pageView.loadFailed')); return }
  loading.value = true
  pageNotPublished.value = false
  chartInstances.forEach(chart => { try { chart.dispose() } catch (_) { /* dispose cleanup */ } })
  chartInstances.clear(); chartDataCache.clear(); chartLoadingPromises.clear(); chartDefinitions.clear()
  pageLayoutConfig.value = null
  try {
    const res = await getPageDefinitionById(Number(id))
    if (!res?.data) { message.error(t('pageView.loadFailed')); return }
    const page = res.data as any
    // 检查页面是否已发布（status === 1 表示启用/已发布）
    if (page.status !== 1) {
      pageNotPublished.value = true
      pageName.value = page.pageName || t('pageView.pageNotPublished')
      tabsStore.updateTabTitle(route.fullPath, pageName.value)
      return
    }
    pageName.value = page.pageName || t('pageView.defaultTitle')
    // 更新标签页标题
    tabsStore.updateTabTitle(route.fullPath, pageName.value)
    // 解析布局配置（画布设计尺寸）
    if (page.layoutConfig) {
      try {
        const cfg = typeof page.layoutConfig === 'string' ? JSON.parse(page.layoutConfig) : page.layoutConfig
        if (cfg.width > 0 && cfg.height > 0) {
          pageLayoutConfig.value = { width: cfg.width, height: cfg.height }
        }
      } catch { /* use fallback */ }
    }
    let themeValue = page.theme || 'default'
    if (typeof themeValue === 'object') themeValue = themeValue.value || 'default'
    pageTheme.value = PAGE_THEMES.find(t => t.value === themeValue) || PAGE_THEMES[0] || null
    if (page.parameterPanel) {
      try {
        const panelConfig = typeof page.parameterPanel === 'string' ? JSON.parse(page.parameterPanel) : page.parameterPanel
        parameterPanelConfig.visible = panelConfig.visible === true
        parameterPanelConfig.height = panelConfig.height || 60
        parameterPanelConfig.showQueryButton = panelConfig.showQueryButton !== false
        parameterPanelConfig.showResetButton = panelConfig.showResetButton !== false
        parameterComponents.value = panelConfig.components || []
        parameterComponents.value.forEach(comp => { if (comp.defaultValue !== undefined) paramValues.value[comp.name] = comp.defaultValue })
      } catch (e) { console.warn('解析parameterPanel配置失败:', e) }
    } else { parameterPanelConfig.visible = false; parameterComponents.value = [] }
    if (page.charts && Array.isArray(page.charts)) {
      pageCharts.value = page.charts.map((chart: PageChart) => {
        // 解析 inlineConfig（后端返回可能是 JSON 字符串）
        let parsedInlineConfig = (chart as any).inlineConfig
        if (typeof parsedInlineConfig === 'string') {
          try { parsedInlineConfig = JSON.parse(parsedInlineConfig) } catch { /* keep as-is */ }
        }
        const normalizedSize = normalizeLoadedChartSize(chart, parsedInlineConfig)
        // 静态组件：从 inlineConfig 中提取 staticType 和 staticConfig
        if (chart.mode === 'static' && parsedInlineConfig) {
          return {
            ...chart,
            left: Number(chart.left) || 0,
            top: Number(chart.top) || 0,
            width: normalizedSize.width,
            height: normalizedSize.height,
            mode: 'static' as const,
            staticType: parsedInlineConfig.staticType,
            staticConfig: parsedInlineConfig.staticConfig || {}
          }
        }
        return {
          ...chart,
          left: Number(chart.left) || 0,
          top: Number(chart.top) || 0,
          width: normalizedSize.width,
          height: normalizedSize.height,
          mode: chart.mode || (chart.chartId ? 'referenced' : 'inline'),
          inlineConfig: parsedInlineConfig || undefined
        }
      })
    } else { pageCharts.value = [] }
    lastUpdateTime.value = new Date().toLocaleString()
    await nextTick()
    setTimeout(() => {
      pageCharts.value.forEach(chartItem => {
        // 静态组件无需初始化图表实例
        if (chartItem.mode === 'static') return
        const effectiveType = getEffectiveChartType(chartItem)
        // 表格类型：加载表格数据（仅引用图表）
        if (isTableChartType(effectiveType) && !isInlineChart(chartItem) && chartItem.chartId) {
          loadTableData(chartItem.chartId)
        } else {
          // ECharts 图表（包括内联图表）
          const instanceKey = getChartInstanceKey(chartItem)
          const itemId = getChartItemId(chartItem)
          const el = document.querySelector(`[data-chart-item-id="${itemId}"]`) as HTMLElement
          if (el && !chartInstances.has(instanceKey)) setChartRef(el, chartItem)
        }
      })
    }, 200)
  } catch (error) { handleApiError(error, 'loadPage') }
  finally { loading.value = false }
}

let resizeObserver: ResizeObserver | null = null
let resizeDebounce: ReturnType<typeof setTimeout> | null = null
onMounted(async () => {
  updateContentSize()
  if (contentRef.value) {
    resizeObserver = new ResizeObserver(() => {
      // Debounced ECharts resize when container dimensions change
      if (resizeDebounce) clearTimeout(resizeDebounce)
      resizeDebounce = setTimeout(() => handleResize(), 150)
    })
    resizeObserver.observe(contentRef.value)
  }
  await loadPage()
  initCountdownTimers()
  updateContentSize()
  window.addEventListener('resize', handleResize)
  document.addEventListener('fullscreenchange', handleFullscreenChange)
})
watch(() => route.params["id"], async (newId) => {
  if (newId) {
    chartInstances.forEach(chart => { try { chart.dispose() } catch (_) { /* dispose cleanup */ } }); chartInstances.clear()
    chartDataCache.clear(); chartLoadingPromises.clear(); chartDefinitions.clear()
    if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
    countdownRemaining.clear()
    await loadPage()
    initCountdownTimers()
  }
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  if (resizeDebounce) { clearTimeout(resizeDebounce); resizeDebounce = null }
  if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null }
  if (countdownInterval) { clearInterval(countdownInterval); countdownInterval = null }
  stopAutoRefresh()
  chartInstances.forEach(chart => { try { chart.dispose() } catch (_) { /* dispose cleanup */ } }); chartInstances.clear()
})
</script>


<style scoped>
/* 预览模式工具栏 */
.preview-mode-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 99998;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-bottom: 1px solid #f59e0b;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.preview-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  max-width: 100%;
}

.preview-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-label {
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
}

.preview-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 预览模式下调整容器上边距 */
.page-view-container:has(.preview-mode-bar) {
  padding-top: 50px;
}

/* 基础容器 */
.page-view-container {
  display: flex;
  flex-direction: column;
  position: relative;
  margin: -20px;
  padding: 10px 12px;
}

/* 全屏模式下的容器样式 */
:fullscreen .page-view-container {
  margin: 0;
  padding: 70px 20px 20px;
  height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
}

:fullscreen .page-content {
  flex: 1;
  overflow: auto;
}

:fullscreen .parameter-section {
  margin-bottom: 16px;
}

:fullscreen .charts-canvas-frame {
  min-height: calc(100vh - 140px);
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
}

.bg-circle.circle-1 {
  width: 600px;
  height: 600px;
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af));
  top: -200px;
  right: -200px;
}

.bg-circle.circle-2 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #00ff88 0%, #00d4ff 100%);
  bottom: -100px;
  left: -100px;
}

/* ===== 大屏控制工具栏（右上角） ===== */
.screen-toolbar {
  position: fixed;
  top: 140px;
  right: 12px;
  z-index: 9999;
  transform: translateY(-10px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.screen-toolbar.is-visible {
  transform: translateY(0);
  opacity: 1;
  pointer-events: auto;
}

.screen-toolbar.is-fullscreen {
  top: 70px;
}

.toolbar-panel {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.98) 100%);
  backdrop-filter: blur(16px);
  border-radius: 12px;
  border: 1px solid rgba(99, 102, 241, 0.15);
  box-shadow: 0 4px 24px rgba(99, 102, 241, 0.12), 0 1px 3px rgba(0, 0, 0, 0.08);
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  min-width: 32px;
  padding: 0 10px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.toolbar-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #334155;
}

.toolbar-btn.active {
  background: rgba(16, 185, 129, 0.15);
  color: #059669;
}

.toolbar-btn.is-loading {
  pointer-events: none;
  opacity: 0.6;
}

.countdown-text {
  font-size: 11px;
  font-weight: 600;
  color: #059669;
}

.interval-btn {
  font-size: 11px;
  padding: 0 6px;
  min-width: auto;
  gap: 2px;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: rgba(0, 0, 0, 0.1);
  margin: 0 4px;
}

.fullscreen-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
  color: #fff !important;
  padding: 0 16px !important;
  min-width: 80px;
  gap: 6px;
}

.fullscreen-btn:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%) !important;
  color: #fff !important;
  transform: scale(1.02);
}

.fullscreen-btn .n-icon {
  color: #fff !important;
}

.fullscreen-btn .btn-text,
.btn-text {
  font-size: 13px;
  font-weight: 600;
  color: #fff !important;
  line-height: 1;
}

.hide-btn {
  padding: 0 4px;
  min-width: 24px;
  color: #94a3b8;
}

.hide-btn:hover {
  color: #64748b;
}

/* 工具栏触发器（隐藏时显示，右侧边缘半圆按钮） */
.toolbar-trigger {
  position: fixed !important;
  top: 50% !important;
  right: 0 !important;
  transform: translateY(-50%);
  z-index: 99999 !important;
  width: 28px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border-radius: 28px 0 0 28px;
  border: none;
  box-shadow: -3px 0 16px rgba(99, 102, 241, 0.4), 0 0 0 3px rgba(99, 102, 241, 0.1);
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-trigger:hover {
  width: 36px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  box-shadow: -4px 0 20px rgba(99, 102, 241, 0.5), 0 0 0 4px rgba(99, 102, 241, 0.15);
}

.toolbar-trigger .n-icon {
  transform: rotate(90deg);
}

.toolbar-trigger.is-fullscreen {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  box-shadow: -3px 0 16px rgba(99, 102, 241, 0.5);
}

.toolbar-trigger.is-fullscreen:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
}

/* 旋转动画 */
.spin-animation {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 全屏模式标题栏 ===== */
.fullscreen-header-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.98) 0%, rgba(15, 23, 42, 0.9) 100%);
  backdrop-filter: blur(16px);
  z-index: 9998;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-time {
  font-size: 13px;
  color: #94a3b8;
  font-family: 'Monaco', 'Consolas', monospace;
}

/* ===== 全屏模式下的工具栏样式调整 ===== */
.screen-toolbar.is-fullscreen .toolbar-panel {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border-color: rgba(99, 102, 241, 0.2);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15), 0 1px 3px rgba(0, 0, 0, 0.1);
}

.screen-toolbar.is-fullscreen .toolbar-btn {
  color: #64748b;
}

.screen-toolbar.is-fullscreen .toolbar-btn:hover {
  background: rgba(99, 102, 241, 0.1);
  color: #6366f1;
}

.screen-toolbar.is-fullscreen .toolbar-btn.active {
  background: rgba(16, 185, 129, 0.15);
  color: #059669;
}

.screen-toolbar.is-fullscreen .countdown-text {
  color: #059669;
}

.screen-toolbar.is-fullscreen .toolbar-divider {
  background: rgba(99, 102, 241, 0.15);
}

.screen-toolbar.is-fullscreen .fullscreen-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff !important;
}

.screen-toolbar.is-fullscreen .fullscreen-btn:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);
  color: #fff !important;
}

.screen-toolbar.is-fullscreen .fullscreen-btn .btn-text {
  color: #fff !important;
}

.screen-toolbar.is-fullscreen .hide-btn {
  color: #94a3b8;
}

.screen-toolbar.is-fullscreen .hide-btn:hover {
  color: #64748b;
}


/* 参数面板 */
.parameter-section {
  padding: 6px 8px 0;
  position: relative;
  z-index: 5;
}

.parameter-section.collapsed .parameter-body {
  display: none;
}

.parameter-panel {
  border-radius: 10px;
  border: 1px solid;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s ease;
}

.parameter-panel:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.parameter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  cursor: pointer;
  border-bottom: 1px solid transparent;
  transition: all 0.2s;
  user-select: none;
}

.param-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.param-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 5px;
  flex-shrink: 0;
}

.param-title {
  font-weight: 500;
  font-size: 13px;
}

.parameter-body {
  padding: 10px 14px 12px;
}

.param-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px 14px;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.param-label {
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
  opacity: 0.75;
}

.param-label.required::after {
  content: '*';
  color: #f5222d;
  margin-left: 2px;
}

.param-input-wrapper {
  min-width: 160px;
  width: 100%;
}

.param-input-wrapper :deep(.n-input),
.param-input-wrapper :deep(.n-select),
.param-input-wrapper :deep(.n-date-picker),
.param-input-wrapper :deep(.n-input-number),
.param-input-wrapper :deep(.n-cascader) {
  width: 100% !important;
}

.param-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-bottom: 1px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  white-space: nowrap;
}

.action-btn:hover {
  filter: brightness(1.08);
}

.action-btn:active {
  transform: scale(0.97);
}

.action-btn.primary:hover {
  box-shadow: 0 4px 14px rgba(24, 160, 88, 0.25);
}

/* 主内容区 */
.page-content {
  flex: 1;
  padding: 0;
  overflow: auto;
  position: relative;
  z-index: 1;
}

.content-spin {
  min-height: 300px;
}

.loading-description {
  margin-top: 16px;
  font-size: 14px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.charts-canvas-frame {
  position: relative;
  width: 100%;
  overflow: visible;
}

.charts-canvas {
  position: relative;
}

/* 图表卡片 */
.chart-card {
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
}

.chart-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 10;
  border-color: rgba(24, 160, 88, 0.3);
}

.card-glow {
  position: absolute;
  inset: -2px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(0, 255, 136, 0.3) 100%);
  z-index: -1;
  opacity: 0;
  transition: opacity 0.4s ease;
  filter: blur(8px);
}

.chart-card:hover .card-glow {
  opacity: 1;
  animation: glowPulse 2s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 8px;
  border-bottom: 0;
  position: absolute;
  top: 0;
  right: 0;
  z-index: 10;
  background: transparent;
  backdrop-filter: none;
  opacity: 0;
  transform: translateY(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
  pointer-events: none;
}

.chart-card:hover .card-header {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.card-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  transition: transform 0.3s ease;
}

.chart-card:hover .card-icon {
  transform: scale(1.1);
}

.card-title {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.card-actions {
  display: flex;
  align-items: center;
  pointer-events: auto;
}

.card-menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  transition: all 0.2s;
  opacity: 0.72;
  backdrop-filter: blur(6px);
}

.chart-card:hover .card-menu-btn {
  opacity: 1;
}

.card-menu-btn:hover {
  background: rgba(0, 0, 0, 0.08) !important;
  transform: scale(1.1);
}

.card-body {
  box-sizing: border-box;
  flex: 1;
  padding: 8px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-container {
  box-sizing: border-box;
  flex: 1;
  width: 100%;
  min-height: 0;
  height: 100%;
}

/* 表格容器 */
.table-container {
  box-sizing: border-box;
  flex: 1;
  width: 100%;
  min-height: 0;
  border-radius: 8px;
}

/* 卡片表格底部固定分页 */
.card-table-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 6px 4px 2px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  animation: fadeIn 0.5s ease-out;
}

.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 2px dashed;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-state h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 500;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* 响应式 - 中等屏幕: 2列网格重排，避免缩放后图表过小 */
@media (max-width: 1024px) and (min-width: 769px) {
  .charts-canvas-frame {
    width: 100% !important;
    height: auto !important;
    max-width: none !important;
  }

  .charts-canvas {
    aspect-ratio: unset !important;
    display: grid !important;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    padding: 12px;
    width: 100% !important;
    height: auto !important;
    transform: none !important;
  }

  .chart-card,
  .static-item {
    position: relative !important;
    left: 0 !important;
    top: auto !important;
    width: 100% !important;
    height: auto !important;
  }

  .chart-card {
    min-height: 300px;
  }

  .static-item {
    min-height: 40px;
  }

  .chart-container {
    flex: none !important;
    height: 280px !important;
  }

  .table-container {
    max-height: 260px;
    overflow: auto;
  }

  .screen-toolbar {
    top: 60px;
    right: 12px;
  }
  
}

/* 响应式 - 小屏幕: 单列流式布局 */
@media (max-width: 768px) {
  .screen-toolbar {
    top: 56px;
    right: 8px;
  }
  
  .toolbar-trigger {
    width: 20px;
    height: 40px;
  }
  
  .toolbar-panel {
    padding: 4px 6px;
    gap: 2px;
  }
  
  .toolbar-btn {
    height: 28px;
    min-width: 28px;
    padding: 0 6px;
  }
  
  .fullscreen-btn {
    padding: 0 8px;
  }
  
  .btn-text {
    display: none;
  }
  
  .parameter-section {
    padding: 12px 16px;
  }
  
  .param-row {
    flex-direction: column;
  }
  
  .param-item {
    width: 100% !important;
  }
  
  .page-content {
    padding: 0;
  }
  
  .charts-canvas {
    aspect-ratio: unset !important;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px;
    width: 100% !important;
    height: auto !important;
    transform: none !important;
  }

  .charts-canvas-frame {
    width: 100% !important;
    height: auto !important;
    max-width: none !important;
  }

  .chart-card,
  .static-item {
    position: relative !important;
    left: 0 !important;
    top: auto !important;
    width: 100% !important;
    height: auto !important;
  }

  .chart-card {
    min-height: 320px;
  }

  .static-item {
    min-height: 48px;
  }

  .chart-container {
    flex: none !important;
    height: 300px !important;
  }

  .table-container {
    max-height: 300px;
    overflow: auto;
  }

  .card-header {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
    position: absolute;
    top: 0;
    right: 0;
  }

  .card-title {
    max-width: none;
  }
}

/* 全屏模态框 */
.fullscreen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.fullscreen-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.fullscreen-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #18a058 0%, #36ad6a 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.3);
}

.fullscreen-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fullscreen-chart-container {
  width: 100%;
  height: calc(85vh - 100px);
  min-height: 400px;
  animation: fadeIn 0.3s ease-out;
}

.fullscreen-table-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: calc(85vh - 100px);
  min-height: 400px;
  animation: fadeIn 0.3s ease-out;
}

.fullscreen-table-container {
  flex: 1;
  width: 100%;
  overflow: hidden;
  min-height: 0;
}

.fullscreen-table-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 10px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 加载骨架屏效果 */
.chart-card.loading .card-body {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.table-container :deep(.n-data-table) {
  --n-border-radius: 8px;
}

/* 自定义表头颜色 - 覆盖Naive UI内部CSS变量，禁止hover变色 */
.custom-header-table.n-data-table {
  --n-th-color: var(--header-bg-color, #f5f7fa) !important;
  --n-th-color-hover: var(--header-bg-color, #f5f7fa) !important;
  --n-th-text-color: var(--header-text-color, #303133) !important;
  --n-th-font-weight: var(--header-font-weight, 600) !important;
  --n-th-icon-color: var(--header-text-color, #303133) !important;
  --n-th-icon-color-active: var(--header-text-color, #303133) !important;
}

.custom-header-table :deep(.n-data-table-th),
.custom-header-table :deep(.n-data-table-th:hover),
.custom-header-table :deep(.n-data-table-th:active),
.custom-header-table :deep(.n-data-table-th:focus),
.custom-header-table :deep(.n-data-table-th--hover),
.custom-header-table :deep(.n-data-table-th--sortable),
.custom-header-table :deep(.n-data-table-th--sortable:hover),
.custom-header-table :deep(.n-data-table-th--sortable:active),
.custom-header-table :deep(.n-data-table-th--sorting) {
  background-color: var(--n-th-color, var(--header-bg-color, #f5f7fa)) !important;
  background: var(--n-th-color, var(--header-bg-color, #f5f7fa)) !important;
  color: var(--n-th-text-color, var(--header-text-color, #303133)) !important;
  font-weight: var(--n-th-font-weight, var(--header-font-weight, 600)) !important;
  transition: none !important;
}

.custom-header-table :deep(.n-data-table-th .n-data-table-th__title),
.custom-header-table :deep(.n-data-table-th:hover .n-data-table-th__title),
.custom-header-table :deep(.n-data-table-th:active .n-data-table-th__title) {
  color: var(--n-th-text-color, var(--header-text-color, #303133)) !important;
}

.custom-header-table :deep(.n-data-table-th .n-data-table-sorter),
.custom-header-table :deep(.n-data-table-th:hover .n-data-table-sorter),
.custom-header-table :deep(.n-data-table-th:active .n-data-table-sorter),
.custom-header-table :deep(.n-data-table-sorter--asc),
.custom-header-table :deep(.n-data-table-sorter--desc) {
  color: var(--n-th-text-color, var(--header-text-color, #303133)) !important;
}

/* 滚动条美化 */
.page-content::-webkit-scrollbar,
.table-container::-webkit-scrollbar,
.fullscreen-table-container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.page-content::-webkit-scrollbar-track,
.table-container::-webkit-scrollbar-track,
.fullscreen-table-container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.page-content::-webkit-scrollbar-thumb,
.table-container::-webkit-scrollbar-thumb,
.fullscreen-table-container::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
  transition: background 0.2s;
}

.page-content::-webkit-scrollbar-thumb:hover,
.table-container::-webkit-scrollbar-thumb:hover,
.fullscreen-table-container::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

/* 深色主题滚动条 */
.theme-dark .page-content::-webkit-scrollbar-track,
.theme-tech .page-content::-webkit-scrollbar-track,
.theme-neon .page-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
}

.theme-dark .page-content::-webkit-scrollbar-thumb,
.theme-tech .page-content::-webkit-scrollbar-thumb,
.theme-neon .page-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
}

.theme-dark .page-content::-webkit-scrollbar-thumb:hover,
.theme-tech .page-content::-webkit-scrollbar-thumb:hover,
.theme-neon .page-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* 深色主题表格样式 */
.theme-dark .table-container :deep(.n-data-table-th),
.theme-tech .table-container :deep(.n-data-table-th),
.theme-neon .table-container :deep(.n-data-table-th) {
  background: rgba(255, 255, 255, 0.05) !important;
}

/* 深色主题卡片悬停效果 */
.theme-dark .chart-card:hover,
.theme-tech .chart-card:hover,
.theme-neon .chart-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
}

/* 深色主题骨架屏 */
.theme-dark .chart-card.loading .card-body,
.theme-tech .chart-card.loading .card-body,
.theme-neon .chart-card.loading .card-body {
  background: linear-gradient(90deg, rgba(255,255,255,0.05) 25%, rgba(255,255,255,0.1) 50%, rgba(255,255,255,0.05) 75%);
  background-size: 200% 100%;
}

/* 科技主题特殊效果 */
.theme-tech .chart-card,
.theme-neon .chart-card {
  border: 1px solid rgba(102, 126, 234, 0.3);
}

.theme-tech .chart-card:hover,
.theme-neon .chart-card:hover {
  border-color: rgba(102, 126, 234, 0.6);
}

/* 霓虹主题发光效果增强 */
.theme-neon .card-glow {
  background: linear-gradient(135deg, rgba(0, 255, 136, 0.4) 0%, rgba(102, 126, 234, 0.4) 100%);
}

.theme-neon .chart-card:hover .card-glow {
  opacity: 1;
  filter: blur(12px);
}

/* 打印样式 */
@media print {
  .screen-toolbar,
  .toolbar-trigger,
  .fullscreen-header-bar,
  .parameter-section,
  .card-actions,
  .bg-decoration {
    display: none !important;
  }
  
  .page-content {
    padding: 0 !important;
  }
  
  .chart-card {
    break-inside: avoid;
    box-shadow: none !important;
    border: 1px solid #ddd !important;
  }
}

/* ===== 静态组件样式 ===== */
.static-item {
  box-sizing: border-box;
  z-index: 1;
  overflow: hidden;
}

.pv-kpi-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  padding: 12px 16px;
  box-sizing: border-box;
}
.pv-kpi-label {
  font-size: 13px;
  margin-bottom: 4px;
}
.pv-kpi-value {
  font-weight: 700;
  line-height: 1.2;
}

.pv-header-bar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 0 24px;
}
.pv-header-title {
  font-weight: 700;
  text-align: center;
  letter-spacing: 4px;
}
.pv-header-time {
  position: absolute;
  right: 24px;
  font-size: 13px;
}

.pv-number-flipper {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  padding: 12px;
  box-sizing: border-box;
}
.pv-flipper-label {
  font-size: 13px;
  margin-bottom: 8px;
}
.pv-flipper-digits {
  display: flex;
  gap: 4px;
}
.pv-digit {
  font-weight: 700;
  font-family: 'Courier New', monospace;
  background: rgba(0,0,0,0.2);
  padding: 4px 8px;
  border-radius: 4px;
  line-height: 1;
}
.pv-flipper-unit {
  font-size: 13px;
  margin-top: 6px;
}

.pv-decor-border {
  height: 100%;
  border: 1px solid;
  border-radius: 4px;
  position: relative;
  padding: 12px;
  box-sizing: border-box;
}
.pv-decor-corner {
  position: absolute;
  width: 12px;
  height: 12px;
  border-color: inherit;
}
.pv-decor-corner.tl { top: -1px; left: -1px; border-top: 2px solid; border-left: 2px solid; }
.pv-decor-corner.tr { top: -1px; right: -1px; border-top: 2px solid; border-right: 2px solid; }
.pv-decor-corner.bl { bottom: -1px; left: -1px; border-bottom: 2px solid; border-left: 2px solid; }
.pv-decor-corner.br { bottom: -1px; right: -1px; border-bottom: 2px solid; border-right: 2px solid; }
.pv-decor-title {
  font-size: 15px;
  font-weight: 600;
  text-align: center;
}

.pv-progress-bar {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 8px;
  padding: 12px 16px;
  box-sizing: border-box;
}
.pv-progress-label {
  font-size: 13px;
  margin-bottom: 8px;
}
.pv-progress-track {
  height: 8px;
  background: rgba(255,255,255,0.1);
  border-radius: 4px;
  overflow: hidden;
}
.pv-progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
}
.pv-progress-value {
  font-size: 14px;
  font-weight: 600;
  margin-top: 6px;
  text-align: right;
}

/* 移动端: 状态栏 */
.pv-status-bar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #000;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}
.pv-status-icons {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 11px;
}

/* 移动端: 导航栏 */
.pv-navbar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  box-shadow: 0 1px 0 rgba(0,0,0,0.06);
}
.pv-navbar-back {
  font-size: 28px;
  font-weight: 300;
  width: 32px;
  cursor: pointer;
}
.pv-navbar-title {
  flex: 1;
  text-align: center;
  font-weight: 600;
}
.pv-navbar-action {
  width: 32px;
}

/* 移动端: 底部标签栏 */
.pv-tabbar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  padding: 4px 0;
}
.pv-tabbar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  color: #999;
  font-size: 10px;
  cursor: pointer;
}
.pv-tabbar-item.active {
  color: #18a058;
}
.pv-tabbar-icon {
  font-size: 20px;
  line-height: 1;
}
.pv-tabbar-label {
  font-size: 10px;
}

/* 移动端: 卡片容器 */
.pv-mobile-card {
  height: 100%;
  padding: 14px 16px;
  box-sizing: border-box;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.pv-mobile-card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
.pv-mobile-card-body {
  font-size: 13px;
  line-height: 1.5;
}

/* 移动端: 列表项 */
.pv-list-item {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 16px;
  box-sizing: border-box;
  border-bottom: 1px solid #f0f0f0;
}
.pv-list-icon {
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}
.pv-list-content {
  flex: 1;
  min-width: 0;
}
.pv-list-title {
  font-size: 15px;
  font-weight: 500;
}
.pv-list-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.pv-list-arrow {
  font-size: 20px;
  color: #ccc;
  margin-left: 8px;
}

/* 移动端: 搜索栏 */
.pv-search-bar {
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 12px;
  box-sizing: border-box;
}
.pv-search-input {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 8px 14px;
  font-size: 14px;
}

/* 大屏: 占位组件 */
.pv-placeholder-widget {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 大屏: 倒计时 */
.pv-countdown {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  padding: 12px;
  box-sizing: border-box;
}
.pv-countdown-label {
  font-size: 13px;
  margin-bottom: 10px;
  text-align: center;
}
.pv-countdown-digits {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pv-countdown-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.pv-countdown-num {
  font-weight: 700;
  font-family: 'Courier New', 'DIN', monospace;
  line-height: 1;
  background: rgba(0,0,0,0.25);
  padding: 4px 8px;
  border-radius: 4px;
  min-width: 2ch;
  text-align: center;
}
.pv-countdown-unit {
  font-size: 11px;
  opacity: 0.7;
  margin-top: 4px;
}
.pv-countdown-colon {
  font-weight: 700;
  font-size: 1.2em;
  opacity: 0.6;
  margin-bottom: 14px;
  animation: blink 1s step-end infinite;
}
@keyframes blink {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 0.15; }
}

/* 大屏: 滚动列表 */
.pv-scroll-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  padding: 10px 12px;
  box-sizing: border-box;
  overflow: hidden;
}
.pv-scroll-list-header {
  font-size: 14px;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  margin-bottom: 6px;
  flex-shrink: 0;
}
.pv-scroll-list-body {
  flex: 1;
  overflow: hidden;
  position: relative;
}
.pv-scroll-list-track {
  animation: scrollUp linear infinite;
  will-change: transform;
}
@keyframes scrollUp {
  0%   { transform: translateY(0); }
  100% { transform: translateY(-50%); }
}
.pv-scroll-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.pv-scroll-rank {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.pv-scroll-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 大屏: 跑马灯 */
.pv-marquee {
  height: 100%;
  display: flex;
  align-items: center;
  border-radius: 4px;
  overflow: hidden;
  padding: 0 12px;
  box-sizing: border-box;
}
.pv-marquee-track {
  white-space: nowrap;
  animation: marqueeScroll linear infinite;
  will-change: transform;
  display: inline-block;
}
@keyframes marqueeScroll {
  0%   { transform: translateX(100vw); }
  100% { transform: translateX(-100%); }
}
</style>

<style>
/* PageView 深色模式（非 scoped） */
html.dark .page-container { background: #0f172a !important; }
html.dark .toolbar-panel { background: rgba(30,41,59,0.95) !important; border-color: rgba(255,255,255,0.1) !important; }
html.dark .toolbar-btn { color: #94a3b8 !important; }
html.dark .toolbar-btn:hover { background: rgba(255,255,255,0.1) !important; color: #e2e8f0 !important; }
html.dark .toolbar-trigger { background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%) !important; }
html.dark .toolbar-trigger:hover { background: linear-gradient(135deg, #4f46e5 0%, #4338ca 100%) !important; }
html.dark .parameter-panel { background: #1e293b !important; border-color: #334155 !important; box-shadow: 0 2px 12px rgba(0,0,0,0.3) !important; }
html.dark .parameter-header { color: #e2e8f0 !important; }
html.dark .param-title { color: #e2e8f0 !important; }
html.dark .param-label { color: #94a3b8 !important; }
html.dark .parameter-body { background: #1e293b !important; }
html.dark .chart-card { background: #1e293b !important; border-color: #334155 !important; box-shadow: 0 2px 8px rgba(0,0,0,0.3) !important; }
html.dark .chart-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.4) !important; }
html.dark .card-header { border-bottom-color: transparent !important; background: transparent !important; backdrop-filter: none !important; }
html.dark .card-title { color: #e2e8f0 !important; }
html.dark .card-menu-btn:hover { background: rgba(255,255,255,0.1) !important; }
html.dark .card-body { background: #1e293b !important; }
html.dark .table-container { background: #1e293b !important; }
html.dark .card-table-pagination { border-top-color: #334155 !important; }
html.dark .empty-state h3 { color: #94a3b8 !important; }
html.dark .empty-state p { color: #64748b !important; }
html.dark .empty-icon { border-color: #334155 !important; color: #64748b !important; }
html.dark .fullscreen-header { color: #e2e8f0 !important; }
html.dark .fullscreen-table-container { background: #1e293b !important; }
html.dark .page-content::-webkit-scrollbar-track { background: rgba(255,255,255,0.05) !important; }
html.dark .page-content::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15) !important; }
html.dark .page-content::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.25) !important; }
html.dark .custom-header-table .n-data-table-th { background: #334155 !important; color: #e2e8f0 !important; border-color: #475569 !important; }
html.dark .custom-header-table .n-data-table-th .n-data-table-th__title { color: #e2e8f0 !important; }
html.dark .custom-header-table .n-data-table .n-data-table-tr--striped .n-data-table-td { background: #1a2536 !important; }
</style>
