import request from './request'

export interface Friend {
  id: number
  userId: number
  username: string
  nickname: string
  avatar?: string
  status: string
  createdAt: string
  hasFriendRelation?: boolean
}

export interface FriendRequest {
  id: number
  fromUserId: number
  fromUsername: string
  fromNickname: string
  fromAvatar?: string
  toUserId?: number
  toUsername?: string
  toNickname?: string
  toAvatar?: string
  message?: string
  status: string
  createdAt: string
  updatedAt?: string
  isFromMe?: boolean
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  avatar?: string
  gender?: string
  email?: string
  phone?: string
  createdAt: string
  resourceCount: number
  favoriteCount?: number
  likeCount?: number
  friendStatus: 'NONE' | 'PENDING' | 'FRIEND' | 'BLOCKED' | 'SELF'
  friendRequestId?: number
}

export function getFriendList() {
  return request.get<{ success: boolean; data: Friend[] }>('/friends')
}

export function getBlockedList() {
  return request.get<{ success: boolean; data: Friend[] }>('/friends/blocked')
}

export function sendFriendRequest(toUserId: number, message?: string) {
  return request.post<{ success: boolean; message?: string }>('/friends/requests', {
    toUserId,
    message,
  })
}

export function getAllRequests() {
  return request.get<{ success: boolean; data: FriendRequest[] }>('/friends/requests/all')
}

export function deleteRequest(requestId: number) {
  return request.delete<{ success: boolean; message?: string }>(`/friends/requests/${requestId}`)
}

export function acceptFriendRequest(requestId: number) {
  return request.put<{ success: boolean; message?: string }>(`/friends/requests/${requestId}/accept`)
}

export function rejectFriendRequest(requestId: number) {
  return request.put<{ success: boolean; message?: string }>(`/friends/requests/${requestId}/reject`)
}

export function deleteFriend(friendId: number) {
  return request.delete<{ success: boolean; message?: string }>(`/friends/${friendId}`)
}

export function blockFriend(friendId: number) {
  return request.post<{ success: boolean; message?: string }>(`/friends/${friendId}/block`)
}

export function unblockFriend(friendId: number) {
  return request.delete<{ success: boolean; message?: string }>(`/friends/${friendId}/block`)
}

export function searchUsers(keyword: string, page = 1, size = 10) {
  return request.get<{ success: boolean; data: UserProfile[] }>('/friends/search', {
    params: { keyword, page, size },
  })
}

export function getUserProfile(userId: number) {
  return request.get<{ success: boolean; data: UserProfile }>(`/users/${userId}/profile`)
}

