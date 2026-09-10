import request from './request'

export interface Resource {
  id: number
  title: string
  categoryId: number
  categoryName?: string
  description: string
  fileUrl: string
  ownerId: number
  ownerName: string
  likeCount: number
  downloadCount: number
  viewCount: number
  favoriteCount?: number
  allowDownload: boolean
  createdAt: string
  visibility?: string
}

export interface PageResponse<T> {
  success: boolean
  data: {
    total: number
    page: number
    size: number
    records: T[]
  }
}

export interface Comment {
  id: number
  resourceId: number
  rootId: number
  parentId: number | null
  userId: number
  displayName: string
  avatar?: string
  replyToName?: string
  content: string
  status: string
  createdAt: string
  children?: Comment[]
}

export function fetchResources(params: { page?: number; size?: number; categoryId?: number; keyword?: string }) {
  return request.get<PageResponse<Resource>>('/resources', { params })
}

export function fetchResourceDetail(id: number) {
  return request.get<{ success: boolean; data: Resource }>(`/resources/${id}`)
}

export function createResource(payload: {
  title: string
  categoryId: number
  description: string
  fileUrl: string
  allowDownload?: boolean
}) {
  return request.post('/resources', payload)
}

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ success: boolean; data: string }>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function likeResource(id: number) {
  return request.post<{ success: boolean; data: Resource; message?: string }>(`/resources/${id}/like`)
}

export function getLikeStatus(id: number) {
  return request.get<{ success: boolean; data: boolean }>(`/resources/${id}/like/status`)
}

export function updateResource(id: number, payload: {
  title: string
  categoryId: number
  description: string
  fileUrl?: string | null
  allowDownload?: boolean
  visibility?: string
}) {
  return request.put<{ success: boolean; data: Resource }>(`/resources/${id}`, payload)
}

export function updateResourceVisibility(id: number, visibility: string) {
  return request.put<{ success: boolean; data: Resource }>(`/resources/${id}/visibility`, null, {
    params: { visibility },
  })
}

export function fetchComments(resourceId: number, params: { page?: number; size?: number }) {
  return request.get<{ success: boolean; data: { total: number; page: number; size: number; records: Comment[] } }>(
    `/resources/${resourceId}/comments`,
    { params }
  )
}

export function createComment(resourceId: number, payload: { content: string; parentId?: number; anonymous?: boolean }) {
  return request.post<{ success: boolean; data: Comment }>(`/resources/${resourceId}/comments`, payload)
}

export function deleteComment(resourceId: number, commentId: number) {
  return request.delete<{ success: boolean; data: any }>(`/resources/${resourceId}/comments/${commentId}`)
}
