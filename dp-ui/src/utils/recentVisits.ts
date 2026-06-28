/**
 * 移动端最近访问记录工具
 * 用于在 MobileDashboard 的"最近访问"区域展示
 */

const STORAGE_KEY = 'dp_recent_visits'
const MAX_ITEMS = 10

interface RecentVisitRecord {
  path: string
  title: string
  time: number
}

/**
 * 保存一条最近访问记录
 */
export function saveRecentVisit(path: string, title: string) {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    let list: RecentVisitRecord[] = raw ? JSON.parse(raw) : []

    // 去重：移除同路径的旧记录
    list = list.filter(item => item.path !== path)

    // 新记录插入顶部
    list.unshift({ path, title, time: Date.now() })

    // 保留最近 N 条
    if (list.length > MAX_ITEMS) {
      list = list.slice(0, MAX_ITEMS)
    }

    localStorage.setItem(STORAGE_KEY, JSON.stringify(list))
  } catch { /* ignore storage errors */ }
}
