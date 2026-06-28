import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SkeletonLoader from '../SkeletonLoader.vue'

describe('SkeletonLoader', () => {
  const allTypes = ['table', 'card', 'chart', 'form', 'list', 'dashboard', 'text'] as const

  it.each(allTypes)('renders %s type without errors', (type) => {
    const wrapper = mount(SkeletonLoader, { props: { type } })
    expect(wrapper.find('.skeleton-loader').exists()).toBe(true)
    expect(wrapper.find(`.skeleton-loader--${type}`).exists()).toBe(true)
  })

  it('has correct ARIA attributes', () => {
    const wrapper = mount(SkeletonLoader, { props: { type: 'table' } })
    const root = wrapper.find('.skeleton-loader')
    expect(root.attributes('role')).toBe('status')
    expect(root.attributes('aria-busy')).toBe('true')
    expect(root.attributes('aria-label')).toContain('正在加载')
  })

  it.each([
    ['table', '表格数据正在加载'],
    ['card', '卡片内容正在加载'],
    ['chart', '图表数据正在加载'],
    ['form', '表单内容正在加载'],
    ['list', '列表内容正在加载'],
    ['dashboard', '仪表盘正在加载'],
    ['text', '文本内容正在加载'],
  ] as const)('type=%s has aria-label "%s"', (type, expectedLabel) => {
    const wrapper = mount(SkeletonLoader, { props: { type } })
    expect(wrapper.find('.skeleton-loader').attributes('aria-label')).toBe(expectedLabel)
  })

  it('renders screen reader text', () => {
    const wrapper = mount(SkeletonLoader, { props: { type: 'list' } })
    expect(wrapper.find('.sr-only').text()).toBe('正在加载...')
  })

  it('accepts custom loadingText', () => {
    const wrapper = mount(SkeletonLoader, { props: { type: 'text', loadingText: 'Loading...' } })
    expect(wrapper.find('.sr-only').text()).toBe('Loading...')
  })

  // Table type
  describe('table type', () => {
    it('renders correct number of rows', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'table', rows: 3, columns: 4 } })
      expect(wrapper.findAll('.skeleton-table__row')).toHaveLength(3)
    })

    it('renders correct number of columns per row', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'table', rows: 2, columns: 5 } })
      const firstRow = wrapper.findAll('.skeleton-table__row')[0]
      expect(firstRow.findAll('.skeleton-table__cell')).toHaveLength(5)
    })

    it('renders header cells', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'table', columns: 3 } })
      expect(wrapper.findAll('.skeleton-table__header-cell')).toHaveLength(3)
    })
  })

  // Card type
  describe('card type', () => {
    it('renders correct number of cards', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'card', rows: 4 } })
      expect(wrapper.findAll('.skeleton-card')).toHaveLength(4)
    })
  })

  // Form type
  describe('form type', () => {
    it('renders correct number of fields', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'form', rows: 3 } })
      expect(wrapper.findAll('.skeleton-form__field')).toHaveLength(3)
    })

    it('renders action buttons', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'form' } })
      expect(wrapper.findAll('.skeleton-form__button')).toHaveLength(2)
    })
  })

  // List type
  describe('list type', () => {
    it('renders correct number of list items', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'list', rows: 6 } })
      expect(wrapper.findAll('.skeleton-list__item')).toHaveLength(6)
    })

    it('each item has avatar, content, and action', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'list', rows: 1 } })
      const item = wrapper.find('.skeleton-list__item')
      expect(item.find('.skeleton-list__avatar').exists()).toBe(true)
      expect(item.find('.skeleton-list__content').exists()).toBe(true)
      expect(item.find('.skeleton-list__action').exists()).toBe(true)
    })
  })

  // Dashboard type
  describe('dashboard type', () => {
    it('renders 4 KPI cards', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'dashboard' } })
      expect(wrapper.findAll('.skeleton-dashboard__kpi-card')).toHaveLength(4)
    })

    it('renders chart areas', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'dashboard' } })
      expect(wrapper.find('.skeleton-dashboard__chart-large').exists()).toBe(true)
      expect(wrapper.find('.skeleton-dashboard__chart-small').exists()).toBe(true)
    })
  })

  // Text type
  describe('text type', () => {
    it('renders heading and text lines', () => {
      const wrapper = mount(SkeletonLoader, { props: { type: 'text', rows: 4 } })
      expect(wrapper.find('.skeleton-text__heading').exists()).toBe(true)
      expect(wrapper.findAll('.skeleton-text__line')).toHaveLength(4)
    })
  })

  // rows minimum enforcement
  it('enforces minimum of 1 row', () => {
    const wrapper = mount(SkeletonLoader, { props: { type: 'list', rows: 0 } })
    expect(wrapper.findAll('.skeleton-list__item')).toHaveLength(1)
  })

  // Pulse animation class
  it('applies skeleton-pulse class to animated elements', () => {
    const wrapper = mount(SkeletonLoader, { props: { type: 'list', rows: 1 } })
    expect(wrapper.find('.skeleton-pulse').exists()).toBe(true)
  })
})
