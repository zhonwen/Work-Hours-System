import request from './request'
import type { PageResult, ApiResponse } from '@/types'

/** 工时查看范围：SELF 仅自己（默认）/ ALL 全部 / USER 指定人 */
export type WorkHourViewScope = 'SELF' | 'ALL' | 'USER'

// 工时查询参数
export interface WorkHourQuery {
  /** 默认 SELF；ALL/USER 需 WORKHOUR_VIEW_ALL */
  viewScope?: WorkHourViewScope
  userId?: number
  projectId?: number
  taskId?: number
  startDate?: string
  endDate?: string
  status?: string
  pageNum?: number
  pageSize?: number
}

/** 工时记录类型 */
export type WorkHourRecordType = 'NORMAL' | 'LEAVE'

/** 是否为请假记录（不计入工时统计） */
export const isLeaveRecord = (wh: { recordType?: WorkHourRecordType | string | number }): boolean => {
  const t = wh?.recordType
  return t === 'LEAVE' || t === 2
}

/** 累加正常工时（排除请假） */
export const sumWorkHours = (list: Array<{ hours?: number; recordType?: WorkHourRecordType | string | number }>): number => {
  return list
    .filter(wh => !isLeaveRecord(wh))
    .reduce((sum, wh) => sum + Number(wh.hours || 0), 0)
}

// 工时信息响应
export interface WorkHourInfo {
  id: number
  userId: number
  projectId?: number
  taskId?: number
  workDate: string
  hours: number
  content: string
  recordType?: WorkHourRecordType
  leaveTypeId?: number
  leaveTypeName?: string
  status: string
  createUserId?: number
  updateUserId?: number
  createTime?: string
  updateTime?: string
  userName?: string
  projectName?: string
  taskName?: string
}

// 创建工时请求
export interface WorkHourCreateReq {
  projectId?: number
  taskId?: number
  workDate: string
  hours: number
  content?: string
  recordType?: WorkHourRecordType
  leaveTypeId?: number
  status?: string
}

// 更新工时请求
export interface WorkHourUpdateReq {
  id: number
  projectId?: number
  taskId?: number
  workDate: string
  hours: number
  content?: string
  recordType?: WorkHourRecordType
  leaveTypeId?: number
  status?: string
}

// API函数（带类型注解）
export const getWorkHourList = (data: WorkHourQuery): Promise<ApiResponse<PageResult<WorkHourInfo>>> => {
  return request.post('/work-hours/query', data)
}

export const getWorkHourDetail = (id: number): Promise<ApiResponse<WorkHourInfo>> => {
  return request.get(`/work-hours/${id}`)
}

export const createWorkHour = (data: WorkHourCreateReq): Promise<ApiResponse<WorkHourInfo>> => {
  return request.post('/work-hours', data)
}

// 批量创建工时
export const createWorkHoursBatch = (data: WorkHourCreateReq[]): Promise<ApiResponse<WorkHourInfo[]>> => {
  return request.post('/work-hours/batch', data)
}

export const updateWorkHour = (data: WorkHourUpdateReq): Promise<ApiResponse<WorkHourInfo>> => {
  return request.put('/work-hours', data)
}

export const deleteWorkHour = (id: number): Promise<ApiResponse<string>> => {
  return request.delete(`/work-hours/${id}`)
}

export const getWorkHoursByUserId = (userId: number): Promise<ApiResponse<WorkHourInfo[]>> => {
  return request.get(`/work-hours/user/${userId}`)
}

/** 按项目查工时：后端 PageResult，拦截器返回 { list, total } */
export const getWorkHoursByProjectId = (
  projectId: number
): Promise<{ list: WorkHourInfo[]; total: number }> => {
  return request.get(`/work-hours/project/${projectId}`)
}

export const getUserWorkHourStatistics = (
  userId: number,
  startDate?: string,
  endDate?: string
): Promise<ApiResponse<{ totalHours: number }>> => {
  return request.get(`/statistics/user/${userId}`, {
    params: { startDate, endDate }
  })
}

export const getProjectWorkHourStatistics = (
  projectId: number,
  startDate?: string,
  endDate?: string
): Promise<ApiResponse<{ totalHours: number }>> => {
  return request.get(`/statistics/project/${projectId}`, {
    params: { startDate, endDate }
  })
}

export const getWorkHourTrend = (params: {
  projectId?: number
  userId?: number
  startDate: string
  endDate: string
}): Promise<ApiResponse<unknown[]>> => {
  return request.get('/statistics/trend', { params })
}

// 获取任务剩余工时
export const getTaskRemainingHours = (taskId: number): Promise<ApiResponse<number>> => {
  return request.get(`/tasks/${taskId}/remaining-hours`)
}
