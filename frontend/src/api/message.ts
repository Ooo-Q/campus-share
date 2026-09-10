import request from './request'

export interface Conversation {
  userId: number
  username: string
  nickname: string
  avatar?: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount: number
}

export interface Message {
  id: number
  fromUserId: number
  fromUsername: string
  fromNickname: string
  fromAvatar?: string
  toUserId: number
  content: string
  isRead: boolean
  createdAt: string
}

export function getConversations() {
  return request.get<{ success: boolean; data: Conversation[] }>('/messages/conversations')
}

export function getMessages(userId: number, page = 1, size = 20, silent = false) {
  return request.get<{ success: boolean; data: Message[] }>(`/messages/conversations/${userId}`, {
    params: { page, size },
    silent,
  } as any)
}

export function sendMessage(toUserId: number, content: string) {
  return request.post<{ success: boolean; data: Message }>('/messages', {
    toUserId,
    content,
  })
}

export function markAsRead(userId: number) {
  return request.put<{ success: boolean; message?: string }>(`/messages/conversations/${userId}/read`)
}

export function getUnreadCount() {
  return request.get<{ success: boolean; data: number }>('/messages/unread-count')
}

export function deleteConversation(userId: number) {
  return request.delete<{ success: boolean; message?: string }>(`/messages/conversations/${userId}`)
}
