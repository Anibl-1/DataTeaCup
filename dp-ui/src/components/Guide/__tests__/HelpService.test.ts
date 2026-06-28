/**
 * HelpService 单元测试
 * HelpService Unit Tests
 * 
 * 需求 19.3: THE DataTeaCup SHALL 提供内置的帮助文档系统
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { createHelpService, HELP_CATEGORIES } from '../HelpService'
import type { HelpArticle } from '../helpTypes'

describe('HelpService', () => {
  let service: ReturnType<typeof createHelpService>

  beforeEach(() => {
    service = createHelpService()
  })

  describe('getCategories', () => {
    it('should return all help categories sorted by order', () => {
      const categories = service.getCategories()
      
      expect(categories.length).toBe(HELP_CATEGORIES.length)
      
      // Verify sorted by order
      for (let i = 1; i < categories.length; i++) {
        expect(categories[i].order).toBeGreaterThanOrEqual(categories[i - 1].order)
      }
    })

    it('should include all expected category IDs', () => {
      const categories = service.getCategories()
      const categoryIds = categories.map(c => c.id)
      
      expect(categoryIds).toContain('getting-started')
      expect(categoryIds).toContain('report-design')
      expect(categoryIds).toContain('chart-design')
      expect(categoryIds).toContain('data-source')
      expect(categoryIds).toContain('faq')
    })
  })

  describe('registerArticle', () => {
    it('should register a valid article', () => {
      const article: HelpArticle = {
        id: 'test-article',
        title: 'Test Article',
        summary: 'Test summary',
        content: 'Test content',
        category: 'getting-started',
        keywords: ['test', 'article']
      }

      service.registerArticle(article)
      
      const retrieved = service.getArticle('test-article')
      expect(retrieved).toEqual(article)
    })

    it('should not register article with missing required fields', () => {
      const invalidArticle = {
        id: '',
        title: 'Test',
        summary: 'Test',
        content: 'Test',
        category: 'getting-started',
        keywords: []
      } as HelpArticle

      service.registerArticle(invalidArticle)
      
      expect(service.getArticle('')).toBeUndefined()
    })

    it('should index article by category', () => {
      const article: HelpArticle = {
        id: 'cat-test',
        title: 'Category Test',
        summary: 'Test',
        content: 'Test',
        category: 'report-design',
        keywords: ['test']
      }

      service.registerArticle(article)
      
      const articles = service.getArticlesByCategory('report-design')
      expect(articles.some(a => a.id === 'cat-test')).toBe(true)
    })

    it('should index article by related features', () => {
      const article: HelpArticle = {
        id: 'feature-test',
        title: 'Feature Test',
        summary: 'Test',
        content: 'Test',
        category: 'getting-started',
        keywords: ['test'],
        relatedFeatures: ['/dashboard', '/reports']
      }

      service.registerArticle(article)
      
      const dashboardHelp = service.getContextualHelp('/dashboard')
      expect(dashboardHelp.some(a => a.id === 'feature-test')).toBe(true)
      
      const reportsHelp = service.getContextualHelp('/reports')
      expect(reportsHelp.some(a => a.id === 'feature-test')).toBe(true)
    })
  })

  describe('registerArticles', () => {
    it('should register multiple articles at once', () => {
      const articles: HelpArticle[] = [
        {
          id: 'batch-1',
          title: 'Batch 1',
          summary: 'Test',
          content: 'Test',
          category: 'getting-started',
          keywords: ['batch']
        },
        {
          id: 'batch-2',
          title: 'Batch 2',
          summary: 'Test',
          content: 'Test',
          category: 'faq',
          keywords: ['batch']
        }
      ]

      service.registerArticles(articles)
      
      expect(service.getArticle('batch-1')).toBeDefined()
      expect(service.getArticle('batch-2')).toBeDefined()
    })
  })

  describe('getArticlesByCategory', () => {
    beforeEach(() => {
      service.registerArticles([
        {
          id: 'gs-1',
          title: 'Getting Started 1',
          summary: 'Test',
          content: 'Test',
          category: 'getting-started',
          keywords: ['start'],
          order: 2
        },
        {
          id: 'gs-2',
          title: 'Getting Started 2',
          summary: 'Test',
          content: 'Test',
          category: 'getting-started',
          keywords: ['start'],
          order: 1
        },
        {
          id: 'faq-1',
          title: 'FAQ 1',
          summary: 'Test',
          content: 'Test',
          category: 'faq',
          keywords: ['faq']
        }
      ])
    })

    it('should return articles for the specified category', () => {
      const articles = service.getArticlesByCategory('getting-started')
      
      expect(articles.length).toBe(2)
      expect(articles.every(a => a.category === 'getting-started')).toBe(true)
    })

    it('should return articles sorted by order', () => {
      const articles = service.getArticlesByCategory('getting-started')
      
      expect(articles[0].id).toBe('gs-2') // order: 1
      expect(articles[1].id).toBe('gs-1') // order: 2
    })

    it('should return empty array for category with no articles', () => {
      const articles = service.getArticlesByCategory('performance')
      
      expect(articles).toEqual([])
    })
  })

  describe('getPopularArticles', () => {
    beforeEach(() => {
      service.registerArticles([
        {
          id: 'popular-1',
          title: 'Popular 1',
          summary: 'Test',
          content: 'Test',
          category: 'getting-started',
          keywords: ['popular'],
          isPopular: true
        },
        {
          id: 'not-popular',
          title: 'Not Popular',
          summary: 'Test',
          content: 'Test',
          category: 'getting-started',
          keywords: ['test'],
          isPopular: false
        },
        {
          id: 'popular-2',
          title: 'Popular 2',
          summary: 'Test',
          content: 'Test',
          category: 'faq',
          keywords: ['popular'],
          isPopular: true
        }
      ])
    })

    it('should return only popular articles', () => {
      const popular = service.getPopularArticles()
      
      expect(popular.length).toBe(2)
      expect(popular.every(a => a.isPopular)).toBe(true)
    })
  })

  describe('search', () => {
    beforeEach(() => {
      service.registerArticles([
        {
          id: 'search-1',
          title: 'Report Design Guide',
          summary: 'Learn how to design reports',
          content: 'This guide covers report design basics',
          category: 'report-design',
          keywords: ['report', 'design', 'guide']
        },
        {
          id: 'search-2',
          title: 'Chart Configuration',
          summary: 'Configure chart settings',
          content: 'Chart configuration options',
          category: 'chart-design',
          keywords: ['chart', 'config']
        },
        {
          id: 'search-3',
          title: 'SQL Query Tips',
          summary: 'Tips for writing SQL queries',
          content: 'SQL query optimization tips',
          category: 'query-builder',
          keywords: ['sql', 'query', 'tips']
        }
      ])
    })

    it('should return empty array for empty query', () => {
      const results = service.search('')
      expect(results).toEqual([])
    })

    it('should find articles by keyword match', () => {
      const results = service.search('report')
      
      expect(results.length).toBeGreaterThan(0)
      expect(results[0].article.id).toBe('search-1')
    })

    it('should find articles by title match', () => {
      const results = service.search('Chart Configuration')
      
      expect(results.length).toBeGreaterThan(0)
      expect(results.some(r => r.article.id === 'search-2')).toBe(true)
    })

    it('should find articles by content match', () => {
      const results = service.search('optimization')
      
      expect(results.length).toBeGreaterThan(0)
      expect(results.some(r => r.article.id === 'search-3')).toBe(true)
    })

    it('should rank keyword matches higher than content matches', () => {
      const results = service.search('query')
      
      // 'query' is a keyword for search-3, should rank higher
      expect(results[0].article.id).toBe('search-3')
    })

    it('should include highlighted title and summary', () => {
      const results = service.search('report')
      
      expect(results[0].highlightedTitle).toContain('<mark>')
      expect(results[0].highlightedSummary).toContain('<mark>')
    })
  })

  describe('state management', () => {
    beforeEach(() => {
      service.registerArticle({
        id: 'state-test',
        title: 'State Test',
        summary: 'Test',
        content: 'Test content',
        category: 'getting-started',
        keywords: ['test']
      })
    })

    it('should open and close help center', () => {
      expect(service.getState().isOpen).toBe(false)
      
      service.open()
      expect(service.getState().isOpen).toBe(true)
      
      service.close()
      expect(service.getState().isOpen).toBe(false)
    })

    it('should open with specific category', () => {
      service.open('report-design')
      
      const state = service.getState()
      expect(state.isOpen).toBe(true)
      expect(state.selectedCategory).toBe('report-design')
    })

    it('should toggle help center', () => {
      service.toggle()
      expect(service.getState().isOpen).toBe(true)
      
      service.toggle()
      expect(service.getState().isOpen).toBe(false)
    })

    it('should select and deselect category', () => {
      service.selectCategory('faq')
      expect(service.getState().selectedCategory).toBe('faq')
      
      service.selectCategory(null)
      expect(service.getState().selectedCategory).toBeNull()
    })

    it('should view article and update state', () => {
      service.viewArticle('state-test')
      
      const state = service.getState()
      expect(state.currentArticle?.id).toBe('state-test')
      expect(state.selectedCategory).toBe('getting-started')
    })

    it('should track article view history', () => {
      service.viewArticle('state-test')
      
      const history = service.getHistory()
      expect(history.length).toBe(1)
      expect(history[0].id).toBe('state-test')
    })

    it('should back to list from article', () => {
      service.viewArticle('state-test')
      expect(service.getState().currentArticle).not.toBeNull()
      
      service.backToList()
      expect(service.getState().currentArticle).toBeNull()
    })

    it('should set and clear search query', () => {
      service.setSearchQuery('test')
      
      let state = service.getState()
      expect(state.searchQuery).toBe('test')
      expect(state.searchResults.length).toBeGreaterThan(0)
      
      service.clearSearch()
      
      state = service.getState()
      expect(state.searchQuery).toBe('')
      expect(state.searchResults).toEqual([])
    })

    it('should clear history', () => {
      service.viewArticle('state-test')
      expect(service.getHistory().length).toBe(1)
      
      service.clearHistory()
      expect(service.getHistory().length).toBe(0)
    })
  })

  describe('getContextualHelp', () => {
    beforeEach(() => {
      service.registerArticles([
        {
          id: 'ctx-1',
          title: 'Dashboard Help',
          summary: 'Help for dashboard',
          content: 'Dashboard content',
          category: 'getting-started',
          keywords: ['dashboard'],
          relatedFeatures: ['/dashboard']
        },
        {
          id: 'ctx-2',
          title: 'Report Help',
          summary: 'Help for reports',
          content: 'Report content',
          category: 'report-design',
          keywords: ['report'],
          relatedFeatures: ['/reports', '/report-designer']
        }
      ])
    })

    it('should return articles related to feature', () => {
      const help = service.getContextualHelp('/dashboard')
      
      expect(help.length).toBe(1)
      expect(help[0].id).toBe('ctx-1')
    })

    it('should return multiple articles for shared feature', () => {
      const help = service.getContextualHelp('/report-designer')
      
      expect(help.length).toBe(1)
      expect(help[0].id).toBe('ctx-2')
    })

    it('should return empty array for unknown feature', () => {
      const help = service.getContextualHelp('/unknown')
      
      expect(help).toEqual([])
    })
  })
})

