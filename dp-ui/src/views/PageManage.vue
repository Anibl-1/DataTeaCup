<template>
  <div class="page-manage-container">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item stat-item-primary">
        <div class="stat-icon">
          <n-icon size="24"><DocumentsOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ paginationItemCount }}</span>
          <span class="stat-label">{{ activeLayoutMode === 'bigscreen' ? '大屏总数' : activeLayoutMode === 'mobile' ? '移动页面' : '页面总数' }}</span>
        </div>
      </div>
      <div class="stat-item stat-item-success">
        <div class="stat-icon">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ enabledCount }}</span>
          <span class="stat-label">已启用</span>
        </div>
      </div>
      <div class="stat-item stat-item-warning">
        <div class="stat-icon">
          <n-icon size="24"><GridOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalChartsCount }}</span>
          <span class="stat-label">图表总数</span>
        </div>
      </div>
      <div class="stat-item stat-item-info">
        <div class="stat-icon">
          <n-icon size="24"><LayersOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tableData.length }}</span>
          <span class="stat-label">当前页</span>
        </div>
      </div>
    </div>

    <!-- 布局模式标签页 -->
    <div class="layout-mode-tabs">
      <div
        v-for="tab in layoutModeTabs"
        :key="tab.mode"
        class="layout-tab"
        :class="{ active: activeLayoutMode === tab.mode, [`mode-${tab.mode}`]: true }"
        @click="handleLayoutModeChange(tab.mode)"
      >
        <n-icon size="18"><component :is="tab.icon" /></n-icon>
        <span>{{ tab.label }}</span>
        <n-badge v-if="tab.count > 0" :value="tab.count" :max="99" :type="tab.mode === 'bigscreen' ? 'info' : tab.mode === 'mobile' ? 'success' : 'default'" />
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper" :class="`header-icon-${activeLayoutMode}`">
            <n-icon size="18"><component :is="activeLayoutMode === 'bigscreen' ? TvOutline : activeLayoutMode === 'mobile' ? PhonePortraitOutline : DocumentsOutline" /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">{{ activeLayoutMode === 'bigscreen' ? '大屏可视化项目' : activeLayoutMode === 'mobile' ? '移动端页面' : '页面管理' }}</span>
            <span class="header-subtitle">{{ activeLayoutMode === 'bigscreen' ? '数据可视化大屏项目管理' : activeLayoutMode === 'mobile' ? '移动端页面设计与管理' : '创建和管理数据分析页面' }}</span>
          </div>
        </div>
      </template>

      <!-- 工具栏 -->
      <div class="table-toolbar">
        <n-space align="center" :wrap="false">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索页面名称或编码"
            clearable
            style="width: 250px"
            @keydown.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
          <n-button @click="handleSearch">搜索</n-button>
          <n-button quaternary @click="handleResetSearch">重置</n-button>
        </n-space>
        <n-space align="center">
          <n-button @click="router.push('/chart-manage')">
            <template #icon>
              <n-icon><GridOutline /></n-icon>
            </template>
            图表资源库
          </n-button>
          <n-button v-if="activeLayoutMode === 'desktop'" secondary @click="showMobilePublish = true">
            <template #icon>
              <n-icon><PhonePortraitOutline /></n-icon>
            </template>
            移动端发布
          </n-button>
          <n-button v-permission="'page:manage:add'" type="primary" @click="handleCreateForCurrentMode">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            {{ activeLayoutMode === 'bigscreen' ? '新建大屏' : activeLayoutMode === 'mobile' ? '新建移动端页面' : '新建页面' }}
          </n-button>
        </n-space>
      </div>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :scroll-x="1100"
        striped
        class="custom-table"
      >
        <template #empty>
          <div class="empty-state-wrapper">
            <div class="empty-state-icon" :class="`empty-${activeLayoutMode}`">
              <n-icon size="48"><component :is="activeLayoutMode === 'bigscreen' ? TvOutline : activeLayoutMode === 'mobile' ? PhonePortraitOutline : DocumentsOutline" /></n-icon>
            </div>
            <div class="empty-state-title">{{ activeLayoutMode === 'bigscreen' ? '还没有大屏项目' : activeLayoutMode === 'mobile' ? '还没有移动端页面' : '还没有页面' }}</div>
            <div class="empty-state-desc">{{ activeLayoutMode === 'bigscreen' ? '创建您的第一个数据可视化大屏，支持 1920×1080 等多种分辨率' : activeLayoutMode === 'mobile' ? '创建您的第一个移动端页面，支持 iOS 和 Android 适配' : '创建您的第一个数据分析页面，拖拽图表即可快速搭建' }}</div>
            <n-button v-permission="'page:manage:add'" type="primary" size="large" style="margin-top: 16px;" @click="handleCreateForCurrentMode">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              {{ activeLayoutMode === 'bigscreen' ? '创建第一个大屏' : activeLayoutMode === 'mobile' ? '创建第一个移动页面' : '创建第一个页面' }}
            </n-button>
          </div>
        </template>
      </n-data-table>
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            共 {{ paginationItemCount }} 条记录
          </n-tag>
        </div>
        <n-pagination
          v-model:page="paginationPage"
          v-model:page-size="paginationPageSize"
          :item-count="paginationItemCount"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    
    <!-- 生成菜单对话框 -->
    <n-modal
      v-model:show="showMenuModal"
      preset="card"
      title="生成菜单"
      style="width: 700px;"
      :segmented="{ content: 'soft', footer: 'soft' }"
    >
      <div style="max-height: 70vh; overflow-y: auto;">
        <n-form
          ref="menuFormRef"
          :model="menuForm"
          label-placement="left"
          label-width="100px"
          :rules="{
            menuName: { required: true, message: '请输入菜单名称', trigger: 'blur' },
            menuCode: { required: true, message: '请输入菜单编码', trigger: 'blur' },
            routePath: { required: true, message: '请输入路由路径', trigger: 'blur' }
          }"
        >
          <n-form-item label="菜单名称" path="menuName">
            <n-input v-model:value="menuForm.menuName" placeholder="请输入菜单名称" />
          </n-form-item>
          <n-form-item label="菜单编码" path="menuCode">
            <n-input v-model:value="menuForm.menuCode" placeholder="请输入菜单编码（唯一标识）" />
          </n-form-item>
          <n-form-item label="父菜单">
            <n-tree-select
              v-model:value="menuForm.parentId"
              :options="menuTreeOptions"
              placeholder="请选择父菜单（不选则为顶级菜单）"
              clearable
              filterable
            />
          </n-form-item>
          <n-form-item label="图标">
            <div class="icon-picker-container">
              <!-- 已选择的图标显示 -->
              <div v-if="menuForm.icon" class="icon-picker-selected">
                <n-icon :component="getIconComponent(menuForm.icon)" size="24" />
                <span>已选择: {{ getIconLabel(menuForm.icon) }}</span>
                <n-button size="tiny" quaternary style="margin-left: auto;" @click="menuForm.icon = ''">清除</n-button>
              </div>
              
              <!-- 搜索框 -->
              <n-input
                v-model:value="iconSearchKeyword"
                placeholder="搜索图标..."
                clearable
                style="margin-bottom: 12px;"
              >
                <template #prefix>
                  <n-icon><SearchOutline /></n-icon>
                </template>
              </n-input>
              
              <!-- 分类标签 -->
              <n-tabs v-model:value="iconCategory" type="segment" size="small" style="margin-bottom: 12px;">
                <n-tab-pane v-for="category in iconCategories" :key="category.key" :name="category.key" :tab="category.label" />
              </n-tabs>
              
              <!-- 图标网格 -->
              <div class="icon-grid">
                <div
                  v-for="iconOption in filteredIcons"
                  :key="iconOption.value"
                  class="icon-select-item"
                  :class="{ 'icon-selected': menuForm.icon === iconOption.value }"
                  :title="iconOption.label"
                  @click="menuForm.icon = iconOption.value"
                >
                  <n-icon v-if="getIconComponent(iconOption.value)" :component="getIconComponent(iconOption.value)" size="28" />
                  <div v-else class="icon-placeholder"></div>
                  <span style="font-size: 11px; text-align: center; word-break: break-word; line-height: 1.2;">{{ iconOption.label }}</span>
                </div>
              </div>
              
              <!-- 无结果提示 -->
              <div v-if="filteredIcons.length === 0" class="icon-empty">未找到匹配的图标</div>
              
              <!-- 提示信息 -->
              <div class="icon-count">共 {{ filteredIcons.length }} 个图标</div>
            </div>
          </n-form-item>
          <n-form-item label="路由路径" path="routePath">
            <n-input v-model:value="menuForm.routePath" placeholder="路由路径" readonly />
          </n-form-item>
          <n-form-item label="组件路径">
            <n-input v-model:value="menuForm.componentPath" placeholder="组件路径" readonly />
          </n-form-item>
          <n-form-item label="权限编码">
            <n-input v-model:value="menuForm.permissionCode" placeholder="权限编码" />
          </n-form-item>
          <n-form-item label="排序">
            <n-input-number v-model:value="menuForm.sortOrder" :min="0" style="width: 100%" />
          </n-form-item>
        </n-form>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showMenuModal = false">取消</n-button>
          <n-button type="primary" :loading="creatingMenu" @click="handleSubmitMenu">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 移动端发布对话框 -->
    <MobilePublishDialog
      v-model:show="showMobilePublish"
      title="页面移动端发布"
      :items="mobilePublishItems"
      @toggle="handleMobileToggle"
      @batch-publish="handleMobileBatchPublish"
      @batch-unpublish="handleMobileBatchUnpublish"
    />
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, h, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag, NSpace, NIcon, NDropdown, useMessage, useDialog, NModal, NForm, NFormItem, NInput, NTreeSelect, NInputNumber, NTabs, NTabPane, NBadge } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import { 
  AddOutline, SearchOutline, DocumentsOutline, CheckmarkCircleOutline, GridOutline, LayersOutline,
  // 图标选择器需要的图标
  HomeOutline, SettingsOutline, PersonOutline, PeopleOutline,
  FolderOutline, DocumentOutline, AppsOutline, MenuOutline, NavigateOutline,
  CompassOutline, MapOutline, LocationOutline, EarthOutline, GlobeOutline,
  FlagOutline, ServerOutline, LibraryOutline, StatsChartOutline, PieChartOutline,
  TrendingUpOutline, TrendingDownOutline, PulseOutline, SpeedometerOutline,
  DownloadOutline, CloudUploadOutline, SyncOutline, RefreshOutline, ReloadOutline,
  CloudOutline, CloudDownloadOutline, DocumentTextOutline,
  FolderOpenOutline, FileTrayOutline, ArchiveOutline, ClipboardOutline,
  SaveOutline, PrintOutline, CopyOutline, CutOutline, TrashBinOutline,
  PlayOutline, PauseOutline, StopOutline, PlaySkipForwardOutline, PlaySkipBackOutline,
  VolumeHighOutline, VolumeLowOutline, VolumeMuteOutline, MicOutline,
  MicOffOutline, CameraOutline, ImageOutline, ImagesOutline, VideocamOutline,
  FilmOutline, MusicalNotesOutline, BusinessOutline, StorefrontOutline,
  CartOutline, WalletOutline, CardOutline, CashOutline, ReceiptOutline,
  CalculatorOutline, LockClosedOutline, LockOpenOutline, KeyOutline,
  ShieldCheckmarkOutline, EyeOutline, EyeOffOutline, PowerOutline,
  BatteryFullOutline, WifiOutline, NotificationsOutline, MailOutline,
  CalendarOutline, TimeOutline, WarningOutline, InformationCircleOutline,
  FilterOutline, ColorPaletteOutline, ColorFilterOutline, BrushOutline,
  ColorFillOutline, ShapesOutline, SquareOutline, AtCircleOutline,
  TriangleOutline, EllipseOutline, RadioButtonOnOutline, RadioButtonOffOutline,
  ToggleOutline, LinkOutline, ShareOutline, StarOutline, HeartOutline,
  ThumbsUpOutline, ThumbsDownOutline, AnalyticsOutline, BarChartOutline,
  CheckboxOutline, CloseCircleOutline, PhonePortraitOutline,
  DesktopOutline, TvOutline, EllipsisVerticalOutline
} from '@vicons/ionicons5'
import MobilePublishDialog from '@/components/mobile/MobilePublishDialog.vue'
import type { PublishItem } from '@/components/mobile/MobilePublishDialog.vue'
import { getPageDefinitionList, deletePageDefinition, updatePageMobileEnabled, getLayoutModeCounts } from '@/api/page'
import { getAllMenus, getVisibleMenus, createMenu, deleteMenu } from '@/api/system/menu'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import type { PageDefinition, PageLayoutMode } from '@/types/page'
import type { PageResult } from '@/types/api'
import type { Menu, MenuForm } from '@/types/menu'
import { formatDateTime } from '@/utils/format'
import { hasPermission } from '@/utils/permission'
import { useTabsStore } from '@/stores/tabs'

const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const tabsStore = useTabsStore()
initMessage(message)

// ==================== 布局模式 ====================
const activeLayoutMode = ref<PageLayoutMode>('desktop')
const layoutModeCounts = ref<Record<string, number>>({ desktop: 0, mobile: 0, bigscreen: 0 })

const layoutModeTabs = computed(() => [
  { mode: 'desktop' as PageLayoutMode, label: '桌面端', icon: DesktopOutline, count: layoutModeCounts.value['desktop'] || 0 },
  { mode: 'mobile' as PageLayoutMode, label: '移动端', icon: PhonePortraitOutline, count: layoutModeCounts.value['mobile'] || 0 },
  { mode: 'bigscreen' as PageLayoutMode, label: '大屏', icon: TvOutline, count: layoutModeCounts.value['bigscreen'] || 0 }
])

const loadLayoutModeCounts = async () => {
  try {
    const res = await getLayoutModeCounts()
    if (res && typeof res === 'object') {
      const data = (res as any).data || res
      if (data && typeof data === 'object') {
        layoutModeCounts.value = {
          desktop: Number(data.desktop) || 0,
          mobile: Number(data.mobile) || 0,
          bigscreen: Number(data.bigscreen) || 0
        }
      }
    }
  } catch { /* ignore */ }
}

const handleLayoutModeChange = (mode: PageLayoutMode) => {
  activeLayoutMode.value = mode
  paginationPage.value = 1
  loadData()
  loadLayoutModeCounts()
}

const handleCreateForCurrentMode = () => {
  const mode = activeLayoutMode.value
  if (mode === 'desktop') {
    handleCreate()
  } else {
    const modeLabel = mode === 'mobile' ? '移动端' : '大屏'
    tabsStore.replaceTab('/page-manage', {
      key: `/page-designer/new?layoutMode=${mode}`,
      title: `新建${modeLabel}页面`,
      closable: true
    })
    router.replace(`/page-designer/new?layoutMode=${mode}`)
  }
}

// ==================== 移动端发布 ====================
const showMobilePublish = ref(false)
const mobilePublishItems = computed<PublishItem[]>(() =>
  tableData.value
    .filter((p): p is PageDefinition & { id: number } => p.id != null)
    .map(p => ({
      id: p.id,
      name: p.pageName,
      code: p.pageCode,
      mobileEnabled: p.mobileEnabled || 0
    }))
)

async function handleMobileToggle(id: number, enabled: boolean) {
  const item = tableData.value.find(p => p.id === id)
  if (item) {
    const mobileEnabled = enabled ? 1 : 0
    try {
      await updatePageMobileEnabled(id, mobileEnabled)
      item.mobileEnabled = mobileEnabled
      message.success(enabled ? '已启用移动端' : '已禁用移动端')
    } catch (e) {
      console.error('[PageManage] API错误:', e)
      handleApiError(e, '更新移动端配置失败')
    }
  }
}

async function handleMobileBatchPublish() {
  try {
    for (const p of tableData.value) {
      if (p.id) {
        await updatePageMobileEnabled(p.id, 1)
        p.mobileEnabled = 1
      }
    }
    message.success('已批量启用移动端')
  } catch (e) {
    handleApiError(e, '批量启用失败')
  }
}

async function handleMobileBatchUnpublish() {
  try {
    for (const p of tableData.value) {
      if (p.id) {
        await updatePageMobileEnabled(p.id, 0)
        p.mobileEnabled = 0
      }
    }
    message.success('已批量禁用移动端')
  } catch (e) {
    handleApiError(e, '批量禁用失败')
  }
}

const loading = ref(false)
const tableData = ref<PageDefinition[]>([])
const searchKeyword = ref('')

const paginationPage = ref(DEFAULT_PAGE)
const paginationPageSize = ref(DEFAULT_PAGE_SIZE)
const paginationItemCount = ref(0)

// 计算属性
const enabledCount = computed(() => {
  return tableData.value.filter(item => item.status === 1).length
})

const totalChartsCount = computed(() => {
  return tableData.value.reduce((sum, item) => {
    if (item.charts && Array.isArray(item.charts)) {
      return sum + item.charts.length
    }
    return sum
  }, 0)
})

// 生成菜单相关
const showMenuModal = ref(false)
const creatingMenu = ref(false)
const currentPage = ref<PageDefinition | null>(null)
const menuList = ref<Menu[]>([])
const menuForm = ref<MenuForm>({
  menuName: '',
  menuCode: '',
  parentId: 0,
  menuType: 'menu',
  routePath: '',
  componentPath: '@/views/PageView.vue',
  icon: '',
  sortOrder: 0,
  isVisible: 1,
  permissionCode: ''
})
const menuFormRef = ref<FormInst | null>(null)

// 图标选择相关
const iconCategory = ref('all')
const iconSearchKeyword = ref('')

// 图标分类
const iconCategories = [
  { key: 'all', label: '全部' },
  { key: 'common', label: '常用' },
  { key: 'navigation', label: '导航' },
  { key: 'data', label: '数据' },
  { key: 'file', label: '文件' },
  { key: 'media', label: '媒体' },
  { key: 'business', label: '商业' },
  { key: 'system', label: '系统' },
  { key: 'ui', label: '界面' }
]

// 图标选项
const allIconOptions = [
  // 常用
  { label: '网格', value: 'GridOutline', category: 'common' },
  { label: '首页', value: 'HomeOutline', category: 'common' },
  { label: '设置', value: 'SettingsOutline', category: 'common' },
  { label: '搜索', value: 'SearchOutline', category: 'common' },
  { label: '用户', value: 'PersonOutline', category: 'common' },
  { label: '用户组', value: 'PeopleOutline', category: 'common' },
  { label: '文件夹', value: 'FolderOutline', category: 'common' },
  { label: '文档', value: 'DocumentOutline', category: 'common' },
  { label: '应用', value: 'AppsOutline', category: 'common' },
  { label: '菜单', value: 'MenuOutline', category: 'common' },
  // 导航
  { label: '导航', value: 'NavigateOutline', category: 'navigation' },
  { label: '指南针', value: 'CompassOutline', category: 'navigation' },
  { label: '地图', value: 'MapOutline', category: 'navigation' },
  { label: '位置', value: 'LocationOutline', category: 'navigation' },
  { label: '地球', value: 'EarthOutline', category: 'navigation' },
  { label: '全球', value: 'GlobeOutline', category: 'navigation' },
  { label: '旗帜', value: 'FlagOutline', category: 'navigation' },
  // 数据
  { label: '服务器', value: 'ServerOutline', category: 'data' },
  { label: '数据库', value: 'LibraryOutline', category: 'data' },
  { label: '分析', value: 'AnalyticsOutline', category: 'data' },
  { label: '统计', value: 'StatsChartOutline', category: 'data' },
  { label: '柱状图', value: 'BarChartOutline', category: 'data' },
  { label: '饼图', value: 'PieChartOutline', category: 'data' },
  { label: '趋势上升', value: 'TrendingUpOutline', category: 'data' },
  { label: '趋势下降', value: 'TrendingDownOutline', category: 'data' },
  { label: '脉冲', value: 'PulseOutline', category: 'data' },
  { label: '速度表', value: 'SpeedometerOutline', category: 'data' },
  { label: '下载', value: 'DownloadOutline', category: 'data' },
  { label: '上传', value: 'CloudUploadOutline', category: 'data' },
  { label: '同步', value: 'SyncOutline', category: 'data' },
  { label: '刷新', value: 'RefreshOutline', category: 'data' },
  { label: '云', value: 'CloudOutline', category: 'data' },
  { label: '云下载', value: 'CloudDownloadOutline', category: 'data' },
  // 文件
  { label: '文档文本', value: 'DocumentTextOutline', category: 'file' },
  { label: '文档集', value: 'DocumentsOutline', category: 'file' },
  { label: '文件夹打开', value: 'FolderOpenOutline', category: 'file' },
  { label: '文件盒', value: 'FileTrayOutline', category: 'file' },
  { label: '归档', value: 'ArchiveOutline', category: 'file' },
  { label: '剪贴板', value: 'ClipboardOutline', category: 'file' },
  { label: '保存', value: 'SaveOutline', category: 'file' },
  { label: '打印', value: 'PrintOutline', category: 'file' },
  { label: '复制', value: 'CopyOutline', category: 'file' },
  { label: '剪切', value: 'CutOutline', category: 'file' },
  { label: '删除', value: 'TrashBinOutline', category: 'file' },
  // 媒体
  { label: '播放', value: 'PlayOutline', category: 'media' },
  { label: '暂停', value: 'PauseOutline', category: 'media' },
  { label: '停止', value: 'StopOutline', category: 'media' },
  { label: '快进', value: 'PlaySkipForwardOutline', category: 'media' },
  { label: '快退', value: 'PlaySkipBackOutline', category: 'media' },
  { label: '音量高', value: 'VolumeHighOutline', category: 'media' },
  { label: '音量低', value: 'VolumeLowOutline', category: 'media' },
  { label: '静音', value: 'VolumeMuteOutline', category: 'media' },
  { label: '麦克风', value: 'MicOutline', category: 'media' },
  { label: '麦克风关闭', value: 'MicOffOutline', category: 'media' },
  { label: '相机', value: 'CameraOutline', category: 'media' },
  { label: '图片', value: 'ImageOutline', category: 'media' },
  { label: '图片集', value: 'ImagesOutline', category: 'media' },
  { label: '视频', value: 'VideocamOutline', category: 'media' },
  { label: '电影', value: 'FilmOutline', category: 'media' },
  { label: '音乐', value: 'MusicalNotesOutline', category: 'media' },
  // 商业
  { label: '商业', value: 'BusinessOutline', category: 'business' },
  { label: '商店', value: 'StorefrontOutline', category: 'business' },
  { label: '购物车', value: 'CartOutline', category: 'business' },
  { label: '钱包', value: 'WalletOutline', category: 'business' },
  { label: '卡片', value: 'CardOutline', category: 'business' },
  { label: '现金', value: 'CashOutline', category: 'business' },
  { label: '收据', value: 'ReceiptOutline', category: 'business' },
  { label: '计算器', value: 'CalculatorOutline', category: 'business' },
  // 系统
  { label: '锁', value: 'LockClosedOutline', category: 'system' },
  { label: '解锁', value: 'LockOpenOutline', category: 'system' },
  { label: '钥匙', value: 'KeyOutline', category: 'system' },
  { label: '盾牌', value: 'ShieldCheckmarkOutline', category: 'system' },
  { label: '眼睛', value: 'EyeOutline', category: 'system' },
  { label: '眼睛关闭', value: 'EyeOffOutline', category: 'system' },
  { label: '电源', value: 'PowerOutline', category: 'system' },
  { label: '电池', value: 'BatteryFullOutline', category: 'system' },
  { label: 'WiFi', value: 'WifiOutline', category: 'system' },
  { label: '通知', value: 'NotificationsOutline', category: 'system' },
  { label: '邮件', value: 'MailOutline', category: 'system' },
  { label: '日历', value: 'CalendarOutline', category: 'system' },
  { label: '时间', value: 'TimeOutline', category: 'system' },
  { label: '警告', value: 'WarningOutline', category: 'system' },
  { label: '信息', value: 'InformationCircleOutline', category: 'system' },
  { label: '成功', value: 'CheckmarkCircleOutline', category: 'system' },
  { label: '错误', value: 'CloseCircleOutline', category: 'system' },
  // 界面
  { label: '筛选', value: 'FilterOutline', category: 'ui' },
  { label: '颜色', value: 'ColorPaletteOutline', category: 'ui' },
  { label: '颜色滤镜', value: 'ColorFilterOutline', category: 'ui' },
  { label: '画笔', value: 'BrushOutline', category: 'ui' },
  { label: '填充', value: 'ColorFillOutline', category: 'ui' },
  { label: '形状', value: 'ShapesOutline', category: 'ui' },
  { label: '正方形', value: 'SquareOutline', category: 'ui' },
  { label: '圆形', value: 'AtCircleOutline', category: 'ui' },
  { label: '三角形', value: 'TriangleOutline', category: 'ui' },
  { label: '椭圆', value: 'EllipseOutline', category: 'ui' },
  { label: '单选', value: 'RadioButtonOnOutline', category: 'ui' },
  { label: '复选框', value: 'CheckboxOutline', category: 'ui' },
  { label: '开关', value: 'ToggleOutline', category: 'ui' },
  { label: '链接', value: 'LinkOutline', category: 'ui' },
  { label: '分享', value: 'ShareOutline', category: 'ui' },
  { label: '星星', value: 'StarOutline', category: 'ui' },
  { label: '心形', value: 'HeartOutline', category: 'ui' },
  { label: '点赞', value: 'ThumbsUpOutline', category: 'ui' },
  { label: '点踩', value: 'ThumbsDownOutline', category: 'ui' }
]

// 图标映射
const iconMap: Record<string, any> = {
  GridOutline, HomeOutline, SettingsOutline, SearchOutline, PersonOutline, PeopleOutline,
  FolderOutline, DocumentOutline, AppsOutline, MenuOutline, NavigateOutline, CompassOutline,
  MapOutline, LocationOutline, EarthOutline, GlobeOutline, FlagOutline, ServerOutline,
  LibraryOutline, AnalyticsOutline, StatsChartOutline, BarChartOutline, PieChartOutline,
  TrendingUpOutline, TrendingDownOutline, PulseOutline, SpeedometerOutline, DownloadOutline,
  CloudUploadOutline, SyncOutline, RefreshOutline, ReloadOutline, CloudOutline, CloudDownloadOutline,
  DocumentTextOutline, DocumentsOutline, FolderOpenOutline, FileTrayOutline, ArchiveOutline,
  ClipboardOutline, SaveOutline, PrintOutline, CopyOutline, CutOutline, TrashBinOutline,
  PlayOutline, PauseOutline, StopOutline, PlaySkipForwardOutline, PlaySkipBackOutline,
  VolumeHighOutline, VolumeLowOutline, VolumeMuteOutline, MicOutline, MicOffOutline,
  CameraOutline, ImageOutline, ImagesOutline, VideocamOutline, FilmOutline, MusicalNotesOutline,
  BusinessOutline, StorefrontOutline, CartOutline, WalletOutline, CardOutline, CashOutline,
  ReceiptOutline, CalculatorOutline, LockClosedOutline, LockOpenOutline, KeyOutline, ShieldCheckmarkOutline,
  EyeOutline, EyeOffOutline, PowerOutline, BatteryFullOutline, WifiOutline, NotificationsOutline,
  MailOutline, CalendarOutline, TimeOutline, WarningOutline, InformationCircleOutline,
  CheckmarkCircleOutline, CloseCircleOutline, FilterOutline, ColorPaletteOutline, ColorFilterOutline,
  BrushOutline, ColorFillOutline, ShapesOutline, SquareOutline, AtCircleOutline, TriangleOutline,
  EllipseOutline, RadioButtonOnOutline, RadioButtonOffOutline, CheckboxOutline, ToggleOutline,
  LinkOutline, ShareOutline, StarOutline, HeartOutline, ThumbsUpOutline, ThumbsDownOutline
}

// 过滤后的图标
const filteredIcons = computed(() => {
  let icons = allIconOptions
  
  // 按分类过滤
  if (iconCategory.value !== 'all') {
    icons = icons.filter(icon => icon.category === iconCategory.value)
  }
  
  // 按关键词搜索
  if (iconSearchKeyword.value) {
    const keyword = iconSearchKeyword.value.toLowerCase()
    icons = icons.filter(icon => 
      icon.label.toLowerCase().includes(keyword) || 
      icon.value.toLowerCase().includes(keyword)
    )
  }
  
  return icons
})

// 获取图标标签
const getIconLabel = (iconValue: string) => {
  const icon = allIconOptions.find(opt => opt.value === iconValue)
  return icon ? icon.label : iconValue
}

// 获取图标组件
const getIconComponent = (iconName?: string): any => {
  if (!iconName) return undefined
  return iconMap[iconName]
}

// ==================== 操作按钮渲染 ====================
const renderActions = (row: PageDefinition) => {
  const existingMenu = getPageMenu(row.id!)
  const buttons = [
    h(NButton, {
      size: 'small',
      type: 'primary',
      secondary: true,
      class: 'page-action-btn page-action-preview',
      onClick: () => handleView(row)
    }, { default: () => '预览' })
  ]
  
  if (hasPermission('page:manage:edit')) {
    buttons.push(
      h(NButton, {
        size: 'small',
        secondary: true,
        class: 'page-action-btn page-action-design',
        onClick: () => handleEdit(row)
      }, { default: () => '设计' })
    )
  }
  
  const moreOptions: any[] = []

  if (hasPermission('page:manage:menu')) {
    if (existingMenu) {
      moreOptions.push({ label: '删除菜单', key: 'delete-menu' })
    } else {
      moreOptions.push({ label: '生成菜单', key: 'create-menu' })
    }
  }
  
  if (hasPermission('page:manage:delete')) {
    if (moreOptions.length > 0) moreOptions.push({ type: 'divider', key: 'divider-delete' })
    moreOptions.push({ label: '删除页面', key: 'delete-page' })
  }

  if (moreOptions.length > 0) {
    buttons.push(
      h(NDropdown, {
        trigger: 'click',
        placement: 'bottom-end',
        options: moreOptions,
        onSelect: (key: string) => handleMoreAction(key, row, existingMenu)
      }, {
        default: () => h(NButton, { size: 'small', quaternary: true, circle: true, class: 'page-action-more' }, {
          icon: () => h(NIcon, null, { default: () => h(EllipsisVerticalOutline) })
        })
      })
    )
  }
  
  return h('div', { class: 'page-row-actions' }, buttons)
}

// ==================== 按模式差异化表格列 ====================
const handleMoreAction = (key: string, row: PageDefinition, existingMenu?: Menu) => {
  if (key === 'create-menu') {
    handleCreateMenu(row)
    return
  }
  if (key === 'delete-menu' && existingMenu?.id) {
    dialog.warning({
      title: '确认删除菜单',
      content: `确定删除页面"${row.pageName}"关联的菜单吗？`,
      positiveText: '删除菜单',
      negativeText: '取消',
      onPositiveClick: () => handleDeleteMenu(existingMenu.id)
    })
    return
  }
  if (key === 'delete-page') {
    handleDelete(row)
  }
}

const columns = computed(() => {
  const mode = activeLayoutMode.value
  const modeIcon = mode === 'bigscreen' ? TvOutline : mode === 'mobile' ? PhonePortraitOutline : DocumentsOutline
  const modeColor = mode === 'bigscreen' ? '#7c3aed' : mode === 'mobile' ? '#16a34a' : '#2563eb'
  const modeBg = mode === 'bigscreen' ? 'rgba(124,58,237,0.08)' : mode === 'mobile' ? 'rgba(22,163,106,0.08)' : 'rgba(37,99,235,0.08)'

  const cols: any[] = [
    {
      title: '页面名称',
      key: 'pageName',
      width: 220,
      ellipsis: { tooltip: true },
      render: (row: PageDefinition) => h('div', { style: 'display:flex;align-items:center;gap:10px;' }, [
        h('div', { style: `width:32px;height:32px;border-radius:8px;display:flex;align-items:center;justify-content:center;flex-shrink:0;background:${modeBg};color:${modeColor};` }, [
          h(NIcon, { size: 16, component: modeIcon })
        ]),
        h('div', { style: 'min-width:0;' }, [
          h('div', { style: 'font-weight:500;font-size:14px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;' }, row.pageName || '-'),
          h('div', { style: 'font-size:11px;color:#8c8c9a;margin-top:1px;' }, row.pageCode || '')
        ])
      ])
    }
  ]
  
  // 大屏：显示分辨率配置
  if (mode === 'bigscreen') {
    cols.push({
      title: '分辨率',
      key: 'bigscreenConfig',
      width: 140,
      render: (row: PageDefinition) => {
        try {
          const config = typeof row.bigscreenConfig === 'string' ? JSON.parse(row.bigscreenConfig) : row.bigscreenConfig
          if (config && config.width && config.height) {
            return h(NTag, { size: 'small', type: 'info', bordered: false }, { default: () => `${config.width}×${config.height}` })
          }
        } catch { /* ignore */ }
        return h(NTag, { size: 'small', bordered: false }, { default: () => '1920×1080' })
      }
    })
  }
  
  // 公共列
  cols.push(
    {
      title: '状态',
      key: 'status',
      width: 80,
      render: (row: PageDefinition) => h(NTag, { type: row.status === 1 ? 'success' : 'default', size: 'small', round: true, bordered: false }, { default: () => row.status === 1 ? '启用' : '禁用' })
    },
    {
      title: '创建时间',
      key: 'createTime',
      width: 170,
      render: (row: PageDefinition) => h('span', { style: 'font-size:13px;color:#666;' }, row.createTime ? formatDateTime(row.createTime) : '-')
    },
    {
      title: '操作',
      key: 'actions',
      width: mode === 'desktop' ? 240 : 210,
      align: 'right',
      fixed: 'right',
      render: renderActions
    }
  )
  
  return cols
})

onMounted(async () => {
  await loadData()
  await loadMenus()
  await loadLayoutModeCounts()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPageDefinitionList({
      page: paginationPage.value,
      pageSize: paginationPageSize.value,
      ...(searchKeyword.value ? { keyword: searchKeyword.value } : {}),
      layoutMode: activeLayoutMode.value
    })
    
    let pageResult: PageResult<PageDefinition> | null = null
    
    if (res && typeof res === 'object') {
      if (res.data && typeof res.data === 'object' && 'list' in res.data && 'total' in res.data) {
        pageResult = res.data as PageResult<PageDefinition>
      } else if ('list' in res && 'total' in res) {
        pageResult = res as PageResult<PageDefinition>
      }
    }
    
    const list = pageResult?.list || []
    tableData.value = list
    
    if (pageResult && pageResult.total !== undefined && pageResult.total !== null) {
      const total = typeof pageResult.total === 'number' 
        ? Math.max(0, pageResult.total)
        : (() => {
            const num = Number(pageResult.total)
            return isNaN(num) ? 0 : Math.max(0, num)
          })()
      paginationItemCount.value = total
    } else {
      paginationItemCount.value = 0
    }
  } catch (error: any) {
    const errorMsg = handleApiError(error, '加载页面列表')
    message.error(errorMsg)
    if (tableData.value.length === 0) {
      paginationItemCount.value = 0
    }
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  paginationPage.value = page
  loadData()
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  loadData()
}

const handleSearch = () => {
  paginationPage.value = 1
  loadData()
}

const handleResetSearch = () => {
  searchKeyword.value = ''
  paginationPage.value = 1
  loadData()
}

const handleCreate = () => {
  // 替换当前标签页
  tabsStore.replaceTab('/page-manage', {
    key: '/page-designer/new',
    title: '新建页面',
    closable: true
  })
  router.replace('/page-designer/new')
}

const handleView = (row: PageDefinition) => {
  // 大屏页面使用大屏预览路由
  const isBigscreen = row.layoutMode === 'bigscreen'
  const viewPath = isBigscreen ? `/bigscreen-view/${row.id}` : `/page-view/${row.id}`
  tabsStore.addTab({
    key: viewPath,
    title: row.pageName || '页面查看',
    closable: true
  })
  router.push(viewPath)
}

const handleEdit = (row: PageDefinition) => {
  // 替换当前标签页
  tabsStore.replaceTab('/page-manage', {
    key: `/page-designer/${row.id}`,
    title: '编辑页面',
    closable: true
  })
  router.replace(`/page-designer/${row.id}`)
}

const handleDelete = async (row: PageDefinition) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除页面 "${row.pageName}" 吗？删除后无法恢复！`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deletePageDefinition(row.id!)
        message.success('删除成功')
        await loadData()
      } catch (error: any) {
        const errorMsg = handleApiError(error, '删除页面', '删除页面失败，请稍后重试')
        message.error(errorMsg)
      }
    }
  })
}

const loadMenus = async () => {
  try {
    let res: any
    try {
      res = await getAllMenus(true)
    } catch {
      res = await getVisibleMenus()
    }
    if (res && typeof res === 'object') {
      if (Array.isArray((res as any).data)) {
        menuList.value = (res as any).data
      } else if (Array.isArray(res)) {
        menuList.value = res as Menu[]
      } else {
        menuList.value = []
      }
    } else {
      menuList.value = []
    }
  } catch (error: any) {
    console.warn('菜单加载失败，不影响主功能', error)
    menuList.value = []
  }
}

// 获取页面对应的菜单（通过 routePath 匹配）
const getPageMenu = (pageId: number): Menu | undefined => {
  const routePath = `/page-view/${pageId}`
  
  // 扁平化菜单列表
  const flattenMenus = (items: Menu[]): Menu[] => {
    const result: Menu[] = []
    const flatten = (list: Menu[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          flatten(item.children)
        }
      })
    }
    flatten(items)
    return result
  }
  
  const flatList = flattenMenus(menuList.value)
  return flatList.find(m => m.routePath === routePath)
}

// 删除菜单
const handleDeleteMenu = async (menuId: number) => {
  try {
    await deleteMenu(menuId)
    message.success('菜单删除成功')
    await loadMenus()
  } catch (error: any) {
    const errorMsg = handleApiError(error, '删除菜单', '删除菜单失败')
    message.error(errorMsg)
  }
}

// 构建菜单树选项（只显示目录类型，或有子菜单的菜单）
const menuTreeOptions = computed(() => {
  if (!menuList.value || menuList.value.length === 0) {
    return []
  }
  
  // 先找出所有有子菜单的菜单ID
  const parentIds = new Set(menuList.value.filter(m => m && m.parentId).map(m => m.parentId))
  
  const buildTree = (menus: Menu[], parentId: number = 0): any[] => {
    return menus
      .filter(m => {
        if (!m || m.parentId !== parentId) return false
        // 目录类型可以选择
        if (m.menuType === 'directory') return true
        // 有子菜单的也可以选择（兼容历史数据）
        if (parentIds.has(m.id)) return true
        return false
      })
      .map(m => {
        const children = buildTree(menus, m.id)
        return {
          label: m.menuName || `菜单${m.id}`,
          value: m.id,
          key: m.id,
          children: children.length > 0 ? children : undefined
        }
      })
  }
  
  try {
    return buildTree(menuList.value)
  } catch (error) {
    console.error('构建菜单树失败:', error)
    return []
  }
})

// 获取指定父菜单下的最大排序值
const getMaxSortOrder = (parentId: number): number => {
  const children = menuList.value.filter(m => m.parentId === parentId)
  if (children.length === 0) return 0
  return Math.max(...children.map(m => m.sortOrder || 0))
}

// 监听父菜单变化，自动更新排序值
watch(() => menuForm.value.parentId, (newParentId) => {
  if (newParentId !== undefined) {
    menuForm.value.sortOrder = getMaxSortOrder(newParentId || 0) + 1
  }
})

const handleCreateMenu = async (page: PageDefinition) => {
  if (!page || !page.id) {
    message.error('页面信息无效')
    return
  }
  
  currentPage.value = page
  
  // 根据布局模式确定路由路径和组件路径
  const layoutMode = page.layoutMode || activeLayoutMode.value
  let routePath = `/page-view/${page.id}`
  let componentPath = '@/views/PageView.vue'
  
  if (layoutMode === 'bigscreen') {
    routePath = `/bigscreen-view/${page.id}`
    componentPath = '@/views/BigscreenView.vue'
  } else if (layoutMode === 'mobile') {
    routePath = `/mobile-view/${page.id}`
    componentPath = '@/views/MobileView.vue'
  }
  
  // 检查是否已存在该页面的菜单
  const existingMenu = menuList.value.find(m => 
    m.routePath === routePath || 
    m.menuCode === `page_${page.pageCode}`
  )
  
  if (existingMenu) {
    dialog.warning({
      title: '菜单已存在',
      content: `页面 "${page.pageName}" 已经存在对应的菜单，是否继续创建新菜单？`,
      positiveText: '继续创建',
      negativeText: '取消',
      onPositiveClick: () => {
        menuForm.value = {
          menuName: page.pageName || `页面${page.id}`,
          menuCode: `page_${page.pageCode || page.id}_${Date.now()}`,
          parentId: 0,
          menuType: 'menu',
          routePath,
          componentPath,
          icon: '',
          sortOrder: getMaxSortOrder(0) + 1,
          isVisible: 1,
          permissionCode: `page:view:${page.pageCode || page.id}`
        }
        showMenuModal.value = true
      }
    })
  } else {
    menuForm.value = {
      menuName: page.pageName || `页面${page.id}`,
      menuCode: `page_${page.pageCode || page.id}`,
      parentId: 0,
      menuType: 'menu',
      routePath,
      componentPath,
      icon: '',
      sortOrder: getMaxSortOrder(0) + 1,
      isVisible: 1,
      permissionCode: `page:view:${page.pageCode || page.id}`
    }
    showMenuModal.value = true
  }
}

const handleSubmitMenu = async () => {
  if (!menuFormRef.value || !currentPage.value) {
    message.warning('请先选择页面')
    return
  }
  
  try {
    // 验证表单
    await menuFormRef.value.validate()
    
    // 验证菜单编码是否已存在
    const existingMenu = menuList.value.find(m => m.menuCode === menuForm.value.menuCode)
    if (existingMenu) {
      message.warning('菜单编码已存在，请使用其他编码')
      return
    }
    
    creatingMenu.value = true
    
    // 确保 parentId 是数字
    const submitData: MenuForm = {
      ...menuForm.value,
      parentId: menuForm.value.parentId || 0,
      sortOrder: menuForm.value.sortOrder || 0,
      isVisible: menuForm.value.isVisible || 1
    }
    
    await createMenu(submitData)
    message.success('菜单创建成功！')
    showMenuModal.value = false
    
    // 重置表单
    menuForm.value = {
      menuName: '',
      menuCode: '',
      parentId: 0,
      menuType: 'menu',
      routePath: '',
      componentPath: '@/views/PageView.vue',
      icon: '',
      sortOrder: 0,
      isVisible: 1,
      permissionCode: ''
    }
    currentPage.value = null
    
    // 重新加载菜单列表
    await loadMenus()
  } catch (error: any) {
    // 如果是验证错误，不显示错误消息（表单会显示）
    if (error && error.length && Array.isArray(error)) {
      // 这是表单验证错误，不需要额外处理
      return
    }
    const errorMsg = handleApiError(error, '创建菜单', '创建菜单失败')
    message.error(errorMsg)
  } finally {
    creatingMenu.value = false
  }
}
</script>

<style scoped>
.page-manage-container {
  padding: 0;
}

/* ==================== 统计卡片区域 ==================== */
.page-header-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eef0f4;
  transition: all 0.3s ease;
  cursor: default;
}
.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-item-primary .stat-icon {
  background: linear-gradient(135deg, #e8f0fe 0%, #d4e4fd 100%);
  color: #2563eb;
}
.stat-item-success .stat-icon {
  background: linear-gradient(135deg, #e6f9ef 0%, #d1f2e1 100%);
  color: #16a34a;
}
.stat-item-warning .stat-icon {
  background: linear-gradient(135deg, #fff7e6 0%, #ffefd1 100%);
  color: #ea8c00;
}
.stat-item-info .stat-icon {
  background: linear-gradient(135deg, #e8f4fd 0%, #d1eafc 100%);
  color: #0891b2;
}
.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
  letter-spacing: -0.5px;
  font-family: 'DIN', 'Roboto Mono', system-ui, sans-serif;
}
.stat-label {
  font-size: 13px;
  color: #8c8c9a;
  font-weight: 400;
}

/* ==================== 布局模式标签页 ==================== */
.layout-mode-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  padding: 5px;
  background: #f0f2f6;
  border-radius: 12px;
  width: fit-content;
}

.layout-tab {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 20px;
  border-radius: 9px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.25s ease;
  user-select: none;
  white-space: nowrap;
}

.layout-tab:hover {
  color: #333;
  background: rgba(255, 255, 255, 0.6);
}

.layout-tab.active {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  font-weight: 600;
}
.layout-tab.active.mode-desktop {
  color: #2563eb;
}
.layout-tab.active.mode-mobile {
  color: #16a34a;
}
.layout-tab.active.mode-bigscreen {
  color: #7c3aed;
}

/* ==================== 主卡片区域 ==================== */
.main-card {
  border-radius: 12px;
  overflow: visible;
}
.card-header-custom {
  display: flex;
  align-items: center;
  gap: 14px;
}
.header-icon-wrapper {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.header-icon-desktop {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #2563eb;
}
.header-icon-mobile {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #16a34a;
}
.header-icon-bigscreen {
  background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%);
  color: #7c3aed;
}
.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}
.header-subtitle {
  font-size: 12px;
  color: #8c8c9a;
  font-weight: 400;
}

/* ==================== 工具栏 ==================== */
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 0;
}

/* ==================== 空状态 ==================== */
.empty-state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
}
.empty-state-icon {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.empty-desktop {
  background: linear-gradient(135deg, #e8f0fe 0%, #d4e4fd 100%);
  color: #2563eb;
}
.empty-mobile {
  background: linear-gradient(135deg, #e6f9ef 0%, #d1f2e1 100%);
  color: #16a34a;
}
.empty-bigscreen {
  background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%);
  color: #7c3aed;
}
.empty-state-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
}
.empty-state-desc {
  font-size: 14px;
  color: #8c8c9a;
  text-align: center;
  max-width: 400px;
  line-height: 1.6;
}

/* ==================== 分页区域 ==================== */
.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f2f6;
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

/* ==================== 表格样式 ==================== */
:deep(.n-data-table-td) {
  padding: 12px 16px;
}

:deep(.n-data-table .n-data-table-th) {
  font-weight: 600;
  font-size: 13px;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.page-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
  min-width: 172px;
  white-space: nowrap;
}

.page-action-btn {
  min-width: 56px;
  height: 28px;
  padding: 0 12px;
  border-radius: 7px;
  font-weight: 500;
}

.page-action-design {
  color: #334155;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.page-action-design:hover {
  color: #2563eb;
  background: #eef4ff;
  border-color: #bfdbfe;
}

.page-action-more {
  color: #64748b;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #f8fafc;
}

.page-action-more:hover {
  color: #2563eb;
  background: #eef4ff;
}

:deep(.custom-table .n-data-table-td--fixed-right),
:deep(.custom-table .n-data-table-th--fixed-right) {
  background: #fff;
}

:deep(.custom-table .n-data-table-td--fixed-right::before),
:deep(.custom-table .n-data-table-th--fixed-right::before) {
  box-shadow: -8px 0 18px rgba(15, 23, 42, 0.05);
}

:deep(.n-button--small-type) {
  font-size: 13px;
}

/* 图标选择样式 */
.icon-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 8px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.icon-select-item:hover {
  border-color: #18a058;
  background: #f0faf4;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.15);
}

.icon-select-item.icon-selected {
  border-color: #18a058;
  background: linear-gradient(135deg, #f0faf4 0%, #e8f5e9 100%);
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.2);
}

.icon-select-item .n-icon {
  color: #666;
}

.icon-select-item:hover .n-icon,
.icon-select-item.icon-selected .n-icon {
  color: #18a058;
}

/* 图标选择器容器 */
.icon-picker-container {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  width: 100%;
}

.icon-picker-selected {
  margin-bottom: 12px;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
  padding: 4px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background: #fafafa;
}

.icon-placeholder {
  width: 28px;
  height: 28px;
  background: #ddd;
  border-radius: 4px;
}

.icon-empty {
  text-align: center;
  padding: 20px;
  color: #999;
}

.icon-count {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

/* ==================== 响应式布局 ==================== */
@media (max-width: 1200px) {
  .page-header-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .page-header-stats {
    grid-template-columns: 1fr;
  }
}



</style>

<style>
/* PageManage 深色模式（非 scoped） */

/* 统计卡片深色 */
html.dark .stat-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .stat-item:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2) !important;
}
html.dark .stat-item-primary .stat-icon {
  background: linear-gradient(135deg, #1e3a5f 0%, #1a3356 100%) !important;
  color: #60a5fa !important;
}
html.dark .stat-item-success .stat-icon {
  background: linear-gradient(135deg, #1a3a2a 0%, #163d28 100%) !important;
  color: #34d399 !important;
}
html.dark .stat-item-warning .stat-icon {
  background: linear-gradient(135deg, #3d3520 0%, #3a3018 100%) !important;
  color: #fbbf24 !important;
}
html.dark .stat-item-info .stat-icon {
  background: linear-gradient(135deg, #1a3340 0%, #162e3a 100%) !important;
  color: #22d3ee !important;
}
html.dark .stat-value {
  color: #e2e8f0 !important;
}
html.dark .stat-label {
  color: #64748b !important;
}

/* 卡片头部深色 */
html.dark .header-title {
  color: #e2e8f0 !important;
}
html.dark .header-subtitle {
  color: #64748b !important;
}
html.dark .header-icon-desktop {
  background: linear-gradient(135deg, #1e3a5f 0%, #1a3356 100%) !important;
  color: #60a5fa !important;
}
html.dark .header-icon-mobile {
  background: linear-gradient(135deg, #1a3a2a 0%, #163d28 100%) !important;
  color: #34d399 !important;
}
html.dark .header-icon-bigscreen {
  background: linear-gradient(135deg, #2e1a5f 0%, #271656 100%) !important;
  color: #a78bfa !important;
}

/* 空状态深色 */
html.dark .empty-state-title {
  color: #e2e8f0 !important;
}
html.dark .empty-state-desc {
  color: #64748b !important;
}
html.dark .empty-desktop {
  background: linear-gradient(135deg, #1e3a5f 0%, #1a3356 100%) !important;
  color: #60a5fa !important;
}
html.dark .empty-mobile {
  background: linear-gradient(135deg, #1a3a2a 0%, #163d28 100%) !important;
  color: #34d399 !important;
}
html.dark .empty-bigscreen {
  background: linear-gradient(135deg, #2e1a5f 0%, #271656 100%) !important;
  color: #a78bfa !important;
}

/* 分页深色 */
html.dark .pagination-wrapper {
  border-top-color: #334155 !important;
}

/* 操作列深色 */
html.dark .page-action-design,
html.dark .page-action-more {
  color: #cbd5e1 !important;
  background: #1f2937 !important;
  border-color: #334155 !important;
}

html.dark .page-action-design:hover,
html.dark .page-action-more:hover {
  color: #93c5fd !important;
  background: rgba(59, 130, 246, 0.14) !important;
  border-color: rgba(96, 165, 250, 0.35) !important;
}

html.dark .custom-table .n-data-table-td--fixed-right,
html.dark .custom-table .n-data-table-th--fixed-right {
  background: #1e293b !important;
}

/* 图标选择器深色 */
html.dark .icon-picker-container {
  border-color: #334155 !important;
  background: #1e293b !important;
}
html.dark .icon-picker-selected {
  background: #334155 !important;
  color: #e2e8f0 !important;
}
html.dark .icon-grid {
  border-color: #334155 !important;
  background: #0f172a !important;
}
html.dark .icon-select-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .icon-select-item:hover {
  background: #1a3a2a !important;
  border-color: #10b981 !important;
}
html.dark .icon-select-item.icon-selected {
  background: linear-gradient(135deg, #1a3a2a 0%, #1e3a30 100%) !important;
  border-color: #10b981 !important;
}
html.dark .icon-select-item .n-icon {
  color: #94a3b8 !important;
}
html.dark .icon-select-item:hover .n-icon,
html.dark .icon-select-item.icon-selected .n-icon {
  color: #10b981 !important;
}
html.dark .icon-placeholder {
  background: #475569 !important;
}
html.dark .icon-empty,
html.dark .icon-count {
  color: #64748b !important;
}

/* 布局模式标签页深色模式 */
html.dark .layout-mode-tabs {
  background: #1e293b !important;
}
html.dark .layout-tab {
  color: #94a3b8 !important;
}
html.dark .layout-tab:hover {
  color: #e2e8f0 !important;
  background: rgba(255, 255, 255, 0.05) !important;
}
html.dark .layout-tab.active {
  background: #334155 !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3) !important;
}
html.dark .layout-tab.active.mode-desktop {
  color: #60a5fa !important;
}
html.dark .layout-tab.active.mode-mobile {
  color: #34d399 !important;
}
html.dark .layout-tab.active.mode-bigscreen {
  color: #a78bfa !important;
}
</style>
