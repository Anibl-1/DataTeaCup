/**
 * Vite 配置文件
 * 
 * 需求: 1.9, 1.10 - 路由懒加载和组件按需加载
 * - 配置代码分割策略
 * - 分离 vendor 库
 * - 按功能模块创建 chunks
 */
import { defineConfig, type PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import { visualizer } from 'rollup-plugin-visualizer'
import { ViteImageOptimizer } from 'vite-plugin-image-optimizer'
import { resolve } from 'path'
import http from 'http'

// Backend proxy target: 默认走网关(8888)，网关负责路由到各微服务
// VITE_BACKEND=direct → 直连单个微服务(仅调试单服务时用)
const isDirect = process.env.VITE_BACKEND === 'direct'
const backendTarget = isDirect
  ? 'http://localhost:9001'
  : 'http://localhost:8888'
// Gateway mode: /api prefix is kept (gateway StripPrefix handles it)
// Direct mode: /api prefix is rewritten away
const apiRewrite = isDirect
  ? (path: string) => path.replace(/^\/api/, '')
  : undefined
// Keep-alive agent fixes Netty Connection:close parse error
const proxyAgent = isDirect
  ? undefined
  : new http.Agent({ keepAlive: true, keepAliveMsecs: 30000 })

const analyzePlugins: PluginOption[] = process.env.ANALYZE
  ? [
      visualizer({
        open: true,
        filename: 'stats.html',
        gzipSize: true,
        brotliSize: true,
        template: 'treemap',
      }),
    ]
  : []

export default defineConfig({
  plugins: [
    vue(),
    ViteImageOptimizer({
      png: { quality: 80 },
      jpeg: { quality: 80 },
      svg: { multipass: true },
    }),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [NaiveUiResolver()]
    }),
    Components({
      resolvers: [NaiveUiResolver()]
    }),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['logo.svg'],
      manifest: {
        name: 'DataTeaCup',
        short_name: 'DataTeaCup',
        description: '开源数据集成与智能 BI 平台',
        theme_color: '#18a058',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: 'logo.svg', sizes: '192x192', type: 'image/svg+xml' },
          { src: 'logo.svg', sizes: '512x512', type: 'image/svg+xml', purpose: 'any maskable' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/.*\/api\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: { maxEntries: 100, maxAgeSeconds: 60 * 60 },
              networkTimeoutSeconds: 5
            }
          },
          {
            urlPattern: /\.(png|jpg|jpeg|svg|gif|webp)$/,
            handler: 'CacheFirst',
            options: {
              cacheName: 'image-cache',
              expiration: { maxEntries: 50, maxAgeSeconds: 7 * 24 * 60 * 60 }
            }
          }
        ]
      }
    }),
    ...analyzePlugins,
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  
  // 构建配置
  build: {
    // 启用 CSS 代码分割
    cssCodeSplit: true,
    
    // chunk 大小警告限制 (500KB)
    chunkSizeWarningLimit: 500,
    
    // Rollup 配置
    rollupOptions: {
      output: {
        // 手动分割 chunks - 将大型依赖分离以优化缓存和并行加载
        manualChunks(id) {
          // Vue 核心库
          if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/') || id.includes('node_modules/@vue/')) {
            return 'vue-vendor'
          }
          // 工具库
          if (id.includes('node_modules/axios/') || id.includes('node_modules/dayjs/')) {
            return 'utils-vendor'
          }
          // Naive UI 组件库
          if (id.includes('node_modules/naive-ui/')) {
            return 'naive-ui'
          }
          // ECharts 核心（core + renderers），扩展图表由动态 import 自动分 chunk
          if (id.includes('node_modules/echarts/')) {
            return 'echarts'
          }
          // Excel 处理
          if (id.includes('node_modules/xlsx/')) {
            return 'xlsx'
          }
        },
        
        // chunk 文件命名
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId
          
          // 根据模块路径确定 chunk 名称
          if (facadeModuleId) {
            // 视图组件
            if (facadeModuleId.includes('/views/')) {
              // 从路径中提取模块名
              const match = facadeModuleId.match(/\/views\/([^/]+)/)
              if (match) {
                const viewName = match[1].replace('.vue', '').toLowerCase()
                return `views/[name]-[hash].js`
              }
            }
            
            // 组件
            if (facadeModuleId.includes('/components/')) {
              return `components/[name]-[hash].js`
            }
          }
          
          return `js/[name]-[hash].js`
        },
        
        // 入口文件命名
        entryFileNames: 'js/[name]-[hash].js',
        
        // 静态资源命名
        assetFileNames: (assetInfo) => {
          const name = assetInfo.name || ''
          
          // CSS 文件
          if (name.endsWith('.css')) {
            return 'css/[name]-[hash][extname]'
          }
          
          // 图片文件
          if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(name)) {
            return 'images/[name]-[hash][extname]'
          }
          
          // 字体文件
          if (/\.(woff2?|eot|ttf|otf)$/i.test(name)) {
            return 'fonts/[name]-[hash][extname]'
          }
          
          return 'assets/[name]-[hash][extname]'
        }
      }
    },
    
    // 压缩配置 - 使用 esbuild (默认，无需额外依赖)
    minify: 'esbuild',
    
    // 生成 source map (生产环境可关闭)
    sourcemap: false,
    
    // 目标浏览器
    target: 'es2015'
  },
  
  // 生产构建移除 console 和 debugger（esbuild 配置需放在根级别）
  esbuild: {
    drop: ['debugger'],
    pure: ['console.log', 'console.debug', 'console.info'],
  },
  
  // 优化配置
  optimizeDeps: {
    // 预构建依赖（仅核心库，减少启动内存）
    include: [
      'vue',
      'vue-router',
      'pinia',
      'axios',
      'dayjs'
    ],
    // 排除大型库，按需加载以降低内存占用
    exclude: ['echarts', 'echarts-wordcloud', 'xlsx', 'jspdf', 'html2canvas']
  },
  
  // 开发服务器配置
  server: {
    port: 3000,
    // 预热常用文件，减少首次访问延迟
    warmup: {
      clientFiles: [
        './src/layouts/MainLayout.vue',
        './src/views/Dashboard.vue',
        './src/views/Login.vue'
      ]
    },
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
        ws: true,
        ...(apiRewrite ? { rewrite: apiRewrite } : {}),
        ...(proxyAgent ? { agent: proxyAgent } : {}),
        configure: (proxy) => {
          proxy.on('error', (err) => {
            console.error('代理错误:', err.message)
          })
        }
      },
      '/file': {
        target: backendTarget,
        changeOrigin: true,
        ...(proxyAgent ? { agent: proxyAgent } : {})
      }
    }
  },
  
  // 预览服务器配置
  preview: {
    port: 4173
  }
})
