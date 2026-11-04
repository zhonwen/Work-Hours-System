import request from './request'

export interface LeaveTypeInfo {
  id: number
  name: string
  code: string
  sort: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface LeaveTypeQuery {
  name?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export const getLeaveTypeList = (params: LeaveTypeQuery) => {
  return request.get('/leave-types', { params })
}

export const getEnabledLeaveTypes = (): Promise<LeaveTypeInfo[]> => {
  return request.get('/leave-types/enabled')
}

export const createLeaveType = (data: Partial<LeaveTypeInfo>) => {
  return request.post('/leave-types', data)
}

export const updateLeaveType = (data: Partial<LeaveTypeInfo> & { id: number }) => {
  return request.put('/leave-types', data)
}

export const deleteLeaveType = (id: number) => {
  return request.delete(`/leave-types/${id}`)
}
