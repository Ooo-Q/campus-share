import axios from 'axios'
import { message } from './feedback'
import { useUserStore } from '../stores/user'
import request from '../api/request'
import type { Resource } from '../api/resource'

/** Normalize avatar path from backend to a browser-loadable URL */
export function getAvatarUrl(avatar?: string | null): string | undefined {
  if (!avatar) return undefined
  const value = avatar.trim()
  if (!value) return undefined

  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('data:')) {
    return value
  }

  // Already proxied
  if (value.startsWith('/api/')) {
    return value
  }

  // Backend stores "/files/public/xxx" or "files/public/xxx"
  if (value.startsWith('/files/') || value.startsWith('files/')) {
    return value.startsWith('/') ? `/api${value}` : `/api/${value}`
  }

  // Bare filename → public files
  if (!value.includes('/')) {
    return `/api/files/public/${value}`
  }

  return value.startsWith('/') ? `/api${value}` : `/api/${value}`
}

export async function downloadResource(
  resource: Resource,
  options?: {
    onSuccess?: () => void
    onError?: (error: any) => void
    updateCount?: (count: number) => void
  },
) {
  if (!resource.fileUrl) {
    message.warning('文件不存在')
    return
  }

  if (resource.allowDownload === false) {
    message.warning('该资料不允许下载')
    return
  }

  try {
    const userStore = useUserStore()
    const headers: any = {}
    if (userStore.token) {
      headers.Authorization = `Bearer ${userStore.token}`
    }

    const backendUrl = import.meta.env.VITE_API_BASE_URL || ''
    const downloadUrl = `${backendUrl}/api${resource.fileUrl}`

    const response = await axios.get(downloadUrl, {
      responseType: 'blob',
      headers,
      timeout: 60000,
    })

    let fileName = resource.title || 'download'
    const contentDisposition = response.headers['content-disposition']
    if (contentDisposition) {
      const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (fileNameMatch && fileNameMatch[1]) {
        fileName = decodeURIComponent(fileNameMatch[1].replace(/['"]/g, ''))
      }
    } else {
      const urlParts = resource.fileUrl.split('/')
      const lastPart = urlParts[urlParts.length - 1]
      if (lastPart) {
        fileName = lastPart
      }
    }

    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)

    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()

    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    try {
      await request.post(`/resources/${resource.id}/download`)
    } catch (e) {
      console.warn('记录下载次数失败:', e)
    }

    if (options?.updateCount) {
      options.updateCount((resource.downloadCount || 0) + 1)
    }

    message.success('下载成功')
    options?.onSuccess?.()
  } catch (error: any) {
    console.error('下载错误详情:', {
      error: error,
      message: error.message,
      response: error.response,
      config: error.config,
      url: error.config?.url,
    })

    let errorMessage = '下载失败'
    if (error.response?.status === 404) {
      errorMessage = '文件不存在'
    } else if (error.code === 'ECONNREFUSED' || error.message?.includes('Network Error')) {
      errorMessage = '无法连接到后端服务器，请确保后端服务运行正常'
    } else if (error.response?.data?.message) {
      errorMessage = error.response.data.message
    } else if (error.message) {
      errorMessage = error.message
    }

    message.error(errorMessage)
    options?.onError?.(error)
  }
}

export function viewResource(
  resource: Resource,
  options?: {
    onView?: () => void
    router?: any
  },
) {
  if (!resource.fileUrl) {
    message.warning('文件不存在')
    return
  }

  if (options?.router) {
    const path = options.router.currentRoute.value.path.startsWith('/admin')
      ? `/admin/resources/${resource.id}`
      : `/student/resources/${resource.id}`
    options.router.push(path)
    options.onView?.()
    return
  }

  const backendUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  const viewUrl = `${backendUrl}/api${resource.fileUrl}`
  window.open(viewUrl, '_blank')
  options?.onView?.()
}
