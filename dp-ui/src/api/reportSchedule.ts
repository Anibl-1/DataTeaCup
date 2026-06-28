import request from './request'

export interface ReportSchedule {
  id?: number
  reportId: number
  scheduleName: string
  cronExpression: string
  recipients: string
  channels?: string | undefined
  emailChannelId?: number | undefined
  wecomChannelId?: number | undefined
  dingtalkChannelId?: number | undefined
  attachExcel?: number | undefined
  /** 附件格式: excel/pdf */
  attachFormat?: string | undefined
  filterParams?: string | undefined
  /** 日期函数参数JSON */
  dateParams?: string | undefined
  isEnabled?: number | undefined
  lastRunTime?: string
  lastRunStatus?: string
  createBy?: number
  createTime?: string
  updateTime?: string
}

export const reportScheduleApi = {
  list() {
    return request.get<ReportSchedule[]>('/report/schedule/list')
  },

  getById(id: number) {
    return request.get<ReportSchedule>(`/report/schedule/${id}`)
  },

  getByReportId(reportId: number) {
    return request.get<ReportSchedule[]>(`/report/schedule/by-report/${reportId}`)
  },

  create(schedule: ReportSchedule) {
    return request.post<ReportSchedule>('/report/schedule', schedule)
  },

  update(id: number, schedule: ReportSchedule) {
    return request.put<ReportSchedule>(`/report/schedule/${id}`, schedule)
  },

  delete(id: number) {
    return request.delete(`/report/schedule/${id}`)
  },

  trigger(id: number) {
    return request.post(`/report/schedule/${id}/trigger`)
  }
}
