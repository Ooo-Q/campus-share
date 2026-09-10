import request from './request'

export interface UserReport {
  id: number
  reportedUserId: number
  reportedUsername?: string
  userId: number
  reason: string
  status: string
  reviewReply?: string
  cancelled: boolean
  createdAt: string
  reviewedAt?: string
  updatedAt?: string
}

export function createUserReport(reportedUserId: number, reason: string) {
  return request.post<{ success: boolean; data: UserReport }>('/user-reports', {
    reportedUserId,
    reason,
  })
}

export function getMyUserReports(status?: string) {
  return request.get<{ success: boolean; data: UserReport[] }>('/user-reports/my', {
    params: status ? { status } : {},
  })
}
