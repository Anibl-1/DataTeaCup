import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTabsStore } from '../tabs'

describe('Tabs Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    // 清除 sessionStorage
    sessionStorage.clear()
  })

  it('should have default tab', () => {
    const store = useTabsStore()
    store.initTabs()
    expect(store.tabs).toHaveLength(1)
    expect(store.tabs[0]!.key).toBe('/dashboard')
    expect(store.activeTab).toBe('/dashboard')
  })

  it('should add new tab', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    expect(store.tabs).toHaveLength(2)
    expect(store.activeTab).toBe('/user')
  })

  it('should not duplicate tabs', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    expect(store.tabs).toHaveLength(2)
  })

  it('should close tab and return next path', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    store.addTab({ key: '/role', title: '角色管理', closable: true })
    
    // 先激活 /user 标签
    store.setActiveTab('/user')
    
    const nextPath = store.closeTab('/user')
    expect(store.tabs).toHaveLength(2)
    expect(nextPath).toBe('/role')
  })

  it('should not close unclosable tab', () => {
    const store = useTabsStore()
    store.initTabs()
    const result = store.closeTab('/dashboard')
    expect(result).toBeNull()
    expect(store.tabs).toHaveLength(1)
  })

  it('should close other tabs', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    store.addTab({ key: '/role', title: '角色管理', closable: true })
    
    store.closeOtherTabs('/user')
    expect(store.tabs).toHaveLength(2) // dashboard + user
    expect(store.tabs.find(t => t.key === '/role')).toBeUndefined()
  })

  it('should close all closable tabs', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    store.addTab({ key: '/role', title: '角色管理', closable: true })
    
    const path = store.closeAllTabs()
    expect(store.tabs).toHaveLength(1)
    expect(path).toBe('/dashboard')
  })

  it('should clear all tabs on logout', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    
    store.clearTabs()
    expect(store.tabs).toHaveLength(1)
    expect(store.tabs[0]!.key).toBe('/dashboard')
    expect(sessionStorage.getItem('tabs-views')).toBeNull()
  })

  it('should persist tabs to sessionStorage', () => {
    const store = useTabsStore()
    store.initTabs()
    store.addTab({ key: '/user', title: '用户管理', closable: true })
    
    const stored = sessionStorage.getItem('tabs-views')
    expect(stored).not.toBeNull()
    const parsed = JSON.parse(stored!)
    expect(parsed).toHaveLength(2)
  })
})
