import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getProfile } from '../api/auth'
import { defaultHomePath } from '../utils/navigation'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { public: true },
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('../views/admin/AdminDashboard.vue'),
        meta: { title: '首页', admin: true },
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('../views/UserProfile.vue'),
        meta: { title: '个人中心', admin: true },
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('../views/Settings.vue'),
        meta: { title: '设置', admin: true },
      },
      {
        path: 'friends',
        name: 'AdminFriendList',
        component: () => import('../views/friend/FriendList.vue'),
        meta: { title: '好友列表', admin: true },
      },
      {
        path: 'messages',
        name: 'AdminMessageList',
        component: () => import('../views/message/MessageList.vue'),
        meta: { title: '消息', admin: true },
      },
      {
        path: 'messages/chat/:userId',
        name: 'AdminChatWindow',
        component: () => import('../views/message/ChatWindow.vue'),
        props: true,
        meta: { title: '聊天', admin: true },
      },
      {
        path: 'students',
        name: 'AdminStudents',
        component: () => import('../views/admin/AdminStudents.vue'),
        meta: { title: '学生管理', admin: true },
      },
      {
        path: 'user/:userId',
        name: 'AdminUserHomePage',
        component: () => import('../views/UserHomePage.vue'),
        props: true,
        meta: { title: '用户主页', admin: true },
      },
      {
        path: 'resources',
        name: 'AdminResources',
        component: () => import('../views/admin/AdminResources.vue'),
        meta: { title: '资料分享管理', admin: true },
      },
      {
        path: 'resources/:id',
        name: 'AdminResourceDetail',
        component: () => import('../views/ResourceDetail.vue'),
        props: true,
        meta: { title: '资料详情', admin: true },
      },
      {
        path: 'upload',
        name: 'AdminUpload',
        component: () => import('../views/UploadResource.vue'),
        meta: { title: '上传资料', admin: true },
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/AdminCategories.vue'),
        meta: { title: '资源分类管理', admin: true },
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('../views/admin/AdminAnnouncements.vue'),
        meta: { title: '系统管理', admin: true },
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: () => import('../views/admin/AdminReports.vue'),
        meta: { title: '举报反馈管理', admin: true },
      },
    ],
  },
  {
    path: '/student',
    component: () => import('../layouts/StudentLayout.vue'),
    meta: { student: true },
    children: [
      {
        path: '',
        name: 'StudentDashboard',
        component: () => import('../views/student/StudentDashboard.vue'),
        meta: { title: '首页', student: true },
      },
      {
        path: 'profile',
        name: 'StudentProfile',
        component: () => import('../views/UserProfile.vue'),
        meta: { title: '个人主页', student: true },
      },
      {
        path: 'user/:userId',
        name: 'UserHomePage',
        component: () => import('../views/UserHomePage.vue'),
        props: true,
        meta: { title: '用户主页', student: true },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/Settings.vue'),
        meta: { title: '设置', student: true },
      },
      {
        path: 'friends',
        name: 'FriendList',
        component: () => import('../views/friend/FriendList.vue'),
        meta: { title: '好友列表', student: true },
      },
      {
        path: 'friends/requests',
        name: 'FriendRequests',
        redirect: { path: '/student/messages', query: { tab: 'requests' } },
        meta: { title: '好友申请', student: true },
      },
      {
        path: 'friends/search',
        name: 'FriendSearch',
        redirect: { path: '/student/friends', query: { tab: 'search' } },
        meta: { title: '搜索好友', student: true },
      },
      {
        path: 'messages',
        name: 'MessageList',
        component: () => import('../views/message/MessageList.vue'),
        meta: { title: '消息', student: true },
      },
      {
        path: 'messages/chat/:userId',
        name: 'ChatWindow',
        component: () => import('../views/message/ChatWindow.vue'),
        props: true,
        meta: { title: '聊天', student: true },
      },
      {
        path: 'favorites',
        name: 'StudentFavorites',
        redirect: { path: '/student/profile', query: { tab: 'favorites' } },
        meta: { title: '我的收藏', student: true },
      },
      {
        path: 'upload',
        name: 'StudentUpload',
        component: () => import('../views/UploadResource.vue'),
        meta: { title: '上传资料', student: true },
      },
      {
        path: 'resources',
        name: 'StudentResources',
        component: () => import('../views/ResourceList.vue'),
        meta: { title: '资料广场', student: true },
      },
      {
        path: 'resources/:id',
        name: 'StudentResourceDetail',
        component: () => import('../views/ResourceDetail.vue'),
        props: true,
        meta: { title: '资料详情', student: true },
      },
      {
        path: 'reports',
        name: 'StudentReports',
        component: () => import('../views/student/StudentReports.vue'),
        meta: { title: '我的举报', student: true },
      },
      {
        path: 'punishments',
        name: 'StudentPunishments',
        component: () => import('../views/student/StudentPunishments.vue'),
        meta: { title: '处罚记录', student: true },
      },
      {
        path: 'announcements',
        name: 'StudentAnnouncements',
        component: () => import('../views/student/StudentAnnouncements.vue'),
        meta: { title: '公告', student: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

let isVerifying = false
let isFirstLoad = true

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    isFirstLoad = false
    return next()
  }

  const isFromAuthPage = _from.name === 'Login' || _from.name === 'Register'
  const isPageRefresh = (isFirstLoad || !_from.name || _from.matched.length === 0) && !isFromAuthPage

  if (isPageRefresh) {
    isFirstLoad = false
    userStore.clear()
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (isFirstLoad && !isFromAuthPage) {
    isFirstLoad = false
  }

  if (!userStore.token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (!userStore.user) {
    if (isVerifying) {
      return next()
    }

    isVerifying = true
    try {
      const res = await getProfile()
      if (res.success && res.data) {
        const backendUser = res.data as any
        const userInfo = {
          userId: backendUser.id || backendUser.userId,
          username: backendUser.username,
          nickname: backendUser.nickname,
          role: backendUser.role,
          avatar: backendUser.avatar,
        }
        userStore.setAuth(userStore.token!, userInfo)
        isVerifying = false

        const isAdminRoute = to.meta.admin || to.matched.some(record => record.meta.admin)
        const isStudentRoute = to.meta.student || to.matched.some(record => record.meta.student)

        if (isAdminRoute && userInfo.role !== 'ADMIN') {
          return next({ path: defaultHomePath(userInfo.role) })
        }

        if (isStudentRoute && userInfo.role === 'ADMIN') {
          return next({ path: '/admin' })
        }

        if (userInfo.role === 'ADMIN' && !isAdminRoute && !isStudentRoute) {
          return next({ path: '/admin' })
        }

        return next()
      } else {
        userStore.clear()
        isVerifying = false
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
    } catch (error: any) {
      const status = error?.response?.status
      const statusCode = error?.response?.statusCode

      if (status === 401 || statusCode === 401) {
        userStore.clear()
        isVerifying = false
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }

      console.error('Token 验证失败:', error)
      userStore.clear()
      isVerifying = false
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }

  const isAdminRoute = to.meta.admin || to.matched.some(record => record.meta.admin)
  const isStudentRoute = to.meta.student || to.matched.some(record => record.meta.student)
  const userRole = userStore.user?.role

  if (isAdminRoute && userRole !== 'ADMIN') {
    return next({ path: defaultHomePath(userRole) })
  }

  if (isStudentRoute && userRole === 'ADMIN') {
    return next({ path: '/admin' })
  }

  if (isAdminRoute && userRole === 'STUDENT') {
    return next({ path: '/student' })
  }

  if (userRole === 'ADMIN' && !isAdminRoute && !isStudentRoute && !to.meta.public) {
    return next({ path: '/admin' })
  }

  next()
})

export default router
