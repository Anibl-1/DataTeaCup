<template>
  <div class="message-channel-page">
    <!-- Page_Header_Stats: 通道类型统计 (Req 11.1) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><MailOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ getTypeCount('email') }}</span>
          <span class="stat-label">邮件通道</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><LogoWechat /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ getTypeCount('wecom') }}</span>
          <span class="stat-label">企业微信</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="22"><MegaphoneOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ getTypeCount('dingtalk') }}</span>
          <span class="stat-label">钉钉通道</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="22"><PhonePortraitOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ getTypeCount('sms') }}</span>
          <span class="stat-label">短信通道</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 通道列表 (Req 1.1, 11.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><ChatbubblesOutline /></n-icon>
          <span>消息通道管理</span>
        </div>
      </template>
      <template #header-extra>
        <n-button type="primary" @click="handleAdd">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新建通道
        </n-button>
      </template>

      <!-- Query_Form: 搜索筛选 -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索通道名称"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="filterType"
            :options="filterTypeOptions"
            placeholder="通道类型"
            clearable
            style="width: 160px;"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="filterStatus"
            :options="filterStatusOptions"
            placeholder="状态"
            clearable
            style="width: 120px;"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">搜索</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="paginatedChannels"
        :loading="loading"
        :pagination="false"
        :scroll-x="900"
        striped
        class="custom-table"
      />

      <n-empty v-if="!loading && filteredChannels.length === 0" description="暂无消息通道配置" style="margin: 32px 0;" />

      <!-- Pagination_Wrapper -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ filteredChannels.length }} 条记录
          </n-tag>
        </div>
        <n-pagination
          v-model:page="currentPage"
          :page-size="pageSize"
          :item-count="filteredChannels.length"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          show-quick-jumper
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 新建/编辑弹窗 -->
    <n-modal v-model:show="showModal" :title="isEdit ? '编辑通道' : '新建通道'" preset="card" class="modal-md" style="border-radius: 16px;">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100px">
        <n-form-item label="通道名称" path="channelName">
          <n-input v-model:value="form.channelName" placeholder="请输入通道名称，如：主邮箱、钉钉群1" />
        </n-form-item>
        <n-form-item label="通道类型" path="channelType">
          <n-select
            v-model:value="form.channelType"
            :options="channelTypeOptions"
            :disabled="isEdit"
            placeholder="请选择通道类型"
          />
        </n-form-item>
        <n-form-item label="描述说明">
          <n-input v-model:value="form.description" type="textarea" :rows="2" placeholder="可选，描述该通道的用途" />
        </n-form-item>
        <n-form-item label="启用状态">
          <n-switch v-model:value="statusSwitch" />
        </n-form-item>
        <n-form-item label="设为默认">
          <n-switch v-model:value="defaultSwitch" />
          <span class="form-hint" style="margin-left: 8px;">同类型只能有一个默认配置</span>
        </n-form-item>

        <!-- 动态配置表单 -->
        <n-divider>{{ getConfigTitle }}</n-divider>

        <!-- 邮件配置 -->
        <template v-if="form.channelType === 'email'">
          <n-form-item label="SMTP服务器" path="configObj.host">
            <n-input v-model:value="configObj.host" placeholder="如：smtp.qq.com" />
          </n-form-item>
          <n-grid :cols="2" :x-gap="16">
            <n-grid-item>
              <n-form-item label="端口" path="configObj.port">
                <n-input-number v-model:value="configObj.port" :min="1" :max="65535" placeholder="465或587" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item>
              <n-form-item label="SSL加密">
                <n-switch v-model:value="configObj.ssl" />
              </n-form-item>
            </n-grid-item>
          </n-grid>
          <n-form-item label="用户名" path="configObj.username">
            <n-input v-model:value="configObj.username" placeholder="发件邮箱地址" />
          </n-form-item>
          <n-form-item label="密码/授权码" path="configObj.password">
            <n-input v-model:value="configObj.password" type="password" show-password-on="click" placeholder="邮箱密码或授权码" />
          </n-form-item>
          <n-form-item label="发件人名称">
            <n-input v-model:value="configObj.fromName" placeholder="可选，如：系统通知" />
          </n-form-item>
        </template>

        <!-- 企业微信配置 -->
        <template v-if="form.channelType === 'wecom'">
          <n-form-item label="消息类型" path="configObj.msgType">
            <n-radio-group v-model:value="configObj.msgType">
              <n-radio value="app">应用消息</n-radio>
              <n-radio value="webhook">群机器人</n-radio>
            </n-radio-group>
          </n-form-item>
          <template v-if="configObj.msgType === 'app'">
            <n-form-item label="企业ID" path="configObj.corpId">
              <n-input v-model:value="configObj.corpId" placeholder="企业微信后台 - 我的企业 - 企业ID" />
            </n-form-item>
            <n-form-item label="应用AgentId" path="configObj.agentId">
              <n-input v-model:value="configObj.agentId" placeholder="应用管理 - 自建应用 - AgentId" />
            </n-form-item>
            <n-form-item label="应用Secret" path="configObj.secret">
              <n-input v-model:value="configObj.secret" type="password" show-password-on="click" placeholder="应用管理 - 自建应用 - Secret" />
            </n-form-item>
            <n-form-item label="API域名">
              <n-input v-model:value="configObj.apiDomain" placeholder="可选，默认 https://qyapi.weixin.qq.com" />
              <n-text depth="3" style="margin-left: 8px; font-size: 12px;">内网部署可修改</n-text>
            </n-form-item>
          </template>
          <template v-if="configObj.msgType === 'webhook'">
            <n-form-item label="Webhook地址" path="configObj.webhookUrl">
              <n-input v-model:value="configObj.webhookUrl" placeholder="群机器人Webhook地址" />
            </n-form-item>
            <n-form-item label="消息格式">
              <n-select v-model:value="configObj.contentType" :options="wecomContentTypeOptions" placeholder="默认text" />
            </n-form-item>
          </template>
        </template>

        <!-- 钉钉配置 -->
        <template v-if="form.channelType === 'dingtalk'">
          <n-form-item label="消息类型" path="configObj.msgType">
            <n-radio-group v-model:value="configObj.msgType">
              <n-radio value="webhook">群机器人</n-radio>
              <n-radio value="app">工作通知</n-radio>
            </n-radio-group>
          </n-form-item>
          <template v-if="configObj.msgType === 'webhook'">
            <n-form-item label="Webhook地址" path="configObj.webhookUrl">
              <n-input v-model:value="configObj.webhookUrl" placeholder="钉钉机器人Webhook地址" />
            </n-form-item>
            <n-form-item label="安全设置">
              <n-select v-model:value="configObj.securityType" :options="dingtalkSecurityOptions" placeholder="选择安全设置类型" />
            </n-form-item>
            <n-form-item v-if="configObj.securityType === 'sign'" label="签名密钥">
              <n-input v-model:value="configObj.secret" type="password" show-password-on="click" placeholder="加签的Secret" />
            </n-form-item>
            <n-form-item v-if="configObj.securityType === 'keyword'" label="关键词">
              <n-input v-model:value="configObj.keyword" placeholder="消息必须包含的关键词" />
            </n-form-item>
          </template>
          <template v-if="configObj.msgType === 'app'">
            <n-form-item label="AgentId" path="configObj.agentId">
              <n-input v-model:value="configObj.agentId" placeholder="钉钉应用的AgentId" />
            </n-form-item>
            <n-form-item label="AppKey" path="configObj.appKey">
              <n-input v-model:value="configObj.appKey" placeholder="钉钉应用的AppKey" />
            </n-form-item>
            <n-form-item label="AppSecret" path="configObj.appSecret">
              <n-input v-model:value="configObj.appSecret" type="password" show-password-on="click" placeholder="钉钉应用的AppSecret" />
            </n-form-item>
            <n-form-item label="API域名">
              <n-input v-model:value="configObj.apiDomain" placeholder="可选，默认 https://oapi.dingtalk.com" />
              <n-text depth="3" style="margin-left: 8px; font-size: 12px;">内网部署可修改</n-text>
            </n-form-item>
          </template>
        </template>

        <!-- 短信配置 -->
        <template v-if="form.channelType === 'sms'">
          <n-form-item label="服务商" path="configObj.provider">
            <n-select v-model:value="configObj.provider" :options="smsProviderOptions" placeholder="请选择短信服务商" @update:value="onSmsProviderChange" />
          </n-form-item>
          <template v-if="configObj.provider === 'aliyun' || configObj.provider === 'tencent'">
            <n-form-item label="AccessKey" path="configObj.accessKey">
              <n-input v-model:value="configObj.accessKey" placeholder="服务商的AccessKey" />
            </n-form-item>
            <n-form-item label="SecretKey" path="configObj.secretKey">
              <n-input v-model:value="configObj.secretKey" type="password" show-password-on="click" placeholder="服务商的SecretKey" />
            </n-form-item>
            <n-form-item v-if="configObj.provider === 'tencent'" label="SDKAppId">
              <n-input v-model:value="configObj.sdkAppId" placeholder="腾讯云短信应用ID" />
            </n-form-item>
          </template>
          <template v-if="configObj.provider === 'internal'">
            <n-form-item label="网关地址" path="configObj.gatewayUrl">
              <n-input v-model:value="configObj.gatewayUrl" placeholder="内网短信网关API地址" />
            </n-form-item>
            <n-form-item label="认证方式">
              <n-select v-model:value="configObj.authType" :options="internalAuthOptions" placeholder="选择认证方式" />
            </n-form-item>
            <template v-if="configObj.authType === 'basic'">
              <n-form-item label="用户名">
                <n-input v-model:value="configObj.username" placeholder="Basic认证用户名" />
              </n-form-item>
              <n-form-item label="密码">
                <n-input v-model:value="configObj.password" type="password" show-password-on="click" placeholder="Basic认证密码" />
              </n-form-item>
            </template>
            <template v-if="configObj.authType === 'token'">
              <n-form-item label="Token">
                <n-input v-model:value="configObj.token" type="password" show-password-on="click" placeholder="API Token" />
              </n-form-item>
            </template>
            <template v-if="configObj.authType === 'apikey'">
              <n-form-item label="API Key">
                <n-input v-model:value="configObj.apiKey" placeholder="API Key" />
              </n-form-item>
              <n-form-item label="API Secret">
                <n-input v-model:value="configObj.apiSecret" type="password" show-password-on="click" placeholder="API Secret" />
              </n-form-item>
            </template>
            <n-form-item label="请求方法">
              <n-select v-model:value="configObj.httpMethod" :options="httpMethodOptions" placeholder="POST" />
            </n-form-item>
            <n-form-item label="参数格式">
              <n-select v-model:value="configObj.contentType" :options="contentTypeOptions" placeholder="JSON" />
            </n-form-item>
            <n-form-item label="手机号参数名">
              <n-input v-model:value="configObj.phoneParam" placeholder="如：mobile, phone, to" />
            </n-form-item>
            <n-form-item label="内容参数名">
              <n-input v-model:value="configObj.contentParam" placeholder="如：content, message, text" />
            </n-form-item>
            <n-form-item label="额外参数">
              <n-input v-model:value="configObj.extraParams" type="textarea" :rows="2" placeholder='JSON格式额外参数' />
            </n-form-item>
          </template>
          <n-form-item label="签名名称" path="configObj.signName">
            <n-input v-model:value="configObj.signName" placeholder="短信签名，如：XX公司" />
          </n-form-item>
          <n-form-item label="模板ID">
            <n-input v-model:value="configObj.templateId" placeholder="可选，默认模板ID" />
          </n-form-item>
        </template>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 测试发送弹窗 -->
    <n-modal v-model:show="showTestModal" preset="card" title="测试发送" style="width: 460px; border-radius: 16px;">
      <n-form label-placement="left" label-width="100px">
        <n-form-item label="通道名称">
          <n-tag type="info">{{ testTarget?.channelName }}</n-tag>
          <n-tag type="default" size="small" style="margin-left: 8px;">{{ testTarget?.channelType }}</n-tag>
        </n-form-item>
        <n-form-item label="接收人">
          <n-input v-model:value="testRecipient" :placeholder="testRecipientPlaceholder" />
        </n-form-item>
        <n-form-item v-if="testTarget?.channelType !== 'wecom' || testConfigObj?.msgType !== 'webhook'" label="测试内容">
          <n-input v-model:value="testContent" type="textarea" :rows="3" placeholder="输入测试消息内容" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTestModal = false">取消</n-button>
          <n-button type="primary" :loading="testSending" :disabled="!testRecipient.trim()" @click="doSendTest">
            <template #icon><n-icon><SendOutline /></n-icon></template>
            发送测试
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, h } from 'vue'
import { NButton, NTag, NSpace, NRadio, NRadioGroup, useMessage, useDialog } from 'naive-ui'
import {
  ChatbubblesOutline,
  AddOutline,
  MailOutline,
  LogoWechat,
  MegaphoneOutline,
  PhonePortraitOutline,
  SearchOutline,
  SendOutline
} from '@vicons/ionicons5'
import type { FormInst } from 'naive-ui'
import {
  getMessageChannels,
  getChannelById,
  createChannel,
  updateChannel,
  deleteChannel,
  setDefaultChannel,
  testChannel,
  type MessageChannel
} from '@/api/messageChannel'

const message = useMessage()
const dialog = useDialog()
const loading = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const isEdit = ref(false)

// Search & filter
const searchKeyword = ref('')
const filterType = ref<string | null>(null)
const filterStatus = ref<number | null>(null)

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)

const channels = ref<MessageChannel[]>([])

const channelTypes = [
  { value: 'email', label: '邮件', icon: MailOutline },
  { value: 'wecom', label: '企业微信', icon: LogoWechat },
  { value: 'dingtalk', label: '钉钉', icon: MegaphoneOutline },
  { value: 'sms', label: '短信', icon: PhonePortraitOutline }
]

const channelTypeOptions = channelTypes.map(t => ({ label: t.label, value: t.value }))
const filterTypeOptions = [
  { label: '邮件', value: 'email' },
  { label: '企业微信', value: 'wecom' },
  { label: '钉钉', value: 'dingtalk' },
  { label: '短信', value: 'sms' }
]
const filterStatusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]
const smsProviderOptions = [
  { label: '阿里云', value: 'aliyun' },
  { label: '腾讯云', value: 'tencent' },
  { label: '内网短信网关', value: 'internal' }
]
const wecomContentTypeOptions = [
  { label: '文本消息', value: 'text' },
  { label: 'Markdown', value: 'markdown' }
]
const dingtalkSecurityOptions = [
  { label: '加签', value: 'sign' },
  { label: '关键词', value: 'keyword' },
  { label: 'IP白名单', value: 'ip' }
]
const internalAuthOptions = [
  { label: '无认证', value: 'none' },
  { label: 'Basic认证', value: 'basic' },
  { label: 'Token认证', value: 'token' },
  { label: 'API Key', value: 'apikey' }
]
const httpMethodOptions = [
  { label: 'POST', value: 'POST' },
  { label: 'GET', value: 'GET' }
]
const contentTypeOptions = [
  { label: 'JSON', value: 'application/json' },
  { label: 'Form表单', value: 'application/x-www-form-urlencoded' }
]

const onSmsProviderChange = () => {
  if (configObj.provider === 'internal') {
    configObj.authType = 'none'
    configObj.httpMethod = 'POST'
    configObj.contentType = 'application/json'
  }
}

const form = reactive({
  id: null as number | null,
  channelName: '',
  channelType: '' as string,
  description: '',
  status: 1,
  isDefault: 0
})

const configObj = reactive<Record<string, any>>({})

const rules = {
  channelName: [{ required: true, message: '请输入通道名称', trigger: 'blur' }],
  channelType: [{ required: true, message: '请选择通道类型', trigger: 'change' }]
}

const statusSwitch = computed({
  get: () => form.status === 1,
  set: (val: boolean) => { form.status = val ? 1 : 0 }
})

const defaultSwitch = computed({
  get: () => form.isDefault === 1,
  set: (val: boolean) => { form.isDefault = val ? 1 : 0 }
})

const getConfigTitle = computed(() => {
  const type = channelTypes.find(t => t.value === form.channelType)
  return type ? `${type.label}配置` : '通道配置'
})

const filteredChannels = computed(() => {
  let list = channels.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(c => c.channelName.toLowerCase().includes(kw))
  }
  if (filterType.value) {
    list = list.filter(c => c.channelType === filterType.value)
  }
  if (filterStatus.value !== null) {
    list = list.filter(c => c.status === filterStatus.value)
  }
  return list
})

const paginatedChannels = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredChannels.value.slice(start, start + pageSize.value)
})

const getTypeCount = (type: string) => {
  return channels.value.filter(c => c.channelType === type).length
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  searchKeyword.value = ''
  filterType.value = null
  filterStatus.value = null
  currentPage.value = 1
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
}

const columns = [
  { title: '通道名称', key: 'channelName', width: 180, ellipsis: { tooltip: true } },
  {
    title: '类型',
    key: 'channelType',
    width: 100,
    render: (row: MessageChannel) => {
      const type = channelTypes.find(t => t.value === row.channelType)
      return h(NTag, { type: 'info', size: 'small' }, { default: () => type?.label || row.channelType })
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: MessageChannel) => h(NTag, { type: row.status === 1 ? 'success' : 'default', size: 'small' }, { default: () => row.status === 1 ? '启用' : '禁用' })
  },
  {
    title: '默认',
    key: 'isDefault',
    width: 80,
    render: (row: MessageChannel) => row.isDefault === 1 ? h(NTag, { type: 'warning', size: 'small' }, { default: () => '默认' }) : '-'
  },
  { title: '描述', key: 'description', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    fixed: 'right' as const,
    render: (row: MessageChannel) => {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          row.isDefault !== 1 && h(NButton, { size: 'small', type: 'info', onClick: () => handleSetDefault(row.id) }, { default: () => '设为默认' }),
          h(NButton, { size: 'small', type: 'tertiary', onClick: () => handleTest(row) }, { default: () => '测试' }),
          h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' })
        ].filter(Boolean)
      })
    }
  }
]

const loadChannels = async () => {
  loading.value = true
  try {
    const res = await getMessageChannels() as any
    channels.value = res.data || []
  } catch (error: any) {
    message.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.id = null
  form.channelName = ''
  form.channelType = ''
  form.description = ''
  form.status = 1
  form.isDefault = 0
  Object.keys(configObj).forEach(k => delete configObj[k])
  showModal.value = true
}

const handleEdit = async (row: MessageChannel) => {
  isEdit.value = true
  form.id = row.id
  form.channelName = row.channelName
  form.channelType = row.channelType
  form.description = row.description || ''
  form.status = row.status
  form.isDefault = row.isDefault

  Object.keys(configObj).forEach(k => delete configObj[k])
  // 从API获取完整配置（列表接口会截断敏感字段）
  try {
    const res = await getChannelById(row.id) as any
    const fullChannel = res.data || res
    if (fullChannel.config) {
      const parsed = JSON.parse(fullChannel.config)
      Object.assign(configObj, parsed)
    }
  } catch {
    // 降级：使用列表数据
    if (row.config) {
      try {
        const parsed = JSON.parse(row.config)
        Object.assign(configObj, parsed)
      } catch { /* ignore */ }
    }
  }
  showModal.value = true
}

const formRef = ref<FormInst | null>(null)

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const data = {
      ...form,
      config: JSON.stringify(configObj)
    }
    if (isEdit.value && form.id) {
      await updateChannel(form.id, data)
      message.success('更新成功')
    } else {
      await createChannel(data)
      message.success('创建成功')
    }
    showModal.value = false
    loadChannels()
  } catch (error: any) {
    message.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row: MessageChannel) => {
  dialog.warning({
    title: '确认删除',
    content: `确定删除通道「${row.channelName}」吗？此操作不可恢复。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteChannel(row.id)
        message.success('删除成功')
        loadChannels()
      } catch (error: any) {
        message.error(error.message || '删除失败')
      }
    }
  })
}

const handleSetDefault = async (id: number) => {
  try {
    await setDefaultChannel(id)
    message.success('设置成功')
    loadChannels()
  } catch (error: any) {
    message.error(error.message || '设置失败')
  }
}

// 测试发送相关
const showTestModal = ref(false)
const testTarget = ref<MessageChannel | null>(null)
const testConfigObj = ref<Record<string, any>>({})
const testRecipient = ref('')
const testContent = ref('这是一条测试消息，请忽略。')
const testSending = ref(false)

const testRecipientPlaceholder = computed(() => {
  if (!testTarget.value) return '请输入接收人'
  switch (testTarget.value.channelType) {
    case 'email': return '请输入接收邮箱地址'
    case 'sms': return '请输入接收手机号'
    case 'wecom': return testConfigObj.value?.msgType === 'webhook' ? 'Webhook无需指定，输入任意值' : '请输入企业微信用户ID'
    case 'dingtalk': return testConfigObj.value?.msgType === 'webhook' ? 'Webhook无需指定，输入任意值' : '请输入钉钉用户ID'
    default: return '请输入接收人'
  }
})

const handleTest = (row: MessageChannel) => {
  testTarget.value = row
  testRecipient.value = ''
  testContent.value = '这是一条测试消息，请忽略。'
  try {
    testConfigObj.value = row.config ? JSON.parse(row.config) : {}
  } catch (_) { testConfigObj.value = {} /* config parse failed, use empty */ }
  showTestModal.value = true
}

const doSendTest = async () => {
  if (!testTarget.value || !testRecipient.value.trim()) return
  testSending.value = true
  try {
    await testChannel(testTarget.value.id, testRecipient.value.trim(), testContent.value)
    message.success('测试消息已发送')
    showTestModal.value = false
  } catch (error: any) {
    message.error(error.message || '测试失败')
  } finally {
    testSending.value = false
  }
}

onMounted(() => {
  loadChannels()
})
</script>

<style scoped>
.message-channel-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: column;
  }
}
</style>
