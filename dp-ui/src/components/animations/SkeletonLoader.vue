<template>
  <div
    :class="[
      'skeleton-loader',
      `skeleton-loader--${type}`
    ]"
    :style="loaderStyle"
    role="status"
    aria-busy="true"
    :aria-label="ariaLabel"
  >
    <!-- 表格骨架屏 -->
    <template v-if="type === 'table'">
      <div class="skeleton-table">
        <!-- 表头 -->
        <div class="skeleton-table__header">
          <div
            v-for="col in columns"
            :key="`header-${col}`"
            class="skeleton-table__header-cell skeleton-pulse"
          />
        </div>
        <!-- 表格行 -->
        <div
          v-for="row in actualRows"
          :key="`row-${row}`"
          class="skeleton-table__row"
        >
          <div
            v-for="col in columns"
            :key="`cell-${row}-${col}`"
            class="skeleton-table__cell skeleton-pulse"
            :style="{ animationDelay: `${(row * columns + col) * 50}ms` }"
          />
        </div>
      </div>
    </template>

    <!-- 卡片骨架屏 -->
    <template v-else-if="type === 'card'">
      <div
        v-for="i in actualRows"
        :key="`card-${i}`"
        class="skeleton-card"
      >
        <div class="skeleton-card__image skeleton-pulse" />
        <div class="skeleton-card__content">
          <div class="skeleton-card__title skeleton-pulse" />
          <div class="skeleton-card__text skeleton-pulse" />
          <div class="skeleton-card__text skeleton-card__text--short skeleton-pulse" />
        </div>
      </div>
    </template>

    <!-- 图表骨架屏 -->
    <template v-else-if="type === 'chart'">
      <div class="skeleton-chart">
        <div class="skeleton-chart__title skeleton-pulse" />
        <div class="skeleton-chart__legend">
          <div
            v-for="i in 3"
            :key="`legend-${i}`"
            class="skeleton-chart__legend-item skeleton-pulse"
          />
        </div>
        <div class="skeleton-chart__area">
          <div class="skeleton-chart__bars">
            <div
              v-for="i in 6"
              :key="`bar-${i}`"
              class="skeleton-chart__bar skeleton-pulse"
              :style="{
                height: `${30 + Math.random() * 50}%`,
                animationDelay: `${i * 100}ms`
              }"
            />
          </div>
          <div class="skeleton-chart__axis skeleton-pulse" />
        </div>
      </div>
    </template>

    <!-- 表单骨架屏 -->
    <template v-else-if="type === 'form'">
      <div class="skeleton-form">
        <div
          v-for="i in actualRows"
          :key="`field-${i}`"
          class="skeleton-form__field"
        >
          <div class="skeleton-form__label skeleton-pulse" />
          <div class="skeleton-form__input skeleton-pulse" />
        </div>
        <div class="skeleton-form__actions">
          <div class="skeleton-form__button skeleton-pulse" />
          <div class="skeleton-form__button skeleton-form__button--secondary skeleton-pulse" />
        </div>
      </div>
    </template>

    <!-- 列表骨架屏 -->
    <template v-else-if="type === 'list'">
      <div class="skeleton-list">
        <div
          v-for="i in actualRows"
          :key="`list-${i}`"
          class="skeleton-list__item"
          :style="{ animationDelay: `${i * 80}ms` }"
        >
          <div class="skeleton-list__avatar skeleton-pulse" />
          <div class="skeleton-list__content">
            <div class="skeleton-list__title skeleton-pulse" />
            <div class="skeleton-list__desc skeleton-pulse" />
          </div>
          <div class="skeleton-list__action skeleton-pulse" />
        </div>
      </div>
    </template>

    <!-- 仪表盘骨架屏 -->
    <template v-else-if="type === 'dashboard'">
      <div class="skeleton-dashboard">
        <!-- KPI 指标行 -->
        <div class="skeleton-dashboard__kpi-row">
          <div
            v-for="i in 4"
            :key="`kpi-${i}`"
            class="skeleton-dashboard__kpi-card"
          >
            <div class="skeleton-dashboard__kpi-label skeleton-pulse" />
            <div class="skeleton-dashboard__kpi-value skeleton-pulse" />
            <div class="skeleton-dashboard__kpi-trend skeleton-pulse" />
          </div>
        </div>
        <!-- 图表区域 -->
        <div class="skeleton-dashboard__charts">
          <div class="skeleton-dashboard__chart-large">
            <div class="skeleton-dashboard__chart-title skeleton-pulse" />
            <div class="skeleton-dashboard__chart-body skeleton-pulse" />
          </div>
          <div class="skeleton-dashboard__chart-small">
            <div class="skeleton-dashboard__chart-title skeleton-pulse" />
            <div class="skeleton-dashboard__chart-body skeleton-pulse" />
          </div>
        </div>
      </div>
    </template>

    <!-- 文本骨架屏 -->
    <template v-else-if="type === 'text'">
      <div class="skeleton-text">
        <div class="skeleton-text__heading skeleton-pulse" />
        <div
          v-for="i in actualRows"
          :key="`text-${i}`"
          class="skeleton-text__line skeleton-pulse"
          :style="{
            width: i === actualRows ? '60%' : '100%',
            animationDelay: `${i * 60}ms`
          }"
        />
      </div>
    </template>

    <!-- 屏幕阅读器提示 -->
    <span class="sr-only">{{ loadingText }}</span>
  </div>
</template>

<script setup lang="ts">
/**
 * SkeletonLoader - 骨架屏加载器
 * 
 * 支持多种类型的骨架屏：表格、卡片、图表、表单、列表、仪表盘、文本
 * 
 * Validates: Requirements 1.5, 15.3
 * WHEN 数据正在加载时，THE DataTeaCup SHALL 显示加载动画或骨架屏占位符
 * WHEN 数据加载中时，THE DataTeaCup SHALL 显示骨架屏占位符
 */
import { computed, type CSSProperties } from 'vue'
import { themeConfig } from '@/config/theme'

// Props 定义
export interface SkeletonLoaderProps {
  /** 骨架屏类型 */
  type: 'table' | 'card' | 'chart' | 'form' | 'list' | 'dashboard' | 'text'
  /** 行数（表格行数、卡片数量、表单字段数、列表项数、文本行数） */
  rows?: number
  /** 表格列数 */
  columns?: number
  /** 加载提示文本 */
  loadingText?: string
  /** 是否启用动画 */
  animated?: boolean
}

const props = withDefaults(defineProps<SkeletonLoaderProps>(), {
  rows: 5,
  columns: 4,
  loadingText: '正在加载...',
  animated: true
})

// 从主题配置获取动画参数
const loadingConfig = themeConfig.animations.loading

// 计算实际行数
const actualRows = computed(() => Math.max(1, props.rows))

// ARIA 标签
const ariaLabel = computed(() => {
  const typeLabels: Record<string, string> = {
    table: '表格数据',
    card: '卡片内容',
    chart: '图表数据',
    form: '表单内容',
    list: '列表内容',
    dashboard: '仪表盘',
    text: '文本内容'
  }
  return `${typeLabels[props.type] || '内容'}正在加载`
})

// 加载器样式
const loaderStyle = computed<CSSProperties>(() => ({
  '--skeleton-duration': loadingConfig.duration,
  '--skeleton-easing': loadingConfig.easing
} as CSSProperties))
</script>

<style scoped>
.skeleton-loader {
  width: 100%;
}

/* 骨架屏脉冲动画 */
.skeleton-pulse {
  background: linear-gradient(
    90deg,
    v-bind('themeConfig.colors.background.tertiary') 25%,
    v-bind('themeConfig.colors.background.secondary') 50%,
    v-bind('themeConfig.colors.background.tertiary') 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse var(--skeleton-duration, 1500ms) var(--skeleton-easing, linear) infinite;
  border-radius: v-bind('themeConfig.borderRadius.sm');
}

@keyframes skeleton-pulse {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 表格骨架屏 */
.skeleton-table {
  width: 100%;
  border: 1px solid v-bind('themeConfig.colors.border.light');
  border-radius: v-bind('themeConfig.borderRadius.md');
  overflow: hidden;
}

.skeleton-table__header {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background-color: v-bind('themeConfig.colors.background.secondary');
  border-bottom: 1px solid v-bind('themeConfig.colors.border.light');
}

.skeleton-table__header-cell {
  flex: 1;
  height: 20px;
}

.skeleton-table__row {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid v-bind('themeConfig.colors.border.light');
}

.skeleton-table__row:last-child {
  border-bottom: none;
}

.skeleton-table__cell {
  flex: 1;
  height: 16px;
}

/* 卡片骨架屏 */
.skeleton-loader--card {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.skeleton-card {
  background-color: v-bind('themeConfig.colors.background.primary');
  border: 1px solid v-bind('themeConfig.colors.border.light');
  border-radius: v-bind('themeConfig.borderRadius.lg');
  overflow: hidden;
  box-shadow: v-bind('themeConfig.shadows.sm');
}

.skeleton-card__image {
  width: 100%;
  height: 160px;
}

.skeleton-card__content {
  padding: 16px;
}

.skeleton-card__title {
  height: 24px;
  margin-bottom: 12px;
  width: 70%;
}

.skeleton-card__text {
  height: 14px;
  margin-bottom: 8px;
}

.skeleton-card__text--short {
  width: 50%;
}

/* 图表骨架屏 */
.skeleton-chart {
  background-color: v-bind('themeConfig.colors.background.primary');
  border: 1px solid v-bind('themeConfig.colors.border.light');
  border-radius: v-bind('themeConfig.borderRadius.lg');
  padding: 20px;
  box-shadow: v-bind('themeConfig.shadows.sm');
}

.skeleton-chart__title {
  height: 24px;
  width: 40%;
  margin-bottom: 16px;
}

.skeleton-chart__legend {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.skeleton-chart__legend-item {
  height: 16px;
  width: 80px;
}

.skeleton-chart__area {
  position: relative;
  height: 200px;
  display: flex;
  flex-direction: column;
}

.skeleton-chart__bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding-bottom: 8px;
}

.skeleton-chart__bar {
  flex: 1;
  min-height: 20px;
  border-radius: v-bind('themeConfig.borderRadius.sm') v-bind('themeConfig.borderRadius.sm') 0 0;
}

.skeleton-chart__axis {
  height: 2px;
  width: 100%;
}

/* 表单骨架屏 */
.skeleton-form {
  max-width: 600px;
}

.skeleton-form__field {
  margin-bottom: 20px;
}

.skeleton-form__label {
  height: 16px;
  width: 120px;
  margin-bottom: 8px;
}

.skeleton-form__input {
  height: 40px;
  width: 100%;
}

.skeleton-form__actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.skeleton-form__button {
  height: 40px;
  width: 100px;
  border-radius: v-bind('themeConfig.borderRadius.md');
}

.skeleton-form__button--secondary {
  width: 80px;
}

/* 列表骨架屏 */
.skeleton-list {
  width: 100%;
}

.skeleton-list__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid v-bind('themeConfig.colors.border.light');
}

.skeleton-list__item:last-child {
  border-bottom: none;
}

.skeleton-list__avatar {
  width: 40px;
  height: 40px;
  border-radius: v-bind('themeConfig.borderRadius.full');
  flex-shrink: 0;
}

.skeleton-list__content {
  flex: 1;
  min-width: 0;
}

.skeleton-list__title {
  height: 16px;
  width: 40%;
  margin-bottom: 8px;
}

.skeleton-list__desc {
  height: 14px;
  width: 70%;
}

.skeleton-list__action {
  width: 60px;
  height: 32px;
  flex-shrink: 0;
  border-radius: v-bind('themeConfig.borderRadius.md');
}

/* 仪表盘骨架屏 */
.skeleton-dashboard {
  width: 100%;
}

.skeleton-dashboard__kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.skeleton-dashboard__kpi-card {
  background-color: v-bind('themeConfig.colors.background.primary');
  border: 1px solid v-bind('themeConfig.colors.border.light');
  border-radius: v-bind('themeConfig.borderRadius.lg');
  padding: 16px;
  box-shadow: v-bind('themeConfig.shadows.sm');
}

.skeleton-dashboard__kpi-label {
  height: 14px;
  width: 60%;
  margin-bottom: 12px;
}

.skeleton-dashboard__kpi-value {
  height: 28px;
  width: 50%;
  margin-bottom: 8px;
}

.skeleton-dashboard__kpi-trend {
  height: 14px;
  width: 40%;
}

.skeleton-dashboard__charts {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.skeleton-dashboard__chart-large,
.skeleton-dashboard__chart-small {
  background-color: v-bind('themeConfig.colors.background.primary');
  border: 1px solid v-bind('themeConfig.colors.border.light');
  border-radius: v-bind('themeConfig.borderRadius.lg');
  padding: 16px;
  box-shadow: v-bind('themeConfig.shadows.sm');
}

.skeleton-dashboard__chart-title {
  height: 20px;
  width: 30%;
  margin-bottom: 16px;
}

.skeleton-dashboard__chart-body {
  height: 200px;
  border-radius: v-bind('themeConfig.borderRadius.md');
}

/* 文本骨架屏 */
.skeleton-text {
  width: 100%;
}

.skeleton-text__heading {
  height: 24px;
  width: 50%;
  margin-bottom: 16px;
}

.skeleton-text__line {
  height: 14px;
  margin-bottom: 10px;
}

.skeleton-text__line:last-child {
  margin-bottom: 0;
}

/* 屏幕阅读器专用 */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
