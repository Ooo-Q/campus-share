import request from './request'
import type { UserInfo } from '../stores/user'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
  email?: string
  phone?: string
}

export interface AuthResponse {
  success: boolean
  message: string
  data: {
    userId: number
    username: string
    nickname?: string
    role: string
    avatar?: string
    token: string
  }
}

export function login(payload: LoginPayload) {
  return request.post<AuthResponse>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return request.post<AuthResponse>('/auth/register', payload)
}

export function getProfile() {
  return request.get<{ success: boolean; data: UserInfo }>('/auth/me')
}

export interface UpdateProfilePayload {
  nickname?: string
  avatar?: string
  gender?: string
  phone?: string
  email?: string
}

export function updateProfile(payload: UpdateProfilePayload) {
  return request.put<{ success: boolean; data: UserInfo }>('/auth/me', payload)
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export function changePassword(payload: ChangePasswordPayload) {
  return request.post<{ success: boolean; message: string }>('/auth/me/password', payload)
}

export interface ChangeUsernamePayload {
  newUsername: string
}

export function changeUsername(payload: ChangeUsernamePayload) {
  return request.post<{ success: boolean; data: UserInfo }>('/auth/me/username', payload)
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{ success: boolean; data: string }>('/files/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

