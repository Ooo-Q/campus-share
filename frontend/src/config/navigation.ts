import type { Component } from 'vue'
import {
  HomeOutline,
  LibraryOutline,
  CloudUploadOutline,
  PeopleOutline,
  ChatbubblesOutline,
  PersonOutline,
  SettingsOutline,
  SchoolOutline,
  FolderOutline,
  FlagOutline,
  MegaphoneOutline,
  AlertCircleOutline,
  DocumentTextOutline,
} from '@vicons/ionicons5'

export type NavItem = {
  key: string
  label: string
  path: string
  icon: Component
  badgeKey?: 'messages' | 'reports' | 'punishments' | 'announcements'
}

export type NavGroup = {
  label?: string
  items: NavItem[]
}

/** Admin IA — governance first, social secondary */
export const adminNav: NavGroup[] = [
  {
    items: [
      { key: 'home', label: '工作台', path: '/admin', icon: HomeOutline },
    ],
  },
  {
    label: '内容',
    items: [
      { key: 'resources', label: '资料管理', path: '/admin/resources', icon: LibraryOutline },
      { key: 'categories', label: '分类管理', path: '/admin/categories', icon: FolderOutline },
      { key: 'announcements', label: '系统公告', path: '/admin/announcements', icon: MegaphoneOutline },
    ],
  },
  {
    label: '治理',
    items: [
      { key: 'students', label: '学生管理', path: '/admin/students', icon: SchoolOutline },
      { key: 'reports', label: '举报处理', path: '/admin/reports', icon: FlagOutline, badgeKey: 'reports' },
    ],
  },
  {
    label: '社交',
    items: [
      { key: 'messages', label: '消息', path: '/admin/messages', icon: ChatbubblesOutline, badgeKey: 'messages' },
      { key: 'friends', label: '好友', path: '/admin/friends', icon: PeopleOutline },
    ],
  },
  {
    label: '账户',
    items: [
      { key: 'profile', label: '个人中心', path: '/admin/profile', icon: PersonOutline },
      { key: 'settings', label: '设置', path: '/admin/settings', icon: SettingsOutline },
    ],
  },
]

/** Student IA — discover & share first */
export const studentNav: NavGroup[] = [
  {
    items: [
      { key: 'home', label: '首页', path: '/student', icon: HomeOutline },
      { key: 'resources', label: '资料广场', path: '/student/resources', icon: LibraryOutline },
      { key: 'upload', label: '上传资料', path: '/student/upload', icon: CloudUploadOutline },
    ],
  },
  {
    label: '社交',
    items: [
      { key: 'messages', label: '消息', path: '/student/messages', icon: ChatbubblesOutline, badgeKey: 'messages' },
      { key: 'friends', label: '好友', path: '/student/friends', icon: PeopleOutline },
    ],
  },
  {
    label: '通知',
    items: [
      { key: 'announcements', label: '系统公告', path: '/student/announcements', icon: MegaphoneOutline, badgeKey: 'announcements' },
      { key: 'reports', label: '我的举报', path: '/student/reports', icon: DocumentTextOutline },
      { key: 'punishments', label: '处罚记录', path: '/student/punishments', icon: AlertCircleOutline, badgeKey: 'punishments' },
    ],
  },
  {
    label: '我的',
    items: [
      { key: 'profile', label: '个人中心', path: '/student/profile', icon: PersonOutline },
      { key: 'settings', label: '设置', path: '/student/settings', icon: SettingsOutline },
    ],
  },
]

/** Mobile bottom tabs — primary destinations only */
export const adminMobileTabs: NavItem[] = [
  { key: 'home', label: '工作台', path: '/admin', icon: HomeOutline },
  { key: 'resources', label: '资料', path: '/admin/resources', icon: LibraryOutline },
  { key: 'reports', label: '举报', path: '/admin/reports', icon: FlagOutline, badgeKey: 'reports' },
  { key: 'messages', label: '消息', path: '/admin/messages', icon: ChatbubblesOutline, badgeKey: 'messages' },
  { key: 'more', label: '更多', path: '/admin/profile', icon: PersonOutline },
]

export const studentMobileTabs: NavItem[] = [
  { key: 'home', label: '首页', path: '/student', icon: HomeOutline },
  { key: 'resources', label: '广场', path: '/student/resources', icon: LibraryOutline },
  { key: 'upload', label: '上传', path: '/student/upload', icon: CloudUploadOutline },
  { key: 'messages', label: '消息', path: '/student/messages', icon: ChatbubblesOutline, badgeKey: 'messages' },
  { key: 'profile', label: '我的', path: '/student/profile', icon: PersonOutline },
]

export function matchNavPath(currentPath: string, itemPath: string): boolean {
  if (itemPath === '/admin' || itemPath === '/student') {
    return currentPath === itemPath
  }
  return currentPath === itemPath || currentPath.startsWith(itemPath + '/')
}
