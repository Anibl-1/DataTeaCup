import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api/request'

/**
 * AI 配置共享 Store
 * AiAssistant.vue 和 AiChat.vue 共用此 store，确保模型配置统一
 */
export const useAiConfigStore = defineStore('aiConfig', () => {
  // ========== AI 状态 ==========
  const aiStatus = ref({
    enabled: false,
    provider: '',
    model: '',
    apiKeyConfigured: false,
    features: {
      sqlGeneration: true,
      dataAnalysis: true,
      codeOptimization: true
    }
  })

  // ========== 使用量统计 ==========
  const usageStats = ref({
    todayCalls: 0,
    todayTokens: 0,
    dailyLimit: 0,
    remaining: 0
  })

  // ========== 提供商名称映射 ==========
  const providerNames: Record<string, string> = {
    openai: 'OpenAI',
    qwen: '通义千问',
    deepseek: 'DeepSeek',
    ollama: 'Ollama',
    'deepseek-local': 'DeepSeek (本地)',
    deepseekLocal: 'DeepSeek (本地)',
    claude: 'Claude',
    gemini: 'Gemini',
    zhipu: '智谱 GLM',
    wenxin: '百度文心',
    spark: '讯飞星火'
  }

  // ========== 提供商选项（统一列表） ==========
  const providerOptions = [
    { label: 'OpenAI', value: 'openai', icon: '🤖' },
    { label: 'DeepSeek', value: 'deepseek', icon: '🔮' },
    { label: '通义千问', value: 'qwen', icon: '🧠' },
    { label: 'Claude', value: 'claude', icon: '�' },
    { label: 'Gemini', value: 'gemini', icon: '💎' },
    { label: '智谱 GLM', value: 'zhipu', icon: '📚' },
    { label: 'Ollama (本地)', value: 'ollama', icon: '�' },
    { label: 'DeepSeek (本地)', value: 'deepseek-local', icon: '🏠' },
    { label: '百度文心', value: 'wenxin', icon: '🔴' },
    { label: '讯飞星火', value: 'spark', icon: '✨' }
  ]

  // ========== 模型选项（按提供商） ==========
  const modelOptionsMap: Record<string, { label: string; value: string }[]> = {
    openai: [
      { label: 'GPT-4.1 (最新推荐)', value: 'gpt-4.1' },
      { label: 'GPT-4.1 Mini', value: 'gpt-4.1-mini' },
      { label: 'GPT-4.1 Nano', value: 'gpt-4.1-nano' },
      { label: 'GPT-4o', value: 'gpt-4o' },
      { label: 'GPT-4o Mini', value: 'gpt-4o-mini' },
      { label: 'o3 (深度推理)', value: 'o3' },
      { label: 'o3-mini', value: 'o3-mini' },
      { label: 'o4-mini (推理)', value: 'o4-mini' },
      { label: 'GPT-4 Turbo', value: 'gpt-4-turbo' }
    ],
    deepseek: [
      { label: 'DeepSeek V4 Flash (最新推荐)', value: 'deepseek-v4-flash' },
      { label: 'DeepSeek V4 Pro (深度推理)', value: 'deepseek-v4-pro' }
    ],
    qwen: [
      { label: 'Qwen-Max (最强)', value: 'qwen-max' },
      { label: 'Qwen-Plus', value: 'qwen-plus' },
      { label: 'Qwen-Turbo (快速)', value: 'qwen-turbo' },
      { label: 'Qwen-Long (长文本)', value: 'qwen-long' },
      { label: 'Qwen2.5-Coder', value: 'qwen2.5-coder-32b-instruct' }
    ],
    claude: [
      { label: 'Claude Sonnet 4 (最新推荐)', value: 'claude-sonnet-4-20250514' },
      { label: 'Claude 3.5 Sonnet', value: 'claude-3-5-sonnet-20241022' },
      { label: 'Claude 3.5 Haiku', value: 'claude-3-5-haiku-20241022' },
      { label: 'Claude 3 Opus', value: 'claude-3-opus-20240229' }
    ],
    gemini: [
      { label: 'Gemini 2.5 Flash (最新推荐)', value: 'gemini-2.5-flash' },
      { label: 'Gemini 2.5 Pro', value: 'gemini-2.5-pro' },
      { label: 'Gemini 2.0 Flash', value: 'gemini-2.0-flash' },
      { label: 'Gemini 1.5 Pro', value: 'gemini-1.5-pro' }
    ],
    zhipu: [
      { label: 'GLM-4-Plus (推荐)', value: 'glm-4-plus' },
      { label: 'GLM-4', value: 'glm-4' },
      { label: 'GLM-4-Flash', value: 'glm-4-flash' },
      { label: 'GLM-4-Long', value: 'glm-4-long' }
    ],
    ollama: [
      { label: 'Llama 3.2 (最新)', value: 'llama3.2' },
      { label: 'Llama 3.1', value: 'llama3.1' },
      { label: 'Qwen 2.5', value: 'qwen2.5' },
      { label: 'DeepSeek-R1 (推理)', value: 'deepseek-r1' },
      { label: 'DeepSeek-Coder-V2', value: 'deepseek-coder-v2' },
      { label: 'Mistral', value: 'mistral' },
      { label: 'CodeLlama', value: 'codellama' }
    ],
    'deepseek-local': [
      { label: 'DeepSeek V4 Flash', value: 'deepseek-v4-flash' },
      { label: 'DeepSeek V4 Pro', value: 'deepseek-v4-pro' },
      { label: '自定义模型', value: 'custom' }
    ],
    deepseekLocal: [
      { label: 'DeepSeek V4 Flash', value: 'deepseek-v4-flash' },
      { label: 'DeepSeek V4 Pro', value: 'deepseek-v4-pro' },
      { label: '自定义模型', value: 'custom' }
    ],
    wenxin: [
      { label: 'ERNIE 4.0', value: 'ernie-4.0' },
      { label: 'ERNIE 3.5', value: 'ernie-3.5' },
      { label: 'ERNIE Speed', value: 'ernie-speed' }
    ],
    spark: [
      { label: 'Spark 4.0 Ultra', value: 'spark-4.0-ultra' },
      { label: 'Spark 3.5 Max', value: 'spark-3.5-max' },
      { label: 'Spark Pro', value: 'spark-pro' }
    ]
  }

  // ========== 提供商默认值 ==========
  const providerDefaults: Record<string, { url: string; model: string; needsApiKey: boolean }> = {
    openai: { url: 'https://api.openai.com/v1', model: 'gpt-4.1', needsApiKey: true },
    deepseek: { url: 'https://api.deepseek.com', model: 'deepseek-v4-flash', needsApiKey: true },
    qwen: { url: '', model: 'qwen-max', needsApiKey: true },
    claude: { url: 'https://api.anthropic.com', model: 'claude-sonnet-4-20250514', needsApiKey: true },
    gemini: { url: 'https://generativelanguage.googleapis.com', model: 'gemini-2.5-flash', needsApiKey: true },
    zhipu: { url: 'https://open.bigmodel.cn/api/paas/v4', model: 'glm-4-plus', needsApiKey: true },
    ollama: { url: 'http://localhost:11434', model: 'llama3.2', needsApiKey: false },
    'deepseek-local': { url: 'http://localhost:8080/v1', model: 'deepseek-v4-flash', needsApiKey: false },
    deepseekLocal: { url: 'http://localhost:8080/v1', model: 'deepseek-v4-flash', needsApiKey: false },
    wenxin: { url: '', model: 'ernie-4.0', needsApiKey: true },
    spark: { url: '', model: 'spark-4.0-ultra', needsApiKey: true }
  }

  // ========== Computed ==========
  const displayLabel = computed(() => {
    if (!aiStatus.value.enabled) return '未配置'
    const p = aiStatus.value.provider
    const pLabel = providerNames[p] || p || ''
    const mLabel = aiStatus.value.model || ''
    if (pLabel && mLabel) return `${pLabel} / ${mLabel}`
    return pLabel || '已配置'
  })

  const isEnabled = computed(() => aiStatus.value.enabled)

  // ========== Actions ==========

  /** 获取 AI 状态 */
  async function refreshStatus() {
    try {
      const res = await request.get('/ai/status')
      if (res.code === 200 && res.data) {
        aiStatus.value = { ...aiStatus.value, ...res.data }
      }
    } catch (e) {
      console.error('获取AI状态失败', e)
    }
  }

  /** 获取使用量统计 */
  async function refreshUsageStats() {
    try {
      const res = await request.get('/ai/usage/stats')
      if (res.data) {
        usageStats.value = res.data
      }
    } catch {
      // 静默处理
    }
  }

  /** 保存配置 */
  async function saveConfig(config: {
    provider: string
    apiKey: string
    baseUrl: string
    model: string
    temperature?: number
    maxTokens?: number
    maxHistory?: number
  }) {
    const res = await request.post('/ai/config', config)
    if (res.code === 200) {
      await refreshStatus()
      await refreshUsageStats()
      return true
    }
    throw new Error(res.message || '保存失败')
  }

  /** 测试连接 */
  async function testConnection(config: {
    provider: string
    apiKey: string
    baseUrl: string
    model: string
  }) {
    const res = await request.post('/ai/test-connection', config)
    if (res.code === 200 && res.data?.success) {
      return { success: true, message: res.data.message || '' }
    }
    return { success: false, error: res.data?.error || res.message || '连接失败' }
  }

  /** 获取模型选项 */
  function getModelOptions(provider: string) {
    return modelOptionsMap[provider] || []
  }

  /** 获取提供商默认值 */
  function getProviderDefaults(provider: string) {
    return providerDefaults[provider] || { url: '', model: '', needsApiKey: true }
  }

  /** 初始化（同时加载状态和统计） */
  async function init() {
    await Promise.all([refreshStatus(), refreshUsageStats()])
  }

  // ========== 跨组件通信：AiAssistant <-> AiChat ==========

  /** 控制 AiChat 抽屉的显隐 */
  const showChatDrawer = ref(false)

  /** 待发送到 AiChat 的预填消息（由 AiAssistant 的功能结果触发） */
  const pendingChatMessage = ref('')

  /** 待发送到 AiChat 的上下文类型标签 */
  const pendingChatContext = ref('')

  /** 打开 AiChat 抽屉并可选地预填消息 */
  function openChatWithMessage(msg: string, context?: string) {
    pendingChatMessage.value = msg
    pendingChatContext.value = context || ''
    showChatDrawer.value = true
  }

  /** 消费预填消息（AiChat 侧调用） */
  function consumePendingMessage() {
    const msg = pendingChatMessage.value
    const ctx = pendingChatContext.value
    pendingChatMessage.value = ''
    pendingChatContext.value = ''
    return { message: msg, context: ctx }
  }

  /** 触发打开配置弹窗的信号（AiAssistant 可请求 AiChat 打开配置） */
  const requestOpenConfig = ref(false)

  function openConfig() {
    requestOpenConfig.value = true
    showChatDrawer.value = true
  }

  /** 消费配置打开请求 */
  function consumeConfigRequest() {
    const val = requestOpenConfig.value
    requestOpenConfig.value = false
    return val
  }

  return {
    // 状态
    aiStatus,
    usageStats,
    providerNames,
    providerOptions,
    displayLabel,
    isEnabled,
    // Actions
    refreshStatus,
    refreshUsageStats,
    saveConfig,
    testConnection,
    getModelOptions,
    getProviderDefaults,
    init,
    // 跨组件通信
    showChatDrawer,
    pendingChatMessage,
    pendingChatContext,
    openChatWithMessage,
    consumePendingMessage,
    requestOpenConfig,
    openConfig,
    consumeConfigRequest
  }
})
