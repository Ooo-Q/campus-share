import axios, { AxiosHeaders } from 'axios'
import type {
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
  AxiosResponse,
} from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const store = useUserStore()
    if (store.token) {
      const headers = AxiosHeaders.from(config.headers)
      headers.set('Authorization', `Bearer ${store.token}`)
      config.headers = headers
    }
    return config
  },
  (error) => Promise.reject(error),
)

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data
    if (data && data.success === false) {
      const silent = (response.config as InternalAxiosRequestConfig & { silent?: boolean }).silent
      if (!silent) {
        ElMessage.error(data.message || '请求失败')
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      const store = useUserStore()
      store.clear()
    } else {
      const silent = error.config ? (error.config as InternalAxiosRequestConfig & { silent?: boolean })?.silent : false
      if (!silent) {
        if (error.config) {
          console.error('Request error:', {
            url: error.config.url,
            method: error.config.method,
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data,
            message: error.message,
          })
        } else {
          console.error('Request error:', {
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data,
            message: error.message,
          })
        }
        ElMessage.error(error.response?.data?.message || error.message || '请求出错')
      }
    }
    return Promise.reject(error)
  },
)

export type ApiRequest = Omit<
  AxiosInstance,
  'get' | 'delete' | 'head' | 'options' | 'post' | 'put' | 'patch' | 'request'
> & {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  head<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  options<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  request<T = unknown>(config: AxiosRequestConfig): Promise<T>
}

export default instance as ApiRequest
