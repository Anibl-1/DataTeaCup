import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import { setupDirectives } from './directives'
import { ErrorReporterPlugin } from './utils/errorReporter'
import { i18n } from './i18n'
import './style.css'
import './styles/high-contrast.css'
import './styles/commercial.css'
import './styles/dark-mode.css'
import './styles/system-pages.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(i18n)
// 注册全局指令
setupDirectives(app)

// 安装错误上报器
// 需求 18.3: THE Error_Handler SHALL 自动收集前端错误并上报到服务端
app.use(ErrorReporterPlugin, {
  reportEndpoint: '/api/error/report',
  enabled: import.meta.env.PROD, // 仅在生产环境启用
  sampleRate: 1,
  captureGlobalErrors: true,
  captureUnhandledRejections: true,
  captureResourceErrors: true,
  captureVueErrors: true
})

app.mount('#app')

