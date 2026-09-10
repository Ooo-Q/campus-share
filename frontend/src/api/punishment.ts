import request from './request'

export interface UserPunishment {
  id: number
  userId: number
  reportId?: number
  resourceId?: number
  resourceTitle?: string
  reportedUserId?: number
  reportedUsername?: string
  punishmentType: string
  reason?: string
  duration?: number
  startDate: string
  endDate?: string
  status: string
  adminId?: number
  createdAt: string
  updatedAt?: string
}

export function createUserPunishment(userId: number, type: string, duration: number | null, reason: string) {
  return request.post<{ success: boolean; data: UserPunishment }>('/punishments/create', {
    userId,
    type,
    duration,
    reason,
  })
}

export function createResourcePunishment(
  userId: number,
  resourceId: number,
  resourceTitle: string,
  type: string,
  duration: number | null,
  reason: string
) {
  return request.post<{ success: boolean; data: UserPunishment }>('/punishments/create-for-resource', {
    userId,
    resourceId,
    resourceTitle,
    type,
    duration,
    reason,
  })
}

export function getMyPunishments(status?: string) {
  return request.get<{ success: boolean; data: UserPunishment[] }>('/punishments/my', {
    params: status ? { status } : {},
  })
}
