/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图标选择器数据 - 从 ChartManage.vue 提取
 * 供 ChartManage.vue 和 ReportManage.vue 等需要图标选择的页面复用
 */
import {
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
  ReceiptOutline, CalculatorOutline, LockClosedOutline, LockOpenOutline, KeyOutline,
  ShieldCheckmarkOutline, EyeOutline, EyeOffOutline, PowerOutline, BatteryFullOutline,
  WifiOutline, NotificationsOutline, MailOutline, CalendarOutline, TimeOutline, WarningOutline,
  InformationCircleOutline, CheckmarkCircleOutline, CloseCircleOutline, FilterOutline,
  ColorPaletteOutline, ColorFilterOutline, BrushOutline, ColorFillOutline, ShapesOutline,
  SquareOutline, AtCircleOutline, TriangleOutline, EllipseOutline, RadioButtonOnOutline,
  RadioButtonOffOutline, CheckboxOutline, ToggleOutline, LinkOutline, ShareOutline,
  StarOutline, HeartOutline, ThumbsUpOutline, ThumbsDownOutline,
  SparklesOutline, GitCommitOutline, CodeWorkingOutline, BulbOutline
} from '@vicons/ionicons5'

/** 图标名称到组件的映射 */
export const iconMap: Record<string, any> = {
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
  ReceiptOutline, CalculatorOutline, LockClosedOutline, LockOpenOutline, KeyOutline,
  ShieldCheckmarkOutline, EyeOutline, EyeOffOutline, PowerOutline, BatteryFullOutline,
  WifiOutline, NotificationsOutline, MailOutline, CalendarOutline, TimeOutline, WarningOutline,
  InformationCircleOutline, CheckmarkCircleOutline, CloseCircleOutline, FilterOutline,
  ColorPaletteOutline, ColorFilterOutline, BrushOutline, ColorFillOutline, ShapesOutline,
  SquareOutline, AtCircleOutline, TriangleOutline, EllipseOutline, RadioButtonOnOutline,
  RadioButtonOffOutline, CheckboxOutline, ToggleOutline, LinkOutline, ShareOutline,
  StarOutline, HeartOutline, ThumbsUpOutline, ThumbsDownOutline,
  SparklesOutline, GitCommitOutline, CodeWorkingOutline, BulbOutline
}

/** 图标分类 */
export const iconCategories = [
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

/** 所有图标选项（带分类） */
export const allIconOptions = [
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
  { label: '点踩', value: 'ThumbsDownOutline', category: 'ui' },
  // AI & 开发
  { label: '闪光', value: 'SparklesOutline', category: 'common' },
  { label: '灯泡', value: 'BulbOutline', category: 'common' },
  { label: 'Git提交', value: 'GitCommitOutline', category: 'data' },
  { label: '代码', value: 'CodeWorkingOutline', category: 'data' }
]

/** 根据图标名称获取图标组件 */
export const getIconComponent = (iconName?: string): any => {
  if (!iconName) return undefined
  return iconMap[iconName] || undefined
}

/** 根据图标值获取图标标签 */
export const getIconLabel = (iconValue: string): string => {
  const icon = allIconOptions.find(opt => opt.value === iconValue)
  return icon ? icon.label : iconValue
}
